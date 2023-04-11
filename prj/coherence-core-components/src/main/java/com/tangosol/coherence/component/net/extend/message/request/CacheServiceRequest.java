
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.message.request.CacheServiceRequest

package com.tangosol.coherence.component.net.extend.message.request;

/**
 * Base component for all CacheService Protocol Request messages.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class CacheServiceRequest
        extends    com.tangosol.coherence.component.net.extend.message.Request
    {
    // ---- Fields declarations ----
    
    /**
     * Property CacheName
     *
     * The name of the target NamedCache.
     */
    private String __m_CacheName;
    
    /**
     * Property CacheService
     *
     * The target of this CacheServiceRequest. This property must be set by the
     * Receiver before the run() method is called.
     */
    private transient com.tangosol.net.CacheService __m_CacheService;
    
    /**
     * Property LockEnabled
     *
     * If false, NamedCache lock or unlock operation will be prohibited.
     */
    private transient boolean __m_LockEnabled;
    
    /**
     * Property ReadOnly
     *
     * If true, any NamedCache operation that may potentially modify cached
     * entries will be prohibited.
     */
    private transient boolean __m_ReadOnly;
    
    /**
     * Property TransferThreshold
     *
     * The approximate maximum number of bytes transfered by a partial
     * response. Results that can be streamed, such as query requests, are
     * returned to the requestor as a sequence of response messages containing
     * a portion of the total result. Each of these response messages will be
     * approximately no larger than the configured size.
     * 
     * Default value is .5 MB.
     */
    private transient long __m_TransferThreshold;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Status", com.tangosol.coherence.component.net.extend.message.Request.Status.get_CLASS());
        }
    
    // Initializing constructor
    public CacheServiceRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/message/request/CacheServiceRequest".replace('/', '.'));
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
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    // Accessor for the property "CacheName"
    /**
     * Getter for property CacheName.<p>
    * The name of the target NamedCache.
     */
    public String getCacheName()
        {
        return __m_CacheName;
        }
    
    // Accessor for the property "CacheService"
    /**
     * Getter for property CacheService.<p>
    * The target of this CacheServiceRequest. This property must be set by the
    * Receiver before the run() method is called.
     */
    public com.tangosol.net.CacheService getCacheService()
        {
        return __m_CacheService;
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        return super.getDescription() + ", CacheName=" + getCacheName();
        }
    
    // Accessor for the property "TransferThreshold"
    /**
     * Getter for property TransferThreshold.<p>
    * The approximate maximum number of bytes transfered by a partial response.
    * Results that can be streamed, such as query requests, are returned to the
    * requestor as a sequence of response messages containing a portion of the
    * total result. Each of these response messages will be approximately no
    * larger than the configured size.
    * 
    * Default value is .5 MB.
     */
    public long getTransferThreshold()
        {
        return __m_TransferThreshold;
        }
    
    // Accessor for the property "LockEnabled"
    /**
     * Getter for property LockEnabled.<p>
    * If false, NamedCache lock or unlock operation will be prohibited.
     */
    public boolean isLockEnabled()
        {
        return __m_LockEnabled;
        }
    
    // Accessor for the property "ReadOnly"
    /**
     * Getter for property ReadOnly.<p>
    * If true, any NamedCache operation that may potentially modify cached
    * entries will be prohibited.
     */
    public boolean isReadOnly()
        {
        return __m_ReadOnly;
        }
    
    // Declared at the super level
    public void readExternal(com.tangosol.io.pof.PofReader in)
            throws java.io.IOException
        {
        super.readExternal(in);
        
        setCacheName(in.readString(1));
        }
    
    // Accessor for the property "CacheName"
    /**
     * Setter for property CacheName.<p>
    * The name of the target NamedCache.
     */
    public void setCacheName(String sName)
        {
        __m_CacheName = sName;
        }
    
    // Accessor for the property "CacheService"
    /**
     * Setter for property CacheService.<p>
    * The target of this CacheServiceRequest. This property must be set by the
    * Receiver before the run() method is called.
     */
    public void setCacheService(com.tangosol.net.CacheService service)
        {
        __m_CacheService = service;
        }
    
    // Accessor for the property "LockEnabled"
    /**
     * Setter for property LockEnabled.<p>
    * If false, NamedCache lock or unlock operation will be prohibited.
     */
    public void setLockEnabled(boolean fEnabled)
        {
        __m_LockEnabled = fEnabled;
        }
    
    // Accessor for the property "ReadOnly"
    /**
     * Setter for property ReadOnly.<p>
    * If true, any NamedCache operation that may potentially modify cached
    * entries will be prohibited.
     */
    public void setReadOnly(boolean fReadOnly)
        {
        __m_ReadOnly = fReadOnly;
        }
    
    // Accessor for the property "TransferThreshold"
    /**
     * Setter for property TransferThreshold.<p>
    * The approximate maximum number of bytes transfered by a partial response.
    * Results that can be streamed, such as query requests, are returned to the
    * requestor as a sequence of response messages containing a portion of the
    * total result. Each of these response messages will be approximately no
    * larger than the configured size.
    * 
    * Default value is .5 MB.
     */
    public void setTransferThreshold(long cb)
        {
        __m_TransferThreshold = cb;
        }
    
    // Declared at the super level
    public void writeExternal(com.tangosol.io.pof.PofWriter out)
            throws java.io.IOException
        {
        super.writeExternal(out);
        
        out.writeString(1, getCacheName());
        }
    }
