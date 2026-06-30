package com.levosoft.microservice.chat.service.messagenormalization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.levosoft.microservice.chat.dto.ChannelEventPayload;
import com.levosoft.microservice.chat.model.ChannelSourceType;

class MessageNormalizationServiceTest {

    @Test
    void normalizeShouldMergeHeadersAndPayloadMetadata() {
        MessageNormalizationService service = new MessageNormalizationService();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-Id", "req-1");
        headers.add("X-Correlation-Id", "corr-1");
        headers.add("X-Customer-Identity", "customer-header");

        ChannelEventPayload payload = new ChannelEventPayload(
                "tenant-1",
                null,
                "hello",
                ChannelSourceType.INTEGRATION,
                Map.of("source_channel", "whatsapp"));

        ChannelEventPayload normalized = service.normalize(payload, headers);

        assertEquals("tenant-1", normalized.tenantIdentity());
        assertEquals("customer-header", normalized.customerIdentity());
        assertEquals("hello", normalized.conversationPayload());
        assertEquals("req-1", normalized.metadata().get("request_id"));
        assertEquals("corr-1", normalized.metadata().get("correlation_id"));
        assertEquals("whatsapp", normalized.metadata().get("source_channel"));
    }
}

