
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.partialRequest.FilterRequest

package com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.partialRequest;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import java.io.IOException;

/**
 * DistributeCacheRequest is a base component for RequestMessage(s) used by the
 * partitioned cache service that are key set or filter based. Quite often a
 * collection of similar requests are sent in parallel and a client thread has
 * to wait for all of them to return.
 * 
 * PartialRequest is a DistributeCacheRequest that is constrained by a subset
 * of partitions owned by a single storage-enabled Member.
 * 
 * FilterRequest is a PartialRequest further constrained by a predicate
 * (Filter).
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class FilterRequest
        extends    com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest
    {
    // ---- Fields declarations ----
    
    /**
     * Property Filter
     *
     * The Filter object representing this request.
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
        __mapChildren.put("Poll", com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.PartialRequest.Poll.get_CLASS());
        }
    
    // Default constructor
    public FilterRequest()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public FilterRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.requestMessage.distributedCacheRequest.partialRequest.FilterRequest();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/requestMessage/distributedCacheRequest/partialRequest/FilterRequest".replace('/', '.'));
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
     * Instantiate a copy of this message. This is quite different from the
    * standard "clone" since only the "transmittable" portion of the message
    * (and none of the internal) state should be cloned.
     */
    public com.tangosol.coherence.component.net.Message cloneMessage()
        {
        FilterRequest msg = (FilterRequest) super.cloneMessage();
        
        msg.setFilter(getFilter());
        
        return msg;
        }
    
    // Accessor for the property "Filter"
    /**
     * Getter for property Filter.<p>
    * The Filter object representing this request.
     */
    public com.tangosol.util.Filter getFilter()
        {
        return __m_Filter;
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Filter;
        
        super.read(input);
        
        try
            {
            setFilter((Filter) readObject(input));
            }
        catch (Throwable t)
            {
            setReadException(Base.ensureRuntimeException(t));
            }
        }
    
    // Accessor for the property "Filter"
    /**
     * Setter for property Filter.<p>
    * The Filter object representing this request.
     */
    public void setFilter(com.tangosol.util.Filter filter)
        {
        __m_Filter = filter;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import java.io.IOException;
        
        super.write(output);
        
        try
            {
            writeObject(output, getFilter());
            }
        catch (IOException e)
            {
            _trace("Filter is not serializable: " + getFilter(), 1);
            throw e;
            }
        }
    }
