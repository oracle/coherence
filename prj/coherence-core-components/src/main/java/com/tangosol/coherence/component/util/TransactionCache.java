
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.TransactionCache

package com.tangosol.coherence.component.util;

import com.tangosol.net.CacheService;
import com.tangosol.util.Base;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.TransactionMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.transaction.Status;

/*
* Integrates
*     com.tangosol.util.ConcurrentMap
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class TransactionCache
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.net.NamedCache,
                   com.tangosol.util.ConcurrentMap,
                   com.tangosol.util.TransactionMap
    {
    // ---- Fields declarations ----
    
    /**
     * Property Concurrency
     *
     * TransactionMap#Concurrency property
     */
    private transient int __m_Concurrency;
    
    /**
     * Property Map
     *
     * Currently active map. 
     */
    private transient com.tangosol.util.ConcurrentMap __m_Map;
    
    /**
     * Property NamedCache
     *
     * Wrapped NameCache object
     */
    private transient com.tangosol.net.NamedCache __m_NamedCache;
    
    /**
     * Property Status
     *
     * Transaction status. The value is one the values defined by
     * javax.transaction.Status interface.
     * 
     * STATUS_ACTIVE (0) - A transaction is associated with the target object
     * and it is in the active state. 
     * 
     * STATUS_COMMITTED (3) - A transaction is associated with the target
     * object and it has been committed. 
     * 
     * STATUS_COMMITTING (8) -  A transaction is associated with the target
     * object and it is in the process of committing. 
     * 
     * STATUS_MARKED_ROLLBACK (1) - A transaction is associated with the target
     * object and it has been marked for rollback, perhaps as a result of a
     * setRollbackOnly operation. 
     * 
     * STATUS_NO_TRANSACTION (6) - No transaction is currently associated with
     * the target object. 
     * 
     * STATUS_PREPARED (2) - A transaction is associated with the target object
     * and it has been prepared, i.e. all subordinates have responded
     * Vote.Commit.
     * 
     * STATUS_PREPARING (7)- A transaction is associated with the target object
     * and it is in the process of preparing. 
     * 
     * STATUS_ROLLEDBACK (4) - A transaction is associated with the target
     * object and the outcome has been determined as rollback. 
     * 
     * STATUS_ROLLING_BACK (9)- A transaction is associated with the target
     * object and it is in the process of rolling back. 
     * 
     * STATUS_UNKNOWN (5) - A transaction is associated with the target object
     * but its current status cannot be determined.
     */
    private transient int __m_Status;
    
    /**
     * Property TransactionIsolation
     *
     * TransactionMap#Isolation property
     */
    private transient int __m_TransactionIsolation;
    
    /**
     * Property TransactionTimeout
     *
     * @see TransactionMap#TransactionTimeout property
     */
    private transient int __m_TransactionTimeout;
    
    /**
     * Property Validator
     *
     * The vaildator
     */
    private com.tangosol.util.TransactionMap.Validator __m_Validator;
    
    /**
     * Property ValuesImmutable
     *
     * Specifies whether or not the values returned by the base NamedCache are
     * immutable. If set to false, the get operation may force the value
     * cloning to avoid a situation when modifications are not rolled back
     * after explicit rollback.
     */
    private boolean __m_ValuesImmutable;
    
    // Default constructor
    public TransactionCache()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public TransactionCache(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setStatus(6);
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
        return new com.tangosol.coherence.component.util.TransactionCache();
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
            clz = Class.forName("com.tangosol.coherence/component/util/TransactionCache".replace('/', '.'));
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
    
    //++ com.tangosol.util.ConcurrentMap integration
    // Access optimization
    // properties integration
    // methods integration
    public void clear()
        {
        getMap().clear();
        }
    public boolean containsKey(Object oKey)
        {
        return getMap().containsKey(oKey);
        }
    public boolean containsValue(Object oValue)
        {
        return getMap().containsValue(oValue);
        }
    public java.util.Set entrySet()
        {
        return getMap().entrySet();
        }
    public Object get(Object oKey)
        {
        return getMap().get(oKey);
        }
    public boolean isEmpty()
        {
        return getMap().isEmpty();
        }
    public java.util.Set keySet()
        {
        return getMap().keySet();
        }
    public boolean lock(Object oKey)
        {
        return getMap().lock(oKey);
        }
    public boolean lock(Object oKey, long cWait)
        {
        return getMap().lock(oKey, cWait);
        }
    public Object put(Object oKey, Object oValue)
        {
        return getMap().put(oKey, oValue);
        }
    public void putAll(java.util.Map map)
        {
        getMap().putAll(map);
        }
    public Object remove(Object oKey)
        {
        return getMap().remove(oKey);
        }
    public int size()
        {
        return getMap().size();
        }
    public boolean unlock(Object oKey)
        {
        return getMap().unlock(oKey);
        }
    public java.util.Collection values()
        {
        return getMap().values();
        }
    //-- com.tangosol.util.ConcurrentMap integration
    
    // From interface: com.tangosol.net.NamedCache
    public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object aggregate(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public void begin()
        {
        // import com.tangosol.util.ConcurrentMap;
        // import com.tangosol.util.TransactionMap;
        // import javax.transaction.Status;
        
        int nStatus = getStatus();
        if (nStatus == Status.STATUS_NO_TRANSACTION ||
            nStatus == Status.STATUS_ROLLEDBACK     ||
            nStatus == Status.STATUS_COMMITTED)
            {
            ConcurrentMap map = getMap();
            if (map instanceof TransactionMap)
                {
                TransactionMap mapTx = (TransactionMap) map;
        
                mapTx.setTransactionTimeout(getTransactionTimeout());
                mapTx.setValidator(getValidator());
        
                mapTx.begin();
                }
            setStatus(Status.STATUS_ACTIVE);
            }
        else
            {
            throw invalidStatus("begin");
            }
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public void commit()
        {
        // import com.tangosol.util.ConcurrentMap;
        // import com.tangosol.util.TransactionMap;
        // import javax.transaction.Status;
        
        if (getStatus() == Status.STATUS_PREPARED)
            {
            try
                {
                setStatus(Status.STATUS_COMMITTING);
        
                ConcurrentMap map = getMap();
                if (map instanceof TransactionMap)
                    {
                    ((TransactionMap) map).commit();
                    }
        
                setStatus(Status.STATUS_COMMITTED);
                }
            catch (RuntimeException e)
                {
                setStatus(Status.STATUS_UNKNOWN);
                throw e;
                }
            }
        else
            {
            throw invalidStatus("commit");
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void destroy()
        {
        rollback();
        getNamedCache().destroy();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet(com.tangosol.util.Filter filter)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    // From interface: com.tangosol.util.ConcurrentMap
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public boolean equals(Object obj)
        {
        if (obj instanceof TransactionCache)
            {
            return getMap().equals(((TransactionCache) obj).getMap());
            }
        return false;
        }
    
    public static String formatStatus(int nStatus)
        {
        // import javax.transaction.Status;
        
        switch (nStatus)
            {
            case Status.STATUS_ACTIVE:
                return "STATUS_ACTIVE";
            case Status.STATUS_COMMITTED:
                return "STATUS_COMMITTED";
            case Status.STATUS_COMMITTING:
                return "STATUS_COMMITTING";
            case Status.STATUS_MARKED_ROLLBACK:
                return "STATUS_ROLLEDBACK";
            case Status.STATUS_NO_TRANSACTION:
                return "STATUS_NO_TRANSACTION";
            case Status.STATUS_PREPARED:
                return "STATUS_PREPARED";
            case Status.STATUS_PREPARING:
                return "STATUS_PREPARING";
            case Status.STATUS_ROLLEDBACK:
                return "STATUS_ROLLEDBACK";
            case Status.STATUS_ROLLING_BACK:
                return "STATUS_ROLLING_BACK";
            case Status.STATUS_UNKNOWN:
                return "STATUS_UNKNWON";
            default:
                throw new IllegalArgumentException("Invalid status: " + nStatus);
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map getAll(java.util.Collection colKeys)
        {
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        
        Map mapResult = new HashMap(colKeys.size()); 
        for (Iterator iter = colKeys.iterator(); iter.hasNext();)
            {
            Object oKey = iter.next();
            if (containsKey(oKey))
                {
                mapResult.put(oKey, get(oKey));
                }
            }
        return mapResult;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public com.tangosol.util.ConcurrentMap getBaseMap()
        {
        return getNamedCache();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public String getCacheName()
        {
        return getNamedCache().getCacheName();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public com.tangosol.net.CacheService getCacheService()
        {
        return getNamedCache().getCacheService();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "Concurrency"
    /**
     * Getter for property Concurrency.<p>
    * TransactionMap#Concurrency property
     */
    public int getConcurrency()
        {
        return __m_Concurrency;
        }
    
    // Accessor for the property "Map"
    /**
     * Getter for property Map.<p>
    * Currently active map. 
     */
    public com.tangosol.util.ConcurrentMap getMap()
        {
        return __m_Map;
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
    
    // Accessor for the property "Status"
    /**
     * Getter for property Status.<p>
    * Transaction status. The value is one the values defined by
    * javax.transaction.Status interface.
    * 
    * STATUS_ACTIVE (0) - A transaction is associated with the target object
    * and it is in the active state. 
    * 
    * STATUS_COMMITTED (3) - A transaction is associated with the target object
    * and it has been committed. 
    * 
    * STATUS_COMMITTING (8) -  A transaction is associated with the target
    * object and it is in the process of committing. 
    * 
    * STATUS_MARKED_ROLLBACK (1) - A transaction is associated with the target
    * object and it has been marked for rollback, perhaps as a result of a
    * setRollbackOnly operation. 
    * 
    * STATUS_NO_TRANSACTION (6) - No transaction is currently associated with
    * the target object. 
    * 
    * STATUS_PREPARED (2) - A transaction is associated with the target object
    * and it has been prepared, i.e. all subordinates have responded
    * Vote.Commit.
    * 
    * STATUS_PREPARING (7)- A transaction is associated with the target object
    * and it is in the process of preparing. 
    * 
    * STATUS_ROLLEDBACK (4) - A transaction is associated with the target
    * object and the outcome has been determined as rollback. 
    * 
    * STATUS_ROLLING_BACK (9)- A transaction is associated with the target
    * object and it is in the process of rolling back. 
    * 
    * STATUS_UNKNOWN (5) - A transaction is associated with the target object
    * but its current status cannot be determined.
     */
    public int getStatus()
        {
        return __m_Status;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "TransactionIsolation"
    /**
     * Getter for property TransactionIsolation.<p>
    * TransactionMap#Isolation property
     */
    public int getTransactionIsolation()
        {
        return __m_TransactionIsolation;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "TransactionTimeout"
    /**
     * Getter for property TransactionTimeout.<p>
    * @see TransactionMap#TransactionTimeout property
     */
    public int getTransactionTimeout()
        {
        return __m_TransactionTimeout;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "Validator"
    /**
     * Getter for property Validator.<p>
    * The vaildator
     */
    public com.tangosol.util.TransactionMap.Validator getValidator()
        {
        return __m_Validator;
        }
    
    // From interface: com.tangosol.net.NamedCache
    // From interface: com.tangosol.util.ConcurrentMap
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public int hashCode()
        {
        return getMap().hashCode();
        }
    
    public void initialize(com.tangosol.net.NamedCache cache)
        {
        // import com.tangosol.net.CacheService;
        
        _assert(cache != null && getNamedCache() == null,
            "NamedCache is not resettable");
        _assert(cache.getCacheService().getInfo().getServiceType() != CacheService.TYPE_REMOTE,
            "RemoteNamedCache does not support local transactions");
        setNamedCache(cache);
        setMap(cache);
        }
    
    protected com.tangosol.coherence.component.util.deltaMap.TransactionMap instantiateTransactionMap()
        {
        // import Component.Util.DeltaMap.TransactionMap as com.tangosol.coherence.component.util.deltaMap.TransactionMap;
        
        int nConcur    = getConcurrency();
        int nIsolation = getTransactionIsolation();
        
        com.tangosol.coherence.component.util.deltaMap.TransactionMap mapTx = instantiateTransactionMap(nConcur, nIsolation);
        
        return mapTx;
        }
    
    public static com.tangosol.coherence.component.util.deltaMap.TransactionMap instantiateTransactionMap(int nConcur, int nIsolation)
        {
        // import Component.Util.DeltaMap.TransactionMap as com.tangosol.coherence.component.util.deltaMap.TransactionMap;
        // import Component.Util.DeltaMap.TransactionMap.Optimistic as com.tangosol.coherence.component.util.deltaMap.transactionMap.Optimistic;
        // import Component.Util.DeltaMap.TransactionMap.Optimistic.Commited as com.tangosol.coherence.component.util.deltaMap.transactionMap.optimistic.Commited;
        // import Component.Util.DeltaMap.TransactionMap.Optimistic.Repeatable as com.tangosol.coherence.component.util.deltaMap.transactionMap.optimistic.Repeatable;
        // import Component.Util.DeltaMap.TransactionMap.Optimistic.Serializable as com.tangosol.coherence.component.util.deltaMap.transactionMap.optimistic.Serializable;
        // import Component.Util.DeltaMap.TransactionMap.Pessimistic.Commited as com.tangosol.coherence.component.util.deltaMap.transactionMap.pessimistic.Commited;
        // import Component.Util.DeltaMap.TransactionMap.Pessimistic.Repeatable as com.tangosol.coherence.component.util.deltaMap.transactionMap.pessimistic.Repeatable;
        // import Component.Util.DeltaMap.TransactionMap.Pessimistic.Serializable as com.tangosol.coherence.component.util.deltaMap.transactionMap.pessimistic.Serializable;
        
        boolean fExternal = false;
        if (nConcur == com.tangosol.coherence.component.util.deltaMap.TransactionMap.CONCUR_EXTERNAL)
            {
            nConcur   = com.tangosol.coherence.component.util.deltaMap.TransactionMap.CONCUR_OPTIMISTIC;
            fExternal = true;
            }
        
        com.tangosol.coherence.component.util.deltaMap.TransactionMap mapTx;
        switch (nConcur*8 + nIsolation)
            {
            case com.tangosol.coherence.component.util.deltaMap.TransactionMap.CONCUR_PESSIMISTIC*8 + com.tangosol.coherence.component.util.deltaMap.TransactionMap.TRANSACTION_GET_COMMITTED:
                mapTx = new com.tangosol.coherence.component.util.deltaMap.transactionMap.pessimistic.Commited();
                break;
            case com.tangosol.coherence.component.util.deltaMap.TransactionMap.CONCUR_PESSIMISTIC*8 + com.tangosol.coherence.component.util.deltaMap.TransactionMap.TRANSACTION_REPEATABLE_GET:
                mapTx = new com.tangosol.coherence.component.util.deltaMap.transactionMap.pessimistic.Repeatable();
                break;
            case com.tangosol.coherence.component.util.deltaMap.TransactionMap.CONCUR_PESSIMISTIC*8 + com.tangosol.coherence.component.util.deltaMap.TransactionMap.TRANSACTION_SERIALIZABLE:
                mapTx = new com.tangosol.coherence.component.util.deltaMap.transactionMap.pessimistic.Serializable();
                break;
            case com.tangosol.coherence.component.util.deltaMap.TransactionMap.CONCUR_OPTIMISTIC*8  + com.tangosol.coherence.component.util.deltaMap.TransactionMap.TRANSACTION_GET_COMMITTED:
                mapTx = new com.tangosol.coherence.component.util.deltaMap.transactionMap.optimistic.Commited();
                break;
            case com.tangosol.coherence.component.util.deltaMap.TransactionMap.CONCUR_OPTIMISTIC*8  + com.tangosol.coherence.component.util.deltaMap.TransactionMap.TRANSACTION_REPEATABLE_GET:
                mapTx = new com.tangosol.coherence.component.util.deltaMap.transactionMap.optimistic.Repeatable();
                break;        
            case com.tangosol.coherence.component.util.deltaMap.TransactionMap.CONCUR_OPTIMISTIC*8  + com.tangosol.coherence.component.util.deltaMap.TransactionMap.TRANSACTION_SERIALIZABLE:
                mapTx = new com.tangosol.coherence.component.util.deltaMap.transactionMap.optimistic.Serializable();
                break;
            default:
                throw new IllegalArgumentException("Invalid concurrency or isolation level: "
                    + nConcur + ", " + nIsolation);
                }
        
        if (fExternal)
            {
            ((com.tangosol.coherence.component.util.deltaMap.transactionMap.Optimistic) mapTx).setLockExternal(true);
            }
        
        return mapTx;
        }
    
    protected RuntimeException invalidStatus(String sMethod)
        {
        return new IllegalStateException(sMethod +
            "-- invalid transaction status: " + this);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object invoke(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean isActive()
        {
        return getNamedCache().isActive();
        }

    // From interface: com.tangosol.net.NamedCache
    public boolean isReady()
        {
        return getNamedCache().isReady();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "ValuesImmutable"
    /**
     * Getter for property ValuesImmutable.<p>
    * Specifies whether or not the values returned by the base NamedCache are
    * immutable. If set to false, the get operation may force the value cloning
    * to avoid a situation when modifications are not rolled back after
    * explicit rollback.
     */
    public boolean isValuesImmutable()
        {
        return __m_ValuesImmutable;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set keySet(com.tangosol.util.Filter filter)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public void prepare()
        {
        // import com.tangosol.util.ConcurrentMap;
        // import com.tangosol.util.TransactionMap;
        // import javax.transaction.Status;
        
        if (getStatus() == Status.STATUS_ACTIVE)
            {
            try
                {
                setStatus(Status.STATUS_PREPARING);
        
                ConcurrentMap map = getMap();
                if (map instanceof TransactionMap)
                    {
                    ((TransactionMap) map).prepare();
                    }
        
                setStatus(Status.STATUS_PREPARED);
                }
            catch (RuntimeException e)
                {
                setStatus(Status.STATUS_MARKED_ROLLBACK);
                throw e;
                }
            }
        else
            {
            throw invalidStatus("prepare");
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
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
    
    // From interface: com.tangosol.net.NamedCache
    public void release()
        {
        rollback();
        getNamedCache().release();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeIndex(com.tangosol.util.ValueExtractor extractor)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public void rollback()
        {
        // import com.tangosol.util.ConcurrentMap;
        // import com.tangosol.util.TransactionMap;
        // import javax.transaction.Status;
        
        try
            {
            setStatus(Status.STATUS_ROLLING_BACK);
        
            ConcurrentMap map = getMap();
            if (map instanceof TransactionMap)
                {
                // rollback should never fail
                ((TransactionMap) map).rollback();
                }
            }
        finally
            {
            setStatus(Status.STATUS_ROLLEDBACK);
            }
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "Concurrency"
    /**
     * Setter for property Concurrency.<p>
    * TransactionMap#Concurrency property
     */
    public void setConcurrency(int nConcurrency)
        {
        __m_Concurrency = nConcurrency;
        }
    
    // Accessor for the property "Map"
    /**
     * Setter for property Map.<p>
    * Currently active map. 
     */
    protected void setMap(com.tangosol.util.ConcurrentMap map)
        {
        __m_Map = map;
        }
    
    // Accessor for the property "NamedCache"
    /**
     * Setter for property NamedCache.<p>
    * Wrapped NameCache object
     */
    protected void setNamedCache(com.tangosol.net.NamedCache cache)
        {
        __m_NamedCache = cache;
        }
    
    // Accessor for the property "Status"
    /**
     * Setter for property Status.<p>
    * Transaction status. The value is one the values defined by
    * javax.transaction.Status interface.
    * 
    * STATUS_ACTIVE (0) - A transaction is associated with the target object
    * and it is in the active state. 
    * 
    * STATUS_COMMITTED (3) - A transaction is associated with the target object
    * and it has been committed. 
    * 
    * STATUS_COMMITTING (8) -  A transaction is associated with the target
    * object and it is in the process of committing. 
    * 
    * STATUS_MARKED_ROLLBACK (1) - A transaction is associated with the target
    * object and it has been marked for rollback, perhaps as a result of a
    * setRollbackOnly operation. 
    * 
    * STATUS_NO_TRANSACTION (6) - No transaction is currently associated with
    * the target object. 
    * 
    * STATUS_PREPARED (2) - A transaction is associated with the target object
    * and it has been prepared, i.e. all subordinates have responded
    * Vote.Commit.
    * 
    * STATUS_PREPARING (7)- A transaction is associated with the target object
    * and it is in the process of preparing. 
    * 
    * STATUS_ROLLEDBACK (4) - A transaction is associated with the target
    * object and the outcome has been determined as rollback. 
    * 
    * STATUS_ROLLING_BACK (9)- A transaction is associated with the target
    * object and it is in the process of rolling back. 
    * 
    * STATUS_UNKNOWN (5) - A transaction is associated with the target object
    * but its current status cannot be determined.
     */
    public void setStatus(int pStatus)
        {
        __m_Status = pStatus;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "TransactionIsolation"
    /**
     * Setter for property TransactionIsolation.<p>
    * TransactionMap#Isolation property
     */
    public void setTransactionIsolation(int nIsolation)
        {
        __m_TransactionIsolation = nIsolation;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "TransactionTimeout"
    /**
     * Setter for property TransactionTimeout.<p>
    * @see TransactionMap#TransactionTimeout property
     */
    public void setTransactionTimeout(int nTimeout)
        {
        __m_TransactionTimeout = nTimeout;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "Validator"
    /**
     * Setter for property Validator.<p>
    * The vaildator
     */
    public void setValidator(com.tangosol.util.TransactionMap.Validator validator)
        {
        __m_Validator = validator;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "ValuesImmutable"
    /**
     * Setter for property ValuesImmutable.<p>
    * Specifies whether or not the values returned by the base NamedCache are
    * immutable. If set to false, the get operation may force the value cloning
    * to avoid a situation when modifications are not rolled back after
    * explicit rollback.
     */
    public void setValuesImmutable(boolean fImmutable)
        {
        __m_ValuesImmutable = fImmutable;
        }
    
    // Declared at the super level
    public String toString()
        {
        // import com.tangosol.util.Base;
        
        return "TransactionalCache: " + formatStatus(getStatus())
            + "\n  BaseMap=" + getNamedCache()
            + "\n  DeltaMap=" + Base.indentString(getMap().toString(), "    ", false)
            ;
        }
    }
