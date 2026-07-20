package com.ledgerai.documents;

import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.documents.domain.Document;
import com.ledgerai.documents.domain.DocumentContent;
import com.ledgerai.documents.domain.ExtractionMethod;
import com.ledgerai.documents.domain.ExtractionQuality;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The transactional boundary for document lifecycle writes during OCR processing (DATABASE §11).
 *
 * <p>It is a separate bean on purpose: {@link DocumentProcessingService} orchestrates across an
 * external OCR call (which MUST NOT be held inside a DB transaction — DATABASE §11), and delegates each
 * atomic state change here so it commits in its own transaction through the Spring proxy. Each status
 * transition commits independently, which is exactly what makes intermediate states
 * ({@code PROCESSING}, {@code OCR_PROCESSING}) observable via the OCR-status poll while the pipeline
 * runs. Only {@link #completeReady} is multi-write, and is atomic (document + content together).
 */
@Component
public class DocumentLifecycleWriter {
    
    private final DocumentRepository documentRepository;
    private final DocumentContentRepository contentRepository;
    
    public DocumentLifecycleWriter(DocumentRepository documentRepository,
                                   DocumentContentRepository contentRepository) {
        this.documentRepository = documentRepository;
        this.contentRepository = contentRepository;
    }
    
    @Transactional
    public void markProcessing(UUID documentId) {
        Document document = require(documentId);
        document.markProcessing();
        documentRepository.save(document);
    }
    
    @Transactional
    public void markOcrProcessing(UUID documentId) {
        Document document = require(documentId);
        document.markOcrProcessing();
        documentRepository.save(document);
    }
    
    /**
     * Atomically transitions the document to {@code READY} and persists its extracted content
     * (DATABASE §5.4) — the two are one unit of work, so a document is never {@code READY} without its
     * text, nor text without a {@code READY} document.
     */
    @Transactional
    public void completeReady(UUID documentId, ExtractionMethod method, String text, ExtractionQuality quality) {
        Document document = require(documentId);
        document.markReady(method);
        documentRepository.save(document);
        contentRepository.save(DocumentContent.create(documentId, text, quality));
    }
    
    @Transactional
    public void markFailed(UUID documentId, String reason) {
        Document document = require(documentId);
        document.markFailed(reason);
        documentRepository.save(document);
    }
    
    private Document require(UUID documentId) {
        return documentRepository.findById(documentId).orElseThrow(ResourceNotFoundException::new);
    }
}
