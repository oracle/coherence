
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.StorageManagerModel

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage;

import com.tangosol.internal.util.VersionHelper;

import com.tangosol.net.NamedCache;
import com.tangosol.net.events.internal.StorageDispatcher;

import com.tangosol.net.internal.PartitionSize;
import com.tangosol.net.internal.PartitionSizeAggregator;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.filter.AlwaysFilter;

import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
public class StorageManagerModel
        extends    com.tangosol.coherence.component.net.management.model.LocalModel
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Storage
     *
     * The Storage object associated with this model.
     */
    private transient Storage __m__Storage;
    
    /**
     * Property _StorageRef
     *
     * The Storage object associated with this model, wrapped in a
     * WeakReference to avoid resource leakage.
     */
    private transient java.lang.ref.WeakReference __m__StorageRef;
    
    /**
     * Property MaxQueryThresholdMillis
     *
     */
    private long __m_MaxQueryThresholdMillis;
    
    // Default constructor
    public StorageManagerModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public StorageManagerModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.management.model.localModel.StorageManagerModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/StorageManagerModel".replace('/', '.'));
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
    
    // Accessor for the property "_Storage"
    /**
     * Getter for property _Storage.<p>
    * The Storage object associated with this model.
     */
    public Storage get_Storage()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        // import java.lang.ref.WeakReference;
        
        WeakReference wr = get_StorageRef();
        return wr == null ? null : (Storage) wr.get();
        }
    
    // Accessor for the property "_StorageRef"
    /**
     * Getter for property _StorageRef.<p>
    * The Storage object associated with this model, wrapped in a WeakReference
    * to avoid resource leakage.
     */
    protected java.lang.ref.WeakReference get_StorageRef()
        {
        return __m__StorageRef;
        }
    
    // Accessor for the property "EventInterceptorInfo"
    /**
     * Getter for property EventInterceptorInfo.<p>
    * Statistics for the UEM event dispatcher.
     */
    public String[] getEventInterceptorInfo()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        // import com.tangosol.net.events.internal.StorageDispatcher;
        
        Storage storage = get_Storage();
        StorageDispatcher dispatcher =
            storage == null ? null : (StorageDispatcher) storage.getEventDispatcher();
            
        if (dispatcher == null)
            {
            return new String[0];
            }
        
        return dispatcher.getStats().toStringArray();
        }
    
    // Accessor for the property "EventsDispatched"
    /**
     * Getter for property EventsDispatched.<p>
     */
    public long getEventsDispatched()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsEventsDispatched();
        }
    
    // Accessor for the property "EvictionCount"
    /**
     * Getter for property EvictionCount.<p>
    * The number of evictions from the backing map managed by this
    * StorageManager caused by entries expiry or insert operations that would
    * make the underlying backing map to cross its configured size limit.
     */
    public long getEvictionCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsEvictions().get();
        }
    
    // Accessor for the property "IndexInfo"
    /**
     * Getter for property IndexInfo.<p>
     */
    public String[] getIndexInfo()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        // import com.tangosol.util.MapIndex;
        // import java.util.ArrayList;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Map;
        
        String[] asInfo   = new String[0];
        Storage  storage  = get_Storage();
        Map      mapIndex = storage == null ? null : storage.getIndexMap();
        
        if (mapIndex != null && !mapIndex.isEmpty())
            {  
            List listInfo = new ArrayList(mapIndex.size());
            for (int cAttempts = 4; cAttempts > 0; --cAttempts)
                {
                try
                    {
                    for (Iterator iter = mapIndex.values().iterator(); iter.hasNext();)
                        {
                        MapIndex index = (MapIndex) iter.next();
                        if (index != null)
                            {
                            listInfo.add(index.toString());
                            }
                        }
                    break;
                    }
                catch (ConcurrentModificationException e)
                    {
                    listInfo.clear();
                    }
                }
        
            asInfo = (String[]) listInfo.toArray(asInfo);
            }
        
        return asInfo;
        }
    
    // Accessor for the property "IndexingTotalMillis"
    /**
     * Getter for property IndexingTotalMillis.<p>
    * The total time taken to build all indices on all partitions, whether as a
    * result of addIndex() or re-distribution.
     */
    public long getIndexingTotalMillis()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        
        return storage == null ? -1L : storage.getStatsIndexingTotalMillis().get();
        }
    
    // Accessor for the property "IndexTotalUnits"
    /**
     * Getter for property IndexTotalUnits.<p>
    * The total units used by all indices on the associated cache.
     */
    public long getIndexTotalUnits()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.MapIndex;
        // import com.tangosol.util.SimpleMapIndex;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.Map;
        
        long     cUnits   = 0L;
        Storage  storage  = get_Storage();
        Map      mapIndex = storage == null ? null : storage.getIndexMap();

        if (mapIndex != null && !mapIndex.isEmpty())
            {  
            for (int cAttempts = 4; cAttempts > 0; --cAttempts)
                {
                try
                    {
                    for (Iterator iter = mapIndex.values().iterator(); iter.hasNext();)
                        {
                        MapIndex index = (MapIndex) iter.next();

                        if (index != null)
                            {
                            cUnits += index.getUnits();
                            }
                        }
                    break;
                    }
                catch (ConcurrentModificationException e)
                    {
                    cUnits = 0;
                    }
                }
            }
        
        return cUnits;
        }
    
    // Accessor for the property "InsertCount"
    /**
     * Getter for property InsertCount.<p>
     */
    public long getInsertCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsInserts().get();
        }
    
    // Accessor for the property "ListenerFilterCount"
    /**
     * Getter for property ListenerFilterCount.<p>
     */
    public int getListenerFilterCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        Storage storage = get_Storage();
        Map     map     = storage == null ? null : storage.getListenerMap();
        if (map == null)
            {
            return 0;
            }
        else
            {
            int cListeners = 0;
            for (int cAttempts = 4; cAttempts > 0; --cAttempts)
                {
                try
                    {
                    for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                        {
                        java.util.Map.Entry entry     = (java.util.Map.Entry) iter.next();
                        Map   mapMember = (Map) entry.getValue();
        
                        if (mapMember != null)
                            {
                            cListeners += mapMember.size();
                            }
                        }
                    break;
                    }
                catch (ConcurrentModificationException e)
                    {
                    cListeners = 0;
                    }
                }
        
            return cListeners;
            }
        }
    
    // Accessor for the property "ListenerKeyCount"
    /**
     * Getter for property ListenerKeyCount.<p>
     */
    public int getListenerKeyCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        Storage storage = get_Storage();
        Map     map     = storage == null ? null : storage.getKeyListenerMap();
        if (map == null)
            {
            return 0;
            }
        else
            {
            int cListeners = 0;
            for (int cAttempts = 4; cAttempts > 0; --cAttempts)
                {
                try
                    {
                    for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                        {
                        java.util.Map.Entry entry     = (java.util.Map.Entry) iter.next();
                        Map   mapMember = (Map) entry.getValue();
        
                        if (mapMember != null)
                            {
                            cListeners += mapMember.size();
                            }
                        }
                    break;
                    }
                catch (ConcurrentModificationException e)
                    {
                    cListeners = 0;
                    }
                }
        
            return cListeners;
            }
        }
    
    // Accessor for the property "ListenerRegistrations"
    /**
     * Getter for property ListenerRegistrations.<p>
     */
    public long getListenerRegistrations()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsListenerRegistrations().get();
        }
    
    // Accessor for the property "LocksGranted"
    /**
     * Getter for property LocksGranted.<p>
     */
    public int getLocksGranted()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        // import java.util.Map;
        
        Storage storage  = get_Storage();
        Map     mapLocks = storage == null ? null : storage.getLeaseMap();
        
        return mapLocks == null ? 0 : mapLocks.size();
        }
    
    // Accessor for the property "LocksPending"
    /**
     * Getter for property LocksPending.<p>
     */
    public int getLocksPending()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        // import java.util.List;
        
        Storage storage      = get_Storage();
        List    listRequests = storage == null ? null : storage.getPendingLockRequest();
        
        return listRequests == null ? 0 : listRequests.size();
        }
    
    // Accessor for the property "MaxQueryDescription"
    /**
     * Getter for property MaxQueryDescription.<p>
     */
    public String getMaxQueryDescription()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? canonicalString(null) : storage.getStatsMaxQueryDescription();
        }
    
    // Accessor for the property "MaxQueryDurationMillis"
    /**
     * Getter for property MaxQueryDurationMillis.<p>
     */
    public long getMaxQueryDurationMillis()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsMaxQueryDurationMillis();
        }
    
    // Accessor for the property "MaxQueryThresholdMillis"
    /**
     * The query statistics threshold, defining when a query have been running
    * long enough to be interresting for recording.
     */
    public long getMaxQueryThresholdMillis()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsMaxQueryThresholdMillis();
        }
    
    // Accessor for the property "NonOptimizedQueryAverageMillis"
    /**
     * Getter for property NonOptimizedQueryAverageMillis.<p>
     */
    public long getNonOptimizedQueryAverageMillis()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsNonOptimizedQueryAverageMillis();
        }
    
    // Accessor for the property "NonOptimizedQueryCount"
    /**
     * Getter for property NonOptimizedQueryCount.<p>
     */
    public long getNonOptimizedQueryCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsNonOptimizedQueryCount().get();
        }
    
    // Accessor for the property "NonOptimizedQueryTotalMillis"
    /**
     * Getter for property NonOptimizedQueryTotalMillis.<p>
     */
    public long getNonOptimizedQueryTotalMillis()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsNonOptimizedQueryTotalMillis().get();
        }
    
    // Accessor for the property "OptimizedQueryAverageMillis"
    /**
     * Getter for property OptimizedQueryAverageMillis.<p>
     */
    public long getOptimizedQueryAverageMillis()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsOptimizedQueryAverageMillis();
        }
    
    // Accessor for the property "OptimizedQueryCount"
    /**
     * Getter for property OptimizedQueryCount.<p>
     */
    public long getOptimizedQueryCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsOptimizedQueryCount().get();
        }
    
    // Accessor for the property "OptimizedQueryTotalMillis"
    /**
     * Getter for property OptimizedQueryTotalMillis.<p>
     */
    public long getOptimizedQueryTotalMillis()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsOptimizedQueryTotalMillis().get();
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
     */
    public long getQueryContentionCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsQueryContentionCount().get();
        }
    
    // Accessor for the property "RemoveCount"
    /**
     * Getter for property RemoveCount.<p>
    * The number of removes from the backing map managed by this StorageManager
    * caused by operations such as clear, remove or invoke.
     */
    public long getRemoveCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsRemoves().get();
        }

    // Accessor for the property "ClearCount"
    /**
     * Getter for property ClearCount.<p>
     * The number of `clear` operations since the last time statistics were reset.
     */
    public long getClearCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;

        Storage storage = get_Storage();
        return storage == null ? -1L : storage.getStatsClears().get();
        }

    // Accessor for the property "TriggerInfo"
    /**
     * Getter for property TriggerInfo.<p>
     */
    public String[] getTriggerInfo()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        // import java.util.ArrayList;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Set;
        
        String[] asInfo     = new String[0];
        Storage  storage    = get_Storage();
        Set      setTrigger = storage == null ? null : storage.getTriggerSet();
        
        if (setTrigger != null && !setTrigger.isEmpty())
            {
            List listInfo = new ArrayList(setTrigger.size());
            for (int cAttempts = 4; cAttempts > 0; --cAttempts)
                {
                try
                    {
                    for (Iterator iter = setTrigger.iterator(); iter.hasNext();)
                        {
                        listInfo.add(iter.next().toString());
                        }
                    break;
                    }
                catch (ConcurrentModificationException e)
                    {
                    listInfo.clear();
                    }
                }
        
            asInfo = (String[]) listInfo.toArray(asInfo);
            }
        
        return asInfo;
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        super.readExternal(in);
        
        Map mapSnapshot = get_SnapshotMap();
        
        mapSnapshot.put("EventInterceptorInfo", ExternalizableHelper.readStringArray(in));
        mapSnapshot.put("EventsDispatched", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("EvictionCount", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("IndexInfo", ExternalizableHelper.readStringArray(in));
        mapSnapshot.put("IndexTotalUnits", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("InsertCount", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("ListenerFilterCount", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("ListenerKeyCount", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("ListenerRegistrations", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("LocksGranted", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("LocksPending", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("MaxQueryDescription", ExternalizableHelper.readUTF(in));
        mapSnapshot.put("MaxQueryDurationMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("MaxQueryThresholdMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("NonOptimizedQueryAverageMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("NonOptimizedQueryCount", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("NonOptimizedQueryTotalMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("OptimizedQueryAverageMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("OptimizedQueryCount", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("OptimizedQueryTotalMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("QueryContentionCount", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("RemoveCount", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TriggerInfo", ExternalizableHelper.readStringArray(in));
        
        if (ExternalizableHelper.isVersionCompatible(in, 21, 6, 0))
            {
            mapSnapshot.put("IndexingTotalMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
            }

        // added in 14.1.2.0.0 / 26.06.7 / 23.09.1
        if (ExternalizableHelper.isVersionCompatible(in, VersionHelper.VERSION_23_09_1)
            || ExternalizableHelper.isPatchCompatible(in, VersionHelper.VERSION_14_1_2_0)
            || ExternalizableHelper.isPatchCompatible(in, VersionHelper.VERSION_14_1_1_2206_7))
            {
            mapSnapshot.put("ClearCount", ExternalizableHelper.readLong(in));
            }
        }
    
    public void resetStatistics()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        Storage storage = get_Storage();
        if (storage != null)
            {
            storage.resetStats();
            }
        }

    public void clearCache()
        {
        checkReadOnly("clearCache");
        Storage storage = get_Storage();
        if (storage != null)
            {
            PartitionedCache service = storage.getService();
            if (service != null)
                {
                NamedCache cache = service.ensureCache(storage.getCacheName(), null);
                cache.clear();
                }
            }
        }

    public void truncateCache()
        {
        checkReadOnly("truncateCache");
        Storage storage = get_Storage();
        if (storage != null)
            {
            PartitionedCache service = storage.getService();
            if (service != null)
                {
                NamedCache cache = service.ensureCache(storage.getCacheName(), null);
                cache.truncate();
                }
            }
        }

    public int size()
        {
        Storage storage = get_Storage();
        if (storage != null)
            {
            PartitionedCache service = storage.getService();
            if (service != null)
                {
                return service.ensureCache(storage.getCacheName(), null).size();
                }
            }
        return 0;
        }

    /**
     * Reports the partition stats in the format specified.
     *
     * @param sFormat specified the format of the data required. Valid values are "json", "csv" or "native".
     *                The "native" format is for use internally by the REST API only.
     * @return
     */
    public Object reportPartitionStats(String sFormat)
        {
        Storage storage = get_Storage();
        if (storage != null)
            {
            PartitionedCache service = storage.getService();
            if (service != null)
                {
                NamedCache cache = service.ensureCache(storage.getCacheName(), null);
                
                Set<PartitionSize> setResults = (Set<PartitionSize>) cache.aggregate(AlwaysFilter.INSTANCE(), new PartitionSizeAggregator());

                if ("native".equals(sFormat))
                    {
                    // Return PartitionSize[] as called from REST API
                    return setResults.toArray();
                    }

                // default format to "json". Use an AtomicInteger, so we can use in the lambda as final
                final String[]      asFormats = new String[]{"{\"partitionId\":%d, \"count\": %d, \"totalSize\": %d, \"maxEntrySize\": %d, \"memberId\": %d}", "%d,%d,%d,%d,%d"};
                final AtomicInteger index     = new AtomicInteger(0);
                String              sJoin     = ",\n";
                String              sFinal    = "]";
                StringBuilder       sb        = new StringBuilder();

                if ("csv".equals(sFormat))
                    {
                    index.set(1);
                    sJoin  = "\n";
                    sFinal = "";
                    }
                else
                    {
                    sb.append("[");
                    }
                
                String sResult = setResults.stream().map(v -> String.format(asFormats[index.intValue()], v.getPartitionId(), v.getCount(), v.getTotalSize(), v.getMaxEntrySize(), v.getMemberId()))
                                           .collect(Collectors.joining(sJoin));
                sb.append(sResult);

                return sb.append(sFinal).toString();
                }
            }

        return "[]]";
        }

    // Accessor for the property "_Storage"
    /**
     * Setter for property _Storage.<p>
    * The Storage object associated with this model.
     */
    public void set_Storage(Storage storage)
        {
        // import java.lang.ref.WeakReference;
        
        set_StorageRef(new WeakReference(storage));
        }
    
    // Accessor for the property "_StorageRef"
    /**
     * Setter for property _StorageRef.<p>
    * The Storage object associated with this model, wrapped in a WeakReference
    * to avoid resource leakage.
     */
    protected void set_StorageRef(java.lang.ref.WeakReference refStorage)
        {
        __m__StorageRef = refStorage;
        }
    
    // Accessor for the property "MaxQueryThresholdMillis"
    /**
     * Setter for property MaxQueryThresholdMillis.<p>
     */
    public void setMaxQueryThresholdMillis(long cMillis)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        
        checkReadOnly("setMaxQueryThresholdMillis");
        
        Storage storage = get_Storage();
        if (storage != null)
            {
            storage.setStatsMaxQueryThresholdMillis(cMillis);
            }
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        super.writeExternal(out);
        
        ExternalizableHelper.writeStringArray(out, getEventInterceptorInfo());
        ExternalizableHelper.writeLong(out, getEventsDispatched());
        ExternalizableHelper.writeLong(out, getEvictionCount());
        ExternalizableHelper.writeStringArray(out, getIndexInfo());
        ExternalizableHelper.writeLong(out, getIndexTotalUnits());
        ExternalizableHelper.writeLong(out, getInsertCount());
        ExternalizableHelper.writeInt(out, getListenerFilterCount());
        ExternalizableHelper.writeInt(out, getListenerKeyCount());
        ExternalizableHelper.writeLong(out, getListenerRegistrations());
        ExternalizableHelper.writeInt(out, getLocksGranted());
        ExternalizableHelper.writeInt(out, getLocksPending());
        ExternalizableHelper.writeUTF(out, getMaxQueryDescription());
        ExternalizableHelper.writeLong(out, getMaxQueryDurationMillis());
        ExternalizableHelper.writeLong(out, getMaxQueryThresholdMillis());
        ExternalizableHelper.writeLong(out, getNonOptimizedQueryAverageMillis());
        ExternalizableHelper.writeLong(out, getNonOptimizedQueryCount());
        ExternalizableHelper.writeLong(out, getNonOptimizedQueryTotalMillis());
        ExternalizableHelper.writeLong(out, getOptimizedQueryAverageMillis());
        ExternalizableHelper.writeLong(out, getOptimizedQueryCount());
        ExternalizableHelper.writeLong(out, getOptimizedQueryTotalMillis());
        ExternalizableHelper.writeLong(out, getQueryContentionCount());
        ExternalizableHelper.writeLong(out, getRemoveCount());
        ExternalizableHelper.writeStringArray(out, getTriggerInfo());
        
        // added in 14.1.2.0.0 / 21.06
        if (ExternalizableHelper.isVersionCompatible(out, 21, 6, 0))
            {
            ExternalizableHelper.writeLong(out, getIndexingTotalMillis());
            }

        // added in 14.1.2.0.0 / 22.06.7 / 23.09.1
        if (ExternalizableHelper.isVersionCompatible(out, VersionHelper.VERSION_23_09_1)
            || ExternalizableHelper.isPatchCompatible(out, VersionHelper.VERSION_14_1_2_0)
            || ExternalizableHelper.isPatchCompatible(out, VersionHelper.VERSION_14_1_1_2206_7))
            {
            ExternalizableHelper.writeLong(out, getClearCount());
            }
        }
    }
