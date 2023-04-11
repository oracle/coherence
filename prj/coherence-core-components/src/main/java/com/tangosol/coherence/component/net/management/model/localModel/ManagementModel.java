
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.ManagementModel

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.coherence.component.net.management.Connector;

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
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ManagementModel
        extends    com.tangosol.coherence.component.net.management.model.LocalModel
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Connector
     *
     * Connector containing the stats for the Management Model.
     */
    private com.tangosol.coherence.component.net.management.Connector __m__Connector;
    
    /**
     * Property ExpiryDelay
     *
     * The number of milliseconds that the Management server will keep a model
     * snapshot.
     */
    private transient long __m_ExpiryDelay;
    
    /**
     * Property RefreshPolicy
     *
     * This is the policy used to determine the behavior when refreshing remote
     * models.
     * 
     * Valid Values : refresh-ahead, refresh-behind, refresh-expired,
     * refresh-onquery
     */
    private transient String __m_RefreshPolicy;
    
    // Default constructor
    public ManagementModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ManagementModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.management.model.localModel.ManagementModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/ManagementModel".replace('/', '.'));
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
    
    // Accessor for the property "_Connector"
    /**
     * Getter for property _Connector.<p>
    * Connector containing the stats for the Management Model.
     */
    public com.tangosol.coherence.component.net.management.Connector get_Connector()
        {
        return __m__Connector;
        }
    
    // Accessor for the property "ExpiryDelay"
    /**
     * Getter for property ExpiryDelay.<p>
    * The number of milliseconds that the Management server will keep a model
    * snapshot.
     */
    public long getExpiryDelay()
        {
        return get_Connector().getRefreshTimeoutMillis();
        }
    
    // Accessor for the property "RefreshCount"
    /**
     * Getter for property RefreshCount.<p>
    * The number of synchronous refresh calls by the MBean Server to remote
    * servers.
     */
    public long getRefreshCount()
        {
        return get_Connector().getStatsRefreshCount();
        }
    
    // Accessor for the property "RefreshExcessCount"
    /**
     * Getter for property RefreshExcessCount.<p>
    * The number of times the MBean server did an asynchronous refresh and the
    * information was not accessed.
     */
    public long getRefreshExcessCount()
        {
        return get_Connector().getStatsRefreshExcessCount();
        }
    
    // Accessor for the property "RefreshPolicy"
    /**
     * Getter for property RefreshPolicy.<p>
    * This is the policy used to determine the behavior when refreshing remote
    * models.
    * 
    * Valid Values : refresh-ahead, refresh-behind, refresh-expired,
    * refresh-onquery
     */
    public String getRefreshPolicy()
        {
        // import Component.Net.Management.Connector;
        
        Connector conn = get_Connector();
        return conn.formatRefreshPolicy(conn.getRefreshPolicy());
        }
    
    // Accessor for the property "RefreshPredictionCount"
    /**
     * Getter for property RefreshPredictionCount.<p>
    * The number of times the MBean server did an asynchronous refresh.
     */
    public long getRefreshPredictionCount()
        {
        return get_Connector().getStatsRefreshPredictionCount();
        }
    
    // Accessor for the property "RefreshTimeoutCount"
    /**
     * Getter for property RefreshTimeoutCount.<p>
    * The number of times the management node has timed out while refreshing
    * remote MBeans. 
     */
    public long getRefreshTimeoutCount()
        {
        return get_Connector().getStatsRefreshTimeoutCount();
        }
    
    // Accessor for the property "RemoteNotificationCount"
    /**
     * Getter for property RemoteNotificationCount.<p>
    * The number of notifications received by the node since the last time the
    * statistis were reset.
     */
    public long getRemoteNotificationCount()
        {
        return get_Connector().getStatsNotificationCount();
        }
    
    // Accessor for the property "RefreshOnQuery"
    /**
     * Getter for property RefreshOnQuery.<p>
    * True if the Custom MBean Server is configured.
     */
    public boolean isRefreshOnQuery()
        {
        return System.getProperty("javax.management.builder.initial", "none").indexOf("WrapperMBeanServerBuilder") != -1;
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        throw new IllegalStateException("ManagementModel is not global");
        }
    
    public void resetStatistics()
        {
        get_Connector().resetStatistics();
        }
    
    // Accessor for the property "_Connector"
    /**
     * Setter for property _Connector.<p>
    * Connector containing the stats for the Management Model.
     */
    public void set_Connector(com.tangosol.coherence.component.net.management.Connector p_Connector)
        {
        __m__Connector = p_Connector;
        }
    
    // Accessor for the property "ExpiryDelay"
    /**
     * Setter for property ExpiryDelay.<p>
    * The number of milliseconds that the Management server will keep a model
    * snapshot.
     */
    public void setExpiryDelay(long nRefreshRate)
        {
        checkReadOnly("setExpiryDelay");
        get_Connector().setRefreshTimeoutMillis(nRefreshRate);
        }
    
    // Accessor for the property "RefreshPolicy"
    /**
     * Setter for property RefreshPolicy.<p>
    * This is the policy used to determine the behavior when refreshing remote
    * models.
    * 
    * Valid Values : refresh-ahead, refresh-behind, refresh-expired,
    * refresh-onquery
     */
    public void setRefreshPolicy(String sRefreshPolicy)
        {
        // import Component.Net.Management.Connector;
        
        checkReadOnly("setRefreshPolicy");
        
        Connector conn = get_Connector();
        conn.setRefreshPolicy(sRefreshPolicy);
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        throw new IllegalStateException("ManagementModel is not global");
        }
    }
