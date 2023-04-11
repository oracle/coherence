
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.multiQueue.SimpleMultiQueue

package com.tangosol.coherence.component.util.queue.concurrentQueue.multiQueue;

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
 * 
 * A SimpleMultiQueue is a concrete implementation which uses wrapper objects
 * to maintain the assigned indices.  While this does generate garbage they are
 * also expected to be very short-lived and should not add significant GC
 * impact.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SimpleMultiQueue
        extends    com.tangosol.coherence.component.util.queue.concurrentQueue.MultiQueue
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
        __mapChildren.put("Entry", SimpleMultiQueue.Entry.get_CLASS());
        __mapChildren.put("Iterator", com.tangosol.coherence.component.util.Queue.Iterator.get_CLASS());
        }
    
    // Default constructor
    public SimpleMultiQueue()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SimpleMultiQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        _addChild(new SimpleMultiQueue.WindowedArray("WindowedArray", this, true), "WindowedArray");
        
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
        return new com.tangosol.coherence.component.util.queue.concurrentQueue.multiQueue.SimpleMultiQueue();
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
            clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/multiQueue/SimpleMultiQueue".replace('/', '.'));
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
        SimpleMultiQueue.Entry entry = new SimpleMultiQueue.Entry();
        entry.setValue(oElement);
        
        return super.add(entry);
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
        SimpleMultiQueue.Entry entry = (SimpleMultiQueue.Entry) super.peekNoWait();
        return entry == null ? null : entry.getValue();
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
        SimpleMultiQueue.Entry entry = (SimpleMultiQueue.Entry) super.remove(cMillis);
        return entry == null ? null : entry.getValue();
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
        SimpleMultiQueue.Entry entry = (SimpleMultiQueue.Entry) super.removeNoWait();
        return entry == null ? null : entry.getValue();
        }

    // ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.multiQueue.SimpleMultiQueue$Entry
    
    /**
     * A SimpleMultiQueue.Entry is a holder for elements added to the queue and
     * maintains the assigned index.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Entry
            extends    com.tangosol.coherence.Component
        {
        // ---- Fields declarations ----
        
        /**
         * Property Value
         *
         * The value associated with the Entry.
         */
        private Object __m_Value;
        
        /**
         * Property VirtualIndex
         *
         * The virtual index at which the Entry is stored.
         */
        private long __m_VirtualIndex;
        
        // Default constructor
        public Entry()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Entry(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setVirtualIndex(-1L);
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
            return new com.tangosol.coherence.component.util.queue.concurrentQueue.multiQueue.SimpleMultiQueue.Entry();
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
                clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/multiQueue/SimpleMultiQueue$Entry".replace('/', '.'));
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
        
        // Accessor for the property "Value"
        /**
         * Getter for property Value.<p>
        * The value associated with the Entry.
         */
        public Object getValue()
            {
            return __m_Value;
            }
        
        // Accessor for the property "VirtualIndex"
        /**
         * Getter for property VirtualIndex.<p>
        * The virtual index at which the Entry is stored.
         */
        public long getVirtualIndex()
            {
            return __m_VirtualIndex;
            }
        
        // Accessor for the property "Value"
        /**
         * Setter for property Value.<p>
        * The value associated with the Entry.
         */
        public void setValue(Object oValue)
            {
            __m_Value = oValue;
            }
        
        // Accessor for the property "VirtualIndex"
        /**
         * Setter for property VirtualIndex.<p>
        * The virtual index at which the Entry is stored.
         */
        public void setVirtualIndex(long lIndex)
            {
            __m_VirtualIndex = lIndex;
            }
        
        // Declared at the super level
        public String toString()
            {
            return "VirtualIndex=" + getVirtualIndex() + ", Value=" + getValue();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.multiQueue.SimpleMultiQueue$WindowedArray
    
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
    public static class WindowedArray
            extends    com.tangosol.coherence.component.util.queue.concurrentQueue.MultiQueue.WindowedArray
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
            __mapChildren.put("PlaceHolder", com.tangosol.coherence.component.util.queue.concurrentQueue.MultiQueue.WindowedArray.PlaceHolder.get_CLASS());
            }
        
        // Default constructor
        public WindowedArray()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public WindowedArray(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.queue.concurrentQueue.multiQueue.SimpleMultiQueue.WindowedArray();
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
                clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/multiQueue/SimpleMultiQueue$WindowedArray".replace('/', '.'));
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
         * Inform the value of its virtual index prior to adding it to the
        * array.  This method may be called for the same object multiple times
        * as part of the add or set operation.
        * 
        * This method may be called while synchronization is held on the
        * associated virtual index, as such the implementation must not make
        * use of any Base.getCommonMonitor() synchronization as this will
        * result in a deadlock.
         */
        protected void assignIndexToValue(long lVirtual, Object o)
            {
            ((SimpleMultiQueue.Entry) o).setVirtualIndex(lVirtual);
            }
        
        // Declared at the super level
        /**
         * Get the virtual index from the value.  An implementation of this
        * method must be aware of the datatype stored within the array and the
        * datatype must be able to supply the index from the value.  For
        * instance when Messages are used as values, the index is the
        * MessageId.
        * 
        * This method may be called while synchronization is held on the
        * associated virtual index, as such the implementation must not make
        * use of any Base.getCommonMonitor() synchronization as this will
        * result in a deadlock.
         */
        protected long retrieveIndexFromValue(Object o)
            {
            return ((SimpleMultiQueue.Entry) o).getVirtualIndex();
            }
        }
    }
