/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.providers;

import org.glassfish.jersey.server.ContainerRequest;

import com.tangosol.coherence.http.AbstractHttpServer;
import com.tangosol.coherence.http.BasicAuthentication;

import com.tangosol.coherence.rest.RestSecurityPolicy;

import com.tangosol.util.CoherenceMode;

import java.io.IOException;

import java.security.Principal;

import jakarta.inject.Inject;

import jakarta.ws.rs.WebApplicationException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;

import jakarta.ws.rs.container.PreMatching;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import jakarta.ws.rs.ext.Provider;

/**
 * Simple authentication filter.
 *
 * In DEV and PROD mode, returns response with http status 401 when REST
 * authentication is engaged but proper authentication is not provided in the
 * incoming request. LEGACY mode preserves the historical fail-open behavior.
 *
 * @author lh
 * @see ContainerRequestFilter
 *
 * @since Coherence 12.2.1
 */
@Provider
@PreMatching
public class SecurityFilter implements ContainerRequestFilter
    {
    @Inject
    jakarta.inject.Provider<UriInfo> uriInfo;

    @Override
    public void filter(ContainerRequestContext filterContext) throws IOException
        {
        SecurityContext contextCurrent = filterContext.getSecurityContext();
        RestSecurityPolicy.AuthenticationState authState =
                RestSecurityPolicy.evaluateAuthentication(contextCurrent);
        SecurityContext securityContext = authenticate((ContainerRequest) filterContext.getRequest(), authState);
        if (securityContext != null)
            {
            filterContext.setSecurityContext(securityContext);
            }
        else if (authState.isPrincipalRequired())
            {
            filterContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\""
                                                          + AbstractHttpServer.HTTP_BASIC_REALM + '"')
                    .build());
            }
        }

    private SecurityContext authenticate(ContainerRequest request, RestSecurityPolicy.AuthenticationState authState)
        {
        String authentication = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        boolean   fAuthEngaged     = authState.isEngaged();
        boolean   fPrincipalUsable = authState.hasUsablePrincipal();
        Principal principal        = fPrincipalUsable ? authState.getPrincipal() : null;
        String    sPrincipalName   = authState.getPrincipalName();
        String    sScheme          = authState.getAuthenticationScheme();
        if (authentication == null)
            {
            if (principal == null)
                {
                return null;
                }
            else
                {
                return new Authorizer(sPrincipalName, principal, sScheme);
                }
            }


        if (fAuthEngaged && CoherenceMode.isCoherenceRestAuthEnforced())
            {
            return fPrincipalUsable
                   ? new Authorizer(sPrincipalName, principal, sScheme)
                   : null;
            }

        if (!fAuthEngaged && CoherenceMode.isCoherenceRestAuthEnforced())
            {
            return null;
            }

        BasicAuthentication.Credentials credentials;
        try
            {
            credentials = BasicAuthentication.parse(authentication);
            }
        catch (IllegalArgumentException e)
            {
            throw new WebApplicationException(400);
            }

        if (credentials == null)
            {
            // "Only HTTP Basic authentication is supported"
            return null;
            }

        return new Authorizer(credentials.getUsername());
        }

    public class Authorizer implements SecurityContext
        {
        private String    m_username;
        private Principal m_principal;
        private String    m_role;
        private String    m_authScheme = SecurityContext.BASIC_AUTH;

        public Authorizer(final String username)
            {
            this.m_username = username;
            this.m_principal = new Principal()
                {
                public String getName()
                    {
                    return m_username;
                    }
                };
            }

        public Authorizer(String username, Principal principal, String scheme)
            {
            this.m_username = username;
            this.m_principal = principal;
            this.m_authScheme = scheme;
            }

        public Principal getUserPrincipal()
            {
            return this.m_principal;
            }

        public boolean isUserInRole(String role)
            {
            return (role.equals(m_role));
            }

        public boolean isSecure()
            {
            return "https".equals(uriInfo.get().getRequestUri().getScheme());
            }

        public String getAuthenticationScheme()
            {
            return m_authScheme;
            }

        public void setAuthenticationScheme(String scheme)
            {
            m_authScheme = scheme;
            }

        public String getRole()
            {
            return m_role;
            }

        public void setRole(String role)
            {
            m_role = role;
            }
        }
}
