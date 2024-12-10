
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.message.request.InvocationServiceRequest

package com.tangosol.coherence.component.net.extend.message.request;

/**
 * Base component for all InvocationService Protocol Request messages.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class InvocationServiceRequest
        extends    com.tangosol.coherence.component.net.extend.message.Request
    {
    // ---- Fields declarations ----
    
    /**
     * Property InvocationService
     *
     * The target of this InvocationServiceRequest. This property must be set
     * by the Receiver before the run() method is called.
     */
    private transient com.tangosol.net.InvocationService __m_InvocationService;
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
    public InvocationServiceRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/message/request/InvocationServiceRequest".replace('/', '.'));
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
    
    // Accessor for the property "InvocationService"
    /**
     * Getter for property InvocationService.<p>
    * The target of this InvocationServiceRequest. This property must be set by
    * the Receiver before the run() method is called.
     */
    public com.tangosol.net.InvocationService getInvocationService()
        {
        return __m_InvocationService;
        }
    
    // Accessor for the property "InvocationService"
    /**
     * Setter for property InvocationService.<p>
    * The target of this InvocationServiceRequest. This property must be set by
    * the Receiver before the run() method is called.
     */
    public void setInvocationService(com.tangosol.net.InvocationService service)
        {
        __m_InvocationService = service;
        }
    }
