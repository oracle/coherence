/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.SocketAddressProvider;
import com.tangosol.net.internal.WrapperSocketAddressProvider;

/**
 * The WrapperSocketAddressProviderBuilder wraps an AddressProviderBuilder so
 * that it can produce a SocketAddressProvider.
 *
 * @author pfm  2013.09.13
 * @since Coherence 12.1.3
 */
public class WrapperSocketAddressProviderBuilder
        implements ParameterizedBuilder<SocketAddressProvider>
    {
    // ----- constructors  --------------------------------------------------

    /**
     * Construct a WrapperSocketAddressProviderBuilder.
     *
     * @param bldr  the AddressProviderBuilder to wrap
     */
    public WrapperSocketAddressProviderBuilder(AddressProviderBuilder bldr)
        {
        if (bldr == null)
            {
            throw new IllegalArgumentException("The AddressProviderBuilder cannot be null");
            }

        m_bldrInner = bldr;
        }

    // ----- ParameterizedBuilder interface  --------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddressProvider realize(ParameterResolver resolver,
            ClassLoader loader, ParameterList listParameters)
        {
        AddressProvider provider = m_bldrInner.realize(resolver, loader, listParameters);

        if (isEphemeral())
            {
            return WrapperSocketAddressProvider
                    .createEphemeralSubPortSocketAddressProvider(provider);
            }
        else
            {
            // Must use Wrapper since it forces address to be InetSocketAddress32 (rather than InetSocketAddress).
            // Oracle commons MultiplexedSocketProvider will reject the address if it is not InetSocketAddress32.
            return provider instanceof WrapperSocketAddressProvider
                    ? provider
                    : new WrapperSocketAddressProvider(provider);
            }
        }


    /**
     * Set the flag indicating that the addresses should be ephemeral.
     *
     * @param fEphemeral  use an ephemeral address
     *
     * @return this object
     */
    public WrapperSocketAddressProviderBuilder setEphemeral(boolean fEphemeral)
        {
        m_fEphemeral = fEphemeral;

        return this;
        }

    /**
     * Return the flag indicating that the addresses should be ephemeral.
     *
     * @return this object
     */
    public boolean isEphemeral()
        {
        return m_fEphemeral;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The AddressProvider builder.
     */
    private AddressProviderBuilder m_bldrInner;

    /**
     * True if the address should be wrapped with a ephemeral provider.
     */
    private boolean m_fEphemeral = false;

    }
