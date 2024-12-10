
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.requestContext.asyncContext.AsyncProcessorContext

package com.tangosol.coherence.component.net.requestContext.asyncContext;

import com.tangosol.net.internal.SimpleConverterEntry;
import com.tangosol.util.Binary;
import com.tangosol.util.processor.AbstractAsynchronousProcessor;

/**
 * RequestContext for AsyncronousProcessor requests.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class AsyncProcessorContext
        extends    com.tangosol.coherence.component.net.requestContext.AsyncContext
    {
    // ---- Fields declarations ----
    
    /**
     * Property AsyncProcessor
     *
     * (Transient) AbstractAsynchronousProcessor associated with this context.
     */
    private transient com.tangosol.util.processor.AbstractAsynchronousProcessor __m_AsyncProcessor;
    
    // Default constructor
    public AsyncProcessorContext()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public AsyncProcessorContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.requestContext.asyncContext.AsyncProcessorContext();
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
            clz = Class.forName("com.tangosol.coherence/component/net/requestContext/asyncContext/AsyncProcessorContext".replace('/', '.'));
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
    
    // Accessor for the property "AsyncProcessor"
    /**
     * Getter for property AsyncProcessor.<p>
    * (Transient) AbstractAsynchronousProcessor associated with this context.
     */
    public com.tangosol.util.processor.AbstractAsynchronousProcessor getAsyncProcessor()
        {
        return __m_AsyncProcessor;
        }
    
    // Declared at the super level
    /**
     * Process the completion of the request submission.
    * Complete the execution.
     */
    public void processCompletion()
        {
        try
            {
            getAsyncProcessor().onComplete();
            }
        catch (Throwable e)
            {
            reportException(e);
            }
        
        getService().unregisterRequestContext(this);
        }
    
    // Declared at the super level
    /**
     * Process an exception that occurred during the request submission.
     */
    public void processException(Throwable e)
        {
        processPartialResult(null, e);
        }
    
    /**
     * Process partial result.
     */
    public void processPartialResult(com.tangosol.util.Binary binKey, Object oResult)
        {
        // import com.tangosol.net.internal.SimpleConverterEntry;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.processor.AbstractAsynchronousProcessor;
        
        AbstractAsynchronousProcessor asyncProc = getAsyncProcessor();
        
        try
            {
            if (oResult instanceof Throwable)
                {
                asyncProc.onException((Throwable) oResult);
                }
            else
                {
                asyncProc.onResult(
                    new SimpleConverterEntry(binKey, (Binary) oResult, getValueConverter()));
                }
            }
        catch (Throwable e)
            {
            reportException(e);
            }
        }
    
    /**
     * Process single result.
     */
    public void processSingleResult(com.tangosol.util.Binary binKey, Object oResult)
        {
        processPartialResult(binKey, oResult);
        
        processCompletion();
        }
    
    // Accessor for the property "AsyncProcessor"
    /**
     * Setter for property AsyncProcessor.<p>
    * (Transient) AbstractAsynchronousProcessor associated with this context.
     */
    public void setAsyncProcessor(com.tangosol.util.processor.AbstractAsynchronousProcessor asyncProcessor)
        {
        __m_AsyncProcessor = asyncProcessor;
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + "{Processor=" + getAsyncProcessor() + '}';
        }
    }
