package com.levosoft.microservice.kafka.document.repository;

import com.levosoft.microservice.kafka.document.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
