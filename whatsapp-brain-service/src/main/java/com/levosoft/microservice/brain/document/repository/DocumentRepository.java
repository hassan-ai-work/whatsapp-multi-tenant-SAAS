package com.levosoft.microservice.brain.document.repository;

import com.levosoft.microservice.brain.document.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndTenantId(Long id, Long tenantId);
}
