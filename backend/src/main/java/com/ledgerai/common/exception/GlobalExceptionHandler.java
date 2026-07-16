package com.ledgerai.common.exception;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ledgerai.auth.exception.EmailAlreadyExistsException;
import com.ledgerai.auth.exception.InvalidCredentialsException;
import com.ledgerai.auth.exception.InvalidRefreshTokenException;
import com.ledgerai.auth.exception.WeakPasswordException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Central error handler (API_SPEC §2.12; SRS §8). Every failure surfaced to a client is rendered as an
 * RFC 7807 {@code application/problem+json} document with the exact fields the contract mandates —
 * {@code type}, {@code title}, {@code status}, {@code detail}, {@code instance}, {@code timestamp},
 * {@code traceId}, and (only for 422) {@code validationErrors}. No framework-specific fields or
 * internal details ever leak.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "/problems/email-already-exists",
            "Email already registered", ex.getMessage(), request);
    }
    
    @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
    public ProblemDetail handleUnauthorized(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "/problems/authentication-failed",
            "Authentication failed", ex.getMessage(), request);
    }
    
    @ExceptionHandler(WeakPasswordException.class)
    public ProblemDetail handleWeakPassword(WeakPasswordException ex, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_ENTITY, "/problems/validation-error",
            "Validation failed", "One or more fields are invalid.", request);
        problem.setProperty("validationErrors", List.of(new ValidationError("password", ex.getMessage())));
        return problem;
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                                           .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                                           .toList();
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_ENTITY, "/problems/validation-error",
            "Validation failed", "One or more fields are invalid.", request);
        problem.setProperty("validationErrors", errors);
        return problem;
    }
    
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        // Deliberately generic: no stack traces or internals reach the client (SRS §8).
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "/problems/internal-error",
            "Unexpected error", "An unexpected error occurred. Please try again later.", request);
    }
    
    private ProblemDetail problem(HttpStatus status, String type, String title, String detail,
                                  HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("traceId", traceId());
        return problem;
    }
    
    private String traceId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            Object existing = attributes.getRequest().getAttribute("traceId");
            if (existing instanceof String traceId) {
                return traceId;
            }
        }
        return UUID.randomUUID().toString();
    }
    
    /**
     * A single field-level validation failure (API_SPEC §2.12).
     */
    public record ValidationError(String field, String message) {
    }
}
