/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.providers;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;

import org.junit.Test;

import java.io.IOException;

import java.net.URI;

import java.security.Principal;

import java.util.Base64;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link SecurityFilter}.
 * <p>
 * Prompt 02 at
 * design/features/security-bugs/plans/rest-01/prompts/02-slice-b-auth-passthrough-implementation.md
 * requires the mode matrix for engaged REST authentication while preserving
 * anonymous no-auth resources.
 *
 * @author Vaso Putica  2026.05.07
 * @since 26.07
 */
public class SecurityFilterTest
    {
    /**
     * Should reject DEV-mode requests when auth is engaged but no principal is
     * available.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldRejectEngagedAuthWithoutPrincipalInDev()
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.dev())
            {
            ContainerRequest request = request(context(SecurityContext.BASIC_AUTH, null));

            new SecurityFilter().filter(request);

            assertUnauthorized(request);
            }
        }

    /**
     * Should reject PROD-mode requests when auth is engaged but no principal is
     * available.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldRejectEngagedAuthWithoutPrincipalInProd()
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            ContainerRequest request = request(context(SecurityContext.BASIC_AUTH, null));

            new SecurityFilter().filter(request);

            assertUnauthorized(request);
            }
        }

    /**
     * Should reject unsupported authorization schemes once auth is engaged.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldRejectUnsupportedAuthSchemeWhenAuthIsEngaged()
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            ContainerRequest request = request(context(SecurityContext.BASIC_AUTH, null));
            request.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer token");

            new SecurityFilter().filter(request);

            assertUnauthorized(request);
            }
        }

    /**
     * Should reject a Basic header when the hosting server/container did not
     * provide an authenticated principal.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldRejectBasicHeaderWhenEngagedAuthHasNoPrincipal()
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            ContainerRequest request = request(context(SecurityContext.BASIC_AUTH, null));
            request.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, basic("client:password"));

            new SecurityFilter().filter(request);

            assertUnauthorized(request);
            }
        }

    /**
     * Should return 401 when the authentication scheme accessor fails in PROD.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldReturnUnauthorizedWhenAuthenticationSchemeThrowsInProd()
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            ContainerRequest request = request(new ThrowingAuthenticationSchemeContext());

            new SecurityFilter().filter(request);

            assertUnauthorized(request);
            }
        }

    /**
     * Should return 401 when the principal accessor fails in PROD.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldReturnUnauthorizedWhenPrincipalAccessorThrowsInProd()
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            ContainerRequest request = request(new ThrowingPrincipalContext());

            new SecurityFilter().filter(request);

            assertUnauthorized(request);
            }
        }

    /**
     * Should return 401 when the principal name accessor fails in PROD.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldReturnUnauthorizedWhenPrincipalNameThrowsInProd()
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            ContainerRequest request = request(new TestSecurityContext(SecurityContext.BASIC_AUTH,
                    new ThrowingPrincipal()));

            new SecurityFilter().filter(request);

            assertUnauthorized(request);
            }
        }

    /**
     * Should preserve LEGACY behavior when the authentication scheme accessor
     * fails.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldNotAbortWhenAuthenticationSchemeThrowsInLegacy()
            throws IOException
        {
        assertLegacyDoesNotAbort(new ThrowingAuthenticationSchemeContext());
        }

    /**
     * Should preserve LEGACY behavior when the principal accessor fails.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldNotAbortWhenPrincipalAccessorThrowsInLegacy()
            throws IOException
        {
        assertLegacyDoesNotAbort(new ThrowingPrincipalContext());
        }

    /**
     * Should preserve LEGACY behavior when the principal name accessor fails.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldNotAbortWhenPrincipalNameThrowsInLegacy()
            throws IOException
        {
        assertLegacyDoesNotAbort(new TestSecurityContext(SecurityContext.BASIC_AUTH, new ThrowingPrincipal()));
        }

    /**
     * Should accept a usable server/container principal when auth is engaged.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldAcceptUsablePrincipalWhenAuthIsEngaged()
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            ContainerRequest request = request(context(SecurityContext.BASIC_AUTH, "client"));

            new SecurityFilter().filter(request);

            assertNull(request.getAbortResponse());
            assertEquals("client", request.getSecurityContext().getUserPrincipal().getName());
            assertEquals(SecurityContext.BASIC_AUTH, request.getSecurityContext().getAuthenticationScheme());
            }
        }

    /**
     * Should preserve the historical fail-open behavior in LEGACY mode.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldPreserveFailOpenBehaviorInLegacy()
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            ContainerRequest request = request(context(SecurityContext.BASIC_AUTH, null));

            new SecurityFilter().filter(request);

            assertNull(request.getAbortResponse());
            }
        }

    /**
     * Should preserve anonymous REST behavior when auth is not engaged.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldPreserveAnonymousBehaviorWhenAuthIsNotEngaged()
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.dev())
            {
            ContainerRequest request = request(context(null, null));

            new SecurityFilter().filter(request);

            assertNull(request.getAbortResponse());
            assertNull(request.getSecurityContext().getUserPrincipal());
            }
        }

    /**
     * Should ignore Basic headers on anonymous REST resources.
     *
     * @throws IOException if the filter fails unexpectedly
     */
    @Test
    public void shouldIgnoreBasicHeaderWhenAuthIsNotEngaged()
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.dev())
            {
            ContainerRequest request = request(context(null, null));
            request.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, basic("spoof:secret"));

            new SecurityFilter().filter(request);

            assertNull(request.getAbortResponse());
            assertNull(request.getSecurityContext().getUserPrincipal());
            }
        }

    /**
     * Create a container request for the supplied security context.
     *
     * @param context  the request security context
     *
     * @return the container request
     */
    private static ContainerRequest request(SecurityContext context)
        {
        return new ContainerRequest(URI.create("http://localhost/api/"),
                URI.create("http://localhost/api/dist-test"), "GET", context, new MapPropertiesDelegate());
        }

    /**
     * Create a test security context.
     *
     * @param sScheme     the authentication scheme
     * @param sPrincipal  the principal name
     *
     * @return the test security context
     */
    private static SecurityContext context(String sScheme, String sPrincipal)
        {
        Principal principal = sPrincipal == null ? null : () -> sPrincipal;
        return new TestSecurityContext(sScheme, principal);
        }

    /**
     * Assert that the request was aborted with a 401 response.
     *
     * @param request  the container request
     */
    private static void assertUnauthorized(ContainerRequest request)
        {
        Response response = request.getAbortResponse();
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals("Basic realm=\"Coherence REST\"",
                response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
        }

    /**
     * Assert that LEGACY mode does not abort for the supplied security context.
     *
     * @param context  the request security context
     *
     * @throws IOException if the filter fails unexpectedly
     */
    private static void assertLegacyDoesNotAbort(SecurityContext context)
            throws IOException
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            ContainerRequest request = request(context);

            new SecurityFilter().filter(request);

            assertNull(request.getAbortResponse());
            }
        }

    /**
     * Create a Basic authorization header value.
     *
     * @param sCredentials  the raw credentials
     *
     * @return the authorization header value
     */
    private static String basic(String sCredentials)
        {
        return "Basic " + Base64.getEncoder().encodeToString(sCredentials.getBytes());
        }


    private static class TestSecurityContext
            implements SecurityContext
        {
        /**
         * Construct a test security context.
         *
         * @param sScheme    the authentication scheme
         * @param principal  the user principal
         */
        TestSecurityContext(String sScheme, Principal principal)
            {
            m_sScheme   = sScheme;
            m_principal = principal;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Principal getUserPrincipal()
            {
            return m_principal;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isUserInRole(String sRole)
            {
            return false;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSecure()
            {
            return false;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getAuthenticationScheme()
            {
            return m_sScheme;
            }

        private final String m_sScheme;

        private final Principal m_principal;
        }


    private static class ThrowingAuthenticationSchemeContext
            extends TestSecurityContext
        {
        /**
         * Construct a context whose authentication scheme accessor fails.
         */
        ThrowingAuthenticationSchemeContext()
            {
            super(null, null);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getAuthenticationScheme()
            {
            throw new IllegalStateException("authentication scheme unavailable");
            }
        }


    private static class ThrowingPrincipalContext
            extends TestSecurityContext
        {
        /**
         * Construct a context whose principal accessor fails.
         */
        ThrowingPrincipalContext()
            {
            super(SecurityContext.BASIC_AUTH, null);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Principal getUserPrincipal()
            {
            throw new IllegalStateException("principal unavailable");
            }
        }


    private static class ThrowingPrincipal
            implements Principal
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getName()
            {
            throw new IllegalStateException("principal name unavailable");
            }
        }
    }
