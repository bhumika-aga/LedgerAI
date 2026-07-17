package com.ledgerai.auth.domain;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for the profile-edit behavior on the account entity (FR-PROF-002/003, DATABASE §5.1).
 * These cover the rules that are pure Java — partial-update semantics, the audit timestamp, and the
 * opaque preferences blob — independently of the database.
 */
class UserAccountTest {
    
    private static UserAccount newUser() {
        return UserAccount.create("pro@example.com", "hashed", "Ada Pro");
    }
    
    @Test
    void appliesSuppliedFields() {
        UserAccount user = newUser();
        
        user.applyProfileUpdate("Ada Professional", "Chartered Accountant", Map.of("theme", "dark"));
        
        assertThat(user.getFullName()).isEqualTo("Ada Professional");
        assertThat(user.getProfessionalDetails()).isEqualTo("Chartered Accountant");
        assertThat(user.getPreferences()).containsEntry("theme", "dark");
    }
    
    @Test
    void leavesUnsuppliedFieldsUntouched() {
        UserAccount user = newUser();
        user.applyProfileUpdate(null, "Chartered Accountant", Map.of("theme", "dark"));
        
        user.applyProfileUpdate("Only Name", null, null);
        
        assertThat(user.getFullName()).isEqualTo("Only Name");
        assertThat(user.getProfessionalDetails()).isEqualTo("Chartered Accountant");
        assertThat(user.getPreferences()).containsEntry("theme", "dark");
    }
    
    @Test
    void clearsATextFieldWithAnEmptyString() {
        UserAccount user = newUser();
        
        user.applyProfileUpdate("", null, null);
        
        assertThat(user.getFullName()).isEmpty();
    }
    
    @Test
    void acceptsNullValuesInsideTheOpaquePreferencesBlob() {
        UserAccount user = newUser();
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("theme", null);
        
        // {"theme": null} is legal JSON and the blob is opaque, so this must not blow up.
        assertThatCode(() -> user.applyProfileUpdate(null, null, preferences)).doesNotThrowAnyException();
        assertThat(user.getPreferences()).containsEntry("theme", null);
    }
    
    @Test
    void movesUpdatedAtOnlyWhenSomethingChanges() throws Exception {
        UserAccount user = newUser();
        user.applyProfileUpdate("Ada Pro", null, null);
        var unchangedAt = user.getUpdatedAt();
        
        // Re-applying identical values is not a change.
        user.applyProfileUpdate("Ada Pro", null, null);
        assertThat(user.getUpdatedAt()).isEqualTo(unchangedAt);
        
        Thread.sleep(2);
        user.applyProfileUpdate("Someone Else", null, null);
        assertThat(user.getUpdatedAt()).isAfter(unchangedAt);
    }
    
    @Test
    void doesNotExposeItsInternalPreferencesMap() {
        UserAccount user = newUser();
        Map<String, Object> supplied = new HashMap<>();
        supplied.put("theme", "dark");
        user.applyProfileUpdate(null, null, supplied);
        
        // Mutating the caller's map must not reach into the entity.
        supplied.put("theme", "light");
        
        assertThat(user.getPreferences()).containsEntry("theme", "dark");
    }
}
