package com.levosoft.microservice.chat.dto;

import java.time.Instant;

public record DeadLetterEvent(
        String sourceTopic,
        String payload,
        String errorType,
        String errorMessage,
        Instant occurredAt
) {
}

