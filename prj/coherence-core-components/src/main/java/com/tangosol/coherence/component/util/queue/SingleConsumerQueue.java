
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.queue.SingleConsumerQueue

package com.tangosol.coherence.component.util.queue;

/**
 * SingleConsumerQueue is a concurrent queue optimized for multi producer
 * single consumer workloads.  More specifically it is not safe to consume from
 * multiple threads.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SingleConsumerQueue
        extends    com.tangosol.coherence.component.util.Queue
    {
    // ---- Fields declarations ----
    
    /**
     * Property AddCount
     *
     * An intentionally non-volatile count of the number of adds.
     */
    private transient int __m_AddCount;
    
    /**
     * Property BatchSize
     *
     * The interval at which to auto-flush the queue during an add operation. 
     * If the BatchSize is greater than one, the caller must externally call
     * flush() when it has finished adding elements in order to ensure that
     * they may be processed by any waiting consumer thread.
     */
    private transient int __m_BatchSize;
    
    /**
     * Property Delegate
     *
     * Delegate java based queue.
     */
    private com.oracle.coherence.common.collections.ConcurrentLinkedQueue __m_Delegate;
    
    /**
     * Property Notifier
     *
     * The notifier.
     */
    private transient com.oracle.coherence.common.base.Notifier __m_Notifier;
    
    // Default constructor
    public SingleConsumerQueue()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SingleConsumerQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setDelegate(new com.oracle.coherence.common.collections.ConcurrentLinkedQueue());
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
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
        return new com.tangosol.coherence.component.util.queue.SingleConsumerQueue();
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
            clz = Class.forName("com.tangosol.coherence/component/util/queue/SingleConsumerQueue".replace('/', '.'));
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
    public boolean add(Object oElement)
        {
        if (getDelegate().add(oElement))
            {
            int nBatchSize = getBatchSize();
            int cAdds      = getAddCount() + 1;
        
            setAddCount(cAdds);
        
            if (nBatchSize <= 1 || cAdds % nBatchSize == 0)
                {
                signal();
                }
        
            return true;
            }
        
        return false;
        }
    
    // Declared at the super level
    /**
     * Inserts  the specified element to the front of this queue.
    * 
    * @see #add
     */
    public boolean addHead(Object oElement)
        {
        throw new UnsupportedOperationException();
        }
    
    // Declared at the super level
    public void await(long cMillis)
            throws java.lang.InterruptedException
        {
        getNotifier().await(cMillis);
        }
    
    // Declared at the super level
    /**
     * Flush the queue.
     */
    public void flush()
        {
        if (getBatchSize() > 1 && !isEmpty())
            {
            signal();
            }
        }
    
    // Accessor for the property "AddCount"
    /**
     * Getter for property AddCount.<p>
    * An intentionally non-volatile count of the number of adds.
     */
    public int getAddCount()
        {
        return __m_AddCount;
        }
    
    // Accessor for the property "BatchSize"
    /**
     * Getter for property BatchSize.<p>
    * The interval at which to auto-flush the queue during an add operation. 
    * If the BatchSize is greater than one, the caller must externally call
    * flush() when it has finished adding elements in order to ensure that they
    * may be processed by any waiting consumer thread.
     */
    public int getBatchSize()
        {
        return __m_BatchSize;
        }
    
    // Accessor for the property "Delegate"
    /**
     * Getter for property Delegate.<p>
    * Delegate java based queue.
     */
    protected com.oracle.coherence.common.collections.ConcurrentLinkedQueue getDelegate()
        {
        return __m_Delegate;
        }
    
    // Accessor for the property "Notifier"
    /**
     * Getter for property Notifier.<p>
    * The notifier.
     */
    public com.oracle.coherence.common.base.Notifier getNotifier()
        {
        return __m_Notifier;
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
        return getDelegate().isEmpty();
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
        return getDelegate().iterator();
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
        return getDelegate().peek();
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
        return getDelegate().poll();
        }
    
    // Accessor for the property "AddCount"
    /**
     * Setter for property AddCount.<p>
    * An intentionally non-volatile count of the number of adds.
     */
    public void setAddCount(int nCount)
        {
        __m_AddCount = nCount;
        }
    
    // Accessor for the property "BatchSize"
    /**
     * Setter for property BatchSize.<p>
    * The interval at which to auto-flush the queue during an add operation. 
    * If the BatchSize is greater than one, the caller must externally call
    * flush() when it has finished adding elements in order to ensure that they
    * may be processed by any waiting consumer thread.
     */
    public void setBatchSize(int nInterval)
        {
        __m_BatchSize = nInterval;
        }
    
    // Accessor for the property "Delegate"
    /**
     * Setter for property Delegate.<p>
    * Delegate java based queue.
     */
    protected void setDelegate(com.oracle.coherence.common.collections.ConcurrentLinkedQueue queueDelegate)
        {
        __m_Delegate = queueDelegate;
        }
    
    // Accessor for the property "Notifier"
    /**
     * Setter for property Notifier.<p>
    * The notifier.
     */
    public void setNotifier(com.oracle.coherence.common.base.Notifier notifier)
        {
        __m_Notifier = notifier;
        }
    
    // Declared at the super level
    public void signal()
        {
        getNotifier().signal();
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
        return getDelegate().size();
        }
    }
