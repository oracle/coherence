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

import com.tangosol.util.ResourceRegistry;

import java.util.List;

/**
 * The {@link ScopeNameProcessor} is responsible for processing the &lt;scope-name&gt; {@link XmlElement}
 * in a Coherence Cache Configuration file.
 *
 * @author bo  2013.12.01
 * @since Coherence 12.1.3
 */
@XmlSimpleName("scope-name")
public class ScopeNameProcessor
        implements ElementProcessor<String>
    {
    @Override
    public String process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        String sScopeName = element.getString();

        return sScopeName == null ? null : sScopeName.trim();
        }
    }
