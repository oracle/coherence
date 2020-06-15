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
 * An {@link ElementProcessor} that creates a {@link BackingMapScheme}
 * for use in a collection scheme.
 *
 * @author jk 2015.05.28
 * @since Coherence 14.1.1
 */
@XmlSimpleName("value-storage-scheme")
public class ValueStorageSchemeProcessor
        implements ElementProcessor<BackingMapScheme>
    {
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
            throw new ConfigurationException("The <value-storage-scheme> is not a CachingScheme",
                "Please ensure that the configured scheme is appropriate for the <backing-map-scheme>");
            }

        return scheme;
        }
    }
