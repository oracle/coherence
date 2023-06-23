
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.Queue

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.Component;
import com.tangosol.util.Base;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The MessageQueue provides a means to efficiently (and in a thread-safe
 * manner) queue received messages and messages to be sent.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Queue
        extends    com.tangosol.coherence.component.Util
        implements com.oracle.coherence.common.base.Notifier
    {
    // ---- Fields declarations ----
    
    /**
     * Property ElementList
     *
     * The List that backs the Queue.
     * 
     * @volatile subclasses are allowed to change the value of ElementList over
     * time, and this property is accessed in unsynchronized methods, thus it
     * is volatile.
     */
    private volatile com.tangosol.util.RecyclingLinkedList __m_ElementList;
    
    /**
     * Property Signaled
     *
     * true if signal has been invoked, without having been consumed by await
     * 
     * @volatile to encourage avoiding sync/wait
     */
    private volatile transient boolean __m_Signaled;
    private static com.tangosol.util.ListMap __mapChildren;

    protected final ReentrantLock f_lock = new ReentrantLock();

    protected final Condition f_notEmpty = f_lock.newCondition();

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
        __mapChildren.put("Iterator", Queue.Iterator.get_CLASS());
        }
    
    // Default constructor
    public Queue()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Queue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.Queue();
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
            clz = Class.forName("com.tangosol.coherence/component/util/Queue".replace('/', '.'));
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
        lock();
        try
            {
            getElementList().add(oElement);

            // this Queue reference is waited on by the blocking remove
            // method; use signal to wakeup waiting threads
            signal();

            return true;
            }
        finally
            {
            unlock();
            }
        }
    
    /**
     * Inserts  the specified element to the front of this queue.
    * 
    * @see #add
     */
    public boolean addHead(Object oElement)
        {
        lock();
        try
            {
            getElementList().add(0, oElement);

            // see #add()
            signal();

            return true;
            }
        finally
            {
            unlock();
            }
        }
    
    // From interface: com.oracle.coherence.common.base.Notifier
    public void await()
            throws java.lang.InterruptedException
        {
        await(0L);
        }
    
    // From interface: com.oracle.coherence.common.base.Notifier
    public void await(long cMillis)
            throws java.lang.InterruptedException
        {
        // import com.oracle.coherence.common.base.Blocking;
        lock();
        try
            {
            if (!isAvailable() && !isSignaled()) // isAvailable is specialized by AssociatedQueue
                {
                f_notEmpty.await(cMillis, TimeUnit.MILLISECONDS);
                }
            setSignaled(false);
            }
        finally
            {
            unlock();
            }

        }
    
    /**
     * Flush the queue.
     */
    public void flush()
        {
        // no-op by default since all adds auto-flush
        }
    
    // Accessor for the property "ElementList"
    /**
     * Getter for property ElementList.
     * <p/>The List that backs the Queue.
     */
    protected com.tangosol.util.RecyclingLinkedList getElementList()
        {
        return __m_ElementList;
        }
    
    // Accessor for the property "Available"
    /**
     * Getter for property Available.<p>
    * True if there are items ready for removal.
     */
    public boolean isAvailable()
        {
        return !isEmpty();
        }
    
    /**
     * Determine if this Queue is empty.
     *
     * The result of this method may change after it is returned, unless the Queue is
     * {@link #lock() locked} before calling this method and the lock is held until the
     * operation based on the returned result is complete.
     *
     * @return {@code true} if this Queue is empty; {@code false} otherwise
     */
    public boolean isEmpty()
        {
        lock();
        try
            {
            return getElementList().isEmpty();
            }
        finally
            {
            unlock();
            }
        }
    
    // Accessor for the property "Signaled"
    /**
     * Getter for property Signaled.<p>
    * true if signal has been invoked, without having been consumed by await
    * 
    * @volatile to encourage avoiding sync/wait
     */
    protected boolean isSignaled()
        {
        return __m_Signaled;
        }
    
    /**
     * Provides an iterator over the elements in this Queue.
     * <p/>
     * The returned iterator is a point-in-time snapshot, and the contents of
     * the Queue may change after the iterator is returned, unless the Queue is
     * {@link #lock() locked} before calling iterator() and the lock is held
     * until the iterator is exhausted.
     *
     * @return an iterator of the elements in this Queue
     */
    public java.util.Iterator iterator()
        {
        lock();
        try
            {
            Queue.Iterator iter = (Queue.Iterator) _newChild("Iterator");
            iter.setList(getElementList());
            return iter;
            }
        finally
            {
            unlock();
            }
        }
    
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
        lock();
        try
            {
            return getElementList().getFirst();
            }
        finally
            {
            unlock();
            }
        }
    
    /**
     * Waits for and removes the first element from the front of this Queue.
    * 
    * If the Queue is empty, this method will block until an element is in the
    * Queue. The non-blocking equivalent of this method is "removeNoWait".
    * 
    * @return the first element in the front of this Queue
    * 
    * @see #removeNoWait
     */
    public Object remove()
        {
        return remove(0L);
        }
    
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
        // import com.tangosol.util.Base;

        lock();
        try
            {
            Object o = removeNoWait();
            while (o == null)
                {
                long cWait = cMillis <= 0L ? 1000L : Math.min(1000L, cMillis);

                try
                    {
                    await(cWait);
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw Base.ensureRuntimeException(e);
                    }

                o = removeNoWait();
                if (cMillis > 0L)
                    {
                    cMillis -= cWait;
                    if (cMillis <= 0L)
                        {
                        break;
                        }
                    }
                }
            return o;
            }
        finally
            {
            unlock();
            }
        }
    
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
        lock();
        try
            {
            return getElementList().removeFirst();
            }
        finally
            {
            unlock();
            }
        }
    
    // Accessor for the property "ElementList"

    /**
     * Setter for property ElementList.
     * <p/>The List that backs the Queue.
     */
    protected void setElementList(com.tangosol.util.RecyclingLinkedList list)
        {
        __m_ElementList = list;
        }
    
    // Accessor for the property "Signaled"
    /**
     * Setter for property Signaled.<p>
    * true if signal has been invoked, without having been consumed by await
    * 
    * @volatile to encourage avoiding sync/wait
     */
    protected void setSignaled(boolean fSignaled)
        {
        __m_Signaled = fSignaled;
        }
    
    // From interface: com.oracle.coherence.common.base.Notifier
    public void signal()
        {
        lock();
        try
            {
            setSignaled(true);
            f_notEmpty.signalAll();
            }
        finally
            {
            unlock();
            }
        }

    /**
     * Determine the number of elements in this Queue.
     *
     * The size of the Queue may change after it is returned from this method,
     * unless the Queue is {@link #lock() locked} before calling {@code size()}
     * and the lock is held until the operation based on this size result is complete.
     *
     * @return the number of elements in this Queue
     */
    public int size()
        {
        lock();
        try
            {
            return getElementList().size();
            }
        finally
            {
            unlock();
            }
        }

    /**
     * Lock this Queue.
     */
    public void lock()
        {
        f_lock.lock();
        }

    /**
     * Unlock this Queue.
     */
    public void unlock()
        {
        f_lock.unlock();
        }

    // ---- class: com.tangosol.coherence.component.util.Queue$Iterator
    
    /**
     * Iterator of a snapshot of the List object that backs the Queue. Supports
     * remove(). Uses the Queue as the monitor if any synchronization is
     * required.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Iterator
            extends    com.tangosol.coherence.component.util.Iterator
        {
        // ---- Fields declarations ----
        
        /**
         * Property List
         *
         * The List object that this Iterator is providing an Iterator over.
         * The contents of the Iterator are obtained as a snapshot of the List
         * contents (using toArray), and the List is used to implement the
         * remove method of the Iterator.
         * 
         * @see #remove
         * @see #setList
         */
        private java.util.List __m_List;
        
        /**
         * Property RemoveCount
         *
         * The number of items removed so far during this iteration. Used to
         * optimize removal from the linked list.
         * 
         * @see #remove
         */
        private int __m_RemoveCount;
        
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
            return new com.tangosol.coherence.component.util.Queue.Iterator();
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
                clz = Class.forName("com.tangosol.coherence/component/util/Queue$Iterator".replace('/', '.'));
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
        
        // Accessor for the property "List"
        /**
         * Getter for property List.<p>
        * The List object that this Iterator is providing an Iterator over. The
        * contents of the Iterator are obtained as a snapshot of the List
        * contents (using toArray), and the List is used to implement the
        * remove method of the Iterator.
        * 
        * @see #remove
        * @see #setList
         */
        protected java.util.List getList()
            {
            return __m_List;
            }
        
        // Accessor for the property "RemoveCount"
        /**
         * Getter for property RemoveCount.<p>
        * The number of items removed so far during this iteration. Used to
        * optimize removal from the linked list.
        * 
        * @see #remove
         */
        protected int getRemoveCount()
            {
            return __m_RemoveCount;
            }
        
        // Declared at the super level
        /**
         * Removes from the underlying collection the last element returned by
        * the iterator (optional operation).  This method can be called only
        * once per call to <tt>next</tt>.  The behavior of an iterator is
        * unspecified if the underlying collection is modified while the
        * iteration is in progress in any way other than by calling this
        * method.
        * 
        * @exception UnsupportedOperationException if the <tt>remove</tt>
        * operation is not supported by this Iterator
        * @exception IllegalStateException if the <tt>next</tt> method has not
        * yet been called, or the <tt>remove</tt> method has already been
        * called after the last call to the <tt>next</tt> method
        * 
        * @exception ConcurrentModificationException if the element has already
        * been removed from the List object
         */
        public void remove()
            {
            // import java.util.List;
            // import java.util.ConcurrentModificationException;
            
            if (isCanRemove())
                {
                setCanRemove(false);
            
                int    cRemoved = getRemoveCount();
                int    iIndex   = getNextIndex() - 1;
                Object oElement = getItem(iIndex);
                int    iGuess   = iIndex - cRemoved;
                List   list     = getList();

                Queue queue = (Queue) get_Parent();
                queue.lock();
                try
                    {
                    if (list.get(iGuess) == oElement)
                        {
                        list.remove(iGuess);
                        }
                    else
                        {
                        if (!list.remove(oElement))
                            {
                            throw new ConcurrentModificationException();
                            }
                        }
                    }
                finally
                    {
                    queue.unlock();
                    }
                
                setRemoveCount(cRemoved + 1);
                }
            else
                {
                throw new IllegalStateException();
                }
            }
        
        // Accessor for the property "List"
        /**
         * Setter for property List.<p>
        * The List object that this Iterator is providing an Iterator over. The
        * contents of the Iterator are obtained as a snapshot of the List
        * contents (using toArray), and the List is used to implement the
        * remove method of the Iterator.
        * 
        * @see #remove
        * @see #setList
         */
        public void setList(java.util.List list)
            {
            _assert(list != null);
            _assert(getList() == null);
            
            __m_List = (list);

            Queue queue = (Queue) get_Parent();
            queue.lock();
            try
                {
                setItem(list.toArray());
                }
            finally
                {
                queue.unlock();
                }
            }
        
        // Accessor for the property "RemoveCount"
        /**
         * Setter for property RemoveCount.<p>
        * The number of items removed so far during this iteration. Used to
        * optimize removal from the linked list.
        * 
        * @see #remove
         */
        protected void setRemoveCount(int pRemoveCount)
            {
            __m_RemoveCount = pRemoveCount;
            }
        }
    }
