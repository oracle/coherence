
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.deltaMap.transactionMap.Optimistic

package com.tangosol.coherence.component.util.deltaMap.transactionMap;

import com.tangosol.util.ChainedEnumerator;
import com.tangosol.util.ConcurrentMap;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
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
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Optimistic
        extends    com.tangosol.coherence.component.util.deltaMap.TransactionMap
    {
    // ---- Fields declarations ----
    
    /**
     * Property LockExternal
     *
     * Indicates that the CONCUR_EXTERNAL concurrency is used.
     */
    private boolean __m_LockExternal;
    
    // Initializing constructor
    public Optimistic(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/deltaMap/transactionMap/Optimistic".replace('/', '.'));
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
    
    // Declared at the super level
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
        // import com.tangosol.util.TransactionMap$Validator as com.tangosol.util.TransactionMap.Validator;
        
        if (fWrite && !isModified(oKey))
            {
            com.tangosol.util.TransactionMap.Validator validator = getValidator();
            if (validator != null)
                {
                validator.enlist(this, oKey);
                }
            }
        
        super.enlist(oKey, fWrite);
        }
    
    // Declared at the super level
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
        // import com.tangosol.util.TransactionMap$Validator as com.tangosol.util.TransactionMap.Validator;
        // import java.util.Iterator;
        // import java.util.Map;
        
        if (!isFullyRead())
            {
            com.tangosol.util.TransactionMap.Validator validator = getValidator();
        
            if (validator != null)
                {
                Map mapOrig = getOriginalMap();
                for (Iterator iter = mapOrig.keySet().iterator(); iter.hasNext();)
                    {
                    enlist(iter.next(), fWrite);
                    }
                }
            }
        
        super.enlistAll(fWrite);
        }
    
    // Accessor for the property "LockExternal"
    /**
     * Getter for property LockExternal.<p>
    * Indicates that the CONCUR_EXTERNAL concurrency is used.
     */
    public boolean isLockExternal()
        {
        return __m_LockExternal;
        }
    
    // Declared at the super level
    public void prepare()
        {
        // import com.tangosol.util.ChainedEnumerator;
        // import com.tangosol.util.ConcurrentMap;
        // import com.tangosol.util.TransactionMap$Validator as com.tangosol.util.TransactionMap.Validator;
        // import java.util.Collections;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.Set;
        
        super.prepare();
        
        ConcurrentMap mapBase = getBaseMap();
        
        Set setInsert  = getInsertKeySet();
        Set setUpdate  = getUpdateKeySet();
        Set setDelete  = getDeleteKeySet();
        Set setRead    = getReadKeySet();
        Set setFanthom = Collections.EMPTY_SET;
        
        if (!isLockExternal())
            {
            Set    setLock    = getBaseLockSet();
            Object oKeyFailed = null;
        
            Iterator iter = new ChainedEnumerator(new Iterator[]
                {
                setInsert.iterator(),
                setUpdate.iterator(),
                setDelete.iterator(),
                setRead  .iterator(),
                });
        
            long cWaitMillis = getLockWaitMillis();
            while (iter.hasNext())
                {
                Object oKey = iter.next();
        
                if (!setLock.contains(oKey))
                    {
                    if (mapBase.lock(oKey, cWaitMillis))
                        {
                        setLock.add(oKey);
                        }
                    else
                        {
                        oKeyFailed = oKey;
                        break;
                        }
                    }
                }
        
            if (oKeyFailed != null)
                {
                rollback();
                throw new ConcurrentModificationException(
                    "Failed to lock during prepare: key=" + oKeyFailed);
                }
            }
        
        com.tangosol.util.TransactionMap.Validator validator = getValidator();
        if (validator != null)
            {
            try
                {
                validator.validate(this, setInsert, setUpdate, setDelete, setRead, setFanthom);
                }
            catch (RuntimeException e)
                {
                rollback();
                throw e;
                }
            }
        }
    
    // Accessor for the property "LockExternal"
    /**
     * Setter for property LockExternal.<p>
    * Indicates that the CONCUR_EXTERNAL concurrency is used.
     */
    public void setLockExternal(boolean fExternal)
        {
        __m_LockExternal = fExternal;
        }
    }
