package com.levosoft.microservice.chat.service.inboundvalidation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.levosoft.microservice.chat.dto.ChannelEventPayload;

@Service
public class InboundValidationService {

    private final List<InboundValidator> validators;

    public InboundValidationService(List<InboundValidator> validators) {
        this.validators = validators;
    }

    public void validate(ChannelEventPayload payload) {
        for (InboundValidator validator : validators) {
            validator.validate(payload);
        }
    }
}

