
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.requestStatus.SinglePartStatus

package com.tangosol.coherence.component.net.requestStatus;

import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;

/**
 * An abstract base for components that carry state associated with a
 * partitioned cache service request.
 * 
 * SinglePartStatus is a RequestStatus for a request associated with a single
 * partition.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class SinglePartStatus
        extends    com.tangosol.coherence.component.net.RequestStatus
    {
    // ---- Fields declarations ----
    
    /**
     * Property Owner
     *
     * The Member that currently owns the request key.
     */
    private com.tangosol.coherence.component.net.Member __m_Owner;
    
    /**
     * Property Partition
     *
     * The partition that the corresponding request is associated with.
     */
    private int __m_Partition;
    
    // Initializing constructor
    public SinglePartStatus(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/requestStatus/SinglePartStatus".replace('/', '.'));
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
    
    // Accessor for the property "Owner"
    /**
     * Getter for property Owner.<p>
    * The Member that currently owns the request key.
     */
    public com.tangosol.coherence.component.net.Member getOwner()
        {
        return __m_Owner;
        }
    
    // Accessor for the property "OwnerSet"
    /**
     * Getter for property OwnerSet.<p>
    * The MemberSet representing the key owner.
     */
    public com.tangosol.coherence.component.net.MemberSet getOwnerSet()
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        
        return SingleMemberSet.instantiate(getOwner());
        }
    
    // Accessor for the property "Partition"
    /**
     * Getter for property Partition.<p>
    * The partition that the corresponding request is associated with.
     */
    public int getPartition()
        {
        return __m_Partition;
        }
    
    // Declared at the super level
    /**
     * Getter for property TargetMissing.<p>
    * (Calculated) Specifies whether the associated request is missing any
    * alive target (i.e. no storage enabled members).
     */
    public boolean isTargetMissing()
        {
        return getOwner() == null;
        }
    
    /**
     * Mark the corresponding partition as being in transition due to
    * re-distribution.
     */
    public void markInTransition()
        {
        setInTransition(true);
        getService().registerContention(getPartition());
        }
    
    // Declared at the super level
    /**
     * Clear any state associated with this status.
     */
    public void reset()
        {
        setOwner(null);
        
        if (isInTransition())
            {
            setInTransition(false);
            getService().clearContention(getPartition());
            }
        }
    
    // Declared at the super level
    /**
     * Setter for property InTransition.<p>
    * Specifies whether or not there are any doubts regarding the correctness
    * (due to a on-going redistribution) of the partition ownership information
    * used by the request associated with this status.
     */
    public void setInTransition(boolean f)
        {
        super.setInTransition(f);
        }
    
    // Accessor for the property "Owner"
    /**
     * Setter for property Owner.<p>
    * The Member that currently owns the request key.
     */
    public void setOwner(com.tangosol.coherence.component.net.Member member)
        {
        __m_Owner = member;
        }
    
    // Accessor for the property "Partition"
    /**
     * Setter for property Partition.<p>
    * The partition that the corresponding request is associated with.
     */
    public void setPartition(int iBucket)
        {
        __m_Partition = iBucket;
        }
    }
