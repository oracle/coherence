
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.protocol.InvocationServiceProtocol

package com.tangosol.coherence.component.net.extend.protocol;

import com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory;

/**
 * The InvocationService Protocol is used to execute Invocable tasks within a
 * remote Coherence cluster.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class InvocationServiceProtocol
        extends    com.tangosol.coherence.component.net.extend.Protocol
    {
    // ---- Fields declarations ----
    
    /**
     * Property Instance
     *
     * The singleton InvocationService Protocol instance.
     */
    private static InvocationServiceProtocol __s_Instance;
    
    private static void _initStatic$Default()
        {
        }
    
    // Static initializer (from _initStatic)
    static
        {
        _initStatic$Default();
        
        setInstance(new InvocationServiceProtocol());
        }
    
    // Default constructor
    public InvocationServiceProtocol()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public InvocationServiceProtocol(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        
        // state initialization: public and protected properties
        try
            {
            setVersionCurrent(1);
            setVersionSupported(1);
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
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
        return new com.tangosol.coherence.component.net.extend.protocol.InvocationServiceProtocol();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/protocol/InvocationServiceProtocol".replace('/', '.'));
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
    
    // Accessor for the property "Instance"
    /**
     * Getter for property Instance.<p>
    * The singleton InvocationService Protocol instance.
     */
    public static InvocationServiceProtocol getInstance()
        {
        return __s_Instance;
        }
    
    // Declared at the super level
    /**
     * Instantiate a new MessageFactory for the given version of this Protocol.
    * 
    * @param nVersion  the version of the Protocol that the returned
    * MessageFactory will use
    * 
    * @return a new MessageFactory for the given version of this Protocol
     */
    protected com.tangosol.coherence.component.net.extend.MessageFactory instantiateMessageFactory(int nVersion)
        {
        // import Component.Net.Extend.MessageFactory.InvocationServiceFactory;
        
        return new InvocationServiceFactory();
        }
    
    // Accessor for the property "Instance"
    /**
     * Setter for property Instance.<p>
    * The singleton InvocationService Protocol instance.
     */
    protected static void setInstance(InvocationServiceProtocol protocol)
        {
        __s_Instance = protocol;
        }
    }
