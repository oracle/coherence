/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;


import com.tangosol.run.xml.XmlElement;

import java.math.BigDecimal;
import java.text.DateFormat;

import java.util.Date;


/**
* Provides column definition for each data element.  Each ColumnView must be
* "backed" by a corresponding ColumnLocator.  The ColumnLocator and the ColumnView
* are "linked" by the QueryHandler through the column identifier getId().
*
* @author ew 2008.01.28
*
* @since Coherence 3.4
*/
public class ColumnView
        implements ReportColumnView, Constants
    {
    /**
    * Sets the configuration of the report column.
    *
    * @param xmlConfig the column xml from the report configuration see
    *                  coherence-report-config.xsd
    */
    public void configure(XmlElement xmlConfig)
        {
        String sId   = xmlConfig.getSafeAttribute("id").getString();
        String sName = xmlConfig.getSafeElement(Reporter.TAG_COLUMNNAME).getString();
        if (sId == null || sId.length() == 0)
            {
            sId = sName;
            }

        String sHeader = xmlConfig.getSafeElement(Reporter.TAG_COLUMNHEAD).getString(null);
               sHeader = sHeader == null ? sName == null ? "" : sName : sHeader;

        m_sId       = sId;
        m_xmlConfig = xmlConfig;
        m_sType     = xmlConfig.getSafeElement(Reporter.TAG_DATATYPE).getString("java.lang.String");
        m_sDesc     = xmlConfig.getSafeElement(Reporter.TAG_DESC).getString(sHeader);

        setHeader(sHeader);
        setVisible(!xmlConfig.getSafeElement(Reporter.TAG_HIDDEN).getBoolean(false));
        }

    /**
    * Configure the ColumnView.
    *
    * @param xmlConfig  the XML configuration
    * @param handler    the QueryHandler
    */
    public void configure(XmlElement xmlConfig, QueryHandler handler)
        {
        setQueryHandler(handler);
        configure(xmlConfig);
        }

    /**
    * Update the QueryHandler for the ColumnView.
    *
    * @param handler    the QueryHandler
    */
    public void setQueryHandler(QueryHandler handler)
        {
        m_handler = handler;
        }

    /**
    * Determine if the ColumnView uses and Aggregate or a Extractor
    *
    * @return true if the ColumnView uses an Aggregate.
    */
    public boolean isAggregate()
        {
        return m_handler.isAggregate(m_sId);
        }

    /**
    * Set the visibility of the column
    *
    * @param fVisible true, the column is displayed, false the column is omitted.
    */
    protected void setVisible(boolean fVisible)
        {
        m_fVisible = fVisible;
        }

    /**
    * Set the column output header
    *
    * @param sHeader value to be displayed on the first row of the file
    */
    protected void setHeader(String sHeader)
        {
        m_sHeader = sHeader;
        }

    /**
    * Get the column view configuration XML.
    */
    public XmlElement getConfig()
        {
        return m_xmlConfig;
        }

    /**
    * Get the type of this column as specified by the configuration XML,
    * or "java.lang.String" if none is specified.
    *
    * @return the canonical name of this column view value's class
    */
    public String getType()
        {
        return m_sType;
        }

    /**
    * Get the description of this column as specified by the configuration XML.
    *
    * @return this column's description
    */
    public String getDescription()
        {
        return m_sDesc;
        }

    /**
    * {@inheritDoc}
    */
    public String getOutputString(Object oObject)
        {
        Object oValue = m_handler.getValue(oObject, m_sId);
        if (oValue == null)
            {
            return "";
            }

        return oValue instanceof Date && m_dateFormat != null
                ? m_dateFormat.format((Date) oValue)
                : oValue instanceof Double ? BigDecimal.valueOf((Double) oValue).toPlainString()
                : oValue.toString();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isRowDetail()
        {
        return m_handler.isDetail(m_sId);
        }

    /**
    * {@inheritDoc}
    */
    public String getHeader()
        {
        return m_sHeader;
        }

    /**
    * {@inheritDoc}
    */
    protected void setDateFormat(DateFormat df)
        {
        m_dateFormat = df;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isVisible()
        {
        return m_fVisible;
        }

    /**
    * {@inheritDoc}
    */
    public String getId()
        {
        return m_sId;
        }

    // ----- data members ----------------------------------------------------

    /**
    * The column view identifier.  There must also be a corresponding
    * column locator with the same identifier.
    */
    protected String m_sId;

    /**
    * The column value type.
    */
    protected String m_sType;

    /**
    * The column description.
    */
    protected String m_sDesc;

    /**
    * The column header.
    */
    protected String m_sHeader;

    /**
     * The DateFormat for displaying date.
     */
    protected DateFormat m_dateFormat;

    /**
    * The flag determining column visiblity in report.
    */
    protected boolean m_fVisible;

    /**
    * The configuration XML
    */
    protected XmlElement m_xmlConfig;

    /**
    * The query handler associated with the column view
    */
    protected QueryHandler m_handler;
    }
