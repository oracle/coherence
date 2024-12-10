
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.LocalCache

package com.tangosol.coherence.component.util;

import com.tangosol.application.ContainerHelper;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.cache.CacheMap;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.IteratorEnumerator;
import com.tangosol.util.Listeners;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.ServiceEvent;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * Coherence Local implementation.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class LocalCache
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.net.CacheService,
                   com.tangosol.net.ServiceInfo
    {
    // ---- Fields declarations ----
    
    /**
     * Property BackingMapContext
     *
     * The BackingMapContext (lazily created) is used by the BackingMapManager
     * (if provided).
     */
    private transient LocalCache.BackingMapContext __m_BackingMapContext;
    
    /**
     * Property BackingMapManager
     *
     * Interface that provides the backing map storage implementations for the
     * cache.
     */
    private transient com.tangosol.net.BackingMapManager __m_BackingMapManager;
    
    /**
     * Property CacheHandlerMap
     *
     */
    private transient java.util.Map __m_CacheHandlerMap;
    
    /**
     * Property Cluster
     *
     */
    private transient com.tangosol.net.Cluster __m_Cluster;
    
    /**
     * Property ContextClassLoader
     *
     */
    private ClassLoader __m_ContextClassLoader;
    
    /**
     * Property LockingEnforced
     *
     * If true the locking is enforced for put, remove and clear operations;
     * otherwise a client is responsible for calling lock and unlock
     * explicitly. Configured by "lock-enforce" element.
     * 
     * @see configure;
     */
    private boolean __m_LockingEnforced;
    
    /**
     * Property LockWaitMillis
     *
     * If locking enforcement is required then this parameter speicifes the
     * number of milliseconds to continue trying to obtain a lock; -1 blocks
     * the calling thread until the lock could be obtained. Configured by
     * "lock-wait" element.
     * 
     * @see configure
     */
    private long __m_LockWaitMillis;
    
    /**
     * Property ResourceRegistry
     *
     * ResourceRegistry associated with this Service.
     */
    private com.tangosol.util.ResourceRegistry __m_ResourceRegistry;
    
    /**
     * Property Running
     *
     */
    private transient boolean __m_Running;
    
    /**
     * Property Serializer
     *
     * A Serializer used by this Service.
     */
    private transient com.tangosol.io.Serializer __m_Serializer;
    
    /**
     * Property ServiceListeners
     *
     * The collection of registered ServiceListener objects.
     * 
     * @see #addServiceListener
     * @see #removeServiceListener
     */
    private com.tangosol.util.Listeners __m_ServiceListeners;
    
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
    
    /**
     * Property ServiceVersion
     *
     */
    private String __m_ServiceVersion;
    
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
        __mapChildren.put("BackingMapContext", LocalCache.BackingMapContext.get_CLASS());
        __mapChildren.put("CacheHandler", LocalCache.CacheHandler.get_CLASS());
        }
    
    // Default constructor
    public LocalCache()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public LocalCache(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setCacheHandlerMap(new com.tangosol.util.SafeHashMap());
            setLockingEnforced(false);
            setLockWaitMillis(0L);
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setServiceListeners(new com.tangosol.util.Listeners());
            setServiceType("LocalCache");
            setServiceVersion("2.2");
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
        return new com.tangosol.coherence.component.util.LocalCache();
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
            clz = Class.forName("com.tangosol.coherence/component/util/LocalCache".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.CacheService
    public void addMemberListener(com.tangosol.net.MemberListener listener)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.CacheService
    public void addServiceListener(com.tangosol.util.ServiceListener listener)
        {
        getServiceListeners().add(listener);
        }
    
    // From interface: com.tangosol.net.CacheService
    public void configure(com.tangosol.run.xml.XmlElement xml)
        {
        if (xml != null)
            {
            // TODO: document in the coherence-operational-config.xsd
            setLockingEnforced(xml.getSafeElement("lock-enforce").getBoolean());
            setLockWaitMillis(xml.getSafeElement("lock-wait").getLong());
            }
        }
    
    // From interface: com.tangosol.net.CacheService
    public void destroyCache(com.tangosol.net.NamedCache map)
        {
        LocalCache.CacheHandler handler = (LocalCache.CacheHandler) map;
        
        getCacheHandlerMap().remove(handler.getCacheName());
        
        handler.invalidate();
        }
    
    // From interface: com.tangosol.net.CacheService
    public synchronized com.tangosol.net.NamedCache ensureCache(String sName, ClassLoader loader)
        {
        // import com.tangosol.net.BackingMapManager as com.tangosol.net.BackingMapManager;
        // import com.tangosol.util.SafeHashMap;
        // import java.util.Map;
        
        Map mapCache = getCacheHandlerMap();
        
        if (sName == null || sName.length() == 0)
            {
            sName = "Default";
            }
        
        LocalCache.CacheHandler handler  = (LocalCache.CacheHandler) mapCache.get(sName);
        if (handler == null)
            {
            com.tangosol.net.BackingMapManager manager = getBackingMapManager();
            Map     map     = manager == null ?
                new SafeHashMap() : manager.instantiateBackingMap(sName);
        
            if (map == null)
                {
                throw new RuntimeException(
                    "BackingMapManager returned \"null\" for map " + sName);
                }
        
            handler = (LocalCache.CacheHandler) _newChild("CacheHandler");
            handler._initFeed(map, isLockingEnforced(), getLockWaitMillis());
            handler.setCacheName(sName);
        
            mapCache.put(sName, handler);
            }
        return handler;
        }
    
    // Accessor for the property "BackingMapContext"
    /**
     * Getter for property BackingMapContext.<p>
    * The BackingMapContext (lazily created) is used by the BackingMapManager
    * (if provided).
     */
    public LocalCache.BackingMapContext getBackingMapContext()
        {
        LocalCache.BackingMapContext context = __m_BackingMapContext;
        if (context == null)
            {
            synchronized (this)
                {
                context = __m_BackingMapContext;
                if (context == null)
                    {
                    context = (LocalCache.BackingMapContext) _newChild("BackingMapContext");
                    setBackingMapContext(context);
                    }
                }
            }
        return context;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "BackingMapManager"
    /**
     * Getter for property BackingMapManager.<p>
    * Interface that provides the backing map storage implementations for the
    * cache.
     */
    public com.tangosol.net.BackingMapManager getBackingMapManager()
        {
        return __m_BackingMapManager;
        }
    
    // Accessor for the property "CacheHandlerMap"
    /**
     * Getter for property CacheHandlerMap.<p>
     */
    public java.util.Map getCacheHandlerMap()
        {
        return __m_CacheHandlerMap;
        }
    
    // From interface: com.tangosol.net.CacheService
    public java.util.Enumeration getCacheNames()
        {
        // import com.tangosol.util.IteratorEnumerator;
        
        return new IteratorEnumerator(getCacheHandlerMap().keySet().iterator());
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "Cluster"
    /**
     * Getter for property Cluster.<p>
     */
    public com.tangosol.net.Cluster getCluster()
        {
        return __m_Cluster;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "ContextClassLoader"
    /**
     * Getter for property ContextClassLoader.<p>
     */
    public ClassLoader getContextClassLoader()
        {
        return __m_ContextClassLoader;
        }
    
    // From interface: com.tangosol.net.CacheService
    public com.tangosol.net.ServiceDependencies getDependencies()
        {
        return null;
        }
    
    // From interface: com.tangosol.net.CacheService
    public com.tangosol.net.ServiceInfo getInfo()
        {
        return this;
        }
    
    // Accessor for the property "LockWaitMillis"
    /**
     * Getter for property LockWaitMillis.<p>
    * If locking enforcement is required then this parameter speicifes the
    * number of milliseconds to continue trying to obtain a lock; -1 blocks the
    * calling thread until the lock could be obtained. Configured by
    * "lock-wait" element.
    * 
    * @see configure
     */
    public long getLockWaitMillis()
        {
        return __m_LockWaitMillis;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public com.tangosol.net.Member getOldestMember()
        {
        return null;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "ResourceRegistry"
    /**
     * Getter for property ResourceRegistry.<p>
    * ResourceRegistry associated with this Service.
     */
    public com.tangosol.util.ResourceRegistry getResourceRegistry()
        {
        return __m_ResourceRegistry;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "Serializer"
    /**
     * Getter for property Serializer.<p>
    * A Serializer used by this Service.
     */
    public com.tangosol.io.Serializer getSerializer()
        {
        return __m_Serializer;
        }
    
    // Accessor for the property "ServiceListeners"
    /**
     * Getter for property ServiceListeners.<p>
    * The collection of registered ServiceListener objects.
    * 
    * @see #addServiceListener
    * @see #removeServiceListener
     */
    public com.tangosol.util.Listeners getServiceListeners()
        {
        return __m_ServiceListeners;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public com.tangosol.net.Member getServiceMember(int nId)
        {
        return null;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public java.util.Set getServiceMembers()
        {
        // import com.tangosol.util.NullImplementation;
        
        return NullImplementation.getSet();
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    // Accessor for the property "ServiceName"
    /**
     * Getter for property ServiceName.<p>
     */
    public String getServiceName()
        {
        return __m_ServiceName;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    // Accessor for the property "ServiceType"
    /**
     * Getter for property ServiceType.<p>
     */
    public String getServiceType()
        {
        return __m_ServiceType;
        }
    
    // Accessor for the property "ServiceVersion"
    /**
     * Getter for property ServiceVersion.<p>
     */
    public String getServiceVersion()
        {
        return __m_ServiceVersion;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    // Accessor for the property "ServiceVersion"
    /**
     * Getter for property ServiceVersion.<p>
     */
    public String getServiceVersion(com.tangosol.net.Member member)
        {
        return getServiceVersion();
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "UserContext"
    /**
     * Getter for property UserContext.<p>
    * User context object associated with this Service.
     */
    public Object getUserContext()
        {
        return __m_UserContext;
        }
    
    // Accessor for the property "LockingEnforced"
    /**
     * Getter for property LockingEnforced.<p>
    * If true the locking is enforced for put, remove and clear operations;
    * otherwise a client is responsible for calling lock and unlock explicitly.
    * Configured by "lock-enforce" element.
    * 
    * @see configure;
     */
    public boolean isLockingEnforced()
        {
        return __m_LockingEnforced;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "Running"
    /**
     * Getter for property Running.<p>
     */
    public boolean isRunning()
        {
        return __m_Running;
        }
    
    // From interface: com.tangosol.net.CacheService
    public boolean isSuspended()
        {
        return false;
        }
    
    // From interface: com.tangosol.net.CacheService
    public void releaseCache(com.tangosol.net.NamedCache map)
        {
        destroyCache(map);
        }
    
    // From interface: com.tangosol.net.CacheService
    public void removeMemberListener(com.tangosol.net.MemberListener listener)
        {
        }
    
    // From interface: com.tangosol.net.CacheService
    public void removeServiceListener(com.tangosol.util.ServiceListener listener)
        {
        getServiceListeners().remove(listener);
        }
    
    // Accessor for the property "BackingMapContext"
    /**
     * Setter for property BackingMapContext.<p>
    * The BackingMapContext (lazily created) is used by the BackingMapManager
    * (if provided).
     */
    protected void setBackingMapContext(LocalCache.BackingMapContext ctx)
        {
        __m_BackingMapContext = ctx;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "BackingMapManager"
    /**
     * Setter for property BackingMapManager.<p>
    * Interface that provides the backing map storage implementations for the
    * cache.
     */
    public void setBackingMapManager(com.tangosol.net.BackingMapManager manager)
        {
        if (isRunning())
            {
            throw new IllegalStateException("Service is already running");
            }
        
        __m_BackingMapManager = (manager);
        }
    
    // Accessor for the property "CacheHandlerMap"
    /**
     * Setter for property CacheHandlerMap.<p>
     */
    protected void setCacheHandlerMap(java.util.Map map)
        {
        __m_CacheHandlerMap = map;
        }
    
    // Accessor for the property "Cluster"
    /**
     * Setter for property Cluster.<p>
     */
    public void setCluster(com.tangosol.net.Cluster cluster)
        {
        __m_Cluster = cluster;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "ContextClassLoader"
    /**
     * Setter for property ContextClassLoader.<p>
     */
    public void setContextClassLoader(ClassLoader loader)
        {
        __m_ContextClassLoader = loader;
        }
    
    // From interface: com.tangosol.net.CacheService
    public void setDependencies(com.tangosol.net.ServiceDependencies Param_1)
        {
        }
    
    // Accessor for the property "LockingEnforced"
    /**
     * Setter for property LockingEnforced.<p>
    * If true the locking is enforced for put, remove and clear operations;
    * otherwise a client is responsible for calling lock and unlock explicitly.
    * Configured by "lock-enforce" element.
    * 
    * @see configure;
     */
    public void setLockingEnforced(boolean fEnforced)
        {
        __m_LockingEnforced = fEnforced;
        }
    
    // Accessor for the property "LockWaitMillis"
    /**
     * Setter for property LockWaitMillis.<p>
    * If locking enforcement is required then this parameter speicifes the
    * number of milliseconds to continue trying to obtain a lock; -1 blocks the
    * calling thread until the lock could be obtained. Configured by
    * "lock-wait" element.
    * 
    * @see configure
     */
    public void setLockWaitMillis(long cWaitMillis)
        {
        __m_LockWaitMillis = cWaitMillis;
        }
    
    // Accessor for the property "ResourceRegistry"
    /**
     * Setter for property ResourceRegistry.<p>
    * ResourceRegistry associated with this Service.
     */
    protected void setResourceRegistry(com.tangosol.util.ResourceRegistry registry)
        {
        __m_ResourceRegistry = registry;
        }
    
    // Accessor for the property "Running"
    /**
     * Setter for property Running.<p>
     */
    protected void setRunning(boolean fRunning)
        {
        __m_Running = fRunning;
        }
    
    // Accessor for the property "Serializer"
    /**
     * Setter for property Serializer.<p>
    * A Serializer used by this Service.
     */
    protected void setSerializer(com.tangosol.io.Serializer serializer)
        {
        __m_Serializer = serializer;
        }
    
    // Accessor for the property "ServiceListeners"
    /**
     * Setter for property ServiceListeners.<p>
    * The collection of registered ServiceListener objects.
    * 
    * @see #addServiceListener
    * @see #removeServiceListener
     */
    protected void setServiceListeners(com.tangosol.util.Listeners listeners)
        {
        __m_ServiceListeners = listeners;
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
    protected void setServiceType(String sType)
        {
        __m_ServiceType = sType;
        }
    
    // Accessor for the property "ServiceVersion"
    /**
     * Setter for property ServiceVersion.<p>
     */
    protected void setServiceVersion(String sVersion)
        {
        __m_ServiceVersion = sVersion;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "UserContext"
    /**
     * Setter for property UserContext.<p>
    * User context object associated with this Service.
     */
    public void setUserContext(Object oCtx)
        {
        __m_UserContext = oCtx;
        }
    
    // From interface: com.tangosol.net.CacheService
    public void shutdown()
        {
        stop();
        }
    
    // From interface: com.tangosol.net.CacheService
    public synchronized void start()
        {
        // import com.tangosol.net.BackingMapManager;
        // import com.tangosol.util.ExternalizableHelper;
        
        setRunning(true);
        
        // per BackingMapManager contract: call init()
        BackingMapManager manager = getBackingMapManager();
        if (manager != null)
            {
            LocalCache.BackingMapContext ctx = getBackingMapContext();
            ctx.setManager(manager);
        
            manager.init(ctx);
            }
        
        setSerializer(ExternalizableHelper.ensureSerializer(getContextClassLoader()));
        }
    
    // From interface: com.tangosol.net.CacheService
    public synchronized void stop()
        {
        // import com.tangosol.util.ServiceEvent;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        if (isRunning())
            {
            Map mapHandler = getCacheHandlerMap();
            for (Iterator iter = mapHandler.entrySet().iterator(); iter.hasNext();)
                {
                java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
        
                LocalCache.CacheHandler handler = (LocalCache.CacheHandler) entry.getValue();
                handler.invalidate();
                iter.remove();
                }
        
            ServiceEvent event = new ServiceEvent(this, ServiceEvent.SERVICE_STOPPED);
            event.dispatch(getServiceListeners());   
            setRunning(false);
            }
        }

    @Override
    public boolean isVersionCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nMajor, nMinor, nMicro, nPatchSet, nPatch);
        return CacheFactory.VERSION_ENCODED >= nEncoded;
        }

    @Override
    public boolean isVersionCompatible(int nYear, int nMonth, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nYear, nMonth, nPatch);
        return CacheFactory.VERSION_ENCODED >= nEncoded;
        }

    @Override
    public boolean isVersionCompatible(int nVersion)
        {
        return CacheFactory.VERSION_ENCODED >= nVersion;
        }

    @Override
    public boolean isVersionCompatible(IntPredicate predicate)
        {
        return predicate.test(CacheFactory.VERSION_ENCODED);
        }

    @Override
    public int getMinimumServiceVersion()
        {
        return CacheFactory.VERSION_ENCODED;
        }

    // Declared at the super level
    public String toString()
        {
        return get_Name() + '{' + getServiceName() + '}';
        }

    // ---- class: com.tangosol.coherence.component.util.LocalCache$BackingMapContext
    
    /**
     * The BackingMapManagerContext implementation.
     * 
     * Added decoration support methods in Coherence 3.2.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BackingMapContext
            extends    com.tangosol.coherence.component.util.BackingMapManagerContext
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public BackingMapContext()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public BackingMapContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.LocalCache.BackingMapContext();
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
                clz = Class.forName("com.tangosol.coherence/component/util/LocalCache$BackingMapContext".replace('/', '.'));
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
        
        // Declared at the super level
        public java.util.Map getBackingMap(String sCacheName)
            {
            LocalCache.CacheHandler handler = (LocalCache.CacheHandler) ((LocalCache) get_Module())
                .getCacheHandlerMap().get(sCacheName);
            
            return handler == null ? null : handler.getActualMap();
            }
        
        // Declared at the super level
        public String toString()
            {
            return super.toString() + "@" + hashCode();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.LocalCache$CacheHandler
    
    /**
     * Simple implementation of ConcurrentMap (and ObservableMap). This
     * component is a trivial integration of
     * com.tangosol.util.WrapperConcurrentMap.
     * 
     * Note: the ConcurrentMap has to be instantiated using _initFeed(Map)
     * constructor.
     * 
     * Known subclasses LocalCache$CacheHandler.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class CacheHandler
            extends    com.tangosol.coherence.component.util.ConcurrentMap
            implements com.tangosol.net.NamedCache,
                       com.tangosol.util.MapListener
        {
        // ---- Fields declarations ----
        
        /**
         * Property CacheName
         *
         */
        private String __m_CacheName;
        
        /**
         * Property DeactivationListeners
         *
         * Registered NamedCacheDeactivationListeners.
         */
        private com.tangosol.util.Listeners __m_DeactivationListeners;
        
        /**
         * Property IndexMap
         *
         * The map of indexes maintaned by this cache handler. The keys of the
         * Map are ValueExtractor objects, and for each key, the corresponding
         * value stored in the Map is a MapIndex object.
         */
        private java.util.Map __m_IndexMap;
        
        /**
         * Property ListenerSupport
         *
         */
        private transient com.tangosol.util.MapListenerSupport __m_ListenerSupport;
        
        // Default constructor
        public CacheHandler()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public CacheHandler(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setDeactivationListeners(new com.tangosol.util.Listeners());
                setListenerSupport(new com.tangosol.util.MapListenerSupport());
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
            return new com.tangosol.coherence.component.util.LocalCache.CacheHandler();
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
                clz = Class.forName("com.tangosol.coherence/component/util/LocalCache$CacheHandler".replace('/', '.'));
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
        
        // From interface: com.tangosol.net.NamedCache
        /**
         * Add an index for the given extractor.  The ValueExtractor object that
        * is used to extract an indexable Object from a value stored in the
        * cache.
         */
        public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
            {
            // import com.tangosol.util.InvocableMapHelper;
            
            InvocableMapHelper.addIndex(extractor, fOrdered, comparator, this,
                    ensureIndexMap());
            }
        
        // From interface: com.tangosol.net.NamedCache
        // Declared at the super level
        public void addMapListener(com.tangosol.util.MapListener listener)
            {
            // import com.tangosol.util.Filter;
            
            addMapListener(listener, (Filter) null, false);
            }
        
        // From interface: com.tangosol.net.NamedCache
        // Declared at the super level
        public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
            {
            // import com.tangosol.internal.net.NamedCacheDeactivationListener;
            // import com.tangosol.util.Filter;
            // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
            
            if (listener instanceof NamedCacheDeactivationListener)
                {
                getDeactivationListeners().add(listener);
                }
            else
                {
                if (listener == this)
                    {
                    super.addMapListener(this, filter, fLite);
                    }
                else if (listener != null)
                    {
                    com.tangosol.util.MapListenerSupport support = getListenerSupport();
                    boolean fWasEmpty;
                    boolean fWasLite;
            
                    synchronized (support)
                        {
                        fWasEmpty = support.isEmpty(filter);
                        fWasLite  = fWasEmpty || !support.containsStandardListeners(filter);
            
                        support.addListener(wrap(listener), filter, fLite);
                        }
            
                    if (fWasEmpty || (fWasLite && !fLite))
                        {
                        try
                            {
                            addMapListener(this, filter, fLite);
                            }
                        catch (RuntimeException e)
                            {
                            support.removeListener(listener, filter);
                            throw e;
                            }
                        }
                    }
                }
            }
        
        // From interface: com.tangosol.net.NamedCache
        // Declared at the super level
        public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
            {
            // import com.tangosol.util.Filter;
            // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
            
            if (listener == this)
                {
                super.addMapListener(this, oKey, fLite);
                }
            else if (listener != null)
                {
                com.tangosol.util.MapListenerSupport support = getListenerSupport();
                boolean fWasEmpty;
                boolean fWasLite;
            
                synchronized (support)
                    {
                    fWasEmpty = support.isEmpty(oKey);
                    fWasLite  = fWasEmpty || !support.containsStandardListeners(oKey);
                    
                    support.addListener(wrap(listener), oKey, fLite);
                    }
                
                if (fWasEmpty || (fWasLite && !fLite))
                    {
                    try
                        {
                        addMapListener(this, oKey, fLite);
                        }
                    catch (RuntimeException e)
                        {
                        support.removeListener(listener, oKey);
                        throw e;
                        }
                    }
                }
            }
        
        // From interface: com.tangosol.net.NamedCache
        public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent)
            {
            return aggregate(keySet(filter), agent);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public Object aggregate(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
            {
            // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
            
            return agent.aggregate(com.tangosol.util.InvocableMapHelper.makeEntrySet(getActualMap(), collKeys, true));
            }

        /**
         * LocalCache does not support AsyncNamedCache
         */
        // From interface: com.tangosol.net.NamedCache
        @Override
        public AsyncNamedCache async(AsyncNamedCache.Option... options)
            {
            throw new UnsupportedOperationException();
            }

        // From interface: com.tangosol.net.NamedCache
        public void destroy()
            {
            getCacheService().destroyCache(this);
            }
        
        /**
         * Ensure that the map of indexes maintaned by this cache handler exists.
         */
        public java.util.Map ensureIndexMap()
            {
            // import com.tangosol.util.SafeHashMap;
            // import java.util.Map;
            
            Map mapIndex = getIndexMap();
            if (mapIndex == null)
                {
                synchronized (this)
                    {
                    mapIndex = getIndexMap();
                    if (mapIndex == null)
                        {
                        setIndexMap(mapIndex = new SafeHashMap());
                        }
                    }
                }
            return mapIndex;
            }
        
        // From interface: com.tangosol.util.MapListener
        public void entryDeleted(com.tangosol.util.MapEvent evt)
            {
            translateMapEvent(evt);
            }
        
        // From interface: com.tangosol.util.MapListener
        public void entryInserted(com.tangosol.util.MapEvent evt)
            {
            translateMapEvent(evt);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Set entrySet(com.tangosol.util.Filter filter)
            {
            // import com.tangosol.util.InvocableMapHelper;
            
            return InvocableMapHelper.query(this, getIndexMap(), filter, true, false, null);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Set entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
            {
            // import com.tangosol.util.InvocableMapHelper;
            
            return InvocableMapHelper.query(this, getIndexMap(), filter, true, true, comparator);
            }
        
        // From interface: com.tangosol.util.MapListener
        public void entryUpdated(com.tangosol.util.MapEvent evt)
            {
            translateMapEvent(evt);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Map getAll(java.util.Collection colKeys)
            {
            // import com.tangosol.net.cache.CacheMap;
            // import java.util.HashMap;
            // import java.util.Iterator;
            // import java.util.Map;
            
            Map map = getActualMap();
            if (map instanceof CacheMap)
                {
                return ((CacheMap) map).getAll(colKeys);
                }
            else
                {
                Map mapResult = new HashMap(colKeys.size()); 
                for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
                    {
                    Object oKey = iter.next();
                    Object oVal = get(oKey);
                    if (oVal != null || containsKey(oKey))
                        {
                        mapResult.put(oKey, oVal);
                        }
                    }
                return mapResult;
                }
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
        // Accessor for the property "CacheService"
        /**
         * Getter for property CacheService.<p>
         */
        public com.tangosol.net.CacheService getCacheService()
            {
            return (LocalCache) get_Module();
            }
        
        // Accessor for the property "DeactivationListeners"
        /**
         * Getter for property DeactivationListeners.<p>
        * Registered NamedCacheDeactivationListeners.
         */
        public com.tangosol.util.Listeners getDeactivationListeners()
            {
            return __m_DeactivationListeners;
            }
        
        // Accessor for the property "IndexMap"
        /**
         * Getter for property IndexMap.<p>
        * The map of indexes maintaned by this cache handler. The keys of the
        * Map are ValueExtractor objects, and for each key, the corresponding
        * value stored in the Map is a MapIndex object.
         */
        public java.util.Map getIndexMap()
            {
            return __m_IndexMap;
            }
        
        // Accessor for the property "ListenerSupport"
        /**
         * Getter for property ListenerSupport.<p>
         */
        public com.tangosol.util.MapListenerSupport getListenerSupport()
            {
            return __m_ListenerSupport;
            }
        
        public void invalidate()
            {
            // import Component.Util.CacheEvent as CacheEvent;
            // import com.tangosol.net.BackingMapManager;
            // import com.tangosol.net.cache.CacheEvent as com.tangosol.net.cache.CacheEvent;
            // import com.tangosol.util.Filter;
            // import com.tangosol.util.Listeners;
            // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
            // import java.util.ConcurrentModificationException;
            // import java.util.Iterator;
            
            com.tangosol.util.MapListenerSupport support = getListenerSupport();
            if (!support.isEmpty())
                {
                for (int i = 0; i < 3; i++)
                    {
                    try
                        {
                        // COH-4272: instead of holding synchronization on the support to
                        //           force a safe iteration, we catch CME and retry
                        for (Iterator iter = support.getFilterSet().iterator(); iter.hasNext();)
                            {
                            removeMapListener(this, (Filter) iter.next());
                            }
                        for (Iterator iter = support.getKeySet().iterator(); iter.hasNext();)
                            {
                            removeMapListener(this, (Object) iter.next());
                            }
            
                        break;
                        }
                    catch (ConcurrentModificationException cme)
                        {
                        }
                    }
                support.clear();
                }
            
            BackingMapManager manager = getCacheService().getBackingMapManager();
            if (manager != null)
                {
                manager.releaseBackingMap(getCacheName(), getActualMap());
                }
            
            Listeners listeners = getDeactivationListeners();
            if (!listeners.isEmpty())
                {
                com.tangosol.net.cache.CacheEvent evt = new com.tangosol.net.cache.CacheEvent(this, com.tangosol.net.cache.CacheEvent.ENTRY_DELETED, null, null, null, true);
                // dispatch the event to the listeners, which are all synchronous (hence the null Queue)
                CacheEvent.dispatchSafe(evt, listeners, null /*Queue*/);
                }
            }
        
        // From interface: com.tangosol.net.NamedCache
        public Object invoke(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
            {
            // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
            
            return com.tangosol.util.InvocableMapHelper.invokeLocked(this,
                com.tangosol.util.InvocableMapHelper.makeEntry(getActualMap(), oKey), agent);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
            {
            // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
            
            return com.tangosol.util.InvocableMapHelper.invokeAllLocked(this, com.tangosol.util.InvocableMapHelper.duplicateEntrySet(getActualMap(),
                    entrySet(filter), false), agent);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Map invokeAll(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
            {
            // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
            
            return com.tangosol.util.InvocableMapHelper.invokeAllLocked(this,
                com.tangosol.util.InvocableMapHelper.makeEntrySet(getActualMap(), collKeys, false), agent);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public boolean isActive()
            {
            return ((LocalCache) get_Module()).getCacheHandlerMap().get(getCacheName()) == this;
            }
        
        // From interface: com.tangosol.net.NamedCache
        public boolean isDestroyed()
            {
            return !isActive();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public boolean isReleased()
            {
            return !isActive();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Set keySet(com.tangosol.util.Filter filter)
            {
            // import com.tangosol.util.InvocableMapHelper;
            
            return InvocableMapHelper.query(this, getIndexMap(), filter, false, false, null);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public Object put(Object oKey, Object oValue, long cMillis)
            {
            // import com.tangosol.net.cache.CacheMap;
            // import java.util.Map;
            
            Map map = getActualMap();
            if (map instanceof CacheMap)
                {
                return ((CacheMap) map).put(oKey, oValue, cMillis);
                }
            else if (cMillis <= 0)
                {
                return put(oKey, oValue);
                }
            else
                {
                throw new UnsupportedOperationException(
                    "Class \"" + map.getClass().getName() +
                    "\" does not implement CacheMap interface");
                }
            }
        
        // From interface: com.tangosol.net.NamedCache
        public void release()
            {
            getCacheService().releaseCache(this);
            }
        
        // From interface: com.tangosol.net.NamedCache
        /**
         * Remove the index associated with the given extractor from the map of
        * indexes maintaned by this cache handler.
         */
        public void removeIndex(com.tangosol.util.ValueExtractor extractor)
            {
            // import com.tangosol.util.InvocableMapHelper;
            
            InvocableMapHelper.removeIndex(extractor, this, ensureIndexMap());
            }
        
        // From interface: com.tangosol.net.NamedCache
        // Declared at the super level
        public void removeMapListener(com.tangosol.util.MapListener listener)
            {
            // import com.tangosol.util.Filter;
            
            removeMapListener(listener, (Filter) null);
            }
        
        // From interface: com.tangosol.net.NamedCache
        // Declared at the super level
        public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
            {
            // import com.tangosol.internal.net.NamedCacheDeactivationListener;
            // import com.tangosol.util.Filter;
            // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
            
            if (listener instanceof NamedCacheDeactivationListener)
                {
                getDeactivationListeners().remove(listener);
                }
            else
                {
                if (listener == this)
                    {
                    super.removeMapListener(this, filter);
                    }
                else
                    {
                    com.tangosol.util.MapListenerSupport support = getListenerSupport();
                    boolean fEmpty;
            
                    synchronized (support)
                        {
                        support.removeListener(wrap(listener), filter);
                        fEmpty = support.isEmpty(filter);
                        }
            
                    if (fEmpty)
                        {
                        removeMapListener(this, filter);
                        }
                    }
                }
            }
        
        // From interface: com.tangosol.net.NamedCache
        // Declared at the super level
        public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
            {
            // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
            
            if (listener == this)
                {
                super.removeMapListener(this, oKey);
                }
            else
                {
                com.tangosol.util.MapListenerSupport support = getListenerSupport();
                boolean fEmpty;
            
                synchronized (support)
                    {
                    support.removeListener(wrap(listener), oKey);
            
                    fEmpty = support.isEmpty(oKey);
                    }
            
                if (fEmpty)
                    {
                    removeMapListener(this, oKey);
                    }
                }
            }
        
        // Accessor for the property "CacheName"
        /**
         * Setter for property CacheName.<p>
         */
        public void setCacheName(String sName)
            {
            __m_CacheName = sName;
            }
        
        // Accessor for the property "DeactivationListeners"
        /**
         * Setter for property DeactivationListeners.<p>
        * Registered NamedCacheDeactivationListeners.
         */
        protected void setDeactivationListeners(com.tangosol.util.Listeners listeners)
            {
            __m_DeactivationListeners = listeners;
            }
        
        // Accessor for the property "IndexMap"
        /**
         * Setter for property IndexMap.<p>
        * The map of indexes maintaned by this cache handler. The keys of the
        * Map are ValueExtractor objects, and for each key, the corresponding
        * value stored in the Map is a MapIndex object.
         */
        protected void setIndexMap(java.util.Map support)
            {
            __m_IndexMap = support;
            }
        
        // Accessor for the property "ListenerSupport"
        /**
         * Setter for property ListenerSupport.<p>
         */
        protected void setListenerSupport(com.tangosol.util.MapListenerSupport support)
            {
            __m_ListenerSupport = support;
            }
        
        // Declared at the super level
        public String toString()
            {
            StringBuffer sb = new StringBuffer();
            
            sb.append(get_Name())
              .append("{Name=")
              .append(getCacheName())
              .append(", ServiceName=")
              .append(getCacheService().getInfo().getServiceName())
              .append('}');
            
            return sb.toString();
            }
        
        protected void translateMapEvent(com.tangosol.util.MapEvent evt)
            {
            // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
            
            // ensure lazy event data access
            evt = com.tangosol.util.MapListenerSupport.convertEvent(evt, this, null, null);
            getListenerSupport().fireEvent(evt, true);
            }
        
        /**
         * Wrap the specified listener into a ContainerContext aware listener.
         */
        protected com.tangosol.util.MapListener wrap(com.tangosol.util.MapListener listener)
            {
            // import com.tangosol.application.ContainerHelper;
            
            return ContainerHelper.getContextAwareListener(getCacheService(), listener);
            }
        }
    }
