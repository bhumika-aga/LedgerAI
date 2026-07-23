package com.ledgerai.reports.dto;

/**
 * Optional body for {@code POST …/reports} (API_SPEC §13.1): {@code { title? }} — an optional generation
 * hint that also becomes the report's title. Absent body / null title generates an untitled draft. The
 * title length is validated in the service (VR-008; the bound is an externalized {@code [Assumption]}).
 */
public record GenerateReportRequest(String title) {
}
