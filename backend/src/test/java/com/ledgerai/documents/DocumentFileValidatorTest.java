package com.ledgerai.documents;

import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.documents.config.DocumentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Unit tests for upload validation (VR-005, SECURITY §9). The emphasis is that the server detects the
 * type from the file's bytes rather than trusting the client-declared type, and rejects empty,
 * oversized, unsupported, and extension-mismatched files before anything is stored.
 */
class DocumentFileValidatorTest {
    
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46, '-', '1', '.', '4'};
    private static final byte[] PNG_MAGIC =
        {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
    private final DocumentFileValidator validator = new DocumentFileValidator(new DocumentProperties(
        DataSize.ofKilobytes(10),
        List.of("application/pdf", "image/png", "image/jpeg"),
        java.time.Duration.ofMinutes(5)));
    
    private ValidationFailedException reject(UploadCommand command) {
        return catchThrowableOfType(() -> validator.validateAndDetectType(command), ValidationFailedException.class);
    }
    
    @Test
    void detectsPdfFromItsBytes() {
        String detected = validator.validateAndDetectType(
            new UploadCommand("statement.pdf", "application/pdf", PDF_MAGIC));
        
        assertThat(detected).isEqualTo("application/pdf");
    }
    
    @Test
    void detectsPngAndJpeg() {
        assertThat(validator.validateAndDetectType(new UploadCommand("scan.png", "image/png", PNG_MAGIC)))
            .isEqualTo("image/png");
        assertThat(validator.validateAndDetectType(new UploadCommand("scan.jpg", "image/jpeg", JPEG_MAGIC)))
            .isEqualTo("image/jpeg");
        assertThat(validator.validateAndDetectType(new UploadCommand("scan.jpeg", "image/jpeg", JPEG_MAGIC)))
            .isEqualTo("image/jpeg");
    }
    
    @Test
    void rejectsAnEmptyFile() {
        assertThat(reject(new UploadCommand("empty.pdf", "application/pdf", new byte[0])).getFieldErrors())
            .containsKey("file");
    }
    
    @Test
    void rejectsAnOversizedFile() {
        byte[] big = new byte[(int) DataSize.ofKilobytes(11).toBytes()];
        System.arraycopy(PDF_MAGIC, 0, big, 0, PDF_MAGIC.length);
        
        assertThat(reject(new UploadCommand("big.pdf", "application/pdf", big)).getFieldErrors())
            .containsKey("file");
    }
    
    @Test
    void rejectsAnUnsupportedType() {
        // Looks like plain text — no supported magic signature.
        byte[] text = "hello world".getBytes();
        
        assertThat(reject(new UploadCommand("notes.txt", "text/plain", text)).getFieldErrors())
            .containsKey("file");
    }
    
    @Test
    void doesNotTrustAClientDeclaredTypeThatContradictsTheBytes() {
        // Declared as a PDF but the bytes are a PNG — the detected type (png) governs, and since the
        // extension says .pdf, the extension/content mismatch is rejected.
        assertThat(reject(new UploadCommand("fake.pdf", "application/pdf", PNG_MAGIC)).getFieldErrors())
            .containsKey("file");
    }
    
    @Test
    void rejectsAnExtensionThatDoesNotMatchTheContents() {
        assertThat(reject(new UploadCommand("statement.png", "image/png", PDF_MAGIC)).getFieldErrors())
            .containsKey("file");
    }
    
    @Test
    void rejectsAMissingExtension() {
        assertThat(reject(new UploadCommand("statement", "application/pdf", PDF_MAGIC)).getFieldErrors())
            .containsKey("file");
    }
}
