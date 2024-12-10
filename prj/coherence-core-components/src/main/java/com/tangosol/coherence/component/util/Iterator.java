
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.Iterator

package com.tangosol.coherence.component.util;

import java.util.NoSuchElementException;

/**
 * Iterator over an array of Objects.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Iterator
        extends    com.tangosol.coherence.component.Util
        implements java.util.Iterator
    {
    // ---- Fields declarations ----
    
    /**
     * Property CanRemove
     *
     * Set to <code>true</code> when the next element has been retrieved and
     * the element can be removed. Set to <code>false</code> when no element
     * has been retrieved or the last element retrieved was already removed.
     * The base Iterator implementation does not support remove; subclasses
     * must override the remove method in a manner such as:<br>
     * <br>
     * <pre><code>if (isCanRemove())
     *     {
     *     setCanRemove(false);
     *     // do the removal
     *     // ...
     *     }
     * else
     *     {
     *     throw new IllegalStateException();
     *     }</code></pre>
     */
    private boolean __m_CanRemove;
    
    /**
     * Property Item
     *
     * The array of Objects that the Iterator is iterating.
     * 
     * @see #next
     */
    private Object[] __m_Item;
    
    /**
     * Property NextIndex
     *
     * The index of the next item for the iterator to iterate.
     * 
     * @see #hasNext
     * @see #next
     */
    private int __m_NextIndex;
    protected static com.tangosol.coherence.Component __singleton;
    
    // Default constructor
    public Iterator()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        // singleton initialization
        if (__singleton != null)
            {
            throw new IllegalStateException("A singleton for \"Iterator\" has already been set");
            }
        __singleton = this;
        
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
        com.tangosol.coherence.Component singleton = __singleton;
        
        if (singleton == null)
            {
            singleton = new com.tangosol.coherence.component.util.Iterator();
            }
        return singleton;
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
            clz = Class.forName("com.tangosol.coherence/component/util/Iterator".replace('/', '.'));
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
    
    // Accessor for the property "Item"
    /**
     * Getter for property Item.<p>
    * The array of Objects that the Iterator is iterating.
    * 
    * @see #next
     */
    protected Object[] getItem()
        {
        return __m_Item;
        }
    
    // Accessor for the property "Item"
    /**
     * Getter for property Item.<p>
    * The array of Objects that the Iterator is iterating.
    * 
    * @see #next
     */
    protected Object getItem(int i)
        {
        return getItem()[i];
        }
    
    // Accessor for the property "NextIndex"
    /**
     * Getter for property NextIndex.<p>
    * The index of the next item for the iterator to iterate.
    * 
    * @see #hasNext
    * @see #next
     */
    protected int getNextIndex()
        {
        return __m_NextIndex;
        }
    
    // From interface: java.util.Iterator
    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
    * words, returns <tt>true</tt> if <tt>next</tt> would return an element
    * rather than throwing an exception.)
    * 
    * @return <tt>true</tt> if the iterator has more elements
     */
    public boolean hasNext()
        {
        return getNextIndex() < getItem().length;
        }
    
    // Accessor for the property "CanRemove"
    /**
     * Getter for property CanRemove.<p>
    * Set to <code>true</code> when the next element has been retrieved and the
    * element can be removed. Set to <code>false</code> when no element has
    * been retrieved or the last element retrieved was already removed. The
    * base Iterator implementation does not support remove; subclasses must
    * override the remove method in a manner such as:<br>
    * <br>
    * <pre><code>if (isCanRemove())
    *     {
    *     setCanRemove(false);
    *     // do the removal
    *     // ...
    *     }
    * else
    *     {
    *     throw new IllegalStateException();
    *     }</code></pre>
     */
    protected boolean isCanRemove()
        {
        return __m_CanRemove;
        }
    
    // From interface: java.util.Iterator
    /**
     * Returns the next element in the iteration.
    * 
    * @return the next element in the iteration
    * 
    * @exception NoSuchElementException iteration has no more elements
     */
    public Object next()
        {
        // import java.util.NoSuchElementException;
        
        try
            {
            int    i = getNextIndex();
            Object o = getItem(i);
            setNextIndex(++i);
            setCanRemove(true);
            return o;
            }
        catch (ArrayIndexOutOfBoundsException e)
            {
            throw new NoSuchElementException();
            }
        }
    
    // From interface: java.util.Iterator
    /**
     * Removes from the underlying collection the last element returned by the
    * iterator (optional operation).  This method can be called only once per
    * call to <tt>next</tt>.  The behavior of an iterator is unspecified if the
    * underlying collection is modified while the iteration is in progress in
    * any way other than by calling this method.
    * 
    * @exception UnsupportedOperationException if the <tt>remove</tt> operation
    * is not supported by this Iterator
    * @exception IllegalStateException if the <tt>next</tt> method has not yet
    * been called, or the <tt>remove</tt> method has already been called after
    * the last call to the <tt>next</tt> method
     */
    public void remove()
        {
        throw new UnsupportedOperationException();
        }
    
    // Accessor for the property "CanRemove"
    /**
     * Setter for property CanRemove.<p>
    * Set to <code>true</code> when the next element has been retrieved and the
    * element can be removed. Set to <code>false</code> when no element has
    * been retrieved or the last element retrieved was already removed. The
    * base Iterator implementation does not support remove; subclasses must
    * override the remove method in a manner such as:<br>
    * <br>
    * <pre><code>if (isCanRemove())
    *     {
    *     setCanRemove(false);
    *     // do the removal
    *     // ...
    *     }
    * else
    *     {
    *     throw new IllegalStateException();
    *     }</code></pre>
     */
    protected void setCanRemove(boolean f)
        {
        __m_CanRemove = f;
        }
    
    // Accessor for the property "Item"
    /**
     * Setter for property Item.<p>
    * The array of Objects that the Iterator is iterating.
    * 
    * @see #next
     */
    protected void setItem(Object[] ao)
        {
        __m_Item = ao;
        }
    
    // Accessor for the property "Item"
    /**
     * Setter for property Item.<p>
    * The array of Objects that the Iterator is iterating.
    * 
    * @see #next
     */
    protected void setItem(int i, Object o)
        {
        getItem()[i] = o;
        }
    
    // Accessor for the property "NextIndex"
    /**
     * Setter for property NextIndex.<p>
    * The index of the next item for the iterator to iterate.
    * 
    * @see #hasNext
    * @see #next
     */
    protected void setNextIndex(int i)
        {
        __m_NextIndex = i;
        }
    }
