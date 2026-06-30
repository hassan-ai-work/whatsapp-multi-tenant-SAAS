# ngrok + Webhook Integration Summary

## Your Questions Answered

### 1. What URL will `ngrok http 9000` produce?

**Format**: `https://<random-id>.ngrok-free.app`

Example outputs:
```
https://abc123def456.ngrok-free.app/webhook/meta
https://xyz789abc123.ngrok-free.app/webhook/meta
```

### 2. Is URL fixed or changes every time?

**Free tier (current)**: Changes per session restart
- Each `ngrok http 9000` generates new random subdomain
- Lasts until ngrok process stops
- Useful for local dev testing

**Paid tier**: Static subdomain available
- Same URL persists across sessions
- Better for production webhooks
- Recommended when moving to staging/production

### 3. Is it safe to place ngrok URL in WhatsApp webhook?

**✅ YES - with proper validation**

**Security layers**:
1. **HTTPS encryption**: ngrok always tunnels over HTTPS (encrypted in transit)
2. **Meta origin verification**: X-Hub-Signature-256 header (HMAC-SHA256)
   - Only Meta can compute valid signature
   - Requires webhook token (stored in env var, never exposed)
3. **Stateless**: Webhook token not in URL, not logged publicly
4. **Rate limited**: ngrok has abuse prevention

**Example**:
- Meta sends: `POST https://abc123.ngrok-free.app/webhook/meta`
  - Header: `X-Hub-Signature-256: sha256=<hmac_value>`
  - Body: WhatsApp message payload
- Gateway validates signature matches: `HMAC-SHA256(body, verify_token)`
- Only valid requests forwarded to chat-service

### 4. ChannelEventPayload Structure Origin

**NOT from WhatsApp webhook directly**

**Actual WhatsApp payload** (from Meta):
```json
{
  "object": "whatsapp_business_account",
  "entry": [{
    "changes": [{
      "value": {
        "messages": [{
          "from": "1234567890",
          "text": {"body": "Hello"},
          "timestamp": "1234567890"
        }],
        "metadata": {
          "phone_number_id": "123",
          "display_phone_number": "1234567890"
        }
      }
    }]
  }]
}
```

**Our generic ChannelEventPayload** (internal format):
```java
record ChannelEventPayload(
    String tenantIdentity,         // mapped from phone_number_id
    String customerIdentity,       // extracted from message.from
    String conversationPayload,    // extracted from message.text.body
    ChannelSourceType channelSourceType,  // "INTEGRATION" for WhatsApp
    Map<String, String> metadata   // phone_number_id, display_phone_number, etc
)
```

**Transformation**: `WhatsAppWebhookTransformer` service adapts Meta structure → our format

---

## Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ WhatsApp Business API (Meta)                                    │
│ Sends: POST with X-Hub-Signature-256 header                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    (HTTPS encrypted)
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ ngrok tunnel: https://abc123.ngrok-free.app                     │
│ Proxies requests to localhost:9000                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ api-gateway:9000/webhook/meta (WebhookController)               │
│ 1. Validate X-Hub-Signature-256 (WhatsAppWebhookValidator)      │
│ 2. Parse request body (WhatsAppWebhookPayload DTO)              │
│ 3. Transform to ChannelEventPayload (WhatsAppWebhookTransformer)│
│ 4. Forward to chat-service (ChatServiceClient)                  │
│ 5. Return 200 OK immediately (async processing)                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ chat-service:8084/ingress/channel (ExternalIngressController)   │
│ Receives ChannelEventPayload                                     │
│ Publishes to Kafka: incoming-customer-messages                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Kafka Consumer (IncomingChannelEventConsumer)                    │
│ Routes to ConversationOrchestrationService                      │
│ - Load history from Redis                                        │
│ - Embed query + vector search                                    │
│ - Generate response via LLM                                      │
│ - Dispatch via OutboundChannelDispatcher (POST back to Meta)     │
└────────────────────────────────────────���────────────────────────┘
```

---

## Environment Setup

### Local Dev
```bash
# Terminal 1: Kafka + Redis + Postgres (docker-compose)
docker-compose up

# Terminal 2: api-gateway
cd api-gateway
export WHATSAPP_WEBHOOK_VERIFY_TOKEN="local-verify-token"
./mvnw spring-boot:run

# Terminal 3: chat-service
cd chat-service
./mvnw spring-boot:run

# Terminal 4: ngrok tunnel
ngrok http 9000

# Output includes:
# Forwarding https://abc123def456.ngrok-free.app -> http://localhost:9000
```

### Meta Webhook Config
1. App Dashboard → Webhooks
2. Callback URL: `https://abc123def456.ngrok-free.app/webhook/meta`
3. Verify Token: `local-verify-token` (or set in env)
4. Click Verify and Save

---

## Key Files Created

1. **`api-gateway/src/main/java/.../controller/WebhookController.java`**
   - POST/GET `/webhook/meta` endpoints
   - Receives WhatsApp messages
   - Verifies signature + forwards

2. **`api-gateway/src/main/java/.../service/WhatsAppWebhookValidator.java`**
   - Validates X-Hub-Signature-256
   - Computes HMAC-SHA256

3. **`api-gateway/src/main/java/.../service/WhatsAppWebhookTransformer.java`**
   - Parses WhatsApp payload DTO
   - Extracts tenant, customer, message, metadata
   - Maps to ChannelEventPayload

4. **`api-gateway/src/main/java/.../client/ChatServiceClient.java`**
   - REST client
   - Forwards ChannelEventPayload to chat-service

5. **`api-gateway/src/main/resources/application.yml`**
   - Added webhook config
   - Added chat-service URL config

6. **`api-gateway/src/main/java/.../config/SecurityConfig.java`**
   - Allow `/webhook/**` routes without Keycloak auth
   - Added RestTemplate + ObjectMapper beans

---

## Testing

### Verify Setup
```bash
# Check api-gateway compiled
cd api-gateway && ./mvnw -q -DskipTests compile

# Check ngrok reachable
curl -v https://abc123.ngrok-free.app/webhook/meta -X GET

# Check signature validation (should fail without valid header)
curl -X POST https://abc123.ngrok-free.app/webhook/meta \
  -H "Content-Type: application/json" \
  -d '{"test":"data"}'
# Expected: 401 Unauthorized
```

### Full Flow Test (Once Meta Connected)
1. Send WhatsApp message to registered number
2. Check logs:
   - api-gateway: Signature validated, forwarded
   - chat-service: Message published to Kafka
   - consumer: Processing started
3. Check Redis: Session key created
4. Check response: AI reply sent back to WhatsApp

---

## Production Checklist

- [ ] Use ngrok paid tier (static subdomain)
- [ ] Set WHATSAPP_WEBHOOK_VERIFY_TOKEN in environment (strong token)
- [ ] Enable Redis keyspace listener (for session archival)
- [ ] Add phone_number_id → tenant DB lookup (no pass-through)
- [ ] Log webhook calls separately (security audit trail)
- [ ] Monitor signature validation failures (abuse detection)
- [ ] Rate limit webhook endpoint (prevent DoS)

