
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService

package com.tangosol.coherence.component.net.extend.remoteService;

import com.tangosol.coherence.component.net.extend.RemoteNamedCache;
import com.tangosol.coherence.component.net.extend.protocol.CacheServiceProtocol;
import com.tangosol.coherence.component.net.extend.protocol.NamedCacheProtocol;
import com.tangosol.internal.net.service.extend.remote.DefaultRemoteCacheServiceDependencies;
import com.tangosol.internal.net.service.extend.remote.RemoteCacheServiceDependencies;
import com.tangosol.net.CacheService;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionInitiator;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.util.IteratorEnumerator;
import com.tangosol.util.NullImplementation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import javax.security.auth.Subject;

/**
 * CacheService implementation that allows a JVM to use a remote CacheService
 * without having to join the Cluster.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class RemoteCacheService
        extends    com.tangosol.coherence.component.net.extend.RemoteService
        implements com.tangosol.net.CacheService
    {
    // ---- Fields declarations ----
    
    /**
     * Property DeferKeyAssociationCheck
     *
     * Whether a key should be checked for KeyAssociation by the extend client
     * (false) or deferred until the key is received by the PartionedService
     * (true).
     */
    private boolean __m_DeferKeyAssociationCheck;
    
    /**
     * Property ScopedCacheStore
     *
     * Store of cache references, optionally scoped by Subject.
     */
    private com.tangosol.net.internal.ScopedCacheReferenceStore __m_ScopedCacheStore;
    
    // Default constructor
    public RemoteCacheService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public RemoteCacheService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setMemberListeners(new com.tangosol.util.Listeners());
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setScopedCacheStore(new com.tangosol.net.internal.ScopedCacheReferenceStore());
            setServiceListeners(new com.tangosol.util.Listeners());
            setServiceVersion("14");
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
        return new com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/remoteService/RemoteCacheService".replace('/', '.'));
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
    /**
     * Create a new Default dependencies object by cloning the input
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone.
    * 
    * @return DefaultRemoteServiceDependencies  the cloned dependencies
     */
    protected com.tangosol.internal.net.service.extend.remote.DefaultRemoteServiceDependencies cloneDependencies(com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.extend.remote.DefaultRemoteCacheServiceDependencies;
        // import com.tangosol.internal.net.service.extend.remote.RemoteCacheServiceDependencies;
        
        return new DefaultRemoteCacheServiceDependencies((RemoteCacheServiceDependencies) deps);
        }
    
    // Declared at the super level
    public void connectionClosed(com.tangosol.net.messaging.ConnectionEvent evt)
        {
        releaseCaches();
        super.connectionClosed(evt);
        }
    
    // Declared at the super level
    public void connectionError(com.tangosol.net.messaging.ConnectionEvent evt)
        {
        releaseCaches();
        super.connectionError(evt);
        }
    
    /**
     * Create a new RemoteNamedCache for the given NamedCache name and
    * ClassLoader.
    * 
    * @param sName  the name of the new RemoteNamedCache
    * @param loader   the ClassLoader used by the RemoteNamedCache to
    * deserialize objects
    * 
    * @return a new RemoteNamedCache
     */
    protected com.tangosol.coherence.component.net.extend.RemoteNamedCache createRemoteNamedCache(String sName, ClassLoader loader)
        {
        // import Component.Net.Extend.MessageFactory.CacheServiceFactory$EnsureCacheRequest as com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.EnsureCacheRequest;
        // import Component.Net.Extend.RemoteNamedCache;
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Connection;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.net.security.SecurityHelper;
        // import com.tangosol.util.NullImplementation;
        // import java.net.URI;
        // import java.net.URISyntaxException;
        // import javax.security.auth.Subject;
        
        Channel          channel    = ensureChannel();
        Connection       connection = channel.getConnection();
        com.tangosol.net.messaging.Protocol.MessageFactory          factory    = channel.getMessageFactory();
        RemoteNamedCache cache      = new RemoteNamedCache();
        Subject          subject    = SecurityHelper.getCurrentSubject();
        com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.EnsureCacheRequest          request    = (com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.EnsureCacheRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.EnsureCacheRequest.TYPE_ID);
        
        request.setCacheName(sName);
        
        URI uri;
        try
            {
            uri = new URI((String) channel.request(request));
            }
        catch (URISyntaxException e)
            {
            throw ensureRuntimeException(e, "error instantiating URI");
            }
        
        if (loader == NullImplementation.getClassLoader())
            {
            loader = getContextClassLoader();
            _assert(loader != null, "ContextClassLoader is missing");
            cache.setPassThrough(true);
            }
        
        cache.setCacheName(sName);
        cache.setCacheService(this);
        cache.setDeferKeyAssociationCheck(isDeferKeyAssociationCheck());
        cache.setEventDispatcher(ensureEventDispatcher());
        
        connection.acceptChannel(uri, loader, cache, subject);
        
        return cache;
        }
    
    // From interface: com.tangosol.net.CacheService
    public void destroyCache(com.tangosol.net.NamedCache map)
        {
        // import Component.Net.Extend.RemoteNamedCache;
        
        if (!(map instanceof RemoteNamedCache))
            {
            throw new IllegalArgumentException("illegal map: " + map);
            }
        
        RemoteNamedCache cache = (RemoteNamedCache) map;
        String           sName = cache.getCacheName();
        
        getScopedCacheStore().releaseCache(cache);
        destroyRemoteNamedCache(cache);
        }
    
    /**
     * Destroy the given RemoteNamedCache.
    * 
    * @param cache  the RemoteNamedCache to destroy
     */
    protected void destroyRemoteNamedCache(com.tangosol.coherence.component.net.extend.RemoteNamedCache cache)
        {
        // import Component.Net.Extend.MessageFactory.CacheServiceFactory$DestroyCacheRequest as com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.DestroyCacheRequest;
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        releaseRemoteNamedCache(cache);
        
        Channel channel = ensureChannel();
        com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
        com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.DestroyCacheRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.DestroyCacheRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.CacheServiceFactory.DestroyCacheRequest.TYPE_ID);
        
        request.setCacheName(cache.getCacheName());
        channel.request(request);
        }
    
    // Declared at the super level
    /**
     * The configure() implementation method. This method must only be called by
    * a thread that has synchronized on this RemoteService.
    * 
    * @param xml  the XmlElement containing the new configuration for this
    * RemoteService
     */
    protected void doConfigure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.extend.remote.DefaultRemoteCacheServiceDependencies;
        // import com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteCacheServiceHelper as com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteCacheServiceHelper;
        
        setDependencies(com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteCacheServiceHelper.fromXml(xml,
            new DefaultRemoteCacheServiceDependencies(), getOperationalContext(),
            getContextClassLoader()));
        }
    
    // Declared at the super level
    /**
     * The shutdown() implementation method. This method must only be called by
    * a thread that has synchronized on this RemoteService.
     */
    protected void doShutdown()
        {
        super.doShutdown();
        
        getScopedCacheStore().clear();
        }
    
    // From interface: com.tangosol.net.CacheService
    public com.tangosol.net.NamedCache ensureCache(String sName, ClassLoader loader)
        {
        // import Component.Net.Extend.RemoteNamedCache;
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.internal.ScopedCacheReferenceStore as com.tangosol.net.internal.ScopedCacheReferenceStore;
        
        if (sName == null || sName.length() == 0)
            {
            sName = "Default";
            }
        
        if (loader == null)
            {
            loader = getContextClassLoader();
            _assert(loader != null, "ContextClassLoader is missing");
            }
        
        com.tangosol.net.internal.ScopedCacheReferenceStore       store = getScopedCacheStore();
        RemoteNamedCache cache = (RemoteNamedCache) store.getCache(sName, loader);
        
        if (cache == null || !cache.isActive())
            {
            // this is one of the few places that acquiring a distinct lock per cache
            // is beneficial due to the potential cost of createRemoteNamedCache
            long cWait = getDependencies().getRequestTimeoutMillis();
            if (cWait <= 0)
                {
                cWait = -1;
                }
            if (!store.lock(sName, cWait))
                {
                throw new RequestTimeoutException("Failed to get a reference to cache '" +
                    sName + "' after " + cWait + "ms");
                }
            try
                {
                cache = (RemoteNamedCache) store.getCache(sName, loader);
                if (cache == null || !cache.isActive())
                    {
                    cache = createRemoteNamedCache(sName, loader);
                    store.putCache(cache, loader);    
                    }
                }
            finally
                {
                store.unlock(sName);
                }
            }
        return cache;
        }
    
    // From interface: com.tangosol.net.CacheService
    public com.tangosol.net.BackingMapManager getBackingMapManager()
        {
        return null;
        }
    
    // From interface: com.tangosol.net.CacheService
    public java.util.Enumeration getCacheNames()
        {
        // import com.tangosol.util.IteratorEnumerator;
        // import java.util.Arrays;
        
        return new IteratorEnumerator(Arrays.asList(getScopedCacheStore().getNames().toArray()).iterator());
        }
    
    // Accessor for the property "ScopedCacheStore"
    /**
     * Getter for property ScopedCacheStore.<p>
    * Store of cache references, optionally scoped by Subject.
     */
    public com.tangosol.net.internal.ScopedCacheReferenceStore getScopedCacheStore()
        {
        return __m_ScopedCacheStore;
        }
    
    // Declared at the super level
    public String getServiceType()
        {
        // import com.tangosol.net.CacheService;
        
        return CacheService.TYPE_REMOTE;
        }
    
    // Accessor for the property "DeferKeyAssociationCheck"
    /**
     * Getter for property DeferKeyAssociationCheck.<p>
    * Whether a key should be checked for KeyAssociation by the extend client
    * (false) or deferred until the key is received by the PartionedService
    * (true).
     */
    public boolean isDeferKeyAssociationCheck()
        {
        return __m_DeferKeyAssociationCheck;
        }
    
    // Declared at the super level
    /**
     * This event occurs when dependencies are injected into the component. 
    * First, call super.onDependencies to allow all super components to process
    * the Dependencies.  Each component is free to chose how it consumes
    * dependencies.  Typically, the  dependencies are copied into the
    * component's properties.  This technique isolates Dependency Injection
    * from the rest of the component code since components continue to access
    * properties just as they did before. 
    * 
    * However, for read-only dependency properties, the component can access
    * the dependencies directly as shown in the example below for
    * RemoteCacheService dependencies.  The advantage to this technique is that
    * the property only exists in the dependencies object, it is not duplicated
    * in the component properties.
    * 
    * RemoteCacheServiceDependencies deps = (RemoteCacheServiceDependencies)
    * getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies deps)
        {
        // import Component.Net.Extend.Protocol.CacheServiceProtocol;
        // import Component.Net.Extend.Protocol.NamedCacheProtocol;
        // import com.tangosol.net.messaging.ConnectionInitiator;
        // import com.tangosol.internal.net.service.extend.remote.RemoteCacheServiceDependencies;
        
        super.onDependencies(deps);
        
        RemoteCacheServiceDependencies remoteDeps = (RemoteCacheServiceDependencies) deps;
        setDeferKeyAssociationCheck(remoteDeps.isDeferKeyAssociationCheck());
        
        // register all Protocols
        ConnectionInitiator initiator = getInitiator();
        initiator.registerProtocol(CacheServiceProtocol.getInstance());
        initiator.registerProtocol(NamedCacheProtocol.getInstance());
        }
    
    // Declared at the super level
    /**
     * Open a Channel to the remote ProxyService.
     */
    protected com.tangosol.net.messaging.Channel openChannel()
        {
        // import Component.Net.Extend.Protocol.CacheServiceProtocol;
        // import com.tangosol.net.messaging.Connection;
        // import com.tangosol.net.security.SecurityHelper;
        
        lookupProxyServiceAddress();
        
        Connection connection = getInitiator().ensureConnection();
        return connection.openChannel(CacheServiceProtocol.getInstance(),
                "CacheServiceProxy",
                null,
                null,
                SecurityHelper.getCurrentSubject());
        }
    
    // From interface: com.tangosol.net.CacheService
    public void releaseCache(com.tangosol.net.NamedCache map)
        {
        // import Component.Net.Extend.RemoteNamedCache;
        
        if (!(map instanceof RemoteNamedCache))
            {
            throw new IllegalArgumentException("illegal map: " + map);
            }
        
        RemoteNamedCache cache = (RemoteNamedCache) map;
        String           sName = cache.getCacheName();
        
        getScopedCacheStore().releaseCache(cache);
        releaseRemoteNamedCache(cache);
        }
    
    /**
     * Releases all the caches fetched from the store and then clears the store.
     */
    protected void releaseCaches()
        {
        // import Component.Net.Extend.RemoteNamedCache;
        // import com.tangosol.net.internal.ScopedCacheReferenceStore as com.tangosol.net.internal.ScopedCacheReferenceStore;
        // import java.util.Iterator;
        
        com.tangosol.net.internal.ScopedCacheReferenceStore store = getScopedCacheStore();
        for (Iterator iter = store.getAllCaches().iterator(); iter.hasNext(); )
            {
            RemoteNamedCache cache = (RemoteNamedCache) iter.next();
            releaseRemoteNamedCache(cache);
            }
        
        store.clear();
        }
    
    /**
     * Release the given RemoteNamedCache.
    * 
    * @param cache  the RemoteNamedCache to release
     */
    protected void releaseRemoteNamedCache(com.tangosol.coherence.component.net.extend.RemoteNamedCache cache)
        {
        // import com.tangosol.net.messaging.Channel;
        
        try
            {
            // when this is called due to certain connection error, e.g. ping
            // timeout, the channel could be null and closed.
            Channel channel = cache.getChannel();
            if (channel != null)
                {
                channel.close();
                }
            }
        catch (RuntimeException e) {}
        }
    
    // From interface: com.tangosol.net.CacheService
    public void setBackingMapManager(com.tangosol.net.BackingMapManager manager)
        {
        }
    
    // Accessor for the property "DeferKeyAssociationCheck"
    /**
     * Setter for property DeferKeyAssociationCheck.<p>
    * Whether a key should be checked for KeyAssociation by the extend client
    * (false) or deferred until the key is received by the PartionedService
    * (true).
     */
    protected void setDeferKeyAssociationCheck(boolean fDefer)
        {
        __m_DeferKeyAssociationCheck = fDefer;
        }
    
    // Accessor for the property "ScopedCacheStore"
    /**
     * Setter for property ScopedCacheStore.<p>
    * Store of cache references, optionally scoped by Subject.
     */
    protected void setScopedCacheStore(com.tangosol.net.internal.ScopedCacheReferenceStore store)
        {
        __m_ScopedCacheStore = store;
        }
    }
