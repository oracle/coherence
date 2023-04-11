
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.safeService.SafeInvocationService

package com.tangosol.coherence.component.util.safeService;

/*
* Integrates
*     com.tangosol.net.InvocationService
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SafeInvocationService
        extends    com.tangosol.coherence.component.util.SafeService
        implements com.tangosol.net.InvocationService
    {
    // ---- Fields declarations ----
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
        __mapChildren.put("EnsureServiceAction", com.tangosol.coherence.component.util.SafeService.EnsureServiceAction.get_CLASS());
        __mapChildren.put("StartAction", com.tangosol.coherence.component.util.SafeService.StartAction.get_CLASS());
        __mapChildren.put("Unlockable", com.tangosol.coherence.component.util.SafeService.Unlockable.get_CLASS());
        }
    
    // Default constructor
    public SafeInvocationService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SafeInvocationService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setLock(new java.util.concurrent.locks.ReentrantLock());
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setSafeServiceState(0);
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
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
        return new com.tangosol.coherence.component.util.safeService.SafeInvocationService();
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
            clz = Class.forName("com.tangosol.coherence/component/util/safeService/SafeInvocationService".replace('/', '.'));
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
    
    //++ com.tangosol.net.InvocationService integration
    // Access optimization
    // properties integration
    // methods integration
    public void execute(com.tangosol.net.Invocable task, java.util.Set setMembers, com.tangosol.net.InvocationObserver observer)
        {
        ((com.tangosol.net.InvocationService) getRunningService()).execute(task, setMembers, observer);
        }
    public java.util.Map query(com.tangosol.net.Invocable task, java.util.Set setMembers)
        {
        return ((com.tangosol.net.InvocationService) getRunningService()).query(task, setMembers);
        }
    //-- com.tangosol.net.InvocationService integration
    }
