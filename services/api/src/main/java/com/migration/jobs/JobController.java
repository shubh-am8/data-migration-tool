package com.migration.jobs;

import com.migration.common.PageResponse;
import com.migration.connectors.ExplainResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public PageResponse<Map<String, Object>> list(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size) {
        return jobService.list(page, size);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        return jobService.get(id);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return jobService.create(body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        return jobService.update(id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        jobService.delete(id);
    }

    @PostMapping("/{id}/preflight")
    public ExplainResult preflight(@PathVariable UUID id) throws Exception {
        return jobService.preflight(id);
    }

    @PostMapping("/{id}/start")
    public Map<String, Object> start(@PathVariable UUID id) {
        return jobService.start(id);
    }

    @PostMapping("/{id}/pause")
    public Map<String, Object> pause(@PathVariable UUID id) {
        return jobService.pause(id);
    }

    @PostMapping("/{id}/resume")
    public Map<String, Object> resume(@PathVariable UUID id) {
        return jobService.resume(id);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable UUID id) {
        return jobService.cancel(id);
    }

    @GetMapping("/{id}/status")
    public Map<String, Object> status(@PathVariable UUID id) {
        return jobService.status(id);
    }
}
