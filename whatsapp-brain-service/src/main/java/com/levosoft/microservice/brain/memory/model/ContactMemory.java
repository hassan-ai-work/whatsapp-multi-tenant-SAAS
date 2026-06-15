package com.levosoft.microservice.brain.memory.model;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "contact_memories", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "contact_identifier", nullable = false)
    private String contactIdentifier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contact_chat_text", nullable = false, columnDefinition = "jsonb")
    private String contactChatText;

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    private String embedding;

    @Column(name = "idempotency_hash", nullable = false, unique = true)
    private String idempotencyHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
