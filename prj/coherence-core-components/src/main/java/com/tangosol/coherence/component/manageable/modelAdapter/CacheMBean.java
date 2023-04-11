
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.CacheMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

/**
 * Generic CacheMBean.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class CacheMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
    {
    // ---- Fields declarations ----
    
    /**
     * Property BatchFactor
     *
     * The BatchFactor attribute is used to calculate the `soft-ripe` time for
     * write-behind queue entries. A queue entry is considered to be `ripe` for
     * a write operation if it has been in the write-behind queue for no less
     * than the QueueDelay interval. The `soft-ripe` time is the point in time
     * prior to the actual `ripe` time after which an entry will be included in
     * a batched asynchronous write operation to the CacheStore (along with all
     * other `ripe` and `soft-ripe` entries). This attribute is only applicable
     * if asynchronous writes are enabled (i.e. the value of the QueueDelay
     * attribute is greater than zero) and the CacheStore implements the
     * storeAll() method. The value of the element is expressed as a percentage
     * of the QueueDelay interval. Valid values are doubles in the interval
     * [0.0, 1.0].
     * 
     * @descriptor rest.collector=set
     */
    private transient double __m_BatchFactor;
    
    /**
     * Property ExpiryDelay
     *
     * The time-to-live for cache entries in milliseconds. Value of zero
     * indicates that the automatic expiry is disabled. Change of this
     * attribute will not affect already-scheduled expiry of existing entries.
     * 
     * @descriptor rest.collector=set
     */
    private transient int __m_ExpiryDelay;
    
    /**
     * Property HighUnits
     *
     * The limit of the cache size measured in units. The cache will prune
     * itself automatically once it reaches its maximum unit level. This is
     * often referred to as the `high water mark` of the cache.
     * 
     * @descriptor rest.collector=sum
     */
    private transient int __m_HighUnits;
    
    /**
     * Property LowUnits
     *
     * The number of units to which the cache will shrink when it prunes. This
     * is often referred to as a `low water mark` of the cache.
     *  
     * @descriptor rest.collector=sum
     */
    private transient int __m_LowUnits;
    
    /**
     * Property QueueDelay
     *
     * The number of seconds that an entry added to a write-behind queue will
     * sit in the queue before being stored via a CacheStore. Applicable only
     * for WRITE-BEHIND persistence type.
     * 
     * @descriptor rest.collector=set
     */
    private transient int __m_QueueDelay;
    
    /**
     * Property RefreshFactor
     *
     * The RefreshFactor attribute is used to calculate the `soft-expiration`
     * time for cache entries. Soft-expiration is the point in time prior to
     * the actual expiration after which any access request for an entry will
     * schedule an asynchronous load request for the entry. This attribute is
     * only applicable for a ReadWriteBackingMap which has an internal
     * LocalCache with scheduled automatic expiration. The value of this
     * element is expressed as a percentage of the internal LocalCache
     * expiration interval. Valid values are doubles in the interval[0.0, 1.0].
     * If zero, refresh-ahead scheduling will be disabled.
     * 
     * @descriptor rest.collector=set
     */
    private transient double __m_RefreshFactor;
    
    /**
     * Property RequeueThreshold
     *
     * The maximum size of the write-behind queue for which failed CacheStore
     * write operations are requeued. If zero, the write-behind requeueing will
     * be disabled. Applicable only for WRITE-BEHIND persistence type.
     * 
     * @descriptor rest.collector=set
     */
    private transient int __m_RequeueThreshold;
    
    // Default constructor
    public CacheMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public CacheMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            set_NotificationDescription("Eviction notifications");
            String[] a0 = new String[2];
                {
                a0[0] = "com.tangosol.coherence.cache.eviction";
                a0[1] = "com.tangosol.coherence.cache.prune";
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.CacheMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/CacheMBean".replace('/', '.'));
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
            "Generic CacheMBean.",
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
        
        // property AverageGetMillis
            {
            mapInfo.put("AverageGetMillis", new Object[]
                {
                "The average number of milliseconds per get() invocation since the last time statistics were reset. This statistic is only tracked for caches that may incur expensive hits (for example the front of a near cache).",
                "getAverageGetMillis",
                null,
                "D",
                null,
                });
            }
        
        // property AverageHitMillis
            {
            mapInfo.put("AverageHitMillis", new Object[]
                {
                "The average number of milliseconds per get() invocation that is a hit. This statistic is only tracked for caches that may incur expensive hits (for example the front of a near cache).",
                "getAverageHitMillis",
                null,
                "D",
                null,
                });
            }
        
        // property AverageMissMillis
            {
            mapInfo.put("AverageMissMillis", new Object[]
                {
                "The average number of milliseconds per get() invocation that is a miss. This statistic is only tracked for caches that may incur expensive hits (for example the front of a near cache).",
                "getAverageMissMillis",
                null,
                "D",
                null,
                });
            }
        
        // property AveragePutMillis
            {
            mapInfo.put("AveragePutMillis", new Object[]
                {
                "The average number of milliseconds per put() invocation since the cache statistics were last reset. This statistic is only tracked for caches that may incur expensive hits (for example the front of a near cache).",
                "getAveragePutMillis",
                null,
                "D",
                null,
                });
            }
        
        // property BatchFactor
            {
            mapInfo.put("BatchFactor", new Object[]
                {
                "The BatchFactor attribute is used to calculate the `soft-ripe` time for write-behind queue entries. A queue entry is considered to be `ripe` for a write operation if it has been in the write-behind queue for no less than the QueueDelay interval. The `soft-ripe` time is the point in time prior to the actual `ripe` time after which an entry will be included in a batched asynchronous write operation to the CacheStore (along with all other `ripe` and `soft-ripe` entries). This attribute is only applicable if asynchronous writes are enabled (i.e. the value of the QueueDelay attribute is greater than zero) and the CacheStore implements the storeAll() method. The value of the element is expressed as a percentage of the QueueDelay interval. Valid values are doubles in the interval [0.0, 1.0].",
                "getBatchFactor",
                "setBatchFactor",
                "D",
                "rest.collector=set",
                });
            }
        
        // property CacheHits
            {
            mapInfo.put("CacheHits", new Object[]
                {
                "The rough number of cache hits  since the last time statistics were reset. A cache hit is a read operation invocation (i.e. get()) for which an entry exists in this map.",
                "getCacheHits",
                null,
                "J",
                "rest.collector=sum,metrics.value=Hits",
                });
            }
        
        // property CacheHitsMillis
            {
            mapInfo.put("CacheHitsMillis", new Object[]
                {
                "The total number of milliseconds (since the last time statistics were reset) for the get() operations for which an entry existed in this map. This statistic is only tracked for caches that may incur expensive hits (for example the front of a near cache).",
                "getCacheHitsMillis",
                null,
                "J",
                "rest.collector=sum,metrics.value=HitsMillis",
                });
            }
        
        // property CacheMisses
            {
            mapInfo.put("CacheMisses", new Object[]
                {
                "The rough number of cache misses since the last time statistics were reset.",
                "getCacheMisses",
                null,
                "J",
                "rest.collector=sum,metrics.value=Misses",
                });
            }
        
        // property CacheMissesMillis
            {
            mapInfo.put("CacheMissesMillis", new Object[]
                {
                "The total number of milliseconds ( since the last time statistics were reset) for the get() operations for which no entry existed in this map. This statistic is only tracked for caches that may incur expensive hits (for example the front of a near cache).",
                "getCacheMissesMillis",
                null,
                "J",
                "rest.collector=sum,metrics.value=MissesMillis",
                });
            }
        
        // property CachePrunes
            {
            mapInfo.put("CachePrunes", new Object[]
                {
                "The number of `prune` operations  since the last time statistics were reset. A prune operation occurs every time the cache reaches its high watermark.",
                "getCachePrunes",
                null,
                "J",
                "rest.collector=sum,metrics.value=Prunes",
                });
            }
        
        // property CachePrunesMillis
            {
            mapInfo.put("CachePrunesMillis", new Object[]
                {
                "The total number of milliseconds for the prune operations  since the last time statistics were reset.",
                "getCachePrunesMillis",
                null,
                "J",
                "rest.collector=sum,metrics.value=PrunesMillis",
                });
            }
        
        // property CacheStoreType
            {
            mapInfo.put("CacheStoreType", new Object[]
                {
                "The cache store type for this cache. Possible values include: NONE, READ-ONLY, WRITE-THROUGH, WRITE-BEHIND.",
                "getCacheStoreType",
                null,
                "Ljava/lang/String;",
                "rest.collector=set",
                });
            }
        
        // property Description
            {
            mapInfo.put("Description", new Object[]
                {
                "The cache description.",
                "getDescription",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property ExpiryDelay
            {
            mapInfo.put("ExpiryDelay", new Object[]
                {
                "The time-to-live for cache entries in milliseconds. Value of zero indicates that the automatic expiry is disabled. Change of this attribute will not affect already-scheduled expiry of existing entries.",
                "getExpiryDelay",
                "setExpiryDelay",
                "I",
                "rest.collector=set",
                });
            }
        
        // property HighUnits
            {
            mapInfo.put("HighUnits", new Object[]
                {
                "The limit of the cache size measured in units. The cache will prune itself automatically once it reaches its maximum unit level. This is often referred to as the `high water mark` of the cache.",
                "getHighUnits",
                "setHighUnits",
                "I",
                "rest.collector=sum",
                });
            }
        
        // property HitProbability
            {
            mapInfo.put("HitProbability", new Object[]
                {
                "The rough probability (0 <= p <= 1) that the next invocation will be a hit, based on the statistics collected  since the last time statistics were reset.",
                "getHitProbability",
                null,
                "D",
                null,
                });
            }
        
        // property LowUnits
            {
            mapInfo.put("LowUnits", new Object[]
                {
                "The number of units to which the cache will shrink when it prunes. This is often referred to as a `low water mark` of the cache.",
                "getLowUnits",
                "setLowUnits",
                "I",
                "rest.collector=sum",
                });
            }
        
        // property MemoryUnits
            {
            mapInfo.put("MemoryUnits", new Object[]
                {
                "Determines if Units is measuring the memory usage of the cache.  If true, Units * UnitFactor is the number of bytes consumed by the cache.",
                "isMemoryUnits",
                null,
                "Z",
                null,
                });
            }
        
        // property PersistenceType
            {
            mapInfo.put("PersistenceType", new Object[]
                {
                "Deprecated - use attribute CacheStoreType instead.",
                "getPersistenceType",
                null,
                "Ljava/lang/String;",
                "rest.collector=set",
                });
            }
        
        // property QueueDelay
            {
            mapInfo.put("QueueDelay", new Object[]
                {
                "The number of seconds that an entry added to a write-behind queue will sit in the queue before being stored via a CacheStore. Applicable only for WRITE-BEHIND persistence type.",
                "getQueueDelay",
                "setQueueDelay",
                "I",
                "rest.collector=set",
                });
            }
        
        // property QueueSize
            {
            mapInfo.put("QueueSize", new Object[]
                {
                "The size of the write-behind queue (if persistence type is WRITE-BEHIND) or the number of pending writes for non-blocking stores (stores implementing the NonBlockingEntryStore interface).",
                "getQueueSize",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property RefreshFactor
            {
            mapInfo.put("RefreshFactor", new Object[]
                {
                "The RefreshFactor attribute is used to calculate the `soft-expiration` time for cache entries. Soft-expiration is the point in time prior to the actual expiration after which any access request for an entry will schedule an asynchronous load request for the entry. This attribute is only applicable for a ReadWriteBackingMap which has an internal LocalCache with scheduled automatic expiration. The value of this element is expressed as a percentage of the internal LocalCache expiration interval. Valid values are doubles in the interval[0.0, 1.0]. If zero, refresh-ahead scheduling will be disabled.",
                "getRefreshFactor",
                "setRefreshFactor",
                "D",
                "rest.collector=set",
                });
            }
        
        // property RequeueThreshold
            {
            mapInfo.put("RequeueThreshold", new Object[]
                {
                "The maximum size of the write-behind queue for which failed CacheStore write operations are requeued. If zero, the write-behind requeueing will be disabled. Applicable only for WRITE-BEHIND persistence type.",
                "getRequeueThreshold",
                "setRequeueThreshold",
                "I",
                "rest.collector=set",
                });
            }
        
        // property Size
            {
            mapInfo.put("Size", new Object[]
                {
                "The number of entries in the cache.",
                "getSize",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property StoreAverageBatchSize
            {
            mapInfo.put("StoreAverageBatchSize", new Object[]
                {
                "The average number of entries stored per CacheStore write operation. A call to the store() method is counted as a batch of one, whereas a call to the storeAll() method is counted as a batch of the passed Map size. The value of this attribute is -1 if the persistence type is NONE.",
                "getStoreAverageBatchSize",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property StoreAverageReadMillis
            {
            mapInfo.put("StoreAverageReadMillis", new Object[]
                {
                "The average time (in millis) spent per read operation; -1 if persistence type is NONE. This statistic is only tracked for caches associated with a CacheStore.",
                "getStoreAverageReadMillis",
                null,
                "J",
                null,
                });
            }
        
        // property StoreAverageWriteMillis
            {
            mapInfo.put("StoreAverageWriteMillis", new Object[]
                {
                "The average time (in millis) spent per write operation; -1 if persistence type is NONE. This statistic is only tracked for caches associated with a CacheStore.",
                "getStoreAverageWriteMillis",
                null,
                "J",
                null,
                });
            }
        
        // property StoreFailures
            {
            mapInfo.put("StoreFailures", new Object[]
                {
                "The total number of CacheStore failures (load, store and erase operations); -1 if persistence type is NONE.",
                "getStoreFailures",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property StoreReadMillis
            {
            mapInfo.put("StoreReadMillis", new Object[]
                {
                "The cummulative time (in millis) spent on load operations; -1 if persistence type is NONE. This statistic is only tracked for caches associated with a CacheStore.",
                "getStoreReadMillis",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property StoreReads
            {
            mapInfo.put("StoreReads", new Object[]
                {
                "The total number of load operations; -1 if persistence type is NONE.",
                "getStoreReads",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property StoreWriteMillis
            {
            mapInfo.put("StoreWriteMillis", new Object[]
                {
                "The cummulative time (in milliseconds) spent on store and erase operations; -1 if persistence type is NONE or READ-ONLY. This statistic is only tracked for caches associated with a CacheStore.",
                "getStoreWriteMillis",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property StoreWrites
            {
            mapInfo.put("StoreWrites", new Object[]
                {
                "The total number of store and erase operations; -1 if persistence type is NONE or READ-ONLY.",
                "getStoreWrites",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TotalGets
            {
            mapInfo.put("TotalGets", new Object[]
                {
                "The total number of get() operations since the last time statistics were reset.",
                "getTotalGets",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TotalGetsMillis
            {
            mapInfo.put("TotalGetsMillis", new Object[]
                {
                "The total number of milliseconds spent on get() operations since the last time statistics were reset. This statistic is only tracked for caches that may incur expensive hits (for example the front of a near cache).",
                "getTotalGetsMillis",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TotalPuts
            {
            mapInfo.put("TotalPuts", new Object[]
                {
                "The total number of put() operations since the last time statistics were reset.",
                "getTotalPuts",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TotalPutsMillis
            {
            mapInfo.put("TotalPutsMillis", new Object[]
                {
                "The total number of milliseconds spent on put() operations since the last time statistics were reset. This statistic is only tracked for caches that may incur expensive hits (for example the front of a near cache).",
                "getTotalPutsMillis",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property UnitFactor
            {
            mapInfo.put("UnitFactor", new Object[]
                {
                "The factor by which the Units, LowUnits and HighUnits properties are adjusted. Using a BINARY unit calculator, for example, the factor of 1048576 could be used to count megabytes instead of bytes.",
                "getUnitFactor",
                null,
                "I",
                "rest.collector=set",
                });
            }
        
        // property Units
            {
            mapInfo.put("Units", new Object[]
                {
                "The size of the cache measured in units. This value needs to be adjusted by the UnitFactor.",
                "getUnits",
                null,
                "I",
                "rest.collector=sum",
                });
            }
        
        // property UnitsBytes
            {
            mapInfo.put("UnitsBytes", new Object[]
                {
                "The size of the cache measured in bytes.",
                "getUnitsBytes",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
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
                "Reset the cache statistics.",
                "resetStatistics",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        return mapInfo;
        }
    
    // Accessor for the property "AverageGetMillis"
    /**
     * Getter for property AverageGetMillis.<p>
    * The average number of milliseconds per get() invocation since the last
    * time statistics were reset. This statistic is only tracked for caches
    * that may incur expensive hits (for example the front of a near cache).
     */
    public double getAverageGetMillis()
        {
        return 0.0;
        }
    
    // Accessor for the property "AverageHitMillis"
    /**
     * Getter for property AverageHitMillis.<p>
    * The average number of milliseconds per get() invocation that is a hit.
    * This statistic is only tracked for caches that may incur expensive hits
    * (for example the front of a near cache).
     */
    public double getAverageHitMillis()
        {
        return 0.0;
        }
    
    // Accessor for the property "AverageMissMillis"
    /**
     * Getter for property AverageMissMillis.<p>
    * The average number of milliseconds per get() invocation that is a miss.
    * This statistic is only tracked for caches that may incur expensive hits
    * (for example the front of a near cache).
     */
    public double getAverageMissMillis()
        {
        return 0.0;
        }
    
    // Accessor for the property "AveragePutMillis"
    /**
     * Getter for property AveragePutMillis.<p>
    * The average number of milliseconds per put() invocation since the cache
    * statistics were last reset. This statistic is only tracked for caches
    * that may incur expensive hits (for example the front of a near cache).
     */
    public double getAveragePutMillis()
        {
        return 0.0;
        }
    
    // Accessor for the property "BatchFactor"
    /**
     * Getter for property BatchFactor.<p>
    * The BatchFactor attribute is used to calculate the `soft-ripe` time for
    * write-behind queue entries. A queue entry is considered to be `ripe` for
    * a write operation if it has been in the write-behind queue for no less
    * than the QueueDelay interval. The `soft-ripe` time is the point in time
    * prior to the actual `ripe` time after which an entry will be included in
    * a batched asynchronous write operation to the CacheStore (along with all
    * other `ripe` and `soft-ripe` entries). This attribute is only applicable
    * if asynchronous writes are enabled (i.e. the value of the QueueDelay
    * attribute is greater than zero) and the CacheStore implements the
    * storeAll() method. The value of the element is expressed as a percentage
    * of the QueueDelay interval. Valid values are doubles in the interval
    * [0.0, 1.0].
    * 
    * @descriptor rest.collector=set
     */
    public double getBatchFactor()
        {
        return __m_BatchFactor;
        }
    
    // Accessor for the property "CacheHits"
    /**
     * Getter for property CacheHits.<p>
    * The rough number of cache hits  since the last time statistics were
    * reset. A cache hit is a read operation invocation (i.e. get()) for which
    * an entry exists in this map.
    * 
    * @descriptor rest.collector=sum,metrics.value=Hits
     */
    public long getCacheHits()
        {
        return 0L;
        }
    
    // Accessor for the property "CacheHitsMillis"
    /**
     * Getter for property CacheHitsMillis.<p>
    * The total number of milliseconds (since the last time statistics were
    * reset) for the get() operations for which an entry existed in this map.
    * This statistic is only tracked for caches that may incur expensive hits
    * (for example the front of a near cache).
    * 
    * @descriptor rest.collector=sum,metrics.value=HitsMillis
     */
    public long getCacheHitsMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "CacheMisses"
    /**
     * Getter for property CacheMisses.<p>
    * The rough number of cache misses since the last time statistics were
    * reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=Misses
     */
    public long getCacheMisses()
        {
        return 0L;
        }
    
    // Accessor for the property "CacheMissesMillis"
    /**
     * Getter for property CacheMissesMillis.<p>
    * The total number of milliseconds ( since the last time statistics were
    * reset) for the get() operations for which no entry existed in this map.
    * This statistic is only tracked for caches that may incur expensive hits
    * (for example the front of a near cache).
    * 
    * @descriptor rest.collector=sum,metrics.value=MissesMillis
     */
    public long getCacheMissesMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "CachePrunes"
    /**
     * Getter for property CachePrunes.<p>
    * The number of `prune` operations  since the last time statistics were
    * reset. A prune operation occurs every time the cache reaches its high
    * watermark.
    * 
    * @descriptor rest.collector=sum,metrics.value=Prunes
     */
    public long getCachePrunes()
        {
        return 0L;
        }
    
    // Accessor for the property "CachePrunesMillis"
    /**
     * Getter for property CachePrunesMillis.<p>
    * The total number of milliseconds for the prune operations  since the last
    * time statistics were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=PrunesMillis
     */
    public long getCachePrunesMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "CacheStoreType"
    /**
     * Getter for property CacheStoreType.<p>
    * The cache store type for this cache. Possible values include: NONE,
    * READ-ONLY, WRITE-THROUGH, WRITE-BEHIND.
    * 
    * @descriptor rest.collector=set
     */
    public String getCacheStoreType()
        {
        return null;
        }
    
    // Accessor for the property "Description"
    /**
     * Getter for property Description.<p>
    * The cache description.
     */
    public String getDescription()
        {
        return null;
        }
    
    // Accessor for the property "ExpiryDelay"
    /**
     * Getter for property ExpiryDelay.<p>
    * The time-to-live for cache entries in milliseconds. Value of zero
    * indicates that the automatic expiry is disabled. Change of this attribute
    * will not affect already-scheduled expiry of existing entries.
    * 
    * @descriptor rest.collector=set
     */
    public int getExpiryDelay()
        {
        return __m_ExpiryDelay;
        }
    
    // Accessor for the property "HighUnits"
    /**
     * Getter for property HighUnits.<p>
    * The limit of the cache size measured in units. The cache will prune
    * itself automatically once it reaches its maximum unit level. This is
    * often referred to as the `high water mark` of the cache.
    * 
    * @descriptor rest.collector=sum
     */
    public int getHighUnits()
        {
        return __m_HighUnits;
        }
    
    // Accessor for the property "HitProbability"
    /**
     * Getter for property HitProbability.<p>
    * The rough probability (0 <= p <= 1) that the next invocation will be a
    * hit, based on the statistics collected  since the last time statistics
    * were reset.
     */
    public double getHitProbability()
        {
        return 0.0;
        }
    
    // Accessor for the property "LowUnits"
    /**
     * Getter for property LowUnits.<p>
    * The number of units to which the cache will shrink when it prunes. This
    * is often referred to as a `low water mark` of the cache.
    *  
    * @descriptor rest.collector=sum
     */
    public int getLowUnits()
        {
        return __m_LowUnits;
        }
    
    // Accessor for the property "PersistenceType"
    /**
     * Getter for property PersistenceType.<p>
    * Deprecated - use attribute CacheStoreType instead.
    * 
    * @descriptor rest.collector=set
     */
    public String getPersistenceType()
        {
        return null;
        }
    
    // Accessor for the property "QueueDelay"
    /**
     * Getter for property QueueDelay.<p>
    * The number of seconds that an entry added to a write-behind queue will
    * sit in the queue before being stored via a CacheStore. Applicable only
    * for WRITE-BEHIND persistence type.
    * 
    * @descriptor rest.collector=set
     */
    public int getQueueDelay()
        {
        return __m_QueueDelay;
        }
    
    // Accessor for the property "QueueSize"
    /**
     * Getter for property QueueSize.<p>
    * The size of the write-behind queue (if persistence type is WRITE-BEHIND)
    * or the number of pending writes for non-blocking stores (stores
    * implementing the NonBlockingEntryStore interface).
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getQueueSize()
        {
        return 0;
        }
    
    // Accessor for the property "RefreshFactor"
    /**
     * Getter for property RefreshFactor.<p>
    * The RefreshFactor attribute is used to calculate the `soft-expiration`
    * time for cache entries. Soft-expiration is the point in time prior to the
    * actual expiration after which any access request for an entry will
    * schedule an asynchronous load request for the entry. This attribute is
    * only applicable for a ReadWriteBackingMap which has an internal
    * LocalCache with scheduled automatic expiration. The value of this element
    * is expressed as a percentage of the internal LocalCache expiration
    * interval. Valid values are doubles in the interval[0.0, 1.0]. If zero,
    * refresh-ahead scheduling will be disabled.
    * 
    * @descriptor rest.collector=set
     */
    public double getRefreshFactor()
        {
        return __m_RefreshFactor;
        }
    
    // Accessor for the property "RequeueThreshold"
    /**
     * Getter for property RequeueThreshold.<p>
    * The maximum size of the write-behind queue for which failed CacheStore
    * write operations are requeued. If zero, the write-behind requeueing will
    * be disabled. Applicable only for WRITE-BEHIND persistence type.
    * 
    * @descriptor rest.collector=set
     */
    public int getRequeueThreshold()
        {
        return __m_RequeueThreshold;
        }
    
    // Accessor for the property "Size"
    /**
     * Getter for property Size.<p>
    * The number of entries in the cache.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getSize()
        {
        return 0;
        }
    
    // Accessor for the property "StoreAverageBatchSize"
    /**
     * Getter for property StoreAverageBatchSize.<p>
    * The average number of entries stored per CacheStore write operation. A
    * call to the store() method is counted as a batch of one, whereas a call
    * to the storeAll() method is counted as a batch of the passed Map size.
    * The value of this attribute is -1 if the persistence type is NONE.
    * 
    * @descriptor metrics.value=_default
     */
    public long getStoreAverageBatchSize()
        {
        return 0L;
        }
    
    // Accessor for the property "StoreAverageReadMillis"
    /**
     * Getter for property StoreAverageReadMillis.<p>
    * The average time (in millis) spent per read operation; -1 if persistence
    * type is NONE. This statistic is only tracked for caches associated with a
    * CacheStore.
     */
    public long getStoreAverageReadMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "StoreAverageWriteMillis"
    /**
     * Getter for property StoreAverageWriteMillis.<p>
    * The average time (in millis) spent per write operation; -1 if persistence
    * type is NONE. This statistic is only tracked for caches associated with a
    * CacheStore.
     */
    public long getStoreAverageWriteMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "StoreFailures"
    /**
     * Getter for property StoreFailures.<p>
    * The total number of CacheStore failures (load, store and erase
    * operations); -1 if persistence type is NONE.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getStoreFailures()
        {
        return 0L;
        }
    
    // Accessor for the property "StoreReadMillis"
    /**
     * Getter for property StoreReadMillis.<p>
    * The cummulative time (in millis) spent on load operations; -1 if
    * persistence type is NONE. This statistic is only tracked for caches
    * associated with a CacheStore.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getStoreReadMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "StoreReads"
    /**
     * Getter for property StoreReads.<p>
    * The total number of load operations; -1 if persistence type is NONE.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getStoreReads()
        {
        return 0L;
        }
    
    // Accessor for the property "StoreWriteMillis"
    /**
     * Getter for property StoreWriteMillis.<p>
    * The cummulative time (in milliseconds) spent on store and erase
    * operations; -1 if persistence type is NONE or READ-ONLY. This statistic
    * is only tracked for caches associated with a CacheStore.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getStoreWriteMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "StoreWrites"
    /**
     * Getter for property StoreWrites.<p>
    * The total number of store and erase operations; -1 if persistence type is
    * NONE or READ-ONLY.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getStoreWrites()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalGets"
    /**
     * Getter for property TotalGets.<p>
    * The total number of get() operations since the last time statistics were
    * reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getTotalGets()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalGetsMillis"
    /**
     * Getter for property TotalGetsMillis.<p>
    * The total number of milliseconds spent on get() operations since the last
    * time statistics were reset. This statistic is only tracked for caches
    * that may incur expensive hits (for example the front of a near cache).
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getTotalGetsMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalPuts"
    /**
     * Getter for property TotalPuts.<p>
    * The total number of put() operations since the last time statistics were
    * reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getTotalPuts()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalPutsMillis"
    /**
     * Getter for property TotalPutsMillis.<p>
    * The total number of milliseconds spent on put() operations since the last
    * time statistics were reset. This statistic is only tracked for caches
    * that may incur expensive hits (for example the front of a near cache).
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getTotalPutsMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "UnitFactor"
    /**
     * Getter for property UnitFactor.<p>
    * The factor by which the Units, LowUnits and HighUnits properties are
    * adjusted. Using a BINARY unit calculator, for example, the factor of
    * 1048576 could be used to count megabytes instead of bytes.
    * 
    * @descriptor rest.collector=set
     */
    public int getUnitFactor()
        {
        return 0;
        }
    
    // Accessor for the property "Units"
    /**
     * Getter for property Units.<p>
    * The size of the cache measured in units. This value needs to be adjusted
    * by the UnitFactor.
    * 
    * @descriptor rest.collector=sum
     */
    public int getUnits()
        {
        return 0;
        }
    
    // Accessor for the property "UnitsBytes"
    /**
     * Getter for property UnitsBytes.<p>
    * The size of the cache measured in bytes.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getUnitsBytes()
        {
        return 0L;
        }
    
    // Accessor for the property "MemoryUnits"
    /**
     * Getter for property MemoryUnits.<p>
    * Determines if Units is measuring the memory usage of the cache.  If true,
    * Units * UnitFactor is the number of bytes consumed by the cache.
     */
    public boolean isMemoryUnits()
        {
        return false;
        }
    
    /**
     * Reset the cache statistics.
     */
    public void resetStatistics()
        {
        }
    
    // Accessor for the property "BatchFactor"
    /**
     * Setter for property BatchFactor.<p>
    * The BatchFactor attribute is used to calculate the `soft-ripe` time for
    * write-behind queue entries. A queue entry is considered to be `ripe` for
    * a write operation if it has been in the write-behind queue for no less
    * than the QueueDelay interval. The `soft-ripe` time is the point in time
    * prior to the actual `ripe` time after which an entry will be included in
    * a batched asynchronous write operation to the CacheStore (along with all
    * other `ripe` and `soft-ripe` entries). This attribute is only applicable
    * if asynchronous writes are enabled (i.e. the value of the QueueDelay
    * attribute is greater than zero) and the CacheStore implements the
    * storeAll() method. The value of the element is expressed as a percentage
    * of the QueueDelay interval. Valid values are doubles in the interval
    * [0.0, 1.0].
    * 
    * @descriptor rest.collector=set
     */
    public void setBatchFactor(double dFactor)
        {
        __m_BatchFactor = dFactor;
        }
    
    // Accessor for the property "ExpiryDelay"
    /**
     * Setter for property ExpiryDelay.<p>
    * The time-to-live for cache entries in milliseconds. Value of zero
    * indicates that the automatic expiry is disabled. Change of this attribute
    * will not affect already-scheduled expiry of existing entries.
    * 
    * @descriptor rest.collector=set
     */
    public void setExpiryDelay(int cUnits)
        {
        __m_ExpiryDelay = cUnits;
        }
    
    // Accessor for the property "HighUnits"
    /**
     * Setter for property HighUnits.<p>
    * The limit of the cache size measured in units. The cache will prune
    * itself automatically once it reaches its maximum unit level. This is
    * often referred to as the `high water mark` of the cache.
    * 
    * @descriptor rest.collector=sum
     */
    public void setHighUnits(int cUnits)
        {
        __m_HighUnits = cUnits;
        }
    
    // Accessor for the property "LowUnits"
    /**
     * Setter for property LowUnits.<p>
    * The number of units to which the cache will shrink when it prunes. This
    * is often referred to as a `low water mark` of the cache.
    *  
    * @descriptor rest.collector=sum
     */
    public void setLowUnits(int cUnits)
        {
        __m_LowUnits = cUnits;
        }
    
    // Accessor for the property "QueueDelay"
    /**
     * Setter for property QueueDelay.<p>
    * The number of seconds that an entry added to a write-behind queue will
    * sit in the queue before being stored via a CacheStore. Applicable only
    * for WRITE-BEHIND persistence type.
    * 
    * @descriptor rest.collector=set
     */
    public void setQueueDelay(int cDelay)
        {
        __m_QueueDelay = cDelay;
        }
    
    // Accessor for the property "RefreshFactor"
    /**
     * Setter for property RefreshFactor.<p>
    * The RefreshFactor attribute is used to calculate the `soft-expiration`
    * time for cache entries. Soft-expiration is the point in time prior to the
    * actual expiration after which any access request for an entry will
    * schedule an asynchronous load request for the entry. This attribute is
    * only applicable for a ReadWriteBackingMap which has an internal
    * LocalCache with scheduled automatic expiration. The value of this element
    * is expressed as a percentage of the internal LocalCache expiration
    * interval. Valid values are doubles in the interval[0.0, 1.0]. If zero,
    * refresh-ahead scheduling will be disabled.
    * 
    * @descriptor rest.collector=set
     */
    public void setRefreshFactor(double dFactor)
        {
        __m_RefreshFactor = dFactor;
        }
    
    // Accessor for the property "RequeueThreshold"
    /**
     * Setter for property RequeueThreshold.<p>
    * The maximum size of the write-behind queue for which failed CacheStore
    * write operations are requeued. If zero, the write-behind requeueing will
    * be disabled. Applicable only for WRITE-BEHIND persistence type.
    * 
    * @descriptor rest.collector=set
     */
    public void setRequeueThreshold(int cThreshold)
        {
        __m_RequeueThreshold = cThreshold;
        }
    }
