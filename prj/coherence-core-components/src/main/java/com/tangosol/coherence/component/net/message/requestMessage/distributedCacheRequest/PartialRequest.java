
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest

package com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.net.partition.PartitionSet;

/**
 * DistributeCacheRequest is a base component for RequestMessage(s) used by the
 * partitioned cache service that are key set or filter based. Quite often a
 * collection of similar requests are sent in parallel and a client thread has
 * to wait for all of them to return.
 * 
 * PartialRequest is a DistributeCacheRequest that is constrained by a subset
 * of partitions owned by a single storage-enabled Member.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class PartialRequest
        extends    com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest
    {
    // ---- Fields declarations ----
    
    /**
     * Property Partitions
     *
     * Transient set of partitions left to be processed which is used by the
     * Poll completion logic to inform the client thread about partitions that
     * have not been processed due to a redistribution. Note that the semantics
     * of this property is slightly different from Partitions property on
     * KeySetRequest.
     */
    private transient com.tangosol.net.partition.PartitionSet __m_Partitions;
    
    /**
     * Property RequestMask
     *
     * Set of partitions that the request processing have to be masked by. A
     * value of "null" indicates that ALL owned partitions have to be processed.
     */
    private com.tangosol.net.partition.PartitionSet __m_RequestMask;
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
        __mapChildren.put("Poll", PartialRequest.Poll.get_CLASS());
        }
    
    // Default constructor
    public PartialRequest()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public PartialRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/distributedCacheRequest/PartialRequest".replace('/', '.'));
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
        PartialRequest msg = (PartialRequest) super.cloneMessage();
        
        msg.setRequestMask(getRequestMask());
        
        return msg;
        }
    
    // Accessor for the property "Partitions"
    /**
     * Getter for property Partitions.<p>
    * Transient set of partitions left to be processed which is used by the
    * Poll completion logic to inform the client thread about partitions that
    * have not been processed due to a redistribution. Note that the semantics
    * of this property is slightly different from Partitions property on
    * KeySetRequest.
     */
    public com.tangosol.net.partition.PartitionSet getPartitions()
        {
        return __m_Partitions;
        }
    
    // Accessor for the property "RequestMask"
    /**
     * Getter for property RequestMask.<p>
    * Set of partitions that the request processing have to be masked by. A
    * value of "null" indicates that ALL owned partitions have to be processed.
     */
    public com.tangosol.net.partition.PartitionSet getRequestMask()
        {
        return __m_RequestMask;
        }
    
    // Accessor for the property "RequestMaskSafe"
    /**
     * Getter for property RequestMaskSafe.<p>
    * A "safe" accessor for the RequestMask property that can be used by the
    * request processing thread.  This safe accessor guarantees that
    * modifications to the returned partition-set will not be reflected on the
    * requestor's "view" of the message.
     */
    public com.tangosol.net.partition.PartitionSet getRequestMaskSafe()
        {
        // import com.tangosol.net.partition.PartitionSet;
        
        PartitionSet partsMask = getRequestMask();
        return partsMask != null && getFromMember() == getService().getThisMember()
            ? new PartitionSet(partsMask)  // must clone the partition set for local messages
            : partsMask;
        }
    
    // Declared at the super level
    /**
     * Getter for property RequestPartitions.<p>
    * (Calculated) Set of partitions that need to be processed for this
    * request. This value is never null for asynchronous requests.
     */
    public com.tangosol.net.partition.PartitionSet getRequestPartitions()
        {
        return getRequestMask();
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import com.tangosol.net.partition.PartitionSet;
        
        super.read(input);
        
        if (input.readBoolean())
            {
            PartitionSet partMask = new PartitionSet();
            partMask.readExternal(input);
            setRequestMask(partMask);
            }
        }
    
    // Accessor for the property "Partitions"
    /**
     * Setter for property Partitions.<p>
    * Transient set of partitions left to be processed which is used by the
    * Poll completion logic to inform the client thread about partitions that
    * have not been processed due to a redistribution. Note that the semantics
    * of this property is slightly different from Partitions property on
    * KeySetRequest.
     */
    public void setPartitions(com.tangosol.net.partition.PartitionSet parts)
        {
        __m_Partitions = parts;
        }
    
    // Accessor for the property "RequestMask"
    /**
     * Setter for property RequestMask.<p>
    * Set of partitions that the request processing have to be masked by. A
    * value of "null" indicates that ALL owned partitions have to be processed.
     */
    public void setRequestMask(com.tangosol.net.partition.PartitionSet partsMask)
        {
        __m_RequestMask = partsMask;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import com.tangosol.net.partition.PartitionSet;
        
        super.write(output);
        
        PartitionSet partMask = getRequestMask();
        if (partMask == null)
            {
            output.writeBoolean(false);
            }
        else
            {
            output.writeBoolean(true);
            partMask.writeExternal(output);
            }
        }

    // ---- class: com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest$Poll
    
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
            extends    com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest.Poll
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
            return new com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest.Poll();
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
                clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/distributedCacheRequest/PartialRequest$Poll".replace('/', '.'));
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
         * This event occurs for each response Message from each polled Member.
         */
        public void onResponse(com.tangosol.coherence.component.net.Message msg)
            {
            // import Component.Net.Message.ResponseMessage.DistributedPartialResponse as com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse;
            // import com.tangosol.net.partition.PartitionSet;
            
            if (msg instanceof com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse)
                {
                PartialRequest msgRequest = (PartialRequest) get_Module();
                com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse msgResponse = (com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse) msg;

                PartitionSet partAll      = msgRequest.getPartitions();
                PartitionSet partRequest  = msgRequest.getRequestMask();
                PartitionSet partReject   = msgResponse.getRejectPartitions();
                PartitionSet partResponse = msgResponse.getResponsePartitions();
                PartitionSet partReplies  = msgRequest.getRepliesMask();

                _assert(partRequest != partAll);

                if (partReject != null)
                    {
                    // some partitions were rejected; adjust the request mask
                    partRequest.remove(partReject);

                    if (partReplies != null)
                        {
                        synchronized (partReplies)
                            {
                            partReplies.remove(partReject);
                            }
                        }
                    setRequestRejected(partRequest.isEmpty());
                    }

                if (partResponse == null)
                    {
                    partResponse = new PartitionSet(partRequest);
                    }

                if (partReplies != null && partResponse != null)
                    {
                    synchronized (partReplies)
                        {
                        partReplies.remove(partResponse);
                        }
                    }

                synchronized (partAll)
                    {
                    boolean fUnique = partAll.remove(partResponse);
                    if (!fUnique)
                        {
                        // soft assertion
                        _trace("Intersecting partial response for " + msgRequest.get_Name() +
                               "; partitions=" + partResponse, 1);
                        }
                    }

                }
            super.onResponse(msg);
            }
        
        // Declared at the super level
        public void processAsyncResponse(com.tangosol.coherence.component.net.Message msg)
            {
            super.processAsyncResponse(msg);
            }
        }
    }
