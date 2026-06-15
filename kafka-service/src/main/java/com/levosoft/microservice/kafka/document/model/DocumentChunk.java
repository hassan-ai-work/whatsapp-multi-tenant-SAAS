package com.levosoft.microservice.kafka.document.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@SQLInsert(sql = """
        INSERT INTO document_chunks (content, created_at, document_id, embedding, metadata, tenant_id)
        VALUES (?, ?, ?, CAST(? AS vector), CAST(? AS jsonb), ?)
        """)
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    private String embedding;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
