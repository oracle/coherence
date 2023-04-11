
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.EmptyModel

package com.tangosol.coherence.component.net.management.model;

/**
 * EmptyModel is used as a temporary replacement for the "responsibility"
 * MBeans during the ownership transitions.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class EmptyModel
        extends    com.tangosol.coherence.component.net.management.Model
    {
    // ---- Fields declarations ----
    
    /**
     * Property MBeanInfo
     *
     * The MbeanInfo for the referring MBean.
     */
    private javax.management.MBeanInfo __m_MBeanInfo;
    
    // Default constructor
    public EmptyModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public EmptyModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.management.model.EmptyModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/EmptyModel".replace('/', '.'));
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
    
    // Accessor for the property "MBeanInfo"
    /**
     * Getter for property MBeanInfo.<p>
    * The MbeanInfo for the referring MBean.
     */
    public javax.management.MBeanInfo getMBeanInfo()
        {
        return __m_MBeanInfo;
        }
    
    // Declared at the super level
    /**
     * Invoke the method with the specified name and parameters on the MBean
    * represented by this model.
     */
    public Object invoke(int nOp, String sName, Object[] aoParam, String[] asSignature)
            throws java.lang.IllegalAccessException,
                   java.lang.NoSuchMethodException,
                   java.lang.reflect.InvocationTargetException,
                   javax.management.MBeanException
        {
        throw new NoSuchMethodException("The target is unavailable");
        }
    
    // Accessor for the property "MBeanInfo"
    /**
     * Setter for property MBeanInfo.<p>
    * The MbeanInfo for the referring MBean.
     */
    public void setMBeanInfo(javax.management.MBeanInfo info)
        {
        __m_MBeanInfo = info;
        }
    }
