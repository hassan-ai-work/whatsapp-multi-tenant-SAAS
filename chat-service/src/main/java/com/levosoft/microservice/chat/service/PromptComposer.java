package com.levosoft.microservice.chat.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.levosoft.microservice.chat.config.ChatProperties;
import com.levosoft.microservice.chat.dto.ChannelEventPayload;
import com.levosoft.microservice.chat.model.DocumentContextChunk;

import dev.langchain4j.model.input.PromptTemplate;

@Component
public class PromptComposer {

    private final PromptTemplate promptTemplate;

    public PromptComposer(ChatProperties chatProperties) {
        this.promptTemplate = PromptTemplate.from(chatProperties.rag().promptTemplate());
    }

    public String compose(ChannelEventPayload payload, String chatHistory, List<DocumentContextChunk> chunks) {
        Map<String, Object> values = new HashMap<>();
        values.put("tenant_identity", payload.tenantIdentity());
        values.put("customer_identity", payload.customerIdentity());
        values.put("document_context_chunks", formatChunks(chunks));
        values.put("chat_history_from_redis", chatHistory == null ? "" : chatHistory);
        values.put("current_customer_message", payload.conversationPayload());
        return promptTemplate.apply(values).text();
    }

    private String formatChunks(List<DocumentContextChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        return chunks.stream()
                .map(chunk -> "- score=" + chunk.score() + " :: " + chunk.content())
                .collect(Collectors.joining(System.lineSeparator()));
    }
}

