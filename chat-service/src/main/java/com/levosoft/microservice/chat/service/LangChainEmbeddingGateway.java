package com.levosoft.microservice.chat.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import com.levosoft.microservice.chat.config.ChatProperties;

@Component
public class LangChainEmbeddingGateway implements EmbeddingGateway {

    private final EmbeddingModel embeddingModel;
    private final ChatProperties chatProperties;

    public LangChainEmbeddingGateway(EmbeddingModel embeddingModel, ChatProperties chatProperties) {
        this.embeddingModel = embeddingModel;
        this.chatProperties = chatProperties;
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[chatProperties.embedding().dimensions()];
        }
        Embedding embedding = embeddingModel.embed(text).content();
        float[] vector = embedding.vector();
        if (vector.length != chatProperties.embedding().dimensions()) {
            throw new IllegalStateException("Embedding dimension mismatch. Expected "
                    + chatProperties.embedding().dimensions() + " but got " + vector.length);
        }
        return vector;
    }
}

