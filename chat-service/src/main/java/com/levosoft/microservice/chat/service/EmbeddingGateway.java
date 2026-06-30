package com.levosoft.microservice.chat.service;

public interface EmbeddingGateway {
    float[] embed(String text);
}

