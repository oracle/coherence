/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import com.sun.tools.visualvm.modules.coherence.Localization;
import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import com.sun.tools.visualvm.modules.coherence.helper.HttpRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.JMXRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;
import java.io.Serializable;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

/**
 * An abstract representation of data to be shown on tables and graphs.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public abstract class AbstractData
        implements Data, DataRetriever, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a new instance and initializes the required values.
     */
    public AbstractData(int nColumnCount)
        {
        this.nColumnCount = nColumnCount;

        oColumnValues     = new Object[nColumnCount];
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public SortedMap<Object, Data> getReporterData(TabularData reportData, VisualVMModel model)
        {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        Set<?>                  setKeys = reportData.keySet();
        Data                    data    = null;

        preProcessReporterData(model);

        if (setKeys.size() > 0)
            {
            // loop through each key, which are the rows of data
            for (Object oKey : setKeys)
                {
                // get the columns as an array
                Object[] aoColumns = ((Collection<Object>) oKey).toArray();

                data = processReporterData(aoColumns, model);

                // save the newly created entry if it exists
                if (data != null)
                    {
                    mapData.put(data.getColumn(0), data);
                    }
                }
            }

        return postProcessReporterData(mapData, model);
        }

    /**
     * Perform any pre-processing before reporter is called.
     *
     * @param  model   the {@link VisualVMModel} to use
     */
    protected void preProcessReporterData(VisualVMModel model)
        {
        }

    /**
     * {@inheritDoc}
     */
    public String preProcessReporterXML(VisualVMModel model, String sReporterXML)
        {
        // default is to leave as is
        return sReporterXML;
        }

    /**
     * Perform any post-processing of the generated data in case some manipulation
     * cannot be carried out by the reporter. This method should be overridden
     * in the specific implementation of AbstractData.
     *
     * @param  mapData generated {@link SortedMap} of the data from the reporter
     * @param  model   the {@link VisualVMModel} to use
     *
     * @return modified data
     */
    protected SortedMap<Object, Data> postProcessReporterData(SortedMap<Object, Data> mapData, VisualVMModel model)
        {
        // default is to return the data as is
        return mapData;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataUsingReport(VisualVMModel model,
                                                                RequestSender requestSender,
                                                                String        sReportXML) throws Exception
        {
        SortedMap<Object, Data> mapCollectedData = null;

        JMXRequestSender jmxRequestSender = (JMXRequestSender) requestSender;
        // carry out any parameter substitution or pre-processing of reporter XML
        sReportXML = preProcessReporterXML(model, new StringBuffer(sReportXML).toString());


        if (m_sReporterLocation == null)
            {
            // reporter location has not been defined, so lets find it
            m_sReporterLocation = jmxRequestSender.getReporterObjectName(jmxRequestSender.getLocalMemberId());
            }

        if (m_sReporterLocation != null)
            {
            try
                {
                // run the given report
                TabularData reportData =
                        (TabularData) jmxRequestSender.invoke(new ObjectName(m_sReporterLocation),
                                "runTabularReport", new Object[]{sReportXML}, new String[]{"java.lang.String"});

                if (reportData != null)
                    {
                    // now that we have output from the reporter, call the
                    // appropriate method in the class to populate
                    mapCollectedData = getReporterData(reportData, model);
                    }
                }
            catch (Exception e)
                {
                String sError =
                        Localization.getLocalText("ERR_error_running_report", new String[]{sReportXML, this.getClass().getCanonicalName(), e.getMessage()});

                LOGGER.log(Level.WARNING, sError);
                e.printStackTrace();

                model.setReporterAvailable(false);

                // this exception is thrown so we can catch above and re-run the report
                // using the standard way
                throw new RuntimeException("Error running report");
                }
            }
        return mapCollectedData;
        }

    // ----- Data methods ---------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public Object getColumn(int nColumn)
        {
        if (nColumn > nColumnCount - 1)
            {
            throw new IllegalArgumentException("Invalid column index " + nColumn);
            }

        return oColumnValues[nColumn];
        }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int nColumn, Object oValue)
        {
        if (nColumn > nColumnCount - 1)
            {
            throw new IllegalArgumentException("Invalid column index nColumn=" + nColumn + " , nColumnCount="
                                               + nColumnCount + ", class=" + this.getClass().getName() + "\n"
                                               + this.toString());
            }

        oColumnValues[nColumn] = oValue;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Turn a String number value that may have decimal points to one without.
     *
     * @param sValue  the String value which may actually have decimal placed
     *                or even be in exponential notation.
     *
     * @return the stripped String value
     */
    public static String getNumberValue(String sValue)
        {
        if (sValue != null)
            {
            String s = String.format("%d", new BigDecimal(sValue).longValue());

            return s;
            }

        return null;
        }

    /**
     * Return the column count.
     *
     * @return the column count
     */
    public int getColumnCount()
        {
        return nColumnCount;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        StringBuffer sb = new StringBuffer("AbstractData: Class = " + this.getClass().getName());

        for (int i = 0; i < nColumnCount; i++)
            {
            sb.append(", Column ").append(i).append("=").append(oColumnValues[i] == null
                      ? "null" : oColumnValues[i].toString());
            }

        return sb.toString();
        }

    /**
     * Return the full service name with a domain partition prefix.
     *
     * @param sDomainPartition  domain partition for the service
     * @param sServiceName      name of the service
     *
     * @return full service name with a domain partition prefix
     */
    public static String getFullServiceName(String sDomainPartition, String sServiceName)
        {
        return sDomainPartition + SERVICE_SEP + sServiceName;
        }

    /**
     * Return the parts for a service name split by "/"
     *
     * @param sServiceName  name of the service
     *
     * @return a String[] of the parts. Only one element if no domain partition
     */
    public static String[] getServiceParts(String sServiceName)
        {
        return sServiceName.split(SERVICE_SEP);
        }

    /**
     * Return the domainPartition and service name if the raw
     * service contains it.
     *
     * @param sRawServiceName  the raw service name
     *
     * @return a String array with the domainPartition and service
     */
    public static String[] getDomainAndService(String sRawServiceName)
        {
        String[] asParts          = getServiceParts(sRawServiceName);
        String   sDomainPartition = asParts.length == 1 ? null : asParts[0];
        String   sServiceName     = sDomainPartition == null ? sRawServiceName : asParts[1];

        return new String[] {sDomainPartition, sServiceName};
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 1803170898872716122L;

    /**
     * Separator for the domain partition and service name.
     */
    public static final String SERVICE_SEP = "/";

    // ----- data members ---------------------------------------------------

    /**
     * The array of objects (statistics) for this instance.
     */
    protected Object[] oColumnValues = null;

    /**
     * The column count.
     */
    protected int nColumnCount;


    /**
     * The report object location to use to run reports.
     */
    private static String m_sReporterLocation = null;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(AbstractData.class.getName());
    }
