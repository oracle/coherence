/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.net.Service;
import com.tangosol.net.Session;

import com.tangosol.net.options.WithClassLoader;

import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.JAASIdentityAsserter;
import com.tangosol.net.security.UsernameAndPassword;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.security.cert.Certificate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

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
 *
 * @author as  2011.06.16
 */
public abstract class AbstractHttpServer
        implements HttpServer, HttpServerStats
    {

    // ----- HttpServer interface  ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void setAuthMethod(String sMethod)
        {
        if (AUTH_BASIC.equalsIgnoreCase(sMethod)      ||
            AUTH_CERT.equalsIgnoreCase(sMethod)       ||
            AUTH_CERT_BASIC.equalsIgnoreCase(sMethod) ||
            AUTH_NONE.equalsIgnoreCase(sMethod))
            {
            m_sAuthMethod = sMethod;
            }
        else
            {
            throw new IllegalArgumentException("unsupported method: " + sMethod);
            }
        }

    /**
     * {@inheritDoc}
     */
    public void setSession(Session session)
        {
        m_session = session;
        }

    /**
     * {@inheritDoc}
     */
    public void setLocalAddress(String sAddr)
        {
        m_sAddr = sAddr;
        }

    /**
     * {@inheritDoc}
     */
    public String getListenAddress()
        {
        return getLocalAddress();
        }

    /**
     * {@inheritDoc}
     */
    public int getListenPort()
        {
        return m_nPort;
        }

    /**
     * {@inheritDoc}
     */
    public void setLocalPort(int nPort)
        {
        m_nPort = nPort;
        }

    /**
    * {@inheritDoc}
    */
    public void setParentService(Service service)
        {
        m_serviceParent = service;
        }

    /**
     * {@inheritDoc}
     */
    public void setResourceConfig(ResourceConfig config)
        {
        setResourceConfig(Collections.singletonMap("/", config));
        }

    /**
     * {@inheritDoc}
     */
    public void setResourceConfig(Map<String, ResourceConfig> mapConfig)
        {
        m_mapResourceConfig.clear();
        m_mapResourceConfig.putAll(mapConfig);
        }

    /**
     * {@inheritDoc}
     */
    public void setSocketProvider(SocketProvider provider)
        {
        m_socketProvider = provider;
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void start()
            throws IOException
        {
        if (!m_fStarted)
            {
            startInternal();
            m_fStarted = true;
            }
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void stop()
            throws IOException
        {
        if (m_fStarted)
            {
            stopInternal();
            m_fStarted = false;
            }
        }

    // ----- HttpServerStats interface --------------------------------------

    @Override
    public long getRequestCount()
        {
        return m_cRequestCount;
        }

    @Override
    public float getAverageRequestTime()
        {
        return m_cRequestCount == 0 ? 0 : (float) m_ltdTotalRequestTime / m_cRequestCount;
        }

    @Override
    public float getRequestsPerSecond()
        {
        return m_cRequestCount == 0 ? 0 :
               (m_cRequestCount * 1.0f) /
                ((float) (Base.getSafeTimeMillis() - m_ldtResetTime) / 1000.0f);
        }

    @Override
    public long getErrorCount()
        {
        return m_cErrors;
        }

    @Override
    public long getHttpStatusCount(int nPrefix)
        {
        validatePrefix(nPrefix);

        return f_aStatusCodes[nPrefix - 1];
        }

    @Override
    public void resetStats ()
        {
        m_cRequestCount       = 0L;
        m_ltdTotalRequestTime = 0L;
        m_cErrors             = 0L;
        m_ldtResetTime        = Base.getSafeTimeMillis();

        for (int i = 0; i < f_aStatusCodes.length ; i++)
            {
            f_aStatusCodes[i] = 0L;
            }
        }

    /**
     * Increment the request count.
     */
    protected void incrementRequestCount ()
        {
        m_cRequestCount++;
        }

    /**
     * Add to the total request time.
     *
     * @param ltdStartTime the start of the request
     */
    protected void logRequestTime(long ltdStartTime)
        {
        m_ltdTotalRequestTime += Base.getLastSafeTimeMillis() - ltdStartTime;
        }

    /**
     * Increment the number of errors.
     */
    protected void incrementErrors()
        {
        m_cErrors++;
        }

    /**
     * Add to the total of status codes.
     *
     * @param nStatusCode  the status code to add
     */
    protected void logStatusCount(int nStatusCode)
        {
        if (nStatusCode >= 100 && nStatusCode <= 600)
            {
            f_aStatusCodes[nStatusCode / 100 - 1]++;
            }
        }

    /**
     * Validate that the prefix is a valid value.
     *
     * @param nPrefix the prefix to validate
     */
    private void validatePrefix(int nPrefix)
        {
        if (nPrefix < 0 || nPrefix > f_aStatusCodes.length)
            {
            throw new IllegalArgumentException("Prefix must be between 0 and " + f_aStatusCodes.length);
            }
        }
    /**
     * Helper to dump current stats.
     */
    protected void dumpStats()
        {
        StringBuilder sb = new StringBuilder("HTTP statistics: ")
                .append(new Date())
                .append('\n').append("  Start time:    ").append(new Date(m_ldtResetTime))
                .append('\n')
                .append("Request Count=").append(getRequestCount())
                .append(", Avg Req Time=").append(getAverageRequestTime())
                .append(", Req/sec=").append(getRequestsPerSecond())
                .append(", Errors=").append(getErrorCount())
                .append("\nStatus counts: ");

        for (int i = 0; i < f_aStatusCodes.length; i++)
            {
            sb.append("Status ")
              .append((i + 1) * 100)
              .append('=')
              .append(f_aStatusCodes[i])
              .append(' ');
            }

        Logger.info(sb.toString());
        }

    // ----- abstract methods -----------------------------------------------

    /**
     * Start the server.
     *
     * @throws IOException  if an error occurs
     */
    protected abstract void startInternal()
            throws IOException;

    /**
     * Stop the server.
     *
     * @throws IOException  if an error occurs
     */
    protected abstract void stopInternal()
            throws IOException;

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

    /**
     * Create and configure a Jersey container that will process HTTP requests.
     *
     * @param resourceConfig  resource configuration
     *
     * @return the container
     */
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
     * Perform HTTP Basic authentication and return authenticated Subject.
     *
     * @param sAuth  the value of Authorization header from the request

     * @return authenticated Subject if successful, null otherwise
     */
    protected Subject authenticate(String sAuth)
        {
        if (sAuth != null && sAuth.startsWith("Basic "))
            {
            sAuth = sAuth.substring("Basic ".length());

            String[] values = fromBase64(sAuth).split(":");
            if (values.length == 2)
                {
                String sUsername = values[0];
                String sPassword = values[1];

                try
                    {
                    return getIdentityAsserter().assertIdentity(
                            new UsernameAndPassword(sUsername, sPassword),
                            getParentService());
                    }
                catch (SecurityException ignore)
                    {
                    // fall through and return null
                    }
                }
            }

        return null;
        }

    /**
     * Creates Subject instance using principal and credentials from the
     * SSL session.
     *
     * @param session  SSL session
     *
     * @return Subject for the client
     *
     * @throws SSLPeerUnverifiedException  if the client is not authenticated
     */
    protected Subject getSubjectFromSession(SSLSession session)
            throws SSLPeerUnverifiedException
        {
        Set<Principal> setPrincipals =
                Collections.singleton(session.getPeerPrincipal());
        Set<Certificate> setCredentials = new HashSet<>(
                Arrays.asList(session.getPeerCertificates()));

        return new Subject(true, setPrincipals, setCredentials, Collections.emptySet());
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

    // ----- Base64 support -------------------------------------------------

    /**
     * Converts a byte array into a Base64 encoded string.
     * @param buffer  byte array to encode
     * @return Base64 encoding of buffer
     */
    public static String toBase64(byte[] buffer)
       {
        byte[] result = java.util.Base64.getEncoder().encode(buffer);

        try
            {
            return new String(result, "ASCII");
            }
        catch (UnsupportedEncodingException e)
            {
            return new String(result);
            }
        }

    /**
     * Converts a string into a Base64 encoded string.
     * @param text  string to encode
     * @return Base64 encoding of text
     */
    public static String toBase64(String text)
        {
        return toBase64(text.getBytes());
        }

    /**
     * Converts a byte array into a Base64 decoded string.
     * @param buffer  byte array to decode
     * @return Base64 decoding of buffer
     */
    public static String fromBase64(byte[] buffer)
        {
        byte[] result = java.util.Base64.getDecoder().decode(buffer);

        try
            {
            return new String(result, "ASCII");
            }
        catch (UnsupportedEncodingException e)
            {
            return new String(result);
            }
        }

    /**
     * Converts a string into a Base64 decoded string
     * @param text  string to decode
     * @return Base64 decoding of text
     */
    public static String fromBase64(String text)
        {
        return fromBase64(text.getBytes());
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

    /**
     * Return the service that is embedding this server.
     *
     * @return the parent service
     */
    public Service getParentService()
        {
        return m_serviceParent;
        }

    /**
     * Return the Coherence {@link Session} to use.
     *
     * @return the Session
     */
    public Session getSession()
        {
        return m_session != null
               ? m_session
               : Session.create(WithClassLoader.autoDetect());
        }

    /**
     * Return the SocketProvider to use.
     *
     * @return the SocketProvider
     */
    public SocketProvider getSocketProvider()
        {
        return m_socketProvider;
        }

    /**
     * Return the SSLContext to use.
     *
     * @return the SSLContext
     */
    public SSLContext getSSLContext()
        {
        return isSecure() 
               ? ((SSLSocketProvider) m_socketProvider).getDependencies().getSSLContext()
               : null;
        }

    /**
     * Return the SSLParameters to use.
     *
     * @return the SSLParameters
     */
    public SSLParameters getSSLParameters()
        {
        return isSecure()
               ? ((SSLSocketProvider) m_socketProvider).getDependencies().getSSLParameters()
               : null;
        }

    /**
     * Return map of context names to Jersey resource configurations.
     *
     * @return map of context names to Jersey resource configurations
     */
    public Map<String, ResourceConfig> getResourceConfig()
        {
        return m_mapResourceConfig;
        }

    /**
     * Return identity asserter to use for HTTP basic authentication.
     *
     * @return the identity asserter to use
     */
    public IdentityAsserter getIdentityAsserter()
        {
        return m_identityAsserter;
        }

    /**
     * Configure the identity asserter to use for HTTP basic authentication.
     *
     * @param asserter  the identity asserter to use
     */
    protected void setIdentityAsserter(IdentityAsserter asserter)
        {
        m_identityAsserter = asserter;
        }

    /**
     * Return true if this server should use HTTP basic authentication.
     *
     * @return true if HTTP basic authentication should be used
     */
    protected boolean isAuthMethodBasic()
        {
        String sAuthMethod = m_sAuthMethod;
        return AUTH_BASIC.equalsIgnoreCase(sAuthMethod)
                || AUTH_CERT_BASIC.equalsIgnoreCase(sAuthMethod);
        }

    /**
     * Return true if this server should use client certificates for
     * authentication.
     *
     * @return true if client certificates should be used for authentication
     */
    protected boolean isAuthMethodCert()
        {
        String sAuthMethod = m_sAuthMethod;
        return AUTH_CERT.equalsIgnoreCase(sAuthMethod)
                || AUTH_CERT_BASIC.equalsIgnoreCase(sAuthMethod);
        }

    /**
     * Return true if this server should not require client authentication.
     *
     * @return true if client authentication is not required
     */
    protected boolean isAuthMethodNone()
        {
        return AUTH_NONE.equalsIgnoreCase(m_sAuthMethod);
        }

    /**
     * Return true if this server uses SSL to secure communication.
     *
     * @return true if this server uses SSL
     */
    protected boolean isSecure()
        {
        return m_socketProvider instanceof SSLSocketProvider;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass())
                + "{Protocol=" + (isSecure() ? "HTTPS" : "HTTP")
                + ", AuthMethod=" + m_sAuthMethod + "}";
        }

    // ----- data members ---------------------------------------------------

    /**
     * Authentication method. Valid values are 'basic', 'cert', 'cert+basic',
     * and 'none'.
     */
    protected String m_sAuthMethod = AUTH_NONE;

    /**
     * Coherence session.
     */
    protected Session m_session;

    /**
     * Address server should listen on.
     */
    protected String m_sAddr = DEFAULT_ADDRESS;

    /**
     * Port number server should listen on.
     */
    protected int m_nPort = DEFAULT_PORT;

    /**
     * Parent service.
     */
    protected Service m_serviceParent;

    /**
     * Map of context names to Jersey resource configurations.
     */
    protected final Map<String, ResourceConfig> m_mapResourceConfig = new HashMap<>();

    /**
     * SocketProvider used by the server.
     */
    protected SocketProvider m_socketProvider;

    /**
     * Identity asserter to use with HTTP basic authentication.
     */
    protected IdentityAsserter m_identityAsserter = DEFAULT_IDENTITY_ASSERTER;

    /**
     * Flag specifying whether the server is already started.
     */
    protected boolean m_fStarted;

    /**
     * Total number of requests.
     */
    private long m_cRequestCount = 0L;

    /**
     * Total request time.
     */
    private long m_ltdTotalRequestTime = 0L;

    /**
     * Total number of errors when processing requests.
     */
    private long m_cErrors = 0L;

    /**
     * Count of status code responses.
     */
    private final long[] f_aStatusCodes = new long[5];

    /**
     * The time stats were last reset.
     */
    private long m_ldtResetTime = Base.getSafeTimeMillis();

    // ----- constants ------------------------------------------------------

    /**
     * Default HTTP server address.
     */
    public static final String DEFAULT_ADDRESS = "localhost";

    /**
     * Default HTTP server port.
     */
    public static final int DEFAULT_PORT = 0;

    /**
     * Default identity asserter.
     */
    public static final IdentityAsserter DEFAULT_IDENTITY_ASSERTER =
            new JAASIdentityAsserter("CoherenceREST");

    /**
     * HTTP basic authentication.
     */
    public static final String AUTH_BASIC = "basic";

    /**
     * Certificate authentication.
     */
    public static final String AUTH_CERT = "cert";

    /**
     * Certificate authentication.
     */
    public static final String AUTH_CERT_BASIC = "cert+basic";

    /**
     * No authentication.
     */
    public static final String AUTH_NONE = "none";

    /**
     * Realm for HTTP basic authentication.
     */
    public static final String HTTP_BASIC_REALM = "Coherence REST";

    /**
     * Attribute name that should be used to store Subject for the request.
     */
    public static final String ATTR_SUBJECT = "__SUBJECT";

    /**
     * Authorization header.
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * WWW-Authenticate header.
     */
    protected static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    /**
     * A {@link Principal} with no name.
     */
    protected static final Principal EMPTY_PRINCIPAL = () -> null;

    /**
     * Symbolic reference for {@code /}.
     */
    protected static final String SLASH = "/";

    /**
     * Symbolic reference for character {@code /}.
     */
    protected static final char SLASH_CHAR = SLASH.charAt(0);

    /**
     * The HTTP header value to return when basic authentication is enabled and required.
     */
    protected static final String DEFAULT_BASIC_AUTH_HEADER_VALUE = "Basic realm=\"" + HTTP_BASIC_REALM + '"';
    }
