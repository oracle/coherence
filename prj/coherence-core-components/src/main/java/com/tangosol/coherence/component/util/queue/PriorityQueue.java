
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.queue.PriorityQueue

package com.tangosol.coherence.component.util.queue;

import java.util.Set;
import java.util.TreeSet;

/**
 * The Queue provides a means to efficiently (and in a thread-safe manner)
 * queue received messages and messages to be sent.
 * 
 * The PriorityQueue will choose a new items position in the queue based on its
 * priority.  Priorities are assumed to be fixed, and should not change while
 * the element remains in the queue.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class PriorityQueue
        extends    com.tangosol.coherence.component.util.Queue
    {
    // ---- Fields declarations ----
    
    /**
     * Property Comparator
     *
     * The Comparator which defines the ordering of the set, if null the
     * natural ordering will be used.
     */
    private java.util.Comparator __m_Comparator;
    
    /**
     * Property Size
     *
     * The number of elements in the queue.
     * 
     * @volatile - cached for optimized reads
     */
    private volatile int __m_Size;
    
    /**
     * Property SortedElementSet
     *
     * Queue Storage, replaces the ElementList with a sorted set.
     */
    private java.util.TreeSet __m_SortedElementSet;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Iterator", com.tangosol.coherence.component.util.Queue.Iterator.get_CLASS());
        }
    
    // Initializing constructor
    public PriorityQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/queue/PriorityQueue".replace('/', '.'));
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
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    // Declared at the super level
    /**
     * Appends the specified element to the end of this queue.
    * 
    * Queues may place limitations on what elements may be added to this Queue.
    *  In particular, some Queues will impose restrictions on the type of
    * elements that may be added. Queue implementations should clearly specify
    * in their documentation any restrictions on what elements may be added.
    * 
    * @param oElement element to be appended to this Queue
    * 
    * @return true (as per the general contract of the Collection.add method)
    * 
    * @throws ClassCastException if the class of the specified element prevents
    * it from being added to this Queue
     */
    public synchronized boolean add(Object oElement)
        {
        // import java.util.Set;
        
        Set setSorted = getSortedElementSet();
        if (oElement == null)
            {
            throw new IllegalArgumentException();
            }
        else if (setSorted.add(oElement))
            {
            setSize(setSorted.size());
            signal();
            return true;
            }
        else
            {
            return false;
            }
        }
    
    // Declared at the super level
    /**
     * Inserts  the specified element to the front of this queue.
    * 
    * @see #add
     */
    public boolean addHead(Object oElement)
        {
        // as all elements are ordered based on their priority
        // an explicit addHead() operation is the same as a plain
        // old add() operation
        return add(oElement);
        }
    
    // Accessor for the property "Comparator"
    /**
     * Getter for property Comparator.<p>
    * The Comparator which defines the ordering of the set, if null the natural
    * ordering will be used.
     */
    public java.util.Comparator getComparator()
        {
        return __m_Comparator;
        }
    
    // Declared at the super level
    /**
     * Getter for property ElementList.<p>
    * Not used by the PriorityQueue
     */
    protected com.tangosol.util.RecyclingLinkedList getElementList()
        {
        throw new UnsupportedOperationException();
        }
    
    // Accessor for the property "Size"
    /**
     * Getter for property Size.<p>
    * The number of elements in the queue.
    * 
    * @volatile - cached for optimized reads
     */
    protected int getSize()
        {
        return __m_Size;
        }
    
    // Accessor for the property "SortedElementSet"
    /**
     * Getter for property SortedElementSet.<p>
    * Queue Storage, replaces the ElementList with a sorted set.
     */
    protected java.util.TreeSet getSortedElementSet()
        {
        return __m_SortedElementSet;
        }
    
    // Declared at the super level
    /**
     * Determine the number of elements in this Queue. The size of the Queue may
    * change after the size is returned from this method, unless the Queue is
    * synchronized on before calling size() and the monitor is held until the
    * operation based on this size result is complete.
    * 
    * @return the number of elements in this Queue
     */
    public boolean isEmpty()
        {
        return size() == 0;
        }
    
    // Declared at the super level
    /**
     * Provides an iterator over the elements in this Queue. The iterator is a
    * point-in-time snapshot, and the contents of the Queue may change after
    * the iterator is returned, unless the Queue is synchronized on before
    * calling iterator() and until the iterator is exhausted.
    * 
    * @return an iterator of the elements in this Queue
     */
    public java.util.Iterator iterator()
        {
        throw new UnsupportedOperationException();
        }
    
    // Declared at the super level
    /**
     * Returns the first element from the front of this Queue. If the Queue is
    * empty, no element is returned.
    * 
    * There is no blocking equivalent of this method as it would require
    * notification to wake up from an empty Queue, and this would mean that the
    * "add" and "addHead" methods would need to perform notifyAll over notify
    * which has performance implications.
    * 
    * @return the first element in the front of this Queue or null if the Queue
    * is empty
    * 
    * @see #remove
     */
    public synchronized Object peekNoWait()
        {
        return isEmpty() ? null : getSortedElementSet().first();
        }
    
    // Declared at the super level
    /**
     * Removes and returns the first element from the front of this Queue. If
    * the Queue is empty, no element is returned.
    * 
    * The blocking equivalent of this method is "remove".
    * 
    * @return the first element in the front of this Queue or null if the Queue
    * is empty
    * 
    * @see #remove
     */
    public synchronized Object removeNoWait()
        {
        // import java.util.TreeSet;
        
        TreeSet set = getSortedElementSet();
        Object  o   = set.pollFirst();
        if (o != null)
            {
            setSize(set.size());
            }
        
        return o;
        }
    
    // Accessor for the property "Comparator"
    /**
     * Setter for property Comparator.<p>
    * The Comparator which defines the ordering of the set, if null the natural
    * ordering will be used.
     */
    public synchronized void setComparator(java.util.Comparator comparator)
        {
        // import java.util.TreeSet;
        
        TreeSet setOld = getSortedElementSet();
        
        __m_Comparator = (comparator);
        
        TreeSet setNew = new TreeSet(comparator);
        setNew.addAll(setOld);
        setOld.clear(); // invalidate any externally held iterators
        
        setSortedElementSet(setNew);
        }
    
    // Accessor for the property "Size"
    /**
     * Setter for property Size.<p>
    * The number of elements in the queue.
    * 
    * @volatile - cached for optimized reads
     */
    protected void setSize(int atomic)
        {
        __m_Size = atomic;
        }
    
    // Accessor for the property "SortedElementSet"
    /**
     * Setter for property SortedElementSet.<p>
    * Queue Storage, replaces the ElementList with a sorted set.
     */
    protected void setSortedElementSet(java.util.TreeSet set)
        {
        __m_SortedElementSet = set;
        }
    
    // Declared at the super level
    /**
     * Determine the number of elements in this Queue. The size of the Queue may
    * change after the size is returned from this method, unless the Queue is
    * synchronized on before calling size() and the monitor is held until the
    * operation based on this size result is complete.
    * 
    * @return the number of elements in this Queue
     */
    public int size()
        {
        // getSortedElementSet().size() would require synchronization to allow for
        // semi accurate dirty reads; instead we utilize an external "volatile" size
        return getSize();
        }
    }
