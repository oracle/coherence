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

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.NullImplementation;

/**
 * The abstract {@link ActionPolicyBuilder} is a base class builder for building {@link ActionPolicy}'s instances
 * and defers cache configuration validation until the instance is realized.
 * The xml content held by the builder is only used to construct an informative message for ConfigurationException.
 * <p>
 * This class removes fail fast cache configuration and replaces it with lazy evaluation to be more
 * like Coherence instantiation in 12.1.2 and prior.
 *
 * @author jf  2015.02.02
 * @since Coherence 12.2.1
 */
public abstract class ActionPolicyBuilder
        implements ParameterizedBuilder<ActionPolicy>
    {

    // ----- inner classes --------------------------------------------------

    /**
     * {@link ActionPolicyBuilder} wrapper for a ParameterizedBuilder.
     */
    static public class ActionPolicyParameterizedBuilder
            extends ActionPolicyBuilder
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs {@link ActionPolicyParameterizedBuilder}
         *
         * @param bldr customized ActionPolicy ParameterizedBuilder.
         */
        public ActionPolicyParameterizedBuilder(ParameterizedBuilder<ActionPolicy> bldr)
            {
            m_builder = bldr;
            }

        // ----- ParameterizedBuilder methods -------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public ActionPolicy realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                throws ConfigurationException
            {
            return m_builder.realize(resolver, loader, listParameters);
            }

        // ----- data members --------------------------------------------

        /**
         * underlying ParameterizedBuilder
         */
        private ParameterizedBuilder<ActionPolicy> m_builder;
        }

    /**
     * ActionPolicy Null Implementation
     */
    static public class NullImplementationBuilder
            extends ActionPolicyBuilder
        {
        // ----- ParameterizedBuilder methods -------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public ActionPolicy realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                throws ConfigurationException
            {
            return NullImplementation.getActionPolicy();
            }
        }

    /**
     * Intermediate QuorumRule with enough information to report a ConfigurationException
     * at instantiation time.
     */
    public static class QuorumRule
        {
        // ----- Constructors -----------------------------------------------

        /**
         * Constructs {@link QuorumRule}
         *
         * @param sRuleElementName  to report configuration exception context
         * @param nRuleMask         rule mask
         * @param nThreshold        threshold
         * @param xmlElement        optional to report configuration exception context
         */
        public QuorumRule(String sRuleElementName, int nRuleMask, int nThreshold, XmlElement xmlElement)
            {
            this(sRuleElementName, nRuleMask, nThreshold, 0.0f, xmlElement);
            }

        /**
         * Constructs {@link QuorumRule}
         *
         * @param sRuleElementName  to report configuration exception context
         * @param nRuleMask         rule mask
         * @param nThreshold        threshold
         * @param flThresholdPct    the threshold in percentage
         * @param xmlElement        optional to report configuration exception context
         */
        public QuorumRule(String sRuleElementName, int nRuleMask, int nThreshold, float flThresholdPct, XmlElement xmlElement)
            {
            m_sRuleElementName = sRuleElementName;
            m_nRuleMask        = nRuleMask;
            m_nThreshold       = nThreshold;
            m_flThresholdPct   = flThresholdPct;
            m_xmlElement       = xmlElement;
            }

        // ----- QuorumRule methods -----------------------------------------

        /**
         * Throw ConfigurationException if this {@link #m_xmlElement} configuration violates constraints.
         *
         * @throws ConfigurationException describing invalid configuration.
         */
        public void validate()
                throws ConfigurationException
            {
            if (m_nThreshold < 0 || m_flThresholdPct < 0.0f || m_flThresholdPct > 1.0f)
                {
                String sValue = m_nThreshold < 0
                               ? String.valueOf(m_nThreshold)
                               : m_flThresholdPct * 100 + "%";

                throw new ConfigurationException(
                    "Invalid value [" + sValue  + "] for <" + m_sRuleElementName
                    + "> in [" + m_xmlElement + "]", "The <" + m_sRuleElementName
                    + "> must be non-negative and less than 100% if percentage is specified.");
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * An optional xml configuration element to be used in ConfigurationException.
         */
        protected XmlElement m_xmlElement;

        /**
         * A rule element name to be used to construct ConfigurationException
         * description if it throws at instantiation time.
         */
        protected String m_sRuleElementName;

        /**
         * Action policy rule mask.
         */
        protected int m_nRuleMask;

        /**
         * Action policy threshold which is always non-negative.
         */
        protected int m_nThreshold;

        /**
         * Action policy threshold in percentage.
         */
        protected float m_flThresholdPct;
        }
    }
