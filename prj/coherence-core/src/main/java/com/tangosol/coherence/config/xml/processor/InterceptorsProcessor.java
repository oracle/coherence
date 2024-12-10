/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.xml.preprocessor.SchemeRefPreprocessor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

import com.tangosol.util.Base;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link ElementProcessor} for the &lt;interceptors&gt; element.
 *
 * @author bo 2012.10.31
 * @since Coherence 12.1.2
 */
@XmlSimpleName("interceptors")
public class InterceptorsProcessor
        implements ElementProcessor<List<NamedEventInterceptorBuilder>>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public List<NamedEventInterceptorBuilder> process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // process all of the NamedEventInterceptorBuilders
        Map<String, ?> mapBuilders = context.processElementsOf(xmlElement);

        List<NamedEventInterceptorBuilder> listBuilders = new ArrayList<>(mapBuilders.size());

        listBuilders.addAll((Collection<NamedEventInterceptorBuilder>) mapBuilders.values());

        return listBuilders;
        }
    }
