/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.extractor.DeltaExtractor;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;
import com.tangosol.util.ListMap;
import com.tangosol.util.ValueExtractor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
* A Locator that returns the difference between 2 executions a ValueExtractor
* or aggregate.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class DeltaLocator
        extends BaseLocator
    {
    // ----- Constructor -----------------------------------------------------
    /**
    * Construct a Delta locator.
    */
    public DeltaLocator()
        {
        super();
        m_mapPrior = new HashMap();
        m_mapKey   = new ListMap();
        }

    // ----- ColumnLocator interface -----------------------------------------

    /**
    * @inheritDoc
    */
    public void configure(XmlElement xml)
        {
        super.configure(xml);
        String sId = xml.getSafeElement(TAG_COLUMNREF).getString();

        m_columnLocator = Base.checkNotNull(m_queryHandler.ensureColumnLocator(xml, sId), "Column locator");
        m_sSourceId     = sId;
        buildExtractors();

        XmlElement xmlColumns  = xml.getSafeElement(TAG_PARAMS);
        List       listColumns = xmlColumns.getElementList();
        if (listColumns != null)
            {
            for (Iterator iter = listColumns.iterator(); iter.hasNext(); )
                {
                sId = ((XmlElement) iter.next()).getString();
                ValueExtractor ve = Base.checkNotNull(m_queryHandler.ensureExtractor(sId), "Column extractor");
                m_mapKey.put(sId, ve);
                }
            }
        }

    /**
    * @inheritDoc
    */
    public ValueExtractor getExtractor()
        {
        super.getExtractor();
        return buildExtractors();
        }

    /**
    * @inheritDoc
    */
    public void reset(Set setResults)
        {
        ValueExtractor veSource = m_veSource;
        DeltaExtractor veDelta  = m_veDelta;

        m_mapPrior.clear();

        for (Iterator i = setResults.iterator(); i.hasNext();)
            {
            Map.Entry entry  = (Map.Entry) i.next();
            Object    oValue = veSource.extract(entry.getKey());
            Object    oKey   = veDelta.getKey(entry.getKey());
            putPrior(oKey, oValue);
            }

        m_veExtractor = null;
        m_veSource    = null;
        m_veDelta     = null;
        }

    /**
    * @inheritDoc
    */
    public boolean isRowDetail()
        {
        return m_columnLocator.isRowDetail();
        }

    // ----- accessors and helpers -------------------------------------------

    /**
    * Maintain a Map of the prior values of the Column.
    *
    * @param oKey the key for the column.
    * @param oValue the prior value for the column.
    */
    public void putPrior(Object oKey, Object oValue)
        {
        m_mapPrior.put(oKey, oValue);
        }

    protected ValueExtractor buildExtractors()
        {
        if (m_veSource == null)
            {
            m_veSource      = m_queryHandler.ensureExtractor(m_sSourceId);
            }
        if (m_veDelta == null)
            {
            m_veDelta = new DeltaExtractor(m_mapPrior, m_veSource, m_mapKey);
            }
        return m_veDelta;
        }

    //----- data members -----------------------------------------------------
    /**
    *  Map containing the prior value of the column.
    *  Key is the ObjectName key property list string.
    *  Value prior result object
    */
    protected Map m_mapPrior;

    /**
    * Reference to the column that returns the value
    */
    protected ColumnLocator m_columnLocator;

    /**
    * The source data value extractor
    */
    protected ValueExtractor m_veSource;

    /**
    * The value extractor used by QueryHandler.
    */
    protected DeltaExtractor m_veDelta;

    /**
    * Reference to the key locator for the delta
    */
    protected Map m_mapKey;

    protected String m_sSourceId;

    }
