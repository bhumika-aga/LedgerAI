package com.ledgerai.common.exception;

/**
 * The requested resource does not exist <em>or</em> is not visible to the caller.
 *
 * <p>Maps to {@code 404} (BACKEND_CODING_STANDARDS §8, API_SPEC §2.4). The conflation is deliberate
 * and is the product's primary confidentiality control: reporting {@code 403} for a resource the
 * caller does not own would confirm that the id is real, leaking existence. Both cases therefore
 * produce this exception, with an identical message, so they are indistinguishable (SECURITY §5).
 *
 * <p>Callers MUST NOT vary the message per cause — doing so reintroduces the leak this type exists to
 * prevent.
 */
public class ResourceNotFoundException extends LedgerAiException {
    
    public ResourceNotFoundException() {
        super("The requested resource was not found.");
    }
}
