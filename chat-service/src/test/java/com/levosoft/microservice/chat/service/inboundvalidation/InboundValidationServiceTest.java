package com.levosoft.microservice.chat.service.inboundvalidation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.levosoft.microservice.chat.config.ChatProperties;
import com.levosoft.microservice.chat.dto.ChannelEventPayload;
import com.levosoft.microservice.chat.model.ChannelSourceType;

class InboundValidationServiceTest {

    @Test
    void validateShouldRejectTooLongMessages() {
        ChatProperties properties = testProperties(5);
        InboundValidationService service = new InboundValidationService(List.of(
                new RequiredFieldsInboundValidator(),
                new MessageLengthInboundValidator(properties)
        ));

        ChannelEventPayload payload = new ChannelEventPayload(
                "tenant",
                "customer",
                "123456",
                ChannelSourceType.INTEGRATION,
                Map.of());

        assertThrows(IllegalArgumentException.class, () -> service.validate(payload));
    }

    @Test
    void validateShouldPassForValidMessage() {
        ChatProperties properties = testProperties(20);
        InboundValidationService service = new InboundValidationService(List.of(
                new RequiredFieldsInboundValidator(),
                new MessageLengthInboundValidator(properties)
        ));

        ChannelEventPayload payload = new ChannelEventPayload(
                "tenant",
                "customer",
                "hello",
                ChannelSourceType.INTEGRATION,
                Map.of());

        assertDoesNotThrow(() -> service.validate(payload));
    }

    private ChatProperties testProperties(int maxLength) {
        return new ChatProperties(
                new ChatProperties.Kafka("incoming", "group", "dlq", 1, 0),
                new ChatProperties.Session("session", "snapshot", Duration.ofMinutes(30), Duration.ofDays(7)),
                new ChatProperties.Llm("ollama", "llama3", "http://localhost:11434", ""),
                new ChatProperties.Embedding("openai", "text-embedding-3-small", "https://api.openai.com/v1", "", 1536),
                new ChatProperties.Rag(5, 0.55, "template"),
                new ChatProperties.Inbound(maxLength),
                new ChatProperties.Outbound("http://localhost:8080/internal/channel/egress", Duration.ofSeconds(5))
        );
    }
}

