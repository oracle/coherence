
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeySetRequest

package com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest;

import java.util.ArrayList;

/**
 * Base component for all NamedCache Protocol Request messages that include a
 * collection of keys.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class KeySetRequest
        extends    com.tangosol.coherence.component.net.extend.message.request.NamedCacheRequest
    {
    // ---- Fields declarations ----
    
    /**
     * Property KeySet
     *
     * The Collection of keys associated with this KeySetRequest.
     */
    private java.util.Collection __m_KeySet;
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
        __mapChildren.put("Status", com.tangosol.coherence.component.net.extend.message.Request.Status.get_CLASS());
        }
    
    // Initializing constructor
    public KeySetRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/message/request/namedCacheRequest/KeySetRequest".replace('/', '.'));
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
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        return super.getDescription() + ", KeySet=" + getKeySet();
        }
    
    // Accessor for the property "KeySet"
    /**
     * Getter for property KeySet.<p>
    * The Collection of keys associated with this KeySetRequest.
     */
    public java.util.Collection getKeySet()
        {
        return __m_KeySet;
        }
    
    // Declared at the super level
    public void readExternal(com.tangosol.io.pof.PofReader in)
            throws java.io.IOException
        {
        // import java.util.ArrayList;
        
        super.readExternal(in);
        
        setKeySet(in.readCollection(1, new ArrayList()));
        }
    
    // Accessor for the property "KeySet"
    /**
     * Setter for property KeySet.<p>
    * The Collection of keys associated with this KeySetRequest.
     */
    public void setKeySet(java.util.Collection colKeys)
        {
        __m_KeySet = colKeys;
        }
    
    // Declared at the super level
    public void writeExternal(com.tangosol.io.pof.PofWriter out)
            throws java.io.IOException
        {
        super.writeExternal(out);
        
        out.writeCollection(1, getKeySet());
        
        // release state
        setKeySet(null);
        }
    }
