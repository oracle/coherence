
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.requestContext.asyncContext.AsyncAggregatorContext

package com.tangosol.coherence.component.net.requestContext.asyncContext;

/**
 * RequestContext for AsyncronousAggregator requests.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class AsyncAggregatorContext
        extends    com.tangosol.coherence.component.net.requestContext.AsyncContext
    {
    // ---- Fields declarations ----
    
    /**
     * Property AsyncAggregator
     *
     * (Transient) AbstractAsynchronousAggregator associated with this context.
     */
    private transient com.tangosol.util.aggregator.AbstractAsynchronousAggregator __m_AsyncAggregator;
    
    // Default constructor
    public AsyncAggregatorContext()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public AsyncAggregatorContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.requestContext.asyncContext.AsyncAggregatorContext();
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
            clz = Class.forName("com.tangosol.coherence/component/net/requestContext/asyncContext/AsyncAggregatorContext".replace('/', '.'));
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
    
    // Accessor for the property "AsyncAggregator"
    /**
     * Getter for property AsyncAggregator.<p>
    * (Transient) AbstractAsynchronousAggregator associated with this context.
     */
    public com.tangosol.util.aggregator.AbstractAsynchronousAggregator getAsyncAggregator()
        {
        return __m_AsyncAggregator;
        }
    
    // Declared at the super level
    /**
     * Process the completion of the request submission.
     */
    public void processCompletion()
        {
        try
            {
            getAsyncAggregator().onComplete();
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
        processPartialResult(e);
        }
    
    /**
     * Process the completion of the request submission.
     */
    public void processPartialResult(Object oResult)
        {
        try
            {
            if (oResult instanceof Throwable)
                {
                getAsyncAggregator().onException((Throwable) oResult);
                }
            else
                {
                getAsyncAggregator().onResult(
                    getValueConverter().convert(oResult));
                }
            }
        catch (Throwable e)
            {
            reportException(e);
            }
        }
    
    // Accessor for the property "AsyncAggregator"
    /**
     * Setter for property AsyncAggregator.<p>
    * (Transient) AbstractAsynchronousAggregator associated with this context.
     */
    public void setAsyncAggregator(com.tangosol.util.aggregator.AbstractAsynchronousAggregator asyncAggregator)
        {
        __m_AsyncAggregator = asyncAggregator;
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + "{Aggregator=" + getAsyncAggregator() + '}';
        }
    }
