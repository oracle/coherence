
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.TransactionValidator

package com.tangosol.coherence.component.util;

/**
 * The abstract base component for TransactionMap$Validator interface
 * implementations.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class TransactionValidator
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.util.TransactionMap.Validator
    {
    // ---- Fields declarations ----
    
    /**
     * Property NextValidator
     *
     */
    private transient com.tangosol.util.TransactionMap.Validator __m_NextValidator;
    
    // Initializing constructor
    public TransactionValidator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/TransactionValidator".replace('/', '.'));
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
    
    // From interface: com.tangosol.util.TransactionMap$Validator
    public void enlist(com.tangosol.util.TransactionMap map, Object oKey)
        {
        // import com.tangosol.util.TransactionMap$Validator as com.tangosol.util.TransactionMap.Validator;
        
        com.tangosol.util.TransactionMap.Validator validatorNext = getNextValidator();
        if (validatorNext != null)
            {
            validatorNext.enlist(map, oKey);
            }
        }
    
    // From interface: com.tangosol.util.TransactionMap$Validator
    // Accessor for the property "NextValidator"
    /**
     * Getter for property NextValidator.<p>
     */
    public com.tangosol.util.TransactionMap.Validator getNextValidator()
        {
        return __m_NextValidator;
        }
    
    // From interface: com.tangosol.util.TransactionMap$Validator
    // Accessor for the property "NextValidator"
    /**
     * Setter for property NextValidator.<p>
     */
    public void setNextValidator(com.tangosol.util.TransactionMap.Validator validator)
        {
        __m_NextValidator = validator;
        }
    
    // From interface: com.tangosol.util.TransactionMap$Validator
    public void validate(com.tangosol.util.TransactionMap map, java.util.Set setInsert, java.util.Set setUpdate, java.util.Set setDelete, java.util.Set setRead, java.util.Set setFanthom)
            throws java.util.ConcurrentModificationException
        {
        // import com.tangosol.util.TransactionMap$Validator as com.tangosol.util.TransactionMap.Validator;
        
        com.tangosol.util.TransactionMap.Validator validatorNext = getNextValidator();
        if (validatorNext != null)
            {
            validatorNext.validate(map, setInsert, setUpdate, setDelete, setRead, setFanthom);
            }
        }
    }
