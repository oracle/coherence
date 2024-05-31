
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet

package com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet;

import com.tangosol.coherence.component.application.console.Coherence;
import com.tangosol.coherence.component.net.Cluster;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.packet.messagePacket.Directed;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService;
import com.tangosol.internal.util.VersionHelper;
import com.tangosol.license.LicensedObject;
import com.tangosol.util.Base;

/**
 * Set of Member objects; must be thread safe.
 * 
 * ActualMemberSet holds references to its members.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class MasterMemberSet
        extends    com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet
    {
    // ---- Fields declarations ----
    
    /**
     * Property AddressMap
     *
     * Map from Member SocketAddress to Member.
     */
    private java.util.Map __m_AddressMap;
    
    /**
     * Property AscensionTimestamp
     *
     * The local "safe time" at which the current OldestMember was identified
     * as such.
     */
    private long __m_AscensionTimestamp;
    
    /**
     * Property MAX_MEMBERS
     *
     * The maximum number of Members that can be supported by the
     * MastMemberSet. (Due to the persistent form of the bit-set itself, the
     * absolute maximum is 255 * 32 = 8160.)
     */
    public static final int MAX_MEMBERS = 8160;

    /**
     * Property TRANSPORT_COMPATIBILITY
     *
     * Denotes that a Member's VERSION is not actual version, but transport compatibility version in Coherence log.
     * @see #appendVersion(StringBuilder, int)
     */
    public static final String TRANSPORT_COMPATIBILITY = "Compat[";
    
    /**
     * Property MaximumPacketLength
     *
     * The Cluster's maximum packet length.  This value is used when inducting
     * new members to ensure that we don't cross over the cluster size limit
     * which would prevent directed messages addressed to all members.
     */
    private transient int __m_MaximumPacketLength;
    
    /**
     * Property MemberMap
     *
     * Map from Member UID to Member.
     */
    private java.util.Map __m_MemberMap;
    
    /**
     * Property RecycleMillis
     *
     * The number of milliseconds before which a mini-id is recycled.
     * Generally, this should be greater than the expected max timeout that
     * determines unannounced Member death.
     */
    private int __m_RecycleMillis;

    /**
     * Property RecycleSet
     *
     * A MemberSet of Members that have been removed that helps the
     * MasterMemberSet avoid giving out a Member mini-id before it has been
     * "recycled" (dead at least RecycleMillis milliseconds).
     */
    private com.tangosol.coherence.component.net.memberSet.ActualMemberSet __m_RecycleSet;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Iterator", com.tangosol.coherence.component.net.MemberSet.Iterator.get_CLASS());
        }
    
    // Default constructor
    public MasterMemberSet()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public MasterMemberSet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setAddressMap(new com.tangosol.util.SafeHashMap());
            setMemberMap(new com.tangosol.util.SafeHashMap());
            setRecycleMillis(60000);
            setRecycleSet(new com.tangosol.coherence.component.net.memberSet.ActualMemberSet());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        
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
        return new com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet();
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
            clz = Class.forName("com.tangosol.coherence/component/net/memberSet/actualMemberSet/serviceMemberSet/MasterMemberSet".replace('/', '.'));
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
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    // Declared at the super level
    public synchronized boolean add(Object o)
        {
        // import Component.Net.Member;
        
        if (super.add(o))
            {
            Member memberNew = (Member) o;
            getMemberMap().put(memberNew.getUid32(), memberNew);
            getAddressMap().put(memberNew.getSocketAddress(), memberNew);
            getRecycleSet().remove(memberNew.getId());
            return true;
            }
        else
            {
            return false;
            }
        }
    
    // Declared at the super level
    protected void appendEdition(StringBuilder sb, int nMember)
        {
        // import Component.Application.Console.Coherence;
        // import Component.Net.Member;
        
        Member member   = getMember(nMember);
        String sEdition = member == null
                ? "UNKNOWN" : Coherence.EDITION_NAMES[member.getEdition()];
        
        sb.append('|').append(sEdition);
        }
    
    // Declared at the super level
    protected void appendEditionHeader(StringBuilder sb)
        {
        sb.append("|Edition");
        }
    
    // Declared at the super level
    protected void appendTransport(StringBuilder sb, int nMember)
        {
        // no transport info for the ClusterService
        return;
        }
    
    // Declared at the super level
    protected void appendTransportHeader(StringBuilder sb)
        {
        // no transport info for the ClusterService
        return;
        }
    
    // Declared at the super level
    protected void appendVersion(StringBuilder sb, int nMember)
        {
        int     nState   = getState(nMember);

        sb.append('|');
        if (nState <= MEMBER_JOINING)
            {
            sb.append(TRANSPORT_COMPATIBILITY);
            }
        sb.append(getServiceVersionExternal(nMember));
        if (nState <= MEMBER_JOINING)
            {
            sb.append("]");
            }
        }
    
    // Declared at the super level
    protected void appendVersionHeader(StringBuilder sb)
        {
        sb.append("|Version");
        }
    
    // Declared at the super level
    public void clear()
        {
        throw new UnsupportedOperationException();
        }
    
    // Declared at the super level
    /**
     * Compare the seniority of two members within this service member set.
    * 
    * return -1, 0, or 1 if memberA is older then, equal to, or younger then
    * memberB.
     */
    public long compareSeniority(com.tangosol.coherence.component.net.Member memberA, com.tangosol.coherence.component.net.Member memberB)
        {
        return memberA.getUid32().compareTo(memberB.getUid32());
        }
    
    // Declared at the super level
    public synchronized com.tangosol.util.ObservableMap ensureMemberConfigMap(int i)
        {
        // as of Coherence 12.1.2, ClusterService no longer uses the MemberConfigMap
        return null;
        }
    
    /**
     * Find a dead (recycled) member with the specified Id and UID.
     */
    public com.tangosol.coherence.component.net.Member findDeadMember(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Member;
        
        Member memberDead = getRecycleSet().getMember(member.getId());
        
        return memberDead != null && memberDead.getUid32().equals(member.getUid32()) ?
            memberDead : null;
        }
    
    // Accessor for the property "AddressMap"
    /**
     * Getter for property AddressMap.<p>
    * Map from Member SocketAddress to Member.
     */
    public java.util.Map getAddressMap()
        {
        return __m_AddressMap;
        }
    
    // Accessor for the property "AscensionTimestamp"
    /**
     * Getter for property AscensionTimestamp.<p>
    * The local "safe time" at which the current OldestMember was identified as
    * such.
     */
    public long getAscensionTimestamp()
        {
        return __m_AscensionTimestamp;
        }
    
    // Declared at the super level
    public String getDescription()
        {
        // import com.tangosol.util.Base;
        
        return "\n  ThisMember=" + getThisMember()
             + super.getDescription()
             + "\n  RecycleMillis="   + getRecycleMillis()
             + "\n  RecycleSet=" + Base.indentString(getRecycleSet().toString(), "  ", false);
        }
    
    // Accessor for the property "MaximumPacketLength"
    /**
     * Getter for property MaximumPacketLength.<p>
    * The Cluster's maximum packet length.  This value is used when inducting
    * new members to ensure that we don't cross over the cluster size limit
    * which would prevent directed messages addressed to all members.
     */
    public int getMaximumPacketLength()
        {
        return __m_MaximumPacketLength;
        }
    
    // Accessor for the property "Member"
    /**
     * Get the member by UID.
     */
    public com.tangosol.coherence.component.net.Member getMember(com.tangosol.util.UUID uid)
        {
        // import Component.Net.Member;
        
        return (Member) getMemberMap().get(uid);
        }
    
    // Accessor for the property "Member"
    /**
     * Get the member by address.
     */
    public com.tangosol.coherence.component.net.Member getMember(java.net.SocketAddress addr)
        {
        // import Component.Net.Member;
        
        return (Member) getAddressMap().get(addr);
        }
    
    // Accessor for the property "MemberMap"
    /**
     * Getter for property MemberMap.<p>
    * Map from Member UID to Member.
     */
    protected java.util.Map getMemberMap()
        {
        return __m_MemberMap;
        }
    
    // Accessor for the property "RecycleMillis"
    /**
     * Getter for property RecycleMillis.<p>
    * The number of milliseconds before which a mini-id is recycled. Generally,
    * this should be greater than the expected max timeout that determines
    * unannounced Member death.
     */
    public int getRecycleMillis()
        {
        return __m_RecycleMillis;
        }
    
    // Accessor for the property "RecycleSet"
    /**
     * Getter for property RecycleSet.<p>
    * A MemberSet of Members that have been removed that helps the
    * MasterMemberSet avoid giving out a Member mini-id before it has been
    * "recycled" (dead at least RecycleMillis milliseconds).
     */
    public com.tangosol.coherence.component.net.memberSet.ActualMemberSet getRecycleSet()
        {
        return __m_RecycleSet;
        }
    
    /**
     * Inducting a member assigns a mini-id and adds the member to the member
    * set.
    * 
    * @param memberNew  a Member object to induct
    * @param service  the ClusterService
    * 
    * @return a ClusterService$REJECT_* code to indicate success (REJECT_NONE)
    * or one of the failure reasons
     */
    public final synchronized int induct(com.tangosol.coherence.component.net.Member memberNew, com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService service)
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.Packet.MessagePacket.Directed;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService;
        // import com.tangosol.license.LicensedObject;
        // import com.tangosol.util.Base;
        
        // this could only be a new member (Id=0) or the first member (Id=1)
        _assert(memberNew != null && memberNew.getId() <= 1);
        _assert(getThisMember() == getOldestMember());
        
        // check the new member's IP against the authorized host list
        if (!((Cluster) service.getCluster()).
                isAuthorizedHost(memberNew.getAddress()))
            {
            return ClusterService.REJECT_AUTHORIZE;
            }
        
        // check the senior's licenses for expiry
        if (LicensedObject.isExpired())
            {
            return ClusterService.REJECT_LICENSE_EXPIRED;
            }
        
        // check for out-of-order joins
        if (memberNew.getTimestamp() <= getLastJoinTime())
            {
            return ClusterService.REJECT_RESTART;
            }
        
        // find an open member mini id that:
        //  a) has not been recycled (not in RecycleSet), or
        //  b) has been "sufficiently" recycled (RecycleMillis have passed
        //     since member.setDead(true) was called)
        long      lCutoffMillis = Base.getSafeTimeMillis() - getRecycleMillis();
        MemberSet setRecycle    = getRecycleSet();
        for (int i = 1; i <= MAX_MEMBERS; ++i)
            {
            if (!contains(i))
                {
                Member memberDead = setRecycle.getMember(i);
                if (memberDead == null || memberDead.getTimestamp() < lCutoffMillis)
                    {
                    memberNew.setId(i);
                    add(memberNew);
        
                    // validate that this doesn't push the cluster to a size which
                    // would prevent sending a message to the entire cluster
                    if (Directed.calcHeaderLength(this) <= getMaximumPacketLength())
                        {
                        return ClusterService.REJECT_NONE; // inducted
                        }
                    
                    // cluster is to large
                    remove(memberNew);
                    getRecycleSet().remove(memberNew);
                    break;
                    }
                }
            }
        return ClusterService.REJECT_SIZE; // cluster is to large
        }

    // Declared at the super level
    public synchronized boolean remove(Object o)
        {
        // import Component.Net.Member;
        
        _assert(o != getThisMember());
        
        if (super.remove(o))
            {
            Member member = (Member) o;
            getMemberMap().remove(member.getUid32());
            getAddressMap().remove(member.getSocketAddress());
        
            member.setDead(true);
            getRecycleSet().add(member);
        
            return true;
            }
        else
            {
            return false;
            }
        }
    
    // Accessor for the property "AddressMap"
    /**
     * Setter for property AddressMap.<p>
    * Map from Member SocketAddress to Member.
     */
    public void setAddressMap(java.util.Map mapAddress)
        {
        __m_AddressMap = mapAddress;
        }
    
    // Accessor for the property "AscensionTimestamp"
    /**
     * Setter for property AscensionTimestamp.<p>
    * The local "safe time" at which the current OldestMember was identified as
    * such.
     */
    public void setAscensionTimestamp(long lTimestamp)
        {
        __m_AscensionTimestamp = lTimestamp;
        }
    
    // Accessor for the property "MaximumPacketLength"
    /**
     * Setter for property MaximumPacketLength.<p>
    * The Cluster's maximum packet length.  This value is used when inducting
    * new members to ensure that we don't cross over the cluster size limit
    * which would prevent directed messages addressed to all members.
     */
    public void setMaximumPacketLength(int cb)
        {
        __m_MaximumPacketLength = cb;
        }
    
    // Accessor for the property "MemberMap"
    /**
     * Setter for property MemberMap.<p>
    * Map from Member UID to Member.
     */
    protected void setMemberMap(java.util.Map map)
        {
        __m_MemberMap = map;
        }
    
    // Declared at the super level
    /**
     * Setter for property OldestMember.<p>
    * The ServiceMemberSet keeps track of the "most senior" Member that is
    * running the Service, to which special synchronization tasks can be
    * delegated.
    * The MasterMemberSet keeps track of the "most senior" Member, to which
    * special synchronization tasks (such as assigning new Member mini-ids) can
    * be delegated.
     */
    protected synchronized void setOldestMember(com.tangosol.coherence.component.net.Member member)
        {
        // import com.tangosol.util.Base;
        
        super.setOldestMember(member);
        
        setAscensionTimestamp(Base.getSafeTimeMillis());
        }
    
    // Accessor for the property "RecycleMillis"
    /**
     * Setter for property RecycleMillis.<p>
    * The number of milliseconds before which a mini-id is recycled. Generally,
    * this should be greater than the expected max timeout that determines
    * unannounced Member death.
     */
    public void setRecycleMillis(int cMillis)
        {
        __m_RecycleMillis = cMillis;
        }
    
    // Accessor for the property "RecycleSet"
    /**
     * Setter for property RecycleSet.<p>
    * A MemberSet of Members that have been removed that helps the
    * MasterMemberSet avoid giving out a Member mini-id before it has been
    * "recycled" (dead at least RecycleMillis milliseconds).
     */
    protected void setRecycleSet(com.tangosol.coherence.component.net.memberSet.ActualMemberSet set)
        {
        __m_RecycleSet = set;
        }
    }
