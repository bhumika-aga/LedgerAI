package com.ledgerai.clients;

import com.ledgerai.clients.domain.ClientStatus;
import com.ledgerai.clients.dto.ClientResponse;
import com.ledgerai.clients.dto.CreateClientRequest;
import com.ledgerai.clients.dto.UpdateClientRequest;
import com.ledgerai.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The Client module's endpoints (API_SPEC §7) — the five documented operations and nothing else.
 *
 * <p>Thin by design (BACKEND_CODING_STANDARDS §4): it binds and shape-validates the request, then
 * delegates. It never resolves the caller or checks ownership — that is the service's job
 * (ARCHITECTURE §7.1) — so no path or body value can influence whose data is touched.
 *
 * <p>{@code DELETE} is the documented <em>archive</em> action, not a hard delete (API_SPEC §7.5,
 * DATABASE §8).
 */
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
    
    private final ClientService clientService;
    
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }
    
    /**
     * {@code page}/{@code size}/{@code sort} are bound by Spring Data's resolver, whose contract matches
     * API_SPEC §2.5 exactly — zero-based page, `field,(asc|desc)` sort — with the page-size default and
     * the documented max set in configuration. Only the <em>resource default</em> sort is declared here,
     * which is the one part §2.5 leaves per-resource: `name,asc`. A collection needs a deterministic
     * order or paging can repeat or skip rows. ({@code @SortDefault}, not {@code @PageableDefault}: the
     * latter carries its own size default that would silently override the configured one.)
     */
    @GetMapping
    public ResponseEntity<PageResponse<ClientResponse>> list(
        @RequestParam(required = false) ClientStatus status,
        @RequestParam(required = false) String q,
        @SortDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(clientService.list(status, q, pageable));
    }
    
    @GetMapping("/{clientId}")
    public ResponseEntity<ClientResponse> get(@PathVariable UUID clientId) {
        return ResponseEntity.ok(clientService.get(clientId));
    }
    
    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }
    
    @PatchMapping("/{clientId}")
    public ResponseEntity<ClientResponse> update(@PathVariable UUID clientId,
                                                 @Valid @RequestBody UpdateClientRequest request) {
        return ResponseEntity.ok(clientService.update(clientId, request));
    }
    
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> archive(@PathVariable UUID clientId) {
        clientService.archive(clientId);
        return ResponseEntity.noContent().build();
    }
}
