package com.GoTicket.Catalogo.Evento.Config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalApiTokenFilterTest {
    @Test
    void rejectsRequestsWhenTokenIsMissingOrInvalid() throws Exception {
        InternalApiTokenFilter filter = new InternalApiTokenFilter();
        ReflectionTestUtils.setField(filter, "expectedToken", "shared-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/eventos");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainReached = new AtomicBoolean();

        FilterChain chain = (servletRequest, servletResponse) -> chainReached.set(true);
        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertFalse(chainReached.get());
    }

    @Test
    void acceptsRequestWithCorrectToken() throws Exception {
        InternalApiTokenFilter filter = new InternalApiTokenFilter();
        ReflectionTestUtils.setField(filter, "expectedToken", "shared-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/eventos");
        request.addHeader("X-GoTicket-Internal-Token", "shared-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainReached = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainReached.set(true));

        assertEquals(200, response.getStatus());
        assertTrue(chainReached.get());
    }

    @Test
    void allowsHealthProbeWithoutInternalToken() throws Exception {
        InternalApiTokenFilter filter = new InternalApiTokenFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health/readiness");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainReached = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainReached.set(true));

        assertEquals(200, response.getStatus());
        assertTrue(chainReached.get());
    }
}
