
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.notificationHandler.LocalHandler

package com.tangosol.coherence.component.net.management.notificationHandler;

import com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder;
import com.tangosol.util.LiteSet;
import java.util.Iterator;
import java.util.Set;

/**
 * NotificationHandler is a trivial base fo components that serve as a shim
 * layer between models and notification listeners.
 * Handles subscriptions and sending notifications to listeners on the managing
 * node.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class LocalHandler
        extends    com.tangosol.coherence.component.net.management.NotificationHandler
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public LocalHandler()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public LocalHandler(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setSubscriptions(new com.tangosol.util.SafeHashSet());
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
        return new com.tangosol.coherence.component.net.management.notificationHandler.LocalHandler();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/notificationHandler/LocalHandler".replace('/', '.'));
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
    
    public void handleNotification(javax.management.Notification notification)
        {
        // import Component.Net.Management.ListenerHolder.LocalHolder;
        // import java.util.Iterator;
        // import java.util.Set;
        
        Set setSubs = getSubscriptions();
        if (setSubs != null)
            {
            for (Iterator iter = setSubs.iterator(); iter.hasNext();)
                {
                LocalHolder holder = (LocalHolder) iter.next();
        
                holder.handleNotification(notification);
                }
            }
        }
    
    /**
     * Remove all subscriptions for the specified listener.
    * 
    * @return a Set<LocalHolder> of removed holders associated with the
    * specified listener
     */
    public java.util.Set unsubscribe(javax.management.NotificationListener listener)
        {
        // import Component.Net.Management.ListenerHolder.LocalHolder;
        // import com.tangosol.util.LiteSet;
        // import java.util.Iterator;
        // import java.util.Set;
        
        Set setHolders = new LiteSet();
        for (Iterator iter = getSubscriptions().iterator(); iter.hasNext(); )
            {
            LocalHolder holder = (LocalHolder) iter.next();
        
            if (holder.getListener() == listener)
                {
                iter.remove();
                setHolders.add(holder);
                }
            }
        return setHolders;
        }
    }
