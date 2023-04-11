
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.connector.connectionInfo.CacheInfo

package com.tangosol.coherence.component.connector.connectionInfo;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

/**
 * @see jakarta.resource.cci.ConnectionSpec
 * @see Chapter 9.5.2 JCA 1.0 specification
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class CacheInfo
        extends    com.tangosol.coherence.component.connector.ConnectionInfo
    {
    // ---- Fields declarations ----
    
    /**
     * Property Concurrency
     *
     *  Default transaction concurrency
     */
    private transient int __m_Concurrency;
    
    /**
     * Property Isolation
     *
     * Default transaction isolation level
     */
    private transient int __m_Isolation;
    
    /**
     * Property ServiceName
     *
     */
    private String __m_ServiceName;
    
    /**
     * Property ServiceType
     *
     */
    private String __m_ServiceType;
    
    /**
     * Property Timeout
     *
     * Default transaction timeout value (in seconds)
     */
    private transient int __m_Timeout;
    
    // Default constructor
    public CacheInfo()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public CacheInfo(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.connector.connectionInfo.CacheInfo();
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
            clz = Class.forName("com.tangosol.coherence/component/connector/connectionInfo/CacheInfo".replace('/', '.'));
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
    public boolean equals(Object obj)
        {
        // import com.tangosol.util.Base;
        
        if (obj instanceof CacheInfo)
            {
            CacheInfo infoThis = this;
            CacheInfo infoThat = (CacheInfo) obj;
        
            return super.equals(obj) &&
                Base.equals(infoThis.getServiceName(), infoThat.getServiceName());
            }
        return false;
        }
    
    // Declared at the super level
    public void fromConnectionSpec(jakarta.resource.cci.ConnectionSpec properties)
        {
        // import com.tangosol.util.ClassHelper;
        
        super.fromConnectionSpec(properties);
        
        if (properties instanceof CacheInfo)
            {
            CacheInfo infoThat = (CacheInfo) properties;
            setServiceName(infoThat.getServiceName());
            setServiceType(infoThat.getServiceType());
            setConcurrency(infoThat.getConcurrency());
            setIsolation  (infoThat.getIsolation());
            setTimeout    (infoThat.getTimeout());
            }
        else
            {
            try
                {
                setServiceName((String) ClassHelper.invoke(properties,
                    "getServiceName", ClassHelper.VOID));
                setServiceType((String) ClassHelper.invoke(properties,
                    "getServiceType", ClassHelper.VOID));
                }
            catch (Exception e)
                {
                throw new IllegalArgumentException(
                    "Illegal ConnectionSpec: " + properties + ", reason=" + e);
                }
        
            try
                {
                setConcurrency(((Integer) ClassHelper.invoke(properties,
                    "getConcurrency", ClassHelper.VOID)).intValue());
                }
            catch (Exception e) {}
        
            try
                {
                setIsolation(((Integer) ClassHelper.invoke(properties,
                    "getIsolation", ClassHelper.VOID)).intValue());
                }
            catch (Exception e) {}
        
             try
                {
                setTimeout(((Integer) ClassHelper.invoke(properties,
                    "getTimeout", ClassHelper.VOID)).intValue());
                }
            catch (Exception e) {}
            }
        }
    
    // Accessor for the property "Concurrency"
    /**
     * Getter for property Concurrency.<p>
    *  Default transaction concurrency
     */
    public int getConcurrency()
        {
        return __m_Concurrency;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
     */
    public String getDescription()
        {
        return super.getDescription() + ", ServiceName=" + getServiceName();
        }
    
    // Accessor for the property "Isolation"
    /**
     * Getter for property Isolation.<p>
    * Default transaction isolation level
     */
    public int getIsolation()
        {
        return __m_Isolation;
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Getter for property ServiceName.<p>
     */
    public String getServiceName()
        {
        return __m_ServiceName;
        }
    
    // Accessor for the property "ServiceType"
    /**
     * Getter for property ServiceType.<p>
     */
    public String getServiceType()
        {
        return __m_ServiceType;
        }
    
    // Accessor for the property "Timeout"
    /**
     * Getter for property Timeout.<p>
    * Default transaction timeout value (in seconds)
     */
    public int getTimeout()
        {
        return __m_Timeout;
        }
    
    // Declared at the super level
    public int hashCode()
        {
        int    iHash        = super.hashCode();
        String sServiceName = getServiceName();
        if (sServiceName != null)
            {
            iHash += sServiceName.hashCode();
            }
        return iHash;
        }
    
    // Accessor for the property "Concurrency"
    /**
     * Setter for property Concurrency.<p>
    *  Default transaction concurrency
     */
    public void setConcurrency(int nConcurrency)
        {
        __m_Concurrency = nConcurrency;
        }
    
    // Accessor for the property "Isolation"
    /**
     * Setter for property Isolation.<p>
    * Default transaction isolation level
     */
    public void setIsolation(int pIsolation)
        {
        __m_Isolation = pIsolation;
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Setter for property ServiceName.<p>
     */
    public void setServiceName(String sName)
        {
        __m_ServiceName = sName;
        }
    
    // Accessor for the property "ServiceType"
    /**
     * Setter for property ServiceType.<p>
     */
    public void setServiceType(String sType)
        {
        __m_ServiceType = sType;
        }
    
    // Accessor for the property "Timeout"
    /**
     * Setter for property Timeout.<p>
    * Default transaction timeout value (in seconds)
     */
    public void setTimeout(int nTimeout)
        {
        __m_Timeout = nTimeout;
        }
    }
