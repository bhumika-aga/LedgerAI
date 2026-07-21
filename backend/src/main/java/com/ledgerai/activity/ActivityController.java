package com.ledgerai.activity;

import com.ledgerai.activity.dto.ActivityResponse;
import com.ledgerai.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The Activity module's single endpoint (API_SPEC §15) — a <strong>read-only</strong> timeline. There is
 * deliberately no create/update/delete endpoint: the log is append-only and Users may not mutate it
 * (FR-TMLN-004, BR-016).
 *
 * <p>Thin by design (BACKEND_CODING_STANDARDS §4): it binds the query and delegates; it never resolves
 * the caller or checks ownership — that is the service's job (ARCHITECTURE §7.1). The default ordering is
 * newest-first (API_SPEC §15.1); the optional {@code clientId} selects the per-client view.
 */
@RestController
public class ActivityController {
    
    private final ActivityService activityService;
    
    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }
    
    @GetMapping("/api/v1/activities")
    public ResponseEntity<PageResponse<ActivityResponse>> timeline(
        @RequestParam(required = false) UUID clientId,
        @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(activityService.getTimeline(clientId, pageable));
    }
}
