
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.ReporterModel

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import java.util.Date;
import java.util.Map;

/**
 * Model components implement the JMX-managed functionality of the
 * corresponding MBeans without being dependent on any JMX classes and could be
 * used both in-process and out-of-process (relative to an MBeanServer).
 * 
 * The LocalModel components operate in two distinct modes: live and snapshot.
 * In the live mode all model methods call corresponding methods on managed
 * objects. The snapshot mode uses the _SnapshotMap to keep the attribute
 * values.
 * 
 * Every time a remote invocation is used by the RemoteModel to do a
 * setAttribute or invoke call, the snapshot model is refreshed.
 */
/*
* Integrates
*     com.tangosol.coherence.reporter.ReportControl
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ReporterModel
        extends    com.tangosol.coherence.component.net.management.model.LocalModel
    {
    // ---- Fields declarations ----
    
    /**
     * Property _ReportControl
     *
     */
    private com.tangosol.coherence.reporter.ReportControl __m__ReportControl;
    
    /**
     * Property AutoStart
     *
     */
    private transient boolean __m_AutoStart;
    
    /**
     * Property ConfigFile
     *
     */
    private transient String __m_ConfigFile;
    
    /**
     * Property CurrentBatch
     *
     */
    private transient long __m_CurrentBatch;
    
    /**
     * Property IntervalSeconds
     *
     */
    private transient long __m_IntervalSeconds;
    
    /**
     * Property LastReport
     *
     */
    private transient String __m_LastReport;
    
    /**
     * Property OutputPath
     *
     */
    private transient String __m_OutputPath;
    
    /**
     * Property Reports
     *
     */
    private transient String[] __m_Reports;
    
    /**
     * Property State
     *
     */
    private transient String __m_State;
    
    // Default constructor
    public ReporterModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ReporterModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        
        // state initialization: public and protected properties
        try
            {
            set_SnapshotMap(new java.util.HashMap());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
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
        return new com.tangosol.coherence.component.net.management.model.localModel.ReporterModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/ReporterModel".replace('/', '.'));
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
    
    //++ com.tangosol.coherence.reporter.ReportControl integration
    // Access optimization
    // properties integration
    // methods integration
    /**
     * Getter for property ConfigFile.<p>
     */
    public String getConfigFile()
        {
        return get_ReportControl().getConfigFile();
        }
    /**
     * Getter for property CurrentBatch.<p>
     */
    public long getCurrentBatch()
        {
        return get_ReportControl().getCurrentBatch();
        }
    /**
     * Getter for property IntervalSeconds.<p>
     */
    public long getIntervalSeconds()
        {
        return get_ReportControl().getIntervalSeconds();
        }
    /**
     * Getter for property LastExecuteTime.<p>
     */
    public java.util.Date getLastExecuteTime()
        {
        return get_ReportControl().getLastExecuteTime();
        }
    /**
     * Getter for property LastReport.<p>
     */
    public String getLastReport()
        {
        return get_ReportControl().getLastReport();
        }
    /**
     * Getter for property OutputPath.<p>
     */
    public String getOutputPath()
        {
        return get_ReportControl().getOutputPath();
        }
    /**
     * Getter for property Reports.<p>
     */
    public String[] getReports()
        {
        return get_ReportControl().getReports();
        }
    /**
     * Getter for property RunAverageMillis.<p>
    * The average runtime in milliseconds since the last statistics reset.
     */
    public double getRunAverageMillis()
        {
        return get_ReportControl().getRunAverageMillis();
        }
    /**
     * Getter for property RunLastMillis.<p>
    * The last runtime in milliseconds
     */
    public long getRunLastMillis()
        {
        return get_ReportControl().getRunLastMillis();
        }
    /**
     * Getter for property RunMaxMillis.<p>
    * The maximum runtime in milliseconds since the last statistics reset.
     */
    public long getRunMaxMillis()
        {
        return get_ReportControl().getRunMaxMillis();
        }
    /**
     * Getter for property State.<p>
     */
    public String getState()
        {
        return get_ReportControl().getState();
        }
    /**
     * Getter for property AutoStart.<p>
     */
    public boolean isAutoStart()
        {
        return get_ReportControl().isAutoStart();
        }
    /**
     * Getter for property Centralized.<p>
     */
    public boolean isCentralized()
        {
        return get_ReportControl().isCentralized();
        }
    /**
     * Getter for property Running.<p>
     */
    public boolean isRunning()
        {
        return get_ReportControl().isRunning();
        }
    public void resetStatistics()
        {
        get_ReportControl().resetStatistics();
        }
    public void runReport(String sReportFile)
        {
        get_ReportControl().runReport(sReportFile);
        }
    public javax.management.openmbean.TabularData runTabularGroupReport(String sReportName, java.util.Map mapXmlReports)
        {
        return get_ReportControl().runTabularGroupReport(sReportName, mapXmlReports);
        }
    public javax.management.openmbean.TabularData runTabularReport(String sReportFile)
        {
        return get_ReportControl().runTabularReport(sReportFile);
        }
    /**
     * Setter for property ConfigFile.<p>
     */
    public void setConfigFile(String sFile)
        {
        get_ReportControl().setConfigFile(sFile);
        }
    /**
     * Setter for property CurrentBatch.<p>
     */
    public void setCurrentBatch(long cBatch)
        {
        get_ReportControl().setCurrentBatch(cBatch);
        }
    /**
     * Setter for property IntervalSeconds.<p>
     */
    public void setIntervalSeconds(long cSeconds)
        {
        get_ReportControl().setIntervalSeconds(cSeconds);
        }
    /**
     * Setter for property LastReport.<p>
     */
    public void setLastReport(String pLastReport)
        {
        get_ReportControl().setLastReport(pLastReport);
        }
    /**
     * Setter for property OutputPath.<p>
     */
    public void setOutputPath(String sPath)
        {
        get_ReportControl().setOutputPath(sPath);
        }
    /**
     * Setter for property Reports.<p>
     */
    public void setReports(String[] pReports)
        {
        get_ReportControl().setReports(pReports);
        }
    /**
     * Setter for property State.<p>
     */
    public void setState(String pState)
        {
        get_ReportControl().setState(pState);
        }
    public void start()
        {
        get_ReportControl().start();
        }
    public void stop()
        {
        get_ReportControl().stop();
        }
    //-- com.tangosol.coherence.reporter.ReportControl integration
    
    // Accessor for the property "_ReportControl"
    /**
     * Getter for property _ReportControl.<p>
     */
    public com.tangosol.coherence.reporter.ReportControl get_ReportControl()
        {
        return __m__ReportControl;
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Date;
        // import java.util.Map;
        
        super.readExternal(in);
        
        Map mapSnapshot = get_SnapshotMap();
        
        mapSnapshot.put("AutoStart", Boolean.valueOf(in.readBoolean()));
        mapSnapshot.put("ConfigFile", ExternalizableHelper.readUTF(in));
        mapSnapshot.put("CurrentBatch", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("IntervalSeconds", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("LastExecuteTime", new Date(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("LastReport", ExternalizableHelper.readUTF(in));
        mapSnapshot.put("OutputPath", ExternalizableHelper.readUTF(in));
        mapSnapshot.put("Reports", ExternalizableHelper.readStringArray(in));
        mapSnapshot.put("RunAverageMillis", Double.valueOf(in.readDouble()));
        mapSnapshot.put("RunLastMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("RunMaxMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("Running", Boolean.valueOf(in.readBoolean()));
        mapSnapshot.put("State", ExternalizableHelper.readUTF(in));
        }
    
    // Accessor for the property "_ReportControl"
    /**
     * Setter for property _ReportControl.<p>
     */
    public void set_ReportControl(com.tangosol.coherence.reporter.ReportControl reportControl)
        {
        __m__ReportControl = reportControl;
        }
    
    // Accessor for the property "AutoStart"
    /**
     * Setter for property AutoStart.<p>
     */
    public void setAutoStart(boolean fAutoStart)
        {
        __m_AutoStart = fAutoStart;
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        super.writeExternal(out);
        
        out.writeBoolean(isAutoStart());
        ExternalizableHelper.writeUTF(out, getConfigFile());
        ExternalizableHelper.writeLong(out, getCurrentBatch());
        ExternalizableHelper.writeLong(out, getIntervalSeconds());
        ExternalizableHelper.writeLong(out, getLastExecuteTime().getTime());
        ExternalizableHelper.writeUTF(out, getLastReport());
        ExternalizableHelper.writeUTF(out, getOutputPath());
        ExternalizableHelper.writeStringArray(out, getReports());
        out.writeDouble(getRunAverageMillis());
        ExternalizableHelper.writeLong(out, getRunLastMillis());
        ExternalizableHelper.writeLong(out, getRunMaxMillis());
        out.writeBoolean(isRunning());
        ExternalizableHelper.writeUTF(out, getState());
        }
    }
