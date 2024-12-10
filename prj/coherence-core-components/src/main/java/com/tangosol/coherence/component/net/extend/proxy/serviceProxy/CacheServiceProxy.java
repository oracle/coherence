
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy

package com.tangosol.coherence.component.net.extend.proxy.serviceProxy;

import com.tangosol.coherence.component.net.extend.message.request.CacheServiceRequest;
import com.tangosol.coherence.component.net.extend.protocol.CacheServiceProtocol;
import com.tangosol.coherence.component.util.SafeNamedCache;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.scheme.ViewScheme;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.internal.net.service.extend.proxy.CacheServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.DefaultCacheServiceProxyDependencies;
import com.tangosol.internal.util.collection.ConvertingNamedCache;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.NearCache;
import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.net.cache.WrapperNamedCache;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.IteratorEnumerator;
import com.tangosol.util.NullImplementation;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Reciever implementation for the CacheService Protocol.
 * 
 * The CacheServiceProxy is the cluster-side handler (Proxy) for a
 * RemoteCacheService. It enabled non-clustered clients to obtain and destroy
 * remote references to NamedCache instances running within the cluster.
 * 
 * @see Component.Net.Extend.RemoteService.RemoteCacheService
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class CacheServiceProxy
        extends    com.tangosol.coherence.component.net.extend.proxy.ServiceProxy
        implements com.tangosol.net.CacheService,
                   com.tangosol.util.SynchronousListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property BackingMapManager
     *
     * See com.tangosol.net.CacheService#getBackingMapManager
     */
    private com.tangosol.net.BackingMapManager __m_BackingMapManager;
    
    /**
     * Property CacheFactory
     *
     * The cache factory that created this service.
     */
    private com.tangosol.net.ConfigurableCacheFactory __m_CacheFactory;
    
    /**
     * Property CacheService
     *
     * The CacheService passed to a CacheServiceRequest. If a custom proxy has
     * not been configured, this property referes to this.
     */
    private com.tangosol.net.CacheService __m_CacheService;
    
    /**
     * Property ConverterFromBinary
     *
     * Child ConverterFromBinary instance.
     */
    private CacheServiceProxy.ConverterFromBinary __m_ConverterFromBinary;
    
    /**
     * Property ConverterToBinary
     *
     * Child ConverterToBinary instance.
     */
    private CacheServiceProxy.ConverterToBinary __m_ConverterToBinary;
    
    /**
     * Property LockEnabled
     *
     * If false, NamedCache lock or unlock operation will be prohibited.
     */
    private boolean __m_LockEnabled;
    
    /**
     * Property NamedCacheSet
     *
     * A Map of NamedCache names created by this CacheServiceProxy. This is
     * used by the ensureCache() method to determine if a warning should be
     * logged if cache does not support pass-through optimizations.
     */
    private java.util.Set __m_NamedCacheSet;
    
    /**
     * Property PassThroughEnabled
     *
     * True iff binary pass-through optimizations are enabled.
     */
    private boolean __m_PassThroughEnabled;
    
    /**
     * Property ReadOnly
     *
     * If true, any NamedCache operation that may potentially modify cached
     * entries will be prohibited.
     */
    private boolean __m_ReadOnly;
    
    /**
     * Property TransferThreshold
     *
     * The approximate maximum number of bytes transfered by a partial
     * response. Results that can be streamed, such as query requests, are
     * returned to the requestor as a sequence of response messages containing
     * a portion of the total result. Each of these response messages will be
     * approximately no larger than the configured size.
     * 
     * Default value is .5 MB.
     */
    private long __m_TransferThreshold;
    
    // Default constructor
    public CacheServiceProxy()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public CacheServiceProxy(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setEnabled(true);
            setNamedCacheSet(new com.tangosol.util.SafeHashSet());
            setPassThroughEnabled(true);
            setServiceVersion("14");
            setTransferThreshold(524288L);
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new CacheServiceProxy.ConverterFromBinary("ConverterFromBinary", this, true), "ConverterFromBinary");
        _addChild(new CacheServiceProxy.ConverterToBinary("ConverterToBinary", this, true), "ConverterToBinary");
        
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
        return new com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/serviceProxy/CacheServiceProxy".replace('/', '.'));
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
    * @return DefaultProxyDependencies  the cloned dependencies
     */
    protected com.tangosol.internal.net.service.extend.proxy.DefaultProxyDependencies cloneDependencies(com.tangosol.internal.net.service.extend.proxy.ProxyDependencies deps)
        {
        // import com.tangosol.internal.net.service.extend.proxy.DefaultCacheServiceProxyDependencies;
        // import com.tangosol.internal.net.service.extend.proxy.CacheServiceProxyDependencies;
        
        return new DefaultCacheServiceProxyDependencies((CacheServiceProxyDependencies) deps);
        }
    
    // From interface: com.tangosol.net.CacheService
    public void destroyCache(com.tangosol.net.NamedCache cache)
        {
        if (isReadOnly())
            {
            throw new SecurityException("NamedCache \"" + cache.getCacheName() + "\" is read-only");
            }
        
        releaseCache(cache, /*fDestroy*/ true);
        }
    
    // From interface: com.tangosol.net.CacheService
    public com.tangosol.net.NamedCache ensureCache(String sName, ClassLoader loader)
        {
        // import com.tangosol.net.cache.TypeAssertion;
        
        return ensureTypedCache(sName, loader, TypeAssertion.WITHOUT_TYPE_CHECKING);
        }
    
    protected com.tangosol.net.NamedCache ensureTypedCache(String sName, ClassLoader loader, com.tangosol.net.cache.TypeAssertion assertion)
        {
        // import com.tangosol.coherence.config.scheme.ViewScheme;
        // import com.tangosol.internal.util.collection.ConvertingNamedCache;
        // import com.tangosol.io.Serializer;
        // import com.tangosol.net.CacheFactory;
        // import com.tangosol.net.ConfigurableCacheFactory;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.cache.NearCache;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.DistributedCacheService;
        // import com.tangosol.net.ExtensibleConfigurableCacheFactory as com.tangosol.net.ExtensibleConfigurableCacheFactory;
        // import com.tangosol.util.ExternalizableHelper;
        // import com.tangosol.util.NullImplementation;
        
        ConfigurableCacheFactory ccf = getCacheFactory();
        
        if (!isPassThroughEnabled())
            {
            return ccf.ensureTypedCache(sName, loader, assertion);
            }
        
        ClassLoader loaderInternal = NullImplementation.getClassLoader();
        if (ccf instanceof com.tangosol.net.ExtensibleConfigurableCacheFactory)
            {
            // pass through is not enabled for CQC and a real loader is used
            if (((com.tangosol.net.ExtensibleConfigurableCacheFactory) ccf).getCacheConfig().
                    findSchemeByCacheName(sName) instanceof ViewScheme)
                {
                loaderInternal = loader;
                }
            }
        
        NamedCache cache = ccf.ensureTypedCache(sName, loaderInternal, assertion);
        
        // optimize front-cache out of storage enabled proxies
        boolean fNear = cache instanceof NearCache;
        if (fNear)
            {
            CacheService service = cache.getCacheService();
            if (service != null && service instanceof DistributedCacheService
                && ((DistributedCacheService) service).isLocalStorageEnabled())
                {
                cache = ((NearCache) cache).getBackCache();
                fNear = false;
                }
            }
        
        // check to see if the Serializer associated with the "backdoor" NamedCache is
        // compatible with the Serializer associated with this CacheServiceProxy; if
        // they are not, replace the "backdoor" NamedCache with the "frontdoor"
        // NamedCache and wrap it with a ConverterNamedCache that uses this
        // CacheServiceProxy's Converters (see ConverterFromBinary and ConverterToBinary)
        Serializer serializerThis = getSerializer();
        Serializer serializerThat = getSerializer(cache);
        if (ExternalizableHelper.isSerializerCompatible(serializerThis, serializerThat))
            {
            if (fNear)
                {
                cache = new ConvertingNamedCache(cache,
                    NullImplementation.getConverter(), ExternalizableHelper.CONVERTER_STRIP_INTDECO,
                    NullImplementation.getConverter(), NullImplementation.getConverter());
                }
            }
        else
            {
            // COH-8758
            // We cannot release the cache obtained with the NullImplementation loader
            // as this will clear local caches (or caches backed by a local cache, such
            // as a wrapper or converter cache of a local cache). The downside of this
            // change is that we will (at worst) have one unused cache reference per
            // configured cache service. The upside is that it will make subsequent
            // calls of this method (with the same cache name) more efficient.
            // ccf.releaseCache(cache);
            cache = ccf.ensureTypedCache(sName, loader, assertion);
            cache = new ConvertingNamedCache(cache,
                    getConverterToBinary(), getConverterFromBinary(),
                    getConverterToBinary(), getConverterFromBinary());
        
            if (getNamedCacheSet().add(sName))
                {
                if (serializerThat == null)
                    {
                    _trace("The cache \"" + sName + "\" does not support"
                         + " pass-through optimization for objects in"
                         + " internal format. If possible, consider using"
                         + " a different cache topology.", 3);
                    }
                else
                    {
                    ExternalizableHelper.reportIncompatibleSerializers(cache,
                            getServiceName(), serializerThis);
                    }
                }
            }
        
        return cache;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "BackingMapManager"
    /**
     * Getter for property BackingMapManager.<p>
    * See com.tangosol.net.CacheService#getBackingMapManager
     */
    public com.tangosol.net.BackingMapManager getBackingMapManager()
        {
        return __m_BackingMapManager;
        }
    
    // Accessor for the property "CacheFactory"
    /**
     * Getter for property CacheFactory.<p>
    * The cache factory that created this service.
     */
    public com.tangosol.net.ConfigurableCacheFactory getCacheFactory()
        {
        return __m_CacheFactory;
        }
    
    // From interface: com.tangosol.net.CacheService
    public java.util.Enumeration getCacheNames()
        {
        // import com.tangosol.util.ImmutableArrayList;
        // import com.tangosol.util.IteratorEnumerator;
        // import java.util.Collection;
        // import java.util.Set;
        
        Set set = getNamedCacheSet();
        synchronized (set)
            {
            Collection col = new ImmutableArrayList(set.toArray());
            return new IteratorEnumerator(col.iterator());
            }
        }
    
    // Accessor for the property "CacheService"
    /**
     * Getter for property CacheService.<p>
    * The CacheService passed to a CacheServiceRequest. If a custom proxy has
    * not been configured, this property referes to this.
     */
    public com.tangosol.net.CacheService getCacheService()
        {
        // import com.tangosol.net.CacheService;
        
        CacheService service = __m_CacheService;
        return service == null ? this : service;
        }
    
    // Accessor for the property "ConverterFromBinary"
    /**
     * Getter for property ConverterFromBinary.<p>
    * Child ConverterFromBinary instance.
     */
    public CacheServiceProxy.ConverterFromBinary getConverterFromBinary()
        {
        return __m_ConverterFromBinary;
        }
    
    // Accessor for the property "ConverterToBinary"
    /**
     * Getter for property ConverterToBinary.<p>
    * Child ConverterToBinary instance.
     */
    public CacheServiceProxy.ConverterToBinary getConverterToBinary()
        {
        return __m_ConverterToBinary;
        }
    
    // Declared at the super level
    public String getName()
        {
        return "CacheServiceProxy";
        }
    
    // Accessor for the property "NamedCacheSet"
    /**
     * Getter for property NamedCacheSet.<p>
    * A Map of NamedCache names created by this CacheServiceProxy. This is used
    * by the ensureCache() method to determine if a warning should be logged if
    * cache does not support pass-through optimizations.
     */
    protected java.util.Set getNamedCacheSet()
        {
        return __m_NamedCacheSet;
        }
    
    // Declared at the super level
    public com.tangosol.net.messaging.Protocol getProtocol()
        {
        // import Component.Net.Extend.Protocol.CacheServiceProtocol;
        
        return CacheServiceProtocol.getInstance();
        }
    
    // Accessor for the property "Serializer"
    /**
     * Return the Serializer associated with the given NamedCache or null if the
    * NamedCache is an in-process cache.
     */
    public static Serializer getSerializer(NamedCache cache)
        {
        Serializer serializer = null;
        
        if (cache instanceof WrapperNamedCache)
            {
            Map map = ((WrapperNamedCache) cache).getMap();
            cache = map instanceof NamedCache ? (NamedCache) map : null;
            }
        else if (cache instanceof NearCache)
            {
            cache = ((NearCache) cache).getBackCache();
            }
        
        if (cache instanceof SafeNamedCache)
            {
            CacheService service = cache.getCacheService();
            
            String sType = service.getInfo().getServiceType();
            if (!CacheService.TYPE_LOCAL.equals(sType) &&        // filter out Local
                !CacheService.TYPE_REPLICATED.equals(sType) &&   // filter out Replicated
                !CacheService.TYPE_OPTIMISTIC.equals(sType))     // filter out Optimistic
                {
                serializer = service.getSerializer();
                }
            }
        // else; filter custom caches whoes front contents may be a transformed version of the back (i.e. CQC)
        
        return serializer;
        }
    
    // Declared at the super level
    public String getServiceType()
        {
        // import com.tangosol.net.CacheService;
        
        return CacheService.TYPE_REMOTE;
        }
    
    // Accessor for the property "TransferThreshold"
    /**
     * Getter for property TransferThreshold.<p>
    * The approximate maximum number of bytes transfered by a partial response.
    * Results that can be streamed, such as query requests, are returned to the
    * requestor as a sequence of response messages containing a portion of the
    * total result. Each of these response messages will be approximately no
    * larger than the configured size.
    * 
    * Default value is .5 MB.
     */
    public long getTransferThreshold()
        {
        return __m_TransferThreshold;
        }
    
    // Accessor for the property "LockEnabled"
    /**
     * Getter for property LockEnabled.<p>
    * If false, NamedCache lock or unlock operation will be prohibited.
     */
    public boolean isLockEnabled()
        {
        return __m_LockEnabled;
        }
    
    // Accessor for the property "PassThroughEnabled"
    /**
     * Getter for property PassThroughEnabled.<p>
    * True iff binary pass-through optimizations are enabled.
     */
    public boolean isPassThroughEnabled()
        {
        return __m_PassThroughEnabled;
        }
    
    // Accessor for the property "ReadOnly"
    /**
     * Getter for property ReadOnly.<p>
    * If true, any NamedCache operation that may potentially modify cached
    * entries will be prohibited.
     */
    public boolean isReadOnly()
        {
        return __m_ReadOnly;
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
    * CacheServiceProxy dependencies.  The advantage to this technique is that
    * the property only exists in the dependencies object, it is not duplicated
    * in the component properties.
    * 
    * CacheServiceProxyDependencies deps = (CacheServiceProxyDependencies)
    * getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.internal.net.service.extend.proxy.ProxyDependencies deps)
        {
        // import com.tangosol.coherence.config.ResolvableParameterList;
        // import com.tangosol.coherence.config.builder.InstanceBuilder;
        // import com.tangosol.coherence.config.builder.ParameterizedBuilder;
        // import com.tangosol.config.expression.NullParameterResolver;
        // import com.tangosol.config.expression.Parameter;
        // import com.tangosol.internal.net.service.extend.proxy.CacheServiceProxyDependencies;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.run.xml.XmlHelper;
        // import com.tangosol.util.Base;
        // import java.util.Iterator;
        
        super.onDependencies(deps);
        
        CacheServiceProxyDependencies proxyDeps = (CacheServiceProxyDependencies) deps;
        
        // For ECCF based config, a custom service builder may be injected by CODI.
        // For DCCF, we are still using the XML for custom services.  
        ParameterizedBuilder bldrService  = proxyDeps.getServiceBuilder();
        if (bldrService == null)
            {
            // DCCF style
            XmlElement xml = proxyDeps.getServiceClassConfig();
            if (xml != null)
                {
                try
                    {
                    setCacheService((CacheService) XmlHelper.createInstance(xml,
                        Base.getContextClassLoader(), /*resolver*/ this, CacheService.class));
                    }
                catch (Exception e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            }
        else
            {
            // ECCF style - only an InstanceBuilder is supported
            ResolvableParameterList listParams = new ResolvableParameterList();
            listParams.add(new Parameter("cache-service", this));
        
            if (bldrService instanceof InstanceBuilder)
                {
                // Add any remaining params, skip the first param which is the service
                Iterator iterParams = ((InstanceBuilder) bldrService).getConstructorParameterList().iterator();
                if (iterParams.hasNext())
                    {
                    iterParams.next();
                    }     
                while (iterParams.hasNext())
                    {
                    listParams.add((Parameter) iterParams.next());
                    }
                }
            setCacheService((CacheService) bldrService.realize(new NullParameterResolver(),
                    Base.getContextClassLoader(), listParams));    
            }
        
        setLockEnabled(proxyDeps.isLockEnabled());
        setReadOnly(proxyDeps.isReadOnly());
        setTransferThreshold(proxyDeps.getTransferThreshold());
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
        setConverterFromBinary(
                (CacheServiceProxy.ConverterFromBinary) _findChild("ConverterFromBinary"));
        setConverterToBinary(
                (CacheServiceProxy.ConverterToBinary) _findChild("ConverterToBinary"));
        
        super.onInit();
        }
    
    // Declared at the super level
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        // import Component.Net.Extend.Message.Request.CacheServiceRequest;
        
        if (message instanceof CacheServiceRequest)
            {
            CacheServiceRequest request = (CacheServiceRequest) message;
            request.setCacheService(getCacheService());
            request.setLockEnabled(isLockEnabled());
            request.setReadOnly(isReadOnly());
            request.setTransferThreshold(getTransferThreshold());
            }
        
        message.run();
        }
    
    // From interface: com.tangosol.net.CacheService
    public void releaseCache(com.tangosol.net.NamedCache cache)
        {
        releaseCache(cache, /*fDestroy*/ false);
        }
    
    public void releaseCache(com.tangosol.net.NamedCache cache, boolean fDestroy)
        {
        // import com.tangosol.net.CacheFactory;
        // import com.tangosol.net.ConfigurableCacheFactory;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.cache.NearCache;
        // import com.tangosol.net.cache.TypeAssertion;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ConverterCollections$ConverterNamedCache as com.tangosol.util.ConverterCollections.ConverterNamedCache;
        // import com.tangosol.util.NullImplementation;
        
        // see #ensureCache
        if (cache instanceof com.tangosol.util.ConverterCollections.ConverterNamedCache)
            {
            cache = ((com.tangosol.util.ConverterCollections.ConverterNamedCache) cache).getNamedCache();
            }
        
        ConfigurableCacheFactory ccf = getCacheFactory();
        try
            {
            if (fDestroy)
                {
                ccf.destroyCache(cache);
                }
            else
                {
                ccf.releaseCache(cache);
                }
            }
        catch (IllegalArgumentException e)
            {
            // this may be the back of a NearCache; see ensureCache
            if (isPassThroughEnabled())
                {
                NamedCache cacheFront = ccf.ensureTypedCache(cache.getCacheName(), NullImplementation.getClassLoader(),
                                                             TypeAssertion.WITHOUT_TYPE_CHECKING);
                if (cacheFront instanceof NearCache && ((NearCache) cacheFront).getBackCache() == cache)
                    {
                    if (fDestroy)
                        {
                        ccf.destroyCache(cacheFront);
                        }
                    else
                        {
                        ccf.releaseCache(cacheFront);
                        }
                    return;
                    }
                }
        
            throw e;
            }
        }
    
    // Declared at the super level
    public Object resolveParameter(String sType, String sValue)
        {
        // import com.tangosol.net.CacheService;
        
        if (CacheService.class.getName().equals(sType) && "{service}".equals(sValue))
            {
            return this;
            }
        
        return super.resolveParameter(sType, sValue);
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "BackingMapManager"
    /**
     * Setter for property BackingMapManager.<p>
    * See com.tangosol.net.CacheService#getBackingMapManager
     */
    public void setBackingMapManager(com.tangosol.net.BackingMapManager manager)
        {
        __m_BackingMapManager = manager;
        }
    
    // Accessor for the property "CacheFactory"
    /**
     * Setter for property CacheFactory.<p>
    * The cache factory that created this service.
     */
    public void setCacheFactory(com.tangosol.net.ConfigurableCacheFactory factory)
        {
        __m_CacheFactory = factory;
        }
    
    // Accessor for the property "CacheService"
    /**
     * Setter for property CacheService.<p>
    * The CacheService passed to a CacheServiceRequest. If a custom proxy has
    * not been configured, this property referes to this.
     */
    protected void setCacheService(com.tangosol.net.CacheService service)
        {
        __m_CacheService = service;
        }
    
    // Declared at the super level
    /**
     * Setter for property Config.<p>
    * The XML configuration for this Adapter.
     */
    public void setConfig(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.extend.proxy.DefaultCacheServiceProxyDependencies;
        // import com.tangosol.internal.net.service.extend.proxy.LegacyXmlCacheServiceProxyHelper as com.tangosol.internal.net.service.extend.proxy.LegacyXmlCacheServiceProxyHelper;
        
        setDependencies(com.tangosol.internal.net.service.extend.proxy.LegacyXmlCacheServiceProxyHelper.fromXml(xml, new DefaultCacheServiceProxyDependencies()));
        }
    
    // Accessor for the property "ConverterFromBinary"
    /**
     * Setter for property ConverterFromBinary.<p>
    * Child ConverterFromBinary instance.
     */
    protected void setConverterFromBinary(CacheServiceProxy.ConverterFromBinary conv)
        {
        __m_ConverterFromBinary = conv;
        }
    
    // Accessor for the property "ConverterToBinary"
    /**
     * Setter for property ConverterToBinary.<p>
    * Child ConverterToBinary instance.
     */
    protected void setConverterToBinary(CacheServiceProxy.ConverterToBinary conv)
        {
        __m_ConverterToBinary = conv;
        }
    
    // Accessor for the property "LockEnabled"
    /**
     * Setter for property LockEnabled.<p>
    * If false, NamedCache lock or unlock operation will be prohibited.
     */
    protected void setLockEnabled(boolean fEnabled)
        {
        __m_LockEnabled = fEnabled;
        }
    
    // Accessor for the property "NamedCacheSet"
    /**
     * Setter for property NamedCacheSet.<p>
    * A Map of NamedCache names created by this CacheServiceProxy. This is used
    * by the ensureCache() method to determine if a warning should be logged if
    * cache does not support pass-through optimizations.
     */
    protected void setNamedCacheSet(java.util.Set set)
        {
        __m_NamedCacheSet = set;
        }
    
    // Accessor for the property "PassThroughEnabled"
    /**
     * Setter for property PassThroughEnabled.<p>
    * True iff binary pass-through optimizations are enabled.
     */
    public void setPassThroughEnabled(boolean fEnabled)
        {
        __m_PassThroughEnabled = fEnabled;
        }
    
    // Accessor for the property "ReadOnly"
    /**
     * Setter for property ReadOnly.<p>
    * If true, any NamedCache operation that may potentially modify cached
    * entries will be prohibited.
     */
    protected void setReadOnly(boolean fReadOnly)
        {
        __m_ReadOnly = fReadOnly;
        }
    
    // Declared at the super level
    /**
     * Setter for property Serializer.<p>
    * @see com.tangosol.net.Service#getSerializer
     */
    public void setSerializer(com.tangosol.io.Serializer serializer)
        {
        super.setSerializer(serializer);
        
        getConverterFromBinary().setSerializer(serializer);
        getConverterToBinary().setSerializer(serializer);
        }
    
    // Accessor for the property "TransferThreshold"
    /**
     * Setter for property TransferThreshold.<p>
    * The approximate maximum number of bytes transfered by a partial response.
    * Results that can be streamed, such as query requests, are returned to the
    * requestor as a sequence of response messages containing a portion of the
    * total result. Each of these response messages will be approximately no
    * larger than the configured size.
    * 
    * Default value is .5 MB.
     */
    protected void setTransferThreshold(long cb)
        {
        __m_TransferThreshold = cb;
        }

    // ---- class: com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy$ConverterFromBinary
    
    /**
     * Converter implementation that converts Objects from a Binary
     * representation via the CacheServiceProxy's Serializer.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConverterFromBinary
            extends    com.tangosol.coherence.component.util.Converter
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConverterFromBinary()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConverterFromBinary(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy.ConverterFromBinary();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/serviceProxy/CacheServiceProxy$ConverterFromBinary".replace('/', '.'));
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
        public Object convert(Object o)
            {
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ExternalizableHelper;
            
            return o == null ? null :
                    ExternalizableHelper.fromBinary((Binary) o, getSerializer());
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy$ConverterToBinary
    
    /**
     * Converter implementation that converts Objects to a Binary
     * representation via the CacheServiceProxy's Serializer.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConverterToBinary
            extends    com.tangosol.coherence.component.util.Converter
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConverterToBinary()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConverterToBinary(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy.ConverterToBinary();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/serviceProxy/CacheServiceProxy$ConverterToBinary".replace('/', '.'));
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
        public Object convert(Object o)
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            return ExternalizableHelper.toBinary(o, getSerializer());
            }
        }
    }
