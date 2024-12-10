
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.collections.WrapperIterator

package com.tangosol.coherence.component.util.collections;

/*
* Integrates
*     java.util.Iterator
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class WrapperIterator
        extends    com.tangosol.coherence.component.util.Collections
        implements java.util.Enumeration,
                   java.util.Iterator
    {
    // ---- Fields declarations ----
    
    /**
     * Property Iterator
     *
     * Wrapped Iterator
     */
    private transient java.util.Iterator __m_Iterator;
    
    // Default constructor
    public WrapperIterator()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public WrapperIterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.collections.WrapperIterator();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return WrapperIterator.class;
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
    
    //++ java.util.Iterator integration
    // Access optimization
    // properties integration
    // methods integration
    public boolean hasNext()
        {
        return getIterator().hasNext();
        }
    public Object next()
        {
        return getIterator().next();
        }
    public void remove()
        {
        getIterator().remove();
        }
    //-- java.util.Iterator integration
    
    // Declared at the super level
    public boolean equals(Object obj)
        {
        if (obj instanceof WrapperIterator)
            {
            return getIterator().equals(
                ((WrapperIterator) obj).getIterator());
            }
        return false;
        }
    
    // Accessor for the property "Iterator"
    /**
     * Getter for property Iterator.<p>
    * Wrapped Iterator
     */
    public java.util.Iterator getIterator()
        {
        return __m_Iterator;
        }
    
    // Declared at the super level
    public int hashCode()
        {
        return getIterator().hashCode();
        }
    
    // From interface: java.util.Enumeration
    public boolean hasMoreElements()
        {
        return hasNext();
        }
    
    public static WrapperIterator instantiate(java.util.Iterator iter)
        {
        WrapperIterator iterWrapper = new WrapperIterator();
        iterWrapper.setIterator(iter);
        return iterWrapper;
        }
    
    // From interface: java.util.Enumeration
    public Object nextElement()
        {
        return next();
        }
    
    // Accessor for the property "Iterator"
    /**
     * Setter for property Iterator.<p>
    * Wrapped Iterator
     */
    public void setIterator(java.util.Iterator iter)
        {
        _assert(iter != null && getIterator() == null, "Iterator is not resettable");
        __m_Iterator = (iter);
        }
    
    // Declared at the super level
    public String toString()
        {
        return super.toString() + ": " + String.valueOf(getIterator());
        }
    }
