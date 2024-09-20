
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.collections.AdvancingIterator

package com.tangosol.coherence.component.util.collections;

import java.util.NoSuchElementException;

/**
 * An implementation of an Iterator based on a single advance() method.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class AdvancingIterator
        extends    com.tangosol.coherence.component.util.Collections
        implements java.util.Iterator
    {
    // ---- Fields declarations ----
    
    /**
     * Property NextElement
     *
     * Next entry to return.
     */
    private Object __m_NextElement;
    
    /**
     * Property NextReady
     *
     * True iff the NextEntry is ready.
     */
    private boolean __m_NextReady;
    
    // Initializing constructor
    public AdvancingIterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return AdvancingIterator.class;
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
    
    protected Object advance()
        {
        return null;
        }
    
    // Accessor for the property "NextElement"
    /**
     * Getter for property NextElement.<p>
    * Next entry to return.
     */
    private Object getNextElement()
        {
        return __m_NextElement;
        }
    
    // From interface: java.util.Iterator
    public boolean hasNext()
        {
        if (isNextReady())
            {
            return true;
            }
        
        Object oNext = advance();
        
        if (oNext == null)
            {
            return false;
            }
        
        setNextElement(oNext);
        setNextReady(true);
        
        return true;
        }
    
    // Accessor for the property "NextReady"
    /**
     * Getter for property NextReady.<p>
    * True iff the NextEntry is ready.
     */
    private boolean isNextReady()
        {
        return __m_NextReady;
        }
    
    // From interface: java.util.Iterator
    public Object next()
        {
        // import java.util.NoSuchElementException;
        
        if (!isNextReady() && !hasNext())
            {
            throw new NoSuchElementException();
            }
        
        setNextReady(false);
        return getNextElement();
        }
    
    // From interface: java.util.Iterator
    public void remove()
        {
        throw new UnsupportedOperationException();
        }
    
    // Accessor for the property "NextElement"
    /**
     * Setter for property NextElement.<p>
    * Next entry to return.
     */
    private void setNextElement(Object entry)
        {
        __m_NextElement = entry;
        }
    
    // Accessor for the property "NextReady"
    /**
     * Setter for property NextReady.<p>
    * True iff the NextEntry is ready.
     */
    private void setNextReady(boolean fReady)
        {
        __m_NextReady = fReady;
        }
    }
