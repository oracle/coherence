/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.SubscriberGroupBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

/**
 * A {@link ElementProcessor} for the &lt;subscriber-group&gt; element.
 *
 * @author jf 2016.03.02
 * @since Coherence 14.1.1
 */
@XmlSimpleName("subscriber-group")
public class SubscriberGroupProcessor implements ElementProcessor<SubscriberGroupBuilder>
    {
    @Override
    public SubscriberGroupBuilder process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        SubscriberGroupBuilder builder = new SubscriberGroupBuilder();
        context.inject(builder, xmlElement);
        return builder;
        }
    }
