
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache

package com.tangosol.coherence.component.net.extend;

import com.tangosol.coherence.component.net.extend.protocol.NamedCacheProtocol;
import com.tangosol.application.ContainerHelper;
import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheMap;
import com.tangosol.net.cache.KeyAssociation;
import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.EntrySetMap;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.ImmutableMultiList;
import com.tangosol.util.Listeners;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.PagedIterator;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.comparator.EntryComparator;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.filter.InKeySetFilter;
import com.tangosol.util.filter.LimitFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NamedCache implementation that delegates to a remote NamedCache using a
 * Channel.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class RemoteNamedCache
        extends    com.tangosol.coherence.component.net.Extend
        implements com.tangosol.net.NamedCache,
                   com.tangosol.net.cache.BinaryEntryStore,
                   com.tangosol.net.cache.CacheStore,
                   com.tangosol.net.messaging.Channel.Receiver
    {
    // ---- Fields declarations ----
    
    /**
     * Property BinaryCache
     *
     * Child BinaryCache instance.
     */
    private RemoteNamedCache.BinaryCache __m_BinaryCache;
    
    /**
     * Property CacheName
     *
     * @see com.tangosol.net.NamedCache#getCacheName
     */
    private String __m_CacheName;
    
    /**
     * Property CacheService
     *
     * @see com.tangosol.net.NamedCache#getCacheService
     */
    private com.tangosol.net.CacheService __m_CacheService;
    
    /**
     * Property Channel
     *
     * The Channel used to exchange Messages with a remote ProxyService.
     */
    private com.tangosol.net.messaging.Channel __m_Channel;
    
    /**
     * Property ConverterCache
     *
     * The client view of the BinaryCache.
     */
    private com.tangosol.net.NamedCache __m_ConverterCache;
    
    /**
     * Property ConverterFromBinary
     *
     * Child ConverterFromBinary instance.
     */
    private RemoteNamedCache.ConverterFromBinary __m_ConverterFromBinary;
    
    /**
     * Property ConverterKeyToBinary
     *
     * Child ConverterKeyToBinary instance.
     */
    private RemoteNamedCache.ConverterKeyToBinary __m_ConverterKeyToBinary;
    
    /**
     * Property ConverterValueToBinary
     *
     * Child ConverterValueToBinary instance.
     */
    private RemoteNamedCache.ConverterValueToBinary __m_ConverterValueToBinary;
    
    /**
     * Property DeactivationListeners
     *
     * Registered NamedCacheDeactivationListeners.
     */
    private com.tangosol.util.Listeners __m_DeactivationListeners;
    
    /**
     * Property DeferKeyAssociationCheck
     *
     * Whether a key should be checked for KeyAssociation by the extend client
     * (false) or deferred until the key is received by the PartionedService
     * (true).
     */
    private boolean __m_DeferKeyAssociationCheck;
    
    /**
     * Property EventDispatcher
     *
     * The QueueProcessor used to dispatch MapEvents.
     */
    private transient com.tangosol.coherence.component.util.daemon.QueueProcessor __m_EventDispatcher;
    
    /**
     * Property LockDeprecateWarned
     *
     * A boolean flag indicating whether we have warned user about the
     * deprecated lock methods.
     */
    private static boolean __s_LockDeprecateWarned;
    
    /**
     * Property PassThrough
     *
     * A boolean flag indicating that this RemoteNamedCache is used by the
     * pass-through optimization and all the incoming and outgoing keys and
     * values are Binary objects.
     */
    private boolean __m_PassThrough;
    
    /**
     * Property Released
     *
     */
    private boolean __m_Released;
    
    // Default constructor
    public RemoteNamedCache()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public RemoteNamedCache(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new RemoteNamedCache.BinaryCache("BinaryCache", this, true), "BinaryCache");
        _addChild(new RemoteNamedCache.ConverterBinaryToDecoratedBinary("ConverterBinaryToDecoratedBinary", this, true), "ConverterBinaryToDecoratedBinary");
        _addChild(new RemoteNamedCache.ConverterBinaryToUndecoratedBinary("ConverterBinaryToUndecoratedBinary", this, true), "ConverterBinaryToUndecoratedBinary");
        _addChild(new RemoteNamedCache.ConverterFromBinary("ConverterFromBinary", this, true), "ConverterFromBinary");
        _addChild(new RemoteNamedCache.ConverterKeyToBinary("ConverterKeyToBinary", this, true), "ConverterKeyToBinary");
        _addChild(new RemoteNamedCache.ConverterValueToBinary("ConverterValueToBinary", this, true), "ConverterValueToBinary");
        
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
        return new com.tangosol.coherence.component.net.extend.RemoteNamedCache();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.NamedCache
    public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        getBinaryCache().addIndex(extractor, fOrdered, comparator);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;
        
        addMapListener(listener, (Filter) null, false);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
        {
        // import com.tangosol.internal.net.NamedCacheDeactivationListener;
        // import com.tangosol.util.filter.InKeySetFilter;
        
        if (listener instanceof NamedCacheDeactivationListener)
            {
            getDeactivationListeners().add(listener);
            }
        else
            {
            if (filter instanceof InKeySetFilter)
                {
                // clone the filter and serialize the keys
                InKeySetFilter filterKeys =
                    new InKeySetFilter(null, ((InKeySetFilter) filter).getKeys());
                filterKeys.ensureConverted(getConverterKeyToBinary());
                filter = filterKeys;
                }
        
            getBinaryCache().addMapListener(
                instantiateConverterListener(listener), filter, fLite);
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
        {
        getBinaryCache().addMapListener(
                instantiateConverterListener(listener),
                getConverterKeyToBinary().convert(oKey),
                fLite);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        return getConverterCache().aggregate(filter, agent);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object aggregate(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        return getConverterCache().aggregate(collKeys, agent);
        }

    /**
     * Coherence*Extend does not support AsyncNamedCache
     */
    // From interface: com.tangosol.net.NamedCache
    @Override
    public AsyncNamedCache async(AsyncNamedCache.Option... options)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void clear()
        {
        getBinaryCache().clear();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean containsKey(Object oKey)
        {
        return getConverterCache().containsKey(oKey);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean containsValue(Object oValue)
        {
        return getConverterCache().containsValue(oValue);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void destroy()
        {
        getCacheService().destroyCache(this);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet()
        {
        return getConverterCache().entrySet();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet(com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.filter.LimitFilter;
        
        // COH-2717
        if (filter instanceof LimitFilter)
            {
            ((LimitFilter) filter).setComparator(null);
            }
        
        return getConverterCache().entrySet(filter);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        // import com.tangosol.util.ImmutableArrayList;
        // import com.tangosol.util.comparator.EntryComparator;
        // import com.tangosol.util.comparator.SafeComparator;
        // import com.tangosol.util.filter.LimitFilter;
        // import java.util.Arrays;
        // import java.util.Comparator;
        // import java.util.Set;
        
        if (comparator == null)
            {
            comparator = SafeComparator.INSTANCE;
            }
        
        // COH-2717
        LimitFilter filterLimit = null;
        if (filter instanceof LimitFilter)
            {
            filterLimit = (LimitFilter) filter;
            filterLimit.setComparator(comparator);
            }
        
        Set set = getConverterCache().entrySet(filter);
        if (set.size() <= 1)
            {
            return set;
            }
        
        Object[]   aEntry    = set.toArray();
        Comparator compEntry = new EntryComparator(comparator);
        
        Arrays.sort(aEntry, compEntry);
        
        if (filterLimit == null)
            {
            return new ImmutableArrayList(aEntry);
            }
        else
            {
            filterLimit.setComparator(compEntry);
            set = new ImmutableArrayList(filterLimit.extractPage(aEntry));
            filterLimit.setComparator(comparator);
            return set;
            }
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void erase(com.tangosol.util.BinaryEntry binEntry)
        {
        getBinaryCache().remove(binEntry.getBinaryKey(), false);
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public void erase(Object oKey)
        {
        getBinaryCache().remove(getConverterKeyToBinary().convert(oKey), false);
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public void eraseAll(java.util.Collection colKeys)
        {
        // import com.tangosol.util.ConverterCollections;
        
        getBinaryCache().removeAll(ConverterCollections.getCollection(colKeys,
                getConverterKeyToBinary(),
                getConverterFromBinary()));
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void eraseAll(java.util.Set setBinEntries)
        {
        // import com.tangosol.util.EntrySetMap;
        
        getBinaryCache().removeAll(new EntrySetMap(setBinEntries).keySet());
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object get(Object oKey)
        {
        return getConverterCache().get(oKey);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map getAll(java.util.Collection colKeys)
        {
        return getConverterCache().getAll(colKeys);
        }
    
    // Accessor for the property "BinaryCache"
    /**
     * Getter for property BinaryCache.<p>
    * Child BinaryCache instance.
     */
    public RemoteNamedCache.BinaryCache getBinaryCache()
        {
        return __m_BinaryCache;
        }
    
    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "CacheName"
    /**
     * Getter for property CacheName.<p>
    * @see com.tangosol.net.NamedCache#getCacheName
     */
    public String getCacheName()
        {
        return __m_CacheName;
        }
    
    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "CacheService"
    /**
     * Getter for property CacheService.<p>
    * @see com.tangosol.net.NamedCache#getCacheService
     */
    public com.tangosol.net.CacheService getCacheService()
        {
        return __m_CacheService;
        }
    
    // Accessor for the property "Channel"
    /**
     * Getter for property Channel.<p>
    * The Channel used to exchange Messages with a remote ProxyService.
     */
    public com.tangosol.net.messaging.Channel getChannel()
        {
        return __m_Channel;
        }
    
    // Accessor for the property "ConverterCache"
    /**
     * Getter for property ConverterCache.<p>
    * The client view of the BinaryCache.
     */
    public com.tangosol.net.NamedCache getConverterCache()
        {
        return __m_ConverterCache;
        }
    
    // Accessor for the property "ConverterFromBinary"
    /**
     * Getter for property ConverterFromBinary.<p>
    * Child ConverterFromBinary instance.
     */
    public RemoteNamedCache.ConverterFromBinary getConverterFromBinary()
        {
        return __m_ConverterFromBinary;
        }
    
    // Accessor for the property "ConverterKeyToBinary"
    /**
     * Getter for property ConverterKeyToBinary.<p>
    * Child ConverterKeyToBinary instance.
     */
    public RemoteNamedCache.ConverterKeyToBinary getConverterKeyToBinary()
        {
        return __m_ConverterKeyToBinary;
        }
    
    // Accessor for the property "ConverterValueToBinary"
    /**
     * Getter for property ConverterValueToBinary.<p>
    * Child ConverterValueToBinary instance.
     */
    public RemoteNamedCache.ConverterValueToBinary getConverterValueToBinary()
        {
        return __m_ConverterValueToBinary;
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
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        return "NamedCache=" + getCacheName()
            + ", Service=" + getCacheService().getInfo().getServiceName();
        }
    
    // Accessor for the property "EventDispatcher"
    /**
     * Getter for property EventDispatcher.<p>
    * The QueueProcessor used to dispatch MapEvents.
     */
    public com.tangosol.coherence.component.util.daemon.QueueProcessor getEventDispatcher()
        {
        return __m_EventDispatcher;
        }
    
    // From interface: com.tangosol.net.NamedCache
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public String getName()
        {
        return "RemoteNamedCache(Cache=" + getCacheName() + ")";
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public com.tangosol.net.messaging.Protocol getProtocol()
        {
        // import Component.Net.Extend.Protocol.NamedCacheProtocol;
        
        return NamedCacheProtocol.getInstance();
        }
    
    /**
     * Instantiate and configure a new ConverterListener for the given
    * MapListener.
    * 
    * @param listener  the MapListener to wrap
    * 
    * @return a new ConverterListener that wraps the given MapListener
     */
    protected com.tangosol.util.MapListener instantiateConverterListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.application.ContainerHelper;
        // import com.tangosol.util.Converter;
        // import com.tangosol.util.ConverterCollections;
        // import com.tangosol.util.MapTriggerListener;
        
        if (listener == null)
            {
            throw new IllegalArgumentException("listener cannot be null");
            }
        
        if (listener instanceof MapTriggerListener)
            {
            return listener;
            }
        
        Converter conv = getConverterFromBinary();
        
        return ContainerHelper.getContextAwareListener(getCacheService(),
            ConverterCollections.getMapListener(this, listener, conv, conv));
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object invoke(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getConverterCache().invoke(oKey, agent);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getConverterCache().invokeAll(filter, agent);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getConverterCache().invokeAll(collKeys, agent);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean isActive()
        {
        return getBinaryCache().isActive();
        }

    @Override
    public boolean isReady()
        {
        return getBinaryCache().isReady();
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
    
    // From interface: com.tangosol.net.NamedCache
    public boolean isDestroyed()
        {
        return getBinaryCache().isDestroyed();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean isEmpty()
        {
        return getBinaryCache().isEmpty();
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
    
    // Accessor for the property "PassThrough"
    /**
     * Getter for property PassThrough.<p>
    * A boolean flag indicating that this RemoteNamedCache is used by the
    * pass-through optimization and all the incoming and outgoing keys and
    * values are Binary objects.
     */
    public boolean isPassThrough()
        {
        return __m_PassThrough;
        }
    
    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "Released"
    /**
     * Getter for property Released.<p>
     */
    public boolean isReleased()
        {
        // the trivial script is needed to work around a TDE issue
        // regarding a default interface method with same name.
        return __m_Released;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set keySet()
        {
        return getConverterCache().keySet();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set keySet(com.tangosol.util.Filter filter)
        {
        return getConverterCache().keySet(filter);
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void load(com.tangosol.util.BinaryEntry binEntry)
        {
        // import com.tangosol.util.Binary;
        
        binEntry.updateBinaryValue((Binary) getBinaryCache().get(binEntry.getBinaryKey()));
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public Object load(Object oKey)
        {
        return getConverterCache().get(oKey);
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public java.util.Map loadAll(java.util.Collection colKeys)
        {
        return getConverterCache().getAll(colKeys);
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void loadAll(java.util.Set setBinEntries)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.BinaryEntry;
        // import com.tangosol.util.EntrySetMap;
        // import java.util.Iterator;
        // import java.util.Map;
        
        Map map = getBinaryCache().getAll(new EntrySetMap(setBinEntries).keySet());
        for (Iterator iter = setBinEntries.iterator(); iter.hasNext();)
            {
            BinaryEntry binEntry = (BinaryEntry) iter.next();
        
            Binary binValue = (Binary) map.get(binEntry.getBinaryKey());
            if (binValue != null)
                {
                binEntry.updateBinaryValue(binValue);
                }
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean lock(Object oKey)
        {
        return lock(oKey, 0L);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean lock(Object oKey, long cWait)
        {
        // import com.tangosol.util.ConcurrentMap;
        
        printLockDeprecatedMessage();
        
        if (oKey == ConcurrentMap.LOCK_ALL)
            {
            throw new UnsupportedOperationException(
                    "RemoteNamedCache does not support LOCK_ALL");
            }
        
        return getConverterCache().lock(oKey, cWait);
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void onChannelClosed(com.tangosol.net.messaging.Channel channel)
        {
        // import Component.Util.CacheEvent as com.tangosol.coherence.component.util.CacheEvent;
        // import com.tangosol.net.cache.CacheEvent;
        // import com.tangosol.util.Listeners;
        
        Listeners listeners = getDeactivationListeners();
        if (!listeners.isEmpty())
            {
            CacheEvent evt = new CacheEvent(this, CacheEvent.ENTRY_DELETED, null, null, null, true);
            // dispatch the event to the listeners, which are all synchronous (hence the null Queue)
            com.tangosol.coherence.component.util.CacheEvent.dispatchSafe(evt, listeners, null /*Queue*/);    
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
        // import com.tangosol.util.ConverterCollections;
        
        setBinaryCache((RemoteNamedCache.BinaryCache) _findChild("BinaryCache"));
        setConverterFromBinary((RemoteNamedCache.ConverterFromBinary) _findChild("ConverterFromBinary"));
        setConverterKeyToBinary((RemoteNamedCache.ConverterKeyToBinary) _findChild("ConverterKeyToBinary"));
        setConverterValueToBinary((RemoteNamedCache.ConverterValueToBinary) _findChild("ConverterValueToBinary"));
        
        setConverterCache(ConverterCollections.getNamedCache(getBinaryCache(),
                getConverterFromBinary(),
                getConverterKeyToBinary(),
                getConverterFromBinary(),
                getConverterValueToBinary()));
        
        super.onInit();
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
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
    
    // From interface: com.tangosol.net.NamedCache
    public Object put(Object oKey, Object oValue)
        {
        return getConverterCache().put(oKey, oValue);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        return getConverterCache().put(oKey, oValue, cMillis);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void putAll(java.util.Map mapEntries)
        {
        getConverterCache().putAll(mapEntries);
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void registerChannel(com.tangosol.net.messaging.Channel channel)
        {
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.pof.ConfigurablePofContext;
        
        setChannel(channel);
        
        Serializer serializer = channel.getSerializer();
        getBinaryCache().setChannel(channel);
        getConverterFromBinary().setSerializer(serializer);
        getConverterValueToBinary().setSerializer(serializer);
        
        if (serializer instanceof ConfigurablePofContext)
            {
            ConfigurablePofContext cpc = (ConfigurablePofContext) serializer;
            if (cpc.isReferenceEnabled())
                {
                cpc = new ConfigurablePofContext(cpc);
                cpc.setReferenceEnabled(false);
                serializer = cpc;
                }
            }
        getConverterKeyToBinary().setSerializer(serializer);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void release()
        {
        setReleased(true);
        getCacheService().releaseCache(this);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object remove(Object oKey)
        {
        return getConverterCache().remove(oKey);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeIndex(com.tangosol.util.ValueExtractor extractor)
        {
        getBinaryCache().removeIndex(extractor);
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
        // import com.tangosol.internal.net.NamedCacheDeactivationListener;
        // import com.tangosol.util.filter.InKeySetFilter;
        
        if (listener instanceof NamedCacheDeactivationListener)
            {
            getDeactivationListeners().remove(listener);
            }
        else
            {
            if (filter instanceof InKeySetFilter)
                {
                // clone the filter and serialize the keys
                InKeySetFilter filterKeys =
                    new InKeySetFilter(null, ((InKeySetFilter) filter).getKeys());
                filterKeys.ensureConverted(getConverterKeyToBinary());
                filter = filterKeys;
                }
        
            getBinaryCache().removeMapListener(
                instantiateConverterListener(listener), filter);
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
        {
        getBinaryCache().removeMapListener(
                instantiateConverterListener(listener),
                getConverterKeyToBinary().convert(oKey));
        }
    
    // Accessor for the property "BinaryCache"
    /**
     * Setter for property BinaryCache.<p>
    * Child BinaryCache instance.
     */
    protected void setBinaryCache(RemoteNamedCache.BinaryCache cache)
        {
        __m_BinaryCache = cache;
        }
    
    // Accessor for the property "CacheName"
    /**
     * Setter for property CacheName.<p>
    * @see com.tangosol.net.NamedCache#getCacheName
     */
    public void setCacheName(String sName)
        {
        __m_CacheName = sName;
        }
    
    // Accessor for the property "CacheService"
    /**
     * Setter for property CacheService.<p>
    * @see com.tangosol.net.NamedCache#getCacheService
     */
    public void setCacheService(com.tangosol.net.CacheService service)
        {
        __m_CacheService = service;
        }
    
    // Accessor for the property "Channel"
    /**
     * Setter for property Channel.<p>
    * The Channel used to exchange Messages with a remote ProxyService.
     */
    public void setChannel(com.tangosol.net.messaging.Channel channel)
        {
        __m_Channel = channel;
        }
    
    // Accessor for the property "ConverterCache"
    /**
     * Setter for property ConverterCache.<p>
    * The client view of the BinaryCache.
     */
    protected void setConverterCache(com.tangosol.net.NamedCache cache)
        {
        __m_ConverterCache = cache;
        }
    
    // Accessor for the property "ConverterFromBinary"
    /**
     * Setter for property ConverterFromBinary.<p>
    * Child ConverterFromBinary instance.
     */
    protected void setConverterFromBinary(RemoteNamedCache.ConverterFromBinary conv)
        {
        __m_ConverterFromBinary = conv;
        }
    
    // Accessor for the property "ConverterKeyToBinary"
    /**
     * Setter for property ConverterKeyToBinary.<p>
    * Child ConverterKeyToBinary instance.
     */
    protected void setConverterKeyToBinary(RemoteNamedCache.ConverterKeyToBinary conv)
        {
        __m_ConverterKeyToBinary = conv;
        }
    
    // Accessor for the property "ConverterValueToBinary"
    /**
     * Setter for property ConverterValueToBinary.<p>
    * Child ConverterValueToBinary instance.
     */
    protected void setConverterValueToBinary(RemoteNamedCache.ConverterValueToBinary conv)
        {
        __m_ConverterValueToBinary = conv;
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
    
    // Accessor for the property "DeferKeyAssociationCheck"
    /**
     * Setter for property DeferKeyAssociationCheck.<p>
    * Whether a key should be checked for KeyAssociation by the extend client
    * (false) or deferred until the key is received by the PartionedService
    * (true).
     */
    public void setDeferKeyAssociationCheck(boolean fDefer)
        {
        __m_DeferKeyAssociationCheck = fDefer;
        }
    
    // Accessor for the property "EventDispatcher"
    /**
     * Setter for property EventDispatcher.<p>
    * The QueueProcessor used to dispatch MapEvents.
     */
    public void setEventDispatcher(com.tangosol.coherence.component.util.daemon.QueueProcessor dispatcher)
        {
        __m_EventDispatcher = (dispatcher);
        
        getBinaryCache().setEventDispatcher(dispatcher);
        }
    
    // Accessor for the property "LockDeprecateWarned"
    /**
     * Setter for property LockDeprecateWarned.<p>
    * A boolean flag indicating whether we have warned user about the
    * deprecated lock methods.
     */
    public static void setLockDeprecateWarned(boolean fDeprecateWarned)
        {
        __s_LockDeprecateWarned = fDeprecateWarned;
        }
    
    // Accessor for the property "PassThrough"
    /**
     * Setter for property PassThrough.<p>
    * A boolean flag indicating that this RemoteNamedCache is used by the
    * pass-through optimization and all the incoming and outgoing keys and
    * values are Binary objects.
     */
    public void setPassThrough(boolean fPassThrough)
        {
        __m_PassThrough = fPassThrough;
        }
    
    // Accessor for the property "Released"
    /**
     * Setter for property Released.<p>
     */
    public void setReleased(boolean fRelease)
        {
        __m_Released = fRelease;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public int size()
        {
        return getBinaryCache().size();
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void store(com.tangosol.util.BinaryEntry binEntry)
        {
        // import com.tangosol.net.cache.CacheMap;
        
        getBinaryCache().put(binEntry.getBinaryKey(), binEntry.getBinaryValue(),
            CacheMap.EXPIRY_DEFAULT, false);
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public void store(Object oKey, Object oValue)
        {
        // import com.tangosol.net.cache.CacheMap;
        // import com.tangosol.util.Converter;
        
        getBinaryCache().put(getConverterKeyToBinary().convert(oKey),
            getConverterValueToBinary().convert(oValue),
            CacheMap.EXPIRY_DEFAULT, false);
        }
    
    // From interface: com.tangosol.net.cache.CacheStore
    public void storeAll(java.util.Map mapEntries)
        {
        getConverterCache().putAll(mapEntries);
        }
    
    // From interface: com.tangosol.net.cache.BinaryEntryStore
    public void storeAll(java.util.Set setBinEntries)
        {
        // import com.tangosol.util.EntrySetMap;
        
        getBinaryCache().putAll(new EntrySetMap(setBinEntries));
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void truncate()
        {
        getBinaryCache().truncate();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean unlock(Object oKey)
        {
        // import com.tangosol.util.ConcurrentMap;
        
        printLockDeprecatedMessage();
        
        if (oKey == ConcurrentMap.LOCK_ALL)
            {
            throw new UnsupportedOperationException(
                    "RemoteNamedCache does not support LOCK_ALL");
            }
        
        return getConverterCache().unlock(oKey);
        }
    
    // From interface: com.tangosol.net.messaging.Channel$Receiver
    public void unregisterChannel(com.tangosol.net.messaging.Channel channel)
        {
        setChannel(null);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Collection values()
        {
        return getConverterCache().values();
        }

    // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$BinaryCache
    
    /**
     * The internal view of the RemoteNamedCache.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BinaryCache
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.net.NamedCache
        {
        // ---- Fields declarations ----
        
        /**
         * Property Channel
         *
         * The Channel used to exchange Messages with a remote ProxyService.
         */
        private com.tangosol.net.messaging.Channel __m_Channel;
        
        /**
         * Property ConverterBinaryToDecoratedBinary
         *
         * The parent ConverterBinaryToDecoratedBinary instance.
         */
        private RemoteNamedCache.ConverterBinaryToDecoratedBinary __m_ConverterBinaryToDecoratedBinary;
        
        /**
         * Property ConverterBinaryToUndecoratedBinary
         *
         * The parent ConverterBinaryToUndecoratedBinary instance.
         */
        private RemoteNamedCache.ConverterBinaryToUndecoratedBinary __m_ConverterBinaryToUndecoratedBinary;
        
        /**
         * Property EntrySet
         *
         * The child EntrySet instance.
         */
        private java.util.Set __m_EntrySet;
        
        /**
         * Property EventDispatcher
         *
         * The QueueProcessor used to dispatch MapEvents.
         */
        private transient com.tangosol.coherence.component.util.daemon.QueueProcessor __m_EventDispatcher;
        
        /**
         * Property FilterArray
         *
         * A LongArray of Filter objects indexed by the unique filter id. These
         * filter id values are used by the MapEvent message to specify what
         * filters caused a cache event.
         * 
         * Note: all access (for update) to this array should be synchronized
         * on the MapListenerSupport object.
         */
        private com.tangosol.util.LongArray __m_FilterArray;
        
        /**
         * Property KeySet
         *
         * The child KeySet instance.
         */
        private java.util.Set __m_KeySet;
        
        /**
         * Property MapListenerSupport
         *
         * MapListenerSupport used by this BinaryCache to dispatch MapEvents to
         * registered MapListeners.
         */
        private com.tangosol.util.MapListenerSupport __m_MapListenerSupport;
        
        /**
         * Property Values
         *
         * The child Values instance.
         */
        private java.util.Collection __m_Values;
        
        // Default constructor
        public BinaryCache()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public BinaryCache(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setFilterArray(new com.tangosol.util.SparseArray());
                setMapListenerSupport(new com.tangosol.util.MapListenerSupport());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new RemoteNamedCache.BinaryCache.EntrySet("EntrySet", this, true), "EntrySet");
            _addChild(new RemoteNamedCache.BinaryCache.KeySet("KeySet", this, true), "KeySet");
            _addChild(new RemoteNamedCache.BinaryCache.Values("Values", this, true), "Values");
            
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
            return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.BinaryCache();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$BinaryCache".replace('/', '.'));
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
        public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$IndexRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.IndexRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.IndexRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.IndexRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.IndexRequest.TYPE_ID);
            
            request.setAdd(true);
            request.setComparator(comparator);
            request.setExtractor(extractor);
            request.setOrdered(fOrdered);
            
            channel.request(request);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public void addMapListener(com.tangosol.util.MapListener listener)
            {
            // import com.tangosol.util.Filter;
            
            addMapListener(listener, (Filter) null, false);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
            {
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.MapListenerSupport;
            // import com.tangosol.util.MapTriggerListener;
            // import com.tangosol.util.filter.InKeySetFilter;
            // import java.util.HashMap;
            // import java.util.Iterator;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            // import java.util.Set;
            
            MapListenerSupport support  = getMapListenerSupport();
            boolean            fPriming = MapListenerSupport.isPrimingListener(listener);
            
            if (listener instanceof MapTriggerListener)
                {
                addRemoteMapListener(filter, 0L, fLite,
                        ((MapTriggerListener) listener).getTrigger(), false /*fPriming*/);
                }
            else if (filter instanceof InKeySetFilter)
                {
                InKeySetFilter filterKeys = (InKeySetFilter) filter;
            
                Set setKeys = filterKeys.getKeys();        // <Binary>
                Map mapKeys = new HashMap(setKeys.size()); // <Binary, Boolean>
            
                // we need to strip the int-decoration before passing keys to support
                // since the events always carry keys with stripped int-decoration
                RemoteNamedCache.ConverterBinaryToUndecoratedBinary conv
                    = getConverterBinaryToUndecoratedBinary();
            
                for (Iterator iter = setKeys.iterator(); iter.hasNext();)
                    {
                    Binary binKey = (Binary) conv.convert(iter.next());
            
                    boolean fNew = support.addListenerWithCheck(listener, binKey, fLite);   
            
                    // "priming" requests should be sent regardless
                    if (fNew || fPriming)
                        {
                        mapKeys.put(binKey, Boolean.valueOf(fNew));
                        }
                    else
                        {
                        // we can safely remove the keys since the set was already cloned
                        iter.remove();
                        }
                    }
            
                if (!setKeys.isEmpty())
                    {
                    try
                        {
                        addRemoteMapListener(filterKeys, 1 /*dummy*/, fLite, null, fPriming);
                        }
                    catch (RuntimeException e)
                        {
                        // only unregister the "new" listeners
                        for (Iterator iter = mapKeys.entrySet().iterator(); iter.hasNext();)
                            {
                            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
                            if (entry.getValue() == Boolean.TRUE)
                                {
                                support.removeListener(listener, entry.getKey());
                                }
                            }
            
                        throw e;
                        }
                    }
                }
            else if (fPriming)
                {
                throw new UnsupportedOperationException(
                    "Priming listeners are only supported with InKeySetFilter");
                }
            else
                {
                long lFilterId;
            
                synchronized (support)
                    {
                    boolean fWasEmpty = support.isEmpty(filter);
            
                    if (!support.addListenerWithCheck(listener, filter, fLite))
                        {
                        return;
                        }
            
                    lFilterId = fWasEmpty ? registerFilter(filter) : getFilterId(filter);
                    }
            
                try
                    {
                    addRemoteMapListener(filter, lFilterId, fLite, null, false /*fPriming*/);
                    }
                catch (RuntimeException e)
                    {
                    synchronized (support)
                        {
                        getFilterArray().remove(lFilterId);
                        support.removeListener(listener, filter);
                        }
                    throw e;
                    }
                }
            }
        
        // From interface: com.tangosol.net.NamedCache
        public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
            {
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.MapListenerSupport;
            // import com.tangosol.util.MapTriggerListener;
            
            if (oKey == null)
                {
                throw new IllegalArgumentException("key cannot be null");
                }
            
            if (listener instanceof MapTriggerListener)
                {
                addRemoteMapListener(oKey, fLite,
                        ((MapTriggerListener) listener).getTrigger(), false /*fPriming*/);
                }
            else
                {
                // keys for received MapEvents will not be int-decorated
                Binary binKey = (Binary) getConverterBinaryToUndecoratedBinary().convert(oKey);
            
                MapListenerSupport support = getMapListenerSupport();
            
                boolean fNew     = support.addListenerWithCheck(listener, binKey, fLite);
                boolean fPriming = support.isPrimingListener(listener);
            
                // "priming" request should be sent regardless
                if (fNew || fPriming)
                    {
                    try
                        {
                        addRemoteMapListener(oKey, fLite, null, fPriming);
                        }
                    catch (RuntimeException e)
                        {
                        if (fNew)
                            {
                            support.removeListener(listener, binKey);
                            }
                        throw e;
                        }
                    }
                }
            }
        
        /**
         * Send a request to the remote NamedCacheProxy to register a
        * MapListener.
        * 
        * @param filter       the Filter used to register the remote
        * MapListener
        * @param lFilterId  the unqiue positive identifier for the specified
        * Filter
        * @param fLite       if the remote MapListener should be "lite"
        * @param trigger   the optional MapTrigger to associate with the
        * request
        * @param fPriming flag indicating if the call is for a NearCache
        * priming listener
         */
        protected void addRemoteMapListener(com.tangosol.util.Filter filter, long lFilterId, boolean fLite, com.tangosol.util.MapTrigger trigger, boolean fPriming)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$ListenerFilterRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerFilterRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            // import com.tangosol.util.filter.InKeySetFilter;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerFilterRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerFilterRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerFilterRequest.TYPE_ID);
            
            // COH-4615
            if (request.getImplVersion() <= 5 && filter instanceof InKeySetFilter && fLite)
                {
                throw new UnsupportedOperationException(
                    "Priming events are not supported");
                }
            
            request.setAdd(true);
            request.setFilter(filter);
            request.setFilterId(lFilterId);
            request.setLite(fLite);
            request.setTrigger(trigger);
            request.setPriming(fPriming);
            
            channel.request(request);
            }
        
        /**
         * Send a request to the remote NamedCacheProxy to register a
        * MapListener.
        * 
        * @param oKey  the key used to register the remote MapListener
        * @param fLite   if the remote MapListener should be "lite"
        * @param trigger  the optional MapTrigger to associate with the request
        * @param fPriming flag indicating if the call is for a NearCache
        * priming listener
         */
        protected void addRemoteMapListener(Object oKey, boolean fLite, com.tangosol.util.MapTrigger trigger, boolean fPriming)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$ListenerKeyRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerKeyRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerKeyRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerKeyRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerKeyRequest.TYPE_ID);
            
            request.setAdd(true);
            request.setKey(oKey);
            request.setLite(fLite);
            request.setTrigger(trigger);
            request.setPriming(fPriming);
            
            channel.request(request);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$AggregateFilterRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateFilterRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateFilterRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateFilterRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateFilterRequest.TYPE_ID);
            
            request.setAggregator(agent);
            request.setFilter(filter);
            
            return channel.request(request);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public Object aggregate(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$AggregateAllRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateAllRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateAllRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateAllRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.AggregateAllRequest.TYPE_ID);
            
            request.setAggregator(agent);
            request.setKeySet(collKeys);
            
            return channel.request(request);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public void clear()
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$ClearRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ClearRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ClearRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ClearRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ClearRequest.TYPE_ID);
            
            channel.request(request);
            }
        
        /**
         * Determine if the remote NamedCache contains the specified keys.
        * 
        * @param colKeys  the keys
        * 
        * @return true if the NamedCache contains the specified keys
         */
        public boolean containsAll(java.util.Collection colKeys)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$ContainsAllRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsAllRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsAllRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsAllRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsAllRequest.TYPE_ID);
            
            request.setKeySet(colKeys);
            
            return ((Boolean) channel.request(request)).booleanValue();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public boolean containsKey(Object oKey)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$ContainsKeyRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsKeyRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsKeyRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsKeyRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsKeyRequest.TYPE_ID);
            
            request.setKey(oKey);
            
            return ((Boolean) channel.request(request)).booleanValue();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public boolean containsValue(Object oValue)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$ContainsValueRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsValueRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsValueRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsValueRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ContainsValueRequest.TYPE_ID);
            
            request.setValue(oValue);
            
            return ((Boolean) channel.request(request)).booleanValue();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public void destroy()
            {
            }
        
        /**
         * Dispatch a MapEvent created using the supplied information to the
        * MapListeners registered with this BinaryCache.
        * 
        * @param nEventId       the identifier of the MapEvent
        * @param alFilterIds       the positive unique identifier(s) of the
        * Filter(s) that caused this MapEvent to be dispatched
        * @param oKey            the key associated with the MapEvent
        * @param oValueOld    the old value associated with the MapEvent
        * @param oValueNew  the new value associated with the MapEvent
        * @param fSynthetic    true if the MapEvent occured because of internal
        * cache processing
        * @param nTransformState  describes how a MapEvent has been or should
        * be transformed
        * @param fPriming   true if the event is a priming event
        * @param fExpired   true if the MapEvent results from a time-based
        * eviction event
         */
        public void dispatch(int nEventId, long[] alFilterIds, Object oKey, Object oValueOld, Object oValueNew, boolean fSynthetic, int nTransformState, boolean fPriming, boolean fExpired)
            {
            // import Component.Util.CacheEvent as com.tangosol.coherence.component.util.CacheEvent;
            // import com.tangosol.net.cache.CacheEvent;
            // import com.tangosol.net.cache.CacheEvent$TransformationState as com.tangosol.net.cache.CacheEvent.TransformationState;
            // import com.tangosol.util.Filter;
            // import com.tangosol.util.Listeners;
            // import com.tangosol.util.LongArray;
            // import com.tangosol.util.MapListenerSupport;
            // import com.tangosol.util.MapListenerSupport$FilterEvent as com.tangosol.util.MapListenerSupport.FilterEvent;
            // import java.util.ArrayList;
            // import java.util.List;
            
            MapListenerSupport  support        = getMapListenerSupport();
            int                 cFilters       = alFilterIds == null ? 0 : alFilterIds.length;
            com.tangosol.net.cache.CacheEvent.TransformationState transformState = com.tangosol.net.cache.CacheEvent.TransformationState.values()[nTransformState];
            CacheEvent          evt            = null;
            
            // collect key-based listeners
            Listeners listeners = transformState == com.tangosol.net.cache.CacheEvent.TransformationState.TRANSFORMED
                    ? null : support.getListeners(oKey);
            if (cFilters > 0)
                {
                LongArray laFilters   = getFilterArray();
                List      listFilters = null;
            
                // collect filter-based listeners
                synchronized (support)
                    {
                    for (int i = 0; i < cFilters; i++)
                        {
                        long lFilterId = alFilterIds[i];
                        if (laFilters.exists(lFilterId))
                            {
                            Filter filter = (Filter) laFilters.get(lFilterId);
                            if (listFilters == null)
                                {
                                listFilters = new ArrayList(cFilters - i);
            
                                // clone the key listeners before merging filter listeners
                                Listeners listenersTemp = new Listeners();
                                listenersTemp.addAll(listeners);
                                listeners = listenersTemp;
                                }
            
                            listFilters.add(filter);
                            listeners.addAll(support.getListeners(filter));
                            }
                        }
                    }
            
                if (listFilters != null)
                    {
                    Filter[] aFilters = new Filter[listFilters.size()];
                    aFilters = (Filter[]) listFilters.toArray(aFilters);
                    
                    evt = new com.tangosol.util.MapListenerSupport.FilterEvent(this, nEventId, oKey, oValueOld, oValueNew,
                            fSynthetic, transformState, fPriming, fExpired, aFilters);
                    }
                }
            
            if (listeners == null || listeners.isEmpty())
                {
                // we cannot safely remove the orphaned listener because of the following
                // race condition: if another thread registers a listener for the same key
                // or filter associated with the event between the time that this thread
                // detected the orphaned listener, but before either sends a message to the
                // server, it is possible for this thread to inadvertently remove the new
                // listener
                //
                // since it is only possible for synchronous listeners to be leaked (due to
                // the extra synchronization in the SafeNamedCache), let's err on the side
                // of leaking a listener than possibly incorrectly removing a listener
                //
                // there is also a valid scenario of a client thread removing an asyn
                // listener while the event is already on the wire; hence no logging makes sense
                }
            else
                {
                if (evt == null)
                    {
                    evt = new CacheEvent(this, nEventId, oKey, oValueOld, oValueNew, fSynthetic, transformState, fPriming, fExpired);
                    }
            
                com.tangosol.coherence.component.util.CacheEvent.dispatchSafe(evt, listeners, getEventDispatcher().getQueue());
                }
            }
        
        /**
         * Return the Channel used by this BinaryCache. If the Channel is null
        * or is not open, this method throws an exception.
        * 
        * @return a Channel that can be used to exchange Messages with the
        * remote ProxyService
         */
        protected com.tangosol.net.messaging.Channel ensureChannel()
            {
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Connection;
            // import com.tangosol.net.messaging.ConnectionException;
            
            Channel channel = getChannel();
            if (channel == null || !channel.isOpen())
                {
                String     sCause     = "released";
                Connection connection = null;
            
                if (channel != null)
                    {
                    connection = channel.getConnection();
                    if (connection == null || !connection.isOpen())
                        {
                        sCause = "closed";
                        }
                    }
            
                throw new ConnectionException("NamedCache \""
                        + getCacheName() + "\" has been " + sCause,
                        connection);
                }
            
            return channel;
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Set entrySet()
            {
            return getEntrySet();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Set entrySet(com.tangosol.util.Filter filter)
            {
            if (filter == null)
                {
                return entrySet();
                }
            
            return query(filter, false);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Set entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
            {
            throw new UnsupportedOperationException();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public Object get(Object oKey)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$GetRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetRequest.TYPE_ID);
            
            request.setKey(oKey);
            
            return channel.request(request);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Map getAll(java.util.Collection colKeys)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$GetAllRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetAllRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            // import com.tangosol.util.ConverterCollections;
            // import com.tangosol.util.NullImplementation;
            // import java.util.Map;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetAllRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetAllRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.GetAllRequest.TYPE_ID);
            
            request.setKeySet(colKeys);
            
            Map response = (Map) channel.request(request);
            
            return response == null
                    ? response
                    : ConverterCollections.getMap(response,
                            NullImplementation.getConverter(),
                            getConverterBinaryToUndecoratedBinary(),
                            NullImplementation.getConverter(),
                            NullImplementation.getConverter());
            }
        
        // From interface: com.tangosol.net.NamedCache
        public String getCacheName()
            {
            return ((RemoteNamedCache) get_Parent()).getCacheName();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public com.tangosol.net.CacheService getCacheService()
            {
            return null;
            }
        
        // Accessor for the property "Channel"
        /**
         * Getter for property Channel.<p>
        * The Channel used to exchange Messages with a remote ProxyService.
         */
        public com.tangosol.net.messaging.Channel getChannel()
            {
            return __m_Channel;
            }
        
        // Accessor for the property "ConverterBinaryToDecoratedBinary"
        /**
         * Getter for property ConverterBinaryToDecoratedBinary.<p>
        * The parent ConverterBinaryToDecoratedBinary instance.
         */
        public RemoteNamedCache.ConverterBinaryToDecoratedBinary getConverterBinaryToDecoratedBinary()
            {
            return __m_ConverterBinaryToDecoratedBinary;
            }
        
        // Accessor for the property "ConverterBinaryToUndecoratedBinary"
        /**
         * Getter for property ConverterBinaryToUndecoratedBinary.<p>
        * The parent ConverterBinaryToUndecoratedBinary instance.
         */
        public RemoteNamedCache.ConverterBinaryToUndecoratedBinary getConverterBinaryToUndecoratedBinary()
            {
            return __m_ConverterBinaryToUndecoratedBinary;
            }
        
        // Accessor for the property "EntrySet"
        /**
         * Getter for property EntrySet.<p>
        * The child EntrySet instance.
         */
        public java.util.Set getEntrySet()
            {
            return __m_EntrySet;
            }
        
        // Accessor for the property "EventDispatcher"
        /**
         * Getter for property EventDispatcher.<p>
        * The QueueProcessor used to dispatch MapEvents.
         */
        public com.tangosol.coherence.component.util.daemon.QueueProcessor getEventDispatcher()
            {
            return __m_EventDispatcher;
            }
        
        // Accessor for the property "FilterArray"
        /**
         * Getter for property FilterArray.<p>
        * A LongArray of Filter objects indexed by the unique filter id. These
        * filter id values are used by the MapEvent message to specify what
        * filters caused a cache event.
        * 
        * Note: all access (for update) to this array should be synchronized on
        * the MapListenerSupport object.
         */
        protected com.tangosol.util.LongArray getFilterArray()
            {
            return __m_FilterArray;
            }
        
        /**
         * Return the unique positivie identifier that the specified Filter was
        * registered with or 0 if the specified Filter has not been registered.
        * 
        * Note: all calls to this method should be synchronized using the
        * MapListenerSupport object.
        * 
        * @param filter  the Filter
        * @return the unique identifier that the specified Filter was
        * registered with
        * @see #registerFilter
         */
        protected long getFilterId(com.tangosol.util.Filter filter)
            {
            // import com.tangosol.util.Base;
            // import com.tangosol.util.Filter;
            // import com.tangosol.util.LongArray$Iterator as com.tangosol.util.LongArray.Iterator;
            
            for (com.tangosol.util.LongArray.Iterator iter = getFilterArray().iterator(); iter.hasNext();)
                {
                Filter filterThat = (Filter) iter.next();
                if (Base.equals(filter, filterThat))
                    {
                    return iter.getIndex();
                    }
                }
            
            return 0L;
            }
        
        // Accessor for the property "KeySet"
        /**
         * Getter for property KeySet.<p>
        * The child KeySet instance.
         */
        public java.util.Set getKeySet()
            {
            return __m_KeySet;
            }
        
        // Accessor for the property "MapListenerSupport"
        /**
         * Getter for property MapListenerSupport.<p>
        * MapListenerSupport used by this BinaryCache to dispatch MapEvents to
        * registered MapListeners.
         */
        public com.tangosol.util.MapListenerSupport getMapListenerSupport()
            {
            return __m_MapListenerSupport;
            }
        
        // Accessor for the property "Values"
        /**
         * Getter for property Values.<p>
        * The child Values instance.
         */
        public java.util.Collection getValues()
            {
            return __m_Values;
            }
        
        // From interface: com.tangosol.net.NamedCache
        public Object invoke(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$InvokeRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeRequest.TYPE_ID);
            
            request.setKey(oKey);
            request.setProcessor(agent);
            
            return channel.request(request);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
            {
            // import Component.Net.Extend.Message.Response.PartialResponse as com.tangosol.coherence.component.net.extend.message.response.PartialResponse;
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$InvokeFilterRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeFilterRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ConverterCollections;
            // import com.tangosol.util.NullImplementation;
            // import java.util.Map;
            
            Channel channel   = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory   = channel.getMessageFactory();
            Binary  binCookie = null;
            Map     mapResult = null;
            
            do
                {
                com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeFilterRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeFilterRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeFilterRequest.TYPE_ID);
            
                request.setCookie(binCookie);
                request.setFilter(filter);
                request.setProcessor(agent);
            
                com.tangosol.net.messaging.Request.Status   status   = channel.send(request);
                com.tangosol.coherence.component.net.extend.message.response.PartialResponse response = (com.tangosol.coherence.component.net.extend.message.response.PartialResponse) status.waitForResponse();
                Map      map      = (Map) processResponse(response);
            
                if (mapResult == null || mapResult.isEmpty())
                    {
                    mapResult = map;
                    }
                else if (map == null || map.isEmpty())
                    {
                    // nothing to do
                    }
                else
                    {
                    mapResult.putAll(map);
                    }
            
                binCookie = response.getCookie();
                }
            while (binCookie != null);
            
            return mapResult == null
                    ? mapResult
                    : ConverterCollections.getMap(mapResult,
                            NullImplementation.getConverter(),
                            getConverterBinaryToUndecoratedBinary(),
                            NullImplementation.getConverter(),
                            NullImplementation.getConverter());
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Map invokeAll(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$InvokeAllRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeAllRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            // import com.tangosol.util.ConverterCollections;
            // import com.tangosol.util.NullImplementation;
            // import java.util.Map;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeAllRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeAllRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.InvokeAllRequest.TYPE_ID);
            
            request.setKeySet(collKeys);
            request.setProcessor(agent);
            
            Map response = (Map) channel.request(request);
            
            return response == null
                    ? response
                    : ConverterCollections.getMap(response,
                            NullImplementation.getConverter(),
                            getConverterBinaryToUndecoratedBinary(),
                            NullImplementation.getConverter(),
                            NullImplementation.getConverter());
            }
        
        // From interface: com.tangosol.net.NamedCache
        public boolean isActive()
            {
            // import com.tangosol.net.messaging.Channel;
            
            Channel channel = getChannel();
            return channel == null ? false : channel.isOpen();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public boolean isDestroyed()
            {
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Connection;
            
            Channel    channel    = getChannel();
            Connection connection = channel == null ? null : channel.getConnection();
            
            if (channel == null || connection == null)
                {
                // unknown if destroyed or not.
                return false;
                }
            else
                {
                // infer destroyed when channel is closed and connection is open.
                return !channel.isOpen() && connection.isOpen();
                }
            }
        
        // From interface: com.tangosol.net.NamedCache
        public boolean isEmpty()
            {
            return size() == 0;
            }

        // From interface: com.tangosol.net.NamedCache
        @Override
        public boolean isReady()
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$SizeRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.SizeRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;

            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();

            if (factory.getVersion() < 11)
                {
                throw new UnsupportedOperationException("NamedMap.isReady is not supported by the current proxy. "
                                                        + "Either upgrade the version of Coherence on the proxy or connect to a proxy "
                                                        + "that supports the isReady operation.");

                }

            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ReadyRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ReadyRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ReadyRequest.TYPE_ID);

            return (boolean) channel.request(request);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Set keySet()
            {
            return getKeySet();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Set keySet(com.tangosol.util.Filter filter)
            {
            // NOTE: The following optimization cannot be made, as the RemoteNamedCache.BinaryCache.KeySet child
            // component (and its Iterator implementation) depends upon this behavior
            /*
            if (filter == null)
                {
                return keySet();
                }
            */
            
            return query(filter, true);
            }
        
        /**
         * Return the next page of keys.
        * 
        * @param binCookie  the optional opaque cookie returned from the last
        * call to this method
        * 
        * @return a PartialResponse containing the next set of keys
         */
        public com.tangosol.coherence.component.net.extend.message.response.PartialResponse keySetPage(com.tangosol.util.Binary binCookie)
            {
            // import Component.Net.Extend.Message.Response.PartialResponse as com.tangosol.coherence.component.net.extend.message.response.PartialResponse;
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$QueryRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.QueryRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.QueryRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.QueryRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.QueryRequest.TYPE_ID);
            
            request.setCookie(binCookie);
            request.setKeysOnly(true);
            
            com.tangosol.net.messaging.Request.Status   status   = channel.send(request);
            com.tangosol.coherence.component.net.extend.message.response.PartialResponse response = (com.tangosol.coherence.component.net.extend.message.response.PartialResponse) status.waitForResponse();
            
            processResponse(response);
            
            return response;
            }
        
        // From interface: com.tangosol.net.NamedCache
        public boolean lock(Object oKey)
            {
            return lock(oKey, 0L);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public boolean lock(Object oKey, long cWait)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$LockRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.LockRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.LockRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.LockRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.LockRequest.TYPE_ID);
            
            request.setKey(oKey);
            request.setTimeoutMillis(cWait);
            
            return ((Boolean) channel.request(request)).booleanValue();
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instantiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            setConverterBinaryToDecoratedBinary((RemoteNamedCache.ConverterBinaryToDecoratedBinary)
                get_Parent()._findChild("ConverterBinaryToDecoratedBinary"));
            setConverterBinaryToUndecoratedBinary((RemoteNamedCache.ConverterBinaryToUndecoratedBinary)
                get_Parent()._findChild("ConverterBinaryToUndecoratedBinary"));
            setEntrySet((RemoteNamedCache.BinaryCache.EntrySet) _findChild("EntrySet"));
            setKeySet((RemoteNamedCache.BinaryCache.KeySet) _findChild("KeySet"));
            setValues((RemoteNamedCache.BinaryCache.Values) _findChild("Values"));
            
            super.onInit();
            }
        
        /**
         * Return the result associated with the given Response.
        * 
        * @param response  the Response to process
        * 
        * @return the result associated with the given Response
        * 
        * @throws RuntimeException if the Response was a failure
         */
        protected Object processResponse(com.tangosol.net.messaging.Response response)
            {
            if (response.isFailure())
                {
                Object oResult = response.getResult();
                if (oResult instanceof Throwable)
                    {
                    throw RemoteNamedCache.ensureRuntimeException((Throwable) oResult);
                    }
                else
                    {
                    throw new RuntimeException("received error: " + oResult);
                    }
                }
            else
                {
                return response.getResult();
                }
            }
        
        // From interface: com.tangosol.net.NamedCache
        public Object put(Object oKey, Object oValue)
            {
            // import com.tangosol.net.cache.CacheMap;
            
            return put(oKey, oValue, CacheMap.EXPIRY_DEFAULT);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public Object put(Object oKey, Object oValue, long cMillis)
            {
            return put(oKey, oValue, cMillis, true);
            }
        
        /**
         * Associate the specified value with the specified key and expiry delay
        * in the remote NamedCache.
        * 
        * @param oKey      the entry key
        * @param oValue   the entry value
        * @param cMillis     the entry expiry delay
        * @param fReturn  if true, the old value will be returned
        * 
        * @return the old value associated with the given key; only applicable
        * if fReturn is true
         */
        public Object put(Object oKey, Object oValue, long cMillis, boolean fReturn)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$PutRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            // import com.tangosol.util.Converter;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutRequest.TYPE_ID);
            
            request.setKey(oKey);
            request.setValue(oValue);
            request.setExpiryDelay(cMillis);
            request.setReturnRequired(fReturn);
            
            return channel.request(request);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public void putAll(java.util.Map mapEntries)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$PutAllRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutAllRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutAllRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutAllRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PutAllRequest.TYPE_ID);
            
            request.setMap(mapEntries);
            channel.request(request);
            }
        
        /**
         * Perform a remote query.
        * 
        * @param filter  the Filter used in the query
        * @param fKeysOnly  if true, only the keys from the result will be
        * returned; otherwise, the entries will be returned
        * 
        * @return the result of the query
         */
        protected java.util.Set query(com.tangosol.util.Filter filter, boolean fKeysOnly)
            {
            // import Component.Net.Extend.Message.Response.PartialResponse as com.tangosol.coherence.component.net.extend.message.response.PartialResponse;
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$PartialResponse as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PartialResponse;
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$QueryRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.QueryRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ConverterCollections;
            // import com.tangosol.util.ImmutableMultiList;
            // import com.tangosol.util.NullImplementation;
            // import com.tangosol.util.filter.LimitFilter;
            // import java.util.ArrayList;
            // import java.util.List;
            // import java.util.Set;
            
            Channel channel   = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory   = channel.getMessageFactory();
            Binary  binCookie = null;
            Set     setResult = null;
            List    listPart  = null;
            
            do
                {
                com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.QueryRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.QueryRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.QueryRequest.TYPE_ID);
            
                request.setCookie(binCookie);
                request.setFilter(filter);
                request.setKeysOnly(fKeysOnly);
            
                if (filter instanceof LimitFilter)
                    {
                    request.setFilterCookie(((LimitFilter) filter).getCookie());
                    }
            
                com.tangosol.net.messaging.Request.Status   status   = channel.send(request);
                com.tangosol.coherence.component.net.extend.message.response.PartialResponse response = (com.tangosol.coherence.component.net.extend.message.response.PartialResponse) status.waitForResponse();
                Set      set      = (Set) processResponse(response);
                
                if (setResult == null || setResult.isEmpty())
                    {
                    // first non-empty result set
                    setResult = set;
                    }
                else if (set == null || set.isEmpty())
                    {
                    // empty result set; nothing to do
                    }
                else
                    {
                    // additional non-empty result set
                    if (listPart == null)
                        {
                        // start recording each result set
                        listPart = new ArrayList();
                        listPart.add(setResult.toArray());
                        }
                    listPart.add(set.toArray());
                    }
            
                if (filter instanceof LimitFilter)
                    {
                    LimitFilter     filterLimit     = (LimitFilter) filter;
                    com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PartialResponse partialResponse = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.PartialResponse) response;
                    LimitFilter     filterReturned  = (LimitFilter) partialResponse.getFilter();
            
                    // update LimitFilter with the state of the returned LimitFilter/Cookie
                    filterLimit.setBottomAnchor(filterReturned.getBottomAnchor());
                    filterLimit.setTopAnchor(filterReturned.getTopAnchor());
                    filterLimit.setCookie(partialResponse.getFilterCookie());
            
                    // note that in this case binCookie will always be null, as we don't
                    // page LimitFilter results; see NamedCacheFactory$QueryRequest.onRun()
                    }
                else
                    {
                    binCookie = response.getCookie();
                    }
                }
            while (binCookie != null);
            
            Set setReturn = listPart == null
                ? setResult
                : new ImmutableMultiList(listPart);
            
            return setReturn == null
                ? setReturn
                : fKeysOnly
                    ? (Set) ConverterCollections.getSet(setReturn,
                        NullImplementation.getConverter(),
                        getConverterBinaryToUndecoratedBinary())
                    : (Set) ConverterCollections.getEntrySet(setReturn,
                        NullImplementation.getConverter(),
                        getConverterBinaryToUndecoratedBinary(),
                        NullImplementation.getConverter(),
                        NullImplementation.getConverter());
            }
        
        /**
         * Create a unqiue positive identifier for the specified Filter.
        * 
        * Note: all calls to this method should be synchronized using the
        * MapListenerSupport object.
        * 
        * @param filter  the Filter
        * @return the unique identifier that the specified Filter was
        * registered with
        * @see #getFilterId
         */
        protected long registerFilter(com.tangosol.util.Filter filter)
            {
            // import com.tangosol.util.LongArray;
            
            LongArray laFilter = getFilterArray();
            if (laFilter.isEmpty())
                {
                laFilter.set(1, filter);
                return 1L;
                }
            else
                {
                return laFilter.add(filter);
                }
            }
        
        // From interface: com.tangosol.net.NamedCache
        public void release()
            {
            }
        
        // From interface: com.tangosol.net.NamedCache
        public Object remove(Object oKey)
            {
            return remove(oKey, true);
            }
        
        /**
         * Remove the entry with the given key from the remote NamedCache.
        * 
        * @param oKey      the key to remove
        * @param fReturn  if true, the removed value will be returned
         */
        public Object remove(Object oKey, boolean fReturn)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$RemoveRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveRequest.TYPE_ID);
            
            request.setKey(oKey);
            request.setReturnRequired(fReturn);
            
            return channel.request(request);
            }
        
        /**
         * Remove the entries with the specified keys from the remote
        * NamedCache.
        * 
        * @param colKeys  the keys to remove
        * 
        * @return true if the NamedCache was modified as a result of this call
         */
        public boolean removeAll(java.util.Collection colKeys)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$RemoveAllRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveAllRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveAllRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveAllRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.RemoveAllRequest.TYPE_ID);
            
            request.setKeySet(colKeys);
            
            return ((Boolean) channel.request(request)).booleanValue();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public void removeIndex(com.tangosol.util.ValueExtractor extractor)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$IndexRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.IndexRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.IndexRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.IndexRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.IndexRequest.TYPE_ID);
            
            request.setExtractor(extractor);
            channel.request(request);
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
            // import com.tangosol.util.ConverterCollections;
            // import com.tangosol.util.MapListenerSupport;
            // import com.tangosol.util.MapListenerSupport$SynchronousListener as com.tangosol.util.MapListenerSupport.SynchronousListener;
            // import com.tangosol.util.MapTriggerListener;
            // import com.tangosol.util.NullImplementation;
            // import com.tangosol.util.filter.InKeySetFilter;
            // import java.util.Set;
            
            MapListenerSupport support = getMapListenerSupport();
            boolean            fSync   = listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener;
            
            if (listener instanceof MapTriggerListener)
                {
                removeRemoteMapListener(filter, 0L, fSync,
                        ((MapTriggerListener) listener).getTrigger(), false/*fPriming*/);
                }
            else if (filter instanceof InKeySetFilter)
                {
                InKeySetFilter filterKeys = (InKeySetFilter) filter;
            
                // strip the int-decoration before passing keys to support
                Set setBinKeys = 
                    ConverterCollections.getSet(filterKeys.getKeys(),
                        getConverterBinaryToUndecoratedBinary(),
                        NullImplementation.getConverter());
            
                // this removes all keys that require no action from the passed set,
                // meaning that there are still existing listeners for them
                // (we can safely do it since the set was already cloned)
                support.removeListenerWithCheck(listener, setBinKeys);
            
                if (!setBinKeys.isEmpty())
                    {
                    removeRemoteMapListener(filterKeys, 1 /*dummy*/, fSync, null,
                        support.isPrimingListener(listener));
                    }
                }
            else
                {
                long lFilterId = 0L;
            
                synchronized (support)
                    {
                    if (support.removeListenerWithCheck(listener, filter))
                        {
                        lFilterId = getFilterId(filter);
                        getFilterArray().remove(lFilterId);
                        }
                    else
                        {
                        return;
                        }
                    }
            
                removeRemoteMapListener(filter, lFilterId, fSync, null, false /*fPriming*/);
                }
            }
        
        // From interface: com.tangosol.net.NamedCache
        public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
            {
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.MapListenerSupport;
            // import com.tangosol.util.MapListenerSupport$SynchronousListener as com.tangosol.util.MapListenerSupport.SynchronousListener;
            // import com.tangosol.util.MapTriggerListener;
            
            if (oKey == null)
                {
                throw new IllegalArgumentException("key cannot be null");
                }
            
            if (listener instanceof MapTriggerListener)
                {
                removeRemoteMapListener(oKey, listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener,
                    ((MapTriggerListener) listener).getTrigger(), false/*fPriming*/);
                }
            else
                {
                // returned keys will not be int decorated
                Binary             binKey   = (Binary) getConverterBinaryToUndecoratedBinary().convert(oKey);
                MapListenerSupport support  = getMapListenerSupport();
                boolean            fEmpty   = support.removeListenerWithCheck(listener, binKey);
                boolean            fPriming = support.isPrimingListener(listener);
                 
                if (fEmpty || fPriming)
                    {
                    removeRemoteMapListener(oKey, listener instanceof com.tangosol.util.MapListenerSupport.SynchronousListener, null, fPriming);
                    }
                }
            }
        
        /**
         * Send a request to the remote NamedCacheProxy to unregister a
        * MapListener.
        * 
        * @param filter       the Filter used to unregister the remote
        * MapListener
        * @param lFilterId   the unique positive identifier for the specified
        * Filter
        * @param fSync    if the local MapListener is a SynchronousListener
        * @param trigger  the optional MapTrigger to associate with the request
        * @param fPriming flag indicating if the call is for a NearCache
        * priming listener
         */
        protected void removeRemoteMapListener(com.tangosol.util.Filter filter, long lFilterId, boolean fSync, com.tangosol.util.MapTrigger trigger, boolean fPriming)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$ListenerFilterRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerFilterRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerFilterRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerFilterRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerFilterRequest.TYPE_ID);
            
            request.setFilter(filter);
            request.setFilterId(lFilterId);
            request.setTrigger(trigger);
            request.setPriming(fPriming);
            
            if (fSync)
                {
                // this is necessary to support the removal of a SynchronousListener from
                // within another SynchronousListener (as is the case in NearCache)
                channel.send(request);
                }
            else
                {
                channel.request(request);
                }
            }
        
        /**
         * Send a request to the remote NamedCacheProxy to unregister a
        * MapListener.
        * 
        * @param oKey   the key used to unregister the remote MapListener
        * @param fSync  if the local MapListener is a SynchronousListener
        * @param trigger  the optional MapTrigger to associate with the request
        * @param fPriming flag indicating if the call is for a NearCache
        * priming listener
         */
        protected void removeRemoteMapListener(Object oKey, boolean fSync, com.tangosol.util.MapTrigger trigger, boolean fPriming)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$ListenerKeyRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerKeyRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerKeyRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerKeyRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ListenerKeyRequest.TYPE_ID);
            
            request.setKey(oKey);
            request.setTrigger(trigger);
            request.setPriming(fPriming);
            
            if (fSync)
                {
                // this is necessary to support the removal of a SynchronousListener from
                // within another SynchronousListener (as is the case in NearCache)
                channel.send(request);
                }
            else
                {
                channel.request(request);
                }
            }
        
        // Accessor for the property "Channel"
        /**
         * Setter for property Channel.<p>
        * The Channel used to exchange Messages with a remote ProxyService.
         */
        public void setChannel(com.tangosol.net.messaging.Channel channel)
            {
            // Channel is immutable
            _assert(getChannel() == null && channel != null);
            
            __m_Channel = (channel);
            
            getConverterBinaryToDecoratedBinary().setSerializer(channel.getSerializer());
            }
        
        // Accessor for the property "ConverterBinaryToDecoratedBinary"
        /**
         * Setter for property ConverterBinaryToDecoratedBinary.<p>
        * The parent ConverterBinaryToDecoratedBinary instance.
         */
        protected void setConverterBinaryToDecoratedBinary(RemoteNamedCache.ConverterBinaryToDecoratedBinary conv)
            {
            __m_ConverterBinaryToDecoratedBinary = conv;
            }
        
        // Accessor for the property "ConverterBinaryToUndecoratedBinary"
        /**
         * Setter for property ConverterBinaryToUndecoratedBinary.<p>
        * The parent ConverterBinaryToUndecoratedBinary instance.
         */
        protected void setConverterBinaryToUndecoratedBinary(RemoteNamedCache.ConverterBinaryToUndecoratedBinary conv)
            {
            __m_ConverterBinaryToUndecoratedBinary = conv;
            }
        
        // Accessor for the property "EntrySet"
        /**
         * Setter for property EntrySet.<p>
        * The child EntrySet instance.
         */
        protected void setEntrySet(java.util.Set set)
            {
            __m_EntrySet = set;
            }
        
        // Accessor for the property "EventDispatcher"
        /**
         * Setter for property EventDispatcher.<p>
        * The QueueProcessor used to dispatch MapEvents.
         */
        public void setEventDispatcher(com.tangosol.coherence.component.util.daemon.QueueProcessor dispatcher)
            {
            __m_EventDispatcher = dispatcher;
            }
        
        // Accessor for the property "FilterArray"
        /**
         * Setter for property FilterArray.<p>
        * A LongArray of Filter objects indexed by the unique filter id. These
        * filter id values are used by the MapEvent message to specify what
        * filters caused a cache event.
        * 
        * Note: all access (for update) to this array should be synchronized on
        * the MapListenerSupport object.
         */
        protected void setFilterArray(com.tangosol.util.LongArray la)
            {
            __m_FilterArray = la;
            }
        
        // Accessor for the property "KeySet"
        /**
         * Setter for property KeySet.<p>
        * The child KeySet instance.
         */
        protected void setKeySet(java.util.Set set)
            {
            __m_KeySet = set;
            }
        
        // Accessor for the property "MapListenerSupport"
        /**
         * Setter for property MapListenerSupport.<p>
        * MapListenerSupport used by this BinaryCache to dispatch MapEvents to
        * registered MapListeners.
         */
        protected void setMapListenerSupport(com.tangosol.util.MapListenerSupport support)
            {
            __m_MapListenerSupport = support;
            }
        
        // Accessor for the property "Values"
        /**
         * Setter for property Values.<p>
        * The child Values instance.
         */
        protected void setValues(java.util.Collection col)
            {
            __m_Values = col;
            }
        
        // From interface: com.tangosol.net.NamedCache
        public int size()
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$SizeRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.SizeRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.SizeRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.SizeRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.SizeRequest.TYPE_ID);
            
            return ((Integer) channel.request(request)).intValue();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public void truncate()
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$ClearRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ClearRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ClearRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ClearRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.ClearRequest.TYPE_ID);
            
            // COH-10216
            if (request.getImplVersion() <= 5)
                {
                throw new UnsupportedOperationException("NamedCache.truncate is not supported by the current proxy. "
                          + "Either upgrade the version of Coherence on the proxy or connect to a proxy "
                          + "that supports the truncate operation.");
                }
            
            request.setTruncate(true);
            
            channel.request(request);
            }
        
        // From interface: com.tangosol.net.NamedCache
        public boolean unlock(Object oKey)
            {
            // import Component.Net.Extend.MessageFactory.NamedCacheFactory$UnlockRequest as com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.UnlockRequest;
            // import com.tangosol.net.messaging.Channel;
            // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
            
            Channel channel = ensureChannel();
            com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
            com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.UnlockRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.UnlockRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.UnlockRequest.TYPE_ID);
            
            request.setKey(oKey);
            
            return ((Boolean) channel.request(request)).booleanValue();
            }
        
        // From interface: com.tangosol.net.NamedCache
        public java.util.Collection values()
            {
            return getValues();
            }

        // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$BinaryCache$EntrySet
        
        /**
         * Virtual Entry Set for the parent BinaryCache.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class EntrySet
                extends    com.tangosol.coherence.component.util.Collections
                implements java.util.Set
            {
            // ---- Fields declarations ----
            
            /**
             * Property BinaryCache
             *
             * The parent BinaryCache instance.
             */
            private RemoteNamedCache.BinaryCache __m_BinaryCache;
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
                __mapChildren.put("Iterator", RemoteNamedCache.BinaryCache.EntrySet.Iterator.get_CLASS());
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
                return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.BinaryCache.EntrySet();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$BinaryCache$EntrySet".replace('/', '.'));
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
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // From interface: java.util.Set
            public boolean add(Object o)
                {
                throw new UnsupportedOperationException();
                }
            
            // From interface: java.util.Set
            public boolean addAll(java.util.Collection col)
                {
                return addAll(this, col);
                }
            
            // From interface: java.util.Set
            public void clear()
                {
                getBinaryCache().clear();
                }
            
            // From interface: java.util.Set
            public boolean contains(Object o)
                {
                // import com.tangosol.util.Base;
                // import java.util.Map$Entry as java.util.Map.Entry;
                
                if (o instanceof java.util.Map.Entry)
                    {
                    RemoteNamedCache.BinaryCache cache = getBinaryCache();
                    java.util.Map.Entry        entry = (java.util.Map.Entry) o;
                    Object       oKey  = entry.getKey();
                
                    // REVIEW: This should really be done using a dedicated BinaryCache method
                    return cache.containsKey(oKey) &&
                           Base.equals(cache.get(oKey), entry.getValue());
                    }
                else
                    {
                    return false;
                    }
                }
            
            // From interface: java.util.Set
            public boolean containsAll(java.util.Collection col)
                {
                return containsAll(this, col);
                }
            
            // From interface: java.util.Set
            // Declared at the super level
            public boolean equals(Object obj)
                {
                // import java.util.Set;
                
                if (obj instanceof Set)
                    {
                    Set setThat = (Set) obj;
                    return size() == setThat.size() && setThat.containsAll(this);
                    }
                
                return false;
                }
            
            // Accessor for the property "BinaryCache"
            /**
             * Getter for property BinaryCache.<p>
            * The parent BinaryCache instance.
             */
            public RemoteNamedCache.BinaryCache getBinaryCache()
                {
                return __m_BinaryCache;
                }
            
            // From interface: java.util.Set
            // Declared at the super level
            public int hashCode()
                {
                // import java.util.Iterator as java.util.Iterator;
                
                int h = 0;
                for (java.util.Iterator iter = iterator(); iter.hasNext(); )
                    {
                    Object o = iter.next();
                    if (o != null)
                        {
                        h += o.hashCode();
                        }
                    }
                return h;
                }
            
            // From interface: java.util.Set
            public boolean isEmpty()
                {
                return getBinaryCache().isEmpty();
                }
            
            // From interface: java.util.Set
            public java.util.Iterator iterator()
                {
                // import java.util.Iterator;
                
                return (Iterator) _newChild("Iterator");
                }
            
            // Declared at the super level
            /**
             * The "component has been initialized" method-notification called
            * out of setConstructed() for the topmost component and that in
            * turn notifies all the children.
            * 
            * This notification gets called before the control returns back to
            * this component instantiator (using <code>new Component.X()</code>
            * or <code>_newInstance(sName)</code>) and on the same thread. In
            * addition, visual components have a "posted" notification
            * <code>onInitUI</code> that is called after (or at the same time
            * as) the control returns back to the instantiator and possibly on
            * a different thread.
             */
            public void onInit()
                {
                setBinaryCache((RemoteNamedCache.BinaryCache) get_Parent());
                
                super.onInit();
                }
            
            // From interface: java.util.Set
            public boolean remove(Object o)
                {
                // import java.util.Collections;
                // import java.util.Map$Entry as java.util.Map.Entry;
                // import java.util.Set;
                
                if (o instanceof java.util.Map.Entry)
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) o;
                    Set   set   = Collections.singleton(entry.getKey());
                    
                    return getBinaryCache().removeAll(set);
                    }
                else
                    {
                    return false;
                    }
                }
            
            // From interface: java.util.Set
            public boolean removeAll(java.util.Collection col)
                {
                // import java.util.Iterator as java.util.Iterator;
                // import java.util.HashSet;
                // import java.util.Map$Entry as java.util.Map.Entry;
                // import java.util.Set;
                
                Set setKeys = new HashSet();
                for (java.util.Iterator iter = col.iterator(); iter.hasNext(); )
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
                    setKeys.add(entry.getKey());
                    }
                
                return getBinaryCache().removeAll(setKeys);
                }
            
            // From interface: java.util.Set
            public boolean retainAll(java.util.Collection col)
                {
                return retainAll(this, col);
                }
            
            // Accessor for the property "BinaryCache"
            /**
             * Setter for property BinaryCache.<p>
            * The parent BinaryCache instance.
             */
            protected void setBinaryCache(RemoteNamedCache.BinaryCache cache)
                {
                __m_BinaryCache = cache;
                }
            
            // From interface: java.util.Set
            public int size()
                {
                return getBinaryCache().size();
                }
            
            // From interface: java.util.Set
            public Object[] toArray()
                {
                return toArray((Object[]) null);
                }
            
            // From interface: java.util.Set
            public Object[] toArray(Object[] ao)
                {
                // import java.util.ConcurrentModificationException;
                
                while (true)
                    {
                    try
                        {
                        return toArray(this, ao);
                        }
                    catch (ConcurrentModificationException e) {}
                    }
                }

            // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$BinaryCache$EntrySet$Iterator
            
            /**
             * Virtual iterator for the parent EntrySet.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.Util
                    implements java.util.Iterator
                {
                // ---- Fields declarations ----
                
                /**
                 * Property BinaryCache
                 *
                 * The BinaryCache that is being iterated.
                 */
                private RemoteNamedCache.BinaryCache __m_BinaryCache;
                
                /**
                 * Property Key
                 *
                 * Last key that was iterated.
                 */
                private Object __m_Key;
                
                /**
                 * Property KeyIterator
                 *
                 * An iterator over the keys returned by BinaryCache.keySet().
                 */
                private transient java.util.Iterator __m_KeyIterator;
                
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
                    return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.BinaryCache.EntrySet.Iterator();
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
                        clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$BinaryCache$EntrySet$Iterator".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                // Accessor for the property "BinaryCache"
                /**
                 * Getter for property BinaryCache.<p>
                * The BinaryCache that is being iterated.
                 */
                public RemoteNamedCache.BinaryCache getBinaryCache()
                    {
                    return __m_BinaryCache;
                    }
                
                // Accessor for the property "Key"
                /**
                 * Getter for property Key.<p>
                * Last key that was iterated.
                 */
                public Object getKey()
                    {
                    return __m_Key;
                    }
                
                // Accessor for the property "KeyIterator"
                /**
                 * Getter for property KeyIterator.<p>
                * An iterator over the keys returned by BinaryCache.keySet().
                 */
                public java.util.Iterator getKeyIterator()
                    {
                    return __m_KeyIterator;
                    }
                
                // From interface: java.util.Iterator
                public boolean hasNext()
                    {
                    return getKeyIterator().hasNext();
                    }
                
                // From interface: java.util.Iterator
                public Object next()
                    {
                    // import com.tangosol.util.SimpleMapEntry;
                    
                    Object oKey = getKeyIterator().next();
                    setKey(oKey);
                    
                    return new SimpleMapEntry(oKey,
                            getBinaryCache().get(getBinaryCache()
                            .getConverterBinaryToDecoratedBinary().convert(oKey)));
                    }
                
                // Declared at the super level
                /**
                 * The "component has been initialized" method-notification
                * called out of setConstructed() for the topmost component and
                * that in turn notifies all the children.
                * 
                * This notification gets called before the control returns back
                * to this component instantiator (using <code>new
                * Component.X()</code> or <code>_newInstance(sName)</code>) and
                * on the same thread. In addition, visual components have a
                * "posted" notification <code>onInitUI</code> that is called
                * after (or at the same time as) the control returns back to
                * the instantiator and possibly on a different thread.
                 */
                public void onInit()
                    {
                    RemoteNamedCache.BinaryCache cache = ((RemoteNamedCache.BinaryCache.EntrySet) get_Parent()).getBinaryCache();
                    
                    setBinaryCache(cache);
                    setKeyIterator(cache.keySet().iterator());
                    
                    super.onInit();
                    }
                
                // From interface: java.util.Iterator
                public void remove()
                    {
                    Object oKey = getKey();
                    if (oKey == null)
                        {
                        throw new IllegalStateException();
                        }
                    
                    try
                        {
                        RemoteNamedCache.BinaryCache cache = getBinaryCache();
                        cache.remove(cache.getConverterBinaryToDecoratedBinary().convert(oKey), false);
                        }
                    finally
                        {
                        setKey(null);
                        }
                    }
                
                // Accessor for the property "BinaryCache"
                /**
                 * Setter for property BinaryCache.<p>
                * The BinaryCache that is being iterated.
                 */
                protected void setBinaryCache(RemoteNamedCache.BinaryCache cache)
                    {
                    __m_BinaryCache = cache;
                    }
                
                // Accessor for the property "Key"
                /**
                 * Setter for property Key.<p>
                * Last key that was iterated.
                 */
                protected void setKey(Object oKey)
                    {
                    __m_Key = oKey;
                    }
                
                // Accessor for the property "KeyIterator"
                /**
                 * Setter for property KeyIterator.<p>
                * An iterator over the keys returned by BinaryCache.keySet().
                 */
                protected void setKeyIterator(java.util.Iterator iterator)
                    {
                    __m_KeyIterator = iterator;
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$BinaryCache$KeySet
        
        /**
         * Virtual key Set for the parent BinaryCache.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class KeySet
                extends    com.tangosol.coherence.component.util.Collections
                implements java.util.Set
            {
            // ---- Fields declarations ----
            
            /**
             * Property BinaryCache
             *
             * The parent BinaryCache instance.
             */
            private RemoteNamedCache.BinaryCache __m_BinaryCache;
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
                __mapChildren.put("Advancer", RemoteNamedCache.BinaryCache.KeySet.Advancer.get_CLASS());
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
                return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.BinaryCache.KeySet();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$BinaryCache$KeySet".replace('/', '.'));
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
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // From interface: java.util.Set
            public boolean add(Object o)
                {
                throw new UnsupportedOperationException();
                }
            
            // From interface: java.util.Set
            public boolean addAll(java.util.Collection col)
                {
                return addAll(this, col);
                }
            
            // From interface: java.util.Set
            public void clear()
                {
                getBinaryCache().clear();
                }
            
            // From interface: java.util.Set
            public boolean contains(Object o)
                {
                return getBinaryCache().containsKey(o);
                }
            
            // From interface: java.util.Set
            public boolean containsAll(java.util.Collection col)
                {
                return getBinaryCache().containsAll(col);
                }
            
            // From interface: java.util.Set
            // Declared at the super level
            public boolean equals(Object obj)
                {
                // import java.util.Set;
                
                if (obj instanceof Set)
                    {
                    Set setThis = getBinaryCache().keySet(null);
                    Set setThat = (Set) obj;
                    return setThat.size() == setThis.size() && setThat.containsAll(setThis);
                    }
                
                return false;
                }
            
            // Accessor for the property "BinaryCache"
            /**
             * Getter for property BinaryCache.<p>
            * The parent BinaryCache instance.
             */
            public RemoteNamedCache.BinaryCache getBinaryCache()
                {
                return __m_BinaryCache;
                }
            
            // From interface: java.util.Set
            // Declared at the super level
            public int hashCode()
                {
                // import java.util.Iterator;
                
                int h = 0;
                for (Iterator iter = iterator(); iter.hasNext(); )
                    {
                    Object o = iter.next();
                    if (o != null)
                        {
                        h += o.hashCode();
                        }
                    }
                return h;
                }
            
            // From interface: java.util.Set
            public boolean isEmpty()
                {
                return getBinaryCache().isEmpty();
                }
            
            // From interface: java.util.Set
            public java.util.Iterator iterator()
                {
                // import com.tangosol.util.PagedIterator;
                // import com.tangosol.util.PagedIterator$Advancer as com.tangosol.util.PagedIterator.Advancer;
                
                return new PagedIterator((com.tangosol.util.PagedIterator.Advancer) _newChild("Advancer"));
                }
            
            // Declared at the super level
            /**
             * The "component has been initialized" method-notification called
            * out of setConstructed() for the topmost component and that in
            * turn notifies all the children.
            * 
            * This notification gets called before the control returns back to
            * this component instantiator (using <code>new Component.X()</code>
            * or <code>_newInstance(sName)</code>) and on the same thread. In
            * addition, visual components have a "posted" notification
            * <code>onInitUI</code> that is called after (or at the same time
            * as) the control returns back to the instantiator and possibly on
            * a different thread.
             */
            public void onInit()
                {
                setBinaryCache((RemoteNamedCache.BinaryCache) get_Parent());
                
                super.onInit();
                }
            
            // From interface: java.util.Set
            public boolean remove(Object o)
                {
                // import java.util.Collections;
                // import java.util.Set;
                
                Set set = Collections.singleton(o);
                return getBinaryCache().removeAll(set);
                }
            
            // From interface: java.util.Set
            public boolean removeAll(java.util.Collection col)
                {
                return getBinaryCache().removeAll(col);
                }
            
            // From interface: java.util.Set
            public boolean retainAll(java.util.Collection col)
                {
                return retainAll(this, col);
                }
            
            // Accessor for the property "BinaryCache"
            /**
             * Setter for property BinaryCache.<p>
            * The parent BinaryCache instance.
             */
            protected void setBinaryCache(RemoteNamedCache.BinaryCache cache)
                {
                __m_BinaryCache = cache;
                }
            
            // From interface: java.util.Set
            public int size()
                {
                return getBinaryCache().size();
                }
            
            // From interface: java.util.Set
            public Object[] toArray()
                {
                return getBinaryCache().keySet(null).toArray();
                }
            
            // From interface: java.util.Set
            public Object[] toArray(Object[] ao)
                {
                return getBinaryCache().keySet(null).toArray(ao);
                }

            // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$BinaryCache$KeySet$Advancer
            
            /**
             * Advancer for the parent KeySet.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Advancer
                    extends    com.tangosol.coherence.component.Util
                    implements com.tangosol.util.PagedIterator.Advancer
                {
                // ---- Fields declarations ----
                
                /**
                 * Property BinaryCache
                 *
                 * The BinaryCache that is being iterated.
                 */
                private RemoteNamedCache.BinaryCache __m_BinaryCache;
                
                /**
                 * Property Cookie
                 *
                 * Opaque cookie used for streaming.
                 */
                private com.tangosol.util.Binary __m_Cookie;
                
                /**
                 * Property Exhausted
                 *
                 * True iff the Advancer has been exhausted.
                 */
                private boolean __m_Exhausted;
                
                // Default constructor
                public Advancer()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Advancer(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.BinaryCache.KeySet.Advancer();
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
                        clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$BinaryCache$KeySet$Advancer".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                // Accessor for the property "BinaryCache"
                /**
                 * Getter for property BinaryCache.<p>
                * The BinaryCache that is being iterated.
                 */
                public RemoteNamedCache.BinaryCache getBinaryCache()
                    {
                    return __m_BinaryCache;
                    }
                
                // Accessor for the property "Cookie"
                /**
                 * Getter for property Cookie.<p>
                * Opaque cookie used for streaming.
                 */
                public com.tangosol.util.Binary getCookie()
                    {
                    return __m_Cookie;
                    }
                
                // Accessor for the property "Exhausted"
                /**
                 * Getter for property Exhausted.<p>
                * True iff the Advancer has been exhausted.
                 */
                public boolean isExhausted()
                    {
                    return __m_Exhausted;
                    }
                
                // From interface: com.tangosol.util.PagedIterator$Advancer
                public java.util.Collection nextPage()
                    {
                    // import Component.Net.Extend.Message.Response.PartialResponse as com.tangosol.coherence.component.net.extend.message.response.PartialResponse;
                    // import com.tangosol.util.Binary;
                    // import java.util.Collection;
                    
                    if (isExhausted())
                        {
                        return null;
                        }
                    
                    com.tangosol.coherence.component.net.extend.message.response.PartialResponse response  = getBinaryCache().keySetPage(getCookie());
                    Binary   binCookie = response.getCookie();
                    
                    setCookie(binCookie);
                    if (binCookie == null)
                        {
                        setExhausted(true);
                        }
                    
                    return (Collection) response.getResult();
                    }
                
                // Declared at the super level
                /**
                 * The "component has been initialized" method-notification
                * called out of setConstructed() for the topmost component and
                * that in turn notifies all the children.
                * 
                * This notification gets called before the control returns back
                * to this component instantiator (using <code>new
                * Component.X()</code> or <code>_newInstance(sName)</code>) and
                * on the same thread. In addition, visual components have a
                * "posted" notification <code>onInitUI</code> that is called
                * after (or at the same time as) the control returns back to
                * the instantiator and possibly on a different thread.
                 */
                public void onInit()
                    {
                    RemoteNamedCache.BinaryCache cache = ((RemoteNamedCache.BinaryCache.KeySet) get_Parent()).getBinaryCache();
                    
                    setBinaryCache(cache);
                    
                    super.onInit();
                    }
                
                // From interface: com.tangosol.util.PagedIterator$Advancer
                public void remove(Object oCurr)
                    {
                    RemoteNamedCache.BinaryCache cache = getBinaryCache();
                    cache.remove(cache.getConverterBinaryToDecoratedBinary().convert(oCurr), false);
                    }
                
                // Accessor for the property "BinaryCache"
                /**
                 * Setter for property BinaryCache.<p>
                * The BinaryCache that is being iterated.
                 */
                protected void setBinaryCache(RemoteNamedCache.BinaryCache cache)
                    {
                    __m_BinaryCache = cache;
                    }
                
                // Accessor for the property "Cookie"
                /**
                 * Setter for property Cookie.<p>
                * Opaque cookie used for streaming.
                 */
                protected void setCookie(com.tangosol.util.Binary bin)
                    {
                    __m_Cookie = bin;
                    }
                
                // Accessor for the property "Exhausted"
                /**
                 * Setter for property Exhausted.<p>
                * True iff the Advancer has been exhausted.
                 */
                protected void setExhausted(boolean f)
                    {
                    __m_Exhausted = f;
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$BinaryCache$Values
        
        /**
         * Virtual values Collection for the parent BinaryCache.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Values
                extends    com.tangosol.coherence.component.util.Collections
                implements java.util.Collection
            {
            // ---- Fields declarations ----
            
            /**
             * Property BinaryCache
             *
             * The parent BinaryCache instance.
             */
            private RemoteNamedCache.BinaryCache __m_BinaryCache;
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
                __mapChildren.put("Iterator", RemoteNamedCache.BinaryCache.Values.Iterator.get_CLASS());
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
                return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.BinaryCache.Values();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$BinaryCache$Values".replace('/', '.'));
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
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // From interface: java.util.Collection
            public boolean add(Object o)
                {
                throw new UnsupportedOperationException();
                }
            
            // From interface: java.util.Collection
            public boolean addAll(java.util.Collection col)
                {
                return addAll(this, col);
                }
            
            // From interface: java.util.Collection
            public void clear()
                {
                getBinaryCache().clear();
                }
            
            // From interface: java.util.Collection
            public boolean contains(Object o)
                {
                return getBinaryCache().containsValue(o);
                }
            
            // From interface: java.util.Collection
            public boolean containsAll(java.util.Collection col)
                {
                return containsAll(this, col);
                }
            
            // From interface: java.util.Collection
            // Declared at the super level
            public boolean equals(Object obj)
                {
                // import java.util.Collection;
                
                if (obj instanceof Collection)
                    {
                    Collection colThat = (Collection) obj;
                    return size() == colThat.size() && colThat.containsAll(this);
                    }
                
                return false;
                }
            
            // Accessor for the property "BinaryCache"
            /**
             * Getter for property BinaryCache.<p>
            * The parent BinaryCache instance.
             */
            public RemoteNamedCache.BinaryCache getBinaryCache()
                {
                return __m_BinaryCache;
                }
            
            // From interface: java.util.Collection
            // Declared at the super level
            public int hashCode()
                {
                // import java.util.Iterator as java.util.Iterator;
                
                int h = 1;
                for (java.util.Iterator iter = iterator(); iter.hasNext(); )
                    {
                    Object o = iter.next();
                    h = 31*h + (o == null ? 0 : o.hashCode());
                    if (o != null)
                        {
                        h += o.hashCode();
                        }
                    }
                return h;
                }
            
            // From interface: java.util.Collection
            public boolean isEmpty()
                {
                return getBinaryCache().isEmpty();
                }
            
            // From interface: java.util.Collection
            public java.util.Iterator iterator()
                {
                // import java.util.Iterator;
                
                return (Iterator) _newChild("Iterator");
                }
            
            // Declared at the super level
            /**
             * The "component has been initialized" method-notification called
            * out of setConstructed() for the topmost component and that in
            * turn notifies all the children.
            * 
            * This notification gets called before the control returns back to
            * this component instantiator (using <code>new Component.X()</code>
            * or <code>_newInstance(sName)</code>) and on the same thread. In
            * addition, visual components have a "posted" notification
            * <code>onInitUI</code> that is called after (or at the same time
            * as) the control returns back to the instantiator and possibly on
            * a different thread.
             */
            public void onInit()
                {
                setBinaryCache((RemoteNamedCache.BinaryCache) get_Parent());
                
                super.onInit();
                }
            
            // From interface: java.util.Collection
            public boolean remove(Object o)
                {
                return remove(this, o);
                }
            
            // From interface: java.util.Collection
            public boolean removeAll(java.util.Collection col)
                {
                return removeAll(this, col);
                }
            
            // From interface: java.util.Collection
            public boolean retainAll(java.util.Collection col)
                {
                return retainAll(this, col);
                }
            
            // Accessor for the property "BinaryCache"
            /**
             * Setter for property BinaryCache.<p>
            * The parent BinaryCache instance.
             */
            protected void setBinaryCache(RemoteNamedCache.BinaryCache cache)
                {
                __m_BinaryCache = cache;
                }
            
            // From interface: java.util.Collection
            public int size()
                {
                return getBinaryCache().size();
                }
            
            // From interface: java.util.Collection
            public Object[] toArray()
                {
                return toArray((Object[]) null);
                }
            
            // From interface: java.util.Collection
            public Object[] toArray(Object[] ao)
                {
                // import java.util.ConcurrentModificationException;
                
                while (true)
                    {
                    try
                        {
                        return toArray(this, ao);
                        }
                    catch (ConcurrentModificationException e) {}
                    }
                }

            // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$BinaryCache$Values$Iterator
            
            /**
             * Virtual iterator for the parent Values.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.Util
                    implements java.util.Iterator
                {
                // ---- Fields declarations ----
                
                /**
                 * Property BinaryCache
                 *
                 * The BinaryCache that is being iterated.
                 */
                private RemoteNamedCache.BinaryCache __m_BinaryCache;
                
                /**
                 * Property Key
                 *
                 * Last key that was iterated.
                 */
                private Object __m_Key;
                
                /**
                 * Property KeyIterator
                 *
                 * An iterator over the keys returned by BinaryCache.keySet().
                 */
                private transient java.util.Iterator __m_KeyIterator;
                
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
                    return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.BinaryCache.Values.Iterator();
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
                        clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$BinaryCache$Values$Iterator".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                // Accessor for the property "BinaryCache"
                /**
                 * Getter for property BinaryCache.<p>
                * The BinaryCache that is being iterated.
                 */
                public RemoteNamedCache.BinaryCache getBinaryCache()
                    {
                    return __m_BinaryCache;
                    }
                
                // Accessor for the property "Key"
                /**
                 * Getter for property Key.<p>
                * Last key that was iterated.
                 */
                public Object getKey()
                    {
                    return __m_Key;
                    }
                
                // Accessor for the property "KeyIterator"
                /**
                 * Getter for property KeyIterator.<p>
                * An iterator over the keys returned by BinaryCache.keySet().
                 */
                public java.util.Iterator getKeyIterator()
                    {
                    return __m_KeyIterator;
                    }
                
                // From interface: java.util.Iterator
                public boolean hasNext()
                    {
                    return getKeyIterator().hasNext();
                    }
                
                // From interface: java.util.Iterator
                public Object next()
                    {
                    Object oKey = getKeyIterator().next();
                    setKey(oKey);
                    
                    return getBinaryCache().get(
                            getBinaryCache().getConverterBinaryToDecoratedBinary().convert(oKey));
                    }
                
                // Declared at the super level
                /**
                 * The "component has been initialized" method-notification
                * called out of setConstructed() for the topmost component and
                * that in turn notifies all the children.
                * 
                * This notification gets called before the control returns back
                * to this component instantiator (using <code>new
                * Component.X()</code> or <code>_newInstance(sName)</code>) and
                * on the same thread. In addition, visual components have a
                * "posted" notification <code>onInitUI</code> that is called
                * after (or at the same time as) the control returns back to
                * the instantiator and possibly on a different thread.
                 */
                public void onInit()
                    {
                    RemoteNamedCache.BinaryCache cache = ((RemoteNamedCache.BinaryCache.Values) get_Parent()).getBinaryCache();
                    
                    setBinaryCache(cache);
                    setKeyIterator(cache.keySet().iterator());
                    
                    super.onInit();
                    }
                
                // From interface: java.util.Iterator
                public void remove()
                    {
                    Object oKey = getKey();
                    if (oKey == null)
                        {
                        throw new IllegalStateException();
                        }
                    
                    try
                        {
                        RemoteNamedCache.BinaryCache cache = getBinaryCache();
                        cache.remove(cache.getConverterBinaryToDecoratedBinary().convert(oKey), false);
                        }
                    finally
                        {
                        setKey(null);
                        }
                    }
                
                // Accessor for the property "BinaryCache"
                /**
                 * Setter for property BinaryCache.<p>
                * The BinaryCache that is being iterated.
                 */
                protected void setBinaryCache(RemoteNamedCache.BinaryCache cache)
                    {
                    __m_BinaryCache = cache;
                    }
                
                // Accessor for the property "Key"
                /**
                 * Setter for property Key.<p>
                * Last key that was iterated.
                 */
                protected void setKey(Object oKey)
                    {
                    __m_Key = oKey;
                    }
                
                // Accessor for the property "KeyIterator"
                /**
                 * Setter for property KeyIterator.<p>
                * An iterator over the keys returned by BinaryCache.keySet().
                 */
                protected void setKeyIterator(java.util.Iterator iterator)
                    {
                    __m_KeyIterator = iterator;
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$ConverterBinaryToDecoratedBinary
    
    /**
     * Converter implementation that deserializes a Binary object using the
     * RemoteNamedCache Channel's Serializer and decorates the Binary using the
     * associated key.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConverterBinaryToDecoratedBinary
            extends    com.tangosol.coherence.component.util.Converter
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConverterBinaryToDecoratedBinary()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConverterBinaryToDecoratedBinary(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.ConverterBinaryToDecoratedBinary();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$ConverterBinaryToDecoratedBinary".replace('/', '.'));
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
            // import com.tangosol.net.cache.KeyAssociation;
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            
            if (o == null || ((RemoteNamedCache) get_Module()).isDeferKeyAssociationCheck())
                {
                return o;
                }
            
            Binary bin = (Binary) o;
            
            if (com.tangosol.util.ExternalizableHelper.isIntDecorated(bin))
                {
                return bin;
                }
            
            Binary binDeco = bin;
            
            o = com.tangosol.util.ExternalizableHelper.fromBinary(bin, getSerializer());
            if (o instanceof KeyAssociation)
                {
                o = ((KeyAssociation) o).getAssociatedKey();
                if (o != null)
                    {
                    binDeco = com.tangosol.util.ExternalizableHelper.toBinary(o, getSerializer());
                    }
                }
            
            return com.tangosol.util.ExternalizableHelper.decorateBinary(bin, binDeco.calculateNaturalPartition(0));
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$ConverterBinaryToUndecoratedBinary
    
    /**
     * Converter implementation that removes an int decoration from a Binary if
     * present.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConverterBinaryToUndecoratedBinary
            extends    com.tangosol.coherence.component.util.Converter
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConverterBinaryToUndecoratedBinary()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConverterBinaryToUndecoratedBinary(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.ConverterBinaryToUndecoratedBinary();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$ConverterBinaryToUndecoratedBinary".replace('/', '.'));
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
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            
            if (o == null || ((RemoteNamedCache) get_Module()).isDeferKeyAssociationCheck())
                {
                return o;
                }
            
            Binary bin = (Binary) o;
            
            return com.tangosol.util.ExternalizableHelper.isIntDecorated(bin) ? com.tangosol.util.ExternalizableHelper.removeIntDecoration(bin) : bin;
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$ConverterFromBinary
    
    /**
     * Converter implementation that converts Objects from a Binary
     * representation via the RemoteNamedCache Channel's Serializer.
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
            return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.ConverterFromBinary();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$ConverterFromBinary".replace('/', '.'));
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
            
            return o == null || ((RemoteNamedCache) get_Module()).isPassThrough()
                    ? (Binary) o
                    : ExternalizableHelper.fromBinary((Binary) o, getSerializer());
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$ConverterKeyToBinary
    
    /**
     * Converter implementation that converts keys into their Binary
     * representation via the RemoteNamedCache Channel's Serializer.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConverterKeyToBinary
            extends    com.tangosol.coherence.component.util.Converter
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConverterKeyToBinary()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConverterKeyToBinary(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.ConverterKeyToBinary();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$ConverterKeyToBinary".replace('/', '.'));
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
            // import com.tangosol.net.cache.KeyAssociation;
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            
            if (((RemoteNamedCache) get_Module()).isPassThrough())
                {
                return o;
                }
            
            Binary bin = com.tangosol.util.ExternalizableHelper.toBinary(o, getSerializer());
            if (((RemoteNamedCache) get_Module()).isDeferKeyAssociationCheck())
                {
                return bin;
                }
            
            Binary binDeco = bin;
            if (o instanceof KeyAssociation)
                {
                o = ((KeyAssociation) o).getAssociatedKey();
                if (o != null)
                    {
                    binDeco = com.tangosol.util.ExternalizableHelper.toBinary(o, getSerializer());
                    }
                }
            
            return com.tangosol.util.ExternalizableHelper.decorateBinary(bin, binDeco.calculateNaturalPartition(0));
            }
        }

    // ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedCache$ConverterValueToBinary
    
    /**
     * Converter implementation that converts values into their Binary
     * representation via the RemoteNamedCache Channel's Serializer.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConverterValueToBinary
            extends    com.tangosol.coherence.component.util.Converter
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConverterValueToBinary()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConverterValueToBinary(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.RemoteNamedCache.ConverterValueToBinary();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteNamedCache$ConverterValueToBinary".replace('/', '.'));
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
            
            return ((RemoteNamedCache) get_Module()).isPassThrough()
                    ? (Binary) o
                    : ExternalizableHelper.toBinary(o, getSerializer());
            }
        }
    }
