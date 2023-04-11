
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.Extend

package com.tangosol.coherence.component.net;

import com.tangosol.util.Base;

/**
 * Base component for all Coherence*Extend implementation components.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Extend
        extends    com.tangosol.coherence.component.Net
    {
    // ---- Fields declarations ----
    
    // Initializing constructor
    public Extend(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/Extend".replace('/', '.'));
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
     * @see com.tangosol.util.Base#ensureRuntimeException()
     */
    public static RuntimeException ensureRuntimeException(Throwable t)
        {
        // import com.tangosol.util.Base;
        
        if (t instanceof Error)
            {
            throw ((Error) t);
            }
        return Base.ensureRuntimeException(t);
        }
    
    /**
     * @see com.tangosol.util.Base#ensureRuntimeException()
     */
    public static RuntimeException ensureRuntimeException(Throwable t, String s)
        {
        // import com.tangosol.util.Base;
        
        if (t instanceof Error)
            {
            throw ((Error) t);
            }
        return Base.ensureRuntimeException(t, s);
        }
    
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        return "";
        }
    
    // Declared at the super level
    public String toString()
        {
        // import com.tangosol.util.Base;
        
        StringBuilder sb = new StringBuilder(get_Name());
        
        String s;
        try
            {
            s = getDescription();
            }
        catch (Throwable e)
            {
            // see COH-5386
            s = null;
            }
        
        if (s == null || s.length() == 0)
            {
            sb.append('@').append(hashCode());
            }
        else
            {
            if (s.charAt(0) == '\n')
                {
                sb.append(Base.indentString(s, "  "));
                }
            else
                {
                sb.append('(').append(s).append(')');
                }
            }
        
        return sb.toString();
        }
    }
