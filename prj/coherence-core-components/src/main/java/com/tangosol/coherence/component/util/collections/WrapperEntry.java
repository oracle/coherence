
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.collections.WrapperEntry

package com.tangosol.coherence.component.util.collections;

/*
* Integrates
*     java.util.Map$Entry
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class WrapperEntry
        extends    com.tangosol.coherence.component.util.Collections
        implements java.util.Map.Entry
    {
    // ---- Fields declarations ----
    
    /**
     * Property Entry
     *
     * Wrapped Entry
     */
    private transient java.util.Map.Entry __m_Entry;
    
    // Default constructor
    public WrapperEntry()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public WrapperEntry(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.collections.WrapperEntry();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return WrapperEntry.class;
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
    
    //++ java.util.Map.Entry integration
    // Access optimization
    // properties integration
    // methods integration
    public Object getKey()
        {
        return getEntry().getKey();
        }
    public Object getValue()
        {
        return getEntry().getValue();
        }
    public Object setValue(Object oValue)
        {
        return getEntry().setValue(oValue);
        }
    //-- java.util.Map.Entry integration
    
    // From interface: java.util.Map$Entry
    // Declared at the super level
    public boolean equals(Object obj)
        {
        if (obj instanceof WrapperEntry)
            {
            return getEntry().equals(
                ((WrapperEntry) obj).getEntry());
            }
        return false;
        }
    
    // Accessor for the property "Entry"
    /**
     * Getter for property Entry.<p>
    * Wrapped Entry
     */
    public java.util.Map.Entry getEntry()
        {
        return __m_Entry;
        }
    
    // From interface: java.util.Map$Entry
    // Declared at the super level
    public int hashCode()
        {
        return getEntry().hashCode();
        }
    
    public static WrapperEntry instantiate(java.util.Map.Entry entry)
        {
        WrapperEntry entryWrapper = new WrapperEntry();
        entryWrapper.setEntry(entry);
        return entryWrapper;
        }
    
    // Accessor for the property "Entry"
    /**
     * Setter for property Entry.<p>
    * Wrapped Entry
     */
    public void setEntry(java.util.Map.Entry entry)
        {
        _assert(entry != null && getEntry() == null, "Entry is not resettable");
        __m_Entry = (entry);
        }
    
    // Declared at the super level
    public String toString()
        {
        return super.toString() + ": " + String.valueOf(getEntry());
        }
    }
