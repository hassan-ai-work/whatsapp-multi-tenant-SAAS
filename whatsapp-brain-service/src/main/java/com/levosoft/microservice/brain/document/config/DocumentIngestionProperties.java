package com.levosoft.microservice.brain.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record DocumentIngestionProperties(
        String documentIngestion
) {
}
