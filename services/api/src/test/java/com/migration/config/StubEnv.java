package com.migration.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.HashMap;
import java.util.Map;

class StubEnv implements Environment {
    private final Map<String, String> props;

    StubEnv(Map<String, String> props) {
        this.props = new HashMap<>(props);
    }

    @Override public String[] getActiveProfiles() { return new String[0]; }
    @Override public String[] getDefaultProfiles() { return new String[0]; }
    @Override public boolean acceptsProfiles(String... profiles) { return false; }
    @Override public boolean acceptsProfiles(Profiles profiles) { return false; }
    @Override public boolean containsProperty(String key) { return props.containsKey(key); }
    @Override public String getProperty(String key) { return props.get(key); }
    @Override public String getProperty(String key, String defaultValue) {
        return props.getOrDefault(key, defaultValue);
    }
    @Override public <T> T getProperty(String key, Class<T> targetType) { return null; }
    @Override public <T> T getProperty(String key, Class<T> targetType, T defaultValue) { return defaultValue; }
    @Override public String getRequiredProperty(String key) { return props.get(key); }
    @Override public <T> T getRequiredProperty(String key, Class<T> targetType) { return null; }
    @Override public String resolvePlaceholders(String text) { return text; }
    @Override public String resolveRequiredPlaceholders(String text) { return text; }
}
