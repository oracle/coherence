
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.SafeCluster

package com.tangosol.coherence.component.util;

import com.oracle.coherence.common.base.Classes;
import com.tangosol.coherence.component.application.console.Coherence;
import com.tangosol.coherence.component.net.Cluster;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.Security;
import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;
import com.tangosol.coherence.component.net.extend.remoteService.RemoteInvocationService;
import com.tangosol.coherence.component.net.management.Gateway;
import com.tangosol.coherence.component.util.safeService.SafeCacheService;
import com.tangosol.coherence.component.util.safeService.SafeInvocationService;
import com.tangosol.coherence.component.util.safeService.SafeProxyService;
import com.tangosol.coherence.component.util.safeService.safeCacheService.SafeDistributedCacheService;
import com.tangosol.coherence.component.util.safeService.safeCacheService.safeDistributedCacheService.SafePagedTopicService;
import com.oracle.coherence.common.base.Timeout;
import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.expression.SystemPropertyParameterResolver;
import com.tangosol.internal.net.cluster.DefaultClusterDependencies;
import com.tangosol.internal.net.cluster.LegacyXmlClusterDependencies;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ClusterDependencies;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.InvocationService;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.ProxyService;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.Service;
import com.tangosol.net.internal.ClusterJoinException;
import com.tangosol.net.management.Registry;
import com.tangosol.net.security.DoAsAction;
import com.tangosol.net.security.LocalPermission;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.util.Base;
import com.tangosol.util.SafeHashSet;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Cluster wrapper that never dies, unless explicitely commanded.
 * SafeCluster has to be configured and started prior to the first use. 
 */
