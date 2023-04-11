
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.wrapperMBean.WrapperEmitterMBean

package com.tangosol.coherence.component.manageable.modelAdapter.wrapperMBean;

/**
 * DynamicMBean implementation for a WrapperModel which generate Notifications.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class WrapperEmitterMBean
        extends    com.tangosol.coherence.component.manageable.modelAdapter.WrapperMBean
        implements javax.management.NotificationEmitter
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public WrapperEmitterMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public WrapperEmitterMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.wrapperMBean.WrapperEmitterMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/wrapperMBean/WrapperEmitterMBean".replace('/', '.'));
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
    /**
     * Auto-generated for concrete Components.
     */
    protected Object[] get_ComponentInfo()
        {
        return new Object[]
            {
            "DynamicMBean implementation for a WrapperModel which generate Notifications.",
            null,
            };
        }
    /**
     * Auto-generated for concrete Components, for Properties that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - at least one public accessor
     */
    protected java.util.Map get_PropertyInfo()
        {
        java.util.Map mapInfo = super.get_PropertyInfo();
        
        return mapInfo;
        }
    /**
     * Auto-generated for concrete Components, for Behaviors that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - public access
     */
    protected java.util.Map get_MethodInfo()
        {
        java.util.Map mapInfo = super.get_MethodInfo();
        
        return mapInfo;
        }
    }
