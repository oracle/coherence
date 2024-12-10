
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.deltaMap.transactionMap.optimistic.Repeatable

package com.tangosol.coherence.component.util.deltaMap.transactionMap.optimistic;

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
public class Repeatable
        extends    com.tangosol.coherence.component.util.deltaMap.transactionMap.Optimistic
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public Repeatable()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Repeatable(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setBaseLockSet(new com.tangosol.util.LiteSet());
            setConcurrency(2);
            setLockWaitMillis(0L);
            setRepeatableRead(true);
            setTransactionIsolation(2);
            setTransactionTimeout(0);
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
        return new com.tangosol.coherence.component.util.deltaMap.transactionMap.optimistic.Repeatable();
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
            clz = Class.forName("com.tangosol.coherence/component/util/deltaMap/transactionMap/optimistic/Repeatable".replace('/', '.'));
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
        if (!isRead(oKey))
            {
            super.enlist(oKey, true);
            }
        }
    }
