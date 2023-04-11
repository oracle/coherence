
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.queue.NullQueue

package com.tangosol.coherence.component.util.queue;

/**
 * The NullQueue is a Queue implementation with no capacity.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class NullQueue
        extends    com.tangosol.coherence.component.util.Queue
    {
    // ---- Fields declarations ----
    protected static com.tangosol.coherence.Component __singleton;
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
    public NullQueue()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public NullQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            throw new IllegalStateException("A singleton for \"NullQueue\" has already been set");
            }
        __singleton = this;
        
        // private initialization
        __initPrivate();
        
        // state initialization: public and protected properties
        try
            {
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
        com.tangosol.coherence.Component singleton = __singleton;
        
        if (singleton == null)
            {
            singleton = new com.tangosol.coherence.component.util.queue.NullQueue();
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
            clz = Class.forName("com.tangosol.coherence/component/util/queue/NullQueue".replace('/', '.'));
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
        return false;
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
        return true;
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
        return null;
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
        throw new UnsupportedOperationException();
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
        return null;
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
        return 0;
        }
    }
