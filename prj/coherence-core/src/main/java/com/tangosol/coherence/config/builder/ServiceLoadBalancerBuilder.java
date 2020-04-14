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

import com.tangosol.net.ServiceLoadBalancer;

import com.tangosol.run.xml.XmlElement;

/**
 * {@link ServiceLoadBalancerBuilder} defers evaluating configuration parameters
 * until ServiceLoadBalancer is instantiated.
 *
 * @author jf  2015.02.10
 * @since Coherence 12.2.1
 */
public abstract class ServiceLoadBalancerBuilder
        implements ParameterizedBuilder<ServiceLoadBalancer>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link ServiceLoadBalancerBuilder}
     *
     * @param builder optional customization of ServiceLoadBalancer.  if null, use {@link #getDefaultLoadBalancer}.
     */
    public ServiceLoadBalancerBuilder(ParameterizedBuilder<ServiceLoadBalancer> builder, XmlElement xmlConfig)
        {
        m_builder = builder;
        f_xmlConfig = xmlConfig;
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceLoadBalancer realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        return m_builder == null ? getDefaultLoadBalancer() : m_builder.realize(resolver, loader, listParameters);
        }

    // ----- ServiceLoaderBalancerBuilder methods ---------------------------

    /**
     * Use this {@link ServiceLoadBalancer} when a customized ServiceLoadBalancer is not provided.
     *
     * @return default ServiceLoadBalancer
     */
    abstract public ServiceLoadBalancer getDefaultLoadBalancer();

    // ----- data members ---------------------------------------------------

    /**
     * Customized ServiceLoadBalancerBuilder.
     */
    protected ParameterizedBuilder<ServiceLoadBalancer> m_builder = null;

    /**
     * Xml Configuration Element to use to report ConfigurationException.
     * This element is optional.
     */
    protected final XmlElement f_xmlConfig;
    }
