
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.WrapperContext

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage;

import com.tangosol.net.BackingMapContext;
import com.tangosol.util.Binary;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;
import java.util.Map;

/**
 * The WrapperContext is a BackingMapManagerContext implementation that routes
 * all requests to the underlying Context and maintaining a map of allocated
 * $StorageContext (BackingMapContext) components.
 * 
 * This component is tightly bound to the PartitionedCache component structure.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class WrapperContext
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.net.BackingMapManagerContext
    {
    // ---- Fields declarations ----
    
    /**
     * Property PrePinnedPartitions
     *
     * The set of partitions externally pinned for the request.
     * It is the responsibility of the caller to unpin the partition set.
     */
    private com.tangosol.net.partition.PartitionSet __m_PrePinnedPartitions;
    
    /**
     * Property StorageMap
     *
     * Map<String, BackingMapContext> containing all named BackingMapContexts
     * known to this InvocationContext.  This map is kept (separately from the
     * service's StorageArray) to ensure that a consistent view is presented
     * throughout the invocation.
     */
    private java.util.Map __m_StorageMap;
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
        __mapChildren.put("StorageContext", WrapperContext.StorageContext.get_CLASS());
        }
    
    // Initializing constructor
    public WrapperContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
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
            clz = Class.forName("com.tangosol.coherence/component/util/WrapperContext".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public Object addInternalValueDecoration(Object oValue, int nDecorId, Object oDecor)
        {
        return getContext().addInternalValueDecoration(oValue, nDecorId, oDecor);
        }
    
    /**
     * Shortcut version of getBackingMapContext by name, for callers which
    * already hold the storage.
     */
    public com.tangosol.net.BackingMapContext ensureBackingMapContext(Storage storage)
        {
        // import com.tangosol.net.BackingMapContext as com.tangosol.net.BackingMapContext;
        // import java.util.Map;
        
        Map       mapStorage = getStorageMap();
        String    sCacheName = storage.getCacheName();
        com.tangosol.net.BackingMapContext ctxWrapper = (com.tangosol.net.BackingMapContext) mapStorage.get(sCacheName);
        
        if (ctxWrapper == null)
            {
            // we don't have any concerns about thread safety since the
            // wrappered context operates on a single thread
        
            WrapperContext.StorageContext ctx = instantiateStorageContext(storage);
            mapStorage.put(sCacheName, ctxWrapper = ctx);
            }
        
        return ctxWrapper;
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public java.util.Map getBackingMap(String sCacheName)
        {
        // import com.tangosol.net.BackingMapContext;
        
        BackingMapContext ctx = getBackingMapContext(sCacheName);
        
        return ctx == null ? null : ctx.getBackingMap();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.net.BackingMapContext getBackingMapContext(String sCacheName)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as Storage;
        // import com.tangosol.net.BackingMapContext as com.tangosol.net.BackingMapContext;
        // import java.util.Map;
        
        Map       mapStorage = getStorageMap();
        com.tangosol.net.BackingMapContext ctxWrapper = (com.tangosol.net.BackingMapContext) mapStorage.get(sCacheName);
        
        if (ctxWrapper == null)
            {
            // we don't have any concerns about thread safety since the
            // wrappered context operates on a single thread
        
            Storage storage = getStorage(sCacheName);
            if (storage != null)
                {
                WrapperContext.StorageContext ctx = instantiateStorageContext(storage);
                mapStorage.put(sCacheName, ctxWrapper = ctx);
                }
            }
        
        return ctxWrapper;
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.net.CacheService getCacheService()
        {
        return getContext().getCacheService();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public ClassLoader getClassLoader()
        {
        return getContext().getClassLoader();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.run.xml.XmlElement getConfig()
        {
        return getContext().getConfig();
        }
    
    // Accessor for the property "Context"
    /**
     * Getter for property Context.<p>
     */
    public com.tangosol.net.BackingMapManagerContext getContext()
        {
        return getService().getBackingMapContext();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public Object getInternalValueDecoration(Object oValue, int nDecorId)
        {
        return getContext().getInternalValueDecoration(oValue, nDecorId);
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.util.Converter getKeyFromInternalConverter()
        {
        return getContext().getKeyFromInternalConverter();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public int getKeyPartition(Object oKey)
        {
        return getContext().getKeyPartition(oKey);
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.util.Converter getKeyToInternalConverter()
        {
        return getContext().getKeyToInternalConverter();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.net.BackingMapManager getManager()
        {
        return getContext().getManager();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public java.util.Set getPartitionKeys(String sCacheName, int nPartition)
        {
        return getContext().getPartitionKeys(sCacheName, nPartition);
        }
    
    // Accessor for the property "PrePinnedPartitions"
    /**
     * Getter for property PrePinnedPartitions.<p>
    * The set of partitions externally pinned for the request.
    * It is the responsibility of the caller to unpin the partition set.
     */
    public com.tangosol.net.partition.PartitionSet getPrePinnedPartitions()
        {
        return __m_PrePinnedPartitions;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache getService()
        {
        return null;
        }
    
    public Storage getStorage(String sCacheName)
        {
        return null;
        }
    
    // Accessor for the property "StorageMap"
    /**
     * Getter for property StorageMap.<p>
    * Map<String, BackingMapContext> containing all named BackingMapContexts
    * known to this InvocationContext.  This map is kept (separately from the
    * service's StorageArray) to ensure that a consistent view is presented
    * throughout the invocation.
     */
    public java.util.Map getStorageMap()
        {
        return __m_StorageMap;
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.util.Converter getValueFromInternalConverter()
        {
        return getContext().getValueFromInternalConverter();
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public com.tangosol.util.Converter getValueToInternalConverter()
        {
        return getContext().getValueToInternalConverter();
        }
    
    /**
     * Factory method to avoid cost of virtual construction of $StorageContext
    * child component.
     */
    protected WrapperContext.StorageContext instantiateStorageContext(Storage storage)
        {
        // this method could be called quite frequently; optimize _newChild()
        WrapperContext.StorageContext ctx = new WrapperContext.StorageContext();
        _linkChild(ctx);
        ctx.setStorage(storage);
        
        return ctx;
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public boolean isInternalValueDecorated(Object oValue, int nDecorId)
        {
        return getContext().isInternalValueDecorated(oValue, nDecorId);
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public boolean isKeyOwned(Object oKey)
        {
        return getContext().isKeyOwned(oKey);
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public Object removeInternalValueDecoration(Object oValue, int nDecorId)
        {
        return getContext().removeInternalValueDecoration(oValue, nDecorId);
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public void setClassLoader(ClassLoader loader)
        {
        getContext().setClassLoader(loader);
        }
    
    // From interface: com.tangosol.net.BackingMapManagerContext
    public void setConfig(com.tangosol.run.xml.XmlElement xml)
        {
        getContext().setConfig(xml);
        }
    
    // Accessor for the property "PrePinnedPartitions"
    /**
     * Setter for property PrePinnedPartitions.<p>
    * The set of partitions externally pinned for the request.
    * It is the responsibility of the caller to unpin the partition set.
     */
    public void setPrePinnedPartitions(com.tangosol.net.partition.PartitionSet parts)
        {
        __m_PrePinnedPartitions = parts;
        }
    
    // Accessor for the property "StorageMap"
    /**
     * Setter for property StorageMap.<p>
    * Map<String, BackingMapContext> containing all named BackingMapContexts
    * known to this InvocationContext.  This map is kept (separately from the
    * service's StorageArray) to ensure that a consistent view is presented
    * throughout the invocation.
     */
    protected void setStorageMap(java.util.Map mapStorage)
        {
        __m_StorageMap = mapStorage;
        }

    // ---- class: com.tangosol.coherence.component.util.WrapperContext$StorageContext
    
    /**
     * The StorageContext is an invocation context aware wrapper of the
     * corresponding $Storage.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class StorageContext
            extends    com.tangosol.coherence.Component
            implements com.tangosol.net.BackingMapContext
        {
        // ---- Fields declarations ----
        
        /**
         * Property Storage
         *
         * The wrapped $Storage.
         */
        private Storage __m_Storage;
        
        // Default constructor
        public StorageContext()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public StorageContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.WrapperContext.StorageContext();
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
                clz = Class.forName("com.tangosol.coherence/component/util/WrapperContext$StorageContext".replace('/', '.'));
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
        
        // From interface: com.tangosol.net.BackingMapContext
        public com.tangosol.util.ObservableMap getBackingMap()
            {
            return getStorage().getBackingMap();
            }
        
        // From interface: com.tangosol.net.BackingMapContext
        public com.tangosol.util.InvocableMap.Entry getBackingMapEntry(Object oKey)
            {
            throw new UnsupportedOperationException();
            }
        
        // From interface: com.tangosol.net.BackingMapContext
        public String getCacheName()
            {
            return getStorage().getCacheName();
            }
        
        // From interface: com.tangosol.net.BackingMapContext
        public java.util.Map getIndexMap()
            {
            return getStorage().getIndexMap();
            }
        
        // From interface: com.tangosol.net.BackingMapContext
        public java.util.Map getIndexMap(com.tangosol.net.partition.PartitionSet partitions)
            {
            return getStorage().getIndexMap(partitions);
            }

        // From interface: com.tangosol.net.BackingMapContext
        public Map<ValueExtractor, MapIndex> getIndexMap(int nPartition)
            {
            return getStorage().getIndexMap(nPartition);
            }

        // From interface: com.tangosol.net.BackingMapContext
        public com.tangosol.net.BackingMapManagerContext getManagerContext()
            {
            return (WrapperContext) get_Module();
            }
        
        // From interface: com.tangosol.net.BackingMapContext
        public com.tangosol.util.InvocableMap.Entry getReadOnlyEntry(Object oKey)
            {
            // import com.tangosol.util.Binary;
            
            try
                {
                Binary  binKey = (Binary) oKey;
                int     iPart  = getManagerContext().getKeyPartition(binKey);
            
                // ensure that the partition has been pinned externally
                if (isPartitionEnlisted(iPart))
                    {
                    return getStorage().getReadOnlyEntry(binKey);
                    }
                else
                    {
                    throw new IllegalStateException("The specified key belongs to a partition "
                        + iPart + " which is not enlisted by the corresponding request");
                    }
                }
            catch (ClassCastException e)
                {
                throw new ClassCastException("This BackingMapContext operates on "
                    + " keys and values in Binary format.");
                }
            }
        
        // Accessor for the property "Storage"
        /**
         * Getter for property Storage.<p>
        * The wrapped $Storage.
         */
        public Storage getStorage()
            {
            return __m_Storage;
            }
        
        protected boolean isPartitionEnlisted(int iPart)
            {
            WrapperContext ctx = (WrapperContext) get_Module();
            
            return ctx.getPrePinnedPartitions().contains(iPart);
            }
        
        // Accessor for the property "Storage"
        /**
         * Setter for property Storage.<p>
        * The wrapped $Storage.
         */
        public void setStorage(Storage storage)
            {
            __m_Storage = storage;
            }
        
        // Declared at the super level
        public String toString()
            {
            return get_Name() +
                " (CacheName=" + getCacheName() + ')';
            }
        }
    }
