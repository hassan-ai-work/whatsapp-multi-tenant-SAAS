-- =====================================================================
-- Base migration for AI Platform Gateway (Postgres + pgvector, HNSW)
-- Optimized for Redis-Driven Idempotency Filters & Multi-Tenancy
-- =====================================================================

-- 1. Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- =====================================================================
-- 2. Enums & Shared Functions
-- =====================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tenant_status') THEN
CREATE TYPE tenant_status AS ENUM ('ACTIVE', 'SUSPENDED');
END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'document_status') THEN
CREATE TYPE document_status AS ENUM ('PENDING', 'PROCESSED', 'FAILED');
END IF;
END$$;

CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- 3. Tenants & Config
-- =====================================================================

CREATE TABLE IF NOT EXISTS tenants (
                                       id          BIGSERIAL PRIMARY KEY,
                                       name        TEXT NOT NULL,
                                       status      tenant_status NOT NULL DEFAULT 'ACTIVE',
                                       created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_tenants_name
    ON tenants (name);

CREATE TRIGGER trg_tenants_set_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

CREATE TABLE IF NOT EXISTS tenant_configs (
                                              id                       BIGSERIAL PRIMARY KEY,
                                              tenant_id                BIGINT NOT NULL UNIQUE,
                                              default_provider         TEXT NOT NULL,
                                              default_model            TEXT NOT NULL,
                                              temperature              DOUBLE PRECISION,
                                              max_tokens               INTEGER,
                                              rag_enabled              BOOLEAN NOT NULL DEFAULT FALSE,
                                              memory_window_size       INTEGER NOT NULL DEFAULT 10,
                                              long_term_memory_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                                              config_json              JSONB NOT NULL DEFAULT '{}'::jsonb,
                                              created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tenant_configs_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    );

CREATE TRIGGER trg_tenant_configs_set_updated_at
    BEFORE UPDATE ON tenant_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- =====================================================================
-- 4. Global Canonical Users
-- =====================================================================

CREATE TABLE IF NOT EXISTS users (
                                     id            BIGSERIAL PRIMARY KEY,
                                     external_id   TEXT NOT NULL UNIQUE,        -- Global anchor key (e.g. WhatsApp JID)
                                     global_status TEXT NOT NULL DEFAULT 'ACTIVE',
                                     user_metadata JSONB NOT NULL DEFAULT '{}'::jsonb, -- Store cross-tenant profile attributes here
                                     created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE TRIGGER trg_users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- =====================================================================
-- 5. Tenant-Scoped Long-Term User Memories
-- =====================================================================

CREATE TABLE IF NOT EXISTS user_memories (
                                             id               BIGSERIAL PRIMARY KEY,
                                             tenant_id        BIGINT NOT NULL,
                                             user_id          BIGINT NOT NULL,
                                             memory_type      TEXT NOT NULL DEFAULT 'FACT', -- 'FACT', 'PREFERENCE', 'SUMMARY'
                                             content          TEXT NOT NULL,            -- The extracted global profile summary text
                                             embedding        VECTOR(1536) NOT NULL,    -- Vector representation of the content summary
    idempotency_hash TEXT NOT NULL,            -- Keeps history traceable to a specific hash verification block
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_memories_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_memories_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    -- ER Diagram Safe: Explicitly enforces exactly one memory profile per user, per tenant
    CONSTRAINT ux_user_memories_tenant_user_identity
    UNIQUE (tenant_id, user_id)
    );

-- Real-time lookups to fetch all valid insights for a specific user under a targeted tenant
CREATE INDEX IF NOT EXISTS idx_user_memories_lookup
    ON user_memories (tenant_id, user_id);

-- High-performance HNSW index using Cosine Distance for optimal text vector operations
CREATE INDEX IF NOT EXISTS idx_user_memories_hnsw_cosine_embedding
    ON user_memories
    USING hnsw (embedding vector_cosine_ops);

CREATE TRIGGER trg_user_memories_set_updated_at
    BEFORE UPDATE ON user_memories
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- =====================================================================
-- 6. Documents & Document Chunks (RAG)
-- =====================================================================

CREATE TABLE IF NOT EXISTS documents (
                                         id          BIGSERIAL PRIMARY KEY,
                                         tenant_id   BIGINT NOT NULL,
                                         title       TEXT NOT NULL,
                                         source      TEXT NOT NULL,
                                         status      document_status NOT NULL DEFAULT 'PENDING',
                                         metadata    JSONB NOT NULL DEFAULT '{}'::jsonb,
                                         created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_documents_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT ux_documents_tenant_identity
    UNIQUE (id, tenant_id)
    );

CREATE INDEX IF NOT EXISTS idx_documents_tenant
    ON documents (tenant_id);

CREATE TRIGGER trg_documents_set_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

CREATE TABLE IF NOT EXISTS document_chunks (
                                               id          BIGSERIAL PRIMARY KEY,
                                               tenant_id   BIGINT NOT NULL,
                                               document_id BIGINT NOT NULL,
                                               content     TEXT NOT NULL,
                                               embedding   VECTOR(1536) NOT NULL,
    metadata    JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_document_chunks_document_validation
    FOREIGN KEY (document_id, tenant_id) REFERENCES documents(id, tenant_id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_document_chunks_tenant
    ON document_chunks (tenant_id);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document
    ON document_chunks (document_id);

CREATE INDEX IF NOT EXISTS idx_document_chunks_hnsw_cosine_embedding
    ON document_chunks
    USING hnsw (embedding vector_cosine_ops);

-- =====================================================================
-- 7. Request Logs & Auditing
-- =====================================================================

CREATE TABLE IF NOT EXISTS request_logs (
                                            id                  BIGSERIAL PRIMARY KEY,
                                            tenant_id           BIGINT NOT NULL,
                                            endpoint            TEXT NOT NULL,
                                            provider            TEXT,
                                            model               TEXT,
                                            status_code         INTEGER NOT NULL,
                                            latency_ms          BIGINT NOT NULL,
                                            prompt_tokens       INTEGER,
                                            completion_tokens   INTEGER,
                                            created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_request_logs_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_request_logs_tenant_created
    ON request_logs (tenant_id, created_at);

CREATE INDEX IF NOT EXISTS idx_request_logs_endpoint
    ON request_logs (endpoint);
