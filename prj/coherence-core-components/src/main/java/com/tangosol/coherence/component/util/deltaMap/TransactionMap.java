
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.deltaMap.TransactionMap

package com.tangosol.coherence.component.util.deltaMap;

import com.tangosol.util.Base;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.WrapperConcurrentMap;
import java.util.Iterator;
import java.util.Set;

/**
 * DeltaMap is a Map implementation based on another map. The implentation is
 * not thread safe. The model for the DeltaMap includes four maps: InsertMap,
 * UpdateMap, DeleteMap and (optionally, based on the RepeatableRead setting)
 * ReadMap
 * 
 * TransactionMap is an implementation of com.tangosol.util.TransactionMap
 * interface.
 * 
 * For a technical reason (to simplify the use of interface constants) it
 * integrates it as well.
 */
/*
* Integrates
*     com.tangosol.util.TransactionMap
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class TransactionMap
        extends    com.tangosol.coherence.component.util.DeltaMap
        implements com.tangosol.util.TransactionMap
    {
    // ---- Fields declarations ----
    
    /**
     * Property BaseLockSet
     *
     * A set of locks on the BaseMap currently held by this transaction.
     */
    private transient com.tangosol.util.LiteSet __m_BaseLockSet;
    
    /**
     * Property BaseMap
     *
     * The base map. It is usually the same as the OriginalMap.
     * 
     * @see #initialize
     */
    private transient com.tangosol.util.ConcurrentMap __m_BaseMap;
    
    /**
     * Property CONCUR_EXTERNAL
     *
     */
    public static final int CONCUR_EXTERNAL = 3; // com.tangosol.util.TransactionMap.CONCUR_EXTERNAL;
    
    /**
     * Property CONCUR_OPTIMISTIC
     *
     */
    public static final int CONCUR_OPTIMISTIC = 2; // com.tangosol.util.TransactionMap.CONCUR_OPTIMISTIC;
    
    /**
     * Property CONCUR_PESSIMISTIC
     *
     */
    public static final int CONCUR_PESSIMISTIC = 1; // com.tangosol.util.TransactionMap.CONCUR_PESSIMISTIC;
    
    /**
     * Property Concurrency
     *
     * @see com.tangosol.util.TransactionMap CONCUR_*
     */
    private int __m_Concurrency;
    
    /**
     * Property LockWaitMillis
     *
     * The number of milliseconds to continue trying to obtain a lock
     * neccessary for a transactional operation. Value of zero means "no wait".
     *  Negative value means "wait as long as necessary". If the value is
     * negative and the TransactionTimeout is positive, the actual value is
     * calculated as a time left till the transaction timeout.
     */
    private long __m_LockWaitMillis;
    
    /**
     * Property TRANSACTION_GET_COMMITTED
     *
     */
    public static final int TRANSACTION_GET_COMMITTED = 1; // com.tangosol.util.TransactionMap.TRANSACTION_GET_COMMITTED;
    
    /**
     * Property TRANSACTION_REPEATABLE_GET
     *
     */
    public static final int TRANSACTION_REPEATABLE_GET = 2; // com.tangosol.util.TransactionMap.TRANSACTION_REPEATABLE_GET;
    
    /**
     * Property TRANSACTION_SERIALIZABLE
     *
     */
    public static final int TRANSACTION_SERIALIZABLE = 3; // com.tangosol.util.TransactionMap.TRANSACTION_SERIALIZABLE;
    
    /**
     * Property TransactionIsolation
     *
     * @see com.tangosol.util.TransactionMap TRANSACTION_*
     */
    private int __m_TransactionIsolation;
    
    /**
     * Property TransactionStart
     *
     * The time of Transaction start. Used only if the TransactionTimeout value
     * is set to a non zero value
     */
    private transient long __m_TransactionStart;
    
    /**
     * Property TransactionTimeout
     *
     * Specifies the timeout value (in seconds) associated with this
     * transaction. Value of 0 means "no timeout".
     */
    private int __m_TransactionTimeout;
    
    /**
     * Property Validator
     *
     */
    private com.tangosol.util.TransactionMap.Validator __m_Validator;
    
    // Initializing constructor
    public TransactionMap(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/deltaMap/TransactionMap".replace('/', '.'));
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
    
    //++ com.tangosol.util.TransactionMap integration
    // Access optimization
    // properties integration
    // methods integration
    //-- com.tangosol.util.TransactionMap integration
    
    // From interface: com.tangosol.util.TransactionMap
    public void begin()
        {
        // import com.tangosol.util.Base;
        
        _assert(getOriginalMap() != null && getUpdateMap() != null);
        setTransactionStart(Base.getSafeTimeMillis());
        }
    
    /**
     * Check whether or not the transaction timed out
     */
    protected void checkTimeout()
        {
        // import com.tangosol.util.Base;
        
        int nTimeout = getTransactionTimeout();
        if (nTimeout > 0)
            {
            long lStart = getTransactionStart();
            long lTime  = Base.getSafeTimeMillis();
        
            if ((int) ((lTime - lStart)/1000) > nTimeout)
                {
                rollback();
                throw new RuntimeException("Transaction timed-out: " + 
                    " elapsed=" + (int) ((lTime - lStart)/1000) + ", timeout=" + nTimeout);
                }
            }
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public void clear()
        {
        enlistAll(true);
        
        super.clear();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public void commit()
        {
        resolve(getBaseMap());
        reset();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public boolean containsKey(Object oKey)
        {
        enlist(oKey, false);
        
        return super.containsKey(oKey);
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public boolean containsValue(Object oValue)
        {
        enlistAll(false);
        
        return super.containsValue(oValue);
        }
    
    /**
     * Enlist a specified resource into a transaction.
    * 
    * @param oKey the resource key
    * @param fWrite if true, the resource should be elisted for read and write;
    * otherwise for read only
    * 
    * @throws ConcurrenModificationException if the resource cannot be enlisted
    * 
     */
    protected void enlist(Object oKey, boolean fWrite)
        {
        }
    
    /**
     * Enlist all map resources into a transaction.
    * 
    * @param fWrite if true, resources should be elisted for read and write;
    * otherwise for read only
    * 
    * @throws ConcurrenModificationException if some of the resources cannot be
    * enlisted
     */
    protected void enlistAll(boolean fWrite)
        {
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public java.util.Set entrySet()
        {
        enlistAll(false);
        
        return super.entrySet();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public Object get(Object oKey)
        {
        enlist(oKey, false);
        
        return super.get(oKey);
        }
    
    // Declared at the super level
    public java.util.Map getAll(java.util.Collection colKeys)
        {
        // import java.util.Iterator;
        
        for (Iterator iter = colKeys.iterator(); iter.hasNext();)
            {
            Object oKey = iter.next();
        
            enlist(oKey, false);
            }
        
        return super.getAll(colKeys);
        }
    
    // Accessor for the property "BaseLockSet"
    /**
     * Getter for property BaseLockSet.<p>
    * A set of locks on the BaseMap currently held by this transaction.
     */
    public com.tangosol.util.LiteSet getBaseLockSet()
        {
        return __m_BaseLockSet;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "BaseMap"
    /**
     * Getter for property BaseMap.<p>
    * The base map. It is usually the same as the OriginalMap.
    * 
    * @see #initialize
     */
    public com.tangosol.util.ConcurrentMap getBaseMap()
        {
        return __m_BaseMap;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "Concurrency"
    /**
     * Getter for property Concurrency.<p>
    * @see com.tangosol.util.TransactionMap CONCUR_*
     */
    public int getConcurrency()
        {
        return __m_Concurrency;
        }
    
    // Accessor for the property "LockWaitMillis"
    /**
     * Getter for property LockWaitMillis.<p>
    * The number of milliseconds to continue trying to obtain a lock neccessary
    * for a transactional operation. Value of zero means "no wait".  Negative
    * value means "wait as long as necessary". If the value is negative and the
    * TransactionTimeout is positive, the actual value is calculated as a time
    * left till the transaction timeout.
     */
    public long getLockWaitMillis()
        {
        // import com.tangosol.util.Base;
        
        long lWaitMillis = __m_LockWaitMillis;
        int  nTimeout    = getTransactionTimeout();
        
        if (lWaitMillis < 0 && nTimeout > 0)
            {
            long lElapsed = Base.getSafeTimeMillis() - getTransactionStart();
        
            lWaitMillis = Math.max(0L, ((long) nTimeout)*1000L - lElapsed);
            }
        
        return lWaitMillis;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "TransactionIsolation"
    /**
     * Getter for property TransactionIsolation.<p>
    * @see com.tangosol.util.TransactionMap TRANSACTION_*
     */
    public int getTransactionIsolation()
        {
        return __m_TransactionIsolation;
        }
    
    // Accessor for the property "TransactionStart"
    /**
     * Getter for property TransactionStart.<p>
    * The time of Transaction start. Used only if the TransactionTimeout value
    * is set to a non zero value
     */
    public long getTransactionStart()
        {
        return __m_TransactionStart;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "TransactionTimeout"
    /**
     * Getter for property TransactionTimeout.<p>
    * Specifies the timeout value (in seconds) associated with this
    * transaction. Value of 0 means "no timeout".
     */
    public int getTransactionTimeout()
        {
        return __m_TransactionTimeout;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "Validator"
    /**
     * Getter for property Validator.<p>
     */
    public com.tangosol.util.TransactionMap.Validator getValidator()
        {
        return __m_Validator;
        }
    
    // Declared at the super level
    public void initialize(java.util.Map mapOrig, java.util.Map mapInsert, java.util.Map mapUpdate, java.util.Map mapDelete, java.util.Map mapRead)
        {
        // import com.tangosol.util.ConcurrentMap;
        // import com.tangosol.util.WrapperConcurrentMap;
        
        if (getBaseMap() == null)
            {
            setBaseMap(mapOrig instanceof ConcurrentMap ?
                (ConcurrentMap) mapOrig : new WrapperConcurrentMap(mapOrig, false, -1L));
            }
        
        super.initialize(mapOrig, mapInsert, mapUpdate, mapDelete, mapRead);
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public boolean isEmpty()
        {
        enlistAll(false);
        
        return super.isEmpty();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public boolean isValuesImmutable()
        {
        return false;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public java.util.Set keySet()
        {
        enlistAll(false);
        
        return super.keySet();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public boolean lock(Object oKey)
        {
        // propagate the lock onto the base
        return getBaseMap().lock(oKey);
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public boolean lock(Object oKey, long cWait)
        {
        // propagate the lock onto the base
        return getBaseMap().lock(oKey, cWait);
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public void prepare()
        {
        checkTimeout();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public Object put(Object oKey, Object oValue)
        {
        enlist(oKey, true);
        
        return super.put(oKey, oValue);
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public Object remove(Object oKey)
        {
        enlist(oKey, true);
        
        return super.remove(oKey);
        }
    
    // Declared at the super level
    /**
     * Discard the changes to the map ("rollback").
     */
    public void reset()
        {
        // import com.tangosol.util.ConcurrentMap;
        // import java.util.Iterator;
        // import java.util.Set;
        
        Set setLock = getBaseLockSet();
        if (!setLock.isEmpty())
            {
            ConcurrentMap mapBase = getBaseMap();
        
            for (Iterator iter = setLock.iterator(); iter.hasNext();)
                {
                mapBase.unlock(iter.next());
                }
            setLock.clear();
            }
        
        super.reset();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public void rollback()
        {
        reset();
        }
    
    // Accessor for the property "BaseLockSet"
    /**
     * Setter for property BaseLockSet.<p>
    * A set of locks on the BaseMap currently held by this transaction.
     */
    protected void setBaseLockSet(com.tangosol.util.LiteSet setLock)
        {
        __m_BaseLockSet = setLock;
        }
    
    // Accessor for the property "BaseMap"
    /**
     * Setter for property BaseMap.<p>
    * The base map. It is usually the same as the OriginalMap.
    * 
    * @see #initialize
     */
    protected void setBaseMap(com.tangosol.util.ConcurrentMap map)
        {
        __m_BaseMap = map;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "Concurrency"
    /**
     * Setter for property Concurrency.<p>
    * @see com.tangosol.util.TransactionMap CONCUR_*
     */
    public void setConcurrency(int nConcurrency)
        {
        if (is_Constructed())
            {
            throw new IllegalStateException();
            }
        __m_Concurrency = (nConcurrency);
        }
    
    // Accessor for the property "LockWaitMillis"
    /**
     * Setter for property LockWaitMillis.<p>
    * The number of milliseconds to continue trying to obtain a lock neccessary
    * for a transactional operation. Value of zero means "no wait".  Negative
    * value means "wait as long as necessary". If the value is negative and the
    * TransactionTimeout is positive, the actual value is calculated as a time
    * left till the transaction timeout.
     */
    public void setLockWaitMillis(long cWaitMillis)
        {
        __m_LockWaitMillis = cWaitMillis;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "TransactionIsolation"
    /**
     * Setter for property TransactionIsolation.<p>
    * @see com.tangosol.util.TransactionMap TRANSACTION_*
     */
    public void setTransactionIsolation(int iLevel)
        {
        if (is_Constructed())
            {
            throw new IllegalStateException();
            }
        __m_TransactionIsolation = (iLevel);
        }
    
    // Accessor for the property "TransactionStart"
    /**
     * Setter for property TransactionStart.<p>
    * The time of Transaction start. Used only if the TransactionTimeout value
    * is set to a non zero value
     */
    public void setTransactionStart(long pTransactionStart)
        {
        __m_TransactionStart = pTransactionStart;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "TransactionTimeout"
    /**
     * Setter for property TransactionTimeout.<p>
    * Specifies the timeout value (in seconds) associated with this
    * transaction. Value of 0 means "no timeout".
     */
    public void setTransactionTimeout(int nTimeout)
        {
        __m_TransactionTimeout = nTimeout;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Accessor for the property "Validator"
    /**
     * Setter for property Validator.<p>
     */
    public void setValidator(com.tangosol.util.TransactionMap.Validator validator)
        {
        __m_Validator = validator;
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public void setValuesImmutable(boolean fImmutable)
        {
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public int size()
        {
        enlistAll(false);
        
        return super.size();
        }
    
    // From interface: com.tangosol.util.TransactionMap
    public boolean unlock(Object oKey)
        {
        // propagate the unlock onto the base
        return getBaseMap().unlock(oKey);
        }
    
    // From interface: com.tangosol.util.TransactionMap
    // Declared at the super level
    public java.util.Collection values()
        {
        enlistAll(false);
        
        return super.values();
        }
    }
