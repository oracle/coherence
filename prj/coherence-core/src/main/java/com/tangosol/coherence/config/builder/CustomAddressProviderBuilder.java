/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.AddressProvider;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.NullImplementation;

/**
 * This class builds an AddressProviderBuilder from a customized {@link ParameterizedBuilder} of
 * {@link AddressProvider}.
 *
 * @author jf  2015.02.26
 * @since Coherence 12.2.1
 */
public class CustomAddressProviderBuilder
        implements AddressProviderBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link CustomAddressProviderBuilder}
     *
     * @param builder customized AddressProvider
     */
    public CustomAddressProviderBuilder(ParameterizedBuilder<AddressProvider> builder)
        {
        this(builder, new NullParameterResolver(), null);
        }

    /**
     * Constructs {@link CustomAddressProviderBuilder}
     *
     * @param builder customized AddressProvider
     * @param resolver optional resolver
     */
    public CustomAddressProviderBuilder(ParameterizedBuilder<AddressProvider> builder, ParameterResolver resolver)
        {
        this(builder, resolver, null);
        }

    /**
     * Constructs {@link CustomAddressProviderBuilder}
     *
     * @param builder customized AddressProvider
     * @param resolver resolver
     * @param xmlConfig optional xmlConfig info to only be used in reporting
     *                  {@link ConfigurationException}.
     */
    public CustomAddressProviderBuilder(ParameterizedBuilder<AddressProvider> builder, ParameterResolver resolver, XmlElement xmlConfig)
        {
        m_builder = builder;
        m_resolver = resolver;
        m_xmlConfig = xmlConfig;
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * Realize the custom builder.
     *
     * @param resolver if non-null, use it.  otherwise use resolver provided at construction time.
     * @param loader classloader
     * @param listParameters list of parameters.
     */
    @Override
    public AddressProvider realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        try
            {
            return m_builder == null ? NullImplementation.getAddressProvider() : m_builder.realize(resolver == null ? m_resolver : resolver, loader, listParameters);
            }
        catch (ClassCastException e)
            {
            StringBuilder sb = new StringBuilder();
            sb.append("invalid customized AddressProviderBuilder ").append(m_builder.getClass().getCanonicalName());
            if (m_xmlConfig != null)
                {
                sb.append(" configured in element <").append(m_xmlConfig).append(">");
                }
            throw new ConfigurationException(sb.toString(),
                                             "fix configuration to reference a class that returns an AddressProvider", e);
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

    // ----- data members ---------------------------------------------------

    /**
     * custom builder
     */
    private ParameterizedBuilder<AddressProvider> m_builder;

    /**
     * resolver
     */
    private ParameterResolver m_resolver;

    /**
     * optional xml configuration info. only to be used to enhance
     * configuration exception.
     */
    private XmlElement m_xmlConfig = null;
    }
