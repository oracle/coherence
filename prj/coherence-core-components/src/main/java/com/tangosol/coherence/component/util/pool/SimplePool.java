
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.pool.SimplePool

package com.tangosol.coherence.component.util.pool;

import com.tangosol.coherence.component.util.Queue;

/**
 * The SimplePool is a basic implementation of a Pool.  SimplePool does not
 * maintain a concept of object ownership, it only tracks objects which are
 * free to be acquired.
 * 
 * A Queue is used as the storage for these objects as random access is not
 * needed, and this allows for head/tail style optimizations.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class SimplePool
        extends    com.tangosol.coherence.component.util.Pool
    {
    // ---- Fields declarations ----
    
    /**
     * Property Storage
     *
     * Holds the elements which may be borrowed from the pool.
     */
    private com.tangosol.coherence.component.util.Queue __m_Storage;
    
    // Initializing constructor
    public SimplePool(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/pool/SimplePool".replace('/', '.'));
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
    * elements the call will block until one is available.
    * 
    * @return a pool element
     */
    public Object acquire()
        {
        return getStorage().remove();
        }
    
    // Accessor for the property "Storage"
    /**
     * Getter for property Storage.<p>
    * Holds the elements which may be borrowed from the pool.
     */
    protected com.tangosol.coherence.component.util.Queue getStorage()
        {
        return __m_Storage;
        }
    
    /**
     * Initializes the pool.
     */
    protected void initializePool()
        {
        }
    
    /**
     * Allocate a new element of the type held by this pool.  Derived
    * implementations must implement this method.
     */
    protected Object instantiateResource()
        {
        return null;
        }
    
    /**
     * Instantiate the storage which will back this SimplePool.
     */
    protected com.tangosol.coherence.component.util.Queue instantiateStorage()
        {
        // import Component.Util.Queue;
        
        return new Queue();
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification called out of
    * setConstructed() for the topmost component and that in turn notifies all
    * the children.
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as) the control returns back to the
    * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        super.onInit();
        
        setStorage(instantiateStorage());
        initializePool();
        }
    
    // Declared at the super level
    /**
     * Return an element to the pool.  The Pool implementation is free to keep
    * or discard this element.
    * 
    * @param oElement the element to return to the pool
     */
    public void release(Object oElement)
        {
        getStorage().add(oElement);
        }
    
    // Accessor for the property "Storage"
    /**
     * Setter for property Storage.<p>
    * Holds the elements which may be borrowed from the pool.
     */
    protected void setStorage(com.tangosol.coherence.component.util.Queue queueStorage)
        {
        __m_Storage = queueStorage;
        }
    }
