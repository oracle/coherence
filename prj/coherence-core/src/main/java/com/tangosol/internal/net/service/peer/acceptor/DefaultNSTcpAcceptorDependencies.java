/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.coherence.config.Config;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NameService;

import com.tangosol.net.SocketProviderFactory;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;

import java.net.InetAddress;

import java.net.UnknownHostException;

/**
 * The DefaultNSTcpAcceptorDependencies class provides a default implementation of
 * {@link TcpAcceptorDependencies} for use by the {@link NameService}'s Acceptor.
 *
 * @author phf 2014.07.10
 *
 * @since Coherence 12.2.1
 */
public class DefaultNSTcpAcceptorDependencies
        extends DefaultTcpAcceptorDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultNSTcpAcceptorDependencies object.
     */
    public DefaultNSTcpAcceptorDependencies()
        {
        this(null);

        // NS isn't SSL protected, limit the request sizes it accepts to prevent OOME based DOS attacks
        setMaxIncomingMessageSize(4096);
        }

    /**
     * Construct a DefaultNSTcpAcceptorDependencies object, copying the values from the
     * specified {@link TcpAcceptorDependencies} object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultNSTcpAcceptorDependencies(TcpAcceptorDependencies deps)
        {
        super(deps);

        if (deps == null || deps.getMaxIncomingMessageSize() == 0)
            {
            // NS isn't SSL protected, limit the request sizes it accepts to prevent OOME based DOS attacks
            setMaxIncomingMessageSize(4096);
            }
        }

    // ----- DefaultTcpAcceptorDependencies methods -------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected InetAddress getDefaultHost()
        {
        String sPropAddr = Config.getProperty("coherence.nameservice.address");
        if (sPropAddr != null)
            {
            try
                {
                return InetAddress.getByName(sPropAddr);
                }
            catch (UnknownHostException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }

        return CacheFactory.getCluster().getDependencies().getLocalDiscoveryAddress();
        }

    @Override
    protected SocketProviderBuilder createDefaultSocketProviderBuilder()
        {
        // never allow the NameService default provider to be overridden by the global provider
        return new SocketProviderBuilder(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER, false);
        }

    @Override
    public boolean canUseGlobalSocketProvider()
        {
        return false;
        }
    }
