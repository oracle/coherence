
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.ManagementMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

/**
 * ManagementMBean contains statistics and settings associated with the grid
 * JMX infrastructure.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ManagementMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
    {
    // ---- Fields declarations ----
    
    /**
     * Property ExpiryDelay
     *
     * The number of milliseconds that the MBeanServer will keep a remote model
     * snapshot before refreshing.
     */
    private transient long __m_ExpiryDelay;
    
    /**
     * Property RefreshPolicy
     *
     * The policy used to determine the behavior when refreshing remote models.
     * Valid values are: refresh-ahead, refresh-behind, refresh-expired,
     * refresh-onquery. Invalid values will convert to `refresh-expired`.
     */
    private transient String __m_RefreshPolicy;
    
    // Default constructor
    public ManagementMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ManagementMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.ManagementMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/ManagementMBean".replace('/', '.'));
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
            "ManagementMBean contains statistics and settings associated with the grid JMX infrastructure.",
            null,
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
        
        // property ExpiryDelay
            {
            mapInfo.put("ExpiryDelay", new Object[]
                {
                "The number of milliseconds that the MBeanServer will keep a remote model snapshot before refreshing.",
                "getExpiryDelay",
                "setExpiryDelay",
                "J",
                null,
                });
            }
        
        // property RefreshCount
            {
            mapInfo.put("RefreshCount", new Object[]
                {
                "The total number of snapshots retrieved since the statistics were last reset.",
                "getRefreshCount",
                null,
                "J",
                null,
                });
            }
        
        // property RefreshExcessCount
            {
            mapInfo.put("RefreshExcessCount", new Object[]
                {
                "The number of times the MBean server predictively refreshed information and the information was not accessed.",
                "getRefreshExcessCount",
                null,
                "J",
                null,
                });
            }
        
        // property RefreshOnQuery
            {
            mapInfo.put("RefreshOnQuery", new Object[]
                {
                "Specifies whether or not the refresh-on-query MBeanServer is configured.  If this is true then the RefreshPolicy value should be `refresh-onquery`.",
                "isRefreshOnQuery",
                null,
                "Z",
                null,
                });
            }
        
        // property RefreshPolicy
            {
            mapInfo.put("RefreshPolicy", new Object[]
                {
                "The policy used to determine the behavior when refreshing remote models. Valid values are: refresh-ahead, refresh-behind, refresh-expired, refresh-onquery. Invalid values will convert to `refresh-expired`.",
                "getRefreshPolicy",
                "setRefreshPolicy",
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property RefreshPredictionCount
            {
            mapInfo.put("RefreshPredictionCount", new Object[]
                {
                "The number of times the MBeanServer used a predictive (refresh-behind, refresh-ahead, refresh-onquery) algorithm to refresh MBean information.",
                "getRefreshPredictionCount",
                null,
                "J",
                null,
                });
            }
        
        // property RefreshTimeoutCount
            {
            mapInfo.put("RefreshTimeoutCount", new Object[]
                {
                "The number of times this management node has timed out while attempting to refresh remote MBean attributes.",
                "getRefreshTimeoutCount",
                null,
                "J",
                null,
                });
            }
        
        // property RemoteNotificationCount
            {
            mapInfo.put("RemoteNotificationCount", new Object[]
                {
                "The total number of remote notifications received for all MBeans by this node since the last time the statistis were reset.",
                "getRemoteNotificationCount",
                null,
                "J",
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
                "Reset the SyncCount, MissCount and AsyncCount Statistics.",
                "resetStatistics",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        return mapInfo;
        }
    
    // Accessor for the property "ExpiryDelay"
    /**
     * Getter for property ExpiryDelay.<p>
    * The number of milliseconds that the MBeanServer will keep a remote model
    * snapshot before refreshing.
     */
    public long getExpiryDelay()
        {
        return __m_ExpiryDelay;
        }
    
    // Accessor for the property "RefreshCount"
    /**
     * Getter for property RefreshCount.<p>
    * The total number of snapshots retrieved since the statistics were last
    * reset.
     */
    public long getRefreshCount()
        {
        return 0L;
        }
    
    // Accessor for the property "RefreshExcessCount"
    /**
     * Getter for property RefreshExcessCount.<p>
    * The number of times the MBean server predictively refreshed information
    * and the information was not accessed.
     */
    public long getRefreshExcessCount()
        {
        return 0L;
        }
    
    // Accessor for the property "RefreshPolicy"
    /**
     * Getter for property RefreshPolicy.<p>
    * The policy used to determine the behavior when refreshing remote models.
    * Valid values are: refresh-ahead, refresh-behind, refresh-expired,
    * refresh-onquery. Invalid values will convert to `refresh-expired`.
     */
    public String getRefreshPolicy()
        {
        return __m_RefreshPolicy;
        }
    
    // Accessor for the property "RefreshPredictionCount"
    /**
     * Getter for property RefreshPredictionCount.<p>
    * The number of times the MBeanServer used a predictive (refresh-behind,
    * refresh-ahead, refresh-onquery) algorithm to refresh MBean information.
     */
    public long getRefreshPredictionCount()
        {
        return 0L;
        }
    
    // Accessor for the property "RefreshTimeoutCount"
    /**
     * Getter for property RefreshTimeoutCount.<p>
    * The number of times this management node has timed out while attempting
    * to refresh remote MBean attributes.
     */
    public long getRefreshTimeoutCount()
        {
        return 0L;
        }
    
    // Accessor for the property "RemoteNotificationCount"
    /**
     * Getter for property RemoteNotificationCount.<p>
    * The total number of remote notifications received for all MBeans by this
    * node since the last time the statistis were reset.
     */
    public long getRemoteNotificationCount()
        {
        return 0L;
        }
    
    // Accessor for the property "RefreshOnQuery"
    /**
     * Getter for property RefreshOnQuery.<p>
    * Specifies whether or not the refresh-on-query MBeanServer is configured. 
    * If this is true then the RefreshPolicy value should be `refresh-onquery`.
     */
    public boolean isRefreshOnQuery()
        {
        return false;
        }
    
    /**
     * Reset the SyncCount, MissCount and AsyncCount Statistics.
     */
    public void resetStatistics()
        {
        }
    
    // Accessor for the property "ExpiryDelay"
    /**
     * Setter for property ExpiryDelay.<p>
    * The number of milliseconds that the MBeanServer will keep a remote model
    * snapshot before refreshing.
     */
    public void setExpiryDelay(long nRefreshRate)
        {
        __m_ExpiryDelay = nRefreshRate;
        }
    
    // Accessor for the property "RefreshPolicy"
    /**
     * Setter for property RefreshPolicy.<p>
    * The policy used to determine the behavior when refreshing remote models.
    * Valid values are: refresh-ahead, refresh-behind, refresh-expired,
    * refresh-onquery. Invalid values will convert to `refresh-expired`.
     */
    public void setRefreshPolicy(String RefreshPolicy)
        {
        __m_RefreshPolicy = RefreshPolicy;
        }
    }
