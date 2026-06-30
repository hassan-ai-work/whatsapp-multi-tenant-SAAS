package com.levosoft.microservice.chat.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

public class FallbackEmbeddingModel implements EmbeddingModel {

    private final int dimension;

    public FallbackEmbeddingModel(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<Embedding> embeddings = new ArrayList<>();
        for (TextSegment segment : textSegments) {
            embeddings.add(Embedding.from(toVector(segment == null ? "" : segment.text())));
        }
        return Response.from(embeddings);
    }

    @Override
    public int dimension() {
        return dimension;
    }

    private float[] toVector(String text) {
        float[] vector = new float[dimension];
        if (text == null || text.isBlank()) {
            return vector;
        }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            vector[i % dimension] += (bytes[i] & 0xFF) / 255.0f;
        }
        normalize(vector);
        return vector;
    }

    private void normalize(float[] vector) {
        float sum = 0f;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0f) {
            return;
        }
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}

