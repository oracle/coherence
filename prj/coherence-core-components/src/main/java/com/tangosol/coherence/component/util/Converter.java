
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.Converter

package com.tangosol.coherence.component.util;

/**
 * An abstract base for Serializer-based Converter implementations.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Converter
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.util.Converter
    {
    // ---- Fields declarations ----
    
    /**
     * Property Serializer
     *
     * A Serializer used by this Converter.
     */
    private transient com.tangosol.io.Serializer __m_Serializer;
    
    // Initializing constructor
    public Converter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/Converter".replace('/', '.'));
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
    
    // From interface: com.tangosol.util.Converter
    public Object convert(Object o)
        {
        return null;
        }
    
    // Accessor for the property "Serializer"
    /**
     * Getter for property Serializer.<p>
    * A Serializer used by this Converter.
     */
    public com.tangosol.io.Serializer getSerializer()
        {
        return __m_Serializer;
        }
    
    // Accessor for the property "Serializer"
    /**
     * Setter for property Serializer.<p>
    * A Serializer used by this Converter.
     */
    public void setSerializer(com.tangosol.io.Serializer serializer)
        {
        __m_Serializer = serializer;
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + "@" + hashCode();
        }
    }
