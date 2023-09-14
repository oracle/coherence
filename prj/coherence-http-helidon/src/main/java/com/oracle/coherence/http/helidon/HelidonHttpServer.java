/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.http.helidon;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.http.AbstractHttpServer;
import com.tangosol.coherence.http.HttpServer;

import com.tangosol.net.Session;

import io.helidon.common.context.Context;

import io.helidon.common.http.Http;

import io.helidon.security.AuthenticationResponse;
import io.helidon.security.Principal;
import io.helidon.security.ProviderRequest;
import io.helidon.security.Security;
import io.helidon.security.SecurityResponse;

import io.helidon.security.integration.jersey.SecurityFeature;
import io.helidon.security.integration.webserver.WebSecurity;

import io.helidon.security.spi.AuthenticationProvider;
import io.helidon.security.spi.SynchronousProvider;

import io.helidon.webserver.ClientAuthentication;
import io.helidon.webserver.HttpException;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.Service;
import io.helidon.webserver.SocketConfiguration;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerTls;

import io.helidon.webserver.jersey.JerseySupport;

import jakarta.annotation.Priority;

import jakarta.inject.Inject;

import jakarta.ws.rs.Priorities;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;

import jakarta.ws.rs.core.SecurityContext;

import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.jersey.internal.inject.AbstractBinder;

import org.glassfish.jersey.server.ResourceConfig;

import javax.security.auth.Subject;

import java.security.cert.X509Certificate;

import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of {@link HttpServer} that uses the Helidon web server.
 *
 * @author Jonathan Knight  2023.09.08
 */
