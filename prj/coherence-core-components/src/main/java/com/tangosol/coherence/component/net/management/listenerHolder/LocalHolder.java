
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder

package com.tangosol.coherence.component.net.management.listenerHolder;

import com.tangosol.util.Base;

/**
 * LocalHolder represents a listener/filter/handback tuple.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class LocalHolder
        extends    com.tangosol.coherence.component.net.management.ListenerHolder
    {
    // ---- Fields declarations ----
    
    /**
     * Property Handback
     *
     * THe handback object associated with the Listener.
     */
    private transient Object __m_Handback;
    
    /**
     * Property Listener
     *
     * The NotificationListener reference.
     */
    private transient javax.management.NotificationListener __m_Listener;
    
    /**
     * Property NextId
     *
     * Unique id generator for RemoteModels.
     */
    private static transient long __s_NextId;
    
    /**
     * Property RemoteHolder
     *
     * RemoteHolder objects representing this LocalHolder.
     */
    private transient RemoteHolder __m_RemoteHolder;
    
    // Default constructor
    public LocalHolder()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public LocalHolder(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/listenerHolder/LocalHolder".replace('/', '.'));
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
     * Create the LocalHoler for the specified (listener, filter, handback)
    * tuple.
     */
    public static LocalHolder create(javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
        {
        LocalHolder holder = new LocalHolder();
        holder.setListener(listener);
        holder.setFilter(filter);
        holder.setHandback(handback);
        return holder;
        }
    
    /**
     * Return a RemoteHolder object representing this LocalHolder.
     */
    public RemoteHolder ensureRemoteHolder()
        {
        RemoteHolder holder = getRemoteHolder();
        if (holder == null)
            {
            synchronized (this)
                {
                // we can use the double-lock here
                holder = getRemoteHolder();
                if (holder == null)
                    {
                    holder = new RemoteHolder();
                    holder.setFilter(getFilter());
                    holder.setHolderId(getNextId());
                    setRemoteHolder(holder);
                    }
                }
            }
        return holder;
        }
    
    // Declared at the super level
    public boolean equals(Object obj)
        {
        // import com.tangosol.util.Base;
        
        if (super.equals(obj))
            {
            if (obj instanceof LocalHolder)
                {
                LocalHolder that = (LocalHolder) obj;
                return Base.equals(this.getListener(), that.getListener())
                    && Base.equals(this.getHandback(), that.getHandback());
                }
            }
        return false;
        }
    
    // Accessor for the property "Handback"
    /**
     * Getter for property Handback.<p>
    * THe handback object associated with the Listener.
     */
    public Object getHandback()
        {
        return __m_Handback;
        }
    
    // Accessor for the property "Listener"
    /**
     * Getter for property Listener.<p>
    * The NotificationListener reference.
     */
    public javax.management.NotificationListener getListener()
        {
        return __m_Listener;
        }
    
    // Accessor for the property "NextId"
    /**
     * Getter for property NextId.<p>
    * Unique id generator for RemoteModels.
     */
    public static long getNextId()
        {
        long lId = __s_NextId + 1;
        setNextId(lId);
        return lId;
        }
    
    // Accessor for the property "RemoteHolder"
    /**
     * Getter for property RemoteHolder.<p>
    * RemoteHolder objects representing this LocalHolder.
     */
    public RemoteHolder getRemoteHolder()
        {
        return __m_RemoteHolder;
        }
    
    /**
     * Handle the specified notification.
     */
    public void handleNotification(javax.management.Notification notification)
        {
        try
            {
            if (evaluate(notification))
                {
                getListener().handleNotification(notification, getHandback());
                }
            }
        catch (Exception e)
            {
            _trace("Exception while handling notification: " + notification +
                   "\n" + getStackTrace(e), 1);
            }
        }
    
    // Declared at the super level
    public int hashCode()
        {
        Object listener = getListener();
        Object handback = getHandback();
        
        return super.hashCode()
             + (listener == null ? 0 : listener.hashCode())
             + (handback == null ? 0 : handback.hashCode());
        }
    
    // Accessor for the property "Handback"
    /**
     * Setter for property Handback.<p>
    * THe handback object associated with the Listener.
     */
    protected void setHandback(Object oHandback)
        {
        __m_Handback = oHandback;
        }
    
    // Accessor for the property "Listener"
    /**
     * Setter for property Listener.<p>
    * The NotificationListener reference.
     */
    protected void setListener(javax.management.NotificationListener listener)
        {
        __m_Listener = listener;
        }
    
    // Accessor for the property "NextId"
    /**
     * Setter for property NextId.<p>
    * Unique id generator for RemoteModels.
     */
    private static void setNextId(long lId)
        {
        __s_NextId = lId;
        }
    
    // Accessor for the property "RemoteHolder"
    /**
     * Setter for property RemoteHolder.<p>
    * RemoteHolder objects representing this LocalHolder.
     */
    protected void setRemoteHolder(RemoteHolder holder)
        {
        __m_RemoteHolder = holder;
        }
    
    // Declared at the super level
    public String toString()
        {
        StringBuilder sb = new StringBuilder(get_Name());
        
        sb.append("{Listener=")
          .append(getListener());
        
        Object filter = getFilter();
        if (filter != null)
            {
            sb.append("; filter=")
              .append(filter);
            }
        
        RemoteHolder hRemote = getRemoteHolder();
        if (hRemote != null)
            {
            sb.append("; remoteId=")
              .append(hRemote.getHolderId());
            }
        sb.append('}');
        
        return sb.toString();
        }
    }
