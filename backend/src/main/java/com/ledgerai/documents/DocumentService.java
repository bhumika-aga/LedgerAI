package com.ledgerai.documents;

import com.ledgerai.clients.ClientService;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.documents.config.DocumentProperties;
import com.ledgerai.documents.domain.Document;
import com.ledgerai.documents.domain.DocumentContent;
import com.ledgerai.documents.domain.DocumentStatus;
import com.ledgerai.documents.dto.DocumentDownloadResponse;
import com.ledgerai.documents.dto.DocumentResponse;
import com.ledgerai.documents.dto.OcrStatusResponse;
import com.ledgerai.documents.port.SignedUrl;
import com.ledgerai.documents.port.StoragePort;
import com.ledgerai.documents.port.StorageUnavailableException;
import com.ledgerai.documents.port.StorageUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Document business rules (SRS §4.4–4.5: FR-UPLD/FR-STOR; VR-005; the SRS §7.1 lifecycle up to
 * {@code UPLOADED}).
 *
 * <p><strong>Ownership.</strong> Documents nest under a client (BR-001), so every operation authorizes
 * via the clients module's published {@link ClientService#requireOwnedByCurrentUser} — the ownership
 * check itself lives in {@code clients} and is not duplicated here (ARCHITECTURE §5.1). A document the
 * caller cannot reach is reported as {@code 404}, never distinguishing "unknown" from "not owned"
 * (SECURITY §5). Deleted documents are excluded from every retrieval path (FR-STOR-005).
 *
 * <p><strong>Storage.</strong> Bytes go to the external store through the {@link StoragePort} (ADR-008),
 * outside any database transaction (DATABASE §11): validate and authorize first, then store, then
 * persist the metadata row. If persistence fails after a successful store, the orphaned object is
 * cleaned up (compensating action, DATABASE §11), and on delete the external file is removed
 * best-effort (API_SPEC §8.4 — SHOULD).
 */
