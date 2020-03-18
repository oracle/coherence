/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.reporter.locator;

import com.tangosol.coherence.reporter.extractor.OperationExtractor;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.ValueExtractor;

/**
 * Locator for returning an MBean operation extractor.
 *
 * @author sr 2017.09.20
 * @since Coherence 12.2.1
 */
public class OperationLocator
        extends BaseLocator
    {
    // ----- ColumnLocator interface ----------------------------------------

    @Override
    public void configure(XmlElement xml)
        {
        super.configure(xml);

        XmlElement xmlParams = xml.getElement("init-params");
        if (xmlParams != null)
            {
            // parseInitParams method throws an exception in case of null
            // param types, hence we do not need an extra validation here
            m_aoMethodParams   = XmlHelper.parseInitParams(xmlParams);
            m_asSignatureTypes = XmlHelper.parseParamTypes(xmlParams);
            }
        }

    @Override
    public ValueExtractor getExtractor()
        {
        if (m_veExtractor == null)
            {
            m_veExtractor = new OperationExtractor(m_sName, m_cDelim, m_aoMethodParams, m_asSignatureTypes, m_source.getMBeanServer());
            }
        return m_veExtractor;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The parameters array.
     */
    protected Object[] m_aoMethodParams;

    /**
     * The signature parameter array.
     */
    protected String[] m_asSignatureTypes;
    }
