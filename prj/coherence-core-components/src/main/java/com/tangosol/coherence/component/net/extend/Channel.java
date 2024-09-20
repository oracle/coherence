
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.Channel

package com.tangosol.coherence.component.net.extend;

import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.security.AccessAdapter;
import com.tangosol.internal.net.security.AccessAdapterPrivilegedAction;
import com.tangosol.io.Serializer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.PofBufferWriter;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.Message;
import com.tangosol.net.messaging.Request;
import com.tangosol.net.messaging.Response;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Gate;
import com.tangosol.util.LongArray;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.security.auth.Subject;

/**
 * Base definition of a Channel component.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Channel
        extends    com.tangosol.coherence.component.net.Extend
        implements com.tangosol.io.pof.PofContext,
                   com.tangosol.io.pof.PofSerializer,
                   com.tangosol.net.messaging.Channel
    {
    // ---- Fields declarations ----
    
    /**
     * Property AccessAdapter
     *
     * AccessAdapter is an internal, environment-specific component which knows
     * how to execute PrivilegedActions.
     */
    private com.tangosol.internal.net.security.AccessAdapter __m_AccessAdapter;
    
    /**
     * Property AttributeMap
     *
     * The Map used to store Channel attributes.
     */
    private java.util.Map __m_AttributeMap;
    
    /**
     * Property CloseNotify
     *
     * Peer notification flag used when the Channel is closed upon exiting the
     * ThreadGate (see CloseOnExit property).
     * 
     * @volatile
     */
    private volatile boolean __m_CloseNotify;
    
    /**
     * Property CloseOnExit
     *
     * If true, the Thread that is currently executing within the Channel
     * should close it immedately upon exiting the Channel's ThreadGate.
     * 
     * @volatile
     */
    private volatile boolean __m_CloseOnExit;
    
    /**
     * Property CloseThrowable
     *
     * The Throwable to pass to the close() method when the Channel is closed
     * upon exiting the ThreadGate (see CloseOnExit property).
     * 
     * @volatile
     */
    private volatile Throwable __m_CloseThrowable;
    
    /**
     * Property Connection
     *
     * The Connection that created this Channel.
     * 
     * @volatile
     * @see com.tangosol.net.messaging.Channel#getConnection
     */
    private volatile transient com.tangosol.net.messaging.Connection __m_Connection;
    
    /**
     * Property EmptySubject
     *
     * A Subject that represents nobody. It is subsituted when the received
     * Subject is null.
     */
    private javax.security.auth.Subject __m_EmptySubject;
    
    /**
     * Property Id
     *
     * The unique identifier for this Channel.
     * 
     * @see com.tangosol.net.messaging.Channel#getId
     */
    private int __m_Id;
    
    /**
     * Property LegacyPromotion
     *
     * Controls behavior in which a null subject is "promoted" to the proxy
     * service subject. Determined by system property
     * tangosol.coherence.security.legacypromotion.
     * 
     * @see #onInit()
     * 
     * @see #execute()
     */
    private boolean __m_LegacyPromotion;
    
    /**
     * Property MessageFactory
     *
     * The MessageFactory used create Message objects that may be sent through
     * this Channel.
     * 
     * @see com.tangosol.net.messaging.Channel#getMessageFactory
     */
    private com.tangosol.net.messaging.Protocol.MessageFactory __m_MessageFactory;
    
    /**
     * Property Open
     *
     * True if the Channel is open; false otherwise.
     * 
     * @volatile
     * @see com.tangosol.net.messaging.Channel#isOpen
     */
    private volatile boolean __m_Open;
    
    /**
     * Property Receiver
     *
     * The optional Receiver that processes unsolicited Message objects sent
     * through this Channel.
     * 
     * @see com.tangosol.net.messaging.Channel#getReceiver
     */
    private com.tangosol.net.messaging.Channel.Receiver __m_Receiver;
    
    /**
     * Property RequestArray
     *
     * The LongArray of open RequestStatus objects, indexed by Request
     * identifier.
     */
    private com.tangosol.util.LongArray __m_RequestArray;
    
    /**
     * Property RequestId
     *
     * A counter used to generate unique identifiers for Requests sent through
     * this Channel.
     */
    private transient AtomicLong __m_RequestId = new AtomicLong();
    
    /**
     * Property SecureContext
     *
     * True if either the channel subject or the proxy service subject exist.
     * When both are null, we can optimize out the Subject.doAs() call.
     * 
     * @see #execute() 
     */
    private boolean __m_SecureContext;
    
    /**
     * Property Serializer
     *
     * The Serializer used to serialize and deserialize payload objects carried
     * by Messages sent through this Channel.
     * 
     * @see com.tangosol.net.messaging.Channel#getSerializer
     */
    private com.tangosol.io.Serializer __m_Serializer;
    
    /**
     * Property Subject
     *
     * The optional Subject associated with the Channel.
     * 
     * @see com.tangosol.net.messaging.Channel#getChannel
     */
    private javax.security.auth.Subject __m_Subject;
    
    /**
     * Property ThreadGate
     *
     * A ThreadGate used to prevent concurrent use of this Channel while it is
     * being closed.
     */
    private transient com.tangosol.util.Gate __m_ThreadGate;
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
        __mapChildren.put("MessageAction", Channel.MessageAction.get_CLASS());
        }
    
    // Default constructor
    public Channel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Channel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setAttributeMap(new com.tangosol.util.SafeHashMap());
            setRequestArray(new com.tangosol.util.SparseArray());
            setThreadGate(new com.tangosol.util.ThreadGate());
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
        return new com.tangosol.coherence.component.net.extend.Channel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/Channel".replace('/', '.'));
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
     * @throws ConnectionException if the Channel is closed or closing
     */
    protected void assertOpen()
        {
        // import com.tangosol.net.messaging.ConnectionException;
        
        if (!isOpen())
            {
            // REVIEW
            throw new ConnectionException("channel is closed", getConnection());
            }
        }
    
    /**
     * Calculate the default timeout in milliseconds for the given Request.
    * 
    * @param request  the Request
    * 
    * @return the default timeout for the given Request in milliseconds
    * 
    * @see #registerRequest()
    * @see #request
     */
    protected long calculateRequestTimeout(com.tangosol.net.messaging.Request request)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        // import com.tangosol.net.PriorityTask;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer manager = getConnectionManager();
        
        long cMillis = 0L;
        if (manager != null)
            {
            cMillis = manager.getRequestTimeout();
            if (request instanceof PriorityTask)
                {
                cMillis = manager.adjustTimeout(cMillis,
                        ((PriorityTask) request).getRequestTimeoutMillis());
                }
        
            // when the RequestContext is in place (COH-1026) we will also have:
            /*
            RequestContext ctx = RequestContext.getContext();
            if (ctx != null)
                {
                cMillis = manager.adjustTimeout(cMillis, ctx.getRequestTimeout());
                }
            */
            }
        
        return cMillis;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    public void close()
        {
        close(true, null);
        }
    
    /**
     * Close the Channel due to an exception or error.
    * 
    * @param fNotify  if true, notify the peer that the Channel is being closed
    * @param e  the optional reason why the Channel is being closed
     */
    public void close(boolean fNotify, Throwable e)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        
        if (isOpen())
            {
            if (getId() == 0)
                {
                throw new UnsupportedOperationException("cannot closed reserved channel: 0");
                }
        
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer manager = getConnectionManager();
            if (Thread.currentThread() == manager.getThread())
                {
                closeInternal(fNotify, e, 0L);
                }
            else
                {
                _assert(!isActiveThread(),
                        "cannot close a channel while executing within the channel");
        
                // COH-18404 - not necessary to wait for the close to complete
                manager.closeChannel(this, fNotify, e, false);
                }
            }
        }
    
    /**
     * The close() implementation method. This method is called on the service
    * thread.
    * 
    * @param fNotify  if true, notify the peer that the Channel is being closed
    * @param e  the optional reason why the Channel is being closed
    * @param cMillis  the number of milliseconds to wait for the Channel to
    * close; pass 0L to perform a non-blocking close or -1L to wait forever
    * 
    * @return true iff the invocation of this method closed the Channel
     */
    public boolean closeInternal(boolean fNotify, Throwable e, long cMillis)
        {
        // import Component.Net.Extend.Connection as Connection;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer$MessageFactory$NotifyChannelClosed as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyChannelClosed;
        // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Channel$Receiver as com.tangosol.net.messaging.Channel.Receiver;
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
        // import com.tangosol.util.LongArray;
        // import java.util.Iterator;
        
        if (!isOpen())
            {
            return false;
            }
        
        // cancel all pending requests and hold synchronization on the request
        // array while closing to prevent new requests from being registered
        LongArray laStatus       = getRequestArray();
        com.tangosol.net.messaging.Channel.Receiver  receiver       = getReceiver();
        boolean   fCloseReceiver = false;  
        
        synchronized (laStatus)
            {
            Throwable eStatus = e == null ? new ConnectionException("channel closed", getConnection()) : e;
            for (Iterator iter = laStatus.iterator(); iter.hasNext(); )
                {
                com.tangosol.net.messaging.Request.Status status = (com.tangosol.net.messaging.Request.Status) iter.next();
                iter.remove();
                status.cancel(eStatus);
                }
        
            // close the com.tangosol.net.messaging.Channel
            boolean fClose = gateClose(cMillis);
            try
                {
                if (!fClose)
                    {
                    // can't close the gate; signal to the holding Thread(s) that it
                    // must close the com.tangosol.net.messaging.Channel immediately after exiting the gate
                    setCloseOnExit(true);
                    setCloseNotify(fNotify);
                    setCloseThrowable(e);
        
                    // double check if we can close the gate, as we want to be sure
                    // that the Thread(s) saw the close notification prior to exiting
                    fClose = gateClose(0L);
                    }
        
                if (fClose && isOpen())
                    {
                    // notify the receiver that the com.tangosol.net.messaging.Channel is closing
                    if (receiver != null)
                        {
                        fCloseReceiver = true;
                        try
                            {
                            receiver.unregisterChannel(this);
                            }
                        catch (Throwable ee)
                            {
                            _trace(ee, "Error unregistering channel from receiver: "
                                    + receiver);
                            }
                        }
        
                    setOpen(false);
                    }
                else
                    {
                    return false;
                    }
                }
            finally
                {
                if (fClose)
                    {
                    gateOpen();
                    }
                }
            }
        
        if (fCloseReceiver)
            {
            try
                {
                // needs to be done outside the request array synchronization block
                receiver.onChannelClosed(this);
                }
            catch (Throwable ee)
                {
                _trace(ee, "Error notifying channel closed to receiver: "
                        + receiver);
                }
            }
        
        // notify the peer that the com.tangosol.net.messaging.Channel is now closed
        if (fNotify && !isOpen() && getId() != 0)
            {
            // send a NotifyChannelClosed to the peer via "Channel0"
            try
                {
                Connection connection = (Connection) getConnection();
                com.tangosol.net.messaging.Channel    channel0   = connection.getChannel(0);
                com.tangosol.net.messaging.Protocol.MessageFactory    factory0   = channel0.getMessageFactory();
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyChannelClosed    message    = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyChannelClosed) factory0.createMessage(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyChannelClosed.TYPE_ID);
        
                message.setCause(e);
                message.setChannelId(getId());
        
                channel0.send(message);
                }
            catch (RuntimeException ignored) {}
            }
        
        // notify the Connection that the com.tangosol.net.messaging.Channel is closed
        ((Connection) getConnection()).unregisterChannel(this);
        
        // notify the ConnectionManager that the com.tangosol.net.messaging.Channel is closed
        getConnectionManager().onChannelClosed(this);
        
        return true;
        }
    
    /**
     * Create a new Message of the specified type using this Channel's
    * MessageFactory.
    * 
    * @param nType  the type identifier of the Message to create
    * 
    * @return a new Message of the specified type
     */
    public com.tangosol.net.messaging.Message createMessage(int nType)
        {
        return getMessageFactory().createMessage(nType);
        }
    
    // From interface: com.tangosol.io.pof.PofSerializer
    public Object deserialize(com.tangosol.io.pof.PofReader in)
            throws java.io.IOException
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.pof.PofSerializer;
        // import com.tangosol.util.Binary;
        
        Serializer serializer = getSerializer();
        if (serializer instanceof PofSerializer)
            {
            return ((PofSerializer) serializer).deserialize(in);
            }
        
        Binary bin = in.readBinary(0);
        in.readRemainder();
        
        // use the serializer to read the object from a binary property
        return serializer.deserialize(bin.getBufferInput());
        }
    
    // From interface: com.tangosol.io.pof.PofContext
    public Object deserialize(com.tangosol.io.ReadBuffer.BufferInput in)
            throws java.io.IOException
        {
        // import com.tangosol.io.pof.PofBufferReader;
        // import com.tangosol.io.pof.PofReader;
        
        PofReader reader = new PofBufferReader(in, this);
        return reader.readObject(-1);
        }
    
    /**
     * Execute the given Message.
    * 
    * @param message  the Message to execute
     */
    protected void execute(com.tangosol.net.messaging.Message message)
        {
        // import com.tangosol.internal.net.security.AccessAdapter;
        // import com.tangosol.internal.net.security.AccessAdapterPrivilegedAction;
        // import com.tangosol.net.messaging.Channel$Receiver as com.tangosol.net.messaging.Channel.Receiver;
        // import com.tangosol.net.messaging.Request;
        // import com.tangosol.net.messaging.Response;
        // import java.security.PrivilegedAction;
        // import javax.security.auth.Subject;
        
        com.tangosol.net.messaging.Channel.Receiver receiver;
        if (message instanceof Response)
            {
            // solicited Message
            receiver = null;
            }
        else
            {
            // unsolicited Message
            receiver = getReceiver();
            }
        
        // execute the Message in the context of the Channel's Subject;
        // since the doAs() cost is relatively high, we avoid that call
        // in the most common case when neither the Channel nor the proxy service
        // has associated Subjects
        
        AccessAdapter adapter = getAccessAdapter();
        
        if (isSecureContext() || adapter != null)
            {
            Channel.MessageAction action = new Channel.MessageAction();
            action.setMessage(message);
            action.setReceiver(receiver);
        
            Subject subject = getSubject();
        
            if (isLegacyPromotion())
                {
                // backward compatible behavior: don't check if the subject is null. if null,
                // the proxy service subject will be used, effectively promoting the null
                // to the proxy service subject
                Subject.doAs(subject, action);
                }
            else
                {
                // Use empty subject if the received subject is null
                subject = subject == null ? getEmptySubject() : subject;
        
                Subject.doAs(subject, adapter == null
                    ? (PrivilegedAction) action
                    : new AccessAdapterPrivilegedAction(adapter, subject, action));
                }
            }
        else
            {
            if (receiver == null)
                {
                message.run();
                }
            else
                {
                receiver.onMessage(message);
                }
            }
        }
    
    /**
     * Attempt to close the Channel ThreadGate.
    * 
    * @param cMillis  the number of milliseconds to wait for the ThreadGate to
    * close; pass 0L to perform a non-blocking close or -1L to wait forever
    * 
    * @return true if the Channel ThreadGate was closed; false otherwise
     */
    protected boolean gateClose(long cMillis)
        {
        return getThreadGate().close(cMillis);
        }
    
    /**
     * Enter the Connection and Channel ThreadGate (in that order).
    * 
    * @throws ConnectionException if the Connection or Channel is closing or
    * closed
     */
    public void gateEnter()
        {
        // import Component.Net.Extend.Connection as Connection;
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.util.Gate;
        
        Connection connection = (Connection) getConnection();
        connection.gateEnter();
        try
            {
            Gate gate = getThreadGate();
        
            // if the thread is entering for the first time, throw an exception if
            // the Channel has been marked for close; this prevents new threads from
            // entering the Channel and thus keeping it open longer than necessary
            if (isCloseOnExit() && !gate.isEnteredByCurrentThread())
                {
                // REVIEW
                throw new ConnectionException("channel is closing", connection);
                }
        
            if (gate.enter(0L)) // see #gateClose
                {
                try
                    {
                    assertOpen();
                    }
                catch (Throwable ee)
                    {
                    gate.exit();
                    throw ensureRuntimeException(ee);
                    }
                }
            else
                {
                // REVIEW
                throw new ConnectionException("connection is closing", connection);
                }
            }
        catch (Throwable e)
            {
            connection.gateExit();
            throw ensureRuntimeException(e);
            }
        }
    
    /**
     * Exit the Channel and Connection ThreadGate (in that order).
     */
    public void gateExit()
        {
        // import Component.Net.Extend.Connection as Connection;
        // import com.tangosol.util.Gate;
        
        Gate gate = getThreadGate();
        gate.exit();
        ((Connection) getConnection()).gateExit();
        
        // see if we've been asked to close the Channel
        if (isCloseOnExit() && !gate.isEnteredByCurrentThread())
            {
            boolean fClose = gateClose(0L);
            try
                {
                if (fClose && isOpen())
                    {
                    gateOpen();
                    fClose = false;
                    close(isCloseNotify(), getCloseThrowable());
                    }
                }
            finally
                {
                if (fClose)
                    {
                    gateOpen();
                    }
                }
            }
        }
    
    /**
     * Open the Channel ThreadGate.
     */
    protected void gateOpen()
        {
        getThreadGate().open();
        }
    
    /**
     * Generate and return a new unique Request identifier.
    * 
    * @return the new unique Request identifier
     */
    protected long generateRequestId()
        {
        return __m_RequestId.getAndIncrement();
        }
    
    // Accessor for the property "AccessAdapter"
    /**
     * Getter for property AccessAdapter.<p>
    * AccessAdapter is an internal, environment-specific component which knows
    * how to execute PrivilegedActions.
     */
    public com.tangosol.internal.net.security.AccessAdapter getAccessAdapter()
        {
        return __m_AccessAdapter;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    public Object getAttribute(String sName)
        {
        return getAttributeMap().get(sName);
        }
    
    // Accessor for the property "AttributeMap"
    /**
     * Getter for property AttributeMap.<p>
    * The Map used to store Channel attributes.
     */
    public java.util.Map getAttributeMap()
        {
        return __m_AttributeMap;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    public java.util.Map getAttributes()
        {
        // import java.util.HashMap;
        // import java.util.Map;
        
        Map map = getAttributeMap();
        synchronized (map)
            {
            return new HashMap(map);
            }
        }
    
    // From interface: com.tangosol.io.pof.PofContext
    public Class getClass(int nTypeId)
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.pof.PofContext;
        
        Serializer serializer = getSerializer();
        if (serializer instanceof PofContext)
            {
            return ((PofContext) serializer).getClass(nTypeId);
            }
        
        throw new IllegalStateException("cannot determine class for user type ID: "
                + nTypeId);
        }
    
    // From interface: com.tangosol.io.pof.PofContext
    public String getClassName(int nTypeId)
        {
        return getClass(nTypeId).getName();
        }
    
    // Accessor for the property "CloseThrowable"
    /**
     * Getter for property CloseThrowable.<p>
    * The Throwable to pass to the close() method when the Channel is closed
    * upon exiting the ThreadGate (see CloseOnExit property).
    * 
    * @volatile
     */
    public Throwable getCloseThrowable()
        {
        return __m_CloseThrowable;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    // Accessor for the property "Connection"
    /**
     * Getter for property Connection.<p>
    * The Connection that created this Channel.
    * 
    * @volatile
    * @see com.tangosol.net.messaging.Channel#getConnection
     */
    public com.tangosol.net.messaging.Connection getConnection()
        {
        return __m_Connection;
        }
    
    /**
     * Return the ConnectionManager that created this Channel or null if the
    * Channel has been closed.
    * 
    * @return the ConnectionManager
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer getConnectionManager()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        // import com.tangosol.net.messaging.Connection as com.tangosol.net.messaging.Connection;
        
        com.tangosol.net.messaging.Connection connection = getConnection();
        return connection == null ? null : (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) connection.getConnectionManager();
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        // import com.tangosol.net.messaging.Channel$Receiver as com.tangosol.net.messaging.Channel.Receiver;
        // import com.tangosol.net.messaging.Connection as com.tangosol.net.messaging.Connection;
        
        StringBuilder sb = new StringBuilder(32);
        
        sb.append("Id=")
          .append(getId());
        
        boolean fOpen = isOpen();
        sb.append(", Open=")
          .append(fOpen);
        
        if (fOpen)
            {
            com.tangosol.net.messaging.Connection connection = getConnection();
            sb.append(", Connection=")
              .append(connection == null
                    ? "null"
                    : String.valueOf(connection.getId()));
        
            MessageFactory factory = (MessageFactory) getMessageFactory();
            if (factory != null)
                {
                Protocol protocol = (Protocol) factory.getProtocol();
                if (protocol != null)
                    {
                    sb.append(", Protocol=").append(protocol).append(", ")
                      .append("NegotiatedProtocolVersion=").append(factory.getVersion());
                    }
                }
        
            com.tangosol.net.messaging.Channel.Receiver receiver = getReceiver();
            if (receiver != null)
                {
                sb.append(", Receiver=").append(receiver);
                }
            }
        
        return sb.toString();
        }
    
    // Accessor for the property "EmptySubject"
    /**
     * Getter for property EmptySubject.<p>
    * A Subject that represents nobody. It is subsituted when the received
    * Subject is null.
     */
    public javax.security.auth.Subject getEmptySubject()
        {
        return __m_EmptySubject;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    // Accessor for the property "Id"
    /**
     * Getter for property Id.<p>
    * The unique identifier for this Channel.
    * 
    * @see com.tangosol.net.messaging.Channel#getId
     */
    public int getId()
        {
        return __m_Id;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    // Accessor for the property "MessageFactory"
    /**
     * Getter for property MessageFactory.<p>
    * The MessageFactory used create Message objects that may be sent through
    * this Channel.
    * 
    * @see com.tangosol.net.messaging.Channel#getMessageFactory
     */
    public com.tangosol.net.messaging.Protocol.MessageFactory getMessageFactory()
        {
        return __m_MessageFactory;
        }
    
    // From interface: com.tangosol.io.pof.PofContext
    public com.tangosol.io.pof.PofSerializer getPofSerializer(int nTypeId)
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.pof.PofContext;
        
        Serializer serializer = getSerializer();
        if (serializer instanceof PofContext)
            {
            return ((PofContext) serializer).getPofSerializer(nTypeId);
            }
        
        if (nTypeId == 0)
            {
            return this;
            }
        
        String sTarget;
        try
            {
            sTarget = getConnectionManager().toString();
            }
        catch (RuntimeException e)
            {
            sTarget = toString();
            }
        
        throw new IllegalStateException(sTarget +
                " has not been configured with a PofContext; " +
                " this channel cannot decode POF-encoded user types");
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    // Accessor for the property "Receiver"
    /**
     * Getter for property Receiver.<p>
    * The optional Receiver that processes unsolicited Message objects sent
    * through this Channel.
    * 
    * @see com.tangosol.net.messaging.Channel#getReceiver
     */
    public com.tangosol.net.messaging.Channel.Receiver getReceiver()
        {
        return __m_Receiver;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    public com.tangosol.net.messaging.Request getRequest(long lId)
        {
        // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
        // import com.tangosol.util.LongArray;
        
        LongArray laStatus = getRequestArray();
        synchronized (laStatus) // see #closeInternal
            {
            com.tangosol.net.messaging.Request.Status status = (com.tangosol.net.messaging.Request.Status) laStatus.get(lId);
            return status == null ? null : status.getRequest();
            }
        }
    
    // Accessor for the property "RequestArray"
    /**
     * Getter for property RequestArray.<p>
    * The LongArray of open RequestStatus objects, indexed by Request
    * identifier.
     */
    public com.tangosol.util.LongArray getRequestArray()
        {
        return __m_RequestArray;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    // Accessor for the property "Serializer"
    /**
     * Getter for property Serializer.<p>
    * The Serializer used to serialize and deserialize payload objects carried
    * by Messages sent through this Channel.
    * 
    * @see com.tangosol.net.messaging.Channel#getSerializer
     */
    public com.tangosol.io.Serializer getSerializer()
        {
        return __m_Serializer;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    // Accessor for the property "Subject"
    /**
     * Getter for property Subject.<p>
    * The optional Subject associated with the Channel.
    * 
    * @see com.tangosol.net.messaging.Channel#getChannel
     */
    public javax.security.auth.Subject getSubject()
        {
        return __m_Subject;
        }
    
    // Accessor for the property "ThreadGate"
    /**
     * Getter for property ThreadGate.<p>
    * A ThreadGate used to prevent concurrent use of this Channel while it is
    * being closed.
     */
    protected com.tangosol.util.Gate getThreadGate()
        {
        return __m_ThreadGate;
        }
    
    // From interface: com.tangosol.io.pof.PofContext
    public int getUserTypeIdentifier(Class clz)
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.pof.PofContext;
        
        Serializer serializer = getSerializer();
        if (serializer instanceof PofContext)
            {
            return ((PofContext) serializer).getUserTypeIdentifier(clz);
            }
        
        _assert(clz != null);
        return 0;
        }
    
    // From interface: com.tangosol.io.pof.PofContext
    public int getUserTypeIdentifier(Object o)
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.pof.PofContext;
        
        Serializer serializer = getSerializer();
        if (serializer instanceof PofContext)
            {
            return ((PofContext) serializer).getUserTypeIdentifier(o);
            }
        
        _assert(o != null);
        return 0;
        }
    
    // From interface: com.tangosol.io.pof.PofContext
    public int getUserTypeIdentifier(String sClass)
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.pof.PofContext;
        
        Serializer serializer = getSerializer();
        if (serializer instanceof PofContext)
            {
            return ((PofContext) serializer).getUserTypeIdentifier(sClass);
            }
        
        _assert(sClass != null);
        return 0;
        }
    
    // Accessor for the property "ActiveThread"
    /**
     * Getter for property ActiveThread.<p>
    * Return true if the calling thread is currently executing within the
    * Channel's ThreadGate.
     */
    public boolean isActiveThread()
        {
        return getThreadGate().isEnteredByCurrentThread();
        }
    
    // Accessor for the property "CloseNotify"
    /**
     * Getter for property CloseNotify.<p>
    * Peer notification flag used when the Channel is closed upon exiting the
    * ThreadGate (see CloseOnExit property).
    * 
    * @volatile
     */
    public boolean isCloseNotify()
        {
        return __m_CloseNotify;
        }
    
    // Accessor for the property "CloseOnExit"
    /**
     * Getter for property CloseOnExit.<p>
    * If true, the Thread that is currently executing within the Channel should
    * close it immedately upon exiting the Channel's ThreadGate.
    * 
    * @volatile
     */
    public boolean isCloseOnExit()
        {
        return __m_CloseOnExit;
        }
    
    // Accessor for the property "LegacyPromotion"
    /**
     * Getter for property LegacyPromotion.<p>
    * Controls behavior in which a null subject is "promoted" to the proxy
    * service subject. Determined by system property
    * tangosol.coherence.security.legacypromotion.
    * 
    * @see #onInit()
    * 
    * @see #execute()
     */
    public boolean isLegacyPromotion()
        {
        return __m_LegacyPromotion;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    // Accessor for the property "Open"
    /**
     * Getter for property Open.<p>
    * True if the Channel is open; false otherwise.
    * 
    * @volatile
    * @see com.tangosol.net.messaging.Channel#isOpen
     */
    public boolean isOpen()
        {
        return __m_Open;
        }
    
    // Accessor for the property "SecureContext"
    /**
     * Getter for property SecureContext.<p>
    * True if either the channel subject or the proxy service subject exist.
    * When both are null, we can optimize out the Subject.doAs() call.
    * 
    * @see #execute() 
     */
    public boolean isSecureContext()
        {
        return __m_SecureContext;
        }
    
    // From interface: com.tangosol.io.pof.PofContext
    public boolean isUserType(Class clz)
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.pof.PofContext;
        
        Serializer serializer = getSerializer();
        if (serializer instanceof PofContext)
            {
            return ((PofContext) serializer).isUserType(clz);
            }
        
        _assert(clz != null);
        return false;
        }
    
    // From interface: com.tangosol.io.pof.PofContext
    public boolean isUserType(Object o)
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.pof.PofContext;
        
        Serializer serializer = getSerializer();
        if (serializer instanceof PofContext)
            {
            return ((PofContext) serializer).isUserType(o);
            }
        
        _assert(o != null);
        return false;
        }
    
    // From interface: com.tangosol.io.pof.PofContext
    public boolean isUserType(String sClass)
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.pof.PofContext;
        
        Serializer serializer = getSerializer();
        if (serializer instanceof PofContext)
            {
            return ((PofContext) serializer).isUserType(sClass);
            }
        
        _assert(sClass != null);
        return false;
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
        // import com.tangosol.coherence.config.Config;
        // import com.tangosol.net.security.SecurityHelper;
        
        setSecureContext(SecurityHelper.getCurrentSubject() != null);
        
        setEmptySubject(SecurityHelper.EMPTY_SUBJECT);
        
        // undocumented system property to preserve backward compatible promotion of null subjects                
        setLegacyPromotion(Config.getBoolean("coherence.security.legacypromotion"));
        
        super.onInit();
        }
    
    /**
     * Called after a Request has completed either successfully or
    * unsuccessfully.
    * 
    * @param status  the Status representing the asynchronous Request
     */
    public void onRequestCompleted(com.tangosol.net.messaging.Request.Status status)
        {
        unregisterRequest(status);
        }
    
    /**
     * Open the Channel.
     */
    public void open()
        {
        openInternal();
        }
    
    /**
     * The open() implementation method. This method is called on the service
    * thread.
     */
    public void openInternal()
        {
        // import com.tangosol.net.messaging.Channel$Receiver as com.tangosol.net.messaging.Channel.Receiver;
        
        if (isOpen())
            {
            return;
            }
        
        _assert(getConnection()     != null);
        _assert(getMessageFactory() != null);
        _assert(getSerializer()     != null);
        
        setOpen(true);
        
        // notify the receiver that the Channel is open
        com.tangosol.net.messaging.Channel.Receiver receiver = getReceiver();
        if (receiver != null)
            {
            try
                {
                receiver.registerChannel(this);
                }
            catch (Throwable e)
                {
                _trace(e, "Error registering channel with receiver: " + receiver);
                }
            }
        
        // notify the ConnectionManager that the Channel is opened
        getConnectionManager().onChannelOpened(this);
        }
    
    /**
     * Asynchronous Message send implementation.
    * 
    * @param message  the Message to send asynchronously
     */
    protected void post(com.tangosol.net.messaging.Message message)
        {
        // import com.tangosol.net.messaging.Request;
        // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
        // import com.tangosol.net.messaging.Response;
        // import com.tangosol.util.Base;
        
        boolean fEnter;
        if (message instanceof Response)
            {
            _assert(isActiveThread(),
                    "can only send a response while executing within a channel");
            fEnter = false;
            }
        else
            {
            fEnter = true;
            }
        
        if (fEnter)
            {
            gateEnter();
            }
        try
            {
            message.setChannel(this);
            getConnectionManager().post(message);
            }
        catch (Throwable e)
            {
            if (message instanceof Request)
                {
                com.tangosol.net.messaging.Request.Status status = ((Request) message).getStatus();
                if (status != null)
                    {
                    status.cancel(e);
                    }
                }
            throw Base.ensureRuntimeException(e);
            }
        finally
            {
            if (fEnter)
                {
                gateExit();
                }
            }
        }
    
    /**
     * Called when a Message is received via this Channel. This method is called
    * on the service thread ("Channel0" Messages) or on a daemon thread.
    * 
    * @param message  the Message
    * 
    * @see Peer#send
     */
    public void receive(com.tangosol.net.messaging.Message message)
        {
        // import Component.Net.Extend.Connection as Connection;
        // import Component.Net.Extend.Message.Request$Status as com.tangosol.coherence.component.net.extend.message.Request.Status;
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.net.messaging.Request;
        // import com.tangosol.net.messaging.Response;
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.LongArray;
        
        _assert(message != null);
        
        try
            {
            gateEnter();
            }
        catch (ConnectionException e)
            {
            // ignore: the Channel or Connection is closed or closing
            return;
            }
        try
            {
            if (message instanceof Request)
                {
                Request request = (Request) message;
                try
                    {
                    execute(message);
                    }
                catch (RuntimeException e)
                    {
                    Response response = request.ensureResponse();
                    _assert(response != null);
                    
                    // see Request#isIncoming and #run
                    if (request.getStatus() == null)
                        {
                        // report the exception and send it back to the peer
                        if (_isTraceEnabled(5))
                            {
                            _trace("An exception occurred while processing a "
                                    + ClassHelper.getSimpleName(message.getClass())
                                    + " for Service="
                                    + getConnectionManager().getServiceName()
                                    + ": " + getStackTrace(e), 5);
                            }
                        }
        
                    response.setFailure(true);
                    response.setResult(e);
                    }
        
                Response response = request.ensureResponse();
                _assert(response != null);
                response.setRequestId(request.getId());
        
                send(response);
                }
            else if (message instanceof Response)
                {
                Response  response = (Response) message;
                LongArray laStatus = getRequestArray();
                long      lId      = response.getRequestId();
        
                com.tangosol.coherence.component.net.extend.message.Request.Status status;
                synchronized (laStatus)
                    {
                    status = (com.tangosol.coherence.component.net.extend.message.Request.Status) laStatus.get(lId);
                    }
        
                if (status == null)
                    {
                    // ignore unsolicited Responses
                    }
                else
                    {
                    try
                        {
                        execute(response);
                        if (response.isFailure())
                            {
                            Object oResult = response.getResult();
        
                            // cancel the com.tangosol.coherence.component.net.extend.message.Request.Status 
                            if (oResult instanceof Throwable)
                                {
                                status.cancel((Throwable) oResult);
                                }
                            else
                                {
                                status.cancel(new RuntimeException(
                                        String.valueOf(oResult)));
                                }
                            }
                        else
                            {
                            status.setResponse(response);
                            }
                        }
                    catch (Throwable e)
                        {
                        status.cancel(e);
                        if (e instanceof Error)
                            {
                            throw (Error) e;
                            }
                        }
                    }
                }
            else
                {
                execute(message);
                }
            }
        catch (Throwable e)
            {
            Connection connection = (Connection) getConnection();
        
            // see Acceptor#onServiceStopped and Initiator#onServiceStopped
            if (!connection.isCloseOnExit() || !Thread.currentThread().isInterrupted())
                {
                _trace(e, "Caught an unhandled exception while processing a "
                        + ClassHelper.getSimpleName(message.getClass())
                        + " for Service=" + getConnectionManager().getServiceName());
                }
        
            if (getId() == 0)
                {
                connection.setCloseOnExit(true);
                connection.setCloseNotify(true);
                connection.setCloseThrowable(e);
                }
            else
                {
                setCloseOnExit(true);
                setCloseNotify(true);
                setCloseThrowable(e);
                }
        
            // propagate fatal error
            if (e instanceof Error)
                {
                throw (Error) e;
                }
            }
        finally
            {
            gateExit();
            }
        }
    
    /**
     * Create a Status for the given Request and register the Status in the
    * RequestArray.
    * 
    * @param request  the Request to register; must not be null
    * 
    * @return the new Status that represents the asynchronous Request
     */
    protected com.tangosol.net.messaging.Request.Status registerRequest(com.tangosol.net.messaging.Request request)
        {
        // import Component.Net.Extend.Message.Request$Status as com.tangosol.coherence.component.net.extend.message.Request.Status;
        // import com.tangosol.util.LongArray;
        
        _assert(request != null);
        
        com.tangosol.coherence.component.net.extend.message.Request.Status status = new com.tangosol.coherence.component.net.extend.message.Request.Status();
        status.setChannel(this);
        status.setDefaultTimeoutMillis(calculateRequestTimeout(request));
        status.setRequest(request);
        
        request.setStatus(status);
        
        LongArray laStatus = getRequestArray();
        synchronized (laStatus) // see #closeInternal
            {
            assertOpen();
        
            // generate a unique request ID
            long lId = generateRequestId();
            request.setId(lId);
        
            Object oStatus = laStatus.set(lId, status);
            if (oStatus != null)
                {
                laStatus.set(lId, oStatus);
                _assert(false, "duplicate request: " + request);
                }
            }
        
        return status;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    public Object removeAttribute(String sName)
        {
        return getAttributeMap().remove(sName);
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    public Object request(com.tangosol.net.messaging.Request request)
        {
        return request(request, -1L);
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    public Object request(com.tangosol.net.messaging.Request request, long cMillis)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
        // import com.tangosol.net.messaging.Response;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer manager = getConnectionManager();
        _assert(manager.getThread() != Thread.currentThread(),
                "request() is a blocking call and cannot be called on the service thread");
        
        // block until the service is ready
        manager.waitAcceptingClients();
        
        if (request == null)
            {
            throw new IllegalArgumentException("request cannot be null");
            }
        
        com.tangosol.net.messaging.Request.Status status = registerRequest(request);
        post(request);
        
        Response response = status.waitForResponse(cMillis);
        if (response.isFailure())
            {
            Object oResult = response.getResult();
            if (oResult instanceof Throwable)
                {
                throw ensureRuntimeException((Throwable) oResult);
                }
            else
                {
                throw new RuntimeException("received error: " + oResult);
                }
            }
        else
            {
            return response.getResult();
            }
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    public void send(com.tangosol.net.messaging.Message message)
        {
        if (message == null)
            {
            throw new IllegalArgumentException("message cannot be null");
            }
        
        post(message);
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    public com.tangosol.net.messaging.Request.Status send(com.tangosol.net.messaging.Request request)
        {
        // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
        
        if (request == null)
            {
            throw new IllegalArgumentException("request cannot be null");
            }
        
        com.tangosol.net.messaging.Request.Status status = registerRequest(request);
        post(request);
        
        return status;
        }
    
    // From interface: com.tangosol.io.pof.PofSerializer
    public void serialize(com.tangosol.io.pof.PofWriter out, Object o)
            throws java.io.IOException
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.WriteBuffer;
        // import com.tangosol.io.pof.PofSerializer;
        // import com.tangosol.util.BinaryWriteBuffer;
        
        Serializer serializer = getSerializer();
        if (serializer instanceof PofSerializer)
            {
            ((PofSerializer) serializer).serialize(out, o);
            }
        else
            {
            WriteBuffer buf = new BinaryWriteBuffer(32);
        
            // use the serializer to write the object out as a binary property
            serializer.serialize(buf.getBufferOutput(), o);
            out.writeBinary(0, buf.toBinary());
            out.writeRemainder(null);
            }
        }
    
    // From interface: com.tangosol.io.pof.PofContext
    public void serialize(com.tangosol.io.WriteBuffer.BufferOutput out, Object o)
            throws java.io.IOException
        {
        // import com.tangosol.io.pof.PofBufferWriter;
        // import com.tangosol.io.pof.PofWriter;
        
        PofWriter writer = new PofBufferWriter(out, this);
        writer.writeObject(-1, o);
        }
    
    // Accessor for the property "AccessAdapter"
    /**
     * Setter for property AccessAdapter.<p>
    * AccessAdapter is an internal, environment-specific component which knows
    * how to execute PrivilegedActions.
     */
    public void setAccessAdapter(com.tangosol.internal.net.security.AccessAdapter subject)
        {
        __m_AccessAdapter = subject;
        }
    
    // From interface: com.tangosol.net.messaging.Channel
    public Object setAttribute(String sName, Object oValue)
        {
        return getAttributeMap().put(sName, oValue);
        }
    
    // Accessor for the property "AttributeMap"
    /**
     * Setter for property AttributeMap.<p>
    * The Map used to store Channel attributes.
     */
    protected void setAttributeMap(java.util.Map map)
        {
        __m_AttributeMap = map;
        }
    
    // Accessor for the property "CloseNotify"
    /**
     * Setter for property CloseNotify.<p>
    * Peer notification flag used when the Channel is closed upon exiting the
    * ThreadGate (see CloseOnExit property).
    * 
    * @volatile
     */
    public void setCloseNotify(boolean f)
        {
        __m_CloseNotify = f;
        }
    
    // Accessor for the property "CloseOnExit"
    /**
     * Setter for property CloseOnExit.<p>
    * If true, the Thread that is currently executing within the Channel should
    * close it immedately upon exiting the Channel's ThreadGate.
    * 
    * @volatile
     */
    public void setCloseOnExit(boolean fClose)
        {
        __m_CloseOnExit = fClose;
        }
    
    // Accessor for the property "CloseThrowable"
    /**
     * Setter for property CloseThrowable.<p>
    * The Throwable to pass to the close() method when the Channel is closed
    * upon exiting the ThreadGate (see CloseOnExit property).
    * 
    * @volatile
     */
    public void setCloseThrowable(Throwable e)
        {
        __m_CloseThrowable = e;
        }
    
    // Accessor for the property "Connection"
    /**
     * Setter for property Connection.<p>
    * The Connection that created this Channel.
    * 
    * @volatile
    * @see com.tangosol.net.messaging.Channel#getConnection
     */
    public void setConnection(com.tangosol.net.messaging.Connection connection)
        {
        _assert(!isOpen());
        
        __m_Connection = (connection);
        }
    
    // Accessor for the property "EmptySubject"
    /**
     * Setter for property EmptySubject.<p>
    * A Subject that represents nobody. It is subsituted when the received
    * Subject is null.
     */
    public void setEmptySubject(javax.security.auth.Subject subject)
        {
        __m_EmptySubject = subject;
        }
    
    // Accessor for the property "Id"
    /**
     * Setter for property Id.<p>
    * The unique identifier for this Channel.
    * 
    * @see com.tangosol.net.messaging.Channel#getId
     */
    public void setId(int nId)
        {
        _assert(!isOpen());
        
        __m_Id = (nId);
        }
    
    // Accessor for the property "LegacyPromotion"
    /**
     * Setter for property LegacyPromotion.<p>
    * Controls behavior in which a null subject is "promoted" to the proxy
    * service subject. Determined by system property
    * tangosol.coherence.security.legacypromotion.
    * 
    * @see #onInit()
    * 
    * @see #execute()
     */
    protected void setLegacyPromotion(boolean fSecure)
        {
        __m_LegacyPromotion = fSecure;
        }
    
    // Accessor for the property "MessageFactory"
    /**
     * Setter for property MessageFactory.<p>
    * The MessageFactory used create Message objects that may be sent through
    * this Channel.
    * 
    * @see com.tangosol.net.messaging.Channel#getMessageFactory
     */
    public void setMessageFactory(com.tangosol.net.messaging.Protocol.MessageFactory factory)
        {
        _assert(!isOpen());
        
        __m_MessageFactory = (factory);
        }
    
    // Accessor for the property "Open"
    /**
     * Setter for property Open.<p>
    * True if the Channel is open; false otherwise.
    * 
    * @volatile
    * @see com.tangosol.net.messaging.Channel#isOpen
     */
    protected void setOpen(boolean fOpen)
        {
        __m_Open = fOpen;
        }
    
    // Accessor for the property "Receiver"
    /**
     * Setter for property Receiver.<p>
    * The optional Receiver that processes unsolicited Message objects sent
    * through this Channel.
    * 
    * @see com.tangosol.net.messaging.Channel#getReceiver
     */
    public void setReceiver(com.tangosol.net.messaging.Channel.Receiver receiver)
        {
        _assert(!isOpen());
        
        __m_Receiver = (receiver);
        }
    
    // Accessor for the property "RequestArray"
    /**
     * Setter for property RequestArray.<p>
    * The LongArray of open RequestStatus objects, indexed by Request
    * identifier.
     */
    protected void setRequestArray(com.tangosol.util.LongArray la)
        {
        __m_RequestArray = la;
        }
    
    // Accessor for the property "SecureContext"
    /**
     * Setter for property SecureContext.<p>
    * True if either the channel subject or the proxy service subject exist.
    * When both are null, we can optimize out the Subject.doAs() call.
    * 
    * @see #execute() 
     */
    protected void setSecureContext(boolean fSecure)
        {
        __m_SecureContext = fSecure;
        }
    
    // Accessor for the property "Serializer"
    /**
     * Setter for property Serializer.<p>
    * The Serializer used to serialize and deserialize payload objects carried
    * by Messages sent through this Channel.
    * 
    * @see com.tangosol.net.messaging.Channel#getSerializer
     */
    public void setSerializer(com.tangosol.io.Serializer serializer)
        {
        _assert(!isOpen());
        
        __m_Serializer = (serializer);
        }
    
    // Accessor for the property "Subject"
    /**
     * Setter for property Subject.<p>
    * The optional Subject associated with the Channel.
    * 
    * @see com.tangosol.net.messaging.Channel#getChannel
     */
    public void setSubject(javax.security.auth.Subject subject)
        {
        // import com.tangosol.net.security.SecurityHelper;
        
        setSecureContext(isSecureContext() || subject != null);
        
        __m_Subject = (subject);
        }
    
    // Accessor for the property "ThreadGate"
    /**
     * Setter for property ThreadGate.<p>
    * A ThreadGate used to prevent concurrent use of this Channel while it is
    * being closed.
     */
    protected void setThreadGate(com.tangosol.util.Gate gate)
        {
        __m_ThreadGate = gate;
        }
    
    /**
     * Unregister the given Status from the RequestArray.
    * 
    * @param request  the Status to unregister; must not be null
     */
    protected void unregisterRequest(com.tangosol.net.messaging.Request.Status status)
        {
        // import com.tangosol.util.LongArray;
        
        _assert(status != null);
        
        LongArray laStatus = getRequestArray();
        synchronized (laStatus) // see #closeInternal
            {
            laStatus.remove(status.getRequest().getId());
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.Channel$MessageAction
    
    /**
     * PrivilegedAction implementation used to process a received Message on
     * behalf of a Subject.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MessageAction
            extends    com.tangosol.coherence.component.net.Extend
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property Message
         *
         * The Message to process.
         */
        private com.tangosol.net.messaging.Message __m_Message;
        
        /**
         * Property Receiver
         *
         * The optional Receiver that will process the Message.
         */
        private com.tangosol.net.messaging.Channel.Receiver __m_Receiver;
        
        // Default constructor
        public MessageAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MessageAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.Channel.MessageAction();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/Channel$MessageAction".replace('/', '.'));
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
         * Return a human-readable description of this component.
        * 
        * @return a String representation of this component
         */
        protected String getDescription()
            {
            return "Message=" + getMessage() + ", Receiver=" + getReceiver();
            }
        
        // Accessor for the property "Message"
        /**
         * Getter for property Message.<p>
        * The Message to process.
         */
        public com.tangosol.net.messaging.Message getMessage()
            {
            return __m_Message;
            }
        
        // Accessor for the property "Receiver"
        /**
         * Getter for property Receiver.<p>
        * The optional Receiver that will process the Message.
         */
        public com.tangosol.net.messaging.Channel.Receiver getReceiver()
            {
            return __m_Receiver;
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
            // no-op: no children
            }
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            // import com.tangosol.net.messaging.Channel$Receiver as com.tangosol.net.messaging.Channel.Receiver;
            // import com.tangosol.net.messaging.Message;
            
            Message  message  = getMessage();
            com.tangosol.net.messaging.Channel.Receiver receiver = getReceiver();
            
            if (receiver == null)
                {
                message.run();
                }
            else
                {
                receiver.onMessage(message);
                }
            
            return null;
            }
        
        // Accessor for the property "Message"
        /**
         * Setter for property Message.<p>
        * The Message to process.
         */
        public void setMessage(com.tangosol.net.messaging.Message message)
            {
            __m_Message = message;
            }
        
        // Accessor for the property "Receiver"
        /**
         * Setter for property Receiver.<p>
        * The optional Receiver that will process the Message.
         */
        public void setReceiver(com.tangosol.net.messaging.Channel.Receiver receiver)
            {
            __m_Receiver = receiver;
            }
        }
    }
