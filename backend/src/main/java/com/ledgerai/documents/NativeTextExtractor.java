package com.ledgerai.documents;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Native embedded-text extraction (ADR-009 native-first; FR-OCR-002, BR-014) using Apache PDFBox.
 *
 * <p>In-process and free — no external call. It extracts the selectable text a PDF already contains so
 * OCR can be skipped when the document is not really a scan. A PDF that is actually an image yields
 * little or no text here, which the pipeline detects (against a configured minimum) and routes to OCR.
 * Only PDFs have embedded text; images are never passed here.
 *
 * <p>An unreadable/corrupt PDF returns empty text (not an error) so the pipeline falls through to OCR
 * rather than failing outright.
 */
@Component
public class NativeTextExtractor {
    
    private static final Logger log = LoggerFactory.getLogger(NativeTextExtractor.class);
    
    /**
     * Extracts embedded text from a PDF, or {@code ""} if none is readable.
     */
    public String extractText(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(document);
            return text == null ? "" : text;
        } catch (IOException | RuntimeException e) {
            // Not a usable PDF for native extraction — fall through to OCR.
            log.debug("Native PDF text extraction failed; will fall back to OCR");
            return "";
        }
    }
}
