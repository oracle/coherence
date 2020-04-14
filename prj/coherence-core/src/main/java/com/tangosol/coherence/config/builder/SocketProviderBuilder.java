/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.net.SSLSettings;
import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.DatagramSocketProvider;
import com.tangosol.net.SocketProviderFactory;

/**
 * {@link SocketProviderBuilder} enables lazy instantiation of SocketProvider.
 * Builder includes methods that allows one to specify whether to get a datagram or demultiplexed
 * {@link SocketProvider} and what subport to use for the socket provider.
 *
 * @author jf  2015.11.11
 * @since Coherence 12.2.1.1
 */
public class SocketProviderBuilder implements ParameterizedBuilder<SocketProvider>
    {
    // ----- Constructors ----------------------------------------------------

    /**
     * Construct a {@link SocketProviderBuilder} from its definition id and its dependencies.
     *
     * @param sId  provider definition id. {@link #UNNAMED_PROVIDER_ID} indicates an inlined anonymous socket provider
     * @param deps SocketProvider dependencies
     */
    public SocketProviderBuilder(String sId, SocketProviderFactory.Dependencies deps)
        {
        f_sId      = sId;
        f_deps     = deps;
        f_provider = null;
        }

    /**
     * Wrapper an existing {@link SocketProvider} into a Builder so it can be registered in cluster BuilderRegistry.
     *
     * @param provider a SocketProvider
     */
    public SocketProviderBuilder(SocketProvider provider)
        {
        f_sId      = null;
        f_deps     = null;
        f_provider = provider;
        }

    // ----- SocketProviderBuilder methods -----------------------------------

    /**
     * Return either an anonymous SocketProviderFactory dependencies for an inlined socket-provider or
     * the global SocketProviderFactory dependencies initialized from cluster socket-providers definitions.
     *
     * @return {@link com.tangosol.net.SocketProviderFactory.Dependencies} for this builder
     */
    public SocketProviderFactory.Dependencies getDependencies()
        {
        return f_deps;
        }

    /**
     * Return the identifier for SocketProvider built by this builder.
     *
     * @return the identifier for {@link SocketProvider} returned by this builder.
     */
    public String getId()
        {
        return f_sId;
        }

    /**
     * Return a Demultiplexed Socket provider
     *
     * @param nSubport  subport for demultiplexed socket provider.
     *
     * @return the provider
     */
    public SocketProvider getDemultiplexedSocketProvider(int nSubport)
        {
        return f_deps.getSocketProviderFactory().getDemultiplexedSocketProvider(f_sId, f_deps, nSubport);
        }

    /**
     * Return an instance of the specified DatagramSocketProvider, creating it if necessary.
     *
     * @param  nSubport  subport for a demultiplexed socket provider.
     *
     * @return the provider
     */
    public DatagramSocketProvider getDatagramSocketProvider(int nSubport)
        {
        return f_deps.getSocketProviderFactory().getDatagramSocketProvider(f_sId, f_deps, nSubport);
        }


    /**
     * Return SSLSettings for {@link SocketProviderBuilder}.
     *
     * @return the sslSettings if the socket provider builder has a ssl settings directly or via delegate.
     */
    public SSLSettings getSSLSettings()
        {
        SSLSocketProvider.Dependencies depsSSL = f_deps.getSSLDependencies(f_sId);
        return depsSSL == null ? null : SocketProviderFactory.createSSLSettings(depsSSL);
        }

    // ----- ParameterizedBuilder methods ------------------------------------

    @Override
    public SocketProvider realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        return f_provider == null ?  f_deps.getSocketProviderFactory().getSocketProvider(f_sId, f_deps, 0) : f_provider;
        }

    // ----- constants -------------------------------------------------------

    /**
     * Default id for unnamed socket providers.
     */
    public static String UNNAMED_PROVIDER_ID = SocketProviderFactory.UNNAMED_PROVIDER_ID;

    // ----- data members ----------------------------------------------------

    /**
     * SocketProvider definition id
     */
    private final String                             f_sId;

    /**
     * Either an anonymous SocketProviderFactory dependencies for an inlined socket-provider or
     * the global SocketProviderFactory dependencies initialized from cluster socket-providers definitions.
     */
    private final SocketProviderFactory.Dependencies f_deps;

    /**
     * A Wrapped SocketProvider.
     */
    private final SocketProvider                     f_provider;
    }
