
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.RequestMessage

package com.tangosol.coherence.component.net.message;

import com.tangosol.coherence.component.net.Packet;
import com.tangosol.coherence.component.net.Poll;
import com.tangosol.coherence.component.net.RequestContext;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.util.Base;
import java.util.Iterator;
import java.util.Map;

/**
 * The Message contains all of the information necessary to describe a message
 * to send or to describe a received message.
 * <p>
 * With regards to the use of Message components within clustered Services,
 * Services are designed by dragging Message components into them as static
 * children. These Messages are the components that a Service can send to other
 * running instances of the same Service within a cluster. To send a Message, a
 * Service calls <code>instantiateMessage(String sMsgName)</code> with the name
 * of the child, then configures the Message object and calls Service.send
 * passing the Message. An incoming Message is created by the Message Receiver
 * by calling the <code>Service.instantiateMessage(int nMsgType)</code> and the
 * configuring the Message using the Received data. The Message is then queued
 * in the Service's Queue. When the Service thread gets the Message out of the
 * Queue, it invokes onMessage passing the Message, and the default
 * implementation for onMessage in turn calls <code>onReceived()</code> on the
 * Message object.
 * <p>
 * A RequestMessage extends the generic Message and adds the capability to poll
 * one or more Members for responses. In the simplest case, the RequestMessage
 * with one destination Member implements the request/response model.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class RequestMessage
        extends    com.tangosol.coherence.component.net.Message
    {
    // ---- Fields declarations ----
    
    /**
     * Property FromPollId
     *
     * The PollId that this Message is a request for.
     */
    private long __m_FromPollId;
    
    /**
     * Property RequestContext
     *
     * A RequestContext object that carries state associated with a transaction
     * or a work unit that this requests is a part of. It is also used by
     * requests that have to be executed in an idempotent manner.
     * 
     * @see #instantiateRequestContext
     * @since Coherence 3.2
     */
    private com.tangosol.coherence.component.net.RequestContext __m_RequestContext;
    
    /**
     * Property RequestPoll
     *
     * This is the Poll that the RequestMessage creates to collect responses.
     */
    private com.tangosol.coherence.component.net.Poll __m_RequestPoll;
    
    /**
     * Property RequestTimeout
     *
     * Transient property optionally used on the client to indicate the (safe
     * local) time after which this logical request should be considered timed
     * out.
     * 
     * Note that a single logical request message may result in multiple
     * physical request messages being sent to mulitple members; this
     * RequestTimeout value will be cloned to all resulting RequestMessage
     * instances.
     * 
     * This value is lazily calculated by #getRequestTimeout or
     * #calculateTimeoutRemaining.
     */
    private transient long __m_RequestTimeout;
    
    // Default constructor
    public RequestMessage()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public RequestMessage(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
    
    // Getter for virtual constant Suspendable
    public boolean isSuspendable()
        {
        return false;
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.net.message.RequestMessage();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/RequestMessage".replace('/', '.'));
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
    
    // Declared at the super level
    /**
     * Add an instance child component with the specified name to this
    * component.
    * 
    * @param child  a component to add to this component as an instance child
    * @param name  a [unique] name to identify this child. If the name is not
    * specified (null is passed) then a unique child name will be automatically
    * assigned
    * 
    * Note: this method fires onAdd() notification only if the parent (this
    * component) has already been fully constructed.
    * Note2: component containment/aggregation produces children initialization
    * code (see __init()) that is executed while the parent is not flagged as
    * _Constructed yet
     */
    public void _addChild(com.tangosol.coherence.Component child, String name)
        {
        // import Component.Net.Poll;
        
        super._addChild(child, name);
        
        if (child instanceof Poll && getRequestPoll() == null)
            {
            setRequestPoll((Poll) child);
            }
        }
    
    /**
     * Calculate and return the remaining request timeout, or 0 if the request
    * should be waited for indefinitely.
    * If the timeout has passed, throw RequestTimeoutException.
     */
    public long checkTimeoutRemaining()
        {
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.Base;
        
        long ldtTimeout = getRequestTimeout();
        if (ldtTimeout == Long.MAX_VALUE)
            {
            return 0L;
            }
        else
            {
            long cMillis = ldtTimeout - Base.getSafeTimeMillis();
            if (cMillis > 0)
                {
                return cMillis;
                }
            else
                {
                throw new RequestTimeoutException();
                }
            }
        }
    
    // Declared at the super level
    /**
     * Instantiate a copy of this message. This is quite different from the
    * standard "clone" since only the "transmittable" portion of the message
    * (and none of the internal) state should be cloned.
     */
    public com.tangosol.coherence.component.net.Message cloneMessage()
        {
        RequestMessage msg = (RequestMessage) super.cloneMessage();
        
        msg.setRequestContext(getRequestContext());
        msg.setRequestTimeout(getRequestTimeout());
        
        return msg;
        }
    
    public com.tangosol.coherence.component.net.Poll ensureRequestPoll()
        {
        // import Component.Net.Poll;
        
        Poll poll = getRequestPoll();
        if (poll == null)
            {
            setRequestPoll(poll = instantiatePoll());
            }
        
        if (getFromPollId() == 0L)
            {
            setFromPollId(getService().getPollArray().add(poll));
            }
        
        return poll;
        }
    
    // Declared at the super level
    /**
     * Getter for property EstimatedByteSize.<p>
    * The estimated serialized size of this message.  A negative value
    * indicates that the size is unknown and that it is safe to estimate the
    * size via a double serialization.
     */
    public int getEstimatedByteSize()
        {
        return super.getEstimatedByteSize() +
            3 + // trint   - FromPollId (writeInternal)
            1;  //  boolean - RequestContext != null
                // Note: RequestContext is not estimated
        }
    
    // Accessor for the property "FromPollId"
    /**
     * Getter for property FromPollId.<p>
    * The PollId that this Message is a request for.
     */
    public long getFromPollId()
        {
        return __m_FromPollId;
        }
    
    // Accessor for the property "RequestContext"
    /**
     * Getter for property RequestContext.<p>
    * A RequestContext object that carries state associated with a transaction
    * or a work unit that this requests is a part of. It is also used by
    * requests that have to be executed in an idempotent manner.
    * 
    * @see #instantiateRequestContext
    * @since Coherence 3.2
     */
    public com.tangosol.coherence.component.net.RequestContext getRequestContext()
        {
        return __m_RequestContext;
        }
    
    // Accessor for the property "RequestPoll"
    /**
     * Getter for property RequestPoll.<p>
    * This is the Poll that the RequestMessage creates to collect responses.
     */
    public com.tangosol.coherence.component.net.Poll getRequestPoll()
        {
        return __m_RequestPoll;
        }
    
    // Accessor for the property "RequestTimeout"
    /**
     * Getter for property RequestTimeout.<p>
    * Transient property optionally used on the client to indicate the (safe
    * local) time after which this logical request should be considered timed
    * out.
    * 
    * Note that a single logical request message may result in multiple
    * physical request messages being sent to mulitple members; this
    * RequestTimeout value will be cloned to all resulting RequestMessage
    * instances.
    * 
    * This value is lazily calculated by #getRequestTimeout or
    * #calculateTimeoutRemaining.
     */
    public long getRequestTimeout()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        long ldtTimeout = __m_RequestTimeout;
        if (ldtTimeout == 0L)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
            long    cMillis = service.calculateRequestTimeout(this);  // 0 means infinite
        
            ldtTimeout = service.adjustWaitTime(cMillis - 1L, com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.TIME_SAFE);
        
            setRequestTimeout(ldtTimeout);
            }
        
        return ldtTimeout;
        }
    
    protected com.tangosol.coherence.component.net.Poll instantiatePoll()
        {
        // import Component.Net.Poll;
        // import com.tangosol.util.Base;
        // import java.util.Map;
        // import java.util.Iterator;
        
        Map map = get_ChildClasses();
        if (map != null)
            {
            Class clz = (Class) map.get("Poll");
            if (clz == null)
                {
                Class clzPoll = Poll.get_CLASS();
                for (Iterator iter = map.values().iterator(); iter.hasNext(); )
                    {
                    Class clzChild = (Class) iter.next();
                    if (clzPoll.isAssignableFrom(clzChild))
                        {
                        clz = clzChild;
                        break;
                        }
                    }
                }
            if (clz != null)
                {
                try
                    {
                    return (Poll) clz.newInstance();
                    }
                catch (Exception e)
                    {
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }
        
        return new Poll();
        }
    
    /**
     * Instantiate a context object associated with this request.
     */
    protected com.tangosol.coherence.component.net.RequestContext instantiateRequestContext()
        {
        // import Component.Net.RequestContext;
        
        RequestContext ctx = getService().instantiateRequestContext();
        if (ctx == null)
            {
            throw new IllegalStateException("Failed to instantiate RequestContext for " + get_Name());
            }
        return ctx;
        }
    
    // Declared at the super level
    /**
     * This is the event that occurs when a Message with NotifySent set to true
    * and the Message is sent and fully acknowledged. Note that this event does
    * not mean that all Members received the Message; it just means that those
    * Members that are still alive and that the Message was addressed to have
    * acknowledged the Message, as well as all older messages from this member.
     */
    public void onDelivery()
        {
        ensureRequestPoll().onDelivery(this);
        }
    
    // Declared at the super level
    /**
     * Asynchronously send this message.  The actual transmission of the message
    * may be deferred due to the send queue batching.
    * This method should not be called directly; see Grid#post(Message).
     */
    public void post()
        {
        // import com.tangosol.util.Base;
        
        try
            {
            ensureRequestPoll().prepareDispatch(this);
        
            super.post();
            }
        catch (Throwable e)
            {
            // failed to dispatch the message;
            // most likely reason is a serialization problem
        
            _trace("Failure (" + e.getMessage() + ") while sending " + this, 2);
        
            // close the poll on the service thread
            getService().doPollClose(ensureRequestPoll(), e);
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import Component.Net.RequestContext;
        
        super.read(input);
        
        if (input.readBoolean())
            {
            RequestContext ctx = instantiateRequestContext();
        
            ctx.readExternal(input);
        
            setRequestContext(ctx);
            }
        }
    
    // Declared at the super level
    public void readInternal(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import Component.Net.Packet;
        
        super.readInternal(input);
        
        // note: ensure that the Poll id is non-zero by or-ing with
        // a value beyond the trint range
        int nPollId = Packet.readUnsignedTrint(input);
        setFromPollId(0x1000000 | nPollId);
        }
    
    // Accessor for the property "FromPollId"
    /**
     * Setter for property FromPollId.<p>
    * The PollId that this Message is a request for.
     */
    public void setFromPollId(long pFromPollId)
        {
        __m_FromPollId = pFromPollId;
        }
    
    // Accessor for the property "RequestContext"
    /**
     * Setter for property RequestContext.<p>
    * A RequestContext object that carries state associated with a transaction
    * or a work unit that this requests is a part of. It is also used by
    * requests that have to be executed in an idempotent manner.
    * 
    * @see #instantiateRequestContext
    * @since Coherence 3.2
     */
    public void setRequestContext(com.tangosol.coherence.component.net.RequestContext ctx)
        {
        __m_RequestContext = ctx;
        }
    
    // Accessor for the property "RequestPoll"
    /**
     * Setter for property RequestPoll.<p>
    * This is the Poll that the RequestMessage creates to collect responses.
     */
    protected void setRequestPoll(com.tangosol.coherence.component.net.Poll poll)
        {
        __m_RequestPoll = poll;
        }
    
    // Accessor for the property "RequestTimeout"
    /**
     * Setter for property RequestTimeout.<p>
    * Transient property optionally used on the client to indicate the (safe
    * local) time after which this logical request should be considered timed
    * out.
    * 
    * Note that a single logical request message may result in multiple
    * physical request messages being sent to mulitple members; this
    * RequestTimeout value will be cloned to all resulting RequestMessage
    * instances.
    * 
    * This value is lazily calculated by #getRequestTimeout or
    * #calculateTimeoutRemaining.
     */
    protected void setRequestTimeout(long ldtTimeout)
        {
        __m_RequestTimeout = ldtTimeout;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import com.tangosol.io.ExternalizableLite;
        
        super.write(output);
        
        ExternalizableLite ctx = getRequestContext();
        if (ctx == null)
            {
            output.writeBoolean(false);
            }
        else
            {
            output.writeBoolean(true);
            ctx.writeExternal(output);
            }
        }
    
    // Declared at the super level
    public void writeInternal(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import Component.Net.Packet;
        
        super.writeInternal(output);
        
        long lPollId = getFromPollId();
        _assert(lPollId != 0l);
        Packet.writeTrint(output, lPollId);
        }
    }
