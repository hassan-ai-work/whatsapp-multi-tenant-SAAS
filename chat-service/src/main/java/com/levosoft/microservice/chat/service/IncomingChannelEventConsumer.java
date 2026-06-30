package com.levosoft.microservice.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.levosoft.microservice.chat.config.ChatProperties;
import com.levosoft.microservice.chat.dto.ChannelEventPayload;
import com.levosoft.microservice.chat.service.deadletter.DeadLetterPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class IncomingChannelEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(IncomingChannelEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final ConversationOrchestrationService orchestrationService;
    private final ChatProperties chatProperties;
    private final DeadLetterPublisher deadLetterPublisher;

    public IncomingChannelEventConsumer(ObjectMapper objectMapper,
                                        ConversationOrchestrationService orchestrationService,
                                        ChatProperties chatProperties,
                                        DeadLetterPublisher deadLetterPublisher) {
        this.objectMapper = objectMapper;
        this.orchestrationService = orchestrationService;
        this.chatProperties = chatProperties;
        this.deadLetterPublisher = deadLetterPublisher;
    }

    @KafkaListener(topics = "${chat.kafka.incoming-topic}", groupId = "${chat.kafka.group-id}")
    public void consume(String eventJson) {
        try {
            ChannelEventPayload payload = objectMapper.readValue(eventJson, ChannelEventPayload.class);
            orchestrationService.process(payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse incoming channel event", ex);
            deadLetterPublisher.publish(chatProperties.kafka().incomingTopic(), eventJson, ex);
        } catch (RuntimeException ex) {
            log.error("Incoming channel event processing failed", ex);
            deadLetterPublisher.publish(chatProperties.kafka().incomingTopic(), eventJson, ex);
        }
    }
}

