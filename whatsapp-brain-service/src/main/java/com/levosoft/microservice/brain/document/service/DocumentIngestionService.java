package com.levosoft.microservice.brain.document.service;

import com.levosoft.microservice.brain.document.config.DocumentIngestionProperties;
import com.levosoft.microservice.brain.document.dto.DocumentIngestAcceptedResponse;
import com.levosoft.microservice.brain.document.dto.DocumentIngestionEvent;
import com.levosoft.microservice.brain.document.model.Document;
import com.levosoft.microservice.brain.document.model.DocumentStatus;
import com.levosoft.microservice.brain.document.repository.DocumentRepository;
import com.levosoft.microservice.brain.storage.service.DocumentStorageService;
import com.levosoft.microservice.brain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionService {

    private static final String SAFE_ACCEPTED_MESSAGE = "Your data will be updated shortly.";

    private final DocumentRepository documentRepository;
    private final TenantRepository tenantRepository;
    private final DocumentStorageService documentStorageService;
    private final KafkaTemplate<String, DocumentIngestionEvent> kafkaTemplate;
    private final DocumentIngestionProperties properties;

    public DocumentIngestAcceptedResponse ingest(
            String authenticatedUsername,
            MultipartFile file,
            String title
    ) {
        validateRequest(authenticatedUsername, file, title);

        String normalizedUsername = authenticatedUsername.trim().toLowerCase();
        String normalizedTitle = title.trim();
        Long tenantId = resolveTenantId(normalizedUsername);
        UUID ingestRequestId = UUID.randomUUID();
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String s3Key = buildS3Key(tenantId, originalFilename, ingestRequestId);

        Map<String, Object> documentMetadata = buildDocumentMetadata(
                originalFilename,
                file.getContentType(),
                file.getSize(),
                ingestRequestId
        );

        // 1. Save metadata in its own transaction and commit immediately as PENDING
        Document savedDocument = saveInitialDocumentMetadata(tenantId, normalizedTitle, s3Key, documentMetadata, normalizedUsername, ingestRequestId);

        try {
            // 2. Perform slow network I/O outside of any DB transaction
            documentStorageService.upload(s3Key, file);

            DocumentIngestionEvent event = new DocumentIngestionEvent(
                    savedDocument.getId(),
                    tenantId,
                    normalizedTitle,
                    s3Key,
                    s3Key,
                    documentStorageService.bucket(),
                    normalizedUsername,
                    originalFilename,
                    file.getContentType(),
                    file.getSize(),
                    ingestRequestId,
                    DocumentStatus.PENDING,
                    documentMetadata
            );

            // 3. Block or handle the future to ensure Kafka actually received the event
            kafkaTemplate.send(properties.documentIngestion(), String.valueOf(tenantId), event).get();

            log.info(
                    "Document ingest event published. documentId={}, tenantId={}, ingestRequestId={}, topic={}, s3Key={}",
                    savedDocument.getId(),
                    tenantId,
                    ingestRequestId,
                    properties.documentIngestion(),
                    s3Key
            );

            // SUCCESS MARKER: The ingestion intake flow completed perfectly. Mark as PROCESSED in the DB.
            markDocumentAsProcessed(savedDocument.getId());

            return new DocumentIngestAcceptedResponse(SAFE_ACCEPTED_MESSAGE);
        } catch (Exception ex) {
            // 4. Update status to FAILED in a fresh transaction if S3 or Kafka fails
            markDocumentAsFailed(savedDocument.getId());
            log.error(
                    "Document ingestion failed. documentId={}, tenantId={}, ingestRequestId={}, s3Key={}",
                    savedDocument.getId(),
                    tenantId,
                    ingestRequestId,
                    s3Key,
                    ex
            );
            String detailedMessage = "Document ingestion failed: " + ex.getMessage();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, detailedMessage, ex);
        }
    }

    @Transactional
    public Document saveInitialDocumentMetadata(Long tenantId, String title, String s3Key, Map<String, Object> metadata, String username, UUID ingestRequestId) {
        Document document = new Document();
        document.setTenantId(tenantId);
        document.setTitle(title);
        document.setSource(s3Key);
        document.setStatus(DocumentStatus.PENDING);
        document.setMetadata(metadata);

        Document savedDocument = documentRepository.save(document);

        log.info(
                "Document metadata committed to database. documentId={}, tenantId={}, ingestRequestId={}, username={}",
                savedDocument.getId(), tenantId, ingestRequestId, username
        );
        return savedDocument;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDocumentAsProcessed(Long documentId) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setStatus(DocumentStatus.PROCESSED);
            documentRepository.save(document);
            log.info("Document ingestion stage completed successfully. Status updated to PROCESSED. documentId={}", documentId);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDocumentAsFailed(Long documentId) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            log.warn("Document marked as FAILED during ingest stage. documentId={}, tenantId={}", document.getId(), document.getTenantId());
        });
    }

    private void validateRequest(String authenticatedUsername, MultipartFile file, String title) {
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or blank X-Authenticated-User header");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required multipart field 'file' must not be empty");
        }
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required form field 'title' must not be blank");
        }
    }

    private Long resolveTenantId(String authenticatedUsername) {
        return tenantRepository.findByUsername(authenticatedUsername)
                .map(tenant -> tenant.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found for authenticated user"));
    }

    private String buildS3Key(Long tenantId, String originalFilename, UUID ingestRequestId) {
        return "document-ingestion/" + tenantId + "/" + ingestRequestId + "/" + originalFilename;
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "uploaded-file";
        }
        return originalFilename.replace("\\", "_").replace("/", "_");
    }

    private Map<String, Object> buildDocumentMetadata(String originalFilename, String contentType, long sizeBytes, UUID ingestRequestId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("originalFilename", originalFilename);
        metadata.put("contentType", contentType != null ? contentType : "application/octet-stream");
        metadata.put("sizeBytes", sizeBytes);
        metadata.put("ingestRequestId", ingestRequestId.toString());
        return metadata;
    }
}
