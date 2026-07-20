package com.migration.config;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class RuntimeConfigCatalog {

    public record Entry(
        String dbKey,
        String envKey,
        String fallbackPropertyKey,
        String defaultValue,
        boolean editable,
        boolean sensitive,
        boolean restartRequired,
        Predicate<String> validator
    ) {}

    private static final List<Entry> ENTRIES = List.of(
        entry("ip_whitelist_mode", "IP_WHITELIST_MODE", null, "OPEN", true, false, false, v ->
            v == null || v.isBlank() || "OPEN".equalsIgnoreCase(v) || "RESTRICTED".equalsIgnoreCase(v)),
        entry("ip_whitelist", "IP_WHITELIST", null, "[]", true, false, false, RuntimeConfigValidators::ipWhitelistJson),
        entry("min_threads_per_job", "MIN_THREADS_PER_JOB", null, "1", true, false, false, RuntimeConfigValidators::positiveInt),
        entry("max_threads_per_job", "MAX_THREADS_PER_JOB", null, "8", true, false, false, RuntimeConfigValidators::positiveInt),
        entry("gspace_webhook_url", "GSPACE_WEBHOOK_URL", "app.gspace-webhook-url", "", true, true, false, v -> true),
        entry("google_client_id", "GOOGLE_CLIENT_ID", null, "", true, true, false, v -> true),
        entry("google_client_secret", "GOOGLE_CLIENT_SECRET", null, "", true, true, false, v -> true),
        entry("allowed_email_domain", "ALLOWED_EMAIL_DOMAIN", "app.auth.allowed-email-domain", "", true, false, false, v -> true)
    );

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static Map<String, Entry> byKey() {
        return ENTRIES.stream().collect(Collectors.toMap(Entry::dbKey, Function.identity()));
    }

    private static Entry entry(String dbKey, String envKey, String fallbackPropertyKey, String defaultValue,
                               boolean editable, boolean sensitive, boolean restartRequired, Predicate<String> validator) {
        return new Entry(dbKey, envKey, fallbackPropertyKey, defaultValue, editable, sensitive, restartRequired, validator);
    }

    private RuntimeConfigCatalog() {}
}
