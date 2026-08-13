/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import jakarta.annotation.Priority;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.security.Principal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test-only security context filter for RAG REST integration tests.
 *
 * @author Aleks Seovic  2026.04.28
 * @since 26.04
 */
@Provider
@Priority(Priorities.USER)
public class TestSecurityContextFilter
        implements ContainerRequestFilter
    {
    @Override
    public void filter(ContainerRequestContext requestContext)
        {
        String sUser = requestContext.getHeaderString(HEADER_USER);
        if (sUser == null || sUser.isBlank())
            {
            return;
            }

        Set<String> setRole = roles(requestContext.getHeaderString(HEADER_ROLES));
        String sScheme = requestContext.getHeaderString(HEADER_SCHEME);
        SecurityContext original = requestContext.getSecurityContext();
        requestContext.setSecurityContext(new SecurityContext()
            {
            @Override
            public Principal getUserPrincipal()
                {
                return () -> sUser;
                }

            @Override
            public boolean isUserInRole(String role)
                {
                return setRole.contains(role);
                }

            @Override
            public boolean isSecure()
                {
                return original != null && original.isSecure();
                }

            @Override
            public String getAuthenticationScheme()
                {
                return sScheme == null || sScheme.isBlank() ? "Bearer" : sScheme;
                }
            });
        }

    private static Set<String> roles(String sRoles)
        {
        if (sRoles == null || sRoles.isBlank())
            {
            return Set.of();
            }

        return Arrays.stream(sRoles.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        }

    /**
     * Test request header containing the authenticated user name.
     */
    public static final String HEADER_USER = "X-RAG-Test-User";

    /**
     * Test request header containing comma-separated role names.
     */
    public static final String HEADER_ROLES = "X-RAG-Test-Roles";

    /**
     * Test request header containing the authentication scheme.
     */
    public static final String HEADER_SCHEME = "X-RAG-Test-Scheme";
    }
