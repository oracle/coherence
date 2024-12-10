/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.CustomScheme;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import java.util.Map;

/**
 * A {@link InternalCacheSchemeProcessor} is responsible for processing an
 * internal-cache-scheme {@link XmlElement}s to produce a {@link MapBuilder}.
 *
 * @author pfm  2011.12.02
 * @since Coherence 12.1.2
 */
@XmlSimpleName("internal-cache-scheme")
public class InternalCacheSchemeProcessor
        implements ElementProcessor<MapBuilder>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public MapBuilder process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        Object oProcessed = context.processOnlyElementOf(element);

        if (oProcessed instanceof CachingScheme)
            {
            return ((CachingScheme) oProcessed);
            }
        else if (oProcessed instanceof MapBuilder)
            {
            return (MapBuilder) oProcessed;
            }
        else if (oProcessed instanceof ParameterizedBuilder)
            {
            return new CustomScheme((ParameterizedBuilder) oProcessed);
            }
        else

            {
            throw new ConfigurationException("The <internal-cache-scheme> is not a CachingScheme or MapBuilder",
                "Please ensure that the configured scheme is appropriate for the <internal-cache-scheme>");
            }
        }
    }
