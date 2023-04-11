
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.connector.InteractionSpec

package com.tangosol.coherence.component.connector;

/**
 * @see jakarta.resource.cci.InteractionSpec
 * @see Chapter 9.6.2 JCA 1.0 specification
 */
/*
* Integrates
*     jakarta.resource.cci.InteractionSpec
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class InteractionSpec
        extends    com.tangosol.coherence.component.Connector
        implements jakarta.resource.cci.InteractionSpec
    {
    // ---- Fields declarations ----
    
    /**
     * Property ExecutionTimeout
     *
     * The number of milliseconds an Interaction will wait for an EIS to
     * execute the specified function 
     */
    private int __m_ExecutionTimeout;
    
    /**
     * Property FetchDirection
     *
     */
    private int __m_FetchDirection;
    
    /**
     * Property FetchSize
     *
     */
    private int __m_FetchSize;
    
    /**
     * Property FunctionName
     *
     * Name of an EIS function 
     */
    private String __m_FunctionName;
    
    /**
     * Property InteractionVerb
     *
     * One of SYNC_SEND, SYNC_SEND_RECEIVE,  SYNC_RECEIVE 
     */
    private int __m_InteractionVerb;
    
    /**
     * Property MaxFieldSize
     *
     */
    private int __m_MaxFieldSize;
    
    /**
     * Property ResultSetConcurrency
     *
     */
    private int __m_ResultSetConcurrency;
    
    /**
     * Property ResultSetType
     *
     */
    private int __m_ResultSetType;
    
    /**
     * Property SYNC_RECEIVE
     *
     */
    public static final int SYNC_RECEIVE = 2; // jakarta.resource.cci.InteractionSpec.SYNC_RECEIVE;
    
    /**
     * Property SYNC_SEND
     *
     */
    public static final int SYNC_SEND = 0; // jakarta.resource.cci.InteractionSpec.SYNC_SEND;
    
    /**
     * Property SYNC_SEND_RECEIVE
     *
     */
    public static final int SYNC_SEND_RECEIVE = 1; // jakarta.resource.cci.InteractionSpec.SYNC_SEND_RECEIVE;
    
    // Default constructor
    public InteractionSpec()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public InteractionSpec(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.connector.InteractionSpec();
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
            clz = Class.forName("com.tangosol.coherence/component/connector/InteractionSpec".replace('/', '.'));
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
    
    //++ jakarta.resource.cci.InteractionSpec integration
    // Access optimization
    // properties integration
    // methods integration
    //-- jakarta.resource.cci.InteractionSpec integration
    
    // Accessor for the property "ExecutionTimeout"
    /**
     * Getter for property ExecutionTimeout.<p>
    * The number of milliseconds an Interaction will wait for an EIS to execute
    * the specified function 
     */
    public int getExecutionTimeout()
        {
        return __m_ExecutionTimeout;
        }
    
    // Accessor for the property "FetchDirection"
    /**
     * Getter for property FetchDirection.<p>
     */
    public int getFetchDirection()
        {
        return __m_FetchDirection;
        }
    
    // Accessor for the property "FetchSize"
    /**
     * Getter for property FetchSize.<p>
     */
    public int getFetchSize()
        {
        return __m_FetchSize;
        }
    
    // Accessor for the property "FunctionName"
    /**
     * Getter for property FunctionName.<p>
    * Name of an EIS function 
     */
    public String getFunctionName()
        {
        return __m_FunctionName;
        }
    
    // Accessor for the property "InteractionVerb"
    /**
     * Getter for property InteractionVerb.<p>
    * One of SYNC_SEND, SYNC_SEND_RECEIVE,  SYNC_RECEIVE 
     */
    public int getInteractionVerb()
        {
        return __m_InteractionVerb;
        }
    
    // Accessor for the property "MaxFieldSize"
    /**
     * Getter for property MaxFieldSize.<p>
     */
    public int getMaxFieldSize()
        {
        return __m_MaxFieldSize;
        }
    
    // Accessor for the property "ResultSetConcurrency"
    /**
     * Getter for property ResultSetConcurrency.<p>
     */
    public int getResultSetConcurrency()
        {
        return __m_ResultSetConcurrency;
        }
    
    // Accessor for the property "ResultSetType"
    /**
     * Getter for property ResultSetType.<p>
     */
    public int getResultSetType()
        {
        return __m_ResultSetType;
        }
    
    // Accessor for the property "ExecutionTimeout"
    /**
     * Setter for property ExecutionTimeout.<p>
    * The number of milliseconds an Interaction will wait for an EIS to execute
    * the specified function 
     */
    public void setExecutionTimeout(int nTimeout)
        {
        __m_ExecutionTimeout = nTimeout;
        }
    
    // Accessor for the property "FetchDirection"
    /**
     * Setter for property FetchDirection.<p>
     */
    public void setFetchDirection(int iDirection)
        {
        __m_FetchDirection = iDirection;
        }
    
    // Accessor for the property "FetchSize"
    /**
     * Setter for property FetchSize.<p>
     */
    public void setFetchSize(int nSize)
        {
        __m_FetchSize = nSize;
        }
    
    // Accessor for the property "FunctionName"
    /**
     * Setter for property FunctionName.<p>
    * Name of an EIS function 
     */
    public void setFunctionName(String sFunction)
        {
        __m_FunctionName = sFunction;
        }
    
    // Accessor for the property "InteractionVerb"
    /**
     * Setter for property InteractionVerb.<p>
    * One of SYNC_SEND, SYNC_SEND_RECEIVE,  SYNC_RECEIVE 
     */
    public void setInteractionVerb(int iVerb)
        {
        __m_InteractionVerb = iVerb;
        }
    
    // Accessor for the property "MaxFieldSize"
    /**
     * Setter for property MaxFieldSize.<p>
     */
    public void setMaxFieldSize(int nSize)
        {
        __m_MaxFieldSize = nSize;
        }
    
    // Accessor for the property "ResultSetConcurrency"
    /**
     * Setter for property ResultSetConcurrency.<p>
     */
    public void setResultSetConcurrency(int nConcurrency)
        {
        __m_ResultSetConcurrency = nConcurrency;
        }
    
    // Accessor for the property "ResultSetType"
    /**
     * Setter for property ResultSetType.<p>
     */
    public void setResultSetType(int nType)
        {
        __m_ResultSetType = nType;
        }
    }
