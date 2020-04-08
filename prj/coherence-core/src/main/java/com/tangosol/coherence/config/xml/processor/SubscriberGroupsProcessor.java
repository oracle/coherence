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

import java.util.Collection;
import java.util.Map;

/**
 * A {@link ElementProcessor} for the &lt;subscriber-groups&gt; element.
 *
 * @author jf 2016.03.02
 * @since Coherence 14.1.1
 */
@XmlSimpleName("subscriber-groups")
public class SubscriberGroupsProcessor implements ElementProcessor<Collection<SubscriberGroupBuilder>>
    {
    @Override
    @SuppressWarnings("unchecked")
    public Collection<SubscriberGroupBuilder> process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        Map<String, ?> map = context.processElementsOf(xmlElement);
        return (Collection<SubscriberGroupBuilder>)  map.values();
        }
    }
