
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.SafeService

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.net.Security;
import com.tangosol.coherence.component.net.extend.RemoteService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
import com.oracle.coherence.common.base.Timeout;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.ProxyService;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.Service;
import com.tangosol.net.ServiceDependencies;
import com.tangosol.net.management.Registry;
import com.tangosol.net.security.DoAsAction;
import com.tangosol.net.security.LocalPermission;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;
import com.tangosol.util.Listeners;
import com.tangosol.util.ServiceEvent;
import com.tangosol.util.SimpleResourceRegistry;
import com.tangosol.util.SynchronousListener;
import java.security.AccessController;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.IntPredicate;

/*
* Integrates
*     com.tangosol.net.Service
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SafeService
        extends    com.tangosol.coherence.component.Util
        implements com.oracle.coherence.common.base.Lockable,
                   com.tangosol.net.MemberListener,
                   com.tangosol.net.Service,
                   com.tangosol.util.ServiceListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property Config
     *
     * The configuration data
     */
    private transient com.tangosol.run.xml.XmlElement __m_Config;
    
    /**
     * Property ContextClassLoader
     *
     * The context ClassLoader for this service.
     */
    private ClassLoader __m_ContextClassLoader;
    
    /**
     * Property Dependencies
     *
     * The dependencies for the wrapped Service.
     */
    private com.tangosol.net.ServiceDependencies __m_Dependencies;
    
    /**
     * Property EnsureServiceAction
     *
     * PrivilegedAction to call ensureRunningService.
     */
    private java.security.PrivilegedAction __m_EnsureServiceAction;
    
    /**
     * Property InternalService
     *
     * The actual (wrapped) Service.
     */
    private transient com.tangosol.net.Service __m_InternalService;
    
    /**
     * Property Lock
     *
     * Lock used to protect this SafeService instance against multi-threaded
     * usage.
     */
    private java.util.concurrent.locks.ReentrantLock __m_Lock;
    
    /**
     * Property MemberListeners
     *
     */
    private com.tangosol.util.Listeners __m_MemberListeners;
    
    /**
     * Property ResourceRegistry
     *
     * ResourceRegistry associated with this SafeService.
     */
    private com.tangosol.util.ResourceRegistry __m_ResourceRegistry;
    
    /**
     * Property Restarting
     *
     * This property is set to true only during the service restart.
     * 
     * @see #restartService
     */
    private transient boolean __m_Restarting;
    
    /**
     * Property SafeCluster
     *
     * The SafeCluster this SafeService belongs to.
     * 
     * @see SafeCluster#getSafeService
     */
    private transient SafeCluster __m_SafeCluster;
    
    /**
     * Property SafeServiceState
     *
     * The state of the SafeService; one of the SERVICE_ enums.
     */
    private int __m_SafeServiceState;
    
    /**
     * Property SERVICE_INITIAL
     *
     * The SafeService has been created but has not been started yet.
     */
    public static final int SERVICE_INITIAL = 0;
    
    /**
     * Property SERVICE_STARTED
     *
     * The SafeService has been started.
     */
    public static final int SERVICE_STARTED = 1;
    
    /**
     * Property SERVICE_STOPPED
     *
     * The SafeService has beed explicitely stopped
     */
    public static final int SERVICE_STOPPED = 2;
    
    /**
     * Property ServiceListeners
     *
     */
    private com.tangosol.util.Listeners __m_ServiceListeners;
    
    /**
     * Property ServiceName
     *
     * Service name
     * @see SafeCluster#ensureService
     */
    private String __m_ServiceName;
    
    /**
     * Property ServiceType
     *
     * Service type
     * @see SafeCluster#ensureService
     */
    private String __m_ServiceType;
    
    /**
     * Property Subject
     *
     * The optional Subject associated with the service.
     */
    private javax.security.auth.Subject __m_Subject;
    
    /**
     * Property Unlockable
     *
     * AutoCloseable to release aquired lock via exclusively().
     */
    private SafeService.Unlockable __m_Unlockable;
    
    /**
     * Property UserContext
     *
     * User context object associated with this Service.
     */
    private Object __m_UserContext;
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
        __mapChildren.put("EnsureServiceAction", SafeService.EnsureServiceAction.get_CLASS());
        __mapChildren.put("StartAction", SafeService.StartAction.get_CLASS());
        __mapChildren.put("Unlockable", SafeService.Unlockable.get_CLASS());
        }
    
    // Default constructor
    public SafeService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SafeService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setLock(new java.util.concurrent.locks.ReentrantLock());
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setSafeServiceState(0);
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
        
        // state initialization: private properties
        try
            {
            __m_MemberListeners = new com.tangosol.util.Listeners();
            __m_ServiceListeners = new com.tangosol.util.Listeners();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.SafeService();
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
            clz = Class.forName("com.tangosol.coherence/component/util/SafeService".replace('/', '.'));
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
    
    //++ com.tangosol.net.Service integration
    // Access optimization
    // properties integration
    // methods integration
    public com.tangosol.net.ServiceInfo getInfo()
        {
        return ((com.tangosol.net.Service) getRunningService()).getInfo();
        }
    public com.tangosol.io.Serializer getSerializer()
        {
        return ((com.tangosol.net.Service) getRunningService()).getSerializer();
        }
    //-- com.tangosol.net.Service integration
    
    // From interface: com.tangosol.net.Service
    public void addMemberListener(com.tangosol.net.MemberListener l)
        {
        // import com.tangosol.net.Service;
        // import com.tangosol.util.Listeners;
        // import com.tangosol.util.SynchronousListener;
        
        ensureLocked();
        try
            {
            Service service = getInternalService();
        
            if (l instanceof SynchronousListener)
                {
                if (service != null && service.isRunning())
                    {
                    service.addMemberListener(l);
                    }
                }
            else
                {
                Listeners listeners = getMemberListeners();
                boolean   fWasEmpty = listeners.isEmpty();
        
                listeners.add(l);
        
                if (fWasEmpty && !listeners.isEmpty())
                    {
                    if (service != null && service.isRunning())
                        {
                        service.addMemberListener(this);
                        }
                    }
                }
            }
        finally
            {
            unlock();
            }
        }
    
    // From interface: com.tangosol.net.Service
    public void addServiceListener(com.tangosol.util.ServiceListener l)
        {
        // import com.tangosol.net.Service;
        // import com.tangosol.util.Listeners;
        // import com.tangosol.util.SynchronousListener;
        
        ensureLocked();
        try
            {
            Service service = getInternalService();
        
            if (l instanceof SynchronousListener)
                {
                if (service != null && service.isRunning())
                    {
                    service.addServiceListener(l);
                    }
                }
            else
                {
                Listeners listeners = getServiceListeners();
                boolean   fWasEmpty = listeners.isEmpty();    
        
                listeners.add(l);
        
                if (fWasEmpty && !listeners.isEmpty())
                    {
                    if (service != null && service.isRunning())
                        {
                        service.addServiceListener(this);
                        }
                    }
                }
            }
        finally
            {
            unlock();
            }
        }
    
    /**
     * Check if the current thread is either the service thread or one of its
    * daemons.  If true, issue a warning that this could lead to deadlock.
    * 
    * This is used to issue a warning for the following type of execution:
    * 
    * 1. Client thread calls a method M which is normally intended for client
    * threads. Method M obtains a syncronization monitor S, issues a request to
    * a service, and blocks waiting for a response.
    * 2. The service thread or a service worker thread handles the request,
    * calls an external module, which in turn calls a method M (or any other
    * method that may obtain monitor S).
    * 3. Deadlock may occur.
    * 
    * In this case the method M should include a call to this method.
    * 
    * @since Coherence 3.1 
     */
    protected void checkClientThread(String sMethod)
        {
        if (isServiceThread())
            {
            _trace("Application code running on \"" + getServiceName()
                 + "\" service thread(s) should not call " + sMethod 
                 + " as this may result in deadlock. The most common case is"
                 + " a CacheFactory call from a custom CacheStore implementation.", 2);
            if (_isTraceEnabled(7))
                {
                _trace("Stack trace:\n" + get_StackTrace(), 7);
                }     
            }
        }
    
    private void checkInternalAccess()
        {
        // import com.tangosol.net.security.LocalPermission;
        
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            security.checkPermission(LocalPermission.INTERNAL_SERVICE);
            }
        }
    
    protected void cleanup()
        {
        // import com.tangosol.util.SimpleResourceRegistry;
        
        setInternalService(null);
        setContextClassLoader(null);
        getMemberListeners().removeAll();
        getServiceListeners().removeAll();
        getResourceRegistry().dispose();
        // since this SafeService instance is reused on restart
        // set the ResourceRegistry to the default value
        setResourceRegistry(new SimpleResourceRegistry());
        }
    
    // From interface: com.tangosol.net.Service
    public void configure(com.tangosol.run.xml.XmlElement xmlConfig)
        {
        setConfig(xmlConfig);
        }
    
    /**
     * Ensure the caller acquires both SafeCluster and SafeService lock, or an
    * excpetion is thrown.
     */
    public void ensureGlobalLock()
        {
        // import com.tangosol.net.ServiceDependencies;
        
        SafeCluster cluster = getSafeCluster();
        
        ServiceDependencies deps     = getDependencies();
        long                cTimeout = deps == null
                                         ? -1l
                                         : deps.getRequestTimeoutMillis();
        
        cluster.ensureLocked(cTimeout == 0 ? -1L : cTimeout);
        try
            {
            ensureLocked();
            }
        catch (RuntimeException e)
            {
            cluster.unlock();
            throw e;
            }
        }
    
    /**
     * Ensure the caller acquires the SafeService lock, or an excpetion is
    * thrown.
     */
    public void ensureLocked()
        {
        // import com.oracle.coherence.common.base.Timeout;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.ServiceDependencies;
        // import com.tangosol.util.Base;
        // import java.util.concurrent.locks.Lock;
        // import java.util.concurrent.TimeUnit;
        
        ServiceDependencies deps = getDependencies();
        long cRequestTimeout = deps == null ? 0l : deps.getRequestTimeoutMillis();
        long cTimeout        = Timeout.isSet()
                ? Timeout.remainingTimeoutMillis() 
                : cRequestTimeout;
        
        Lock lock = getLock();
        try
            {
            if (lock.tryLock(cTimeout <= 0 ? Long.MAX_VALUE : cTimeout, TimeUnit.MILLISECONDS))
                {
                return;
                }
        
            throw Base.ensureRuntimeException(
                    new RequestTimeoutException("Failed to acquire service lock in " + cTimeout + "ms"));
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw Base.ensureRuntimeException(e, "Interrupted while attempting to acquire service lock"); 
            }
        }
    
    /**
     * Return the wrapped Service. This method ensures that the returned Service
    * is running before returning it. If the Service is not running and has not
    * been explicitly stopped,  the Service is restarted.
     */
    public com.tangosol.net.Service ensureRunningService()
        {
        // import Component.Net.Security;
        // import com.tangosol.net.Cluster;
        // import com.tangosol.net.InvocationService;
        // import com.tangosol.net.ProxyService;
        // import com.tangosol.net.Service;
        
        checkInternalAccess();
        
        Service service = getInternalService();
        if (service == null || !service.isRunning())
            {
            boolean fRegister = true;
        
            // to prevent a deadlock during restart we need to obtain the cluster lock
            // before restarting the service (see problem COH-77)
            Cluster cluster = getSafeCluster();
        
            ensureGlobalLock();
            try
                {
                String sName      = getServiceName();
                String sType      = getServiceType();
                String sCacheName = (InvocationService.TYPE_DEFAULT.equals(sType)
                    || ProxyService.TYPE_DEFAULT.equals(sType)) ? null : "*";
        
                service = getInternalService();
                switch (getSafeServiceState())
                    {
                    case SERVICE_INITIAL:
                        Security.checkPermission(cluster, sName, sCacheName, "join");
                        if (service == null)
                            {
                            setInternalService(service = restartService());
                            }
                        else
                            {
                            startService(service);
                            }
                        break;
        
                    case SERVICE_STARTED:
                        Security.checkPermission(cluster, sName, sCacheName, "join");
                        if (service == null || !service.isRunning())
                            {
                            setInternalService(service = null); // release memory before restarting
        
                            // restart the actual service
                            _trace("Restarting Service: " + getServiceName(), 3);
        
                            setInternalService(service = restartService());
                            }
                        else
                            {
                            // has been just registered by another thread
                            fRegister = false;
                            }
                        break;
        
                    case SERVICE_STOPPED:
                        throw new IllegalStateException("SafeService was explicitly stopped");
                    }
                }
            finally
                {
                unlockGlobal();
                }
        
            if (fRegister)
                {
                register();
                }
            }
        
        return service;
        }
    
    // From interface: com.oracle.coherence.common.base.Lockable
    public com.oracle.coherence.common.base.Lockable.Unlockable exclusively()
        {
        ensureLocked();
        
        return getUnlockable();
        }
    
    // From interface: com.tangosol.net.Service
    public com.tangosol.net.Cluster getCluster()
        {
        return getSafeCluster();
        }
    
    // Accessor for the property "Config"
    /**
     * Getter for property Config.<p>
    * The configuration data
     */
    public com.tangosol.run.xml.XmlElement getConfig()
        {
        return __m_Config;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "ContextClassLoader"
    /**
     * Getter for property ContextClassLoader.<p>
    * The context ClassLoader for this service.
     */
    public ClassLoader getContextClassLoader()
        {
        // import com.tangosol.util.Base;
        
        ClassLoader loader = __m_ContextClassLoader;
        if (loader == null)
            {
            loader = Base.getContextClassLoader(this);
            }
        return loader;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "Dependencies"
    /**
     * Getter for property Dependencies.<p>
    * The dependencies for the wrapped Service.
     */
    public com.tangosol.net.ServiceDependencies getDependencies()
        {
        return __m_Dependencies;
        }
    
    // Accessor for the property "EnsureServiceAction"
    /**
     * Getter for property EnsureServiceAction.<p>
    * PrivilegedAction to call ensureRunningService.
     */
    public java.security.PrivilegedAction getEnsureServiceAction()
        {
        return __m_EnsureServiceAction;
        }
    
    // Accessor for the property "InternalService"
    /**
     * Getter for property InternalService.<p>
    * The actual (wrapped) Service.
     */
    protected com.tangosol.net.Service getInternalService()
        {
        return __m_InternalService;
        }
    
    // Accessor for the property "Lock"
    /**
     * Getter for property Lock.<p>
    * Lock used to protect this SafeService instance against multi-threaded
    * usage.
     */
    public java.util.concurrent.locks.ReentrantLock getLock()
        {
        return __m_Lock;
        }
    
    // Accessor for the property "MemberListeners"
    /**
     * Getter for property MemberListeners.<p>
     */
    protected com.tangosol.util.Listeners getMemberListeners()
        {
        return __m_MemberListeners;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "ResourceRegistry"
    /**
     * Getter for property ResourceRegistry.<p>
    * ResourceRegistry associated with this SafeService.
     */
    public com.tangosol.util.ResourceRegistry getResourceRegistry()
        {
        return __m_ResourceRegistry;
        }
    
    // Accessor for the property "RunningService"
    /**
     * Getter for property RunningService.<p>
    * Calculated property returning a running service.
     */
    public com.tangosol.util.Service getRunningService()
        {
        // import com.tangosol.net.Service;
        // import com.tangosol.net.security.DoAsAction;
        // import java.security.AccessController;
        
        if (System.getSecurityManager() == null)
            {
            return ensureRunningService();
            }
        
        return (Service) AccessController.doPrivileged(
            new DoAsAction(getEnsureServiceAction()));
        }
    
    // Accessor for the property "SafeCluster"
    /**
     * Getter for property SafeCluster.<p>
    * The SafeCluster this SafeService belongs to.
    * 
    * @see SafeCluster#getSafeService
     */
    public SafeCluster getSafeCluster()
        {
        return __m_SafeCluster;
        }
    
    // Accessor for the property "SafeServiceState"
    /**
     * Getter for property SafeServiceState.<p>
    * The state of the SafeService; one of the SERVICE_ enums.
     */
    public int getSafeServiceState()
        {
        return __m_SafeServiceState;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * Public access to the InternalService that requires the corresponding
    * LocalPermission.
     */
    public com.tangosol.net.Service getService()
        {
        checkInternalAccess();
        
        return getInternalService();
        }
    
    // Accessor for the property "ServiceListeners"
    /**
     * Getter for property ServiceListeners.<p>
     */
    protected com.tangosol.util.Listeners getServiceListeners()
        {
        return __m_ServiceListeners;
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Getter for property ServiceName.<p>
    * Service name
    * @see SafeCluster#ensureService
     */
    public String getServiceName()
        {
        return __m_ServiceName;
        }
    
    // Accessor for the property "ServiceType"
    /**
     * Getter for property ServiceType.<p>
    * Service type
    * @see SafeCluster#ensureService
     */
    public String getServiceType()
        {
        return __m_ServiceType;
        }
    
    // Accessor for the property "Subject"
    /**
     * Getter for property Subject.<p>
    * The optional Subject associated with the service.
     */
    public javax.security.auth.Subject getSubject()
        {
        return __m_Subject;
        }
    
    // Accessor for the property "Unlockable"
    /**
     * Getter for property Unlockable.<p>
    * AutoCloseable to release aquired lock via exclusively().
     */
    public SafeService.Unlockable getUnlockable()
        {
        return __m_Unlockable;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "UserContext"
    /**
     * Getter for property UserContext.<p>
    * User context object associated with this Service.
     */
    public Object getUserContext()
        {
        return __m_UserContext;
        }
    
    // Accessor for the property "LocalService"
    /**
     * Getter for property LocalService.<p>
    * This property is true if the underlying service is a local
    * (non-clustered) service.
     */
    public boolean isLocalService()
        {
        return getSafeCluster().isLocalService(getServiceType());
        }
    
    // Accessor for the property "Restarting"
    /**
     * Getter for property Restarting.<p>
    * This property is set to true only during the service restart.
    * 
    * @see #restartService
     */
    public boolean isRestarting()
        {
        return __m_Restarting;
        }
    
    // From interface: com.tangosol.net.Service
    public boolean isRunning()
        {
        // import com.tangosol.net.Service;
        
        Service service = getInternalService();
        return service != null && service.isRunning() &&
            (isLocalService() || service.getCluster().isRunning());
        }
    
    // Accessor for the property "ServiceThread"
    /**
     * Getter for property ServiceThread.<p>
    * Return true if the current thread is one of the Service threads.
     */
    public boolean isServiceThread()
        {
        // import Component.Net.Extend.RemoteService;
        // import Component.Util.Daemon.QueueProcessor.Service as com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
        // import com.tangosol.net.Service as com.tangosol.net.Service;
        
        com.tangosol.net.Service _service = getInternalService();
        
        if (_service instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.Service)
            {
            return ((com.tangosol.coherence.component.util.daemon.queueProcessor.Service) _service).isServiceThread(false);
            }
        else if (_service instanceof RemoteService)
            {
            return ((RemoteService) _service).isServiceThread(false);
            }
        return false;
        }
    
    // From interface: com.tangosol.net.Service
    public boolean isSuspended()
        {
        // import com.tangosol.net.Service;
        
        if (isLocalService())
            {
            return false;
            }
        
        Service service = getInternalService();
        return service != null && service.isSuspended();
        }
    
    // From interface: com.tangosol.net.MemberListener
    public void memberJoined(com.tangosol.net.MemberEvent evt)
        {
        translateEvent(evt);
        }
    
    // From interface: com.tangosol.net.MemberListener
    public void memberLeaving(com.tangosol.net.MemberEvent evt)
        {
        translateEvent(evt);
        }
    
    // From interface: com.tangosol.net.MemberListener
    public void memberLeft(com.tangosol.net.MemberEvent evt)
        {
        translateEvent(evt);
        }
    
    @Override
    public void memberRecovered(com.tangosol.net.MemberEvent evt)
        {
        translateEvent(evt);
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
        setEnsureServiceAction((SafeService.EnsureServiceAction) _newChild("EnsureServiceAction"));
        setUnlockable((SafeService.Unlockable) _newChild("Unlockable"));
        
        super.onInit();
        }
    
    protected void register()
        {
        // import Component.Net.Extend.RemoteService;
        // import com.tangosol.net.Member;
        // import com.tangosol.net.management.Registry;
        
        Registry registry = getSafeCluster().getManagement();
        if (registry != null && !getServiceType().equals("Cluster")
            && !(getInternalService() instanceof RemoteService))
            {
            Member member = getSafeCluster().getLocalMember();
            if (member != null)
                {
                String sName = Registry.SERVICE_TYPE + ",name=" + getServiceName();
                sName = registry.ensureGlobalName(sName);
                registry.register(sName, this);
                }
            }
        }
    
    // From interface: com.tangosol.net.Service
    public void removeMemberListener(com.tangosol.net.MemberListener l)
        {
        // import com.tangosol.net.Service;
        // import com.tangosol.util.Listeners;
        // import com.tangosol.util.SynchronousListener;
        
        ensureLocked();
        try
            {
            Service service = getInternalService();
        
            if (l instanceof SynchronousListener)
                {
                if (service != null && service.isRunning())
                    {
                    service.removeMemberListener(l);
                    }
                }
            else
                {
                Listeners listeners = getMemberListeners();
                if (!listeners.isEmpty())
                    {
                    listeners.remove(l);
                    if (listeners.isEmpty())
                        {
                        if (service != null && service.isRunning())
                            {
                            service.removeMemberListener(this);
                            }
                        }
                    }
                }
            }
        finally
            {
            unlock();
            }
        }
    
    // From interface: com.tangosol.net.Service
    public void removeServiceListener(com.tangosol.util.ServiceListener l)
        {
        // import com.tangosol.net.Service;
        // import com.tangosol.util.Listeners;
        // import com.tangosol.util.SynchronousListener;
        
        ensureLocked();
        try
            {
            Service service = getInternalService();
        
            if (l instanceof SynchronousListener)
                {
                if (service != null && service.isRunning())
                    {
                    service.removeServiceListener(l);
                    }
                }
            else
                {
                Listeners listeners = getServiceListeners();
                if (!listeners.isEmpty())
                    {
                    listeners.remove(l);
                    if (listeners.isEmpty())
                        {
                        if (service != null && service.isRunning())
                            {
                            service.removeServiceListener(this);
                            }
                        }
                    }
                }
            }
        finally
            {
            unlock();
            }
        }
    
    protected com.tangosol.net.Service restartService()
        {
        // import com.tangosol.net.Service;
        
        setRestarting(true);
        try
            {
            Service service = getSafeCluster().instantiateService(getServiceName(), getServiceType());
        
            startService(service);
            return service;
            }
        finally
            {
            setRestarting(false);
            }
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStarted(com.tangosol.util.ServiceEvent evt)
        {
        translateEvent(evt);
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStarting(com.tangosol.util.ServiceEvent evt)
        {
        translateEvent(evt);
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStopped(com.tangosol.util.ServiceEvent evt)
        {
        translateEvent(evt);
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStopping(com.tangosol.util.ServiceEvent evt)
        {
        translateEvent(evt);
        }
    
    // Accessor for the property "Config"
    /**
     * Setter for property Config.<p>
    * The configuration data
     */
    public void setConfig(com.tangosol.run.xml.XmlElement xmlConfig)
        {
        __m_Config = xmlConfig;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "ContextClassLoader"
    /**
     * Setter for property ContextClassLoader.<p>
    * The context ClassLoader for this service.
     */
    public void setContextClassLoader(ClassLoader loader)
        {
        // import com.tangosol.net.security.LocalPermission;
        // import com.tangosol.net.Service;
        
        SecurityManager security = System.getSecurityManager();
        if (security != null && loader != null)
            {
            security.checkPermission(
                new LocalPermission("BackingMapManagerContext.setClassLoader"));
            }
        
        __m_ContextClassLoader = (loader);
        
        Service service = getInternalService();
        if (service != null)
            {
            service.setContextClassLoader(loader);
            }
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "Dependencies"
    /**
     * Setter for property Dependencies.<p>
    * The dependencies for the wrapped Service.
     */
    public void setDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        __m_Dependencies = deps;
        }
    
    // Accessor for the property "EnsureServiceAction"
    /**
     * Setter for property EnsureServiceAction.<p>
    * PrivilegedAction to call ensureRunningService.
     */
    protected void setEnsureServiceAction(java.security.PrivilegedAction action)
        {
        __m_EnsureServiceAction = action;
        }
    
    // Accessor for the property "InternalService"
    /**
     * Setter for property InternalService.<p>
    * The actual (wrapped) Service.
     */
    public void setInternalService(com.tangosol.net.Service service)
        {
        __m_InternalService = service;
        }
    
    // Accessor for the property "Lock"
    /**
     * Setter for property Lock.<p>
    * Lock used to protect this SafeService instance against multi-threaded
    * usage.
     */
    public void setLock(java.util.concurrent.locks.ReentrantLock lock)
        {
        __m_Lock = lock;
        }
    
    // Accessor for the property "MemberListeners"
    /**
     * Setter for property MemberListeners.<p>
     */
    private void setMemberListeners(com.tangosol.util.Listeners listeners)
        {
        __m_MemberListeners = listeners;
        }
    
    // Accessor for the property "ResourceRegistry"
    /**
     * Setter for property ResourceRegistry.<p>
    * ResourceRegistry associated with this SafeService.
     */
    protected void setResourceRegistry(com.tangosol.util.ResourceRegistry registry)
        {
        __m_ResourceRegistry = registry;
        }
    
    // Accessor for the property "Restarting"
    /**
     * Setter for property Restarting.<p>
    * This property is set to true only during the service restart.
    * 
    * @see #restartService
     */
    protected void setRestarting(boolean fRestarting)
        {
        __m_Restarting = fRestarting;
        }
    
    // Accessor for the property "SafeCluster"
    /**
     * Setter for property SafeCluster.<p>
    * The SafeCluster this SafeService belongs to.
    * 
    * @see SafeCluster#getSafeService
     */
    public void setSafeCluster(SafeCluster cluster)
        {
        __m_SafeCluster = cluster;
        }
    
    // Accessor for the property "SafeServiceState"
    /**
     * Setter for property SafeServiceState.<p>
    * The state of the SafeService; one of the SERVICE_ enums.
     */
    protected void setSafeServiceState(int nState)
        {
        __m_SafeServiceState = nState;
        }
    
    // Accessor for the property "ServiceListeners"
    /**
     * Setter for property ServiceListeners.<p>
     */
    private void setServiceListeners(com.tangosol.util.Listeners listeners)
        {
        __m_ServiceListeners = listeners;
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Setter for property ServiceName.<p>
    * Service name
    * @see SafeCluster#ensureService
     */
    public void setServiceName(String sName)
        {
        __m_ServiceName = sName;
        }
    
    // Accessor for the property "ServiceType"
    /**
     * Setter for property ServiceType.<p>
    * Service type
    * @see SafeCluster#ensureService
     */
    public void setServiceType(String sType)
        {
        __m_ServiceType = sType;
        }
    
    // Accessor for the property "Subject"
    /**
     * Setter for property Subject.<p>
    * The optional Subject associated with the service.
     */
    public void setSubject(javax.security.auth.Subject subject)
        {
        __m_Subject = subject;
        }
    
    // Accessor for the property "Unlockable"
    /**
     * Setter for property Unlockable.<p>
    * AutoCloseable to release aquired lock via exclusively().
     */
    public void setUnlockable(SafeService.Unlockable unlockable)
        {
        __m_Unlockable = unlockable;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "UserContext"
    /**
     * Setter for property UserContext.<p>
    * User context object associated with this Service.
     */
    public void setUserContext(Object oCtx)
        {
        // import com.tangosol.net.Service;
        
        __m_UserContext = (oCtx);
        
        Service service = (Service) getInternalService();
        if (service != null)
            {
            service.setUserContext(oCtx);
            }
        }
    
    // From interface: com.tangosol.net.Service
    public void shutdown()
        {
        // import com.tangosol.net.Service;
        
        ensureGlobalLock();
        try
            {
            if (getSafeServiceState() != SERVICE_STOPPED)
                {
                Service service = getInternalService();
                if (service != null)
                    {
                    service.shutdown();
                    }
                unregister();
                cleanup();
                setSafeServiceState(SERVICE_STOPPED);
                }
            }
        finally
            {
            unlockGlobal();
            }
        }
    
    // From interface: com.tangosol.net.Service
    public void start()
        {
        // import com.tangosol.net.security.DoAsAction;
        // import java.security.AccessController;
        
        AccessController.doPrivileged(
            new DoAsAction((SafeService.StartAction) _newChild("StartAction")));
        }
    
    /**
     * Declared as public only to be accessed by the action.
     */
    public void startInternal()
        {
        ensureGlobalLock();
        try
            {
            if (getSafeServiceState() == SERVICE_STOPPED)
                {
                // allow restart after explicit stop
                setSafeServiceState(SERVICE_INITIAL);
                }
        
            ensureRunningService();
            }
        finally
            {
            // SERVICE_STARTED indicates that "start" was called
            setSafeServiceState(SERVICE_STARTED);
            unlockGlobal();
            }
        }
    
    protected void startService(com.tangosol.net.Service service)
        {
        // import Component.Net.Extend.RemoteService;
        // import Component.Util.Daemon.QueueProcessor.Service as com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Listeners;
        // import com.tangosol.run.xml.XmlElement;
        
        if (service instanceof RemoteService)
            {
            ((RemoteService) service).setOperationalContext(getSafeCluster());
            }
        else if (service instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.Service)
            {
            ((com.tangosol.coherence.component.util.daemon.queueProcessor.Service) service).setOperationalContext(getSafeCluster());
            }
        
        // the resource registry is managed by the safe tier, which allows the lifecycle
        // of the registry to span service restarts, and accessed by the internal
        // service; therefore propagate the SafeService's registry to the internal service
        if (service instanceof Grid)
            {
            ((Grid) service).setResourceRegistry(getResourceRegistry());
            }
        
        service.setContextClassLoader(getContextClassLoader());
        
        try
            {
            // TODO: Remove when configure(xmlConfig) is no longer required
            XmlElement xmlConfig = getConfig();
            if (xmlConfig == null) 
                {
                service.setDependencies(getDependencies());
                }
            else
                {
                service.configure(xmlConfig);
                }
            }
        catch (Throwable e)
            {
            _trace("Error while configuring service \"" + getServiceName() + "\": " +
                    getStackTrace(e), 1);
            if (e instanceof Error)
                {
                throw (Error) e;
                }
            throw (RuntimeException) e;
            }
        
        service.setUserContext(getUserContext());
        Listeners listenersMember = getMemberListeners();
        if (!listenersMember.isEmpty())
            {
            service.addMemberListener(this);
            }
        
        Listeners listenersService = getServiceListeners();
        if (!listenersService.isEmpty())
            {
            service.addServiceListener(this);
            }
        
        try
            {
            service.start();
            }
        catch (Throwable e)
            {
            _trace("Error while starting service \"" + getServiceName() + "\": " +
                    getStackTrace(e), 1);
            try
                {
                service.stop();
                }
            catch (Throwable e2)
                {
                _trace("Failed to stop service \"" + getServiceName() + "\": " +
                        getStackTrace(e2), 2);
                // eat the exception
                }
        
            if (e instanceof Error)
                {
                throw (Error) e;
                }
            throw (RuntimeException) e;
            }
        }
    
    // From interface: com.tangosol.net.Service
    public void stop()
        {
        // import com.tangosol.net.Service;
        
        ensureGlobalLock();
        try
            {
            if (getSafeServiceState() != SERVICE_STOPPED)
                {
                Service service = getInternalService();
                if (service != null)
                    {
                    service.stop();
                    }
                cleanup();
                setSafeServiceState(SERVICE_STOPPED);
                }
            }
        finally
            {
            unlockGlobal();
            }
        }
    
    // Declared at the super level
    public String toString()
        {
        // import com.tangosol.net.Service;
        
        Service service = getInternalService();
        return get_Name() + ": " +
            (service == null ? "STOPPED" : service.toString());
        }
    
    protected void translateEvent(com.tangosol.net.MemberEvent evtMember)
        {
        // import com.tangosol.net.MemberEvent;
        // import com.tangosol.net.Service;
        // import com.tangosol.util.Listeners;
        
        Service service = (Service) getInternalService();
        
        if (service == null)
            {
            // for the JOIN events, the Service property may not be set until after
            // the "real" service is started
            // (see lock acquisition at the ensureRunningService() method)
            // just wait till we are out of there
        
            ensureLocked();
                {
                service = (Service) getInternalService();
                }
        
            unlock();
            }
        
        // allow for post-mortem events
        Listeners listeners = getMemberListeners();
        if (!listeners.isEmpty())
            {
            Service serviceSource = evtMember.getService();
        
            MemberEvent evtSafe = new MemberEvent(
                service == serviceSource ? this : serviceSource,
                evtMember.getId(), evtMember.getMember());
            evtSafe.dispatch(listeners);
            }
        }
    
    protected void translateEvent(com.tangosol.util.ServiceEvent evtService)
        {
        // import com.tangosol.net.Service;
        // import com.tangosol.util.ServiceEvent;
        // import com.tangosol.util.Listeners;
        
        Service service = getInternalService();
        if (service == null)
            {
            // for the JOIN events, the Service property may not be set until after
            // the "real" service is started
            // (see lock acquisition at the ensureRunningService() method)
            // just wait till we are out of there
        
            ensureLocked();
                {
                service = getInternalService();
                }
            unlock();
            }
        
        // allow for post-mortem events
        Listeners listeners = getServiceListeners();
        if (!listeners.isEmpty())
            {
            Service serviceSource = (Service) evtService.getService();
        
            ServiceEvent evtSafe = new ServiceEvent(
                service == serviceSource ? this : serviceSource,
                evtService.getId());
            evtSafe.dispatch(listeners);
            }
        }
    
    public void unlock()
        {
        getLock().unlock();
        }
    
    /**
     * Unlock both SafeService and SafeCluster locks.
     */
    public void unlockGlobal()
        {
        getLock().unlock();
        getSafeCluster().getLock().unlock();
        }
    
    protected void unregister()
        {
        // import Component.Net.Extend.RemoteService;
        // import com.tangosol.net.Member;
        // import com.tangosol.net.management.Registry;
        
        Registry registry = getSafeCluster().getManagement();
        if (registry != null && !getServiceType().equals("Cluster")
            && !(getInternalService() instanceof RemoteService))
            {
            Member member = getSafeCluster().getLocalMember();
            if (member != null)
                {
                String sName = Registry.SERVICE_TYPE + ",name=" + getServiceName();
                sName = registry.ensureGlobalName(sName);
                registry.unregister(sName);
                }
            }
        }

    @Override
    public boolean isVersionCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        return getService().isVersionCompatible(nMajor, nMinor, nMicro, nPatchSet, nPatch);
        }

    @Override
    public boolean isVersionCompatible(int nYear, int nMonth, int nPatch)
        {
        return getService().isVersionCompatible(nYear, nMonth, nPatch);
        }

    @Override
    public boolean isVersionCompatible(int nVersion)
        {
        return getService().isVersionCompatible(nVersion);
        }

    @Override
    public boolean isVersionCompatible(IntPredicate predicate)
        {
        return getService().isVersionCompatible(predicate);
        }

    @Override
    public int getMinimumServiceVersion()
        {
        return getService().getMinimumServiceVersion();
        }

    // ---- class: com.tangosol.coherence.component.util.SafeService$EnsureServiceAction
    
    /**
     * PrivilegedAction used to start services in case they are scoped by
     * Subject
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EnsureServiceAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public EnsureServiceAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public EnsureServiceAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.SafeService.EnsureServiceAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeService$EnsureServiceAction".replace('/', '.'));
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
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            return ((SafeService) get_Module()).ensureRunningService();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.SafeService$StartAction
    
    /**
     * Privileged action to start a service.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class StartAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public StartAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public StartAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.SafeService.StartAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeService$StartAction".replace('/', '.'));
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
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            ((SafeService) get_Module()).startInternal();
            
            return null;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.SafeService$Unlockable
    
    /**
     * An Autocloseable that is responsible for releasing the acquired lock.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Unlockable
            extends    com.tangosol.coherence.component.Util
            implements com.oracle.coherence.common.base.Lockable.Unlockable
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public Unlockable()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Unlockable(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.SafeService.Unlockable();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeService$Unlockable".replace('/', '.'));
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
        
        // From interface: com.oracle.coherence.common.base.Lockable$Unlockable
        public void close()
            {
            ((SafeService) get_Module()).unlock();
            }
        }
    }
