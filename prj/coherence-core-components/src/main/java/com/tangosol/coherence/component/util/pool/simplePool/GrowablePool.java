
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.pool.simplePool.GrowablePool

package com.tangosol.coherence.component.util.pool.simplePool;

import com.tangosol.coherence.component.util.Queue;

/**
 * The GrowablePool is a SimplePool which grows on demand.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class GrowablePool
        extends    com.tangosol.coherence.component.util.pool.SimplePool
    {
    // ---- Fields declarations ----
    
    /**
     * Property Capacity
     *
     * The maximum size the Pool can grow to.  If less than or equal to zero
     * the Pool size is unlimited.
     */
    private int __m_Capacity;
    
    /**
     * Property Size
     *
     * The size of the pool, including acquired and released objects.
     */
    private int __m_Size;
    
    // Initializing constructor
    public GrowablePool(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/pool/simplePool/GrowablePool".replace('/', '.'));
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
     * Borrow an element from the Pool.  If the pool contains no available
    * elements but is under capacity it will be grown, otherwise the call will
    * block until an element becomes available.
    * 
    * @return a pool element
     */
    public Object acquire()
        {
        Object oElement = getStorage().removeNoWait();
        
        if (oElement == null)
            {
            boolean fNew = false;
            synchronized (this)
                {
                int cSize     = getSize();
                int cCapacity = getCapacity();
                if (cSize < cCapacity || cCapacity <= 0)
                    {
                    // grow the pool
                    setSize(cSize + 1);
                    fNew = true;
                    }
                }
            if (fNew)
                {
                return instantiateResource();
                }
            else
                {
                // at capacity, must wait
                return getStorage().remove();
                }
            }
        else
            {
            return oElement;
            }
        }
    
    // Accessor for the property "Capacity"
    /**
     * Getter for property Capacity.<p>
    * The maximum size the Pool can grow to.  If less than or equal to zero the
    * Pool size is unlimited.
     */
    public int getCapacity()
        {
        return __m_Capacity;
        }
    
    // Accessor for the property "Size"
    /**
     * Getter for property Size.<p>
    * The size of the pool, including acquired and released objects.
     */
    public int getSize()
        {
        return __m_Size;
        }
    
    /**
     * Grow the pool to the specified size.
     */
    public synchronized void grow(int cResources)
        {
        // import Component.Util.Queue;
        
        int iSize = getSize();
        _assert((iSize + cResources) <= getCapacity(), "growth cannot exceed capacity");
        
        Queue queue = getStorage();
        for (int i = iSize; i < cResources; ++i)
            {
            queue.add(instantiateResource());
            }
        
        setSize(iSize + cResources);
        }
    
    // Accessor for the property "Capacity"
    /**
     * Setter for property Capacity.<p>
    * The maximum size the Pool can grow to.  If less than or equal to zero the
    * Pool size is unlimited.
     */
    public void setCapacity(int cElements)
        {
        if (getCapacity() != 0)
            {
            throw new UnsupportedOperationException("GrowablePool maximum capacity cannot be changed.");
            }
        
        __m_Capacity = (cElements);
        }
    
    // Accessor for the property "Size"
    /**
     * Setter for property Size.<p>
    * The size of the pool, including acquired and released objects.
     */
    protected void setSize(int cElements)
        {
        __m_Size = cElements;
        }
    }
