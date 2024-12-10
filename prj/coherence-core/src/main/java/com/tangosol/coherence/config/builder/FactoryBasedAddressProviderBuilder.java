/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.AddressProviderFactory;
import com.tangosol.util.NullImplementation;

/**
 * This class builds an AddressProviderBuilder from a AddressProviderFactory.
 *       
 * @author jf  2015.02.26
 * @since Coherence 12.2.1
 */
public class FactoryBasedAddressProviderBuilder
        implements AddressProviderBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link FactoryBasedAddressProviderBuilder}
     *
     *
     * @param factory wrapped factory
     */
    public FactoryBasedAddressProviderBuilder(AddressProviderFactory factory)
        {
        m_factory = factory;
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public AddressProvider realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        if (m_eDeferred != null)
            {
            throw m_eDeferred;
            }
        try
            {
            return m_factory == null ? NullImplementation.getAddressProvider() : m_factory.createAddressProvider(loader);
            }
        catch (ClassCastException e)
            {
            throw new ConfigurationException("AddressProviderFactory must return a AddressProvider",
                                             "fix configuration so the referenced class implements AddressProviderFactory", e);
            }
        }

    // ----- AddressProviderFactory methods ---------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public AddressProvider createAddressProvider(ClassLoader loader)
        {
        return realize(null, loader, null);
        }

    // ----- AddressProviderFactoryBuilder methods --------------------------

    /**
     * Defer reporting ConfigurationException until realized.
     *
     * @param e ConfigurationException to throw when instantiated.
     */
    public AddressProviderBuilder setDeferredException(ConfigurationException e)
        {
        m_eDeferred = e;
        return this;
        }

    // ----- data members ---------------------------------------------------

    /**
     * wrapped factory
     */
    private AddressProviderFactory m_factory = null;

    /**
     * defer configuration exception until realized.
     */
    private ConfigurationException m_eDeferred = null;
    }
