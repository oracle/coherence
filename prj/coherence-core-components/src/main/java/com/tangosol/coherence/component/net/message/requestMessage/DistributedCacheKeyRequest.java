
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheKeyRequest

package com.tangosol.coherence.component.net.message.requestMessage;

import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.HashHelper;
import com.tangosol.util.ThreadGate;

/**
 * DistributedCacheKeyRequest is a base component for resource related request
 * messages used by DistributedCache service.
 * 
 * Attributes:
 *     CacheId
 *     Key
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class DistributedCacheKeyRequest
        extends    com.tangosol.coherence.component.net.message.RequestMessage
        implements com.tangosol.net.PriorityTask,
                   com.tangosol.net.cache.KeyAssociation,
                   Runnable
    {
    // ---- Fields declarations ----
    
    /**
     * Property CacheId
     *
     * The Id of the cache this request is for.
     */
    private long __m_CacheId;
    
    /**
     * Property ExecutionTimeoutMillis
     *
     * From PriorityTask interface.
     */
    private long __m_ExecutionTimeoutMillis;
    
    /**
     * Property Key
     *
     * The resource key this request is for.
     */
    private com.tangosol.util.Binary __m_Key;
    
    /**
     * Property ProcessedPartition
     *
     * The partition-id processed by this request, or -1.
     * 
     * This transient property is optional, and is filled in only after the
     * request is processed.
     */
    private transient int __m_ProcessedPartition;
    
    /**
     * Property RequestTimeoutMillis
     *
     * From PriorityTask interface.
     */
    private long __m_RequestTimeoutMillis;
    
    /**
     * Property RESPONSE_UNKNOWN
     *
     * The constant indicating that there was no definite response to a
     * request. This could be a result of either death of the Member the
     * request was sent to or an indication that the response said "I don't
     * know; try again".
     */
    public static final Object RESPONSE_UNKNOWN;
    
    /**
     * Property SchedulingPriority
     *
     * From PriorityTask interface.
     */
    private int __m_SchedulingPriority;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        try
            {
            RESPONSE_UNKNOWN = new java.lang.Object();
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
        __mapChildren.put("Poll", DistributedCacheKeyRequest.Poll.get_CLASS());
        }
    
    // Default constructor
    public DistributedCacheKeyRequest()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public DistributedCacheKeyRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
    
    // Getter for virtual constant ReadOnly
    public boolean isReadOnly()
        {
        return true;
        }
    
    // Getter for virtual constant Suspendable
    public boolean isSuspendable()
        {
        return true;
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheKeyRequest();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/DistributedCacheKeyRequest".replace('/', '.'));
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
     * Check whether or not the response value for this request is valid, or
    * rethrow the exception if the request failed with one.
     */
    public boolean checkResponse(Object oResponse)
        {
        if (oResponse == RESPONSE_UNKNOWN)
            {
            return false;
            }
        else
            {
            if (oResponse instanceof RuntimeException)
                {
                throw (RuntimeException) oResponse;
                }
            return true;
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
        DistributedCacheKeyRequest msg = (DistributedCacheKeyRequest) super.cloneMessage();
        
        msg.setCacheId(getCacheId());
        msg.setKey(getKey());
        msg.setExecutionTimeoutMillis(getExecutionTimeoutMillis());
        msg.setRequestTimeoutMillis(getRequestTimeoutMillis());
        msg.setSchedulingPriority(getSchedulingPriority());
        
        return msg;
        }
    
    /**
     * Copy the PriorityTask attributes from the specified task.
     */
    public void copyPriorityAttributes(com.tangosol.net.PriorityTask task)
        {
        if (task != null)
            {
            setExecutionTimeoutMillis(task.getExecutionTimeoutMillis());
            setRequestTimeoutMillis(task.getRequestTimeoutMillis());
            setSchedulingPriority(task.getSchedulingPriority());
            }
        }
    
    // Declared at the super level
    public boolean equals(Object obj)
        {
        // import com.tangosol.util.Base;
        
        // this implementation is used to associate non-commutative requests;
        // it is intentionally agnostic about the request specifics
        // (see comments to getAssociatedKey() implementation)
        
        if (obj instanceof DistributedCacheKeyRequest)
            {
            DistributedCacheKeyRequest that = (DistributedCacheKeyRequest) obj;
        
            return this.getService() == that.getService()
                && this.getCacheId() == that.getCacheId()
                && Base.equals(this.getKey(), that.getKey());
            }
        return false;
        }
    
    // From interface: com.tangosol.net.cache.KeyAssociation
    public Object getAssociatedKey()
        {
        // Prior to Coherence 3.4, this method returned the Key property and,
        // as a result, requests with the same key but for different caches were
        // processed in the serial order, which was absolutely unnecessary (COH-1139).
        // Ideally, we would want to return a [CacheId, Key] touple, but that would
        // mean creating additional amount of garbage in a relatively tight loop.
        // Alternative (and somewhat unconventional) solution is to return the Request
        // object itself, which would mean to implement the hashCode() and equals() in
        // such a way that any two request messages against the same cache and same key
        // would be considered "equal". It requires that subclasses DO NOT override
        // the equals() and hashCode() implementations.
        
        return this;
        }
    
    // Accessor for the property "CacheId"
    /**
     * Getter for property CacheId.<p>
    * The Id of the cache this request is for.
     */
    public long getCacheId()
        {
        return __m_CacheId;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Used for debugging purposes (from toString). Create a human-readable
    * description of the specific Message data.
     */
    public String getDescription()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache;
        
        String sCacheName = ((PartitionedCache) getService()).getCacheName(getCacheId());
        if (sCacheName == null)
            {
            sCacheName = "<unknown>";
            }
        
        return "CacheName=" + sCacheName;
        }
    
    // From interface: com.tangosol.net.PriorityTask
    // Accessor for the property "ExecutionTimeoutMillis"
    /**
     * Getter for property ExecutionTimeoutMillis.<p>
    * From PriorityTask interface.
     */
    public long getExecutionTimeoutMillis()
        {
        return __m_ExecutionTimeoutMillis;
        }
    
    // Accessor for the property "Key"
    /**
     * Getter for property Key.<p>
    * The resource key this request is for.
     */
    public com.tangosol.util.Binary getKey()
        {
        return __m_Key;
        }
    
    // Accessor for the property "ProcessedPartition"
    /**
     * Getter for property ProcessedPartition.<p>
    * The partition-id processed by this request, or -1.
    * 
    * This transient property is optional, and is filled in only after the
    * request is processed.
     */
    public int getProcessedPartition()
        {
        return __m_ProcessedPartition;
        }
    
    // From interface: com.tangosol.net.PriorityTask
    // Accessor for the property "RequestTimeoutMillis"
    /**
     * Getter for property RequestTimeoutMillis.<p>
    * From PriorityTask interface.
     */
    public long getRequestTimeoutMillis()
        {
        return __m_RequestTimeoutMillis;
        }
    
    // From interface: com.tangosol.net.PriorityTask
    // Accessor for the property "SchedulingPriority"
    /**
     * Getter for property SchedulingPriority.<p>
    * From PriorityTask interface.
     */
    public int getSchedulingPriority()
        {
        return __m_SchedulingPriority;
        }
    
    // Declared at the super level
    public int hashCode()
        {
        // import com.tangosol.util.HashHelper;
        
        // this implementation is used to associate non-commutative requests;
        // it is intentionally agnostic about the request specifics
        // (see comments to getAssociatedKey() implementation)
        
        Object oKey  = getKey();
        int    iHash = oKey == null ? 0 : oKey.hashCode();
        
        return HashHelper.hash(getCacheId(), iHash);
        }
    
    // Declared at the super level
    protected com.tangosol.coherence.component.net.Poll instantiatePoll()
        {
        return (DistributedCacheKeyRequest.Poll) _newChild("Poll");
        }
    
    // Accessor for the property "CoherentResult"
    /**
     * Getter for property CoherentResult.<p>
    * Return true if this message requires a coherence result thus must be
    * served by the primary.
     */
    public boolean isCoherentResult()
        {
        return true;
        }
    
    // Declared at the super level
    /**
     * This is the event that is executed when a Message is received.
    * <p>
    * It is the main processing event of the Message called by the
    * <code>Service.onMessage()</code> event. With regards to the use of
    * Message components within clustered Services, Services are designed by
    * dragging Message components into them as static children. These Messages
    * are the components that a Service can send to other running instances of
    * the same Service within a cluster. When the onReceived event is invoked
    * by a Service, it means that the Message has been received; the code in
    * the onReceived event is therefore the Message specific logic for
    * processing a received Message. For example, when onReceived is invoked on
    * a Message named FindData, the onReceived event should do the work to
    * "find the data", because it is being invoked by the Service that received
    * the "find the data" Message.
     */
    public void onReceived()
        {
        super.onReceived();
        getService().getDaemonPool().add(this);
        }
    
    // Declared at the super level
    /**
     * Preprocess this message.
    * 
    * @return true iff this message has been fully processed (onReceived was
    * called)
     */
    public boolean preprocess()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService;
        // import com.tangosol.util.ThreadGate;
        
        if (isDeserializationRequired())
            {
            return false;
            }
        
        // Note: we check if the service is concurrent rather then
        // checking that the thread pool is running. The result is that
        // when the thread-pool is running this work is dispatched there, but
        // when it is not enabled, but the service is concurrent
        // we can execute the work directly on the IO thread. Basically
        // configuring a negative thread count enables IO thread request processing.
        PartitionedService service = (PartitionedService) getService();
        if (service.isConcurrent())
            {
            ThreadGate gate = service.getPreprocessingGate();
            if (gate.enter(0))
                {
                try
                    {
                    service.onMessage(this);
                    return true;
                    }
                finally
                    {
                    gate.exit();
                    }
                }
            }
        
        return false;
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper;
        
        super.read(input);
        
        setCacheId(ExternalizableHelper.readLong(input));
        setKey((Binary) readObject(input));
        setExecutionTimeoutMillis(ExternalizableHelper.readLong(input));
        setRequestTimeoutMillis(ExternalizableHelper.readLong(input));
        setSchedulingPriority(ExternalizableHelper.readInt(input));
        }
    
    // From interface: java.lang.Runnable
    public void run()
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.PriorityTask
    public void runCanceled(boolean fAbandoned)
        {
        }
    
    // Accessor for the property "CacheId"
    /**
     * Setter for property CacheId.<p>
    * The Id of the cache this request is for.
     */
    public void setCacheId(long lCacheId)
        {
        __m_CacheId = lCacheId;
        }
    
    // Accessor for the property "ExecutionTimeoutMillis"
    /**
     * Setter for property ExecutionTimeoutMillis.<p>
    * From PriorityTask interface.
     */
    public void setExecutionTimeoutMillis(long cMillis)
        {
        __m_ExecutionTimeoutMillis = cMillis;
        }
    
    // Accessor for the property "Key"
    /**
     * Setter for property Key.<p>
    * The resource key this request is for.
     */
    public void setKey(com.tangosol.util.Binary binKey)
        {
        __m_Key = binKey;
        }
    
    // Accessor for the property "ProcessedPartition"
    /**
     * Setter for property ProcessedPartition.<p>
    * The partition-id processed by this request, or -1.
    * 
    * This transient property is optional, and is filled in only after the
    * request is processed.
     */
    public void setProcessedPartition(int nPartition)
        {
        __m_ProcessedPartition = nPartition;
        }
    
    // Accessor for the property "RequestTimeoutMillis"
    /**
     * Setter for property RequestTimeoutMillis.<p>
    * From PriorityTask interface.
     */
    public void setRequestTimeoutMillis(long cMillis)
        {
        __m_RequestTimeoutMillis = cMillis;
        }
    
    // Accessor for the property "SchedulingPriority"
    /**
     * Setter for property SchedulingPriority.<p>
    * From PriorityTask interface.
     */
    public void setSchedulingPriority(int nPriority)
        {
        __m_SchedulingPriority = nPriority;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        super.write(output);
        
        ExternalizableHelper.writeLong(output, getCacheId());
        writeObject(output, getKey());
        ExternalizableHelper.writeLong(output, getExecutionTimeoutMillis());
        ExternalizableHelper.writeLong(output, getRequestTimeoutMillis());
        ExternalizableHelper.writeInt(output, getSchedulingPriority());
        }

    // ---- class: com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheKeyRequest$Poll
    
    /**
     * The Poll contains information regarding a request sent to one or more
     * Cluster Members that require responses. A Service may poll other Members
     * that are running the same Service, and the Poll is used to wait for and
     * assemble the responses from each of those Members. A client thread may
     * also use the Poll to block on a response or set of responses, thus
     * waiting for the completion of the Poll. In its simplest form, which is a
     * Poll that is sent to one Member of the Cluster, the Poll actually
     * represents the request/response model.
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
            return new com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheKeyRequest.Poll();
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
                clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/DistributedCacheKeyRequest$Poll".replace('/', '.'));
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
         * This is the event that is executed when all the Members that were
        * polled have responded or have left the Service.
         */
        protected void onCompletion()
            {
            // import Component.Util.Daemon.QueueProcessor.Service;
            
            if (getService().getServiceState() == Service.SERVICE_STOPPED ||
                getRespondedMemberSet().isEmpty())
                {
                // since the DistributedCacheKeyRequest is sent to one and only one member,
                // an empty RespondedMemberSet means that either the responder is dead
                // or that the poll has timed out
                setResult(DistributedCacheKeyRequest.RESPONSE_UNKNOWN);
                }
            
            super.onCompletion();
            }
        
        // Declared at the super level
        /**
         * This event occurs for each response Message from each polled Member.
         */
        public void onResponse(com.tangosol.coherence.component.net.Message msg)
            {
            // import Component.Net.Message.ResponseMessage.SimpleResponse as com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse;
            // import com.tangosol.util.Base;
            
            com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse msgResponse = (com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse) msg;
            switch (msgResponse.getResult())
                {
                case com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_SUCCESS:
                    setResult(msgResponse.getValue());
                    break;
            
                case com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_RETRY:
                    setResult(DistributedCacheKeyRequest.RESPONSE_UNKNOWN);
                    break;
            
                case com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse.RESULT_FAILURE:
                    setResult(msgResponse.getFailure());
                    break;
            
                default:
                    throw new IllegalStateException();
                }
            
            super.onResponse(msg);
            }
        }
    }
