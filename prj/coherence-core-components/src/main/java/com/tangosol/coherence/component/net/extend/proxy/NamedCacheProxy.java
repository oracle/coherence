
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy

package com.tangosol.coherence.component.net.extend.proxy;

import com.tangosol.coherence.component.net.extend.message.request.NamedCacheRequest;
import com.tangosol.coherence.component.net.extend.protocol.NamedCacheProtocol;

import com.tangosol.coherence.component.util.DaemonPool;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.SuspectConnectionException;

import com.tangosol.util.SegmentedConcurrentMap;

import java.util.Iterator;
import java.util.Map;

/**
 * Reciever implementation for the NamedCache Protocol.
 * 
 * The NamedCacheProxy is the cluster-side handler (Proxy) for a
 * RemoteNamedCache. It enabled non-clustered clients to use a NamedCache
 * running within the cluster.
 * 
 * @see Component.Net.Extend.RemoteNamedCache
 */
/*
* Integrates
*     com.tangosol.net.NamedCache
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class NamedCacheProxy
        extends    com.tangosol.coherence.component.net.extend.Proxy
        implements com.tangosol.internal.net.NamedCacheDeactivationListener,
                   com.tangosol.net.MemberListener,
                   com.tangosol.net.NamedCache,
                   com.tangosol.net.messaging.Channel.Receiver,
                   Runnable
    {
    // ---- Fields declarations ----
    
    /**
     * Property ATTR_LISTENER
     *
     * The name of the Channel attribute that contains the MapListenerProxy
     * registered on behalf of the RemoteNamedCache.
     */
    public static final String ATTR_LISTENER = "named-cache-listener";
    
    /**
     * Property ATTR_LOCK_MAP
     *
     * The name of the Channel attribute that contains the Map of keys locked
     * on behalf of the RemoteNamedCache.
     */
    public static final String ATTR_LOCK_MAP = "named-cache-lock-map";
    
    /**
     * Property Channel
     *
     * The Channel registered with this proxy.
     */
    private com.tangosol.net.messaging.Channel __m_Channel;
    
    /**
     * Property LockDeprecateWarned
     *
     * A boolean flag indicating whether we have warned user about the
     * deprecated lock methods.
     */
    private static boolean __s_LockDeprecateWarned;
    
    /**
     * Property LockEnabled
     *
     * If false, NamedCache lock or unlock operation will be prohibited.
     */
    private boolean __m_LockEnabled;
    
    /**
     * Property NamedCache
     *
     * The NamedCache proxied by this NamedCacheProxy.
     */
    private com.tangosol.net.NamedCache __m_NamedCache;
    
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
    private static com.tangosol.util.ListMap __mapChildren;

    private int m_cacheId;

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
        __mapChildren.put("CleanupTask", NamedCacheProxy.CleanupTask.get_CLASS());
        __mapChildren.put("EntrySet", NamedCacheProxy.EntrySet.get_CLASS());
        __mapChildren.put("KeySet", NamedCacheProxy.KeySet.get_CLASS());
        __mapChildren.put("Values", NamedCacheProxy.Values.get_CLASS());
        }
    
    // Default constructor
    public NamedCacheProxy()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public NamedCacheProxy(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setTransferThreshold(524288L);
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
        return new com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/NamedCacheProxy".replace('/', '.'));
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

    /**
     * Obtain the cache identifier.
     *
     * @return  the cache identifier
     */
    public int getCacheId()
        {
        return m_cacheId;
        }

    /**
     * Set the cache identifier.
     *
     * @param cacheId  the cache identifier
     */
    public void setCacheId(int cacheId)
        {
        m_cacheId = cacheId;
        }

    //++ com.tangosol.net.NamedCache integration
    // Access optimization
    // properties integration
    // methods integration

    @Override
    public AsyncNamedCache async()
        {
        return getNamedCache().async();
        }

    @Override
    public AsyncNamedCache async(AsyncNamedCache.Option... options)
        {
        return getNamedCache().async(options);
        }

    private void addIndex$Router(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        getNamedCache().addIndex(extractor, fOrdered, comparator);
        }
    public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        assertWriteable();
        addIndex$Router(extractor, fOrdered, comparator);
        }
    public void addMapListener(com.tangosol.util.MapListener listener)
        {
        getNamedCache().addMapListener(listener);
        }
    public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
        {
        getNamedCache().addMapListener(listener, filter, fLite);
        }
    public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
        {
        getNamedCache().addMapListener(listener, oKey, fLite);
        }
    public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        return getNamedCache().aggregate(filter, agent);
        }
    public Object aggregate(java.util.Collection colKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        return getNamedCache().aggregate(colKeys, agent);
        }
    private void clear$Router()
        {
        getNamedCache().clear();
        }
    public void clear()
        {
        assertWriteable();
        clear$Router();
        }
    public boolean containsKey(Object oKey)
        {
        return getNamedCache().containsKey(oKey);
        }
    public boolean containsValue(Object oValue)
        {
        return getNamedCache().containsValue(oValue);
        }
    private java.util.Set entrySet$Router()
        {
        return getNamedCache().entrySet();
        }
    public java.util.Set entrySet()
        {
        NamedCacheProxy.EntrySet set = (NamedCacheProxy.EntrySet) _newChild("EntrySet");
        set.setSet(entrySet$Router());
        return set;
        }
    public java.util.Set entrySet(com.tangosol.util.Filter filter)
        {
        return getNamedCache().entrySet(filter);
        }
    public java.util.Set entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        return getNamedCache().entrySet(filter, comparator);
        }
    public Object get(Object oKey)
        {
        return getNamedCache().get(oKey);
        }
    public java.util.Map getAll(java.util.Collection colKeys)
        {
        return getNamedCache().getAll(colKeys);
        }
    public String getCacheName()
        {
        return getNamedCache().getCacheName();
        }
    public com.tangosol.net.CacheService getCacheService()
        {
        return getNamedCache().getCacheService();
        }
    private Object invoke$Router(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getNamedCache().invoke(oKey, agent);
        }
    public Object invoke(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        assertWriteable();
        return invoke$Router(oKey, agent);
        }
    private java.util.Map invokeAll$Router(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getNamedCache().invokeAll(filter, agent);
        }
    public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        assertWriteable();
        return invokeAll$Router(filter, agent);
        }
    private java.util.Map invokeAll$Router(java.util.Collection colKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getNamedCache().invokeAll(colKeys, agent);
        }
    public java.util.Map invokeAll(java.util.Collection colKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        assertWriteable();
        return invokeAll$Router(colKeys, agent);
        }
    public boolean isActive()
        {
        return getNamedCache().isActive();
        }
    @Override
    public boolean isReady()
        {
        return getNamedCache().isReady();
        }
    public boolean isEmpty()
        {
        return getNamedCache().isEmpty();
        }
    private java.util.Set keySet$Router()
        {
        return getNamedCache().keySet();
        }
    public java.util.Set keySet()
        {
        NamedCacheProxy.KeySet set = (NamedCacheProxy.KeySet) _newChild("KeySet");
        set.setSet(keySet$Router());
        return set;
        }
    public java.util.Set keySet(com.tangosol.util.Filter filter)
        {
        return getNamedCache().keySet(filter);
        }
    private boolean lock$Router(Object oKey)
        {
        return getNamedCache().lock(oKey);
        }
    public boolean lock(Object oKey)
        {
        assertLockEnabled();
        return lock$Router(oKey);
        }
    private boolean lock$Router(Object oKey, long cWait)
        {
        return getNamedCache().lock(oKey, cWait);
        }
    public boolean lock(Object oKey, long cWait)
        {
        printLockDeprecatedMessage();
        
        assertLockEnabled();
        return lock$Router(oKey, cWait);
        }
    private Object put$Router(Object oKey, Object oValue)
        {
        return getNamedCache().put(oKey, oValue);
        }
    public Object put(Object oKey, Object oValue)
        {
        assertWriteable();
        return put$Router(oKey, oValue);
        }
    private Object put$Router(Object oKey, Object oValue, long cMillis)
        {
        return getNamedCache().put(oKey, oValue, cMillis);
        }
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        assertWriteable();
        return put$Router(oKey, oValue, cMillis);
        }
    private void putAll$Router(java.util.Map map)
        {
        getNamedCache().putAll(map);
        }
    public void putAll(java.util.Map map)
        {
        assertWriteable();
        putAll$Router(map);
        }
    private Object remove$Router(Object oKey)
        {
        return getNamedCache().remove(oKey);
        }
    public Object remove(Object oKey)
        {
        assertWriteable();
        return remove$Router(oKey);
        }
    private void removeIndex$Router(com.tangosol.util.ValueExtractor extractor)
        {
        getNamedCache().removeIndex(extractor);
        }
    public void removeIndex(com.tangosol.util.ValueExtractor extractor)
        {
        assertWriteable();
        removeIndex$Router(extractor);
        }
    public void removeMapListener(com.tangosol.util.MapListener listener)
        {
        getNamedCache().removeMapListener(listener);
        }
    public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
        {
        getNamedCache().removeMapListener(listener, filter);
        }
    public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
        {
        getNamedCache().removeMapListener(listener, oKey);
        }
    public int size()
        {
        return getNamedCache().size();
        }
    private void truncate$Router()
        {
        getNamedCache().truncate();
        }
    public void truncate()
        {
        assertWriteable();
        truncate$Router();
        }
    private boolean unlock$Router(Object oKey)
        {
        return getNamedCache().unlock(oKey);
        }
    public boolean unlock(Object oKey)
        {
        printLockDeprecatedMessage();
        
        assertLockEnabled();
        return unlock$Router(oKey);
        }
    private java.util.Collection values$Router()
        {
        return getNamedCache().values();
        }
    public java.util.Collection values()
        {
        NamedCacheProxy.Values col = (NamedCacheProxy.Values) _newChild("Values");
        col.setCollection(values$Router());
        return col;
        }
    //-- com.tangosol.net.NamedCache integration
    
    /**
     * Assert that NamedCache lock and unlock operations are allowed.
    * 
    * @throws SecurityException if lock and unlock operations are prohibited
     */
    public void assertLockEnabled()
        {
        if (isLockEnabled())
            {
            return;
            }
        
        throw new SecurityException("lock operations are prohibited on NamedCache \""
                + getCacheName() + '"');
        }
    
    /**
     * Assert that NamedCache operations that potentially modify cached entries
    * are allowed.
    * 
    * @throws SecurityException if mutating operations are prohibited
     */
    public void assertWriteable()
        {
        if (isReadOnly())
            {
            throw new SecurityException("NamedCache \"" + getCacheName()
                    + "\" is read-only");
            }
        }
    
    /**
     * Close the channel.
     */
    protected void closeChannel()
        {
        // import com.tangosol.net.messaging.Channel;
        
        Channel channel = getChannel();
        if (channel != null)
            {
            channel.close();
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void destroy()
        {
        // must destroy the NamedCache via the CacheServiceProxy
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.internal.net.NamedCacheDeactivationListener
    public void entryDeleted(com.tangosol.util.MapEvent evt)
        {
        if (evt.getKey() == null)
            {
            // COH-25261 delegate processing to the memberLeft event
            if (getProtocol().getCurrentVersion() >= 10)
                {
                if (getCacheService().isRunning())
                    {
                    closeChannel();
                    }
                }
            else
                {
                closeChannel();
                }
            }
        }
    
    // From interface: com.tangosol.internal.net.NamedCacheDeactivationListener
    public void entryInserted(com.tangosol.util.MapEvent evt)
        {
        }
    
    // From interface: com.tangosol.internal.net.NamedCacheDeactivationListener
    public void entryUpdated(com.tangosol.util.MapEvent evt)
        {
        // import Component.Net.Extend.MessageFactory.NamedCacheFactory$MapEvent as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent;
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        Channel channel = getChannel();
        _assert(channel != null);
        
        com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
        com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent message = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent.TYPE_ID);
        
        // For older release, have to closeChannel to trigger front map reset, see RemoteNamedCache.unregisterChannel
        if (message.getImplVersion() >= 6)
            {
            message.setTruncate(true);
        
            try
                {
                channel.send(message);
                }
            catch (ConnectionException e)
                {
                // the Channel is most likely closing or has been closed
                }
            catch (Throwable t)
                {
                _trace(t, "Error sending MapEvent to " + channel);
                }
            }
        else
            {
            closeChannel();
            }
        }
    
    // Accessor for the property "Channel"
    /**
     * Getter for property Channel.<p>
    * The Channel registered with this proxy.
     */
    public com.tangosol.net.messaging.Channel getChannel()
        {
        return __m_Channel;
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        String sCacheName   = null;
        String sServiceName = null;
        
        try
            {
            sCacheName   = getCacheName();
            sServiceName = getCacheService().getInfo().getServiceName();
            }
        catch (Throwable t)
            {
            }
        return "NamedCache=" + (sCacheName   == null ? "N/A" : sCacheName)
            + ", Service="   + (sServiceName == null ? "N/A" : sServiceName);
        }
    
    // From interface: com.tangosol.net.NamedCache
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public String getName()
        {
        return toString();
        }
    
    // Accessor for the property "NamedCache"
    /**
     * Getter for property NamedCache.<p>
    * The NamedCache proxied by this NamedCacheProxy.
     */
    public com.tangosol.net.NamedCache getNamedCache()
        {
        return __m_NamedCache;
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public com.tangosol.net.messaging.Protocol getProtocol()
        {
        // import Component.Net.Extend.Protocol.NamedCacheProtocol;
        
        return NamedCacheProtocol.getInstance();
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
    
    // Accessor for the property "LockDeprecateWarned"
    /**
     * Getter for property LockDeprecateWarned.<p>
    * A boolean flag indicating whether we have warned user about the
    * deprecated lock methods.
     */
    public static boolean isLockDeprecateWarned()
        {
        return __s_LockDeprecateWarned;
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
    
    // From interface: com.tangosol.net.MemberListener
    public void memberJoined(com.tangosol.net.MemberEvent evt)
        {
        }
    
    // From interface: com.tangosol.net.MemberListener
    public void memberLeaving(com.tangosol.net.MemberEvent evt)
        {
        }
    
    // From interface: com.tangosol.net.MemberListener
    public void memberLeft(com.tangosol.net.MemberEvent evt)
        {
        // import Component.Net.Extend.MessageFactory.NamedCacheFactory$NoStorageMembers as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.NoStorageMembers;
        // import com.tangosol.net.DistributedCacheService;
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        Channel channel = getChannel();
        
        if (channel != null)
            {
            DistributedCacheService service = (DistributedCacheService) evt.getService();
        
            // avoid iterating the memberset (getOwnershipSenior()) if partition 0 has an assignment
            if (service.getPartitionOwner(0) == null && service.getOwnershipSenior() == null)
                {
                if (getProtocol().getCurrentVersion() >= 10)
                    {
                    com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
        
                    com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.NoStorageMembers message = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.NoStorageMembers) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.NoStorageMembers.TYPE_ID);
                    channel.send(message);
                    }
                else
                    {
                    closeChannel();
                    }
                }
            }
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        // import Component.Net.Extend.Message.Request.NamedCacheRequest;
        
        if (message instanceof NamedCacheRequest)
            {
            NamedCacheRequest request = (NamedCacheRequest) message;
        
            request.setNamedCache(this);
            request.setTransferThreshold(getTransferThreshold());
            }
        
        message.run();
        }
    
    public void printLockDeprecatedMessage()
        {
        if (!isLockDeprecateWarned())
            {
            _trace("Using the lock API from a Coherence*Extend client is deprecated and will be removed in a future release", 2);
            setLockDeprecateWarned(true);
            }
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void registerChannel(com.tangosol.net.messaging.Channel channel)
        {
        // import Component.Net.Extend.Proxy.MapListenerProxy;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.DistributedCacheService;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.util.SegmentedConcurrentMap;
        
        _assert(getChannel() == null);
        
        NamedCache cache = getNamedCache();
        _assert(cache != null);
        
        MapListenerProxy proxyListener = new MapListenerProxy();
        proxyListener.setChannel(channel);
        
        channel.setAttribute(ATTR_LISTENER, proxyListener);
        channel.setAttribute(ATTR_LOCK_MAP, new SegmentedConcurrentMap());
        
        setChannel(channel);
        
        cache.addMapListener(this);
        
        CacheService service = cache.getCacheService();
        if (service instanceof DistributedCacheService &&
            !((DistributedCacheService) service).isLocalStorageEnabled())
            {
            service.addMemberListener(this);
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void release()
        {
        // must release the NamedCache via the CacheServiceProxy
        throw new UnsupportedOperationException();
        }
    
    // From interface: java.lang.Runnable
    public void run()
        {
        }
    
    // Accessor for the property "Channel"
    /**
     * Setter for property Channel.<p>
    * The Channel registered with this proxy.
     */
    protected void setChannel(com.tangosol.net.messaging.Channel set)
        {
        __m_Channel = set;
        }
    
    // Accessor for the property "LockDeprecateWarned"
    /**
     * Setter for property LockDeprecateWarned.<p>
    * A boolean flag indicating whether we have warned user about the
    * deprecated lock methods.
     */
    public static void setLockDeprecateWarned(boolean fEnabled)
        {
        __s_LockDeprecateWarned = fEnabled;
        }
    
    // Accessor for the property "LockEnabled"
    /**
     * Setter for property LockEnabled.<p>
    * If false, NamedCache lock or unlock operation will be prohibited.
     */
    public void setLockEnabled(boolean fEnabled)
        {
        __m_LockEnabled = fEnabled;
        }
    
    // Accessor for the property "NamedCache"
    /**
     * Setter for property NamedCache.<p>
    * The NamedCache proxied by this NamedCacheProxy.
     */
    public void setNamedCache(com.tangosol.net.NamedCache cache)
        {
        __m_NamedCache = cache;
        }
    
    // Accessor for the property "ReadOnly"
    /**
     * Setter for property ReadOnly.<p>
    * If true, any NamedCache operation that may potentially modify cached
    * entries will be prohibited.
     */
    public void setReadOnly(boolean fReadOnly)
        {
        __m_ReadOnly = fReadOnly;
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
    public void setTransferThreshold(long cb)
        {
        __m_TransferThreshold = cb;
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void unregisterChannel(com.tangosol.net.messaging.Channel channel)
        {
        // import Component.Net.Extend.Connection as com.tangosol.coherence.component.net.extend.Connection;
        // import Component.Net.Extend.Proxy.MapListenerProxy;
        // import Component.Net.Extend.Proxy.NamedCacheProxy$CleanupTask as NamedCacheProxy.CleanupTask;
        // import Component.Util.DaemonPool;
        // import com.tangosol.net.messaging.Connection;
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.net.messaging.SuspectConnectionException;
        // import com.tangosol.net.NamedCache;
        // import java.util.Iterator;
        // import java.util.Map;
        
        _assert(getChannel() == channel);
        
        NamedCache cache = getNamedCache();
        _assert(cache != null);
        
        MapListenerProxy proxyListener = (MapListenerProxy) channel.getAttribute(ATTR_LISTENER);
        _assert(proxyListener != null);
        
        cache.removeMapListener(this);
        Connection     conn     = channel.getConnection();
        if (conn instanceof com.tangosol.coherence.component.net.extend.Connection)
            {
            com.tangosol.coherence.component.net.extend.Connection connImpl = (com.tangosol.coherence.component.net.extend.Connection) conn;
            Throwable      t        = connImpl.getCloseThrowable();
            if (t != null && t instanceof SuspectConnectionException)
                {
                if (_isTraceEnabled(3))
                    {
                    StringBuilder sb = new StringBuilder(connImpl.toString() + ' ');
        
                    // check that there are listeners to report
                    Map filterMap      = proxyListener.getFilterMap();
                    Map keyMap         = proxyListener.getKeyMap();
                    int nListenerCount = 0;                
        
                    if (filterMap.isEmpty() && keyMap.isEmpty())
                        {
                        sb.append("At the time it was closed, this suspect connection had no registered listeners"); 
                        }
                    else
                        {
                        sb.append("At the time it was closed, this suspect connection had registered " + keyMap.size()
                                + " key and " + filterMap.size() + " filter listeners, including "); 
        
                        if (!filterMap.isEmpty())
                            {
                            for (Iterator it = filterMap.keySet().iterator(); it.hasNext() && nListenerCount < 50; nListenerCount++)
                                {
                                Object   nextKey   = it.next();
                                Object[] nextValue = (Object[]) filterMap.get(nextKey);
                                sb.append("{filter=" + nextKey + ", Lite event=" + ((Boolean) nextValue[1]).toString() + '}');
                                }
                            }
                        sb.append(", (" + nListenerCount + '/' + filterMap.size() + " filter listeners logged)");
                        }
                     if (sb.length() > 0)
                        {
                        _trace(sb.toString(), 3);
                        }
                    }
                }
            }
        
        NamedCacheProxy.CleanupTask task = new NamedCacheProxy.CleanupTask();
        task.setProxy(this);
        DaemonPool pool = (DaemonPool) getDaemonPool();
        if (pool != null && pool.isStarted())
            {
            pool.add(task);
            }
        else
            {
            task.run();
            }
        
        channel.setAttribute(ATTR_LISTENER, null);
        channel.setAttribute(ATTR_LOCK_MAP, null);
        
        setChannel(null);
        }

    // ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy$CleanupTask
    
    /**
     * 
     * Task instantiated to cleanup the resources held by a proxy, such as
     * listeners or locks.  Runs when the proxy is disconnected, or the cache
     * released.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class CleanupTask
            extends    com.tangosol.coherence.component.util.Collections
            implements Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Proxy
         *
         * 
         * Proxy instance for which this cleanup task runs, in order to clean
         * up resources held by this proxy, such as listeners and locks.
         */
        private NamedCacheProxy __m_Proxy;
        
        // Default constructor
        public CleanupTask()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public CleanupTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.CleanupTask();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/NamedCacheProxy$CleanupTask".replace('/', '.'));
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
        
        // Accessor for the property "Proxy"
        /**
         * Getter for property Proxy.<p>
        * 
        * Proxy instance for which this cleanup task runs, in order to clean up
        * resources held by this proxy, such as listeners and locks.
         */
        public NamedCacheProxy getProxy()
            {
            return __m_Proxy;
            }
        
        // From interface: java.lang.Runnable
        public void run()
            {
            // import Component.Net.Extend.Proxy.MapListenerProxy;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.NamedCache;
            // import java.util.Iterator;
            // import java.util.Map;
            
            NamedCacheProxy  proxy         = (NamedCacheProxy) getProxy();
            NamedCache       cache         = proxy.getNamedCache();
            Channel          channel       = proxy.getChannel();
            MapListenerProxy proxyListener = (MapListenerProxy) channel.getAttribute(NamedCacheProxy.ATTR_LISTENER);
            
            Map mapLock = (Map) channel.getAttribute(NamedCacheProxy.ATTR_LOCK_MAP);
            
            
            try
                {
                 // release all listeners
                proxyListener.removeListener(proxy);
            
                // release all locks
                for (Iterator iter = mapLock.keySet().iterator(); iter.hasNext(); )
                    {
                    cache.unlock(iter.next());
                    }
                }
            catch (RuntimeException e)
                {
                // see COH-3315
                }
            }
        
        // Accessor for the property "Proxy"
        /**
         * Setter for property Proxy.<p>
        * 
        * Proxy instance for which this cleanup task runs, in order to clean up
        * resources held by this proxy, such as listeners and locks.
         */
        public void setProxy(NamedCacheProxy pool)
            {
            __m_Proxy = pool;
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy$EntrySet
    
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
            __mapChildren.put("Entry", NamedCacheProxy.EntrySet.Entry.get_CLASS());
            __mapChildren.put("Iterator", NamedCacheProxy.EntrySet.Iterator.get_CLASS());
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
            return new com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.EntrySet();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/NamedCacheProxy$EntrySet".replace('/', '.'));
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

        // ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy$EntrySet$Entry
        
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
                return new com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.EntrySet.Entry();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/NamedCacheProxy$EntrySet$Entry".replace('/', '.'));
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

        // ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy$EntrySet$Iterator
        
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
                return new com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.EntrySet.Iterator();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/NamedCacheProxy$EntrySet$Iterator".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy$KeySet
    
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
            __mapChildren.put("Iterator", NamedCacheProxy.KeySet.Iterator.get_CLASS());
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
            return new com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.KeySet();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/NamedCacheProxy$KeySet".replace('/', '.'));
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
            ((NamedCacheProxy) get_Module()).assertWriteable();
            return super.remove(o);
            }
        
        // Declared at the super level
        public boolean removeAll(java.util.Collection col)
            {
            ((NamedCacheProxy) get_Module()).assertWriteable();
            return removeAll(this, col);
            }
        
        // Declared at the super level
        /**
         * Helper method for colThis.removeAll(colThat)
         */
        public static boolean removeAll(java.util.Collection colThis, java.util.Collection colThat)
            {
            // import java.util.Iterator as java.util.Iterator;
            
            boolean fModified = false;
            for (java.util.Iterator iter = colThat.iterator(); iter.hasNext();)
                {
                fModified |= colThis.remove(iter.next());
                }
            
            return fModified;
            }

        // ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy$KeySet$Iterator
        
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
                return new com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.KeySet.Iterator();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/NamedCacheProxy$KeySet$Iterator".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy$Values
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Values
            extends    com.tangosol.coherence.component.util.collections.WrapperCollection
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
            __mapChildren.put("Iterator", NamedCacheProxy.Values.Iterator.get_CLASS());
            }
        
        // Default constructor
        public Values()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Values(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.Values();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/NamedCacheProxy$Values".replace('/', '.'));
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

        // ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy$Values$Iterator
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Iterator
                extends    com.tangosol.coherence.component.util.collections.WrapperCollection.Iterator
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
                return new com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy.Values.Iterator();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/NamedCacheProxy$Values$Iterator".replace('/', '.'));
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
    }
