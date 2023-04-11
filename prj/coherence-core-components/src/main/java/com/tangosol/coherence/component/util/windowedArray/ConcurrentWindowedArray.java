
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.windowedArray.ConcurrentWindowedArray

package com.tangosol.coherence.component.util.windowedArray;

import com.oracle.coherence.common.base.Blocking;
import com.tangosol.util.Base;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A WindowedArray is an object that has attributes of a queue and a
 * dynamically resizing array.
 * 
 * The "window" is the active, or visible, portion of the virtual array. Only
 * elements within the window may be accessed or removed.
 * 
 * As elements are added, they are added to the "end" or "top" of the array,
 * dynamically resizing if necessary, and adjusting the window so that it
 * includes the new elements.
 * 
 * As items are removed, if they are removed from the "start" or "bottom" of
 * the array, the window adjusts such that those elements are no longer
 * visible.
 * 
 * The concurrent version of of the WindowedArray avoids contention for threads
 * accessing different virtual indices.
 * 
 * This is an abstract component, any concrete implementation must provide
 * assignIndexToValue and retrieveIndexFromValue methods.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class ConcurrentWindowedArray
        extends    com.tangosol.coherence.component.util.WindowedArray
    {
    // ---- Fields declarations ----
    
    /**
     * Property AssumedFirstIndex
     *
     * The virtual index which is assumed to be the "first" index.  This value
     * is guaranteed to be <= to the real first index.
     * 
     * @volatile
     */
    private volatile long __m_AssumedFirstIndex;
    
    /**
     * Property AtomicLastIndex
     *
     * The AtomicCounter representing the LastIndex.
     */
    private java.util.concurrent.atomic.AtomicLong __m_AtomicLastIndex;
    
    /**
     * Property LockOffset
     *
     * The offset into the array of common monitors.
     */
    private transient long __m_LockOffset;
    
    /**
     * Property RecentPlaceHolders
     *
     * An array of recently allocated PlaceHolder objects.
     */
    private ConcurrentWindowedArray.PlaceHolder[] __m_RecentPlaceHolders;
    
    /**
     * Property StatsGetsOptimistic
     *
     * The total number of optimistic gets which returned a non-null result.
     */
    private long __m_StatsGetsOptimistic;
    
    /**
     * Property StatsPlaceHolderAllocations
     *
     * The total number of PlaceHolder objects which have been allocated over
     * time.
     */
    private long __m_StatsPlaceHolderAllocations;
    
    /**
     * Property StatsWaits
     *
     * The total number of times a thread "waited" for an element to arrive in
     * order to get or remove it.
     */
    private long __m_StatsWaits;
    
    /**
     * Property WaitingThreadCount
     *
     * The number of threads waiting to indices to be set.
     */
    private java.util.concurrent.atomic.AtomicLong __m_WaitingThreadCount;
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
        __mapChildren.put("PlaceHolder", ConcurrentWindowedArray.PlaceHolder.get_CLASS());
        }
    
    // Initializing constructor
    public ConcurrentWindowedArray(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/windowedArray/ConcurrentWindowedArray".replace('/', '.'));
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
     * Add an Object to the WindowedArray and return the index of the new
    * element.  The value's internal index will be updated prior to storing it
    * in the array, see assignIndexToValue.
    * 
    * @param o  the Object to add to the end of the WindowedArray
    * 
    * @return the index of the element that was added
    * 
    * @throws IndexOutOfBoundsException if the WindowedArray cannot add the
    * element because the MaximumCapacity has already been reached
     */
    public long add(Object o)
        {
        long iVirtual = getAtomicLastIndex().incrementAndGet();
        
        setInternal(iVirtual, o);
        
        return iVirtual;
        }
    
    /**
     * Inform the value of its virtual index prior to adding it to the array. 
    * This method may be called for the same object multiple times as part of
    * the add or set operation.
    * 
    * This method may be called while synchronization is held on the associated
    * virtual index, as such the implementation must not make use of any
    * Base.getCommonMonitor() synchronization as this will result in a deadlock.
     */
    protected void assignIndexToValue(long lVirtual, Object o)
        {
        }
    
    /**
     * Ensure that the LastIndex is at least the specified index.
     */
    protected void ensureLastIndexMinimum(long lVirtual)
        {
        // import java.util.concurrent.atomic.AtomicLong;
        
        AtomicLong atomicLast = getAtomicLastIndex();
        while (true)
            {
            long lvLast = atomicLast.get();
            if (lvLast >= lVirtual || atomicLast.compareAndSet(lvLast, lVirtual))
                {
                return;
                }
            }
        }
    
    // Declared at the super level
    /**
     * Return a status report as a string.
     */
    public String formatStats()
        {
        StringBuilder sb = new StringBuilder(super.formatStats());
        sb.append(", optimistic gets=")
          .append(getStatsGetsOptimistic())
          .append(", place holder allocations=")
          .append(getStatsPlaceHolderAllocations())
          .append(", waits=")
          .append(getStatsWaits())
          .append(", waiting threads=")
          .append(getWaitingThreadCount().get());
        
        return sb.toString();
        }
    
    // Declared at the super level
    /**
     * Obtain an Object from the WindowedArray at the specified index.
    * 
    * @param lVirtual  the index of the element to obtain
    * 
    * @return the Object at the requested index, or null if the requested index
    * is outside the bounds of the WindowedArray
    * 
    * @throws IndexOutOfBoundsException if the index is negative
     */
    public Object get(long lVirtual)
        {
        Object[] aoStore = getStableStore();
        int      iActual = (int) (lVirtual % aoStore.length);
        Object   oValue;
        
        synchronized (getIndexLock(lVirtual))
            {
            oValue = aoStore[iActual];
            }
        
        return safeRetrieveIndexFromValue(oValue, iActual) == lVirtual &&
               !(oValue instanceof ConcurrentWindowedArray.PlaceHolder) ? oValue : null;
        }
    
    /**
     * Obtain an Object from the WindowedArray at the specified index. 
    * Initially an optimistic attempt will be made to find all the value, if it
    * is found then this optimistic result will be returned, if the value is
    * are not found (null) then a non-optimistic attempt will be made.  If is
    * expected that the value may not exist in the array, and it is allowable
    * to have a miss then consider calling #optimisticGet() instead.
    * 
    * @param i  the index of the element to obtain
    * @param cMillis  the number of milliseconds to wait for a non-null value
    * to be available.  A value of -1 specifies an indefinite wait, a value of
    * 0 specifies not to wait
    * 
    * @return the Object at the requested index, or null if the requested index
    * has been removed or the timeout has expired
    * 
    * @throws IndexOutOfBoundsException if the index is negative
     */
    public Object get(long lVirtual, long cMillis)
            throws java.lang.InterruptedException
        {
        long ldtTimeout = cMillis > 0L ? System.currentTimeMillis() + cMillis : 0L;
        while (true)
            {
            Object[] aoStore = getStableStore();
            int      iActual = (int) (lVirtual % aoStore.length);
            Object   oLock   = getIndexLock(lVirtual);
        
            synchronized (oLock)
                {
                Object oValue  = aoStore[iActual];
                long   lvFound = safeRetrieveIndexFromValue(oValue, iActual);
        
                if (lvFound > lVirtual)
                    {
                    // lVirtual was already removed
                    return null;
                    }
                else if (lvFound < lVirtual || oValue instanceof ConcurrentWindowedArray.PlaceHolder)
                    {
                    if (cMillis == 0L)
                        {
                        // not there and non-blocking
                        return null;
                        }
        
                    long cMillisLeft = waitIndex(oLock, ldtTimeout);
                    if (cMillis > 0L && (cMillisLeft <= 0L || cMillisLeft > cMillis))
                        {
                        // timeout expired, or clock moved back
                        return null;
                        }
                    }
                else
                    {
                    // found it, and it's not null
                    return oValue;
                    }
                }
            }
        }
    
    // Declared at the super level
    /**
     * Obtain a set of Objects from the WindowedArray at the specified indices. 
    * Note that while the returned Object for each index is guaranteed to be
    * "current" at the time it was fetched, there is no such guarantee across
    * the multiple items returned.  Thus, it is possible that the returned
    * values for i and i+1 never existed in the array at the same time.
    * 
    * @param aIndex  the array of indices to obtain
    * @param aObject the array in which to insert the values
    * @param cEntries the number of indices to process, thus allowing longer
    * arrays then necessary to be supplied
    * 
    * @throws IndexOutOfBoundsException if any of the indices is negative
    * 
    * @return the number of items found, previously removed items will not be
    * counted.
     */
    public int getAll(long[] alIndex, int cEntries, Object[] aoResult)
        {
        Object[] aoStore   = getStableStore();
        int      cCapacity = aoStore.length;
        int      cFound    = 0;
        
        for (int iReq = 0; iReq < cEntries; ++iReq)
            {
            long   lVirtual = alIndex[iReq];
            int    iActual  = (int) (lVirtual % cCapacity);
            Object oValue;
        
            synchronized (getIndexLock(lVirtual))
                {
                oValue = aoStore[iActual];
                }
        
            if (safeRetrieveIndexFromValue(oValue, iActual) == lVirtual &&
                    !(oValue instanceof ConcurrentWindowedArray.PlaceHolder))
                {
                ++cFound;
                aoResult[iReq] = oValue;
                }
            else
                {
                aoResult[iReq] = null;
                }
            }
        
        return cFound;
        }
    
    // Accessor for the property "AssumedFirstIndex"
    /**
     * Getter for property AssumedFirstIndex.<p>
    * The virtual index which is assumed to be the "first" index.  This value
    * is guaranteed to be <= to the real first index.
    * 
    * @volatile
     */
    protected long getAssumedFirstIndex()
        {
        return __m_AssumedFirstIndex;
        }
    
    // Accessor for the property "AtomicLastIndex"
    /**
     * Getter for property AtomicLastIndex.<p>
    * The AtomicCounter representing the LastIndex.
     */
    protected java.util.concurrent.atomic.AtomicLong getAtomicLastIndex()
        {
        return __m_AtomicLastIndex;
        }
    
    // Declared at the super level
    /**
     * Getter for property FirstIndex.<p>
    * The FirstIndex property provides the lowest [virtual] index into the
    * WindowedArray that can be set and which may return a non-null value. As
    * elements are removed from the front of the WindowedArray, the FirstIndex
    * property increases. The relationship between FirstIndex and LastIndex and
    * Size is as follows: A WindowedArray of n elements of which the first m
    * elements have been removed has a FirstIndex of m, a LastIndex of n-1, and
    * a Size of n.
    * 
    * Note: Within the implementation virtual indices are prefixed with "lv"
    * (long virtual).
    * 
    * For the ConcurrentWindowedArray this is a calculated property, which
    * obtains index synchronization.  Thus, it is somewhat expensive to call,
    * and cannot be called while holding any other index lock.  Its internal
    * use is restricted to growth operations.
     */
    public long getFirstIndex()
        {
        // For the ConcurrentWindowedArray the FirstIndex property is calculated rather
        // than stored. The rationale for this is that accurately maintaining the value
        // requires the remove() operation to do a forward scan after removal at
        // FirstIndex to find the new FirstIndex.  When the forward scan finds the new
        // FirstIndex value it must synchronize on that index to safely update the FirstIndex.
        // The problem with this is that a series of threads removing lvFirst ... lvFirst+N
        // will end up frequently contending as each new FirstIndex contends with its next.
        //
        // Instead, remove() does an optimistic update to AssumedFirstIndex and leaves
        // the remainder of the work for this method. The assumption is that a user of
        // the ConcurrentWindowedArray generally will not care about the first index,
        // and will not call this method, avoiding the cost altogether.
        //
        // One example where the caller does want to know the FirstIndex is MultiQueue,
        // but it maintains its own FirstIndex using an AtomicCounter which it is able
        // to update without the forward scan as it controls the order of removal.
        
        long     lvAssumed = getAssumedFirstIndex();
        Object[] aoStore   = getStableStore();
        int      cCapacity = aoStore.length;
        
        for (long lVirtual = lvAssumed; true; ++lVirtual)
            {
            int iActual = (int) (lVirtual % cCapacity);
            if (safeRetrieveIndexFromValue(aoStore[iActual], iActual) <= lVirtual)
                {
                // dirty read (memory model) indicates that this may be the first
                synchronized (getIndexLock(lVirtual))
                    {
                    // clean read validates if it is
                    long lvFound = safeRetrieveIndexFromValue(aoStore[iActual], iActual);
                    if (lvFound == lVirtual)
                        {
                        if (lVirtual != lvAssumed)
                            {
                            setAssumedFirstIndex(lVirtual);
                            }
                        return lVirtual;
                        }
                    else if (lvFound < lVirtual)
                        {
                        // somehow AssumedFirst passed the real first
                        throw new IllegalStateException(lvFound + ", " + lVirtual);
                        }
                    // else lvFound > lVirtual, lVirtual was removed, keep scanning
                    }
                }
            }
        }
    
    /**
     * Return the lock associated with the specified virtual index.  While
    * holding the lock the caller may not synchronize on the WindowedArray,
    * though synchronization in the other order is allowable.
     */
    protected Object getIndexLock(long lVirtual)
        {
        // import com.tangosol.util.Base;
        
        return Base.getCommonMonitor(getLockOffset() + lVirtual);
        }
    
    // Declared at the super level
    /**
     * Getter for property LastIndex.<p>
    * The LastIndex property provides the current highest legal index into the
    * WindowedArray. As elements are added to the WindowedArray, the LastIndex
    * property increases. The relationship between FirstIndex and LastIndex and
    * Size is as follows: A WindowedArray of n elements of which the first m
    * elements have been removed has a FirstIndex of m, a LastIndex of n-1, and
    * a Size of n.
     */
    public long getLastIndex()
        {
        return getAtomicLastIndex().get();
        }
    
    // Accessor for the property "LockOffset"
    /**
     * Getter for property LockOffset.<p>
    * The offset into the array of common monitors.
     */
    protected long getLockOffset()
        {
        return __m_LockOffset;
        }
    
    /**
     * Return a PlaceHolder for the specified virtual offset.
     */
    protected Object getPlaceHolder(long lOffset)
        {
        // check if one of the recent PlaceHolders matches
        ConcurrentWindowedArray.PlaceHolder[] aHolders = getRecentPlaceHolders();
        
        int  iOldest       = 0;
        long lOldestOffset = Long.MAX_VALUE;
        
        for (int i = 0, c = aHolders.length; i < c; ++i)
            {
            ConcurrentWindowedArray.PlaceHolder holder = aHolders[i];
            if (holder == null)
                {
                lOldestOffset = -1L;
                iOldest       = i;
                }
            else
                {
                long lFoundOffset = holder.getVirtualOffset();
                if (lFoundOffset == lOffset)
                    {
                    return holder;
                    }
        
                if (lFoundOffset < lOldestOffset)
                    {
                    lOldestOffset = lFoundOffset;
                    iOldest       = i;
                    }
                }
            }
        
        if (lOffset < 0L)
            {
            throw new IllegalStateException();
            }
        
        // no usable PlaceHolder found, allocate
        ConcurrentWindowedArray.PlaceHolder holder = new ConcurrentWindowedArray.PlaceHolder();
        holder.setVirtualOffset(lOffset);
        
        // replace oldest
        aHolders[iOldest] = holder;
        
        setStatsPlaceHolderAllocations(getStatsPlaceHolderAllocations() + 1L);
        
        return holder;
        }
    
    // Accessor for the property "RecentPlaceHolders"
    /**
     * Getter for property RecentPlaceHolders.<p>
    * An array of recently allocated PlaceHolder objects.
     */
    protected ConcurrentWindowedArray.PlaceHolder[] getRecentPlaceHolders()
        {
        return __m_RecentPlaceHolders;
        }
    
    /**
     * Return a stable (non-null) reference to the storage array.  This call may
    * block while the storage array is growing.
     */
    protected Object[] getStableStore()
        {
        Object[] aoStore = getStore();
        
        if (aoStore == null)
            {
            // another thread is growing the array
            synchronized (this)
                {
                // for us to get the synchronization they will have finished resizing
                aoStore = getStore();
                }
            _assert(aoStore != null);
            }
        
        return aoStore;
        }
    
    // Accessor for the property "StatsGetsOptimistic"
    /**
     * Getter for property StatsGetsOptimistic.<p>
    * The total number of optimistic gets which returned a non-null result.
     */
    public long getStatsGetsOptimistic()
        {
        return __m_StatsGetsOptimistic;
        }
    
    // Accessor for the property "StatsPlaceHolderAllocations"
    /**
     * Getter for property StatsPlaceHolderAllocations.<p>
    * The total number of PlaceHolder objects which have been allocated over
    * time.
     */
    public long getStatsPlaceHolderAllocations()
        {
        return __m_StatsPlaceHolderAllocations;
        }
    
    // Accessor for the property "StatsWaits"
    /**
     * Getter for property StatsWaits.<p>
    * The total number of times a thread "waited" for an element to arrive in
    * order to get or remove it.
     */
    public long getStatsWaits()
        {
        return __m_StatsWaits;
        }
    
    // Accessor for the property "WaitingThreadCount"
    /**
     * Getter for property WaitingThreadCount.<p>
    * The number of threads waiting to indices to be set.
     */
    protected java.util.concurrent.atomic.AtomicLong getWaitingThreadCount()
        {
        return __m_WaitingThreadCount;
        }
    
    // Declared at the super level
    /**
     * Getter for property WindowSize.<p>
    * The WindowSize is the number of elements that the  WindowedArray is
    * currently using in the Store.
    * 
    * For the ConcurrentWindowedArray this is a calculated property, which
    * obtains index synchronization.  Thus, it is somewhat expensive to call,
    * and cannot be called while holding any other index lock.  The accessor is
    * not called by the implementation.
     */
    public int getWindowSize()
        {
        return (int) ((getLastIndex() - getFirstIndex()) + 1);
        }
    
    // Declared at the super level
    /**
     * Grow the storage array to a size such that it can store the specified
    * virtual index.  The caller must be synchronized on the WindowedArray.
     */
    protected void grow(long lVirtual)
        {
        Object[] aoStore = getStore();
        
        long lvFirst      = getFirstIndex();
        int  cOldCapacity = aoStore.length;
        int  cNewCapacity = checkCapacity(Math.max(cOldCapacity * 2,
                (int) (lVirtual - lvFirst) + 3));
        
        try
            {
            setStore(null); // mark that we are growing
        
            // Now that all other threads will see a null store we can read LastIndex
            // which will be the highest index which could possibly contain a non-null
            // value. Other threads could still increment LastIndex, but it is guaranteed
            // that they will not be able to write to the old store.
            long lvLast       = getLastIndex();
            int  cReqCapacity = (int) (lvLast - lvFirst) + 1;
            if (cReqCapacity > cNewCapacity)
                {        
                // new capacity must be at least large enough to hold lvFirst ... lvlast
                // it is possible that lVirtual is less than lvLast, if multiple threads attempt
                // to trigger growth
                if (cReqCapacity > getMaximumCapacity())
                    {
                    // checkCapacity is not used as it pulls the store, which is now null
                    throw new IndexOutOfBoundsException(get_Name() + " has exceeded max capacity");
                    }
                cNewCapacity = cReqCapacity;
                }
        
            Object[] aoNew      = new Object[cNewCapacity];
            int      iaNewFirst = (int) (lvFirst % cNewCapacity);
            long     lNewOffset = lvFirst - iaNewFirst;
            Object   oFree;
        
            // fill the new array with PlaceHolders, taking the wrap around point into account
        
            oFree = getPlaceHolder(lNewOffset);
            for (int iActual = iaNewFirst; iActual < cNewCapacity; ++iActual)
                {
                aoNew[iActual] = oFree;
                }
        
            oFree = getPlaceHolder(lNewOffset + cNewCapacity);
            for (int iActual = 0; iActual < iaNewFirst; ++iActual)
                {
                aoNew[iActual] = oFree;
                }
        
            // copy contents, maintaining mod based index translation
            for (long lvNext = lvFirst; lvNext <= lvLast; ++lvNext)
                {
                int    iaOld = (int) (lvNext % cOldCapacity);
                Object oOld;
        
                synchronized (getIndexLock(lvNext))
                    {
                    // ensure we copy the latest value
                    oOld = aoStore[iaOld];
                    }
        
                long lvFound = safeRetrieveIndexFromValue(oOld, iaOld);
                int  iaNew   = (int) (lvNext % cNewCapacity);
                if (lvFound == lvNext && !(oOld instanceof ConcurrentWindowedArray.PlaceHolder))
                    {
                    aoNew[iaNew] = oOld;
                    }
                else if (lvFound > lvNext)
                    {
                    // Virtual index lvNext had been removed, it cannot be revived. Ensure
                    // that iaNew holds the PlaceHolder for its next virtual index.
                    aoNew[iaNew] = getPlaceHolder((lvNext + cNewCapacity) - iaNew);
                    }
                // else, maintain existing aoNew[iaNew] ConcurrentWindowedArray.PlaceHolder
                }
        
            aoStore = aoNew;
            }
        finally
            {
            setStore(aoStore); // commit or rollback
            setStatsExpansions(getStatsExpansions() + 1);
            }
        }
    
    // Declared at the super level
    /**
     * Return true if the specified index has been removed from the windowed
    * array.
     */
    public boolean isRemoved(long lVirtual)
        {
        Object[] aoStore = getStableStore();
        int      iActual = (int) (lVirtual % aoStore.length);
        Object   oValue  = aoStore[iActual];
        
        if (safeRetrieveIndexFromValue(oValue, iActual) > lVirtual)
            {
            // dirty read was able to verify that removal had occurred
            return true;
            }
        
        synchronized (getIndexLock(lVirtual))
            {
            oValue = aoStore[iActual];
            }
        
        return safeRetrieveIndexFromValue(oValue, iActual) > lVirtual;
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification called out of
    * setConstructed() for the topmost component and that in turn notifies all
    * the children.
    * 
    * This notification gets called before the control returns to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as) the control returns to the
    * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        // import java.util.concurrent.atomic.AtomicLong;
        
        setAtomicLastIndex(new AtomicLong(-1));
        setWaitingThreadCount(new AtomicLong());
        setLockOffset(System.identityHashCode(this));
        setRecentPlaceHolders(new ConcurrentWindowedArray.PlaceHolder[3]);
        
        super.onInit(); // allocates store
        
        // fill in Store with PlaceHolders
        Object[] aoStore = getStore();
        Object   oFree   = getPlaceHolder(0L);
        for (int i = 0, c = aoStore.length; i < c; ++i)
            {
            aoStore[i] = oFree;
            }
        setStore(aoStore); // ensure visibility of PlaceHolders
        }
    
    /**
     * Obtain an Object from the WindowedArray at the specified index via an
    * optimistic read.  This method will acquire no locks as part of the read,
    * and while it is guaranteed that it will return an Object that was at one
    * time stored as the specified index the value may be out-of-date.  As all
    * indices initially start with a value of null, this may also be returned. 
    * For most uses of the ConcurrentWindowedArray it is expected that this
    * method will not provide any significant performance improvement over the
    * standard get() method, unless the specified index is expected to be
    * accessed concurrently by many threads.
    * 
    * @param i  the index of the element to obtain
    * 
    * @return a value which is guaranteed to have at one point in time been
    * stored at the specified index
    * 
    * @throws IndexOutOfBoundsException if the index is negative
     */
    public Object optimisticGet(long lVirtual)
        {
        // getStableStore() is not used as this is an optimistic read
        Object[] aoStore = getStore();
        if (aoStore != null)
            {
            int    iActual = (int) (lVirtual % aoStore.length);
            Object oValue  = aoStore[iActual];
        
            if (safeRetrieveIndexFromValue(oValue, iActual) == lVirtual &&
                    !(oValue instanceof ConcurrentWindowedArray.PlaceHolder))
                {
                setStatsGetsOptimistic(getStatsGetsOptimistic() + 1L);
                return oValue;
                }
            }
        
        return null;
        }
    
    // Declared at the super level
    /**
     * Remove an Object from the WindowedArray and return the Object. Note that
    * it is necessary to remove the elements at the front of the WindowedArray
    * to allow the FirstIndex to increase, thus allowing the WindowedArray to
    * store more values without resizing the Store.  For the
    * ConcurrentWindowedArray once an index has been removed it cannot be
    * re-used.  If re-use is required then set(i, null) should be used instead
    * of the intermediate remove(i) call.
    * 
    * @param i  the index of the element to remove
    * 
    * @return the Object that was removed or null if there were no Object at
    * that index
    * 
    * @throws IndexOutOfBoundsException if the index is negative or greater
    * than the LastIndex of the WindowedArray
     */
    public Object remove(long lVirtual)
        {
        // import com.tangosol.util.Base;
        
        if (lVirtual > getLastIndex())
            {
            throw new IndexOutOfBoundsException("cannot remove beyond the window");    
            }
        
        try
            {   
            return removeInternal(lVirtual, 0L, false);
            }
        catch (InterruptedException e)
            {
            // should not be possible with the 0L timeout
            Thread.currentThread().interrupt();
            throw Base.ensureRuntimeException(e);
            }
        }
    
    /**
     * Internal version of remove supporting safe and unsafe index removal. 
    * Safe index removal does not allow removal of null values, but does allow
    * for a timed wait for the value to become non-null.  Unsafe removal is
    * allowed to remove nulls, and as such does not supported timed removal as
    * there is nothing to wait for.
    * 
    * @param i  the index of the element to remove
    * @param cMillis  the number of milliseconds to wait for a non-null value
    * to be removable.  A value of -1 will wait indefinitely, a value of 0 will
    * not wait at all.  Specifying a value other than 0 requires that fSafe
    * also be set to true.
    * @param fSafe  if true the null values are not considered to be removable
    * 
    * @return the Object at the requested index, or null if the requested index
    * has already been removed or the timeout has expired
    * 
    * @throws IndexOutOfBoundsException if the index is negative, or if it is
    * greater than the LastIndex and fSafe is false
     */
    protected Object removeInternal(long lVirtual, long cMillis, boolean fSafe)
            throws java.lang.InterruptedException
        {
        if (lVirtual < 0L)
            {
            throw new IndexOutOfBoundsException("negative index is illegal: " + lVirtual);
            }
        
        Object   oResult    = null;
        int      cCapacity  = 0;
        boolean  fBlocking  = cMillis != 0L;
        long     ldtTimeout = cMillis > 0L ? System.currentTimeMillis() + cMillis : 0L;
        int      iActual    = 0;
        Object[] aoStore;
        
        if (fBlocking && !fSafe)
            {
            throw new IllegalArgumentException("Blocking remove cannot remove nulls");
            }
        
        while (true)
            {
            Object oLock = getIndexLock(lVirtual); 
            synchronized (oLock)
                {
                aoStore = getStore(); // must be pulled within sync to ensure atomic result
                if (aoStore != null)
                    {
                    cCapacity = aoStore.length;
                    iActual   = (int) (lVirtual % cCapacity);
                    oResult   = aoStore[iActual];
        
                    long lvFound = safeRetrieveIndexFromValue(oResult, iActual);
                    if (lvFound > lVirtual)
                        {
                        // lVirtual had already been removed
                        return null;
                        }
                    else if (lvFound < lVirtual || (fSafe && oResult instanceof ConcurrentWindowedArray.PlaceHolder))
                        {
                        // no removable value at lVirtual yet, either because it isn't in the window yet,
                        // or because it is null and we aren't allowed to remove nulls
                        if (fBlocking)
                            {
                            // wait for a notification
                            long cMillisLeft = waitIndex(oLock, ldtTimeout);
                            if (cMillis >= 0L && (cMillisLeft <= 0L || cMillisLeft > cMillis))
                                {
                                // timeout expired, or clock moved back
                                return null;
                                }
                            }
                        else
                            {
                            // non-blocking
                            if (fSafe)
                                {
                                // we're non-blocking and not allowed to remove nulls
                                return null;
                                }
                            else
                                {
                                // we're non-blocking and allowed to remove nulls, but
                                // lVirtual is not yet in the window, and we're not allowed to wait
                                throw new IndexOutOfBoundsException("cannot remove beyond the window");
                                }
                            }
                        }
                   else
                        {
                        // we found the correct index, and are allowed to remove even if it is null
                        aoStore[iActual] = getPlaceHolder((lVirtual - iActual) + cCapacity);
                        if (getWaitingThreadCount().get() > 0L)
                            {
                            oLock.notifyAll();
                            }
                        break;
                        }
                    }
                } // synchronized (oLock)
        
            // wait from growth to complete
            if (aoStore == null)
                {
                // wait for other thread to finish growth        
                getStableStore();
                }
            // retry remove
            }
        
        // remove of lVirtual has occurred
        
        // iActual is now reserved for the virtual index (lVirtual + cCapacity),
        // we need to synchronize on the new virtual index to ensure that the PlaceHolder
        // will be immediately visible by other threads.
        synchronized (getIndexLock(lVirtual + cCapacity))
            {
            // flush all prior changes
            }
        
        // push AssumedFirstIndex forward if possible
        if (lVirtual == getAssumedFirstIndex())
            {
            // we know that:
            //   - assumedFirst <= realFirst (we never allow it)
            //   - realFirst <= lVirtual (otherwise it couldn't have been removed)
            //   - lVirtual == assumedFirst
            //   therefore lVirtual == realFirst
        
            // we just removed lvFirst, find next assumed non-removed virtual index
            // it is ok to be wrong as long as we don't choose a value greater then realFirst
            for (long lvFirst = lVirtual + 1L; true; ++lvFirst)
                {
                iActual = (int) (lvFirst % cCapacity);
                if (safeRetrieveIndexFromValue(aoStore[iActual], iActual) <= lvFirst)
                    {
                    // dirty read (memory model) indicates that this may be first, we
                    // know that it is at least <= to the real first, which makes it a
                    // usable assumed first
                    setAssumedFirstIndex(lvFirst);
                    break;
                    }
                }
            }
        
        return oResult instanceof ConcurrentWindowedArray.PlaceHolder ? null : oResult;
        }
    
    /**
     * Get the virtual index from the value.  An implementation of this method
    * must be aware of the datatype stored within the array and the datatype
    * must be able to supply the index from the value.  For instance when
    * Messages are used as values, the index is the MessageId.
    * 
    * This method may be called while synchronization is held on the associated
    * virtual index, as such the implementation must not make use of any
    * Base.getCommonMonitor() synchronization as this will result in a deadlock.
     */
    protected long retrieveIndexFromValue(Object o)
        {
        return 0L;
        }
    
    /**
     * Remove a non-null Object from the WindowedArray and return that Object.
    * 
    * Note: As safeRemove only removes non-null values, if a null value needs
    * to be removed, the remove() method must be used.
    * 
    * @param i  the index of the element to remove
    * 
    * @return the Object at the requested index, or null if there was no
    * non-null object to remove.
    * 
    * @throws IndexOutOfBoundsException if the index is negative
     */
    public Object safeRemove(long lVirtual)
        {
        // import com.tangosol.util.Base;
        
        try
            {
            return removeInternal(lVirtual, 0L, true);
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw Base.ensureRuntimeException(e);
            }
        }
    
    /**
     * Remove a non-null Object from the WindowedArray and return that Object,
    * waiting up to cMillis if necessary for the value at the specified index
    * to become non-null.
    * 
    * Note: As safeRemove only removes non-null values, if a null value needs
    * to be removed, the remove() method must be used.
    * 
    * @param i  the index of the element to remove
    * @param cMillis  the number of milliseconds to wait for a non-null value
    * to be removable.  A value of -1 specifies an indefinite wait, a value of
    * 0 specifies not to wait
    * 
    * @return the Object at the requested index, or null if the requested index
    * has already been removed or the timeout has expired
    * 
    * @throws IndexOutOfBoundsException if the index is negative
     */
    public Object safeRemove(long lVirtual, long cMillis)
            throws java.lang.InterruptedException
        {
        return removeInternal(lVirtual, cMillis, true);
        }
    
    /**
     * Get the virtual index from the value.
    * 
    * @param o           the object to retrieve the virtual index from
    * @param iActual  its storage position within the array
    * 
    * @return virtual index of the value or -1 if the specified value does not
    * contain an index or is null.
     */
    protected long safeRetrieveIndexFromValue(Object o, int iActual)
        {
        return o instanceof ConcurrentWindowedArray.PlaceHolder
            ? ((ConcurrentWindowedArray.PlaceHolder) o).getVirtualIndex(iActual) : retrieveIndexFromValue(o);
        }
    
    // Declared at the super level
    /**
     * Store an Object in the WindowedArray at the specified index and return
    * the Object that was previously stored at that index.  The value's
    * internal index will be updated prior to storing it in the array, see
    * assignIndexToValue.
    * 
    * @param i  the index of the element to store
    * @param o  the Object to store (could be null)
    * 
    * @return the Object that was previously at that index or null if there
    * were no Object at that index
    * 
    * @throws IndexOutOfBoundsException if the index is negative, less than the
    * FirstIndex, or if storing a value at that index would cause the Store to
    * exceed the MaximumCapacity
     */
    public Object set(long lVirtual, Object o)
        {
        if (lVirtual < 0L)
            {
            throw new IndexOutOfBoundsException("negative index is illegal: " + lVirtual);
            }
        
        // must be before set, in case another thread grows while we
        // are within set, otherwise it may not be included in the copy
        ensureLastIndexMinimum(lVirtual);
        
        return setInternal(lVirtual, o);
        }
    
    // Accessor for the property "AssumedFirstIndex"
    /**
     * Setter for property AssumedFirstIndex.<p>
    * The virtual index which is assumed to be the "first" index.  This value
    * is guaranteed to be <= to the real first index.
    * 
    * @volatile
     */
    protected void setAssumedFirstIndex(long i)
        {
        __m_AssumedFirstIndex = i;
        }
    
    // Accessor for the property "AtomicLastIndex"
    /**
     * Setter for property AtomicLastIndex.<p>
    * The AtomicCounter representing the LastIndex.
     */
    protected void setAtomicLastIndex(java.util.concurrent.atomic.AtomicLong counter)
        {
        __m_AtomicLastIndex = counter;
        }
    
    // Declared at the super level
    /**
     * Setter for property FirstIndex.<p>
    * The FirstIndex property provides the lowest [virtual] index into the
    * WindowedArray that can be set and which may return a non-null value. As
    * elements are removed from the front of the WindowedArray, the FirstIndex
    * property increases. The relationship between FirstIndex and LastIndex and
    * Size is as follows: A WindowedArray of n elements of which the first m
    * elements have been removed has a FirstIndex of m, a LastIndex of n-1, and
    * a Size of n.
    * 
    * Note: Within the implementation virtual indices are prefixed with "lv"
    * (long virtual).
    * 
    * For the ConcurrentWindowedArray this is a calculated property, which
    * obtains index synchronization.  Thus, it is somewhat expensive to call,
    * and cannot be called while holding any other index lock.  Its internal
    * use is restricted to growth operations.
     */
    protected void setFirstIndex(long lVirtual)
        {
        throw new UnsupportedOperationException();
        }
    
    /**
     * Internal version of set(), this method does not ensure that LastIndex is
    * updated.
     */
    protected Object setInternal(long lVirtual, Object o)
        {
        while (true)
            {
            Object[] aoStore;
        
            long   lvFound = 0L;
            int    iActual = 0;
            Object oLock   = getIndexLock(lVirtual);    
            synchronized (oLock)
                {
                aoStore = getStore(); // must be pulled within sync to ensure atomic result
                if (aoStore != null)
                    {
                    iActual = (int) (lVirtual % aoStore.length);
        
                    Object oOld = aoStore[iActual];
        
                    lvFound = safeRetrieveIndexFromValue(oOld, iActual);
                    if (lvFound == lVirtual)
                        {
                        if (o == null)
                            {
                            // don't store nulls
                            o = getPlaceHolder(lVirtual - iActual);
                            }
                        else
                            {
                            assignIndexToValue(lVirtual, o);
                            }
        
                        aoStore[iActual] = o;
        
                        if (getWaitingThreadCount().get() > 0L)
                            {
                            oLock.notifyAll();
                            }
                        return oOld;
                        }
                    }
                }
        
            if (aoStore == null)
                {
                // growth in progress, wait
                getStableStore();
                }
            else if (lvFound > lVirtual)
                {
                // lVirtual has already been removed
                int iaFound = (int) (lvFound % aoStore.length);
                if (iaFound == iActual)
                    {
                    // their fault
                    throw new IndexOutOfBoundsException("index " + lVirtual +
                        " has already been removed and cannot be reset");
                    }
                else
                    {
                    // our fault
                    throw new IllegalStateException("found unexpected value at virtual index " +
                        lvFound + " actual " + iaFound + " capacity " + aoStore.length);
                    }
                }
            else // lvFound < lVirtual
                {
                // we need to grow the array
                synchronized (this)
                    {
                    // ensure that another thread didn't just call the grow
                    if (getStore() == aoStore)
                        {
                        grow(lVirtual);
                        }
                    }
                }
            }
        }
    
    // Accessor for the property "LockOffset"
    /**
     * Setter for property LockOffset.<p>
    * The offset into the array of common monitors.
     */
    protected void setLockOffset(long lOffset)
        {
        __m_LockOffset = lOffset;
        }
    
    // Accessor for the property "RecentPlaceHolders"
    /**
     * Setter for property RecentPlaceHolders.<p>
    * An array of recently allocated PlaceHolder objects.
     */
    protected void setRecentPlaceHolders(ConcurrentWindowedArray.PlaceHolder[] aPlaceHolder)
        {
        __m_RecentPlaceHolders = aPlaceHolder;
        }
    
    // Accessor for the property "StatsGetsOptimistic"
    /**
     * Setter for property StatsGetsOptimistic.<p>
    * The total number of optimistic gets which returned a non-null result.
     */
    public void setStatsGetsOptimistic(long pStatsGetsOptimistic)
        {
        __m_StatsGetsOptimistic = pStatsGetsOptimistic;
        }
    
    // Accessor for the property "StatsPlaceHolderAllocations"
    /**
     * Setter for property StatsPlaceHolderAllocations.<p>
    * The total number of PlaceHolder objects which have been allocated over
    * time.
     */
    protected void setStatsPlaceHolderAllocations(long cHolders)
        {
        __m_StatsPlaceHolderAllocations = cHolders;
        }
    
    // Accessor for the property "StatsWaits"
    /**
     * Setter for property StatsWaits.<p>
    * The total number of times a thread "waited" for an element to arrive in
    * order to get or remove it.
     */
    protected void setStatsWaits(long cWaits)
        {
        __m_StatsWaits = cWaits;
        }
    
    // Declared at the super level
    /**
     * Setter for property Store.<p>
    * The Store property actually holds the contents of the WindowedArray; it
    * is an array of Object references. This array is initialized by the onInit
    * event and resized (note: only grows, does not shrink) as necessary by the
    * ensureIndex method. The WindowedArray uses the Store as a circular queue,
    * with the head of the queue being at the index into the array denoted by
    * WindowIndex, and the number of elements in the queue denoted by
    * WindowSize. Since the queue is stored in a "circular" fashion, the head
    * of the queue may be at any point in the array, and once the tail reaches
    * the end of the array, it "rolls over" to the front of the array.
    * Likewise, as elements are removed from the queue, when the head reaches
    * the end of the array it will "roll over" to the front of the array.
    * 
    * @volatile for ConcurrentWindowedArray.getStableStore()
     */
    protected void setStore(Object[] ao)
        {
        _assert(ao == null || ao.length != 0);
        
        super.setStore(ao);
        }
    
    // Accessor for the property "WaitingThreadCount"
    /**
     * Setter for property WaitingThreadCount.<p>
    * The number of threads waiting to indices to be set.
     */
    protected void setWaitingThreadCount(java.util.concurrent.atomic.AtomicLong counter)
        {
        __m_WaitingThreadCount = counter;
        }
    
    // Declared at the super level
    /**
     * Translate a "virtual index" (an index into the WindowedArray) to an index
    * into the Store.
    * 
    * @param lVirtual  the "virtual index" (the index into the WindowedArray)
    * that is known to be backed by the Store
    * 
    * @return the translated index into the Store that the passed index is
    * backed by
     */
    protected int translateIndex(long lVirtual)
        {
        // translation on the ConcurrentWindowedArray is simple, it is always iActual = iVirtual % capacity
        // it is thus inlined everywhere making a definition here redundant and a bit confusing.
        throw new UnsupportedOperationException();
        }
    
    /**
     * Wait for notification on the specified index.  The caller must hold
    * synchronization on the index.
    * 
    * Note the timing is based on System.currentTimeMillis() rather than
    * Base.getSafeTimeMillis() to avoid the single point of synchronization,
    * which would defeat the purpose of the ConcurrentWindowedArray.  As such
    * it is possible for the clock to shift back and for start + return >
    * timeout.  The caller should watch for this and treat it as a timeout.
    * 
    * @param oLock  the lock associated with the virtual index on which to wait
    * for notification
    * @param ldtTimeout  the expiration timestamp as measured by
    * System.currentTimeMillis()
    * 
    * @return the amount of time remaining on the timeout, thus a value which
    * is >=0 implies notification
     */
    protected long waitIndex(Object oLock, long ldtTimeout)
            throws java.lang.InterruptedException
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import java.util.concurrent.atomic.AtomicLong;
        
        AtomicLong waiting = getWaitingThreadCount();
        
        waiting.incrementAndGet();
        setStatsWaits(getStatsWaits() + 1L);
        try
            {
            if (ldtTimeout > 0L)
                {
                // timed wait
                long cWaitMillis = ldtTimeout - System.currentTimeMillis();
                if (cWaitMillis > 0L)
                    {
                    Blocking.wait(oLock, Math.min(1000L, cWaitMillis)); // protect against clock rollback
                    return ldtTimeout - System.currentTimeMillis();
                    }
                else
                    {
                    // timeout expired
                    return cWaitMillis;
                    }
                }
            else
                {
                // indefinite wait
                Blocking.wait(oLock);
                return Long.MAX_VALUE;
                }
            }
        finally
            {
            waiting.decrementAndGet();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.windowedArray.ConcurrentWindowedArray$PlaceHolder
    
    /**
     * A PlaceHolder represents a value of null and is used to mark the virtual
     * index assigned to an actual index within the storage array.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class PlaceHolder
            extends    com.tangosol.coherence.Component
        {
        // ---- Fields declarations ----
        
        /**
         * Property VirtualOffset
         *
         * The virtual array offset associated with this PlaceHolder.  The
         * relationship between the actual storage location, the virtual
         * storage location, and the offset is as follows.
         * 
         * iVirtual = lOffset + iActual
         */
        private long __m_VirtualOffset;
        
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
            return new com.tangosol.coherence.component.util.windowedArray.ConcurrentWindowedArray.PlaceHolder();
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
                clz = Class.forName("com.tangosol.coherence/component/util/windowedArray/ConcurrentWindowedArray$PlaceHolder".replace('/', '.'));
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
        
        // Accessor for the property "VirtualIndex"
        /**
         * Getter for property VirtualIndex.<p>
        * The virtual index at which the PlaceHolder resides.
         */
        public long getVirtualIndex(int iActual)
            {
            return getVirtualOffset() + iActual;
            }
        
        // Accessor for the property "VirtualOffset"
        /**
         * Getter for property VirtualOffset.<p>
        * The virtual array offset associated with this PlaceHolder.  The
        * relationship between the actual storage location, the virtual storage
        * location, and the offset is as follows.
        * 
        * iVirtual = lOffset + iActual
         */
        public long getVirtualOffset()
            {
            return __m_VirtualOffset;
            }
        
        // Accessor for the property "VirtualOffset"
        /**
         * Setter for property VirtualOffset.<p>
        * The virtual array offset associated with this PlaceHolder.  The
        * relationship between the actual storage location, the virtual storage
        * location, and the offset is as follows.
        * 
        * iVirtual = lOffset + iActual
         */
        public void setVirtualOffset(long lVirtual)
            {
            __m_VirtualOffset = lVirtual;
            }
        
        // Declared at the super level
        public String toString()
            {
            return "PlaceHolder VirtualOffset=" + getVirtualOffset();
            }
        }
    }
