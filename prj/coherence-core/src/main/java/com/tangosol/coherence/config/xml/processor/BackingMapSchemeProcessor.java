/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.scheme.BackingMapScheme;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.CustomScheme;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

/**
 * A BackingMapSchemeProcessor is responsible for processing a
 * backing-map-scheme {@link XmlElement} to produce a {@link BackingMapScheme}.
 *
 * @author pfm  2011.12.02
 * @since Coherence 12.1.2
 */
@XmlSimpleName(BackingMapSchemeProcessor.ELEMENT_NAME)
public class BackingMapSchemeProcessor
        implements ElementProcessor<BackingMapScheme>
    {
    // ----- ElementProcessor interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public BackingMapScheme process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        // create a BackingMapScheme instance and inject all annotated properties
        BackingMapScheme scheme = context.inject(new BackingMapScheme(), element);

        // the remaining element must be the CachingScheme definition
        Object oResult = context.processRemainingElementOf(element);

        if (oResult instanceof CachingScheme)
            {
            scheme.setInnerScheme((CachingScheme) oResult);
            }
        else if (oResult instanceof ParameterizedBuilder)
            {
            scheme.setInnerScheme(new CustomScheme((ParameterizedBuilder) oResult));
            }
        else
            {
            throw new ConfigurationException("The <" + m_sElementName + "> is not a CachingScheme",
                "Please ensure that the configured scheme is appropriate for the <" + m_sElementName + '>');
            }

        return scheme;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the {@link XmlElement} to process.
     *
     * @since 12.2.1.4
     */
    protected String m_sElementName = ELEMENT_NAME;

    // ----- constants ------------------------------------------------------

    /**
     * The name of the backing-map-scheme {@link XmlElement}.
     *
     * @since 12.2.1.4
     */
    public static final String ELEMENT_NAME = "backing-map-scheme";
    }
