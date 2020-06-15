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

import com.tangosol.util.Base;

/**
 * A {@link MemorySizeProcessor} is responsible for processing Coherence
 * memory sizes and returning them in bytes.
 *
 * @author bo  2013.07.02
 * @since Coherence 12.1.3
 */
public class MemorySizeProcessor
        extends AbstractEmptyElementProcessor<Integer>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link MemorySizeProcessor}.
     */
    public MemorySizeProcessor()
        {
        super(EmptyElementBehavior.IGNORE);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        return (int) Base.parseMemorySize(xmlElement.getString());
        }
    }
