package com.example.demo.reconciliation;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/reconciliation")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReconciliationAdminController {

    private final ReconciliationService service;

    @PostMapping("/run")
    public ResponseEntity<ReconciliationService.RunSummary> run() {
        return ResponseEntity.ok(service.run("MANUAL"));
    }

    @GetMapping("/items")
    public ResponseEntity<List<Map<String, Object>>> items(@RequestParam(defaultValue = "OPEN") String status) {
        return ResponseEntity.ok(service.items(status.toUpperCase()));
    }

    public record ResolveRequest(@NotBlank @Size(max = 500) String note) {
    }

    @PatchMapping("/items/{id}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest request) {
        return service.resolve(id, request.note()) ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
