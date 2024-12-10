
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.memberSet.EmptyMemberSet

package com.tangosol.coherence.component.net.memberSet;

import com.tangosol.util.NullImplementation;

/**
 * An empty and immutable MemberSet.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class EmptyMemberSet
        extends    com.tangosol.coherence.component.net.MemberSet
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
        __mapChildren.put("Iterator", com.tangosol.coherence.component.net.MemberSet.Iterator.get_CLASS());
        }
    
    // Default constructor
    public EmptyMemberSet()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public EmptyMemberSet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            throw new IllegalStateException("A singleton for \"EmptyMemberSet\" has already been set");
            }
        __singleton = this;
        
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
        com.tangosol.coherence.Component singleton = __singleton;
        
        if (singleton == null)
            {
            singleton = new com.tangosol.coherence.component.net.memberSet.EmptyMemberSet();
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
            clz = Class.forName("com.tangosol.coherence/component/net/memberSet/EmptyMemberSet".replace('/', '.'));
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
    public boolean add(Object o)
        {
        throw new UnsupportedOperationException();
        }
    
    // Declared at the super level
    public boolean addAll(java.util.Collection collection)
        {
        throw new UnsupportedOperationException();
        }
    
    // Declared at the super level
    public void clear()
        {
        return;
        }
    
    // Declared at the super level
    public boolean contains(int nId)
        {
        return false;
        }
    
    // Declared at the super level
    public boolean contains(Object o)
        {
        return false;
        }
    
    // Declared at the super level
    public boolean containsAll(java.util.Collection collection)
        {
        return collection.isEmpty();
        }
    
    // Declared at the super level
    public boolean isEmpty()
        {
        return true;
        }
    
    // Declared at the super level
    public java.util.Iterator iterator()
        {
        // import com.tangosol.util.NullImplementation;
        
        return NullImplementation.getIterator();
        }
    
    // Declared at the super level
    public boolean remove(Object o)
        {
        return false;
        }
    
    // Declared at the super level
    public boolean removeAll(java.util.Collection collection)
        {
        return false;
        }
    
    // Declared at the super level
    public boolean retainAll(java.util.Collection collection)
        {
        return false;
        }
    
    // Declared at the super level
    public int size()
        {
        return 0;
        }
    
    // Declared at the super level
    public Object[] toArray()
        {
        return new Object[0];
        }
    
    // Declared at the super level
    public Object[] toArray(Object[] ao)
        {
        if (ao.length > 0)
            {
            ao[0] = null;
            }
        
        return ao;
        }
    }
