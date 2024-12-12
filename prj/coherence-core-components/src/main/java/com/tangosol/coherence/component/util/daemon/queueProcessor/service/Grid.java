
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid

package com.tangosol.coherence.component.util.daemon.queueProcessor.service;

import com.oracle.coherence.common.collections.NullableSortedMap;
import com.tangosol.coherence.component.net.Cluster;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.MessageHandler;
import com.tangosol.coherence.component.net.Packet;
import com.tangosol.coherence.component.net.Poll;
import com.tangosol.coherence.component.net.RequestContext;
import com.tangosol.coherence.component.net.Security;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
import com.tangosol.coherence.component.net.message.DiscoveryMessage;
import com.tangosol.coherence.component.net.message.RequestMessage;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.base.MutableLong;
import com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier;
import com.oracle.coherence.common.io.BufferManagers;
import com.oracle.coherence.common.io.BufferSequence;
import com.oracle.coherence.common.io.Buffers;
import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.MessageBus;
import com.oracle.coherence.common.util.Duration;
import com.tangosol.application.ContainerHelper;
import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ServiceFailurePolicyBuilder;
import com.tangosol.config.expression.Parameter;
import com.tangosol.internal.net.service.grid.DefaultGridDependencies;
import com.tangosol.internal.net.service.grid.GridDependencies;
import com.tangosol.internal.tracing.Scope;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.SpanContext;
import com.tangosol.internal.tracing.TracingHelper;
import com.tangosol.internal.util.MessagePublisher;
import com.tangosol.internal.util.NullMessagePublisher;
import com.tangosol.io.SizeEstimatingBufferOutput;
import com.tangosol.io.WrapperBufferInput;
import com.tangosol.io.WrapperBufferOutput;
import com.tangosol.io.WrapperDataInputStream;
import com.tangosol.io.WrapperDataOutputStream;
import com.tangosol.io.WriteBuffer;
import com.tangosol.license.LicenseException;
import com.tangosol.net.ActionPolicy;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.ServiceInfo;
import com.tangosol.net.management.NotificationManager;
import com.tangosol.net.management.Registry;
import com.tangosol.net.security.LocalPermission;
import com.tangosol.net.security.PermissionInfo;
import com.tangosol.run.component.EventDeathException;
import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;
import com.tangosol.util.InflatableSet;
import com.tangosol.util.Listeners;
import com.tangosol.util.LiteMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.WrapperException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntPredicate;
import javax.management.Notification;

