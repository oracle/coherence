/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * A {@link MillisProcessor} is responsible for processing Coherence time values
 * and returning them in milliseconds.
 *
 * @author bo  2013.03.07
 * @since Coherence 12.1.3
 */
public class MillisProcessor
        extends AbstractEmptyElementProcessor<Long>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link MillisProcessor}.
     */
    public MillisProcessor()
        {
        super(EmptyElementBehavior.IGNORE);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Long onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        return XmlHelper.parseTime(xmlElement.getString());
        }
    }
