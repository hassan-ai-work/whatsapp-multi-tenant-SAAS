package com.levosoft.microservice.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class WhatsAppWebhookValidator {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    @Value("${whatsapp.webhook.verify-token:}")
    private String webhookVerifyToken;

    /**
     * Validate incoming webhook signature. <br/>
     * X-Hub-Signature-256 = sha256=HMAC-SHA256(body, webhook_token)
     */
    public boolean isValidSignature(String xHubSignature, String rawBody) {
        if (webhookVerifyToken == null || webhookVerifyToken.isBlank()) {
            throw new IllegalStateException("whatsapp.webhook.verify-token not configured");
        }

        if (xHubSignature == null || !xHubSignature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        String providedSignature = xHubSignature.substring(SIGNATURE_PREFIX.length());
        String expectedSignature = computeHmacSha256(rawBody, webhookVerifyToken);

        return providedSignature.equals(expectedSignature);
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(),
                0,
                secret.getBytes().length,
                HMAC_ALGORITHM
            );
            mac.init(keySpec);
            byte[] macData = mac.doFinal(data.getBytes());
            return bytesToHex(macData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}

