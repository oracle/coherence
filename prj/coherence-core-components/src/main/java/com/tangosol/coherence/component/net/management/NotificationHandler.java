
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.NotificationHandler

package com.tangosol.coherence.component.net.management;

import com.tangosol.coherence.component.net.management.ListenerHolder;
import java.util.Iterator;
import java.util.Set;

/**
 * NotificationHandler is a trivial base fo components that serve as a shim
 * layer between models and notification listeners.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class NotificationHandler
        extends    com.tangosol.coherence.component.net.Management
    {
    // ---- Fields declarations ----
    
    /**
     * Property Name
     *
     * Internal name of the MBean holding on this handler.
     */
    private String __m_Name;
    
    /**
     * Property Subscriptions
     *
     * Set<NotificationHolder> maintaining subscriptions. The types of holders
     * are:
     * - LocalHolder for LocalHandler
     * - RemotelHolder for RemotelHandler
     */
    private java.util.Set __m_Subscriptions;
    
    // Initializing constructor
    public NotificationHandler(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/NotificationHandler".replace('/', '.'));
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
    
    // Accessor for the property "Name"
    /**
     * Getter for property Name.<p>
    * Internal name of the MBean holding on this handler.
     */
    public String getName()
        {
        return __m_Name;
        }
    
    // Accessor for the property "Subscriptions"
    /**
     * Getter for property Subscriptions.<p>
    * Set<NotificationHolder> maintaining subscriptions. The types of holders
    * are:
    * - LocalHolder for LocalHandler
    * - RemotelHolder for RemotelHandler
     */
    public java.util.Set getSubscriptions()
        {
        return __m_Subscriptions;
        }
    
    // Accessor for the property "SubscribedTo"
    /**
     * Specifies whether or not subscriptions exist for this handler.
     */
    public boolean isSubscribedTo()
        {
        return getSubscriptions().size() > 0;
        }
    
    // Accessor for the property "Name"
    /**
     * Setter for property Name.<p>
    * Internal name of the MBean holding on this handler.
     */
    public void setName(String sName)
        {
        __m_Name = sName;
        }
    
    // Accessor for the property "Subscriptions"
    /**
     * Setter for property Subscriptions.<p>
    * Set<NotificationHolder> maintaining subscriptions. The types of holders
    * are:
    * - LocalHolder for LocalHandler
    * - RemotelHolder for RemotelHandler
     */
    protected void setSubscriptions(java.util.Set setSubscriptions)
        {
        __m_Subscriptions = setSubscriptions;
        }
    
    /**
     * Subscribe to notifications represented by the specified holder.
    * 
    * @return true if the subscription has been added; false if it alreadd
    * existed
     */
    public boolean subscribe(ListenerHolder holder)
        {
        _assert(holder != null);
        
        return getSubscriptions().add(holder);
        }
    
    // Declared at the super level
    /**
     * Overload of the toString to display the underlying subsc riptions.  This
    * is helpful when debugging through the console.
     */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();
        
        sb.append(get_Name())
          .append(": Name=")
          .append(getName())
          .append(", Subscriptions=")
          .append(getSubscriptions());
          
        return sb.toString();
        }
    
    /**
     * Unregister notification subscribtion represented by the specified holder.
    * 
    * @return true if the subscription has been removed; false if it did not
    * exist
     */
    public ListenerHolder unsubscribe(ListenerHolder holder)
        {
        // import Component.Net.Management.ListenerHolder;
        // import java.util.Set;
        // import java.util.Iterator;
        
        _assert(holder != null);
        
        Set setSubscriptions = getSubscriptions();
        
        for (Iterator iter = setSubscriptions.iterator(); iter.hasNext();)
            {
            ListenerHolder holderTest = (ListenerHolder) iter.next();
            if (holder.equals(holderTest))
                {
                iter.remove();
                return holderTest;
                }
            }
         
        return null;
        }
    
    /**
     * Remove all subscriptions.
     */
    public void unsubscribeAll()
        {
        getSubscriptions().clear();
        }
    }
