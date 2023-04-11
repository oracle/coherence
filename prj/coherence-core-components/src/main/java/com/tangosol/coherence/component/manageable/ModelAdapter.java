
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.ModelAdapter

package com.tangosol.coherence.component.manageable;

import com.tangosol.coherence.component.net.management.Model;
import com.tangosol.util.WrapperException;
import java.lang.reflect.InvocationTargetException;
import javax.management.MBeanException;

/**
 * Manageable component is a DynamicMBean implementation. 
 * 
 * For non-abstract components It automatically generates:
 *   1) MBeanInfo based on the component doc.
 *       Note: remember not to use the asterisk ('); use (`) instead.
 * 
 *   2) AttributeInfo for properties that:
 *      - are instance (non static);
 *      - not from super;
 *      - have names _not_ starting with underscore;
 *      - have at least one public accessor.
 * 
 *   3) MethodInfo for methods that:
 *      - are instance (non static);
 *      - have manual origin; not from super;
 *      - have names _not_ starting with underscore;
 *      - have public access.
 * 
 * As of Coherence 12.2.1, the doc for component, properties and methods could
 * have an optional @descriptor tag at the end. The format of that tag is:
 * 
 * @descriptor key1=value1,key2=value2,...
 * 
 * If this tag exists, the corresponding Descriptor will be added to the
 * MBeanInfo, AttributeInfo or MethodInfo.
 * ModelAdapter component is an abstract root of the MBeans that route all
 * their invocations to a corresponding Model component. The naming convention
 * for the corresponding Model component properties and methods is:
 *  - for each Attribute there should be an equivalent property
 *  - for each Method there should be an equivalent method
 * 
 * As of Coherence 3.6, the ModelAdapter provides the base implementation for
 * NotificationEmitter interface methods, but does not declare the interface
 * itself. (Due to a TDE bug those methods cannot be protected.) To support
 * notifications,  the sub-components need to declare the NotificationEmitter
 * interface and define values for _Notification* properties and make them
 * "visible" ("advanced" visibility is the default).
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class ModelAdapter
        extends    com.tangosol.coherence.component.Manageable
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Model
     *
     * The underlying managed resource Model.
     */
    private transient com.tangosol.coherence.component.net.management.Model __m__Model;
    
    // Initializing constructor
    public ModelAdapter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/ModelAdapter".replace('/', '.'));
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
        
        // property RefreshTime
            {
            mapInfo.put("RefreshTime", new Object[]
                {
                "The timestamp when this model was last retrieved from a corresponding node. For local servers it is the local time.",
                "getRefreshTime",
                null,
                "Ljava/util/Date;",
                null,
                });
            }
        
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
     * Base implementation.
     */
    public void addNotificationListener(javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
        {
        get_Model()._addNotificationListener(listener, filter, handback);
        }
    
    // Declared at the super level
    /**
     * Auto-generated for concrete Components.
     */
    protected Object[] get_ComponentInfo()
        {
        return new Object[] {"Model-driven MBean"};
        }
    
    // Accessor for the property "_Model"
    /**
     * Getter for property _Model.<p>
    * The underlying managed resource Model.
     */
    public com.tangosol.coherence.component.net.management.Model get_Model()
        {
        return __m__Model;
        }
    
    // Accessor for the property "RefreshTime"
    /**
     * Getter for property RefreshTime.<p>
    * The timestamp when this model was last retrieved from a corresponding
    * node. For local servers it is the local time.
     */
    public java.util.Date getRefreshTime()
        {
        return null;
        }
    
    // Declared at the super level
    /**
     * Invoke the method and operation on the underlying model.
     */
    protected Object invoke(int nOp, Object oTarget, String sMethod, Object[] aoParam)
            throws javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        return invoke(nOp, oTarget, sMethod, aoParam, null);
        }
    
    // Declared at the super level
    /**
     * Invoke the method and operation on the underlying model.
     */
    protected Object invoke(int nOp, Object oTarget, String sMethod, Object[] aoParam, String[] asSignature)
            throws javax.management.MBeanException,
                   javax.management.ReflectionException
        {
        // import Component.Net.Management.Model;
        // import com.tangosol.util.WrapperException;
        // import java.lang.reflect.InvocationTargetException;
        // import javax.management.MBeanException;
        
        Model model = get_Model();
        try
            {
            return model.invoke(nOp, sMethod, aoParam, asSignature);
            }
        catch (Throwable e)
            {
            while (true)
                {
                if (e instanceof InvocationTargetException)
                    {
                    e = ((InvocationTargetException) e).getTargetException();
                    }
                else if (e instanceof WrapperException)
                    {
                    e = ((WrapperException) e).getOriginalException();
                    }
                else
                    {
                    break;
                    }
                }
        
            String sMsg = "Exception during method invocation \"" + sMethod +
                          "\" at " + model;
        
            if (e instanceof Error)
                {
                // RuntimeErrorException and RuntmeMBeanException do not
                // show up correctly in the HttpAdapter...
                e = new WrapperException(e);
                }
            throw new MBeanException((Exception) e, sMsg);
            }
        }
    
    // Declared at the super level
    /**
     * Base implementation.
     */
    public void removeNotificationListener(javax.management.NotificationListener listener)
            throws javax.management.ListenerNotFoundException
        {
        get_Model()._removeNotificationListener(listener);
        }
    
    // Declared at the super level
    /**
     * Base implementation.
     */
    public void removeNotificationListener(javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
            throws javax.management.ListenerNotFoundException
        {
        get_Model()._removeNotificationListener(listener, filter, handback);
        }
    
    // Accessor for the property "_Model"
    /**
     * Setter for property _Model.<p>
    * The underlying managed resource Model.
     */
    public void set_Model(com.tangosol.coherence.component.net.management.Model model)
        {
        // import Component.Net.Management.Model;
        
        Model modelPrev = get_Model();
        
        __m__Model = (model);
        
        if (model != null && modelPrev != null && model != modelPrev &&
            modelPrev.is_SubscribedTo())
            {
            model._transferSubscriptions(modelPrev);
            }
        }
    
    // Declared at the super level
    /**
     * Setter for property _NotificationDescription.<p>
    * The description of the Notification that could be emitted by this MBean.
     */
    public void set_NotificationDescription(String sDescr)
        {
        super.set_NotificationDescription(sDescr);
        }
    
    // Declared at the super level
    /**
     * Setter for property _NotificationName.<p>
    * Then Notification name (class). According to the JMX contracts, this
    * should be a fully qualified Java class name of the corresponding
    * notification. Currently, only a single Notification class per MBean is
    * supported.
     */
    public void set_NotificationName(String sName)
        {
        super.set_NotificationName(sName);
        }
    
    // Declared at the super level
    /**
     * Setter for property _NotificationType.<p>
    * An array of Notification types for this MBean.
     */
    public void set_NotificationType(String[] asType)
        {
        super.set_NotificationType(asType);
        }
    
    // Declared at the super level
    /**
     * Setter for property MBeanInfo.<p>
    * Lazy calculation.
     */
    public void setMBeanInfo(javax.management.MBeanInfo infoBean)
        {
        super.setMBeanInfo(infoBean);
        }
    }
