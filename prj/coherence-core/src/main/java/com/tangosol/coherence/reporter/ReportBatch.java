/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.reporter;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.TaskDaemon;

import java.io.File;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.concurrent.atomic.AtomicReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.oracle.coherence.common.base.Blocking;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;


/**
* Management class to continually run the reporting process.
*
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class ReportBatch
        extends Base
        implements Runnable, ReportControl
    {
    //----- constructor ----------------------------------------------------

    /**
    * Default constructor.
    */
    public ReportBatch()
        {
        }

    /**
    * Continually execute a group of reports from the configuration file.
    * <pre>
    * Example:
    *   java com.tangosol.coherence.reporter.ReportBatch config-file
    * </pre>
    * Note: to run a group of reports programmaticly, one could to do the following:
    * <code>
    *   new ReportBatch().start();
    * </code>
    */
    public static void main(String[] asArg)
        {
        if (asArg.length == 0)
            {
            showUsage();
            return;
            }

        String sFile = asArg[0];

        System.setProperty("coherence.management.report.configuration", sFile);
        System.setProperty("coherence.management.report.autostart", "true");
        System.setProperty("coherence.management.report.distributed", "false");

        while (true)
            {
            CacheFactory.ensureCluster();
            try
                {
                Blocking.sleep(5000);
                }
            catch (InterruptedException e)
                {
                Thread.interrupted();
                break;
                }
            }
        }

    //----- ReportBatch methods ----------------------------------------------

    /**
    * Run the report batch.
    */
    public void run()
        {
        long          ldtStart = System.currentTimeMillis();
        ReportBatch   model     = this;
        Map           map       = m_mapReporters;
        Reporter      reporter;

        // refresh settings if they have changed
        model.setCurrentBatch(model.getCurrentBatch() + 1);

        long         nBatchId = model.getCurrentBatch();
        String[]     aReport  = model.getReports();
        XmlElement[] aParam   = model.getParams();
        String       sPath    = m_sOutputDir;
        long         cReports = aReport.length;

        if (model.setState(/*sExpectState*/ STATE_WAITING, /*sNewState*/ STATE_RUNNING) ||
                model.setState(/*sExpectState*/ STATE_STARTED, /*sNewState*/ STATE_RUNNING))
            {
            model.setLastExecutionMillis(System.currentTimeMillis());
            for (int i = 0; i < cReports; i++)
                {
                Integer nKey = Integer.valueOf(i);
                String sDefFile = aReport[i];
                XmlElement xmlParam = aParam[i];

                reporter = (Reporter) map.get(nKey);
                if (reporter == null)
                    {
                    reporter = new Reporter();
                    map.put(nKey, reporter);
                    }

                reporter.setDateFormat(m_dateFormat);
                model.setLastReport(sDefFile);

                reporter.run(sDefFile, sPath, nBatchId, xmlParam,
                        ReportBatch.class.getClassLoader());
                }

            updateStats(ldtStart);
            model.setState(/*sExpectState*/ STATE_RUNNING, /*sNewState*/ STATE_WAITING);
            }
        }

    /**
    * Output usage instructions.
    */
    public static void showUsage()
        {
        out();
        out("java com.tangosol.coherence.reporter.ReportBatch <config-file>");
        out();
        out("command option descriptions:");
        out("\t<config-file> the file containing the report configuration XML");
        out();
        }

    // ----- ReportControl methods -------------------------------------------

    /**
    * Obtain the daemon for the Reporter task.
    *
    * @return the daemon the Reporter is executing
    */
    public TaskDaemon getDaemon()
        {
        return m_daemon;
        }

    /**
    * Set the daemon for the Reporter task.
    *
    * @param daemon the daemon the Reporter is executing
    */
    public void setDaemon(TaskDaemon daemon)
        {
        m_daemon = daemon;
        }

    /**
    * Check to see if the execution thread is running.
    *
    * @return the thread running the reporter
    */
    public boolean isRunning()
        {
        return m_fRun;
        }

    /**
    * Set the last report executed.
    *
    * @param sLastReport the last Report Executed
    */
    public void setLastReport(String sLastReport)
        {
        m_sLastReport = sLastReport;
        }

    /**
    * Set the list of reports in the execution list.
    *
    * @param asReports the report execution list
    */
    public void setReports(String[] asReports)
        {
        m_asReports = asReports;
        }

    /**
    * Set the last time a report was executed.
    *
    * @param ldtTime the last time a reported executed as a long
    */
    public void setLastExecutionMillis(long ldtTime)
        {
        m_ldtLastExecutionMillis = ldtTime;
        }

    /**
    * Set the last time a report was executed.
    *
    * @return the last time a reported executed as a long
    */
    public long getLastExecutionMillis()
        {
        return m_ldtLastExecutionMillis;
        }

    /**
    * Set the state of the reporter.
    *
    * @param sState the state of the reporter
    */
    public void setState(String sState)
        {
        m_refState.set(sState);
        }

    /**
    * Compare and Set the state of the reporter.
    *
    * @param sExpectState the expected state of the reporter
    * @param sNewState    the new state to set
    *
    * @return true if the state is set successfully
    */
    public boolean setState(String sExpectState, String sNewState)
        {
        return m_refState.compareAndSet(sExpectState, sNewState);
        }

    /**
    * Get the batch configuration XML that conforms to batch-config.xml.
    *
    * @return the batch configuration XML
    */
    public XmlDocument getXml()
        {
        return m_xml;
        }

    /**
    * Set the batch configuration XML that conforms to batch-config.xml.
    *
    * @param xml the XML configuration for the Reporter
    */
    public void setXml(XmlDocument xml)
        {
        m_xml = xml;
        }

    /**
    * Convert the batch configuration XML to an array for the MBean.
    *
    * @param xmlReports the batch configuration report list
    *
    * @return the array of report configuration file names
    */
    private String[] makeReportArray(XmlElement xmlReports)
        {
        List         listReports = xmlReports.getElementList();
        String[]     asReports   = new String[listReports.size()];
        XmlElement[] axmlParam   = new XmlElement[listReports.size()];
        int          c           = 0;
        for (Iterator i = listReports.iterator(); i.hasNext();)
            {
            XmlElement o = (XmlElement)i.next();
            asReports[c] = o.getSafeElement(TAG_LOCATION).getString();
            axmlParam[c] = o.getElement(TAG_PARAMS);
            c++;
            }
        m_aParams = axmlParam;
        return asReports;
        }

    //----- ReportControl Interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public long getCurrentBatch()
        {
        return m_nCurrentBatch;
        }

    /**
    * {@inheritDoc}
    */
    public void setCurrentBatch(long nNewBatch)
        {
        m_nCurrentBatch = nNewBatch;
        }

    /**
    * {@inheritDoc}
    */
    public long getIntervalSeconds()
        {
        return m_nInterval / 1000;
        }

    /**
    * {@inheritDoc}
    */
    public String getOutputPath()
        {
        return m_sOutputDir == null ? "" : new File(m_sOutputDir).getAbsolutePath();
        }

    /**
    * {@inheritDoc}
    */
    public void setOutputPath(String sPath)
        {
        m_sOutputDir = sPath;
        }

    /**
    * {@inheritDoc}
    */
    public void setIntervalSeconds(long nInterval)
        {
        m_nInterval = nInterval * 1000;
        }

    /**
    * {@inheritDoc}
    */
    public String getConfigFile()
        {
        return m_sConfigFile;
        }

    /**
    * {@inheritDoc}
    */
    public String getState()
        {
        return m_refState.get();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isAutoStart()
        {
        return getDependencies().isAutoStart();
        }

    /**
    * {@inheritDoc}
    */
    public void stop()
        {
        MBeanHelper.checkReadOnly("stop");

        if (!getState().equals(STATE_ERROR))
            {
            synchronized (this)
                {
                if (isRunning())
                    {
                    m_fRun = false;
                    setState(STATE_STOPPING);
                    getDaemon().stop();
                    setState(STATE_STOPPED);
                    setDaemon(null);
                    Base.log("Management Reporting - Stopped");
                    }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void start()
        {
        MBeanHelper.checkReadOnly("start");

        if (getState().equals(STATE_ERROR))
            {
            Base.log("Management Reporting - "
                    + "An unrecoverable error has occurred. Reporter not started.");
            }
        else
            {
            synchronized (this)
                {
                TaskDaemon daemon = getDaemon();
                if (m_daemon == null && m_sConfigFile != null)
                    {
                    ReportBatch oReport = this;
                    daemon = new TaskDaemon("Reporter");
                    daemon.schedulePeriodicTask(oReport, System.currentTimeMillis()
                            + getIntervalSeconds() * 1000, getIntervalSeconds() * 1000);
                    Base.log("Management Reporting -  Started");
                    daemon.start();

                    setState(STATE_STARTED);
                    m_fRun = true;
                    }

                setDaemon(daemon);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public String[] getReports()
        {
        return m_asReports;
        }

    /**
    * {@inheritDoc}
    */
    public XmlElement[] getParams()
        {
        return m_aParams;
        }

    /**
     * Set the array of XML elements for the initialization parameters.
     *
     * @param aXml array of xml elements
     */
    public void setParams(XmlElement[] aXml)
        {
        m_aParams = aXml;
        }

    /**
    * {@inheritDoc}
    */
    public void setConfigFile(String sInputFilename)
        {
        try
            {
            synchronized (this)
                {
                m_mapReporters = new HashMap();
                m_sConfigFile  = sInputFilename;
                XmlDocument xml = XmlHelper.loadFileOrResource(
                    sInputFilename, "Reporter configuration",
                    ReportBatch.class.getClassLoader());
                XmlHelper.replaceSystemProperties(xml, "system-property");

                setXml(xml);
                setOutputPath(xml.getSafeElement(TAG_DIR).getString(""));
                setIntervalSeconds(Base.parseTime(xml.getSafeElement(TAG_FREQ).getString(DEFAULT_FREQ))/1000);
                m_asReports = makeReportArray(xml.getSafeElement(TAG_LIST));
                }
            }
        catch (Exception e) // FileNotFoundException
            {
            setState(STATE_ERROR);
            Base.log("Failed to start Reporter " + e);
            m_asReports = new String[0];
            }
        }

    /**
    * {@inheritDoc}
    */
    public void runReport(String sReportFile)
        {
        if (!getState().equals(STATE_ERROR))
            {
            new Reporter().run(sReportFile, m_sOutputDir, m_nCurrentBatch, null,
                    ReportBatch.class.getClassLoader());
            }
        }

    /**
    * {@inheritDoc}
    */
    public TabularData runTabularReport(String sReportFile)
        {
        boolean fURI = Reporter.isURI(sReportFile);

        // Reporters for URI based reports are cached.
        TabularReportRunner runner = fURI ? f_mapReporter.get(sReportFile)
                                          : new TabularReportRunner(sReportFile, fURI);
        if (runner == null)
            {
            runner = new TabularReportRunner(sReportFile, fURI);
            f_mapReporter.put(sReportFile, runner);
            }
        return runner.runTabularReport();
        }

     /**
     * {@inheritDoc}
     */
    public TabularData runTabularGroupReport(String sReportName, Map<String, String> mapXmlReports)
        {
        TabularReportRunner runner = new TabularReportRunner(sReportName, mapXmlReports);
        return runner.runTabularReport();
        }

    /**
    * {@inheritDoc}
    */
    public String getLastReport()
        {
        return m_sLastReport;
        }

    /**
    * {@inheritDoc}
    */
    public Date getLastExecuteTime()
        {
        return new Date(getLastExecutionMillis());
        }

    /**
    * {@inheritDoc}
    */
    public long getRunLastMillis()
        {
        return m_lastRuntimeMillis;
        }

    /**
    * {@inheritDoc}
    */
    public long getRunMaxMillis()
        {
        return this.m_maxRuntimeMillis;
        }

    /**
    * {@inheritDoc}
    */
    public double getRunAverageMillis()
        {
        return (m_cExecutionCount == 0) ? 0.0
               : this.m_totalRuntimeMillis / m_cExecutionCount;
        }

    /**
    * {@inheritDoc}
    */
    public void resetStatistics()
        {
        m_lastRuntimeMillis  = 0;
        m_maxRuntimeMillis   = 0;
        m_cExecutionCount    = 0;
        m_totalRuntimeMillis = 0;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isCentralized()
        {
        return getDependencies().isDistributed();
        }


    // ----- helper methods --------------------------------------------------

    protected void updateStats(long ldtStart)
        {
        long lRuntime = System.currentTimeMillis() - ldtStart;
        m_cExecutionCount ++;
        m_maxRuntimeMillis   = (m_maxRuntimeMillis < lRuntime)
                             ? lRuntime
                             : m_maxRuntimeMillis;
        m_totalRuntimeMillis += lRuntime;
        m_lastRuntimeMillis  =  lRuntime;
        }

    /**
    * {@inheritDoc}
    */
    public void setDependencies(Dependencies dps)
        {
        if (getDependencies() == null)
            {
            m_dependencies = dps = new DefaultDependencies(dps).validate();
            }
        else
            {
            throw new IllegalStateException("Reporter dependencies cannot be reset");
            }

        setConfigFile(dps.getConfigFile());

        String sTimezone        = dps.getTimeZone();
        String sTimeStampFormat = dps.getDateFormat();

        m_dateFormat = new SimpleDateFormat(sTimeStampFormat);
        if (!sTimezone.isEmpty())
            {
            m_dateFormat.setTimeZone(getTimeZone(sTimezone));
            }
        }

    /**
    * {@inheritDoc}
    */
    public Dependencies getDependencies()
        {
        return m_dependencies;
        }

    // ----- inner interface --------------------------------------------------

    /**
     *  The interface used to provide reporter with its external dependencies.
     */
    public interface Dependencies
        {
        /**
         * Return the report configuration that contain the location for
         * the Reporter batch.
         *
         * @return the report configuration file
         */
        String getConfigFile();

        /**
         * Return the report switch for reporter.
         *
         * @return true to enable reporter
         */
        boolean isAutoStart();

        /**
         * Return the distributed flag that specifies whether or not to run
         * reporter on multiple management node.
         *
         * @return true to enable distributed reporter
         */
        boolean isDistributed();

        /**
         * Return the time zone for the generated reports.
         *
         * @return time zone
         */
        String getTimeZone();

        /**
         * Return the time stamp format for reporter.
         *
         * @return time output format
         */
        String getDateFormat();
        }


    // ----- inner classes --------------------------------------------------

    /**
     * Default {@link Dependencies} implementation.
     */
    public static class DefaultDependencies
            implements Dependencies
        {
        /**
         * Construct a DefaultReportDependencies object. Uses default value for each dependency.
         */
        public DefaultDependencies()
            {
            this(null);
            }

        /**
         * Construct a DefaultReportDependencies object copying the values
         * from the specified ReporterDependencies object.
         *
         * @param deps  the dependencies to copy
         */
        public DefaultDependencies(Dependencies deps)
            {
            if (deps != null)
                {
                m_sConfigFile = deps.getConfigFile();
                m_autoStart   = deps.isAutoStart();
                m_distributed = deps.isDistributed();
                m_sTimezone   = deps.getTimeZone();
                m_sDateFormat = deps.getDateFormat();
                }
            }

        /**
         * {@inheritDoc}
         */
        public String getConfigFile()
            {
            return m_sConfigFile;
            }

        /**
         * Set the report configuration file.
         *
         * @param sConfFile  the report configuration file
         *
         * @return this object
         */
        public DefaultDependencies setConfigFile(String sConfFile)
            {
            m_sConfigFile = sConfFile;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        public boolean isAutoStart()
            {
            return m_autoStart;
            }

        /**
         * Set the reporter switch.
         *
         * @param fAutoStart  the reporter switch, true to enable reporting.
         *
         * @return this object
         */
        public DefaultDependencies setAutoStart(boolean fAutoStart)
            {
            m_autoStart = fAutoStart;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        public boolean isDistributed()
            {
            return m_distributed;
            }

        /**
         * Set the distributed flag.
         *
         * @param fDistributed  specify whether the reporter should run on multiple nodes.
         *
         * @return this object
         */
        public DefaultDependencies setDistributed(boolean fDistributed)
            {
            m_distributed = fDistributed;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        public String getTimeZone()
            {
            return m_sTimezone;
            }

        /**
         * Set the time zone.
         *
         * @param sTimeZone  time zone for the reports.
         *
         * @return this object
         */
        public DefaultDependencies setTimeZone(String sTimeZone)
            {
            m_sTimezone = sTimeZone;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        public String getDateFormat()
            {
            return m_sDateFormat;
            }

        /**
         * Set the time format.
         *
         * @param sTimeFormat  time stamp format for the reports.
         *
         * @return this object
         */
        public DefaultDependencies setDateFormat(String sTimeFormat)
            {
            m_sDateFormat = sTimeFormat;
            return this;
            }

        /**
         * Validate the report configuration.
         *
         * @throws IllegalArgumentException if the configuration file are not valid
         *
         * @return this object
         */
        public DefaultDependencies validate()
            {
            Base.checkNotNull(m_sConfigFile, "configuration");
            return this;
            }

        // ----- data members of DefaultDependencies --------------------------------

        /**
         * The report configuration file.
         */
        protected String m_sConfigFile = "reports/report-group.xml";

        /**
         * The reporter switch, true to enable reporting.
         */
        protected boolean m_autoStart = false;

        /**
         * The distributed flag that specifies whether or not to run reporter
         * on multiple management node..
         */
        protected boolean m_distributed = false;

        /**
         * The time zone for reports.
         */
        protected String m_sTimezone = "";

        /**
         * The time stamp format for reports.
         */
        protected String m_sDateFormat = "EEE MMM dd HH:mm:ss zzz yyyy";
        }

    // ----- inner classes --------------------------------------------------

    /**
     * TablularReportRunner runs the report and returns the data in a tabular data format.
     */
    public class TabularReportRunner
        {
        /**
         * Construct a {@code TabularReportRunner} using the specified parameters.
         *
         * @param sReportOrGroup  the URI or contents of either a report group or individual
         *                        report file.
         * @param fURI            flag indicating if the report file is a URI or
         *                        XML content.
         */
        public TabularReportRunner(String sReportOrGroup, boolean fURI)
            {
            f_fURI                  = fURI;
            XmlDocument xmlDocument = fURI
                    ? XmlHelper.loadFileOrResource(sReportOrGroup, "Reporter configuration",
                           ReportBatch.class.getClassLoader())
                    : XmlHelper.loadXml(sReportOrGroup);

            // could be a report group or a single report
            f_fReportGrp = xmlDocument.getName().equals("report-group");

            if (f_fReportGrp)
                {
                f_sReportGroup  = sReportOrGroup;
                f_sReport       = null;
                List xmlReports = xmlDocument.getSafeElement(TAG_LIST).getElementList();
                f_mapReports    = new LinkedHashMap<String, String>(xmlReports.size());
                for (Iterator iter = xmlReports.iterator(); iter.hasNext();)
                    {
                    // Individual reports can only be URI. Hence the value is null.
                    f_mapReports.put(((XmlElement) iter.next()).getSafeElement(TAG_LOCATION).getString(), null);
                    }
                }
            else
                {
                f_sReport      = sReportOrGroup;
                f_sReportGroup = null;
                }
            }

        /**
         * Construct a {@code TabularReportRunner} using the specified parameters.
         *
         * @param sReportGroup   the URI of the report group.
         * @param mapXmlReports  map of Individual report names and their XML content.
         */
        public TabularReportRunner(String sReportGroup, Map<String, String> mapXmlReports)
            {
            f_sReportGroup = sReportGroup;
            f_fURI         = false;
            f_fReportGrp   = true;
            f_sReport      = null;
            // Individual reports can only be XML content.
            f_mapReports   = new LinkedHashMap<String, String>(mapXmlReports);
            }

        /**
         * Run the report.
         *
         * @return the data in TabularData format
         */
        public TabularData runTabularReport()
            {
            if (!getState().equals(STATE_ERROR))
                {
                if (f_fReportGrp)
                    {
                    int                     cReports      = f_mapReports.size();
                    OpenType[]              aOpenTypes    = new OpenType[cReports];
                    String[]                asReportDesc  = new String[cReports];
                    String[]                asReportNames = new String[cReports];
                    Map<String,TabularData> mapTabulars   = new HashMap<String,TabularData>();
                    int i = 0;
                    for (Map.Entry<String, String> entry : f_mapReports.entrySet())
                        {
                        String sReport  = entry.getKey();
                        String sContent = entry.getValue();

                        // If the individual reports are URIs, then entry's value will be null and the key is the URI
                        // otherwise entry's value is the report's xml content.
                        int         index        = sReport.lastIndexOf('/');
                        String      sTabularType = index < 0 ? sReport : sReport.substring(index);
                        TabularData tabData      = runSingleReport(sContent == null ? sReport : sContent, sTabularType);

                        asReportNames[i] = sReport;
                        mapTabulars.put(sReport, tabData);
                        if (tabData == null)
                            {
                            aOpenTypes[i]   = SimpleType.STRING;
                            asReportDesc[i] = sTabularType;
                            }
                        else
                            {
                            TabularType tabType = tabData.getTabularType();
                            aOpenTypes[i]       = tabType;
                            asReportDesc[i]     = tabType.getDescription();
                            }
                        i++;
                        }

                    try
                        {
                        String             sReport        = f_sReportGroup;
                        CompositeType      rowType        = new CompositeType(sReport, sReport, asReportNames, asReportDesc, aOpenTypes);
                        TabularType        tabType        = new TabularType(sReport, sReport, rowType, asReportNames);
                        TabularDataSupport tabDataSupport = new TabularDataSupport(tabType);
                        tabDataSupport.put(new CompositeDataSupport(rowType, mapTabulars));
                        return tabDataSupport;
                        }
                    catch (OpenDataException e)
                        {
                        throw Base.ensureRuntimeException(e);
                        }
                    }
                else
                    {
                    return runSingleReport(f_sReport, f_fURI ? f_sReport : DEFAULT_TABULAR_TYPE_NAME);
                    }
                }
            return null;
            }

        /**
         * Run an individual report.
         *
         * @param sReport       the URI or the content of the report
         * @param sTabularType  the typeName of the {@code TabularType}
         *
         * @return report data in TabularData format
         */
        protected TabularData runSingleReport(String sReport, String sTabularType)
            {
            // reporter can handle both URI and xml content.
            Reporter reporter = getReporter(sReport);
            return reporter.run(sReport, m_sOutputDir, sTabularType, m_nCurrentBatch, null,
                ReportBatch.class.getClassLoader(), false, true);
            }

        /**
         * Get the Reporter.
         *
         * @param sReportFile  Report file
         *
         * @return Reporter associated with the given report file.
         */
        protected Reporter getReporter(String sReportFile)
            {
            Reporter reporter = f_fURI ? f_mapReporter.get(sReportFile) : new Reporter();
            if (reporter == null)
                {
                reporter = new Reporter();
                f_mapReporter.put(sReportFile, reporter);
                }
            return reporter;
            }

        // ----- data members ------------------------------------------------

        /**
         * Report URI or content. Reporter MBeans can be invoked by passing the report
         * content(for example when used in JVisualVM) or the report URI.
         */
        protected final String f_sReport;

        /**
         * Report Group URI or content. Reporter MBeans can be invoked by passing the report
         * group content(for example when used in JVisualVM) or the report group URI.
         */
        protected final String f_sReportGroup;

        /**
         * Flag indicating if the report name is a URI.
         */
        protected final boolean f_fURI;

        /**
         * Flag indicating if the report is a group report.
         */
        protected final boolean f_fReportGrp;

        /**
         * Map of Individual report names and their XML content.
         */
        protected Map<String, String> f_mapReports;

        /**
         * Map of Individual reports and their associated Reporter.
         */
        protected final Map<String, Reporter> f_mapReporter = new HashMap<String, Reporter>();
        }

    // ----- data members ----------------------------------------------------

    /**
    * The state of the execution thread.
    */
    private AtomicReference<String> m_refState = new AtomicReference<>(STATE_STOPPED);

    /**
    * The file name of the last report executed.
    */
    private String m_sLastReport;

    /**
    * The output path of the data files.
    */
    private String m_sOutputDir;

    /**
    * The batch configuration filename.
    */
    private String m_sConfigFile;

    /**
    * The current execution batch.
    */
    private long m_nCurrentBatch;

    /**
    * Flag to determine if the process should be running (false stops execution
    * thread.
    */
    private boolean m_fRun;

    /**
    * Array of report configuration file names in the batch.
    */
    private String[] m_asReports;

    /**
    * Number of milliseconds to wait between batch executions.
    */
    private long m_nInterval;

    /**
    * The batch configuration XML.
    */
    private XmlDocument m_xml;

    /**
    * The report execution daemon.
    */
    protected TaskDaemon m_daemon;

    /**
    * The start date and time of the last batch execution.
    */
    private long m_ldtLastExecutionMillis;

    /**
    * The parameters for each report.
    */
    private XmlElement[] m_aParams;

    /**
    * The map of Reporter instances to running in the batch.
    */
    protected Map m_mapReporters = new HashMap();

    /**
    * The last batch execution time in milliseconds.
    */
    protected long m_lastRuntimeMillis;

    /**
    * The maximum runtime in milliseconds.
    */
    protected long m_maxRuntimeMillis;

    /**
    * The total number of executions.
    */
    protected long m_cExecutionCount;

    /**
    * The total runtime in milliseconds.
    */
    protected long m_totalRuntimeMillis;

    /**
     * Report dependencies.
     */
    private Dependencies m_dependencies;

    /**
     * Date format.
     */
    protected DateFormat m_dateFormat;

    /**
     * Map of Reports and their associated TablularReportRunner. Reporters for URI
     * based reports are cached.
     */
    protected Map<String, TabularReportRunner> f_mapReporter = new HashMap<String, TabularReportRunner>();

    // ----- Constants ------------------------------------------------------

    /**
    * The execution thread has been started.
    */
    public static final String STATE_STARTED = "Started";

    /**
    * The controlling thread is attempting to stop the execution thread.
    */
    public static final String STATE_STOPPING = "Stopping";

    /**
    * The execution thread is stopped.
    */
    public static final String STATE_STOPPED = "Stopped";

    /**
    * The execution thread is waiting for the frequency time before running.
    */
    public static final String STATE_WAITING = "Sleeping";

    /**
    * The execution thread is running a report.
    */
    public static final String STATE_RUNNING = "Running";

    /**
    * The Reporter Batch has received an Error and can not continue.
    */
    public static final String STATE_ERROR = "Error";

    /**
    * The frequency which the report batch will execute.
    */
    public static final String TAG_FREQ = "frequency";

   /**
    * The tag in the xml which contains the location of the report configuration.
    */
    public static final String TAG_LOCATION = "location";

   /**
    * The tag in the xml which contains the report configuration pararameters.
    */
    public static final String TAG_PARAMS = "init-params";

   /**
    * The tag in the xml which contains the output path.
    */
    public static final String TAG_DIR = "output-directory";

    /**
    * The tag in the xml which contains the report-list.
    */
    public static final String TAG_LIST = "report-list";

    /**
    * The value of the default frequency if frequency is not specified.
    */
    public static final String DEFAULT_FREQ = "60s";

    /**
     * The constants to be used as the typeName of the {@code TabularType} in non URL based
     * reports i.e. where report XML's are passed as the content of the Reporter invocation.
     */
    public static final String DEFAULT_TABULAR_TYPE_NAME = "coherence-report.xml";
    }
