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
import com.tangosol.net.AddressProvider;
import com.tangosol.net.ConfigurableQuorumPolicy;

import com.tangosol.run.xml.XmlElement;

import java.util.ArrayList;

import static com.tangosol.net.ConfigurableQuorumPolicy.MembershipQuorumPolicy;
import static com.tangosol.net.ConfigurableQuorumPolicy.PartitionedCacheQuorumPolicy;


/**
 * Defer cache configuration validation of an ActionPolicy until realized.
 *
 * @author jf  2015.01.29
 * @since Coherence 12.2.1
 */
public class PartitionedCacheQuorumPolicyBuilder
        extends ActionPolicyBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link PartitionedCacheQuorumPolicyBuilder} from configuration file context and xml element
     *
     * @param bldrRecoveryHostAddress Recovery Host AddressProvider builder
     */
    public PartitionedCacheQuorumPolicyBuilder(AddressProviderBuilder bldrRecoveryHostAddress, XmlElement xmlConfig)
        {
        m_bldrRecoveryHostAddress = bldrRecoveryHostAddress;
        m_aRules = new ArrayList<>(PartitionedCacheQuorumPolicy.ActionRule.values().length);
        m_xmlConfig = xmlConfig;
        }


    // ----- PartitionedCacheQuorumPolicyBuilder methods --------------------

    public void addQuorumRule(String sRuleName, int nRuleMask, int nRuleThreshold)
        {
        addQuorumRule(sRuleName, nRuleMask, nRuleThreshold, 0.0f);
        }

    public void addQuorumRule(String sRuleName, int nRuleMask, int nRuleThreshold, float flRuleThresholdPct)
        {
        m_aRules.add(new QuorumRule(sRuleName, nRuleMask, nRuleThreshold, flRuleThresholdPct, m_xmlConfig));
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionPolicy realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
            throws ConfigurationException
        {
        MembershipQuorumPolicy.QuorumRule[] rules = new MembershipQuorumPolicy.QuorumRule[m_aRules.size()];

        int i = 0;
        for (QuorumRule rule : m_aRules)
            {
            rule.validate();
            rules[i] = new MembershipQuorumPolicy.QuorumRule(rule.m_nRuleMask, rule.m_nThreshold, rule.m_flThresholdPct);
            i++;
            }

        AddressProvider recoveryHostAddress = m_bldrRecoveryHostAddress == null ? null :
            m_bldrRecoveryHostAddress.realize(resolver, loader, listParameters);

        return ConfigurableQuorumPolicy.instantiatePartitionedCachePolicy(rules, recoveryHostAddress);
        }

    // ----- data members ---------------------------------------------------

    /**
     * ActionPolicy rules with xml configuration info to enable constructing
     * informative ConfigurationException message if a constraint is violated.
     */
    private ArrayList<QuorumRule> m_aRules;

    /**
     * RecoveryHost AddressBuilder
     */
    private AddressProviderBuilder m_bldrRecoveryHostAddress;

    /**
     * Optional xml configuration for reporting CacheConfiguration error.
     */
    private XmlElement m_xmlConfig;
    }
