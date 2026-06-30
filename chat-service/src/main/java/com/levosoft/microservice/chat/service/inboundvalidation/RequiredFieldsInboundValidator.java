package com.levosoft.microservice.chat.service.inboundvalidation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.levosoft.microservice.chat.dto.ChannelEventPayload;

@Component
public class RequiredFieldsInboundValidator implements InboundValidator {

    @Override
    public void validate(ChannelEventPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Channel event payload is required");
        }
        if (!StringUtils.hasText(payload.tenantIdentity())) {
            throw new IllegalArgumentException("tenantIdentity is required");
        }
        if (!StringUtils.hasText(payload.customerIdentity())) {
            throw new IllegalArgumentException("customerIdentity is required");
        }
        if (!StringUtils.hasText(payload.conversationPayload())) {
            throw new IllegalArgumentException("conversationPayload is required");
        }
    }
}

