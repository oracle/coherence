
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.requestStatus.MultiPartStatus

package com.tangosol.coherence.component.net.requestStatus;

import com.tangosol.net.partition.PartitionSet;

/**
 * An abstract base for components that carry state associated with a
 * partitioned cache service request.
 * 
 * MultiPartStatus is a RequestStatus for a request associated with multiple
 * partitions.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class MultiPartStatus
        extends    com.tangosol.coherence.component.net.RequestStatus
    {
    // ---- Fields declarations ----
    
    /**
     * Property ContendedPartitions
     *
     * PartitionSet containing partitions were rejected for the corresponding
     * request.
     */
    private com.tangosol.net.partition.PartitionSet __m_ContendedPartitions;
    
    /**
     * Property OrphanedPartitions
     *
     * PartitionSet containing partitions that the corresponding request is
     * associated with and are currently orphaned. This property value is most
     * commonly null (except during redistribution).
     */
    private com.tangosol.net.partition.PartitionSet __m_OrphanedPartitions;
    
    // Initializing constructor
    public MultiPartStatus(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/requestStatus/MultiPartStatus".replace('/', '.'));
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
    
    // Accessor for the property "ContendedPartitions"
    /**
     * Getter for property ContendedPartitions.<p>
    * PartitionSet containing partitions were rejected for the corresponding
    * request.
     */
    public com.tangosol.net.partition.PartitionSet getContendedPartitions()
        {
        return __m_ContendedPartitions;
        }
    
    // Accessor for the property "OrphanedPartitions"
    /**
     * Getter for property OrphanedPartitions.<p>
    * PartitionSet containing partitions that the corresponding request is
    * associated with and are currently orphaned. This property value is most
    * commonly null (except during redistribution).
     */
    public com.tangosol.net.partition.PartitionSet getOrphanedPartitions()
        {
        return __m_OrphanedPartitions;
        }
    
    /**
     * Mark the corresponding partitions as being in transition due to
    * re-distribution. If the status has previously marked some other
    * partitions as being in transition, they will be cleared.
    * 
    * @param partitions  partitions that are still in transition; the caller
    * must not hold on this PartitionSet after the call
     */
    public void markInTransition(com.tangosol.net.partition.PartitionSet partitions)
        {
        // import com.tangosol.net.partition.PartitionSet;
        
        PartitionSet partsOld = getContendedPartitions();
        if (partsOld == null)
            {
            setInTransition(true);
            }
        else
            {
            partsOld.remove(partitions);
            if (!partsOld.isEmpty())
                {
                getService().clearContention(partsOld);
                }
            }
        
        setContendedPartitions(partitions);
        getService().registerContention(partitions);
        }
    
    // Declared at the super level
    /**
     * Clear any state associated with this status.
     */
    public void reset()
        {
        // import com.tangosol.net.partition.PartitionSet;
        
        setOrphanedPartitions(null);
        
        if (isInTransition())
            {
            PartitionSet partsContended = getContendedPartitions();
        
            setInTransition(false);
            setContendedPartitions(null);
            getService().clearContention(partsContended);
            }
        }
    
    // Accessor for the property "ContendedPartitions"
    /**
     * Setter for property ContendedPartitions.<p>
    * PartitionSet containing partitions were rejected for the corresponding
    * request.
     */
    protected void setContendedPartitions(com.tangosol.net.partition.PartitionSet parts)
        {
        __m_ContendedPartitions = parts;
        }
    
    // Accessor for the property "OrphanedPartitions"
    /**
     * Setter for property OrphanedPartitions.<p>
    * PartitionSet containing partitions that the corresponding request is
    * associated with and are currently orphaned. This property value is most
    * commonly null (except during redistribution).
     */
    public void setOrphanedPartitions(com.tangosol.net.partition.PartitionSet parts)
        {
        __m_OrphanedPartitions = parts;
        }
    }
