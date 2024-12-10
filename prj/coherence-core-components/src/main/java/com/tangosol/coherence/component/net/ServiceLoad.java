
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.ServiceLoad

package com.tangosol.coherence.component.net;

import com.tangosol.util.LiteMap;
import java.util.Map;

/**
 * Implementation of the ServiceLoad interface.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class ServiceLoad
        extends    com.tangosol.coherence.component.Net
        implements com.tangosol.net.ServiceLoad
    {
    // ---- Fields declarations ----
    
    /**
     * Property ConnectionCount
     *
     * The connection count.
     */
    private int __m_ConnectionCount;
    
    /**
     * Property ConnectionFactor
     *
     * A measure of how utilized the corresponding Service is in terms of
     * connection count. A value of 0 indicates that the Service is 0%
     * utilized, whereas a value of Integer.MAX_VALUE indicates that the
     * Service is 100% utilized. A value of -1 indicates that the factor hasn't
     * been calculated.
     */
    private int __m_ConnectionFactor;
    
    /**
     * Property ConnectionLimit
     *
     * The maximum number of simultaneous connections allowed. Valid values are
     * positive integers and zero. A value of zero implies no limit.
     */
    private int __m_ConnectionLimit;
    
    /**
     * Property ConnectionPendingCount
     *
     * The number of connections that are pending.
     */
    private int __m_ConnectionPendingCount;
    
    /**
     * Property DaemonActiveCount
     *
     * The number of daemon threads that are currently processing messages.
     */
    private int __m_DaemonActiveCount;
    
    /**
     * Property DaemonCount
     *
     * The number of daemon threads that are used to process messages.
     */
    private int __m_DaemonCount;
    
    /**
     * Property DaemonFactor
     *
     * A measure of how utilized the corresponding Service is in terms of
     * daemon count. A value of 0 indicates that the Service is 0% utilized,
     * whereas a value of Integer.MAX_VALUE indicates that the Service is 100%
     * utilized. A value of -1 indicates that the factor hasn't been calculated.
     */
    private int __m_DaemonFactor;
    
    /**
     * Property MessageBacklogFactor
     *
     * A measure of how utilized the corresponding Service is in terms of
     * connection backlog. A value of 0 indicates that the Service is 0%
     * utilized, whereas a value of Integer.MAX_VALUE indicates that the
     * Service is 100% utilized. A value of -1 indicates that the factor hasn't
     * been calculated.
     */
    private int __m_MessageBacklogFactor;
    
    /**
     * Property MessageBacklogIncoming
     *
     * The number of messages that are queued for processing.
     */
    private int __m_MessageBacklogIncoming;
    
    /**
     * Property MessageBacklogOutgoing
     *
     * The number of messages that are queued for delivery.
     */
    private int __m_MessageBacklogOutgoing;
    
    // Initializing constructor
    public ServiceLoad(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/ServiceLoad".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.ServiceLoad
    // Declared at the super level
    /**
     * Compares this object with the specified object for order.  Returns a
    * negative integer, zero, or a positive integer as this object is less
    * than, equal to, or greater than the specified object.
    * 
    * @param o  the Object to be compared.
    * @return  a negative integer, zero, or a positive integer as this object
    * is less than, equal to, or greater than the specified object.
    * 
    * @throws ClassCastException if the specified object's type prevents it
    * from being compared to this Object.
     */
    public int compareTo(Object o)
        {
        ServiceLoad that = (ServiceLoad) o;
        
        int n = getConnectionFactor() - that.getConnectionFactor();
        if (n == 0)
            {
            n = getMessageBacklogFactor() - that.getMessageBacklogFactor();
            if (n == 0)
                {
                n = getDaemonFactor() - that.getDaemonFactor();
                }
            }
        
        return n;
        }
    
    /**
     * Initialize this ServiceLoad from a Map representation.
    * 
    * @param map  the Map representation of a ServiceLoad
     */
    public void fromMap(java.util.Map map)
        {
        if (map != null)
            {
            Integer I = (Integer) map.get("ConnectionCount");
            if (I != null)
                {
                setConnectionCount(I.intValue());
                }
        
            I = (Integer) map.get("ConnectionPendingCount");
            if (I != null)
                {
                setConnectionPendingCount(I.intValue());
                }
        
            I = (Integer) map.get("ConnectionLimit");
            if (I != null)
                {
                setConnectionLimit(I.intValue());
                }
        
            I = (Integer) map.get("DaemonActiveCount");
            if (I != null)
                {
                setDaemonActiveCount(I.intValue());
                }
        
            I = (Integer) map.get("DaemonCount");
            if (I != null)
                {
                setDaemonCount(I.intValue());
                }
        
            I = (Integer) map.get("MessageBacklogIncoming");
            if (I != null)
                {
                setMessageBacklogIncoming(I.intValue());
                }
        
            I = (Integer) map.get("MessageBacklogOutgoing");
            if (I != null)
                {
                setMessageBacklogOutgoing(I.intValue());
                }
            }
        }
    
    // From interface: com.tangosol.net.ServiceLoad
    // Accessor for the property "ConnectionCount"
    /**
     * Getter for property ConnectionCount.<p>
    * The connection count.
     */
    public int getConnectionCount()
        {
        return __m_ConnectionCount;
        }
    
    // Accessor for the property "ConnectionFactor"
    /**
     * Getter for property ConnectionFactor.<p>
    * A measure of how utilized the corresponding Service is in terms of
    * connection count. A value of 0 indicates that the Service is 0% utilized,
    * whereas a value of Integer.MAX_VALUE indicates that the Service is 100%
    * utilized. A value of -1 indicates that the factor hasn't been calculated.
     */
    public int getConnectionFactor()
        {
        int n = __m_ConnectionFactor;
        if (n >= 0)
            {
            return n;
            }
        
        int cCurrent = getConnectionCount();
        int cPending = getConnectionPendingCount();
        int cLimit   = getConnectionLimit();
        int cTotal   = (int) Math.min(((long) cCurrent) + cPending, Integer.MAX_VALUE);
        
        if (cLimit > 0 && cTotal >= cLimit)
            {
            n = Integer.MAX_VALUE;
            }
        else
            {
            n = cTotal;
            }
        
        setConnectionFactor(n);
        return n;
        }
    
    // From interface: com.tangosol.net.ServiceLoad
    // Accessor for the property "ConnectionLimit"
    /**
     * Getter for property ConnectionLimit.<p>
    * The maximum number of simultaneous connections allowed. Valid values are
    * positive integers and zero. A value of zero implies no limit.
     */
    public int getConnectionLimit()
        {
        return __m_ConnectionLimit;
        }
    
    // From interface: com.tangosol.net.ServiceLoad
    // Accessor for the property "ConnectionPendingCount"
    /**
     * Getter for property ConnectionPendingCount.<p>
    * The number of connections that are pending.
     */
    public int getConnectionPendingCount()
        {
        return __m_ConnectionPendingCount;
        }
    
    // From interface: com.tangosol.net.ServiceLoad
    // Accessor for the property "DaemonActiveCount"
    /**
     * Getter for property DaemonActiveCount.<p>
    * The number of daemon threads that are currently processing messages.
     */
    public int getDaemonActiveCount()
        {
        return __m_DaemonActiveCount;
        }
    
    // From interface: com.tangosol.net.ServiceLoad
    // Accessor for the property "DaemonCount"
    /**
     * Getter for property DaemonCount.<p>
    * The number of daemon threads that are used to process messages.
     */
    public int getDaemonCount()
        {
        return __m_DaemonCount;
        }
    
    // Accessor for the property "DaemonFactor"
    /**
     * Getter for property DaemonFactor.<p>
    * A measure of how utilized the corresponding Service is in terms of daemon
    * count. A value of 0 indicates that the Service is 0% utilized, whereas a
    * value of Integer.MAX_VALUE indicates that the Service is 100% utilized. A
    * value of -1 indicates that the factor hasn't been calculated.
     */
    public int getDaemonFactor()
        {
        int n = __m_DaemonFactor;
        if (n >= 0)
            {
            return n;
            }
        
        int cCurrent = getDaemonActiveCount();
        int cLimit   = getDaemonCount();
        
        if (cCurrent >= cLimit)
            {
            n = Integer.MAX_VALUE;
            }
        else
            {
            n = cCurrent;
            }
        
        setDaemonFactor(n);
        return n;
        }
    
    protected String getDescription()
        {
        return   "ConnectionCount="        + getConnectionCount() +
               ", ConnectionPendingCount=" + getConnectionPendingCount() +
               ", ConnectionLimit="        + getConnectionLimit() +
               ", ConnectionFactor="       + getConnectionFactor() +
               ", DaemonActiveCount="      + getDaemonActiveCount() +       
               ", DaemonCount="            + getDaemonCount() +
               ", DaemonFactor="           + getDaemonFactor() +       
               ", MessageBacklogIncoming=" + getMessageBacklogIncoming() +
               ", MessageBacklogOutgoing=" + getMessageBacklogOutgoing() +
               ", MessageBacklogFactor="   + getMessageBacklogFactor();
        }
    
    // Accessor for the property "MessageBacklogFactor"
    /**
     * Getter for property MessageBacklogFactor.<p>
    * A measure of how utilized the corresponding Service is in terms of
    * connection backlog. A value of 0 indicates that the Service is 0%
    * utilized, whereas a value of Integer.MAX_VALUE indicates that the Service
    * is 100% utilized. A value of -1 indicates that the factor hasn't been
    * calculated.
     */
    public int getMessageBacklogFactor()
        {
        int n = __m_MessageBacklogFactor;
        if (n >= 0)
            {
            return n;
            }
        
        long li = getMessageBacklogIncoming();
        long lo = getMessageBacklogOutgoing();
        
        n = (int) Math.min(li + lo, Integer.MAX_VALUE);
        
        setMessageBacklogFactor(n);
        return n;
        }
    
    // From interface: com.tangosol.net.ServiceLoad
    // Accessor for the property "MessageBacklogIncoming"
    /**
     * Getter for property MessageBacklogIncoming.<p>
    * The number of messages that are queued for processing.
     */
    public int getMessageBacklogIncoming()
        {
        return __m_MessageBacklogIncoming;
        }
    
    // From interface: com.tangosol.net.ServiceLoad
    // Accessor for the property "MessageBacklogOutgoing"
    /**
     * Getter for property MessageBacklogOutgoing.<p>
    * The number of messages that are queued for delivery.
     */
    public int getMessageBacklogOutgoing()
        {
        return __m_MessageBacklogOutgoing;
        }
    
    // Accessor for the property "ConnectionCount"
    /**
     * Setter for property ConnectionCount.<p>
    * The connection count.
     */
    public void setConnectionCount(int c)
        {
        _assert(c >= 0);
        
        __m_ConnectionCount = (c);
        
        setConnectionFactor(-1);
        }
    
    // Accessor for the property "ConnectionFactor"
    /**
     * Setter for property ConnectionFactor.<p>
    * A measure of how utilized the corresponding Service is in terms of
    * connection count. A value of 0 indicates that the Service is 0% utilized,
    * whereas a value of Integer.MAX_VALUE indicates that the Service is 100%
    * utilized. A value of -1 indicates that the factor hasn't been calculated.
     */
    protected void setConnectionFactor(int n)
        {
        __m_ConnectionFactor = n;
        }
    
    // Accessor for the property "ConnectionLimit"
    /**
     * Setter for property ConnectionLimit.<p>
    * The maximum number of simultaneous connections allowed. Valid values are
    * positive integers and zero. A value of zero implies no limit.
     */
    public void setConnectionLimit(int c)
        {
        _assert(c >= 0);
        
        __m_ConnectionLimit = (c);
        
        setConnectionFactor(-1);
        }
    
    // Accessor for the property "ConnectionPendingCount"
    /**
     * Setter for property ConnectionPendingCount.<p>
    * The number of connections that are pending.
     */
    public void setConnectionPendingCount(int c)
        {
        _assert(c >= 0);
        
        __m_ConnectionPendingCount = (c);
        
        setConnectionFactor(-1);
        }
    
    // Accessor for the property "DaemonActiveCount"
    /**
     * Setter for property DaemonActiveCount.<p>
    * The number of daemon threads that are currently processing messages.
     */
    public void setDaemonActiveCount(int c)
        {
        _assert(c >= 0);
        
        __m_DaemonActiveCount = (c);
        
        setDaemonFactor(-1);
        }
    
    // Accessor for the property "DaemonCount"
    /**
     * Setter for property DaemonCount.<p>
    * The number of daemon threads that are used to process messages.
     */
    public void setDaemonCount(int c)
        {
        _assert(c >= 0);
        
        __m_DaemonCount = (c);
        
        setDaemonFactor(-1);
        }
    
    // Accessor for the property "DaemonFactor"
    /**
     * Setter for property DaemonFactor.<p>
    * A measure of how utilized the corresponding Service is in terms of daemon
    * count. A value of 0 indicates that the Service is 0% utilized, whereas a
    * value of Integer.MAX_VALUE indicates that the Service is 100% utilized. A
    * value of -1 indicates that the factor hasn't been calculated.
     */
    protected void setDaemonFactor(int n)
        {
        __m_DaemonFactor = n;
        }
    
    // Accessor for the property "MessageBacklogFactor"
    /**
     * Setter for property MessageBacklogFactor.<p>
    * A measure of how utilized the corresponding Service is in terms of
    * connection backlog. A value of 0 indicates that the Service is 0%
    * utilized, whereas a value of Integer.MAX_VALUE indicates that the Service
    * is 100% utilized. A value of -1 indicates that the factor hasn't been
    * calculated.
     */
    protected void setMessageBacklogFactor(int n)
        {
        __m_MessageBacklogFactor = n;
        }
    
    // Accessor for the property "MessageBacklogIncoming"
    /**
     * Setter for property MessageBacklogIncoming.<p>
    * The number of messages that are queued for processing.
     */
    public void setMessageBacklogIncoming(int c)
        {
        _assert(c >= 0);
        
        __m_MessageBacklogIncoming = (c);
        
        setMessageBacklogFactor(-1);
        }
    
    // Accessor for the property "MessageBacklogOutgoing"
    /**
     * Setter for property MessageBacklogOutgoing.<p>
    * The number of messages that are queued for delivery.
     */
    public void setMessageBacklogOutgoing(int c)
        {
        _assert(c >= 0);
        
        __m_MessageBacklogOutgoing = (c);
        
        setMessageBacklogFactor(-1);
        }
    
    /**
     * Create a Map representation of this ServiceLoad.
    * 
    * @return the Map representation of this ServiceLoad
     */
    public java.util.Map toMap()
        {
        // import com.tangosol.util.LiteMap;
        // import java.util.Map;
        
        Map map = new LiteMap();
        map.put("ConnectionCount",        Integer.valueOf(getConnectionCount()));
        map.put("ConnectionPendingCount", Integer.valueOf(getConnectionPendingCount()));
        map.put("ConnectionLimit",        Integer.valueOf(getConnectionLimit()));
        map.put("DaemonActiveCount",      Integer.valueOf(getDaemonActiveCount()));
        map.put("DaemonCount",            Integer.valueOf(getDaemonCount()));
        map.put("MessageBacklogIncoming", Integer.valueOf(getMessageBacklogIncoming()));
        map.put("MessageBacklogOutgoing", Integer.valueOf(getMessageBacklogOutgoing()));
        
        return map;
        }
    
    // Declared at the super level
    public String toString()
        {
        StringBuffer sb = new StringBuffer();
        
        sb.append("ServiceLoad(")
          .append("ConnectionCount=")
          .append(getConnectionCount())
          .append(", MsgBacklogIncoming=")
          .append(getMessageBacklogIncoming())
          .append("\n)");
        
        return sb.toString();
        }
    
    /**
     * Update the connection count by adding the specified value to the existing
    * connection count.
    * 
    * @param c  the count delta
     */
    public void updateConnectionCount(int c)
        {
        setConnectionCount(Math.max(0, getConnectionCount() + c));
        }
    
    /**
     * Update the connection pending count by adding the specified value to the
    * existing connection pending count.
    * 
    * @param c  the count delta
     */
    public void updateConnectionPendingCount(int c)
        {
        setConnectionPendingCount(getConnectionPendingCount() + c);
        }
    }
