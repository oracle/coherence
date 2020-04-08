/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder.NameListDependencies;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

import java.util.List;

/**
 * An {@link ElementProcessor} that will parse and produce a
 * NameListDependencies based on a protocol-versions or cipher-suites configuration element.
 *
 * @author jf  2015.11.11
 * @since Coherence 12.2.1.1
 */
public class SSLNameListProcessor implements ElementProcessor<NameListDependencies>
    {
    // ----- constructors ----------------------------------------------------

    public SSLNameListProcessor(String sName)
        {
        f_sName = sName;
        }

    @Override
    public NameListDependencies process(ProcessingContext context, XmlElement xmlElement) throws ConfigurationException
        {
        NameListDependencies bldr = new NameListDependencies(xmlElement.getQualifiedName().getLocalName());
        XmlValue value = xmlElement.getAttribute("usage");
        if (value != null)
            {
            bldr.setUsage(value.getString());
            }

        for (XmlElement elementChild : ((List<XmlElement>) xmlElement.getElementList()))
            {
            if (elementChild.getQualifiedName().getLocalName().equals("name"))
                {
                bldr.add((String) elementChild.getValue());
                }
            }

        return bldr;
        }

    // ----- constants -------------------------------------------------------
    /**
     * xmlElement local name should be this value.
     */
    private final String f_sName;
    }
