package com.levosoft.microservice.brain.document.dto;

import com.levosoft.microservice.brain.document.model.DocumentStatus;

import java.util.Map;
import java.util.UUID;

public record DocumentIngestionEvent(
        Long documentId,
        Long tenantId,
        String title,
        String source,
        String s3Key,
        String bucket,
        String uploader,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        UUID ingestRequestId,
        DocumentStatus status,
        Map<String, Object> metadata
) {
}
