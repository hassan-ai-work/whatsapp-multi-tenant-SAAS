package com.levosoft.microservice.brain.document.dto;

public record DocumentDownloadResponse(
        Long documentId,
        String title,
        String downloadUrl
) {
}
