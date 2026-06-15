package com.levosoft.microservice.brain.document.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DocumentIngestionProperties.class)
public class DocumentIngestionConfig {
}
