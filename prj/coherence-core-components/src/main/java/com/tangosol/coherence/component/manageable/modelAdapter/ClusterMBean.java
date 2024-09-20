
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.ClusterMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

/**
 * The ClusterMBean represents a Cluster object.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ClusterMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
        implements javax.management.NotificationEmitter
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public ClusterMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ClusterMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            set_NotificationDescription("Member events");
            String[] a0 = new String[2];
                {
                a0[0] = "member.joined";
                a0[1] = "member.left";
                }
            set_NotificationType(a0);
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.ClusterMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/ClusterMBean".replace('/', '.'));
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
            "The ClusterMBean represents a Cluster object.",
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
        
        // property ClusterName
            {
            mapInfo.put("ClusterName", new Object[]
                {
                "The name of the cluster.",
                "getClusterName",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property ClusterSize
            {
            mapInfo.put("ClusterSize", new Object[]
                {
                "The total number of cluster nodes.",
                "getClusterSize",
                null,
                "I",
                "metrics.value=Size",
                });
            }
        
        // property LicenseMode
            {
            mapInfo.put("LicenseMode", new Object[]
                {
                "The license mode that this cluster is using. Possible values are Evaluation, Development or Production.",
                "getLicenseMode",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property LocalMemberId
            {
            mapInfo.put("LocalMemberId", new Object[]
                {
                "The member id for the cluster member that is co-located with the reporting MBeanServer; -1 if the cluster service is not running.",
                "getLocalMemberId",
                null,
                "I",
                null,
                });
            }
        
        // property MemberIds
            {
            mapInfo.put("MemberIds", new Object[]
                {
                "An array of all existing cluster member ids.",
                "getMemberIds",
                null,
                "[I",
                null,
                });
            }
        
        // property Members
            {
            mapInfo.put("Members", new Object[]
                {
                "An array of all existing cluster members.",
                "getMembers",
                null,
                "[Ljava/lang/String;",
                null,
                });
            }
        
        // property MembersDeparted
            {
            mapInfo.put("MembersDeparted", new Object[]
                {
                "An array of strings containing the Member information for recently departed cluster members.   Members will be removed from this array when the member id is recycled.   This information is since the node has joined the cluster and is reset when the MBeanServer node leaves and rejoins the cluster.\n\nThe MembersDepartureCount is the total count of departed members and not the size of this array.",
                "getMembersDeparted",
                null,
                "[Ljava/lang/String;",
                null,
                });
            }
        
        // property MembersDepartureCount
            {
            mapInfo.put("MembersDepartureCount", new Object[]
                {
                "The number of times this node has observed another node`s departure from the cluster since this management node has joined the cluster or statistics have been reset.",
                "getMembersDepartureCount",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property OldestMemberId
            {
            mapInfo.put("OldestMemberId", new Object[]
                {
                "The senior cluster member id; -1 if the cluster service is not running.",
                "getOldestMemberId",
                null,
                "I",
                "metrics.tag=senior_member_id",
                });
            }
        
        // property Running
            {
            mapInfo.put("Running", new Object[]
                {
                "Specifies whether or not the cluster is running.",
                "isRunning",
                null,
                "Z",
                null,
                });
            }
        
        // property Version
            {
            mapInfo.put("Version", new Object[]
                {
                "The Coherence version.",
                "getVersion",
                null,
                "Ljava/lang/String;",
                "metrics.tag=version",
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
        
        // behavior configureTracing(String sRole, float fRatio)
            {
            mapInfo.put("configureTracing(Ljava.lang.String;F)", new Object[]
                {
                "Configure the tracing sampling ratio for those members matching the specified role, or if role is null or empty,\nconfigure the tracing ratio for all cluster members.",
                "configureTracing",
                "V",
                new String[] {"sRole", "fRatio", },
                new String[] {"Ljava.lang.String;", "F", },
                null,
                });
            }
        
        // behavior dumpClusterHeap(String sRole)
            {
            mapInfo.put("dumpClusterHeap(Ljava.lang.String;)", new Object[]
                {
                "Dump heap across the cluster.",
                "dumpClusterHeap",
                "V",
                new String[] {"sRole", },
                new String[] {"Ljava.lang.String;", },
                null,
                });
            }
        
        // behavior ensureRunning()
            {
            mapInfo.put("ensureRunning()", new Object[]
                {
                "Ensures that the cluster service is running on this node.",
                "ensureRunning",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior flightRecording(String sRole, String sCmd, String sOptions)
            {
            mapInfo.put("flightRecording(Ljava.lang.String;Ljava.lang.String;Ljava.lang.String;)", new Object[]
                {
                "Perform a Java flight recorder operation on all eligible cluster members.  If a role is specified, the JFR will be performed on the members of the given role; otherwise, it will be performed on all the members.\nThe valid commands are: jfrStart, jfrStop, jfrDump, jfrCheck.\n\nThe options are comma separated JFR options.",
                "flightRecording",
                "[Ljava/lang/String;",
                new String[] {"sRole", "sCmd", "sOptions", },
                new String[] {"Ljava.lang.String;", "Ljava.lang.String;", "Ljava.lang.String;", },
                null,
                });
            }

        // behavior getClusterConfig()
            {
            mapInfo.put("getClusterConfig()", new Object[]
                {
                "Get Coherence Cluster configuration.",
                "getClusterConfig",
                "Ljava/lang/String;",
                new String[] {},
                new String[] {},
                null
                });
            }

        // behavior getClusterDescription()
            {
            mapInfo.put("getClusterDescription()", new Object[]
                {
                "Get cluster description.",
                "getClusterDescription",
                "Ljava/lang/String;",
                new String[] {},
                new String[] {},
                null,
                });
            }

        // behavior logClusterState(String sRole)
            {
            mapInfo.put("logClusterState(Ljava.lang.String;)", new Object[]
                {
                "Log state on cluster members running with specified role. If sRole is not specified, state will be logged on all cluster members. The state includes full thread dump and outstanding polls.",
                "logClusterState",
                "V",
                new String[] {"sRole", },
                new String[] {"Ljava.lang.String;", },
                null,
                });
            }
        
        // behavior resumeService(String sService)
            {
            mapInfo.put("resumeService(Ljava.lang.String;)", new Object[]
                {
                "Resume all suspended members of the service identified by the specified name.\n\nIf \"Cluster\" is passed for the service name, all services (including the ClusterService) will be resumed.",
                "resumeService",
                "V",
                new String[] {"sService", },
                new String[] {"Ljava.lang.String;", },
                null,
                });
            }
        
        // behavior shutdown()
            {
            mapInfo.put("shutdown()", new Object[]
                {
                "Shuts down the cluster service on this node.",
                "shutdown",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior suspendService(String sService)
            {
            mapInfo.put("suspendService(Ljava.lang.String;)", new Object[]
                {
                "Suspend all members of the service identified by the specified name.  A suspended Service has been placed in a \"quiesced\" or \"deactivated\" state in preparation to be shutdown.  Once suspended, a service may be \"resumed\" or \"reactivated\" with the resumeService operation.\n\nIf \"Cluster\" is passed for the service name, all services (including the ClusterService) will be suspended.",
                "suspendService",
                "V",
                new String[] {"sService", },
                new String[] {"Ljava.lang.String;", },
                null,
                });
            }
        
        return mapInfo;
        }
    
    /**
     * Configure the tracing sampling ratio for those members matching the
    * specified role, or if role is null or empty,
    * configure the tracing ratio for all cluster members.
     */
    public void configureTracing(String sRole, float fRatio)
        {
        }
    
    /**
     * Dump heap across the cluster.
     */
    public void dumpClusterHeap(String sRole)
        {
        }
    
    /**
     * Ensures that the cluster service is running on this node.
     */
    public void ensureRunning()
        {
        }
    
    /**
     * Perform a Java flight recorder operation on all eligible cluster members.
    *  If a role is specified, the JFR will be performed on the members of the
    * given role; otherwise, it will be performed on all the members.
    * The valid commands are: jfrStart, jfrStop, jfrDump, jfrCheck.
    * 
    * The options are comma separated JFR options.
     */
    public String[] flightRecording(String sRole, String sCmd, String sOptions)
        {
        return null;
        }

    /**
     * Get the Coherence Cluster Configuration
     */
    public String getClusterConfig()
        {
        return null;
        }

    // Accessor for the property "ClusterName"
    /**
     * Getter for property ClusterName.<p>
    * The name of the cluster.
     */
    public String getClusterName()
        {
        return null;
        }
    
    // Accessor for the property "ClusterSize"
    /**
     * Getter for property ClusterSize.<p>
    * The total number of cluster nodes.
    * 
    * @descriptor metrics.value=Size
     */
    public int getClusterSize()
        {
        return 0;
        }
    
    // Accessor for the property "LicenseMode"
    /**
     * Getter for property LicenseMode.<p>
    * The license mode that this cluster is using. Possible values are
    * Evaluation, Development or Production.
     */
    public String getLicenseMode()
        {
        return null;
        }
    
    // Accessor for the property "LocalMemberId"
    /**
     * Getter for property LocalMemberId.<p>
    * The member id for the cluster member that is co-located with the
    * reporting MBeanServer; -1 if the cluster service is not running.
     */
    public int getLocalMemberId()
        {
        return 0;
        }
    
    // Accessor for the property "MemberIds"
    /**
     * Getter for property MemberIds.<p>
    * An array of all existing cluster member ids.
     */
    public int[] getMemberIds()
        {
        return null;
        }
    
    // Accessor for the property "Members"
    /**
     * Getter for property Members.<p>
    * An array of all existing cluster members.
     */
    public String[] getMembers()
        {
        return null;
        }
    
    // Accessor for the property "MembersDeparted"
    /**
     * Getter for property MembersDeparted.<p>
    * An array of strings containing the Member information for recently
    * departed cluster members.   Members will be removed from this array when
    * the member id is recycled.   This information is since the node has
    * joined the cluster and is reset when the MBeanServer node leaves and
    * rejoins the cluster.
    * 
    * The MembersDepartureCount is the total count of departed members and not
    * the size of this array.
     */
    public String[] getMembersDeparted()
        {
        return null;
        }
    
    // Accessor for the property "MembersDepartureCount"
    /**
     * Getter for property MembersDepartureCount.<p>
    * The number of times this node has observed another node`s departure from
    * the cluster since this management node has joined the cluster or
    * statistics have been reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getMembersDepartureCount()
        {
        return 0L;
        }
    
    // Accessor for the property "OldestMemberId"
    /**
     * Getter for property OldestMemberId.<p>
    * The senior cluster member id; -1 if the cluster service is not running.
    * 
    * @descriptor metrics.tag=senior_member_id
     */
    public int getOldestMemberId()
        {
        return 0;
        }
    
    // Accessor for the property "Version"
    /**
     * Getter for property Version.<p>
    * The Coherence version.
    * 
    * @descriptor metrics.tag=version
     */
    public String getVersion()
        {
        return null;
        }
    
    // Accessor for the property "Running"
    /**
     * Getter for property Running.<p>
    * Specifies whether or not the cluster is running.
     */
    public boolean isRunning()
        {
        return false;
        }
    
    /**
     * Log state on cluster members running with specified role. If sRole is not
    * specified, state will be logged on all cluster members. The state
    * includes full thread dump and outstanding polls.
     */
    public void logClusterState(String sRole)
        {
        }
    
    /**
     * Resume all suspended members of the service identified by the specified
    * name.
    * 
    * If "Cluster" is passed for the service name, all services (including the
    * ClusterService) will be resumed.
     */
    public void resumeService(String sService)
        {
        }
    
    /**
     * Shuts down the cluster service on this node.
     */
    public void shutdown()
        {
        }
    
    /**
     * Suspend all members of the service identified by the specified name.  A
    * suspended Service has been placed in a "quiesced" or "deactivated" state
    * in preparation to be shutdown.  Once suspended, a service may be
    * "resumed" or "reactivated" with the resumeService operation.
    * 
    * If "Cluster" is passed for the service name, all services (including the
    * ClusterService) will be suspended.
     */
    public void suspendService(String sService)
        {
        }
    }
