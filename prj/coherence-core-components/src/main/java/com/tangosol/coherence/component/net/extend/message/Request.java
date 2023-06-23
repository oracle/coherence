
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.message.Request

package com.tangosol.coherence.component.net.extend.message;

import com.tangosol.coherence.component.net.extend.Channel;

import com.tangosol.net.RequestTimeoutException;

import com.tangosol.util.Base;
import com.tangosol.util.WrapperException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base definition of a Request component.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Request
        extends    com.tangosol.coherence.component.net.extend.Message
        implements com.tangosol.net.messaging.Request
    {
    // ---- Fields declarations ----
    
    /**
     * Property Id
     *
     * The unique identifier of this Request.
     * 
     * @see com.tangosol.net.messaging.Request#getId
     */
    private long __m_Id;
    
    /**
     * Property Response
     *
     * The Response.
     * 
     * @see com.tangosol.net.messaging.Request#getResponse
     */
    private transient com.tangosol.net.messaging.Response __m_Response;
    
    /**
     * Property Status
     *
     * The Status of this Request.
     * 
     * @see com.tangosol.net.messaging.Request#getStatus
     */
    private transient com.tangosol.net.messaging.Request.Status __m_Status;
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
        __mapChildren.put("Status", Request.Status.get_CLASS());
        }
    
    // Initializing constructor
    public Request(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/message/Request".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.messaging.Request
    public com.tangosol.net.messaging.Response ensureResponse()
        {
        // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.net.messaging.Response as com.tangosol.net.messaging.Response;
        
        com.tangosol.net.messaging.Response response = getResponse();
        if (response == null)
            {
            com.tangosol.net.messaging.Channel channel = getChannel();
            _assert(channel != null);
        
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            _assert(factory != null);
        
            setResponse(response = instantiateResponse(factory));
            }
        
        return response;
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        return super.getDescription() + ", Id=" + getId() + ", Status=" + getStatus();
        }
    
    // From interface: com.tangosol.net.messaging.Request
    // Accessor for the property "Id"
    /**
     * Getter for property Id.<p>
    * The unique identifier of this Request.
    * 
    * @see com.tangosol.net.messaging.Request#getId
     */
    public long getId()
        {
        return __m_Id;
        }
    
    // Accessor for the property "Response"
    /**
     * Getter for property Response.<p>
    * The Response.
    * 
    * @see com.tangosol.net.messaging.Request#getResponse
     */
    protected com.tangosol.net.messaging.Response getResponse()
        {
        return __m_Response;
        }
    
    // From interface: com.tangosol.net.messaging.Request
    // Accessor for the property "Status"
    /**
     * Getter for property Status.<p>
    * The Status of this Request.
    * 
    * @see com.tangosol.net.messaging.Request#getStatus
     */
    public com.tangosol.net.messaging.Request.Status getStatus()
        {
        return __m_Status;
        }
    
    /**
     * Factory method: create a new Response instance.
    * 
    * @param  the MessageFactory used to create the new Response object
    * 
    * @return a new Response object
     */
    protected Response instantiateResponse(com.tangosol.net.messaging.Protocol.MessageFactory factory)
        {
        return (Response) factory.createMessage(0);
        }
    
    // Accessor for the property "Incoming"
    /**
     * Getter for property Incoming.<p>
    * True iff this is an incoming Request sent by a peer.
     */
    public boolean isIncoming()
        {
        return getStatus() == null;
        }
    
    /**
     * Called when a RuntimException is caught while executing the Request.
    * 
    * @see #run
     */
    protected void onException(RuntimeException e)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        
        if (isIncoming())
            {
            if (_isTraceEnabled(5))
                {
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer manager = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) getChannel()
                        .getConnection()
                        .getConnectionManager();
        
                _trace("An exception occurred while processing a "
                        + get_Name() + " for Service="
                        + manager.getServiceName()
                        + ": " + getStackTrace(e), 5);
                }
            }
        }
    
    /**
     * Called when the Request is run.
    * 
    * @param response  the Response that should be populated with the result of
    * running the Request
     */
    protected void onRun(Response response)
        {
        }
    
    // Declared at the super level
    public void readExternal(com.tangosol.io.pof.PofReader in)
            throws java.io.IOException
        {
        super.readExternal(in);
        
        setId(in.readLong(0));
        }
    
    // From interface: com.tangosol.net.messaging.Request
    // Declared at the super level
    public final void run()
        {
        Response response = (Response) ensureResponse();
        try
            {
            onRun(response);
            }
        catch (RuntimeException e)
            {
            onException(e);
        
            response.setFailure(true);
            response.setResult(e);
            }
        }
    
    // From interface: com.tangosol.net.messaging.Request
    // Accessor for the property "Id"
    /**
     * Setter for property Id.<p>
    * The unique identifier of this Request.
    * 
    * @see com.tangosol.net.messaging.Request#getId
     */
    public void setId(long lId)
        {
        __m_Id = lId;
        }
    
    // Accessor for the property "Response"
    /**
     * Setter for property Response.<p>
    * The Response.
    * 
    * @see com.tangosol.net.messaging.Request#getResponse
     */
    protected void setResponse(com.tangosol.net.messaging.Response response)
        {
        _assert(getResponse() == null);
        
        __m_Response = (response);
        }
    
    // From interface: com.tangosol.net.messaging.Request
    // Accessor for the property "Status"
    /**
     * Setter for property Status.<p>
    * The Status of this Request.
    * 
    * @see com.tangosol.net.messaging.Request#getStatus
     */
    public void setStatus(com.tangosol.net.messaging.Request.Status status)
        {
        _assert(status != null);
        
        if (getStatus() == null)
            {
            __m_Status = (status);
            }
        else
            {
            throw new IllegalStateException();
            }
        }
    
    // Declared at the super level
    public void writeExternal(com.tangosol.io.pof.PofWriter out)
            throws java.io.IOException
        {
        super.writeExternal(out);
        
        out.writeLong(0, getId());
        }

    // ---- class: com.tangosol.coherence.component.net.extend.message.Request$Status
    
    /**
     * Implementation of the Request$Status interface.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Status
            extends    com.tangosol.coherence.component.net.Extend
            implements com.tangosol.net.messaging.Request.Status
        {
        // ---- Fields declarations ----
        
        /**
         * Property Channel
         *
         * The Channel associated with this Status.
         * 
         * @volatile
         */
        private volatile com.tangosol.coherence.component.net.extend.Channel __m_Channel;
        
        /**
         * Property Closed
         *
         * Flag that indicates whether or not the Request represented by this
         * Status has completed successfully, completed unsucessfully, or been
         * canceled.
         * 
         * @see com.tangosol.net.messaging.Request$Status#isClosed
         * @volatile
         */
        private volatile boolean __m_Closed;
        
        /**
         * Property DefaultTimeoutMillis
         *
         * The default request timeout in milliseconds.
         * 
         * @see #waitForResponse()
         */
        private long __m_DefaultTimeoutMillis;
        
        /**
         * Property Error
         *
         * The throwable associated with a failed or canceled request.
         */
        private Throwable __m_Error;
        
        /**
         * Property InitTimeMillis
         *
         * The time (in millseconds) that this Status object was initialized.
         * 
         * @volatile
         */
        private volatile long __m_InitTimeMillis;
        
        /**
         * Property Request
         *
         * The Request represented by this Status.
         * 
         * @see com.tangosol.net.management.Request$Status#getRequest
         * @volatile
         */
        private volatile com.tangosol.net.messaging.Request __m_Request;
        
        /**
         * Property Response
         *
         * The Response sent by the peer.  
         * 
         * @see com.tangosol.net.management.Request$Status#getResponse
         * @volatile
         */
        private volatile com.tangosol.net.messaging.Response __m_Response;

        /**
         * The lock that should be used to control concurrent access to this Status.
         */
        private final Lock __m_Lock = new ReentrantLock();

        /**
         * The condition that is used to signal when the request is completed.
         */
        private final Condition __m_Completed = __m_Lock.newCondition();

        // Default constructor
        public Status()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.message.Request.Status();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/message/Request$Status".replace('/', '.'));
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
        
        // From interface: com.tangosol.net.messaging.Request$Status
        public void cancel()
            {
            cancel(null);
            }
        
        // From interface: com.tangosol.net.messaging.Request$Status
        public void cancel(Throwable cause)
            {
            // import Component.Net.Extend.Channel;
            
            if (cause == null)
                {
                cause = new RuntimeException("Request was canceled");
                }
            
            Channel channel;
            lock();
            try
                {
                if (isClosed())
                    {
                    channel = null;
                    }
                else
                    {
                    channel = getChannel();
            
                    setError(cause);
                    onCompletion();
                    }
                }
            finally
                {
                unlock();
                }
            
            if (channel != null)
                {
                channel.onRequestCompleted(this);
                }
            }
        
        // Accessor for the property "Channel"
        /**
         * Getter for property Channel.<p>
        * The Channel associated with this Status.
        * 
        * @volatile
         */
        public com.tangosol.coherence.component.net.extend.Channel getChannel()
            {
            return __m_Channel;
            }
        
        // Accessor for the property "DefaultTimeoutMillis"
        /**
         * Getter for property DefaultTimeoutMillis.<p>
        * The default request timeout in milliseconds.
        * 
        * @see #waitForResponse()
         */
        public long getDefaultTimeoutMillis()
            {
            return __m_DefaultTimeoutMillis;
            }
        
        // Declared at the super level
        /**
         * Return a human-readable description of this component.
        * 
        * @return a String representation of this component
         */
        protected String getDescription()
            {
            StringBuffer sb = new StringBuffer();
            
            boolean fClosed = isClosed();
            sb.append("InitTimeMillis=")
              .append(getInitTimeMillis())
              .append(", Closed=")
              .append(fClosed);
            
            if (fClosed)
                {
                Throwable t = getError();
                if (t == null)
                    {
                    sb.append(", Response=")
                      .append(getResponse());
                    }
                else
                    {
                    sb.append(", Error=")
                      .append(t);
                    }
                }
            
            return sb.toString();
            }
        
        // Accessor for the property "Error"
        /**
         * Getter for property Error.<p>
        * The throwable associated with a failed or canceled request.
         */
        public Throwable getError()
            {
            return __m_Error;
            }
        
        // Accessor for the property "InitTimeMillis"
        /**
         * Getter for property InitTimeMillis.<p>
        * The time (in millseconds) that this Status object was initialized.
        * 
        * @volatile
         */
        public long getInitTimeMillis()
            {
            return __m_InitTimeMillis;
            }
        
        // From interface: com.tangosol.net.messaging.Request$Status
        // Accessor for the property "Request"
        /**
         * Getter for property Request.<p>
        * The Request represented by this Status.
        * 
        * @see com.tangosol.net.management.Request$Status#getRequest
        * @volatile
         */
        public com.tangosol.net.messaging.Request getRequest()
            {
            return __m_Request;
            }
        
        // From interface: com.tangosol.net.messaging.Request$Status
        // Accessor for the property "Response"
        /**
         * Getter for property Response.<p>
        * The Response sent by the peer.  
        * 
        * @see com.tangosol.net.management.Request$Status#getResponse
        * @volatile
         */
        public com.tangosol.net.messaging.Response getResponse()
            {
            Throwable t = getError();
            if (t == null)
                {
                return __m_Response;
                }
            throw ensureRuntimeException(t);
            }
        
        // From interface: com.tangosol.net.messaging.Request$Status
        // Accessor for the property "Closed"
        /**
         * Getter for property Closed.<p>
        * Flag that indicates whether or not the Request represented by this
        * Status has completed successfully, completed unsucessfully, or been
        * canceled.
        * 
        * @see com.tangosol.net.messaging.Request$Status#isClosed
        * @volatile
         */
        public boolean isClosed()
            {
            return __m_Closed;
            }
        
        /**
         * Called after the Request represented by this Status has completed
        * (successfully or unsuccessfully) or been canceled.
         */
        protected void onCompletion()
            {
            setClosed(true);
            __m_Completed.signalAll();
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
            // super.onInit(): no children
            setInitTimeMillis(System.currentTimeMillis());
            }
        
        // Accessor for the property "Channel"
        /**
         * Setter for property Channel.<p>
        * The Channel associated with this Status.
        * 
        * @volatile
         */
        public void setChannel(com.tangosol.coherence.component.net.extend.Channel channel)
            {
            _assert(!isClosed() && channel != null && getChannel() == null);
            
            __m_Channel = (channel);
            }
        
        // Accessor for the property "Closed"
        /**
         * Setter for property Closed.<p>
        * Flag that indicates whether or not the Request represented by this
        * Status has completed successfully, completed unsucessfully, or been
        * canceled.
        * 
        * @see com.tangosol.net.messaging.Request$Status#isClosed
        * @volatile
         */
        protected void setClosed(boolean fClosed)
            {
            __m_Closed = fClosed;
            }
        
        // Accessor for the property "DefaultTimeoutMillis"
        /**
         * Setter for property DefaultTimeoutMillis.<p>
        * The default request timeout in milliseconds.
        * 
        * @see #waitForResponse()
         */
        public void setDefaultTimeoutMillis(long cMillis)
            {
            __m_DefaultTimeoutMillis = cMillis;
            }
        
        // Accessor for the property "Error"
        /**
         * Setter for property Error.<p>
        * The throwable associated with a failed or canceled request.
         */
        protected void setError(Throwable cause)
            {
            __m_Error = cause;
            }
        
        // Accessor for the property "InitTimeMillis"
        /**
         * Setter for property InitTimeMillis.<p>
        * The time (in millseconds) that this Status object was initialized.
        * 
        * @volatile
         */
        protected void setInitTimeMillis(long cMillis)
            {
            __m_InitTimeMillis = cMillis;
            }
        
        // Accessor for the property "Request"
        /**
         * Setter for property Request.<p>
        * The Request represented by this Status.
        * 
        * @see com.tangosol.net.management.Request$Status#getRequest
        * @volatile
         */
        public void setRequest(com.tangosol.net.messaging.Request request)
            {
            _assert(!isClosed() && request != null && getRequest() == null);
            
            __m_Request = (request);
            }
        
        // Accessor for the property "Response"
        /**
         * Setter for property Response.<p>
        * The Response sent by the peer.  
        * 
        * @see com.tangosol.net.management.Request$Status#getResponse
        * @volatile
         */
        public void setResponse(com.tangosol.net.messaging.Response response)
            {
            // import Component.Net.Extend.Channel;
            
            _assert(response != null);
            
            Channel channel;
            lock();
            try
                {
                if (isClosed())
                    {
                    channel = null;
                    }
                else
                    {
                    channel = getChannel();
            
                    _assert(getResponse() == null);
            
                    __m_Response = (response);
                    onCompletion();
                    }
                }
            finally
                {
                unlock();
                }
            
            if (channel != null)
                {
                channel.onRequestCompleted(this);
                }
            }
        
        // From interface: com.tangosol.net.messaging.Request$Status
        public com.tangosol.net.messaging.Response waitForResponse()
            {
            return waitForResponse(-1L);
            }
        
        // From interface: com.tangosol.net.messaging.Request$Status
        public com.tangosol.net.messaging.Response waitForResponse(long cMillis)
            {
            // import com.oracle.coherence.common.base.Blocking;
            // import com.tangosol.net.RequestTimeoutException;
            // import com.tangosol.util.Base;
            // import com.tangosol.util.WrapperException;
            
            if (cMillis == -1L)
                {
                cMillis = getDefaultTimeoutMillis();
                }
            
            try
                {
                if (cMillis <= 0L)
                    {
                    getChannel().getConnectionManager().drainOverflow(0L);

                    lock();
                    try
                        {
                        while (!isClosed())
                            {
                            __m_Completed.await();
                            }
                        }
                    finally
                        {
                        unlock();
                        }
                    }
                else
                    {
                    long cRemain = getChannel().getConnectionManager().drainOverflow(cMillis);

                    lock();
                    try
                        {
                        long ldtStart = -1L;
            
                        while (!isClosed())
                            {
                            if (ldtStart < 0L)
                                {
                                ldtStart = Base.getSafeTimeMillis();
                                }

                            __m_Completed.await(cRemain, TimeUnit.MILLISECONDS);
            
                            if (isClosed())
                                {
                                break;
                                }
                            else if ((cRemain -= (Base.getSafeTimeMillis() - ldtStart)) <= 0L)
                                {
                                throw new RequestTimeoutException("Request timed out");
                                }
                            }
                        }
                    finally
                        {
                        unlock();
                        }
                    }
                }
            catch (Exception e)
                {
                // COH-6105 - Process exceptions outside of the synchronized blocks
                cancel(e);
                if (e instanceof InterruptedException)
                    {
                    Thread.currentThread().interrupt();
                    throw new WrapperException(e, "Request interrupted");
                    }
                }
            
            return getResponse();
            }

        /**
         * Lock this Status.
         */
        public void lock()
            {
            __m_Lock.lock();
            }

        /**
         * Unlock this Status.
         */
        public void unlock()
            {
            __m_Lock.unlock();
            }
        }
    }
