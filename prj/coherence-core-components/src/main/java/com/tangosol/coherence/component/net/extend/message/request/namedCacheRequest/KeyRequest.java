
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest.KeyRequest

package com.tangosol.coherence.component.net.extend.message.request.namedCacheRequest;

/**
 * Base component for all NamedCache Protocol Request messages that include a
 * key.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class KeyRequest
        extends    com.tangosol.coherence.component.net.extend.message.request.NamedCacheRequest
        implements com.tangosol.net.cache.KeyAssociation
    {
    // ---- Fields declarations ----
    
    /**
     * Property Key
     *
     * The key associated with this KeyRequest.
     */
    private Object __m_Key;
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
    public KeyRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/message/request/namedCacheRequest/KeyRequest".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.cache.KeyAssociation
    public Object getAssociatedKey()
        {
        // COH-10721: no need to have association for anything, but key-based listener requests
        // (see NamedCacheFactory$ListenerKeyRequest)
        return null;
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        return super.getDescription() + ", Key=" + getKey();
        }
    
    // Accessor for the property "Key"
    /**
     * Getter for property Key.<p>
    * The key associated with this KeyRequest.
     */
    public Object getKey()
        {
        return __m_Key;
        }
    
    // Declared at the super level
    public void readExternal(com.tangosol.io.pof.PofReader in)
            throws java.io.IOException
        {
        super.readExternal(in);
        
        setKey(in.readObject(1));
        }
    
    // Accessor for the property "Key"
    /**
     * Setter for property Key.<p>
    * The key associated with this KeyRequest.
     */
    public void setKey(Object oKey)
        {
        __m_Key = oKey;
        }
    
    // Declared at the super level
    public void writeExternal(com.tangosol.io.pof.PofWriter out)
            throws java.io.IOException
        {
        super.writeExternal(out);
        
        out.writeObject(1, getKey());
        
        // release state
        setKey(null);
        }
    }
