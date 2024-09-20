
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.ServiceModel

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.coherence.component.manageable.ModelAdapter;
import com.tangosol.coherence.component.net.MessageHandler;
import com.tangosol.coherence.component.net.Poll;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.coherence.component.util.DaemonPool;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.WindowedArray;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.internal.net.socketbus.AbstractSocketBus;
import com.oracle.coherence.common.net.exabus.MessageBus;
import com.oracle.coherence.persistence.PersistenceEnvironment;
import com.tangosol.coherence.component.util.safeService.SafeProxyService;
import com.tangosol.internal.health.HealthCheckDependencies;
import com.tangosol.internal.net.service.grid.PersistenceDependencies;
import com.tangosol.internal.util.MessagePublisher;
import com.tangosol.net.ActionPolicy;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.Service;
import com.tangosol.net.ServiceDependencies;
import com.tangosol.net.events.internal.ServiceDispatcher;
import com.tangosol.net.messaging.ConnectionAcceptor;
import com.tangosol.persistence.PersistenceEnvironmentInfo;
import com.tangosol.persistence.SafePersistenceWrappers;
import com.tangosol.persistence.SnapshotArchiver;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * 
 * Generic ServiceMBean
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ServiceModel
        extends    com.tangosol.coherence.component.net.management.model.LocalModel
        implements com.tangosol.util.HealthCheck
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Service
     *
     * Service object associated with this model.
     */
    private transient com.tangosol.net.Service __m__Service;
    
    /**
     * Property _ServiceImplRef
     *
     * Underlying "real" Service object wrapped in WeakReference to avoid
     * resource leakage.
     */
    private transient java.lang.ref.WeakReference __m__ServiceImplRef;
    
    /**
     * Property _ServiceName
     *
     * Name of the service. We need to store it to be able to restart the
     * service from this model.
     */
    private String __m__ServiceName;
    
    /**
     * Property _ServiceRef
     *
     * Service object associated with this model wrapped in WeakReference to
     * avoid resource leakage.
     */
    private transient java.lang.ref.WeakReference __m__ServiceRef;
    
    /**
     * Property HealthyReady
     *
     */
    private boolean __m_HealthyReady;
    
    /**
     * Property RequestTimeoutMillis
     *
     * The default timeout value in milliseconds for requests that can be
     * timed-out (e.g. implement the com.tangosol.net.PriorityTask interface),
     * but do not explicitly specify the request timeout value.
     */
    private transient long __m_RequestTimeoutMillis;
    
    /**
     * Property TaskHungThresholdMillis
     *
     * The amount of time in milliseconds that a task can execute before it is
     * considered hung. Note that a posted task that has not yet started is
     * never considered as hung.
     */
    private transient long __m_TaskHungThresholdMillis;
    
    /**
     * Property TaskTimeoutMillis
     *
     * The default timeout value in milliseconds for tasks that can be
     * timed-out (e.g. implement the com.tangosol.net.PriorityTask interface),
     * but do not explicitly specify the execution timeout value.
     */
    private transient long __m_TaskTimeoutMillis;
    
    /**
     * Property THREAD_COUNT_LIMIT
     *
     */
    public static final int THREAD_COUNT_LIMIT = 512;
    
    /**
     * Property ThreadCount
     *
     * The number of threads in the service thread pool. For services that
     * support dynamic thread pool sizing, this is the current thread pool size.
     */
    private int __m_ThreadCount;
    
    /**
     * Property ThreadCountMax
     *
     * The maximum thread count allowed for this service when dynamic thread
     * pool sizing is enabled.
     */
    private transient int __m_ThreadCountMax;
    
    /**
     * Property ThreadCountMin
     *
     * The minimum thread count for this service when dynamic thread pool
     * sizing is enabled.
     */
    private transient int __m_ThreadCountMin;
    
    // Default constructor
    public ServiceModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ServiceModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setHealthyReady(false);
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
        return new com.tangosol.coherence.component.net.management.model.localModel.ServiceModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/ServiceModel".replace('/', '.'));
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
    
    // Accessor for the property "_DaemonPool"
    /**
     * Getter for property _DaemonPool.<p>
    * Running DaemonPool or null.
     */
    protected com.tangosol.coherence.component.util.DaemonPool get_DaemonPool()
        {
        // import Component.Util.Daemon.QueueProcessor.Service as com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ProxyService;
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.Service serviceImpl = get_ServiceImpl();
        
        // extract the ProxyService Acceptor's DaemonPool, if applicable
        if (serviceImpl instanceof ProxyService)
            {
            ConnectionAcceptor acceptor = ((ProxyService) serviceImpl).getAcceptor();
            serviceImpl = acceptor instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.Service ? (com.tangosol.coherence.component.util.daemon.queueProcessor.Service) acceptor : null;
            }
        
        return serviceImpl == null ? null : serviceImpl.getDaemonPool();
        }
    
    // Accessor for the property "_MessageHandler"
    /**
     * Getter for property _MessageHandler.<p>
    * Underlying service's MessageHandler if any.
     */
    protected com.tangosol.coherence.component.net.MessageHandler get_MessageHandler()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid svc = get_ServiceImpl();
        return svc == null ? null : svc.getMessageHandler();
        }
    
    // Accessor for the property "_Service"
    /**
     * Getter for property _Service.<p>
    * Service object associated with this model.
     */
    public com.tangosol.net.Service get_Service()
        {
        // import com.tangosol.net.Service;
        // import java.lang.ref.WeakReference;
        
        WeakReference wr = get_ServiceRef();
        return wr == null ? null : (Service) wr.get();
        }
    
    // Accessor for the property "_ServiceImpl"
    /**
     * Getter for property _ServiceImpl.<p>
    * Underlying "real" Service object.
     */
    protected com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid get_ServiceImpl()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid;
        // import java.lang.ref.WeakReference;
        
        WeakReference wr = get_ServiceImplRef();
        return wr != null && isRunning() ? (Grid) wr.get() : null;
        }
    
    // Accessor for the property "_ServiceImplRef"
    /**
     * Getter for property _ServiceImplRef.<p>
    * Underlying "real" Service object wrapped in WeakReference to avoid
    * resource leakage.
     */
    protected java.lang.ref.WeakReference get_ServiceImplRef()
        {
        return __m__ServiceImplRef;
        }
    
    // Accessor for the property "_ServiceName"
    /**
     * Getter for property _ServiceName.<p>
    * Name of the service. We need to store it to be able to restart the
    * service from this model.
     */
    public String get_ServiceName()
        {
        return __m__ServiceName;
        }
    
    // Accessor for the property "_ServiceRef"
    /**
     * Getter for property _ServiceRef.<p>
    * Service object associated with this model wrapped in WeakReference to
    * avoid resource leakage.
     */
    protected java.lang.ref.WeakReference get_ServiceRef()
        {
        return __m__ServiceRef;
        }
    
    // Accessor for the property "BackupCount"
    /**
     * Getter for property BackupCount.<p>
    * The number of backups for every cache storage.
     */
    public int getBackupCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import com.tangosol.net.CacheService;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl instanceof PartitionedService)
            {
            return ((PartitionedService) serviceImpl).getBackupCount();
            }
        else if (serviceImpl instanceof CacheService)
            {
            return serviceImpl.getServiceMemberSet().size() - 1;
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "BackupCountAfterWritebehind"
    /**
     * Getter for property BackupCountAfterWritebehind.<p>
    * The number of members of the partitioned (distributed) cache service that
    * will retain backup data that does _not_ require write-behind, i.e. data
    * that is not vulnerable to being lost even if the entire cluster were shut
    * down.
     */
    public int getBackupCountAfterWritebehind()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache;
        
        PartitionedCache serviceDist = getPartitionedCache();
        
        return serviceDist == null
            ? -1
            : serviceDist.getBackupCountOpt();
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human readable description.
    * 
    * @see ModelAdapter#toString()
     */
    public String getDescription()
        {
        // import Component.Util.SafeService;
        
        SafeService serviceSafe = (SafeService) get_Service();
        return serviceSafe == null ? canonicalString(null) : serviceSafe.getServiceName();
        }
    
    // Accessor for the property "EventBacklog"
    /**
     * Getter for property EventBacklog.<p>
    * The size of the backlog queue that holds events scheduled to be processed
    * by the EventDispatcher thread.
     */
    public int getEventBacklog()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service$EventDispatcher as com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid         service    = get_ServiceImpl();
        com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher dispatcher = service == null ? null : service.getEventDispatcher();
        
        return dispatcher == null ? -1 : dispatcher.getQueue().size();
        }
    
    // Accessor for the property "EventCount"
    /**
     * Getter for property EventCount.<p>
    * The total number of processed events since the last time the statistics
    * were reset.
     */
    public long getEventCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service$EventDispatcher as com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid         service    = get_ServiceImpl();
        com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher dispatcher = service == null ? null : service.getEventDispatcher();
        
        return dispatcher == null ? -1L : dispatcher.getEventCount();
        }
    
    // Accessor for the property "EventInterceptorInfo"
    /**
     * Getter for property EventInterceptorInfo.<p>
    * Statistics for the UEM event dispatcher.
     */
    public String[] getEventInterceptorInfo()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache;
        // import com.tangosol.net.events.internal.ServiceDispatcher;
        
        PartitionedCache  serviceDist = getPartitionedCache();
        ServiceDispatcher dispatcher  = serviceDist == null
                ? null : serviceDist.getEventsHelper().getServiceDispatcher();
        
        return dispatcher == null
                ? new String[0]
                : dispatcher.getStats().toStringArray();
        }
    
    // Accessor for the property "IndexingTotalMillis"
    /**
     * Getter for property IndexingTotalMillis.<p>
    * The total amount of time taken to build indices for all storage instances
    * and partitions owned by this member. Building of indices may be performed
    * in parallel thus this value may be less than suming
    * StorageManagerMBean.IndexingTotalMillis.
     */
    public long getIndexingTotalMillis()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache;
        
        PartitionedCache serviceDist = getPartitionedCache();
        
        return serviceDist == null
            ? -1
            : serviceDist.getStatsIndexingTime().get();
        }
    
    // Accessor for the property "JoinTime"
    /**
     * Getter for property JoinTime.<p>
    * Return the date/time value (in cluster time) that this Member joined the
    * service.
     */
    public java.util.Date getJoinTime()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import java.util.Date;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        return serviceImpl != null
                ? new Date(serviceImpl.getServiceMemberSet().getServiceJoinTime(serviceImpl.getThisMember().getId()))
                : new Date(0);
        }
    
    // Accessor for the property "MemberCount"
    /**
     * Getter for property MemberCount.<p>
    * Specifies the total number of cluster nodes running this service.
     */
    public int getMemberCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid svc = get_ServiceImpl();
        
        return svc == null
            ? -1 // non-clustered
            : svc.getServiceMemberSet().size();
        }
    
    // Accessor for the property "MessagesLocal"
    /**
     * Getter for property MessagesLocal.<p>
    * The total number of messages which were self-addressed messages since the
    * last time the statistics were reset.  Such messages are used for
    * servicing process-local requests and do not have an associated network
    * cost.
     */
    public long getMessagesLocal()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid svc = get_ServiceImpl();
        return svc == null ? -1L : svc.getStatsSentLocal();
        }
    
    // Accessor for the property "MessagesReceived"
    /**
     * Getter for property MessagesReceived.<p>
    * The total number of messages received by this service since the last time
    * the statistics were reset. This value accounts for messages received by
    * any (local, dedicated or shared) transport.
     */
    public long getMessagesReceived()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid svc = get_ServiceImpl();
        return svc == null ? -1L : svc.getStatsReceived();
        }
    
    // Accessor for the property "MessagesSent"
    /**
     * Getter for property MessagesSent.<p>
    * The number of messages sent by this service since the last time the
    * statistics were reset. This value accounts for messages sent by any
    * (local, dedicated or shared) transport.
     */
    public long getMessagesSent()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid svc = get_ServiceImpl();
        return svc == null ? -1L : svc.getStatsSent();
        }
    
    // From interface: com.tangosol.util.HealthCheck
    public String getName()
        {
        return get_ServiceName();
        }
    
    // Accessor for the property "OutgoingTransferCount"
    /**
     * Getter for property OutgoingTransferCount.<p>
    * The number of partitions that are currently being transferred by this
    * service member to other members.
     */
    public int getOutgoingTransferCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        
        PartitionedService serviceDist = getPartitionedCache();
        
        return serviceDist == null || !serviceDist.isOwnershipEnabled()
                ? -1
                : serviceDist.getTransferControl().getTransferCount();
        }
    
    // Accessor for the property "OwnedPartitionsBackup"
    /**
     * Getter for property OwnedPartitionsBackup.<p>
    * The number of partitions that this Member backs up (responsible for the
    * backup storage).
     */
    public int getOwnedPartitionsBackup()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import com.tangosol.net.CacheService;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl instanceof CacheService)
            {
            if (serviceImpl instanceof PartitionedService)
                {
                PartitionedService serviceDist = (PartitionedService) serviceImpl;
                if (serviceDist.isOwnershipEnabled())
                    {
                    return serviceDist.calculateThisOwnership(false);
                    }
                else
                    {
                    return 0;
                    }
                }
            else
                {
                return 1;
                }
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "OwnedPartitionsPrimary"
    /**
     * Getter for property OwnedPartitionsPrimary.<p>
    * The number of partitions that this Member owns (responsible for the
    * primary storage).
     */
    public int getOwnedPartitionsPrimary()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import com.tangosol.net.CacheService;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl instanceof CacheService)
            {
            if (serviceImpl instanceof PartitionedService)
                {
                PartitionedService serviceDist = (PartitionedService) serviceImpl;
                if (serviceDist.isOwnershipEnabled())
                    {
                    return serviceDist.calculateThisOwnership(true);
                    }
                else
                    {
                    return 0;
                    }
                }
            else
                {
                return 1;
                }
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "PartitionedCache"
    /**
     * Getter for property PartitionedCache.<p>
    * Return the associated PartitionedCache or null.
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache getPartitionedCache()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl instanceof PartitionedCache)
            {
            return (PartitionedCache) serviceImpl;
            }
        return null;
        }
    
    // Accessor for the property "PartitionsAll"
    /**
     * Getter for property PartitionsAll.<p>
    * The total number of partitions that every cache storage will be divided
    * into.
     */
    public int getPartitionsAll()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import com.tangosol.net.CacheService;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl instanceof PartitionedService)
            {
            return ((PartitionedService) serviceImpl).getPartitionCount();
            }
        else
            {
            return serviceImpl instanceof CacheService ? 1 : -1;
            }
        }
    
    // Accessor for the property "PartitionsEndangered"
    /**
     * Getter for property PartitionsEndangered.<p>
    * The total number of partitions that are not currently backed up.
     */
    public int getPartitionsEndangered()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import com.tangosol.net.CacheService;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl instanceof CacheService)
            {
            if (serviceImpl instanceof PartitionedService)
                {
                PartitionedService serviceDist = (PartitionedService) serviceImpl;
        
                // without a backup the request is meaningless
                return serviceDist.getBackupCount() == 0 ? -1 : serviceDist.calculateEndangered();
                }
            else
                {
                return serviceImpl.getServiceMemberSet().size() > 1 ? 0 : 1;
                }
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "PartitionsUnbalanced"
    /**
     * Getter for property PartitionsUnbalanced.<p>
    * The total number of primary and backup partitions which remain to be
    * transferred until the partition distribution across the storage enabled
    * service members is fully balanced.
     */
    public int getPartitionsUnbalanced()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import com.tangosol.net.CacheService;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl instanceof CacheService)
            {
            if (serviceImpl instanceof PartitionedService)
                {
                PartitionedService serviceDist = (PartitionedService) serviceImpl;
        
                return serviceDist.calculateUnbalanced();
                }
            else
                {
                // replicated cache is always balanced
                return 0;
                }
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "PartitionsVulnerable"
    /**
     * Getter for property PartitionsVulnerable.<p>
    * The total number of partitions that are backed up on the same machine
    * where the primary partition owner resides.
     */
    public int getPartitionsVulnerable()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import com.tangosol.net.CacheService;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl instanceof CacheService)
            {
            if (serviceImpl instanceof PartitionedService)
                {
                PartitionedService serviceDist = (PartitionedService) serviceImpl;
        
                return serviceDist.getBackupCount() == 0 ? -1 : serviceDist.calculateVulnerable();
                }
            else
                {
                return serviceImpl.getServiceMemberSet().
                    getDistantMembers(serviceImpl.getThisMember()).size() > 0 ? 0 : 1;
                }
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "PersistenceActiveSpaceAvailable"
    /**
     * Getter for property PersistenceActiveSpaceAvailable.<p>
    * The total remaining free space (in bytes) of the file system used by the
    * persistence layer to persist active cache data.
     */
    public long getPersistenceActiveSpaceAvailable()
        {
        // import com.tangosol.persistence.PersistenceEnvironmentInfo;
        // import java.io.File;
        
        PersistenceEnvironmentInfo envInfo = getPersistenceEnvironmentInfo();
        if (envInfo != null)
            {
            File fileDir = envInfo.getPersistenceActiveDirectory();
            return fileDir == null ? -1L : fileDir.getUsableSpace();
            }
        
        return -1L;
        }
    
    // Accessor for the property "PersistenceActiveSpaceTotal"
    /**
     * Getter for property PersistenceActiveSpaceTotal.<p>
    * The total size (in bytes) of the file system used by the persistence
    * layer to persist active cache data.
     */
    public long getPersistenceActiveSpaceTotal()
        {
        // import com.tangosol.persistence.PersistenceEnvironmentInfo;
        // import java.io.File;
        
        PersistenceEnvironmentInfo envInfo = getPersistenceEnvironmentInfo();
        if (envInfo != null)
            {
            File fileDir = envInfo.getPersistenceActiveDirectory();
            return fileDir == null ? -1L : fileDir.getTotalSpace();
            }
        
        return -1L;
        }
    
    // Accessor for the property "PersistenceActiveSpaceUsed"
    /**
     * Getter for property PersistenceActiveSpaceUsed.<p>
    * The total size (in bytes) used by the persistence layer to persist active
    * cache data.
     */
    public long getPersistenceActiveSpaceUsed()
        {
        // import com.tangosol.persistence.PersistenceEnvironmentInfo;
        
        PersistenceEnvironmentInfo envInfo = getPersistenceEnvironmentInfo();
        if (envInfo != null && envInfo.getPersistenceActiveDirectory() != null)
            {
            return envInfo.getPersistenceActiveSpaceUsed();
            }
        
        return -1L;
        }
    
    // Accessor for the property "PersistenceBackupSpaceAvailable"
    /**
     * Getter for property PersistenceBackupSpaceAvailable.<p>
    * The total remaining free space (in bytes) of the file system used by the
    * persistence layer to persist backup cache data.
     */
    public long getPersistenceBackupSpaceAvailable()
        {
        // import com.tangosol.persistence.PersistenceEnvironmentInfo;
        // import java.io.File;
        
        PersistenceEnvironmentInfo envInfo = getPersistenceEnvironmentInfo();
        if (envInfo != null)
            {
            File fileDir = envInfo.getPersistenceBackupDirectory();
            return fileDir == null ? -1L : fileDir.getUsableSpace();
            }
        
        return -1L;
        }
    
    // Accessor for the property "PersistenceBackupSpaceTotal"
    /**
     * Getter for property PersistenceBackupSpaceTotal.<p>
    * The total size (in bytes) of the file system used by the persistence
    * layer to persist backup cache data.
     */
    public long getPersistenceBackupSpaceTotal()
        {
        // import com.tangosol.persistence.PersistenceEnvironmentInfo;
        // import java.io.File;
        
        PersistenceEnvironmentInfo envInfo = getPersistenceEnvironmentInfo();
        if (envInfo != null)
            {
            File fileDir = envInfo.getPersistenceBackupDirectory();
            return fileDir == null ? -1L : fileDir.getTotalSpace();
            }
        
        return -1L;
        }
    
    // Accessor for the property "PersistenceBackupSpaceUsed"
    /**
     * Getter for property PersistenceBackupSpaceUsed.<p>
    * The total size (in bytes) used by the persistence layer to persist backup
    * cache data.
     */
    public long getPersistenceBackupSpaceUsed()
        {
        // import com.tangosol.persistence.PersistenceEnvironmentInfo;
        
        PersistenceEnvironmentInfo envInfo = getPersistenceEnvironmentInfo();
        if (envInfo != null && envInfo.getPersistenceBackupDirectory() != null)
            {
            return envInfo.getPersistenceBackupSpaceUsed();
            }
        
        return -1L;
        }
    
    /**
     * Helper method to return the configured PersistenceControl object or null
    * if persistence is not configured.
     */
    protected com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PersistenceControl getPersistenceControl()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        
        PartitionedService serviceDist = getPartitionedCache();
        
        return serviceDist == null
                ? null
                : serviceDist.getPersistenceControl();
        }
    
    /**
     * Helper method to return the configured PersistenceEnvironment or null if
    * persistence is not configured.
     */
    protected com.oracle.coherence.persistence.PersistenceEnvironment getPersistenceEnv()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService$PersistenceControl as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PersistenceControl;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PersistenceControl ctrl = getPersistenceControl();
        return ctrl == null ? null : ctrl.getPersistenceEnvironment();
        }
    
    // Accessor for the property "PersistenceEnvironment"
    /**
     * Getter for property PersistenceEnvironment.<p>
    * A description of the configured persistence environment or 'n/a' if one
    * has not been configured.
     */
    public String getPersistenceEnvironment()
        {
        // import com.oracle.coherence.persistence.PersistenceEnvironment;
        
        PersistenceEnvironment env = getPersistenceEnv();
        
        return env == null ? canonicalString(null) : env.toString();
        }
    
    /**
     * Helper method to return the PersistenceEnvironmentInfo for the
    * PersistenceEnvironment, if available.
     */
    protected com.tangosol.persistence.PersistenceEnvironmentInfo getPersistenceEnvironmentInfo()
        {
        // import com.oracle.coherence.persistence.PersistenceEnvironment;
        // import com.tangosol.persistence.PersistenceEnvironmentInfo;
        // import com.tangosol.persistence.SafePersistenceWrappers;
        
        PersistenceEnvironment env = SafePersistenceWrappers.unwrap(getPersistenceEnv());
        
        // return the Info for this PersistenceEnvironment
        return env instanceof PersistenceEnvironmentInfo ? (PersistenceEnvironmentInfo) env : null;
        }
    
    // Accessor for the property "PersistenceLatencyAverage"
    /**
     * Getter for property PersistenceLatencyAverage.<p>
    * The average latency (in millis) added to a mutating cache operation by
    * active persistence operations.
     */
    public float getPersistenceLatencyAverage()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService$PersistenceControl as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PersistenceControl;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PersistenceControl ctrl = getPersistenceControl();
        if (ctrl != null)
            {
            long cStatsLatency = ctrl.getStatsLatencyCount();
            return cStatsLatency == 0 ? 0.0f :
                   (float) ctrl.getStatsLatencyTotal() / cStatsLatency;
            }
        
        return -1.0f;
        }
    
    // Accessor for the property "PersistenceLatencyMax"
    /**
     * Getter for property PersistenceLatencyMax.<p>
    * The maximum latency (in millis) added to a mutating cache operation by an
    * active persistence operation.
     */
    public long getPersistenceLatencyMax()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService$PersistenceControl as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PersistenceControl;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PersistenceControl ctrl = getPersistenceControl();
        
        return ctrl == null ? -1L : ctrl.getStatsLatencyMax();
        }
    
    // Accessor for the property "PersistenceMode"
    /**
     * Getter for property PersistenceMode.<p>
    * The current persistence mode for this service.  A value of 'active'
    * indicates that all mutating cache operations will be persisted via the
    * configured persistence-environment. A value of 'on-demand' indicates that
    * a persistence-environment has been configured and is available but is not
    * being actively used. In both modes a persistent snapshot can be taken of
    * all caches managed by this service using the configured
    * persistence-environment. A value of 'n/a' indicates that persistence is
    * not configured for this service.
     */
    public String getPersistenceMode()
        {
        // import com.tangosol.internal.net.service.grid.PersistenceDependencies;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService$PersistenceControl as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PersistenceControl;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PersistenceControl ctrl        = getPersistenceControl();
        PartitionedService serviceDist = getPartitionedCache();
        
        PersistenceDependencies depsPersist = serviceDist == null
                ? null : serviceDist.getPersistenceDependencies();
        
        String sMode = depsPersist == null
                ? "n/a" : depsPersist.getPersistenceMode();
        
        return ctrl != null && ctrl.isDisabled()
                ? "disabled" : sMode;
        }
    
    // Accessor for the property "PersistenceSnapshotArchiver"
    /**
     * Getter for property PersistenceSnapshotArchiver.<p>
    * A description of the configured snapshot archiver or 'n/a' if one has not
    * been configured.
     */
    public String getPersistenceSnapshotArchiver()
        {
        // import com.tangosol.persistence.SnapshotArchiver;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService$PersistenceControl as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PersistenceControl;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService.PersistenceControl ctrl = getPersistenceControl();
        if (ctrl == null)
            {
            return canonicalString(null);
            }
        else
            {
            SnapshotArchiver archiver = ctrl.getSnapshotArchiver();
            return archiver == null ? canonicalString(null) : archiver.toString();
            }
        }
    
    // Accessor for the property "PersistenceSnapshotSpaceAvailable"
    /**
     * Getter for property PersistenceSnapshotSpaceAvailable.<p>
    * The total remaining free space (in bytes) of the file system used by the
    * persistence layer to store snapshots.
     */
    public long getPersistenceSnapshotSpaceAvailable()
        {
        // import com.tangosol.persistence.PersistenceEnvironmentInfo;
        // import java.io.File;
        
        PersistenceEnvironmentInfo envInfo = getPersistenceEnvironmentInfo();
        if (envInfo != null)
            {
            File fileDir = envInfo.getPersistenceSnapshotDirectory();
            return fileDir == null ? -1L : fileDir.getUsableSpace();
            }
        
        return -1L;
        }
    
    // Accessor for the property "PersistenceSnapshotSpaceTotal"
    /**
     * Getter for property PersistenceSnapshotSpaceTotal.<p>
    * The total size (in bytes) of the file system used by the persistence
    * layer to store snapshots.
     */
    public long getPersistenceSnapshotSpaceTotal()
        {
        // import com.tangosol.persistence.PersistenceEnvironmentInfo;
        // import java.io.File;
        
        PersistenceEnvironmentInfo envInfo = getPersistenceEnvironmentInfo();
        if (envInfo != null)
            {
            File fileDir = envInfo.getPersistenceSnapshotDirectory();
            return fileDir == null ? -1L : fileDir.getTotalSpace();
            }
        
        return -1L;
        }
    
    // Accessor for the property "QuorumStatus"
    /**
     * Getter for property QuorumStatus.<p>
     */
    public String getQuorumStatus()
        {
        // import com.tangosol.net.ActionPolicy;
        // import com.tangosol.net.ConfigurableQuorumPolicy as com.tangosol.net.ConfigurableQuorumPolicy;
        // import com.tangosol.util.NullImplementation$NullActionPolicy as com.tangosol.util.NullImplementation.NullActionPolicy;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid      serviceImpl = get_ServiceImpl();
        ActionPolicy policy      = serviceImpl == null ? null : serviceImpl.getActionPolicy();
        String       sQuorum     = null;
        
        if (serviceImpl != null && serviceImpl.isSuspended())
            {
            sQuorum = "Suspended";
            }
        else if (policy instanceof com.tangosol.util.NullImplementation.NullActionPolicy)
            {
            // special-case the common scenario of no quorum-policy configured
            sQuorum = "Not configured";
            }
        else if (policy instanceof com.tangosol.net.ConfigurableQuorumPolicy)
            {
            sQuorum = ((com.tangosol.net.ConfigurableQuorumPolicy) policy).getStatusDescription();
            }
        else if (policy != null)
            {
            sQuorum = policy.toString();
            }
        
        return canonicalString(sQuorum);
        }
    
    // Accessor for the property "RequestAverageDuration"
    /**
     * Getter for property RequestAverageDuration.<p>
    * The average duration (in milliseconds) of an individual request issued by
    * the service since the last time the statistics were reset.
     */
    public float getRequestAverageDuration()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl != null)
            {
            long cMillisTotal = serviceImpl.getStatsPollDuration();
            long cRequests    = serviceImpl.getStatsPollCount();
        
            return cRequests == 0L ? 0.0f : (float) (((double) cMillisTotal)/ ((double) cRequests));
            }
        
        return -1f;
        }
    
    // Accessor for the property "RequestMaxDuration"
    /**
     * Getter for property RequestMaxDuration.<p>
    * The maximum duration (in milliseconds) of a request issued by the service
    * since the last time the statistics were reset.
     */
    public long getRequestMaxDuration()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl != null)
            {
            return serviceImpl.getStatsPollMaxDuration();
            }
        
        return -1L;
        }
    
    // Accessor for the property "RequestPendingCount"
    /**
     * Getter for property RequestPendingCount.<p>
    * The number of pending requests issued by the service.
     */
    public long getRequestPendingCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.WindowedArray;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl != null)
            {
            WindowedArray waPoll = serviceImpl.getPollArray();
            if (waPoll != null)
                {
                long cPolls = 0;
                try
                    {
                    for (long i = waPoll.getFirstIndex(), j = waPoll.getLastIndex(); i <= j; ++i)
                        {
                        if (waPoll.get(i) != null)
                            {
                            ++cPolls;
                            }
                        }
                    }
                catch (IndexOutOfBoundsException e)
                    {
                    }
                return cPolls;
                }
            }
        
        return -1L;
        }
    
    // Accessor for the property "RequestPendingDuration"
    /**
     * Getter for property RequestPendingDuration.<p>
    * The duration (in milliseconds) of the oldest pending request issued by
    * the service.
     */
    public long getRequestPendingDuration()
        {
        // import com.tangosol.util.Base;
        // import Component.Net.Poll;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.WindowedArray;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl != null)
            {
            WindowedArray waPoll = serviceImpl.getPollArray();
            if (waPoll != null)
                {
                final long cTimeMillis = Base.getSafeTimeMillis();
                long       cMaxMillis  = 0L;
                try
                    {
                    for (long i = waPoll.getFirstIndex(), j = waPoll.getLastIndex(); i <= j; ++i)
                        {
                        Poll poll = (Poll) waPoll.get(i);
                        if (poll != null)
                            {
                            long cMillis = cTimeMillis - poll.getInitTimeMillis();
                            if (cMillis > cMaxMillis)
                                {
                                cMaxMillis = cMillis;
                                }
                            }
                        }
                    }
                catch (IndexOutOfBoundsException e)
                    {
                    }
                return cMaxMillis;
                }
            }
        
        return -1L;
        }
    
    // Accessor for the property "RequestTimeoutCount"
    /**
     * Getter for property RequestTimeoutCount.<p>
    * The total number of timed-out requests since the last time the statistics
    * were reset.
     */
    public long getRequestTimeoutCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl != null)
            {
            return serviceImpl.getStatsTimeoutCount();
            }
        
        return -1L;
        }
    
    // Accessor for the property "RequestTimeoutMillis"
    /**
     * Getter for property RequestTimeoutMillis.<p>
    * The default timeout value in milliseconds for requests that can be
    * timed-out (e.g. implement the com.tangosol.net.PriorityTask interface),
    * but do not explicitly specify the request timeout value.
     */
    public long getRequestTimeoutMillis()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl != null)
            {
            return serviceImpl.getRequestTimeout();
            }
        
        return -1L;
        }
    
    // Accessor for the property "RequestTotalCount"
    /**
     * Getter for property RequestTotalCount.<p>
    * The number of Daemon threads used by this Service.
     */
    public long getRequestTotalCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl != null)
            {
            return serviceImpl.getStatsPollCount();
            }
        
        return -1L;
        }
    
    // Accessor for the property "SeniorMemberId"
    /**
     * Getter for property SeniorMemberId.<p>
    * The service senior member id; -1 if the service is not running.
     */
    public int getSeniorMemberId()
        {
        try
            {
            return isRunning()
                ? get_ServiceImpl().getServiceMemberSet().getOldestMember().getId()
                : -1;
            }
        catch (NullPointerException e)
            {
            return -1;
            }
        }
    
    // Accessor for the property "Statistics"
    /**
     * Getter for property Statistics.<p>
    * Returns statistics for this service.
     */
    public String getStatistics()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        return serviceImpl == null ? canonicalString(null) : serviceImpl.formatStats();
        }
    
    // Accessor for the property "StatusHA"
    /**
     * Getter for property StatusHA.<p>
    * The High Availability status for this service. The value of MACHINE-SAFE
    * means that all the cluster nodes running on any given machine could be
    * stoppped at once without data loss. The value of NODE-SAFE means that any
    * cluster node could be stoppped without data loss.  The value of
    * ENDANGERED indicates that abnormal termination of any cluster node that
    * runs this service may cause data loss. If the rack and site names are
    * configured, the values SITE-SAFE and RACK-SAFE mean that all the cluster
    * nodes running on any given site or rack, respectively, could be stoppped
    * without data loss.
     */
    public String getStatusHA()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import com.tangosol.net.CacheService;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl instanceof CacheService)
            {
            if (serviceImpl instanceof PartitionedService)
                {
                return canonicalString(((PartitionedService) serviceImpl).getBackupStrengthName());
                }
            else
                {
                int nEndangered = getPartitionsEndangered();
                if (nEndangered == -1)
                    {
                    return canonicalString(null);
                    }
                if (nEndangered > 0)
                    {
                    return "ENDANGERED";
                    }
        
                return getPartitionsVulnerable() == 0 ? "MACHINE-SAFE" : "NODE-SAFE";
                }
            }
        else
            {
            return canonicalString(null);
            }
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
     */
    public int getStatusHACode()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.partition.SimpleStrategyMBean$HAStatus as com.tangosol.net.partition.SimpleStrategyMBean.HAStatus;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl instanceof CacheService)
            {
            if (serviceImpl instanceof PartitionedService)
                {
                return ((PartitionedService) serviceImpl).getBackupStrength();
                }
            else
                {
                int nEndangered = getPartitionsEndangered();
                if (nEndangered == -1)
                    {
                    return -1;
                    }
                if (nEndangered > 0)
                    {
                    return com.tangosol.net.partition.SimpleStrategyMBean.HAStatus.ENDANGERED.getCode();
                    }
        
                return getPartitionsVulnerable() == 0
                           ? com.tangosol.net.partition.SimpleStrategyMBean.HAStatus.MACHINE_SAFE.getCode()
                           : com.tangosol.net.partition.SimpleStrategyMBean.HAStatus.NODE_SAFE.getCode();
                }
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "StorageEnabledCount"
    /**
     * Getter for property StorageEnabledCount.<p>
    * Specifies the total number of cluster nodes running this Service for
    * which local storage is enabled.
     */
    public int getStorageEnabledCount()
        {
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.DistributedCacheService;
        // import com.tangosol.net.Service;
        
        if (isRunning())
            {
            Service service = get_Service();
        
            if (service instanceof DistributedCacheService)
                {
                return ((DistributedCacheService) service).getStorageEnabledMembers().size();
                }
            if (service instanceof CacheService)
                {
                return service.getInfo().getServiceMembers().size();
                }
            }
        
        return -1; // n/a
        }
    
    // Accessor for the property "TaskAverageDuration"
    /**
     * Getter for property TaskAverageDuration.<p>
    * The number of Daemon threads used by this Service.
     */
    public float getTaskAverageDuration()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        if (pool != null && pool.isStarted())
            {
            long cPoolTotal = pool.getStatsActiveMillis();
            long cTasks     = pool.getStatsTaskCount();
        
            return cTasks == 0L ? 0.0f : (float) (((double) cPoolTotal)/ ((double) cTasks));
            }
        
        return -1f;
        }
    
    // Accessor for the property "TaskBacklog"
    /**
     * Getter for property TaskBacklog.<p>
    * The number of Daemon threads used by this Service.
     */
    public int getTaskBacklog()
        {
        // import Component.Util.DaemonPool;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        int cTask = 0;
        
        DaemonPool pool = get_DaemonPool();
        if (pool != null && pool.isStarted())
            {
            cTask += pool.getBacklog();
            }
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl != null)
            {
            cTask += serviceImpl.getQueue().size();
            }
        
        return cTask;
        }
    
    // Accessor for the property "TaskCount"
    /**
     * Getter for property TaskCount.<p>
    * The total number of executed tasks since the last time the statistics
    * were reset.
     */
    public long getTaskCount()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isStarted() ? pool.getStatsTaskCount() : -1;
        }
    
    // Accessor for the property "TaskHungCount"
    /**
     * Getter for property TaskHungCount.<p>
    * The total number of currently executing hung tasks.
     */
    public int getTaskHungCount()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isStarted() ? pool.getStatsHungCount() : -1;
        }
    
    // Accessor for the property "TaskHungDuration"
    /**
     * Getter for property TaskHungDuration.<p>
    * The longest currently executing hung task duration in milliseconds.
     */
    public long getTaskHungDuration()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isStarted() ? pool.getStatsHungDuration() : -1L;
        }
    
    // Accessor for the property "TaskHungTaskId"
    /**
     * Getter for property TaskHungTaskId.<p>
    * The id of the of the longest currently executing hung task.
     */
    public String getTaskHungTaskId()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return canonicalString(
            pool != null && pool.isStarted() ? pool.getStatsHungTaskId() : null);
        }
    
    // Accessor for the property "TaskHungThresholdMillis"
    /**
     * Getter for property TaskHungThresholdMillis.<p>
    * The amount of time in milliseconds that a task can execute before it is
    * considered hung. Note that a posted task that has not yet started is
    * never considered as hung.
     */
    public long getTaskHungThresholdMillis()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isStarted() ? pool.getHungThreshold() : -1L;
        }
    
    // Accessor for the property "TaskMaxBacklog"
    /**
     * Getter for property TaskMaxBacklog.<p>
    * The number of Daemon threads used by this Service.
     */
    public int getTaskMaxBacklog()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isStarted() ? pool.getStatsMaxBacklog() : -1;
        }
    
    // Accessor for the property "TaskTimeoutCount"
    /**
     * Getter for property TaskTimeoutCount.<p>
    * The total number of timed-out tasks since the last time the statistics
    * were reset.
     */
    public int getTaskTimeoutCount()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isStarted() ? pool.getStatsTimeoutCount() : 0;
        }
    
    // Accessor for the property "TaskTimeoutMillis"
    /**
     * Getter for property TaskTimeoutMillis.<p>
    * The default timeout value in milliseconds for tasks that can be timed-out
    * (e.g. implement the com.tangosol.net.PriorityTask interface), but do not
    * explicitly specify the execution timeout value.
     */
    public long getTaskTimeoutMillis()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isStarted() ? pool.getTaskTimeout() : 0L;
        }
    
    // Accessor for the property "ThreadAbandonedCount"
    /**
     * Getter for property ThreadAbandonedCount.<p>
    * The number of abandoned threads from the service thread pool. A thread is
    * abandoned and replaced with a new thread if it executes a task for a
    * period of time longer than execution timeout and all attempts to
    * interrupt it fail.
     */
    public int getThreadAbandonedCount()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isStarted() ? pool.getStatsAbandonedCount() : 0;
        }
    
    // Accessor for the property "ThreadAverageActiveCount"
    /**
     * Getter for property ThreadAverageActiveCount.<p>
    * The average number of active (not idle) threads in the service thread
    * pool since the last time the statistics were reset.
     */
    public float getThreadAverageActiveCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.DaemonPool;
        // import com.tangosol.util.Base;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl != null)
            {
            DaemonPool pool = get_DaemonPool();
            if (pool.isStarted())
                {
                long cTotal     = Base.getSafeTimeMillis() - serviceImpl.getStatsReset();
                long cPoolTotal = pool.getStatsActiveMillis();
        
                return cTotal == 0L ? 0.0f : (float) (((double) cPoolTotal)/ ((double) cTotal));
                }
            }
        
        return -1f;
        }
    
    // Accessor for the property "ThreadCount"
    /**
     * Getter for property ThreadCount.<p>
    * The number of threads in the service thread pool. For services that
    * support dynamic thread pool sizing, this is the current thread pool size.
     */
    public int getThreadCount()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isStarted() ? pool.getDaemonCount() : 0;
        }
    
    // Accessor for the property "ThreadCountMax"
    /**
     * Getter for property ThreadCountMax.<p>
    * The maximum thread count allowed for this service when dynamic thread
    * pool sizing is enabled.
     */
    public int getThreadCountMax()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isDynamic() ? pool.getDaemonCountMax() : -1;
        }
    
    // Accessor for the property "ThreadCountMin"
    /**
     * Getter for property ThreadCountMin.<p>
    * The minimum thread count for this service when dynamic thread pool sizing
    * is enabled.
     */
    public int getThreadCountMin()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isDynamic() ? pool.getDaemonCountMin() : -1;
        }
    
    // Accessor for the property "ThreadCountUpdateTime"
    /**
     * Getter for property ThreadCountUpdateTime.<p>
    * The last time, in java.util.Date form, that an update was made to the
    * ThreadCount.
     */
    public java.util.Date getThreadCountUpdateTime()
        {
        // import Component.Util.DaemonPool;
        // import java.util.Date;
        
        DaemonPool pool = get_DaemonPool();
        long       ldt  = pool == null ? 0L : pool.getStatsLastResizeMillis();
        return new Date(ldt);
        }
    
    // Accessor for the property "ThreadIdleCount"
    /**
     * Getter for property ThreadIdleCount.<p>
    * The number of currently idle threads in the service thread pool.
     */
    public int getThreadIdleCount()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isStarted() ?
            pool.getDaemonCount() - pool.getActiveDaemonCount() : -1;
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
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? canonicalString(null)
            : handler.getMessageBus().getLocalEndPoint().getCanonicalName();
        }
    
    // Accessor for the property "TransportBackloggedConnectionList"
    /**
     * Getter for property TransportBackloggedConnectionList.<p>
    * A list of currently backlogged connections on the service dedicated
    * transport.
     */
    public String[] getTransportBackloggedConnectionList()
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Net.MessageHandler;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import java.util.ArrayList;
        // import java.util.Iterator;
        // import java.util.List;
        
        List           listBacklog = new ArrayList();
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid        service     = get_ServiceImpl();
        MessageHandler handler     = get_MessageHandler();
        
        if (service != null && handler != null)
            {
            if (handler.isLocalBacklog())
                {
                listBacklog.add(service.getThisMember().toString());
                }
        
            boolean          fGlobal    = handler.isGlobalBacklog();
            ServiceMemberSet setMembers = service.getServiceMemberSet();
            for (Iterator iter = setMembers.iterator(); iter.hasNext(); )
                {
                Member member = (Member) iter.next();
                int    nId    = member.getId();
                if ((fGlobal && setMembers.getServiceEndPoint(nId) != null) ||
                     setMembers.isServiceBacklogged(nId))
                    {
                    listBacklog.add(member.toString());
                    }
                }
            }
        
        return (String[]) listBacklog.toArray(new String[listBacklog.size()]);
        }
    
    // Accessor for the property "TransportBackloggedConnections"
    /**
     * Getter for property TransportBackloggedConnections.<p>
    * The number of currently backlogged connections on the service dedicated
    * transport.  Any new requests which require the connection will block
    * until the backlog is cleared.
     */
    public int getTransportBackloggedConnections()
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Net.MessageHandler;
        
        MessageHandler handler  = get_MessageHandler();
        if (handler == null)
            {
            return -1;
            }
        
        if (handler.isGlobalBacklog())
            {
            return getTransportConnections();
            }
        
        int cBacklog = 0;
        if (handler.isLocalBacklog())
            {
            ++cBacklog;
            }
        
        ServiceMemberSet setMembers = get_ServiceImpl().getServiceMemberSet();
        int[]            anMember   = setMembers.toIdArray();
        for (int i = 0, c = anMember.length; i < c; ++i)
            {
            if (setMembers.isServiceBacklogged(anMember[i]))
                {
                ++cBacklog;
                }
            }
        
        return cBacklog;
        }
    
    // Accessor for the property "TransportConnections"
    /**
     * Getter for property TransportConnections.<p>
    * The number of connections currently maintained by the service dedicated
    * transport.  This count may be lower than MemberCount if some members have
    * been not been configured to use the dedicated transport, or if it has
    * been identified that there is no advantage in using the dedicated
    * transport for communication with certain members.
     */
    public int getTransportConnections()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1
            : handler.getConnectionMap().size();
        }
    
    // Accessor for the property "TransportReceivedBytes"
    /**
     * Getter for property TransportReceivedBytes.<p>
    * The number of bytes received by the service dedicated transport since the
    * last time the statistics were reset.
     */
    public long getTransportReceivedBytes()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1L
            : handler.getStatsBusBytesIn();
        }
    
    // Accessor for the property "TransportReceivedMessages"
    /**
     * Getter for property TransportReceivedMessages.<p>
    * The number of messages received by the service dedicated transport since
    * the last time the statistics were reset.
     */
    public long getTransportReceivedMessages()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1L
            : handler.getStatsBusReceives();
        }
    
    // Accessor for the property "TransportRetainedBytes"
    /**
     * Getter for property TransportRetainedBytes.<p>
    * The number of bytes retained by the service dedicated transport awaiting
    * delivery acknowledgment.  This memory is allocated outside of the Java GC
    * heap space.
     */
    public long getTransportRetainedBytes()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1L
            : handler.getStatsBusBytesOutBuffered().get();
        }
    
    // Accessor for the property "TransportSentBytes"
    /**
     * Getter for property TransportSentBytes.<p>
    * The number of bytes sent by the service dedicated transport since the
    * last time the statistics were reset.
     */
    public long getTransportSentBytes()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1L
            : handler.getStatsBusBytesOut();
        }
    
    // Accessor for the property "TransportSentMessages"
    /**
     * Getter for property TransportSentMessages.<p>
    * The number of messages sent by the service dedicated transport since the
    * last time the statistics were reset.
     */
    public long getTransportSentMessages()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1L
            : handler.getStatsBusSends();
        }
    
    // Accessor for the property "TransportStatus"
    /**
     * Getter for property TransportStatus.<p>
    * The service dedicated transport status information. 
     */
    public String getTransportStatus()
        {
        return getTransportStatus(false);
        }
    
    // Accessor for the property "TransportStatus"
    /**
     * Getter for property TransportStatus.<p>
    * The service dedicated transport status information. 
     */
    public String getTransportStatus(boolean fVerbose)
        {
        // import com.tangosol.internal.util.MessagePublisher;
        // import com.oracle.coherence.common.internal.net.socketbus.AbstractSocketBus;
        // import com.oracle.coherence.common.net.exabus.MessageBus;
        // import Component.Net.MessageHandler;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid;
        // import Component.Net.Member;
        // import java.net.InetAddress;
        
        MessageHandler handler = get_MessageHandler();
        if (handler != null) {
            MessageBus messageBus = handler.getMessageBus();
            return messageBus instanceof AbstractSocketBus
                   ? ((AbstractSocketBus) messageBus).toString(fVerbose)
                   : messageBus.toString();
        }
        
        Grid service = get_ServiceImpl();
        if (service == null) {
            return canonicalString(null);
        }
        
        MessagePublisher mp = service.getMessagePublisher();
        if (mp instanceof MessageHandler) {
            MessageBus messageBus = ((MessageHandler) mp).getMessageBus();
            return messageBus instanceof AbstractSocketBus
                   ? ((AbstractSocketBus) messageBus).toString(fVerbose)
                   : messageBus.toString();
        }
        
        Member member = service.getThisMember();
        if (member == null) {
            return canonicalString("Datagram");
        }
        
        StringBuilder sb = new StringBuilder("Datagram(");
        
        InetAddress address = member.getAddress();
        if (address != null) {
            sb.append('/').append(address).append(':').append(member.getPort());
        }
        
        String sStats = getStatistics();
        if (!"n/a".equals(sStats)) {
            if (sb.length() > 9) {
              sb.append(", ");
            }
            sb.append(sStats);
        }
        
        sb.append(')');
        return sb.toString();
        }
    
    // Accessor for the property "Type"
    /**
     * Getter for property Type.<p>
    * The type identifier of the service.
     */
    public String getType()
        {
        String sType = null;
        try
            {
            sType = get_ServiceImpl().getInfo().getServiceType();
            }
        catch (Exception e)
            {
            }
        return canonicalString(sType);
        }
    
    // Accessor for the property "HealthyReady"
    /**
     * Getter for property HealthyReady.<p>
     */
    public boolean isHealthyReady()
        {
        return __m_HealthyReady;
        }
    
    // From interface: com.tangosol.util.HealthCheck
    public boolean isLive()
        {
        return isRunning();
        }
    
    // From interface: com.tangosol.util.HealthCheck
    public boolean isMemberHealthCheck()
        {
        // import com.tangosol.internal.health.HealthCheckDependencies;
        // import com.tangosol.net.Service;
        // import com.tangosol.net.ServiceDependencies;
        
        Service             service       = get_Service();
        ServiceDependencies serviceDeps   = service.getDependencies();
        boolean             isHealthCheck = true;
        if (serviceDeps != null)
            {
            HealthCheckDependencies deps = serviceDeps.getHealthCheckDependencies();
            isHealthCheck = deps == null || deps.isMemberHealthCheck();
            }
        
        return isHealthCheck;
        }
    
    // From interface: com.tangosol.util.HealthCheck
    public boolean isReady()
        {
        boolean fReady = isHealthyReady();
        
        if (fReady)
            {
            // Once the service has been ready previously it is always ready
            // unless it is not running
            return true;
            }

        // to get here, either the service has never been ready or has restarted
        Service service = get_Service();
        if (service instanceof com.tangosol.net.ProxyService)
            {
            // this is a Proxy service
            if (service instanceof SafeProxyService)
                {
                service = ((SafeProxyService) service).getService();
                }
            ProxyService       proxyService = (ProxyService) service;
            ConnectionAcceptor acceptor     = proxyService.getAcceptor();
            fReady = proxyService.isStarted() && acceptor != null && acceptor.isRunning();
            }
        else
            {
            // likely a cache service, the first ready check is the same as the safe (HA) check
            fReady = isSafe();
            }

        setHealthyReady(fReady);
        return fReady;
        }
    
    // Accessor for the property "Running"
    /**
     * Getter for property Running.<p>
    * Determines whether or not the service is running.
     */
    public boolean isRunning()
        {
        // import com.tangosol.net.Service;
        
        Service service = get_Service();
        return service != null && service.isRunning();
        }
    
    // From interface: com.tangosol.util.HealthCheck
    public boolean isSafe()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache;
        // import com.oracle.coherence.common.base.Logger;
        // import com.tangosol.net.Member;
        // import java.util.Arrays;
        // import java.util.Set;
        // import java.util.stream.Collectors;
        
        PartitionedCache partitionedCache = getPartitionedCache();
        
        if (partitionedCache == null || !partitionedCache.isOwnershipEnabled())
            {
            return true;
            }
        
        if (!isRunning())
            {
            return false;
            }
        
        Set setOwnershipEnabledMembers = partitionedCache.getOwnershipEnabledMembers();
        int cMember                    = setOwnershipEnabledMembers.size();
        
        if (cMember == 1)
            {
            // Storage enabled and only one member, check we own all partitions
            // If a member just died and this is th eonly member left then the
            // partitions may still be being recovered
        
            int cPart  = partitionedCache.getPartitionCount();
            int cOwned = partitionedCache.calculateThisOwnership(true);
        
            if (cOwned != cPart)
                {
                Logger.finest("Health: StatusHA check failed for service " + get_ServiceName()
                                 + ". This member is the only storage enabled member, "
                                 + "but owns only " + cOwned + " of " + cPart + " partitions.");
        
                return false;
                }
            }
        
        // We create a string description of the member set here to use in log messages
        String[] asMemberIds = new String[cMember];
        int      nMember     = 0;    
        Iterator it          = setOwnershipEnabledMembers.iterator();
        
        while (it.hasNext())
            {
            Member member = (Member) it.next();
            asMemberIds[nMember++] = String.valueOf(member.getId());    
            }
        
        String sMembers = "memberCount=" + cMember + " members=" + Arrays.toString(asMemberIds);
        
        String                  sStatusHA        = partitionedCache.getBackupStrengthName();
        int                     cBackup          = partitionedCache.getBackupCount();
        String                  sName            = get_ServiceName();
        HealthCheckDependencies deps             = partitionedCache.getDependencies().getHealthCheckDependencies();
        boolean                 fAllowEndangered = deps != null && deps.allowEndangered();
        
        if (cMember > 1 && cBackup > 0 && "ENDANGERED".equals(sStatusHA) && !fAllowEndangered)
            {
            Logger.fine("Health: StatusHA check failed. Service " + sName + " has HA status of " + sStatusHA
                    + ", suspended=" + partitionedCache.isSuspended() + ", members=" + sMembers);
        
            return false;
            }
        
        if (partitionedCache.isDistributionInProgress())
            {
            Logger.fine("Health: StatusHA check failed. Service " + sName + " distribution in progress, members=" + sMembers);
            return false;
            }
        
        if (partitionedCache.isRecoveryInProgress())
            {
            Logger.fine("Health: StatusHA check failed. Service " + sName + " recovery in progress, members=" + sMembers);
            return false;
            }
        
        if (partitionedCache.isRestoreInProgress())
            {
            Logger.fine("Health: StatusHA check failed. Service " + sName + " restore in progress, members=" + sMembers);
            return false;
            }
        
        if (partitionedCache.isTransferInProgress())
            {
            Logger.fine("Health: StatusHA check failed. Service " + sName + " transfer in progress, members=" + sMembers);
            return false;
            }
        
        return true;
        }
    
    // From interface: com.tangosol.util.HealthCheck
    public boolean isStarted()
        {
        return isRunning();
        }
    
    // Accessor for the property "StorageEnabled"
    /**
     * Getter for property StorageEnabled.<p>
    * Specifies whether or not the local storage is enabled for this cluster
    * Member.
     */
    public boolean isStorageEnabled()
        {
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.DistributedCacheService;
        // import com.tangosol.net.Service;
        
        Service service = get_Service();
        return isRunning() &&
               (service instanceof DistributedCacheService
                ? ((DistributedCacheService) service).isLocalStorageEnabled()
                : service instanceof CacheService);
        }
    
    // Accessor for the property "ThreadPoolSizingEnabled"
    /**
     * Getter for property ThreadPoolSizingEnabled.<p>
    * Whether or not dynamic thread pool sizing is enabled for this service.
     */
    public boolean isThreadPoolSizingEnabled()
        {
        // import Component.Util.DaemonPool;
        
        DaemonPool pool = get_DaemonPool();
        return pool != null && pool.isDynamic();
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Date;
        // import java.util.Map;
        
        super.readExternal(in);
        
        Map mapSnapshot = get_SnapshotMap();
        
        mapSnapshot.put("BackupCount", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("BackupCountAfterWritebehind", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("EventBacklog", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("EventCount", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("EventInterceptorInfo", ExternalizableHelper.readStringArray(in));
        mapSnapshot.put("JoinTime", new Date(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("MemberCount", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("MessagesLocal", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("MessagesReceived", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("MessagesSent", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("OutgoingTransferCount", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("OwnedPartitionsBackup", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("OwnedPartitionsPrimary", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("PartitionsAll", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("PartitionsEndangered", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("PartitionsUnbalanced", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("PartitionsVulnerable", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("QuorumStatus", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("RequestAverageDuration", Float.valueOf(in.readFloat()));
        mapSnapshot.put("RequestMaxDuration", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("RequestPendingCount", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("RequestPendingDuration", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("RequestTimeoutCount", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("RequestTimeoutMillis", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("RequestTotalCount", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("Running", in.readBoolean() ? Boolean.TRUE : Boolean.FALSE);
        mapSnapshot.put("SeniorMemberId", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("Statistics", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("StatusHA", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("StorageEnabled", in.readBoolean() ? Boolean.TRUE : Boolean.FALSE);
        mapSnapshot.put("StorageEnabledCount", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("TaskAverageDuration", Float.valueOf(in.readFloat()));
        mapSnapshot.put("TaskBacklog", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("TaskCount", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TaskHungCount", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("TaskHungDuration", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TaskHungTaskId", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("TaskHungThresholdMillis", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TaskMaxBacklog", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("TaskTimeoutCount", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("TaskTimeoutMillis", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("ThreadAbandonedCount", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("ThreadAverageActiveCount", Float.valueOf(in.readFloat()));
        mapSnapshot.put("ThreadCount", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("ThreadCountMax", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("ThreadCountMin", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("ThreadCountUpdateTime", new Date(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("ThreadIdleCount", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("ThreadPoolSizingEnabled", in.readBoolean() ? Boolean.TRUE : Boolean.FALSE);
        mapSnapshot.put("TransportAddress", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("TransportBackloggedConnectionList", (String[]) ExternalizableHelper.readObject(in));
        mapSnapshot.put("TransportBackloggedConnections", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("TransportConnections", Integer.valueOf(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("TransportReceivedBytes", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TransportReceivedMessages", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TransportRetainedBytes", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TransportSentBytes", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TransportSentMessages", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TransportStatus", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("Type", ExternalizableHelper.readSafeUTF(in));
        
        // Persistence statistics
        mapSnapshot.put("PersistenceEnvironment", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("PersistenceLatencyAverage", Float.valueOf(in.readFloat()));
        mapSnapshot.put("PersistenceLatencyMax", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PersistenceMode", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("PersistenceActiveSpaceAvailable", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PersistenceSnapshotSpaceAvailable", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PersistenceActiveSpaceTotal", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PersistenceSnapshotSpaceTotal", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PersistenceActiveSpaceUsed", Long.valueOf(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PersistenceSnapshotArchiver", ExternalizableHelper.readSafeUTF(in));
        
        if (ExternalizableHelper.isVersionCompatible(in, 21, 6, 0))
            {
            mapSnapshot.put("IndexingTotalMillis", Long.valueOf(ExternalizableHelper.readLong(in)));
            }
        
        if (ExternalizableHelper.isVersionCompatible(in, 22, 6, 0))
            {
            mapSnapshot.put("PersistenceBackupSpaceAvailable", Long.valueOf(ExternalizableHelper.readLong(in)));
            mapSnapshot.put("PersistenceBackupSpaceTotal", Long.valueOf(ExternalizableHelper.readLong(in)));
            mapSnapshot.put("PersistenceBackupSpaceUsed", Long.valueOf(ExternalizableHelper.readLong(in)));
            }
        }

    /**
     * Get service description.
     */
    public String getServiceDescription()
        {
        Grid svc = get_ServiceImpl();
        if (svc == null)
            {
            return canonicalString(null);
            }
        ServiceMemberSet setMember = svc.getServiceMemberSet();
        return svc + (setMember == null ? canonicalString(null) : setMember.getDescription());
        }
    
    /**
     * Report partition distributions for which this service member is either
    * the sender or receiver, and which are still in-progress since the last
    * partition assignment analysis. Verbose mode includes partition numbers
    * for all pending or scheduled partitions in the report.
     */
    public String reportDistributionState(boolean fVerbose)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        
        PartitionedService serviceDist = getPartitionedCache();
        
        return serviceDist == null || !serviceDist.isOwnershipEnabled()
                ? canonicalString(null)
                : serviceDist.getDistributionStrategy().reportLocalDistributionState(fVerbose);
        }
    
    public String reportOwnership(boolean fVerbose)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import com.tangosol.util.Base;
        
        PartitionedService serviceDist = getPartitionedCache();
        
        return serviceDist == null
                ? canonicalString(null)
                : Base.replace(serviceDist.reportOwnership(fVerbose), "\n", "<br/>");
        }
    
    public void resetStatistics()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl != null)
            {
            serviceImpl.resetStats();
            }
        }
    
    // Accessor for the property "_Service"
    /**
     * Setter for property _Service.<p>
    * Service object associated with this model.
     */
    public void set_Service(com.tangosol.net.Service service)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid;
        // import Component.Util.SafeService;
        // import com.tangosol.net.Service;
        // import java.lang.ref.WeakReference;
        
        set_ServiceRef(new WeakReference(service));
        set_ServiceName(service.getInfo().getServiceName());
        if (service instanceof SafeService)
            {
            Service impl = ((SafeService) service).getService();
            if (impl instanceof Grid)
                {
                set_ServiceImplRef(new WeakReference(impl));
                }
            }
        }
    
    // Accessor for the property "_ServiceImplRef"
    /**
     * Setter for property _ServiceImplRef.<p>
    * Underlying "real" Service object wrapped in WeakReference to avoid
    * resource leakage.
     */
    protected void set_ServiceImplRef(java.lang.ref.WeakReference refServiceImpl)
        {
        __m__ServiceImplRef = refServiceImpl;
        }
    
    // Accessor for the property "_ServiceName"
    /**
     * Setter for property _ServiceName.<p>
    * Name of the service. We need to store it to be able to restart the
    * service from this model.
     */
    public void set_ServiceName(String sName)
        {
        __m__ServiceName = sName;
        }
    
    // Accessor for the property "_ServiceRef"
    /**
     * Setter for property _ServiceRef.<p>
    * Service object associated with this model wrapped in WeakReference to
    * avoid resource leakage.
     */
    protected void set_ServiceRef(java.lang.ref.WeakReference refService)
        {
        __m__ServiceRef = refService;
        }
    
    // Accessor for the property "HealthyReady"
    /**
     * Setter for property HealthyReady.<p>
     */
    public void setHealthyReady(boolean fReady)
        {
        __m_HealthyReady = fReady;
        }
    
    // Accessor for the property "RequestTimeoutMillis"
    /**
     * Setter for property RequestTimeoutMillis.<p>
    * The default timeout value in milliseconds for requests that can be
    * timed-out (e.g. implement the com.tangosol.net.PriorityTask interface),
    * but do not explicitly specify the request timeout value.
     */
    public void setRequestTimeoutMillis(long cMillis)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        checkReadOnly("setRequestTimeoutMillis");
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceImpl = get_ServiceImpl();
        if (serviceImpl != null)
            {
            serviceImpl.setRequestTimeout(Math.max(0L, cMillis));
            }
        }
    
    // Accessor for the property "TaskHungThresholdMillis"
    /**
     * Setter for property TaskHungThresholdMillis.<p>
    * The amount of time in milliseconds that a task can execute before it is
    * considered hung. Note that a posted task that has not yet started is
    * never considered as hung.
     */
    public void setTaskHungThresholdMillis(long cMillis)
        {
        // import Component.Util.DaemonPool;
        
        checkReadOnly("setTaskHungThresholdMillis");
        
        DaemonPool pool = get_DaemonPool();
        if (pool != null && pool.isStarted())
            {
            checkReadOnly("setTaskHungThresholdMillis");
            pool.setHungThreshold(cMillis);
            }
        else if (cMillis > 0)
            {
            throw new IllegalArgumentException(
                "ThreadPool is not configured");
            }
        }
    
    // Accessor for the property "TaskTimeoutMillis"
    /**
     * Setter for property TaskTimeoutMillis.<p>
    * The default timeout value in milliseconds for tasks that can be timed-out
    * (e.g. implement the com.tangosol.net.PriorityTask interface), but do not
    * explicitly specify the execution timeout value.
     */
    public void setTaskTimeoutMillis(long cMillis)
        {
        // import Component.Util.DaemonPool;
        
        checkReadOnly("setTaskTimeoutMillis");
        
        DaemonPool pool = get_DaemonPool();
        if (pool != null && pool.isStarted())
            {
            checkReadOnly("setTaskTimeoutMillis");
            pool.setTaskTimeout(cMillis);
            }
        else if (cMillis > 0)
            {
            throw new IllegalArgumentException(
                "ThreadPool is not configured");
            }
        }
    
    // Accessor for the property "ThreadCount"
    /**
     * Setter for property ThreadCount.<p>
    * The number of threads in the service thread pool. For services that
    * support dynamic thread pool sizing, this is the current thread pool size.
     */
    public void setThreadCount(int cThreads)
        {
        // import Component.Util.DaemonPool;
        
        checkReadOnly("setThreadCount");
        
        DaemonPool pool = get_DaemonPool();
        if (pool != null && pool.isStarted())
            {
            checkRange("setThreadCount", cThreads, 1, Integer.MAX_VALUE);
            pool.setDaemonCount(cThreads);
            }
        else if (cThreads > 0)
            {
            throw new IllegalArgumentException(
                "ThreadPool cannot be started dynamically");
            }
        }
    
    // Accessor for the property "ThreadCountMax"
    /**
     * Setter for property ThreadCountMax.<p>
    * The maximum thread count allowed for this service when dynamic thread
    * pool sizing is enabled.
     */
    public void setThreadCountMax(int nMax)
        {
        // import Component.Util.DaemonPool;
        
        checkReadOnly("setThreadCountMax");
        
        DaemonPool pool = get_DaemonPool();
        if (pool != null && pool.isDynamic())
            {
            checkRange("setThreadCountMax", nMax, getThreadCountMin() + 1, Integer.MAX_VALUE);
            pool.setDaemonCountMax(nMax);
            }
        else if (nMax > 0)
            {
            throw new IllegalArgumentException(
                "Dynamic thread pool sizing is not available or enabled");
            }
        }
    
    // Accessor for the property "ThreadCountMin"
    /**
     * Setter for property ThreadCountMin.<p>
    * The minimum thread count for this service when dynamic thread pool sizing
    * is enabled.
     */
    public void setThreadCountMin(int nMin)
        {
        // import Component.Util.DaemonPool;
        
        checkReadOnly("setThreadCountMin");
        
        DaemonPool pool = get_DaemonPool();
        if (pool != null && pool.isDynamic())
            {
            checkRange("setThreadCountMin", nMin, 1, getThreadCountMax() - 1);
            pool.setDaemonCountMin(nMin);
            }
        else if (nMin > 0)
            {
            throw new IllegalArgumentException(
                "Dynamic thread pool sizing is not available or enabled");
            }
        }
    
    /**
     * Show detailed information on the reliable transport.
     */
    public String showTransportStatus()
        {
        return getTransportStatus(true);
        }
    
    public void shutdown()
        {
        // import com.tangosol.net.Service;
        
        checkReadOnly("shutdown");
        
        Service service = get_Service();
        if (service != null)
            {
            service.shutdown();
            }
        }
    
    public void start()
        {
        // import com.tangosol.net.CacheFactory;
        
        checkReadOnly("start");
        CacheFactory.getConfigurableCacheFactory().ensureService(get_ServiceName());
        }
    
    public void stop()
        {
        // import com.tangosol.net.Service;
        
        checkReadOnly("stop");
        
        Service service = get_Service();
        if (service != null)
            {
            service.stop();
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
        
        ExternalizableHelper.writeInt(out, getBackupCount());
        ExternalizableHelper.writeInt(out, getBackupCountAfterWritebehind());
        ExternalizableHelper.writeInt(out, getEventBacklog());
        ExternalizableHelper.writeLong(out, getEventCount());
        ExternalizableHelper.writeStringArray(out, getEventInterceptorInfo());
        ExternalizableHelper.writeLong(out, getJoinTime().getTime());
        ExternalizableHelper.writeInt(out, getMemberCount());
        ExternalizableHelper.writeLong(out, getMessagesLocal());
        ExternalizableHelper.writeLong(out, getMessagesReceived());
        ExternalizableHelper.writeLong(out, getMessagesSent());
        ExternalizableHelper.writeInt(out, getOutgoingTransferCount());
        ExternalizableHelper.writeInt(out, getOwnedPartitionsBackup());
        ExternalizableHelper.writeInt(out, getOwnedPartitionsPrimary());
        ExternalizableHelper.writeInt(out, getPartitionsAll());
        ExternalizableHelper.writeInt(out, getPartitionsEndangered());
        ExternalizableHelper.writeInt(out, getPartitionsUnbalanced());
        ExternalizableHelper.writeInt(out, getPartitionsVulnerable());
        ExternalizableHelper.writeSafeUTF(out, getQuorumStatus());
        out.writeFloat(getRequestAverageDuration());
        ExternalizableHelper.writeLong(out, getRequestMaxDuration());
        ExternalizableHelper.writeLong(out, getRequestPendingCount());
        ExternalizableHelper.writeLong(out, getRequestPendingDuration());
        ExternalizableHelper.writeLong(out, getRequestTimeoutCount());
        ExternalizableHelper.writeLong(out, getRequestTimeoutMillis());
        ExternalizableHelper.writeLong(out, getRequestTotalCount());
        out.writeBoolean(isRunning());
        ExternalizableHelper.writeInt(out, getSeniorMemberId());
        ExternalizableHelper.writeSafeUTF(out, getStatistics());
        ExternalizableHelper.writeSafeUTF(out, getStatusHA());
        out.writeBoolean(isStorageEnabled());
        ExternalizableHelper.writeInt(out, getStorageEnabledCount());
        out.writeFloat(getTaskAverageDuration());
        ExternalizableHelper.writeInt(out, getTaskBacklog());
        ExternalizableHelper.writeLong(out, getTaskCount());
        ExternalizableHelper.writeInt(out, getTaskHungCount());
        ExternalizableHelper.writeLong(out, getTaskHungDuration());
        ExternalizableHelper.writeSafeUTF(out, getTaskHungTaskId());
        ExternalizableHelper.writeLong(out, getTaskHungThresholdMillis());
        ExternalizableHelper.writeInt(out, getTaskMaxBacklog());
        ExternalizableHelper.writeInt(out, getTaskTimeoutCount());
        ExternalizableHelper.writeLong(out, getTaskTimeoutMillis());
        ExternalizableHelper.writeInt(out, getThreadAbandonedCount());
        out.writeFloat(getThreadAverageActiveCount());
        ExternalizableHelper.writeInt(out, getThreadCount());
        ExternalizableHelper.writeInt(out, getThreadCountMax());
        ExternalizableHelper.writeInt(out, getThreadCountMin());
        ExternalizableHelper.writeLong(out, getThreadCountUpdateTime().getTime());
        ExternalizableHelper.writeInt(out, getThreadIdleCount());
        out.writeBoolean(isThreadPoolSizingEnabled());
        ExternalizableHelper.writeSafeUTF(out, getTransportAddress());
        ExternalizableHelper.writeObject(out, getTransportBackloggedConnectionList());
        ExternalizableHelper.writeInt(out, getTransportBackloggedConnections());
        ExternalizableHelper.writeInt(out, getTransportConnections());
        ExternalizableHelper.writeLong(out, getTransportReceivedBytes());
        ExternalizableHelper.writeLong(out, getTransportReceivedMessages());
        ExternalizableHelper.writeLong(out, getTransportRetainedBytes());
        ExternalizableHelper.writeLong(out, getTransportSentBytes());
        ExternalizableHelper.writeLong(out, getTransportSentMessages());
        ExternalizableHelper.writeSafeUTF(out, getTransportStatus());
        ExternalizableHelper.writeSafeUTF(out, getType());
        
        // Persistence statistics
        ExternalizableHelper.writeSafeUTF(out, getPersistenceEnvironment());
        out.writeFloat(getPersistenceLatencyAverage());
        ExternalizableHelper.writeLong(out, getPersistenceLatencyMax());
        ExternalizableHelper.writeSafeUTF(out, getPersistenceMode());
        ExternalizableHelper.writeLong(out, getPersistenceActiveSpaceAvailable());
        ExternalizableHelper.writeLong(out, getPersistenceSnapshotSpaceAvailable());
        ExternalizableHelper.writeLong(out, getPersistenceActiveSpaceTotal());
        ExternalizableHelper.writeLong(out, getPersistenceSnapshotSpaceTotal());
        ExternalizableHelper.writeLong(out, getPersistenceActiveSpaceUsed());
        ExternalizableHelper.writeSafeUTF(out, getPersistenceSnapshotArchiver());
        
        // added in 14.1.2.0.0 / 21.06.0
        if (ExternalizableHelper.isVersionCompatible(out, 21, 6, 0))
            {
            ExternalizableHelper.writeLong(out, getIndexingTotalMillis());
            }
        
            // added in 14.1.2.0.0 / 22.06.0
        if (ExternalizableHelper.isVersionCompatible(out, 22, 6, 0))
            {
            ExternalizableHelper.writeLong(out, getPersistenceBackupSpaceAvailable());
            ExternalizableHelper.writeLong(out, getPersistenceBackupSpaceTotal());
            ExternalizableHelper.writeLong(out, getPersistenceBackupSpaceUsed());
            }
        }
    }
