# 🧠 whatsapp-brain-service

`☕ Java` &nbsp;&nbsp; `🌱 Spring Boot` &nbsp;&nbsp; `🟤 Apache Kafka` &nbsp;&nbsp; `🐘 PostgreSQL` &nbsp;&nbsp; `🔴 Redis` &nbsp;&nbsp; `🐳 Docker` &nbsp;&nbsp; `🤖 OpenAI`

---

This is the core AI orchestration microservice for the multi-tenant WhatsApp AI Agent ecosystem. It functions as an internal resource server sitting safely behind the API Gateway.

## 🚀 Key Responsibilities
* **Asynchronous Processing:** Consumes incoming WhatsApp text events from Kafka.
* **Session State:** Uses Redis to manage live short-term conversation session state.
* **Semantic Retrieval (RAG):** Queries business-scoped documentation and vector data from PostgreSQL (`pgvector`).
* **AI Generation:** Interfaces with LLMs (OpenAI / Ollama) via LangChain4j and manages cyclic multi-agent states using LangGraph4j.
* **Multi-Tenancy:** Enforces strict ownership boundaries across tenant, business, channel, document, and memory records.
* **MCP:** MCP Server for contracts.

## 🏗️ Current Domain Model
The current registration and ownership flow is:

`Tenant -> Business -> Registered Channel`

### Tenant
A tenant is the top-level customer boundary.

### Business
A tenant can own many businesses.

Each business:
- belongs to exactly one tenant
- has one registered business number in `tenant_business.registered_number`
- is the ownership boundary for channels, documents, and memories

### Registered Channel
A business can register channels such as:
- `whatsapp`
- `telegram`

Current behavior:
- channel types are looked up from the `channels` table by `code`
- a business can have channel registration history per channel code
- old registration rows are preserved when a channel is replaced
- one business can have many channels over time, and many channels per business later as the model expands

## 🧠 Memory and Conversation Model
### Live Session State
Redis handles live conversation session state for active chats.

### Long-Term AI Reference
`contact_memories` stores:
- `contact_identifier` such as phone number or chat identity
- final conversation text as stored blob data
- embedding vectors for future AI retrieval/reference

This table is intended for later AI memory usage, not for live session coordination.

## 📄 Document Ingestion Model
Documents are now business-scoped.

That means:
- each document belongs to a `tenant_id`
- each document also belongs to a `business_id`
- downstream document chunks are expected to remain business-scoped as well

The current ingestion flow resolves the tenant from the authenticated username, validates business ownership, stores the document under the business, uploads the file, and publishes the ingestion event for downstream processing.

## 🛠️ Technology Stack
* **Runtime:** Java 21 / Spring Boot 4.x
* **Messaging:** Spring Kafka (Consumer/Producer)
* **Storage:** Redis (Session State) & PostgreSQL (Relational Data & Vector Store)
* **AI Ecosystem:** LangChain4j, LangGraph4j, Apache Tika

---

## 🚧 Status: Work in Progress
* [x] Architecture & package dependencies mapped.
* [x] Tenant module scaffolded.
* [x] Business module scaffolded.
* [x] Channel registration module scaffolded.
* [x] Memory module scaffolded.
* [x] Business-scoped document ingestion flow scaffolded.
* [ ] Build Kafka consumer to ingest live conversation events.
* [ ] Implement LangChain4j / Redis stateful conversation pipeline.
* [ ] Connect long-term memory retrieval into agent workflows.

> **Note:** Development is ongoing. For infrastructure setup (Kafka/Redis/Postgres), refer to the parallel orchestration folders.
