/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.ConfigurableAddressProvider;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.RefreshableAddressProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * This class builds an AddressProviderBuilder from a list of address and port.
 *
 * @author jf  2015.02.26
 * @since Coherence 12.2.1
 */
public class ListBasedAddressProviderBuilder
        implements AddressProviderBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link ListBasedAddressProviderBuilder}
     */
    public ListBasedAddressProviderBuilder()
        {
        m_listAddress = new ArrayList<>();
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public AddressProvider realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        validate();
        ConfigurableAddressProvider provider = new ConfigurableAddressProvider(m_listAddress, true);

        return m_fRefreshable ? new RefreshableAddressProvider(provider) : provider;
        }

    // ----- AddressProvideBuilder methods ----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public AddressProvider createAddressProvider(ClassLoader loader)
        {
        return realize(null, loader, null);
        }

    // ----- AddressListProviderBuilder methods -----------------------------

    /**
     * Add an address and port.
     *
     * @param sAddr either an ip address or a host name
     * @param nPort a non-negative port
     */
    public ListBasedAddressProviderBuilder add(String sAddr, int nPort)
        {
        m_fRefreshable |= (sAddr != null && InetAddressHelper.isHostName(sAddr));
        m_listAddress.add(new ConfigurableAddressProvider.AddressHolder(sAddr, nPort));

        return this;
        }

    /**
     * Returns true if any of the added addresses has been computed to be a hostname.
     * <P>
     * Introduced for unit testing.
     *
     * @return true iff the realized AddressProvider will refresh its list.
     */
    public boolean isRefreshable()
        {
        return m_fRefreshable;
        }

    /**
     * Returns {@code true} if this builder contains no addresses.
     *
     * @return {@code true} if this builder contains no addresses
     */
    public boolean isEmpty()
        {
        return m_listAddress.isEmpty();
        }

    // ----- helpers --------------------------------------------------------

    private void validate()
        {
        for (ConfigurableAddressProvider.AddressHolder holder : m_listAddress)
            {
            try
                {
                holder.validate();
                }
            catch (Exception e)
                {
                throw new ConfigurationException("invalid address in AddressListProviderBuilder", "fix constraint violation", e);
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * list of addresses.
     */
    private final List<ConfigurableAddressProvider.AddressHolder> m_listAddress;

    /**
     * Computed value that is true if one of the address' added to the builder
     * is a host name.
     */
    private boolean m_fRefreshable = false;
    }
