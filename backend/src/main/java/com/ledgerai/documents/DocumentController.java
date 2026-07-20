package com.ledgerai.documents;

import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.documents.domain.DocumentStatus;
import com.ledgerai.documents.dto.DocumentDownloadResponse;
import com.ledgerai.documents.dto.DocumentResponse;
import com.ledgerai.documents.dto.OcrStatusResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * The Documents module's endpoints (API_SPEC §8) — the five documented operations and nothing else.
 * Upload and list are nested under the owning client (BR-001); a single document is addressed directly
 * for read/delete/download.
 *
 * <p>Thin by design (BACKEND_CODING_STANDARDS §4): it binds the request and reads the multipart file
 * into a web-free {@link UploadCommand}, then delegates. It never resolves the caller or checks
 * ownership — that is the service's job (ARCHITECTURE §7.1). There is deliberately no update endpoint:
 * API_SPEC §8 defines none.
 */
@RestController
public class DocumentController {
    
    private final DocumentService documentService;
    
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    @PostMapping("/api/v1/clients/{clientId}/documents")
    public ResponseEntity<DocumentResponse> upload(@PathVariable UUID clientId,
                                                   @RequestParam("file") MultipartFile file) {
        UploadCommand command = toCommand(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.upload(clientId, command));
    }
    
    @GetMapping("/api/v1/clients/{clientId}/documents")
    public ResponseEntity<PageResponse<DocumentResponse>> list(
        @PathVariable UUID clientId,
        @RequestParam(required = false) DocumentStatus status,
        @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(documentService.list(clientId, status, pageable));
    }
    
    @GetMapping("/api/v1/documents/{documentId}")
    public ResponseEntity<DocumentResponse> get(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.get(documentId));
    }
    
    @GetMapping("/api/v1/documents/{documentId}/download")
    public ResponseEntity<DocumentDownloadResponse> download(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.download(documentId));
    }
    
    /**
     * API_SPEC §9.1 — the OCR/processing status poll endpoint (there is no user-triggered OCR action;
     * OCR runs automatically during processing, FR-OCR-001).
     */
    @GetMapping("/api/v1/documents/{documentId}/ocr-status")
    public ResponseEntity<OcrStatusResponse> ocrStatus(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.getOcrStatus(documentId));
    }
    
    @DeleteMapping("/api/v1/documents/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID documentId) {
        documentService.delete(documentId);
        return ResponseEntity.noContent().build();
    }
    
    private UploadCommand toCommand(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            // Non-empty file is required (VR-005); surfaced as a field error like every other validation.
            throw new ValidationFailedException(Map.of("file", "A non-empty file is required."));
        }
        try {
            return new UploadCommand(file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new ValidationFailedException(Map.of("file", "The file could not be read."));
        }
    }
}
