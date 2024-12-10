
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.WrapperMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

import com.tangosol.coherence.component.net.management.model.EmptyModel;
import com.tangosol.coherence.component.net.management.model.RemoteModel;
import com.tangosol.coherence.component.net.management.model.localModel.WrapperModel;
import com.tangosol.util.ClassHelper;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;

/**
 * DynamicMBean implementation for a WrapperModel.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class WrapperMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public WrapperMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public WrapperMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.WrapperMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/WrapperMBean".replace('/', '.'));
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
            "DynamicMBean implementation for a WrapperModel.",
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
    
    /**
     * Validate the attribute name and prepend the is/get to the front of a
    * property.
     */
    protected String findAttributeGetter(javax.management.MBeanInfo infoBean, String sName)
            throws javax.management.AttributeNotFoundException
        {
        // import javax.management.AttributeNotFoundException;
        // import javax.management.MBeanAttributeInfo;
        
        String sMethod = null;
        
        MBeanAttributeInfo[] aInfo = infoBean.getAttributes();
        for (int i = 0, c = aInfo.length; i < c; i++)
            {
            MBeanAttributeInfo info = aInfo[i];
            if (info.getName().equals(sName))
                {
                if (info.isReadable())
                    {
                    sMethod = info.isIs() ? "is" + sName : "get" + sName;
                    }
                break;
                }
            }
        
        if (sMethod == null)
            {
            throw new AttributeNotFoundException("Attribute \""
                + sName
                + "\" cannot be retrieved in "
                + infoBean.getClassName());
            }
        
        return sMethod;
        }
    
    /**
     * Validate the Attribute name and prepend "set"
     */
    protected String findAttributeSetter(javax.management.MBeanInfo infoBean, String sName)
            throws javax.management.AttributeNotFoundException
        {
        // import javax.management.AttributeNotFoundException;
        // import javax.management.MBeanAttributeInfo;
        
        String sMethod = null;
        
        MBeanAttributeInfo[] aInfo = infoBean.getAttributes();
        for (int i = 0, c = aInfo.length; i < c; i++)
            {
            MBeanAttributeInfo info = aInfo[i];
            if (info.getName().equals(sName))
                {
                if (info.isWritable())
                    {
                    sMethod = "set" + sName;
                    }
                break;
                }
            }
        
        if (sMethod == null)
            {
            throw new AttributeNotFoundException("Attribute \""
                + sName
                + "\" cannot be set in "
                + infoBean.getClassName());
            }
        
        return sMethod;
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
        
        if (!isDynamic(get_Model()))
            {
            // Append the "is/get" for non-DynamicMBeans.
            sName = findAttributeGetter(getMBeanInfo(), sName);
            }
        
        return invoke(OP_GET, oTarget, sName, ClassHelper.VOID, null);
        }
    
    /**
     * Check if the specified Model represents a DynamicMBean.
    * 
    * @param model  an instance of either WrapperModel or RemoteModel
    * 
    * @see set_Model
     */
    protected static boolean isDynamic(com.tangosol.coherence.component.net.management.Model model)
        {
        // import Component.Net.Management.Model.EmptyModel;
        // import Component.Net.Management.Model.LocalModel.WrapperModel;
        // import Component.Net.Management.Model.RemoteModel;
        
        if (model instanceof EmptyModel)
            {
            return true;
            }
        
        WrapperModel modelWrapper = (WrapperModel) 
            (model instanceof WrapperModel ? model : ((RemoteModel) model).getSnapshot());
        
        return modelWrapper.isDynamic();
        }
    
    // Declared at the super level
    /**
     * Set the underlying model and update the associated MBeanInfo.
     */
    public void set_Model(com.tangosol.coherence.component.net.management.Model model)
        {
        // import Component.Net.Management.Model.EmptyModel;
        // import Component.Net.Management.Model.LocalModel.WrapperModel;
        // import Component.Net.Management.Model.RemoteModel;
        
        _assert(model != null);
        
        super.set_Model(model);
        
        if (model instanceof RemoteModel)
            {
            model = ((RemoteModel) model).getSnapshot();
            }
        
        if (model instanceof WrapperModel)
            {
            setMBeanInfo(((WrapperModel) model).getMBeanInfo());
            }
        else if (model instanceof EmptyModel)
            {
            setMBeanInfo(((EmptyModel) model).getMBeanInfo());
            }
        else
            {
            throw new IllegalArgumentException("Unsupported model " + model);
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
        if (!isDynamic(get_Model()))
            {
            // Append the "set" for non-DynamicMBeans.
            sName = findAttributeSetter(getMBeanInfo(), sName);
            }
        
        invoke(OP_SET, oTarget, sName, new Object[] {oValue});
        }
    }