@Service
public class DocumentService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    
    private final DocumentRepository documentRepository;
    private final DocumentContentRepository contentRepository;
    private final ClientService clientService;
    private final StoragePort storagePort;
    private final DocumentFileValidator fileValidator;
    private final DocumentProcessingService processingService;
    private final DocumentProperties properties;
    
    public DocumentService(DocumentRepository documentRepository, DocumentContentRepository contentRepository,
                           ClientService clientService, StoragePort storagePort,
                           DocumentFileValidator fileValidator, DocumentProcessingService processingService,
                           DocumentProperties properties) {
        this.documentRepository = documentRepository;
        this.contentRepository = contentRepository;
        this.clientService = clientService;
        this.storagePort = storagePort;
        this.fileValidator = fileValidator;
        this.processingService = processingService;
        this.properties = properties;
    }
    
    /**
     * FR-UPLD-001: validate (VR-005), store the bytes, then persist the metadata at {@code UPLOADED}
     * (API_SPEC §8.1). Not transactional across the storage call (DATABASE §11); a failed persist rolls
     * back the stored object.
     */
    public DocumentResponse upload(UUID clientId, UploadCommand command) {
        clientService.requireOwnedByCurrentUser(clientId);
        String detectedType = fileValidator.validateAndDetectType(command);
        
        String storageReference = storagePort.store(new StorageUpload(command.content(), detectedType));
        Document saved;
        try {
            // repository.save is atomic on its own; kept outside a wrapping transaction so the storage
            // call above is never held inside a DB transaction (DATABASE §11).
            saved = documentRepository.save(Document.create(
                clientId, command.originalFilename(), detectedType, command.content().length, storageReference));
        } catch (RuntimeException persistFailure) {
            // Compensating cleanup: don't leave an orphaned object behind a failed row (DATABASE §11).
            safelyDeleteObject(storageReference);
            throw persistFailure;
        }
        
        // The upload response reports the initial status (API_SPEC §8.1: UPLOADED), captured before
        // processing runs. Extraction then proceeds synchronously-with-status (ADR-013) using the bytes
        // already in hand — no background worker — and the caller observes the result by polling the
        // OCR-status endpoint (API_SPEC §9.1, §2.11). Processing never throws (it fails the document, not
        // the upload), so the upload always succeeds once the row is stored.
        DocumentResponse response = DocumentResponse.from(saved);
        processingService.process(saved.getId(), command.content(), detectedType);
        return response;
    }
    
    /**
     * FR-CLNT-006 / §8.2: a client's documents, excluding soft-deleted (FR-STOR-005), paged.
     */
    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> list(UUID clientId, DocumentStatus status, Pageable pageable) {
        clientService.requireOwnedByCurrentUser(clientId);
        Page<Document> page;
        if (status == null) {
            page = documentRepository.findByClientIdAndStatusNot(clientId, DocumentStatus.DELETED, pageable);
        } else if (status == DocumentStatus.DELETED) {
            // A deleted document is never returned through any retrieval path (FR-STOR-005).
            page = Page.empty(pageable);
        } else {
            page = documentRepository.findByClientIdAndStatus(clientId, status, pageable);
        }
        return PageResponse.from(page, DocumentResponse::from);
    }
    
    /**
     * §8.3: a single non-deleted document the caller owns.
     */
    @Transactional(readOnly = true)
    public DocumentResponse get(UUID documentId) {
        return DocumentResponse.from(requireOwnedDocument(documentId));
    }
    
    /**
     * §8.5: a short-lived, owner-scoped download reference for a non-deleted, owned document.
     */
    @Transactional(readOnly = true)
    public DocumentDownloadResponse download(UUID documentId) {
        Document document = requireOwnedDocument(documentId);
        if (document.getStorageReference() == null) {
            // No stored object to link to (should not occur once upload completes).
            throw new ResourceNotFoundException();
        }
        SignedUrl signed = storagePort.createDownloadUrl(document.getStorageReference(), properties.downloadUrlTtl());
        return new DocumentDownloadResponse(
            signed.url(), signed.expiresAt(),
            document.getMimeType(), document.getOriginalFilename(), document.getSizeBytes());
    }
    
    /**
     * FR-STOR-004 / §8.4: soft-delete, idempotent. Loads including already-deleted rows (delete is not a
     * retrieval path), authorizes ownership, transitions to {@code DELETED}, and removes the external
     * file best-effort.
     */
    @Transactional
    public void delete(UUID documentId) {
        Document document = documentRepository.findById(documentId).orElseThrow(ResourceNotFoundException::new);
        clientService.requireOwnedByCurrentUser(document.getClientId());
        boolean wasActive = !document.isDeleted();
        document.markDeleted();
        documentRepository.save(document);
        if (wasActive && document.getStorageReference() != null) {
            safelyDeleteObject(document.getStorageReference());
        }
    }
    
    /**
     * API_SPEC §9.1: report extraction/processing status for a non-deleted document the caller owns.
     * The poll endpoint for upload/processing (§2.11); a deleted/unknown/non-owned document is
     * {@code 404} (FR-STOR-005, SECURITY §5).
     */
    @Transactional(readOnly = true)
    public OcrStatusResponse getOcrStatus(UUID documentId) {
        Document document = requireOwnedDocument(documentId);
        DocumentContent content = contentRepository.findByDocumentId(documentId).orElse(null);
        return OcrStatusResponse.of(document, content);
    }
    
    /**
     * Published cross-module access for grounded AI actions (AI Summary): the owned, non-deleted
     * document's lifecycle status and its extracted text, as a {@link DocumentContentView}. Authorizes
     * ownership exactly as every other read path — an unknown, deleted, or non-owned document is
     * {@code 404} (BR-004, SECURITY §5, FR-STOR-005) — so the AI module reuses this module's ownership
     * enforcement rather than duplicating it (CLAUDE.md — never bypass ownership validation). It does
     * <strong>not</strong> enforce the {@code READY} precondition or a non-empty-text rule; that is the
     * AI action's business rule (BR-010, AI_ARCHITECTURE §4), so it is decided by the caller from the
     * returned status/text.
     */
    @Transactional(readOnly = true)
    public DocumentContentView requireOwnedContentForAi(UUID documentId) {
        Document document = requireOwnedDocument(documentId);
        String extractedText = contentRepository.findByDocumentId(documentId)
                                   .map(DocumentContent::getExtractedText)
                                   .orElse(null);
        return new DocumentContentView(document.getId(), document.getStatus(), extractedText);
    }
    
    private Document requireOwnedDocument(UUID documentId) {
        Document document = documentRepository
                                .findByIdAndStatusNot(documentId, DocumentStatus.DELETED)
                                .orElseThrow(ResourceNotFoundException::new);
        clientService.requireOwnedByCurrentUser(document.getClientId());
        return document;
    }
    
    private void safelyDeleteObject(String storageReference) {
        try {
            storagePort.delete(storageReference);
        } catch (StorageUnavailableException e) {
            // Best-effort (SHOULD). The row is authoritative; a reconciliation job can sweep orphans.
            log.warn("Failed to remove object from storage; leaving for later cleanup");
        }
    }
}
