
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.RemoteModel

package com.tangosol.coherence.component.net.management.model;

import com.tangosol.coherence.component.net.management.Connector;
import com.tangosol.coherence.component.net.management.Gateway;
import com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder;
import com.tangosol.coherence.component.net.management.model.LocalModel;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Continuation;
import com.tangosol.net.InvocationService;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Model components implement the JMX-managed functionality of the
 * corresponding MBeans without being dependent on any JMX classes and could be
 * used both in-process and out-of-process (relative to an MBeanServer).
 * 
 * RemoteModel components use InvocationService to retrive a snapshot of the
 * LocalModel (which exists on another VM) and then invoke a corresponding
 * operation on a snapshot model.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class RemoteModel
        extends    com.tangosol.coherence.component.net.management.Model
        implements com.tangosol.io.ExternalizableLite,
                   com.tangosol.net.Invocable,
                   com.tangosol.net.InvocationObserver,
                   com.tangosol.net.PriorityTask
    {
    // ---- Fields declarations ----
    
    /**
     * Property Accessed
     *
     * Indicates that this model was accessed since the last refresh.
     */
    private transient boolean __m_Accessed;
    
    /**
     * Property AccessTime
     *
     * Specifies the last time the model was accessed.  Used to ensure the data
     * integrity between read and refresh.
     */
    private transient long __m_AccessTime;
    
    /**
     * Property Connector
     *
     */
    private com.tangosol.coherence.component.net.management.Connector __m_Connector;
    
    /**
     * Property Continuation
     *
     * The Continuation used by the RemoteModel when an invocation is performed
     * on the service thread.
     * 
     * @see  #invocationCompleted
     */
    private com.oracle.coherence.common.base.Continuation __m_Continuation;
    
    /**
     * Property ExecutingFlag
     *
     * Serves as a mutex to prevent concurrent network calls.
     * 
     * @volatile
     */
    private volatile transient boolean __m_ExecutingFlag;
    
    /**
     * Property ExecutionLastTimeMillis
     *
     * The last date time a remote execution call was made.  This is used when
     * determining if a node has timed out and not returned a result from a
     * prior call. 
     */
    private transient long __m_ExecutionLastTimeMillis;
    
    /**
     * Property ExecutionTimeoutMillis
     *
     * Task execution timeout is currently no supported.
     */
    private long __m_ExecutionTimeoutMillis;
    
    /**
     * Property InvocationObserver
     *
     * An InvocationObserver used to process asynchronous response from the
     * Model owner.
     * 
     * Note: the execute mutex ensures a single async invocation occurs at a
     * time.
     */
    private RemoteModel.InvocationObserver __m_InvocationObserver;
    
    /**
     * Property InvokeName
     *
     * The method name to call on the remote model.
     */
    private String __m_InvokeName;
    
    /**
     * Property InvokeOp
     *
     * The operation to be used by the remote model. 
     * 
     * Valid values: OP_GET, OP_INVOKE, OP_SET
     */
    private transient int __m_InvokeOp;
    
    /**
     * Property InvokeParam
     *
     * The parameters to be used by a reflection call on the remote model .
     */
    private Object[] __m_InvokeParam;
    
    /**
     * Property InvokeSignature
     *
     * The method parameter signature to be used by a Dynamic MBean Invoke call
     * on the remote node.  
     */
    private String[] __m_InvokeSignature;
    
    /**
     * Property ModelOwner
     *
     * (Transient) The cluster Member that holds (owns) the corresponding local
     * model.
     */
    private transient com.tangosol.net.Member __m_ModelOwner;
    
    /**
     * Property MUTEX_ACQUIRED_NOWAIT
     *
     * Indicates that acquireExecutionMutex call  immediately acquired the
     * mutex.
     */
    public static final int MUTEX_ACQUIRED_NOWAIT = 1;
    
    /**
     * Property MUTEX_ACQUIRED_WAIT
     *
     * Indicates that acquireExecutionMutex call had to wait before acquiring
     * the mutex.
     */
    public static final int MUTEX_ACQUIRED_WAIT = 2;
    
    /**
     * Property MUTEX_TIMEOUT
     *
     * Indicates that acquireExecutionMutex failed to acquire the mutex (with
     * or without waiting).
     */
    public static final int MUTEX_TIMEOUT = -1;
    
    /**
     * Property OP_GET
     *
     * Denotes getAttribute operation.
     */
    public static final int OP_GET = 1;
    
    /**
     * Property OP_INVOKE
     *
     * Denotes invoke operation.
     */
    public static final int OP_INVOKE = 2;
    
    /**
     * Property OP_SET
     *
     * Denotes setAttribute operation.
     */
    public static final int OP_SET = 3;
    
    /**
     * Property PauseDuration
     *
     * Specifies the number of milliseconds that must pass since the last
     * access before the model is considered "inactive".
     */
    private transient long __m_PauseDuration;
    
    /**
     * Property Snapshot
     *
     * The snapshot of the remote model.
     */
    private transient LocalModel __m_Snapshot;
    
    /**
     * Property SnapshotNext
     *
     * There exists a possibility that while one client thread retrieves
     * attributes from a Snapshot the snapshot expires and gets retrieved again
     * causing the client to see values from different time series.  The
     * SnapshotNext represents the remote model snapshot that has been
     * retrieved but not yet applied.
     */
    private transient LocalModel __m_SnapshotNext;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("InvocationObserver", RemoteModel.InvocationObserver.get_CLASS());
        }
    
    // Default constructor
    public RemoteModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public RemoteModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setAccessed(false);
            setExecutingFlag(false);
            setExecutionTimeoutMillis(0L);
            setPauseDuration(128L);
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        
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
        return new com.tangosol.coherence.component.net.management.model.RemoteModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/RemoteModel".replace('/', '.'));
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
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    // Declared at the super level
    /**
     * Add the Notification listener to the model.
    * 
    * @return the LocalHolder representing the listener/filter/handback tuple
    * or null if such a registration already exists
     */
    public com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder _addNotificationListener(javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
        {
        // import Component.Net.Management.ListenerHolder.LocalHolder;
        
        LocalHolder holder = super._addNotificationListener(listener, filter, handback);
        
        if (holder != null)
            {
            getConnector().subscribe(
                get_ModelName(), getModelOwner(), holder.ensureRemoteHolder());
            }
        return holder;
        }
    
    // Declared at the super level
    /**
     * Remove all the notifications for the specified listener from the model.
    * 
    * @return a Set<LocalHolder> of holders associated with the specified
    * listener
     */
    public java.util.Set _removeNotificationListener(javax.management.NotificationListener listener)
        {
        // import Component.Net.Management.ListenerHolder.LocalHolder;
        // import java.util.Set;
        
        Set setHolders = super._removeNotificationListener(listener);
        if (setHolders != null)
            {
            getConnector().unsubscribe(get_ModelName(), getModelOwner(), setHolders);
            }
        return setHolders;
        }
    
    // Declared at the super level
    /**
     * Remove the Notification Listener from the model.
    * 
    * @return the LocalHolder representing the listener/filter/handback tuple
    * or null if the registration did not exist
     */
    public com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder _removeNotificationListener(javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
        {
        // import Component.Net.Management.ListenerHolder.LocalHolder;
        // import java.util.Collections;
        
        LocalHolder holder = super._removeNotificationListener(listener, filter, handback);
        if (holder != null)
            {
            getConnector().unsubscribe(get_ModelName(), getModelOwner(), Collections.singleton(holder));
            }
        return holder;
        }
    
    /**
     * Retrieve a mutex for singular network call.
    * 
    * @param cTimeout number of milliseconds to wait for mutex acquisition
    * 
    * @return one of the MUTEX_* values
     */
    public int acquireExecuteMutex(long cTimeout)
        {
        // import Component.Net.Management.Connector;
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.util.Base;
        
        int     nWaited = MUTEX_ACQUIRED_NOWAIT; // assume success
        boolean fWaited = false;
        
        synchronized (this)
            {
            while (isExecutingFlag())
                {
                try
                    {
                    long cElapsed = Base.getSafeTimeMillis() - getExecutionLastTimeMillis();
                    if (cElapsed < cTimeout)
                        {
                        nWaited = MUTEX_ACQUIRED_WAIT;
                        fWaited = true;
                        Blocking.wait(this, cTimeout - cElapsed);
                        }
                    else
                        {
                        nWaited = MUTEX_TIMEOUT;
                        if (fWaited)
                            {
                            Connector conn = getConnector();
                            conn.setStatsRefreshTimeoutCount(
                                conn.getStatsRefreshTimeoutCount() + 1);
                            }
                        break;
                        }
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw Base.ensureRuntimeException(e);
                    }
                }
        
            if (nWaited != MUTEX_TIMEOUT)
                {
                setExecutingFlag(true);
                if (nWaited == MUTEX_ACQUIRED_NOWAIT)
                    {
                    setExecutionLastTimeMillis(Base.getSafeTimeMillis());
                    }
                }
            }
        
        return nWaited;
        }
    
    /**
     * Return a clone of this RemoteModel.
     */
    public RemoteModel cloneModel()
        {
        RemoteModel model = new RemoteModel();
        
        model.set_ModelName(get_ModelName());
        model.setConnector(getConnector());
        model.setAccessed(model.isAccessed());
        model.setModelOwner(model.getModelOwner());
        
        return model;
        }
    
    /**
     * Remote op.
     */
    public Object doGet(String sName)
        {
        // import com.tangosol.util.Base;
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        // import java.util.Map;
        
        _assert(sName != null);
        
        Connector connector = getConnector();
        if (connector.getInvokeContinuation() != null)
            {
            return invokeRemote(OP_GET, sName, null);
            }
        
        LocalModel model   = connector.ensureFreshSnapshot(this);
        Map        mapAttr = model.get_SnapshotMap();
        
        setAccessed(true);
        
        if (sName.startsWith("is"))
            {   
            sName = sName.substring(2);
            }
        else if (sName.startsWith("get"))
            {
            sName = sName.substring(3);
            }
        
        setAccessTime(Base.getSafeTimeMillis());
        
        // else the sName is a Dynamic Model Attribute
        
        // TODO: consider a token response that would force an actual
        // method call:
        // if (mapAttr.get(sName) == LocalModel.VALUE_CALCULATED)
        //    {
        //    return ClassHelper.invoke(model, sName, ClassHelper.VOID);
        //    }
        return mapAttr.get(sName);
        }
    
    /**
     * Remote op.
     */
    public void doSet(String sName, Object[] aoParam)
        {
        invokeRemote(OP_SET, sName, aoParam);
        }
    
    // Accessor for the property "AccessTime"
    /**
     * Getter for property AccessTime.<p>
    * Specifies the last time the model was accessed.  Used to ensure the data
    * integrity between read and refresh.
     */
    protected long getAccessTime()
        {
        return __m_AccessTime;
        }
    
    // Accessor for the property "AttributeTimeoutMillis"
    /**
     * Getter for property AttributeTimeoutMillis.<p>
    * The time in milliseconds that the model snapshot can be used before a
    * refresh is required.
     */
    public long getAttributeTimeoutMillis()
        {
        return getConnector().getRefreshAttributeTimeoutMillis();
        }
    
    // Accessor for the property "Connector"
    /**
     * Getter for property Connector.<p>
     */
    public com.tangosol.coherence.component.net.management.Connector getConnector()
        {
        return __m_Connector;
        }
    
    // Accessor for the property "Continuation"
    /**
     * Getter for property Continuation.<p>
    * The Continuation used by the RemoteModel when an invocation is performed
    * on the service thread.
    * 
    * @see  #invocationCompleted
     */
    protected com.oracle.coherence.common.base.Continuation getContinuation()
        {
        return __m_Continuation;
        }
    
    // Accessor for the property "ExecutionLastTimeMillis"
    /**
     * Getter for property ExecutionLastTimeMillis.<p>
    * The last date time a remote execution call was made.  This is used when
    * determining if a node has timed out and not returned a result from a
    * prior call. 
     */
    public long getExecutionLastTimeMillis()
        {
        // import com.tangosol.util.Base;
        
        long cMillis = __m_ExecutionLastTimeMillis;
        
        return cMillis == 0L ? Base.getSafeTimeMillis() : cMillis;
        }
    
    // From interface: com.tangosol.net.PriorityTask
    // Accessor for the property "ExecutionTimeoutMillis"
    /**
     * Getter for property ExecutionTimeoutMillis.<p>
    * Task execution timeout is currently no supported.
     */
    public long getExecutionTimeoutMillis()
        {
        return __m_ExecutionTimeoutMillis;
        }
    
    // Accessor for the property "InvocationObserver"
    /**
     * Getter for property InvocationObserver.<p>
    * An InvocationObserver used to process asynchronous response from the
    * Model owner.
    * 
    * Note: the execute mutex ensures a single async invocation occurs at a
    * time.
     */
    public RemoteModel.InvocationObserver getInvocationObserver()
        {
        return __m_InvocationObserver;
        }
    
    // Accessor for the property "InvokeName"
    /**
     * Getter for property InvokeName.<p>
    * The method name to call on the remote model.
     */
    public String getInvokeName()
        {
        return __m_InvokeName;
        }
    
    // Accessor for the property "InvokeOp"
    /**
     * Getter for property InvokeOp.<p>
    * The operation to be used by the remote model. 
    * 
    * Valid values: OP_GET, OP_INVOKE, OP_SET
     */
    public int getInvokeOp()
        {
        return __m_InvokeOp;
        }
    
    // Accessor for the property "InvokeParam"
    /**
     * Getter for property InvokeParam.<p>
    * The parameters to be used by a reflection call on the remote model .
     */
    public Object[] getInvokeParam()
        {
        return __m_InvokeParam;
        }
    
    // Accessor for the property "InvokeSignature"
    /**
     * Getter for property InvokeSignature.<p>
    * The method parameter signature to be used by a Dynamic MBean Invoke call
    * on the remote node.  
     */
    public String[] getInvokeSignature()
        {
        return __m_InvokeSignature;
        }
    
    // Accessor for the property "ModelOwner"
    /**
     * Getter for property ModelOwner.<p>
    * (Transient) The cluster Member that holds (owns) the corresponding local
    * model.
     */
    public com.tangosol.net.Member getModelOwner()
        {
        return __m_ModelOwner;
        }
    
    // Accessor for the property "PauseDuration"
    /**
     * Getter for property PauseDuration.<p>
    * Specifies the number of milliseconds that must pass since the last access
    * before the model is considered "inactive".
     */
    public long getPauseDuration()
        {
        return __m_PauseDuration;
        }
    
    /**
     * @return the request timeout configured for the underlying
    * InvocationService
     */
    public long getRequestTimeout()
        {
        return getConnector().getRequestTimeout();
        }
    
    // From interface: com.tangosol.net.PriorityTask
    public long getRequestTimeoutMillis()
        {
        return getInvokeOp() == OP_GET
                ? getConnector().getRefreshRequestTimeoutMillis()
                : getConnector().getRequestTimeout();   // OP_INVOKE, OP_SET
        }
    
    // From interface: com.tangosol.net.Invocable
    // Accessor for the property "Result"
    /**
     * Getter for property Result.<p>
    * The result of the Invocation.
     */
    public Object getResult()
        {
        return getSnapshot();
        }
    
    // From interface: com.tangosol.net.PriorityTask
    public int getSchedulingPriority()
        {
        return 0;
        }
    
    // Accessor for the property "Snapshot"
    /**
     * Returns the snapshot and 
     */
    public LocalModel getSnapshot()
        {
        LocalModel modelNext = getSnapshotNext();
        
        // check if a fresh snapshot exists and should be used
        if (modelNext != null && !isActive())
            {
            int nMutex = acquireExecuteMutex(getAttributeTimeoutMillis());
            try
                {
                modelNext = getSnapshotNext();
                if (modelNext != null)
                    {
                    setSnapshot(modelNext);
                    setSnapshotNext(null);
                    }
                }
            finally
                {
                if (nMutex != MUTEX_TIMEOUT)
                    { 
                    releaseExecuteMutex();
                    }
                }
            }
        
        return __m_Snapshot;
        }
    
    // Accessor for the property "SnapshotNext"
    /**
     * Getter for property SnapshotNext.<p>
    * There exists a possibility that while one client thread retrieves
    * attributes from a Snapshot the snapshot expires and gets retrieved again
    * causing the client to see values from different time series.  The
    * SnapshotNext represents the remote model snapshot that has been retrieved
    * but not yet applied.
     */
    public LocalModel getSnapshotNext()
        {
        return __m_SnapshotNext;
        }
    
    // From interface: com.tangosol.net.Invocable
    public void init(com.tangosol.net.InvocationService service)
        {
        // import Component.Net.Management.Connector;
        
        setConnector((Connector) service.getUserContext());
        }
    
    // From interface: com.tangosol.net.InvocationObserver
    /**
     * Method sets the state of the invocation to "false" (i.e no invocation
    * call is pending), notifies all threads waiting for the invocation to
    * complete and sets the number of uses of the MBean to zero(0).  The use
    * count is used to determine if the 
     */
    public void invocationCompleted()
        {
        // import com.oracle.coherence.common.base.Continuation;
        
        Continuation cont = getContinuation();
        if (cont == null)
            {
            releaseExecuteMutex();
            }
        else
            {
            // depending on the refresh cycle the latest result could
            // be either in SnapshotNext or Snapshot
            LocalModel model = getSnapshotNext();
            if (model == null)
                {
                model = getSnapshot();
                }
        
            cont.proceed(model.get_InvocationResult());
            setContinuation(null);
            }
        }
    
    // Declared at the super level
    /**
     * Invoke the method with the specified name and parameters on the MBean
    * represented by this model.
    * Invoke the specified operation on this model.
     */
    public Object invoke(int nOp, String sName, Object[] aoParam)
            throws java.lang.IllegalAccessException,
                   java.lang.NoSuchMethodException,
                   java.lang.reflect.InvocationTargetException,
                   javax.management.MBeanException
        {
        return invoke(nOp, sName, aoParam, null);
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
        switch (nOp)
            {
            case OP_GET:
                return doGet(sName);
               
            case OP_SET:
                doSet(sName, aoParam);
                return null;
        
            case OP_INVOKE:
                return invokeRemote(nOp, sName, aoParam, asSignature);
                    
            default:
                throw new IllegalStateException();
            }
        }
    
    /**
     * Invoke remote method (optional) and bring it back along with a fresh
    * snapshot.
    * 
    * @param sName  name of a remote method; null indicates a request for all
    * attributes
     */
    public Object invokeRemote(int nOp, String sName, Object[] aoParam)
        {
        return invokeRemote(nOp, sName, aoParam, null);
        }
    
    /**
     * Invoke remote method (optional) and bring it back along with a fresh
    * snapshot.
    * 
    * @param sName  name of a remote method; null indicates a request for all
    * attributes
     */
    public Object invokeRemote(int nOp, String sName, Object[] aoParam, String[] asSignature)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        // import com.oracle.coherence.common.base.Continuation;
        // import com.tangosol.net.InvocationService;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.Base;
        // import java.util.Collections;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;
        
        int nWaited = MUTEX_TIMEOUT;
        try
            {
            if (sName == null)
                {
                // All attributes retrieval (sName == null)
                invokeRemoteAsync();
                
                // wait for the asynchronous call to return
                nWaited = acquireExecuteMutex(getAttributeTimeoutMillis());
                return getSnapshot();
                }
        
            // remote method invocation; use service request timeout (COH-6071)
            nWaited = acquireExecuteMutex(0L);
            
            RemoteModel task = nWaited == MUTEX_TIMEOUT
                ? cloneModel() : this;
        
            task.setInvokeOp(nOp);
            task.setInvokeName(sName);
            task.setInvokeParam(aoParam);
            task.setInvokeSignature(asSignature);
        
            Connector         connector = getConnector();
            InvocationService service   = connector.getService();
            Set               setTarget = Collections.singleton(getModelOwner());
            Continuation      cont      = connector.getInvokeContinuation();
            if (cont != null)
                {
                if (service == null)
                    {
                    cont.proceed(new RuntimeException("Service has been stopped"));
                    }
                else
                    {
                    RemoteModel.InvocationObserver observer = (RemoteModel.InvocationObserver) _newChild("InvocationObserver");
                    observer.setContinuation(cont);
        
                    service.execute(task, setTarget, observer);
                    }
                return null;
                }
        
            if (service == null)
                {
                throw new RuntimeException("Service has been stopped");
                }
            
            Map    mapResult = service.query(task, setTarget);
            Object oResult   = null;
           
            if (!mapResult.isEmpty())
                {
                java.util.Map.Entry      entry = (java.util.Map.Entry) mapResult.entrySet().iterator().next();
                LocalModel model = (LocalModel) entry.getValue();
        
                if (model == null)
                    {
                    // the model is missing on the target
                    }
                else
                    {
                    oResult = model.get_InvocationResult();
        
                    if (oResult instanceof Throwable)
                        {
                        throw Base.ensureRuntimeException((Throwable) oResult);
                        }
        
                    if (nOp == OP_INVOKE)
                        {
                        // ensure the model is refreshed immediately as the invoke may
                        // have altered MBean state that should be observable
                        setAccessTime(0L);
                        }
                    setSnapshot(model);
                    }
                }
            return oResult;
            }
        catch (RequestTimeoutException e)
            {
            throw Base.ensureRuntimeException(e,
                "Timeout occurred when executing remote method");
            }
        finally
            {
            // If the asynchronous call doesn't return in the specified timeframe,
            // the mutex should not be released.  The release will occur when "blocking"
            // thread completes (or is terminated). This is done in the invocationCompleted()
            // method.
        
            if (nWaited != MUTEX_TIMEOUT)
                {
                releaseExecuteMutex();
                }
            }
        }
    
    /**
     * Invoke an asynchronous request for a remote snapshot.
    * 
    * @return true if an invocation request  has been issued
     */
    public boolean invokeRemoteAsync()
        {
        // import com.tangosol.net.InvocationService;
        // import java.util.Collections;
        
        int nMutex = MUTEX_TIMEOUT;
        try
            {
            nMutex = acquireExecuteMutex(0L);
            if (nMutex == MUTEX_ACQUIRED_NOWAIT)
                {
                RemoteModel task = this;
                task.setInvokeOp(OP_GET);
                task.setInvokeName(null);
                task.setInvokeParam(null);
        
                InvocationService service = getConnector().getService();
                if (service != null)
                    {
                    service.execute(task, Collections.singleton(getModelOwner()), getInvocationObserver());
                    return true;
                    }
                }
            }
        finally
            {
            if (nMutex == MUTEX_ACQUIRED_WAIT)
                {
                releaseExecuteMutex();
                }
            }
        
        return false;
        }
    
    // Accessor for the property "Accessed"
    /**
     * Getter for property Accessed.<p>
    * Indicates that this model was accessed since the last refresh.
     */
    public boolean isAccessed()
        {
        return __m_Accessed;
        }
    
    // Accessor for the property "Active"
    /**
     * True if the model is currently being accessed by a client.
     */
    public boolean isActive()
        {
        // import com.tangosol.util.Base;
        
        // notice that uninitialized AccessTime will yield Active == false
        return Base.getSafeTimeMillis() < getAccessTime() + getPauseDuration();
        }
    
    // Accessor for the property "ExecutingFlag"
    /**
     * Getter for property ExecutingFlag.<p>
    * Serves as a mutex to prevent concurrent network calls.
    * 
    * @volatile
     */
    public boolean isExecutingFlag()
        {
        return __m_ExecutingFlag;
        }
    
    // Accessor for the property "RefreshRequired"
    /**
     * Boolean used to determine if the snapshot should be refreshed.
     */
    public boolean isRefreshRequired()
        {
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.Management.Connector;
        
        // the "next" snapshot is always fresher (if exists)
        
        LocalModel modelNext = getSnapshotNext();
        LocalModel model     = modelNext == null ? getSnapshot() : modelNext;
        
        Connector connector = getConnector();
        if (connector == null || model == null)
            {
            return false;
            }
        
        return !isExecutingFlag() && model._checkExpired(connector.getRefreshTimeoutMillis());
        }
    
    // Accessor for the property "ResponsibilityMBean"
    /**
     * Getter for property ResponsibilityMBean.<p>
    * Return true if this RemoteModel represents a responsibility MBean.
     */
    public boolean isResponsibilityMBean()
        {
        // import Component.Net.Management.Gateway;
        
        return Gateway.isResponsibilityMBean(get_ModelName());
        }
    
    // From interface: com.tangosol.net.InvocationObserver
    /**
     * Processes the result of the remote invocation.
     */
    public void memberCompleted(com.tangosol.net.Member member, Object oResult)
        {
        LocalModel model = (LocalModel) oResult;
        
        if (model == null)
            {
            _trace("Missing result for " + get_ModelName() + " from " + member, 4);
            }
        else
            {
            setSnapshot(model);
            }
        }
    
    // From interface: com.tangosol.net.InvocationObserver
    public void memberFailed(com.tangosol.net.Member member, Throwable exception)
        {
        }
    
    // From interface: com.tangosol.net.InvocationObserver
    public void memberLeft(com.tangosol.net.Member member)
        {
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification called out of
    * setConstructed() for the topmost component and that in turn notifies all
    * the children.
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as) the control returns back to the
    * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        setInvocationObserver((RemoteModel.InvocationObserver) _newChild("InvocationObserver"));
        }
    
    // From interface: com.tangosol.io.ExternalizableLite
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        set_ModelName(ExternalizableHelper.readSafeUTF(in));
        setInvokeName(ExternalizableHelper.readSafeUTF(in));
        setInvokeParam((Object[]) ExternalizableHelper.readObject(in));
        setInvokeOp(ExternalizableHelper.readInt(in));
        
        boolean fSig = in.readBoolean(); // Boolean to determine if Signature array is not null.
        if (fSig)
            {
            setInvokeSignature((String[]) ExternalizableHelper.readObject(in));
            }
        }
    
    /**
     * Method to set Critical section for Invocation.
     */
    public void releaseExecuteMutex()
        {
        synchronized (this)
            {
            setExecutingFlag(false);
            notify();
            }
        }
    
    // From interface: com.tangosol.net.Invocable
    /**
     * Refreshes the data, sets a property or performs an invoke on a remote
    * server.
     */
    public void run()
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        // import com.tangosol.util.ClassHelper;
        
        String   sModel  = get_ModelName();
        String   sMethod = getInvokeName();
        Object[] aoParam = getInvokeParam();
        int      nOp     = getInvokeOp();
        String[] asSign  = getInvokeSignature();
        
        LocalModel model = (LocalModel) getConnector().getLocalRegistry().get(sModel);
        if (model == null)
            {
            if (!isResponsibilityMBean())
                {
                _trace("Missing model " + sModel, 4);
                }
            // else an MBeanServer may temporarily have many model owners and
            // returning a null result will allow the MBeanServer to correct itself
            }
        else
            {
            // Note: we don't need to worry about concurrency
            // since this instance is a deserialized clone
            setSnapshot(model);
            if (sMethod == null)
                {
                model.set_InvocationResult(null);
                }
            else
                {
                try
                    {
                    if (aoParam == null)
                        {
                        aoParam = ClassHelper.VOID;
                        }
                    model.set_InvocationResult(model.invoke(nOp, sMethod, aoParam, asSign));
                    }
                catch (Throwable e)
                    {
                    model.set_InvocationResult(e);
                    }
                }
            }
        }
    
    // From interface: com.tangosol.net.PriorityTask
    public void runCanceled(boolean fAbandoned)
        {
        }
    
    // Accessor for the property "Accessed"
    /**
     * Used is set to true when a doGet is executed and false when an
    * "unrequested" retrieval is done.    
     */
    public void setAccessed(boolean fUsed)
        {
        __m_Accessed = fUsed;
        }
    
    // Accessor for the property "AccessTime"
    /**
     * Setter for property AccessTime.<p>
    * Specifies the last time the model was accessed.  Used to ensure the data
    * integrity between read and refresh.
     */
    protected void setAccessTime(long ldt)
        {
        __m_AccessTime = ldt;
        }
    
    // Accessor for the property "Connector"
    /**
     * Setter for property Connector.<p>
     */
    public void setConnector(com.tangosol.coherence.component.net.management.Connector connector)
        {
        __m_Connector = connector;
        }
    
    // Accessor for the property "Continuation"
    /**
     * Setter for property Continuation.<p>
    * The Continuation used by the RemoteModel when an invocation is performed
    * on the service thread.
    * 
    * @see  #invocationCompleted
     */
    protected void setContinuation(com.oracle.coherence.common.base.Continuation continuation)
        {
        __m_Continuation = continuation;
        }
    
    // Accessor for the property "ExecutingFlag"
    /**
     * Setter for property ExecutingFlag.<p>
    * Serves as a mutex to prevent concurrent network calls.
    * 
    * @volatile
     */
    protected void setExecutingFlag(boolean fExecuting)
        {
        __m_ExecutingFlag = fExecuting;
        }
    
    // Accessor for the property "ExecutionLastTimeMillis"
    /**
     * Setter for property ExecutionLastTimeMillis.<p>
    * The last date time a remote execution call was made.  This is used when
    * determining if a node has timed out and not returned a result from a
    * prior call. 
     */
    protected void setExecutionLastTimeMillis(long cMillis)
        {
        __m_ExecutionLastTimeMillis = cMillis;
        }
    
    // Accessor for the property "ExecutionTimeoutMillis"
    /**
     * Setter for property ExecutionTimeoutMillis.<p>
    * Task execution timeout is currently no supported.
     */
    protected void setExecutionTimeoutMillis(long cMillis)
        {
        __m_ExecutionTimeoutMillis = cMillis;
        }
    
    // Accessor for the property "InvocationObserver"
    /**
     * Setter for property InvocationObserver.<p>
    * An InvocationObserver used to process asynchronous response from the
    * Model owner.
    * 
    * Note: the execute mutex ensures a single async invocation occurs at a
    * time.
     */
    public void setInvocationObserver(RemoteModel.InvocationObserver observerInvocation)
        {
        __m_InvocationObserver = observerInvocation;
        }
    
    // Accessor for the property "InvokeName"
    /**
     * Setter for property InvokeName.<p>
    * The method name to call on the remote model.
     */
    protected void setInvokeName(String sName)
        {
        __m_InvokeName = sName;
        }
    
    // Accessor for the property "InvokeOp"
    /**
     * Setter for property InvokeOp.<p>
    * The operation to be used by the remote model. 
    * 
    * Valid values: OP_GET, OP_INVOKE, OP_SET
     */
    protected void setInvokeOp(int nOp)
        {
        __m_InvokeOp = nOp;
        }
    
    // Accessor for the property "InvokeParam"
    /**
     * Setter for property InvokeParam.<p>
    * The parameters to be used by a reflection call on the remote model .
     */
    protected void setInvokeParam(Object[] aoParams)
        {
        __m_InvokeParam = aoParams;
        }
    
    // Accessor for the property "InvokeSignature"
    /**
     * Setter for property InvokeSignature.<p>
    * The method parameter signature to be used by a Dynamic MBean Invoke call
    * on the remote node.  
     */
    protected void setInvokeSignature(String[] asSig)
        {
        __m_InvokeSignature = asSig;
        }
    
    // Accessor for the property "ModelOwner"
    /**
     * Setter for property ModelOwner.<p>
    * (Transient) The cluster Member that holds (owns) the corresponding local
    * model.
     */
    public void setModelOwner(com.tangosol.net.Member member)
        {
        __m_ModelOwner = member;
        }
    
    // Accessor for the property "PauseDuration"
    /**
     * Setter for property PauseDuration.<p>
    * Specifies the number of milliseconds that must pass since the last access
    * before the model is considered "inactive".
     */
    public void setPauseDuration(long cMillis)
        {
        __m_PauseDuration = cMillis;
        }
    
    // Accessor for the property "Snapshot"
    /**
     * Setter for property Snapshot.<p>
    * The snapshot of the remote model.
     */
    public void setSnapshot(LocalModel model)
        {
        // import java.util.Map;
        // import java.util.Date;
        
        Map mapSnapshot = model.get_SnapshotMap();
        
        mapSnapshot.put("RefreshTime", new Date(System.currentTimeMillis()));
        
        if (isActive())
            {
            // store the new snapshot off
            // (see comments for SnapshotNext property)
            setSnapshotNext(model);
            }
        else
            {
            __m_Snapshot = (model);
            setSnapshotNext(null);
            }
        }
    
    // Accessor for the property "SnapshotNext"
    /**
     * Setter for property SnapshotNext.<p>
    * There exists a possibility that while one client thread retrieves
    * attributes from a Snapshot the snapshot expires and gets retrieved again
    * causing the client to see values from different time series.  The
    * SnapshotNext represents the remote model snapshot that has been retrieved
    * but not yet applied.
     */
    protected void setSnapshotNext(LocalModel model)
        {
        __m_SnapshotNext = model;
        }
    
    // Declared at the super level
    public String toString()
        {
        return super.toString() + ", owner=" + getModelOwner();
        }
    
    // From interface: com.tangosol.io.ExternalizableLite
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        String[] asSig = getInvokeSignature();
        boolean  fSig  = asSig != null;
        
        ExternalizableHelper.writeSafeUTF(out, get_ModelName());
        ExternalizableHelper.writeSafeUTF(out, getInvokeName());
        ExternalizableHelper.writeObject (out, getInvokeParam());
        ExternalizableHelper.writeInt    (out, getInvokeOp());
        
        // write out a true if a signature exists (work around NPE from ExtHelper).
        out.writeBoolean(fSig);
        if (fSig)
            { 
            ExternalizableHelper.writeObject (out, getInvokeSignature());
            }
        }

    // ---- class: com.tangosol.coherence.component.net.management.model.RemoteModel$InvocationObserver
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InvocationObserver
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.net.InvocationObserver
        {
        // ---- Fields declarations ----
        
        /**
         * Property Continuation
         *
         */
        private com.oracle.coherence.common.base.Continuation __m_Continuation;
        
        // Default constructor
        public InvocationObserver()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public InvocationObserver(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.management.model.RemoteModel.InvocationObserver();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/net/management/model/RemoteModel$InvocationObserver".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "Continuation"
        /**
         * Getter for property Continuation.<p>
         */
        public com.oracle.coherence.common.base.Continuation getContinuation()
            {
            return __m_Continuation;
            }
        
        // Accessor for the property "RemoteModel"
        /**
         * Getter for property RemoteModel.<p>
         */
        public RemoteModel getRemoteModel()
            {
            return (RemoteModel) get_Parent();
            }
        
        // From interface: com.tangosol.net.InvocationObserver
        public void invocationCompleted()
            {
            // import com.oracle.coherence.common.base.Continuation;
            
            RemoteModel      remoteModel = getRemoteModel();
            Continuation cont        = getContinuation();
            if (cont == null)
                {
                remoteModel.releaseExecuteMutex();
                }
            else
                {
                // depending on the refresh cycle the latest result could
                // be either in SnapshotNext or Snapshot
                LocalModel model = remoteModel.getSnapshotNext();
                if (model == null)
                    {
                    model = remoteModel.getSnapshot();
                    }
            
                cont.proceed(model.get_InvocationResult());
                setContinuation(null);
                }
            }
        
        // From interface: com.tangosol.net.InvocationObserver
        public void memberCompleted(com.tangosol.net.Member member, Object oResult)
            {
            RemoteModel    remoteModel = getRemoteModel();
            LocalModel model       = (LocalModel) oResult;
            
            if (model == null)
                {
                _trace("Missing result for " + remoteModel.get_ModelName() +
                    " from " + member, 4);
                }
            else
                {
                remoteModel.setSnapshot(model);
                }
            }
        
        // From interface: com.tangosol.net.InvocationObserver
        public void memberFailed(com.tangosol.net.Member member, Throwable exception)
            {
            }
        
        // From interface: com.tangosol.net.InvocationObserver
        public void memberLeft(com.tangosol.net.Member member)
            {
            }
        
        // Accessor for the property "Continuation"
        /**
         * Setter for property Continuation.<p>
         */
        public void setContinuation(com.oracle.coherence.common.base.Continuation continuation)
            {
            __m_Continuation = continuation;
            }
        }
    }
