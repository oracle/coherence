
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.ReporterMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

/**
 * ReporterMBean contains settings and statistics for the Coherence JMX
 * Reporter.  
 * 
 * @descriptor com.bea.owner=Context,com.bea.VisibleToPartitions=ALWAYS
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ReporterMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
    {
    // ---- Fields declarations ----
    
    /**
     * Property ConfigFile
     *
     * The configuration file for the Reporter.
     */
    private transient String __m_ConfigFile;
    
    /**
     * Property CurrentBatch
     *
     * The batch identifier for the Reporter.
     */
    private transient long __m_CurrentBatch;
    
    /**
     * Property IntervalSeconds
     *
     * The interval between executions in seconds.
     */
    private transient long __m_IntervalSeconds;
    
    /**
     * Property OutputPath
     *
     * The path where report output will be located.
     */
    private transient String __m_OutputPath;
    
    // Default constructor
    public ReporterMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ReporterMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        
        if (fInit)
            {
            __init();
            }
        }
    
    // Main initializer
    public void __init()
        {
        // private initialization
        __initPrivate();
        
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.manageable.modelAdapter.ReporterMBean();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/ReporterMBean".replace('/', '.'));
            }
        catch (ClassNotFoundException e)
            {
            throw new NoClassDefFoundError(e.getMessage());
            }
        return clz;
        }
    
    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }
    /**
     * Auto-generated for concrete Components.
     */
    protected Object[] get_ComponentInfo()
        {
        return new Object[]
            {
            "ReporterMBean contains settings and statistics for the Coherence JMX Reporter.",
            "com.bea.owner=Context,com.bea.VisibleToPartitions=ALWAYS",
            };
        }
    /**
     * Auto-generated for concrete Components, for Properties that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - at least one public accessor
     */
    protected java.util.Map get_PropertyInfo()
        {
        java.util.Map mapInfo = super.get_PropertyInfo();
        
        // property AutoStart
            {
            mapInfo.put("AutoStart", new Object[]
                {
                "True when the Reporter starts automatically with the node.",
                "isAutoStart",
                null,
                "Z",
                null,
                });
            }
        
        // property ConfigFile
            {
            mapInfo.put("ConfigFile", new Object[]
                {
                "The configuration file for the Reporter.",
                "getConfigFile",
                "setConfigFile",
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property CurrentBatch
            {
            mapInfo.put("CurrentBatch", new Object[]
                {
                "The batch identifier for the Reporter.",
                "getCurrentBatch",
                "setCurrentBatch",
                "J",
                null,
                });
            }
        
        // property IntervalSeconds
            {
            mapInfo.put("IntervalSeconds", new Object[]
                {
                "The interval between executions in seconds.",
                "getIntervalSeconds",
                "setIntervalSeconds",
                "J",
                null,
                });
            }
        
        // property LastExecuteTime
            {
            mapInfo.put("LastExecuteTime", new Object[]
                {
                "The last time a report batch was executed.",
                "getLastExecuteTime",
                null,
                "Ljava/util/Date;",
                null,
                });
            }
        
        // property LastReport
            {
            mapInfo.put("LastReport", new Object[]
                {
                "The last report to execute.",
                "getLastReport",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property OutputPath
            {
            mapInfo.put("OutputPath", new Object[]
                {
                "The path where report output will be located.",
                "getOutputPath",
                "setOutputPath",
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property Reports
            {
            mapInfo.put("Reports", new Object[]
                {
                "The list of reports executed.",
                "getReports",
                null,
                "[Ljava/lang/String;",
                null,
                });
            }
        
        // property RunAverageMillis
            {
            mapInfo.put("RunAverageMillis", new Object[]
                {
                "The average batch runtime in milliseconds since the statistics were last reset.",
                "getRunAverageMillis",
                null,
                "D",
                null,
                });
            }
        
        // property RunLastMillis
            {
            mapInfo.put("RunLastMillis", new Object[]
                {
                "The last batch runtime in milliseconds since the statistics were last reset.",
                "getRunLastMillis",
                null,
                "J",
                null,
                });
            }
        
        // property RunMaxMillis
            {
            mapInfo.put("RunMaxMillis", new Object[]
                {
                "The maximum batch runtime in milliseconds since the statistics were last reset.",
                "getRunMaxMillis",
                null,
                "J",
                null,
                });
            }
        
        // property State
            {
            mapInfo.put("State", new Object[]
                {
                "The state of the Reporter. Valid values are:\n\nRunning (reports are being executed);\nWaiting (the reporter is waiting for the interval to complete);\nStarting (the reporter is being started);\nStopping (the reporter is attempting to stop execution and waiting for running reports to complete);\nStopped (the reporter is stopped).",
                "getState",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        return mapInfo;
        }
    /**
     * Auto-generated for concrete Components, for Behaviors that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - public access
     */
    protected java.util.Map get_MethodInfo()
        {
        java.util.Map mapInfo = super.get_MethodInfo();
        
        // behavior resetStatistics()
            {
            mapInfo.put("resetStatistics()", new Object[]
                {
                "Reset the runtime performance statistics for the Reporter.",
                "resetStatistics",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior runReport(String sReportFile)
            {
            mapInfo.put("runReport(Ljava.lang.String;)", new Object[]
                {
                "Run the report configuration file one time.",
                "runReport",
                "V",
                new String[] {"sReportFile", },
                new String[] {"Ljava.lang.String;", },
                null,
                });
            }
        
        // behavior runTabularGroupReport(String sReportName, java.util.Map mapXmlReports)
            {
            mapInfo.put("runTabularGroupReport(Ljava.lang.String;Ljava.util.Map;)", new Object[]
                {
                "Execute the specified group report. The group's member report names and their xml content \nare passed in the map.\nThe returned TabularData will have a single CompositeData with report-name as keys \nand TabularDatas of the reports as values. \n\nFor example:\nTabularData[ CompositeData { report1 -> TabularData(report1), report2 -> TabularData(report2) } ]\n\n@param sReport  group report name\n\n@param mapXmlReports map of individual report names in the group report and their xml content\n\n@return a tabularData with the above specified representation.",
                "runTabularGroupReport",
                "Ljavax/management/openmbean/TabularData;",
                new String[] {"sReportName", "mapXmlReports", },
                new String[] {"Ljava.lang.String;", "Ljava.util.Map;", },
                null,
                });
            }
        
        // behavior runTabularReport(String sReportFile)
            {
            mapInfo.put("runTabularReport(Ljava.lang.String;)", new Object[]
                {
                "Execute the specified report file or report XML defined in the {@code sReport}\nargument. If the report XML file or XML content defines a single report, the returned\nTabularData will have a CompositeData for each row of values from the report.\nIt will also include a rowId attribute for indexing.\nFor example:\n<pre> \nTabularData(sReportFile) =\n            TabularData[ CompositeData { attribute1 -> value1,\n                                         attribute2 -> value2,\n                                         rowId      -> 1},\n                         CompositeData { attribute1 -> value1,\n                                         attribute2 -> value2,\n                                         rowId      -> 2} ]\n</pre>\nIf the specified file or XML content defines a report group, the returned\nTabularData will have a single CompositeData with report-names as keys\nand TabularDatas of the reports as values. For example:\n<pre> \nTabularData[ CompositeData { report1 -> TabularData(report1),\n                             report2 -> TabularData(report2) } ]\n</pre>\n\n@param sReport  a report or report-group configuration path and filename\n                    or a String containing the report XML\n\n@return a tabularData with the above specified representation.",
                "runTabularReport",
                "Ljavax/management/openmbean/TabularData;",
                new String[] {"sReportFile", },
                new String[] {"Ljava.lang.String;", },
                null,
                });
            }
        
        // behavior start()
            {
            mapInfo.put("start()", new Object[]
                {
                "",
                "start",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior stop()
            {
            mapInfo.put("stop()", new Object[]
                {
                "",
                "stop",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        return mapInfo;
        }
    
    // Accessor for the property "ConfigFile"
    /**
     * Getter for property ConfigFile.<p>
    * The configuration file for the Reporter.
     */
    public String getConfigFile()
        {
        return __m_ConfigFile;
        }
    
    // Accessor for the property "CurrentBatch"
    /**
     * Getter for property CurrentBatch.<p>
    * The batch identifier for the Reporter.
     */
    public long getCurrentBatch()
        {
        return __m_CurrentBatch;
        }
    
    // Accessor for the property "IntervalSeconds"
    /**
     * Getter for property IntervalSeconds.<p>
    * The interval between executions in seconds.
     */
    public long getIntervalSeconds()
        {
        return __m_IntervalSeconds;
        }
    
    // Accessor for the property "LastExecuteTime"
    /**
     * Getter for property LastExecuteTime.<p>
    * The last time a report batch was executed.
     */
    public java.util.Date getLastExecuteTime()
        {
        return null;
        }
    
    // Accessor for the property "LastReport"
    /**
     * Getter for property LastReport.<p>
    * The last report to execute.
     */
    public String getLastReport()
        {
        return null;
        }
    
    // Accessor for the property "OutputPath"
    /**
     * Getter for property OutputPath.<p>
    * The path where report output will be located.
     */
    public String getOutputPath()
        {
        return __m_OutputPath;
        }
    
    // Accessor for the property "Reports"
    /**
     * Getter for property Reports.<p>
    * The list of reports executed.
     */
    public String[] getReports()
        {
        return null;
        }
    
    // Accessor for the property "RunAverageMillis"
    /**
     * Getter for property RunAverageMillis.<p>
    * The average batch runtime in milliseconds since the statistics were last
    * reset.
     */
    public double getRunAverageMillis()
        {
        return 0.0;
        }
    
    // Accessor for the property "RunLastMillis"
    /**
     * Getter for property RunLastMillis.<p>
    * The last batch runtime in milliseconds since the statistics were last
    * reset.
     */
    public long getRunLastMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "RunMaxMillis"
    /**
     * Getter for property RunMaxMillis.<p>
    * The maximum batch runtime in milliseconds since the statistics were last
    * reset.
     */
    public long getRunMaxMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "State"
    /**
     * Getter for property State.<p>
    * The state of the Reporter. Valid values are:
    * 
    * Running (reports are being executed);
    * Waiting (the reporter is waiting for the interval to complete);
    * Starting (the reporter is being started);
    * Stopping (the reporter is attempting to stop execution and waiting for
    * running reports to complete);
    * Stopped (the reporter is stopped).
     */
    public String getState()
        {
        return null;
        }
    
    // Accessor for the property "AutoStart"
    /**
     * Getter for property AutoStart.<p>
    * True when the Reporter starts automatically with the node.
     */
    public boolean isAutoStart()
        {
        return false;
        }
    
    /**
     * Reset the runtime performance statistics for the Reporter.
     */
    public void resetStatistics()
        {
        }
    
    /**
     * Run the report configuration file one time.
     */
    public void runReport(String sReportFile)
        {
        }
    
    /**
     * Execute the specified group report. The group's member report names and
    * their xml content 
    * are passed in the map.
    * The returned TabularData will have a single CompositeData with
    * report-name as keys 
    * and TabularDatas of the reports as values. 
    * 
    * For example:
    * TabularData[ CompositeData { report1 -> TabularData(report1), report2 ->
    * TabularData(report2) } ]
    * 
    * @param sReport  group report name
    * 
    * @param mapXmlReports map of individual report names in the group report
    * and their xml content
    * 
    * @return a tabularData with the above specified representation.
     */
    public javax.management.openmbean.TabularData runTabularGroupReport(String sReportName, java.util.Map mapXmlReports)
        {
        return null;
        }
    
    /**
     * Execute the specified report file or report XML defined in the {@code
    * sReport}
    * argument. If the report XML file or XML content defines a single report,
    * the returned
    * TabularData will have a CompositeData for each row of values from the
    * report.
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
    * If the specified file or XML content defines a report group, the returned
    * TabularData will have a single CompositeData with report-names as keys
    * and TabularDatas of the reports as values. For example:
    * <pre> 
    * TabularData[ CompositeData { report1 -> TabularData(report1),
    *                              report2 -> TabularData(report2) } ]
    * </pre>
    * 
    * @param sReport  a report or report-group configuration path and filename
    *                     or a String containing the report XML
    * 
    * @return a tabularData with the above specified representation.
     */
    public javax.management.openmbean.TabularData runTabularReport(String sReportFile)
        {
        return null;
        }
    
    // Accessor for the property "ConfigFile"
    /**
     * Setter for property ConfigFile.<p>
    * The configuration file for the Reporter.
     */
    public void setConfigFile(String pConfigFile)
        {
        __m_ConfigFile = pConfigFile;
        }
    
    // Accessor for the property "CurrentBatch"
    /**
     * Setter for property CurrentBatch.<p>
    * The batch identifier for the Reporter.
     */
    public void setCurrentBatch(long pCurrentBatch)
        {
        __m_CurrentBatch = pCurrentBatch;
        }
    
    // Accessor for the property "IntervalSeconds"
    /**
     * Setter for property IntervalSeconds.<p>
    * The interval between executions in seconds.
     */
    public void setIntervalSeconds(long pIntervalSeconds)
        {
        __m_IntervalSeconds = pIntervalSeconds;
        }
    
    // Accessor for the property "OutputPath"
    /**
     * Setter for property OutputPath.<p>
    * The path where report output will be located.
     */
    public void setOutputPath(String pOutputPath)
        {
        __m_OutputPath = pOutputPath;
        }
    
    public void start()
        {
        }
    
    public void stop()
        {
        }
    }
