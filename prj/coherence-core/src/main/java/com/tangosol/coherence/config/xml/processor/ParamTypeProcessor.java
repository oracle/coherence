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
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ClassHelper;

/**
 * A {@link ParamTypeProcessor} is responsible for processing &lt;param-type&gt; {@link XmlElement}s to produce a
 * fully qualified class name.
 *
 * @author bo  2011.06.24
 * @since Coherence 12.1.2
 */
@XmlSimpleName("param-type")
public class ParamTypeProcessor
        implements ElementProcessor<String>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public String process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        String sType = element.getString();

        return ClassHelper.getFullyQualifiedClassNameOf(sType);
        }
    }
