
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.notificationHandler.RemoteHandler

package com.tangosol.coherence.component.net.management.notificationHandler;

import com.tangosol.coherence.component.net.management.Connector;
import com.tangosol.coherence.component.net.management.listenerHolder.RemoteHolder;
import com.tangosol.net.Member;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Implementation for remote notification handling
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class RemoteHandler
        extends    com.tangosol.coherence.component.net.management.NotificationHandler
    {
    // ---- Fields declarations ----
    
    /**
     * Property Connector
     *
     * Reference to the Connector component.
     */
    private com.tangosol.coherence.component.net.management.Connector __m_Connector;
    
    // Default constructor
    public RemoteHandler()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public RemoteHandler(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.management.notificationHandler.RemoteHandler();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/notificationHandler/RemoteHandler".replace('/', '.'));
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
     * Return a running Connector or null.
     */
    protected com.tangosol.coherence.component.net.management.Connector ensureRunningConnector()
        {
        // import Component.Net.Management.Connector;
        
        Connector conn = getConnector();
        
        if (conn.isStarted())
            {
            return conn;
            }
        
        conn.startService(null);
        return conn.isStarted() ? conn : null;
        }
    
    // Accessor for the property "Connector"
    /**
     * Getter for property Connector.<p>
    * Reference to the Connector component.
     */
    public com.tangosol.coherence.component.net.management.Connector getConnector()
        {
        return __m_Connector;
        }
    
    public void handleNotification(javax.management.Notification notification)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.ListenerHolder.RemoteHolder;
        // import com.tangosol.net.Member;
        // import java.util.Iterator;
        // import java.util.HashSet;
        // import java.util.Set;
        
        Connector connector  = ensureRunningConnector();
        Set       setHolders = getSubscriptions();
        
        if (connector == null)
            {
            _trace("Unable to send notification due to an abnormal termination of the management service: " +
                notification, 2);
            }
        else if (setHolders != null)
            {
            Set setMembers = new HashSet();
            for (Iterator iter = setHolders.iterator(); iter.hasNext();)
                {
                RemoteHolder holder = (RemoteHolder) iter.next();
        
                Member member = connector.getMember(holder.getMemberId());
        
                if (member != null && !setMembers.contains(member) && holder.evaluate(notification))
                    {
                    setMembers.add(member);
                    }
                }
        
            if (setMembers.size() > 0)
                {
                connector.sendNotification(setMembers, getName(), notification);
                }
            }
        }
    
    // Accessor for the property "Connector"
    /**
     * Setter for property Connector.<p>
    * Reference to the Connector component.
     */
    public void setConnector(com.tangosol.coherence.component.net.management.Connector connector)
        {
        __m_Connector = connector;
        }
    
    // Declared at the super level
    /**
     * Setter for property Subscriptions.<p>
    * Set<NotificationHolder> maintaining subscriptions. The types of holders
    * are:
    * - LocalHolder for LocalHandler
    * - RemotelHolder for RemotelHandler
     */
    public void setSubscriptions(java.util.Set setSubscriptions)
        {
        super.setSubscriptions(setSubscriptions);
        }
    
    /**
     * Remove the subscription represented by the specified holder id for the
    * given member.
     */
    public void unsubscribe(int nMember, long lHolderId)
        {
        // import Component.Net.Management.ListenerHolder.RemoteHolder;
        // import java.util.Iterator;
        
        for (Iterator iter = getSubscriptions().iterator(); iter.hasNext(); )
            {
            RemoteHolder holder = (RemoteHolder) iter.next(); 
        
            if (holder.getMemberId() == nMember && holder.getHolderId() == lHolderId)
                {
                iter.remove();
                }
            }
        }
    
    /**
     * Remove all subscriptions for the specified member.
     */
    public void unsubscribeMember(int nMember)
        {
        // import Component.Net.Management.ListenerHolder.RemoteHolder;
        // import java.util.Iterator;
        
        for (Iterator iter = getSubscriptions().iterator(); iter.hasNext(); )
            {
            RemoteHolder holder = (RemoteHolder) iter.next(); 
        
            if (holder.getMemberId() == nMember)
                {
                iter.remove();
                }
            }
        }
    }
