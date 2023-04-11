
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.connector.NamedCacheRecord

package com.tangosol.coherence.component.connector;

/**
 * Expose a NamedCache service as a MappedRecord interface
 */
/*
* Integrates
*     com.tangosol.net.NamedCache
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class NamedCacheRecord
        extends    com.tangosol.coherence.component.Connector
        implements com.tangosol.net.NamedCache,
                   jakarta.resource.cci.MappedRecord
    {
    // ---- Fields declarations ----
    
    /**
     * Property NamedCache
     *
     * Wrapped NameCache object
     */
    private transient com.tangosol.net.NamedCache __m_NamedCache;
    
    /**
     * Property RecordName
     *
     */
    private transient String __m_RecordName;
    
    /**
     * Property RecordShortDescription
     *
     */
    private String __m_RecordShortDescription;
    
    // Default constructor
    public NamedCacheRecord()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public NamedCacheRecord(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setRecordShortDescription("NamedCache exposed as MappedRecord");
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
        return new com.tangosol.coherence.component.connector.NamedCacheRecord();
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
            clz = Class.forName("com.tangosol.coherence/component/connector/NamedCacheRecord".replace('/', '.'));
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
    
    //++ com.tangosol.net.NamedCache integration
    // Access optimization
    // properties integration
    // methods integration
    public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        getNamedCache().addIndex(extractor, fOrdered, comparator);
        }
    public void addMapListener(com.tangosol.util.MapListener l)
        {
        getNamedCache().addMapListener(l);
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
    public Object aggregate(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        return getNamedCache().aggregate(collKeys, agent);
        }
    public void clear()
        {
        getNamedCache().clear();
        }
    public boolean containsKey(Object oKey)
        {
        return getNamedCache().containsKey(oKey);
        }
    public boolean containsValue(Object oValue)
        {
        return getNamedCache().containsValue(oValue);
        }
    public void destroy()
        {
        getNamedCache().destroy();
        }
    public java.util.Set entrySet()
        {
        return getNamedCache().entrySet();
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
    public java.util.Map getAll(java.util.Collection col)
        {
        return getNamedCache().getAll(col);
        }
    public String getCacheName()
        {
        return getNamedCache().getCacheName();
        }
    public com.tangosol.net.CacheService getCacheService()
        {
        return getNamedCache().getCacheService();
        }
    public Object invoke(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getNamedCache().invoke(oKey, agent);
        }
    public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getNamedCache().invokeAll(filter, agent);
        }
    public java.util.Map invokeAll(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return getNamedCache().invokeAll(collKeys, agent);
        }
    public boolean isActive()
        {
        return getNamedCache().isActive();
        }
    public boolean isEmpty()
        {
        return getNamedCache().isEmpty();
        }
    public java.util.Set keySet()
        {
        return getNamedCache().keySet();
        }
    public java.util.Set keySet(com.tangosol.util.Filter filter)
        {
        return getNamedCache().keySet(filter);
        }
    public boolean lock(Object oKey)
        {
        return getNamedCache().lock(oKey);
        }
    public boolean lock(Object oKey, long cWait)
        {
        return getNamedCache().lock(oKey, cWait);
        }
    public Object put(Object oKey, Object oValue)
        {
        return getNamedCache().put(oKey, oValue);
        }
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        return getNamedCache().put(oKey, oValue, cMillis);
        }
    public void putAll(java.util.Map map)
        {
        getNamedCache().putAll(map);
        }
    public void release()
        {
        getNamedCache().release();
        }
    public Object remove(Object oKey)
        {
        return getNamedCache().remove(oKey);
        }
    public void removeIndex(com.tangosol.util.ValueExtractor extractor)
        {
        getNamedCache().removeIndex(extractor);
        }
    public void removeMapListener(com.tangosol.util.MapListener l)
        {
        getNamedCache().removeMapListener(l);
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
    public boolean unlock(Object oKey)
        {
        return getNamedCache().unlock(oKey);
        }
    public java.util.Collection values()
        {
        return getNamedCache().values();
        }
    //-- com.tangosol.net.NamedCache integration
    
    // From interface: jakarta.resource.cci.MappedRecord
    // Declared at the super level
    public Object clone()
        {
        try
            {
            return super.clone();
            }
        catch (java.lang.CloneNotSupportedException e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    // From interface: jakarta.resource.cci.MappedRecord
    // Declared at the super level
    public boolean equals(Object obj)
        {
        if (obj instanceof NamedCacheRecord)
            {
            return getNamedCache().equals(((NamedCacheRecord) obj).getNamedCache());
            }
        return false;
        }
    
    // Accessor for the property "NamedCache"
    /**
     * Getter for property NamedCache.<p>
    * Wrapped NameCache object
     */
    public com.tangosol.net.NamedCache getNamedCache()
        {
        return __m_NamedCache;
        }
    
    // From interface: jakarta.resource.cci.MappedRecord
    // Accessor for the property "RecordName"
    /**
     * Getter for property RecordName.<p>
     */
    public String getRecordName()
        {
        return getCacheName();
        }
    
    // From interface: jakarta.resource.cci.MappedRecord
    // Accessor for the property "RecordShortDescription"
    /**
     * Getter for property RecordShortDescription.<p>
     */
    public String getRecordShortDescription()
        {
        return __m_RecordShortDescription;
        }
    
    // From interface: com.tangosol.net.NamedCache
    // From interface: jakarta.resource.cci.MappedRecord
    // Declared at the super level
    public int hashCode()
        {
        return getNamedCache().hashCode();
        }
    
    // Accessor for the property "NamedCache"
    /**
     * Setter for property NamedCache.<p>
    * Wrapped NameCache object
     */
    public void setNamedCache(com.tangosol.net.NamedCache cache)
        {
        __m_NamedCache = cache;
        }
    
    // From interface: jakarta.resource.cci.MappedRecord
    // Accessor for the property "RecordName"
    /**
     * Setter for property RecordName.<p>
     */
    public void setRecordName(String sName)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: jakarta.resource.cci.MappedRecord
    // Accessor for the property "RecordShortDescription"
    /**
     * Setter for property RecordShortDescription.<p>
     */
    public void setRecordShortDescription(String sDescr)
        {
        __m_RecordShortDescription = sDescr;
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name().toString() + ": RecordName=" + getRecordName() +
            ", Cache=" + getNamedCache();
        }
    }
