
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.DeltaMap

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.collections.wrapperSet.EntrySet;
import com.tangosol.coherence.component.util.collections.wrapperSet.KeySet;
import com.tangosol.net.cache.CacheMap;
import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.SafeHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * DeltaMap is a Map implementation based on another map. The implentation is
 * not thread safe. The model for the DeltaMap includes four maps: InsertMap,
 * UpdateMap, DeleteMap and (optionally, based on the RepeatableRead setting)
 * ReadMap
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class DeltaMap
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.net.cache.CacheMap,
                   com.tangosol.util.ObservableMap,
                   java.util.Map
    {
    // ---- Fields declarations ----
    
    /**
     * Property DeleteMap
     *
     * Map containing the removed items
     */
    private java.util.Map __m_DeleteMap;
    
    /**
     * Property FullyRead
     *
     * Specifies whether or not the map base was fully read (used only if the
     * RepeatableRead is set to true).
     */
    private transient boolean __m_FullyRead;
    
    /**
     * Property InsertMap
     *
     * Map containing the inserted items
     */
    private java.util.Map __m_InsertMap;
    
    /**
     * Property ListenerSupport
     *
     * Registered listeners to the MapEvent notifications.
     */
    private transient com.tangosol.util.MapListenerSupport __m_ListenerSupport;
    
    /**
     * Property NO_VALUE
     *
     * This special value denotes the fact that a key did not exist at the
     * original map during the first "repeatable get" operation.
     */
    public static final Object NO_VALUE;
    
    /**
     * Property OriginalMap
     *
     * Map containing the original data
     */
    private java.util.Map __m_OriginalMap;
    
    /**
     * Property ReadMap
     *
     * Map containing the repeatable reads.
     */
    private java.util.Map __m_ReadMap;
    
    /**
     * Property RepeatableRead
     *
     * Sepcifies whether or not "repeatable read" feature is supported.
     */
    private boolean __m_RepeatableRead;
    
    /**
     * Property UpdateMap
     *
     * Map containing the updates
     */
    private java.util.Map __m_UpdateMap;
    
    // Static initializer
    static
        {
        try
            {
            NO_VALUE = null;
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    // Default constructor
    public DeltaMap()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public DeltaMap(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setRepeatableRead(false);
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
        return new com.tangosol.coherence.component.util.DeltaMap();
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
            clz = Class.forName("com.tangosol.coherence/component/util/DeltaMap".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    public void addMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;
        
        addMapListener(listener, (Filter) null, false);
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    public synchronized void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        _assert(listener != null);
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (support == null)
            {
            setListenerSupport(support = new com.tangosol.util.MapListenerSupport());
            }
        
        support.addListener(listener, filter, fLite);
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    public synchronized void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        _assert(listener != null);
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (support == null)
            {
            setListenerSupport(support = new com.tangosol.util.MapListenerSupport());
            }
        
        support.addListener(listener, oKey, fLite);
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public void clear()
        {
        clearImpl();
        }
    
    protected void clearImpl()
        {
        // import java.util.Iterator;
        // import java.util.Map;
        
        getInsertMap().clear();
        getUpdateMap().clear();
        
        Map mapOrig;
        
        if (isRepeatableRead())
            {
            ensureReadAll();
            mapOrig = getReadMap();
            }
        else
            {
            mapOrig = getOriginalMap();
            }
        
        for (Iterator iter = mapOrig.keySet().iterator(); iter.hasNext();)
            {
            getDeleteMap().put(iter.next(), null);
            }
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public boolean containsKey(Object oKey)
        {
        return containsKeyImpl(oKey);
        }
    
    protected boolean containsKeyImpl(Object oKey)
        {
        boolean fRepeatableRead = isRepeatableRead();
        Object  oValueRead      = fRepeatableRead ? ensureRead(oKey) : null;
        
        if (getInsertMap().containsKey(oKey) || getUpdateMap().containsKey(oKey))
            {
            return true;
            }
        
        if (getDeleteMap().containsKey(oKey))
            {
            return false;
            }
        
        return fRepeatableRead ?
            oValueRead != NO_VALUE : getOriginalMap().containsKey(oKey);
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public boolean containsValue(Object oValue)
        {
        return containsValueImpl(oValue);
        }
    
    protected boolean containsValueImpl(Object oValue)
        {
        // import com.tangosol.util.Base;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        boolean fRepeatableRead = isRepeatableRead();
        if (fRepeatableRead)
            {
            ensureReadAll();
            }
        
        if (getInsertMap().containsValue(oValue) ||
            getUpdateMap().containsValue(oValue))
            {
            return true;
            }
        
        Map mapDelete = getDeleteMap();
        
        if (!fRepeatableRead && mapDelete.isEmpty())
            {
            return getOriginalMap().containsValue(oValue);
            }
        else
            {
            Map mapOrig = fRepeatableRead ? getReadMap() : getOriginalMap();
        
            for (Iterator iter = mapOrig.entrySet().iterator(); iter.hasNext();)
                {
                java.util.Map.Entry  entry = (java.util.Map.Entry) iter.next();
                Object oKey  = entry.getKey();
                Object oVal  = entry.getValue();
        
                if (!mapDelete.containsKey(oKey) && Base.equals(oVal, oValue))
                    {
                    return true;
                    }
                }
        
            return false;
            }
        }
    
    /**
     * Ensure that a resource with the specified key is read. This method is
    * only called if RepeatableRead is set to true.
     */
    protected Object ensureRead(Object oKey)
        {
        // import java.util.Map;
        
        Map mapRead = getReadMap();
        if (isFullyRead() || mapRead.containsKey(oKey))
            {
            return mapRead.get(oKey);
            }
        
        Map    mapOrig = getOriginalMap();
        Object oValue  = mapOrig.containsKey(oKey) ? mapOrig.get(oKey) : NO_VALUE;
        
        mapRead.put(oKey, oValue);
        
        return oValue;
        }
    
    /**
     * Ensure that all resource from original map are read. This method is only
    * called if RepeatableRead is set to true.
     */
    protected void ensureReadAll()
        {
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        Map mapRead = getReadMap();
        
        if (!isFullyRead())
            {
            Map mapOrig = getOriginalMap();
        
            for (Iterator iter = mapOrig.entrySet().iterator(); iter.hasNext();)
                {
                java.util.Map.Entry  entry = (java.util.Map.Entry) iter.next();
                Object oKey  = entry.getKey();
                Object oVal  = entry.getValue();
        
                if (!mapRead.containsKey(oKey))
                    {
                    mapRead.put(oKey, oVal);
                    }
                }
            setFullyRead(true);
            }
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public java.util.Set entrySet()
        {
        return entrySetImpl();
        }
    
    protected java.util.Set entrySetImpl()
        {
        // import Component.Util.Collections.WrapperSet.EntrySet;
        
        if (isRepeatableRead())
            {
            ensureReadAll();
            }
        
        return EntrySet.instantiate(getResolvedMap().entrySet(), this);
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public Object get(Object oKey)
        {
        return getImpl(oKey);
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    public java.util.Map getAll(java.util.Collection colKeys)
        {
        return getAllImpl(colKeys);
        }
    
    protected java.util.Map getAllImpl(java.util.Collection colKeys)
        {
        // import com.tangosol.net.cache.CacheMap;
        // import java.util.Collection;
        // import java.util.HashSet;
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        
        boolean    fRepeatableRead = isRepeatableRead();
        Map        mapRead         = getReadMap();
        Map        mapResult       = new HashMap(colKeys.size());
        Collection colMiss         = colKeys;
        
        // first collect all the entries that we have read previousely
        if (fRepeatableRead)
            {
            colMiss = new HashSet();
            for (Iterator iter = colKeys.iterator(); iter.hasNext();)
                {
                Object oKey = iter.next();
                if (mapRead.containsKey(oKey))
                    {
                    mapResult.put(oKey, getImpl(oKey));
                    }
                else
                    {
                    colMiss.add(oKey);
                    }
                }
            }
        
        // now get the rest
        if (!colMiss.isEmpty())
            {
            Map mapOrig = getOriginalMap();
            if (mapOrig instanceof CacheMap)
                {
                Map mapExist = ((CacheMap) mapOrig).getAll(colMiss);
                for (Iterator iter = colMiss.iterator(); iter.hasNext();)
                    {
                    Object oKey = iter.next();
                    if (fRepeatableRead)
                        {
                        // we can get here only if the value is read the very
                        // first time -- update the ReadMap
                        if (mapExist.containsKey(oKey))
                            {
                            Object oValue = mapExist.get(oKey);
                            mapResult.put(oKey, oValue);
                            mapRead.put(oKey, oValue);
                            }
                        else
                            {
                            mapRead.put(oKey, NO_VALUE);
                            }
                        }
                    else
                        {
                        if (mapExist.containsKey(oKey))
                            {
                            mapResult.put(oKey, mapExist.get(oKey));
                            }
                        }
                    }
                
                }
            else
                {
                for (Iterator iter = colMiss.iterator(); iter.hasNext();)
                    {
                    Object oKey = iter.next();
                    if (fRepeatableRead)
                        {
                        Object oValue = ensureRead(oKey);
                        if (oValue != NO_VALUE)
                            {
                            mapResult.put(oKey, oValue);
                            }
                        }
                    else
                        {
                        Object oValue = mapOrig.get(oKey);
                        if (oValue != null || mapOrig.containsKey(oKey))
                            {
                            mapResult.put(oKey, oValue);
                            }
                        }
                    }
                }
            }
        return mapResult;
        }
    
    // Accessor for the property "DeleteKeySet"
    /**
     * Getter for property DeleteKeySet.<p>
    * A calculated helper property returning a cloned set of removed resources
    * keys.
     */
    public java.util.Set getDeleteKeySet()
        {
        // import java.util.HashSet;
        
        return new HashSet(getDeleteMap().keySet());
        }
    
    // Accessor for the property "DeleteMap"
    /**
     * Getter for property DeleteMap.<p>
    * Map containing the removed items
     */
    protected java.util.Map getDeleteMap()
        {
        return __m_DeleteMap;
        }
    
    protected Object getImpl(Object oKey)
        {
        // import java.util.Map;
        
        boolean fRepeatableRead = isRepeatableRead();
        Object  oValueRead      = fRepeatableRead ? ensureRead(oKey) : null;
        
        Map mapUpdate = getUpdateMap();
        if (mapUpdate.containsKey(oKey))
            {
            return mapUpdate.get(oKey);
            }
        
        Map mapInsert = getInsertMap();
        if (mapInsert.containsKey(oKey))
            {
            return mapInsert.get(oKey);
            }
        
        Map mapDelete = getDeleteMap();
        if (mapDelete.containsKey(oKey))
            {
            return null;
            }
        
        if (fRepeatableRead)
            {
            return oValueRead == NO_VALUE ? null : oValueRead;
            }
        
        return getOriginalMap().get(oKey);
        }
    
    // Accessor for the property "InsertKeySet"
    /**
     * Getter for property InsertKeySet.<p>
    * A calculated helper property returning a cloned set of inserted resources
    * keys.
    * 
     */
    public java.util.Set getInsertKeySet()
        {
        // import java.util.HashSet;
        
        return new HashSet(getInsertMap().keySet());
        }
    
    // Accessor for the property "InsertMap"
    /**
     * Getter for property InsertMap.<p>
    * Map containing the inserted items
     */
    protected java.util.Map getInsertMap()
        {
        return __m_InsertMap;
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
    
    // Accessor for the property "OriginalMap"
    /**
     * Getter for property OriginalMap.<p>
    * Map containing the original data
     */
    public java.util.Map getOriginalMap()
        {
        return __m_OriginalMap;
        }
    
    // Accessor for the property "ReadKeySet"
    /**
     * Getter for property ReadKeySet.<p>
    * A calculated helper property returning a cloned set of read resources
    * keys (not including the modified resources).
     */
    public java.util.Set getReadKeySet()
        {
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        HashSet setRead = new HashSet();
        Map     mapRead = getReadMap();
        
        // get rid of the NO_VALUE reads
        // and the modified resources
        for (Iterator iter = mapRead.entrySet().iterator(); iter.hasNext();)
            {
            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
        
            Object oKey = entry.getKey();
        
            if (entry.getValue() != NO_VALUE && !isModified(oKey))
                {
                setRead.add(oKey);
                }
            }
        
        return setRead;
        }
    
    // Accessor for the property "ReadMap"
    /**
     * Getter for property ReadMap.<p>
    * Map containing the repeatable reads.
     */
    protected java.util.Map getReadMap()
        {
        return __m_ReadMap;
        }
    
    // Accessor for the property "ResolvedMap"
    /**
     * Getter for property ResolvedMap.<p>
    * Calculatable property returning a new map representing a resolved view of
    * the data. 
    * 
    * Note that while the content of the ResolvedMap takes into account the
    * values in the ReadMap, the resolve method doesn't use ReadMap content.
     */
    public java.util.Map getResolvedMap()
        {
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        HashMap map = new HashMap();
        
        if (isRepeatableRead())
            {
            if (!isFullyRead())
                {
                map.putAll(getOriginalMap());
                }
        
            // override all reads
            Map mapRead = getReadMap();
            for (Iterator iter = mapRead.entrySet().iterator(); iter.hasNext();)
                {
                java.util.Map.Entry  entry = (java.util.Map.Entry) iter.next();
                Object oKey  = entry.getKey();
                Object oVal  = entry.getValue();
        
                if (oVal == NO_VALUE)
                    {
                    map.remove(oKey);
                    }
                else
                    {
                    map.put(oKey, oVal);
                    }
                }
            }
        else
            {
            map.putAll(getOriginalMap());
            }
        
        resolve(map);
        
        return map;
        }
    
    // Accessor for the property "UpdateKeySet"
    /**
     * Getter for property UpdateKeySet.<p>
    * A calculated helper property returning a cloned set of updated resources
    * keys.
     */
    public java.util.Set getUpdateKeySet()
        {
        // import java.util.HashSet;
        
        return new HashSet(getUpdateMap().keySet());
        }
    
    // Accessor for the property "UpdateMap"
    /**
     * Getter for property UpdateMap.<p>
    * Map containing the updates
     */
    protected java.util.Map getUpdateMap()
        {
        return __m_UpdateMap;
        }
    
    public void initialize(java.util.Map mapOrig, java.util.Map mapInsert, java.util.Map mapUpdate, java.util.Map mapDelete, java.util.Map mapRead)
        {
        // import com.tangosol.util.SafeHashMap;
        // import java.util.Collections;
        
        _assert(mapOrig != null && getOriginalMap() == null);
        
        setOriginalMap(mapOrig);
        setInsertMap(mapInsert == null ? new SafeHashMap() : mapInsert);
        setUpdateMap(mapUpdate == null ? new SafeHashMap() : mapUpdate);
        setDeleteMap(mapDelete == null ? new SafeHashMap() : mapDelete);
        setReadMap  (mapRead   == null ? (isRepeatableRead() ?
            new SafeHashMap() : Collections.EMPTY_MAP)     : mapRead);
        }
    
    public static DeltaMap instantiate(java.util.Map mapOrig)
        {
        // import com.tangosol.util.SafeHashMap;
        
        DeltaMap map = new DeltaMap();
        
        map.initialize(mapOrig, null, null, null, null);
        
        return map;
        }
    
    /**
     * Factory pattern
     */
    protected com.tangosol.util.MapEvent instantiateMapEvent(int nEventId, Object oKey, Object oValueOld, Object oValue)
        {
        // import com.tangosol.util.MapEvent;
        
        return new MapEvent(this, nEventId, oKey, oValueOld, oValue);
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public boolean isEmpty()
        {
        return size() == 0;
        }
    
    // Accessor for the property "FullyRead"
    /**
     * Getter for property FullyRead.<p>
    * Specifies whether or not the map base was fully read (used only if the
    * RepeatableRead is set to true).
     */
    protected boolean isFullyRead()
        {
        return __m_FullyRead;
        }
    
    /**
     * Specifies whether or not the specified resource has been modified by this
    * DeltaMap.
    * 
    * @param oKey the resource key
    * 
    * @return true if the specified resource has been inserted, updated or
    * removed; false otherwise
     */
    public boolean isModified(Object oKey)
        {
        return getInsertMap().containsKey(oKey) ||
               getUpdateMap().containsKey(oKey) ||
               getDeleteMap().containsKey(oKey);
        }
    
    /**
     * Specifies whether or not the specified resource has been read by this
    * DeltaMap. The return value could be different from isModified(oKey) only
    * in case of RepeatableRead set to true.
    * 
    * @param oKey the resource key
    * 
    * @return true if the specified resource has been inserted, updated,
    * removed or read; false otherwise
     */
    public boolean isRead(Object oKey)
        {
        return isModified(oKey) || getReadMap().containsKey(oKey);
        }
    
    // Accessor for the property "RepeatableRead"
    /**
     * Getter for property RepeatableRead.<p>
    * Sepcifies whether or not "repeatable read" feature is supported.
     */
    protected boolean isRepeatableRead()
        {
        return __m_RepeatableRead;
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public java.util.Set keySet()
        {
        return keySetImpl();
        }
    
    protected java.util.Set keySetImpl()
        {
        // import Component.Util.Collections.WrapperSet.KeySet;
        
        if (isRepeatableRead())
            {
            ensureReadAll();
            }
        
        return KeySet.instantiate(getResolvedMap().keySet(), this);
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public Object put(Object oKey, Object oValue)
        {
        return putImpl(oKey, oValue);
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        if (cMillis == 0)
            {
            return put(oKey, oValue);
            }
        else
            {
            throw new UnsupportedOperationException();
            }
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public void putAll(java.util.Map map)
        {
        // import Component.Util.Collections as com.tangosol.coherence.component.util.Collections;
        
        com.tangosol.coherence.component.util.Collections.putAll(this, map);
        }
    
    protected Object putImpl(Object oKey, Object oValue)
        {
        // import com.tangosol.util.MapEvent;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import java.util.Map;
        
        boolean fRepeatableRead = isRepeatableRead();
        Object  oValueRead      = fRepeatableRead ? ensureRead(oKey) : null;
        
        Map mapInsert = getInsertMap();
        Map mapUpdate = getUpdateMap();
        Map mapDelete = getDeleteMap();
        
        mapDelete.remove(oKey);
        
        Object  oValueOld = null;
        boolean fInsert   = false;
        
        if (mapUpdate.containsKey(oKey))
            {
            oValueOld = mapUpdate.put(oKey, oValue);
            }
        else if (mapInsert.containsKey(oKey))
            {
            oValueOld = mapInsert.get(oKey);
            mapInsert.put(oKey, oValue);
            }
        else
            {
            if (fRepeatableRead)
                {
                if (oValueRead == NO_VALUE)
                    {
                    fInsert = true;
                    }
                else
                    {
                    oValueOld = oValueRead;
                    }
                }
            else
                {
                Map mapOrig = getOriginalMap();
                if (mapOrig.containsKey(oKey))
                    {
                    oValueOld = mapOrig.get(oKey);
                    }
                else
                    {
                    fInsert = true;
                    }
                }
        
            if (fInsert)
                {
                mapInsert.put(oKey, oValue);
                }
            else
                {
                mapUpdate.put(oKey, oValue);
                }
            }
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (support != null)
            {
            MapEvent event = instantiateMapEvent(
                fInsert ? MapEvent.ENTRY_INSERTED : MapEvent.ENTRY_UPDATED,
                oKey, oValueOld, oValue);
        
            support.fireEvent(event, true);
            }
        return oValueOld;
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public Object remove(Object oKey)
        {
        return removeImpl(oKey);
        }
    
    protected Object removeImpl(Object oKey)
        {
        // import com.tangosol.util.MapEvent;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import java.util.Map;
        
        boolean fRepeatableRead = isRepeatableRead();
        Object  oValueRead      = fRepeatableRead ? ensureRead(oKey) : null;
        
        Map mapInsert = getInsertMap();
        Map mapUpdate = getUpdateMap();
        Map mapDelete = getDeleteMap();
        
        Object oValue = null;
        
        if (!mapDelete.containsKey(oKey))
            {
            if (mapUpdate.containsKey(oKey))
                {
                oValue = mapUpdate.remove(oKey);
                }
            else if (mapInsert.containsKey(oKey))
                {
                oValue = mapInsert.remove(oKey);
                }
            else if (fRepeatableRead)
                {
                oValue = oValueRead == NO_VALUE ? null : oValueRead;
                }
            else
                {
                oValue = getOriginalMap().get(oKey);
                }
        
            mapDelete.put(oKey, null);
        
            com.tangosol.util.MapListenerSupport support = getListenerSupport();
            if (support != null)
                {
                MapEvent event = instantiateMapEvent(
                    MapEvent.ENTRY_DELETED, oKey, oValue, null);
        
                support.fireEvent(event, true);
                }
            }
        
        return oValue;
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    public void removeMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;
        
        removeMapListener(listener, (Filter) null);
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    public synchronized void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        _assert(listener != null);
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (support != null)
            {
            support.removeListener(listener, filter);
            if (support.isEmpty())
                {
                setListenerSupport(null);
                }
            }
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    public synchronized void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        _assert(listener != null);
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (support != null)
            {
            support.removeListener(listener, oKey);
            if (support.isEmpty())
                {
                setListenerSupport(null);
                }
            }
        }
    
    /**
     * Discard the changes to the map ("rollback").
     */
    public void reset()
        {
        getInsertMap().clear();
        getUpdateMap().clear();
        getDeleteMap().clear();
        getReadMap()  .clear();
        }
    
    /**
     * Apply the changes to the base map ("commit").
     */
    public void resolve()
        {
        resolve(getOriginalMap());
        reset();
        }
    
    /**
     * Apply the changes to the specified map.
     */
    protected void resolve(java.util.Map mapOrig)
        {
        // import java.util.Iterator;
        // import java.util.Map;
        
        // add inserts
            {
            Map map = getInsertMap();
            if (!map.isEmpty())
                {
                mapOrig.putAll(map);
                }
            }
        
        // add updates
            {
            Map map = getUpdateMap();
            if (!map.isEmpty())
                {
                mapOrig.putAll(map);
                }
            }
        
        // remove deletes
            {
            Map map = getDeleteMap();
            if (!map.isEmpty())
                {
                try
                    {
                    mapOrig.keySet().removeAll(map.keySet());
                    }
                catch (UnsupportedOperationException e)
                    {
                    for (Iterator iter = map.keySet().iterator(); iter.hasNext();)
                        {
                        mapOrig.remove(iter.next());
                        }
                    }
                }
            }
        }
    
    // Accessor for the property "DeleteMap"
    /**
     * Setter for property DeleteMap.<p>
    * Map containing the removed items
     */
    protected void setDeleteMap(java.util.Map map)
        {
        __m_DeleteMap = map;
        }
    
    // Accessor for the property "FullyRead"
    /**
     * Setter for property FullyRead.<p>
    * Specifies whether or not the map base was fully read (used only if the
    * RepeatableRead is set to true).
     */
    protected void setFullyRead(boolean fReadFully)
        {
        __m_FullyRead = fReadFully;
        }
    
    // Accessor for the property "InsertMap"
    /**
     * Setter for property InsertMap.<p>
    * Map containing the inserted items
     */
    protected void setInsertMap(java.util.Map map)
        {
        __m_InsertMap = map;
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
    
    // Accessor for the property "OriginalMap"
    /**
     * Setter for property OriginalMap.<p>
    * Map containing the original data
     */
    protected void setOriginalMap(java.util.Map map)
        {
        __m_OriginalMap = map;
        }
    
    // Accessor for the property "ReadMap"
    /**
     * Setter for property ReadMap.<p>
    * Map containing the repeatable reads.
     */
    protected void setReadMap(java.util.Map pReadMap)
        {
        __m_ReadMap = pReadMap;
        }
    
    // Accessor for the property "RepeatableRead"
    /**
     * Setter for property RepeatableRead.<p>
    * Sepcifies whether or not "repeatable read" feature is supported.
     */
    protected void setRepeatableRead(boolean fRepeatableRead)
        {
        __m_RepeatableRead = fRepeatableRead;
        }
    
    // Accessor for the property "UpdateMap"
    /**
     * Setter for property UpdateMap.<p>
    * Map containing the updates
     */
    protected void setUpdateMap(java.util.Map map)
        {
        __m_UpdateMap = map;
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public int size()
        {
        return sizeImpl();
        }
    
    protected int sizeImpl()
        {
        if (isRepeatableRead())
            {
            ensureReadAll();
            }
        return getResolvedMap().size();
        }
    
    // Declared at the super level
    public String toString()
        {
        return getClass().getName()
             + "\nOriginal=" + getOriginalMap()
             + "\nRead="     + getReadMap()
             + "\nInsert="   + getInsertMap()
             + "\nUpdate="   + getUpdateMap()
             + "\nDelete="   + getDeleteMap();
        }
    
    // From interface: com.tangosol.net.cache.CacheMap
    // From interface: com.tangosol.util.ObservableMap
    // From interface: java.util.Map
    public java.util.Collection values()
        {
        return valuesImpl();
        }
    
    protected java.util.Collection valuesImpl()
        {
        // import java.util.Collections;
        
        if (isRepeatableRead())
            {
            ensureReadAll();
            }
        
        return Collections.unmodifiableCollection(getResolvedMap().values());
        }
    }
