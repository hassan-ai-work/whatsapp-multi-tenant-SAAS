package com.levosoft.microservice.chat.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhookPayload(
        String object,
        List<Entry> entry
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            String id,
            List<Change> changes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Change(
            String field,
            Value value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            @JsonProperty("messaging_product") String messagingProduct,
            Metadata metadata,
            List<Contact> contacts,
            List<WhatsAppTextMessage> messages
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Metadata(
                @JsonProperty("phone_number_id") String phoneNumberId,
                @JsonProperty("display_phone_number") String displayPhoneNumber
        ) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Contact(
                @JsonProperty("wa_id") String waId,
                Profile profile
        ) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Profile(String name) {
            }
        }
    }
}

