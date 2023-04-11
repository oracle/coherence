
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.queue.ConcurrentQueue

package com.tangosol.coherence.component.util.queue;

import com.oracle.coherence.common.base.Notifier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The ConcurrentQueue provides a means to efficiently (and in a thread-safe
 * manner) queue elements with minimal contention.
 * 
 * Note: The ConcurrentQueue does not support null entries.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ConcurrentQueue
        extends    com.tangosol.coherence.component.util.Queue
    {
    // ---- Fields declarations ----
    
    /**
     * Property AtomicFlushState
     *
     * The AtomicLong used to maintain the FlushState.  See getFlushState() and
     * setFlushState() helper methods.
     */
    private transient java.util.concurrent.atomic.AtomicInteger __m_AtomicFlushState;
    
    /**
     * Property BatchSize
     *
     * The queue size at which to auto-flush the queue during an add operation.
     *  If the BatchSize is greater than one, the caller must externally call
     * flush() when it has finished adding elements in order to ensure that
     * they may be processed by any waiting consumer thread.
     */
    private transient int __m_BatchSize;
    
    /**
     * Property ElementCounter
     *
     * A counter for maintaining the size of the queue.
     */
    private java.util.concurrent.atomic.AtomicInteger __m_ElementCounter;
    
    /**
     * Property FLUSH_AUTO
     *
     * State indicating that no flush is pending as the queue has been auto
     * flushed.
     */
    public static final int FLUSH_AUTO = 1;
    
    /**
     * Property FLUSH_EXPLICIT
     *
     * State indicating that no flush is pending as the queue has been
     * explicitly flushed.
     */
    public static final int FLUSH_EXPLICIT = 2;
    
    /**
     * Property FLUSH_PENDING
     *
     * State indicating that a flush is pending.
     */
    public static final int FLUSH_PENDING = 0;
    
    /**
     * Property Notifier
     *
     * The monitor on which notifications related to a queue addition will be
     * performed.  The default value is the Queue itself.  The Notifier should
     * not be changed while the queue is in use.  If the Notifier is null then
     * notification will be disabled.
     */
    private com.oracle.coherence.common.base.Notifier __m_Notifier;
    
    /**
     * Property StatsEmptied
     *
     * The total number of times the queue transitioned to the empty state.
     */
    private long __m_StatsEmptied;
    
    /**
     * Property StatsFlushed
     *
     * The total number of times the queue has been flushed.
     */
    private long __m_StatsFlushed;
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
    public ConcurrentQueue()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ConcurrentQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.queue.ConcurrentQueue();
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
            clz = Class.forName("com.tangosol.coherence/component/util/queue/ConcurrentQueue".replace('/', '.'));
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
            throw new IllegalArgumentException("The ConcurrentQueue does not support null values.");
            }
        
        getElementList().add(oElement);
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
        if (oElement == null)
            {
            throw new IllegalArgumentException("The ConcurrentQueue does not support null values.");
            }
        
        getElementList().add(0, oElement);
        onAddElement();
        return true;
        }
    
    // Declared at the super level
    public void await(long cMillis)
            throws java.lang.InterruptedException
        {
        // import com.oracle.coherence.common.base.Notifier;
        
        Notifier notifier = getNotifier();
        if (notifier == null)
            {
            throw new UnsupportedOperationException("blocking remove without a notifier");
            }
        else if (!isAvailable())
            {
            if (notifier == this)
                {
                super.await(cMillis);
                }
            else
                {
                notifier.await(cMillis);        
                }
            }
        }
    
    /**
     * Check whether the flush (notify) is necessary. This method is always
    * called when a new item is added to the queue.
    * 
    * @param cElement the number of elements in the queue after the addition
     */
    protected void checkFlush(int cElements)
        {
        if (getNotifier() != null)
            {
            int nFlushState;
            if (cElements == 1)
                {
                // queue was previously empty
                nFlushState = FLUSH_PENDING;
                updateFlushState(nFlushState);
                }
            else
                {
                nFlushState = getFlushState();
                }
        
            switch (nFlushState)
                {
                case FLUSH_PENDING:
                    if (cElements % getBatchSize() == 0)
                        {
                        flush(true);
                        }
                    break;
        
                case FLUSH_EXPLICIT:
                    // producer has started adding again before consumer fully drained,
                    // exit the FLUSH_EXPLICT state as that may be used as an indication
                    // that no more data is coming
                    updateFlushStateConditionally(FLUSH_EXPLICIT, FLUSH_AUTO);
                    break;
        
                case FLUSH_AUTO:
                    // noop
                    break;
                }
            }
        }
    
    // Declared at the super level
    /**
     * Flush the queue.
     */
    public void flush()
        {
        flush(false);
        }
    
    /**
     * Flush the queue.
    * 
    * @param fAuto iff the flush was invoked automatically based on the
    * notification batch size
     */
    protected void flush(boolean fAuto)
        {
        // The ConcurrentQueue implementation is optimized for a single producer [thread] -
        // single consumer [thread] model; it only notifies the consumer while transitioning
        // from an empty to non-empty state. However, to be correct in a case of multiple
        // consumers, it has to use the notifyAll() call.
        
        if (updateFlushState(fAuto ? FLUSH_AUTO : FLUSH_EXPLICIT) == FLUSH_PENDING)
            {
            // transitioned from FLUSH_PENDING
            setStatsFlushed(getStatsFlushed() + 1L);
            signal();
            }
        }
    
    // Accessor for the property "AtomicFlushState"
    /**
     * Getter for property AtomicFlushState.<p>
    * The AtomicLong used to maintain the FlushState.  See getFlushState() and
    * setFlushState() helper methods.
     */
    protected java.util.concurrent.atomic.AtomicInteger getAtomicFlushState()
        {
        return __m_AtomicFlushState;
        }
    
    // Accessor for the property "BatchSize"
    /**
     * Getter for property BatchSize.<p>
    * The queue size at which to auto-flush the queue during an add operation. 
    * If the BatchSize is greater than one, the caller must externally call
    * flush() when it has finished adding elements in order to ensure that they
    * may be processed by any waiting consumer thread.
     */
    public int getBatchSize()
        {
        return __m_BatchSize;
        }
    
    // Accessor for the property "ElementCounter"
    /**
     * Getter for property ElementCounter.<p>
    * A counter for maintaining the size of the queue.
     */
    public java.util.concurrent.atomic.AtomicInteger getElementCounter()
        {
        return __m_ElementCounter;
        }
    
    /**
     * Return the current flush state.
     */
    public int getFlushState()
        {
        return getAtomicFlushState().get();
        }
    
    // Accessor for the property "Notifier"
    /**
     * Getter for property Notifier.<p>
    * The monitor on which notifications related to a queue addition will be
    * performed.  The default value is the Queue itself.  The Notifier should
    * not be changed while the queue is in use.  If the Notifier is null then
    * notification will be disabled.
     */
    public com.oracle.coherence.common.base.Notifier getNotifier()
        {
        return __m_Notifier;
        }
    
    // Accessor for the property "StatsEmptied"
    /**
     * Getter for property StatsEmptied.<p>
    * The total number of times the queue transitioned to the empty state.
     */
    public long getStatsEmptied()
        {
        return __m_StatsEmptied;
        }
    
    // Accessor for the property "StatsFlushed"
    /**
     * Getter for property StatsFlushed.<p>
    * The total number of times the queue has been flushed.
     */
    public long getStatsFlushed()
        {
        return __m_StatsFlushed;
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
    
    // Accessor for the property "FlushPending"
    /**
     * Getter for property FlushPending.<p>
    * (Calculated) Helper property indicating that there is a pending flush.
     */
    public boolean isFlushPending()
        {
        return getFlushState() == FLUSH_PENDING;
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
    
    /**
     * Called each time an element is added to the queue.
     */
    protected void onAddElement()
        {
        checkFlush(getElementCounter().incrementAndGet());
        }
    
    /**
     * Called when the queue becomes empty.  setFlushPending(true) can be
    * monitored to track when a queue leaves the empty state.
     */
    protected void onEmpty()
        {
        setStatsEmptied(getStatsEmptied() + 1L);
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
        // import java.util.concurrent.atomic.AtomicInteger;
        
        setElementCounter(new AtomicInteger());
        setAtomicFlushState(new AtomicInteger(FLUSH_PENDING));
        setNotifier(this);
        
        super.onInit();
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
        
        AtomicInteger counter = getElementCounter();
        if (counter.get() == 0)
            {
            return null;
            }
        
        Object oEntry = getElementList().removeFirst();
        if (oEntry != null && counter.decrementAndGet() == 0)
            {
            onEmpty();
            }
        return oEntry;
        }
    
    // Accessor for the property "AtomicFlushState"
    /**
     * Setter for property AtomicFlushState.<p>
    * The AtomicLong used to maintain the FlushState.  See getFlushState() and
    * setFlushState() helper methods.
     */
    protected void setAtomicFlushState(java.util.concurrent.atomic.AtomicInteger atomicState)
        {
        _assert(getAtomicFlushState() == null);
        __m_AtomicFlushState = (atomicState);
        }
    
    // Accessor for the property "BatchSize"
    /**
     * Setter for property BatchSize.<p>
    * The queue size at which to auto-flush the queue during an add operation. 
    * If the BatchSize is greater than one, the caller must externally call
    * flush() when it has finished adding elements in order to ensure that they
    * may be processed by any waiting consumer thread.
     */
    public void setBatchSize(int cBatch)
        {
        if (cBatch > 0)
            {
            __m_BatchSize = (cBatch);
            }
        }
    
    // Accessor for the property "ElementCounter"
    /**
     * Setter for property ElementCounter.<p>
    * A counter for maintaining the size of the queue.
     */
    protected void setElementCounter(java.util.concurrent.atomic.AtomicInteger counter)
        {
        __m_ElementCounter = counter;
        }
    
    // Accessor for the property "Notifier"
    /**
     * Setter for property Notifier.<p>
    * The monitor on which notifications related to a queue addition will be
    * performed.  The default value is the Queue itself.  The Notifier should
    * not be changed while the queue is in use.  If the Notifier is null then
    * notification will be disabled.
     */
    public void setNotifier(com.oracle.coherence.common.base.Notifier notifier)
        {
        __m_Notifier = notifier;
        }
    
    // Accessor for the property "StatsEmptied"
    /**
     * Setter for property StatsEmptied.<p>
    * The total number of times the queue transitioned to the empty state.
     */
    protected void setStatsEmptied(long cEmptied)
        {
        __m_StatsEmptied = cEmptied;
        }
    
    // Accessor for the property "StatsFlushed"
    /**
     * Setter for property StatsFlushed.<p>
    * The total number of times the queue has been flushed.
     */
    protected void setStatsFlushed(long cFlush)
        {
        __m_StatsFlushed = cFlush;
        }
    
    // Declared at the super level
    public void signal()
        {
        // import com.oracle.coherence.common.base.Notifier;
        
        Notifier notifier = getNotifier();
        if (notifier == this)
            {
            super.signal();
            }
        else if (notifier != null)
            {
            notifier.signal();
            }
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
        return getElementCounter().get();
        }
    
    // Declared at the super level
    public String toString()
        {
        StringBuilder sb = new StringBuilder();
        
        sb.append(get_Name())
          .append(" size=")
          .append(size())
          .append(", emptied=")
          .append(getStatsEmptied())  
          .append(", flushed=")
          .append(getStatsFlushed())
          .append(", first=")
          .append(peekNoWait());
        
        return sb.toString();
        }
    
    /**
     * Set the flush state and return the previous state.
     */
    protected int updateFlushState(int nState)
        {
        return getAtomicFlushState().getAndSet(nState);
        }
    
    /**
     * Set the flush state iff the assumed state is correct, return true if
    * change was made.
     */
    protected boolean updateFlushStateConditionally(int nStateAssumed, int nStateNew)
        {
        return getAtomicFlushState().compareAndSet(nStateAssumed, nStateNew);
        }
    }
