
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache;

import com.tangosol.coherence.component.net.Cluster;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.Poll;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.TimeHelper;
import com.oracle.coherence.common.base.Timeout;
import com.tangosol.internal.net.topic.SimpleChannelAllocationStrategy;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicConfigMap;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.net.CacheService;
import com.tangosol.net.RequestPolicyException;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.ServiceDependencies;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.internal.ScopedTopicReferenceStore;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.TopicException;
import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.LongArray;
import com.tangosol.util.TaskDaemon;
import com.tangosol.util.ValueExtractor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * See PartitionedCacheService.doc in the main depot
 * (//dev/main/doc/coherence-core).
 * 
 * The message range from [51, 100] is reserved for usage by the
 * PartitionedCache component.
 * 
 * Currently used MessageTypes:
 * 51    AggregateAllRequest
 * 52    AggregateFilterRequest
 * 53    BackupAllRequest
 * 54    ClearRequest
 * 55    ContainsAllRequest
 * 56    ContainsKeyRequest
 * 57    ContainsValueRequest
 * 58    GetAllRequest
 * 59    GetRequest
 * 60    IndexRequest
 * 61    InvokeAllRequest
 * 62    InvokeFilterRequest
 * 63    InvokeRequest
 * 64    KeyIteratorRequest
 * 65    KeyListenerRequest
 * 66    ListenerRequest
 * 67    LockRequest
 * 68    MapEvent
 * 69    PartialMapResponse
 * 70    PartialValueResponse
 * 71    PutAllRequest
 * 72    PutRequest
 * 73    QueryRequest
 * 74    QueryResponse
 * 75    RemoveAllRequest
 * 76    RemoveRequest
 * 77    SizeRequest
 * 78    StorageIdRequest
 * 79    UnlockRequest
 * 80    BackupSingleRequest
 * 81    BackupLockRequest
 * 82    BackupListenerRequest
 * 83    StorageConfirmRequest
 * 84    UpdateIndexRequest
 * 85    KeyListenerAllRequest
 * 86    BackupListenerAllRequest
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class PagedTopic
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache
        implements com.tangosol.net.PagedTopicService
    {
    // ---- Fields declarations ----
    
    /**
     * Property ChannelAllocationStrategy
     *
     */
    private com.tangosol.internal.net.topic.ChannelAllocationStrategy __m_ChannelAllocationStrategy;
    
    /**
     * Property ChannelCountExecutor
     *
     */
    private com.tangosol.util.TaskDaemon __m_ChannelCountExecutor;
    
    /**
     * Property CONFIG_MAP_TOPIC
     *
     */
    public static final int CONFIG_MAP_TOPIC = 3;
    
    /**
     * Property ScopedTopicStore
     *
     */
    private com.tangosol.net.internal.ScopedTopicReferenceStore __m_ScopedTopicStore;
    
    /**
     * Property SubscriptionArray
     *
     */
    private com.tangosol.util.LongArray __m_SubscriptionArray;
    
    /**
     * Property SubscriptionGraveyard
     *
     */
    private java.util.Map __m_SubscriptionGraveyard;
    
    /**
     * Property SubscriptionLock
     *
     */
    private java.util.concurrent.locks.ReentrantLock __m_SubscriptionLock;
    
    /**
     * Property SUID_SUBSCRIPTION
     *
     */
    public static final int SUID_SUBSCRIPTION = 4;
    
    /**
     * Property TopicConfigMap
     *
     */
    private PagedTopic.TopicConfig.Map __m_TopicConfigMap;
    
    /**
     * Property TopicStoreLock
     *
     */
    private java.util.concurrent.locks.ReentrantLock __m_TopicStoreLock;
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
        __mapChildren.put("Acknowledgement", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Acknowledgement.get_CLASS());
        __mapChildren.put("AggregateAllRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.AggregateAllRequest.get_CLASS());
        __mapChildren.put("AggregateFilterRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.AggregateFilterRequest.get_CLASS());
        __mapChildren.put("BackingMapContext", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BackingMapContext.get_CLASS());
        __mapChildren.put("BackupAllRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BackupAllRequest.get_CLASS());
        __mapChildren.put("BackupAssignment", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.BackupAssignment.get_CLASS());
        __mapChildren.put("BackupConfirmRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BackupConfirmRequest.get_CLASS());
        __mapChildren.put("BackupListenerAllRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BackupListenerAllRequest.get_CLASS());
        __mapChildren.put("BackupListenerRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BackupListenerRequest.get_CLASS());
        __mapChildren.put("BackupLockRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BackupLockRequest.get_CLASS());
        __mapChildren.put("BackupSingleRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BackupSingleRequest.get_CLASS());
        __mapChildren.put("BatchContext", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BatchContext.get_CLASS());
        __mapChildren.put("BinaryMap", PagedTopic.BinaryMap.get_CLASS());
        __mapChildren.put("BusEventMessage", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.BusEventMessage.get_CLASS());
        __mapChildren.put("CentralDistribution", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.CentralDistribution.get_CLASS());
        __mapChildren.put("ClearRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ClearRequest.get_CLASS());
        __mapChildren.put("ConfigRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest.get_CLASS());
        __mapChildren.put("ConfigResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigResponse.get_CLASS());
        __mapChildren.put("ConfigSync", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync.get_CLASS());
        __mapChildren.put("ConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigUpdate.get_CLASS());
        __mapChildren.put("ContainsAllRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ContainsAllRequest.get_CLASS());
        __mapChildren.put("ContainsKeyRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ContainsKeyRequest.get_CLASS());
        __mapChildren.put("ContainsValueRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ContainsValueRequest.get_CLASS());
        __mapChildren.put("Contention", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.Contention.get_CLASS());
        __mapChildren.put("ConverterFromBinary", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ConverterFromBinary.get_CLASS());
        __mapChildren.put("ConverterKeyToBinary", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.ConverterKeyToBinary.get_CLASS());
        __mapChildren.put("ConverterValueToBinary", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ConverterValueToBinary.get_CLASS());
        __mapChildren.put("DispatchEvent", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.DispatchEvent.get_CLASS());
        __mapChildren.put("DispatchNotification", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchNotification.get_CLASS());
        __mapChildren.put("DistributionPlanUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.DistributionPlanUpdate.get_CLASS());
        __mapChildren.put("DistributionRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.DistributionRequest.get_CLASS());
        __mapChildren.put("EnsureChannelCountTask", PagedTopic.EnsureChannelCountTask.get_CLASS());
        __mapChildren.put("GetAllRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.GetAllRequest.get_CLASS());
        __mapChildren.put("GetRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.GetRequest.get_CLASS());
        __mapChildren.put("IndexRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.IndexRequest.get_CLASS());
        __mapChildren.put("InvocationContext", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.InvocationContext.get_CLASS());
        __mapChildren.put("InvokeAllRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.InvokeAllRequest.get_CLASS());
        __mapChildren.put("InvokeFilterRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.InvokeFilterRequest.get_CLASS());
        __mapChildren.put("InvokeRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.InvokeRequest.get_CLASS());
        __mapChildren.put("KeyIteratorRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.KeyIteratorRequest.get_CLASS());
        __mapChildren.put("KeyListenerAllRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.KeyListenerAllRequest.get_CLASS());
        __mapChildren.put("KeyListenerRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.KeyListenerRequest.get_CLASS());
        __mapChildren.put("ListenerRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ListenerRequest.get_CLASS());
        __mapChildren.put("LockRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.LockRequest.get_CLASS());
        __mapChildren.put("MapEvent", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.MapEvent.get_CLASS());
        __mapChildren.put("MemberConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.MemberConfigUpdate.get_CLASS());
        __mapChildren.put("MemberJoined", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberJoined.get_CLASS());
        __mapChildren.put("MemberWelcome", PagedTopic.MemberWelcome.get_CLASS());
        __mapChildren.put("MemberWelcomeRequest", PagedTopic.MemberWelcomeRequest.get_CLASS());
        __mapChildren.put("MemberWelcomeRequestTask", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequestTask.get_CLASS());
        __mapChildren.put("NotifyConnectionClose", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionClose.get_CLASS());
        __mapChildren.put("NotifyConnectionOpen", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionOpen.get_CLASS());
        __mapChildren.put("NotifyMemberJoined", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberJoined.get_CLASS());
        __mapChildren.put("NotifyMemberLeaving", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeaving.get_CLASS());
        __mapChildren.put("NotifyMemberLeft", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeft.get_CLASS());
        __mapChildren.put("NotifyMessageReceipt", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMessageReceipt.get_CLASS());
        __mapChildren.put("NotifyPollClosed", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyPollClosed.get_CLASS());
        __mapChildren.put("NotifyResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyResponse.get_CLASS());
        __mapChildren.put("NotifyServiceAnnounced", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceAnnounced.get_CLASS());
        __mapChildren.put("NotifyServiceJoining", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining.get_CLASS());
        __mapChildren.put("NotifyServiceLeaving", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.NotifyServiceLeaving.get_CLASS());
        __mapChildren.put("NotifyServiceLeft", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeft.get_CLASS());
        __mapChildren.put("NotifyServiceQuiescence", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.NotifyServiceQuiescence.get_CLASS());
        __mapChildren.put("NotifyShutdown", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.NotifyShutdown.get_CLASS());
        __mapChildren.put("NotifySnapshotRecoverRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.NotifySnapshotRecoverRequest.get_CLASS());
        __mapChildren.put("NotifyStartup", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyStartup.get_CLASS());
        __mapChildren.put("OwnershipRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.OwnershipRequest.get_CLASS());
        __mapChildren.put("OwnershipResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.OwnershipResponse.get_CLASS());
        __mapChildren.put("PartialMapResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PartialMapResponse.get_CLASS());
        __mapChildren.put("PartialValueResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PartialValueResponse.get_CLASS());
        __mapChildren.put("PartitionAbandonRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PartitionAbandonRequest.get_CLASS());
        __mapChildren.put("PartitionControl", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PartitionControl.get_CLASS());
        __mapChildren.put("PartitionFilter", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PartitionFilter.get_CLASS());
        __mapChildren.put("PartitionRecoverCleanup", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PartitionRecoverCleanup.get_CLASS());
        __mapChildren.put("PartitionRecoverRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PartitionRecoverRequest.get_CLASS());
        __mapChildren.put("PartitionStatsUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PartitionStatsUpdate.get_CLASS());
        __mapChildren.put("PartitionSwapRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PartitionSwapRequest.get_CLASS());
        __mapChildren.put("PartitionVersionSyncRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PartitionVersionSyncRequest.get_CLASS());
        __mapChildren.put("PersistenceControl", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PersistenceControl.get_CLASS());
        __mapChildren.put("PingRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PingRequest.get_CLASS());
        __mapChildren.put("PinningIterator", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PinningIterator.get_CLASS());
        __mapChildren.put("ProtocolContext", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ProtocolContext.get_CLASS());
        __mapChildren.put("PutAllRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PutAllRequest.get_CLASS());
        __mapChildren.put("PutRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.PutRequest.get_CLASS());
        __mapChildren.put("QueryRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.QueryRequest.get_CLASS());
        __mapChildren.put("QueryResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.QueryResponse.get_CLASS());
        __mapChildren.put("RemoveAllRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.RemoveAllRequest.get_CLASS());
        __mapChildren.put("RemoveRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.RemoveRequest.get_CLASS());
        __mapChildren.put("Response", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Response.get_CLASS());
        __mapChildren.put("ResultInfo", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ResultInfo.get_CLASS());
        __mapChildren.put("SendBackupsTask", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.SendBackupsTask.get_CLASS());
        __mapChildren.put("SetChannelCountRequest", PagedTopic.SetChannelCountRequest.get_CLASS());
        __mapChildren.put("SizeRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.SizeRequest.get_CLASS());
        __mapChildren.put("SnapshotArchiveRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.SnapshotArchiveRequest.get_CLASS());
        __mapChildren.put("SnapshotListRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.SnapshotListRequest.get_CLASS());
        __mapChildren.put("SnapshotRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.SnapshotRequest.get_CLASS());
        __mapChildren.put("Storage", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.Storage.get_CLASS());
        __mapChildren.put("StorageConfirmRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.StorageConfirmRequest.get_CLASS());
        __mapChildren.put("StorageIdRequest", PagedTopic.StorageIdRequest.get_CLASS());
        __mapChildren.put("SubscriberConfirmRequest", PagedTopic.SubscriberConfirmRequest.get_CLASS());
        __mapChildren.put("SubscriberIdRequest", PagedTopic.SubscriberIdRequest.get_CLASS());
        __mapChildren.put("TransferRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.TransferRequest.get_CLASS());
        __mapChildren.put("TransferResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.TransferResponse.get_CLASS());
        __mapChildren.put("UnlockRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.UnlockRequest.get_CLASS());
        __mapChildren.put("UpdateIndexRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.UpdateIndexRequest.get_CLASS());
        __mapChildren.put("ViewMap", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ViewMap.get_CLASS());
        __mapChildren.put("WrapperGuardable", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.WrapperGuardable.get_CLASS());
        }
    
    // Default constructor
    public PagedTopic()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public PagedTopic(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setAcceptingClients(false);
            setAcceptingOthers(false);
            setBackupAllEnvelopeSize(new java.util.concurrent.atomic.AtomicInteger());
            setBackupCount(0);
            setBackupCountOpt(0);
            setBackupSingleEnvelopeSize(new java.util.concurrent.atomic.AtomicInteger());
            setBinaryMapArray(new com.tangosol.util.CopyOnWriteLongArray());
            setDaemonState(0);
            setDefaultGuardRecovery(0.9F);
            setDefaultGuardTimeout(60000L);
            setDistributionNextMillis(9223372036854775807L);
            setDistributionRepeatMillis(2000);
            setDistributionSynchronized(true);
            setFinalizing(false);
            setLeaseGranularity(0);
            setLockingNextMillis(9223372036854775807L);
            setMessageClassMap(new java.util.HashMap());
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            setOldestPendingRequestSUIDCounter(new java.util.concurrent.atomic.AtomicLong());
            setOwnershipEnabled(true);
            setPartitionListeners(new com.tangosol.util.Listeners());
            setPendingIndexUpdate(new java.util.concurrent.ConcurrentLinkedQueue());
            setProcessedEvents(new com.tangosol.util.SparseArray());
            setReferencesBinaryMap(new com.tangosol.util.SafeHashMap());
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setScopedCacheStore(new com.tangosol.net.internal.ScopedCacheReferenceStore());
            setScopedTopicStore(new com.tangosol.net.internal.ScopedTopicReferenceStore());
            setSerializerMap(new java.util.WeakHashMap());
            setStandardLeaseMillis(0L);
            setStatsIndexingTime(new java.util.concurrent.atomic.AtomicLong());
            setStorageArray(new com.tangosol.util.CopyOnWriteLongArray());
            setStrictPartitioning(true);
            setSubscriptionArray(new com.tangosol.util.CopyOnWriteLongArray());
            setSubscriptionLock(new java.util.concurrent.locks.ReentrantLock());
            setSuspendPollLimit(new java.util.concurrent.atomic.AtomicLong());
            setTLOContext(new java.lang.ThreadLocal());
            setTLORecoveryThread(new java.lang.ThreadLocal());
            setTopicStoreLock(new java.util.concurrent.locks.ReentrantLock());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.Continuations("Continuations", this, true), "Continuations");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.DaemonPool("DaemonPool", this, true), "DaemonPool");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.EventDispatcher("EventDispatcher", this, true), "EventDispatcher");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.EventsHelper("EventsHelper", this, true), "EventsHelper");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Guard("Guard", this, true), "Guard");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.LazyLookup("LazyLookup", this, true), "LazyLookup");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.MemberConfigListener("MemberConfigListener", this, true), "MemberConfigListener");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PartitionConfig("PartitionConfig", this, true), "PartitionConfig");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PollArray("PollArray", this, true), "PollArray");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ReceiveQueue("ReceiveQueue", this, true), "ReceiveQueue");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.RequestCoordinator("RequestCoordinator", this, true), "RequestCoordinator");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ResourceCoordinator("ResourceCoordinator", this, true), "ResourceCoordinator");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ServiceConfig("ServiceConfig", this, true), "ServiceConfig");
        _addChild(new PagedTopic.TopicConfig("TopicConfig", this, true), "TopicConfig");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.TransferControl("TransferControl", this, true), "TransferControl");
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    // Getter for virtual constant ServiceType
    public String getServiceType()
        {
        return "PagedTopic";
        }
    
    // Getter for virtual constant SUIDCounterLength
    public int getSUIDCounterLength()
        {
        return 6;
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic".replace('/', '.'));
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
    
    protected boolean confirmSubscriber(long lSubscription, com.tangosol.internal.net.topic.impl.paged.model.SubscriberId subscriberId)
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicConfigMap;
        
        return PagedTopicConfigMap.hasSubscription(getTopicConfigMap(), lSubscription, subscriberId);
        }
    
    protected boolean confirmSubscriber(String sTopicName, long lSubscription, com.tangosol.internal.net.topic.impl.paged.model.SubscriberId subscriberId)
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches$Names as com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names;
        // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
        // import java.util.Map;
        
        boolean fConfirmed = false;
        
        if (lSubscription != 0)
            {
            String     sCacheName    = com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names.SUBSCRIPTIONS.cacheNameForTopicName(sTopicName);
            Map        mapRefsBinary = getReferencesBinaryMap();
            PagedTopic.BinaryMap mapBinary     = (PagedTopic.BinaryMap) mapRefsBinary.get(sCacheName);
        
            fConfirmed = mapBinary != null && mapBinary.confirmSubscriber(lSubscription, subscriberId);
            }
        
        return fConfirmed;
        }
    
    // Declared at the super level
    /**
     * Return an XmlElement that holds cache info; specifically the cache name
    * and id.
     */
    public com.tangosol.run.xml.XmlElement createCacheInfo(String sCacheName, long lCacheId)
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches$Names as com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
        // import com.tangosol.run.xml.XmlElement;
        
        XmlElement xmlCacheInfo = super.createCacheInfo(sCacheName, lCacheId);
        
        String                      sTopicName = com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names.getTopicName(sCacheName);
        PagedTopicBackingMapManager mgr        = (PagedTopicBackingMapManager) getTopicBackingMapManager();
        PagedTopicDependencies      deps       = mgr.getTopicDependencies(sTopicName);
        
        xmlCacheInfo.addAttribute("channels").setInt(deps.getConfiguredChannelCount());
        
        return xmlCacheInfo;
        }
    
    protected com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription createSubscription(String sTopicName, com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId groupId, long lSubscriptionId, com.tangosol.util.Filter filter, com.tangosol.util.ValueExtractor extractor)
        {
        PagedTopicSubscription subscription = new PagedTopicSubscription();
        subscription.setKey(sTopicName, groupId);
        subscription.setSubscriptionId(lSubscriptionId);
        subscription.setFilter(filter);
        subscription.setConverter(extractor);
        return subscription;
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public void destroySubscriberGroup(String sTopicName, String sGroupName)
        {
        // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
        
        SubscriberGroupId groupId       = SubscriberGroupId.withName(sGroupName);
        long              lSubscription = getSubscriptionId(sTopicName, groupId);
        
        if (lSubscription > 0)
            {
            destroySubscription(lSubscription);
            }
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public void destroySubscription(long lSubscriptionId)
        {
        destroySubscription(lSubscriptionId, null);
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public void destroySubscription(long lSubscriptionId, com.tangosol.net.topic.Subscriber.Id id)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
        
        if (id == null || id instanceof SubscriberId)
            {
            com.tangosol.coherence.component.net.Member memberCoordinator = getServiceOldestMember();
            if (isVersionCompatible(memberCoordinator, 22, 6, 4))
                {
                SubscriberId subscriberId = (SubscriberId) id;
        
                PagedTopic.SubscriberIdRequest msg = (PagedTopic.SubscriberIdRequest) instantiateMessage("SubscriberIdRequest");
        
                // only the Service senior is allowed to create or destroy the subscription
                msg.addToMember(getServiceOldestMember());
                msg.setSubscriptionId(lSubscriptionId);
                msg.setSubscriberAction(PagedTopic.SubscriberIdRequest.SUBSCRIBER_DESTROY);
        
                if (subscriberId == null)
                    {
                    msg.setSubscriberIds(new SubscriberId[0]);
                    }
                else
                    {
                    msg.setSubscriberIds(new SubscriberId[]{subscriberId});
                    }
        
                poll(msg);
                }
            }
        else
            {
            throw new IllegalArgumentException("Id must be an instance of " + SubscriberId.class + " but is " + id.getClass());
            }
        }
    
    public boolean destroySubscriptionInternal(long lSubscriptionId)
        {
        // import com.tangosol.net.management.MBeanHelper;
        // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
        // import com.tangosol.util.LongArray;
        // import java.util.concurrent.locks.ReentrantLock;
        
        _assert(lSubscriptionId != 0);
        
        PagedTopic                service       = (PagedTopic) get_Module();
        LongArray              aSubscription = getSubscriptionArray();
        PagedTopicSubscription subscription  = (PagedTopicSubscription) aSubscription.get(lSubscriptionId);
        
        if (subscription != null)
            {
            ReentrantLock lock = getSubscriptionLock();
            lock.lock();
            try
                {
                subscription = (PagedTopicSubscription) aSubscription.get(lSubscriptionId);
                if (subscription != null)
                    {
                    MBeanHelper.unregisterSubscriberGroupMBean(this, subscription);
                    getSubscriptionGraveyard().put(Long.valueOf(lSubscriptionId), subscription.getKey());
                    aSubscription.remove(lSubscriptionId);
                    return true;
                    }
                
                }
            finally
                {
                lock.unlock();
                }
            }
        
        return false;
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public void destroyTopic(com.tangosol.net.topic.NamedTopic topic)
        {
        // import java.util.concurrent.locks.ReentrantLock;
        
        ReentrantLock lock = getTopicStoreLock();
        lock.lock();
        try
            {
            getScopedTopicStore().releaseTopic(topic);
            }
        finally
            {
            lock.unlock();
            }
        topic.destroy();
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public int ensureChannelCount(String sTopic, int cChannel)
        {
        return ensureChannelCount(sTopic, cChannel, cChannel);
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public int ensureChannelCount(String sTopic, int cRequired, int cChannel)
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import com.oracle.coherence.common.base.Exceptions;
        // import com.oracle.coherence.common.base.TimeHelper;
        // import com.oracle.coherence.common.base.Timeout;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.ServiceDependencies;
        // import Component.Net.Member;
        
        int cActual = getChannelCountFromConfigMap(sTopic);
        if (cActual < cRequired)
            {
            PagedTopic.SetChannelCountRequest msg = (PagedTopic.SetChannelCountRequest) instantiateMessage("SetChannelCountRequest");
            Member member = getServiceOldestMember();
        
            // senior must be >= 22.06.2 but not 22.09.0
            if (isVersionCompatible(member, 22, 9, 1)
                    || (isVersionCompatible(member, 22, 6, 3) && !isVersionCompatible(member, 22, 9, 0)))
                {
                msg.addToMember(getServiceOldestMember());
                msg.setTopicName(sTopic);
                msg.setRequiredCount(cRequired);
                msg.setChannelCount(cChannel);
        
                send(msg);
        
                Logger.config("Request for increase of channel count for topic \"" + sTopic + "\" from "
                    + cActual + " to " + cChannel + " sent to senior member " + member);
        
                ServiceDependencies deps = getDependencies();
        
                long cRequestTimeout = deps == null ? 0l : deps.getRequestTimeoutMillis();
                long cStart = TimeHelper.getSafeTimeMillis();
                long cTimeout = Timeout.isSet()
                        ? Timeout.remainingTimeoutMillis()
                        : cRequestTimeout;
        
                if (cTimeout <= 0)
                    {
                    cTimeout = 5L * 60L * 1000L;
                    }
        
                cActual = getChannelCountFromConfigMap(sTopic);
                while (cActual < cRequired)
                    {
                    if (TimeHelper.getSafeTimeMillis() - cStart > cTimeout)
                        {
                        throw new RequestTimeoutException("Timed out waiting for topic channel count to be set to " + cRequired);
                        }
                    try
                        {
                        Blocking.sleep(10L);
                        }
                    catch (InterruptedException e)
                        {
                        Thread.currentThread().interrupt();
                        throw Exceptions.ensureRuntimeException(e);
                        }
                    cActual = getChannelCountFromConfigMap(sTopic);
                    }
                }
            else
                {
                Logger.fine("Cannot ensure publisher channel count, senior member is not version compatible " + member);
                cActual = getChannelCount(sTopic);
                }
            }
        
        return cActual;
        }
    
    public void ensureKnownTopics()
        {
        // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
        // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription$Key as com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key;
        // import java.util.Map;;
        // import java.util.Map;
        // import java.util.Iterator;
        
        Map map = getTopicConfigMap();
        for (Iterator it = map.keySet().iterator(); it.hasNext(); )
            {
            Object oKey = it.next();
            if (oKey instanceof com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key)
                {
                ensureSubscription((PagedTopicSubscription) map.get(oKey));
                }
            }
        }
    
    protected com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches ensurePagedTopicCaches(String sTopicName)
        {
        return null;
        }
    
    // Declared at the super level
    /**
     * Either ensure $Storage is created or a $BinaryMap and dispatch relevant
    * events.
    * 
    * @param sName  cache name
    * @param lCacheId  the id of the cache
    * @param fInit  whether this Storage is being created due to initialization
    * and therefore must override any existing $Storage instances with the same
    * name
     */
    public void ensureStorageInternal(String sName, long lCacheId, boolean fInit)
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches$Names as com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names;
        
        super.ensureStorageInternal(sName, lCacheId, fInit);
        
        if (com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names.SUBSCRIPTIONS.isA(sName))
            {
            // we need to also ensure the BimaryMap for Subscriptions as we
            // need to use if for subscriber confirmation calls later
            ensureBinaryMap(sName, lCacheId);
            }
        else if (com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names.CONTENT.isA(sName))
            {
            // we need to also ensure the BimaryMap for Content as we
            // need to use if for topic confirmation calls later
            ensureBinaryMap(sName, lCacheId);
        
            if (isOwnershipEnabled())
                {
                String sTopicName = com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names.getTopicName(sName);
                MBeanHelper.registerPagedTopicMBean(this, sTopicName);
                }
            }
        }
    
    public void ensureSubscription(long lSubscription, com.tangosol.net.topic.Subscriber.Id id)
        {
        }
    
    public long ensureSubscription(com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription subscription)
        {
        // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
        // import com.tangosol.net.management.MBeanHelper;
        // import com.tangosol.util.LongArray;
        // import java.util.concurrent.locks.ReentrantLock;;
        // import java.util.concurrent.locks.ReentrantLock;
        
        long lSubscriptionId = subscription.getSubscriptionId();
        _assert(lSubscriptionId != 0);
        
        PagedTopic   service       = (PagedTopic) get_Module();
        LongArray aSubscription = getSubscriptionArray();
        
        ReentrantLock lock = getSubscriptionLock();
        lock.lock();
        try
            {
            PagedTopicSubscription subCurrent = (PagedTopicSubscription) aSubscription.get(lSubscriptionId);
            if (subCurrent == null)
                {
                subCurrent = new PagedTopicSubscription(subscription);
                aSubscription.set(lSubscriptionId, subCurrent);
        
                if (isOwnershipEnabled() && !subscription.isAnonymous())
                    {
                    // Register the MBean
                    MBeanHelper.registerSubscriberGroupMBean(this, subCurrent);
                    }
                }
            else
                {
                subCurrent.update(subscription);
                }
            }
        finally
            {
            lock.unlock();
            }
        
        return lSubscriptionId;
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public void ensureSubscription(String sTopicName, long lSubscription, com.tangosol.net.topic.Subscriber.Id id)
        {
        // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
        // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
        // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.ValueExtractor;
        
        if (id == null || id instanceof SubscriberId)
            {
            SubscriberId           subscriberId = (SubscriberId) id;
            PagedTopicSubscription subscription = getSubscription(lSubscription);
        
            if (subscription != null && subscription.hasSubscriber(subscriberId))
                {
                return;
                }
        
            do
                {
                PagedTopic.SubscriberIdRequest msg = (PagedTopic.SubscriberIdRequest) instantiateMessage("SubscriberIdRequest");
        
                msg.addToMember(getThisMember());
                msg.setSubscriptionId(lSubscription);
                msg.setSubscriberAction(PagedTopic.SubscriberIdRequest.SUBSCRIBER_CREATE);
        
                if (id == null || SubscriberId.NullSubscriber.equals(id))
                    {
                    msg.setSubscriberIds(new SubscriberId[0]);
                    }
                else
                    {
                    msg.setSubscriberIds(new SubscriberId[]{subscriberId});
                    }
        
                Object oResult = poll(msg);
                if (oResult instanceof Long)
                    {
                    lSubscription = ((Long) oResult).longValue();
                    }
                else if (oResult instanceof Throwable)
                    {
                    throw Exceptions.ensureRuntimeException((Throwable) oResult);
                    }
        
                if (lSubscription == -1L)
                    {
                    // the senior is not version compatible
                    return;
                    }
                }
            while (!confirmSubscriber(sTopicName, lSubscription, subscriberId));
            }
        else
            {
            throw new IllegalArgumentException("Id must be an instance of " + SubscriberId.class + " but is " + id.getClass());
            }
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public long ensureSubscription(String sTopicName, com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId groupId, com.tangosol.net.topic.Subscriber.Id id, com.tangosol.util.Filter filter, com.tangosol.util.ValueExtractor extractor)
        {
        // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
        // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.ValueExtractor;
        
        if (id == null || id instanceof SubscriberId)
            {
            SubscriberId           subscriberId = (SubscriberId) id;
            PagedTopicSubscription subscription = getSubscription(sTopicName, groupId);
        
            if (subscription != null)
                {
                if (filter != null && !Objects.equals(filter, subscription.getFilter()))
                    {
                    // do not allow new subscription request with a different filter
                    throw new TopicException("Cannot change the Filter in existing Subscriber group \""
                            + groupId + "\" current=" + subscription.getFilter() + " new=" + filter);
                    }
                else if (extractor != null && !Objects.equals(subscription.getConverter(), extractor))
                    {
                    // do not allow new subscription request with a different converter function
                    throw new TopicException("Cannot change the ValueExtractor in existing Subscriber group \""
                            + groupId + "\" current=" + subscription.getConverter() + " new=" + extractor);
                    }
        
                if (subscription.hasSubscriber(subscriberId))
                    {
                    return subscription.getSubscriptionId();
                    }
                }
            
            
            long lSubscription = 0L;
            long start         = System.currentTimeMillis();
            long lastTime      = start;
            int  cAttempt      = 0;
        
            do
                {
                lSubscription = getSubscriptionId(sTopicName, groupId);
        
                PagedTopic.SubscriberIdRequest msg = (PagedTopic.SubscriberIdRequest) instantiateMessage("SubscriberIdRequest");
        
                msg.addToMember(getThisMember());
                msg.setTopicName(sTopicName);
                msg.setGroupId(groupId);
                msg.setSubscriptionId(lSubscription);
                msg.setFilter(filter);
                msg.setConverter(extractor);
                msg.setSubscriberAction(PagedTopic.SubscriberIdRequest.SUBSCRIBER_CREATE);
        
                if (id == null || SubscriberId.NullSubscriber.equals(id))
                    {
                    msg.setSubscriberIds(new SubscriberId[0]);
                    }
                else
                    {
                    msg.setSubscriberIds(new SubscriberId[]{subscriberId});
                    }
        
                Object oResult = poll(msg);
                if (oResult instanceof Long)
                    {
                    lSubscription = ((Long) oResult).longValue();
                    }
                else if (oResult instanceof Throwable)
                    {
                    throw Exceptions.ensureRuntimeException((Throwable) oResult);
                    }
        
                if (lSubscription == -1L)
                    {
                    _trace("Could not request subscription id for subscription on topic "
                            + sTopicName + " group " + groupId + " subscriber " + id            
                            + " from senior member " + getServiceOldestMember(), 6); 
                    // the senior is not version compatible
                    return lSubscription;
                    }
        
                cAttempt++;
                long now = System.currentTimeMillis();
                if ((now - lastTime) > 30000)
                    {
                    lastTime = now;
                    long cSeconds = (now - start) / 1000;
                    _trace("This member has been waiting for subscription confirmation for " + cSeconds + " seconds (attempts "
                            + cAttempt + ") for subscription "
                            + lSubscription + " on topic " + sTopicName + " group " + groupId + " subscriber " + id, 7);
                    }
                }
            while (!confirmSubscriber(sTopicName, lSubscription, subscriberId));
        
            return getSubscriptionId(sTopicName, groupId);
            }
        else
            {
            throw new IllegalArgumentException("Id must be an instance of " + SubscriberId.class + " but is " + id.getClass());
            }
        }
    
    protected com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription ensureSubscriptionInternal(String sTopicName, com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId groupId, long lSubscriptionId, com.tangosol.util.Filter filter, com.tangosol.util.ValueExtractor extractor)
        {
        // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
        // import com.tangosol.util.LongArray;
        // import java.util.concurrent.locks.ReentrantLock;
        
        PagedTopicSubscription subscription;
        
        if (lSubscriptionId == 0)
            {
            // the caller did not have a subscription id, but we may already
            // have a subscription with the group name
            lSubscriptionId = getSubscriptionId(sTopicName, groupId);
            }
        
        if (lSubscriptionId == 0)
            {
            // there was no existing subscription, so this is a create
            lSubscriptionId = generateSubscriptionId();
            }
        
        PagedTopic service       = (PagedTopic) get_Module();
        LongArray  aSubscription = getSubscriptionArray();
        
        subscription = (PagedTopicSubscription) aSubscription.get(lSubscriptionId);
        if (subscription == null)
            {
            ReentrantLock lock = getSubscriptionLock();
            lock.lock();
            try
                {
                subscription = (PagedTopicSubscription) aSubscription.get(lSubscriptionId);
        
                if (subscription == null)
                    {
                    subscription = createSubscription(sTopicName, groupId, lSubscriptionId, filter, extractor);
                    aSubscription.set(lSubscriptionId, subscription);
                    if (isOwnershipEnabled() && !subscription.isAnonymous())
                        {
                        // Register the MBean only for durable subscribers
                        MBeanHelper.registerSubscriberGroupMBean(this, subscription);
                        }
                    }
                }
            finally
                {
                lock.unlock();
                }
            }
        
        return (PagedTopicSubscription) aSubscription.get(lSubscriptionId);
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public com.tangosol.net.topic.NamedTopic ensureTopic(String sName, ClassLoader loader)
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopic as com.tangosol.internal.net.topic.impl.paged.PagedTopic;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
        // import com.tangosol.net.internal.ScopedTopicReferenceStore;
        // import com.tangosol.net.management.MBeanHelper;
        // import com.tangosol.net.topic.NamedTopic;
        // import java.util.concurrent.locks.ReentrantLock;
        
        if (!isRunning())
            {
            throw new IllegalStateException("Service is not running: " + this);
            }
        
        if (sName == null || sName.length() == 0)
            {
            sName = "Default";
            }
        
        if (loader == null)
            {
            loader = getContextClassLoader();
            _assert(loader != null, "ContextClassLoader is missing");
            }
        
        // the implementation is optimized for topics that already
        // exist and already have the requested class-loader view
        NamedTopic                topic = null;
        ScopedTopicReferenceStore store = getScopedTopicStore();
        
        topic = (NamedTopic) store.get(sName, loader);
        if (topic != null)
            {
            return topic;
            }
        
        ReentrantLock lock = getTopicStoreLock();
        lock.lock();
        try
            {
            PagedTopicCaches topicCaches = new PagedTopicCaches(sName, this);
            com.tangosol.internal.net.topic.impl.paged.PagedTopic      pagedTopic  = new com.tangosol.internal.net.topic.impl.paged.PagedTopic(topicCaches);
            
            store.put(pagedTopic, loader);
            topic = pagedTopic;
            }
        finally
            {
            lock.unlock();
            }
        return topic;
        }
    
    public long generateSubscriptionId()
        {
        // import com.tangosol.util.LongArray;
        
        // create unique subscription id
        long      lSubscriptionId;
        LongArray laSubscription = getSubscriptionArray();
        PagedTopic   service        = (PagedTopic) get_Module();
        
        do
            {
            lSubscriptionId = service.getSUIDRange(SUID_SUBSCRIPTION, 1);
            }
        while (laSubscription.exists(lSubscriptionId));
        
        return lSubscriptionId;
        }
    
    public long getCacheId(String sTopicName, com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names name)
        {
        // import java.util.Map;
        
        String     sCacheName   = name.cacheNameForTopicName(sTopicName);
        Map        mapConfig    = getServiceConfigMap();
        XmlElement xmlCacheInfo = (XmlElement) mapConfig.get(sCacheName);
        
        return xmlCacheInfo == null ? 0 : xmlCacheInfo.getAttribute("id").getLong();
        }
    
    // Accessor for the property "ChannelAllocationStrategy"
    /**
     * Getter for property ChannelAllocationStrategy.<p>
     */
    public com.tangosol.internal.net.topic.ChannelAllocationStrategy getChannelAllocationStrategy()
        {
        return __m_ChannelAllocationStrategy;
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public int getChannelCount(String sName)
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
        
        int cChannel = getChannelCountFromConfigMap(sName);
        
        if (cChannel <= 0)
            {
            PagedTopicBackingMapManager mgr  = (PagedTopicBackingMapManager) getTopicBackingMapManager();
            PagedTopicDependencies      deps = mgr.getTopicDependencies(sName);
            cChannel = deps.getConfiguredChannelCount();
            }
        
        return cChannel;
        }
    
    // Accessor for the property "ChannelCountExecutor"
    /**
     * Getter for property ChannelCountExecutor.<p>
     */
    public com.tangosol.util.TaskDaemon getChannelCountExecutor()
        {
        // import com.tangosol.util.TaskDaemon;
        
        TaskDaemon daemon = __m_ChannelCountExecutor;
        if (daemon == null)
            {
            daemon = new TaskDaemon("PagedTopic:" + getServiceName() + ":ChannelCountExecutor", Thread.NORM_PRIORITY, true);
            setChannelCountExecutor(daemon);
            }
        return daemon;
        }
    
    public int getChannelCountFromConfigMap(String sName)
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches$Names as com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names;
        // import com.tangosol.run.xml.XmlElement;
        
        String     sCacheName = com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names.CONTENT.cacheNameForTopicName(sName);
        XmlElement xmlElement = (XmlElement) getServiceConfigMap().get(sCacheName);
        return xmlElement == null ? 0 : xmlElement.getSafeAttribute("channels").getInt();
        }
    
    // Declared at the super level
    /**
     * Return the ServiceConfig$Map for the specified map type.
     */
    public com.tangosol.coherence.component.util.ServiceConfig.Map getConfigMap(int nMapType)
        {
        if (nMapType == CONFIG_MAP_TOPIC)
            {
            return getTopicConfigMap();
            }
        else
            {
            return super.getConfigMap(nMapType);
            }
        }
    
    // Declared at the super level
    public com.tangosol.coherence.component.net.MemberSet getConfigMapUpdateMembers(com.tangosol.coherence.component.util.ServiceConfig.Map map)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        
        MemberSet setMember = super.getConfigMapUpdateMembers(map);
        
        if (map.getMapType() == CONFIG_MAP_TOPIC && setMember.size() != 0)
            {
            // return only compatible members if map is topic config
            for (Iterator it = setMember.iterator(); it.hasNext(); )
                {
                Member member = (Member) it.next(); 
                if (!isVersionCompatible(member, 22, 6, 4))
                    {
                    it.remove();
                    }
                }
            }
        return setMember;
        }
    
    // Accessor for the property "ScopedTopicStore"
    /**
     * Getter for property ScopedTopicStore.<p>
     */
    public com.tangosol.net.internal.ScopedTopicReferenceStore getScopedTopicStore()
        {
        return __m_ScopedTopicStore;
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public java.util.Set getSubscriberGroups(String sTopicName)
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicConfigMap;
        
        return PagedTopicConfigMap.getSubscriberGroups(getTopicConfigMap(), sTopicName);
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public java.util.Set getSubscribers(String sTopicName, com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId groupId)
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicConfigMap;
        
        return PagedTopicConfigMap.getSubscribers(getTopicConfigMap(), sTopicName, groupId);
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription getSubscription(long lSubscriptionId)
        {
        // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
        // import com.tangosol.util.LongArray;
        
        LongArray aSubscription = getSubscriptionArray();
        return (PagedTopicSubscription) aSubscription.get(lSubscriptionId);
        }
    
    public com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription getSubscription(String sTopicName, com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId groupId)
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicConfigMap;
        
        if (groupId == null)
            {
            return null;
            }
        return PagedTopicConfigMap.getSubscription(getTopicConfigMap(), sTopicName, groupId);
        }
    
    // Accessor for the property "SubscriptionArray"
    /**
     * Getter for property SubscriptionArray.<p>
     */
    public com.tangosol.util.LongArray getSubscriptionArray()
        {
        return __m_SubscriptionArray;
        }
    
    // Accessor for the property "SubscriptionGraveyard"
    /**
     * Getter for property SubscriptionGraveyard.<p>
     */
    public java.util.Map getSubscriptionGraveyard()
        {
        return __m_SubscriptionGraveyard;
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public long getSubscriptionId(String sTopicName, com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId groupId)
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicConfigMap;
        
        return PagedTopicConfigMap.getSubscriptionId(getTopicConfigMap(), sTopicName, groupId);
        }
    
    public long getSubscriptionId(String sTopicName, String sGroupName)
        {
        return 0L;
        }
    
    // Accessor for the property "SubscriptionLock"
    /**
     * Getter for property SubscriptionLock.<p>
     */
    public java.util.concurrent.locks.ReentrantLock getSubscriptionLock()
        {
        return __m_SubscriptionLock;
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager getTopicBackingMapManager()
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
        
        return (PagedTopicBackingMapManager) getBackingMapManager();
        }
    
    // Accessor for the property "TopicConfigMap"
    /**
     * Getter for property TopicConfigMap.<p>
     */
    public PagedTopic.TopicConfig.Map getTopicConfigMap()
        {
        PagedTopic.TopicConfig.Map map = __m_TopicConfigMap;
        
        if (map == null)
            {
            map = (PagedTopic.TopicConfig.Map) ((PagedTopic.TopicConfig) _findChild("TopicConfig")).getMap();
            setTopicConfigMap(map);
            }
        
        return map;
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public java.util.Set getTopicNames()
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicConfigMap;
        
        return PagedTopicConfigMap.getTopicNames(getTopicConfigMap());
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics getTopicStatistics(String sTopicName)
        {
        return getTopicBackingMapManager().getStatistics(sTopicName);
        }
    
    // Accessor for the property "TopicStoreLock"
    /**
     * Getter for property TopicStoreLock.<p>
     */
    public java.util.concurrent.locks.ReentrantLock getTopicStoreLock()
        {
        return __m_TopicStoreLock;
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public boolean hasSubscription(long lSubscriptionId)
        {
        // import com.tangosol.util.LongArray;
        
        LongArray aSubscription = getSubscriptionArray();
        return aSubscription != null && aSubscription.exists(lSubscriptionId);
        }
    
    // Declared at the super level
    /**
     * Returns {@code true} if this service is compatible with the specified
    * service type.
    * <p>
    * This is mainly used during rolling updgrade where a new but compatible
    * service type is being used.
    * For example when PagedTopic was introduced, this is compatible with the
    * DistributedCache type.
    * 
    * @param sType  the type of this service running on the senior member
    * 
    * @return {@code true} if this service is compatible with the senior
    * member's service type
     */
    public boolean isCompatibleServiceType(String sType)
        {
        // import com.tangosol.net.CacheService;
        
        return getServiceType().equals(sType) || CacheService.TYPE_DISTRIBUTED.equals(sType);
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public boolean isSubscriptionDestroyed(long lSubscriptionId)
        {
        return getSubscriptionGraveyard().containsKey(Long.valueOf(lSubscriptionId));
        }
    
    // Declared at the super level
    /**
     * Check whether the specified members runs a version that precedes the
    * specified one.
     */
    public boolean isVersionCompatible(com.tangosol.coherence.component.net.Member member, int nYear, int nMonth, int nPatch)
        {
        if (nYear == 22 && nMonth == 6 && nPatch == 4)
            {
            // 22.06.4 is versions >= 22.06.4 but not 22.09.0
            return super.isVersionCompatible(member, 22, 9, 1)
                || (super.isVersionCompatible(member, 22, 6, 4) && !super.isVersionCompatible(member, 22, 9, 0));
            }
        return super.isVersionCompatible(member, nYear, nMonth, nPatch);
        }
    
    // Declared at the super level
    /**
     * Event notification called right before the daemon thread terminates. This
    * method is guaranteed to be called only once and on the daemon's thread.
     */
    protected void onExit()
        {
        // detach the topic config listeners
        getTopicConfigMap().getConfig().removeConfigListener();
        
        super.onExit();
        }
    
    // Declared at the super level
    /**
     * Called when the service is AcceptingOthers.
    * 
    * Note: specializations of this event *must* either call this implemenation
    * via super or call setAcceptingClients(true) to notify any clients waiting
    * for the service to start.
     */
    protected void onFinalizeStartup()
        {
        super.onFinalizeStartup();
        
        // ensureKnownTopics() should only be called after AcceptingClients flag is set
        // to true, at which point the client thread performing the service "start" sequence
        // is notified, allowing it to release all acquired monitors (cluster, service, etc).
        // This is critical, since any backing map instantiation caused by ensureKnownTopics()
        // call may attempt to acquire synchronization on the cluster and/or service.
        //
        // While the client thread is now free to perform topic requests, those calls will be
        // blocked waiting for a response from the TopicIdRequest or SubscriberIdRequest poll
        // until the known topics and subscriptions are initialized below
        // (since We are running on the service thread).
        
        ensureKnownTopics();
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification (kind of
    * WM_NCCREATE event) called out of setConstructed() for the topmost
    * component and that in turn notifies all the children. <p>
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as)  the control returns back to the
    * instatiator and possibly on a different thread.
     */
    public void onInit()
        {
        // import com.tangosol.internal.net.topic.SimpleChannelAllocationStrategy;
        
        super.onInit();
        
        setChannelAllocationStrategy(new SimpleChannelAllocationStrategy());
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to true.
    * If the Service has not completed preparing at this point, then the
    * Service must override this implementation and only set AcceptingClients
    * to true when the Service has actually "finished starting".
     */
    public void onServiceStarted()
        {
        // import com.tangosol.net.cache.LocalCache;
        
        // attach the topic config listeners
        getTopicConfigMap().getConfig().attachConfigListener();
        
        long cExpiry = ((Cluster) getCluster()).getClusterService().getDeliveryTimeoutMillis();
        setSubscriptionGraveyard(new LocalCache(LocalCache.DEFAULT_UNITS, (int) cExpiry));
        
        super.onServiceStarted();
        }
    
    public void onSetChannelCountRequest(PagedTopic.SetChannelCountRequest msgRequest)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid$ServiceConfig$Map as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig.Map;
        // import com.oracle.coherence.common.base.Logger;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches$Names as com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names;
        
        int        cRequired  = msgRequest.getRequiredCount();
        String     sTopic     = msgRequest.getTopicName();
        int        cConfigMap = getChannelCountFromConfigMap(sTopic);
        int        cActual;
        
        if (cConfigMap == 0)
            {
            // the channel count is not in the config map, so the senior when the
            // topic was created was an older version. We need to use whatever is
            // the configured channel count for this member.
            cActual = getChannelCount(sTopic);
            }
        else
            {
            cActual = cConfigMap;
            }
        
        if (cRequired > cConfigMap)
            {
            PagedTopic.EnsureChannelCountTask task = (PagedTopic.EnsureChannelCountTask) _newChild("EnsureChannelCountTask");
            task.setTopicName(sTopic);
            task.setRequiredChannelCount(cRequired);
            task.setChannelCount(msgRequest.getChannelCount());
            task.setMember(msgRequest.getFromMember());
            getChannelCountExecutor().executeTask(task);
            }
        }
    
    public void onSubscriberConfirm(PagedTopic.SubscriberConfirmRequest request)
        {
        // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
        // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
        // import com.tangosol.net.partition.PartitionSet;
        // import java.util.Map;
        
        PagedTopic.PartialValueResponse msgResponse =
            (PagedTopic.PartialValueResponse) instantiateMessage("PartialValueResponse");
        
        msgResponse.respondTo(request);
        
        PartitionSet partsMask       = request.getRequestMaskSafe();
        long         lSubscriptionId = request.getSubscriptionId();
        SubscriberId subscriberId    = request.getSubscriberId();          
        
        PagedTopicSubscription subscription = getSubscription(lSubscriptionId);
        if (subscription == null)
            {
            if (getSubscriptionGraveyard().containsKey(Long.valueOf(lSubscriptionId)))
                {
                // mark all partitions as processed, but with an exception
                partsMask.clear();
                msgResponse.setException(new RequestPolicyException(
                    "Group \"" + request.getGroupId() + "\" (id=" + lSubscriptionId + ") has been concurrently destroyed"));
                }
                // else we are rejecting all partitions; this will repeat the request
            }
        else if (subscriberId == null || subscription.hasSubscriber(subscriberId))
            {
            // we have the subscription
            partsMask.remove(collectOwnedPartitions(/*fPrimary*/ true));
            msgResponse.setResult(Boolean.TRUE);
            }
        
        msgResponse.setRejectPartitions(partsMask);
        post(msgResponse);
        }
    
    public void onSubscriberId(PagedTopic.SubscriberIdRequest request)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches$Names as com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names;;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches$Names as com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicConfigMap;
        // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
        // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
        // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
        // import com.tangosol.net.topic.TopicException;
        // import com.tangosol.run.xml.SimpleElement;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.run.xml.XmlHelper;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.ValueExtractor;
        // import java.util.ArrayList;;
        // import java.util.ArrayList;
        // import java.util.Arrays;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.SortedMap;
        // import java.util.HashMap;
        // import java.util.List;
        // import java.util.Map;
        // import java.util.Objects;
        // import java.util.concurrent.locks.ReentrantLock;
        
        long           lSubscriptionId = request.getSubscriptionId();
        SubscriberId[] aSubscriberId   = request.getSubscriberIds();          
        Map            configMap       = getTopicConfigMap();
        PagedTopic.Response      msgResponse     = (PagedTopic.Response) instantiateMessage("Response");
        
        msgResponse.respondTo(request);
        
        com.tangosol.coherence.component.net.Member memberThis        = getThisMember();
        com.tangosol.coherence.component.net.Member memberFrom        = request.getFromMember();
        com.tangosol.coherence.component.net.Member memberCoordinator = getServiceOldestMember();
        
        if (memberThis == memberCoordinator)
            {
            ReentrantLock lock = getSubscriptionLock();
            lock.lock();
            try
                {
                msgResponse.respondTo(request);
        
                PagedTopicSubscription subscription = null;
        
                switch (request.getSubscriberAction())
                    {
                    case PagedTopic.SubscriberIdRequest.SUBSCRIBER_CREATE:
                        Filter         filter     = request.getFilter();
                        ValueExtractor converter  = request.getConverter();
        
                        if (lSubscriptionId == 0)
                            {
                            // the caller did not have a subscription id, but we may already
                            // have a subscription with the group name
                            String            sTopicName = request.getTopicName();
                            SubscriberGroupId groupId    = request.getGroupId();
                            subscription = ensureSubscriptionInternal(sTopicName, groupId, lSubscriptionId, filter, converter);
                            }
                        else
                            {
                            subscription = getSubscription(lSubscriptionId);
                            }
        
                        if (subscription != null)
                            {
                            // we either have a new or existing subscription
                            try
                                {
                                subscription.assertFilterAndConverter(filter, converter);
                                if (aSubscriberId.length > 0)
                                    {
                                    if (subscription.addSubscribers(aSubscriberId))
                                        {
                                        subscription.updateChannelAllocations(getChannelAllocationStrategy(), getChannelCount(subscription.getTopicName()));                            
                                        Logger.finest("Added subscribers " + Arrays.toString(aSubscriberId) + " to subscription " + subscription);
                                        PagedTopicConfigMap.updateSubscription(configMap, subscription);
                                        }
                                    }
                                else
                                    {
                                    // this is just a group creation request
                                    PagedTopicConfigMap.updateSubscription(configMap, subscription);
                                    }
                        
                                msgResponse.setResult(PagedTopic.Response.RESULT_SUCCESS);
                                msgResponse.setValue(Long.valueOf(subscription.getSubscriptionId()));
                                }
                            catch (TopicException error)
                                {
                                msgResponse.setResult(PagedTopic.Response.RESULT_FAILURE);
                                msgResponse.setValue(error);
                                }
                            }
                        else
                            {
                            // there was no subscription for the id in the request
                            String sReason = isSubscriptionDestroyed(lSubscriptionId)
                                    ? "has been destroyed"
                                    : "is invalid";
        
                            IllegalStateException error = new IllegalStateException("The subscriber group id=" + lSubscriptionId + ") " + sReason);
                            msgResponse.setResult(PagedTopic.Response.RESULT_FAILURE);
                            msgResponse.setValue(error);
                            }
                        
                        post(msgResponse);
                        break;
         
                    case PagedTopic.SubscriberIdRequest.SUBSCRIBER_DESTROY:
                        subscription = getSubscription(lSubscriptionId);
                        if (subscription != null)
                            {
                            if (aSubscriberId.length == 0)
                                {
                                // this is a group destroy
                                destroySubscriptionInternal(lSubscriptionId);
                                PagedTopicConfigMap.removeSubscription(configMap, subscription.getKey());
                                Logger.finest("Destroyed subscription " + subscription.getKey());
                                }
                            else if (aSubscriberId.length == 1 && SubscriberId.NullSubscriber.equals(aSubscriberId[0]))
                                {
                                // this is a removal of all subscribers for a group
                                if (subscription.removeAllSubscribers())
                                    {
                                    Logger.finest("Removed all subscribers from subscription " + subscription.getKey());
                                    PagedTopicConfigMap.updateSubscription(configMap, subscription);
                                    }
                                }
                            else
                                {
                                // this is a close/destroy of one or more subscribers, so remove them from the subscription
                                if (subscription.removeSubscribers(aSubscriberId))
                                    {
                                    subscription.updateChannelAllocations(getChannelAllocationStrategy());
                                    PagedTopicConfigMap.updateSubscription(configMap, subscription);
                                    Logger.finest("Removed subscribers from " + Arrays.toString(aSubscriberId)
                                            + " from subscription " + subscription.getKey());
                                    }
                                }
                            }
        
                        msgResponse.setResult(PagedTopic.Response.RESULT_SUCCESS);
                        msgResponse.setValue(Long.valueOf(lSubscriptionId));
                        post(msgResponse);
                        break;
        
                    default:
                        throw new IllegalStateException();
                    }
                }
            finally
                {
                lock.unlock();
                }
            }
        else
            {
            // only the service senior can create the subscription
            if (isVersionCompatible(memberCoordinator, 22, 6, 4))
                {
                PagedTopic.SubscriberIdRequest msg = (PagedTopic.SubscriberIdRequest) request.cloneMessage();
                msg.addToMember(memberCoordinator);
                msg.setResponse(msgResponse);
                post(msg);
                }
            else
                {
                // Senior is not version compatible, send back -1 as the Id result
                msgResponse.setResult(PagedTopic.Response.RESULT_SUCCESS);
                msgResponse.setValue(Long.valueOf(-1L));
                post(msgResponse);
                }
            }
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    public void releaseTopic(com.tangosol.net.topic.NamedTopic topic)
        {
        // import com.tangosol.net.management.MBeanHelper;
        // import java.util.concurrent.locks.ReentrantLock;
        
        ReentrantLock lock = getTopicStoreLock();
        lock.lock();
        try
            {
            getScopedTopicStore().releaseTopic(topic);
            MBeanHelper.unregisterPagedTopicMBean(this, topic);
            }
        finally
            {
            lock.unlock();
            }
        topic.release();
        }
    
    // Declared at the super level
    /**
     * Remove storage of the specified cacheId from the storage array.
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.Storage removeStorage(long lCacheId)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.Storage;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches$Names as com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.Storage storage = super.removeStorage(lCacheId);
        if (storage != null && isOwnershipEnabled())
            {
            String  sName   = storage.getCacheName(); 
            if (com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names.CONTENT.isA(sName))
                {
                String sTopicName = com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names.getTopicName(sName);
                MBeanHelper.unregisterPagedTopicMBean(this, sTopicName);
                }
            }
        
        return storage;
        }
    
    // Accessor for the property "ChannelAllocationStrategy"
    /**
     * Setter for property ChannelAllocationStrategy.<p>
     */
    public void setChannelAllocationStrategy(com.tangosol.internal.net.topic.ChannelAllocationStrategy strategyAllocation)
        {
        __m_ChannelAllocationStrategy = strategyAllocation;
        }
    
    // Accessor for the property "ChannelCountExecutor"
    /**
     * Setter for property ChannelCountExecutor.<p>
     */
    public void setChannelCountExecutor(com.tangosol.util.TaskDaemon daemonExecutor)
        {
        __m_ChannelCountExecutor = daemonExecutor;
        }
    
    // Accessor for the property "ScopedTopicStore"
    /**
     * Setter for property ScopedTopicStore.<p>
     */
    public void setScopedTopicStore(com.tangosol.net.internal.ScopedTopicReferenceStore sProperty)
        {
        __m_ScopedTopicStore = sProperty;
        }
    
    // Accessor for the property "SubscriptionArray"
    /**
     * Setter for property SubscriptionArray.<p>
     */
    public void setSubscriptionArray(com.tangosol.util.LongArray arraySubscription)
        {
        __m_SubscriptionArray = arraySubscription;
        }
    
    // Accessor for the property "SubscriptionGraveyard"
    /**
     * Setter for property SubscriptionGraveyard.<p>
     */
    public void setSubscriptionGraveyard(java.util.Map mapGraveyard)
        {
        __m_SubscriptionGraveyard = mapGraveyard;
        }
    
    // Accessor for the property "SubscriptionLock"
    /**
     * Setter for property SubscriptionLock.<p>
     */
    public void setSubscriptionLock(java.util.concurrent.locks.ReentrantLock lockSubscription)
        {
        __m_SubscriptionLock = lockSubscription;
        }
    
    // Accessor for the property "TopicConfigMap"
    /**
     * Setter for property TopicConfigMap.<p>
     */
    public void setTopicConfigMap(PagedTopic.TopicConfig.Map mapConfig)
        {
        __m_TopicConfigMap = mapConfig;
        }
    
    // Accessor for the property "TopicStoreLock"
    /**
     * Setter for property TopicStoreLock.<p>
     */
    public void setTopicStoreLock(java.util.concurrent.locks.ReentrantLock lockStore)
        {
        __m_TopicStoreLock = lockStore;
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    // Declared at the super level
    /**
     * Hard-stop the Service. Use shutdown() for normal  termination.
     */
    public void stop()
        {
        // import java.util.Collection;
        // import java.util.Iterator;
        // import java.util.concurrent.locks.ReentrantLock;
        
        super.stop();
        
        ReentrantLock lock = getTopicStoreLock();
        lock.lock();
        try
            {
            ScopedTopicReferenceStore store  = getScopedTopicStore();
            Collection                topics = store.getAll();
            Iterator                  it     = topics.iterator();
            
            while (it.hasNext())
                {
                NamedTopic topic = (NamedTopic) it.next();
                store.releaseTopic(topic);
                }
            }
        finally
            {
            lock.unlock();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$BinaryMap
    
    /**
     * The internal view of the distributed cache.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BinaryMap
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap
        {
        // ---- Fields declarations ----
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
            __mapChildren.put("Entry", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap.Entry.get_CLASS());
            __mapChildren.put("EntryAdvancer", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap.EntryAdvancer.get_CLASS());
            __mapChildren.put("KeyAdvancer", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap.KeyAdvancer.get_CLASS());
            __mapChildren.put("KeyRequestStatus", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap.KeyRequestStatus.get_CLASS());
            __mapChildren.put("KeySetRequestStatus", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap.KeySetRequestStatus.get_CLASS());
            __mapChildren.put("MapRequestStatus", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap.MapRequestStatus.get_CLASS());
            __mapChildren.put("PartialRequestStatus", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap.PartialRequestStatus.get_CLASS());
            }
        
        // Default constructor
        public BinaryMap()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public BinaryMap(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setFilterArray(new com.tangosol.util.SparseArray());
                setListenerSupport(new com.tangosol.util.MapListenerSupport());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap.EntrySet("EntrySet", this, true), "EntrySet");
            _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap.KeySet("KeySet", this, true), "KeySet");
            _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap.Values("Values", this, true), "Values");
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.BinaryMap();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$BinaryMap".replace('/', '.'));
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
        
        public boolean confirmSubscriber(long lSubscriptionId, com.tangosol.internal.net.topic.impl.paged.model.SubscriberId subscriberId)
            {
            // import com.tangosol.net.RequestPolicyException;
            // import com.tangosol.net.partition.PartitionSet;
            
            PagedTopic.SubscriberConfirmRequest msg = (PagedTopic.SubscriberConfirmRequest) getService()
                    .instantiateMessage("SubscriberConfirmRequest");
            
            msg.setCacheId(getCacheId());
            msg.setSubscriptionId(lSubscriptionId);
            msg.setSubscriberId(subscriberId);
            
            try
                {
                // we don't care about the responses, just that the request completes
                mergePartialResponse(sendPartitionedRequestByMember(msg, makePartitionSet(), true));
                }
            catch (RequestPolicyException e)
                {
                return false;
                }
            
            return true;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$EnsureChannelCountTask
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EnsureChannelCountTask
            extends    com.tangosol.coherence.Component
            implements Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property ChannelCount
         *
         * The channel count to set if the topic does not have the required
         * number of channels.
         */
        private int __m_ChannelCount;
        
        /**
         * Property Member
         *
         */
        private com.tangosol.coherence.component.net.Member __m_Member;
        
        /**
         * Property RequiredChannelCount
         *
         * The minimum number of channels required.
         */
        private int __m_RequiredChannelCount;
        
        /**
         * Property TopicName
         *
         * The name of the topic to set the channel count for
         */
        private String __m_TopicName;
        
        // Default constructor
        public EnsureChannelCountTask()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public EnsureChannelCountTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.EnsureChannelCountTask();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$EnsureChannelCountTask".replace('/', '.'));
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
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "ChannelCount"
        /**
         * Getter for property ChannelCount.<p>
        * The channel count to set if the topic does not have the required
        * number of channels.
         */
        public int getChannelCount()
            {
            return __m_ChannelCount;
            }
        
        // Accessor for the property "Member"
        /**
         * Getter for property Member.<p>
         */
        public com.tangosol.coherence.component.net.Member getMember()
            {
            return __m_Member;
            }
        
        // Accessor for the property "RequiredChannelCount"
        /**
         * Getter for property RequiredChannelCount.<p>
        * The minimum number of channels required.
         */
        public int getRequiredChannelCount()
            {
            return __m_RequiredChannelCount;
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
        * The parent PagedTopic service.
         */
        public PagedTopic getService()
            {
            return (PagedTopic) get_Module();
            }
        
        // Accessor for the property "TopicName"
        /**
         * Getter for property TopicName.<p>
        * The name of the topic to set the channel count for
         */
        public String getTopicName()
            {
            return __m_TopicName;
            }
        
        // From interface: java.lang.Runnable
        public void run()
            {
            // import Component.Net.Cluster;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid$ServiceConfig$Map as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig.Map;
            // import com.oracle.coherence.common.base.Logger;
            // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches$Names as com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names;
            // import java.util.concurrent.locks.ReentrantLock;
            
            PagedTopic service    = getService();
            String  sTopic     = getTopicName();
            int     cRequired  = getRequiredChannelCount();
            int     cChannel   = getChannelCount();
            int     cConfigMap = service.getChannelCountFromConfigMap(sTopic);
            int     cActual;
            
            if (cConfigMap == 0)
                {
                // the channel count is not in the config map, so the senior when the
                // topic was created was an older version. We need to use whatever is
                // the configured channel count for this member.
                cActual = service.getChannelCount(sTopic);
                }
            else
                {
                cActual = cConfigMap;
                }
            
            if (cRequired > cConfigMap)
                {
                ReentrantLock lock = service.getTopicStoreLock();
                lock.lock();
                try
                    {
                    cConfigMap = service.getChannelCountFromConfigMap(sTopic);
                    if (cRequired > cConfigMap)
                        {
                        String  sServiceName = service.getServiceName();
                        boolean fSuspend     = !service.isSuspendedFully();
            
                        try
                            {
                            if (fSuspend)
                                {
                                ((Cluster) service.getCluster()).suspendService(sServiceName, /*fResumeOnFailover*/ true);
                                }
            
                            String     sCacheName = com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names.CONTENT.cacheNameForTopicName(sTopic);
                            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig.Map      map        = service.getServiceConfigMap(); 
                            XmlElement xmlElement = (XmlElement) map.get(sCacheName);
            
                            xmlElement.getSafeAttribute("channels").setInt(cChannel);
                            map.put(sCacheName, xmlElement);
                            
                            if (cConfigMap != cChannel)
                                {
                                // only log if we actually changed the count, we may have only updated the config map
                                Logger.config("Increased channel count for topic \"" + sTopic + "\" from "
                                    + cActual + " to " + cChannel + " requested by " + getMember());
                                }
                            }
                        finally
                            {
                            if (fSuspend)
                                {
                                ((Cluster) service.getCluster()).resumeService(sServiceName);
                                }
                            }
                        }
                    }
                finally
                    {
                    lock.unlock();
                    }
                }
            }
        
        // Accessor for the property "ChannelCount"
        /**
         * Setter for property ChannelCount.<p>
        * The channel count to set if the topic does not have the required
        * number of channels.
         */
        public void setChannelCount(int nCount)
            {
            __m_ChannelCount = nCount;
            }
        
        // Accessor for the property "Member"
        /**
         * Setter for property Member.<p>
         */
        public void setMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_Member = member;
            }
        
        // Accessor for the property "RequiredChannelCount"
        /**
         * Setter for property RequiredChannelCount.<p>
        * The minimum number of channels required.
         */
        public void setRequiredChannelCount(int nCount)
            {
            __m_RequiredChannelCount = nCount;
            }
        
        // Accessor for the property "TopicName"
        /**
         * Setter for property TopicName.<p>
        * The name of the topic to set the channel count for
         */
        public void setTopicName(String sName)
            {
            __m_TopicName = sName;
            }
        
        // Declared at the super level
        public String toString()
            {
            return "EnsureChannelCountTask(topic=" + getTopicName() + ", requiredCount="
                + getRequiredChannelCount() + ", channelCount=" + getChannelCount() + ")";
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$MemberWelcome
    
    /**
     * This Message is used to welcome a new member into this Service.
     * 
     * Attributes:
     *     MemberConfigMap
     *     ServiceConfigMap  (optional)
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberWelcome
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.MemberWelcome
        {
        // ---- Fields declarations ----
        
        /**
         * Property TopicConfigMap
         *
         */
        private java.util.Map __m_TopicConfigMap;
        
        // Default constructor
        public MemberWelcome()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MemberWelcome(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-2);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.MemberWelcome();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$MemberWelcome".replace('/', '.'));
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
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return super.getDescription() + ", TopicConfig=" + getTopicConfigMap();
            }
        
        // Accessor for the property "TopicConfigMap"
        /**
         * Getter for property TopicConfigMap.<p>
         */
        public java.util.Map getTopicConfigMap()
            {
            return __m_TopicConfigMap;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import java.util.Map;
            
            super.onReceived();
            
            PagedTopic service   = (PagedTopic) getService();
            Map     mapConfig = getTopicConfigMap();
            
            _trace("Received MemberWelcome from member " + getFromMember().getId() +
                   ", topicConfigMap: " + mapConfig, 7);
            
            if (mapConfig != null)
                {
                // set the TopicConfigMap contents
                service.getTopicConfigMap().updateInternal(mapConfig, false);
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Util.ServiceConfig as com.tangosol.coherence.component.util.ServiceConfig;
            // import java.io.IOException;
            // import java.util.HashMap;
            // import java.util.Map;
            
            super.read(input);
            
            if (isRejected())
                {
                return;
                }
            
            PagedTopic service = (PagedTopic) getService();
            
            try
                {
                Map     mapConfig    = new HashMap();
                boolean fTopicConfig = input.available() > 0 && input.readBoolean();
                if (fTopicConfig)
                    {
                    com.tangosol.coherence.component.util.ServiceConfig cfgTopic = service.getTopicConfigMap().getConfig();
            
                    // read the topic config map
                    for (int i = 0, c = input.readInt(); i < c; ++i)
                        {
                        // Note: we use the PagedTopic.TopicConfig to deserialize the contents,
                        //       but we read the map contents into a temporary map for 
                        //       later processing (see #onReceived())
                        Object oKey   = cfgTopic.readObject(input);
                        Object oValue = cfgTopic.readObject(input);
                        mapConfig.put(oKey, oValue);
                        }
                    }
            
                setTopicConfigMap(mapConfig);
                }
            catch (IOException e)
                {
                service.onConfigIOException(e, getFromMember());
                }
            }
        
        // Accessor for the property "TopicConfigMap"
        /**
         * Setter for property TopicConfigMap.<p>
         */
        public void setTopicConfigMap(java.util.Map mapConfig)
            {
            __m_TopicConfigMap = mapConfig;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import Component.Util.ServiceConfig as com.tangosol.coherence.component.util.ServiceConfig;
            // import java.util.Iterator;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            
            super.write(output);
            
            if (isRejected())
                {
                return;
                }
            
            PagedTopic service   = (PagedTopic) getService();
            Map     mapConfig = getTopicConfigMap();
            
            // write the topic config map
            if (mapConfig == null)
                {
                output.writeBoolean(false);
                }
            else
                {
                com.tangosol.coherence.component.util.ServiceConfig cfgTopic = service.getTopicConfigMap().getConfig();
            
                output.writeBoolean(true);
                output.writeInt(mapConfig.size());
                for (Iterator iter = mapConfig.entrySet().iterator(); iter.hasNext(); )
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
            
                    cfgTopic.writeObject(output, entry.getKey());
                    cfgTopic.writeObject(output, entry.getValue());
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$MemberWelcomeRequest
    
    /**
     * This Message is sent to all other Members running this service to
     * request their up-to-date Member ConfigMap for this Service.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberWelcomeRequest
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.MemberWelcomeRequest
        {
        // ---- Fields declarations ----
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
            __mapChildren.put("Poll", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.MemberWelcomeRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public MemberWelcomeRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MemberWelcomeRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-1);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.MemberWelcomeRequest();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$MemberWelcomeRequest".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * Populate and return the WelcomeMember message to respond to this
        * welcome request with.
         */
        protected com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcome populateWelcomeMessage()
            {
            // import Component.Util.ServiceConfig$Map as com.tangosol.coherence.component.util.ServiceConfig.Map;
            // import java.util.HashMap;
            
            PagedTopic.MemberWelcome msgWelcome = (PagedTopic.MemberWelcome) super.populateWelcomeMessage();
            if (!msgWelcome.isRejected())
                {
                PagedTopic   service        = (PagedTopic) getService();
                com.tangosol.coherence.component.util.ServiceConfig.Map mapTopicConfig = service.getTopicConfigMap();
            
                if (service.getThisMember() == mapTopicConfig.getConfigCoordinator())
                    {
                    // if we are the topic config coordinator, send the topic config as well
                    msgWelcome.setTopicConfigMap(mapTopicConfig);
                    }
                }
            
            return msgWelcome;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$SetChannelCountRequest
    
    /**
     * The Message contains all of the information necessary to describe a
     * message to send or to describe a received message.
     * <p>
     * With regards to the use of Message components within clustered Services,
     * Services are designed by dragging Message components into them as static
     * children. These Messages are the components that a Service can send to
     * other running instances of the same Service within a cluster. To send a
     * Message, a Service calls <code>instantiateMessage(String
     * sMsgName)</code> with the name of the child, then configures the Message
     * object and calls Service.send passing the Message. An incoming Message
     * is created by the Message Receiver by calling the
     * <code>Service.instantiateMessage(int nMsgType)</code> and the
     * configuring the Message using the Received data. The Message is then
     * queued in the Service's Queue. When the Service thread gets the Message
     * out of the Queue, it invokes onMessage passing the Message, and the
     * default implementation for onMessage in turn calls
     * <code>onReceived()</code> on the Message object.
     * <p>
     * A RequestMessage extends the generic Message and adds the capability to
     * poll one or more Members for responses. In the simplest case, the
     * RequestMessage with one destination Member implements the
     * request/response model.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class SetChannelCountRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property ChannelCount
         *
         * The channel count to set
         */
        private int __m_ChannelCount;
        
        /**
         * Property RequiredCount
         *
         */
        private int __m_RequiredCount;
        
        /**
         * Property TopicName
         *
         * The name of the topic to set the channel count for.
         */
        private String __m_TopicName;
        
        // Default constructor
        public SetChannelCountRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public SetChannelCountRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(1000);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.SetChannelCountRequest();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$SetChannelCountRequest".replace('/', '.'));
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
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Instantiate a copy of this message. This is quite different from the
        * standard "clone" since only the "transmittable" portion of the
        * message (and none of the internal) state should be cloned.
         */
        public com.tangosol.coherence.component.net.Message cloneMessage()
            {
            PagedTopic.SetChannelCountRequest msg = (PagedTopic.SetChannelCountRequest) super.cloneMessage();
            
            msg.setTopicName(getTopicName());
            msg.setChannelCount(getChannelCount());
            msg.setRequiredCount(getRequiredCount());
            
            return msg;
            }
        
        // Accessor for the property "ChannelCount"
        /**
         * Getter for property ChannelCount.<p>
        * The channel count to set
         */
        public int getChannelCount()
            {
            return __m_ChannelCount;
            }
        
        // Accessor for the property "RequiredCount"
        /**
         * Getter for property RequiredCount.<p>
         */
        public int getRequiredCount()
            {
            return __m_RequiredCount;
            }
        
        // Accessor for the property "TopicName"
        /**
         * Getter for property TopicName.<p>
        * The name of the topic to set the channel count for.
         */
        public String getTopicName()
            {
            return __m_TopicName;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            PagedTopic service = (PagedTopic) getService();
            service.onSetChannelCountRequest(this);
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            
            super.read(input);
            
            setTopicName(input.readUTF());
            setRequiredCount(com.tangosol.util.ExternalizableHelper.readInt(input));
            setChannelCount(com.tangosol.util.ExternalizableHelper.readInt(input));
            }
        
        // Accessor for the property "ChannelCount"
        /**
         * Setter for property ChannelCount.<p>
        * The channel count to set
         */
        public void setChannelCount(int nCount)
            {
            __m_ChannelCount = nCount;
            }
        
        // Declared at the super level
        /**
         * Setter for property RequestTimeout.<p>
        * Transient property optionally used on the client to indicate the
        * (safe local) time after which this logical request should be
        * considered timed out.
        * 
        * Note that a single logical request message may result in multiple
        * physical request messages being sent to mulitple members; this
        * RequestTimeout value will be cloned to all resulting RequestMessage
        * instances.
        * 
        * This value is lazily calculated by #getRequestTimeout or
        * #calculateTimeoutRemaining.
         */
        public void setRequestTimeout(long ldtTimeout)
            {
            super.setRequestTimeout(ldtTimeout);
            }
        
        // Accessor for the property "RequiredCount"
        /**
         * Setter for property RequiredCount.<p>
         */
        public void setRequiredCount(int nCount)
            {
            __m_RequiredCount = nCount;
            }
        
        // Accessor for the property "TopicName"
        /**
         * Setter for property TopicName.<p>
        * The name of the topic to set the channel count for.
         */
        public void setTopicName(String sName)
            {
            __m_TopicName = sName;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            
            super.write(output);
            
            output.writeUTF(getTopicName());
            com.tangosol.util.ExternalizableHelper.writeInt(output, getRequiredCount());
            com.tangosol.util.ExternalizableHelper.writeInt(output, getChannelCount());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$StorageIdRequest
    
    /**
     * StorageIdRequest is a poll (by a client) sent to the Senior member to
     * create or destroy a cache.  The action must be taken by the senior in
     * order to guarantee a unique storage id for a given cache name.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class StorageIdRequest
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.StorageIdRequest
        {
        // ---- Fields declarations ----
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
            __mapChildren.put("Poll", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.StorageIdRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public StorageIdRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public StorageIdRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(78);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.StorageIdRequest();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$StorageIdRequest".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches$Names as com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names;
            // import com.tangosol.internal.net.topic.impl.paged.PagedTopicConfigMap;
            // import Component.Net.Member as com.tangosol.coherence.component.net.Member;;
            // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
            // import com.tangosol.run.xml.SimpleElement;
            // import com.tangosol.run.xml.XmlElement;
            // import java.util.ArrayList;
            // import java.util.Arrays;
            // import java.util.Iterator;
            // import java.util.HashMap;
            // import java.util.List;
            // import java.util.Map;
            
            super.onReceived();
            
            PagedTopic   service      = (PagedTopic) getService();
            Map       mapConfig    = service.getTopicConfigMap();
            String[]  asCacheNames = getCacheNames();
            
            com.tangosol.coherence.component.net.Member memberThis        = service.getThisMember();
            com.tangosol.coherence.component.net.Member memberCoordinator = service.getServiceOldestMember();
            
            if (memberThis == memberCoordinator)
                {
                // must be coordinator to do a bulk-update of the service-config
                switch (getCacheAction())
                    {
                    case CACHE_CREATE:
                        {
                        Map mapXml = null;
            
                        for (int i = 0, cNames = asCacheNames.length; i < cNames; i++)
                            {
                            String sCacheName = asCacheNames[i];
                            if (sCacheName == null)
                                {
                                continue;
                                }
            
                            String     sTopicName   = com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names.getTopicName(sCacheName);
                            XmlElement xmlTopicInfo = (XmlElement) mapConfig.get(sTopicName);
                            if (xmlTopicInfo == null)
                                {
                                xmlTopicInfo = new SimpleElement("topic-info");
                                if (mapXml == null)
                                    {
                                    mapXml = new HashMap(cNames);
                                    }
                                mapXml.put(sTopicName, xmlTopicInfo);
                                }
                            }
            
                        if (mapXml != null)
                            {
                            mapConfig.putAll(mapXml);
                            }
            
                        break;
                        }
             
                    case CACHE_DESTROY:
                        // Destroy the topic.  Updating the service-config map will result in
                        // config updates sent to all service members
                        for (int i = 0, cNames = asCacheNames.length; i < cNames; i++)
                            {
                            String sCacheName = asCacheNames[i];
                            if (sCacheName == null)
                                {
                                continue;
                                }
                            
                            String sTopicName = com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names.getTopicName(sCacheName);
                            PagedTopicConfigMap.removeTopic(mapConfig, sTopicName);
                            }
                        break;
            
                    default:
                        throw new IllegalStateException();
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$SubscriberConfirmRequest
    
    /**
     * DistributeCacheRequest is a base component for RequestMessage(s) used by
     * the partitioned cache service that are key set or filter based. Quite
     * often a collection of similar requests are sent in parallel and a client
     * thread has to wait for all of them to return.
     * 
     * PartialRequest is a DistributeCacheRequest that is constrained by a
     * subset of partitions owned by a single storage-enabled Member.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class SubscriberConfirmRequest
            extends    com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property GroupId
         *
         */
        private com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId __m_GroupId;
        
        /**
         * Property SubscriberId
         *
         */
        private com.tangosol.internal.net.topic.impl.paged.model.SubscriberId __m_SubscriberId;
        
        /**
         * Property SubscriptionId
         *
         */
        private long __m_SubscriptionId;
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
            __mapChildren.put("Poll", PagedTopic.SubscriberConfirmRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public SubscriberConfirmRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public SubscriberConfirmRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(1002);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.SubscriberConfirmRequest();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$SubscriberConfirmRequest".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * Instantiate a copy of this message. This is quite different from the
        * standard "clone" since only the "transmittable" portion of the
        * message (and none of the internal) state should be cloned.
         */
        public com.tangosol.coherence.component.net.Message cloneMessage()
            {
            PagedTopic.SubscriberConfirmRequest msg = (PagedTopic.SubscriberConfirmRequest) super.cloneMessage();
            
            msg.setSubscriberId(getSubscriberId());
            msg.setSubscriptionId(getSubscriptionId());
            
            return msg;
            }
        
        // Accessor for the property "GroupId"
        /**
         * Getter for property GroupId.<p>
         */
        public com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId getGroupId()
            {
            return __m_GroupId;
            }
        
        // Accessor for the property "SubscriberId"
        /**
         * Getter for property SubscriberId.<p>
         */
        public com.tangosol.internal.net.topic.impl.paged.model.SubscriberId getSubscriberId()
            {
            return __m_SubscriberId;
            }
        
        // Accessor for the property "SubscriptionId"
        /**
         * Getter for property SubscriptionId.<p>
         */
        public long getSubscriptionId()
            {
            return __m_SubscriptionId;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            ((PagedTopic) getService()).onSubscriberConfirm(this);
            }
        
        // Declared at the super level
        /**
         * Preprocess this message.
        * 
        * @return true iff this message has been fully processed (onReceived
        * was called)
         */
        public boolean preprocess()
            {
            return false;
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
            // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            
            super.read(input);
            
            setSubscriptionId(input.readLong());
            setSubscriberId((SubscriberId) com.tangosol.util.ExternalizableHelper.readObject(input));
            }
        
        // Accessor for the property "GroupId"
        /**
         * Setter for property GroupId.<p>
         */
        public void setGroupId(com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId idGroup)
            {
            __m_GroupId = idGroup;
            }
        
        // Accessor for the property "SubscriberId"
        /**
         * Setter for property SubscriberId.<p>
         */
        public void setSubscriberId(com.tangosol.internal.net.topic.impl.paged.model.SubscriberId idSubscriber)
            {
            __m_SubscriberId = idSubscriber;
            }
        
        // Accessor for the property "SubscriptionId"
        /**
         * Setter for property SubscriptionId.<p>
         */
        public void setSubscriptionId(long lId)
            {
            __m_SubscriptionId = lId;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            
            super.write(output);
            
            output.writeLong(getSubscriptionId());
            com.tangosol.util.ExternalizableHelper.writeObject(output, getSubscriberId());
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$SubscriberConfirmRequest$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest.Poll
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.SubscriberConfirmRequest.Poll();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$SubscriberConfirmRequest$Poll".replace('/', '.'));
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
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$SubscriberIdRequest
    
    /**
     * SubscriberIdRequest is a poll (by a client) sent to the Senior member to
     * create or destroy a subsciption and optionally a subscriber.  The action
     * must be taken by the senior in order to guarantee a unique subscription
     * id for a given subscriber group.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class SubscriberIdRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property ChannelAllocations
         *
         */
        private long[] __m_ChannelAllocations;
        
        /**
         * Property Converter
         *
         */
        private com.tangosol.util.ValueExtractor __m_Converter;
        
        /**
         * Property Filter
         *
         */
        private com.tangosol.util.Filter __m_Filter;
        
        /**
         * Property GroupId
         *
         */
        private com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId __m_GroupId;
        
        /**
         * Property Response
         *
         */
        private com.tangosol.coherence.component.net.Message __m_Response;
        
        /**
         * Property SUBSCRIBER_CREATE
         *
         */
        public static final int SUBSCRIBER_CREATE = 1;
        
        /**
         * Property SUBSCRIBER_DESTROY
         *
         */
        public static final int SUBSCRIBER_DESTROY = 2;
        
        /**
         * Property SUBSCRIBER_UPDATE
         *
         */
        public static final int SUBSCRIBER_UPDATE = 3;
        
        /**
         * Property SubscriberAction
         *
         */
        private int __m_SubscriberAction;
        
        /**
         * Property SubscriberIds
         *
         */
        private com.tangosol.internal.net.topic.impl.paged.model.SubscriberId[] __m_SubscriberIds;
        
        /**
         * Property SubscriptionId
         *
         * The unique identifier of the subscriber group
         */
        private long __m_SubscriptionId;
        
        /**
         * Property TopicName
         *
         * The name of the topic to set the channel count for.
         */
        private String __m_TopicName;
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
            __mapChildren.put("Poll", PagedTopic.SubscriberIdRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public SubscriberIdRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public SubscriberIdRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(1001);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.SubscriberIdRequest();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$SubscriberIdRequest".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * Instantiate a copy of this message. This is quite different from the
        * standard "clone" since only the "transmittable" portion of the
        * message (and none of the internal) state should be cloned.
         */
        public com.tangosol.coherence.component.net.Message cloneMessage()
            {
            PagedTopic.SubscriberIdRequest msg = (PagedTopic.SubscriberIdRequest) super.cloneMessage();
            
            msg.setSubscriberAction(getSubscriberAction());
            msg.setTopicName(getTopicName());
            msg.setGroupId(getGroupId());
            msg.setSubscriberIds(getSubscriberIds());
            msg.setSubscriptionId(getSubscriptionId());
            msg.setFilter(getFilter());
            msg.setConverter(getConverter());
            msg.setChannelAllocations(getChannelAllocations());
            
            return msg;
            }
        
        // Accessor for the property "ChannelAllocations"
        /**
         * Getter for property ChannelAllocations.<p>
         */
        public long[] getChannelAllocations()
            {
            return __m_ChannelAllocations;
            }
        
        // Accessor for the property "ChannelAllocations"
        /**
         * Getter for property ChannelAllocations.<p>
         */
        public long getChannelAllocations(int i)
            {
            return getChannelAllocations()[i];
            }
        
        // Accessor for the property "Converter"
        /**
         * Getter for property Converter.<p>
         */
        public com.tangosol.util.ValueExtractor getConverter()
            {
            return __m_Converter;
            }
        
        // Accessor for the property "Filter"
        /**
         * Getter for property Filter.<p>
         */
        public com.tangosol.util.Filter getFilter()
            {
            return __m_Filter;
            }
        
        // Accessor for the property "GroupId"
        /**
         * Getter for property GroupId.<p>
         */
        public com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId getGroupId()
            {
            return __m_GroupId;
            }
        
        // Accessor for the property "Response"
        /**
         * Getter for property Response.<p>
         */
        public com.tangosol.coherence.component.net.Message getResponse()
            {
            return __m_Response;
            }
        
        // Accessor for the property "SubscriberAction"
        /**
         * Getter for property SubscriberAction.<p>
         */
        public int getSubscriberAction()
            {
            return __m_SubscriberAction;
            }
        
        // Accessor for the property "SubscriberIds"
        /**
         * Getter for property SubscriberIds.<p>
         */
        public com.tangosol.internal.net.topic.impl.paged.model.SubscriberId[] getSubscriberIds()
            {
            return __m_SubscriberIds;
            }
        
        // Accessor for the property "SubscriberIds"
        /**
         * Getter for property SubscriberIds.<p>
         */
        public com.tangosol.internal.net.topic.impl.paged.model.SubscriberId getSubscriberIds(int i)
            {
            return getSubscriberIds()[i];
            }
        
        // Accessor for the property "SubscriptionId"
        /**
         * Getter for property SubscriptionId.<p>
        * The unique identifier of the subscriber group
         */
        public long getSubscriptionId()
            {
            return __m_SubscriptionId;
            }
        
        // Accessor for the property "TopicName"
        /**
         * Getter for property TopicName.<p>
        * The name of the topic to set the channel count for.
         */
        public String getTopicName()
            {
            return __m_TopicName;
            }
        
        // Declared at the super level
        protected com.tangosol.coherence.component.net.Poll instantiatePoll()
            {
            // import Component.Net.Poll;
            
            return (Poll) _newChild("Poll");
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            super.onReceived();
            
            PagedTopic service = (PagedTopic) getService();
            service.onSubscriberId(this);
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.util.Filter;
            // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
            // import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            // import com.tangosol.util.ValueExtractor;
            
            super.read(input);
            
            setTopicName(com.tangosol.util.ExternalizableHelper.readSafeUTF(input));
            setGroupId((SubscriberGroupId) com.tangosol.util.ExternalizableHelper.readObject(input));
            setSubscriptionId(com.tangosol.util.ExternalizableHelper.readLong(input));
            setSubscriberIds((SubscriberId[]) com.tangosol.util.ExternalizableHelper.readObject(input));
            setSubscriberAction(com.tangosol.util.ExternalizableHelper.readInt(input));
            setChannelAllocations((long[]) com.tangosol.util.ExternalizableHelper.readObject(input));
            setFilter((Filter) com.tangosol.util.ExternalizableHelper.readObject(input));
            setConverter((ValueExtractor) com.tangosol.util.ExternalizableHelper.readObject(input));
            
            readTracing(input);
            }
        
        // Accessor for the property "ChannelAllocations"
        /**
         * Setter for property ChannelAllocations.<p>
         */
        public void setChannelAllocations(long[] alAllocations)
            {
            __m_ChannelAllocations = alAllocations;
            }
        
        // Accessor for the property "ChannelAllocations"
        /**
         * Setter for property ChannelAllocations.<p>
         */
        public void setChannelAllocations(int i, long lAllocations)
            {
            getChannelAllocations()[i] = lAllocations;
            }
        
        // Accessor for the property "Converter"
        /**
         * Setter for property Converter.<p>
         */
        public void setConverter(com.tangosol.util.ValueExtractor extractorConverter)
            {
            __m_Converter = extractorConverter;
            }
        
        // Accessor for the property "Filter"
        /**
         * Setter for property Filter.<p>
         */
        public void setFilter(com.tangosol.util.Filter filter)
            {
            __m_Filter = filter;
            }
        
        // Accessor for the property "GroupId"
        /**
         * Setter for property GroupId.<p>
         */
        public void setGroupId(com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId idGroup)
            {
            __m_GroupId = idGroup;
            }
        
        // Declared at the super level
        /**
         * Setter for property RequestTimeout.<p>
        * Transient property optionally used on the client to indicate the
        * (safe local) time after which this logical request should be
        * considered timed out.
        * 
        * Note that a single logical request message may result in multiple
        * physical request messages being sent to mulitple members; this
        * RequestTimeout value will be cloned to all resulting RequestMessage
        * instances.
        * 
        * This value is lazily calculated by #getRequestTimeout or
        * #calculateTimeoutRemaining.
         */
        public void setRequestTimeout(long ldtTimeout)
            {
            super.setRequestTimeout(ldtTimeout);
            }
        
        // Accessor for the property "Response"
        /**
         * Setter for property Response.<p>
         */
        public void setResponse(com.tangosol.coherence.component.net.Message msgResponse)
            {
            __m_Response = msgResponse;
            }
        
        // Accessor for the property "SubscriberAction"
        /**
         * Setter for property SubscriberAction.<p>
         */
        public void setSubscriberAction(int nAction)
            {
            __m_SubscriberAction = nAction;
            }
        
        // Accessor for the property "SubscriberIds"
        /**
         * Setter for property SubscriberIds.<p>
         */
        public void setSubscriberIds(com.tangosol.internal.net.topic.impl.paged.model.SubscriberId[] aidIds)
            {
            __m_SubscriberIds = aidIds;
            }
        
        // Accessor for the property "SubscriberIds"
        /**
         * Setter for property SubscriberIds.<p>
         */
        public void setSubscriberIds(int i, com.tangosol.internal.net.topic.impl.paged.model.SubscriberId idIds)
            {
            getSubscriberIds()[i] = idIds;
            }
        
        // Accessor for the property "SubscriptionId"
        /**
         * Setter for property SubscriptionId.<p>
        * The unique identifier of the subscriber group
         */
        public void setSubscriptionId(long lId)
            {
            __m_SubscriptionId = lId;
            }
        
        // Accessor for the property "TopicName"
        /**
         * Setter for property TopicName.<p>
        * The name of the topic to set the channel count for.
         */
        public void setTopicName(String sName)
            {
            __m_TopicName = sName;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            
            super.write(output);
            
            com.tangosol.util.ExternalizableHelper.writeSafeUTF(output, getTopicName());
            com.tangosol.util.ExternalizableHelper.writeObject(output, getGroupId());
            com.tangosol.util.ExternalizableHelper.writeLong(output, getSubscriptionId());
            com.tangosol.util.ExternalizableHelper.writeObject(output, getSubscriberIds());
            com.tangosol.util.ExternalizableHelper.writeInt(output, getSubscriberAction());
            com.tangosol.util.ExternalizableHelper.writeObject(output, getChannelAllocations());
            com.tangosol.util.ExternalizableHelper.writeObject(output, getFilter());
            com.tangosol.util.ExternalizableHelper.writeObject(output, getConverter());
            
            writeTracing(output);
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$SubscriberIdRequest$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.SubscriberIdRequest.Poll();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$SubscriberIdRequest$Poll".replace('/', '.'));
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
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            protected void onCompletion()
                {
                // import Component.Net.Member;
                // import Component.Net.Message;
                // import java.util.Map;
                
                super.onCompletion();
                
                PagedTopic.SubscriberIdRequest msgRequest  = (PagedTopic.SubscriberIdRequest) get_Parent();
                PagedTopic.Response            msgResponse = (PagedTopic.Response) msgRequest.getResponse();
                
                if (msgResponse != null)
                    {
                    PagedTopic service       = (PagedTopic) getService();
                    Long    lSubscriberId = (Long) getResult();
                
                    if (lSubscriberId == null)
                        {
                        // the senior died; repeat the request
                
                        if (!service.isExiting() && service.getServiceState() < PagedTopic.SERVICE_STOPPING)
                            {
                            PagedTopic.SubscriberIdRequest msg = (PagedTopic.SubscriberIdRequest) msgRequest.cloneMessage();
                            msg.addToMember(service.getServiceOldestMember());
                
                            service.post(msg);
                            }
                        }
                    else
                        {
                        msgResponse.setValue(lSubscriberId);
                        service.post(msgResponse);
                        }
                    }
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                PagedTopic.Response response = (PagedTopic.Response) msg;
                
                setResult(response.getValue());
                
                super.onResponse(msg);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$TopicConfig
    
    /**
     * ServiceConfig provides a service-wide configuration map.  All updates to
     * a service config are published service-wide by the configuration
     * coordinator.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class TopicConfig
            extends    com.tangosol.coherence.component.util.ServiceConfig
        {
        // ---- Fields declarations ----
        
        /**
         * Property TYPE_ANY
         *
         */
        public static final int TYPE_ANY = 0;
        
        /**
         * Property TYPE_LONG
         *
         */
        public static final int TYPE_LONG = 1;
        
        /**
         * Property TYPE_SUBSCRIPTION
         *
         */
        public static final int TYPE_SUBSCRIPTION = 2;
        
        /**
         * Property TYPE_SUBSCRIPTION_KEY
         *
         */
        public static final int TYPE_SUBSCRIPTION_KEY = 3;
        
        // Default constructor
        public TopicConfig()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public TopicConfig(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setPendingConfigUpdates(new java.util.LinkedList());
                setPendingPolls(new com.tangosol.util.LiteMap());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new PagedTopic.TopicConfig.ConfigListener("ConfigListener", this, true), "ConfigListener");
            _addChild(new PagedTopic.TopicConfig.Map("Map", this, true), "Map");
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.TopicConfig();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$TopicConfig".replace('/', '.'));
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
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Deserialize a ConfigMap related object from the specified DataInput.
        * 
        * @param in  the DataInput containing a serialized object
        * 
        * @return the deserialized object
         */
        public Object readObject(java.io.DataInput in)
                throws java.io.IOException
            {
            // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
            // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription$Key as com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key;
            // import java.io.IOException;
            
            // we only store certain object types in the TopicConfig
            int nType;
            switch (nType = in.readByte())
                {
                case TYPE_SUBSCRIPTION:
                    {
                    PagedTopicSubscription subscription = new PagedTopicSubscription();
                    subscription.readExternal(in);
                    return subscription;
                    }
            
                case TYPE_SUBSCRIPTION_KEY:
                    {
                    com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key key = new com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key();
                    key.readExternal(in);
                    return key;
                    }
            
                case TYPE_LONG:
                    {
                    return Long.valueOf(in.readLong());
                    }
            
                case TYPE_ANY:
                    return super.readObject(in);
            
                default:
                    throw new IOException("invalid type: " + nType);
                }
            }
        
        // Declared at the super level
        /**
         * Serialize a ConfigMap related object to the specified DataOutput.
        * 
        * @param out  the DataOutput
        * @param o  the object to serialize
         */
        public void writeObject(java.io.DataOutput out, Object o)
                throws java.io.IOException
            {
            // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
            // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription$Key as com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key;
            
            // we only store certain object types in the PartitionConfig
            if (o instanceof PagedTopicSubscription)
                {
                out.write(TYPE_SUBSCRIPTION);
                ((PagedTopicSubscription) o).writeExternal(out);
                }
            else if (o instanceof com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key)
                {
                out.write(TYPE_SUBSCRIPTION_KEY);
                ((com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key) o).writeExternal(out);
                }
            else if (o instanceof Long)
                {
                out.write(TYPE_LONG);
                out.writeLong(((Long) o).longValue());
                }
            else
                {
                out.write(TYPE_ANY);
                super.writeObject(out, o);
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$TopicConfig$ConfigListener
        
        /**
         * ConfigListener is used to receive config map updates for this
         * ServiceConfig.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class ConfigListener
                extends    com.tangosol.coherence.component.util.ServiceConfig.ConfigListener
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public ConfigListener()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public ConfigListener(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.TopicConfig.ConfigListener();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$TopicConfig$ConfigListener".replace('/', '.'));
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
            public void entryDeleted(com.tangosol.util.MapEvent evt)
                {
                // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
                // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription$Key as com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key;
                // import com.tangosol.run.xml.XmlElement;
                
                super.entryDeleted(evt);
                
                PagedTopic service = (PagedTopic) get_Module();
                Object  oKey    = evt.getKey();
                
                if (oKey instanceof com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key)
                    {
                    PagedTopicSubscription subscription = (PagedTopicSubscription) evt.getOldValue();
                    service.destroySubscriptionInternal(subscription.getSubscriptionId());
                    }
                }
            
            // Declared at the super level
            public void entryInserted(com.tangosol.util.MapEvent evt)
                {
                // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
                // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription$Key as com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key;
                
                super.entryInserted(evt);
                
                PagedTopic service = (PagedTopic) get_Module();
                Object  oKey    = evt.getKey();
                
                if (oKey instanceof com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key)
                    {
                    com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key key = (com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key) oKey;
                    service.ensureSubscription((PagedTopicSubscription) evt.getNewValue());
                    }
                }
            
            // Declared at the super level
            public void entryUpdated(com.tangosol.util.MapEvent evt)
                {
                // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
                // import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription$Key as com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key;
                // import com.tangosol.run.xml.XmlElement;
                
                super.entryUpdated(evt);
                
                PagedTopic service = (PagedTopic) get_Module();
                Object  oKey    = evt.getKey();
                
                if (oKey instanceof com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key)
                    {
                    com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key key = (com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription.Key) oKey;
                    service.ensureSubscription((PagedTopicSubscription) evt.getNewValue());
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$TopicConfig$Map
        
        /**
         * The "live" configuration map.  Mutations on this Map through the
         * java.util.Map interface will be published to all members sharing the
         * ServiceConfig.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Map
                extends    com.tangosol.coherence.component.util.ServiceConfig.Map
            {
            // ---- Fields declarations ----
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
                __mapChildren.put("EntrySet", PagedTopic.TopicConfig.Map.EntrySet.get_CLASS());
                __mapChildren.put("KeySet", PagedTopic.TopicConfig.Map.KeySet.get_CLASS());
                __mapChildren.put("Values", PagedTopic.TopicConfig.Map.Values.get_CLASS());
                }
            
            // Default constructor
            public Map()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Map(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.TopicConfig.Map();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$TopicConfig$Map".replace('/', '.'));
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
            
            // Declared at the super level
            /**
             * Getter for property MapType.<p>
            * An integer value, unique to the Service using this map, that
            * defines the type of this config map.
            * 
            * @see Grid#getConfigMap(int)
             */
            public int getMapType()
                {
                return PagedTopic.CONFIG_MAP_TOPIC;
                }
            
            // Declared at the super level
            /**
             * Update the local contents of the config map, returning the
            * previously associated value.  Called only on the service thread.
            * 
            * @param oKey         the key to update
            * @param oValue      the associated value (or null if fRemove)
            * @param fRemove   true iff the specified key should be removed
             */
            public Object updateInternal(Object oKey, Object oValue, boolean fRemove)
                {
                return super.updateInternal(oKey, oValue, fRemove);
                }
            
            // Declared at the super level
            /**
             * Update the local contents of the config map.  Called only on the
            * service thread.
            * 
            * @param mapUpdate  the mappings to update the config map with
            * @param fRemove      true iff the specified keys should be removed
             */
            public void updateInternal(java.util.Map mapUpdate, boolean fRemove)
                {
                super.updateInternal(mapUpdate, fRemove);
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$TopicConfig$Map$EntrySet
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class EntrySet
                    extends    com.tangosol.coherence.component.util.ServiceConfig.Map.EntrySet
                {
                // ---- Fields declarations ----
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
                    __mapChildren.put("Entry", PagedTopic.TopicConfig.Map.EntrySet.Entry.get_CLASS());
                    __mapChildren.put("Iterator", PagedTopic.TopicConfig.Map.EntrySet.Iterator.get_CLASS());
                    }
                
                // Default constructor
                public EntrySet()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public EntrySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.TopicConfig.Map.EntrySet();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$TopicConfig$Map$EntrySet".replace('/', '.'));
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
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$TopicConfig$Map$EntrySet$Entry
                
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Entry
                        extends    com.tangosol.coherence.component.util.ServiceConfig.Map.EntrySet.Entry
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Entry()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Entry(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.TopicConfig.Map.EntrySet.Entry();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$TopicConfig$Map$EntrySet$Entry".replace('/', '.'));
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
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }

                // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$TopicConfig$Map$EntrySet$Iterator
                
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Iterator
                        extends    com.tangosol.coherence.component.util.ServiceConfig.Map.EntrySet.Iterator
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
                        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.TopicConfig.Map.EntrySet.Iterator();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$TopicConfig$Map$EntrySet$Iterator".replace('/', '.'));
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
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$TopicConfig$Map$KeySet
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class KeySet
                    extends    com.tangosol.coherence.component.util.ServiceConfig.Map.KeySet
                {
                // ---- Fields declarations ----
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
                    __mapChildren.put("Iterator", PagedTopic.TopicConfig.Map.KeySet.Iterator.get_CLASS());
                    }
                
                // Default constructor
                public KeySet()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public KeySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.TopicConfig.Map.KeySet();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$TopicConfig$Map$KeySet".replace('/', '.'));
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
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$TopicConfig$Map$KeySet$Iterator
                
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Iterator
                        extends    com.tangosol.coherence.component.util.ServiceConfig.Map.KeySet.Iterator
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
                        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.TopicConfig.Map.KeySet.Iterator();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$TopicConfig$Map$KeySet$Iterator".replace('/', '.'));
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
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$TopicConfig$Map$Values
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Values
                    extends    com.tangosol.coherence.component.util.ServiceConfig.Map.Values
                {
                // ---- Fields declarations ----
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
                    __mapChildren.put("Iterator", PagedTopic.TopicConfig.Map.Values.Iterator.get_CLASS());
                    }
                
                // Default constructor
                public Values()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Values(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.TopicConfig.Map.Values();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$TopicConfig$Map$Values".replace('/', '.'));
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
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic$TopicConfig$Map$Values$Iterator
                
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Iterator
                        extends    com.tangosol.coherence.component.util.ServiceConfig.Map.Values.Iterator
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
                        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.TopicConfig.Map.Values.Iterator();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/partitionedService/partitionedCache/PagedTopic$TopicConfig$Map$Values$Iterator".replace('/', '.'));
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
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }
            }
        }
    }
