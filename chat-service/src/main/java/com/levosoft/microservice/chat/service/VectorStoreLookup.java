package com.levosoft.microservice.chat.service;

import java.util.List;

import com.levosoft.microservice.chat.model.DocumentContextChunk;

public interface VectorStoreLookup {
    List<DocumentContextChunk> lookup(String tenantIdentity, float[] queryVector, int limit);
}

