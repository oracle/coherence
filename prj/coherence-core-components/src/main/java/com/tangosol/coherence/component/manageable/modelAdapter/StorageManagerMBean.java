
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.StorageManagerMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

/**
 * The StorageManagerMBean represents a Storage instance for a storage-enabled
 * DistributedCacheService. A Storage instance manages all index, listener, and
 * lock information for the portion of the DistributedCache managed by the
 * local member.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class StorageManagerMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
    {
    // ---- Fields declarations ----
    
    /**
     * Property MaxQueryThresholdMillis
     *
     * A query execution threshold in milliseconds The longest query executing
     * longer than this threshold will be reported by the MaxQueryDescription
     * attribute.
     * 
     * @descriptor rest.collector=set
     */
    private transient long __m_MaxQueryThresholdMillis;
    
    // Default constructor
    public StorageManagerMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public StorageManagerMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.StorageManagerMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/StorageManagerMBean".replace('/', '.'));
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
            "The StorageManagerMBean represents a Storage instance for a storage-enabled DistributedCacheService. A Storage instance manages all index, listener, and lock information for the portion of the DistributedCache managed by the local member.",
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
        
        // property EventInterceptorInfo
            {
            mapInfo.put("EventInterceptorInfo", new Object[]
                {
                "An array of statistics for events processed by event interceptors.",
                "getEventInterceptorInfo",
                null,
                "[Ljava/lang/String;",
                null,
                });
            }
        
        // property EventsDispatched
            {
            mapInfo.put("EventsDispatched", new Object[]
                {
                "The total number of events dispatched by the StorageManager since the last time the statistics were reset.",
                "getEventsDispatched",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property EvictionCount
            {
            mapInfo.put("EvictionCount", new Object[]
                {
                "The number of evictions from the backing map managed by this StorageManager caused by entries expiry or insert operations that would make the underlying backing map to reach its configured size limit.  The eviction count is used to audit the cache size in a static system.  Cache Size =  Insert Count - Remove Count - Eviction count.  Therefore the eviction count is not reset by the reset statistics method.",
                "getEvictionCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property IndexInfo
            {
            mapInfo.put("IndexInfo", new Object[]
                {
                "An array of information for each index applied to the portion of the partitioned cache managed by the StorageManager. Each element is a string value that includes a ValueExtractor description, ordered flag (true to indicate that the contents of the index are ordered; false otherwise), and cardinality (number of unique values indexed).",
                "getIndexInfo",
                null,
                "[Ljava/lang/String;",
                null,
                });
            }
        
        // property IndexingTotalMillis
            {
            mapInfo.put("IndexingTotalMillis", new Object[]
                {
                "The cumulative duration in milliseconds of index builds since statistics were last reset.",
                "getIndexingTotalMillis",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property IndexTotalUnits
            {
            mapInfo.put("IndexTotalUnits", new Object[]
                {
                "The total units used by all indices on the associated cache.",
                "getIndexTotalUnits",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property InsertCount
            {
            mapInfo.put("InsertCount", new Object[]
                {
                "The number of inserts into the backing map managed by this StorageManager. In addition to standard inserts caused by put and invoke operations or synthetic inserts caused by get operations with read-through backing map topology, this counter is incremented when distribution transfers move resources `into` the underlying backing map and is decremented when  distribution transfers move data `out`.  The insert count is used to audit the cache size in a static system.  Cache Size =  Insert Count - Remove Count - Eviction count.  Therefore the insert count is not reset by the reset statistics method.",
                "getInsertCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property ListenerFilterCount
            {
            mapInfo.put("ListenerFilterCount", new Object[]
                {
                "The number of filter-based listeners currently registered with the StorageManager.",
                "getListenerFilterCount",
                null,
                "I",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property ListenerKeyCount
            {
            mapInfo.put("ListenerKeyCount", new Object[]
                {
                "The number of key-based listeners currently registered with the StorageManager.",
                "getListenerKeyCount",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property ListenerRegistrations
            {
            mapInfo.put("ListenerRegistrations", new Object[]
                {
                "The total number of listener registration requests processed by the StorageManager since the last time the statistics were reset.",
                "getListenerRegistrations",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property LocksGranted
            {
            mapInfo.put("LocksGranted", new Object[]
                {
                "The number of locks currently granted for the portion of the partitioned cache managed by the StorageManager.",
                "getLocksGranted",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property LocksPending
            {
            mapInfo.put("LocksPending", new Object[]
                {
                "The number of pending lock requests for the portion of the partitioned cache managed by the StorageManager.",
                "getLocksPending",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property MaxQueryDescription
            {
            mapInfo.put("MaxQueryDescription", new Object[]
                {
                "A string representation of a query with the longest execution time exceeding the MaxQueryThresholdMillis since statistics were last reset.",
                "getMaxQueryDescription",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property MaxQueryDurationMillis
            {
            mapInfo.put("MaxQueryDurationMillis", new Object[]
                {
                "The duration in milliseconds of the longest query execution since statistics were last reset.",
                "getMaxQueryDurationMillis",
                null,
                "J",
                "rest.collector=max,metrics.value=_default",
                });
            }
        
        // property MaxQueryThresholdMillis
            {
            mapInfo.put("MaxQueryThresholdMillis", new Object[]
                {
                "A query execution threshold in milliseconds The longest query executing longer than this threshold will be reported by the MaxQueryDescription attribute.",
                "getMaxQueryThresholdMillis",
                "setMaxQueryThresholdMillis",
                "J",
                "rest.collector=set",
                });
            }
        
        // property NonOptimizedQueryAverageMillis
            {
            mapInfo.put("NonOptimizedQueryAverageMillis", new Object[]
                {
                "The average duration in milliseconds per non-optimized query execution since the statistics were last reset.",
                "getNonOptimizedQueryAverageMillis",
                null,
                "J",
                null,
                });
            }
        
        // property NonOptimizedQueryCount
            {
            mapInfo.put("NonOptimizedQueryCount", new Object[]
                {
                "The total number of queries that could not be resolved or were partially resolved against indexes since statistics were last reset.",
                "getNonOptimizedQueryCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property NonOptimizedQueryTotalMillis
            {
            mapInfo.put("NonOptimizedQueryTotalMillis", new Object[]
                {
                "The total execution time in milliseconds for queries that could not be resolved or were partially resolved against indexes since statistics were last reset.",
                "getNonOptimizedQueryTotalMillis",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property OptimizedQueryAverageMillis
            {
            mapInfo.put("OptimizedQueryAverageMillis", new Object[]
                {
                "The average duration in milliseconds per optimized query execution since the statistics were last reset.",
                "getOptimizedQueryAverageMillis",
                null,
                "J",
                null,
                });
            }
        
        // property OptimizedQueryCount
            {
            mapInfo.put("OptimizedQueryCount", new Object[]
                {
                "The total number of queries that were fully resolved using indexes since statistics were last reset.",
                "getOptimizedQueryCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property OptimizedQueryTotalMillis
            {
            mapInfo.put("OptimizedQueryTotalMillis", new Object[]
                {
                "The total execution time in milliseconds for queries that were fully resolved using indexes since statistics were last reset.",
                "getOptimizedQueryTotalMillis",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property QueryContentionCount
            {
            mapInfo.put("QueryContentionCount", new Object[]
                {
                "Total number of times a query had to be re-evaluated due to a concurrent update since statistics were last reset. This statistics provides a measure of an impact of concurrent updates on the query perfomance. If the total number of queries is Q and the number of contentions is C then the expected performance degradation factor should be no more than (Q + C)/Q.",
                "getQueryContentionCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property RemoveCount
            {
            mapInfo.put("RemoveCount", new Object[]
                {
                "The number of removes from the backing map managed by this StorageManager caused by operations such as clear, remove or invoke.  The remove count is used to audit the cache size in a static system.  Cache Size =  Insert Count - Remove Count - Eviction count.  Therefore the remove count is not reset by the reset statistics method.",
                "getRemoveCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }

        // property ClearCount
            {
            mapInfo.put("ClearCount", new Object[]
                {
                "The number of clear() operations since the last time statistics were reset.",
                "getClearCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TriggerInfo
            {
            mapInfo.put("TriggerInfo", new Object[]
                {
                "An array of information for each trigger applied to the portion of the partitioned cache managed by the StorageManager. Each element is a string value that represents a human-readable description of the corresponding MapTrigger.",
                "getTriggerInfo",
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
                "Reset the storage statistics.  This method does not reset the EvictionCount, InsertCount or RemoveCount attributes.",
                "resetStatistics",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        // behavior clearCache()
            {
            mapInfo.put("clearCache()", new Object[]
                {
                "Removes all items from this cache. Clearing cache can be both a memory and CPU intensive task.",
                "clearCache",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        // behavior truncateCache()
            {
            mapInfo.put("truncateCache()", new Object[]
                {
                "Removes all items from this cache. The removal of entries caused by this truncate operation will not be observable.",
                "truncateCache",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        // behavior size()
            {
            mapInfo.put("size()", new Object[]
                {
                "Returns the total size of the cache.",
                "size",
                "Ljava/lang/Integer;",
                new String[] {},
                new String[] {},
                null,
                });
            }
        // behavior reportPartitionStats()
            {
            mapInfo.put("reportPartitionStats()", new Object[]
                {
                "Reports the size and count of entries for this cache across partitions. Format can be 'json' or 'csv'. For 'csv' the columns are partition, count, totalSize, maxEntrySize and memberId.",
                "reportPartitionStats",
                "Ljava/lang/Object;",
                new String[] {"sFormat", },
                new String[] {"Ljava/lang/String;"},
                null,
                });
            }

        return mapInfo;
        }
    
    // Accessor for the property "EventInterceptorInfo"
    /**
     * Getter for property EventInterceptorInfo.<p>
    * An array of statistics for events processed by event interceptors.
     */
    public String[] getEventInterceptorInfo()
        {
        return null;
        }
    
    // Accessor for the property "EventsDispatched"
    /**
     * Getter for property EventsDispatched.<p>
    * The total number of events dispatched by the StorageManager since the
    * last time the statistics were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getEventsDispatched()
        {
        return 0L;
        }
    
    // Accessor for the property "EvictionCount"
    /**
     * Getter for property EvictionCount.<p>
    * The number of evictions from the backing map managed by this
    * StorageManager caused by entries expiry or insert operations that would
    * make the underlying backing map to reach its configured size limit.  The
    * eviction count is used to audit the cache size in a static system.  Cache
    * Size =  Insert Count - Remove Count - Eviction count.  Therefore the
    * eviction count is not reset by the reset statistics method.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getEvictionCount()
        {
        return 0L;
        }
    
    // Accessor for the property "IndexInfo"
    /**
     * Getter for property IndexInfo.<p>
    * An array of information for each index applied to the portion of the
    * partitioned cache managed by the StorageManager. Each element is a string
    * value that includes a ValueExtractor description, ordered flag (true to
    * indicate that the contents of the index are ordered; false otherwise),
    * and cardinality (number of unique values indexed).
     */
    public String[] getIndexInfo()
        {
        return null;
        }
    
    // Accessor for the property "IndexingTotalMillis"
    /**
     * Getter for property IndexingTotalMillis.<p>
    * The cumulative duration in milliseconds of index builds since statistics
    * were last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getIndexingTotalMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "IndexTotalUnits"
    /**
     * Getter for property IndexTotalUnits.<p>
    * The total units used by all indices on the associated cache.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getIndexTotalUnits()
        {
        return 0L;
        }
    
    // Accessor for the property "InsertCount"
    /**
     * Getter for property InsertCount.<p>
    * The number of inserts into the backing map managed by this
    * StorageManager. In addition to standard inserts caused by put and invoke
    * operations or synthetic inserts caused by get operations with
    * read-through backing map topology, this counter is incremented when
    * distribution transfers move resources `into` the underlying backing map
    * and is decremented when  distribution transfers move data `out`.  The
    * insert count is used to audit the cache size in a static system.  Cache
    * Size =  Insert Count - Remove Count - Eviction count.  Therefore the
    * insert count is not reset by the reset statistics method.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getInsertCount()
        {
        return 0L;
        }
    
    // Accessor for the property "ListenerFilterCount"
    /**
     * Getter for property ListenerFilterCount.<p>
    * The number of filter-based listeners currently registered with the
    * StorageManager.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public int getListenerFilterCount()
        {
        return 0;
        }

    // Accessor for the property "ClearCount"
    /**
     * Getter for property ClearCount.<p>
     * The number of clear() operations since the last time statistics were reset.
     *
     * @descriptor rest.collector=set,metrics.value=_default
     */
    public long getClearCount()
        {
        return 0L;
        }

    // Accessor for the property "ListenerKeyCount"
    /**
     * Getter for property ListenerKeyCount.<p>
    * The number of key-based listeners currently registered with the
    * StorageManager.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getListenerKeyCount()
        {
        return 0;
        }
    
    // Accessor for the property "ListenerRegistrations"
    /**
     * Getter for property ListenerRegistrations.<p>
    * The total number of listener registration requests processed by the
    * StorageManager since the last time the statistics were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getListenerRegistrations()
        {
        return 0L;
        }
    
    // Accessor for the property "LocksGranted"
    /**
     * Getter for property LocksGranted.<p>
    * The number of locks currently granted for the portion of the partitioned
    * cache managed by the StorageManager.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getLocksGranted()
        {
        return 0;
        }
    
    // Accessor for the property "LocksPending"
    /**
     * Getter for property LocksPending.<p>
    * The number of pending lock requests for the portion of the partitioned
    * cache managed by the StorageManager.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getLocksPending()
        {
        return 0;
        }
    
    // Accessor for the property "MaxQueryDescription"
    /**
     * Getter for property MaxQueryDescription.<p>
    * A string representation of a query with the longest execution time
    * exceeding the MaxQueryThresholdMillis since statistics were last reset.
     */
    public String getMaxQueryDescription()
        {
        return null;
        }
    
    // Accessor for the property "MaxQueryDurationMillis"
    /**
     * Getter for property MaxQueryDurationMillis.<p>
    * The duration in milliseconds of the longest query execution since
    * statistics were last reset.
    * 
    * @descriptor rest.collector=max,metrics.value=_default
     */
    public long getMaxQueryDurationMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "MaxQueryThresholdMillis"
    /**
     * Getter for property MaxQueryThresholdMillis.<p>
    * A query execution threshold in milliseconds The longest query executing
    * longer than this threshold will be reported by the MaxQueryDescription
    * attribute.
    * 
    * @descriptor rest.collector=set
     */
    public long getMaxQueryThresholdMillis()
        {
        return __m_MaxQueryThresholdMillis;
        }
    
    // Accessor for the property "NonOptimizedQueryAverageMillis"
    /**
     * Getter for property NonOptimizedQueryAverageMillis.<p>
    * The average duration in milliseconds per non-optimized query execution
    * since the statistics were last reset.
     */
    public long getNonOptimizedQueryAverageMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "NonOptimizedQueryCount"
    /**
     * Getter for property NonOptimizedQueryCount.<p>
    * The total number of queries that could not be resolved or were partially
    * resolved against indexes since statistics were last reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getNonOptimizedQueryCount()
        {
        return 0L;
        }
    
    // Accessor for the property "NonOptimizedQueryTotalMillis"
    /**
     * Getter for property NonOptimizedQueryTotalMillis.<p>
    * The total execution time in milliseconds for queries that could not be
    * resolved or were partially resolved against indexes since statistics were
    * last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getNonOptimizedQueryTotalMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "OptimizedQueryAverageMillis"
    /**
     * Getter for property OptimizedQueryAverageMillis.<p>
    * The average duration in milliseconds per optimized query execution since
    * the statistics were last reset.
     */
    public long getOptimizedQueryAverageMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "OptimizedQueryCount"
    /**
     * Getter for property OptimizedQueryCount.<p>
    * The total number of queries that were fully resolved using indexes since
    * statistics were last reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getOptimizedQueryCount()
        {
        return 0L;
        }
    
    // Accessor for the property "OptimizedQueryTotalMillis"
    /**
     * Getter for property OptimizedQueryTotalMillis.<p>
    * The total execution time in milliseconds for queries that were fully
    * resolved using indexes since statistics were last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getOptimizedQueryTotalMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "QueryContentionCount"
    /**
     * Getter for property QueryContentionCount.<p>
    * Total number of times a query had to be re-evaluated due to a concurrent
    * update since statistics were last reset. This statistics provides a
    * measure of an impact of concurrent updates on the query perfomance. If
    * the total number of queries is Q and the number of contentions is C then
    * the expected performance degradation factor should be no more than (Q +
    * C)/Q.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getQueryContentionCount()
        {
        return 0L;
        }
    
    // Accessor for the property "RemoveCount"
    /**
     * Getter for property RemoveCount.<p>
    * The number of removes from the backing map managed by this StorageManager
    * caused by operations such as clear, remove or invoke.  The remove count
    * is used to audit the cache size in a static system.  Cache Size =  Insert
    * Count - Remove Count - Eviction count.  Therefore the remove count is not
    * reset by the reset statistics method.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getRemoveCount()
        {
        return 0L;
        }
    
    // Accessor for the property "TriggerInfo"
    /**
     * Getter for property TriggerInfo.<p>
    * An array of information for each trigger applied to the portion of the
    * partitioned cache managed by the StorageManager. Each element is a string
    * value that represents a human-readable description of the corresponding
    * MapTrigger.
     */
    public String[] getTriggerInfo()
        {
        return null;
        }
    
    /**
     * Reset the storage statistics.  This method does not reset the
    * EvictionCount, InsertCount or RemoveCount attributes.
     */
    public void resetStatistics()
        {
        }
    
    /**
     * Clears cache.
     *
     * Invoking the clear() operation against a distributed cache can be both
     * a memory and CPU intensive task and therefore is generally not recommended.
     */
    public void clearCache()
        {
        }

    /**
     * Truncates cache.
     *
     * The removal of entries caused by this truncate operation will not be
     * observable. This includes any registered listeners, triggers, or
     * interceptors. However, a CacheLifecycleEvent is raised to notify
     * subscribers of the execution of this operation.
     */
    public void truncateCache()
        {
        }

    /**
     * Returns the total size of the cache.
     */
    public int size()
        {
        return 0;
        }

    /**
     * Reports the size and count of entries for this cache across partitions.
     *
     * @param sFormat if "native" then the underlying return type is an array of PartitionSize[] is returned. This is
     *                only used for the REST API.
     *                if format is "json" then Json text is returned otherwise "csv" returns a csv list.
     */
    public Object reportPartitionStats(String sFormat)
        {
        return null;
        }

    // Accessor for the property "MaxQueryThresholdMillis"
    /**
     * Setter for property MaxQueryThresholdMillis.<p>
    * A query execution threshold in milliseconds The longest query executing
    * longer than this threshold will be reported by the MaxQueryDescription
    * attribute.
    * 
    * @descriptor rest.collector=set
     */
    public void setMaxQueryThresholdMillis(long cMillis)
        {
        __m_MaxQueryThresholdMillis = cMillis;
        }
    }
