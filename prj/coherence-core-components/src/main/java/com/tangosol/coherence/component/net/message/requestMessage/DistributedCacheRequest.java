
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest

package com.tangosol.coherence.component.net.message.requestMessage;

import com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse;
import com.tangosol.coherence.component.net.requestContext.AsyncContext;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.net.internal.PartitionVersions;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ThreadGate;
import java.util.List;

/**
 * DistributeCacheRequest is a base component for RequestMessage(s) used by the
 * partitioned cache service that are key set or filter based. Quite often a
 * collection of similar requests are sent in parallel and a client thread has
 * to wait for all of them to return.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class DistributedCacheRequest
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
     * Property OrderId
     *
     * Unit-of-order id for asynchronous agents. This value is zero for
     * synchronous requests.
     * 
     * @see com.tangosol.util.processor.AsynchronousProcessor,
     * com.tangosol.util.aggregator.AsynchronousAggregator
     */
    private long __m_OrderId;
    
    /**
     * Property OwnershipVersions
     *
     * The ownership versions of the partitions associated with this Request
     * (from the client's point of view).
     * 
     * Used for AsyncOperations only.
     */
    private com.tangosol.net.internal.PartitionVersions __m_OwnershipVersions;
    
    /**
     * Property PartResults
     *
     * Transient list of partial results returned by [storage-enabled] service
     * members that processed this request.  Most commonly the elements of this
     * list are DistributedPartialResponse messages.
     */
    private transient java.util.List __m_PartResults;
    
    /**
     * Property ProcessedPartitions
     *
     * The set of partitions processed by this request.
     * 
     * This transient property is optional, and is filled in only after the
     * request is processed.
     */
    private com.tangosol.net.partition.PartitionSet __m_ProcessedPartitions;
    
    /**
     * Property ReadException
     *
     * (Transient) An Exception that occurred during the read() and had to be
     * deferred to be processed during onReceived() ans possibly reported back
     * to the client (requestor). Usually it is an IOException, but for
     * technical reasons could also be a ClassCastException.
     * 
     * See COH-2150 for details.
     */
    private transient Exception __m_ReadException;
    
    /**
     * Property RequestTimeoutMillis
     *
     * From PriorityTask interface.
     */
    private long __m_RequestTimeoutMillis;
    
    /**
     * Property SchedulingPriority
     *
     * From PriorityTask interface.
     */
    private int __m_SchedulingPriority;
    private static com.tangosol.util.ListMap __mapChildren;

    /**
     * Property RepliesMask
     *
     * Keep track of replies against request mask while keeping the latter safe.
     *
     * This property is not serialized, it is only used locally.
     */
    private transient PartitionSet __m_RepliesMask;

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
        __mapChildren.put("Poll", DistributedCacheRequest.Poll.get_CLASS());
        }
    
    // Default constructor
    public DistributedCacheRequest()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public DistributedCacheRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/DistributedCacheRequest".replace('/', '.'));
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
    
    // Declared at the super level
    /**
     * Instantiate a copy of this message. This is quite different from the
    * standard "clone" since only the "transmittable" portion of the message
    * (and none of the internal) state should be cloned.
     */
    public com.tangosol.coherence.component.net.Message cloneMessage()
        {
        DistributedCacheRequest msg = (DistributedCacheRequest) super.cloneMessage();
        
        msg.setCacheId(getCacheId());
        msg.setExecutionTimeoutMillis(getExecutionTimeoutMillis());
        msg.setRequestTimeoutMillis(getRequestTimeoutMillis());
        msg.setSchedulingPriority(getSchedulingPriority());
        msg.setOrderId(getOrderId());
        
        if (isAsyncOperation())
            {
            msg.setOwnershipVersions(getOwnershipVersions());
            }
        
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
    
    // From interface: com.tangosol.net.cache.KeyAssociation
    public Object getAssociatedKey()
        {
        long lOrderId = getOrderId();
        
        return lOrderId == 0L ? null : Long.valueOf(lOrderId);
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
            8 + // long - CacheId
            8 + // long - ExecutionTimeoutMillis
            8 + // long - RequestTimeoutMillis
            4 + // int  - SchedulingPriority
            8;  // long - OrderId
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
    
    // Accessor for the property "OrderId"
    /**
     * Getter for property OrderId.<p>
    * Unit-of-order id for asynchronous agents. This value is zero for
    * synchronous requests.
    * 
    * @see com.tangosol.util.processor.AsynchronousProcessor,
    * com.tangosol.util.aggregator.AsynchronousAggregator
     */
    public long getOrderId()
        {
        return __m_OrderId;
        }
    
    // Accessor for the property "OwnershipVersions"
    /**
     * Getter for property OwnershipVersions.<p>
    * The ownership versions of the partitions associated with this Request
    * (from the client's point of view).
    * 
    * Used for AsyncOperations only.
     */
    public com.tangosol.net.internal.PartitionVersions getOwnershipVersions()
        {
        return __m_OwnershipVersions;
        }
    
    // Accessor for the property "PartResults"
    /**
     * Getter for property PartResults.<p>
    * Transient list of partial results returned by [storage-enabled] service
    * members that processed this request.  Most commonly the elements of this
    * list are DistributedPartialResponse messages.
     */
    public java.util.List getPartResults()
        {
        return __m_PartResults;
        }
    
    // Accessor for the property "ProcessedPartitions"
    /**
     * Getter for property ProcessedPartitions.<p>
    * The set of partitions processed by this request.
    * 
    * This transient property is optional, and is filled in only after the
    * request is processed.
     */
    public com.tangosol.net.partition.PartitionSet getProcessedPartitions()
        {
        return __m_ProcessedPartitions;
        }
    
    // Accessor for the property "ReadException"
    /**
     * Getter for property ReadException.<p>
    * (Transient) An Exception that occurred during the read() and had to be
    * deferred to be processed during onReceived() ans possibly reported back
    * to the client (requestor). Usually it is an IOException, but for
    * technical reasons could also be a ClassCastException.
    * 
    * See COH-2150 for details.
     */
    public Exception getReadException()
        {
        return __m_ReadException;
        }
    
    // Accessor for the property "RequestPartitions"
    /**
     * Getter for property RequestPartitions.<p>
    * (Calculated) Set of partitions that need to be processed for this
    * request. This value is never null for asynchronous requests.
     */
    public com.tangosol.net.partition.PartitionSet getRequestPartitions()
        {
        // this method needs to be overridden to be used
        
        throw new UnsupportedOperationException();
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

    /**
     * Getter for RepliesMask
     *
     * Keep track of replies against request mask while keeping the latter safe.
     *
     * @return the replies mask PartitionSet
     */
    public PartitionSet getRepliesMask()
        {
        return __m_RepliesMask;
        }

    // Declared at the super level
    protected com.tangosol.coherence.component.net.Poll instantiatePoll()
        {
        return (DistributedCacheRequest.Poll) _newChild("Poll");
        }
    
    // Accessor for the property "AsyncOperation"
    /**
     * Getter for property AsyncOperation.<p>
    * Calculated property indicating whether or not this message represents an
    * asynchronous operation.
     */
    public boolean isAsyncOperation()
        {
        return getOrderId() != 0;
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
     * Asynchronously send this message.  The actual transmission of the message
    * may be deferred due to the send queue batching.
    * This method should not be called directly; see Grid#post(Message).
     */
    public void post()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
        
        if (isAsyncOperation())
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache service = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache) getService();
        
            // stamp async requests with the client's view of the ownership versions
            setOwnershipVersions(service.collectOwnershipVersions(getRequestPartitions()));
            }
        
        super.post();
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
        // import com.tangosol.net.internal.PartitionVersions;
        // import com.tangosol.util.ExternalizableHelper;
        
        super.read(input);
        
        setCacheId(ExternalizableHelper.readLong(input));
        setExecutionTimeoutMillis(ExternalizableHelper.readLong(input));
        setRequestTimeoutMillis(ExternalizableHelper.readLong(input));
        setSchedulingPriority(ExternalizableHelper.readInt(input));
        setOrderId(ExternalizableHelper.readLong(input));
        
        if (isAsyncOperation())
            {
            PartitionVersions versions = new PartitionVersions();
            versions.readExternal(input);
            setOwnershipVersions(versions);
            }
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
    
    // Accessor for the property "OrderId"
    /**
     * Setter for property OrderId.<p>
    * Unit-of-order id for asynchronous agents. This value is zero for
    * synchronous requests.
    * 
    * @see com.tangosol.util.processor.AsynchronousProcessor,
    * com.tangosol.util.aggregator.AsynchronousAggregator
     */
    public void setOrderId(long lId)
        {
        __m_OrderId = lId;
        }
    
    // Accessor for the property "OwnershipVersions"
    /**
     * Setter for property OwnershipVersions.<p>
    * The ownership versions of the partitions associated with this Request
    * (from the client's point of view).
    * 
    * Used for AsyncOperations only.
     */
    public void setOwnershipVersions(com.tangosol.net.internal.PartitionVersions versionsOwnership)
        {
        __m_OwnershipVersions = versionsOwnership;
        }
    
    // Accessor for the property "PartResults"
    /**
     * Setter for property PartResults.<p>
    * Transient list of partial results returned by [storage-enabled] service
    * members that processed this request.  Most commonly the elements of this
    * list are DistributedPartialResponse messages.
     */
    public void setPartResults(java.util.List listResults)
        {
        __m_PartResults = listResults;
        }
    
    // Accessor for the property "ProcessedPartitions"
    /**
     * Setter for property ProcessedPartitions.<p>
    * The set of partitions processed by this request.
    * 
    * This transient property is optional, and is filled in only after the
    * request is processed.
     */
    public void setProcessedPartitions(com.tangosol.net.partition.PartitionSet parts)
        {
        __m_ProcessedPartitions = parts;
        }
    
    // Accessor for the property "ReadException"
    /**
     * Setter for property ReadException.<p>
    * (Transient) An Exception that occurred during the read() and had to be
    * deferred to be processed during onReceived() ans possibly reported back
    * to the client (requestor). Usually it is an IOException, but for
    * technical reasons could also be a ClassCastException.
    * 
    * See COH-2150 for details.
     */
    public void setReadException(Exception exception)
        {
        __m_ReadException = exception;
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

    /**
     * Setter for RepliesMask
     *
     * Keep track of replies against request mask while keeping the latter safe.
     *
     * @param set  PartitionSet initially set to request mask
     */
    public void setRepliesMask(PartitionSet set)
        {
        __m_RepliesMask = set;
        }

    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        super.write(output);
        
        ExternalizableHelper.writeLong(output, getCacheId());
        ExternalizableHelper.writeLong(output, getExecutionTimeoutMillis());
        ExternalizableHelper.writeLong(output, getRequestTimeoutMillis());
        ExternalizableHelper.writeInt(output, getSchedulingPriority());
        ExternalizableHelper.writeLong(output, getOrderId());
        
        if (isAsyncOperation())
            {
            getOwnershipVersions().writeExternal(output);
            }
        }

    // ---- class: com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest$Poll
    
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
        
        /**
         * Property RequestRejected
         *
         * False if the response carries any partial result; true if the
         * request has been fully rejected.
         * 
         * Note: this property is used only by the onResponse() method and any
         * changes to its default value by sub-components should be done
         * *before* super.onResponse() call is made.
         */
        private boolean __m_RequestRejected;
        
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
            return new com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest.Poll();
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
                clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/DistributedCacheRequest$Poll".replace('/', '.'));
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
        
        // Accessor for the property "RequestRejected"
        /**
         * Getter for property RequestRejected.<p>
        * False if the response carries any partial result; true if the request
        * has been fully rejected.
        * 
        * Note: this property is used only by the onResponse() method and any
        * changes to its default value by sub-components should be done
        * *before* super.onResponse() call is made.
         */
        protected boolean isRequestRejected()
            {
            return __m_RequestRejected;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when all the Members that were
        * polled have responded or have left the Service.
         */
        protected void onCompletion()
            {
            // import Component.Net.Message.RequestMessage.DistributedCacheRequest as DistributedCacheRequest;
            // import Component.Net.Message.ResponseMessage.DistributedPartialResponse as com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse;
            // import Component.Net.RequestContext.AsyncContext;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$RequestCoordinator as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.RequestCoordinator;
            // import com.tangosol.net.partition.PartitionSet;
            
            super.onCompletion();
            
            DistributedCacheRequest msgRequest = (DistributedCacheRequest) get_Module();
            
            if (msgRequest.isAsyncOperation())
                {
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache      service     = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache) getService();
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.RequestCoordinator  coordinator = service.getRequestCoordinator();
                AsyncContext context     = (AsyncContext) msgRequest.getRequestContext();
                PartitionSet partRequest = msgRequest.getRequestPartitions();
                PartitionSet partRemain  = context.getPartitionSet();
            
                com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse msgResponse = (com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse) getResult();
                if (msgResponse == null)
                    {
                    if (getRespondedMemberSet().isEmpty() && getLeftMemberSet().isEmpty())
                        {
                        // exception during message post()
                        partRemain.remove(partRequest);
                        }
                    else
                        {
                        // Note1: we must run this logic (to create a new message and call submit()
                        //        even if the service has stopped, as it is our only mechanism to
                        //        unblock a potentially waiting client
                        // Note2: resubmit may not throw; any exceptions are reported via the context
            
                        if (!coordinator.resubmitRequest((DistributedCacheRequest) msgRequest.cloneMessage(), partRequest, null))
                            {
                            // none of the partitions need to be resubmitted any longer
                            partRemain.remove(partRequest);
                            }
            
                        // this is either the service or a transport thread; no need to flush
                        }
                    }
                else
                    {
                    // re-submit rejected keys/partitions
                    PartitionSet partReject = msgResponse.getRejectPartitions();
                    if (!coordinator.resubmitRequest((DistributedCacheRequest) msgRequest.cloneMessage(), partReject, partReject))
                        {
                        if (partReject != null)
                            {
                            partRemain.remove(partReject);
                            }
                        }
            
                    // process the results
                    processAsyncResponse(msgResponse);
            
                    PartitionSet partResult = partRequest;
                    if (partReject != null)
                        {
                        // finalize the response only after resubmitting rejected partitions (COH-10351)
                        coordinator.finalizeResponse(partReject); // only rejected partitions
                        }
                    }
            
               if (partRemain.isEmpty())
                    {
                    context.processCompletion();
                    }
                
                // finalize the response only after resubmitting rejected partitions (COH-10351)
                coordinator.finalizeResponse(partRequest); // does not include rejected partitions
                }
            }
        
        // Declared at the super level
        /**
         * This is the event that occurs when the RequestMessage associated with
        * this poll failed in post()
         */
        public void onException(Throwable eReason)
            {
            // import Component.Net.RequestContext.AsyncContext;
            
            DistributedCacheRequest msgRequest = (DistributedCacheRequest) get_Module();
            if (msgRequest.isAsyncOperation())
                {
                AsyncContext context = (AsyncContext) msgRequest.getRequestContext();
                context.processException(eReason);
                }
            }
        
        // Declared at the super level
        /**
         * This event occurs for each response Message from each polled Member.
         */
        public void onResponse(com.tangosol.coherence.component.net.Message msg)
            {
            // import java.util.List;
            
            DistributedCacheRequest msgRequest = (DistributedCacheRequest) get_Module();
            
            List listParts = msgRequest.getPartResults();
            if (listParts == null)
                {
                // optimization for pseudo-partial requests (e.g. KeyAssociatedFilter)
                setResult(msg);
                }
            else if (!isRequestRejected())
                {
                synchronized (listParts)
                    {
                    listParts.add(msg);
                    }
                }

            // don't call (and close the poll) unless all partial responses have been received
            if (!(msgRequest instanceof PartitionedCache.PartitionedQueryRequest) ||
                msgRequest.getRepliesMask() == null ||
                msgRequest.getRepliesMask().isEmpty() ||
                ((DistributedPartialResponse) msg).getException() != null)
                {
                super.onResponse(msg);
                }
            }
        
        // Declared at the super level
        /**
         * Preprocess the response to this Poll.
        * 
        * @return true iff the response message has been fully processed
        * (onMessage was called)
         */
        public boolean preprocessResponse(com.tangosol.coherence.component.net.Message msgResponse)
            {
            DistributedCacheRequest msgRequest = (DistributedCacheRequest) get_Module();
            
            // for asynchronous operations, onCompletion() logic may require synchronization
            // and calls into user's methods and is not a good fit for preprocessing
            return !msgRequest.isAsyncOperation()
                && super.preprocessResponse(msgResponse);
            }
        
        protected void processAsyncResponse(com.tangosol.coherence.component.net.Message msg)
            {
            // this method needs to be overridden to be used
            
            throw new UnsupportedOperationException();
            }
        
        // Accessor for the property "RequestRejected"
        /**
         * Setter for property RequestRejected.<p>
        * False if the response carries any partial result; true if the request
        * has been fully rejected.
        * 
        * Note: this property is used only by the onResponse() method and any
        * changes to its default value by sub-components should be done
        * *before* super.onResponse() call is made.
         */
        protected void setRequestRejected(boolean fRejected)
            {
            __m_RequestRejected = fRejected;
            }
        }
    }
