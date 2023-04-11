
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.ServiceInfo

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.memberSet.ActualMemberSet;
import com.tangosol.util.Base;
import java.util.Set;

/**
 * This component is used to keep data about a Service that is operating in the
 * cluster.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ServiceInfo
        extends    com.tangosol.coherence.component.Net
        implements com.tangosol.net.ServiceInfo
    {
    // ---- Fields declarations ----
    
    /**
     * Property MemberSet
     *
     * The Members that are running the Service.
     */
    private com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet __m_MemberSet;
    
    /**
     * Property ServiceId
     *
     * The id assigned by the cluster to the Service. Every Member in the
     * cluster knows the Service by this id. Service id zero is reserved; it is
     * used by the ClusterService itself.
     */
    private int __m_ServiceId;
    
    /**
     * Property ServiceJoinTime
     *
     * The cluster time when a Member joined the Service, indexed by Member id.
     */
    private long[] __m_ServiceJoinTime;
    
    /**
     * Property ServiceName
     *
     * The name of the Service.
     */
    private String __m_ServiceName;
    
    /**
     * Property ServiceType
     *
     * The type of the Service.
     */
    private String __m_ServiceType;
    
    /**
     * Property ServiceVersion
     *
     * The version of a Service, indexed by Member id. (Different Members may
     * have different versions of the Service running.)
     */
    private String[] __m_ServiceVersion;
    
    /**
     * Property Suspended
     *
     * True iff the service has been suspended.
     */
    private boolean __m_Suspended;
    
    // Default constructor
    public ServiceInfo()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ServiceInfo(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.ServiceInfo();
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
            clz = Class.forName("com.tangosol.coherence/component/net/ServiceInfo".replace('/', '.'));
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
    
    // Accessor for the property "MemberSet"
    /**
     * Getter for property MemberSet.<p>
    * The Members that are running the Service.
     */
    public com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet getMemberSet()
        {
        return __m_MemberSet;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    // Accessor for the property "OldestMember"
    /**
     * Getter for property OldestMember.<p>
    * (Calculated) The "most senior" Member that is running the Service.
     */
    public com.tangosol.net.Member getOldestMember()
        {
        return getMemberSet().getOldestMember();
        }
    
    // Accessor for the property "ServiceId"
    /**
     * Getter for property ServiceId.<p>
    * The id assigned by the cluster to the Service. Every Member in the
    * cluster knows the Service by this id. Service id zero is reserved; it is
    * used by the ClusterService itself.
     */
    public int getServiceId()
        {
        return __m_ServiceId;
        }
    
    // Accessor for the property "ServiceJoinTime"
    /**
     * Getter for property ServiceJoinTime.<p>
    * The cluster time when a Member joined the Service, indexed by Member id.
     */
    public long getServiceJoinTime(int i)
        {
        return getMemberSet().getServiceJoinTime(i);
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public com.tangosol.net.Member getServiceMember(int nId)
        {
        return getMemberSet().getMember(nId);
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public java.util.Set getServiceMembers()
        {
        // import Component.Net.MemberSet.ActualMemberSet;
        // import java.util.Set;
        
        // this is a publicly exposed property; return a clone
        
        Set setMember = new ActualMemberSet();
        setMember.addAll(getMemberSet());
        
        return setMember;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    // Accessor for the property "ServiceName"
    /**
     * Getter for property ServiceName.<p>
    * The name of the Service.
     */
    public String getServiceName()
        {
        return __m_ServiceName;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    // Accessor for the property "ServiceType"
    /**
     * Getter for property ServiceType.<p>
    * The type of the Service.
     */
    public String getServiceType()
        {
        return __m_ServiceType;
        }
    
    // Accessor for the property "ServiceVersion"
    /**
     * Getter for property ServiceVersion.<p>
    * The version of a Service, indexed by Member id. (Different Members may
    * have different versions of the Service running.)
     */
    public String getServiceVersion(int i)
        {
        return getMemberSet().getServiceVersion(i);
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    // Accessor for the property "ServiceVersion"
    /**
     * Getter for property ServiceVersion.<p>
    * The version of a Service, indexed by Member id. (Different Members may
    * have different versions of the Service running.)
     */
    public String getServiceVersion(com.tangosol.net.Member member)
        {
        // import Component.Net.Member;
        
        return member instanceof Member ?
            getServiceVersion(((Member) member).getId()) : null;
        }
    
    // Accessor for the property "Suspended"
    /**
     * Getter for property Suspended.<p>
    * True iff the service has been suspended.
     */
    public boolean isSuspended()
        {
        return __m_Suspended;
        }
    
    public void read(java.io.DataInputStream stream)
            throws java.io.IOException
        {
        setServiceId(stream.readUnsignedShort());
        setServiceName(stream.readUTF());
        setServiceType(stream.readUTF());
        }
    
    // Accessor for the property "MemberSet"
    /**
     * Setter for property MemberSet.<p>
    * The Members that are running the Service.
     */
    public void setMemberSet(com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet setMember)
        {
        _assert(getMemberSet() == null, "Not resettable");
        
        __m_MemberSet = (setMember);
        }
    
    // Accessor for the property "ServiceId"
    /**
     * Setter for property ServiceId.<p>
    * The id assigned by the cluster to the Service. Every Member in the
    * cluster knows the Service by this id. Service id zero is reserved; it is
    * used by the ClusterService itself.
     */
    public void setServiceId(int nId)
        {
        _assert(getServiceId() == 0 && nId >= 0);
        
        getMemberSet().setServiceId(nId);
        
        __m_ServiceId = (nId);
        }
    
    // Accessor for the property "ServiceJoinTime"
    /**
     * Setter for property ServiceJoinTime.<p>
    * The cluster time when a Member joined the Service, indexed by Member id.
     */
    public void setServiceJoinTime(int i, long lMillis)
        {
        getMemberSet().setServiceJoinTime(i, lMillis);
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Setter for property ServiceName.<p>
    * The name of the Service.
     */
    public void setServiceName(String sName)
        {
        _assert(getServiceName() == null && sName != null);
        
        getMemberSet().setServiceName(sName);
        
        __m_ServiceName = (sName);
        }
    
    // Accessor for the property "ServiceType"
    /**
     * Setter for property ServiceType.<p>
    * The type of the Service.
     */
    public void setServiceType(String sType)
        {
        _assert(getServiceType() == null && sType != null);
        
        __m_ServiceType = (sType);
        }
    
    // Accessor for the property "ServiceVersion"
    /**
     * Setter for property ServiceVersion.<p>
    * The version of a Service, indexed by Member id. (Different Members may
    * have different versions of the Service running.)
     */
    public void setServiceVersion(int i, String sVersion)
        {
        getMemberSet().setServiceVersion(i, sVersion);
        }
    
    // Accessor for the property "Suspended"
    /**
     * Setter for property Suspended.<p>
    * True iff the service has been suspended.
     */
    public void setSuspended(boolean fSuspended)
        {
        __m_Suspended = fSuspended;
        }
    
    // Declared at the super level
    public String toString()
        {
        // import com.tangosol.util.Base;
        
        StringBuffer sb = new StringBuffer();
        
        sb.append("ServiceInfo(")
          .append("Id=")
          .append(getServiceId())
          .append(", Name=")
          .append(getServiceName())
          .append(", Type=")
          .append(getServiceType())
          .append("\n  MemberSet=")
          .append(Base.indentString(getMemberSet().toString(), "  ", false))
          .append("\n)");
        
        return sb.toString();
        }
    
    public void write(java.io.DataOutputStream stream)
            throws java.io.IOException
        {
        stream.writeShort(getServiceId());
        stream.writeUTF(getServiceName());
        stream.writeUTF(getServiceType());
        }
    }
