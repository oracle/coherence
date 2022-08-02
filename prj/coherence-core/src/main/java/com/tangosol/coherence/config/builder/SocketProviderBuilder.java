/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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
     * @param sId            provider definition id. {@link #UNNAMED_PROVIDER_ID} indicates an inlined
     *                       anonymous socket provider
     * @param deps           SocketProvider dependencies
     * @param fCanUseGlobal  {@code true} if the global provider builder can be used over this builder
     */
    public SocketProviderBuilder(String sId, SocketProviderFactory.Dependencies deps, boolean fCanUseGlobal)
        {
        f_sId        = sId;
        f_deps       = deps;
        f_provider   = null;
        f_fUseGlobal = fCanUseGlobal;
        }

    /**
     * Wrapper an existing {@link SocketProvider} into a Builder so it can be registered in cluster BuilderRegistry.
     *
     * @param provider       a {@link SocketProvider}
     * @param fCanUseGlobal  {@code true} if the global provider builder can be used over this builder
     */
    public SocketProviderBuilder(SocketProvider provider, boolean fCanUseGlobal)
        {
        f_sId        = null;
        f_deps       = null;
        f_provider   = provider;
        f_fUseGlobal = fCanUseGlobal;
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
     * @throws NullPointerException if this builders {@link #f_deps} field is {@code null}
     */
    public SocketProvider getDemultiplexedSocketProvider(int nSubport)
        {
        if (f_deps == null)
            {
            throw new NullPointerException("The SocketProviderFactory dependencies field is null");
            }
        return f_deps.getSocketProviderFactory().getDemultiplexedSocketProvider(f_sId, f_deps, nSubport, f_fUseGlobal);
        }

    /**
     * Return an instance of the specified DatagramSocketProvider, creating it if necessary.
     *
     * @param  nSubport  subport for a demultiplexed socket provider.
     *
     * @return the provider
     * @throws NullPointerException if this builders {@link #f_deps} field is {@code null}
     */
    public DatagramSocketProvider getDatagramSocketProvider(int nSubport)
        {
        if (f_deps == null)
            {
            throw new NullPointerException("The SocketProviderFactory dependencies field is null");
            }
        return f_deps.getSocketProviderFactory().getDatagramSocketProvider(f_sId, f_deps, nSubport, f_fUseGlobal);
        }


    /**
     * Return SSLSettings for {@link SocketProviderBuilder}.
     * <p>
     * If this builder's {@link #canUseGlobal()} method returns {@code} and there is a
     * global {@link SocketProviderBuilder} configured, then the result of calling the
     * global builder's getSSLSettings() method will be returned.
     *
     * @return the sslSettings if the socket provider builder has a ssl settings directly or via delegate.
     * @throws NullPointerException if the global builder is not used and this builders {@link #f_deps}
     *                              field is {@code null}
     */
    public SSLSettings getSSLSettings()
        {
        if (canUseGlobal())
            {
            SocketProviderBuilder builder = SocketProviderFactory.getGlobalSocketProviderBuilder();
            if (builder != null)
                {
                return builder.getSSLSettings();
                }
            }

        if (f_deps == null)
            {
            throw new NullPointerException("The SocketProviderFactory dependencies field is null");
            }

        SSLSocketProvider.Dependencies depsSSL = f_deps.getSSLDependencies(f_sId);
        return depsSSL == null ? null : SocketProviderFactory.createSSLSettings(depsSSL);
        }

    /**
     * Returns {@code true} if the {@link SocketProviderFactory} can use the
     * global provider builder over this builder, if a global builder is
     * present.
     *
     * @return  {@code true} if the {@link SocketProviderFactory} can use the
     *          global provider builder over this builder
     */
    public boolean canUseGlobal()
        {
        return f_fUseGlobal;
        }

    // ----- ParameterizedBuilder methods ------------------------------------

    @Override
    public SocketProvider realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        if (f_fUseGlobal)
            {
            SocketProviderBuilder builder = SocketProviderFactory.getGlobalSocketProviderBuilder();
            if (builder != null)
                {
                return builder.realize(resolver, loader, listParameters);
                }
            }

        if (f_provider != null)
            {
            return f_provider;
            }

        if (f_deps == null)
            {
            throw new NullPointerException("The SocketProviderFactory dependencies field is null");
            }

        return f_deps.getSocketProviderFactory().getSocketProvider(f_sId, f_deps, 0);
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
    private final String f_sId;

    /**
     * Either an anonymous SocketProviderFactory dependencies for an inlined socket-provider or
     * the global SocketProviderFactory dependencies initialized from cluster socket-providers definitions.
     */
    private final SocketProviderFactory.Dependencies f_deps;

    /**
     * A Wrapped SocketProvider.
     */
    private final SocketProvider f_provider;

    /**
     * If {@code true} the {@link SocketProviderFactory} can supply the global
     * {@link SocketProviderBuilder} over this builder, if one is present.
     */
    private final boolean f_fUseGlobal;
    }
