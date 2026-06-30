package com.levosoft.microservice.chat.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.levosoft.microservice.chat.model.ChannelSourceType;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record OutboundChannelRequest(
        String tenantIdentity,
        String customerIdentity,
        ChannelSourceType channelSourceType,
        String message,
        String sessionKey,
        Instant dispatchedAt,
        Map<String, String> metadata
) {
}

