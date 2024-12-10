
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.Poll

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.Component;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.memberSet.EmptyMemberSet;
import com.tangosol.coherence.component.net.memberSet.LiteSingleMemberSet;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Notifier;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.TracingHelper;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.util.Base;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Poll contains information regarding a request sent to one or more
 * Cluster Members that require responses. A Service may poll other Members
 * that are running the same Service, and the Poll is used to wait for and
 * assemble the responses from each of those Members. A client thread may also
 * use the Poll to block on a response or set of responses, thus waiting for
 * the completion of the Poll. In its simplest form, which is a Poll that is
 * sent to one Member of the Cluster, the Poll actually represents the
 * request/response model.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Poll
        extends    com.tangosol.coherence.component.Net
    {
    // ---- Fields declarations ----
    
    /**
     * Property CloseableOnDelivery
     *
     * True if the poll is closeable once delivery notification is received for
     * the corresponding RequestMessage.
     * 
     * To optimize the memory usage, the value is stored in the _StateAux
     * property.
     * 
     * @functional
     */
    
    /**
     * Property ExpiryTimeMillis
     *
     * The time (in milliseconds) that this Poll object will expire.  Expired
     * polls will be automatically closed by the corresponding service.
     * 
     * Note: the expiry must be configured prior to posting the associated
     * message in order to have any effect.
     */
    private long __m_ExpiryTimeMillis;
    
    /**
     * Property InitTimeMillis
     *
     * The time (in milliseconds) that this Poll object was initialized.
     */
    private long __m_InitTimeMillis;
    
    /**
     * Property LeftMemberSet
     *
     * The Set of Members that left the cluster while the poll was open.
     */
    private MemberSet __m_LeftMemberSet;
    
    /**
     * Property Notifier
     *
     * Note the usage of the OffloadingMultiNotifier is relying on the
     * SingleWaiterCooperativeNotifier.flush() call in Grid.flush()
     */
    private transient com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier __m_Notifier;
    
    /**
     * Property PollId
     *
     * The Poll number assigned to this Poll by the Service.
     */
    private long __m_PollId;
    
    /**
     * Property RemainingMemberSet
     *
     * The Set of Members that have not yet responded to the poll and that are
     * still running the Service.
     */
    private MemberSet __m_RemainingMemberSet;
    
    /**
     * Property RespondedMemberSet
     *
     * The Set of Members that have responded to the poll request.
     */
    private MemberSet __m_RespondedMemberSet;
    
    /**
     * Property Result
     *
     * The result of the Poll. This property is used to collect the result of
     * the Poll and return it to the client thread that sent the original
     * RequestMessage.
     */
    private Object __m_Result;
    
    /**
     * Property Service
     *
     * The Service that is managing the poll.
     */
    private com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid __m_Service;
    
    /**
     * Property State
     *
     * The state of the poll, one of the STATE_ constants.
     * 
     * @volatile - for dirty read in waitCompletion
     */
    private volatile int __m_State;
    
    /**
     * Property STATE_CLOSED
     *
     * State indicate that a Poll is closed.
     */
    public static final int STATE_CLOSED = 2;
    
    /**
     * Property STATE_CLOSING
     *
     * State indicate that a Poll is in the processing of closing.
     */
    public static final int STATE_CLOSING = 1;
    
    /**
     * Property STATE_OPEN
     *
     * State indicate that a Poll is open.
     */
    public static final int STATE_OPEN = 0;
    
    /**
     * Property TracingSpan
     *
     * The OpenTracing Span associated with this poll.
     */
    private com.tangosol.internal.tracing.Span __m_TracingSpan;
    
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
        
        // state initialization: private properties
        try
            {
            __m_Notifier = new com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    // Getter for virtual constant Preprocessable
    public boolean isPreprocessable()
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
        return new com.tangosol.coherence.component.net.Poll();
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
            clz = Class.forName("com.tangosol.coherence/component/net/Poll".replace('/', '.'));
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
     * Check whether or not the timeout is reached.
    * 
    * @param ldtStart  an operation start time
    * @param cTimeout  the number of milliseconds left until timeout is
    * reached, or 0 for no timeout
    * 
    * @return an adjusted number of milliseconds (> 0) left until timeout is
    * reached, or 0 if no timeout was specified
    * @throws RequestTimeoutException if the timeout has been reached
     */
    protected long checkRequestTimeout(long ldtStart, long cTimeout)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.Base;
        
        if (cTimeout == 0L)
            {
            return 0L;
            }
        
        long cElapsed = Base.getSafeTimeMillis() - ldtStart;
        if (cElapsed >= cTimeout)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
            service.setStatsTimeoutCount(service.getStatsTimeoutCount() + 1L);
            throw new RequestTimeoutException("Request timed out after " + cElapsed + " milliseconds");
            }
        return cTimeout - Math.max(0L, cElapsed);
        }
    
    /**
     * Closes the poll, whether all Members have responded or not. The client of
    * the poll (or subclasses of the poll itself) can determine the results by
    * examining the Members that responded, left the service, or did neither by
    * the time the poll closed.
    * 
    * @see #LeftMemberSet
    * @see #RemainingMemberSet
    * @see #RespondedMemberSet
     */
    public void close()
        {
        // import com.tangosol.internal.tracing.Span;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        boolean fCleanup = false;
        synchronized (this)
            {
            if (getState() == STATE_OPEN)
                {
                setState(STATE_CLOSING);
                try
                    {
                    onCompletion();
                    }
                finally
                    {
                    Span span = getTracingSpan();
                    if (span != null)
                        {
                        // Span must be finished before we set STATE_CLOSED, to avoid
                        // it being inferred as the active span by notified thread
                        span.end();
                        }
        
                    setState(STATE_CLOSED);
        
                    getNotifier().signal();
                    fCleanup = true;
                    }
                }
            }
        
        // perform post-close cleanup outside of poll synchronization, this allows
        // the user thread waiting on this poll to proceed earlier
        if (fCleanup)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid svc = getService();
            if (svc != null)
                {
                svc.onPollClosed(this);
                }
            }
        }
    
    // Accessor for the property "Description"
    /**
     * Getter for property Description.<p>
    * Used for debugging purposes (from toString). Create a human-readable
    * description of the specific Message data.
     */
    public String getDescription()
        {
        return null;
        }
    
    // Accessor for the property "ExpiryTimeMillis"
    /**
     * Getter for property ExpiryTimeMillis.<p>
    * The time (in milliseconds) that this Poll object will expire.  Expired
    * polls will be automatically closed by the corresponding service.
    * 
    * Note: the expiry must be configured prior to posting the associated
    * message in order to have any effect.
     */
    public long getExpiryTimeMillis()
        {
        return __m_ExpiryTimeMillis;
        }
    
    // Accessor for the property "InitTimeMillis"
    /**
     * Getter for property InitTimeMillis.<p>
    * The time (in milliseconds) that this Poll object was initialized.
     */
    public long getInitTimeMillis()
        {
        return __m_InitTimeMillis;
        }
    
    // Accessor for the property "LeftMemberSet"
    /**
     * Getter for property LeftMemberSet.<p>
    * The Set of Members that left the cluster while the poll was open.
     */
    public MemberSet getLeftMemberSet()
        {
        return __m_LeftMemberSet;
        }
    
    // Accessor for the property "Notifier"
    /**
     * Getter for property Notifier.<p>
    * Note the usage of the OffloadingMultiNotifier is relying on the
    * SingleWaiterCooperativeNotifier.flush() call in Grid.flush()
     */
    public com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier getNotifier()
        {
        return __m_Notifier;
        }
    
    // Accessor for the property "PollId"
    /**
     * Getter for property PollId.<p>
    * The Poll number assigned to this Poll by the Service.
     */
    public long getPollId()
        {
        return __m_PollId;
        }
    
    // Accessor for the property "RemainingMemberSet"
    /**
     * Getter for property RemainingMemberSet.<p>
    * The Set of Members that have not yet responded to the poll and that are
    * still running the Service.
     */
    public MemberSet getRemainingMemberSet()
        {
        return __m_RemainingMemberSet;
        }
    
    // Accessor for the property "RespondedMemberSet"
    /**
     * Getter for property RespondedMemberSet.<p>
    * The Set of Members that have responded to the poll request.
     */
    public MemberSet getRespondedMemberSet()
        {
        return __m_RespondedMemberSet;
        }
    
    // Accessor for the property "Result"
    /**
     * Getter for property Result.<p>
    * The result of the Poll. This property is used to collect the result of
    * the Poll and return it to the client thread that sent the original
    * RequestMessage.
     */
    public Object getResult()
        {
        return __m_Result;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * The Service that is managing the poll.
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid getService()
        {
        return __m_Service;
        }
    
    // Accessor for the property "State"
    /**
     * Getter for property State.<p>
    * The state of the poll, one of the STATE_ constants.
    * 
    * @volatile - for dirty read in waitCompletion
     */
    public int getState()
        {
        return __m_State;
        }
    
    // Accessor for the property "TracingSpan"
    /**
     * Getter for property TracingSpan.<p>
    * The OpenTracing Span associated with this poll.
     */
    public com.tangosol.internal.tracing.Span getTracingSpan()
        {
        return __m_TracingSpan;
        }
    
    /**
     * Factory method for the responded member set.
     */
    protected MemberSet instantiateRespondedMemberSet()
        {
        // import Component.Net.MemberSet;
        
        return new MemberSet();
        }
    
    // Accessor for the property "CloseableOnDelivery"
    /**
     * Getter for property CloseableOnDelivery.<p>
    * True if the poll is closeable once delivery notification is received for
    * the corresponding RequestMessage.
    * 
    * To optimize the memory usage, the value is stored in the _StateAux
    * property.
    * 
    * @functional
     */
    public boolean isCloseableOnDelivery()
        {
        return get_StateAux() != 0;
        }
    
    // Accessor for the property "Closed"
    /**
     * Getter for property Closed.<p>
    * Virtual property indicating if a poll has been closed.
     */
    public boolean isClosed()
        {
        return getState() == STATE_CLOSED;
        }
    
    /**
     * This is the event that is executed when all the Members that were polled
    * have responded or have left the Service.
     */
    protected void onCompletion()
        {
        }
    
    /**
     * This is the event that occurs when the RequestMessage associated with
    * this poll has NotifyDelivery set to true and the Message is sent and
    * fully acknowledged.
    * 
    * @see Message.NotifyDelivery/Message.onDelivery
     */
    public void onDelivery(com.tangosol.coherence.component.net.message.RequestMessage msg)
        {
        if (getState() == STATE_OPEN && isCloseableOnDelivery())
            {
            close();
            }
        }
    
    /**
     * This is the event that occurs when the RequestMessage associated with
    * this poll failed in post()
     */
    public void onException(Throwable eReason)
        {
        // import com.tangosol.internal.tracing.TracingHelper;
        
        TracingHelper.augmentSpanWithErrorDetails(getTracingSpan(), true, eReason);
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
        // import Component.Net.MemberSet.EmptyMemberSet;
        // import com.tangosol.util.Base;
        
        super.onInit();
        
        EmptyMemberSet setEmpty =
            (EmptyMemberSet) EmptyMemberSet.get_Instance();
        setRemainingMemberSet(setEmpty);
        setRespondedMemberSet(setEmpty);
        setLeftMemberSet(setEmpty);
        setInitTimeMillis(Base.getSafeTimeMillis());
        }
    
    /**
     * This event occurs when a Member has left the Service (or died).  Note: as
    * the service does not assume internal knowledge of the Poll this method
    * will be called for any Member's which leave the service, include ones
    * which were never part of the Poll.
     */
    public synchronized void onLeft(Member member)
        {
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.EmptyMemberSet;
        
        MemberSet setRemain = getRemainingMemberSet();
        MemberSet setLeft   = getLeftMemberSet();
        if (!isClosed() && setRemain.contains(member))
            {
            if (setRemain.size() == 1 && setLeft.isEmpty())
                {
                // most common case is a poll of one Member
                MemberSet setTemp = setLeft;
                setLeft   = setRemain;
                setRemain = setTemp;
        
                setLeftMemberSet(setLeft);
                setRemainingMemberSet(setRemain);
                }
            else
                {
                setRemain.remove(member);
                if (setLeft instanceof EmptyMemberSet)
                    {
                    setLeft = new MemberSet();
                    setLeftMemberSet(setLeft);
                    }
                setLeft.add(member);
                }
            
            if (setRemain.isEmpty())
                {
                close();
                }
            }
        }
    
    /**
     * This event occurs when a response from a Member is processed.
     */
    public void onResponded(Member member)
        {
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.EmptyMemberSet;
        
        boolean fClose = false;
        synchronized (this)
            {
            MemberSet setRemain    = getRemainingMemberSet();
            MemberSet setResponded = getRespondedMemberSet();
        
            if (!isClosed() && setRemain.contains(member))
                {
                if (setRemain.size() == 1 && setResponded.isEmpty())
                    {
                    // most common case is a poll of one Member
                    MemberSet setTemp = setResponded;
                    setResponded = setRemain;
                    setRemain    = setTemp;
        
                    setRespondedMemberSet(setResponded);
                    setRemainingMemberSet(setRemain);
                    }
                else
                    {
                    setRemain.remove(member);
                    if (setResponded instanceof EmptyMemberSet)
                        {
                        setResponded = instantiateRespondedMemberSet();
                        setRespondedMemberSet(setResponded);
                        }
                    setResponded.add(member);
                    }
        
                if (setRemain.isEmpty())
                    {
                    fClose = true;
                    }
                }
            }
        
        // close will re-acquire sync, but needs to be able to drop it for a post-close optimization
        if (fClose)
            {
            close();
            }
        }
    
    /**
     * This event occurs for each response Message from each polled Member.
     */
    public void onResponse(Message msg)
        {
        if (!isClosed())
            {
            onResponded(msg.getFromMember());
            }
        }
    
    /**
     * This method is called just before the parent RequestMessage is dispatched.
     */
    public void prepareDispatch(com.tangosol.coherence.component.net.message.RequestMessage msg)
        {
        // import com.tangosol.internal.tracing.Span;
        // import com.tangosol.internal.tracing.Span$Metadata as com.tangosol.internal.tracing.Span.Metadata;
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import Component.Net.MemberSet.LiteSingleMemberSet;
        // import Component.Net.MemberSet.EmptyMemberSet;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        // ensure that the poll has been registered
        _assert(getPollId() != 0L);
        
        // build remaining set of Members to poll
        MemberSet setMsg = msg.getToMemberSet();
        MemberSet setPoll;
        switch (setMsg.size())
            {
            case 0:
                setPoll = (EmptyMemberSet) EmptyMemberSet.get_Instance();
                break;
        
            case 1:
                setPoll = LiteSingleMemberSet.copyFrom(setMsg);
                break;
        
            default:
                setPoll = new MemberSet();
                setPoll.addAll(setMsg);
                break;
            }
        
        setRemainingMemberSet(setPoll);
        
        // before potentially blocking on the poll, make sure that
        // all polled members still exist; this is necessary to
        // support the other half of this insurance, which is the
        // cleanup processing done by the NotifyServiceLeft message
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid   service   = getService();
        MemberSet setMember = service.getServiceMemberSet();
        
        if (service.getServiceState() == com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.SERVICE_STOPPED)
            {
            // service is stopped; we must close the poll here
            close();
            }
        else if (setPoll.isEmpty())
            {
            // no recipients; close the poll on the service thread
            service.doPollClose(this);
            }
        else if (setMember == null)
            {
            if (setPoll.size() == 1 && setPoll.contains(service.getThisMember()))
                {
                // local polls are allowed to be posted *before* the service starts
                }
            else
                {
                throw new IllegalStateException();
                }
            }
        else if (!setMember.containsAll(setPoll))
            {
            MasterMemberSet setMaster = service.getClusterMemberSet();
            for (int i = setPoll.getFirstId(), iLast = setPoll.getLastId(); i <= iLast; ++i)
                {
                if (setPoll.contains(i) && !setMember.contains(i))
                    {
                    Member member = setMaster.getMember(i);
                    if (member == null)
                        {
                        // COH-4825: need to cross a synchronization barrier before
                        //           checking the recycle-set (see MasterMemberSet#remove)
                        synchronized (setMaster)
                            {
                            member = setMaster.getRecycleSet().getMember(i);
                            }
                        _assert(member != null);
                        }
        
                    // update the poll (on the service thread)
                    service.doPollMemberLeft(this, member);
                    }
                }
            }
        
        Span span = getTracingSpan();
        span.setMetadata("internal.message", msg.getMessageType() < 0);
        // tracing span propogation
        msg.setTracingSpanContext(span.getContext());
        }
    
    /**
     * Preprocess the response to this Poll.
    * 
    * @return true iff the response message has been fully processed (onMessage
    * was called)
     */
    public boolean preprocessResponse(Message msgResponse)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        if (isPreprocessable())
            {
            // In general, processing Poll responses on transport threads is considered safe
            // only if we can prove that any prior messages from the same source have been processed;
            // we do it by ensuring that queue is empty and the service is in onWait()
            // Note: the order of checks below is important!
        
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
            if (service.getQueue().isEmpty() && service.isInWait())
                {
                service.onMessage(msgResponse);
                return true;
                }
            }
        
        return false;
        }
    
    // Accessor for the property "CloseableOnDelivery"
    /**
     * Setter for property CloseableOnDelivery.<p>
    * True if the poll is closeable once delivery notification is received for
    * the corresponding RequestMessage.
    * 
    * To optimize the memory usage, the value is stored in the _StateAux
    * property.
    * 
    * @functional
     */
    protected void setCloseableOnDelivery(boolean fDelivery)
        {
        set_StateAux(fDelivery ? 1 : 0);
        }
    
    // Accessor for the property "ExpiryTimeMillis"
    /**
     * Setter for property ExpiryTimeMillis.<p>
    * The time (in milliseconds) that this Poll object will expire.  Expired
    * polls will be automatically closed by the corresponding service.
    * 
    * Note: the expiry must be configured prior to posting the associated
    * message in order to have any effect.
     */
    public void setExpiryTimeMillis(long ldtExpiry)
        {
        __m_ExpiryTimeMillis = ldtExpiry;
        }
    
    // Accessor for the property "InitTimeMillis"
    /**
     * Setter for property InitTimeMillis.<p>
    * The time (in milliseconds) that this Poll object was initialized.
     */
    protected void setInitTimeMillis(long cMillis)
        {
        __m_InitTimeMillis = cMillis;
        }
    
    // Accessor for the property "LeftMemberSet"
    /**
     * Setter for property LeftMemberSet.<p>
    * The Set of Members that left the cluster while the poll was open.
     */
    protected void setLeftMemberSet(MemberSet setMember)
        {
        __m_LeftMemberSet = setMember;
        }
    
    // Accessor for the property "Notifier"
    /**
     * Setter for property Notifier.<p>
    * Note the usage of the OffloadingMultiNotifier is relying on the
    * SingleWaiterCooperativeNotifier.flush() call in Grid.flush()
     */
    private void setNotifier(com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier notifier)
        {
        __m_Notifier = notifier;
        }
    
    // Accessor for the property "PollId"
    /**
     * Setter for property PollId.<p>
    * The Poll number assigned to this Poll by the Service.
     */
    public void setPollId(long lMsgId)
        {
        __m_PollId = lMsgId;
        }
    
    // Accessor for the property "RemainingMemberSet"
    /**
     * Setter for property RemainingMemberSet.<p>
    * The Set of Members that have not yet responded to the poll and that are
    * still running the Service.
     */
    protected void setRemainingMemberSet(MemberSet setMember)
        {
        __m_RemainingMemberSet = setMember;
        }
    
    // Accessor for the property "RespondedMemberSet"
    /**
     * Setter for property RespondedMemberSet.<p>
    * The Set of Members that have responded to the poll request.
     */
    protected void setRespondedMemberSet(MemberSet setMember)
        {
        __m_RespondedMemberSet = setMember;
        }
    
    // Accessor for the property "Result"
    /**
     * Setter for property Result.<p>
    * The result of the Poll. This property is used to collect the result of
    * the Poll and return it to the client thread that sent the original
    * RequestMessage.
     */
    public void setResult(Object oResult)
        {
        __m_Result = oResult;
        }
    
    // Accessor for the property "Service"
    /**
     * Setter for property Service.<p>
    * The Service that is managing the poll.
     */
    public void setService(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        // allow to be configured but not modified
        _assert(service != null && getService() == null);
        
        __m_Service = (service);
        }
    
    // Accessor for the property "State"
    /**
     * Setter for property State.<p>
    * The state of the poll, one of the STATE_ constants.
    * 
    * @volatile - for dirty read in waitCompletion
     */
    protected void setState(int nState)
        {
        __m_State = nState;
        }
    
    // Accessor for the property "TracingSpan"
    /**
     * Setter for property TracingSpan.<p>
    * The OpenTracing Span associated with this poll.
     */
    public void setTracingSpan(com.tangosol.internal.tracing.Span spanTracing)
        {
        __m_TracingSpan = spanTracing;
        }
    
    // Declared at the super level
    public String toString()
        {
        // import Component;
        // import Component.Net.MemberSet;
        // import Component.Net.Message;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import com.tangosol.util.Base;
        // import java.sql.Timestamp;
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("Poll")
          .append("\n  {")
          .append("\n  PollId=")
          .append(getPollId())
          .append(isClosed() ? ", closed" : ", active(" + getNotifier() + ")")
          .append("\n  InitTimeMillis=")
          .append(new Timestamp(getInitTimeMillis()))
          .append("\n  Service=");
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
        if (service == null)
            {
            sb.append("null");
            }
        else
            {
            sb.append(service.getServiceName())
              .append(" (")
              .append(service.getServiceId())
              .append(')');
            }
        
        Component parent = get_Parent();
        if (parent != null)
            {
            sb.append(parent instanceof Message ?
                      "\n  Message=" : "\n  Parent=")
              .append(parent.get_Name());
            }
        
        sb.append("\n  RespondedMemberSet=[")
          .append(getRespondedMemberSet().getIdList())
          .append(']')
          .append("\n  LeftMemberSet=[")
          .append(getLeftMemberSet().getIdList())
          .append(']')
          .append("\n  RemainingMemberSet=[")
          .append(getRemainingMemberSet().getIdList())
          .append(']');
        
        String sDesc = getDescription();
        if (sDesc != null && sDesc.length() > 0)
            {
            sb.append('\n')
              .append(Base.indentString(sDesc, "  "));
            }
        
        sb.append("\n  }");
        
        return sb.toString();
        }
    
    /**
     * Wait for the poll to close and return the result
    * 
    * @param  ldtStart             the start time
    * @param cMillisTimeout  the maximum time to wait, or 0 for infinite
    * 
    * @return the result
    * 
    * @throws RequestTimeoutException if the timeout expires before the poll is
    * closed
     */
    public Object waitCompletion(long ldtStart, long cMillisTimeout)
            throws java.lang.InterruptedException
        {
        // import com.oracle.coherence.common.base.Notifier;
        
        Notifier notifier = getNotifier();
        while (!isClosed())
            {
            notifier.await(checkRequestTimeout(ldtStart, cMillisTimeout));
            }
        
        return getResult();
        }
    
    /**
     * Wait for the service to resume.
    * 
    * @param  ldtStart             the start time
    * @param cMillisTimeout  the maximum time to wait, or 0 for infinite
    * 
    * @return the remaining timeout
    * 
    * @throws RequestTimeoutException if the timeout expires before the service
    * resumes
     */
    public long waitServiceResume(long ldtStart, long cMillisTimeout)
            throws java.lang.InterruptedException
        {
        // import java.util.concurrent.atomic.AtomicLong;
        // import com.oracle.coherence.common.base.Blocking;
        
        long       lId = getPollId();
        AtomicLong atl = getService().getSuspendPollLimit();
        synchronized (atl)
            {
            while (lId > atl.get())
                {
                Blocking.wait(atl, checkRequestTimeout(ldtStart, cMillisTimeout));
                }
            }
        
        return checkRequestTimeout(ldtStart, cMillisTimeout);
        }
    }
