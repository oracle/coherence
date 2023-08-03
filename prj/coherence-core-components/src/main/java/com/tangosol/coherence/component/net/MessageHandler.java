
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.MessageHandler

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.coherence.component.net.message.BusEventMessage;
import com.tangosol.coherence.component.util.Queue;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.internal.net.socketbus.AbstractSocketBus;
import com.oracle.coherence.common.internal.util.HeapDump;
import com.oracle.coherence.common.io.BufferManagers;
import com.oracle.coherence.common.io.BufferSequence;
import com.oracle.coherence.common.io.Buffers;
import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.Event;
import com.oracle.coherence.common.net.exabus.MessageBus;
import com.oracle.coherence.common.util.Duration;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.io.BufferSequenceWriteBufferPool;
import com.tangosol.io.MultiBufferReadBuffer;
import com.tangosol.io.MultiBufferWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.nio.ByteBufferReadBuffer;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.util.Base;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLException;

/**
 * MessageHandler is an adapter between the Grid services and the MessageBus.
 * 
 * The MessageHandler may be used in one of two modes:
 * 
 * Service dedicated mode.  In this mode the Handler resides within a service
 * and is used only for exchanging messages with other members of the same
 * service.
 * 
 * Shared mode.  In this mode the Handler resides on the TransportService and
 * other services make use of it via Service.getMessagePublisher().  In this
 * mode it becomes very important to differentiate between
 * handler.getService().getMemberSet() and msg.getService().getMemberSet(), as
 * the former indicates who you can send to on this transport, while the later
 * indicates who you can send to on any transport.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class MessageHandler
        extends    com.tangosol.coherence.component.Net
        implements com.tangosol.internal.util.MessagePublisher
    {
    // ---- Fields declarations ----
    
    /**
     * Property BufferManager
     *
     * The BufferManager to use for message serialization.
     */
    private com.oracle.coherence.common.io.BufferManager __m_BufferManager;
    
    /**
     * Property Closing
     *
     * True once the bus has started closing, and true thereafter.
     * 
     * @volatile
     */
    private volatile transient boolean __m_Closing;
    
    /**
     * Property ConnectionMap
     *
     * Map<EndPoint, $Connection>
     */
    private transient java.util.Map __m_ConnectionMap;
    
    /**
     * Property DeliveryTimeoutMillis
     *
     * The message delivery timeout.
     */
    private long __m_DeliveryTimeoutMillis;
    
    /**
     * Property DisconnectCounter
     *
     * Represents the number of connections for which a DISCONNECT event has
     * been received, but for which a RELEASE event is still pending.  This
     * provides a cheap means to determine if a RECEIPT indicates successful
     * delivery, i.e. if the count is 0 then it must be a success, where as a
     * non-zero count means it is in doubt.  This approach is used as opposed
     * to tracking state on the Connection because that would require
     * Connection lookup, which is a more expensive operation, and most of the
     * time the counter will be zero.
     */
    private java.util.concurrent.atomic.AtomicLong __m_DisconnectCounter;
    
    /**
     * Property EstimateMessageSize
     *
     * If true then the MessageHandler will expend extra CPU in an attempt to
     * estimate the serialized size of a message.  Controlled by the
     * tangosol.coherence.estimateBusMessageSize system property.
     */
    private static transient boolean __s_EstimateMessageSize;
    
    /**
     * Property EventCollector
     *
     * The Collector used to receive events from the MessageBus.
     */
    private com.oracle.coherence.common.base.Collector __m_EventCollector;
    
    /**
     * Property GlobalBacklog
     *
     * Flag indicating if the MessageBus has declared a backlog which is not
     * associated with a specific peer.
     */
    private boolean __m_GlobalBacklog;
    
    /**
     * Property GlobalBacklogMonitor
     *
     * Monitor to wait on in case of a global backlog
     */
    private Object __m_GlobalBacklogMonitor;
    
    /**
     * Property HungConnectionIPs
     *
     * The set Map<InetAddr, null> of IPs for hung connections.  
     * Hung is identified as Connections which are in a DISCONNECTED state and
     * awaiting the corresponding MemberLeft notification.
     * 
     * Note, we only loosly track the IPs, specifically once one member from
     * the IP unregisters we remove the IP from the map without accounting for
     * any other members on that IP which may still be hung.  The overal intent
     * is to ensure that if we lose communication with many members we will
     * self terminate, and this is still preserved.
     */
    private com.tangosol.util.SafeHashMap __m_HungConnectionIPs;
    
    /**
     * Property IncomingQueue
     *
     * The queue for messages being sent TO the service.
     */
    private com.tangosol.coherence.component.util.Queue __m_IncomingQueue;
    
    /**
     * Property LocalBacklog
     *
     * Flag indicating if the MessageBus has declared a backlog associated with
     * this peer.
     */
    private boolean __m_LocalBacklog;
    
    /**
     * Property MessageBus
     *
     * The MessageBus this MessageHandler is associated with.
     */
    private com.oracle.coherence.common.net.exabus.MessageBus __m_MessageBus;
    
    /**
     * Property ParentMessagePublisher
     *
     * The MessagePublisher to hand off messages to in the case that a
     * connection does not exist in this handler.
     */
    private com.tangosol.internal.util.MessagePublisher __m_ParentMessagePublisher;
    
    /**
     * Property PendingParentFlush
     *
     * True iff there is a pending flush to the ParentMessagePublisher.
     */
    private java.util.concurrent.atomic.AtomicBoolean __m_PendingParentFlush;
    
    /**
     * Property Service
     *
     * The Service this MessageHandler is associated with.
     */
    private com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid __m_Service;
    
    /**
     * Property StatsBacklogDirect
     *
     * The number of times the direct connection backlog was announced since
     * the last time the statistics were reset.
     */
    private long __m_StatsBacklogDirect;
    
    /**
     * Property StatsBacklogGlobal
     *
     * The number of times the global backlog was announced since the last time
     * the statistics were reset.
     */
    private long __m_StatsBacklogGlobal;
    
    /**
     * Property StatsBacklogLocal
     *
     * The number of times the local backlog was announced since the last time
     * the statistics were reset.
     */
    private long __m_StatsBacklogLocal;
    
    /**
     * Property StatsBusBytesIn
     *
     * The number of bytes received on the bus since the last time the
     * statistics were reset.
     */
    private transient long __m_StatsBusBytesIn;
    
    /**
     * Property StatsBusBytesOut
     *
     * The number of bytes sent out by the bus since the last time the
     * statistics were reset.
     */
    private transient long __m_StatsBusBytesOut;
    
    /**
     * Property StatsBusBytesOutBuffered
     *
     * The number of bytes "buffered" by the bus for outgoing sends.  This is
     * the number of bytes for which we are waiting on receipts before
     * releasing.
     */
    private java.util.concurrent.atomic.AtomicLong __m_StatsBusBytesOutBuffered;
    
    /**
     * Property StatsBusReceives
     *
     * The number of messages received via the bus since the last time the
     * statistics were reset.
     */
    private long __m_StatsBusReceives;
    
    /**
     * Property StatsBusSends
     *
     * The number of messages sent via the bus since the last time the
     * statistics were reset.
     */
    private long __m_StatsBusSends;
    
    /**
     * Property StatsDrainOverflowDuration
     *
     * Accumulated time spend in drainOverflow.
     */
    private java.util.concurrent.atomic.LongAdder __m_StatsDrainOverflowDuration;
    private static com.tangosol.util.ListMap __mapChildren;
    
    private static void _initStatic$Default()
        {
        __initStatic();
        }
    
    // Static initializer (from _initStatic)
    static
        {
        // import com.tangosol.coherence.config.Config;
        
        _initStatic$Default();
        
        setEstimateMessageSize(Boolean.parseBoolean(Config.getProperty(
            "coherence.estimateBusMessageSize", "true")));
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Connection", MessageHandler.Connection.get_CLASS());
        }
    
    // Default constructor
    public MessageHandler()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public MessageHandler(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setConnectionMap(new com.tangosol.util.SafeHashMap());
            setGlobalBacklogMonitor(new java.lang.Object());
            setStatsBusBytesOutBuffered(new java.util.concurrent.atomic.AtomicLong());
            setStatsDrainOverflowDuration(new java.util.concurrent.atomic.LongAdder());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new MessageHandler.EventCollector("EventCollector", this, true), "EventCollector");
        
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
            __m_DisconnectCounter = new java.util.concurrent.atomic.AtomicLong();
            __m_HungConnectionIPs = new com.tangosol.util.SafeHashMap();
            __m_PendingParentFlush = new java.util.concurrent.atomic.AtomicBoolean();
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
        return new com.tangosol.coherence.component.net.MessageHandler();
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
            clz = Class.forName("com.tangosol.coherence/component/net/MessageHandler".replace('/', '.'));
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
    
    /**
     * Validate an assumption that a connection that corresponds to the
    * specified EndPoint has been released.
     */
    protected boolean checkReleased(com.oracle.coherence.common.net.exabus.EndPoint peer)
        {
        MessageHandler.Connection connect = (MessageHandler.Connection) getConnectionMap().get(peer);
        
        return connect == null || connect.isReleased();
        }
    
    /**
     * Close all resources associated with the MessageBus
     */
    public void close()
        {
        // import Component.Net.Message.BusEventMessage;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Queue;
        
        synchronized (this)
            {
            if (!isClosing())
                {
                setClosing(true);
                getMessageBus().close();
        
                // drain the Message queue until the CLOSE event is received
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
                Queue   queue   = getIncomingQueue();
        
                while (true)
                    {
                    Message msg = (Message) queue.remove();
        
                    // since we are closing anyway, there is no reason
                    // to process anything except the state-change bus events
                    if (msg instanceof BusEventMessage)
                        {
                        switch (((BusEventMessage) msg).getEvent().getType().ordinal())
                            {
                            case MessageHandler.EventCollector.ET_DISCONNECT:
                            case MessageHandler.EventCollector.ET_RELEASE:
                                service.onMessage(msg);
                                break;
        
                            case MessageHandler.EventCollector.ET_CLOSE:
                                service.onMessage(msg);
                                return; // end of queue
        
                            default:
                                // ignore the message
                                break;
                            }
                        }
                    else if (msg.isDeserializationRequired())
                        {
                        msg.releaseIncoming();
                        }
                    }
                }
            }
        }
    
    public int compareImportance(Member member)
        {
        return getService().compareImportance(member);
        }
    
    /**
     * Create a connection to the specified member. Called on the service thread
    * when either 
    *   (a) the specified Member is announced to join this Service; or 
    *   (b) when this Service has completed the start sequence (see
    * connectAll). 
    * It's important to note that in the case (a) the ServiceMemberSet does not
    * contain this new Member yet.
    * 
    * @param member  the member to connect to
    * @param peer  the corresponding EndPoint
    * 
    * @return true iff the connection is successfully initiated
     */
    public boolean connect(Member member, com.oracle.coherence.common.net.exabus.EndPoint peer)
        {
        // import java.util.Map;
        
        Map mapConnect = getConnectionMap();
        
        _assert(member != null && peer != null);
        _assert(!mapConnect.containsKey(peer), "Connect request out of order");
        
        MessageHandler.Connection connect = instantiateConnection(peer, member);
        connect.setState(MessageHandler.Connection.STATE_CONNECTING);
        
        mapConnect.put(peer, connect);
        
        try
            {
            getMessageBus().connect(peer);
            }
        catch (IllegalArgumentException e)
            {
            // incompatible bus protocol
            _trace("Unable to connect to " + peer + " using " + getMessageBus().getLocalEndPoint() +
                " (" + e.getMessage() + "), falling back on default cluster transport", 2);
        
            mapConnect.remove(peer);
            return false;
            }
        
        _trace("Registered " + connect, 6);
        
        return true;
        }
    
    /**
     * Connect to all service members. This is a non-blocking call on the
    * Service thread after the ClusterService notifies all other nodes about
    * the presence of the new Service, but before the service sends any message
    * to other nodes.
     */
    public void connectAll()
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet as com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import com.oracle.coherence.common.net.exabus.EndPoint;
        // import java.util.Iterator;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid   service    = getService();
        com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet setMembers = service.getServiceMemberSet();
        Member    memberThis = service.getThisMember();
        
        for (Iterator iter = setMembers.iterator(); iter.hasNext();)
            {
            Member member = (Member) iter.next();
        
            if (member != memberThis)
                {
                int      nMember = member.getId();
                EndPoint peer    = setMembers.getServiceEndPoint(nMember);
                if (peer == null)
                    {
                    peer = service.resolveEndPoint(setMembers.getServiceEndPointName(nMember), member,
                                                   setMembers.getMemberConfigMap(nMember));
        
                    if (peer != null && connect(member, peer))
                        {
                        setMembers.setServiceEndPoint(nMember, peer);
                        }
                    }
        
                // record the name by which we've connected to the peer
                setMembers.setServiceEndPointName(nMember, peer == null ? null : peer.getCanonicalName());
                }
            }
        }
    
    /**
     * Create a ReadBuffer based on the specified BufferSequence.
     */
    public com.tangosol.io.ReadBuffer createReadBuffer(com.oracle.coherence.common.io.BufferSequence bufseq)
        {
        // import com.tangosol.io.MultiBufferReadBuffer;
        // import com.tangosol.io.ReadBuffer;
        // import com.tangosol.io.nio.ByteBufferReadBuffer;
        
        int cBuffers = bufseq.getBufferCount();
        if (cBuffers == 1)
            {
            return new ByteBufferReadBuffer(bufseq.getBuffer(0));
            }
        
        ReadBuffer[] abuf = new ReadBuffer[cBuffers];
        for (int i = 0; i < cBuffers; ++i)
            {
            abuf[i] = new ByteBufferReadBuffer(bufseq.getBuffer(i));
            }
        return new MultiBufferReadBuffer(abuf);
        }
    
    // From interface: com.tangosol.internal.util.MessagePublisher
    public long drainOverflow(java.util.Set setDest, long cMillisTimeout)
            throws java.lang.InterruptedException
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import com.oracle.coherence.common.net.exabus.EndPoint;
        // import com.tangosol.util.Base;
        
        MemberSet setMember = (MemberSet) setDest;
        if (setMember.size() == 1 && !isGlobalBacklog() && !isLocalBacklog())
            {
            ServiceMemberSet setMemberService = getService().getServiceMemberSet();
            int              nMemberTo        = setMember.getFirstId();
            EndPoint         peer             = setMemberService.getServiceEndPoint(nMemberTo);
            if (peer != null && !setMemberService.isServiceBacklogged(nMemberTo))
                {
                return cMillisTimeout; // common case
                }
            }
        
        long ldtStart  = Base.getSafeTimeMillis();
        
        cMillisTimeout = drainOverflowComplex(setMember, cMillisTimeout);
        
        long ldtDelta  = Base.getSafeTimeMillis() - ldtStart;
        
        if (ldtDelta > 0)
            {
            getStatsDrainOverflowDuration().add(ldtDelta);
            }
        
        return cMillisTimeout;
        }
    
    /**
     * Wait for any backlog condition to clear.
    * 
    * @param setMember  the members of interest
    * @param cMillisTimeout  the maximum wait time, or zero for infinite
    * 
    * @return the remaining time
    * 
    * @throws RequestTimeoutException on timeout
     */
    protected long drainOverflowComplex(MemberSet setMember, long cMillisTimeout)
            throws java.lang.InterruptedException
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import com.oracle.coherence.common.base.Blocking;
        // import com.oracle.coherence.common.net.exabus.EndPoint;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.Base;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid          service           = getService();
        ServiceMemberSet setMemberService  = service.getServiceMemberSet();
        long             ldtTimeout        = cMillisTimeout == 0L ? Long.MAX_VALUE : Base.getSafeTimeMillis() + cMillisTimeout;
        
        if (isGlobalBacklog())
            {
            Object oMonitor = getGlobalBacklogMonitor();
            synchronized (oMonitor)
                {
                while (cMillisTimeout >= 0L && isGlobalBacklog())
                    {
                    Blocking.wait(oMonitor, cMillisTimeout);
                    cMillisTimeout = Base.computeSafeWaitTime(ldtTimeout);
                    }
                }
            }
        
        if (isLocalBacklog())
            {
            EndPoint pointLocal = getMessageBus().getLocalEndPoint();
            synchronized (pointLocal)
                {
                while (cMillisTimeout >= 0L && isLocalBacklog())
                    {
                    Blocking.wait(pointLocal, cMillisTimeout);
                    cMillisTimeout = Base.computeSafeWaitTime(ldtTimeout);
                    }
                }
            }
        
        boolean fParent = false;
        switch (setMember.size())
            {
            case 0: // no one to wait on
                break;
        
            case 1: // common case
                {
                int      nMemberTo = setMember.getFirstId();
                EndPoint peer      = setMemberService.getServiceEndPoint(nMemberTo);
                if (peer == null)
                    {
                    fParent = true;
                    }
                else if (nMemberTo != 0 && setMemberService.isServiceBacklogged(nMemberTo))
                    {
                    synchronized (peer)
                        {
                        while (cMillisTimeout >= 0L && setMemberService.isServiceBacklogged(nMemberTo))
                            {
                            Blocking.wait(peer, cMillisTimeout);
                            cMillisTimeout = Base.computeSafeWaitTime(ldtTimeout);
                            }
                        }
                    }
                break;
                }
        
            default:
                {
                int[] aId = setMember.toIdArray();
                for (int i = 0, c = aId.length; i < c; ++i)
                    {
                    int      nMemberTo = aId[i];
                    EndPoint peer      = setMemberService.getServiceEndPoint(nMemberTo);
                    if (peer == null)
                        {
                        fParent = true;
                        }                
                    else if (nMemberTo != 0 && setMemberService.isServiceBacklogged(nMemberTo))
                        {
                        synchronized (peer)
                            {
                            while (cMillisTimeout >= 0L && setMemberService.isServiceBacklogged(nMemberTo))
                                {
                                Blocking.wait(peer, cMillisTimeout);
                                cMillisTimeout = Base.computeSafeWaitTime(ldtTimeout);
                                }
                            }
                        }            
                    }
                break;
                }
            }
        
        if (cMillisTimeout < 0L)
            {
            throw new RequestTimeoutException("Request timed out");
            }
        
        return fParent
            ? getParentMessagePublisher().drainOverflow(setMember, cMillisTimeout)           
            : cMillisTimeout;
        }
    
    // From interface: com.tangosol.internal.util.MessagePublisher
    /**
     * Flush the outgoing transport queues.
     */
    public void flush()
        {
        // import java.util.concurrent.atomic.AtomicBoolean;
        
        try
            {
            getMessageBus().flush();
            }
        catch (IllegalStateException e)
            {
            if (!isClosing())
                {
                throw e;
                }
            // else; some thread was using service while it was in shutdown, this is allowable
            }
        
        AtomicBoolean atomicFlush = getPendingParentFlush();
        if (atomicFlush.get() && atomicFlush.compareAndSet(true, false))
            {
            getParentMessagePublisher().flush();
            }
        // else; common case which is why we try to avoid the failed CAS which is noticibly
        // more expensive then a volatile read
        }
    
    // Accessor for the property "BufferManager"
    /**
     * Getter for property BufferManager.<p>
    * The BufferManager to use for message serialization.
     */
    public com.oracle.coherence.common.io.BufferManager getBufferManager()
        {
        return __m_BufferManager;
        }
    
    // Accessor for the property "ConnectionMap"
    /**
     * Getter for property ConnectionMap.<p>
    * Map<EndPoint, $Connection>
     */
    public java.util.Map getConnectionMap()
        {
        return __m_ConnectionMap;
        }
    
    // Accessor for the property "DeliveryTimeoutMillis"
    /**
     * Getter for property DeliveryTimeoutMillis.<p>
    * The message delivery timeout.
     */
    public long getDeliveryTimeoutMillis()
        {
        return __m_DeliveryTimeoutMillis;
        }
    
    // Accessor for the property "DisconnectCounter"
    /**
     * Getter for property DisconnectCounter.<p>
    * Represents the number of connections for which a DISCONNECT event has
    * been received, but for which a RELEASE event is still pending.  This
    * provides a cheap means to determine if a RECEIPT indicates successful
    * delivery, i.e. if the count is 0 then it must be a success, where as a
    * non-zero count means it is in doubt.  This approach is used as opposed to
    * tracking state on the Connection because that would require Connection
    * lookup, which is a more expensive operation, and most of the time the
    * counter will be zero.
     */
    public java.util.concurrent.atomic.AtomicLong getDisconnectCounter()
        {
        return __m_DisconnectCounter;
        }
    
    // Accessor for the property "EventCollector"
    /**
     * Getter for property EventCollector.<p>
    * The Collector used to receive events from the MessageBus.
     */
    public com.oracle.coherence.common.base.Collector getEventCollector()
        {
        return __m_EventCollector;
        }
    
    // Accessor for the property "GlobalBacklogMonitor"
    /**
     * Getter for property GlobalBacklogMonitor.<p>
    * Monitor to wait on in case of a global backlog
     */
    public Object getGlobalBacklogMonitor()
        {
        return __m_GlobalBacklogMonitor;
        }
    
    // Accessor for the property "HungConnectionIPs"
    /**
     * Getter for property HungConnectionIPs.<p>
    * The set Map<InetAddr, null> of IPs for hung connections.  
    * Hung is identified as Connections which are in a DISCONNECTED state and
    * awaiting the corresponding MemberLeft notification.
    * 
    * Note, we only loosly track the IPs, specifically once one member from the
    * IP unregisters we remove the IP from the map without accounting for any
    * other members on that IP which may still be hung.  The overal intent is
    * to ensure that if we lose communication with many members we will self
    * terminate, and this is still preserved.
     */
    public com.tangosol.util.SafeHashMap getHungConnectionIPs()
        {
        return __m_HungConnectionIPs;
        }
    
    // Accessor for the property "IncomingQueue"
    /**
     * Getter for property IncomingQueue.<p>
    * The queue for messages being sent TO the service.
     */
    public com.tangosol.coherence.component.util.Queue getIncomingQueue()
        {
        return __m_IncomingQueue;
        }
    
    // Accessor for the property "MessageBus"
    /**
     * Getter for property MessageBus.<p>
    * The MessageBus this MessageHandler is associated with.
     */
    public com.oracle.coherence.common.net.exabus.MessageBus getMessageBus()
        {
        return __m_MessageBus;
        }
    
    // Accessor for the property "ParentMessagePublisher"
    /**
     * Getter for property ParentMessagePublisher.<p>
    * The MessagePublisher to hand off messages to in the case that a
    * connection does not exist in this handler.
     */
    public com.tangosol.internal.util.MessagePublisher getParentMessagePublisher()
        {
        return __m_ParentMessagePublisher;
        }
    
    // Accessor for the property "PendingParentFlush"
    /**
     * Getter for property PendingParentFlush.<p>
    * True iff there is a pending flush to the ParentMessagePublisher.
     */
    protected java.util.concurrent.atomic.AtomicBoolean getPendingParentFlush()
        {
        return __m_PendingParentFlush;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * The Service this MessageHandler is associated with.
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid getService()
        {
        return __m_Service;
        }
    
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid getServiceById(int nSvcId)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
        
        _assert(service.getServiceId() == nSvcId);
        
        return service;
        }
    
    // Accessor for the property "StatsBacklogDirect"
    /**
     * Getter for property StatsBacklogDirect.<p>
    * The number of times the direct connection backlog was announced since the
    * last time the statistics were reset.
     */
    public long getStatsBacklogDirect()
        {
        return __m_StatsBacklogDirect;
        }
    
    // Accessor for the property "StatsBacklogGlobal"
    /**
     * Getter for property StatsBacklogGlobal.<p>
    * The number of times the global backlog was announced since the last time
    * the statistics were reset.
     */
    public long getStatsBacklogGlobal()
        {
        return __m_StatsBacklogGlobal;
        }
    
    // Accessor for the property "StatsBacklogLocal"
    /**
     * Getter for property StatsBacklogLocal.<p>
    * The number of times the local backlog was announced since the last time
    * the statistics were reset.
     */
    public long getStatsBacklogLocal()
        {
        return __m_StatsBacklogLocal;
        }
    
    // Accessor for the property "StatsBusBytesIn"
    /**
     * Getter for property StatsBusBytesIn.<p>
    * The number of bytes received on the bus since the last time the
    * statistics were reset.
     */
    public long getStatsBusBytesIn()
        {
        return __m_StatsBusBytesIn;
        }
    
    // Accessor for the property "StatsBusBytesOut"
    /**
     * Getter for property StatsBusBytesOut.<p>
    * The number of bytes sent out by the bus since the last time the
    * statistics were reset.
     */
    public long getStatsBusBytesOut()
        {
        return __m_StatsBusBytesOut;
        }
    
    // Accessor for the property "StatsBusBytesOutBuffered"
    /**
     * Getter for property StatsBusBytesOutBuffered.<p>
    * The number of bytes "buffered" by the bus for outgoing sends.  This is
    * the number of bytes for which we are waiting on receipts before releasing.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsBusBytesOutBuffered()
        {
        return __m_StatsBusBytesOutBuffered;
        }
    
    // Accessor for the property "StatsBusReceives"
    /**
     * Getter for property StatsBusReceives.<p>
    * The number of messages received via the bus since the last time the
    * statistics were reset.
     */
    public long getStatsBusReceives()
        {
        return __m_StatsBusReceives;
        }
    
    // Accessor for the property "StatsBusSends"
    /**
     * Getter for property StatsBusSends.<p>
    * The number of messages sent via the bus since the last time the
    * statistics were reset.
     */
    public long getStatsBusSends()
        {
        return __m_StatsBusSends;
        }
    
    // Accessor for the property "StatsDrainOverflowDuration"
    /**
     * Getter for property StatsDrainOverflowDuration.<p>
    * Accumulated time spend in drainOverflow.
     */
    public java.util.concurrent.atomic.LongAdder getStatsDrainOverflowDuration()
        {
        return __m_StatsDrainOverflowDuration;
        }
    
    /**
     * Initialize this MessageHandler with the specified MessageBus, that is
    * already bound to the local EndPoint. If a peer node does not support the
    * same protocol as this bus, the specified "datagram" associated Queue
    * should be used. Called on the service thread.
     */
    public void initialize(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service, com.oracle.coherence.common.net.exabus.MessageBus bus, com.tangosol.internal.util.MessagePublisher publisherParent)
        {
        // import com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver$DefaultDependencies as com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.DefaultDependencies;
        // import com.oracle.coherence.common.internal.net.socketbus.AbstractSocketBus;
        // import com.oracle.coherence.common.io.BufferManagers;
        
        _assert(getService() == null, "Already initialized");
        
        service.ensureGuardSupport();
        setService(service);
        setMessageBus(bus);
        setParentMessagePublisher(publisherParent);
        setIncomingQueue(service.getQueue());
        setDeliveryTimeoutMillis(service.getCluster().getDependencies().getPublisherResendTimeoutMillis());
        setBufferManager(bus instanceof AbstractSocketBus
            ? com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver.DefaultDependencies.DEFAULT_BUFFER_MANAGER
            : BufferManagers.getNetworkDirectManager());
        
        // this method is called while no other threads can see this handler,
        // so no synchronization is required yet
        bus.setEventCollector(getEventCollector());
        bus.open();
        }
    
    protected MessageHandler.Connection instantiateConnection(com.oracle.coherence.common.net.exabus.EndPoint peer, Member member)
        {
        MessageHandler.Connection connect = (MessageHandler.Connection) _newChild("Connection");
        connect.setPeer(peer);
        connect.setMember(member);
        return connect;
        }
    
    // Accessor for the property "Closing"
    /**
     * Getter for property Closing.<p>
    * True once the bus has started closing, and true thereafter.
    * 
    * @volatile
     */
    public boolean isClosing()
        {
        return __m_Closing;
        }
    
    // Accessor for the property "EstimateMessageSize"
    /**
     * Getter for property EstimateMessageSize.<p>
    * If true then the MessageHandler will expend extra CPU in an attempt to
    * estimate the serialized size of a message.  Controlled by the
    * tangosol.coherence.estimateBusMessageSize system property.
     */
    public static boolean isEstimateMessageSize()
        {
        return __s_EstimateMessageSize;
        }
    
    // Accessor for the property "GlobalBacklog"
    /**
     * Getter for property GlobalBacklog.<p>
    * Flag indicating if the MessageBus has declared a backlog which is not
    * associated with a specific peer.
     */
    public boolean isGlobalBacklog()
        {
        return __m_GlobalBacklog;
        }
    
    // Accessor for the property "LocalBacklog"
    /**
     * Getter for property LocalBacklog.<p>
    * Flag indicating if the MessageBus has declared a backlog associated with
    * this peer.
     */
    public boolean isLocalBacklog()
        {
        return __m_LocalBacklog;
        }
    
    /**
     * BusEvent handler. Handle a backlog condition on the connection.
    * 
    * @param peer            the EndPoint on which the backlog condition is
    * occuring
    * @param fExcessive  true if the backlog is excessive, false if it is normal
     */
    public void onBacklog(com.oracle.coherence.common.net.exabus.EndPoint peer, boolean fExcessive)
        {
        // import com.oracle.coherence.common.net.exabus.EndPoint;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        
        EndPoint pointLocal = getMessageBus().getLocalEndPoint();
        
        if (peer == null)
            {
            if (fExcessive)
                {
                setGlobalBacklog(true);
                setStatsBacklogGlobal(getStatsBacklogGlobal() + 1L);
                }
            else
                {
                Object oMonitor = getGlobalBacklogMonitor();
                synchronized (oMonitor)
                    {
                    setGlobalBacklog(false);
                    oMonitor.notifyAll();
                    }
                }
            }
        else if (peer.equals(pointLocal))
            {
            if (fExcessive)
                {
                setLocalBacklog(true);
                setStatsBacklogLocal(getStatsBacklogLocal() + 1L);
                }
            else
                {
                synchronized (pointLocal)
                    {
                    setLocalBacklog(false);
                    pointLocal.notifyAll();
                    }
                }
            }
        else
            {
            MessageHandler.Connection connect = (MessageHandler.Connection) getConnectionMap().get(peer);
        
            if (connect != null)
                {
                ServiceMemberSet setMember = getService().getServiceMemberSet();
                int              nMember   = connect.getMember().getId();
        
                if (fExcessive)
                    {
                    setMember.setServiceBacklogged(nMember, true);
                    setStatsBacklogDirect(getStatsBacklogDirect() + 1L);
                    }
                else
                    {        
                    peer = connect.getPeer(); // use the same reference as in the ServiceMemberSet
                    synchronized (peer)
                        {
                        setMember.setServiceBacklogged(nMember, false);
                        peer.notifyAll();
                        }
                    }
                }
            // else; ignore flow control event
            }
        }
    
    /**
     * Handler for Bus events offloaded onto the service thread. Called on the
    * Service thread only.
     */
    public void onBusEvent(com.oracle.coherence.common.net.exabus.Event event)
        {
        // import com.oracle.coherence.common.io.BufferSequence;
        // import com.tangosol.util.Base;
        // import java.io.IOException;
        
        try
            {
            // pseudo enum support
            switch (event.getType().ordinal())
                {
                case MessageHandler.EventCollector.ET_OPEN:
                    onOpen();
                    break;
        
                case MessageHandler.EventCollector.ET_CLOSE:
                    onClose();
                    break;
        
                case MessageHandler.EventCollector.ET_CONNECT:
                    onConnect(event.getEndPoint());
                    break;
        
                case MessageHandler.EventCollector.ET_MESSAGE:
                    onMessage(event.getEndPoint(), (BufferSequence) event.getContent());
                    break;
                    
                case MessageHandler.EventCollector.ET_DISCONNECT:
                    onDisconnect(event.getEndPoint(), (Throwable) event.getContent());
                    break;
        
                case MessageHandler.EventCollector.ET_RELEASE:
                    onReleased(event.getEndPoint());
                    break;
        
                case MessageHandler.EventCollector.ET_BACKLOG_EXCESSIVE:
                    onBacklog(event.getEndPoint(), /*fExcessive*/ true);
                    break;
                    
                case MessageHandler.EventCollector.ET_BACKLOG_NORMAL:
                    onBacklog(event.getEndPoint(), /*fExcessive*/ false);
                    break;
        
                default:
                    // ignore for now
                    break;
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        finally
            {
            event.dispose();
            }
        }
    
    /**
     * BusEvent handler.
     */
    protected void onClose()
        {
        // clear any local or global backlogs
        onBacklog(getMessageBus().getLocalEndPoint(), false);
        onBacklog(null, false);
        }
    
    /**
     * BusEvent handler.
     */
    protected void onConnect(com.oracle.coherence.common.net.exabus.EndPoint peer)
        {
        MessageHandler.Connection conn = (MessageHandler.Connection) getConnectionMap().get(peer);
        
        if (conn == null)
            {
            // unsolicited CONNECT could happen due to:
            // - a rogue connection (e.g. non-Coherence MessageBus user)
            // - a connection from a member which left the service while we were joining
        
            getMessageBus().release(peer);
            }
        else
            {
            switch (conn.getState())
                {
                case MessageHandler.Connection.STATE_CONNECTING:
                    conn.setState(MessageHandler.Connection.STATE_CONNECTED);
                    break;
        
                case MessageHandler.Connection.STATE_DISCONNECTING:
                    // the release() was called before the CONNECT event was processed
                    break;
        
                default:
                    throw new IllegalStateException("Unexpected Connect event: " + conn);
                }
            }
        }
    
    /**
     * BusEvent handler.
     */
    protected void onDisconnect(com.oracle.coherence.common.net.exabus.EndPoint peer, Throwable tReason)
        {
        MessageHandler.Connection conn = (MessageHandler.Connection) getConnectionMap().get(peer);
        if (conn != null)
            {
            conn.onDisconnect(tReason);
            }
        }
    
    /**
     * Event handler. Could be called on the Bus thread as well as the Service
    * thread.
     */
    public void onException(Throwable e)
        {
        getService().onException(e);
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification called out of
    * setConstructed() for the topmost component and that in turn notifies all
    * the children.
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as) the control returns back to the
    * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        super.onInit();
        
        setEventCollector((MessageHandler.EventCollector) _findChild("EventCollector"));
        }
    
    public void onInterval()
        {
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        
        try
            {
            for (Iterator iter = getConnectionMap().values().iterator(); iter.hasNext(); )
                {
                ((MessageHandler.Connection) iter.next()).onInterval();
                }
            }
        catch (ConcurrentModificationException e)
            {
            // ignore; we'll just retry on next interval
            }
        }
    
    /**
     * BusEvent handler for MESSAGE events.  Normally MESSAGE events are
    * processed on the collector thread and turned into Coherence Messages and
    * queue'd to the service.  onMessage is only used for processing MESSAGE
    * events for EndPoints which are not assigned a MessageHandler$Connection
    * and associated with a Member.  This includes MESSAGE events which are
    * received before a member association is made, and after the member
    * association is torn down.
     */
    protected void onMessage(com.oracle.coherence.common.net.exabus.EndPoint peer, com.oracle.coherence.common.io.BufferSequence bufseq)
            throws java.io.IOException
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        MessageHandler.Connection connect = (MessageHandler.Connection) getConnectionMap().get(peer);
        if (connect == null)
            {
            _trace("Discarding a message from disconnected or unknown peer: " + peer, 2);
            }
        else
            {
            if (connect.isEstablished() || connect.establish())
                {
                Message msg = connect.prepareMessage(bufseq);
                if (msg != null)
                    {
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = msg.getService();
                    if (service == getService())
                        {
                        if (!msg.isDeserializationRequired() || service.deserializeMessage(msg))
                            {
                            service.onMessage(msg);
                            }
                        }
                    else
                        {
                        // not even when using the TransportService is this possible as the
                        // msg.getService() on both sides had to have joined before they could
                        // have started their dependent services
                        throw new IllegalStateException();
                        }
                    }
                // else; msg == null logged in prepareMessage
                }
            else
                {
                // an accepted connection that was never "established" and is now disconnecting
                _trace("Ignoring delayed message from departing " + connect, 6);
                }
            }
        }
    
    /**
     * BusEvent handler.
     */
    protected void onOpen()
        {
        }
    
    /**
     * BusEvent handler.
     */
    protected void onReleased(com.oracle.coherence.common.net.exabus.EndPoint peer)
        {
        onBacklog(peer, /*fExcessive*/ false);
        
        MessageHandler.Connection conn = (MessageHandler.Connection) getConnectionMap().remove(peer);
        if (conn != null)
            {
            conn.onReleased();
        
            _trace("Unregistered " + conn, 6);
            }
        }
    
    // From interface: com.tangosol.internal.util.MessagePublisher
    /**
     * Send the specified message.
    * 
    * @return true if the message has been serialized and sent on the network,
    * false if the message was not serialized (all addressees are gone)
     */
    public boolean post(Object oMsg)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import com.oracle.coherence.common.io.BufferSequence;
        // import com.oracle.coherence.common.net.exabus.EndPoint;
        
        if (isClosing())
            {
            return false;
            }
        
        Message          msg         = (Message) oMsg;
        ServiceMemberSet setEndPoint = getService().getServiceMemberSet();
        MemberSet        setMemberTo = msg.getToMemberSet();
        
        _assert(setMemberTo != null, "MessageBus cannot be used to broadcast");
        
        switch (setMemberTo.size())
            {
            case 0: // no one to send to
                return false;
        
            case 1: // common case
                {
                int      nMemberTo = setMemberTo.getFirstId();
                EndPoint peer      = setEndPoint.getServiceEndPoint(nMemberTo);
                if (peer == null)
                    {
                    if (getParentMessagePublisher().post(msg))
                        {
                        getPendingParentFlush().set(true);
                        return true;
                        }
                    return false;
                    }
        
                BufferSequence bufseq = serializeMessage(msg);
                long           cbMsg  = bufseq.getLength();
        
                getStatsBusBytesOutBuffered().addAndGet(cbMsg);
        
                // single-point message
                try
                    {
                    MessageHandler.Connection conn = ((MessageHandler.Connection) getConnectionMap().get(peer));
                    if (conn == null)
                        {
                        // Note: we can "legitimately" get here during concurrent release, but
                        // we'll check fo rhtat in the catch
                        throw new IllegalArgumentException("unknown peer " + peer);
                        }
        
                    getMessageBus().send(peer, bufseq, msg, /*fSocketWrite*/false);
        
                    // TODO: should this and ReceivedMessageCount be tracked on ServiceMemberSet for efficiency?
                    conn.getSentMessageCount().incrementAndGet();
        
                    // update stats
                    setStatsBusSends(getStatsBusSends() + 1L);
                    setStatsBusBytesOut(getStatsBusBytesOut() + cbMsg);
        
                    return true;
                    }
                catch (RuntimeException e)
                    {
                    msg.releaseOutgoing(/*fSuspect*/ true);
        
                    if (isClosing() || checkReleased(peer))
                        {
                        // the connection was concurrently released
                        return false;
                        }
        
                    throw e;
                    }
                }
        
            default:
                return postMulti(msg);
            }
        }
    
    /**
     * Create and post a BusEventMessage to the Service thread. Called on a Bus
     * thread.
     *
     * @param event  the Bus event
     */
    public void postEventMessage(com.oracle.coherence.common.net.exabus.Event event)
        {
        // import Component.Net.Message.BusEventMessage;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid         service  = getService();
        BusEventMessage msgEvent = (BusEventMessage) service.instantiateMessage("BusEventMessage");
        
        msgEvent.setEvent(event);
        msgEvent.setMessageHandler(this);
        msgEvent.addToMember(service.getThisMember());
        
        service.post(msgEvent);
        }
    
    /**
     * Send the specified message to multiple peers
    * 
    * @return true if the message has been serialized and sent on the network,
    * false if the message was not serialized (addressed to no external members)
     */
    protected boolean postMulti(Message msg)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import com.oracle.coherence.common.net.exabus.EndPoint;
        // import com.oracle.coherence.common.net.exabus.MessageBus;
        // import com.oracle.coherence.common.io.BufferSequence;
        
        ServiceMemberSet setEndPoint = getService().getServiceMemberSet();
        MemberSet        setMemberTo = msg.getToMemberSet();
        MessageBus       bus         = getMessageBus();
        BufferSequence   bufseq      = null;
        boolean          fSent       = false;
        long             cbSent      = 0L;
        long             cbMsg       = 0L;
        
        int[] anId = setMemberTo.toIdArray();
        for (int i = 0, c = anId.length; i < c; ++i)
            {
            int nId = anId[i];    
        
            _assert(nId != 0);
        
            EndPoint peer = setEndPoint.getServiceEndPoint(nId);
            if (peer != null)
                {
                if (bufseq == null)
                    {
                    bufseq = serializeMessage(msg);
                    cbMsg  = bufseq.getLength();
                    }
        
                setMemberTo.remove(nId); // we've taken ownership of this member
                try
                    {
                    MessageHandler.Connection conn = ((MessageHandler.Connection) getConnectionMap().get(peer));
                    if (conn == null)
                        {
                        // Note: we can "legitimately" get here during concurrent release, but
                        // we'll check fo rhtat in the catch
                        throw new IllegalArgumentException("unknown peer " + peer);
                        }
        
                    bus.send(peer, bufseq, msg);
        
                    conn.getSentMessageCount().incrementAndGet();
                    
                    cbSent += cbMsg;
                    fSent   = true;
                    }
                catch (RuntimeException e)
                    {
                    msg.releaseOutgoing(/*fSuspect*/ true);
        
                    if (!(isClosing() || checkReleased(peer)))
                        {
                        throw e;
                        }
                    // else the connection was concurrently released; continue with other members
                    }
                }
            }
        
        if (cbSent != 0L)
            {
            // update stats
            setStatsBusSends(getStatsBusSends() + 1L);
            setStatsBusBytesOut(getStatsBusBytesOut() + cbSent);
            getStatsBusBytesOutBuffered().addAndGet(cbSent);
            }
        
        if (!setMemberTo.isEmpty())
            {
            if (getParentMessagePublisher().post(msg))
                {
                fSent = true;
                getPendingParentFlush().set(true);
                }
            }
        
        return fSent;
        }
    
    /**
     * Process the Bus message. Called on a Bus thread.
    * 
    * @return true if the event has been handled and is ready to be disposed;
    * false if further processing is required
     */
    public void processMessage(com.oracle.coherence.common.net.exabus.Event event)
            throws java.io.IOException
        {
        // import com.oracle.coherence.common.io.BufferSequence;
        // import com.oracle.coherence.common.net.exabus.EndPoint;
        
        EndPoint       peer    = event.getEndPoint();
        BufferSequence bufseq  = (BufferSequence) event.getContent();
        MessageHandler.Connection    connect = (MessageHandler.Connection) getConnectionMap().get(peer);
        
        // track stats
        setStatsBusReceives(getStatsBusReceives() + 1L);
        setStatsBusBytesIn(getStatsBusBytesIn() + bufseq.getLength());
        
        try
            {
            if (connect == null)
                {
                // a not yet accepted connection, the service has yet to call MessageHandler.connect for this EndPoint
                // queue the event to the service for deferred processing
                postEventMessage(event);
                event = null;
                }
            else
                {
                if (connect.isEstablished() || connect.establish())
                    {
                    // an accepted connection; here we perform deserialization and execute request on a
                    // bus collector thread if the service is configured with a negative worker thread count,
                    // or only deserialize request if the worker thread count is zero and daemon pool disabled;
                    // Otherwise, we add minimally deserialized message to the service queue for processing.
                    // Note: we don't call service.post, but rather add the message directly to the service
                    // queue.  This is done to avoid making this message appear to have originated locally.
                    Message msg = connect.prepareMessage(bufseq);
                    if (msg != null)
                        {
                        if (msg.isDeserializationRequired())
                            {
                            // couple eventual deserialization with event disposal
                            msg.setBufferController(event);
                            event = null;
                            }

                        Grid service  = msg.getService();
                        int  cWorkers = service.getDependencies().getWorkerThreadCount();

                        if (cWorkers <= 0 && (service.isAcceptingOthers() || service == getService()))
                            {
                            // we deserialize on a transport thread if the service doesn't have a daemon pool,
                            // to avoid deserialization on the service thread
                            if (service.deserializeMessage(msg))
                                {
                                // COH-28112: execute request on a transport thread
                                if (cWorkers < 0)
                                    {
                                    service.onMessage(msg);
                                    return;
                                    }
                                }
                            else
                                {
                                msg = null;
                                }
                            }

                        if (msg != null)
                            {
                            msg.getService().getQueue().add(msg);
                            }
                        }
                    // else; msg == null logged in prepareMessage
                    }
                else
                    {
                    // an accepted connection that was never "established" and is now disconnecting
                    _trace("Ignoring delayed message from departing " + connect, 6);
                    }
                }
            }
        finally
            {
            if (event != null)
                {
                event.dispose();
                }
            }
        }
    
    /**
     * Process the Bus receipt. Called on a Bus thread.
     */
    public void processReceipt(com.oracle.coherence.common.net.exabus.EndPoint peer, Message msg, boolean fSuspect)
        {
        // import com.oracle.coherence.common.io.BufferSequence;
        
        if (msg == null) // COH-13763 diagnostics
            {
            _trace("received a delivery receipt for a null message", 2);
            _trace(new Throwable());
            return;
            }
        
        BufferSequence bufseq = (BufferSequence) msg.getBufferController();
        if (bufseq == null) // COH-13763 diagnostics
            {
            _trace("received a delivery receipt for a disposed message: " + msg, 2);
            _trace(new Throwable());
            return;
            }
        
        getStatsBusBytesOutBuffered().addAndGet(-bufseq.getLength());
        
        MessageHandler.Connection connection = (MessageHandler.Connection) getConnectionMap().get(peer);
        
        if (connection != null)
            {
            connection.setReceivedReceiptCount(connection.getReceivedReceiptCount() + 1L);
            }
        
        msg.releaseOutgoing(fSuspect);
        }
    
    /**
     * Release the connection with the specified member/peer. Called on the
    * Service thread when a departure of the specified service Member is
    * announced.
    * 
    * Note: it's possible that there is yet no connection associated with the
    * specified peer, which can happen if that member departed while the local
    * member was joining the service.
    * 
    * @param peer  the EndPoint for the connection to be released
    * @param continuation  the continuation to call after the connection is
    * released
     */
    public void release(com.oracle.coherence.common.net.exabus.EndPoint peer, com.oracle.coherence.common.base.Continuation continuation)
        {
        MessageHandler.Connection conn = (MessageHandler.Connection) getConnectionMap().get(peer);
        if (conn == null)
            {
            throw new IllegalArgumentException("No connection to: " + peer);
            }
        
        conn.release(continuation);
        }
    
    /**
     * Reset the handler statistics.
     */
    public void resetStats()
        {
        setStatsBusSends(0L);
        setStatsBusReceives(0L);
        setStatsBusBytesOut(0L);
        setStatsBusBytesIn(0L);
        setStatsBacklogGlobal(0L);
        setStatsBacklogLocal(0L);
        setStatsBacklogDirect(0L);
        
        // getStatsBusBytesOutBuffered().getAndSet(0L); // Note: Not reset as the stat is active and would go negative
        }
    
    /**
     * Serialize a message by passing it through the configured network filters
    * and then splitting it into a BufferSequence.
    * 
    * This method is executed on the client thread is analogous to
    * PacketPublisher.serializeMessage().
     */
    protected com.oracle.coherence.common.io.BufferSequence serializeMessage(Message msg)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid;
        // import com.tangosol.internal.io.BufferSequenceWriteBufferPool;
        // import com.tangosol.io.MultiBufferWriteBuffer;
        // import com.tangosol.util.Base;
        // import com.oracle.coherence.common.io.BufferSequence;
        
        BufferSequence bufseq = (BufferSequence) msg.getBufferController();
        
        if (bufseq == null)
            {
            BufferSequenceWriteBufferPool adapter = new BufferSequenceWriteBufferPool(getBufferManager());
        
            try
                {
                Grid service = msg.getService();
        
                // create a ByteBuffers backed WriteBuffer
                service.serializeMessage(msg, new MultiBufferWriteBuffer(adapter, 0).getBufferOutput());
                }
            catch (Throwable e)
                {
                adapter.toBufferSequence().dispose();
                throw Base.ensureRuntimeException(e);
                }
        
            // extract the resulting buffer sequence, which for the outgoing messages
            // also serves as the buffer controller
            bufseq = adapter.toBufferSequence();
            msg.setBufferController(bufseq, msg.getToMemberSet().size());
            }
        // else; layered MessageHandlers, already serialized    
        
        return bufseq;
        }
    
    // Accessor for the property "BufferManager"
    /**
     * Setter for property BufferManager.<p>
    * The BufferManager to use for message serialization.
     */
    public void setBufferManager(com.oracle.coherence.common.io.BufferManager managerBuffer)
        {
        __m_BufferManager = (managerBuffer);
        
        _trace("Initialized BufferManager to: " + managerBuffer, 4);
        }
    
    // Accessor for the property "Closing"
    /**
     * Setter for property Closing.<p>
    * True once the bus has started closing, and true thereafter.
    * 
    * @volatile
     */
    protected void setClosing(boolean fClosing)
        {
        __m_Closing = fClosing;
        }
    
    // Accessor for the property "ConnectionMap"
    /**
     * Setter for property ConnectionMap.<p>
    * Map<EndPoint, $Connection>
     */
    protected void setConnectionMap(java.util.Map map)
        {
        __m_ConnectionMap = map;
        }
    
    // Accessor for the property "DeliveryTimeoutMillis"
    /**
     * Setter for property DeliveryTimeoutMillis.<p>
    * The message delivery timeout.
     */
    public void setDeliveryTimeoutMillis(long lMillis)
        {
        __m_DeliveryTimeoutMillis = lMillis;
        }
    
    // Accessor for the property "DisconnectCounter"
    /**
     * Setter for property DisconnectCounter.<p>
    * Represents the number of connections for which a DISCONNECT event has
    * been received, but for which a RELEASE event is still pending.  This
    * provides a cheap means to determine if a RECEIPT indicates successful
    * delivery, i.e. if the count is 0 then it must be a success, where as a
    * non-zero count means it is in doubt.  This approach is used as opposed to
    * tracking state on the Connection because that would require Connection
    * lookup, which is a more expensive operation, and most of the time the
    * counter will be zero.
     */
    private void setDisconnectCounter(java.util.concurrent.atomic.AtomicLong longCounter)
        {
        __m_DisconnectCounter = longCounter;
        }
    
    // Accessor for the property "EstimateMessageSize"
    /**
     * Setter for property EstimateMessageSize.<p>
    * If true then the MessageHandler will expend extra CPU in an attempt to
    * estimate the serialized size of a message.  Controlled by the
    * tangosol.coherence.estimateBusMessageSize system property.
     */
    public static void setEstimateMessageSize(boolean fSize)
        {
        __s_EstimateMessageSize = fSize;
        }
    
    // Accessor for the property "EventCollector"
    /**
     * Setter for property EventCollector.<p>
    * The Collector used to receive events from the MessageBus.
     */
    protected void setEventCollector(com.oracle.coherence.common.base.Collector collector)
        {
        __m_EventCollector = collector;
        }
    
    // Accessor for the property "GlobalBacklog"
    /**
     * Setter for property GlobalBacklog.<p>
    * Flag indicating if the MessageBus has declared a backlog which is not
    * associated with a specific peer.
     */
    public void setGlobalBacklog(boolean pGlobalBacklog)
        {
        __m_GlobalBacklog = pGlobalBacklog;
        }
    
    // Accessor for the property "GlobalBacklogMonitor"
    /**
     * Setter for property GlobalBacklogMonitor.<p>
    * Monitor to wait on in case of a global backlog
     */
    protected void setGlobalBacklogMonitor(Object oMonitor)
        {
        __m_GlobalBacklogMonitor = oMonitor;
        }
    
    // Accessor for the property "HungConnectionIPs"
    /**
     * Setter for property HungConnectionIPs.<p>
    * The set Map<InetAddr, null> of IPs for hung connections.  
    * Hung is identified as Connections which are in a DISCONNECTED state and
    * awaiting the corresponding MemberLeft notification.
    * 
    * Note, we only loosly track the IPs, specifically once one member from the
    * IP unregisters we remove the IP from the map without accounting for any
    * other members on that IP which may still be hung.  The overal intent is
    * to ensure that if we lose communication with many members we will self
    * terminate, and this is still preserved.
     */
    private void setHungConnectionIPs(com.tangosol.util.SafeHashMap integerCount)
        {
        __m_HungConnectionIPs = integerCount;
        }
    
    // Accessor for the property "IncomingQueue"
    /**
     * Setter for property IncomingQueue.<p>
    * The queue for messages being sent TO the service.
     */
    protected void setIncomingQueue(com.tangosol.coherence.component.util.Queue queue)
        {
        __m_IncomingQueue = queue;
        }
    
    // Accessor for the property "LocalBacklog"
    /**
     * Setter for property LocalBacklog.<p>
    * Flag indicating if the MessageBus has declared a backlog associated with
    * this peer.
     */
    public void setLocalBacklog(boolean pGlobalBacklog)
        {
        __m_LocalBacklog = pGlobalBacklog;
        }
    
    // Accessor for the property "MessageBus"
    /**
     * Setter for property MessageBus.<p>
    * The MessageBus this MessageHandler is associated with.
     */
    protected void setMessageBus(com.oracle.coherence.common.net.exabus.MessageBus bus)
        {
        __m_MessageBus = bus;
        }
    
    // Accessor for the property "ParentMessagePublisher"
    /**
     * Setter for property ParentMessagePublisher.<p>
    * The MessagePublisher to hand off messages to in the case that a
    * connection does not exist in this handler.
     */
    protected void setParentMessagePublisher(com.tangosol.internal.util.MessagePublisher publisher)
        {
        __m_ParentMessagePublisher = publisher;
        }
    
    // Accessor for the property "PendingParentFlush"
    /**
     * Setter for property PendingParentFlush.<p>
    * True iff there is a pending flush to the ParentMessagePublisher.
     */
    private void setPendingParentFlush(java.util.concurrent.atomic.AtomicBoolean atomic)
        {
        __m_PendingParentFlush = atomic;
        }
    
    // Accessor for the property "Service"
    /**
     * Setter for property Service.<p>
    * The Service this MessageHandler is associated with.
     */
    protected void setService(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        __m_Service = service;
        }
    
    // Accessor for the property "StatsBacklogDirect"
    /**
     * Setter for property StatsBacklogDirect.<p>
    * The number of times the direct connection backlog was announced since the
    * last time the statistics were reset.
     */
    protected void setStatsBacklogDirect(long c)
        {
        __m_StatsBacklogDirect = c;
        }
    
    // Accessor for the property "StatsBacklogGlobal"
    /**
     * Setter for property StatsBacklogGlobal.<p>
    * The number of times the global backlog was announced since the last time
    * the statistics were reset.
     */
    protected void setStatsBacklogGlobal(long c)
        {
        __m_StatsBacklogGlobal = c;
        }
    
    // Accessor for the property "StatsBacklogLocal"
    /**
     * Setter for property StatsBacklogLocal.<p>
    * The number of times the local backlog was announced since the last time
    * the statistics were reset.
     */
    protected void setStatsBacklogLocal(long c)
        {
        __m_StatsBacklogLocal = c;
        }
    
    // Accessor for the property "StatsBusBytesIn"
    /**
     * Setter for property StatsBusBytesIn.<p>
    * The number of bytes received on the bus since the last time the
    * statistics were reset.
     */
    protected void setStatsBusBytesIn(long lOut)
        {
        __m_StatsBusBytesIn = lOut;
        }
    
    // Accessor for the property "StatsBusBytesOut"
    /**
     * Setter for property StatsBusBytesOut.<p>
    * The number of bytes sent out by the bus since the last time the
    * statistics were reset.
     */
    protected void setStatsBusBytesOut(long lOut)
        {
        __m_StatsBusBytesOut = lOut;
        }
    
    // Accessor for the property "StatsBusBytesOutBuffered"
    /**
     * Setter for property StatsBusBytesOutBuffered.<p>
    * The number of bytes "buffered" by the bus for outgoing sends.  This is
    * the number of bytes for which we are waiting on receipts before releasing.
     */
    protected void setStatsBusBytesOutBuffered(java.util.concurrent.atomic.AtomicLong longBuffered)
        {
        __m_StatsBusBytesOutBuffered = longBuffered;
        }
    
    // Accessor for the property "StatsBusReceives"
    /**
     * Setter for property StatsBusReceives.<p>
    * The number of messages received via the bus since the last time the
    * statistics were reset.
     */
    protected void setStatsBusReceives(long lReceives)
        {
        __m_StatsBusReceives = lReceives;
        }
    
    // Accessor for the property "StatsBusSends"
    /**
     * Setter for property StatsBusSends.<p>
    * The number of messages sent via the bus since the last time the
    * statistics were reset.
     */
    protected void setStatsBusSends(long lSends)
        {
        __m_StatsBusSends = lSends;
        }
    
    // Accessor for the property "StatsDrainOverflowDuration"
    /**
     * Setter for property StatsDrainOverflowDuration.<p>
    * Accumulated time spend in drainOverflow.
     */
    public void setStatsDrainOverflowDuration(java.util.concurrent.atomic.LongAdder adderDuration)
        {
        __m_StatsDrainOverflowDuration = adderDuration;
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() +
            "{Service=" + getService().getServiceName() +
            (isClosing() ? ", closing" : "") +
            ", connections=" + getConnectionMap().size() +
            ", disconnectedIPs=" + getHungConnectionIPs().size() +
            ", backlogs=(" + getStatsBacklogGlobal() +
                       "/" + getStatsBacklogLocal() +
                       "/" + getStatsBacklogDirect() +
            "), bus=" + getMessageBus() + "}";
        }

    // ---- class: com.tangosol.coherence.component.net.MessageHandler$Connection
    
    /**
     * Information about a connection to a peer.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Connection
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.net.Guardable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Context
         *
         * The Connection's GuardContext or null if it is not currently guarded.
         */
        private com.tangosol.net.Guardian.GuardContext __m_Context;
        
        /**
         * Property DisconnectCause
         *
         * The reason, if any, that was provided with the DISCONNECT event.
         */
        private Throwable __m_DisconnectCause;
        
        /**
         * Property Established
         *
         * Indicates that this connection is allowed to add messages to the
         * service incoming queue. Once set, it will never be negated. This
         * property is modified only while holding synchronization on the
         * connection.
         */
        private boolean __m_Established;
        
        /**
         * Property LastBadMessage
         *
         * The last bad message received.  This is held only temporarily so
         * that it can be easily found in the HeapDump as part of diagnosing SR
         * 3-15874362211.
         */
        private com.oracle.coherence.common.io.BufferSequence __m_LastBadMessage;
        
        /**
         * Property LastHealthyTimestamp
         *
         * The timestamp at which this connection was last proved to be healthy.
         */
        private long __m_LastHealthyTimestamp;
        
        /**
         * Property LastHeuristicDeathTimestamp
         *
         * The last time the connection was stuck for more then 1/2 of the
         * delivery timeout.
         */
        private long __m_LastHeuristicDeathTimestamp;
        
        /**
         * Property Member
         *
         * The Member this Connection object is for.
         */
        private Member __m_Member;
        
        /**
         * Property Peer
         *
         * The EndPoint this Connection object is for.
         */
        private com.oracle.coherence.common.net.exabus.EndPoint __m_Peer;
        
        /**
         * Property ReceivedReceiptCount
         *
         * The number of acked messages from this peer.
         * 
         * receipts are delivired sequentially by the bus and thus unlike
         * SentMessageCount this propertly does not need to be an atomic.
         * 
         * @volatile - set by bus thread, but read on service thread
         */
        private volatile long __m_ReceivedReceiptCount;
        
        /**
         * Property ReleaseAction
         *
         * The continuation to run when the connection is released.
         */
        private com.oracle.coherence.common.base.Continuation __m_ReleaseAction;
        
        /**
         * Property SentMessageCount
         *
         * The number of ackable messages sent to this peer.
         * 
         * This is an atomic as many threads may concurrently increment it and
         * it is not just some dirty stat.
         */
        private java.util.concurrent.atomic.AtomicLong __m_SentMessageCount;
        
        /**
         * Property State
         *
         * The state of the connection represented by this Connection. Valid
         * values are any of the STATE_* constants.
         */
        private int __m_State;
        
        /**
         * Property STATE_CONNECTED
         *
         * Bus is connected to the corresponding Member.
         */
        public static final int STATE_CONNECTED = 2;
        
        /**
         * Property STATE_CONNECTING
         *
         * Bus is in process of connecting to the corresponding Member.
         */
        public static final int STATE_CONNECTING = 1;
        
        /**
         * Property STATE_DISCONNECTED
         *
         * Bus is disconnected from the corresponding Member.
         */
        public static final int STATE_DISCONNECTED = 4;
        
        /**
         * Property STATE_DISCONNECTING
         *
         * Bus is in process of disconnecting from the corresponding Member.
         */
        public static final int STATE_DISCONNECTING = 3;
        
        /**
         * Property STATE_INITIAL
         *
         * Initial.
         */
        public static final int STATE_INITIAL = 0;
        
        /**
         * Property STATE_RELEASED
         *
         * Bus is released from the corresponding Member.
         */
        public static final int STATE_RELEASED = 5;
        
        /**
         * Property SuspectReceivedReceiptCount
         *
         * The ReceivedReceiptCount at the time this connection became suspect
         */
        private long __m_SuspectReceivedReceiptCount;
        
        /**
         * Property SuspectTimeoutTimestamp
         *
         * The timestamp at which a suspect connection will be considered to
         * have timedout.
         */
        private long __m_SuspectTimeoutTimestamp;
        
        // Default constructor
        public Connection()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Connection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            
            // state initialization: private properties
            try
                {
                __m_SentMessageCount = new java.util.concurrent.atomic.AtomicLong();
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
            return new com.tangosol.coherence.component.net.MessageHandler.Connection();
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
                clz = Class.forName("com.tangosol.coherence/component/net/MessageHandler$Connection".replace('/', '.'));
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
        
        /**
         * Check if the connection is in the "CONNECTED" state
         */
        public boolean establish()
            {
            synchronized (this)
                {
                if (getState() < STATE_DISCONNECTING)
                    {
                    if (!isEstablished())
                        {
                        _trace("Connection established with " + getPeer(), 3);
                        setEstablished(true);
                        }
                    return true;
                    }
                return false;
                }
            }
        
        /**
         * Format the specified connection state as a human-readable string.
         */
        public String formatStateName(int nState)
            {
            switch (nState)
                {
                case  STATE_INITIAL:
                    return "INITIAL";
                case  STATE_CONNECTING:
                    return "CONNECTING";
                case  STATE_CONNECTED:
                    return "CONNECTED";
                case  STATE_DISCONNECTING:
                    return "DISCONNECTING";
                case  STATE_DISCONNECTED:
                    {
                    Throwable t = getDisconnectCause();
                    return "DISCONNECTED"  + (t == null ? "" : "(" + t + ")");
                    }
                case  STATE_RELEASED:
                    return "RELEASED";
                default:
                    return "<unknown> " + nState;
                }
            }
        
        // From interface: com.tangosol.net.Guardable
        // Accessor for the property "Context"
        /**
         * Getter for property Context.<p>
        * The Connection's GuardContext or null if it is not currently guarded.
         */
        public com.tangosol.net.Guardian.GuardContext getContext()
            {
            return __m_Context;
            }
        
        public long getDeliveryTimeoutMillis()
            {
            MessageHandler handler = (MessageHandler) get_Module();
            long    cMillis = handler.getDeliveryTimeoutMillis();
            Member  member  = getMember();        
            
            if (member != null)
                {
                int nImportance = handler.compareImportance(member);
                if (nImportance > 0 ||
                   (nImportance == 0 && handler.getService().getThisMember().getTimestamp() < member.getTimestamp()))
                    {
                    // in the case that both sides of the connection detect a problem we'd prefer that only one commit
                    // suicide.  Weight things in favor of the more important member surviving by delaying how long
                    // it will take to invoke the witness protocol.
                    cMillis += cMillis / 20;
                    }
                }
            
            return cMillis;
            }
        
        // Accessor for the property "DisconnectCause"
        /**
         * Getter for property DisconnectCause.<p>
        * The reason, if any, that was provided with the DISCONNECT event.
         */
        public Throwable getDisconnectCause()
            {
            return __m_DisconnectCause;
            }
        
        // Accessor for the property "LastBadMessage"
        /**
         * Getter for property LastBadMessage.<p>
        * The last bad message received.  This is held only temporarily so that
        * it can be easily found in the HeapDump as part of diagnosing SR
        * 3-15874362211.
         */
        public com.oracle.coherence.common.io.BufferSequence getLastBadMessage()
            {
            return __m_LastBadMessage;
            }
        
        // Accessor for the property "LastHealthyTimestamp"
        /**
         * Getter for property LastHealthyTimestamp.<p>
        * The timestamp at which this connection was last proved to be healthy.
         */
        public long getLastHealthyTimestamp()
            {
            return __m_LastHealthyTimestamp;
            }
        
        // Accessor for the property "LastHeuristicDeathTimestamp"
        /**
         * Getter for property LastHeuristicDeathTimestamp.<p>
        * The last time the connection was stuck for more then 1/2 of the
        * delivery timeout.
         */
        public long getLastHeuristicDeathTimestamp()
            {
            return __m_LastHeuristicDeathTimestamp;
            }
        
        // Accessor for the property "Member"
        /**
         * Getter for property Member.<p>
        * The Member this Connection object is for.
         */
        public Member getMember()
            {
            return __m_Member;
            }
        
        // Accessor for the property "Peer"
        /**
         * Getter for property Peer.<p>
        * The EndPoint this Connection object is for.
         */
        public com.oracle.coherence.common.net.exabus.EndPoint getPeer()
            {
            return __m_Peer;
            }
        
        // Accessor for the property "ReceivedReceiptCount"
        /**
         * Getter for property ReceivedReceiptCount.<p>
        * The number of acked messages from this peer.
        * 
        * receipts are delivired sequentially by the bus and thus unlike
        * SentMessageCount this propertly does not need to be an atomic.
        * 
        * @volatile - set by bus thread, but read on service thread
         */
        public long getReceivedReceiptCount()
            {
            return __m_ReceivedReceiptCount;
            }
        
        // Accessor for the property "ReleaseAction"
        /**
         * Getter for property ReleaseAction.<p>
        * The continuation to run when the connection is released.
         */
        public com.oracle.coherence.common.base.Continuation getReleaseAction()
            {
            return __m_ReleaseAction;
            }
        
        // Accessor for the property "SentMessageCount"
        /**
         * Getter for property SentMessageCount.<p>
        * The number of ackable messages sent to this peer.
        * 
        * This is an atomic as many threads may concurrently increment it and
        * it is not just some dirty stat.
         */
        public java.util.concurrent.atomic.AtomicLong getSentMessageCount()
            {
            return __m_SentMessageCount;
            }
        
        // Accessor for the property "State"
        /**
         * Getter for property State.<p>
        * The state of the connection represented by this Connection. Valid
        * values are any of the STATE_* constants.
         */
        public int getState()
            {
            return __m_State;
            }
        
        // Accessor for the property "SuspectReceivedReceiptCount"
        /**
         * Getter for property SuspectReceivedReceiptCount.<p>
        * The ReceivedReceiptCount at the time this connection became suspect
         */
        public long getSuspectReceivedReceiptCount()
            {
            return __m_SuspectReceivedReceiptCount;
            }
        
        // Accessor for the property "SuspectTimeoutTimestamp"
        /**
         * Getter for property SuspectTimeoutTimestamp.<p>
        * The timestamp at which a suspect connection will be considered to
        * have timedout.
         */
        public long getSuspectTimeoutTimestamp()
            {
            return __m_SuspectTimeoutTimestamp;
            }
        
        // Accessor for the property "Established"
        /**
         * Getter for property Established.<p>
        * Indicates that this connection is allowed to add messages to the
        * service incoming queue. Once set, it will never be negated. This
        * property is modified only while holding synchronization on the
        * connection.
         */
        public boolean isEstablished()
            {
            return __m_Established;
            }
        
        // Accessor for the property "Released"
        /**
         * Getter for property Released.<p>
        * Returns true iff the connection has been released.
         */
        public boolean isReleased()
            {
            synchronized (this)
                {
                // this synchronization makes the State property visible
                // to the current thread (see release())
                return getState() >= STATE_DISCONNECTING;
                }
            }
        
        /**
         * Invoked once it has been identified that the we've failed to deliver
        * a message after an extended period.  Once this state occurs, this
        * method will be called once per onInterval cycle until the connection
        * is terminated or delivery succeeds.
         */
        public void onDeliveryTimeout()
            {
            // import com.oracle.coherence.common.net.exabus.EndPoint;
            // import com.oracle.coherence.common.util.Duration;
            
            if (getState() < STATE_DISCONNECTING)
                {
                MessageHandler  handler = (MessageHandler) get_Module();
                EndPoint peer    = getPeer();
            
                _trace("Disconnecting with " + peer + " after failing to deliver a message for "
                    + new Duration(handler.getDeliveryTimeoutMillis() * 1000000L), 3);
                
                handler.getMessageBus().disconnect(peer);
                }
            }
        
        /**
         * Event handler. Called on the Service thread.
         */
        public void onDisconnect(Throwable tReason)
            {
            // import Component.Net.Cluster as Cluster;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
            // import com.oracle.coherence.common.util.Duration;
            // import java.io.IOException;
            // import javax.net.ssl.SSLException;
            
            MessageHandler handler = (MessageHandler) get_Module();
            
            if (getState() != STATE_DISCONNECTING && !handler.isClosing())
                {
                // this indicates that the peer left, but that TCMP hasn't detected it (yet)
                // this would be quite bad if TCMP never detects it, but absolutely normal
                // otherwise as there is a natural race condition between death detection of
                // the various underlying network protocol (TCMP, TCP, ...)
                
                // register with the guardian, expecting that we'll have learned of the member
                // departure over TCMP before the timeout expires
            
                // as the number of pending disconnects increases, decrease the amount of time
                // before taking action.  The intent is to quickly kill a member who's bus has
                // failed to a larger degree (by counting the number of disconnected channels)
            
                // Note: we track pending disconnects at the machine rather then connection or member level
                // to help combat rolling restarts from triggering members to self destruct.  In a rolling
                // restart it is supposed to be safe to kill all members on a machine at the same time.  If
                // this is a large number of members that were started sequentially then they are likely
                // each other's TcpRing buddies, and thus the job of detecting and declaring their deaths will
                // likely fall onto the first member which is on another machine.  While a TcpRing buddy can
                // normally detect a death in under a millisecond, this particular case can be a bit worse.
                // Since a single buddy would have the responsibility of detecting death of many members it
                // would do so by trying to open a new TCP connection to them and being actively rejected.
                // Normally this also only takes about 1ms, except on Windows where it can take a few seconds.
                // For this reason we track deaths at the machine level, and thus only truncate our wait time
                // as the number of machines we are disconnected from increases.  Additionally restrict the
                // lower bound on how long we wait to be depenent on the IpMon timeout as it specificies how
                // long it should take us to detect a downed machine.
                
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service    = handler.getService();
                Cluster cluster    = (Cluster) service.getCluster();
                long    cMillisMax = cluster.getDependencies().getPublisherResendTimeoutMillis() * 3 / 2;
                long    cMillisMin = cluster.getIpMonitor().getAddressTimeout() * 3 / 2;
                long    cMillis    = cMillisMin == 0
                    ? cMillisMax // without IpMon we don't do truncated waits
                    : Math.max(cMillisMin, cMillisMax >> handler.getHungConnectionIPs().size());
            
                Member member = getMember();
                if (member != null)
                    {
                    int nImportance = service.compareImportance(member);
                    if (nImportance > 0 ||
                       (nImportance == 0 && service.getThisMember().getTimestamp() < member.getTimestamp()))
                        {
                        // give more important member a better chance at survival, ideally only one service
                        // member will need to take action and kill itself
                        cMillis += cMillis / 2;
                        }
                    }
            
                String sReason;
                if (tReason == null)
                    {
                    sReason = "n/a";
                    }
                else
                    {
                    sReason = tReason.getMessage();
                    if (sReason == null || sReason.length() == 0)
                        {
                        sReason = tReason.getClass().getName();
                        }
            
                    _trace(getStackTrace(tReason), tReason instanceof SSLException
                        ? 2
                        : tReason instanceof IOException
                            ? 9 : 7);
                    }
                
                _trace("Detected disconnect (" + sReason + ") of " + this +
                    " awaiting ServiceLeft notification with timeout of " +
                    new Duration(cMillis * 1000000L) + " based on " +
                    ((MessageHandler) get_Module()).getDisconnectCounter() + " concurrent disconnects" ,
                    tReason instanceof SSLException ? 2 : 7);
                service.guard(this, cMillis, 1.0F);
                }
            
            setState(MessageHandler.Connection.STATE_DISCONNECTED);
            setDisconnectCause(tReason);
            }
        
        /**
         * Invoked from onInterval when the connection is found to be idle.
         */
        public void onIdle()
            {
            }
        
        public void onInterval()
            {
            // import com.tangosol.util.Base;
            
            long ldtNow   = Base.getLastSafeTimeMillis(); // updated by Service.run prior to calling Service.onInterval->Handler.onInterval->Connection.onInterval
            long cRecNow  = getReceivedReceiptCount();
            long cRecLast = getSuspectReceivedReceiptCount();
            
            setSuspectReceivedReceiptCount(cRecNow);
            
            if (cRecNow > cRecLast)
                {
                // we've made progress; we're not stuck
                long ldtLast = getSuspectTimeoutTimestamp();
                if (ldtLast != 0 && ldtNow - ldtLast > getDeliveryTimeoutMillis() / 2)
                    {
                    setLastHeuristicDeathTimestamp(ldtNow);
                    } 
                setSuspectTimeoutTimestamp(0L);
                setLastHealthyTimestamp(ldtNow);
                }
            else if (cRecNow < getSentMessageCount().get())
                {
                // we could be stuck, or just delayed; setup a timeout
                long ldtTimeout = getSuspectTimeoutTimestamp();
                if (ldtTimeout == 0L)
                    {
                    // first onInterval call since we became "stuck", setup timeout
                    setSuspectTimeoutTimestamp(ldtNow + getDeliveryTimeoutMillis());
                    }
                else if (ldtNow > ldtTimeout)
                    {
                    onDeliveryTimeout();
                    }
                // else; do nothing until we timeout
                }
            else // cRecNow == getSuspectReceivedReceiptCount == getSentMessageCount; we continue to be idle
                {
                onIdle();
                }
            }
        
        /**
         * Event handler. Called on the Service thread.
         */
        public void onReleased()
            {
            // import com.oracle.coherence.common.base.Continuation;
            // import com.tangosol.net.Guardian$GuardContext as com.tangosol.net.Guardian.GuardContext;
            
            if (getState() != STATE_DISCONNECTED)
                {
                _trace("Unexpected RELEASE event: " + this, 1); // soft assert
                }
            
            setState(STATE_RELEASED);
            
            com.tangosol.net.Guardian.GuardContext context = getContext();
            if (context != null)
                {
                context.release();
                }
            
            Continuation action = getReleaseAction();
            if (action != null)
                {
                setReleaseAction(null);
            
                action.proceed(null);
                }
            }
        
        /**
         * Construct and deserialize a Message based on the supplied
         * BufferSequence
         *
         * @return the message
         */
        public Message prepareMessage(com.oracle.coherence.common.io.BufferSequence bufseq)
                throws java.io.IOException
            {
            // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
            // import com.tangosol.io.ReadBuffer;
            // import com.tangosol.io.ReadBuffer$BufferInput as com.tangosol.io.ReadBuffer.BufferInput;
            // import com.oracle.coherence.common.io.Buffers;
            // import com.oracle.coherence.common.internal.util.HeapDump;
            
            MessageHandler handler  = (MessageHandler) get_Module();
            ReadBuffer     buffer   = handler.createReadBuffer(bufseq);
            com.tangosol.io.ReadBuffer.BufferInput          input    = buffer.getBufferInput();
            int            nService = input.readShort(); // message header contains svc id
            int            nMsgType = input.readShort(); // message header contains type
            
            // reset the buffer - will be skipped later (see Grid.deserializeMessage)
            input.setOffset(0);
            
            // construct the incoming message
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service;
            
            try
                {
                service = handler.getServiceById(nService);
                }
            catch (RuntimeException e) // ArrayIOOBE
                {
                // SR 3-15874362211
                setLastBadMessage(bufseq);
                String sDump = HeapDump.dumpHeapForBug("SR-3-15874362211");
                setLastBadMessage(null); // only hold it long enough that it will be easily found in the heap dump
                
                throw new IllegalStateException("Heap dump " + sDump +
                        " has been generated due to an invalid service id " +
                        nService + " from " + this + " received in message: " +
                        Buffers.toString(bufseq, true, 1024), e);
                }
            
            if (service == null)
                {
                _trace("Ignoring message from " + this + " for locally stopped service " +
                       nService + "; message type " + nMsgType, 3);
                return null;
                }
            
            try
                {
                Message msg = service.instantiateMessage(nMsgType);
            
                msg.setFromMember(getMember());
                msg.setReadBuffer(buffer);
                msg.setDeserializationRequired(true);
            
                return msg;
                }
            catch (RuntimeException e)
                {
                _trace("Received corrupted message from " + this + " content " + Buffers.toString(bufseq, /*buf delim*/ true, /*cbLimit*/ 1024*1024), 1);
                throw e;
                }
            }
        
        // From interface: com.tangosol.net.Guardable
        public void recover()
            {
            }
        
        /**
         * Release the connection to the peer. Called on the service thread.
        * 
        * @param continuation  the continuation to call after the connection is
        * released
         */
        public void release(com.oracle.coherence.common.base.Continuation continuation)
            {
            // This method is called by the ServiceLeft notification, which itself could be sent
            // by the ClusterService in two different scenarios:
            //   - a normal service departure
            //   - a synthetic notification triggered by an "unexpected" join request
            //     caused by an aborted join attempt
            //     (for example see ClusterService$ServiceJoining.onReceived()
            //
            // In the first case, we need to defer the removal of the service member until all outstanding inbound messages
            // from that member are delivered and the RELEASE event is processed (signifying the "end of message stream").
            // While waiting for the RELEASE, any re-join attempt by the same member will be rejected
            // (see Grid$NotifyServiceJoining.onReceived())
            //
            // In the second case, the Grid$NotifyServiceLeft will always be immediately followed by the
            // Grid$NotifyServiceJoining, which will be rejected if we are to defer calling the continuation until
            // the RELEASE event is processed.
            //
            // However, if we can prove that there could be no outstanding messages, we could call the continuation immediately
            // and thus avoiding the rejection. The Established property serves as an efficient means to do so
            // (see MessageHandler#processMessage).
            
            int nState = getState();
            if (nState < STATE_DISCONNECTING)
                {
                setState(STATE_DISCONNECTING);
                }
            else
                {
                _assert(nState < STATE_RELEASED);
                }
            
            boolean fEstablished;
            synchronized (this)
                {
                // this synchronization also makes the State property visible
                // to the current thread (see establish())
                fEstablished = isEstablished();
                }
            
            if (fEstablished)
                {
                setReleaseAction(continuation);
                }
            else if (continuation != null)
                {
                continuation.proceed(null);
                }
            
            ((MessageHandler) get_Module()).getMessageBus().release(getPeer());
            }
        
        // From interface: com.tangosol.net.Guardable
        // Accessor for the property "Context"
        /**
         * Setter for property Context.<p>
        * The Connection's GuardContext or null if it is not currently guarded.
         */
        public void setContext(com.tangosol.net.Guardian.GuardContext context)
            {
            // despite making use of a SafeHashMap, we still need to be synchronized here to avoid the possibility of
            // the context being changed concurrently by multiple threads which can happen if for instance the
            // guardable releases just as the guardian terminates.  Without the synchronization we could have issues
            // such as incurrent set/unset and we could leave an IP in the map.
            
            synchronized (this)
                {
                Member member = getMember();
                if (member != null)
                    {
                    if (getContext() == null)
                        {
                        if (context != null) // start being guarded; add IP
                            {            
                            ((MessageHandler) get_Module()).getHungConnectionIPs().put(member.getAddress(), null);
                            }
                        // else; null -> null; don't change count we're still not guarded
                        }
                    else
                        {
                        if (context == null) // no-longer guarded; remove IP
                            {
                            ((MessageHandler) get_Module()).getHungConnectionIPs().remove(member.getAddress());
                            }
                        // else; non-null -> non-null; don't change we're still guarded
                        }
                    }
            
                __m_Context = (context);
                }
            }
        
        // Accessor for the property "DisconnectCause"
        /**
         * Setter for property DisconnectCause.<p>
        * The reason, if any, that was provided with the DISCONNECT event.
         */
        public void setDisconnectCause(Throwable tCause)
            {
            __m_DisconnectCause = tCause;
            }
        
        // Accessor for the property "Established"
        /**
         * Setter for property Established.<p>
        * Indicates that this connection is allowed to add messages to the
        * service incoming queue. Once set, it will never be negated. This
        * property is modified only while holding synchronization on the
        * connection.
         */
        protected void setEstablished(boolean fEstablished)
            {
            __m_Established = fEstablished;
            }
        
        // Accessor for the property "LastBadMessage"
        /**
         * Setter for property LastBadMessage.<p>
        * The last bad message received.  This is held only temporarily so that
        * it can be easily found in the HeapDump as part of diagnosing SR
        * 3-15874362211.
         */
        public void setLastBadMessage(com.oracle.coherence.common.io.BufferSequence sequenceMessage)
            {
            __m_LastBadMessage = sequenceMessage;
            }
        
        // Accessor for the property "LastHealthyTimestamp"
        /**
         * Setter for property LastHealthyTimestamp.<p>
        * The timestamp at which this connection was last proved to be healthy.
         */
        public void setLastHealthyTimestamp(long lTimestamp)
            {
            __m_LastHealthyTimestamp = lTimestamp;
            }
        
        // Accessor for the property "LastHeuristicDeathTimestamp"
        /**
         * Setter for property LastHeuristicDeathTimestamp.<p>
        * The last time the connection was stuck for more then 1/2 of the
        * delivery timeout.
         */
        protected void setLastHeuristicDeathTimestamp(long lTimestamp)
            {
            __m_LastHeuristicDeathTimestamp = lTimestamp;
            }
        
        // Accessor for the property "Member"
        /**
         * Setter for property Member.<p>
        * The Member this Connection object is for.
         */
        public void setMember(Member member)
            {
            _assert(member != null && getMember() == null, "Not resettable");
            
            __m_Member = (member);
            }
        
        // Accessor for the property "Peer"
        /**
         * Setter for property Peer.<p>
        * The EndPoint this Connection object is for.
         */
        public void setPeer(com.oracle.coherence.common.net.exabus.EndPoint peer)
            {
            _assert(peer != null);
            _assert(getPeer() == null || getPeer().equals(peer), "Not resettable");
            
            __m_Peer = (peer);
            }
        
        // Accessor for the property "ReceivedReceiptCount"
        /**
         * Setter for property ReceivedReceiptCount.<p>
        * The number of acked messages from this peer.
        * 
        * receipts are delivired sequentially by the bus and thus unlike
        * SentMessageCount this propertly does not need to be an atomic.
        * 
        * @volatile - set by bus thread, but read on service thread
         */
        public void setReceivedReceiptCount(long atomicCount)
            {
            __m_ReceivedReceiptCount = atomicCount;
            }
        
        // Accessor for the property "ReleaseAction"
        /**
         * Setter for property ReleaseAction.<p>
        * The continuation to run when the connection is released.
         */
        public void setReleaseAction(com.oracle.coherence.common.base.Continuation cont)
            {
            __m_ReleaseAction = cont;
            }
        
        // Accessor for the property "SentMessageCount"
        /**
         * Setter for property SentMessageCount.<p>
        * The number of ackable messages sent to this peer.
        * 
        * This is an atomic as many threads may concurrently increment it and
        * it is not just some dirty stat.
         */
        private void setSentMessageCount(java.util.concurrent.atomic.AtomicLong atomicCount)
            {
            __m_SentMessageCount = atomicCount;
            }
        
        // Accessor for the property "State"
        /**
         * Setter for property State.<p>
        * The state of the connection represented by this Connection. Valid
        * values are any of the STATE_* constants.
         */
        public void setState(int nState)
            {
            __m_State = nState;
            }
        
        // Accessor for the property "SuspectReceivedReceiptCount"
        /**
         * Setter for property SuspectReceivedReceiptCount.<p>
        * The ReceivedReceiptCount at the time this connection became suspect
         */
        protected void setSuspectReceivedReceiptCount(long lTimestamp)
            {
            __m_SuspectReceivedReceiptCount = lTimestamp;
            }
        
        // Accessor for the property "SuspectTimeoutTimestamp"
        /**
         * Setter for property SuspectTimeoutTimestamp.<p>
        * The timestamp at which a suspect connection will be considered to
        * have timedout.
         */
        protected void setSuspectTimeoutTimestamp(long lTimestamp)
            {
            __m_SuspectTimeoutTimestamp = lTimestamp;
            }
        
        // From interface: com.tangosol.net.Guardable
        public void terminate()
            {
            // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
            
            MessageHandler handler = ((MessageHandler) get_Module());
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = handler.getService();
            
            // no need to log per-connection information, that would have already been logged
            // as part of standard guardian logging
            _trace("This member has been unexpectedly disconnected from members on " +
                Math.max(1, handler.getHungConnectionIPs().size()) + " machines running service " +
                service.getServiceName() + "; stopping service", 1);
            
            Throwable tReason = getDisconnectCause();
            if (tReason != null)
                {
                _trace(tReason);
                }
            service.stop();
            }
        
        // Declared at the super level
        public String toString()
            {
            // import com.tangosol.util.Base;
            // import com.oracle.coherence.common.util.Duration;
            // import com.oracle.coherence.common.net.exabus.EndPoint;
            
            long ldtLast  = getLastHealthyTimestamp();
            long ldtStuck = getLastHeuristicDeathTimestamp();
            long ldtNow   = Base.getSafeTimeMillis();
            long ldtNext  = getSuspectTimeoutTimestamp();
            
            Member   member  = getMember();
            EndPoint peer    = getPeer();
            MessageHandler  handler = (MessageHandler) get_Module();
            
            return get_Name()
                + " {Peer="    + peer
                + ", Service=" + handler.getService().getServiceName()
                + ", Member="  + (member == null ? 0 : member.getId())
                + (isEstablished() ? "" : ", Not established")
                + ", State="   + formatStateName(getState())
                + (ldtLast  == 0 ? "" : ", lastAck=" + new Duration((ldtNow - ldtLast) * 1000000L))
                + (ldtStuck == 0 ? "" : ", lastStuck=" + new Duration((ldtNow - ldtStuck) * 1000000L))
                + (ldtNext  == 0 ? "" : ", pendingAckTimeout=" + new Duration(Math.max(0, ldtNext - ldtNow) * 1000000L))
                + ", " + handler.getMessageBus().toString(peer)
                + "}";
            }
        }

    // ---- class: com.tangosol.coherence.component.net.MessageHandler$EventCollector
    
    /**
     * The Collector implementation used by the MessageHandler.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EventCollector
            extends    com.tangosol.coherence.component.Util
            implements com.oracle.coherence.common.base.Collector
        {
        // ---- Fields declarations ----
        
        /**
         * Property ET_BACKLOG_EXCESSIVE
         *
         * Ordinal value for Event.Type.BACKLOG_EXCESSIVE.
         */
        public static final int ET_BACKLOG_EXCESSIVE = 5;
        
        /**
         * Property ET_BACKLOG_NORMAL
         *
         * Ordinal value for Event.Type.BACKLOG_NORMAL.
         */
        public static final int ET_BACKLOG_NORMAL = 6;
        
        /**
         * Property ET_CLOSE
         *
         * Ordinal value for Event.Type.CLOSE.
         */
        public static final int ET_CLOSE = 1;
        
        /**
         * Property ET_CONNECT
         *
         * Ordinal value for Event.Type.CONNECT.
         */
        public static final int ET_CONNECT = 2;
        
        /**
         * Property ET_DISCONNECT
         *
         * Ordinal value for Event.Type.DISCONNECT.
         */
        public static final int ET_DISCONNECT = 3;
        
        /**
         * Property ET_MESSAGE
         *
         * Ordinal value for Event.Type.MESSAGE.
         */
        public static final int ET_MESSAGE = 9;
        
        /**
         * Property ET_OPEN
         *
         * Ordinal value for Event.Type.OPEN.
         */
        public static final int ET_OPEN = 0;
        
        /**
         * Property ET_RECEIPT
         *
         * Ordinal value for Event.Type.RECEIPT.
         */
        public static final int ET_RECEIPT = 7;
        
        /**
         * Property ET_RELEASE
         *
         * Ordinal value for Event.Type.RELEASE.
         */
        public static final int ET_RELEASE = 4;
        
        /**
         * Property ET_SIGNAL
         *
         * Ordinal value for Event.Type.SIGNAL.
         */
        public static final int ET_SIGNAL = 8;
        
        private static void _initStatic$Default()
            {
            }
        
        // Static initializer (from _initStatic)
        static
            {
            // import com.oracle.coherence.common.net.exabus.Event$Type as com.oracle.coherence.common.net.exabus.Event.Type;
            
            // unfortunately TDE does not support Enums, so we need to assign 
            // all constants manually; let's verify that it was done correctly
            
            _initStatic$Default();
            
            com.oracle.coherence.common.net.exabus.Event.Type[] enumType = new com.oracle.coherence.common.net.exabus.Event.Type[]
                {
                com.oracle.coherence.common.net.exabus.Event.Type.OPEN,
                com.oracle.coherence.common.net.exabus.Event.Type.CLOSE,
                com.oracle.coherence.common.net.exabus.Event.Type.CONNECT,
                com.oracle.coherence.common.net.exabus.Event.Type.DISCONNECT,
                com.oracle.coherence.common.net.exabus.Event.Type.RELEASE,
                com.oracle.coherence.common.net.exabus.Event.Type.BACKLOG_EXCESSIVE,
                com.oracle.coherence.common.net.exabus.Event.Type.BACKLOG_NORMAL,
                com.oracle.coherence.common.net.exabus.Event.Type.RECEIPT,
                com.oracle.coherence.common.net.exabus.Event.Type.SIGNAL,
                com.oracle.coherence.common.net.exabus.Event.Type.MESSAGE,
                };
            
            for (int i = 0, c = enumType.length; i < c; i++)
                {
                com.oracle.coherence.common.net.exabus.Event.Type type = enumType[i];
            
                _assert(type.ordinal() == i, "Invalid ordinal value for " + type);
                }
            }
        
        // Default constructor
        public EventCollector()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public EventCollector(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.MessageHandler.EventCollector();
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
                clz = Class.forName("com.tangosol.coherence/component/net/MessageHandler$EventCollector".replace('/', '.'));
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
        
        // From interface: com.oracle.coherence.common.base.Collector
        /**
         * Usually called on a Bus thread, but could also be called [by] on a
        * client thread calling into the bus API.
         */
        public void add(Object oEvent)
            {
            // import com.oracle.coherence.common.net.exabus.Event;
            
            MessageHandler handler = (MessageHandler) get_Module();
            Event   event   = (Event) oEvent;
            
            try
                {
                // pseudo enum support
                switch (event.getType().ordinal())
                    {
                    case ET_CLOSE:
                        if (handler.isClosing())
                            {
                            // process everything else on the service thread
                            handler.postEventMessage(event);
                            break;
                            }
                        else
                            {
                            // the bus closed on its own
                            event.dispose();
                            throw new IllegalStateException("Unexpected CLOSE event");
                            }
            
                    case ET_RECEIPT:
                        handler.processReceipt(event.getEndPoint(), (Message) event.getContent(), handler.getDisconnectCounter().get() > 0L);        
                        event.dispose();
                        break;
            
                    case ET_BACKLOG_EXCESSIVE:
                        handler.onBacklog(event.getEndPoint(), /*fExcessive*/ true);
                        event.dispose();
                        break;
                        
                    case ET_BACKLOG_NORMAL:
                        handler.onBacklog(event.getEndPoint(), /*fExcessive*/ false);
                        event.dispose();
                        break;
            
                    case ET_DISCONNECT:
                        handler.getDisconnectCounter().incrementAndGet();
                        handler.postEventMessage(event);
                        break;
            
                    case ET_RELEASE:
                        handler.getDisconnectCounter().decrementAndGet();
                        handler.postEventMessage(event);
                        break;
            
                    case ET_MESSAGE:
                        handler.processMessage(event);
                        break;
            
                    default:
                        // process everything else on the service thread
                        handler.postEventMessage(event);
                        break;
                    }
                }
            catch (Throwable e)
                {
                handler.onException(e);
                }
            }
        
        // From interface: com.oracle.coherence.common.base.Collector
        public void flush()
            {
            ((MessageHandler) get_Module()).getService().flush();
            }
        }
    }
