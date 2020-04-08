/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.reporter;


import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.TaskDaemon;

import java.util.Date;
import java.util.Map;

import javax.management.openmbean.TabularData;


/**
* A custom MBean to manage the reporter execution.
*
* The following properties may be used for configuration of the monitoring node:
*
* <table>
*   <tr>
*     <td valign="top"><tt>coherence.management.report.group</tt></td>
*     <td valign="top">Specifies the report group XML file to execute.  The
*     report group file must conform to coherence-report-group.xsd
*     </td>
*   </tr>
*   <tr>
*     <td valign="top"><tt>coherence.management.report.autostart</tt></td>
*     <td valign="top">Specifies if the service is to auto start. Valid values:
*     "true" and "false"</td>
*   </tr>
*   <tr>
*     <td valign="top"><tt>coherence.management.report.centralized</tt></td>
*     <td valign="top">If true the Reporter MBean is registered as a singleton.
*      If false, the Reporter MBean is registered with a global nodeId. Valid values:
*     "true" and "false"</td>
*   </tr>
* </table>
*
* @author ew 2008.02.28
* @since Coherence 3.4
*/
public interface ReportControl
    {
    // ----- ReportControl methods -------------------------------------------

    /**
     * Specify the ReportControl instance with specified Dependencies.
     *
     * @param dps   The reporter dependencies to utilize
     */
    public void setDependencies(ReportBatch.Dependencies dps);

    /**
     * Getter for reporter property Dependencies.
     *
     * @return dependencies object
     */
    public ReportBatch.Dependencies getDependencies();

    /**
    * Obtain the daemon for the Reporter task.
    *
    * @return the daemon the Reporter is executing
    */

    public TaskDaemon getDaemon();

    /**
    * Set the daemon for the Reporter task.
    *
    * @param daemon the daemon the Reporter is executing
    */
    public void setDaemon(TaskDaemon daemon);

    /**
    * Check to see if the execution thread is running.
    *
    * @return the thread running the reporter
    */
    public boolean isRunning();

    /**
    * Set the last report executed.
    *
    * @param sLastReport the last Report Executed
    */
    public void setLastReport(String sLastReport);

    /**
    * Set the list of reports in the execution list.
    *
    * @param asReports the report execution list
    */
    public void setReports(String[] asReports);

    /**
    * Set the last time a report was executed.
    *
    * @param ldtExeTime the last time a reported executed as a long
    */
    public void setLastExecutionMillis(long ldtExeTime);

    /**
    * Set the last time a report was executed.
    *
    * @return the last time a reported executed as a long
    */
    public long getLastExecutionMillis();

    /**
    * Set the state of the reporter.
    *
    * @param sState the state of the reporter
    */
    public void setState(String sState);

    /**
    * Get the batch configuration XML that conforms to batch-config.xml.
    *
    * @return the batch configuration XML
    */
    public XmlDocument getXml();

    /**
    * Set the batch configuration XML that conforms to batch-config.xml.
    *
    */
    public void setXml(XmlDocument xml);

    /**
    * Get the bacth identifier for the Reporter.
    *
    * @return the current batch for execution
    */
    public long getCurrentBatch();

    /**
    * set the bacth identifier for the Reporter.
    *
    * @param nNewBatch the new batch identifier
    */
    public void setCurrentBatch(long nNewBatch);

    /**
    * Get the frequency of execution in Seconds.
    *
    * @return the frequency which the reporter executes the configured reports
    */
    public long getIntervalSeconds();

    /**
    * Get the output path for the data files.
    *
    * @return the output path for the data files.
    */
    public String getOutputPath();

    /**
    * Set the output path for the data files.
    *
    * @param sPath the new batch identifier
    */
    public void setOutputPath(String sPath);

    /**
    * Set the frequency of execution in Milliseconds.
    *
    * @param nInterval  the frequency which the reporter executes the configured reports
    */
    public void setIntervalSeconds(long nInterval);

    /**
    * Get the batch configuration file name.   The file corresponds to
    * coherence-report-config.xsd.
    *
    * @return the frequency which the reporter executes the configured reports
    */
    public String getConfigFile();

    /**
    * Get the state of the reporter. Valid values are : Started, Stopped, Stopping,
    *  Running and Waiting.
    *
    * @return the state of the reporter
    */
    public String getState();

    /**
    * Determine if the Reporter auto starts.
    *
    * @return true if autostart
    */
    public boolean isAutoStart();

    /**
    * Stop the reporter.  The reporter will continue to execute the current
    * batch of reports prior to termination.
    */
    public void stop();

    /**
    * Start the reporter.  If the reporter is already running, this method does
    * not start another one.
    */
    public void start();

    /**
    * Get a list of reports executing by the batch reporter.
    *
    * @return a string array of the reports in the execution batch
    */
    public String[] getReports();

    /**
    * Get the array of XML elements for the initialization parameters.
    */
    public XmlElement[] getParams();

    /**
    * Set the Parameter XML.
    */
    public void setParams(XmlElement[] aXml);

    /**
    * Set the batch configuration file name.  When setting this value on a remote
    * server, the remote server must have access to the path and file.
    * The file must conform to coherence-report-config.xsd.
    *
    * @param sInputFilename The path and file name of the batch configuration
    */
    public void setConfigFile(String sInputFilename);

    /**
    * Execute a single report on time.
    *
    * @param sReport  a report configuration path or XML content
    */
    public void runReport(String sReport);

    /**
    * Execute the specified report file or report XML defined in the {@code sReport}
    * argument. If the report XML file or XML content defines a single report, the returned
    * TabularData will have a CompositeData for each row of values from the report.
    * It will also include a rowId attribute for indexing.
    * For example:
    * <pre>
    * TabularData(sReportFile) =
    *             TabularData[ CompositeData { attribute1 -> value1,
    *                                          attribute2 -> value2,
    *                                          rowId      -> 1},
    *                          CompositeData { attribute1 -> value1,
    *                                          attribute2 -> value2,
    *                                          rowId      -> 2} ]
    * </pre>
    * If the specified file defines a report group, the returned
    * TabularData will have a single CompositeData with report-names as keys
    * and TabularDatas of the reports as values. For example:
    * <pre>
    * TabularData[ CompositeData { report1 -> TabularData(report1),
    *                              report2 -> TabularData(report2) } ]
    * </pre>
    * <p>
    * If the XML content in the {@code sReport} defines a report group, then it
    * must contain the URI of individual reports in the group. It cannot have XML
    * content for the individual reports.
    *
    * @param sReport  a report or report-group configuration path and filename
    *                 or a String containing the report XML
    *
    * @return a tabularData with the above specified representation.
    */
    public TabularData runTabularReport(String sReport);

    /**
     * Execute the specified group report. The group's member report names and
     * their xml content are passed in the map.
     * <p>
     * The returned TabularData will have a single CompositeData with report-name as keys
     * and TabularDatas of the reports as values. For example:
     * <pre>
     * TabularData[ CompositeData { report1 -> TabularData(report1),
     *                              report2 -> TabularData(report2) } ]
     * </pre>
     *
     * @param sReportName   report-group name
     * @param mapXmlReports Map of Individual report names and their XML content.
     *
     * @return a tabularData with the above specified representation.
     */
    public TabularData runTabularGroupReport(String sReportName, Map<String, String>mapXmlReports);

    /**
    * Get the last report file executed.
    *
    * @return a string array of the reports in the execution batch
    */
    public String getLastReport();

    /**
    * Get the date and time the reporter executed the batch.
    *
    * @return the date and time the reporter last executed
    */
    public Date getLastExecuteTime();

    /**
    * Get the last execution duration in milliseconds since the last statistics
    * reset.
    *
    * @return the date and time the reporter last executed
    */
    public long getRunLastMillis();

    /**
    * Get the Maximum execution duration in milliseconds since the last
    * statistics reset.
    *
    * @return the date and time the reporter last executed
    */
    public long getRunMaxMillis();

    /**
    * Get the average execution duration in milliseconds since the last
    * statistics reset.
    *
    * @return the date and time the reporter last executed
    */
    public double getRunAverageMillis();

    /**
    * Reset the execution statistics.
    */
    public void resetStatistics();

    /**
    * Determine if the Reporter is running in a centralized or Distributed mode.
    */
    public boolean isCentralized();

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
    * The tag in the xml which contains the frequency time.
    */
    public static final String TAG_FREQ = "frequency";

    /**
    * The tag in the xml which contains the frequency time.
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
    }
