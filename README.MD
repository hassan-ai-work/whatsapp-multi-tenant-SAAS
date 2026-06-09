# 🧠 whatsapp-brain-service

`☕ Java` &nbsp;&nbsp; `🌱 Spring Boot` &nbsp;&nbsp; `🟤 Apache Kafka` &nbsp;&nbsp; `🐘 PostgreSQL` &nbsp;&nbsp; `🔴 Redis` &nbsp;&nbsp; `🐳 Docker` &nbsp;&nbsp; `🤖 OpenAI`

---

This is the core AI orchestration microservice for the multi-tenant WhatsApp AI Agent ecosystem. It functions as an internal resource server sitting safely behind the API Gateway.

## 🚀 Key Responsibilities
* **Asynchronous Processing:** Consumes incoming WhatsApp text events from Kafka.
* **Context Management:** Manages rapid short-term conversation histories via Redis Cache.
* **Semantic Retrieval (RAG):** Queries enterprise business documentation streams out of PostgreSQL (`pgvector`).
* **AI Generation:** Interfaces with LLMs (OpenAI / Ollama) via LangChain4j and manages cyclic multi-agent states using LangGraph4j.
* **Multi-Tenancy:** Ensures strict data isolation by appending logical tenant filtering to standard text logs.
* **MCP:** MCP Server for contracts.

## 🛠️ Technology Stack
* **Runtime:** Java 21 / Spring Boot 4.x
* **Messaging:** Spring Kafka (Consumer)
* **Storage:** Redis (Cache) & PostgreSQL (Data & Vector Store)
* **AI Ecosystem:** LangChain4j, LangGraph4j, Apache Tika

---

## 🚧 Status: Work in Progress
* [x] Architecture & package dependencies mapped.
* [ ] Initialize boilerplate project via start.spring.io.
* [ ] Configure Multi-Tenant database schemas and text log tracking.
* [ ] Build Kafka consumer to ingest events from the ingestion router.
* [ ] Implement LangChain4j / Redis stateful conversation pipeline.

> **Note:** Development is ongoing. For infrastructure setup (Kafka/Redis/Postgres), refer to the parallel orchestration folders.
