package com.levosoft.microservice.brain.document.controller;

import com.levosoft.microservice.brain.document.dto.DocumentIngestAcceptedResponse;
import com.levosoft.microservice.brain.document.service.DocumentIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/ingest/docs")
@RequiredArgsConstructor
@Validated
@Tag(name = "Document Ingestion", description = "Endpoints for document ingestion")
public class DocumentIngestionController {

    private final DocumentIngestionService documentIngestionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary = "Ingest document",
            description = "Accepts a document upload and queues it for ingestion."
    )
    public DocumentIngestAcceptedResponse ingestDocument(
            @RequestHeader("X-Authenticated-User")
            @Parameter(description = "Authenticated username")
            String authenticatedUser,
            @RequestParam("file")
            @Parameter(description = "Document file to upload", required = true)
            MultipartFile file,
            @RequestParam("title")
            @NotBlank(message = "Title is required")
            @Parameter(description = "Document title", required = true)
            String title
    ) {
        return documentIngestionService.ingest(authenticatedUser, file, title);
    }
}
