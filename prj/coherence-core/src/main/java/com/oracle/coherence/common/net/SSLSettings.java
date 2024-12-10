/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;


/**
 * SSLSettings provides a means to configure the common aspects of
 * SSL properties.
 *
 * @author bbc
 */
public class SSLSettings
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an empty SSL configuration.
     */
    public SSLSettings()
        {
        }

    /**
     * Construct a SSLSettings object copying the values from the
     * specified settingsSSL object
     *
     * @param settingsSSL  the object to copy, or null
     */

    public SSLSettings(SSLSettings settingsSSL)
        {
        if (settingsSSL != null)
            {
            m_ctx                       = settingsSSL.getSSLContext();
            m_clientAuthMode            = settingsSSL.getClientAuth();
            m_hostnameVerifier          = settingsSSL.getHostnameVerifier();
            m_asCipherSuitesEnabled     = settingsSSL.getEnabledCipherSuites();
            m_asProtocolVersionsEnabled = settingsSSL.getEnabledProtocolVersions();
            }
        }

    /**
     * Return the SSLContext representing the SSL implementation and
     * configuration.
     *
     * @return  the SSLContext
     */
    public SSLContext getSSLContext()
        {
        SSLContext ctx = m_ctx;
        try
            {
            return ctx == null ? SSLContext.getDefault() : ctx;
            }
        catch (NoSuchAlgorithmException e)
            {
            throw new IllegalStateException(e);
            }
        }

    /**
     * Specify the SSLContex to utilize.
     *
     * @param ctx  the SSLContext
     *
     * @return this object
     */
    public SSLSettings setSSLContext(SSLContext ctx)
        {
        m_ctx = ctx;
        return this;
        }

    /**
     * Return the client auth mode for the produced server sockets.
     *
     * @return  the client auth mode for the produced server sockets
     */
    public SSLSocketProvider.ClientAuthMode getClientAuth()
        {
        return m_clientAuthMode;
        }

    /**
     * Specify the client authentication mode.
     *
     * @param mode  the client authentication mode
     *
     * @return this object
     */
    public SSLSettings setClientAuth(SSLSocketProvider.ClientAuthMode mode)
        {
        m_clientAuthMode = mode;
        return this;
        }

    /**
     * Return the SSL HostnameVerifier to be used to verify hostnames
     * once an SSL session has been established.
     *
     * @return  the verifier, or null to disable
     */
    public HostnameVerifier getHostnameVerifier()
        {
        return m_hostnameVerifier;
        }

    /**
     * Specify the HostnameVerifier.
     *
     * @param verifier  the HostnameVerifier
     *
     * @return this object
     */
    public SSLSettings setHostnameVerifier(HostnameVerifier verifier)
        {
        m_hostnameVerifier = verifier;
        return this;
        }

    /**
     * Return the set of enabled SSL cipher suites.
     *
     * @return  the enabled SSL cipher suites, or null for default
     */
    public String[] getEnabledCipherSuites()
        {
        return m_asCipherSuitesEnabled;
        }

    /**
     * Specify the enabled cipher suites.
     *
     * @param asCiphers  the enabled ciper suites
     *
     * @return this object
     */
    public SSLSettings setEnabledCipherSuites(String[] asCiphers)
        {
        m_asCipherSuitesEnabled = asCiphers;
        return this;
        }

    /**
     * Return the set of enabled protocol versions.
     *
     * @return  the enabled protocol versions
     */
    public String[] getEnabledProtocolVersions()
        {
        return m_asProtocolVersionsEnabled;
        }

    /**
     * Specify the enabled protocol versions.
     *
     * @param asProtocols  the enabled protocol versions
     *
     * @return this object
     */
    public SSLSettings setEnabledProtocolVersions(String[] asProtocols)
        {
        m_asProtocolVersionsEnabled = asProtocols;
        return this;
        }

    // ----- data members ---------------------------------------------------
    /**
     * The SSLContext, default is jvm ssl context.
     */
    protected SSLContext m_ctx;

    /**
     * The client authentication mode.
     */
    protected SSLSocketProvider.ClientAuthMode m_clientAuthMode;

    /**
     * The HostnameVerifier, null for default.
     */
    protected HostnameVerifier m_hostnameVerifier;

    /**
     * The enabled cipher suites or null for default.
     */
    protected String[] m_asCipherSuitesEnabled;

    /**
     * The enabled protocol versions.
     */
    protected String[] m_asProtocolVersionsEnabled;
    }
