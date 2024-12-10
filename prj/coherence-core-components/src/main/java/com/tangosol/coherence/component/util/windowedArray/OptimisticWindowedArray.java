
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.windowedArray.OptimisticWindowedArray

package com.tangosol.coherence.component.util.windowedArray;

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
 * The optimistic version of the WindowedArray provides optimistic (dirty) read
 * methods.  The get and getAll implementations are unsynchronized, and thus
 * may return stale data.  The general usage pattern would be to do an
 * unsynchronized read, and if the desired items are not found, to repeat the
 * operation while synchronized on the windowed array.
 * 
 * This is an abstract component, any concrete implementation must implement
 * the assignIndexToValue and retrieveIndexFromValue methods.  These methods
 * are what makes the optimistic reads possible, as they provide a mechanism to
 * validate the guess as to the proper indices for a desired value.  Thus, the
 * OptimisticWindowedArray can only store values which allow for derivation of
 * their associated windowed (virtual) index.  See the getAll method for
 * further details on the implementation.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class OptimisticWindowedArray
        extends    com.tangosol.coherence.component.util.WindowedArray
    {
    // ---- Fields declarations ----
    
    // Initializing constructor
    public OptimisticWindowedArray(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/windowedArray/OptimisticWindowedArray".replace('/', '.'));
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
    public synchronized long add(Object o)
        {
        long lVirtual = getSize();
        int  iActual  = ensureIndex(lVirtual);
        if (o != null)
            {
            // we assign first to so that the index is present prior to the
            // item being store in the cache.    
            assignIndexToValue(lVirtual, o);
            }
        getStore()[iActual] = o;
        return lVirtual;
        }
    
    /**
     * Inform the value of its virtual index prior to adding it to the array. 
    * This is used by the add() method as the value cannot know it's index
    * prior to calling the add method.
     */
    protected void assignIndexToValue(long lVirtual, Object o)
        {
        }
    
    // Declared at the super level
    /**
     * Obtain an Object from the WindowedArray at the specified index.  As this
    * method is not synchronized the returned value may be out-of-date.  The
    * implementation guarantees that if a value is returned it was at one time
    * at the specified index.
    * 
    * 
    * @param i  the index of the element to obtain
    * 
    * @return the Object at the requested index, or null if the requested index
    * is outside of the bounds of the WindowedArray
    * 
    * @throws IndexOutOfBoundsException if the index is negative
     */
    public Object get(long lVirtual)
        {
        // see getAll for details of how the dirty lookup works
        
        if (lVirtual < 0L)
            {
            throw new IndexOutOfBoundsException("negative index is illegal: " + lVirtual);
            }
        
        Object[] aoStore   = getStore();
        int      iaAnchor  = -1;             // index within store (actual)
        long     lvAnchor  = -1;             // windowed anchor index (virtual)
        int      cCapacity = aoStore.length; // can't trust getCapacity()
        
        // Find any valid item in the store (starting at the WindowIndex)
        int iaStart = getWindowIndex() % cCapacity;
        int iaEnd   = (cCapacity + iaStart - 1) % cCapacity;
        for (int j = iaStart; j != iaEnd; j = ((j + 1) % cCapacity))
            {
            Object o = aoStore[j];
            if (o != null && o != REMOVED)
                {
                lvAnchor = retrieveIndexFromValue(o);
                if (lvAnchor >= 0)
                    {
                    iaAnchor = j;
                    break;
                    }
                } 
            }
        
        if (iaAnchor == -1)
            {
            // no anchor, so no values either
            return null;
            }
        
        long lDelta = lvAnchor - lVirtual;
        
        if (Math.abs(lDelta) > cCapacity)
            {
            // not in range
            return null;
            }
        
        long   lAdjust  = cCapacity + iaAnchor;    
        int    iaStore  = (int) ((lAdjust - lDelta) % cCapacity);
        Object o        = aoStore[iaStore];
        return (o == null || o == REMOVED || retrieveIndexFromValue(o) != lVirtual) ? null : o;
        }
    
    // Declared at the super level
    /**
     * Obtain a set of Objects from the WindowedArray at the specified indices. 
    * As this method is not synchronized the returned values may be
    * out-of-date.  The implementation guarantees that if a value is returned
    * it was at one time at the specified index.
    * 
    * @param aIndex  the array of indices to obtain
    * @param aObject the array in which to insert the values
    * @param cEntries the number of indices to process, thus allowing longer
    * arrays then necessary to be supplied
    * 
    * @throws IndexOutOfBoundsException if any of the indices is negative
    * 
    * @return the number of non-null items which were found.
     */
    public int getAll(long[] alIndex, int cEntries, Object[] aoResult)
        {
        // do a dirty lookup against the store, this works as the relative position of items will not change
        // unless the store is resized, which results in a new array anyhow.
         
        Object[] aoStore   = getStore();
        int      iaAnchor  = -1; // index within store (actual)
        long     lvAnchor  = -1; // windowed anchor index (virtual)
        int      cCapacity = aoStore.length; // can't trust getCapacity()
        
        // Find any valid item in the store (starting at the WindowIndex)
        int iaStart = getWindowIndex() % cCapacity;
        int iaEnd   = (cCapacity + iaStart - 1) % cCapacity;
        for (int i = iaStart; i != iaEnd; i = ((i + 1) % cCapacity))
            {
            Object o = aoStore[i];
            if (o != null && o != REMOVED)
                {
                lvAnchor = retrieveIndexFromValue(o);
                if (lvAnchor >= 0L)
                    {
                    iaAnchor = i;
                    // NOTE: even if the anchor is subsequently removed while we are
                    // basing our indices off of its index we are still OK, since the elements
                    // in the array will be relative to it unless all elements in the array
                    // are removed while we were searching, in which case our elements are
                    // gone anyhow
                    break;
                    }
                } 
            }
        
        if (iaAnchor == -1)
            {      
            // there was nothing left in the store
            // all the results are null
            for (int i = 0; i < cEntries; ++i)
                {
                aoResult[i] = null;
                }
            return 0;
            }
        
        int  cFound = 0;
        long lAdjust = cCapacity + iaAnchor;
        for (int i = 0; i < cEntries; ++i)
            {
            long lVirtual = alIndex[i];
            if (lVirtual < 0L)
                {
                throw new IndexOutOfBoundsException("negative index is illegal: " + lVirtual);
                }
        
            long lDelta = lvAnchor - lVirtual;
        
            if (Math.abs(lDelta) > cCapacity)
                {
                // not in range
                aoResult[i] = null;
                continue;
                }
                
            int    iaStore = (int) ((lAdjust - lDelta) % cCapacity);   
            Object o       = aoStore[iaStore];
            if (o == null || o == REMOVED || retrieveIndexFromValue(o) != lVirtual)
                {
                // the getIndexFromValue check is performed to detect the case
                // where the value was removed and subsequently overwritten
                aoResult[i] = null;
                }
            else
                {
                aoResult[i] = o;
                ++cFound;
                }
            }
        return cFound;
        }
    
    /**
     * Get the virtual index from the value.  An implementation of this method
    * must be supplied based on the type of data to be stored within the array.
    *  It requires that there be a way to compute the index from the value. 
    * For instance when Messages are used as the value, the index is the
    * Message Id.
     */
    protected long retrieveIndexFromValue(Object o)
        {
        return 0L;
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
    public synchronized Object set(long lVirtual, Object o)
        {
        // we assign first to so that the index is present prior to the
        // item being store in the cache.
        assignIndexToValue(lVirtual, o);
        return super.set(lVirtual, o);
        }
    }
