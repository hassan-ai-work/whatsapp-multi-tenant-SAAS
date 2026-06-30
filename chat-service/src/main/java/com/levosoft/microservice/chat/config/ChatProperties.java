package com.levosoft.microservice.chat.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat")
public record ChatProperties(
        Kafka kafka,
        Session session,
        Llm llm,
        Embedding embedding,
        Rag rag,
        Inbound inbound,
        Outbound outbound
) {
    public record Kafka(String incomingTopic,
                        String groupId,
                        String deadLetterTopic,
                        int deadLetterMaxAttempts,
                        long deadLetterBackoffMs) {}

    public record Session(String keyPrefix, String snapshotPrefix, Duration ttl, Duration snapshotTtl) {}

    public record Llm(String provider, String model, String baseUrl, String apiKey) {}

    public record Embedding(String provider, String model, String baseUrl, String apiKey, int dimensions) {}

    public record Rag(int maxResults, double minimumScore, String promptTemplate) {}

    public record Inbound(int maxMessageLength) {}

    public record Outbound(String endpoint, Duration timeout) {}
}

