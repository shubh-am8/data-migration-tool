package com.migration.workers;

import com.migration.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/workers")
public class WorkerController {
    private final WorkerHeartbeatRepository repository;

    public WorkerController(WorkerHeartbeatRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public PageResponse<Map<String, Object>> list(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size) {
        Instant stale = Instant.now().minus(30, ChronoUnit.SECONDS);
        int p = PageResponse.clampPage(page);
        int s = PageResponse.clampSize(size);
        var result = repository.findAll(PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "lastSeen")));
        return PageResponse.of(result.getContent().stream().map(w -> {
            Map<String, Object> m = new HashMap<>();
            m.put("workerId", w.getWorkerId());
            m.put("activeThreads", w.getActiveThreads());
            m.put("currentJobId", w.getCurrentJobId() != null ? w.getCurrentJobId().toString() : "");
            m.put("threadDetails", w.getThreadDetails());
            m.put("lastSeen", w.getLastSeen());
            m.put("online", w.getLastSeen() != null && w.getLastSeen().isAfter(stale));
            return m;
        }).toList(), p, s, result.getTotalElements());
    }
}
