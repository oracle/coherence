
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.ServiceMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

/**
 * ServiceMBean represents a clustered Service.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ServiceMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
    {
    // ---- Fields declarations ----
    
    /**
     * Property RequestTimeoutMillis
     *
     * The default timeout value in milliseconds for requests that can be
     * timed-out (e.g. implement the com.tangosol.net.PriorityTask interface),
     * but do not explicitly specify the request timeout value.
     * 
     * @descriptor rest.collector=set
     */
    private transient long __m_RequestTimeoutMillis;
    
    /**
     * Property TaskHungThresholdMillis
     *
     * The amount of time in milliseconds that a task can execute before it is
     * considered hung. Note that a posted task that has not yet started is
     * never considered as hung.
     * 
     * @descriptor rest.collector=set
     */
    private transient long __m_TaskHungThresholdMillis;
    
    /**
     * Property TaskTimeoutMillis
     *
     * The default timeout value in milliseconds for tasks that can be
     * timed-out (e.g. implement the com.tangosol.net.PriorityTask interface),
     * but do not explicitly specify the task execution timeout value.
     * 
     * @descriptor rest.collector=set
     */
    private transient long __m_TaskTimeoutMillis;
    
    /**
     * Property ThreadCount
     *
     * The number of threads in the service thread pool. For services that
     * support dynamic thread pool sizing, this is the current thread pool
     * size.
     * 
     * @descriptor rest.collector=set,metrics.value=_default
     */
    private transient int __m_ThreadCount;
    
    /**
     * Property ThreadCountMax
     *
     * The maximum thread count allowed for this service when dynamic thread
     * pool sizing is enabled.
     * 
     * @descriptor rest.collector=set
     */
    private transient int __m_ThreadCountMax;
    
    /**
     * Property ThreadCountMin
     *
     * The minimum thread count for this service when dynamic thread pool
     * sizing is enabled.
     * 
     * @descriptor rest.collector=set
     */
    private transient int __m_ThreadCountMin;
    
    // Default constructor
    public ServiceMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ServiceMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.ServiceMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/ServiceMBean".replace('/', '.'));
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
            "ServiceMBean represents a clustered Service.",
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
        
        // property BackupCount
            {
            mapInfo.put("BackupCount", new Object[]
                {
                "The number of backups for every cache storage.",
                "getBackupCount",
                null,
                "I",
                "rest.collector=set",
                });
            }
        
        // property BackupCountAfterWritebehind
            {
            mapInfo.put("BackupCountAfterWritebehind", new Object[]
                {
                "The number of members of the partitioned (distributed) cache service that will retain backup data that does _not_ require write-behind, i.e. data that is not vulnerable to being lost even if the entire cluster were shut down.",
                "getBackupCountAfterWritebehind",
                null,
                "I",
                "rest.collector=set",
                });
            }
        
        // property EventBacklog
            {
            mapInfo.put("EventBacklog", new Object[]
                {
                "The size of the backlog queue that holds events scheduled to be processed by the EventDispatcher thread.",
                "getEventBacklog",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property EventCount
            {
            mapInfo.put("EventCount", new Object[]
                {
                "The total number of processed events since the last time the statistics were reset.",
                "getEventCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
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
        
        // property IndexingTotalMillis
            {
            mapInfo.put("IndexingTotalMillis", new Object[]
                {
                "The total amount of time taken to build indices for all storage instances and partitions owned by this member. Building of indices may be performed in parallel thus this value may be less than suming StorageManagerMBean.IndexingTotalMillis.",
                "getIndexingTotalMillis",
                null,
                "J",
                "rest.collector=max,metrics.value=_default",
                });
            }
        
        // property JoinTime
            {
            mapInfo.put("JoinTime", new Object[]
                {
                "The date/time value (in cluster time) that this Member joined the service.",
                "getJoinTime",
                null,
                "Ljava/util/Date;",
                null,
                });
            }
        
        // property MemberCount
            {
            mapInfo.put("MemberCount", new Object[]
                {
                "Specifies the total number of cluster nodes running this service.",
                "getMemberCount",
                null,
                "I",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property MessagesLocal
            {
            mapInfo.put("MessagesLocal", new Object[]
                {
                "The total number of messages which were self-addressed messages since the last time the statistics were reset.  Such messages are used for servicing process-local requests and do not have an associated network cost.",
                "getMessagesLocal",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property MessagesReceived
            {
            mapInfo.put("MessagesReceived", new Object[]
                {
                "The total number of messages received by this service since the last time the statistics were reset. This value accounts for messages received by any (local, dedicated or shared) transport.",
                "getMessagesReceived",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property MessagesSent
            {
            mapInfo.put("MessagesSent", new Object[]
                {
                "The number of messages sent by this service since the last time the statistics were reset. This value accounts for messages sent by any (local, dedicated or shared) transport.",
                "getMessagesSent",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property OutgoingTransferCount
            {
            mapInfo.put("OutgoingTransferCount", new Object[]
                {
                "The number of partitions that are currently being transferred by this service member to other members.",
                "getOutgoingTransferCount",
                null,
                "I",
                "rest.collector=sum",
                });
            }
        
        // property OwnedPartitionsBackup
            {
            mapInfo.put("OwnedPartitionsBackup", new Object[]
                {
                "The number of partitions that this Member backs up (responsible for the backup storage).",
                "getOwnedPartitionsBackup",
                null,
                "I",
                "rest.collector=list,metrics.value=_default",
                });
            }
        
        // property OwnedPartitionsPrimary
            {
            mapInfo.put("OwnedPartitionsPrimary", new Object[]
                {
                "The number of partitions that this Member owns (responsible for the primary storage).",
                "getOwnedPartitionsPrimary",
                null,
                "I",
                "rest.collector=list,metrics.value=_default",
                });
            }
        
        // property PartitionsAll
            {
            mapInfo.put("PartitionsAll", new Object[]
                {
                "The total number of partitions that every cache storage will be divided into.",
                "getPartitionsAll",
                null,
                "I",
                "rest.collector=set",
                });
            }
        
        // property PartitionsEndangered
            {
            mapInfo.put("PartitionsEndangered", new Object[]
                {
                "The total number of partitions that are not currently backed up.",
                "getPartitionsEndangered",
                null,
                "I",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property PartitionsUnbalanced
            {
            mapInfo.put("PartitionsUnbalanced", new Object[]
                {
                "The total number of primary and backup partitions which remain to be transferred until the partition distribution across the storage enabled service members is fully balanced.",
                "getPartitionsUnbalanced",
                null,
                "I",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property PartitionsVulnerable
            {
            mapInfo.put("PartitionsVulnerable", new Object[]
                {
                "The total number of partitions that are backed up on the same machine where the primary partition owner resides.",
                "getPartitionsVulnerable",
                null,
                "I",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property PersistenceActiveSpaceAvailable
            {
            mapInfo.put("PersistenceActiveSpaceAvailable", new Object[]
                {
                "The total remaining free space (in bytes) of the file system used by the persistence layer to persist active cache data.",
                "getPersistenceActiveSpaceAvailable",
                null,
                "J",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property PersistenceActiveSpaceTotal
            {
            mapInfo.put("PersistenceActiveSpaceTotal", new Object[]
                {
                "The total size (in bytes) of the file system used by the persistence layer to persist active cache data.",
                "getPersistenceActiveSpaceTotal",
                null,
                "J",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property PersistenceActiveSpaceUsed
            {
            mapInfo.put("PersistenceActiveSpaceUsed", new Object[]
                {
                "The total size (in bytes) used by the persistence layer to persist active cache data.",
                "getPersistenceActiveSpaceUsed",
                null,
                "J",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property PersistenceBackupSpaceAvailable
            {
            mapInfo.put("PersistenceBackupSpaceAvailable", new Object[]
                {
                "The total remaining free space (in bytes) of the file system used by the persistence layer to persist backup cache data.",
                "getPersistenceBackupSpaceAvailable",
                null,
                "J",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property PersistenceBackupSpaceTotal
            {
            mapInfo.put("PersistenceBackupSpaceTotal", new Object[]
                {
                "The total size (in bytes) of the file system used by the persistence layer to persist backup cache data.",
                "getPersistenceBackupSpaceTotal",
                null,
                "J",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property PersistenceBackupSpaceUsed
            {
            mapInfo.put("PersistenceBackupSpaceUsed", new Object[]
                {
                "The total size (in bytes) used by the persistence layer to persist backup cache data.",
                "getPersistenceBackupSpaceUsed",
                null,
                "J",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property PersistenceEnvironment
            {
            mapInfo.put("PersistenceEnvironment", new Object[]
                {
                "A description of the configured persistence environment or 'n/a' if one has not been configured.",
                "getPersistenceEnvironment",
                null,
                "Ljava/lang/String;",
                "rest.collector=set",
                });
            }
        
        // property PersistenceLatencyAverage
            {
            mapInfo.put("PersistenceLatencyAverage", new Object[]
                {
                "The average latency (in millis) added to a mutating cache operation by active persistence operations.",
                "getPersistenceLatencyAverage",
                null,
                "F",
                "metrics.value=_default",
                });
            }
        
        // property PersistenceLatencyMax
            {
            mapInfo.put("PersistenceLatencyMax", new Object[]
                {
                "The maximum latency (in millis) added to a mutating cache operation by an active persistence operation.",
                "getPersistenceLatencyMax",
                null,
                "J",
                "rest.collector=max,metrics.value=_default",
                });
            }
        
        // property PersistenceMode
            {
            mapInfo.put("PersistenceMode", new Object[]
                {
                "The current persistence mode for this service.  A value of 'active' indicates that all mutating cache operations (i.e. writes) will be persisted via the configured persistence-environment. A value of 'live' indicates that all reads and writes will be performed against the configured persistence-environment.  A value of 'on-demand' indicates that a persistence-environment has been configured and is available but is not being actively used. In all modes a persistent snapshot can be taken of all caches managed by this service using the configured persistence-environment. A value of 'n/a' indicates that persistence is not configured for this service.",
                "getPersistenceMode",
                null,
                "Ljava/lang/String;",
                "rest.collector=set",
                });
            }
        
        // property PersistenceSnapshotArchiver
            {
            mapInfo.put("PersistenceSnapshotArchiver", new Object[]
                {
                "A description of the configured snapshot archiver or 'n/a' if one has not been configured.",
                "getPersistenceSnapshotArchiver",
                null,
                "Ljava/lang/String;",
                "rest.collector=set",
                });
            }
        
        // property PersistenceSnapshotSpaceAvailable
            {
            mapInfo.put("PersistenceSnapshotSpaceAvailable", new Object[]
                {
                "The total remaining free space (in bytes) of the file system used by the persistence layer to store snapshots.",
                "getPersistenceSnapshotSpaceAvailable",
                null,
                "J",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property PersistenceSnapshotSpaceTotal
            {
            mapInfo.put("PersistenceSnapshotSpaceTotal", new Object[]
                {
                "The total size (in bytes) of the file system used by the persistence layer to store snapshots.",
                "getPersistenceSnapshotSpaceTotal",
                null,
                "J",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property QuorumStatus
            {
            mapInfo.put("QuorumStatus", new Object[]
                {
                "The current state of the service quorum.",
                "getQuorumStatus",
                null,
                "Ljava/lang/String;",
                "rest.collector=set",
                });
            }
        
        // property RequestAverageDuration
            {
            mapInfo.put("RequestAverageDuration", new Object[]
                {
                "The average duration (in milliseconds) of an individual request issued by the service since the last time the statistics were reset.",
                "getRequestAverageDuration",
                null,
                "F",
                "metrics.value=_default",
                });
            }
        
        // property RequestMaxDuration
            {
            mapInfo.put("RequestMaxDuration", new Object[]
                {
                "The maximum duration (in milliseconds) of a request issued by the service since the last time the statistics were reset.",
                "getRequestMaxDuration",
                null,
                "J",
                "rest.collector=max,metrics.value=_default",
                });
            }
        
        // property RequestPendingCount
            {
            mapInfo.put("RequestPendingCount", new Object[]
                {
                "The number of pending requests issued by the service.",
                "getRequestPendingCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property RequestPendingDuration
            {
            mapInfo.put("RequestPendingDuration", new Object[]
                {
                "The duration (in milliseconds) of the oldest pending request issued by the service.",
                "getRequestPendingDuration",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property RequestTimeoutCount
            {
            mapInfo.put("RequestTimeoutCount", new Object[]
                {
                "The total number of timed-out requests since the last time the statistics were reset.",
                "getRequestTimeoutCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property RequestTimeoutMillis
            {
            mapInfo.put("RequestTimeoutMillis", new Object[]
                {
                "The default timeout value in milliseconds for requests that can be timed-out (e.g. implement the com.tangosol.net.PriorityTask interface), but do not explicitly specify the request timeout value.",
                "getRequestTimeoutMillis",
                "setRequestTimeoutMillis",
                "J",
                "rest.collector=set",
                });
            }
        
        // property RequestTotalCount
            {
            mapInfo.put("RequestTotalCount", new Object[]
                {
                "The total number of synchronous requests issued by the service since the last time the statistics were reset.",
                "getRequestTotalCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property Running
            {
            mapInfo.put("Running", new Object[]
                {
                "Specifies whether or not the service is running.",
                "isRunning",
                null,
                "Z",
                "rest.collector=set",
                });
            }
        
        // property SeniorMemberId
            {
            mapInfo.put("SeniorMemberId", new Object[]
                {
                "The service senior member id; -1 if the service is not running.",
                "getSeniorMemberId",
                null,
                "I",
                "rest.collector=set",
                });
            }
        
        // property Statistics
            {
            mapInfo.put("Statistics", new Object[]
                {
                "Statistics for this service in human readable format.",
                "getStatistics",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property StatusHA
            {
            mapInfo.put("StatusHA", new Object[]
                {
                "The High Availability status for this service. The value of MACHINE-SAFE means that all the cluster nodes running on any given machine could be stopped at once without data loss. The value of NODE-SAFE means that any cluster node could be stopped without data loss.  The value of ENDANGERED indicates that abnormal termination of any cluster node that runs this service may cause data loss. If either or both site and rack groupings are configured, the values SITE-SAFE and RACK-SAFE mean that all the cluster nodes running on any given site or rack, respectively,  could be stopped at once without data loss.",
                "getStatusHA",
                null,
                "Ljava/lang/String;",
                "rest.collector=set",
                });
            }
        
        // property StatusHACode
            {
            mapInfo.put("StatusHACode", new Object[]
                {
                "The High Availability status for this service. The value of 3 (MACHINE-SAFE) means that all the cluster nodes running on any given machine could be stoppped at once without data loss. The value of 2 (NODE-SAFE) means that any cluster node could be stoppped without data loss.  The value of 1 (ENDANGERED) indicates that abnormal termination of any cluster node that runs this service may cause data loss. If the rack and site names are configured, the values 5 (SITE-SAFE) and 4 (RACK-SAFE) mean that all the cluster nodes running on any given site or rack, respectively, could be stoppped without data loss. If StatusHA is not applicable for the service, -1 is returned.",
                "getStatusHACode",
                null,
                "I",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property StorageEnabled
            {
            mapInfo.put("StorageEnabled", new Object[]
                {
                "Specifies whether or not the local storage is enabled for this cluster Member.",
                "isStorageEnabled",
                null,
                "Z",
                "rest.collector=set",
                });
            }
        
        // property StorageEnabledCount
            {
            mapInfo.put("StorageEnabledCount", new Object[]
                {
                "Specifies the total number of cluster nodes running this Service for which local storage is enabled.",
                "getStorageEnabledCount",
                null,
                "I",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property TaskAverageDuration
            {
            mapInfo.put("TaskAverageDuration", new Object[]
                {
                "The average duration (in milliseconds) of an individual task execution.",
                "getTaskAverageDuration",
                null,
                "F",
                "metrics.value=_default",
                });
            }
        
        // property TaskBacklog
            {
            mapInfo.put("TaskBacklog", new Object[]
                {
                "The size of the backlog queue that holds tasks scheduled to be executed by one of the service threads.",
                "getTaskBacklog",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TaskCount
            {
            mapInfo.put("TaskCount", new Object[]
                {
                "The total number of executed tasks since the last time the statistics were reset.",
                "getTaskCount",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TaskHungCount
            {
            mapInfo.put("TaskHungCount", new Object[]
                {
                "The total number of currently executing hung tasks.",
                "getTaskHungCount",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TaskHungDuration
            {
            mapInfo.put("TaskHungDuration", new Object[]
                {
                "The longest currently executing hung task duration in milliseconds.",
                "getTaskHungDuration",
                null,
                "J",
                "rest.collector=max,metrics.value=_default",
                });
            }
        
        // property TaskHungTaskId
            {
            mapInfo.put("TaskHungTaskId", new Object[]
                {
                "The id of the of the longest currently executing hung task.",
                "getTaskHungTaskId",
                null,
                "Ljava/lang/String;",
                "rest.collector=set",
                });
            }
        
        // property TaskHungThresholdMillis
            {
            mapInfo.put("TaskHungThresholdMillis", new Object[]
                {
                "The amount of time in milliseconds that a task can execute before it is considered hung. Note that a posted task that has not yet started is never considered as hung.",
                "getTaskHungThresholdMillis",
                "setTaskHungThresholdMillis",
                "J",
                "rest.collector=set",
                });
            }
        
        // property TaskMaxBacklog
            {
            mapInfo.put("TaskMaxBacklog", new Object[]
                {
                "The maximum size of the backlog queue since the last time the statistics were reset.",
                "getTaskMaxBacklog",
                null,
                "I",
                "rest.collector=max,metrics.value=_default",
                });
            }
        
        // property TaskTimeoutCount
            {
            mapInfo.put("TaskTimeoutCount", new Object[]
                {
                "The total number of timed-out tasks since the last time the statistics were reset.",
                "getTaskTimeoutCount",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TaskTimeoutMillis
            {
            mapInfo.put("TaskTimeoutMillis", new Object[]
                {
                "The default timeout value in milliseconds for tasks that can be timed-out (e.g. implement the com.tangosol.net.PriorityTask interface), but do not explicitly specify the task execution timeout value.",
                "getTaskTimeoutMillis",
                "setTaskTimeoutMillis",
                "J",
                "rest.collector=set",
                });
            }
        
        // property ThreadAbandonedCount
            {
            mapInfo.put("ThreadAbandonedCount", new Object[]
                {
                "The number of abandoned threads from the service thread pool. A thread is abandoned and replaced with a new thread if it executes a task for a period of time longer than execution timeout and all attempts to interrupt it fail.",
                "getThreadAbandonedCount",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property ThreadAverageActiveCount
            {
            mapInfo.put("ThreadAverageActiveCount", new Object[]
                {
                "The average number of active (not idle) threads in the service thread pool since the last time the statistics were reset.",
                "getThreadAverageActiveCount",
                null,
                "F",
                "metrics.value=_default",
                });
            }
        
        // property ThreadCount
            {
            mapInfo.put("ThreadCount", new Object[]
                {
                "The number of threads in the service thread pool. For services that support dynamic thread pool sizing, this is the current thread pool size.",
                "getThreadCount",
                "setThreadCount",
                "I",
                "rest.collector=set,metrics.value=_default",
                });
            }
        
        // property ThreadCountMax
            {
            mapInfo.put("ThreadCountMax", new Object[]
                {
                "The maximum thread count allowed for this service when dynamic thread pool sizing is enabled.",
                "getThreadCountMax",
                "setThreadCountMax",
                "I",
                "rest.collector=set",
                });
            }
        
        // property ThreadCountMin
            {
            mapInfo.put("ThreadCountMin", new Object[]
                {
                "The minimum thread count for this service when dynamic thread pool sizing is enabled.",
                "getThreadCountMin",
                "setThreadCountMin",
                "I",
                "rest.collector=set",
                });
            }
        
        // property ThreadCountUpdateTime
            {
            mapInfo.put("ThreadCountUpdateTime", new Object[]
                {
                "The last time an update was made to the ThreadCount.  This attribute is only valid when ThreadPoolSizingEnabled is true.",
                "getThreadCountUpdateTime",
                null,
                "Ljava/util/Date;",
                "rest.collector=set",
                });
            }
        
        // property ThreadIdleCount
            {
            mapInfo.put("ThreadIdleCount", new Object[]
                {
                "The number of currently idle threads in the service thread pool.",
                "getThreadIdleCount",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property ThreadPoolSizingEnabled
            {
            mapInfo.put("ThreadPoolSizingEnabled", new Object[]
                {
                "Whether or not dynamic thread pool sizing is enabled for this service.",
                "isThreadPoolSizingEnabled",
                null,
                "Z",
                "rest.collector=set",
                });
            }
        
        // property TransportAddress
            {
            mapInfo.put("TransportAddress", new Object[]
                {
                "The service dedicated transport address if any.  When present this transport allows the service instance to communicate with other service members via a dedicated transport rather then using the shared cluster transport.",
                "getTransportAddress",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property TransportBackloggedConnectionList
            {
            mapInfo.put("TransportBackloggedConnectionList", new Object[]
                {
                "A list of currently backlogged connections on the service dedicated transport.",
                "getTransportBackloggedConnectionList",
                null,
                "[Ljava/lang/String;",
                null,
                });
            }
        
        // property TransportBackloggedConnections
            {
            mapInfo.put("TransportBackloggedConnections", new Object[]
                {
                "The number of currently backlogged connections on the service dedicated transport.  Any new requests which require the connection will block until the backlog is cleared.",
                "getTransportBackloggedConnections",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TransportConnections
            {
            mapInfo.put("TransportConnections", new Object[]
                {
                "The number of connections currently maintained by the service dedicated transport.  This count may be lower than MemberCount if some members have been not been configured to use the dedicated transport, or if it has been identified that there is no advantage in using the dedicated transport for communication with certain members.",
                "getTransportConnections",
                null,
                "I",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TransportReceivedBytes
            {
            mapInfo.put("TransportReceivedBytes", new Object[]
                {
                "The number of bytes received by the service dedicated transport since the last time the statistics were reset.",
                "getTransportReceivedBytes",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TransportReceivedMessages
            {
            mapInfo.put("TransportReceivedMessages", new Object[]
                {
                "The number of messages received by the service dedicated transport since the last time the statistics were reset.",
                "getTransportReceivedMessages",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TransportRetainedBytes
            {
            mapInfo.put("TransportRetainedBytes", new Object[]
                {
                "The number of bytes retained by the service dedicated transport awaiting delivery acknowledgment.  This memory is allocated outside of the Java GC heap space.",
                "getTransportRetainedBytes",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TransportSentBytes
            {
            mapInfo.put("TransportSentBytes", new Object[]
                {
                "The number of bytes sent by the service dedicated transport since the last time the statistics were reset.",
                "getTransportSentBytes",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TransportSentMessages
            {
            mapInfo.put("TransportSentMessages", new Object[]
                {
                "The number of messages sent by the service dedicated transport since the last time the statistics were reset.",
                "getTransportSentMessages",
                null,
                "J",
                "rest.collector=sum,metrics.value=_default",
                });
            }
        
        // property TransportStatus
            {
            mapInfo.put("TransportStatus", new Object[]
                {
                "The service dedicated transport status information. \n\nrest.collector=set",
                "getTransportStatus",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property Type
            {
            mapInfo.put("Type", new Object[]
                {
                "The type identifier of the service.",
                "getType",
                null,
                "Ljava/lang/String;",
                "rest.collector=set,metrics.tag=type",
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
        
        // behavior getServiceDescription()
            {
            mapInfo.put("getServiceDescription()", new Object[]
                {
                "Get service description",
                "getServiceDescription",
                "Ljava/lang/String;",
                new String[] {},
                new String[] {},
                null,
                });
            }

        // behavior reportDistributionState(boolean fVerbose)
            {
            mapInfo.put("reportDistributionState(Z)", new Object[]
                {
                "Report partition distributions for which this service member is either the sender or receiver, and which are still in-progress since the last partition assignment analysis. Verbose mode includes partition numbers for all pending or scheduled partitions in the report.",
                "reportDistributionState",
                "Ljava/lang/String;",
                new String[] {"fVerbose", },
                new String[] {"Z", },
                null,
                });
            }
        
        // behavior reportOwnership(boolean fVerbose)
            {
            mapInfo.put("reportOwnership(Z)", new Object[]
                {
                "Report the ownership summary.  If called with the verbose flag set to true, include specific partition numbers in the report. Hint: machine-safe partitions are marked with a '+'; vulnerable partitions are not.",
                "reportOwnership",
                "Ljava/lang/String;",
                new String[] {"fVerbose", },
                new String[] {"Z", },
                null,
                });
            }
        
        // behavior resetStatistics()
            {
            mapInfo.put("resetStatistics()", new Object[]
                {
                "Reset the service statistics.",
                "resetStatistics",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior showTransportStatus()
            {
            mapInfo.put("showTransportStatus()", new Object[]
                {
                "Show detailed status information on the reliable transport.",
                "showTransportStatus",
                "Ljava/lang/String;",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior shutdown()
            {
            mapInfo.put("shutdown()", new Object[]
                {
                "Stop the service. This is a controlled shut-down, and is preferred to the 'stop' method.",
                "shutdown",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior start()
            {
            mapInfo.put("start()", new Object[]
                {
                "Start the service.",
                "start",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior stop()
            {
            mapInfo.put("stop()", new Object[]
                {
                "Hard-stop the service. Use 'shutdown()' method for normal service termination.",
                "stop",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        return mapInfo;
        }
    
    // Accessor for the property "BackupCount"
    /**
     * Getter for property BackupCount.<p>
    * The number of backups for every cache storage.
    * 
    * @descriptor rest.collector=set
     */
    public int getBackupCount()
        {
        return 0;
        }
    
    // Accessor for the property "BackupCountAfterWritebehind"
    /**
     * Getter for property BackupCountAfterWritebehind.<p>
    * The number of members of the partitioned (distributed) cache service that
    * will retain backup data that does _not_ require write-behind, i.e. data
    * that is not vulnerable to being lost even if the entire cluster were shut
    * down.
    * 
    * @descriptor rest.collector=set
     */
    public int getBackupCountAfterWritebehind()
        {
        return 0;
        }
    
    // Accessor for the property "EventBacklog"
    /**
     * Getter for property EventBacklog.<p>
    * The size of the backlog queue that holds events scheduled to be processed
    * by the EventDispatcher thread.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getEventBacklog()
        {
        return 0;
        }
    
    // Accessor for the property "EventCount"
    /**
     * Getter for property EventCount.<p>
    * The total number of processed events since the last time the statistics
    * were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getEventCount()
        {
        return 0L;
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
    
    // Accessor for the property "IndexingTotalMillis"
    /**
     * Getter for property IndexingTotalMillis.<p>
    * The total amount of time taken to build indices for all storage instances
    * and partitions owned by this member. Building of indices may be performed
    * in parallel thus this value may be less than suming
    * StorageManagerMBean.IndexingTotalMillis.
    * 
    * @descriptor rest.collector=max,metrics.value=_default
     */
    public long getIndexingTotalMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "JoinTime"
    /**
     * Getter for property JoinTime.<p>
    * The date/time value (in cluster time) that this Member joined the service.
     */
    public java.util.Date getJoinTime()
        {
        return null;
        }
    
    // Accessor for the property "MemberCount"
    /**
     * Getter for property MemberCount.<p>
    * Specifies the total number of cluster nodes running this service.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public int getMemberCount()
        {
        return 0;
        }
    
    // Accessor for the property "MessagesLocal"
    /**
     * Getter for property MessagesLocal.<p>
    * The total number of messages which were self-addressed messages since the
    * last time the statistics were reset.  Such messages are used for
    * servicing process-local requests and do not have an associated network
    * cost.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getMessagesLocal()
        {
        return 0L;
        }
    
    // Accessor for the property "MessagesReceived"
    /**
     * Getter for property MessagesReceived.<p>
    * The total number of messages received by this service since the last time
    * the statistics were reset. This value accounts for messages received by
    * any (local, dedicated or shared) transport.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getMessagesReceived()
        {
        return 0L;
        }
    
    // Accessor for the property "MessagesSent"
    /**
     * Getter for property MessagesSent.<p>
    * The number of messages sent by this service since the last time the
    * statistics were reset. This value accounts for messages sent by any
    * (local, dedicated or shared) transport.
    * 
    * @descriptor
    * rest.collector=sum,metrics.value=_default
     */
    public long getMessagesSent()
        {
        return 0L;
        }
    
    // Accessor for the property "OutgoingTransferCount"
    /**
     * Getter for property OutgoingTransferCount.<p>
    * The number of partitions that are currently being transferred by this
    * service member to other members.
    * 
    * @descriptor rest.collector=sum
     */
    public int getOutgoingTransferCount()
        {
        return 0;
        }
    
    // Accessor for the property "OwnedPartitionsBackup"
    /**
     * Getter for property OwnedPartitionsBackup.<p>
    * The number of partitions that this Member backs up (responsible for the
    * backup storage).
    * 
    * @descriptor rest.collector=list,metrics.value=_default
     */
    public int getOwnedPartitionsBackup()
        {
        return 0;
        }
    
    // Accessor for the property "OwnedPartitionsPrimary"
    /**
     * Getter for property OwnedPartitionsPrimary.<p>
    * The number of partitions that this Member owns (responsible for the
    * primary storage).
    * 
    * @descriptor rest.collector=list,metrics.value=_default
     */
    public int getOwnedPartitionsPrimary()
        {
        return 0;
        }
    
    // Accessor for the property "PartitionsAll"
    /**
     * Getter for property PartitionsAll.<p>
    * The total number of partitions that every cache storage will be divided
    * into.
    * 
    * @descriptor rest.collector=set
     */
    public int getPartitionsAll()
        {
        return 0;
        }
    
    // Accessor for the property "PartitionsEndangered"
    /**
     * Getter for property PartitionsEndangered.<p>
    * The total number of partitions that are not currently backed up.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public int getPartitionsEndangered()
        {
        return 0;
        }
    
    // Accessor for the property "PartitionsUnbalanced"
    /**
     * Getter for property PartitionsUnbalanced.<p>
    * The total number of primary and backup partitions which remain to be
    * transferred until the partition distribution across the storage enabled
    * service members is fully balanced.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public int getPartitionsUnbalanced()
        {
        return 0;
        }
    
    // Accessor for the property "PartitionsVulnerable"
    /**
     * Getter for property PartitionsVulnerable.<p>
    * The total number of partitions that are backed up on the same machine
    * where the primary partition owner resides.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public int getPartitionsVulnerable()
        {
        return 0;
        }
    
    // Accessor for the property "PersistenceActiveSpaceAvailable"
    /**
     * Getter for property PersistenceActiveSpaceAvailable.<p>
    * The total remaining free space (in bytes) of the file system used by the
    * persistence layer to persist active cache data.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public long getPersistenceActiveSpaceAvailable()
        {
        return 0L;
        }
    
    // Accessor for the property "PersistenceActiveSpaceTotal"
    /**
     * Getter for property PersistenceActiveSpaceTotal.<p>
    * The total size (in bytes) of the file system used by the persistence
    * layer to persist active cache data.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public long getPersistenceActiveSpaceTotal()
        {
        return 0L;
        }
    
    // Accessor for the property "PersistenceActiveSpaceUsed"
    /**
     * Getter for property PersistenceActiveSpaceUsed.<p>
    * The total size (in bytes) used by the persistence layer to persist active
    * cache data.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public long getPersistenceActiveSpaceUsed()
        {
        return 0L;
        }
    
    // Accessor for the property "PersistenceBackupSpaceAvailable"
    /**
     * Getter for property PersistenceBackupSpaceAvailable.<p>
    * The total remaining free space (in bytes) of the file system used by the
    * persistence layer to persist backup cache data.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public long getPersistenceBackupSpaceAvailable()
        {
        return 0L;
        }
    
    // Accessor for the property "PersistenceBackupSpaceTotal"
    /**
     * Getter for property PersistenceBackupSpaceTotal.<p>
    * The total size (in bytes) of the file system used by the persistence
    * layer to persist backup cache data.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public long getPersistenceBackupSpaceTotal()
        {
        return 0L;
        }
    
    // Accessor for the property "PersistenceBackupSpaceUsed"
    /**
     * Getter for property PersistenceBackupSpaceUsed.<p>
    * The total size (in bytes) used by the persistence layer to persist backup
    * cache data.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public long getPersistenceBackupSpaceUsed()
        {
        return 0L;
        }
    
    // Accessor for the property "PersistenceEnvironment"
    /**
     * Getter for property PersistenceEnvironment.<p>
    * A description of the configured persistence environment or 'n/a' if one
    * has not been configured.
    * 
    * @descriptor rest.collector=set
     */
    public String getPersistenceEnvironment()
        {
        return null;
        }
    
    // Accessor for the property "PersistenceLatencyAverage"
    /**
     * Getter for property PersistenceLatencyAverage.<p>
    * The average latency (in millis) added to a mutating cache operation by
    * active persistence operations.
    * 
    * @descriptor metrics.value=_default
     */
    public float getPersistenceLatencyAverage()
        {
        return 0.0F;
        }
    
    // Accessor for the property "PersistenceLatencyMax"
    /**
     * Getter for property PersistenceLatencyMax.<p>
    * The maximum latency (in millis) added to a mutating cache operation by an
    * active persistence operation.
    * 
    * @descriptor rest.collector=max,metrics.value=_default
     */
    public long getPersistenceLatencyMax()
        {
        return 0L;
        }
    
    // Accessor for the property "PersistenceMode"
    /**
     * Getter for property PersistenceMode.<p>
    * The current persistence mode for this service.  A value of 'active'
    * indicates that all mutating cache operations (i.e. writes) will be
    * persisted via the configured persistence-environment. A value of 'live'
    * indicates that all reads and writes will be performed against the
    * configured persistence-environment.  A value of 'on-demand' indicates
    * that a persistence-environment has been configured and is available but
    * is not being actively used. In all modes a persistent snapshot can be
    * taken of all caches managed by this service using the configured
    * persistence-environment. A value of 'n/a' indicates that persistence is
    * not configured for this service.
    * 
    * @descriptor rest.collector=set
     */
    public String getPersistenceMode()
        {
        return null;
        }
    
    // Accessor for the property "PersistenceSnapshotArchiver"
    /**
     * Getter for property PersistenceSnapshotArchiver.<p>
    * A description of the configured snapshot archiver or 'n/a' if one has not
    * been configured.
    * 
    * @descriptor rest.collector=set
     */
    public String getPersistenceSnapshotArchiver()
        {
        return null;
        }
    
    // Accessor for the property "PersistenceSnapshotSpaceAvailable"
    /**
     * Getter for property PersistenceSnapshotSpaceAvailable.<p>
    * The total remaining free space (in bytes) of the file system used by the
    * persistence layer to store snapshots.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public long getPersistenceSnapshotSpaceAvailable()
        {
        return 0L;
        }
    
    // Accessor for the property "PersistenceSnapshotSpaceTotal"
    /**
     * Getter for property PersistenceSnapshotSpaceTotal.<p>
    * The total size (in bytes) of the file system used by the persistence
    * layer to store snapshots.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public long getPersistenceSnapshotSpaceTotal()
        {
        return 0L;
        }
    
    // Accessor for the property "QuorumStatus"
    /**
     * Getter for property QuorumStatus.<p>
    * The current state of the service quorum.
    * 
    * @descriptor rest.collector=set
     */
    public String getQuorumStatus()
        {
        return null;
        }
    
    // Accessor for the property "RequestAverageDuration"
    /**
     * Getter for property RequestAverageDuration.<p>
    * The average duration (in milliseconds) of an individual request issued by
    * the service since the last time the statistics were reset.
    * 
    * @descriptor metrics.value=_default
     */
    public float getRequestAverageDuration()
        {
        return 0.0F;
        }
    
    // Accessor for the property "RequestMaxDuration"
    /**
     * Getter for property RequestMaxDuration.<p>
    * The maximum duration (in milliseconds) of a request issued by the service
    * since the last time the statistics were reset.
    * 
    * @descriptor rest.collector=max,metrics.value=_default
     */
    public long getRequestMaxDuration()
        {
        return 0L;
        }
    
    // Accessor for the property "RequestPendingCount"
    /**
     * Getter for property RequestPendingCount.<p>
    * The number of pending requests issued by the service.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getRequestPendingCount()
        {
        return 0L;
        }
    
    // Accessor for the property "RequestPendingDuration"
    /**
     * Getter for property RequestPendingDuration.<p>
    * The duration (in milliseconds) of the oldest pending request issued by
    * the service.
    * 
    * @descriptor metrics.value=_default
     */
    public long getRequestPendingDuration()
        {
        return 0L;
        }
    
    // Accessor for the property "RequestTimeoutCount"
    /**
     * Getter for property RequestTimeoutCount.<p>
    * The total number of timed-out requests since the last time the statistics
    * were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getRequestTimeoutCount()
        {
        return 0L;
        }
    
    // Accessor for the property "RequestTimeoutMillis"
    /**
     * Getter for property RequestTimeoutMillis.<p>
    * The default timeout value in milliseconds for requests that can be
    * timed-out (e.g. implement the com.tangosol.net.PriorityTask interface),
    * but do not explicitly specify the request timeout value.
    * 
    * @descriptor rest.collector=set
     */
    public long getRequestTimeoutMillis()
        {
        return __m_RequestTimeoutMillis;
        }
    
    // Accessor for the property "RequestTotalCount"
    /**
     * Getter for property RequestTotalCount.<p>
    * The total number of synchronous requests issued by the service since the
    * last time the statistics were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getRequestTotalCount()
        {
        return 0L;
        }
    
    // Accessor for the property "SeniorMemberId"
    /**
     * Getter for property SeniorMemberId.<p>
    * The service senior member id; -1 if the service is not running.
    * 
    * @descriptor rest.collector=set
     */
    public int getSeniorMemberId()
        {
        return 0;
        }
    
    // Accessor for the property "Statistics"
    /**
     * Getter for property Statistics.<p>
    * Statistics for this service in human readable format.
     */
    public String getStatistics()
        {
        return null;
        }
    
    // Accessor for the property "StatusHA"
    /**
     * Getter for property StatusHA.<p>
    * The High Availability status for this service. The value of MACHINE-SAFE
    * means that all the cluster nodes running on any given machine could be
    * stopped at once without data loss. The value of NODE-SAFE means that any
    * cluster node could be stopped without data loss.  The value of ENDANGERED
    * indicates that abnormal termination of any cluster node that runs this
    * service may cause data loss. If either or both site and rack groupings
    * are configured, the values SITE-SAFE and RACK-SAFE mean that all the
    * cluster nodes running on any given site or rack, respectively,  could be
    * stopped at once without data loss.
    * 
    * @descriptor rest.collector=set
     */
    public String getStatusHA()
        {
        return null;
        }
    
    // Accessor for the property "StatusHACode"
    /**
     * Getter for property StatusHACode.<p>
    * The High Availability status for this service. The value of 3
    * (MACHINE-SAFE) means that all the cluster nodes running on any given
    * machine could be stoppped at once without data loss. The value of 2
    * (NODE-SAFE) means that any cluster node could be stoppped without data
    * loss.  The value of 1 (ENDANGERED) indicates that abnormal termination of
    * any cluster node that runs this service may cause data loss. If the rack
    * and site names are configured, the values 5 (SITE-SAFE) and 4 (RACK-SAFE)
    * mean that all the cluster nodes running on any given site or rack,
    * respectively, could be stoppped without data loss. If StatusHA is not
    * applicable for the service, -1 is returned.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public int getStatusHACode()
        {
        return 0;
        }
    
    // Accessor for the property "StorageEnabledCount"
    /**
     * Getter for property StorageEnabledCount.<p>
    * Specifies the total number of cluster nodes running this Service for
    * which local storage is enabled.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public int getStorageEnabledCount()
        {
        return 0;
        }
    
    // Accessor for the property "TaskAverageDuration"
    /**
     * Getter for property TaskAverageDuration.<p>
    * The average duration (in milliseconds) of an individual task execution.
    * 
    * @descriptor metrics.value=_default
     */
    public float getTaskAverageDuration()
        {
        return 0.0F;
        }
    
    // Accessor for the property "TaskBacklog"
    /**
     * Getter for property TaskBacklog.<p>
    * The size of the backlog queue that holds tasks scheduled to be executed
    * by one of the service threads.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getTaskBacklog()
        {
        return 0;
        }
    
    // Accessor for the property "TaskCount"
    /**
     * Getter for property TaskCount.<p>
    * The total number of executed tasks since the last time the statistics
    * were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getTaskCount()
        {
        return 0L;
        }
    
    // Accessor for the property "TaskHungCount"
    /**
     * Getter for property TaskHungCount.<p>
    * The total number of currently executing hung tasks.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getTaskHungCount()
        {
        return 0;
        }
    
    // Accessor for the property "TaskHungDuration"
    /**
     * Getter for property TaskHungDuration.<p>
    * The longest currently executing hung task duration in milliseconds.
    * 
    * @descriptor rest.collector=max,metrics.value=_default
     */
    public long getTaskHungDuration()
        {
        return 0L;
        }
    
    // Accessor for the property "TaskHungTaskId"
    /**
     * Getter for property TaskHungTaskId.<p>
    * The id of the of the longest currently executing hung task.
    * 
    * @descriptor rest.collector=set
     */
    public String getTaskHungTaskId()
        {
        return null;
        }
    
    // Accessor for the property "TaskHungThresholdMillis"
    /**
     * Getter for property TaskHungThresholdMillis.<p>
    * The amount of time in milliseconds that a task can execute before it is
    * considered hung. Note that a posted task that has not yet started is
    * never considered as hung.
    * 
    * @descriptor rest.collector=set
     */
    public long getTaskHungThresholdMillis()
        {
        return __m_TaskHungThresholdMillis;
        }
    
    // Accessor for the property "TaskMaxBacklog"
    /**
     * Getter for property TaskMaxBacklog.<p>
    * The maximum size of the backlog queue since the last time the statistics
    * were reset.
    * 
    * @descriptor rest.collector=max,metrics.value=_default
     */
    public int getTaskMaxBacklog()
        {
        return 0;
        }
    
    // Accessor for the property "TaskTimeoutCount"
    /**
     * Getter for property TaskTimeoutCount.<p>
    * The total number of timed-out tasks since the last time the statistics
    * were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getTaskTimeoutCount()
        {
        return 0;
        }
    
    // Accessor for the property "TaskTimeoutMillis"
    /**
     * Getter for property TaskTimeoutMillis.<p>
    * The default timeout value in milliseconds for tasks that can be timed-out
    * (e.g. implement the com.tangosol.net.PriorityTask interface), but do not
    * explicitly specify the task execution timeout value.
    * 
    * @descriptor rest.collector=set
     */
    public long getTaskTimeoutMillis()
        {
        return __m_TaskTimeoutMillis;
        }
    
    // Accessor for the property "ThreadAbandonedCount"
    /**
     * Getter for property ThreadAbandonedCount.<p>
    * The number of abandoned threads from the service thread pool. A thread is
    * abandoned and replaced with a new thread if it executes a task for a
    * period of time longer than execution timeout and all attempts to
    * interrupt it fail.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getThreadAbandonedCount()
        {
        return 0;
        }
    
    // Accessor for the property "ThreadAverageActiveCount"
    /**
     * Getter for property ThreadAverageActiveCount.<p>
    * The average number of active (not idle) threads in the service thread
    * pool since the last time the statistics were reset.
    * 
    * @descriptor metrics.value=_default
     */
    public float getThreadAverageActiveCount()
        {
        return 0.0F;
        }
    
    // Accessor for the property "ThreadCount"
    /**
     * Getter for property ThreadCount.<p>
    * The number of threads in the service thread pool. For services that
    * support dynamic thread pool sizing, this is the current thread pool size.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public int getThreadCount()
        {
        return __m_ThreadCount;
        }
    
    // Accessor for the property "ThreadCountMax"
    /**
     * Getter for property ThreadCountMax.<p>
    * The maximum thread count allowed for this service when dynamic thread
    * pool sizing is enabled.
    * 
    * @descriptor rest.collector=set
     */
    public int getThreadCountMax()
        {
        return __m_ThreadCountMax;
        }
    
    // Accessor for the property "ThreadCountMin"
    /**
     * Getter for property ThreadCountMin.<p>
    * The minimum thread count for this service when dynamic thread pool sizing
    * is enabled.
    * 
    * @descriptor rest.collector=set
     */
    public int getThreadCountMin()
        {
        return __m_ThreadCountMin;
        }
    
    // Accessor for the property "ThreadCountUpdateTime"
    /**
     * Getter for property ThreadCountUpdateTime.<p>
    * The last time an update was made to the ThreadCount.  This attribute is
    * only valid when ThreadPoolSizingEnabled is true.
    * 
    * @descriptor rest.collector=set
     */
    public java.util.Date getThreadCountUpdateTime()
        {
        return null;
        }
    
    // Accessor for the property "ThreadIdleCount"
    /**
     * Getter for property ThreadIdleCount.<p>
    * The number of currently idle threads in the service thread pool.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getThreadIdleCount()
        {
        return 0;
        }
    
    // Accessor for the property "TransportAddress"
    /**
     * Getter for property TransportAddress.<p>
    * The service dedicated transport address if any.  When present this
    * transport allows the service instance to communicate with other service
    * members via a dedicated transport rather then using the shared cluster
    * transport.
     */
    public String getTransportAddress()
        {
        return null;
        }
    
    // Accessor for the property "TransportBackloggedConnectionList"
    /**
     * Getter for property TransportBackloggedConnectionList.<p>
    * A list of currently backlogged connections on the service dedicated
    * transport.
     */
    public String[] getTransportBackloggedConnectionList()
        {
        return null;
        }
    
    // Accessor for the property "TransportBackloggedConnections"
    /**
     * Getter for property TransportBackloggedConnections.<p>
    * The number of currently backlogged connections on the service dedicated
    * transport.  Any new requests which require the connection will block
    * until the backlog is cleared.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getTransportBackloggedConnections()
        {
        return 0;
        }
    
    // Accessor for the property "TransportConnections"
    /**
     * Getter for property TransportConnections.<p>
    * The number of connections currently maintained by the service dedicated
    * transport.  This count may be lower than MemberCount if some members have
    * been not been configured to use the dedicated transport, or if it has
    * been identified that there is no advantage in using the dedicated
    * transport for communication with certain members.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public int getTransportConnections()
        {
        return 0;
        }
    
    // Accessor for the property "TransportReceivedBytes"
    /**
     * Getter for property TransportReceivedBytes.<p>
    * The number of bytes received by the service dedicated transport since the
    * last time the statistics were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getTransportReceivedBytes()
        {
        return 0L;
        }
    
    // Accessor for the property "TransportReceivedMessages"
    /**
     * Getter for property TransportReceivedMessages.<p>
    * The number of messages received by the service dedicated transport since
    * the last time the statistics were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getTransportReceivedMessages()
        {
        return 0L;
        }
    
    // Accessor for the property "TransportRetainedBytes"
    /**
     * Getter for property TransportRetainedBytes.<p>
    * The number of bytes retained by the service dedicated transport awaiting
    * delivery acknowledgment.  This memory is allocated outside of the Java GC
    * heap space.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getTransportRetainedBytes()
        {
        return 0L;
        }
    
    // Accessor for the property "TransportSentBytes"
    /**
     * Getter for property TransportSentBytes.<p>
    * The number of bytes sent by the service dedicated transport since the
    * last time the statistics were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getTransportSentBytes()
        {
        return 0L;
        }
    
    // Accessor for the property "TransportSentMessages"
    /**
     * Getter for property TransportSentMessages.<p>
    * The number of messages sent by the service dedicated transport since the
    * last time the statistics were reset.
    * 
    * @descriptor rest.collector=sum,metrics.value=_default
     */
    public long getTransportSentMessages()
        {
        return 0L;
        }
    
    // Accessor for the property "TransportStatus"
    /**
     * Getter for property TransportStatus.<p>
    * The service dedicated transport status information. 
    * 
    * rest.collector=set
     */
    public String getTransportStatus()
        {
        return null;
        }
    
    // Accessor for the property "Type"
    /**
     * Getter for property Type.<p>
    * The type identifier of the service.
    * 
    * @descriptor rest.collector=set,metrics.tag=type
     */
    public String getType()
        {
        return null;
        }
    
    // Accessor for the property "Running"
    /**
     * Getter for property Running.<p>
    * Specifies whether or not the service is running.
    * 
    * @descriptor rest.collector=set
     */
    public boolean isRunning()
        {
        return false;
        }
    
    // Accessor for the property "StorageEnabled"
    /**
     * Getter for property StorageEnabled.<p>
    * Specifies whether or not the local storage is enabled for this cluster
    * Member.
    * 
    * @descriptor rest.collector=set
     */
    public boolean isStorageEnabled()
        {
        return false;
        }
    
    // Accessor for the property "ThreadPoolSizingEnabled"
    /**
     * Getter for property ThreadPoolSizingEnabled.<p>
    * Whether or not dynamic thread pool sizing is enabled for this service.
    * 
    * @descriptor rest.collector=set
     */
    public boolean isThreadPoolSizingEnabled()
        {
        return false;
        }
    
    /**
     * Report partition distributions for which this service member is either
    * the sender or receiver, and which are still in-progress since the last
    * partition assignment analysis. Verbose mode includes partition numbers
    * for all pending or scheduled partitions in the report.
     */
    public String reportDistributionState(boolean fVerbose)
        {
        return null;
        }
    
    /**
     * Report the ownership summary.  If called with the verbose flag set to
    * true, include specific partition numbers in the report. Hint:
    * machine-safe partitions are marked with a '+'; vulnerable partitions are
    * not.
     */
    public String reportOwnership(boolean fVerbose)
        {
        return null;
        }
    
    /**
     * Reset the service statistics.
     */
    public void resetStatistics()
        {
        }
    
    // Accessor for the property "RequestTimeoutMillis"
    /**
     * Setter for property RequestTimeoutMillis.<p>
    * The default timeout value in milliseconds for requests that can be
    * timed-out (e.g. implement the com.tangosol.net.PriorityTask interface),
    * but do not explicitly specify the request timeout value.
    * 
    * @descriptor rest.collector=set
     */
    public void setRequestTimeoutMillis(long cMillis)
        {
        __m_RequestTimeoutMillis = cMillis;
        }
    
    // Accessor for the property "TaskHungThresholdMillis"
    /**
     * Setter for property TaskHungThresholdMillis.<p>
    * The amount of time in milliseconds that a task can execute before it is
    * considered hung. Note that a posted task that has not yet started is
    * never considered as hung.
    * 
    * @descriptor rest.collector=set
     */
    public void setTaskHungThresholdMillis(long cMillis)
        {
        __m_TaskHungThresholdMillis = cMillis;
        }
    
    // Accessor for the property "TaskTimeoutMillis"
    /**
     * Setter for property TaskTimeoutMillis.<p>
    * The default timeout value in milliseconds for tasks that can be timed-out
    * (e.g. implement the com.tangosol.net.PriorityTask interface), but do not
    * explicitly specify the task execution timeout value.
    * 
    * @descriptor rest.collector=set
     */
    public void setTaskTimeoutMillis(long cMillis)
        {
        __m_TaskTimeoutMillis = cMillis;
        }
    
    // Accessor for the property "ThreadCount"
    /**
     * Setter for property ThreadCount.<p>
    * The number of threads in the service thread pool. For services that
    * support dynamic thread pool sizing, this is the current thread pool size.
    * 
    * @descriptor rest.collector=set,metrics.value=_default
     */
    public void setThreadCount(int cThreads)
        {
        __m_ThreadCount = cThreads;
        }
    
    // Accessor for the property "ThreadCountMax"
    /**
     * Setter for property ThreadCountMax.<p>
    * The maximum thread count allowed for this service when dynamic thread
    * pool sizing is enabled.
    * 
    * @descriptor rest.collector=set
     */
    public void setThreadCountMax(int nMax)
        {
        __m_ThreadCountMax = nMax;
        }
    
    // Accessor for the property "ThreadCountMin"
    /**
     * Setter for property ThreadCountMin.<p>
    * The minimum thread count for this service when dynamic thread pool sizing
    * is enabled.
    * 
    * @descriptor rest.collector=set
     */
    public void setThreadCountMin(int nMin)
        {
        __m_ThreadCountMin = nMin;
        }
    
    /**
     * Show detailed status information on the reliable transport.
     */
    public String showTransportStatus()
        {
        return null;
        }
    
    /**
     * Stop the service. This is a controlled shut-down, and is preferred to the
    * 'stop' method.
     */
    public void shutdown()
        {
        }
    
    /**
     * Start the service.
     */
    public void start()
        {
        }
    
    /**
     * Hard-stop the service. Use 'shutdown()' method for normal service
    * termination.
     */
    public void stop()
        {
        }
    }
