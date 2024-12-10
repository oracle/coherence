
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.util.JndiUtil

package com.tangosol.coherence.component.net.extend.util;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A collection of JNDI-related utility methods.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class JndiUtil
        extends    com.tangosol.coherence.component.net.extend.Util
    {
    // ---- Fields declarations ----
    
    // Initializing constructor
    public JndiUtil(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/util/JndiUtil".replace('/', '.'));
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
     * Close the given InitialContext. If the InitialContext is closed
    * successfully, this method returns true; otherwise, this method returns
    * false.
    * 
    * @param ctx  the InitialContext to close; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean close(javax.naming.InitialContext ctx)
        {
        // import javax.naming.NamingException;
        
        if (ctx != null)
            {
            try
                {
                ctx.close();
                return true;
                }
            catch (NamingException e) {}
            }
        return false;
        }
    
    /**
     * Retrieve an object from JNDI using the specified JNDI name and narrow the
    * reference to the given type.
    * 
    * This method retrieves the object using a new InitialContext instance.
    * 
    * @param sName  the name to resolve
    * @param clz        the type to narrow the resolved reference to
    * 
    * @param the resolved Object with the given JNDI name
     */
    public static Object lookup(String sName, Class clz)
            throws javax.naming.NamingException
        {
        // import javax.naming.InitialContext;
        
        InitialContext ctx = new InitialContext();
        try
            {
            return lookup(ctx, sName, clz);
            }
        finally
            {
            close(ctx);
            }
        }
    
    /**
     * Retrieve an object from JNDI using the specified InitialContext and JNDI
    * name and narrow the reference to the given type.
    * 
    * @param ctx        the InitialContext used to resolve the given JNDI name
    * @param sName  the name to resolve
    * @param clz        the type to narrow the resolved reference to
    * 
    * @param the resolved Object with the given JNDI name
     */
    public static Object lookup(javax.naming.InitialContext ctx, String sName, Class clz)
            throws javax.naming.NamingException
        {
        if (ctx == null)
            {
            return lookup(sName, clz);
            }
        
        Object object = ctx.lookup(sName);
        if (!clz.isAssignableFrom(object.getClass()))
            {
            throw new IllegalArgumentException("the object retrieved from JNDI using"
                    + " the name \"" + sName + "\" is not an instance of "
                    + object.getClass().getName());
            }
        
        return object;
        }
    }
