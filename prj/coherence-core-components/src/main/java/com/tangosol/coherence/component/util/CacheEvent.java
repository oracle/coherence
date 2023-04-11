
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.CacheEvent

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.tangosol.internal.tracing.Scope;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.TracingHelper;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Listeners;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

/**
 * Runnable MapEvent.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class CacheEvent
        extends    com.tangosol.coherence.component.Util
        implements Runnable
    {
    // ---- Fields declarations ----
    
    /**
     * Property Listeners
     *
     * Optional Listeners object containing MapListener objects.
     */
    private com.tangosol.util.Listeners __m_Listeners;
    
    /**
     * Property ListenerSupport
     *
     * Optional MapListenerSupport object containing MapListener objects.
     */
    private com.tangosol.util.MapListenerSupport __m_ListenerSupport;
    
    /**
     * Property MapEvent
     *
     * The actual MapEvent to fire.
     */
    private com.tangosol.util.MapEvent __m_MapEvent;
    
    /**
     * Property MapListener
     *
     * Optional MapListener object.
     */
    private com.tangosol.util.MapListener __m_MapListener;
    
    /**
     * Property ParentTracingSpan
     *
     * The TracingContext of the parent span.
     */
    private com.tangosol.internal.tracing.Span __m_ParentTracingSpan;
    
    // Default constructor
    public CacheEvent()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public CacheEvent(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.CacheEvent();
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
            clz = Class.forName("com.tangosol.coherence/component/util/CacheEvent".replace('/', '.'));
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
     * Dispatch the specified MapEvent to all Synchronous listeners and add to
    * the specified Queue for deferred execution for standard ones.
     */
    public static void dispatchSafe(com.tangosol.util.MapEvent event, com.tangosol.util.Listeners listeners, Queue queue)
        {
        // import com.tangosol.internal.tracing.Scope;
        // import com.tangosol.internal.tracing.Span;
        // import com.tangosol.internal.tracing.Span$Type as com.tangosol.internal.tracing.Span.Type;
        // import com.tangosol.internal.tracing.TracingHelper;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.util.MapListener;
        // import com.tangosol.util.SynchronousListener as com.tangosol.util.SynchronousListener;
        
        if (listeners != null)
            {
            Object[] aListener = listeners.listeners();
            for (int i = 0, c = aListener.length; i < c; i++)
                {
                MapListener listener = (MapListener) aListener[i];
                if (listener instanceof com.tangosol.util.SynchronousListener)
                    {
                    NamedCache   cache    = (NamedCache) event.getSource();
                    CacheService svc      = cache.getCacheService();
                    boolean      fTracing = TracingHelper.isEnabled();
                    Span         span     = null;
                    Scope        scope    = null;
        
                    if (fTracing)
                        {
                        span  = TracingHelper.newSpan("process", event)
                            .withMetadata(com.tangosol.internal.tracing.Span.Type.COMPONENT.key(), svc == null ? "n/a" : svc.getInfo().getServiceName())
                            .withMetadata("cache", cache.getCacheName())
                            .withMetadata("event.action", event.getDescription(event.getId()))
                            .withMetadata("listener", listener.getClass().getName())
                            .startSpan();
                        scope = TracingHelper.getTracer().withSpan(span);
                        }      
        
                    try
                        {
                        event.dispatch(listener);
                        }
                    catch (Throwable e)
                        {
                        if (fTracing)
                            {
                            TracingHelper.augmentSpanWithErrorDetails(span, true, e);
                            }
                        _trace("An exception occurred while dispatching synchronous event:" + event, 1);
                        _trace(e);
                        _trace("(The exception has been logged and execution is continuing.)", 1);
                        }
                    finally
                        {
                        if (fTracing)
                            {
                            scope.close();
                            span.end();
                            }
                        }
                    }
                else
                    {
                    queue.add(instantiate(event, listener));
                    }
                }
            }
        }
    
    // Accessor for the property "Listeners"
    /**
     * Getter for property Listeners.<p>
    * Optional Listeners object containing MapListener objects.
     */
    public com.tangosol.util.Listeners getListeners()
        {
        return __m_Listeners;
        }
    
    // Accessor for the property "ListenerSupport"
    /**
     * Getter for property ListenerSupport.<p>
    * Optional MapListenerSupport object containing MapListener objects.
     */
    public com.tangosol.util.MapListenerSupport getListenerSupport()
        {
        return __m_ListenerSupport;
        }
    
    // Accessor for the property "MapEvent"
    /**
     * Getter for property MapEvent.<p>
    * The actual MapEvent to fire.
     */
    public com.tangosol.util.MapEvent getMapEvent()
        {
        return __m_MapEvent;
        }
    
    // Accessor for the property "MapListener"
    /**
     * Getter for property MapListener.<p>
    * Optional MapListener object.
     */
    public com.tangosol.util.MapListener getMapListener()
        {
        return __m_MapListener;
        }
    
    // Accessor for the property "ParentTracingSpan"
    /**
     * Getter for property ParentTracingSpan.<p>
    * The TracingContext of the parent span.
     */
    public com.tangosol.internal.tracing.Span getParentTracingSpan()
        {
        return __m_ParentTracingSpan;
        }
    
    public static CacheEvent instantiate(com.tangosol.util.MapEvent event, com.tangosol.util.Listeners listeners)
        {
        _assert(event != null && listeners != null);
        
        CacheEvent task = new CacheEvent();
        task.setMapEvent(event);
        task.setListeners(listeners);
        return task;
        }
    
    public static CacheEvent instantiate(com.tangosol.util.MapEvent event, com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.internal.tracing.TracingHelper;
        
        _assert(event != null && listener != null);
        
        CacheEvent task = new CacheEvent();
        task.setMapEvent(event);
        task.setMapListener(listener);
        task.setParentTracingSpan(TracingHelper.getActiveSpan());
        return task;
        }
    
    public static CacheEvent instantiate(com.tangosol.util.MapEvent event, com.tangosol.util.MapListenerSupport support)
        {
        _assert(event != null && support != null);
        
        CacheEvent task = new CacheEvent();
        task.setMapEvent(event);
        task.setListenerSupport(support);
        return task;
        }
    
    // From interface: java.lang.Runnable
    public void run()
        {
        // import Component.Util.Daemon.QueueProcessor.Service;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.util.Listeners;
        // import com.tangosol.util.MapEvent;
        // import com.tangosol.util.MapListener;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import com.tangosol.internal.tracing.Scope;
        // import com.tangosol.internal.tracing.TracingHelper;
        
        MapEvent   event = getMapEvent();
        NamedCache cache = (NamedCache) event.getSource();
        
        if (cache.isActive())
            {
            com.tangosol.util.MapListenerSupport support = getListenerSupport();
            Service svc     = ((Service) cache.getCacheService());
            Scope   scope   = svc == null
                ? null
                : TracingHelper.getTracer().withSpan(svc.newTracingSpan("process", event)
                            .setParent(getParentTracingSpan())
                            .withMetadata("cache", cache.getCacheName())
                            .withMetadata("event.action", event.getDescription(event.getId()))
                            .startSpan());
        
            try
                {
                if (support == null)
                    {
                    Listeners listeners = getListeners();
                    if (listeners == null)
                        {
                        event.dispatch(getMapListener());
                        }
                    else
                        {
                        event.dispatch(listeners, true);
                        }
                    }
                else
                    {
                    support.fireEvent(event, true);
                    }
                }
            finally
                {
                if (scope != null)
                    {
                    scope.close();
                    }
                }
            }
        }
    
    // Accessor for the property "Listeners"
    /**
     * Setter for property Listeners.<p>
    * Optional Listeners object containing MapListener objects.
     */
    protected void setListeners(com.tangosol.util.Listeners listeners)
        {
        __m_Listeners = listeners;
        }
    
    // Accessor for the property "ListenerSupport"
    /**
     * Setter for property ListenerSupport.<p>
    * Optional MapListenerSupport object containing MapListener objects.
     */
    protected void setListenerSupport(com.tangosol.util.MapListenerSupport support)
        {
        __m_ListenerSupport = support;
        }
    
    // Accessor for the property "MapEvent"
    /**
     * Setter for property MapEvent.<p>
    * The actual MapEvent to fire.
     */
    protected void setMapEvent(com.tangosol.util.MapEvent event)
        {
        __m_MapEvent = event;
        }
    
    // Accessor for the property "MapListener"
    /**
     * Setter for property MapListener.<p>
    * Optional MapListener object.
     */
    protected void setMapListener(com.tangosol.util.MapListener listener)
        {
        __m_MapListener = listener;
        }
    
    // Accessor for the property "ParentTracingSpan"
    /**
     * Setter for property ParentTracingSpan.<p>
    * The TracingContext of the parent span.
     */
    public void setParentTracingSpan(com.tangosol.internal.tracing.Span spanTracing)
        {
        __m_ParentTracingSpan = spanTracing;
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + ": " + getMapEvent();
        }
    }
