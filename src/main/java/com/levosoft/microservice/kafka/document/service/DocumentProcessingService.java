package com.levosoft.microservice.kafka.document.service;

import com.levosoft.microservice.kafka.document.dto.DocumentIngestionEvent;
import com.levosoft.microservice.kafka.document.model.Document;
import com.levosoft.microservice.kafka.document.model.DocumentChunk;
import com.levosoft.microservice.kafka.document.model.DocumentStatus;
import com.levosoft.microservice.kafka.document.repository.DocumentChunkRepository;
import com.levosoft.microservice.kafka.document.repository.DocumentRepository;
import com.levosoft.microservice.kafka.storage.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final String CHUNK_STRATEGY = "fixed-window";
    private static final String DOCUMENT_CHUNKS_IDEMPOTENCY_CONSTRAINT = "ux_document_chunks_idempotency";
    private static final Duration EMBEDDING_TIMEOUT = Duration.ofSeconds(300);
    private static final int EMBEDDING_DIMENSION = 1536;

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentStorageService documentStorageService;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    @Value("${app.embedding.provider:ollama}")
    private String embeddingProvider;

    @Value("${app.embedding.fallback-enabled:true}")
    private boolean embeddingFallbackEnabled;

    private volatile boolean fallbackEmbeddingWarningLogged;
    private volatile boolean noProviderMatchWarningLogged;
    private volatile boolean embeddingDimensionAdjustmentWarningLogged;
    private final Tika tika = new Tika();

    // Use lightweight Java 21 Virtual Threads for non-blocking concurrent requests
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @KafkaListener(topics = "${app.kafka.topics.document-ingestion}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(DocumentIngestionEvent event) {
        log.info(
                "Received document ingestion event. documentId={}, tenantId={}, businessId={}",
                event.documentId(),
                event.tenantId(),
                event.businessId()
        );
        try {
            process(event);
        } catch (Exception ex) {
            log.error("Document ingestion consumer failed framework boundary loop. documentId={}", event.documentId(), ex);
            markDocumentFailed(event.documentId(), ex.getMessage());
        }
    }

    public void process(DocumentIngestionEvent event) throws Exception {
        OffsetDateTime processingStartedAt = OffsetDateTime.now();

        // Database Optimization: Fetch the document once at the start of execution
        Document document = documentRepository.findById(event.documentId())
                .orElseThrow(() -> new IllegalStateException("Document not found: " + event.documentId()));

        try (InputStream inputStream = documentStorageService.download(event.bucket(), event.s3Key())) {
            String text = tika.parseToString(inputStream);
            List<TextChunk> chunks = chunkText(text);

            updateDocumentMetadata(document, event, text, chunks.size(), processingStartedAt, "PROCESSING", null);

            // Execute all chunk embedding requests concurrently in parallel
            List<CompletableFuture<Void>> parallelTasks = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                final int chunkIndex = i;
                TextChunk chunk = chunks.get(i);
                String chunkHash = sha256(chunk.content());

                CompletableFuture<Void> task = CompletableFuture.supplyAsync(() -> {
                            try {
                                return embed(chunk.content());
                            } catch (Exception e) {
                                throw new RuntimeException("Embedding failure at index: " + chunkIndex, e);
                            }
                        }, virtualThreadExecutor)
                        .thenAccept(embedding -> {
                            Map<String, Object> metadata = new LinkedHashMap<>();
                            metadata.put("chunkIndex", chunkIndex);
                            metadata.put("chunkStart", chunk.start());
                            metadata.put("chunkEnd", chunk.end());
                            metadata.put("chunkHash", chunkHash);

                            DocumentChunk documentChunk = new DocumentChunk();
                            documentChunk.setTenantId(event.tenantId());
                            documentChunk.setBusinessId(event.businessId());
                            documentChunk.setDocumentId(event.documentId());
                            documentChunk.setContent(chunk.content());
                            documentChunk.setEmbedding(toPgVector(embedding));
                            documentChunk.setMetadata(metadata);
                            documentChunk.setCreatedAt(OffsetDateTime.now());

                            saveChunkIdempotently(
                                    documentChunk,
                                    event.documentId(),
                                    event.tenantId(),
                                    event.businessId(),
                                    chunkIndex,
                                    chunkHash
                            );
                        });

                parallelTasks.add(task);
            }

            // Await complete parallel generation non-blockingly together
            CompletableFuture.allOf(parallelTasks.toArray(new CompletableFuture[0]))
                    .get(EMBEDDING_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            markDocumentProcessed(document, processingStartedAt);
            log.info(
                    "Document processing completed. documentId={}, tenantId={}, businessId={}, chunkCount={}",
                    event.documentId(),
                    event.tenantId(),
                    event.businessId(),
                    chunks.size()
            );
        } catch (Exception ex) {
            markDocumentFailed(event.documentId(), ex.getMessage());
            throw ex;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveChunkIdempotently(
            DocumentChunk documentChunk,
            Long documentId,
            Long tenantId,
            Long businessId,
            int chunkIndex,
            String chunkHash
    ) {
        try {
            documentChunkRepository.saveAndFlush(documentChunk);
        } catch (DataIntegrityViolationException ex) {
            if (isIdempotentDuplicate(ex)) {
                log.info(
                        "Duplicate chunk ignored during Kafka redelivery. documentId={}, tenantId={}, businessId={}, chunkIndex={}, chunkHash={}",
                        documentId,
                        tenantId,
                        businessId,
                        chunkIndex,
                        chunkHash
                );
                return;
            }
            throw ex;
        }
    }

    private boolean isIdempotentDuplicate(DataIntegrityViolationException ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(DOCUMENT_CHUNKS_IDEMPOTENCY_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private List<Double> embed(String content) {
        EmbeddingModel embeddingModel = selectEmbeddingModel();
        if (embeddingModel == null) {
            if (embeddingFallbackEnabled) {
                if (!fallbackEmbeddingWarningLogged) {
                    log.warn("No EmbeddingModel bean configured. Falling back to deterministic hash embedding.");
                    fallbackEmbeddingWarningLogged = true;
                }
                return deterministicFallbackEmbedding(content);
            }
            throw new IllegalStateException("No EmbeddingModel bean configured");
        }

        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(List.of(content), null));
        float[] output = response.getResults().getFirst().getOutput();

        List<Double> values = new ArrayList<>(output.length);
        for (float val : output) {
            values.add((double) val);
        }
        return normalizeEmbeddingDimensions(values);
    }

    private EmbeddingModel selectEmbeddingModel() {
        List<EmbeddingModel> candidates = embeddingModelProvider.orderedStream().toList();
        if (candidates.isEmpty()) return null;

        String provider = embeddingProvider == null ? "" : embeddingProvider.trim().toLowerCase(Locale.ROOT);
        if (provider.isBlank()) return candidates.getFirst();

        return candidates.stream()
                .filter(model -> model.getClass().getName().toLowerCase(Locale.ROOT).contains(provider))
                .findFirst()
                .orElseGet(() -> {
                    if (!noProviderMatchWarningLogged) {
                        log.warn("No EmbeddingModel bean matched provider='{}', using fallback first available.", provider);
                        noProviderMatchWarningLogged = true;
                    }
                    return candidates.getFirst();
                });
    }

    private List<Double> normalizeEmbeddingDimensions(List<Double> rawEmbedding) {
        if (rawEmbedding.size() == EMBEDDING_DIMENSION) {
            return rawEmbedding;
        }

        if (!embeddingDimensionAdjustmentWarningLogged) {
            log.warn("Embedding dimension mismatch detected. expected={}, actual={}.", EMBEDDING_DIMENSION, rawEmbedding.size());
            embeddingDimensionAdjustmentWarningLogged = true;
        }

        // Fast Fixed Array Allocation Workaround
        Double[] normalizedArray = new Double[EMBEDDING_DIMENSION];
        Arrays.fill(normalizedArray, 0.0d);

        int copyLength = Math.min(rawEmbedding.size(), EMBEDDING_DIMENSION);
        for (int i = 0; i < copyLength; i++) {
            normalizedArray[i] = rawEmbedding.get(i);
        }

        return Arrays.asList(normalizedArray);
    }

    private List<Double> deterministicFallbackEmbedding(String content) {
        Double[] values = new Double[EMBEDDING_DIMENSION];
        byte[] previous = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        int index = 0;

        while (index < EMBEDDING_DIMENSION) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                previous = digest.digest(previous);
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to generate fallback embedding", ex);
            }

            for (byte b : previous) {
                if (index >= EMBEDDING_DIMENSION) break;
                values[index++] = (double) b / 128.0d;
            }
        }
        return Arrays.asList(values);
    }

    private String toPgVector(List<Double> embedding) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (Double val : embedding) {
            joiner.add(val.toString());
        }
        return joiner.toString();
    }

    private List<TextChunk> chunkText(String text) {
        String normalized = text == null ? "" : text.trim();
        List<TextChunk> chunks = new ArrayList<>();
        if (normalized.isEmpty()) {
            chunks.add(new TextChunk("", 0, 0));
            return chunks;
        }

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            chunks.add(new TextChunk(normalized.substring(start, end), start, end));
            if (end >= normalized.length()) break;
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunks;
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDocumentMetadata(Document document, DocumentIngestionEvent event, String extractedText, int chunkCount, OffsetDateTime processingStartedAt, String ingestionStatus, String failureReason) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (document.getMetadata() != null) metadata.putAll(document.getMetadata());

        Map<String, Object> ingestion = new LinkedHashMap<>();
        Object existingIngestion = metadata.get("ingestion");
        if (existingIngestion instanceof Map<?, ?> existingIngestionMap) {
            existingIngestionMap.forEach((k, v) -> {
                if (k instanceof String) ingestion.put((String) k, v);
            });
        }

        ingestion.put("status", ingestionStatus);
        ingestion.put("processingStartedAt", processingStartedAt.toString());
        ingestion.put("processedAt", OffsetDateTime.now().toString());
        ingestion.put("contentType", event.contentType());
        ingestion.put("source", event.source());

        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("bucket", event.bucket());
        storage.put("s3Key", event.s3Key());
        ingestion.put("storage", storage);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("ingestRequestId", event.ingestRequestId() != null ? event.ingestRequestId().toString() : null);
        ingestion.put("request", request);

        Map<String, Object> extraction = new LinkedHashMap<>();
        extraction.put("status", failureReason == null ? "COMPLETED" : "FAILED");
        extraction.put("textLength", extractedText != null ? extractedText.length() : 0);
        if (failureReason != null && !failureReason.isBlank()) extraction.put("failureReason", failureReason);
        ingestion.put("extraction", extraction);

        Map<String, Object> chunking = new LinkedHashMap<>();
        chunking.put("strategy", CHUNK_STRATEGY);
        chunking.put("chunkSize", CHUNK_SIZE);
        chunking.put("chunkOverlap", CHUNK_OVERLAP);
        chunking.put("chunkCount", chunkCount);
        ingestion.put("chunking", chunking);

        metadata.put("ingestion", ingestion);
        document.setMetadata(metadata);

        if ("PROCESSING".equals(ingestionStatus)) document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDocumentProcessed(Document document, OffsetDateTime processingStartedAt) {
        document.setStatus(DocumentStatus.PROCESSED);
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (document.getMetadata() != null) metadata.putAll(document.getMetadata());

        Map<String, Object> ingestion = new LinkedHashMap<>();
        Object existingIngestion = metadata.get("ingestion");
        if (existingIngestion instanceof Map<?, ?> existingIngestionMap) {
            existingIngestionMap.forEach((k, v) -> {
                if (k instanceof String) ingestion.put((String) k, v);
            });
        }

        ingestion.put("status", "PROCESSED");
        ingestion.put("processingStartedAt", processingStartedAt.toString());
        ingestion.put("processedAt", OffsetDateTime.now().toString());

        metadata.put("ingestion", ingestion);
        document.setMetadata(metadata);
        documentRepository.save(document);
        log.info("Document status successfully updated to PROCESSED. documentId={}", document.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDocumentFailed(Long documentId, String failureReason) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setStatus(DocumentStatus.FAILED);
            Map<String, Object> metadata = new LinkedHashMap<>();
            if (document.getMetadata() != null) metadata.putAll(document.getMetadata());

            Map<String, Object> ingestion = new LinkedHashMap<>();
            Object existingIngestion = metadata.get("ingestion");
            if (existingIngestion instanceof Map<?, ?> existingIngestionMap) {
                existingIngestionMap.forEach((k, v) -> {
                    if (k instanceof String) ingestion.put((String) k, v);
                });
            }

            ingestion.put("status", "FAILED");
            ingestion.put("processedAt", OffsetDateTime.now().toString());

            Map<String, Object> extraction = new LinkedHashMap<>();
            Object existingExtraction = ingestion.get("extraction");
            if (existingExtraction instanceof Map<?, ?> existingExtractionMap) {
                existingExtractionMap.forEach((k, v) -> {
                    if (k instanceof String) extraction.put((String) k, v);
                });
            }
            extraction.put("status", "FAILED");
            if (failureReason != null && !failureReason.isBlank()) extraction.put("failureReason", failureReason);
            ingestion.put("extraction", extraction);

            metadata.put("ingestion", ingestion);
            document.setMetadata(metadata);
            documentRepository.save(document);
            log.warn("Document marked as FAILED. documentId={}, reason={}", document.getId(), failureReason);
        });
    }

    private record TextChunk(String content, int start, int end) {}
}
