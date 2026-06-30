package com.levosoft.microservice.chat.service.inboundvalidation;

import org.springframework.stereotype.Component;

import com.levosoft.microservice.chat.config.ChatProperties;
import com.levosoft.microservice.chat.dto.ChannelEventPayload;

@Component
public class MessageLengthInboundValidator implements InboundValidator {

    private final ChatProperties chatProperties;

    public MessageLengthInboundValidator(ChatProperties chatProperties) {
        this.chatProperties = chatProperties;
    }

    @Override
    public void validate(ChannelEventPayload payload) {
        if (payload == null || payload.conversationPayload() == null) {
            return;
        }
        int maxLength = chatProperties.inbound().maxMessageLength();
        if (maxLength > 0 && payload.conversationPayload().length() > maxLength) {
            throw new IllegalArgumentException("conversationPayload exceeds max length " + maxLength);
        }
    }
}

