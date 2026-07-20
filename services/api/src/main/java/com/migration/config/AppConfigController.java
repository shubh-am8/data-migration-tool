package com.migration.config;

import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class AppConfigController {
    private final AppConfigService appConfigService;

    public AppConfigController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping
    public Map<String, AppConfigService.ConfigEntryDto> getConfig() {
        return appConfigService.getEditableWithMeta();
    }

    @GetMapping("/{key}/reveal")
    public Map<String, String> revealConfig(@PathVariable String key) {
        return new LinkedHashMap<>(Map.of("value", appConfigService.revealSensitiveValue(key)));
    }

    @PutMapping
    public Map<String, String> updateConfig(@RequestBody Map<String, String> updates) {
        return appConfigService.update(updates);
    }
}
