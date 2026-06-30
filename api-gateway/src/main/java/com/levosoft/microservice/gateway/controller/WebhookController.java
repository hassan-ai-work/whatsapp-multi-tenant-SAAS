package com.levosoft.microservice.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.levosoft.microservice.gateway.service.WhatsAppWebhookValidator;
import org.springframework.beans.factory.annotation.Value;
import com.levosoft.microservice.gateway.client.ChatServiceClient;

import java.io.IOException;

/**
 * Public webhook endpoint for WhatsApp Business API.
 * No auth required (Meta provides HMAC signature validation).
 * Receives incoming messages and forwards to chat-service.
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private final WhatsAppWebhookValidator validator;
    private final ChatServiceClient chatServiceClient;
    private final ObjectMapper objectMapper;
    private final String webhookVerifyToken;

    public WebhookController(
        WhatsAppWebhookValidator validator,
        ChatServiceClient chatServiceClient,
        ObjectMapper objectMapper,
        @Value("${whatsapp.webhook.verify-token:}") String webhookVerifyToken
    ) {
        this.validator = validator;
        this.chatServiceClient = chatServiceClient;
        this.objectMapper = objectMapper;
        this.webhookVerifyToken = webhookVerifyToken;
    }

    /**
     * POST /webhook/meta - Receive incoming WhatsApp messages.
     * Validates X-Hub-Signature-256, validates payload shape, forwards raw body to chat-service.
     */
    @PostMapping("/meta")
    public ResponseEntity<Void> receiveWhatsAppWebhook(
        @RequestHeader("X-Hub-Signature-256") String xHubSignature,
        @RequestBody String rawBody
    ) {
        // Validate signature
        if (!validator.isValidSignature(xHubSignature, rawBody)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            JsonNode payload = objectMapper.readTree(rawBody);

            // Minimal payload validation in gateway.
            if (!hasAtLeastOneMessage(payload)) {
                return ResponseEntity.ok().build();
            }

            chatServiceClient.forwardWhatsAppWebhook(rawBody);

            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * GET /webhook/meta - Webhook verification endpoint (Meta calls this on setup).
     * Responds with challenge parameter if token matches.
     */
    @GetMapping("/meta")
    public ResponseEntity<String> verifyWebhook(
        @RequestParam(name = "hub.mode") String mode,
        @RequestParam(name = "hub.challenge") String challenge,
        @RequestParam(name = "hub.verify_token") String token
    ) {
        if (webhookVerifyToken == null || webhookVerifyToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if ("subscribe".equals(mode) && webhookVerifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private boolean hasAtLeastOneMessage(JsonNode payload) {
        if (payload == null || !payload.has("entry") || !payload.get("entry").isArray() || payload.get("entry").isEmpty()) {
            return false;
        }
        JsonNode entry = payload.get("entry").get(0);
        JsonNode changes = entry.get("changes");
        if (changes == null || !changes.isArray() || changes.isEmpty()) {
            return false;
        }
        JsonNode value = changes.get(0).get("value");
        if (value == null) {
            return false;
        }
        JsonNode messages = value.get("messages");
        return messages != null && messages.isArray() && !messages.isEmpty();
    }
}

