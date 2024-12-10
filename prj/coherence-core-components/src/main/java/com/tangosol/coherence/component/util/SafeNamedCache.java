
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.SafeNamedCache

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.safeService.SafeCacheService;
import com.oracle.coherence.common.base.NonBlocking;
import com.oracle.coherence.common.base.Timeout;
import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.internal.util.listener.VersionAwareListeners;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.ServiceDependencies;
import com.tangosol.net.ServiceStoppedException;
import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.CacheStore;
import com.tangosol.net.partition.VersionAwareMapListener;
import com.tangosol.net.partition.VersionedPartitions;
import com.tangosol.net.security.DoAsAction;
import com.tangosol.net.security.LocalPermission;
import com.tangosol.util.AsynchronousAgent;
import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.ValueExtractor;
import java.security.AccessController;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import javax.security.auth.Subject;

/*
* Integrates
*     com.tangosol.net.NamedCache
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SafeNamedCache
        extends    com.tangosol.coherence.component.Util
        implements com.oracle.coherence.common.base.Lockable,
                   com.tangosol.io.ClassLoaderAware,
                   com.tangosol.net.NamedCache,
                   com.tangosol.net.cache.BinaryEntryStore,
                   com.tangosol.net.cache.CacheStore,
                   com.tangosol.net.partition.VersionAwareMapListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property CacheName
     *
     */
    private transient String __m_CacheName;
    
    /**
     * Property ClassLoader
     *
     */
    private transient ClassLoader __m_ClassLoader;
    
    /**
     * Property Destroyed
     *
     * Specifies whether or not the underlying NamedCache has been explicitly
     * destroyed.
     */
    private boolean __m_Destroyed;
    
    /**
     * Property EnsureCacheAction
     *
     * PrivilegedAction to call ensureRunningNamedService.
     */
    private java.security.PrivilegedAction __m_EnsureCacheAction;
    
    /**
     * Property InternalNamedCache
     *
     * Actual (wrapped) NamedCache.
     * 
     * @volatile
     */
    private volatile com.tangosol.net.NamedCache __m_InternalNamedCache;
    
    /**
     * Property ListenerSupport
     *
     */
    private transient com.tangosol.util.MapListenerSupport __m_ListenerSupport;
    
    /**
     * Property Lock
     *
     * Lock used to protect this SafeNamedCache instance against multi-threaded
     * usage. 
     */
    private java.util.concurrent.locks.ReentrantLock __m_Lock;
    
    /**
     * Property Released
     *
     * Specifies whether or not the underlying NamedCache has been explicitly
     * released.
     */
    private boolean __m_Released;
    
    /**
     * Property RestartCacheAction
     *
     * PrivilegedAction to call ensureCache.
     */
    private java.security.PrivilegedAction __m_RestartCacheAction;
    
    /**
     * Property SafeAsyncNamedCache
     *
     */
    private SafeAsyncNamedCache __m_SafeAsyncNamedCache;
    
    /**
     * Property SafeCacheService
     *
     */
    private com.tangosol.coherence.component.util.safeService.SafeCacheService __m_SafeCacheService;
    
    /**
     * Property Started
     *
     * 
     * @volatile
     */
    private volatile boolean __m_Started;
    
    /**
     * Property Subject
     *
     * The optional Subject associated with the cache.
     */
    private javax.security.auth.Subject __m_Subject;
    
    /**
     * Property TloListenerVersions
     *
     */
    private ThreadLocal __m_TloListenerVersions;
    
    /**
     * Property Unlockable
     *
     * AutoCloseable to release aquired lock via exclusively().
     */
    private SafeNamedCache.Unlockable __m_Unlockable;
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
        __mapChildren.put("EnsureCacheAction", SafeNamedCache.EnsureCacheAction.get_CLASS());
        __mapChildren.put("RestartCacheAction", SafeNamedCache.RestartCacheAction.get_CLASS());
        __mapChildren.put("Unlockable", SafeNamedCache.Unlockable.get_CLASS());
        }
    
    // Default constructor
    public SafeNamedCache()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SafeNamedCache(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setListenerSupport(new com.tangosol.util.MapListenerSupport());
            setLock(new java.util.concurrent.locks.ReentrantLock());
            setTloListenerVersions(new java.lang.ThreadLocal());
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
        return new com.tangosol.coherence.component.util.SafeNamedCache();
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
            clz = Class.forName("com.tangosol.coherence/component/util/SafeNamedCache".replace('/', '.'));
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
    
    //++ com.tangosol.net.NamedCache integration
    // Access optimization
    // properties integration
    // methods integration
    private void addIndex$Router(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        getRunningNamedCache().addIndex(extractor, fOrdered, comparator);
        }
    public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        addIndex$Router(prepareExtractor(extractor), fOrdered, comparator);
        }
    private Object aggregate$Router(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        return getRunningNamedCache().aggregate(filter, agent);
        }
    public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        // import com.tangosol.net.NamedCache;
        
        NamedCache cache = getNonblockingCache(agent);
        
        return cache == null ? aggregate$Router(filter, agent) : cache.aggregate(filter, agent);
        }
    private Object aggregate$Router(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        return getRunningNamedCache().aggregate(collKeys, agent);
        }
    public Object aggregate(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        // import com.tangosol.net.NamedCache;
        
        NamedCache cache = getNonblockingCache(agent);
        
        return cache == null ? aggregate$Router(collKeys, agent) : cache.aggregate(collKeys, agent);
        }
    public void clear()
        {
        getRunningNamedCache().clear();
        }
    public Object compute(Object key, com.tangosol.util.function.Remote.BiFunction function)
        {
        return getRunningNamedCache().compute(key, function);
        }
    public Object compute(Object oKey, java.util.function.BiFunction function)
        {
        return getRunningNamedCache().compute(oKey, function);
        }
    public Object computeIfAbsent(Object key, com.tangosol.util.function.Remote.Function function)
        {
        return getRunningNamedCache().computeIfAbsent(key, function);
        }
    public Object computeIfAbsent(Object oKey, java.util.function.Function function)
        {
        return getRunningNamedCache().computeIfAbsent(oKey, function);
        }
    public Object computeIfPresent(Object key, com.tangosol.util.function.Remote.BiFunction function)
        {
        return getRunningNamedCache().computeIfPresent(key, function);
        }
    public Object computeIfPresent(Object oKey, java.util.function.BiFunction function)
        {
        return getRunningNamedCache().computeIfPresent(oKey, function);
        }
    public boolean containsKey(Object oKey)
        {
        return getRunningNamedCache().containsKey(oKey);
        }
    public boolean containsValue(Object oValue)
        {
        return getRunningNamedCache().containsValue(oValue);
        }
    public java.util.Set entrySet()
        {
        return getRunningNamedCache().entrySet();
        }
    public java.util.Set entrySet(com.tangosol.util.Filter filter)
        {
        return getRunningNamedCache().entrySet(filter);
        }
    public java.util.Set entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        return getRunningNamedCache().entrySet(filter, comparator);
        }
    public void forEach(com.tangosol.util.Filter filter, java.util.function.BiConsumer action)
        {
        getRunningNamedCache().forEach(filter, action);
        }
    public void forEach(java.util.Collection collKeys, java.util.function.BiConsumer action)
        {
        getRunningNamedCache().forEach(collKeys, action);
        }
    public void forEach(java.util.function.BiConsumer consumer)
        {
        getRunningNamedCache().forEach(consumer);
        }
    public Object get(Object oKey)
        {
        return getRunningNamedCache().get(oKey);
        }
    public java.util.Map getAll(java.util.Collection col)
        {
        return getRunningNamedCache().getAll(col);
        }
    public Object getOrDefault(Object oKey, Object oValue)
        {
        return getRunningNamedCache().getOrDefault(oKey, oValue);
        }
    private Object invoke$Router(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getRunningNamedCache().invoke(oKey, agent);
        }
    public Object invoke(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.net.NamedCache;
        
        NamedCache cache = getNonblockingCache(agent);
        
        return cache == null ? invoke$Router(oKey, agent) : cache.invoke(oKey, agent);
        }
    private java.util.Map invokeAll$Router(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getRunningNamedCache().invokeAll(filter, agent);
        }
    public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.net.NamedCache;
        
        NamedCache cache = getNonblockingCache(agent);
        
        return cache == null ? invokeAll$Router(filter, agent) : cache.invokeAll(filter, agent);
        }
    private java.util.Map invokeAll$Router(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getRunningNamedCache().invokeAll(collKeys, agent);
        }
    public java.util.Map invokeAll(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.net.NamedCache;
        
        NamedCache cache = getNonblockingCache(agent);
        
        return cache == null ? invokeAll$Router(collKeys, agent) : cache.invokeAll(collKeys, agent);
        }
    public boolean isEmpty()
        {
        return getRunningNamedCache().isEmpty();
        }
    public java.util.Set keySet()
        {
        return getRunningNamedCache().keySet();
        }
    public java.util.Set keySet(com.tangosol.util.Filter filter)
        {
        return getRunningNamedCache().keySet(filter);
        }
    public boolean lock(Object oKey)
        {
        return getRunningNamedCache().lock(oKey);
        }
    public boolean lock(Object oKey, long cMillis)
        {
        return getRunningNamedCache().lock(oKey, cMillis);
        }
    public Object merge(Object key, Object value, com.tangosol.util.function.Remote.BiFunction function)
        {
        return getRunningNamedCache().merge(key, value, function);
        }
    public Object merge(Object oKey, Object oValue, java.util.function.BiFunction function)
        {
        return getRunningNamedCache().merge(oKey, oValue, function);
        }
    public Object put(Object oKey, Object oValue)
        {
        return getRunningNamedCache().put(oKey, oValue);
        }
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        return getRunningNamedCache().put(oKey, oValue, cMillis);
        }
    public void putAll(java.util.Map map)
        {
        getRunningNamedCache().putAll(map);
        }
    public Object putIfAbsent(Object oKey, Object oValue)
        {
        return getRunningNamedCache().putIfAbsent(oKey, oValue);
        }
    public Object remove(Object oKey)
        {
        return getRunningNamedCache().remove(oKey);
        }
    public boolean remove(Object oKey, Object oValue)
        {
        return getRunningNamedCache().remove(oKey, oValue);
        }
    private void removeIndex$Router(com.tangosol.util.ValueExtractor extractor)
        {
        getRunningNamedCache().removeIndex(extractor);
        }
    public void removeIndex(com.tangosol.util.ValueExtractor extractor)
        {
        removeIndex$Router(prepareExtractor(extractor));
        }
    public Object replace(Object oKey, Object oValue)
        {
        return getRunningNamedCache().replace(oKey, oValue);
        }
    public boolean replace(Object oKey, Object oValueOld, Object oValueNew)
        {
        return getRunningNamedCache().replace(oKey, oValueOld, oValueNew);
        }
    public void replaceAll(com.tangosol.util.Filter filter, com.tangosol.util.function.Remote.BiFunction function)
        {
        getRunningNamedCache().replaceAll(filter, function);
        }
    public void replaceAll(com.tangosol.util.function.Remote.BiFunction function)
        {
        getRunningNamedCache().replaceAll(function);
        }
    public void replaceAll(java.util.Collection collKeys, com.tangosol.util.function.Remote.BiFunction function)
        {
        getRunningNamedCache().replaceAll(collKeys, function);
        }
    public void replaceAll(java.util.function.BiFunction function)
        {
        getRunningNamedCache().replaceAll(function);
        }
    public int size()
        {
        return getRunningNamedCache().size();
        }
    public com.tangosol.util.stream.RemoteStream stream()
        {
        return getRunningNamedCache().stream();
        }
    public com.tangosol.util.stream.RemoteStream stream(com.tangosol.util.Filter filter)
        {
        return getRunningNamedCache().stream(filter);
        }
    public com.tangosol.util.stream.RemoteStream stream(com.tangosol.util.Filter filter, com.tangosol.util.ValueExtractor extractor)
        {
        return getRunningNamedCache().stream(filter, extractor);
        }
    public com.tangosol.util.stream.RemoteStream stream(com.tangosol.util.ValueExtractor extractor)
        {
        return getRunningNamedCache().stream(extractor);
        }
    public com.tangosol.util.stream.RemoteStream stream(java.util.Collection collKeys)
        {
        return getRunningNamedCache().stream(collKeys);
        }
    public com.tangosol.util.stream.RemoteStream stream(java.util.Collection collKeys, com.tangosol.util.ValueExtractor extractor)
        {
        return getRunningNamedCache().stream(collKeys, extractor);
        }
    public void truncate()
        {
        getRunningNamedCache().truncate();
        }
    public boolean unlock(Object oKey)
        {
        return getRunningNamedCache().unlock(oKey);
        }
    public java.util.Collection values()
        {
        return getRunningNamedCache().values();
        }
    public java.util.Collection values(com.tangosol.util.Filter filter)
        {
        return getRunningNamedCache().values(filter);
        }
    public java.util.Collection values(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        return getRunningNamedCache().values(filter, comparator);
        }
    //-- com.tangosol.net.NamedCache integration
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;
        
        addMapListener(listener, (Filter) null, false);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
        {
        // import com.tangosol.internal.util.listener.VersionAwareListeners;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.partition.VersionAwareMapListener;
        // import com.tangosol.util.MapListenerSupport;
        // import com.tangosol.util.MapListenerSupport$SynchronousListener as com.tangosol.util.MapListenerSupport.SynchronousListener;
        // import com.tangosol.util.MapTriggerListener;
        
        if (listener == this)
            {
            NamedCache cache = getRunningNamedCache();
            try
                {
                cache.addMapListener(listener, filter, fLite);
                }
            catch (RuntimeException e)
                {
                if (cache != null && cache.isActive() &&
                        cache.getCacheService().isRunning())
                    {
                    throw e;
                    }
                // NamedCache has been invalidated
                }
            }
        else if (listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener ||
                 listener instanceof MapTriggerListener)
            {
            getRunningNamedCache().addMapListener(listener, filter, fLite);
            }
        else if (listener != null)
            {
            MapListenerSupport support = getListenerSupport();    
        
            boolean fRegister = false;
            synchronized (support)
                {
                listener = VersionAwareListeners.createListener(listener);
        
                fRegister = support.addListenerWithCheck(listener, filter, fLite);
                }
        
            if (fRegister)
                {
                try
                    {
                    if (listener.isVersionAware())
                        {
                        getTloListenerVersions().set(
                            ((VersionAwareMapListener) listener).getVersions());
                        }
        
                    addMapListener(this, filter, fLite);
                    }
                catch (RuntimeException e)
                    {
                    getListenerSupport().removeListener(listener, filter);
                    throw e;
                    }
                finally
                    {
                    getTloListenerVersions().remove();
                    }
                }
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
        {
        // import com.tangosol.internal.util.listener.VersionAwareListeners;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.partition.VersionAwareMapListener;
        // import com.tangosol.util.MapListenerSupport;
        // import com.tangosol.util.MapListenerSupport$SynchronousListener as com.tangosol.util.MapListenerSupport.SynchronousListener;
        
        if (listener == this)
            {
            NamedCache cache = getRunningNamedCache();
            try
                {
                cache.addMapListener(listener, oKey, fLite);
                }
            catch (RuntimeException e)
                {
                if (cache != null && cache.isActive() &&
                        cache.getCacheService().isRunning())
                    {
                    throw e;
                    }
                // NamedCache has been invalidated
                }
            }
        else if (listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener)
            {
            getRunningNamedCache().addMapListener(listener, oKey, fLite);
            }
        else if (listener != null)
            {
            MapListenerSupport support = getListenerSupport();
        
            boolean fRegister = false;
            synchronized (support)
                {
                listener = VersionAwareListeners.createListener(listener);
        
                fRegister = support.addListenerWithCheck(listener, oKey, fLite);
                }
        
            if (fRegister)
                {
                try
                    {
                    if (listener.isVersionAware())
                        {
                        getTloListenerVersions().set(
                            ((VersionAwareMapListener) listener).getVersions());
                        }
                    addMapListener(this, oKey, fLite);
                    }
                catch (RuntimeException e)
                    {
                    getListenerSupport().removeListener(listener, oKey);
                    throw e;
                    }
                finally
                    {
                    getTloListenerVersions().remove();
                    }
                }
            // TODO hraja: if a listener is already registered that 'covers' this
            //      key we need to consider where lVersion is:
            //        -2) replay all MapEvents on the server
            //        -1) current version and all future mapevents (priming)
            //         0) plain register
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    @Override
    @SuppressWarnings("resource")
    public com.tangosol.net.AsyncNamedCache async(com.tangosol.net.AsyncNamedMap.Option[] options)
        {
        SafeAsyncNamedCache cacheAsync = getSafeAsyncNamedCache();
        if (cacheAsync == null)
            {
            ensureLocked();
            try
                {
                NamedCache          cache     = getRunningNamedCache();
                SafeAsyncNamedCache cacheSafe = new SafeAsyncNamedCache();
                cacheSafe.setCacheName(getCacheName());
                cacheSafe.setSafeNamedCache(this);
                cacheSafe.setSafeCacheService(getSafeCacheService());
                cacheSafe.setClassLoader(getClassLoader());
                cacheSafe.setOptions(options);
                cacheSafe.setInternalNamedCache(cache.async(options));
                cacheSafe.setStarted(true);
                setSafeAsyncNamedCache(cacheAsync = cacheSafe);
                }
            finally
                {
                unlock();
                }
            }
        return cacheAsync;
        }
    
    // From interface: com.tangosol.net.partition.VersionAwareMapListener
    public int characteristics()
        {
        // import com.tangosol.util.MapListener;
        
        return MapListener.ASYNCHRONOUS | MapListener.VERSION_AWARE;
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
    
    // From interface: com.tangosol.net.NamedCache
    public void destroy()
        {
        // import Component.Util.SafeService.SafeCacheService;
        
        SafeCacheService safeservice = getSafeCacheService();
        SafeCluster      safecluster = safeservice.getSafeCluster();
        
        releaseListeners();
        
        safeservice.destroyCache(this);
        
        ensureGlobalLock();
        try
            {
            setDestroyed(true);
            setReleased(true);
            setInternalNamedCache(null);
            }
        finally
            {
            unlockGlobal();
            }
        }
    
    /**
     * Ensure the caller acquires all locks,  including SafeCluster, SafeService
    * and SafeNamedCache locks, or an excpetion is thrown.
     */
    public void ensureGlobalLock()
        {
        SafeService service = getSafeCacheService();
        
        service.ensureGlobalLock();
        try
            {
            ensureLocked();
            }
        catch (RuntimeException e)
            {
            service.unlockGlobal();
            throw e;
            }
        }
    
    /**
     * Ensure the caller acquires the SafeNamedCache lock, or an excpetion is
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
        
        ServiceDependencies deps = getSafeCacheService().getDependencies();
        
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
                    new RequestTimeoutException("Failed to acquire NamedCache lock in " + cTimeout + "ms"));
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw Base.ensureRuntimeException(e, "Interrupted while attempting to acquire NamedCache lock"); 
            }
        }
    
    public com.tangosol.net.NamedCache ensureRunningNamedCache()
        {
        // import com.tangosol.net.NamedCache;
        
        checkInternalAccess();
        
        NamedCache  cache       = getInternalNamedCache();
        SafeService serviceSafe = getSafeCacheService();
        if (serviceSafe == null || !serviceSafe.isRunning() ||
            cache == null || !cache.isActive() || !isStarted())
            {
            if ((serviceSafe == null || !serviceSafe.isRunning()) && serviceSafe.isServiceThread())
                {
                throw new IllegalStateException(
                    "Service can not be restarted on a thread owned by the service");
                }
        
            ensureGlobalLock();
            try
                {
                cache       = getInternalNamedCache();
                serviceSafe = getSafeCacheService();
                if (serviceSafe == null || !serviceSafe.isRunning() ||
                    cache == null || !cache.isActive() || !isStarted())
                    {
                    if (isReleased() || isDestroyed())
                        {
                        String reason = isDestroyed() ? "destroyed" : "released";
                        throw new IllegalStateException("SafeNamedCache was explicitly " + reason);
                        }
                    else
                        {
                        // restart the actual named cache
                        if (cache != null)
                            {
                            setInternalNamedCache(null);
                            _trace("Restarting NamedCache: " + getCacheName(), 3);
                            }
        
                        setInternalNamedCache(cache = restartNamedCache());
                        registerListeners(cache);
        
                        setStarted(true);
                        }
                    }
                }
            finally
                {
                unlockGlobal();
                }
            }
        
        return cache;
        }
    
    // From interface: com.tangosol.net.partition.VersionAwareMapListener
    public void entryDeleted(com.tangosol.util.MapEvent evt)
        {
        translateMapEvent(evt);
        }
    
    // From interface: com.tangosol.net.partition.VersionAwareMapListener
    public void entryInserted(com.tangosol.util.MapEvent evt)
        {
        translateMapEvent(evt);
        }
    
    // From interface: com.tangosol.net.partition.VersionAwareMapListener
    public void entryUpdated(com.tangosol.util.MapEvent evt)
        {
        translateMapEvent(evt);
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void erase(com.tangosol.util.BinaryEntry binEntry)
        {
        getRunningBinaryEntryStore().erase(binEntry);
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public void erase(Object oKey)
        {
        getRunningCacheStore().erase(oKey);
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public void eraseAll(java.util.Collection colKeys)
        {
        getRunningCacheStore().eraseAll(colKeys);
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void eraseAll(java.util.Set setBinEntries)
        {
        getRunningBinaryEntryStore().eraseAll(setBinEntries);
        }
    
    // From interface: com.oracle.coherence.common.base.Lockable
    public com.oracle.coherence.common.base.Lockable.Unlockable exclusively()
        {
        ensureLocked();
        
        return ((SafeNamedCache.Unlockable) _newChild("Unlockable"));
        }
    
    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "CacheName"
    /**
     * Getter for property CacheName.<p>
     */
    public String getCacheName()
        {
        return __m_CacheName;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public com.tangosol.net.CacheService getCacheService()
        {
        return getSafeCacheService();
        }
    
    // Accessor for the property "ClassLoader"
    /**
     * Getter for property ClassLoader.<p>
     */
    public ClassLoader getClassLoader()
        {
        return __m_ClassLoader;
        }
    
    // From interface: com.tangosol.io.ClassLoaderAware
    public ClassLoader getContextClassLoader()
        {
        return getClassLoader();
        }
    
    // Accessor for the property "EnsureCacheAction"
    /**
     * Getter for property EnsureCacheAction.<p>
    * PrivilegedAction to call ensureRunningNamedService.
     */
    public java.security.PrivilegedAction getEnsureCacheAction()
        {
        return __m_EnsureCacheAction;
        }
    
    // Accessor for the property "InternalNamedCache"
    /**
     * Getter for property InternalNamedCache.<p>
    * Actual (wrapped) NamedCache.
    * 
    * @volatile
     */
    protected com.tangosol.net.NamedCache getInternalNamedCache()
        {
        return __m_InternalNamedCache;
        }
    
    // Accessor for the property "ListenerSupport"
    /**
     * Getter for property ListenerSupport.<p>
     */
    public com.tangosol.util.MapListenerSupport getListenerSupport()
        {
        return __m_ListenerSupport;
        }
    
    // Accessor for the property "Lock"
    /**
     * Getter for property Lock.<p>
    * Lock used to protect this SafeNamedCache instance against multi-threaded
    * usage. 
     */
    public java.util.concurrent.locks.ReentrantLock getLock()
        {
        return __m_Lock;
        }
    
    // Accessor for the property "NamedCache"
    /**
     * Getter for property NamedCache.<p>
    * Public access to the InternalNamedCache that requires the corresponding
    * LocalPermission.
     */
    public com.tangosol.net.NamedCache getNamedCache()
        {
        checkInternalAccess();
        
        return getInternalNamedCache();
        }
    
    /**
     * Check whether or not the caller requires non-blocking processing and if
    * so, return an underlying NamedCache reference that is guaranteed to
    * execute without blocking. Otherwise return null.
     */
    protected com.tangosol.net.NamedCache getNonblockingCache()
        {
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.ServiceStoppedException;
        // import com.oracle.coherence.common.base.NonBlocking;
        
        NamedCache cache;
        if (NonBlocking.isNonBlockingCaller())
            {
            cache = getInternalNamedCache();
            if (cache == null)
                {
                throw new ServiceStoppedException("Service has been terminated");
                }
            }
        else
            {
            cache = null;
            }
        
        return cache;
        }
    
    /**
     * Check whether or not the specified aggregator requires non-blocking
    * processing and if so, return an underlying NamedCache reference that is
    * guaranteed to execute without blocking. Otherwise return null.
     */
    protected com.tangosol.net.NamedCache getNonblockingCache(com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        // import com.tangosol.util.AsynchronousAgent;
        
        return agent instanceof AsynchronousAgent
            ? getNonblockingCache()
            : null;
        }
    
    /**
     * Check whether or not the specified processor requires non-blocking
    * processing and if so, return an underlying NamedCache reference that is
    * guaranteed to execute without blocking. Otherwise return null.
     */
    protected com.tangosol.net.NamedCache getNonblockingCache(com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.util.AsynchronousAgent;
        
        return agent instanceof AsynchronousAgent
            ? getNonblockingCache()
            : null;
        }
    
    // Accessor for the property "RestartCacheAction"
    /**
     * Getter for property RestartCacheAction.<p>
    * PrivilegedAction to call ensureCache.
     */
    public java.security.PrivilegedAction getRestartCacheAction()
        {
        return __m_RestartCacheAction;
        }
    
    protected com.tangosol.net.cache.BinaryEntryStore getRunningBinaryEntryStore()
        {
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.cache.BinaryEntryStore;
        
        NamedCache cache = getRunningNamedCache();
        try
            {
            return (BinaryEntryStore) cache;
            }
        catch (ClassCastException e)
            {
            throw new UnsupportedOperationException();
            }
        }
    
    protected com.tangosol.net.cache.CacheLoader getRunningCacheLoader()
        {
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.cache.CacheLoader;
        
        NamedCache cache = getRunningNamedCache();
        try
            {
            return (CacheLoader) cache;
            }
        catch (ClassCastException e)
            {
            throw new UnsupportedOperationException();
            }
        }
    
    protected com.tangosol.net.cache.CacheStore getRunningCacheStore()
        {
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.cache.CacheStore;
        
        NamedCache cache = getRunningNamedCache();
        try
            {
            return (CacheStore) cache;
            }
        catch (ClassCastException e)
            {
            throw new UnsupportedOperationException();
            }
        }
    
    // Accessor for the property "RunningNamedCache"
    /**
     * Getter for property RunningNamedCache.<p>
    * Calculated property returning a running NamedCache.
    * 
    * The only reason we have "getRunningNamedCache" in addition to
    * "ensureRunningNamedCache" is that RunningNamedCache property is used by
    * the integrator.
     */
    protected com.tangosol.net.NamedCache getRunningNamedCache()
        {
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.security.DoAsAction;
        // import java.security.AccessController;
        
        if (System.getSecurityManager() == null)
            {
            return ensureRunningNamedCache();
            }
        
        return (NamedCache) AccessController.doPrivileged(
            new DoAsAction(getEnsureCacheAction()));
        }
    
    // Accessor for the property "SafeAsyncNamedCache"
    /**
     * Getter for property SafeAsyncNamedCache.<p>
     */
    public SafeAsyncNamedCache getSafeAsyncNamedCache()
        {
        return __m_SafeAsyncNamedCache;
        }
    
    // Accessor for the property "SafeCacheService"
    /**
     * Getter for property SafeCacheService.<p>
     */
    public com.tangosol.coherence.component.util.safeService.SafeCacheService getSafeCacheService()
        {
        return __m_SafeCacheService;
        }
    
    // Accessor for the property "Subject"
    /**
     * Getter for property Subject.<p>
    * The optional Subject associated with the cache.
     */
    public javax.security.auth.Subject getSubject()
        {
        return __m_Subject;
        }
    
    // Accessor for the property "TloListenerVersions"
    /**
     * Getter for property TloListenerVersions.<p>
     */
    public ThreadLocal getTloListenerVersions()
        {
        return __m_TloListenerVersions;
        }
    
    // Accessor for the property "Unlockable"
    /**
     * Getter for property Unlockable.<p>
    * AutoCloseable to release aquired lock via exclusively().
     */
    public SafeNamedCache.Unlockable getUnlockable()
        {
        return __m_Unlockable;
        }
    
    // From interface: com.tangosol.net.partition.VersionAwareMapListener
    public com.tangosol.net.partition.VersionedPartitions getVersions()
        {
        // import com.tangosol.net.partition.VersionedPartitions;
        
        return (VersionedPartitions) getTloListenerVersions().get();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean isActive()
        {
        try
            {
            return getInternalNamedCache().isActive();
            }
        catch (RuntimeException e)
            {
            return false;
            }
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean isReady()
        {
        try
            {
            return getInternalNamedCache().isReady();
            }
        catch (UnsupportedOperationException uoe)
            {
            throw uoe;
            }
        catch (RuntimeException e)
            {
            return false;
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "Destroyed"
    /**
     * Getter for property Destroyed.<p>
    * Specifies whether or not the underlying NamedCache has been explicitly
    * destroyed.
     */
    public boolean isDestroyed()
        {
        try
            {
            return __m_Destroyed || getInternalNamedCache().isDestroyed();
            }
        catch (RuntimeException e)
            {
            // no way to compute, so return false
            return false;
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "Released"
    /**
     * Getter for property Released.<p>
    * Specifies whether or not the underlying NamedCache has been explicitly
    * released.
     */
    public boolean isReleased()
        {
        try
            {
            return __m_Released || getInternalNamedCache().isReleased();
            }
        catch (RuntimeException e)
            {
            // no way to compute, so return false
            return false;
            }
        }
    
    // Accessor for the property "Started"
    /**
     * Getter for property Started.<p>
    * 
    * @volatile
     */
    public boolean isStarted()
        {
        return __m_Started;
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void load(com.tangosol.util.BinaryEntry binEntry)
        {
        getRunningBinaryEntryStore().load(binEntry);
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public Object load(Object oKey)
        {
        return getRunningCacheLoader().load(oKey);
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public java.util.Map loadAll(java.util.Collection colKeys)
        {
        return getRunningCacheLoader().loadAll(colKeys);
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void loadAll(java.util.Set setBinEntries)
        {
        getRunningBinaryEntryStore().loadAll(setBinEntries);
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
        setEnsureCacheAction((SafeNamedCache.EnsureCacheAction) _newChild("EnsureCacheAction"));
        setRestartCacheAction((SafeNamedCache.RestartCacheAction) _newChild("RestartCacheAction"));
        setUnlockable((SafeNamedCache.Unlockable) _newChild("Unlockable"));
        
        super.onInit();
        }
    
    /**
     * Check whether or not the specified extractor is a lamda and wrap if
    * necessary.
     */
    protected com.tangosol.util.ValueExtractor prepareExtractor(com.tangosol.util.ValueExtractor extractor)
        {
        // import com.tangosol.internal.util.invoke.Lambdas;
        // import com.tangosol.util.ValueExtractor;
        
        return (ValueExtractor) Lambdas.ensureRemotable(extractor);
        }
    
    protected void registerListeners(com.tangosol.net.NamedCache cache)
        {
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        Filter[] aFilter = new Filter[0];
        Object[] aoKey   = new Object[0];
        com.tangosol.util.MapListenerSupport  support = getListenerSupport();
        synchronized (support)
            {
            if (!support.isEmpty())
                {
                aFilter = (Filter[]) support.getFilterSet().toArray(aFilter);
                aoKey   = support.getKeySet().toArray();
                }
            }
        
        for (int i = 0, c = aFilter.length; i < c; i++)
            {
            Filter filter = aFilter[i];
        
            getTloListenerVersions().set(support.getMinVersions(filter));
        
            cache.addMapListener(this, filter,
                !support.containsStandardListeners(filter));
        
            getTloListenerVersions().remove();
            }
        
        for (int i = 0, c = aoKey.length; i < c; i++)
            {
            Object oKey = aoKey[i];
        
            getTloListenerVersions().set(support.getMinVersions(oKey));
        
            cache.addMapListener(this, oKey,
                !support.containsStandardListeners(oKey));
        
            getTloListenerVersions().remove();
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void release()
        {
        // import Component.Util.SafeService.SafeCacheService;
        
        SafeCacheService safeservice = getSafeCacheService();
        SafeCluster      safecluster = safeservice.getSafeCluster();
        
        releaseListeners();
        
        safeservice.releaseCache(this);
        
        ensureGlobalLock();
        try
            {
            setReleased(true);
            setClassLoader(null);
            setInternalNamedCache(null);
            }
        finally
            {
            unlockGlobal();
            }
        }
    
    protected void releaseListeners()
        {
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import java.util.Iterator;
        // import java.util.LinkedList;
        // import java.util.List;
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (!support.isEmpty())
            {
            List listFilter = new LinkedList();
            List listKeys   = new LinkedList();
        
            synchronized (support)
                {
                if (!support.isEmpty())
                    {
                    listFilter.addAll(support.getFilterSet());
                    listKeys.addAll(support.getKeySet());
                    support.clear();
                    }
                }
        
            for (Iterator iter = listFilter.iterator(); iter.hasNext();)
                {
                removeMapListener(this, (Filter) iter.next());
                }
            for (Iterator iter = listKeys.iterator(); iter.hasNext();)
                {
                removeMapListener(this, (Object) iter.next());
                }
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;
        
        removeMapListener(listener, (Filter) null);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
        {
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.util.MapListener;
        // import com.tangosol.util.MapListenerSupport;
        // import com.tangosol.util.MapListenerSupport$SynchronousListener as com.tangosol.util.MapListenerSupport.SynchronousListener;
        // import com.tangosol.util.MapTriggerListener;
        
        if (listener == this || listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener ||
                                listener instanceof MapTriggerListener)
            {
            NamedCache cache = getInternalNamedCache();
            try
                {
                cache.removeMapListener(listener, filter);
                }
            catch (RuntimeException e)
                {
                if (cache != null && cache.isActive() &&
                        cache.getCacheService().isRunning())
                    {
                    throw e;
                    }
                // NamedCache has been invalidated
                }
            }
        else if (listener != null)
            {
            MapListenerSupport support = getListenerSupport();
        
            boolean fUnregister = support.removeListenerWithCheck(listener, filter);
            
            if (fUnregister)
                {
                removeMapListener(this, filter);
                }
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
        {
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.util.MapListener;
        // import com.tangosol.util.MapListenerSupport;
        // import com.tangosol.util.MapListenerSupport$SynchronousListener as com.tangosol.util.MapListenerSupport.SynchronousListener;
        
        if (listener == this || listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener)
            {
            NamedCache cache = getInternalNamedCache();
            try
                {
                cache.removeMapListener(listener, oKey);
                }
            catch (RuntimeException e)
                {
                if (cache != null && cache.isActive() &&
                        cache.getCacheService().isRunning())
                    {
                    throw e;
                    }
                // NamedCache has been invalidated
                }
            }
        else if (listener != null)
            {
            MapListenerSupport support = getListenerSupport();
        
            boolean fUnregister = support.removeListenerWithCheck(listener, oKey);
            
            if (fUnregister)
                {
                removeMapListener(this, oKey);
                }
            }
        }
    
    protected com.tangosol.net.NamedCache restartNamedCache()
        {
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.NamedCache;
        // import javax.security.auth.Subject;
        
        Subject subject = getSubject();
        
        // In case the underlying cache is scoped by Subject, use the original Subject
        NamedCache cache;
        
        if (subject == null)
            {
            CacheService service = (CacheService) getSafeCacheService().getRunningService();
            cache = service.ensureCache(getCacheName(), getClassLoader());
            }
        else
            {
            cache = (NamedCache) Subject.doAs(subject, getRestartCacheAction());
            }
        
        return cache;
        }
    
    // Accessor for the property "CacheName"
    /**
     * Setter for property CacheName.<p>
     */
    public void setCacheName(String sName)
        {
        __m_CacheName = sName;
        }
    
    // Accessor for the property "ClassLoader"
    /**
     * Setter for property ClassLoader.<p>
     */
    public void setClassLoader(ClassLoader loader)
        {
        __m_ClassLoader = loader;
        }
    
    // From interface: com.tangosol.io.ClassLoaderAware
    public void setContextClassLoader(ClassLoader loader)
        {
        throw new UnsupportedOperationException();
        }
    
    // Accessor for the property "Destroyed"
    /**
     * Setter for property Destroyed.<p>
    * Specifies whether or not the underlying NamedCache has been explicitly
    * destroyed.
     */
    private void setDestroyed(boolean fDestroyed)
        {
        __m_Destroyed = fDestroyed;
        }
    
    // Accessor for the property "EnsureCacheAction"
    /**
     * Setter for property EnsureCacheAction.<p>
    * PrivilegedAction to call ensureRunningNamedService.
     */
    protected void setEnsureCacheAction(java.security.PrivilegedAction action)
        {
        __m_EnsureCacheAction = action;
        }
    
    // Accessor for the property "InternalNamedCache"
    /**
     * Setter for property InternalNamedCache.<p>
    * Actual (wrapped) NamedCache.
    * 
    * @volatile
     */
    public void setInternalNamedCache(com.tangosol.net.NamedCache cache)
        {
        if (cache == null)
            {
            setStarted(false);
            }
        __m_InternalNamedCache = (cache);
        }
    
    // Accessor for the property "ListenerSupport"
    /**
     * Setter for property ListenerSupport.<p>
     */
    protected void setListenerSupport(com.tangosol.util.MapListenerSupport support)
        {
        __m_ListenerSupport = support;
        }
    
    // Accessor for the property "Lock"
    /**
     * Setter for property Lock.<p>
    * Lock used to protect this SafeNamedCache instance against multi-threaded
    * usage. 
     */
    public void setLock(java.util.concurrent.locks.ReentrantLock lock)
        {
        __m_Lock = lock;
        }
    
    // Accessor for the property "Released"
    /**
     * Setter for property Released.<p>
    * Specifies whether or not the underlying NamedCache has been explicitly
    * released.
     */
    public void setReleased(boolean fRelease)
        {
        ensureLocked();
        try
            {
            if (isReleased() && !fRelease)
                {
                throw new IllegalStateException("Cache cannot be un-released");
                }
        
            __m_Released = (fRelease);
            }
        finally
            {
            getLock().unlock();
            }
        }
    
    // Accessor for the property "RestartCacheAction"
    /**
     * Setter for property RestartCacheAction.<p>
    * PrivilegedAction to call ensureCache.
     */
    protected void setRestartCacheAction(java.security.PrivilegedAction action)
        {
        __m_RestartCacheAction = action;
        }
    
    // Accessor for the property "SafeAsyncNamedCache"
    /**
     * Setter for property SafeAsyncNamedCache.<p>
     */
    public void setSafeAsyncNamedCache(SafeAsyncNamedCache sProperty)
        {
        __m_SafeAsyncNamedCache = sProperty;
        }
    
    // Accessor for the property "SafeCacheService"
    /**
     * Setter for property SafeCacheService.<p>
     */
    public void setSafeCacheService(com.tangosol.coherence.component.util.safeService.SafeCacheService service)
        {
        __m_SafeCacheService = service;
        }
    
    // Accessor for the property "Started"
    /**
     * Setter for property Started.<p>
    * 
    * @volatile
     */
    public void setStarted(boolean fStarted)
        {
        __m_Started = fStarted;
        }
    
    // Accessor for the property "Subject"
    /**
     * Setter for property Subject.<p>
    * The optional Subject associated with the cache.
     */
    public void setSubject(javax.security.auth.Subject subject)
        {
        __m_Subject = subject;
        }
    
    // Accessor for the property "TloListenerVersions"
    /**
     * Setter for property TloListenerVersions.<p>
     */
    public void setTloListenerVersions(ThreadLocal localVersions)
        {
        __m_TloListenerVersions = localVersions;
        }
    
    // Accessor for the property "Unlockable"
    /**
     * Setter for property Unlockable.<p>
    * AutoCloseable to release aquired lock via exclusively().
     */
    public void setUnlockable(SafeNamedCache.Unlockable unlockable)
        {
        __m_Unlockable = unlockable;
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void store(com.tangosol.util.BinaryEntry binEntry)
        {
        getRunningBinaryEntryStore().store(binEntry);
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public void store(Object oKey, Object oValue)
        {
        getRunningCacheStore().store(oKey, oValue);
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public void storeAll(java.util.Map mapEntries)
        {
        getRunningCacheStore().storeAll(mapEntries);
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void storeAll(java.util.Set setBinEntries)
        {
        getRunningBinaryEntryStore().storeAll(setBinEntries);
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + ": " + getInternalNamedCache();
        }
    
    protected void translateMapEvent(com.tangosol.util.MapEvent evt)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        if (evt.getSource() == getInternalNamedCache())
            {
            // ensure lazy event data access
            evt = com.tangosol.util.MapListenerSupport.convertEvent(evt, this, null, null);
            getListenerSupport().fireEvent(evt, true);
            }
        else
            {
            _trace("Ah ha!\nevent.source: " + evt.getSource() + "\nInternalCache: " + getInternalNamedCache(), 3);
            }
        }
    
    public void unlock()
        {
        getLock().unlock();
        }
    
    /**
     * Unlock SafeNamedCache, SafeService and SafeCluster locks.
     */
    public void unlockGlobal()
        {
        unlock();
        getSafeCacheService().unlockGlobal();
        }

    // ---- class: com.tangosol.coherence.component.util.SafeNamedCache$EnsureCacheAction
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EnsureCacheAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public EnsureCacheAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public EnsureCacheAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.SafeNamedCache.EnsureCacheAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeNamedCache$EnsureCacheAction".replace('/', '.'));
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
            return ((SafeNamedCache) get_Module()).ensureRunningNamedCache();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.SafeNamedCache$RestartCacheAction
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class RestartCacheAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public RestartCacheAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public RestartCacheAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.SafeNamedCache.RestartCacheAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeNamedCache$RestartCacheAction".replace('/', '.'));
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
            // import com.tangosol.net.CacheService;
            
            SafeNamedCache      cacheSafe = (SafeNamedCache) get_Module();
            CacheService service   = (CacheService) cacheSafe.getSafeCacheService().getRunningService();
            
            return service.ensureCache(cacheSafe.getCacheName(), cacheSafe.getClassLoader());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.SafeNamedCache$Unlockable
    
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
            return new com.tangosol.coherence.component.util.SafeNamedCache.Unlockable();
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
                clz = Class.forName("com.tangosol.coherence/component/util/SafeNamedCache$Unlockable".replace('/', '.'));
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
            ((SafeNamedCache) get_Module()).unlock();
            }
        }
    }
