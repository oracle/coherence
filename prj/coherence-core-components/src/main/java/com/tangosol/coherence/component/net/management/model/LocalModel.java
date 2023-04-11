
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.LocalModel

package com.tangosol.coherence.component.net.management.model;

import com.tangosol.coherence.component.net.management.notificationHandler.RemoteHandler;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.Notification;

/**
 * Model components implement the JMX-managed functionality of the
 * corresponding MBeans without being dependent on any JMX classes and could be
 * used both in-process and out-of-process (relative to an MBeanServer).
 * 
 * The LocalModel components operate in two distinct modes: live and snapshot.
 * In the live mode all model methods call corresponding methods on managed
 * objects. The snapshot mode uses the _SnapshotMap to keep the attribute
 * values.
 * 
 * Every time a remote invocation is used by the RemoteModel to do a
 * setAttribute or invoke call, the snapshot model is refreshed.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class LocalModel
        extends    com.tangosol.coherence.component.net.management.Model
        implements com.tangosol.io.ExternalizableLite
    {
    // ---- Fields declarations ----
    
    /**
     * Property _InvocationResult
     *
     * This field of the LocalMode component is used to bring back a result of
     * remote invocation (setAttribute or invoke) along with the latest model
     * snapshot.
     * 
     * @see RemoteModel#run()
     */
    private Object __m__InvocationResult;
    
    /**
     * Property _ReadOnly
     *
     * Specifies whether or not only the viewing of attributes is allowed.
     */
    private transient boolean __m__ReadOnly;
    
    /**
     * Property _RefreshTimeMillis
     *
     * The [safe] timestamp when the model was last refreshed.
     */
    private transient long __m__RefreshTimeMillis;
    
    /**
     * Property _RemoteNotificationHandler
     *
     * The RemoteHandler component managing remote subscriptions.
     */
    private transient com.tangosol.coherence.component.net.management.notificationHandler.RemoteHandler __m__RemoteNotificationHandler;
    
    /**
     * Property _Sequence
     *
     * the Notification sequence counter
     */
    private java.util.concurrent.atomic.AtomicLong __m__Sequence;
    
    /**
     * Property _Snapshot
     *
     * Specifies whether or not this model is a snapshot of the remote model.
     */
    private transient boolean __m__Snapshot;
    
    /**
     * Property _SnapshotMap
     *
     * The map of the snapshot attributes. The keys are the attribute accessor
     * names, the values are cached attribute values. Meaningful only for a
     * snapshot model.
     */
    private transient java.util.Map __m__SnapshotMap;
    
    // Initializing constructor
    public LocalModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/LocalModel".replace('/', '.'));
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
     * Subscribe to the local model from a remote member using the Notification
    * Listener Reference
     */
    public void _addRemoteNotificationListener(com.tangosol.coherence.component.net.management.listenerHolder.RemoteHolder holder, com.tangosol.coherence.component.net.management.Connector connector)
        {
        _ensureRemoteNotificationHandler(connector).subscribe(holder);
        }
    
    /**
     * Check whether or not this model *snapshot* is still permitted to be used.
    * 
    * @param cRefreshMillis  the number of milliseconds that a snapshot is
    * permitted to be used before refreshing; zero means no expiry
    * 
    * @return true iff this snapshot is out-of-date
     */
    public boolean _checkExpired(long cRefreshMillis)
        {
        // import com.tangosol.util.Base;
        
        return is_Snapshot() &&
            get_RefreshTimeMillis() + cRefreshMillis < Base.getSafeTimeMillis();
        }
    
    /**
     * return the remote notification handler,  if one does not exist create
    * one.  This should only be used on subscribe.
     */
    public com.tangosol.coherence.component.net.management.notificationHandler.RemoteHandler _ensureRemoteNotificationHandler(com.tangosol.coherence.component.net.management.Connector connector)
        {
        // import Component.Net.Management.NotificationHandler.RemoteHandler;
        
        RemoteHandler handler = get_RemoteNotificationHandler();
        if (handler == null)
            {
            handler = new RemoteHandler();
            handler.setName(get_ModelName());
            handler.setConnector(connector);
            set_RemoteNotificationHandler(handler);
            }
        
        return handler;
        }
    
    /**
     * Process the Notification for this model.
     */
    public void _handleNotification(String sType, String sMessage)
            throws java.lang.IllegalArgumentException
        {
        // import javax.management.Notification;
        
        _handleNotification(
            new Notification(sType, get_ModelName(), _nextSequence(),
                System.currentTimeMillis(), sMessage));
        }
    
    // Declared at the super level
    /**
     * Process the Notification for this model.
     */
    public void _handleNotification(javax.management.Notification notification)
        {
        // import Component.Net.Management.NotificationHandler.RemoteHandler;
        
        if (notification.getSequenceNumber() < 0L)
            {
            notification.setSequenceNumber(_nextSequence());
            }
        super._handleNotification(notification);
        
        RemoteHandler handler = get_RemoteNotificationHandler();
        if (handler != null)
            {
            handler.handleNotification(notification);
            }
        }
    
    /**
     * get the next sequence number for the notifications
     */
    protected long _nextSequence()
        {
        return get_Sequence().incrementAndGet();
        }
    
    // Declared at the super level
    /**
     * Remove all notifications from the model.
     */
    public void _removeNotificationListeners()
        {
        // import Component.Net.Management.NotificationHandler.RemoteHandler;
        
        RemoteHandler handler = get_RemoteNotificationHandler();
        if (handler != null)
            {
            handler.unsubscribeAll();
            }
        
        super._removeNotificationListeners();
        }
    
    /**
     * Unsubscribe remote notifications represented by the specified holder id
    * for the given member.
     */
    public void _removeRemoteNotificationListener(int nMember, long lHolderId)
        {
        // import Component.Net.Management.NotificationHandler.RemoteHandler;
        
        RemoteHandler handler = get_RemoteNotificationHandler();
        if (handler != null)
            {
            handler.unsubscribe(nMember, lHolderId);
            }
        }
    
    /**
     * Checks whether or not the "write" operation is allowed.
    * 
    * @throws SecurityException if the model is "read-only"
     */
    protected void checkRange(String sOperation, double dValue, double dMin, double dMax)
        {
        checkReadOnly(sOperation);
        if (dValue < dMin || dValue > dMax)
            {
            throw new IllegalArgumentException("Operation \"" + sOperation +
                "\" -- value out of range [" + dMin + ", " + dMax + "]: " + dValue);
            }
        }
    
    /**
     * Checks whether or not the "write" operation is allowed.
    * 
    * @throws SecurityException if the model is "read-only"
     */
    protected void checkRange(String sOperation, int nValue, int nMin, int nMax)
        {
        checkReadOnly(sOperation);
        if (nValue < nMin || nValue > nMax)
            {
            throw new IllegalArgumentException("Operation \"" + sOperation +
                "\" -- value out of range [" + nMin + ", " + nMax + "]: " + nValue);
            }
        }
    
    /**
     * Checks whether or not the "write" operation is allowed.
    * 
    * @throws SecurityException if the model is "read-only"
     */
    protected void checkReadOnly(String sOperation)
        {
        if (is_ReadOnly())
            {
            throw new SecurityException("Operation is not allowed: " + sOperation);
            }
        }
    
    // Accessor for the property "_InvocationResult"
    /**
     * Getter for property _InvocationResult.<p>
    * This field of the LocalMode component is used to bring back a result of
    * remote invocation (setAttribute or invoke) along with the latest model
    * snapshot.
    * 
    * @see RemoteModel#run()
     */
    public Object get_InvocationResult()
        {
        return __m__InvocationResult;
        }
    
    // Accessor for the property "_RefreshTimeMillis"
    /**
     * Returns the time the snapshot was refreshed or the current time on a
    * local model.
     */
    public long get_RefreshTimeMillis()
        {
        // import com.tangosol.util.Base;
        
        return is_Snapshot() ? __m__RefreshTimeMillis : Base.getSafeTimeMillis();
        }
    
    // Accessor for the property "_RemoteNotificationHandler"
    /**
     * Getter for property _RemoteNotificationHandler.<p>
    * The RemoteHandler component managing remote subscriptions.
     */
    public com.tangosol.coherence.component.net.management.notificationHandler.RemoteHandler get_RemoteNotificationHandler()
        {
        return __m__RemoteNotificationHandler;
        }
    
    // Accessor for the property "_Sequence"
    /**
     * Getter for property _Sequence.<p>
    * the Notification sequence counter
     */
    protected java.util.concurrent.atomic.AtomicLong get_Sequence()
        {
        // import java.util.concurrent.atomic.AtomicLong;
        
        AtomicLong counter = __m__Sequence;
        if (counter == null)
            {
            counter = new AtomicLong();
            set_Sequence(counter);
            }
        return counter;
        }
    
    // Accessor for the property "_SnapshotMap"
    /**
     * Getter for property _SnapshotMap.<p>
    * The map of the snapshot attributes. The keys are the attribute accessor
    * names, the values are cached attribute values. Meaningful only for a
    * snapshot model.
     */
    public java.util.Map get_SnapshotMap()
        {
        return __m__SnapshotMap;
        }
    
    // Accessor for the property "Description"
    /**
     * Getter for property Description.<p>
    * Human readable description.
    * 
    * @see Manageable.ModelAdapter#toString()
     */
    public String getDescription()
        {
        return get_Name();
        }
    
    // Accessor for the property "RefreshTime"
    /**
     * Returns the refresh time in a date format.
     */
    public java.util.Date getRefreshTime()
        {
        // import java.util.Date;
        
        return new Date(get_RefreshTimeMillis());
        }
    
    // Declared at the super level
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
        // the caller must have already reflect the operation in the [method] name
        // so we can ignore the operation parameter
        return invoke(sName, aoParam);
        }
    
    /**
     * Invoke the method with the specified name on this LocalModel with the
    * specified parameters.
     */
    public Object invoke(String sMethod, Object[] aoParam)
            throws java.lang.IllegalAccessException,
                   java.lang.NoSuchMethodException,
                   java.lang.reflect.InvocationTargetException
        {
        // import com.tangosol.util.ClassHelper;
        
        return ClassHelper.invoke(this, sMethod, aoParam);
        }
    
    // Accessor for the property "_ReadOnly"
    /**
     * Getter for property _ReadOnly.<p>
    * Specifies whether or not only the viewing of attributes is allowed.
     */
    public boolean is_ReadOnly()
        {
        return __m__ReadOnly;
        }
    
    // Accessor for the property "_Snapshot"
    /**
     * Getter for property _Snapshot.<p>
    * Specifies whether or not this model is a snapshot of the remote model.
     */
    public boolean is_Snapshot()
        {
        return __m__Snapshot;
        }
    
    // Declared at the super level
    /**
     * Determine if the current model has subscriptions
     */
    public boolean is_SubscribedTo()
        {
        // import Component.Net.Management.NotificationHandler.RemoteHandler;
        
        if (super.is_SubscribedTo())
            {
            return true;
            }
        
        RemoteHandler handler = get_RemoteNotificationHandler();
        return handler != null && handler.isSubscribedTo();
        }
    
    // From interface: com.tangosol.io.ExternalizableLite
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        set_Snapshot(true);
        
        set_ModelName(ExternalizableHelper.readSafeUTF(in));
        set_InvocationResult(ExternalizableHelper.readObject(in));
        
        Map mapSnapshot = get_SnapshotMap();
        mapSnapshot.put("Description", ExternalizableHelper.readSafeUTF(in));
        
        set_RefreshTimeMillis(Base.getSafeTimeMillis());
        }
    
    // Accessor for the property "_InvocationResult"
    /**
     * Setter for property _InvocationResult.<p>
    * This field of the LocalMode component is used to bring back a result of
    * remote invocation (setAttribute or invoke) along with the latest model
    * snapshot.
    * 
    * @see RemoteModel#run()
     */
    public void set_InvocationResult(Object oResult)
        {
        __m__InvocationResult = oResult;
        }
    
    // Accessor for the property "_ReadOnly"
    /**
     * Setter for property _ReadOnly.<p>
    * Specifies whether or not only the viewing of attributes is allowed.
     */
    public void set_ReadOnly(boolean flag)
        {
        __m__ReadOnly = flag;
        }
    
    // Accessor for the property "_RefreshTimeMillis"
    /**
     * Setter for property _RefreshTimeMillis.<p>
    * The [safe] timestamp when the model was last refreshed.
     */
    public void set_RefreshTimeMillis(long ldt)
        {
        __m__RefreshTimeMillis = ldt;
        }
    
    // Accessor for the property "_RemoteNotificationHandler"
    /**
     * Setter for property _RemoteNotificationHandler.<p>
    * The RemoteHandler component managing remote subscriptions.
     */
    protected void set_RemoteNotificationHandler(com.tangosol.coherence.component.net.management.notificationHandler.RemoteHandler handler)
        {
        __m__RemoteNotificationHandler = handler;
        }
    
    // Accessor for the property "_Sequence"
    /**
     * Setter for property _Sequence.<p>
    * the Notification sequence counter
     */
    protected void set_Sequence(java.util.concurrent.atomic.AtomicLong sequence)
        {
        __m__Sequence = sequence;
        }
    
    // Accessor for the property "_Snapshot"
    /**
     * Setter for property _Snapshot.<p>
    * Specifies whether or not this model is a snapshot of the remote model.
     */
    protected void set_Snapshot(boolean flag)
        {
        // import com.tangosol.util.Base;
        // import java.util.HashMap;
        
        _assert(flag, "One way property");
        
        __m__Snapshot = (flag);
        
        set_SnapshotMap(new HashMap());
        }
    
    // Accessor for the property "_SnapshotMap"
    /**
     * Setter for property _SnapshotMap.<p>
    * The map of the snapshot attributes. The keys are the attribute accessor
    * names, the values are cached attribute values. Meaningful only for a
    * snapshot model.
     */
    protected void set_SnapshotMap(java.util.Map map)
        {
        __m__SnapshotMap = map;
        }
    
    // From interface: com.tangosol.io.ExternalizableLite
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        _assert(!is_Snapshot());
        
        ExternalizableHelper.writeSafeUTF(out, get_ModelName());
        ExternalizableHelper.writeObject(out, get_InvocationResult());
        ExternalizableHelper.writeSafeUTF(out, getDescription());
        }
    }
