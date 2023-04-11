
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.BalancedQueue

package com.tangosol.coherence.component.util.queue.concurrentQueue;

import com.tangosol.coherence.component.util.Queue;
import com.tangosol.util.Base;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Queue which multiplexes enqueued items onto one of many internal queues
 * based on a "target" property of the enqueued item.  Items enqueued with the
 * same target will maintain FIFO ordering, but may be re-ordered with respect
 * to items with for different targets.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class BalancedQueue
        extends    com.tangosol.coherence.component.util.queue.ConcurrentQueue
    {
    // ---- Fields declarations ----
    
    /**
     * Property Entries
     *
     * An array of the TargetMaps entries.
     * 
     * @volatile updated by producer thread, read by consumer
     */
    private volatile java.util.Map.Entry[] __m_Entries;
    
    /**
     * Property EntryIndex
     *
     * Incremental counter that serves as an index to the entry array.
     */
    private int __m_EntryIndex;
    
    /**
     * Property LockArray
     *
     * An array of lock objects.
     * 
     * Note: While conceptually similar to Base.getCommonMonitor, common
     * monitors are not suitable here as the BalancedQueue calls into "unknown"
     * code while holding synchronization which is not allowable with common
     * monitors.
     */
    private static transient Object[] __s_LockArray;
    
    /**
     * Property TargetMap
     *
     * Map of targets to their respective Queues.
     */
    private java.util.Map __m_TargetMap;
    private static com.tangosol.util.ListMap __mapChildren;
    
    private static void _initStatic$Default()
        {
        __initStatic();
        }
    
    // Static initializer (from _initStatic)
    static
        {
        _initStatic$Default();
        
        int      c  = Math.max(17, Runtime.getRuntime().availableProcessors());
        Object[] ao = new Object[c];
        
        for (int i = 0; i < c; ++i)
            {
            ao[i] = new Object();
            }
        
        setLockArray(ao);
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Iterator", com.tangosol.coherence.component.util.Queue.Iterator.get_CLASS());
        __mapChildren.put("TargetQueue", BalancedQueue.TargetQueue.get_CLASS());
        }
    
    // Initializing constructor
    public BalancedQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/BalancedQueue".replace('/', '.'));
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
    * @return true (as per the general contract of the Collection#add method)
    * 
    * @throws ClassCastException if the class of the specified element prevents
    * it from being added to this Queue
     */
    public boolean add(Object oElement)
        {
        synchronized (getLock(getTarget(oElement)))
            {
            ensureTargetQueue(oElement).add(oElement);
            }
        onAddElement();
        return true;
        }
    
    // Declared at the super level
    /**
     * Inserts  the specified element to the front of this queue.
    * 
    * @see #add
     */
    public boolean addHead(Object oElement)
        {
        synchronized (getLock(getTarget(oElement)))
            {
            ensureTargetQueue(oElement).addHead(oElement);
            }
        onAddElement();
        return true;
        }
    
    /**
     * Return the queue associated with the target of the specified entry.
     */
    protected com.tangosol.coherence.component.util.Queue ensureTargetQueue(Object oElement)
        {
        // import Component.Util.Queue;
        // import java.util.Map;
        
        Object oTarget = getTarget(oElement);
        Map    map     = getTargetMap();
        Queue  queue   = (Queue) map.get(oTarget);
        
        if (queue == null)
            {
            queue = instantiateTargetQueue();
            map.put(oTarget, queue);
            refreshEntries();
            }
        
        return queue;
        }
    
    // Accessor for the property "CurrentEntry"
    /**
     * Getter for property CurrentEntry.<p>
    * The current target queue.
     */
    public java.util.Map.Entry getCurrentEntry()
        {
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        java.util.Map.Entry[] aEntry = getEntries();
        
        int cEntries = aEntry == null ? -1 : aEntry.length;
        
        if (cEntries > 0)
            {
            return aEntry[getEntryIndex() % cEntries];
            }
        
        return null;
        }
    
    // Accessor for the property "Entries"
    /**
     * Getter for property Entries.<p>
    * An array of the TargetMaps entries.
    * 
    * @volatile updated by producer thread, read by consumer
     */
    public java.util.Map.Entry[] getEntries()
        {
        return __m_Entries;
        }
    
    // Accessor for the property "EntryIndex"
    /**
     * Getter for property EntryIndex.<p>
    * Incremental counter that serves as an index to the entry array.
     */
    public int getEntryIndex()
        {
        return __m_EntryIndex;
        }
    
    /**
     * Return the object that serves as a mutex for this target's
    * synchronization.
     */
    protected Object getLock(Object oTarget)
        {
        Object[] aoLock = getLockArray();
        int      nHash  = oTarget == null ? 0 : 0x7FFFFFFF & oTarget.hashCode();
        
        return aoLock[nHash % aoLock.length];
        }
    
    // Accessor for the property "LockArray"
    /**
     * Getter for property LockArray.<p>
    * An array of lock objects.
    * 
    * Note: While conceptually similar to Base.getCommonMonitor, common
    * monitors are not suitable here as the BalancedQueue calls into "unknown"
    * code while holding synchronization which is not allowable with common
    * monitors.
     */
    public static Object[] getLockArray()
        {
        return __s_LockArray;
        }
    
    /**
     * Get the target for the specified item.
     */
    public Object getTarget(Object oElement)
        {
        return null;
        }
    
    // Accessor for the property "TargetCount"
    /**
     * Getter for property TargetCount.<p>
    * The number of queues currently within the BalancedQueue.
     */
    public int getTargetCount()
        {
        return getTargetMap().size();
        }
    
    // Accessor for the property "TargetMap"
    /**
     * Getter for property TargetMap.<p>
    * Map of targets to their respective Queues.
     */
    protected java.util.Map getTargetMap()
        {
        return __m_TargetMap;
        }
    
    /**
     * Instantiate a new target queue.
     */
    protected com.tangosol.coherence.component.util.Queue instantiateTargetQueue()
        {
        return new BalancedQueue.TargetQueue();
        }
    
    /**
     * Advance the target iterator, and return the previous target.
     */
    protected java.util.Map.Entry nextEntry()
        {
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        java.util.Map.Entry entryCurrent = getCurrentEntry();
        
        int iEntry = getEntryIndex();
        
        // check for integer overflow, or end of array
        if (++iEntry < 0 || entryCurrent == null)
            {
            iEntry = 0;
            }
        
        setEntryIndex(iEntry);
        
        return entryCurrent;
        }
    
    /**
     * Called when a target queue has returned null from removeNoWait(),
    * indicating that it is empty.  The queue may become non-empty at any point.
     */
    public void onEmptyTarget(Object oTarget, com.tangosol.coherence.component.util.Queue queueTarget)
        {
        // import com.tangosol.util.Base;
        
        // TODO: optimize spinning on empty queues, while minimizing churning
        // potentially pool and re-use the target queues?
        
        // empty queue is only deleted occasionally to prevent churning
        // TODO: probability of remove should be based on probability of miss based
        // on stats, i.e. if there is a 50% miss ratio then use 50% remove probability
        if (Base.getRandom().nextInt(1000) == 0 && getTargetMap().size() > 1)
            {
            safeRemoveTargetQueue(oTarget);
            }
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
        // import Component.Util.Queue;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        for (java.util.Map.Entry entry = getCurrentEntry(); !isEmpty(); entry = nextEntry())
            {
            if (entry != null)
                {
                Queue  queue = (Queue) entry.getValue();
                Object oPeek = queue.peekNoWait();        
                if (oPeek != null)
                    {
                    return oPeek;
                    }
                }
            }
        
        return null;
        }
    
    /**
     * Refresh the target array, and return the new value.
     */
    protected synchronized java.util.Map.Entry[] refreshEntries()
        {
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;
        
        Set     setEntry = getTargetMap().entrySet();
        java.util.Map.Entry[] aEntry   = getEntries();
        
        if (aEntry == null)
            {
            aEntry = new java.util.Map.Entry[setEntry.size()];
            }
        
        aEntry = (java.util.Map.Entry[]) setEntry.toArray(aEntry);
        
        setEntries(aEntry);
        return aEntry;
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
        // import Component.Util.Queue;
        // import java.util.concurrent.atomic.AtomicInteger;
        // import com.tangosol.util.Base;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        Map           map     = getTargetMap();
        AtomicInteger counter = getElementCounter();
        while (counter.get() > 0)
            {    
            java.util.Map.Entry entry = nextEntry();
            if (entry == null)
                {
                continue;
                }
        
            Queue  queue    = (Queue) entry.getValue();
            Object oRemoved = queue.removeNoWait();
        
            if (oRemoved == null)
                {
                onEmptyTarget(entry.getKey(), queue);
                continue;
                }
        
            if (counter.decrementAndGet() == 0)
                {
                onEmpty();
                }    
            return oRemoved;
            }
        
        return null;
        }
    
    /**
     * Remove the specified queue for the specified target, so long as it is
    * empty.
     */
    protected void safeRemoveTargetQueue(Object oTarget)
        {
        // import Component.Util.Queue;
        // import java.util.Map;
        
        boolean fRemoved = false;
        
        // protect from add/addHead
        synchronized (getLock(oTarget))
            {
            Map   mapTarget   = getTargetMap();
            Queue queueTarget = (Queue) mapTarget.get(oTarget);
            if (queueTarget != null && queueTarget.isEmpty())
                {
                mapTarget.remove(oTarget);
                fRemoved = true;
                }
            }
        
        if (fRemoved)
            {
            refreshEntries();
            }
        }
    
    // Accessor for the property "Entries"
    /**
     * Setter for property Entries.<p>
    * An array of the TargetMaps entries.
    * 
    * @volatile updated by producer thread, read by consumer
     */
    protected void setEntries(java.util.Map.Entry[] cEntries)
        {
        __m_Entries = cEntries;
        }
    
    // Accessor for the property "EntryIndex"
    /**
     * Setter for property EntryIndex.<p>
    * Incremental counter that serves as an index to the entry array.
     */
    protected void setEntryIndex(int i)
        {
        __m_EntryIndex = i;
        }
    
    // Accessor for the property "LockArray"
    /**
     * Setter for property LockArray.<p>
    * An array of lock objects.
    * 
    * Note: While conceptually similar to Base.getCommonMonitor, common
    * monitors are not suitable here as the BalancedQueue calls into "unknown"
    * code while holding synchronization which is not allowable with common
    * monitors.
     */
    public static void setLockArray(Object[] aoArray)
        {
        __s_LockArray = aoArray;
        }
    
    // Accessor for the property "TargetMap"
    /**
     * Setter for property TargetMap.<p>
    * Map of targets to their respective Queues.
     */
    protected void setTargetMap(java.util.Map mapTarget)
        {
        __m_TargetMap = mapTarget;
        }
    
    // Declared at the super level
    public String toString()
        {
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        StringBuilder sb = new StringBuilder(super.toString());
        
        Map map = getTargetMap();
        synchronized (map)
            {
            for (java.util.Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                {
                java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
                sb.append(", target[")
                  .append(entry.getKey())
                  .append("]={")
                  .append(entry.getValue())
                  .append('}');
                }
            }
        
        return sb.toString();
        }

    // ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.BalancedQueue$TargetQueue
    
    /**
     * Child queue implementation; it is based on DualQueue but avoids all
     * notification during add as there are no threads that block on the child
     * queue.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class TargetQueue
            extends    com.tangosol.coherence.component.util.queue.concurrentQueue.DualQueue
        {
        // ---- Fields declarations ----
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
            __mapChildren.put("Iterator", BalancedQueue.TargetQueue.Iterator.get_CLASS());
            }
        
        // Default constructor
        public TargetQueue()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public TargetQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.queue.concurrentQueue.BalancedQueue.TargetQueue();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/BalancedQueue$TargetQueue".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Getter for property HeadLock.<p>
        * Lock protecting operations on the head of the Queue, and head-tail
        * swapping.   We cannot simply lock on the head element list as it gets
        * swapped with the tail.
        * 
        * To avoid deadlock issues the Queue lock should never be obtained
        * while holding the head lock.
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
        public Object getHeadLock()
            {
            return super.getHeadLock();
            }
        
        // Declared at the super level
        /**
         * Called each time an element is added to the queue.
         */
        protected void onAddElement()
            {
            // non-blocking queue, only increment, never flush
            getElementCounter().incrementAndGet();
            }
        
        // Declared at the super level
        /**
         * Waits for and removes the first element from the front of this Queue.
        * 
        * If the Queue is empty, this method will block until an element is in
        * the Queue. The non-blocking equivalent of this method is
        * "removeNoWait".
        * 
        * @return the first element in the front of this Queue
        * 
        * @see #removeNoWait
         */
        public Object remove()
            {
            throw new UnsupportedOperationException(); // see add
            }

        // ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.BalancedQueue$TargetQueue$Iterator
        
        /**
         * Iterator of a snapshot of the List object that backs the Queue.
         * Supports remove(). Uses the Queue as the monitor if any
         * synchronization is required.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Iterator
                extends    com.tangosol.coherence.component.util.Queue.Iterator
            {
            // ---- Fields declarations ----
            
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
                return new com.tangosol.coherence.component.util.queue.concurrentQueue.BalancedQueue.TargetQueue.Iterator();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/BalancedQueue$TargetQueue$Iterator".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }
        }
    }
