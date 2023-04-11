
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.ListenerHolder

package com.tangosol.coherence.component.net.management;

import com.tangosol.util.Base;
import javax.management.NotificationFilter;

/**
 * ListenerHolder is a trivial abstract base for holders.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class ListenerHolder
        extends    com.tangosol.coherence.component.net.Management
    {
    // ---- Fields declarations ----
    
    /**
     * Property Filter
     *
     * The NotificationFilter used by the underlying listener.
     */
    private javax.management.NotificationFilter __m_Filter;
    
    // Initializing constructor
    public ListenerHolder(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/ListenerHolder".replace('/', '.'));
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
    
    // Declared at the super level
    public boolean equals(Object obj)
        {
        // import com.tangosol.util.Base;
        
        if (obj instanceof ListenerHolder)
            {
            ListenerHolder that = (ListenerHolder) obj;
            return Base.equals(this.getFilter(), that.getFilter());
            }
        return false;
        }
    
    /**
     * Evaluate the specified Notification against the Filter held by this
    * holder.
     */
    public boolean evaluate(javax.management.Notification notification)
        {
        // import javax.management.NotificationFilter;
        
        NotificationFilter filter = getFilter();
        try
            {
            return filter == null || filter.isNotificationEnabled(notification);
            }
        catch (Exception e)
            {
            _trace("Exception while evaluating notification: " + notification +
                   "\n" + getStackTrace(e), 1);
            return false;
            }
        }
    
    // Accessor for the property "Filter"
    /**
     * Getter for property Filter.<p>
    * The NotificationFilter used by the underlying listener.
     */
    public javax.management.NotificationFilter getFilter()
        {
        return __m_Filter;
        }
    
    // Declared at the super level
    public int hashCode()
        {
        // let's avoid relying on the correct hashCode() implementation by the NotificationFilter
        return 17;
        }
    
    // Accessor for the property "Filter"
    /**
     * Setter for property Filter.<p>
    * The NotificationFilter used by the underlying listener.
     */
    protected void setFilter(javax.management.NotificationFilter filter)
        {
        __m_Filter = filter;
        }
    }
