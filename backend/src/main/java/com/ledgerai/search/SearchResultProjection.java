package com.ledgerai.search;

import java.util.UUID;

/**
 * Read projection for a full-text search hit (the columns the native query selects). Deliberately narrow:
 * the identity and title for navigation, a bounded body excerpt from which the service derives the
 * snippet/match-context, and the document's last-updated time as epoch millis.
 *
 * <p>The temporal column is returned as an epoch-millis {@code Long} (not a JDBC temporal type) so the
 * native-query projection binding is type-safe and driver-independent; the service converts it to an
 * {@link java.time.Instant}. Column aliases in the query are quoted to match these property names exactly.
 */
public interface SearchResultProjection {
    
    UUID getDocumentId();
    
    UUID getClientId();
    
    String getTitle();
    
    /**
     * A bounded prefix of the extracted text; the service derives {@code snippet}/{@code matchContext} from it.
     */
    String getBodyExcerpt();
    
    Long getUpdatedAtEpochMs();
}
