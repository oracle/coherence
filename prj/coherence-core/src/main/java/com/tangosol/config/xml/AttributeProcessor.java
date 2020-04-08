/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;

import com.tangosol.run.xml.XmlAttribute;

/**
 * An {@link AttributeProcessor} is responsible for processing {@link XmlAttribute} content
 * to return a strongly-typed value.
 *
 * @param <T>  the type of value that will be returned by the {@link AttributeProcessor}
 *
 * @author bo  2011.06.14
 * @since Coherence 12.1.2
 */
public interface AttributeProcessor<T>
    {
    /**
     * Process an {@link XmlAttribute} and return a specific type of value.
     *
     * @param context    the {@link ProcessingContext} in which the {@link XmlAttribute} is being processed
     * @param attribute  the {@link XmlAttribute} to be processed
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return a value of type T
     */
    public T process(ProcessingContext context, XmlAttribute attribute)
            throws ConfigurationException;
    }
