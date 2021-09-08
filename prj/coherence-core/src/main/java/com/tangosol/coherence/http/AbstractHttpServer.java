/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import com.tangosol.net.Service;
import com.tangosol.net.Session;

import java.io.IOException;

import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import javax.ws.rs.core.SecurityContext;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;

import org.glassfish.hk2.utilities.BuilderHelper;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Abstract base class for {@link HttpServer} implementations.
 * <p>
 * This class and its sub-classes use Jersey ResourceConfig instances to implement
 * the actual http endpoints to be served.
 *
 * @author as  2011.06.16
 */
public abstract class AbstractHttpServer
        extends AbstractGenericHttpServer<ResourceConfig>
        implements HttpServer
    {
    // ----- AbstractHttpServer methods -------------------------------------

    /**
     * Factory method for Jersey container instances.
     *
     * @param config   the resource configuration
     * @param locator  the parent service locator
     *
     * @return container instance
     */
    protected abstract Object instantiateContainer(ResourceConfig config, ServiceLocator locator);

    // ----- helpers --------------------------------------------------------

    @Override
    protected Object createContainer(ResourceConfig resourceConfig)
        {
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        ServiceLocator        locator = factory.create(getClass().getName());
        DynamicConfiguration  config  = locator
                .getService(DynamicConfigurationService.class)
                .createDynamicConfiguration();

        if (getParentService() != null)
            {
            config.bind(BuilderHelper.createConstantDescriptor(getParentService(), null, Service.class));
            }
        config.bind(BuilderHelper.createConstantDescriptor(getSession(), null, Session.class));

        config.commit();

        return instantiateContainer(resourceConfig, locator);
        }

    /**
     * Handle HTTP(S) request.
     *
     * @param app      web application that should handle request
     * @param request  the request
     * @param subject  the subject, can be null
     *
     * @throws IOException  if an error occurs
     */
    protected void handleRequest(final ApplicationHandler app,
                                 final ContainerRequest request,
                                 final Subject subject)
            throws IOException
        {
        if (subject == null)
            {
            app.handle(request);
            }
        else
            {
            try
                {
                Subject.doAs(subject, new PrivilegedExceptionAction<Object>()
                    {
                    public Object run()
                            throws IOException
                        {
                        app.handle(request);
                        return null;
                        }
                    });
                }
            catch (PrivilegedActionException e)
                {
                Exception cause = e.getException();
                if (cause instanceof RuntimeException)
                    {
                    throw (RuntimeException) cause;
                    }
                else
                    {
                    throw (IOException) cause;
                    }
                }
            }
        }

    // ----- inner class: SimpleSecurityContext -----------------------------

    /**
     * Simple implementation of the SecurityContext interface.
     */
    public static class SimpleSecurityContext
            implements SecurityContext
        {
        /**
         * Create a new SimpleSecurityContext instance.
         *
         * @param sAuthScheme  string value of the authentication scheme used
         *                     to protect resources
         * @param principal    the Principal containing the name of the
         *                     current authenticated user
         * @param fSecure      a boolean value indicating whether a request
         *                     was made using a secure channel, such as HTTPS
         */
        public SimpleSecurityContext(String sAuthScheme,
                Principal principal, boolean fSecure)
            {
            m_sAuthScheme = sAuthScheme;
            m_principal   = principal;
            m_fSecure     = fSecure;
            }

        /**
         * Return the string value of the authentication scheme used to
         * protect the resource.
         */
        public String getAuthenticationScheme()
            {
            return m_sAuthScheme;
            }

        /**
         * Return a Principal object containing the name of the current
         * authenticated user.
         */
        public Principal getUserPrincipal()
            {
            return m_principal;
            }

        /**
         * Return a boolean indicating whether this request was made using a
         * secure channel, such as HTTPS.
         */
        public boolean isSecure()
            {
            return m_fSecure;
            }

        /**
         * Return a boolean indicating whether the authenticated user is
         * included in the specified logical "role".
         *
         * @param sRole  the name of the role
         */
        public boolean isUserInRole(String sRole)
            {
            return false;
            }

        /**
         * The authentication scheme.
         */
        private String m_sAuthScheme;

        /**
         * The current authenticated principal.
         */
        private Principal m_principal;

        /**
         * True if the request was made using a secure channel, false
         * otherwise.
         */
        private boolean m_fSecure;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the address the server should listen on.
     *
     * @return the address
     */
    public String getLocalAddress()
        {
        return m_sAddr;
        }

    /**
     * Return the port number the server should listen on.
     *
     * @return the port number
     */
    public int getLocalPort()
        {
        return m_nPort;
        }
    }
