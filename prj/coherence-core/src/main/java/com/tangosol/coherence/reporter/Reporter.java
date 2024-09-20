/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;

import com.tangosol.util.Base;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.text.DateFormat;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

/**
* The Reporter can create a CSV file or a TabularData object using a report
* configuration descriptor compliant with coherence-report-config.xsd or
* coherence-report-group-config.xsd.
*
* @author ew 2008.02.28
* @since Coherence 3.4
*/
public class Reporter
        extends    Base
        implements Constants
    {
    /**
    * Run the specified report.
    * <pre>
    * Usage:
    *   java com.tangosol.coherence.reporter.Reporter config-path [output-directory] [batchId]
    * </pre>
    */
    public static void main(String[] asArg)
        {
        int cArgs = asArg.length;
        if (cArgs == 0)
            {
            showUsage();
            return;
            }

        String sConfigFile = asArg[0];
        String sOutputPath = cArgs > 1 ? asArg[1] : "";
        long   lBatchId    = cArgs > 2 ? Long.parseLong(asArg[2]) : 1;

        CacheFactory.ensureCluster();
        new Reporter().run(sConfigFile, sOutputPath, lBatchId, null, null);
        }

    /**
    * Generate the CSV file based on the specified configuration.
    *
    * @param sReport        the URI or the content of the report
    * @param sPathTemplate  the output path template
    * @param lBatch         a unique identifier for the report execution
    * @param xmlParams      the initialization parameters
    */
    public void run(String sReport, String sPathTemplate, long lBatch, XmlElement xmlParams)
        {
        run(sReport, sPathTemplate, null, lBatch, xmlParams, null, true, false);
        }

    /**
    * Generate the CSV file based on the specified configuration.
    *
    * @param sReport        the URI or the content of the report
    * @param sPathTemplate  the output path template
    * @param lBatch         a unique identifier for the report execution
    * @param xmlParams      the initialization parameters
    * @param loader         the class loader that should be used to load
    *                       report definition
    */
    public void run(String sReport, String sPathTemplate, long lBatch, XmlElement xmlParams, ClassLoader loader)
        {
        run(sReport, sPathTemplate, null, lBatch, xmlParams, loader, true, false);
        }

    /**
    * Generate the CSV file and/or TabularData based on the specified
    * configuration.
    *
    * @param sReport        the URI or the content of the report
    * @param sPathTemplate  the output path template
    * @param sTabularType   the typeName of the {@code TabularType}
    * @param lBatch         a unique identifier for the report execution
    * @param xmlParams      the initialization parameters
    * @param loader         the class loader that should be used to load
    *                       report definition
    * @param fReportFile    true iff results should be written to a file
    * @param fTabular       true iff we generate and return the values as a tabular data
    *
    * @return a TabularData of the result if fTabular is set to true, null otherwise
    */
    public TabularData run(String sReport, String sPathTemplate, String sTabularType, long lBatch, XmlElement xmlParams,
                    ClassLoader loader, boolean fReportFile, boolean fTabular)
        {
        setBatch(lBatch);

        TabularData tabData   = null;
        XmlElement  xmlConfig = m_xml;
        if (xmlConfig == null)
            {
            boolean fURI = isURI(sReport);

            XmlElement xmlReports = fURI
                        ? XmlHelper.loadFileOrResource(sReport, "Reporter configuration", loader)
                        : XmlHelper.loadXml(sReport);

            xmlReports = replaceParams(xmlReports, xmlParams);
            xmlConfig  = xmlReports.getElement(TAG_REPORT);
            if (xmlConfig == null)
                {
                return null;
                }

            setConfig(xmlConfig);
            }

        XmlElement   xmlQuery = xmlConfig.getElement(TAG_QUERY);
        QueryHandler handler  = ensureQueryHandler(xmlConfig, xmlQuery, lBatch);

        // Execute the query.
        handler.execute();

        Set<MBeanQuery.Entry> setBeans = handler.getKeys();

        if (setBeans.size() > 0)
            {
            if (!m_fInitialized)
                {
                initDisplayColumns(handler.isMultiTenant());
                m_fInitialized = true;
                }

            if (fReportFile)
                {
                writeReportFile(sPathTemplate, handler);
                }

            if (fTabular)
                {
                tabData = tabular(handler, sTabularType);
                }
            }

        // apply deltas and clean up
        handler.postProcess();

        return tabData;
        }

    /**
    * Write the contents of the provided handler into a file in the given path template.
    *
    * @param sPathTemplate  the output path template
    * @param handler        the QueryHandler with the execution results
    */
    protected void writeReportFile(String sPathTemplate, QueryHandler handler)
        {
        char        cDelim      = getDelim();
        String      sDesc       = getDescTemplate();
        File        fileOut     = getFile(sPathTemplate);
        PrintStream streamOut   = getPrintStream(fileOut);
        List        listDisplay = m_listDisplay;

        // Write out report description and headers for new output files
        if (m_fHeaders && fileOut.length() == 0)
            {
            sDesc = replaceMacros(sDesc, null);
            if (sDesc.length() > 0)
                {
                writeDescription(streamOut, sDesc);
                }
            writeHeader(streamOut, listDisplay, cDelim);
            }

        boolean fDetail = false; // true if any column's value is row dependent
        for (Iterator iterCol = listDisplay.iterator(); iterCol.hasNext();)
            {
            ReportColumnView columnView = (ReportColumnView) iterCol.next();
            if (columnView != null)
                {
                fDetail |= columnView.isRowDetail() && columnView.isVisible();
                }
            }

        if (fDetail)
            {
            // write the details;
            for (Iterator iter = handler.getGroupKeys().iterator(); iter.hasNext(); )
                {
                writeDetail(streamOut, listDisplay, iter.next(), cDelim);
                }
            }
        else
            {
            // Write the aggregates and constants only
            Iterator iter = handler.getGroupKeys().iterator();

            // group-keys are row identifiers; any row will do for aggregates
            Object oFirstKey = iter.hasNext()
                    ? iter.next()
                    : null;

            writeDetail(streamOut, listDisplay, oFirstKey, cDelim);
            }
        streamOut.close();
        }

    /**
    * Return the contents of the provided QueryHandler as a TabularData indexed by
    * the handler's keys.
    *
    * @param handler       the QueryHandler to transform into a TabularData
    * @param sTabularType  the typeName of the {@code TabularType}
    *
    * @return the TabularData with values from the QueryHandler
    */
    protected TabularData tabular(QueryHandler handler, String sTabularType)
        {
        // create the TabularType for this report
        List listDisplay = m_listDisplay;
        int  cVisibleCol = 0;
        for (Iterator iterCol = listDisplay.iterator(); iterCol.hasNext();)
            {
            ColumnView columnView = (ColumnView) iterCol.next();
            if (columnView != null && columnView.isVisible())
                {
                cVisibleCol++;
                }
            }

        int        cTotalCols   = cVisibleCol + 1; // number of visible columns + rowID column
        String[]   aColumnNames = new String[cTotalCols];
        String[]   aColumnDesc  = new String[cTotalCols];
        OpenType[] aOpenTypes   = new OpenType[cTotalCols];
        int        iCol         = 0;

        aColumnNames[iCol] = "rowID";
        aColumnDesc[iCol]  = "Unique Row ID";
        aOpenTypes[iCol++] = SimpleType.INTEGER;

        for (Iterator iterCol = listDisplay.iterator(); iterCol.hasNext();)
            {
            ColumnView columnView = (ColumnView) iterCol.next();
            if (columnView != null && columnView.isVisible())
                {
                aColumnNames[iCol] = columnView.getId();
                aColumnDesc[iCol]  = columnView.getDescription();
                aOpenTypes[iCol]   = getSimpleType(columnView.getType());

                iCol++;
                }
            }

        String sDesc = getDescTemplate();
        sDesc = sDesc == null || sDesc.length() == 0 ? sTabularType : sDesc;

        TabularType tabType;

        try
            {
            CompositeType rowType = new CompositeType(sTabularType, sDesc, aColumnNames, aColumnDesc, aOpenTypes);

            tabType = new TabularType(sTabularType, sDesc, rowType, aColumnNames);
            }
        catch (OpenDataException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        // create the TabularData for this report
        TabularDataSupport tabDataSupport = new TabularDataSupport(tabType);
        CompositeType      rowType        = tabDataSupport.getTabularType().getRowType();

        try
            {
            int iRow = 1;
            for (Iterator iterKey = handler.getGroupKeys().iterator(); iterKey.hasNext(); )
                {
                Object oKey       = iterKey.next();
                Map    mapRowData = new HashMap();

                for (Iterator iterCol = rowType.keySet().iterator(); iterCol.hasNext();)
                    {
                    String sColID = (String) iterCol.next();
                    Object oValue = handler.getValue(oKey, sColID);
                    if (rowType.getType(sColID) == SimpleType.STRING)
                        {
                        // For arrays, simply return its stringified representation.
                        oValue = oValue == null ? null
                                                : oValue.getClass().isArray()
                                                    ? Arrays.deepToString((Object[]) oValue)
                                                    : oValue.toString();
                        }
                    mapRowData.put(sColID, oValue);
                    }
                mapRowData.put("rowID", iRow);

                tabDataSupport.put(new CompositeDataSupport(rowType, mapRowData));
                iRow++;
                }
            }
        catch (OpenDataException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        return tabDataSupport;
        }

    /**
    * Convert the given String identifier into the corresponding SimpleType.
    *
    * @param sType  the name of the type
    *
    * @return the SimpleType for the type name, or SimpleType.STRING if an invalid name is given
    */
    protected static SimpleType getSimpleType(String sType)
        {
        SimpleType type = MBeanHelper.SCALAR_SIMPLETYPES.get(sType);
        return type == null ? SimpleType.STRING : type;
        }

    /**
    * Replace the system-properties and init-params in the XML.
    *
    * @param xmlReports  the XML to modify
    * @param xmlParams   the init-params XML
    *
    * @return the report definition XML with system-property and init-param
    *         values
    */
    public XmlElement replaceParams(XmlElement xmlReports, XmlElement xmlParams)
        {
        XmlHelper.replaceSystemProperties(xmlReports, "system-property");
        XmlElement xmlParseParams = new SimpleElement();
        if (xmlParams != null)
            {
            XmlHelper.transformInitParams(xmlParseParams, xmlParams);
            }
        replaceInitParams(xmlReports, xmlParseParams);
        return xmlReports;
        }

    /**
    * Ensure ReportColumnViews.
    */
    protected void initDisplayColumns(boolean fMultiTenant)
        {
        List listXmlCol  = m_listXmlCol;
        Map  mapColumns  = m_mapColumns;
        List listDisplay = m_listDisplay;

         // Initialize ColumnViews. Column renames will override macros.
        if (mapColumns.isEmpty())
            {
            for (Iterator iterCol = listXmlCol.iterator(); iterCol.hasNext();)
                {
                XmlElement       xmlColumn  = (XmlElement) iterCol.next();
                ReportColumnView columnView = ensureDisplayColumn(replaceHidden(xmlColumn, !fMultiTenant));

                listDisplay.add(columnView);
                mapColumns.put(columnView.getId(), columnView);
                }

            if (mapColumns.get(MACRO_BATCH) == null)
                {
                BatchView view = new BatchView();
                view.setQueryHandler(m_queryHandler);
                mapColumns.put(MACRO_BATCH, view);
                }

            if (mapColumns.get(MACRO_DATE) == null)
                {
                DateView view = new DateView();
                view.setQueryHandler(m_queryHandler);
                mapColumns.put(MACRO_DATE, view);
                }

            if (mapColumns.get(MACRO_NODE) == null)
                {
                mapColumns.put(MACRO_NODE, new NodeView());
                }
            }
        }

    /**
    * Obtains a ReportDisplay instance based on the XML configuration.
    *
    * @param xmlColumn  the column definition XML
    *
    * @return a ReportDisplay instance
    */
    public ReportColumnView ensureDisplayColumn(XmlElement xmlColumn)
        {
        XmlValue   xmlId      = xmlColumn.getAttribute("id");
        XmlElement xmlColDef  = xmlColumn;
        String     sId;

        if (xmlId != null)
            {
            sId       = xmlId.getString();
            xmlColDef = getColumnById(xmlColumn, sId);
            }

        ColumnView columnView = new ColumnView();
        columnView.setQueryHandler(m_queryHandler);
        columnView.configure(xmlColDef);
        columnView.setDateFormat(m_dateFormat);
        return columnView;
        }

    /**
    * Returns mapColumns key for the given column XML.
    *
    * @param xmlColumn  the XML definition which references the column id
    * @param sId        the string identifier to locate
    *
    * @return the id attribute of the column or the column configuration XML
    */
    protected XmlElement getColumnById(XmlElement xmlColumn, String sId)
        {
        XmlElement xmlRow  = getRowXml(xmlColumn);
        List       listXml = xmlRow.getElementList();
        for (Iterator i = listXml.iterator(); i.hasNext();)
            {
            XmlElement xmlSub = (XmlElement) i.next();
            String     sTemp  = xmlSub.getSafeAttribute("id").getString();
            if (sTemp.equals(sId))
                {
                return xmlSub;
                }
            }
        return null;
        }

    /**
    * Obtain the row xml for a given column.
    *
    * @param xml  a column xml or column reference
    *
    * @return the XmlElement for the row
    */
    protected static XmlElement getRowXml(XmlElement xml)
        {
        XmlElement xmlTemp = xml;
        while (xmlTemp.getElement(TAG_ROW) == null)
            {
            xmlTemp = xmlTemp.getParent();
            if (xmlTemp == null)
                {
                return null;
                }
            }
        return xmlTemp.getElement(TAG_ROW);
        }

    /**
    * Writes an array of strings to the output file.
    *
    * @param wps    the output PrintStream
    * @param sDesc  the Description to be written
    */
     protected void writeDescription(PrintStream  wps, String sDesc)
         {
         wps.println(sDesc);
         // without this line sometimes the following row is omitted.
         wps.flush();
         }

    /**
    * Writes the data from the list of columnViews to the file.
    *
    * @param wps         the output PrintStream
    * @param listColumn  list of columns to be displayed
    * @param oKey        the row identifier
    * @param cDelim      the column delimiter
    */
     protected void writeDetail(PrintStream wps, List listColumn, Object oKey, char cDelim)
         {
         int c = 0;
         for (Iterator iCol = listColumn.iterator(); iCol.hasNext(); )
             {
             ReportColumnView columnView = (ReportColumnView) iCol.next();
             if (columnView != null && columnView.isVisible())
                 {
                 if (c != 0)
                    {
                    wps.print(cDelim);
                    }
                 c++;

                 wps.print(columnView.getOutputString(oKey));
                 }
             }
         wps.println();
         wps.flush();
         }

    /**
    * Writes an array of strings to the output file.
    *
    * @param wps         the output PrintStream
    * @param listColumn  list of columnViews to be displayed
    * @param cDelim      the column delimiter
    */
     protected void writeHeader(PrintStream wps, List listColumn, char cDelim)
         {
         String sData;
         int c = 0;
         for (Iterator iCol = listColumn.iterator(); iCol.hasNext(); )
             {
             ReportColumnView columnView = (ReportColumnView) iCol.next();
             if (columnView != null)
                 {
                 if (columnView.isVisible())
                     {
                     if (c != 0)
                         {
                         wps.print(cDelim);
                         }
                     c++;
                     Object oResult = columnView.getHeader();
                     sData = oResult == null ? "" : oResult.toString();
                     wps.print(sData);
                     }
                 }
             }
         wps.println();
         // without this line sometimes the header is not printed correctly.
         wps.flush();
         }

    /**
    * Return the first part of a '/' delimited string.
    *
    * @param  sKey  a '/' delimited path string for composite data
    *
    * @return the first part of the path
    */
    public String currentKey(String sKey)
        {
        if (sKey.length() == 0)
            {
            return "";
            }

        String[] arKey = Base.parseDelimitedString(sKey, '/');

        return arKey[0];
        }

    /**
    * Extract the column delimiter from the report configuration.
    *
    * @return the column delimiter character
    */
    protected char getDelim()
        {
        char cDelim = m_cDelim;
        if (cDelim == Character.UNASSIGNED)
            {
            String sDelim = m_xml.getSafeElement(TAG_DELIM).getString(VALUE_TAB);

            m_cDelim = cDelim = sDelim.equals(VALUE_TAB)
                    ? '\t'
                    : sDelim.equals(VALUE_SPACE) ? ' ' : sDelim.charAt(0);
            }

        return cDelim;
        }

    /**
    * Obtain the report description template.
    *
    * @return the report description template
    */
    public String getDescTemplate()
        {
        return m_sDescTemplate;
        }

    /**
    * Obtain the Map of existing locator for the Reporter keyed by column id.
    *
    * @return the Map of locator
    */
    public Map ensureColumnMap()
        {
        Map mapColumns = m_mapColumns;
        if (mapColumns == null)
            {
            m_mapColumns = mapColumns = new HashMap();
            }
        return mapColumns;
        }

    /**
    * Obtain the report batch.
    *
    * @return the report batch number
    */
    public long getBatch()
        {
        return m_lBatch;
        }

    /**
    * Set the report batch.
    *
    * @param lBatch  the report batch number
    */
    public void setBatch(long lBatch)
        {
        m_lBatch = lBatch;
        }

    /**
    * Obtain the list of locator for the Reporter.
    *
    * @return the List of locator
    */
    protected List ensureDisplayList()
        {
        if (m_listDisplay == null)
            {
            m_listDisplay = new LinkedList();
            }
        return m_listDisplay;
        }

    /**
    * Determine the output file name and create the file.
    *
    * @param  sPathTempl  the output path template
    *
    * @return the output file for the report
    */
    protected File getFile(String sPathTempl)
        {
        try
            {
            String sFileTempl = m_sFileTempl;
            String sFileName  = replaceMacros(
               getFileTemplate(sFileTempl, sPathTempl), null);

            if (sFileName.length() == 0)
                {
                log("Report writer:No output file specified. Report terminated");
                return null;
                }

            File fOut = new File(sFileName);
            if (!fOut.exists())
                {
                fOut.createNewFile();
                }

            return fOut;
            }
         catch (IOException e) // FileNoteFoundException
            {
            throw ensureRuntimeException(e, "Invalid or unable to create output file.");
            }
        }

    /**
    * Determine the output file name, create the file and write the column headers.
    *
    * @param fOut  the output File
    *
    * @return the output file for the report
    */
    protected PrintStream getPrintStream(File fOut)
        {
        try
            {
            OutputStream os  = new FileOutputStream(fOut, true);
            return new PrintStream(new BufferedOutputStream(os));
            }
        catch (FileNotFoundException e)
            {
            // This error will never occur.  The file will be created.
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Get the output file template.
    *
    * @param sTemplate  the filename template
    * @param sPath      the path for the file
    *
    * @return the output file name
    */
    protected String getFileTemplate(String sTemplate, String sPath)
        {
        String sFileName = sTemplate;

        if (sFileName.length() == 0)
            {
            log("Report writer:No output file specified. Report terminated");
            return null;
            }

        try
            {
            sFileName = new File(sPath).getCanonicalPath() + File.separatorChar + sFileName;
            }
        catch (IOException e)
            {
            // leave the problem for caller
            sFileName = sPath + sFileName;
            }

        return sFileName;
        }

    /**
    * Set the {@link DateFormat} that will be used when formatting date columns.
    *
    * @param df  the {@link DateFormat}
    */
    public void setDateFormat(DateFormat df)
        {
        m_dateFormat = df;
        }

    /**
    * Output usage instructions.
    */
    public static void showUsage()
        {
        out();
        out("java com.tangosol.coherence.reporter.Reporter <report-config> [[<output-directory> [<batchId>]]");
        out();
        out("command option descriptions:");
        out("\t<report-config> the file containing the report configuration XML");
        out();
        out("\t<output-directory> (optional) the path where the output files are created.");
        out("\t(default is the current directory)");
        out();
        out("\t<batchId> (optional) a long indentifier for the report execution.");
        out("\tThe batchId is useful when differientiating data from different");
        out("\texecutions contained in a single file");
        out("\t (default is 1)");
        out();
        }


     /**
     * Return true if the {@code sConfigFileName} is a URI containing a report config file
     * rather than a String containing the XML for the report.
     *
     * @param sConfigFileName  the config file or XML
     *
     * @return true if the config file name is a URI
     */
     protected static boolean isURI(String sConfigFileName)
         {
         try
             {
             new URI(sConfigFileName);
             return true;
             }
         catch (URISyntaxException e)
             {
             return false;
             }
        }

    /**
     * Replace the column macro in the hidden attribute.
     *
     * @param  xml     the XmlElement to replace the macro
     * @param  fValue  whether the attribute should be hidden
     *
     * @return the XmlElement after the macro is replaced
     */
    protected static XmlElement replaceHidden(XmlElement xml, boolean fValue)
        {
        Set setMacros = Reporter.getMacros(xml.toString());

        for (Iterator iter = setMacros.iterator(); iter.hasNext();)
            {
            String sId = (String) iter.next();
            if (sId.equals(MACRO_NONMT))
                {
                xml.getElement(TAG_HIDDEN).setString(String.valueOf(fValue));
                }
            }
        return xml;
        }

    /**
    * Replace string macros in the template with the associated values from
    * the source object (source).
    *
    * @param sTemplate  the template contain the macros
    * @param source     the MBean source for the replacement values
    *
    * @return a string based on the Template with JMX values included
    */
    protected String replaceMacros(String sTemplate, Object source)
        {
        String sRet      = sTemplate;
        Set    setMacros = Reporter.getMacros(sTemplate);

        for (Iterator i = setMacros.iterator(); i.hasNext();)
            {
            String           sId        = (String) i.next();
            ReportColumnView columnView = (ReportColumnView) m_mapColumns.get(sId);
            Object           oValue     = columnView.getOutputString(source);
            if (columnView != null && oValue != null)
                {
                sRet = sRet.replaceAll(MACRO_START + sId + MACRO_STOP,
                            oValue.toString());
                }
            }
        return sRet;
        }

    /**
    * Determine if the string contains any macros, without sanity checking the macro string.
    *
    * @param sTemplate  String containing column reference identifiers.
    *                   these identifiers must be with in curly braces {}
    *
    * @return the set of strings contained with {}
    */
    public static Set getMacros(String sTemplate)
        {
        Set             listRet = new HashSet();
        StringTokenizer st      = new StringTokenizer(sTemplate, "{}", true);
        while (st.hasMoreTokens())
            {
            String sToken = st.nextToken();
            if (sToken.equals("{"))
                {
                String sAttrib = st.nextToken();
                listRet.add(sAttrib);
                sToken = st.nextToken();
                if (!sToken.equals("}"))
                    {
                    // Error - un-matched {}
                    Logger.err("Error processing string \"" + sTemplate + "\". Unmatched brace {.");
                    return null;
                    }
                }
            }
        return listRet;
        }

    /**
    * Parse a string and get the defaults from the included macros.
    *
    * @param sTemplate  a string containing {macros defaults}
    *
    * @return a set keyed by the macro name with the value of the default
    */
    public static Set getMacroDefaults(String sTemplate)
        {
        Map          mapResults = new HashMap();
        int          ofStart    = sTemplate.indexOf('{');
        int          ofEnd      = -1;

        while (ofStart >= 0)
            {
            ofEnd = sTemplate.indexOf('}', ofStart);
            if (ofEnd < 0)
                {
                Logger.err("Invalid attribute format: " + sTemplate);
                break;
                }

            String sAttrName  = sTemplate.substring(ofStart + 1, ofEnd); // "name value"
            String sDefault   = null;
            int    ofDefault  = sAttrName.indexOf(' ');
            if (ofDefault > 0)
                {
                sDefault   = sAttrName.substring(ofDefault + 1).trim();
                sAttrName = sAttrName.substring(0, ofDefault);
                }
            mapResults.put(sAttrName, sDefault);
            ofStart    = sTemplate.indexOf('{', ofEnd);
            }
        return mapResults.entrySet();
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
    * Configure the Reporter.
    *
    * @param xmlReportCfg  the report configuration XML
    */
    public void setConfig(XmlElement xmlReportCfg)
        {
        if (m_xml == null)
            {
            m_xml = xmlReportCfg;

            // Configuration Declarations
            XmlElement xmlRow = xmlReportCfg.getElement(TAG_ROW);
            m_sDescTemplate   = xmlReportCfg.getSafeElement(Reporter.TAG_DESC).getString();
            m_fHeaders        = !xmlReportCfg.getSafeElement(Reporter.TAG_HEADERS).getBoolean(false);
            m_listXmlCol      = xmlRow.getElementList();
            m_sFileTempl      = xmlReportCfg.getSafeElement(TAG_FILENAME).getString();
            }
        }

    /**
    * Replace the Initialization "macros" with the initialization parameters.
    *
    * @param xml        the XML containing the macros
    * @param xmlParams  the initialization parameter
    */
    public static void replaceInitParams(XmlElement xml, XmlElement xmlParams)
        {
        String sTemplate = xml.getString();
        if (sTemplate != null && sTemplate.length() > 0)
            {
            String sRet = sTemplate;
            Set setMacros = Reporter.getMacroDefaults(sTemplate);

            // set the element's value from the specified system property
            for (Iterator i = setMacros.iterator(); i.hasNext();)
                {
                Map.Entry entry = (Map.Entry) i.next();
                String sMacro = (String) entry.getKey();
                Object oDefault = entry.getValue();
                String sDefault = (oDefault == null) ? null: oDefault.toString();
                String sValue = (xmlParams == null) ? null :
                                 xmlParams.getSafeElement(sMacro).getString();
                String sId    = (sDefault == null) ? sMacro : sMacro + " " + sDefault;
                if (sValue != null && sValue.length() > 0)
                    {
                    sRet = sRet.replaceAll(MACRO_START + sId + MACRO_STOP, sValue);
                    }
                else
                    {
                    if (sDefault != null)
                        {
                        sRet = sRet.replaceAll(MACRO_START + sId + MACRO_STOP, sDefault);
                        }
                    }
                }
            if (!sRet.equals(sTemplate))
                {
                xml.setString(sRet);
                }
            }

        // iterate for each contained element
        for (Iterator iter = xml.getElementList().iterator(); iter.hasNext();)
            {
            replaceInitParams((XmlElement) iter.next(), xmlParams);
            }
        }

    /**
    * Get the XML used to configure the reporter.
    *
    * @return an XmlElement containing the parsed results of the configuration
    *         file
    */
    public XmlElement getConfig()
        {
        return m_xml;
        }

    /**
    * Create/return the QueryHandler for the Report.
    *
    * @param xmlConfig  the report configuration
    * @param xmlQuery   the query to be executed
    * @param lBatch     the batch identifier for the query
    *
    * @return the QueryHandler for the Reporter
    */
    public QueryHandler ensureQueryHandler(XmlElement xmlConfig,  XmlElement xmlQuery, long lBatch)
        {
        QueryHandler qh = m_queryHandler;
        if (qh == null)
            {
            qh = new JMXQueryHandler();
            qh.setContext(xmlQuery, xmlConfig);
            m_queryHandler = qh;
            }

        ((JMXQueryHandler)qh).setBatch(lBatch);

        return qh;
        }


    //----- static mthods ---------------------------------------------------

    /**
    * Determine the key for the m_mapColumns given column XML.
    *
    * @param xmlColumn  the column definition XML
    *
    * @return  the id attribute of the column or the column configuration XML
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
    * Compare two String or Number objects.
    *
    * @param o1  first Object
    * @param o2  second Object
    *
    * @return 0 when o1 = o2,  -1 when o1 < o2, 1 when o1 > o2
    */
    public static int compare(Object o1, Object o2)
        {
        return o1 instanceof Comparable && o2 instanceof Comparable ?
            ((Comparable) o1).compareTo(o2) :
            String.valueOf(o1).compareTo(String.valueOf(o2));
        }


    //----- data members ----------------------------------------------------

    /**
    * A list of all Display Objects, including those not visible.
    */
    protected List m_listDisplay = new LinkedList();

    /**
    * DateFormat used to display Date columns.
    */
    protected DateFormat m_dateFormat;

    /**
    * A list of all XML Column Definition elements to convert into columnViews for the report.
    */
    protected List m_listXmlCol;

    /**
    * A Map from column ID to columnView for the Reporter.
    */
    protected Map m_mapColumns = new HashMap();

    /**
    * Used as a Description Template for the report description.
    */
    protected String m_sDescTemplate;

    /**
    * The batch id for the report execution.
    */
    public long m_lBatch;

    /**
    * The configuration XML.
    */
    protected XmlElement m_xml;

    /**
    * The column delimiter.
    */
    protected char m_cDelim;

    /**
    * The headers flag - true to print headers, false to hide headers.
    */
    protected boolean m_fHeaders;

    /**
    * The filename template.
    */
    protected String m_sFileTempl;

    /**
    * The Query Handler for the report.
    */
    protected QueryHandler m_queryHandler;

    /**
    * true if the report has run once before
    */
    protected boolean m_fInitialized;


    //----- Constants --------------------------------------------------------

    /**
    * A static Map between the filter XML definition and the implementation class
    * name.
    */
    public static Map m_mapColumnClass = new HashMap();

    static
        {
        String sBase = "com.tangosol.coherence.reporter.";
        m_mapColumnClass.put(VALUE_ATTRIB, sBase + "AttributeColumn");
        m_mapColumnClass.put(VALUE_SUBQUERY, sBase + "SubQueryColumn");
        m_mapColumnClass.put(VALUE_CONSTANT, sBase + "ConstantColumn");
        m_mapColumnClass.put(VALUE_PROPERTY, sBase + "PropertyColumn");
        m_mapColumnClass.put(VALUE_GLOBAL + "," + VALUE_BATCH, sBase + "BatchColumn");
        m_mapColumnClass.put(VALUE_GLOBAL + "," + VALUE_TIME, sBase + "DateTimeColumn");
        m_mapColumnClass.put(VALUE_KEY, sBase + "KeyColumn");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_SUM, sBase + "SumColumn");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_COUNT, sBase + "CountColumn");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_AVG, sBase + "AverageColumn");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_MIN, sBase + "MinColumn");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_MAX, sBase + "MaxColumn");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_DELTA, sBase + "DeltaColumn");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_DIVIDE, sBase + "DivideSource");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_ADD, sBase + "AddSource");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_SUB, sBase + "SubtractSource");
        m_mapColumnClass.put(VALUE_FUNC + "," + VALUE_MULTI, sBase + "MultiplySource");
        }
    }
