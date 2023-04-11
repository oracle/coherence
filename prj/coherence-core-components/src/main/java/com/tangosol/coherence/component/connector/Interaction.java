
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.connector.Interaction

package com.tangosol.coherence.component.connector;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.ResourceWarning;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Interaction
        extends    com.tangosol.coherence.component.Connector
        implements jakarta.resource.cci.Interaction
    {
    // ---- Fields declarations ----
    
    /**
     * Property Warnings
     *
     */
    private transient jakarta.resource.cci.ResourceWarning __m_Warnings;
    
    // Default constructor
    public Interaction()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Interaction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.connector.Interaction();
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
            clz = Class.forName("com.tangosol.coherence/component/connector/Interaction".replace('/', '.'));
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
    
    // From interface: jakarta.resource.cci.Interaction
    public void clearWarnings()
            throws jakarta.resource.ResourceException
        {
        setWarnings(null);
        }
    
    // From interface: jakarta.resource.cci.Interaction
    public void close()
            throws jakarta.resource.ResourceException
        {
        }
    
    // From interface: jakarta.resource.cci.Interaction
    public jakarta.resource.cci.Record execute(jakarta.resource.cci.InteractionSpec ispec, jakarta.resource.cci.Record input)
            throws jakarta.resource.ResourceException
        {
        // import jakarta.resource.NotSupportedException;
        
        throw new NotSupportedException("execute(ispec=" + ispec + ", input=" + input + ')');
        }
    
    // From interface: jakarta.resource.cci.Interaction
    public boolean execute(jakarta.resource.cci.InteractionSpec ispec, jakarta.resource.cci.Record input, jakarta.resource.cci.Record output)
            throws jakarta.resource.ResourceException
        {
        // import jakarta.resource.NotSupportedException;
        
        throw new NotSupportedException("execute(ispec=" + ispec + ", input=" + input +
            ", output=" + output + ')');
        }
    
    // From interface: jakarta.resource.cci.Interaction
    // Accessor for the property "Connection"
    /**
     * Getter for property Connection.<p>
     */
    public jakarta.resource.cci.Connection getConnection()
        {
        return null;
        }
    
    // From interface: jakarta.resource.cci.Interaction
    // Accessor for the property "Warnings"
    /**
     * Getter for property Warnings.<p>
     */
    public jakarta.resource.cci.ResourceWarning getWarnings()
            throws jakarta.resource.ResourceException
        {
        return __m_Warnings;
        }
    
    // Accessor for the property "Warnings"
    /**
     * Setter for property Warnings.<p>
     */
    public void setWarnings(jakarta.resource.cci.ResourceWarning warning)
        {
        // import jakarta.resource.cci.ResourceWarning;
        // import jakarta.resource.ResourceException;
        
        if (warning != null)
            {
            try
                {
                ResourceWarning warningOrig = getWarnings();
                if (warningOrig != null)
                    {
                    warningOrig.setLinkedWarning(warning);
                    return;
                    }
                }
            catch (ResourceException e) {}
            }
        
        __m_Warnings = (warning);
        }
    }
