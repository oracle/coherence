/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;

import com.tangosol.run.xml.XmlElement;

/**
 * A {@link ConditionalElementProcessor} is an {@link ElementProcessor} that supports conditionally
 * processing {@link XmlElement}s.   Unlike a regular {@link ElementProcessor}, when a {@link ProcessingContext}
 * encounters a {@link ConditionalElementProcessor}, it will first query the said processor to
 * determine if it should process an {@link XmlElement}.
 *
 * @param <T>  the type of value that will be returned by the {@link ConditionalElementProcessor},
 *             should an {@link XmlElement} be processed.
 *
 * @author bo  2013.09.14
 * @since Coherence 12.1.2
 */
public interface ConditionalElementProcessor<T>
        extends ElementProcessor<T>
    {
    /**
     * Determines if the specified {@link XmlElement} should be processed.
     *
     * @param context     the {@link ProcessingContext} in which the {@link XmlElement} is being processed
     * @param xmlElement  the {@link XmlElement} that would be processed
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return <code>true</code> if the {@link XmlElement} should be processed
     */
    public boolean accepts(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException;
    }
