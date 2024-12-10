/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;


import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * An {@link ExtendPreprocessor} is an {@link ElementPreprocessor} that will
 * inject an "acceptor-config" {@link XmlElement} into a "proxy-scheme"
 * {@link XmlElement} if one does not exist.
 *
 * @author lh  2013.07.09
 * @since Coherence 12.1.3
 */
public class ExtendPreprocessor
        implements ElementPreprocessor
    {
    // ----- ElementPreprocessor methods ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preprocess(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        XmlElement xmlProxyScheme = element.getElement("proxy-scheme");
        if (xmlProxyScheme == null)
            {
            return false;
            }

        XmlHelper.ensureElement(xmlProxyScheme, "acceptor-config");
        return false;
        }

    // ----- constants ------------------------------------------------------

    /**
     * This singleton instance of the {@link ExtendPreprocessor}.
     */
    public static final ExtendPreprocessor INSTANCE = new ExtendPreprocessor();
    }
