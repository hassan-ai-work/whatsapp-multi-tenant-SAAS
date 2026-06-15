package com.levosoft.microservice.kafka.document.dto;

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
        String status,
        Map<String, Object> metadata
) {
}
