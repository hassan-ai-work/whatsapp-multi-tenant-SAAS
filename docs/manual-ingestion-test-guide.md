# Manual Ingestion Test Guide

This guide explains how to manually test the document ingestion flow implemented across:

- `api-gateway`
- `whatsapp-brain-service`
- `kafka-service`

It includes:

1. current ownership model
2. prerequisites
3. startup order
4. how to get an access token
5. how to call the ingest endpoint
6. how to verify database rows
7. how to verify MinIO upload
8. how to verify Kafka processing
9. how to test failure scenarios

---

## 1. Current ownership model

The current ownership model is:

`Tenant -> Business -> Registered Channel`

### Tenant
- top-level ownership boundary
- resolved in the brain service from `X-Authenticated-User`
- `preferred_username` in the JWT must match `public.tenants.username`

### Business
- belongs to one tenant
- a tenant can own many businesses
- each business currently has one registered number
- documents now belong to both a tenant and a business

### Registered Channel
- channels are looked up by `channel_code`
- one business may register channels such as `whatsapp` or `telegram`
- channel replacement preserves old rows using `linked_status`

### Memory
- Redis handles live conversation session state
- `contact_memories` stores final conversation text and embedding for future AI reference

### Documents
- documents are business-scoped
- document chunks are also expected to be business-scoped
- ingest requests must target a valid business owned by the resolved tenant

---

## 2. Prerequisites

Before testing, make sure these are running:

- PostgreSQL
- Kafka
- MinIO
- Keycloak
- `whatsapp-brain-service`
- `kafka-service`
- `api-gateway`

Also make sure:

- DB schema already exists
- MinIO bucket exists:
  - `document-ingestion`
- JWT contains:
  - `preferred_username`
- a tenant exists in DB with username matching `preferred_username`
- a business exists for that tenant, because ingestion is now business-scoped

---

## 3. Startup order

Recommended order:

1. PostgreSQL
2. Kafka
3. MinIO
4. Keycloak
5. `whatsapp-brain-service`
6. `kafka-service`
7. `api-gateway`

---

## 4. Verify tenant and business exist

Current tenant resolution depends on username.

The brain service resolves tenant ownership from:

- `tenantRepository.findByUsername(authenticatedUsername.toLowerCase())`

Use SQL:

