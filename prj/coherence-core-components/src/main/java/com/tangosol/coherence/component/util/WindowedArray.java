
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.WindowedArray

package com.tangosol.coherence.component.util;

import com.tangosol.util.Base;

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
 * the array, the window adjusts such that those elements are no longer visible.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class WindowedArray
        extends    com.tangosol.coherence.component.Util
    {
    // ---- Fields declarations ----
    
    /**
     * Property FirstIndex
     *
     * The FirstIndex property provides the lowest [virtual] index into the
     * WindowedArray that can be set and which may return a non-null value. As
     * elements are removed from the front of the WindowedArray, the FirstIndex
     * property increases. The relationship between FirstIndex and LastIndex
     * and Size is as follows: A WindowedArray of n elements of which the first
     * m elements have been removed has a FirstIndex of m, a LastIndex of n-1,
     * and a Size of n.
     * 
     * Note: Within the implementation virtual indices are prefixed with "lv"
     * (long virtual).
     */
    private long __m_FirstIndex;
    
    /**
     * Property FirstStuckIndex
     *
     * The index of an element which appears to be stuck and causing the window
     * to grow uncontrollably.
     * 
     * @see #setWindowSize
     */
    private transient long __m_FirstStuckIndex;
    
    /**
     * Property LastSizeWarningMillis
     *
     * The time at which the last size warning was issued.
     * 
     * @see #setWindowSize
     */
    private transient long __m_LastSizeWarningMillis;
    
    /**
     * Property REMOVED
     *
     * This is a token that is used to denote an element that has been
     * specifically removed, as opposed to one set to null or one which has not
     * yet been set.
     */
    protected static final Object REMOVED;
    
    /**
     * Property StatsExpansions
     *
     * The total number of times the Store has been expanded.
     */
    private int __m_StatsExpansions;
    
    /**
     * Property Store
     *
     * The Store property actually holds the contents of the WindowedArray; it
     * is an array of Object references. This array is initialized by the
     * onInit event and resized (note: only grows, does not shrink) as
     * necessary by the ensureIndex method. The WindowedArray uses the Store as
     * a circular queue, with the head of the queue being at the index into the
     * array denoted by WindowIndex, and the number of elements in the queue
     * denoted by WindowSize. Since the queue is stored in a "circular"
     * fashion, the head of the queue may be at any point in the array, and
     * once the tail reaches the end of the array, it "rolls over" to the front
     * of the array. Likewise, as elements are removed from the queue, when the
     * head reaches the end of the array it will "roll over" to the front of
     * the array.
     * 
     * @volatile for ConcurrentWindowedArray.getStableStore()
     */
    private volatile Object[] __m_Store;
    
    /**
     * Property WindowIndex
     *
     * The WindowIndex is the 0-based offset into the Store at which the first
     * element of the WindowedArray is stored.
     * 
     * Note: Within the implementation actual indices are prefixed with "ia"
     * (int actual).
     */
    private int __m_WindowIndex;
    
    /**
     * Property WindowSize
     *
     * The WindowSize is the number of elements that the  WindowedArray is
     * currently using in the Store.
     */
    private int __m_WindowSize;
    
    // Static initializer
    static
        {
        try
            {
            REMOVED = new java.lang.Object();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
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
        
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    // Getter for virtual constant InitialCapacity
    public int getInitialCapacity()
        {
        return 64;
        }
    
    // Getter for virtual constant MaximumCapacity
    public int getMaximumCapacity()
        {
        return 16777216;
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.WindowedArray();
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
            clz = Class.forName("com.tangosol.coherence/component/util/WindowedArray".replace('/', '.'));
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
    
    /**
     * Add an Object to the WindowedArray and return the index of the new
    * element.
    * 
    * @param o  the Object to add to the end of the WindowedArray
    * 
    * @return the index of the element that was added
    * 
    * @throws IndexOutOfBoundsException if the WindowedArray cannot add the
    * element because the MaximumCapacity has already been reached
     */
    public synchronized long add(Object o)
        {
        long lVirtual = getSize();
        int  iActual  = ensureIndex(lVirtual);
        
        getStore()[iActual] = o;
        
        return lVirtual;
        }
    
    /**
     * Check that the specified capacity is allowable.
    * 
    * @param cCapacity the desired capacity
    * 
    * @return the allowed capacity
     */
    protected int checkCapacity(int cCapacity)
        {
        // import com.tangosol.util.Base;
        
        int cMaxCapacity     = getMaximumCapacity();
        int cCurrentCapacity = getCapacity();
        if (cCurrentCapacity == cMaxCapacity)
            {
            // issue error with information which may help to identify why the
            // array has grown so large
            long   lvFirst = getFirstIndex();
            Object oFirst  = get(lvFirst);
            String sFirst  = null;
            if (oFirst != null)
                {
                sFirst = oFirst.getClass().getName() + ":" + oFirst;
                }
        
            throw new IndexOutOfBoundsException(get_Name() + " has exceeded max capacity of " +
                cMaxCapacity + " size = " + getSize() + "; first element[" + lvFirst + "] = " +
                sFirst);
            }
        else if (cCapacity > getMaximumCapacity() >>> 4)
            {
            // we're big and getting bigger, check if we're stuck on the first index
            long lvFirst = getFirstIndex();
            if (lvFirst == getFirstStuckIndex())
                {
                // we've been stuck on the same element since the last growth cycle    
                long ldtNow = Base.getSafeTimeMillis();
                if (ldtNow - getLastSizeWarningMillis() > 30000L)
                    {
                    Object oFirst = get(lvFirst);
                    String sFirst = null;
                    if (oFirst != null)
                        {
                        sFirst = oFirst.getClass().getName() + ":" + oFirst;
                        }
        
                    // periodically issue a warning
                    _trace(get_Name() + " window size has grown to " + cCapacity +
                        " elements; first element[" + lvFirst + "] = " + sFirst, 2);
                    setLastSizeWarningMillis(ldtNow);
                    }
                }
            setFirstStuckIndex(lvFirst);
            }
        
        return Math.min(cCapacity, cMaxCapacity);
        }
    
    /**
     * Ensure that the Store is large enough that the WindowedArray can store an
    * element at the specified index.
    * 
    * @param lVirtual  the "virtual index" (the index into the WindowedArray)
    * that must be backed by the Store
    * 
    * @return the translated index into the Store that the ensured index is
    * backed by
    * 
    * @throws IndexOutOfBoundsException if the specified index is less than the
    * FirstIndex, or if storing a value at the specified index would cause the
    * Store to exceed the MaximumCapacity
     */
    protected int ensureIndex(long lVirtual)
        {
        long lvFirst = getFirstIndex();
        if (lVirtual < lvFirst)
            {
            throw new IndexOutOfBoundsException("window cannot grow backwards (index="
                + lVirtual + ", window first index=" + lvFirst + ")");
            }
        
        long iLast = getLastIndex();
        if (lVirtual > iLast)
            {
            // the index is out of bounds for the window;
            // make sure it is in bounds for the actual store    
            int cNewElements = (int) (lVirtual - lvFirst) + 1;
            if (cNewElements > getCapacity())
                {    
                grow(lVirtual);
                }
        
            // resize the window to include the new index
            setWindowSize(cNewElements);
            }
        
        return translateIndex(lVirtual);
        }
    
    /**
     * Return a status report.
     */
    public String formatStats()
        {
        StringBuilder sb = new StringBuilder();
        
        sb.append("capacity=")
          .append(getCapacity())
          .append(", expansions=")
          .append(getStatsExpansions())
          .append(", size=")
          .append(getSize())
          .append(", window index=")
          .append(getWindowIndex())
          .append(", window size=")
          .append(getWindowSize())
          .append(", first index=")
          .append(getFirstIndex())
          .append(", last index=")
          .append(getLastIndex());
        
        return sb.toString();
        }
    
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
    public synchronized Object get(long lVirtual)
        {
        if (lVirtual < 0L)
            {
            throw new IndexOutOfBoundsException("negative index is illegal: " + lVirtual);
            }
        
        // if the index is out of the range of the window, then the
        // value is null (there is no exception)
        if (lVirtual < getFirstIndex() || lVirtual > getLastIndex())
            {
            return null;
            }
        
        Object o = getStore()[translateIndex(lVirtual)];
        return o == REMOVED ? null : o;
        }
    
    /**
     * Obtain a set of Objects from the WindowedArray at the specified indices.
    * 
    * @param alIndex  the array of indices to obtain
    * @param cEntries the number of indices to process, thus allowing longer
    * arrays then necessary to be supplied
    * @param aoResult the array in which to insert the values
    * 
    * @return the number of items found, previously removed items will not be
    * counted
    * 
    * @throws IndexOutOfBoundsException if any of the indices is negative
     */
    public synchronized int getAll(long[] alIndex, int cEntries, Object[] aoResult)
        {
        long lvFirstIndex = getFirstIndex();
        long lvLastIndex  = getLastIndex();
        int  cFound       = 0;
        for (int i = 0; i < cEntries; ++i)
            {
            long lVirtual = alIndex[i];
            if (lVirtual < 0L)
                {
                throw new IndexOutOfBoundsException("negative index is illegal: " + lVirtual);
                }
        
            // if the index is out of the range of the window, then the
            // value is null (there is no exception)
            if (lVirtual < lvFirstIndex || lVirtual > lvLastIndex)
                {
                aoResult[i] = null;
                }
            else
                {
                Object o = getStore()[translateIndex(lVirtual)];
                if (o == REMOVED)
                    {
                    aoResult[i] = null;
                    }
                else
                    {
                    aoResult[i] = o;
                    ++cFound;
                    }
                }
            }
        return cFound;
        }
    
    // Accessor for the property "Capacity"
    /**
     * Getter for property Capacity.<p>
    * The Capacity property provides the maximum number of Objects that can be
    * stored in the WindowedArray without resizing the WindowedArray's
    * underlying store. This property is calculated.
     */
    public int getCapacity()
        {
        return getStore().length;
        }
    
    // Accessor for the property "FirstIndex"
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
     */
    public long getFirstIndex()
        {
        return __m_FirstIndex;
        }
    
    // Accessor for the property "FirstStuckIndex"
    /**
     * Getter for property FirstStuckIndex.<p>
    * The index of an element which appears to be stuck and causing the window
    * to grow uncontrollably.
    * 
    * @see #setWindowSize
     */
    public long getFirstStuckIndex()
        {
        return __m_FirstStuckIndex;
        }
    
    // Accessor for the property "LastIndex"
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
        return getSize() - 1;
        }
    
    // Accessor for the property "LastSizeWarningMillis"
    /**
     * Getter for property LastSizeWarningMillis.<p>
    * The time at which the last size warning was issued.
    * 
    * @see #setWindowSize
     */
    public long getLastSizeWarningMillis()
        {
        return __m_LastSizeWarningMillis;
        }
    
    // Accessor for the property "Size"
    /**
     * Getter for property Size.<p>
    * The Size property provides the number of virtual elements in the
    * WindowedArray, starting at the zero-ith element and preceding to the
    * element at LastIndex. The relationship between FirstIndex and LastIndex
    * and Size is as follows: A WindowedArray of n elements of which the first
    * m elements have been removed has a FirstIndex of m, a LastIndex of n-1,
    * and a Size of n.
     */
    public long getSize()
        {
        return getFirstIndex() + getWindowSize();
        }
    
    // Accessor for the property "StatsExpansions"
    /**
     * Getter for property StatsExpansions.<p>
    * The total number of times the Store has been expanded.
     */
    public int getStatsExpansions()
        {
        return __m_StatsExpansions;
        }
    
    // Accessor for the property "Store"
    /**
     * Getter for property Store.<p>
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
    protected Object[] getStore()
        {
        return __m_Store;
        }
    
    // Accessor for the property "WindowIndex"
    /**
     * Getter for property WindowIndex.<p>
    * The WindowIndex is the 0-based offset into the Store at which the first
    * element of the WindowedArray is stored.
    * 
    * Note: Within the implementation actual indices are prefixed with "ia"
    * (int actual).
     */
    protected int getWindowIndex()
        {
        return __m_WindowIndex;
        }
    
    // Accessor for the property "WindowSize"
    /**
     * Getter for property WindowSize.<p>
    * The WindowSize is the number of elements that the  WindowedArray is
    * currently using in the Store.
     */
    public int getWindowSize()
        {
        return __m_WindowSize;
        }
    
    /**
     * Grow the storage array to a size such that it can store the specified
    * virtual index.  The caller must be synchronized on the WindowedArray.
     */
    protected void grow(long lVirtual)
        {
        int      cOldCapacity = getCapacity();
        int      cNewCapacity = checkCapacity(Math.max(cOldCapacity * 2,
                                    (int) (lVirtual - getFirstIndex()) + 3));
        Object[] aoOld        = getStore();
        Object[] aoNew        = new Object[cNewCapacity];
        int      iaStart      = getWindowIndex();
        int      cElements    = getWindowSize();
        int      iaEnd        = iaStart + cElements - 1;
        
        if (iaEnd >= cOldCapacity)
            {
            // the virtual array is "wrapped" around the end
            // of the real array
            int cPrewrapElements  = cOldCapacity - iaStart;
            int cPostwrapElements = iaEnd - cOldCapacity + 1;
            System.arraycopy(aoOld, iaStart, aoNew, 0, cPrewrapElements);
            System.arraycopy(aoOld, 0, aoNew, cPrewrapElements, cPostwrapElements);
            }
        else
            {
            System.arraycopy(aoOld, iaStart, aoNew, 0, cElements);
            }
        
        setStatsExpansions(getStatsExpansions() + 1);
        setStore(aoNew);
        setWindowIndex(0);
        }
    
    // Accessor for the property "Removed"
    /**
     * Return true if the specified index has been removed from the windowed
    * array.
     */
    public synchronized boolean isRemoved(long lVirtual)
        {
        return lVirtual <= getLastIndex() &&
              (lVirtual < getFirstIndex() || getStore()[translateIndex(lVirtual)] == REMOVED);
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
        setStore(new Object[getInitialCapacity()]);
        
        super.onInit();
        }
    
    /**
     * Remove an Object from the WindowedArray and return that Object. Note that
    * it is necessary to remove the elements at the front of the WindowedArray
    * to allow the FirstIndex to increase, thus allowing the WindowedArray to
    * store more values without resizing the Store.
    * 
    * @param lVirtual  the index of the element to remove
    * 
    * @return the Object that was removed or null if there were no Object at
    * that index
    * 
    * @throws IndexOutOfBoundsException if the index is negative or greater
    * than the LastIndex of the WindowedArray
     */
    public synchronized Object remove(long lVirtual)
        {
        if (lVirtual < 0L)
            {
            throw new IndexOutOfBoundsException("negative index is illegal: " + lVirtual);
            }
        
        long lvFirst = getFirstIndex();
        if (lVirtual < lvFirst)
            {
            // already removed
            return null;
            }
        
        long lvLast = getLastIndex();
        if (lVirtual > lvLast)
            {
            // not added yet
            throw new IndexOutOfBoundsException("remove beyond window (index=" +
                lVirtual + ", window last index=" + lvLast + ")");
            }
        
        // remove the stored value
        Object[] aoElement = getStore();
        int      iActual   = translateIndex(lVirtual);
        Object   oOrig     = aoElement[iActual];
        aoElement[iActual] = REMOVED;
        
        if (lVirtual == lvFirst)
            {
            // _assert(iActual == getWindowIndex());
        
            // remove all contiguous REMOVED elements
            int iaIndex = iActual;
            int cWindowElements = getWindowSize();
            while (cWindowElements > 0)
                {
                iActual = iaIndex;
                if (aoElement[iActual] != REMOVED)
                    {
                    break;
                    }
        
                // drop this element from the window
                aoElement[iActual] = null;
        
                // update the window index/location/extent
                ++lvFirst;
                iaIndex = translateIndex(lvFirst);
                --cWindowElements;
                }
        
            // store the new window location / extent
            setFirstIndex(lvFirst);
            setWindowIndex(iaIndex);
            setWindowSize(cWindowElements);
            }
        
        return oOrig == REMOVED ? null : oOrig;
        }
    
    /**
     * Store an Object in the WindowedArray at the specified index and return
    * the Object that was previously stored at that index.
    * 
    * @param lVirtual  the index of the element to store
    * @param o  the Object to store (could be null)
    * 
    * @return the Object that was previously at that index or null if there
    * were no Object at that index
    * 
    * @throws IndexOutOfBoundsException if the index is negative, less than the
    * FirstIndex, or if storing a value at that index would cause the Store to
    * exceed the MaximumCapacity
     */
    public synchronized Object set(long lVirtual, Object o)
        {
        if (lVirtual < 0L)
            {
            throw new IndexOutOfBoundsException("negative index is illegal: " + lVirtual);
            }
        
        int      iActual = ensureIndex(lVirtual);
        Object[] ao      = getStore();
        Object   oOrig   = ao[iActual];
        
        ao[iActual] = o;
        
        return oOrig == REMOVED ? null : oOrig;
        }
    
    // Accessor for the property "FirstIndex"
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
     */
    protected void setFirstIndex(long lVirtual)
        {
        __m_FirstIndex = lVirtual;
        }
    
    // Accessor for the property "FirstStuckIndex"
    /**
     * Setter for property FirstStuckIndex.<p>
    * The index of an element which appears to be stuck and causing the window
    * to grow uncontrollably.
    * 
    * @see #setWindowSize
     */
    protected void setFirstStuckIndex(long lIndex)
        {
        __m_FirstStuckIndex = lIndex;
        }
    
    // Accessor for the property "LastSizeWarningMillis"
    /**
     * Setter for property LastSizeWarningMillis.<p>
    * The time at which the last size warning was issued.
    * 
    * @see #setWindowSize
     */
    protected void setLastSizeWarningMillis(long ldtMillis)
        {
        __m_LastSizeWarningMillis = ldtMillis;
        }
    
    // Accessor for the property "StatsExpansions"
    /**
     * Setter for property StatsExpansions.<p>
    * The total number of times the Store has been expanded.
     */
    public void setStatsExpansions(int pStatsExpansions)
        {
        __m_StatsExpansions = pStatsExpansions;
        }
    
    // Accessor for the property "Store"
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
        __m_Store = ao;
        }
    
    // Accessor for the property "WindowIndex"
    /**
     * Setter for property WindowIndex.<p>
    * The WindowIndex is the 0-based offset into the Store at which the first
    * element of the WindowedArray is stored.
    * 
    * Note: Within the implementation actual indices are prefixed with "ia"
    * (int actual).
     */
    protected void setWindowIndex(int i)
        {
        __m_WindowIndex = i;
        }
    
    // Accessor for the property "WindowSize"
    /**
     * Setter for property WindowSize.<p>
    * The WindowSize is the number of elements that the  WindowedArray is
    * currently using in the Store.
     */
    protected void setWindowSize(int c)
        {
        __m_WindowSize = c;
        }
    
    // Declared at the super level
    /**
     * @return a String description of the WindowedArray's state, including its
    * contents
     */
    public synchronized String toString()
        {
        StringBuilder sb = new StringBuilder();
        
        sb.append(get_Name())
          .append('[')
          .append(formatStats())
          .append(']');
          
        for (long lVirtual = getFirstIndex(), lvLast = getLastIndex();
             lVirtual <= lvLast; ++lVirtual)
            {
            Object o = get(lVirtual);
            if (o != null)
                {        
                sb.append("\n[")
                  .append(lVirtual)
                  .append("]=\"")
                  .append(o)
                  .append('\"');
                }
            }
        
        return sb.toString();
        }
    
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
        // WindowIndex is the index of the 0th element in the window,
        // which is the location of the FirstIndex element of the
        // virtual array; the virtual array may "wrap" around the end
        // of the Store array
        return (getWindowIndex() + ((int) (lVirtual - getFirstIndex()))) % getCapacity();
        }
    }
