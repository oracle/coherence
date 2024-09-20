
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.SafeAsyncNamedCache

package com.tangosol.coherence.component.util;

import com.oracle.coherence.common.base.Timeout;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.ServiceDependencies;
import com.tangosol.util.Base;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/*
* Integrates
*     com.tangosol.net.AsyncNamedCache
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SafeAsyncNamedCache
        extends    com.tangosol.coherence.component.Util
        implements com.oracle.coherence.common.base.Lockable,
                   com.tangosol.io.ClassLoaderAware,
                   com.tangosol.net.AsyncNamedCache
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
     * Property InternalNamedCache
     *
     */
    private com.tangosol.net.AsyncNamedCache __m_InternalNamedCache;
    
    /**
     * Property Lock
     *
     */
    private java.util.concurrent.locks.ReentrantLock __m_Lock;
    
    /**
     * Property Options
     *
     */
    private com.tangosol.net.AsyncNamedMap.Option[] __m_Options;
    
    /**
     * Property SafeCacheService
     *
     */
    private com.tangosol.coherence.component.util.safeService.SafeCacheService __m_SafeCacheService;
    
    /**
     * Property SafeNamedCache
     *
     */
    private SafeNamedCache __m_SafeNamedCache;
    
    /**
     * Property Started
     *
     */
    private boolean __m_Started;
    
    // Default constructor
    public SafeAsyncNamedCache()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SafeAsyncNamedCache(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.SafeAsyncNamedCache();
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
            clz = Class.forName("com.tangosol.coherence/component/util/SafeAsyncNamedCache".replace('/', '.'));
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
    
    //++ com.tangosol.net.AsyncNamedCache integration
    // Access optimization
    // properties integration
    // methods integration
    public java.util.concurrent.CompletableFuture aggregate(com.tangosol.util.Filter Param_1, com.tangosol.util.InvocableMap.EntryAggregator Param_2)
        {
        return getRunningNamedCache().aggregate(Param_1, Param_2);
        }
    public java.util.concurrent.CompletableFuture aggregate(com.tangosol.util.InvocableMap.EntryAggregator aggregator)
        {
        return getRunningNamedCache().aggregate(aggregator);
        }
    public java.util.concurrent.CompletableFuture aggregate(java.util.Collection Param_1, com.tangosol.util.InvocableMap.EntryAggregator Param_2)
        {
        return getRunningNamedCache().aggregate(Param_1, Param_2);
        }
    public java.util.concurrent.CompletableFuture clear()
        {
        return getRunningNamedCache().clear();
        }
    public java.util.concurrent.CompletableFuture compute(Object key, com.tangosol.util.function.Remote.BiFunction remappingFunction)
        {
        return getRunningNamedCache().compute(key, remappingFunction);
        }
    public java.util.concurrent.CompletableFuture computeIfAbsent(Object key, com.tangosol.util.function.Remote.Function mappingFunction)
        {
        return getRunningNamedCache().computeIfAbsent(key, mappingFunction);
        }
    public java.util.concurrent.CompletableFuture computeIfPresent(Object key, com.tangosol.util.function.Remote.BiFunction remappingFunction)
        {
        return getRunningNamedCache().computeIfPresent(key, remappingFunction);
        }
    public java.util.concurrent.CompletableFuture containsKey(Object key)
        {
        return getRunningNamedCache().containsKey(key);
        }
    public java.util.concurrent.CompletableFuture entrySet()
        {
        return getRunningNamedCache().entrySet();
        }
    public java.util.concurrent.CompletableFuture entrySet(com.tangosol.util.Filter filter)
        {
        return getRunningNamedCache().entrySet(filter);
        }
    public java.util.concurrent.CompletableFuture entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        return getRunningNamedCache().entrySet(filter, comparator);
        }
    public java.util.concurrent.CompletableFuture entrySet(com.tangosol.util.Filter filter, java.util.function.BiConsumer callback)
        {
        return getRunningNamedCache().entrySet(filter, callback);
        }
    public java.util.concurrent.CompletableFuture entrySet(com.tangosol.util.Filter filter, java.util.function.Consumer callback)
        {
        return getRunningNamedCache().entrySet(filter, callback);
        }
    public java.util.concurrent.CompletableFuture entrySet(java.util.function.BiConsumer callback)
        {
        return getRunningNamedCache().entrySet(callback);
        }
    public java.util.concurrent.CompletableFuture entrySet(java.util.function.Consumer callback)
        {
        return getRunningNamedCache().entrySet(callback);
        }
    public java.util.concurrent.CompletableFuture get(Object key)
        {
        return getRunningNamedCache().get(key);
        }
    public java.util.concurrent.CompletableFuture getAll(com.tangosol.util.Filter filter)
        {
        return getRunningNamedCache().getAll(filter);
        }
    public java.util.concurrent.CompletableFuture getAll(java.util.Collection colKeys)
        {
        return getRunningNamedCache().getAll(colKeys);
        }
    public java.util.concurrent.CompletableFuture getAll(java.util.Collection colKeys, java.util.function.BiConsumer callback)
        {
        return getRunningNamedCache().getAll(colKeys, callback);
        }
    public java.util.concurrent.CompletableFuture getAll(java.util.Collection colKeys, java.util.function.Consumer callback)
        {
        return getRunningNamedCache().getAll(colKeys, callback);
        }
    public java.util.concurrent.CompletableFuture getOrDefault(Object key, Object valueDefault)
        {
        return getRunningNamedCache().getOrDefault(key, valueDefault);
        }
    public java.util.concurrent.CompletableFuture invoke(Object Param_1, com.tangosol.util.InvocableMap.EntryProcessor Param_2)
        {
        return getRunningNamedCache().invoke(Param_1, Param_2);
        }
    public java.util.concurrent.CompletableFuture invokeAll(com.tangosol.util.Filter Param_1, com.tangosol.util.InvocableMap.EntryProcessor Param_2)
        {
        return getRunningNamedCache().invokeAll(Param_1, Param_2);
        }
    public java.util.concurrent.CompletableFuture invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor processor, java.util.function.BiConsumer callback)
        {
        return getRunningNamedCache().invokeAll(filter, processor, callback);
        }
    public java.util.concurrent.CompletableFuture invokeAll(com.tangosol.util.Filter Param_1, com.tangosol.util.InvocableMap.EntryProcessor Param_2, java.util.function.Consumer Param_3)
        {
        return getRunningNamedCache().invokeAll(Param_1, Param_2, Param_3);
        }
    public java.util.concurrent.CompletableFuture invokeAll(com.tangosol.util.InvocableMap.EntryProcessor processor)
        {
        return getRunningNamedCache().invokeAll(processor);
        }
    public java.util.concurrent.CompletableFuture invokeAll(com.tangosol.util.InvocableMap.EntryProcessor processor, java.util.function.BiConsumer callback)
        {
        return getRunningNamedCache().invokeAll(processor, callback);
        }
    public java.util.concurrent.CompletableFuture invokeAll(com.tangosol.util.InvocableMap.EntryProcessor processor, java.util.function.Consumer callback)
        {
        return getRunningNamedCache().invokeAll(processor, callback);
        }
    public java.util.concurrent.CompletableFuture invokeAll(java.util.Collection Param_1, com.tangosol.util.InvocableMap.EntryProcessor Param_2)
        {
        return getRunningNamedCache().invokeAll(Param_1, Param_2);
        }
    public java.util.concurrent.CompletableFuture invokeAll(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryProcessor processor, java.util.function.BiConsumer callback)
        {
        return getRunningNamedCache().invokeAll(collKeys, processor, callback);
        }
    public java.util.concurrent.CompletableFuture invokeAll(java.util.Collection Param_1, com.tangosol.util.InvocableMap.EntryProcessor Param_2, java.util.function.Consumer Param_3)
        {
        return getRunningNamedCache().invokeAll(Param_1, Param_2, Param_3);
        }
    public java.util.concurrent.CompletableFuture isEmpty()
        {
        return getRunningNamedCache().isEmpty();
        }
    public java.util.concurrent.CompletableFuture keySet()
        {
        return getRunningNamedCache().keySet();
        }
    public java.util.concurrent.CompletableFuture keySet(com.tangosol.util.Filter filter)
        {
        return getRunningNamedCache().keySet(filter);
        }
    public java.util.concurrent.CompletableFuture keySet(com.tangosol.util.Filter filter, java.util.function.Consumer callback)
        {
        return getRunningNamedCache().keySet(filter, callback);
        }
    public java.util.concurrent.CompletableFuture keySet(java.util.function.Consumer callback)
        {
        return getRunningNamedCache().keySet(callback);
        }
    public java.util.concurrent.CompletableFuture merge(Object key, Object value, com.tangosol.util.function.Remote.BiFunction remappingFunction)
        {
        return getRunningNamedCache().merge(key, value, remappingFunction);
        }
    public java.util.concurrent.CompletableFuture put(Object key, Object value)
        {
        return getRunningNamedCache().put(key, value);
        }
    public java.util.concurrent.CompletableFuture put(Object key, Object value, long cMillis)
        {
        return getRunningNamedCache().put(key, value, cMillis);
        }
    public java.util.concurrent.CompletableFuture putAll(java.util.Map map)
        {
        return getRunningNamedCache().putAll(map);
        }

    public CompletableFuture putAll(Map map, long cMillis)
        {
        return getRunningNamedCache().putAll(map, cMillis);
        }

    public java.util.concurrent.CompletableFuture putIfAbsent(Object key, Object value)
        {
        return getRunningNamedCache().putIfAbsent(key, value);
        }
    public java.util.concurrent.CompletableFuture remove(Object key)
        {
        return getRunningNamedCache().remove(key);
        }
    public java.util.concurrent.CompletableFuture remove(Object key, Object value)
        {
        return getRunningNamedCache().remove(key, value);
        }
    public java.util.concurrent.CompletableFuture removeAll(com.tangosol.util.Filter filter)
        {
        return getRunningNamedCache().removeAll(filter);
        }
    public java.util.concurrent.CompletableFuture removeAll(java.util.Collection colKeys)
        {
        return getRunningNamedCache().removeAll(colKeys);
        }
    public java.util.concurrent.CompletableFuture replace(Object key, Object value)
        {
        return getRunningNamedCache().replace(key, value);
        }
    public java.util.concurrent.CompletableFuture replace(Object key, Object oldValue, Object newValue)
        {
        return getRunningNamedCache().replace(key, oldValue, newValue);
        }
    public java.util.concurrent.CompletableFuture replaceAll(com.tangosol.util.Filter filter, com.tangosol.util.function.Remote.BiFunction function)
        {
        return getRunningNamedCache().replaceAll(filter, function);
        }
    public java.util.concurrent.CompletableFuture replaceAll(com.tangosol.util.function.Remote.BiFunction function)
        {
        return getRunningNamedCache().replaceAll(function);
        }
    public java.util.concurrent.CompletableFuture replaceAll(java.util.Collection collKeys, com.tangosol.util.function.Remote.BiFunction function)
        {
        return getRunningNamedCache().replaceAll(collKeys, function);
        }
    public java.util.concurrent.CompletableFuture size()
        {
        return getRunningNamedCache().size();
        }
    public java.util.concurrent.CompletableFuture values()
        {
        return getRunningNamedCache().values();
        }
    public java.util.concurrent.CompletableFuture values(com.tangosol.util.Filter filter)
        {
        return getRunningNamedCache().values(filter);
        }
    public java.util.concurrent.CompletableFuture values(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        return getRunningNamedCache().values(filter, comparator);
        }
    public java.util.concurrent.CompletableFuture values(com.tangosol.util.Filter filter, java.util.function.Consumer callback)
        {
        return getRunningNamedCache().values(filter, callback);
        }
    public java.util.concurrent.CompletableFuture values(java.util.function.Consumer callback)
        {
        return getRunningNamedCache().values(callback);
        }
    //-- com.tangosol.net.AsyncNamedCache integration
    
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
                    new RequestTimeoutException("Failed to acquire AsyncNamedCache lock in " + cTimeout + "ms"));
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw Base.ensureRuntimeException(e, "Interrupted while attempting to acquire NamedCache lock"); 
            }
        }
    
    public com.tangosol.net.AsyncNamedCache ensureRunningNamedCache()
        {
        // import com.tangosol.net.AsyncNamedCache;
        // import com.tangosol.net.NamedCache;
        
        AsyncNamedCache asyncCache  = getInternalNamedCache();
        NamedCache      cache       = asyncCache.getNamedCache();
        SafeService     serviceSafe = getSafeCacheService();
        
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
                asyncCache  = getInternalNamedCache();
                cache       = asyncCache.getNamedCache();
                serviceSafe = getSafeCacheService();
                if (serviceSafe == null || !serviceSafe.isRunning() ||
                    cache == null || !cache.isActive() || !isStarted())
                    {
                    if (cache.isReleased() || cache.isDestroyed())
                        {
                        String reason = cache.isDestroyed() ? "destroyed" : "released";
                        throw new IllegalStateException("SafeAsyncNamedCache was explicitly " + reason);
                        }
                    else
                        {
                        // restart the actual named cache
                        if (cache != null)
                            {
                            setInternalNamedCache(null);
                            _trace("Restarting AsyncNamedCache: " + getCacheName(), 3);
                            }
        
                        SafeNamedCache cacheSafe = getSafeNamedCache();
                        setInternalNamedCache(asyncCache = cacheSafe.ensureRunningNamedCache().async(getOptions()));
        
                        setStarted(true);
                        }
                    }
                }
            finally
                {
                unlockGlobal();
                }
            }
        
        return asyncCache;
        }
    
    // Accessor for the property "AsyncNamedCache"
    /**
     * Getter for property AsyncNamedCache.<p>
     */
    public com.tangosol.net.AsyncNamedCache getAsyncNamedCache()
        {
        return getInternalNamedCache();
        }
    
    // Accessor for the property "CacheName"
    /**
     * Getter for property CacheName.<p>
     */
    public String getCacheName()
        {
        return __m_CacheName;
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
        return null;
        }
    
    // Accessor for the property "InternalNamedCache"
    /**
     * Getter for property InternalNamedCache.<p>
     */
    public com.tangosol.net.AsyncNamedCache getInternalNamedCache()
        {
        return __m_InternalNamedCache;
        }
    
    // Accessor for the property "Lock"
    /**
     * Getter for property Lock.<p>
     */
    public java.util.concurrent.locks.ReentrantLock getLock()
        {
        return __m_Lock;
        }
    
    // From interface: com.tangosol.net.AsyncNamedCache
    public com.tangosol.net.NamedCache getNamedCache()
        {
        return getSafeNamedCache();
        }
    
    // From interface: com.tangosol.net.AsyncNamedCache
    public com.tangosol.net.NamedMap getNamedMap()
        {
        return getSafeNamedCache();
        }
    
    // Accessor for the property "Options"
    /**
     * Getter for property Options.<p>
     */
    public com.tangosol.net.AsyncNamedMap.Option[] getOptions()
        {
        return __m_Options;
        }
    
    // Accessor for the property "Options"
    /**
     * Getter for property Options.<p>
     */
    public com.tangosol.net.AsyncNamedMap.Option getOptions(int i)
        {
        return getOptions()[i];
        }
    
    // Accessor for the property "RunningNamedCache"
    /**
     * Getter for property RunningNamedCache.<p>
     */
    public com.tangosol.net.AsyncNamedCache getRunningNamedCache()
        {
        return ensureRunningNamedCache();
        }
    
    // Accessor for the property "SafeCacheService"
    /**
     * Getter for property SafeCacheService.<p>
     */
    public com.tangosol.coherence.component.util.safeService.SafeCacheService getSafeCacheService()
        {
        return __m_SafeCacheService;
        }
    
    // Accessor for the property "SafeNamedCache"
    /**
     * Getter for property SafeNamedCache.<p>
     */
    public SafeNamedCache getSafeNamedCache()
        {
        return __m_SafeNamedCache;
        }
    
    // Accessor for the property "Started"
    /**
     * Getter for property Started.<p>
     */
    public boolean isStarted()
        {
        return __m_Started;
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
    public void setClassLoader(ClassLoader loaderClass)
        {
        __m_ClassLoader = loaderClass;
        }
    
    // From interface: com.tangosol.io.ClassLoaderAware
    public void setContextClassLoader(ClassLoader Param_1)
        {
        }
    
    // Accessor for the property "InternalNamedCache"
    /**
     * Setter for property InternalNamedCache.<p>
     */
    public void setInternalNamedCache(com.tangosol.net.AsyncNamedCache cache)
        {
        if (cache == null)
            {
            setStarted(false);
            }
        __m_InternalNamedCache = (cache);
        }
    
    // Accessor for the property "Lock"
    /**
     * Setter for property Lock.<p>
     */
    public void setLock(java.util.concurrent.locks.ReentrantLock lock)
        {
        __m_Lock = lock;
        }
    
    // Accessor for the property "Options"
    /**
     * Setter for property Options.<p>
     */
    public void setOptions(com.tangosol.net.AsyncNamedMap.Option[] sProperty)
        {
        __m_Options = sProperty;
        }
    
    // Accessor for the property "Options"
    /**
     * Setter for property Options.<p>
     */
    public void setOptions(int i, com.tangosol.net.AsyncNamedMap.Option optionOptions)
        {
        getOptions()[i] = optionOptions;
        }
    
    // Accessor for the property "SafeCacheService"
    /**
     * Setter for property SafeCacheService.<p>
     */
    public void setSafeCacheService(com.tangosol.coherence.component.util.safeService.SafeCacheService serviceCache)
        {
        __m_SafeCacheService = serviceCache;
        }
    
    // Accessor for the property "SafeNamedCache"
    /**
     * Setter for property SafeNamedCache.<p>
     */
    public void setSafeNamedCache(SafeNamedCache cacheNamed)
        {
        __m_SafeNamedCache = cacheNamed;
        }
    
    // Accessor for the property "Started"
    /**
     * Setter for property Started.<p>
     */
    public void setStarted(boolean fStarted)
        {
        __m_Started = fStarted;
        }
    
    public void unlock()
        {
        getLock().unlock();
        }
    
    public void unlockGlobal()
        {
        unlock();
        getSafeCacheService().unlockGlobal();
        }
    }
