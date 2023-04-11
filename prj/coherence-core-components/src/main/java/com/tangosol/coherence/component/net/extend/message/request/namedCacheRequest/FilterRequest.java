
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.FilterRequest

package com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest;

import com.tangosol.util.Filter;

/**
 * Base component for all NamedCache Protocol Request messages that include a
 * com.tangosol.util.Filter.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class FilterRequest
        extends    com.tangosol.coherence.component.net.extend.message.request.NamedCacheRequest
    {
    // ---- Fields declarations ----
    
    /**
     * Property Filter
     *
     * The Filter associated with this FilterRequest.
     */
    private com.tangosol.util.Filter __m_Filter;
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
    public FilterRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/message/request/namedCacheRequest/FilterRequest".replace('/', '.'));
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
        return super.getDescription() + ", Filter=" + getFilter();
        }
    
    // Accessor for the property "Filter"
    /**
     * Getter for property Filter.<p>
    * The Filter associated with this FilterRequest.
     */
    public com.tangosol.util.Filter getFilter()
        {
        return __m_Filter;
        }
    
    // Declared at the super level
    public void readExternal(com.tangosol.io.pof.PofReader in)
            throws java.io.IOException
        {
        // import com.tangosol.util.Filter;
        
        super.readExternal(in);
        
        setFilter((Filter) in.readObject(1));
        }
    
    // Accessor for the property "Filter"
    /**
     * Setter for property Filter.<p>
    * The Filter associated with this FilterRequest.
     */
    public void setFilter(com.tangosol.util.Filter filter)
        {
        __m_Filter = filter;
        }
    
    // Declared at the super level
    public void writeExternal(com.tangosol.io.pof.PofWriter out)
            throws java.io.IOException
        {
        super.writeExternal(out);
        
        out.writeObject(1, getFilter());
        }
    }
