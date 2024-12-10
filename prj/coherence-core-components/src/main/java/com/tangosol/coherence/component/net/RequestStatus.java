
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.RequestStatus

package com.tangosol.coherence.component.net;

/**
 * An abstract base for components that carry state associated with a
 * partitioned cache service request.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class RequestStatus
        extends    com.tangosol.coherence.component.Net
    {
    // ---- Fields declarations ----
    
    /**
     * Property InTransition
     *
     * Specifies whether or not there are any doubts regarding the correctness
     * (due to a on-going redistribution) of the partition ownership
     * information used by the request associated with this status.
     */
    private transient boolean __m_InTransition;
    
    // Initializing constructor
    public RequestStatus(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/RequestStatus".replace('/', '.'));
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
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * The DistributedCache service component associated with this request
    * status.
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService getService()
        {
        return null;
        }
    
    // Accessor for the property "InTransition"
    /**
     * Getter for property InTransition.<p>
    * Specifies whether or not there are any doubts regarding the correctness
    * (due to a on-going redistribution) of the partition ownership information
    * used by the request associated with this status.
     */
    public boolean isInTransition()
        {
        return __m_InTransition;
        }
    
    // Accessor for the property "TargetMissing"
    /**
     * Getter for property TargetMissing.<p>
    * (Calculated) Specifies whether the associated request is missing any
    * alive target (i.e. no storage enabled members).
     */
    public boolean isTargetMissing()
        {
        return false;
        }
    
    /**
     * Clear any state associated with this status.
     */
    public void reset()
        {
        }
    
    // Accessor for the property "InTransition"
    /**
     * Setter for property InTransition.<p>
    * Specifies whether or not there are any doubts regarding the correctness
    * (due to a on-going redistribution) of the partition ownership information
    * used by the request associated with this status.
     */
    protected void setInTransition(boolean f)
        {
        __m_InTransition = f;
        }
    }
