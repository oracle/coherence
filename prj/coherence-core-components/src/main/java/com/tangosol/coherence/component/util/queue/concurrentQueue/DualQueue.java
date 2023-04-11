
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.DualQueue

package com.tangosol.coherence.component.util.queue.concurrentQueue;

import com.tangosol.util.ChainedEnumerator;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.RecyclingLinkedList;
import com.tangosol.util.SimpleEnumerator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The DualQueue is optimized for the producer consumer use case.
 * 
 * Producers work on the tail of the queue, consumers operate on the head of
 * the queue.  The two portions of the queue are maintained as seperate lists,
 * and protected by seperate locks.
 * 
 * When a consumer looks at the head of the queue, if it is empty, the head and
 * tail will be swaped.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class DualQueue
        extends    com.tangosol.coherence.component.util.queue.ConcurrentQueue
    {
    // ---- Fields declarations ----
    
    /**
     * Property HeadElementList
     *
     * The storage for the head of the Queue.
     * 
     * @volatile this value is periodically swapped with the tail of the list,
     * and is also accessed outside of synchronization, thus it is volatile.
     */
    private volatile com.tangosol.util.RecyclingLinkedList __m_HeadElementList;
    
    /**
     * Property HeadLock
     *
     * Lock protecting operations on the head of the Queue, and head-tail
     * swapping.   We cannot simply lock on the head element list as it gets
     * swapped with the tail.
     * 
     * To avoid deadlock issues the Queue lock should never be obtained while
     * holding the head lock.
     * 
     * For example:
     * 
     * synchronized (getHeadLock())
     *     {
     *     synchronized (this)
     *         {
     *         // this is NOT ok
     *         }
     *     }
     * 
     * synchronized (this)
     *     {
     *     synchronized (getHeadLock())
     *         {
     *         // this is ok
     *         }
     *     }
     * 
     * The later approach was chosen as it allows users of the DualQueue to
     * perform external synchronization without risking a deadlock.
     */
    private transient Object __m_HeadLock;
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
    
    // Default constructor
    public DualQueue()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public DualQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        
        // state initialization: public and protected properties
        try
            {
            setBatchSize(1);
            setElementList(new com.tangosol.util.RecyclingLinkedList());
            setHeadElementList(new com.tangosol.util.RecyclingLinkedList());
            setHeadLock(new java.lang.Object());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        
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
        return new com.tangosol.coherence.component.util.queue.concurrentQueue.DualQueue();
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
            clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/DualQueue".replace('/', '.'));
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
     * Inserts  the specified element to the front of this queue.
    * 
    * @see #add
     */
    public boolean addHead(Object oElement)
        {
        if (oElement == null)
            {
            throw new IllegalArgumentException("The ConcurrentQueue does not support null values.");
            }
        
        synchronized (getHeadLock())
            {
            getHeadElementList().add(0, oElement);
            }
        
        onAddElement();
        return true;
        }
    
    // Accessor for the property "HeadElementList"
    /**
     * Getter for property HeadElementList.<p>
    * The storage for the head of the Queue.
    * 
    * @volatile this value is periodically swapped with the tail of the list,
    * and is also accessed outside of synchronization, thus it is volatile.
     */
    protected com.tangosol.util.RecyclingLinkedList getHeadElementList()
        {
        return __m_HeadElementList;
        }
    
    // Accessor for the property "HeadLock"
    /**
     * Getter for property HeadLock.<p>
    * Lock protecting operations on the head of the Queue, and head-tail
    * swapping.   We cannot simply lock on the head element list as it gets
    * swapped with the tail.
    * 
    * To avoid deadlock issues the Queue lock should never be obtained while
    * holding the head lock.
    * 
    * For example:
    * 
    * synchronized (getHeadLock())
    *     {
    *     synchronized (this)
    *         {
    *         // this is NOT ok
    *         }
    *     }
    * 
    * synchronized (this)
    *     {
    *     synchronized (getHeadLock())
    *         {
    *         // this is ok
    *         }
    *     }
    * 
    * The later approach was chosen as it allows users of the DualQueue to
    * perform external synchronization without risking a deadlock.
     */
    protected Object getHeadLock()
        {
        return __m_HeadLock;
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
        // import com.tangosol.util.ChainedEnumerator;
        // import com.tangosol.util.NullImplementation;
        // import com.tangosol.util.SimpleEnumerator;
        // import java.util.List;
        
        Object[] aoHead = null;
        Object[] aoTail = null;
        
        synchronized (getHeadLock())
            {
            List listHead = getHeadElementList();
            List listTail = getElementList();
        
            if (!listHead.isEmpty())
                {
                aoHead = listHead.toArray();
                }
            if (!listTail.isEmpty())
                {
                aoTail = listTail.toArray();
                }
            }
        
        java.util.Iterator iterHead = aoHead == null ? null : new SimpleEnumerator(aoHead);
        java.util.Iterator iterTail = aoTail == null ? null : new SimpleEnumerator(aoTail);
        
        return iterHead == null
               ? (iterTail == null ? NullImplementation.getIterator() : iterTail)
               : (iterTail == null ? iterHead : new ChainedEnumerator(iterHead, iterTail));
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
    public Object peekNoWait()
        {
        // import java.util.List;
        
        if (isEmpty())
            {
            return null;
            }
        
        synchronized (getHeadLock())
            {
            List listHead = getHeadElementList();
            if (listHead.isEmpty())
                {
                if (!swapNoWait())
                    {
                    // tail was also empty
                    return null;
                    }
                listHead = getHeadElementList();
                }           
            return listHead.get(0);        
            }
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
    public Object removeNoWait()
        {
        // import java.util.concurrent.atomic.AtomicInteger;
        // import java.util.List;
        
        AtomicInteger counter = getElementCounter();
        if (counter.get() == 0)
            {
            return null;
            }
        
        Object oEntry;
        synchronized (getHeadLock())
            {
            List listHead = getHeadElementList();
            if (listHead.isEmpty())
                {
                if (!swapNoWait())
                    {
                    // tail was also empty
                    return null;
                    }
                listHead = getHeadElementList();
                }
            oEntry = listHead.remove(0);
            }
        
        if (counter.decrementAndGet() == 0)
            {
            onEmpty();
            }
        
        return oEntry;
        }
    
    // Accessor for the property "HeadElementList"
    /**
     * Setter for property HeadElementList.<p>
    * The storage for the head of the Queue.
    * 
    * @volatile this value is periodically swapped with the tail of the list,
    * and is also accessed outside of synchronization, thus it is volatile.
     */
    protected void setHeadElementList(com.tangosol.util.RecyclingLinkedList listHead)
        {
        __m_HeadElementList = listHead;
        }
    
    // Accessor for the property "HeadLock"
    /**
     * Setter for property HeadLock.<p>
    * Lock protecting operations on the head of the Queue, and head-tail
    * swapping.   We cannot simply lock on the head element list as it gets
    * swapped with the tail.
    * 
    * To avoid deadlock issues the Queue lock should never be obtained while
    * holding the head lock.
    * 
    * For example:
    * 
    * synchronized (getHeadLock())
    *     {
    *     synchronized (this)
    *         {
    *         // this is NOT ok
    *         }
    *     }
    * 
    * synchronized (this)
    *     {
    *     synchronized (getHeadLock())
    *         {
    *         // this is ok
    *         }
    *     }
    * 
    * The later approach was chosen as it allows users of the DualQueue to
    * perform external synchronization without risking a deadlock.
     */
    protected void setHeadLock(Object oLock)
        {
        __m_HeadLock = oLock;
        }
    
    /**
     * Swap the head and tail, but only if the head is empty and the tail is
    * not.  The calling thread must already hold the head lock.
    * 
    * @return true if the head and tail were swapped
     */
    protected boolean swapNoWait()
        {
        // import com.tangosol.util.RecyclingLinkedList;
        
        RecyclingLinkedList listHead = getHeadElementList();
        if (listHead.isEmpty())
            {
            RecyclingLinkedList listTail = getElementList();
            if (!listTail.isEmpty())
                {        
                setElementList(listHead);
                setHeadElementList(listTail);
                return true;                        
                }
            }
        // no swapping needed
        return false;
        }
    }
