package com.ledgerai.documents;

import com.ledgerai.clients.ClientService;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.documents.config.DocumentProperties;
import com.ledgerai.documents.domain.Document;
import com.ledgerai.documents.domain.DocumentStatus;
import com.ledgerai.documents.dto.DocumentDownloadResponse;
import com.ledgerai.documents.dto.DocumentResponse;
import com.ledgerai.documents.port.SignedUrl;
import com.ledgerai.documents.port.StoragePort;
import com.ledgerai.documents.port.StorageUnavailableException;
import com.ledgerai.documents.port.StorageUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the document business rules (SRS §4.4–4.5; VR-005; lifecycle to {@code UPLOADED}).
 * Ownership is delegated to a mocked {@link ClientService} (the published check), and storage to a
 * mocked {@link StoragePort}, so this proves the orchestration: authorize → validate → store → persist,
 * soft-delete, the exclusion of deleted documents, and compensating cleanup.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    
    private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, '-', '1'};
    
    @Mock
    private DocumentRepository documentRepository;
    
    @Mock
    private DocumentContentRepository contentRepository;
    
    @Mock
    private ClientService clientService;
    
    @Mock
    private StoragePort storagePort;
    
    @Mock
    private DocumentProcessingService processingService;
    
    private DocumentService service;
    private UUID clientId;
    
    @BeforeEach
    void setUp() {
        DocumentProperties properties = new DocumentProperties(
            DataSize.ofMegabytes(25), List.of("application/pdf", "image/png", "image/jpeg"),
            Duration.ofMinutes(5), 16);
        service = new DocumentService(
            documentRepository, contentRepository, clientService, storagePort,
            new DocumentFileValidator(properties), processingService, properties);
        clientId = UUID.randomUUID();
    }
    
    private UploadCommand pdfUpload() {
        return new UploadCommand("statement.pdf", "application/pdf", PDF);
    }
    
    private Document storedDocument() {
        return Document.create(clientId, "statement.pdf", "application/pdf", PDF.length, "ref-123");
    }
    
    @Test
    void uploadAuthorizesValidatesStoresThenPersists() {
        when(storagePort.store(any(StorageUpload.class))).thenReturn("ref-123");
        when(documentRepository.save(any(Document.class))).thenAnswer(call -> call.getArgument(0));
        
        DocumentResponse response = service.upload(clientId, pdfUpload());
        
        // The response reports the initial status (API_SPEC §8.1: UPLOADED), captured before processing.
        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(response.status()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(response.mimeType()).isEqualTo("application/pdf");
        assertThat(response.sizeBytes()).isEqualTo(PDF.length);
        // Ownership checked before storing; storage happens before persistence; processing is triggered
        // synchronously after the row is stored (ADR-013).
        verify(clientService).requireOwnedByCurrentUser(clientId);
        verify(storagePort).store(any(StorageUpload.class));
        verify(documentRepository).save(any(Document.class));
        verify(processingService).process(any(UUID.class), eq(PDF), eq("application/pdf"));
    }
    
    @Test
    void uploadRejectsANonOwnedClientBeforeStoringAnything() {
        doThrow(new ResourceNotFoundException()).when(clientService).requireOwnedByCurrentUser(clientId);
        
        assertThatThrownBy(() -> service.upload(clientId, pdfUpload()))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(storagePort, never()).store(any());
        verify(documentRepository, never()).save(any());
    }
    
    @Test
    void uploadRejectsAnInvalidFileBeforeStoring() {
        assertThatThrownBy(() -> service.upload(clientId,
            new UploadCommand("notes.txt", "text/plain", "plain".getBytes())))
            .isInstanceOf(com.ledgerai.common.exception.ValidationFailedException.class);
        verify(storagePort, never()).store(any());
    }
    
    @Test
    void uploadCleansUpTheStoredObjectIfPersistenceFails() {
        when(storagePort.store(any(StorageUpload.class))).thenReturn("ref-123");
        when(documentRepository.save(any(Document.class))).thenThrow(new RuntimeException("db down"));
        
        assertThatThrownBy(() -> service.upload(clientId, pdfUpload())).isInstanceOf(RuntimeException.class);
        // Compensating cleanup — no orphaned object left behind (DATABASE §11).
        verify(storagePort).delete("ref-123");
    }
    
    @Test
    void listExcludesDeletedByDefault() {
        Pageable pageable = PageRequest.of(0, 20);
        when(documentRepository.findByClientIdAndStatusNot(clientId, DocumentStatus.DELETED, pageable))
            .thenReturn(new PageImpl<>(List.of(storedDocument()), pageable, 1));
        
        PageResponse<DocumentResponse> page = service.list(clientId, null, pageable);
        
        assertThat(page.content()).hasSize(1);
        verify(documentRepository).findByClientIdAndStatusNot(clientId, DocumentStatus.DELETED, pageable);
    }
    
    @Test
    void listNeverReturnsDeletedDocumentsEvenWhenFilteredForThem() {
        Pageable pageable = PageRequest.of(0, 20);
        
        PageResponse<DocumentResponse> page = service.list(clientId, DocumentStatus.DELETED, pageable);
        
        // FR-STOR-005: a deleted document is never returned; no repository call yields them.
        assertThat(page.content()).isEmpty();
        verify(documentRepository, never()).findByClientIdAndStatus(any(), eq(DocumentStatus.DELETED), any());
        verify(documentRepository, never()).findByClientIdAndStatusNot(any(), any(), any());
    }
    
    @Test
    void listRequiresClientOwnership() {
        doThrow(new ResourceNotFoundException()).when(clientService).requireOwnedByCurrentUser(clientId);
        
        assertThatThrownBy(() -> service.list(clientId, null, PageRequest.of(0, 20)))
            .isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void getReturnsAnOwnedNonDeletedDocument() {
        Document document = storedDocument();
        when(documentRepository.findByIdAndStatusNot(document.getId(), DocumentStatus.DELETED))
            .thenReturn(Optional.of(document));
        
        assertThat(service.get(document.getId()).originalFilename()).isEqualTo("statement.pdf");
        verify(clientService).requireOwnedByCurrentUser(clientId);
    }
    
    @Test
    void getTreatsADeletedOrUnknownDocumentAsNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findByIdAndStatusNot(id, DocumentStatus.DELETED)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void getRejectsANonOwnedDocumentAsNotFound() {
        Document document = storedDocument();
        when(documentRepository.findByIdAndStatusNot(document.getId(), DocumentStatus.DELETED))
            .thenReturn(Optional.of(document));
        doThrow(new ResourceNotFoundException()).when(clientService).requireOwnedByCurrentUser(clientId);
        
        assertThatThrownBy(() -> service.get(document.getId())).isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void downloadReturnsASignedLinkPlusFileMetadata() {
        Document document = storedDocument();
        Instant expiresAt = Instant.now().plusSeconds(300);
        when(documentRepository.findByIdAndStatusNot(document.getId(), DocumentStatus.DELETED))
            .thenReturn(Optional.of(document));
        when(storagePort.createDownloadUrl(eq("ref-123"), any(Duration.class)))
            .thenReturn(new SignedUrl("https://storage.test/download/ref-123", expiresAt));
        
        DocumentDownloadResponse response = service.download(document.getId());
        
        assertThat(response.downloadUrl()).isEqualTo("https://storage.test/download/ref-123");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        assertThat(response.mimeType()).isEqualTo("application/pdf");
        assertThat(response.originalFilename()).isEqualTo("statement.pdf");
        assertThat(response.sizeBytes()).isEqualTo(PDF.length);
    }
    
    @Test
    void downloadSurfacesStorageFailure() {
        Document document = storedDocument();
        when(documentRepository.findByIdAndStatusNot(document.getId(), DocumentStatus.DELETED))
            .thenReturn(Optional.of(document));
        when(storagePort.createDownloadUrl(any(), any()))
            .thenThrow(new StorageUnavailableException("down", null));
        
        assertThatThrownBy(() -> service.download(document.getId()))
            .isInstanceOf(StorageUnavailableException.class);
    }
    
    @Test
    void deleteSoftDeletesAndRemovesTheObject() {
        Document document = storedDocument();
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(call -> call.getArgument(0));
        
        service.delete(document.getId());
        
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.DELETED);
        assertThat(document.getDeletedAt()).isNotNull();
        verify(clientService).requireOwnedByCurrentUser(clientId);
        verify(storagePort).delete("ref-123");
    }
    
    @Test
    void deleteIsIdempotentAndDoesNotReDeleteTheObject() {
        Document document = storedDocument();
        document.markDeleted();
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(call -> call.getArgument(0));
        
        service.delete(document.getId());
        
        // Already deleted: still succeeds, but the object is not removed a second time.
        verify(storagePort, never()).delete(any());
    }
    
    @Test
    void deleteTreatsAnUnknownDocumentAsNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
        verify(storagePort, never()).delete(any());
    }
    
    @Test
    void deleteSucceedsEvenIfStorageCleanupFails() {
        Document document = storedDocument();
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(call -> call.getArgument(0));
        doThrow(new StorageUnavailableException("down", null)).when(storagePort).delete("ref-123");
        
        service.delete(document.getId());
        
        // The row is authoritative; a failed best-effort cleanup does not fail the delete.
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.DELETED);
        verify(storagePort, times(1)).delete("ref-123");
    }
}
