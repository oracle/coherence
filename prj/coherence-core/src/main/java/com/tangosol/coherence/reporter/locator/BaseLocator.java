/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.Constants;
import com.tangosol.coherence.reporter.DataSource;
import com.tangosol.coherence.reporter.JMXQueryHandler;
import com.tangosol.coherence.reporter.QueryHandler;
import com.tangosol.coherence.reporter.Reporter;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import java.util.Set;


/**
* Base class for providing standard Locator functionality.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class BaseLocator
        implements Constants, ColumnLocator
    {
    // ----- ColumnLocator interface -----------------------------------------

    /**
    * @inheritDoc
    */
    public Object getValue(Object oKey)
        {
        if (isAggregate())
            {
            return m_source.getAggValue(oKey, m_iAggregatePos);
            }
        else

            {
            return m_source.getValue(oKey, m_iExtractorPos);
            }
        }

    /**
    * @inheritDoc
    */
    public void reset(Set setResults)
        {
        m_veExtractor = null;
        }

    /**
    * @inheritDoc
    */
    public void setDataSource(DataSource source)
        {
        m_source = source;
        if (isAggregate())
            {
            m_iAggregatePos = source.addAggregator(getAggregator());
            }
        else
            {
            ValueExtractor ve = getExtractor();
            m_iExtractorPos = source.addExtractor(ve);

            // group-by for hidden columns is not supported (see COH-14871)
            if (m_fGroupBy && !m_fHidden)
                {
                source.addGroupBy(ve);
                }
            }
        }

    /**
    * @inheritDoc
    */
    public ValueExtractor getExtractor()
        {
        return null;
        }

    /**
    * @inheritDoc
    */
    public InvocableMap.EntryAggregator getAggregator()
        {
        return null;
        }

    /**
    * @inheritDoc
    */
    public boolean isAggregate()
        {
        return false;
        }

    /**
    * @inheritDoc
    */
    public void configure(XmlElement xmlConfig)
        {
        m_xml    = xmlConfig;
        m_cDelim = getDelim();

        m_sName    = xmlConfig.getSafeElement(Reporter.TAG_COLUMNNAME).getString();
        m_sId      = xmlConfig.getSafeAttribute("id").getString();
        m_fGroupBy = xmlConfig.getSafeElement("group-by").getBoolean(false);
        m_fHidden  = xmlConfig.getSafeElement("hidden").getBoolean(false);

        if (m_sId.length() == 0)
            {
            m_sId = m_sName;
            }
        }

    /**
    * @inheritDoc
    */
    public XmlElement getConfig()
        {
        return m_xml;
        }

    /**
    * @inheritDoc
    */
    public boolean isRowDetail()
        {
        return false;
        }

    /**
    * @inheritDoc
    */
    public String getName()
        {
        return m_sName;
        }

    /**
    * @inheritDoc
    */
    public String getId()
        {
        return m_sId;
        }

    /**
    * @inheritDoc
    */
    public void setQuery(QueryHandler handler)
        {
        m_queryHandler = (JMXQueryHandler) handler;
        }

    /**
    * @inheritDoc
    */
    public void configure(XmlElement xmlConfig, JMXQueryHandler handler, DataSource source)
        {
        setQuery(handler);
        configure(xmlConfig);
        setDataSource(source);
        }

    // ----- accessors and helpers -------------------------------------------
    /**
    * extract the column delimiter from the report configuration
    *
    * @return the column delimiter character
    */
    protected char getDelim()
        {
        if (m_cDelim == Character.UNASSIGNED)
            {
            String sDelim = m_xml.getSafeElement(TAG_DELIM).getString(
                    VALUE_TAB);

            if (sDelim.equals(VALUE_TAB))
                {
                m_cDelim = '\t';
                }
            else if (sDelim.equals(VALUE_SPACE))
                {
                m_cDelim = ' ';
                }
            else
                {
                m_cDelim = sDelim.charAt(0);
                }
            }
        return m_cDelim;
        }


    // ----- data members ----------------------------------------------------

    /**
    * The column name.
    */
    protected String m_sName;

    /**
    * The column name.
    */
    protected String m_sId;

    /**
    * The configuration XML
    */
    protected XmlElement m_xml;

    /**
    * The column delimiter.
    */
    protected char m_cDelim;

    /**
    * Reference to the QueryHandler.
    */
    protected JMXQueryHandler m_queryHandler;

    /**
    * The Value Extractor of the Locator
    */
    protected ValueExtractor m_veExtractor;

    /**
    * The index into the Extractor results for the Locator
    */
    protected int m_iExtractorPos;

    /**
    * The index into the Extractor results for the Locator
    */
    protected int m_iGroupPos;

    /**
    * The index into the aggregator results for the Locator
    */
    protected int m_iAggregatePos;

    /**
    * Datasource where extractor and aggregate results reside.
    */
    protected DataSource m_source;

    /**
    * The flag indicating whether the column should be part of the group by clause.
    */
    protected boolean m_fGroupBy;

    /**
     * The flag indicating whether the column should be hidden.
     */
    protected boolean m_fHidden;
    }