public class HelidonHttpServer
        extends AbstractHttpServer
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct HelidonHttpServer instance.
     */
    public HelidonHttpServer()
        {
        }

    // ----- AbstractHttpServer implementation ------------------------------

    @Override
    protected void startInternal()
        {
        if (m_server != null)
            {
            return;
            }

        f_lock.lock();
        try
            {
            if (m_server != null)
                {
                return;
                }

            m_server = createHelidonServer();
            m_server.start().get();
            m_nListenPort = m_server.port();
            resetStats();
            }
        catch (ExecutionException | InterruptedException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        finally
            {
            f_lock.unlock();
            }
        }

    @Override
    protected void stopInternal()
        {
        if (m_server == null)
            {
            return;
            }

        f_lock.lock();
        try
            {
            if (m_server == null)
                {
                return;
                }

            m_server.shutdown();
            m_server = null;
            }
        finally
            {
            f_lock.unlock();
            }
        }

    @Override
    public int getListenPort()
        {
        return m_nListenPort;
        }

    @Override
    protected Object instantiateContainer(ResourceConfig config, ServiceLocator locator)
        {
        return config;
        }

    @Override
    public Class<ResourceConfig> getResourceType()
        {
        return ResourceConfig.class;
        }

    @Override
    protected Object createContainer(ResourceConfig config)
        {
        return super.createContainer(config);
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Create a Helidon {@link WebServer}.
     *
     * @return  the Helidon {@link WebServer}
     */
    protected WebServer createHelidonServer()
        {
        Context ctx = Context.create();
        ctx.register("coherence.auth.basic", isAuthMethodBasic());
        ctx.register("coherence.auth.cert", isAuthMethodCert());

        WebServer.Builder builder = WebServer.builder()
                .context(ctx)
                .addSocket(SocketConfiguration.builder()
                        .bindAddress(getLocalAddress())
                        .port(getLocalPort())
                        .build());

        if (isSecure())
            {
            ClientAuthentication clientAuth = isAuthMethodCert()
                    ? ClientAuthentication.REQUIRE
                    : ClientAuthentication.OPTIONAL;

            builder.tls(WebServerTls.builder()
                    .enabled(true)
                    .sslContext(getSSLContext())
                    .clientAuth(clientAuth)
                    .build());
            }

        Routing.Builder routingBuilder = Routing.builder()
                .error(Throwable.class, (req, res, ex) ->
                    {
                    if (ex instanceof HttpException)
                        {
                        req.next(ex);
                        }
                    else
                        {
                        Logger.err(ex);
                        res.status(Http.Status.INTERNAL_SERVER_ERROR_500);
                        res.send();
                        }
                    });

        Security.Builder securityBuilder = Security.builder().enabled(true);

        if (isAuthMethodBasic())
            {
            securityBuilder.authenticationProvider(new BasicAuthProvider());
            }

        Security           security        = securityBuilder.build();
        HelidonBinder      binder          = new HelidonBinder();
        boolean            fAuthenticate   = isAuthMethodBasic() || isAuthMethodCert();
        RequireCertService certService     = new RequireCertService(isAuthMethodCert());
        SecurityFeature    securityFeature = SecurityFeature.builder(security)
                                                    .authenticateAnnotatedOnly(false)
                                                    .build();

        if (fAuthenticate)
            {
            routingBuilder.register(WebSecurity.create(security).securityDefaults(WebSecurity.authenticate()));
            }

        for (Map.Entry<String, ResourceConfig> entry : getResourceConfig().entrySet())
            {
            JerseySupport.Builder jerseyBuilder = JerseySupport.builder(entry.getValue())
                    .register(binder);

            if (fAuthenticate)
                {
                if (isAuthMethodBasic())
                    {
                    jerseyBuilder.register(new BasicAuthFilter());
                    }
                if (isAuthMethodCert())
                    {
                    jerseyBuilder.register(new ClientCertFilter());
                    }
                jerseyBuilder.register(securityFeature);
                }
            routingBuilder.register(entry.getKey(), certService, jerseyBuilder.build());
            }

        builder.addRouting(routingBuilder.build());

        return builder.build();
        }

    // ----- inner class: HelidonBinder -------------------------------------

    /**
     * An {@link AbstractBinder} implementation used to bind the
     * current {@link Session} and optionally the
     * {@link com.tangosol.net.Service}.
     */
    protected class HelidonBinder
            extends AbstractBinder
        {
        @Override
        protected void configure()
            {
            bind(getSession()).to(Session.class);
            if (getParentService() != null)
                {
                bind(getParentService()).to(com.tangosol.net.Service.class);
                }
            }
        }

    // ----- inner class: ClientCertFilter ----------------------------------

    /**
     * A Jersey {@link ContainerRequestFilter} to handle the client cert.
     * <p/>
     * If the {@link ContainerRequestContext} already has a principal and the
     * auth method is "basic" then this filter does nothing. If the request
     * contains a client cert, a new {@link SecurityContext} containing the
     * client cert principal.
     */
    @Priority(Priorities.AUTHENTICATION + 2)
    protected static class ClientCertFilter
            implements ContainerRequestFilter
        {
        @Inject
        @SuppressWarnings("CdiInjectionPointsInspection")
        private ServerRequest m_request;

        @Override
        public void filter(ContainerRequestContext ctx)
            {
            SecurityContext securityContext = ctx.getSecurityContext();

            if (SecurityContext.BASIC_AUTH.equals(securityContext.getAuthenticationScheme())
                    && securityContext.getUserPrincipal() != null)
                {
                // the current security context already has a principal with basic auth.
                return;
                }

            m_request.context()
                    .get(WebServerTls.CLIENT_X509_CERTIFICATE, X509Certificate.class)
                    .ifPresent(cert -> ctx.setSecurityContext(new SimpleSecurityContext(SecurityContext.CLIENT_CERT_AUTH,
                            cert.getSubjectX500Principal(), true)));
            }
        }

    // ----- inner class: ClientCertFilter ----------------------------------

    /**
     * A Jersey {@link ContainerRequestFilter} to handle the basic auth.
     * <p/>
     * This filter creates a new {@link SecurityContext} containing the original
     * {@link java.security.Principal} and sets the auth scheme to
     * {@link SecurityContext#BASIC_AUTH}.
     */
    @Priority(Priorities.AUTHENTICATION + 1)
    protected static class BasicAuthFilter
            implements ContainerRequestFilter
        {
        @Override
        public void filter(ContainerRequestContext ctx)
            {
            SecurityContext securityContext = ctx.getSecurityContext();
            ctx.setSecurityContext(new SimpleSecurityContext(SecurityContext.BASIC_AUTH,
                    securityContext.getUserPrincipal(), true));
            }
        }

    // ----- inner class: RequireCertService --------------------------------

    /**
     * A Helidon {@link Service} that will verify the presence
     * of a client certificate.
     */
    protected static class RequireCertService
            implements Service
        {
        /**
         * Create a {@link RequireCertService}.
         *
         * @param fRequired {@code true} if authentication is required
         */
        public RequireCertService(boolean fRequired)
            {
            f_fRequired = fRequired;
            }

        @Override
        public void update(Routing.Rules rules)
            {
            rules.any((req, res) ->
                {
                if (f_fRequired)
                    {
                    req.context().get(WebServerTls.CLIENT_X509_CERTIFICATE, X509Certificate.class)
                        .ifPresentOrElse(cert -> {}, () ->
                            {
                            res.status(Http.Status.UNAUTHORIZED_401);
                            res.send();
                            });
                    }
                req.next();
                });
            }

        // ----- data members -----------------------------------------------

        /**
         * A flag that is {@code true} if authentication is required.
         */
        private final boolean f_fRequired;
        }

    // ----- inner class: BasicAuthProvider ---------------------------------

    /**
     * A Helidon {@link SynchronousProvider} to check basic auth.
     */
    protected class BasicAuthProvider
            extends SynchronousProvider
            implements AuthenticationProvider
        {
        @Override
        protected AuthenticationResponse syncAuthenticate(ProviderRequest request)
            {
            Map<String, List<String>> headers = request.env().headers();
            List<String>              list    = headers.get(HEADER_AUTHORIZATION);

            Subject subject = list == null || list.isEmpty()
                    ? null : HelidonHttpServer.this.authenticate(list.get(0));

            if (subject == null)
                {
                return AuthenticationResponse.builder()
                        .status(SecurityResponse.SecurityStatus.FAILURE)
                        .description("Unauthorized")
                        .responseHeader(HEADER_WWW_AUTHENTICATE, DEFAULT_BASIC_AUTH_HEADER_VALUE)
                        .statusCode(Http.Status.UNAUTHORIZED_401.code())
                        .build();
                }

            io.helidon.security.Subject.Builder builder = io.helidon.security.Subject.builder();

            subject.getPrivateCredentials().forEach(builder::addPrivateCredential);
            subject.getPublicCredentials().forEach(builder::addPublicCredential);
            subject.getPrincipals().stream()
                    .map(p -> Principal.builder().name(p.getName()).build())
                    .forEach(builder::principal);

            return AuthenticationResponse.success(builder.build());
            }
        }

    // ---- data members ----------------------------------------------------

    /**
     * Helidon HTTP server instance.
     */
    protected WebServer m_server;

    /**
     * The cached listen port of this server.
     */
    protected int m_nListenPort;

    /**
     * The lock to control access to the start and stop methods.
     */
    private final Lock f_lock = new ReentrantLock();
    }
