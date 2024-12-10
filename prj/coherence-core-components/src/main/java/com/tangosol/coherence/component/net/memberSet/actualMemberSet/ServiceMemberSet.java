
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet

package com.tangosol.coherence.component.net.memberSet.actualMemberSet;

import com.oracle.coherence.common.net.exabus.EndPoint;

import com.tangosol.coherence.component.net.Member;

import com.tangosol.internal.util.VersionHelper;

import com.tangosol.util.Base;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.ObservableMap;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntPredicate;

/**
 * Set of Member objects; must be thread safe.
 * 
 * ActualMemberSet holds references to its members.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ServiceMemberSet
        extends    com.tangosol.coherence.component.net.memberSet.ActualMemberSet
    {
    // ---- Fields declarations ----
    
    /**
     * Property BackloggedAtomic
     *
     * Array of AtomicLong objects used as a bitset to track the MessageBus
     * backlog state of the members.
     * 
     * For member set held by the ServiceInfo this property is not used.
     */
    private transient java.util.concurrent.atomic.AtomicLong[] __m_BackloggedAtomic;
    
    /**
     * Property LastJoinTime
     *
     * The ServiceJoinTime of the youngest member to have been added to the
     * MemberSet.
     */
    private long __m_LastJoinTime;
    
    /**
     * Property MEMBER_JOINED
     *
     * Membership state indicating that the member has joined the service.  The
     * JOINED state is reached by direct communication from the member.
     */
    public static final int MEMBER_JOINED = 2;
    
    /**
     * Property MEMBER_JOINING
     *
     * Membership state indicating that the member is in the process of joining
     * the service.  The JOINING state is initiated by cluster senior
     * announcement.
     */
    public static final int MEMBER_JOINING = 1;
    
    /**
     * Property MEMBER_LEAVING
     *
     * Membership state indicating that the member is in the process of leaving
     * the service.  The LEAVING state is initiated by direct communication
     * from the member.
     */
    public static final int MEMBER_LEAVING = 3;
    
    /**
     * Property MEMBER_NEW
     *
     * Membership state indicating that the member is new to the service and
     * has not yet been announced via the join protocol.
     */
    public static final int MEMBER_NEW = 0;
    
    /**
     * Property MemberConfigMap
     *
     * A map of configuration data local to the Service and specific to the
     * Member. This data is maintained by (and mutable only by) the Member that
     * it represents.
     */
    private com.tangosol.util.ObservableMap[] __m_MemberConfigMap;
    
    /**
     * Property OldestLocalMember
     *
     * The ServiceMemberSet keeps track of the "most senior" Member colocated
     * with this member that is running the Service, to which special
     * synchronization tasks can be delegated.
     */
    private com.tangosol.coherence.component.net.Member __m_OldestLocalMember;
    
    /**
     * Property OldestMember
     *
     * The ServiceMemberSet keeps track of the "most senior" Member that is
     * running the Service, to which special synchronization tasks can be
     * delegated.
     */
    private com.tangosol.coherence.component.net.Member __m_OldestMember;
    
    /**
     * Property ServiceBacklogged
     *
     * Calculated property indicating if a given member's EndPoint is
     * backlogged.
     */
    private transient boolean[] __m_ServiceBacklogged;
    
    /**
     * Property ServiceEndPoint
     *
     * The EndPoint used by each Service member, indexed by Member id. The
     * value of null indicates that the member is using the "datagram"
     * transport.
     * 
     * For member set held by the ServiceInfo this property is not used.
     */
    private transient com.oracle.coherence.common.net.exabus.EndPoint[] __m_ServiceEndPoint;
    
    /**
     * Property ServiceEndPointName
     *
     * The EndPoint name used by each Service member, indexed by Member id. The
     * end point name for the "datagram" transport is an empty string.
     * 
     * For member set held by ServiceInfo holds the advertised EndPoint name. 
     * For the member set held by the service this holds the connected EndPoint
     * name. 
     */
    private transient String[] __m_ServiceEndPointName;
    
    /**
     * Property ServiceId
     *
     * The ServiceId.
     * 
     * @see ServiceInfo#setServiceId
     */
    private transient int __m_ServiceId;
    
    /**
     * Property ServiceJoinTime
     *
     * The cluster time that each Member joined the Service, indexed by Member
     * id.
     */
    private long[] __m_ServiceJoinTime;
    
    /**
     * Property ServiceName
     *
     * The ServiceName.
     * 
     * @see ServiceInfo#setServiceName
     */
    private transient String __m_ServiceName;
    
    /**
     * Property ServiceVersion
     *
     * The Service version string that each Member is running, indexed by
     * Member id.
     * 
     * As of Coherence 12.2.1.1 the string accessors are for bakward
     * compatibility only, while internally the versions are stored as encoded
     * integers. 
     * 
     * @functional
     */
    
    /**
     * Property ServiceVersionInt
     *
     * The Service version internal integer representation for each running
     * Member, indexed by Member id.
     */
    private int[] __m_ServiceVersionInt;

    /**
     * The minimum version across all members.
     */
    private int m_nVersionMin;

    /**
     * The maximum version across all members.
     */
    private int m_nVersionMax;

    /**
     * Property State
     *
     * The current state of the service member (indexed by member id).  The
     * state is one of the MEMBER_* constants.
     */
    private int[] __m_State;
    
    /**
     * Property SuccessorMember
     *
     * Return the next oldest member after this member.
     */
    private com.tangosol.coherence.component.net.Member __m_SuccessorMember;
    
    /**
     * Property ThisMember
     *
     * The local member.  This property is only applicable for the
     * ServiceMemberSet held by the service.
     */
    private com.tangosol.coherence.component.net.Member __m_ThisMember;
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
    public ServiceMemberSet()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ServiceMemberSet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet();
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
            clz = Class.forName("com.tangosol.coherence/component/net/memberSet/actualMemberSet/ServiceMemberSet".replace('/', '.'));
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
    
    protected void appendEdition(StringBuilder sb, int nMember)
        {
        // no edition info for Services
        }
    
    protected void appendEditionHeader(StringBuilder sb)
        {
        // no edition info for Services
        }
    
    protected void appendTransport(StringBuilder sb, int nMember)
        {
        sb.append('|')
          .append(formatEndPoint(getServiceEndPointName(nMember)));
        }
    
    protected void appendTransportHeader(StringBuilder sb)
        {
        sb.append("|EndPoint");
        }
    
    protected void appendVersion(StringBuilder sb, int nMember)
        {
        // no version info for Services
        }
    
    protected void appendVersionHeader(StringBuilder sb)
        {
        // no version info for Services
        }
    
    // Declared at the super level
    public synchronized void clear()
        {
        throw new UnsupportedOperationException();
        }
    
    /**
     * Compare the seniority of two members within this service member set.
    * 
    * return -1, 0, or 1 if memberA is older then, equal to, or younger then
    * memberB.
     */
    public long compareSeniority(com.tangosol.coherence.component.net.Member memberA, com.tangosol.coherence.component.net.Member memberB)
        {
        return getServiceJoinTime(memberA.getId()) - getServiceJoinTime(memberB.getId());
        }
    
    /**
     * Copy attributes from the specified ServiceInfo member set into this newly
    * created ServiceMemberSet .
     */
    public void copy(ServiceMemberSet setMember)
        {
        // import Component.Net.Member;
        // import java.util.Iterator as java.util.Iterator;
        
        _assert(isEmpty());
        
        setServiceId(setMember.getServiceId());
        setServiceName(setMember.getServiceName());
        
        synchronized (setMember)
            {
            for (java.util.Iterator iter = setMember.iterator(); iter.hasNext();)
                {
                Member member  = (Member) iter.next();
                int    nMember = member.getId();
        
                add(member);
        
                // copy attribues
                setServiceVersion(nMember, setMember.getServiceVersion(nMember));
                setServiceJoinTime(nMember, setMember.getServiceJoinTime(nMember));
                setServiceEndPointName(nMember, setMember.getServiceEndPointName(nMember));
                setState(nMember, setMember.getState(nMember));
        
                // copy the config map content
                Map mapConfig = ensureMemberConfigMap(nMember);
                if (mapConfig != null)
                    {
                    mapConfig.putAll(setMember.getMemberConfigMap(nMember));
                    }
                }
        
            // validate
            _assert(equals(setMember) && getOldestMember() == setMember.getOldestMember());
            }
        }
    
    /**
     * Convert the version elements into a single integer value.
     */
    public static int encodeVersion(int nYear, int nMonth, int nPatch)
        {
        return VersionHelper.encodeVersion(nYear, nMonth, nPatch);
        }
    
    /**
     * Convert the version elements into a single integer value.
     */
    public static int encodeVersion(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        return VersionHelper.encodeVersion(nMajor, nMinor, nMicro, nPatchSet, nPatch);
        }
    
    public synchronized com.tangosol.util.ObservableMap ensureMemberConfigMap(int i)
        {
        // import com.tangosol.util.ObservableMap;
        // import com.tangosol.util.ObservableHashMap;
        
        ObservableMap[] amap = getMemberConfigMap();
        ObservableMap   map  = (amap == null || i >= amap.length ? null : amap[i]);
        
        if (map == null)
            {
            map = new ObservableHashMap();
            setMemberConfigMap(i, map);
            }
        
        return map;
        }
    
    /**
     * Format the specified EndPoint name as a human-readable string.
     */
    public static String formatEndPoint(String sName)
        {
        return sName == null || sName.length() == 0 ? "shared" : sName;
        }
    
    /**
     * Format the specified timestamp as a human-readable string.
     */
    public static String formatJoinTime(long ldt)
        {
        // import java.sql.Timestamp;
        
        return new Timestamp(ldt).toString();
        }
    
    /**
     * Format the specified membership state as a human-readable string.
     */
    public static String formatStateName(int nState)
        {
        switch (nState)
            {
            case MEMBER_NEW:
                return "NEW";
            case MEMBER_JOINING:
                return "JOINING";
            case MEMBER_JOINED:
                return "JOINED";
            case MEMBER_LEAVING:
                return "LEAVING";
            default:
                return "<unknown>";
            }
        }
    
    // Accessor for the property "BackloggedAtomic"
    /**
     * Getter for property BackloggedAtomic.<p>
    * Array of AtomicLong objects used as a bitset to track the MessageBus
    * backlog state of the members.
    * 
    * For member set held by the ServiceInfo this property is not used.
     */
    protected java.util.concurrent.atomic.AtomicLong[] getBackloggedAtomic()
        {
        return __m_BackloggedAtomic;
        }
    
    // Accessor for the property "BackloggedAtomic"
    /**
     * Getter for property BackloggedAtomic.<p>
    * Array of AtomicLong objects used as a bitset to track the MessageBus
    * backlog state of the members.
    * 
    * For member set held by the ServiceInfo this property is not used.
     */
    protected java.util.concurrent.atomic.AtomicLong getBackloggedAtomic(int i)
        {
        // import java.util.concurrent.atomic.AtomicLong;
        
        AtomicLong[] aBits = getBackloggedAtomic();
        
        if (aBits == null || i >= aBits.length)
            {
            synchronized (this)
                {
                // double check
                aBits = getBackloggedAtomic();
                if (aBits == null || i >= aBits.length)
                    {
                    // grow
                    AtomicLong[] aBitsNew = new AtomicLong[i + 8];
        
                    // copy original data
                    if (aBits != null)
                        {
                        System.arraycopy(aBits, 0, aBitsNew, 0, aBits.length);
                        }
        
                    // fill in new slots with Atomics
                    for (int j = aBits == null ? 0 : aBits.length; j < aBitsNew.length; ++j)
                        {
                        aBitsNew[j] = new AtomicLong();
                        }
        
                    // store array
                    aBits = aBitsNew;
                    setBackloggedAtomic(aBits);
                    }
                }
            }
        
        return aBits[i];
        }
    
    public String getDescription()
        {
        // import Component.Net.Member;
        // import com.tangosol.util.Base;
        // import java.util.Iterator as java.util.Iterator;
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n  OldestMember="    + getOldestMember())
          .append("\n  ActualMemberSet=" + Base.indentString(toString(Member.SHOW_STD), "  ", false))
          .append("\n  MemberId|ServiceJoined|MemberState");
        
        appendTransportHeader(sb);
        appendVersionHeader(sb);
        appendEditionHeader(sb);
        
        boolean fFirst = true;
        for (java.util.Iterator iter = iterator(); iter.hasNext(); )
            {
            if (fFirst)
                {
                fFirst = false;
                }
            else
                {
                sb.append(',');
                }
        
            int nMember = ((Member) iter.next()).getId();
            sb.append("\n    ")
              .append(nMember)
              .append('|')
              .append(formatJoinTime(getServiceJoinTime(nMember)))
              .append('|')
              .append(formatStateName(getState(nMember)));
        
            appendTransport(sb, nMember);
            appendVersion(sb, nMember);
            appendEdition(sb, nMember);
            }
        
        return sb.toString();
        }
    
    /**
     * Return the service member uniquely identified by the specified join-time,
    * or null if no such member exists.
     */
    public com.tangosol.coherence.component.net.Member getJoinedMember(long ldtJoined)
        {
        long[] aldtJoined = getServiceJoinTime();
        for (int i = 0, c = aldtJoined.length; i < c; i++)
            {
            if (aldtJoined[i] == ldtJoined)
                {
                return getMember(i);
                }
            }
        
        return null;
        }
    
    // Accessor for the property "LastJoinTime"
    /**
     * Getter for property LastJoinTime.<p>
    * The ServiceJoinTime of the youngest member to have been added to the
    * MemberSet.
     */
    public long getLastJoinTime()
        {
        return __m_LastJoinTime;
        }
    
    // Accessor for the property "MemberConfigMap"
    /**
     * Getter for property MemberConfigMap.<p>
    * A map of configuration data local to the Service and specific to the
    * Member. This data is maintained by (and mutable only by) the Member that
    * it represents.
     */
    protected com.tangosol.util.ObservableMap[] getMemberConfigMap()
        {
        return __m_MemberConfigMap;
        }
    
    // Accessor for the property "MemberConfigMap"
    /**
     * Getter for property MemberConfigMap.<p>
    * A map of configuration data local to the Service and specific to the
    * Member. This data is maintained by (and mutable only by) the Member that
    * it represents.
     */
    public com.tangosol.util.ObservableMap getMemberConfigMap(int i)
        {
        // import com.tangosol.util.ObservableMap;
        // import com.tangosol.util.NullImplementation;
        
        ObservableMap[] amap = getMemberConfigMap();
        ObservableMap   map  = (amap == null || i >= amap.length ? null : amap[i]);
        
        if (map == null)
            {
            map = NullImplementation.getObservableMap();
            }
        
        return map;
        }
    
    // Accessor for the property "OldestLocalMember"
    /**
     * Getter for property OldestLocalMember.<p>
    * The ServiceMemberSet keeps track of the "most senior" Member colocated
    * with this member that is running the Service, to which special
    * synchronization tasks can be delegated.
     */
    public com.tangosol.coherence.component.net.Member getOldestLocalMember()
        {
        return __m_OldestLocalMember;
        }
    
    // Accessor for the property "OldestMember"
    /**
     * Getter for property OldestMember.<p>
    * The ServiceMemberSet keeps track of the "most senior" Member that is
    * running the Service, to which special synchronization tasks can be
    * delegated.
     */
    public com.tangosol.coherence.component.net.Member getOldestMember()
        {
        return __m_OldestMember;
        }
    
    // Accessor for the property "ServiceEndPoint"
    /**
     * Getter for property ServiceEndPoint.<p>
    * The EndPoint used by each Service member, indexed by Member id. The value
    * of null indicates that the member is using the "datagram" transport.
    * 
    * For member set held by the ServiceInfo this property is not used.
     */
    protected com.oracle.coherence.common.net.exabus.EndPoint[] getServiceEndPoint()
        {
        return __m_ServiceEndPoint;
        }
    
    // Accessor for the property "ServiceEndPoint"
    /**
     * Getter for property ServiceEndPoint.<p>
    * The EndPoint used by each Service member, indexed by Member id. The value
    * of null indicates that the member is using the "datagram" transport.
    * 
    * For member set held by the ServiceInfo this property is not used.
     */
    public com.oracle.coherence.common.net.exabus.EndPoint getServiceEndPoint(int i)
        {
        // import com.oracle.coherence.common.net.exabus.EndPoint;
        
        EndPoint[] aPoint = getServiceEndPoint();
        return aPoint == null || i >= aPoint.length ? null : aPoint[i];
        }
    
    // Accessor for the property "ServiceEndPointName"
    /**
     * Getter for property ServiceEndPointName.<p>
    * The EndPoint name used by each Service member, indexed by Member id. The
    * end point name for the "datagram" transport is an empty string.
    * 
    * For member set held by ServiceInfo holds the advertised EndPoint name. 
    * For the member set held by the service this holds the connected EndPoint
    * name. 
     */
    protected String[] getServiceEndPointName()
        {
        return __m_ServiceEndPointName;
        }
    
    // Accessor for the property "ServiceEndPointName"
    /**
     * Getter for property ServiceEndPointName.<p>
    * The EndPoint name used by each Service member, indexed by Member id. The
    * end point name for the "datagram" transport is an empty string.
    * 
    * For member set held by ServiceInfo holds the advertised EndPoint name. 
    * For the member set held by the service this holds the connected EndPoint
    * name. 
     */
    public String getServiceEndPointName(int i)
        {
        String[] asEndPoint = getServiceEndPointName();
        return asEndPoint == null || i >= asEndPoint.length ? null : asEndPoint[i];
        }
    
    // Accessor for the property "ServiceId"
    /**
     * Getter for property ServiceId.<p>
    * The ServiceId.
    * 
    * @see ServiceInfo#setServiceId
     */
    public int getServiceId()
        {
        return __m_ServiceId;
        }
    
    // Accessor for the property "ServiceJoinTime"
    /**
     * Getter for property ServiceJoinTime.<p>
    * The cluster time that each Member joined the Service, indexed by Member
    * id.
     */
    protected long[] getServiceJoinTime()
        {
        return __m_ServiceJoinTime;
        }
    
    // Accessor for the property "ServiceJoinTime"
    /**
     * Getter for property ServiceJoinTime.<p>
    * The cluster time that each Member joined the Service, indexed by Member
    * id.
     */
    public long getServiceJoinTime(int i)
        {
        long[] alMillis = getServiceJoinTime();
        return alMillis == null || i >= alMillis.length ? 0L : alMillis[i];
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Getter for property ServiceName.<p>
    * The ServiceName.
    * 
    * @see com.tangosol.coherence.component.net.ServiceInfo#setServiceName
     */
    public String getServiceName()
        {
        return __m_ServiceName;
        }

    /**
     * Return {@code true} if all members have the same version.
     *
     * @return {@code true} if all members have the same version
     */
    public boolean isVersionConsistent()
        {
        return m_nVersionMax == m_nVersionMin;
        }

    /**
     * Return the minimum version for the members in this set.
     *
     * @return the minimum version for the members in this set
     */
    public  int getMinimumVersion()
        {
        return m_nVersionMin;
        }

    /**
     * Return {@code true} if all members are running a version that is greater than
     * or equal to the specified version.
     *
     * @return {@code true} if all members are running a version that is greater than
     *          or equal to the specified version
     */
    public boolean isVersionCompatible(int nVersion)
        {
        return m_nVersionMin >= nVersion;
        }

    /**
     * Return {@code true} if all members are running a version that is greater than
     * or equal to the specified version.
     *
     * @param predicate  an {@link IntPredicate} to use to apply to the minumum service version
     *
     * @return {@code true} if all members are running a version that is greater than
     *          or equal to the specified version
     */
    public boolean isVersionCompatible(IntPredicate predicate)
        {
        return predicate.test(m_nVersionMin);
        }

    /**
     * Determine whether the specified member has a version that is the same as
     * the encode version with the same or a higher patch level.
     *
     * @param nMember          the member id to test
     * @param nEncodedVersion  the encoded required version
     *
     * @return true iff specified member's version is the same version with
     *         the same or a higher patch level to the encoded version
     */
    public boolean isPatchCompatible(int nMember, int nEncodedVersion)
        {
        int nVersion = getServiceVersionInt(nMember);
        return VersionHelper.isPatchCompatible(nEncodedVersion, nVersion);
        }

    /**
     * Determine whether the all members have a version that is the same as
     * the encode version with the same or a higher patch level.
     *
     * @param nEncodedVersion  the encoded required version
     *
     * @return true iff specified member's version is the same version with
     *         the same or a higher patch level to the encoded version
     */
    public boolean isPatchCompatible(int nEncodedVersion)
        {
        return VersionHelper.isPatchCompatible(nEncodedVersion, m_nVersionMin);
        }


    // Accessor for the property "ServiceVersion"
    /**
     * Getter for property ServiceVersion.<p>
    * The Service version string that each Member is running, indexed by Member
    * id.
    * 
    * As of Coherence 12.2.1.1 the string accessors are for bakward
    * compatibility only, while internally the versions are stored as encoded
    * integers. 
    * 
    * @functional
     */
    public String getServiceVersion(int i)
        {
        return toVersionString(getServiceVersionInt(i), /*fIncludePrefix*/ true);
        }
    
    public String getServiceVersionExternal(int nMember)
        {
        // import Component.Net.Member;
        
        Member member = getMember(nMember);
        
        // the following format is encoded with the month and patch number packed
        // into the same int given that the month will always be either
        // 06 or 12
        //   <major>.<minor>.<macro>.YY.MM.<patch>
        
        int nVersion = getServiceVersionInt(nMember);
        int nYear    = (nVersion & 0x00000FC0) >> 6;
        int nPatch   = nVersion  & 0x0000003F;
        
        if (nYear >= 20 && member.getEdition() == 3) // suggests that this member is using a calendar version and is CE
            {
            int nPatchActual = nPatch & ~0x20;
            String sVersion  = nYear + ".";
        
            if ((nPatch & 0x20) == 0)
                {
                // display 6 for CE in years before 22 and 03 for years beyond
                sVersion += (nYear <= 22 ? "06" : "03");
                }
            else
                {
                // display 12 for CE in years before 21 and 09 for years beyond
                sVersion += (nYear <= 21 ? "12" : "09");
                }
        
            return sVersion + "." + nPatchActual;
            }
        
        return getServiceVersion(nMember);
        }
    
    // Accessor for the property "ServiceVersionInt"
    /**
     * Getter for property ServiceVersionInt.<p>
    * The Service version internal integer representation for each running
    * Member, indexed by Member id.
     */
    public int getServiceVersionInt(int i)
        {
        int[] anVersion = __m_ServiceVersionInt;
        return anVersion == null || i >= anVersion.length ? 0 : anVersion[i];
        }
    
    // Accessor for the property "State"
    /**
     * Getter for property State.<p>
    * The current state of the service member (indexed by member id).  The
    * state is one of the MEMBER_* constants.
     */
    protected int[] getState()
        {
        return __m_State;
        }
    
    // Accessor for the property "State"
    /**
     * Getter for property State.<p>
    * The current state of the service member (indexed by member id).  The
    * state is one of the MEMBER_* constants.
     */
    public int getState(int i)
        {
        int[] anState = getState();
        return anState == null || i >= anState.length ? 0 : anState[i];
        }
    
    // Accessor for the property "SuccessorMember"
    /**
     * Getter for property SuccessorMember.<p>
    * Return the next oldest member after this member.
     */
    public com.tangosol.coherence.component.net.Member getSuccessorMember()
        {
        return __m_SuccessorMember;
        }
    
    // Accessor for the property "ThisMember"
    /**
     * Getter for property ThisMember.<p>
    * The local member.  This property is only applicable for the
    * ServiceMemberSet held by the service.
     */
    public com.tangosol.coherence.component.net.Member getThisMember()
        {
        return __m_ThisMember;
        }
    
    /**
     * Convert the version elements into a single integer value.
     */
    public static int getVersionPrefix(int nYear, int nMonth)
        {
        return VersionHelper.getVersionPrefix(nYear, nMonth);
        }
    
    /**
     * Return true if the provided part of the version could represent a year.
     */
    protected static boolean isCalendarVersion(int nVersion)
        {
        return VersionHelper.isCalendarVersion(nVersion);
        }
    
    /**
     * Check whether we received config map from the specified member.
     */
    public boolean isMemberConfigured(int nMember)
        {
        // import com.tangosol.util.ObservableMap;
        
        ObservableMap[] amap = getMemberConfigMap();
        ObservableMap   map  = (amap == null || nMember >= amap.length ? null : amap[nMember]);
        
        return map != null;
        }
    
    // Accessor for the property "ServiceBacklogged"
    /**
     * Getter for property ServiceBacklogged.<p>
    * Calculated property indicating if a given member's EndPoint is backlogged.
     */
    public boolean isServiceBacklogged(int i)
        {
        return (getBackloggedAtomic(i / 64).get() & (1L << (i % 64))) != 0;
        }
    
    /**
     * Is the specified Member is already known to have finished joining the
    * Service.
     */
    public boolean isServiceJoined(int nMember)
        {
        return getState(nMember) == MEMBER_JOINED;
        }
    
    /**
     * Is the specified Member in the process of joining the Service.
     */
    public boolean isServiceJoining(int nMember)
        {
        return getState(nMember) == MEMBER_JOINING;
        }
    
    /**
     * Is the specified Member in the process of leaving the Service.
     */
    public boolean isServiceLeaving(int nMember)
        {
        return getState(nMember) == MEMBER_LEAVING;
        }
    
    /**
     * Parse the specified version string and generate an internal integer
    * representation.
     */
    public static int parseVersion(String sVersion)
        {
        return VersionHelper.parseVersion(sVersion);
        }
    
    // Declared at the super level
    public synchronized boolean remove(Object o)
        {
        // import Component.Net.Member;
        // import java.util.Iterator as java.util.Iterator;
        
        if (super.remove(o))
            {
            Member memberLeft = (Member) o;
            int    nId        = memberLeft.getId();
        
            setServiceVersion   (nId, null);
            setServiceJoinTime  (nId, 0L);
            setServiceEndPoint  (nId, null);
            setServiceBacklogged(nId, false);
            setMemberConfigMap  (nId, null);
            setState            (nId, 0);
        
            if (memberLeft == getOldestMember()      ||
                memberLeft == getOldestLocalMember() ||
                memberLeft == getSuccessorMember())
                {
                // find next-oldest member
                Member memberThis        = getThisMember();
                Member memberOldest      = null;
                Member memberOldestLocal = memberThis;
                Member memberSuccessor   = null;
        
                for (java.util.Iterator iter = iterator(); iter.hasNext(); )
                    {
                    Member member = (Member) iter.next();
        
                    if (memberOldest == null || compareSeniority(member, memberOldest) < 0L)
                        {
                        memberOldest = member;
                        }
        
                    if (memberOldestLocal != null && memberOldestLocal.isCollocated(member) &&
                        (compareSeniority(member, memberOldestLocal) < 0L))
                        {
                        memberOldestLocal = member;
                        }
        
                    if (memberThis != null && compareSeniority(memberThis, member) < 0L &&
                        (memberSuccessor == null || compareSeniority(member, memberSuccessor) < 0L))
                        {
                        memberSuccessor = member;
                        }
                    }
        
                setOldestMember(memberOldest);
                setOldestLocalMember(memberOldestLocal);
                setSuccessorMember(memberSuccessor);
                }
        
            return true;
            }
        else
            {
            return false;
            }
        }
    
    // Accessor for the property "BackloggedAtomic"
    /**
     * Setter for property BackloggedAtomic.<p>
    * Array of AtomicLong objects used as a bitset to track the MessageBus
    * backlog state of the members.
    * 
    * For member set held by the ServiceInfo this property is not used.
     */
    protected void setBackloggedAtomic(java.util.concurrent.atomic.AtomicLong[] aPoint)
        {
        __m_BackloggedAtomic = aPoint;
        }
    
    // Accessor for the property "BackloggedAtomic"
    /**
     * Setter for property BackloggedAtomic.<p>
    * Array of AtomicLong objects used as a bitset to track the MessageBus
    * backlog state of the members.
    * 
    * For member set held by the ServiceInfo this property is not used.
     */
    protected void setBackloggedAtomic(int i, java.util.concurrent.atomic.AtomicLong point)
        {
        getBackloggedAtomic()[i] = point;
        }
    
    // Accessor for the property "LastJoinTime"
    /**
     * Setter for property LastJoinTime.<p>
    * The ServiceJoinTime of the youngest member to have been added to the
    * MemberSet.
     */
    protected void setLastJoinTime(long ldt)
        {
        __m_LastJoinTime = ldt;
        }
    
    // Accessor for the property "MemberConfigMap"
    /**
     * Setter for property MemberConfigMap.<p>
    * A map of configuration data local to the Service and specific to the
    * Member. This data is maintained by (and mutable only by) the Member that
    * it represents.
     */
    protected void setMemberConfigMap(com.tangosol.util.ObservableMap[] amap)
        {
        __m_MemberConfigMap = amap;
        }
    
    // Accessor for the property "MemberConfigMap"
    /**
     * Setter for property MemberConfigMap.<p>
    * A map of configuration data local to the Service and specific to the
    * Member. This data is maintained by (and mutable only by) the Member that
    * it represents.
     */
    protected synchronized void setMemberConfigMap(int i, com.tangosol.util.ObservableMap map)
        {
        // import com.tangosol.util.ObservableMap;
        
        ObservableMap[] amap = getMemberConfigMap();
        
        // resize if storing non-null beyond bounds
        boolean fBeyondBounds = (amap == null || i >= amap.length);
        if (map != null && fBeyondBounds)
            {
            // resize
            ObservableMap[] amapNew = new ObservableMap[i + 8];
        
            // copy original data
            if (amap != null)
                {
                System.arraycopy(amap, 0, amapNew, 0, amap.length);
                }
        
            // store array
            amap = amapNew;
            setMemberConfigMap(amap);
        
            fBeyondBounds = false;
            }
        
        if (!fBeyondBounds)
            {
            amap[i] = map;
            }
        }
    
    // Accessor for the property "OldestLocalMember"
    /**
     * Setter for property OldestLocalMember.<p>
    * The ServiceMemberSet keeps track of the "most senior" Member colocated
    * with this member that is running the Service, to which special
    * synchronization tasks can be delegated.
     */
    protected void setOldestLocalMember(com.tangosol.coherence.component.net.Member member)
        {
        __m_OldestLocalMember = member;
        }
    
    // Accessor for the property "OldestMember"
    /**
     * Setter for property OldestMember.<p>
    * The ServiceMemberSet keeps track of the "most senior" Member that is
    * running the Service, to which special synchronization tasks can be
    * delegated.
     */
    protected void setOldestMember(com.tangosol.coherence.component.net.Member member)
        {
        __m_OldestMember = member;
        }
    
    // Accessor for the property "ServiceBacklogged"
    /**
     * Setter for property ServiceBacklogged.<p>
    * Calculated property indicating if a given member's EndPoint is backlogged.
     */
    public void setServiceBacklogged(int i, boolean fBacklogged)
        {
        // import java.util.concurrent.atomic.AtomicLong;
        
        AtomicLong atl  = getBackloggedAtomic(i / 64);
        long       iBit = 1L << (i % 64);
        long       lCur;
        do
            {
            lCur = atl.get();
            }
        while (!atl.compareAndSet(lCur, fBacklogged ? lCur | iBit : lCur & ~iBit));
        }
    
    // Accessor for the property "ServiceEndPoint"
    /**
     * Setter for property ServiceEndPoint.<p>
    * The EndPoint used by each Service member, indexed by Member id. The value
    * of null indicates that the member is using the "datagram" transport.
    * 
    * For member set held by the ServiceInfo this property is not used.
     */
    protected void setServiceEndPoint(com.oracle.coherence.common.net.exabus.EndPoint[] aPoint)
        {
        __m_ServiceEndPoint = aPoint;
        }
    
    // Accessor for the property "ServiceEndPoint"
    /**
     * Setter for property ServiceEndPoint.<p>
    * The EndPoint used by each Service member, indexed by Member id. The value
    * of null indicates that the member is using the "datagram" transport.
    * 
    * For member set held by the ServiceInfo this property is not used.
     */
    public void setServiceEndPoint(int i, com.oracle.coherence.common.net.exabus.EndPoint point)
        {
        // import com.oracle.coherence.common.net.exabus.EndPoint;
        
        EndPoint[] aPoint = getServiceEndPoint();
        
        // resize if storing non-null beyond bounds
        boolean fBeyondBounds = (aPoint == null || i >= aPoint.length);
        if (point != null && fBeyondBounds)
            {
            // resize
            EndPoint[] aPointNew = new EndPoint[i + 8];
        
            // copy original data
            if (aPoint != null)
                {
                System.arraycopy(aPoint, 0, aPointNew, 0, aPoint.length);
                }
        
            // store array
            aPoint = aPointNew;
            setServiceEndPoint(aPoint);
        
            fBeyondBounds = false;
            }
        
        if (!fBeyondBounds)
            {
            aPoint[i] = point;
            setServiceBacklogged(i, /*fExcessive*/ false);
            }
        }
    
    // Accessor for the property "ServiceEndPointName"
    /**
     * Setter for property ServiceEndPointName.<p>
    * The EndPoint name used by each Service member, indexed by Member id. The
    * end point name for the "datagram" transport is an empty string.
    * 
    * For member set held by ServiceInfo holds the advertised EndPoint name. 
    * For the member set held by the service this holds the connected EndPoint
    * name. 
     */
    protected void setServiceEndPointName(String[] asName)
        {
        __m_ServiceEndPointName = asName;
        }
    
    // Accessor for the property "ServiceEndPointName"
    /**
     * Setter for property ServiceEndPointName.<p>
    * The EndPoint name used by each Service member, indexed by Member id. The
    * end point name for the "datagram" transport is an empty string.
    * 
    * For member set held by ServiceInfo holds the advertised EndPoint name. 
    * For the member set held by the service this holds the connected EndPoint
    * name. 
     */
    public void setServiceEndPointName(int i, String sEndPoint)
        {
        String[] asEndPoint = getServiceEndPointName();
        
        // resize if storing non-null beyond bounds
        boolean fBeyondBounds = (asEndPoint == null || i >= asEndPoint.length);
        if (sEndPoint != null && fBeyondBounds)
            {
            // resize
            String[] asEndPointNew = new String[i + 8];
        
            // copy original data
            if (asEndPoint != null)
                {
                System.arraycopy(asEndPoint, 0, asEndPointNew, 0, asEndPoint.length);
                }
        
            // store array
            asEndPoint = asEndPointNew;
            setServiceEndPointName(asEndPoint);
        
            fBeyondBounds = false;
            }
        
        if (!fBeyondBounds)
            {
            asEndPoint[i] = sEndPoint;
            }
        }
    
    // Accessor for the property "ServiceId"
    /**
     * Setter for property ServiceId.<p>
    * The ServiceId.
    * 
    * @see ServiceInfo#setServiceId
     */
    public void setServiceId(int nId)
        {
        __m_ServiceId = nId;
        }
    
    /**
     * Set the membership state to reflect that the specified member Member has
    * finished joining the Service.
     */
    public void setServiceJoined(int nMember)
        {
        setState(nMember, MEMBER_JOINED);
        }
    
    /**
     * Set the membership state to reflect that the specified member Member is
    * joining the Service.
     */
    public void setServiceJoining(int nMember)
        {
        setState(nMember, MEMBER_JOINING);
        }
    
    // Accessor for the property "ServiceJoinTime"
    /**
     * Setter for property ServiceJoinTime.<p>
    * The cluster time that each Member joined the Service, indexed by Member
    * id.
     */
    protected void setServiceJoinTime(long[] alMillis)
        {
        __m_ServiceJoinTime = alMillis;
        }
    
    // Accessor for the property "ServiceJoinTime"
    /**
     * Setter for property ServiceJoinTime.<p>
    * The cluster time that each Member joined the Service, indexed by Member
    * id.
     */
    public synchronized void setServiceJoinTime(int i, long lMillis)
        {
        // import Component.Net.Member;
        
        if (lMillis > getLastJoinTime())
            {
            setLastJoinTime(lMillis);
            }
        
        long[] alMillis = getServiceJoinTime();
        
        // resize if storing non-null beyond bounds
        boolean fBeyondBounds = (alMillis == null || i >= alMillis.length);
        if (lMillis != 0L && fBeyondBounds)
            {
            // resize
            long[] alMillisNew = new long[i + 8];
        
            // copy original data
            if (alMillis != null)
                {
                System.arraycopy(alMillis, 0, alMillisNew, 0, alMillis.length);
                }
        
            // store array
            alMillis = alMillisNew;
            setServiceJoinTime(alMillis);
        
            fBeyondBounds = false;
            }
        
        if (!fBeyondBounds)
            {
            alMillis[i] = lMillis;
        
            Member member = getMember(i);
            if (member != null)
                {
                Member memberOldest = getOldestMember();
                if (memberOldest == null || compareSeniority(member, memberOldest) < 0L)
                    {
                    setOldestMember(member);
                    }
        
                memberOldest = getOldestLocalMember();
                if (memberOldest != null && memberOldest.isCollocated(member) &&
                    compareSeniority(member, memberOldest) < 0L)
                    {
                    setOldestLocalMember(member);
                    }
        
                memberOldest = getSuccessorMember();
                Member memberThis = getThisMember();
                if ((memberThis  != null  && compareSeniority(memberThis, member)   < 0L) &&
                    (memberOldest == null || compareSeniority(member, memberOldest) < 0L))
                    {
                    setSuccessorMember(member);
                    }
                }
            // else; part of remove
            }
        }
    
    /**
     * Set the membership state to reflect that the specified member Member is
    * leaving the Service.
     */
    public void setServiceLeaving(int nMember)
        {
        setState(nMember, MEMBER_LEAVING);
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Setter for property ServiceName.<p>
    * The ServiceName.
    * 
    * @see ServiceInfo#setServiceName
     */
    public void setServiceName(String sName)
        {
        __m_ServiceName = sName;
        }
    
    // Accessor for the property "ServiceVersion"
    /**
     * Setter for property ServiceVersion.<p>
    * The Service version string that each Member is running, indexed by Member
    * id.
    * 
    * As of Coherence 12.2.1.1 the string accessors are for bakward
    * compatibility only, while internally the versions are stored as encoded
    * integers. 
    * 
    * @functional
     */
    public void setServiceVersion(int i, String sVersion)
        {
        setServiceVersionInt(i, parseVersion(sVersion));
        }
    
    // Accessor for the property "ServiceVersionInt"
    /**
     * Setter for property ServiceVersionInt.<p>
    * The Service version internal integer representation for each running
    * Member, indexed by Member id.
     */
    private synchronized void setServiceVersionInt(int i, int nVersion)
        {
        int[] anVersion = __m_ServiceVersionInt;
        
        // resize if storing non-null beyond bounds
        boolean fBeyondBounds = (anVersion == null || i >= anVersion.length);
        if (nVersion != 0 && fBeyondBounds)
            {
            // resize
            int[] anVersionNew = new int[i + 8];
        
            // copy original data
            if (anVersion != null)
                {
                System.arraycopy(anVersion, 0, anVersionNew, 0, anVersion.length);
                }
        
            // store array
            anVersion = anVersionNew;
            __m_ServiceVersionInt = anVersion;
            fBeyondBounds = false;
            }
        
        if (!fBeyondBounds)
            {
            anVersion[i] = nVersion;
            }

        if (anVersion != null)
            {
            int nMin = Integer.MAX_VALUE;
            int nMax = Integer.MIN_VALUE;
            for (int n : anVersion)
                {
                if (n != 0)
                    {
                    nMin = Math.min(n, nMin);
                    nMax = Math.max(n, nMax);
                    }
                }
            m_nVersionMin = nMin;
            m_nVersionMax = nMax;
            }
        }
    
    // Accessor for the property "State"
    /**
     * Setter for property State.<p>
    * The current state of the service member (indexed by member id).  The
    * state is one of the MEMBER_* constants.
     */
    protected void setState(int[] aiState)
        {
        __m_State = aiState;
        }
    
    // Accessor for the property "State"
    /**
     * Setter for property State.<p>
    * The current state of the service member (indexed by member id).  The
    * state is one of the MEMBER_* constants.
     */
    public synchronized void setState(int i, int nState)
        {
        int[] anState = getState();
        
        if (anState == null || i >= anState.length)
            {
            // resize
            int[] anStateNew = new int[i + 8];
        
            // copy original data
            if (anState != null)
                {
                System.arraycopy(anState, 0, anStateNew, 0, anState.length);
                }
        
            // store array
            anState = anStateNew;
            setState(anState);
            }
        
        anState[i] = nState;
        }
    
    // Accessor for the property "SuccessorMember"
    /**
     * Setter for property SuccessorMember.<p>
    * Return the next oldest member after this member.
     */
    public void setSuccessorMember(com.tangosol.coherence.component.net.Member memberSuccessor)
        {
        __m_SuccessorMember = memberSuccessor;
        }
    
    // Accessor for the property "ThisMember"
    /**
     * Setter for property ThisMember.<p>
    * The local member.  This property is only applicable for the
    * ServiceMemberSet held by the service.
     */
    public void setThisMember(com.tangosol.coherence.component.net.Member memberThis)
        {
        // import Component.Net.Member;
        // import java.util.Iterator as java.util.Iterator;
        
        _assert(memberThis != null && memberThis.getId() != 0);
        _assert(getThisMember() == null || memberThis == getThisMember());
        _assert(contains(memberThis));
        
        __m_ThisMember = (memberThis);
        
        // find oldest local, and successor member
        Member memberOldestLocal = memberThis;
        Member memberSuccessor   = null;
        for (java.util.Iterator iter = iterator(); iter.hasNext(); )
            {
            Member member = (Member) iter.next();
            if (memberThis.isCollocated(member) && compareSeniority(member, memberOldestLocal) < 0L)
                {
                memberOldestLocal = member;
                }
        
            if (compareSeniority(memberThis, member) < 0L &&
                (memberSuccessor == null || compareSeniority(member, memberSuccessor) < 0L))
                {
                memberSuccessor = member;
                }
            }
        
        setOldestLocalMember(memberOldestLocal);
        setSuccessorMember(memberSuccessor);
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + '(' + getDescription() + "\n  )";
        }
    
    /**
     * Create an array of version elements, where index 0 is "major", 1 is
    * "minor", ... 4 is "patch".
    * 
    * @see #toVersionString
     */
    public static int[] toVersionArray(String sVersion)
        {
        return VersionHelper.toVersionArray(sVersion);
        }
    
    /**
     * Create a string representation of the specified version in internal
    * encoding.
    * 
    * @see #encodeVersion
     */
    protected static String toVersionString(int nVersion, boolean fIncludePrefix)
        {
        return VersionHelper.toVersionString(nVersion, fIncludePrefix);
        }
    
    /**
     * Update member config map.
    * 
    * @param nMember the member id
    * @param map the member config map for the service
     */
    public void updateMemberConfigMap(int nMember, java.util.Map map)
        {
        // import java.util.Map;
        
        if (map != null)
            {
            Map mapConfig = ensureMemberConfigMap(nMember);
        
            // store the new Member's config state
            if (mapConfig != null)
                {
                mapConfig.clear();
                mapConfig.putAll(map);
                }
            }
        }
    }
