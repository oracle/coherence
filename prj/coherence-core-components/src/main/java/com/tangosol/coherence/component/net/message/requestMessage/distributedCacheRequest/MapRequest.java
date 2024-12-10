
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.MapRequest

package com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest;

import com.tangosol.internal.util.UnsafeBinaryWriteBuffer;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * DistributeCacheRequest is a base component for RequestMessage(s) used by the
 * partitioned cache service that are key set or filter based. Quite often a
 * collection of similar requests are sent in parallel and a client thread has
 * to wait for all of them to return.
 * 
 * MapRequest is a DistributeCacheRequest that is sent to one storage enabled
 * Member that presumably owns the specified entries.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class MapRequest
        extends    com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest
    {
    // ---- Fields declarations ----
    
    /**
     * Property Map
     *
     * Map of entries requested to be updated by this message
     * (Map<Binary, Binary>).  As responses come from the serving nodes,
     * rejected entries could be removed from this map.
     */
    private java.util.Map __m_Map;
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
        __mapChildren.put("Poll", MapRequest.Poll.get_CLASS());
        }
    
    // Default constructor
    public MapRequest()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public MapRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.MapRequest();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/distributedCacheRequest/MapRequest".replace('/', '.'));
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
        MapRequest msg = (MapRequest) super.cloneMessage();
        
        msg.setMap(getMap());
        
        return msg;
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
        // import com.tangosol.util.Binary;
        // import java.util.Iterator;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        int cb = super.getEstimatedByteSize() +
            4; // int - cEntries
        
        for (Iterator iter = getMap().entrySet().iterator(); iter.hasNext();)
            {
            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
            
            cb += 4 + // int - type
                  4 + // int - length
                ((Binary) entry.getKey()).length();
        
            Binary binValue = (Binary) entry.getValue();
            cb += 4 + // int - type
                (binValue == null ? 0 : 4 + binValue.length());
            }
        
        return cb;
        }
    
    // Accessor for the property "Map"
    /**
     * Getter for property Map.<p>
    * Map of entries requested to be updated by this message
    * (Map<Binary, Binary>).  As responses come from the serving nodes,
    * rejected entries could be removed from this map.
     */
    public java.util.Map getMap()
        {
        return __m_Map;
        }
    
    // Accessor for the property "MapSafe"
    /**
     * Getter for property MapSafe.<p>
    * A "safe" accessor for the Map property that can be used by the request
    * processing thread.  This safe accessor guarantees that modifications to
    * the returned map will not be reflected on the requestor's "view" of the
    * message.
     */
    public java.util.Map getMapSafe()
        {
        // import java.util.Map;
        
        Map map = getMap();
        if (map != null && getFromMember() == getService().getThisMember())
            {
            // must clone the map for local messages
            Map mapSafe = instantiateEntryMap(map.size());
            mapSafe.putAll(map);
            return mapSafe;
            }
        else
            {
            return map;
            }
        }
    
    /**
     * Instantiate and return a Map to hold the entries for this request.
    * 
    * @param cEntries  the size of the map
     */
    protected java.util.Map instantiateEntryMap(int cEntries)
        {
        // import java.util.HashMap;
        
        return new HashMap(cEntries);
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        super.read(input);
        
        int cEntries   = ExternalizableHelper.readInt(input);
        Map mapEntries = instantiateEntryMap(cEntries);
        
        for (int i = 0; i < cEntries; i++)
            {
            // both key and value are Binary objects
            Object binKey = readObject(input);
            Object binVal = readObject(input);
        
            mapEntries.put(binKey, binVal);
            }
        
        setMap(mapEntries);
        }
    
    // Accessor for the property "Map"
    /**
     * Setter for property Map.<p>
    * Map of entries requested to be updated by this message
    * (Map<Binary, Binary>).  As responses come from the serving nodes,
    * rejected entries could be removed from this map.
     */
    public void setMap(java.util.Map map)
        {
        __m_Map = map;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$ConverterValueToBinary as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ConverterValueToBinary;
        // import com.tangosol.internal.util.UnsafeBinaryWriteBuffer;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        try
            {
            super.write(output);
        
            Map mapEntries = getMap();
            int cEntries   = mapEntries.size();
        
            ExternalizableHelper.writeInt(output, cEntries);
        
            // Set an unsafe write buffer to reuse when serializing values.
            //
            // This buffer is thread local, will only be used by the entry.getValue() calls
            // in the loop below, and must be released by setting to null in the finally block
        
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ConverterValueToBinary.setWriteBuffer(UnsafeBinaryWriteBuffer.get());
        
            for (Iterator iter = mapEntries.entrySet().iterator(); iter.hasNext(); )
                {
                java.util.Map.Entry  entry  = (java.util.Map.Entry) iter.next();
                Object binKey = entry.getKey();
        
                writeObject(output, binKey);
                writeObject(output, (Binary) entry.getValue());
                }
            }
        finally
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ConverterValueToBinary.setWriteBuffer(null);
            }
        
        // do not cleanup the Map; it will be done on a poll response
        }

    // ---- class: com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.MapRequest$Poll
    
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
            return new com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.MapRequest.Poll();
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
                clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/distributedCacheRequest/MapRequest$Poll".replace('/', '.'));
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
            // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
            // import com.tangosol.net.partition.PartitionSet;
            // import com.tangosol.util.Binary;
            // import java.util.Iterator;
            
            if (msg instanceof com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse)
                {
                MapRequest         msgRequest  = (MapRequest) get_Parent();
                com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse msgResponse = (com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse) msg;
                PartitionSet    partReject  = msgResponse.getRejectPartitions();
            
                if (partReject == null || partReject.isEmpty())
                    {
                    msgRequest.setMap(null);
                    }
                else
                    {
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache service = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache) getService();
            
                    // some partitions were rejected;
                    // check for partial results and adjust the request key set
                    boolean fRejected = true;
                    for (Iterator iter = msgRequest.getMap().keySet().iterator(); iter.hasNext();)
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
                }
            
            super.onResponse(msg);
            }
        }
    }
