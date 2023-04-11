
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.PartialJob

package com.tangosol.coherence.component.util;

/**
 * PartialJob represents a segment of a DistributedCacheRequest that contains
 * keys (entries) that belong to the same partition (bucket). It assumes to
 * belong to (contained by) a DistributedRequestMessage.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class PartialJob
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.net.PriorityTask,
                   com.tangosol.net.cache.KeyAssociation,
                   Runnable
    {
    // ---- Fields declarations ----
    
    /**
     * Property BatchContext
     *
     */
    private com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BatchContext __m_BatchContext;
    
    /**
     * Property Partition
     *
     * Partition number for this job.
     */
    private transient int __m_Partition;
    
    /**
     * Property Request
     *
     * The DistributedCacheRequest that this job is a part of (belongs to).
     */
    private transient com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest __m_Request;
    
    /**
     * Property RequestContext
     *
     */
    private com.tangosol.coherence.component.net.RequestContext __m_RequestContext;
    
    // Default constructor
    public PartialJob()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public PartialJob(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.PartialJob();
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
            clz = Class.forName("com.tangosol.coherence/component/util/PartialJob".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.cache.KeyAssociation
    public Object getAssociatedKey()
        {
        return getRequest().getAssociatedKey();
        }
    
    // Accessor for the property "BatchContext"
    /**
     * Getter for property BatchContext.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BatchContext getBatchContext()
        {
        return __m_BatchContext;
        }
    
    // Accessor for the property "CacheId"
    /**
     * Getter for property CacheId.<p>
    * The cache id for this job.
     */
    public long getCacheId()
        {
        return getRequest().getCacheId();
        }
    
    // Accessor for the property "Description"
    /**
     * Getter for property Description.<p>
    * Used for debugging purposes (from toString). Create a human-readable
    * description of the specific job data.
     */
    public String getDescription()
        {
        return "CacheId="   + getCacheId() +
             ", Partition=" + getPartition();
        }
    
    // From interface: com.tangosol.net.PriorityTask
    public long getExecutionTimeoutMillis()
        {
        return getRequest().getExecutionTimeoutMillis();
        }
    
    // Accessor for the property "Partition"
    /**
     * Getter for property Partition.<p>
    * Partition number for this job.
     */
    public int getPartition()
        {
        return __m_Partition;
        }
    
    // Accessor for the property "Request"
    /**
     * Getter for property Request.<p>
    * The DistributedCacheRequest that this job is a part of (belongs to).
     */
    public com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest getRequest()
        {
        return __m_Request;
        }
    
    // Accessor for the property "RequestContext"
    /**
     * Getter for property RequestContext.<p>
     */
    public com.tangosol.coherence.component.net.RequestContext getRequestContext()
        {
        return __m_RequestContext;
        }
    
    // From interface: com.tangosol.net.PriorityTask
    public long getRequestTimeoutMillis()
        {
        return getRequest().getSchedulingPriority();
        }
    
    // Accessor for the property "Response"
    /**
     * Getter for property Response.<p>
    * The response message to accumulate the individual responses at.
     */
    public com.tangosol.coherence.component.net.Message getResponse()
        {
        return getBatchContext().getPrimaryResponse();
        }
    
    // From interface: com.tangosol.net.PriorityTask
    public int getSchedulingPriority()
        {
        return getRequest().getSchedulingPriority();
        }
    
    // From interface: java.lang.Runnable
    public void run()
        {
        }
    
    // From interface: com.tangosol.net.PriorityTask
    public void runCanceled(boolean fAbandoned)
        {
        getRequest().runCanceled(fAbandoned);
        }
    
    // Accessor for the property "BatchContext"
    /**
     * Setter for property BatchContext.<p>
     */
    public void setBatchContext(com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BatchContext ctx)
        {
        __m_BatchContext = ctx;
        }
    
    // Accessor for the property "Partition"
    /**
     * Setter for property Partition.<p>
    * Partition number for this job.
     */
    public void setPartition(int nPartition)
        {
        __m_Partition = nPartition;
        }
    
    // Accessor for the property "Request"
    /**
     * Setter for property Request.<p>
    * The DistributedCacheRequest that this job is a part of (belongs to).
     */
    public void setRequest(com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest request)
        {
        __m_Request = request;
        }
    
    // Accessor for the property "RequestContext"
    /**
     * Setter for property RequestContext.<p>
     */
    public void setRequestContext(com.tangosol.coherence.component.net.RequestContext contextRequest)
        {
        __m_RequestContext = contextRequest;
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + '{' + getDescription() + '}';
        }
    }
