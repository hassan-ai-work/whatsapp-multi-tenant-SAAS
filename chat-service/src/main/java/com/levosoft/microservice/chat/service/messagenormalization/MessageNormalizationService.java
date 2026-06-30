package com.levosoft.microservice.chat.service.messagenormalization;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.levosoft.microservice.chat.dto.ChannelEventPayload;
import com.levosoft.microservice.chat.model.ChannelSourceType;

@Service
public class MessageNormalizationService {

    public ChannelEventPayload normalize(ChannelEventPayload payload, HttpHeaders headers) {
        Map<String, String> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "request_id", headers.getFirst("X-Request-Id"));
        putIfPresent(metadata, "correlation_id", headers.getFirst("X-Correlation-Id"));
        putIfPresent(metadata, "user_agent", headers.getFirst(HttpHeaders.USER_AGENT));
        putIfPresent(metadata, "forwarded_for", headers.getFirst("X-Forwarded-For"));
        if (payload.metadata() != null) {
            metadata.putAll(payload.metadata());
        }

        return new ChannelEventPayload(
                firstText(payload.tenantIdentity(), headers.getFirst("X-Tenant-Identity")),
                firstText(payload.customerIdentity(), headers.getFirst("X-Customer-Identity")),
                firstText(payload.conversationPayload(), headers.getFirst("X-Conversation-Payload")),
                payload.channelSourceType() == null
                        ? ChannelSourceType.fromValue(headers.getFirst("X-Channel-Source-Type"))
                        : payload.channelSourceType(),
                metadata);
    }

    private void putIfPresent(Map<String, String> metadata, String key, String value) {
        if (StringUtils.hasText(value)) {
            metadata.put(key, value.trim());
        }
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary.trim() : (fallback == null ? null : fallback.trim());
    }
}

