/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ResourceMapping;
import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.TopicMapping;
import com.tangosol.coherence.config.scheme.TopicScheme;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;

import com.tangosol.run.xml.XmlElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An {@link TopicMappingProcessor} is responsible for processing &lt;topic-mapping&gt;
 * {@link XmlElement}s to produce a {@link TopicMapping}.
 *
 * @author jk  2015.05.21
 * @since Coherence 14.1.1
 */
@XmlSimpleName("topic-mapping")
public class TopicMappingProcessor
        implements ElementProcessor<TopicMapping>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link TopicMappingProcessor}.
     *
     * @param sNameElementName  the name of the element
     * @param clsScheme         the type of the topic scheme
     */
    public TopicMappingProcessor(String sNameElementName, Class<? extends TopicScheme> clsScheme)
        {
        f_sNameElementName = sNameElementName;
        f_clsScheme        = clsScheme;
        }

    // ----- ElementProcessor methods ---------------------------------------

    @Override
    public TopicMapping process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // construct the TopicMapping with the required properties
        String       sCacheNamePattern = context.getMandatoryProperty(f_sNameElementName, String.class, element);
        String       sSchemeName       = context.getMandatoryProperty("scheme-name", String.class, element);
        TopicMapping mapping           = new TopicMapping(sCacheNamePattern, sSchemeName, f_clsScheme);

        // now inject any other (optional) properties it may require
        context.inject(mapping, element);

        List<ResourceMapping> subMappings = mapping.getSubMappings();
        List<ResourceMapping> list        = PagedTopicCaches.Names.values().stream()
                .map(queueCacheNames -> createSubMapping(mapping, queueCacheNames, sCacheNamePattern, sSchemeName))
                .collect(Collectors.toList());

        subMappings.addAll(list);

        // add the topic-mapping as a cookie so that child processors may access it (mainly to add resources if necessary)
        context.addCookie(TopicMapping.class, mapping);

        // process all of the foreign elements
        // (this allows the elements to modify the configuration)
        context.processForeignElementsOf(element);

        return mapping;
        }

    // ----- helper methods -------------------------------------------------

    private CacheMapping createSubMapping(TopicMapping mappingCol, PagedTopicCaches.Names<?,?> type,
            String sNamePattern, String sSchemeName)
        {
        CacheMapping mapping = new CacheMapping(type.cacheNameForTopicName(sNamePattern), sSchemeName);

        mapping.setKeyClassName(type.getKeyClass().getCanonicalName());
        mapping.setValueClassName(type.getValueClass().getCanonicalName());
        mapping.setParameterResolver(mappingCol.getParameterResolver());
        mapping.setInternal(type.isInternal());

        return mapping;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        TopicMappingProcessor that = (TopicMappingProcessor) o;

        return f_sNameElementName.equals(that.f_sNameElementName);

        }

    @Override
    public int hashCode()
        {
        return f_sNameElementName.hashCode();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the XML element to use to obtain the mapping name
     */
    private final String f_sNameElementName;

    /**
     * The type of the topic scheme.
     */
    private final Class<? extends TopicScheme> f_clsScheme;
    }
