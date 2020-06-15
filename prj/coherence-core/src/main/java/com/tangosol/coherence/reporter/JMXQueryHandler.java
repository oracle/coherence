/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;


import com.tangosol.coherence.reporter.locator.ColumnLocator;
import com.tangosol.coherence.reporter.locator.CorrelatedLocator;
import com.tangosol.coherence.reporter.locator.NodeLocator;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


/**
* Handles creation of the JMX data source and retrieval of the JMX information.
*
* @author ew 2008.02.29
* @since  Coherence 3.4
*/
public class JMXQueryHandler
        implements QueryHandler, Constants
    {
    /**
    * @inheritDoc
    */
    public void setContext(XmlElement xmlQuery, XmlElement xmlReportCfg)
        {
        DataSource source = m_source;
        if (source == null)
            {
            source = m_source = new DataSource();
            }

        if (m_xml == null)
            {
            m_xml          = xmlReportCfg;
            m_xmlColumns   = xmlReportCfg.getElement(TAG_ROW);
            m_listXmlCol   = m_xmlColumns.getElementList();
            m_sQueryTemp   = xmlQuery.getSafeElement(Reporter.TAG_PATTERN).getString();
            m_xmlFilter    = xmlQuery.getSafeElement(Reporter.TAG_FILTERREF);

            boolean fMultiTenant = isMultiTenant();

            // The list of columns not set as Sub-Query if possible.
            List    listQueryCol = m_listQueryCol = xmlQuery.getSafeElement(TAG_PARAMS).getElementList();
            boolean fQueryEmpty  = false;
            if (listQueryCol.size() == 0)
                {
                fQueryEmpty  = true;
                listQueryCol = m_listQueryCol = new ArrayList();
                }

            List    listXmlCol = m_listXmlCol;
            boolean fGroupBy   = m_fGroupBy;
            for (Iterator iterCol = listXmlCol.iterator(); iterCol.hasNext();)
                {
                XmlElement xmlColumn = Reporter.replaceHidden((XmlElement) iterCol.next(), !fMultiTenant);
                boolean    fHidden   = xmlColumn.getSafeElement(TAG_HIDDEN).getBoolean(false);
                boolean    fSubQuery = xmlColumn.getSafeElement(TAG_SUBQUERY).getBoolean(false);

                if (fQueryEmpty && !fSubQuery)
                    {
                    String     sColumnRef = xmlColumn.getSafeAttribute("id").getString();
                    XmlElement xmlTemp    = new SimpleElement(TAG_COLUMNREF, sColumnRef);
                    listQueryCol.add(xmlTemp);
                    }

                // group-by for hidden columns is not supported (see COH-14871)
                fGroupBy |= !fHidden && xmlColumn.getSafeElement("group-by").getBoolean(false);
                }
            m_fGroupBy = fGroupBy;
            }

        }

    /**
    * @inheritDoc
    */
    public XmlElement getContext()
        {
        return m_xml;
        }

    /**
    * @inheritDoc
    */
    public void setPattern(String sPattern)
        {
        m_sQueryTemp = sPattern;
        }

    /**
    * @inheritDoc
    */
    public void postProcess()
        {
        List listColumn = m_listColumns;
        Set  setKeys    = m_setKeys;

        for (Iterator iterCol = listColumn.iterator(); iterCol.hasNext();)
            {
            ColumnLocator columnLocator = (ColumnLocator) iterCol.next();
            if (columnLocator != null)
                {
                columnLocator.reset(setKeys);
                }
            }
        m_source.postProcess();
        }

    /**
    * @inheritDoc
    */
    public void execute()
        {
        m_ldtStartTime = System.currentTimeMillis();

        DataSource source = m_source;

        source.setGroupBy(m_fGroupBy);

        initColumns();

        MBeanQuery query   = ensureBeanQuery();
        Set        setKeys = getKeys();

        source.execute(query, setKeys);
        }

    /**
    * Obtain the keys for the report.
    *
    * @return a set containing the keys from the query
    */
    public Set getKeys()
        {
        MBeanQuery mapBeans   = ensureBeanQuery();
        XmlElement xmlFilters = m_xml.getElement(TAG_FILTERS);
        Set        setKeys;

        if (xmlFilters == null)
            {
            setKeys = mapBeans.keySet();
            }
        else
            {
            FilterFactory filterFactory = new FilterFactory(this, xmlFilters);
            String        sId           = m_xmlFilter.getString("");
            if (sId.length() == 0)
                {
                setKeys = mapBeans.keySet();
                }
            else
                {
                Filter filter = filterFactory.getFilter(sId);

                setKeys = filter == null ? mapBeans.keySet() : mapBeans.keySet(filter);
                }
            }

        m_setKeys = setKeys;
        return setKeys;
        }

    /**
    * Obtain the keys for the group by report.
    *
    * @return a set containing the keys from the group by
    */
    public Set getGroupKeys()
        {
        return m_source.getGroupKeys();
        }

    /**
    * Obtain the value for the key and column from the ColumnLocator
    *
    * @param key        the key for the object
    * @param oSourceId  the ColumnLocator identifier
    *
    * @return the value of the column
    */
    public Object getValue(Object key, Object oSourceId)
        {
        Map           mapSources = m_mapColumns;
        ColumnLocator qc         = (ColumnLocator) mapSources.get(oSourceId);

        return qc == null ? null : qc.getValue(key);
        }

    /**
    * Determine if the Column Identifier is an aggregate.
    *
    * @param column  the column identifier
    *
    * @return true if the column is an aggregate
    */
    public boolean isAggregate(Object column)
        {
        Map           mapColumns = m_mapColumns;
        ColumnLocator qc         = (ColumnLocator) mapColumns.get(column);

        return qc != null && qc.isAggregate();
        }

    /**
    * Determine if the Column Identifier has detail values.
    *
    * @param column  the column identifier
    *
    * @return true if the column is an aggregate
    */
    public boolean isDetail(Object column)
        {
        Map           mapColumns = m_mapColumns;
        ColumnLocator qc         = (ColumnLocator) mapColumns.get(column);

        return qc != null && qc.isRowDetail();
        }

    /**
    * Return whether true if any ObjectNames under the query expression that
    * limits the report has a multi-tenant attribute.
    *
    * @return true if ObjectNames with a multi-tenant attibute exist
    */
    public boolean isMultiTenant()
        {
        Boolean FMultiTenant = m_FMultiTenant;
        if (FMultiTenant == null)
            {
            String sPattern = replaceMacros(m_sQueryTemp, null);
            try
                {
                Optional<ObjectName> optName = getMBeanServer()
                    .queryNames(new ObjectName(sPattern), null)
                        .stream()
                        .filter(name -> name.getKeyProperty(Constants.DOMAIN_PARTITION) != null)
                        .findAny();

                FMultiTenant = m_FMultiTenant = Boolean.valueOf(optName.isPresent());
                }
            catch (MalformedObjectNameException e)
                {
                e.printStackTrace();
                }
            }
        return FMultiTenant.booleanValue();
        }

    /**
    * Initialize locator.
    */
    protected void initColumns()
        {
        Map  mapColumns = m_mapColumns;
        List listColumn = m_listColumns;

         // Initialize locator.  Column renames will override macros.
        if (mapColumns.isEmpty())
            {
            for (Iterator iterCol = m_listQueryCol.iterator(); iterCol.hasNext();)
                {
                XmlElement    xmlRef  = (XmlElement) iterCol.next();
                String        sColRef = xmlRef.getString();
                ColumnLocator locator = ensureColumnLocator(xmlRef, sColRef);
                if (locator != null)
                    {
                    listColumn.add(locator);
                    mapColumns.put(locator.getId(), locator);
                    if (locator instanceof CorrelatedLocator)
                        {
                        ((CorrelatedLocator) locator).setCorrellatedObject(this.m_oCorrelated);
                        }
                    }
                }
            }
        else
            {
            for (Iterator iter = listColumn.iterator(); iter.hasNext();)
                {
                ColumnLocator cl = (ColumnLocator)iter.next();
                cl.configure(cl.getConfig());
                cl.setDataSource(m_source); // Set the new datasource
                if (cl instanceof CorrelatedLocator)
                    {
                    ((CorrelatedLocator) cl).setCorrellatedObject(this.m_oCorrelated);
                    }
                }
            }
        }

    /**
    * Obtains a QueryColumn instance based on the XML configuration.
    *
    * @param xmlColumn   the column definition XML
    *
    * @return a QueryColumn instance
    */
    public ColumnLocator ensureColumnLocator(XmlElement xmlColumn)
        {
        if (xmlColumn == null)
            {
            return null;
            }

        XmlValue   xmlId      = xmlColumn.getAttribute("id");
        Map        mapColumns = ensureSourceMap();
        XmlElement xmlColDef  = xmlColumn;
        String     sId;

        if (xmlId != null)
            {
            sId       = xmlId.getString();
            xmlColDef = getColumnCfg(xmlColumn, sId);
            }

        ColumnLocator columnLocator = (ColumnLocator) mapColumns.get(getColumnKey(xmlColumn));
        if (columnLocator == null)
            {
             try
                 {
                 columnLocator = newColumnLocator(xmlColDef);
                 columnLocator.configure(xmlColDef, this, m_source);
                 mapColumns.put(getColumnKey(xmlColDef), columnLocator);
                 }
             catch (IllegalArgumentException e)
                 {
                 // Missing column-ref is already logged. See getColumnCfg()
                 return null;
                 }
             catch (Exception e) // ClassNotFoundException, InstantiationException, IllegalAccessException
                 {
                 // Log the Error and Continue.
                 Base.log(e);
                 return null;
                 }
            }
        return columnLocator;
        }

    /**
    * Obtain the Map of existing locator for the Reporter keyed by column id.
    *
    * @return the Map of locator
    */
    public Map ensureSourceMap()
        {
        Map mapColumns = m_mapColumns;
        if (mapColumns == null)
            {
            m_mapColumns = mapColumns = new HashMap();
            }
        return mapColumns;
        }

    /**
    * Instantiate a new report column based on the column configuration XML.
    *
    * @param xmlColumn  the column definition XML
    *
    * @return the ColumnLocator to access the data
    */
    public static ColumnLocator newColumnLocator(XmlElement xmlColumn)
        {
        String sTypeValue = xmlColumn.getSafeElement(TAG_COLUMNTYPE).getString(VALUE_ATTRIB);
        String sClass;

        if (sTypeValue.equals(VALUE_CUSTOM))
            {
            sClass = xmlColumn.getSafeElement(TAG_CLASS).getString();
            }
        else
            {
            int nType = columnFromString(sTypeValue);
            switch (nType)
                {
                case COL_GLOBAL:
                    sTypeValue += "," + xmlColumn.getSafeElement(TAG_COLUMNNAME).getString();
                    break;

                case COL_CALC:
                    sTypeValue += "," + xmlColumn.getSafeElement(TAG_COLUMNFUNC).getString();
                    break;
                }

            sClass = (String) m_mapColumnClass.get(sTypeValue);
            }

        if (sClass == null || sClass.length() == 0)
            {
            sClass = sTypeValue;
            }

        try
            {
            return (ColumnLocator) ClassHelper.newInstance(Class.forName(sClass), null);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e,
                    "Failed to instantiate ColumnLocator " + sClass);
            }
        }

    /**
    * Obtain the list of locator for the Reporter.
    *
    * @return the List of locator
    */
    protected List ensureColumnList()
        {
        List listColumns = m_listColumns;
        if (listColumns == null)
            {
            m_listColumns = listColumns = new LinkedList();
            }
        return listColumns;
        }

    /**
    * Returns the XML column config given the column id.
    *
    * @param xmlColumn  the XML definition which references the column id
    * @param sId        the string identifier to locate
    *
    * @return the requested column configuration XML, null if not found
    */
    protected XmlElement getColumnCfg(XmlElement xmlColumn, String sId)
        {
        List listXml = m_listXmlCol;

        for (Iterator i = listXml.iterator(); i.hasNext();)
            {
            XmlElement xmlSub = (XmlElement) i.next();
            String     sTemp  = xmlSub.getSafeAttribute("id").getString();
            if (sTemp.equals(sId))
                {
                return xmlSub;
                }
            }

        Base.log("Unable to locate column-ref \""+sId+"\"");
        return null;
        }

    /**
    * Determine the key for the m_mapColumns given column XML.
    *
    * @param xmlColumn  the column definition XML
    *
    * @return the id attribute of the column or the column configuration XML
    */
    protected static Object getColumnKey(XmlElement xmlColumn)
        {
        XmlValue xmlTemp = xmlColumn.getAttribute("id");
        if (xmlTemp == null)
            {
            return xmlColumn;
            }
        return xmlTemp.getString();
        }

    /**
    * Replace all "macro" strings with the value from the ColumnLocator.
    *
    * @param sTemplate  the template contain the macros
    *
    * @return a string based on the Template with JMX values included
    */
    public String replaceMacros(String sTemplate, Object oSource)
        {
        String sRet               = sTemplate;
        Set    setMacros          = Reporter.getMacros(sTemplate);
        Map    mapMacroExtractors = m_mapMacroExtractors;

        for (Iterator iter = setMacros.iterator(); iter.hasNext();)
            {
            String         sId = (String) iter.next();
            ColumnLocator  nl  = (ColumnLocator) mapMacroExtractors.get(sId);
            ValueExtractor ve  = nl == null ? ensureExtractor(sId) : nl.getExtractor();

            if (ve != null)
                {
                Object oValue = ve.extract(oSource);
                if (oValue != null)
                    {
                    sRet = sRet.replaceAll(MACRO_START + sId + MACRO_STOP,
                                oValue.toString());
                    }
                }
            }
        return sRet;
        }

    /**
    * Create the ColumnLocator based on the XML configuration of the sId.
    *
    * @param xmlColRef  a column-ref XmlElement
    * @param sId        the column sId to create a ColumnLocator for
    *
    * @return a ColumnLocator configured with the XML
    */
    public ColumnLocator ensureColumnLocator(XmlElement xmlColRef, String sId)
        {
        return ensureColumnLocator(getColumnCfg(xmlColRef, sId));
        }

    /**
    * Create/Obtain the MBeanQuery.
    *
    * @return the MBeanQuery
    */
    protected MBeanQuery ensureBeanQuery()
        {
        MBeanQuery query = m_mbeanQuery;
        if (query == null)
            {
            String      sTemplate  = m_sQueryTemp;
            String      sQuery     = replaceMacros(sTemplate, null);
            m_mbeanQuery = query = new MBeanQuery(sQuery, getMBeanServer());
            }
        return query;
        }

    /**
    * Obtain the ValueExtractor from the column definition XML.
    *
    * @param xmlColumn  the column definition XML
    *
    * @return the ValueExtractor for the column
    */
    public ValueExtractor ensureExtractor(XmlElement xmlColumn)
        {
        String sId = xmlColumn.getAttribute("id").getString();
        ensureColumnLocator(xmlColumn);
        return ensureExtractor(sId);
        }

    /**
    * Obtain the ValueExtractor from the Extractor Identifier.
    *
    * @param sExtractorId  the column sId
    *
    * @return the ValueExtractor for the column
    */
    public ValueExtractor ensureExtractor(String sExtractorId)
        {
        ColumnLocator qc = ensureColumnLocator(null, sExtractorId);
        return qc == null ? null : qc.getExtractor();
        }


    // ----- accessors and helpers -------------------------------------------

    /**
    * Set the correlated key target for a sub query.
    *
    * @param oTarget  the key that should be used for accessing values in the
    *                 outer query
    */
    public void setCorrelated(Object oTarget)
        {
        m_oCorrelated = oTarget;
        }

    /**
    * Get the current running batch identifier.
    *
    * @return the batch identifier
    */
    public long getBatch()
        {
        return m_lBatch;
        }

    /**
    * Set the batch identifier.
    *
    * @param lBatch  the batch identifier
    */
    protected void setBatch(long lBatch)
        {
        m_lBatch = lBatch;
        }

    /**
    * Get the time the report started execution.
    *
    * @return the long representing the time the report started
    */
    public long getStartTime()
        {
        return m_ldtStartTime;
        }

    /**
    * Convert the column string into the internal representation.
    *
    * @param sType  the string representation of the column type
    *
    * @return the internal representation of the column type
    */
    protected static int columnFromString(String sType)
        {
        if (sType.equals(VALUE_GLOBAL))
            {
            return COL_GLOBAL;
            }
        else if (sType.equals(VALUE_COLCALC))
            {
            return COL_CALC;
            }
        else if (sType.equals(VALUE_METHOD))
            {
            return COL_METHOD;
            }
        else if (sType.equals(VALUE_KEY))
            {
            return COL_KEY;
            }
        else if (sType.equals(""))
            {
            return COL_ATTRIB;
            }
        else if (sType.equals(VALUE_ATTRIB))
            {
            return COL_ATTRIB;
            }
        return COL_ERR;
        }

    /**
    * Return an appropriate {@link MBeanServer} to use.
    *
    * @return an appropriate MBeanServer to use
    */
    protected MBeanServer getMBeanServer()
        {
        return m_source == null ? null : m_source.getMBeanServer();
        }

    // ----- data members ----------------------------------------------------

    /**
    * The batch identifier for the query.
    */
    protected long m_lBatch;

    /**
    * The start time of the query
    */
    protected long m_ldtStartTime;

    /**
    * A list of ColumnLocators for the report
    */
    protected List m_listColumns = new LinkedList();

    /**
    * Either the ColumnViews included in the query or the columns specified in the .xml
    */
    protected List m_listQueryCol;

    /**
    * A map from getColumnKey() to ColumnLocator for the report
    */
    protected Map m_mapColumns = new HashMap();

    /**
    * A list of Column XML Elements to be made into locators for the report
    */
    protected List m_listXmlCol;

    /**
    * The query/pattern template for the report
    */
    protected String m_sQueryTemp;

    /**
    * The report configuration XML
    */
    protected XmlElement m_xml;

    /**
    * The filter configuration XML
    */
    protected XmlElement m_xmlFilter;

    /**
    * The XmlElement holding the list of columns to be included in the Report
    */
    protected XmlElement m_xmlColumns;

    /**
    * The query MBean
    */
    protected MBeanQuery m_mbeanQuery;

    /**
    * The attribute and aggregate data source.
    */
    protected DataSource m_source;

    /**
    * The result set from the query.
    */
    protected Set m_setKeys;

    /**
    * The correlated target key for sub query.
    */
    protected Object m_oCorrelated;

    /**
    * Idicates whether there are any Group-By columns.
    */
    protected boolean m_fGroupBy;

    /**
    * Whether MBeans with a multi-tenant attribute exist.
    */
    protected Boolean m_FMultiTenant;

    /**
    * A static Map between the String name of the macro and the implementation of it.
    */
    public static Map m_mapMacroExtractors = new HashMap();

    /**
    * A static Map between the filter XML definition and the implementation class
    * name.
    */
    public static Map m_mapColumnClass = new HashMap();

    static
        {
        String sBase = "com.tangosol.coherence.reporter.locator.";
        m_mapMacroExtractors.put(MACRO_NODE, new NodeLocator());
        m_mapColumnClass.put(VALUE_ATTRIB, sBase + "AttributeLocator");
        m_mapColumnClass.put(VALUE_METHOD, sBase + "OperationLocator");
        m_mapColumnClass.put(VALUE_SUBQUERY, sBase + "SubQueryLocator");
        m_mapColumnClass.put(VALUE_CONSTANT, sBase + "ConstantLocator");
        m_mapColumnClass.put(VALUE_CORRELATED, sBase + "CorrelatedLocator");
        m_mapColumnClass.put(VALUE_PROPERTY, sBase + "PropertyLocator");
        m_mapColumnClass.put(VALUE_GLOBAL + "," + VALUE_BATCH, sBase + "BatchLocator");
        m_mapColumnClass.put(VALUE_GLOBAL + "," + MACRO_START + MACRO_NODE + MACRO_STOP, sBase + "NodeLocator");        /////   Do you want this macro to exist in the columns
        m_mapColumnClass.put(VALUE_GLOBAL + "," + VALUE_TIME, sBase + "DateTimeLocator");
        m_mapColumnClass.put(VALUE_KEY, sBase + "KeyLocator");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_SUM, sBase + "SumLocator");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_COUNT, sBase + "CountLocator");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_AVG, sBase + "AverageLocator");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_MIN, sBase + "MinLocator");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_MAX, sBase + "MaxLocator");

        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_DELTA, sBase + "DeltaLocator");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_DIVIDE, sBase + "DivideLocator");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_ADD, sBase + "AddLocator");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_SUB, sBase + "SubtractLocator");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_MULTI, sBase + "MultiplyLocator");
        }
    }
