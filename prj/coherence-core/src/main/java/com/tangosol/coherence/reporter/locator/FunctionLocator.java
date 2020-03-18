/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.Reporter;
import com.tangosol.coherence.reporter.DataSource;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;

import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
*  Base class for locator that calcculate their value based on two other locator.
*
* @since Coherence 3.4
* @author ew 2008.01.28
*/
public class FunctionLocator
        extends BaseLocator
    {

    /**
    * @inheritDoc
    */
    public void configure(XmlElement xmlConfig)
        {
        super.configure(xmlConfig);

        XmlElement xmlColumns  = xmlConfig.getElement(Reporter.TAG_PARAMS);
        List       listColumns = xmlColumns.getElementList();

        Iterator      iter = listColumns.iterator();
        String        sId  = ((XmlElement) iter.next()).getString();
        ColumnLocator qc   = m_queryHandler.ensureColumnLocator(null, sId);

        m_veColumn1 = Base.checkNotNull(m_queryHandler.ensureExtractor(sId), "Column extractor");

        boolean   fAgg1 = qc.isAggregate();
        boolean   fDet1 = qc.isRowDetail();

        m_sId1 = sId;
        sId    = ((XmlElement) iter.next()).getString();
        qc     = m_queryHandler.ensureColumnLocator(null, sId);

        m_veColumn2 = Base.checkNotNull(m_queryHandler.ensureExtractor(sId), "Column extractor");

        boolean   fAgg2 = qc.isAggregate();
        boolean   fDet2 = qc.isRowDetail();

        m_sId2       = sId;
        m_fAggregate = fAgg1 && fAgg2;
        m_fDetail    = fDet1 || fDet2;
        }

    /**
    * @inheritDoc
    */
    public void reset(Set setResults)
        {
        super.reset(setResults);

        // Reset the ValueExtractors for subsequent executions.
        m_veColumn1 = null;
        m_veColumn2 = null;
        }


   /**
    * @inheritDoc
    */
    public Object getValue(Object oKey)
        {
        if (m_fDetail)
            {
            return m_source.getValue(oKey, m_iExtractorPos);
            }
        else
            {
            return m_source.getScalarValue(oKey, m_iExtractorPos);
            }
        }


   /**
    * @inheritDoc
    */
    public void setDataSource(DataSource source)
        {
        m_source = source;
        if (m_fDetail)
            {
            m_iExtractorPos = m_source.addExtractor(getExtractor());
            }
        else
            {
            m_iExtractorPos = m_source.addScalar((getExtractor()));
            }
        }
    /**
    * @inheritDoc
    */
    public boolean isAggregate()
        {
        return m_fAggregate;
        }

    /**
    * @inheritDoc
    */
    public boolean isRowDetail()
        {
        return m_fDetail;
        }

    protected void buildExtractors()
        {
        if (m_veColumn1 == null)
            {
            m_veColumn1 = m_queryHandler.ensureExtractor(m_sId1);
            }

        if (m_veColumn2 == null)
            {
            m_veColumn2 = m_queryHandler.ensureExtractor(m_sId2);
            }
        }

    // ----- data members ----------------------------------------------------
    /*
    * the first column operand
    */
    protected ValueExtractor m_veColumn1;

    /*
    * the second column operand
    */
    protected ValueExtractor m_veColumn2;

    /*
    * the first column Identifier
    */
    protected String m_sId1;

    /*
    * the second column Identifier
    */
    protected String m_sId2;

    /**
    * Flag to determine if the underlying locators are aggregates
    */
    protected boolean m_fAggregate;

    /**
    * Flag to determine if the function needs to display detail
    */
    protected boolean m_fDetail;
    }
