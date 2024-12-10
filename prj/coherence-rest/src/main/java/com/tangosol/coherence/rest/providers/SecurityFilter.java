/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.providers;

import org.glassfish.jersey.server.ContainerRequest;

import com.tangosol.coherence.http.AbstractHttpServer;

import java.io.IOException;

import java.security.Principal;

import javax.inject.Inject;

import javax.ws.rs.WebApplicationException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import javax.ws.rs.container.PreMatching;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import javax.ws.rs.ext.Provider;

/**
 * Simple authentication filter.
 *
 * Returns response with http status 401 when proper authentication is not provided in incoming request.
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
    javax.inject.Provider<UriInfo> uriInfo;

    @Override
    public void filter(ContainerRequestContext filterContext) throws IOException
        {
        SecurityContext securityContext = authenticate((ContainerRequest) filterContext.getRequest(),
                filterContext.getSecurityContext());
        if (securityContext != null)
            {
            filterContext.setSecurityContext(securityContext);
            }
        }

    private SecurityContext authenticate(ContainerRequest request, Object securityContext)
        {
        // Extract authentication credentials
        String authentication = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        Principal principal = null;
        if (authentication == null)
            {
            if (securityContext instanceof SecurityContext)
                {
                try
                    {
                    principal = ((SecurityContext) securityContext).getUserPrincipal();
                    }
                catch (Exception ignore)
                    {
                    // fall through and return null
                    }
                }

            if (principal == null)
                {
                return null;
                }
            else
                {
                return new Authorizer(principal.getName(), principal, SecurityContext.CLIENT_CERT_AUTH);
                }
            }

        if (!authentication.startsWith("Basic "))
            {
            // "Only HTTP Basic authentication is supported"
            return null;
            }
        authentication = authentication.substring("Basic ".length());
        String[] values = AbstractHttpServer.fromBase64(authentication).split(":");
        if (values.length < 2)
            {
            // "Invalid syntax for username and password"
            throw new WebApplicationException(400);
            }
        String sUsername = values[0];
        String sPassword = values[1];
        if ((sUsername == null) || (sPassword == null))
            {
            // "Missing username or password"
            throw new WebApplicationException(400);
            }

        // Nothing to do; our HTTP server already authenticated user
        return new Authorizer(sUsername);
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
