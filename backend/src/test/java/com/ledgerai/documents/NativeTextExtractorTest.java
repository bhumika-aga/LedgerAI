package com.ledgerai.documents;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for native (embedded-text) extraction with PDFBox (ADR-009, FR-OCR-002). Real PDFs are
 * generated in-test, so this exercises the actual library, not a mock.
 */
class NativeTextExtractorTest {
    
    private final NativeTextExtractor extractor = new NativeTextExtractor();
    
    private static byte[] pdfWithText(String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(72, 700);
                stream.showText(text);
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
    
    @Test
    void extractsEmbeddedTextFromAPdf() throws Exception {
        String text = extractor.extractText(pdfWithText("Invoice total 1234.56"));
        
        assertThat(text).contains("Invoice total 1234.56");
    }
    
    @Test
    void returnsEmptyForBytesThatAreNotAPdf() {
        assertThat(extractor.extractText(new byte[]{0x00, 0x01, 0x02})).isEmpty();
    }
    
    @Test
    void returnsEmptyForAnEmptyInput() {
        assertThat(extractor.extractText(new byte[0])).isEmpty();
    }
}
