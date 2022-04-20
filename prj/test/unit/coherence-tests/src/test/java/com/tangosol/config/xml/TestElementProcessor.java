/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

/**
 * {@link TestElementProcessor} used for testing.
 */
public class TestElementProcessor
        implements ElementProcessor<XmlElement>
    {
    /**
     * {@inheritDoc}
     */
    public XmlElement process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        return element;
        }
    }
