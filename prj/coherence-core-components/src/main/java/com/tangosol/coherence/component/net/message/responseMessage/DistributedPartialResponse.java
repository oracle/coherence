
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse

package com.tangosol.coherence.component.net.message.responseMessage;

import com.tangosol.net.partition.PartitionSet;

/**
 * Response to DistributedCache requests that returns a set of partitions that
 * have been processed.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class DistributedPartialResponse
        extends    com.tangosol.coherence.component.net.message.ResponseMessage
    {
    // ---- Fields declarations ----
    
    /**
     * Property Exception
     *
     * Exception that occurred while executing a request.
     */
    private RuntimeException __m_Exception;
    
    /**
     * Property RejectPartitions
     *
     * A PartitionSet object representing a collection of partitions that have
     * been rejected by the corresponding request.
     */
    private com.tangosol.net.partition.PartitionSet __m_RejectPartitions;

    /**
     * Property PartsResponse
     *
     * Set of partitions that the request has successfully processed. These are
     * used to indicate whether only a subset of the request mask has been
     * processed, in which case this means that more partial responses are to
     * arrive and the poll must remain open.
     */
    private com.tangosol.net.partition.PartitionSet __m_ResponsePartitions;

    // Default constructor
    public DistributedPartialResponse()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public DistributedPartialResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.responseMessage.DistributedPartialResponse();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/responseMessage/DistributedPartialResponse".replace('/', '.'));
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
     * Getter for property EstimatedByteSize.<p>
    * The estimated serialized size of this message.  A negative value
    * indicates that the size is unknown and that it is safe to estimate the
    * size via a double serialization.
     */
    public int getEstimatedByteSize()
        {
        return super.getEstimatedByteSize() +
            1; // boolean - RejectPartitions.isEmpty()
               // Note: RejectParitions is not estimated
        }
    
    // Accessor for the property "Exception"
    /**
     * Getter for property Exception.<p>
    * Exception that occurred while executing a request.
     */
    public RuntimeException getException()
        {
        return __m_Exception;
        }
    
    // Accessor for the property "RejectPartitions"
    /**
     * Getter for property RejectPartitions.<p>
    * A PartitionSet object representing a collection of partitions that have
    * been rejected by the corresponding request.
     */
    public com.tangosol.net.partition.PartitionSet getRejectPartitions()
        {
        return __m_RejectPartitions;
        }

    /**
     * Getter for ResponsePartitions
     *
     * Set of partitions that the request has successfully processed. These are
     * used to indicate whether only a subset of the request mask has been
     * processed, in which case this means that more partial responses are to
     * arrive and the poll must remain open.
     */
    public PartitionSet getResponsePartitions()
        {
        return __m_ResponsePartitions;
        }

    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import com.tangosol.net.partition.PartitionSet;
        
        super.read(input);
        
        if (input.readBoolean())
            {
            PartitionSet partReject = new PartitionSet();
            partReject.readExternal(input);
            setRejectPartitions(partReject);
            }
        
        setException((RuntimeException) readObject(input));
        }
    
    /**
     * Helper method that ensures there the RejectPartition set contains the
    * specified bucket number.
     */
    public synchronized void rejectPartition(int iBucket)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
        // import com.tangosol.net.partition.PartitionSet;
        
        PartitionSet partReject = getRejectPartitions();
        if (partReject == null)
            {
            partReject = new PartitionSet(((com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache) getService()).getPartitionCount());
            setRejectPartitions(partReject);
            }
        partReject.add(iBucket);
        }
    
    // Accessor for the property "Exception"
    /**
     * Setter for property Exception.<p>
    * Exception that occurred while executing a request.
     */
    public void setException(RuntimeException e)
        {
        __m_Exception = e;
        }
    
    // Accessor for the property "RejectPartitions"
    /**
     * Setter for property RejectPartitions.<p>
    * A PartitionSet object representing a collection of partitions that have
    * been rejected by the corresponding request.
     */
    public void setRejectPartitions(com.tangosol.net.partition.PartitionSet set)
        {
        __m_RejectPartitions = set;
        }

    /**
     * Setter for ResponsePartitions
     *
     * Set of partitions that the request has successfully processed. These are
     * used to indicate whether only a subset of the request mask has been
     * processed, in which case this means that more partial responses are to
     * arrive and the poll must remain open.
     */
    public void setResponsePartitions(PartitionSet set)
        {
        __m_ResponsePartitions = set;
        }

    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import com.tangosol.net.partition.PartitionSet;
        
        super.write(output);
        
        PartitionSet partReject = getRejectPartitions();
        if (partReject == null || partReject.isEmpty())
            {
            output.writeBoolean(false);
            }
        else
            {
            output.writeBoolean(true);
            partReject.writeExternal(output);
            }
        
        writeObject(output, getException());
        }
    }
