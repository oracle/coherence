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
 * An {@link ElementProcessor} is responsible for processing {@link XmlElement} content
 * to return a strongly-typed value.
 *
 * @param <T>  the type of value that will be returned by the {@link ElementProcessor}
 *
 * @author bo  2011.06.14
 * @since Coherence 12.1.2
 */
public interface ElementProcessor<T>
    {
    /**
     * Process an {@link XmlElement} to return a specific type of value.
     *
     * @param context     the {@link ProcessingContext} in which the
     *                    {@link XmlElement} is being processed
     * @param xmlElement  the {@link XmlElement} to process
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return a value of type T
     */
    public T process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException;
    }
