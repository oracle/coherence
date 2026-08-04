/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;

import com.tangosol.coherence.rest.server.DefaultResourceConfig;

import jakarta.annotation.Priority;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Priorities;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;

import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.security.Principal;

import java.util.Base64;

/**
 * Test-only REST config that simulates an engaged container security context.
 * <p>
 * Prompt reference:
 * {@code design/features/security-bugs/plans/rest-01/prompts/08-slice-f-test-plan-implementation.md}.
 *
 * @author Vaso Putica  2026.05.10
 * @since 26.07
 */
@ApplicationPath("/api")
public class Rest01EngagedAuthResourceConfig
        extends DefaultResourceConfig
    {
    /**
     * Construct a REST resource config that marks auth as engaged before the
     * product {@code SecurityFilter} runs.
     */
    public Rest01EngagedAuthResourceConfig()
        {
        register(EngagedAuthFilter.class);
        }

    /**
     * Pre-match filter that provides the container auth state required by the
     * REST-01 Decision 1 functional smoke.
     */
    @PreMatching
    @Priority(Priorities.AUTHENTICATION - 100)
    public static class EngagedAuthFilter
            implements ContainerRequestFilter
        {
        @Override
        public void filter(ContainerRequestContext context) throws IOException
            {
            String sAuthorization = context.getHeaderString(HttpHeaders.AUTHORIZATION);
            Principal principal = BASIC_AUTH_HEADER.equals(sAuthorization) ? PRINCIPAL : null;
            context.setSecurityContext(new TestSecurityContext(principal));
            }
        }

    /**
     * Security context used to emulate an auth-enabled hosting container.
     */
    private static class TestSecurityContext
            implements SecurityContext
        {
        /**
         * Construct a test security context with an optional principal.
         *
         * @param principal  the authenticated principal, or {@code null}
         */
        private TestSecurityContext(Principal principal)
            {
            m_principal = principal;
            }

        @Override
        public Principal getUserPrincipal()
            {
            return m_principal;
            }

        @Override
        public boolean isUserInRole(String sRole)
            {
            return false;
            }

        @Override
        public boolean isSecure()
            {
            return false;
            }

        @Override
        public String getAuthenticationScheme()
            {
            return SecurityContext.BASIC_AUTH;
            }

        private final Principal m_principal;
        }

    /**
     * Principal supplied when the test client sends the known valid credentials.
     */
    private static final Principal PRINCIPAL = () -> "client";

    /**
     * Basic auth header for client/password.
     */
    private static final String BASIC_AUTH_HEADER = "Basic "
            + Base64.getEncoder().encodeToString("client:password".getBytes(StandardCharsets.UTF_8));
    }
