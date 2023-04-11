
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.proxy.MapListenerProxy

package com.tangosol.coherence.component.net.extend.proxy;

import com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.Message;
import com.tangosol.util.Binary;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.filter.InKeySetFilter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * MapListener implementation used to send Messages containing information
 * about MapEvents raised by a NamedCache through a Channel.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class MapListenerProxy
        extends    com.tangosol.coherence.component.net.extend.Proxy
        implements com.tangosol.util.MapListenerSupport.SynchronousListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property Channel
     *
     * The Channel though which Messages containing information about MapEvents
     * will be sent.
     */
    private com.tangosol.net.messaging.Channel __m_Channel;
    
    /**
     * Property FilterMap
     *
     * The map of Filters that this MapListenerProxy was registered with. Each
     * value is a two element array, the first element being the unique
     * positive identifier for the Filter and the second being the
     * corresponding "lite" flag.
     */
    private com.tangosol.util.ConcurrentMap __m_FilterMap;
    
    /**
     * Property KeyMap
     *
     * The map of "normalized" keys that this MapListenerProxy was registered
     * with. Each value is the combination of the corresponding "lite" flag and
     * "priming" flag.
     */
    private com.tangosol.util.ConcurrentMap __m_KeyMap;
    
    /**
     * Property KeySet
     *
     * The set of keys that this MapListenerProxy was registered with.
     */
    private java.util.Set __m_KeySet;
    
    /**
     * Property LITE
     *
     * Constant to indicate that the listener is registered for "lite" events.
     */
    protected static final int LITE = 1;
    
    /**
     * Property PRIMING
     *
     * Constant to indicate that the listener is registered for "priming"
     * events.
     */
    protected static final int PRIMING = 2;
    
    /**
     * Property PrimingListener
     *
     * Wrapper map event listener. This listener registration should force a
     * synthetic event containing the current value to the requesting client.
     */
    private com.tangosol.util.MapListener __m_PrimingListener;
    
    // Default constructor
    public MapListenerProxy()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public MapListenerProxy(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setFilterMap(new com.tangosol.util.SegmentedConcurrentMap());
            setKeyMap(new com.tangosol.util.SegmentedConcurrentMap());
            setKeySet(new com.tangosol.util.SegmentedHashSet());
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
        return new com.tangosol.coherence.component.net.extend.proxy.MapListenerProxy();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/MapListenerProxy".replace('/', '.'));
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
    
    /**
     * Add this MapListenerProxy as a filter-based listener of the given
    * NamedCache.
    * 
    * @param cache  the NamedCache to listen to
    * @param filter  the Filter to listen to
    * @param lFilterId  the unique positive identifier of the Filter
    * @param fLite  true to add a "lite" listener
    * @param fPriming  true if the listener is a priming listener
     */
    public void addListener(com.tangosol.net.NamedCache cache, com.tangosol.util.Filter filter, long lFilterId, boolean fLite, boolean fPriming)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ConcurrentMap;
        // import com.tangosol.util.filter.InKeySetFilter;
        // import java.util.Iterator;
        // import java.util.Set;
        
        _assert(lFilterId > 0);
        
        if (filter instanceof InKeySetFilter)
            {
            InKeySetFilter filterKeys = (InKeySetFilter) filter;
        
            // the keys are always in the internal format
            // (see the corresponding RemoteNamedCache#addMapListener)
            for (Iterator iter = filterKeys.getKeys().iterator(); iter.hasNext();)
                {
                Binary binKey = (Binary) iter.next();
        
                addListener(cache, binKey, fLite, fPriming, false /*fRegister*/);
                }
        
            // make sure we don't "double dip" at the $ViewMap
            filterKeys.markConverted();
        
            cache.addMapListener(fPriming ? getPrimingListener() : this, filterKeys, fLite);
            }
        else if (fPriming)
            {
            throw new UnsupportedOperationException(
                "Priming listeners are only supported with InKeySetFilter");
            }
        else
            {  
            ConcurrentMap map = getFilterMap();
            map.lock(filter, -1L);
            try
                {              
                map.put(filter, new Object[] {Long.valueOf(lFilterId), Boolean.valueOf(fLite)});      
                cache.addMapListener(this, filter, fLite);
                }
            finally
                {
                map.unlock(filter);
                }
            }
        }
    
    /**
     * Add this MapListenerProxy as a key-based listener of the given
    * NamedCache.
    * 
    * @param cache  the NamedCache to listen to
    * @param oKey  the key to listen to
    * @param fLite  true to add a "lite" listener
    * @param fPriming true if the listener is a priming listener
     */
    public void addListener(com.tangosol.net.NamedCache cache, Object oKey, boolean fLite, boolean fPriming)
        {
        addListener(cache, oKey, fLite, fPriming, /*fRegister*/ true);
        }
    
    /**
     * Add this MapListenerProxy as a key-based listener of the given
    * NamedCache.
    * 
    * @param cache  the NamedCache to listen to
    * @param oKey  the key to listen to
    * @param fLite  true to add a "lite" listener
    * @param fPriming true if the listener is a priming listener
    * @param fRegister true if the listener should be added to the underlying
    * cache
     */
    protected void addListener(com.tangosol.net.NamedCache cache, Object oKey, boolean fLite, boolean fPriming, boolean fRegister)
        {
        // import Component.Net.Extend.MessageFactory.NamedCacheFactory$MapEvent as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent;
        // import Component.Net.Extend.Proxy.NamedCacheProxy;
        // import Component.Net.Message.MapEventMessage as com.tangosol.coherence.component.net.message.MapEventMessage;
        // import com.tangosol.net.cache.CacheEvent$TransformationState as com.tangosol.net.cache.CacheEvent.TransformationState;
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.util.ConcurrentMap;
        
        // normalize the key, if necessary
        Object oKeyDown = oKey;
        if (cache instanceof NamedCacheProxy)
            {
            oKey = normalizeKey(oKey);
            }
        
        ConcurrentMap map = getKeyMap();
        map.lock(oKey, -1L);
        try
            {
            int nFlags  = fLite    ? LITE    : 0;
                nFlags |= fPriming ? PRIMING : nFlags;
        
            if (map.containsKey(oKey))
                {
                // either a priming or non-priming listener was already registered
        
                nFlags = ((Integer) map.get(oKey)).intValue();
                if ((nFlags & PRIMING) == PRIMING)
                    {
                    fRegister = false;
                    }
        
                if (fPriming)
                    {
                    // was priming therefore nothing to do
                    fRegister = false;
                    
                    // as we have already registered a map listener and now
                    // we have a NearCache.get it is unnecessary to register
                    // the listener with PartitionedCache but we must dispatch
                    // the com.tangosol.coherence.component.net.message.MapEventMessage
        
                    Channel channel = getChannel();
                    com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
        
                    com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent message = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent.TYPE_ID);
                    message.setId(com.tangosol.coherence.component.net.message.MapEventMessage.ENTRY_UPDATED | com.tangosol.coherence.component.net.message.MapEventMessage.EVT_SYNTHETIC | com.tangosol.coherence.component.net.message.MapEventMessage.EVT_PRIMING);
                    message.setKey(oKey);
                    message.setSynthetic(true);
        
                    message.setTransformationState(com.tangosol.net.cache.CacheEvent.TransformationState.TRANSFORMABLE.ordinal());
                    message.setPriming(true);
                    message.setValueNew(cache.get(oKey));
        
                    channel.send(message);
        
                    nFlags   |= PRIMING;
                    fRegister = false;
                    }
                // else re-registration of map listener on the same key
        
                if (!fLite)
                    {
                    // switching from lite to heavy requires re-registration with storage
                    nFlags    &= ~LITE;
                    fRegister = true;
                    }
                }
        
            map.put(oKey, Integer.valueOf(nFlags));
              
            getKeySet().add(oKeyDown);
        
            if (fRegister)
                {
                cache.addMapListener(
                    fPriming ? getPrimingListener() : this, oKeyDown, fLite);
                }
            }
        finally
            {
            map.unlock(oKey);
            }
        }
    
    // From interface: com.tangosol.util.MapListenerSupport$SynchronousListener
    public void entryDeleted(com.tangosol.util.MapEvent evt)
        {
        onMapEvent(evt);
        }
    
    // From interface: com.tangosol.util.MapListenerSupport$SynchronousListener
    public void entryInserted(com.tangosol.util.MapEvent evt)
        {
        onMapEvent(evt);
        }
    
    // From interface: com.tangosol.util.MapListenerSupport$SynchronousListener
    public void entryUpdated(com.tangosol.util.MapEvent evt)
        {
        onMapEvent(evt);
        }
    
    // Accessor for the property "Channel"
    /**
     * Getter for property Channel.<p>
    * The Channel though which Messages containing information about MapEvents
    * will be sent.
     */
    public com.tangosol.net.messaging.Channel getChannel()
        {
        return __m_Channel;
        }
    
    // Accessor for the property "FilterMap"
    /**
     * Getter for property FilterMap.<p>
    * The map of Filters that this MapListenerProxy was registered with. Each
    * value is a two element array, the first element being the unique positive
    * identifier for the Filter and the second being the corresponding "lite"
    * flag.
     */
    public com.tangosol.util.ConcurrentMap getFilterMap()
        {
        return __m_FilterMap;
        }
    
    // Accessor for the property "KeyMap"
    /**
     * Getter for property KeyMap.<p>
    * The map of "normalized" keys that this MapListenerProxy was registered
    * with. Each value is the combination of the corresponding "lite" flag and
    * "priming" flag.
     */
    public com.tangosol.util.ConcurrentMap getKeyMap()
        {
        return __m_KeyMap;
        }
    
    // Accessor for the property "KeySet"
    /**
     * Getter for property KeySet.<p>
    * The set of keys that this MapListenerProxy was registered with.
     */
    public java.util.Set getKeySet()
        {
        return __m_KeySet;
        }
    
    // Accessor for the property "PrimingListener"
    /**
     * Getter for property PrimingListener.<p>
    * Wrapper map event listener. This listener registration should force a
    * synthetic event containing the current value to the requesting client.
     */
    protected com.tangosol.util.MapListener getPrimingListener()
        {
        // import com.tangosol.util.MapListenerSupport$WrapperPrimingListener as com.tangosol.util.MapListenerSupport.WrapperPrimingListener;
        
        com.tangosol.util.MapListenerSupport.WrapperPrimingListener listener = (com.tangosol.util.MapListenerSupport.WrapperPrimingListener) __m_PrimingListener;
        if (listener == null)
            {
            synchronized (this)
                {
                listener = (com.tangosol.util.MapListenerSupport.WrapperPrimingListener) __m_PrimingListener;
                if (listener == null)
                    {
                    listener = new com.tangosol.util.MapListenerSupport.WrapperPrimingListener(this);
                    setPrimingListener(listener);
                    }
                }
            }
        
        return listener;
        }
    
    /**
     * Factory method: create new Message(s) using the information in the
    * supplied MapEvent.
    * 
    * @param evt  the MapEvent used to configure the newly created Message(s)
     */
    protected com.tangosol.coherence.component.net.extend.Message[] instantiateMapEventMessages(com.tangosol.util.MapEvent evt)
        {
        // import Component.Net.Extend.MessageFactory.NamedCacheFactory$MapEvent as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent;
        // import com.tangosol.net.cache.CacheEvent;
        // import com.tangosol.net.cache.CacheEvent$TransformationState as com.tangosol.net.cache.CacheEvent.TransformationState;
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.MapEvent;
        // import com.tangosol.util.MapListenerSupport;
        // import com.tangosol.util.MapListenerSupport$FilterEvent as com.tangosol.util.MapListenerSupport.FilterEvent;
        // import java.util.Map;
        
        int        nId          = evt.getId();
        Object     oKey         = evt.getKey();
        Integer    NFlags       = (Integer) getKeyMap().get(oKey);
        boolean    fKeyLite     = NFlags == null || (NFlags.intValue() & LITE) != 0;
        boolean    fPriming     = NFlags != null && (NFlags.intValue() & PRIMING) != 0;
        int        cFilters     = 0;
        long[]     alFilterIds  = null;
        boolean[]  afFilterLite = null;
        boolean    fFilterLite  = true;
        CacheEvent evtCache     = evt instanceof CacheEvent ? (CacheEvent) evt : null;
        boolean    fSynthetic   = evtCache != null && evtCache.isSynthetic();
        boolean    fExpired     = evtCache != null && evtCache.isExpired();
        Map        mapFilters   = getFilterMap();
        
        // determine the identifier(s) of Filter(s) associated with the MapEvent
        MapEvent evtUnwrapped = MapListenerSupport.unwrapEvent(evt);
        if (evtUnwrapped instanceof com.tangosol.util.MapListenerSupport.FilterEvent)
            {
            Filter[] aFilters = ((com.tangosol.util.MapListenerSupport.FilterEvent) evtUnwrapped).getFilter();
        
            cFilters     = aFilters.length;
            alFilterIds  = new long[cFilters];
            afFilterLite = new boolean[cFilters];
            for (int i = 0; i < cFilters; ++i)
                {
                Object[] ao = (Object[]) mapFilters.get(aFilters[i]);
                if (ao != null)
                    {
                    // see #addListener
                    long    lId     = ((Long) ao[0]).longValue();
                    boolean fLite   = ((Boolean) ao[1]).booleanValue();
                    alFilterIds[i]  = lId;
                    afFilterLite[i] = fLite;
                    if (!fLite)
                        {
                        fFilterLite = false;
                        }
                    }
                }
            }
        else
            {
            Object[] ao   = (Object[]) mapFilters.get(null); // there was no filter
            if (ao != null)
                {
                long    lId   = ((Long) ao[0]).longValue();
                boolean fLite = ((Boolean) ao[1]).booleanValue();
        
                cFilters     = 1;
                alFilterIds  = new long[] {lId};
                afFilterLite = new boolean[] {fLite};
                fFilterLite  = fLite;
                }
            }
        
        Channel   channel  = getChannel();
        com.tangosol.net.messaging.Protocol.MessageFactory   factory  = channel.getMessageFactory();
        com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent[] aMessages;
        
        // COH-8238
        if (factory.getVersion() > 3 || cFilters == 0)
            {
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent message = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent.TYPE_ID);
            message.setId(nId);
            message.setFilterIds(alFilterIds);
            message.setKey(oKey);
            message.setSynthetic(fSynthetic);
            message.setExpired(fExpired);
        
            if (!fKeyLite || !fFilterLite || fPriming)
                {
                message.setValueNew(evt.getNewValue());
                if (!fPriming) // priming events don't need the old value
                    {
                    message.setValueOld(evt.getOldValue());
                    }
                }
        
            message.setTransformationState((evtCache == null
                                           ? com.tangosol.net.cache.CacheEvent.TransformationState.TRANSFORMABLE
                                           : evtCache.getTransformationState()).ordinal());
            message.setPriming(evtCache != null && evtCache.isPriming());
        
            aMessages = new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent[] {message};
            }
        else
            {
            aMessages = new com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent[cFilters];
            for (int i = 0; i < cFilters; ++i)
                {
                com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent message = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.MapEvent.TYPE_ID);
                message.setId(nId);
                message.setKey(oKey);
                message.setFilterId(alFilterIds[i]);
                message.setSynthetic(fSynthetic);
                message.setExpired(fExpired);
        
                if (!afFilterLite[i])
                    {
                    message.setValueNew(evt.getNewValue());
                    message.setValueOld(evt.getOldValue());
                    }
        
                aMessages[i] = message;
                }
            }
        
        return aMessages;
        }
    
    /**
     * Normalize the specified key to the format that matches the events fired
    * by the NamedCacheProxy.
    * 
    * @param oKey  the key to normalize
    * 
    * @return a normalized representation of the key
     */
    protected Object normalizeKey(Object oKey)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        
        if (oKey instanceof Binary)
            {
            // see RemoteNamedCache#addMapListener
            Binary binKey = (Binary) oKey;
            if (com.tangosol.util.ExternalizableHelper.isIntDecorated(binKey))
                {
                oKey = com.tangosol.util.ExternalizableHelper.removeIntDecoration(binKey);
                }
            }
        return oKey;
        }
    
    /**
     * Called when a MapEvent has been raised by the NamedCache that this
    * MapListener has been registered with.
    * 
    * @param evt  the MapEvent
     */
    public void onMapEvent(com.tangosol.util.MapEvent evt)
        {
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.net.messaging.Message;
        
        Channel channel = getChannel();
        _assert(channel != null);
        
        Message[] aMessages = instantiateMapEventMessages(evt);
        try
            {
            for (int i = 0, c = aMessages.length; i < c; ++i)
                {
                channel.send(aMessages[i]);
                }
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
    
    /**
     * Remove this MapListenerProxy as a listener of the given NamedCache.
    * 
    * @param cache  the NamedCache to stop listening to
     */
    public void removeListener(com.tangosol.net.NamedCache cache)
        {
        // import com.tangosol.util.ConcurrentMap;
        // import com.tangosol.util.Filter;
        // import java.util.Iterator;
        // import java.util.Set;
        
        // unregister all filter-based listeners
        ConcurrentMap map = getFilterMap();
        map.lock(ConcurrentMap.LOCK_ALL, -1L);
        try
            {
            for (Iterator iter = map.keySet().iterator(); iter.hasNext(); )
                {
                Filter filter = (Filter) iter.next();
                cache.removeMapListener(this, filter);
                }
            map.clear();
            }
        finally
            {
            map.unlock(ConcurrentMap.LOCK_ALL);
            }
        
        // unregister all key-based listeners
        map = getKeyMap();
        map.lock(ConcurrentMap.LOCK_ALL, -1L);
        try
            {
            Set set = getKeySet();
            for (Iterator iter = set.iterator(); iter.hasNext(); )
                {
                Object oKey     = iter.next();
                Object oKeyDown = oKey;
                if (cache instanceof NamedCacheProxy)
                    {
                    oKey = normalizeKey(oKey);
                    }
                Integer NFlags   = (Integer) map.remove(oKey);
                boolean fPriming = NFlags != null && (NFlags.intValue() & PRIMING) != 0;
                cache.removeMapListener(fPriming ? getPrimingListener() : this, oKeyDown);
                }
            set.clear();
            }
        finally
            {
            map.unlock(ConcurrentMap.LOCK_ALL);
            }
        }
    
    /**
     * Remove this MapListenerProxy as a filter-based listener of the given
    * NamedCache.
    * 
    * @param cache      the NamedCache to stop listening to
    * @param filter        the Filter to stop listening to
    * @param fPriming  true if the listener is a priming listener
     */
    public void removeListener(com.tangosol.net.NamedCache cache, com.tangosol.util.Filter filter, boolean fPriming)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ConcurrentMap;
        // import com.tangosol.util.filter.InKeySetFilter;
        // import java.util.Iterator;
        // import java.util.Set;
        
        if (filter instanceof InKeySetFilter)
            {
            InKeySetFilter filterKeys = (InKeySetFilter) filter;
        
            // the keys are always in the internal format
            // (see the corresponding RemoteNamedCache#addMapListener)
            for (Iterator iter = filterKeys.getKeys().iterator(); iter.hasNext();)
                {
                Binary binKey = (Binary) iter.next();
        
                removeListener(cache, binKey, fPriming, false /*fUnregister*/);
                }
        
            // make sure we don't "double dip" at the $ViewMap
            filterKeys.markConverted();
        
            cache.removeMapListener(fPriming ? getPrimingListener() : this, filterKeys);
            }
        else if (fPriming)
            {
            throw new UnsupportedOperationException(
                "Priming listeners are only supported with InKeySetFilter");
            }
        else
            {
            ConcurrentMap map = getFilterMap();
            map.lock(filter, -1L);
            try
                {
                if (map.remove(filter) != null)
                    {
                    cache.removeMapListener(this, filter);
                    }
                }
            finally
                {
                map.unlock(filter);
                }
            }
        }
    
    /**
     * Remove this MapListenerProxy as a key-based listener of the given
    * NamedCache.
    * 
    * @param cache      the NamedCache to stop listening to
    * @param oKey        the key to stop listening to
    * @param fPriming  true if the listener is a priming listener
     */
    public void removeListener(com.tangosol.net.NamedCache cache, Object oKey, boolean fPriming)
        {
        removeListener(cache, oKey, fPriming, /*fUnregister*/ true);
        }
    
    /**
     * Remove this MapListenerProxy as a key-based listener of the given
    * NamedCache.
    * 
    * @param cache          the NamedCache to stop listening to
    * @param oKey            the key to stop listening to
    * @param fPriming      true if the listener is a priming listener
    * @param fUnregister true if the listener should be removed from the
    * underlying cache
     */
    protected void removeListener(com.tangosol.net.NamedCache cache, Object oKey, boolean fPriming, boolean fUnregister)
        {
        // import Component.Net.Extend.Proxy.NamedCacheProxy;
        // import com.tangosol.util.ConcurrentMap;
        
        // normalize the key, if necessary
        Object oKeyDown = oKey;
        if (cache instanceof NamedCacheProxy)
            {
            oKey = normalizeKey(oKey);
            }
        
        ConcurrentMap map = getKeyMap();
        map.lock(oKey, -1L);
        try
            {
            Integer NFlags = (Integer) map.remove(oKey);
            if (NFlags != null)
                {
                int nFlags = NFlags.intValue();
        
                // only remove the priming listener if it was actually registered
                // @see addListener
                fPriming &= (nFlags & PRIMING) == PRIMING;
        
                if (getKeySet().remove(oKeyDown))
                    {
                    if (fUnregister)
                        {
                        cache.removeMapListener(fPriming ? getPrimingListener() : this, oKeyDown);
                        }
                    }
                else
                    {
                    _assert(false);
                    }
                }
            }
        finally
            {
            map.unlock(oKey);
            }
        }
    
    // Accessor for the property "Channel"
    /**
     * Setter for property Channel.<p>
    * The Channel though which Messages containing information about MapEvents
    * will be sent.
     */
    public void setChannel(com.tangosol.net.messaging.Channel channel)
        {
        _assert(getChannel() == null);
        
        __m_Channel = (channel);
        }
    
    // Accessor for the property "FilterMap"
    /**
     * Setter for property FilterMap.<p>
    * The map of Filters that this MapListenerProxy was registered with. Each
    * value is a two element array, the first element being the unique positive
    * identifier for the Filter and the second being the corresponding "lite"
    * flag.
     */
    protected void setFilterMap(com.tangosol.util.ConcurrentMap map)
        {
        __m_FilterMap = map;
        }
    
    // Accessor for the property "KeyMap"
    /**
     * Setter for property KeyMap.<p>
    * The map of "normalized" keys that this MapListenerProxy was registered
    * with. Each value is the combination of the corresponding "lite" flag and
    * "priming" flag.
     */
    protected void setKeyMap(com.tangosol.util.ConcurrentMap map)
        {
        __m_KeyMap = map;
        }
    
    // Accessor for the property "KeySet"
    /**
     * Setter for property KeySet.<p>
    * The set of keys that this MapListenerProxy was registered with.
     */
    protected void setKeySet(java.util.Set set)
        {
        __m_KeySet = set;
        }
    
    // Accessor for the property "PrimingListener"
    /**
     * Setter for property PrimingListener.<p>
    * Wrapper map event listener. This listener registration should force a
    * synthetic event containing the current value to the requesting client.
     */
    public void setPrimingListener(com.tangosol.util.MapListener sProperty)
        {
        __m_PrimingListener = sProperty;
        }
    }
