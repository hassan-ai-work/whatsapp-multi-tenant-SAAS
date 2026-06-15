package com.levosoft.microservice.brain.document.service;

import com.levosoft.microservice.brain.document.dto.DocumentDownloadResponse;
import com.levosoft.microservice.brain.document.model.Document;
import com.levosoft.microservice.brain.document.repository.DocumentRepository;
import com.levosoft.microservice.brain.storage.service.DocumentStorageService;
import com.levosoft.microservice.brain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentDownloadService {

    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(15);

    private final DocumentRepository documentRepository;
    private final TenantRepository tenantRepository;
    private final DocumentStorageService documentStorageService;

    public DocumentDownloadResponse generateDownloadUrl(String authenticatedUsername, Long documentId) {
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or blank X-Authenticated-User header");
        }

        if (documentId == null || documentId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document id must be a positive number");
        }

        String normalizedUsername = authenticatedUsername.trim().toLowerCase();

        Long requesterTenantId = tenantRepository.findByUsername(normalizedUsername)
                .map(tenant -> tenant.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found for authenticated user"));

        Document document = documentRepository.findByIdAndTenantId(documentId, requesterTenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        String s3Key = document.getSource();
        if (s3Key == null || s3Key.isBlank()) {
            log.warn(
                    "Document source missing for download request. documentId={}, tenantId={}, username={}",
                    documentId,
                    requesterTenantId,
                    normalizedUsername
            );
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document source not found");
        }

        String downloadUrl = documentStorageService.generatePresignedDownloadUrl(s3Key, DOWNLOAD_URL_TTL).toString();

        log.info(
                "Generated document download URL. documentId={}, tenantId={}, username={}, s3Key={}, ttlSeconds={}",
                document.getId(),
                requesterTenantId,
                normalizedUsername,
                s3Key,
                DOWNLOAD_URL_TTL.getSeconds()
        );

        return new DocumentDownloadResponse(
                document.getId(),
                document.getTitle(),
                downloadUrl
        );
    }
}