/**
 * Base definition of a clustered Service component.
 * 
 * Internal Messages are defined with negative message types.  
 * 
 * The message ranges from [-32, -1], [1,32] are reserved for usage by the Grid
 * component
 * 
 * Currently used MessageTypes:
 * -24 NotifyConnectionClose
 * -23 NotifyConnectionOpen
 * -22 NotifyServiceQuiescence
 * -21 BusEventMessage
 * -20  NotifyResponse
 * -18  ConfigUpdate
 * -17  ConfigSync
 * -16  ConfigResponse
 * -15  ConfigRequest
 * -14  NotifyStartup
 * -13  NotifyShutdown
 * -12  NotifyServiceLeft
 * -11  NotifyServiceLeaving
 * -10  NotifyServiceJoining (was "NotifyServiceJoined")
 * -9    NotifyServiceAnnounced
 * -8    NotifyPollClosed
 * -7    NotifyMessageReceipt
 * -6    NotifyMemberLeft
 * -5    NotifyMemberLeaving
 * -4    NotifyMemberJoined
 * -3    MemberConfigUpdate
 * -2    MemberWelcome   (was "MemberConfigResponse")
 * -1    MemberWelcomeRequest   (was "MemberConfigRequest")
 * 1     Acknowledgement
 * 2     Response
 * 3     PingRequest
 * 4     MemberJoined
 * 5     * RESERVED for join protocol compatibility *
 * 6     * RESERVED for join protocol compatibility *
 * 7     * RESERVED for join protocol compatibility *
 * 8     * RESERVED for join protocol compatibility *
 * 9     * RESERVED for join protocol compatibility *
 * 10   * RESERVED for join protocol compatibility *
 * 11   * RESERVED for join protocol compatibility *
 * 12   * RESERVED for join protocol compatibility *
 * 13   * RESERVED for join protocol compatibility *
 * 17   * RESERVED for join protocol compatibility *
 * 18    MemberRecovered
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment", "JavadocBlankLines", "JavadocDeclaration", "EnhancedSwitchMigration"})
public abstract class Grid
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service
        implements com.tangosol.internal.util.GridComponent,
                   com.tangosol.net.Service
    {
    // ---- Fields declarations ----
    
    /**
     * Property AcceptingOthers
     *
     * Set to true when the Service has advanced to the state at which it can
     * accept requests from other Members.
     * 
     * Note: Services should not design this property (ClusterService is the
     * only permissible exception).
     */
    private boolean __m_AcceptingOthers;
    
    /**
     * Property ActionPolicy
     *
     * The action policy for this service.
     */
    private com.tangosol.net.ActionPolicy __m_ActionPolicy;
    
    /**
     * Property BaseSUIDThisMember
     *
     * The Base SUID for this Member.
     * 
     * Stored as Long to avoid needless production when used with standard Java
     * "maps".
     */
    private Long __m_BaseSUIDThisMember;
    
    /**
     * Property BufferManager
     *
     * The BufferManager which to release the received buffers to. This
     * property is used only if the service is using the "datagram" transport.
     */
    private com.oracle.coherence.common.io.BufferManager __m_BufferManager;
    
    /**
     * Property Cluster
     *
     * The Cluster that this Service is a part of.
     */
    private transient com.tangosol.net.Cluster __m_Cluster;
    
    /**
     * Property ClusterMemberSet
     *
     * Set of all Members in the cluster. This set is a live reference to the
     * MasterMemberSet maintained by the ClusterService and should be used
     * cautiously in a read-only manner.
     */
    private com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet __m_ClusterMemberSet;
    
    /**
     * Property CompletedRequestSUIDs
     *
     * A set (implemented via a map) of completed request SUIDs.  SUIDs only
     * exist in this set while there are older incomplete SUIDs.
     * 
     * Used by requests that need to implement idempotent re-execution in the
     * case of a failover.
     */
    private transient java.util.concurrent.ConcurrentMap __m_CompletedRequestSUIDs;
    
    /**
     * Property CONFIG_MAP_SERVICE
     *
     * Type constant for the Service config map.
     */
    public static final int CONFIG_MAP_SERVICE = 1;
    
    /**
     * Property DepartingMembers
     *
     * This MemberSet contains members that are about to be removed from the
     * ServiceMemberSet. Used only for Members connected via a MessageBus while
     * the "release" operation is in progress. Used only by the service thread
     * to throttle the MemberWelcome protocol.
     */
    private transient com.tangosol.coherence.component.net.MemberSet __m_DepartingMembers;
    
    /**
     * Property InWait
     *
     * True iff the service thread is waiting, i.e. in onWait()
     * 
     * @volatile - read on threads adding to the service's queue
     */
    private volatile boolean __m_InWait;
    
    /**
     * Property MemberListeners
     *
     * The list of registered MemberListeners.
     * 
     * @see #addMemberListener and #removeMemberListener
     */
    private com.tangosol.util.Listeners __m_MemberListeners;
    
    /**
     * Property MESSAGE_OFFSET
     *
     * The maximum absolute value for the internal (negative) message ids.
     */
    public static final int MESSAGE_OFFSET = 32;
    
    /**
     * Property MessageClass
     *
     * An indexed property that translates Message numbers into Message classes.
     */
    private Class[] __m_MessageClass;
    
    /**
     * Property MessageClassMap
     *
     * A map, keyed by message name, of Message classes known to this Grid
     * component.
     */
    private java.util.Map __m_MessageClassMap;
    
    /**
     * Property MessageHandler
     *
     * MessageHandler used by this service for MessageBus-based communications.
     */
    private com.tangosol.coherence.component.net.MessageHandler __m_MessageHandler;
    
    /**
     * Property MessagePublisher
     *
     * The MessagePublisher that the Service uses to send Messages.
     * 
     * @volatile - see Grid.stop()
     */
    private volatile com.tangosol.internal.util.MessagePublisher __m_MessagePublisher;
    
    /**
     * Property OldestPendingRequestSUIDCounter
     *
     * The AtomicLong backing OldestPendingRequestSUID.
     */
    private transient java.util.concurrent.atomic.AtomicLong __m_OldestPendingRequestSUIDCounter;
    
    /**
     * Property PendingQuiescenceResponses
     *
     * Delayed response messages for a pending QuiescenceRequests, to be sent
     * once service has finished quiescencing. null if there are no pending
     * reponses.
     */
    private transient java.util.Collection __m_PendingQuiescenceResponses;
    
    /**
     * Property PollArray
     *
     * This is a WindowedArray of active Poll objects.
     */
    private Grid.PollArray __m_PollArray;
    
    /**
     * Property QueueDeferred
     *
     * This is the Queue to which messages are deferred until startup is
     * complete.
     */
    private com.tangosol.coherence.component.util.Queue __m_QueueDeferred;
    
    /**
     * Property QueueDeferredWelcome
     *
     * This Queue holds deferred MemberWelcomeRequest messages while this
     * member is yet to finish the join protocol or any Member departure
     * processing is in progress. Used only by the service thread to order and
     * throttle the MemberWelcome protocol.
     */
    private transient com.tangosol.coherence.component.util.Queue __m_QueueDeferredWelcome;
    
    /**
     * Property RANDOM
     *
     * Special constant used by the adjustWaitTime() method indicating the
     * system time.
     */
    protected static final java.util.Random RANDOM;
    
    /**
     * Property RequestTimeout
     *
     * A default timeout value for polls and PriorityTasks that don't
     * explicitly specify the request timeout value.
     */
    private long __m_RequestTimeout;
    
    /**
     * Property ResourceRegistry
     *
     * ResourceRegistry associated with this Service.
     */
    private com.tangosol.util.ResourceRegistry __m_ResourceRegistry;
    
    /**
     * Property ServiceConfigMap
     *
     * This ObservableMap is shared by all Members running this Service and is
     * maintained on the service thread. All updates are published via the
     * senior Service Member.
     * 
     * Services may need to verify locally configured properties to ensure that
     * they are consistent across the grid (e.g. partition-count, backup-count,
     * etc.)  There is a well-known entry in the ServiceConfigMap, keyed by
     * ﻿Integer((int) '$'), that holds this configuration.
     */
    private Grid.ServiceConfig.Map __m_ServiceConfigMap;
    
    /**
     * Property ServiceFailurePolicy
     *
     */
    private com.tangosol.net.ServiceFailurePolicy __m_ServiceFailurePolicy;
    
    /**
     * Property ServiceId
     *
     * The id assigned to this Service within the cluster.
     */
    private int __m_ServiceId;
    
    /**
     * Property ServiceLeftActions
     *
     * The Map<Member, List<Continuation>> of actions whose processing is
     * deferred until the associated member's departure from the service has
     * been processed.
     */
    private java.util.Map __m_ServiceLeftActions;
    
    /**
     * Property ServiceMemberSet
     *
     * Set of Members that joined this service. As of Coherence 3.7.1, for all
     * services except the ClusterService (service zero) this set is not the
     * same ServiceMemberSet that is held by the ClusterService in the
     * corresponding ServiceInfo.
     */
    private com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet __m_ServiceMemberSet;
    
    /**
     * Property StatsPollCount
     *
     * Statistics: total number of Polls processed.
     */
    private transient long __m_StatsPollCount;
    
    /**
     * Property StatsPollDuration
     *
     * Statistics: total duration (in milliseconds) taken to process Polls.
     */
    private transient long __m_StatsPollDuration;
    
    /**
     * Property StatsPollMaxDuration
     *
     * Statistics: maximum duration (in milliseconds) taken to process a single
     * Poll.
     */
    private transient long __m_StatsPollMaxDuration;
    
    /**
     * Property StatsSent
     *
     * Statistics: total number of sent messages.
     */
    private transient long __m_StatsSent;
    
    /**
     * Property StatsSentLocal
     *
     * Statistics: total number of self-addressed messages.
     */
    private transient long __m_StatsSentLocal;
    
    /**
     * Property StatsTimeoutCount
     *
     * The total number of timed-out requests since the last time the
     * statistics were reset.
     * 
     * @volatile
     */
    private volatile transient long __m_StatsTimeoutCount;
    
    /**
     * Property SUID_REQUEST
     *
     * The SUID domain used for tracking requests.
     */
    public static final int SUID_REQUEST = 0;
    
    /**
     * Property SUIDCounter
     *
     * An AtomicCounter object that is used for SUID related calculations.
     */
    private transient java.util.concurrent.atomic.AtomicLong[] __m_SUIDCounter;
    
    /**
     * Property SuspendPollLimit
     *
     * The poll id which represents the maximum allowable poll which can be
     * sent currently.  A message with a greater poll id must not be sent until
     * this value increases making the send allowable.  Each time the value
     * increases the AtomicLong will also be notified, allowing for threads to
     * wait on this value.
     */
    private java.util.concurrent.atomic.AtomicLong __m_SuspendPollLimit;
    
    /**
     * Property TIME_CLUSTER
     *
     * Special constant used by the adjustWaitTime() method indicating the
     * cluster time.
     */
    public static final long TIME_CLUSTER = -3L;
    
    /**
     * Property TIME_SAFE
     *
     * Special constant used by the adjustWaitTime() method indicating the safe
     * time.
     */
    public static final long TIME_SAFE = -2L;
    
    /**
     * Property TIME_SYSTEM
     *
     * Special constant used by the adjustWaitTime() method indicating the
     * system time.
     */
    public static final long TIME_SYSTEM = -1L;
    
    /**
     * Property UserContext
     *
     * User context object associated with this Service.
     */
    private Object __m_UserContext;
    
    /**
     * Property WrapperStreamFactoryAllList
     *
     * All filters that should be applied during onNotify.
     */
    private java.util.List __m_WrapperStreamFactoryAllList;
    
    /**
     * Property WrapperStreamFactoryList
     *
     * List of WrapperStreamFactory objects that affect how Messages sent from/
     * to this specific Service are written and read.
     */
    private java.util.List __m_WrapperStreamFactoryList;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        try
            {
            RANDOM = new java.util.Random();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Acknowledgement", Grid.Acknowledgement.get_CLASS());
        __mapChildren.put("BusEventMessage", Grid.BusEventMessage.get_CLASS());
        __mapChildren.put("ConfigRequest", Grid.ConfigRequest.get_CLASS());
        __mapChildren.put("ConfigResponse", Grid.ConfigResponse.get_CLASS());
        __mapChildren.put("ConfigSync", Grid.ConfigSync.get_CLASS());
        __mapChildren.put("ConfigUpdate", Grid.ConfigUpdate.get_CLASS());
        __mapChildren.put("DispatchEvent", Grid.DispatchEvent.get_CLASS());
        __mapChildren.put("DispatchNotification", Grid.DispatchNotification.get_CLASS());
        __mapChildren.put("MemberConfigUpdate", Grid.MemberConfigUpdate.get_CLASS());
        __mapChildren.put("MemberJoined", Grid.MemberJoined.get_CLASS());
        __mapChildren.put("MemberRecovered", Grid.MemberRecovered.get_CLASS());
        __mapChildren.put("MemberWelcome", Grid.MemberWelcome.get_CLASS());
        __mapChildren.put("MemberWelcomeRequest", Grid.MemberWelcomeRequest.get_CLASS());
        __mapChildren.put("MemberWelcomeRequestTask", Grid.MemberWelcomeRequestTask.get_CLASS());
        __mapChildren.put("NotifyConnectionClose", Grid.NotifyConnectionClose.get_CLASS());
        __mapChildren.put("NotifyConnectionOpen", Grid.NotifyConnectionOpen.get_CLASS());
        __mapChildren.put("NotifyMemberJoined", Grid.NotifyMemberJoined.get_CLASS());
        __mapChildren.put("NotifyMemberLeaving", Grid.NotifyMemberLeaving.get_CLASS());
        __mapChildren.put("NotifyMemberLeft", Grid.NotifyMemberLeft.get_CLASS());
        __mapChildren.put("NotifyMessageReceipt", Grid.NotifyMessageReceipt.get_CLASS());
        __mapChildren.put("NotifyPollClosed", Grid.NotifyPollClosed.get_CLASS());
        __mapChildren.put("NotifyResponse", Grid.NotifyResponse.get_CLASS());
        __mapChildren.put("NotifyServiceAnnounced", Grid.NotifyServiceAnnounced.get_CLASS());
        __mapChildren.put("NotifyServiceJoining", Grid.NotifyServiceJoining.get_CLASS());
        __mapChildren.put("NotifyServiceLeaving", Grid.NotifyServiceLeaving.get_CLASS());
        __mapChildren.put("NotifyServiceLeft", Grid.NotifyServiceLeft.get_CLASS());
        __mapChildren.put("NotifyServiceQuiescence", Grid.NotifyServiceQuiescence.get_CLASS());
        __mapChildren.put("NotifyShutdown", Grid.NotifyShutdown.get_CLASS());
        __mapChildren.put("NotifyStartup", Grid.NotifyStartup.get_CLASS());
        __mapChildren.put("PingRequest", Grid.PingRequest.get_CLASS());
        __mapChildren.put("ProtocolContext", Grid.ProtocolContext.get_CLASS());
        __mapChildren.put("Response", Grid.Response.get_CLASS());
        __mapChildren.put("WrapperGuardable", Grid.WrapperGuardable.get_CLASS());
        }
    
    // Initializing constructor
    public Grid(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        
        // state initialization: private properties
        try
            {
            __m_MemberListeners = new com.tangosol.util.Listeners();
            __m_QueueDeferred = new com.tangosol.coherence.component.util.Queue();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    // Getter for virtual constant ServiceType
    public String getServiceType()
        {
        return null;
        }
    
    // Getter for virtual constant SUIDCounterLength
    public int getSUIDCounterLength()
        {
        return 1;
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid".replace('/', '.'));
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
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    public void addMemberListener(com.tangosol.net.MemberListener l)
        {
        ensureEventDispatcher();
        getMemberListeners().add(l);
        }
    
    /**
     * Adjust the wait interval/time according to the original wait interval and
    * the time adjustment value.
    * 
    * @param cWaitMillis the wait interval; zero means no wait; negative value
    * means indefinite wait
    * @param cAdjustMillis the adjustment time; must be non-negative or one of
    * the special negative values: TIME_*
    * 
    * @return the adjusted wait interval; always positive
    * 
    * Note: passing a timestamp as the adjustment time parameter makes this
    * method return the corresponding timeout timestamp
     */
    public long adjustWaitTime(long cWaitMillis, long cAdjustMillis)
        {
        // import com.tangosol.util.Base;
        
        if (cWaitMillis < 0L)
            {
            return Long.MAX_VALUE;
            }
        
        if (cAdjustMillis < 0L)
            {
            if (cAdjustMillis == TIME_CLUSTER)
                {
                cAdjustMillis = getClusterTime();
                }
            else if
                (cAdjustMillis == TIME_SAFE)
                {
                cAdjustMillis = Base.getSafeTimeMillis();
                }
            else if
                (cAdjustMillis == TIME_SYSTEM)
                {
                cAdjustMillis = System.currentTimeMillis();
                }
            else
                {
                throw new IllegalArgumentException("Invalid adjustment: " + cAdjustMillis);
                }
            }
        
        // adjustment may overflow into negative territory
        cWaitMillis += cAdjustMillis;
        return cWaitMillis < 0L ? Long.MAX_VALUE : cWaitMillis;
        }
    
    /**
     * Calculate the smallest (oldest) SUID registered by the specified member
    * in the specified LongArray; -1 if there are none.
     */
    public static long calculateOldestSUID(com.tangosol.util.LongArray array, int nMember)
        {
        long lBase = getBaseSUID(nMember);
        long lSUID;
        
        synchronized (array)
            {
            lSUID = array.ceilingIndex(lBase);
            }
        
        return lSUID == -1L || getMemberId(lSUID) != nMember ? -1L : lSUID;
        }
    
    /**
     * Calculate the smallest (oldest) SUID registered by the specified member
    * in the specified SortedMap; -1 if there are none.
     */
    public static long calculateOldestSUID(java.util.NavigableMap mapSUID, int nMember)
        {
        long lBaseMember = getBaseSUID(nMember);
        Long LSUID       = (Long) mapSUID.ceilingKey(Long.valueOf(lBaseMember));
        
        return LSUID == null || getBaseSUID(LSUID.longValue()) != lBaseMember
            ? -1L : LSUID.longValue();
        }
    
    /**
     * Calculate the RequestTimeout value for  the specified RequestMessage.
    * 
    * @param msg  (optional) RequestMessage object
     */
    public long calculateRequestTimeout(com.tangosol.coherence.component.net.message.RequestMessage msg)
        {
        // import com.tangosol.net.PriorityTask;
        
        long cTimeoutMillis = getRequestTimeout();
        if (msg instanceof PriorityTask)
            {
            cTimeoutMillis = adjustTimeout(cTimeoutMillis,
                ((PriorityTask) msg).getRequestTimeoutMillis());
            }
        
        return cTimeoutMillis;
        }
    
    /**
     * Security check.
     */
    protected void checkShutdownPermission()
        {
        // import com.tangosol.net.security.LocalPermission;
        
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            security.checkPermission(
                new LocalPermission("Cluster.shutdown"));
            }
        }
    
    // Declared at the super level
    /**
     * Create a new Default dependencies object by copying the supplies
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone, producing their variant of the
    * dependencies interface.
    * 
    * @return the cloned dependencies
     */
    protected com.tangosol.internal.net.service.DefaultServiceDependencies cloneDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.grid.DefaultGridDependencies;
        // import com.tangosol.internal.net.service.grid.GridDependencies;
        
        return new DefaultGridDependencies((GridDependencies) deps);
        }
    
    /**
     * Called on the service thread when the service is stopped to close all
    * currently open polls.
     */
    protected void closePolls()
        {
        // import Component.Net.Poll;
        
        getServiceConfigMap().clearPendingPolls();
        
        // this method could be called without any prior synchronization;
        // we need to make sure the FirstIndex and [most importantly]
        // the LastIndex values are flushed (see COH-618)
        
        Grid.PollArray waPoll = getPollArray();
        long       lFirst;
        long       lLast;
        
        waPoll.checkPolls();
        
        synchronized (waPoll)
            {
            lFirst = waPoll.getFirstIndex();
            lLast  = waPoll.getLastIndex();
            }
        
        for (long l = lFirst; l <= lLast; ++l)
            {
            Poll poll = (Poll) waPoll.get(l);
            if (poll != null)
                {
                poll.close();
                }
            }
        }
    
    /**
     * Called on the service thread to close all polls to the specified (dead)
    * Member
     */
    public void closePolls(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Poll;
        
        _assert(Thread.currentThread() == getThread());
        
        // this method could be called without any prior synchronization;
        // we need to make sure the FirstIndex and [most importantly]
        // the LastIndex values are flushed (see COH-618)
        
        Grid.PollArray waPoll = getPollArray();
        long       lFirst;
        long       lLast;
        
        synchronized (waPoll)
            {
            lFirst = waPoll.getFirstIndex();
            lLast  = waPoll.getLastIndex();
            }
        
        for (long l = lFirst; l <= lLast; ++l)
            {
            Poll poll = (Poll) waPoll.get(l);
            if (poll != null)
                {
                poll.onLeft(member);
                }
            }
        }
    
    /**
     * Collect available connection information regarding the specified Member.
     */
    public String collectTransportStats(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Member;
        // import Component.Net.MessageHandler;
        // import com.oracle.coherence.common.net.exabus.EndPoint;
        
        MessageHandler handler = getMessageHandler();
        if (handler != null)
            {
            EndPoint peer = getServiceMemberSet().getServiceEndPoint(member.getId());
        
            if (peer != null)
                {
                return String.valueOf(handler.getConnectionMap().get(peer));
                }
            }
        
        return member.toString(Member.SHOW_STATS);
        }
    
    /**
     * Compare importance of the specified Member with the local Member. This
    * method is usually called during processing of the partial packet delivery
    * failure, when a decision has to be made to either kill another member or
    * commit a suicide.
    * 
    * @return zero if this service does not care or views those members as of
    * "equal importance"; negative number if the importance of this Member is
    * lesser than of the specified one and positive number if the importance of
    * this Member is greater than of the specified one. The absolute value of
    * the return value signifies a difference in importance.
     */
    public int compareImportance(com.tangosol.coherence.component.net.Member memberThat)
        {
        // TODO: provide an ability to plug a custom feedback
        return 0;
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    // Declared at the super level
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.grid.DefaultGridDependencies;
        // import com.tangosol.internal.net.service.grid.LegacyXmlGridHelper as com.tangosol.internal.net.service.grid.LegacyXmlGridHelper;
        
        setDependencies(com.tangosol.internal.net.service.grid.LegacyXmlGridHelper.fromXml(xml,
            new DefaultGridDependencies(), getOperationalContext(), getContextClassLoader()));
        
        setServiceConfig(xml);
        }
    
    /**
     * Create a ResolvableParameterList with the service level parameters (e.g.
    * "service-name", "class-loader")
     */
    protected com.tangosol.coherence.config.ResolvableParameterList createResolver()
        {
        // import com.tangosol.net.Cluster as com.tangosol.net.Cluster;
        // import com.tangosol.coherence.config.ResolvableParameterList;
        // import com.tangosol.config.expression.Parameter;
        
        ResolvableParameterList resolver = new ResolvableParameterList();
        
        resolver.add(new Parameter("class-loader", getContextClassLoader()));
        resolver.add(new Parameter("service-name", getServiceName()));
        
        com.tangosol.net.Cluster cluster = getCluster();
        if (cluster != null)
            {
            resolver.add(new Parameter("cluster-name", cluster.getClusterName()));
            }
        
        return resolver;
        }
    
    /**
     * Defer the completion of the specified continuation until after the
    * specified member's departure from the service has been processed.
    * Called on the service thread only.
     */
    public void deferUntilServiceLeft(com.tangosol.coherence.component.net.Member member, com.oracle.coherence.common.base.Continuation continuation)
        {
        // import java.util.LinkedList;
        // import java.util.List;
        // import java.util.Map;
        
        Map  mapDeferred = getServiceLeftActions();
        List listActions = (List) mapDeferred.get(member);
        if (listActions == null)
            {
            listActions = new LinkedList();
            mapDeferred.put(member, listActions);
            }
        
        listActions.add(continuation);
        }
    
    public boolean deserializeMessage(com.tangosol.coherence.component.net.Message msg)
        {
        // import com.tangosol.io.ReadBuffer$BufferInput as com.tangosol.io.ReadBuffer.BufferInput;
        // import com.tangosol.io.WrapperBufferInput$VersionAwareBufferInput as com.tangosol.io.WrapperBufferInput.VersionAwareBufferInput;
        
        if (!validateMessage(msg))
            {
            return false;
            }
        
        try
            {
            // construct a stream to read the message contents
            com.tangosol.io.ReadBuffer.BufferInput input = msg.getReadBuffer().getBufferInput();
        
            // skip the service id & message type (needed for the "bus" transport)
            input.readShort(); // service id
            input.readShort(); // message type
        
            boolean fWrapped = isProtocolFiltered();
            if (fWrapped)
                {
                input = wrapStream(input, msg);
                }
            if (!(input instanceof com.tangosol.io.WrapperBufferInput.VersionAwareBufferInput))
                {
                input = new com.tangosol.io.WrapperBufferInput.VersionAwareBufferInput(input, getContextClassLoader(), msg);
                }
        
            // read the message contents
            msg.readInternal(input);
            msg.read(input);
        
            if (fWrapped)
                {
                input.close();
                }
        
            return true;
            }
        catch (Throwable e)
            {
            return onMessageReadException(e, msg);
            }
        finally
            {
            msg.setDeserializationRequired(false);
            msg.releaseIncoming();
            }
        }
    
    public void dispatchMemberEvent(com.tangosol.coherence.component.net.Member member, int nEvent)
        {
        // import com.tangosol.net.MemberEvent;
        // import com.tangosol.util.Listeners;
        
        Listeners listeners = getMemberListeners();
        if (!listeners.isEmpty())
            {
            dispatchEvent(new MemberEvent(this, nEvent, member), listeners);
            }
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    /**
     * Dispatch a JMX notification.
    * 
    * @see NotificationManager#trigger()
    * 
    * Note: oData must be an intrinsic or an OpenType value
     */
    public void dispatchNotification(String sMBeanName, String sType, String sMsg, Object oData)
        {
        // import com.tangosol.net.management.Registry;
        
        Registry registry = getCluster().getManagement();
        if (registry != null)
            {
            // COH-25733 - defer "isSubscribedTo()" check as it contains a blocking (synchronized) call
            Grid.DispatchNotification notify = (Grid.DispatchNotification)
                _newChild("DispatchNotification");
            notify.setMBeanName(sMBeanName);
            notify.setSource(getThisMember().toString());
            notify.setType(sType);
            notify.setMessage(sMsg);
            notify.setUserData(oData);
        
            ensureEventDispatcher().getQueue().add(notify);
            }
        }
    
    /**
     * Notify the service thread to close the poll.
    * 
    * @param poll the poll to close
     */
    public void doPollClose(com.tangosol.coherence.component.net.Poll poll)
        {
        if (!poll.isClosed())
            {
            Grid.NotifyPollClosed msgNotify = (Grid.NotifyPollClosed) instantiateMessage("NotifyPollClosed");
        
            msgNotify.setNotifyPoll(poll);
            send(msgNotify);
            }
        }
    
    /**
     * Notify the service thread to close the poll.
    * 
    * @param poll          the poll to close
    * @param eReason  Reason for closing the poll
     */
    public void doPollClose(com.tangosol.coherence.component.net.Poll poll, Throwable eReason)
        {
        if (!poll.isClosed())
            {
            Grid.NotifyPollClosed msgNotify = (Grid.NotifyPollClosed) instantiateMessage("NotifyPollClosed");
        
            msgNotify.setException(eReason);
            msgNotify.setNotifyPoll(poll);
        
            send(msgNotify);
            }
        }
    
    /**
     * Notify the service thread to update a poll to reflect that the specified
    * recipient member has left.
    * 
    * @param poll          the poll that should be updated
    * @param member  the message recipient that has left
     */
    public void doPollMemberLeft(com.tangosol.coherence.component.net.Poll poll, com.tangosol.coherence.component.net.Member member)
        {
        Grid.NotifyPollClosed msgNotify = (Grid.NotifyPollClosed) instantiateMessage("NotifyPollClosed");
        
        msgNotify.setNotifyPoll(poll);
        msgNotify.setNotifyMember(member);
        send(msgNotify);
        }
    
    /**
     * Wait for the service's associated backlog to drain.
    * 
    * @param setMembers  the members of interest
    * @param cMillis             the maximum amount of time to wait, or 0 for
    * infinite
    * 
    * @return the remaining timeout
    * 
    * @throw RequestTimeoutException on timeout
     */
    public long drainOverflow(com.tangosol.coherence.component.net.MemberSet setMembers, long cMillis)
            throws java.lang.InterruptedException
        {
        // import Component.Net.Cluster;
        // import Component.Net.MessageHandler;
        
        // monitor the event dispatcher queue and slow down if it gets too long
        Grid.EventDispatcher dispatcher = (Grid.EventDispatcher) getEventDispatcher();
        if (dispatcher != null)
            {
            cMillis = dispatcher.drainOverflow(cMillis);
            }
        
        // monitor message publisher as well
        return getMessagePublisher().drainOverflow(setMembers, cMillis);
        }
    
    /**
     * Return an estimate of the serialized size of the supplied message in
    * bytes.
    * 
    * @param msg  the message to estimate
     */
    public int estimateMessageSize(com.tangosol.coherence.component.net.Message msg)
            throws java.io.IOException
        {
        // import com.tangosol.io.SizeEstimatingBufferOutput;
        
        int cbMsg = msg.getEstimatedByteSize();
        
        return cbMsg < 0
            ? serializeMessage(msg, new SizeEstimatingBufferOutput())
            : cbMsg;
        }
    
    /**
     * Ensure that the send queue has been flushed.  This must be called prior
    * to blocking for a response which is dependent on any sent message.
     */
    public void flush()
        {
        // import com.tangosol.internal.util.MessagePublisher;
        // import com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier;
        
        
        if (getThread() != Thread.currentThread())
            {
            getQueue().flush();
            }
        
        // MessagePublisher could be null during the MessageBus initilization
        MessagePublisher publisher = getMessagePublisher();
        if (publisher != null)
            {
            publisher.flush();
            }
        
        SingleWaiterCooperativeNotifier.flush(); // see Poll.Notifier
        }
    
    /**
     * @return a human-readible description of the Service statistics
     */
    public String formatStats(boolean fVerbose)
        {
        String  sStats  = formatStats();
        if (fVerbose)
            {
            sStats += " " + getMessagePublisher(); 
            }
        
        return sStats;
        }
    
    // Accessor for the property "ActionPolicy"
    /**
     * Getter for property ActionPolicy.<p>
    * The action policy for this service.
     */
    public com.tangosol.net.ActionPolicy getActionPolicy()
        {
        return __m_ActionPolicy;
        }
    
    /**
     * Calculate the smallest possible SUID value that could be generated by the
    * specified member id.
     */
    public static long getBaseSUID(int nMember)
        {
        return ((long) nMember) << 48;
        }
    
    /**
     * Calculate the smallest possible SUID value that could be generated by the
    * same member that generated the specified SUID.
     */
    public static long getBaseSUID(long lSUID)
        {
        return lSUID & 0xFFFF000000000000L;
        }
    
    // Accessor for the property "BaseSUIDThisMember"
    /**
     * Getter for property BaseSUIDThisMember.<p>
    * The Base SUID for this Member.
    * 
    * Stored as Long to avoid needless production when used with standard Java
    * "maps".
     */
    public Long getBaseSUIDThisMember()
        {
        return __m_BaseSUIDThisMember;
        }
    
    // Accessor for the property "BufferManager"
    /**
     * Getter for property BufferManager.<p>
    * The BufferManager which to release the received buffers to. This property
    * is used only if the service is using the "datagram" transport.
     */
    public com.oracle.coherence.common.io.BufferManager getBufferManager()
        {
        return __m_BufferManager;
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    // Accessor for the property "Cluster"
    /**
     * Getter for property Cluster.<p>
    * The Cluster that this Service is a part of.
     */
    public com.tangosol.net.Cluster getCluster()
        {
        return __m_Cluster;
        }
    
    // Accessor for the property "ClusterMemberSet"
    /**
     * Getter for property ClusterMemberSet.<p>
    * Set of all Members in the cluster. This set is a live reference to the
    * MasterMemberSet maintained by the ClusterService and should be used
    * cautiously in a read-only manner.
     */
    public com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet getClusterMemberSet()
        {
        return __m_ClusterMemberSet;
        }
    
    // Accessor for the property "ClusterOldestMember"
    /**
     * Getter for property ClusterOldestMember.<p>
    * Returns the Member object for the senior Member, or null if this Member
    * has not yet joined the cluster.
     */
    public com.tangosol.coherence.component.net.Member getClusterOldestMember()
        {
        return getClusterMemberSet().getOldestMember();
        }
    
    // Accessor for the property "ClusterService"
    /**
     * Getter for property ClusterService.<p>
    * (Helper) The ClusterService instance.
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService getClusterService()
        {
        // import Component.Net.Cluster;
        
        return ((Cluster) getCluster()).getClusterService();
        }
    
    // Accessor for the property "ClusterTime"
    /**
     * Getter for property ClusterTime.<p>
    * Returns the current "cluster time", which is analogous to the
    * Base.getSafeTimeMillis() except that the cluster time is (almost) the
    * same for all Members in the cluster.
    * 
    * @see ClusterTimeVariance
     */
    public long getClusterTime()
        {
        return getCluster().getTimeMillis();
        }
    
    // Accessor for the property "ClusterTimeVariance"
    /**
     * Getter for property ClusterTimeVariance.<p>
    * The maximum number of apparent milliseconds difference allowed between
    * Cluster time for various nodes. Specifically, if a cluster node N1 takes
    * a ClusterTime snapshot (T1) and sends it to a cluster node N2, which at
    * that time takes the ClusterTime snapshot (T2) then its guaranteed that:
    * 
    *     T2 >= T1 - ClusterTimeVariance
     */
    public int getClusterTimeVariance()
        {
        return getClusterService().getTimestampMaxVariance();
        }
    
    // Accessor for the property "CompletedRequestSUIDs"
    /**
     * Getter for property CompletedRequestSUIDs.<p>
    * A set (implemented via a map) of completed request SUIDs.  SUIDs only
    * exist in this set while there are older incomplete SUIDs.
    * 
    * Used by requests that need to implement idempotent re-execution in the
    * case of a failover.
     */
    public java.util.concurrent.ConcurrentMap getCompletedRequestSUIDs()
        {
        return __m_CompletedRequestSUIDs;
        }
    
    /**
     * Return the ServiceConfig$Map for the specified map type.
     */
    public com.tangosol.coherence.component.util.ServiceConfig.Map getConfigMap(int nMapType)
        {
        if (nMapType == CONFIG_MAP_SERVICE)
            {
            return getServiceConfigMap();
            }
        else
            {
            throw new IllegalArgumentException();
            }
        }
    
    public com.tangosol.coherence.component.net.MemberSet getConfigMapUpdateMembers(com.tangosol.coherence.component.util.ServiceConfig.Map map)
        {
        return getOthersMemberSet();
        }
    
    /**
     * Delay the service thread for some time based on the value of the
    * {@code coherence.service.<serviceName>.<sOperation>.delay} property,
    *  {@code coherence.service.<sOperation>.delay} property, if the same
    * delay value should be used for all services.
    * <p/>
    * The acceptable property values are:
    * <ul>
    *      <li>0: do not delay (default)</li>
    *      <li>-1: delay forever; will simply return {@code false}</li>
    *      <li>any positive value: introduce a fixed delay of {@code value}
    * ms</li>
    *      <li>any negative value other than -1: introduce a random delay bound
    * by {@code Math.abs(value)} ms</li>
    * </ul>
    * 
    * @param sOperation  the name of the operation we are delaying
    * 
    * @return {@code true} if the delayed operation should proceed; {@code
    * false} otherwise
     */
    protected long getDelay(String sOperation)
        {
        // import com.tangosol.coherence.config.Config;
        // import java.util.Random;
        
        int cMillis = Config.getInteger("coherence.service." + getServiceName() + "." + sOperation + ".delay",
                                      Config.getInteger("coherence.service." + sOperation + ".delay", 0).intValue()).intValue();
        if (cMillis < 0L)
            {
            cMillis = RANDOM.nextInt(-1 * cMillis);
            }
        return (long) cMillis;
        }
    
    // Accessor for the property "DepartingMembers"
    /**
     * Getter for property DepartingMembers.<p>
    * This MemberSet contains members that are about to be removed from the
    * ServiceMemberSet. Used only for Members connected via a MessageBus while
    * the "release" operation is in progress. Used only by the service thread
    * to throttle the MemberWelcome protocol.
     */
    public com.tangosol.coherence.component.net.MemberSet getDepartingMembers()
        {
        return __m_DepartingMembers;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of additional Service properties. Used by
    * toString().
     */
    public String getDescription()
        {
        StringBuffer sb = new StringBuffer();
        
        sb.append("Id=")
          .append(getServiceId())
          ;
        
        if (isRunning() && getServiceOldestMember() != null)
            {
            sb.append(", OldestMemberId=")
              .append(getServiceOldestMember().getId());
            }
        
        return sb.toString();
        }
    
    // Accessor for the property "EndPointName"
    /**
     * Getter for property EndPointName.<p>
    * (Calculated) The local EndPoint name used by this services bus. This will
    * return an empty string if the service uses the "datagram" transport.
     */
    public String getEndPointName()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = getMessageHandler();
        return handler == null ? "" : handler.getMessageBus().getLocalEndPoint().toString();
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    public com.tangosol.net.ServiceInfo getInfo()
        {
        return getCluster().getServiceInfo(getServiceName());
        }
    
    /**
     * Extract the member id that generated the specified SUID.
     */
    public static int getMemberId(long lSUID)
        {
        return (int) (lSUID >>> 48);
        }
    
    // Accessor for the property "MemberListeners"
    /**
     * Getter for property MemberListeners.<p>
    * The list of registered MemberListeners.
    * 
    * @see #addMemberListener and #removeMemberListener
     */
    public com.tangosol.util.Listeners getMemberListeners()
        {
        return __m_MemberListeners;
        }
    
    /**
     * Create a member set that contains only the members from the
    * ServiceMemberSet that have transitioned to the specified state.
    * 
    * @param nState the state threshold; one of the ServiceMemberSet.MEMBER_*
    * constants
     */
    protected com.tangosol.coherence.component.net.MemberSet getMemberSet(int nState)
        {
        return getMemberSet(getServiceMemberSet(), nState);
        }
    
    /**
     * Create a member set that contains only the members from the
    * ServiceMemberSet that have transitioned to the specified state.
    * 
    * @param nState the state threshold; one of the ServiceMemberSet.MEMBER_*
    * constants
     */
    protected com.tangosol.coherence.component.net.MemberSet getMemberSet(com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet setMembers, int nState)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import java.util.Iterator;
        
        MemberSet setResult = new MemberSet();
        
        for (Iterator iter = setMembers.iterator(); iter.hasNext();)
            {
            Member member = (Member) iter.next();
            if (setMembers.getState(member.getId()) == nState)
                {
                setResult.add(member);
                }
            }
        
        return setResult;
        }
    
    // Accessor for the property "MessageClass"
    /**
     * Getter for property MessageClass.<p>
    * An indexed property that translates Message numbers into Message classes.
     */
    private Class[] getMessageClass()
        {
        return __m_MessageClass;
        }
    
    // Accessor for the property "MessageClass"
    /**
     * Getter for property MessageClass.<p>
    * An indexed property that translates Message numbers into Message classes.
     */
    public Class getMessageClass(int i)
        {
        // adjust Message id to support the negative id's used by service 0
        i += MESSAGE_OFFSET;
        
        Class[] aClass = getMessageClass();
        return aClass != null && i < aClass.length ? aClass[i] : null;
        }
    
    // Accessor for the property "MessageClass"
    /**
     * Return the Message class known to the service by the specified name
    * 
    * @param sMsgName   the name of the message
     */
    public Class getMessageClass(String sMsgName)
        {
        return (Class) getMessageClassMap().get(sMsgName);
        }
    
    // Accessor for the property "MessageClassMap"
    /**
     * Getter for property MessageClassMap.<p>
    * A map, keyed by message name, of Message classes known to this Grid
    * component.
     */
    public java.util.Map getMessageClassMap()
        {
        return __m_MessageClassMap;
        }
    
    // Accessor for the property "MessageHandler"
    /**
     * Getter for property MessageHandler.<p>
    * MessageHandler used by this service for MessageBus-based communications.
     */
    public com.tangosol.coherence.component.net.MessageHandler getMessageHandler()
        {
        return __m_MessageHandler;
        }
    
    // Accessor for the property "MessagePublisher"
    /**
     * Getter for property MessagePublisher.<p>
    * The MessagePublisher that the Service uses to send Messages.
    * 
    * @volatile - see Grid.stop()
     */
    public com.tangosol.internal.util.MessagePublisher getMessagePublisher()
        {
        return __m_MessagePublisher;
        }
    
    // Accessor for the property "OldestPendingRequestSUID"
    /**
     * Getter for property OldestPendingRequestSUID.<p>
    * Calculate the smallest (oldest) pending request SUID issued by this
    * member
    * 
    * Usually this value is sent along with a request SUID to indicate that all
    * requests up to this point have been fully processed and any relevant
    * information could be safely discarded
     */
    public long getOldestPendingRequestSUID()
        {
        return getOldestPendingRequestSUIDCounter().get();
        }
    
    // Accessor for the property "OldestPendingRequestSUIDCounter"
    /**
     * Getter for property OldestPendingRequestSUIDCounter.<p>
    * The AtomicLong backing OldestPendingRequestSUID.
     */
    public java.util.concurrent.atomic.AtomicLong getOldestPendingRequestSUIDCounter()
        {
        return __m_OldestPendingRequestSUIDCounter;
        }
    
    /**
     * A helper method to obtain a MemberSet of all other Members running this
    * Service.
     */
    public com.tangosol.coherence.component.net.MemberSet getOthersMemberSet()
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        
        return getOthersMemberSet(ServiceMemberSet.MEMBER_NEW);
        }
    
    /**
     * Create a member set that contains only the members from the
    * ServiceMemberSet that have transitioned to or beyond the specified state.
    * 
    * @param nState the state threshold; one of the ServiceMemberSet.MEMBER_*
    * constants
     */
    public com.tangosol.coherence.component.net.MemberSet getOthersMemberSet(int nState)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import java.util.Iterator;
        
        ServiceMemberSet setAll    = getServiceMemberSet();
        MemberSet        setOthers = new MemberSet();
        
        setOthers.addAll(setAll);
        setOthers.remove(getThisMember());
        
        if (nState > ServiceMemberSet.MEMBER_NEW)
            {
            for (Iterator iter = setAll.iterator(); iter.hasNext();)
                {
                Member member = (Member) iter.next();
        
                if (setAll.getState(member.getId()) < nState)
                    {
                    setOthers.remove(member);
                    }
                }
            }
        return setOthers;
        }
    
    /**
     * Return a list of Polls that have been outstanding for more than specified
    * time in milliseconds, or half of the startup timeout or default guard
    * timeout for the service (whichever is greater), if the specified time is
    * <= 0.
     */
    public java.util.List getOutstandingPolls(long cAgeMillis)
        {
        // import Component.Net.Poll;
        // import com.tangosol.util.Base;
        // import java.util.Collections;
        // import java.util.LinkedList;
        
        Grid.PollArray waPoll = getPollArray();
        
        // Note: we don't synchronize for performance reasons.  We check for 
        //       an index inversion, so the only other possibility is that we may miss
        //       some new polls, which would not be old enough to be of interest.
        //       Borrowed from Grid.PollArray.validatePolls().
        long lFirst = waPoll.getFirstIndex();
        long lLast  = waPoll.getLastIndex();
        
        // if there are no polls registered, then we're done
        if (lFirst > lLast)
            {
            return Collections.emptyList();
            }
        // The threshold age will not be set for requests coming from JMX,
        // in that case, use half of the larger of guard timeout or startup timeout.
        if (cAgeMillis <= 0L)
            {
            // polls less than half of startup timeout old are ignored
            // see Grid.PollArray.validatePolls()
            cAgeMillis = Math.max(getDefaultGuardTimeout(), getStartupTimeout()) >>> 1;
            }
        
        LinkedList listPolls = new LinkedList();
        long       ldtCutoff = Base.getSafeTimeMillis() - cAgeMillis;
        
        for (long l = lFirst; l <= lLast; ++l)
            {
            Poll poll = (Poll) waPoll.get(l);
            if (poll == null || poll.isClosed())
                {
                continue;
                }
        
            // poll array is sorted by init time
            if (poll.getInitTimeMillis() >= ldtCutoff)
                {
                return listPolls;
                }
        
            listPolls.add(poll);
            }
        
        return listPolls;
        }
    
    // Accessor for the property "PendingQuiescenceResponses"
    /**
     * Getter for property PendingQuiescenceResponses.<p>
    * Delayed response messages for a pending QuiescenceRequests, to be sent
    * once service has finished quiescencing. null if there are no pending
    * reponses.
     */
    public java.util.Collection getPendingQuiescenceResponses()
        {
        return __m_PendingQuiescenceResponses;
        }
    
    // Accessor for the property "PollArray"
    /**
     * Getter for property PollArray.<p>
    * This is a WindowedArray of active Poll objects.
     */
    public Grid.PollArray getPollArray()
        {
        return __m_PollArray;
        }
    
    /**
     * Return the current protocol context based on the supplied message.
     */
    public com.tangosol.net.internal.ProtocolAwareStream.ProtocolContext getProtocolContext(com.tangosol.coherence.component.net.Message msg)
        {
        Grid.ProtocolContext context = new Grid.ProtocolContext(); // cheap child
        context.setMessage(msg);
        return context;
        }
    
    // Accessor for the property "QueueDeferred"
    /**
     * Getter for property QueueDeferred.<p>
    * This is the Queue to which messages are deferred until startup is
    * complete.
     */
    private com.tangosol.coherence.component.util.Queue getQueueDeferred()
        {
        return __m_QueueDeferred;
        }
    
    // Accessor for the property "QueueDeferredWelcome"
    /**
     * Getter for property QueueDeferredWelcome.<p>
    * This Queue holds deferred MemberWelcomeRequest messages while this member
    * is yet to finish the join protocol or any Member departure processing is
    * in progress. Used only by the service thread to order and throttle the
    * MemberWelcome protocol.
     */
    public com.tangosol.coherence.component.util.Queue getQueueDeferredWelcome()
        {
        return __m_QueueDeferredWelcome;
        }
    
    // Accessor for the property "RequestTimeout"
    /**
     * Getter for property RequestTimeout.<p>
    * A default timeout value for polls and PriorityTasks that don't explicitly
    * specify the request timeout value.
     */
    public long getRequestTimeout()
        {
        return __m_RequestTimeout;
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    // Accessor for the property "ResourceRegistry"
    /**
     * Getter for property ResourceRegistry.<p>
    * ResourceRegistry associated with this Service.
     */
    public com.tangosol.util.ResourceRegistry getResourceRegistry()
        {
        return __m_ResourceRegistry;
        }
    
    // Accessor for the property "ServiceConfigMap"
    /**
     * Getter for property ServiceConfigMap.<p>
    * This ObservableMap is shared by all Members running this Service and is
    * maintained on the service thread. All updates are published via the
    * senior Service Member.
    * 
    * Services may need to verify locally configured properties to ensure that
    * they are consistent across the grid (e.g. partition-count, backup-count,
    * etc.)  There is a well-known entry in the ServiceConfigMap, keyed by
    * ﻿Integer((int) '$'), that holds this configuration.
     */
    public Grid.ServiceConfig.Map getServiceConfigMap()
        {
        Grid.ServiceConfig.Map map = __m_ServiceConfigMap;
        
        if (map == null)
            {
            map = (Grid.ServiceConfig.Map) ((Grid.ServiceConfig) _findChild("ServiceConfig")).getMap();
            setServiceConfigMap(map);
            }
        
        return map;
        }
    
    // Accessor for the property "ServiceFailurePolicy"
    /**
     * Getter for property ServiceFailurePolicy.<p>
     */
    public com.tangosol.net.ServiceFailurePolicy getServiceFailurePolicy()
        {
        return __m_ServiceFailurePolicy;
        }
    
    // Accessor for the property "ServiceId"
    /**
     * Getter for property ServiceId.<p>
    * The id assigned to this Service within the cluster.
     */
    public int getServiceId()
        {
        return __m_ServiceId;
        }
    
    // Accessor for the property "ServiceLeftActions"
    /**
     * Getter for property ServiceLeftActions.<p>
    * The Map<Member, List<Continuation>> of actions whose processing is
    * deferred until the associated member's departure from the service has
    * been processed.
     */
    public java.util.Map getServiceLeftActions()
        {
        return __m_ServiceLeftActions;
        }
    
    // Accessor for the property "ServiceMemberSet"
    /**
     * Getter for property ServiceMemberSet.<p>
    * Set of Members that joined this service. As of Coherence 3.7.1, for all
    * services except the ClusterService (service zero) this set is not the
    * same ServiceMemberSet that is held by the ClusterService in the
    * corresponding ServiceInfo.
     */
    public com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet getServiceMemberSet()
        {
        return __m_ServiceMemberSet;
        }
    
    // Accessor for the property "ServiceOldestMember"
    /**
     * Getter for property ServiceOldestMember.<p>
    * Returns the Member object for the senior Member, or null if this Member
    * has not yet joined the cluster.
     */
    public com.tangosol.coherence.component.net.Member getServiceOldestMember()
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        
        ServiceMemberSet setMember = getServiceMemberSet();
        
        // ServiceMemberSet is null until the ServiceJoiningRequest poll is closed
        return setMember == null ? null : setMember.getOldestMember();
        }
    
    // Accessor for the property "ServiceVersion"
    /**
     * Getter for property ServiceVersion.<p>
    * The version string for this Service implementation. This property is
    * currently used *only* on the ClusterService and is left here only for the
    * backward compatibility.
     */
    public String getServiceVersion()
        {
        return "";
        }
    
    // Declared at the super level
    /**
     * Getter for property StartupTimeout.<p>
    * The time (in millis) that limits the waiting period during the service
    * startup sequence. Non-positive numbers indicate no timeout.
     */
    public long getStartupTimeout()
        {
        // import com.tangosol.coherence.config.Config;
        
        long cMillis = super.getStartupTimeout();
        if (cMillis <= 0L)
            {
            cMillis = Config.getLong("coherence.service." + getServiceName() + ".startup.timeout",
                                     Config.getLong("coherence.service.startup.timeout",
                                                    Config.getLong("coherence.service.startuptimeout", 0L).longValue()).longValue()).longValue(); // keep it for backwards compatibility
        
            if (cMillis <= 0L)
                {
                cMillis = getClusterService().getDeliveryTimeoutMillis();
                }
        
            setStartupTimeout(cMillis);
            }
        
        return cMillis;
        }
    
    // Accessor for the property "StatsPollCount"
    /**
     * Getter for property StatsPollCount.<p>
    * Statistics: total number of Polls processed.
     */
    public long getStatsPollCount()
        {
        return __m_StatsPollCount;
        }
    
    // Accessor for the property "StatsPollDuration"
    /**
     * Getter for property StatsPollDuration.<p>
    * Statistics: total duration (in milliseconds) taken to process Polls.
     */
    public long getStatsPollDuration()
        {
        return __m_StatsPollDuration;
        }
    
    // Accessor for the property "StatsPollMaxDuration"
    /**
     * Getter for property StatsPollMaxDuration.<p>
    * Statistics: maximum duration (in milliseconds) taken to process a single
    * Poll.
     */
    public long getStatsPollMaxDuration()
        {
        return __m_StatsPollMaxDuration;
        }
    
    // Accessor for the property "StatsSent"
    /**
     * Getter for property StatsSent.<p>
    * Statistics: total number of sent messages.
     */
    public long getStatsSent()
        {
        return __m_StatsSent;
        }
    
    // Accessor for the property "StatsSentLocal"
    /**
     * Getter for property StatsSentLocal.<p>
    * Statistics: total number of self-addressed messages.
     */
    public long getStatsSentLocal()
        {
        return __m_StatsSentLocal;
        }
    
    // Accessor for the property "StatsTimeoutCount"
    /**
     * Getter for property StatsTimeoutCount.<p>
    * The total number of timed-out requests since the last time the statistics
    * were reset.
    * 
    * @volatile
     */
    public long getStatsTimeoutCount()
        {
        return __m_StatsTimeoutCount;
        }
    
    // Accessor for the property "SUID"
    /**
     * Getter for property SUID.<p>
    * Provides a long value that is unique within this service for the duration
    * of the life of the cluster, and which will almost certainly remain unique
    * (other than system clock discrepancy issues) even if the cluster and
    * service are shut down then restarted.
    * 
    * The structure of the SUID is such that it's always possible to know what
    * Member is the SUID originator. There are two helper methods that are
    * associated with SUID generation:
    *   - long getBaseSUID(int nMember)
    *   - int getMemberId(long lSUID)
    * 
    * As of Coherence 3.2, the SUID is implemented using the AtomicCounter and
    * does not cause any synchronization.
    * 
    * As of Coherence 12.1.3, SUIDs are divided into multiple "domains", and
    * IDs are only guarenteed to be unique within the given domain.  This
    * mechanism allows for alternate SUID generation logic, which in some cases
    * (RequestInfo) can result in dramatically reduced contention.
     */
    public long getSUID(int nDomain)
        {
        return getSUIDCounter(nDomain).incrementAndGet();
        }
    
    // Accessor for the property "SUIDCounter"
    /**
     * Getter for property SUIDCounter.<p>
    * An AtomicCounter object that is used for SUID related calculations.
     */
    protected java.util.concurrent.atomic.AtomicLong[] getSUIDCounter()
        {
        return __m_SUIDCounter;
        }
    
    // Accessor for the property "SUIDCounter"
    /**
     * Getter for property SUIDCounter.<p>
    * An AtomicCounter object that is used for SUID related calculations.
     */
    protected java.util.concurrent.atomic.AtomicLong getSUIDCounter(int i)
        {
        return getSUIDCounter()[i];
        }
    
    public long getSUIDRange(int nDomain, int nWidth)
        {
        _assert(nWidth > 0);
        return getSUIDCounter(nDomain).getAndAdd(nWidth) + 1L;
        }
    
    // Accessor for the property "SuspendPollLimit"
    /**
     * Getter for property SuspendPollLimit.<p>
    * The poll id which represents the maximum allowable poll which can be sent
    * currently.  A message with a greater poll id must not be sent until this
    * value increases making the send allowable.  Each time the value increases
    * the AtomicLong will also be notified, allowing for threads to wait on
    * this value.
     */
    public java.util.concurrent.atomic.AtomicLong getSuspendPollLimit()
        {
        return __m_SuspendPollLimit;
        }
    
    // Accessor for the property "ThisMember"
    /**
     * Getter for property ThisMember.<p>
    * Returns the Member object for this Member, or null if this Member has not
    * yet joined the cluster.
     */
    public com.tangosol.coherence.component.net.Member getThisMember()
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        
        MasterMemberSet setMembers = getClusterMemberSet();
        return setMembers == null ? null : setMembers.getThisMember();
        }
    
    // Accessor for the property "ThisMemberConfigMap"
    /**
     * Getter for property ThisMemberConfigMap.<p>
    * Returns a map of configuration data local to the Service and specific to
    * this Member, or null if this Member has not yet joined the cluster.
     */
    public com.tangosol.util.ObservableMap getThisMemberConfigMap()
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        
        Member memberThis = getThisMember();
        if (memberThis == null)
            {
            return null;
            }
        
        ServiceMemberSet setMemberService = getServiceMemberSet();
        
        // ServiceMemberSet is null until the ServiceJoiningRequest poll is closed
        return setMemberService == null ?
            getClusterService().getServiceInfo(getServiceId()).getMemberSet().ensureMemberConfigMap(memberThis.getId()) :
            setMemberService.ensureMemberConfigMap(memberThis.getId());
        }
    
    // Declared at the super level
    /**
     * Getter for property ThreadName.<p>
    * Specifies the name of the daemon thread. If not specified, the component
    * name will be used.
    * 
    * This property can be set at design time or runtime. If set at runtime,
    * this property must be configured before start() is invoked to cause the
    * daemon thread to have the specified name.
     */
    public String getThreadName()
        {
        String sServiceType = getServiceType();
        String sServiceName = getServiceName();
        
        return sServiceName.equals(sServiceType) ? sServiceName :
             sServiceType + ':' + sServiceName;
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    // Accessor for the property "UserContext"
    /**
     * Getter for property UserContext.<p>
    * User context object associated with this Service.
     */
    public Object getUserContext()
        {
        return __m_UserContext;
        }
    
    // Declared at the super level
    /**
     * Getter for property WaitMillis.<p>
    * The number of milliseconds that the daemon will wait for notification.
    * Zero means to wait indefinitely. Negative value means to skip waiting
    * altogether.
    * 
    * @see #onWait
     */
    public long getWaitMillis()
        {
        // import com.tangosol.util.Base;
        
        long cWait = super.getWaitMillis();
        
        if (isGuardian())
            {
            // A Guardian must wake up on a regular intervals to manage its Guardables
            long ldtNow       = Base.getLastSafeTimeMillis();
            long cGuardMillis = Math.max(1L, getGuardSupport().getNextCheckTime() - ldtNow);
        
            cWait = cWait == 0 ? cGuardMillis : Math.min(cWait, cGuardMillis);
            }
        else if (isGuarded())
            {
            // A guarded Daemon must wake up periodically (at least once a second)
            cWait = cWait == 0 ? 1000L : Math.min(cWait, 1000L);
            }
        
        return cWait == 0L ? 1000L : cWait;
        }
    
    // Accessor for the property "WrapperStreamFactoryAllList"
    /**
     * Getter for property WrapperStreamFactoryAllList.<p>
    * All filters that should be applied during onNotify.
     */
    public java.util.List getWrapperStreamFactoryAllList()
        {
        return __m_WrapperStreamFactoryAllList;
        }
    
    // Accessor for the property "WrapperStreamFactoryList"
    /**
     * Getter for property WrapperStreamFactoryList.<p>
    * List of WrapperStreamFactory objects that affect how Messages sent from/
    * to this specific Service are written and read.
     */
    public java.util.List getWrapperStreamFactoryList()
        {
        return __m_WrapperStreamFactoryList;
        }
    
    // Declared at the super level
    public com.tangosol.net.Guardian.GuardContext guard(com.tangosol.net.Guardable guardable, long cMillis, float flPctRecover)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid$Guard as Grid.Guard;
        
        // wrap the guardable with a service-aware wrapper
        if (guardable instanceof Grid.Guard)
            {
            // the guardable is from a (Grid) service itself; instantiate a wrapper
            // that associates it with itself.
            //
            // For example, ClusterService guarding PartitionedService should use
            // a wrapper guardable that is associated with the PartitionedService.
            Grid service = (Grid) ((Grid.Guard) guardable).get_Parent();
            guardable = service.instantiateWrapperGuardable(guardable);
            }
        else
            {
            // the guardable is not from a (Grid) service; instantiate a wrapper
            // that associates it with this service
            //
            // For example, PartitionedService guarding a worker thread should use
            // a wrapper guardable that is associated with the PartitionedService.
            guardable = instantiateWrapperGuardable(guardable);
            }
        
        return super.guard(guardable, cMillis, flPctRecover);
        }
    
    /**
     * Initialized the SID related data.
     */
    protected void initializeSUID()
        {
        // import java.util.concurrent.atomic.AtomicLong;
        
        int  nMember = getThisMember().getId();
        long lMillis = getClusterTime();
        long lFirst  = (((long) nMember) << 48) | (lMillis & 0xFFFFFFFFFFFFL);
        
        AtomicLong[] aCounter = new AtomicLong[getSUIDCounterLength()];
        for (int i = 0, c = aCounter.length; i < c; ++i)
            {
            AtomicLong counter = new AtomicLong(lFirst);
            aCounter[i] = counter;
            }
        setSUIDCounter(aCounter);
        setBaseSUIDThisMember(Long.valueOf(lFirst));
        setOldestPendingRequestSUIDCounter(new AtomicLong(lFirst + 1L));
        }
    
    /**
     * Bind this service to a transport: datagram (using Publisher) or a
    * MessageBus (using MessageHandler). Called on the service thread.
     */
    public void initializeTransport()
        {
        // import Component.Net.Cluster;
        // import Component.Net.MemberSet;
        // import Component.Net.MessageHandler;
        // import com.tangosol.internal.net.service.grid.GridDependencies;
        // import com.oracle.coherence.common.net.exabus.MessageBus;
        
        // it's possible that transport initialization is called multiple times
        // (if the senior node departs during the service joining handshake);
        // in that case we need to close the "old" bus, but *only* after the new bus
        // is created (to avoid a probability of the bus reusing the same EndPoint)
        // Note: if the configured transport is fully specified (down to a non-ephemeral port),
        //       the second attempt to call createMessageBus() will fail
        
        Cluster cluster    = (Cluster) getCluster();
        String  sTransport = ((GridDependencies) getDependencies()).getReliableTransport();
        
        if (Cluster.TRANSPORT_DATAGRAM.equals(sTransport))
            {
            // explicit datagram, bypassing any shared bus
            // Note: this is outbound only, other nodes may still send to us over a shared bus
            setMessagePublisher(cluster.getPublisher());
            }
        else
            {
            MessageHandler handler = getMessageHandler();
            MessageBus     bus     = cluster.createMessageBus(getServiceType(), sTransport);
            if (handler != null)
                {
                _trace("Service " + getServiceName() + " unbound from " + handler.getMessageBus().getLocalEndPoint(), 3);
        
                setMessageHandler(null); // prevent double close
        
                handler.close();
                }
        
            if (bus == null)
                {
                // shared cluster transport
                setMessagePublisher(cluster.getMessagePublisher());
                }
            else
                {
                handler = instantiateMessageHandler();
        
                handler.initialize(this, bus, cluster.getMessagePublisher());
        
                setMessagePublisher(handler);
                setMessageHandler(handler);
        
                setDepartingMembers(new MemberSet()); // used to throttle the MemberWelcome protocol
        
                _trace("Service " + getServiceName() + " is bound to " + bus.getLocalEndPoint(), 5);
                }
            }
        }
    
    /**
     * Initialize the service config for this member.
    * 
    * @return  the config element to put in the service config map.
     */
    protected com.tangosol.run.xml.XmlElement initServiceConfig()
        {
        // import com.tangosol.run.xml.SimpleElement;
        
        return new SimpleElement("config");
        }
    
    public com.tangosol.coherence.component.net.Message instantiateMessage(int nMsgType)
        {
        // import Component.Net.Message;
        // import com.tangosol.util.Base;
        
        Class clz = getMessageClass(nMsgType);
        if (clz == null)
            {
            throw new RuntimeException("Service " + getServiceName()
                + " was unable to instantiate MessageType=" + nMsgType);
            }
        
        try
            {
            Message msg = (Message) clz.newInstance();
            msg.setService(this);
            return msg;
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    public com.tangosol.coherence.component.net.Message instantiateMessage(String sMsgName)
        {
        // import Component.Net.Message;
        // import com.tangosol.util.Base;
        
        Class clz = getMessageClass(sMsgName);
        if (clz == null)
            {
            throw new IllegalArgumentException("Unable to instantiate Message \""
                + sMsgName + '"');
            }
        
        try
            {
            Message msg = (Message) clz.newInstance();
            msg.setService(this);
            return msg;
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    /**
     * Instantiate a MessageHandler.
     */
    protected com.tangosol.coherence.component.net.MessageHandler instantiateMessageHandler()
        {
        // import Component.Net.MessageHandler;
        
        return new MessageHandler();
        }
    
    // Declared at the super level
    /**
     * Create the queue for this Service.
     */
    protected com.tangosol.coherence.component.util.Queue instantiateQueue()
        {
        return (Grid.ReceiveQueue) _findChild("ReceiveQueue");
        }
    
    /**
     * Instantiate a new un-initialized RequestContext.
     */
    public com.tangosol.coherence.component.net.RequestContext instantiateRequestContext()
        {
        // import Component.Net.RequestContext;
        
        return new RequestContext();
        }
    
    /**
     * Instantiate a WrapperGuardable that associates the specified guardable
    * with this Service.
    * WrapperGuardable is used in conjunction with the configured
    * ServiceFailurePolicy to allow policies to provide custom logic to control
    * guardable recovery, termination, and service-termination failure.
    * 
    * @param guardable  the guardable to wrap
    * 
    * @return a WrapperGuardable that associates the guardable with this Service
     */
    public Grid.WrapperGuardable instantiateWrapperGuardable(com.tangosol.net.Guardable guardable)
        {
        Grid.WrapperGuardable wrapper = (Grid.WrapperGuardable) _newChild("WrapperGuardable");
        
        wrapper.setGuardable(guardable);
        return wrapper;
        }
    
    // Accessor for the property "AcceptingOthers"
    /**
     * Getter for property AcceptingOthers.<p>
    * Set to true when the Service has advanced to the state at which it can
    * accept requests from other Members.
    * 
    * Note: Services should not design this property (ClusterService is the
    * only permissible exception).
     */
    public boolean isAcceptingOthers()
        {
        return __m_AcceptingOthers;
        }
    
    /**
     * Check if the current thread is one of the Cluster threads, i.e. any
    * Service or TCMP thread.
    * 
    * @param fStrict  if true, then true is returned only when the calling
    * thread is a member of the cluster thread group; otherwise true is
    * returned only when the cluster thread group is an ancestor of the calling
    * threads group
     */
    public boolean isClusterThread(boolean fStrict)
        {
        Thread threadService = getThread();
        if (threadService == null)
            {
            return false;
            }
        
        ThreadGroup groupCluster = threadService.getThreadGroup();
        ThreadGroup groupThis    = Thread.currentThread().getThreadGroup();
        
        return fStrict ? groupCluster == groupThis : groupCluster.parentOf(groupThis);
        }
    
    /**
     * Check if the current thread is a coherence thread.
     */
    public boolean isCoherenceThread()
        {
        // import com.tangosol.net.GuardSupport;
        
        return GuardSupport.getThreadContext() != null;
        }
    
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
        return getServiceType().equals(sType);
        }
    
    // Accessor for the property "InWait"
    /**
     * Getter for property InWait.<p>
    * True iff the service thread is waiting, i.e. in onWait()
    * 
    * @volatile - read on threads adding to the service's queue
     */
    public boolean isInWait()
        {
        return __m_InWait;
        }
    
    // Accessor for the property "ProtocolFiltered"
    /**
     * Getter for property ProtocolFiltered.<p>
    * True iff TCMP protocol filters have been configured.
     */
    public boolean isProtocolFiltered()
        {
        return getWrapperStreamFactoryAllList() != null ||
               getWrapperStreamFactoryList() != null;
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    // Accessor for the property "Suspended"
    /**
     * Getter for property Suspended.<p>
     */
    public boolean isSuspended()
        {
        return getSuspendPollLimit().get() != Long.MAX_VALUE;
        }
    
    // Accessor for the property "SuspendedFully"
    /**
     * Getter for property SuspendedFully.<p>
    * As opposed to the Suspended this property indicates if the service has
    * finished the process of suspending itself, i.e. it has ensured that all
    * in-flight operations have completed.   Note this only refers to
    * operations which are "owned" by this service instance as we cannot
    * locally prevent new operations coming in from the outside and must still
    * honor them even while fully suspended in order to let our peers also
    * fully suspend.  Thus from a clustered perspective a service is not
    * finished suspending until all its members have become fully suspended.
    * 
    * This property has no specific meaning if the service is not suspend.
     */
    public boolean isSuspendedFully()
        {
        // import Component.Net.Message.RequestMessage;
        // import Component.Net.Poll;
        
        long lLast = getSuspendPollLimit().get(); // last poll which may be suspendable and open
        if (lLast < Long.MAX_VALUE)
            {
            // ensure that all outstanding polls are not suspendable
            Grid.PollArray aPoll = getPollArray();
            for (long lNext = aPoll.getFirstIndex(); lNext <= lLast; ++lNext)
                {
                Poll poll = (Poll) aPoll.get(lNext);
                if (poll != null)
                    {
                    RequestMessage msg = (RequestMessage) poll.get_Parent();
                    if (msg != null && msg.isSuspendable())
                        {
                        // there is an outstanding suspendable operation
                        return false;
                        }
                    }
                }
        
            return true;
            }
        
        return false;
        }
    
    /**
     * Check whether the members of this service run a version that is greater than
     * or equal to the specified version.
     *
     * @param nMajor     the major version number
     * @param nMinor     the minor version number
     * @param nMicro     the micro version number
     * @param nPatchSet  the patch set version number
     * @param nPatch     the patch version number
     *
     * @return {@code true} if the members of the service are all running a version that
     *         is greater than or equal to the specified version
     */
    public boolean isVersionCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nMajor, nMinor, nMicro, nPatchSet, nPatch);
        return isVersionCompatible(nEncoded);
        }

    /**
     * Check whether the members of this service run a version that is greater than
     * or equal to the specified version.
     *
     * @param nYear   the year version number
     * @param nMonth  the month version number
     * @param nPatch  the patch version number
     *
     * @return {@code true} if the members of the service are all running a version that
     *         is greater than or equal to the specified version
     */
    public boolean isVersionCompatible(int nYear, int nMonth, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nYear, nMonth, nPatch);
        return isVersionCompatible(nEncoded);
        }

    /**
     * Check whether the members of this service run a version that is greater than
     * or equal to the specified version.
     *
     * @param nVersion  the encoded version to compare
     *
     * @return {@code true} if the members of the service are all running a version that
     *         is greater than or equal to the specified version
     */
    public boolean isVersionCompatible(int nVersion)
        {
        MasterMemberSet memberSet = getClusterMemberSet();
        return memberSet != null && memberSet.isVersionCompatible(nVersion);
        }

    /**
     * Check whether the members of this service run a minimum service version
     * that matches a specified {@link IntPredicate}.
     *
     * @param predicate  an {@link IntPredicate} to apply to the minimum encoded service version
     *
     * @return {@code true} if the minimum service version matches the predicate
     */
    public boolean isVersionCompatible(IntPredicate predicate)
        {
        MasterMemberSet memberSet = getClusterMemberSet();
        return memberSet != null && memberSet.isVersionCompatible(predicate);
        }

    /**
     * Return the minimum Coherence version being run by members of this service.
     *
     * @return  the minimum Coherence version being run by members of this service,
     *          or zero if the service member set is {@code null}.
     */
    public int getMinimumServiceVersion()
        {
        MasterMemberSet memberSet = getClusterMemberSet();
        return memberSet == null ? 0 : memberSet.getMinimumVersion();
        }

    /**
     * Check whether the specified member run a version that is greater than
     * or equal to the specified one.
     *
     * @param member    the {@link com.tangosol.coherence.component.net.Member} member to check
     * @param nVersion  the encoded version to compare
     */
    public boolean isVersionCompatible(com.tangosol.coherence.component.net.Member member, int nVersion)
        {
        return getClusterMemberSet().getServiceVersionInt(member.getId()) >= nVersion;
        }

    /**
     * Check whether the specified members runs a version that precedes the
    * specified one.
     */
    public boolean isVersionCompatible(com.tangosol.coherence.component.net.Member member, int nYear, int nMonth, int nPatch)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        
        return isVersionCompatible(member, ServiceMemberSet.encodeVersion(nYear, nMonth, nPatch));
        }
    
    /**
     * Check whether the specified members runs a version that precedes the
    * specified one.
     */
    public boolean isVersionCompatible(com.tangosol.coherence.component.net.Member member, int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        
        return isVersionCompatible(member,
            ServiceMemberSet.encodeVersion(nMajor, nMinor, nMicro, nPatchSet, nPatch));
        }
    
    /**
     * Check whether any of the members in the specified MemberSet run a version
    * that precedes the specified one.
     */
    public boolean isVersionCompatible(com.tangosol.coherence.component.net.MemberSet setMembers, int nVersion)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet as com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
        
        com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet setMaster = getClusterMemberSet();
        
        switch (setMembers.size())
            {
            case 0: // no recipients
                return true;
        
            case 1: // common case
                return setMaster.getServiceVersionInt(setMembers.getFirstId()) >= nVersion;
        
            default:
                {
                int[] anMember = setMembers.toIdArray();
                for (int i = 0, c = anMember.length; i < c; i++)
                    {
                    if (setMaster.getServiceVersionInt(anMember[i]) < nVersion)
                        {
                        return false;
                        }
                    }
                return true; 
                }
            }
        }
    
    /**
     * Check whether any of the members in the specified MemberSet run a version
    * that precedes the specified one.
     */
    public boolean isVersionCompatible(com.tangosol.coherence.component.net.MemberSet setMembers, int nYear, int nMonth, int nPatch)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet as com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
        
        return isVersionCompatible(setMembers,
            com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet.encodeVersion(nYear, nMonth, nPatch));
        }
    
    /**
     * Check whether any of the members in the specified MemberSet run a version
    * that precedes the specified one.
     */
    public boolean isVersionCompatible(com.tangosol.coherence.component.net.MemberSet setMembers, int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet as com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
        
        return isVersionCompatible(setMembers,
            com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet.encodeVersion(nMajor, nMinor, nMicro, nPatchSet, nPatch));
        }

    public boolean isPatchCompatible(Member member, int nEncodedVersion)
        {
        return getClusterMemberSet().isPatchCompatible(member.getId(), nEncodedVersion);
        }

    public boolean isPatchCompatible(MemberSet setMembers, int nEncodedVersion)
        {
        if (setMembers.isEmpty())
            {
            return true;
            }

        MasterMemberSet setMaster = getClusterMemberSet();
        int[]           anId      = setMembers.toIdArray();

        for (int i = 0; i < anId.length; i++)
            {
            if (anId[i] == 0)
                {
                continue;
                }
            if (!setMaster.isPatchCompatible(anId[i], nEncodedVersion))
                {
                return false;
                }
            }

        return true;
        }

    /**
     * Check whether the specified member is welcomed by the local member.
     */
    public boolean isWelcomedBy(com.tangosol.coherence.component.net.Member member)
        {
        return(getServiceMemberSet().isMemberConfigured(member.getId()));
        }
    
    /**
     * Notify the ClusterService (and other Members of this service) that this
    * member has finished starting
    * and has fully joined the service.
    * 
    * Called on the service thread only.
     */
    protected void notifyServiceJoined()
        {
        // import Component.Net.Member;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService;
        
        // do the final service notification
        ClusterService clusterService = getClusterService();
        if (clusterService != this)
            {
            // The service has really finished starting
            int nIdThis = getThisMember().getId();
            getServiceMemberSet().setServiceJoined(nIdThis);
        
            // notify all service members of the join
            Grid.MemberJoined msg = (Grid.MemberJoined) instantiateMessage("MemberJoined");
            msg.setMemberId(nIdThis);
            msg.setToMemberSet(getOthersMemberSet());
            send(msg);
        
            // notify all cluster members of the join
            clusterService.doServiceJoined(this);
            }
        }
    
    /**
     * IOException is thrown during processing configuration updates from the
    * specified member.
     */
    public void onConfigIOException(java.io.IOException e, com.tangosol.coherence.component.net.Member member)
        {
        StringBuffer sb = new StringBuffer();
        
        if (getServiceId() == 0)
            {
            sb.append("Failed to deserialize the config Message received from member ")
              .append(member.getId())
              .append(". This member is configured with the following serializer: ")
              .append(getSerializer())
              .append(", which may be incompatible with the serializer configured by the sender.");
            }
        else
            {
            sb.append("The service \"" + getServiceName() + '"');
        
            String sSerializerThat = (String) getServiceMemberSet().
                    getMemberConfigMap(member.getId()).get("Serializer");
            sb.append(" is configured to use serializer ")
              .append(getSerializer())
              .append(", which appears to be different from ")
              .append(sSerializerThat == null ? "the serializer" :           // we don't know what it is
                      sSerializerThat.length() == 0 ? "default serializer" : // we know it's a default one
                      sSerializerThat)
              .append(" used by ")
              .append(member)
              .append('.');
            }
        
        if (member == getServiceOldestMember())
            {
            sb.append('\n')
              .append(getStackTrace(e))
              .append("Stopping the ")
              .append(getServiceName())
              .append(" service.");
            _trace(sb.toString(), 1);
            stop();
            }
        else
            {
            _trace(sb.toString(), 3);
        
            // the other node should terminate itself in a bit when receives
            // undecipherable messages from the senior (e.g. ConfigSync);
            // the only downside of not doing anything about this exception
            // is that the log could be seen a number of times (once per
            // configuration element), so we log it at a very high level
            }
        }
    
    // Declared at the super level
    /**
     * This event occurs when dependencies are injected into the component. 
    * First, call super.onDependencies to allow all super components to process
    * the Dependencies.  Each component is free to chose how it consumes
    * dependencies from within onDependencies.  Often (though not ideal), the 
    * dependencies are copied into the component's properties.  This technique
    * isolates Dependency Injection from the rest of the component code since
    * components continue to access properties just as they did before. 
    * 
    * PartitionedCacheDependencies deps = (PartitionedCacheDependencies)
    * getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import com.tangosol.coherence.config.builder.ParameterizedBuilder;
        // import com.tangosol.coherence.config.builder.ServiceFailurePolicyBuilder;
        // import com.tangosol.internal.net.service.grid.GridDependencies;
        // import com.tangosol.net.ActionPolicy;
        // import com.tangosol.net.GuardSupport;
        // import com.tangosol.net.MemberListener;
        // import java.util.Iterator;
        // import java.util.List;
        
        super.onDependencies(deps);
        
        GridDependencies depsGrid = (GridDependencies) deps;
        
        ActionPolicy policyAction = (ActionPolicy) depsGrid.getActionPolicyBuilder().realize(createResolver(), getContextClassLoader(), null);
        setActionPolicy(policyAction);
        setRequestTimeout(depsGrid.getRequestTimeoutMillis());
        
        long cTimeoutMillis = depsGrid.getDefaultGuardTimeoutMillis();
        if (cTimeoutMillis > 0)
            {
            // default member-wide guardian timeout is overridden for this
            // service; reset the default guard timeout for this service and
            // its dependent threads (e.g. worker, write-behind)
            //
            // Note: service-startup is guarded by the cluster-service's
            // guardian SLA.  Only after the service is started, is the SLA
            // for the service thread updated.
            cTimeoutMillis = Math.max(cTimeoutMillis, GuardSupport.GUARDIAN_MAX_CHECK_INTERVAL);
        
            setDefaultGuardTimeout(cTimeoutMillis);
            }
        
        // unless the ServiceFailurePolicyBuilder is explicitly specified for this service,
        // stay with default policy
        ServiceFailurePolicyBuilder builder = depsGrid.getServiceFailurePolicyBuilder();
        if (builder != null)
            {
            setServiceFailurePolicy(builder.realize(createResolver(), getContextClassLoader(), null));
            }
        
        // add all of the member listeners
        List listBuilder = depsGrid.getMemberListenerBuilders();
        if (listBuilder != null)
            {
            for (Iterator iter = listBuilder.iterator(); iter.hasNext(); )
                {
                ParameterizedBuilder bldr = (ParameterizedBuilder) iter.next();
                addMemberListener((MemberListener) bldr.realize(createResolver(), getContextClassLoader(), null));
                }
            }
        }
    
    // Declared at the super level
    /**
     * Event notification called once the daemon's thread starts and before the
    * daemon thread goes into the "wait - perform" loop. Unlike the
    * <code>onInit()</code> event, this method executes on the daemon's thread.
    * 
    * Note1: this method is called while the caller's thread is still waiting
    * for a notification to  "unblock" itself.
    * Note2: any exception thrown by this method will terminate the thread
    * immediately
    * Note3: most commonly, the assumption is that onEnter() method executes
    * while the service state is "SERVICE_INITIAL", so it is important for the
    * subclasses to make the super.onEnter() call at the very end
     */
    protected void onEnter()
        {
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        // import com.tangosol.application.ContainerHelper;
        // import com.tangosol.util.Base;
        // import java.util.HashMap;
        
        // Note: we don't delegate to super.onEnter() here as the ServiceState
        // change is handled via the NotifyStartup message and may be deferred.
        
        setStartTimestamp(Base.getSafeTimeMillis());
        
        resetStats();
        
        // set the thread context dictated by the container
        ContainerHelper.initializeThreadContext(this);
        
        initializeTransport();
        
        // prime the SUID
        if (getServiceId() > 0)
            {
            initializeSUID();
        
            setQueueDeferredWelcome(new com.tangosol.coherence.component.util.Queue());
        
            setServiceLeftActions(new HashMap());
        
            // at the moment, this is only done to prevent starting service nodes
            // with different serializers (COH-2186)
            // see onConfigIOException
            getThisMemberConfigMap().put("Serializer", ensureSerializer().getClass().getName());
            }
        
        // initialize the action policy upfront as internal msgs
        // may invoke ActionPolicy.isAllowed()
        getActionPolicy().init(this);
        
        // now change the service state from INITIAL to STARTING
        // (see Note3 of the doc)
        setServiceState(SERVICE_STARTING);
        post(instantiateMessage("NotifyStartup"));
        }
    
    // Declared at the super level
    /**
     * This event occurs when an exception is thrown from onEnter, onWait,
    * onNotify and onExit.
    * 
    * If the exception should terminate the daemon, call stop(). The default
    * implementation prints debugging information and terminates the daemon.
    * 
    * @param e  the Throwable object (a RuntimeException or an Error)
    * 
    * @throws RuntimeException may be thrown; will terminate the daemon
    * @throws Error may be thrown; will terminate the daemon
     */
    public void onException(Throwable e)
        {
        // import com.tangosol.internal.tracing.TracingHelper;
        
        TracingHelper.augmentSpanWithErrorDetails(TracingHelper.getActiveSpan(), true, e);
        
        super.onException(e);
        }
    
    // Declared at the super level
    /**
     * Event notification called right before the daemon thread terminates. This
    * method is guaranteed to be called only once and on the daemon's thread.
     */
    protected void onExit()
        {
        // import Component.Net.Message;
        // import Component.Net.MessageHandler;
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        
        super.onExit();
        
        MessageHandler handler = getMessageHandler();
        if (handler != null)
            {
            handler.close();
            }
        
        // drain the Message queue to dispose resources
        com.tangosol.coherence.component.util.Queue queue = getQueue();
        while (true)
            {
            Message msg = (Message) queue.removeNoWait();
            if (msg == null)
                {
                break;
                }
        
            if (msg.isDeserializationRequired())
                {
                msg.releaseIncoming();
                }
            }
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
        // import Component.Net.Message;
        // import Component.Net.Poll;
        // import java.util.concurrent.ConcurrentHashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        super.onInit();
        
        setCompletedRequestSUIDs(new ConcurrentHashMap(/*initialCapacity*/  16,
                                                       /*loadFactor*/       0.75f,
                                                       /*concurrencyLevel*/ Runtime.getRuntime().availableProcessors() * 3));
        
        // go through all static children and register all those that
        // are derived from Message
        Map   map        = get_ChildClasses();
        Class clzMessage = Message.class;
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
            {
            java.util.Map.Entry  entry    = (java.util.Map.Entry)  iter.next();
            String sMsgName = (String) entry.getKey();
            Class  clz      = (Class)  entry.getValue();
            if (clzMessage.isAssignableFrom(clz))
                {
                registerMessageType(sMsgName, clz);
                }
            }
        
        // initialize poll windowed-array to skip index zero
        Grid.PollArray waPoll = (Grid.PollArray) _findChild("PollArray");
        setPollArray(waPoll);
        waPoll.remove(waPoll.add(new Poll()));
        
        getSuspendPollLimit().set(Long.MAX_VALUE);
        }
    
    // Declared at the super level
    /**
     * Event notification for performing low frequency periodic maintenance
    * tasks.  The interval is dictated by the WaitMillis property, 
    * 
    * This is used for tasks which have a high enough cost that it is not
    * reasonable to perform them on every call to onWait() since it could be
    * called with a high frequency in the presence of work-loads with fast
    * oscillation between onWait() and onNotify().  As an example a single
    * threaded client could produce such a load.
     */
    protected void onInterval()
        {
        // import Component.Net.MessageHandler;
        // import java.util.Collection;
        // import java.util.Iterator;
        
        // check for stuck polls periodically
        getPollArray().checkPolls();
        
        // check for pending quiescences that can be responded to once we're fully suspended
        Collection colQuiescenceResp = getPendingQuiescenceResponses();
        if (colQuiescenceResp != null && isSuspendedFully())
            {
            for (Iterator iter = colQuiescenceResp.iterator(); iter.hasNext(); )
                {
                Grid.NotifyResponse msgResp = (Grid.NotifyResponse) iter.next();
                post(msgResp);
                }
            setPendingQuiescenceResponses(null);
            }
        
        
        MessageHandler handler = getMessageHandler();
        if (handler != null)
            {
            handler.onInterval();
            }
        
        super.onInterval();
        }
    
    /**
     * This event is invoked when a Message is posted on this Service queue.
    * 
    * @see #onNotify
     */
    public void onMessage(com.tangosol.coherence.component.net.Message msg)
        {
        // import Component.Net.Poll;
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.Message.RequestMessage;
        // import com.tangosol.internal.tracing.Scope;
        // import com.tangosol.internal.tracing.Span;
        // import com.tangosol.internal.tracing.TracingHelper;
        // import com.tangosol.run.component.EventDeathException;
        
        if (msg != null)
            {
            try
                {
                if (!validateMessage(msg))
                    {
                    return;
                    }
        
                Member memberFrom = msg.getFromMember();
                if (memberFrom != null)
                    {
                    MemberSet.readBarrier(memberFrom.getId());
                    }
        
                setStatsReceived(getStatsReceived() + 1L);
        
                boolean fTracing = msg instanceof RequestMessage && TracingHelper.isEnabled();
                Span    span     = null;
                Scope   scope    = null;
               
                if (fTracing)
                    {
                    SpanContext  ctx     = msg.getTracingSpanContext();
                    Span.Builder builder = newTracingSpan(msg instanceof Runnable ? "dispatch" : "process", msg)
                            .withMetadata("member.source",
                                          Long.valueOf(memberFrom == null ? -1 : memberFrom.getId()).longValue());

                    if (!TracingHelper.isNoop(ctx))
                        {
                        builder.setParent(ctx);
                        }

                    span  = builder.startSpan();
                    scope = TracingHelper.getTracer().withSpan(span);
                    }
        
                try
                    {
                    msg.onReceived();
                    }
                catch (EventDeathException e)
                    {
                    // a "normal" exception to "get out of" an event
                    if (msg instanceof RequestMessage)
                        {
                        String sMsg = "Processing for RequestMessage has been interrupted:\n" + msg;
                        if (fTracing)
                            {
                            span.log(sMsg);
                            }
                        _trace(sMsg, 2);
                        _trace(getStackTrace(e), 4);
                        }
                    }
                catch (Throwable t)
                    {
                    if (fTracing)
                        {
                        TracingHelper.augmentSpanWithErrorDetails(span, true, t);
                        }
                    throw t;
                    }
                finally
                    {
                    if (fTracing)
                        {
                        scope.close();
                        span.end();
                        }
                    }
        
                Poll poll = msg.getPoll();
                if (poll != null)
                    {
                    poll.onResponse(msg);
                    }
                }
            catch (Throwable e)
                {
                onException(e);
                }
            }
        }
    
    /**
     * This event occurs when an exception is thrown while reading message
    * contents.
    * 
    * If the exception should terminate the daemon, call stop(). The default
    * implementation prints debugging information and terminates the daemon.
    * Returning from this method will result in the partially instantiated
    * message being processed, which should be avoided unless the service is
    * prepared to take this into account.
    * 
    * @param e      the Throwable object (a RuntimeException or an Error)
    * @param msg  the partially constructed message which triggered the
    * exception
    * 
    * @return true if the partial message should be processed, false if it
    * should be skipped.
    * 
    * @throws RuntimeException may be thrown; will terminate the daemon
    * @throws Error may be thrown; will terminate the daemon
     */
    protected boolean onMessageReadException(Throwable e, com.tangosol.coherence.component.net.Message msg)
        {
        // import com.oracle.coherence.common.io.Buffers;
        
        if (e instanceof SecurityException)
            {
            // security exception while reading a message, most likely someone is talking
            // to us who shouldn't be; we assume it was an attacker and don't kill ourselves
            // as this is potentially quite serious we log the message and the full stack
            _trace("SecurityException received while reading " + msg + "\n" +
                getStackTrace(e), 1);
            }
        else
            {
            _trace("An exception (" + e.getClass().getName()
                + "): '" + e.getMessage() + "', occurred reading Message " + msg.get_Name()
                + " Type=" + msg.getMessageType() + " for Service=" + this
                + " from " + msg.getFromMember()
                + " with read buffer=" + Buffers.toString(msg.getReadBuffer().toByteBuffer(), 1024*1024), 1);
            onException(e);
            }
        
        return false; // skip the message
        }
    
    /**
     * This event is invoked by the Publisher when all the Members, to which a
    * Message is marked return-receipt, are either dead or have acknowledged
    * full receipt.
    * 
    * @see #onSent
     */
    public void onMessageReceipt(com.tangosol.coherence.component.net.Message msg)
        {
        if (msg != null)
            {
            if (((Grid.ReceiveQueue) getQueue()).isPreprocessingEnabled() &&
                msg.preprocessSentNotification())
                {
                // the notification has been preprocessed
                }
            else
                {
                Grid.NotifyMessageReceipt msgRecpt = (Grid.NotifyMessageReceipt)
                        instantiateMessage("NotifyMessageReceipt");
                msgRecpt.setNotifyMessage(msg);
                post(msgRecpt);
                }
            }
        }
    
    // Declared at the super level
    /**
     * Event notification to perform a regular daemon activity. To get it
    * called, another thread has to set Notification to true:
    * <code>daemon.setNotification(true);</code>
    * 
    * @see #onWait
     */
    protected void onNotify()
        {
        // import Component.Net.Message;
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        // import com.tangosol.util.Base;
        
        long  lStart        = Base.getLastSafeTimeMillis();
        com.tangosol.coherence.component.util.Queue queueDeferred = getQueueDeferred();
        com.tangosol.coherence.component.util.Queue queueIn       = getQueue();
        
        // limit the number of iterations in the "tight loop" to ensure that
        // onInterval can periodically get called when we are under heavy load
        for (int i = 0; i < 512; i++)
            {
            Message msg = queueDeferred == null
                ? (Message) queueIn.removeNoWait() // common case
                : processDeferredQueue();          // startup/join case
        
            if (msg == null)
                {
                break; // we've drained our queue(s)
                }
            else if (!msg.isDeserializationRequired() || deserializeMessage(msg))
                {
                onMessage(msg);
                }
            }
        
        setStatsCpu(getStatsCpu() + (Base.getSafeTimeMillis() - lStart));
        
        super.onNotify();
        }
    
    /**
     * Called to complete the "service-joined" processing for the specified
    * member.
     */
    public void onNotifyServiceJoined(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import com.tangosol.net.MemberEvent;
        
        int              nMember      = member.getId();
        ServiceMemberSet setMember    = getServiceMemberSet();
        Member           memberOldest = setMember.getOldestMember();
        
        _trace("Member " + nMember + " joined Service " +
                getServiceName() + " with senior member " +
                (memberOldest == null ? "n/a" : String.valueOf(memberOldest.getId())), 3);
        
        setMember.setServiceJoined(nMember);
        
        dispatchMemberEvent(member, MemberEvent.MEMBER_JOINED);
        }
    
    /**
     * Called to complete the "service-left" processing for the specified
    * member.  This notification is processed only after the associated
    * endpoint has been released by the message handler.  See
    * $NotifyServiceLeft#onReceived/#proceed.
    * Called on the service thread only.
     */
    public void onNotifyServiceLeft(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import com.tangosol.net.MemberEvent;
        
        int              nMember      = member.getId();
        ServiceMemberSet setMember    = getServiceMemberSet();
        boolean          fJoining     = setMember.isServiceJoining(nMember);
        Member           memberOldest = getServiceOldestMember();
        
        _trace("Member " + member.getId() +
               (setMember.getState(nMember) == ServiceMemberSet.MEMBER_JOINING
               ? " aborted joining" : " left") +
               " service " + getServiceName() + " with senior member " +
               (memberOldest == null ? "n/a" : String.valueOf(memberOldest.getId())), 3);
        
        // record whether the departed member was a service senior before
        // removing it from the service member set
        boolean fOldest = member == memberOldest;
        
        // to protect the semantic of pre-3.7.1 behavior we need to remove the NotifyMember
        // from the ServiceMemberSet before any other actions are taken
        setMember.remove(member);
        
        // clean up remaining polls to that Member
        closePolls(member);
        
        // notify service config map about the departed member
        getServiceConfigMap().onServiceLeft(member, fOldest);
        
        if (fJoining)
            {
            // the member left before it finished the join protocol sequence;
            // no need to raise the event
            }
        else
            {
            dispatchMemberEvent(member, MemberEvent.MEMBER_LEFT);
            }
        }
    
    /**
     * Called to complete the "service-quiescence" processing for the local
    * member.
    * Called on the service thread only.
    * 
    * @param fResume                     false to suspend this service; true to
    * resume this service
    * @param fResumeOnFailover  whether the service should be automatically
    * resumed on failover
     */
    public void onNotifyServiceQuiescence(boolean fResume, boolean fResumeOnFailover)
        {
        // import java.util.concurrent.atomic.AtomicLong;
        
        AtomicLong atlSuspend = getSuspendPollLimit();
        if (fResume ^ isSuspended())
            {
            // we are already in the "requested" state
            return;
            }
        else if (fResume)
            {
            synchronized (atlSuspend)
                {
                atlSuspend.set(Long.MAX_VALUE);
                atlSuspend.notifyAll();
                }
            }
        else // suspend
            {
            atlSuspend.set(getPollArray().getLastIndex());
            }
        
        _trace("Service " + getServiceName() + " has been " + (fResume ? "resumed" : "suspended"), 5);
        }
    
    public void onPollClosed(com.tangosol.coherence.component.net.Poll poll)
        {
        unregisterPoll(poll);
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
        // NOTE: onServiceStarted() marks the end of the startup sequence
        //       and may require special action by different services.  For
        //       example, PartitionedService overrides this method and does
        //       not delegate here. In the same manner, we will not
        //       delegate to our super either.
        
        // validate that the local service config is compatible with
        // the other service members
        if (validateServiceConfig())
            {
            // attach the service config listener
            getServiceConfigMap().getConfig().attachConfigListener();
        
            notifyServiceJoined();
        
            // open for business
            setAcceptingOthers(true);
            setAcceptingClients(true);
            }
        else
            {
            post(instantiateMessage("NotifyShutdown"));
            }
        }
    
    // Declared at the super level
    /**
     * Called when the Service has transitioned to the specified state.
    * 
    * @param nState the new Service state
     */
    protected void onServiceState(int nState)
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import Component.Net.MessageHandler;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService;
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        // import com.tangosol.net.ServiceInfo;
        // import java.util.Arrays;
        // import java.util.Iterator;
        // import java.util.concurrent.TimeUnit;
        
        // don't call super.onServiceState()
        
        ClusterService clusterservice = getClusterService();
        switch (nState)
            {
            case SERVICE_STARTING:
                onServiceStarting();
                break;
        
            case SERVICE_STARTED:
                {
                if (this == clusterservice)
                    {
                    onServiceStarted();
                    }
                else
                    {
                    // delay a bit if we are already a senior for any service, so someone else can become a service senior
                    // when many members are started at the same time
                    if (clusterservice.getServiceMemberSet().size() > 1)
                        {
                        if (clusterservice.getServiceOldestMember() == getThisMember())
                            {
                            sleep(1000L);
                            }
                        else
                            {
                            Iterator iterCaches = Arrays.stream(clusterservice.getServiceInfo()).iterator();
                            while (iterCaches.hasNext())
                                {
                                ServiceInfo info = (ServiceInfo) iterCaches.next();
                                if (info.getOldestMember() == getThisMember())
                                    {
                                    sleep(1000L);
                                    break;
                                    }
                                }
                            }
                        }
                    clusterservice.doServiceJoining(this);
        
                    Member memberSenior = getServiceOldestMember();
        
                    _trace("Service " + getServiceName() + " joined the cluster with "
                        + "senior service member " + memberSenior.getId(), 3);
        
                    // hook up listener for this Member's config Map so that all
                    // subsequent updates by this service are published
                    getThisMemberConfigMap().addMapListener(
                        (Grid.MemberConfigListener) _findChild("MemberConfigListener"));
        
                    MessageHandler handler = getMessageHandler();
                    if (handler != null)
                        {
                        handler.connectAll();
                        }
        
                     if (getThisMember() == memberSenior)
                         {
                         onServiceStarted();
                         }
                     else
                        {
                        long cDelayMillis = getDelay("join");
        
                        _trace("Starting service " + getServiceName() + (cDelayMillis > 0 ? " in " + cDelayMillis + " ms" : " immediately"), 6);
        
                        Runnable task = (Runnable) _newChild("MemberWelcomeRequestTask");
        
                        if (cDelayMillis > 0)
                            {
                            Grid service = (Grid) get_Module();
                            com.tangosol.coherence.component.util.DaemonPool pool = service.getDaemonPool();
        
                            if (pool.isStarted())
                                {
                                pool.schedule(task, cDelayMillis);
                                }
                            else
                                {
                                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                                scheduler.schedule(task, cDelayMillis, TimeUnit.MILLISECONDS);
                                scheduler.shutdown();
                                }
                            }
                        else
                            {
                            task.run();
                            }
                        }
                    }
                break;
                }
        
            case SERVICE_STOPPING:
                getServiceMemberSet().setServiceLeaving(getThisMember().getId());
                onServiceStopping();
        
                clusterservice.doServiceLeaving(this);
                break;
        
            case SERVICE_STOPPED:
                try
                    {
                    onServiceStopped();
                    closePolls();
                    }
                finally
                    {
                    // no matter what, we must call doServiceLeft() to deregister this service
                    clusterservice.doServiceLeft(this);
                    _trace("Service " + getServiceName() + " left the cluster", 3);
                    }
                break;
            }
        }
    
    // Declared at the super level
    /**
     * Event notification called if the start() sequence could not be completed
    * within the StartupTimeout time.
    * 
    * @see #start
     */
    protected void onStartupTimeout()
        {
        // import com.tangosol.net.RequestTimeoutException;
        
        throw new RequestTimeoutException("Timeout during service start: " + getInfo());
        }
    
    // Declared at the super level
    /**
     * Event notification called when  the daemon's Thread is waiting for work.
    * 
    * @see #run
     */
    protected void onWait()
            throws java.lang.InterruptedException
        {
        flush();
        
        setInWait(true);
        try
            {
            super.onWait();
            setInWait(false);
            }
        catch (InterruptedException e) // in TDE this is cheaper then using finally
            {
            setInWait(false);
            throw e;
            }
        }
    
    /**
     * Send the specified RequestMessages and block the caller till either the
    * poll is closed or the specified time elapses.
    * 
    * @param aMsgs  an array of messages to send
    * @param cMillisTimeout  the amount of time the caller is willing to wait
    * for a poll to complete; zero for indefinite wait
    * 
    * @throws RequestTimeoutException if the poll has not been completed in the
    * specified time interval
     */
    public void poll(com.tangosol.coherence.component.net.message.RequestMessage[] aMsgs, long cMillisTimeout)
        {
        // import Component.Net.Poll;
        // import Component.Net.Message.RequestMessage;
        // import com.oracle.coherence.common.base.Disposable;
        // import com.oracle.coherence.common.io.BufferManagers;
        // import com.oracle.coherence.common.io.BufferSequence;
        // import com.tangosol.io.WriteBuffer;
        // import com.tangosol.util.Base;
        
        _assert(getThread() != Thread.currentThread(),
            "this is a blocking operation and cannot be called on the Service thread");
        
        // block until the Service is ready
        waitAcceptingClients();
        
        long ldtStart    = cMillisTimeout == 0L ? 0L : Base.getSafeTimeMillis();
        long cMillisWait = cMillisTimeout;
        int  cMsg        = aMsgs.length;
        try
            {
            Poll pollMax    = null;
            long lPollIdMax = 0L;
            for (int i = 0; i < cMsg; ++i)
                {
                RequestMessage msg  = aMsgs[i];
                if (msg.isSuspendable())
                    {
                    Poll poll    = msg.ensureRequestPoll();
                    long lPollId = poll.getPollId();
        
                    if (lPollId > lPollIdMax)
                        {
                        lPollIdMax = lPollId;
                        pollMax    = poll;
                        }
                    }
                }
        
            if (lPollIdMax != 0L && lPollIdMax > getSuspendPollLimit().get())
                {
                // the service suspended before this poll was registered
                // wait for resume
                cMillisWait = pollMax.waitServiceResume(ldtStart, cMillisWait);
                }
        
            long cbPending  = 0L;
            int  nLast      = -1;
        
            for (int i = 0; i < cMsg; i++)
                {
                RequestMessage msg  = aMsgs[i];
                cbPending = post(msg, cbPending);
        
                if (cbPending == -1L)  // the last call to post resulted in a flush
                    {
                    // keep this thread from producing any new work if the system is backlogged
                    for (int j = nLast + 1; j <= i; j++)
                        {
                        cMillisWait = drainOverflow(aMsgs[j].getToMemberSet(), cMillisWait);
                        }
        
                    cbPending = 0L;
                    nLast = i;
                    }
                }
        
            flush();
        
            // keep this thread from producing any new work if the system is backlogged
            // drain between send and poll to hide any cost associated with this call
            // when there is no backlog.
            for (int i = nLast + 1; i < cMsg; i++)
                {
                // keep this thread from producing any new work if the system is backlogged
                cMillisWait = drainOverflow(aMsgs[i].getToMemberSet(), cMillisWait);
                }
        
            // wait on polls in reverse order to encourage most polls completing
            // before calling into waitCompletion, thus avoiding poll synchronization
            for (int i = cMsg - 1; i >= 0; i--)
                {
                aMsgs[i].getRequestPoll().waitCompletion(ldtStart, cMillisTimeout);
                }
            
            aMsgs = null;
            }
        catch (InterruptedException e)
            {
            if (isCoherenceThread())
                {
                // ensure the current thread's interrupted status is cleared if 
                // this is a coherence thread; also heartbeat so that this thread can
                // transition back to a healthy state and avoid further interrupts
        
                heartbeat();
        
                Thread.interrupted();
                }
            else
                {
                Thread.currentThread().interrupt();
                }
        
            throw processPollInterrupt(aMsgs, e);
            }
        finally
            {
            if (aMsgs != null)
                {
                for (int i = 0; i < cMsg; i++)
                    {
                    doPollClose(aMsgs[i].ensureRequestPoll()); // close the polls on the service thread
                    }
                }
            }
        }
    
    /**
     * Send the specified RequestMessages and block the caller till either the
    * poll is closed or the specified time elapses.
    * 
    * @param colMsgs  a collection of messages to send
    * @param cMillisTimeout  the amount of time the caller is willing to wait
    * for a poll to complete; zero for indefinite wait
    * 
    * @throws RequestTimeoutException if the poll has not been completed in the
    * specified time interval
     */
    public void poll(java.util.Collection colMsgs, long cMillisTimeout)
        {
        // import Component.Net.Message.RequestMessage;
        // import java.util.Collection;
        // import java.util.Iterator;
        
        if (colMsgs != null && !colMsgs.isEmpty())
            {
            RequestMessage[] aMsgs = new RequestMessage[colMsgs.size()];
            int i = 0;
            for (Iterator it = colMsgs.iterator(); it.hasNext(); )
                {
                aMsgs[i++] = (RequestMessage) it.next();
                }
        
            poll(aMsgs, cMillisTimeout);
            }
        }
    
    /**
     * Send the specified RequestMessage and block the caller till the poll is
    * closed.
     */
    public Object poll(com.tangosol.coherence.component.net.message.RequestMessage msg)
        {
        return poll(msg, msg.checkTimeoutRemaining());
        }
    
    /**
     * Send the specified RequestMessage and block the caller till the either
    * poll is closed or the specified time elapses.
    * 
    * @param cMillisTimeout  the amount of time the caller is willing to wait
    * for a poll to complete; zero for indefinite wait
    * 
    * @throws RequestTimeoutException if the poll has not been completed in the
    * specified time interval
     */
    public Object poll(com.tangosol.coherence.component.net.message.RequestMessage msg, long cMillisTimeout)
        {
        // import Component.Net.Poll;
        // import Component.Net.Message.RequestMessage;
        // import com.tangosol.util.Base;
        
        _assert(getThread() != Thread.currentThread(),
            "this is a blocking operation and cannot be called on the Service thread");
        
        // block until the Service is ready
        waitAcceptingClients();
        
        long ldtStart    = cMillisTimeout == 0L ? 0L : Base.getSafeTimeMillis();
        long cMillisWait = cMillisTimeout;
        Poll poll        = msg.ensureRequestPoll();
        
        try
            {
            if (msg.isSuspendable())
                {
                if (poll.getPollId() > getSuspendPollLimit().get())
                    {
                    // the service suspended before this poll was registered
                    // wait for resume
                    cMillisWait = poll.waitServiceResume(ldtStart, cMillisWait);
                    }
                }
            
            send(msg);
        
            // keep this thread from producing any new work if the system is backlogged
            // drain between send and poll to hide any cost associated with this call
            // when there is no backlog.
            drainOverflow(msg.getToMemberSet(), cMillisWait);
        
            return poll.waitCompletion(ldtStart, cMillisTimeout);
            }
        catch (InterruptedException e)
            {
            if (isCoherenceThread())
                {
                // ensure the current thread's interrupted status is cleared if 
                // this is a coherence thread; also heartbeat so that this thread can
                // transition back to a healthy state and avoid further interrupts
        
                heartbeat();
        
                Thread.interrupted();
                }
            else
                {
                Thread.currentThread().interrupt();
                }
        
            throw processPollInterrupt(new RequestMessage[]{msg}, e);
            }
        finally
            {
            doPollClose(poll); // close the poll on the service thread
            }
        }
    
    /**
     * Asynchronously send the message.  The actual transmission of the message
    * may be deferred due to the send queue batching.  To ensure that the
    * message will eventually be processed a call to flush() must eventually
    * follow this call.  The general usage pattern is to perform a series of
    * post(msg) operations followed by a single flush() operation before
    * entering a blocking state.
    * 
    * @since Coherence 3.3
    * 
    * @see flush()
     */
    public void post(com.tangosol.coherence.component.net.Message msg)
        {
        msg.setService(this);
        msg.post(); // ask the message to post itself
        }
    
    /**
     * Asynchronously send the message, flushing automatically if the bytes
    * pending count exceeds a certain threshold. 
    * 
    * This method is useful when calling post multiple times in a tight loop,
    * as it will limit the amount of native memory used by automatically
    * issuing a flush() call if the bytes pending count exceeds the threshold. 
    * 
    * A final flush() call should still be issued after the last call to this
    * method, to ensure that any remaining, unflushed messages are sent.
    * 
    * @param msg             the message to send
    * @param cbPending  the count of unflushed bytes pending <i>before</i> the
    * specified message has been posted
    * 
    * @return the count of unflushed bytes pending <i>after</i> the specified
    * message has been posted, or -1 if the posting of the specified message
    * caused {@link #flush()} to be called.
    * 
    * @since Coherence 23.03
    * 
    * @see flush()
     */
    public long post(com.tangosol.coherence.component.net.Message msg, long cbPending)
        {
        // import com.oracle.coherence.common.base.Disposable;
        // import com.oracle.coherence.common.io.BufferManagers;
        // import com.oracle.coherence.common.io.BufferSequence;
        // import com.tangosol.io.WriteBuffer;
        
        post(msg);
        
        Disposable bufferController = msg.getBufferController();
        long       cbMsg            = bufferController instanceof BufferSequence
                                      ? ((BufferSequence) bufferController).getLength()
                                      : bufferController instanceof WriteBuffer
                                        ? ((WriteBuffer) bufferController).length()
                                        : 0;
                                        
        cbPending += cbMsg;
        
        if (cbPending > Math.max(256 * 1024 * 1024,    // 256 MB
                                 BufferManagers.getDirectManager().getCapacity() / Runtime.getRuntime().availableProcessors()))
            {
            flush();
            cbPending = -1;
            }
        
        return cbPending;
        }
    
    /**
     * Handle the processing of messages during startup.
     */
    protected com.tangosol.coherence.component.net.Message processDeferredQueue()
        {
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        // import Component.Net.Message;
        
        com.tangosol.coherence.component.util.Queue   queueDeferred = getQueueDeferred();
        Message msg           = null;
        if (isAcceptingOthers())
            {
            // the only reason for us to be here now is because we have not finished processing
            // of the deferred queue after starting the service, once empty we set the deferred
            // queue to null and won't come in here again
            if (queueDeferred != null)
                {
                msg = (Message) queueDeferred.removeNoWait();
                if (msg == null)
                    {
                    // we've complete the processing of all deferred items
                    setQueueDeferred(null);
                    }
                }
            }
        else
            {
            // we've yet to complete startup, defer non-internal messages for later processing
            msg = (Message) getQueue().removeNoWait();
            if (msg != null && !msg.isInternal()) // isDeferrable
                {
                // defer the message
                queueDeferred.add(msg);
                msg = null;
                }
            }
        
        return msg;
        }
    
    /**
     * Log the status of open polls and wrap the given InterruptedException
    * 
    * @param aMsgs  array of RequestMessages this thread was waiting for
    * responses from when it was interrupted
    * @param e  the InterruptedException that resulted from this thread's
    * interruption
    * 
    * @return WrapperException containing InterruptedException e and a
    * description containing member ids of members that did not respond to open
    * polls
     */
    protected RuntimeException processPollInterrupt(com.tangosol.coherence.component.net.message.RequestMessage[] aMsgs, InterruptedException e)
        {
        // import Component.Net.Member;
        // import Component.Net.Poll;
        // import com.tangosol.util.WrapperException;
        // import java.util.Map;
        // import java.util.TreeMap;
        
        StringBuilder sbLog   = new StringBuilder();
        Map           mapMbrs = new TreeMap();
        
        sbLog.append("This thread was interrupted while waiting for the results ") 
             .append("of a request:\n");
        
        for (int i = 0, c = aMsgs.length; i < c; ++i)
            {
            Poll poll = aMsgs[i].ensureRequestPoll();
            if (!poll.isClosed())
                {
                sbLog.append(poll.toString());
                sbLog.append(',');
        
                int[] anMbr = poll.getRemainingMemberSet().toIdArray();
                for (int j = 0; j < anMbr.length; j++)
                    {
                    int    nMemberId = anMbr[j];
                    Member member    = getClusterMemberSet().getMember(nMemberId);
                    
                    mapMbrs.put(Integer.valueOf(nMemberId),
                            member == null ? String.valueOf(nMemberId)
                                           : member.toString());
                    }
                }
            }
        
        _trace(sbLog.toString(), 2);
        
        return new WrapperException(e, "Request interrupted while waiting for response from " +
                mapMbrs.values());
        }
    
    /**
     * Process deferred service-left actions.
    * Called on the service thread only.
     */
    public void processServiceLeftActions(com.tangosol.coherence.component.net.Member member)
        {
        // import com.oracle.coherence.common.base.Continuation;
        // import java.util.Iterator;
        // import java.util.List;
        
        List listActions = (List) getServiceLeftActions().remove(member);
        if (listActions != null && !listActions.isEmpty())
            {
            for (Iterator iter = listActions.iterator(); iter.hasNext(); )
                {
                ((Continuation) iter.next()).proceed(null);
                }
            }
        }
    
    /**
     * Register this service with the ClusterService.
    * 
    * @see start()
     */
    protected void register()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService;
        
        // assign an id to the service and ensure the ServiceInfo
        ClusterService clusterservice = getClusterService();
        int            nId            = clusterservice.ensureService(this);
        
        _assert(nId > 0);
        
        setServiceId(nId);
        
        // configure the ClusterMemberSet
        setClusterMemberSet(clusterservice.getClusterMemberSet());
        
        // as of Coherence 3.7.1, the ServiceMemberSet is NOT initialized until
        // the ClusterService$ServiceJoinRequest poll is closed
        
        // potential TODO is to verify version compatibility
        }
    
    /**
     * Register the specified message class.
    * 
    * @param sMsgName  the name of the message 
    * @param clz                the class of the message component
     */
    public void registerMessageType(String sMsgName, Class clz)
        {
        // import Component.Net.Message;
        
        try
            {
            Message msg   = (Message) clz.newInstance();
            int     nType = msg.getMessageType();
            if (getMessageClass(nType) != null)
                {
                throw new IllegalStateException(clz +
                    " - duplicate MessageType: " + nType + " " + getMessageClass(nType));
                }
            setMessageClass(nType, clz);
            getMessageClassMap().put(sMsgName, clz);
            }
        catch (Exception e)
            {
            _trace("Service.registerMessageType: Unable to instantiate " + clz, 1);
            _trace(e);
            }
        }
    
    /**
     * Register the specified new RequestContext.
    * 
    * @param ctx  the RequestContext to register; if null a new one will be
    * instantiated
     */
    public com.tangosol.coherence.component.net.RequestContext registerRequestContext(com.tangosol.coherence.component.net.RequestContext ctx)
        {
        if (ctx == null)
            {
            ctx = instantiateRequestContext();
            }
        
        ctx.setRequestSUID(getSUID(SUID_REQUEST));
        ctx.setOldestPendingSUID(getOldestPendingRequestSUID());
        
        return ctx;
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    public void removeMemberListener(com.tangosol.net.MemberListener l)
        {
        getMemberListeners().remove(l);
        }
    
    /**
     * Remove all SUIDs that belong to the specified member from the specified
    * LongArray and optionally return all the corresponding values.
     */
    public static java.util.List removeSUIDRange(com.tangosol.util.LongArray array, int nMember, boolean fValues)
        {
        return removeSUIDRange(array, getBaseSUID(nMember), getBaseSUID(nMember + 1), fValues);
        }
    
    /**
     * Remove all SUIDs that belong to the specified range from the specified
    * LongArray and optionally return all the corresponding values.
     */
    public static java.util.List removeSUIDRange(com.tangosol.util.LongArray array, long lFrom, long lTo, boolean fValues)
        {
        // import com.tangosol.util.LongArray$Iterator as com.tangosol.util.LongArray.Iterator;
        // import java.util.LinkedList;
        // import java.util.List;
        
        synchronized (array)
            {
            if (fValues)
                {
                List listValues = new LinkedList();
        
                for (com.tangosol.util.LongArray.Iterator iter = array.iterator(lFrom); iter.hasNext();)
                    {
                    Object oValue = iter.next();
                    if (iter.getIndex() < lTo)
                        {
                        iter.remove();
                        listValues.add(oValue);
                        }
                    else
                        {
                        break;
                        }
                    }
        
                return listValues;
                }
            else
                {
                array.remove(lFrom, lTo);
                return null;
                }
            }
        }
    
    /**
     * Remove all SUIDs that belong to the specified member from the specified
    * sorted map of SUIDs.
     */
    public static void removeSUIDRange(java.util.SortedMap mapSUID, int nMember)
        {
        removeSUIDRange(mapSUID, getBaseSUID(nMember), getBaseSUID(nMember + 1));
        }
    
    /**
     * Remove all SUIDs that belong to the specified range from the specified
    * sorted map of SUIDs.
     */
    public static void removeSUIDRange(java.util.SortedMap mapSUID, long lFrom, long lTo)
        {
        mapSUID.subMap(Long.valueOf(lFrom), Long.valueOf(lTo)).clear();
        }
    
    /**
     * Report the mismatch of the transport configuration between the specified
    * service Member and this node.
     */
    protected void reportTransportMismatch(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import com.tangosol.util.Base;
        
        ServiceMemberSet setMember  = getServiceMemberSet();
        Member           memberThis = getThisMember();
        String           sRoleThis  = memberThis.getRoleName();
        String           sRoleThat  = member.getRoleName();
        
        // mismatch between members of the same role is probably not intentional
        
        _trace(member + " is joining the service using \""
            + ServiceMemberSet.formatEndPoint(setMember.getServiceEndPointName(member.getId()))
            + "\" transport while this member is configured to use \""
            + ServiceMemberSet.formatEndPoint(setMember.getServiceEndPointName(memberThis.getId()))
            + '"',  Base.equals(sRoleThis, sRoleThat) ? 2 : 3);
        }
    
    /**
     * For debugging only.
     */
    public void reportTransportStats()
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import java.util.Iterator;
        
        ServiceMemberSet setMember = getServiceMemberSet();
        for (Iterator iter = setMember.iterator(); iter.hasNext();)
            {
            Member member = (Member) iter.next();
        
            _trace(collectTransportStats(member));
            }
        }
    
    // Declared at the super level
    /**
     * Reset the Service statistics.
    * Reset the statistics.
     */
    public void resetStats()
        {
        // import Component.Net.MessageHandler;
        
        setStatsPollCount(0L);
        setStatsPollDuration(0L);
        setStatsPollMaxDuration(0L);
        setStatsTimeoutCount(0L);
        setStatsSent(0L);
        setStatsSentLocal(0L);
        
        MessageHandler handler = getMessageHandler();
        if (handler != null)
            {
            handler.resetStats();
            }
        
        super.resetStats();
        }
    
    /**
     * Resolve the canonical EndPoint name for the specified member.
    * 
    * Important note: while it's possible that a service overrides this method
    * and chooses not to use the bus for communications with the specified
    * member, this logic must be commutative: if member M1 decides not to talk
    * to M2 over the bus, M2 must make an identical decision. Failure to do so
    * will cause the service join protocol to hang and the service start
    * timeout.
    * 
    * @param sEndPoint                  the service's endpoint name
    * @param member                     the associated member
    * @param mapMemberConfig  the MemberConfigMap for the associated member at
    * the time it joined the service
    * 
    * @return the EndPoint reference retrieved from the [local] Depot, or null
    * if the EndPoint name cannot be resolved or if the service prefers NOT to
    * communicate with the specified member over the bus
     */
    public com.oracle.coherence.common.net.exabus.EndPoint resolveEndPoint(String sEndPoint, com.tangosol.coherence.component.net.Member member, java.util.Map mapMemberConfig)
        {
        // import Component.Net.Cluster;
        // import com.oracle.coherence.common.net.exabus.EndPoint;
        
        EndPoint peer = null;
        try
            {
            if (sEndPoint != null && sEndPoint.length() > 0)
                {
                peer = ((Cluster) getCluster()).getSocketManager().getDepot().resolveEndPoint(sEndPoint);
                }
            }
        catch (IllegalArgumentException e)
            {
            _trace("Failed to resolve the EndPoint name \"" + sEndPoint
                + "\" to communicate with service " + getServiceName()
                + " at " + member + "; falling back on the datagram transport", 1);
            return null;
            }
        
        if (getMessageHandler() != null && peer == null)
            {
            reportTransportMismatch(member);
            }
        
        return peer;
        }
    
    /**
     * Send the message and return immediately.
     */
    public void send(com.tangosol.coherence.component.net.Message msg)
        {
        post(msg);
        flush();
        }
    
    /**
     * Helper method to issue a "PingRequest" to the specified member, with the
    * specified continuation.
     */
    public void sendPingRequest(com.tangosol.coherence.component.net.Member member, com.oracle.coherence.common.base.Continuation continuation)
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        
        Grid.PingRequest msgPing = (Grid.PingRequest) instantiateMessage("PingRequest");
        
        msgPing.setContinuation(continuation);
        msgPing.setToMemberSet(SingleMemberSet.instantiate(member));
        
        post(msgPing);
        }
    
    /**
     * Serialize the specified message.
    * 
    * @return the message size
     */
    public int serializeMessage(com.tangosol.coherence.component.net.Message msg, com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import com.tangosol.io.WrapperBufferOutput$VersionAwareBufferOutput as com.tangosol.io.WrapperBufferOutput.VersionAwareBufferOutput;
        
        // header - the service id and message type is needed for "bus" transports
        // MessageHandler and BusEventMessage both "know" this format
        output.writeShort(getServiceId());
        output.writeShort(msg.getMessageType());
        
        boolean fFiltered = isProtocolFiltered();
        
        if (fFiltered)
            {
            output = wrapStream(output, msg);
            }
        
        if (!(output instanceof com.tangosol.io.WrapperBufferOutput.VersionAwareBufferOutput))
            {
            output = new com.tangosol.io.WrapperBufferOutput.VersionAwareBufferOutput(output, msg);
            }
        
        msg.writeInternal(output);
        msg.write(output);
        
        if (fFiltered) // non-filtered streams don't require closing
            {
            output.close();
            }
        
        return output.getOffset();
        }
    
    // Declared at the super level
    /**
     * Setter for property AcceptingClients.<p>
    * Set to true when the Service has advanced to the state at which it can
    * accept requests from client threads.
     */
    public void setAcceptingClients(boolean fAccepting)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService;
        // import com.tangosol.net.GuardSupport;
        // import com.tangosol.net.MemberEvent;
        
        boolean fWasAccepting = isAcceptingClients();
        
        if (!fWasAccepting && fAccepting)
            {
            dispatchMemberEvent(getThisMember(), MemberEvent.MEMBER_JOINED);
            }
        
        super.setAcceptingClients(fAccepting);
        
        if (!fWasAccepting && fAccepting)
            {
            ClusterService clusterService = getClusterService();
            if (clusterService != this)
                {
                // guard the service; Note: ClusterService guard is setup in Cluster
                clusterService.guard(getGuardable(), getDefaultGuardTimeout(), getDefaultGuardRecovery());
                GuardSupport.setThreadContext(getGuardable().getContext());
                }
            }
        }
    
    // Accessor for the property "AcceptingOthers"
    /**
     * Setter for property AcceptingOthers.<p>
    * Set to true when the Service has advanced to the state at which it can
    * accept requests from other Members.
    * 
    * Note: Services should not design this property (ClusterService is the
    * only permissible exception).
     */
    protected void setAcceptingOthers(boolean fAccepting)
        {
        __m_AcceptingOthers = fAccepting;
        }
    
    // Accessor for the property "ActionPolicy"
    /**
     * Setter for property ActionPolicy.<p>
    * The action policy for this service.
     */
    protected void setActionPolicy(com.tangosol.net.ActionPolicy policy)
        {
        __m_ActionPolicy = policy;
        }
    
    // Accessor for the property "BaseSUIDThisMember"
    /**
     * Setter for property BaseSUIDThisMember.<p>
    * The Base SUID for this Member.
    * 
    * Stored as Long to avoid needless production when used with standard Java
    * "maps".
     */
    public void setBaseSUIDThisMember(Long longMember)
        {
        __m_BaseSUIDThisMember = longMember;
        }
    
    // Accessor for the property "BufferManager"
    /**
     * Setter for property BufferManager.<p>
    * The BufferManager which to release the received buffers to. This property
    * is used only if the service is using the "datagram" transport.
     */
    public void setBufferManager(com.oracle.coherence.common.io.BufferManager mgr)
        {
        __m_BufferManager = mgr;
        }
    
    // Accessor for the property "Cluster"
    /**
     * Setter for property Cluster.<p>
    * The Cluster that this Service is a part of.
     */
    public void setCluster(com.tangosol.net.Cluster cluster)
        {
        _assert(getCluster() == null);
        __m_Cluster = (cluster);
        }
    
    // Accessor for the property "ClusterMemberSet"
    /**
     * Setter for property ClusterMemberSet.<p>
    * Set of all Members in the cluster. This set is a live reference to the
    * MasterMemberSet maintained by the ClusterService and should be used
    * cautiously in a read-only manner.
     */
    public void setClusterMemberSet(com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet setMember)
        {
        _assert(!isStarted());
        __m_ClusterMemberSet = (setMember);
        }
    
    // Accessor for the property "CompletedRequestSUIDs"
    /**
     * Setter for property CompletedRequestSUIDs.<p>
    * A set (implemented via a map) of completed request SUIDs.  SUIDs only
    * exist in this set while there are older incomplete SUIDs.
    * 
    * Used by requests that need to implement idempotent re-execution in the
    * case of a failover.
     */
    protected void setCompletedRequestSUIDs(java.util.concurrent.ConcurrentMap map)
        {
        __m_CompletedRequestSUIDs = map;
        }
    
    // Accessor for the property "DepartingMembers"
    /**
     * Setter for property DepartingMembers.<p>
    * This MemberSet contains members that are about to be removed from the
    * ServiceMemberSet. Used only for Members connected via a MessageBus while
    * the "release" operation is in progress. Used only by the service thread
    * to throttle the MemberWelcome protocol.
     */
    protected void setDepartingMembers(com.tangosol.coherence.component.net.MemberSet setMembers)
        {
        __m_DepartingMembers = setMembers;
        }
    
    // Accessor for the property "InWait"
    /**
     * Setter for property InWait.<p>
    * True iff the service thread is waiting, i.e. in onWait()
    * 
    * @volatile - read on threads adding to the service's queue
     */
    protected void setInWait(boolean fWait)
        {
        __m_InWait = fWait;
        }
    
    // Accessor for the property "MemberListeners"
    /**
     * Setter for property MemberListeners.<p>
    * The list of registered MemberListeners.
    * 
    * @see #addMemberListener and #removeMemberListener
     */
    private void setMemberListeners(com.tangosol.util.Listeners listeners)
        {
        __m_MemberListeners = listeners;
        }
    
    // Accessor for the property "MessageClass"
    /**
     * Setter for property MessageClass.<p>
    * An indexed property that translates Message numbers into Message classes.
     */
    private void setMessageClass(Class[] aclz)
        {
        __m_MessageClass = aclz;
        }
    
    // Accessor for the property "MessageClass"
    /**
     * Setter for property MessageClass.<p>
    * An indexed property that translates Message numbers into Message classes.
     */
    protected void setMessageClass(int i, Class clz)
        {
        // adjust Message id to support the negative id's used by service 0
        i += MESSAGE_OFFSET;
        
        Class[] aClass = getMessageClass();
        
        boolean fBeyondBounds = (aClass == null || i >= aClass.length);
        if (fBeyondBounds && clz != null)
            {
            // resize, making the array bigger than necessary (avoid resizes)
            int     cNew      = Math.max(i + (i >>> 1), i + 4);
            Class[] aClassNew = new Class[cNew];
        
            // copy original data
            if (aClass != null)
                {
                System.arraycopy(aClass, 0, aClassNew, 0, aClass.length);
                }
        
            setMessageClass(aClass = aClassNew);
            fBeyondBounds = false;
            }
        
        if (!fBeyondBounds)
            {
            aClass[i] = clz;
            }
        }
    
    // Accessor for the property "MessageClassMap"
    /**
     * Setter for property MessageClassMap.<p>
    * A map, keyed by message name, of Message classes known to this Grid
    * component.
     */
    protected void setMessageClassMap(java.util.Map map)
        {
        __m_MessageClassMap = map;
        }
    
    // Accessor for the property "MessageHandler"
    /**
     * Setter for property MessageHandler.<p>
    * MessageHandler used by this service for MessageBus-based communications.
     */
    protected void setMessageHandler(com.tangosol.coherence.component.net.MessageHandler handler)
        {
        __m_MessageHandler = handler;
        }
    
    // Accessor for the property "MessagePublisher"
    /**
     * Setter for property MessagePublisher.<p>
    * The MessagePublisher that the Service uses to send Messages.
    * 
    * @volatile - see Grid.stop()
     */
    public void setMessagePublisher(com.tangosol.internal.util.MessagePublisher queue)
        {
        __m_MessagePublisher = queue;
        }
    
    // Accessor for the property "OldestPendingRequestSUIDCounter"
    /**
     * Setter for property OldestPendingRequestSUIDCounter.<p>
    * The AtomicLong backing OldestPendingRequestSUID.
     */
    protected void setOldestPendingRequestSUIDCounter(java.util.concurrent.atomic.AtomicLong map)
        {
        __m_OldestPendingRequestSUIDCounter = map;
        }
    
    // Accessor for the property "PendingQuiescenceResponses"
    /**
     * Setter for property PendingQuiescenceResponses.<p>
    * Delayed response messages for a pending QuiescenceRequests, to be sent
    * once service has finished quiescencing. null if there are no pending
    * reponses.
     */
    public void setPendingQuiescenceResponses(java.util.Collection colResponses)
        {
        __m_PendingQuiescenceResponses = colResponses;
        }
    
    // Accessor for the property "PollArray"
    /**
     * Setter for property PollArray.<p>
    * This is a WindowedArray of active Poll objects.
     */
    protected void setPollArray(Grid.PollArray waPoll)
        {
        __m_PollArray = waPoll;
        }
    
    // Accessor for the property "QueueDeferred"
    /**
     * Setter for property QueueDeferred.<p>
    * This is the Queue to which messages are deferred until startup is
    * complete.
     */
    private void setQueueDeferred(com.tangosol.coherence.component.util.Queue queue)
        {
        __m_QueueDeferred = queue;
        }
    
    // Accessor for the property "QueueDeferredWelcome"
    /**
     * Setter for property QueueDeferredWelcome.<p>
    * This Queue holds deferred MemberWelcomeRequest messages while this member
    * is yet to finish the join protocol or any Member departure processing is
    * in progress. Used only by the service thread to order and throttle the
    * MemberWelcome protocol.
     */
    public void setQueueDeferredWelcome(com.tangosol.coherence.component.util.Queue queue)
        {
        __m_QueueDeferredWelcome = queue;
        }
    
    // Accessor for the property "RequestTimeout"
    /**
     * Setter for property RequestTimeout.<p>
    * A default timeout value for polls and PriorityTasks that don't explicitly
    * specify the request timeout value.
     */
    public void setRequestTimeout(long cMillis)
        {
        __m_RequestTimeout = cMillis;
        }
    
    // Accessor for the property "ResourceRegistry"
    /**
     * Setter for property ResourceRegistry.<p>
    * ResourceRegistry associated with this Service.
     */
    public void setResourceRegistry(com.tangosol.util.ResourceRegistry registry)
        {
        __m_ResourceRegistry = registry;
        }
    
    // Accessor for the property "ServiceConfigMap"
    /**
     * Setter for property ServiceConfigMap.<p>
    * This ObservableMap is shared by all Members running this Service and is
    * maintained on the service thread. All updates are published via the
    * senior Service Member.
    * 
    * Services may need to verify locally configured properties to ensure that
    * they are consistent across the grid (e.g. partition-count, backup-count,
    * etc.)  There is a well-known entry in the ServiceConfigMap, keyed by
    * ﻿Integer((int) '$'), that holds this configuration.
     */
    protected void setServiceConfigMap(Grid.ServiceConfig.Map map)
        {
        __m_ServiceConfigMap = map;
        }
    
    // Accessor for the property "ServiceFailurePolicy"
    /**
     * Setter for property ServiceFailurePolicy.<p>
     */
    public void setServiceFailurePolicy(com.tangosol.net.ServiceFailurePolicy policy)
        {
        __m_ServiceFailurePolicy = policy;
        }
    
    // Accessor for the property "ServiceId"
    /**
     * Setter for property ServiceId.<p>
    * The id assigned to this Service within the cluster.
     */
    public void setServiceId(int nId)
        {
        _assert(!isStarted());
        __m_ServiceId = (nId);
        }
    
    // Accessor for the property "ServiceLeftActions"
    /**
     * Setter for property ServiceLeftActions.<p>
    * The Map<Member, List<Continuation>> of actions whose processing is
    * deferred until the associated member's departure from the service has
    * been processed.
     */
    public void setServiceLeftActions(java.util.Map mapActions)
        {
        __m_ServiceLeftActions = mapActions;
        }
    
    // Accessor for the property "ServiceMemberSet"
    /**
     * Setter for property ServiceMemberSet.<p>
    * Set of Members that joined this service. As of Coherence 3.7.1, for all
    * services except the ClusterService (service zero) this set is not the
    * same ServiceMemberSet that is held by the ClusterService in the
    * corresponding ServiceInfo.
     */
    public void setServiceMemberSet(com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet setMember)
        {
        _assert(getServiceMemberSet() == null || setMember == null);
        
        __m_ServiceMemberSet = (setMember);
        }
    
    // Declared at the super level
    /**
     * Setter for property ServiceState.<p>
    * The state of the Service; one of the SERVICE_ enums.
    * 
    * @volatile as of 12.1.3
     */
    public void setServiceState(int nState)
        {
        // import com.tangosol.net.MemberEvent;
        
        if (nState > getServiceState())
            {
            switch (nState)
                {
                case SERVICE_STARTED:
                    // defer the event; see #setAcceptingClients
                    break;
        
                case SERVICE_STOPPING:
                    dispatchMemberEvent(getThisMember(), MemberEvent.MEMBER_LEAVING);
                    break;
        
                case SERVICE_STOPPED:
                    dispatchMemberEvent(getThisMember(), MemberEvent.MEMBER_LEFT);
                    break;
                }
            }
        
        super.setServiceState(nState);
        }
    
    // Accessor for the property "StatsPollCount"
    /**
     * Setter for property StatsPollCount.<p>
    * Statistics: total number of Polls processed.
     */
    protected void setStatsPollCount(long cPolls)
        {
        __m_StatsPollCount = cPolls;
        }
    
    // Accessor for the property "StatsPollDuration"
    /**
     * Setter for property StatsPollDuration.<p>
    * Statistics: total duration (in milliseconds) taken to process Polls.
     */
    protected void setStatsPollDuration(long cMillis)
        {
        __m_StatsPollDuration = cMillis;
        }
    
    // Accessor for the property "StatsPollMaxDuration"
    /**
     * Setter for property StatsPollMaxDuration.<p>
    * Statistics: maximum duration (in milliseconds) taken to process a single
    * Poll.
     */
    protected void setStatsPollMaxDuration(long cMillis)
        {
        __m_StatsPollMaxDuration = cMillis;
        }
    
    // Declared at the super level
    /**
     * Setter for property StatsReceived.<p>
    * Statistics: total number of received messages.
     */
    public void setStatsReceived(long cMsgs)
        {
        super.setStatsReceived(cMsgs);
        }
    
    // Accessor for the property "StatsSent"
    /**
     * Setter for property StatsSent.<p>
    * Statistics: total number of sent messages.
     */
    public void setStatsSent(long cMsgs)
        {
        __m_StatsSent = cMsgs;
        }
    
    // Accessor for the property "StatsSentLocal"
    /**
     * Setter for property StatsSentLocal.<p>
    * Statistics: total number of self-addressed messages.
     */
    public void setStatsSentLocal(long cMsgs)
        {
        __m_StatsSentLocal = cMsgs;
        }
    
    // Accessor for the property "StatsTimeoutCount"
    /**
     * Setter for property StatsTimeoutCount.<p>
    * The total number of timed-out requests since the last time the statistics
    * were reset.
    * 
    * @volatile
     */
    public void setStatsTimeoutCount(long cRequests)
        {
        __m_StatsTimeoutCount = cRequests;
        }
    
    // Accessor for the property "SUIDCounter"
    /**
     * Setter for property SUIDCounter.<p>
    * An AtomicCounter object that is used for SUID related calculations.
     */
    protected void setSUIDCounter(java.util.concurrent.atomic.AtomicLong[] alongCounter)
        {
        __m_SUIDCounter = alongCounter;
        }
    
    // Accessor for the property "SUIDCounter"
    /**
     * Setter for property SUIDCounter.<p>
    * An AtomicCounter object that is used for SUID related calculations.
     */
    protected void setSUIDCounter(int i, java.util.concurrent.atomic.AtomicLong longCounter)
        {
        getSUIDCounter()[i] = longCounter;
        }
    
    // Accessor for the property "SuspendPollLimit"
    /**
     * Setter for property SuspendPollLimit.<p>
    * The poll id which represents the maximum allowable poll which can be sent
    * currently.  A message with a greater poll id must not be sent until this
    * value increases making the send allowable.  Each time the value increases
    * the AtomicLong will also be notified, allowing for threads to wait on
    * this value.
     */
    public void setSuspendPollLimit(java.util.concurrent.atomic.AtomicLong atomicLimit)
        {
        __m_SuspendPollLimit = atomicLimit;
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    // Accessor for the property "UserContext"
    /**
     * Setter for property UserContext.<p>
    * User context object associated with this Service.
     */
    public void setUserContext(Object oCtx)
        {
        __m_UserContext = oCtx;
        }
    
    // Accessor for the property "WrapperStreamFactoryAllList"
    /**
     * Setter for property WrapperStreamFactoryAllList.<p>
    * All filters that should be applied during onNotify.
     */
    public void setWrapperStreamFactoryAllList(java.util.List listAll)
        {
        __m_WrapperStreamFactoryAllList = listAll;
        }
    
    // Accessor for the property "WrapperStreamFactoryList"
    /**
     * Setter for property WrapperStreamFactoryList.<p>
    * List of WrapperStreamFactory objects that affect how Messages sent from/
    * to this specific Service are written and read.
     */
    public void setWrapperStreamFactoryList(java.util.List list)
        {
        _assert(!isStarted());
        __m_WrapperStreamFactoryList = (list);
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    // Declared at the super level
    /**
     * Stop the Controllable.
     */
    public synchronized void shutdown()
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.util.Base;
        
        // do not call super to avoid the immediate call to stop()
        
        if (isStarted())
            {
            if (getServiceState() < SERVICE_STOPPING)
                {
                checkShutdownPermission();
        
                // send the request to shut down
                send(instantiateMessage("NotifyShutdown"));
                }
            }
        
        Thread thread = getThread();
        if (thread != Thread.currentThread())
            {
            // wait for the service to stop or the thread to die
            while (isStarted() && getServiceState() < SERVICE_STOPPED)
                {
                try
                    {
                    Blocking.wait(this, 1000L);
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw Base.ensureRuntimeException(e);
                    }
                }
        
            // wait for a bounded amount of time for the event dispatcher to drain
            waitForEventDispatcher();
        
            if (getServiceState() != SERVICE_STOPPED)
                {
                stop();
                }
            }
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    // Declared at the super level
    /**
     * Starts the daemon thread associated with this component. If the thread is
    * already starting or has started, invoking this method has no effect.
    * 
    * Synchronization is used here to verify that the start of the thread
    * occurs; the lock is obtained before the thread is started, and the daemon
    * thread notifies back that it has started from the run() method.
     */
    public synchronized void start()
        {
        if (getServiceState() == SERVICE_INITIAL)
            {
            register();
            }
        
        super.start();
        }
    
    // From interface: com.tangosol.internal.util.GridComponent
    // From interface: com.tangosol.net.Service
    // Declared at the super level
    /**
     * Hard-stop the Service. Use shutdown() for normal  termination.
     */
    public void stop()
        {
        // import com.tangosol.internal.util.NullMessagePublisher;
        
        checkShutdownPermission();
        
        setMessagePublisher(NullMessagePublisher.INSTANCE); // prevent further external communication
        
        super.stop();
        }
    
    /**
     * Convert the specified Throwable into a RuntimeException and tag it with a
    * node specific information before it gets returned back to a client if the
    * service is running, or rethrown as EventDeathException if the service is
    * stopping.
     */
    public RuntimeException tagException(Throwable e)
        {
        // import com.tangosol.io.SizeEstimatingBufferOutput;
        // import com.tangosol.license.LicenseException;
        // import com.tangosol.run.component.EventDeathException;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.WrapperException;
        
        if (e instanceof LicenseException)
            {
            return (LicenseException) e;
            }
        
        String sMsg = null;
        try
            {
            sMsg = "Failed request execution for " + getServiceName() +
                          " service on " + getThisMember();
        
            if (e instanceof WrapperException)
                {
                WrapperException we = (WrapperException) e;
                e     = we.getOriginalException();
                sMsg += ' ' + we.getMessage();
                }
        
            if (e instanceof OutOfMemoryError)
                {
                // real OutOfMemory does not always provide stack trace
                StackTraceElement[] atrace = e.getStackTrace();
                if (atrace == null || atrace.length == 0)
                    {
                    // replace with a pseudo OOME
                    e = new OutOfMemoryError();
                    }
                }
            }
        catch (Throwable eIgnore) {}
        
        if (isExiting())
            {
            // if the service is exiting, don't return the exception to the client as
            // it may be due to the process of shutting down; throwing EventDeathException
            // instead will ensure that the service stops processing the request without
            // responding.
        
            _trace(get_Name() + " encountered an exception (" +
                   e.getClass().getName() + ": " + e.getMessage() + ") while stopping.", 3);
            throw new EventDeathException(sMsg);
            }
        else
            {
            // make sure the exception is serializable; use the pseudo-buffer to reduce the cost
            try
                {
                getSerializer().serialize(new SizeEstimatingBufferOutput(), e);
                }
            catch (Throwable ex)
                {
                e = new RuntimeException(e.toString() + " (Note: The orignal exception is not serializable).");
                }
        
            return Base.ensureRuntimeException(e, sMsg);
            }
        }
    
    protected void unregisterPoll(com.tangosol.coherence.component.net.Poll poll)
        {
        // import com.tangosol.util.Base;
        
        // note: poll.getPollId() may be 0L in one extremely rare case, which is when
        // the member died as the poll was being registered on a client thread, so the
        // poll was added to the PollArray but the id has not yet been set on the poll
        // (both happen on Service.registerPoll)
        
        _assert(poll != null && poll.getService() == this);
        getPollArray().remove(poll.getPollId());
        
        // update poll statistics
        long cMillis = Base.getSafeTimeMillis() - poll.getInitTimeMillis();
        setStatsPollCount(getStatsPollCount() + 1L);
        setStatsPollDuration(getStatsPollDuration() + cMillis);
        if (cMillis > getStatsPollMaxDuration())
            {
            setStatsPollMaxDuration(cMillis);
            }
        }
    
    /**
     * Unregister the specified RequestContext.
     */
    public void unregisterRequestContext(com.tangosol.coherence.component.net.RequestContext ctx)
        {
        // import com.oracle.coherence.common.base.MutableLong;
        // import java.util.concurrent.atomic.AtomicLong;
        // import java.util.Map;
        
        if (ctx == null)
            {
            return;
            }
        
        AtomicLong  counterOldest = getOldestPendingRequestSUIDCounter();
        Map         map           = getCompletedRequestSUIDs();
        long        lIdCtx        = ctx.getRequestSUID();
        long        lIdNext       = lIdCtx + 1L;
        MutableLong LIdCtx        = new MutableLong(lIdCtx);
        
        if (!counterOldest.compareAndSet(lIdCtx, lIdNext))
            {
            // not the oldest, leave a marker indicating that this request has
            // completed
            map.put(LIdCtx, Boolean.TRUE);
        
            // double check that we didn't just become oldest
            if (!counterOldest.compareAndSet(lIdCtx, lIdNext))
                {
                // still not oldest will do cleanup later
                return;
                }
        
            map.remove(LIdCtx);
            // note since we've ensure that LIdCtx isn't in the map we can reuse it below
            }
        
        // we are the oldest pending SUID, cleanup map
        while (map.containsKey(LIdCtx.set(lIdNext)) &&             // avoid taking out lock
               map.remove(LIdCtx) != null &&                       // take out lock
               counterOldest.compareAndSet(lIdNext, lIdNext + 1L)) // only cas if this thread did the remove
            {
            ++lIdNext;
            }
        }
    
    /**
     * Returns {@code true} if the given message is eligible for processing
    * otherwise
    * produces a log entry and returns {@code false}.
    * 
    * @param msg  the message to validate
    * 
    * @returns  {@code true} if the given message is eligible for processing
    * otherwise
    *                   produces a log entry and returns {@code false}
     */
    protected boolean validateMessage(com.tangosol.coherence.component.net.Message msg)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        
        Member memberFrom = msg.getFromMember();
        
        if (memberFrom != null)
            {
            int       nMemberFrom = memberFrom.getId();
            MemberSet setMember   = getServiceMemberSet();
            boolean   fMember     = false;
        
            if (setMember != null)
                {
                synchronized (setMember)
                    {
                    fMember = setMember.getMember(nMemberFrom) != null;
                    }
                }
        
            if (!fMember && !msg.isInternal() && getServiceId() > 0)
                {
                // It is possible that messages from a departed member were put on
                // the service queue after that member was removed from the service
                // member set.
                // It is also possible that this member recently restarted the service,
                // and the queued message was addressed to the "old" service instance.
                // (COH-2713)
                _trace("Discarding a " + msg.get_Name() + " message from member "
                       + nMemberFrom + " which does not belong to the service.", 3);
        
                return false;
                }
            }
        
        return true;
        }
    
    /**
     * Validate the local service config is compatible with the service config
    * in use by the senior member.
    * 
    * @return true if the configuration is validated; false otherwise
     */
    protected boolean validateServiceConfig()
        {
        // import com.tangosol.run.xml.XmlElement;
        // import java.util.Map;
        
        boolean    fSenior    = getThisMember() == getServiceOldestMember();
        Object     oConfigKey = Character.valueOf('$');
        Map        mapConfig  = getServiceConfigMap();
        XmlElement xmlConfig  = fSenior ? null : (XmlElement) mapConfig.get(oConfigKey);
        
        if (xmlConfig == null)
            {
            // we must be the service senior; our config will define the
            // "global" picture.
            //
            // Note: the fix for COH-5774 relies on a non-empty service-config
            //       (see Grid.ConfigUpdate.onReceived/$ConfigSync.onReceived)
            _assert(fSenior, "The service configuration is missing.");
            xmlConfig = initServiceConfig();
            mapConfig.put(oConfigKey, xmlConfig);
            return true;
            }
        
        return validateServiceConfig(xmlConfig);
        }
    
    /**
     * Validate the specified service config against this member.
    * 
    * @param xmlConfig   the service config
    * 
    * @return true  if this member is consistent with the specified config
     */
    protected boolean validateServiceConfig(com.tangosol.run.xml.XmlElement xmlConfig)
        {
        return true;
        }
    
    /**
     * Verify that a feature with the specified name has the expected value.
     */
    protected boolean verifyFeature(String sFeature, Object oValue, Object oTest)
        {
        if (oValue.equals(oTest))
            {
            return true;
            }
        else
            {
            _trace("Incompatible " + sFeature + " implementation: "
                 + "this node is configured to use " + oValue
                 + ", but the service senior is using " + oTest
                 + "; stopping the service.", 1);
            return false;
            }
        }
    
    protected com.tangosol.io.ReadBuffer.BufferInput wrapStream(com.tangosol.io.ReadBuffer.BufferInput input, com.tangosol.coherence.component.net.Message msg)
        {
        // import com.tangosol.io.WrapperDataInputStream;
        // import com.tangosol.io.WrapperBufferInput;
        // import com.tangosol.io.WrapperStreamFactory as com.tangosol.io.WrapperStreamFactory;
        // import com.tangosol.io.ReadBuffer$BufferInput as com.tangosol.io.ReadBuffer.BufferInput;
        // import com.tangosol.net.internal.ProtocolAwareStream as com.tangosol.net.internal.ProtocolAwareStream;
        // import java.util.List;
        // import java.io.DataInputStream;
        // import java.io.InputStream;
        
        // wrap the input (if applicable)
        InputStream streamWrap = null;
        
        List listAll = getWrapperStreamFactoryAllList();
        if (listAll != null)
            {
            streamWrap = new WrapperDataInputStream(input);
            for (int i = 0, c = listAll.size(); i < c; ++i)
                {
                 streamWrap = ((com.tangosol.io.WrapperStreamFactory) listAll.get(i)).
                    getInputStream(streamWrap);
                if (streamWrap instanceof com.tangosol.net.internal.ProtocolAwareStream)
                    {
                    ((com.tangosol.net.internal.ProtocolAwareStream) streamWrap).setProtocolContext
                        (getProtocolContext(msg));
                    }
                }
            }
        
        List listSvc = getWrapperStreamFactoryList();
        if (listSvc != null)
            {
            if (streamWrap == null)
                {
                streamWrap = new WrapperDataInputStream(input);
                }
        
            for (int i = 0, c = listSvc.size(); i < c; ++i)
                {
                streamWrap = ((com.tangosol.io.WrapperStreamFactory) listSvc.get(i)).
                    getInputStream(streamWrap);
                if (streamWrap instanceof com.tangosol.net.internal.ProtocolAwareStream)
                   {
                   ((com.tangosol.net.internal.ProtocolAwareStream) streamWrap).setProtocolContext
                       (getProtocolContext(msg));
                   }
                }
            }
        
        return streamWrap == null
            ? input // no filters are configured
            : streamWrap instanceof com.tangosol.io.ReadBuffer.BufferInput
                ? (com.tangosol.io.ReadBuffer.BufferInput) streamWrap // filter returned BufferInput
                : new WrapperBufferInput(new DataInputStream(streamWrap), getContextClassLoader()); // filter returned BufferInput
        }
    
    public com.tangosol.io.WriteBuffer.BufferOutput wrapStream(com.tangosol.io.WriteBuffer.BufferOutput output, com.tangosol.coherence.component.net.Message msg)
        {
        // import com.tangosol.io.WrapperBufferOutput;
        // import com.tangosol.io.WrapperDataOutputStream;
        // import com.tangosol.io.WrapperStreamFactory as com.tangosol.io.WrapperStreamFactory;
        // import com.tangosol.io.WriteBuffer$BufferOutput as com.tangosol.io.WriteBuffer.BufferOutput;
        // import com.tangosol.net.internal.ProtocolAwareStream as com.tangosol.net.internal.ProtocolAwareStream;
        // import java.util.List;
        // import java.io.DataOutputStream;
        // import java.io.OutputStream;
        
        // wrap the stream, if applicable
        List         listSvc    = getWrapperStreamFactoryList();
        OutputStream streamWrap = null;
        
        if (listSvc != null)
            {
            streamWrap = new WrapperDataOutputStream(output);
            for (int i = 0, c = listSvc.size(); i < c; ++i)
                {
                streamWrap = ((com.tangosol.io.WrapperStreamFactory) listSvc.get(i)).getOutputStream(streamWrap);
                if (streamWrap instanceof com.tangosol.net.internal.ProtocolAwareStream)
                    {
                    ((com.tangosol.net.internal.ProtocolAwareStream) streamWrap).setProtocolContext
                        (getProtocolContext(msg));
                    }
                }
            }
        
        List listAll = getWrapperStreamFactoryAllList();
        if (listAll != null)
            {
            if (streamWrap == null)
                {
                streamWrap = new WrapperDataOutputStream(output);
                }
        
            for (int i = 0, c = listAll.size(); i < c; ++i)
                {
                streamWrap = ((com.tangosol.io.WrapperStreamFactory) listAll.get(i)).getOutputStream(streamWrap);
                if (streamWrap instanceof com.tangosol.net.internal.ProtocolAwareStream)
                    {
                    ((com.tangosol.net.internal.ProtocolAwareStream) streamWrap).setProtocolContext
                        ((getProtocolContext(msg)));
                    }
                }
            }
        
        return streamWrap == null
            ? output // no filters are configured
            : streamWrap instanceof com.tangosol.io.WriteBuffer.BufferOutput
                ? (com.tangosol.io.WriteBuffer.BufferOutput) streamWrap // filter returned BufferOutput
                : new WrapperBufferOutput(new DataOutputStream(streamWrap)); // filter returned BufferOutput
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$Acknowledgement
    
    /**
     * This Message is meant to be used as a simple acknowledgement to a
     * RequestMessage requiring an poll response.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Acknowledgement
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public Acknowledgement()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Acknowledgement(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(1);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Acknowledgement();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$Acknowledgement".replace('/', '.'));
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
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$BusEventMessage
    
    /**
     * BusEventMessage is an internal message used to pass Exabus events onto
     * the corresponding service thread.
     * 
     * Attributes:
     *     MessageHandler
     *     Event
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BusEventMessage
            extends    com.tangosol.coherence.component.net.message.BusEventMessage
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public BusEventMessage()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public BusEventMessage(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-21);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.BusEventMessage();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$BusEventMessage".replace('/', '.'));
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
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ConfigRequest
    
    /**
     * This RequestMessage is a poll (where Poll is an instance child) that all
     * members send to the config coordinator Member to request that a
     * ConfigUpdate be sent.
     * 
     * Attributes:
     *     Key
     *     Value
     *     Remove
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConfigRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property ConfigMap
         *
         * The config map that generated this ConfigRequest.
         */
        private transient com.tangosol.coherence.component.util.ServiceConfig.Map __m_ConfigMap;
        
        /**
         * Property Remove
         *
         * True if the Message indicates a delete ("remove") operation; false
         * if the Message indicates an insert or update ("put").
         */
        private boolean __m_Remove;
        
        /**
         * Property UpdateMap
         *
         * The map of keys being modified to the values they are being updated
         * to (or null if the keys are being removed).
         */
        private java.util.Map __m_UpdateMap;
        
        // Default constructor
        public ConfigRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConfigRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-15);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new Grid.ConfigRequest.Poll("Poll", this, true), "Poll");
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ConfigRequest".replace('/', '.'));
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
        
        // Accessor for the property "ConfigMap"
        /**
         * Getter for property ConfigMap.<p>
        * The config map that generated this ConfigRequest.
         */
        public com.tangosol.coherence.component.util.ServiceConfig.Map getConfigMap()
            {
            return __m_ConfigMap;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "UpdateMap=" + getUpdateMap() + "\nRemove=" + isRemove();
            }
        
        // Accessor for the property "UpdateMap"
        /**
         * Getter for property UpdateMap.<p>
        * The map of keys being modified to the values they are being updated
        * to (or null if the keys are being removed).
         */
        public java.util.Map getUpdateMap()
            {
            return __m_UpdateMap;
            }
        
        // Accessor for the property "Remove"
        /**
         * Getter for property Remove.<p>
        * True if the Message indicates a delete ("remove") operation; false if
        * the Message indicates an insert or update ("put").
         */
        public boolean isRemove()
            {
            return __m_Remove;
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
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import Component.Util.ServiceConfig$Map as com.tangosol.coherence.component.util.ServiceConfig.Map;
            // import java.util.Iterator;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            super.onReceived();
            
            Grid   service    = getService();
            com.tangosol.coherence.component.util.ServiceConfig.Map mapConfig  = getConfigMap();
            Member    memberThis = service.getThisMember();
            Member    memberFrom = getFromMember();
            Map       mapUpdate  = getUpdateMap();
            boolean   fRemove    = isRemove();
            
            // respond to the poll;
            // Note: the requestor has already updated its com.tangosol.coherence.component.util.ServiceConfig.Map
            //       so there is no need to send the actual data
            Grid.ConfigResponse response = (Grid.ConfigResponse)
                service.instantiateMessage("ConfigResponse");
            
            if (memberThis == mapConfig.getConfigCoordinator())
            	{
                // The potential problem we are trying to solve here is:
                // assume configMap.get(k) == v0
                // and the coordinator requests two updates in a row
                //    configMap.put(k, v1);
                //    configMap.put(k, v2);
                // then the sequence of events could be:
                // 1)  update v0 -> v1 (at ServiceConfig$Map#put)
                // 2)  update v1 -> v2 (at ServiceConfig$Map#put)
                // 1a) update v2 -> v1 (at $ConfigRequest#onReceived)
                // 2a) update v1 -> v2 (at $ConfigRequest#onReceived)
                //
                // if a new member joins and is updated using Grid.ConfigSync
                // between 1a) and 2a), this new member will not get the v2 update
            
                if (memberThis == memberFrom)
                    {
                    for (Iterator iter = mapUpdate.entrySet().iterator(); iter.hasNext(); )
                        {
                        java.util.Map.Entry  entry = (java.util.Map.Entry) iter.next();
                        Object oKey  = entry.getKey();
                        if (mapConfig.containsKey(oKey))
                            {
                            if (fRemove)
                                {
                                iter.remove();
                                }
                            else
                                {
                                entry.setValue(mapConfig.get(oKey));
                                }
                            }
                        else
                            {
                            if (!fRemove)
                                {
                                iter.remove();
                                }
                            }
                        }
                    }
                else
                    {
                    mapConfig.updateInternal(mapUpdate, fRemove);
                    }
            
                // publish requested change cluster-wide
                MemberSet setOthers = new MemberSet();
                setOthers.addAll(service.getConfigMapUpdateMembers(mapConfig));
                setOthers.remove(memberFrom);
            
                Grid.ConfigUpdate msg = (Grid.ConfigUpdate) service.instantiateMessage("ConfigUpdate");
                msg.setConfigMap(mapConfig);
                msg.setToMemberSet(setOthers);
                msg.setUpdateMap(mapUpdate);
                msg.setRemove(fRemove);
            
                response.setAcknowledged(true);
                service.post(msg);
                }
            else
                {
                // this could happen in two cases:
                // a) this service has restarted but the sender did not know it while sending
                // b) an old senior has died and this member is a new senior,
                //    which sender already knows, but this member does not know yet
                response.setAcknowledged(false);
                }
            
            response.respondTo(this);
            service.post(response);
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Util.ServiceConfig$Map as com.tangosol.coherence.component.util.ServiceConfig.Map;
            // import com.tangosol.util.LiteMap;
            // import java.io.IOException;
            
            LiteMap map     = new LiteMap();
            Grid service = getService();
            try
                {
                com.tangosol.coherence.component.util.ServiceConfig.Map configMap = service.getConfigMap(input.readInt());
            
                setConfigMap(configMap);
                setRemove(input.readBoolean());
                int cEntries  = input.readInt();
            
                for (int i = 0; i < cEntries; i++)
                    {
                    map.put(configMap.readObject(input),
                            configMap.readObject(input));
                    }
                }
            catch (IOException e)
                {
                service.onConfigIOException(e, getFromMember());
                }
            
            setUpdateMap(map);
            }
        
        // Accessor for the property "ConfigMap"
        /**
         * Setter for property ConfigMap.<p>
        * The config map that generated this ConfigRequest.
         */
        public void setConfigMap(com.tangosol.coherence.component.util.ServiceConfig.Map mapConfig)
            {
            __m_ConfigMap = mapConfig;
            }
        
        // Accessor for the property "Remove"
        /**
         * Setter for property Remove.<p>
        * True if the Message indicates a delete ("remove") operation; false if
        * the Message indicates an insert or update ("put").
         */
        public void setRemove(boolean fRemove)
            {
            __m_Remove = fRemove;
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
        
        // Accessor for the property "UpdateMap"
        /**
         * Setter for property UpdateMap.<p>
        * The map of keys being modified to the values they are being updated
        * to (or null if the keys are being removed).
         */
        public void setUpdateMap(java.util.Map mapUpdate)
            {
            __m_UpdateMap = mapUpdate;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import Component.Util.ServiceConfig$Map as com.tangosol.coherence.component.util.ServiceConfig.Map;
            // import java.util.Iterator;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            com.tangosol.coherence.component.util.ServiceConfig.Map mapConfig = getConfigMap();
            Map       mapUpdate = getUpdateMap();
            
            output.writeInt(getConfigMap().getMapType());
            output.writeBoolean(isRemove());
            output.writeInt(mapUpdate.size());
            
            for (Iterator iter = mapUpdate.entrySet().iterator(); iter.hasNext(); )
                {
                java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
                mapConfig.writeObject(output, entry.getKey());
                mapConfig.writeObject(output, entry.getValue());
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ConfigRequest$Poll
        
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
            
            /**
             * Property Acknowledged
             *
             * If true, the ConfigRequest has been received by the config
             * coordinator and corresponding ConfigUpdate has been sent to all
             * service members.
             */
            private boolean __m_Acknowledged;
            
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest.Poll();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ConfigRequest$Poll".replace('/', '.'));
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
            
            // Accessor for the property "Acknowledged"
            /**
             * Getter for property Acknowledged.<p>
            * If true, the ConfigRequest has been received by the config
            * coordinator and corresponding ConfigUpdate has been sent to all
            * service members.
             */
            public boolean isAcknowledged()
                {
                return __m_Acknowledged;
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            protected void onCompletion()
                {
                // import Component.Net.Member;
                // import Component.Util.ServiceConfig$Map as com.tangosol.coherence.component.util.ServiceConfig.Map;
                // import com.tangosol.util.Base;
                // import com.tangosol.util.LiteMap;
                // import java.util.Iterator;
                // import java.util.Map;
                // import java.util.Map$Entry as java.util.Map.Entry;
                
                Grid service = getService();
                if (service.getServiceState() == Grid.SERVICE_STOPPED)
                    {
                    return;
                    }
                
                Grid.ConfigRequest msgRequest = (Grid.ConfigRequest) get_Parent();
                com.tangosol.coherence.component.util.ServiceConfig.Map      mapConfig  = msgRequest.getConfigMap();
                
                Map     mapPending = mapConfig.getConfig().getPendingPolls();
                Map     mapUpdate  = msgRequest.getUpdateMap();
                boolean fRemove    = msgRequest.isRemove(); 
                
                if (!mapUpdate.isEmpty())
                    {
                    Map mapInternal = mapConfig.getMap();
                    synchronized (mapInternal)
                        {
                        for (Iterator iter = mapUpdate.entrySet().iterator(); iter.hasNext(); )
                            {
                            java.util.Map.Entry  entry = (java.util.Map.Entry) iter.next();
                            Object oKey  = entry.getKey();
                            if (mapPending.get(oKey) == this)
                                {
                                mapPending.remove(oKey);
                
                                if (!isAcknowledged())
                                    {
                                    // resend the request;
                                    // we should only do this in the case when data have not changed,
                                    // which means that the latest change may not got delivered to everyone
                
                                    Object  oValueOld  = entry.getValue();
                                    Object  oValueNew  = mapInternal.get(oKey);
                                    boolean fRemoveNew = !mapInternal.containsKey(oKey);
                
                                    if (fRemove == fRemoveNew && Base.equals(oValueOld, oValueNew))
                                        {
                                        Member memberCoordinator = mapConfig.getConfigCoordinator();
                                        Map    mapUpdateNew = new LiteMap();
                
                                        mapUpdateNew.put(oKey, oValueOld);
                
                                        _trace("Service " + service.getServiceName()
                                            + ": resending ConfigRequest to member "
                                            + memberCoordinator.getId()
                                            + " for key=" + oKey
                                            + ", value=" + oValueOld, 3);
                
                                        Grid.ConfigRequest msg = (Grid.ConfigRequest)
                                                service.instantiateMessage("ConfigRequest");
                                        msg.setConfigMap(mapConfig);
                                        msg.setUpdateMap(mapUpdateNew);
                                        msg.setRemove(fRemove);
                                        msg.addToMember(memberCoordinator);
                
                                        mapPending.put(oKey, msg.ensureRequestPoll());
                                        service.post(msg); // poll
                                        }
                                    }
                                }
                            }
                        }
                    }
                super.onCompletion();
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                Grid.ConfigResponse msgResponse = (Grid.ConfigResponse) msg;
                setAcknowledged(msgResponse.isAcknowledged());
                
                super.onResponse(msg);
                }
            
            // Accessor for the property "Acknowledged"
            /**
             * Setter for property Acknowledged.<p>
            * If true, the ConfigRequest has been received by the config
            * coordinator and corresponding ConfigUpdate has been sent to all
            * service members.
             */
            protected void setAcknowledged(boolean fAcknowledged)
                {
                __m_Acknowledged = fAcknowledged;
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ConfigResponse
    
    /**
     * This Message is sent by the config coordinator Member as a reponse to
     * the ConfigRequest (update or remove) poll. It serves as a proof that the
     * corresponding ConfigUpdate has been sent to all service members.
     * 
     * Attributes:
     *     Acknowledged
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConfigResponse
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Acknowledged
         *
         * If true, the ConfigRequest has been received by the config
         * coordinator and corresponding ConfigUpdate has been sent to all
         * service members.
         */
        private boolean __m_Acknowledged;
        
        // Default constructor
        public ConfigResponse()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConfigResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-16);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigResponse();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ConfigResponse".replace('/', '.'));
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
        
        // Accessor for the property "Acknowledged"
        /**
         * Getter for property Acknowledged.<p>
        * If true, the ConfigRequest has been received by the config
        * coordinator and corresponding ConfigUpdate has been sent to all
        * service members.
         */
        public boolean isAcknowledged()
            {
            return __m_Acknowledged;
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            setAcknowledged(input.readBoolean());
            }
        
        // Accessor for the property "Acknowledged"
        /**
         * Setter for property Acknowledged.<p>
        * If true, the ConfigRequest has been received by the config
        * coordinator and corresponding ConfigUpdate has been sent to all
        * service members.
         */
        public void setAcknowledged(boolean fAcknowledged)
            {
            __m_Acknowledged = fAcknowledged;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            output.writeBoolean(isAcknowledged());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ConfigSync
    
    /**
     * This Message is sent by the config coordinator Member to a new Service
     * Member to provide the current ConfigMap contents, and also to update all
     * Members when a new Member becomes the config coordinator.
     * 
     * Attributes:
     *     SyncMap
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConfigSync
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property ConfigMap
         *
         * The config map to which this ConfigSync applies.
         */
        private transient com.tangosol.coherence.component.util.ServiceConfig.Map __m_ConfigMap;
        
        /**
         * Property SyncMap
         *
         * The config map contents for the Member sending this message.
         */
        private java.util.Map __m_SyncMap;
        
        // Default constructor
        public ConfigSync()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConfigSync(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-17);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ConfigSync".replace('/', '.'));
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
        
        // Accessor for the property "ConfigMap"
        /**
         * Getter for property ConfigMap.<p>
        * The config map to which this ConfigSync applies.
         */
        public com.tangosol.coherence.component.util.ServiceConfig.Map getConfigMap()
            {
            return __m_ConfigMap;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "SyncMap=" + getSyncMap();
            }
        
        // Accessor for the property "SyncMap"
        /**
         * Getter for property SyncMap.<p>
        * The config map contents for the Member sending this message.
         */
        public java.util.Map getSyncMap()
            {
            return __m_SyncMap;
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
            // import Component.Net.Member;
            // import Component.Util.ServiceConfig$Map as com.tangosol.coherence.component.util.ServiceConfig.Map;
            // import com.tangosol.util.Base;
            // import java.util.Iterator;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            Grid   service           = getService();
            com.tangosol.coherence.component.util.ServiceConfig.Map mapConfig         = getConfigMap();
            Member    memberFrom        = getFromMember();
            Member    memberCoordinator = mapConfig.getConfigCoordinator();
            
            if (memberCoordinator != null && memberCoordinator != memberFrom &&
                service.isAcceptingOthers())
                {
                // if the service is fully initialized, and we receive a ConfigSync
                // from a member other than the config coordinator, it can only mean
                // one of 2 things; either the (old) coordinator has left and we
                // have not been notified yet, or we are receiving a really late sync
                // message from a since-departed member
                if (service.getServiceMemberSet().contains(memberFrom))
                    {
                    // We got this update before we got notification that the sender
                    // had left the service.  Queue the sync, as well as any updates
                    // until we have been notified of the (old coordinator) member
                    // leaving.
                    mapConfig.deferConfigUpdate(this);
                    }
                else
                    {
                    // ignore messages from members no longer in the service
                    _trace("Ignoring ConfigSync from departed member " + memberFrom, 5);
                    }
                return;
                }
            
            super.onReceived();
            
            if (!service.isWelcomedBy(memberFrom))
                {
                // COH-5774: drop "early" updates. We must have received a MemberWelcome first.
                _trace("Ignoring premature ConfigSync from member " + memberFrom.getId(), 5);
                return;
                }
            
            Map mapSrc      = getSyncMap();
            Map mapInternal = mapConfig.getMap();
            if (!mapSrc.isEmpty())
                {
                _trace("Service " + service.getServiceName()
                     + ": received " + getConfigMap().getConfig().get_Name()
                     + " ConfigSync from member " + memberFrom.getId()
                     + " containing " + mapSrc.size() + " entries", 3);
                }
            
            // update local image based on the sync image (will avoid any
            // actual put() and remove() calls if no changes have occurred)
            mapInternal.keySet().retainAll(mapSrc.keySet());
            mapConfig.updateInternal(mapSrc, /*fRemove*/ false);
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Util.ServiceConfig$Map as com.tangosol.coherence.component.util.ServiceConfig.Map;
            // import java.io.IOException;
            // import java.util.HashMap;
            // import java.util.Map;
            
            Grid service = getService();
            Map     map     = new HashMap();
            
            try
                {
                com.tangosol.coherence.component.util.ServiceConfig.Map mapConfig = service.getConfigMap(input.readInt());
                int       c         = input.readInt();
            
                setConfigMap(mapConfig);
                for (int i = 0; i < c; ++i)
                    {
                    Object oKey   = mapConfig.readObject(input);
                    Object oValue = mapConfig.readObject(input);
                    map.put(oKey, oValue);
                    }
                }
            catch (IOException e)
                {
                service.onConfigIOException(e, getFromMember());
                }
            
            setSyncMap(map);
            }
        
        // Accessor for the property "ConfigMap"
        /**
         * Setter for property ConfigMap.<p>
        * The config map to which this ConfigSync applies.
         */
        public void setConfigMap(com.tangosol.coherence.component.util.ServiceConfig.Map mapConfig)
            {
            __m_ConfigMap = mapConfig;
            }
        
        // Accessor for the property "SyncMap"
        /**
         * Setter for property SyncMap.<p>
        * The config map contents for the Member sending this message.
         */
        public void setSyncMap(java.util.Map mapSync)
            {
            __m_SyncMap = mapSync;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import Component.Util.ServiceConfig$Map as com.tangosol.coherence.component.util.ServiceConfig.Map;
            // import java.util.Iterator;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            com.tangosol.coherence.component.util.ServiceConfig.Map mapConfig = getConfigMap();
            Map       mapSync   = getSyncMap();
            
            // mapSync must be a reference to the *actual* ObservableHashMap
            synchronized (mapSync)
                {
                output.writeInt(mapConfig.getMapType());
                output.writeInt(mapSync.size());
            
                for (Iterator iter = mapSync.entrySet().iterator(); iter.hasNext(); )
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
            
                    mapConfig.writeObject(output, entry.getKey());
                    mapConfig.writeObject(output, entry.getValue());
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ConfigUpdate
    
    /**
     * This Message is sent by the config coordinator Member to all other
     * Members running this service to inform them of a change to the
     * ConfigMap.
     * 
     * Attributes:
     *     Key
     *     Value
     *     Remove
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConfigUpdate
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property ConfigMap
         *
         * The config map to which this ConfigUpdate applies.
         */
        private transient com.tangosol.coherence.component.util.ServiceConfig.Map __m_ConfigMap;
        
        /**
         * Property Remove
         *
         * True if the Message indicates a delete ("remove") operation; false
         * if the Message indicates an insert or update ("put").
         */
        private boolean __m_Remove;
        
        /**
         * Property UpdateMap
         *
         * The map of keys being modified to the values they are being updated
         * to (or null if the keys are being removed).
         */
        private java.util.Map __m_UpdateMap;
        
        // Default constructor
        public ConfigUpdate()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConfigUpdate(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-18);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigUpdate();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ConfigUpdate".replace('/', '.'));
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
        
        // Accessor for the property "ConfigMap"
        /**
         * Getter for property ConfigMap.<p>
        * The config map to which this ConfigUpdate applies.
         */
        public com.tangosol.coherence.component.util.ServiceConfig.Map getConfigMap()
            {
            return __m_ConfigMap;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "UpdateMap=" + getUpdateMap() + "; Remove=" + isRemove();
            }
        
        // Accessor for the property "UpdateMap"
        /**
         * Getter for property UpdateMap.<p>
        * The map of keys being modified to the values they are being updated
        * to (or null if the keys are being removed).
         */
        public java.util.Map getUpdateMap()
            {
            return __m_UpdateMap;
            }
        
        // Accessor for the property "Remove"
        /**
         * Getter for property Remove.<p>
        * True if the Message indicates a delete ("remove") operation; false if
        * the Message indicates an insert or update ("put").
         */
        public boolean isRemove()
            {
            return __m_Remove;
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
            // import Component.Net.Member;
            // import Component.Util.ServiceConfig$Map as com.tangosol.coherence.component.util.ServiceConfig.Map;
            // import java.util.Iterator;
            // import java.util.Map;
            
            Grid   service           = getService();
            com.tangosol.coherence.component.util.ServiceConfig.Map mapConfig         = getConfigMap();
            Member    memberFrom        = getFromMember();
            Member    memberCoordinator = mapConfig.getConfigCoordinator();
            
            if (memberCoordinator != null && memberCoordinator != memberFrom)
                {
                if (service.getServiceMemberSet().contains(memberFrom))
                    {
                    // We got this update before we got notification that the sender
                    // had left the service.  Queue the update until we have been
                    // notified of the (old config coordinator) member leaving.
                    mapConfig.deferConfigUpdate(this);
                    }
                else
                    {
                    // ignore messages from members no longer in the service
                    _trace("Ignoring ConfigUpdate from departed member " + memberFrom.getId(), 5);
                    }
                return;
                }
            
            super.onReceived();
            
            if (!service.isWelcomedBy(memberFrom))
                {
                // COH-5774: drop "early" updates.  We must have received a MemberWelcome first.
                _trace("Ignoring premature ConfigSync from member " + memberFrom.getId(), 5);
                return;
                }
            
            Map mapUpdate = getUpdateMap();
            for (Iterator iter = mapUpdate.keySet().iterator(); iter.hasNext(); )
                {
                Object oKey = iter.next();
                if (mapConfig.isRequestPending(oKey))
                    {
                    // ignore the update
                    _trace("Request is pending; ignoring the ConfigUpdate " + getDescription(), 5);
                    iter.remove();
                    }
                }
            
            mapConfig.updateInternal(mapUpdate, isRemove());
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Util.ServiceConfig$Map as com.tangosol.coherence.component.util.ServiceConfig.Map;
            // import com.tangosol.util.LiteMap;
            // import java.io.IOException;
            
            Grid service = getService();
            LiteMap map     = new LiteMap();
            
            try
                {
                com.tangosol.coherence.component.util.ServiceConfig.Map mapConfig = service.getConfigMap(input.readInt());
                setConfigMap(mapConfig);
                setRemove(input.readBoolean());
            
                int cEntries  = input.readInt();
                for (int i = 0; i < cEntries; i++)
                    {
                    map.put(mapConfig.readObject(input),
                            mapConfig.readObject(input));
                    }
                }
            catch (IOException e)
                {
                getService().onConfigIOException(e, getFromMember());
                }
            
            setUpdateMap(map);
            }
        
        // Accessor for the property "ConfigMap"
        /**
         * Setter for property ConfigMap.<p>
        * The config map to which this ConfigUpdate applies.
         */
        public void setConfigMap(com.tangosol.coherence.component.util.ServiceConfig.Map mapConfig)
            {
            __m_ConfigMap = mapConfig;
            }
        
        // Accessor for the property "Remove"
        /**
         * Setter for property Remove.<p>
        * True if the Message indicates a delete ("remove") operation; false if
        * the Message indicates an insert or update ("put").
         */
        public void setRemove(boolean fRemove)
            {
            __m_Remove = fRemove;
            }
        
        // Accessor for the property "UpdateMap"
        /**
         * Setter for property UpdateMap.<p>
        * The map of keys being modified to the values they are being updated
        * to (or null if the keys are being removed).
         */
        public void setUpdateMap(java.util.Map mapUpdate)
            {
            __m_UpdateMap = mapUpdate;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import Component.Util.ServiceConfig$Map as com.tangosol.coherence.component.util.ServiceConfig.Map;
            // import java.util.Iterator;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            com.tangosol.coherence.component.util.ServiceConfig.Map mapConfig = getConfigMap();
            Grid   service   = getService();
            Map       mapUpdate = getUpdateMap();
            
            output.writeInt(mapConfig.getMapType());
            output.writeBoolean(isRemove());
            output.writeInt(mapUpdate.size());
            for (Iterator iter = mapUpdate.entrySet().iterator(); iter.hasNext(); )
                {
                java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
                mapConfig.writeObject(output, entry.getKey());
                mapConfig.writeObject(output, entry.getValue());
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$DaemonPool
    
    /**
     * DaemonPool is a class thread pool implementation for processing queued
     * operations on one or more daemon threads.
     * 
     * The designable properties are:
     *     AutoStart
     *     DaemonCount
     * 
     * The simple API for the DaemonPool is:
     *     public void start()
     *     public boolean isStarted()
     *     public void add(Runnable task)
     *     public void stop()
     * 
     * The advanced API for the DaemonPool is:
     *     DaemonCount property
     *     Daemons property
     *     Queues property
     *     ThreadGroup property
     * 
     * The DaemonPool is composed of two key components:
     * 
     * 1) An array of WorkSlot components that may or may not share Queues with
     * other WorkSlots. 
     * 
     * 2) An array of Daemon components feeding off the Queues. This collection
     * is accessed by the DaemonCount and Daemons properties, and is managed by
     * the DaemonCount mutator.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DaemonPool
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool
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
            __mapChildren.put("Daemon", Grid.DaemonPool.Daemon.get_CLASS());
            __mapChildren.put("ResizeTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.ResizeTask.get_CLASS());
            __mapChildren.put("ScheduleTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.ScheduleTask.get_CLASS());
            __mapChildren.put("StartTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.StartTask.get_CLASS());
            __mapChildren.put("StopTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.StopTask.get_CLASS());
            __mapChildren.put("WorkSlot", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.WorkSlot.get_CLASS());
            __mapChildren.put("WrapperTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.WrapperTask.get_CLASS());
            }
        
        // Default constructor
        public DaemonPool()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DaemonPool(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setAbandonThreshold(8);
                setDaemonCountMax(2147483647);
                setDaemonCountMin(1);
                setScheduledTasks(new java.util.HashSet());
                setStatsTaskAddCount(new java.util.concurrent.atomic.AtomicLong());
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DaemonPool();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$DaemonPool".replace('/', '.'));
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

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$DaemonPool$Daemon
        
        /**
         * The prototypical Daemon thread component that will belong to the
         * DaemonPool. An instance of this component is created for each thread
         * in the pool.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Daemon
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.Daemon
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Daemon()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Daemon(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setDaemonState(0);
                    setDefaultGuardRecovery(0.9F);
                    setDefaultGuardTimeout(60000L);
                    setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
                    setThreadName("Worker");
                    }
                catch (java.lang.Exception e)
                    {
                    // re-throw as a runtime exception
                    throw new com.tangosol.util.WrapperException(e);
                    }
                
                // containment initialization: children
                _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.Daemon.Guard("Guard", this, true), "Guard");
                
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DaemonPool.Daemon();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$DaemonPool$Daemon".replace('/', '.'));
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
             * Event notification called once the daemon's thread starts and
            * before the daemon thread goes into the "wait - perform" loop.
            * Unlike the <code>onInit()</code> event, this method executes on
            * the daemon's thread.
            * 
            * Note1: this method is called while the caller's thread is still
            * waiting for a notification to  "unblock" itself.
            * Note2: any exception thrown by this method will terminate the
            * thread immediately
             */
            protected void onEnter()
                {
                // import com.tangosol.application.ContainerHelper;
                
                super.onEnter();
                
                ContainerHelper.initializeThreadContext((Grid) get_Module());
                }
            
            // Declared at the super level
            /**
             * Event notification called right before the daemon thread
            * terminates. This method is guaranteed to be called only once and
            * on the daemon's thread.
             */
            protected void onExit()
                {
                ((Grid) get_Module()).flush();
                
                super.onExit();
                }
            
            // Declared at the super level
            /**
             * Event notification called when  the daemon's Thread is waiting
            * for work.
            * 
            * @see #run
             */
            protected void onWait()
                    throws java.lang.InterruptedException
                {
                ((Grid) get_Module()).flush();
                
                super.onWait();
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$DispatchEvent
    
    /**
     * Runnable event.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DispatchEvent
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DispatchEvent
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public DispatchEvent()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DispatchEvent(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchEvent();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$DispatchEvent".replace('/', '.'));
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
         * import com.tangosol.net.MemberEvent;
        * 
        * ((MemberEvent) getEvent()).dispatch(getListeners());
         */
        public void run()
            {
            // import com.tangosol.net.MemberEvent;
            // import java.util.EventObject;
            
            EventObject evt = getEvent();
            if (evt instanceof MemberEvent)
                {
                ((MemberEvent) evt).dispatch(getListeners());
                }
            else
                {
                super.run();
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$DispatchNotification
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DispatchNotification
            extends    com.tangosol.coherence.component.Util
            implements Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property MBeanName
         *
         * The MBean name.
         */
        private String __m_MBeanName;
        
        /**
         * Property Message
         *
         * The notification message.
         */
        private String __m_Message;
        
        /**
         * Property Source
         *
         * The name of the notification source.
         */
        private String __m_Source;
        
        /**
         * Property Type
         *
         * The notification type.
         */
        private String __m_Type;
        
        /**
         * Property UserData
         *
         * Payload (must be an intrinsic or an OpenType value).
         */
        private Object __m_UserData;
        
        // Default constructor
        public DispatchNotification()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DispatchNotification(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchNotification();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$DispatchNotification".replace('/', '.'));
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
        
        // Accessor for the property "MBeanName"
        /**
         * Getter for property MBeanName.<p>
        * The MBean name.
         */
        public String getMBeanName()
            {
            return __m_MBeanName;
            }
        
        // Accessor for the property "Message"
        /**
         * Getter for property Message.<p>
        * The notification message.
         */
        public String getMessage()
            {
            return __m_Message;
            }
        
        // Accessor for the property "Source"
        /**
         * Getter for property Source.<p>
        * The name of the notification source.
         */
        public String getSource()
            {
            return __m_Source;
            }
        
        // Accessor for the property "Type"
        /**
         * Getter for property Type.<p>
        * The notification type.
         */
        public String getType()
            {
            return __m_Type;
            }
        
        // Accessor for the property "UserData"
        /**
         * Getter for property UserData.<p>
        * Payload (must be an intrinsic or an OpenType value).
         */
        public Object getUserData()
            {
            return __m_UserData;
            }
        
        // From interface: java.lang.Runnable
        public void run()
            {
            // import com.tangosol.net.management.NotificationManager;
            // import com.tangosol.net.management.Registry;
            // import javax.management.Notification;
            
            Grid  service  = (Grid) get_Module();
            Registry registry = service.getCluster().getManagement();
            
            if (registry != null)
                {
                NotificationManager manager    = registry.getNotificationManager();
                String              sMBeanName = getMBeanName();
            
                if (manager.isSubscribedTo(sMBeanName))
                    {
                    Notification note = new Notification(getType(), getSource(), -1l, getMessage());
                    note.setUserData(getUserData());
            
                    manager.trigger(sMBeanName, note);
                    }
                }
            }
        
        // Accessor for the property "MBeanName"
        /**
         * Setter for property MBeanName.<p>
        * The MBean name.
         */
        public void setMBeanName(String sName)
            {
            __m_MBeanName = sName;
            }
        
        // Accessor for the property "Message"
        /**
         * Setter for property Message.<p>
        * The notification message.
         */
        public void setMessage(String sMsg)
            {
            __m_Message = sMsg;
            }
        
        // Accessor for the property "Source"
        /**
         * Setter for property Source.<p>
        * The name of the notification source.
         */
        public void setSource(String sSource)
            {
            __m_Source = sSource;
            }
        
        // Accessor for the property "Type"
        /**
         * Setter for property Type.<p>
        * The notification type.
         */
        public void setType(String sType)
            {
            __m_Type = sType;
            }
        
        // Accessor for the property "UserData"
        /**
         * Setter for property UserData.<p>
        * Payload (must be an intrinsic or an OpenType value).
         */
        public void setUserData(Object oData)
            {
            __m_UserData = oData;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$EventDispatcher
    
    /**
     * This is a Daemon component that waits for items to process from a Queue.
     * Whenever the Queue contains an item, the onNotify event occurs. It is
     * expected that sub-classes will process onNotify as follows:
     * <pre><code>
     * Object o;
     * while ((o = getQueue().removeNoWait()) != null)
     *     {
     *     // process the item
     *     // ...
     *     }
     * </code></pre>
     * <p>
     * The Queue is used as the synchronization point for the daemon.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EventDispatcher
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher
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
            __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher.Queue.get_CLASS());
            }
        
        // Default constructor
        public EventDispatcher()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public EventDispatcher(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setCloggedCount(1024);
                setCloggedDelay(32);
                setDaemonState(0);
                setDefaultGuardRecovery(0.9F);
                setDefaultGuardTimeout(60000L);
                setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher.Guard("Guard", this, true), "Guard");
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.EventDispatcher();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$EventDispatcher".replace('/', '.'));
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
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
         */
        public Grid getService()
            {
            return (Grid) get_Module();
            }
        
        // Declared at the super level
        /**
         * Event notification called once the daemon's thread starts and before
        * the daemon thread goes into the "wait - perform" loop. Unlike the
        * <code>onInit()</code> event, this method executes on the daemon's
        * thread.
        * 
        * Note1: this method is called while the caller's thread is still
        * waiting for a notification to  "unblock" itself.
        * Note2: any exception thrown by this method will terminate the thread
        * immediately
         */
        protected void onEnter()
            {
            // import com.tangosol.application.ContainerHelper;
            
            super.onEnter();
            
            ContainerHelper.initializeThreadContext((Grid) get_Module());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$Guard
    
    /**
     * Guard provides the Guardable interface implementation for the Daemon.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Guard
            extends    com.tangosol.coherence.component.util.Daemon.Guard
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
            __mapChildren.put("StopService", Grid.Guard.StopService.get_CLASS());
            }
        
        // Default constructor
        public Guard()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Guard(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Guard();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$Guard".replace('/', '.'));
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
        public void terminate()
            {
            // import com.tangosol.util.Base;
            
            Grid service = (Grid) get_Parent();
            
            // Try to stop the service.  If the Service is not stopped after
            // 1 sec, give up and consider the service failed and unrecoverable
            Thread threadStop = Base.makeThread(null, (Runnable) _newChild("StopService"), "StopService");
            threadStop.setDaemon(true);
            threadStop.start();
            
            service.sleep(1000L);
            
            if (service.getServiceState() != Grid.SERVICE_STOPPED)
                {
                try
                    {
                    // log details about the service thread and the stop thread
                    Thread threadService = service.getThread();
            
                    _trace("onServiceFailed: Failed to stop service " + service.getServiceName() + " with state=" + service.getServiceState() +
                        ", isAlive=" + (threadService != null && threadService.isAlive() ? "true" : "false") + ", stop service thread isAlive=" + threadStop.isAlive(), 1);
                    
                    if (threadService != null && threadService.isAlive())
                        {
                        // log stack trace of Service thread that failed to stop
                        _trace("onServiceFailed: Service thread: " + Base.getStackTrace(threadService), 1);
                        }
            
                    if (threadStop.isAlive())
                        {
                        _trace("onServiceFailed: Stop service thread: " + Base.getStackTrace(threadStop), 1);
                        }
                    }
                catch (Throwable ignored) {}
                service.getServiceFailurePolicy().onServiceFailed(service.getCluster());
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$Guard$StopService
        
        /**
         * StopService provides the logic to stop the associated service within
         * the Runnable interface, so that it is suitable to be run on a
         * dedicated thread.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class StopService
                extends    com.tangosol.coherence.Component
                implements Runnable
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public StopService()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public StopService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Guard.StopService();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$Guard$StopService".replace('/', '.'));
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
            
            // From interface: java.lang.Runnable
            public void run()
                {
                ((Grid) get_Module()).stop();
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$MemberConfigListener
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberConfigListener
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.util.MapListener
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public MemberConfigListener()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MemberConfigListener(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberConfigListener();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$MemberConfigListener".replace('/', '.'));
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
        
        // From interface: com.tangosol.util.MapListener
        public void entryDeleted(com.tangosol.util.MapEvent event)
            {
            onEvent(event);
            }
        
        // From interface: com.tangosol.util.MapListener
        public void entryInserted(com.tangosol.util.MapEvent event)
            {
            onEvent(event);
            }
        
        // From interface: com.tangosol.util.MapListener
        public void entryUpdated(com.tangosol.util.MapEvent event)
            {
            onEvent(event);
            }
        
        protected void onEvent(com.tangosol.util.MapEvent event)
            {
            // import com.tangosol.util.MapEvent;
            
            Grid service = (Grid) get_Module();
            
            Grid.MemberConfigUpdate msg = (Grid.MemberConfigUpdate)
                service.instantiateMessage("MemberConfigUpdate");
            
            msg.setKey(event.getKey());
            msg.setValue(event.getNewValue());
            msg.setRemove(event.getId() == MapEvent.ENTRY_DELETED);
            
            msg.setToMemberSet(service.getOthersMemberSet());
            service.send(msg);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$MemberConfigUpdate
    
    /**
     * This Message is sent to all other Members running this service to inform
     * them of a change to this Member's ConfigMap for this Service.
     * 
     * Attributes:
     *     Key
     *     Value
     *     Remove
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberConfigUpdate
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Key
         *
         * The key identifying the entry being modified.
         */
        private Object __m_Key;
        
        /**
         * Property Remove
         *
         * True if the Message indicates a delete ("remove") operation; false
         * if the Message indicates an insert or update ("put").
         */
        private boolean __m_Remove;
        
        /**
         * Property Value
         *
         * The value (may be null) to update the entry to, or null if Remove is
         * true.
         */
        private Object __m_Value;
        
        // Default constructor
        public MemberConfigUpdate()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MemberConfigUpdate(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-3);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberConfigUpdate();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$MemberConfigUpdate".replace('/', '.'));
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
            return "Key=" + getKey() + "; Value=" + getValue() + "; Remove=" + isRemove();
            }
        
        // Accessor for the property "Key"
        /**
         * Getter for property Key.<p>
        * The key identifying the entry being modified.
         */
        public Object getKey()
            {
            return __m_Key;
            }
        
        // Accessor for the property "Value"
        /**
         * Getter for property Value.<p>
        * The value (may be null) to update the entry to, or null if Remove is
        * true.
         */
        public Object getValue()
            {
            return __m_Value;
            }
        
        // Accessor for the property "Remove"
        /**
         * Getter for property Remove.<p>
        * True if the Message indicates a delete ("remove") operation; false if
        * the Message indicates an insert or update ("put").
         */
        public boolean isRemove()
            {
            return __m_Remove;
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
            // import Component.Net.Member;
            // import java.util.Map;
            
            super.onReceived();
            
            Grid service = getService();
            Member  member  = getFromMember();
            
            // must not process own-message (it should never come anyway)
            _assert(member != service.getThisMember());
            
            Map    map  = service.getServiceMemberSet().ensureMemberConfigMap(member.getId());
            Object oKey = getKey();
            
            if (isRemove())
                {
                map.remove(oKey);
                }
            else
                {
                map.put(oKey, getValue());
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import java.io.IOException;
            
            Grid service = getService();
            
            try
                {
                setKey   (service.readObject(input));
                setValue (service.readObject(input));
                setRemove(input.readBoolean());
                }
            catch (IOException e)
                {
                service.onConfigIOException(e, getFromMember());
                }
            }
        
        // Accessor for the property "Key"
        /**
         * Setter for property Key.<p>
        * The key identifying the entry being modified.
         */
        public void setKey(Object pKey)
            {
            __m_Key = pKey;
            }
        
        // Accessor for the property "Remove"
        /**
         * Setter for property Remove.<p>
        * True if the Message indicates a delete ("remove") operation; false if
        * the Message indicates an insert or update ("put").
         */
        public void setRemove(boolean pRemove)
            {
            __m_Remove = pRemove;
            }
        
        // Accessor for the property "Value"
        /**
         * Setter for property Value.<p>
        * The value (may be null) to update the entry to, or null if Remove is
        * true.
         */
        public void setValue(Object pValue)
            {
            __m_Value = pValue;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            Grid service = getService();
            
            service.writeObject(output, getKey());
            service.writeObject(output, getValue());
            output.writeBoolean(isRemove());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$MemberJoined
    
    /**
     * This Message is sent to a service when a cluster Member that previously
     * did not expose the same service now does expose the same service. In
     * other words, if cluster Members are (A, B, C), and (A, B) have a service
     * #3, and C subsequently starts a service #3, then the service #3 on (A,
     * B) will be notified that C has a service #3.
     * 
     * Note: Prior to 12.2.1 this was NotifyServiceJoined as was sent by the
     * local ClusterService.  As of 12.2.1 it is a inner service message to
     * ensure ordering with any service specific transport.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberJoined
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property MemberId
         *
         * The ID of the member who has joined.
         */
        private int __m_MemberId;
        
        // Default constructor
        public MemberJoined()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MemberJoined(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(4);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberJoined();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$MemberJoined".replace('/', '.'));
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
            return "NotifyMember=" + getFromMember();
            }
        
        // Accessor for the property "MemberId"
        /**
         * Getter for property MemberId.<p>
        * The ID of the member who has joined.
         */
        public int getMemberId()
            {
            return __m_MemberId;
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
            
            Grid service = (Grid) get_Module();
            service.onNotifyServiceJoined(service.getServiceMemberSet().getMember(getMemberId()));
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            super.read(input);
            
            setMemberId(input.readInt());
            }
        
        // Accessor for the property "MemberId"
        /**
         * Setter for property MemberId.<p>
        * The ID of the member who has joined.
         */
        public void setMemberId(int nId)
            {
            __m_MemberId = nId;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            output.writeInt(getMemberId());
            }
        }

    /**
     * This Message is used to signal that recovery has completed either as 
     * part of active persistence or snapshot recovery.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberRecovered
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----

        /**
         * Property MemberId
         *
         * The ID of the member who has recovered.
         */
        private int __m_MemberId;

        // Default constructor
        public MemberRecovered()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public MemberRecovered(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(18);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberRecovered();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$MemberRecovered".replace('/', '.'));
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
            return "MemberRecovered=" + getFromMember();
            }

        // Accessor for the property "MemberId"
        /**
         * Getter for property MemberId.<p>
         * The ID of the member who has recovered.
         */
        public int getMemberId()
            {
            return __m_MemberId;
            }

        // Declared at the super level
        public void onReceived()
            {
            super.onReceived();
            Grid   service = (Grid) get_Module();
            Member member  = service.getServiceMemberSet().getMember(getMemberId());
            service.dispatchMemberEvent(member, MemberEvent.MEMBER_RECOVERED);
            }

        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            super.read(input);

            setMemberId(input.readInt());
            }

        // Accessor for the property "MemberId"
        /**
         * Setter for property MemberId.<p>
         * The ID of the member who has recovered.
         */
        public void setMemberId(int nId)
            {
            __m_MemberId = nId;
            }

        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);

            output.writeInt(getMemberId());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$MemberWelcome
    
    /**
     * This Message is used to welcome a new member into this Service.
     * 
     * Attributes:
     *     MemberConfigMap
     *     ServiceConfigMap  (optional)
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberWelcome
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property MemberConfigMap
         *
         * The ConfigMap for the Member sending this response for this Service.
         */
        private java.util.Map __m_MemberConfigMap;
        
        /**
         * Property Rejected
         *
         * True iff this message represents a rejection from the existing
         * service member.
         */
        private boolean __m_Rejected;
        
        /**
         * Property ServiceConfigMap
         *
         * The service configuration, sent by the service senior.
         * May be null.
         */
        private java.util.Map __m_ServiceConfigMap;
        
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcome();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$MemberWelcome".replace('/', '.'));
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
            return "MemberConfigMap=" + getMemberConfigMap();
            }
        
        // Accessor for the property "MemberConfigMap"
        /**
         * Getter for property MemberConfigMap.<p>
        * The ConfigMap for the Member sending this response for this Service.
         */
        public java.util.Map getMemberConfigMap()
            {
            return __m_MemberConfigMap;
            }
        
        // Accessor for the property "ServiceConfigMap"
        /**
         * Getter for property ServiceConfigMap.<p>
        * The service configuration, sent by the service senior.
        * May be null.
         */
        public java.util.Map getServiceConfigMap()
            {
            return __m_ServiceConfigMap;
            }
        
        // Accessor for the property "Rejected"
        /**
         * Getter for property Rejected.<p>
        * True iff this message represents a rejection from the existing
        * service member.
         */
        public boolean isRejected()
            {
            return __m_Rejected;
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
            
            if (isRejected())
                {
                return;
                }
            
            Grid service = getService();
            
            service.getServiceMemberSet().
                updateMemberConfigMap(getFromMember().getId(), getMemberConfigMap());
            
            Map mapServiceConfig = getServiceConfigMap();
            if (mapServiceConfig != null)
                {
                // set the service config; this should only be sent by the service senior
                service.getServiceConfigMap().updateInternal(mapServiceConfig, false);
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Util.ServiceConfig as com.tangosol.coherence.component.util.ServiceConfig;
            // import com.tangosol.util.ObservableHashMap;
            // import java.io.IOException;
            // import java.util.HashMap;
            // import java.util.Map;
            
            boolean fRejected = input.readBoolean();
            setRejected(fRejected);
            
            if (fRejected)
                {
                return;
                }
            
            Grid service = getService();
            
            try
                {
                // read the member config map
                Map mapMemberConfig = new ObservableHashMap();
                for (int i = 0, c = input.readInt(); i < c; ++i)
                    {
                    Object oKey   = service.readObject(input);
                    Object oValue = service.readObject(input);
                    mapMemberConfig.put(oKey, oValue);
                    }
            
                Map     mapServiceConfig = null;
                boolean fServiceConfig   = input.readBoolean();
                if (fServiceConfig)
                    {
                    com.tangosol.coherence.component.util.ServiceConfig cfgService = service.getServiceConfigMap().getConfig();
            
                    mapServiceConfig = new HashMap();
            
                    // read the service config map
                    for (int i = 0, c = input.readInt(); i < c; ++i)
                        {
                        // Note: we use the Grid.ServiceConfig to deserialize the contents,
                        //       but we read the map contents into a temporary map for 
                        //       later processing (see #onReceived())
                        Object oKey   = cfgService.readObject(input);
                        Object oValue = cfgService.readObject(input);
                        mapServiceConfig.put(oKey, oValue);
                        }
                    }
            
                setMemberConfigMap (mapMemberConfig);
                setServiceConfigMap(mapServiceConfig);
                }
            catch (IOException e)
                {
                service.onConfigIOException(e, getFromMember());
                }
            }
        
        // Accessor for the property "MemberConfigMap"
        /**
         * Setter for property MemberConfigMap.<p>
        * The ConfigMap for the Member sending this response for this Service.
         */
        public void setMemberConfigMap(java.util.Map map)
            {
            __m_MemberConfigMap = map;
            }
        
        // Accessor for the property "Rejected"
        /**
         * Setter for property Rejected.<p>
        * True iff this message represents a rejection from the existing
        * service member.
         */
        public void setRejected(boolean fRejected)
            {
            __m_Rejected = fRejected;
            }
        
        // Accessor for the property "ServiceConfigMap"
        /**
         * Setter for property ServiceConfigMap.<p>
        * The service configuration, sent by the service senior.
        * May be null.
         */
        public void setServiceConfigMap(java.util.Map mapConfig)
            {
            __m_ServiceConfigMap = mapConfig;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import Component.Util.ServiceConfig as com.tangosol.coherence.component.util.ServiceConfig;
            // import java.util.Iterator;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            boolean fRejected = isRejected();
            output.writeBoolean(fRejected);
            
            if (fRejected)
                {
                return;
                }
            
            Grid service          = getService();
            Map     mapServiceConfig = getServiceConfigMap();
            Map     mapMemberConfig  = getMemberConfigMap();
            
            // write the member config map
            synchronized (mapMemberConfig)
                {
                output.writeInt(mapMemberConfig.size());
                for (Iterator iter = mapMemberConfig.entrySet().iterator(); iter.hasNext(); )
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
            
                    service.writeObject(output, entry.getKey());
                    service.writeObject(output, entry.getValue());
                    }
                }
            
            // write the service config map
            if (mapServiceConfig == null)
                {
                output.writeBoolean(false);
                }
            else
                {
                com.tangosol.coherence.component.util.ServiceConfig cfgService = service.getServiceConfigMap().getConfig();
            
                output.writeBoolean(true);
                output.writeInt(mapServiceConfig.size());
                for (Iterator iter = mapServiceConfig.entrySet().iterator(); iter.hasNext(); )
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
            
                    cfgService.writeObject(output, entry.getKey());
                    cfgService.writeObject(output, entry.getValue());
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$MemberWelcomeRequest
    
    /**
     * This Message is sent to all other Members running this service to
     * request to be "welcomed" to the service (see $MemberWelcome)
     * 
     * Attributes:
     *     SenderMemberSet
     * 
     * As of Coherence 12.1.2 this message is not used by ClusterService
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberWelcomeRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property SenderMemberSet
         *
         * The sender's view of the service member-set.  This is used to
         * enforce a "synchronized" view of the member-set during the
         * MemberWelcome protocol.
         */
        private com.tangosol.coherence.component.net.MemberSet __m_SenderMemberSet;
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
            __mapChildren.put("Poll", Grid.MemberWelcomeRequest.Poll.get_CLASS());
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$MemberWelcomeRequest".replace('/', '.'));
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
        
        // Accessor for the property "SenderMemberSet"
        /**
         * Getter for property SenderMemberSet.<p>
        * The sender's view of the service member-set.  This is used to enforce
        * a "synchronized" view of the member-set during the MemberWelcome
        * protocol.
         */
        public com.tangosol.coherence.component.net.MemberSet getSenderMemberSet()
            {
            return __m_SenderMemberSet;
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
            // import Component.Net.MemberSet;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            
            Grid service = getService();
            
            service.post(populateWelcomeMessage());
            }
        
        /**
         * Populate and return the WelcomeMember message to respond to this
        * welcome request with.
         */
        protected Grid.MemberWelcome populateWelcomeMessage()
            {
            // import Component.Net.MemberSet;
            
            Grid service = getService();
            
            _assert(service.getServiceId() > 0);
            
            Grid.MemberWelcome msgWelcome =
                (Grid.MemberWelcome) service.instantiateMessage("MemberWelcome");
            msgWelcome.respondTo(this);
            
            MemberSet setMemberThis = service.getServiceMemberSet();
            MemberSet setMemberThat = getSenderMemberSet();
            
            if (setMemberThis.size() == setMemberThat.size() &&
                setMemberThis.containsAll(setMemberThat))
                {
                // send our Member config
                msgWelcome.setMemberConfigMap(service.getThisMemberConfigMap());
            
                if (service.getThisMember() == service.getServiceOldestMember())
                    {
                    // if we are the service senior, send the Service config as well
                    msgWelcome.setServiceConfigMap(service.getServiceConfigMap());
                    }
                }
            else
                {
                StringBuilder sb = new StringBuilder();
                sb.append("Rejecting service handshake request from member ")
                  .append(getFromMember().getId())
                  .append(" due to a concurrent membership change");
            
                MemberSet setDelta = new MemberSet();
                setDelta.addAll(setMemberThis);
                setDelta.removeAll(setMemberThat);
            
                if (!setDelta.isEmpty())
                    {
                    sb.append("; requestor's member set is missing " + setDelta);
                    }
            
                setDelta.clear();
                setDelta.addAll(setMemberThat);
                setDelta.removeAll(setMemberThis);
             
                if (!setDelta.isEmpty())
                    {
                    sb.append("; requestor's member set has extra " + setDelta);
                    }
                _trace(sb.toString(), 3);
            
                // COH-4755: the service member set of the requestor does not match our
                //           member set.  Send a rejection and wait for the membership
                //           to settle.
                msgWelcome.setRejected(true);
                }
            
            return msgWelcome;
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.MemberSet;
            
            super.read(input);
            
            // deserialize the sender's memberset
            MemberSet setMembers = new MemberSet();
            setMembers.readExternal(input);
            
            setSenderMemberSet(setMembers);
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
        
        // Accessor for the property "SenderMemberSet"
        /**
         * Setter for property SenderMemberSet.<p>
        * The sender's view of the service member-set.  This is used to enforce
        * a "synchronized" view of the member-set during the MemberWelcome
        * protocol.
         */
        public void setSenderMemberSet(com.tangosol.coherence.component.net.MemberSet setMember)
            {
            // import Component.Net.MemberSet;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            
            if (setMember instanceof ServiceMemberSet)
                {
                MemberSet setSnapshot = new MemberSet();
                setSnapshot.addAll(setMember);
                setMember = setSnapshot;
                }
            
            __m_SenderMemberSet = (setMember);
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            // COH-4755: serialize the sender's service member set for comparison
            getSenderMemberSet().writeExternal(output);
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$MemberWelcomeRequest$Poll
        
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
            
            /**
             * Property RejectedMemberSet
             *
             * The set of responders that rejected the MemberWelcomeRequest.
             */
            private transient com.tangosol.coherence.component.net.MemberSet __m_RejectedMemberSet;
            
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequest.Poll();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$MemberWelcomeRequest$Poll".replace('/', '.'));
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
             * Getter for property Description.<p>
            * Used for debugging purposes (from toString). Create a
            * human-readable description of the specific Message data.
             */
            public String getDescription()
                {
                StringBuilder sb = new StringBuilder();
                
                sb.append("RejectedMemberSet=[")
                  .append(getRejectedMemberSet().getIdList())
                  .append(']');
                
                return sb.toString();
                }
            
            // Accessor for the property "RejectedMemberSet"
            /**
             * Getter for property RejectedMemberSet.<p>
            * The set of responders that rejected the MemberWelcomeRequest.
             */
            public com.tangosol.coherence.component.net.MemberSet getRejectedMemberSet()
                {
                return __m_RejectedMemberSet;
                }
            
            // Accessor for the property "Rejected"
            /**
             * Getter for property Rejected.<p>
            * (Helper) True iff this poll was rejected (requiring a repeat of
            * the member welcome protocol).
             */
            public boolean isRejected()
                {
                return !getRejectedMemberSet().isEmpty();
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            protected void onCompletion()
                {
                // import Component.Net.MemberSet;
                // import com.oracle.coherence.common.util.Duration;
                
                Grid service = getService();
                if (service.getServiceState() == Grid.SERVICE_STARTED)
                    {
                    MemberSet setLeft     = getLeftMemberSet();
                    MemberSet setRejected = getRejectedMemberSet();
                
                    // if this member became service senior, simply start
                    if (service.getThisMember() == service.getServiceOldestMember())
                        {
                        _trace("This member has become service senior. Starting " + service.getServiceName(), 3);
                        // see Service#setServiceState()
                        if (service.getServiceId() > 0)
                            {
                            service.onServiceStarted();
                            }
                        }
                    // repeat the request if there was any skew in the service member-set
                    // between the requestor and the responders during this poll
                    else if (!setRejected.isEmpty() || !setLeft.isEmpty())
                        {
                        MemberSet setCurrent = service.getServiceMemberSet();
                        MemberSet setRetry;
                        int       cPauseMillis;
                
                        StringBuilder sbMsg = new StringBuilder(
                            "Retrying service handshake request due to a concurrent membership change");
                
                        // calculate the retry member set and the wait time
                        // (this service is still starting, so we are allowed to block the service thread)
                        if (setLeft.isEmpty())
                            {
                            MemberSet setPrevious = ((Grid.MemberWelcomeRequest) get_Parent()).getSenderMemberSet();
                
                            setRetry = new MemberSet();
                            if (setPrevious.size() == setCurrent.size())
                                {
                                // our view of the membership has not changed; assuming it's correct now
                                // means that all rejecting members didn't have the latest;
                                // wait for the change to propagate to them
                                cPauseMillis = Math.min(500, 10 * setRejected.size());
                                }
                            else
                                {
                                // our view of the membership has changed; retry right away
                                cPauseMillis = 0;
                                setRetry.addAll(setCurrent);
                                setRetry.removeAll(setPrevious);
                                }
                
                            setRetry.addAll(setRejected);
                            setRetry.retainAll(service.getOthersMemberSet());
                
                            sbMsg.append("; resending the request to ")
                                 .append(setRetry);
                            }
                        else
                            {
                            // members left during the request;
                            // wait a tiny bit for the MemberLeft notification to get to everyone
                            cPauseMillis = 15;
                
                            setRetry = service.getOthersMemberSet();
                
                            sbMsg.append("; responders left ")
                                 .append(setLeft)
                                 .append("; resending the request to all ")
                                 .append(setRetry.size())
                                 .append(" service members");
                            }
                
                        if (cPauseMillis > 0)
                            {
                            sbMsg.append(" after a ")
                                 .append(new Duration(cPauseMillis * 1000000L))
                                 .append(" backoff period");
                
                            service.sleep(cPauseMillis);
                            }
                        _trace(sbMsg.toString(), 3);
                
                        Grid.MemberWelcomeRequest msg = (Grid.MemberWelcomeRequest)
                            service.instantiateMessage("MemberWelcomeRequest");
                        msg.setSenderMemberSet(setCurrent);
                        msg.setToMemberSet(setRetry);
                        service.post(msg);
                
                        // set this poll to "rejected"
                        setResult(Boolean.FALSE);
                        }
                    else
                        {
                        // see Service#setServiceState()
                        if (service.getServiceId() > 0)
                            {
                            service.onServiceStarted();
                            }
                        }
                    }
                
                super.onCompletion();
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
                // import Component.Net.MemberSet;
                
                setRejectedMemberSet(new MemberSet());
                
                super.onInit();
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                if (((Grid.MemberWelcome) msg).isRejected())
                    {
                    getRejectedMemberSet().add(msg.getFromMember());
                    }
                
                super.onResponse(msg);
                }
            
            // Accessor for the property "RejectedMemberSet"
            /**
             * Setter for property RejectedMemberSet.<p>
            * The set of responders that rejected the MemberWelcomeRequest.
             */
            protected void setRejectedMemberSet(com.tangosol.coherence.component.net.MemberSet setReject)
                {
                __m_RejectedMemberSet = setReject;
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$MemberWelcomeRequestTask
    
    /**
     * Task to run MemberWelcomeRequest.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberWelcomeRequestTask
            extends    com.tangosol.coherence.Component
            implements Runnable
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public MemberWelcomeRequestTask()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MemberWelcomeRequestTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequestTask();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$MemberWelcomeRequestTask".replace('/', '.'));
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
        
        // From interface: java.lang.Runnable
        public void run()
            {
            // use a poll to request service and member configuration maps from each service member;
            // invokes onServiceStarted from poll.onCompletion()
            Grid.MemberWelcomeRequest msg = (Grid.MemberWelcomeRequest)
                ((Grid)get_Module()).instantiateMessage("MemberWelcomeRequest");
            
            msg.setSenderMemberSet(((Grid)get_Module()).getServiceMemberSet());
            msg.setToMemberSet(((Grid)get_Module()).getOthersMemberSet());
            ((Grid)get_Module()).post(msg);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyConnectionClose
    
    /**
     * This internal Message is sent to all member aware services when an
     * extend client connection is closed or lost in ProxyService. The
     * NotifyMember refers to the extend client member leaving the
     * ProxyService.
     * 
     * Attributes:
     *     NotifyMember
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyConnectionClose
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property NotifyMember
         *
         * Extend client member that lost connection to  proxy service.
         */
        private com.tangosol.coherence.component.net.Member __m_NotifyMember;
        
        // Default constructor
        public NotifyConnectionClose()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyConnectionClose(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-24);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionClose();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyConnectionClose".replace('/', '.'));
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
         * Print description of NotifyConnectionClose event.
         */
        public String getDescription()
            {
            return "NotifyMember=" + getNotifyMember();
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Getter for property NotifyMember.<p>
        * Extend client member that lost connection to  proxy service.
         */
        public com.tangosol.coherence.component.net.Member getNotifyMember()
            {
            return __m_NotifyMember;
            }
        
        // Declared at the super level
        /**
         * When Grid Service member receives a NotifyConnectionClose event,
        * dispatch MemberEvent.MEMBER_LEFT for the connection's extend member
        * to registered MemberListeners.
         */
        public void onReceived()
            {
            // import com.tangosol.net.MemberEvent;
            // import Component.Net.Member;
            
            super.onReceived();
                        
            Grid    service = getService();
            Member  member  = getNotifyMember();
            
            service.dispatchMemberEvent(member, MemberEvent.MEMBER_LEFT);
            }
        
        // Declared at the super level
        /**
         * Serialize NotifyConnectionClose instance to output
        * 
        * @param input  buffer input to serialize to
         */
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
            
            super.read(input);
            
            Grid service = getService();
            
            setNotifyMember((com.tangosol.coherence.component.net.Member) service.readObject(input));
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Setter for property NotifyMember.<p>
        * Extend client member that lost connection to  proxy service.
         */
        public void setNotifyMember(com.tangosol.coherence.component.net.Member sProperty)
            {
            __m_NotifyMember = sProperty;
            }
        
        // Declared at the super level
        /**
         * Serialize NotifyConnectionClose instance to output.
        * 
        * @param output  serialize to this buffer output
         */
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            Grid service = getService();
            
            service.writeObject(output, getNotifyMember());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyConnectionOpen
    
    /**
     * This internal Message is sent to all member aware services  when an
     * extend client opens a connection in ProxyService. The NotifyMember
     * refers to the extend client member.
     * 
     * Attributes:
     *     NotifyMember
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyConnectionOpen
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property NotifyMember
         *
         * Extend client member that opened a connection in proxy service.
         */
        private com.tangosol.coherence.component.net.Member __m_NotifyMember;
        
        // Default constructor
        public NotifyConnectionOpen()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyConnectionOpen(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-23);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionOpen();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyConnectionOpen".replace('/', '.'));
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
         * Print description of NotifyConnectionOpen event.
         */
        public String getDescription()
            {
            return "NotifyMember=" + getNotifyMember();
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Getter for property NotifyMember.<p>
        * Extend client member that opened a connection in proxy service.
         */
        public com.tangosol.coherence.component.net.Member getNotifyMember()
            {
            return __m_NotifyMember;
            }
        
        // Declared at the super level
        /**
         * When Grid Service member receives a NotifyConnectionOpen event,
        * dispatch MemberEvent.MEMBER_JOINED for the connection's extend member
        * to registered MemberListeners.
         */
        public void onReceived()
            {
            // import com.tangosol.net.MemberEvent;
            // import Component.Net.Member;
            
            super.onReceived();
            
            Grid service = getService();
            Member  member  = getNotifyMember();
            
            service.dispatchMemberEvent(member, MemberEvent.MEMBER_JOINED);
            }
        
        // Declared at the super level
        /**
         * Deserialize NotifyConnectionOpen instance from input.
        * 
        * @param input  buffer input to deserialize from
         */
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
            
            super.read(input);
            
            Grid service = getService();
            
            setNotifyMember((com.tangosol.coherence.component.net.Member) service.readObject(input));
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Setter for property NotifyMember.<p>
        * Extend client member that opened a connection in proxy service.
         */
        public void setNotifyMember(com.tangosol.coherence.component.net.Member memberNotify)
            {
            __m_NotifyMember = memberNotify;
            }
        
        // Declared at the super level
        /**
         * Serialize NotifyConnectionOpen instance to output
        * 
        * @param input  buffer input to serialize to
         */
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            Grid service = getService();
            
            service.writeObject(output, getNotifyMember());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyMemberJoined
    
    /**
     * This internal Message is sent to all services when a new Member joins
     * the cluster.
     * 
     * Attributes:
     *     NotifyMember
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyMemberJoined
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property NotifyMember
         *
         * Member that joined the cluster.
         */
        private com.tangosol.coherence.component.net.Member __m_NotifyMember;
        
        // Default constructor
        public NotifyMemberJoined()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyMemberJoined(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-4);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberJoined();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyMemberJoined".replace('/', '.'));
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
            return "NotifyMember=" + getNotifyMember();
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Getter for property NotifyMember.<p>
        * Member that joined the cluster.
         */
        public com.tangosol.coherence.component.net.Member getNotifyMember()
            {
            return __m_NotifyMember;
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Setter for property NotifyMember.<p>
        * Member that joined the cluster.
         */
        public void setNotifyMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_NotifyMember = member;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyMemberLeaving
    
    /**
     * This internal Message is sent to all services when a Member announces
     * its impending departure from the cluster.
     * 
     * Attributes:
     *     NotifyMember
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyMemberLeaving
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property NotifyMember
         *
         * Member that is leaving the cluster.
         */
        private com.tangosol.coherence.component.net.Member __m_NotifyMember;
        
        // Default constructor
        public NotifyMemberLeaving()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyMemberLeaving(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-5);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeaving();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyMemberLeaving".replace('/', '.'));
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
            return "NotifyMember=" + getNotifyMember();
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Getter for property NotifyMember.<p>
        * Member that is leaving the cluster.
         */
        public com.tangosol.coherence.component.net.Member getNotifyMember()
            {
            return __m_NotifyMember;
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Setter for property NotifyMember.<p>
        * Member that is leaving the cluster.
         */
        public void setNotifyMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_NotifyMember = member;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyMemberLeft
    
    /**
     * This internal Message is sent to all services when a Member has departed
     * (normally or not) the cluster.
     * 
     * Attributes:
     *     NotifyMember
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyMemberLeft
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property NotifyMember
         *
         * Member that left the cluster.
         */
        private com.tangosol.coherence.component.net.Member __m_NotifyMember;
        
        // Default constructor
        public NotifyMemberLeft()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyMemberLeft(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-6);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeft();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyMemberLeft".replace('/', '.'));
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
            return "NotifyMember=" + getNotifyMember();
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Getter for property NotifyMember.<p>
        * Member that left the cluster.
         */
        public com.tangosol.coherence.component.net.Member getNotifyMember()
            {
            return __m_NotifyMember;
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Setter for property NotifyMember.<p>
        * Member that left the cluster.
         */
        public void setNotifyMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_NotifyMember = member;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyMessageReceipt
    
    /**
     * This internal Message is sent to a service to inform it that a Message
     * with the return-receipt-requested option set was delivered (or that the
     * Message was at least partially undeliverable because one or more Members
     * to which it should have been delivered are dead).  To receive this
     * notification the message of interest must have reached a state where it
     * is free for processing by all living recipients, i.e. they've also
     * acknowledged all previous messages addressed to them by this member,
     * such that message ordering will not keep the message from being
     * processed.
     * 
     * Attributes:
     *     NotifyMessage
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyMessageReceipt
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property NotifyMessage
         *
         * Message that has been published.
         */
        private com.tangosol.coherence.component.net.Message __m_NotifyMessage;
        
        // Default constructor
        public NotifyMessageReceipt()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyMessageReceipt(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-7);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMessageReceipt();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyMessageReceipt".replace('/', '.'));
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
            // import com.tangosol.util.Base;
            
            return "NotifyMessage=" + Base.indentString(getNotifyMessage().toString(),
                   "              ", false);
            }
        
        // Accessor for the property "NotifyMessage"
        /**
         * Getter for property NotifyMessage.<p>
        * Message that has been published.
         */
        public com.tangosol.coherence.component.net.Message getNotifyMessage()
            {
            return __m_NotifyMessage;
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
            // import Component.Net.Message;
            
            super.onReceived();
            
            Message msg = getNotifyMessage();
            if (msg != null)
                {
                msg.onDelivery();
                }
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
            // import Component.Net.Message;
            
            // see Grid.onMsgReceipt which optimizes out the construction of this message
            // when preprocessing is enabled and msg.preprocessSentNotification() == true
            
            Message msg = getNotifyMessage();
            return msg == null || msg.preprocessSentNotification();
            }
        
        // Accessor for the property "NotifyMessage"
        /**
         * Setter for property NotifyMessage.<p>
        * Message that has been published.
         */
        public void setNotifyMessage(com.tangosol.coherence.component.net.Message msg)
            {
            __m_NotifyMessage = msg;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyPollClosed
    
    /**
     * This internal Message is sent to a service to inform it that an
     * outstanding Poll for a departed member, or one which was sent without
     * any destination members.
     * 
     * Attributes:
     *     NotifyMember
     *     NotifyPoll
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyPollClosed
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Exception
         *
         * Exception which caused the poll to be closed on the client
         */
        private Throwable __m_Exception;
        
        /**
         * Property NotifyMember
         *
         * A departed destination member of the NotifyMessage to update (and
         * possibly close) the poll with.
         */
        private com.tangosol.coherence.component.net.Member __m_NotifyMember;
        
        /**
         * Property NotifyPoll
         *
         * The Poll to operate on.
         */
        private transient com.tangosol.coherence.component.net.Poll __m_NotifyPoll;
        
        // Default constructor
        public NotifyPollClosed()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyPollClosed(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-8);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyPollClosed();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyPollClosed".replace('/', '.'));
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
            // import com.tangosol.util.Base;
            
            return "NotifyMessage=" + Base.indentString(getNotifyPoll().toString(),
                   "              ", false);
            }
        
        // Accessor for the property "Exception"
        /**
         * Getter for property Exception.<p>
        * Exception which caused the poll to be closed on the client
         */
        public Throwable getException()
            {
            return __m_Exception;
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Getter for property NotifyMember.<p>
        * A departed destination member of the NotifyMessage to update (and
        * possibly close) the poll with.
         */
        public com.tangosol.coherence.component.net.Member getNotifyMember()
            {
            return __m_NotifyMember;
            }
        
        // Accessor for the property "NotifyPoll"
        /**
         * Getter for property NotifyPoll.<p>
        * The Poll to operate on.
         */
        public com.tangosol.coherence.component.net.Poll getNotifyPoll()
            {
            return __m_NotifyPoll;
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
            // import Component.Net.Member;
            // import Component.Net.Poll;
            
            super.onReceived();
            
            Poll poll = getNotifyPoll();
            if (poll != null)
                {
                Throwable e = getException();
                if (e != null)
                    {
                    poll.onException(e);
                    }
                
                Member memberLeft = getNotifyMember();
            
                if (memberLeft == null)
                    {
                    // the message had no destination members; close the poll
                    poll.close();
                    }
                else
                    {
                    // update the poll (which may cause it to be closed)
                    poll.onLeft(memberLeft);
                    }
                }
            }
        
        // Accessor for the property "Exception"
        /**
         * Setter for property Exception.<p>
        * Exception which caused the poll to be closed on the client
         */
        public void setException(Throwable eException)
            {
            __m_Exception = eException;
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Setter for property NotifyMember.<p>
        * A departed destination member of the NotifyMessage to update (and
        * possibly close) the poll with.
         */
        public void setNotifyMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_NotifyMember = member;
            }
        
        // Accessor for the property "NotifyPoll"
        /**
         * Setter for property NotifyPoll.<p>
        * The Poll to operate on.
         */
        public void setNotifyPoll(com.tangosol.coherence.component.net.Poll msg)
            {
            __m_NotifyPoll = msg;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyResponse
    
    /**
     * Some of the internal messages (e.g. NotifyServiceJoining) are used as
     * the inter-service request-response communications between the
     * ClusterService and other services. This message serves as a generic
     * response.
     * 
     * Unlike the Response, this message is an internal one and would never be
     * deferred.
     * 
     * Attributes:
     *     Result
     * 
     * @since Coherence 3.7.1
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyResponse
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Result
         *
         * The result of execution.
         */
        private transient Object __m_Result;
        
        // Default constructor
        public NotifyResponse()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-20);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyResponse();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyResponse".replace('/', '.'));
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
        
        // Accessor for the property "Result"
        /**
         * Getter for property Result.<p>
        * The result of execution.
         */
        public Object getResult()
            {
            return __m_Result;
            }
        
        // Accessor for the property "Result"
        /**
         * Setter for property Result.<p>
        * The result of execution.
         */
        public void setResult(Object oResult)
            {
            __m_Result = oResult;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyServiceAnnounced
    
    /**
     * This internal Message is sent to all services when a Member announces a
     * Service that has not previously been available.
     * 
     * Attributes:
     *     NotifyServiceName
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyServiceAnnounced
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property NotifyServiceName
         *
         * Name of the Service that has been announced.
         */
        private String __m_NotifyServiceName;
        
        // Default constructor
        public NotifyServiceAnnounced()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyServiceAnnounced(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-9);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceAnnounced();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyServiceAnnounced".replace('/', '.'));
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
            return "NotifyServiceName=" + getNotifyServiceName();
            }
        
        // Accessor for the property "NotifyServiceName"
        /**
         * Getter for property NotifyServiceName.<p>
        * Name of the Service that has been announced.
         */
        public String getNotifyServiceName()
            {
            return __m_NotifyServiceName;
            }
        
        // Accessor for the property "NotifyServiceName"
        /**
         * Setter for property NotifyServiceName.<p>
        * Name of the Service that has been announced.
         */
        public void setNotifyServiceName(String sName)
            {
            __m_NotifyServiceName = sName;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyServiceJoining
    
    /**
     * This internal Message is sent to a service when a cluster Member that
     * previously did not expose the same service has started the same service.
     * In other words, if cluster Members are (A, B, C), and (A, B) have a
     * service #3, and C subsequently registers a service #3, then the service
     * #3 on (A, B) will be notified that C has started service #3.
     * 
     * As of Coherence 3.7.1, this notification is a poll that is sent by the
     * ClustersService BEFORE the specified member is added to the
     * correspondning ServiceMemberSet. The service join protocol will be
     * blocked until this service closes the underlying poll.
     * 
     * Subsequent $NotifyServiceJoined notification will be sent when the
     * member has finished starting the service.
     * 
     * Attributes:
     *     NotifyMember
     *     NotifyMemberConfigMap
     *     NotifyServiceEndPointName (@since Coherence 3.7.1)
     *     NotifyServiceJoinTime
     *     NotifyServiceVersion
     *     ContinuationMessage (@since Coherence 3.7.1)
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyServiceJoining
            extends    com.tangosol.coherence.component.net.message.RequestMessage
            implements com.tangosol.net.Service.MemberJoinAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property ContinuationMessage
         *
         * A Message that is that serves as continuation for the communication
         * between the ClusterService and this Service. When all the service
         * joining-related processing is completed, the service should close
         * the poll, which will post this Message back to the ClusterService
         * queue.
         */
        private Grid.NotifyResponse __m_ContinuationMessage;
        
        /**
         * Property NotifyMember
         *
         * Member that is joining the Service.
         */
        private com.tangosol.coherence.component.net.Member __m_NotifyMember;
        
        /**
         * Property NotifyMemberConfigMap
         *
         * MemberConfigMap for the service the NotifyMember is starting.
         */
        private java.util.Map __m_NotifyMemberConfigMap;
        
        /**
         * Property NotifyServiceEndPointName
         *
         * EndPoint name for the service the NotifyMember is starting.
         */
        private String __m_NotifyServiceEndPointName;
        
        /**
         * Property NotifyServiceJoinTime
         *
         * Join timestamp for the service the NotifyMember is starting.
         */
        private long __m_NotifyServiceJoinTime;
        
        /**
         * Property NotifyServiceVersion
         *
         * The version string for the service the NotifyMember is starting.
         */
        private String __m_NotifyServiceVersion;
        
        /**
         * Property PermissionInfo
         *
         * The information needed to validate a security related (optional)
         * part of the join request.
         */
        private com.tangosol.net.security.PermissionInfo __m_PermissionInfo;
        
        // Default constructor
        public NotifyServiceJoining()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyServiceJoining(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-10);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new Grid.NotifyServiceJoining.Poll("Poll", this, true), "Poll");
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyServiceJoining".replace('/', '.'));
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
        
        // Accessor for the property "ContinuationMessage"
        /**
         * Getter for property ContinuationMessage.<p>
        * A Message that is that serves as continuation for the communication
        * between the ClusterService and this Service. When all the service
        * joining-related processing is completed, the service should close the
        * poll, which will post this Message back to the ClusterService queue.
         */
        public Grid.NotifyResponse getContinuationMessage()
            {
            return __m_ContinuationMessage;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "NotifyMember=" + getNotifyMember()
                 + "\nNotifyEndPoint=" + getNotifyServiceEndPointName();
            }
        
        // From interface: com.tangosol.net.Service$MemberJoinAction
        public com.tangosol.net.Member getJoiningMember()
            {
            return getNotifyMember();
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Getter for property NotifyMember.<p>
        * Member that is joining the Service.
         */
        public com.tangosol.coherence.component.net.Member getNotifyMember()
            {
            return __m_NotifyMember;
            }
        
        // Accessor for the property "NotifyMemberConfigMap"
        /**
         * Getter for property NotifyMemberConfigMap.<p>
        * MemberConfigMap for the service the NotifyMember is starting.
         */
        public java.util.Map getNotifyMemberConfigMap()
            {
            return __m_NotifyMemberConfigMap;
            }
        
        // Accessor for the property "NotifyServiceEndPointName"
        /**
         * Getter for property NotifyServiceEndPointName.<p>
        * EndPoint name for the service the NotifyMember is starting.
         */
        public String getNotifyServiceEndPointName()
            {
            return __m_NotifyServiceEndPointName;
            }
        
        // Accessor for the property "NotifyServiceJoinTime"
        /**
         * Getter for property NotifyServiceJoinTime.<p>
        * Join timestamp for the service the NotifyMember is starting.
         */
        public long getNotifyServiceJoinTime()
            {
            return __m_NotifyServiceJoinTime;
            }
        
        // Accessor for the property "NotifyServiceVersion"
        /**
         * Getter for property NotifyServiceVersion.<p>
        * The version string for the service the NotifyMember is starting.
         */
        public String getNotifyServiceVersion()
            {
            return __m_NotifyServiceVersion;
            }
        
        // Accessor for the property "PermissionInfo"
        /**
         * Getter for property PermissionInfo.<p>
        * The information needed to validate a security related (optional) part
        * of the join request.
         */
        public com.tangosol.net.security.PermissionInfo getPermissionInfo()
            {
            return __m_PermissionInfo;
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
            // import Component.Net.Cluster;
            // import Component.Net.Member;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            // import Component.Net.MessageHandler;
            // import Component.Net.Security;
            // import com.oracle.coherence.common.net.exabus.EndPoint;
            // import com.tangosol.net.security.PermissionInfo;
            // import java.util.Map;
            
            super.onReceived();
            
            Grid          service     = getService();
            Member           memberThis  = service.getThisMember();
            Member           memberFrom  = getNotifyMember();
            Map              mapConfig   = getNotifyMemberConfigMap();
            int              nMemberFrom = memberFrom.getId();
            ServiceMemberSet setMember   = service.getServiceMemberSet();
            Object           oReject     = null;
                
            Grid.NotifyResponse  msgResponse = (Grid.NotifyResponse)
                service.instantiateMessage("NotifyResponse");
            msgResponse.respondTo(this);
            
            if (setMember.contains(memberFrom))
                {
                // this can only mean that while we are still in the process of releasing
                // the connection to that service member (due to the service termination on it),
                // it re-started and re-initiated the service joining protocol;
                // reject the new join request
                _trace("Rejecting the ServiceJoining request processing from member " + nMemberFrom, 3);
            
                oReject = "Member " + memberThis.getId() +
                          " has not finished processing service departure notification";
                }
            
            if (oReject == null && !service.getActionPolicy().isAllowed(service, this))
                {
                // the quorum policy rejected the new member join
                String sMsg = "The request from member " + nMemberFrom
                            + " to join the service was rejected by the service action policy";
            
                _trace(sMsg, 3);
            
                oReject = new RuntimeException(sMsg);
                }
            
            Security security = service.getServiceId() > Cluster.MAX_SYSTEM_SERVICE
                ? Security.getInstance()
                : null; // no security for system services
            if (oReject == null &&
                security != null && setMember.getOldestMember() == memberThis)
                {
                PermissionInfo piRequest = getPermissionInfo();
                if (piRequest == null)
                    {
                    _trace("Request is rejected due to disabled security at " + memberFrom, 1);
                    oReject = new SecurityException();
                    }
                else
                    {
                    Object oResponse = security.processSecureRequest(memberThis, memberFrom, piRequest);
                    if (oResponse instanceof RuntimeException)
                        {
                        _trace("Security configuration mismatch or break-in attempt: " +
                            ((RuntimeException) oResponse).getMessage(), 1);
                        oReject = new SecurityException();
                        }
                    else
                        {
                        msgResponse.setResult(oResponse);
                        }
                    }
                }
            
            EndPoint peer = null;
            if (oReject == null)
                {
                peer = service.resolveEndPoint(getNotifyServiceEndPointName(), memberFrom, mapConfig);
                if (peer != null)
                    {
                    MessageHandler handler = service.getMessageHandler();
                    if (handler != null)
                        {
                        if (handler.getConnectionMap().containsKey(peer))
                            {
                            // this means that while we are still in the process of releasing
                            // the connection to that peer (due to the process termination on it),
                            // another process started and initiated the service joining protocol
                            // using the same end-point; reject the new join request
                            _trace("Rejecting the ServiceJoining request processing from member " + nMemberFrom
                                + "; EndPoint=" + peer, 3);
                            oReject = "Member " + memberThis.getId() +
                                      " has not finished processing service departure notification";
                            }
                        else if (!handler.connect(memberFrom, peer))
                            {
                            // failed to connect over bus; will use the datagram
                            peer = null;
                            }
                        }
                    }
                }
            
            if (oReject == null)
                {
                // update the service's ServiceMemberSet
                synchronized (setMember)
                    {
                    setMember.add(memberFrom);
                    setMember.setServiceVersion(nMemberFrom, getNotifyServiceVersion());
                    setMember.setServiceJoinTime(nMemberFrom, getNotifyServiceJoinTime());
                    setMember.setServiceEndPointName(nMemberFrom, peer == null ? null : peer.getCanonicalName());
                    setMember.setServiceEndPoint(nMemberFrom, peer);
                    setMember.updateMemberConfigMap(nMemberFrom, mapConfig);
                    setMember.setServiceJoining(nMemberFrom);
                    }
                }
            else
                {
                msgResponse.setResult(oReject);
                }
            
            service.post(msgResponse);
            }
        
        // Accessor for the property "ContinuationMessage"
        /**
         * Setter for property ContinuationMessage.<p>
        * A Message that is that serves as continuation for the communication
        * between the ClusterService and this Service. When all the service
        * joining-related processing is completed, the service should close the
        * poll, which will post this Message back to the ClusterService queue.
         */
        public void setContinuationMessage(Grid.NotifyResponse msg)
            {
            __m_ContinuationMessage = msg;
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Setter for property NotifyMember.<p>
        * Member that is joining the Service.
         */
        public void setNotifyMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_NotifyMember = member;
            }
        
        // Accessor for the property "NotifyMemberConfigMap"
        /**
         * Setter for property NotifyMemberConfigMap.<p>
        * MemberConfigMap for the service the NotifyMember is starting.
         */
        public void setNotifyMemberConfigMap(java.util.Map map)
            {
            __m_NotifyMemberConfigMap = map;
            }
        
        // Accessor for the property "NotifyServiceEndPointName"
        /**
         * Setter for property NotifyServiceEndPointName.<p>
        * EndPoint name for the service the NotifyMember is starting.
         */
        public void setNotifyServiceEndPointName(String sName)
            {
            __m_NotifyServiceEndPointName = sName;
            }
        
        // Accessor for the property "NotifyServiceJoinTime"
        /**
         * Setter for property NotifyServiceJoinTime.<p>
        * Join timestamp for the service the NotifyMember is starting.
         */
        public void setNotifyServiceJoinTime(long ldt)
            {
            __m_NotifyServiceJoinTime = ldt;
            }
        
        // Accessor for the property "NotifyServiceVersion"
        /**
         * Setter for property NotifyServiceVersion.<p>
        * The version string for the service the NotifyMember is starting.
         */
        public void setNotifyServiceVersion(String sVersion)
            {
            __m_NotifyServiceVersion = sVersion;
            }
        
        // Accessor for the property "PermissionInfo"
        /**
         * Setter for property PermissionInfo.<p>
        * The information needed to validate a security related (optional) part
        * of the join request.
         */
        public void setPermissionInfo(com.tangosol.net.security.PermissionInfo infoPermission)
            {
            __m_PermissionInfo = infoPermission;
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

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyServiceJoining$Poll
        
        /**
         * This Poll serves as continuation for the communication between the
         * ClusterService and this Service.
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining.Poll();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyServiceJoining$Poll".replace('/', '.'));
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
                Grid.NotifyServiceJoining msgReq = (Grid.NotifyServiceJoining) get_Parent();
                
                Grid.NotifyResponse msgContinuation = (Grid.NotifyResponse) msgReq.getContinuationMessage();
                if (msgContinuation != null)
                    {
                    msgContinuation.setResult(getResult());
                    msgContinuation.getService().send(msgContinuation);
                    }
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                Grid.NotifyResponse msgResponse = (Grid.NotifyResponse) msg;
                setResult(msgResponse.getResult());
                
                super.onResponse(msg);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyServiceLeaving
    
    /**
     * This internal Message is sent to a service when a cluster Member that
     * previously exposed the same service will no longer expose the same
     * service (potentially because the Member is leaving the cluster). In
     * other words, if cluster Members are (A, B, C) and each has a service #3,
     * and C subsequently announces that it is leaving the cluster, then the
     * service #3 on (A, B) will be notified that C will no longer have a
     * service #3.
     * 
     * Attributes:
     *     NotifyMember
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyServiceLeaving
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property NotifyMember
         *
         * Member that is stopping the Service.
         */
        private com.tangosol.coherence.component.net.Member __m_NotifyMember;
        
        // Default constructor
        public NotifyServiceLeaving()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyServiceLeaving(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-11);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeaving();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyServiceLeaving".replace('/', '.'));
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
            return "NotifyMember=" + getNotifyMember();
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Getter for property NotifyMember.<p>
        * Member that is stopping the Service.
         */
        public com.tangosol.coherence.component.net.Member getNotifyMember()
            {
            return __m_NotifyMember;
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
            // import Component.Net.Member;
            // import com.tangosol.net.MemberEvent;
            
            super.onReceived();
            
            Grid service = getService();
            Member  member  = getNotifyMember();
            
            service.getServiceMemberSet().setServiceLeaving(member.getId());
            
            service.dispatchMemberEvent(member, MemberEvent.MEMBER_LEAVING);
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Setter for property NotifyMember.<p>
        * Member that is stopping the Service.
         */
        public void setNotifyMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_NotifyMember = member;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyServiceLeft
    
    /**
     * This internal Message is sent to a service when a cluster Member that
     * previously exposed the same service now does not expose the same service
     * (potentially because the Member left the cluster). In other words, if
     * cluster Members are (A, B, C) and each has a service #3, and C
     * subsequently leaves the cluster, then the service #3 on (A, B) will be
     * notified that C no longer has a service #3.
     * 
     * Attributes:
     *     NotifyMember
     *     NotifyMemberJoined
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyServiceLeft
            extends    com.tangosol.coherence.component.net.Message
            implements com.oracle.coherence.common.base.Continuation
        {
        // ---- Fields declarations ----
        
        /**
         * Property NotifyMember
         *
         * Member that stopped the Service.
         */
        private com.tangosol.coherence.component.net.Member __m_NotifyMember;
        
        // Default constructor
        public NotifyServiceLeft()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyServiceLeft(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-12);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeft();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyServiceLeft".replace('/', '.'));
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
            return "NotifyMember=" + getNotifyMember();
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Getter for property NotifyMember.<p>
        * Member that stopped the Service.
         */
        public com.tangosol.coherence.component.net.Member getNotifyMember()
            {
            return __m_NotifyMember;
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
            // import Component.Net.Member;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            // import Component.Net.MessageHandler;
            // import com.oracle.coherence.common.net.exabus.EndPoint;
            
            super.onReceived();
            
            Grid service = getService();
            Member  member  = getNotifyMember();
            int     nMember = member.getId();
            
            _assert(nMember != service.getThisMember().getId());
            
            ServiceMemberSet setMember = service.getServiceMemberSet();
            if (setMember.getState(nMember) == ServiceMemberSet.MEMBER_JOINING)
                {
                // the join protocol was aborted by the newcomer
                _trace("Aborted join protocol for " + service.collectTransportStats(member) + "\n"
                      + service.formatStats(/*fVerbose*/ true), 3);
                }
            
            MessageHandler handler = service.getMessageHandler();
            EndPoint       peer    = setMember.getServiceEndPoint(nMember);
            
            if (peer == null || handler == null)
                {
                proceed(null);
                }
            else
                {
                service.getDepartingMembers().add(member);
            
                // if the connection is not yet "established",
                // the continuation will be called synchronously
                handler.release(peer, /*continuation*/ this);
                }
            }
        
        // From interface: com.oracle.coherence.common.base.Continuation
        /**
         * Continuation.
         */
        public void proceed(Object oResult)
            {
            // import Component.Net.Member;
            
            Grid service = (Grid) getService();
            Member  member  = getNotifyMember();
            
            _assert(Thread.currentThread() == service.getThread());
            
            service.onNotifyServiceLeft(member);
            
            // only after all service-left processing has been completed,
            // process the deferred service-left actions.
            service.processServiceLeftActions(member);
            }
        
        // Accessor for the property "NotifyMember"
        /**
         * Setter for property NotifyMember.<p>
        * Member that stopped the Service.
         */
        public void setNotifyMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_NotifyMember = member;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyServiceQuiescence
    
    /**
     * This internal Message is sent to a service when a cluster Member has
     * requested this service to be suspended or resumed (cluster-wide).
     * 
     * This notification is a poll that is sent by the ClusterService.  The
     * service quiescence protocol will be blocked until this service closes
     * the underlying poll.
     * 
     * Attributes:
     *     ContinuationMessage
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyServiceQuiescence
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property ContinuationMessage
         *
         * A Message that is that serves as continuation for the communication
         * between the ClusterService and this Service.  When all of the
         * service quiesce-related processing is completed, the service should
         * close the poll, which will post this Message back to the
         * ClusterService queue.
         */
        private Grid.NotifyResponse __m_ContinuationMessage;
        
        /**
         * Property Resume
         *
         * Flag that indicates whether the target service should be suspended
         * (false) or resumed (true).
         */
        private boolean __m_Resume;
        
        /**
         * Property ResumeOnFailover
         *
         * Flag that indicates whether the target service(s) should be resumed
         * upon failover.
         * 
         * Specifically this indicates that the suspension of a service was
         * synthetically induced and therefore should be automatically resumed
         * in the case of senior departure.
         */
        private boolean __m_ResumeOnFailover;
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
            __mapChildren.put("Poll", Grid.NotifyServiceQuiescence.Poll.get_CLASS());
            }
        
        // Default constructor
        public NotifyServiceQuiescence()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyServiceQuiescence(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-22);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyServiceQuiescence".replace('/', '.'));
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
        
        // Accessor for the property "ContinuationMessage"
        /**
         * Getter for property ContinuationMessage.<p>
        * A Message that is that serves as continuation for the communication
        * between the ClusterService and this Service.  When all of the service
        * quiesce-related processing is completed, the service should close the
        * poll, which will post this Message back to the ClusterService queue.
         */
        public Grid.NotifyResponse getContinuationMessage()
            {
            return __m_ContinuationMessage;
            }
        
        // Declared at the super level
        protected com.tangosol.coherence.component.net.Poll instantiatePoll()
            {
            // import Component.Net.Poll;
            
            return (Poll) _newChild("Poll");
            }
        
        // Accessor for the property "Resume"
        /**
         * Getter for property Resume.<p>
        * Flag that indicates whether the target service should be suspended
        * (false) or resumed (true).
         */
        public boolean isResume()
            {
            return __m_Resume;
            }
        
        // Accessor for the property "ResumeOnFailover"
        /**
         * Getter for property ResumeOnFailover.<p>
        * Flag that indicates whether the target service(s) should be resumed
        * upon failover.
        * 
        * Specifically this indicates that the suspension of a service was
        * synthetically induced and therefore should be automatically resumed
        * in the case of senior departure.
         */
        protected boolean isResumeOnFailover()
            {
            return __m_ResumeOnFailover;
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
            // import java.util.Collection;
            // import java.util.Iterator;
            // import java.util.LinkedList;
            
            Grid         service     = getService();
            boolean         fResume     = isResume();
            Grid.NotifyResponse msgResponse = (Grid.NotifyResponse) service.instantiateMessage("NotifyResponse");
            Collection      colResp     = service.getPendingQuiescenceResponses();
            
            service.onNotifyServiceQuiescence(fResume, isResumeOnFailover());
            
            msgResponse.respondTo(this);
            
            if (fResume)
                {
                if (colResp != null)
                    {
                    // resuming before we finish suspending effectively cancels the suspend
                    for (Iterator iter = colResp.iterator(); iter.hasNext(); )
                        {
                        service.post((Grid.NotifyResponse) iter.next());
                        }
                    service.setPendingQuiescenceResponses(null);
                    }
                service.post(msgResponse);
                }
            else if (!service.isAcceptingClients() || service.isSuspendedFully())
                {
                service.post(msgResponse);
                }
            else // defer reponse until all polls are closed; see Service.onInterval
                {
                if (colResp == null)
                    {
                    colResp = new LinkedList();
                    service.setPendingQuiescenceResponses(colResp);
                    }
                colResp.add(msgResponse);
                }
            }
        
        // Accessor for the property "ContinuationMessage"
        /**
         * Setter for property ContinuationMessage.<p>
        * A Message that is that serves as continuation for the communication
        * between the ClusterService and this Service.  When all of the service
        * quiesce-related processing is completed, the service should close the
        * poll, which will post this Message back to the ClusterService queue.
         */
        public void setContinuationMessage(Grid.NotifyResponse responseMessage)
            {
            __m_ContinuationMessage = responseMessage;
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
        
        // Accessor for the property "Resume"
        /**
         * Setter for property Resume.<p>
        * Flag that indicates whether the target service should be suspended
        * (false) or resumed (true).
         */
        public void setResume(boolean fResume)
            {
            __m_Resume = fResume;
            }
        
        // Accessor for the property "ResumeOnFailover"
        /**
         * Setter for property ResumeOnFailover.<p>
        * Flag that indicates whether the target service(s) should be resumed
        * upon failover.
        * 
        * Specifically this indicates that the suspension of a service was
        * synthetically induced and therefore should be automatically resumed
        * in the case of senior departure.
         */
        public void setResumeOnFailover(boolean fResume)
            {
            __m_ResumeOnFailover = fResume;
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyServiceQuiescence$Poll
        
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence.Poll();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyServiceQuiescence$Poll".replace('/', '.'));
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
                Grid.NotifyServiceQuiescence msgReq = (Grid.NotifyServiceQuiescence) get_Parent();
                
                Grid.NotifyResponse msgContinuation = (Grid.NotifyResponse) msgReq.getContinuationMessage();
                if (msgContinuation != null)
                    {
                    msgContinuation.setResult(getResult());
                    msgContinuation.getService().send(msgContinuation);
                    }
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                Grid.NotifyResponse msgResponse = (Grid.NotifyResponse) msg;
                setResult(msgResponse.getResult());
                
                super.onResponse(msg);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyShutdown
    
    /**
     * This internal Message is sent to a Service it is supposed to shut down.
     * The Service must clean up and unregister itself. Note that the only task
     * of the shut-down is to begin the process of shutting down the service;
     * technically the Service does not have to be stopped by the time the
     * shutdown Message completes its processing, although the default
     * implementation does stop it immediately.
     * 
     * Attributes:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyShutdown
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public NotifyShutdown()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyShutdown(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-13);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyShutdown();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyShutdown".replace('/', '.'));
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
            
            Grid service = getService();
            service.setServiceState(Grid.SERVICE_STOPPING);
            
            service.stop();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$NotifyStartup
    
    /**
     * This internal Message is sent to a Service when it first has been
     * started.
     * 
     * Note that this Message is the Service's only opportunity to configure
     * this Member's service-specific config map
     * (getServiceMemberSet().ensureMemberConfigMap) before other Members are
     * aware that this Member is running this Service. Changes to the map
     * during this Message's processing will be sent as part of the
     * ClusterService's ServiceJoining message.
     * 
     * Attributes:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyStartup
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public NotifyStartup()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyStartup(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-14);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyStartup();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$NotifyStartup".replace('/', '.'));
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
            
            getService().setServiceState(Grid.SERVICE_STARTED);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$PingRequest
    
    /**
     * This request is sent to "ping" a service member, illiciting a response
     * whose receipt guarantees that any in-flight messages between the
     * requestor and responder will have been flushed.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class PingRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property Continuation
         *
         * A transient (optional) continuation to invoke when the ping request
         * is answered or fails.  If the request is answered, the continuation
         * is passed Boolean.TRUE, or Boolean.FALSE otherwise.
         */
        private transient com.oracle.coherence.common.base.Continuation __m_Continuation;
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
            __mapChildren.put("Poll", Grid.PingRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public PingRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public PingRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(3);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PingRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$PingRequest".replace('/', '.'));
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
        
        // Accessor for the property "Continuation"
        /**
         * Getter for property Continuation.<p>
        * A transient (optional) continuation to invoke when the ping request
        * is answered or fails.  If the request is answered, the continuation
        * is passed Boolean.TRUE, or Boolean.FALSE otherwise.
         */
        public com.oracle.coherence.common.base.Continuation getContinuation()
            {
            return __m_Continuation;
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
            // import Component.Net.Message;
            
            Grid service = (Grid) getService();
            Message msg     = service.instantiateMessage("Response");
            msg.respondTo(this);
            
            service.post(msg);
            }
        
        // Accessor for the property "Continuation"
        /**
         * Setter for property Continuation.<p>
        * A transient (optional) continuation to invoke when the ping request
        * is answered or fails.  If the request is answered, the continuation
        * is passed Boolean.TRUE, or Boolean.FALSE otherwise.
         */
        public void setContinuation(com.oracle.coherence.common.base.Continuation continuation)
            {
            __m_Continuation = continuation;
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

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$PingRequest$Poll
        
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PingRequest.Poll();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$PingRequest$Poll".replace('/', '.'));
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
                // import com.oracle.coherence.common.base.Continuation;
                
                Continuation continuation = ((Grid.PingRequest) get_Parent()).getContinuation();
                if (continuation != null)
                    {
                    continuation.proceed(getRespondedMemberSet().isEmpty() ? Boolean.FALSE : Boolean.TRUE);
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$PollArray
    
    /**
     * A WindowedArray is an object that has attributes of a queue and a
     * dynamically resizing array.
     * 
     * The "window" is the active, or visible, portion of the virtual array.
     * Only elements within the window may be accessed or removed.
     * 
     * As elements are added, they are added to the "end" or "top" of the
     * array, dynamically resizing if necessary, and adjusting the window so
     * that it includes the new elements.
     * 
     * As items are removed, if they are removed from the "start" or "bottom"
     * of the array, the window adjusts such that those elements are no longer
     * visible.
     * 
     * The concurrent version of of the WindowedArray avoids contention for
     * threads accessing different virtual indices.
     * 
     * This is an abstract component, any concrete implementation must provide
     * assignIndexToValue and retrieveIndexFromValue methods.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class PollArray
            extends    com.tangosol.coherence.component.util.windowedArray.ConcurrentWindowedArray
        {
        // ---- Fields declarations ----
        
        /**
         * Property ExpiryMap
         *
         * PollExpiryMap holds the polls that auto-expire, keyed by the expiry
         * time.
         */
        private NullableSortedMap __m_ExpiryMap;
        
        /**
         * Property LastNullPollId
         *
         * The PollId of the last null Poll found at the start of the
         * PollArray.  If the value does not change between invocations of
         * validate() the poll is assumed to be stuck, and is removed.
         */
        private transient long __m_LastNullPollId;
        
        /**
         * Property LastNullPollIdTimeout
         *
         * The time at which the current null poll (represented by
         * LastNullPollId) will be considred an error.
         */
        private transient long __m_LastNullPollIdTimeout;
        
        /**
         * Property POLL_EXPIRY_RESOLUTION
         *
         * The resolution with which to consider poll expiry times.
         * 
         * Note: this must be a power of 2
         */
        public static final long POLL_EXPIRY_RESOLUTION = 256L;
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
            __mapChildren.put("PlaceHolder", Grid.PollArray.PlaceHolder.get_CLASS());
            }
        
        // Default constructor
        public PollArray()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public PollArray(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setExpiryMap(new NullableSortedMap());
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PollArray();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$PollArray".replace('/', '.'));
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
         * Add an Object to the WindowedArray and return the index of the new
        * element.  The value's internal index will be updated prior to storing
        * it in the array, see assignIndexToValue.
        * 
        * @param o  the Object to add to the end of the WindowedArray
        * 
        * @return the index of the element that was added
        * 
        * @throws IndexOutOfBoundsException if the WindowedArray cannot add the
        * element because the MaximumCapacity has already been reached
         */
        public long add(Object o)
            {
            // import Component.Net.Poll;
            // import java.util.Set;
            
            Poll poll = (Poll) o;
            
            _assert(poll.getService() == null && poll.getPollId() == 0L && !poll.isClosed());
            
            Grid service = (Grid) get_Module();
            
            poll.setService(service);
            poll.setTracingSpan(service.newTracingSpan("request", poll)
                .startSpan());
            
            Set setExpiry = getExpirySet(poll);
            if (setExpiry != null)
                {
                Set setExpiryPost = setExpiry;
                do
                    {
                    // do this in a loop in order to check that the
                    // set still exists in the expiry-map after we insert.
                    // This covers the case where we are adding the poll to
                    // the expiry set concurrently while another thread is
                    // expiring polls.  In the case where we interleave, we
                    // will end up creating a new expiry set (containing our
                    // poll) which will get expired during the next call to
                    // expirePolls().
                    setExpiry = setExpiryPost;
                    setExpiry.add(poll);
                    setExpiryPost = getExpirySet(poll);
                    }
                while (setExpiry != setExpiryPost);
                }
            
            return super.add(o);
            }
        
        // Declared at the super level
        /**
         * Inform the value of its virtual index prior to adding it to the
        * array.  This method may be called for the same object multiple times
        * as part of the add or set operation.
        * 
        * This method may be called while synchronization is held on the
        * associated virtual index, as such the implementation must not make
        * use of any Base.getCommonMonitor() synchronization as this will
        * result in a deadlock.
         */
        protected void assignIndexToValue(long lVirtual, Object o)
            {
            // import Component.Net.Poll;
            
            ((Poll) o).setPollId(lVirtual);
            }
        
        /**
         * Check the polls in the poll array, and perform any necessary
        * maintenance on the PollArray.
         */
        public void checkPolls()
            {
            // import com.tangosol.util.Base;
            
            long ldtNow = Base.getSafeTimeMillis();
            
            validatePolls(ldtNow);
            expirePolls  (ldtNow);
            }
        
        /**
         * Check for any polls that expired before the specified time, and close
        * them if necessary.
        * 
        * @param ldt   the time before which to close expired polls
         */
        protected void expirePolls(long ldt)
            {
            // import Component.Net.Poll;
            // import java.util.Iterator;
            // import java.util.Map$Entry as java.util.Map.Entry;
            // import java.util.NoSuchElementException;
            // import java.util.Set;

            NullableSortedMap map = (NullableSortedMap) getExpiryMap();
            
            // most of the time we will have nothing to do, so start by just checking
            // if their oldest key has yet to expire
            try
                {
                if (map.isEmpty() || ((Long) map.firstKey()).longValue() > ldt)
                    {
                    // we've handled everything that has already expired;
                    // done for now
                    return;    
                    }
                }
            catch (NoSuchElementException e)
                {
                return; // map concurrently became empty
                }
            
            // iterate over the expiry map (in chronological order)
            for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                {
                java.util.Map.Entry entry     = (java.util.Map.Entry) iter.next();
                long  ldtExpiry = ((Long) entry.getKey()).longValue();
            
                if (ldtExpiry > ldt)
                    {
                    // we've handled everything that has already expired;
                    // done for now
                    return;
                    }
            
                // remove the set from the expiry map prior to expiring the polls.
                // See also add().
                iter.remove();
            
                for (Iterator iterPoll = ((Set) entry.getValue()).iterator(); iterPoll.hasNext(); )
                    {
                    ((Poll) iterPoll.next()).close();
                    }
                }
            }
        
        // Declared at the super level
        /**
         * Obtain an Object from the WindowedArray at the specified index.
        * 
        * @param lVirtual  the index of the element to obtain
        * 
        * @return the Object at the requested index, or null if the requested
        * index is outside the bounds of the WindowedArray
        * 
        * @throws IndexOutOfBoundsException if the index is negative
         */
        public Object get(long lVirtual)
            {
            if (lVirtual == 0L)
                {
                // PollId 0 is never a real Poll
                return null;
                }
            
            // try optimistic get, and fall back on synchronized if it fails
            Object o = optimisticGet(lVirtual);
            return o == null ? super.get(lVirtual) : o;
            }
        
        // Accessor for the property "ExpiryMap"
        /**
         * Getter for property ExpiryMap.<p>
        * PollExpiryMap holds the polls that auto-expire, keyed by the expiry
        * time.
         */
        public Map getExpiryMap()
            {
            return __m_ExpiryMap;
            }
        
        /**
         * Create or return a set of polls whose expiry time is within the
        * expiry resolution of the specified poll's expiry timeout, or null if
        * the specified poll does not have an expiry time configured.
         */
        protected java.util.Set getExpirySet(com.tangosol.coherence.component.net.Poll poll)
            {
            // import com.tangosol.util.InflatableSet;
            // import java.util.Map;
            // import java.util.Set;
            
            long ldtExpiry = poll.getExpiryTimeMillis();
            if (ldtExpiry <= 0 || ldtExpiry >= Long.MAX_VALUE - POLL_EXPIRY_RESOLUTION)
                {
                // MAX_VALUE is equivalent to no-expiry
                return null;
                }
            
            // round up the expiry time to a multiple of POLL_EXPIRY_RESOLUTION
            Map  mapExpiry = getExpiryMap();
            Long LdtGroup  = Long.valueOf((ldtExpiry + POLL_EXPIRY_RESOLUTION - 1) & ~(POLL_EXPIRY_RESOLUTION - 1));
            Set  setPoll   = (Set) mapExpiry.get(LdtGroup);
            if (setPoll == null)
                {
                synchronized (mapExpiry)
                    {
                    setPoll = (Set) mapExpiry.get(LdtGroup);
                    if (setPoll == null)
                        {
                        setPoll = new InflatableSet();
                        }
                    mapExpiry.put(LdtGroup, setPoll);
                    }
                }
            
            return setPoll;
            }
        
        // Accessor for the property "LastNullPollId"
        /**
         * Getter for property LastNullPollId.<p>
        * The PollId of the last null Poll found at the start of the PollArray.
        *  If the value does not change between invocations of validate() the
        * poll is assumed to be stuck, and is removed.
         */
        public long getLastNullPollId()
            {
            return __m_LastNullPollId;
            }
        
        // Accessor for the property "LastNullPollIdTimeout"
        /**
         * Getter for property LastNullPollIdTimeout.<p>
        * The time at which the current null poll (represented by
        * LastNullPollId) will be considred an error.
         */
        public long getLastNullPollIdTimeout()
            {
            return __m_LastNullPollIdTimeout;
            }
        
        // Declared at the super level
        /**
         * Remove an Object from the WindowedArray and return the Object. Note
        * that it is necessary to remove the elements at the front of the
        * WindowedArray to allow the FirstIndex to increase, thus allowing the
        * WindowedArray to store more values without resizing the Store.  For
        * the ConcurrentWindowedArray once an index has been removed it cannot
        * be re-used.  If re-use is required then set(i, null) should be used
        * instead of the intermediate remove(i) call.
        * 
        * @param i  the index of the element to remove
        * 
        * @return the Object that was removed or null if there were no Object
        * at that index
        * 
        * @throws IndexOutOfBoundsException if the index is negative or greater
        * than the LastIndex of the WindowedArray
         */
        public Object remove(long lVirtual)
            {
            // import Component.Net.Poll;
            // import java.util.Set;
            
            Poll poll = (Poll) super.remove(lVirtual);
            if (poll != null)
                {
                Set setExpiry = getExpirySet(poll);
                if (setExpiry != null)
                    {
                    setExpiry.remove(poll);
                    }
                }
            
            return poll;
            }
        
        // Declared at the super level
        /**
         * Get the virtual index from the value.  An implementation of this
        * method must be aware of the datatype stored within the array and the
        * datatype must be able to supply the index from the value.  For
        * instance when Messages are used as values, the index is the
        * MessageId.
        * 
        * This method may be called while synchronization is held on the
        * associated virtual index, as such the implementation must not make
        * use of any Base.getCommonMonitor() synchronization as this will
        * result in a deadlock.
         */
        protected long retrieveIndexFromValue(Object o)
            {
            // import Component.Net.Poll;
            
            return ((Poll) o).getPollId();
            }
        
        // Accessor for the property "ExpiryMap"
        /**
         * Setter for property ExpiryMap.<p>
        * PollExpiryMap holds the polls that auto-expire, keyed by the expiry
        * time.
         */
        protected void setExpiryMap(Map mapExpiry)
            {
            __m_ExpiryMap = (NullableSortedMap) mapExpiry;
            }
        
        // Accessor for the property "LastNullPollId"
        /**
         * Setter for property LastNullPollId.<p>
        * The PollId of the last null Poll found at the start of the PollArray.
        *  If the value does not change between invocations of validate() the
        * poll is assumed to be stuck, and is removed.
         */
        public void setLastNullPollId(long pLastNullPollId)
            {
            __m_LastNullPollId = pLastNullPollId;
            }
        
        // Accessor for the property "LastNullPollIdTimeout"
        /**
         * Setter for property LastNullPollIdTimeout.<p>
        * The time at which the current null poll (represented by
        * LastNullPollId) will be considred an error.
         */
        protected void setLastNullPollIdTimeout(long ldtTimeout)
            {
            __m_LastNullPollIdTimeout = ldtTimeout;
            }
        
        /**
         * Check the oldest poll to make sure that the oldest poll should not
        * have already been closed, and make sure that the poll windowed array
        * gap has not grown too large.
        * 
        * @param ldtNow  the current time in milliseconds
        * 
        * Related to COH-743, COH-744
         */
        protected void validatePolls(long ldtNow)
            {
            // import Component.Net.Poll;
            // import com.tangosol.net.Guardian$GuardContext as com.tangosol.net.Guardian.GuardContext;
            
            try
                {
                // Note: we don't use a memory barrier here to enforce a clean read
                //       for performance reasons.  We check for an index inversion
                //       below, so the only other possibility is that we may miss
                //       polls for some time.
                long lFirst = getFirstIndex();
                long lLast  = getLastIndex();
            
                // if there are no polls registered, then we're done
                boolean fEmpty = lFirst > lLast;
                if (fEmpty)
                    {
                    return;
                    }
            
                // first registered poll element must never be null
                Poll poll = (Poll) get(lFirst);
                if (poll == null)
                    {
                    // This can happen on ConcurrentWindowedArray for a brief period if the addition of
                    // lFirst + 1 completes before the addition of lFirst, but will be corrected once
                    // the storage of lFirst completes.  Save the value of this null lFirst and re-check
                    // on the next pass
                    long lLastNullPollId = getLastNullPollId();
                    if (lLastNullPollId == lFirst)
                        {
                        if (getLastNullPollIdTimeout() < ldtNow)
                            {
                            remove(lFirst);
                            _trace("validatePolls: " + "Removed empty poll element #" + lFirst +
                                   " from array " + formatStats(), 1);
                            }
                        }
                    else
                        {
                        setLastNullPollId(lFirst);
                        com.tangosol.net.Guardian.GuardContext ctx = ((Grid) get_Module()).getGuardable().getContext();
                        setLastNullPollIdTimeout(ldtNow + (ctx == null ? 1000L : ctx.getSoftTimeoutMillis()));
                        }
            
                    return;
                    }
            
                Grid service = (Grid) get_Module();
            
                // ignore polls until they are at least half of the timeout delay old
                long cMillisAge = ldtNow - poll.getInitTimeMillis();
                if (cMillisAge < service.getStartupTimeout() >>> 1)
                    {
                    return;
                    }
            
                // ignore polls registered after the service has suspended
                if (poll.getPollId() > service.getSuspendPollLimit().get())
                    {
                    return;
                    }
            
                validatePollsExtra(poll, lFirst, lLast, cMillisAge, ldtNow);
                }
            catch (Throwable e)
                {
                _trace("validatePolls: " + "Non-fatal exception detected during processing:", 1);
                _trace(e);
                _trace("validatePolls: " + "Exception has been logged; continuing processing.", 1);
                }
            }
        
        /**
         * Very uncommon part of the validatePolls method (split for better
        * hotsptot optimization).
         */
        protected void validatePollsExtra(com.tangosol.coherence.component.net.Poll poll, long lFirst, long lLast, long cMillisAge, long ldtNow)
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
            // import Component.Net.Packet;
            // import com.tangosol.util.Base;
            
            // attempt to safely format the current state of the poll as a string;
            // note that the poll information could change between this point and
            // the point that we display it, since this is not synchronized
            String sPoll = poll.getClass().getName();
            try
                {
                sPoll = poll.toString();
                if (poll.get_Parent() != null)
                    {
                    sPoll += "\nRequest=" + poll.get_Parent().toString();
                    }
                }
            catch (Exception e) {}
            
            synchronized (poll)
                {
                // now that we own the synchronization, double check that no progress
                // has already been made; if it has, assume things are working
                if (lFirst != getFirstIndex())
                    {
                    return;
                    }
            
                // verify that the poll is not already closed; if it is, then it is a bug
                if (poll.isClosed())
                    {
                    remove(lFirst);
                    _trace("validatePolls: " + "Removed closed poll: " + sPoll +
                           " from array " + formatStats(), 1);
                    return;
                    }
            
                // verify that the poll does not have an empty remaining set; if it
                // does, then it is a bug
                MemberSet setRemain = poll.getRemainingMemberSet();
                if (setRemain == null || setRemain.isEmpty())
                    {
                    poll.close();
                    _trace("validatePolls: " + "Closed poll that had no remaining members: " + sPoll +
                        " within array " + formatStats(), 1);
                    return;
                    }
            
                // validate the members that remain on the poll
                Grid         service    = (Grid) get_Module();
                MemberSet       setService = service.getServiceMemberSet();
                MasterMemberSet setMaster  = service.getClusterMemberSet();
                boolean         fClose     = false;
            
                for (int nMember = setRemain.getFirstId(); nMember > 0; 
                         nMember = setRemain.getNextId(nMember))
                    {
                    if (!setService.contains(nMember))
                        {
                        Member member;
                        synchronized (setMaster)
                            {
                            // get a "clean" copy of the Member to ensure the correct
                            // timestamp and the recycled set
                            member = setMaster.getMember(nMember);
                            }
                        if (member == null)
                            {
                            member = setMaster.getRecycleSet().getMember(nMember);
                            if (member == null)
                                {
                                // can't find a member object for that member, so at this point,
                                // what else can be done but to close the poll out-right
                                fClose = setRemain.size() == 1 || cMillisAge > 60000L;
                                }
                            else if (ldtNow > member.getTimestamp() + 30000L)
                                {
                                // member has been dead for more then 30s
                                poll.onLeft(member);
                                _trace("validatePolls: "
                                     + "Removed missing member " + member + " from poll: " + sPoll +
                                     " within array " + formatStats(), 1);
                                }
                            }
                        }
                    }
            
                if (poll.isClosed())
                    {
                    return;
                    }
                else if (fClose)
                    {
                    poll.close();
                    _trace("validatePolls: " + "Closed poll that had missing member: " + sPoll +
                        " within array " + formatStats(), 1);
                    return;
                    }
            
                // collect info for the remaining member set
                StringBuilder sbRemain = new StringBuilder("\nRemaining members info:\n  {");
                for (int nMember = setRemain.getFirstId(); nMember > 0; 
                         nMember = setRemain.getNextId(nMember))
                    {
                    Member member = setMaster.getMember(nMember);
                    if (member != null)
                        {
                        sbRemain.append("\n  ")
                          .append(service.collectTransportStats(member));
                        }
                    }
                sbRemain.append("\n  }");
            
                // if the window grows too large, it gets in danger of messing
                // everything up; at this point, this member must die, the other
                // member must die, or at least the poll must be forcibly closed
                long cGap = lLast - lFirst;
                if (cGap > (Packet.TRINT_MAX_VARIANCE >>> 1))
                    {
                    // TODO: kill itself? the responder?
                    poll.close();
                    _trace("validatePolls: "
                         + "Manual intervention is required to stop " + "this node or "
                         + "the members that have not responded to this "
                         + "poll (gap size=" + cGap + "): " + sPoll + sbRemain
                         + " within array " + formatStats(), 1);
                    }
                else if (service.getServiceState() == Grid.SERVICE_STOPPED && !service.isAcceptingClients())
                    {
                    // quite likely the unanswered poll is a reason for the service termination
                    // (e.g. failure to respond to the MemberWelcomeRequest poll)
                    _trace("validatePolls: "
                        + "This service timed-out due to unanswered handshake request. "
                        + "Manual intervention is required to stop "
                        + "the members that have not responded to this " + sPoll + sbRemain + "\n"
                        + service.formatStats(/*fVerbose*/ true), 1);
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$PollArray$PlaceHolder
        
        /**
         * A PlaceHolder represents a value of null and is used to mark the
         * virtual index assigned to an actual index within the storage array.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class PlaceHolder
                extends    com.tangosol.coherence.component.util.windowedArray.ConcurrentWindowedArray.PlaceHolder
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public PlaceHolder()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public PlaceHolder(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setVirtualOffset(-1L);
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PollArray.PlaceHolder();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$PollArray$PlaceHolder".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ProtocolContext
    
    /**
     * A description of the service's current protocol context.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ProtocolContext
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.net.internal.ProtocolAwareStream.ProtocolContext
        {
        // ---- Fields declarations ----
        
        /**
         * Property Message
         *
         * The message to extract context from.
         */
        private com.tangosol.coherence.component.net.Message __m_Message;
        
        // Default constructor
        public ProtocolContext()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ProtocolContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ProtocolContext();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ProtocolContext".replace('/', '.'));
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
        
        // From interface: com.tangosol.net.internal.ProtocolAwareStream$ProtocolContext
        public com.tangosol.net.Member getFromMember()
            {
            return getMessage().getFromMember();
            }
        
        // Accessor for the property "Message"
        /**
         * Getter for property Message.<p>
        * The message to extract context from.
         */
        public com.tangosol.coherence.component.net.Message getMessage()
            {
            return __m_Message;
            }
        
        // From interface: com.tangosol.net.internal.ProtocolAwareStream$ProtocolContext
        public com.tangosol.net.Service getService()
            {
            return getMessage().getService();
            }
        
        // From interface: com.tangosol.net.internal.ProtocolAwareStream$ProtocolContext
        public java.util.Set getToMemberSet()
            {
            // import Component.Net.Message;
            // import Component.Net.Message.DiscoveryMessage;
            // import java.util.Collections;
            // import java.util.Set;
            
            Message msg = getMessage();
            
            Set setTo = msg.getToMemberSet();
            if (setTo == null && msg instanceof DiscoveryMessage)
                {
                setTo = Collections.singleton(((DiscoveryMessage) msg).getToMember());
                }
            
            return setTo;
            }
        
        // From interface: com.tangosol.net.internal.ProtocolAwareStream$ProtocolContext
        public boolean isClusterService()
            {
            return getMessage().getService().getServiceId() == 0;
            }
        
        // From interface: com.tangosol.net.internal.ProtocolAwareStream$ProtocolContext
        public boolean isInductionMessage()
            {
            // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService$NewMemberRequestIdReply as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberRequestIdReply;
            
            return getMessage() instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberRequestIdReply;
            }
        
        // Accessor for the property "Message"
        /**
         * Setter for property Message.<p>
        * The message to extract context from.
         */
        public void setMessage(com.tangosol.coherence.component.net.Message pMessage)
            {
            __m_Message = pMessage;
            }
        
        // Declared at the super level
        public String toString()
            {
            return getMessage().toString();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ReceiveQueue
    
    /**
     * SingleConsumerQueue is a concurrent queue optimized for multi producer
     * single consumer workloads.  More specifically it is not safe to consume
     * from multiple threads.
     * 
     * The Grid$ReceiveQueue is a concrete implementation holding Message
     * object.  The virtual index is stored within the Message ToMessageId
     * property.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ReceiveQueue
            extends    com.tangosol.coherence.component.util.queue.SingleConsumerQueue
        {
        // ---- Fields declarations ----
        
        /**
         * Property PreprocessingEnabled
         *
         * True if preprocessing optimizations are allowed.  Controlled via
         * tangosol.coherence.grid.preprocess.
         * 
         * @see onInit
         */
        private transient boolean __m_PreprocessingEnabled;
        
        // Default constructor
        public ReceiveQueue()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ReceiveQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setBatchSize(1);
                setDelegate(new com.oracle.coherence.common.collections.ConcurrentLinkedQueue());
                setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ReceiveQueue();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ReceiveQueue".replace('/', '.'));
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
         * Appends the specified element to the end of this queue.
        * 
        * Queues may place limitations on what elements may be added to this
        * Queue.  In particular, some Queues will impose restrictions on the
        * type of elements that may be added. Queue implementations should
        * clearly specify in their documentation any restrictions on what
        * elements may be added.
        * 
        * @param oElement element to be appended to this Queue
        * 
        * @return true (as per the general contract of the Collection.add
        * method)
        * 
        * @throws ClassCastException if the class of the specified element
        * prevents it from being added to this Queue
         */
        public boolean add(Object oElement)
            {
            // import Component.Net.Message;
            
            return (isPreprocessingEnabled() && ((Message) oElement).preprocess()) || super.add(oElement);
            }
        
        // Accessor for the property "PreprocessingEnabled"
        /**
         * Getter for property PreprocessingEnabled.<p>
        * True if preprocessing optimizations are allowed.  Controlled via
        * tangosol.coherence.grid.preprocess.
        * 
        * @see onInit
         */
        public boolean isPreprocessingEnabled()
            {
            return __m_PreprocessingEnabled;
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
            
            setPreprocessingEnabled(Boolean.parseBoolean(Config.getProperty("coherence.grid.preprocess", "true")));
            setBatchSize(Integer.parseInt(Config.getProperty("coherence.service.batch", "8")));
            
            super.onInit();
            }
        
        // Accessor for the property "PreprocessingEnabled"
        /**
         * Setter for property PreprocessingEnabled.<p>
        * True if preprocessing optimizations are allowed.  Controlled via
        * tangosol.coherence.grid.preprocess.
        * 
        * @see onInit
         */
        protected void setPreprocessingEnabled(boolean fEnabled)
            {
            __m_PreprocessingEnabled = fEnabled;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$Response
    
    /**
     * Response is a Message component used to respond to generic request
     * messages, carrying a value and return code.
     * 
     * Attributes:
     *     Result
     *     Value
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Response
            extends    com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public Response()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Response(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(2);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Response();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$Response".replace('/', '.'));
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
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ServiceConfig
    
    /**
     * ServiceConfig provides a service-wide configuration map.  All updates to
     * a service config are published service-wide by the configuration
     * coordinator.
     * The Service-wide config map for service-related shared state.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceConfig
            extends    com.tangosol.coherence.component.util.ServiceConfig
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ServiceConfig()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceConfig(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                // identified pendingPolls required thread-safe data structure and
                // verified that pendingConfigUpdates only accessed on single service thread, see details in COH-30132.
                setPendingConfigUpdates(new java.util.LinkedList());
                setPendingPolls(new ConcurrentHashMap<>());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new com.tangosol.coherence.component.util.ServiceConfig.ConfigListener("ConfigListener", this, true), "ConfigListener");
            _addChild(new Grid.ServiceConfig.Map("Map", this, true), "Map");
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ServiceConfig".replace('/', '.'));
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

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ServiceConfig$Map
        
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
                __mapChildren.put("EntrySet", Grid.ServiceConfig.Map.EntrySet.get_CLASS());
                __mapChildren.put("KeySet", Grid.ServiceConfig.Map.KeySet.get_CLASS());
                __mapChildren.put("Values", Grid.ServiceConfig.Map.Values.get_CLASS());
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig.Map();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ServiceConfig$Map".replace('/', '.'));
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
                return Grid.CONFIG_MAP_SERVICE;
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ServiceConfig$Map$EntrySet
            
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
                    __mapChildren.put("Entry", Grid.ServiceConfig.Map.EntrySet.Entry.get_CLASS());
                    __mapChildren.put("Iterator", Grid.ServiceConfig.Map.EntrySet.Iterator.get_CLASS());
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig.Map.EntrySet();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ServiceConfig$Map$EntrySet".replace('/', '.'));
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

                // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ServiceConfig$Map$EntrySet$Entry
                
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
                        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig.Map.EntrySet.Entry();
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
                            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ServiceConfig$Map$EntrySet$Entry".replace('/', '.'));
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

                // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ServiceConfig$Map$EntrySet$Iterator
                
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
                        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig.Map.EntrySet.Iterator();
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
                            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ServiceConfig$Map$EntrySet$Iterator".replace('/', '.'));
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

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ServiceConfig$Map$KeySet
            
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
                    __mapChildren.put("Iterator", Grid.ServiceConfig.Map.KeySet.Iterator.get_CLASS());
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig.Map.KeySet();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ServiceConfig$Map$KeySet".replace('/', '.'));
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

                // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ServiceConfig$Map$KeySet$Iterator
                
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
                        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig.Map.KeySet.Iterator();
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
                            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ServiceConfig$Map$KeySet$Iterator".replace('/', '.'));
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

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ServiceConfig$Map$Values
            
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
                    __mapChildren.put("Iterator", Grid.ServiceConfig.Map.Values.Iterator.get_CLASS());
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig.Map.Values();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ServiceConfig$Map$Values".replace('/', '.'));
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

                // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$ServiceConfig$Map$Values$Iterator
                
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
                        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig.Map.Values.Iterator();
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
                            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$ServiceConfig$Map$Values$Iterator".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid$WrapperGuardable
    
    /**
     * WrapperGuardable is used to encapsulate a Guardable object and associate
     * it with this Service.
     * 
     * WrapperGuardables are used to allow the ServiceFailurePolicy to specify
     * service-specific recovery, termination, or service-termination failure
     * logic.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class WrapperGuardable
            extends    com.tangosol.coherence.Component
            implements com.tangosol.net.Guardable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Guardable
         *
         * The wrapped guardable
         */
        private com.tangosol.net.Guardable __m_Guardable;
        
        // Default constructor
        public WrapperGuardable()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public WrapperGuardable(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.WrapperGuardable();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/Grid$WrapperGuardable".replace('/', '.'));
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
        public boolean equals(Object obj)
            {
            // import com.tangosol.util.Base;
            
            return Base.equals(obj instanceof Grid.WrapperGuardable
                        ? ((Grid.WrapperGuardable)obj).getGuardable()
                        : obj
                  , getGuardable());
            }
        
        // From interface: com.tangosol.net.Guardable
        public com.tangosol.net.Guardian.GuardContext getContext()
            {
            return getGuardable().getContext();
            }
        
        // Accessor for the property "Guardable"
        /**
         * Getter for property Guardable.<p>
        * The wrapped guardable
         */
        public com.tangosol.net.Guardable getGuardable()
            {
            return __m_Guardable;
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
        * The Grid service associated with this WrapperGuardable.
         */
        public Grid getService()
            {
            return (Grid) get_Module();
            }
        
        // Accessor for the property "ServiceFailurePolicy"
        /**
         * Getter for property ServiceFailurePolicy.<p>
        * The service failure policy configured on the associated service.
         */
        public com.tangosol.net.ServiceFailurePolicy getServiceFailurePolicy()
            {
            return getService().getServiceFailurePolicy();
            }
        
        // Declared at the super level
        public int hashCode()
            {
            return getGuardable().hashCode();
            }
        
        // From interface: com.tangosol.net.Guardable
        public void recover()
            {
            // import Component.Net.Cluster;
            
            Grid service = (Grid) getService();
            
            // notify the Cluster object that there is a soft-timeout happening
            ((Cluster) service.getCluster()).onGuardableRecover();
            
            reportOutstandingPolls();
            
            service.reportTransportStats();
            
            _trace(service.formatStats(/*fVerbose*/ true), 5);
            
            getServiceFailurePolicy().onGuardableRecovery(getGuardable(), service);
            }
        
        /**
         * Generate a human readable report on outstanding polls.
         */
        protected void reportOutstandingPolls()
            {
            // import com.tangosol.net.Guardian$GuardContext as com.tangosol.net.Guardian.GuardContext;
            // import java.util.Iterator;
            // import java.util.List;
            
            com.tangosol.net.Guardian.GuardContext ctx            = getContext();
            long    cTimeoutMillis = ctx == null
                ? getService().getDefaultGuardTimeout()
                : ctx.getSoftTimeoutMillis();
            
            List listPolls = getService().getOutstandingPolls(cTimeoutMillis >> 1);
            if (listPolls.isEmpty())
                {
                return;
                }
            
            StringBuilder sb = new StringBuilder("\n\n *** Outstanding polls: ***");
            for (Iterator iter = listPolls.iterator(); iter.hasNext()
                    && sb.length() < 32000;) // limit the output to 30K 
                {
                sb.append('\n')
                  .append(iter.next());
                }
            
            sb.append("\n *** Service Stats: ***\n")
              .append(getService().formatStats(/*fVerbose*/ true));
            
            _trace(sb.toString(), 1);
            }
        
        // From interface: com.tangosol.net.Guardable
        public void setContext(com.tangosol.net.Guardian.GuardContext context)
            {
            getGuardable().setContext(context);
            }
        
        // Accessor for the property "Guardable"
        /**
         * Setter for property Guardable.<p>
        * The wrapped guardable
         */
        public void setGuardable(com.tangosol.net.Guardable guardable)
            {
            __m_Guardable = guardable;
            }
        
        // From interface: com.tangosol.net.Guardable
        public void terminate()
            {
            // import Component.Net.Cluster;
            
            Grid service = (Grid) getService();
            
            // notify the Cluster object that there is a hard-timeout happening
            ((Cluster) service.getCluster()).onGuardableTerminate();
            
            reportOutstandingPolls();
            
            service.reportTransportStats();
            
            _trace(service.formatStats(/*fVerbose*/ true), 5);
            
            getServiceFailurePolicy().onGuardableTerminate(getGuardable(), service);
            }
        
        // Declared at the super level
        public String toString()
            {
            StringBuilder sb = new StringBuilder();
            
            sb.append("{WrapperGuardable ")
              .append(getGuardable().toString())
              .append(" Service=")
              .append(getService())
              .append('}');
            
            return sb.toString();
            }
        }
    }
