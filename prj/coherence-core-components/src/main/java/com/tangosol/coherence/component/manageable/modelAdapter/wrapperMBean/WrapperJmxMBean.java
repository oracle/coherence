
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.wrapperMBean.WrapperJmxMBean

package com.tangosol.coherence.component.manageable.modelAdapter.wrapperMBean;

import com.tangosol.coherence.component.net.management.model.LocalModel;
import com.tangosol.coherence.component.net.management.model.RemoteModel;
import com.tangosol.coherence.component.net.management.model.localModel.wrapperModel.WrapperJmxModel;
import com.tangosol.util.ClassHelper;

/**
 * Represents an MBean that is registered with an MBeanServer running on some
 * cluster node.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class WrapperJmxMBean
        extends    com.tangosol.coherence.component.manageable.modelAdapter.WrapperMBean
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public WrapperJmxMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public WrapperJmxMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.wrapperMBean.WrapperJmxMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/wrapperMBean/WrapperJmxMBean".replace('/', '.'));
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
            "Represents an MBean that is registered with an MBeanServer running on some cluster node.",
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
    
    // Declared at the super level
    /**
     * Get the specified Attribute on the MBean.
     */
    protected Object getAttribute(Object oTarget, String sName)
            throws javax.management.AttributeNotFoundException,
                   javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        // import com.tangosol.util.ClassHelper;
        
        return invoke(OP_GET, oTarget, sName, ClassHelper.VOID, null);
        }
    
    // Declared at the super level
    /**
     * Set the underlying model and update the associated MBeanInfo.
    * Overloaded the set Model to automatically update the MBean Information
    * when the model is updated.
     */
    public void set_Model(com.tangosol.coherence.component.net.management.Model model)
        {
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.Management.Model.LocalModel.WrapperModel.WrapperJmxModel;
        // import Component.Net.Management.Model.RemoteModel;
        
        super.set_Model(model);
        
        if (model instanceof RemoteModel)
            {
            LocalModel snapshot = ((RemoteModel) model).getSnapshot();
            if (snapshot instanceof WrapperJmxModel)
                {
                setMBeanInfo(((WrapperJmxModel) snapshot).getMBeanInfo());
                }
            }
        }
    
    // Declared at the super level
    /**
     * Set the specified Attribute on the MBean.
     */
    protected void setAttribute(Object oTarget, String sName, Object oValue)
            throws javax.management.AttributeNotFoundException,
                   javax.management.InvalidAttributeValueException,
                   javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        invoke(OP_SET, oTarget, sName, new Object[] {oValue});
        }
    }
