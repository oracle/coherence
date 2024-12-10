
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.message.response.PartialResponse

package com.tangosol.coherence.component.net.extend.message.response;

/**
 * Abstract com.tangosol.net.messaging.Response implementation that carries a
 * partial result.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class PartialResponse
        extends    com.tangosol.coherence.component.net.extend.message.Response
    {
    // ---- Fields declarations ----
    
    /**
     * Property Cookie
     *
     * Opaque cookie used to support streaming.
     * 
     * If non-null, this PartialResponse contains a partial result. The
     * receiver of a PartialResponse can accumulate or iterate the entire
     * result by sending additional Request(s) until this property is null.
     */
    private com.tangosol.util.Binary __m_Cookie;
    
    // Initializing constructor
    public PartialResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/message/response/PartialResponse".replace('/', '.'));
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
    
    // Accessor for the property "Cookie"
    /**
     * Getter for property Cookie.<p>
    * Opaque cookie used to support streaming.
    * 
    * If non-null, this PartialResponse contains a partial result. The receiver
    * of a PartialResponse can accumulate or iterate the entire result by
    * sending additional Request(s) until this property is null.
     */
    public com.tangosol.util.Binary getCookie()
        {
        return __m_Cookie;
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        return super.getDescription() + ", Cookie=" + getCookie();
        }
    
    // Declared at the super level
    public void readExternal(com.tangosol.io.pof.PofReader in)
            throws java.io.IOException
        {
        super.readExternal(in);
        
        setCookie(in.readBinary(6));
        }
    
    // Accessor for the property "Cookie"
    /**
     * Setter for property Cookie.<p>
    * Opaque cookie used to support streaming.
    * 
    * If non-null, this PartialResponse contains a partial result. The receiver
    * of a PartialResponse can accumulate or iterate the entire result by
    * sending additional Request(s) until this property is null.
     */
    public void setCookie(com.tangosol.util.Binary bin)
        {
        __m_Cookie = bin;
        }
    
    // Declared at the super level
    public void writeExternal(com.tangosol.io.pof.PofWriter out)
            throws java.io.IOException
        {
        super.writeExternal(out);
        
        out.writeBinary(6, getCookie());
        }
    }
