package com.ledgerai.auth;

import com.ledgerai.auth.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link UserAccount}. Data access only — no business rules
 * (BACKEND_CODING_STANDARDS §4).
 *
 * <p>Email lookups are case-insensitive (DATABASE §5.1): the {@code email} column is {@code citext}, which
 * enforces case-insensitive <em>uniqueness</em> at write time, but a derived finder binds its argument as
 * {@code varchar}, so PostgreSQL resolves {@code citext = varchar} to a case-<strong>sensitive</strong>
 * comparison on the read path. These queries normalize both sides with {@code lower(...)} so the read path
 * is case-insensitive too, matching the documented behavior.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    
    @Query("select u from UserAccount u where lower(u.email) = lower(:email)")
    Optional<UserAccount> findByEmail(@Param("email") String email);
    
    /**
     * Case-insensitive existence check (DATABASE §5.1), delegating to the case-insensitive
     * {@link #findByEmail} so both paths share one comparison rule and neither re-introduces the
     * {@code citext = varchar} case-sensitivity gap. Registration is the only caller, so loading the row
     * rather than issuing a {@code count} is immaterial.
     */
    default boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
}
