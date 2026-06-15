package com.levosoft.microservice.kafka.document.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DocumentIngestionProperties.class)
public class DocumentIngestionConfig {
}
