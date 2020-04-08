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

import com.tangosol.internal.net.cluster.DefaultServiceFailurePolicy;

import com.tangosol.net.ServiceFailurePolicy;

import com.tangosol.run.xml.XmlElement;

import static com.tangosol.internal.net.cluster.DefaultServiceFailurePolicy.POLICY_EXIT_CLUSTER;
import static com.tangosol.internal.net.cluster.DefaultServiceFailurePolicy.POLICY_EXIT_PROCESS;
import static com.tangosol.internal.net.cluster.DefaultServiceFailurePolicy.POLICY_LOGGING;

/**
 * Build a default or customized {@link ServiceFailurePolicy}.
 * <P>
 *  Defer configuration exception reporting until instantiated.
 *
 * @author jf  2015.02.24
 * @since Coherence 12.2.1
 */
public class ServiceFailurePolicyBuilder
        implements ParameterizedBuilder<ServiceFailurePolicy>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create Defaults.
     *
     * @param nPolicy one of {@link DefaultServiceFailurePolicy#POLICY_EXIT_CLUSTER}, {@link DefaultServiceFailurePolicy#POLICY_EXIT_PROCESS} or
     *                {@link DefaultServiceFailurePolicy#POLICY_LOGGING}
     */
    public ServiceFailurePolicyBuilder(int nPolicy)
        {
        m_xmlConfig = null;
        m_builder   = null;

        String sPolicy;

        switch (nPolicy)
            {
            case POLICY_EXIT_CLUSTER :
                sPolicy = "exit-cluster";
                break;

            case POLICY_EXIT_PROCESS :
                sPolicy = "exit-process";
                break;

            case POLICY_LOGGING :
                sPolicy = "logging";
                break;

            default :
                sPolicy = "unknown default ServiceFailurePolicy number " + nPolicy;
            }

        m_sPolicyDefault = sPolicy;
        }

    /**
     * {@link ServiceFailurePolicy} constructor for customized builder.
     * @param builder customized builder
     * @param xmlConfig optional configuration element for reporting configuration error.
     */
    public ServiceFailurePolicyBuilder(ParameterizedBuilder<ServiceFailurePolicy> builder, XmlElement xmlConfig)
        {
        m_builder        = builder;
        m_sPolicyDefault = null;
        m_xmlConfig      = xmlConfig;
        }

    /**
     * Default ServiceFailurePolicy from Xml value in configuration &lt;service-failure-policy&gt;.
     *
     * @param sPolicy default ServiceFailurePolicy {@link com.tangosol.internal.net.cluster.DefaultServiceFailurePolicy}
     * @param xmlConfig optional configuration element for reporting configuration error.
     */
    public ServiceFailurePolicyBuilder(String sPolicy, XmlElement xmlConfig)
        {
        m_builder        = null;
        m_sPolicyDefault = sPolicy;
        m_xmlConfig      = xmlConfig;
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceFailurePolicy realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        if (m_builder == null)
            {
            int nPolicyType;

            switch (m_sPolicyDefault)
                {
                case "exit-cluster" :

                    nPolicyType = DefaultServiceFailurePolicy.POLICY_EXIT_CLUSTER;
                    break;

                case "exit-process" :

                    nPolicyType = DefaultServiceFailurePolicy.POLICY_EXIT_PROCESS;
                    break;

                case "logging" :

                    nPolicyType = DefaultServiceFailurePolicy.POLICY_LOGGING;
                    break;

                default :
                    {
                    StringBuilder sb = new StringBuilder();

                    sb.append("Unknown default service failure policy: :").append(m_sPolicyDefault);

                    if (m_xmlConfig != null)
                        {
                        sb.append(" in [").append(m_xmlConfig).append("].");
                        }

                    throw new ConfigurationException(sb.toString(),
                        "Please specify a default ServiceFailurePolicy value of exit-cluster, exit-process or logging");
                    }
                }

            return new DefaultServiceFailurePolicy(nPolicyType);
            }
        else
            {
            try
                {
                return m_builder.realize(resolver, loader, listParameters);
                }
            catch (ClassCastException e)
                {
                StringBuilder sb = new StringBuilder();

                sb.append("Invalid customized ServiceFailurePolicy class ")
                    .append(m_builder.getClass().getCanonicalName());

                if (m_xmlConfig != null)
                    {
                    sb.append(" in [").append(m_xmlConfig).append("]");
                    }

                throw new ConfigurationException(sb.toString(),
                                                 "Provide a customized class that implements ServiceFailurePolicy ", e);
                }
            }

        }

    // ----- constants ------------------------------------------------------

    /**
     * Default service failure policy. Null if customized builder provided.
     */
    private final String m_sPolicyDefault;

    /**
     * Customized Builder. Null if Default service failure policy provided.
     */
    private final ParameterizedBuilder<ServiceFailurePolicy> m_builder;

    /**
     * Optional xml configuration. Used in ConfigurationException message if non-null.
     */
    private final XmlElement m_xmlConfig;
    }
