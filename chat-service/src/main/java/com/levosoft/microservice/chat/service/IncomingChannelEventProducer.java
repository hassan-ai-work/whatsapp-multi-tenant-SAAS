package com.levosoft.microservice.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.levosoft.microservice.chat.config.ChatProperties;
import com.levosoft.microservice.chat.dto.ChannelEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IncomingChannelEventProducer {

    private static final Logger log = LoggerFactory.getLogger(IncomingChannelEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ChatProperties chatProperties;

    public IncomingChannelEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                                       ObjectMapper objectMapper,
                                       ChatProperties chatProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.chatProperties = chatProperties;
    }

    public void publish(ChannelEventPayload payload) {
        try {
            String eventJson = objectMapper.writeValueAsString(payload);
            log.info("Incoming channel event queued tenant={} customer={} topic={}",
                    payload.tenantIdentity(), payload.customerIdentity(), chatProperties.kafka().incomingTopic());
            kafkaTemplate.send(chatProperties.kafka().incomingTopic(), payload.customerIdentity(), eventJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize channel event", ex);
        }
    }
}

