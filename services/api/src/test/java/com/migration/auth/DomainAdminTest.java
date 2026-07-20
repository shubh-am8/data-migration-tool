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
        assertTrue(DomainAdmin.isAdmin("user@chatbot.team", "chatbot.team"));
        assertTrue(DomainAdmin.isAdmin("User@Chatbot.Team", "CHATBOT.TEAM"));
        assertTrue(DomainAdmin.isAdmin("user@chatbot.team", "@chatbot.team"));
    }

    @Test
    void otherDomainIsNotAdmin() {
        assertFalse(DomainAdmin.isAdmin("user@gmail.com", "chatbot.team"));
        assertFalse(DomainAdmin.isAdmin("chatbot.team@evil.com", "chatbot.team"));
    }
}
