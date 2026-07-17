package com.ledgerai.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * The pagination envelope every collection endpoint returns (API_SPEC §17.9, §2.5):
 * {@code { content, page, size, totalElements, totalPages, hasNext }}.
 *
 * <p>A shared schema — API_SPEC §21 defines it once and references it from every paginated resource —
 * so it lives in {@code common} rather than being redeclared per module. It exposes exactly the
 * documented fields: Spring's own {@code Page} carries extra framework detail that must never reach the
 * wire (API_SPEC §2.12 — "No framework-specific fields are ever included").
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext) {
    
    /**
     * Projects a persistence {@link Page} into the wire envelope, mapping entities to DTOs.
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
            page.getContent().stream().map(mapper).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext());
    }
}
