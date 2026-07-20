package com.migration.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainAdminTest {

    @Test
    void blankDomainMakesAnyEmailAdmin() {
        assertTrue(DomainAdmin.isAdmin("a@b.com", ""));
        assertTrue(DomainAdmin.isAdmin("a@b.com", null));
        assertFalse(DomainAdmin.isAdmin("", ""));
    }

    @Test
    void matchingDomainIsAdmin() {
        assertTrue(DomainAdmin.isAdmin("user@example.com", "example.com"));
        assertTrue(DomainAdmin.isAdmin("User@Example.Com", "EXAMPLE.COM"));
        assertTrue(DomainAdmin.isAdmin("user@example.com", "@example.com"));
    }

    @Test
    void otherDomainIsNotAdmin() {
        assertFalse(DomainAdmin.isAdmin("user@gmail.com", "example.com"));
        assertFalse(DomainAdmin.isAdmin("example.com@evil.com", "example.com"));
    }
}
