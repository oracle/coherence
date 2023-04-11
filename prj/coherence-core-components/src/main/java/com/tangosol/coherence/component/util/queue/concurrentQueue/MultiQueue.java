
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.MultiQueue

package com.tangosol.coherence.component.util.queue.concurrentQueue;

import com.tangosol.util.Base;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The ConcurrentQueue provides a means to efficiently (and in a thread-safe
 * manner) queue elements with minimal contention.
 * 
 * Note: The ConcurrentQueue does not support null entries.
 * 
 * The MultiQueue provides high concurrency by spreading synchronization over
 * multiple monitors.  As compared to the DualQueue which has two independent
 * synchronization points (head and tail), each element in the MultiQueue is a
 * logically independent synchronization point.  While the DualQueue is
 * optimized for the two thread model with one producer and one consumer, the
 * MultiQueue is optimized for N producers and either one or more consumers. 
 * See the Notifier property for details on configuring the MultiQueue for
 * either one or N consumers.
 * 
 * The MultiQueue is abstract and requires implementations of the
 * assignIndexToValue and retrieveIndexFromValue methods on the WindowedArray
 * child component.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class MultiQueue
        extends    com.tangosol.coherence.component.util.queue.ConcurrentQueue
    {
    // ---- Fields declarations ----
    
    /**
     * Property NextRemoveableIndex
     *
     * An AtomicCounter identifying the next removable index in the
     * WindowedArray.  When in multi-notifier mode this will refer to the next
     * index to be removable without contending with other remove operations.
     */
    private java.util.concurrent.atomic.AtomicLong __m_NextRemoveableIndex;
    
    /**
     * Property WindowedArray
     *
     * The WindowedArray that backs the Queue.
     */
    private MultiQueue.WindowedArray __m_WindowedArray;
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
    public MultiQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/MultiQueue".replace('/', '.'));
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
        if (oElement == null)
            {
            throw new IllegalArgumentException("The MultiQueue does not support null values.");
            }
        
        getWindowedArray().add(oElement);
        
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
        // WindowedArray does not support insertion operations as every element in the
        // array "knows" its position
        throw new UnsupportedOperationException();
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
        if (cElements == 0)
            {
            // This method is called by super.onAddElement(), as part of an add().
            // With the multiqueue it's possible for the remove operation to
            // complete before the associated add completes.  Thus onEmpty() can
            // be triggered for the -1 -> 0 transition as well as the 1 -> 0 transition
            onEmpty();
            }
        else
            {
            super.checkFlush(cElements);
            }
        }
    
    // Accessor for the property "NextRemoveableIndex"
    /**
     * Getter for property NextRemoveableIndex.<p>
    * An AtomicCounter identifying the next removable index in the
    * WindowedArray.  When in multi-notifier mode this will refer to the next
    * index to be removable without contending with other remove operations.
     */
    public java.util.concurrent.atomic.AtomicLong getNextRemoveableIndex()
        {
        return __m_NextRemoveableIndex;
        }
    
    // Accessor for the property "WindowedArray"
    /**
     * Getter for property WindowedArray.<p>
    * The WindowedArray that backs the Queue.
     */
    protected MultiQueue.WindowedArray getWindowedArray()
        {
        return __m_WindowedArray;
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
        
        super.onInit();
        
        MultiQueue.WindowedArray wa = (MultiQueue.WindowedArray) _findChild("WindowedArray");
        setWindowedArray(wa);
        
        setNextRemoveableIndex(new AtomicLong(wa.getFirstIndex()));
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
        // import java.util.concurrent.atomic.AtomicLong;
        
        MultiQueue.WindowedArray wa         = getWindowedArray();
        AtomicLong     atomicNext = getNextRemoveableIndex();
        long           lNext      = atomicNext.get();
        Object         oPeek      = wa.optimisticGet(lNext);
        
        // if optimistic get failed, retry non-optimistic with updated index
        return oPeek == null && lNext <= wa.getLastIndex()
            ? wa.get(atomicNext.get()) : oPeek;
        }
    
    // Declared at the super level
    /**
     * Waits for and removes the first element from the front of this Queue.
    * 
    * If the Queue is empty, this method will block until an element is in the
    * Queue or until the specified wait time has passed. The non-blocking
    * equivalent of this method is "removeNoWait".
    * 
    * @param cMillis  the number of ms to wait for an element; pass 0 to wait
    * indefinitely
    * 
    * @return the first element in the front of this Queue or null if the Queue
    * is empty after the specified wait time has passed
    * 
    * @see #removeNoWait
     */
    public Object remove(long cMillis)
        {
        return getNotifier() == null ? removeMulti(cMillis) : super.remove(cMillis);
        }
    
    /**
     * Internal implementation of remove() optimized for multiple consumer
    * threads.  Remove and return the first element from the queue, or null if
    * the specified timeout expires.
    * 
    * @param cMillis  the number of milliseconds to wait for an element; pass 0
    * to wait indefinitely
     */
    protected Object removeMulti(long cMillis)
        {
        // import com.tangosol.util.Base;
        
        MultiQueue.WindowedArray wa = getWindowedArray();
        while (true)
            {
            long    lNext     = getNextRemoveableIndex().getAndIncrement();
            boolean fRollback = true;
            long    cWait     = 0 < cMillis && cMillis < 1000L ? cMillis : 1000L;
            try
                {
                // we don't wait indefinitely as we must ensure that if another thread
                // doing this operation for a lower index fails (interrupted) that
                // we would wake up and pick up any earlier element in the queue
                Object oRemoved = wa.safeRemove(lNext, cWait);
        
                if (oRemoved == null)
                    {
                    fRollback = !wa.isRemoved(lNext);
                    if (cMillis != 0)
                        {
                        // finite wait time specified
                        cMillis -= cWait;
                        if (cMillis <= 0)
                            {
                            // ran out of time
                            return null;
                            }
                        }
                    }
                else
                    {
                    fRollback = false;
                    if (getElementCounter().decrementAndGet() == 0)
                        {
                        // we just made the queue empty
                        onEmpty();
                        }
                    return oRemoved;
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw Base.ensureRuntimeException(e);
                }
            finally
                {
                if (fRollback)
                    {
                    // we were not able to remove lNext
                    rollbackNextIndex(lNext);
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
        return getNotifier() == null ? removeNoWaitMulti() : removeNoWaitSingle();
        }
    
    /**
     * Internal implementation of removeNoWait() optimized for multiple consumer
    * threads.
     */
    protected Object removeNoWaitMulti()
        {
        // import java.util.concurrent.atomic.AtomicLong;
        // import com.tangosol.util.Base;
        
        MultiQueue.WindowedArray wa         = getWindowedArray();
        AtomicLong     atomicNext = getNextRemoveableIndex();
        Object         oRemoved   = null;
        boolean        fRollback  = true;
        
        do
            {
            long lNext = atomicNext.getAndIncrement();
            try
                {
                oRemoved = wa.safeRemove(lNext);
                if (oRemoved == null)
                    {
                    if (lNext <= wa.getLastIndex())
                        {
                        // we can get here because the add for lNext is still in progress
                        // or if another thread removed lNext, which is unlikely but
                        // can happen; even if the next call returns null, we don't roll back
                        oRemoved = wa.safeRemove(lNext, -1L);
                        }
                    else
                        {
                        // queue was empty; we'll roll back
                        return null;
                        }
                    }
                fRollback = false;
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw Base.ensureRuntimeException(e);
                }
            finally
                {
                if (fRollback)
                    {
                    // we were not able to remove lNext
                    rollbackNextIndex(lNext);
                    }
                }
            }
        while (oRemoved == null);
        
        if (getElementCounter().decrementAndGet() == 0)
            {
            // we just made the queue empty
            onEmpty();
            }
        
        return oRemoved;
        }
    
    /**
     * Internal implementation of removeNoWait() optimized for a single consumer
    * thread.
     */
    protected Object removeNoWaitSingle()
        {
        // import com.tangosol.util.Base;
        // import java.util.concurrent.atomic.AtomicLong;
        
        MultiQueue.WindowedArray wa         = getWindowedArray();
        AtomicLong     atomicNext = getNextRemoveableIndex();
        Object         oRemoved   = wa.safeRemove(atomicNext.get());
        
        try
            {
            while (oRemoved == null)
                {
                // either lFirst is still being added or another thread removed it
                long lFirst = atomicNext.get();
                if (lFirst > wa.getLastIndex())
                    {
                    // queue was empty
                    return null;
                    }
        
                oRemoved = wa.safeRemove(lFirst, -1L);
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw Base.ensureRuntimeException(e);
            }
        
        atomicNext.incrementAndGet();
        
        if (getElementCounter().decrementAndGet() == 0)
            {
            // we just made the queue empty
            onEmpty();
            }
        
        return oRemoved;
        }
    
    /**
     * Ensure that NextRemoveableIndex is less than or equal to the specified
    * value.
     */
    protected void rollbackNextIndex(long iVirtual)
        {
        // import java.util.concurrent.atomic.AtomicLong;
        
        AtomicLong atomicNext = getNextRemoveableIndex();
        while (true)
            {
            long lCur = atomicNext.get();
            if (lCur <= iVirtual || atomicNext.compareAndSet(lCur, iVirtual))
                {
                return;
                }
            }
        }
    
    // Accessor for the property "NextRemoveableIndex"
    /**
     * Setter for property NextRemoveableIndex.<p>
    * An AtomicCounter identifying the next removable index in the
    * WindowedArray.  When in multi-notifier mode this will refer to the next
    * index to be removable without contending with other remove operations.
     */
    public void setNextRemoveableIndex(java.util.concurrent.atomic.AtomicLong pNextRemoveableIndex)
        {
        __m_NextRemoveableIndex = pNextRemoveableIndex;
        }
    
    // Accessor for the property "WindowedArray"
    /**
     * Setter for property WindowedArray.<p>
    * The WindowedArray that backs the Queue.
     */
    protected void setWindowedArray(MultiQueue.WindowedArray waElements)
        {
        __m_WindowedArray = waElements;
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
        // COH-8589
        // MultiQueue's element count can temporarily go negative, making ElementCounter
        // unsafe. it can go negative by up to the consumer thread count.  Concurrent
        // producers could then artificially push the negative count up to zero when there
        // are still element in the queue
        
        // the order in which we read these atomics is important as concurrent
        // inserts and removes may be occurring.  by reading nextRemove before
        // nextInsert we ensure that at worst we return an incorrectly large size
        // rather than an incorrectly small size, which could result in incorrectly
        // believing the queue to be empty when it is not (again COH-8589)
        // Note that in the case the value is incorrectly large, it is inflated by at most
        // the number of concurrent insertions made during this method call.
        long iNextRemove = getNextRemoveableIndex().get();
        long iNextInsert = getWindowedArray().getLastIndex() + 1L;
        return (int) (iNextInsert - iNextRemove);
        }
    
    // Declared at the super level
    public String toString()
        {
        StringBuilder sb = new StringBuilder(super.toString());
        
        sb.append(", next=")
          .append(getNextRemoveableIndex().get())
          .append(", storage array={")
          .append(getWindowedArray())
          .append('}');
        
        return sb.toString();
        }

    // ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.MultiQueue$WindowedArray
    
    /**
     * A WindowedArray is an object that has attributes of a queue and a
     * dynamically resizing array.
     * 
     * The "window" is the active, or visible, portion of the virtual array.
     * Only elements within the window may be accessed or removed.
     * 
     * As elements are added, they are added to the "end" or "top" of the
     * array, dynamically resizing if necessary, and adjusting the window so
     * that it includes the new elements.
     * 
     * As items are removed, if they are removed from the "start" or "bottom"
     * of the array, the window adjusts such that those elements are no longer
     * visible.
     * 
     * The concurrent version of the WindowedArray avoids contention for
     * threads accessing different virtual indices.
     * 
     * This is an abstract component, any concrete implementation must provide
     * assignIndexToValue and retrieveIndexFromValue methods.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static abstract class WindowedArray
            extends    com.tangosol.coherence.component.util.windowedArray.ConcurrentWindowedArray
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
            __mapChildren.put("PlaceHolder", MultiQueue.WindowedArray.PlaceHolder.get_CLASS());
            }
        
        // Initializing constructor
        public WindowedArray(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/MultiQueue$WindowedArray".replace('/', '.'));
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
         * Setter for property StatsWaits.<p>
        * The total number of times a thread "waited" for an element to arrive
        * in order to get or remove it.
         */
        public void setStatsWaits(long cWaits)
            {
            super.setStatsWaits(cWaits);
            }

        // ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.MultiQueue$WindowedArray$PlaceHolder
        
        /**
         * A PlaceHolder represents a value of null and is used to mark the
         * virtual index assigned to an actual index within the storage array.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class PlaceHolder
                extends    com.tangosol.coherence.component.util.windowedArray.ConcurrentWindowedArray.PlaceHolder
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public PlaceHolder()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public PlaceHolder(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setVirtualOffset(-1L);
                    }
                catch (java.lang.Exception e)
                    {
                    // re-throw as a runtime exception
                    throw new com.tangosol.util.WrapperException(e);
                    }
                
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
                return new com.tangosol.coherence.component.util.queue.concurrentQueue.MultiQueue.WindowedArray.PlaceHolder();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/MultiQueue$WindowedArray$PlaceHolder".replace('/', '.'));
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
