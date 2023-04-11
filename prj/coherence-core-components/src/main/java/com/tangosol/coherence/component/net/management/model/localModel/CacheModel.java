
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.CacheModel

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.net.cache.CacheStatistics;
import com.tangosol.net.cache.CachingMap;
import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.NearCache;
import com.tangosol.net.cache.OverflowMap;
import com.tangosol.net.cache.ReadWriteBackingMap;
import com.tangosol.net.cache.SimpleCacheStatistics;
import com.tangosol.net.cache.SimpleMemoryCalculator;
import com.tangosol.net.cache.SimpleOverflowMap;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Model components implement the JMX-managed functionality of the
 * corresponding MBeans without being dependent on any JMX classes and could be
 * used both in-process and out-of-process (relative to an MBeanServer).
 * 
 * The LocalModel components operate in two distinct modes: live and snapshot.
 * In the live mode all model methods call corresponding methods on managed
 * objects. The snapshot mode uses the _SnapshotMap to keep the attribute
 * values.
 * 
 * Every time a remote invocation is used by the RemoteModel to do a
 * setAttribute or invoke call, the snapshot model is refreshed.
 * 
 * Generic cache MBean.
 */
/*
* Integrates
*     com.tangosol.net.cache.CacheStatistics
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class CacheModel
        extends    com.tangosol.coherence.component.net.management.model.LocalModel
    {
    // ---- Fields declarations ----
    
    /**
     * Property _BackingMapRef
     *
     * The underlying ReadWriteBackingMap wrapped in WeakReference to avoid
     * resource leakage.
     */
    private transient java.lang.ref.WeakReference __m__BackingMapRef;
    
    /**
     * Property _ConfigurableCacheRef
     *
     * The underlying LocalCache, wrapped in WeakReference to avoid resource
     * leakage.
     */
    private transient java.lang.ref.WeakReference __m__ConfigurableCacheRef;
    
    /**
     * Property _Map
     *
     * The Map object associated with this model.
     */
    private transient java.util.Map __m__Map;
    
    /**
     * Property _MapRef
     *
     * The Map object associated with this model, wrapped in WeakReference to
     * reduce resource leakage.
     */
    private transient java.lang.ref.WeakReference __m__MapRef;
    
    /**
     * Property _Stats
     *
     * The underlying Map's statistics.
     */
    private transient com.tangosol.net.cache.CacheStatistics __m__Stats;
    
    /**
     * Property BatchFactor
     *
     */
    private double __m_BatchFactor;
    
    /**
     * Property ExpiryDelay
     *
     */
    private int __m_ExpiryDelay;
    
    /**
     * Property HighUnits
     *
     */
    private int __m_HighUnits;
    
    /**
     * Property LowUnits
     *
     */
    private int __m_LowUnits;
    
    /**
     * Property QueueDelay
     *
     */
    private int __m_QueueDelay;
    
    /**
     * Property RefreshFactor
     *
     */
    private double __m_RefreshFactor;
    
    /**
     * Property RequeueThreshold
     *
     */
    private int __m_RequeueThreshold;
    
    // Default constructor
    public CacheModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public CacheModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            set_SnapshotMap(new java.util.HashMap());
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
        return new com.tangosol.coherence.component.net.management.model.localModel.CacheModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/CacheModel".replace('/', '.'));
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
    
    //++ com.tangosol.net.cache.CacheStatistics integration
    // Access optimization
    // properties integration
    // methods integration
    /**
     * Getter for property AverageGetMillis.<p>
     */
    public double getAverageGetMillis()
        {
        return get_Stats().getAverageGetMillis();
        }
    /**
     * Getter for property AverageHitMillis.<p>
     */
    public double getAverageHitMillis()
        {
        return get_Stats().getAverageHitMillis();
        }
    /**
     * Getter for property AverageMissMillis.<p>
     */
    public double getAverageMissMillis()
        {
        return get_Stats().getAverageMissMillis();
        }
    /**
     * Getter for property AveragePutMillis.<p>
     */
    public double getAveragePutMillis()
        {
        return get_Stats().getAveragePutMillis();
        }
    /**
     * Getter for property CacheHits.<p>
     */
    public long getCacheHits()
        {
        return get_Stats().getCacheHits();
        }
    /**
     * Getter for property CacheHitsMillis.<p>
     */
    public long getCacheHitsMillis()
        {
        return get_Stats().getCacheHitsMillis();
        }
    /**
     * Getter for property CacheMisses.<p>
     */
    public long getCacheMisses()
        {
        return get_Stats().getCacheMisses();
        }
    /**
     * Getter for property CacheMissesMillis.<p>
     */
    public long getCacheMissesMillis()
        {
        return get_Stats().getCacheMissesMillis();
        }
    private long getCachePrunes$Router()
        {
        return get_Stats().getCachePrunes();
        }
    /**
     * Getter for property CachePrunes.<p>
     */
    public long getCachePrunes()
        {
        // import com.tangosol.net.cache.CachingMap;
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.cache.LocalCache;
        // import com.tangosol.net.cache.OverflowMap;
        // import com.tangosol.net.cache.SimpleOverflowMap;
        // import java.util.Map;
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        Map                  map   = get_Map();
        return (map instanceof CachingMap
               || map instanceof OverflowMap
               || map instanceof SimpleOverflowMap)
               && cache instanceof LocalCache
           ? ((LocalCache) cache).getCacheStatistics().getCachePrunes()
           : getCachePrunes$Router();
        }
    private long getCachePrunesMillis$Router()
        {
        return get_Stats().getCachePrunesMillis();
        }
    /**
     * Getter for property CachePrunesMillis.<p>
     */
    public long getCachePrunesMillis()
        {
        // import com.tangosol.net.cache.CachingMap;
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.cache.LocalCache;
        // import com.tangosol.net.cache.OverflowMap;
        // import com.tangosol.net.cache.SimpleOverflowMap;
        // import java.util.Map;
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        Map                  map   = get_Map();
        return (map instanceof CachingMap
               || map instanceof OverflowMap
               || map instanceof SimpleOverflowMap)
               && cache instanceof LocalCache
           ? ((LocalCache) cache).getCacheStatistics().getCachePrunesMillis()
           : getCachePrunesMillis$Router();
        }
    /**
     * Getter for property HitProbability.<p>
     */
    public double getHitProbability()
        {
        return get_Stats().getHitProbability();
        }
    /**
     * Getter for property TotalGets.<p>
     */
    public long getTotalGets()
        {
        return get_Stats().getTotalGets();
        }
    /**
     * Getter for property TotalGetsMillis.<p>
     */
    public long getTotalGetsMillis()
        {
        return get_Stats().getTotalGetsMillis();
        }
    /**
     * Getter for property TotalPuts.<p>
     */
    public long getTotalPuts()
        {
        return get_Stats().getTotalPuts();
        }
    /**
     * Getter for property TotalPutsMillis.<p>
     */
    public long getTotalPutsMillis()
        {
        return get_Stats().getTotalPutsMillis();
        }
    private void resetStatistics$Router()
        {
        get_Stats().resetHitStatistics();
        }
    public void resetStatistics()
        {
        resetStatistics$Router();
        
        try
            {
            get_BackingMap().getCacheStore().resetStatistics();
            }
        catch (NullPointerException e) {}
        }
    //-- com.tangosol.net.cache.CacheStatistics integration
    
    // Accessor for the property "_BackingMap"
    /**
     * Getter for property _BackingMap.<p>
    * The underlying ReadWriteBackingMap (could be null).
     */
    protected com.tangosol.net.cache.ReadWriteBackingMap get_BackingMap()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import java.lang.ref.WeakReference;
        
        WeakReference wr = get_BackingMapRef();
        return wr == null ? null : (ReadWriteBackingMap) wr.get();
        }
    
    // Accessor for the property "_BackingMapRef"
    /**
     * Getter for property _BackingMapRef.<p>
    * The underlying ReadWriteBackingMap wrapped in WeakReference to avoid
    * resource leakage.
     */
    protected java.lang.ref.WeakReference get_BackingMapRef()
        {
        return __m__BackingMapRef;
        }
    
    // Accessor for the property "_ConfigurableCache"
    /**
     * Getter for property _ConfigurableCache.<p>
    * The underlying LocalCache (could be null).
     */
    protected com.tangosol.net.cache.ConfigurableCacheMap get_ConfigurableCache()
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        // import java.lang.ref.WeakReference;
        
        WeakReference wr = get_ConfigurableCacheRef();
        return wr == null ? null : (ConfigurableCacheMap) wr.get();
        }
    
    // Accessor for the property "_ConfigurableCacheRef"
    /**
     * Getter for property _ConfigurableCacheRef.<p>
    * The underlying LocalCache, wrapped in WeakReference to avoid resource
    * leakage.
     */
    protected java.lang.ref.WeakReference get_ConfigurableCacheRef()
        {
        return __m__ConfigurableCacheRef;
        }
    
    // Accessor for the property "_Map"
    /**
     * Getter for property _Map.<p>
    * The Map object associated with this model.
     */
    public java.util.Map get_Map()
        {
        // import java.util.Map;
        // import java.lang.ref.WeakReference;
        
        WeakReference wr = get_MapRef();
        return wr == null ? null : (Map) wr.get();
        }
    
    // Accessor for the property "_MapRef"
    /**
     * Getter for property _MapRef.<p>
    * The Map object associated with this model, wrapped in WeakReference to
    * reduce resource leakage.
     */
    protected java.lang.ref.WeakReference get_MapRef()
        {
        return __m__MapRef;
        }
    
    // Accessor for the property "_Stats"
    /**
     * Getter for property _Stats.<p>
    * The underlying Map's statistics.
     */
    public com.tangosol.net.cache.CacheStatistics get_Stats()
        {
        return __m__Stats;
        }
    
    // Accessor for the property "BatchFactor"
    /**
     * Getter for property BatchFactor.<p>
     */
    public double getBatchFactor()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null && map.isWriteBehind())
            {
            return map.getWriteBatchFactor();
            }
        else
            {
            return 0.0;
            }
        }
    
    // Accessor for the property "CacheStoreType"
    /**
     * Getter for property CacheStoreType.<p>
     */
    public String getCacheStoreType()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        
        ReadWriteBackingMap map = get_BackingMap();
        
        String sType = "NONE";
        if (map != null && map.getCacheStore() != null)
            {
            if (map.isReadOnly())
                {
                sType = "READ-ONLY";
                }
            else if (map.isWriteThrough())
                {
                sType = "WRITE-THROUGH";
                }
            else if (map.isWriteBehind())
                {
                sType = "WRITE-BEHIND";
                }
            }
        return sType;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human readable description.
    * 
    * @see Manageable.ModelAdapter#toString()
     */
    public String getDescription()
        {
        // import java.util.Map;
        
        Map map = get_Map();
        return map == null ? canonicalString(null) : "Implementation: " + map.getClass().getName();
        }
    
    // Accessor for the property "ExpiryDelay"
    /**
     * Getter for property ExpiryDelay.<p>
     */
    public int getExpiryDelay()
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        return cache == null ? -1 : cache.getExpiryDelay();
        }
    
    // Accessor for the property "HighUnits"
    /**
     * Getter for property HighUnits.<p>
     */
    public int getHighUnits()
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        return cache == null ? -1 : cache.getHighUnits();
        }
    
    // Accessor for the property "LowUnits"
    /**
     * Getter for property LowUnits.<p>
     */
    public int getLowUnits()
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        return cache == null ? -1 : cache.getLowUnits();
        }
    
    // Accessor for the property "PersistenceType"
    /**
     * Getter for property PersistenceType.<p>
     */
    public String getPersistenceType()
        {
        // deprecated - use CacheStoreType
        return getCacheStoreType();
        }
    
    // Accessor for the property "QueueDelay"
    /**
     * Getter for property QueueDelay.<p>
     */
    public int getQueueDelay()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null && map.isWriteBehind())
            {
            return map.getWriteBehindSeconds();
            }
        else
            {
            return -1;
            }
        }
    
    // Accessor for the property "QueueSize"
    /**
     * Getter for property QueueSize.<p>
     */
    public int getQueueSize()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap$StoreWrapper as com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null)
            {
            return map.getPendingWrites();
            }
        
        return -1;
        }
    
    // Accessor for the property "RefreshFactor"
    /**
     * Getter for property RefreshFactor.<p>
     */
    public double getRefreshFactor()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null && map.isRefreshAhead())
            {
            return map.getRefreshAheadFactor();
            }
        else
            {
            return 0.0;
            }
        }
    
    // Accessor for the property "RequeueThreshold"
    /**
     * Getter for property RequeueThreshold.<p>
     */
    public int getRequeueThreshold()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null && map.isWriteBehind())
            {
            return map.getWriteRequeueThreshold();
            }
        else
            {
            return 0;
            }
        }
    
    // Accessor for the property "Size"
    /**
     * Getter for property Size.<p>
     */
    public int getSize()
        {
        // import com.tangosol.net.cache.NearCache;
        // import java.util.Map;
        
        Map map = get_Map();
        if (map instanceof NearCache)
            {
            map = ((NearCache) map).getFrontMap();
            }
        return map == null ? -1 : map.keySet().size();
        }
    
    // Accessor for the property "StoreAverageBatchSize"
    /**
     * Getter for property StoreAverageBatchSize.<p>
     */
    public long getStoreAverageBatchSize()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap$StoreWrapper as com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null)
            {
            com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper store = map.getCacheStore();
            if (store != null)
                {
                return store.getAverageBatchSize();
                }
            }
        return -1;
        }
    
    // Accessor for the property "StoreAverageReadMillis"
    /**
     * Getter for property StoreAverageReadMillis.<p>
     */
    public long getStoreAverageReadMillis()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap$StoreWrapper as com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null)
            {
            com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper store = map.getCacheStore();
            if (store != null)
                {
                return store.getAverageLoadMillis();
                }
            }
        return -1;
        }
    
    // Accessor for the property "StoreAverageWriteMillis"
    /**
     * Getter for property StoreAverageWriteMillis.<p>
     */
    public long getStoreAverageWriteMillis()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap$StoreWrapper as com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null)
            {
            com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper store = map.getCacheStore();
            if (store != null)
                {
                long cOps    = store.getStoreOps()    + store.getEraseOps();
                long cMillis = store.getStoreMillis() + store.getEraseMillis();
                return cOps > 0L ? cMillis / cOps : 0L;
                }
            }
        return -1;
        }
    
    // Accessor for the property "StoreFailures"
    /**
     * Getter for property StoreFailures.<p>
     */
    public long getStoreFailures()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap$StoreWrapper as com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null)
            {
            com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper store = map.getCacheStore();
            if (store != null)
                {
                return store.getLoadFailures() +
                       store.getStoreFailures() +
                       store.getEraseFailures();
                }
            }
        return -1;
        }
    
    // Accessor for the property "StoreReadMillis"
    /**
     * Getter for property StoreReadMillis.<p>
     */
    public long getStoreReadMillis()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap$StoreWrapper as com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null)
            {
            com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper store = map.getCacheStore();
            if (store != null)
                {
                return store.getLoadMillis();
                }
            }
        return -1;
        }
    
    // Accessor for the property "StoreReads"
    /**
     * Getter for property StoreReads.<p>
     */
    public long getStoreReads()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap$StoreWrapper as com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null)
            {
            com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper store = map.getCacheStore();
            if (store != null)
                {
                return store.getLoadOps();
                }
            }
        return -1;
        }
    
    // Accessor for the property "StoreWriteMillis"
    /**
     * Getter for property StoreWriteMillis.<p>
     */
    public long getStoreWriteMillis()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap$StoreWrapper as com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null && !map.isReadOnly())
            {
            com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper store = map.getCacheStore();
            if (store != null)
                {
                return store.getStoreMillis() +
                       store.getEraseMillis();
                }
            }
        return -1;
        }
    
    // Accessor for the property "StoreWrites"
    /**
     * Getter for property StoreWrites.<p>
     */
    public long getStoreWrites()
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap$StoreWrapper as com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper;
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null && !map.isReadOnly())
            {
            com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper store = map.getCacheStore();
            if (store != null)
                {
                return store.getStoreOps() +
                       store.getEraseOps();
                }
            }
        return -1;
        }
    
    // Accessor for the property "UnitFactor"
    /**
     * Getter for property UnitFactor.<p>
     */
    public int getUnitFactor()
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        return cache == null ? -1 : cache.getUnitFactor();
        }
    
    // Accessor for the property "Units"
    /**
     * Getter for property Units.<p>
     */
    public int getUnits()
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        return cache == null ? -1 : cache.getUnits();
        }
    
    public long getUnitsBytes()
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        return cache == null ? -1 : (long) cache.getUnits() * cache.getUnitFactor();
        }
    
    // Accessor for the property "MemoryUnits"
    /**
     * Getter for property MemoryUnits.<p>
     */
    public boolean isMemoryUnits()
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.cache.ConfigurableCacheMap$UnitCalculator as com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator;
        // import com.tangosol.net.cache.SimpleMemoryCalculator;
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator calc = cache == null ? null : cache.getUnitCalculator();
        return calc instanceof SimpleMemoryCalculator;
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        super.readExternal(in);
        
        Map mapSnapshot = get_SnapshotMap();
        
        mapSnapshot.put("AverageGetMillis", Double.valueOf(in.readDouble()));
        mapSnapshot.put("AverageHitMillis", Double.valueOf(in.readDouble()));
        mapSnapshot.put("AverageMissMillis", Double.valueOf(in.readDouble()));
        mapSnapshot.put("AveragePutMillis", Double.valueOf(in.readDouble()));
        mapSnapshot.put("BatchFactor", Double.valueOf(in.readDouble()));
        mapSnapshot.put("CacheHits", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("CacheHitsMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("CacheMisses", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("CacheMissesMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("CachePrunes", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("CachePrunesMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("CacheStoreType", ExternalizableHelper.readUTF(in));
        mapSnapshot.put("ExpiryDelay", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("HighUnits", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("HitProbability", Double.valueOf(in.readDouble()));
        mapSnapshot.put("LowUnits", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("MemoryUnits", Boolean.valueOf(in.readBoolean()));
        mapSnapshot.put("PersistenceType", ExternalizableHelper.readUTF(in));
        mapSnapshot.put("QueueDelay", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("QueueSize", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("RefreshFactor", Double.valueOf(in.readDouble()));
        mapSnapshot.put("RequeueThreshold", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("Size", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("StoreAverageBatchSize", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("StoreAverageReadMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("StoreAverageWriteMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("StoreFailures", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("StoreReadMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("StoreReads", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("StoreWriteMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("StoreWrites", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalGets", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalGetsMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalPuts", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TotalPutsMillis", Base.makeLong(ExternalizableHelper.readLong(in)));
        
        int nUnitFactor = ExternalizableHelper.readInt(in);
        int cUnits      = ExternalizableHelper.readInt(in);
        
        mapSnapshot.put("UnitFactor", Base.makeInteger(nUnitFactor));
        mapSnapshot.put("Units", Base.makeInteger(cUnits));
        mapSnapshot.put("UnitsBytes", Base.makeLong((long) cUnits * nUnitFactor));
        }
    
    // Accessor for the property "_BackingMapRef"
    /**
     * Setter for property _BackingMapRef.<p>
    * The underlying ReadWriteBackingMap wrapped in WeakReference to avoid
    * resource leakage.
     */
    protected void set_BackingMapRef(java.lang.ref.WeakReference refMap)
        {
        __m__BackingMapRef = refMap;
        }
    
    // Accessor for the property "_ConfigurableCacheRef"
    /**
     * Setter for property _ConfigurableCacheRef.<p>
    * The underlying LocalCache, wrapped in WeakReference to avoid resource
    * leakage.
     */
    protected void set_ConfigurableCacheRef(java.lang.ref.WeakReference cache)
        {
        __m__ConfigurableCacheRef = cache;
        }
    
    // Accessor for the property "_Map"
    /**
     * Setter for property _Map.<p>
    * The Map object associated with this model.
     */
    public void set_Map(java.util.Map map)
        {
        // import com.tangosol.net.cache.CacheStatistics;
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        // import com.tangosol.net.cache.NearCache;
        // import com.tangosol.net.cache.OverflowMap;
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        // import com.tangosol.net.cache.SimpleCacheStatistics;
        // import com.tangosol.util.ClassHelper;
        // import java.lang.ref.WeakReference;
        
        set_MapRef(new WeakReference(map));
        
        if (map instanceof ReadWriteBackingMap)
            {
            set_BackingMapRef(new WeakReference(map));
            map = ((ReadWriteBackingMap) map).getInternalCache();
            }
        
        CacheStatistics stats = null;
        try
            {
            // TODO: make a JCache interface call when impl'd
            stats = (CacheStatistics) ClassHelper.invoke(
                map, "getCacheStatistics", ClassHelper.VOID);
            }
        catch (Throwable e) {}
        
        if (stats == null)
            {
            stats = new SimpleCacheStatistics();
            }
        set_Stats(stats);
        
        if (map instanceof NearCache)
            {
            map = ((NearCache) map).getFrontMap();
            }
        if (map instanceof OverflowMap)
            {
            map = ((OverflowMap) map).getFrontMap();
            }
        
        if (map instanceof ConfigurableCacheMap)
            {
            set_ConfigurableCacheRef(new WeakReference(map));
            }
        }
    
    // Accessor for the property "_MapRef"
    /**
     * Setter for property _MapRef.<p>
    * The Map object associated with this model, wrapped in WeakReference to
    * reduce resource leakage.
     */
    protected void set_MapRef(java.lang.ref.WeakReference refMap)
        {
        __m__MapRef = refMap;
        }
    
    // Accessor for the property "_Stats"
    /**
     * Setter for property _Stats.<p>
    * The underlying Map's statistics.
     */
    protected void set_Stats(com.tangosol.net.cache.CacheStatistics stats)
        {
        __m__Stats = stats;
        }
    
    // Accessor for the property "BatchFactor"
    /**
     * Setter for property BatchFactor.<p>
     */
    public void setBatchFactor(double dFactor)
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        
        checkReadOnly("setBatchFactor");
        checkRange("setBatchFactor", dFactor, 0.0, 1.0);
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null && map.isWriteBehind())
            {
            map.setWriteBatchFactor(dFactor);
            }
        }
    
    // Accessor for the property "ExpiryDelay"
    /**
     * Setter for property ExpiryDelay.<p>
     */
    public void setExpiryDelay(int cMillis)
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        
        checkReadOnly("setExpiryDelay");
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        if (cache != null)
            {
            checkRange("setExpiryDelay", cMillis, 0, Integer.MAX_VALUE);
            cache.setExpiryDelay(cMillis);
            }
        }
    
    // Accessor for the property "HighUnits"
    /**
     * Setter for property HighUnits.<p>
     */
    public void setHighUnits(int cUnits)
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        
        checkReadOnly("setHighUnits");
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        if (cache != null)
            {
            checkRange("setHighUnits", cUnits, 0, Integer.MAX_VALUE);
            cache.setHighUnits(cUnits);
            }
        }
    
    // Accessor for the property "LowUnits"
    /**
     * Setter for property LowUnits.<p>
     */
    public void setLowUnits(int cUnits)
        {
        // import com.tangosol.net.cache.ConfigurableCacheMap;
        
        checkReadOnly("setLowUnits");
        
        ConfigurableCacheMap cache = get_ConfigurableCache();
        if (cache != null)
            {
            checkRange("setLowUnits", cUnits, 0, Integer.MAX_VALUE);
            cache.setLowUnits(cUnits);
            }
        }
    
    // Accessor for the property "QueueDelay"
    /**
     * Setter for property QueueDelay.<p>
     */
    public void setQueueDelay(int cDelay)
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        
        checkReadOnly("setQueueDelay");
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null && map.isWriteBehind())
            {
            map.setWriteBehindSeconds(cDelay);
            }
        }
    
    // Accessor for the property "RefreshFactor"
    /**
     * Setter for property RefreshFactor.<p>
     */
    public void setRefreshFactor(double dFactor)
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        
        checkReadOnly("setRefreshFactor");
        checkRange("setRefreshFactor", dFactor, 0.0, 1.0);
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null && map.isRefreshAhead())
            {
            map.setRefreshAheadFactor(dFactor);
            }
        }
    
    // Accessor for the property "RequeueThreshold"
    /**
     * Setter for property RequeueThreshold.<p>
     */
    public void setRequeueThreshold(int cThreshold)
        {
        // import com.tangosol.net.cache.ReadWriteBackingMap;
        
        checkReadOnly("setRequeueThreshold");
        checkRange("setRequeueThreshold", cThreshold, 0, Integer.MAX_VALUE);
        
        ReadWriteBackingMap map = get_BackingMap();
        if (map != null && map.isWriteBehind())
            {
            map.setWriteRequeueThreshold(cThreshold);
            }
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        
        super.writeExternal(out);
        
        out.writeDouble(getAverageGetMillis());
        out.writeDouble(getAverageHitMillis());
        out.writeDouble(getAverageMissMillis());
        out.writeDouble(getAveragePutMillis());
        out.writeDouble(getBatchFactor());
        ExternalizableHelper.writeLong(out, getCacheHits());
        ExternalizableHelper.writeLong(out, getCacheHitsMillis());
        ExternalizableHelper.writeLong(out, getCacheMisses());
        ExternalizableHelper.writeLong(out, getCacheMissesMillis());
        ExternalizableHelper.writeLong(out, getCachePrunes());
        ExternalizableHelper.writeLong(out, getCachePrunesMillis());
        ExternalizableHelper.writeUTF(out, getCacheStoreType());
        ExternalizableHelper.writeInt(out, getExpiryDelay());
        ExternalizableHelper.writeInt(out, getHighUnits());
        out.writeDouble(getHitProbability());
        ExternalizableHelper.writeInt(out, getLowUnits());
        out.writeBoolean(isMemoryUnits());
        ExternalizableHelper.writeUTF(out, getPersistenceType());
        ExternalizableHelper.writeInt(out, getQueueDelay());
        ExternalizableHelper.writeInt(out, getQueueSize());
        out.writeDouble(getRefreshFactor());
        ExternalizableHelper.writeInt(out, getRequeueThreshold());
        
        try
            {
            ExternalizableHelper.writeInt(out, getSize());
            }
        catch (IllegalStateException e)
            {
            _trace("IllegalStateException serializing the cache model. Size equals zero");
            ExternalizableHelper.writeInt(out, 0);
            }
            
        ExternalizableHelper.writeLong(out, getStoreAverageBatchSize());
        ExternalizableHelper.writeLong(out, getStoreAverageReadMillis());
        ExternalizableHelper.writeLong(out, getStoreAverageWriteMillis());
        ExternalizableHelper.writeLong(out, getStoreFailures());
        ExternalizableHelper.writeLong(out, getStoreReadMillis());
        ExternalizableHelper.writeLong(out, getStoreReads());
        ExternalizableHelper.writeLong(out, getStoreWriteMillis());
        ExternalizableHelper.writeLong(out, getStoreWrites());
        ExternalizableHelper.writeLong(out, getTotalGets());
        ExternalizableHelper.writeLong(out, getTotalGetsMillis());
        ExternalizableHelper.writeLong(out, getTotalPuts());
        ExternalizableHelper.writeLong(out, getTotalPutsMillis());
        ExternalizableHelper.writeInt(out, getUnitFactor());
        ExternalizableHelper.writeInt(out, getUnits());
        }
    }
