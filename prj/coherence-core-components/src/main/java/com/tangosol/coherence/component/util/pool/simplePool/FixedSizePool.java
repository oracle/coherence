
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.pool.simplePool.FixedSizePool

package com.tangosol.coherence.component.util.pool.simplePool;

import com.tangosol.coherence.component.util.Queue;

/**
 * The FixedSizePool is a SimplePool of fixed size.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class FixedSizePool
        extends    com.tangosol.coherence.component.util.pool.SimplePool
    {
    // ---- Fields declarations ----
    
    /**
     * Property Capacity
     *
     * The capacity of the fixed sized pool.
     */
    private transient int __m_Capacity;
    
    // Initializing constructor
    public FixedSizePool(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/pool/simplePool/FixedSizePool".replace('/', '.'));
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
    
    // Accessor for the property "Capacity"
    /**
     * Getter for property Capacity.<p>
    * The capacity of the fixed sized pool.
     */
    public int getCapacity()
        {
        return __m_Capacity;
        }
    
    // Declared at the super level
    /**
     * Getter for property Storage.<p>
    * Holds the elements which may be borrowed from the pool.
     */
    public com.tangosol.coherence.component.util.Queue getStorage()
        {
        return super.getStorage();
        }
    
    // Declared at the super level
    /**
     * Initializes the pool, filling it based on the capacity.
     */
    protected void initializePool()
        {
        // import Component.Util.Queue;
        
        Queue queueStorage = getStorage();
        for (int i = 0, c = getCapacity(); i < c; ++i)
            {
            queueStorage.add(instantiateResource());    
            }
        }
    
    // Accessor for the property "Capacity"
    /**
     * Setter for property Capacity.<p>
    * The capacity of the fixed sized pool.
     */
    public void setCapacity(int cElements)
        {
        if (getCapacity() != 0)
            {
            throw new UnsupportedOperationException("FixedSizePool cannot be resized");
            }
        
        __m_Capacity = (cElements);
        
        if (is_Constructed())
            {
            initializePool();
            }
        }
    
    // Declared at the super level
    /**
     * Setter for property Storage.<p>
    * Holds the elements which may be borrowed from the pool.
     */
    public void setStorage(com.tangosol.coherence.component.util.Queue queueStorage)
        {
        super.setStorage(queueStorage);
        }
    }
