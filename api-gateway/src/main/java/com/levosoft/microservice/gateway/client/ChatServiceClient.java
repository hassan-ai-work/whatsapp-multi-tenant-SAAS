package com.levosoft.microservice.gateway.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Chat service client for forwarding ingress messages.
 */
@Service
public class ChatServiceClient {
    private final RestTemplate restTemplate;
    private final String chatServiceUrl;

    public ChatServiceClient(
        RestTemplate restTemplate,
        @Value("${chat-service.url:http://localhost:8084}") String chatServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.chatServiceUrl = chatServiceUrl;
    }

    /**
     * Forward validated raw WhatsApp payload to chat-service.
     */
    public void forwardWhatsAppWebhook(String rawBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Channel-Source-Type", "WHATSAPP");
            HttpEntity<String> request = new HttpEntity<>(rawBody, headers);
            restTemplate.postForObject(chatServiceUrl + "/ingress/whatsapp", request, Object.class);
        } catch (Exception e) {
            // Do not block webhook response path.
            System.err.println("Failed to forward to chat-service: " + e.getMessage());
        }
    }
}

