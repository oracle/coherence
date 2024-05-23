/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.persistence.PersistentStore;
import com.tangosol.coherence.component.net.Lease;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PartitionControl;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.TracingHelper;
import com.tangosol.internal.util.BMEventFabric;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.HeuristicCommitException;
import com.tangosol.internal.util.KeyIndexManager;
import com.tangosol.internal.util.LockContentionException;
import com.tangosol.internal.util.PartitionedIndexMap;
import com.tangosol.internal.util.QueryResult;
import com.tangosol.internal.util.SimpleBinaryEntry;
import com.tangosol.internal.util.UnsafeSubSet;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;
import com.tangosol.io.nio.ByteBufferManager;
import com.tangosol.io.nio.MappedBufferManager;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.Member;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.cache.BinaryMemoryCalculator;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheMap;
import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.ReadWriteBackingMap;
import com.tangosol.net.events.internal.StorageDispatcher;
import com.tangosol.net.internal.EntryInfo;
import com.tangosol.net.internal.StorageVersion;
import com.tangosol.net.internal.Trint;
import com.tangosol.net.management.Registry;
import com.tangosol.net.partition.ObservableSplittingBackingMap;
import com.tangosol.net.partition.PartitionAwareBackingMap;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.net.partition.PartitionSplittingBackingMap;
import com.tangosol.net.partition.ReadWriteSplittingBackingMap;
import com.tangosol.net.security.LocalPermission;
import com.tangosol.run.component.EventDeathException;
import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.CopyOnWriteLongArray;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.FilterEnumerator;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.ImmutableMultiList;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.LiteSet;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEventTransformer;
import com.tangosol.util.MapIndex;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SafeHashSet;
import com.tangosol.util.SegmentedHashMap;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.SimpleMapIndex;
import com.tangosol.util.Streamer;
import com.tangosol.util.SubSet;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.WrapperObservableMap;
import com.tangosol.util.comparator.EntryComparator;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.IndexAwareExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.FilterTrigger;
import com.tangosol.util.filter.IndexAwareFilter;
import com.tangosol.util.filter.LimitFilter;
import com.tangosol.util.filter.NeverFilter;
import com.tangosol.util.filter.QueryRecorderFilter;
import com.tangosol.util.filter.ScriptFilter;
import com.tangosol.util.filter.WrapperQueryRecorderFilter;
import java.io.File;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.security.auth.Subject;


/**
 * Storage component represents a part of a given named cache maintained by
 * this cluster member.
 *
 * @see PartitionedCache#StorageArray property
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Storage
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.net.BackingMapContext
    {
    // ---- Fields declarations ----

    /**
     * Property AccessAuthorizer
     *
     */
    private com.tangosol.net.security.StorageAccessAuthorizer __m_AccessAuthorizer;

    /**
     * Property AdjustPartitionSize
     *
     * If true, it is the responsibility of the storage to update the
     * partition size as data change. Otherwise BM is PABM and we can get
     * the partition size directly.
     */
    private boolean __m_AdjustPartitionSize;

    /**
     * Property BackingConfigurableCache
     *
     * A ConfigurableCacheMap. It refers to the same reference as the
     * backing map if the backing map is a ConfigurableCacheMap. If the
     * backing map is a ReadWriteBackingMap, it refers to the backing map's
     * internal cache. Otherwise, it is null.
     */
    private transient com.tangosol.net.cache.ConfigurableCacheMap __m_BackingConfigurableCache;

    /**
     * Property BackingInternalCache
     *
     * If the backing map is a ReadWriteBackingMap, it refers to the
     * backing map's internal cache. It allows us to avoid expired entries
     * from causing a CacheStore.load() on read as well as store() and
     * eraase() on synthetic update and remove.
     *
     * If the backing map is not RWBM, this reference is the same as the
     * BackingMap.
     *
     * @see COH-8468
     */
    private com.tangosol.util.ObservableMap __m_BackingInternalCache;

    /**
     * Property BackingMapAction
     *
     * PrivilegedAction to call getBackingMap().
     */
    private java.security.PrivilegedAction __m_BackingMapAction;

    /**
     * Property BackingMapExpiryDelay
     *
     * The default expiry in ms of the configured backing-map if expiry is
     * supported, or CacheMap.EXPIRY_NEVER (-1L) otherwise.
     */
    private transient int __m_BackingMapExpiryDelay;

    /**
     * Property BackingMapInternal
     *
     * The [primary] map of resources maintained by this storage with keys
     * and values being Binary objects.
     */
    private com.tangosol.util.ObservableMap __m_BackingMapInternal;

    /**
     * Property BackupKeyListenerMap
     *
     * A map of backups for key based listener proxies.
     *
     * @see #KeyListenerMap property
     */
    private transient java.util.Map __m_BackupKeyListenerMap;

    /**
     * Property BackupLeaseMap
     *
     * The backup map of leases.
     *
     * @see #LeaseMap property
     */
    private java.util.Map __m_BackupLeaseMap;

    /**
     * Property BackupMap
     *
     * The map of resource backups maintaned by this storage with keys and
     * values being Binary objects.
     *
     * @see #ResourceMap property
     */
    private java.util.Map __m_BackupMap;

    /**
     * Property CacheId
     *
     * Id of the cache this storage represents.
     */
    private long __m_CacheId;

    /**
     * Property CacheName
     *
     * Name of the cache this storage represents.
     */
    private String __m_CacheName;

    /**
     * Property ConfiguredBackupListeners
     *
     * The map of backup map listeners keyed by corresponding backup map
     * references. Used only if the backup map was created by
     * DefaultConfigurableCacheFactory.
     *
     * @see #instantiateBackupMap()
     * @see #ivalidateBackupMap()
     */
    private transient java.util.Map __m_ConfiguredBackupListeners;

    /**
     * Property ConverterKeyDown
     *
     * Cached KeyToInternal converter.
     */
    private transient com.tangosol.util.Converter __m_ConverterKeyDown;

    /**
     * Property ConverterUp
     *
     * Cached ValueFromInternal (same as KeyFromInternal) converter.
     */
    private transient com.tangosol.util.Converter __m_ConverterUp;

    /**
     * Property ConverterValueDown
     *
     * Cached ValueToInternal converter.
     */
    private transient com.tangosol.util.Converter __m_ConverterValueDown;

    /**
     * Property EntryStatusMap
     *
     * The map of keys to their associated EntryStatus.
     */
    private java.util.concurrent.ConcurrentMap __m_EntryStatusMap;

    /**
     * Property EntryToBinaryEntryConverter
     *
     * Converter that produces a read-only $BinaryEntry from a "present"
     * Map$Entry.
     */
    private EntryToBinaryEntryConverter __m_EntryToBinaryEntryConverter;

    /**
     * Property EventDispatcher
     *
     * The BackingMapDispatcher for this Storage, used by EventsHelper.
     *
     * See $EventsHelper#registerStorageDispatcher.
     */
    private com.tangosol.net.events.internal.StorageDispatcher __m_EventDispatcher;

    /**
     * Property EventDispatcherInterceptor
     *
     * An EventInterceptor that is notified as interceptors are added and
     * removed to the StorageDispatcher.
     *
     * @see PartitionedCache.EventsHelper.registerStorageDispatcher
     */
    private DispatcherInterceptor __m_EventDispatcherInterceptor;

    /**
     * Property EvictionTask
     *
     * The task that is sheduled to perform backing map expiry based
     * eviction.
     */
    private EvictionTask __m_EvictionTask;

    /**
     * Property ExpirySliding
     *
     * True iff cache is configured with a non-zero "expiry-delay" and
     * "expiry-sliding" is enabled.
     */
    private boolean __m_ExpirySliding;

    /**
     * Property FilterIdMap
     *
     * The map of FIlter ids keyed by the Member objects with values that
     * are maps of (Filter, Sets of Long filter ids) entries.
     */
    private java.util.Map __m_FilterIdMap;

    /**
     * Property IndexExtractorMap
     *
     * The Map<ValueExtractor, Comparator> containing the indexed
     * extractors on this cache.  Each extractor is associated with a
     * Comparable that is used to sort the index, or null for an unsorted
     * index.  In the case of IndexAwareExtractor, the actual extractor
     * used by the cache index may not be the one held by this map.
     *
     * @see onNotifyServiceJoined()
     */
    private java.util.Map __m_IndexExtractorMap;

    /**
     * Property InternBackupKeys
     *
     * Specifies whether or not to intern Backup Keys.
     */
    private boolean __m_InternBackupKeys;

    /**
     * Property InternPrimaryKeys
     *
     * Specifies whether or not to intern Primary Keys.
     */
    private boolean __m_InternPrimaryKeys;

    /**
     * Property KeyListenerMap
     *
     * A map of key based listener proxies representing service Members
     * that have requested to be notified with MapEvents regarding this
     * cache. The map is keyed by the "listened to" keys and the values are
     * maps of (Member, Boolean) entries.
     */
    private java.util.Map __m_KeyListenerMap;

    /**
     * Property KeyToBinaryEntryConverter
     *
     * Converter that produces a read-only $BinaryEntry from a  binary key.
     */
    private KeyToBinaryEntryConverter __m_KeyToBinaryEntryConverter;

    /**
     * Property LeaseMap
     *
     * The map of leases granted by this storage.
     */
    private java.util.Map __m_LeaseMap;

    /**
     * Property ListenerMap
     *
     * A map of filter based listener proxies representing service Members
     * that have requested to be notified with MapEvents regarding this
     * cache. The map is keyed by the Filter objects and the values are
     * maps of (Member, Boolean) entries. Since null is a valid filter and
     * we are using the ConcurrentHashMap, which doesn't support nulls, the
     * null filter will be replaced with the BINARY_EXISTS tag as a key.
     */
    private transient java.util.Map __m_ListenerMap;

    /**
     * Property MisconfigLoggedBackup
     *
     * Used by movePartition() / insertBackupTransfer() to limit the number
     * of error messages for a misconfigured cache.
     */
    private boolean __m_MisconfigLoggedBackup;

    /**
     * Property MisconfigLoggedPrimary
     *
     * Used by movePartition() / insertPrimaryTransfer() to limit the
     * number of error messages for a misconfigured cache.
     */
    private boolean __m_MisconfigLoggedPrimary;

    /**
     * Property OldValueRequired
     *
     * Specifies whether or not the old value is likely to be accessed
     * either during or post request processing.
     *
     * @volatile
     */
    private volatile transient boolean __m_OldValueRequired;

    /**
     * Property PartitionAwareBackingMap
     *
     * Returns the backing map as a PartitionAwareBackingMap if the backing
     * map is partition-aware; null otherwise.
     */
    private com.tangosol.net.partition.PartitionAwareBackingMap __m_PartitionAwareBackingMap;

    /**
     * Property PartitionAwareBackupMap
     *
     * Returns the backup map as a PartitionAwareBackingMap if the backup
     * map is partition-aware; null otherwise.
     */
    private com.tangosol.net.partition.PartitionAwareBackingMap __m_PartitionAwareBackupMap;

    /**
     * Property PartitionedIndexMap
     *
     * The map of partition indexes maintained by this storage. The keys of
     * the Map are partition IDs, and for each key, the corresponding value
     * stored in the Map is a map of indices for that partition, with
     * ValueExtractor objects as keys and MapIndex objects as values.
     *
     * @see com.tangosol.util.ValueExtractor
     * @see com.tangosol.util.MapIndex
     *
     * @volatile
     */
    private volatile java.util.Map __m_PartitionedIndexMap;

    /**
     * Property PartitionedKeyIndex
     *
     * PartitionAwareBackingMap used as a key partition index. Used iff the
     * ResourceMap itself is not partition aware.
     */
    private com.tangosol.net.partition.PartitionAwareBackingMap __m_PartitionedKeyIndex;

    /**
     * Property PendingLockRequest
     *
     * The queue of pending LockRequest messages.
     */
    private java.util.List __m_PendingLockRequest;

    /**
     * Property Persistent
     *
     * True iff the contents of this Storage should be persisted.
     */
    private boolean __m_Persistent;

    /**
     * Property PotentiallyEvicting
     *
     * Specifies whether or not the backing map is potentially evicting.
     */
    private transient boolean __m_PotentiallyEvicting;

    /**
     * Property PreferPutAllBackup
     *
     * Specifies whether or not the backup backing map prefers putAll to
     * regular put operations.
     */
    private boolean __m_PreferPutAllBackup;

    /**
     * Property PreferPutAllPrimary
     *
     * Specifies whether or not the primary backing map prefers putAll to
     * regular put operations.
     */
    private boolean __m_PreferPutAllPrimary;

    /**
     * Property PrimaryListener
     *
     * Primary storage listener. Used only if a custom backing map manager
     * uses an ObservableMap to implement the [primary] local storage.
     */
    private transient com.tangosol.util.MapListener __m_PrimaryListener;

    /**
     * Property QUERY_AGGREGATE
     *
     * A query mode used for invocation that requires read-only entries
     * that may be left uninitialized.
     */
    public static final int QUERY_AGGREGATE = 4;

    /**
     * Property QUERY_ENTRIES
     *
     * A query mode that requires fully populated read-only entries.
     */
    public static final int QUERY_ENTRIES = 2;

    /**
     * Property QUERY_INVOKE
     *
     * A query mode used for invocation that requires read-write entries
     * that may be left uninitialized.
     */
    public static final int QUERY_INVOKE = 3;

    /**
     * Property QUERY_KEYS
     *
     * A query mode that requires just keys.
     */
    public static final int QUERY_KEYS = 1;

    /**
     * Property QueryRetries
     *
     * Controlls the maximum number of query index retries before falling
     * back on entry by entry evaluation.
     *
     * The undocumented system  property used to set this value is
     * 'tangosol.coherence.query.retry', defaults to Integer.MAX_VALUE.
     */
    private int __m_QueryRetries;

    /**
     * Property QuerySizeCache
     *
     * This cache holds temporary statistics for filter-based requests. The
     * value is a total size in bytes for matching values contained within
     * a single randomly choosen partition.
     */
    private java.util.Map __m_QuerySizeCache;

    /**
     * Property ResourceControlMap
     *
     * Used to control access to keys.
     */
    private com.tangosol.util.ConcurrentMap __m_ResourceControlMap;

    /**
     * Property StatsEventsDispatched
     *
     * The total number of MapEvents dispatched by this Storage.
     */
    private long __m_StatsEventsDispatched;

    /**
     * Property StatsEvictions
     *
     * A counter for the number of evictions from the backing map.
     */
    private transient java.util.concurrent.atomic.AtomicLong __m_StatsEvictions;

    /**
     * Property StatsIndexingTotalMillis
     *
     * Total amount of time it took to build indices since statistics were
     * last reset.
     */
    private java.util.concurrent.atomic.AtomicLong __m_StatsIndexingTotalMillis;

    /**
     * Property StatsInserts
     *
     * A counter for the number of inserts into the backing map.
     * This counter gets incremented during direct inserts caused by put or
     * invoke operations; read-ahead synthetic inserts and data
     * distribution transfers "in". It gets decremented during data
     * distribution transfers "out".
     */
    private transient java.util.concurrent.atomic.AtomicLong __m_StatsInserts;

    /**
     * Property StatsListenerRegistrations
     *
     * The total number of Listener registration requests processed by this
     * Storage.
     */
    private java.util.concurrent.atomic.AtomicLong __m_StatsListenerRegistrations;

    /**
     * Property StatsMaxQueryDescription
     *
     * A string representation of a query with the longest execution time
     * exceeding the MaxQueryThresholdMillis since statistics were last
     * reset.
     */
    private String __m_StatsMaxQueryDescription;

    /**
     * Property StatsMaxQueryDurationMillis
     *
     * The number of milliseconds of the longest running query since
     * statistics were last reset.
     */
    private long __m_StatsMaxQueryDurationMillis;

    /**
     * Property StatsMaxQueryThresholdMillis
     *
     * A query execution threshold in milliseconds The longest query
     * executing longer than this threshold will be reported in  the
     * MaxQueryDescription attribute.
     */
    private long __m_StatsMaxQueryThresholdMillis;

    /**
     * Property StatsNonOptimizedQueryCount
     *
     * Total number of queries that could not be resolved or was partial
     * resolved against indexes since statistics were last reset.
     */
    private java.util.concurrent.atomic.AtomicLong __m_StatsNonOptimizedQueryCount;

    /**
     * Property StatsNonOptimizedQueryTotalMillis
     *
     * Total number of milliseconds for queries that could not be resolved
     * or was partial resolved against indexes since statistics were last
     * reset.
     */
    private transient java.util.concurrent.atomic.AtomicLong __m_StatsNonOptimizedQueryTotalMillis;

    /**
     * Property StatsOptimizedQueryCount
     *
     * Total number of queries that were fully resolved using indexes since
     * statistics were last reset.
     */
    private transient java.util.concurrent.atomic.AtomicLong __m_StatsOptimizedQueryCount;

    /**
     * Property StatsOptimizedQueryTotalMillis
     *
     * The total number of milliseconds for optimized query operations
     * since statistics were last reset.
     */
    private transient java.util.concurrent.atomic.AtomicLong __m_StatsOptimizedQueryTotalMillis;

    /**
     * Property StatsQueryContentionCount
     *
     * Total number of times a query had to be re-evaluated due to a
     * concurrent update since statistics were last reset. This statistics
     * provides a measure of an impact of concurrent updates on the query
     * perfomance. If the total number of queries is Q and the number of
     * contentions is C then the expected performance degradation factor
     * should be no more than (Q + C)/Q.
     */
    private java.util.concurrent.atomic.AtomicLong __m_StatsQueryContentionCount;

    /**
     * Property StatsRemoves
     *
     * A counter for the number of removes from the backing map.
     * This counter gets incremented during direct removes caused by clear,
     * remove or invoke operations.
     */
    private transient java.util.concurrent.atomic.AtomicLong __m_StatsRemoves;

    /**
     * Property StatsClears
     *
     * A counter for the number of clear operations on the backing map.
     * This counter gets incremented during clear operations.
     */
    private transient java.util.concurrent.atomic.AtomicLong __m_StatsClears;

    /**
     * Property TempBinaryEntry
     *
     * A singleton temporary BinaryEntry that is used (solely) by the
     * service thread to minimize garbage creation.
     *
     * WARNING:  THIS SHOULD ONLY BE USED BY SERVICE THREAD!
     */
    private transient BinaryEntry __m_TempBinaryEntry;

    /**
     * Property TriggerSet
     *
     * A set of MapTriggers registered for this cache.
     *
     * @volatile
     */
    private volatile transient java.util.Set __m_TriggerSet;

    /**
     * Property Valid
     *
     * Indicates whether the storage is valid.  If false, this means the
     * storage has not been initialized or it has been invalidated.
     *
     * This property is only modifed on the service thread.
     *
     * @volatile
     *
     * @see #setCacheName
     * @see #invalidate
     */
    private volatile boolean __m_Valid;

    /**
     * Property Version
     *
     * Data structure holding current versions of the backing map, the
     * partitions and corresponding indicies.
     */
    private com.tangosol.net.internal.StorageVersion __m_Version;
    private static com.tangosol.util.ListMap __mapChildren;

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

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
        __mapChildren.put("Advancer", Advancer.get_CLASS());
        __mapChildren.put("BackingManager", BackingManager.get_CLASS());
        __mapChildren.put("BackingMapAction", BackingMapAction.get_CLASS());
        __mapChildren.put("BinaryEntry", BinaryEntry.get_CLASS());
        __mapChildren.put("DeferredEvent", DeferredEvent.get_CLASS());
        __mapChildren.put("EnlistingConverter", EnlistingConverter.get_CLASS());
        __mapChildren.put("EntryStatus", EntryStatus.get_CLASS());
        __mapChildren.put("EntryToBinaryEntryConverter", EntryToBinaryEntryConverter.get_CLASS());
        __mapChildren.put("EvictionTask", EvictionTask.get_CLASS());
        __mapChildren.put("KeyToBinaryEntryConverter", KeyToBinaryEntryConverter.get_CLASS());
        __mapChildren.put("LazyKeySet", LazyKeySet.get_CLASS());
        __mapChildren.put("PrimaryListener", PrimaryListener.get_CLASS());
        __mapChildren.put("Scanner", Scanner.get_CLASS());
        }

    // Default constructor
    public Storage()
        {
        this(null, null, true);
        }

    // Initializing constructor
    public Storage(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setAdjustPartitionSize(true);
            setEntryStatusMap(new java.util.concurrent.ConcurrentHashMap());
            setFilterIdMap(new com.tangosol.util.SafeHashMap());
            setIndexExtractorMap(new com.tangosol.util.SafeHashMap());
            setInternBackupKeys(false);
            setInternPrimaryKeys(false);
            setLeaseMap(new com.tangosol.util.SegmentedHashMap());
            setPartitionedIndexMap(new java.util.concurrent.ConcurrentHashMap());
            setPendingLockRequest(new com.tangosol.util.SafeLinkedList());
            setStatsEvictions(new java.util.concurrent.atomic.AtomicLong());
            setStatsIndexingTotalMillis(new java.util.concurrent.atomic.AtomicLong());
            setStatsInserts(new java.util.concurrent.atomic.AtomicLong());
            setStatsListenerRegistrations(new java.util.concurrent.atomic.AtomicLong());
            setStatsMaxQueryThresholdMillis(30L);
            setStatsNonOptimizedQueryCount(new java.util.concurrent.atomic.AtomicLong());
            setStatsNonOptimizedQueryTotalMillis(new java.util.concurrent.atomic.AtomicLong());
            setStatsOptimizedQueryCount(new java.util.concurrent.atomic.AtomicLong());
            setStatsOptimizedQueryTotalMillis(new java.util.concurrent.atomic.AtomicLong());
            setStatsQueryContentionCount(new java.util.concurrent.atomic.AtomicLong());
            setStatsRemoves(new java.util.concurrent.atomic.AtomicLong());
            setStatsClears(new java.util.concurrent.atomic.AtomicLong());
            setValid(false);
            setVersion(new com.tangosol.net.internal.StorageVersion());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }

        // containment initialization: children
        _addChild(new DispatcherInterceptor("DispatcherInterceptor", this, true), "DispatcherInterceptor");

        // signal the end of the initialization
        set_Constructed(true);
        }

    // Private initializer
    protected void __initPrivate()
        {

        super.__initPrivate();

        // state initialization: private properties
        try
            {
            __m_PotentiallyEvicting = false;
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }

    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
     * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new Storage();
        }

    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
     * Property with auto-generated accessor that returns the Class object
     * for a given component.
     */
    public static Class get_CLASS()
        {
        return Storage.class;
        }

    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design
     * time] parent component.
     *
     * Note: the class generator will ignore any custom implementation for
     * this behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this.get_Parent();
        }

    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
     * [static] children.
     *
     * Note: the class generator will ignore any custom implementation for
     * this behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }

    /**
     * Accumulate the old and the new $MapEvent holders.  $MapEvent holders
     * are logically polymorphic, and can take the types: null, $MapEvent,
     * and List<$MapEvent>
     *
     * @param oEvtHolderOld    the old $MapEvent holder
     * @param oEvtHolderNew   the new $MapEvent holder
     *
     * @return the accumulated $MapEvent holder
     */
    public static Object accumulateMapEvents(Object oEvtHolderOld, Object oEvtHolderNew)
        {
        // import java.util.LinkedList;
        // import java.util.List;

        List listEvents = null;
        if (oEvtHolderOld == null)
            {
            return oEvtHolderNew;
            }
        else if (oEvtHolderNew == null)
            {
            return oEvtHolderOld;
            }
        else if (oEvtHolderOld instanceof PartitionedCache.MapEvent)
            {
            listEvents = new LinkedList();
            listEvents.add(oEvtHolderOld);
            }
        else
            {
            listEvents = (List) oEvtHolderOld;
            }

        if (oEvtHolderNew instanceof PartitionedCache.MapEvent)
            {
            listEvents.add(oEvtHolderNew);
            }
        else
            {
            listEvents.addAll((List) oEvtHolderNew);
            }
        return listEvents;
        }

    /**
     * Populate the specified MapIndex.
     *
     * Called on the service thread only.
     */
    public void addIndex(com.tangosol.util.ValueExtractor extractor)
        {
        // import com.tangosol.net.GuardSupport;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapIndex;
        // import java.util.Iterator;
        // import java.util.Map$Entry as java.util.Map.Entry;

        int          cEntries  = 0;
        BinaryEntry entryTemp = instantiateBinaryEntry(null, null, true);
        for (Iterator iter = getBackingMapInternal().entrySet().iterator(); iter.hasNext();)
            {
            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();

            entryTemp.reset((Binary) entry.getKey(), (Binary) entry.getValue());

            // update partition index
            int      nPart = getService().getKeyPartition(entryTemp.getBinaryKey());
            MapIndex index = (MapIndex) getPartitionIndexMap(nPart).get(extractor);

            if (index != null)
                {
                index.insert(entryTemp);
                }

            // index insertion is a potentially expensive operation;
            // issue a guardian heartbeat every 1024 entries
            if ((++cEntries & 0x3FF) == 0x3FF)
                {
                GuardSupport.heartbeat();
                }
            }
        }

    /**
     * Add an index using the specified extractor and comparator.  A null
     * comparator signifies an unordered index.
     *
     * Called on the service thread only.
     */
    public void addIndex(com.tangosol.coherence.component.net.RequestContext context, com.tangosol.net.partition.PartitionSet partsMask, com.tangosol.util.ValueExtractor extractor, java.util.Comparator comparator)
        {
        // import com.tangosol.net.security.StorageAccessAuthorizer as com.tangosol.net.security.StorageAccessAuthorizer;
        // import com.tangosol.util.extractor.IndexAwareExtractor;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
        // import com.tangosol.util.MapIndex;
        // import com.tangosol.util.ValueExtractor;
        // import java.util.HashMap;
        // import java.util.Map;

        if (!checkIndexExists(extractor, comparator, true))
            {
            checkAccess(context, BinaryEntry.ACCESS_WRITE_ANY, com.tangosol.net.security.StorageAccessAuthorizer.REASON_INDEX_ADD);

            // create index for each partition
            MapIndex index = null;
            for (int iPart = partsMask.next(0); iPart >= 0; iPart = partsMask.next(iPart + 1))
                {
                createMapIndex(getPartitionIndexMap(iPart), extractor, comparator);
                }

            getIndexExtractorMap().put(extractor, comparator);

            persistIndexRegistration(partsMask, extractor, comparator, true);

            if (extractor instanceof IndexAwareExtractor)
                {
                ValueExtractor extractorReal = createMapIndex(new HashMap(), extractor, comparator).getValueExtractor();

                if (extractorReal == null)
                    {
                    getIndexExtractorMap().remove(extractor);

                    throw new RuntimeException("IndexAwareExtractor \"" +
                                               extractor.getClass().getName() +
                                               "\" failed to obtain the underlying extractor");
                    }
                extractor = extractorReal;
                }

            PartitionedCache service = (PartitionedCache) getService();

            if (getService().getDaemonPool().isStarted())
                {
                for (int iPart = partsMask.next(0); iPart >= 0; iPart = partsMask.next(iPart + 1))
                    {
                    service.scheduleInitialIndexUpdate(iPart, com.tangosol.util.MapEvent.ENTRY_INSERTED, this, extractor,
                                                       (MapIndex) getPartitionIndexMap(iPart).get(extractor));
                    }
                }
            else
                {
                // create index synchronously on service thread, see COH-15600
                try
                    {
                    addIndex(extractor);
                    }
                catch (Throwable e)
                    {
                    _trace("Exception occurred during index creation: " + getStackTrace(e), 1);
                    removeIndex(context, partsMask, extractor, comparator);
                    rethrow(e);
                    }
                }
            }
        }

    /**
     * Called on the service or a daemon pool thread after acquiring the key
     * lock.
     */
    public void addKeyListener(com.tangosol.coherence.component.net.Member member, com.tangosol.util.Binary binKey, boolean fLite, boolean fPrimary)
        {
        // intern the binary key
        binKey = getCanonicalKey(binKey);

        addListenerProxy(fPrimary ? ensureKeyListenerMap() : ensureBackupKeyListenerMap(),
                         binKey, member, fLite);

        if (fPrimary)
            {
            // update listener registration stats
            getStatsListenerRegistrations().incrementAndGet();

            if (!fLite)
                {
                // MapEvent.getOldValue is likely to be called
                ensureOldValueRequired();
                }
            }
        }

    /**
     * Called on the service thread only.
     */
    public void addListener(com.tangosol.coherence.component.net.Member member, com.tangosol.util.Filter filter, long lFilterId, boolean fLite)
        {
        // import com.tangosol.util.SafeHashMap;
        // import com.tangosol.util.SafeHashSet;
        // import java.util.Map;
        // import java.util.Set;

        // ListenerMap (ConcurrentHashMap) doesn't support null keys
        Object oFilter = filter == null ? (Object) Binary.EMPTY : filter;

        addListenerProxy(ensureListenerMap(), oFilter, member, fLite);

        Map mapFilterId = getFilterIdMap();

        Map mapMemberFilterId = (Map) mapFilterId.get(member);
        if (mapMemberFilterId == null)
            {
            mapFilterId.put(member, mapMemberFilterId = new SafeHashMap());
            }

        Set setFilterId = (Set) mapMemberFilterId.get(filter);
        if (setFilterId == null)
            {
            mapMemberFilterId.put(filter, setFilterId = new SafeHashSet());
            }

        setFilterId.add(Long.valueOf(lFilterId));

        // update listener registration stats
        getStatsListenerRegistrations().incrementAndGet();

        if (!fLite)
            {
            // MapEvent.getOldValue is likely to be called
            ensureOldValueRequired();
            }
        }

    /**
     * Called on the service or a daemon pool thread.
     */
    protected void addListenerProxy(java.util.Map map, Object anyKey, com.tangosol.coherence.component.net.Member member, boolean fLite)
        {
        // import java.util.concurrent.ConcurrentHashMap;
        // import java.util.Map;

        Map mapMembers = (Map) map.get(anyKey);
        if (mapMembers == null)
            {
            // we use ConcurrentHashMap to prevent the ConcurrentModification
            // during $TransferRequest#write
            map.put(anyKey, mapMembers = new ConcurrentHashMap());
            }

        if (!fLite || !mapMembers.containsKey(member))
            {
            mapMembers.put(member, Boolean.valueOf(fLite));
            }
        }

    /**
     * Called on the service thread only.
     */
    public void addTrigger(com.tangosol.net.partition.PartitionSet partsMask, com.tangosol.util.MapTrigger trigger)
        {
        // import com.tangosol.util.SafeHashSet;
        // import java.util.Set;

        Set set = getTriggerSet();
        if (set == null)
            {
            setTriggerSet(set = new SafeHashSet());
            }
        set.add(trigger);
        }

    protected void adjustStorageStats(com.tangosol.util.MapEvent evt, com.tangosol.net.partition.PartitionStatistics stats)
        {
        // import com.tangosol.net.cache.BinaryMemoryCalculator as com.tangosol.net.cache.BinaryMemoryCalculator;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;

        int nEvtType   = evt.getId();
        com.tangosol.net.cache.BinaryMemoryCalculator calculator = com.tangosol.net.cache.BinaryMemoryCalculator.INSTANCE;

        switch (nEvtType)
            {
            case com.tangosol.util.MapEvent.ENTRY_INSERTED:
                stats.adjustIndirectStorageSize(
                        calculator.calculateUnits((Binary) evt.getKey(), (Binary) evt.getNewValue()));
                break;

            case com.tangosol.util.MapEvent.ENTRY_UPDATED:
                Binary binValueNew = (Binary) evt.getNewValue();
                Binary binValueOld = (Binary) evt.getOldValue();

                stats.adjustIndirectStorageSize(binValueNew.length() - binValueOld.length());
                break;

            case com.tangosol.util.MapEvent.ENTRY_DELETED:
                stats.adjustIndirectStorageSize(
                        -calculator.calculateUnits((Binary) evt.getKey(), (Binary) evt.getOldValue()));
                break;
            }
        }

    public void aggregateByProbe(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent, com.tangosol.net.partition.PartitionSet partMask,
                                 PartitionedCache.AggregateFilterRequest msgRequest, PartitionedCache.PartialValueResponse msgResponse)
        {
        // import com.tangosol.internal.tracing.TracingHelper;
        // import com.tangosol.internal.util.QueryResult;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ImmutableArrayList;
        // import com.tangosol.util.InvocableMap$ParallelAwareAggregator$PartialResultAggregator as com.tangosol.util.InvocableMap.ParallelAwareAggregator.PartialResultAggregator;
        // import java.util.LinkedList;
        // import java.util.List;

        PartitionedCache service     = getService();
        int     cPartitions = service.getPartitionCount();
        long    cbScratch   = service.reserveScratchSpace();
        boolean fPartial    = agent instanceof com.tangosol.util.InvocableMap.ParallelAwareAggregator.PartialResultAggregator;
        Long    cbSize      = (Long) getQuerySizeCache().get(filter);
        long    cbPart      = cbSize == null ? -1L : cbSize.longValue();
        int     nProbePart  = -1;

        if (TracingHelper.isEnabled())
            {
            TracingHelper.augmentSpan()
                    .setMetadata("agent.class", agent.getClass().getName());
            }

        try
            {
            List         listResult = new LinkedList();
            Converter    converter  = service.getBackingMapContext().getValueToInternalConverter();
            PartitionSet partQuery  = new PartitionSet(cPartitions);

            if (cbPart == -1L && !partMask.isEmpty())
                {
                // the size of the filter is unknown or the statistics have expired,
                // (re-)probe a random partition:
                nProbePart = partMask.rnd();

                partQuery.add(nProbePart);

                // temporarly remove it from the partition mask such that it is
                // not reprocessed
                partMask.remove(nProbePart);

                QueryResult result = query(filter, QUERY_ENTRIES, partQuery, msgRequest.checkTimeoutRemaining());
                Object[]    aEntry = result.getResults();

                // since keys are always stored on-heap and seldom deserialized during
                // aggregation there is no need to account for them, we just accumulate
                // the size of the values
                cbPart = 1;

                int cEntries = result.getCount();
                for (int i = 0; i < cEntries; i++)
                    {
                    Binary binValue = ((BinaryEntry) aEntry[i]).getBinaryValue();
                    cbPart += binValue == null ? 0 : binValue.length();
                    }

                Object oResult = agent.aggregate(
                        new ImmutableArrayList(aEntry, 0, cEntries).getSet());
                listResult.add(fPartial ? oResult : converter.convert(oResult));

                getQuerySizeCache().put(filter, Long.valueOf(cbPart));
                }

            // calculate the number of partitions to request for each iteration;
            // current scratch space divided by the number of bytes used by the
            // single partition probe, times 2 to compensate for deserialization
            int cPartsMax = Math.min(cPartitions, Math.max(1, (int) (cbScratch / (cbPart * 2))));
            int nPartNext = partMask.next(0);  // first unprocessed partition

            while (nPartNext >= 0)
                {
                // save the current "position" in case something goes wrong
                int nPartPrev = nPartNext;

                partQuery.clear();
                for (int i = 0; i < cPartsMax && nPartNext >= 0;
                     nPartNext = partMask.next(nPartNext + 1), i++)
                    {
                    partQuery.add(nPartNext);
                    }

                if (partQuery.isEmpty())
                    {
                    // no partitions to query
                    break;
                    }

                try
                    {
                    QueryResult result = query(filter, QUERY_ENTRIES, partQuery, msgRequest.checkTimeoutRemaining());

                    Object oResult = agent.aggregate(
                            new ImmutableArrayList(result.getResults(), 0, result.getCount()).getSet());

                    listResult.add(fPartial ? oResult : converter.convert(oResult));

                    service.checkInterrupt();
                    }
                catch (OutOfMemoryError e)
                    {
                    _trace("Memory exhausted during aggregation of " + partQuery.cardinality()
                           + " partitions: " + filter, 2);

                    if (cPartsMax == 1)
                        {
                        throw e;
                        }
                    cPartsMax = 1;
                    nPartNext = nPartPrev;
                    }
                }

            if (fPartial)
                {
                Object oResult  = null;
                int    cResults = listResult.size();
                if (cResults > 1)
                    {
                    oResult = ((com.tangosol.util.InvocableMap.ParallelAwareAggregator.PartialResultAggregator) agent).aggregatePartialResults(listResult);
                    }
                else if (cResults == 1)
                    {
                    oResult = listResult.get(0);
                    }
                msgResponse.setResult(converter.convert(oResult));
                }
            else
                {
                msgResponse.setCollectionResult(listResult);
                }
            }
        finally
            {
            if (nProbePart != -1)
                {
                // add back the probed partition
                partMask.add(nProbePart);
                }

            service.releaseScratchSpace(cbScratch);
            }
        }

    public Object aggregateByStreaming(com.tangosol.util.Filter filter, InvocableMap.StreamingAggregator agent, com.tangosol.net.partition.PartitionSet partMask, long cTimeoutMillis)
        {
        // import com.tangosol.internal.tracing.Span;
        // import com.tangosol.internal.tracing.TracingHelper;

        Span span = TracingHelper.getActiveSpan();
        if (!TracingHelper.isNoop(span))
            {
            span.setMetadata("agent.class", agent.getClass().getName());
            if (filter != null)
                {
                span.setMetadata("filter", filter.toString());
                }
            }

        // common case optimization
        if (AlwaysFilter.INSTANCE.equals(filter))
            {
            filter = null;
            }

        Object result = null;
        if (agent.isByPartition() && agent.isParallel() && Daemons.isForkJoinPoolEnabled())
            {
            // let's run aggregator in parallel across individual partitions using ForkJoinPool

            Future<Object> future = Daemons.forkJoinPool().submit(new PartitionedAggregateTask<Object>(this, filter, agent, partMask));
            try
                {
                result = cTimeoutMillis == 0L
                         ? future.get()
                         : future.get(cTimeoutMillis, TimeUnit.MILLISECONDS);
                }
            catch (TimeoutException e)
                {
                future.cancel(true);
                throw new RequestTimeoutException("Aggregation request has timed out");
                }
            catch (InterruptedException e)
                {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new RequestTimeoutException("Aggregation request has been interrupted");
                }
            catch (Exception e)
                {
                throw new RuntimeException(e);
                }
            }
        else
            {
            // run aggregator on the current worker thread; the query to create a Streamer
            // may still be run in parallel, if FJP is enabled

            agent.accumulate(createStreamer(filter, agent, partMask));
            result = agent.getPartialResult();
            }

        return result;
        }

    public Object aggregateByStreaming(java.util.Set setKeys, com.tangosol.util.InvocableMap.StreamingAggregator agent)
        {
        // import com.tangosol.internal.tracing.TracingHelper;

        if (TracingHelper.isEnabled())
            {
            TracingHelper.augmentSpan()
                    .setMetadata("agent.class", agent.getClass().getName());
            }

        agent.accumulate(createStreamer(setKeys, agent));

        return agent.getPartialResult();
        }

    /**
     * Index related querying.
     */
    protected com.tangosol.internal.util.QueryResult applyIndex(com.tangosol.util.Filter filter, com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.internal.util.QueryResult;
        // import com.tangosol.internal.util.UnsafeSubSet;
        // import com.tangosol.util.filter.AlwaysFilter;
        // import com.tangosol.util.filter.IndexAwareFilter;
        // import java.util.ConcurrentModificationException;

        Object[]  aoResult        = null;
        Filter<?> filterRemaining = filter;

        if (filter instanceof IndexAwareFilter)
            {
            IndexAwareFilter filterIx = (IndexAwareFilter) filter;

            try
                {
                UnsafeSubSet setKeys  = new UnsafeSubSet(
                        instantiateLazyKeySet(partMask, false), getManagerContext(), partMask);

                // now that we typically query by partition, optimize for a situation
                // when there are no entries in the partition
                if (setKeys.isEmpty())
                    {
                    filterRemaining = null;
                    aoResult        = EMPTY_OBJECT_ARRAY;
                    }
                else
                    {
                    filterRemaining = filterIx.applyIndex(getIndexMap(partMask), setKeys);
                    aoResult        = setKeys.toArray();
                    }
                }
            catch (ConcurrentModificationException e)
                {
                _trace("Excessive concurrent updates while querying "
                       + getCacheName() + ":\n" + getStackTrace(e)
                       + "\nIgnoring exception and running snapshot-based query", 3);

                // Note: there is a slim chance that the CME was caused by the iteration
                // over custom index data structures (OOTB implementations are safe)
                UnsafeSubSet setKeys = new UnsafeSubSet(
                        instantiateLazyKeySet(partMask, true), getManagerContext(), partMask);

                filterRemaining = filterIx.applyIndex(getIndexMap(partMask), setKeys);
                aoResult        = setKeys.toArray();
                }
            catch (Throwable e)
                {
                _trace("Exception occurred during query processing: " + getStackTrace(e), 1);
                rethrow(e);
                }
            }

        return new QueryResult(partMask, aoResult, aoResult == null ? 0 : aoResult.length, filterRemaining);
        }

    /**
     * Return the number of the primary storage keys that belong to the
     * specified PartitionSet.
     *
     * @param fStrict if true, the calculation needs to be precise - all
     * expirted entries need to be evicted
     */
    public int calculateSize(com.tangosol.net.partition.PartitionSet partMask, boolean fStrict)
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.util.Binary;
        // import java.util.ConcurrentModificationException;
        // import java.util.Map;
        // import java.util.Iterator;

        PartitionedCache service = (PartitionedCache) getService();
        com.tangosol.net.partition.PartitionAwareBackingMap    mapKeys = getPartitionAwareBackingMap();

        if (mapKeys == null)
            {
            mapKeys = getPartitionedKeyIndex();

            if (mapKeys != null && fStrict)
                {
                // since we calculate size against the key index only
                // force the eviction if necessary (as LocalCache.size() would)
                ConfigurableCacheMap mapCCM = getBackingConfigurableCache();
                if (mapCCM != null)
                    {
                    mapCCM.evict();

                    // make sure the key index is updated
                    service.processChanges();
                    }
                }
            }

        if (mapKeys != null)
            {
            return mapKeys.getPartitionMap(partMask).size();
            }

        Map mapPrime = getBackingMapInternal();
        if (partMask.equals(service.collectOwnedPartitions(true)))
            {
            return mapPrime.size();
            }
        else
            {
            // since we calculate size against the keySet only
            // force the eviction if necessary (as LocalCache.size() would)
            ConfigurableCacheMap mapCCM = getBackingConfigurableCache();
            if (fStrict && mapCCM != null)
                {
                mapCCM.evict();
                }

            while (true)
                {
                int cSize = 0;
                try
                    {
                    for (Iterator iter = mapPrime.keySet().iterator(); iter.hasNext();)
                        {
                        if (partMask.contains(service.getKeyPartition((Binary) iter.next())))
                            {
                            cSize++;
                            }
                        }
                    return cSize;
                    }
                catch (ConcurrentModificationException ignored) {}
                }
            }
        }

    /**
     * Check access authorization for this storage.
     *
     * @param nAccessRequired - ACCESS_READ_ANY or ACCESS_WRITE_ANY
     */
    public void checkAccess(com.tangosol.coherence.component.net.RequestContext context, int nAccessRequired, int nReason)
        {
        // import com.tangosol.net.security.StorageAccessAuthorizer as com.tangosol.net.security.StorageAccessAuthorizer;
        // import javax.security.auth.Subject;

        com.tangosol.net.security.StorageAccessAuthorizer authorizer = getAccessAuthorizer();
        if (authorizer != null)
            {
            Subject subject = context == null ? null : context.getSubject();

            switch (nAccessRequired)
                {
                case BinaryEntry.ACCESS_READ_ANY:
                    authorizer.checkReadAny(this, subject, nReason);
                    break;

                case BinaryEntry.ACCESS_WRITE_ANY:
                    authorizer.checkWriteAny(this, subject, nReason);
                    break;

                default:
                    throw new IllegalArgumentException("Invalid access: " + nAccessRequired);
                }
            }
        }

    /**
     * Get the submitted (backing map) version after making sure that
     * doBackingMapEvent processing has finished for all the backing map
     * mutations.
     */
    protected long checkBackingMapVersion(java.util.Map mapPrime, com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import java.util.Map;

        com.tangosol.net.partition.PartitionAwareBackingMap pabm = getPartitionAwareBackingMap();
        if (pabm == null)
            {
            synchronized (mapPrime)
                {
                return getVersion().getSubmittedVersion();
                }
            }
        else
            {
            Map map = pabm.getPartitionMap(partMask.first());
            if (map != null)
                {
                synchronized (map)
                    {
                    return getVersion().getSubmittedVersion();
                    }
                }
            }

        // should not happen
        return getVersion().getSubmittedVersion();
        }

    /**
     * Check that the backing map hasn't changed while populating the result
     * with the values - otherwise invalid results may have been added after
     * the keys were matched by the index.
     *
     * If the backing map has changed, the values that are suspect will be
     * rechecked; if a value no longer matches it will be removed from the
     * result.
     *
     * @param filter the IndexAwareFilter used to obtain the matching keys
     * @param aoResult the populated $BinaryEntry or $Status objects
     * @param cResults the number of $BinaryEntry in the aoResult array
     * @param nQueryType one of the QUERY_* values; except QUERY_KEYS since
     * it does not require  consistency
     * @param partMask partitionSet that keys belong to
     * @param lIdxVersion the version of the indicies before the filter was
     * applied
     */
    protected int checkIndexConsistency(com.tangosol.util.Filter filter, Object[] aoResult, int cResults, int nQueryType, com.tangosol.net.partition.PartitionSet partMask, long lIdxVersion)
        {
        // import com.tangosol.net.internal.StorageVersion as com.tangosol.net.internal.StorageVersion;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Binary;
        // import java.util.HashMap;
        // import java.util.Map;

        PartitionedCache service    = (PartitionedCache) get_Module();
        Map     mapPrime   = getBackingMapInternal();
        long    lBMVersion = -1L;
        com.tangosol.net.internal.StorageVersion version    = getVersion();

        // a value of lIdxVersion == -1 means that nQueryType is QUERY_KEYS
        // therefore there is nothing that could have been missed.
        if (lIdxVersion != -1L)
            {
            // make sure that doBackingMapEvent completed for any mutations
            // on the resourceMap.
            // Note: this assumes synchronization during backing map updates
            lBMVersion = checkBackingMapVersion(mapPrime, partMask);
            }

        // check if the backing map has changed after the index was applied; resolve
        // by re-evaluation of the affected keys
        // Note: for QUERY_INVOKE all the keys are already locked
        if (lIdxVersion < lBMVersion)
            {
            partMask = version.getModifiedPartitions(lIdxVersion, partMask);

            Map     mapEval = new HashMap();
            boolean fInvoke = nQueryType == QUERY_INVOKE;

            for (int i = 0; i < cResults; i++)
                {
                Binary binKey;
                int    nPartition;

                if (fInvoke)
                    {
                    EntryStatus status = (EntryStatus) aoResult[i];
                    binKey     = status.getKey();
                    nPartition = status.getPartition();
                    }
                else
                    {
                    binKey     = ((BinaryEntry) aoResult[i]).getBinaryKey();
                    nPartition = service.getKeyPartition(binKey);
                    }

                // collect all the suspect keys in the mapEval
                if (partMask.contains(nPartition))
                    {
                    mapEval.put(binKey, null);
                    aoResult[i] = binKey;
                    }
                }

            if (!mapEval.isEmpty())
                {
                // re-evaluate all the suspect keys again
                reevaluateQueryResults(filter, mapEval, nQueryType, partMask);

                int iW = 0;
                for (int iR = 0; iR < cResults; iR++)
                    {
                    Object oTest = aoResult[iR];
                    if (!(oTest instanceof Binary))
                        {
                        // the original entry is "correct" (BinaryEntry or $Status)
                        aoResult[iW++] = oTest;
                        continue;
                        }

                    Binary binKey = (Binary) oTest;

                    // Note: depending on the query type the values in the mapEval
                    // will be either BinaryEntry or EntryStatus
                    Object oValue = mapEval.get(binKey);
                    if (oValue == null)
                        {
                        // entry doesn't match anymore
                        aoResult[iR] = null;
                        }
                    else
                        {
                        // the original entry is still good; re-insert
                        // at the write-index position
                        aoResult[iW++] = oValue;
                        }
                    }

                cResults = iW;
                }
            }

        return cResults;
        }

    /**
     * Check for the existence of an index for the specified extractor and
     * comparator, throwing IllegalArgumentException if an incompatible
     * index already exists
     *
     * @param extractor        the extractor to check for an existing index
     * @param comparator    the comparator to validate against the existing
     * index, or null
     * @param fValidate         true iff this method should validate the
     * existing index against the specified comparator
     *
     * @return true iff a compatible index exists for the specified
     * extractor and comparator, false if no index exists, or throw if an
     * incompatible index exists
     */
    protected boolean checkIndexExists(com.tangosol.util.ValueExtractor extractor, java.util.Comparator comparator, boolean fValidate)
        {
        // import com.tangosol.util.Base;
        // import java.util.Map;

        Map mapIndexExtractor = getIndexExtractorMap();
        if (!mapIndexExtractor.isEmpty())
            {
            if (mapIndexExtractor.containsKey(extractor))
                {
                // null comparator means unordered index
                if (fValidate && !Base.equals(comparator, mapIndexExtractor.get(extractor)))
                    {
                    // index exists, but with an incompatible comparator
                    throw new IllegalArgumentException("An incompatible index for " + extractor +
                                                       " already exists; remove the index and add it with the new settings");
                    }
                else
                    {
                    return true;
                    }
                }
            }

        return false;
        }

    /**
     * Clear all the keys that belong to the specified partition set. Called
     * on the service or a daemon pool thread after acquiring LOCK_ALL for
     * the storage.
     */
    public void clear(PartitionedCache.InvocationContext ctxInvoke, com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.net.partition.ReadWriteSplittingBackingMap;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.MapTrigger;
        // import com.tangosol.util.NullImplementation;
        // import com.tangosol.util.filter.FilterTrigger;
        // import com.tangosol.util.filter.NeverFilter;
        // import java.util.Collections;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Set;

        // Note: clear() is called while holding LOCK_ALL

        Map        mapResource   = getBackingMapInternal();
        com.tangosol.net.partition.PartitionAwareBackingMap       mapPABM       = getPartitionAwareBackingMap();
        Set        setKeys       = collectKeySet(partMask, false);
        MapTrigger triggerRemove = new FilterTrigger(NeverFilter.INSTANCE,
                                                     FilterTrigger.ACTION_REMOVE_LOGICAL);

        // pass a lazily constructed map to processInterceptors, which will enlist
        // each entry into the provided InvocationContext on access of each
        // element in the collection; this will reduce memory pressure if no
        // interceptors are present; the EnlistingConverter also calls the provided
        // MapTrigger

        // the map results in a Map<Storage, Collection<BinaryEntry>>

        Map mapEntries = Collections.singletonMap(this,
                                                  ConverterCollections.getCollection(setKeys,
                                                                                     instantiateEnlistingConverter(ctxInvoke, triggerRemove),
                                                                                     NullImplementation.getConverter()));

        ctxInvoke.processInterceptors(mapEntries);

        // Note: we can not apply the partitioned map clear optimization for RWBM as
        //       the CacheStore needs to be notified of the removal
        if (mapPABM == null || mapPABM instanceof ReadWriteSplittingBackingMap
            || ctxInvoke.hasEntriesEnlisted(this))
            {
            for (Iterator iter = setKeys.iterator(); iter.hasNext(); )
                {
                Binary binKey = (Binary) iter.next();

                // no need to remove (again) items that were enlisted by
                // the EnlistingConverter above
                if (!ctxInvoke.hasEntryEnlisted(this, binKey))
                    {
                    mapResource.remove(binKey);
                    }
                }
            }
        else
            {
            for (int iPart = partMask.next(0); iPart >= 0; iPart = partMask.next(iPart + 1))
                {
                mapPABM.getPartitionMap(iPart).clear();
                }
            }
        ctxInvoke.postInvoke();
        getStatsClears().incrementAndGet();
        }

    /**
     * Return an array of the primary storage keys that belong to the
     * specified partition.
     *
     * @param aoKeys the array into which the keys are to be copied, if it
     * is big enough; otherwise, a new array will be allocated
     */
    public Object[] collectKeys(int iPartition, Object[] aoKeys)
        {
        // import java.util.Set;

        Set setKeys = getKeySet(iPartition);

        // see comment in collectKeySet()
        return setKeys == null
               ? collectKeySet(iPartition).toArray(aoKeys)
               : setKeys.toArray(aoKeys);
        }

    /**
     * Collect an array of primary storage keys that belong to the specified
     * PartitionSet.
     */
    protected Object[] collectKeys(com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;

        com.tangosol.net.partition.PartitionAwareBackingMap pabmPrime = getPartitionAwareBackingMap();
        if (pabmPrime != null)
            {
            return extractPartitionedKeys(pabmPrime, partMask);
            }

        com.tangosol.net.partition.PartitionAwareBackingMap mapKeyIndex = getPartitionedKeyIndex();
        if (mapKeyIndex != null)
            {
            return extractPartitionedKeys(mapKeyIndex, partMask);
            }

        // this could only happen before the Storage is aware of its name
        return extractKeysDirect(getBackingMapInternal(), partMask).toArray();
        }

    /**
     * Return a subset of the primary storage keys that belong to the
     * specified partition. The returned Set is a always an immutable
     * snapshot of keys.
     */
    public java.util.Set collectKeySet(int iPartition)
        {
        // import com.tangosol.util.ImmutableArrayList;
        // import java.util.Collections;
        // import java.util.Map;
        // import java.util.Set;

        Set setKeys = getKeySet(iPartition);
        if (setKeys == null)
            {
            // this should only happen before the Storage is aware of its name
            // (hence the Storage does not have the "real" resource map yet).
            // This can happen if the ServiceConfigUpdate message sent by the
            // senior in reponse to the StorageIdRequest that creates the
            // cache is received by the client before it is received by this
            // member. The client could then send a request for that cache to
            // this member before the config update arrives and before the
            // "real" backing map has been created.

            Map mapPrime = getBackingMapInternal();
            if (mapPrime.isEmpty())
                {
                // if the primary resource map is empty, there is nothing to
                // do; just return an empty set.
                return Collections.EMPTY_SET;
                }

            // iterate the strorage map and extract the keys the slow way.  Log a
            // trace message here; if we are not here in the small window of time
            // before the Storage knows its name (outlined above), this could be
            // indicative of more serious problem.
            _trace("Collecting keys for partition " + iPartition + ": " + getCacheName(), 4);

            return extractKeysDirect(mapPrime, getService().instantiatePartitionSet(iPartition));
            }
        else
            {
            return new ImmutableArrayList(setKeys.toArray());
            }
        }

    /**
     * Return a subset of the primary storage keys that belong to the
     * specified PartitionSet.
     *
     * @param fSnapshot if true, the returned Set is an immutable snapshot
     * of keys; otherwise it's a read-only view into an underlying
     * PartitionAwareBackingMap
     */
    public java.util.Set collectKeySet(com.tangosol.net.partition.PartitionSet partMask, boolean fSnapshot)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import java.util.Map;

        Map  mapPrime = getBackingMapInternal();
        com.tangosol.net.partition.PartitionAwareBackingMap mapKeys  = getPartitionAwareBackingMap();
        mapKeys  = mapKeys == null ? getPartitionedKeyIndex() : mapKeys;

        if (mapKeys != null)
            {
            return fSnapshot ? extractPartitionedKeySet(mapKeys, partMask)
                             : mapKeys.getPartitionMap(partMask).keySet();
            }

        // this could only happen before the Storage is aware of its name
        return extractKeysDirect(mapPrime, partMask);
        }

    /**
     * Optionally compress the specified result, given the associated old
     * and new values.
     *
     * @param binResult         the result to compress
     * @param binValueOld    the associated "old" value
     * @param binValueNew   the associated "new" value
     */
    public static com.tangosol.util.Binary compressResult(com.tangosol.io.ReadBuffer bufResult, com.tangosol.io.ReadBuffer bufValueOld, com.tangosol.io.ReadBuffer bufValueNew)
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;

        return Base.equals(bufResult, bufValueOld) ? Binary.EMPTY : com.tangosol.util.ExternalizableHelper.asBinary(bufResult);
        }

    public boolean containsKey(com.tangosol.util.Binary binKey)
        {
        // check the resource map here instead of the partitioned
        // key index, as containsKey() should be able to cause expiry
        return getBackingMapInternal().containsKey(binKey);
        }

    public boolean containsValue(com.tangosol.util.Binary binValue)
        {
        return getBackingMapInternal().containsValue(binValue);
        }

    /**
     * Create a lazily deserializing MapEvent.
     *
     * @param binEntry  (optional) allows to re-use already deserialized
     * values if the indexing is on
     */
    protected com.tangosol.util.ConverterCollections.ConverterMapEvent createConverterEvent(int nEventType, com.tangosol.util.Binary binKey, com.tangosol.util.Binary binValueOld, com.tangosol.util.Binary binValueNew, BinaryEntry binEntry)
        {
        // import com.tangosol.net.cache.CacheEvent;
        // import com.tangosol.net.cache.CacheEvent$TransformationState as com.tangosol.net.cache.CacheEvent.TransformationState;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections$ConverterMapEvent as com.tangosol.util.ConverterCollections.ConverterMapEvent;
        // import com.tangosol.util.ConverterCollections as com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.NullImplementation;
        // import com.tangosol.util.ObservableMap;

        Converter     conv     = getConverterUp();
        ObservableMap mapProxy = NullImplementation.getObservableMap();
        CacheEvent evtProxy = new CacheEvent(mapProxy,
                                             nEventType & PartitionedCache.MapEvent.EVT_TYPE_MASK, binKey, binValueOld, binValueNew,
                                             (nEventType & PartitionedCache.MapEvent.EVT_SYNTHETIC) != 0,
                                             com.tangosol.net.cache.CacheEvent.TransformationState.TRANSFORMABLE, false,
                                             (nEventType & PartitionedCache.MapEvent.EVT_EXPIRED) != 0);

        com.tangosol.util.ConverterCollections.ConverterMapEvent evt = (com.tangosol.util.ConverterCollections.ConverterMapEvent) com.tangosol.util.ConverterCollections.getMapEvent(
                mapProxy, evtProxy, conv, conv, getService().getBackingMapContext());
        if (binEntry != null && binEntry.isValueConverted())
            {
            // if possible, inject an already converted value into the event

            Binary binValue = binEntry.getBinaryValue();
            if (binValueNew == binValue)
                {
                evt.setNewValue(binEntry.getValue());
                }
            else if (binValueOld == binValue)
                {
                evt.setOldValue(binEntry.getValue());
                }
            }
        return evt;
        }

    /**
     * Create the index for the  array of entries in the specified
     * partition. Used during index rebuild/recovery or index creation (see
     * onUpdateIndexRequest).
     *
     * @param aEntry the array of  entries to be processed
     * @param cEntries size of the array
     * @param mapIndex the index map that contains a subset of extractors to
     * be processed;
     *                                   if null, all extractors for this
     * Storage are to be processed
     * @param lIdxVersion the committed version before we fetch values from
     * backing map
     *
     * @return null if the index is successfully updated; otherwise a list
     * of "offending" extractors
     */
    protected java.util.List createIndexBatch(BinaryEntry[] aEntry, int cEntries, int nPartition, java.util.Map mapIndex, long lIdxVersion)
        {
        // import com.tangosol.net.internal.StorageVersion;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
        // import java.util.List;
        // import java.util.Map;

        StorageVersion version = getVersion();

        for (int i = 0; i < cEntries; i++)
            {
            List listFailed = updateIndex(com.tangosol.util.MapEvent.ENTRY_INSERTED, aEntry[i], mapIndex);
            if (listFailed != null)
                {
                return listFailed;
                }
            }

        if (!version.isPartitionModified(lIdxVersion, nPartition))
            {
            return null;
            }

        // revert the index changes
        for (int i = 0; i < cEntries; i++)
            {
            updateIndex(com.tangosol.util.MapEvent.ENTRY_DELETED, aEntry[i], mapIndex);
            }

        PartitionedCache service = getService();

        // make sure all potential events caused by mapInternal.get() are processed
        service.processChanges();

        // wait for pending updates to commit
        lIdxVersion = version.waitForPendingCommit(nPartition);

        // slow path; check the version on each update
        Map map = getBackingMapInternal();
        for (int i = 0; i < cEntries; i++)
            {
            BinaryEntry entry  = aEntry[i];

            Binary binKey = entry.getBinaryKey();

            while (true)
                {
                Binary binVal = (Binary) map.get(binKey);
                if (binVal == null)
                    {
                    break;
                    }

                entry.reset(binKey, binVal);

                List listFailed = updateIndex(com.tangosol.util.MapEvent.ENTRY_INSERTED, entry, mapIndex);
                if (listFailed != null)
                    {
                    return listFailed;
                    }

                if (version.isPartitionModified(lIdxVersion, nPartition))
                    {
                    // revert and try again
                    updateIndex(com.tangosol.util.MapEvent.ENTRY_DELETED, entry, mapIndex);
                    service.processChanges();
                    lIdxVersion = version.waitForPendingCommit(nPartition);
                    }
                else
                    {
                    break;
                    }
                }
            }

        // make sure all potential events are processed
        service.processChanges();

        return null;
        }

    /**
     * Create MapIndex for the specified ValueExtractor and add to IndexMap.
     */
    public com.tangosol.util.MapIndex createMapIndex(java.util.Map mapIndex, com.tangosol.util.ValueExtractor extractor, java.util.Comparator comparator)
        {
        // import com.tangosol.util.ForwardOnlyMapIndex;
        // import com.tangosol.util.MapIndex;
        // import com.tangosol.util.SimpleMapIndex;
        // import com.tangosol.util.extractor.IndexAwareExtractor;
        // import com.tangosol.util.extractor.IdentityExtractor;

        boolean  fOrdered = comparator != null;
        MapIndex index;

        if (extractor instanceof IndexAwareExtractor)
            {
            index = ((IndexAwareExtractor) extractor).
                    createIndex(fOrdered, comparator, mapIndex, this);

            if (index == null)
                {
                return null;
                }

            if (!(index instanceof SimpleMapIndex) ||
                !((SimpleMapIndex) index).isForwardIndexSupported())
                {
                // MapEvent.getOldValue is likely to be called
                ensureOldValueRequired();
                }
            }
        else
            {
            index = new SimpleMapIndex(extractor, fOrdered, comparator, this);
            mapIndex.put(extractor, index);
            }

        return index;
        }

    /**
     * Create the index for the specified partition. Used during index
     * rebuild/recovery or index creation (see onUpdateIndexRequest).
     *
     * @param mapIndex the index map that contains a subset of extractors to
     * be processed; if null, all extractors for this Storage are to be
     * processed
     *
     * @return null if the index is successfully updated; otherwise a list
     * of "offending" extractors or an empty list indicating KeyIndexMap is
     * not ready
     */
    public java.util.List createPartitionIndex(int nPartition, java.util.Map mapIndex)
        {
        // import com.tangosol.net.GuardSupport;
        // import com.tangosol.net.internal.StorageVersion;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Map;
        // import java.util.Set;

        StorageVersion version     = getVersion();
        long           lIdxVersion = version.waitForPendingCommit(nPartition);
        Set            setKeys     = getKeySet(nPartition);

        if (setKeys == null)
            {
            // should never happen; see the comments in collectKeySet()
            setKeys = collectKeySet(nPartition);
            }
        else
            {
            // setKeys is a live set; we can safely use it without locking
            // since updates are not allowed yet (see ensureIndexReady)
            }

        int cBatchMax = Math.min(setKeys.size(), 16);
        if (cBatchMax == 0)
            {
            return null;
            }

        // we are processing entries in batches rather than one-by-one
        // to reduce the impact of the StorageVersion check;
        // allocate a temporary array of entries to reduce the garbage amount

        BinaryEntry[] aEntry = new BinaryEntry[cBatchMax];
        for (int i = 0; i < cBatchMax; i++)
            {
            aEntry[i] = instantiateBinaryEntry(null, null, true);
            }

        Map mapInternal = getBackingMapInternal();
        int cProcessed  = 0; // used only for the heartbeat
        int cBatch      = 0;

        for (Iterator iter = setKeys.iterator(); iter.hasNext();)
            {
            Binary binKey = (Binary) iter.next();
            Binary binVal = (Binary) mapInternal.get(binKey);

            if (binVal == null)
                {
                // the entry has been evicted or removed through the back door
                continue;
                }

            aEntry[cBatch++].reset(binKey, binVal);

            if (cBatch == cBatchMax)
                {
                List listFailed = createIndexBatch(aEntry, cBatch, nPartition, mapIndex, lIdxVersion);
                if (listFailed != null)
                    {
                    return listFailed;
                    }

                // updateIndexBatch has called "processChanges", so we can safely wait
                lIdxVersion = version.waitForPendingCommit(nPartition);
                cBatch      = 0;
                }

            // COH-3006: index insertion is a potentially expensive operation;
            //           issue a guardian heartbeat every 1024 entries
            if ((++cProcessed & 0x3FF) == 0x3FF)
                {
                GuardSupport.heartbeat();
                }
            }

        if (cBatch > 0)
            {
            List listFailed = createIndexBatch(aEntry, cBatch, nPartition, mapIndex, lIdxVersion);
            if (listFailed != null)
                {
                return listFailed;
                }
            }

        return null;
        }

    /**
     * Retrieve query results.
     *
     * When calling this method aoResult contains the binary keys for all
     * the entries to instantiate. When returning, it contains the
     * instantiated binary entries or statuses, for all the keys with values
     * matching the filter.
     *
     * @param filterOrig the original filter supplied to query() method
     * @param aoResult passed in binary keys; upon return may contain
     * $BinaryEntry or $Status objects
     * @param nQueryType one of the QUERY_* values
     * @param partMask partitionSet that keys belong to
     * @param lIdxVersion the version of the index as it was before applying
     * the index; -1 means that the query did not use indexes or was
     * QUERY_KEYS type
     *
     * @return the number of "real" results in the aoResult array
     */
    protected int createQueryResult(com.tangosol.util.Filter filterOrig, Object[] aoResult, int nQueryType, com.tangosol.net.partition.PartitionSet partMask, long lIdxVersion)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.filter.IndexAwareFilter;
        // import java.util.Map;

        if (nQueryType == QUERY_KEYS || nQueryType == QUERY_AGGREGATE)
            {
            return aoResult.length;
            }

        Map     mapPrime = getBackingInternalCache();
        int     cResults = 0;
        boolean fInvoke  = nQueryType == QUERY_INVOKE;

        PartitionedCache.InvocationContext ctxInvoke = fInvoke ? getService().getInvocationContext() : null;

        // replace all valid keys with corresponding entries or statuses
        // Note: for QUERY_INVOKE the keys are sorted to avoid a deadldock
        for (int i = 0, c = aoResult.length; i < c; i++)
            {
            Binary binKey   = (Binary) aoResult[i];
            Binary binValue = (Binary) mapPrime.get(binKey);

            if (binValue != null) // must've expired if it's null
                {
                EntryStatus status = fInvoke
                                     ? ctxInvoke.lockEntry(this, binKey, false)
                                     : null;

                BinaryEntry entry  = fInvoke
                                     ? status.getBinaryEntry().setBinaryValue(binValue)
                                     : instantiateBinaryEntry(binKey, binValue, true);

                // Check (after locking the key or creating a read-only copy) that the
                // BinaryEntry still matches the filter.
                //
                // The value might have changed after the initial filtering, so we need to
                // re-read and re-evaluate after we have locked it or copied it (COH-1209, COH-3647)
                if (filterOrig == null || InvocableMapHelper.evaluateEntry(filterOrig, entry))
                    {
                    aoResult[cResults++] = fInvoke ? status : entry;
                    }
                }

            if ((i & 0x3FF) == 0x3FF)
                {
                getService().checkInterrupt();
                }
            }
        return checkIndexConsistency(filterOrig, aoResult, cResults, nQueryType, partMask, lIdxVersion);
        }

    /**
     * Retrieve query results.
     *
     * When calling this method aoResult contains the binary keys for all
     * the entries to instantiate. When returning, it contains the
     * instantiated binary entries or statuses, for all the keys with values
     * matching the filter.
     *
     * @param filter the remaining filter, that does not match any index
     * @param filterOrig the original filter as supplied to query() method
     * @param aoResult passed in binary keys; upon return may contain
     * $BinaryEntry or $Status objects
     * @param nQueryType one of the QUERY_* values
     * @param partMask partitionSet that keys belong to
     * @param lIdxVersion the version of the index as it was before applying
     * the index; -1 means that the query did not use indexes or was
     * QUERY_KEYS type
     *
     * @return the number of "real" results in the aoResult array
     */
    protected int createQueryResult(com.tangosol.util.Filter filter, com.tangosol.util.Filter filterOrig, Object[] aoResult, int nQueryType, com.tangosol.net.partition.PartitionSet partMask, long lIdxVersion)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.filter.IndexAwareFilter;
        // import com.tangosol.util.InvocableMapHelper;
        // import com.tangosol.util.filter.LimitFilter;
        // import com.tangosol.net.partition.PartitionSet;
        // import java.util.Map;

        Map     mapPrime = getBackingInternalCache();
        boolean fKeys    = nQueryType == QUERY_KEYS || nQueryType == QUERY_AGGREGATE;
        boolean fInvoke  = nQueryType == QUERY_INVOKE;
        boolean fSame    = filter == filterOrig;

        // in case of an enclosing LimitFilter we should limit a number
        // of iterations to a minimum (assuming no sorting is required)
        int cLimit = Integer.MAX_VALUE;

        if (filterOrig instanceof LimitFilter)
            {
            LimitFilter filterLimit = (LimitFilter) filterOrig;
            if (nQueryType == QUERY_KEYS || filterLimit.getComparator() == null)
                {
                // see $BinaryMap#querySequential and LimitFilter#extractPage
                Object oAnchorTop = filterLimit.getTopAnchor();
                int    cPageSize  = filterLimit.getPageSize();

                cLimit = cPageSize + (oAnchorTop instanceof Integer
                                      ? ((Integer) oAnchorTop).intValue()
                                      :  filterLimit.getPage() * cPageSize);
                }

            filterOrig = filterLimit.getFilter();
            }

        PartitionedCache.InvocationContext ctxInvoke = fInvoke ? getService().getInvocationContext() : null;
        BinaryEntry tmpEntry = instantiateBinaryEntry(null, null, true);
        int         cResults = 0;

        // Note: for QUERY_INVOKE the keys are sorted to avoid a deadlock
        for (int i = 0, c = aoResult.length; i < c && cResults < cLimit; i++)
            {
            Binary binKey   = (Binary) aoResult[i];
            Binary binValue = (Binary) mapPrime.get(binKey);

            if (binValue != null) // must've expired; we can simply skip it
                {
                // we should only lock and re-evaluate entry if it matches the remaining filter,
                // in order to prevent contention caused by locking all entries outside of index before filter
                // evaluation, as described in COH-5727 (and verified by ContentionTests.testContention)
                if (InvocableMapHelper.evaluateEntry(filter, tmpEntry.reset(binKey, binValue)))
                    {
                    if (fKeys)
                        {
                        // If we are only querying for keys, index consistency isn't an issue, so we are all set
                        aoResult[cResults++] = binKey;
                        }
                    else
                        {
                        // Otherwise, we need to lock the entry for invoke, or create a shallow copy in other cases
                        EntryStatus status = fInvoke
                                             ? ctxInvoke.lockEntry(this, binKey, false)
                                             : null;

                        BinaryEntry entry = fInvoke
                                            ? status.getBinaryEntry().setBinaryValue(binValue)
                                            : instantiateBinaryEntry(binKey, binValue, true);

                        // Note that because we already evaluated tmpEntry, we likely have, and can reuse,
                        // deserialized key and value, in order to avoid double deserialization
                        if (tmpEntry.isKeyConverted())
                            {
                            entry.setConvertedKey(tmpEntry.getConvertedKey())
                                    .setState(entry.getState() | BinaryEntry.KEY_CONVERTED);
                            }
                        if (tmpEntry.isValueConverted())
                            {
                            entry.setConvertedValue(tmpEntry.getConvertedValue())
                                    .setState(entry.getState() | BinaryEntry.VALUE_CONVERTED);
                            }

                        // The value might have changed after the partial index was applied, so we need to
                        // re-evaluate the entry after we have locked it or copied it to make sure
                        // that it still matches the full, original filter (COH-1209, COH-3647).
                        //
                        // However, if the remaining and the original filter are the same, that means that
                        // no indexes were applied, so we don't have to re-evaluate.
                        if (fSame || InvocableMapHelper.evaluateEntry(filterOrig, entry))
                            {
                            aoResult[cResults++] = fInvoke ? status : entry;
                            }
                        }
                    }
                }

            if ((i & 0x3FF) == 0x3FF)
                {
                getService().checkInterrupt();
                }
            }

        return fKeys
               ? cResults
               : checkIndexConsistency(filterOrig, aoResult, cResults, nQueryType, partMask, lIdxVersion);
        }

    /**
     * Create a streamer of $BinaryEntry objects for a given filter and partition set.
     */
    protected com.tangosol.util.Streamer createStreamer(com.tangosol.util.Filter filter, InvocableMap.StreamingAggregator agent, com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.internal.util.QueryResult;
        // import com.tangosol.util.InvocableMap$StreamingAggregator as com.tangosol.util.InvocableMap.StreamingAggregator;
        // import com.tangosol.util.SimpleEnumerator;

        if (filter == null || !isIndexed())
            {
            Scanner scanner = (Scanner) _newChild("Scanner");

            scanner.setFilter(filter);
            scanner.setPartitions(partMask);
            scanner.setReuseAllowed(!agent.isRetainsEntries());

            return scanner;
            }
        else
            {
            long lVersion = getVersion().getCommittedVersion();

            QueryResult result = query(filter, QUERY_AGGREGATE, partMask, 0L);

            Advancer advancer = (Advancer) _newChild("Advancer");

            advancer.setIterator(new SimpleEnumerator(result.getResults()));
            advancer.setSize(result.getCount());
            advancer.setFilter(result.getFilterRemaining());
            advancer.setFilterOriginal(filter);
            advancer.setVersion(lVersion);
            advancer.setPresentOnly(true); // filter-based aggregation uses only "present" entries
            advancer.setReuseAllowed(!agent.isRetainsEntries());
            advancer.setCheckVersion(!agent.isAllowInconsistencies() && getQueryRetries() > 0);
            return advancer;
            }
        }

    /**
     * Create a streamer of $BinaryEntry objects for a given key set.
     */
    protected com.tangosol.util.Streamer createStreamer(java.util.Set setKeys, InvocableMap.StreamingAggregator agent)
        {
        // import com.tangosol.util.InvocableMap$StreamingAggregator as com.tangosol.util.InvocableMap.StreamingAggregator;

        Advancer advancer = (Advancer) _newChild("Advancer");

        advancer.setIterator(setKeys.iterator());
        advancer.setVersion(Long.MAX_VALUE);
        advancer.setReuseAllowed(!agent.isRetainsEntries());

        boolean fPresentOnly = agent.isPresentOnly();
        advancer.setPresentOnly(fPresentOnly);

        if (fPresentOnly)
            {
            // the key set may contain non-present entries;
            // use the negative value as an indication that
            // the size could be way off
            advancer.setSize(-setKeys.size());

            // disallow read-through for read-only entries
            getService().getInvocationContext().setAllowReadThrough(false);
            }
        else
            {
            advancer.setSize(setKeys.size());
            }

        return advancer;
        }

    /**
     * Decode the (possibly extant) expiry decoration and return the number
     * of milliseconds remaining until expiry.
     */
    protected long decodeExpiry(com.tangosol.util.Binary binValue)
        {
        // import com.tangosol.net.cache.CacheMap;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;

        long cExpiry = com.tangosol.util.ExternalizableHelper.decodeExpiry(binValue);

        return cExpiry == CacheMap.EXPIRY_NEVER || cExpiry == CacheMap.EXPIRY_DEFAULT
               ? cExpiry : Math.max(cExpiry - getService().getClusterTime(), 1L);
        }

    /**
     * Optionally decompress the specified result, given the associated old
     * and new values.
     *
     * @param binResult         the result to decompress
     * @param binValueOld    the associated "old" value
     * @param binValueNew   the associated "new" value
     */
    public static com.tangosol.util.Binary decompressResult(com.tangosol.util.Binary binResult, com.tangosol.util.Binary binValueOld, com.tangosol.util.Binary binValueNew)
        {
        // import com.tangosol.util.Base;

        return Binary.EMPTY.equals(binResult) ? binValueOld : binResult;
        }

    /**
     * Called on the service thread, daemon pool thread or a write-behind
     * thread to handle a backing map event.
     */
    public void doBackingMapEvent(com.tangosol.util.MapEvent evt)
        {
        // import com.tangosol.internal.util.BMEventFabric;
        // import com.tangosol.internal.util.BMEventFabric$EventHolder as com.tangosol.internal.util.BMEventFabric.EventHolder;
        // import com.tangosol.internal.util.BMEventFabric$EventQueue as com.tangosol.internal.util.BMEventFabric.EventQueue;
        // import com.tangosol.net.cache.CacheEvent;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;

        PartitionedCache service     = getService();
        Binary  binKey      = (Binary) evt.getKey();
        int     iPartition  = service.getKeyPartition(binKey);
        int     nOwner      = service.getPartitionAssignments()[iPartition][0];
        int     nMemberThis = service.getThisMember().getId();
        int     nEvent      = evt.getId();

        if (nOwner == nMemberThis)
            {
            PartitionedCache.ResourceCoordinator coordinator = getResourceCoordinator();
            com.tangosol.internal.util.BMEventFabric.EventQueue                queueByThd  = coordinator.getEventQueue();

            // update the statistics
            switch (nEvent)
                {
                case com.tangosol.util.MapEvent.ENTRY_INSERTED:
                    // update stats even for synthetic inserts (if there is such a thing)
                    getStatsInserts().incrementAndGet();
                    break;

                case com.tangosol.util.MapEvent.ENTRY_DELETED:
                    if (evt instanceof CacheEvent && ((CacheEvent) evt).isSynthetic())
                        {
                        getStatsEvictions().incrementAndGet();
                        }
                    else
                        {
                        getStatsRemoves().incrementAndGet();
                        }
                    // fall-through

                case com.tangosol.util.MapEvent.ENTRY_UPDATED:
                default:
                    // pull the old value iff it is likely to be needed, which will
                    // only be true for deletes or updates
                    if (isOldValueRequired())
                        {
                        evt.getOldValue();
                        }
                    break;
                }

            while (true)
                {
                EntryStatus status = coordinator.ensureStatus(this, binKey);

                synchronized (status)
                    {
                    // check for a race against another thread which might have
                    // concurrently processed events against the same key
                    if (status.isActive()) // implies that the status is still in the map
                        {
                        PartitionedCache.InvocationContext ctxInvoke = service.getInvocationContext();
                        BinaryEntry       binEntry  = status.getBinaryEntry();
                        if (ctxInvoke != null && ctxInvoke.hasEntryEnlisted(this, binKey) &&
                            binEntry != null && !binEntry.isValueChanged() &&
                            nEvent == com.tangosol.util.MapEvent.ENTRY_INSERTED)
                            {
                            // COH-14777: this must be a read-through by get/getAll against RWBM
                            binEntry.updateLoadedValue((Binary) evt.getNewValue());
                            }

                        boolean fExpiryOnly = ctxInvoke != null &&
                                              (ctxInvoke.isReadOnlyRequest() || status.isExpiryOnly()) &&
                                              Base.equals(evt.getOldValue(), evt.getNewValue());

                        if (!fExpiryOnly)
                            {
                            long lPartVersion = getVersion().submit(iPartition);

                            status.addRawMapEvent(evt = evt.with(iPartition, lPartVersion));
                            }

                        BMEventFabric fabric = coordinator.getEventFabric();
                        com.tangosol.internal.util.BMEventFabric.EventHolder   holder = fabric.createEventHolder(
                                status, evt,
                                coordinator.getIdCounter().incrementAndGet());

                        fabric.add(holder, queueByThd, status.getEventQueue());
                        break;
                        }
                    }
                }
            }
        else
            {
            // The observed BM event operates on a key that is not owned.  This can
            // happen if threads (other than service or worker threads) concurrently
            // update the BM while a transfer is in progress.
            //
            // Though it is not correct (COH-6606), drop the key since it is no longer
            // owned.  Other than to prevent the un-owned update, no action can be taken
            // here that is truly correct.  If we drop the key and the transfer either
            // fails or we become the backup, we will have either lost the entry from
            // the cache or corrupted the backup (delta).  On the otherhand, it would
            // clearly be wrong to accept the update.
            //
            // COH-6626 mitigates the risk of this by narrowing the window where the
            // WB-thread could make an un-owned update

            if (nEvent != com.tangosol.util.MapEvent.ENTRY_DELETED)
                {
                Binary binValueOld = (Binary) evt.getOldValue();
                Binary binValueNew = (Binary) evt.getNewValue();

                if (binValueOld != null && binValueNew != null &&
                    (com.tangosol.util.ExternalizableHelper.isDecorated(binValueOld) || com.tangosol.util.ExternalizableHelper.isDecorated(binValueNew)) &&
                    com.tangosol.util.ExternalizableHelper.getUndecorated(binValueOld).equals(com.tangosol.util.ExternalizableHelper.getUndecorated(binValueNew)))
                    {
                    if (com.tangosol.util.ExternalizableHelper.isDecorated(binValueOld, com.tangosol.util.ExternalizableHelper.DECO_STORE) &&
                        !com.tangosol.util.ExternalizableHelper.isDecorated(binValueNew, com.tangosol.util.ExternalizableHelper.DECO_STORE))
                        {
                        // this should be exceedingly unlikely (see RWBM$StoreWrapper.replace)
                        _trace("Asynchronous write-behind operation for key " + binKey
                               + " in partition " + iPartition + " of the "
                               + "partitioned cache \"" + getCacheName()
                               + "\" has completed after the partition has been moved, "
                               + "which may cause a duplicate \"store\" operation by the new owner.\n", 1);
                        }
                    else
                        {
                        // the difference is in decoration only; ignore
                        }
                    }
                else
                    {
                    _trace("An entry was inserted into the backing map for the "
                           + "partitioned cache \"" + getCacheName()
                           + "\" in partition " + iPartition + " which is not owned by this member;"
                           + " the entry will be removed.\n" + evt + '\n' + get_StackTrace(), 1);
                    }

                try
                    {
                    evt.getMap().keySet().remove(binKey);
                    }
                catch (Exception ignored) {}
                }
            }
        }

    /**
     * Encode the expiry and use it to decorate the Binary value (@since 3.1)
     */
    public com.tangosol.util.Binary encodeExpiry(com.tangosol.util.Binary binValue, long cMillis)
        {
        // import com.tangosol.net.cache.CacheMap;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;

        if (cMillis < 0L)
            {
            // negative expiry is unspecified; map it to EXPIRY_NEVER
            cMillis = CacheMap.EXPIRY_NEVER;
            }

        if (cMillis != CacheMap.EXPIRY_DEFAULT &&
            cMillis != CacheMap.EXPIRY_NEVER)
            {
            cMillis += getService().getClusterTime();
            }

        return com.tangosol.util.ExternalizableHelper.asBinary(com.tangosol.util.ExternalizableHelper.encodeExpiry(binValue, cMillis));
        }

    protected java.util.Map ensureBackupKeyListenerMap()
        {
        // import com.tangosol.util.SafeHashMap;
        // import java.util.Map;

        Map map = getBackupKeyListenerMap();
        if (map == null)
            {
            synchronized (this)
                {
                map = getBackupKeyListenerMap();
                if (map == null)
                    {
                    setBackupKeyListenerMap(map = new SafeHashMap());
                    }
                }
            }
        return map;
        }

    /**
     * Called on the Service thread only.
     */
    public void ensureInitialized(String sName)
        {
        ensureInitialized(sName, /*fRegisterExtents*/ false);
        }

    /**
     * Ensure this Storage instance has been initialized,  setting the valid
     * property value to true once complete.
     */
    public void ensureInitialized(String sName, boolean fRegisterExtents)
        {
        // import com.tangosol.net.BackingMapManager as com.tangosol.net.BackingMapManager;
        // import com.tangosol.net.management.Registry;
        // import com.tangosol.util.ObservableMap;
        // import java.util.Map;

        _assert(sName != null);

        if (is_Constructed())
            {
            PartitionedCache service = (PartitionedCache) getService();

            _assert(Thread.currentThread() == service.getThread());

            String sNameOld = getCacheName();
            if (sNameOld != null)
                {
                if (sNameOld.equals(sName))
                    {
                    return;
                    }
                throw new IllegalStateException("Attempt to modify the CacheName: " +
                                                this + " to " + sName);
                }

            instantiateBackingMap(sName);

            if (service.getBackupCount() > 0)
                {
                instantiateBackupMap(sName);
                }

            // Note: defer setting the cache-name until the resource maps have
            //       been constructed to avoid exposing the "default" backing map
            setCacheName(sName);

            if (fRegisterExtents)
                {
                preparePersistentExtent();
                }

            Registry registry = service.getCluster().getManagement();
            if (registry != null)
                {
                // register MBean:
                // type=StorageManager,service=<name>,cache=<name>,nodeId=<id>
                String sBean = new StringBuilder(Registry.STORAGE_MANAGER_TYPE)
                        .append(",service=")
                        .append(service.getServiceName())
                        .append(",cache=")
                        .append(sName)
                        .toString();

                registry.register(registry.ensureGlobalName(sBean), this);
                }

            // register the UEM dispatcher and EventInterceptors
            service.getEventsHelper().onCacheConfigured(this);

            setValid(true);
            }
        }

    protected java.util.Map ensureKeyListenerMap()
        {
        // import com.tangosol.util.SafeHashMap;
        // import java.util.Map;

        Map map = getKeyListenerMap();
        if (map == null)
            {
            synchronized (this)
                {
                map = getKeyListenerMap();
                if (map == null)
                    {
                    setKeyListenerMap(map = new SafeHashMap());
                    }
                }
            }
        return map;
        }

    protected java.util.Map ensureListenerMap()
        {
        // import java.util.concurrent.ConcurrentHashMap;
        // import java.util.Map;

        Map map = getListenerMap();
        if (map == null)
            {
            synchronized (this)
                {
                map = getListenerMap();
                if (map == null)
                    {
                    setListenerMap(map = new ConcurrentHashMap());
                    }
                }
            }
        return map;
        }

    /**
     * Set the OldValueRequired property to true, as MapEvent.getOldValue or
     * BinaryEntry.getOriginalValue is likely to be called.
     */
    public void ensureOldValueRequired()
        {
        // if possible avoid the volatile write
        if (!isOldValueRequired())
            {
            setOldValueRequired(true);
            }
        }

    /**
     * Ensure that the specified PartitionAwareBakingMap contains the
     * specified partition. Called on the Service thread only.
     */
    public static void ensurePartition(com.tangosol.net.partition.PartitionAwareBackingMap mapPartitioned, int iPartition)
        {
        if (mapPartitioned.getPartitionMap(iPartition) == null)
            {
            mapPartitioned.createPartition(iPartition);
            }
        }

    /**
     * Record a projected query execution cost by the given filter.
     *
     * @param filter  the filter
     * @param partMask  the partitions involved in the query
     */
    public com.tangosol.util.QueryRecord.PartialResult explain(com.tangosol.util.Filter filter, com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.internal.util.SimpleQueryContext as com.tangosol.internal.util.SimpleQueryContext;
        // import com.tangosol.util.SimpleQueryRecord$PartialResult as com.tangosol.util.SimpleQueryRecord.PartialResult;
        // import com.tangosol.util.filter.QueryRecorderFilter;
        // import com.tangosol.util.filter.WrapperQueryRecorderFilter;
        // import java.util.Set;

        com.tangosol.internal.util.SimpleQueryContext ctx     = new com.tangosol.internal.util.SimpleQueryContext(this);
        com.tangosol.util.SimpleQueryRecord.PartialResult  result  = new com.tangosol.util.SimpleQueryRecord.PartialResult(ctx, partMask);
        Set     setKeys = collectKeySet(partMask, true);

        if (!setKeys.isEmpty())
            {
            QueryRecorderFilter filterRecorder = filter instanceof QueryRecorderFilter
                                                 ? (QueryRecorderFilter) filter
                                                 : new WrapperQueryRecorderFilter(filter);

            filterRecorder.explain(ctx, result.instantiateExplainStep(filter), setKeys);
            }

        return result;
        }

    /**
     * Sort and extract a subset of entries according to the LimitFilter
     * attributes.
     *
     * @param aEntry an array of $BinaryEntry objects
     */
    protected Object[] extractBinaryEntries(Object[] aEntry, com.tangosol.util.filter.LimitFilter filterLimit)
        {
        // import com.tangosol.util.comparator.EntryComparator;
        // import com.tangosol.util.filter.LimitFilter;
        // import java.util.Arrays;
        // import java.util.Comparator;

        Comparator comparator =
                new EntryComparator(filterLimit.getComparator(), EntryComparator.CMP_ENTRY);

        Arrays.sort(aEntry, comparator);

        // don't modify the passed in filter!!!
        // (client thread might be using it)
        filterLimit = (LimitFilter) filterLimit.clone();
        filterLimit.setComparator(comparator);
        aEntry = filterLimit.extractPage(aEntry);

        return aEntry;
        }

    /**
     * Return an array of keys from specified map by directly emumerating
     * all the keys.
     */
    protected java.util.Set extractKeysDirect(java.util.Map map, com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.util.Binary;
        // import java.util.ConcurrentModificationException;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Set;

        PartitionedCache service = (PartitionedCache) getService();
        Set     setKeys = new HashSet();
        while (true)
            {
            try
                {
                for (Iterator iter = map.keySet().iterator(); iter.hasNext();)
                    {
                    Binary binKey = (Binary) iter.next();

                    if (partMask.contains(service.getKeyPartition(binKey)))
                        {
                        setKeys.add(binKey);
                        }
                    }
                return setKeys;
                }
            catch (ConcurrentModificationException ignored) {}
            }
        }

    /**
     * Extract keys that belong to the specified PartitionSet from the
     * specified PartitionAwareBackingMap.
     */
    private static Object[] extractPartitionedKeys(com.tangosol.net.partition.PartitionAwareBackingMap mapPartitioned, com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.ImmutableMultiList;

        int cPartitions = partMask.cardinality();
        if (cPartitions == 1)
            {
            return mapPartitioned.getPartitionMap(partMask.next(0)).keySet().toArray();
            }
        else if (cPartitions == 0)
            {
            return ClassHelper.VOID;
            }
        else
            {
            Object[][] aaoKeys = new Object[cPartitions][];
            int        cTotal  = 0;
            for (int i = 0, iPartition = partMask.next(0); iPartition >= 0;
                 i++, iPartition = partMask.next(iPartition + 1))
                {
                Object[] ao = mapPartitioned.getPartitionMap(iPartition).keySet().toArray();
                aaoKeys[i] = ao;
                cTotal    += ao.length;
                }
            return ImmutableMultiList.flatten(aaoKeys, cTotal, null);
            }
        }

    /**
     * Extract keys that belong to the specified PartitionSet from the
     * specified PartitionAwareBackingMap.
     */
    private static java.util.Set extractPartitionedKeySet(com.tangosol.net.partition.PartitionAwareBackingMap mapPartitioned, com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.util.ImmutableArrayList;
        // import com.tangosol.util.ImmutableMultiList;
        // import com.tangosol.util.NullImplementation;

        int cPartitions = partMask.cardinality();
        if (cPartitions == 1)
            {
            return new ImmutableArrayList(
                    mapPartitioned.getPartitionMap(partMask.next(0)).keySet().toArray()).getSet();
            }
        else if (cPartitions == 0)
            {
            return NullImplementation.getSet();
            }
        else
            {
            Object[][] aaoKeys = new Object[cPartitions][];
            for (int i = 0, iPartition = partMask.next(0); iPartition >= 0;
                 i++, iPartition = partMask.next(iPartition + 1))
                {
                aaoKeys[i] = mapPartitioned.getPartitionMap(iPartition).keySet().toArray();
                }
            return new ImmutableMultiList(aaoKeys).getSet();
            }
        }

    /**
     * Fire all pending locks for the specified partition. This method is
     * called when a partition ownership changes, which means that
     * processing pending LockRequest will result in a RETRY response.
     * Called on the service thread only.
     */
    protected void firePendingLocks(int iPartition)
        {
        // import com.tangosol.run.component.EventDeathException;
        // import java.util.ArrayList;
        // import java.util.Iterator;
        // import java.util.List;

        PartitionedCache service     = getService();
        List    listFire    = null;
        List    listPending = getPendingLockRequest();
        synchronized (listPending)
            {
            if (!listPending.isEmpty())
                {
                listFire = new ArrayList(listPending.size());
                for (Iterator iter = listPending.iterator(); iter.hasNext();)
                    {
                    PartitionedCache.LockRequest msgLock = (PartitionedCache.LockRequest) iter.next();
                    if (service.getKeyPartition(msgLock.getKey()) == iPartition)
                        {
                        iter.remove();
                        listFire.add(msgLock);
                        }
                    }
                }
            }

        if (listFire != null && !listFire.isEmpty())
            {
            for (Iterator iter = listFire.iterator(); iter.hasNext();)
                {
                PartitionedCache.LockRequest msgLock = (PartitionedCache.LockRequest) iter.next();
                try
                    {
                    // this should result in PartitionedCache.Response.RESULT_RETRY
                    msgLock.onReceived();
                    }
                catch (EventDeathException ignored) {}
                }
            }
        }

    /**
     * Fire all pending locks for the specified key (there could be many due
     * to the "member" lease ownership model). This method is called when a
     * current lease is terminated.
     */
    public void firePendingLocks(com.tangosol.util.Binary binKey)
        {
        // import com.tangosol.run.component.EventDeathException;
        // import java.util.ArrayList;
        // import java.util.Iterator;
        // import java.util.List;

        List listFire    = null;
        List listPending = getPendingLockRequest();
        synchronized (listPending)
            {
            if (!listPending.isEmpty())
                {
                listFire = new ArrayList(listPending.size());
                for (Iterator iter = listPending.iterator(); iter.hasNext();)
                    {
                    PartitionedCache.LockRequest msgLock = (PartitionedCache.LockRequest) iter.next();
                    if (msgLock.getKey().equals(binKey))
                        {
                        iter.remove();
                        listFire.add(msgLock);
                        }
                    }
                }
            }

        if (listFire != null && !listFire.isEmpty())
            {
            PartitionedCache service = getService();
            for (Iterator iter = listFire.iterator(); iter.hasNext();)
                {
                PartitionedCache.LockRequest msgLock = (PartitionedCache.LockRequest) iter.next();
                try
                    {
                    // at least the first one should succeed now
                    if (Thread.currentThread() == service.getThread())
                        {
                        msgLock.onReceived();
                        }
                    else
                        {
                        service.onLockRequest(msgLock);
                        }
                    }
                catch (EventDeathException ignored) {}
                }
            }
        }

    public com.tangosol.util.Binary get(PartitionedCache.InvocationContext ctxInvoke, EntryStatus status, com.tangosol.util.Binary binKey)
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.util.Binary;
        // import java.util.Map;

        Map     mapPrime = getBackingMapInternal();
        Binary  binValue = (Binary) mapPrime.get(binKey);
        boolean fRWBM    = mapPrime instanceof ReadWriteBackingMap;

        if (fRWBM)
            {
            BinaryEntry binEntry = status.getBinaryEntry();
            if (binEntry.isValueLoaded())
                {
                binEntry.ensureReadOnly();
                }
            ctxInvoke.processInterceptors();
            }

        if (fRWBM || isExpirySliding())
            {
            ctxInvoke.postInvoke(); // this commits all entries enlisted with the InvocationContext
            }
        return binValue;

            /*
            // TODO: hraja - let see if we can bring in something like the below

            import com.tangosol.net.cache.ReadWriteBackingMap;
            import com.tangosol.util.Binary;
            import java.util.Map;

            Map          mapPrime = getBackingMapInternal();
            boolean      fRWBM    = mapPrime instanceof ReadWriteBackingMap;
            BinaryEntry binEntry = status.getBinaryEntry();
            Binary       binValue = binEntry.getBinaryValue();

            if (fRWBM)
                {
                if (binEntry.isValueLoaded())
                    {
                    binEntry.ensureReadOnly();
                    }
                ctxInvoke.processInterceptors();
                }

            if (fRWBM || isExpirySliding())
                {
                ctxInvoke.postInvoke(); // this commits all entries enlisted with the InvocationContext
                }
            return binValue;
            */
        }

    // Accessor for the property "AccessAuthorizer"
    /**
     * Getter for property AccessAuthorizer.<p>
     */
    public com.tangosol.net.security.StorageAccessAuthorizer getAccessAuthorizer()
        {
        return __m_AccessAuthorizer;
        }

    /**
     * Retrieve all the entries in the colKey collection, keys that are
     * missing in the backing map will not be part of the resulting map.
     */
    public java.util.Map getAll(PartitionedCache.InvocationContext ctxInvoke, java.util.Collection colKeys)
        {
        // import com.tangosol.net.cache.CacheMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.HashMap;
        // import java.util.Map;

        Map mapPrime = getBackingMapInternal();
        Map mapResult;

        if (mapPrime instanceof CacheMap)
            {
            mapResult = ((CacheMap) mapPrime).getAll(colKeys);
            }
        else
            {
            mapResult = new HashMap(colKeys.size());

            for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
                {
                Binary binKey = (Binary) iter.next();
                Binary binVal = (Binary) mapPrime.get(binKey);

                if (binVal != null)
                    {
                    mapResult.put(binKey, binVal);
                    }
                }
            }

        boolean fRWBM = mapPrime instanceof ReadWriteBackingMap;
        if (fRWBM)
            {
            for (Iterator iter = ctxInvoke.getEntryStatuses().iterator(); iter.hasNext(); )
                {
                EntryStatus status   = (EntryStatus) iter.next();
                BinaryEntry binEntry = status.getBinaryEntry();
                if (binEntry.isValueLoaded())
                    {
                    binEntry.ensureReadOnly();
                    }
                }
            ctxInvoke.processInterceptors();
            }

        if (fRWBM || isExpirySliding())
            {
            ctxInvoke.postInvoke(); // this commits all entries enlisted with the InvocationContext
            }

        return mapResult;
        }

    /**
     * Retrieve all the entries in the partitioned map of keys from backup
     * storage. The provided PartitionSet will be added to if the partition
     * is no longer the backup.
     */
    public java.util.Map getAllFromBackup(java.util.Map mapPartKeys, com.tangosol.net.partition.PartitionSet partsReject)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.util.Binary;
        // import java.util.Collection;
        // import java.util.Iterator;
        // import java.util.HashMap;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        com.tangosol.net.partition.PartitionAwareBackingMap    mapPABM   = getPartitionAwareBackupMap();
        Map     mapBackup = getBackupMap();
        Map     mapResult = null;
        PartitionedCache service   = getService();

        for (Iterator iter = mapPartKeys.entrySet().iterator(); iter.hasNext(); )
            {
            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
            int   iPart = ((Integer) entry.getKey()).intValue();

            Collection colPartKeys = (Collection) entry.getValue();

            Map mapSrc = mapPABM == null ? mapBackup : mapPABM.getPartitionMap(iPart);

            Map mapResultTmp = new HashMap(colPartKeys.size());

            for (Iterator iterKeys = colPartKeys.iterator(); iterKeys.hasNext(); )
                {
                Binary binKey = (Binary) iterKeys.next();
                Binary binVal = (Binary) mapSrc.get(binKey);

                if (binVal != null)
                    {
                    mapResultTmp.put(binKey, binVal);
                    }
                }

            if (service.isBackupOwner(iPart))
                {
                if (mapResult == null)
                    {
                    mapResult = mapResultTmp;
                    }
                else
                    {
                    mapResult.putAll(mapResultTmp);
                    }
                }
            else
                {
                partsReject.add(iPart);
                }
            }

        return mapResult;
        }

    // Accessor for the property "BackingConfigurableCache"
    /**
     * Getter for property BackingConfigurableCache.<p>
     * A ConfigurableCacheMap. It refers to the same reference as the
     * backing map if the backing map is a ConfigurableCacheMap. If the
     * backing map is a ReadWriteBackingMap, it refers to the backing map's
     * internal cache. Otherwise, it is null.
     */
    public com.tangosol.net.cache.ConfigurableCacheMap getBackingConfigurableCache()
        {
        return __m_BackingConfigurableCache;
        }

    // Accessor for the property "BackingInternalCache"
    /**
     * Getter for property BackingInternalCache.<p>
     * If the backing map is a ReadWriteBackingMap, it refers to the backing
     * map's internal cache. It allows us to avoid expired entries from
     * causing a CacheStore.load() on read as well as store() and eraase()
     * on synthetic update and remove.
     *
     * If the backing map is not RWBM, this reference is the same as the
     * BackingMap.
     *
     * @see COH-8468
     */
    public com.tangosol.util.ObservableMap getBackingInternalCache()
        {
        return __m_BackingInternalCache;
        }

    // From interface: com.tangosol.net.BackingMapContext
    // Accessor for the property "BackingMap"
    /**
     * Getter for property BackingMap.<p>
     * The [primary] map of resources maintained by this storage with keys
     * and values being Binary objects.
     */
    public com.tangosol.util.ObservableMap getBackingMap()
        {
        // import com.tangosol.net.security.LocalPermission;

        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            security.checkPermission(LocalPermission.BACKING_MAP);
            }

        return getBackingMapInternal();
        }

    // Accessor for the property "BackingMapAction"
    /**
     * Getter for property BackingMapAction.<p>
     * PrivilegedAction to call getBackingMap().
     */
    public java.security.PrivilegedAction getBackingMapAction()
        {
        return __m_BackingMapAction;
        }

    // From interface: com.tangosol.net.BackingMapContext
    public com.tangosol.util.InvocableMap.Entry getBackingMapEntry(Object oKey)
        {
        throw new IllegalStateException("Context is not transactional");
        }

    // Accessor for the property "BackingMapExpiryDelay"
    /**
     * Getter for property BackingMapExpiryDelay.<p>
     * The default expiry in ms of the configured backing-map if expiry is
     * supported, or CacheMap.EXPIRY_NEVER (-1L) otherwise.
     */
    public int getBackingMapExpiryDelay()
        {
        return __m_BackingMapExpiryDelay;
        }

    // Accessor for the property "BackingMapInternal"
    /**
     * Getter for property BackingMapInternal.<p>
     * The [primary] map of resources maintained by this storage with keys
     * and values being Binary objects.
     */
    public com.tangosol.util.ObservableMap getBackingMapInternal()
        {
        return __m_BackingMapInternal;
        }

    // Accessor for the property "BackupKeyListenerMap"
    /**
     * Getter for property BackupKeyListenerMap.<p>
     * A map of backups for key based listener proxies.
     *
     * @see #KeyListenerMap property
     */
    public java.util.Map getBackupKeyListenerMap()
        {
        return __m_BackupKeyListenerMap;
        }

    // Accessor for the property "BackupLeaseMap"
    /**
     * Getter for property BackupLeaseMap.<p>
     * The backup map of leases.
     *
     * @see #LeaseMap property
     */
    public java.util.Map getBackupLeaseMap()
        {
        return __m_BackupLeaseMap;
        }

    // Accessor for the property "BackupMap"
    /**
     * Getter for property BackupMap.<p>
     * The map of resource backups maintaned by this storage with keys and
     * values being Binary objects.
     *
     * @see #ResourceMap property
     */
    public java.util.Map getBackupMap()
        {
        return __m_BackupMap;
        }

    // Accessor for the property "CacheId"
    /**
     * Getter for property CacheId.<p>
     * Id of the cache this storage represents.
     */
    public long getCacheId()
        {
        return __m_CacheId;
        }

    // From interface: com.tangosol.net.BackingMapContext
    // Accessor for the property "CacheName"
    /**
     * Getter for property CacheName.<p>
     * Name of the cache this storage represents.
     */
    public String getCacheName()
        {
        return __m_CacheName;
        }

    /**
     * Return a canonicalized (interned) binary key thus ensuring all
     * structures that strongly reference the key refer to the same
     * instance.
     *
     * @param binKey  the binary Key
     *
     * @return the interned binary key
     */
    public com.tangosol.util.Binary getCanonicalKey(com.tangosol.util.Binary binKey)
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap as com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.SafeHashMap;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        PartitionedCache service        = getService();
        int     nPartition     = service.getKeyPartition(binKey);
        boolean fPrimary       = service.isPrimaryOwner(nPartition);
        Binary  binKeyInterned = null;
        com.tangosol.net.cache.ConfigurableCacheMap     mapIntern      = null;

        // InternPrimaryKeys is true iff the backing map is com.tangosol.net.partition.PartitionAwareBackingMap;
        // PartitionedKeyIndex is used in all other cases.
        if (fPrimary && isInternPrimaryKeys())
            {
            mapIntern = (com.tangosol.net.cache.ConfigurableCacheMap) ((com.tangosol.net.partition.PartitionAwareBackingMap) getBackingMapInternal()).getPartitionMap(nPartition);
            }
        else if (!fPrimary && isInternBackupKeys())
            {
            mapIntern = (com.tangosol.net.cache.ConfigurableCacheMap) getBackupMap();
            }

        if (mapIntern == null)
            {
            if (fPrimary)
                {
                // check the PartitionedKeyIndex
                Map mapPKI = getPartitionedKeyIndex();
                if (mapPKI != null)
                    {
                    // key and value in the PartitionedKeyIndex are the same
                    binKeyInterned = (Binary) mapPKI.get(binKey);
                    }
                }
            }
        else
            {
            // check the backing/backup maps for a canonical key
            java.util.Map.Entry entry = mapIntern.getCacheEntry(binKey);
            if (entry != null)
                {
                binKeyInterned = (Binary) entry.getKey();
                }
            }

        // check the primary/backup listener maps
        if (binKeyInterned == null)
            {
            SafeHashMap mapListeners = (SafeHashMap) (fPrimary ? getKeyListenerMap() : getBackupKeyListenerMap());
            if (mapListeners != null)
                {
                java.util.Map.Entry entry = mapListeners.getEntry(binKey);
                if (entry != null)
                    {
                    // Bug 35355767 - avoid leaking listeners
                    synchronized (mapListeners)
                        {
                        entry = mapListeners.getEntry(binKey);
                        if (entry != null)
                            {
                            binKey = (Binary) entry.getKey();
                            }
                        }
                    }
                }
            }
        else
            {
            binKey = binKeyInterned;
            }

        return binKey;
        }

    /**
     *  Return a ConfigurableCacheMap for a partition or entire cache if the
     * backing map is not partitioned.
     */
    public com.tangosol.net.cache.ConfigurableCacheMap getConfigurableCacheMap(int iPartition)
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap as com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import java.util.Map;

        com.tangosol.net.partition.PartitionAwareBackingMap pabmPrime = getPartitionAwareBackingMap();
        Map  mapPart   = pabmPrime == null ? null : pabmPrime.getPartitionMap(iPartition);

        return mapPart != null && mapPart instanceof com.tangosol.net.cache.ConfigurableCacheMap ? (com.tangosol.net.cache.ConfigurableCacheMap) mapPart : getBackingConfigurableCache();
        }

    // Accessor for the property "ConfiguredBackupListeners"
    /**
     * Getter for property ConfiguredBackupListeners.<p>
     * The map of backup map listeners keyed by corresponding backup map
     * references. Used only if the backup map was created by
     * DefaultConfigurableCacheFactory.
     *
     * @see #instantiateBackupMap()
     * @see #ivalidateBackupMap()
     */
    public java.util.Map getConfiguredBackupListeners()
        {
        // import java.util.IdentityHashMap;
        // import java.util.Map;

        Map mapListeners = __m_ConfiguredBackupListeners;
        if (mapListeners == null)
            {
            mapListeners = new IdentityHashMap();
            setConfiguredBackupListeners(mapListeners);
            }
        return mapListeners;
        }

    // Accessor for the property "ConverterKeyDown"
    /**
     * Getter for property ConverterKeyDown.<p>
     * Cached KeyToInternal converter.
     */
    public com.tangosol.util.Converter getConverterKeyDown()
        {
        return __m_ConverterKeyDown;
        }

    // Accessor for the property "ConverterUp"
    /**
     * Getter for property ConverterUp.<p>
     * Cached ValueFromInternal (same as KeyFromInternal) converter.
     */
    public com.tangosol.util.Converter getConverterUp()
        {
        return __m_ConverterUp;
        }

    // Accessor for the property "ConverterValueDown"
    /**
     * Getter for property ConverterValueDown.<p>
     * Cached ValueToInternal converter.
     */
    public com.tangosol.util.Converter getConverterValueDown()
        {
        return __m_ConverterValueDown;
        }

    // Accessor for the property "DeserializationAccelerator"
    /**
     * Getter for property DeserializationAccelerator.<p>
     */
    public com.tangosol.util.MapIndex getDeserializationAccelerator()
        {
        // import com.tangosol.util.MapIndex;
        // import com.tangosol.util.extractor.IdentityExtractor;

        return (MapIndex) getIndexMap().get(IdentityExtractor.INSTANCE);
        }

    public com.tangosol.util.MapIndex getDeserializationAccelerator(int nPart)
        {
        // import com.tangosol.util.MapIndex;
        // import com.tangosol.util.extractor.IdentityExtractor;

        return getIndexMap(nPart).get(IdentityExtractor.INSTANCE);
        }

    // Accessor for the property "EntryStatusMap"
    /**
     * Getter for property EntryStatusMap.<p>
     * The map of keys to their associated EntryStatus.
     */
    public java.util.concurrent.ConcurrentMap getEntryStatusMap()
        {
        return __m_EntryStatusMap;
        }

    // Accessor for the property "EntryToBinaryEntryConverter"
    /**
     * Getter for property EntryToBinaryEntryConverter.<p>
     * Converter that produces a read-only $BinaryEntry from a "present"
     * Map$Entry.
     */
    public EntryToBinaryEntryConverter getEntryToBinaryEntryConverter()
        {
        EntryToBinaryEntryConverter converter = __m_EntryToBinaryEntryConverter;
        if (converter == null)
            {
            converter = (EntryToBinaryEntryConverter) _newChild("EntryToBinaryEntryConverter");
            setEntryToBinaryEntryConverter(converter);
            }

        return converter;
        }

    // Accessor for the property "EventDispatcher"
    /**
     * Getter for property EventDispatcher.<p>
     * The BackingMapDispatcher for this Storage, used by EventsHelper.
     *
     * See $EventsHelper#registerStorageDispatcher.
     */
    public com.tangosol.net.events.internal.StorageDispatcher getEventDispatcher()
        {
        return __m_EventDispatcher;
        }

    // Accessor for the property "EventDispatcherInterceptor"
    /**
     * Getter for property EventDispatcherInterceptor.<p>
     * An EventInterceptor that is notified as interceptors are added and
     * removed to the StorageDispatcher.
     *
     * @see PartitionedCache.EventsHelper.registerStorageDispatcher
     */
    public DispatcherInterceptor getEventDispatcherInterceptor()
        {
        return __m_EventDispatcherInterceptor;
        }

    // Accessor for the property "EvictionTask"
    /**
     * Getter for property EvictionTask.<p>
     * The task that is sheduled to perform backing map expiry based
     * eviction.
     */
    public EvictionTask getEvictionTask()
        {
        return __m_EvictionTask;
        }

    // Accessor for the property "FilterIdMap"
    /**
     * Getter for property FilterIdMap.<p>
     * The map of FIlter ids keyed by the Member objects with values that
     * are maps of (Filter, Sets of Long filter ids) entries.
     */
    public java.util.Map getFilterIdMap()
        {
        return __m_FilterIdMap;
        }

    public com.tangosol.util.Binary getFromBackup(com.tangosol.util.Binary binKey)
        {
        // import com.tangosol.util.Binary;

        return (Binary) getBackupMap().get(binKey);
        }

    // Accessor for the property "IndexExtractorMap"
    /**
     * Getter for property IndexExtractorMap.<p>
     * The Map<ValueExtractor, Comparator> containing the indexed extractors
     * on this cache.  Each extractor is associated with a Comparable that
     * is used to sort the index, or null for an unsorted index.  In the
     * case of IndexAwareExtractor, the actual extractor used by the cache
     * index may not be the one held by this map.
     *
     * @see onNotifyServiceJoined()
     */
    public java.util.Map getIndexExtractorMap()
        {
        return __m_IndexExtractorMap;
        }

    // From interface: com.tangosol.net.BackingMapContext
    public java.util.Map getIndexMap()
        {
        return getIndexMap(null);
        }

    // From interface: com.tangosol.net.BackingMapContext
    public java.util.Map getIndexMap(com.tangosol.net.partition.PartitionSet partitions)
        {
        // import com.tangosol.internal.util.PartitionedIndexMap;

        if (partitions == null || partitions.cardinality() > 1)
            {
            return new PartitionedIndexMap(this, getPartitionedIndexMap(), partitions);
            }
        else if (partitions.cardinality() == 1)
            {
            return getPartitionIndexMap(partitions.first());
            }

        return null;
        }

    // From interface: com.tangosol.net.BackingMapContext
    public Map<ValueExtractor, MapIndex> getIndexMap(int nPartition)
        {
        return getPartitionIndexMap(nPartition);
        }

    // Accessor for the property "KeyListenerMap"
    /**
     * Getter for property KeyListenerMap.<p>
     * A map of key based listener proxies representing service Members that
     * have requested to be notified with MapEvents regarding this cache.
     * The map is keyed by the "listened to" keys and the values are maps of
     * (Member, Boolean) entries.
     */
    public java.util.Map getKeyListenerMap()
        {
        return __m_KeyListenerMap;
        }

    /**
     * Return a set of the primary storage keys that belong to the specified
     * partition. The returned set is "live" and can change concurrently.
     * Also, this method can return null if the $Storage does not have the
     * "real" resource map yet.
     */
    protected java.util.Set getKeySet(int iPartition)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import java.util.Map;

        Map  mapPartition = null;
        com.tangosol.net.partition.PartitionAwareBackingMap pabmPrime    = getPartitionAwareBackingMap();

        if (pabmPrime == null)
            {
            com.tangosol.net.partition.PartitionAwareBackingMap mapKeyIndex = getPartitionedKeyIndex();
            if (mapKeyIndex != null)
                {
                mapPartition = mapKeyIndex.getPartitionMap(iPartition);
                }
            }
        else
            {
            mapPartition = pabmPrime.getPartitionMap(iPartition);
            }

        return mapPartition == null ? null : mapPartition.keySet();
        }

    // Accessor for the property "KeyToBinaryEntryConverter"
    /**
     * Getter for property KeyToBinaryEntryConverter.<p>
     * Converter that produces a read-only $BinaryEntry from a  binary key.
     */
    public KeyToBinaryEntryConverter getKeyToBinaryEntryConverter()
        {
        KeyToBinaryEntryConverter converter = __m_KeyToBinaryEntryConverter;
        if (converter == null)
            {
            converter = (KeyToBinaryEntryConverter) _newChild("KeyToBinaryEntryConverter");
            setKeyToBinaryEntryConverter(converter);
            }

        return converter;
        }

    // Accessor for the property "LeaseMap"
    /**
     * Getter for property LeaseMap.<p>
     * The map of leases granted by this storage.
     */
    public java.util.Map getLeaseMap()
        {
        return __m_LeaseMap;
        }

    // Accessor for the property "ListenerMap"
    /**
     * Getter for property ListenerMap.<p>
     * A map of filter based listener proxies representing service Members
     * that have requested to be notified with MapEvents regarding this
     * cache. The map is keyed by the Filter objects and the values are maps
     * of (Member, Boolean) entries. Since null is a valid filter and we are
     * using the ConcurrentHashMap, which doesn't support nulls, the null
     * filter will be replaced with the BINARY_EXISTS tag as a key.
     */
    public java.util.Map getListenerMap()
        {
        return __m_ListenerMap;
        }

    // From interface: com.tangosol.net.BackingMapContext
    public com.tangosol.net.BackingMapManagerContext getManagerContext()
        {
        return getService().getBackingMapContext();
        }

    // Accessor for the property "PartitionAwareBackingMap"
    /**
     * Getter for property PartitionAwareBackingMap.<p>
     * Returns the backing map as a PartitionAwareBackingMap if the backing
     * map is partition-aware; null otherwise.
     */
    public com.tangosol.net.partition.PartitionAwareBackingMap getPartitionAwareBackingMap()
        {
        return __m_PartitionAwareBackingMap;
        }

    // Accessor for the property "PartitionAwareBackupMap"
    /**
     * Getter for property PartitionAwareBackupMap.<p>
     * Returns the backup map as a PartitionAwareBackingMap if the backup
     * map is partition-aware; null otherwise.
     */
    protected com.tangosol.net.partition.PartitionAwareBackingMap getPartitionAwareBackupMap()
        {
        return __m_PartitionAwareBackupMap;
        }

    /**
     * Based on the provided partition id return a map of Binary objects
     * currently stored in the backup map.
     *
     * @param iPartition  the partition id the backup entries should belong
     * to
     *
     * @return a map of backup entries
     */
    public java.util.Map getPartitionedBackupMap(int iPartition)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.util.Binary;
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        PartitionedCache service    = getService();
        com.tangosol.net.partition.PartitionAwareBackingMap    pabmBackup = getPartitionAwareBackupMap();
        Map     mapFrom    = pabmBackup == null ? getBackupMap() : pabmBackup.getPartitionMap(iPartition);
        Map     mapEntries = mapFrom;

        if (pabmBackup == null)
            {
            // if the backup map is not com.tangosol.net.partition.PartitionAwareBackingMap, collect the entries to promote
            mapEntries = new HashMap();
            for (Iterator iter = mapFrom.entrySet().iterator(); iter.hasNext();)
                {
                java.util.Map.Entry  entry  = (java.util.Map.Entry) iter.next();
                Binary binKey = (Binary) entry.getKey();
                Binary binVal = (Binary) entry.getValue();
                if (service.getKeyPartition(binKey) == iPartition)
                    {
                    mapEntries.put(binKey, binVal);
                    }
                }
            }

        return mapEntries;
        }

    // Accessor for the property "PartitionedIndexMap"
    /**
     * Getter for property PartitionedIndexMap.<p>
     * The map of partition indexes maintained by this storage. The keys of
     * the Map are partition IDs, and for each key, the corresponding value
     * stored in the Map is a map of indices for that partition, with
     * ValueExtarctor objects as keys and MapIndex objects as values.
     *
     * @see com.tangosol.util.ValueExtractor
     * @see com.tangosol.util.MapIndex
     *
     * @volatile
     */
    public java.util.Map getPartitionedIndexMap()
        {
        return __m_PartitionedIndexMap;
        }

    // Accessor for the property "PartitionedKeyIndex"
    /**
     * Getter for property PartitionedKeyIndex.<p>
     * PartitionAwareBackingMap used as a key partition index. Used iff the
     * ResourceMap itself is not partition aware.
     */
    public com.tangosol.net.partition.PartitionAwareBackingMap getPartitionedKeyIndex()
        {
        return __m_PartitionedKeyIndex;
        }

    /**
     * Return an index map for a specified partition.
     */
    public java.util.Map getPartitionIndexMap(int nPartition)
        {
        // import java.util.Map as java.util.Map;
        // import java.util.concurrent.ConcurrentHashMap;

        java.util.Map mapIndex     = getPartitionedIndexMap();
        java.util.Map mapPartIndex = (java.util.Map) mapIndex.get(Integer.valueOf(nPartition));

        if (mapPartIndex == null)
            {
            mapPartIndex = new ConcurrentHashMap();
            java.util.Map mapPrev = (java.util.Map) mapIndex.putIfAbsent(Integer.valueOf(nPartition), mapPartIndex);
            if (mapPrev != null)
                {
                return mapPrev;
                }
            }

        return mapPartIndex;
        }

    public java.util.Set getPartitionKeys(int nPartition)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import java.util.Collections;
        // import java.util.Map;

        com.tangosol.net.partition.PartitionAwareBackingMap pabmPrime = getPartitionAwareBackingMap();
        com.tangosol.net.partition.PartitionAwareBackingMap pabmKeys  = pabmPrime == null ? getPartitionedKeyIndex() : pabmPrime;
        Map  mapKeys   = pabmKeys == null ? null : pabmKeys.getPartitionMap(nPartition);

        return mapKeys == null ? Collections.EMPTY_SET : mapKeys.keySet();
        }

    // Accessor for the property "PendingLockRequest"
    /**
     * Getter for property PendingLockRequest.<p>
     * The queue of pending LockRequest messages.
     */
    public java.util.List getPendingLockRequest()
        {
        return __m_PendingLockRequest;
        }

    /**
     * Send a "priming" MapEvent message to the NearCache.
     */
    public Object getPreviousEvents(com.tangosol.coherence.component.net.Member member, com.tangosol.util.Binary binKey, int iPart, long lVersion)
        {
        // import java.util.Collections;

        return getPreviousEvents(member, Collections.singleton(binKey), iPart, lVersion, null);
        }

    /**
     * Send a "priming" MapEvent message to the NearCache.
     */
    public Object getPreviousEvents(com.tangosol.coherence.component.net.Member member, com.tangosol.util.Filter filter, int iPart, long lVersion, boolean fLite, long lFilterId, Object oHolder)
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        // import com.oracle.coherence.persistence.PersistentStore;
        // import com.tangosol.net.partition.VersionAwareMapListener as com.tangosol.net.partition.VersionAwareMapListener;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
        // import java.util.Collections;
        // import java.util.Iterator;
        // import java.util.TreeMap;

        PartitionedCache.PartitionControl ctrlPart    = (PartitionedCache.PartitionControl) getService().getPartitionControl(iPart);
        PersistentStore storeEvents = ctrlPart.getPersistentEventsStore();

        if (lVersion == com.tangosol.net.partition.VersionAwareMapListener.HEAD)
            {
            // return a synthetic event that notifies the client of the latest event version
            lVersion = getVersion().getSubmittedVersion(iPart);
            if (lVersion == 0L)
                {
                // no changes on this partition slice therefore respond with com.tangosol.net.partition.VersionAwareMapListener.ALL
                // to ensure that if the client does not recieve any events prior to disconnect,
                // upon re-registration it will request all events that occurred
                lVersion = com.tangosol.net.partition.VersionAwareMapListener.ALL;
                }

            return prepareEventMessage(SingleMemberSet.instantiate(member),
                    /*nEventId*/   com.tangosol.util.MapEvent.ENTRY_UPDATED | PartitionedCache.MapEvent.EVT_SYNTHETIC | PartitionedCache.MapEvent.EVT_PRIMING,
                    /*key*/        null,
                    /*oldValue*/   null,
                    /*binValue*/   null,
                    /*alFilterId*/ new long[] {lFilterId},
                    /*iPartition*/ iPart,
                    /*lVersion*/   lVersion);
            }


        TreeMap mapEvents = new TreeMap();

        if (storeEvents != null)
            {
            storeEvents.iterate(com.tangosol.persistence.CachePersistenceHelper.instantiateEventsVisitor(
                    getCacheId(),
                    filter,
                    lVersion,
                    mapEvents,
                    com.tangosol.persistence.CachePersistenceHelper.LONG_CONVERTER_DOWN,
                    com.tangosol.persistence.CachePersistenceHelper.LONG_CONVERTER_UP,
                    getConverterValueDown(),
                    getConverterUp()));
            }

        // prepareDispatch for each message
        for (Iterator iter = mapEvents.values().iterator(); iter.hasNext(); )
            {
            com.tangosol.util.MapEvent event = (com.tangosol.util.MapEvent) iter.next();

            oHolder = accumulateMapEvents(
                    oHolder,
                    prepareDispatch(event, filter, member, fLite, lFilterId));
            }

        return oHolder;
        }

    /**
     * Send a "priming" MapEvent message to the NearCache.
     */
    public Object getPreviousEvents(com.tangosol.coherence.component.net.Member member, java.util.Set setKeys, int iPart, long lVersion, Object oHolder)
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        // import com.oracle.coherence.persistence.PersistentStore;
        // import com.tangosol.net.partition.VersionAwareMapListener as com.tangosol.net.partition.VersionAwareMapListener;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
        // import java.util.Collections;
        // import java.util.Iterator;
        // import java.util.TreeMap;

        PartitionedCache.PartitionControl ctrlPart    = (PartitionedCache.PartitionControl) getService().getPartitionControl(iPart);
        PersistentStore   storeEvents = ctrlPart.getPersistentEventsStore();

        if (lVersion == com.tangosol.net.partition.VersionAwareMapListener.HEAD)
            {
            // return a synthetic event that notifies the client of the latest event version
            lVersion = getVersion().getSubmittedVersion(iPart);
            if (lVersion == 0L)
                {
                // no changes on this partition slice therefore respond with com.tangosol.net.partition.VersionAwareMapListener.ALL
                // to ensure that if the client does not recieve any events prior to disconnect,
                // upon re-registration it will request all events that occurred
                lVersion = com.tangosol.net.partition.VersionAwareMapListener.ALL;
                }

            return prepareEventMessage(SingleMemberSet.instantiate(member),
                    /*nEventId*/   com.tangosol.util.MapEvent.ENTRY_UPDATED | PartitionedCache.MapEvent.EVT_SYNTHETIC | PartitionedCache.MapEvent.EVT_PRIMING,
                    /*key*/        null,
                    /*oldValue*/   null,
                    /*binValue*/   null,
                    /*alFilterId*/ null,
                    /*iPartition*/ iPart,
                    /*lVersion*/   lVersion);
            }

        TreeMap mapEvents = new TreeMap();

        if (storeEvents != null)
            {
            storeEvents.iterate(com.tangosol.persistence.CachePersistenceHelper.instantiateEventsVisitor(
                    getCacheId(),
                    setKeys,
                    lVersion,
                    mapEvents,
                    com.tangosol.persistence.CachePersistenceHelper.LONG_CONVERTER_DOWN,
                    com.tangosol.persistence.CachePersistenceHelper.LONG_CONVERTER_UP,
                    getConverterValueDown(),
                    getConverterUp()));
            }

        // prepareDispatch for each message

        for (Iterator iter = mapEvents.values().iterator(); iter.hasNext(); )
            {
            com.tangosol.util.MapEvent event = (com.tangosol.util.MapEvent) iter.next();

            PartitionedCache.MapEvent eventMsg = prepareEventMessage(SingleMemberSet.instantiate(member),
                    /*nEventId*/   event.getId() | PartitionedCache.MapEvent.EVT_SYNTHETIC | PartitionedCache.MapEvent.EVT_PRIMING,
                    /*key*/        (Binary) event.getKey(),
                    /*oldValue*/   (Binary) event.getOldValue(),
                    /*binValue*/   (Binary) event.getNewValue(),
                    /*alFilterId*/ null,
                    /*iPartition*/ iPart,
                    /*lVersion*/   event.getVersion());

            oHolder = accumulateMapEvents(oHolder, eventMsg);
            }

        return oHolder;
        }

    // Accessor for the property "PrimaryListener"
    /**
     * Getter for property PrimaryListener.<p>
     * Primary storage listener. Used only if a custom backing map manager
     * uses an ObservableMap to implement the [primary] local storage.
     */
    public com.tangosol.util.MapListener getPrimaryListener()
        {
        return __m_PrimaryListener;
        }

    /**
     * Send a "priming" MapEvent message to the NearCache.
     */
    public Object getPrimingEvent(com.tangosol.coherence.component.net.Member member, com.tangosol.util.Binary binKey, com.tangosol.util.Binary binValue, int iPart)
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;

        // TODO: hraja: the version of the event is not truly the version that caused
        //       the latest value, however as the key is locked it is at least the version
        //       that caused the change and less than the next value related to this key

        return prepareEventMessage(SingleMemberSet.instantiate(member),
                /*nEventId*/   PartitionedCache.MapEvent.ENTRY_UPDATED | PartitionedCache.MapEvent.EVT_SYNTHETIC | PartitionedCache.MapEvent.EVT_PRIMING,
                /*key*/        (Binary) binKey,
                /*oldValue*/   (Binary) null,
                /*binValue*/   (Binary) binValue,
                /*alFilterId*/ null,
                /*iPartition*/ iPart,
                /*lVersion*/   getVersion().getSubmittedVersion(iPart));
        }

    /**
     * Send a "priming" MapEvent message to the NearCache.
     */
    public Object getPrimingEvent(com.tangosol.coherence.component.net.Member member, EntryStatus status)
        {
        return getPrimingEvent(member, status.getKey(), status.getBinaryEntry().getBinaryValue(), status.getPartition());
        }

    /**
     * Send a "priming" MapEvent message to the NearCache.
     */
    public Object getPrimingEvents(com.tangosol.coherence.component.net.Member member, java.util.Map map)
        {
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.Map$Entry as java.util.Map.Entry;

        Object  oHolder = null;
        PartitionedCache service = (PartitionedCache) getService();

        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
            {
            java.util.Map.Entry  entry  = (java.util.Map.Entry) iter.next();
            Binary binKey = (Binary) entry.getKey();

            // TODO: this needs fixing
            oHolder = accumulateMapEvents(oHolder,
                                          getPrimingEvent(member, binKey, (Binary) entry.getValue(), service.getKeyPartition(binKey)));
            }

        return oHolder;
        }

    // Accessor for the property "QueryRetries"
    /**
     * Getter for property QueryRetries.<p>
     * Controlls the maximum number of query index retries before falling
     * back on entry by entry evaluation.
     *
     * The undocumented system  property used to set this value is
     * 'tangosol.coherence.query.retry', defaults to Integer.MAX_VALUE.
     */
    public int getQueryRetries()
        {
        return __m_QueryRetries;
        }

    // Accessor for the property "QuerySizeCache"
    /**
     * Getter for property QuerySizeCache.<p>
     * This cache holds temporary statistics for filter-based requests. The
     * value is a total size in bytes for matching values contained within a
     * single randomly choosen partition.
     */
    public java.util.Map getQuerySizeCache()
        {
        return __m_QuerySizeCache;
        }

    // From interface: com.tangosol.net.BackingMapContext
    public com.tangosol.util.InvocableMap.Entry getReadOnlyEntry(Object oKey)
        {
        // import com.tangosol.net.security.DoAsAction;
        // import com.tangosol.util.Binary;
        // import java.security.AccessController;
        // import java.util.Map;

        Map    map    = getBackingInternalCache();
        Binary binKey = (Binary) oKey;
        Binary binVal = isPotentiallyEvicting() ? (Binary) map.get(binKey) : null;

        return instantiateBinaryEntry(binKey, binVal, /*fReadOnly*/ true);
        }

    // Accessor for the property "ResourceControlMap"
    /**
     * Getter for property ResourceControlMap.<p>
     * Used to control access to keys.
     */
    public com.tangosol.util.ConcurrentMap getResourceControlMap()
        {
        return __m_ResourceControlMap;
        }

    // Accessor for the property "ResourceCoordinator"
    /**
     * Getter for property ResourceCoordinator.<p>
     */
    public PartitionedCache.ResourceCoordinator getResourceCoordinator()
        {
        return getService().getResourceCoordinator();
        }

    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
     */
    public PartitionedCache getService()
        {
        return (PartitionedCache) get_Module();
        }

    // Accessor for the property "StatsEventsDispatched"
    /**
     * Getter for property StatsEventsDispatched.<p>
     * The total number of MapEvents dispatched by this Storage.
     */
    public long getStatsEventsDispatched()
        {
        return __m_StatsEventsDispatched;
        }

    // Accessor for the property "StatsEvictions"
    /**
     * Getter for property StatsEvictions.<p>
     * A counter for the number of evictions from the backing map.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsEvictions()
        {
        return __m_StatsEvictions;
        }

    // Accessor for the property "StatsIndexingTotalMillis"
    /**
     * Getter for property StatsIndexingTotalMillis.<p>
     * Total amount of time it took to build indices since statistics were
     * last reset.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsIndexingTotalMillis()
        {
        return __m_StatsIndexingTotalMillis;
        }

    // Accessor for the property "StatsInserts"
    /**
     * Getter for property StatsInserts.<p>
     * A counter for the number of inserts into the backing map.
     * This counter gets incremented during direct inserts caused by put or
     * invoke operations; read-ahead synthetic inserts and data distribution
     * transfers "in". It gets decremented during data distribution
     * transfers "out".
     */
    public java.util.concurrent.atomic.AtomicLong getStatsInserts()
        {
        return __m_StatsInserts;
        }

    // Accessor for the property "StatsListenerRegistrations"
    /**
     * Getter for property StatsListenerRegistrations.<p>
     * The total number of Listener registration requests processed by this
     * Storage.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsListenerRegistrations()
        {
        return __m_StatsListenerRegistrations;
        }

    // Accessor for the property "StatsMaxQueryDescription"
    /**
     * Getter for property StatsMaxQueryDescription.<p>
     * A string representation of a query with the longest execution time
     * exceeding the MaxQueryThresholdMillis since statistics were last
     * reset.
     */
    public String getStatsMaxQueryDescription()
        {
        return __m_StatsMaxQueryDescription;
        }

    // Accessor for the property "StatsMaxQueryDurationMillis"
    /**
     * Getter for property StatsMaxQueryDurationMillis.<p>
     * The number of milliseconds of the longest running query since
     * statistics were last reset.
     */
    public long getStatsMaxQueryDurationMillis()
        {
        return __m_StatsMaxQueryDurationMillis;
        }

    // Accessor for the property "StatsMaxQueryThresholdMillis"
    /**
     * Getter for property StatsMaxQueryThresholdMillis.<p>
     * A query execution threshold in milliseconds The longest query
     * executing longer than this threshold will be reported in  the
     * MaxQueryDescription attribute.
     */
    public long getStatsMaxQueryThresholdMillis()
        {
        return __m_StatsMaxQueryThresholdMillis;
        }

    // Accessor for the property "StatsNonOptimizedQueryAverageMillis"
    /**
     * Getter for property StatsNonOptimizedQueryAverageMillis.<p>
     * The average number of milliseconds per non-optimized query execution
     * since the statistics were last reset.
     */
    public long getStatsNonOptimizedQueryAverageMillis()
        {
        long cCount = getStatsNonOptimizedQueryCount().get();

        return cCount == 0 ? 0 : getStatsNonOptimizedQueryTotalMillis().get() / cCount;
        }

    // Accessor for the property "StatsNonOptimizedQueryCount"
    /**
     * Getter for property StatsNonOptimizedQueryCount.<p>
     * Total number of queries that could not be resolved or was partial
     * resolved against indexes since statistics were last reset.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsNonOptimizedQueryCount()
        {
        return __m_StatsNonOptimizedQueryCount;
        }

    // Accessor for the property "StatsNonOptimizedQueryTotalMillis"
    /**
     * Getter for property StatsNonOptimizedQueryTotalMillis.<p>
     * Total number of milliseconds for queries that could not be resolved
     * or was partial resolved against indexes since statistics were last
     * reset.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsNonOptimizedQueryTotalMillis()
        {
        return __m_StatsNonOptimizedQueryTotalMillis;
        }

    // Accessor for the property "StatsOptimizedQueryAverageMillis"
    /**
     * Getter for property StatsOptimizedQueryAverageMillis.<p>
     * The average number of milliseconds per optimized query execution
     * since the statistics were last reset.
     */
    public long getStatsOptimizedQueryAverageMillis()
        {
        long cCount = getStatsOptimizedQueryCount().get();

        return cCount == 0 ? 0 : getStatsOptimizedQueryTotalMillis().get() / cCount;
        }

    // Accessor for the property "StatsOptimizedQueryCount"
    /**
     * Getter for property StatsOptimizedQueryCount.<p>
     * Total number of queries that were fully resolved using indexes since
     * statistics were last reset.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsOptimizedQueryCount()
        {
        return __m_StatsOptimizedQueryCount;
        }

    // Accessor for the property "StatsOptimizedQueryTotalMillis"
    /**
     * Getter for property StatsOptimizedQueryTotalMillis.<p>
     * The total number of milliseconds for optimized query operations since
     * statistics were last reset.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsOptimizedQueryTotalMillis()
        {
        return __m_StatsOptimizedQueryTotalMillis;
        }

    // Accessor for the property "StatsQueryContentionCount"
    /**
     * Getter for property StatsQueryContentionCount.<p>
     * Total number of times a query had to be re-evaluated due to a
     * concurrent update since statistics were last reset. This statistics
     * provides a measure of an impact of concurrent updates on the query
     * perfomance. If the total number of queries is Q and the number of
     * contentions is C then the expected performance degradation factor
     * should be no more than (Q + C)/Q.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsQueryContentionCount()
        {
        return __m_StatsQueryContentionCount;
        }

    // Accessor for the property "StatsRemoves"
    /**
     * Getter for property StatsRemoves.<p>
     * A counter for the number of removes from the backing map.
     * This counter gets incremented during direct removes caused by clear,
     * remove or invoke operations.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsRemoves()
        {
        return __m_StatsRemoves;
        }

    // Accessor for the property "StatsClears"
    /**
     * Getter for property StatsClears.<p>
     * A counter for the number of clear operations on the backing map.
     * This counter gets incremented during clear operations.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsClears()
        {
        return __m_StatsClears;
        }

    // Accessor for the property "TempBinaryEntry"
    /**
     * Getter for property TempBinaryEntry.<p>
     * A singleton temporary BinaryEntry that is used (solely) by the
     * service thread to minimize garbage creation.
     *
     * WARNING:  THIS SHOULD ONLY BE USED BY SERVICE THREAD!
     */
    protected BinaryEntry getTempBinaryEntry()
        {
        // _assert(Thread.currentThread() == getService().getThread());

        BinaryEntry binEntry = __m_TempBinaryEntry;
        if (binEntry == null)
            {
            binEntry = new BinaryEntry();
            _linkChild(binEntry);

            setTempBinaryEntry(binEntry);
            }

        return binEntry;
        }

    // Accessor for the property "TriggerSet"
    /**
     * Getter for property TriggerSet.<p>
     * A set of MapTriggers registered for this cache.
     *
     * @volatile
     */
    public java.util.Set getTriggerSet()
        {
        return __m_TriggerSet;
        }

    /**
     * Safely retrieve the resource value for the specified key. The
     * returned value coulde be decorated with the expiry time. Should be
     * called ONLY when the gate for the corresponding partition is
     * "closed".
     * Note: This method returns a ReadBuffer instead of a Binary to avoid a
     * redundant copy of the value.
     * @param  binKey key representing the entry in the backing map.
     * @param  fRemove if true remove the entry from the backing map.
     */
    public com.tangosol.io.ReadBuffer getValueForTransfer(com.tangosol.util.Binary binKey, boolean fRemove)
        {
        // import com.tangosol.io.ReadBuffer;
        // import com.tangosol.net.cache.CacheMap;
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.cache.ConfigurableCacheMap$Entry as com.tangosol.net.cache.ConfigurableCacheMap.Entry;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;

        // the partition is "closed" and no other thread could work on any
        // keys from the same partition, so the only way a value could
        // "disappear" is an eviction; the worst that could happen after that
        // is a botched override of a get() in a custom backing map
        ReadBuffer bufValue    = null;
        Map        mapResource = getBackingMapInternal();
        try
            {
            ConfigurableCacheMap mapConfigurable = getBackingConfigurableCache();
            if (mapConfigurable == null)
                {
                bufValue = fRemove ? (ReadBuffer) mapResource.remove(binKey)
                                   : (ReadBuffer) mapResource.get(binKey);
                }
            else
                {
                com.tangosol.net.cache.ConfigurableCacheMap.Entry entry = mapConfigurable.getCacheEntry(binKey);
                if (entry != null)
                    {
                    Object oValue    = entry.getValue();
                    long   ldtExpiry = entry.getExpiryMillis();

                    // COH-6336: compensate for the possibility of custom backing
                    //           maps not implementing getEntry() correctly
                    bufValue  = (ReadBuffer) (oValue instanceof ReadBuffer ? oValue : mapResource.get(binKey));

                    // Note: the return value of 0 from entry.getExpiryMillis() or
                    //       CCM.getExpiryDelay() means no-expiry, however the values
                    //       of EXPIRY_NEVER/EXPIRY_DEFAULT constants do not match
                    if (ldtExpiry == 0L && mapConfigurable.getExpiryDelay() == 0L)
                        {
                        // the entry expiry is "never", and so is the backing-map default;
                        // no need to decorate
                        }
                    else
                        {
                        ldtExpiry = ldtExpiry == 0L
                                    ? CacheMap.EXPIRY_NEVER
                                    : getService().getClusterService().calcTimestamp(ldtExpiry);

                        bufValue = com.tangosol.util.ExternalizableHelper.asBinary(
                                com.tangosol.util.ExternalizableHelper.encodeExpiry(bufValue, ldtExpiry));
                        }

                    if (fRemove)
                        {
                        mapResource.remove(binKey);
                        }
                    }
                }
            }
        catch (Throwable e)
            {
            String sOp = fRemove ? "remove" : "load";
            _trace("Failed " + sOp + " during transfer: key=" + binKey + '\n' +
                   e + '\n' + Base.getStackTrace(e), 1);
            }

        return bufValue;
        }

    // Accessor for the property "Version"
    /**
     * Getter for property Version.<p>
     * Data structure holding current versions of the backing map, the
     * partitions and corresponding indicies.
     */
    public com.tangosol.net.internal.StorageVersion getVersion()
        {
        return __m_Version;
        }

    /**
     * Returns true iff the corresponding partition contains data.
     *
     * @param iPartition  partition to check
     *
     * @return true if it contains data
     */
    public boolean hasData(int iPartition)
        {
        Set setKeys        = collectKeySet(getService().instantiatePartitionSet(iPartition), false);
        Map mapKeyListener = getKeyListenerMap();
        Map mapLease       = getLeaseMap();

        return setKeys.size() > 0
               || (mapKeyListener != null && !Collections.disjoint(mapKeyListener.keySet(), setKeys))
               || (mapLease       != null && !Collections.disjoint(mapLease.keySet(), setKeys));
        }

    /**
     * Determine whether there are any interceptors registered against the
     * StorageDispatcher.
     */
    public boolean hasInterceptors()
        {
        return getEventDispatcherInterceptor().getInterceptorCount().get() > 0;
        }

    public boolean hasListeners()
        {
        // import java.util.Map;

        Map mapListeners    = getListenerMap();
        Map mapKeyListeners = getKeyListenerMap();

        return getService().isPersistEvents()                       ||
               (mapListeners    != null && !mapListeners.isEmpty()) ||
               (mapKeyListeners != null && !mapKeyListeners.isEmpty());
        }

    /**
     * Initialize the specified PartitionAwareBakingMap based on the
     * ownership for the specified store index. Called on the Service thread
     * only.
     */
    protected void initializePartitions(com.tangosol.net.partition.PartitionAwareBackingMap mapPartitioned, int iStore)
        {
        // import Component.Net.Member;
        // import com.tangosol.net.partition.PartitionSet;

        PartitionedCache      service    = (PartitionedCache) getService();
        Member memberThis = service.getThisMember();
        PartitionSet partitions = service.calculatePartitionSet(memberThis, iStore);

        if (iStore == 0 && service.isTransferInProgress())
            {
            // If there are primary partition-transfers in-flight
            // (not yet completed), we must include these in the set
            // of partitions used to initialize the backing-map.
            // When the PartitionedCache.TransferRequest is completed, we will attempt
            // to destroy the partition in the PABM.  Otherwise if the
            // request fails, we will reassign the primary ownership to
            // ourselves.  In both cases, we expect that the primary
            // backing-maps have "prepared" the "in-flight" partition.

            for (PartitionedCache.TransferControl.TransferIterator iter = (PartitionedCache.TransferControl.TransferIterator) service.getTransferControl().iterateTransfersInProgress();
                 iter.hasNext(); )
                {
                // check for primary transfers in-progress, and add them
                // to the set of partitions to initialize the PABM with
                iter.next();
                if (iter.getBackup() == 0)
                    {
                    partitions.add(iter.getPartition());
                    }
                }
            }

        for (int iPartition = partitions.next(0); iPartition >= 0;
             iPartition = partitions.next(iPartition + 1))
            {
            mapPartitioned.createPartition(iPartition);
            }
        }

    /**
     * Insert the data from the specified array of leases into the primary
     * storage. Called on the service thread only.
     */
    public void insertBackupLeaseTransfer(int iPartition, com.tangosol.coherence.component.net.Lease[] aLease)
        {
        // import Component.Net.Lease;
        // import java.util.Map;

        Map map = getBackupLeaseMap();
        for (int i = 0, c = aLease.length; i < c; i++)
            {
            Lease lease = aLease[i];

            map.put(lease.getResourceKey(), lease);
            }
        }

    /**
     * Insert the data from the specified array of key-listener entries into
     * the backup storage. Called on the service thread only.
     */
    public void insertBackupListenerTransfer(int iPartition, java.util.Map.Entry[] aEntry)
        {
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        Map map = ensureBackupKeyListenerMap();
        for (int i = 0, c = aEntry.length; i < c; i++)
            {
            java.util.Map.Entry entry = aEntry[i];

            map.put(entry.getKey(), entry.getValue());
            }
        }

    /**
     * Insert the data from the specified array into the backup storage.
     * Called on the service thread only.
     */
    public void insertBackupTransfer(int iPartition, java.util.Map.Entry[] aEntry)
        {
        // import java.util.HashMap;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        PartitionedCache           service   = getService();
        boolean           fPutAll   = isPreferPutAllBackup();
        int               cEntries  = aEntry.length;
        Map               map       = fPutAll ? new HashMap(cEntries) : getBackupMap();
        long              lExtentId = getCacheId();

        try
            {
            // handle persistence
            if (isPersistent() && service.isBackupPersistence())
                {
                PartitionedCache.PartitionControl ctrl      = (PartitionedCache.PartitionControl) service.getPartitionControl(iPartition);
                PersistentStore                   store     = ctrl.ensureOpenPersistentStore(null, true, true);

                ctrl.ensureBackupPersistentExtent(lExtentId);

                Object oToken = store.begin();
                try
                    {
                    for (int i = 0; i < cEntries; i++)
                        {
                        java.util.Map.Entry entry = aEntry[i];

                        store.store(lExtentId, (ReadBuffer) entry.getKey(), (ReadBuffer) entry.getValue(), oToken);

                        map.put(entry.getKey(), entry.getValue());
                        }

                    store.commit(oToken);
                    }
                catch (Throwable t)
                    {
                    // ensure the persistence transaction is aborted if there is a
                    // non-persistence-related failure
                    store.abort(oToken);
                    throw Base.ensureRuntimeException(t);
                    }
                }
            else
                {
                for (int i = 0; i < cEntries; i++)
                    {
                    java.util.Map.Entry entry = aEntry[i];

                    map.put(entry.getKey(), entry.getValue());
                    }
                }

            if (fPutAll)
                {
                getBackupMap().putAll(map);
                }
            }
        catch (RuntimeException e)
            {
            reportTransferFailure(e, "backup");
            }
        }

    /**
     * Insert the cache entries represented in the specified
     * Iterator<Map.Entry> into the primary storage and the persistent store
     * (if this cache is persistent).
     *
     * This method is a helper intended to be used during incoming primary
     * transfer, or promote from backup, called only on the service thread.
     */
    protected void insertPrimaryData(int iPartition, java.util.Iterator iter)
        {
        // import com.oracle.coherence.persistence.PersistentStore;
        // import com.tangosol.net.cache.ConfigurableCacheMap as com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.cache.ConfigurableCacheMap$EvictionApprover as com.tangosol.net.cache.ConfigurableCacheMap.EvictionApprover;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        if (!iter.hasNext())
            {
            return;
            }

        PartitionedCache          service  = getService();
        int              cEntries = 0;
        com.tangosol.net.cache.ConfigurableCacheMap.EvictionApprover approver = null;
        com.tangosol.net.cache.ConfigurableCacheMap              mapCCM   = getConfigurableCacheMap(iPartition);
        long             lCacheId = getCacheId();

        boolean fScheduledBackups = service.isScheduledBackups();
        try
            {
            if (mapCCM != null)
                {
                // install eviction disapprover to prevent eviction at the cost of service thread
                approver = mapCCM.getEvictionApprover();
                mapCCM.setEvictionApprover(com.tangosol.net.cache.ConfigurableCacheMap.EvictionApprover.DISAPPROVER);
                }

            PartitionedCache.PartitionControl ctrlPartition = (PartitionedCache.PartitionControl) service.getPartitionControl(iPartition);

            if (isPersistent())
                {
                PersistentStore   store         = ctrlPartition.ensureOpenPersistentStore();
                Object            oToken        = store.begin();
                long              lExtentId     = getCacheId();

                ctrlPartition.ensurePersistentExtent(lExtentId);

                try
                    {
                    while (iter.hasNext())
                        {
                        java.util.Map.Entry  entry    = (java.util.Map.Entry) iter.next();
                        Binary binKey   = (Binary) entry.getKey();
                        Binary binValue = (Binary) entry.getValue();

                        // schedule a backup (after put) if decorated
                        boolean fScheduleBackup = false;
                        if (fScheduledBackups && com.tangosol.util.ExternalizableHelper.isDecorated(binValue, com.tangosol.util.ExternalizableHelper.DECO_BACKUP))
                            {
                            fScheduleBackup = true;
                            binValue        = com.tangosol.util.ExternalizableHelper.undecorate(binValue, com.tangosol.util.ExternalizableHelper.DECO_BACKUP);
                            }

                        if (binValue != Binary.EMPTY)
                            {
                            // insert into the backing-map
                            putPrimaryResource(binKey, binValue);
                            }

                        // insert into the persistent-store
                        store.store(lExtentId, binKey, binValue, oToken);

                        if (fScheduleBackup)
                            {
                            service.scheduleBackup(iPartition, lCacheId, binKey);
                            }

                        ++cEntries;
                        }

                    store.commit(oToken);
                    }
                catch (Throwable t)
                    {
                    // ensure the persistence transaction is aborted if there is a
                    // non-persistence-related failure
                    store.abort(oToken);
                    throw Base.ensureRuntimeException(t);
                    }
                }
            else
                {
                while (iter.hasNext())
                    {
                    java.util.Map.Entry  entry    = (java.util.Map.Entry) iter.next();
                    Binary binKey   = (Binary) entry.getKey();
                    Binary binValue = (Binary) entry.getValue();

                    // schedule a backup (after put) if decorated
                    boolean fScheduleBackup = false;
                    if (fScheduledBackups && com.tangosol.util.ExternalizableHelper.isDecorated(binValue, com.tangosol.util.ExternalizableHelper.DECO_BACKUP))
                        {
                        fScheduleBackup = true;
                        binValue        = com.tangosol.util.ExternalizableHelper.undecorate(binValue, com.tangosol.util.ExternalizableHelper.DECO_BACKUP);
                        }

                    if (binValue != Binary.EMPTY)
                        {
                        // insert into the backing-map
                        putPrimaryResource(binKey, binValue);
                        }

                    if (fScheduleBackup)
                        {
                        service.scheduleBackup(iPartition, lCacheId, binKey);
                        }

                    ++cEntries;
                    }
                }
            }
        finally
            {
            if (mapCCM != null)
                {
                // restore the original eviction approver
                mapCCM.setEvictionApprover(approver);
                }
            }

        // process any (unexpected) OOB events
        service.processChanges();

        // update the insert stats
        getStatsInserts().addAndGet(cEntries);
        }

    /**
     * Insert the cache listeners represented in the specified
     * Iterator<Map.Entry> into the primary storage and the persistent store
     * (if this cache is persistent).
     *
     * This method is a helper intended to be used during incoming primary
     * transfer, or promote from backup, called only on the service thread.
     */
    protected void insertPrimaryKeyListeners(int iPartition, java.util.Iterator iter)
        {
        // import Component.Net.Member;
        // import com.oracle.coherence.persistence.PersistentStore;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        PartitionedCache service         = getService();
        Map     mapKeyListeners = ensureKeyListenerMap();
        boolean fLiteOnly       = true;

        if (isPersistent())
            {
            PartitionedCache.PartitionControl ctrlPartition = (PartitionedCache.PartitionControl) service.getPartitionControl(iPartition);
            long              lExtentId     = getCacheId();

            ctrlPartition.ensurePersistentExtent(lExtentId);

            PersistentStore store     = ctrlPartition.ensureOpenPersistentStore();
            Object          oToken    = store.begin();
            try
                {
                while (iter.hasNext())
                    {
                    java.util.Map.Entry  entry      = (java.util.Map.Entry)  iter.next();
                    Binary binKey     = (Binary) entry.getKey();
                    Map    mapMembers = (Map)    entry.getValue();

                    // insert into the listener map
                    mapKeyListeners.put(binKey, mapMembers);

                    for (Iterator iterMember = mapMembers.entrySet().iterator(); iterMember.hasNext();)
                        {
                        java.util.Map.Entry   entryMember = (java.util.Map.Entry)   iterMember.next();
                        Member  member      = (Member)  entryMember.getKey();
                        Boolean FLite       = (Boolean) entryMember.getValue();

                        // insert into the persistent-store
                        com.tangosol.persistence.CachePersistenceHelper.registerListener(store, lExtentId, binKey,
                                                                                         service.getServiceMemberSet().getServiceJoinTime(member.getId()),
                                                                                         FLite.booleanValue(), oToken);

                        if (fLiteOnly && FLite == Boolean.FALSE)
                            {
                            fLiteOnly = false;
                            // MapEvent.getOldValue is likely to be called
                            ensureOldValueRequired();
                            }
                        }
                    }

                store.commit(oToken);
                }
            catch (Throwable t)
                {
                // ensure the persistence transaction is aborted if there is a
                // non-persistence-related failure
                store.abort(oToken);
                throw Base.ensureRuntimeException(t);
                }
            }
        else
            {
            while (iter.hasNext())
                {
                java.util.Map.Entry entry      = (java.util.Map.Entry) iter.next();
                Map   mapMembers = (Map) entry.getValue();

                // insert into the listener map
                mapKeyListeners.put(entry.getKey(), mapMembers);

                if (fLiteOnly && mapMembers.containsValue(Boolean.FALSE))
                    {
                    fLiteOnly = false;
                    // MapEvent.getOldValue is likely to be called
                    ensureOldValueRequired();
                    }
                }
            }
        }

    /**
     * Insert the cache listeners represented in the specified
     * Iterator<Map.Entry> into the primary storage and the persistent store
     * (if this cache is persistent).
     *
     * This method is a helper intended to be used during incoming primary
     * transfer, or promote from backup, called only on the service thread.
     */
    protected void insertPrimaryLeases(int iPartition, java.util.Iterator iter)
        {
        // import Component.Net.Lease;
        // import Component.Net.Member;
        // import com.oracle.coherence.persistence.PersistentStore;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        if (!iter.hasNext())
            {
            return;
            }

        PartitionedCache service  = getService();
        Map     mapLease = getLeaseMap();

        if (isPersistent())
            {
            PartitionControl ctrlPartition = (PartitionControl) service.getPartitionControl(iPartition);
            long              lExtentId     = getCacheId();

            ctrlPartition.ensurePersistentExtent(lExtentId);

            PersistentStore store     = ctrlPartition.ensureOpenPersistentStore();
            Object          oToken    = store.begin();

            try
                {
                while (iter.hasNext())
                    {
                    Lease  lease  = (Lease)  iter.next();
                    Binary binKey = (Binary) lease.getResourceKey();

                    // insert into the listener map
                    mapLease.put(binKey, lease);

                    // insert into the persistent-store
                    com.tangosol.persistence.CachePersistenceHelper.registerLock(store, lExtentId, binKey,
                                                                                 service.getServiceMemberSet().getServiceJoinTime(lease.getHolderId()),
                                                                                 lease.getHolderThreadId(), oToken);
                    }

                store.commit(oToken);
                }
            catch (Throwable t)
                {
                // ensure the persistence transaction is aborted if there is a
                // non-persistence-related failure
                store.abort(oToken);
                throw Base.ensureRuntimeException(t);
                }
            }
        else
            {
            while (iter.hasNext())
                {
                Lease lease = (Lease) iter.next();

                // insert into the listener map
                mapLease.put(lease.getResourceKey(), lease);
                }
            }
        }

    /**
     * Insert the data from the specified array of leases into the primary
     * storage. Called on the service thread only.
     */
    public void insertPrimaryLeaseTransfer(int iPartition, com.tangosol.coherence.component.net.Lease[] aLease)
        {
        // import com.tangosol.util.SimpleEnumerator;

        // insert the data to primary and persistent store
        insertPrimaryLeases(iPartition, new SimpleEnumerator(aLease));
        }

    /**
     * Insert the data from the specified array of key-listener entries into
     * the primary storage. Called on the service thread only.
     */
    public void insertPrimaryListenerTransfer(int iPartition, java.util.Map.Entry[] aEntry)
        {
        // import com.tangosol.util.SimpleEnumerator;

        if (aEntry.length == 0)
            {
            return;
            }

        // insert the data to primary and persistent store
        insertPrimaryKeyListeners(iPartition, new SimpleEnumerator(aEntry));
        }

    /**
     * Insert the data from the specified array of entries into the primary
     * storage. Called on the service thread only.
     */
    public void insertPrimaryTransfer(int iPartition, java.util.Map.Entry[] aEntry)
        {
        // import com.tangosol.util.SimpleEnumerator;

        // insert the data to primary and persistent store
        try
            {
            insertPrimaryData(iPartition, new SimpleEnumerator(aEntry));
            }
        catch (RuntimeException e)
            {
            reportTransferFailure(e, "primary");
            }
        }

    /**
     * Called on the Service thread only.
     */
    protected void instantiateBackingMap(String sCacheName)
        {
        // import com.tangosol.internal.util.KeyIndexManager;
        // import com.tangosol.net.cache.BinaryMemoryCalculator;
        // import com.tangosol.net.DefaultConfigurableCacheFactory as com.tangosol.net.DefaultConfigurableCacheFactory;
        // import com.tangosol.net.BackingMapManager as com.tangosol.net.BackingMapManager;
        // import com.tangosol.net.cache.CacheMap;
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.partition.ObservableSplittingBackingMap;
        // import com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.net.partition.PartitionSplittingBackingMap;
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.ObservableMap;
        // import com.tangosol.util.WrapperObservableMap;
        // import java.util.Map;

        PartitionedCache       service     = (PartitionedCache) getService();
        ObservableMap mapResource = getBackingMapInternal();

        com.tangosol.net.BackingMapManager manager = service.getBackingMapManager();
        if (manager == null)
            {
            // use the default Map
            }
        else
            {
            Map mapNew = null;
            try
                {
                setAccessAuthorizer(manager.getStorageAccessAuthorizer(sCacheName));
                mapNew = manager.instantiateBackingMap(sCacheName);
                if (mapNew == null)
                    {
                    _trace("BackingMapManager " + manager.getClass().getName() +
                           ": returned \"null\" for a cache: " + sCacheName, 1);
                    }
                else if (!mapNew.isEmpty())
                    {
                    // this could happen if the service restarted, and a custom manager
                    // failed to clear the contents during releaseCache()
                    mapNew.clear();
                    }
                }
            catch (RuntimeException e)
                {
                _trace("BackingMapManager " + manager.getClass().getName() +
                       ": failed to instantiate a cache: " + sCacheName, 1);
                _trace(e);
                }

            if (mapNew != null)
                {
                mapResource = mapNew instanceof ObservableMap
                              ? (ObservableMap) mapNew
                              : mapNew instanceof PartitionAwareBackingMap
                                ? new ObservableSplittingBackingMap((PartitionAwareBackingMap) mapNew)
                                : new WrapperObservableMap(mapNew, true);

                mapResource.addMapListener(instantiatePrimaryListener());
                setPersistent(service.getPersistenceManager() != null &&
                              manager.isBackingMapPersistent(sCacheName));
                setPreferPutAllPrimary(com.tangosol.net.DefaultConfigurableCacheFactory.isPutAllOptimized(mapResource));
                setBackingMapInternal(mapResource);
                setBackingInternalCache(mapResource);

                int cDefaultExpiry = 0;  // 0 means never here
                if (mapResource instanceof ReadWriteBackingMap)
                    {
                    // while it seems that we could look up the ReadWriteBackingMap's internal
                    // cache and do the same check as above, the CacheStore implementation may
                    // set the expiry using the "back door" without invoking neither postPut() nor
                    // putPrimaryResource(), so we need to consider it as potentially evicting
                    setPotentiallyEvicting(true);

                    ObservableMap mapInternal = ((ReadWriteBackingMap) mapResource).getInternalCache();
                    setBackingInternalCache(mapInternal);

                    if (mapInternal instanceof ConfigurableCacheMap)
                        {
                        ConfigurableCacheMap mapCCM = (ConfigurableCacheMap) mapInternal;
                        setBackingConfigurableCache(mapCCM);
                        cDefaultExpiry = mapCCM.getExpiryDelay();
                        }
                    }
                else if (mapResource instanceof ConfigurableCacheMap)
                    {
                    ConfigurableCacheMap mapConfigurable = (ConfigurableCacheMap) mapResource;
                    int                  cHighUnits      = mapConfigurable.getHighUnits();
                    int                  cExpiryDelay    = mapConfigurable.getExpiryDelay();

                    setPotentiallyEvicting(cHighUnits > 0 && cHighUnits < Integer.MAX_VALUE
                                           || cExpiryDelay > 0);
                    setBackingConfigurableCache(mapConfigurable);
                    cDefaultExpiry = mapConfigurable.getExpiryDelay();
                    }
                else
                    {
                    if (mapResource instanceof CacheMap)
                        {
                        // a backing-map implementation that is a CacheMap but not a CCM;
                        // warn that not all expiry functionality is supported
                        _trace("Cache \"" + sCacheName + "\" is configured with a backing-map "
                               + " implementation (" + ClassHelper.getSimpleName(mapResource.getClass())
                               + ") which does not implement the ConfigurableCacheMap interface; "
                               + "some advanced expiry functionality may not be available.", 3);
                        }

                    // for all other non-CCM implementations we need to play it safe
                    setPotentiallyEvicting(true);
                    }

                setBackingMapExpiryDelay(cDefaultExpiry == 0 ? (int) CacheMap.EXPIRY_NEVER : cDefaultExpiry);

                if (cDefaultExpiry > 0)
                    {
                    scheduleEviction(cDefaultExpiry);
                    setExpirySliding(manager.isBackingMapSlidingExpiry(sCacheName));
                    }
                }
            }

        if (mapResource instanceof PartitionAwareBackingMap)
            {
            PartitionAwareBackingMap mapPrime = (PartitionAwareBackingMap) mapResource;
            initializePartitions(mapPrime, 0);
            setPartitionAwareBackingMap(mapPrime);
            setInternPrimaryKeys(com.tangosol.net.DefaultConfigurableCacheFactory.isCanonicalKeySupported(mapResource));
            if (mapResource instanceof ConfigurableCacheMap &&
                ((ConfigurableCacheMap) mapResource).getUnitCalculator() instanceof BinaryMemoryCalculator)
                {
                setAdjustPartitionSize(false);
                }
            }
        else
            {
            KeyIndexManager mgr = new KeyIndexManager();
            mgr.init(service.getBackingMapContext());

            PartitionSplittingBackingMap mapKeyIndex =
                    new PartitionSplittingBackingMap(mgr, sCacheName + "$KeyIndex");
            mapKeyIndex.setStrict(service.isStrictPartitioning());

            initializePartitions(mapKeyIndex, 0);
            setPartitionedKeyIndex(mapKeyIndex);
            }
        }

    /**
     * Called on the Service thread only.
     */
    protected void instantiateBackupMap(String sCacheName)
        {
        // import com.tangosol.net.DefaultConfigurableCacheFactory as com.tangosol.net.DefaultConfigurableCacheFactory;
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap$EvictingBackupMap as com.tangosol.net.cache.ReadWriteBackingMap.EvictingBackupMap;
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.net.partition.PartitionSplittingBackingMap;
        // import java.util.Map;

        PartitionedCache service   = (PartitionedCache) getService();
        Map     mapBackup = null;

        // COH-503: When write-behind caching is used, support a configuration where
        // only not yet persisted data is backed up
        if (getBackingMapInternal() instanceof ReadWriteBackingMap
            && service.getBackupCountOpt() < service.getBackupCount())
            {
            mapBackup = new com.tangosol.net.cache.ReadWriteBackingMap.EvictingBackupMap();
            }

        BackingManager mgr = (BackingManager) _newChild("BackingManager");
        try
            {
            if (mapBackup == null)
                {
                mgr.setCacheName(sCacheName);
                mgr.parseConfiguration();
                if (mgr.isPartitioned())
                    {
                    // Note: The PSBM.createPartition(PID) method will create maps
                    // as needed by calling the BackingManager, which implements the
                    // BackingMapManager interface.
                    PartitionSplittingBackingMap mapPartitioned =
                            new PartitionSplittingBackingMap(mgr, sCacheName + "$Backup");
                    mapPartitioned.setStrict(service.isStrictPartitioning());

                    for (int iStore = 1; iStore <= service.getBackupCount(); iStore++)
                        {
                        initializePartitions(mapPartitioned, iStore);
                        }

                    mapBackup = mapPartitioned;
                    }
                else
                    {
                    mapBackup = mgr.instantiateBackingMap(sCacheName);
                    }
                }

            Map mapOld = getBackupMap();
            if (!mapOld.isEmpty())
                {
                try
                    {
                    _trace("Transferring " + mapOld.size() + " to backup for: " + sCacheName, 3);
                    mapBackup.putAll(mapOld);
                    }
                catch (NullPointerException ignored) {} // see PartitionSplittingBackingMap.put()
                mapOld.clear();
                }
            setPreferPutAllBackup(com.tangosol.net.DefaultConfigurableCacheFactory.isPutAllOptimized(mapBackup));
            setInternBackupKeys(com.tangosol.net.DefaultConfigurableCacheFactory.isCanonicalKeySupported(mapBackup));
            setPartitionAwareBackupMap(mapBackup instanceof com.tangosol.net.partition.PartitionAwareBackingMap ? (com.tangosol.net.partition.PartitionAwareBackingMap) mapBackup : null);
            setBackupMap(mapBackup);
            }
        catch (RuntimeException e)
            {
            _trace("BackingMapManager " + mgr +
                   ": failed to instantiate a backup map for cache: " + sCacheName, 1);
            _trace(e);
            }
        }

    /**
     * Instantiate a BinaryEntry with an active conversion.
     *
     * @param binKey the binary key
     * @param binValue the binary value; passing null indicates a deferred
     * get operation
     */
    public BinaryEntry instantiateBinaryEntry(com.tangosol.util.Binary binKey, com.tangosol.util.Binary binValue, boolean fReadOnly)
        {
        // BinaryEntry objects could be created quite frequently,
        // so we manually link it instead of using _newChild() approach

        BinaryEntry entry = new BinaryEntry();
        entry.setBinaryKey(binKey);
        entry.setBinaryValue(binValue);

        if (fReadOnly)
            {
            entry.ensureReadOnly();
            }

        _linkChild(entry);

        return entry;
        }

    protected DeferredEvent instantiateDeferredEvent(com.tangosol.util.MapEvent evt, boolean fReapply)
        {
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;

        DeferredEvent deferred = (DeferredEvent) _newChild("DeferredEvent");

        deferred.setEvent(evt);
        deferred.setReapply(fReapply);

        // pull the old value iff it is likely to be needed, which will
        // only be true for deletes or updates
        int nEvent = evt.getId();
        if ((nEvent == com.tangosol.util.MapEvent.ENTRY_DELETED || nEvent == com.tangosol.util.MapEvent.ENTRY_UPDATED) &&
            isOldValueRequired())
            {
            evt.getOldValue();
            }

        return deferred;
        }

    protected com.tangosol.util.Converter instantiateEnlistingConverter(PartitionedCache.InvocationContext ctxInvoke, com.tangosol.util.MapTrigger trigger)
        {
        EnlistingConverter conv = new EnlistingConverter();

        _linkChild(conv);

        conv.setInvocationContext(ctxInvoke);
        conv.setTrigger(trigger);

        return conv;
        }

    /**
     * Instantiates a new $LazyKeySet which will collect all the primary
     * keys belonging to the passed in partitions.
     *
     * @param parts the PartitionSet to collect the primary keys for
     * @param  fSnapshot if true create a copy of the primary key set
     */
    protected LazyKeySet instantiateLazyKeySet(com.tangosol.net.partition.PartitionSet parts, boolean fSnapshot)
        {
        LazyKeySet set = new LazyKeySet();
        _linkChild(set);

        set.setPartitionMask(parts);
        set.setSnapshot(fSnapshot);

        return set;
        }

    protected com.tangosol.util.MapListener instantiatePrimaryListener()
        {
        PrimaryListener listener =
                (PrimaryListener) _newChild("PrimaryListener");

        setPrimaryListener(listener);

        return listener;
        }

    /**
     * Invalidate the storage and release all storage related resources.
     * Called on the service thread only.
     */
    public void invalidate()
        {
        // import com.tangosol.net.BackingMapManager;
        // import com.tangosol.net.management.Registry;
        // import com.tangosol.util.MapListener;
        // import com.tangosol.util.NullImplementation;
        // import com.tangosol.util.ObservableMap;
        // import java.util.Map;

        PartitionedCache service = (PartitionedCache) getService();

        Map         mapPrime = getBackingMapInternal();
        MapListener listener = getPrimaryListener();
        if (listener != null)
            {
            ((ObservableMap) mapPrime).removeMapListener(listener);
            }

        setListenerMap(null);
        setKeyListenerMap(null);

        String            sName   = getCacheName();
        BackingMapManager manager = service.getBackingMapManager();
        if (sName != null)
            {
            if (manager != null)
                {
                try
                    {
                    manager.releaseBackingMap(sName, mapPrime);
                    }
                catch (RuntimeException e)
                    {
                    _trace("BackingMapManager " + manager.getClass().getName() +
                           ": failed to release a cache: " + sName, 1);
                    _trace(e);
                    }
                }

            Registry registry = service.getCluster().getManagement();
            if (registry != null)
                {
                // unregister MBean:
                // type=StorageManager,service=<name>,cache=<name>,nodeId=<id>
                String sBean = new StringBuilder(Registry.STORAGE_MANAGER_TYPE)
                        .append(",service=")
                        .append(service.getServiceName())
                        .append(",cache=")
                        .append(sName)
                        .toString();

                registry.unregister(registry.ensureGlobalName(sBean));
                }
            }

        if (service.getBackupCount() > 0)
            {
            setBackupKeyListenerMap(null);
            invalidateBackupMap();
            }

        getLeaseMap().clear();
        setBackingMapInternal(NullImplementation.getObservableMap());
        setBackingInternalCache(NullImplementation.getObservableMap());
        setBackingConfigurableCache(null);
        setPartitionAwareBackingMap(null);
        setIndexExtractorMap(NullImplementation.getMap());
        setFilterIdMap(null);
        setAdjustPartitionSize(true);

        EvictionTask taskEvict = getEvictionTask();
        if (taskEvict != null)
            {
            taskEvict.cancel();
            setEvictionTask(null);
            }

        // unregister the UEM dispatcher
        service.getEventsHelper().unregisterStorageDispatcher(this);

        setValid(false);
        }

    protected void invalidateBackupMap()
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.NullImplementation;
        // import java.util.Map;

        Map  mapBackup  = getBackupMap();
        com.tangosol.net.partition.PartitionAwareBackingMap pabmBackup = getPartitionAwareBackupMap();

        if (pabmBackup == null)
            {
            String sCacheName = getCacheName();
            if (sCacheName != null)
                {
                BackingManager mgr = (BackingManager) _newChild("BackingManager");
                mgr.setCacheName(sCacheName);
                mgr.parseConfiguration();
                mgr.releaseBackingMap(sCacheName, mapBackup);
                }
            }
        else
            {
            PartitionSet partitions = ((PartitionedCache) getService()).
                    collectOwnedPartitions(false);

            for (int iPartition = partitions.next(0); iPartition >= 0;
                 iPartition = partitions.next(iPartition + 1))
                {
                pabmBackup.destroyPartition(iPartition);
                }
            }

        setBackupMap(NullImplementation.getObservableMap());
        }

    /**
     * Called on the service or a daemon pool thread after acquiring the key
     * lock.
     */
    public EntryStatus invoke(PartitionedCache.InvocationContext ctxInvoke, EntryStatus status, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.internal.tracing.TracingHelper;
        // import com.tangosol.util.Binary;
        // import java.util.Collections;
        // import java.util.Set;

        if (TracingHelper.isEnabled())
            {
            TracingHelper.augmentSpan()
                    .setMetadata("agent.class", agent.getClass().getName());
            }

        PartitionedCache.EventsHelper evtHelper  = getService().getEventsHelper();
        Set           setEntries = Collections.singleton(status.getBinaryEntry());

        evtHelper.onInvoking(this, setEntries, agent, null);

        Object oResult = agent.process(status.getBinaryEntry());
        status.setResult((Binary) getConverterValueDown().convert(oResult));

        // Note: exception in process() will propagate up (and skip postInvoke)
        ctxInvoke.processInterceptors();
        ctxInvoke.postInvoke(); // this commits all entries enlisted with InvocationContext

        evtHelper.onInvoked(this, setEntries, agent, null);

        return status;
        }

    /**
     * Called on the service or a daemon pool thread after acquiring locks
     * for all keys.
     *
     * @param ctxInvoke  the $InvocationContext
     * @param aEntry        the array of $BinaryEntry being invoked on (same
     * length as aStatus)
     * @param aStatus      the array of $EntryStatus objects corresponding
     * to the entries invoked (same length as aEntry)
     * @param iFrom         the index into the arrays of the first entry to
     * be invoked
     * @param iTo             the index into the arrays of the last entry to
     * be invoked
     * @param agent          the EntryProcessor to invoke
     */
    public void invokeAll(PartitionedCache.InvocationContext ctxInvoke, Object[] aoStatus, int iFrom, int iTo, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.internal.util.LockContentionException;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        // import com.tangosol.util.SubSet;
        // import com.tangosol.internal.tracing.TracingHelper;
        // import java.util.LinkedHashSet;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;

        if (iFrom == iTo)
            {
            // nothing to do; this could happen due to request re-submission after failover.
            return;
            }

        if (TracingHelper.isEnabled())
            {
            TracingHelper.augmentSpan()
                    .setMetadata("agent.class", agent.getClass().getName());
            }

        PartitionedCache.EventsHelper evtHelper   = getService().getEventsHelper();
        Converter     convDown    = getConverterValueDown();
        Converter     convKeyDown = getConverterKeyDown();
        Set           setEntries  = new LinkedHashSet();
        Set           setRemoved  = null;

        for (int i = iFrom; i < iTo; i++)
            {
            setEntries.add(((EntryStatus) aoStatus[i]).getBinaryEntry());
            }
        setEntries = new SubSet(setEntries);

        Throwable tError    = null;
        Map       mapResult = null;
        try
            {
            // dispatch UEM pre-invoke event
            evtHelper.onInvoking(this, setEntries, agent, null);

            mapResult = agent.processAll(setEntries);

            // update the respective EntryStatus objects with the invocation results
            if (mapResult != null && !mapResult.isEmpty())
                {
                int cResults = mapResult.size();

                for (int i = iFrom; i < iTo; i++)
                    {
                    EntryStatus status = (EntryStatus) aoStatus[i];
                    BinaryEntry entry  = status.getBinaryEntry();

                    if (entry.isKeyConverted())
                        {
                        Object oKey = entry.getKey(); // in Object format

                        if (mapResult.containsKey(oKey))
                            {
                            Object oValue = mapResult.get(oKey);
                            status.setResult(oValue instanceof Binary ? (Binary) oValue : (Binary) convDown.convert(oValue));
                            mapResult.remove(oKey);
                            }
                        }
                    }

                if (!mapResult.isEmpty())
                    {
                    // uncommon path; the entry-processor did not deserialize some keys;
                    // lookup the associated key in the enlisted entries

                    for (Iterator iter = mapResult.entrySet().iterator(); iter.hasNext();)
                        {
                        java.util.Map.Entry  entry = (java.util.Map.Entry) iter.next();
                        Object oKey  = entry.getKey();

                        // we presume EP implementations do not send back decorated binaries
                        boolean fConverted = oKey instanceof Binary &&
                                             com.tangosol.util.ExternalizableHelper.isIntDecorated((Binary) oKey);

                        Binary binKey = fConverted
                                        ? (Binary) oKey
                                        : (Binary) convKeyDown.convert(oKey);

                        EntryStatus status = ctxInvoke.getEntryStatus(this, binKey);
                        if (status != null)
                            {
                            Object oValue = entry.getValue();
                            status.setResult(oValue instanceof Binary ? (Binary) oValue : (Binary) convDown.convert(oValue));
                            mapResult.remove(oKey);
                            }
                        }
                    }

                if (!mapResult.isEmpty())
                    {
                    for (Iterator iter = mapResult.keySet().iterator(); iter.hasNext();)
                        {
                        _trace("Unexpected key: \"" + iter.next() + "\" is  returned from processAll() invocation", 2);
                        }
                    }
                }
            }
        catch (LockContentionException e)
            {
            throw e;
            }
        catch (Throwable e)
            {
            tError = e;
            // processAll() failed; According to the processAll() contract, only
            // entries removed by the EntryProcessor were "logically" processed.
            // All other entries that are left in setEntries were not processed.
            Set setEntriesRemoved = ((SubSet) setEntries).getRemoved();
            setRemoved            = new LinkedHashSet();
            for (int i = iFrom; i < iTo; i++)
                {
                EntryStatus status = (EntryStatus) aoStatus[i];
                BinaryEntry entry  = status.getBinaryEntry();
                if (entry != null)
                    {
                    if (setEntriesRemoved.contains(entry))
                        {
                        setRemoved.add(status);
                        }
                    else
                        {
                        // null the status for unprocessed keys
                        aoStatus[i] = null;
                        // reset so that triggers are not called for the entry
                        entry.reset(entry.getBinaryKey());
                        }
                    }
                }
            }

        // update the sandbox (InvocationContext) for processed entries
        Set setEntriesAdded = ctxInvoke.processInterceptors();

        if (setRemoved == null)
            {
            ctxInvoke.postInvoke();
            }
        else
            {
            // update the sandbox (InvocationContext) for "processed" entries
            ctxInvoke.postInvokeAll(setRemoved);
            ctxInvoke.postInvokeAll(setEntriesAdded);
            }

        // dispatch UEM post-invoke event only for "processed" entries
        evtHelper.onInvoked(this, ((SubSet) setEntries).getRemoved(), agent, null);

        if (tError != null)
            {
            // re-throw the processor's exception (to be packaged as a client response)
            throw Base.ensureRuntimeException(tError);
            }
        }

    // Accessor for the property "AdjustPartitionSize"
    /**
     * Getter for property AdjustPartitionSize.<p>
     * If true, it is the responsibility of the storage to update the
     * partition size as data change. Otherwise BM is PABM and we can get
     * the partition size directly.
     */
    public boolean isAdjustPartitionSize()
        {
        return __m_AdjustPartitionSize;
        }

    // Accessor for the property "ExpirySliding"
    /**
     * Getter for property ExpirySliding.<p>
     * True iff cache is configured with a non-zero "expiry-delay" and
     * "expiry-sliding" is enabled.
     */
    public boolean isExpirySliding()
        {
        return __m_ExpirySliding;
        }

    // Accessor for the property "Indexed"
    /**
     * Getter for property Indexed.<p>
     * Specifies whether or not there are any indexes for this storage.
     */
    public boolean isIndexed()
        {
        return !getIndexExtractorMap().isEmpty();
        }

    // Accessor for the property "InternBackupKeys"
    /**
     * Getter for property InternBackupKeys.<p>
     * Specifies whether or not to intern Backup Keys.
     */
    public boolean isInternBackupKeys()
        {
        return __m_InternBackupKeys;
        }

    // Accessor for the property "InternPrimaryKeys"
    /**
     * Getter for property InternPrimaryKeys.<p>
     * Specifies whether or not to intern Primary Keys.
     */
    public boolean isInternPrimaryKeys()
        {
        return __m_InternPrimaryKeys;
        }

    // Accessor for the property "MisconfigLoggedBackup"
    /**
     * Getter for property MisconfigLoggedBackup.<p>
     * Used by movePartition() / insertBackupTransfer() to limit the number
     * of error messages for a misconfigured cache.
     */
    public boolean isMisconfigLoggedBackup()
        {
        return __m_MisconfigLoggedBackup;
        }

    // Accessor for the property "MisconfigLoggedPrimary"
    /**
     * Getter for property MisconfigLoggedPrimary.<p>
     * Used by movePartition() / insertPrimaryTransfer() to limit the number
     * of error messages for a misconfigured cache.
     */
    public boolean isMisconfigLoggedPrimary()
        {
        return __m_MisconfigLoggedPrimary;
        }

    // Accessor for the property "OldValueRequired"
    /**
     * Getter for property OldValueRequired.<p>
     * Specifies whether or not the old value is likely to be accessed
     * either during or post request processing.
     *
     * @volatile
     */
    protected boolean isOldValueRequired()
        {
        return __m_OldValueRequired;
        }

    // Accessor for the property "Persistent"
    /**
     * Getter for property Persistent.<p>
     * True iff the contents of this Storage should be persisted.
     */
    public boolean isPersistent()
        {
        return __m_Persistent;
        }

    // Accessor for the property "PotentiallyEvicting"
    /**
     * Check whether or not the specified backing map is potentially
     * evicting.
     */
    public boolean isPotentiallyEvicting()
        {
        return __m_PotentiallyEvicting;
        }

    // Accessor for the property "PreferPutAllBackup"
    /**
     * Getter for property PreferPutAllBackup.<p>
     * Specifies whether or not the backup backing map prefers putAll to
     * regular put operations.
     */
    public boolean isPreferPutAllBackup()
        {
        return __m_PreferPutAllBackup;
        }

    // Accessor for the property "PreferPutAllPrimary"
    /**
     * Getter for property PreferPutAllPrimary.<p>
     * Specifies whether or not the primary backing map prefers putAll to
     * regular put operations.
     */
    public boolean isPreferPutAllPrimary()
        {
        return __m_PreferPutAllPrimary;
        }

    // Accessor for the property "Valid"
    /**
     * Getter for property Valid.<p>
     * Indicates whether the storage is valid.  If false, this means the
     * storage has not been initialized or it has been invalidated.
     *
     * This property is only modifed on the service thread.
     *
     * @volatile
     *
     * @see #setCacheName
     * @see #invalidate
     */
    public boolean isValid()
        {
        return __m_Valid;
        }

    /**
     * Called on the service or a daemon pool thread.
     */
    public boolean lock(com.tangosol.coherence.component.net.Lease lease)
        {
        // import Component.Net.Lease;
        // import com.oracle.coherence.persistence.PersistentStore;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;
        // import com.tangosol.util.Binary;
        // import java.util.Map;

        Map    mapLease        = getLeaseMap();
        Binary binKey          = (Binary) lease.getResourceKey();
        Lease  leaseCurrent    = (Lease)  mapLease.get(binKey);
        int    nHolderId       = lease.getHolderId();
        long   lHolderThreadId = lease.getHolderThreadId();

        PartitionedCache service = getService();
        if (leaseCurrent == null ||
            (leaseCurrent.getHolderId()       == nHolderId &&
             leaseCurrent.getHolderThreadId() == lHolderThreadId))
            {
            // protect against lock by member which is concurrently dying or leaving cluster;
            // see validateLocks
            synchronized (mapLease)
                {
                if (!service.getServiceMemberSet().contains(nHolderId))
                    {
                    return false;
                    }

                mapLease.put(binKey, lease);

                if (isPersistent())
                    {
                    // persist the lock registration
                    int               nPartition    = service.getKeyPartition(binKey);
                    PartitionedCache.PartitionControl ctrl = (PartitionedCache.PartitionControl) service.getPartitionControl(nPartition);
                    PersistentStore   store         = ctrl.ensureOpenPersistentStore(/*storeFrom*/ null, /*fSeal*/ true);
                    long              ldtJoined     = service.getServiceMemberSet().getServiceJoinTime(nHolderId);

                    // the lock holder (as known to this service) is uniquely identified
                    // by its service join-time
                    com.tangosol.persistence.CachePersistenceHelper.registerLock(store, getCacheId(), binKey, ldtJoined, lHolderThreadId, /*oToken*/ null);
                    }
                }

            return true;
            }
        else
            {
            return false;
            }
        }

    /**
     * Move the content of key based maps between the maps for all keys in
     * the specified partition.
     *
     * @param iPartition    the partition number
     */
    protected void moveData(int iPartition, java.util.Map mapFrom, java.util.Map mapTo, String sDescr)
        {
        // import Component.Net.Lease;
        // import Component.Net.Member;
        // import com.tangosol.util.Binary;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.Map$Entry as java.util.Map.Entry;

        PartitionedCache           service   = getService();
        PartitionedCache.PartitionControl ctrl      = (PartitionedCache.PartitionControl) service.getPartitionControl(iPartition);
        PersistentStore   store     = ctrl.getPersistentBackupStore();
        long              lExtentId = getCacheId();
        Object            oToken    = null;

        // instead of synchronizing on the map and blocking all
        // the "put" and "remove" operations, we just catch any
        // ConcurrentModificationException and try again
        while (true)
            {
            try
                {
                for (Iterator iter = mapFrom.entrySet().iterator(); iter.hasNext();)
                    {
                    java.util.Map.Entry  entry  = (java.util.Map.Entry) iter.next();
                    Binary binKey = (Binary) entry.getKey();

                    if (service.getKeyPartition(binKey) == iPartition)
                        {
                        mapTo.put(binKey, entry.getValue());

                        // insert into the persistent-store
                        if (isPersistent() && service.isBackupPersistence())
                            {
                            oToken = store.begin();

                            if ("locks".equals(sDescr))
                                {
                                Lease  lease       = (Lease)  iter.next();
                                Binary binLeaseKey = (Binary) lease.getResourceKey();

                                com.tangosol.persistence.CachePersistenceHelper.registerLock(store, lExtentId, binLeaseKey,
                                                                                             service.getServiceMemberSet().getServiceJoinTime(lease.getHolderId()),
                                                                                             lease.getHolderThreadId(), oToken);
                                }
                            else if ("listeners".equals(sDescr))
                                {
                                Member  member = (Member)  entry.getKey();
                                Boolean FLite  = (Boolean) entry.getValue();

                                com.tangosol.persistence.CachePersistenceHelper.registerListener(store, lExtentId, binKey,
                                                                                                 service.getServiceMemberSet().getServiceJoinTime(member.getId()),
                                                                                                 FLite.booleanValue(), oToken);
                                }

                            store.commit(oToken);
                            }

                        iter.remove();
                        }
                    }
                break;
                }
            catch (ConcurrentModificationException e)
                {
                _trace("Failed to move " + sDescr + ": " + e + "; trying again", 2);
                }
            catch (Throwable t)
                {
                // ensure the persistence transaction is aborted if there is a
                // non-persistence-related failure
                store.abort(oToken);
                throw Base.ensureRuntimeException(t);
                }
            }
        }

    /**
     * Move all the data in specified partition from the backup storage into
     * the primary storage or back. Called on the service thread only.
     *
     * @param iPartition   the partition number to move
     * @param fToBackup  if true, move from primary to backup; otherwise
     * from backup to primary
     *
     * @see #preparePartition, releasePartition
     */
    public void movePartition(int iPartition, boolean fToBackup)
        {
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.FilterEnumerator;
        // import com.tangosol.util.InvocableMapHelper;
        // import java.util.Map;

        // important:
        // - move listeners from primary to backup BEFORE the data get moved
        // - move listeners from backup to primary AFTER the data get moved

        PartitionedCache service = getService();
        if (fToBackup)
            {
            // locks
            moveData(iPartition, getLeaseMap(), getBackupLeaseMap(), "locks");
            firePendingLocks(iPartition);

            // listeners
            Map mapListeners = getKeyListenerMap();
            if (mapListeners != null && !mapListeners.isEmpty())
                {
                moveData(iPartition, mapListeners, ensureBackupKeyListenerMap(), "listeners");
                }

            // data
            try
                {
                moveResourcesToBackup(iPartition);
                }
            catch (RuntimeException e)
                {
                reportTransferFailure(e, "backup");
                }

            if (isPersistent() && service.isBackupPersistence())
                {
                persistGlobalMetadata(iPartition, /*fBackup*/ true, /*oToken*/ null);
                }
            }
        else
            {
            // data
            try
                {
                moveResourcesToPrimary(iPartition);
                }
            catch (RuntimeException e)
                {
                reportTransferFailure(e, "primary");
                }

            // locks
            insertPrimaryLeases(iPartition,
                                ConverterCollections.getIterator(
                                        new FilterEnumerator(getBackupLeaseMap().entrySet().iterator(),
                                                             service.instantiatePartitionFilter(iPartition)),
                                        InvocableMapHelper.ENTRY_TO_VALUE_CONVERTER));

            // listeners
            Map mapListeners = getBackupKeyListenerMap();
            if (mapListeners != null && !mapListeners.isEmpty())
                {
                insertPrimaryKeyListeners(iPartition,
                                          new FilterEnumerator(mapListeners.entrySet().iterator(),
                                                               service.instantiatePartitionFilter(iPartition)));
                }

            // ensure that any "global" meta-data are properly persisted
            if (isPersistent())
                {
                persistGlobalMetadata(iPartition, /*oToken*/ null);
                }
            }
        }

    /**
     * Move all the resources in specified partition from the primary
     * storage into the backup storage. Called on the service thread only.
     *
     * @param iPartition  the partition number to move
     *
     * @see #releaseBucket
     */
    protected void moveResourcesToBackup(int iPartition)
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap as com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.cache.ConfigurableCacheMap$EvictionApprover as com.tangosol.net.cache.ConfigurableCacheMap.EvictionApprover;
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        // import java.util.Collections;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Set;

        PartitionedCache           service    = getService();
        Map               mapPrime   = getBackingMapInternal();
        Map               mapBackup  = getBackupMap();
        boolean           fPutAll    = isPreferPutAllBackup();
        Set               setKeys    = collectKeySet(iPartition);
        com.tangosol.net.partition.PartitionAwareBackingMap              pabmPrime  = getPartitionAwareBackingMap();
        com.tangosol.net.partition.PartitionAwareBackingMap              pabmBackup = getPartitionAwareBackupMap();

        if (pabmBackup != null)
            {
            ensurePartition(pabmBackup, iPartition);
            }

        com.tangosol.net.cache.ConfigurableCacheMap.EvictionApprover approver  = null;
        com.tangosol.net.cache.ConfigurableCacheMap              mapCCM    = getConfigurableCacheMap(iPartition);
        int              cRemoved  = 0;
        long             lExtentId = getCacheId();
        try
            {
            if (mapCCM != null)
                {
                // install eviction disapprover to prevent eviction at the cost of service thread
                approver = mapCCM.getEvictionApprover();
                mapCCM.setEvictionApprover(com.tangosol.net.cache.ConfigurableCacheMap.EvictionApprover.DISAPPROVER);
                }

            for (Iterator iter = setKeys.iterator(); iter.hasNext();)
                {
                Binary binKey   = (Binary) iter.next();
                Binary binValue = com.tangosol.util.ExternalizableHelper.asBinary(getValueForTransfer(binKey, /*fRemove*/ true));
                if (binValue == null)
                    {
                    continue;
                    }

                if (fPutAll)
                    {
                    mapBackup.putAll(Collections.singletonMap(binKey, binValue));
                    }
                else
                    {
                    mapBackup.put(binKey, binValue);
                    }

                // handle persistence
                if (isPersistent() && service.isBackupPersistence())
                    {
                    PartitionedCache.PartitionControl ctrl       = (PartitionedCache.PartitionControl) service.getPartitionControl(iPartition);
                    PersistentStore                   store      = ctrl.ensureOpenPersistentStore(null, true, true);

                    ctrl.ensureBackupPersistentExtent(lExtentId);

                    Object oToken = store.begin(null, ctrl);
                    try
                        {
                        store.store(lExtentId, (ReadBuffer) binKey, (ReadBuffer) binValue, oToken);

                        store.commit(oToken);
                        }
                    catch (Throwable t)
                        {
                        // ensure the persistence transaction is aborted if there is a
                        // non-persistence-related failure
                        store.abort(oToken);
                        throw Base.ensureRuntimeException(t);
                        }
                    }

                ++cRemoved;
                }
            }
        finally
            {
            if (mapCCM != null)
                {
                mapCCM.setEvictionApprover(approver);
                }
            }

        // Last-chance to process any (unexpected) OOB events before we release the partition
        service.processChanges();

        // decrement the inserts count, as that stat transfers with the partition
        getStatsInserts().addAndGet(-cRemoved);

        // cleanup the com.tangosol.net.partition.PartitionAwareBackingMap, or the primary key index
        if (pabmPrime == null)
            {
            com.tangosol.net.partition.PartitionAwareBackingMap mapKeyIndex = getPartitionedKeyIndex();
            if (mapKeyIndex != null)
                {
                mapKeyIndex.destroyPartition(iPartition);
                }
            }
        else
            {
            pabmPrime.destroyPartition(iPartition);
            }
        }

    /**
     * Move all the resources in specified partition from the backup storage
     * into the primary storage. Called on the service thread only.
     *
     * @param iPartition  the partition number to move
     *
     * @see #releaseBucket
     */
    protected void moveResourcesToPrimary(int iPartition)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.util.Binary;
        // import java.util.Collections;
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        PartitionedCache service    = getService();
        Map     mapFrom    = getBackupMap();
        com.tangosol.net.partition.PartitionAwareBackingMap    pabmBackup = getPartitionAwareBackupMap();

        if (pabmBackup != null)
            {
            mapFrom = pabmBackup.getPartitionMap(iPartition);
            }

        preparePartition(iPartition, true);

        Map mapEntries = mapFrom;
        if (pabmBackup == null)
            {
            // if the backup map is not com.tangosol.net.partition.PartitionAwareBackingMap, collect the entries to promote
            mapEntries = new HashMap();
            for (Iterator iter = mapFrom.entrySet().iterator(); iter.hasNext();)
                {
                java.util.Map.Entry  entry  = (java.util.Map.Entry) iter.next();
                Binary binKey = (Binary) entry.getKey();
                Binary binVal = (Binary) entry.getValue();
                if (service.getKeyPartition(binKey) == iPartition)
                    {
                    mapEntries.put(binKey, binVal);
                    }
                }
            }

        if (mapEntries == null)
            {
            // If mapEntries is null here, we know that we have already messed up
            // and are missing some backup storage.
            _trace("Backup storage for partition " + iPartition + " is missing", 2);
            return;
            }

        // insert the data to primary and persistent store
        insertPrimaryData(iPartition, mapEntries.entrySet().iterator());

        // if the backup map was com.tangosol.net.partition.PartitionAwareBackingMap, notify it to destroy the partition
        // otherwise, remove the promoted entries from the monolithic backup map
        if (pabmBackup == null)
            {
            mapFrom.keySet().removeAll(mapEntries.keySet());
            }
        else
            {
            pabmBackup.destroyPartition(iPartition);
            }
        }

    /**
     * Called on the service thread, daemon pool thread or a write-behind
     * thread. Note that the caller is quite probably holding a
     * synchronization monitor for the correspoding ResourceMap.
     */
    public void onBackingMapEvent(com.tangosol.util.MapEvent evt)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;

        Binary            binKey        = (Binary) evt.getKey();
        PartitionedCache           service       = getService();
        int               iPartition    = service.getKeyPartition(binKey);
        PartitionedCache.PartitionControl ctrlPartition = (PartitionedCache.PartitionControl) service.getPartitionControl(iPartition);
        PartitionedCache.TransferControl  ctrlTransfer  = (PartitionedCache.TransferControl) service.getTransferControl();

        //
        // Observed backing-map events are generally handled immediately by calling
        // doBackingMapEvent().  In certain transfer-related scenarios, the handling
        // of the observed BM event may be deferred or handled specially.
        //
        // * BM events that are generated by transfer changes (e.g. primary map
        //   insertions) are handled immediately via a fast-path
        // * BM events that occur during a transfer but are directly related
        //   to the transfer (e.g. eviction during transfer snapshot) are deferred.
        //
        // There are 3 kinds of transfer that could be in-progress that
        // would prevent immediate handling of unexpected backing-map events:
        //
        // 1. Primary partition transfer (outgoing)
        // 2. Primary partition transfer (incoming)
        // 3. Backup transfer snapshot (outgoing)
        //
        // In cases 2 and 3, handling of the event (e.g. sending of backup messages)
        // should be deferred until the transfer has finished.  Case 1 breaks up into
        // 2 subcases, depending on when the BM event is raised:
        //
        // 1a. Transfer messages for the partition are being built
        // 1b. Transfer messages for the partition has already been sent
        //
        // In the case of 1a, we should amend the transfer messages to include the
        // event as an "addendum" of sorts.  Upon successful completion of the transfer,
        // the *new* primary owner will replay the addendum, as well as issue backup
        // messages.  Upon transfer failure, the old primary owner will need to issue
        // backup messages.
        //
        // In the case of 1b, we should reject the event as occurring on an "unowned"
        // partition, since it has already been "sent out the door".
        //
        while (true)
            {
            if (ctrlPartition == null)
                {
                // only possible if the partition is completely unowned; call doBME
                // to log the "un-owned" update and return
                doBackingMapEvent(evt);
                return;
                }

            // Attempt to enter the partition control gate before handling the BME.
            int     nLockState  = ctrlPartition.getLockType();
            boolean fEnter      = ctrlPartition.enter(0L);

            if (fEnter || service.isRecoveryThread() && ctrlPartition.isRecovering())
                {
                // We were able to acquire a read-lock; either no transfer is
                // in-progress (common case) or we are running on the transfer thread.
                // Determine which is the case and handle the event accordingly.
                try
                    {
                    // Avoid calling getOldValue if backing map is PABM and CCM
                    // with BinaryCalculator, see PartitionedCache.PartitionControl.setDirectStorageSize
                    if (isAdjustPartitionSize())
                        {
                        adjustStorageStats(evt, ctrlPartition.getStatistics());
                        }

                    if (ctrlPartition.isLocked())
                        {
                        // We must be on the service thread (during transfer):
                        // There are 5 possible transfer scenarios during which the
                        // service thread would be allowed to enter the partition control:
                        // * recovery from persistent store
                        //   Recovery-driven inserts would be "owned" events, with a transfer
                        //   state of PERSISTENCE.
                        //   In this state, insert events are expected (and handled on
                        //   the fast-path); all other events are deferred.
                        //
                        // * transfer-in
                        //   Incoming partition transfer would be "owned" events, with
                        //   a transfer state of XFER_IN.
                        //   In this state, insert events are expected. If the Daemon pool is started,
                        //   then insert events are deferred and handled asynchronously after the transfer
                        //   is done; otherwise (no pool) the events are handled on the fast-path.
                        //   All other events are deferred.
                        //
                        // * primary transfer-out
                        //   Outgoing primary transfer would be "unowned" events, with
                        //   a transfer-state of XFER_OUT|XFER_SENT
                        //   In this state, remove events are expected (and handled on
                        //   the fast-path); all other events are deferred
                        //
                        // * backup transfer-out
                        //   Backup snapshot should not result in BM events
                        //   In this state, no events are expected.  All events are deferred.
                        //
                        // * backup-promotion
                        //   promotion from backup would be "unowned" events, with a
                        //   transfer-state of XFER_IN
                        //   In this state, insert events are expected (and handled on
                        //   the fast-path); all other events are deferred
                        //
                        // Note: PartitionedCache.PartitionControl.isLocked() is true iff the partition lock
                        //       has been fully acquired (i.e. not currently pending).
                        //       Since we have previously entered the control, we know that
                        //       this thread must be the locking thread.

                        int     nEvtType   = evt.getId();
                        boolean fPartIn    = nLockState == PartitionedCache.PartitionControl.LOCK_PRIMARY_XFER_IN ||
                                             nLockState == PartitionedCache.PartitionControl.LOCK_PERSISTENCE;

                        if ((nEvtType == com.tangosol.util.MapEvent.ENTRY_INSERTED && fPartIn) ||
                            (nLockState == PartitionedCache.PartitionControl.LOCK_PRIMARY_XFER_OUT &&
                             ctrlPartition.getTransferState() == PartitionedCache.PartitionControl.XFER_SENT &&
                             nEvtType == com.tangosol.util.MapEvent.ENTRY_DELETED))
                            {
                            // expected event
                            if (isIndexed() && (nEvtType == com.tangosol.util.MapEvent.ENTRY_DELETED ||
                                                !service.getDaemonPool().isStarted()))
                                {
                                BinaryEntry binEntry = getTempBinaryEntry();
                                binEntry.reset(binKey, (Binary) evt.getNewValue());

                                // COH-3146: the BinaryEntry does not have the
                                //   OriginalValue but in some cases, it is needed
                                //   for index maintenance, so we must provide it here.
                                binEntry.updateOriginalBinaryValue((Binary) evt.getOldValue());

                                binEntry.ensureReadOnly();
                                updateIndex(nEvtType, binEntry, null);
                                }

                            // update the key index
                            updateKeyIndex(evt);

                            return;
                            }
                        else
                            {
                            // event needs to be deferred
                            }
                        }
                    else
                        {
                        doBackingMapEvent(evt);
                        return;
                        }
                    }
                finally
                    {
                    if (fEnter)
                        {
                        ctrlPartition.exit();
                        }
                    }
                }

            // the conditions below are very uncommon "corner-cases" of BM events
            // that arise unexpectedly while a partition is being transferred
            synchronized (ctrlPartition)
                {
                switch (ctrlPartition.getLockType())
                    {
                    default:
                    case PartitionedCache.PartitionControl.LOCK_NONE:
                        // We were not able to acquire a read-lock, but no transfer
                        // is in progress.  This must be a transient condition as
                        // state is changing; try again.
                        break;

                    case PartitionedCache.PartitionControl.LOCK_PERSISTENCE:
                    case PartitionedCache.PartitionControl.LOCK_PRIMARY_XFER_IN:
                    case PartitionedCache.PartitionControl.LOCK_BACKUP_XFER_OUT:
                        // The partition is locked for transfer, so we are unable to obtain
                        // a read-lock. Defer the processing of the BM event until the
                        // transfer lock is released.
                        ctrlPartition.addUnlockAction(instantiateDeferredEvent(evt, false));
                        return;

                    case PartitionedCache.PartitionControl.LOCK_PRIMARY_XFER_OUT:
                        if (ctrlPartition.getTransferState() == PartitionedCache.PartitionControl.XFER_SENT)
                            {
                            // transfer messages have been put on the wire and
                            // are "out the door".  Defer the handling of the event
                            // until the transfer either completes or rolls back
                            ctrlPartition.addUnlockAction(instantiateDeferredEvent(evt, false));
                            }
                        else
                            {
                            // transfer messages have not yet been put on the
                            // wire; add an addendum to be processed by the
                            // new primary owner (or by us in the case of transfer
                            // failure).  See PartitionedCache.TransferControl.finalizeReceive()
                            ctrlTransfer.deferEvent(instantiateDeferredEvent(evt, true));
                            }
                        return;
                    }
                }
            }
        }

    // Declared at the super level
    /**
     * The "component has been initialized" method-notification called out
     * of setConstructed() for the topmost component and that in turn
     * notifies all the children.
     *
     * This notification gets called before the control returns back to this
     * component instantiator (using <code>new Component.X()</code> or
     * <code>_newInstance(sName)</code>) and on the same thread. In
     * addition, visual components have a "posted" notification
     * <code>onInitUI</code> that is called after (or at the same time as)
     * the control returns back to the instantiator and possibly on a
     * different thread.
     */
    public void onInit()
        {
        // import com.tangosol.coherence.config.Config;
        // import com.tangosol.net.cache.LocalCache;
        // import com.tangosol.net.internal.StorageVersion;
        // import com.tangosol.util.SafeHashMap;
        // import com.tangosol.util.SegmentedHashMap;
        // import com.tangosol.util.WrapperObservableMap;
        // import java.util.Collections;

        PartitionedCache service = (PartitionedCache) getService();

        if (service.getBackupCount() > 0)
            {
            setBackupMap(new SegmentedHashMap());
            setBackupLeaseMap(new SafeHashMap());
            }

        PartitionedCache.BackingMapContext ctx = service.getBackingMapContext();
        setConverterUp       (ctx.getValueFromInternalConverter());
        setConverterKeyDown  (ctx.getKeyToInternalConverter());
        setConverterValueDown(ctx.getValueToInternalConverter());

        setBackingMapInternal(new WrapperObservableMap(Collections.EMPTY_MAP));
        setResourceControlMap(service.getResourceCoordinator().instantiateControlMap());

        setStatsMaxQueryThresholdMillis(
                Config.getLong("coherence.distributed.query.statistics.threshold",
                               getStatsMaxQueryThresholdMillis()).longValue());

        // COH-6601: default to a max of 10 index reevaluations per query
        setQueryRetries(Config.getInteger("coherence.query.retry", 10).intValue());

        // Keep 1000 filters for at most 10 minutes
        setQuerySizeCache(new LocalCache(1000, 10 * 60 * 1000));

        // create an interceptor to observe the addition/removal of interceptors
        setEventDispatcherInterceptor((DispatcherInterceptor) _findChild("DispatcherInterceptor"));

        // security helper
        setBackingMapAction((BackingMapAction) _newChild("BackingMapAction"));

        super.onInit();
        }

    /**
     * Ensure that the global metadata is properly persisted as part of this
     * partition's persistent form (if applicable).
     *
     * @param nPartition    the partition
     * @param store         the persistent store
     * @param oToken        the persistence token to use for persisting the
     * global metadata, or null for auto-commit
     */
    public void persistGlobalMetadata(int nPartition, com.oracle.coherence.persistence.PersistentStore store, Object oToken)
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        // import java.util.Iterator;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Map;
        // import java.util.Set;

        if (com.tangosol.persistence.CachePersistenceHelper.isGlobalPartitioningSchemePID(nPartition))
            {
            PartitionedCache    service    = getService();
            Serializer serializer = service.getSerializer();

            if (store == null || !store.isOpen())
                {
                store = service.getPartitionControl(nPartition).ensureOpenPersistentStore();
                }

            // persist the index registrations
            //
            // COH-3966: recreate indices from the source extractor,
            //           not the created indices (see IndexAwareExtractor)
            Map mapExtractor = getIndexExtractorMap();
            if (!mapExtractor.isEmpty())
                {
                for (Iterator iter = mapExtractor.entrySet().iterator(); iter.hasNext();)
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();

                    Binary binExtractor  = com.tangosol.util.ExternalizableHelper.toBinary(entry.getKey(), serializer);
                    Binary binComparator = com.tangosol.util.ExternalizableHelper.toBinary(entry.getValue(), serializer);

                    com.tangosol.persistence.CachePersistenceHelper.registerIndex(store, getCacheId(),
                                                                                  binExtractor, binComparator, oToken);
                    }
                }

            // persist the trigger registrations
            Set setTriggers = getTriggerSet();
            if (setTriggers != null)
                {
                for (Iterator iter = setTriggers.iterator(); iter.hasNext();)
                    {
                    Binary binTrigger = com.tangosol.util.ExternalizableHelper.toBinary(iter.next(), serializer);

                    com.tangosol.persistence.CachePersistenceHelper.registerTrigger(store, getCacheId(), binTrigger, oToken);
                    }
                }

            // Note: key-listeners are persisted as part of the associated partition;
            //       global-listeners are not persisted, as their registration is managed
            //       the client (see #createWelcomeRequests).
            }
        }

    /**
     * Ensure that the global metadata is properly persisted as part of this
     * partition's persistent form (if applicable).
     *
     * @param nPartition    the partition
     * @param oToken        the persistence token to use for persisting the
     * global metadata, or null for auto-commit
     */
    public void persistGlobalMetadata(int nPartition, Object oToken)
        {
        persistGlobalMetadata(nPartition, null, oToken);
        }

    /**
     * Ensure that the global metadata is properly persisted as part of this
     * partition's persistent form (if applicable).
     *
     * @param nPartition    the partition
     * @param oToken        the persistence token to use for persisting the
     * global metadata, or null for auto-commit
     */
    public void persistGlobalMetadata(int nPartition, boolean fBackup, Object oToken)
        {
        persistGlobalMetadata(nPartition,
                              getService().getPartitionControl(nPartition).ensurePersistentStore(null, /*fEvents*/ false, /*fBackup*/ fBackup), oToken);
        }

    /**
     * Ensure that the index associated with the specified extractor is
     * properly persisted (or removed) from this partition's persistent form
     * (if applicable).
     *
     * @param nPartition      the partition
     * @param extractor       the value extractor associated with the index
     * to persist or remove
     * @param comparator   the comparator associated with the index to add
     * (only used with fAdd = true)
     * @param fAdd               true iff the index should be added to the
     * persistent form, or removed otherwise
     */
    protected void persistIndexRegistration(com.tangosol.net.partition.PartitionSet parts, com.tangosol.util.ValueExtractor extractor, java.util.Comparator comparator, boolean fAdd)
        {
        // import com.oracle.coherence.persistence.PersistentStore;
        // import com.tangosol.io.Serializer;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;

        if (isPersistent())
            {
            PartitionedCache service = getService();

            // if the cache requires persistence, ensure that the index registration is
            // persisted with any partitions that are responsible for "global" metadata

            PartitionSet partsGlobal = com.tangosol.persistence.CachePersistenceHelper.getGlobalPartitions(service);
            partsGlobal.retain(parts);

            if (!partsGlobal.isEmpty())
                {
                Serializer serializer   = service.getSerializer();
                Binary     binExtractor = com.tangosol.util.ExternalizableHelper.toBinary(extractor, serializer);

                for (int iPart = partsGlobal.next(0); iPart >= 0; iPart = partsGlobal.next(iPart + 1))
                    {
                    // Note: while we could cache the serialized binary form of the
                    //       extractor/comparator from the IndexRequest, there are
                    //       several other code-paths that require persistence of global
                    //       metadata, and the additional complexity seems unwarranted

                    PartitionedCache.PartitionControl ctrlPart = (PartitionedCache.PartitionControl) service.getPartitionControl(iPart);
                    PersistentStore   store    = ctrlPart.ensureOpenPersistentStore(/*storeFrom*/ null, /*fSeal*/ true);

                    if (store != null)
                        {
                        if (fAdd)
                            {
                            // ensure lazily written cache metadata is persisted to ensure index
                            // is materialized on recovery
                            ctrlPart.ensurePersistentExtent(getCacheId());

                            com.tangosol.persistence.CachePersistenceHelper.registerIndex(store, getCacheId(), binExtractor,
                                                                                          com.tangosol.util.ExternalizableHelper.toBinary(comparator, serializer), /*oToken*/ null);
                            }
                        else
                            {
                            com.tangosol.persistence.CachePersistenceHelper.unregisterIndex(store, getCacheId(), binExtractor, /*oToken*/ null);
                            }
                        }
                    }
                }
            }
        }

    /**
     * Ensure that a key listener is properly persisted (or removed) from
     * this partition's persistent form (if applicable).
     *
     * @param member  the listening member
     * @param binKey     the key
     * @param fAdd        true iff the listener should be added to the
     * persistent form, or removed otherwise
     */
    public void persistListenerRegistration(com.tangosol.coherence.component.net.Member member, com.tangosol.util.Binary binKey, boolean fLite, boolean fAdd)
        {
        // import com.oracle.coherence.persistence.PersistentStore;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;

        if (isPersistent())
            {
            PartitionedCache           service       = getService();
            int               nPartition    = service.getKeyPartition(binKey);
            PartitionedCache.PartitionControl ctrlPartition = (PartitionedCache.PartitionControl) service.getPartitionControl(nPartition);
            long              ldtJoined     = service.getServiceMemberSet().getServiceJoinTime(member.getId());
            PersistentStore   store         = ctrlPartition.ensureOpenPersistentStore(/*storeFrom*/ null, /*fSeal*/ true);
            long              lExtentId     = getCacheId();

            ctrlPartition.ensurePersistentExtent(lExtentId);

            // the listening member (as known to this service) is uniquely identified
            // by the service join-time
            if (fAdd)
                {
                com.tangosol.persistence.CachePersistenceHelper.registerListener(store, lExtentId, binKey, ldtJoined, fLite, /*oToken*/ null);
                }
            else
                {
                com.tangosol.persistence.CachePersistenceHelper.unregisterListener(store, lExtentId, binKey, ldtJoined, /*oToken*/ null);
                }
            }
        }

    /**
     * Ensure that the specified trigger is properly persisted (or removed)
     * from this partition's persistent form (if applicable).
     *
     * @param nPartition   the partition
     * @param trigger        the trigger to persist or remove
     * @param fAdd            true iff the trigger should be added to the
     * persistent form, or removed otherwise
     */
    public void persistTriggerRegistration(com.tangosol.net.partition.PartitionSet parts, com.tangosol.util.MapTrigger trigger, boolean fAdd)
        {
        // import com.oracle.coherence.persistence.PersistentStore;
        // import com.tangosol.io.Serializer;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;

        if (isPersistent())
            {
            PartitionedCache    service    = getService();
            Serializer serializer = service.getSerializer();

            // if the cache requires persistence, ensure that the trigger registration is
            // persisted with any partitions that are responsible for "global" metadata
            PartitionSet partsGlobal = com.tangosol.persistence.CachePersistenceHelper.getGlobalPartitions(service);
            partsGlobal.retain(parts);

            for (int iPart = partsGlobal.next(0); iPart >= 0; iPart = partsGlobal.next(iPart + 1))
                {
                Binary          binTrigger = com.tangosol.util.ExternalizableHelper.toBinary(trigger, serializer);
                PersistentStore store      = service.getPartitionControl(iPart).ensureOpenPersistentStore(/*storeFrom*/ null, /*fSeal*/ true);

                if (fAdd)
                    {
                    com.tangosol.persistence.CachePersistenceHelper.registerTrigger(store, getCacheId(), binTrigger, /*oToken*/ null);
                    }
                else
                    {
                    com.tangosol.persistence.CachePersistenceHelper.unregisterTrigger(store, getCacheId(), binTrigger, /*oToken*/ null);
                    }
                }
            }
        }

    /**
     * Check to see if the given entry's value has changed as a result of an
     * "invoke" operation and update the storage and status accordingly.
     */
    public void postInvoke(BinaryEntry entry)
        {
        // import com.tangosol.internal.util.HeuristicCommitException;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;

        // Note: while this appears to follow a very similar flow to postPut()
        //       and postRemove(), the handling of the "result" is subtly different for
        //       the 3 cases ("invoke", "put", and "remove").
        if (entry.isValueChanged())
            {
            entry.ensureReadOnly();

            Binary binKey = entry.getBinaryKey();
            try
                {
                if (entry.isValueRemoved())
                    {
                    removePrimaryResource(binKey, /*fBlind*/ true, entry.isSynthetic());
                    }
                else // entry.isValueUpdated() || entry.isExpireChanged()
                    {
                    // need to pull the new value from the entry after firing triggers
                    Binary  binValueNew = entry.getBinaryValue();
                    Binary  binValueOld = entry.getOriginalBinaryValue();
                    long    cExpiry     = entry.getExpiry();
                    boolean fSynthetic  = entry.isSynthetic() ||
                                          entry.isExpireChanged() && !entry.isValueUpdated();

                    if (Base.equals(binValueNew, binValueOld) && cExpiry == 0L ||
                        !entry.isValueUpdated() && binValueOld == null)
                        {
                        // optimize-out an equivalent update or
                        // an expiry only change to a non-existent entry
                        }
                    else
                        {
                        putPrimaryResource(binKey, binValueNew, cExpiry, /*fBlind*/ true, fSynthetic);
                        }
                    }
                }
            catch (Throwable e)
                {
                throw new HeuristicCommitException(e);
                }
            }
        }

    /**
     * Check to see if the entry in the given list has changed as a result
     * of an "invoke" operation and update the storage and status
     * accordingly.
     */
    public void postInvokeAll(java.util.Collection colEntries)
        {
        // import com.tangosol.internal.util.HeuristicCommitException;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import java.util.HashMap;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Set;

        Map mapUpdates = new HashMap(colEntries.size());
        Set setRemove  = null;
        for (Iterator iter = colEntries.iterator(); iter.hasNext();)
            {
            BinaryEntry entry = (BinaryEntry) iter.next();
            if (entry.isValueChanged())
                {
                entry.ensureReadOnly();

                Binary binKey = entry.getBinaryKey();
                try
                    {
                    if (entry.isValueRemoved())
                        {
                        boolean fSynthetic = entry.isSynthetic();
                        if (fSynthetic)
                            {
                            removePrimaryResource(binKey, /*fBlind*/ true, fSynthetic);
                            }
                        else
                            {
                            if (setRemove == null)
                                {
                                setRemove = new HashSet(colEntries.size());
                                }
                            setRemove.add(binKey);
                            }
                        }
                    else
                        {
                        // need to pull the new value from the entry after firing triggers
                        Binary  binValueNew = entry.getBinaryValue();
                        Binary  binValueOld = entry.getOriginalBinaryValue();
                        long    cExpiry     = entry.getExpiry();
                        boolean fSynthetic  = entry.isSynthetic() ||
                                              entry.isExpireChanged() && !entry.isValueUpdated();

                        if (Base.equals(binValueNew, binValueOld) && cExpiry == 0L ||
                            !entry.isValueUpdated() && binValueOld == null)
                            {
                            // optimize-out an equivalent update or
                            // an expiry only change to a non-existent entry
                            }
                        else if (entry.isExpireChanged() || fSynthetic)
                            {
                            putPrimaryResource(binKey, binValueNew, cExpiry, /*fBlind*/ true, fSynthetic);
                            }
                        else
                            {
                            mapUpdates.put(getCanonicalKey(binKey), binValueNew);
                            }
                        }
                    }
                catch (Throwable e)
                    {
                    throw new HeuristicCommitException(e);
                    }
                }
            }

        if (!mapUpdates.isEmpty())
            {
            putAllPrimaryResource(mapUpdates);
            }

        if (setRemove != null)
            {
            removeAllPrimaryResource(setRemove);
            }
        }

    /**
     * Check to see if the given entry's value has changed as a result of an
     * "put" operation and update the storage and status accordingly.
     */
    protected void postPut(BinaryEntry entry, EntryStatus status, boolean fBlind)
        {
        // import com.tangosol.internal.util.HeuristicCommitException;
        // import com.tangosol.util.Binary;

        // Note: while this appears to follow a very similar flow to postInvoke()
        //       and postRemove(), the handling of the "result" is subtly different for
        //       the 3 cases ("invoke", "put", and "remove").
        // Note: we process the "put" operation as a change even if the value has
        //       not changed. One reason for this is reset for expiry and decorations;
        //       another reason is that some clients (e.g. NearCache) expect it
        //       unconditionally. This is not consistent with how invoke() is handled.
        //       A read-only invoke() does not generate any events.

        entry.ensureReadOnly();

        Binary binKey         = entry.getBinaryKey();
        Binary binValueReturn = null;
        try
            {
            if (entry.isValueRemoved())
                {
                binValueReturn = removePrimaryResource(binKey, fBlind, entry.isSynthetic());
                }
            else
                {
                // need to pull the new value from the entry after firing triggers
                Binary binValueNew = entry.getBinaryValue();
                long   cExpiry     = entry.getExpiry();

                binValueReturn = putPrimaryResource(binKey, binValueNew, cExpiry, fBlind, entry.isSynthetic());
                }
            }
        catch (Throwable e)
            {
            throw new HeuristicCommitException(e);
            }

        if (!fBlind)
            {
            status.setResult(binValueReturn);
            }
        }

    /**
     * Check to see if the given entry's value has changed as a result of an
     * "put" operation and update the storage and status accordingly.
     */
    protected void postPutAll(EntryStatus[] aStatus)
        {
        // import java.util.HashMap;
        // import java.util.Map;
        // import java.util.Set;

        int cSize = aStatus.length;
        Map map   = new HashMap(cSize);
        for (int i = 0; i < cSize; i++)
            {
            EntryStatus status = aStatus[i];
            BinaryEntry binEntry = status.getBinaryEntry();

            // A putAll would not normally have expiry but if the
            // trigger has added expiry, or the trigger removed the
            // entry, then process it individually as a put, otherwise
            // add it to the map for a bulk putAll call on the backing map
            if (binEntry.isExpireChanged() || binEntry.isValueRemoved())
                {
                postPut(binEntry, status, true);
                }
            else
                {
                map.put(getCanonicalKey(binEntry.getBinaryKey()),
                        binEntry.getBinaryValue());
                }
            }

        putAllPrimaryResource(map);
        }

    /**
     * Check to see if the given entry's value has changed as a result of an
     * "remove" operation and update the storage and status accordingly.
     */
    protected void postRemove(BinaryEntry entry, EntryStatus status, boolean fBlind)
        {
        // import com.tangosol.internal.util.HeuristicCommitException;
        // import com.tangosol.util.Binary;

        // Note: while this appears to follow a very similar flow to postInvoke()
        //       and postPut(), the handling of the "result" is subtly different for
        //       the 3 cases ("invoke", "put", and "remove").
        if (entry.isValueChanged())
            {
            entry.ensureReadOnly();

            Binary  binKey         = entry.getBinaryKey();
            Binary  binValueReturn = null;
            boolean fExists        = false;

            try
                {
                if (fBlind)
                    {
                    // for the "remove" operation, we need to do a contains-key check for
                    // existence, prior to changing the BM
                    fExists = getBackingMapInternal().containsKey(binKey);
                    }

                if (entry.isValueRemoved())
                    {
                    binValueReturn = removePrimaryResource(binKey, fBlind, entry.isSynthetic());
                    }
                else // entry.isValueUpdated() || entry.isExpireUpdated()
                    {
                    // need to pull the new value from the entry after firing triggers
                    Binary  binValueNew = entry.getBinaryValue();
                    long    cExpiry     = entry.getExpiry();
                    boolean fSynthetic  = entry.isSynthetic() ||
                                          entry.isExpireChanged() && !entry.isValueUpdated();

                    binValueReturn = putPrimaryResource(binKey, binValueNew, cExpiry, fBlind, fSynthetic);
                    }

                if (fBlind)
                    {
                    binValueReturn = fExists ? Binary.EMPTY : null;
                    }
                }
            catch (Throwable e)
                {
                throw new HeuristicCommitException(e);
                }

            status.setResult(binValueReturn);
            }
        }

    /**
     * Check to see if the given entry's value has been removed as a result
     * of a "remove" operation and update the storage and status accordingly.
     */
    protected boolean postRemoveAll(EntryStatus[] aStatus)
        {
        // import com.tangosol.internal.util.HeuristicCommitException;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import java.util.HashMap;
        // import java.util.HashSet;
        // import java.util.Map;
        // import java.util.Set;

        int     cSize      = aStatus.length;
        Set     setRemove  = null;
        Map     mapUpdates = null;
        boolean fRemoved   = false;

        for (int i = 0; i < cSize; i++)
            {
            EntryStatus status   = aStatus[i];
            BinaryEntry binEntry = status.getBinaryEntry();

            if (binEntry.isValueChanged())
                {
                Binary binKey = binEntry.getBinaryKey();

                binEntry.ensureReadOnly();

                try
                    {
                    if (binEntry.isValueRemoved())
                        {
                        if (setRemove == null)
                            {
                            setRemove = new HashSet(cSize);
                            }

                        setRemove.add(binEntry.getBinaryKey());
                        fRemoved |= true;
                        }
                    else // binEntry.isValueUpdated() || binEntry.isExpireUpdated()
                        {
                        // need to pull the new value from the entry after firing triggers
                        Binary  binValueNew = binEntry.getBinaryValue();
                        Binary  binValueOld = binEntry.getOriginalBinaryValue();
                        long    cExpiry     = binEntry.getExpiry();
                        boolean fSynthetic  = binEntry.isSynthetic() ||
                                              binEntry.isExpireChanged() && !binEntry.isValueUpdated();

                        if (Base.equals(binValueNew, binValueOld) && cExpiry == 0L ||
                            !binEntry.isValueUpdated() && binValueOld == null)
                            {
                            // optimize-out an equivalent update or
                            // an expiry only change to a non-existent entry
                            }
                        else if (binEntry.isExpireChanged() || fSynthetic)
                            {
                            putPrimaryResource(binKey, binValueNew, cExpiry, /*fBlind*/ true, fSynthetic);
                            }
                        else
                            {
                            if (mapUpdates == null)
                                {
                                mapUpdates = new HashMap(cSize);
                                }
                            mapUpdates.put(getCanonicalKey(binKey), binValueNew);
                            }
                        }
                    }
                catch (Throwable e)
                    {
                    throw new HeuristicCommitException(e);
                    }
                }
            }

        if (mapUpdates != null)
            {
            putAllPrimaryResource(mapUpdates);
            }

        if (setRemove != null)
            {
            removeAllPrimaryResource(setRemove);
            }

        return fRemoved;
        }

    /**
     * Called on the service thread, daemon pool thread or a write-behind
     * thread.
     *
     * @param event       the MapEvent to be dispatched
     * @param binEntry  the entry object
     *
     * @return a (polymorphic) "event holder" for the generated $MapEvent
     * messages.  See accumulateMapEvent().
     */
    public PartitionedCache.MapEvent prepareDispatch(com.tangosol.util.MapEvent event, com.tangosol.util.Filter filter, com.tangosol.coherence.component.net.Member member, boolean fLite, long lFilterId)
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ConverterCollections$ConverterMapEvent as com.tangosol.util.ConverterCollections.ConverterMapEvent;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
        // import com.tangosol.util.MapEventTransformer;

        // prepare to dispatch events

        int     nEventType   = event.getId();
        Binary  binKey       = (Binary) event.getKey();
        Binary  binValueOld  = (Binary) event.getOldValue();
        Binary  binValueNew  = (Binary) event.getNewValue();
        boolean fTransformed = false;

        com.tangosol.util.ConverterCollections.ConverterMapEvent evtConv = createConverterEvent(
                nEventType, binKey, binValueOld, binValueNew, /*binEntry*/null);

        // TODO: hraja - caller needs to catch Runtime exception in case Filter
        //       or MapEventTransformer blows

        // Note: we assume the provided filter has already passed with the given com.tangosol.util.MapEvent

        if (filter instanceof MapEventTransformer)
            {
            com.tangosol.util.MapEvent evtTrans = ((MapEventTransformer) filter).transform(evtConv);
            if (evtTrans == null)
                {
                return null;
                }

            // if either key or values were converted, need to assume a change
            // and use the converted values
            boolean fNew = evtTrans != evtConv;
            if (fNew || evtConv.isKeyConverted())
                {
                binKey       = (Binary) getConverterKeyDown().convert(evtTrans.getKey());
                fTransformed = true;
                }
            if (fNew || evtConv.isOldValueConverted())
                {
                binValueOld  = (Binary) getConverterValueDown().convert(evtTrans.getOldValue());
                fTransformed = true;
                }
            if (fNew || evtConv.isNewValueConverted())
                {
                binValueNew  = (Binary) getConverterValueDown().convert(evtTrans.getNewValue());
                fTransformed = true;
                }
            }

        if (fTransformed)
            {
            nEventType |= PartitionedCache.MapEvent.EVT_TRANSFORMED;
            }

        return prepareEventMessage(
                SingleMemberSet.instantiate(member),
                nEventType,
                binKey,
                fLite || fTransformed ? null : binValueOld,
                fLite || fTransformed ? null : binValueNew,
                new long[] {lFilterId}, event.getPartition(), event.getVersion());
        }

    /**
     * Called on the service thread, daemon pool thread or a write-behind
     * thread.
     *
     * @param event       the MapEvent to be dispatched
     * @param binEntry  the entry object
     *
     * @return a (polymorphic) "event holder" for the generated $MapEvent
     * messages.  See accumulateMapEvent().
     */
    public Object prepareDispatch(com.tangosol.util.MapEvent event, BinaryEntry binEntry)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ConverterCollections$ConverterMapEvent as com.tangosol.util.ConverterCollections.ConverterMapEvent;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
        // import com.tangosol.util.MapEventTransformer;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;

        // prepare to dispatch events
        Object oEvtHolder      = null;
        Map    mapListeners    = getListenerMap();
        Map    mapKeyListeners = getKeyListenerMap();
        int    cFilters        = mapListeners    == null ? 0 : mapListeners.size();
        int    cKeys           = mapKeyListeners == null ? 0 : mapKeyListeners.size();

        if (cFilters == 0 && cKeys == 0)
            {
            return oEvtHolder;
            }

        int       nEventType  = event.getId() |
                                (binEntry.isSynthetic() ? PartitionedCache.MapEvent.EVT_SYNTHETIC : 0) |
                                (event instanceof CacheEvent ?
                                 (((CacheEvent) event).isExpired() ? PartitionedCache.MapEvent.EVT_EXPIRED : 0) :
                                 0);
        Binary    binKey      = (Binary) event.getKey();
        Binary    binValueOld = (Binary) event.getOldValue();
        Binary    binValueNew = (Binary) event.getNewValue();
        MemberSet setStd      = new MemberSet();
        MemberSet setLite     = new MemberSet();

        // check the key listeners
        if (cKeys > 0)
            {
            Map mapMembers = (Map) mapKeyListeners.get(binKey);
            if (mapMembers != null)
                {
                for (Iterator iter = mapMembers.entrySet().iterator(); iter.hasNext();)
                    {
                    java.util.Map.Entry   entry  = (java.util.Map.Entry) iter.next();
                    Member  member = (Member)  entry.getKey();
                    Boolean FLite  = (Boolean) entry.getValue();

                    if (FLite.booleanValue())
                        {
                        setLite.add(member);
                        }
                    else
                        {
                        setStd.add(member);
                        }
                    }
                }
            }

        // check all filter listeners
        long[] alFilterId = null;
        if (cFilters > 0)
            {
            Map mapFilterId = getFilterIdMap();
            Set setIds      = new HashSet();

            com.tangosol.util.ConverterCollections.ConverterMapEvent evtConv = null;

            for (Iterator iterFilter = mapListeners.entrySet().iterator(); iterFilter.hasNext();)
                {
                java.util.Map.Entry   entry            = (java.util.Map.Entry) iterFilter.next();
                Object  filter           = entry.getKey();
                Map     mapMembers       = (Map) entry.getValue();
                boolean fTransformed     = false;
                Binary  binKeyTrans      = binKey;
                Binary  binValueOldTrans = binValueOld;
                Binary  binValueNewTrans = binValueNew;

                if (filter instanceof Filter)
                    {
                    if (evtConv == null)
                        {
                        evtConv = createConverterEvent(
                                nEventType, binKey, binValueOld, binValueNew, binEntry);
                        }

                    try
                        {
                        if (!((Filter) filter).evaluate(evtConv))
                            {
                            continue;
                            }
                        }
                    catch (RuntimeException e)
                        {
                        _trace("Exception occurred during filter evaluation: " + filter +
                               "; removing the filter...", 1);
                        _trace(e);
                        iterFilter.remove();
                        continue;
                        }

                    if (filter instanceof MapEventTransformer)
                        {
                        try
                            {
                            // clear cached conversions before applying the transformer
                            // as the presence of a previously converted key/value will
                            // force an unnecessary serialization (COH-1360)
                            evtConv.clearConverted();

                            com.tangosol.util.MapEvent evtTrans = ((MapEventTransformer) filter).transform(evtConv);
                            if (evtTrans == null)
                                {
                                continue;
                                }

                            // if either key or values were converted, need to assume a change
                            // and use the converted values
                            boolean fNew = evtTrans != evtConv;
                            if (fNew || evtConv.isKeyConverted())
                                {
                                binKeyTrans  = (Binary) getConverterKeyDown().convert(evtTrans.getKey());
                                fTransformed = true;
                                }
                            if (fNew || evtConv.isOldValueConverted())
                                {
                                binValueOldTrans = (Binary) getConverterValueDown().convert(evtTrans.getOldValue());
                                fTransformed     = true;
                                }
                            if (fNew || evtConv.isNewValueConverted())
                                {
                                binValueNewTrans = (Binary) getConverterValueDown().convert(evtTrans.getNewValue());
                                fTransformed     = true;
                                }
                            }
                        catch (RuntimeException e)
                            {
                            _trace("Exception occurred during event transformation: " + filter +
                                   "; removing the filter...", 1);
                            _trace(e);
                            iterFilter.remove();
                            continue;
                            }
                        }
                    }

                // all non-transformed events could be sent at once (for all filters);
                // all transformed events should be sent individually (one per filter)
                MemberSet setStdTrans  = fTransformed ? new MemberSet() : setStd;
                MemberSet setLiteTrans = fTransformed ? new MemberSet() : setLite;
                Set       setIdsTrans  = fTransformed ? new HashSet()   : setIds;

                for (Iterator iter = mapMembers.entrySet().iterator(); iter.hasNext();)
                    {
                    java.util.Map.Entry entryMember = (java.util.Map.Entry) iter.next();

                    Member  member = (Member)  entryMember.getKey();
                    Boolean FLite  = (Boolean) entryMember.getValue();

                    // both mapMemberFilterId and setFilterId could be null only if
                    // they have been concurrently removed by the service thread
                    Map mapMemberFilterId = (Map) mapFilterId.get(member);
                    Set setFilterId       = mapMemberFilterId == null ? null :
                                            (Set) mapMemberFilterId.get(filter == Binary.EMPTY ? null : filter);

                    if (setFilterId != null)
                        {
                        setIdsTrans.addAll(setFilterId);

                        if (FLite.booleanValue())
                            {
                            if (!setStdTrans.contains(member))
                                {
                                setLiteTrans.add(member);
                                }
                            }
                        else
                            {
                            setStdTrans.add(member);
                            setLiteTrans.remove(member);
                            }
                        }
                    }

                if (fTransformed)
                    {
                    long[] alFilterIdTrans = toLongArray(setIdsTrans);

                    if (!setLiteTrans.isEmpty())
                        {
                        oEvtHolder = accumulateMapEvents(oEvtHolder, prepareEventMessage(
                                setLiteTrans, nEventType | PartitionedCache.MapEvent.EVT_TRANSFORMED,
                                binKeyTrans, null, null, alFilterIdTrans, event.getPartition(), event.getVersion()));
                        }

                    if (!setStdTrans.isEmpty())
                        {
                        oEvtHolder = accumulateMapEvents(oEvtHolder, prepareEventMessage(
                                setStdTrans, nEventType | PartitionedCache.MapEvent.EVT_TRANSFORMED,
                                binKeyTrans, binValueOldTrans, binValueNewTrans, alFilterIdTrans,
                                event.getPartition(), event.getVersion()));
                        }

                    // Clear cached conversions to protect subsequent filters from
                    // transformed side-effects (COH-3063)
                    evtConv.clearConverted();
                    }
                }

            alFilterId = toLongArray(setIds);
            }

        if (!setLite.isEmpty())
            {
            // even if the listeners are light, inform them about a change to a "put/putAll"
            // operation by a trigger or an interceptor (COH-15130);
            // (we reuse the VALUE_STALE flag for that purpose)
            oEvtHolder = accumulateMapEvents(oEvtHolder, prepareEventMessage(
                    setLite, nEventType, binKey, null, binEntry.isValueStale() ? binValueNew : null,
                    alFilterId, event.getPartition(), event.getVersion()));
            }

        if (!setStd.isEmpty())
            {
            oEvtHolder = accumulateMapEvents(oEvtHolder, prepareEventMessage(
                    setStd, nEventType, binKey, binValueOld, binValueNew, alFilterId,
                    event.getPartition(), event.getVersion()));
            }

        return oEvtHolder;
        }

    /**
     * Prepare a MapEvent message.
     */
    protected PartitionedCache.MapEvent prepareEventMessage(com.tangosol.coherence.component.net.MemberSet setMembers, int nEventType, com.tangosol.util.Binary binKey, com.tangosol.util.Binary binValueOld, com.tangosol.util.Binary binValueNew, long[] alFilterId, int iPartition, long lVersion)
        {
        // import com.tangosol.util.LongArray;
        // import com.tangosol.internal.tracing.Span;
        // import com.tangosol.internal.tracing.TracingHelper;

        PartitionedCache   service   = getService();
        LongArray laPending = service.getPendingEvents();

        PartitionedCache.MapEvent msg = (PartitionedCache.MapEvent) service.instantiateMessage("MapEvent");

        // create the EventSUID, register it and get OldestPendingEventSUID atomically
        long lEventSUID;
        long lOldestEventSUID;
        synchronized (laPending)
            {
            lEventSUID = service.getSUID(PartitionedCache.SUID_EVENT);
            laPending.set(lEventSUID, msg); // store event for possible re-send see PartitionedCache.onInterval
            lOldestEventSUID = service.getOldestPendingEventSUID(); // cannot be -1
            }

        msg.setEventSUID(lEventSUID);
        msg.setOldestPendingEventSUID(lOldestEventSUID);
        msg.setToMemberSet(setMembers);
        msg.setCacheId(getCacheId());
        msg.setEventType(nEventType);
        msg.setKey(binKey);
        msg.setOldValue(binValueOld);
        msg.setNewValue(binValueNew);
        msg.setFilterId(alFilterId);
        msg.setPartition(iPartition);
        msg.setVersion(lVersion);

        Span span = TracingHelper.getActiveSpan();
        if (span != null)
            {
            msg.setTracingSpanContext(span.getContext());
            }

        // update event stats
        setStatsEventsDispatched(getStatsEventsDispatched() + 1L);
        return msg;
        }

    /**
     * Ensure that the specified partition is prepared to store data. Called
     * on the service thread only.
     *
     * @param iPartition  the partition number to ensure
     * @param fPrimary  if true the primary content should be prepared;
     * otherwise the backup content
     *
     * @see #releasePartition, movePartition
     */
    public void preparePartition(int iPartition, boolean fPrimary)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap;
        // import java.util.Map;

        Map map = fPrimary ? getBackingMapInternal() : getBackupMap();

        if (map instanceof PartitionAwareBackingMap)
            {
            ensurePartition(((PartitionAwareBackingMap) map), iPartition);
            }
        else if (fPrimary)
            {
            PartitionAwareBackingMap mapKeyIndex = getPartitionedKeyIndex();
            if (mapKeyIndex != null)
                {
                ensurePartition(mapKeyIndex, iPartition);
                }
            }
        }

    /**
     * Register this CacheId with all of the PersistentStores.
     */
    public void preparePersistentExtent()
        {
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.LongArray;

        if (isPersistent())
            {
            PartitionedCache      service  = getService();
            LongArray    laCaches = service.getPersistentCacheIds();
            long         lCacheId = getCacheId();
            PartitionSet parts    = service.calculatePartitionSet(service.getThisMember(), 0);

            for (int i = parts.next(0); i >= 0; i = parts.next(i + 1))
                {
                service.getPartitionControl(i).preparePersistentExtent(lCacheId, laCaches, /*fBackup*/ false);
                }

            if (service.isBackupPersistence())
                {
                for (int i = 1; i <= service.getBackupCount(); i++)
                    {
                    PartitionSet partsBackup = service.calculatePartitionSet(service.getThisMember(), i);

                    for (int j = partsBackup.next(0); j >= 0; j = partsBackup.next(j + 1))
                        {
                        service.getPartitionControl(j).preparePersistentExtent(lCacheId, laCaches, /*fBackup*/ true);
                        }
                    }
                }
            }
        }

    protected java.util.List processIndexFailure(Throwable e, com.tangosol.util.ValueExtractor extractor, BinaryEntry binEntry, java.util.List list)
        {
        // import java.util.Comparator;
        // import java.util.LinkedList;

        String sKey = "";
        try
            {
            sKey = String.valueOf(binEntry.getKey());
            }
        catch (RuntimeException ignored) {}

        _trace("Exception occurred during index update of key " + sKey + " (" +
               binEntry.getBinaryKey() + "); removing index (" + extractor + ")", 1);
        _trace(e);

        removeIndex(null, getService().collectOwnedPartitions(true), extractor,
                    (Comparator) getIndexExtractorMap().get(extractor));

        if (list == null)
            {
            list = new LinkedList();
            }
        list.add(extractor);

        return list;
        }

    /**
     * Called on the service thread or daemon pool thread.
     */
    public void processInterceptors(java.util.Collection colEntries)
        {
        // import java.util.Iterator;

        PartitionedCache.EventsHelper evtHelper = getService().getEventsHelper();
        if (getTriggerSet() != null || evtHelper.hasPreCommitInterceptors(this))
            {
            for (Iterator iter = colEntries.iterator(); iter.hasNext(); )
                {
                BinaryEntry entry = (BinaryEntry) iter.next();

                if (entry != null && (entry.isValueChanged() || entry.isValueLoaded()))
                    {
                    processTriggers(entry);
                    evtHelper.onEntryChanging(entry, null);
                    }
                }
            }
        }

    /**
     * Called on the service thread or daemon pool thread.
     */
    public void processTriggers(BinaryEntry binEntry)
        {
        // import com.tangosol.util.MapTrigger;
        // import java.util.Iterator;
        // import java.util.Set;

        Set setTriggers = getTriggerSet();
        if (setTriggers != null)
            {
            for (Iterator iterTrigger = setTriggers.iterator(); iterTrigger.hasNext();)
                {
                MapTrigger trigger = (MapTrigger) iterTrigger.next();

                trigger.process(binEntry);
                }
            }
        }

    /**
     * Called on the service or a daemon pool thread after acquiring the key
     * lock.
     */
    public void put(PartitionedCache.InvocationContext ctxInvoke, EntryStatus status, com.tangosol.util.Binary binValueNew, long cMillis, boolean fBlind)
        {
        // import com.tangosol.net.cache.CacheMap;
        // import java.util.Map;
        // import java.util.Set;

        Map          mapResource = getBackingMapInternal();
        BinaryEntry entry       = status.getBinaryEntry();

        entry.updateBinaryValue(binValueNew); // pseudo "invoke"

        if (cMillis != CacheMap.EXPIRY_DEFAULT)
            {
            if (cMillis > 0 && !(mapResource instanceof CacheMap))
                {
                throw new UnsupportedOperationException(
                        "Class \"" + mapResource.getClass().getName() +
                        "\" does not implement CacheMap interface");
                }

            entry.expire(cMillis);
            }

        Set setEntriesAdded = ctxInvoke.processInterceptors();

        if (entry.getBinaryValue() != binValueNew)
            {
            // reuse the "stale" flag to indicate that the "put" value has been
            // changed by the interceptors
            entry.markStale();
            }

        postPut(entry, status, fBlind);
        ctxInvoke.postInvokeAll(setEntriesAdded);
        }

    /**
     * Called on the service or a daemon pool thread after acquiring the key
     * lock.
     */
    public void putAll(PartitionedCache.InvocationContext ctxInvoke, EntryStatus[] aStatus, com.tangosol.util.Binary[] aBinValuesNew)
        {
        // import java.util.Set;

        // update new values before calling prepare
        int cSize = aStatus.length;
        for (int i = 0; i < cSize; i++)
            {
            EntryStatus status = aStatus[i];
            BinaryEntry entry  = status.getBinaryEntry();

            entry.updateBinaryValue(aBinValuesNew[i]); // pseudo "invoke"
            }

        Set setEntriesAdded = ctxInvoke.processInterceptors();

        for (int i = 0; i < cSize; i++)
            {
            EntryStatus status = aStatus[i];
            BinaryEntry entry  = status.getBinaryEntry();

            if (entry.getBinaryValue() != aBinValuesNew[i])
                {
                // reuse the "stale" flag to indicate that the "putAll" value has been
                // changed by the interceptors
                entry.markStale();
                }
            }

        postPutAll(aStatus);
        ctxInvoke.postInvokeAll(setEntriesAdded);
        }

    /**
     * Helper for a "put" operation into the primary storage as a result of
     * a distribution transfer, backup restore or invocation change. It
     * includes the special handling for entries that have an expiry encoded.
     */
    protected void putAllPrimaryResource(java.util.Map mapEntries)
        {
        // import com.tangosol.internal.util.HeuristicCommitException;

        if (!mapEntries.isEmpty())
            {
            try
                {
                Map map = getBackingMapInternal();
                map.putAll(mapEntries);
                }
            catch (Throwable e)
                {
                throw new HeuristicCommitException(e);
                }
            }
        }

    /**
     * Helper for a "put" operation into the primary storage as a result of
     * a distribution transfer, backup restore or invocation change. It
     * includes the special handling for entries that have an expiry encoded.
     */
    public void putPrimaryResource(com.tangosol.util.Binary binKey, com.tangosol.util.Binary binValue)
        {
        putPrimaryResource(binKey, binValue, decodeExpiry(binValue), /*fBlind*/ true, /*fSynthetic*/ false);
        }

    /**
     * Helper for a "put" operation into the primary storage as a result of
     * a distribution transfer, backup restore or invocation change. It
     * includes the special handling for entries that have an expiry encoded.
     */
    protected com.tangosol.util.Binary putPrimaryResource(com.tangosol.util.Binary binKey, com.tangosol.util.Binary binValue, long cExpiry, boolean fBlind, boolean fSynthetic)
        {
        // import com.tangosol.net.cache.CacheMap;
        // import com.tangosol.net.cache.ConfigurableCacheMap as com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        // import java.util.Collections;
        // import java.util.Map;

        Map    map            = getBackingMapInternal();
        Binary binValueReturn = null;

        if (fSynthetic)
            {
            // if the put is "synthetic", operate directly on the RWBM internal map
            map = getBackingInternalCache();
            }

        // intern the binary key
        binKey = getCanonicalKey(binKey);

        // Note: for non-CCM CacheMaps and EXPIRY_DEFAULT, we have no way to compute
        //       the value (and we already warned them).
        if (cExpiry != CacheMap.EXPIRY_DEFAULT)
            {
            if (map instanceof CacheMap)
                {
                com.tangosol.net.cache.ConfigurableCacheMap mapCCM = getBackingConfigurableCache();
                if (mapCCM == null)
                    {
                    // for backing-map implementations that are not CCM's (if any),
                    // we unfortunately have no choice but to decorate
                    if (!com.tangosol.util.ExternalizableHelper.isDecorated(binValue, com.tangosol.util.ExternalizableHelper.DECO_EXPIRY))
                        {
                        binValue = encodeExpiry(binValue, cExpiry);
                        }
                    }
                else
                    {
                    // as of 12.1.3, we no longer decorate values in the primary
                    // backing-map if it is a com.tangosol.net.cache.ConfigurableCacheMap (COH-8587)
                    binValue = com.tangosol.util.ExternalizableHelper.undecorate(binValue, com.tangosol.util.ExternalizableHelper.DECO_EXPIRY);

                    if (cExpiry > 0L && cExpiry != mapCCM.getExpiryDelay())
                        {
                        scheduleEviction(cExpiry);
                        }
                    }

                binValueReturn = (Binary) ((CacheMap) map).put(binKey, binValue, cExpiry);

                if (cExpiry != CacheMap.EXPIRY_NEVER)
                    {
                    // make sure that PotentiallyEvicting is flagged. The Storage may
                    // have been created as non-evicting however a three-value put
                    // means it could now be evicting
                    setPotentiallyEvicting(true);
                    }
                }
            else
                {
                _trace("Dropping an entry expiry attribute due to incompatible "
                       + "backing map configuration (cache=" + getCacheName() + ')', 1);
                }
            }
        else if (fBlind && isPreferPutAllPrimary())
            {
            map.putAll(Collections.singletonMap(binKey, binValue));
            }
        else
            {
            binValueReturn = (Binary) map.put(binKey, binValue);
            }

        return binValueReturn;
        }

    /**
     * ForkJoinTask that splits the aggregate request targeting multiple partitions into
     * multiple tasks (one per partition) that can be executed in parallel.
     */
    public static class PartitionedAggregateTask<P>
            extends RecursiveTask<P>
        {
        /**
         * Construct {@link PartitionedAggregateTask}.
         *
         * @param storage      the Storage instance to query
         * @param filter       the Filter to evaluate
         * @param agent        the agent to use for aggregation
         * @param parts        the set of partitions to query
         */
        public PartitionedAggregateTask(Storage storage, Filter filter, InvocableMap.StreamingAggregator<?, ?, P, ?> agent, PartitionSet parts)
            {
            f_storage = storage;
            f_filter  = filter;
            f_agent   = agent;
            f_parts   = parts;
            }

        @Override
        protected P compute()
            {
            if (f_parts.cardinality() == 1)
                {
                f_agent.accumulate(f_storage.createStreamer(f_filter, f_agent, f_parts));
                return f_agent.getPartialResult();
                }
            else
                {
                PartitionSet partMask = f_parts;

                PartitionedAggregateTask<P>[] aTasks = new PartitionedAggregateTask[partMask.cardinality()];
                int nPos = 0;
                for (int nPart : partMask)
                    {
                    PartitionSet part = new PartitionSet(partMask.getPartitionCount(), nPart);

                    // create PartitionedAggregateTask for each partition
                    aTasks[nPos++] = new PartitionedAggregateTask(f_storage, f_filter, f_agent.supply(), part);
                    }

                invokeAll(aTasks);

                for (int i = 0; i < aTasks.length; i++)
                    {
                    f_agent.combine(aTasks[i].join());
                    }

                return f_agent.getPartialResult();
                }
            }

        // ---- data members ------------------------------------------------

        private final Storage f_storage;
        private final Filter f_filter;
        private final InvocableMap.StreamingAggregator<?, ?, P, ?> f_agent;
        private final PartitionSet f_parts;
        }

    /**
     * ForkJoinTask that splits the query request targeting multiple partitions into
     * multiple tasks (one per partition) that can be executed in parallel.
     */
    public static class PartitionedQueryTask
            extends RecursiveTask<QueryResult>
        {
        /**
         * Construct {@link PartitionedQueryTask}.
         *
         * @param storage      the Storage instance to query
         * @param filter       the Filter to evaluate
         * @param nQueryType   the query type; one of QUERY_* constants
         * @param parts        the set of partitions to query
         * @param lIdxVersion  the index version
         */
        public PartitionedQueryTask(Storage storage, Filter filter, int nQueryType, PartitionSet parts, long lIdxVersion)
            {
            f_storage     = storage;
            f_filter      = filter;
            f_nQueryType  = nQueryType;
            f_parts = parts;
            f_lIdxVersion = lIdxVersion;
            }

        @Override
        protected QueryResult compute()
            {
            if (f_parts.cardinality() == 1)
                {
                return f_storage.queryInternal(f_filter, f_nQueryType == QUERY_INVOKE ? QUERY_KEYS : f_nQueryType, f_parts, f_lIdxVersion);
                }
            else
                {
                PartitionSet partMask = f_parts;

                PartitionedQueryTask[] aTasks = new PartitionedQueryTask[partMask.cardinality()];
                int nPos = 0;
                for (int nPart : partMask)
                    {
                    PartitionSet part = new PartitionSet(partMask.getPartitionCount(), nPart);

                    // query each partition to find the matching entries
                    //
                    // for QUERY_INVOKE, let's get the candidate keys first without the need
                    // for InvocationContext (so we can do it in parallel), and then we'll
                    // post-process them on this thread, using current InvocationContext
                    aTasks[nPos++] = new PartitionedQueryTask(f_storage,
                                                              f_filter,
                                                              f_nQueryType == QUERY_INVOKE ? QUERY_KEYS : f_nQueryType,
                                                              part,
                                                              f_lIdxVersion);
                    }

                invokeAll(aTasks);

                QueryResult[] aPartResults = new QueryResult[aTasks.length];
                for (int i = 0; i < aTasks.length; i++)
                    {
                    QueryResult result = aTasks[i].join();
                    aPartResults[i] = result;
                    }

                return new QueryResult(aPartResults);
                }
            }

        // ---- data members ------------------------------------------------

        private final Storage f_storage;
        private final Filter f_filter;
        private final int f_nQueryType;
        private final PartitionSet f_parts;
        private final long f_lIdxVersion;
        }

    /**
     * Called on the service or a daemon pool thread.
     *
     * @param nQueryType  one of the QUERY_* values
     * @param partMask    partitionSet that all keys should belong to
     */
    public com.tangosol.internal.util.QueryResult query(com.tangosol.util.Filter filter, int nQueryType, com.tangosol.net.partition.PartitionSet partMask, long cTimeoutMillis)
        {
        Span span = TracingHelper.getActiveSpan();
        if (!TracingHelper.isNoop(span) && filter != null)
            {
            span.setMetadata("filter", filter.toString());
            }

        // common case optimization
        if (AlwaysFilter.INSTANCE.equals(filter))
            {
            filter = null;
            }

        long   ldtStart    = Base.getSafeTimeMillis();
        long   lIdxVersion = nQueryType == QUERY_KEYS ? -1 : getVersion().getCommittedVersion();
        int    cTotal      = calculateSize(partMask, false); // total number of entries (before filtering); used for stats

        QueryResult result;

        if (partMask.cardinality() == 1 || filter instanceof LimitFilter)
            {
            // the request is for a single partition or a LimitFilter; run it directly

            result = queryInternal(filter, nQueryType == QUERY_INVOKE ? QUERY_KEYS : nQueryType, partMask, lIdxVersion);
            }
        else if (!Daemons.isForkJoinPoolEnabled() || filter instanceof ScriptFilter)
            {
            // We have to use single-threaded execution if:
            //
            // 1. Coherence FJP is disabled
            // 2. We are evaluating ScriptFilter using GraalVM integration, as GraalVM doesn't support access to script context from multiple threads

            QueryResult[] aResult = new QueryResult[partMask.cardinality()];
            int           nPos    = 0;
            for (int nPart : partMask)
                {
                aResult[nPos++] = queryInternal(filter, nQueryType == QUERY_INVOKE ? QUERY_KEYS : nQueryType, new PartitionSet(partMask.getPartitionCount(), nPart), lIdxVersion);
                }

            result = new QueryResult(aResult);
            }
        else
            {
            Future<QueryResult> future = Daemons.forkJoinPool().submit(new PartitionedQueryTask(this, filter, nQueryType, partMask, lIdxVersion));
            try
                {
                result = cTimeoutMillis == 0L
                         ? future.get()
                         : future.get(cTimeoutMillis, TimeUnit.MILLISECONDS);
                }
            catch (TimeoutException e)
                {
                future.cancel(true);
                throw new RequestTimeoutException("Query request has timed out");
                }
            catch (InterruptedException e)
                {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new RequestTimeoutException("Query request has been interrupted");
                }
            catch (Exception e)
                {
                throw new RuntimeException(e);
                }
            }

        if (nQueryType == QUERY_INVOKE)
            {
            Object[] aoResult = result.getResults();

            // we only have the keys from all the partitions so far;
            // need to sort them, lock them, and reevaluate the filter before
            // replacing them with EntryStatus instances and returning to the caller

            // sort the keys prior to locking to avoid deadlock
            Arrays.sort(aoResult, SafeComparator.INSTANCE);

            int cResults = createQueryResult(filter, aoResult, nQueryType, partMask, lIdxVersion);
            result = new QueryResult(partMask, aoResult, cResults);
            }

        updateQueryStatistics(filter, result.isOptimized(), ldtStart, cTotal,
                              result.getScannedCount(), result.getCount(), nQueryType, partMask);

        return result;
        }

    protected com.tangosol.internal.util.QueryResult queryInternal(com.tangosol.util.Filter filter, int nQueryType, com.tangosol.net.partition.PartitionSet partMask, long lIdxVersion)
        {
        // import com.tangosol.internal.util.QueryResult;
        // import com.tangosol.internal.tracing.Span;
        // import com.tangosol.internal.tracing.TracingHelper;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.comparator.SafeComparator;
        // import com.tangosol.util.filter.LimitFilter;
        // import java.util.Arrays;

        Filter filterOrig = filter;

        QueryResult result = applyIndex(filter, partMask);

        Object[] aoResult = result.getResults(); // starts as keys; could be reused for entries/statuses

        if (aoResult == null)
            {
            aoResult = collectKeys(partMask);
            result.setResults(aoResult);
            }

        filter = result.getFilterRemaining();

        if (filterOrig instanceof LimitFilter)
            {
            // LimitFilter: sort always to prevent discrepancies on partitioned index
            Arrays.sort(aoResult, SafeComparator.INSTANCE);
            }

        int cResults = filter == null
                       ? createQueryResult(filterOrig, aoResult, nQueryType, partMask, lIdxVersion)
                       : createQueryResult(filter, filterOrig, aoResult, nQueryType, partMask, lIdxVersion);

        // Note: QUERY_INVOKE and QUERY_AGGREGATE are never used with LimitFilter
        if (filterOrig instanceof LimitFilter)
            {
            if (cResults < aoResult.length)
                {
                Object[] ao = new Object[cResults];
                System.arraycopy(aoResult, 0, ao, 0, cResults);
                aoResult = ao;
                }

            LimitFilter filterLimit = (LimitFilter) filterOrig;
            int         cAvailable  = aoResult.length;

            filterLimit.setCookie(Integer.valueOf(cAvailable));

            aoResult = nQueryType == QUERY_KEYS || filterLimit.getComparator() == null
                       ? filterLimit.extractPage(aoResult)
                       : extractBinaryEntries(aoResult, filterLimit);
            result.setResults(aoResult);
            }
        else
            {
            result.setResults(aoResult, cResults);
            }

        return result;
        }

    /**
     * An index query returned some keys, which is suspect to have values
     * that where during the population phase of the query. These values
     * need to be reevaluated to verify that they are still valid.
     * <p>
     * When a consistent snapshot has been established any potential
     * sub-filter that isn't using indexes will evaluate the remaining
     * values.
     *
     * @param filterOrig         the IndexAwareFilter used by the #query
     * @param mapEval           a Map<Binary, $BinaryEntry> containg only
     * the keys that needs re-evaluating
     * @param nQueryType   one of the QUERY_* values, except for QUERY_KEYS
     * since this type does not need re-evaluation
     * @param partMask         partitionSet that keys belong to
     */
    public void reevaluateQueryResults(com.tangosol.util.Filter filterOrig, java.util.Map mapEval, int nQueryType, com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.net.internal.StorageVersion as com.tangosol.net.internal.StorageVersion;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.InvocableMapHelper;
        // import com.tangosol.util.SubSet;
        // import com.tangosol.util.filter.IndexAwareFilter;
        // import java.util.ConcurrentModificationException;
        // import java.util.Map;
        // import java.util.Iterator;
        // import java.util.Set;
        // import java.util.HashSet;

        PartitionedCache service     = (PartitionedCache) get_Module();
        Map     mapPrime    = getBackingMapInternal();
        com.tangosol.net.internal.StorageVersion version     = getVersion();
        Filter  filter      = null;
        int     cRetry      = 0;
        int     cRetryMax   = getQueryRetries();
        long    lIdxVerPrev = -1L;
        boolean fInvoke     = nQueryType == QUERY_INVOKE;
        Set     setKeysEval = mapEval.keySet();
        SubSet  setKeys     = new SubSet(setKeysEval);

        while (!setKeys.isEmpty())
            {
            long lIdxVersion = version.getCommittedVersion();
            if (lIdxVersion == lIdxVerPrev)
                {
                if (++cRetry >= cRetryMax)
                    {
                    break;
                    }

                // COH-5256: since this thread may have caused evictions or [read-through] inserts,
                // we need to flush those events to update potential indexes
                service.processChanges();

                version.waitForPendingCommit(partMask.first());
                continue;
                }

            getStatsQueryContentionCount().incrementAndGet();

            try
                {
                for (int nPart : partMask)
                    {
                    filter = filterOrig instanceof IndexAwareFilter
                             ? ((IndexAwareFilter) filterOrig).applyIndex(getIndexMap(nPart), setKeys)
                             : filterOrig;
                    }
                }
            catch (ConcurrentModificationException e)
                {
                // while the set of keys is local and cannot be concurrently changed,
                // iterations over the index data structures could still produce a CME
                continue;
                }
            catch (Throwable e)
                {
                _trace("Exception occurred during query processing: " + getStackTrace(e), 1);
                rethrow(e);
                }

            // Note: the SubSet becomes unusable after its base changes,
            // so we need to realize the removed and retained sets before modifying the base
            Set setRemoved  = setKeys.getRemoved();
            Set setRetained = setKeys.getRetained();

            // remove entries that no longer match from mapEval
            mapEval.keySet().removeAll(setRemoved);

            // unfortunately we need to re-load the entries as we have no way
            // to know whether or not the values have actually changed
            for (Iterator iter = setRetained.iterator(); iter.hasNext(); )
                {
                Binary       binKey = (Binary) iter.next();
                BinaryEntry entry;

                if (fInvoke)
                    {
                    // the entry should already be (locked) in the resource coordinator
                    // we just need to include the EntryStatus to the mapEval again
                    // since it matches the index.
                    EntryStatus status = service.getResourceCoordinator().getStatus(this, binKey);

                    entry = status.getBinaryEntry();
                    mapEval.put(binKey, status);

                    // the entry may be stale based on the statement order prior to
                    // checkIndexConsistency, i.e. {get, lock} || {lock, get}
                    if (entry.isValueStale())
                        {
                        entry.reset(binKey);
                        entry.ensureWriteable();
                        }
                    }
                else
                    {
                    entry = (BinaryEntry) mapEval.get(binKey);
                    if (entry == null)
                        {
                        mapEval.put(binKey, entry = instantiateBinaryEntry(binKey, null, true));
                        }
                    else
                        {
                        entry.reset(binKey); // read-only is preserved
                        }
                    }

                Binary binVal = entry.getBinaryValue();
                if (binVal == null)
                    {
                    // expired or removed
                    iter.remove();
                    mapEval.remove(binKey);
                    }
                }

            if (lIdxVersion == checkBackingMapVersion(mapPrime, partMask))
                {
                // no changes this time - the mapEval contains correct entries
                setKeys.clear();
                break;
                }

            // some changes occur during the index application and value loading;
            // need to re-evaluate again
            Set setStable = new HashSet();
            Map mapStatus = getEntryStatusMap();

            // reduce the partition set to suspected partitions only
            partMask = version.getModifiedPartitions(lIdxVersion, partMask);

            if (partMask.isEmpty())
                {
                // there are no updates to the relevant partitions;
                // mapEval contains correct entries
                setKeys.clear();
                break;
                }

            // make sure we don't modify the original set
            if (setRetained == setKeysEval)
                {
                setRetained = new HashSet(setRetained);
                }

            for (Iterator iter = setRetained.iterator(); iter.hasNext(); )
                {
                Binary binKey = (Binary) iter.next();

                if (!partMask.contains(service.getKeyPartition(binKey)))
                    {
                    // this key belongs to a "stable" partitions the entry is already
                    // loaded into mapEval; no need to re-eval again
                    iter.remove();
                    }
                else if (!mapStatus.containsKey(binKey))
                    {
                    // keys are added to mapStatus before the BMVersion changes
                    // and removed after IxVersion changes;
                    // assuming the index version remains the same
                    // the fact that this key is not in the mapStatus means
                    // that the loaded value corresponds the index
                    setStable.add(binKey);
                    }
                }

            if (lIdxVersion == version.getCommittedVersion())
                {
                setRetained.removeAll(setStable);
                }

            setKeys     = new SubSet(setRetained);
            lIdxVerPrev = lIdxVersion;

            service.checkInterrupt();
            }

        if (!setKeys.isEmpty())
            {
            // after the maximum number of retries the index appears to be stuck;
            // it could be caused by an OOB update (e.g refresh-ahead or write-behind)
            // or even by this thread's actions (e.g. expiry)
            // the index will catch up later during processChanges, but our last resort
            // now is to re-evaluate the remaining entries one by one
            _trace("Performing partial entry scan (" + setKeys.size() + " out of " +
                   mapEval.size() + ") due to concurrent updates during query execution", 3);

            for (Iterator iter = setKeys.iterator(); iter.hasNext(); )
                {
                Binary binKey = (Binary) iter.next();

                BinaryEntry binEntry = fInvoke
                                                                ? ((EntryStatus) mapEval.get(binKey)).getBinaryEntry()
                                                                : (BinaryEntry)  mapEval.get(binKey);
                binEntry.setForceExtract(true);
                try
                    {

                    if (!InvocableMapHelper.evaluateEntry(filterOrig, binEntry))
                        {
                        mapEval.remove(binKey);
                        }
                    }
                catch (RuntimeException e)
                    {
                    // the reason we need to catch the exception here is that some customers
                    // intentionally fail value deserialization to detect non-indexed queries;
                    // that is also why we perform this scan ONLY as a last resort
                    _trace("Skipping potential entry match for " + binKey + "\n"
                           + getStackTrace(e), 1);
                    mapEval.remove(binKey);
                    }
                binEntry.setForceExtract(false);
                }
            }

        if (filter != null)
            {
            // not all filters could be applied against indexes; each entry needs
            // to be verified against the remaining filter(s)
            for (Iterator iter = mapEval.values().iterator(); iter.hasNext(); )
                {
                BinaryEntry entry = fInvoke
                                                             ? ((EntryStatus) iter.next()).getBinaryEntry()
                                                             : (BinaryEntry)  iter.next();
                if (!InvocableMapHelper.evaluateEntry(filter, entry))
                    {
                    iter.remove();
                    }
                }
            }
        }

    /**
     * Release all the backup data in specified partition. Called on the
     * service thread only.
     *
     * @param iPartition  the partition number to release
     */
    protected void releaseBackupResources(int iPartition)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.Map;

        PartitionedCache service    = getService();
        com.tangosol.net.partition.PartitionAwareBackingMap    pabmBackup = getPartitionAwareBackupMap();

        if (pabmBackup != null)
            {
            // common case - drop an entire partition; event emission (or not) is down
            // to the backing map impl however as this is for backups we are good

            pabmBackup.destroyPartition(iPartition);
            }
        else
            {
            for (Iterator iter = getBackupMap().keySet().iterator(); iter.hasNext();)
                {
                Binary binKey = (Binary) iter.next();

                if (service.getKeyPartition(binKey) == iPartition)
                    {
                    iter.remove();
                    }
                }
            }
        }

    /**
     * Move the content of key based maps between the maps for all keys in
     * the specified partition.
     *
     * @param iPartition  the partition number
     */
    protected void releaseData(int iPartition, java.util.Map map, String sDescr)
        {
        // import com.tangosol.util.Binary;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;

        PartitionedCache service = getService();

        // instead of synchronizing on the map and blocking all
        // the "put" and "remove" operations, we just catch any
        // ConcurrentModificationException and try again
        while (true)
            {
            try
                {
                for (Iterator iter = map.keySet().iterator(); iter.hasNext();)
                    {
                    Binary binKey = (Binary) iter.next();

                    if (service.getKeyPartition(binKey) == iPartition)
                        {
                        iter.remove();
                        }
                    }
                break;
                }
            catch (ConcurrentModificationException e)
                {
                _trace("Failed to release " + sDescr + ": " + e + "; trying again", 2);
                }
            }
        }

    /**
     * Release all the data in specified partition. Called on the service
     * thread only.
     *
     * @param iPartition  the partition number to release
     * @param fPrimary  if true the primary content should be removed;
     * otherwise the backup storage
     *
     * @see #preparePartition, movePartition
     */
    public void releasePartition(int iPartition, boolean fPrimary)
        {
        // import java.util.Map;

        // important:
        // - remove listeners BEFORE the data get removed

        // listeners
        {
        Map map = fPrimary ? getKeyListenerMap() : getBackupKeyListenerMap();
        if (map != null && !map.isEmpty())
            {
            releaseData(iPartition, map, "listeners");
            }
        }

        // locks
        {
        Map map = fPrimary ? getLeaseMap() : getBackupLeaseMap();

        releaseData(iPartition, map, "locks");
        if (fPrimary)
            {
            firePendingLocks(iPartition);
            }
        }

        if (fPrimary)
            {
            releasePrimaryResources(iPartition);
            }
        else
            {
            releaseBackupResources(iPartition);
            }
        }

    /**
     * Release all the data in specified partition. Called on the service
     * thread only.
     *
     * @param iPartition  the partition number to release
     */
    protected void releasePrimaryResources(int iPartition)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.util.ObservableMap;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Set;

        int  cRemoves;
        Map  mapPrime  = getBackingMapInternal();
        com.tangosol.net.partition.PartitionAwareBackingMap pabmPrime = getPartitionAwareBackingMap();
        Map  mapPart   = pabmPrime == null ? null : pabmPrime.getPartitionMap(iPartition);

        if (mapPart instanceof ObservableMap)
            {
            cRemoves = mapPart.size();
            mapPart.clear();
            }
        else
            {
            Set setKeys = collectKeySet(iPartition);

            // unroll AbstractSet.removeAll() to avoid unproductive "optimization"
            for (Iterator iter = setKeys.iterator(); iter.hasNext(); )
                {
                mapPrime.remove(iter.next());
                }

            cRemoves = setKeys.size();
            }

        // handle any BM events that may be raised, expected or otherwise
        // (e.g. update user and key indices).
        //
        // update the indices to reflect the removal, but we should not publish
        // "changes" (events or backup messages), as this is not a logical change
        getService().processChanges();

        // decrement the inserts count, as that stat transfers with the partition
        getStatsInserts().addAndGet(-cRemoves);

        // cleanup the com.tangosol.net.partition.PartitionAwareBackingMap, or the primary key index
        if (pabmPrime == null)
            {
            com.tangosol.net.partition.PartitionAwareBackingMap mapKeyIndex = getPartitionedKeyIndex();
            if (mapKeyIndex != null)
                {
                mapKeyIndex.destroyPartition(iPartition);
                }
            }
        else
            {
            pabmPrime.destroyPartition(iPartition);
            }
        }

    /**
     * Called on the service or a daemon pool thread after acquiring the key
     * lock.
     */
    public void remove(PartitionedCache.InvocationContext ctxInvoke, EntryStatus status, boolean fBlind)
        {
        // import java.util.Set;

        status.getBinaryEntry().remove(false); // pseudo "invoke"

        Set setEntriesAdded = ctxInvoke.processInterceptors();
        postRemove(status.getBinaryEntry(), status, fBlind);
        ctxInvoke.postInvokeAll(setEntriesAdded);
        }

    /**
     * Called on the service or a daemon pool thread after acquiring the key
     * lock.
     *
     * @return true if  the map got changed
     */
    public boolean removeAll(PartitionedCache.InvocationContext ctxInvoke, EntryStatus[] aStatus)
        {
        // import java.util.Set;

        boolean fRemoved = false;
        int     cSize    = aStatus.length;
        for (int i = 0; i < cSize; i++)
            {
            EntryStatus status = aStatus[i];

            status.getBinaryEntry().remove(false); // pseudo "invoke"
            }

        Set setEntriesAdded = ctxInvoke.processInterceptors();

        fRemoved = postRemoveAll(aStatus);
        ctxInvoke.postInvokeAll(setEntriesAdded);

        return fRemoved;
        }

    /**
     * Helper for a "removeAll" operation from the primary storage.
     */
    protected void removeAllPrimaryResource(java.util.Set setKeys)
        {
        // import com.tangosol.internal.util.HeuristicCommitException;

        if (!setKeys.isEmpty())
            {
            try
                {
                getBackingMapInternal().keySet().removeAll(setKeys);
                }
            catch (Throwable e)
                {
                throw new HeuristicCommitException(e);
                }
            }
        }

    /**
     * Called on the service thread only.
     */
    public void removeIndex(com.tangosol.coherence.component.net.RequestContext context, com.tangosol.net.partition.PartitionSet partsMask, com.tangosol.util.ValueExtractor extractor, java.util.Comparator comparator)
        {
        // import com.tangosol.net.security.StorageAccessAuthorizer as com.tangosol.net.security.StorageAccessAuthorizer;
        // import com.tangosol.util.MapIndex;
        // import com.tangosol.util.extractor.IdentityExtractor;
        // import com.tangosol.util.extractor.IndexAwareExtractor;

        if (checkIndexExists(extractor, comparator, false))
            {
            if (context != null)
                {
                checkAccess(context, BinaryEntry.ACCESS_WRITE_ANY, com.tangosol.net.security.StorageAccessAuthorizer.REASON_INDEX_REMOVE);
                }

            // remove from each partition index
            for (int iPart = partsMask.next(0); iPart >= 0; iPart = partsMask.next(iPart + 1))
                {
                Map      mapIndex = getPartitionIndexMap(iPart);
                MapIndex index    = extractor instanceof IndexAwareExtractor ?
                                    ((IndexAwareExtractor) extractor).destroyIndex(mapIndex) :
                                    (MapIndex) mapIndex.remove(extractor);
                }

            getIndexExtractorMap().remove(extractor);

            persistIndexRegistration(partsMask, extractor, comparator, false);
            }
        }

    /**
     * Called on the service or a daemon pool thread after acquiring the key
     * lock.
     */
    public void removeKeyListener(com.tangosol.coherence.component.net.Member member, com.tangosol.util.Binary binKey, boolean fPrimary)
        {
        removeListenerProxy(fPrimary ? getKeyListenerMap() : getBackupKeyListenerMap(),
                            binKey, member);
        }

    /**
     * Called on the service thread only.
     */
    public void removeListener(com.tangosol.coherence.component.net.Member member, com.tangosol.util.Filter filter, long lFilterId)
        {
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Set;

        // ListenerMap (ConcurrentHashMap) doesn't support null keys
        Object oFilter = filter == null ? (Object) Binary.EMPTY : filter;

        removeListenerProxy(getListenerMap(), oFilter, member);

        Map mapFilterId = getFilterIdMap();
        if (mapFilterId != null)
            {
            Map mapMemberFilterId = (Map) mapFilterId.get(member);
            if (mapMemberFilterId != null)
                {
                Set setFilterId = (Set) mapMemberFilterId.get(filter);
                if (setFilterId != null)
                    {
                    setFilterId.remove(Long.valueOf(lFilterId));
                    if (setFilterId.isEmpty())
                        {
                        mapMemberFilterId.remove(filter);
                        if (mapMemberFilterId.isEmpty())
                            {
                            mapFilterId.remove(member);
                            }
                        }
                    }
                }
            }
        }

    /**
     * Called on the service or a daemon pool thread.
     */
    protected void removeListenerProxy(java.util.Map map, Object anyKey, com.tangosol.coherence.component.net.Member member)
        {
        // import java.util.Map;

        if (map != null)
            {
            Map mapMembers = (Map) map.get(anyKey);
            if (mapMembers != null)
                {
                mapMembers.remove(member);
                if (mapMembers.isEmpty())
                    {
                    map.remove(anyKey);
                    }
                }
            }
        }

    /**
     * Helper for a "remove" operation from the primary storage.
     */
    protected com.tangosol.util.Binary removePrimaryResource(com.tangosol.util.Binary binKey, boolean fBlind, boolean fSynthetic)
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.cache.ConfigurableCacheMap$Entry as com.tangosol.net.cache.ConfigurableCacheMap.Entry;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import java.util.Map;

        Binary binValReturn = null;
        Map    map          = getBackingMapInternal();

        if (fSynthetic)
            {
            // COH-9787 - call CCM.evict to generate synthetic events if the
            //            backing map is a CCM (or an RWBM with a CCM internal)
            ConfigurableCacheMap mapCCM = getBackingConfigurableCache();
            if (mapCCM != null)
                {
                if (!fBlind)
                    {
                    binValReturn = (Binary) mapCCM.get(binKey);
                    }

                mapCCM.evict(binKey);

                // evict could fail, e.g, disapproved during partition transfer
                com.tangosol.net.cache.ConfigurableCacheMap.Entry entry = mapCCM.getCacheEntry(binKey);
                if (entry != null)
                    {
                    entry.setExpiryMillis(Base.getSafeTimeMillis());
                    }

                return binValReturn;
                }

            // if the put is "synthetic", operate directly on the RWBM internal map
            map = getBackingInternalCache();
            }

        if (fBlind)
            {
            map.keySet().remove(binKey);
            }
        else
            {
            binValReturn = (Binary) map.remove(binKey);
            }

        return binValReturn;
        }

    /**
     * Safely remove the resource value associated with the specified key.
     * Should be called ONLY when the gate for the corresponding partition
     * is "closed".
     */
    protected com.tangosol.util.Binary removeSafe(com.tangosol.util.Binary binKey)
        {
        // import com.tangosol.util.Binary;

        // Protected the calling (service) thread from custom implementations
        try
            {
            return (Binary) getBackingMapInternal().remove(binKey);
            }
        catch (Throwable e)
            {
            _trace("Failed remove during transfer: key=" + binKey + " " + e , 1);
            return null;
            }
        }

    /**
     * Called on the service thread only.
     */
    public void removeTrigger(com.tangosol.net.partition.PartitionSet partsMask, com.tangosol.util.MapTrigger trigger)
        {
        // import java.util.Set;

        Set setTriggers = getTriggerSet();
        if (setTriggers != null)
            {
            setTriggers.remove(trigger);
            if (setTriggers.isEmpty())
                {
                setTriggerSet(null);
                }
            }
        }

    /**
     * For debugging only. Used by PrimaryListener
     */
    public String reportIndexes()
        {
        // import com.tangosol.util.Base;
        // import java.util.Map;

        Map mapIndex = getIndexMap();
        return mapIndex.isEmpty()
               ? "No indexes"
               : Base.toDelimitedString(mapIndex.values().iterator(), "\n");
        }

    /**
     * For debugging only.
     */
    public String reportKeys(java.util.Set setKeys)
        {
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections as com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.NullImplementation;

        Converter conv = getService().getBackingMapContext().getKeyFromInternalConverter();

        return String.valueOf(com.tangosol.util.ConverterCollections.getSet(setKeys,
                                                                            conv, /* NotSupported */ NullImplementation.getConverter()));
        }

    /**
     * Used by insertPrimaryTransfer(), insertBackupTransfer() and
     * movePartition() methods for logging purpose.
     *
     * @param e  RuntimeException  exception that needs to be logged
     * @param sTransferType  String  mentions if the failure is related to
     * primary transfer or backup transfer
     */
    protected void reportTransferFailure(RuntimeException e, String sTransferType)
        {
        if (e instanceof UnsupportedOperationException)
            {
            if (sTransferType.equals("primary") && !isMisconfigLoggedPrimary())
                {
                _trace("Unable to complete primary transfer into cache: " +
                       getCacheName() + " due to misconfigured cache", 1);
                setMisconfigLoggedPrimary(true);
                }
            if (sTransferType.equals("backup") && !isMisconfigLoggedBackup())
                {
                _trace("Unable to complete backup transfer into cache: " +
                       getCacheName() + " due to misconfigured cache", 1);
                setMisconfigLoggedBackup(true);
                }
            }
        else
            {
            _trace("Unable to complete " + sTransferType + " transfer into cache: " +
                   getCacheName() + " due to " + e + "\n" + get_StackTrace(), 1);
            }
        }

    /**
     * Reset statistics.
     */
    public void resetStats()
        {
        // import com.tangosol.net.events.internal.StorageDispatcher;

        setStatsEventsDispatched(0L);
        getStatsListenerRegistrations().set(0L);
        setStatsMaxQueryDescription("");
        setStatsMaxQueryDurationMillis(0L);
        getStatsNonOptimizedQueryCount().set(0);
        getStatsNonOptimizedQueryTotalMillis().set(0L);
        getStatsOptimizedQueryTotalMillis().set(0L);
        getStatsOptimizedQueryCount().set(0);
        getStatsQueryContentionCount().set(0L);
        getStatsClears().set(0L);

        getStatsIndexingTotalMillis().set(0L);

        StorageDispatcher dispatcher = (StorageDispatcher) getEventDispatcher();
        if (dispatcher != null)
            {
            dispatcher.getStats().reset();
            }
        }

    /**
     * Retrow a Throwable that must be an Error or RuntimeException.
     */
    protected static void rethrow(Throwable e)
        {
        if (e instanceof Error)
            {
            throw (Error) e;
            }
        else
            {
            throw (RuntimeException) e;
            }
        }

    /**
     * Schedule to run the eviction task at specified expiry delay
     * (cExpiryMillis) if no task is currently scheduled.  Cancel the
     * existing task and create a new one if new expiry time is sooner than
     * previosely scheduled one.
     *
     * @param cExpiryMillis   expiry delay in millis
     */
    public synchronized void scheduleEviction(long cExpiryMillis)
        {
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        // import com.tangosol.util.Base;

        com.tangosol.coherence.component.util.DaemonPool pool = getService().getDaemonPool();

        if (!pool.isStarted())
            {
            return;
            }

        EvictionTask task = getEvictionTask();

        if (task == null)
            {
            task = (EvictionTask) _newChild("EvictionTask");
            task.setEvictionTime(0);
            setEvictionTask(task);
            }

        long ldtOldNext = task.getEvictionTime();
        long ldtNewNext = Base.getSafeTimeMillis() + cExpiryMillis;

        task.setPrune(cExpiryMillis == 0L);

        if (ldtOldNext > 0L)
            {
            if (cExpiryMillis == 0L ||
                (ldtNewNext + EvictionTask.EVICTION_DELAY) < ldtOldNext)
                {
                // either this is an immediate eviction request or
                // the new expiry time is sooner than previosely scheduled one;
                // cancel (by unlinking) the old scheduled task and create a new one
                task.cancel();

                setEvictionTask(task = (EvictionTask) _newChild("EvictionTask"));
                task.setPrune(cExpiryMillis == 0L);
                }
            else
                {
                return;
                }
            }

        task.setEvictionTime(ldtNewNext);

        pool.schedule(task, cExpiryMillis);
        }

    // Accessor for the property "AccessAuthorizer"
    /**
     * Setter for property AccessAuthorizer.<p>
     */
    protected void setAccessAuthorizer(com.tangosol.net.security.StorageAccessAuthorizer authorizer)
        {
        if (getAccessAuthorizer() != null)
            {
            throw new IllegalStateException("Authorizer is not resettable");
            }

        __m_AccessAuthorizer = (authorizer);
        }

    // Accessor for the property "AdjustPartitionSize"
    /**
     * Setter for property AdjustPartitionSize.<p>
     * If true, it is the responsibility of the storage to update the
     * partition size as data change. Otherwise BM is PABM and we can get
     * the partition size directly.
     */
    public void setAdjustPartitionSize(boolean fSkip)
        {
        __m_AdjustPartitionSize = fSkip;
        }

    // Accessor for the property "BackingConfigurableCache"
    /**
     * Setter for property BackingConfigurableCache.<p>
     * A ConfigurableCacheMap. It refers to the same reference as the
     * backing map if the backing map is a ConfigurableCacheMap. If the
     * backing map is a ReadWriteBackingMap, it refers to the backing map's
     * internal cache. Otherwise, it is null.
     */
    protected void setBackingConfigurableCache(com.tangosol.net.cache.ConfigurableCacheMap mapCCM)
        {
        __m_BackingConfigurableCache = mapCCM;
        }

    // Accessor for the property "BackingInternalCache"
    /**
     * Setter for property BackingInternalCache.<p>
     * If the backing map is a ReadWriteBackingMap, it refers to the backing
     * map's internal cache. It allows us to avoid expired entries from
     * causing a CacheStore.load() on read as well as store() and eraase()
     * on synthetic update and remove.
     *
     * If the backing map is not RWBM, this reference is the same as the
     * BackingMap.
     *
     * @see COH-8468
     */
    protected void setBackingInternalCache(com.tangosol.util.ObservableMap mapCache)
        {
        __m_BackingInternalCache = mapCache;
        }

    // Accessor for the property "BackingMapAction"
    /**
     * Setter for property BackingMapAction.<p>
     * PrivilegedAction to call getBackingMap().
     */
    protected void setBackingMapAction(java.security.PrivilegedAction action)
        {
        __m_BackingMapAction = action;
        }

    // Accessor for the property "BackingMapExpiryDelay"
    /**
     * Setter for property BackingMapExpiryDelay.<p>
     * The default expiry in ms of the configured backing-map if expiry is
     * supported, or CacheMap.EXPIRY_NEVER (-1L) otherwise.
     */
    protected void setBackingMapExpiryDelay(int cMillis)
        {
        __m_BackingMapExpiryDelay = cMillis;
        }

    // Accessor for the property "BackingMapInternal"
    /**
     * Setter for property BackingMapInternal.<p>
     * The [primary] map of resources maintained by this storage with keys
     * and values being Binary objects.
     */
    protected void setBackingMapInternal(com.tangosol.util.ObservableMap map)
        {
        __m_BackingMapInternal = map;
        }

    // Accessor for the property "BackupKeyListenerMap"
    /**
     * Setter for property BackupKeyListenerMap.<p>
     * A map of backups for key based listener proxies.
     *
     * @see #KeyListenerMap property
     */
    protected void setBackupKeyListenerMap(java.util.Map map)
        {
        __m_BackupKeyListenerMap = map;
        }

    // Accessor for the property "BackupLeaseMap"
    /**
     * Setter for property BackupLeaseMap.<p>
     * The backup map of leases.
     *
     * @see #LeaseMap property
     */
    protected void setBackupLeaseMap(java.util.Map map)
        {
        __m_BackupLeaseMap = map;
        }

    // Accessor for the property "BackupMap"
    /**
     * Setter for property BackupMap.<p>
     * The map of resource backups maintaned by this storage with keys and
     * values being Binary objects.
     *
     * @see #ResourceMap property
     */
    protected void setBackupMap(java.util.Map map)
        {
        __m_BackupMap = map;
        }

    // Accessor for the property "CacheId"
    /**
     * Setter for property CacheId.<p>
     * Id of the cache this storage represents.
     */
    public void setCacheId(long lCacheId)
        {
        __m_CacheId = lCacheId;
        }

    // Accessor for the property "CacheName"
    /**
     * Called on the Service thread only.
     */
    public void setCacheName(String sName)
        {
        __m_CacheName = sName;
        }

    // Accessor for the property "ConfiguredBackupListeners"
    /**
     * Setter for property ConfiguredBackupListeners.<p>
     * The map of backup map listeners keyed by corresponding backup map
     * references. Used only if the backup map was created by
     * DefaultConfigurableCacheFactory.
     *
     * @see #instantiateBackupMap()
     * @see #ivalidateBackupMap()
     */
    protected void setConfiguredBackupListeners(java.util.Map map)
        {
        __m_ConfiguredBackupListeners = map;
        }

    // Accessor for the property "ConverterKeyDown"
    /**
     * Setter for property ConverterKeyDown.<p>
     * Cached KeyToInternal converter.
     */
    protected void setConverterKeyDown(com.tangosol.util.Converter converter)
        {
        __m_ConverterKeyDown = converter;
        }

    // Accessor for the property "ConverterUp"
    /**
     * Setter for property ConverterUp.<p>
     * Cached ValueFromInternal (same as KeyFromInternal) converter.
     */
    protected void setConverterUp(com.tangosol.util.Converter converter)
        {
        __m_ConverterUp = converter;
        }

    // Accessor for the property "ConverterValueDown"
    /**
     * Setter for property ConverterValueDown.<p>
     * Cached ValueToInternal converter.
     */
    protected void setConverterValueDown(com.tangosol.util.Converter converter)
        {
        __m_ConverterValueDown = converter;
        }

    // Accessor for the property "EntryStatusMap"
    /**
     * Setter for property EntryStatusMap.<p>
     * The map of keys to their associated EntryStatus.
     */
    public void setEntryStatusMap(java.util.concurrent.ConcurrentMap pEntryStatusMap)
        {
        __m_EntryStatusMap = pEntryStatusMap;
        }

    // Accessor for the property "EntryToBinaryEntryConverter"
    /**
     * Setter for property EntryToBinaryEntryConverter.<p>
     * Converter that produces a read-only $BinaryEntry from a "present"
     * Map$Entry.
     */
    protected void setEntryToBinaryEntryConverter(EntryToBinaryEntryConverter converter)
        {
        __m_EntryToBinaryEntryConverter = converter;
        }

    // Accessor for the property "EventDispatcher"
    /**
     * Setter for property EventDispatcher.<p>
     * The BackingMapDispatcher for this Storage, used by EventsHelper.
     *
     * See $EventsHelper#registerStorageDispatcher.
     */
    public void setEventDispatcher(com.tangosol.net.events.internal.StorageDispatcher dispatcher)
        {
        __m_EventDispatcher = dispatcher;
        }

    // Accessor for the property "EventDispatcherInterceptor"
    /**
     * Setter for property EventDispatcherInterceptor.<p>
     * An EventInterceptor that is notified as interceptors are added and
     * removed to the StorageDispatcher.
     *
     * @see PartitionedCache.EventsHelper.registerStorageDispatcher
     */
    protected void setEventDispatcherInterceptor(DispatcherInterceptor listener)
        {
        __m_EventDispatcherInterceptor = listener;
        }

    // Accessor for the property "EvictionTask"
    /**
     * Setter for property EvictionTask.<p>
     * The task that is sheduled to perform backing map expiry based
     * eviction.
     */
    public void setEvictionTask(EvictionTask taskEviction)
        {
        __m_EvictionTask = taskEviction;
        }

    // Accessor for the property "ExpirySliding"
    /**
     * Setter for property ExpirySliding.<p>
     * True iff cache is configured with a non-zero "expiry-delay" and
     * "expiry-sliding" is enabled.
     */
    public void setExpirySliding(boolean fSliding)
        {
        __m_ExpirySliding = fSliding;
        }

    // Accessor for the property "FilterIdMap"
    /**
     * Setter for property FilterIdMap.<p>
     * The map of FIlter ids keyed by the Member objects with values that
     * are maps of (Filter, Sets of Long filter ids) entries.
     */
    protected void setFilterIdMap(java.util.Map map)
        {
        __m_FilterIdMap = map;
        }

    // Accessor for the property "IndexExtractorMap"
    /**
     * Setter for property IndexExtractorMap.<p>
     * The Map<ValueExtractor, Comparator> containing the indexed extractors
     * on this cache.  Each extractor is associated with a Comparable that
     * is used to sort the index, or null for an unsorted index.  In the
     * case of IndexAwareExtractor, the actual extractor used by the cache
     * index may not be the one held by this map.
     *
     * @see onNotifyServiceJoined()
     */
    protected void setIndexExtractorMap(java.util.Map map)
        {
        __m_IndexExtractorMap = map;
        }

    // Accessor for the property "InternBackupKeys"
    /**
     * Setter for property InternBackupKeys.<p>
     * Specifies whether or not to intern Backup Keys.
     */
    protected void setInternBackupKeys(boolean fIntern)
        {
        __m_InternBackupKeys = fIntern;
        }

    // Accessor for the property "InternPrimaryKeys"
    /**
     * Setter for property InternPrimaryKeys.<p>
     * Specifies whether or not to intern Primary Keys.
     */
    protected void setInternPrimaryKeys(boolean fIntern)
        {
        __m_InternPrimaryKeys = fIntern;
        }

    // Accessor for the property "KeyListenerMap"
    /**
     * Setter for property KeyListenerMap.<p>
     * A map of key based listener proxies representing service Members that
     * have requested to be notified with MapEvents regarding this cache.
     * The map is keyed by the "listened to" keys and the values are maps of
     * (Member, Boolean) entries.
     */
    protected void setKeyListenerMap(java.util.Map map)
        {
        __m_KeyListenerMap = map;
        }

    // Accessor for the property "KeyToBinaryEntryConverter"
    /**
     * Setter for property KeyToBinaryEntryConverter.<p>
     * Converter that produces a read-only $BinaryEntry from a  binary key.
     */
    public void setKeyToBinaryEntryConverter(KeyToBinaryEntryConverter converter)
        {
        __m_KeyToBinaryEntryConverter = converter;
        }

    // Accessor for the property "LeaseMap"
    /**
     * Setter for property LeaseMap.<p>
     * The map of leases granted by this storage.
     */
    protected void setLeaseMap(java.util.Map map)
        {
        __m_LeaseMap = map;
        }

    // Accessor for the property "ListenerMap"
    /**
     * Setter for property ListenerMap.<p>
     * A map of filter based listener proxies representing service Members
     * that have requested to be notified with MapEvents regarding this
     * cache. The map is keyed by the Filter objects and the values are maps
     * of (Member, Boolean) entries. Since null is a valid filter and we are
     * using the ConcurrentHashMap, which doesn't support nulls, the null
     * filter will be replaced with the BINARY_EXISTS tag as a key.
     */
    protected void setListenerMap(java.util.Map map)
        {
        __m_ListenerMap = map;
        }

    // Accessor for the property "MisconfigLoggedBackup"
    /**
     * Setter for property MisconfigLoggedBackup.<p>
     * Used by movePartition() / insertBackupTransfer() to limit the number
     * of error messages for a misconfigured cache.
     */
    protected void setMisconfigLoggedBackup(boolean fLogged)
        {
        __m_MisconfigLoggedBackup = fLogged;
        }

    // Accessor for the property "MisconfigLoggedPrimary"
    /**
     * Setter for property MisconfigLoggedPrimary.<p>
     * Used by movePartition() / insertPrimaryTransfer() to limit the number
     * of error messages for a misconfigured cache.
     */
    protected void setMisconfigLoggedPrimary(boolean fLogged)
        {
        __m_MisconfigLoggedPrimary = fLogged;
        }

    // Accessor for the property "OldValueRequired"
    /**
     * Setter for property OldValueRequired.<p>
     * Specifies whether or not the old value is likely to be accessed
     * either during or post request processing.
     *
     * @volatile
     */
    protected void setOldValueRequired(boolean fRequired)
        {
        __m_OldValueRequired = fRequired;
        }

    // Accessor for the property "PartitionAwareBackingMap"
    /**
     * Setter for property PartitionAwareBackingMap.<p>
     * Returns the backing map as a PartitionAwareBackingMap if the backing
     * map is partition-aware; null otherwise.
     */
    protected void setPartitionAwareBackingMap(com.tangosol.net.partition.PartitionAwareBackingMap mapPrime)
        {
        __m_PartitionAwareBackingMap = mapPrime;
        }

    // Accessor for the property "PartitionAwareBackupMap"
    /**
     * Setter for property PartitionAwareBackupMap.<p>
     * Returns the backup map as a PartitionAwareBackingMap if the backup
     * map is partition-aware; null otherwise.
     */
    protected void setPartitionAwareBackupMap(com.tangosol.net.partition.PartitionAwareBackingMap mapBackup)
        {
        __m_PartitionAwareBackupMap = mapBackup;
        }

    // Accessor for the property "PartitionedIndexMap"
    /**
     * Setter for property PartitionedIndexMap.<p>
     * The map of partition indexes maintained by this storage. The keys of
     * the Map are partition IDs, and for each key, the corresponding value
     * stored in the Map is a map of indices for that partition, with
     * ValueExtarctor objects as keys and MapIndex objects as values.
     *
     * @see com.tangosol.util.ValueExtractor
     * @see com.tangosol.util.MapIndex
     *
     * @volatile
     */
    protected void setPartitionedIndexMap(java.util.Map pIndexMap)
        {
        __m_PartitionedIndexMap = pIndexMap;
        }

    // Accessor for the property "PartitionedKeyIndex"
    /**
     * Setter for property PartitionedKeyIndex.<p>
     * PartitionAwareBackingMap used as a key partition index. Used iff the
     * ResourceMap itself is not partition aware.
     */
    protected void setPartitionedKeyIndex(com.tangosol.net.partition.PartitionAwareBackingMap map)
        {
        __m_PartitionedKeyIndex = map;
        }

    // Accessor for the property "PendingLockRequest"
    /**
     * Setter for property PendingLockRequest.<p>
     * The queue of pending LockRequest messages.
     */
    protected void setPendingLockRequest(java.util.List list)
        {
        __m_PendingLockRequest = list;
        }

    // Accessor for the property "Persistent"
    /**
     * Setter for property Persistent.<p>
     * True iff the contents of this Storage should be persisted.
     */
    protected void setPersistent(boolean fPersistent)
        {
        __m_Persistent = fPersistent;
        }

    // Accessor for the property "PotentiallyEvicting"
    /**
     * Setter for property PotentiallyEvicting.<p>
     * Specifies whether or not the backing map is potentially evicting.
     */
    private void setPotentiallyEvicting(boolean fPotentiallyEvicting)
        {
        __m_PotentiallyEvicting = fPotentiallyEvicting;
        }

    // Accessor for the property "PreferPutAllBackup"
    /**
     * Setter for property PreferPutAllBackup.<p>
     * Specifies whether or not the backup backing map prefers putAll to
     * regular put operations.
     */
    protected void setPreferPutAllBackup(boolean fPrefer)
        {
        __m_PreferPutAllBackup = fPrefer;
        }

    // Accessor for the property "PreferPutAllPrimary"
    /**
     * Setter for property PreferPutAllPrimary.<p>
     * Specifies whether or not the primary backing map prefers putAll to
     * regular put operations.
     */
    protected void setPreferPutAllPrimary(boolean fPrefer)
        {
        __m_PreferPutAllPrimary = fPrefer;
        }

    // Accessor for the property "PrimaryListener"
    /**
     * Setter for property PrimaryListener.<p>
     * Primary storage listener. Used only if a custom backing map manager
     * uses an ObservableMap to implement the [primary] local storage.
     */
    protected void setPrimaryListener(com.tangosol.util.MapListener listener)
        {
        __m_PrimaryListener = listener;
        }

    // Accessor for the property "QueryRetries"
    /**
     * Setter for property QueryRetries.<p>
     * Controlls the maximum number of query index retries before falling
     * back on entry by entry evaluation.
     *
     * The undocumented system  property used to set this value is
     * 'tangosol.coherence.query.retry', defaults to Integer.MAX_VALUE.
     */
    public void setQueryRetries(int nRetries)
        {
        __m_QueryRetries = nRetries;
        }

    // Accessor for the property "QuerySizeCache"
    /**
     * Setter for property QuerySizeCache.<p>
     * This cache holds temporary statistics for filter-based requests. The
     * value is a total size in bytes for matching values contained within a
     * single randomly choosen partition.
     */
    protected void setQuerySizeCache(java.util.Map map)
        {
        __m_QuerySizeCache = map;
        }

    // Accessor for the property "ResourceControlMap"
    /**
     * Setter for property ResourceControlMap.<p>
     * Used to control access to keys.
     */
    public void setResourceControlMap(com.tangosol.util.ConcurrentMap mapResourceControl)
        {
        __m_ResourceControlMap = mapResourceControl;
        }

    // Accessor for the property "StatsEventsDispatched"
    /**
     * Setter for property StatsEventsDispatched.<p>
     * The total number of MapEvents dispatched by this Storage.
     */
    protected void setStatsEventsDispatched(long cEvents)
        {
        __m_StatsEventsDispatched = cEvents;
        }

    // Accessor for the property "StatsEvictions"
    /**
     * Setter for property StatsEvictions.<p>
     * A counter for the number of evictions from the backing map.
     */
    protected void setStatsEvictions(java.util.concurrent.atomic.AtomicLong counter)
        {
        __m_StatsEvictions = counter;
        }

    // Accessor for the property "StatsIndexingTotalMillis"
    /**
     * Setter for property StatsIndexingTotalMillis.<p>
     * Total amount of time it took to build indices since statistics were
     * last reset.
     */
    public void setStatsIndexingTotalMillis(java.util.concurrent.atomic.AtomicLong atomicMillis)
        {
        __m_StatsIndexingTotalMillis = atomicMillis;
        }

    // Accessor for the property "StatsInserts"
    /**
     * Setter for property StatsInserts.<p>
     * A counter for the number of inserts into the backing map.
     * This counter gets incremented during direct inserts caused by put or
     * invoke operations; read-ahead synthetic inserts and data distribution
     * transfers "in". It gets decremented during data distribution
     * transfers "out".
     */
    protected void setStatsInserts(java.util.concurrent.atomic.AtomicLong counter)
        {
        __m_StatsInserts = counter;
        }

    // Accessor for the property "StatsListenerRegistrations"
    /**
     * Setter for property StatsListenerRegistrations.<p>
     * The total number of Listener registration requests processed by this
     * Storage.
     */
    protected void setStatsListenerRegistrations(java.util.concurrent.atomic.AtomicLong cRequests)
        {
        __m_StatsListenerRegistrations = cRequests;
        }

    // Accessor for the property "StatsMaxQueryDescription"
    /**
     * Setter for property StatsMaxQueryDescription.<p>
     * A string representation of a query with the longest execution time
     * exceeding the MaxQueryThresholdMillis since statistics were last
     * reset.
     */
    public void setStatsMaxQueryDescription(String sQueryDescription)
        {
        __m_StatsMaxQueryDescription = sQueryDescription;
        }

    // Accessor for the property "StatsMaxQueryDurationMillis"
    /**
     * Setter for property StatsMaxQueryDurationMillis.<p>
     * The number of milliseconds of the longest running query since
     * statistics were last reset.
     */
    public void setStatsMaxQueryDurationMillis(long cMillis)
        {
        __m_StatsMaxQueryDurationMillis = cMillis;
        }

    // Accessor for the property "StatsMaxQueryThresholdMillis"
    /**
     * Setter for property StatsMaxQueryThresholdMillis.<p>
     * A query execution threshold in milliseconds The longest query
     * executing longer than this threshold will be reported in  the
     * MaxQueryDescription attribute.
     */
    public void setStatsMaxQueryThresholdMillis(long cMillis)
        {
        __m_StatsMaxQueryThresholdMillis = cMillis;
        }

    // Accessor for the property "StatsNonOptimizedQueryCount"
    /**
     * Setter for property StatsNonOptimizedQueryCount.<p>
     * Total number of queries that could not be resolved or was partial
     * resolved against indexes since statistics were last reset.
     */
    public void setStatsNonOptimizedQueryCount(java.util.concurrent.atomic.AtomicLong atomicLong)
        {
        __m_StatsNonOptimizedQueryCount = atomicLong;
        }

    // Accessor for the property "StatsNonOptimizedQueryTotalMillis"
    /**
     * Setter for property StatsNonOptimizedQueryTotalMillis.<p>
     * Total number of milliseconds for queries that could not be resolved
     * or was partial resolved against indexes since statistics were last
     * reset.
     */
    public void setStatsNonOptimizedQueryTotalMillis(java.util.concurrent.atomic.AtomicLong atomicLong)
        {
        __m_StatsNonOptimizedQueryTotalMillis = atomicLong;
        }

    // Accessor for the property "StatsOptimizedQueryCount"
    /**
     * Setter for property StatsOptimizedQueryCount.<p>
     * Total number of queries that were fully resolved using indexes since
     * statistics were last reset.
     */
    public void setStatsOptimizedQueryCount(java.util.concurrent.atomic.AtomicLong atmomicLong)
        {
        __m_StatsOptimizedQueryCount = atmomicLong;
        }

    // Accessor for the property "StatsOptimizedQueryTotalMillis"
    /**
     * Setter for property StatsOptimizedQueryTotalMillis.<p>
     * The total number of milliseconds for optimized query operations since
     * statistics were last reset.
     */
    public void setStatsOptimizedQueryTotalMillis(java.util.concurrent.atomic.AtomicLong atomicLong)
        {
        __m_StatsOptimizedQueryTotalMillis = atomicLong;
        }

    // Accessor for the property "StatsQueryContentionCount"
    /**
     * Setter for property StatsQueryContentionCount.<p>
     * Total number of times a query had to be re-evaluated due to a
     * concurrent update since statistics were last reset. This statistics
     * provides a measure of an impact of concurrent updates on the query
     * perfomance. If the total number of queries is Q and the number of
     * contentions is C then the expected performance degradation factor
     * should be no more than (Q + C)/Q.
     */
    public void setStatsQueryContentionCount(java.util.concurrent.atomic.AtomicLong atomicLong)
        {
        __m_StatsQueryContentionCount = atomicLong;
        }

    // Accessor for the property "StatsRemoves"
    /**
     * Setter for property StatsRemoves.<p>
     * A counter for the number of removes from the backing map.
     * This counter gets incremented during direct removes caused by clear,
     * remove or invoke operations.
     */
    protected void setStatsRemoves(java.util.concurrent.atomic.AtomicLong counter)
        {
        __m_StatsRemoves = counter;
        }

    // Accessor for the property "StatsClears"
    /**
     * Setter for property StatsClears.<p>
     * A counter for the number of clear operations on the backing map.
     * This counter gets incremented during clear operations.
     */
    protected void setStatsClears(java.util.concurrent.atomic.AtomicLong counter)
        {
        __m_StatsClears = counter;
        }

    // Accessor for the property "TempBinaryEntry"
    /**
     * Setter for property TempBinaryEntry.<p>
     * A singleton temporary BinaryEntry that is used (solely) by the
     * service thread to minimize garbage creation.
     *
     * WARNING:  THIS SHOULD ONLY BE USED BY SERVICE THREAD!
     */
    protected void setTempBinaryEntry(BinaryEntry binEntry)
        {
        __m_TempBinaryEntry = binEntry;
        }

    // Accessor for the property "TriggerSet"
    /**
     * Setter for property TriggerSet.<p>
     * A set of MapTriggers registered for this cache.
     *
     * @volatile
     */
    protected void setTriggerSet(java.util.Set set)
        {
        __m_TriggerSet = set;
        }

    // Accessor for the property "Valid"
    /**
     * Setter for property Valid.<p>
     * Indicates whether the storage is valid.  If false, this means the
     * storage has not been initialized or it has been invalidated.
     *
     * This property is only modifed on the service thread.
     *
     * @volatile
     *
     * @see #setCacheName
     * @see #invalidate
     */
    protected void setValid(boolean fValid)
        {
        __m_Valid = fValid;
        }

    // Accessor for the property "Version"
    /**
     * Setter for property Version.<p>
     * Data structure holding current versions of the backing map, the
     * partitions and corresponding indicies.
     */
    protected void setVersion(com.tangosol.net.internal.StorageVersion version)
        {
        __m_Version = version;
        }

    public int size()
        {
        return getBackingMapInternal().size();
        }

    /**
     * Write a snapshot of the contents of this $Storage for the specified
     * partition to the specified store.
     *
     * @param iPartition   the partition
     * @param store          the persistent store
     */
    public void snapshotPartition(int iPartition, com.oracle.coherence.persistence.PersistentStore store, Object oToken)
        {
        // import Component.Net.Lease;
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import com.tangosol.net.cache.ConfigurableCacheMap as com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.cache.ConfigurableCacheMap$EvictionApprover as com.tangosol.net.cache.ConfigurableCacheMap.EvictionApprover;
        // import com.tangosol.net.partition.PartitionAwareBackingMap as com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        long             lCacheId   = getCacheId();
        PartitionedCache          service    = getService();
        ServiceMemberSet setMembers = service.getServiceMemberSet();

        // locks
        Map mapLeases = getLeaseMap();
        for (Iterator iterLeases = mapLeases.values().iterator(); iterLeases.hasNext(); )
            {
            Lease  lease  = (Lease) iterLeases.next();
            Binary binKey = (Binary) lease.getResourceKey();

            if (service.getKeyPartition(binKey) == iPartition)
                {
                long ldtJoined = setMembers.getServiceJoinTime(lease.getHolderId());

                // the lock holder (as known to this service) is uniquely identified
                // by its service join-time
                com.tangosol.persistence.CachePersistenceHelper.registerLock(store, lCacheId, binKey, ldtJoined, lease.getHolderThreadId(), oToken);
                }
            }

        // listeners
        Map mapListeners = getKeyListenerMap();
        if (mapListeners != null && !mapListeners.isEmpty())
            {
            for (Iterator iterListener = mapListeners.entrySet().iterator(); iterListener.hasNext();)
                {
                java.util.Map.Entry  entry  = (java.util.Map.Entry) iterListener.next();
                Binary binKey = (Binary) entry.getKey();
                if (service.getKeyPartition(binKey) == iPartition)
                    {
                    // the key-listener map is a Map<Member, Boolean> indicating lite/std
                    Map mapMembers = (Map) entry.getValue();
                    for (Iterator iterMembers = mapMembers.entrySet().iterator(); iterMembers.hasNext(); )
                        {
                        java.util.Map.Entry   entryMember = (java.util.Map.Entry) iterMembers.next();
                        Member  member      = (Member) entryMember.getKey();
                        long    ldtJoined   = setMembers.getServiceJoinTime(member.getId());
                        boolean fLite       = ((Boolean) entryMember.getValue()).booleanValue();

                        // the listening member (as known to this service) is uniquely identified
                        // by the service join-time
                        com.tangosol.persistence.CachePersistenceHelper.registerListener(store, lCacheId, binKey, ldtJoined, fLite, oToken);
                        }
                    }
                }
            }

        // data
        com.tangosol.net.partition.PartitionAwareBackingMap pabmPrime = getPartitionAwareBackingMap();
        Map  mapPart   = pabmPrime == null ? null : pabmPrime.getPartitionMap(iPartition);
        com.tangosol.net.cache.ConfigurableCacheMap  mapCCM    = mapPart != null && mapPart instanceof com.tangosol.net.cache.ConfigurableCacheMap ? (com.tangosol.net.cache.ConfigurableCacheMap) mapPart :
                                                                 getBackingConfigurableCache();

        com.tangosol.net.cache.ConfigurableCacheMap.EvictionApprover approver = null;
        try
            {
            if (mapCCM != null)
                {
                // install eviction disapprover during the BM iteration to prevent motion
                approver = mapCCM.getEvictionApprover();
                mapCCM.setEvictionApprover(com.tangosol.net.cache.ConfigurableCacheMap.EvictionApprover.DISAPPROVER);
                }

            if (mapPart != null)
                {
                // the backing-map is com.tangosol.net.partition.PartitionAwareBackingMap so we can iterate the entry-set
                for (Iterator iterEntries = mapPart.entrySet().iterator();
                     iterEntries.hasNext(); )
                    {
                    java.util.Map.Entry  entry    = (java.util.Map.Entry) iterEntries.next();
                    Binary binKey   = (Binary) entry.getKey();
                    Binary binValue = (Binary) entry.getValue();

                    store.store(lCacheId, binKey, binValue, oToken);
                    }
                }
            else
                {
                // non-PABM backing-map, so drive off of the partitioned-key index
                Map mapResource = getBackingMapInternal();
                for (Iterator iterKeys = getPartitionKeys(iPartition).iterator();
                     iterKeys.hasNext(); )
                    {
                    Binary binKey   = (Binary) iterKeys.next();
                    Binary binValue = (Binary) mapResource.get(binKey);

                    store.store(lCacheId, binKey, binValue, oToken);
                    }
                }
            }
        finally
            {
            if (mapCCM != null)
                {
                // restore the old eviction approver during the BM iteration
                mapCCM.setEvictionApprover(approver);
                }
            }

        // listeners, triggers, indexes, etc.
        persistGlobalMetadata(iPartition, store, oToken);
        }

    /**
     * Convert an array of Long objects to a long[].
     */
    protected static long[] toLongArray(java.util.Set setLong)
        {
        // import java.util.Iterator;

        int c = setLong.size();
        if (c == 0)
            {
            return null;
            }

        long[]   al   = new long[c];
        Iterator iter = setLong.iterator();

        for (int i = 0; iter.hasNext(); i++)
            {
            al[i] = ((Long) iter.next()).longValue();
            }
        return al;
        }

    // Declared at the super level
    public String toString()
        {
        return get_Name() +
               " (CacheName=" + getCacheName() +
               ", CacheId="   + getCacheId()   + ')';
        }

    /**
     * Record the actual query execution cost by the given filter.
     *
     * @param filter  the filter
     * @param partMask  the partitions involved in the query
     */
    public com.tangosol.util.QueryRecord.PartialResult trace(com.tangosol.util.Filter filter, com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.internal.util.SimpleQueryContext as com.tangosol.internal.util.SimpleQueryContext;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.QueryRecord$PartialResult$TraceStep as com.tangosol.util.QueryRecord.PartialResult.TraceStep;
        // import com.tangosol.util.SimpleQueryRecord$PartialResult as com.tangosol.util.SimpleQueryRecord.PartialResult;
        // import com.tangosol.util.SubSet;
        // import com.tangosol.util.filter.QueryRecorderFilter;
        // import com.tangosol.util.filter.WrapperQueryRecorderFilter;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Set;

        com.tangosol.internal.util.SimpleQueryContext ctx     = new com.tangosol.internal.util.SimpleQueryContext(this);
        com.tangosol.util.SimpleQueryRecord.PartialResult  result  = new com.tangosol.util.SimpleQueryRecord.PartialResult(ctx, partMask);
        Set     setKeys = new SubSet(collectKeySet(partMask, true));

        if (!setKeys.isEmpty())
            {
            com.tangosol.util.QueryRecord.PartialResult.TraceStep step = result.instantiateTraceStep(filter);

            QueryRecorderFilter filterRecorder = filter instanceof QueryRecorderFilter
                                                 ? (QueryRecorderFilter) filter
                                                 : new WrapperQueryRecorderFilter(filter);

            filter = filterRecorder.trace(ctx, step, setKeys);

            if (filter != null)
                {
                // we still have a filter to process the remaining keys
                Map mapPrime = getBackingMapInternal();

                step = result.instantiateTraceStep(filter);

                filterRecorder = filter instanceof QueryRecorderFilter
                                 ? (QueryRecorderFilter) filter
                                 : new WrapperQueryRecorderFilter(filter);

                BinaryEntry entryTemp = instantiateBinaryEntry(null, null, true);

                for (Iterator iter = setKeys.iterator(); iter.hasNext();)
                    {
                    Binary binKey   = (Binary) iter.next();
                    Binary binValue = (Binary) mapPrime.get(binKey);
                    if (binValue == null)
                        {
                        continue;
                        }

                    entryTemp.reset(binKey, binValue);

                    filterRecorder.trace(ctx, step, entryTemp);
                    }
                }
            }

        return result;
        }

    /**
     * Initialize the cloned $Storage with new resources.
     */
    public Storage truncate()
        {
        // import com.tangosol.util.ValueExtractor;
        // import java.util.Comparator;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;

        Map      mapExtractor = getIndexExtractorMap();
        String   sCacheName   = getCacheName();
        PartitionedCache  service      = getService();
        Storage storageNew   = (Storage) service._newChild("Storage");

        storageNew.setCacheId(getCacheId());
        storageNew.setIndexExtractorMap(mapExtractor);

        Set setTrigger = getTriggerSet();
        if (setTrigger != null)
            {
            storageNew.setTriggerSet(setTrigger);
            }

        Map mapFilter = getFilterIdMap();
        if (mapFilter != null)
            {
            storageNew.setFilterIdMap(mapFilter);
            }

        Map mapListener = getListenerMap();
        if (mapListener != null)
            {
            storageNew.setListenerMap(mapListener);
            }

        // remove persisted data
        service.truncatePersistentExtent(this);

        // destroy the old storage
        invalidate();

        // initalize the cloned Storage with a new resources
        // including backing map and indices

        storageNew.ensureInitialized(sCacheName);

        for (Iterator iter = mapExtractor.entrySet().iterator(); iter.hasNext(); )
            {
            java.util.Map.Entry          entry      = (java.util.Map.Entry) iter.next();
            ValueExtractor extractor  = (ValueExtractor) entry.getKey();
            Comparator     comparator = (Comparator) entry.getValue();

            for (Iterator it = getPartitionedIndexMap().keySet().iterator(); it.hasNext(); )
                {
                Integer nPart = (Integer) it.next();
                storageNew.createMapIndex(storageNew.getPartitionIndexMap(nPart.intValue()), extractor, comparator);
                }
            }

        return storageNew;
        }

    /**
     * Called on the service or a daemon pool thread.
     */
    public boolean unlock(com.tangosol.coherence.component.net.Lease lease)
        {
        // import Component.Net.Lease;
        // import com.oracle.coherence.persistence.PersistentStore;
        // import com.tangosol.persistence.CachePersistenceHelper as com.tangosol.persistence.CachePersistenceHelper;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Map;

        Map    mapLease        = getLeaseMap();
        Binary binKey          = (Binary) lease.getResourceKey();
        Lease  leaseCurrent    = (Lease)  mapLease.get(binKey);
        int    nHolderId       = lease.getHolderId();
        long   lHolderThreadId = lease.getHolderThreadId();

        if (leaseCurrent == null ||
            (leaseCurrent.getHolderId()       == nHolderId &&
             leaseCurrent.getHolderThreadId() == lHolderThreadId))
            {
            mapLease.remove(binKey);

            if (isPersistent())
                {
                // persist the lock registration
                PartitionedCache           service       = getService();
                int               nPartition    = service.getKeyPartition(binKey);
                PartitionedCache.PartitionControl ctrlPartition = (PartitionedCache.PartitionControl) service.getPartitionControl(nPartition);
                PersistentStore   store         = ctrlPartition.getPersistentStore();
                long              ldtJoined     = service.getServiceMemberSet().getServiceJoinTime(nHolderId);

                // the lock holder (as known to this service) is uniquely identified
                // by its service join-time
                com.tangosol.persistence.CachePersistenceHelper.unregisterLock(store, getCacheId(), binKey, ldtJoined, lHolderThreadId, /*oToken*/ null);
                }

            return true;
            }
        else
            {
            // check the request queue and remove pending lock requests
            // that came from that same requestor
            List listPending = getPendingLockRequest();
            synchronized (listPending)
                {
                PartitionedCache service = getService();
                for (Iterator iter = listPending.iterator(); iter.hasNext();)
                    {
                    PartitionedCache.LockRequest msgRequest = (PartitionedCache.LockRequest) iter.next();

                    if (msgRequest.getKey().equals(binKey) &&
                        msgRequest.getLeaseHolderId() == lease.getHolderId() &&
                        msgRequest.getLeaseThreadId() == lease.getHolderThreadId())
                        {
                        iter.remove();

                        PartitionedCache.Response msgResponse =
                                (PartitionedCache.Response) service.instantiateMessage("Response");
                        msgResponse.respondTo(msgRequest);
                        msgResponse.setResult(PartitionedCache.Response.RESULT_RETRY);
                        service.post(msgResponse);
                        }
                    }
                }
            return false;
            }
        }

    public void updateConverters()
        {
        PartitionedCache.BackingMapContext ctx = getService().getBackingMapContext();

        setConverterUp       (ctx.getValueFromInternalConverter());
        setConverterKeyDown  (ctx.getKeyToInternalConverter());
        setConverterValueDown(ctx.getValueToInternalConverter());
        }

    /**
     * Update the index for the specified partition. Used during index
     * rebuild/recovery or index creation (see onUpdateIndexRequest).
     *
     * @param mapIndex the index map that contains a subset of extractors to
     * be processed; if null, all extractors for this Storage are to be
     * processed
     * @param fInitial true if it is an intitial index creation (from
     * addIndex) and the index registration should be persisted
     *
     * @return null if the index is successfully updated; otherwise a list
     * of "offending" extractors
     */
    public java.util.List updateIndex(int nEventId, int nPartition, java.util.Map mapIndex)
        {
        // import com.tangosol.net.GuardSupport;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ValueExtractor;
        // import java.util.Comparator;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Map;
        // import java.util.Set;

        Map          map       = getBackingMapInternal();
        BinaryEntry entryTemp = instantiateBinaryEntry(null, null, true);
        Set          setKeys   = getKeySet(nPartition);

        if (setKeys == null)
            {
            // should never happen; see the comments in collectKeySet()
            setKeys = collectKeySet(nPartition);
            }
        else
            {
            // setKeys is a live set; we can safely use it without locking
            // since updates are not allowed yet (see ensureIndexReady)
            }

        int cEntries = 0;
        for (Iterator iter = setKeys.iterator(); iter.hasNext();)
            {
            entryTemp.reset((Binary) iter.next());

            if (entryTemp.isPresent())
                {
                List listFailed = updateIndex(nEventId, entryTemp, mapIndex);

                if (listFailed != null)
                    {
                    return listFailed;
                    }
                }

            // COH-3006: index insertion is a potentially expensive operation;
            //           issue a guardian heartbeat every 1024 entries
            if ((++cEntries & 0x3FF) == 0x3FF)
                {
                GuardSupport.heartbeat();
                }
            }

        return null;
        }

    /**
     * Update the index based on the specified entry representing the
     * change. If a failure occurs, the corresponding index will be removed.
     *
     * @param mapIndex the index map that contains a subset of extractors to
     * be processed; if null, all extractors for this Storage are to be
     * processed
     *
     * @return null if the index is successfully updated; otherwise a list
     * of "offending" extractors
     */
    public java.util.List updateIndex(int nEventId, BinaryEntry binEntry, java.util.Map mapIndex)
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
        // import com.tangosol.util.MapIndex;
        // import com.tangosol.util.ValueExtractor;
        // import java.util.Iterator;
        // import java.util.List;

        int nPart = getService().getKeyPartition(binEntry.getBinaryKey());
        if (mapIndex == null)
            {
            mapIndex = getPartitionIndexMap(nPart);
            }

        List listFailed = null;

        if (!mapIndex.isEmpty())
            {
            for (Iterator iter = Base.randomize(mapIndex.keySet()).iterator(); iter.hasNext();)
                {
                ValueExtractor extractor = (ValueExtractor) iter.next();
                MapIndex       index     = (MapIndex) getPartitionIndexMap(nPart).get(extractor);

                binEntry.setForceExtract(true);
                try
                    {
                    // update partitioned index
                    if (index != null)
                        {
                        switch (nEventId)
                            {
                            case com.tangosol.util.MapEvent.ENTRY_INSERTED:
                                index.insert(binEntry);
                                break;

                            case com.tangosol.util.MapEvent.ENTRY_UPDATED:
                                index.update(binEntry);
                                break;

                            case com.tangosol.util.MapEvent.ENTRY_DELETED:
                                index.delete(binEntry);
                                break;
                            }
                        }
                    }
                catch (RuntimeException e)
                    {
                    listFailed = processIndexFailure(e,
                                                     extractor, binEntry, listFailed);
                    }
                binEntry.setForceExtract(false);
                }
            }

        return listFailed;
        }

    /**
     * Update index build duration cumulatively
     *
     * @param ldtStart the time at which duration has started, to be
     * substracted from time now.
     */
    public void updateIndexStatistics(long ldtStart)
        {
        // import com.tangosol.util.Base;

        getStatsIndexingTotalMillis().getAndAdd(Base.getSafeTimeMillis() - ldtStart);
        }

    /**
     * Called on the service thread, daemon pool thread or a write-behind
     * thread.
     */
    public void updateKeyIndex(com.tangosol.util.MapEvent event)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;

        PartitionAwareBackingMap mapKeyIndex = getPartitionedKeyIndex();
        if (mapKeyIndex != null)
            {
            Binary binKey = (Binary) event.getKey();
            switch (event.getId())
                {
                case PartitionedCache.MapEvent.ENTRY_INSERTED:
                    mapKeyIndex.put(binKey, binKey);
                    break;

                case PartitionedCache.MapEvent.ENTRY_DELETED:
                    mapKeyIndex.remove(binKey);
                    break;
                }
            }
        }

    /**
     * Update the query statistics.
     *
     * @param filterOrig  the filter passed to the #query
     * @param fOptimized  true indicates that the all filters matched an index
     * @param ldtStart    the time when the query started executing
     * @param cTotal      the total number of entries evaluated
     * @param cScanned    the number of entries evaluated individually
     * @param cResults    the number of returned entries matching the query
     * @param nQueryType  one of the QUERY_* values
     * @param partMask    partitionSet that all keys should belong to
     */
    protected void updateQueryStatistics(com.tangosol.util.Filter filterOrig, boolean fOptimized, long ldtStart, int cTotal, int cScanned, int cResults, int nQueryType, com.tangosol.net.partition.PartitionSet partMask)
        {
        // import com.tangosol.util.Base;

        // Update the query statistics
        long    cMillis  = Base.getSafeTimeMillis() - ldtStart;
        boolean fPartial = cTotal > cScanned;

        if (fOptimized)
            {
            getStatsOptimizedQueryCount().incrementAndGet();
            getStatsOptimizedQueryTotalMillis().getAndAdd(cMillis);
            }
        else
            {
            getStatsNonOptimizedQueryCount().incrementAndGet();
            getStatsNonOptimizedQueryTotalMillis().getAndAdd(cMillis);
            }

        if (cMillis >= getStatsMaxQueryDurationMillis() &&
            cMillis >= getStatsMaxQueryThresholdMillis())
            {
            String sItem       = nQueryType == QUERY_KEYS ? " keys" : " entries";
            String sFilterDesc = filterOrig == null ? ""
                                                    : Base.truncateString(filterOrig.toString(), 1024);

            if (filterOrig == null)
                {
                setStatsMaxQueryDescription("Full key set retrieval for " + partMask.cardinality()
                                            + " partitions (" + cTotal + " " + sItem + ") - duration "
                                            + cMillis + "ms.");
                }
            else if (fOptimized)
                {
                setStatsMaxQueryDescription("Optimized query (" + cTotal + " " + sItem
                                            + ", "+ cResults + " matches for " + sFilterDesc
                                            + ") - duration " + cMillis + "ms.");
                }
            else if (fPartial)
                {
                setStatsMaxQueryDescription("Partially optimized query (" + cTotal
                                            + " " + sItem + ", scan of " + cScanned + ", " + cResults
                                            + " matches for " + sFilterDesc + ") - duration "
                                            + cMillis + "ms.");
                }
            else
                {
                setStatsMaxQueryDescription("Non optimized query (" + cTotal
                                            + " " +  sItem  + ", full scan, " + cResults + " matches for "
                                            + sFilterDesc + ") - duration " + cMillis + "ms.");
                }

            setStatsMaxQueryDurationMillis(cMillis);
            }
        }

    /**
     * Remove all the listener proxies representing departed members. Called
     * on the service thread only.
     */
    public void validateListeners()
        {
        // import Component.Net.MemberSet;
        // import com.tangosol.util.Binary;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;

        PartitionedCache   service    = getService();
        MemberSet setMembers = service.getServiceMemberSet();

        Map[] aMap = new Map[]
                {
                        getListenerMap(), getKeyListenerMap(), getBackupKeyListenerMap(),
                        };

        for (int i = 0, c = aMap.length; i < c; i++)
            {
            Map map = aMap[i];
            while (map != null)
                {
                try
                    {
                    for (Iterator iter = map.values().iterator(); iter.hasNext();)
                        {
                        Map mapMembers = (Map) iter.next();

                        mapMembers.keySet().retainAll(setMembers);
                        if (mapMembers.isEmpty())
                            {
                            iter.remove();
                            }
                        }
                    break;
                    }
                catch (ConcurrentModificationException ignored) {}
                }
            }

        Map mapFilterId = getFilterIdMap();
        if (mapFilterId != null)
            {
            mapFilterId.keySet().retainAll(setMembers);
            }
        }

    /**
     * Remove all the locks held by the departed members and fire relevant
     * pending LockRequests. Called on the service thread only.
     */
    public void validateLocks()
        {
        // import Component.Net.Lease;
        // import Component.Net.MemberSet;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.LiteSet;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Map;
        // import java.util.Set;

        PartitionedCache   service    = getService();
        MemberSet setMembers = service.getServiceMemberSet();

        // remove leases by departed members and collect the corresponding keys
        Set setKeys = new LiteSet();
        while (true)
            {
            try
                {
                Map mapLease = getLeaseMap();
                // protect against lock by member which is concurrently dying or leaving cluster;
                // see lock
                synchronized (mapLease)
                    {
                    for (Iterator iter = getLeaseMap().values().iterator(); iter.hasNext();)
                        {
                        Lease lease = (Lease) iter.next();
                        if (!setMembers.contains(lease.getHolderId()))
                            {
                            setKeys.add(lease.getResourceKey());
                            iter.remove();
                            }
                        }
                    break;
                    }
                }
            catch (ConcurrentModificationException ignored) {}
            }

        // remove backup leases by departed members
        while (service.getBackupCount() > 0)
            {
            try
                {
                for (Iterator iter = getBackupLeaseMap().values().iterator(); iter.hasNext();)
                    {
                    Lease lease = (Lease) iter.next();
                    if (!setMembers.contains(lease.getHolderId()))
                        {
                        iter.remove();
                        }
                    }
                break;
                }
            catch (ConcurrentModificationException ignored) {}
            }

        // remove pending leases by departed members
        List listPending = getPendingLockRequest();
        synchronized (listPending)
            {
            if (!listPending.isEmpty())
                {
                for (Iterator iter = listPending.iterator(); iter.hasNext();)
                    {
                    PartitionedCache.LockRequest msgLock = (PartitionedCache.LockRequest) iter.next();

                    // clear up lock requests by dead members
                    if (!setMembers.contains(msgLock.getFromMember()))
                        {
                        iter.remove();
                        }
                    }
                }
            }

        // fire relevant events (internal)
        for (Iterator iter = setKeys.iterator(); iter.hasNext();)
            {
            firePendingLocks((Binary) iter.next());
            }
        }

    /**
     * For debugging only.
     *
     * @return PartitionSet for entries that are present, but not supposed
     * to be
     */
    public com.tangosol.net.partition.PartitionSet validatePartitionedContent(boolean fPrimary, boolean fFix)
        {
        // import com.tangosol.net.partition.PartitionAwareBackingMap;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.Map;

        PartitionedCache      service      = getService();
        Map          map          = fPrimary ? getBackingMapInternal() : getBackupMap();
        PartitionSet partsInvalid = service.collectOwnedPartitions(fPrimary);

        partsInvalid.invert();

        if (map instanceof PartitionAwareBackingMap)
            {
            PartitionAwareBackingMap mapPartitioned = (PartitionAwareBackingMap) map;

            for (int iPartition = partsInvalid.next(0); iPartition >= 0;
                 iPartition = partsInvalid.next(iPartition + 1))
                {
                if (mapPartitioned.getPartitionMap(iPartition) == null)
                    {
                    partsInvalid.remove(iPartition);

                    if (fFix)
                        {
                        mapPartitioned.destroyPartition(iPartition);
                        }
                    }
                }
            }
        else
            {
            PartitionSet partsTemp = new PartitionSet(partsInvalid.getPartitionCount());

            for (Iterator iter = map.keySet().iterator(); iter.hasNext();)
                {
                Binary binKey     = (Binary) iter.next();
                int    iPartition = service.getKeyPartition(binKey);
                if (partsInvalid.contains(iPartition))
                    {
                    partsTemp.add(iPartition);

                    if (fFix)
                        {
                        iter.remove();
                        }
                    }
                }
            partsInvalid = partsTemp;
            }

        if (!partsInvalid.isEmpty())
            {
            PartitionAwareBackingMap mapKeyIndex = getPartitionedKeyIndex();
            if (fFix && mapKeyIndex != null)
                {
                for (int iPartition = partsInvalid.next(0); iPartition >= 0;
                     iPartition = partsInvalid.next(iPartition + 1))
                    {
                    if (mapKeyIndex.getPartitionMap(iPartition) != null)
                        {
                        mapKeyIndex.destroyPartition(iPartition);
                        }
                    }
                }

            _trace("Discovered non-owned " + (fPrimary ? "primary" : "backup")
                   + " " + partsInvalid, 1);
            return partsInvalid;
            }
        return null;
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$Advancer

    /**
     * A Streamer implementation based on a key iterator, a storage version
     * and a filter.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Advancer
            extends    com.tangosol.coherence.component.util.collections.AdvancingIterator
            implements com.tangosol.util.Streamer
        {
        // ---- Fields declarations ----

        /**
         * Property CheckVersion
         *
         * True if query should be reevaluated when storage version changed.
         */
        private boolean __m_CheckVersion;

        /**
         * Property Count
         *
         * The number of processed elements.
         */
        private int __m_Count;

        /**
         * Property DeferredEntries
         *
         * Map<Binary, $BinaryEntry> that contains entries that require
         * re-evaluation.
         */
        private java.util.Map __m_DeferredEntries;

        /**
         * Property DeferredPartitions
         *
         * PartitionSet for entries that require re-evaluation.
         */
        private com.tangosol.net.partition.PartitionSet __m_DeferredPartitions;

        /**
         * Property EntryTemp
         *
         * A single entry per Advancer (if ReuseAllowed is true).
         */
        private BinaryEntry __m_EntryTemp;

        /**
         * Property Filter
         *
         * The filter that remained to be evaluated.
         */
        private com.tangosol.util.Filter __m_Filter;

        /**
         * Property FilterOriginal
         *
         * The filter used by the corresponding query.
         */
        private com.tangosol.util.Filter __m_FilterOriginal;

        /**
         * Property Iterator
         *
         * An iterator that contains keys or inflated entries.
         */
        private java.util.Iterator __m_Iterator;

        /**
         * Property PresentOnly
         *
         * If true, only the "present" entries should be returned.
         */
        private boolean __m_PresentOnly;

        /**
         * Property ReuseAllowed
         *
         * If true, the $BinaryEntry object could be reused across the
         * calls.
         */
        private boolean __m_ReuseAllowed;

        /**
         * Property Size
         *
         * An estimated size.
         */
        private int __m_Size;

        /**
         * Property Version
         *
         * The CommittedVersion for the parent $Storage taken before the
         * corresponding query ran.
         */
        private long __m_Version;

        // Default constructor
        public Advancer()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public Advancer(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new Advancer();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$Advancer".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // Declared at the super level
        protected Object advance()
            {
            // import com.tangosol.net.internal.StorageVersion;
            // import com.tangosol.net.internal.Trint;
            // import com.tangosol.net.partition.PartitionSet;
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.InvocableMapHelper;
            // import com.tangosol.util.Filter;
            // import java.util.HashMap;
            // import java.util.Iterator;
            // import java.util.Map;

            Iterator       iterator = getIterator();
            Filter         filter   = getFilter();
            long           lVersion = getVersion();
            Storage        storage  = getStorage();
            StorageVersion version  = storage.getVersion();
            BinaryEntry    entry    = getEntryTemp();

            // In the same way as in  createQueryResult(), we will skip the entry
            // initialization if the backing map is not evicting.
            // Note: to be more correct, though, we should also have a flag indicating that
            // the indexed attributes are "constants" - not changing for a given entry.

            boolean fInit    = storage.isPotentiallyEvicting() || isPresentOnly() || filter != null;
            Map     mapPrime = storage.getBackingInternalCache();

            while (iterator.hasNext())
                {
                Object oNext = iterator.next();

                int c = getCount();
                setCount(c + 1);
                if ((c & 0x3FF) == 0x3FF)
                    {
                    storage.getService().checkInterrupt();
                    }

                if (oNext instanceof Binary)
                    {
                    Binary binKey   = (Binary) oNext;
                    Binary binValue = null;

                    if (fInit)
                        {
                        binValue = (Binary) mapPrime.get(binKey);
                        if (binValue == null && isPresentOnly())
                            {
                            continue;
                            }
                        }

                    if (entry == null)
                        {
                        entry = storage.instantiateBinaryEntry(binKey, binValue, true);
                        if (isReuseAllowed())
                            {
                            setEntryTemp(entry);
                            }
                        }
                    else
                        {
                        entry.reset(binKey, binValue); // read-only is preserved
                        }

                    int nPart = entry.getKeyPartition();
                    if (!isCheckVersion()
                        || version.getCommittedVersion() <= lVersion
                        || !version.isPartitionModified(lVersion, nPart))
                        {
                        entry.setVersionTrint(Trint.makeTrint14(lVersion));

                        if (filter == null || InvocableMapHelper.evaluateEntry(filter, entry))
                            {
                            // the entry is good
                            return entry;
                            }
                        else
                            {
                            // the entry doesn't match; reuse
                            continue;
                            }
                        }
                    else
                        {
                        if (storage.getDeserializationAccelerator(nPart) != null)
                            {
                            // the cost of direct re-evaluation should be low,
                            // but the forward index content could be stale
                            entry.setForceExtract(true);

                            if (InvocableMapHelper.evaluateEntry(getFilterOriginal(), entry))
                                {
                                // the entry is good
                                return entry;
                                }

                            // the entry is no longer a match; reuse
                            continue;
                            }

                        // defer the re-evaluation
                        Map mapDefer = getDeferredEntries();
                        if (mapDefer == null)
                            {
                            setDeferredEntries(mapDefer = new HashMap());
                            }
                        mapDefer.put(binKey, entry);

                        PartitionSet partsDefer = getDeferredPartitions();
                        if (partsDefer == null)
                            {
                            setDeferredPartitions(partsDefer =
                                                          new PartitionSet(storage.getService().getPartitionCount()));
                            }
                        partsDefer.add(nPart);

                        entry = null; // don't reuse
                        }
                    }
                else if (oNext != null) // null in the middle should not happen
                    {
                    // must be an "inflated" entry
                    return (BinaryEntry) oNext;
                    }
                }

            Map mapDeferred = getDeferredEntries();
            if (mapDeferred != null)
                {
                storage.reevaluateQueryResults(
                        getFilterOriginal(), mapDeferred, QUERY_KEYS, getDeferredPartitions());

                setIterator(mapDeferred.values().iterator());
                setDeferredEntries(null);
                setDeferredPartitions(null);
                setCheckVersion(false);

                return advance();
                }

            setEntryTemp(null);
            return null;
            }

        // From interface: com.tangosol.util.Streamer
        public int characteristics()
            {
            // import com.tangosol.util.Streamer;

            // negative size indicates that the number of entries could be
            // significantly less than the estimateSize() value
            return getFilter() == null && getSize() >= 0 ? Streamer.SIZED : 0;
            }

        // Accessor for the property "Count"
        /**
         * Getter for property Count.<p>
         * The number of processed elements.
         */
        private int getCount()
            {
            return __m_Count;
            }

        // Accessor for the property "DeferredEntries"
        /**
         * Getter for property DeferredEntries.<p>
         * Map<Binary, $BinaryEntry> that contains entries that require
         * re-evaluation.
         */
        private java.util.Map getDeferredEntries()
            {
            return __m_DeferredEntries;
            }

        // Accessor for the property "DeferredPartitions"
        /**
         * Getter for property DeferredPartitions.<p>
         * PartitionSet for entries that require re-evaluation.
         */
        private com.tangosol.net.partition.PartitionSet getDeferredPartitions()
            {
            return __m_DeferredPartitions;
            }

        // Accessor for the property "EntryTemp"
        /**
         * Getter for property EntryTemp.<p>
         * A single entry per Advancer (if ReuseAllowed is true).
         */
        private BinaryEntry getEntryTemp()
            {
            return __m_EntryTemp;
            }

        // Accessor for the property "Filter"
        /**
         * Getter for property Filter.<p>
         * The filter that remained to be evaluated.
         */
        protected com.tangosol.util.Filter getFilter()
            {
            return __m_Filter;
            }

        // Accessor for the property "FilterOriginal"
        /**
         * Getter for property FilterOriginal.<p>
         * The filter used by the corresponding query.
         */
        protected com.tangosol.util.Filter getFilterOriginal()
            {
            return __m_FilterOriginal;
            }

        // Accessor for the property "Iterator"
        /**
         * Getter for property Iterator.<p>
         * An iterator that contains keys or inflated entries.
         */
        protected java.util.Iterator getIterator()
            {
            return __m_Iterator;
            }

        // Accessor for the property "Size"
        /**
         * Getter for property Size.<p>
         * An estimated size.
         */
        protected int getSize()
            {
            return __m_Size;
            }

        // Accessor for the property "Storage"
        /**
         * Getter for property Storage.<p>
         */
        protected Storage getStorage()
            {
            return (Storage) get_Parent();
            }

        // Accessor for the property "Version"
        /**
         * Getter for property Version.<p>
         * The CommittedVersion for the parent $Storage taken before the
         * corresponding query ran.
         */
        protected long getVersion()
            {
            return __m_Version;
            }

        // Accessor for the property "CheckVersion"
        /**
         * Getter for property CheckVersion.<p>
         * True if query should be reevaluated when storage version changed.
         */
        public boolean isCheckVersion()
            {
            return __m_CheckVersion;
            }

        // Accessor for the property "PresentOnly"
        /**
         * Getter for property PresentOnly.<p>
         * If true, only the "present" entries should be returned.
         */
        protected boolean isPresentOnly()
            {
            return __m_PresentOnly;
            }

        // Accessor for the property "ReuseAllowed"
        /**
         * Getter for property ReuseAllowed.<p>
         * If true, the $BinaryEntry object could be reused across the calls.
         */
        protected boolean isReuseAllowed()
            {
            return __m_ReuseAllowed;
            }

        // Accessor for the property "CheckVersion"
        /**
         * Setter for property CheckVersion.<p>
         * True if query should be reevaluated when storage version changed.
         */
        public void setCheckVersion(boolean fVersion)
            {
            __m_CheckVersion = fVersion;
            }

        // Accessor for the property "Count"
        /**
         * Setter for property Count.<p>
         * The number of processed elements.
         */
        private void setCount(int c)
            {
            __m_Count = c;
            }

        // Accessor for the property "DeferredEntries"
        /**
         * Setter for property DeferredEntries.<p>
         * Map<Binary, $BinaryEntry> that contains entries that require
         * re-evaluation.
         */
        private void setDeferredEntries(java.util.Map map)
            {
            __m_DeferredEntries = map;
            }

        // Accessor for the property "DeferredPartitions"
        /**
         * Setter for property DeferredPartitions.<p>
         * PartitionSet for entries that require re-evaluation.
         */
        private void setDeferredPartitions(com.tangosol.net.partition.PartitionSet parts)
            {
            __m_DeferredPartitions = parts;
            }

        // Accessor for the property "EntryTemp"
        /**
         * Setter for property EntryTemp.<p>
         * A single entry per Advancer (if ReuseAllowed is true).
         */
        private void setEntryTemp(BinaryEntry entry)
            {
            __m_EntryTemp = entry;
            }

        // Accessor for the property "Filter"
        /**
         * Setter for property Filter.<p>
         * The filter that remained to be evaluated.
         */
        public void setFilter(com.tangosol.util.Filter filter)
            {
            __m_Filter = filter;
            }

        // Accessor for the property "FilterOriginal"
        /**
         * Setter for property FilterOriginal.<p>
         * The filter used by the corresponding query.
         */
        public void setFilterOriginal(com.tangosol.util.Filter filter)
            {
            __m_FilterOriginal = filter;
            }

        // Accessor for the property "Iterator"
        /**
         * Setter for property Iterator.<p>
         * An iterator that contains keys or inflated entries.
         */
        public void setIterator(java.util.Iterator iter)
            {
            __m_Iterator = iter;
            }

        // Accessor for the property "PresentOnly"
        /**
         * Setter for property PresentOnly.<p>
         * If true, only the "present" entries should be returned.
         */
        public void setPresentOnly(boolean fOnly)
            {
            __m_PresentOnly = fOnly;
            }

        // Accessor for the property "ReuseAllowed"
        /**
         * Setter for property ReuseAllowed.<p>
         * If true, the $BinaryEntry object could be reused across the calls.
         */
        public void setReuseAllowed(boolean fReuse)
            {
            __m_ReuseAllowed = fReuse;
            }

        // Accessor for the property "Size"
        /**
         * Setter for property Size.<p>
         * An estimated size.
         */
        public void setSize(int c)
            {
            __m_Size = c;
            }

        // Accessor for the property "Version"
        /**
         * Setter for property Version.<p>
         * The CommittedVersion for the parent $Storage taken before the
         * corresponding query ran.
         */
        public void setVersion(long lVersion)
            {
            __m_Version = lVersion;
            }

        // From interface: com.tangosol.util.Streamer
        public long size()
            {
            return Math.abs(getSize());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$BackingManager

    /**
     * BackingMapManager used by the $Storage to manage backup maps, key
     * index and to serve as a bridge manager for
     * PartitionAwareBackingMap(s).
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BackingManager
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.net.BackingMapManager
        {
        // ---- Fields declarations ----

        /**
         * Property BACKUP_CUSTOM
         *
         * <backup-storage/type> value is "custom".
         */
        public static final int BACKUP_CUSTOM = 3;

        /**
         * Property BACKUP_FILE
         *
         * <backup-storage/type> value is "file-mapped".
         */
        public static final int BACKUP_FILE = 2;

        /**
         * Property BACKUP_FLASHJOURNAL
         *
         * Used to represent the default backup map type for flashjournal.
         */
        public static final int BACKUP_FLASHJOURNAL = 7;

        /**
         * Property BACKUP_OFFHEAP
         *
         * <backup-storage/type> value is "off-heap"
         */
        public static final int BACKUP_OFFHEAP = 1;

        /**
         * Property BACKUP_ONHEAP
         *
         * <backup-storage/type> value is "on-heap"
         */
        public static final int BACKUP_ONHEAP = 0;

        /**
         * Property BACKUP_RAMJOURNAL
         *
         * Used to represent the default backup map type for ramjournal
         */
        public static final int BACKUP_RAMJOURNAL = 8;

        /**
         * Property BACKUP_SCHEME
         *
         * <backup-storage/type> value is "scheme".
         */
        public static final int BACKUP_SCHEME = 4;

        /**
         * Property BackupClass
         *
         * Specifies the class for custom backup storage implementation.
         */
        private Class __m_BackupClass;

        /**
         * Property BackupDir
         *
         * Specifies the directory for file-mapped backup. Applicable only
         * if the BackupType is BACKUP_FILE.
         */
        private java.io.File __m_BackupDir;

        /**
         * Property BackupInitSize
         *
         * Specifies the initial buffer size. Applicable only if the
         * BackupType is BACKUP_OFFHEAP or BACKUP_FILE.
         *
         * Default value is 1MB.
         */
        private int __m_BackupInitSize;

        /**
         * Property BackupMaxSize
         *
         * Specifies the maximum buffer size. Applicable only if the
         * BackupType is BACKUP_OFFHEAP or BACKUP_FILE.
         *
         * Default value is 1GB.
         */
        private int __m_BackupMaxSize;

        /**
         * Property BackupScheme
         *
         * Specifies the scheme name for backup storage implementation.
         */
        private String __m_BackupScheme;

        /**
         * Property CacheName
         *
         * The cache name for the Storage component this manager is
         * associated with. We could not use the
         * getStorage().getCacheName() due to the initialization ordering
         * (COH-3486).
         */
        private String __m_CacheName;

        /**
         * Property Partitioned
         *
         * Specifies wether the backup backing map for the corresponding
         * Storage should be partitioned. In most cases the answer is "yes"
         * and this is provided as a backward-compatibility safety net.
         */
        private boolean __m_Partitioned;

        /**
         * Property Type
         *
         * One of the BACKUP_*, KEY_INDEX or FACTORY_ADAPTER constants.
         */
        private int __m_Type;

        // Default constructor
        public BackingManager()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public BackingManager(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setBackupInitSize(1048576);
                setBackupMaxSize(1073741824);
                setPartitioned(true);
                setType(-1);
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
            return new BackingManager();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$BackingManager".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // Accessor for the property "BackupClass"
        /**
         * Getter for property BackupClass.<p>
         * Specifies the class for custom backup storage implementation.
         */
        protected Class getBackupClass()
            {
            return __m_BackupClass;
            }

        // Accessor for the property "BackupDir"
        /**
         * Getter for property BackupDir.<p>
         * Specifies the directory for file-mapped backup. Applicable only
         * if the BackupType is BACKUP_FILE.
         */
        protected java.io.File getBackupDir()
            {
            return __m_BackupDir;
            }

        // Accessor for the property "BackupInitSize"
        /**
         * Getter for property BackupInitSize.<p>
         * Specifies the initial buffer size. Applicable only if the
         * BackupType is BACKUP_OFFHEAP or BACKUP_FILE.
         *
         * Default value is 1MB.
         */
        protected int getBackupInitSize()
            {
            return __m_BackupInitSize;
            }

        // Accessor for the property "BackupMaxSize"
        /**
         * Getter for property BackupMaxSize.<p>
         * Specifies the maximum buffer size. Applicable only if the
         * BackupType is BACKUP_OFFHEAP or BACKUP_FILE.
         *
         * Default value is 1GB.
         */
        protected int getBackupMaxSize()
            {
            return __m_BackupMaxSize;
            }

        // Accessor for the property "BackupScheme"
        /**
         * Getter for property BackupScheme.<p>
         * Specifies the scheme name for backup storage implementation.
         */
        protected String getBackupScheme()
            {
            return __m_BackupScheme;
            }

        // From interface: com.tangosol.net.BackingMapManager
        public com.tangosol.net.ConfigurableCacheFactory getCacheFactory()
            {
            return null;
            }

        // Accessor for the property "CacheName"
        /**
         * Getter for property CacheName.<p>
         * The cache name for the Storage component this manager is
         * associated with. We could not use the getStorage().getCacheName()
         * due to the initialization ordering (COH-3486).
         */
        public String getCacheName()
            {
            return __m_CacheName;
            }

        // From interface: com.tangosol.net.BackingMapManager
        public com.tangosol.net.BackingMapManagerContext getContext()
            {
            return getService().getBackingMapContext();
            }

        // Accessor for the property "DefaultFactory"
        /**
         * Getter for property DefaultFactory.<p>
         * The DefaultConfigurableCacheFactory associated with the service.
         */
        public com.tangosol.net.DefaultConfigurableCacheFactory getDefaultFactory()
            {
            // import com.tangosol.net.DefaultConfigurableCacheFactory$Manager as com.tangosol.net.DefaultConfigurableCacheFactory.Manager;

            com.tangosol.net.DefaultConfigurableCacheFactory.Manager mgr = (com.tangosol.net.DefaultConfigurableCacheFactory.Manager) getService().getBackingMapManager();
            return mgr.getCacheFactory();
            }

        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
         */
        public PartitionedCache getService()
            {
            return (PartitionedCache) get_Module();
            }

        // Accessor for the property "Storage"
        /**
         * Getter for property Storage.<p>
         * The Storage this BackupManager belongs to.
         */
        public Storage getStorage()
            {
            return (Storage) get_Parent();
            }

        // From interface: com.tangosol.net.BackingMapManager
        public com.tangosol.net.security.StorageAccessAuthorizer getStorageAccessAuthorizer(String sName)
            {
            return null;
            }

        // Accessor for the property "Type"
        /**
         * Getter for property Type.<p>
         * One of the BACKUP_*, KEY_INDEX or FACTORY_ADAPTER constants.
         */
        public int getType()
            {
            return __m_Type;
            }

        // From interface: com.tangosol.net.BackingMapManager
        public void init(com.tangosol.net.BackingMapManagerContext context)
            {
            }

        // From interface: com.tangosol.net.BackingMapManager
        /**
         * Instantiate a backup map based on the already parsed properties.
         */
        public java.util.Map instantiateBackingMap(String sName)
            {
            // import com.tangosol.io.nio.BinaryMap as com.tangosol.io.nio.BinaryMap;
            // import com.tangosol.io.nio.MappedBufferManager;
            // import com.tangosol.net.BackingMapManager;
            // import com.tangosol.net.DefaultConfigurableCacheFactory as com.tangosol.net.DefaultConfigurableCacheFactory;
            // import com.tangosol.net.ExtensibleConfigurableCacheFactory$Manager as com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager;
            // import com.tangosol.net.DefaultConfigurableCacheFactory$CacheInfo as com.tangosol.net.DefaultConfigurableCacheFactory.CacheInfo;
            // import com.tangosol.run.xml.XmlElement;
            // import com.tangosol.run.xml.SimpleElement;
            // import com.tangosol.util.Base;
            // import com.tangosol.util.SafeHashMap;
            // import java.util.HashMap;
            // import java.util.Map;

            PartitionedCache           service  = getService();
            BackingMapManager mgr      = service.getBackingMapManager();

            if (mgr instanceof com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager)
                {
                // COH-7808:  In the case of a partitioned backing map, the
                // PartitionedSplittingBackingMap passes a cache name that has $Backup
                // appended.  This breaks the com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager since it needs the cache name
                // to look up the scheme in the CacheConfig. Use the original cache
                // name instead.  See instantiateBackupMap for
                // more information.
                return ((com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager) mgr).instantiateBackupMap(getCacheName());
                }

            Map map = null;

            switch (getType())
                {
                case BACKUP_CUSTOM:
                    try
                        {
                        map = (Map) getBackupClass().newInstance();
                        }
                    catch (Exception e)
                        {
                        _trace("Failed to create a 'custom' backup map: " + e +
                               "\nusing the 'on-heap' type instead", 1);
                        }
                    break;

                case BACKUP_FILE:
                    map = new com.tangosol.io.nio.BinaryMap(new MappedBufferManager(
                            getBackupInitSize(), getBackupMaxSize(),
                            getBackupDir()));
                    break;

                case BACKUP_SCHEME:
                    try
                        {
                        // use the "main name" instead of a synthetic one
                        com.tangosol.net.DefaultConfigurableCacheFactory       factory   = getDefaultFactory();
                        com.tangosol.net.DefaultConfigurableCacheFactory.CacheInfo  info      = factory.findSchemeMapping(getCacheName());
                        XmlElement xmlScheme = factory.resolveScheme(
                                new com.tangosol.net.DefaultConfigurableCacheFactory.CacheInfo(sName, getBackupScheme(), info.getAttributes()));

                        // allow "automatic" backup processing
                        xmlScheme.addAttribute("target").setString("backup");

                        // for a partitioned scheme, the actual partitioned map is created by
                        // Storage.instantiateBackupMap and for each partition, the BackingManager
                        // will be used to create the map. To stop the recursion the partitioned
                        // element will have to be set to false
                        xmlScheme.addAttribute("partitioned").setString("false");

                        map = factory.configureBackingMap(info, xmlScheme,
                                                          service.getBackingMapContext(), null, getStorage().getConfiguredBackupListeners());
                        }
                    catch (ClassCastException e)
                        {
                        _trace("\"Scheme\" backup is only supported by the " +
                               "com.tangosol.net.DefaultConfigurableCacheFactory" +
                               "\nusing the 'on-heap' type instead", 1);
                        }
                    break;

                case BACKUP_FLASHJOURNAL:
                case BACKUP_RAMJOURNAL:
                {
                String sScheme = getType() == BACKUP_FLASHJOURNAL
                                 ? "flashjournal-scheme" : "ramjournal-scheme";
                try
                    {
                    com.tangosol.net.DefaultConfigurableCacheFactory       factory   = getDefaultFactory();
                    XmlElement xmlScheme = new SimpleElement(sScheme);
                    com.tangosol.net.DefaultConfigurableCacheFactory.CacheInfo  info      = new com.tangosol.net.DefaultConfigurableCacheFactory.CacheInfo(sName, sScheme, null);

                    // allow "automatic" backup processing
                    xmlScheme.addAttribute("target").setString("backup");

                    // for a partitioned scheme, the actual partitioned map is created by
                    // Storage.instantiateBackupMap and for each partition, the BackingManager
                    // will be used to create the map. To stop the recursion the partitioned
                    // element will have to be set to false
                    xmlScheme.addAttribute("partitioned").setString("false");

                    map = factory.configureBackingMap(info, xmlScheme,
                                                      service.getBackingMapContext(), null, getStorage().getConfiguredBackupListeners());
                    }
                catch (Exception e)
                    {
                    _trace("Failed to create backup map of type " + sScheme + " due to exception " + e, 1);
                    }
                }
                break;

                case BACKUP_ONHEAP:
                    break;

                default:
                    throw new IllegalStateException("Unknown type: " + getType());
                }

            return map == null ? new SafeHashMap() : map;
            }

        // From interface: com.tangosol.net.BackingMapManager
        public boolean isBackingMapPersistent(String sName)
            {
            return false;
            }

        // From interface: com.tangosol.net.BackingMapManager
        public boolean isBackingMapSlidingExpiry(String sName)
            {
            return false;
            }

        // Accessor for the property "Partitioned"
        /**
         * Getter for property Partitioned.<p>
         * Specifies wether the backup backing map for the corresponding
         * Storage should be partitioned. In most cases the answer is "yes"
         * and this is provided as a backward-compatibility safety net.
         */
        public boolean isPartitioned()
            {
            return __m_Partitioned;
            }

        /**
         * This method parses the "backup-storage" element for an enclosing
         * "distributed-scheme", storing the result as properties in this
         * component.
         */
        public void parseConfiguration()
            {
            // import com.tangosol.coherence.config.Config;
            // import com.tangosol.net.BackingMapManager;
            // import com.tangosol.net.DefaultConfigurableCacheFactory as com.tangosol.net.DefaultConfigurableCacheFactory;
            // import com.tangosol.net.DefaultConfigurableCacheFactory$CacheInfo as com.tangosol.net.DefaultConfigurableCacheFactory.CacheInfo;
            // import com.tangosol.net.ExtensibleConfigurableCacheFactory$Manager as com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager;
            // import com.tangosol.run.xml.XmlElement;
            // import com.tangosol.util.Base;
            // import java.io.File;
            // import java.util.Map;

            String            sCacheName = getCacheName();
            BackingMapManager mgr        = getService().getBackingMapManager();

            // ECCF uses BackupMapManager which is already configured
            if (mgr instanceof com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager)
                {
                setPartitioned(((com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager) mgr).isBackupPartitioned(sCacheName));
                return;
                }

            com.tangosol.net.DefaultConfigurableCacheFactory       factory       = getDefaultFactory();
            com.tangosol.net.DefaultConfigurableCacheFactory.CacheInfo  info          = factory.findSchemeMapping(sCacheName);
            XmlElement xmlCache      = factory.resolveScheme(info);
            XmlElement xmlBackingMap = factory.resolveBackingMapScheme(info, xmlCache);

            // parse the "backup-storage" element
            XmlElement xmlBackup = xmlCache.getElement("backup-storage");
            if (xmlBackup != null)
                {
                String sType = xmlBackup.getSafeElement("type").getString("on-heap");

                if (sType.equals("on-heap"))
                    {
                    setType(BackingManager.BACKUP_ONHEAP);
                    }
                else if (sType.equals("file-mapped"))
                    {
                    long cbInit = Base.parseMemorySize(
                            xmlBackup.getSafeElement("initial-size").getString("1"),    Base.POWER_M);
                    long cbMax  = Base.parseMemorySize(
                            xmlBackup.getSafeElement("maximum-size").getString("1024"), Base.POWER_M);

                    // Bounds check:
                    // 1 <= cbInitSize <= cbMaxSize <= Integer.MAX_VALUE - 1023
                    // (Integer.MAX_VALUE - 1023 is the largest integer multiple of 1024)
                    int cbMaxSize  = (int) Math.min(Math.max(cbMax, 1L), (long) Integer.MAX_VALUE - 1023);
                    int cbInitSize = (int) Math.min(Math.max(cbInit, 1L), cbMaxSize);

                    setBackupInitSize(cbInitSize);
                    setBackupMaxSize(cbMaxSize);
                    setPartitioned(false);

                    String sPath = xmlBackup.getSafeElement("directory").getString();
                    if (sPath.length() > 0)
                        {
                        File dir = new File(sPath);
                        if (dir.isDirectory())
                            {
                            setBackupDir(dir);
                            }
                        }
                    setType(BackingManager.BACKUP_FILE);
                    }
                else if (sType.equals("custom"))
                    {
                    String sBackupClass = xmlBackup.getSafeElement("class-name").getString();
                    try
                        {
                        Class clzBackup = getService().getContextClassLoader().loadClass(sBackupClass);

                        _assert(Map.class.isAssignableFrom(clzBackup));
                        setBackupClass(clzBackup);
                        }
                    catch (Exception e)
                        {
                        throw Base.ensureRuntimeException(e,
                                                          "Invalid backup class: " + sBackupClass);
                        }
                    setType(BackingManager.BACKUP_CUSTOM);
                    }
                else if (sType.equals("scheme"))
                    {
                    String sSchemeName = xmlBackup.getSafeElement("scheme-name").getString();
                    setBackupScheme(sSchemeName);
                    setType(BackingManager.BACKUP_SCHEME);
                    }
                else
                    {
                    throw new IllegalArgumentException("Unknown backup type: " + sType);
                    }
                }
            else
                {
                // on-heap is default, but if the backing map is ramjournal or flashjournal,
                // set the backup map to be flashjournal, by default.
                setType(BackingManager.BACKUP_ONHEAP);

                if (xmlBackingMap == null)
                    {
                    // in case there is no backing map configuration - do nothing
                    }
                else
                    {
                    String sName = xmlBackingMap.getName();
                    if (sName.equals("ramjournal-scheme"))
                        {
                        setType(Boolean.parseBoolean(Config.getProperty("coherence.journal.backuptoflash", "true")) ?
                                BackingManager.BACKUP_FLASHJOURNAL : BackingManager.BACKUP_RAMJOURNAL);
                        }
                    else if (sName.equals("flashjournal-scheme"))
                        {
                        setType(BackingManager.BACKUP_FLASHJOURNAL);
                        }
                    }
                }
            }

        // From interface: com.tangosol.net.BackingMapManager
        /**
         * Releases a backup map.
         */
        public void releaseBackingMap(String sName, java.util.Map map)
            {
            // import com.tangosol.io.nio.BinaryMap as com.tangosol.io.nio.BinaryMap;
            // import com.tangosol.io.nio.ByteBufferManager;
            // import com.tangosol.io.nio.MappedBufferManager;
            // import com.tangosol.net.BackingMapManager;
            // import com.tangosol.net.ExtensibleConfigurableCacheFactory$Manager as com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager;
            // import com.tangosol.util.SafeHashMap;

            BackingMapManager mgr = getService().getBackingMapManager();

            if (mgr instanceof com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager)
                {
                // the backup manager will catch the exception and trace
                ((com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager) mgr).releaseBackupMap(sName, map,
                                                                                                     getStorage().getConfiguredBackupListeners());

                return;
                }

            // check for the actual class first, since we could already
            // reported a problem during "instantiateBackupMap()"
            if (!(map instanceof SafeHashMap))
                {
                try
                    {
                    switch (getType())
                        {
                        case BACKUP_CUSTOM:
                                /* consider an Xml element describing a "close" method
                                if (map instanceof XmlConfigurable)
                                    {
                                    XmlElement xmlBackup = ((XmlConfigurable) map).getConfig();
                                    }
                                */
                            break;

                        case BACKUP_FILE:
                            map.clear();
                            try
                                {
                                ByteBufferManager bufferMgr = ((com.tangosol.io.nio.BinaryMap) map).getBufferManager();
                                ((MappedBufferManager) bufferMgr).close();
                                }
                            catch (ClassCastException ignored) {}
                            break;

                        case BACKUP_OFFHEAP:
                            map.clear();
                            break;

                        case BACKUP_SCHEME:
                        case BACKUP_RAMJOURNAL:
                        case BACKUP_FLASHJOURNAL:
                            try
                                {
                                getDefaultFactory().release(
                                        map, getStorage().getConfiguredBackupListeners());
                                }
                            catch (ClassCastException ignored) {}
                            break;

                        case BACKUP_ONHEAP:
                            break;

                        default:
                            throw new IllegalStateException("Unknown type: " + getType());
                        }
                    }
                catch (Exception e)
                    {
                    _trace("Failed to invalidate backing map: " + e, 2);
                    }
                }
            }

        // Accessor for the property "BackupClass"
        /**
         * Setter for property BackupClass.<p>
         * Specifies the class for custom backup storage implementation.
         */
        protected void setBackupClass(Class clzBackupClass)
            {
            __m_BackupClass = clzBackupClass;
            }

        // Accessor for the property "BackupDir"
        /**
         * Setter for property BackupDir.<p>
         * Specifies the directory for file-mapped backup. Applicable only
         * if the BackupType is BACKUP_FILE.
         */
        protected void setBackupDir(java.io.File dir)
            {
            __m_BackupDir = dir;
            }

        // Accessor for the property "BackupInitSize"
        /**
         * Setter for property BackupInitSize.<p>
         * Specifies the initial buffer size. Applicable only if the
         * BackupType is BACKUP_OFFHEAP or BACKUP_FILE.
         *
         * Default value is 1MB.
         */
        protected void setBackupInitSize(int cInitSize)
            {
            __m_BackupInitSize = cInitSize;
            }

        // Accessor for the property "BackupMaxSize"
        /**
         * Setter for property BackupMaxSize.<p>
         * Specifies the maximum buffer size. Applicable only if the
         * BackupType is BACKUP_OFFHEAP or BACKUP_FILE.
         *
         * Default value is 1GB.
         */
        protected void setBackupMaxSize(int cMaxSize)
            {
            __m_BackupMaxSize = cMaxSize;
            }

        // Accessor for the property "BackupScheme"
        /**
         * Setter for property BackupScheme.<p>
         * Specifies the scheme name for backup storage implementation.
         */
        protected void setBackupScheme(String sBackupScheme)
            {
            __m_BackupScheme = sBackupScheme;
            }

        // Accessor for the property "CacheName"
        /**
         * Setter for property CacheName.<p>
         * The cache name for the Storage component this manager is
         * associated with. We could not use the getStorage().getCacheName()
         * due to the initialization ordering (COH-3486).
         */
        public void setCacheName(String sCacheName)
            {
            __m_CacheName = sCacheName;
            }

        // Accessor for the property "Partitioned"
        /**
         * Setter for property Partitioned.<p>
         * Specifies wether the backup backing map for the corresponding
         * Storage should be partitioned. In most cases the answer is "yes"
         * and this is provided as a backward-compatibility safety net.
         */
        public void setPartitioned(boolean fPartitioned)
            {
            __m_Partitioned = fPartitioned;
            }

        // Accessor for the property "Type"
        /**
         * Setter for property Type.<p>
         * One of the BACKUP_*, KEY_INDEX or FACTORY_ADAPTER constants.
         */
        public void setType(int iType)
            {
            __m_Type = iType;
            }

        // Declared at the super level
        public String toString()
            {
            // import com.tangosol.net.BackingMapManager;
            // import com.tangosol.net.ExtensibleConfigurableCacheFactory$Manager as com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager;
            // import com.tangosol.util.ClassHelper;

            Class             clz = get_CLASS();
            BackingMapManager mgr = getService().getBackingMapManager();

            if (mgr instanceof com.tangosol.net.ExtensibleConfigurableCacheFactory.Manager)
                {
                clz = mgr.getClass();
                return clz.getName();
                }

            return ClassHelper.getSimpleName(clz);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$BackingMapAction

    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BackingMapAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----

        // Default constructor
        public BackingMapAction()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public BackingMapAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new BackingMapAction();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$BackingMapAction".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            return ((Storage) get_Parent()).getBackingMap();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$BinaryEntry

    /**
     * An wrapper around a Binary entry (both Key and Value are Binary
     * objects) that lazily converts and caches the converted value. It is
     * also optimized to be used for InvocableMap operations.
     *
     * Since the BinaryEntry components could be created quite frequently,
     * we don't use the standard _newChild() approach, but just "new" it.
     *
     * Additionally, the following fields inherited from Component are
     * overloaded:
     *
     * _Feed     OriginalValue (object form)
     * _Sink      OriginalBinaryValue (serialized form)
     * _Order   Expiry
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BinaryEntry
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.io.SerializationSupport,
                       com.tangosol.util.BinaryEntry,
                       com.tangosol.util.MapTrigger.Entry
        {
        // ---- Fields declarations ----

        /**
         * Property ACCESS_MASK
         *
         * The mask for ACCESS_* values.
         *
         * 0xE000 (ACCESS_READ | ACCESS_WRITE | 0x8000)
         */
        public static final int ACCESS_MASK = 57344;

        /**
         * Property ACCESS_READ
         *
         * Indicates that READ access has been granted.
         *
         * 0x2000
         */
        public static final int ACCESS_READ = 8192;

        /**
         * Property ACCESS_READ_ANY
         *
         * Indicates that READ_ANY access has been granted. Implies READ
         * access.
         *
         * 0xA000 (ACCESS_READ | 0x8000)
         */
        public static final int ACCESS_READ_ANY = 40960;

        /**
         * Property ACCESS_WRITE
         *
         * Indicates that WRITE access has been granted. Implies READ
         * access.
         *
         * 0x4000
         */
        public static final int ACCESS_WRITE = 16384;

        /**
         * Property ACCESS_WRITE_ANY
         *
         * Indicates that WRITE access has been granted. Implies READ_ANY
         * access.
         *
         * 0xC000 (ACCESS_WRITE | 0x8000)
         */
        public static final int ACCESS_WRITE_ANY = 49152;

        /**
         * Property BinaryKey
         *
         * Binary key.
         */
        private com.tangosol.util.Binary __m_BinaryKey;

        /**
         * Property BinaryValue
         *
         * Binary value.
         */
        private com.tangosol.util.Binary __m_BinaryValue;

        /**
         * Property ConvertedKey
         *
         * Converted key.
         */
        private transient Object __m_ConvertedKey;

        /**
         * Property ConvertedValue
         *
         * Converted value.
         */
        private transient Object __m_ConvertedValue;

        /**
         * Property EXPIRE_UPDATED
         *
         * Indicates that the entry's expire value has been altered.
         */
        protected static final int EXPIRE_UPDATED = 4;

        /**
         * Property ExpiryValue
         *
         * Expiry value.
         */
        private long __m_ExpiryValue;

        /**
         * Property KEY_CONVERTED
         *
         * Indicates that the conversion has already been applied to the
         * key.
         */
        protected static final int KEY_CONVERTED = 2;

        /**
         * Property ORIG_MASK
         *
         * The mask for the ORIG_* flags.
         *
         * 0x1800
         */
        protected static final int ORIG_MASK = 6144;

        /**
         * Property ORIG_NONE
         *
         * Indicates that the value did not exist before the start of an
         * invocation.
         *
         * 0x0800
         */
        protected static final int ORIG_NONE = 2048;

        /**
         * Property ORIG_PRESENT
         *
         * Indicates that the value existed before the start of an
         * invocation.
         *
         * 0x1000
         */
        protected static final int ORIG_PRESENT = 4096;

        /**
         * Property State
         *
         * A combination of flags reflecting the state of this BinaryEntry.
         * The reason we keep it as a single value is to decrease the
         * memory footprint.
         *
         * As of Coherence 12.2.1 this property is optimized to use
         * _StateAux and as a result is limited to 30 bits.
         *
         * The supported flags are KEY_*, ORIG_*, VALUE_*, ACCESS_*
         * constants.
         *
         * @functional
         */

        /**
         * Property STATE_MASK
         *
         * The mask for all values handled by the State property.
         *
         * 0xFFFF
         */
        public static final int STATE_MASK = 65535;

        /**
         * Property VALUE_CONVERTED
         *
         * Indicates that the conversion has already been applied to the
         * value.
         */
        protected static final int VALUE_CONVERTED = 8;

        /**
         * Property VALUE_FORCE_EXTRACT
         *
         * Indicates that any indexes present may not yet have been
         * synchronised with the current value so should not be used to
         * shortcut value extraction.
         */
        protected static final int VALUE_FORCE_EXTRACT = 1024;

        /**
         * Property VALUE_LOADED
         *
         * Indicates that the original value was not present, but the value
         * was loaded (most probably read-through) during the very first
         * getValue() or getBinaryValue() call.
         */
        protected static final int VALUE_LOADED = 16;

        /**
         * Property VALUE_NONE
         *
         * Indicates that the BinaryValue was explicitly set to null (a
         * value does not exist).
         */
        protected static final int VALUE_NONE = 32;

        /**
         * Property VALUE_READONLY
         *
         * Indicates that the value cannot be modified.
         */
        protected static final int VALUE_READONLY = 64;

        /**
         * Property VALUE_REMOVED
         *
         * Indicates that the value has been removed.
         */
        protected static final int VALUE_REMOVED = 128;

        /**
         * Property VALUE_STALE
         *
         * Indicates that the binary value was set prior to having a lock
         * on the entry thus may be stale.
         */
        protected static final int VALUE_STALE = 1;

        /**
         * Property VALUE_SYNTHETIC
         *
         * Indicates that the value modification event has to be marked as
         * synthetic.
         */
        protected static final int VALUE_SYNTHETIC = 256;

        /**
         * Property VALUE_UPDATED
         *
         * Indicates that the value (either converted or binary) has been
         * updated.
         */
        protected static final int VALUE_UPDATED = 512;

        /**
         * Property VERSION_MASK
         *
         * The mask for the VersionTrint value.
         *
         * 0x3FFF_0000
         */
        public static final int VERSION_MASK = 1073676288;

        /**
         * Property VERSION_OFFSET
         *
         * The bit offset for the IndexVersion value.
         */
        public static final int VERSION_OFFSET = 16;

        /**
         * Property VersionTrint
         *
         * The "trint14" value representing the version of the index as it
         * was before the binary value for this entry was read.
         *
         * This value is held in the bits 16-29 of the _StateAux property.
         *
         * @functional
         */

        // Default constructor
        public BinaryEntry()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public BinaryEntry(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                set_Order(-2.0F);
                setExpiryValue(-2L);
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
            return new BinaryEntry();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$BinaryEntry".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        /**
         * Check the access authorization for this entry.
         *
         * @param nAccess one of ACCESS_READ or ACCESS_WRITE
         */
        protected void checkAccess(int nAccess)
            {
            // import com.tangosol.net.security.StorageAccessAuthorizer as com.tangosol.net.security.StorageAccessAuthorizer;

            Storage storage = getStorage();
            if (storage == null)
                {
                // check during entry initialization
                return;
                }

            com.tangosol.net.security.StorageAccessAuthorizer authorizer = storage.getAccessAuthorizer();
            if (authorizer != null)
                {
                int nState = getState();

                if ((nState & nAccess) == nAccess)
                    {
                    // the access has already been authorized for this entry
                    return;
                    }

                PartitionedCache.InvocationContext ctxInvoke = getService().getInvocationContext();
                if (ctxInvoke == null || ctxInvoke.getAccessReason() == 0)
                    {
                    // OOB access; this must be an internal use
                    return;
                    }

                if ((ctxInvoke.getAccessGranted() & nAccess) == nAccess
                    && ctxInvoke.getAccessStorage() == storage)
                    {
                    // this access has already been authorized at the global level
                    return;
                    }

                // allow the authorizer to access the entry
                setState(nState | nAccess);

                switch (nAccess)
                    {
                    case ACCESS_READ:
                        authorizer.checkRead(this, ctxInvoke.getAccessSubject(), ctxInvoke.getAccessReason());
                        break;

                    case ACCESS_WRITE:
                        authorizer.checkWrite(this, ctxInvoke.getAccessSubject(), ctxInvoke.getAccessReason());
                        break;

                    default:
                        throw new IllegalArgumentException("Invalid entry access: " + nAccess);
                    }
                }
            }

        /**
         * Verify that this entry is mutable.
         */
        protected void checkMutable()
            {
            if (isReadOnly())
                {
                throw new UnsupportedOperationException(
                        "Read-only entry does not allow Map modifications");
                }
            }

        /**
         * Check if the value extracted from the forward index could be used
         * by this entry.
         */
        protected boolean checkVersion()
            {
            // import com.tangosol.net.internal.StorageVersion;
            // import com.tangosol.net.internal.Trint;

            int nTrint = getVersionTrint();
            if (nTrint == 0)
                {
                // no version info
                return true;
                }

            Storage        storage  = getStorage();
            StorageVersion version  = storage.getVersion();
            long           lCurrent = version.getSubmittedVersion();
            long           lVersion = Trint.translateTrint14(nTrint, lCurrent);

            return lVersion == lCurrent ||
                   !version.isPartitionModified(lVersion, storage.getService().getKeyPartition(getBinaryKey()));
            }

        // Declared at the super level
        /**
         * Compares this object with the specified object for order.
         * Returns a negative integer, zero, or a positive integer as this
         * object is less than, equal to, or greater than the specified
         * object.
         *
         * @param o  the Object to be compared.
         * @return  a negative integer, zero, or a positive integer as this
         * object is less than, equal to, or greater than the specified
         * object.
         *
         * @throws ClassCastException if the specified object's type
         * prevents it from being compared to this Object.
         */
        public int compareTo(Object o)
            {
            // import com.tangosol.util.comparator.SafeComparator;

            return SafeComparator.compareSafe(null,
                                              getBinaryKey(), ((BinaryEntry) o).getBinaryKey());
            }

        /**
         * Prevent the conversion during both getKey() and getValue
         * operations. This method should be called  before an entry is
         * about to be returned as a result of a request. Both serialization
         * (see $QueryResponse.write) or in-process shortcut will use the
         * Map.Entry API to retrieve the Binary content.
         */
        public void disableConversion()
            {
            }

        /**
         * Make the entry read-only.
         */
        public void ensureReadOnly()
            {
            setState(getState() | VALUE_READONLY);
            }

        /**
         * Make the entry read-write.
         */
        public void ensureWriteable()
            {
            setState(getState() & ~VALUE_READONLY);
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        // Declared at the super level
        public boolean equals(Object obj)
            {
            // import com.tangosol.util.Base;

            // Note: this equals implementation does not obey the Map$Entry contract
            return obj instanceof BinaryEntry
                   && Base.equals(getBinaryKey(), ((BinaryEntry) obj).getBinaryKey())
                   && Base.equals(getStorage(), ((BinaryEntry) obj).getStorage());
            }

        // From interface: com.tangosol.util.BinaryEntry
        /**
         * Update the entry with the specified expiry delay.
         */
        public void expire(long cMillis)
            {
            // import com.tangosol.net.cache.CacheMap;

            checkMutable();

            setExpiryValue(cMillis >= 0 ? cMillis : CacheMap.EXPIRY_NEVER);

            setState(getState() | EXPIRE_UPDATED);
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        public Object extract(com.tangosol.util.ValueExtractor extractor)
            {
            // import com.tangosol.util.InvocableMapHelper;
            // import com.tangosol.util.MapIndex;

            int nState = getState();
            if ((nState & (VALUE_UPDATED | VALUE_REMOVED | VALUE_LOADED | VALUE_FORCE_EXTRACT)) == 0)
                {
                MapIndex index = (MapIndex) getStorage()
                        .getPartitionIndexMap(getStorage().getManagerContext().getKeyPartition(getBinaryKey()))
                        .get(extractor);
                if (index != null)
                    {
                    Object oValue = index.get(getBinaryKey());
                    if (oValue != MapIndex.NO_VALUE)
                        {
                        if (checkVersion())
                            {
                            checkAccess(ACCESS_READ);

                            return oValue;
                            }

                        // cache the checkVersion() result
                        setForceExtract(true);
                        }
                    }
                }

            return InvocableMapHelper.extractFromEntry(extractor, this);
            }

        public static String formatState(int nState)
            {
            StringBuilder sb = new StringBuilder();

            if ((nState & KEY_CONVERTED) != 0)
                {
                sb.append(" | KEY_CONVERTED");
                }
            if ((nState & VALUE_CONVERTED) != 0)
                {
                sb.append(" | VALUE_CONVERTED");
                }
            if ((nState & VALUE_FORCE_EXTRACT) != 0)
                {
                sb.append(" | VALUE_FORCE_EXTRACT");
                }
            if ((nState & VALUE_NONE) != 0)
                {
                sb.append(" | VALUE_NONE");
                }
            if ((nState & VALUE_READONLY) != 0)
                {
                sb.append(" | VALUE_READONLY");
                }
            if ((nState & VALUE_REMOVED) != 0)
                {
                sb.append(" | VALUE_REMOVED");
                }
            if ((nState & VALUE_SYNTHETIC) != 0)
                {
                sb.append(" | VALUE_SYNTHETIC");
                }
            if ((nState & EXPIRE_UPDATED) != 0)
                {
                sb.append(" | EXPIRE_UPDATED");
                }
            if ((nState & VALUE_UPDATED) != 0)
                {
                sb.append(" | VALUE_UPDATED");
                }
            if ((nState & VALUE_LOADED) != 0)
                {
                sb.append(" | VALUE_LOADED");
                }
            if ((nState & ORIG_PRESENT) != 0)
                {
                sb.append(" | ORIG_PRESENT");
                }
            if ((nState & ORIG_NONE) != 0)
                {
                sb.append(" | ORIG_NONE");
                }

            return sb.length() > 0 ? sb.substring(3) : "";
            }

        // From interface: com.tangosol.util.BinaryEntry
        public com.tangosol.util.ObservableMap getBackingMap()
            {
            return getStorage().getBackingMap();
            }

        // From interface: com.tangosol.util.BinaryEntry
        // Accessor for the property "BackingMapContext"
        /**
         * Getter for property BackingMapContext.<p>
         * The BackingMapContext.
         */
        public com.tangosol.net.BackingMapContext getBackingMapContext()
            {
            PartitionedCache.InvocationContext ctx     = getInvocationContext();
            Storage           storage = getStorage();

            return ctx == null ? storage : ctx.ensureBackingMapContext(storage);
            }

        protected com.tangosol.util.ObservableMap getBackingMapInternal()
            {
            // import com.tangosol.net.security.DoAsAction;
            // import com.tangosol.util.ObservableMap;
            // import java.security.AccessController;

            if (System.getSecurityManager() == null)
                {
                return getStorage().getBackingMap();
                }

            return (ObservableMap) AccessController.doPrivileged(getStorage().getBackingMapAction());
            }

        // From interface: com.tangosol.util.BinaryEntry
        // Accessor for the property "BinaryKey"
        /**
         * Getter for property BinaryKey.<p>
         * Binary key.
         */
        public com.tangosol.util.Binary getBinaryKey()
            {
            return __m_BinaryKey;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // Accessor for the property "BinaryValue"
        /**
         * Getter for property BinaryValue.<p>
         * Binary value.
         */
        public com.tangosol.util.Binary getBinaryValue()
            {
            // import com.tangosol.util.Binary;

            Binary binValue = __m_BinaryValue;
            if (binValue == null)
                {
                int nState = getState();

                if ((nState & VALUE_NONE) == 0)
                    {
                    if ((nState & VALUE_CONVERTED) != 0)
                        {
                        binValue = (Binary) getStorage().
                                getConverterValueDown().convert(getConvertedValue());
                        }
                    else if (isOriginalPresent())
                        {
                        binValue = getOriginalBinaryValue();
                        }
                    else
                        {
                        // the original value does not exist;
                        // let's try to force the read-through
                        binValue = (Binary) getBackingMapInternal().get(getBinaryKey());
                        if (binValue != null)
                            {
                            // this should be seen as an insert
                            setState(getState() | VALUE_LOADED);
                            }
                        }

                    if (binValue == null)
                        {
                        setState(getState() | VALUE_NONE);
                        }
                    else
                        {
                        setBinaryValue(binValue);
                        }
                    }
                }

            // the authorizer should be able to see the newly read value
            checkAccess(ACCESS_READ);

            return binValue;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // Accessor for the property "Context"
        /**
         * Getter for property Context.<p>
         */
        public com.tangosol.net.BackingMapManagerContext getContext()
            {
            // import com.tangosol.net.BackingMapManagerContext as com.tangosol.net.BackingMapManagerContext;

            PartitionedCache.InvocationContext ctx = getInvocationContext();

            if (ctx == null)
                {
                return (com.tangosol.net.BackingMapManagerContext) getService().getBackingMapContext();
                }

            // make sure the BackingMapContext for this entry's storage is cached
            // by the InvocationContext (see COH-4090)
            // Note: in general, we should not call $BinrayEntry.getContext() ourselves
            //       (outside of the invocation context)
            ctx.ensureBackingMapContext(getStorage());

            return ctx;
            }

        // Accessor for the property "ConvertedKey"
        /**
         * Getter for property ConvertedKey.<p>
         * Converted key.
         */
        protected Object getConvertedKey()
            {
            return __m_ConvertedKey;
            }

        // Accessor for the property "ConvertedValue"
        /**
         * Getter for property ConvertedValue.<p>
         * Converted value.
         */
        protected Object getConvertedValue()
            {
            return __m_ConvertedValue;
            }

        // From interface: com.tangosol.util.BinaryEntry
        /**
         * Return the expiry time for this Entry.
         */
        public long getExpiry()
            {
            // import com.tangosol.net.cache.CacheMap;
            // import com.tangosol.net.cache.ConfigurableCacheMap;

            long cExpiry;
            if (isExpiryInitialized())
                {
                cExpiry = getExpiryValue();
                }
            else
                {
                // uninitialized
                if (isValueChanged() || isValueRemoved() || !isOriginalPresent())
                    {
                    // value has been changed or removed (and #expire has not been
                    // called), or the original is not present in the backing-map;
                    // the only thing we can do is set it to the "default"
                    setExpiryValue(cExpiry = CacheMap.EXPIRY_DEFAULT);
                    }
                else
                    {
                    // this will extract the expiry; see #getOriginalBinaryValue
                    getOriginalBinaryValue();

                    cExpiry = getExpiryValue(); // cExpiry is a timestamp now
                    }
                }

            // cExpiry could be DEFAULT (0), NEVER (-1), a user-supplied expiry offset,
            // or an expiry timestamp (in cluster-time)
            if (cExpiry == CacheMap.EXPIRY_DEFAULT)
                {
                // set the expiry to the default (expiry-delay) set on the backing map
                ConfigurableCacheMap mapCCM = getStorage().getBackingConfigurableCache();
                if (mapCCM != null)
                    {
                    cExpiry = mapCCM.getExpiryDelay();
                    }
                }
            else if (cExpiry != CacheMap.EXPIRY_NEVER && !isExpireChanged())
                {
                long ldtNow = getService().getClusterTime();
                cExpiry = Math.max(1L, cExpiry - ldtNow);  // elapsed expiry becomes 1
                }

            return cExpiry;
            }

        // Accessor for the property "ExpiryValue"
        /**
         * Getter for property ExpiryValue.<p>
         * Expiry value.
         */
        private long getExpiryValue()
            {
            return __m_ExpiryValue;
            }

        protected PartitionedCache.InvocationContext getInvocationContext()
            {
            PartitionedCache.InvocationContext ctx = getService().getInvocationContext();

            // if the entry is marked as "read-only", don't allow access to a mutable context
            return ctx != null && (ctx.isReadOnly() || !isReadOnly() || isValueLoaded()) ? ctx : null;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        public Object getKey()
            {
            int nState = getState();

            if ((nState & KEY_CONVERTED) != 0)
                {
                return getConvertedKey();
                }

            Object oKey = getStorage().getConverterUp().convert(getBinaryKey());
            setConvertedKey(oKey);
            setState(nState | KEY_CONVERTED);

            return oKey;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // Accessor for the property "OriginalBinaryValue"
        /**
         * Getter for property OriginalBinaryValue.<p>
         * Original Binary value.
         *
         * 0x1000
         */
        public com.tangosol.util.Binary getOriginalBinaryValue()
            {
            // import com.tangosol.io.ReadBuffer;
            // import com.tangosol.net.cache.CacheMap;
            // import com.tangosol.net.cache.ConfigurableCacheMap;
            // import com.tangosol.net.cache.ConfigurableCacheMap$Entry as com.tangosol.net.cache.ConfigurableCacheMap.Entry;
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;

            // Note: the isOriginalPresent() check is necessary to prevent a
            //       premature load if the backing-map has read-through semantics

            Object  binValue   = get_Sink();
            boolean fExpirySet = isExpiryInitialized();
            long    ldtExpiry  = fExpirySet ? getExpiryValue() : CacheMap.EXPIRY_DEFAULT;

            if (binValue != null && fExpirySet)
                {
                // loading the value or expiry is unnecessary
                return (Binary) binValue;
                }
            else if (isOriginalPresent())
                {
                Binary               binKey  = getBinaryKey();
                Storage             storage = getStorage();
                ConfigurableCacheMap ccm     = storage.getBackingConfigurableCache();

                if (ccm == null)
                    {
                    if (binValue == null)
                        {
                        binValue = getBackingMapInternal().get(binKey);
                        }

                    if (binValue != null && !fExpirySet)
                        {
                        // the backing-map is not a CCM; we must rely on the value decoration
                        ldtExpiry = com.tangosol.util.ExternalizableHelper.decodeExpiry((Binary) binValue);  // a special value or decorated cluster-time
                        }
                    }
                else
                    {
                    com.tangosol.net.cache.ConfigurableCacheMap.Entry entry = ccm.getCacheEntry(binKey);
                    if (entry != null)
                        {
                        if (binValue == null)
                            {
                            Object oValue = entry.getValue();
                            // COH-6336: compensate for the possibility of custom backing
                            //           maps not implementing getEntry() correctly
                            binValue = oValue instanceof ReadBuffer ? oValue : ccm.get(binKey);
                            }

                        if (!fExpirySet)
                            {
                            ldtExpiry = entry.getExpiryMillis(); // 0 (never), or a local time
                            ldtExpiry = ldtExpiry == 0L
                                        ? CacheMap.EXPIRY_NEVER
                                        : getService().getClusterService().calcTimestamp(ldtExpiry);
                            }
                        }
                    }

                // cache the binary original value using the _Sink property
                set_Sink(binValue);
                setExpiryValue(ldtExpiry);
                }

            return (Binary) binValue;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        // Accessor for the property "OriginalValue"
        /**
         * Getter for property OriginalValue.<p>
         * Original value (converted).
         */
        public Object getOriginalValue()
            {
            // cache the converted original value using the _Feed property
            Object oValue = get_Feed();
            if (oValue == null)
                {
                oValue = getStorage().getConverterUp().convert(getOriginalBinaryValue());
                set_Feed(oValue);
                }
            return oValue;
            }

        // From interface: com.tangosol.util.BinaryEntry
        public com.tangosol.io.Serializer getSerializer()
            {
            return getService().getSerializer();
            }

        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
         */
        public PartitionedCache getService()
            {
            return (PartitionedCache) get_Module();
            }

        // Accessor for the property "State"
        /**
         * Getter for property State.<p>
         * A combination of flags reflecting the state of this BinaryEntry.
         * The reason we keep it as a single value is to decrease the memory
         * footprint.
         *
         * As of Coherence 12.2.1 this property is optimized to use
         * _StateAux and as a result is limited to 30 bits.
         *
         * The supported flags are KEY_*, ORIG_*, VALUE_*, ACCESS_*
         * constants.
         *
         * @functional
         */
        protected int getState()
            {
            return get_StateAux() & STATE_MASK;
            }

        // Accessor for the property "Storage"
        /**
         * Getter for property Storage.<p>
         */
        public Storage getStorage()
            {
            return (Storage) get_Parent();
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        public Object getValue()
            {
            // import com.tangosol.util.MapIndex;

            int nState = getState();

            if ((nState & VALUE_CONVERTED) != 0)
                {
                return getConvertedValue();
                }

            Storage storage  = getStorage();
            MapIndex indexFwd = storage.getDeserializationAccelerator(getKeyPartition());

            // if the value is updated or removed "in place", we should not use the forward index
            Object oValue = indexFwd == null || (nState & (VALUE_FORCE_EXTRACT | VALUE_UPDATED | VALUE_REMOVED)) != 0
                            ? MapIndex.NO_VALUE
                            : indexFwd.get(getBinaryKey());
            if (oValue == MapIndex.NO_VALUE)
                {
                oValue = storage.getConverterUp().convert(getBinaryValue());
                }
            else
                {
                checkAccess(ACCESS_READ);
                }

            setConvertedValue(oValue);
            setState(getState() | VALUE_CONVERTED); // can't use "nState"; getBinaryValue() could have changed it
            
            return oValue;
            }

        // Accessor for the property "VersionTrint"
        /**
         * Getter for property VersionTrint.<p>
         * The "trint14" value representing the version of the index as it
         * was before the binary value for this entry was read.
         *
         * This value is held in the bits 16-29 of the _StateAux property.
         *
         * @functional
         */
        public int getVersionTrint()
            {
            return (get_StateAux() & VERSION_MASK) >>> VERSION_OFFSET;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        // Declared at the super level
        public int hashCode()
            {
            // import com.tangosol.util.Binary;

            // Note: this hashCode implementation does not obey the Map$Entry contract
            //       and intentiionally does not include hashCode
            Binary binKey = getBinaryKey();
            return binKey == null ? 0 : binKey.hashCode();
            }

        // Accessor for the property "ExpireChanged"
        /**
         * Getter for property ExpireChanged.<p>
         * Calculated property that checks whether or not an EntryProcessor
         * has changed the entry's expiry value.
         */
        public boolean isExpireChanged()
            {
            return (getState() & EXPIRE_UPDATED) != 0;
            }

        /**
         * Return true if expiry has been initialized (read from the backing
         * map's entry or decoded from the binary value).
         */
        public boolean isExpiryInitialized()
            {
            return getExpiryValue() != -2L;
            }

        // Accessor for the property "KeyConverted"
        /**
         * Getter for property KeyConverted.<p>
         * Calculated property that checks whether or not the entry's
         * converted key has been set.
         */
        public boolean isKeyConverted()
            {
            return (getState() & KEY_CONVERTED) != 0;
            }

        // From interface: com.tangosol.util.MapTrigger$Entry
        public boolean isOriginalPresent()
            {
            int nState = getState();
            if ((nState & ORIG_MASK) != 0)
                {
                return (nState & ORIG_PRESENT) != 0;
                }

            if (getBackingMapInternal().containsKey(getBinaryKey()))
                {
                setState(nState | ORIG_PRESENT);
                return true;
                }
            else
                {
                setState(nState | ORIG_NONE);
                return false;
                }
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        public boolean isPresent()
            {
            int nState = getState();

            if ((nState & (VALUE_REMOVED | VALUE_NONE)) != 0)
                {
                return false;
                }

            return (nState & (VALUE_UPDATED | VALUE_LOADED)) != 0 || isOriginalPresent();
            }

        // From interface: com.tangosol.util.BinaryEntry
        // Accessor for the property "ReadOnly"
        /**
         * Getter for property ReadOnly.<p>
         */
        public boolean isReadOnly()
            {
            return (getState() & VALUE_READONLY) != 0;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        public boolean isSynthetic()
            {
            return (getState() & VALUE_SYNTHETIC) != 0;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // Accessor for the property "ValueChanged"
        /**
         * Getter for property ValueChanged.<p>
         * Calculated property that checks whether or not an EntryProcessor
         * has changed (updated or removed) the entry's value (either
         * Converted or Binary).
         */
        public boolean isValueChanged()
            {
            return (getState() & (VALUE_UPDATED | EXPIRE_UPDATED | VALUE_REMOVED)) != 0;
            }

        // Accessor for the property "ValueConverted"
        /**
         * Getter for property ValueConverted.<p>
         * Calculated property that checks whether or not the entry's
         * converted value has been set.
         */
        public boolean isValueConverted()
            {
            return (getState() & VALUE_CONVERTED) != 0;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // Accessor for the property "ValueLoaded"
        /**
         * Getter for property ValueLoaded.<p>
         * Calculated property that checks whether or not an EntryProcessor
         * has loaded the entry's value. This flag could be true only when
         * OriginalPresent is false.
         */
        public boolean isValueLoaded()
            {
            return (getState() & VALUE_LOADED) != 0;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // Accessor for the property "ValueRemoved"
        /**
         * Getter for property ValueRemoved.<p>
         * Calculated property that checks whether or not an EntryProcessor
         * has removed the entry.
         */
        public boolean isValueRemoved()
            {
            return (getState() & VALUE_REMOVED) != 0;
            }

        // Accessor for the property "ValueStale"
        /**
         * Getter for property ValueStale.<p>
         * Calculated property that indicates whether the binary value
         * associated to this entry is potentially stale. This is generally
         * the result of loading the entry prior to locking the entry.
         */
        public boolean isValueStale()
            {
            return (getState() & VALUE_STALE) != 0;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // Accessor for the property "ValueUpdated"
        /**
         * Getter for property ValueUpdated.<p>
         * Calculated property that checks whether or not an EntryProcessor
         * has updated the entry's value (either Converted or Binary).
         */
        public boolean isValueUpdated()
            {
            return (getState() & VALUE_UPDATED) != 0;
            }

        /**
         * Marks this entry such that it can be determined whether its
         * binary value has the potential to be stale.
         *
         * @see isValueStale()
         */
        public BinaryEntry markStale()
            {
            return setState(getState() | VALUE_STALE);
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        public void remove(boolean fSynthetic)
            {
            checkMutable();

            setConvertedValue(null);
            setBinaryValue(null);

            int nState = getState() & ~(VALUE_UPDATED | VALUE_SYNTHETIC);
            setState(nState | VALUE_CONVERTED | VALUE_REMOVED | VALUE_NONE |
                     (fSynthetic ? VALUE_SYNTHETIC : 0));
            }

        /**
         * Reset this entry with a new BinaryKey.
         */
        public BinaryEntry reset(com.tangosol.util.Binary binKey)
            {
            return reset(binKey, null);
            }

        /**
         * Reset this entry with a new Binary key and value.
         */
        public BinaryEntry reset(com.tangosol.util.Binary binKey, com.tangosol.util.Binary binValue)
            {
            _assert(binKey != null);

            // the only flag to be preserved is "read-only"
            setState(getState() & VALUE_READONLY);
            setBinaryKey(binKey);
            setBinaryValue(binValue);
            set_Feed(null); // OriginalValue
            set_Sink(null); // OriginalBinaryValue
            setExpiryValue(-2L);
            return this;
            }

        // Accessor for the property "BinaryKey"
        /**
         * Setter for property BinaryKey.<p>
         * Binary key.
         */
        public BinaryEntry setBinaryKey(com.tangosol.util.Binary binKey)
            {
            __m_BinaryKey = binKey;
            return this;
            }

        // Accessor for the property "BinaryValue"
        /**
         * Setter for property BinaryValue.<p>
         * Binary value.
         */
        public BinaryEntry setBinaryValue(com.tangosol.util.Binary binValue)
            {
            __m_BinaryValue = (binValue);

            // the authorizer should be able to see the newly set value
            checkAccess(ACCESS_WRITE);
            return this;
            }

        // Accessor for the property "ConvertedKey"
        /**
         * Setter for property ConvertedKey.<p>
         * Converted key.
         */
        protected BinaryEntry setConvertedKey(Object oKey)
            {
            __m_ConvertedKey = oKey;
            return this;
            }

        // Accessor for the property "ConvertedValue"
        /**
         * Setter for property ConvertedValue.<p>
         * Converted value.
         */
        protected BinaryEntry setConvertedValue(Object oValue)
            {
            __m_ConvertedValue = oValue;
            return this;
            }

        // Accessor for the property "ExpiryValue"
        /**
         * Setter for property ExpiryValue.<p>
         * Expiry value.
         */
        public BinaryEntry setExpiryValue(long lValue)
            {
            __m_ExpiryValue = lValue;
            return this;
            }

        /**
         * Marks this entry such that it can be determined whether any index
         * entries for this entry may be out of sync so indexes should not
         * be used to shortcut extractors.
         */
        public BinaryEntry setForceExtract(boolean fForce)
            {
            return fForce
                   ? setState(getState() | VALUE_FORCE_EXTRACT)
                   : setState(getState() & ~VALUE_FORCE_EXTRACT);
            }

        // Accessor for the property "State"
        /**
         * Setter for property State.<p>
         * A combination of flags reflecting the state of this BinaryEntry.
         * The reason we keep it as a single value is to decrease the memory
         * footprint.
         *
         * As of Coherence 12.2.1 this property is optimized to use
         * _StateAux and as a result is limited to 30 bits.
         *
         * The supported flags are KEY_*, ORIG_*, VALUE_*, ACCESS_*
         * constants.
         *
         * @functional
         */
        protected BinaryEntry setState(int nState)
            {
            set_StateAux((get_StateAux() & ~STATE_MASK) | (nState & STATE_MASK));
            return this;
            }

        /**
         * Mark the associated mutations to this Entry as either synthetic
         * or not.
         *
         * @param fSynthetic  whether the mutations to this entry should be
         * considered as synthetic
         */
        public BinaryEntry setSynthetic(boolean fSynthetic)
            {
            return fSynthetic
                   ? setState(getState() | VALUE_SYNTHETIC)
                   : setState(getState() & ~VALUE_SYNTHETIC);
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        public Object setValue(Object oValue)
            {
            checkMutable();

            Object oValueOld = getValue();

            setValue(oValue, false);

            return oValueOld;
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        public void setValue(Object oValue, boolean fSynthetic)
            {
            // import com.tangosol.net.cache.CacheMap;

            checkMutable();

            setConvertedValue(oValue);
            setBinaryValue(null);
            if (!isExpireChanged())
                {
                // set the default expiry unless they already specified an expiry delay
                setExpiryValue(CacheMap.EXPIRY_DEFAULT);
                }

            int nState = (getState() | VALUE_CONVERTED | VALUE_UPDATED) & ~(VALUE_REMOVED | VALUE_NONE);
            if (fSynthetic)
                {
                nState |= VALUE_SYNTHETIC;
                }
            else
                {
                nState &= ~VALUE_SYNTHETIC;
                }
            setState(nState);
            }

        // Accessor for the property "VersionTrint"
        /**
         * Setter for property VersionTrint.<p>
         * The "trint14" value representing the version of the index as it
         * was before the binary value for this entry was read.
         *
         * This value is held in the bits 16-29 of the _StateAux property.
         *
         * @functional
         */
        public void setVersionTrint(int nTrint)
            {
            set_StateAux((get_StateAux() & ~VERSION_MASK) | (nTrint << VERSION_OFFSET));
            }

        // Declared at the super level
        public String toString()
            {
            int nState = getState();
            return get_Name()
                   + "{Key="    + ((nState & KEY_CONVERTED) != 0   ? String.valueOf(getKey())   : toString(getBinaryKey()))
                   + ", Value=" + ((nState & VALUE_CONVERTED) != 0 ? String.valueOf(getValue()) : toString(getBinaryValue()))
                   +  ", State=" + formatState(nState) + '}';
            }

        /**
         * Provide a human readable string for the specified Binary.
         */
        protected String toString(com.tangosol.util.Binary binValue)
            {
            // import com.tangosol.util.Base;

            final int MAX_BYTES = 512;

            int cb = binValue == null ? 0 : binValue.length();

            return cb <= MAX_BYTES ? String.valueOf(binValue) :
                   "Binary(length=" + binValue.length() + ", value=" +
                   Base.toHexEscape(binValue.toByteArray(0, MAX_BYTES)) + "...)";
            }

        // From interface: com.tangosol.util.BinaryEntry
        // From interface: com.tangosol.util.MapTrigger$Entry
        public void update(com.tangosol.util.ValueUpdater updater, Object oValue)
            {
            // import com.tangosol.util.InvocableMapHelper;

            // if the updater had a reference to a corresponding extractor
            // we could update an index without deserialization,
            // and keep the update as a delta to a Binary;
            // seems like a long shot though ...

            InvocableMapHelper.updateEntry(updater, this, oValue);
            }

        // From interface: com.tangosol.util.BinaryEntry
        /**
         * Update the BinaryValue for this entry.
         */
        public void updateBinaryValue(com.tangosol.util.Binary binValue)
            {
            updateBinaryValue(binValue, false);
            }

        // From interface: com.tangosol.util.BinaryEntry
        public void updateBinaryValue(com.tangosol.util.Binary binValue, boolean fSynthetic)
            {
            // import com.tangosol.net.cache.CacheMap;

            checkMutable();

            if (binValue == null)
                {
                remove(fSynthetic);
                }
            else
                {
                int nState = getState() & ~(VALUE_SYNTHETIC | VALUE_REMOVED | VALUE_CONVERTED | VALUE_NONE);
                setState(nState | VALUE_UPDATED | (fSynthetic ? VALUE_SYNTHETIC : 0));
                setBinaryValue(binValue);
                setConvertedValue(null);

                if (!isExpireChanged())
                    {
                    // set the default expiry unless they already specified an expiry delay
                    setExpiryValue(CacheMap.EXPIRY_DEFAULT);
                    }
                }
            }

        public void updateLoadedValue(com.tangosol.util.Binary binValue)
            {
            checkMutable();

            if (binValue == null)
                {
                throw new IllegalArgumentException();
                }

            setState(getState() | (VALUE_LOADED | ORIG_PRESENT | VALUE_SYNTHETIC));
            setBinaryValue(binValue);
            }

        /**
         * Synthetic setter.
         */
        public void updateOriginalBinaryValue(com.tangosol.util.Binary binValue)
            {
            set_Sink(binValue);
            if (binValue == null)
                {
                setState((getState() & ~ORIG_MASK) | ORIG_NONE);
                }
            else
                {
                setState((getState() & ~ORIG_MASK) | ORIG_PRESENT);
                }
            }

        // From interface: com.tangosol.io.SerializationSupport
        /**
         * Return an object that should be serialized instead of this
         * instance.
         */
        public Object writeReplace()
                throws java.io.ObjectStreamException
            {
            // import com.tangosol.internal.util.SimpleBinaryEntry;

            return new SimpleBinaryEntry(getBinaryKey(), getBinaryValue());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$DeferredEvent

    /**
     * DeferredEvent is a BME whose processing is deferred.  The main
     * reason for deferring the handing of a BME is that the partition in
     * question is currently locked for transfer.
     *
     * See also $PartitionControl#unlock(), $TransferControl#processEvents()
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DeferredEvent
            extends    com.tangosol.coherence.component.Util
            implements com.oracle.coherence.common.base.Continuation
        {
        // ---- Fields declarations ----

        /**
         * Property Event
         *
         * The MapEvent to defer.
         */
        private com.tangosol.util.MapEvent __m_Event;

        /**
         * Property Reapply
         *
         * Should this event be re-applied to the backing-map?
         *
         * If true, the effects of the event are performed on the
         * backing-map (including any generated BMEs).  This happens when
         * an event is raised during primary transfer and added as a
         * transfer addendum (it must be re-played on the receiver).
         * Otherwise, if false, the event is handled as if it were a
         * generated BME.  This happens when an event is raised while
         * receiving transfer and handling must be deferred until the
         * receive is fully committed.
         */
        private boolean __m_Reapply;

        // Default constructor
        public DeferredEvent()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public DeferredEvent(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new DeferredEvent();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$DeferredEvent".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // Accessor for the property "CacheId"
        /**
         * Getter for property CacheId.<p>
         * The cache id that generated the deferred event.
         */
        public long getCacheId()
            {
            return getStorage().getCacheId();
            }

        // Accessor for the property "Event"
        /**
         * Getter for property Event.<p>
         * The MapEvent to defer.
         */
        public com.tangosol.util.MapEvent getEvent()
            {
            return __m_Event;
            }

        // Accessor for the property "Storage"
        /**
         * Getter for property Storage.<p>
         * The $Storage corresponding to this event.
         */
        public Storage getStorage()
            {
            return (Storage) get_Parent();
            }

        // Accessor for the property "Reapply"
        /**
         * Getter for property Reapply.<p>
         * Should this event be re-applied to the backing-map?
         *
         * If true, the effects of the event are performed on the
         * backing-map (including any generated BMEs).  This happens when an
         * event is raised during primary transfer and added as a transfer
         * addendum (it must be re-played on the receiver).  Otherwise, if
         * false, the event is handled as if it were a generated BME.  This
         * happens when an event is raised while receiving transfer and
         * handling must be deferred until the receive is fully committed.
         */
        public boolean isReapply()
            {
            return __m_Reapply;
            }

        // From interface: com.oracle.coherence.common.base.Continuation
        public void proceed(Object oResult)
            {
            // import java.util.Map;
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;

            com.tangosol.util.MapEvent evt     = getEvent();
            Storage storage = getStorage();

            if (evt == null)
                {
                return;
                }

            if (isReapply())
                {
                Map    map    = storage.getBackingMap();
                Binary binKey = (Binary) evt.getKey();

                switch (evt.getId())
                    {
                    case com.tangosol.util.MapEvent.ENTRY_DELETED:
                        map.remove(binKey);
                        break;

                    case com.tangosol.util.MapEvent.ENTRY_UPDATED:
                    case com.tangosol.util.MapEvent.ENTRY_INSERTED:
                        map.put(binKey, evt.getNewValue());
                        break;

                    default:
                        _assert(false);
                    }
                }

            storage.doBackingMapEvent(evt);
            }

        public void read(java.io.DataInput stream)
                throws java.io.IOException
            {
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ExternalizableHelper;
            // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
            // import com.tangosol.util.ObservableMap;
            // import java.util.Map;

            setReapply(stream.readBoolean());

            // read the event data
            int    nEvtId      = ExternalizableHelper.readInt(stream);
            Binary binKey      = (Binary) ExternalizableHelper.readObject(stream);
            Binary binValueOld = (Binary) ExternalizableHelper.readObject(stream);
            Binary binValueNew = (Binary) ExternalizableHelper.readObject(stream);
            Map    mapResource = getStorage().getBackingMap();

            // the resource map should be observable (as it originally raised this BME)
            if (mapResource instanceof ObservableMap)
                {
                setEvent(new com.tangosol.util.MapEvent(
                        (ObservableMap) mapResource, nEvtId, binKey, binValueOld, binValueNew));
                }
            else
                {
                _trace("Deferred \"" + com.tangosol.util.MapEvent.getDescription(nEvtId)
                       + "\" event cannot be applied for " + binKey, 2);
                }
            }

        // Accessor for the property "Event"
        /**
         * Setter for property Event.<p>
         * The MapEvent to defer.
         */
        public void setEvent(com.tangosol.util.MapEvent pEvent)
            {
            __m_Event = pEvent;
            }

        // Accessor for the property "Reapply"
        /**
         * Setter for property Reapply.<p>
         * Should this event be re-applied to the backing-map?
         *
         * If true, the effects of the event are performed on the
         * backing-map (including any generated BMEs).  This happens when an
         * event is raised during primary transfer and added as a transfer
         * addendum (it must be re-played on the receiver).  Otherwise, if
         * false, the event is handled as if it were a generated BME.  This
         * happens when an event is raised while receiving transfer and
         * handling must be deferred until the receive is fully committed.
         */
        public void setReapply(boolean pReapply)
            {
            __m_Reapply = pReapply;
            }

        // Declared at the super level
        public String toString()
            {
            return "DeferredEvent{CacheId=" + getCacheId() + " Event=" + getEvent() + "}";
            }

        public void write(java.io.DataOutput stream)
                throws java.io.IOException
            {
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ExternalizableHelper;
            // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;

            stream.writeBoolean(isReapply());

            // write the event state
            com.tangosol.util.MapEvent event = getEvent();
            ExternalizableHelper.writeInt   (stream, event.getId());
            ExternalizableHelper.writeObject(stream, event.getKey());
            ExternalizableHelper.writeObject(stream, event.getOldValue());
            ExternalizableHelper.writeObject(stream, event.getNewValue());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$DispatcherInterceptor

    /**
     * An EventInterceptor that expects to receive
     * InterceptorRegistrationEvent events and will call
     * $Storage.checkOldValueRequired as a result.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DispatcherInterceptor
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.net.events.EventInterceptor
        {
        // ---- Fields declarations ----

        /**
         * Property InterceptorCount
         *
         * A count of the number of storage interceptors registered.
         */
        private transient java.util.concurrent.atomic.AtomicLong __m_InterceptorCount;

        // Default constructor
        public DispatcherInterceptor()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public DispatcherInterceptor(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setInterceptorCount(new java.util.concurrent.atomic.AtomicLong());
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
            return new DispatcherInterceptor();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$DispatcherInterceptor".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // Accessor for the property "InterceptorCount"
        /**
         * Getter for property InterceptorCount.<p>
         * A count of the number of storage interceptors registered.
         */
        public java.util.concurrent.atomic.AtomicLong getInterceptorCount()
            {
            return __m_InterceptorCount;
            }

        // Accessor for the property "Storage"
        /**
         * Getter for property Storage.<p>
         */
        public Storage getStorage()
            {
            return (Storage) get_Parent();
            }

        // From interface: com.tangosol.net.events.EventInterceptor
        public void onEvent(com.tangosol.net.events.Event event)
            {
            // import com.tangosol.net.events.EventDispatcher$InterceptorRegistrationEvent as com.tangosol.net.events.EventDispatcher.InterceptorRegistrationEvent;
            // import com.tangosol.net.events.EventDispatcher$InterceptorRegistrationEvent$Type as com.tangosol.net.events.EventDispatcher.InterceptorRegistrationEvent.Type;

            // an interceptor has been added or removed therefore notify Storage
            // to ensure the old value is available if needed
            com.tangosol.net.events.EventDispatcher.InterceptorRegistrationEvent evtDisp = (com.tangosol.net.events.EventDispatcher.InterceptorRegistrationEvent) event;

            if (evtDisp.getInterceptor() != this)
                {
                Enum type = evtDisp.getType();
                if (type == com.tangosol.net.events.EventDispatcher.InterceptorRegistrationEvent.Type.INSERTED)
                    {
                    getInterceptorCount().incrementAndGet();
                    getStorage().ensureOldValueRequired();
                    }
                else if (type == com.tangosol.net.events.EventDispatcher.InterceptorRegistrationEvent.Type.REMOVED)
                    {
                    getInterceptorCount().decrementAndGet();
                    }
                }
            }

        // Accessor for the property "InterceptorCount"
        /**
         * Setter for property InterceptorCount.<p>
         * A count of the number of storage interceptors registered.
         */
        public void setInterceptorCount(java.util.concurrent.atomic.AtomicLong atomicCount)
            {
            __m_InterceptorCount = atomicCount;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$EnlistingConverter

    /**
     * A Converter that converts a binary key to a BinaryEntry and enlists
     * the entry with the appropriate InvocationContext.
     *
     * If a MapTrigger is provided to this converter it is called with the
     * BinaryEntry prior to it being returned.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EnlistingConverter
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.util.Converter
        {
        // ---- Fields declarations ----

        /**
         * Property InvocationContext
         *
         */
        private PartitionedCache.InvocationContext __m_InvocationContext;

        /**
         * Property Trigger
         *
         */
        private com.tangosol.util.MapTrigger __m_Trigger;

        // Default constructor
        public EnlistingConverter()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public EnlistingConverter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new EnlistingConverter();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$EnlistingConverter".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // From interface: com.tangosol.util.Converter
        public Object convert(Object oKey)
            {
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.MapTrigger;

            Binary             binKey    = (Binary) oKey;
            PartitionedCache.InvocationContext ctxInvoke = getInvocationContext();

            _assert(ctxInvoke != null && binKey != null);

            EntryStatus status   = ctxInvoke.enlistSynthetic(getStorage(), binKey);
            BinaryEntry binEntry = status.getBinaryEntry();

            // call any registered triggers to manipulate the entry
            MapTrigger trigger = getTrigger();
            if (trigger != null)
                {
                trigger.process(binEntry);
                }

            return binEntry;
            }

        // Accessor for the property "InvocationContext"
        /**
         * Getter for property InvocationContext.<p>
         */
        public PartitionedCache.InvocationContext getInvocationContext()
            {
            return __m_InvocationContext;
            }

        // Accessor for the property "Storage"
        /**
         * Getter for property Storage.<p>
         */
        protected Storage getStorage()
            {
            return (Storage) get_Parent();
            }

        // Accessor for the property "Trigger"
        /**
         * Getter for property Trigger.<p>
         */
        public com.tangosol.util.MapTrigger getTrigger()
            {
            return __m_Trigger;
            }

        // Accessor for the property "InvocationContext"
        /**
         * Setter for property InvocationContext.<p>
         */
        public void setInvocationContext(PartitionedCache.InvocationContext ctxInvoke)
            {
            __m_InvocationContext = ctxInvoke;
            }

        // Accessor for the property "Trigger"
        /**
         * Setter for property Trigger.<p>
         */
        public void setTrigger(com.tangosol.util.MapTrigger trigger)
            {
            __m_Trigger = trigger;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$EntryStatus

    /**
     * $EntryStatus encapsulates the state related to handling how changes
     * to a cache entry are processed.
     *
     * Since the EntryStatus components could be created quite frequently,
     * we don't use the standard _newChild() approach, but just "new" it.
     *
     * Also, we are using the _Feed and _Sink properties as a space
     * optimization.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EntryStatus
            extends    com.tangosol.coherence.component.Util
        {
        // ---- Fields declarations ----

        /**
         * Property Active
         *
         * True iff the status is still "active".  An EntryStatus is active
         * until it has been processed.  Events may not be posted to an
         * EntryStatus once it has become inactive.
         */
        private boolean __m_Active;

        /**
         * Property AnyAction
         *
         * @volatile
         *
         * True iff any actions have been processed against this
         * EntryStatus.
         */
        private volatile boolean __m_AnyAction;

        /**
         * Property BinaryEntry
         *
         * The BinaryEntry represented by this EntryStatus
         */
        private BinaryEntry __m_BinaryEntry;

        /**
         * Property ExpiryOnly
         *
         * True iff the only event associated with this status is an expiry
         * change (raised from read operations on backing map with
         * sliding-expiry enabled).
         */
        private boolean __m_ExpiryOnly;

        /**
         * Property Managed
         *
         * Is this EntryStatus being managed as part of a "front-door"
         * request/operation?
         *
         * @volatile
         */
        private volatile boolean __m_Managed;

        /**
         * Property MapEventHolder
         *
         * A holder for $MapEvents associated with this status.
         *
         * $MapEvent represents a global event to be sent to MapListeners.
         *
         * @see $Storage#prepareDispatch
         */
        private Object __m_MapEventHolder;

        /**
         * Property MapEventsRaw
         *
         * MapEvents to be posted.
         *
         * @volatile
         */
        private volatile Object __m_MapEventsRaw;

        /**
         * Property MergedNewValue
         *
         * @volatile
         *
         * A combined new value for all events processed against this
         * EntryStatus.
         */
        private volatile com.tangosol.io.ReadBuffer __m_MergedNewValue;

        /**
         * Property OldValue
         *
         * The original binary value associated with this EntryStatus
         * before any of the processed events occurred.
         */
        private com.tangosol.util.Binary __m_OldValue;

        /**
         * Property Pending
         *
         * Specifies whether or not the status is pending.
         */
        private transient boolean __m_Pending;

        /**
         * Property Result
         *
         * A converted result of the EntryProcessor.process() invocation.
         */
        private com.tangosol.util.Binary __m_Result;

        /**
         * Property SuppressEvents
         *
         * Indicates that the change represented by this status should not
         * be observed by UEM interceptors (e.g. DECO_STORE decoration
         * change only).
         */
        private boolean __m_SuppressEvents;

        /**
         * Property WaitingThreadCount
         *
         * The number of threads waiting for the pending status to be
         * opened.
         */
        private transient int __m_WaitingThreadCount;

        // Default constructor
        public EntryStatus()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public EntryStatus(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setActive(true);
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
            return new EntryStatus();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$EntryStatus".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        /**
         * Add the specified MapEvent to the list of events to be posted
         */
        public void addEventMessages(Object oEvtHolder)
            {
            setMapEventHolder(accumulateMapEvents(getMapEventHolder(), oEvtHolder));
            }

        /**
         * Add the specified MapEvent to the list of events to be posted
         */
        public void addRawMapEvent(com.tangosol.util.MapEvent event)
            {
            // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;
            // import com.tangosol.util.CopyOnWriteLongArray;
            // import com.tangosol.util.LongArray;

            Object    oEvent   = getMapEventsRaw();
            LongArray laEvents = null;

            if (oEvent == null)
                {
                setMapEventsRaw(event);
                }
            else if (oEvent instanceof com.tangosol.util.MapEvent)
                {
                setMapEventsRaw(laEvents = new CopyOnWriteLongArray());

                laEvents.set(((com.tangosol.util.MapEvent) oEvent).getVersion(), oEvent);
                }
            else
                {
                laEvents = (LongArray) oEvent;
                }

            if (laEvents != null)
                {
                laEvents.set(event.getVersion(), event);
                }
            }

        // Accessor for the property "BinaryEntry"
        /**
         * Getter for property BinaryEntry.<p>
         * The BinaryEntry represented by this EntryStatus
         */
        public BinaryEntry getBinaryEntry()
            {
            return __m_BinaryEntry;
            }

        // Accessor for the property "EventQueue"
        /**
         * Getter for property EventQueue.<p>
         * The queue of $EventHolder object associated with this status that
         * have yet to be processed.
         *
         * EventHolder objects represent observed BackingMap events.
         */
        public com.tangosol.internal.util.BMEventFabric.EventQueue getEventQueue()
            {
            // import com.tangosol.internal.util.BMEventFabric$EventQueue as com.tangosol.internal.util.BMEventFabric.EventQueue;

            com.tangosol.internal.util.BMEventFabric.EventQueue queue = (com.tangosol.internal.util.BMEventFabric.EventQueue) get_Feed();
            if (queue == null)
                {
                synchronized (this)
                    {
                    queue = (com.tangosol.internal.util.BMEventFabric.EventQueue) get_Feed();
                    if (queue == null)
                        {
                        queue = getStorage().getResourceCoordinator().instantiateEventQueue(false);
                        set_Feed(queue);
                        }
                    }
                }
            return queue;
            }

        public com.tangosol.net.events.partition.cache.EntryEvent.Type getEventType()
            {
            // import com.tangosol.net.events.partition.cache.EntryEvent$Type as com.tangosol.net.events.partition.cache.EntryEvent.Type;

            return isValueRemoved() ? com.tangosol.net.events.partition.cache.EntryEvent.Type.REMOVED :
                   isValueUpdated() ? getOldValue() == null ? com.tangosol.net.events.partition.cache.EntryEvent.Type.INSERTED : com.tangosol.net.events.partition.cache.EntryEvent.Type.UPDATED
                                    : null;
            }

        // Accessor for the property "Key"
        /**
         * Getter for property Key.<p>
         * The (Binary) key that this $EntryStatus represents.
         */
        public com.tangosol.util.Binary getKey()
            {
            // import com.tangosol.util.Binary;

            return (Binary) get_Sink();
            }

        // Accessor for the property "MapEventHolder"
        /**
         * Getter for property MapEventHolder.<p>
         * A holder for $MapEvents associated with this status.
         *
         * $MapEvent represents a global event to be sent to MapListeners.
         *
         * @see $Storage#prepareDispatch
         */
        public Object getMapEventHolder()
            {
            return __m_MapEventHolder;
            }

        // Accessor for the property "MapEventsRaw"
        /**
         * Getter for property MapEventsRaw.<p>
         * MapEvents to be posted.
         *
         * @volatile
         */
        public Object getMapEventsRaw()
            {
            return __m_MapEventsRaw;
            }

        // Accessor for the property "MaxMapEventVersion"
        /**
         * Getter for property MaxMapEventVersion.<p>
         */
        public long getMaxMapEventVersion()
            {
            // import com.tangosol.util.LongArray;
            // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;

            Object oEvent = getMapEventsRaw();

            return oEvent instanceof LongArray
                   ? ((LongArray) oEvent).floorIndex(Long.MAX_VALUE) :
                   oEvent == null
                   ? -1L
                   : ((com.tangosol.util.MapEvent) oEvent).getVersion();
            }

        // Accessor for the property "MergedNewValue"
        /**
         * Getter for property MergedNewValue.<p>
         * @volatile
         *
         * A combined new value for all events processed against this
         * EntryStatus.
         */
        public com.tangosol.io.ReadBuffer getMergedNewValue()
            {
            return __m_MergedNewValue;
            }

        // Accessor for the property "OldValue"
        /**
         * Getter for property OldValue.<p>
         * The original binary value associated with this EntryStatus before
         * any of the processed events occurred.
         */
        public com.tangosol.util.Binary getOldValue()
            {
            return __m_OldValue;
            }

        /**
         * Returns the partition number of the key that this $EntryStatus
         * represents.
         */
        public int getPartition()
            {
            return Float.floatToRawIntBits(get_Order());
            }

        /**
         * Return a read-only BinaryEntry for this status. If this status is
         * managed by the current thread the cached BinaryEntry is made
         * read-only and returned, otherwise a new BinaryEntry is created.
         */
        public synchronized BinaryEntry getReadOnlyEntry()
            {
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ConcurrentMap;
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;

            Storage      storage = getStorage();
            Binary        binKey  = getKey();
            boolean       fConc   = storage.getService().isConcurrent();
            ConcurrentMap mapCtrl = storage.getResourceControlMap();

            if (isManaged() && (!fConc || mapCtrl.lock(binKey, 0)))
                {
                // we hold the lock; therefore it is safe to reuse the same BinaryEntry
                BinaryEntry entry = getBinaryEntry();

                if (entry == null)
                    {
                    entry = storage.instantiateBinaryEntry(binKey, com.tangosol.util.ExternalizableHelper.asBinary(getMergedNewValue()), true);
                    setBinaryEntry(entry);
                    }
                else
                    {
                    entry.ensureReadOnly();
                    }

                if (fConc)
                    {
                    // if we requested the lock we must compensate by decrementing
                    // the lock count
                    mapCtrl.unlock(binKey);
                    }

                return entry;
                }

            // either the status is unmanaged or we do not hold the lock;
            // return a new BinaryEntry
            return storage.instantiateBinaryEntry(binKey, com.tangosol.util.ExternalizableHelper.asBinary(getMergedNewValue()), true);
            }

        // Accessor for the property "Result"
        /**
         * Getter for property Result.<p>
         * A converted result of the EntryProcessor.process() invocation.
         */
        public com.tangosol.util.Binary getResult()
            {
            return __m_Result;
            }

        // Accessor for the property "Storage"
        /**
         * Getter for property Storage.<p>
         */
        public Storage getStorage()
            {
            return (Storage) get_Parent();
            }

        // Accessor for the property "WaitingThreadCount"
        /**
         * Getter for property WaitingThreadCount.<p>
         * The number of threads waiting for the pending status to be opened.
         */
        private int getWaitingThreadCount()
            {
            return __m_WaitingThreadCount;
            }

        /**
         * Return true if the specified MapEvent is to be posted.
         */
        public synchronized boolean hasEvent(com.tangosol.util.MapEvent event)
            {
            // import com.tangosol.util.LongArray;
            // import com.tangosol.util.MapEvent as com.tangosol.util.MapEvent;

            Object oEvent = getMapEventsRaw();
            return oEvent != null && (oEvent instanceof com.tangosol.util.MapEvent
                                      ? ((com.tangosol.util.MapEvent) oEvent) == event
                                      : ((LongArray) oEvent).contains(event));
            }

        /**
         * Instantiate a EntryInfo that associates with the status.
         */
        public com.tangosol.net.internal.EntryInfo instantiateEntryInfo()
            {
            // import com.tangosol.net.events.partition.cache.EntryEvent$Type as com.tangosol.net.events.partition.cache.EntryEvent.Type;
            // import com.tangosol.net.internal.EntryInfo;
            // import java.util.Collections;
            // import com.tangosol.util.BinaryEntry;

            return new EntryInfo(getEventType(), getReadOnlyEntry());
            }

        public static EntryStatus instantiateStatus(com.tangosol.util.Binary binKey)
            {
            EntryStatus status = new EntryStatus();
            status.set_Sink(binKey); // Key

            return status;
            }

        // Accessor for the property "Active"
        /**
         * Getter for property Active.<p>
         * True iff the status is still "active".  An EntryStatus is active
         * until it has been processed.  Events may not be posted to an
         * EntryStatus once it has become inactive.
         */
        public boolean isActive()
            {
            return __m_Active;
            }

        // Accessor for the property "AnyAction"
        /**
         * Getter for property AnyAction.<p>
         * @volatile
         *
         * True iff any actions have been processed against this EntryStatus.
         */
        public boolean isAnyAction()
            {
            return __m_AnyAction;
            }

        // Accessor for the property "AnyEvent"
        /**
         * Getter for property AnyEvent.<p>
         * True iff any MapEvents have been processed against this
         * EntryStatus
         */
        public boolean isAnyEvent()
            {
            return getMapEventHolder() != null;
            }

        // Accessor for the property "ExpiryOnly"
        /**
         * Getter for property ExpiryOnly.<p>
         * True iff the only event associated with this status is an expiry
         * change (raised from read operations on backing map with
         * sliding-expiry enabled).
         */
        public boolean isExpiryOnly()
            {
            return __m_ExpiryOnly;
            }

        // Accessor for the property "Managed"
        /**
         * Getter for property Managed.<p>
         * Is this EntryStatus being managed as part of a "front-door"
         * request/operation?
         *
         * @volatile
         */
        public boolean isManaged()
            {
            return __m_Managed;
            }

        // Accessor for the property "Pending"
        /**
         * Getter for property Pending.<p>
         * Specifies whether or not the status is pending.
         */
        private boolean isPending()
            {
            return __m_Pending;
            }

        // Accessor for the property "SuppressEvents"
        /**
         * Getter for property SuppressEvents.<p>
         * Indicates that the change represented by this status should not
         * be observed by UEM interceptors (e.g. DECO_STORE decoration
         * change only).
         */
        public boolean isSuppressEvents()
            {
            return __m_SuppressEvents;
            }

        // Accessor for the property "ValueRemoved"
        /**
         * Getter for property ValueRemoved.<p>
         * (Calculated) True if the entry was removed as a result of an
         * operation.
         */
        public boolean isValueRemoved()
            {
            return isAnyAction() && getMergedNewValue() == null;
            }

        // Accessor for the property "ValueUpdated"
        /**
         * Getter for property ValueUpdated.<p>
         * (Calculated) True if the entry was updated as a result of an
         * operation.
         */
        public boolean isValueUpdated()
            {
            return isAnyAction() && getMergedNewValue() != null;
            }

        /**
         * Prepare the EntryStatus to be published (to backup and
         * persistence).  This method will:
         *   - compress the map events
         *   - compress the result
         *   - ensure that the new value is expiry decorated
         */
        public void preparePublish()
            {
            // import com.tangosol.io.ReadBuffer;
            // import com.tangosol.net.cache.CacheMap;
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;

            // Note: we must not compress the result here; only the copy of the
            //       result sent to the backup may be compressed.  The events may
            //       be compressed as the MapEventMessage data-structure is
            //       polymorphic.

            ReadBuffer bufValueNew = getMergedNewValue();  // could be null for "remove"
            Binary     binValueOld = getOldValue();

            // compress the map-events
            setMapEventHolder(PartitionedCache.MapEvent.compressEventHolder(getMapEventHolder(), binValueOld, com.tangosol.util.ExternalizableHelper.asBinary(bufValueNew)));

            // expiry-decorate the "new-value"
            if (bufValueNew != null)
                {
                BinaryEntry binEntry     = getBinaryEntry();
                Storage     storage      = getStorage();
                long         cEntryExpiry = CacheMap.EXPIRY_DEFAULT;

                // ensure the BinaryEntry and the status are in sync
                if (binEntry != null)
                    {
                    binEntry.updateOriginalBinaryValue(binValueOld);
                    binEntry.setBinaryValue(com.tangosol.util.ExternalizableHelper.asBinary(bufValueNew));
                    cEntryExpiry = binEntry.getExpiry();
                    }

                if (storage.getBackingConfigurableCache() == null)
                    {
                    // the backing-map is not a CCM; the value is either already
                    // decorated with a custom expiry (or EXPIRY_NEVER), or it is
                    // a default expiry and there is nothing more we can do
                    }
                else
                    {
                    long cBMExpiry = storage.getBackingMapExpiryDelay();

                    if ((cEntryExpiry == CacheMap.EXPIRY_NEVER || cEntryExpiry == CacheMap.EXPIRY_DEFAULT) &&
                        cBMExpiry == CacheMap.EXPIRY_NEVER)
                        {
                        // the entry expiry is "never", and so is the backing-map default; no need to decorate
                        }
                    else
                        {
                        long cExpiry = cEntryExpiry == CacheMap.EXPIRY_DEFAULT ? cBMExpiry : cEntryExpiry;

                        // cExpiry is either CacheMap.EXPIRY_NEVER, or a delay in ms
                        bufValueNew = com.tangosol.util.ExternalizableHelper.encodeExpiry(bufValueNew,
                                                                                          cExpiry == CacheMap.EXPIRY_NEVER
                                                                                          ? cExpiry
                                                                                          : storage.getService().getClusterTime() + cExpiry);

                        setMergedNewValue(bufValueNew);
                        }
                    }
                }
            }

        public void reset()
            {
            setManaged       (false);
            setBinaryEntry   (null);
            setMapEventHolder(null);
            setOldValue      (null);
            setMergedNewValue(null);
            setResult        (null);
            setAnyAction     (false);
            setExpiryOnly    (false);
            }

        // Accessor for the property "Active"
        /**
         * Setter for property Active.<p>
         * True iff the status is still "active".  An EntryStatus is active
         * until it has been processed.  Events may not be posted to an
         * EntryStatus once it has become inactive.
         */
        public void setActive(boolean fActive)
            {
            if (fActive && is_Constructed())
                {
                // cannot reset an inactive status to be active
                _assert(isActive());
                }

            __m_Active = (fActive);
            }

        // Accessor for the property "AnyAction"
        /**
         * Setter for property AnyAction.<p>
         * @volatile
         *
         * True iff any actions have been processed against this EntryStatus.
         */
        public void setAnyAction(boolean fAnyAction)
            {
            __m_AnyAction = fAnyAction;
            }

        // Accessor for the property "BinaryEntry"
        /**
         * Setter for property BinaryEntry.<p>
         * The BinaryEntry represented by this EntryStatus
         */
        public void setBinaryEntry(BinaryEntry binEntry)
            {
            __m_BinaryEntry = binEntry;
            }

        // Accessor for the property "ExpiryOnly"
        /**
         * Setter for property ExpiryOnly.<p>
         * True iff the only event associated with this status is an expiry
         * change (raised from read operations on backing map with
         * sliding-expiry enabled).
         */
        public void setExpiryOnly(boolean fOnly)
            {
            __m_ExpiryOnly = fOnly;
            }

        // Accessor for the property "Managed"
        /**
         * Setter for property Managed.<p>
         * Is this EntryStatus being managed as part of a "front-door"
         * request/operation?
         *
         * @volatile
         */
        public synchronized void setManaged(boolean fManaged)
            {
            __m_Managed = fManaged;
            }

        // Accessor for the property "MapEventHolder"
        /**
         * Setter for property MapEventHolder.<p>
         * A holder for $MapEvents associated with this status.
         *
         * $MapEvent represents a global event to be sent to MapListeners.
         *
         * @see $Storage#prepareDispatch
         */
        protected void setMapEventHolder(Object oHolder)
            {
            __m_MapEventHolder = oHolder;
            }

        // Accessor for the property "MapEventsRaw"
        /**
         * Setter for property MapEventsRaw.<p>
         * MapEvents to be posted.
         *
         * @volatile
         */
        public void setMapEventsRaw(Object oRaw)
            {
            __m_MapEventsRaw = oRaw;
            }

        // Accessor for the property "MergedNewValue"
        /**
         * Setter for property MergedNewValue.<p>
         * @volatile
         *
         * A combined new value for all events processed against this
         * EntryStatus.
         */
        public void setMergedNewValue(com.tangosol.io.ReadBuffer bufValue)
            {
            __m_MergedNewValue = bufValue;
            }

        // Accessor for the property "OldValue"
        /**
         * Setter for property OldValue.<p>
         * The original binary value associated with this EntryStatus before
         * any of the processed events occurred.
         */
        public void setOldValue(com.tangosol.util.Binary binValueOld)
            {
            __m_OldValue = binValueOld;
            }

        /**
         * Set the partition number of the key that this $EntryStatus
         * represents.
         */
        public void setPartition(int nPartition)
            {
            // In order to keep the memory footprint for the EntryStatus as small
            // as possible, we are "hijacking" the _Order property to hold the
            // partition number with an understanding that the int -> float -> int
            // conversion is lossy.

            set_Order(Float.intBitsToFloat(nPartition));
            }

        // Accessor for the property "Pending"
        /**
         * Setter for property Pending.<p>
         * Specifies whether or not the status is pending.
         */
        public synchronized void setPending(boolean fPending)
            {
            // import com.oracle.coherence.common.base.Blocking;

            if (fPending)
                {
                boolean fInterrupted = false;
                while (isPending())
                    {
                    setWaitingThreadCount(getWaitingThreadCount() + 1);
                    try
                        {
                        Blocking.wait(this);
                        }
                    catch (InterruptedException e)
                        {
                        fInterrupted = true;
                        }
                    finally
                        {
                        int nWaitingThreads = getWaitingThreadCount();
                        _assert(nWaitingThreads > 0);
                        setWaitingThreadCount(--nWaitingThreads);
                        }
                    }

                __m_Pending = (true);
                if (fInterrupted)
                    {
                    Thread.currentThread().interrupt();
                    }
                }
            else
                {
                __m_Pending = (false);
                if (getWaitingThreadCount() > 0)
                    {
                    notify();
                    }
                }
            }

        // Accessor for the property "Result"
        /**
         * Setter for property Result.<p>
         * A converted result of the EntryProcessor.process() invocation.
         */
        public void setResult(com.tangosol.util.Binary binResult)
            {
            __m_Result = binResult;
            }

        // Accessor for the property "SuppressEvents"
        /**
         * Setter for property SuppressEvents.<p>
         * Indicates that the change represented by this status should not
         * be observed by UEM interceptors (e.g. DECO_STORE decoration
         * change only).
         */
        public void setSuppressEvents(boolean fEvents)
            {
            __m_SuppressEvents = fEvents;
            }

        // Accessor for the property "WaitingThreadCount"
        /**
         * Setter for property WaitingThreadCount.<p>
         * The number of threads waiting for the pending status to be opened.
         */
        private void setWaitingThreadCount(int nCount)
            {
            __m_WaitingThreadCount = nCount;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$EntryToBinaryEntryConverter

    /**
     * Converts a Map$Entry into a read-only BinaryEntry.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EntryToBinaryEntryConverter
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.util.Converter
        {
        // ---- Fields declarations ----

        // Default constructor
        public EntryToBinaryEntryConverter()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public EntryToBinaryEntryConverter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new EntryToBinaryEntryConverter();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$EntryToBinaryEntryConverter".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // From interface: com.tangosol.util.Converter
        /**
         * Converts an Map$Entry to a BinaryEntry.
         */
        public Object convert(Object o)
            {
            // import com.tangosol.util.Binary;
            // import java.util.Map$Entry as java.util.Map.Entry;

            java.util.Map.Entry entry = (java.util.Map.Entry) o;
            return ((Storage) get_Parent()).instantiateBinaryEntry(
                    (Binary) entry.getKey(), (Binary) entry.getValue(), true);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$EvictionTask

    /**
     * A task to perform backing map eviction for the storage.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EvictionTask
            extends    com.tangosol.coherence.component.Util
            implements Runnable
        {
        // ---- Fields declarations ----

        /**
         * Property EVICTION_DELAY
         *
         * The minimum delay until the next time eviction is attempted.
         */
        public static final long EVICTION_DELAY = 250L;

        /**
         * Property EvictionTime
         *
         * The next sheduled eviction time. Zero indicates that nothing is
         * scheduled.
         */
        private long __m_EvictionTime;

        /**
         * Property Prune
         *
         */
        private boolean __m_Prune;

        // Default constructor
        public EvictionTask()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public EvictionTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new EvictionTask();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$EvictionTask".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        /**
         * Cancel execution of this task.
         */
        public void cancel()
            {
            // the best wee can do is to unlink it, so the timer thread
            // holds only on a shallow object

            get_Parent()._unlinkChild(this);
            }

        // Accessor for the property "EvictionTime"
        /**
         * Getter for property EvictionTime.<p>
         * The next sheduled eviction time. Zero indicates that nothing is
         * scheduled.
         */
        public long getEvictionTime()
            {
            return __m_EvictionTime;
            }

        // Accessor for the property "Prune"
        /**
         * Getter for property Prune.<p>
         */
        protected boolean isPrune()
            {
            return __m_Prune;
            }

        // From interface: java.lang.Runnable
        public void run()
            {
            // import com.tangosol.net.cache.ConfigurableCacheMap as com.tangosol.net.cache.ConfigurableCacheMap;
            // import com.tangosol.util.Base;

            Storage storage = (Storage) get_Parent();
            if (storage == null)
                {
                // has been canceled
                return;
                }

            PartitionedCache service = storage.getService();
            com.tangosol.net.cache.ConfigurableCacheMap     mapCCM  = storage.getBackingConfigurableCache();

            if (mapCCM != null)
                {
                if (isPrune())
                    {
                    // stimulate a prune by setting the high-units
                    mapCCM.setHighUnits(mapCCM.getHighUnits());
                    }
                mapCCM.evict();

                service.processChanges();

                long cMillisReschedule = -1L;
                synchronized (storage) // in case of concurrent storage.scheduleEviction
                    {
                    long ldtNow  = Base.getSafeTimeMillis();
                    long ldtNext = mapCCM.getNextExpiryTime();

                    if (ldtNext > 0)
                        {
                        cMillisReschedule = Math.max(EvictionTask.EVICTION_DELAY, ldtNext - ldtNow);
                        }
                    else
                        {
                        long cDefaultExpiry = mapCCM.getExpiryDelay();
                        if (cDefaultExpiry > 0L)
                            {
                            ldtNext           = ldtNow + cDefaultExpiry;
                            cMillisReschedule = cDefaultExpiry;
                            }
                        }

                    setEvictionTime(ldtNext);
                    }

                if (cMillisReschedule != -1L)
                    {
                    service.getDaemonPool().schedule(this, cMillisReschedule);
                    }
                }
            }

        // Accessor for the property "EvictionTime"
        /**
         * Setter for property EvictionTime.<p>
         * The next sheduled eviction time. Zero indicates that nothing is
         * scheduled.
         */
        public void setEvictionTime(long lTime)
            {
            __m_EvictionTime = lTime;
            }

        // Accessor for the property "Prune"
        /**
         * Setter for property Prune.<p>
         */
        public void setPrune(boolean fPrune)
            {
            __m_Prune = fPrune;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$KeyToBinaryEntryConverter

    /**
     * Converts a Binary key into a "lazy" read-only BinaryEntry.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class KeyToBinaryEntryConverter
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.util.Converter
        {
        // ---- Fields declarations ----

        // Default constructor
        public KeyToBinaryEntryConverter()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public KeyToBinaryEntryConverter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new KeyToBinaryEntryConverter();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$KeyToBinaryEntryConverter".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // From interface: com.tangosol.util.Converter
        public Object convert(Object binKey)
            {
            // import com.tangosol.util.Binary;

            return ((Storage) get_Parent()).instantiateBinaryEntry((Binary) binKey, null, true);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$LazyKeySet

    /**
     * The LazyKeySet is used to defer collection the keys from the primary
     * storage if the query could be answered by an index.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LazyKeySet
            extends    com.tangosol.coherence.component.util.collections.WrapperSet
        {
        // ---- Fields declarations ----

        /**
         * Property PartitionMask
         *
         * The partiotions which to collect the primary keys for.
         */
        private com.tangosol.net.partition.PartitionSet __m_PartitionMask;

        /**
         * Property Snapshot
         *
         * If false use a view over the primary keys otherwise make a copy
         * of the keys.
         */
        private boolean __m_Snapshot;
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
            __mapChildren.put("Iterator", LazyKeySet.Iterator.get_CLASS());
            }

        // Default constructor
        public LazyKeySet()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public LazyKeySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new LazyKeySet();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$LazyKeySet".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design
         * time [static] children.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }

        // Accessor for the property "PartitionMask"
        /**
         * Getter for property PartitionMask.<p>
         * The partiotions which to collect the primary keys for.
         */
        public com.tangosol.net.partition.PartitionSet getPartitionMask()
            {
            return __m_PartitionMask;
            }

        // Declared at the super level
        /**
         * Getter for property Set.<p>
         * Wrapped Set
         */
        public java.util.Set getSet()
            {
            // import java.util.Set;

            Set set = super.getSet();
            if (set == null)
                {
                setSet(set = ((Storage) get_Parent()).collectKeySet(getPartitionMask(), isSnapshot()));
                }

            return set;
            }

        // Accessor for the property "Snapshot"
        /**
         * Getter for property Snapshot.<p>
         * If false use a view over the primary keys otherwise make a copy
         * of the keys.
         */
        public boolean isSnapshot()
            {
            return __m_Snapshot;
            }

        // Accessor for the property "PartitionMask"
        /**
         * Setter for property PartitionMask.<p>
         * The partiotions which to collect the primary keys for.
         */
        public void setPartitionMask(com.tangosol.net.partition.PartitionSet parts)
            {
            __m_PartitionMask = parts;
            }

        // Accessor for the property "Snapshot"
        /**
         * Setter for property Snapshot.<p>
         * If false use a view over the primary keys otherwise make a copy
         * of the keys.
         */
        public void setSnapshot(boolean fSnapshot)
            {
            __m_Snapshot = fSnapshot;
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$LazyKeySet$Iterator

        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Iterator
                extends    com.tangosol.coherence.component.util.collections.WrapperSet.Iterator
            {
            // ---- Fields declarations ----

            // Default constructor
            public Iterator()
                {
                this(null, null, true);
                }

            // Initializing constructor
            public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new LazyKeySet.Iterator();
                }

            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
             * Property with auto-generated accessor that returns the Class
             * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$LazyKeySet$Iterator".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }

            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global
             * [design time] parent component.
             *
             * Note: the class generator will ignore any custom
             * implementation for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent().get_Parent();
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$PrimaryListener

    /**
     * The primary storage listener. Used only if a custom backing map
     * manager uses an ObservableMap to implement the [primary] local
     * storage.
     *
     * @see $Storage#setCacheName
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class PrimaryListener
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.util.MapListener
        {
        // ---- Fields declarations ----

        // Default constructor
        public PrimaryListener()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public PrimaryListener(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new PrimaryListener();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$PrimaryListener".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // From interface: com.tangosol.util.MapListener
        public void entryDeleted(com.tangosol.util.MapEvent evt)
            {
            ((Storage) get_Parent()).onBackingMapEvent(evt);
            }

        // From interface: com.tangosol.util.MapListener
        public void entryInserted(com.tangosol.util.MapEvent evt)
            {
            ((Storage) get_Parent()).onBackingMapEvent(evt);
            }

        // From interface: com.tangosol.util.MapListener
        public void entryUpdated(com.tangosol.util.MapEvent evt)
            {
            ((Storage) get_Parent()).onBackingMapEvent(evt);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache/Storage$Scanner

    /**
     * A Streamer implementation based on the partition set and a filter.
     * Note: it never provides entries that are not present.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Scanner
            extends    com.tangosol.coherence.component.util.collections.AdvancingIterator
            implements com.tangosol.util.Streamer
        {
        // ---- Fields declarations ----

        /**
         * Property Count
         *
         * The number of processed elements.
         */
        private int __m_Count;

        /**
         * Property EntryTemp
         *
         * A single entry per Scanner (if ReuseAllowed is true).
         */
        private BinaryEntry __m_EntryTemp;

        /**
         * Property Filter
         *
         * The filter used by the corresponding query. This property is
         * most commonly null, except for non-IndexAware filters.
         */
        private com.tangosol.util.Filter __m_Filter;

        /**
         * Property Iterator
         *
         * An iterator that contains keys or inflated entries.
         */
        private java.util.Iterator __m_Iterator;

        /**
         * Property Keys
         *
         * An array of keys; used to reduce the array allocations.
         */
        private Object[] __m_Keys;

        /**
         * Property NextPartition
         *
         * The next partition to be scanned.
         */
        private int __m_NextPartition;

        /**
         * Property Partitions
         *
         * PartitionSet for all partitions to be scanned that require
         * evaluation.
         */
        private com.tangosol.net.partition.PartitionSet __m_Partitions;

        /**
         * Property ReuseAllowed
         *
         * If true, the $BinartEntry object could be reused across the
         * calls.
         */
        private boolean __m_ReuseAllowed;

        // Default constructor
        public Scanner()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public Scanner(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new Scanner();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
         * Property with auto-generated accessor that returns the Class
         * object for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/Storage$Scanner".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
         * time] parent component.
         *
         * Note: the class generator will ignore any custom implementation
         * for this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent().get_Parent();
            }

        // Declared at the super level
        protected Object advance()
            {
            // import com.tangosol.net.partition.PartitionSet;
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.Filter;
            // import com.tangosol.util.InvocableMapHelper;
            // import java.util.Iterator;
            // import java.util.Map;

            Storage     storage  = getStorage();
            Filter       filter   = getFilter();
            BinaryEntry entry    = getEntryTemp();
            Iterator     iterator = getIterator();

            // in the same way as in  createQueryResult(), we will skip the entry
            // initialization if the backing map is not evicting
            boolean fInit    = storage.isPotentiallyEvicting();
            Map     mapPrime = storage.getBackingInternalCache();

            if (iterator == null)
                {
                // first time initialization
                iterator = advanceIterator();
                }

            while (iterator != null)
                {
                while (iterator.hasNext())
                    {
                    Binary binKey = (Binary) iterator.next();

                    if (binKey == null)
                        {
                        // null indicates "end of the underlying array"
                        break;
                        }

                    int c = getCount();
                    setCount(c + 1);
                    if ((c & 0x3FF) == 0x3FF)
                        {
                        storage.getService().checkInterrupt();
                        }

                    Binary binValue = null;
                    if (fInit)
                        {
                        binValue = (Binary) mapPrime.get(binKey);
                        if (binValue == null) // must've expired
                            {
                            continue;
                            }
                        }

                    if (entry == null)
                        {
                        entry = storage.instantiateBinaryEntry(binKey, binValue, true);
                        if (isReuseAllowed())
                            {
                            setEntryTemp(entry);
                            }
                        }
                    else
                        {
                        entry.reset(binKey, binValue); // read-only is preserved
                        }

                    if (filter == null || InvocableMapHelper.evaluateEntry(filter, entry))
                        {
                        return entry;
                        }
                    }

                iterator = advanceIterator();
                }

            setEntryTemp(null);
            return null;
            }

        protected java.util.Iterator advanceIterator()
            {
            // import com.tangosol.util.SimpleEnumerator;
            // import java.util.Iterator;

            for (int iPart = getPartitions().next(getNextPartition()); iPart >= 0 ;
                 iPart = getPartitions().next(iPart + 1))
                {
                Object[] aoKeys = getStorage().collectKeys(iPart, getKeys());

                if (aoKeys.length > 0)
                    {
                    setNextPartition(iPart + 1);
                    setKeys(aoKeys);

                    Iterator iterator = new SimpleEnumerator(aoKeys);
                    setIterator(iterator);
                    return iterator;
                    }
                }

            setKeys(null);
            setIterator(null);
            return null;
            }

        // From interface: com.tangosol.util.Streamer
        public int characteristics()
            {
            // import com.tangosol.util.Streamer;

            return getFilter() == null ? (Streamer.SIZED | Streamer.ALL_INCLUSIVE) : 0;
            }

        // Accessor for the property "Count"
        /**
         * Getter for property Count.<p>
         * The number of processed elements.
         */
        private int getCount()
            {
            return __m_Count;
            }

        // Accessor for the property "EntryTemp"
        /**
         * Getter for property EntryTemp.<p>
         * A single entry per Scanner (if ReuseAllowed is true).
         */
        private BinaryEntry getEntryTemp()
            {
            return __m_EntryTemp;
            }

        // Accessor for the property "Filter"
        /**
         * Getter for property Filter.<p>
         * The filter used by the corresponding query. This property is most
         * commonly null, except for non-IndexAware filters.
         */
        protected com.tangosol.util.Filter getFilter()
            {
            return __m_Filter;
            }

        // Accessor for the property "Iterator"
        /**
         * Getter for property Iterator.<p>
         * An iterator that contains keys or inflated entries.
         */
        private java.util.Iterator getIterator()
            {
            return __m_Iterator;
            }

        // Accessor for the property "Keys"
        /**
         * Getter for property Keys.<p>
         * An array of keys; used to reduce the array allocations.
         */
        private Object[] getKeys()
            {
            return __m_Keys;
            }

        // Accessor for the property "NextPartition"
        /**
         * Getter for property NextPartition.<p>
         * The next partition to be scanned.
         */
        private int getNextPartition()
            {
            return __m_NextPartition;
            }

        // Accessor for the property "Partitions"
        /**
         * Getter for property Partitions.<p>
         * PartitionSet for all partitions to be scanned that require
         * evaluation.
         */
        protected com.tangosol.net.partition.PartitionSet getPartitions()
            {
            return __m_Partitions;
            }

        // Accessor for the property "Storage"
        /**
         * Getter for property Storage.<p>
         */
        protected Storage getStorage()
            {
            return (Storage) get_Parent();
            }

        // Accessor for the property "ReuseAllowed"
        /**
         * Getter for property ReuseAllowed.<p>
         * If true, the $BinartEntry object could be reused across the calls.
         */
        protected boolean isReuseAllowed()
            {
            return __m_ReuseAllowed;
            }

        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called
         * out of setConstructed() for the topmost component and that in
         * turn notifies all the children.
         *
         * This notification gets called before the control returns back to
         * this component instantiator (using <code>new Component.X()</code>
         * or <code>_newInstance(sName)</code>) and on the same thread. In
         * addition, visual components have a "posted" notification
         * <code>onInitUI</code> that is called after (or at the same time
         * as) the control returns back to the instantiator and possibly on
         * a different thread.
         */
        public void onInit()
            {
            setKeys(new Object[0]);

            super.onInit();
            }

        // Accessor for the property "Count"
        /**
         * Setter for property Count.<p>
         * The number of processed elements.
         */
        private void setCount(int c)
            {
            __m_Count = c;
            }

        // Accessor for the property "EntryTemp"
        /**
         * Setter for property EntryTemp.<p>
         * A single entry per Scanner (if ReuseAllowed is true).
         */
        private void setEntryTemp(BinaryEntry entryTemp)
            {
            __m_EntryTemp = entryTemp;
            }

        // Accessor for the property "Filter"
        /**
         * Setter for property Filter.<p>
         * The filter used by the corresponding query. This property is most
         * commonly null, except for non-IndexAware filters.
         */
        public void setFilter(com.tangosol.util.Filter filter)
            {
            __m_Filter = filter;
            }

        // Accessor for the property "Iterator"
        /**
         * Setter for property Iterator.<p>
         * An iterator that contains keys or inflated entries.
         */
        private void setIterator(java.util.Iterator iter)
            {
            __m_Iterator = iter;
            }

        // Accessor for the property "Keys"
        /**
         * Setter for property Keys.<p>
         * An array of keys; used to reduce the array allocations.
         */
        private void setKeys(Object[] aoKeys)
            {
            __m_Keys = aoKeys;
            }

        // Accessor for the property "NextPartition"
        /**
         * Setter for property NextPartition.<p>
         * The next partition to be scanned.
         */
        private void setNextPartition(int nPart)
            {
            __m_NextPartition = nPart;
            }

        // Accessor for the property "Partitions"
        /**
         * Setter for property Partitions.<p>
         * PartitionSet for all partitions to be scanned that require
         * evaluation.
         */
        public void setPartitions(com.tangosol.net.partition.PartitionSet parts)
            {
            __m_Partitions = parts;
            }

        // Accessor for the property "ReuseAllowed"
        /**
         * Setter for property ReuseAllowed.<p>
         * If true, the $BinartEntry object could be reused across the calls.
         */
        public void setReuseAllowed(boolean fReuse)
            {
            __m_ReuseAllowed = fReuse;
            }

        // From interface: com.tangosol.util.Streamer
        public long size()
            {
            return getStorage().calculateSize(getPartitions(), true);
            }
        }
    }
