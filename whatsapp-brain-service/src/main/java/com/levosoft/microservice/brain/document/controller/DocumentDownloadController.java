package com.levosoft.microservice.brain.document.controller;

import com.levosoft.microservice.brain.document.dto.DocumentDownloadResponse;
import com.levosoft.microservice.brain.document.service.DocumentDownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Download", description = "Endpoints for secure document download")
public class DocumentDownloadController {

    private final DocumentDownloadService documentDownloadService;

    @GetMapping("/{documentId}/download")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get secure download URL", description = "Returns a pre-signed URL for downloading the original tenant document")
    public DocumentDownloadResponse downloadDocument(
            @RequestHeader("X-Authenticated-User")
            @Parameter(description = "Authenticated username")
            String authenticatedUser,
            @PathVariable Long documentId
    ) {
        return documentDownloadService.generateDownloadUrl(authenticatedUser, documentId);
    }
}
