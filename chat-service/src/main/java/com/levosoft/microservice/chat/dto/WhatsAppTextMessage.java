package com.levosoft.microservice.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppTextMessage(
        String id,
        String from,
        String timestamp,
        TextBody text,
        String type
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TextBody(String body) {
    }
}

