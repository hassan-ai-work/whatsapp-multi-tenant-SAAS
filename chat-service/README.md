# chat-service

Generic async multi-tenant chat orchestrator.

## Flow
1. `ExternalIngressController` accepts anonymous POST traffic.
2. Payload goes to Kafka topic `incoming-customer-messages`.
3. Kafka consumer loads Redis history, runs LangChain4j RAG, and generates reply.
4. `OutboundChannelDispatcher` sends reply to outbound endpoint.
5. Redis expiry listener archives idle sessions into `contact_memories`.

## Run locally
```bash
cd /home/hassan/D/work/AI/whatsapp-multi-tenant-SAAS/chat-service
./mvnw spring-boot:run
```

## Build image
```bash
cd /home/hassan/D/work/AI/whatsapp-multi-tenant-SAAS/chat-service
docker build -t chat-service:local .
```

## Notes
- Embeddings use `text-embedding-3-small` by default to keep 1536 dimensions aligned with `document_chunks` and `contact_memories`.
- Redis key format: `session:{tenantIdentity}:{customerIdentity}`.

## Inbound modules
- `channel-adapters/`: adapter registry + WhatsApp adapter (`service/channeladapters`).
- `identity-resolution/`: `TenantIdentityResolver` implements `IdentityResolutionService` and enriches metadata from DB.
- `message-normalization/`: canonical header/payload merge (`service/messagenormalization`).
- `inbound-validation/`: validator chain for required fields and max message length (`service/inboundvalidation`).
- `dead-letter/`: failed consumer events go to Kafka DLQ with retry/backoff (`service/deadletter`).

## Run tests
```bash
cd /home/hassan/D/work/AI/whatsapp-multi-tenant-SAAS/chat-service
./mvnw test
```

