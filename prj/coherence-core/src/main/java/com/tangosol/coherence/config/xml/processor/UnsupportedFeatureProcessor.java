/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.run.xml.XmlElement;

/**
 * UnsupportedFeatureProcessor is an ElementProcessor that fails fast highlighting
 * which feature is not supported in this edition of the product.
 *
 * @author hr  2020.01.23
 */
public class UnsupportedFeatureProcessor
        implements ElementProcessor<Object>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct ElementProcessor that reports which feature is not support.
     *
     * @param sFeature  the feature that is not supported
     */
    public UnsupportedFeatureProcessor(String sFeature)
        {
        f_sFeature = sFeature;
        }

    // ----- ElementProcess interface ---------------------------------------

    @Override
    public Object process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        throw new UnsupportedOperationException(f_sFeature + " is not supported in this edition");
        }

    // ----- data members ---------------------------------------------------

    /**
     * The feature that is not supported
     */
    protected final String f_sFeature;
    }
