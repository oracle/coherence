
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.requestContext.AsyncContext

package com.tangosol.coherence.component.net.requestContext;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.partition.PartitionSet;

/**
 * RequestContext for asyncronous requests.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class AsyncContext
        extends    com.tangosol.coherence.component.net.RequestContext
        implements com.tangosol.net.FlowControl
    {
    // ---- Fields declarations ----
    
    /**
     * Property Partition
     *
     * (Transient) Partition this context is associated with. This value is
     * meaningful only if the PartitionSet value is null.
     */
    private transient int __m_Partition;
    
    /**
     * Property PartitionSet
     *
     * (Transient) PartitionSet this context is associated with.
     * If this value is null, the context is associated with a single partition
     * held by the Partition property. Otherwise, it contains the partitions
     * that are yet to be processed.
     */
    private transient com.tangosol.net.partition.PartitionSet __m_PartitionSet;
    
    /**
     * Property ValueConverter
     *
     * (Transient) Converter used to convert keys and values from internal
     * format.
     */
    private transient com.tangosol.util.Converter __m_ValueConverter;
    
    // Initializing constructor
    public AsyncContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/requestContext/AsyncContext".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.FlowControl
    public boolean checkBacklog(com.oracle.coherence.common.base.Continuation continueNormal)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache;
        // import com.tangosol.net.partition.PartitionSet;
        
        PartitionedCache service = getService();
        PartitionSet     parts   = getPartitionSet();
        
        if (parts == null)
            {
            return service.getRequestCoordinator().checkBacklog(getPartition(), continueNormal);
            }
        else
            {
            return service.getRequestCoordinator().checkBacklog(parts, continueNormal);
            }
        }
    
    // From interface: com.tangosol.net.FlowControl
    public long drainBacklog(long cMillis)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.partition.PartitionSet;
        
        PartitionedCache service = (PartitionedCache) getService();
        try
            {
            PartitionSet parts = getPartitionSet();
            if (parts == null)
                {
                return service.getRequestCoordinator().drainBacklog(getPartition(), cMillis);
                }
            else
                {
                return service.getRequestCoordinator().drainBacklog(parts, cMillis);
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            return -1L;
            }
        catch (RequestTimeoutException e)
            {
            return -1L;
            }
        }
    
    // From interface: com.tangosol.net.FlowControl
    public void flush()
        {
        getService().flush();
        }
    
    // Accessor for the property "Cache"
    /**
     * Getter for property Cache.<p>
    * The client cache reference (BinaryMap) associated with this async request
    * context.
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.BinaryMap getCache()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$BinaryMap as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.BinaryMap;
        
        return (com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.BinaryMap) get_Parent();
        }
    
    // Accessor for the property "Partition"
    /**
     * Getter for property Partition.<p>
    * (Transient) Partition this context is associated with. This value is
    * meaningful only if the PartitionSet value is null.
     */
    public int getPartition()
        {
        return __m_Partition;
        }
    
    // Accessor for the property "PartitionSet"
    /**
     * Getter for property PartitionSet.<p>
    * (Transient) PartitionSet this context is associated with.
    * If this value is null, the context is associated with a single partition
    * held by the Partition property. Otherwise, it contains the partitions
    * that are yet to be processed.
     */
    public com.tangosol.net.partition.PartitionSet getPartitionSet()
        {
        return __m_PartitionSet;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * The PartitionedCache service that created this context.
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache getService()
        {
        return getCache().getService();
        }
    
    // Accessor for the property "ValueConverter"
    /**
     * Getter for property ValueConverter.<p>
    * (Transient) Converter used to convert keys and values from internal
    * format.
     */
    public com.tangosol.util.Converter getValueConverter()
        {
        return __m_ValueConverter;
        }
    
    /**
     * Process the completion of the request submission.
     */
    public void processCompletion()
        {
        }
    
    /**
     * Process an exception that occurred during the request submission.
     */
    public void processException(Throwable e)
        {
        }
    
    protected void reportException(Throwable e)
        {
        _trace("An exception occurred during asynchronous operation: " + this + "\n"
             + getStackTrace(e)
             + "\nThe exception has been logged and execution is continuing.)", 1);
        }
    
    // Accessor for the property "Partition"
    /**
     * Setter for property Partition.<p>
    * (Transient) Partition this context is associated with. This value is
    * meaningful only if the PartitionSet value is null.
     */
    public void setPartition(int nPartition)
        {
        __m_Partition = nPartition;
        }
    
    // Accessor for the property "PartitionSet"
    /**
     * Setter for property PartitionSet.<p>
    * (Transient) PartitionSet this context is associated with.
    * If this value is null, the context is associated with a single partition
    * held by the Partition property. Otherwise, it contains the partitions
    * that are yet to be processed.
     */
    public void setPartitionSet(com.tangosol.net.partition.PartitionSet nPartition)
        {
        __m_PartitionSet = nPartition;
        }
    
    // Accessor for the property "ValueConverter"
    /**
     * Setter for property ValueConverter.<p>
    * (Transient) Converter used to convert keys and values from internal
    * format.
     */
    public void setValueConverter(com.tangosol.util.Converter converter)
        {
        __m_ValueConverter = converter;
        }
    }
