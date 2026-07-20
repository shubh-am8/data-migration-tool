package com.migration.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SecurityFilterRegistrationTest {

    @Test
    void disablesServletRegistrationForSecurityFilters() {
        SecurityFilterRegistration reg = new SecurityFilterRegistration();
        JwtAuthFilter jwt = mock(JwtAuthFilter.class);
        IpWhitelistFilter ip = mock(IpWhitelistFilter.class);

        FilterRegistrationBean<JwtAuthFilter> jwtReg = reg.jwtAuthFilterRegistration(jwt);
        FilterRegistrationBean<IpWhitelistFilter> ipReg = reg.ipWhitelistFilterRegistration(ip);

        assertFalse(jwtReg.isEnabled());
        assertFalse(ipReg.isEnabled());
        assertSame(jwt, jwtReg.getFilter());
        assertSame(ip, ipReg.getFilter());
    }
}
