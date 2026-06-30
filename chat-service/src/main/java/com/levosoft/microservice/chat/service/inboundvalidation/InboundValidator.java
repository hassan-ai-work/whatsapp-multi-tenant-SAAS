package com.levosoft.microservice.chat.service.inboundvalidation;

import com.levosoft.microservice.chat.dto.ChannelEventPayload;

public interface InboundValidator {

    void validate(ChannelEventPayload payload);
}

