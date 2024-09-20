
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.collections.wrapperMap.WrapperNamedCache

package com.tangosol.coherence.component.util.collections.wrapperMap;

import com.tangosol.util.Filter;

/*
* Integrates
*     com.tangosol.net.NamedCache
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class WrapperNamedCache
        extends    com.tangosol.coherence.component.util.collections.WrapperMap
        implements com.tangosol.net.NamedCache
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
        __mapChildren.put("EntrySet", com.tangosol.coherence.component.util.collections.WrapperMap.EntrySet.class);
        __mapChildren.put("KeySet", com.tangosol.coherence.component.util.collections.WrapperMap.KeySet.class);
        __mapChildren.put("Values", com.tangosol.coherence.component.util.collections.WrapperMap.Values.class);
        }
    
    // Default constructor
    public WrapperNamedCache()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public WrapperNamedCache(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.collections.wrapperMap.WrapperNamedCache();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        return WrapperNamedCache.class;
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
    
    //++ com.tangosol.net.NamedCache integration
    // Access optimization
    // properties integration
    // methods integration
    public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        ((com.tangosol.net.NamedCache) getMap()).addIndex(extractor, fOrdered, comparator);
        }
    public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
        {
        ((com.tangosol.net.NamedCache) getMap()).addMapListener(listener, filter, fLite);
        }
    public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
        {
        ((com.tangosol.net.NamedCache) getMap()).addMapListener(listener, oKey, fLite);
        }
    public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        return ((com.tangosol.net.NamedCache) getMap()).aggregate(filter, agent);
        }
    public Object aggregate(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        return ((com.tangosol.net.NamedCache) getMap()).aggregate(collKeys, agent);
        }
    public void destroy()
        {
        ((com.tangosol.net.NamedCache) getMap()).destroy();
        }
    public java.util.Set entrySet(com.tangosol.util.Filter filter)
        {
        return ((com.tangosol.net.NamedCache) getMap()).entrySet(filter);
        }
    public java.util.Set entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        return ((com.tangosol.net.NamedCache) getMap()).entrySet(filter, comparator);
        }
    public java.util.Map getAll(java.util.Collection colKeys)
        {
        return ((com.tangosol.net.NamedCache) getMap()).getAll(colKeys);
        }
    public String getCacheName()
        {
        return ((com.tangosol.net.NamedCache) getMap()).getCacheName();
        }
    /**
     * Getter for property CacheService.<p>
     */
    public com.tangosol.net.CacheService getCacheService()
        {
        return ((com.tangosol.net.NamedCache) getMap()).getCacheService();
        }
    public Object invoke(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return ((com.tangosol.net.NamedCache) getMap()).invoke(oKey, agent);
        }
    public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return ((com.tangosol.net.NamedCache) getMap()).invokeAll(filter, agent);
        }
    public java.util.Map invokeAll(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return ((com.tangosol.net.NamedCache) getMap()).invokeAll(collKeys, agent);
        }
    public boolean isActive()
        {
        return ((com.tangosol.net.NamedCache) getMap()).isActive();
        }
    @Override
    public boolean isReady()
        {
        return ((com.tangosol.net.NamedCache) getMap()).isReady();
        }
    public java.util.Set keySet(com.tangosol.util.Filter filter)
        {
        return ((com.tangosol.net.NamedCache) getMap()).keySet(filter);
        }
    public boolean lock(Object oKey)
        {
        return ((com.tangosol.net.NamedCache) getMap()).lock(oKey);
        }
    public boolean lock(Object oKey, long cWait)
        {
        return ((com.tangosol.net.NamedCache) getMap()).lock(oKey, cWait);
        }
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        return ((com.tangosol.net.NamedCache) getMap()).put(oKey, oValue, cMillis);
        }
    public void release()
        {
        ((com.tangosol.net.NamedCache) getMap()).release();
        }
    public void removeIndex(com.tangosol.util.ValueExtractor extractor)
        {
        ((com.tangosol.net.NamedCache) getMap()).removeIndex(extractor);
        }
    public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
        {
        ((com.tangosol.net.NamedCache) getMap()).removeMapListener(listener, filter);
        }
    public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
        {
        ((com.tangosol.net.NamedCache) getMap()).removeMapListener(listener, oKey);
        }
    public boolean unlock(Object oKey)
        {
        return ((com.tangosol.net.NamedCache) getMap()).unlock(oKey);
        }
    //-- com.tangosol.net.NamedCache integration
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;
        
        addMapListener(listener, (Filter) null, false);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;
        
        removeMapListener(listener, (Filter) null);
        }
    }
