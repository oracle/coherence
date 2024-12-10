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

import com.tangosol.net.ActionPolicy;
import com.tangosol.net.ConfigurableQuorumPolicy;

import com.tangosol.run.xml.XmlElement;

import java.util.Map;

import static com.tangosol.net.ConfigurableQuorumPolicy.ClusterQuorumPolicy;

/**
 * Defer cache configuration validation of a {@link ClusterQuorumPolicyBuilder} until realized.
 *
 * @author jf  2015.02.04
 * @since Coherence 12.2.1
 */
public class ClusterQuorumPolicyBuilder
        extends ActionPolicyBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link ClusterQuorumPolicyBuilder} from <code>mapQuorum</code>
     *
     *  @param mapQuorum  map of role to quorum threshold
     *  @param xmlConfigElement optional configuration element cluster-quorum-policy used for reporting
     *                          configuration exception context.
     */
    public ClusterQuorumPolicyBuilder(Map<String, Integer> mapQuorum, XmlElement xmlConfigElement)
        {
        m_mapQuorum = mapQuorum;
        m_xmlConfig = xmlConfigElement;
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionPolicy realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
            throws ConfigurationException
        {
        validate();
        return ConfigurableQuorumPolicy.instantiateClusterPolicy(m_mapQuorum);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Validate cluster-quorum-configuration at instantiation time.
     *
     * @throws IllegalArgumentException if any value in m_mapQuorum map is negative.
     */
    private void validate()
            throws IllegalArgumentException
        {
        for (Map.Entry<String, Integer> entry : m_mapQuorum.entrySet())
            {
            if (entry.getValue() < 0)
                {
                throw new IllegalArgumentException("The cluster-quorum for role " + entry.getKey()
                                                   + " must be non-negative in configuration element <"
                                                   + m_xmlConfig + ">.");
                }

            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Map of role and quorum.  The role can be {@link ClusterQuorumPolicy#ROLE_ALL}.
     */
    private Map<String, Integer> m_mapQuorum;


    /**
     * Optional configuration element to assist in reporting ConfigurationException.
     */
    private XmlElement m_xmlConfig;
    }
