
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.Collections

package com.tangosol.coherence.component.util;

import com.tangosol.util.Base;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Collections
        extends    com.tangosol.coherence.component.Util
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public Collections()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Collections(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.Collections();
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
            clz = Class.forName("com.tangosol.coherence/component/util/Collections".replace('/', '.'));
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
     * Helper method for colThis.addAll(colThat)
     */
    public static boolean addAll(java.util.Collection colThis, java.util.Collection colThat)
        {
        // import java.util.Iterator;
        
        boolean fModified = false;
        for (Iterator iter = colThat.iterator(); iter.hasNext();)
            {
            if (colThis.add(iter.next()))
                {
                fModified = true;
                }
            }
        return fModified;
        }
    
    /**
     * Helper method for col.clear() that uses an iterator.
     */
    public static void clear(java.util.Collection col)
        {
        // import java.util.Iterator;
        
        for (Iterator iter = col.iterator(); iter.hasNext();)
            {
            iter.remove();
            }
        }
    
    /**
     * Helper method for colThis.containsAll(colThat)
     */
    public static boolean containsAll(java.util.Collection colThis, java.util.Collection colThat)
        {
        // import java.util.Iterator;
        
        for (Iterator iter = colThat.iterator(); iter.hasNext();)
            {
            if (!colThis.contains(iter.next()))
                {
                return false;
                }
            }
        return true;
        }
    
    /**
     * Helper method for mapThis.putAll(mapThat)
     */
    public static void putAll(java.util.Map mapThis, java.util.Map mapThat)
        {
        // import java.util.Iterator;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        for (Iterator iter = mapThat.entrySet().iterator(); iter.hasNext();)
            {
            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
        
            mapThis.put(entry.getKey(), entry.getValue());
            }
        }
    
    /**
     * Helper method for col.remove(o) that uses an iterator.
     */
    public static boolean remove(java.util.Collection col, Object o)
        {
        // import com.tangosol.util.Base;
        // import java.util.Iterator;
        
        for (Iterator iter = col.iterator(); iter.hasNext();)
            {
            if (Base.equals(iter.next(), o))
                {
                iter.remove();
                return true;
                }
            }
        
        return false;
        }
    
    /**
     * Helper method for colThis.removeAll(colThat)
     */
    public static boolean removeAll(java.util.Collection colThis, java.util.Collection colThat)
        {
        // import java.util.Iterator;
        
        boolean fModified = false;
        if (colThis.size() > colThat.size())
            {
            for (Iterator iter = colThat.iterator(); iter.hasNext();)
                {
                fModified |= colThis.remove(iter.next());
                }
            }
        else
            {
            for (Iterator iter = colThis.iterator(); iter.hasNext(); )
                {
                if (colThat.contains(iter.next()))
                    {
                    iter.remove();
                    fModified = true;
                    }
                }
            }
        
        return fModified;
        }
    
    /**
     * Helper method for colThis.retainAll(colThat)
     */
    public static boolean retainAll(java.util.Collection colThis, java.util.Collection colThat)
        {
        // import java.util.Iterator;
        
        boolean fModified = false;
        for (Iterator iter = colThis.iterator(); iter.hasNext();)
            {
            Object o = iter.next();
            if (!colThat.contains(o))
                {
                iter.remove();
                fModified = true;
                }
            }
        return fModified;
        }
    
    /**
     * Helper method for col.toArray() implementations
     */
    public static Object[] toArray(java.util.Collection col, Object[] ao)
        {
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        
        int c = col.size();
        if (ao == null)
            {
            ao = new Object[c];
            }
        else if (ao.length < c)
            {
            ao = (Object[])java.lang.reflect.Array.newInstance(
                ao.getClass().getComponentType(), c);
            }
        else if (ao.length > c)
            {
            ao[c] = null;
            }
        
        Iterator iter = col.iterator();
        for (int i = 0; i < c; i++)
            {
            try
                {
                ao[i] = iter.next();
                }
            catch (RuntimeException e) // NoSuchElement; IndexOutOfBounds
                {
                throw new ConcurrentModificationException(e.toString());
                }
            }
        
        return ao;
        }
    }
