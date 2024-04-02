/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.AddressProviderBuilder;
import com.tangosol.coherence.config.builder.FactoryBasedAddressProviderBuilder;
import com.tangosol.coherence.config.builder.ClusterQuorumPolicyBuilder;
import com.tangosol.coherence.config.builder.PartitionedCacheQuorumPolicyBuilder;
import com.tangosol.coherence.config.builder.ProxyQuorumPolicyBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.internal.net.LegacyXmlConfigHelper;

import com.tangosol.net.ActionPolicy;
import com.tangosol.net.AddressProviderFactory;
import com.tangosol.net.ConfigurableQuorumPolicy;
import com.tangosol.net.ConfigurableQuorumPolicy.ClusterQuorumPolicy;
import com.tangosol.net.ConfigurableQuorumPolicy.PartitionedCacheQuorumPolicy.ActionRule;
import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;

import com.tangosol.util.Base;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * LegacyXmlConfigurableQuorumPolicy parses the Quorum policy XML, <cluster-quorum-policy>
 * and injects dependencies in the Quorum related classes.
 *
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author pfm  2011.05.08
 * @since Coherence 3.7.1
 */
public class LegacyXmlConfigurableQuorumPolicy
    {
    /**
     * Create an action policy based on the specified XML configuration.
     *
     * @param xmlConfig     the cluster-quorum-policy XML element
     * @param loader        the class loader to instantiate the policy
     *
     * @return a ConfigurableQuorumPolicy instance
     */
    public ActionPolicyBuilder createPolicyBuilder(XmlElement xmlConfig, OperationalContext ctx, ClassLoader loader)
        {
        if (XmlHelper.isInstanceConfigEmpty(xmlConfig))
            {
            if (!xmlConfig.getElementList().isEmpty())
                {
                String sConfig = xmlConfig.getName();
                if (sConfig.equals("partitioned-quorum-policy-scheme"))
                    {
                    XmlElement xmlHosts = xmlConfig.getElement("recovery-hosts");
                    AddressProviderFactory factory = xmlHosts == null
                                                     ? null
                                                     : LegacyXmlConfigHelper.parseAddressProvider("recovery-hosts",
                            xmlConfig, ctx.getAddressProviderMap());
                    AddressProviderBuilder bldrRecoveryHosts = new FactoryBasedAddressProviderBuilder(factory);

                    PartitionedCacheQuorumPolicyBuilder builder =
                        new PartitionedCacheQuorumPolicyBuilder(bldrRecoveryHosts, xmlConfig);

                    for (ActionRule action: ActionRule.values())
                        {
                        int   nThreshold     = 0;
                        float flThresholdPct = 0.0f;
                        if (action.getMask() == ConfigurableQuorumPolicy.PartitionedCacheQuorumPolicy.MASK_RECOVER)
                            {
                            String sElementName = action.getElementName();
                            String sThreshold   = xmlConfig.getSafeElement(sElementName).getString();
                            int    ofPct        = sThreshold.indexOf("%");
                            if (ofPct >= 0)
                                {
                                try
                                    {
                                    flThresholdPct = Base.parsePercentage(sThreshold);
                                    }
                                catch (IllegalArgumentException e)
                                    {
                                    throw new ConfigurationException("The <" + sElementName + "> is not a valid value",
                                                "Please ensure that the value is non-nagative integer with or without %  <" + sElementName + '>');
                                    }
                                }
                            else
                                {
                                nThreshold = Integer.parseInt(sThreshold);
                                }
                            }
                        else
                            {
                            nThreshold = xmlConfig.getSafeElement(action.getElementName()).getInt();;
                            }

                        builder.addQuorumRule(action.getElementName(), action.getMask(), nThreshold, flThresholdPct);
                        }

                    return builder;
                    }
                else if (sConfig.equals("proxy-quorum-policy-scheme"))
                    {
                    int nThreshold = xmlConfig.getSafeElement(ProxyQuorumPolicyBuilder.CONNECT_RULE_NAME).getInt(0);
                    ProxyQuorumPolicyBuilder builder = new ProxyQuorumPolicyBuilder(nThreshold, xmlConfig);

                    return builder;
                    }
                else if (sConfig.equals("cluster-quorum-policy"))
                    {
                    return new ClusterQuorumPolicyBuilder(getQuorumMap(xmlConfig), xmlConfig);
                    }
                }

            return new ActionPolicyBuilder.NullImplementationBuilder();
            }
        else
            {
            return new ActionPolicyBuilder.ActionPolicyParameterizedBuilder(LegacyXmlConfigHelper.createBuilder(
                xmlConfig, ActionPolicy.class));
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Construct a ClusterQuorumPolicy with the specified configuration.
     *
     * @param xmlConfig  the XML configuration element
     *
     * @return the quorum map of role to quorum count
     */
    private Map<String, Integer> getQuorumMap(XmlElement xmlConfig)
        {
        Map<String, Integer> mapQuorum = new HashMap<>();

        if (xmlConfig != null)
            {
            processQuorumElement(xmlConfig, mapQuorum,
                "timeout-survivor-quorum", "");
            processQuorumElement(xmlConfig, mapQuorum,
                "timeout-site-quorum", ClusterQuorumPolicy.SITES);
            processQuorumElement(xmlConfig, mapQuorum,
                "timeout-machine-quorum", ClusterQuorumPolicy.MACHINES);
            }
        return mapQuorum;
        }

    /**
     * Process a specific quorum group.
     */
    private void processQuorumElement(XmlElement xmlConfig, Map<String,Integer> mapQuorum,
            String sGroup, String sPrefix)
        {
        for (Iterator iter = xmlConfig.getElements(sGroup); iter.hasNext(); )
            {
            XmlElement xmlQuorum = (XmlElement) iter.next();

            XmlValue xmlRole = xmlQuorum.getAttribute("role");
            int      nQuorum = xmlQuorum.getInt();

            // default the role to the wildcard
            String sRole = xmlRole == null || xmlRole.isEmpty()
                    ? ClusterQuorumPolicy.ROLE_ALL : xmlRole.getString();

            mapQuorum.put(sPrefix + sRole, Integer.valueOf(nQuorum));
            }
        }
    }
