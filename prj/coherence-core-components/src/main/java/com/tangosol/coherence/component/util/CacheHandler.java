
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.CacheHandler

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.net.Lease;
import com.tangosol.coherence.component.util.CacheEvent;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache;
import com.oracle.coherence.common.base.Blocking;
import com.tangosol.application.ContainerHelper;
import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.Serializer;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.cache.CacheMap;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.OverflowMap;
import com.tangosol.run.component.EventDeathException;
import com.tangosol.run.xml.SimpleElement;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.Listeners;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.WrapperException;
import com.tangosol.util.filter.InKeySetFilter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * CacheHandler represents a named Cache handled by the ReplicatedCache
 * service. During creation each handler is assigned an index that could be
 * used to obtain this handler out of the indexed property "CacheHandler"
 * maintained by the ReplicatedCache service. For the same index there could be
 * a list of handlers that differ only by the value of ClassLoader property.
 * The NextHandler property is used to maintain this list.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class CacheHandler
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.io.ClassLoaderAware,
                   com.tangosol.net.NamedCache,
                   com.tangosol.run.xml.XmlSerializable,
                   com.tangosol.util.Converter
    {
    // ---- Fields declarations ----
    
    /**
     * Property BackingMapListener
     *
     * BackingMap listener. Used only if a custom backing map manager uses an
     * ObservableMap to implement the local storage.
     */
    private transient com.tangosol.util.MapListener __m_BackingMapListener;
    
    /**
     * Property CacheIndex
     *
     * This index of this handler in the indexed CacheHandler property
     * maintained by the cache service.
     * 
     * @see ReplicatedCache#instantiateCacheHandler
     */
    private int __m_CacheIndex;
    
    /**
     * Property CacheName
     *
     * This name of the cache that is managed by this handler. It could only be
     * null if the cached resources started coming from another member, but the
     * Catalog has not been updated yet. It could only be empty (length == 0)
     * for the Catalog Cache handler.
     * 
     * @see ReplicatedCache#instantiateCacheHandler
     */
    private String __m_CacheName;
    
    /**
     * Property ClassLoader
     *
     * ClassLoader that should be used by ResourceMessages to deserialize
     * resources that are managed by this handler. The only situations when the
     * ClassLoader could be null are:
     * <ul>
     * <li>the cache has already been replicated by the service, but no client
     * is yet connected to this map
     * <li>the client has called the "release" on the last copy of the cache
     * handler with a given name
     * </ul>
     * 
     * @see #getCachedResource
     */
    private transient ClassLoader __m_ClassLoader;
    
    /**
     * Property ConverterFromInternal
     *
     * A converter that takes service specific "transmittable" serializable
     * objects and converts them via deserialization (etc.) to the objects
     * expected by clients of the cache.
     */
    private transient com.tangosol.util.Converter __m_ConverterFromInternal;
    
    /**
     * Property ConverterToInternal
     *
     * A converter that takes ClassLoader dependent objects and converts them
     * via serialization into Binary objects.
     */
    private transient com.tangosol.util.Converter __m_ConverterToInternal;
    
    /**
     * Property DeactivationListeners
     *
     * Registered NamedCacheDeactivationListeners.
     */
    private com.tangosol.util.Listeners __m_DeactivationListeners;
    
    /**
     * Property IgnoreKey
     *
     * The key of a resource in the local storage that is currently being
     * processed by the service and therefore should be ignored by the
     * BackingMapListener.
     */
    private transient Object __m_IgnoreKey;
    
    /**
     * Property IndexMap
     *
     * This Map assosiates resource keys with the corresponding Lease objects
     * for each leased resource. Its key set is a subset of the ResourceMap key
     * set.
     */
    private transient java.util.Map __m_IndexMap;
    
    /**
     * Property LeaseMap
     *
     * This Map assosiates resource keys with the corresponding Lease objects
     * for each leased resource. Its key set is a subset of the ResourceMap key
     * set.
     */
    private transient java.util.Map __m_LeaseMap;
    
    /**
     * Property ListenerSupport
     *
     * Registered listeners to the MapEvent notifications.
     */
    private transient com.tangosol.util.MapListenerSupport __m_ListenerSupport;
    
    /**
     * Property MAX_MAP_RETRIES
     *
     */
    public static final int MAX_MAP_RETRIES = 32;
    
    /**
     * Property NextHandler
     *
     * Reference to another handler with exact same proprties as this one
     * except the ClassLoader property
     */
    private transient CacheHandler __m_NextHandler;
    
    /**
     * Property PutExpiryWarned
     *
     * Log a warning the first time a 3-valued put() is called for per-entry
     * expiration.
     */
    private boolean __m_PutExpiryWarned;
    
    /**
     * Property ResourceMap
     *
     * This Map holds the resources assosiated with the Leases held in the
     * LeaseMap.
     */
    private transient java.util.Map __m_ResourceMap;
    
    /**
     * Property Serializer
     *
     * A Serializer used by this CacheHandler.
     * 
     * @see setClassLoader
     */
    private transient com.tangosol.io.Serializer __m_Serializer;
    
    /**
     * Property StandardLeaseMillis
     *
     * The duration of a standard Lease in milliseconds for the resources
     * controlled by this CacheHandler.
     */
    private long __m_StandardLeaseMillis;
    
    /**
     * Property UseEventDaemon
     *
     * If true, all map events will be dispatched on the MapEventQueue thread;
     * otherwise on the service thread itself
     */
    private boolean __m_UseEventDaemon;
    
    /**
     * Property Valid
     *
     * Returns false iff this CacheHandler has been explicitely invalidated
     * (destroyed).
     */
    private boolean __m_Valid;
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
        __mapChildren.put("BackingMapListener", CacheHandler.BackingMapListener.get_CLASS());
        __mapChildren.put("EntrySet", CacheHandler.EntrySet.get_CLASS());
        __mapChildren.put("KeySet", CacheHandler.KeySet.get_CLASS());
        __mapChildren.put("Validator", CacheHandler.Validator.get_CLASS());
        }
    
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
            setCacheIndex(-1);
            setDeactivationListeners(new com.tangosol.util.Listeners());
            setIgnoreKey(new java.lang.Object());
            setPutExpiryWarned(false);
            setStandardLeaseMillis(20000L);
            setUseEventDaemon(true);
            setValid(true);
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
        return new com.tangosol.coherence.component.util.CacheHandler();
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
            clz = Class.forName("com.tangosol.coherence/component/util/CacheHandler".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.NamedCache
    /**
     * Add an index for the given extractor.  The ValueExtractor object that is
    * used to extract an indexable Object from a value stored in the cache.
     */
    public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        // import com.tangosol.util.InvocableMapHelper;
        
        InvocableMapHelper.addIndex(extractor, fOrdered, comparator, this,
                ensureIndexMap());
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;
        
        addMapListener(listener, (Filter) null, false);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public synchronized void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
        {
        // import com.tangosol.internal.net.NamedCacheDeactivationListener;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import com.tangosol.util.filter.InKeySetFilter;
        // import java.util.Collection;
        // import java.util.Iterator;
        
        if (listener instanceof NamedCacheDeactivationListener)
            {
            getDeactivationListeners().add(listener);
            }
        else
            {
            _assert(listener != null);
        
            com.tangosol.util.MapListenerSupport support = getListenerSupport();
            if (support == null)
                {
                setListenerSupport(support = new com.tangosol.util.MapListenerSupport());
                }
        
            if (com.tangosol.util.MapListenerSupport.isPrimingListener(listener))
                {
                if (filter instanceof InKeySetFilter)
                    {
                    Collection colKeys = ((InKeySetFilter) filter).getKeys();
                    for (Iterator iter = colKeys.iterator(); iter.hasNext();)
                        {
                        addMapListener(listener, iter.next(), fLite);
                        }
                    }
                else
                    {
                    throw new UnsupportedOperationException(
                        "Priming listeners are only supported with InKeySetFilter");
                    }
                }
            else
                {
                support.addListener(wrap(listener), filter, fLite);
                }
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public synchronized void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        _assert(listener != null);
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (support == null)
            {
            setListenerSupport(support = new com.tangosol.util.MapListenerSupport());
            }
        
        support.addListener(wrap(listener), oKey, fLite);
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
        
        return agent.aggregate(com.tangosol.util.InvocableMapHelper.makeEntrySet(this, collKeys, true));
        }
    
    /**
     * Check the validity of this cache handler -- in other words wheter or not
    * this map is accessible by a client.
    * 
    * @exception IllegalStateException is thrown if the handler is not
    * currently active or the underlying service is not running
     */
    protected void checkAccess()
        {
        if (Thread.currentThread() != getService().getThread() && !isValid())
            {
            throw new IllegalStateException("Map has been invalidated:\n" + this);
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void clear()
        {
        // import java.util.Enumeration;
        
        for (Enumeration e = getResourceKeys(); e.hasMoreElements();)
            {
            Object oKey = e.nextElement();
        
            remove(oKey, false);
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean containsKey(Object oKey)
        {
        checkAccess();
        
        return getResourceMap().containsKey(oKey);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean containsValue(Object oValue)
        {
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        checkAccess();
        
        // the values could still be in serialized format, so we cannot just call:
        //     return getResourceMap().containsValue(oValue);
        Map mapResource = getResourceMap();
        return mapResource.containsValue(oValue) || mapResource.containsValue(
            ExternalizableHelper.toBinary(oValue, getSerializer()));
        }
    
    // From interface: com.tangosol.util.Converter
    /**
     * Key-to-value conversion used by to create effective "value()" collection.
    * 
    * @see #values()
     */
    public Object convert(Object o)
        {
        return getCachedResource(o);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void destroy()
        {
        getCacheService().destroyCache(this);
        }
    
    protected void dispatchEvent(com.tangosol.util.MapEvent event)
        {
        // import com.tangosol.util.MapEvent;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        if (event != null)
            {
            CacheHandler handler = this;
            do
                {
                com.tangosol.util.MapListenerSupport support = handler.getListenerSupport();
                if (support != null)
                    {
                    handler.dispatchEvent(event, support);
                    }
                handler = handler.getNextHandler();
                }
            while (handler != null);
            }
        }
    
    public void dispatchEvent(com.tangosol.util.MapEvent event, com.tangosol.util.MapListenerSupport support)
        {
        // import com.tangosol.util.Listeners;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        // ensure lazy event data conversion
        event = com.tangosol.util.MapListenerSupport.convertEvent(event, this, null, getConverterFromInternal());
        if (isUseEventDaemon())
            {
            Listeners listeners = support.collectListeners(event);
            CacheEvent.dispatchSafe(com.tangosol.util.MapListenerSupport.enrichEvent(event, listeners),
                listeners, getService().ensureEventDispatcher().getQueue());
            }
        else
            {
            support.fireEvent(event, true);
            }
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
    
    /**
     * Obtain a Lease for the specified resource. If the Lease doesn't exist a
    * new Lease object will be created
    * 
    * @param oKey  the resource key
    * 
    * @return  the Lease object for the specified resource
    * 
    * Note: the lease object should not be modified directly by a client.
     */
    public com.tangosol.coherence.component.net.Lease ensureLease(Object oKey)
        {
        // import Component.Net.Lease;
        // import java.util.Map;
        
        Lease lease = getLease(oKey);
        
        if (lease == null)
            {
            Map mapLease = getLeaseMap();
            synchronized (mapLease)
                {
                lease = getLease(oKey);
                if (lease == null)
                    {
                    lease = Lease.instantiate(getCacheIndex(), oKey, getService());
                    mapLease.put(oKey, lease);
                    }
                }
            }
        
        return lease;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet()
        {
        checkAccess();
        
        CacheHandler.EntrySet set = (CacheHandler.EntrySet) _newChild("EntrySet");
        set.setMap(this);
        set.setSet(getResourceMap().entrySet());
        
        return set;
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
    
    // From interface: com.tangosol.run.xml.XmlSerializable
    public void fromXml(com.tangosol.run.xml.XmlElement xml)
        {
        _assert(xml.getName().equals(get_Name()));
        
        setCacheName          (xml.getElement("CacheName")          .getString());
        setCacheIndex         (xml.getElement("CacheIndex")         .getInt());
        setStandardLeaseMillis(xml.getElement("StandardLeaseMillis").getLong());
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object get(Object oKey)
        {
        return getCachedResource(oKey);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map getAll(java.util.Collection colKeys)
        {
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        
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
    
    // Accessor for the property "BackingMapListener"
    /**
     * Getter for property BackingMapListener.<p>
    * BackingMap listener. Used only if a custom backing map manager uses an
    * ObservableMap to implement the local storage.
     */
    protected com.tangosol.util.MapListener getBackingMapListener()
        {
        return __m_BackingMapListener;
        }
    
    /**
     * Return a (possibly) dirty instance of the resource value.
    * 
    * @see #getLockedResource
     */
    public Object getCachedResource(Object oKey)
        {
        // import Component.Net.Lease;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.WrapperException;
        // import java.util.Map;
        
        checkAccess();
        
        boolean fNew        = false;
        Map     mapResource = getResourceMap();
        Lease   lease       = getLease(oKey);
        if (lease == null)
            {
            // non-existant resource; call into mapResource anyway:
            // (1) in case there is a read-through; (2) to update the statistics
            if (!mapResource.containsKey(oKey))
                {
                return null;
                }
            // orphaned or "read-through" resource, proceed
            lease = ensureLease(oKey);
            fNew  = true;
            }
        
        ClassLoader loaderResource;
        Object      oResource;
        synchronized (lease)
            {
            oResource      = mapResource.get(oKey);
            loaderResource = lease.getClassLoader();
            if (fNew)
                {
                lease.incrementResourceVersion();
                }
            }
        
        ClassLoader loaderCache = getClassLoader();
        if (loaderResource != null && loaderResource != loaderCache)
            {
            oResource = releaseClassLoader(lease);
            }
        
        if (oResource instanceof Binary)
            {
            Binary binValue = (Binary) oResource;
            try
                {
                oResource = getConverterFromInternal().convert(binValue);
                }
            catch (WrapperException e)
                {
                throw new WrapperException(e.getOriginalException(),
                    "CacheName=" + getCacheName() + ", Key=" + oKey);
                }
        
            synchronized (lease)
                {
                // make sure the resource has not changed since we got it
                if (binValue == mapResource.get(oKey))
                    {
                    lease.setClassLoader(loaderCache);
                    lease.setResourceSize(binValue.length());
        
                    mapResource.put(oKey, oResource);
                    }
                }
            }
        
        return oResource;
        }
    
    // Accessor for the property "CacheIndex"
    /**
     * Getter for property CacheIndex.<p>
    * This index of this handler in the indexed CacheHandler property
    * maintained by the cache service.
    * 
    * @see ReplicatedCache#instantiateCacheHandler
     */
    public int getCacheIndex()
        {
        return __m_CacheIndex;
        }
    
    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "CacheName"
    /**
     * Getter for property CacheName.<p>
    * This name of the cache that is managed by this handler. It could only be
    * null if the cached resources started coming from another member, but the
    * Catalog has not been updated yet. It could only be empty (length == 0)
    * for the Catalog Cache handler.
    * 
    * @see ReplicatedCache#instantiateCacheHandler
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
        return getService();
        }
    
    // Accessor for the property "ClassLoader"
    /**
     * Getter for property ClassLoader.<p>
    * ClassLoader that should be used by ResourceMessages to deserialize
    * resources that are managed by this handler. The only situations when the
    * ClassLoader could be null are:
    * <ul>
    * <li>the cache has already been replicated by the service, but no client
    * is yet connected to this map
    * <li>the client has called the "release" on the last copy of the cache
    * handler with a given name
    * </ul>
    * 
    * @see #getCachedResource
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
    
    // Accessor for the property "ConverterFromInternal"
    /**
     * Getter for property ConverterFromInternal.<p>
    * A converter that takes service specific "transmittable" serializable
    * objects and converts them via deserialization (etc.) to the objects
    * expected by clients of the cache.
     */
    protected com.tangosol.util.Converter getConverterFromInternal()
        {
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.NullImplementation;
        
        Converter conv = __m_ConverterFromInternal;
        if (conv == null)
            {
            synchronized (this)
                {
                conv = __m_ConverterFromInternal;
                if (conv == null)
                    {
                    ClassLoader loader = getClassLoader();
                    conv = loader == NullImplementation.getClassLoader()
                        ? NullImplementation.getConverter()
                        : getService().instantiateConverterFromInternal(loader);
                    setConverterFromInternal(conv);
                    }
                }
            }
        return conv;
        }
    
    // Accessor for the property "ConverterToInternal"
    /**
     * Getter for property ConverterToInternal.<p>
    * A converter that takes ClassLoader dependent objects and converts them
    * via serialization into Binary objects.
     */
    protected com.tangosol.util.Converter getConverterToInternal()
        {
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.NullImplementation;
        
        Converter conv = __m_ConverterToInternal;
        if (conv == null)
            {
            synchronized (this)
                {
                conv = __m_ConverterToInternal;
                if (conv == null)
                    {
                    ClassLoader loader = getClassLoader();
                    conv = loader == NullImplementation.getClassLoader()
                        ? NullImplementation.getConverter()
                        : getService().instantiateConverterToInternal(loader);
                    setConverterToInternal(conv);
                    }
                }
            }
        return conv;
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
    
    // Accessor for the property "IgnoreKey"
    /**
     * Getter for property IgnoreKey.<p>
    * The key of a resource in the local storage that is currently being
    * processed by the service and therefore should be ignored by the
    * BackingMapListener.
     */
    public Object getIgnoreKey()
        {
        return __m_IgnoreKey;
        }
    
    // Accessor for the property "IndexMap"
    /**
     * Getter for property IndexMap.<p>
    * This Map assosiates resource keys with the corresponding Lease objects
    * for each leased resource. Its key set is a subset of the ResourceMap key
    * set.
     */
    public java.util.Map getIndexMap()
        {
        return __m_IndexMap;
        }
    
    /**
     * Get a current Lease for the specified resource.
    * 
    * @param oKey  the resource key
    * 
    * @return  the Lease object for the specified resource; null if no Lease is
    * known to exist for this resource
    * 
    * Note: the lease object should not be modified directly by a client.
     */
    public com.tangosol.coherence.component.net.Lease getLease(Object oKey)
        {
        // import Component.Net.Lease;
        
        return (Lease) getLeaseMap().get(oKey);
        }
    
    // Accessor for the property "LeaseKeys"
    /**
     * Getter for property LeaseKeys.<p>
    * Calculated property returning the Enumeration of <i>issued</i> lease keys.
     */
    public java.util.Enumeration getLeaseKeys()
        {
        // import com.tangosol.util.SimpleEnumerator;
        
        return new SimpleEnumerator(getLeaseMap().keySet().toArray());
        }
    
    // Accessor for the property "LeaseMap"
    /**
     * Getter for property LeaseMap.<p>
    * This Map assosiates resource keys with the corresponding Lease objects
    * for each leased resource. Its key set is a subset of the ResourceMap key
    * set.
     */
    public java.util.Map getLeaseMap()
        {
        return __m_LeaseMap;
        }
    
    // Accessor for the property "ListenerSupport"
    /**
     * Getter for property ListenerSupport.<p>
    * Registered listeners to the MapEvent notifications.
     */
    public com.tangosol.util.MapListenerSupport getListenerSupport()
        {
        return __m_ListenerSupport;
        }
    
    /**
     * Obtain a locked resource with the specified name. This method returns a
    * <i>Locked</i> resource or an exception is thrown. This method blocks the
    * calling thread until the Lease can be locked. If the object did not exist
    * previously, null will be returned after the Lease is obtained. If the
    * lease has already been locked, it will be renewed. This method should
    * only be called on a client's thread.
    * 
    * @param oKey  the requested resource key
    * 
    * @return the requested resource or null if the resource does not exist.
    * 
    * @see #getCachedResource
     */
    public Object getLockedResource(Object oKey)
        {
        checkAccess();
        
        if (getService().lockResource(this, oKey, getStandardLeaseMillis(), -1))
            {
            return getCachedResource(oKey);
            }
        else
            {
            throw new IllegalStateException();
            }
        }
    
    // Accessor for the property "NextHandler"
    /**
     * Getter for property NextHandler.<p>
    * Reference to another handler with exact same proprties as this one except
    * the ClassLoader property
     */
    public CacheHandler getNextHandler()
        {
        return __m_NextHandler;
        }
    
    // Accessor for the property "ResourceKeys"
    /**
     * Getter for property ResourceKeys.<p>
    * Calculated property returning the Enumeration of resource keys.
     */
    public java.util.Enumeration getResourceKeys()
        {
        // import com.tangosol.util.SimpleEnumerator;
        
        return new SimpleEnumerator(getResourceMap().keySet().toArray());
        }
    
    // Accessor for the property "ResourceMap"
    /**
     * Getter for property ResourceMap.<p>
    * This Map holds the resources assosiated with the Leases held in the
    * LeaseMap.
     */
    public java.util.Map getResourceMap()
        {
        return __m_ResourceMap;
        }
    
    // Accessor for the property "Serializer"
    /**
     * Getter for property Serializer.<p>
    * A Serializer used by this CacheHandler.
    * 
    * @see setClassLoader
     */
    public com.tangosol.io.Serializer getSerializer()
        {
        return __m_Serializer;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache getService()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ReplicatedCache;
        
        return (ReplicatedCache) get_Parent();
        }
    
    // Accessor for the property "StandardLeaseMillis"
    /**
     * Getter for property StandardLeaseMillis.<p>
    * The duration of a standard Lease in milliseconds for the resources
    * controlled by this CacheHandler.
     */
    public long getStandardLeaseMillis()
        {
        return __m_StandardLeaseMillis;
        }
    
    // Accessor for the property "Validator"
    /**
     * Getter for property Validator.<p>
    * Returns a new instance of default implementation of Validator interface.
    * 
    * @deprecated since Coherence 2.3
     */
    public com.tangosol.util.TransactionMap.Validator getValidator()
        {
        return (CacheHandler.Validator) _newChild("Validator");
        }
    
    protected boolean hasListeners()
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        CacheHandler handler = this;
        
        do
            {
            com.tangosol.util.MapListenerSupport support = handler.getListenerSupport();
            if (support != null)
                {
                return true;
                }
            handler = handler.getNextHandler();
            }
        while (handler != null);
        return false;
        }
    
    private java.util.Map instantiateBackingMap(com.tangosol.net.BackingMapManager manager, String sName)
        {
        // import com.tangosol.util.ObservableMap;
        // import java.util.Map;
        
        Map map = null;
        try
            {
            map = manager.instantiateBackingMap(sName);
            if (map == null)
                {
                _trace("BackingMapManager " + manager.getClass().getName() +
                       ": returned \"null\" for a cache: " + sName, 1);
                }
            else if (!map.isEmpty())
                {
                // this could happen if the service restarted, and a custom manager
                // failed to clear the contents during releaseCache()
                map.clear();
                }
            }
        catch (RuntimeException e)
            {
            _trace("BackingMapManager " + manager.getClass().getName() +
                   ": failed to instantiate a cache: " + sName, 1);
            _trace(e);
            }
        
        if (map instanceof ObservableMap)
            {
            ((ObservableMap) map).addMapListener(instantiateBackingMapListener());
            }
        
        return map;
        }
    
    protected com.tangosol.util.MapListener instantiateBackingMapListener()
        {
        // import com.tangosol.util.MapListener;
        
        CacheHandler.BackingMapListener listener =
            (CacheHandler.BackingMapListener) _newChild("BackingMapListener");
        
        setBackingMapListener(listener);
        return listener;
        }
    
    public void invalidate()
        {
        // import Component.Util.CacheEvent as CacheEvent;
        // import com.tangosol.net.BackingMapManager;
        // import com.tangosol.net.cache.CacheEvent as com.tangosol.net.cache.CacheEvent;
        // import com.tangosol.util.Listeners;
        // import com.tangosol.util.MapListener;
        // import com.tangosol.util.ObservableMap;
        // import com.tangosol.util.SafeHashMap;
        // import java.util.Map;
        
        synchronized (this)
            {
        if (!isValid())
            {
            return;
            }
        
        Map mapResource = getResourceMap();
        
        MapListener listener = getBackingMapListener();
        if (listener != null)
            {
            ((ObservableMap) mapResource).removeMapListener(listener);
            setBackingMapListener(null);
            }
        
        BackingMapManager manager = getService().getBackingMapManager();
        if (manager != null)
            {
            String sName = getCacheName();
            if (sName != null)
                {
                try
                    {
                    manager.releaseBackingMap(sName, mapResource);
                    }
                catch (RuntimeException e)
                    {
                    _trace("BackingMapManager " + manager.getClass().getName() +
                           ": failed to release a cache: " + sName, 1);
                    _trace(e);
                    }
                }
            }
        
        
        setClassLoader(null);
        setListenerSupport(null);
        setConverterFromInternal(null);
        setConverterToInternal(null);
        
        getLeaseMap().clear();
        setResourceMap(new SafeHashMap());
        
            setValid(false);
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
        
        return com.tangosol.util.InvocableMapHelper.invokeLocked(this, com.tangosol.util.InvocableMapHelper.makeEntry(this, oKey), agent);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return invokeAll(keySet(filter), agent);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
        
        return com.tangosol.util.InvocableMapHelper.invokeAllLocked(this, com.tangosol.util.InvocableMapHelper.makeEntrySet(this, collKeys, false), agent);
        }
    
    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "Active"
    /**
     * Getter for property Active.<p>
    * Calculated property specifiying whether or not this handler is currently
    * active (has a valid ClassLoader)
     */
    public boolean isActive()
        {
        return getClassLoader() != null;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean isEmpty()
        {
        checkAccess();
        
        return getResourceMap().isEmpty();
        }
    
    // Accessor for the property "PutExpiryWarned"
    /**
     * Getter for property PutExpiryWarned.<p>
    * Log a warning the first time a 3-valued put() is called for per-entry
    * expiration.
     */
    public boolean isPutExpiryWarned()
        {
        return __m_PutExpiryWarned;
        }
    
    // Accessor for the property "UseEventDaemon"
    /**
     * Getter for property UseEventDaemon.<p>
    * If true, all map events will be dispatched on the MapEventQueue thread;
    * otherwise on the service thread itself
     */
    public boolean isUseEventDaemon()
        {
        return __m_UseEventDaemon;
        }
    
    // Accessor for the property "Valid"
    /**
     * Getter for property Valid.<p>
    * Returns false iff this CacheHandler has been explicitely invalidated
    * (destroyed).
     */
    public boolean isValid()
        {
        return __m_Valid;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set keySet()
        {
        checkAccess();
        
        CacheHandler.KeySet set = (CacheHandler.KeySet) _newChild("KeySet");
        set.setMap(this);
        set.setSet(getResourceMap().keySet());
        
        return set;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set keySet(com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.InvocableMapHelper;
        
        return InvocableMapHelper.query(this, getIndexMap(), filter, false, false, null);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean lock(Object oKey)
        {
        checkAccess();
        
        return getService().lockResource(this, oKey, getStandardLeaseMillis(), 0);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean lock(Object oKey, long cWait)
        {
        checkAccess();
        
        return getService().lockResource(this, oKey, getStandardLeaseMillis(), cWait);
        }
    
    /**
     * Farewell a service member.
     */
    public void onFarewell(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Lease;
        // import java.util.Enumeration;
        
        for (Enumeration e = getLeaseKeys(); e.hasMoreElements();)
            {
            Object oKey  = e.nextElement();
            Lease  lease = getLease(oKey);
        
            if (lease != null)
                {
                validateLease(lease);
                }
            }
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
        // import com.tangosol.net.cache.LocalCache;
        // import com.tangosol.net.cache.OverflowMap;
        // import com.tangosol.util.ObservableHashMap;
        // import com.tangosol.util.ObservableMap;
        // import com.tangosol.util.SafeHashMap;
        // import java.util.Map;
        
        super.onInit();
        
        if (getLeaseMap() == null)
            {
            Map mapLease;
        
            int nGraveyardSize = getService().getReplicatedCacheDependencies().getGraveyardSize();
            if (nGraveyardSize > 0)
                {
                ObservableMap mapFront = new ObservableHashMap();
                LocalCache    mapBack  = new LocalCache(nGraveyardSize, 0);
        
                mapBack.setEvictionType(LocalCache.EVICTION_POLICY_LRU);
        
                mapLease = new OverflowMap(mapFront, mapBack);
                }
            else
                {
                mapLease = new SafeHashMap();
                }
            setLeaseMap(mapLease);
            }
        }
    
    /**
     * Remove the specified lease. This method is called on the service thread
    * and should not be called externally.
    * 
    * @param lease  the Lease
    * 
    * @see ReplciatedCahce#onLeaseRemove
     */
    public void onLeaseRemove(com.tangosol.coherence.component.net.Lease lease)
        {
        // import Component.Net.Lease;
        // import Component.Util.CacheEvent;
        // import com.tangosol.run.component.EventDeathException;
        // import com.tangosol.util.MapEvent;
        
        Object  oKey         = lease.getResourceKey();
        Lease   leaseCurrent = getLease(oKey);
        
        if (leaseCurrent == null)
            {
            // nothing to remove
            return;
            }
        
        int iCompare = leaseCurrent.compareTo(lease);
        if (iCompare >= 0)
            {
            _trace("Rejected remove: " + leaseCurrent + "\n by " + lease, 4);
            throw new EventDeathException();
            }
        
        MapEvent event = null;
        
        synchronized (leaseCurrent)
            {
            boolean fNotify = isActive() && hasListeners() && containsKey(oKey);
            Object  oValueOld;
            
            // COH-11059: we need to set the "ignore" key on the base handler -
            //            the one that is held by the BackingMapListener
            CacheHandler handlerBase = this;
            while (true)
                {
                CacheHandler handlerNext = handlerBase.getNextHandler();
                if (handlerNext == null)
                    {
                    break;
                    }
                handlerBase = handlerNext;
                }
        
        
            handlerBase.setIgnoreKey(oKey);
            try
                {
                // the value could still be a Binary
                oValueOld = getResourceMap().remove(oKey);
                }
            finally
                {
                handlerBase.setIgnoreKey(this); // since 'null' is allowed, use something else
                }
        
            if (fNotify)
                {
                event = new MapEvent(this, MapEvent.ENTRY_DELETED, oKey, oValueOld, null);
                }
        
            if (leaseCurrent.getStatus() == Lease.LEASE_AVAILABLE)
                {
                terminateLease(oKey);
                }
            }
        
        if (event != null)
            {
            dispatchEvent(event);
            }
        }
    
    /**
     * Update a current lease as directed by the specified lease. This method is
    * called on the service thread and should not be called externally.
    * 
    * @param lease  the Lease
    * @param fUpdateResource  if true, update the resource value; otherwise
    * Lease only
    * @param oValue  the resource value; may be null
    * 
    * Important note: the value is quite likely to be a "deferred" value
    * serialized into a DataInputStream. The actual value could be retrieved
    * from the handler using getCachedResource method
    * 
    * @see ReplicatedCahce#onLeaseUpdate
     */
    public void onLeaseUpdate(com.tangosol.coherence.component.net.Lease lease, boolean fUpdateResource, Object oValue, long cExpiryMillis)
        {
        // import Component.Net.Lease;
        // import Component.Util.CacheEvent;
        // import com.tangosol.net.cache.CacheMap;
        // import com.tangosol.run.component.EventDeathException;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.MapEvent;
        // import java.util.Map;
        
        Object oKey         = lease.getResourceKey();
        Lease  leaseCurrent = ensureLease(oKey);
        
        int iCompare = leaseCurrent.compareTo(lease);
        if (iCompare >= 0)
            {
            _trace("Rejected update: " + leaseCurrent + "\n by " + lease, 4);
            throw new EventDeathException();
            }
        
        MapEvent event = null;
        
        synchronized (leaseCurrent)
            {
            leaseCurrent.copyFrom(lease);
        
            Map mapResource = getResourceMap();
            if (fUpdateResource)
                {
                int nEventId = 0;
        
                if (isActive() && hasListeners())
                    {
                    nEventId = containsKey(oKey) ?
                        MapEvent.ENTRY_UPDATED : MapEvent.ENTRY_INSERTED;
                    }
        
                // both new and old values could still be Binary objects
                leaseCurrent.setClassLoader(
                    oValue instanceof Binary ? null : getClassLoader());
        
                Object oValueOld;
                if (mapResource instanceof CacheMap)
                    {
                    oValueOld = ((CacheMap) mapResource).put(oKey, oValue, cExpiryMillis);
                    }
                else
                    {
                    if (cExpiryMillis > 0)
                        {
                        _trace("UnsupportedOperation: " + "class \"" +
                            mapResource.getClass().getName() +
                            "\" does not implement CacheMap interface", 1);
                        }
                    oValueOld = mapResource.put(oKey, oValue);
                    }
        
                if (nEventId > 0)
                    {
                    event = new MapEvent(this, nEventId, oKey, oValueOld, oValue);
                    }
                }
            else // lock, unlock
                {
                if (leaseCurrent.getStatus() == Lease.LEASE_AVAILABLE &&
                        !mapResource.containsKey(oKey))
                    {
                    // unlocked lease w/out resource -- expire
                    terminateLease(oKey);
                    }
                }
            }
        
        if (event != null)
            {
            dispatchEvent(event);
            }
        }
    
    /**
     * Populate the cache using the specified message. This method is called on
    * the service thread and should not be called externally.
     */
    public void populateCache(com.tangosol.coherence.component.net.message.CacheMessage msg)
        {
        // import Component.Net.Lease;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ReplicatedCache;
        // import com.tangosol.run.component.EventDeathException;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import com.tangosol.io.ByteArrayReadBuffer;
        // import com.tangosol.io.ReadBuffer$BufferInput as com.tangosol.io.ReadBuffer.BufferInput;
        // import java.io.IOException;
        
        ReplicatedCache      service = getService();
        int                  iCache  = getCacheIndex();
        int                  cLease  = msg.getLeaseCount();
        com.tangosol.io.ReadBuffer.BufferInput                input   = new ByteArrayReadBuffer(msg.getCacheData()).getBufferInput();
        
        for (int i = 0; i < cLease; i++)
            {
            try
                {
                Object oKey  = msg.readObject(input);
                Lease  lease = Lease.instantiate(iCache, oKey, service);
        
                lease.read(input);
        
                Binary binResource = (Binary) msg.readObject(input);
                if (binResource == null)
                    {
                    onLeaseUpdate(lease, false, null, 0L);
                    }
                else
                    {
                    lease.setResourceSize(binResource.length());
                    onLeaseUpdate(lease, true, binResource, 0L);
                    }
                }
            catch (EventDeathException e)
                {
                continue;
                }
            catch (IOException e)
                {
                _trace("An exception (" + e +
                    ") occurred while populating cache: " + this, 1);
                _trace(e);
                break;
                }
            }
        }
    
    /**
     * Populate the specified message with the content of the cache. This method
    * is called on the service thread and should not be called externally.
    * 
    * @param msg the CacheMessage object to be filled with cache data
    * @param cbSize  prefered message size
    * @param enum  an Enumeration object that contains items remained to be
    * processed from the previous invocation
    * 
    * @return an Enumeration object that contains the remaining items; null if
    * all the resources in this cache have been processed
     */
    public java.util.Enumeration populateUpdateMessage(com.tangosol.coherence.component.net.message.CacheMessage msg, int cbSize, java.util.Enumeration enumLeaseKeys)
        {
        // import Component.Net.Lease;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import com.tangosol.io.Serializer;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper;
        // import com.tangosol.util.WrapperException;
        // import com.tangosol.io.ByteArrayWriteBuffer;
        // import com.tangosol.io.WriteBuffer$BufferOutput as com.tangosol.io.WriteBuffer.BufferOutput;
        // import java.io.ByteArrayOutputStream;
        // import java.io.DataOutputStream;
        // import java.io.IOException;
        // import java.util.Enumeration;
        // import java.util.Map;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid               service     = getService();
        int                   nThisId     = service.getThisMember().getId();
        int                   nOldestId   = service.getServiceOldestMember().getId();
        Serializer            serializer  = getSerializer();
        Map                   mapResource = getResourceMap();
        ByteArrayOutputStream streamBytes = new ByteArrayOutputStream(1024);
        DataOutputStream      streamData  = new DataOutputStream(streamBytes);
        ByteArrayWriteBuffer  resBytes    = null; // temporary resource buffer
        com.tangosol.io.WriteBuffer.BufferOutput                output      = null;
        int                   cLease      = 0;
        
        if (enumLeaseKeys == null)
            {
            enumLeaseKeys = getLeaseKeys();
            }
        
        while (enumLeaseKeys.hasMoreElements())
            {
            Object oKey  = enumLeaseKeys.nextElement();
            Lease  lease = getLease(oKey);
        
            if (lease == null)
                {
                continue;
                }
        
            lease.validate();
        
            boolean fInclude = false;
            switch (lease.getStatus())
                {
                case Lease.LEASE_UNISSUED:
                    // the issuer is gone, the senior service member
                    // becomes the issuer (consider a buddy instead)
                    lease.setIssuerId(nOldestId);
                    // fall through
        
                case Lease.LEASE_AVAILABLE:
                case Lease.LEASE_LOCKED:
                case Lease.LEASE_DIRTY:
                    fInclude = (lease.getIssuerId() == nThisId);
                    break;
        
                default:
                    throw new IllegalStateException();
                }
        
            if (fInclude)
                {
                // inform the newcomer about this resource
                try
                    {
                    Binary binResource = null;
        
                    if (mapResource.containsKey(oKey))
                        {
                        Object oResource = mapResource.get(oKey);
                        binResource = oResource instanceof Binary ? (Binary) oResource :
                                        ExternalizableHelper.toBinary(oResource, serializer);
                        }
        
                    // serialize the resource into a temp buffer to isolate serialization exceptions
                    if (resBytes == null)
                        {
                        int cb = lease.getResourceSize();
                        resBytes = new ByteArrayWriteBuffer(cb <= 0 ? 256 : 32 + cb);
                        }
                    else
                        {
                        resBytes.clear();
                        }
        
                    output = resBytes.getBufferOutput();
                    
                    msg.writeObject(output, oKey);
                    lease.write(output);
                    msg.writeObject(output, binResource);
                    }
                catch (IOException e)
                    {
                    _trace("An exception (" + e +
                        ") occurred while serializing " + lease +
                        " for " + this, 1);
                    _trace(e);
                    continue;
                    }
        
                try
                    {
                    streamData.write(resBytes.toByteArray());
                    }
                catch (IOException e)
                    {
                    // should not happen
                    _trace("An exception (" + e +
                        ") occurred while streaming the message " + msg +
                        " for " + this, 1);
                    _trace(e);
        
                    // the stream could be corrupted -- no recourse
                    msg.setLeaseCount(0);
                    msg.setCacheData(new byte[0]);
                    return null;
                    }
        
                cLease++;
        
                if (streamData.size() > cbSize)
                    {
                    break;
                    }
                }
            }
        
        msg.setLeaseCount(cLease);
        msg.setCacheData(streamBytes.toByteArray());
        
        return enumLeaseKeys.hasMoreElements() ? enumLeaseKeys : null;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object put(Object oKey, Object oValue)
        {
        return put(oKey, oValue, 0L, true);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        // import com.tangosol.net.cache.CacheMap;
        
        if (cMillis != CacheMap.EXPIRY_DEFAULT && !isPutExpiryWarned())
            {
            _trace("ReplicatedCache does not support per-entry expiration; " +
                    "proceeding with the cache-configured expiry.", 2);
            setPutExpiryWarned(true);
            }
        
        return put(oKey, oValue, cMillis, true);
        }
    
    /**
     * @param fReturn  if true, the return value is required; otherwise it will
    * be ignored
     */
    public Object put(Object oKey, Object oValue, long cMillis, boolean fReturn)
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import java.util.ConcurrentModificationException;
        
        checkAccess();
        
        ConcurrentModificationException cme = null;
        
        for (int i = 1, c = MAX_MAP_RETRIES; i <= c; ++i)
            {
            try
                {
                return getService().updateResource(
                    this, oKey, oValue, cMillis, false, fReturn);
                }
            catch (ConcurrentModificationException e)
                {
                cme = e;
                }
        
            try
                {
                Blocking.sleep(i << 4);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                break;
                }
            }
        
        throw cme;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void putAll(java.util.Map map)
        {
        // import java.util.Iterator;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
            {
            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
        
            put(entry.getKey(), entry.getValue(), 0L, false);
            }
        }
    
    public Object putFinal(Object oKey, Object oValue, boolean fReturn)
            throws java.util.ConcurrentModificationException
        {
        checkAccess();
        
        return getService().updateResource(this, oKey, oValue, 0L, true, fReturn);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void release()
        {
        getCacheService().releaseCache(this);
        }
    
    /**
     * Release the class loader used by this handler.
     */
    public void releaseClassLoader()
        {
        // import Component.Net.Lease;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ReplicatedCache;
        // import java.util.Enumeration;
        
        // serialize all the relevant resources to release the class loader
        ClassLoader loader = getClassLoader();
        if (loader != null)
            {
            // deactivate the handler before proceeding
            setClassLoader(null);
        
            ReplicatedCache service = getService();
            for (Enumeration e = getLeaseKeys(); e.hasMoreElements();)
                {
                Object oKey  = e.nextElement();
                Lease  lease = getLease(oKey);
        
                if (lease != null)
                    {
                    if (service.getThreadStatus(lease) == Lease.LEASE_LOCKED)
                        {
                        service.unlockResource(this, oKey);
                        }
        
                    if (lease.getClassLoader() == loader)
                        {
                        releaseClassLoader(lease);
                        }
                    }
                }
            }
        }
    
    /**
     * Make sure that resource represented by the specified lease is stored in a
    * serialized form and does not depend on a class loader.
    * 
    * @see #getCachedResource
     */
    protected Object releaseClassLoader(com.tangosol.coherence.component.net.Lease lease)
        {
        // import com.tangosol.util.WrapperException;
        // import java.util.Map;
        
        synchronized (lease)
            {
            Map    mapResource = getResourceMap();
            Object oKey        = lease.getResourceKey();
            Object oValue      = mapResource.get(oKey);
            
            lease.setClassLoader(null);
            if (oValue != null || mapResource.containsKey(oKey))
                {
                try
                    {
                    Object oInternal = getConverterToInternal().convert(oValue);
                    if (oInternal != oValue)
                        {
                        mapResource.put(oKey, oValue = oInternal);
                        }
                    }
                catch (WrapperException e)
                    {
                    throw new WrapperException(e.getOriginalException(),
                        "CacheName=" + getCacheName() + ", Key=" + oKey);
                    }
                }
            return oValue;
            }
        }
    
    /**
     * Remove each index contained in the  map of indexes that is maintaned by
    * this cache handler.
     */
    public void releaseIndexMap()
        {
        // import com.tangosol.util.ValueExtractor;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Map;
        
        Map mapIndex = getIndexMap();
        if (mapIndex != null)
            {
            HashSet setExtractors = new HashSet(mapIndex.keySet());
            for (Iterator iter = setExtractors.iterator(); iter.hasNext();)
                {
                removeIndex((ValueExtractor) iter.next());
                }
            setIndexMap(null);
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object remove(Object oKey)
        {
        return remove(oKey, true);
        }
    
    /**
     * @param fReturn  if true, the return value is required; otherwise it will
    * be ignored
     */
    public Object remove(Object oKey, boolean fReturn)
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import java.util.ConcurrentModificationException;
        
        checkAccess();
        
        ConcurrentModificationException cme = null;
        
        for (int i = 1, c = MAX_MAP_RETRIES; i <= c; ++i)
            {
            try
                {
                return getService().removeResource(this, oKey, fReturn);
                }
            catch (ConcurrentModificationException e)
                {
                cme = e;
                }
        
            try
                {
                Blocking.sleep(i << 4);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                break;
                }
            }
        
        throw cme;
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
    public void removeMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;
        
        removeMapListener(listener, (Filter) null);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public synchronized void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
        {
        // import com.tangosol.internal.net.NamedCacheDeactivationListener;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        if (listener instanceof NamedCacheDeactivationListener)
            {
            getDeactivationListeners().remove(listener);
            }
        else
            {
            _assert(listener != null);
        
            com.tangosol.util.MapListenerSupport support = getListenerSupport();
            if (support != null)
                {
                support.removeListener(wrap(listener), filter);
                if (support.isEmpty())
                    {
                    setListenerSupport(null);
                    }
                }
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public synchronized void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        _assert(listener != null);
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (support != null)
            {
            support.removeListener(wrap(listener), oKey);
            if (support.isEmpty())
                {
                setListenerSupport(null);
                }
            }
        }
    
    // Accessor for the property "BackingMapListener"
    /**
     * Setter for property BackingMapListener.<p>
    * BackingMap listener. Used only if a custom backing map manager uses an
    * ObservableMap to implement the local storage.
     */
    protected void setBackingMapListener(com.tangosol.util.MapListener listener)
        {
        __m_BackingMapListener = listener;
        }
    
    // Accessor for the property "CacheIndex"
    /**
     * Setter for property CacheIndex.<p>
    * This index of this handler in the indexed CacheHandler property
    * maintained by the cache service.
    * 
    * @see ReplicatedCache#instantiateCacheHandler
     */
    public void setCacheIndex(int index)
        {
        if (is_Constructed())
            {
            int indexOld = getCacheIndex();
        
            if (indexOld != -1 && index != indexOld)
                {
                throw new IllegalStateException("Attempt to modify the CacheIndex: " +
                    this + " to " + index);
                }
            }
        __m_CacheIndex = (index);
        }
    
    // Accessor for the property "CacheName"
    /**
     * Setter for property CacheName.<p>
    * This name of the cache that is managed by this handler. It could only be
    * null if the cached resources started coming from another member, but the
    * Catalog has not been updated yet. It could only be empty (length == 0)
    * for the Catalog Cache handler.
    * 
    * @see ReplicatedCache#instantiateCacheHandler
     */
    public void setCacheName(String sName)
        {
        // import com.tangosol.net.BackingMapManager as com.tangosol.net.BackingMapManager;
        // import com.tangosol.util.SafeHashMap;
        // import java.util.Map;
        
        if (is_Constructed())
            {
            String sNameOld = getCacheName();
            if (sNameOld != null && !sNameOld.equals(sName))
                {
                throw new IllegalStateException("Attempt to modify the CacheName: " +
                    this + " to " + sName);
                }
        
            com.tangosol.net.BackingMapManager manager = getService().getBackingMapManager();
            Map     map     = getResourceMap();
            boolean fClone  = getNextHandler() != null;
        
            if (fClone)
                {
                _assert(map != null, "Resource map must be copied");
                }
            else if (map == null)
                {
                if (manager != null)
                    {
                    if (sName == null)
                        {
                        // temporary store replacement
                        // in the unlikely event that a cache entry has
                        // been reported prior to the cache being set
                        _trace("Creating a temporary map: " + getCacheIndex(), 5);
                        }
                    else
                        {
                        map = instantiateBackingMap(manager, sName);
                        }
                    }
                setResourceMap(map == null ? new SafeHashMap() : map);
                }
            else if (sName != null && sNameOld == null && manager != null)
                {
                // copy the entries from a temporary store
                Map mapActual = instantiateBackingMap(manager, sName);
                if (mapActual != null)
                    {
                    if (!map.isEmpty())
                        {
                        _trace("Transferring " + map.size() + " to: " + sName, 5);
                        mapActual.putAll(map);
                        }
                    setResourceMap(mapActual);
                    }
                }
            }
        
        __m_CacheName = (sName);
        }
    
    // Accessor for the property "ClassLoader"
    /**
     * Setter for property ClassLoader.<p>
    * ClassLoader that should be used by ResourceMessages to deserialize
    * resources that are managed by this handler. The only situations when the
    * ClassLoader could be null are:
    * <ul>
    * <li>the cache has already been replicated by the service, but no client
    * is yet connected to this map
    * <li>the client has called the "release" on the last copy of the cache
    * handler with a given name
    * </ul>
    * 
    * @see #getCachedResource
     */
    public synchronized void setClassLoader(ClassLoader loader)
        {
        __m_ClassLoader = (loader);
        
        // invalidate the "From" converter that uses ClassLoader
        setConverterFromInternal(null);
        
        setSerializer(loader == null ? null : getService().ensureSerializer(loader));
        }
    
    // From interface: com.tangosol.io.ClassLoaderAware
    public void setContextClassLoader(ClassLoader loader)
        {
        throw new UnsupportedOperationException();
        }
    
    // Accessor for the property "ConverterFromInternal"
    /**
     * Setter for property ConverterFromInternal.<p>
    * A converter that takes service specific "transmittable" serializable
    * objects and converts them via deserialization (etc.) to the objects
    * expected by clients of the cache.
     */
    protected void setConverterFromInternal(com.tangosol.util.Converter conv)
        {
        __m_ConverterFromInternal = conv;
        }
    
    // Accessor for the property "ConverterToInternal"
    /**
     * Setter for property ConverterToInternal.<p>
    * A converter that takes ClassLoader dependent objects and converts them
    * via serialization into Binary objects.
     */
    protected void setConverterToInternal(com.tangosol.util.Converter pConverterToInternal)
        {
        __m_ConverterToInternal = pConverterToInternal;
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
    
    // Accessor for the property "IgnoreKey"
    /**
     * Setter for property IgnoreKey.<p>
    * The key of a resource in the local storage that is currently being
    * processed by the service and therefore should be ignored by the
    * BackingMapListener.
     */
    protected void setIgnoreKey(Object oKey)
        {
        __m_IgnoreKey = oKey;
        }
    
    // Accessor for the property "IndexMap"
    /**
     * Setter for property IndexMap.<p>
    * This Map assosiates resource keys with the corresponding Lease objects
    * for each leased resource. Its key set is a subset of the ResourceMap key
    * set.
     */
    public void setIndexMap(java.util.Map map)
        {
        __m_IndexMap = map;
        }
    
    // Accessor for the property "LeaseMap"
    /**
     * Setter for property LeaseMap.<p>
    * This Map assosiates resource keys with the corresponding Lease objects
    * for each leased resource. Its key set is a subset of the ResourceMap key
    * set.
     */
    public void setLeaseMap(java.util.Map map)
        {
        __m_LeaseMap = map;
        }
    
    // Accessor for the property "ListenerSupport"
    /**
     * Setter for property ListenerSupport.<p>
    * Registered listeners to the MapEvent notifications.
     */
    protected void setListenerSupport(com.tangosol.util.MapListenerSupport support)
        {
        __m_ListenerSupport = support;
        }
    
    // Accessor for the property "NextHandler"
    /**
     * Setter for property NextHandler.<p>
    * Reference to another handler with exact same proprties as this one except
    * the ClassLoader property
     */
    public void setNextHandler(CacheHandler handler)
        {
        __m_NextHandler = handler;
        }
    
    // Accessor for the property "PutExpiryWarned"
    /**
     * Setter for property PutExpiryWarned.<p>
    * Log a warning the first time a 3-valued put() is called for per-entry
    * expiration.
     */
    protected void setPutExpiryWarned(boolean fWarned)
        {
        __m_PutExpiryWarned = fWarned;
        }
    
    // Accessor for the property "ResourceMap"
    /**
     * Setter for property ResourceMap.<p>
    * This Map holds the resources assosiated with the Leases held in the
    * LeaseMap.
     */
    public void setResourceMap(java.util.Map map)
        {
        __m_ResourceMap = map;
        }
    
    // Accessor for the property "Serializer"
    /**
     * Setter for property Serializer.<p>
    * A Serializer used by this CacheHandler.
    * 
    * @see setClassLoader
     */
    protected void setSerializer(com.tangosol.io.Serializer serializer)
        {
        __m_Serializer = serializer;
        }
    
    // Accessor for the property "StandardLeaseMillis"
    /**
     * Setter for property StandardLeaseMillis.<p>
    * The duration of a standard Lease in milliseconds for the resources
    * controlled by this CacheHandler.
     */
    public void setStandardLeaseMillis(long lMillis)
        {
        __m_StandardLeaseMillis = (Math.max(0L, lMillis));
        }
    
    // Accessor for the property "UseEventDaemon"
    /**
     * Setter for property UseEventDaemon.<p>
    * If true, all map events will be dispatched on the MapEventQueue thread;
    * otherwise on the service thread itself
     */
    public void setUseEventDaemon(boolean pUseEventDaemon)
        {
        __m_UseEventDaemon = pUseEventDaemon;
        }
    
    // Accessor for the property "Valid"
    /**
     * Setter for property Valid.<p>
    * Returns false iff this CacheHandler has been explicitely invalidated
    * (destroyed).
     */
    protected void setValid(boolean fValid)
        {
        __m_Valid = fValid;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public int size()
        {
        checkAccess();
        
        return getResourceMap().size();
        }
    
    /**
     * Move the specified Lease to the graveyard.
    * 
    * @param oKey  the resource key
     */
    public void terminateLease(Object oKey)
        {
        // import com.tangosol.net.cache.OverflowMap;
        // import java.util.Map;
        
        Map mapLease = getLeaseMap();
        if (mapLease instanceof OverflowMap)
            {
            Map mapFront = ((OverflowMap) mapLease).getFrontMap();
            mapFront.remove(oKey);
            }
        else
            {
            mapLease.remove(oKey);
            }
        }
    
    // Declared at the super level
    public String toString()
        {
        StringBuffer sb = new StringBuffer();
        
        sb.append(get_Name())
          .append("{Name=")
          .append(getCacheName())
          .append(", Index=")
          .append(getCacheIndex())
          .append(", ServiceName=")
          .append(getService().getServiceName());
        
        if (isValid())
            {
            sb.append(", ClassLoader=")
              .append(getClassLoader());
            }
        else
            {
            sb.append(", INVALID");
            }
        sb.append('}');
        
        return sb.toString();
        }
    
    // From interface: com.tangosol.run.xml.XmlSerializable
    public com.tangosol.run.xml.XmlElement toXml()
        {
        // import com.tangosol.run.xml.SimpleElement;
        
        SimpleElement xml = new SimpleElement(get_Name());
        
        xml.addElement("CacheName")          .setString(getCacheName());
        xml.addElement("CacheIndex")         .setInt   (getCacheIndex());
        xml.addElement("StandardLeaseMillis").setLong  (getStandardLeaseMillis());
        
        return xml;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean unlock(Object oKey)
        {
        checkAccess();
        
        return getService().unlockResource(this, oKey);
        }
    
    /**
     * Validate and possibly change the lease due to the membership change
     */
    protected void validateLease(com.tangosol.coherence.component.net.Lease lease)
        {
        // import Component.Net.Lease;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
        int     nThisId = service.getThisMember().getId();
        
        // check whether this lease has become an orphan
        lease.validate();
        
        switch (lease.getStatus())
            {
            case Lease.LEASE_UNISSUED:
                // the issuer is gone, the senior service member
                // becomes the issuer (consider a buddy instead)
                lease.setIssuerId(service.getServiceOldestMember().getId());
        
                // break through
            case Lease.LEASE_AVAILABLE:
                // if (lease.getIssuerId() == nThisId) // commented out 2.3b261
                    {
                    // we cannot use "handler.containsKey()" here
                    // since the handler may be not "active"
                    Object oKey = lease.getResourceKey();
                    if (!getResourceMap().containsKey(oKey))
                        {
                        // an unlocked lease without a resource - move to the graveyard
                        terminateLease(oKey);
                        }
                    }
                break;
        
            case Lease.LEASE_LOCKED:
            case Lease.LEASE_DIRTY:
                if (lease.getIssuerId() == 0)
                    {
                    // the issuer is gone;
                    // the holder becomes the issuer
                    lease.setIssuerId(lease.getHolderId());
                    }
                break;
        
            default:
                throw new IllegalStateException();
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Collection values()
        {
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.NullImplementation;
        
        checkAccess();
        
        // the values could still be in serialized form
        // to minimize memory use let's lazily convert the keys into values
        // (see convert())
        return ConverterCollections.getCollection(getResourceMap().keySet(),
            this, NullImplementation.getConverter());
        }
    
    /**
     * Wrap the specified listener into a ContainerContext aware listener.
     */
    protected com.tangosol.util.MapListener wrap(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.application.ContainerHelper;
        
        return ContainerHelper.getContextAwareListener(getService(), listener);
        }

    // ---- class: com.tangosol.coherence.component.util.CacheHandler$BackingMapListener
    
    /**
     * The backing map listener. Used only if a custom backing map manager uses
     * an ObservableMap to implement the local storage.
     * 
     * @see CacheHandler#instantiateBackingMap
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BackingMapListener
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.util.MapListener
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public BackingMapListener()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public BackingMapListener(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.CacheHandler.BackingMapListener();
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
                clz = Class.forName("com.tangosol.coherence/component/util/CacheHandler$BackingMapListener".replace('/', '.'));
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
        
        // From interface: com.tangosol.util.MapListener
        public void entryDeleted(com.tangosol.util.MapEvent evt)
            {
            // import com.tangosol.util.Base;
            
            CacheHandler handler = (CacheHandler) get_Parent();
            Object  oKey    = evt.getKey();
            
            if (!Base.equals(handler.getIgnoreKey(), oKey))
                {
                // this could fire the "LeasePrune" event (to inform other nodes)
            
                handler.terminateLease(oKey);
                }
            }
        
        // From interface: com.tangosol.util.MapListener
        public void entryInserted(com.tangosol.util.MapEvent evt)
            {
            }
        
        // From interface: com.tangosol.util.MapListener
        public void entryUpdated(com.tangosol.util.MapEvent evt)
            {
            }
        }

    // ---- class: com.tangosol.coherence.component.util.CacheHandler$EntrySet
    
    /**
     * @see CacheHandler#entrySet
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EntrySet
            extends    com.tangosol.coherence.component.util.collections.wrapperSet.EntrySet
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
            __mapChildren.put("Entry", CacheHandler.EntrySet.Entry.get_CLASS());
            __mapChildren.put("Iterator", CacheHandler.EntrySet.Iterator.get_CLASS());
            }
        
        // Default constructor
        public EntrySet()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public EntrySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.CacheHandler.EntrySet();
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
                clz = Class.forName("com.tangosol.coherence/component/util/CacheHandler$EntrySet".replace('/', '.'));
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
        public boolean remove(Object o)
            {
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            CacheHandler handler = (CacheHandler) getMap();
            java.util.Map.Entry   entry   = (java.util.Map.Entry) o;
            Object oKey  = entry == null ? null : entry.getKey();
            
            if (handler.containsKey(oKey))
                {
                handler.remove(oKey, false);
                return true;
                }
            else
                {
                return false;
                }
            }

        // ---- class: com.tangosol.coherence.component.util.CacheHandler$EntrySet$Entry
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Entry
                extends    com.tangosol.coherence.component.util.collections.wrapperSet.EntrySet.Entry
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Entry()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Entry(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.CacheHandler.EntrySet.Entry();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/CacheHandler$EntrySet$Entry".replace('/', '.'));
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
            
            // Declared at the super level
            public Object getValue()
                {
                // the values could still be in serialized form
                return ((CacheHandler) get_Module()).convert(getKey());
                }
            
            // Declared at the super level
            public Object setValue(Object oValue)
                {
                // the values could still be in serialized form
                Object oValueOld = getValue();
                super.setValue(oValue);
                return oValueOld;
                }
            
            // Declared at the super level
            public String toString()
                {
                return get_Name() + ": key=\"" + getKey() + "\", value=\"" + getValue() + '"';
                }
            }

        // ---- class: com.tangosol.coherence.component.util.CacheHandler$EntrySet$Iterator
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Iterator
                extends    com.tangosol.coherence.component.util.collections.wrapperSet.EntrySet.Iterator
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Iterator()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.CacheHandler.EntrySet.Iterator();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/CacheHandler$EntrySet$Iterator".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.util.CacheHandler$KeySet
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class KeySet
            extends    com.tangosol.coherence.component.util.collections.wrapperSet.KeySet
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
            __mapChildren.put("Iterator", CacheHandler.KeySet.Iterator.get_CLASS());
            }
        
        // Default constructor
        public KeySet()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public KeySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.CacheHandler.KeySet();
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
                clz = Class.forName("com.tangosol.coherence/component/util/CacheHandler$KeySet".replace('/', '.'));
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
        public boolean remove(Object o)
            {
            CacheHandler handler = (CacheHandler) getMap();
            
            if (handler.containsKey(o))
                {
                handler.remove(o, false);
                return true;
                }
            else
                {
                return false;
                }
            }

        // ---- class: com.tangosol.coherence.component.util.CacheHandler$KeySet$Iterator
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Iterator
                extends    com.tangosol.coherence.component.util.collections.WrapperSet.Iterator
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Iterator()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.CacheHandler.KeySet.Iterator();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/CacheHandler$KeySet$Iterator".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.util.CacheHandler$Validator
    
    /**
     * The default Validator that uses the Lease version to resolve the  update
     * conflicts [in the case of optimistic concurrency].
     * 
     * Deprecated since Coherence 2.3
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Validator
            extends    com.tangosol.coherence.component.util.TransactionValidator
        {
        // ---- Fields declarations ----
        
        /**
         * Property LeaseMap
         *
         * Map holding the Lease objects for resources enlisted into a
         * transaction
         */
        private transient com.tangosol.util.SafeHashMap __m_LeaseMap;
        
        // Default constructor
        public Validator()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Validator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setLeaseMap(new com.tangosol.util.SafeHashMap());
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
            return new com.tangosol.coherence.component.util.CacheHandler.Validator();
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
                clz = Class.forName("com.tangosol.coherence/component/util/CacheHandler$Validator".replace('/', '.'));
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
        public void enlist(com.tangosol.util.TransactionMap map, Object oKey)
            {
            // import Component.Net.Lease;
            
            CacheHandler handler = (CacheHandler) get_Parent();
            _assert(handler == map.getBaseMap());
            
            Lease lease = handler.getLease(oKey);
            getLeaseMap().put(oKey, lease == null ? null : lease.clone());
            
            super.enlist(map, oKey);
            }
        
        // Accessor for the property "LeaseMap"
        /**
         * Getter for property LeaseMap.<p>
        * Map holding the Lease objects for resources enlisted into a
        * transaction
         */
        public com.tangosol.util.SafeHashMap getLeaseMap()
            {
            return __m_LeaseMap;
            }
        
        // Accessor for the property "LeaseMap"
        /**
         * Setter for property LeaseMap.<p>
        * Map holding the Lease objects for resources enlisted into a
        * transaction
         */
        public void setLeaseMap(com.tangosol.util.SafeHashMap pLeaseMap)
            {
            __m_LeaseMap = pLeaseMap;
            }
        
        // Declared at the super level
        public void validate(com.tangosol.util.TransactionMap map, java.util.Set setInsert, java.util.Set setUpdate, java.util.Set setDelete, java.util.Set setRead, java.util.Set setFanthom)
                throws java.util.ConcurrentModificationException
            {
            // import java.util.ConcurrentModificationException;
            
            super.validate(map, setInsert, setUpdate, setDelete, setRead, setFanthom);
            
            CacheHandler handler = (CacheHandler) get_Parent();
            _assert(handler == map.getBaseMap());
            
            // inserts -- there should be no value
            validateInsert(map, setInsert);
            
            // updates -- the versions should be the same
            validateVersion(map, setUpdate);
            
            // deletes -- the versions should be the same
            validateVersion(map, setDelete);
            
            // reads -- the values should be the same
            validateValue(map, setRead);
            
            // fanthoms -- should be empty
            
            boolean fFail = false;
            String  sMsg  = "";
            if (!setInsert.isEmpty())
                {
                sMsg  += "\ninserts=" + setInsert;
                fFail  = true;
                }
            if (!setUpdate.isEmpty())
                {
                sMsg  += "\nupdates=" + setUpdate;
                fFail  = true;
                }
            if (!setDelete.isEmpty())
                {
                sMsg  += "\ndeletes=" + setDelete;
                fFail  = true;
                }
            if (!setRead.isEmpty())
                {
                sMsg  += "\nreads=" + setRead;
                fFail  = true;
                }
            if (!setFanthom.isEmpty())
                {
                sMsg  += "\nfanthoms=" + setFanthom;
                fFail  = true;
                }
            
            if (fFail)
                {
                throw new ConcurrentModificationException("validation failed: " + sMsg);
                }
            }
        
        protected void validateInsert(com.tangosol.util.TransactionMap map, java.util.Set set)
            {
            // import com.tangosol.util.Base;
            // import java.util.Iterator;
            // import java.util.Map;
            
            Map mapBase = map.getBaseMap();
            
            for (Iterator iter = set.iterator(); iter.hasNext();)
                {
                Object oKey = iter.next();
            
                if (!mapBase.containsKey(oKey))
                    {
                    iter.remove();
                    }
                }
            }
        
        protected void validateValue(com.tangosol.util.TransactionMap map, java.util.Set set)
            {
            // import com.tangosol.util.Base;
            // import java.util.Iterator;
            // import java.util.Map;
            
            Map mapBase = map.getBaseMap();
            
            for (Iterator iter = set.iterator(); iter.hasNext();)
                {
                Object oKey     = iter.next();
                Object oValBase = mapBase.get(oKey);
                Object oValCurr = map.get(oKey);
            
                if (Base.equals(oValBase, oValCurr))
                    {
                    iter.remove();
                    }
                }
            }
        
        protected void validateVersion(com.tangosol.util.TransactionMap map, java.util.Set set)
            {
            // import Component.Net.Lease;
            // import java.util.Iterator;
            // import java.util.Map;
            
            CacheHandler handler  = (CacheHandler) get_Parent();
            Map     mapLease = getLeaseMap();
            
            for (Iterator iter = set.iterator(); iter.hasNext();)
                {
                Object oKey      = iter.next();
                Lease  leaseOrig = (Lease) mapLease.get(oKey);
                Lease  leaseCurr = handler.getLease(oKey);
            
                if (leaseOrig != null && leaseCurr != null &&
                    leaseOrig.getResourceVersion() == leaseCurr.getResourceVersion())
                    {
                    iter.remove();
                    }
                }
            }
        }
    }
