package com.levosoft.microservice.kafka.document.repository;

import com.levosoft.microservice.kafka.document.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
}
