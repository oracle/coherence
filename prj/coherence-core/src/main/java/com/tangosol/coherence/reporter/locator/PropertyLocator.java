/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.config.Config;

import com.tangosol.coherence.reporter.extractor.ConstantExtractor;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ValueExtractor;

/**
* Class to include an immutable system property into a report.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class PropertyLocator
        extends BaseLocator
    {
    /**
    * @inheritDoc
    */
    public void configure(XmlElement xml)
        {
        super.configure(xml);
        m_sProperty = Config.getProperty(m_sName);
        }

    /**
    * @inheritDoc
    */
    public ValueExtractor getExtractor()
        {
        super.getExtractor();

        if (m_veExtractor == null)
            {
            m_veExtractor = new ConstantExtractor(m_sProperty);
            }
        return m_veExtractor;
        }

    /**
    * @inheritDoc
    */
    public Object getValue(Object oKey)
        {
        return m_sProperty;
        }

    //---- data members ------------------------------------------------------
    /**
    * The value extracted from the system property
    */
    protected String m_sProperty;
    }
