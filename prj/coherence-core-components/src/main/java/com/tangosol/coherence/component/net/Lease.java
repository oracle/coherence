
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.Lease

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.WrapperException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Lease represents an expirable exclusive posession of a resource shared
 * across a cluster. None of the property accessors are synchronized and
 * usually require extrenal synchronization. The synchronized methods are
 * <code>lock(), validate(), copyFrom(Lease)</code>.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Lease
        extends    com.tangosol.coherence.component.Net
        implements Cloneable
    {
    // ---- Fields declarations ----
    
    /**
     * Property BY_MEMBER
     *
     * Indicates a Member based lease ownership.
     */
    public static final int BY_MEMBER = 1;
    
    /**
     * Property BY_THREAD
     *
     * Indicates a Thread (per Member) based lease ownership.
     */
    public static final int BY_THREAD = 0;
    
    /**
     * Property CacheIndex
     *
     * The index of the cache that this Lease belongs to. This index is used by
     * the cache service to get to an appropriate cache [handler].
     */
    private int __m_CacheIndex;
    
    /**
     * Property ClassLoader
     *
     * The ClassLoader that the corresponding resource is known to be
     * assosiated with.
     */
    private transient ClassLoader __m_ClassLoader;
    
    /**
     * Property EffectiveTime
     *
     * Cluster time that the Lease was locked at.
     */
    private long __m_EffectiveTime;
    
    /**
     * Property ExpirationTime
     *
     * Cluster time that the Lease expires at.
     */
    private long __m_ExpirationTime;
    
    /**
     * Property HolderId
     *
     * Member id of the holder for this Lease. Value of 0 indicates that
     * currently there is no holder for this Lease.
     */
    private transient int __m_HolderId;
    
    /**
     * Property HolderThreadId
     *
     * Unique id of the Thread which holds a lock for this Lease. This property
     * has meaning only if the following holds true:
     * <pre><code>
     *     getHolderId() == getService().getThisMember().getId()
     * </code></pre>
     */
    private long __m_HolderThreadId;
    
    /**
     * Property IssuerId
     *
     * Member id of the issuer (registrar) for this Lease. Value of 0 indicates
     * that currently there is no issuer for this Lease.
     */
    private transient int __m_IssuerId;
    
    /**
     * Property LEASE_AVAILABLE
     *
     * Indicates that a resource is known to be available.
     */
    public static final int LEASE_AVAILABLE = 2;
    
    /**
     * Property LEASE_DIRTY
     *
     * Indicates that another Member of the Cluster currently holds a Lease for
     * a resource.
     */
    public static final int LEASE_DIRTY = 4;
    
    /**
     * Property LEASE_LOCKED
     *
     * Indicates that this Member of the Cluster currently holds a Lease for a
     * resource.
     */
    public static final int LEASE_LOCKED = 3;
    
    /**
     * Property LEASE_UNISSUED
     *
     * Indicates that the Lease issuer is gone.
     */
    public static final int LEASE_UNISSUED = 1;
    
    /**
     * Property LEASE_UNKNOWN
     *
     * Indicates that there is no known Lease for a resource.
     */
    public static final int LEASE_UNKNOWN = 0;
    
    /**
     * Property LeaseVersion
     *
     * The version of the Lease. It is intended to be used to resolve
     * simultaneous conflicting requests.
     * 
     * The value of LeaseVersion is in a range of 0..255, where value of zero
     * represents a not existing lease and value of one represents a newly
     * inserted lease.
     */
    private int __m_LeaseVersion;
    
    /**
     * Property ResourceKey
     *
     * Key for the resource represented by this Lease. This property is set
     * during initialization only.
     * 
     * @see #instantiate()
     */
    private Object __m_ResourceKey;
    
    /**
     * Property ResourceSize
     *
     * The size of the (serialized) resource represented by this Lease in
     * bytes. It is inteneded to be used by the cache implementations that have
     * automatic purge strategies that are based on the resource "weight".
     * This property is calculated asynchronously and is not guaranteed to
     * carry the precise value at all times. The value of -1 indicates that the
     * resource has not yet been deserialized.
     */
    private int __m_ResourceSize;
    
    /**
     * Property ResourceVersion
     *
     * The version of the resource represented by this Lease. It is intended to
     * be used in the optimistic scenarios that do not "lock" prior to the
     * resource updates, but instead discard the "outdated" update requests.
     * 
     * The value of ResourceVersion is in a range of 0..255, where value of
     * zero represents a not existing resource and value of one represents a
     * newly inserted resource.
     */
    private int __m_ResourceVersion;
    
    /**
     * Property Service
     *
     * Service object handling this Lease. This property is set during
     * initialization only.
     * 
     * @see #instantiateLease()
     */
    private transient com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid __m_Service;
    
    /**
     * Property ThreadIdCounter
     *
     * Atomic counter used to assign unique thread identifiers.
     */
    private static transient java.util.concurrent.atomic.AtomicLong __s_ThreadIdCounter;
    
    /**
     * Property ThreadIdHolder
     *
     * ThreadLocal object holding unique thread identifiers.
     */
    private static transient ThreadLocal __s_ThreadIdHolder;
    
    private static void _initStatic$Default()
        {
        __initStatic();
        }
    
    // Static initializer (from _initStatic)
    static
        {
        // import java.util.concurrent.atomic.AtomicLong;
        
        _initStatic$Default();
        
        setThreadIdCounter(new AtomicLong());
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // state initialization: static properties
        try
            {
            __s_ThreadIdHolder = new java.lang.ThreadLocal();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    // Default constructor
    public Lease()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Lease(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setCacheIndex(-1);
            setResourceSize(-1);
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
        return new com.tangosol.coherence.component.net.Lease();
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
            clz = Class.forName("com.tangosol.coherence/component/net/Lease".replace('/', '.'));
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
    
    // Declared at the super level
    public synchronized Object clone()
        {
        // import com.tangosol.util.WrapperException;
        
        try
            {
            return super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw new WrapperException(e);
            }
        }
    
    // Declared at the super level
    /**
     * Compares this Lease object with the specified Lease object with an intent
    * to figure out which lease is more "up-to-date". Returns a negative
    * integer, zero, or a positive integer if this Lease is outdated, equally
    * dated, or newer than the specified Lease.
    * 
    * @param o  the Lease object to be compared.
    * @return  a negative integer, zero, or a positive integer as this Lease
    * information is older than, equal to, or newer than the specified lease.
    * 
    * @throws ClassCastException if the specified object's type prevents it
    * from being compared to this Lease.
     */
    public int compareTo(Object o)
        {
        if (o == this)
            {
            return 0;
            }
        
        Lease leaseThis = this;
        Lease leaseThat = (Lease) o;
        
        // versions are circular (2 .. 128 .. 255)
        // 0 represents a not existing lease or resource
        // 1 represents a newly inserted lease or resource
        
        int nVersionThis = leaseThis.getLeaseVersion();
        int nVersionThat = leaseThat.getLeaseVersion();
        
        if (nVersionThis == nVersionThat) // commented out in build 59: || nVersionThis == 1 || nVersionThat == 1)
            {
            nVersionThis = leaseThis.getResourceVersion();
            nVersionThat = leaseThat.getResourceVersion();
        
            if (nVersionThis == nVersionThat)
                {
                long lTimeThis = leaseThis.getEffectiveTime();
                long lTimeThat = leaseThat.getEffectiveTime();
        
                return lTimeThis == lTimeThat ?  0 :
                       lTimeThis >  lTimeThat ? +1 : -1;
                }
            }
        
        if (nVersionThis > nVersionThat)
            {
            return (nVersionThat == 0 || nVersionThis - nVersionThat < 128) ? +1 : -1;
            }
        else
            {
            return (nVersionThis == 0 || nVersionThat - nVersionThis < 128) ? -1 : +1;
            }
        }
    
    /**
     * Copy the lease data from the specified Lease.
    * 
    * @param lease  the Lease object to copy the data from
     */
    public synchronized void copyFrom(Lease lease)
        {
        // import com.tangosol.util.Base;
        
        if (lease != this)
            {
            _assert(Base.equals(this.getResourceKey(), lease.getResourceKey()));
            _assert(getCacheIndex() == lease.getCacheIndex());
        
            setIssuerId      (lease.getIssuerId());
            setHolderId      (lease.getHolderId());
            setHolderThreadId(lease.getHolderThreadId());
            setEffectiveTime (lease.getEffectiveTime());
            setExpirationTime(lease.getExpirationTime());
        
            // the ResourceSize may not be there
            int cbSize = lease.getResourceSize();
            if (cbSize >= 0)
                {
                setResourceSize(cbSize);
                }
        
            copyVersion(lease);
            }
        notifyAll();
        }
    
    /**
     * Copy the lease version from the specified Lease.
    * 
    * @param lease  the Lease object to copy the version info from
     */
    public void copyVersion(Lease lease)
        {
        // the LeaseVersion may not be there
        int nVersion = lease.getLeaseVersion();
        if (nVersion > 0)
            {
            setLeaseVersion(nVersion);
            }
        
        // the ResourceVersion may not be there
        nVersion = lease.getResourceVersion();
        if (nVersion > 0)
            {
            setResourceVersion(nVersion);
            }
        }
    
    /**
     * Helper method used for reporting.
     */
    public static String formatCacheName(int iCache, com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ReplicatedCache;
        // import com.tangosol.net.NamedCache;
        
        NamedCache cache = null;
        if (service instanceof ReplicatedCache)
            {
            cache = ((ReplicatedCache) service).getCacheHandler(iCache);
            }
        else if (service instanceof NamedCache)
            {
            cache = (NamedCache) service;
            }
        
        return cache == null ?
            service.getServiceName() + "[" + iCache + "]" : cache.getCacheName();
        }
    
    public static String formatStatusName(int nStatus)
        {
        switch (nStatus)
            {
            case LEASE_UNKNOWN:
                return "LEASE_UNKNOWN";
            case LEASE_UNISSUED:
                return "LEASE_UNISSUED";
            case LEASE_AVAILABLE:
                return "LEASE_AVAILABLE";
            case LEASE_LOCKED:
                return "LEASE_LOCKED";
            case LEASE_DIRTY:
                return "LEASE_DIRTY";
            default:
                return "<invalid>";
            }
        }
    
    // Accessor for the property "CacheIndex"
    /**
     * Getter for property CacheIndex.<p>
    * The index of the cache that this Lease belongs to. This index is used by
    * the cache service to get to an appropriate cache [handler].
     */
    public int getCacheIndex()
        {
        return __m_CacheIndex;
        }
    
    // Accessor for the property "ClassLoader"
    /**
     * Getter for property ClassLoader.<p>
    * The ClassLoader that the corresponding resource is known to be assosiated
    * with.
     */
    public ClassLoader getClassLoader()
        {
        return __m_ClassLoader;
        }
    
    // Accessor for the property "CurrentThreadId"
    /**
     * Getter for property CurrentThreadId.<p>
    * (Calculated) Helper property that (unlike the System.identityHashcode)
    * provides a unique id for a current thread.
     */
    public static long getCurrentThreadId()
        {
        // import com.tangosol.util.Base;
        
        // TODO: When we switch to JDK 1.5, this will become trivial:
        //    return Thread.currentThread().getId();
        
        ThreadLocal tlo = getThreadIdHolder();
        Long        Id  = (Long) tlo.get();
        if (Id == null)
            {
            long lId = getThreadIdCounter().incrementAndGet();
            tlo.set(Base.makeLong(lId));
            return lId;
            }
        else
            {
            return Id.longValue();
            }
        }
    
    // Accessor for the property "EffectiveTime"
    /**
     * Getter for property EffectiveTime.<p>
    * Cluster time that the Lease was locked at.
     */
    public long getEffectiveTime()
        {
        return __m_EffectiveTime;
        }
    
    // Accessor for the property "ExpirationTime"
    /**
     * Getter for property ExpirationTime.<p>
    * Cluster time that the Lease expires at.
     */
    public long getExpirationTime()
        {
        return __m_ExpirationTime;
        }
    
    // Accessor for the property "HolderId"
    /**
     * Getter for property HolderId.<p>
    * Member id of the holder for this Lease. Value of 0 indicates that
    * currently there is no holder for this Lease.
     */
    public int getHolderId()
        {
        return __m_HolderId;
        }
    
    // Accessor for the property "HolderThreadId"
    /**
     * Getter for property HolderThreadId.<p>
    * Unique id of the Thread which holds a lock for this Lease. This property
    * has meaning only if the following holds true:
    * <pre><code>
    *     getHolderId() == getService().getThisMember().getId()
    * </code></pre>
     */
    public long getHolderThreadId()
        {
        return __m_HolderThreadId;
        }
    
    // Accessor for the property "IssuerId"
    /**
     * Getter for property IssuerId.<p>
    * Member id of the issuer (registrar) for this Lease. Value of 0 indicates
    * that currently there is no issuer for this Lease.
     */
    public int getIssuerId()
        {
        return __m_IssuerId;
        }
    
    // Accessor for the property "LeaseVersion"
    /**
     * Getter for property LeaseVersion.<p>
    * The version of the Lease. It is intended to be used to resolve
    * simultaneous conflicting requests.
    * 
    * The value of LeaseVersion is in a range of 0..255, where value of zero
    * represents a not existing lease and value of one represents a newly
    * inserted lease.
     */
    public int getLeaseVersion()
        {
        return __m_LeaseVersion;
        }
    
    // Accessor for the property "ResourceKey"
    /**
     * Getter for property ResourceKey.<p>
    * Key for the resource represented by this Lease. This property is set
    * during initialization only.
    * 
    * @see #instantiate()
     */
    public Object getResourceKey()
        {
        return __m_ResourceKey;
        }
    
    // Accessor for the property "ResourceSize"
    /**
     * Getter for property ResourceSize.<p>
    * The size of the (serialized) resource represented by this Lease in bytes.
    * It is inteneded to be used by the cache implementations that have
    * automatic purge strategies that are based on the resource "weight".
    * This property is calculated asynchronously and is not guaranteed to carry
    * the precise value at all times. The value of -1 indicates that the
    * resource has not yet been deserialized.
     */
    public int getResourceSize()
        {
        return __m_ResourceSize;
        }
    
    // Accessor for the property "ResourceVersion"
    /**
     * Getter for property ResourceVersion.<p>
    * The version of the resource represented by this Lease. It is intended to
    * be used in the optimistic scenarios that do not "lock" prior to the
    * resource updates, but instead discard the "outdated" update requests.
    * 
    * The value of ResourceVersion is in a range of 0..255, where value of zero
    * represents a not existing resource and value of one represents a newly
    * inserted resource.
     */
    public int getResourceVersion()
        {
        return __m_ResourceVersion;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * Service object handling this Lease. This property is set during
    * initialization only.
    * 
    * @see #instantiateLease()
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid getService()
        {
        return __m_Service;
        }
    
    // Accessor for the property "Status"
    /**
     * Getter for property Status.<p>
    * Calculated property that returns this Lease status. The return value is
    * one of:
    * <ul>
    * <li> LEASE_UNISSUED - a request for a lease issue has not been confirmed
    * yet or the issuer is gone
    * <li> LEASE_AVAILABLE - the lease is known to be available
    * <li> LEASE_LOCKED - the lease is known to be held by this service member 
    * <li> LEASE_DIRTY - the lease is known to be held by another service
    * member
    * </ul>
     */
    public int getStatus()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Net.MemberSet;
        
        // Note: a locked lease may not be "unissued"!
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid   service    = getService();
        MemberSet setMember  = service.getServiceMemberSet();
        int       nThisId    = service.getThisMember().getId();
        int       nHolderId  = getHolderId();
        boolean   fAvailable = nHolderId == 0 || setMember.getMember(nHolderId) == null;
        
        if (!fAvailable)
            {
            // check the expiration time being more pessimistic
            // on the holder side (compensating for time difference)
            long lExpirationTime = getExpirationTime();
        
            if (nThisId == nHolderId)
                {
                // 10 == service.getCluster().getClusterService().getTimestampMaxVariance();
                lExpirationTime -= 10;
                }
            fAvailable = lExpirationTime  <= service.getClusterTime();
            }
        
        if (fAvailable)
            {
            int nIssuerId = getIssuerId();
        
            return nIssuerId == 0 || setMember.getMember(nIssuerId) == null ?
                LEASE_UNISSUED : LEASE_AVAILABLE;
            }
        else
            {
            // since the Lease is thread agnostic, the further decision
            // is made at the level where the calling thread is known
            // (see #ReplicatedCache.getThreadStatus(Lease))
            return nHolderId == nThisId ?
                LEASE_LOCKED : LEASE_DIRTY;
            }
        }
    
    // Accessor for the property "ThreadIdCounter"
    /**
     * Getter for property ThreadIdCounter.<p>
    * Atomic counter used to assign unique thread identifiers.
     */
    private static java.util.concurrent.atomic.AtomicLong getThreadIdCounter()
        {
        return __s_ThreadIdCounter;
        }
    
    // Accessor for the property "ThreadIdHolder"
    /**
     * Getter for property ThreadIdHolder.<p>
    * ThreadLocal object holding unique thread identifiers.
     */
    private static ThreadLocal getThreadIdHolder()
        {
        return __s_ThreadIdHolder;
        }
    
    /**
     * Helper method to increment the version
     */
    public synchronized void incrementLeaseVersion()
        {
        int nVersion = getLeaseVersion();
        if (++nVersion > 255)
            {
            nVersion = 2;
            }
        setLeaseVersion(nVersion);
        }
    
    /**
     * Helper method to increment the version
     */
    public synchronized void incrementResourceVersion()
        {
        int nVersion = getResourceVersion();
        if (++nVersion > 255)
            {
            nVersion = 2;
            }
        setResourceVersion(nVersion);
        }
    
    /**
     * Instantiate a new Lease for  the specified cache index, resource key and
    * service.
     */
    public static Lease instantiate(int iCache, Object oKey, com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        Lease lease = new Lease();
        
        lease.setCacheIndex(iCache);
        lease.setResourceKey(oKey);
        lease.setService(service);
        
        return lease;
        }
    
    public void read(java.io.DataInput stream)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        // LeaseMessage is responsible for CacheIndex and ResourceKey deserialization
        
        setIssuerId(stream.readUnsignedShort());
        setHolderId(stream.readUnsignedShort());
        setHolderThreadId(ExternalizableHelper.readLong(stream));
        setEffectiveTime(ExternalizableHelper.readLong(stream));
        setExpirationTime(ExternalizableHelper.readLong(stream));
        int nVersion = stream.readUnsignedShort();
        setLeaseVersion(nVersion & 0xFF);
        setResourceVersion((nVersion & 0xFF00) >>> 8);
        }
    
    // Accessor for the property "CacheIndex"
    /**
     * Setter for property CacheIndex.<p>
    * The index of the cache that this Lease belongs to. This index is used by
    * the cache service to get to an appropriate cache [handler].
     */
    public void setCacheIndex(int iCache)
        {
        __m_CacheIndex = iCache;
        }
    
    // Accessor for the property "ClassLoader"
    /**
     * Setter for property ClassLoader.<p>
    * The ClassLoader that the corresponding resource is known to be assosiated
    * with.
     */
    public void setClassLoader(ClassLoader loader)
        {
        __m_ClassLoader = loader;
        }
    
    // Accessor for the property "EffectiveTime"
    /**
     * Setter for property EffectiveTime.<p>
    * Cluster time that the Lease was locked at.
     */
    public void setEffectiveTime(long lDatetime)
        {
        __m_EffectiveTime = lDatetime;
        }
    
    // Accessor for the property "ExpirationTime"
    /**
     * Setter for property ExpirationTime.<p>
    * Cluster time that the Lease expires at.
     */
    public void setExpirationTime(long lDatetime)
        {
        __m_ExpirationTime = lDatetime;
        }
    
    // Accessor for the property "HolderId"
    /**
     * Setter for property HolderId.<p>
    * Member id of the holder for this Lease. Value of 0 indicates that
    * currently there is no holder for this Lease.
     */
    public void setHolderId(int nId)
        {
        __m_HolderId = nId;
        }
    
    // Accessor for the property "HolderThreadId"
    /**
     * Setter for property HolderThreadId.<p>
    * Unique id of the Thread which holds a lock for this Lease. This property
    * has meaning only if the following holds true:
    * <pre><code>
    *     getHolderId() == getService().getThisMember().getId()
    * </code></pre>
     */
    public void setHolderThreadId(long lThreadId)
        {
        __m_HolderThreadId = lThreadId;
        }
    
    // Accessor for the property "IssuerId"
    /**
     * Setter for property IssuerId.<p>
    * Member id of the issuer (registrar) for this Lease. Value of 0 indicates
    * that currently there is no issuer for this Lease.
     */
    public void setIssuerId(int nId)
        {
        __m_IssuerId = nId;
        }
    
    // Accessor for the property "LeaseVersion"
    /**
     * Setter for property LeaseVersion.<p>
    * The version of the Lease. It is intended to be used to resolve
    * simultaneous conflicting requests.
    * 
    * The value of LeaseVersion is in a range of 0..255, where value of zero
    * represents a not existing lease and value of one represents a newly
    * inserted lease.
     */
    protected void setLeaseVersion(int nVersion)
        {
        __m_LeaseVersion = nVersion;
        }
    
    // Accessor for the property "ResourceKey"
    /**
     * Setter for property ResourceKey.<p>
    * Key for the resource represented by this Lease. This property is set
    * during initialization only.
    * 
    * @see #instantiate()
     */
    protected void setResourceKey(Object oKey)
        {
        __m_ResourceKey = oKey;
        }
    
    // Accessor for the property "ResourceSize"
    /**
     * Setter for property ResourceSize.<p>
    * The size of the (serialized) resource represented by this Lease in bytes.
    * It is inteneded to be used by the cache implementations that have
    * automatic purge strategies that are based on the resource "weight".
    * This property is calculated asynchronously and is not guaranteed to carry
    * the precise value at all times. The value of -1 indicates that the
    * resource has not yet been deserialized.
     */
    public void setResourceSize(int cbSize)
        {
        __m_ResourceSize = cbSize;
        }
    
    // Accessor for the property "ResourceVersion"
    /**
     * Setter for property ResourceVersion.<p>
    * The version of the resource represented by this Lease. It is intended to
    * be used in the optimistic scenarios that do not "lock" prior to the
    * resource updates, but instead discard the "outdated" update requests.
    * 
    * The value of ResourceVersion is in a range of 0..255, where value of zero
    * represents a not existing resource and value of one represents a newly
    * inserted resource.
     */
    protected void setResourceVersion(int nVersion)
        {
        __m_ResourceVersion = nVersion;
        }
    
    // Accessor for the property "Service"
    /**
     * Setter for property Service.<p>
    * Service object handling this Lease. This property is set during
    * initialization only.
    * 
    * @see #instantiateLease()
     */
    protected void setService(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        __m_Service = service;
        }
    
    // Accessor for the property "ThreadIdCounter"
    /**
     * Setter for property ThreadIdCounter.<p>
    * Atomic counter used to assign unique thread identifiers.
     */
    private static void setThreadIdCounter(java.util.concurrent.atomic.AtomicLong counter)
        {
        __s_ThreadIdCounter = counter;
        }
    
    // Accessor for the property "ThreadIdHolder"
    /**
     * Setter for property ThreadIdHolder.<p>
    * ThreadLocal object holding unique thread identifiers.
     */
    private static void setThreadIdHolder(ThreadLocal tlo)
        {
        __s_ThreadIdHolder = tlo;
        }
    
    // Declared at the super level
    public String toString()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import java.util.Date;
        
        StringBuffer sb = new StringBuffer();
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service   = getService();
        int     nIssuerId = getIssuerId();
        int     nHolderId = getHolderId();
        long    lThreadId = getHolderThreadId();
        int     cbSize    = getResourceSize();
        int     nStatus   = getStatus();
        
        sb.append("Lease: ")
          .append(getResourceKey())
          .append(" (Cache=")
          .append(formatCacheName(getCacheIndex(), service))
          .append(", Size=")
          .append(cbSize == -1 ? "Unknown" : String.valueOf(cbSize))
          .append(", Version=")
          .append(getLeaseVersion())
          .append('/')
          .append(getResourceVersion())
          .append(", IssuerId=")
          .append(nIssuerId)
          .append(", HolderId=")
          .append(nHolderId)
          .append(", Status=")
          .append(formatStatusName(nStatus));
        
        if (nStatus == LEASE_LOCKED || nStatus == LEASE_DIRTY)
            {
            if (nHolderId == service.getThisMember().getId())
                {
                sb.append(", Held by threadId=")
                  .append(lThreadId);
                }
        
            sb.append(", Locked at ")
              .append(new Date(getEffectiveTime()))
              .append(", Expires in ")
              .append(getExpirationTime() - service.getClusterTime())
              .append(" millis");
            }
        else
            {
            if (nHolderId == 0)
                {
                sb.append(", Last locked at ")
                  .append(new Date(getEffectiveTime()));
                }
            else
                {
                sb.append(", Last held by member ")
                  .append(nHolderId)
                  .append(" from ")
                  .append(new Date(getEffectiveTime()))
                  .append(" to ")
                  .append(new Date(getExpirationTime()));
                }
            }
        sb.append(')');
        
        return sb.toString();
        }
    
    /**
     * Unlock the lease.
     */
    public synchronized void unlock()
        {
        setHolderId(0);
        setHolderThreadId(0L);
        setExpirationTime(getService().getClusterTime());
        
        notifyAll();
        }
    
    /**
     * Validate the lease.
     */
    public synchronized void validate()
        {
        MemberSet setMember = getService().getServiceMemberSet();
        
        // check the holder
        int nHolderId = getHolderId();
        if (nHolderId != 0 && setMember.getMember(nHolderId) == null)
            {
            // the lease holder is gone - remove the lock
            unlock();
            }
        
        // check the expiration time
        if (nHolderId != 0 && getExpirationTime() <= getService().getClusterTime())
            {
            // the lease has expired -- remove the lock
            unlock();
            }
        
        // check the issuer
        int nIssuerId = getIssuerId();
        if (nIssuerId != 0 && setMember.getMember(nIssuerId) == null)
            {
            // the issuer is gone
            setIssuerId(0);
            notifyAll();
            }
        }
    
    public void write(java.io.DataOutput stream)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        // LeaseMessage is responsible for CacheIndex and ResourceKey serialization
        
        stream.writeShort(getIssuerId());
        stream.writeShort(getHolderId());
        ExternalizableHelper.writeLong(stream, getHolderThreadId());
        ExternalizableHelper.writeLong(stream, getEffectiveTime());
        ExternalizableHelper.writeLong(stream, getExpirationTime());
        stream.writeShort(getLeaseVersion() | (getResourceVersion() << 8));
        }
    }
