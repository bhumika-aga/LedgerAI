package com.ledgerai.documents;

import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.documents.config.DocumentProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Enforces VR-005 and the file-upload controls of SECURITY §9 <strong>before</strong> anything is
 * stored: non-empty, within the configured size limit, an allowed type, and an extension consistent
 * with that type. Crucially it does not trust the client-declared content type alone (SECURITY §9): the
 * type is detected from the file's magic bytes, and the detected type is what gets stored
 * ({@code mime_type} is the "detected content type", DATABASE §5.3).
 *
 * <p>The supported set is intentionally small (the configured allow-list of financial-document
 * formats), so byte-signature sniffing is sufficient and needs no external library.
 */
@Component
public class DocumentFileValidator {
    
    private static final String FILE_FIELD = "file";
    
    private final DocumentProperties properties;
    
    public DocumentFileValidator(DocumentProperties properties) {
        this.properties = properties;
    }
    
    /**
     * The validated, server-detected content type to persist and store.
     */
    public String validateAndDetectType(UploadCommand command) {
        Map<String, String> errors = new LinkedHashMap<>();
        byte[] content = command.content();
        
        if (content == null || content.length == 0) {
            errors.put(FILE_FIELD, "The file is empty.");
            throw new ValidationFailedException(errors);
        }
        if (content.length > properties.maxFileSize().toBytes()) {
            errors.put(FILE_FIELD,
                "The file exceeds the maximum size of " + properties.maxFileSize().toMegabytes() + " MB.");
            throw new ValidationFailedException(errors);
        }
        
        Optional<String> detected = detectType(content);
        if (detected.isEmpty() || !properties.allowedMimeTypes().contains(detected.get())) {
            errors.put(FILE_FIELD, "Unsupported file type. Allowed types: "
                                       + String.join(", ", properties.allowedMimeTypes()) + ".");
            throw new ValidationFailedException(errors);
        }
        
        String detectedType = detected.get();
        if (!extensionMatches(command.originalFilename(), detectedType)) {
            errors.put(FILE_FIELD, "The file extension does not match its contents.");
            throw new ValidationFailedException(errors);
        }
        return detectedType;
    }
    
    private Optional<String> detectType(byte[] content) {
        if (startsWith(content, 0x25, 0x50, 0x44, 0x46)) { // %PDF
            return Optional.of("application/pdf");
        }
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) { // PNG
            return Optional.of("image/png");
        }
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) { // JPEG
            return Optional.of("image/jpeg");
        }
        return Optional.empty();
    }
    
    private boolean extensionMatches(String filename, String detectedType) {
        String extension = extensionOf(filename);
        return switch (detectedType) {
            case "application/pdf" -> extension.equals("pdf");
            case "image/png" -> extension.equals("png");
            case "image/jpeg" -> extension.equals("jpg") || extension.equals("jpeg");
            default -> false;
        };
    }
    
    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }
    
    private boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Exposed for callers that need the configured allow-list (e.g. tests, messages).
     */
    public List<String> allowedMimeTypes() {
        return properties.allowedMimeTypes();
    }
}
