
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service

package com.tangosol.coherence.component.util.daemon.queueProcessor;

import com.tangosol.coherence.component.net.Security;
import com.tangosol.coherence.component.net.message.RequestMessage;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Logger;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Continuation;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.service.DefaultServiceDependencies;
import com.tangosol.internal.tracing.Scope;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.TracingHelper;
import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;
import com.tangosol.io.WrapperBufferInput;
import com.tangosol.io.WrapperBufferOutput;
import com.tangosol.license.LicensedObject;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Listeners;
import com.tangosol.util.SafeLinkedList;
import com.tangosol.util.ServiceEvent;
import com.tangosol.util.WrapperException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base definition of a Service component.
 * 
 * A Service has a service thread, an optional execute thread pool, and an
 * event dispatcher thread.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Service
        extends    com.tangosol.coherence.component.util.daemon.QueueProcessor
        implements com.tangosol.util.Service
    {
    // ---- Fields declarations ----
    
    /**
     * Property AcceptingClients
     *
     * Set to true when the Service has advanced to the state at which it can
     * accept requests from client threads.
     */
    private boolean __m_AcceptingClients;
    
    /**
     * Property ContextClassLoader
     *
     * @see com.tangosol.io.ClassLoaderAware
     */
    private transient ClassLoader __m_ContextClassLoader;
    
    /**
     * Property DaemonPool
     *
     * The daemon pool.
     * 
     * @see #configure(XmlElement)
     */
    private transient com.tangosol.coherence.component.util.DaemonPool __m_DaemonPool;
    
    /**
     * Property Dependencies
     *
     * The external dependencies needed by this component. The dependencies
     * object must be full populated and validated before this property is set.
     *  See setDependencies.  
     * 
     * The mechanism for creating and populating dependencies is hidden from
     * this component. Typically, the dependencies object is populated using
     * data from some external configuration, such as XML, but this may not
     * always be the case.
     */
    private com.tangosol.net.ServiceDependencies __m_Dependencies;
    
    /**
     * Property EventDispatcher
     *
     * The event dispatcher daemon.
     */
    private transient Service.EventDispatcher __m_EventDispatcher;
    
    /**
     * Property EventListeners
     *
     * The list of registered EventListeners.
     * 
     * @see #addEventListener and #removeEventListener
     */
    private com.tangosol.util.Listeners __m_EventListeners;
    
    /**
     * Property OperationalContext
     *
     * The OperationalContext for this Service.
     */
    private com.tangosol.net.OperationalContext __m_OperationalContext;
    
    /**
     * Property Serializer
     *
     * The cached Serializer instance for the Service ContextClassLoader.
     */
    private transient com.tangosol.io.Serializer __m_Serializer;
    
    /**
     * Property SerializerFactory
     *
     * The SerializerFactory used by this Service.
     */
    private com.tangosol.io.SerializerFactory __m_SerializerFactory;
    
    /**
     * Property SerializerMap
     *
     * A Map of configured Serializer instances, keyed by ClassLoader (if
     * applicable).
     */
    private java.util.Map __m_SerializerMap;
    
    /**
     * Property SERVICE_INITIAL
     *
     * The Service has been created but has not been started yet.
     */
    public static final int SERVICE_INITIAL = 0;
    
    /**
     * Property SERVICE_STARTED
     *
     * The Service is running.
     */
    public static final int SERVICE_STARTED = 2;
    
    /**
     * Property SERVICE_STARTING
     *
     * The Service has been asked to start but has not yet finished starting.
     */
    public static final int SERVICE_STARTING = 1;
    
    /**
     * Property SERVICE_STOPPED
     *
     * The Service has shut down gracefully (shutdown method) or has been
     * stopped hard (stop method).
     */
    public static final int SERVICE_STOPPED = 4;
    
    /**
     * Property SERVICE_STOPPING
     *
     * The Service has been asked to shut down gracefully but has not yet
     * finished shutting down gracefully.
     */
    public static final int SERVICE_STOPPING = 3;
    
    /**
     * Property ServiceConfig
     *
     * Original XML configuration that was supplied to the Service; may be null.
     */
    private com.tangosol.run.xml.XmlElement __m_ServiceConfig;
    
    /**
     * Property ServiceName
     *
     * The name of this Service.
     */
    private String __m_ServiceName;
    
    /**
     * Property ServiceState
     *
     * The state of the Service; one of the SERVICE_ enums.
     * 
     * @volatile as of 12.1.3
     */
    private volatile int __m_ServiceState;
    
    /**
     * Property StartupTimeout
     *
     * The time (in millis) that limits the waiting period during the service
     * startup sequence. Non-positive numbers indicate no timeout.
     */
    private transient long __m_StartupTimeout;
    
    /**
     * Property StatsCpu
     *
     * Statistics: total time spent while processing messages.
     */
    private transient long __m_StatsCpu;
    
    /**
     * Property StatsReceived
     *
     * Statistics: total number of received messages.
     */
    private transient long __m_StatsReceived;
    
    /**
     * Property StatsReset
     *
     * Statistics: Date/time value that the stats have been reset.
     */
    private transient long __m_StatsReset;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("DispatchEvent", Service.DispatchEvent.get_CLASS());
        __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.get_CLASS());
        }
    
    // Initializing constructor
    public Service(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        
        // state initialization: private properties
        try
            {
            __m_EventListeners = new com.tangosol.util.Listeners();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    // Getter for virtual constant NonBlocking
    public boolean isNonBlocking()
        {
        return true;
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service".replace('/', '.'));
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
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    /**
     * Register the given EventListener with this Service.
    * 
    * @param l the EventListener to register
    * 
    * @see #removeEventListener
     */
    protected void addEventListener(java.util.EventListener l)
        {
        ensureEventDispatcher();
        getEventListeners().add(l);
        }
    
    // From interface: com.tangosol.util.Service
    public void addServiceListener(com.tangosol.util.ServiceListener l)
        {
        addEventListener(l);
        }
    
    /**
     * Adjust the default timeout value using the passed context-specific
    * timeout value.
    * 
    * @param lDefaultValue the default timeout value (must be non-negative)
    * @param lContextValue positive, or zero for "default timeout", or -1 for
    * "no timeout"
    * 
    * @return the adjusted timeout value
     */
    public static long adjustTimeout(long lDefaultTimeout, long lContextTimeout)
        {
        final long TIMEOUT_DEFAULT = 0L;
        final long TIMEOUT_NONE    = -1L;
        
        return lContextTimeout == TIMEOUT_DEFAULT ? lDefaultTimeout :
               lContextTimeout == TIMEOUT_NONE    ? 0L
                                                  : lContextTimeout;
        }
    
    /**
     * Create a new Default dependencies object by copying the supplies
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone, producing their variant of the
    * dependencies interface.
    * 
    * @return the cloned dependencies
     */
    protected com.tangosol.internal.net.service.DefaultServiceDependencies cloneDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.DefaultServiceDependencies;
        
        return new DefaultServiceDependencies(deps);
        }
    
    // From interface: com.tangosol.util.Service
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.DefaultServiceDependencies;
        // import com.tangosol.internal.net.service.LegacyXmlServiceHelper as com.tangosol.internal.net.service.LegacyXmlServiceHelper;
        
        // This is still needed by DCCF based config.  ECCF based code will
        // call setDependencies from the scheme code (see AbstractServiceScheme.java)
        setDependencies(com.tangosol.internal.net.service.LegacyXmlServiceHelper.fromXml(
            xml, new DefaultServiceDependencies(), getOperationalContext()));
        
        setServiceConfig(xml);
        }
    
    /**
     * Dispatch the given event to the EventDispatcher.
    * 
    * @param evt the event to dispatch
     */
    protected void dispatchEvent(java.util.EventObject evt, com.tangosol.util.Listeners listeners)
        {
        // import java.util.EventListener;
        
        if (listeners.isEmpty())
            {
            // no listeners; nothing to do
            return;
            }
        
        EventListener[] aSyncListeners  = listeners.getSynchronousListeners();
        EventListener[] aAsyncListeners = listeners.getAsynchronousListeners();
        
        Service.DispatchEvent task = instantiateDispatchEvent();
        task.setEvent(evt);
        if (aSyncListeners.length > 0)
            {
            // dispatch to synchronous listeners
            try
                {
                task.setListeners(aSyncListeners);
                task.run();
                }
            catch (Throwable t)
                {
                _trace("An exception occurred while dispatching the following event:\n"
                     + String.valueOf(task), 1);
                _trace(t);
                _trace("The service thread has logged the exception and is continuing.", 1);
                }
            }
        
        if (aAsyncListeners.length > 0)
            {
            // queue the dispatch to the asynchronous listeners
            task.setListeners(aAsyncListeners);
            ensureEventDispatcher().getQueue().add(task);
            }
        }
    
    /**
     * Dispatch a ServiceEvent to the EventDispatcher.
    * 
    * @param nEvent the ID of the ServiceEvent
     */
    public void dispatchServiceEvent(int nEvent)
        {
        // import com.tangosol.util.Listeners;
        // import com.tangosol.util.ServiceEvent;
        
        Listeners listeners = getEventListeners();
        if (!listeners.isEmpty())
            {
            dispatchEvent(new ServiceEvent(this, nEvent), listeners);
            }
        }
    
    /**
     * @return a running EventDispatcher
     */
    public Service.EventDispatcher ensureEventDispatcher()
        {
        // import com.tangosol.coherence.config.Config;
        
        Service.EventDispatcher dispatcher = getEventDispatcher();
        
        if (dispatcher == null)
            {
            synchronized (this)
                {
                dispatcher = getEventDispatcher();
                if (dispatcher == null)
                    {
                    dispatcher = (Service.EventDispatcher) _findChild("EventDispatcher");
                    if (!isExiting())
                        {
                        long   cTimeout = getDefaultGuardTimeout();
                        String sTimeout = Config.getProperty("coherence.events.timeout");
                        if (sTimeout != null)
                            {
                            // check the undocumented guard timeout override property
                            try
                                {
                                cTimeout = Long.parseLong(sTimeout);
                                }
                            catch (Exception e) {}
                            }
        
                        if (cTimeout > 0)
                            {
                            guard(dispatcher.getGuardable(), cTimeout, getDefaultGuardRecovery());
                            }
        
                        dispatcher.setPriority(getDependencies().getEventDispatcherThreadPriority());
                        dispatcher.setThreadGroup(getThreadGroup());
                        dispatcher.start();
                        }
                    setEventDispatcher(dispatcher);
                    }
                }
            }
        
        return dispatcher;
        }
    
    /**
     * Return an instance of the configured Serializer.
    * 
    * @return a Serializer
     */
    public com.tangosol.io.Serializer ensureSerializer()
        {
        // import com.tangosol.io.Serializer;
        
        Serializer serializer = getSerializer();
        if (serializer == null)
            {
            serializer = instantiateSerializer(getContextClassLoader());
            setSerializer(serializer);
            }
        
        return serializer;
        }
    
    /**
     * Return an instance of the configured Serializer.
    * 
    * @param loader  if the Serializer class implements ClassLoaderAware, the
    * returned Serializer will be configured with this ClassLoader
    * 
    * @return a Serializer
     */
    public com.tangosol.io.Serializer ensureSerializer(ClassLoader loader)
        {
        // import com.tangosol.io.ClassLoaderAware;
        // import com.tangosol.io.Serializer;
        // import com.tangosol.io.SerializerFactory;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        ClassLoader loaderCtx = getContextClassLoader();
        if (loader == null)
            {
            loader = loaderCtx;
            }
        
        Serializer serializer = ensureSerializer();
        if (loader == loaderCtx || !(serializer instanceof ClassLoaderAware))
            {
            // what we have fits the request
            return serializer;
            }
        
        SerializerFactory factory = getSerializerFactory();
        if (factory == null)
            {
            // no serializer is configured so use the default;
            // additionally, avoid caching the serializer twice
            return ExternalizableHelper.ensureSerializer(loader);
            }
        
        // cache the serializer by loader
        Map map = getSerializerMap();
        synchronized (map)
            {
            serializer = (Serializer) map.get(loader);
            if (serializer == null)
                {
                serializer = factory.createSerializer(loader);
                map.put(loader, serializer);
                }
            }
        
        return serializer;
        }
    
    /**
     * Return the string value of a named child element of the given XML
    * configuration.
    * 
    * @param xmlConfig the parent XML configuration element
    * @param sName the name of the child XML configuration element
    * @param sDefault the default value to return if the child element is
    * missing or empty
    * 
    * @throws IllegalArgumentException if the child element and default value
    * is missing or empty
     */
    public static String ensureStringValue(com.tangosol.run.xml.XmlElement xmlConfig, String sName, String sDefault)
        {
        String sValue = xmlConfig.getSafeElement(sName).getString(sDefault);
        if (sValue == null || sValue.length() == 0)
            {
            throw new IllegalArgumentException("the required \"" + sName
                    + "\" configuration element is missing or empty");
            }
        
        return sValue;
        }
    
    /**
     * @return a human-readible description of the given Service state
     */
    public static String formatServiceStateName(int nState)
        {
        switch (nState)
            {
            case SERVICE_INITIAL:
                return "SERVICE_INITIAL";
            case SERVICE_STARTING:
                return "SERVICE_STARTING";
            case SERVICE_STARTED:
                return "SERVICE_STARTED";
            case SERVICE_STOPPING:
                return "SERVICE_STOPPING";
            case SERVICE_STOPPED:
                return "SERVICE_STOPPED";
            default:
                return "<unknown>";
            }
        }
    
    /**
     * @return a human-readable description of the Service statistics
     */
    public String formatStats()
        {
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        // import com.tangosol.util.Base;
        
        long   cCpu   = getStatsCpu();
        long   cTotal = Base.getSafeTimeMillis() - getStatsReset();
        long   cMsgs  = getStatsReceived();
        double dCpu   = cTotal == 0L ? 0.0 : ((double) cCpu)/((double) cTotal);
        double dThru  = cCpu == 0L ? 0.0 : ((double) cMsgs*1000)/((double) cCpu);
        
        // round rates
        dCpu = ((int) (dCpu * 1000)) / 10D; // percentage
        
        StringBuffer sb = new StringBuffer();
        sb.append("Cpu=")
          .append(cCpu)
          .append("ms (")
          .append(dCpu)
          .append("%), Messages=")
          .append(cMsgs)
          .append(", Throughput=")
          .append((float) dThru)
          .append("msg/sec")
          ;
        
        // thread pool stats
        com.tangosol.coherence.component.util.DaemonPool pool = getDaemonPool();
        if (pool.isStarted())
            {
            long   cPoolTotal  = pool.getStatsActiveMillis();
            long   cTasks      = pool.getStatsTaskCount();
            long   cHung       = pool.getStatsHungCount();
            float  flAvgThread = cTotal == 0L ? 0.0f : (float) (((double) cPoolTotal)/ ((double) cTotal));
            float  flAvgTask   = cTasks == 0L ? 0.0f : (float) (((double) cPoolTotal)/ ((double) cTasks));
        
            sb.append(", AverageActiveThreadCount=")
              .append(flAvgThread)
              .append(", Tasks=")
              .append(cTasks)
              .append(", AverageTaskDuration=")
              .append(flAvgTask)
              .append("ms, MaximumBacklog=")
              .append(pool.getStatsMaxBacklog());
        
              if (cHung > 0)
                {
                sb.append(", HungTaskCount=")
                  .append(cHung)
                  .append(", HungMaxDuration=")
                  .append(pool.getStatsHungDuration())
                  .append(", HungMaxId=")
                  .append(pool.getStatsHungTaskId());
                }
            }
        
        return sb.toString();
        }
    
    // Declared at the super level
    /**
     * *** LICENSE ***
     */
    public final Object get_Feed()
        {
        // import com.tangosol.license.LicensedObject;
        // import com.tangosol.license.LicensedObject$LicenseData as com.tangosol.license.LicensedObject.LicenseData;
        // import com.tangosol.util.Base;
        // import java.util.ArrayList;
        // import java.util.Iterator;
        // import java.util.List;
        
        try
            {
            /* TODO
            Object    feed = super.get_Feed();
            com.tangosol.license.LicensedObject.LicenseData[] aLic = (com.tangosol.license.LicensedObject.LicenseData[]) ClassHelper.invoke(feed,
                "getClassLicenseData", ClassHelper.VOID);
            */
            com.tangosol.license.LicensedObject.LicenseData[] aLic = LicensedObject.getLicenseData();
        
            // cull the license data:
            //  - throw away all invalid licenses
            //  - if there is a production license ignore all other types
            //  - if there is a dev license ignore all evalualtion ones
        
            List list     = new ArrayList();
            int  nMaxType = -1;
            for (int i = 0, c = aLic.length; i < c; i++)
                {
                com.tangosol.license.LicensedObject.LicenseData lic = aLic[i];
        
                if (LicensedObject.getLicenseFailure(lic) == null)
                    {
                    int nType = lic.nLicenseType;
                    if (nType >= nMaxType)
                        {
                        nMaxType = nType;
                        list.add(lic);
                        }
                    }
                }
        
            for (Iterator iter = list.iterator(); iter.hasNext();)
                {
                com.tangosol.license.LicensedObject.LicenseData lic = (com.tangosol.license.LicensedObject.LicenseData) iter.next();
                if (lic.nLicenseType < nMaxType)
                    {
                    iter.remove();
                    }
                }
        
            return list.toArray(new com.tangosol.license.LicensedObject.LicenseData[list.size()]);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // From interface: com.tangosol.util.Service
    // Accessor for the property "ContextClassLoader"
    /**
     * Getter for property ContextClassLoader.<p>
    * @see com.tangosol.io.ClassLoaderAware
     */
    public ClassLoader getContextClassLoader()
        {
        return __m_ContextClassLoader;
        }
    
    // Accessor for the property "DaemonPool"
    /**
     * Getter for property DaemonPool.<p>
    * The daemon pool.
    * 
    * @see #configure(XmlElement)
     */
    public com.tangosol.coherence.component.util.DaemonPool getDaemonPool()
        {
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        
        com.tangosol.coherence.component.util.DaemonPool pool = __m_DaemonPool;
        
        if (pool == null)
            {
            pool = (com.tangosol.coherence.component.util.DaemonPool) _findChild("DaemonPool");
            setDaemonPool(pool);
            }
        
        return pool;
        }
    
    // Accessor for the property "DecoratedThreadName"
    /**
     * Getter for property DecoratedThreadName.<p>
    * (Calculated) Name of the service thread decorated with any additional
    * information that could be useful for thread dump analysis. The decorated
    * part is always trailing the full name delimited by the '|' character and
    * is truncated by the Logger.
     */
    public String getDecoratedThreadName()
        {
        // import Component.Util.Daemon.QueueProcessor.Logger;
        
        int    nState = getServiceState();
        String sState = nState == SERVICE_STARTED ? "" :
                        Logger.THREAD_NAME_DELIM + formatServiceStateName(nState);
        return getThreadName() + sState;
        }
    
    // Accessor for the property "Dependencies"
    /**
     * Getter for property Dependencies.<p>
    * The external dependencies needed by this component. The dependencies
    * object must be full populated and validated before this property is set. 
    * See setDependencies.  
    * 
    * The mechanism for creating and populating dependencies is hidden from
    * this component. Typically, the dependencies object is populated using
    * data from some external configuration, such as XML, but this may not
    * always be the case.
     */
    public com.tangosol.net.ServiceDependencies getDependencies()
        {
        return __m_Dependencies;
        }
    
    // Accessor for the property "Description"
    /**
     * Getter for property Description.<p>
    * Human-readable description of additional Service properties. Used by
    * toString().
     */
    public String getDescription()
        {
        return null;
        }
    
    // Accessor for the property "EventDispatcher"
    /**
     * Called on the service thread only.
     */
    public Service.EventDispatcher getEventDispatcher()
        {
        return __m_EventDispatcher;
        }
    
    // Accessor for the property "EventListeners"
    /**
     * Getter for property EventListeners.<p>
    * The list of registered EventListeners.
    * 
    * @see #addEventListener and #removeEventListener
     */
    public com.tangosol.util.Listeners getEventListeners()
        {
        return __m_EventListeners;
        }
    
    // Accessor for the property "OperationalContext"
    /**
     * Getter for property OperationalContext.<p>
    * The OperationalContext for this Service.
     */
    public com.tangosol.net.OperationalContext getOperationalContext()
        {
        return __m_OperationalContext;
        }
    
    // Accessor for the property "Serializer"
    /**
     * Getter for property Serializer.<p>
    * The cached Serializer instance for the Service ContextClassLoader.
     */
    public com.tangosol.io.Serializer getSerializer()
        {
        return __m_Serializer;
        }
    
    // Accessor for the property "SerializerFactory"
    /**
     * Getter for property SerializerFactory.<p>
    * The SerializerFactory used by this Service.
     */
    public com.tangosol.io.SerializerFactory getSerializerFactory()
        {
        return __m_SerializerFactory;
        }
    
    // Accessor for the property "SerializerMap"
    /**
     * Getter for property SerializerMap.<p>
    * A Map of configured Serializer instances, keyed by ClassLoader (if
    * applicable).
     */
    public java.util.Map getSerializerMap()
        {
        return __m_SerializerMap;
        }
    
    // Accessor for the property "ServiceConfig"
    /**
     * Getter for property ServiceConfig.<p>
    * Original XML configuration that was supplied to the Service; may be null.
     */
    public com.tangosol.run.xml.XmlElement getServiceConfig()
        {
        return __m_ServiceConfig;
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Getter for property ServiceName.<p>
    * The name of this Service.
     */
    public String getServiceName()
        {
        String sName = __m_ServiceName;
        return sName == null ? get_Name() : sName;
        }
    
    // Accessor for the property "ServiceState"
    /**
     * Getter for property ServiceState.<p>
    * The state of the Service; one of the SERVICE_ enums.
    * 
    * @volatile as of 12.1.3
     */
    public int getServiceState()
        {
        return __m_ServiceState;
        }
    
    // Accessor for the property "ServiceStateName"
    /**
     * Getter for property ServiceStateName.<p>
    * Calculated helper property; returns a human-readable description of the
    * ServiceState property.
     */
    public String getServiceStateName()
        {
        return formatServiceStateName(getServiceState());
        }
    
    // Accessor for the property "StartupTimeout"
    /**
     * Getter for property StartupTimeout.<p>
    * The time (in millis) that limits the waiting period during the service
    * startup sequence. Non-positive numbers indicate no timeout.
     */
    public long getStartupTimeout()
        {
        return __m_StartupTimeout;
        }
    
    // Accessor for the property "StatsCpu"
    /**
     * Getter for property StatsCpu.<p>
    * Statistics: total time spent while processing messages.
     */
    public long getStatsCpu()
        {
        return __m_StatsCpu;
        }
    
    // Accessor for the property "StatsReceived"
    /**
     * Getter for property StatsReceived.<p>
    * Statistics: total number of received messages.
     */
    public long getStatsReceived()
        {
        return __m_StatsReceived;
        }
    
    // Accessor for the property "StatsReset"
    /**
     * Getter for property StatsReset.<p>
    * Statistics: Date/time value that the stats have been reset.
     */
    public long getStatsReset()
        {
        return __m_StatsReset;
        }
    
    // Declared at the super level
    /**
     * Getter for property ThreadName.<p>
    * Specifies the name of the daemon thread. If not specified, the component
    * name will be used.
    * 
    * This property can be set at design time or runtime. If set at runtime,
    * this property must be configured before start() is invoked to cause the
    * daemon thread to have the specified name.
     */
    public String getThreadName()
        {
        return getServiceName();
        }
    
    // Declared at the super level
    /**
     * Getter for property WaitMillis.<p>
    * The number of milliseconds that the daemon will wait for notification.
    * Zero means to wait indefinitely. Negative value means to skip waiting
    * altogether.
    * 
    * @see #onWait
     */
    public long getWaitMillis()
        {
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        
        long       cWait = super.getWaitMillis();
        com.tangosol.coherence.component.util.DaemonPool pool  = getDaemonPool();
        
        if (pool.isStarted())
            {
            long cHungThreshold = pool.getHungThreshold();
            if (cHungThreshold > 0L)
                {
                // If the daemon pool is configured with a hung-task threshold,
                // we should wake periodically to check for hung-tasks.        
                long cWaitHungCheck = Math.max(1L, Math.min(cHungThreshold >> 2, 1000L));
                cWait = cWait == 0L ? cWaitHungCheck : Math.min(cWait, cWaitHungCheck);
                }
            }
        return cWait;
        }
    
    // Declared at the super level
    /**
     * Halt the daemon.  Brings down the daemon in an ungraceful manner.
    * This method should not synchronize or block in any way.
    * This method may not return.
     */
    protected void halt()
        {
        super.halt();
        
        ((Service.DaemonPool) getDaemonPool()).halt();
        }
    
    // Declared at the super level
    /**
     * Issue heartbeat.  See com.tangosol.net.Guardian$GuardContext.
    * 
    * @param cMillis  the duration of heartbeat to issue, or 0 for the default
    * heartbeat
     */
    protected void heartbeat(long cMillis)
        {
        if (!((Service.DaemonPool) getDaemonPool()).isStuck())
            {
            super.heartbeat(cMillis);
            }
        // else; the service has a DaemonPool configured, and
        // no progress has been made on a WorkSlot since
        // the last heartbeat, then the pool might be stuck.
        }
    
    /**
     * Factory pattern: create a new DispatchEvent instance.
    * 
    * @return the new DispatchEven
     */
    protected Service.DispatchEvent instantiateDispatchEvent()
        {
        return (Service.DispatchEvent) _newChild("DispatchEvent");
        }
    
    /**
     * Instantiate a Serializer and optionally configure it with the specified
    * ClassLoader.
    * 
    * @return the serializer
     */
    protected com.tangosol.io.Serializer instantiateSerializer(ClassLoader loader)
        {
        // import com.tangosol.io.SerializerFactory;
        // import com.tangosol.util.ExternalizableHelper;
        
        SerializerFactory factory = getSerializerFactory();
        return factory == null
                ? ExternalizableHelper.ensureSerializer(loader)
                : factory.createSerializer(loader);
        }
    
    // Accessor for the property "AcceptingClients"
    /**
     * Getter for property AcceptingClients.<p>
    * Set to true when the Service has advanced to the state at which it can
    * accept requests from client threads.
     */
    public boolean isAcceptingClients()
        {
        return __m_AcceptingClients;
        }
    
    // From interface: com.tangosol.util.Service
    // Accessor for the property "Running"
    /**
     * Getter for property Running.<p>
    * Calculated helper property; returns true if the service state is
    * SERVICE_STARTED.
     */
    public boolean isRunning()
        {
        return isAcceptingClients() && !isExiting();
        }
    
    /**
     * Return true if the current thread is one of the Service threads.
    * 
    * @param fStrict if true then only the service thread and event dispatcher
    * thread are considered to be service threads, if false, then DaemonPool
    * threads are also considered to be service threads.
     */
    public boolean isServiceThread(boolean fStrict)
        {
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        
        Thread           thread     = Thread.currentThread();
        Service.EventDispatcher dispatcher = getEventDispatcher();
        
        if (thread == getThread() ||
            dispatcher != null && thread == dispatcher.getThread())
            {
            return true;
            }
        else if (!fStrict)
            {
            com.tangosol.coherence.component.util.DaemonPool pool = getDaemonPool();
            return pool != null && pool.getThreadGroup() == thread.getThreadGroup();
            }
        
        return false;
        }
    
    // Accessor for the property "Stopped"
    /**
     * Getter for property Stopped.<p>
    * Return whether the service has completed the shutdown process.
     */
    public boolean isStopped()
        {
        return getServiceState() == SERVICE_STOPPED;
        }
    
    /**
     * Return a new OpenTracing Scope for the supplied op
    * 
    * @param sStage          the stage of the op, or null
    * @param op                 a RequestMessage or DaemonPool task (i.e.
    * Runnable)
    * @param spanParent  the parent span, or null for implicit parent
     */
    public com.tangosol.internal.tracing.Span.Builder newTracingSpan(String sStage, Object op)
        {
        // import com.tangosol.internal.tracing.Span$Type as com.tangosol.internal.tracing.Span.Type;
        // import com.tangosol.internal.tracing.Span$Builder as com.tangosol.internal.tracing.Span.Builder;
        // import com.tangosol.internal.tracing.TracingHelper;
        // import Component.Net.Message.RequestMessage;
        
        com.tangosol.internal.tracing.Span.Builder bldSpan = TracingHelper.newSpan(sStage, op)
            .withMetadata(com.tangosol.internal.tracing.Span.Type.COMPONENT.key(), getServiceName());
        
        if (op instanceof RequestMessage)
            {
            bldSpan.withMetadata("internal.message", ((RequestMessage) op).getMessageType() < 0);
            }
        
        return bldSpan;
        }
    
    /**
     * Notify all Continuations in the specified list that the backlog going to
    * normal.
     */
    public static void notifyBacklogNormal(com.tangosol.util.SafeLinkedList list)
        {
        // import com.oracle.coherence.common.base.Continuation;
        
        if (list == null)
            {
            return;
            }
        
        while (true)
            {
            Continuation cont = (Continuation) list.removeFirst();
            if (cont == null)
                {
                break;
                }
        
            try
                {
                cont.proceed(null);
                }
            catch (Throwable e)
                {
                _trace("The following exception was thrown by the backlog continuation:", 1);
                _trace(e);
                _trace("(The service thread has logged the exception and is continuing.)", 1);
                }
            }
        }
    
    /**
     * This event occurs when dependencies are injected into the component. 
    * First, call super.onDependencies to allow all super components to process
    * the Dependencies.  Each component is free to chose how it consumes
    * dependencies from within onDependencies.  Often (though not ideal), the 
    * dependencies are copied into the component's properties.  This technique
    * isolates Dependency Injection from the rest of the component code since
    * components continue to access properties just as they did before. 
    * 
    * PartitionedCacheDependencies deps = (PartitionedCacheDependencies)
    * getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        
        int cThreads = deps.getWorkerThreadCountMin();
        if (cThreads > 0)
            {
            com.tangosol.coherence.component.util.DaemonPool pool = getDaemonPool();
            pool.setDaemonCount(cThreads);
            pool.setDaemonCountMax(deps.getWorkerThreadCountMax());
            pool.setDaemonCountMin(cThreads);
            pool.setHungThreshold(deps.getTaskHungThresholdMillis());
            pool.setName(getServiceName());
            pool.setTaskTimeout(deps.getTaskTimeoutMillis());
            pool.setThreadPriority(deps.getWorkerThreadPriority());
            }
        
        setPriority(deps.getThreadPriority());
        setSerializerFactory(deps.getSerializerFactory());
        }
    
    // Declared at the super level
    /**
     * Event notification called once the daemon's thread starts and before the
    * daemon thread goes into the "wait - perform" loop. Unlike the
    * <code>onInit()</code> event, this method executes on the daemon's thread.
    * 
    * Note1: this method is called while the caller's thread is still waiting
    * for a notification to  "unblock" itself.
    * Note2: any exception thrown by this method will terminate the thread
    * immediately
     */
    protected void onEnter()
        {
        super.onEnter();
        
        resetStats();
        
        setServiceState(SERVICE_STARTED);
        }
    
    // Declared at the super level
    /**
     * This event occurs when an exception is thrown from onEnter, onWait,
    * onNotify and onExit.
    * 
    * If the exception should terminate the daemon, call stop(). The default
    * implementation prints debugging information and terminates the daemon.
    * 
    * @param e  the Throwable object (a RuntimeException or an Error)
    * 
    * @throws RuntimeException may be thrown; will terminate the daemon
    * @throws Error may be thrown; will terminate the daemon
     */
    public void onException(Throwable e)
        {
        if (getServiceState() < SERVICE_STARTED ||
                getServiceState() == SERVICE_STARTED && !isAcceptingClients())
            {
            setStartException(e);
            }
        super.onException(e);
        }
    
    // Declared at the super level
    /**
     * Event notification called right before the daemon thread terminates. This
    * method is guaranteed to be called only once and on the daemon's thread.
     */
    protected void onExit()
        {
        // import Component.Net.Security;
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        
        super.onExit();
        
        // Stop the daemon pool
        com.tangosol.coherence.component.util.DaemonPool pool = getDaemonPool();
        if (pool.isStarted())
            {
            pool.stop();
        
            // give the pool a chance to drain it's queue
            if (!pool.join(1000L))
                {
                int cActive = pool.getActiveDaemonCount();
                if (cActive > 0)
                    {
                    _trace("failed to stop " + cActive + " worker threads; abandoning", 2);
                    }
                }
            }
        
        // Set the service-state to STOPPED
        try
            {
            // if Coherence classes are deployed along with an application (as
            // opposed to an application server classpath), it's possible that by
            // this time the corresponding class loader has been invalidated and an
            // attempt to load anything that has not been yet loaded will fail (a
            // known issue with WL 8.1 and WAS 5.0); since we are exiting anyway,
            // just log the exception...
            setServiceState(SERVICE_STOPPED);
            }
        catch (Throwable e)
            {
            _trace("Exception occurred during exiting:\n " +  getStackTrace(e), 4);
            }
        
        // Stop the EventDispatcher
        // Note: this must be done after changing the service state, which could
        //       cause events to be emitted
        Service.EventDispatcher daemon = getEventDispatcher();
        if (daemon != null)
            {
            daemon.stop();
        
            // give the daemon a chance to drain its queue
            if (!daemon.join(1000L))
                {
                _trace("failed to stop " + daemon + "; abandoning", 2);
                }
            }
        
        Security security = Security.getInstance();
        if (security != null)
            {
            security.releaseSecureContext(getServiceName());
            }
        }
    
    // Declared at the super level
    /**
     * Event notification for performing low frequency periodic maintenance
    * tasks.  The interval is dictated by the WaitMillis property, 
    * 
    * This is used for tasks which have a high enough cost that it is not
    * reasonable to perform them on every call to onWait() since it could be
    * called with a high frequency in the presence of work-loads with fast
    * oscillation between onWait() and onNotify().  As an example a single
    * threaded client could produce such a load.
     */
    protected void onInterval()
        {
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        
        com.tangosol.coherence.component.util.DaemonPool pool = getDaemonPool();
        if (pool.isStarted())
            {
            pool.checkHungTasks();
            }
        
        super.onInterval();
        }
    
    /**
     * The default implementation of this method sets AcceptingClients to true.
    * If the Service has not completed preparing at this point, then the
    * Service must override this implementation and only set AcceptingClients
    * to true when the Service has actually "finished starting".
     */
    public void onServiceStarted()
        {
        setAcceptingClients(true);
        }
    
    /**
     * The default implementation of this method does nothing.
     */
    protected void onServiceStarting()
        {
        }
    
    /**
     * Called when the Service has transitioned to the specified state.
    * 
    * @param nState the new Service state
     */
    protected void onServiceState(int nState)
        {
        switch (nState)
            {
            case SERVICE_STARTING:
                onServiceStarting();
                break;
        
            case SERVICE_STARTED:
                onServiceStarted();
                break;
        
            case SERVICE_STOPPING:
                onServiceStopping();
                break;
        
            case SERVICE_STOPPED:
                onServiceStopped();
                break;
        
            default:
                _assert(false);
            }
        }
    
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopped()
        {
        setAcceptingClients(false);
        }
    
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopping()
        {
        setAcceptingClients(false);
        }
    
    /**
     * Event notification called if the start() sequence could not be completed
    * within the StartupTimeout time.
    * 
    * @see #start
     */
    protected void onStartupTimeout()
        {
        }
    
    /**
     * Parse the String value of the child XmlElement with the given name as a
    * time in milliseconds. If the specified child XmlElement does not exist or
    * is empty, the specified default value is returned.
    * 
    * @param xml the parent XmlElement
    * @param sName the name of the child XmlElement
    * @param cDefault the default value
    * 
    * @return the time (in milliseconds) represented by the specified child
    * XmlElement
     */
    protected static long parseTime(com.tangosol.run.xml.XmlElement xml, String sName, long cDefault)
        {
        // import com.tangosol.run.xml.XmlHelper;
        
        return XmlHelper.parseTime(xml, sName, cDefault);
        }
    
    /**
     * Deserialize an object from the specified BufferInput using the Service
    * Serializer.
    * 
    * @param in  the BufferInput containing a serialized object
    * 
    * @return the deserialized object
     */
    public Object readObject(com.tangosol.io.ReadBuffer.BufferInput in)
            throws java.io.IOException
        {
        return ensureSerializer().deserialize(in);
        }
    
    /**
     * Deserialize an object from the specified DataInput.
    * 
    * @param in  the DataInput containing a serialized object
    * 
    * @return the deserialized object
     */
    public Object readObject(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.io.ReadBuffer$BufferInput as com.tangosol.io.ReadBuffer.BufferInput;
        // import com.tangosol.io.WrapperBufferInput;
        
        return ensureSerializer().deserialize(in instanceof com.tangosol.io.ReadBuffer.BufferInput ?
            ((com.tangosol.io.ReadBuffer.BufferInput) in) : new WrapperBufferInput(in, getContextClassLoader()));
        }
    
    /**
     * Unregister the given EventListener from this Service.
    * 
    * @param l the EventListener to unregister
    * 
    * @see #addEventListener
     */
    protected void removeEventListener(java.util.EventListener l)
        {
        getEventListeners().remove(l);
        }
    
    // From interface: com.tangosol.util.Service
    public void removeServiceListener(com.tangosol.util.ServiceListener l)
        {
        removeEventListener(l);
        }
    
    /**
     * Reset the Service statistics.
     */
    public void resetStats()
        {
        // import com.tangosol.util.Base;
        
        getDaemonPool().resetStats();
        setStatsCpu(0L);
        setStatsReceived(0L);
        setStatsReset(Base.getSafeTimeMillis());
        
        Service.EventDispatcher dispatcher = getEventDispatcher();
        if (dispatcher != null)
            {
            dispatcher.resetStats();
            }
        }
    
    // Accessor for the property "AcceptingClients"
    /**
     * Setter for property AcceptingClients.<p>
    * Set to true when the Service has advanced to the state at which it can
    * accept requests from client threads.
     */
    public synchronized void setAcceptingClients(boolean fAccepting)
        {
        __m_AcceptingClients = (fAccepting);
        
        // free any blocked client threads
        notifyAll();
        }
    
    // From interface: com.tangosol.util.Service
    // Accessor for the property "ContextClassLoader"
    /**
     * Setter for property ContextClassLoader.<p>
    * @see com.tangosol.io.ClassLoaderAware
     */
    public void setContextClassLoader(ClassLoader loader)
        {
        if (getContextClassLoader() != loader)
            {
            __m_ContextClassLoader = (loader);
        
            if (getSerializer() != null)
                {
                // re-initialize the "main" Service Serializer
                setSerializer(instantiateSerializer(loader));
                }
            }
        }
    
    // Accessor for the property "DaemonPool"
    /**
     * Setter for property DaemonPool.<p>
    * The daemon pool.
    * 
    * @see #configure(XmlElement)
     */
    protected void setDaemonPool(com.tangosol.coherence.component.util.DaemonPool pool)
        {
        __m_DaemonPool = pool;
        }
    
    // Accessor for the property "Dependencies"
    /**
     * Inject the Dependencies object into the component.  First clone the
    * dependencies, then validate the cloned copy.  Note that the validate
    * method may modify the cloned dependencies, so it is important to use the
    * cloned dependencies for all subsequent operations.  Once the dependencies
    * have been validated, call onDependencies so that each Component in the
    * class hierarchy can process the dependencies as needed.  
    * 
    * NOTE: This method is final and it is not intended that derived components
    * intercept the call to setDependencies.  Instead they should hook in via
    * cloneDependencies and onDependencies.
     */
    public final void setDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        if (getDependencies() != null)
            {
            throw new IllegalStateException("Dependencies already set");
            }
        
        if (getServiceState() > SERVICE_INITIAL)
            {
            throw new IllegalStateException("Service has already been initialized");
            }
        
        __m_Dependencies = (cloneDependencies(deps).validate());
        
        // use the cloned dependencies
        onDependencies(getDependencies());
        }
    
    // Accessor for the property "EventDispatcher"
    /**
     * Setter for property EventDispatcher.<p>
    * The event dispatcher daemon.
     */
    private void setEventDispatcher(Service.EventDispatcher daemon)
        {
        __m_EventDispatcher = daemon;
        }
    
    // Accessor for the property "EventListeners"
    /**
     * Setter for property EventListeners.<p>
    * The list of registered EventListeners.
    * 
    * @see #addEventListener and #removeEventListener
     */
    private void setEventListeners(com.tangosol.util.Listeners listeners)
        {
        __m_EventListeners = listeners;
        }
    
    // Accessor for the property "OperationalContext"
    /**
     * Setter for property OperationalContext.<p>
    * The OperationalContext for this Service.
     */
    public void setOperationalContext(com.tangosol.net.OperationalContext ctx)
        {
        _assert(getOperationalContext() == null);
        
        __m_OperationalContext = (ctx);
        }
    
    // Accessor for the property "Serializer"
    /**
     * Setter for property Serializer.<p>
    * The cached Serializer instance for the Service ContextClassLoader.
     */
    protected void setSerializer(com.tangosol.io.Serializer serializer)
        {
        __m_Serializer = serializer;
        }
    
    // Accessor for the property "SerializerFactory"
    /**
     * Setter for property SerializerFactory.<p>
    * The SerializerFactory used by this Service.
     */
    public void setSerializerFactory(com.tangosol.io.SerializerFactory factory)
        {
        __m_SerializerFactory = factory;
        }
    
    // Accessor for the property "SerializerMap"
    /**
     * Setter for property SerializerMap.<p>
    * A Map of configured Serializer instances, keyed by ClassLoader (if
    * applicable).
     */
    protected void setSerializerMap(java.util.Map map)
        {
        __m_SerializerMap = map;
        }
    
    // Accessor for the property "ServiceConfig"
    /**
     * Setter for property ServiceConfig.<p>
    * Original XML configuration that was supplied to the Service; may be null.
     */
    public void setServiceConfig(com.tangosol.run.xml.XmlElement xml)
        {
        if (getServiceState() != SERVICE_INITIAL)
            {
            throw new IllegalStateException(
                "Configuration cannot be specified once the service has been started: " + this);
            }
        
        __m_ServiceConfig = (xml);
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Setter for property ServiceName.<p>
    * The name of this Service.
     */
    public void setServiceName(String sName)
        {
        _assert(!isStarted());
        __m_ServiceName = (sName);
        }
    
    // Accessor for the property "ServiceState"
    /**
     * Setter for property ServiceState.<p>
    * The state of the Service; one of the SERVICE_ enums.
    * 
    * @volatile as of 12.1.3
     */
    public void setServiceState(int nState)
        {
        // import com.tangosol.util.ServiceEvent;
        
        int nPrevState;
        synchronized (this)
            {
            nPrevState = getServiceState();
            if (nState > nPrevState)
                {
                __m_ServiceState = (nState);
                }
            }
        
        if (nState > nPrevState)
            {
            updateServiceThreadName();
        
            // this is going to unblock client threads
            onServiceState(nState);
            switch (nState)
                {
                case SERVICE_STARTING:
                    dispatchServiceEvent(ServiceEvent.SERVICE_STARTING);
                    break;
        
                case SERVICE_STARTED:
                    dispatchServiceEvent(ServiceEvent.SERVICE_STARTED);
                    break;
        
                case SERVICE_STOPPING:
                    dispatchServiceEvent(ServiceEvent.SERVICE_STOPPING);
                    break;
        
                case SERVICE_STOPPED:
                    dispatchServiceEvent(ServiceEvent.SERVICE_STOPPED);
                    break;
        
                default:
                    _assert(false);
                }
            }
        else
            {
            _assert(nState == nPrevState);
            }
        }
    
    // Accessor for the property "StartupTimeout"
    /**
     * Setter for property StartupTimeout.<p>
    * The time (in millis) that limits the waiting period during the service
    * startup sequence. Non-positive numbers indicate no timeout.
     */
    public void setStartupTimeout(long cMillis)
        {
        __m_StartupTimeout = cMillis;
        }
    
    // Accessor for the property "StatsCpu"
    /**
     * Setter for property StatsCpu.<p>
    * Statistics: total time spent while processing messages.
     */
    protected void setStatsCpu(long cMillis)
        {
        __m_StatsCpu = cMillis;
        }
    
    // Accessor for the property "StatsReceived"
    /**
     * Setter for property StatsReceived.<p>
    * Statistics: total number of received messages.
     */
    protected void setStatsReceived(long cMsgs)
        {
        __m_StatsReceived = cMsgs;
        }
    
    // Accessor for the property "StatsReset"
    /**
     * Setter for property StatsReset.<p>
    * Statistics: Date/time value that the stats have been reset.
     */
    protected void setStatsReset(long lMillis)
        {
        __m_StatsReset = lMillis;
        }
    
    // From interface: com.tangosol.util.Service
    /**
     * Stop the Service. The default implementation of this method simply calls
    * stop().
     */
    public synchronized void shutdown()
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.util.Base;
        
        stop();
        
        // as of Coherence 3.5, this method will not return until the service
        // has actually stopped
        
        if (getThread() != Thread.currentThread())
            {
            while (getDaemonState() != DAEMON_EXITED)
                {
                try
                    {
                    Blocking.wait(this, 1000L);
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupted();
                    throw Base.ensureRuntimeException(e);
                    }
                }
        
            // wait for a bounded amount of time for the event dispatcher to drain
            waitForEventDispatcher();
            }
        }
    
    // From interface: com.tangosol.util.Service
    // Declared at the super level
    /**
     * Starts the daemon thread associated with this component. If the thread is
    * already starting or has started, invoking this method has no effect.
    * 
    * Synchronization is used here to verify that the start of the thread
    * occurs; the lock is obtained before the thread is started, and the daemon
    * thread notifies back that it has started from the run() method.
     */
    public synchronized void start()
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.WrapperException;
        
        _assert(getServiceState() <= SERVICE_STARTED,
            "Service restart is illegal (ServiceName=" + getServiceName() + ')');
        
        super.start();
        
        long cTimeout  = getStartupTimeout();
        long ldtCutoff = cTimeout > 0 ? Base.getSafeTimeMillis() + cTimeout : Long.MAX_VALUE;
        
        while (isStarted() && getServiceState() <= SERVICE_STARTED
                && !isAcceptingClients())
            {
            try
                {
                Blocking.wait(this, 1000L);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new WrapperException(e);
                }
        
            if (Base.getSafeTimeMillis() > ldtCutoff)
                {
                onStartupTimeout();
                break;
                }
            }
        
        if (getServiceState() != SERVICE_STARTED)
            {
            Throwable e = getStartException();
            String    s = "Failed to start Service \"" + getServiceName()
                + "\" (ServiceState=" + getServiceStateName() + ')';
            throw e == null ? new RuntimeException(s) : new WrapperException(e, s);
            }
        }
    
    // From interface: com.tangosol.util.Service
    // Declared at the super level
    /**
     * Hard-stop the Service. Use shutdown() for normal  termination.
     */
    public void stop()
        {
        if (!isStarted())
            {
            synchronized (this)
                {
                if (!isStarted())
                    {
                    // service is not running ... don't worry about whether
                    // it has never been running vs. whether it started and
                    // stopped ... just set the state to register that stop
                    // was called so no one can later start it
                    setServiceState(SERVICE_STOPPED);
                    return;
                    }
                }
            }
        
        super.stop();
        
        if (getThread() == Thread.currentThread())
            {
            setServiceState(SERVICE_STOPPED);
            }
        }
    
    // Declared at the super level
    public String toString()
        {
        StringBuffer sb = new StringBuffer();
        
        sb.append(get_Name())
          .append("{Name=")
          .append(getServiceName())
          .append(", State=")
          .append("(")
          .append(getServiceStateName())
          .append(')');
        
        String sDesc = getDescription();
        if (sDesc != null && sDesc.length() > 0)
            {
            sb.append(", ")
              .append(sDesc);
            }
        
        sb.append('}');
        
        return sb.toString();
        }
    
    /**
     * *** LICENSE ***
    * WARNING: This method name is obfuscated.
    * 
    * decrypts a "ticked string" (i.e. one that will NOT show up verbatim in
    * the class file)
     */
    public String toString(String s)
        {
        return com.tangosol.util.HashHelper.hash(s);
        }
    
    /**
     * Update the service thread name with any additional information that could
    * be useful for thread dump analysis. This method is usually called every
    * time when a service state changes.
    * 
    * @see #setServiceState
     */
    protected void updateServiceThreadName()
        {
        Thread thread = getThread();
        if (thread != null)
            {
            try
                {
                thread.setName(getDecoratedThreadName());
                }
            catch (Throwable e) {}
            }
        }
    
    /**
     * Block the calling thread until the Service has advanced to the state at
    * which it can accept requests from client threads.
     */
    public void waitAcceptingClients()
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.util.WrapperException;
        
        while (!isAcceptingClients())
            {
            synchronized (this)
                {
                if (getServiceState() > SERVICE_STARTED)
                    {
                    Throwable e = getStartException();
                    String    s = "Failed to start Service \"" + getServiceName()
                        + "\" (ServiceState=" + getServiceStateName() + ')';
                    throw e == null ? new RuntimeException(s) : new WrapperException(e, s);
                    }
        
                if (!isAcceptingClients())
                    {
                    try
                        {
                        Blocking.wait(this);
                        }
                    catch (InterruptedException e)
                        {
                        Thread.currentThread().interrupt();
                        throw new WrapperException(e);
                        }
                    }
                }
            }
        }
    
    /**
     * Block the calling thread until either the Service's EventDispatcher
    * thread has drained its queue of events or 5s has passed.
     */
    protected void waitForEventDispatcher()
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.util.Base;
        
        // wait for all events to be dispatched prior to returning control
        Service.EventDispatcher dispatcher = (Service.EventDispatcher) getEventDispatcher();
        if (dispatcher != null)
            {
            try
                {
                dispatcher.drainOverflow(5000L, 1);
        
                // wait for the final event to be processed
                if (dispatcher.isDispatching())
                    {
                    Blocking.sleep(1000L);
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw Base.ensureRuntimeException(e);
                }
            }
        }
    
    /**
     * Serialize an object to the specified BufferOutput using the Service
    * Serializer.
    * 
    * @param out  the BufferOutput
    * @param o  the object to serialize
     */
    public void writeObject(com.tangosol.io.WriteBuffer.BufferOutput out, Object o)
            throws java.io.IOException
        {
        ensureSerializer().serialize(out, o);
        }
    
    /**
     * Serialize an object to the specified DataOutput using the Service
    * Serializer.
    * 
    * @param out  the DataOutput
    * @param o  the object to serialize
     */
    public void writeObject(java.io.DataOutput out, Object o)
            throws java.io.IOException
        {
        // import com.tangosol.io.WrapperBufferOutput;
        // import com.tangosol.io.WriteBuffer$BufferOutput as com.tangosol.io.WriteBuffer.BufferOutput;
        
        ensureSerializer().serialize(out instanceof com.tangosol.io.WriteBuffer.BufferOutput ?
            ((com.tangosol.io.WriteBuffer.BufferOutput) out) : new WrapperBufferOutput(out), o);
        }
    
    /**
     * This method is a part of "Serializable" pseudo-interface.
    * 
    * All Service are inherently non-serializable.
     */
    private void writeObject(java.io.ObjectOutputStream out)
            throws java.io.IOException
        {
        throw new UnsupportedOperationException("Service is not serializable: " + this);
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$DaemonPool
    
    /**
     * DaemonPool is a class thread pool implementation for processing queued
     * operations on one or more daemon threads.
     * 
     * The designable properties are:
     *     AutoStart
     *     DaemonCount
     * 
     * The simple API for the DaemonPool is:
     *     public void start()
     *     public boolean isStarted()
     *     public void add(Runnable task)
     *     public void stop()
     * 
     * The advanced API for the DaemonPool is:
     *     DaemonCount property
     *     Daemons property
     *     Queues property
     *     ThreadGroup property
     * 
     * The DaemonPool is composed of two key components:
     * 
     * 1) An array of WorkSlot components that may or may not share Queues with
     * other WorkSlots. 
     * 
     * 2) An array of Daemon components feeding off the Queues. This collection
     * is accessed by the DaemonCount and Daemons properties, and is managed by
     * the DaemonCount mutator.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DaemonPool
            extends    com.tangosol.coherence.component.util.DaemonPool
        {
        // ---- Fields declarations ----
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Daemon", Service.DaemonPool.Daemon.get_CLASS());
            __mapChildren.put("ResizeTask", Service.DaemonPool.ResizeTask.get_CLASS());
            __mapChildren.put("ScheduleTask", Service.DaemonPool.ScheduleTask.get_CLASS());
            __mapChildren.put("StartTask", Service.DaemonPool.StartTask.get_CLASS());
            __mapChildren.put("StopTask", Service.DaemonPool.StopTask.get_CLASS());
            __mapChildren.put("WorkSlot", Service.DaemonPool.WorkSlot.get_CLASS());
            __mapChildren.put("WrapperTask", Service.DaemonPool.WrapperTask.get_CLASS());
            }
        
        // Default constructor
        public DaemonPool()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DaemonPool(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            
            // state initialization: public and protected properties
            try
                {
                setAbandonThreshold(8);
                setDaemonCountMax(2147483647);
                setDaemonCountMin(1);
                setScheduledTasks(new java.util.HashSet());
                setStatsTaskAddCount(new java.util.concurrent.atomic.AtomicLong());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$DaemonPool".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Getter for property Name.<p>
        * The name of this DaemonPool.
         */
        public String getName()
            {
            String sName = super.getName();
            
            if (sName == null)
                {
                sName = ((Service) get_Module()).getServiceName() + "Pool";
                }
            
            return sName;
            }
        
        // Declared at the super level
        /**
         * Halt the thread pool, halting all worker daemons.  Halt can be called
        * even if the pool is not started (e.g. before it is started or after
        * it is stopped); in that case it will have no effect.  This will bring
        * down the DaemonPool in a forceful and ungraceful manner.
        * 
        * This method should not synchronize or block in any way.
        * This method may not return.
         */
        public void halt()
            {
            super.halt();
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$DaemonPool$Daemon
        
        /**
         * The prototypical Daemon thread component that will belong to the
         * DaemonPool. An instance of this component is created for each thread
         * in the pool.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Daemon
                extends    com.tangosol.coherence.component.util.DaemonPool.Daemon
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Daemon()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Daemon(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                
                // state initialization: public and protected properties
                try
                    {
                    setDaemonState(0);
                    setDefaultGuardRecovery(0.9F);
                    setDefaultGuardTimeout(60000L);
                    setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
                    setThreadName("Worker");
                    }
                catch (java.lang.Exception e)
                    {
                    // re-throw as a runtime exception
                    throw new com.tangosol.util.WrapperException(e);
                    }
                
                // containment initialization: children
                _addChild(new Service.DaemonPool.Daemon.Guard("Guard", this, true), "Guard");
                
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.Daemon();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$DaemonPool$Daemon".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * Instantiate a new thread that will be used by this Daemon.
             */
            protected Thread instantiateThread()
                {
                Service service = (Service) get_Module();
                
                Thread thread = super.instantiateThread();
                
                // use the service classloader for the Daemon thread
                thread.setContextClassLoader(service.getContextClassLoader());
                
                return thread;
                }
            
            // Declared at the super level
            /**
             * This event occurs when an exception is thrown from onEnter,
            * onWait, onNotify and onExit.
            * 
            * If the exception should terminate the daemon, call stop(). The
            * default implementation prints debugging information and
            * terminates the daemon.
            * 
            * @param e  the Throwable object (a RuntimeException or an Error)
            * 
            * @throws RuntimeException may be thrown; will terminate the daemon
            * @throws Error may be thrown; will terminate the daemon
             */
            protected void onException(Throwable e)
                {
                if (isExiting())
                    {
                    super.onException(e);
                    }
                else
                    {
                    // unhandled exception on a daemon could be as fatal
                    // as on the service itself
                    ((Service) get_Module()).onException(e);
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$DaemonPool$Daemon$Guard
            
            /**
             * Guard provides the Guardable interface implementation for the
             * Daemon.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Guard
                    extends    com.tangosol.coherence.component.util.DaemonPool.Daemon.Guard
                {
                // ---- Fields declarations ----
                private static com.tangosol.util.ListMap __mapChildren;
                
                // Static initializer
                static
                    {
                    __initStatic();
                    }
                
                // Default static initializer
                private static void __initStatic()
                    {
                    // register child classes
                    __mapChildren = new com.tangosol.util.ListMap();
                    __mapChildren.put("Abandon", Service.DaemonPool.Daemon.Guard.Abandon.get_CLASS());
                    }
                
                // Default constructor
                public Guard()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Guard(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    
                    
                    // containment initialization: children
                    
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.Daemon.Guard();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$DaemonPool$Daemon$Guard".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$DaemonPool$Daemon$Guard$Abandon
                
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Abandon
                        extends    com.tangosol.coherence.component.util.DaemonPool.Daemon.Guard.Abandon
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Abandon()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Abandon(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.Daemon.Guard.Abandon();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$DaemonPool$Daemon$Guard$Abandon".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$DaemonPool$ResizeTask
        
        /**
         * Runnable periodic task used to implement the dynamic resizing
         * algorithm.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class ResizeTask
                extends    com.tangosol.coherence.component.util.DaemonPool.ResizeTask
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public ResizeTask()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public ResizeTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.ResizeTask();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$DaemonPool$ResizeTask".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$DaemonPool$ScheduleTask
        
        /**
         * Runnable task that is used to schedule a task to be added to the
         * DaemonPool.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class ScheduleTask
                extends    com.tangosol.coherence.component.util.DaemonPool.ScheduleTask
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public ScheduleTask()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public ScheduleTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.ScheduleTask();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$DaemonPool$ScheduleTask".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$DaemonPool$StartTask
        
        /**
         * Runnable pseudo-task that is used to start one and only one daemon
         * thread.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class StartTask
                extends    com.tangosol.coherence.component.util.DaemonPool.StartTask
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public StartTask()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public StartTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.StartTask();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$DaemonPool$StartTask".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$DaemonPool$StopTask
        
        /**
         * Runnable pseudo-task that is used to terminate one and only one
         * daemon thread.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class StopTask
                extends    com.tangosol.coherence.component.util.DaemonPool.StopTask
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public StopTask()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public StopTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.StopTask();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$DaemonPool$StopTask".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$DaemonPool$WorkSlot
        
        /**
         * To reduce the contention across the worker threads, all tasks added
         * to the DaemonPool are directed to one of the WorkSlots in a way that
         * respects the association between tasks. The total number of slots is
         * fixed and calculated based on the number of processors. Depending on
         * the number of daemon threads, different slots may share the queues.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class WorkSlot
                extends    com.tangosol.coherence.component.util.DaemonPool.WorkSlot
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public WorkSlot()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public WorkSlot(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                
                // state initialization: public and protected properties
                try
                    {
                    setIndex(-1);
                    }
                catch (java.lang.Exception e)
                    {
                    // re-throw as a runtime exception
                    throw new com.tangosol.util.WrapperException(e);
                    }
                
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.WorkSlot();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$DaemonPool$WorkSlot".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$DaemonPool$WrapperTask
        
        /**
         * A task that is used to wrap the actual tasks.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class WrapperTask
                extends    com.tangosol.coherence.component.util.DaemonPool.WrapperTask
            {
            // ---- Fields declarations ----
            
            /**
             * Property ParentTracingSpan
             *
             * The parent span.
             * 
             * @volatile
             */
            private volatile com.tangosol.internal.tracing.Span __m_ParentTracingSpan;
            
            // Default constructor
            public WrapperTask()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public WrapperTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.WrapperTask();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$DaemonPool$WrapperTask".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Accessor for the property "ParentTracingSpan"
            /**
             * Getter for property ParentTracingSpan.<p>
            * The parent span.
            * 
            * @volatile
             */
            public com.tangosol.internal.tracing.Span getParentTracingSpan()
                {
                return __m_ParentTracingSpan;
                }
            
            // Declared at the super level
            /**
             * The "component has been initialized" method-notification called
            * out of setConstructed() for the topmost component and that in
            * turn notifies all the children.
            * 
            * This notification gets called before the control returns back to
            * this component instantiator (using <code>new Component.X()</code>
            * or <code>_newInstance(sName)</code>) and on the same thread. In
            * addition, visual components have a "posted" notification
            * <code>onInitUI</code> that is called after (or at the same time
            * as) the control returns back to the instantiator and possibly on
            * a different thread.
             */
            public void onInit()
                {
                // import com.tangosol.internal.tracing.TracingHelper;
                
                super.onInit();
                
                setParentTracingSpan(TracingHelper.getActiveSpan());
                }
            
            // Declared at the super level
            public void run()
                {
                // import com.tangosol.internal.tracing.Scope;
                // import com.tangosol.internal.tracing.Span;
                // import com.tangosol.internal.tracing.TracingHelper;
                
                Span span = ((Service) get_Module()).newTracingSpan("process", getTask())
                                    .setParent(getParentTracingSpan())
                                    .startSpan();
                
                Scope scope = TracingHelper.getTracer().withSpan(span);
                try
                    {
                    super.run();
                    }
                catch (RuntimeException e)
                    {
                    TracingHelper.augmentSpanWithErrorDetails(span, true, e);
                    throw e;
                    }
                finally
                    {
                    scope.close();
                    span.end();
                    }
                }
            
            // Accessor for the property "ParentTracingSpan"
            /**
             * Setter for property ParentTracingSpan.<p>
            * The parent span.
            * 
            * @volatile
             */
            public void setParentTracingSpan(com.tangosol.internal.tracing.Span spanTracing)
                {
                __m_ParentTracingSpan = spanTracing;
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$DispatchEvent
    
    /**
     * Runnable event.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DispatchEvent
            extends    com.tangosol.coherence.component.Util
            implements Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Event
         *
         * The event to dispatch.
         */
        private java.util.EventObject __m_Event;
        
        /**
         * Property Listeners
         *
         * The array of EventListeners to dispatch to.
         */
        private java.util.EventListener[] __m_Listeners;
        
        // Default constructor
        public DispatchEvent()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DispatchEvent(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DispatchEvent();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$DispatchEvent".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "Event"
        /**
         * Getter for property Event.<p>
        * The event to dispatch.
         */
        public java.util.EventObject getEvent()
            {
            return __m_Event;
            }
        
        // Accessor for the property "Listeners"
        /**
         * Getter for property Listeners.<p>
        * The array of EventListeners to dispatch to.
         */
        public java.util.EventListener[] getListeners()
            {
            return __m_Listeners;
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instantiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            // no-op: no children
            }
        
        // From interface: java.lang.Runnable
        /**
         * import com.tangosol.net.MemberEvent;
        * 
        * ((MemberEvent) getEvent()).dispatch(getListeners());
         */
        public void run()
            {
            // import com.tangosol.util.ServiceEvent;
            
            ((ServiceEvent) getEvent()).dispatch(getListeners());
            }
        
        // Accessor for the property "Event"
        /**
         * Setter for property Event.<p>
        * The event to dispatch.
         */
        public void setEvent(java.util.EventObject evt)
            {
            __m_Event = evt;
            }
        
        // Accessor for the property "Listeners"
        /**
         * Setter for property Listeners.<p>
        * The array of EventListeners to dispatch to.
         */
        public void setListeners(java.util.EventListener[] listeners)
            {
            __m_Listeners = listeners;
            }
        
        // Declared at the super level
        public String toString()
            {
            return get_Name() + ": " + getEvent();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$EventDispatcher
    
    /**
     * This is a Daemon component that waits for items to process from a Queue.
     * Whenever the Queue contains an item, the onNotify event occurs. It is
     * expected that sub-classes will process onNotify as follows:
     * <pre><code>
     * Object o;
     * while ((o = getQueue().removeNoWait()) != null)
     *     {
     *     // process the item
     *     // ...
     *     }
     * </code></pre>
     * <p>
     * The Queue is used as the synchronization point for the daemon.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class EventDispatcher
            extends    com.tangosol.coherence.component.util.daemon.QueueProcessor
        {
        // ---- Fields declarations ----
        
        /**
         * Property BacklogContinuations
         *
         * A list of Continuations that need to be called when the event
         * backlog goes back to normal.
         * 
         * @volatile - double-check lazy initialization
         */
        private volatile com.tangosol.util.SafeLinkedList __m_BacklogContinuations;
        
        /**
         * Property CloggedCount
         *
         * The maximum number of events in the queue before determining that
         * the dispatcher is clogged. Zero means no limit.
         */
        private int __m_CloggedCount;
        
        /**
         * Property CloggedDelay
         *
         * The number of milliseconds to pause client threads when a clog
         * occurs, to wait for the clog to dissipate. (The pause is repeated
         * until the clog is gone.) Anything less than one (e.g. zero) is
         * treated as one.
         */
        private int __m_CloggedDelay;
        
        /**
         * Property Dispatching
         *
         * Set to true while the EventDispatcher daemon thread is in the
         * process of dispatching events.
         * 
         * @volatile
         */
        private volatile boolean __m_Dispatching;
        
        /**
         * Property EventCount
         *
         * The number of events processed by this EventDispatcher since the
         * last call to resetStats().
         * 
         * @volatile
         */
        private volatile long __m_EventCount;
        
        /**
         * Property LastLogTime
         *
         * The timestamp at which the last queue stuck warning was logged.
         */
        private transient long __m_LastLogTime;
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Queue", Service.EventDispatcher.Queue.get_CLASS());
            }
        
        // Default constructor
        public EventDispatcher()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public EventDispatcher(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            
            // state initialization: public and protected properties
            try
                {
                setCloggedCount(1024);
                setCloggedDelay(32);
                setDaemonState(0);
                setDefaultGuardRecovery(0.9F);
                setDefaultGuardTimeout(60000L);
                setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new Service.EventDispatcher.Guard("Guard", this, true), "Guard");
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$EventDispatcher".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        /**
         * Check for the event backlog.
        * 
        * @see com.tangosol.net.FlowControl for the semantics of the
        * Continuation
         */
        public boolean checkBacklog(com.oracle.coherence.common.base.Continuation continuation)
            {
            // import com.tangosol.util.SafeLinkedList;
            
            long cMaxEvents = getCloggedCount();
            
            if (cMaxEvents > 0 && getQueue().size() > cMaxEvents)
                {
                if (continuation != null)
                    {
                    SafeLinkedList list = getBacklogContinuations();
                    if (list == null)
                        {
                        synchronized (this)
                            {
                            list = getBacklogContinuations();
                            if (list == null)
                                {
                                setBacklogContinuations(list = new SafeLinkedList());
                                }
                            }
                        }
            
                    list.add(continuation);
            
                    // check is the service is still running (volatile)
                    // *after* crossing the write-barrier above;
                    // the logic in onExit() checks those same barriers in the inverse order
                    Service service = (Service) get_Module();
                    if (service.getServiceState() == Service.SERVICE_STOPPED)
                        {
                        // no need to add it, but it could have been aready called
                        // by the notifyBacklogNormal()
                        return !list.remove(continuation);
                        }
                    }
            
                return true;
                }
            
            return false;
            }
        
        /**
         * Wait for the event queue to drain any backlog.
        * 
        * @param cMillisTimeout  the maximum amount of time to wait, or 0 for
        * indefinite
        * 
        * @return the amount of time remaining in the timeout, or 0 for
        * indefinite
        * 
        * @throw RequestTimeoutException on timeout
         */
        public long drainOverflow(long cMillisTimeout)
                throws java.lang.InterruptedException
            {
            return drainOverflow(cMillisTimeout, getCloggedCount());
            }
        
        /**
         * Wait for the event queue to drain any backlog.
        * 
        * @param cMillisTimeout  the maximum amount of time to wait, or 0 for
        * indefinite
        * @param cMaxEvents       the maximum number of events in the queue
        * before determining that the dispatcher is clogged
        * 
        * @return the amount of time remaining in the timeout, or 0 for
        * indefinite
        * 
        * @throw RequestTimeoutException on timeout
         */
        public long drainOverflow(long cMillisTimeout, int cMaxEvents)
                throws java.lang.InterruptedException
            {
            return cMaxEvents <= 0 || getQueue().size() < cMaxEvents
                ? cMillisTimeout // common case; no overflow
                : drainOverflowComplex(cMillisTimeout, cMaxEvents);
            }
        
        /**
         * Wait for the event queue to drain any backlog.
        * 
        * @param cMillisTimeout  the maximum amount of time to wait, or 0 for
        * indefinite
        * 
        * @return the amount of time remaining in the timeout, or 0 for
        * indefinite
        * 
        * @throw RequestTimeoutException on timeout
         */
        protected long drainOverflowComplex(long cMillisTimeout, int cMaxEvents)
                throws java.lang.InterruptedException
            {
            // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
            // import com.oracle.coherence.common.base.Blocking;
            // import com.tangosol.net.GuardSupport;
            // import com.tangosol.net.RequestTimeoutException;
            // import com.tangosol.util.Base;
            
            if (Thread.currentThread() == getThread())
                {
                // It is "allowable" for client code as part
                // of processing an event to perform an operation
                // against the same service. We must not block
                // that thread from draining its own queue as a
                // result of its queue being too long.
            
                // Note: We don't make this thread exempt from
                // being blocked elsewhere, for instance if it
                // called into another service, or even for this
                // service but as part of publisher.drainOverflow.
                // Any block such as that can naturally resolve
                // itself, but blocking the draining of its own
                // queue could not naturaly resolve.
                
                return cMillisTimeout;
                }
            
            // slow down too agressive clients to prevent memory overflow
            com.tangosol.coherence.component.util.Queue  queue           = getQueue();
            Object oHead           = null;
            int    cEventsPrev     = 0;
            int    nIter           = 0;
            long   lGrowthInterval = 5000L;
            long   ldtGrowthCheck  = 0L;
            long   ldtTimeout      = 0L;
            long   cMillisDelay    = getCloggedDelay();
            
            while (isStarted())
                {
                int cEvents = queue.size();    
                if (cEvents < cMaxEvents)
                    {
                    // queue is under limit
                    break;
                    }
            
                long ldtNow = Base.getSafeTimeMillis();
                if (nIter == 0) // first pass
                    {
                    oHead          = queue.peekNoWait();
                    ldtGrowthCheck = ldtNow + lGrowthInterval;
                    cEventsPrev    = cEvents;
                    
                    if (cMillisTimeout != 0L)
                        {
                        ldtTimeout   = ldtNow + cMillisTimeout;
                        cMillisDelay = Math.min(cMillisDelay, cMillisTimeout);
                        }
                    }
                else if (cMillisTimeout != 0L) // subsequent passes
                    {
                    long cMillisLeft = ldtTimeout - ldtNow;
                    if (cMillisLeft <= 0L)
                        {
                        throw new RequestTimeoutException("Request timed out");
                        }
                    cMillisDelay = Math.min(cMillisDelay, cMillisLeft);
                    }
                
                Blocking.sleep(cMillisDelay);
            
                if (++nIter % 10 == 0)
                    {
                    Object oHeadCurrent = queue.peekNoWait();  
            
                    if (oHead == oHeadCurrent)
                        {
                        // after waiting several iterations, the event queue
                        // has made no progress; we may be stuck while holding a
                        // monitor (see COH-1424), thus log and let it go
                        Service service = (Service) get_Module();
                        if (!service.isStopped() && ldtNow > getLastLogTime() + 60000L)
                            {
                            _trace("The event queue appears to be stuck.", 2);
                            GuardSupport.logStackTraces();
                            setLastLogTime(ldtNow);
                            }
                        break;
                        }
            
                    if (ldtNow > ldtGrowthCheck)
                        {
                        if (cEvents >= cEventsPrev)
                            {
                            // if the event queue grew in size over the last interval
                            // while we are not adding to it; it may be growing out of
                            // control (faster than it can be drained).
                            _trace("The events are processed at a slower rate than they arrive."
                                 + " During the last " + lGrowthInterval + "ms, the event "
                                 + " backlog went from " + cEventsPrev + " to " + cEvents, 2);
                            }
                        ldtGrowthCheck = ldtNow + lGrowthInterval;
                        cEventsPrev    = cEvents;
                        }
                    
                    // we made some progress, but the queue is still backed up;
                    // re-peek the head element and continue draining.
                    oHead = oHeadCurrent;
                    }
                }
            
            // non-timeout return the amount of time left
            return cMillisTimeout == 0L
                ? 0L
                : Math.max(1L, ldtTimeout - Base.getSafeTimeMillis());
            }
        
        /**
         * Check if Continuations waiting for the backlog going to normal need
        * to be notified.
         */
        protected void evaluateBacklog()
            {
            // import com.tangosol.util.SafeLinkedList;
            
            SafeLinkedList list = getBacklogContinuations();
            if (list != null && !list.isEmpty())
                {
                // to avoid thrashing, consider the "normal" threshold
                // being 3/4 of the CloggedCount
                long cNormal = (getCloggedCount() >> 2) * 3;
            
                if (getQueue().size() < cNormal)
                    {
                    Service.notifyBacklogNormal(list);
                    }
                }
            }
        
        // Accessor for the property "BacklogContinuations"
        /**
         * Getter for property BacklogContinuations.<p>
        * A list of Continuations that need to be called when the event backlog
        * goes back to normal.
        * 
        * @volatile - double-check lazy initialization
         */
        public com.tangosol.util.SafeLinkedList getBacklogContinuations()
            {
            return __m_BacklogContinuations;
            }
        
        // Accessor for the property "CloggedCount"
        /**
         * Getter for property CloggedCount.<p>
        * The maximum number of events in the queue before determining that the
        * dispatcher is clogged. Zero means no limit.
         */
        public int getCloggedCount()
            {
            return __m_CloggedCount;
            }
        
        // Accessor for the property "CloggedDelay"
        /**
         * Getter for property CloggedDelay.<p>
        * The number of milliseconds to pause client threads when a clog
        * occurs, to wait for the clog to dissipate. (The pause is repeated
        * until the clog is gone.) Anything less than one (e.g. zero) is
        * treated as one.
         */
        public int getCloggedDelay()
            {
            return __m_CloggedDelay;
            }
        
        // Accessor for the property "EventCount"
        /**
         * Getter for property EventCount.<p>
        * The number of events processed by this EventDispatcher since the last
        * call to resetStats().
        * 
        * @volatile
         */
        public long getEventCount()
            {
            return __m_EventCount;
            }
        
        // Accessor for the property "LastLogTime"
        /**
         * Getter for property LastLogTime.<p>
        * The timestamp at which the last queue stuck warning was logged.
         */
        public long getLastLogTime()
            {
            return __m_LastLogTime;
            }
        
        // Declared at the super level
        /**
         * Getter for property ThreadName.<p>
        * Specifies the name of the daemon thread. If not specified, the
        * component name will be used.
        * 
        * This property can be set at design time or runtime. If set at
        * runtime, this property must be configured before start() is invoked
        * to cause the daemon thread to have the specified name.
         */
        public String getThreadName()
            {
            return ((Service) get_Module()).getThreadName() + ':' + super.getThreadName();
            }
        
        // Declared at the super level
        /**
         * Getter for property WaitMillis.<p>
        * The number of milliseconds that the daemon will wait for
        * notification. Zero means to wait indefinitely. Negative value means
        * to skip waiting altogether.
        * 
        * @see #onWait
         */
        public long getWaitMillis()
            {
            long cWait = super.getWaitMillis();
            
            if (isGuarded() || isGuardian())
                {
                // If this Daemon is being Guarded, it should not wait for longer
                // than a fraction of the timeout.  In practice, the SLA's shuold
                // be set fairly high (higher than the packet timeout), so limiting
                // wait times to 1 sec should be sufficient.  This saves a more
                // expensive calculation to find the exact wait time which
                // profiling/testing has shown to be expensive in critical loops.
            
                long cMaxWait = 1000L;
                cWait = cWait == 0 ? cMaxWait : Math.min(cWait, cMaxWait);
                }
            return cWait;
            }
        
        // Accessor for the property "Dispatching"
        /**
         * Getter for property Dispatching.<p>
        * Set to true while the EventDispatcher daemon thread is in the process
        * of dispatching events.
        * 
        * @volatile
         */
        public boolean isDispatching()
            {
            return __m_Dispatching;
            }
        
        // Declared at the super level
        /**
         * Event notification called once the daemon's thread starts and before
        * the daemon thread goes into the "wait - perform" loop. Unlike the
        * <code>onInit()</code> event, this method executes on the daemon's
        * thread.
        * 
        * Note1: this method is called while the caller's thread is still
        * waiting for a notification to  "unblock" itself.
        * Note2: any exception thrown by this method will terminate the thread
        * immediately
         */
        protected void onEnter()
            {
            // import com.tangosol.net.GuardSupport;
            
            super.onEnter();
            
            // set the guardian-context for the service thread
            if (isGuarded())
                {
                GuardSupport.setThreadContext(getGuardable().getContext());
                }
            }
        
        // Declared at the super level
        /**
         * This event occurs when an exception is thrown from onEnter, onWait,
        * onNotify and onExit.
        * 
        * If the exception should terminate the daemon, call stop(). The
        * default implementation prints debugging information and terminates
        * the daemon.
        * 
        * @param e  the Throwable object (a RuntimeException or an Error)
        * 
        * @throws RuntimeException may be thrown; will terminate the daemon
        * @throws Error may be thrown; will terminate the daemon
         */
        protected void onException(Throwable e)
            {
            if (!isExiting())
                {
                _trace("The following exception was caught by the event dispatcher:", 1);
                _trace(e);
                _trace("(The service event thread has logged the exception and is continuing.)", 1);
                }
            }
        
        // Declared at the super level
        /**
         * Event notification called right before the daemon thread terminates.
        * This method is guaranteed to be called only once and on the daemon's
        * thread.
         */
        protected void onExit()
            {
            // drain the queue;
            // it's very important that notifyBacklogNormal() will be called by onNotify()
            // *after* the service state (volatile) has been changed to "SERVICE_STOPPED"
            
            onNotify();
            
            super.onExit();
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification (kind of
        * WM_NCCREATE event) called out of setConstructed() for the topmost
        * component and that in turn notifies all the children. <p>
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as) 
        * the control returns back to the instatiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            // import com.tangosol.coherence.config.Config;
            
            try
                {
                String sMaxEvents = Config.getProperty("coherence.events.limit");
                String sDelay     = Config.getProperty("coherence.events.delay");
            
                if (sMaxEvents != null)
                    {
                    setCloggedCount(Integer.parseInt(sMaxEvents));
                    }
                if (sDelay != null)
                    {
                    setCloggedDelay(Integer.parseInt(sDelay));
                    }
                }
            catch (Exception e) {}
            
            super.onInit();
            }
        
        // Declared at the super level
        /**
         * Event notification to perform a regular daemon activity. To get it
        * called, another thread has to set Notification to true:
        * <code>daemon.setNotification(true);</code>
        * 
        * @see #onWait
         */
        protected void onNotify()
            {
            // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
            // import com.tangosol.util.Base;
            
            super.onNotify();
            
            com.tangosol.coherence.component.util.Queue    queue = getQueue();
            Runnable task  = null;
            
            setDispatching(true);
            try
                {
                // limit the number of iterations in the "tight loop" to ensure that
                // onInterval can periodically get called when we are under heavy load
                int  cEvents        = 0;
                long cMax           = 1L;
                long cSoftTimeout   = (long) (getDefaultGuardTimeout() * getDefaultGuardRecovery());
                long ldtStartTime   = Base.getSafeTimeMillis();
                long ldtSoftTimeout = ldtStartTime + cSoftTimeout;
                do
                    {
                    task = (Runnable) queue.removeNoWait();
                    if (task == null)
                        {
                        Service.notifyBacklogNormal(getBacklogContinuations());
                        break;
                        }
                    task.run();
            
                    long ldtNow = Base.getSafeTimeMillis();
                         cMax   = Math.max(cMax, ldtNow - ldtStartTime);
            
                    if (ldtNow + (cMax << 2) > ldtSoftTimeout)
                        {
                        heartbeat();
                        ldtSoftTimeout = ldtNow + cSoftTimeout;
                        }
            
                    ldtStartTime = ldtNow;
                    }
                while (++cEvents < 512);
            
                if (cEvents > 0)
                    {
                    setEventCount(getEventCount() + cEvents);
                    }
                
                evaluateBacklog();
                }
            catch (Throwable e)
                {
                if (((Service) get_Module()).isRunning())
                    {
                    _trace("An exception occurred while dispatching the following event:\n"
                        + String.valueOf(task), 1);
                    onException(e);
                    }
                }
            finally
                {
                setDispatching(false);
                }
            }
        
        // Declared at the super level
        /**
         * Event notification called when  the daemon's Thread is waiting for
        * work.
        * 
        * @see #run
         */
        protected void onWait()
                throws java.lang.InterruptedException
            {
            if (isGuarded())
                {
                // send a heartbeat before going into a wait
                heartbeat();
                }
            
            super.onWait();
            }
        
        /**
         * Reset the EventCount attribute. 
         */
        public void resetStats()
            {
            setEventCount(0L);
            }
        
        // Accessor for the property "BacklogContinuations"
        /**
         * Setter for property BacklogContinuations.<p>
        * A list of Continuations that need to be called when the event backlog
        * goes back to normal.
        * 
        * @volatile - double-check lazy initialization
         */
        protected void setBacklogContinuations(com.tangosol.util.SafeLinkedList listContinuations)
            {
            __m_BacklogContinuations = listContinuations;
            }
        
        // Accessor for the property "CloggedCount"
        /**
         * Setter for property CloggedCount.<p>
        * The maximum number of events in the queue before determining that the
        * dispatcher is clogged. Zero means no limit.
         */
        public void setCloggedCount(int cMaxEvents)
            {
            __m_CloggedCount = cMaxEvents;
            }
        
        // Accessor for the property "CloggedDelay"
        /**
         * Setter for property CloggedDelay.<p>
        * The number of milliseconds to pause client threads when a clog
        * occurs, to wait for the clog to dissipate. (The pause is repeated
        * until the clog is gone.) Anything less than one (e.g. zero) is
        * treated as one.
         */
        public void setCloggedDelay(int cMillis)
            {
            __m_CloggedDelay = (Math.max(1, cMillis));
            }
        
        // Accessor for the property "Dispatching"
        /**
         * Setter for property Dispatching.<p>
        * Set to true while the EventDispatcher daemon thread is in the process
        * of dispatching events.
        * 
        * @volatile
         */
        protected void setDispatching(boolean fDispatching)
            {
            __m_Dispatching = fDispatching;
            }
        
        // Accessor for the property "EventCount"
        /**
         * Setter for property EventCount.<p>
        * The number of events processed by this EventDispatcher since the last
        * call to resetStats().
        * 
        * @volatile
         */
        protected void setEventCount(long cMillis)
            {
            __m_EventCount = cMillis;
            }
        
        // Accessor for the property "LastLogTime"
        /**
         * Setter for property LastLogTime.<p>
        * The timestamp at which the last queue stuck warning was logged.
         */
        protected void setLastLogTime(long ldtLog)
            {
            __m_LastLogTime = ldtLog;
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$EventDispatcher$Guard
        
        /**
         * Guard provides the Guardable interface implementation for the Daemon.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Guard
                extends    com.tangosol.coherence.component.util.Daemon.Guard
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Guard()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Guard(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher.Guard();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$EventDispatcher$Guard".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            public void terminate()
                {
                Service service = (Service) get_Module();
                
                // EventDispatcher termination should cause service termination
                service.getGuardable().terminate();
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$EventDispatcher$Queue
        
        /**
         * This is the Queue to which items that need to be processed are
         * added, and from which the daemon pulls items to process.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Queue
                extends    com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue
            {
            // ---- Fields declarations ----
            private static com.tangosol.util.ListMap __mapChildren;
            
            // Static initializer
            static
                {
                __initStatic();
                }
            
            // Default static initializer
            private static void __initStatic()
                {
                // register child classes
                __mapChildren = new com.tangosol.util.ListMap();
                __mapChildren.put("Iterator", Service.EventDispatcher.Queue.Iterator.get_CLASS());
                }
            
            // Default constructor
            public Queue()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Queue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                
                // state initialization: public and protected properties
                try
                    {
                    setElementList(new com.tangosol.util.RecyclingLinkedList());
                    }
                catch (java.lang.Exception e)
                    {
                    // re-throw as a runtime exception
                    throw new com.tangosol.util.WrapperException(e);
                    }
                
                // containment initialization: children
                
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher.Queue();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$EventDispatcher$Queue".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Declared at the super level
            /**
             * Appends the specified element to the end of this queue.
            * 
            * Queues may place limitations on what elements may be added to
            * this Queue.  In particular, some Queues will impose restrictions
            * on the type of elements that may be added. Queue implementations
            * should clearly specify in their documentation any restrictions on
            * what elements may be added.
            * 
            * @param oElement element to be appended to this Queue
            * 
            * @return true (as per the general contract of the Collection.add
            * method)
            * 
            * @throws ClassCastException if the class of the specified element
            * prevents it from being added to this Queue
             */
            public boolean add(Object oElement)
                {
                Service service = (Service) get_Module();
                int     nState  = service.getServiceState();
                switch (nState)
                    {
                    case Service.SERVICE_STARTING:
                    case Service.SERVICE_STARTED:
                    case Service.SERVICE_STOPPING:
                    case Service.SERVICE_STOPPED:
                        return super.add((Runnable) oElement);
                
                    default:
                        throw new IllegalStateException(
                            "Illegal service state " + nState + " for service " + service);
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.Service$EventDispatcher$Queue$Iterator
            
            /**
             * Iterator of a snapshot of the List object that backs the Queue.
             * Supports remove(). Uses the Queue as the monitor if any
             * synchronization is required.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.Iterator
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Iterator()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher.Queue.Iterator();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/Service$EventDispatcher$Queue$Iterator".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }
        }
    }
