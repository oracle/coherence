
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.safeService.SafeCacheService

package com.tangosol.coherence.component.util.safeService;

import com.tangosol.coherence.component.net.Security;
import com.tangosol.coherence.component.util.SafeNamedCache;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.security.SecurityHelper;
import java.security.AccessController;

/*
* Integrates
*     com.tangosol.net.CacheService
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SafeCacheService
        extends    com.tangosol.coherence.component.util.SafeService
        implements com.tangosol.net.CacheService
    {
    // ---- Fields declarations ----
    
    /**
     * Property BackingMapManager
     *
     */
    private transient com.tangosol.net.BackingMapManager __m_BackingMapManager;
    
    /**
     * Property ScopedCacheStore
     *
     * Store cache references with subject scoping if configured.
     */
    private com.tangosol.net.internal.ScopedCacheReferenceStore __m_ScopedCacheStore;
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
        __mapChildren.put("DestroyCacheAction", SafeCacheService.DestroyCacheAction.get_CLASS());
        __mapChildren.put("EnsureServiceAction", com.tangosol.coherence.component.util.SafeService.EnsureServiceAction.get_CLASS());
        __mapChildren.put("ReleaseCacheAction", SafeCacheService.ReleaseCacheAction.get_CLASS());
        __mapChildren.put("StartAction", com.tangosol.coherence.component.util.SafeService.StartAction.get_CLASS());
        __mapChildren.put("Unlockable", com.tangosol.coherence.component.util.SafeService.Unlockable.get_CLASS());
        }
    
    // Default constructor
    public SafeCacheService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SafeCacheService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setScopedCacheStore(new com.tangosol.net.internal.ScopedCacheReferenceStore());
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
        return new com.tangosol.coherence.component.util.safeService.SafeCacheService();
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
            clz = Class.forName("com.tangosol.coherence/component/util/safeService/SafeCacheService".replace('/', '.'));
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
    
    //++ com.tangosol.net.CacheService integration
    // Access optimization
    // properties integration
    // methods integration
    private com.tangosol.net.NamedCache ensureCache$Router(String sName, ClassLoader loader)
        {
        return getRunningCacheService().ensureCache(sName, loader);
        }
    public com.tangosol.net.NamedCache ensureCache(String sName, ClassLoader loader)
        {
        // import Component.Net.Security;
        // import Component.Util.SafeNamedCache;
        // import com.tangosol.net.security.SecurityHelper;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.internal.ScopedCacheReferenceStore as com.tangosol.net.internal.ScopedCacheReferenceStore;
        
        checkClientThread("ensureCache");
        
        if (sName == null || sName.length() == 0)
            {
            sName = "Default";
            }
        
        if (loader == null)
            {
            loader = getContextClassLoader();
            }
        
        Security.checkPermission(getSafeCluster(), getServiceName(), sName, "join");
        
        com.tangosol.net.internal.ScopedCacheReferenceStore     store     = getScopedCacheStore();
        SafeNamedCache cacheSafe = (SafeNamedCache) store.getCache(sName, loader);
        
        if (cacheSafe != null)
            {
            if (cacheSafe.isActive() || (!cacheSafe.isDestroyed() && !cacheSafe.isReleased()))
                {
                return cacheSafe;
                }
            else
                {
                // don't return a released/destroyed SafeNamedCache; allow a new one to be returned.
                cacheSafe = null;
                }
            }
        
        // ensure no released/destroyed cache refs in store
        store.clearInactiveCacheRefs();
        
        while (cacheSafe == null)
            {
            NamedCache cache = ensureCache$Router(sName, loader);
        
            cacheSafe = new SafeNamedCache();
            cacheSafe.setSubject(SecurityHelper.getCurrentSubject());
            cacheSafe.setSafeCacheService(this);
            cacheSafe.setCacheName(sName);
            cacheSafe.setClassLoader(loader);
            cacheSafe.setInternalNamedCache(cache);
            cacheSafe.setStarted(true);
        
            if (store.putCacheIfAbsent(cacheSafe, loader) == null)
                {
                break;
                }
        
            cacheSafe = (SafeNamedCache) store.getCache(sName, loader);
            }
        return cacheSafe;
        }
    public java.util.Enumeration getCacheNames()
        {
        return getRunningCacheService().getCacheNames();
        }
    //-- com.tangosol.net.CacheService integration
    
    // Declared at the super level
    protected void cleanup()
        {
        super.cleanup();
        
        setBackingMapManager(null);
        getScopedCacheStore().clear();
        }
    
    // From interface: com.tangosol.net.CacheService
    public void destroyCache(com.tangosol.net.NamedCache cache)
        {
        // import Component.Net.Security;
        // import Component.Util.SafeNamedCache;
        // import com.tangosol.net.CacheService;
        // import java.security.AccessController;
        
        Security.checkPermission(getSafeCluster(),
            getServiceName(), cache.getCacheName(), "destroy");
        
        SafeNamedCache cacheSafe = (SafeNamedCache) cache;
        
        removeCacheReference(cacheSafe);
        
        SafeCacheService.DestroyCacheAction action = (SafeCacheService.DestroyCacheAction) _newChild("DestroyCacheAction");
        action.setSafeNamedCache(cacheSafe);
        action.setCacheService((CacheService) getInternalService());
        
        AccessController.doPrivileged(action);
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "BackingMapManager"
    /**
     * Getter for property BackingMapManager.<p>
     */
    public com.tangosol.net.BackingMapManager getBackingMapManager()
        {
        return __m_BackingMapManager;
        }
    
    // Accessor for the property "RunningCacheService"
    /**
     * Getter for property RunningCacheService.<p>
    * Calculated property returning a running cache service.
     */
    public com.tangosol.net.CacheService getRunningCacheService()
        {
        // import com.tangosol.net.CacheService;
        
        return (CacheService) getRunningService();
        }
    
    // Accessor for the property "ScopedCacheStore"
    /**
     * Getter for property ScopedCacheStore.<p>
    * Store cache references with subject scoping if configured.
     */
    protected com.tangosol.net.internal.ScopedCacheReferenceStore getScopedCacheStore()
        {
        return __m_ScopedCacheStore;
        }
    
    // From interface: com.tangosol.net.CacheService
    public void releaseCache(com.tangosol.net.NamedCache cache)
        {
        // import Component.Util.SafeNamedCache;
        // import com.tangosol.net.CacheService;
        // import java.security.AccessController;
        
        SafeNamedCache cacheSafe = (SafeNamedCache) cache;
        
        removeCacheReference(cacheSafe);
        
        SafeCacheService.ReleaseCacheAction action = (SafeCacheService.ReleaseCacheAction) _newChild("ReleaseCacheAction");
        action.setSafeNamedCache(cacheSafe);
        action.setCacheService((CacheService) getInternalService());
        
        AccessController.doPrivileged(action);
        }
    
    protected void removeCacheReference(com.tangosol.coherence.component.util.SafeNamedCache cacheSafe)
        {
        cacheSafe.setReleased(true);
        getScopedCacheStore().releaseCache(cacheSafe);
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "BackingMapManager"
    /**
     * Setter for property BackingMapManager.<p>
     */
    public void setBackingMapManager(com.tangosol.net.BackingMapManager manager)
        {
        __m_BackingMapManager = manager;
        }
    
    // Accessor for the property "ScopedCacheStore"
    /**
     * Setter for property ScopedCacheStore.<p>
    * Store cache references with subject scoping if configured.
     */
    protected void setScopedCacheStore(com.tangosol.net.internal.ScopedCacheReferenceStore scopedStore)
        {
        __m_ScopedCacheStore = scopedStore;
        }
    
    // Declared at the super level
    protected void startService(com.tangosol.net.Service service)
        {
        // import com.tangosol.net.CacheService;
        
        ((CacheService) service).setBackingMapManager(getBackingMapManager());
        
        super.startService(service);
        }

    // ---- class: com.tangosol.coherence.component.util.safeService.SafeCacheService$DestroyCacheAction
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DestroyCacheAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property CacheService
         *
         */
        private com.tangosol.net.CacheService __m_CacheService;
        
        /**
         * Property SafeNamedCache
         *
         */
        private com.tangosol.coherence.component.util.SafeNamedCache __m_SafeNamedCache;
        
        // Default constructor
        public DestroyCacheAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DestroyCacheAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.safeService.SafeCacheService.DestroyCacheAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/safeService/SafeCacheService$DestroyCacheAction".replace('/', '.'));
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
        
        // Accessor for the property "CacheService"
        /**
         * Getter for property CacheService.<p>
         */
        public com.tangosol.net.CacheService getCacheService()
            {
            return __m_CacheService;
            }
        
        // Accessor for the property "SafeNamedCache"
        /**
         * Getter for property SafeNamedCache.<p>
         */
        public com.tangosol.coherence.component.util.SafeNamedCache getSafeNamedCache()
            {
            return __m_SafeNamedCache;
            }
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            // import com.tangosol.net.CacheService;
            // import com.tangosol.net.NamedCache;
            
            CacheService serviceInternal = getCacheService();
            NamedCache   cacheInternal   = getSafeNamedCache().getNamedCache();
            
            if (cacheInternal == null)
                {
                throw new IllegalStateException("Cache is already released");
                }
            
            try
                {
                serviceInternal.destroyCache(cacheInternal);
                }
            catch (RuntimeException e)
                {
                if (serviceInternal != null && serviceInternal.isRunning())
                    {
                    throw e;
                    }
                }
            
            return null;
            }
        
        // Accessor for the property "CacheService"
        /**
         * Setter for property CacheService.<p>
         */
        public void setCacheService(com.tangosol.net.CacheService service)
            {
            __m_CacheService = service;
            }
        
        // Accessor for the property "SafeNamedCache"
        /**
         * Setter for property SafeNamedCache.<p>
         */
        public void setSafeNamedCache(com.tangosol.coherence.component.util.SafeNamedCache cacheSafe)
            {
            __m_SafeNamedCache = cacheSafe;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.safeService.SafeCacheService$ReleaseCacheAction
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ReleaseCacheAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property CacheService
         *
         */
        private com.tangosol.net.CacheService __m_CacheService;
        
        /**
         * Property SafeNamedCache
         *
         */
        private com.tangosol.coherence.component.util.SafeNamedCache __m_SafeNamedCache;
        
        // Default constructor
        public ReleaseCacheAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ReleaseCacheAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.safeService.SafeCacheService.ReleaseCacheAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/safeService/SafeCacheService$ReleaseCacheAction".replace('/', '.'));
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
        
        // Accessor for the property "CacheService"
        /**
         * Getter for property CacheService.<p>
         */
        public com.tangosol.net.CacheService getCacheService()
            {
            return __m_CacheService;
            }
        
        // Accessor for the property "SafeNamedCache"
        /**
         * Getter for property SafeNamedCache.<p>
         */
        public com.tangosol.coherence.component.util.SafeNamedCache getSafeNamedCache()
            {
            return __m_SafeNamedCache;
            }
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            // import com.tangosol.net.CacheService;
            // import com.tangosol.net.NamedCache;
            
            CacheService serviceInternal = getCacheService();
            NamedCache   cacheInternal   = getSafeNamedCache().getNamedCache();
            
            if (cacheInternal == null)
                {
                throw new IllegalStateException("Cache is already released");
                }
            
            try
                {
                serviceInternal.releaseCache(cacheInternal);
                }
            catch (RuntimeException e)
                {
                if (serviceInternal != null && serviceInternal.isRunning())
                    {
                    throw e;
                    }
                }
            
            return null;
            }
        
        // Accessor for the property "CacheService"
        /**
         * Setter for property CacheService.<p>
         */
        public void setCacheService(com.tangosol.net.CacheService service)
            {
            __m_CacheService = service;
            }
        
        // Accessor for the property "SafeNamedCache"
        /**
         * Setter for property SafeNamedCache.<p>
         */
        public void setSafeNamedCache(com.tangosol.coherence.component.util.SafeNamedCache cacheSafe)
            {
            __m_SafeNamedCache = cacheSafe;
            }
        }
    }
