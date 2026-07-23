package com.ledgerai.reports.dto;

/**
 * Body for {@code PATCH …/reports/{reportId}} (API_SPEC §13.4, FR-RPT-003): {@code { title?, content?, status? }}.
 * Every field is optional (partial edit); {@code status} may move {@code DRAFT → SAVED}.
 *
 * <p>{@code status} is bound as a raw string and parsed in the service so an invalid value surfaces as a
 * {@code 422} field error (VR-008) through the shared validation model, rather than a framework {@code 400}
 * from enum deserialization.
 */
public record UpdateReportRequest(String title, String content, String status) {
}
