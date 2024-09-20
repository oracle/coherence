/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.net.SocketProviderFactory;

import com.tangosol.util.Base;

import java.util.HashMap;
import java.util.Map;

/**
 * The DefaultHttpAcceptorDependencies class provides a default implementation of
 * HttpAcceptorDependencies.
 *
 * @author pfm 2011.08.25
 * @since 12.1.2
 */
public class DefaultHttpAcceptorDependencies
        extends AbstractAcceptorDependencies
        implements HttpAcceptorDependencies
    {
    // ----- constructors ---------------------------------------------------

     /**
     * Construct a DefaultHttpAcceptorDependencies object.
     */
    public DefaultHttpAcceptorDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultHttpAcceptorDependencies object, copying the values from the
     * specified HttpAcceptorDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultHttpAcceptorDependencies(HttpAcceptorDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_oHttpServer           = deps.getHttpServer();
            m_builderSocketProvider = deps.getSocketProviderBuilder();
            m_sLocalAddress         = deps.getLocalAddress();
            m_nLocalPort            = deps.getLocalPort();
            m_mapResourceConfig     = deps.getResourceConfig();
            m_sAuthMethod           = deps.getAuthMethod();
            }
        }

    // ----- DefaultHttpAcceptorDependencies methods ------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getHttpServer()
        {
        return m_oHttpServer;
        }

    /**
     * Set the HTTP server.
     *
     * @param oServer  the HTTP server
     */
    public void setHttpServer(Object oServer)
        {
        m_oHttpServer = oServer;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketProviderBuilder getSocketProviderBuilder()
        {
        return m_builderSocketProvider;
        }

    /**
     * Set the SocketProviderBuilder that may be used by the HttpAcceptor to open
     * ServerSocketChannels.
     *
     * @param builder  the socket provider builder
     */
    @Injectable("socket-provider")
    public void setSocketProviderBuilder(SocketProviderBuilder builder)
        {
        m_builderSocketProvider = builder;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLocalPort()
        {
        return m_nLocalPort;
        }

    /**
     * Set the local port.
     *
     * @param nPort  the local port
     */
    @Injectable("local-address/port")
    public void setLocalPort(int nPort)
        {
        m_nLocalPort = nPort;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalAddress()
        {
        return m_sLocalAddress;
        }

    /**
     * Set the local address.
     *
     * @param sAddress  the local address
     */
    @Injectable("local-address/address")
    public void setLocalAddress(String sAddress)
        {
        m_sLocalAddress = normalizeAddress(sAddress, getLocalPort());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getResourceConfig()
        {
        return m_mapResourceConfig;
        }

    /**
     * Set the Jersey ResourceConfig.
     *
     * @param mapConfig  the resource configuration
     */
    public void setResourceConfig(Map<String, Object> mapConfig)
        {
        m_mapResourceConfig = mapConfig;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthMethod()
        {
        return m_sAuthMethod;
        }

    /**
     * Set the client authentication method.
     *
     * @param sMethod  the client authentication method
     */
    @Injectable("auth-method")
    public void setAuthMethod(String sMethod)
        {
        m_sAuthMethod = sMethod;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultHttpAcceptorDependencies validate()
        {
        super.validate();

        Base.checkNotNull(getHttpServer(), "HttpServer");
        Base.checkNotEmpty(getLocalAddress(), "LocalAddress");
        Base.checkRange(getLocalPort(), 0, 65535, "LocalPort");

        Map<String, Object> mapConfig = getResourceConfig();
        if (mapConfig != null)
            {
            for (String sContext : mapConfig.keySet())
                {
                if (sContext == null || !sContext.startsWith("/"))
                    {
                    throw new IllegalArgumentException("illegal context path: "
                            + sContext);
                    }
                }
            }

        String sMethod = getAuthMethod();
        if (!"basic".equalsIgnoreCase(sMethod)      &&
            !"cert".equalsIgnoreCase(sMethod)       &&
            !"cert+basic".equalsIgnoreCase(sMethod) &&
            !"none".equalsIgnoreCase(sMethod))
            {
            throw new IllegalArgumentException("unsupported AuthMethod: " + sMethod);
            }

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString()
                + "{HttpServer="             + getHttpServer()
                + ", SocketProviderBuilder=" + getSocketProviderBuilder()
                + ", LocalAddress="          + getLocalAddress()
                + ", LocalPort="             + getLocalPort()
                + ", ResourceConfig="        + getResourceConfig()
                + ", AuthMethod="            + getAuthMethod()
                + "}";
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The HTTP server.
     */
    private Object m_oHttpServer;

    /**
     * The {@link SocketProviderBuilder}.
     */
    private SocketProviderBuilder m_builderSocketProvider = new SocketProviderBuilder(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER,true);

    /**
     * The local address.
     */
    private String m_sLocalAddress = normalizeAddress(null, 0);

    /**
     * The local port.
     */
    private int m_nLocalPort = 0;

    /**
     * The Jersey resource configuration.
     */
    private Map<String, Object> m_mapResourceConfig = new HashMap<>();

    /**
     * The authentication method.
     */
    private String m_sAuthMethod = "none";
    }
