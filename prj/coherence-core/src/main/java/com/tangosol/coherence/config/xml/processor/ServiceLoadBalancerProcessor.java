/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.InvalidConfigServiceLoadBalancerBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.builder.ProxyServiceLoadBalancerBuilder;
import com.tangosol.coherence.config.builder.ServiceLoadBalancerBuilder;
import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} for &lt;load-balancer&gt; configuration used
 * by federated and proxy services.
 *
 * @author bb  2014.04.03
 */
@XmlSimpleName("load-balancer")
public class ServiceLoadBalancerProcessor
        implements ElementProcessor<ServiceLoadBalancerBuilder>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceLoadBalancerBuilder process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        String sLoadBalancer = xmlElement.getString().trim();

        // compute default load balanced service type from context
        if (sLoadBalancer == null || sLoadBalancer.length() == 0)
            {
            String sScheme = xmlElement.getParent().getQualifiedName().getLocalName();
            switch (sScheme)
                {
                case FEDERATED_SCHEME:
                    sLoadBalancer = FEDERATION;
                    break;

                case PROXY_SCHEME:
                    sLoadBalancer = PROXY;
                    break;

                default:
                    sLoadBalancer = "no default load balancer for scheme " + sScheme;
                }
            }

        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);
        switch (sLoadBalancer)
            {
            case PROXY:
                return new ProxyServiceLoadBalancerBuilder(bldr, xmlElement);

            case CLIENT:
                return null;

            default:
                return new InvalidConfigServiceLoadBalancerBuilder(sLoadBalancer, xmlElement);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Federated-scheme.
     */
    public static final String FEDERATED_SCHEME = "federated-scheme";

    /**
     * Proxy scheme.
     */
    public static final String PROXY_SCHEME = "proxy-scheme";

    /**
     * Federation option for the service load balancer.
     */
    public static final String FEDERATION = "federation";

    /**
     * Proxy option for the service load balancer.
     */
    public static final String PROXY = "proxy";

    /**
     * Client option for the service load balancer.
     */
    public static final String CLIENT = "client";
    }
