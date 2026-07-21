package com.migration.marketplace;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/lab/seed-sessions")
public class LabSeedController {
    private final LabSeedSessionService seedSessionService;

    public LabSeedController(LabSeedSessionService seedSessionService) {
        this.seedSessionService = seedSessionService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return seedSessionService.list();
    }

    @PostMapping("/{jobId}/start")
    public Map<String, Object> start(@PathVariable UUID jobId) {
        return wrap(() -> seedSessionService.start(jobId));
    }

    @PostMapping("/{jobId}/pause")
    public Map<String, Object> pause(@PathVariable UUID jobId) {
        return wrap(() -> seedSessionService.pause(jobId));
    }

    @PostMapping("/{jobId}/resume")
    public Map<String, Object> resume(@PathVariable UUID jobId) {
        return wrap(() -> seedSessionService.resume(jobId));
    }

    @PostMapping("/{jobId}/stop")
    public Map<String, Object> stop(@PathVariable UUID jobId) {
        return wrap(() -> seedSessionService.stop(jobId));
    }

    @PatchMapping("/{jobId}")
    public Map<String, Object> patch(@PathVariable UUID jobId, @RequestBody Map<String, Object> body) {
        Integer inserts = body.get("insertsPerMinute") instanceof Number n ? n.intValue() : null;
        Integer updates = body.get("updatesPerMinute") instanceof Number n ? n.intValue() : null;
        return wrap(() -> seedSessionService.updateRates(jobId, inserts, updates));
    }

    private Map<String, Object> wrap(Supplier supplier) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface Supplier {
        Map<String, Object> get();
    }
}
