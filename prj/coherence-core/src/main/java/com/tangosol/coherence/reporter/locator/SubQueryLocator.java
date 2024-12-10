/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;

import com.tangosol.coherence.reporter.Constants;
import com.tangosol.coherence.reporter.extractor.SubQueryExtractor;
import com.tangosol.coherence.reporter.Reporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Iterator;


/**
* Column to evaluate a sub query.
*
* @since Coherence 3.4
* @author ew 2008.01.28
*/
public class SubQueryLocator
        extends BaseLocator
        implements Constants
    {
    /**
    * @inheritDoc
    */
    public void configure(XmlElement xmlConfig)
        {
        super.configure(xmlConfig);

        m_xmlQuery = xmlConfig.getSafeElement(Reporter.TAG_QUERY);

        XmlElement xmlColumns  = xmlConfig.getSafeElement(Reporter.TAG_PARAMS);
        XmlElement xmlRef      = xmlConfig.getSafeElement(Reporter.TAG_COLUMNREF);
        String     sColumnRef  = xmlRef.getString();
        List       listColumns = xmlColumns.getElementList();
        int        nParamCount = listColumns.size();

        m_filterColumns = new ArrayList(nParamCount);
        m_sSourceId     = sColumnRef;
        }

    /**
    * @inheritDoc
    */
    public ValueExtractor getExtractor()
        {
        if (m_veExtractor == null)
            {
            String           sPattern     = m_xmlQuery.getSafeElement(TAG_PATTERN)
                                                      .getString();
            Set              setMacros    = Reporter.getMacros(sPattern);
            ValueExtractor[] veCorrelated = new ValueExtractor[setMacros.size()];

            int c = 0;
            for (Iterator iter = setMacros.iterator(); iter.hasNext();)
                {
                String sId = (String)iter.next();

                veCorrelated[c] = Base.checkNotNull(m_queryHandler.ensureExtractor(sId), "Column extractor");
                }

            m_fCorrelated = setMacros.size() > 0;

            m_veExtractor = new SubQueryExtractor(veCorrelated, m_xmlQuery,
                    m_queryHandler, m_sSourceId);
            }

        return m_veExtractor;
        }

    /**
    * @inheritDoc
    */
    public boolean isRowDetail()
        {
        return m_fCorrelated;
        }


    //------ data members ----------------------------------------------------
    /**
    * The query string template.
    */
    protected XmlElement m_xmlQuery;

    /**
    * The list of correlated locator used in the subquery filter.
    */
    protected List m_filterColumns;

    /**
    * The source column for the subquery
    */
    protected ColumnLocator m_asColumnLocator;

    /**
    * The identifier for the source column for the subquery
    */
    protected String m_sSourceId;

    /**
    * Determine if the Subquery is Correlated
    */
    protected boolean m_fCorrelated;
    }
