
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.dualQueue.AssociationQueue

package com.tangosol.coherence.component.util.queue.concurrentQueue.dualQueue;

import com.oracle.coherence.common.base.Associated;
import com.oracle.coherence.common.util.AssociationPile;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The AssociationQueue is a DualQueue that allows the en-queued items be
 * associated with one another.  Items associated to the same "association key"
 * will maintain FIFO ordering, but may be re-ordered with respect to items
 * with different associations. Moreover, the AssociationQueue assumes that
 * de-queued items are being processed in parallel on multiple threads (e.g.
 * ThreadPool) and prohibits de-queuing of an item until any previously
 * de-queued associated item has been "released". Any item returned by either
 * "remove" or "removeNoWait" methods must be released via the 
 * "removeNoWait(oPrevious)" or "release()" calls.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class AssociationQueue
        extends    com.tangosol.coherence.component.util.queue.concurrentQueue.DualQueue
        implements com.oracle.coherence.common.util.AssociationPile
    {
    // ---- Fields declarations ----
    
    /**
     * Property AddCounter
     *
     * The atomic counter for the number of queue adds. Used internally to
     * compute the "Available" property.
     */
    private transient java.util.concurrent.atomic.AtomicLong __m_AddCounter;
    
    /**
     * Property AddStamp
     *
     * A snapshot of the AddCounter value at the time when no un-contended
     * items were available.  Used internally to compute the "Available"
     * property.
     */
    private transient long __m_AddStamp;
    
    /**
     * Property AllLocked
     *
     * Indicates that ASSOCIATION_ALL element has been polled.
     */
    private boolean __m_AllLocked;
    
    /**
     * Property ContendedKeys
     *
     * A set of keys associated with elements that are currently processed by
     * this queue's consumers. This set should be accessed only while holding
     * the HeadLock synchronization.
     */
    private java.util.Set __m_ContendedKeys;
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
    public AssociationQueue()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public AssociationQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setContendedKeys(new java.util.HashSet());
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
        return new com.tangosol.coherence.component.util.queue.concurrentQueue.dualQueue.AssociationQueue();
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
            clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/dualQueue/AssociationQueue".replace('/', '.'));
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
     * Check whether the flush (notify) is necessary. This method is always
    * called when a new item is added to the queue.
    * 
    * @param cElement the number of elements in the queue after the addition
     */
    protected void checkFlush(int cElements)
        {
        // The ConcurrentQueue implementation is optimized for the single producer-consumer
        // model, notifying the consumer(s) only when the queue goes from empty to non-empty.
        // The AssociationQueue is quite different - it usually has a single producer with
        // multiple consumers and there is a possibility that a presence of a task in the queue
        // does not mean it can be "removed" by workers until a worker running an "associated"
        // task finishes and releases the association. As a result, we need to revert to the simple
        // notification model.
        
        getNotifier().signal();
        }
    
    // Accessor for the property "AddCounter"
    /**
     * Getter for property AddCounter.<p>
    * The atomic counter for the number of queue adds. Used internally to
    * compute the "Available" property.
     */
    protected java.util.concurrent.atomic.AtomicLong getAddCounter()
        {
        return __m_AddCounter;
        }
    
    // Accessor for the property "AddStamp"
    /**
     * Getter for property AddStamp.<p>
    * A snapshot of the AddCounter value at the time when no un-contended items
    * were available.  Used internally to compute the "Available" property.
     */
    protected long getAddStamp()
        {
        return __m_AddStamp;
        }
    
    /**
     * Retrieve a key associated with the specified item or null.
     */
    public static Object getAssociatedKey(Object oItem)
        {
        // import com.oracle.coherence.common.base.Associated;
        
        return oItem instanceof Associated ?
            ((Associated) oItem).getAssociatedKey() : null;
        }
    
    // Accessor for the property "ContendedKeys"
    /**
     * Getter for property ContendedKeys.<p>
    * A set of keys associated with elements that are currently processed by
    * this queue's consumers. This set should be accessed only while holding
    * the HeadLock synchronization.
     */
    protected java.util.Set getContendedKeys()
        {
        return __m_ContendedKeys;
        }
    
    // Accessor for the property "AllLocked"
    /**
     * Getter for property AllLocked.<p>
    * Indicates that ASSOCIATION_ALL element has been polled.
     */
    protected boolean isAllLocked()
        {
        return __m_AllLocked;
        }
    
    // From interface: com.oracle.coherence.common.util.AssociationPile
    // Declared at the super level
    /**
     * Getter for property Available.<p>
    * True if there are items ready for removal.
    * If true, this indicates that there are potentially some uncontended items
    * on the queue.
     */
    public boolean isAvailable()
        {
        return !isEmpty() && getAddCounter().get() > getAddStamp();
        }
    
    // Declared at the super level
    /**
     * Called each time an element is added to the queue.
     */
    protected void onAddElement()
        {
        getAddCounter().incrementAndGet();
        
        super.onAddElement();
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
        // import java.util.concurrent.atomic.AtomicLong;
        
        setAddCounter(new AtomicLong());
        
        super.onInit();
        }
    
    // From interface: com.oracle.coherence.common.util.AssociationPile
    public Object poll()
        {
        // import com.oracle.coherence.common.util.AssociationPile;
        // import java.util.concurrent.atomic.AtomicInteger;
        // import java.util.List;
        
        AtomicInteger counter = getElementCounter();
        Object        oEntry;
        long          lStamp;
        
        synchronized (getHeadLock())
            {
            // this check must be done while holding the head lock;
            // see COH-1231
            if (counter.get() == 0)
                {
                return null;
                }
        
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
        
            lStamp = getAddCounter().get();
        
            boolean fUnassocOnly = isAllLocked();
        
            oEntry = removeUncontended(listHead, fUnassocOnly);
            if (oEntry == null)
                {
                oEntry = removeUncontended(getElementList(), fUnassocOnly);
                if (oEntry == AssociationPile.ASSOCIATION_ALL)
                    {
                    oEntry = null;
                    }
                }
            else if (oEntry == AssociationPile.ASSOCIATION_ALL)
                {
                oEntry = removeUncontended(getElementList(), true);
                }
            }
        
        if (oEntry == null)
            {
            setAddStamp(lStamp);
            }
        else if (counter.decrementAndGet() == 0)
            {
            onEmpty();
            }
        
        return oEntry;
        }
    
    // From interface: com.oracle.coherence.common.util.AssociationPile
    /**
     * Release an association for the specified item.
    * 
    * @param oItem if specified, this item wil be released
     */
    public void release(Object oItem)
        {
        // import com.oracle.coherence.common.util.AssociationPile;
        
        Object oKey = getAssociatedKey(oItem);
        if (oKey != null)
            {
            synchronized (getHeadLock())
                {
                if (oKey == AssociationPile.ASSOCIATION_ALL)
                    {
                    setAllLocked(false);
                    }
                else
                    {
                    getContendedKeys().remove(oKey);
                    setAddStamp(0L);
                    }
                }
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
        return poll();
        }
    
    /**
     * Remove an uncontended item from the specified list, given that the
    * specified previous key is being released, and update the association
    * contention-set iff an entry is successfully removed from the queue and
    * returned.
    * 
    * This method assumes that the caller hold the HeadLock.
    * 
    * @return ASSOCIATION_ALL if the global association element has been
    * encountered
     */
    protected Object removeUncontended(java.util.List listItem, boolean fUnassocOnly)
        {
        // import com.oracle.coherence.common.util.AssociationPile;
        // import java.util.Set;
        
        boolean fAllFound = false;
            
        int cItems = listItem.size();
        if (cItems > 0)
            {
            Set setKeys = getContendedKeys();
        
            for (int i = 0; i < cItems; i++)
                {
                Object oEntry = listItem.get(i);
                Object oKey   = getAssociatedKey(oEntry);
        
                if (oKey != null)
                    {
                    if (fUnassocOnly || setKeys.contains(oKey))
                        {
                        continue;
                        }
                    if (oKey == AssociationPile.ASSOCIATION_ALL)
                        {
                        if (setKeys.isEmpty())
                            {
                            setAllLocked(true);
                            oKey = null;
                            }
                        else
                            {
                            fAllFound    = true;
                            fUnassocOnly = true;
                            continue;
                            }
                        }
                    }
        
                if (listItem.remove(i) == oEntry)
                    {
                    // update the contended associations
                    if (oKey != null)
                        {
                        setKeys.add(oKey);
                        }
        
                    return oEntry;
                    }
                else
                    {
                    throw new IllegalStateException();
                    }
                }
            }
        
        return fAllFound ? AssociationPile.ASSOCIATION_ALL : null;
        }
    
    // Accessor for the property "AddCounter"
    /**
     * Setter for property AddCounter.<p>
    * The atomic counter for the number of queue adds. Used internally to
    * compute the "Available" property.
     */
    protected void setAddCounter(java.util.concurrent.atomic.AtomicLong counter)
        {
        __m_AddCounter = counter;
        }
    
    // Accessor for the property "AddStamp"
    /**
     * Setter for property AddStamp.<p>
    * A snapshot of the AddCounter value at the time when no un-contended items
    * were available.  Used internally to compute the "Available" property.
     */
    protected void setAddStamp(long lStamp)
        {
        __m_AddStamp = lStamp;
        }
    
    // Accessor for the property "AllLocked"
    /**
     * Setter for property AllLocked.<p>
    * Indicates that ASSOCIATION_ALL element has been polled.
     */
    protected void setAllLocked(boolean fLocked)
        {
        __m_AllLocked = fLocked;
        }
    
    // Accessor for the property "ContendedKeys"
    /**
     * Setter for property ContendedKeys.<p>
    * A set of keys associated with elements that are currently processed by
    * this queue's consumers. This set should be accessed only while holding
    * the HeadLock synchronization.
     */
    protected void setContendedKeys(java.util.Set setKeys)
        {
        __m_ContendedKeys = setKeys;
        }
    }
