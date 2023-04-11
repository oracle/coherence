
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.PointToPointMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

/**
 * The PointToPointMBean exposes network status between two cluster members. 
 * More specificially, this MBean's values are from the perspective of this
 * viewing member with respect to a configurable viewed member.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class PointToPointMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
    {
    // ---- Fields declarations ----
    
    /**
     * Property ViewedMemberId
     *
     * The Id of the member being viewed.
     */
    private transient int __m_ViewedMemberId;
    
    // Default constructor
    public PointToPointMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public PointToPointMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.PointToPointMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/PointToPointMBean".replace('/', '.'));
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
            "The PointToPointMBean exposes network status between two cluster members.  More specificially, this MBean's values are from the perspective of this viewing member with respect to a configurable viewed member.",
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
        
        // property DeferredPackets
            {
            mapInfo.put("DeferredPackets", new Object[]
                {
                "The number of packets addressed to the viewed member that the viewing member is currently deferring to send.  The viewing member will delay sending these packets until the number of outstanding packets falls below the value of the Threshold attribute.  The value of this attribute is only meaningful if the viewing member has FlowControl enabled.",
                "getDeferredPackets",
                null,
                "I",
                null,
                });
            }
        
        // property Deferring
            {
            mapInfo.put("Deferring", new Object[]
                {
                "Indicates whether or not the viewing member is currently deferring packets to the viewed member.  The value of this attribute is only meaningful if the viewing member has FlowControl enabled.",
                "isDeferring",
                null,
                "Z",
                null,
                });
            }
        
        // property LastIn
            {
            mapInfo.put("LastIn", new Object[]
                {
                "The number of milliseconds that have elapsed since the viewing member last received an acknowledgment from the viewed member.",
                "getLastIn",
                null,
                "J",
                null,
                });
            }
        
        // property LastOut
            {
            mapInfo.put("LastOut", new Object[]
                {
                "The number of milliseconds that have elapsed since the viewing member last sent a packet to the viewed member.",
                "getLastOut",
                null,
                "J",
                null,
                });
            }
        
        // property LastSlow
            {
            mapInfo.put("LastSlow", new Object[]
                {
                "The number of milliseconds that have elapsed since the viewing member declared the viewed member as slow, or -1 if the viewed member has never been declared slow.",
                "getLastSlow",
                null,
                "J",
                null,
                });
            }
        
        // property OutstandingPackets
            {
            mapInfo.put("OutstandingPackets", new Object[]
                {
                "The number of packets that the viewing member has sent to the viewed member which have yet to be acknowledged.  The value of this attribute is only meaningful if the viewing member has FlowControl enabled.",
                "getOutstandingPackets",
                null,
                "I",
                null,
                });
            }
        
        // property Paused
            {
            mapInfo.put("Paused", new Object[]
                {
                "Indicates whether or not the viewing member currently considers the viewed member to be unresponsive.  The value of this attribute is only meaningful if the viewing member has FlowControl enabled.",
                "isPaused",
                null,
                "Z",
                null,
                });
            }
        
        // property PauseRate
            {
            mapInfo.put("PauseRate", new Object[]
                {
                "The percentage of time since the last time statistics were reset in which the viewing member considered the viewed member to be unresponsive. Under normal conditions this value should be very close to 0.0.  Values near 1.0 would indicate that the viewed node is nearly inoperable, likely due to extremely long GC pauses.  The value of this attribute is only meaningful if the viewing member has FlowControl enabled.",
                "getPauseRate",
                null,
                "F",
                null,
                });
            }
        
        // property PublisherSuccessRate
            {
            mapInfo.put("PublisherSuccessRate", new Object[]
                {
                "The publisher success rate from the viewing node to the viewed node since the statistics were last reset.",
                "getPublisherSuccessRate",
                null,
                "F",
                null,
                });
            }
        
        // property ReceiverSuccessRate
            {
            mapInfo.put("ReceiverSuccessRate", new Object[]
                {
                "The receiver success rate from the viewing node to the viewed node since the statistics were last reset.",
                "getReceiverSuccessRate",
                null,
                "F",
                null,
                });
            }
        
        // property Threshold
            {
            mapInfo.put("Threshold", new Object[]
                {
                "The maximum number of outstanding packets for the viewed member that the viewing member is allowed to accumulate before initiating the deferral algorithm.  The value of this attribute is only meaningful if the viewing member has FlowControl enabled.",
                "getThreshold",
                null,
                "I",
                null,
                });
            }
        
        // property ViewedMemberId
            {
            mapInfo.put("ViewedMemberId", new Object[]
                {
                "The Id of the member being viewed.",
                "getViewedMemberId",
                "setViewedMemberId",
                "I",
                null,
                });
            }
        
        // property ViewerStatistics
            {
            mapInfo.put("ViewerStatistics", new Object[]
                {
                "Human readable summary of the point-to-point statistics from the viewing member for all other members.",
                "getViewerStatistics",
                null,
                "[Ljava/lang/String;",
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
                "Reset the viewing member`s point-to-point statistics for all other members.",
                "resetStatistics",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior trackWeakest()
            {
            mapInfo.put("trackWeakest()", new Object[]
                {
                "Instruct the PointToPointMBean to track the weakest member.  A viewed member is considered to be weak if either the corresponding publisher or receiver success rates are below 1.0.",
                "trackWeakest",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        return mapInfo;
        }
    
    // Accessor for the property "DeferredPackets"
    /**
     * Getter for property DeferredPackets.<p>
    * The number of packets addressed to the viewed member that the viewing
    * member is currently deferring to send.  The viewing member will delay
    * sending these packets until the number of outstanding packets falls below
    * the value of the Threshold attribute.  The value of this attribute is
    * only meaningful if the viewing member has FlowControl enabled.
     */
    public int getDeferredPackets()
        {
        return 0;
        }
    
    // Accessor for the property "LastIn"
    /**
     * Getter for property LastIn.<p>
    * The number of milliseconds that have elapsed since the viewing member
    * last received an acknowledgment from the viewed member.
     */
    public long getLastIn()
        {
        return 0L;
        }
    
    // Accessor for the property "LastOut"
    /**
     * Getter for property LastOut.<p>
    * The number of milliseconds that have elapsed since the viewing member
    * last sent a packet to the viewed member.
     */
    public long getLastOut()
        {
        return 0L;
        }
    
    // Accessor for the property "LastSlow"
    /**
     * Getter for property LastSlow.<p>
    * The number of milliseconds that have elapsed since the viewing member
    * declared the viewed member as slow, or -1 if the viewed member has never
    * been declared slow.
     */
    public long getLastSlow()
        {
        return 0L;
        }
    
    // Accessor for the property "OutstandingPackets"
    /**
     * Getter for property OutstandingPackets.<p>
    * The number of packets that the viewing member has sent to the viewed
    * member which have yet to be acknowledged.  The value of this attribute is
    * only meaningful if the viewing member has FlowControl enabled.
     */
    public int getOutstandingPackets()
        {
        return 0;
        }
    
    // Accessor for the property "PauseRate"
    /**
     * Getter for property PauseRate.<p>
    * The percentage of time since the last time statistics were reset in which
    * the viewing member considered the viewed member to be unresponsive. Under
    * normal conditions this value should be very close to 0.0.  Values near
    * 1.0 would indicate that the viewed node is nearly inoperable, likely due
    * to extremely long GC pauses.  The value of this attribute is only
    * meaningful if the viewing member has FlowControl enabled.
     */
    public float getPauseRate()
        {
        return 0.0F;
        }
    
    // Accessor for the property "PublisherSuccessRate"
    /**
     * Getter for property PublisherSuccessRate.<p>
    * The publisher success rate from the viewing node to the viewed node since
    * the statistics were last reset.
     */
    public float getPublisherSuccessRate()
        {
        return 0.0F;
        }
    
    // Accessor for the property "ReceiverSuccessRate"
    /**
     * Getter for property ReceiverSuccessRate.<p>
    * The receiver success rate from the viewing node to the viewed node since
    * the statistics were last reset.
     */
    public float getReceiverSuccessRate()
        {
        return 0.0F;
        }
    
    // Accessor for the property "Threshold"
    /**
     * Getter for property Threshold.<p>
    * The maximum number of outstanding packets for the viewed member that the
    * viewing member is allowed to accumulate before initiating the deferral
    * algorithm.  The value of this attribute is only meaningful if the viewing
    * member has FlowControl enabled.
     */
    public int getThreshold()
        {
        return 0;
        }
    
    // Accessor for the property "ViewedMemberId"
    /**
     * Getter for property ViewedMemberId.<p>
    * The Id of the member being viewed.
     */
    public int getViewedMemberId()
        {
        return __m_ViewedMemberId;
        }
    
    // Accessor for the property "ViewerStatistics"
    /**
     * Getter for property ViewerStatistics.<p>
    * Human readable summary of the point-to-point statistics from the viewing
    * member for all other members.
     */
    public String[] getViewerStatistics()
        {
        return null;
        }
    
    // Accessor for the property "Deferring"
    /**
     * Getter for property Deferring.<p>
    * Indicates whether or not the viewing member is currently deferring
    * packets to the viewed member.  The value of this attribute is only
    * meaningful if the viewing member has FlowControl enabled.
     */
    public boolean isDeferring()
        {
        return false;
        }
    
    // Accessor for the property "Paused"
    /**
     * Getter for property Paused.<p>
    * Indicates whether or not the viewing member currently considers the
    * viewed member to be unresponsive.  The value of this attribute is only
    * meaningful if the viewing member has FlowControl enabled.
     */
    public boolean isPaused()
        {
        return false;
        }
    
    /**
     * Reset the viewing member`s point-to-point statistics for all other
    * members.
     */
    public void resetStatistics()
        {
        }
    
    // Accessor for the property "ViewedMemberId"
    /**
     * Setter for property ViewedMemberId.<p>
    * The Id of the member being viewed.
     */
    public void setViewedMemberId(int nMemberId)
        {
        __m_ViewedMemberId = nMemberId;
        }
    
    /**
     * Instruct the PointToPointMBean to track the weakest member.  A viewed
    * member is considered to be weak if either the corresponding publisher or
    * receiver success rates are below 1.0.
     */
    public void trackWeakest()
        {
        }
    }
