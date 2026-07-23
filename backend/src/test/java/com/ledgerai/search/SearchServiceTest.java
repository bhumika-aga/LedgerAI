package com.ledgerai.search;

import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.search.config.SearchProperties;
import com.ledgerai.search.dto.SearchResultResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SearchService} — query validation (VR-006 → 422), owner scoping, sort stripping,
 * and projection→DTO mapping (API_SPEC §14.1). The repository is mocked, so no database is touched.
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {
    
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private final SearchProperties properties = new SearchProperties(256);
    @Mock
    private DocumentSearchRepository searchRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    
    private SearchService service() {
        return new SearchService(searchRepository, currentUserProvider, properties);
    }
    
    private SearchResultProjection projection(String body) {
        return new SearchResultProjection() {
            @Override
            public UUID getDocumentId() {
                return DOC_ID;
            }
            
            @Override
            public UUID getClientId() {
                return CLIENT_ID;
            }
            
            @Override
            public String getTitle() {
                return "statement.pdf";
            }
            
            @Override
            public String getBodyExcerpt() {
                return body;
            }
            
            @Override
            public Long getUpdatedAtEpochMs() {
                return 1_700_000_000_000L;
            }
        };
    }
    
    @Test
    void aBlankQueryReturnsAnEmptyPage() {
        // VR-006 / FR-SRCH-006 / API_SPEC §14.1: an empty query is valid and yields a helpful empty state
        // (an empty page), never a 422. No database round-trip and no user lookup are needed.
        PageResponse<SearchResultResponse> page = service().search("   ", PageRequest.of(0, 20));
        
        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        verifyNoInteractions(searchRepository, currentUserProvider);
    }
    
    @Test
    void aMissingQueryReturnsAnEmptyPage() {
        PageResponse<SearchResultResponse> page = service().search(null, PageRequest.of(0, 20));
        
        assertThat(page.content()).isEmpty();
        verifyNoInteractions(searchRepository, currentUserProvider);
    }
    
    @Test
    void anOversizedQueryIsRejectedWith422() {
        assertThatThrownBy(() -> service().search("x".repeat(257), PageRequest.of(0, 20)))
            .isInstanceOf(ValidationFailedException.class);
        verifyNoInteractions(searchRepository);
    }
    
    @Test
    void searchesOwnerScopedAndStripsAnyIncomingSort() {
        when(currentUserProvider.requireUserId()).thenReturn(USER_ID);
        when(searchRepository.search(eq(USER_ID), eq("invoice"), any()))
            .thenReturn(new PageImpl<>(List.of(projection("The invoice total is 4200."))));
        
        PageResponse<SearchResultResponse> page =
            service().search("  invoice  ", PageRequest.of(0, 20, Sort.by("updatedAt")));
        
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(searchRepository).search(eq(USER_ID), eq("invoice"), pageable.capture());
        // Native full-text query cannot take dynamic sorting; the service strips it (relevance is fixed).
        assertThat(pageable.getValue().getSort().isSorted()).isFalse();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        
        assertThat(page.content()).singleElement().satisfies(hit -> {
            assertThat(hit.documentId()).isEqualTo(DOC_ID);
            assertThat(hit.clientId()).isEqualTo(CLIENT_ID);
            assertThat(hit.title()).isEqualTo("statement.pdf");
            assertThat(hit.matchContext()).contains("invoice");
            assertThat(hit.updatedAt().toEpochMilli()).isEqualTo(1_700_000_000_000L);
        });
    }
    
    @Test
    void aValidQueryWithNoMatchesReturnsAnEmptyPage() {
        when(currentUserProvider.requireUserId()).thenReturn(USER_ID);
        when(searchRepository.search(eq(USER_ID), eq("nothing"), any())).thenReturn(Page.empty());
        
        PageResponse<SearchResultResponse> page = service().search("nothing", PageRequest.of(0, 20));
        
        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }
}
