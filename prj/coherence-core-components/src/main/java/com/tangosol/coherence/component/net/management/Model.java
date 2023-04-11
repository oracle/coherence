
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.Model

package com.tangosol.coherence.component.net.management;

import com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder;
import com.tangosol.coherence.component.net.management.model.RemoteModel;
import com.tangosol.coherence.component.net.management.notificationHandler.LocalHandler;
import com.tangosol.coherence.config.Config;
import java.util.Iterator;
import java.util.Set;

/**
 * Model components implement the JMX-managed functionality of the
 * corresponding MBeans without being dependent on any JMX classes and could be
 * used both in-process and out-of-process (relative to an MBeanServer).
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Model
        extends    com.tangosol.coherence.component.net.Management
    {
    // ---- Fields declarations ----
    
    /**
     * Property _EMPTY
     *
     * Replacement for a null or empty string. The default value is "n/a".
     */
    private static transient String __s__EMPTY;
    
    /**
     * Property _HealthModelName
     *
     */
    private transient String __m__HealthModelName;
    
    /**
     * Property _LocalNotificationHandler
     *
     * The LocalHandler component managing local subscriptions.
     */
    private transient com.tangosol.coherence.component.net.management.notificationHandler.LocalHandler __m__LocalNotificationHandler;
    
    /**
     * Property _ModelName
     *
     * The canonical name of the Model as registered with the owner node's
     * Connector (LocalRegistry) and the primary Gateway's (LocalModels).
     */
    private transient String __m__ModelName;
    
    private static void _initStatic$Default()
        {
        }
    
    // Static initializer (from _initStatic)
    static
        {
        // import com.tangosol.coherence.config.Config;
        
        _initStatic$Default();
        
        // currently un-documented
        set_EMPTY(Config.getProperty("coherence.management.emptytag", "n/a"));
        }
    
    // Initializing constructor
    public Model(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/Model".replace('/', '.'));
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
     * Add the Notification listener to the model.
    * 
    * @return the LocalHolder representing the listener/filter/handback tuple
    * or null if such a registration already exists
     */
    public com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder _addNotificationListener(javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
        {
        // import Component.Net.Management.ListenerHolder.LocalHolder;
        
        LocalHolder holder = LocalHolder.create(listener, filter, handback);
        
        return _ensureLocalNotificationHandler().subscribe(holder) ? holder : null;
        }
    
    /**
     * Obtain the local notification handler.  if one does not exist it is
    * created.  This should only occur on subscription.
     */
    protected synchronized com.tangosol.coherence.component.net.management.notificationHandler.LocalHandler _ensureLocalNotificationHandler()
        {
        // import Component.Net.Management.NotificationHandler.LocalHandler;
        
        LocalHandler handler = get_LocalNotificationHandler();
        if (handler == null)
            {
            handler = new LocalHandler();
            handler.setName(get_ModelName());
            set_LocalNotificationHandler(handler);
            }
        
        return handler;
        }
    
    /**
     * Process the Notification for this model.
     */
    public void _handleNotification(javax.management.Notification notification)
        {
        // import Component.Net.Management.NotificationHandler.LocalHandler;
        
        LocalHandler handler = get_LocalNotificationHandler();
        if (handler != null)
            {
            handler.handleNotification(notification);
            }
        }
    
    /**
     * Remove all the notifications for the specified listener from the model.
    * 
    * @return a Set<LocalHolder> of holders associated with the specified
    * listener
     */
    public java.util.Set _removeNotificationListener(javax.management.NotificationListener listener)
        {
        // import Component.Net.Management.NotificationHandler.LocalHandler;
        
        LocalHandler handler = get_LocalNotificationHandler();
        return handler == null ? null : handler.unsubscribe(listener);
        }
    
    /**
     * Remove the Notification Listener from the model.
    * 
    * @return the LocalHolder representing the listener/filter/handback tuple
    * or null if the registration did not exist
     */
    public com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder _removeNotificationListener(javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
        {
        // import Component.Net.Management.ListenerHolder.LocalHolder;
        // import Component.Net.Management.NotificationHandler.LocalHandler;
        
        LocalHandler handler = get_LocalNotificationHandler();
        if (handler == null)
            {
            return null;
            }
        
        LocalHolder holder = LocalHolder.create(listener, filter, handback);
        return (LocalHolder) handler.unsubscribe(holder);
        }
    
    /**
     * Remove all notifications from the model.
     */
    public void _removeNotificationListeners()
        {
        // import Component.Net.Management.NotificationHandler.LocalHandler;
        
        LocalHandler handler = get_LocalNotificationHandler();
        
        if (handler != null)
            {
            handler.unsubscribeAll();
            }
        }
    
    /**
     * Transfer subscriptions from the specified model.
     */
    public void _transferSubscriptions(Model model)
        {
        // import Component.Net.Management.ListenerHolder.LocalHolder;
        // import java.util.Iterator;
        // import java.util.Set;
        
        _assert(get_LocalNotificationHandler() == null, "Subscriptions already exist");
        
        Set setSubs = model._ensureLocalNotificationHandler().getSubscriptions();
        if (setSubs != null)
            {
            for (Iterator iter = setSubs.iterator(); iter.hasNext();)
                {
                LocalHolder holder = (LocalHolder) iter.next();
        
                _addNotificationListener(holder.getListener(), holder.getFilter(), holder.getHandback());
                }
            }
        
        model.set_LocalNotificationHandler(null);
        }
    
    /**
     * Returns the specified string if it is not null and not empty; or the
    * "n/a" otherwise.
     */
    public static String canonicalString(String s)
        {
        return s == null || s.length() == 0 ? get_EMPTY() : s;
        }
    
    // Accessor for the property "_EMPTY"
    /**
     * Getter for property _EMPTY.<p>
    * Replacement for a null or empty string. The default value is "n/a".
     */
    public static String get_EMPTY()
        {
        return __s__EMPTY;
        }
    
    // Accessor for the property "_HealthModelName"
    /**
     * Getter for property _HealthModelName.<p>
     */
    public String get_HealthModelName()
        {
        return __m__HealthModelName;
        }
    
    // Accessor for the property "_LocalNotificationHandler"
    /**
     * Getter for property _LocalNotificationHandler.<p>
    * The LocalHandler component managing local subscriptions.
     */
    public com.tangosol.coherence.component.net.management.notificationHandler.LocalHandler get_LocalNotificationHandler()
        {
        return __m__LocalNotificationHandler;
        }
    
    // Accessor for the property "_MBeanComponent"
    /**
     * Getter for property _MBeanComponent.<p>
    * The name of the corresponding MBean component. If not overridden at the
    * specific Model subcomponent, the naming convention is:
    * 
    * sMBeanName = "Component.Manageable.ModelAdapter." + 
    * (get_Name() - "Model") + "MBean";
     */
    public String get_MBeanComponent()
        {
        // import Component.Net.Management.Model.RemoteModel;
        
        if (this instanceof RemoteModel)
            {
            return ((RemoteModel) this).getSnapshot().get_MBeanComponent();
            }
        else
            {
            String sModelName = get_Name();
            if (sModelName.endsWith("Model"))
                {
                sModelName = sModelName.substring(0, sModelName.length() - "Model".length());
                }
            else
                {
                // must be overridden
                throw new IllegalStateException("Unconventional model name");
                }
            return "Component.Manageable.ModelAdapter." + sModelName + "MBean";
            }
        }
    
    // Accessor for the property "_ModelName"
    /**
     * Getter for property _ModelName.<p>
    * The canonical name of the Model as registered with the owner node's
    * Connector (LocalRegistry) and the primary Gateway's (LocalModels).
     */
    public String get_ModelName()
        {
        return __m__ModelName;
        }
    
    /**
     * Invoke the method with the specified name and parameters on the MBean
    * represented by this model.
     */
    public Object invoke(int nOp, String sName, Object[] aoParam)
            throws java.lang.IllegalAccessException,
                   java.lang.NoSuchMethodException,
                   java.lang.reflect.InvocationTargetException,
                   javax.management.MBeanException
        {
        return invoke(nOp, sName, aoParam, null);
        }
    
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
        return null;
        }
    
    // Accessor for the property "_SubscribedTo"
    /**
     * Determine if the current model has subscriptions
     */
    public boolean is_SubscribedTo()
        {
        // import Component.Net.Management.NotificationHandler.LocalHandler;
        
        LocalHandler handler = get_LocalNotificationHandler();
        if (handler != null)
            {
            return handler.isSubscribedTo();
            }
        return false;
        }
    
    // Accessor for the property "_EMPTY"
    /**
     * Setter for property _EMPTY.<p>
    * Replacement for a null or empty string. The default value is "n/a".
     */
    public static void set_EMPTY(String sTag)
        {
        __s__EMPTY = sTag;
        }
    
    // Accessor for the property "_HealthModelName"
    /**
     * Setter for property _HealthModelName.<p>
     */
    public void set_HealthModelName(String sName)
        {
        __m__HealthModelName = sName;
        }
    
    // Accessor for the property "_LocalNotificationHandler"
    /**
     * Setter for property _LocalNotificationHandler.<p>
    * The LocalHandler component managing local subscriptions.
     */
    protected void set_LocalNotificationHandler(com.tangosol.coherence.component.net.management.notificationHandler.LocalHandler handler)
        {
        __m__LocalNotificationHandler = handler;
        }
    
    // Accessor for the property "_ModelName"
    /**
     * Setter for property _ModelName.<p>
    * The canonical name of the Model as registered with the owner node's
    * Connector (LocalRegistry) and the primary Gateway's (LocalModels).
     */
    public void set_ModelName(String sModelName)
        {
        __m__ModelName = sModelName;
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + ": " + get_ModelName();
        }
    }