/*
* Integrates
*     com.tangosol.net.Cluster
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SafeCluster
        extends    com.tangosol.coherence.component.Util
        implements com.oracle.coherence.common.base.Lockable,
                   com.tangosol.net.Cluster,
                   com.tangosol.net.OperationalContext,
                   com.tangosol.util.ServiceListener,
                   com.tangosol.util.SynchronousListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property ContextClassLoader
     *
     * The context ClassLoader for this cluster.
     */
    private ClassLoader __m_ContextClassLoader;
    
    /**
     * Property Dependencies
     *
     * The dependencies for the wrapped Cluster.
     * @volatile
     */
    private volatile com.tangosol.net.ClusterDependencies __m_Dependencies;
    
    /**
     * Property Disposed
     *
     * Specifies whether or not the SafeCluster reference has been disposed.
     * Once this flag is set, even explicit restarts are no longer possible.
     */
    private boolean __m_Disposed;
    
    /**
     * Property EnsureClusterAction
     *
     * PrivilegedAction to call ensureRunningCluster.
     */
    private java.security.PrivilegedAction __m_EnsureClusterAction;
    
    /**
     * Property InternalCluster
     *
     * The actual (wrapped) cluster.
     */
    private transient com.tangosol.coherence.component.net.Cluster __m_InternalCluster;
    
    /**
     * Property LocalServices
     *
     * A set of local services.
     */
    private java.util.Set __m_LocalServices;
    
    /**
     * Property Lock
     *
     * Lock used to protect this SafeCluster instance against multi-threaded
     * usage.
     */
    private java.util.concurrent.locks.ReentrantLock __m_Lock;
    
    /**
     * Property Management
     *
     * The management gateway.
     */
    private transient com.tangosol.net.management.Registry __m_Management;
    
    /**
     * Property Restart
     *
     * Specifies whether or not the underlying cluster has to be automatically
     * restarted. Once this flag is turned to false only an explicit start()
     * call can restart it.
     * 
     * Note: this flag is also used to report the "Running" status since an
     * explicit shutdown always turns it off.
     */
    private boolean __m_Restart;
    
    /**
     * Property ScopedServiceStore
     *
     * The scoped store for Service references.
     */
    private com.tangosol.net.internal.ScopedServiceReferenceStore __m_ScopedServiceStore;
    
    /**
     * Property ServiceContext
     *
     * A map of Service related PermissionInfo objects keyed by the service
     * name.
     * 
     * Previously this property was maintained on the ClusterService, but it
     * was moved here as a result of Bug 27376204 which demonstrated that a
     * service restart could span two Cluster instances and if security was
     * enabled the PermissionInfo could be written into the first
     * ClusterService instance, node restart, and then read from the second,
     * resulting in a failure.
     */
    private transient java.util.Map __m_ServiceContext;
    
    /**
     * Property ShutdownHook
     *
     * A runnable component used to clean up local services. Since the
     * SafeCluster does not have an associated thread and the local service may
     * not be cleaned up unless the stop() method is explicitly called, the
     * ShutdownHook child holds only a WeakReference to the parent SafeCluster
     * component. This avoids the Java runtime holding a hard reference to the
     * SafeCluster and all objects it refers to preventing a potential "memory
     * leak".
     */
    private SafeCluster.ShutdownHook __m_ShutdownHook;
    
    /**
     * Property SurrogateMember
     *
     * A Member component used in lieu of the Cluster local member in the
     * "remote" or "local" scenario, when Cluster services are not running.
     */
    private com.tangosol.coherence.component.net.Member __m_SurrogateMember;
    
    /**
     * Property Unlockable
     *
     * AutoCloseable to release aquired lock via exclusively().
     */
    private SafeCluster.Unlockable __m_Unlockable;

    /**
     * The common {@link DaemonPool}.
     */
    private DaemonPool m_commonDaemon;

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
        __mapChildren.put("EnsureClusterAction", SafeCluster.EnsureClusterAction.get_CLASS());
        __mapChildren.put("EnsureSafeServiceAction", SafeCluster.EnsureSafeServiceAction.get_CLASS());
        __mapChildren.put("ParseDependenciesAction", SafeCluster.ParseDependenciesAction.get_CLASS());
        __mapChildren.put("ShutdownHook", SafeCluster.ShutdownHook.get_CLASS());
        __mapChildren.put("Unlockable", SafeCluster.Unlockable.get_CLASS());
        }
    
    // Default constructor
    public SafeCluster()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SafeCluster(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setRestart(true);
            setScopedServiceStore(new com.tangosol.net.internal.ScopedServiceReferenceStore());
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
            __m_ServiceContext = new com.tangosol.util.SafeHashMap();
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
        return new com.tangosol.coherence.component.util.SafeCluster();
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
            clz = Class.forName("com.tangosol.coherence/component/util/SafeCluster".replace('/', '.'));
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
    
    //++ com.tangosol.net.Cluster integration
    // Access optimization
    // properties integration
    // methods integration
    public String getClusterName()
        {
        return ((com.tangosol.net.Cluster) getRunningCluster()).getClusterName();
        }
    public java.util.Set getMemberSet()
        {
        return ((com.tangosol.net.Cluster) getRunningCluster()).getMemberSet();
        }
    /**
     * Getter for property OldestMember.<p>
     */
    public com.tangosol.net.Member getOldestMember()
        {
        return ((com.tangosol.net.Cluster) getRunningCluster()).getOldestMember();
        }
    public com.oracle.coherence.common.base.Disposable getResource(String sName)
        {
        return ((com.tangosol.net.Cluster) getRunningCluster()).getResource(sName);
        }
    public com.tangosol.util.ResourceRegistry getResourceRegistry()
        {
        return ((com.tangosol.net.Cluster) getRunningCluster()).getResourceRegistry();
        }
    public com.tangosol.net.ServiceInfo getServiceInfo(String sName)
        {
        return ((com.tangosol.net.Cluster) getRunningCluster()).getServiceInfo(sName);
        }
    public java.util.Enumeration getServiceNames()
        {
        return ((com.tangosol.net.Cluster) getRunningCluster()).getServiceNames();
        }
    public void registerResource(String sName, com.oracle.coherence.common.base.Disposable disposable)
        {
        ((com.tangosol.net.Cluster) getRunningCluster()).registerResource(sName, disposable);
        }
    public com.oracle.coherence.common.base.Disposable unregisterResource(String sName)
        {
        return ((com.tangosol.net.Cluster) getRunningCluster()).unregisterResource(sName);
        }
    //-- com.tangosol.net.Cluster integration
    
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
        setInternalCluster(null);
        setSurrogateMember(null);

        if (m_commonDaemon != null)
            {
            m_commonDaemon.shutdown();
            }
        
        getScopedServiceStore().clear();
        getLocalServices().clear();
        }
    
    // From interface: com.tangosol.net.Cluster
    public void configure(com.tangosol.run.xml.XmlElement xmlConfig)
        {
        // import com.tangosol.net.ClusterDependencies;
        // import java.security.AccessController;
        
        SafeCluster.ParseDependenciesAction action =
            (SafeCluster.ParseDependenciesAction) _newChild("ParseDependenciesAction");
        action.setXmlConfig(xmlConfig);
        
        setDependencies((ClusterDependencies) AccessController.doPrivileged(action));
        }
    
    /**
     * Shutdown and mark this SafeCluster as disposed . After this point,  a
    * start or restart of this SafeCluster is not allowed.
     */
    public void dispose()
        {
        ensureLocked();
        try
            {
            if (!isDisposed())
                {
                setDisposed(true);
        
                shutdown();
                }
            }
        finally
            {
            unlock();
            }
        }
    
    /**
     * Return the cluster dependencies, creating it if necessary
    * 
    * @return the cluster dependencies
     */
    public com.tangosol.net.ClusterDependencies ensureDependencies()
        {
        // import com.tangosol.net.ClusterDependencies;
        // import com.tangosol.net.CacheFactory;
        
        ClusterDependencies deps = getDependencies();
        if (deps == null)
            {
            configure(CacheFactory.getClusterConfig());
            deps = getDependencies();
            }
        
        return deps;
        }
    
    public com.tangosol.net.Service ensureLocalService(String sName, String sType)
        {
        // import Component.Net.Extend.RemoteService.RemoteCacheService;
        // import Component.Net.Extend.RemoteService.RemoteInvocationService;
        // import Component.Util.LocalCache;
        // import com.tangosol.coherence.config.Config;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.ClusterDependencies$ServiceProvider as com.tangosol.net.ClusterDependencies.ServiceProvider;
        // import com.tangosol.net.InvocationService;
        // import com.tangosol.net.Service;
        // import com.tangosol.util.Base;
        // import java.lang.ref.WeakReference;
        // import java.util.Set;
        // import java.util.function.BiFunction;
        
        Service serviceLocal;
        if (sType.equals(CacheService.TYPE_LOCAL))
            {
            LocalCache service = new LocalCache();
            service.setServiceName(sName);
            service.setCluster(this);
            serviceLocal = service;
            }
        else if (sType.equals(CacheService.TYPE_REMOTE))
            {
            RemoteCacheService service = new RemoteCacheService();
            service.setServiceName(sName);
            service.setCluster(this);
            serviceLocal = service;
            }
        else if (sType.equals(InvocationService.TYPE_REMOTE))
            {
            RemoteInvocationService service = new RemoteInvocationService();
            service.setServiceName(sName);
            service.setCluster(this);
            serviceLocal = service;
            }
        else
            {
            com.tangosol.net.ClusterDependencies.ServiceProvider provider = (com.tangosol.net.ClusterDependencies.ServiceProvider) getDependencies().getLocalServiceProvider(sType);
            serviceLocal = provider == null ? null : (Service) provider.createService(sName, this);
            if (serviceLocal == null)
                {
                throw new IllegalArgumentException("illegal local service type: " + sType);
                }
            }
        serviceLocal.addServiceListener(this);
        
        // undocumented property; may be necessary in some very esoteric cases
        String sHook = Config.getProperty("coherence.shutdownhook.local", "true");
        
        Set setLocal = getLocalServices();
        if (setLocal.isEmpty() && getShutdownHook() == null && "true".equals(sHook))
            {
            synchronized (setLocal)
                {
                if (setLocal.isEmpty() && getShutdownHook() == null)
                    {
                    try
                        {
                        SafeCluster.ShutdownHook hook = new SafeCluster.ShutdownHook();
                        hook.set_Feed(new WeakReference(this));
                        setShutdownHook(hook);
                        hook.register();
                        }
                    catch (Throwable e) {}
                    }
                }
            }
        
        setLocal.add(serviceLocal);
        
        return serviceLocal;
        }
    
    /**
     * Ensure that the caller acquires the SafeCluster lock, or an excpetion is
    * thrown
     */
    public void ensureLocked()
        {
        ensureLocked(-1l);
        }
    
    /**
     * Ensure that the caller acquires the SafeCluster lock, or an excpetion is
    * thrown
     */
    public void ensureLocked(long cRequestTimeout)
        {
        // import com.oracle.coherence.common.base.Timeout;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.util.Base;
        // import java.util.concurrent.locks.Lock;
        // import java.util.concurrent.TimeUnit;
        
        Lock lock     = getLock();
        long cTimeout = (Timeout.isSet() || cRequestTimeout == -1)
                ? Timeout.remainingTimeoutMillis()
                : cRequestTimeout;
        
        try
            {
            if (lock.tryLock(cTimeout, TimeUnit.MILLISECONDS))
                {
                return;
                }
        
            throw Base.ensureRuntimeException(
                    new RequestTimeoutException("Failed to acquire cluster lock in " + cTimeout + "ms"));
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw Base.ensureRuntimeException(e, "Interrupted while attempting to acquire cluster lock"); 
            }
        }
    
    /**
     * Declared as public only to be accessed by the action.
     */
    public com.tangosol.coherence.component.net.Cluster ensureRunningCluster()
        {
        // import Component.Net.Cluster;
        // import Component.Net.Management.Gateway;
        // import com.tangosol.net.management.Registry;
        
        checkInternalAccess();
        
        Cluster cluster = getInternalCluster();
        if (cluster == null || !cluster.isRunning())
            {
            ensureLocked();
            boolean fInit = true;
            try
                {
                cluster = getInternalCluster();
                if (cluster == null || !cluster.isRunning())
                    {
                    if (isRestart())
                        {
                        // create or restart the actual cluster
                        if (cluster != null)
                            {
                            if (cluster.isHalted())
                                {
                                throw new IllegalStateException("The cluster has been halted and is not restartable. This cluster member's JVM process must be restarted.");
                                }
        
                            cluster.ensureStopped();
                            setInternalCluster(cluster = null); // release memory
                            _trace("Restarting cluster", 3);
                            }
        
                        setInternalCluster(cluster = restartCluster());
                        }
                    else
                        {
                        throw new IllegalStateException(isDisposed()
                            ? "SafeCluster has been shutdown"
                            : "SafeCluster has been explicitly stopped or has not been started");
                        }
                    }
                else
                    {
                    // has been just registered by another thread
                    fInit = false;
                    }
                }
            finally
                {
                unlock();
                }
        
            if (fInit)
                {
                Gateway registry = (Gateway) getManagement();
                if (registry != null)
                    {
                    registry.reset();
        
                    String sNodeName = registry.ensureGlobalName(Registry.NODE_TYPE);
                    registry.register(sNodeName, getLocalMember());
        
                    String sP2PName  = registry.ensureGlobalName(Registry.POINT_TO_POINT_TYPE);
                    registry.register(sP2PName, cluster.getClusterService().getClusterMemberSet());
        
                    registry.registerReporter();
                    registry.registerCustomBeans();
                    }
                }
            }
        return cluster;
        }
    
    /**
     * Declared as public only to be accessed by the action.
     */
    public SafeService ensureSafeService(String sName, String sType)
        {
        // import Component.Util.SafeService;
        // import com.tangosol.net.Service;
        // import com.tangosol.net.internal.ScopedServiceReferenceStore as com.tangosol.net.internal.ScopedServiceReferenceStore;
        // import com.tangosol.net.security.SecurityHelper;
        
        com.tangosol.net.internal.ScopedServiceReferenceStore store       = getScopedServiceStore();
        SafeService  serviceSafe = (SafeService) store.getService(sName);
        if (serviceSafe == null)
            {
            ensureLocked();
            try
                {
                serviceSafe = (SafeService) store.getService(sName);
        
                if (serviceSafe == null)
                    {
                    serviceSafe = instantiateSafeService(instantiateService(sName, sType));
        
                    serviceSafe.setContextClassLoader(getContextClassLoader());
                    serviceSafe.setSafeCluster(this);
                    serviceSafe.setServiceName(sName);
                    serviceSafe.setServiceType(sType);
                    serviceSafe.setSubject(SecurityHelper.getCurrentSubject());
        
                    store.putService(serviceSafe, sName, sType);
                    }
                }
            finally
                {
                unlock();
                }
            }
        
        if (!serviceSafe.getServiceType().equals(sType))
            {
            throw new IllegalArgumentException("Requested service type \"" + sType +
                "\", but the existing service has type \"" + serviceSafe.getServiceType() + '"');
            }
        
        return serviceSafe;
        }
    
    // From interface: com.tangosol.net.Cluster
    public com.tangosol.net.Service ensureService(String sName, String sType)
        {
        // import Component.Net.Security;
        // import com.tangosol.net.InvocationService;
        // import com.tangosol.net.ProxyService;
        // import com.tangosol.net.security.DoAsAction;
        // import com.tangosol.net.Service;
        // import java.security.AccessController;
        
        String sCacheName = (InvocationService.TYPE_DEFAULT.equals(sType)
            || ProxyService.TYPE_DEFAULT.equals(sType)) ? null : "*";
        Security.checkPermission(getInternalCluster(), sName, sCacheName, "join");
        
        SafeCluster.EnsureSafeServiceAction action = (SafeCluster.EnsureSafeServiceAction) _newChild("EnsureSafeServiceAction");
        action.setServiceName(sName);
        action.setServiceType(sType);
        
        return (Service) (System.getSecurityManager() == null
                 ? action.run()
                 : AccessController.doPrivileged(new DoAsAction(action)));
        }
    
    // From interface: com.oracle.coherence.common.base.Lockable
    public com.oracle.coherence.common.base.Lockable.Unlockable exclusively()
        {
        ensureLocked();
        
        return getUnlockable();
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public java.util.Map getAddressProviderMap()
        {
        return ensureDependencies().getAddressProviderMap();
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry getBuilderRegistry()
        {
        return ensureDependencies().getBuilderRegistry();
        }
    
    // Accessor for the property "Cluster"
    /**
     * Getter for property Cluster.<p>
    * Public access to the InternalCluster that requires the corresponding
    * LocalPermission.
     */
    public com.tangosol.coherence.component.net.Cluster getCluster()
        {
        checkInternalAccess();
        
        return getInternalCluster();
        }

    @Override
    public DaemonPool getCommonDaemonPool()
        {
        DaemonPool pool = m_commonDaemon;
        if (pool == null)
            {
            synchronized (this)
                {
                pool = m_commonDaemon;
                if (pool == null)
                    {
                    ParameterizedBuilder<DaemonPool> builder = getBuilderRegistry().getBuilder(DaemonPool.class, DaemonPool.COMMON_POOL_BUILDER_NAME);
                    if (builder != null)
                        {
                        SystemPropertyParameterResolver resolver = new SystemPropertyParameterResolver();
                        ClassLoader                     loader = Classes.getContextClassLoader();
                        pool = m_commonDaemon = builder.realize(resolver, loader, null);
                        }

                    if (pool == null)
                        {
                        pool = Daemons.newDaemonPool(new DefaultDaemonPoolDependencies());
                        }

                    pool.start();
                    }
                }
            }
        return pool;
        }

    // From interface: com.tangosol.net.Cluster
    // Accessor for the property "ContextClassLoader"
    /**
     * Getter for property ContextClassLoader.<p>
    * The context ClassLoader for this cluster.
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
    
    // From interface: com.tangosol.net.Cluster
    // Accessor for the property "Dependencies"
    /**
     * Getter for property Dependencies.<p>
    * The dependencies for the wrapped Cluster.
    * @volatile
     */
    public com.tangosol.net.ClusterDependencies getDependencies()
        {
        return __m_Dependencies;
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public java.net.InetAddress getDiscoveryInterface()
        {
        return ensureDependencies().getGroupInterface();
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public int getDiscoveryTimeToLive()
        {
        return ensureDependencies().getGroupTimeToLive();
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public int getEdition()
        {
        return ensureDependencies().getEdition();
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public String getEditionName()
        {
        // import Component.Application.Console.Coherence;
        
        return Coherence.EDITION_NAMES[ensureDependencies().getEdition()];
        }
    
    // Accessor for the property "EnsureClusterAction"
    /**
     * Getter for property EnsureClusterAction.<p>
    * PrivilegedAction to call ensureRunningCluster.
     */
    public java.security.PrivilegedAction getEnsureClusterAction()
        {
        return __m_EnsureClusterAction;
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public java.util.Map getFilterMap()
        {
        return ensureDependencies().getFilterMap();
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public com.tangosol.net.security.IdentityAsserter getIdentityAsserter()
        {
        // import Component.Net.Security;
        
        return Security.getInstance().getIdentityAsserter();
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public com.tangosol.net.security.IdentityTransformer getIdentityTransformer()
        {
        // import Component.Net.Security;
        
        return Security.getInstance().getIdentityTransformer();
        }
    
    // Accessor for the property "InternalCluster"
    /**
     * Getter for property InternalCluster.<p>
    * The actual (wrapped) cluster.
     */
    protected com.tangosol.coherence.component.net.Cluster getInternalCluster()
        {
        return __m_InternalCluster;
        }
    
    // From interface: com.tangosol.net.Cluster
    // From interface: com.tangosol.net.OperationalContext
    public com.tangosol.net.Member getLocalMember()
        {
        // import Component.Net.Cluster;
        
        Cluster cluster = getInternalCluster();
        return cluster == null ? getSurrogateMember() : cluster.getLocalMember();
        }
    
    // Accessor for the property "LocalServices"
    /**
     * Getter for property LocalServices.<p>
    * A set of local services.
     */
    protected java.util.Set getLocalServices()
        {
        return __m_LocalServices;
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public int getLocalTcpPort()
        {
        // import Component.Net.Cluster;
        
        Cluster cluster = getCluster();
        return cluster == null
            ? 0
            : cluster.getSocketManager().getAcceptorChannel().getPort();
        }
    
    // Accessor for the property "Lock"
    /**
     * Getter for property Lock.<p>
    * Lock used to protect this SafeCluster instance against multi-threaded
    * usage.
     */
    public java.util.concurrent.locks.ReentrantLock getLock()
        {
        return __m_Lock;
        }
    
    // From interface: com.tangosol.net.Cluster
    // Accessor for the property "Management"
    /**
     * Getter for property Management.<p>
    * The management gateway.
     */
    public com.tangosol.net.management.Registry getManagement()
        {
        return __m_Management;
        }
    
    // Accessor for the property "RunningCluster"
    /**
     * Getter for property RunningCluster.<p>
    * Calculated property returning a running cluster. 
    * 
    * The only reason we have "getRunningCluster" in addition to
    * "ensureRunningCluster" is that RunningCluster property is used by the
    * integrator.
     */
    public com.tangosol.coherence.component.net.Cluster getRunningCluster()
        {
        // import Component.Net.Cluster;
        // import com.tangosol.net.security.DoAsAction;
        // import java.security.AccessController;
        
        if (System.getSecurityManager() == null)
            {
            return ensureRunningCluster();
            }
        
        return (Cluster) AccessController.doPrivileged(
            new DoAsAction(getEnsureClusterAction()));
        }
    
    // Accessor for the property "ScopedServiceStore"
    /**
     * Getter for property ScopedServiceStore.<p>
    * The scoped store for Service references.
     */
    public com.tangosol.net.internal.ScopedServiceReferenceStore getScopedServiceStore()
        {
        return __m_ScopedServiceStore;
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public java.util.Map getSerializerMap()
        {
        return ensureDependencies().getSerializerMap();
        }
    
    // From interface: com.tangosol.net.Cluster
    public com.tangosol.net.Service getService(String sName)
        {
        // import Component.Net.Security;
        // import Component.Util.SafeService;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.Service;
        // import com.tangosol.net.internal.ScopedServiceReferenceStore as com.tangosol.net.internal.ScopedServiceReferenceStore;
        
        Security.checkPermission(getInternalCluster(), sName, null, "join");
        
        com.tangosol.net.internal.ScopedServiceReferenceStore store       = getScopedServiceStore();
        SafeService  serviceSafe = (SafeService) store.getService(sName);
        if (serviceSafe == null)
            {
            ensureLocked();
            try
                {
                serviceSafe = (SafeService) store.getService(sName);
                if (serviceSafe == null)
                    {
                    Service service = getRunningCluster().getService(sName);
                    if (service != null)
                        {
                        serviceSafe = instantiateSafeService(service);
        
                        serviceSafe.setSafeCluster(this);
                        serviceSafe.setServiceName(sName);
        
                        String sServiceType = service.getInfo().getServiceType();
        
                        serviceSafe.setServiceType(sServiceType);
                        serviceSafe.setContextClassLoader(getContextClassLoader());
        
                        store.putService(serviceSafe, sName, sServiceType);
                        }
                    }
                }
            finally
                {
                unlock();
                }
            }
        
        return serviceSafe;
        }
    
    // Accessor for the property "ServiceContext"
    /**
     * Getter for property ServiceContext.<p>
    * A map of Service related PermissionInfo objects keyed by the service
    * name.
    * 
    * Previously this property was maintained on the ClusterService, but it was
    * moved here as a result of Bug 27376204 which demonstrated that a service
    * restart could span two Cluster instances and if security was enabled the
    * PermissionInfo could be written into the first ClusterService instance,
    * node restart, and then read from the second, resulting in a failure.
     */
    protected java.util.Map getServiceContext()
        {
        return __m_ServiceContext;
        }
    
    // Accessor for the property "ShutdownHook"
    /**
     * Getter for property ShutdownHook.<p>
    * A runnable component used to clean up local services. Since the
    * SafeCluster does not have an associated thread and the local service may
    * not be cleaned up unless the stop() method is explicitly called, the
    * ShutdownHook child holds only a WeakReference to the parent SafeCluster
    * component. This avoids the Java runtime holding a hard reference to the
    * SafeCluster and all objects it refers to preventing a potential "memory
    * leak".
     */
    public SafeCluster.ShutdownHook getShutdownHook()
        {
        return __m_ShutdownHook;
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public java.util.Map getSnapshotArchiverMap()
        {
        return ensureDependencies().getSnapshotArchiverMap();
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public com.tangosol.net.SocketProviderFactory getSocketProviderFactory()
        {
        return ensureDependencies().getSocketProviderFactory();
        }
    
    public long getStartupTimeout()
        {
        return 0L;
        }
    
    // Accessor for the property "SurrogateMember"
    /**
     * Getter for property SurrogateMember.<p>
    * A Member component used in lieu of the Cluster local member in the
    * "remote" or "local" scenario, when Cluster services are not running.
     */
    public com.tangosol.coherence.component.net.Member getSurrogateMember()
        {
        // import Component.Net.Member;
        // import com.tangosol.net.ClusterDependencies;
        
        Member member = __m_SurrogateMember;
        if (member == null)
            {
            ClusterDependencies deps = ensureDependencies();
            member = new Member();
            member.configure(deps.getMemberIdentity(), deps.getLocalAddress());
        
            setSurrogateMember(member);
            }
        
        return member;
        }
    
    // From interface: com.tangosol.net.Cluster
    public long getTimeMillis()
        {
        try
            {
            return getInternalCluster().getTimeMillis();
            }
        catch (NullPointerException e)
            {
            return 0l;
            }
        }
    
    // Accessor for the property "Unlockable"
    /**
     * Getter for property Unlockable.<p>
    * AutoCloseable to release aquired lock via exclusively().
     */
    public SafeCluster.Unlockable getUnlockable()
        {
        return __m_Unlockable;
        }
    
    /**
     * Instantiate an instance of SafeService for a given service.
     */
    protected SafeService instantiateSafeService(com.tangosol.net.Service service)
        {
        // import Component.Util.SafeService;
        // import Component.Util.SafeService.SafeCacheService;
        // import Component.Util.SafeService.SafeCacheService.SafeDistributedCacheService;
        // import Component.Util.SafeService.SafeCacheService.SafeDistributedCacheService.SafePagedTopicService;
        // import Component.Util.SafeService.SafeInvocationService;
        // import Component.Util.SafeService.SafeProxyService;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.DistributedCacheService;
        // import com.tangosol.net.InvocationService;
        // import com.tangosol.net.PagedTopicService;
        // import com.tangosol.net.ProxyService;
        
        SafeService serviceSafe = service instanceof CacheService ?
                (
                service instanceof PagedTopicService      ? new SafePagedTopicService()
        
             : service instanceof DistributedCacheService ? new SafeDistributedCacheService()
                                                             : new SafeCacheService()
                )
             : service instanceof InvocationService ? new SafeInvocationService()
             : service instanceof ProxyService      ? new SafeProxyService()
                                                    : new SafeService();
        
        serviceSafe.setInternalService(service);
        return serviceSafe;
        }
    
    /**
     * Instantiate a Service of the given type.
     */
    public com.tangosol.net.Service instantiateService(String sName, String sType)
        {
        return isLocalService(sType)
            ? ensureLocalService(sName, sType)
            : getRunningCluster().ensureService(sName, sType);
        }
    
    // Accessor for the property "Disposed"
    /**
     * Getter for property Disposed.<p>
    * Specifies whether or not the SafeCluster reference has been disposed.
    * Once this flag is set, even explicit restarts are no longer possible.
     */
    public boolean isDisposed()
        {
        return __m_Disposed;
        }
    
    public static boolean isLocalService(String sType)
        {
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.InvocationService;
        
        return sType.equals(CacheService.TYPE_LOCAL)  ||
               sType.equals(CacheService.TYPE_REMOTE) ||
               sType.equals(CacheService.TYPE_REMOTE_GRPC) ||
               sType.equals(InvocationService.TYPE_REMOTE);
        }
    
    // Accessor for the property "Restart"
    /**
     * Getter for property Restart.<p>
    * Specifies whether or not the underlying cluster has to be automatically
    * restarted. Once this flag is turned to false only an explicit start()
    * call can restart it.
    * 
    * Note: this flag is also used to report the "Running" status since an
    * explicit shutdown always turns it off.
     */
    public boolean isRestart()
        {
        return __m_Restart;
        }
    
    // From interface: com.tangosol.net.Cluster
    public boolean isRunning()
        {
        // import Component.Net.Cluster;
        
        Cluster cluster = getInternalCluster();
        return cluster != null && isRestart() && cluster.isRunning();
        }
    
    // From interface: com.tangosol.net.OperationalContext
    public boolean isSubjectScopingEnabled()
        {
        // import Component.Net.Security;
        
        return Security.getInstance().isSubjectScoped();
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
        // import com.tangosol.util.SafeHashSet;
        
        setLocalServices(new SafeHashSet(5, 1.0f, 1.0f));
        setEnsureClusterAction((SafeCluster.EnsureClusterAction) _newChild("EnsureClusterAction"));
        setUnlockable((SafeCluster.Unlockable) _newChild("Unlockable"));
        
        super.onInit();
        }
    
    protected com.tangosol.coherence.component.net.Cluster restartCluster()
        {
        // import Component.Net.Cluster;
        // import com.tangosol.net.internal.ClusterJoinException;
        
        for (int cAttempts = 0; ; cAttempts++)
            {
            Cluster cluster = null;
            try
                {
                cluster = new Cluster();
                startCluster(cluster);
        
                return cluster;
                }
            catch (Throwable e)
                {
                if ((e instanceof ClusterJoinException ||
                     e.getCause() instanceof ClusterJoinException) &&
                    ++cAttempts < 5)
                    {
                    _trace("Cluster seniority changed during join; rejoining the cluster", 3);
                    continue;
                    }
        
                _trace("Error while starting cluster: " + getStackTrace(e), 1);
        
                try
                    {
                    if (isRunning())
                        {
                        cluster.stop();
                        }
                    }
                catch (Throwable e2)
                    {
                    _trace("Failed to stop cluster: " + getStackTrace(e2), 2);
                    // eat the exception
                    }
        
                if (e instanceof Error)
                    {
                    throw (Error) e;
                    }
                throw (RuntimeException) e;
                }
            }
        }
    
    // From interface: com.tangosol.net.Cluster
    public void resumeService(String sService)
        {
        // import Component.Net.Cluster;
        // import Component.Net.Security;
        // import com.tangosol.util.Base;
        
        Cluster cluster = getRunningCluster();
        if (cluster != null)
            {
            Security.checkPermission(cluster,
                                     Base.equals(sService, "Cluster") ? "*" : sService,
                                     /*sCache*/ null,
                                     "create");
        
            cluster.resumeService(sService);
            }
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStarted(com.tangosol.util.ServiceEvent evt)
        {
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStarting(com.tangosol.util.ServiceEvent evt)
        {
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStopped(com.tangosol.util.ServiceEvent evt)
        {
        // import com.tangosol.net.Service;
        
        Service service = (Service) evt.getService();
        if (isLocalService(service.getInfo().getServiceType()))
            {
            getLocalServices().remove(service);
            }
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStopping(com.tangosol.util.ServiceEvent evt)
        {
        }
    
    // From interface: com.tangosol.net.Cluster
    // Accessor for the property "ContextClassLoader"
    /**
     * Setter for property ContextClassLoader.<p>
    * The context ClassLoader for this cluster.
     */
    public void setContextClassLoader(ClassLoader loader)
        {
        __m_ContextClassLoader = loader;
        }
    
    // From interface: com.tangosol.net.Cluster
    // Accessor for the property "Dependencies"
    /**
     * Setter for property Dependencies.<p>
    * The dependencies for the wrapped Cluster.
    * @volatile
     */
    public void setDependencies(com.tangosol.net.ClusterDependencies deps)
        {
        // import com.tangosol.internal.net.cluster.DefaultClusterDependencies;
        
        if (isRunning())
            {
            throw new IllegalStateException("Cannot configure running cluster");
            }
        
        __m_Dependencies = (new DefaultClusterDependencies(deps).validate());
        
        // reset the surrogate member so it would be recreated on-demand
        // based on new ClusterDependendencies object (see getSurrogateMember)
        setSurrogateMember(null);
        }
    
    // Accessor for the property "Disposed"
    /**
     * Setter for property Disposed.<p>
    * Specifies whether or not the SafeCluster reference has been disposed.
    * Once this flag is set, even explicit restarts are no longer possible.
     */
    protected void setDisposed(boolean fDispose)
        {
        __m_Disposed = fDispose;
        }
    
    // Accessor for the property "EnsureClusterAction"
    /**
     * Setter for property EnsureClusterAction.<p>
    * PrivilegedAction to call ensureRunningCluster.
     */
    protected void setEnsureClusterAction(java.security.PrivilegedAction action)
        {
        __m_EnsureClusterAction = action;
        }
    
    // Accessor for the property "InternalCluster"
    /**
     * Setter for property InternalCluster.<p>
    * The actual (wrapped) cluster.
     */
    protected void setInternalCluster(com.tangosol.coherence.component.net.Cluster cluster)
        {
        __m_InternalCluster = cluster;
        }
    
    // Accessor for the property "LocalServices"
    /**
     * Setter for property LocalServices.<p>
    * A set of local services.
     */
    protected void setLocalServices(java.util.Set set)
        {
        __m_LocalServices = set;
        }
    
    // Accessor for the property "Lock"
    /**
     * Setter for property Lock.<p>
    * Lock used to protect this SafeCluster instance against multi-threaded
    * usage.
     */
    public void setLock(java.util.concurrent.locks.ReentrantLock lock)
        {
        __m_Lock = lock;
        }
    
    // From interface: com.tangosol.net.Cluster
    // Accessor for the property "Management"
    /**
     * Setter for property Management.<p>
    * The management gateway.
     */
    public void setManagement(com.tangosol.net.management.Registry registry)
        {
        // import Component.Net.Cluster;
        
        __m_Management = (registry);
        
        Cluster cluster = getInternalCluster();
        if (cluster != null)
            {
            cluster.setManagement(registry);
            }
        }
    
    // Accessor for the property "Restart"
    /**
     * Setter for property Restart.<p>
    * Specifies whether or not the underlying cluster has to be automatically
    * restarted. Once this flag is turned to false only an explicit start()
    * call can restart it.
    * 
    * Note: this flag is also used to report the "Running" status since an
    * explicit shutdown always turns it off.
     */
    protected void setRestart(boolean fRestart)
        {
        __m_Restart = fRestart;
        }
    
    // Accessor for the property "ScopedServiceStore"
    /**
     * Setter for property ScopedServiceStore.<p>
    * The scoped store for Service references.
     */
    protected void setScopedServiceStore(com.tangosol.net.internal.ScopedServiceReferenceStore store)
        {
        __m_ScopedServiceStore = store;
        }
    
    // Accessor for the property "ServiceContext"
    /**
     * Setter for property ServiceContext.<p>
    * A map of Service related PermissionInfo objects keyed by the service
    * name.
    * 
    * Previously this property was maintained on the ClusterService, but it was
    * moved here as a result of Bug 27376204 which demonstrated that a service
    * restart could span two Cluster instances and if security was enabled the
    * PermissionInfo could be written into the first ClusterService instance,
    * node restart, and then read from the second, resulting in a failure.
     */
    private void setServiceContext(java.util.Map mapContext)
        {
        __m_ServiceContext = mapContext;
        }
    
    // Accessor for the property "ShutdownHook"
    /**
     * Setter for property ShutdownHook.<p>
    * A runnable component used to clean up local services. Since the
    * SafeCluster does not have an associated thread and the local service may
    * not be cleaned up unless the stop() method is explicitly called, the
    * ShutdownHook child holds only a WeakReference to the parent SafeCluster
    * component. This avoids the Java runtime holding a hard reference to the
    * SafeCluster and all objects it refers to preventing a potential "memory
    * leak".
     */
    protected void setShutdownHook(SafeCluster.ShutdownHook hook)
        {
        __m_ShutdownHook = hook;
        }
    
    // Accessor for the property "SurrogateMember"
    /**
     * Setter for property SurrogateMember.<p>
    * A Member component used in lieu of the Cluster local member in the
    * "remote" or "local" scenario, when Cluster services are not running.
     */
    protected void setSurrogateMember(com.tangosol.coherence.component.net.Member member)
        {
        __m_SurrogateMember = member;
        }
    
    // Accessor for the property "Unlockable"
    /**
     * Setter for property Unlockable.<p>
    * AutoCloseable to release aquired lock via exclusively().
     */
    public void setUnlockable(SafeCluster.Unlockable unlockable)
        {
        __m_Unlockable = unlockable;
        }
    
    // From interface: com.tangosol.net.Cluster
    public void shutdown()
        {
        // import Component.Net.Cluster;
        
        ensureLocked();
        try
            {
            setRestart(false);
        
            Cluster cluster = getInternalCluster();
            if (cluster != null)
                {
                synchronized (cluster)
                    {
                    if (cluster.isRunning())
                        {
                        cluster.shutdown();
                        }
                    }
                }
        
            shutdownLocalServices();
            cleanup();
            }
        finally
            {
            unlock();
            }
        }
    
    public void shutdownLocalServices()
        {
        // import com.tangosol.net.Service;
        // import java.util.Iterator;
        
        SafeCluster.ShutdownHook hook = getShutdownHook();
        if (hook != null)
            {
            hook.unregister();
            setShutdownHook(null);
            }
        
        try
            {
            for (Iterator iter = getLocalServices().iterator(); iter.hasNext();)
                {
                Service service = (Service) iter.next();
                iter.remove();
                service.removeServiceListener(this);
                service.shutdown();
                }
            }
        catch (Throwable e) {}
        }
    
    // From interface: com.tangosol.net.Cluster
    public void start()
        {
        if (!isDisposed())
            {
            ensureLocked();
            try
                {
                if (!isDisposed())
                    {
                    setRestart(true);
                    }
                getRunningCluster();
                }
            finally
                {
                unlock();
                }
            }
        }
    
    protected void startCluster(com.tangosol.coherence.component.net.Cluster cluster)
        {
        cluster.setDependencies(ensureDependencies());
        cluster.setOperationalContext(this);
        cluster.setManagement(getManagement());
        cluster.getClusterService().setServiceContext(getServiceContext());
        cluster.start();
        _trace("Started cluster " + cluster, 3);
        }
    
    // From interface: com.tangosol.net.Cluster
    public void stop()
        {
        // import Component.Net.Cluster;
        
        ensureLocked();
        try
            {
            setRestart(false);
        
            Cluster cluster = getInternalCluster();
            if (cluster != null)
                {
                synchronized (cluster)
                    {
                    if (cluster.isRunning())
                        {
                        cluster.stop();
                        }
                    }
                }
        
            shutdownLocalServices();
            cleanup();
            }
        finally
             {
             unlock();
             }
        }
    
    // From interface: com.tangosol.net.Cluster
    public void suspendService(String sService)
        {
        // import Component.Net.Cluster;
        // import Component.Net.Security;
        // import com.tangosol.util.Base;
        
        Cluster cluster = getRunningCluster();
        if (cluster != null)
            {
            Security.checkPermission(cluster,
                                     Base.equals(sService, "Cluster") ? "*" : sService,
                                     /*sCache*/ null,
                                     "destroy");
        
            cluster.suspendService(sService);
            }
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + ": " + getInternalCluster();
        }
    
    public void unlock()
        {
        getLock().unlock();
        }

    // ---- class: com.tangosol.coherence.component.util.SafeCluster$EnsureClusterAction
    
    /**
     * Privileged action to start cluster.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EnsureClusterAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public EnsureClusterAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public EnsureClusterAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.SafeCluster.EnsureClusterAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeCluster$EnsureClusterAction".replace('/', '.'));
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
            return ((SafeCluster) get_Module()).ensureRunningCluster();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.SafeCluster$EnsureSafeServiceAction
    
    /**
     * Privileged action to start a service.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EnsureSafeServiceAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property ServiceName
         *
         */
        private String __m_ServiceName;
        
        /**
         * Property ServiceType
         *
         */
        private String __m_ServiceType;
        
        // Default constructor
        public EnsureSafeServiceAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public EnsureSafeServiceAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.SafeCluster.EnsureSafeServiceAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeCluster$EnsureSafeServiceAction".replace('/', '.'));
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
        
        // Accessor for the property "ServiceName"
        /**
         * Getter for property ServiceName.<p>
         */
        public String getServiceName()
            {
            return __m_ServiceName;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Getter for property ServiceType.<p>
         */
        public String getServiceType()
            {
            return __m_ServiceType;
            }
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            return ((SafeCluster) get_Module()).ensureSafeService(getServiceName(), getServiceType());
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Setter for property ServiceName.<p>
         */
        public void setServiceName(String sName)
            {
            __m_ServiceName = sName;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Setter for property ServiceType.<p>
         */
        public void setServiceType(String sType)
            {
            __m_ServiceType = sType;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.SafeCluster$ParseDependenciesAction
    
    /**
     * Privileged action.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ParseDependenciesAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property XmlConfig
         *
         */
        private com.tangosol.run.xml.XmlElement __m_XmlConfig;
        
        // Default constructor
        public ParseDependenciesAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ParseDependenciesAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.SafeCluster.ParseDependenciesAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeCluster$ParseDependenciesAction".replace('/', '.'));
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
        
        // Accessor for the property "XmlConfig"
        /**
         * Getter for property XmlConfig.<p>
         */
        public com.tangosol.run.xml.XmlElement getXmlConfig()
            {
            return __m_XmlConfig;
            }
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            // import com.tangosol.internal.net.cluster.LegacyXmlClusterDependencies;
            
            return new LegacyXmlClusterDependencies().fromXml(getXmlConfig());
            }
        
        // Accessor for the property "XmlConfig"
        /**
         * Setter for property XmlConfig.<p>
         */
        public void setXmlConfig(com.tangosol.run.xml.XmlElement xmlConfig)
            {
            __m_XmlConfig = xmlConfig;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.SafeCluster$ShutdownHook
    
    /**
     * Abstract runnable component used as a virtual-machine shutdown hook.
     * Runnable component used as a virtual-machine shutdown hook for local
     * services.
     * 
     * @see SafeCluster#ensureLocalService
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ShutdownHook
            extends    com.tangosol.coherence.component.util.ShutdownHook
        {
        // ---- Fields declarations ----
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
            __mapChildren.put("UnregisterAction", SafeCluster.ShutdownHook.UnregisterAction.get_CLASS());
            }
        
        // Default constructor
        public ShutdownHook()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ShutdownHook(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.SafeCluster.ShutdownHook();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeCluster$ShutdownHook".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        public void run()
            {
            // import java.lang.ref.WeakReference;
            
            if (getThread() != null)
                {
                WeakReference refSafe     = (WeakReference) get_Feed();
                SafeCluster       clusterSafe = (SafeCluster) refSafe.get();
                if (clusterSafe != null)
                    {
                    clusterSafe.shutdownLocalServices();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.SafeCluster$ShutdownHook$UnregisterAction
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class UnregisterAction
                extends    com.tangosol.coherence.component.util.ShutdownHook.UnregisterAction
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public UnregisterAction()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public UnregisterAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.SafeCluster.ShutdownHook.UnregisterAction();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/SafeCluster$ShutdownHook$UnregisterAction".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.SafeCluster$Unlockable
    
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
            return new com.tangosol.coherence.component.util.SafeCluster.Unlockable();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeCluster$Unlockable".replace('/', '.'));
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
            ((SafeCluster) get_Module()).unlock();
            }
        }
    }
