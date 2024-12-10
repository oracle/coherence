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
import com.tangosol.net.proxy.DefaultProxyServiceLoadBalancer;
import com.tangosol.net.proxy.ProxyServiceLoadBalancer;

import com.tangosol.run.xml.XmlElement;

/**
 * {@link ProxyServiceLoadBalancerBuilder} defers evaluating configuration parameters
 * until ServiceLoadBalancer is instantiated.
 *
 * @author jf  2015.02.10
 * @since Coherence 12.2.1
 */
public class ProxyServiceLoadBalancerBuilder
        extends ServiceLoadBalancerBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link ProxyServiceLoadBalancerBuilder}
     *
     * @param bldr optional customized ProxyServiceLoadBalancer
     * @param xmlConfig optional configuration element for reporting configuration exception.
     */
    public ProxyServiceLoadBalancerBuilder(ParameterizedBuilder bldr, XmlElement xmlConfig)
        {
        super(bldr, xmlConfig);
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceLoadBalancer realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        try
            {
            ProxyServiceLoadBalancer loadBalancer = (ProxyServiceLoadBalancer) super.realize(resolver, loader, listParameters);
            return loadBalancer;
            }
        catch (ClassCastException e)
            {
                throw new ConfigurationException("invalid ProxyServiceLoadBalancer configuration for element <"
                                                 + f_xmlConfig + ">", "Provide a ProxyServiceLoadBalancer", e);
            }
        }

    // ----- ServiceLoadBalancerBuilder methods -----------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceLoadBalancer getDefaultLoadBalancer()
        {
        return new DefaultProxyServiceLoadBalancer();
        }
    }
