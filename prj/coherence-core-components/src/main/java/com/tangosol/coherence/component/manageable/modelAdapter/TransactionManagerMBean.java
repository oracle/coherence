
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.TransactionManagerMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

/**
 * The TransactionManagerMBean contains statistics for all transaction related
 * information managed for the local member.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class TransactionManagerMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public TransactionManagerMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public TransactionManagerMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.TransactionManagerMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/TransactionManagerMBean".replace('/', '.'));
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
    /**
     * Auto-generated for concrete Components.
     */
    protected Object[] get_ComponentInfo()
        {
        return new Object[]
            {
            "The TransactionManagerMBean contains statistics for all transaction related information managed for the local member.",
            null,
            };
        }
    /**
     * Auto-generated for concrete Components, for Properties that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - at least one public accessor
     */
    protected java.util.Map get_PropertyInfo()
        {
        java.util.Map mapInfo = super.get_PropertyInfo();
        
        // property CommitTotalMillis
            {
            mapInfo.put("CommitTotalMillis", new Object[]
                {
                "The cumulative time (in milliseconds) spent during the commit phase since the last time statistics were reset.",
                "getCommitTotalMillis",
                null,
                "J",
                null,
                });
            }
        
        // property TimeoutMillis
            {
            mapInfo.put("TimeoutMillis", new Object[]
                {
                "The transaction timeout value in milliseconds.  Note that this value will only apply to transactional connections obtained after the value is set.",
                "getTimeoutMillis",
                null,
                "J",
                null,
                });
            }
        
        // property TotalActive
            {
            mapInfo.put("TotalActive", new Object[]
                {
                "The total number of currently active transactions. An active transaction is counted as any transaction that contains at least one modified entry and has yet to be committed or rolled back.   Note that the count is maintained at the coordinator node for the transaction, even though multiple nodes may have participated in the transaction.",
                "getTotalActive",
                null,
                "J",
                null,
                });
            }
        
        // property TotalCommitted
            {
            mapInfo.put("TotalCommitted", new Object[]
                {
                "The total number of transactions that have been committed by the Transaction Manager since the last time the statistics were reset. Note that the count is maintained at the coordinator node for the transaction being committed, even though multiple nodes may have participated in the transaction.",
                "getTotalCommitted",
                null,
                "J",
                null,
                });
            }
        
        // property TotalRecovered
            {
            mapInfo.put("TotalRecovered", new Object[]
                {
                "The total number of transactions that have been recovered by the Transaction Manager since the last time the statistics were reset. Note that the count is maintained at the coordinator node for the transaction being recovered, even though multiple nodes may have participated in the transaction.",
                "getTotalRecovered",
                null,
                "J",
                null,
                });
            }
        
        // property TotalRolledback
            {
            mapInfo.put("TotalRolledback", new Object[]
                {
                "The total number of transactions that have been rolled back by the Transaction Manager since the last time the statistics were reset. Note that the count is maintained at the coordinator node for the transaction being rolled back, even though multiple nodes may have participated in the transaction.",
                "getTotalRolledback",
                null,
                "J",
                null,
                });
            }
        
        // property TransactionTotalMillis
            {
            mapInfo.put("TransactionTotalMillis", new Object[]
                {
                "The cumulative time (in milliseconds) spent on active transactions since the last time the statistics were reset.",
                "getTransactionTotalMillis",
                null,
                "J",
                null,
                });
            }
        
        return mapInfo;
        }
    /**
     * Auto-generated for concrete Components, for Behaviors that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - public access
     */
    protected java.util.Map get_MethodInfo()
        {
        java.util.Map mapInfo = super.get_MethodInfo();
        
        // behavior resetStatistics()
            {
            mapInfo.put("resetStatistics()", new Object[]
                {
                "Reset the transaction statistics.",
                "resetStatistics",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        return mapInfo;
        }
    
    // Accessor for the property "CommitTotalMillis"
    /**
     * Getter for property CommitTotalMillis.<p>
    * The cumulative time (in milliseconds) spent during the commit phase since
    * the last time statistics were reset.
     */
    public long getCommitTotalMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "TimeoutMillis"
    /**
     * Getter for property TimeoutMillis.<p>
    * The transaction timeout value in milliseconds.  Note that this value will
    * only apply to transactional connections obtained after the value is set.
     */
    public long getTimeoutMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalActive"
    /**
     * Getter for property TotalActive.<p>
    * The total number of currently active transactions. An active transaction
    * is counted as any transaction that contains at least one modified entry
    * and has yet to be committed or rolled back.   Note that the count is
    * maintained at the coordinator node for the transaction, even though
    * multiple nodes may have participated in the transaction.
     */
    public long getTotalActive()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalCommitted"
    /**
     * Getter for property TotalCommitted.<p>
    * The total number of transactions that have been committed by the
    * Transaction Manager since the last time the statistics were reset. Note
    * that the count is maintained at the coordinator node for the transaction
    * being committed, even though multiple nodes may have participated in the
    * transaction.
     */
    public long getTotalCommitted()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalRecovered"
    /**
     * Getter for property TotalRecovered.<p>
    * The total number of transactions that have been recovered by the
    * Transaction Manager since the last time the statistics were reset. Note
    * that the count is maintained at the coordinator node for the transaction
    * being recovered, even though multiple nodes may have participated in the
    * transaction.
     */
    public long getTotalRecovered()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalRolledback"
    /**
     * Getter for property TotalRolledback.<p>
    * The total number of transactions that have been rolled back by the
    * Transaction Manager since the last time the statistics were reset. Note
    * that the count is maintained at the coordinator node for the transaction
    * being rolled back, even though multiple nodes may have participated in
    * the transaction.
     */
    public long getTotalRolledback()
        {
        return 0L;
        }
    
    // Accessor for the property "TransactionTotalMillis"
    /**
     * Getter for property TransactionTotalMillis.<p>
    * The cumulative time (in milliseconds) spent on active transactions since
    * the last time the statistics were reset.
     */
    public long getTransactionTotalMillis()
        {
        return 0L;
        }
    
    /**
     * Reset the transaction statistics.
     */
    public void resetStatistics()
        {
        }
    }
