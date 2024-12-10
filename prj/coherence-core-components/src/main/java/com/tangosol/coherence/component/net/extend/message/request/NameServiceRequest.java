
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.message.request.NameServiceRequest

package com.tangosol.coherence.component.net.extend.message.request;

/**
 * Base component for all NameService Protocol Request messages.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class NameServiceRequest
        extends    com.tangosol.coherence.component.net.extend.message.Request
    {
    // ---- Fields declarations ----
    
    /**
     * Property NameService
     *
     * The target of this NameServiceRequest. This property must be set by the
     * Receiver before the run() method is called.
     */
    private transient com.tangosol.net.NameService __m_NameService;
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
    public NameServiceRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/message/request/NameServiceRequest".replace('/', '.'));
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
    
    // Accessor for the property "NameService"
    /**
     * Getter for property NameService.<p>
    * The target of this NameServiceRequest. This property must be set by the
    * Receiver before the run() method is called.
     */
    public com.tangosol.net.NameService getNameService()
        {
        return __m_NameService;
        }
    
    // Accessor for the property "NameService"
    /**
     * Setter for property NameService.<p>
    * The target of this NameServiceRequest. This property must be set by the
    * Receiver before the run() method is called.
     */
    public void setNameService(com.tangosol.net.NameService service)
        {
        __m_NameService = service;
        }
    }
