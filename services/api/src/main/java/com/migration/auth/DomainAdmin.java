package com.migration.auth;

final class DomainAdmin {
    private DomainAdmin() {}

    /**
     * Domain-admin rule (2C): if allowedDomain is blank, every authenticated email is admin;
     * otherwise email must end with {@code @}{@code allowedDomain} (case-insensitive).
     */
    static boolean isAdmin(String email, String allowedDomain) {
        if (email == null || email.isBlank()) return false;
        if (allowedDomain == null || allowedDomain.isBlank()) return true;
        String domain = allowedDomain.trim().toLowerCase();
        if (domain.startsWith("@")) domain = domain.substring(1);
        String lower = email.trim().toLowerCase();
        return lower.endsWith("@" + domain);
    }
}
