package com.migration.security;

import com.migration.config.AppConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IpWhitelistFilterTest {

    @Test
    void openModeAllowsAnyIp() throws Exception {
        AppConfigService cfg = mock(AppConfigService.class);
        when(cfg.get("ip_whitelist_mode")).thenReturn("OPEN");
        IpWhitelistFilter filter = new IpWhitelistFilter(cfg);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/jobs");
        req.setRemoteAddr("203.0.113.9");
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean[] reached = {false};

        filter.doFilter(req, res, (r, s) -> reached[0] = true);

        assertTrue(reached[0]);
        assertEquals(200, res.getStatus());
    }

    @Test
    void blankModeDefaultsToAllowAll() throws Exception {
        AppConfigService cfg = mock(AppConfigService.class);
        when(cfg.get("ip_whitelist_mode")).thenReturn("");
        IpWhitelistFilter filter = new IpWhitelistFilter(cfg);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/jobs");
        req.setRemoteAddr("203.0.113.9");
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean[] reached = {false};

        filter.doFilter(req, res, (r, s) -> reached[0] = true);

        assertTrue(reached[0]);
    }

    @Test
    void restrictedModeAllowsListedIp() throws Exception {
        AppConfigService cfg = mock(AppConfigService.class);
        when(cfg.get("ip_whitelist_mode")).thenReturn("RESTRICTED");
        when(cfg.get("ip_whitelist")).thenReturn("[\"203.0.113.9\"]");
        IpWhitelistFilter filter = new IpWhitelistFilter(cfg);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/jobs");
        req.setRemoteAddr("203.0.113.9");
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean[] reached = {false};

        filter.doFilter(req, res, (r, s) -> reached[0] = true);

        assertTrue(reached[0]);
    }

    @Test
    void restrictedModeRejectsUnlistedIp() throws Exception {
        AppConfigService cfg = mock(AppConfigService.class);
        when(cfg.get("ip_whitelist_mode")).thenReturn("RESTRICTED");
        when(cfg.get("ip_whitelist")).thenReturn("[\"203.0.113.9\"]");
        IpWhitelistFilter filter = new IpWhitelistFilter(cfg);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/jobs");
        req.setRemoteAddr("198.51.100.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean[] reached = {false};

        filter.doFilter(req, res, (r, s) -> reached[0] = true);

        assertFalse(reached[0]);
        assertEquals(403, res.getStatus());
    }

    @Test
    void restrictedModeIgnoresClientSpoofedXForwardedFor() throws Exception {
        // Client XFF is not trusted; Spring rewrites remoteAddr when
        // server.forward-headers-strategy=framework and the proxy is trusted.
        AppConfigService cfg = mock(AppConfigService.class);
        when(cfg.get("ip_whitelist_mode")).thenReturn("RESTRICTED");
        when(cfg.get("ip_whitelist")).thenReturn("[\"203.0.113.9\"]");
        IpWhitelistFilter filter = new IpWhitelistFilter(cfg);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/jobs");
        req.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean[] reached = {false};

        filter.doFilter(req, res, (r, s) -> reached[0] = true);

        assertFalse(reached[0]);
        assertEquals(403, res.getStatus());
    }

    @Test
    void healthAndOauthPathsAreNeverBlocked() throws Exception {
        AppConfigService cfg = mock(AppConfigService.class);
        when(cfg.get("ip_whitelist_mode")).thenReturn("RESTRICTED");
        when(cfg.get("ip_whitelist")).thenReturn("[]");
        IpWhitelistFilter filter = new IpWhitelistFilter(cfg);

        for (String path : new String[] {"/api/health", "/actuator/health", "/oauth2/authorization/google", "/login/oauth2/code/google"}) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
            req.setRemoteAddr("198.51.100.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            boolean[] reached = {false};

            filter.doFilter(req, res, (r, s) -> reached[0] = true);

            assertTrue(reached[0], "expected " + path + " to bypass the whitelist filter");
        }
    }
}
