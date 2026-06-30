package com.levosoft.microservice.chat.model;

public record DocumentContextChunk(
        String content,
        double score,
        String metadataJson
) {
}

