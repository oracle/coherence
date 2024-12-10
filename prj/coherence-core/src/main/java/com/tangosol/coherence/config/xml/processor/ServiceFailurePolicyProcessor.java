/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ServiceFailurePolicyBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.net.ServiceFailurePolicy;

import com.tangosol.run.xml.XmlElement;

/**
 * An ElementProcessor that will parse a &lt;service-failure-policyr&gt; and
 * produce a suitable ServiceFailurePolicy
 *
 * @author bo  2013.03.07
 * @since Coherence 12.1.3
 */
@XmlSimpleName("service-failure-policy")
public class ServiceFailurePolicyProcessor
        extends AbstractEmptyElementProcessor<ServiceFailurePolicyBuilder>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ServiceFailurePolicyProcessor}.
     */
    public ServiceFailurePolicyProcessor()
        {
        super(EmptyElementBehavior.IGNORE);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceFailurePolicyBuilder onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // attempt to locate a ParameterizedBuilder
        ParameterizedBuilder<ServiceFailurePolicy> bldr =
            (ParameterizedBuilder<ServiceFailurePolicy>) ElementProcessorHelper.processParameterizedBuilder(context,
                xmlElement);

        return bldr == null
               ? new ServiceFailurePolicyBuilder(xmlElement.getString().trim(), xmlElement)
               : new ServiceFailurePolicyBuilder(bldr, xmlElement);

        }
    }
