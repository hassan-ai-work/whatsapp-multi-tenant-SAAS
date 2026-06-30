package com.levosoft.microservice.chat.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.levosoft.microservice.chat.model.ChannelSourceType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ChannelEventPayload(
        String tenantIdentity,
        String customerIdentity,
        String conversationPayload,
        ChannelSourceType channelSourceType,
        Map<String, String> metadata
) {
    public ChannelEventPayload {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        channelSourceType = channelSourceType == null ? ChannelSourceType.UNKNOWN : channelSourceType;
    }
}

