package com.ledgerai.documents;

import com.ledgerai.documents.config.DocumentProperties;
import com.ledgerai.documents.domain.ExtractionMethod;
import com.ledgerai.documents.domain.ExtractionQuality;
import com.ledgerai.documents.port.OcrPort;
import com.ledgerai.documents.port.OcrResult;
import com.ledgerai.documents.port.OcrUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the OCR Processing pipeline (SRS §4.6, §7.1; ADR-009). The lifecycle writer, native
 * extractor, and OCR port are mocked, so this pins the <strong>orchestration</strong>: native-first
 * routing, the exact state-transition sequence, OCR fallback, and that every failure mode ends in
 * {@code FAILED} without the pipeline throwing.
 */
@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {
    
    private static final byte[] PDF_BYTES = {0x25, 0x50, 0x44, 0x46};
    private static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47};
    
    @Mock
    private DocumentLifecycleWriter lifecycleWriter;
    
    @Mock
    private NativeTextExtractor nativeTextExtractor;
    
    @Mock
    private OcrPort ocrPort;
    
    private DocumentProcessingService service;
    private UUID documentId;
    
    @BeforeEach
    void setUp() {
        DocumentProperties properties = new DocumentProperties(
            DataSize.ofMegabytes(25), List.of("application/pdf", "image/png", "image/jpeg"),
            Duration.ofMinutes(5), 16);
        service = new DocumentProcessingService(lifecycleWriter, nativeTextExtractor, ocrPort, properties);
        documentId = UUID.randomUUID();
    }
    
    @Test
    void nativePathReadyWhenPdfHasSufficientEmbeddedText() {
        when(nativeTextExtractor.extractText(PDF_BYTES)).thenReturn("This PDF has plenty of selectable text.");
        
        service.process(documentId, PDF_BYTES, "application/pdf");
        
        // UPLOADED→PROCESSING then straight to READY(NATIVE, HIGH); OCR never touched (ADR-009).
        InOrder order = inOrder(lifecycleWriter);
        order.verify(lifecycleWriter).markProcessing(documentId);
        order.verify(lifecycleWriter).completeReady(eq(documentId), eq(ExtractionMethod.NATIVE),
            eq("This PDF has plenty of selectable text."), eq(ExtractionQuality.HIGH));
        verify(lifecycleWriter, never()).markOcrProcessing(any());
        verifyNoInteractions(ocrPort);
    }
    
    @Test
    void fallsBackToOcrWhenPdfHasInsufficientNativeText() {
        when(nativeTextExtractor.extractText(PDF_BYTES)).thenReturn("  short  ");
        when(ocrPort.extract(PDF_BYTES, "application/pdf"))
            .thenReturn(new OcrResult("OCR text from an image-only PDF", ExtractionQuality.HIGH));
        
        service.process(documentId, PDF_BYTES, "application/pdf");
        
        // PROCESSING → OCR_PROCESSING → READY(OCR).
        InOrder order = inOrder(lifecycleWriter, ocrPort);
        order.verify(lifecycleWriter).markProcessing(documentId);
        order.verify(lifecycleWriter).markOcrProcessing(documentId);
        order.verify(ocrPort).extract(PDF_BYTES, "application/pdf");
        order.verify(lifecycleWriter).completeReady(documentId, ExtractionMethod.OCR,
            "OCR text from an image-only PDF", ExtractionQuality.HIGH);
    }
    
    @Test
    void imagesGoStraightToOcrWithoutNativeExtraction() {
        when(ocrPort.extract(PNG_BYTES, "image/png"))
            .thenReturn(new OcrResult("Scanned text", ExtractionQuality.LOW));
        
        service.process(documentId, PNG_BYTES, "image/png");
        
        verifyNoInteractions(nativeTextExtractor);
        verify(lifecycleWriter).markOcrProcessing(documentId);
        verify(lifecycleWriter).completeReady(documentId, ExtractionMethod.OCR, "Scanned text", ExtractionQuality.LOW);
    }
    
    @Test
    void failsWhenOcrProducesNoText() {
        when(ocrPort.extract(PNG_BYTES, "image/png")).thenReturn(new OcrResult("   ", ExtractionQuality.UNKNOWN));
        
        service.process(documentId, PNG_BYTES, "image/png");
        
        // OCR_PROCESSING → FAILED (FR-OCR-005); no content persisted.
        verify(lifecycleWriter).markOcrProcessing(documentId);
        verify(lifecycleWriter).markFailed(eq(documentId), any());
        verify(lifecycleWriter, never()).completeReady(any(), any(), any(), any());
    }
    
    @Test
    void failsWhenOcrProviderIsUnavailable() {
        when(ocrPort.extract(PNG_BYTES, "image/png"))
            .thenThrow(new OcrUnavailableException("provider down", null));
        
        service.process(documentId, PNG_BYTES, "image/png");
        
        verify(lifecycleWriter).markFailed(eq(documentId), eq("provider down"));
        verify(lifecycleWriter, never()).completeReady(any(), any(), any(), any());
    }
    
    @Test
    void failsSafelyOnAnUnexpectedError() {
        when(ocrPort.extract(PNG_BYTES, "image/png")).thenThrow(new RuntimeException("boom"));
        
        // Never propagates — the document fails, not the request (graceful degradation).
        service.process(documentId, PNG_BYTES, "image/png");
        
        verify(lifecycleWriter).markFailed(eq(documentId), any());
    }
}
