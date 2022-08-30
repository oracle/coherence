/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;


import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} that does nothing.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class NoOpElementProcessor
        implements ElementProcessor<Void>
    {
    /**
     * Private constructor, because this processor can be a singleton.
     */
    private NoOpElementProcessor()
        {
        }

    @Override
    public Void process(ProcessingContext context, XmlElement xmlElement) throws ConfigurationException
        {
        return null;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton instance of {@link NoOpElementProcessor}
     */
    public static final NoOpElementProcessor INSTANCE = new NoOpElementProcessor();
    }
