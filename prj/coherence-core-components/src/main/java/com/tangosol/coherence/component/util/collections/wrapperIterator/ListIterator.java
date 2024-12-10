
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.collections.wrapperIterator.ListIterator

package com.tangosol.coherence.component.util.collections.wrapperIterator;

/*
* Integrates
*     java.util.ListIterator
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ListIterator
        extends    com.tangosol.coherence.component.util.collections.WrapperIterator
        implements java.util.ListIterator
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public ListIterator()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ListIterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.collections.wrapperIterator.ListIterator();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return ListIterator.class;
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
    
    //++ java.util.ListIterator integration
    // Access optimization
    // properties integration
    // methods integration
    public void add(Object o)
        {
        ((java.util.ListIterator) getIterator()).add(o);
        }
    public boolean hasPrevious()
        {
        return ((java.util.ListIterator) getIterator()).hasPrevious();
        }
    public int nextIndex()
        {
        return ((java.util.ListIterator) getIterator()).nextIndex();
        }
    public Object previous()
        {
        return ((java.util.ListIterator) getIterator()).previous();
        }
    public int previousIndex()
        {
        return ((java.util.ListIterator) getIterator()).previousIndex();
        }
    public void set(Object o)
        {
        ((java.util.ListIterator) getIterator()).set(o);
        }
    //-- java.util.ListIterator integration
    
    // Declared at the super level
    public static com.tangosol.coherence.component.util.collections.WrapperIterator instantiate(java.util.Iterator iter)
        {
        return instantiate((java.util.ListIterator) iter);
        }
    
    public static ListIterator instantiate(java.util.ListIterator iter)
        {
        ListIterator iterWrapper = new ListIterator();
        iterWrapper.setIterator(iter);
        return iterWrapper;
        }
    }
