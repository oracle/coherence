
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest

package com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest;

import com.tangosol.coherence.component.net.requestContext.AsyncContext;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * DistributeCacheRequest is a base component for RequestMessage(s) used by the
 * partitioned cache service that are key set or filter based. Quite often a
 * collection of similar requests are sent in parallel and a client thread has
 * to wait for all of them to return.
 * 
 * KeySetRequest is a DistributeCacheRequest that is sent to one storage
 * enabled Member that presumably owns the specified keys.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class KeySetRequest
        extends    com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest
    {
    // ---- Fields declarations ----
    
    /**
     * Property KeySet
     *
     * Set of keys requested to be processed by this message
     * (Set<Binary>). This set is also used by the Poll to inform the client
     * thread about keys that have not been processed due to a redistribution.
     * 
     * @see $Poll#onResponse
     */
    private java.util.Set __m_KeySet;
    
    /**
     * Property Partitions
     *
     * Transient set of partitions left to be processed. This value could be
     * inferred from the KeySet, and is maintained only for optimization
     * purposes by the async processor completion logic. Note that the
     * semantics of this property is slightly different from Partitions
     * property on PartialRequest.
     */
    private transient com.tangosol.net.partition.PartitionSet __m_Partitions;
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
        __mapChildren.put("Poll", KeySetRequest.Poll.get_CLASS());
        }
    
    // Default constructor
    public KeySetRequest()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public KeySetRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/distributedCacheRequest/KeySetRequest".replace('/', '.'));
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
        KeySetRequest msg = (KeySetRequest) super.cloneMessage();
        
        msg.setKeySet(getKeySet());
        
        return msg;
        }
    
    // Accessor for the property "KeySet"
    /**
     * Getter for property KeySet.<p>
    * Set of keys requested to be processed by this message
    * (Set<Binary>). This set is also used by the Poll to inform the client
    * thread about keys that have not been processed due to a redistribution.
    * 
    * @see $Poll#onResponse
     */
    public java.util.Set getKeySet()
        {
        return __m_KeySet;
        }
    
    // Accessor for the property "KeySetSafe"
    /**
     * Getter for property KeySetSafe.<p>
    * A "safe" accessor for the KeySet property that can be used by the request
    * processing thread.  This safe accessor guarantees that modifications to
    * the returned key-set will not be reflected on the requestor's "view" of
    * the message.
     */
    public java.util.Set getKeySetSafe()
        {
        // import java.util.Set;
        
        Set setKeys = getKeySet();
        if (setKeys != null && getFromMember() == getService().getThisMember())
            {
            // must clone the key-set for local messages
            Set setKeysSafe = instantiateKeySet(setKeys.size());
            setKeysSafe.addAll(setKeys);
            return setKeysSafe;
            }
        else
            {
            return setKeys;
            }
        }
    
    // Accessor for the property "Partitions"
    /**
     * Getter for property Partitions.<p>
    * Transient set of partitions left to be processed. This value could be
    * inferred from the KeySet, and is maintained only for optimization
    * purposes by the async processor completion logic. Note that the semantics
    * of this property is slightly different from Partitions property on
    * PartialRequest.
     */
    public com.tangosol.net.partition.PartitionSet getPartitions()
        {
        return __m_Partitions;
        }
    
    // Declared at the super level
    /**
     * Getter for property RequestPartitions.<p>
    * (Calculated) Set of partitions that need to be processed for this
    * request. This value is never null for asynchronous requests.
     */
    public com.tangosol.net.partition.PartitionSet getRequestPartitions()
        {
        return getPartitions();
        }
    
    protected java.util.Set instantiateKeySet(int cKeys)
        {
        // import java.util.HashSet;
        
        return new HashSet(cKeys);
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
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Set;
        
        super.read(input);
        
        int cKeys   = ExternalizableHelper.readInt(input);
        Set setKeys = instantiateKeySet(cKeys);
        
        for (int i = 0; i < cKeys; i++)
            {
            // keys are Binary objects
            Object binKey = readObject(input);
            
            setKeys.add(binKey);
            }
        
        setKeySet(setKeys);
        }
    
    // Accessor for the property "KeySet"
    /**
     * Setter for property KeySet.<p>
    * Set of keys requested to be processed by this message
    * (Set<Binary>). This set is also used by the Poll to inform the client
    * thread about keys that have not been processed due to a redistribution.
    * 
    * @see $Poll#onResponse
     */
    public void setKeySet(java.util.Set set)
        {
        __m_KeySet = set;
        }
    
    // Accessor for the property "Partitions"
    /**
     * Setter for property Partitions.<p>
    * Transient set of partitions left to be processed. This value could be
    * inferred from the KeySet, and is maintained only for optimization
    * purposes by the async processor completion logic. Note that the semantics
    * of this property is slightly different from Partitions property on
    * PartialRequest.
     */
    public void setPartitions(com.tangosol.net.partition.PartitionSet parts)
        {
        __m_Partitions = parts;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Iterator;
        // import java.util.Set;
        
        super.write(output);
        
        Set setKeys = getKeySet();
        
        ExternalizableHelper.writeInt(output, setKeys.size());
        
        for (Iterator iter = setKeys.iterator(); iter.hasNext();)
            {
            Object binKey = iter.next();
        
            writeObject(output, binKey);
            }
        
        // don't clean up the keys since they are used to identify a subset that
        // has been rejected due to a re-distribution (see $Poll#onResponse)
        // and will need to be re-processed
        }

    // ---- class: com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest$Poll
    
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
            return new com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.KeySetRequest.Poll();
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
                clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/distributedCacheRequest/KeySetRequest$Poll".replace('/', '.'));
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
            // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
            // import Component.Net.Message.ResponseMessage.DistributedPartialResponse as com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse;
            // import Component.Net.RequestContext.AsyncContext;
            // import com.tangosol.net.partition.PartitionSet;
            // import com.tangosol.util.Binary;
            // import java.util.Iterator;
            
            if (msg instanceof com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse)
                {
                KeySetRequest         msgRequest  = (KeySetRequest) get_Module();
                com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse msgResponse = (com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse) msg;
                PartitionSet    partReject  = msgResponse.getRejectPartitions();
            
                if (partReject == null)
                    {
                    msgRequest.setKeySet(null);
                    }
                else
                    {
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache service = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache) getService();
            
                    // some partitions were rejected;
                    // check for partial results and adjust the request key set
                    boolean fRejected = true;
                    for (Iterator iter = msgRequest.getKeySet().iterator(); iter.hasNext();)
                        {
                        Binary binKey = (Binary) iter.next();
                        if (!partReject.contains(service.getKeyPartition(binKey)))
                            {
                            iter.remove();
                            fRejected = false; // partial rejection
                            }
                        }
            
                    if (fRejected)
                        {
                        setRequestRejected(true);
                        }
                    }
            
                if (msgRequest.isAsyncOperation())
                    {
                    PartitionSet partRequest = msgRequest.getRequestPartitions();
            
                    if (partReject != null)
                        {
                        partRequest.remove(partReject);
                        }
            
                    // update the AsyncContext PartitionSet. KeySetRequest and its associated AsyncContext
                    // have separate PartitionSet instances.
                    PartitionSet partAll = ((AsyncContext) msgRequest.getRequestContext()).getPartitionSet();
                    synchronized (partAll)
                        {
                        partAll.remove(partRequest);
                        }
                    }
                }
            
            super.onResponse(msg);
            }
        }
    }
