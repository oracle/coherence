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

import static com.tangosol.net.ConfigurableQuorumPolicy.MembershipQuorumPolicy;

/**
 * Defer cache configuration validation of a ProxyQuorumPolicy until realized.
 *
 * @author jf  2015.02.05
 * @since Coherence 12.2.1
 */
public class ProxyQuorumPolicyBuilder
        extends ActionPolicyBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     *     Constructs {@link ProxyQuorumPolicyBuilder} from configuration file context and xml element
     *
     *     @param nRuleThreshold  connect quorum rule threshold
     *     @param xmlConfig      containing element proxy-cache-quorum-policy
     */
    public ProxyQuorumPolicyBuilder(int nRuleThreshold, XmlElement xmlConfig)
        {
        m_aRule = new QuorumRule(CONNECT_RULE_NAME, MASK_CONNECT, nRuleThreshold, xmlConfig);
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionPolicy realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
            throws ConfigurationException
        {
        m_aRule.validate();

        MembershipQuorumPolicy.QuorumRule rules[] =
            {
            new MembershipQuorumPolicy.QuorumRule(m_aRule.m_nRuleMask, m_aRule.m_nThreshold)
            };

        return ConfigurableQuorumPolicy.MembershipQuorumPolicy.instantiateProxyPolicy(rules);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Connect quorum mask.
     */
    public static final int MASK_CONNECT = ConfigurableQuorumPolicy.ProxyQuorumPolicy.MASK_CONNECT;

    /**
     * Connect description and also xml configuration element name.
     */
    public static final String CONNECT_RULE_NAME = "connect-quorum";

    // ----- data members ---------------------------------------------------

    /**
     * ActionPolicy rules with xml configuration info to enable constructing
     * informative {@link ConfigurationException} message if a constraint is violated.
     */
    private QuorumRule m_aRule;
    }
