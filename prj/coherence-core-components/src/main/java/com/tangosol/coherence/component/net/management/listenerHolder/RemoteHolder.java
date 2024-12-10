
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.listenerHolder.RemoteHolder

package com.tangosol.coherence.component.net.management.listenerHolder;

import com.tangosol.util.ExternalizableHelper;
import java.io.EOFException;
import javax.management.NotificationFilter;

/**
 * RemoteHolder is a proxy for a LocalHolder on a different node.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class RemoteHolder
        extends    com.tangosol.coherence.component.net.management.ListenerHolder
        implements com.tangosol.util.ExternalizableLite
    {
    // ---- Fields declarations ----
    
    /**
     * Property HolderId
     *
     * A unique Id for the corresponding LocalHolder.
     */
    private long __m_HolderId;
    
    /**
     * Property MemberId
     *
     * Member id for the listener represented by this holder.
     */
    private int __m_MemberId;
    
    // Default constructor
    public RemoteHolder()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public RemoteHolder(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.management.listenerHolder.RemoteHolder();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/listenerHolder/RemoteHolder".replace('/', '.'));
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
        if (super.equals(obj))
            {
            if (obj instanceof RemoteHolder)
                {
                RemoteHolder that = (RemoteHolder) obj;
                return this.getHolderId() == that.getHolderId()
                    && this.getMemberId() == that.getMemberId();
                }
            }
        return false;
        }
    
    // Accessor for the property "HolderId"
    /**
     * Getter for property HolderId.<p>
    * A unique Id for the corresponding LocalHolder.
     */
    public long getHolderId()
        {
        return __m_HolderId;
        }
    
    // Accessor for the property "MemberId"
    /**
     * Getter for property MemberId.<p>
    * Member id for the listener represented by this holder.
     */
    public int getMemberId()
        {
        return __m_MemberId;
        }
    
    // Declared at the super level
    public int hashCode()
        {
        return super.hashCode() + (int) getHolderId() + getMemberId();
        }
    
    // From interface: com.tangosol.util.ExternalizableLite
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        // import java.io.EOFException;
        // import javax.management.NotificationFilter;
        
        setMemberId(ExternalizableHelper.readInt(in));
        setHolderId(ExternalizableHelper.readLong(in));
        try
            {
            setFilter((NotificationFilter) ExternalizableHelper.readObject(in));
            }
        catch (Throwable e)
            {
            if (!(e instanceof EOFException))
                {
                _trace("The NotificationFilter is not deserializable; " + e, 3);
                }
            }
        }
    
    // Declared at the super level
    /**
     * Setter for property Filter.<p>
    * The NotificationFilter used by the underlying listener.
     */
    public void setFilter(javax.management.NotificationFilter filter)
        {
        super.setFilter(filter);
        }
    
    // Accessor for the property "HolderId"
    /**
     * Setter for property HolderId.<p>
    * A unique Id for the corresponding LocalHolder.
     */
    public void setHolderId(long lHolderId)
        {
        __m_HolderId = lHolderId;
        }
    
    // Accessor for the property "MemberId"
    /**
     * Setter for property MemberId.<p>
    * Member id for the listener represented by this holder.
     */
    public void setMemberId(int nMemberId)
        {
        __m_MemberId = nMemberId;
        }
    
    // Declared at the super level
    public String toString()
        {
        StringBuilder sb = new StringBuilder(get_Name());
        
        sb.append("{HolderId=")
          .append(getHolderId())
          .append("; Member=")
          .append(getMemberId());
        
        Object filter = getFilter();
        if (filter != null)
            {
            sb.append("; filter=")
              .append(filter);
            }
        
        sb.append('}');
        
        return sb.toString();
        }
    
    // From interface: com.tangosol.util.ExternalizableLite
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        ExternalizableHelper.writeInt(out, getMemberId());
        ExternalizableHelper.writeLong(out, getHolderId());
        try
            {
            ExternalizableHelper.writeObject(out, getFilter());
            }
        catch (Throwable e)
            {
            _trace("The NotificationFilter \"" + getFilter().getClass().getName()
                 + "\" is not serializable; " + e, 3);
            }
        }
    }
