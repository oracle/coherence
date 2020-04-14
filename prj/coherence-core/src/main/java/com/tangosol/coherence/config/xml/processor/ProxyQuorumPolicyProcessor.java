/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.ProxyQuorumPolicyBuilder;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.net.ActionPolicy;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.NullImplementation;

/**
 * An {@link ElementProcessor} that will parse a &lt;proxy-quorum-policy-scheme&gt;
 * and produce a suitable {@link ActionPolicy}
 *
 * @author bo  2013.07.12
 * @since Coherence 12.1.3
 */
@XmlSimpleName("proxy-quorum-policy-scheme")
public class ProxyQuorumPolicyProcessor
        implements ElementProcessor<ActionPolicyBuilder>
    {
    // ----- ElementProcessor methods  --------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionPolicyBuilder process(ProcessingContext context, XmlElement xmlElement)
        {
        // attempt to locate a ParameterizedBuilder
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr == null)
            {
            if (xmlElement.getElementList().isEmpty())
                {
                return new ActionPolicyBuilder.NullImplementationBuilder();
                }
            else
                {
                int nThreshold = context.getOptionalProperty(ProxyQuorumPolicyBuilder.CONNECT_RULE_NAME, Integer.class, 0, xmlElement);
                return new ProxyQuorumPolicyBuilder(nThreshold, xmlElement);
                }
            }
        else
            {
            ParameterizedBuilder<ActionPolicy> bldrPolicy =
                    (ParameterizedBuilder<ActionPolicy>) bldr;

            return new ActionPolicyBuilder.ActionPolicyParameterizedBuilder(bldrPolicy);
            }
        }
    }
