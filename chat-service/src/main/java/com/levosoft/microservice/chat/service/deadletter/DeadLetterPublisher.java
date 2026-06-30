package com.levosoft.microservice.chat.service.deadletter;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.levosoft.microservice.chat.config.ChatProperties;
import com.levosoft.microservice.chat.dto.DeadLetterEvent;

@Service
public class DeadLetterPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ChatProperties chatProperties;

    public DeadLetterPublisher(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               ChatProperties chatProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.chatProperties = chatProperties;
    }

    public void publish(String sourceTopic, String payload, Exception error) {
        DeadLetterEvent event = new DeadLetterEvent(
                sourceTopic,
                payload,
                error.getClass().getSimpleName(),
                error.getMessage(),
                Instant.now());

        String eventJson = serialize(event);
        int maxAttempts = Math.max(1, chatProperties.kafka().deadLetterMaxAttempts());
        long backoffMs = Math.max(0L, chatProperties.kafka().deadLetterBackoffMs());

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                kafkaTemplate.send(chatProperties.kafka().deadLetterTopic(), eventJson);
                log.warn("Dead-letter queued topic={} attempt={} errorType={}",
                        chatProperties.kafka().deadLetterTopic(), attempt, event.errorType());
                return;
            } catch (Exception ex) {
                if (attempt == maxAttempts) {
                    log.error("Dead-letter publish failed after {} attempts", maxAttempts, ex);
                    return;
                }
                if (backoffMs > 0) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    private String serialize(DeadLetterEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"dead_letter_serialization_failed\"}";
        }
    }
}

