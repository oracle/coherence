/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.extractor.CorrelatedExtractor;
import com.tangosol.coherence.reporter.Reporter;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;

/**
* Class to include a correlated reference in a sub query filter.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class CorrelatedLocator
        extends BaseLocator
    {
    // ----- ColumnLocator interface -----------------------------------------
    /**
    * @inheritDoc
    */
    public void configure(XmlElement xml)
        {
        super.configure(xml);
        m_sColumnRef = xml.getSafeElement(Reporter.TAG_COLUMNREF).getString();
        }

    /**
    * @inheritDoc
    */
    public ValueExtractor getExtractor()
        {
        super.getExtractor();
        if (m_veExtractor == null)
            {
            m_veExtractor = new CorrelatedExtractor(
                    Base.checkNotNull(m_queryHandler.ensureExtractor(m_sColumnRef), "Column extractor"));
            }
        ((CorrelatedExtractor) m_veExtractor).setTarget(m_oCorrelated);
        return m_veExtractor;
        }

    // ----- accessors -------------------------------------------------------

    /**
    * Set the reference to the outer object.
    *
    * @param oCorrelatedRef the correlated MBean reference name
    */
    public void setCorrellatedObject(Object oCorrelatedRef)
        {
        m_oCorrelated = oCorrelatedRef;
        }

    // ----- data members ----------------------------------------------------
    /**
    * The wrapped column reference
    */
    protected ColumnLocator m_columnLocator;

    /**
    * The wrapped column reference
    */
    protected Object m_oCorrelated;

    /**
    * The correlated column string identifier.
    */
    protected String m_sColumnRef;
    }
