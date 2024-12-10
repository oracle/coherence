/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.AddressProviderBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.PartitionedCacheQuorumPolicyBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.net.ActionPolicy;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import static com.tangosol.net.ConfigurableQuorumPolicy.PartitionedCacheQuorumPolicy;

/**
 * An ElementProcessor that will parse a &lt;partitioned-quorum-policy-scheme&gt;
 * and produce a suitable {@link ActionPolicy}
 *
 * @author bo  2013.03.08
 * @since Coherence 12.1.3
 */
@XmlSimpleName("partitioned-quorum-policy-scheme")
public class PartitionedQuorumPolicyProcessor
        implements ElementProcessor<ActionPolicyBuilder>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionPolicyBuilder process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // attempt to locate a ParameterizedBuilder
        ParameterizedBuilder<ActionPolicy> builderCustom = (ParameterizedBuilder<ActionPolicy>)
                ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (builderCustom == null)
            {
            XmlElement xmlRecoveryHosts = xmlElement.getSafeElement("recovery-hosts");

            AddressProviderBuilder bldrRecoveryHosts = xmlRecoveryHosts.isEmpty() ? null :
                    new AddressProviderBuilderProcessor().process(context,
                        xmlRecoveryHosts);

            PartitionedCacheQuorumPolicyBuilder builder =
                new PartitionedCacheQuorumPolicyBuilder(bldrRecoveryHosts, xmlElement);

            for (PartitionedCacheQuorumPolicy.ActionRule action: PartitionedCacheQuorumPolicy.ActionRule.values())
                {
                int   nThreshold     = 0;
                float flThresholdPct = 0.0f;
                if (action.getMask() == PartitionedCacheQuorumPolicy.MASK_RECOVER)
                    {
                    String sElementName = action.getElementName();
                    String sThreshold   = context.getOptionalProperty(sElementName, String.class, "0", xmlElement);
                    int    ofPct        = sThreshold.indexOf("%");
                    if (ofPct >= 0)
                        {
                        try
                            {
                            flThresholdPct = Base.parsePercentage(sThreshold);
                            }
                        catch (IllegalArgumentException e)
                            {
                            throw new ConfigurationException("The partitioned-quorum-policy-scheme/" +
                                      sElementName + " value is not valid: " + sThreshold,
                                      "Please ensure that the value is a non-nagative integer with or without a %  <" + sElementName + ">");
                            }
                        }
                    else
                        {
                        nThreshold = Integer.parseInt(sThreshold);
                        }
                    }
                else
                    {
                    nThreshold = context.getOptionalProperty(action.getElementName(), Integer.class, 0, xmlElement);
                    }

                builder.addQuorumRule(action.getElementName(), action.getMask(), nThreshold, flThresholdPct);
                }

            return builder;
            }
        else
            {
            return new ActionPolicyBuilder.ActionPolicyParameterizedBuilder(builderCustom);
            }
        }
    }
