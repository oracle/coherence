
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.connector.ConnectionInfo

package com.tangosol.coherence.component.connector;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

/**
 * @see jakarta.resource.cci.ConnectionSpec
 * @see Chapter 9.5.2 JCA 1.0 specification
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ConnectionInfo
        extends    com.tangosol.coherence.component.Connector
        implements jakarta.resource.cci.ConnectionSpec,
                   jakarta.resource.spi.ConnectionRequestInfo
    {
    // ---- Fields declarations ----
    
    /**
     * Property Password
     *
     */
    private String __m_Password;
    
    /**
     * Property UserName
     *
     */
    private String __m_UserName;
    
    // Default constructor
    public ConnectionInfo()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ConnectionInfo(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.connector.ConnectionInfo();
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
            clz = Class.forName("com.tangosol.coherence/component/connector/ConnectionInfo".replace('/', '.'));
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
    
    // From interface: jakarta.resource.spi.ConnectionRequestInfo
    // Declared at the super level
    public boolean equals(Object obj)
        {
        // import com.tangosol.util.Base;
        
        if (obj instanceof ConnectionInfo)
            {
            ConnectionInfo infoThis = this;
            ConnectionInfo infoThat = (ConnectionInfo) obj;
            return Base.equals(infoThis.getUserName(), infoThat.getUserName());
            }
        return false;
        }
    
    public void fromConnectionSpec(jakarta.resource.cci.ConnectionSpec properties)
        {
        // import com.tangosol.util.ClassHelper;
        
        if (properties instanceof ConnectionInfo)
            {
            ConnectionInfo infoThat = (ConnectionInfo) properties;
            setUserName(infoThat.getUserName());
            setPassword(infoThat.getPassword());
            }
        else
            {
            // see section 9.5.2 of JCA 1.0 specification
            try
                {
                String sUserName = (String) ClassHelper.invoke(properties,
                    "getUserName", ClassHelper.VOID);
                    
                String sPassword = (String) ClassHelper.invoke(properties,
                    "getPassword", ClassHelper.VOID);
        
                setUserName(sUserName);
                setPassword(sPassword);
                }
            catch (Exception e)
                {
                throw new IllegalArgumentException(
                    "Illegal ConnectionSpec: " + properties + ", reason: " + e);
                }
            }
        }
    
    // Accessor for the property "Description"
    /**
     * Getter for property Description.<p>
     */
    public String getDescription()
        {
        return "UserName=" + getUserName();
        }
    
    // Accessor for the property "Password"
    /**
     * Getter for property Password.<p>
     */
    public String getPassword()
        {
        return __m_Password;
        }
    
    // Accessor for the property "UserName"
    /**
     * Getter for property UserName.<p>
     */
    public String getUserName()
        {
        return __m_UserName;
        }
    
    // From interface: jakarta.resource.spi.ConnectionRequestInfo
    // Declared at the super level
    public int hashCode()
        {
        String sUserName = getUserName();
        return sUserName == null ? 0 : sUserName.hashCode();
        }
    
    // Accessor for the property "Password"
    /**
     * Setter for property Password.<p>
     */
    public void setPassword(String sPassword)
        {
        __m_Password = sPassword;
        }
    
    // Accessor for the property "UserName"
    /**
     * Setter for property UserName.<p>
     */
    public void setUserName(String sUserName)
        {
        __m_UserName = sUserName;
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + '[' + getDescription() + ']';
        }
    }
