/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.AttributeProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlAttribute;
import com.tangosol.run.xml.XmlValue;

/**
 * {@link TestAttributeProcessor} is used for testing.
 */
public class TestAttributeProcessor
        implements AttributeProcessor<XmlValue>
    {
    /**
     * {@inheritDoc}
     */
    public XmlValue process(ProcessingContext context, XmlAttribute attribute)
            throws ConfigurationException
        {
        return attribute.getXmlValue();
        }
    }
