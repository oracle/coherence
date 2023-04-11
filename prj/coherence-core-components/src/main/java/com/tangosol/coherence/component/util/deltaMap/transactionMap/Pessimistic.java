
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.deltaMap.transactionMap.Pessimistic

package com.tangosol.coherence.component.util.deltaMap.transactionMap;

import com.tangosol.coherence.config.Config;
import com.tangosol.util.ConcurrentMap;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
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
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Pessimistic
        extends    com.tangosol.coherence.component.util.deltaMap.TransactionMap
    {
    // ---- Fields declarations ----
    
    /**
     * Property ValidateLocks
     *
     * Specifies whether or not locks should be validated during prepare()
     */
    private boolean __m_ValidateLocks;
    
    // Initializing constructor
    public Pessimistic(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/deltaMap/transactionMap/Pessimistic".replace('/', '.'));
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
        // import java.util.ConcurrentModificationException;
        // import java.util.Set;
        
        // all writes require locking
        if (fWrite)
            {
            Set setLock = getBaseLockSet();
        
            if (!setLock.contains(oKey))
                {
                if (getBaseMap().lock(oKey, getLockWaitMillis()))
                    {
                    setLock.add(oKey);
                    }
                else
                    {
                    throw new ConcurrentModificationException("Failed to lock: key=" + oKey);
                    }
                }
            }
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
        // import com.tangosol.util.ConcurrentMap;
        // import java.util.ArrayList;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.Set;
        
        if (fWrite)
            {
            ConcurrentMap mapBase    = getBaseMap();
            Set           setLock    = getBaseLockSet();
            ArrayList     listLocked = new ArrayList();
            Object        oKeyFailed = null;
        
            for (Iterator iter = keySetImpl().iterator(); iter.hasNext();)
                {
                Object oKey = iter.next();
        
                if (!setLock.contains(oKey))
                    {
                    if (mapBase.lock(oKey, getLockWaitMillis()))
                        {
                        listLocked.add(oKey);
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
                for (Iterator iter = listLocked.iterator(); iter.hasNext();)
                    {
                    Object oKey = iter.next();
        
                    mapBase.unlock(oKey);
                    }
                throw new ConcurrentModificationException("Failed to lock: key=" + oKeyFailed);
                }
        
            setLock.addAll(listLocked);
            }
        }
    
    // Accessor for the property "ValidateLocks"
    /**
     * Getter for property ValidateLocks.<p>
    * Specifies whether or not locks should be validated during prepare()
     */
    protected boolean isValidateLocks()
        {
        return __m_ValidateLocks;
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
        // import com.tangosol.coherence.config.Config;
        
        setValidateLocks(Config.getBoolean(
            "coherence.transaction.pessimistic.validatelocks"));
        
        super.onInit();
        }
    
    // Declared at the super level
    public void prepare()
        {
        // import com.tangosol.util.ConcurrentMap;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        // import java.util.Set;
        
        super.prepare();
        
        boolean fValidateLocks = isValidateLocks(); // not necessary if locks are not expirable
        if (fValidateLocks)
            {
            ConcurrentMap mapBase = getBaseMap();
            Set           setLock = getBaseLockSet();
        
            for (Iterator iter = setLock.iterator(); iter.hasNext();)
                {
                Object oKey = iter.next();
        
                if (!mapBase.lock(oKey))
                    {
                    throw new ConcurrentModificationException("Lock has expired: key=" + oKey);
                    }
                }
            }
        }
    
    // Accessor for the property "ValidateLocks"
    /**
     * Setter for property ValidateLocks.<p>
    * Specifies whether or not locks should be validated during prepare()
     */
    protected void setValidateLocks(boolean fValidate)
        {
        __m_ValidateLocks = fValidate;
        }
    }
