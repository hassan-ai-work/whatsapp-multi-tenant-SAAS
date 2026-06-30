# WhatsApp Webhook Integration Guide

## Overview
Receive WhatsApp Business API messages locally via ngrok tunneling.

## Flow
```
WhatsApp API (Meta)
  ↓ (X-Hub-Signature-256 header)
ngrok: https://<id>.ngrok-free.app/webhook/meta
  ↓ (HTTPS tunnel)
api-gateway:9000/webhook/meta
  ├─ Validate signature (HMAC-SHA256)
  ├─ Parse WhatsApp payload
  └─ Forward to chat-service:8084/ingress/channel
    ├─ Publish to Kafka topic: incoming-customer-messages
    ├─ Consumer processes async
    └─ Response sent back to WhatsApp via outbound dispatcher
```

## Setup

### 1. Start Services
```bash
# Start api-gateway (port 9000)
cd api-gateway && ./mvnw spring-boot:run

# Start chat-service (port 8084)
cd chat-service && ./mvnw spring-boot:run

# (Kafka, Redis, Postgres assumed running via docker-compose)
```

### 2. Configure ngrok
```bash
# Start tunnel to api-gateway port 9000
ngrok http 9000

# Output:
# Session Status: online
# URL: https://abc123def456.ngrok-free.app
```

### 3. Meta Webhook Setup
1. Go to **Meta App Dashboard** → Your App → **Webhooks**
2. Edit webhook subscription:
   - **Callback URL**: `https://abc123def456.ngrok-free.app/webhook/meta`
   - **Verify Token**: Set to env var `WHATSAPP_WEBHOOK_VERIFY_TOKEN` (default: `local-verify-token`)
3. Click **Verify and Save**

### 4. Test Message Flow
- Send message from WhatsApp to registered phone number
- Logs will show:
  - api-gateway: Signature validation + forward to chat-service
  - chat-service: Kafka publish → consumer processes
  - outbound dispatcher: Response sent back

## What ngrok Does

### URL Format
- **Free tier**: `https://<random-id>.ngrok-free.app` (changes per session)
- **Paid tier**: Static subdomain option available

### Signature Validation (No Leakage)
- Meta sends `X-Hub-Signature-256: sha256=<hmac_value>`
- Gateway validates with stored webhook token
- Token never exposed in URL or logs
- Random ID per session adds security

### Safety Notes
- ✅ HTTPS encrypted tunnel
- ✅ Meta validates origin via signature
- ✅ Verify token stays in env vars (not in code/logs)
- ✅ Disable webhook when not testing (remove from Meta dashboard)
- ⚠️ ngrok logs all traffic by default (can disable in paid tier)

## Environment Variables

In `api-gateway`:
```yaml
# application.yml
whatsapp:
  webhook:
    verify-token: ${WHATSAPP_WEBHOOK_VERIFY_TOKEN:local-verify-token}

chat-service:
  url: ${CHAT_SERVICE_URL:http://localhost:8084}
```

Set before running:
```bash
export WHATSAPP_WEBHOOK_VERIFY_TOKEN="your-token-from-meta"
export CHAT_SERVICE_URL="http://localhost:8084"
```

## Troubleshooting

### "401 Unauthorized" on webhook
- Verify token mismatch
- Check env var `WHATSAPP_WEBHOOK_VERIFY_TOKEN` matches Meta config
- Ensure X-Hub-Signature-256 header present

### "400 Bad Request"
- Malformed JSON from Meta payload
- Check ngrok logs: `ngrok -v`

### Message not reaching chat-service
- Check api-gateway logs for signature validation
- Verify ngrok tunnel active: `ngrok status` or visit ngrok web UI
- Confirm Kafka consumer running (see chat-service logs)

### ngrok URL Changes on Restart
- Free tier generates new URL per session
- Must update Meta webhook callback URL
- Use paid tier for static subdomain (production)

## Files Changed
- `api-gateway/src/main/java/.../controller/WebhookController.java` → Receiver + verification
- `api-gateway/src/main/java/.../service/WhatsAppWebhookValidator.java` → HMAC validation
- `api-gateway/src/main/java/.../service/WhatsAppWebhookTransformer.java` → Payload transformation
- `api-gateway/src/main/java/.../client/ChatServiceClient.java` → Forward to chat-service
- `api-gateway/src/main/resources/application.yml` → Webhook config
- `api-gateway/src/main/java/.../config/SecurityConfig.java` → Allow /webhook/** without auth

