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
 * {@link InvalidConfigServiceLoadBalancerBuilder} defers reporting
 * configuration exception until realized.  Enables non-autostarted
 * services to be improperly configured and configuration error reporting
 * performed only if containing service is started.
 *
 * @author jf  2015.02.10
 * @since Coherence 12.2.1
 */
public class InvalidConfigServiceLoadBalancerBuilder
        extends ServiceLoadBalancerBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link InvalidConfigServiceLoadBalancerBuilder}. Defer reporting
     * configuration exception until instantiated.
     *
     * @param sServiceKind invalid load-balancer default value or used on invalid scheme
     * @param xmlConfig  xml config element
     */
    public InvalidConfigServiceLoadBalancerBuilder(String sServiceKind, XmlElement xmlConfig)
        {
        super(null, xmlConfig);

        StringBuilder msg = new StringBuilder();

        msg.append("Unknown load balancer [").append(sServiceKind).append("]");

        if (xmlConfig != null)
            {
            msg.append(" specified in xml element [").append(xmlConfig).append("].");
            }

        m_eDeferred = new ConfigurationException(msg.toString(), "Please specify a known load balancer");
        }

    // ----- ServiceLoadBalancerBuilder methods -----------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceLoadBalancer getDefaultLoadBalancer()
        {
        return null;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceLoadBalancer realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        throw m_eDeferred;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Only non-null when configuration processing results in an exception.
     * Defer throwing ConfigurationException until instance is realized.
     */
    private final ConfigurationException m_eDeferred;
    }
