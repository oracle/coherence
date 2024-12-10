/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.application.ContainerContext;
import com.tangosol.application.Context;
import com.tangosol.application.LifecycleListener;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import com.tangosol.internal.net.service.LegacyXmlServiceHelper;

import com.tangosol.net.security.DoAsAction;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

import java.io.File;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.regex.Pattern;

/**
* DefaultCacheServer is a simple command line facility and convenience API
* that starts all services that are declared as requiring an "autostart" in
* the configurable factory XML descriptor.
* <p>
* DefaultCacheServer can also monitor services it started to ensure they
* exist. Monitoring services is enabled by default.
*
* @author gg/yc/hr
* @since Coherence 2.2
*/
public class DefaultCacheServer
        extends Base
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a DefaultCacheServer using the provided factory.
    *
    * @param factory  the ConfigurableCacheFactory
    */
    public DefaultCacheServer(ConfigurableCacheFactory factory)
        {
        m_factory = factory;
        }

    // ----- public API -----------------------------------------------------

    /**
     * Add a {@link LifecycleListener} that will be notified of lifecycle
     * events for this {@link DefaultCacheServer}.
     *
     * @param listener  the listener to add
     */
    public void addLifecycleListener(LifecycleListener listener)
        {
        ensureLifecycleListeners().add(listener);
        }

    /**
     * Remove a {@link LifecycleListener} so that it will no longer be notified
     * of lifecycle events for this {@link DefaultCacheServer}.
     * <p>
     * Listeners are stored in a {@link List} and will be removed based on a
     * simple object equality check.
     *
     * @param listener  the listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener)
        {
        ensureLifecycleListeners().remove(listener);
        }

    /**
    * Start the cache server and check the service status periodically,
    * restarting any stopped services.
    * <p>
    * This method will block the calling thread.
    *
    * @param cWaitMillis  the number of milliseconds between checks
    */
    public void startAndMonitor(final long cWaitMillis)
        {
        AccessController.doPrivileged(
            (PrivilegedAction<Object>) () ->
                {
                initialStartServices(cWaitMillis);

                markServicesStarted();

                monitorServices(cWaitMillis);

                return null;
                });
        }

    /**
    * Start the cache server and asynchronously check the service status
    * periodically, restarting any services that have stopped.
    *
    * @param cWaitMillis  the number of milliseconds between checks
    *
    * @since Coherence 12.1.2
    */
    public void startDaemon(final long cWaitMillis)
        {
        while (true)
            {
            try
                {
                AccessController.doPrivileged(new DoAsAction(
                    () -> {
                        initialStartServices(cWaitMillis);
                        return null;
                        }));

                markServicesStarted();
                break;
                }
            catch (Exception e)
                {
                // COH-2775: This can happen if some of the services
                // failed to start before main entered into its health
                // check loop. Since the startDaemon() is often called
                // during an application initialization logic when
                // running within a container, we should try to wait
                // a bit and do another attempt. In the worst case
                // they will see tons of restart attempts in the log.
                Logger.err("Failed to start services: " + e);
                Runtime.getRuntime().removeShutdownHook(m_threadShutdown);
                try
                    {
                    Blocking.sleep(DEFAULT_WAIT_MILLIS);
                    }
                catch (InterruptedException e2)
                    {
                    // this is a daemon thread; we can ignore interrupts
                    // why should we ignore an explicit interrupt even if
                    // this is a daemon
                    break;
                    }
                }
            }
        }

    /**
    * Start all services that are declared as requiring an "autostart" in
    * the configurable factory XML descriptor.
    *
    * @return list of services that have been successfully started
    */
    public List<Service> startServices()
        {
        return new LinkedList(startServicesInternal().keySet());
        }

    /**
    * Shutdown the DefaultCacheServer and Coherence cluster.
    */
    public void shutdownServer()
        {
        stop();
        CacheFactory.shutdown();
        }

    /**
    * Stop this DefaultCacheServer and dispose the {@link ConfigurableCacheFactory}
    * that this server wraps.
    * 
    * @see ConfigurableCacheFactory#dispose()
    */
    public void stop()
        {
        List<LifecycleListener> listListener = ensureLifecycleListeners();
        Context                 ctx          = new LifecycleContext();

        for (LifecycleListener listener : listListener)
            {
            try
                {
                listener.preStop(ctx);
                }
            catch (Throwable e)
                {
                Logger.err(e);
                }
            }

        stopServiceMonitor();

        if (m_factory != null)
            {
            m_factory.dispose();
            CacheFactory.getCacheFactoryBuilder().release(m_factory);
            }

        for (LifecycleListener listener : listListener)
            {
            try
                {
                listener.postStop(ctx);
                }
            catch (Throwable e)
                {
                Logger.err(e);
                }
            }

        notifyShutdown();
        }

    /**
    * Block the calling thread until DefaultCacheServer has called start on all
    * associated services. Upon control being returned services may not be running
    * due to possible error on start or concurrent shutdown.
    *
    * @throws IllegalStateException iff the services were not attempted to be
    *         started, which could be due to concurrent shutdown
    */
    public void waitForServiceStart()
        {
        if (!m_fServicesStarted)
            {
            synchronized (m_lock)
                {
                while (!(m_fServicesStarted || m_fShutdown))
                    {
                    try
                        {
                        Blocking.wait(m_lock);
                        }
                    catch (InterruptedException e) {}
                    }

                if (!m_fServicesStarted)
                    {
                    throw new IllegalStateException("Services were not started");
                    }
                }
            }
        }

    /**
     * Set the "services started" flag.
     */
    protected void markServicesStarted()
        {
        synchronized (m_lock)
            {
            m_fServicesStarted = true;
            m_lock.notifyAll();
            }
        }

    /**
    * Shutdown the cache server.
    *
    * @since Coherence 3.2
    */
    public static void shutdown()
        {
        DefaultCacheServer dcs = getInstance();
        if (dcs != null)
            {
            s_instance = null;
            dcs.shutdownServer();
            }
        }

    /**
    * Start the cache server on a dedicated daemon thread. This method is
    * intended to be used within managed containers.
    *
    * @since Coherence 3.2
    *
    * @deprecated use {@link #startServerDaemon()} instead
    */
    public static void startDaemon()
        {
        startServerDaemon();
        }

    /**
     * Start the cache server on a dedicated daemon thread, using default
     * {@link ConfigurableCacheFactory}.
     * This method is intended to be used within managed containers.
     *
     * @return the instance of the {@link DefaultCacheServer} started
     *
     * @since Coherence 12.1.2
     */
    public static DefaultCacheServer startServerDaemon()
        {
        return startServerDaemon(getConfigurableCacheFactory());
        }

    /**
     * Start the cache server on a dedicated daemon thread, using specified
     * {@link ConfigurableCacheFactory}.
     * This method is intended to be used within managed containers.
     *
     * @param ccf  the {@link ConfigurableCacheFactory} to use
     *
     * @return the instance of the {@link DefaultCacheServer} started
     *
     * @since 20.06
     */
    public static DefaultCacheServer startServerDaemon(ConfigurableCacheFactory ccf)
        {
        DefaultCacheServer server = ensureInstance(ccf);
        startDaemon(server);

        return server;
        }

    /**
    * Entry point: start the cache server under two possible contexts:
    * <ol>
    *   <li>With a "coherence-cache-config.xsd" compliant configuration file.</li>
    *   <li>With a GAR file or a directory containing the contents of a GAR
    *       file and optionally an application name and comma-delimited list of
     *      tenant names.</li>
    * </ol>
    * If both are provided the latter takes precedence. Additionally
    * DefaultCacheServer accepts a numeric value to monitor the service
    * status (keep alive interval).
    * <p>
    * Default configuration file is "coherence-cache-config.xml"; default
    * keep alive interval is 5 sec.
    * <pre>
    * Example:
    *   java -server -Xms512m -Xmx512m com.tangosol.net.DefaultCacheServer cache-config.xml 5
    * GAR Example:
    *   java -server -Xms512m -Xmx512m com.tangosol.net.DefaultCacheServer my-app.gar MyApp 5
    * </pre>
    *
    * @param asArg the command line arguments
    */
    public static void main(String[] asArg)
        {
        long     cWaitMillis = DEFAULT_WAIT_MILLIS;
        File     fileGar     = null;
        String   sAppName    = null;
        String[] asTenant    = null;

        for (int i = 0; i < asArg.length; ++i)
            {
            String sArg = asArg[i];
            if (sArg.endsWith(".xml"))
                {
                CacheFactory.getCacheFactoryBuilder().setCacheConfiguration(null,
                    XmlHelper.loadFileOrResource(sArg, "cache configuration", null));
                }
            else if (sArg.equals("--version"))
                {
                System.out.println(CacheFactory.VERSION);
                if (asArg.length == 1)
                    {
                    System.exit(0);
                    }
                }
            else if (Pattern.matches("[0-9]*", sArg))
                {
                try
                    {
                    cWaitMillis = 1000L * Math.max(0, Integer.parseInt(sArg));
                    }
                catch (Exception e) {}
                }
            else if (sArg.endsWith(".gar") || sArg.contains(File.separator) || ".".equals(sArg))
                {
                fileGar = new File(sArg);
                }
            else if (sArg.contains(","))
                {
                asTenant = Base.parseDelimitedString(sArg, ',');
                }
            else
                {
                sAppName = sArg;
                }
            }

        DefaultCacheServer dcs = ensureInstance(getConfigurableCacheFactory());

        if (asTenant != null)
            {
            Logger.warn("Multi-tenancy is only supported for a GAR deployment");
            }

        dcs.startAndMonitor(cWaitMillis);
        }

    // ----- backward compatibility methods ---------------------------------

    /**
    * Start all services that are declared as requiring an "autostart" in
    * the default configurable factory XML descriptor.
    * <p>
    * This method will not create a ServiceMonitor for started services.
    *
    * @return list of services that have been successfully started
    */
    public static List start()
        {
        return start(getConfigurableCacheFactory());
        }

    /**
    * Start all services that are declared as requiring an "autostart" in
    * the configurable factory XML descriptor.
    * <p>
    * This method will not create a ServiceMonitor for started services.
    *
    * @param factory  ConfigurableCacheFactory to use
    *
    * @return list of services that have been successfully started
    */
    public static List start(ConfigurableCacheFactory factory)
        {
        return ensureInstance(factory).startServices();
        }

    // ----- subclassing support methods ------------------------------------

    /**
    * Ensures the DCS instance has shutdown and the associated
    * ServiceMonitor is no longer monitoring services.
    *
    * return true if the monitoring was active and has been stopped as a result
    *        of this call; false if the monitoring was not active
    */
    protected boolean stopMonitoring()
        {
        stopServiceMonitor();

        return notifyShutdown();
        }

    /**
     * Stop the ServiceMonitor.
     */
    protected void stopServiceMonitor()
        {
        ServiceMonitor monitor = m_serviceMon;
        if (monitor != null)
            {
            monitor.stopMonitoring();
            }
        }

    /**
     * Ensure the DCS instance has shutdown.
     *
     * return true if DCS instance was called to start on the associated services
     */
    protected boolean notifyShutdown()
        {
        synchronized (m_lock)
            {
            boolean fWasStarted = m_fServicesStarted;

            m_fServicesStarted = false;
            m_fShutdown        = true;
            m_lock.notifyAll();

            return fWasStarted;
            }
        }

    /**
    * Setup any necessary resources and start all services.
    *
    * @param cWaitMillis  the interval, in milliseconds, to monitor services
    */
    protected void initialStartServices(long cWaitMillis)
        {
        Runtime.getRuntime().addShutdownHook(m_threadShutdown);

        m_serviceMon = new SimpleServiceMonitor(cWaitMillis);

        List<LifecycleListener> listListener = ensureLifecycleListeners();
        Context                 ctx          = new LifecycleContext();

        for (LifecycleListener listener : listListener)
            {
            try
                {
                listener.preStart(ctx);
                }
            catch (Throwable e)
                {
                Logger.err(e);
                }
            }

        Cluster cluster     = CacheFactory.getCluster();
        boolean fWasRunning = cluster.isRunning();

        Map<Service, String> mapServices = startServicesInternal();

        m_serviceMon.setConfigurableCacheFactory(m_factory);
        m_serviceMon.registerServices(mapServices);

        // if this DCS instance resulted in the Cluster starting make sure
        // system services, in addition to autostart services, are reported
        if (!fWasRunning && cluster.isRunning())
            {
            reportStarted();
            }
        else
            {
            reportStarted(mapServices.keySet());
            }

        for (LifecycleListener listener : listListener)
            {
            try
                {
                listener.postStart(ctx);
                }
            catch (Throwable e)
                {
                Logger.err(e);
                }
            }
        }

    /**
     * Returns the list of {@link LifecycleListener}s registered for this {@link DefaultCacheServer}.
     * <p>
     * If the list of listeners does not yet exist it will be created and initially populated
     * using the {@link ServiceLoader} to discover and load listeners.
     *
     * @return the list of {@link LifecycleListener}s registered for this {@link DefaultCacheServer}
     */
    protected List<LifecycleListener> ensureLifecycleListeners()
        {
        if (m_listLifecycleListener == null)
            {
            synchronized (this)
                {
                List<LifecycleListener>          list   = new CopyOnWriteArrayList<>();
                ServiceLoader<LifecycleListener> loader = ServiceLoader.load(LifecycleListener.class);
                for (LifecycleListener listener : loader)
                    {
                    list.add(listener);
                    }
                m_listLifecycleListener = list;
                }
            }
        return m_listLifecycleListener;
        }

    /**
     * Log the start message.
     */
    protected void reportStarted()
        {
        Logger.info("Started " + getClass().getSimpleName() +
                CacheFactory.getCluster().getServiceBanner());
        }

    /**
     * Log the start message.
     *
     * @param colServices  the collection of started services
     */
    protected void reportStarted(Collection<Service> colServices)
        {
        Logger.info("Started " + getClass().getSimpleName() +
                getServiceBanner(colServices));
        }

    /**
    * Starts the services marked with autostart returning a map of a service
    * name to the started service.
    *
    * @return a map holding all started services keyed by names
    */
    protected Map<Service, String> startServicesInternal()
        {
        ConfigurableCacheFactory factory     = m_factory;
        Map<Service, String>     mapServices = null;

        if (factory instanceof DefaultConfigurableCacheFactory)
            {
            DefaultConfigurableCacheFactory dccf = (DefaultConfigurableCacheFactory) factory;

            mapServices = new LinkedHashMap();
            for (Object o : dccf.getConfig().getSafeElement("caching-schemes")
                    .getElementList())
                {
                XmlElement xmlScheme = (XmlElement) o;

                if (xmlScheme.getSafeElement("autostart").getBoolean())
                    {
                    try
                        {
                        Service service = dccf.ensureService(xmlScheme);
                        if (!mapServices.containsKey(service))
                            {
                            mapServices.put(service,
                                LegacyXmlServiceHelper.getServiceName(xmlScheme));
                            }
                        }
                    catch (RuntimeException e)
                        {
                        handleEnsureServiceException(e);
                        }
                    }
                }
            }
        else if (factory instanceof ExtensibleConfigurableCacheFactory)
            {
            ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) factory;
            try
                {
                eccf.activate();

                mapServices = eccf.getServiceMap();
                }
            catch (RuntimeException e)
                {
                handleEnsureServiceException(e);
                }
            }
        else
            {
            throw new IllegalStateException("StartServices not supported for factory " + factory);
            }

        try
            {
            MetricsHttpHelper.ensureMetricsService(mapServices);
            }
        catch (Exception e)
            {
            err(e);
            }

        return mapServices;
        }

    /**
    * Check the ServiceMonitor status ensuring it is monitoring services. This
    * method blocks the caller's thread until the DefaultCacheServer is
    * shutdown and monitoring stops.
    *
    * @param cWaitMillis  the number of milliseconds between checks
    */
    protected void monitorServices(long cWaitMillis)
        {
        while (true)
            {
            synchronized (m_lock)
                {
                try
                    {
                    Blocking.wait(m_lock, cWaitMillis);
                    }
                catch (InterruptedException e)
                    {
                    // we don't care because we are probably shutting down
                    }
                }

            if (isMonitorStopped())
                {
                stopMonitoring();
                break;
                }
            }
        }

    /**
    * Invoked if starting a service raises an exception. Re-throws the
    * exception. Override, for example, to log the exception and continue
    * starting services.
    *
    * @param e  the RuntimeException
    *
    * @see #startServices()
    */
    protected void handleEnsureServiceException(RuntimeException e)
        {
        throw e;
        }

    /**
    * Return a service banner for a collection of services.
    *
    * @param colService  the collection of services
    *
    * @return a service banner for a collection of services
    */
    protected String getServiceBanner(Collection<Service> colService)
        {
        StringBuilder sb = new StringBuilder();

        sb.append("\nServices\n  (\n  ");

        for (Service service : colService)
            {
            try
                {
                service = (Service) ClassHelper.
                        invoke(service, "getService", null);
                }
            catch (Exception e)
                {
                // If this fails for some reason, revert to SafeService
                }

            sb.append(service).append("\n  ");
            }
        sb.append(")");

        return sb.toString();
        }

    /**
     * Return the {@link ServiceMonitor} used by the cache server.
     *
     * @return the ServiceMonitor
     */
    public boolean isMonitorStopped()
        {
        return !m_serviceMon.isMonitoring();
        }

    /**
     * Return {@code true} if this {@link DefaultCacheServer} is monitoring services.
     *
     * @return {@code true} if this {@link DefaultCacheServer} is monitoring services
     */
    public boolean isMonitoringServices()
        {
        if (m_serviceMon == null || !m_serviceMon.isMonitoring())
            {
            return false;
            }

        Thread thread = m_serviceMon.getThread();
        return thread != null && thread.isAlive();
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Start the provided cache server on a dedicated daemon thread. This
    * method is intended to be used within managed containers.
    *
    * @param dcs  the cache server to start
    */
    protected static void startDaemon(final DefaultCacheServer dcs)
        {
        Thread thread = makeThread(null,
            () -> dcs.startDaemon(DEFAULT_WAIT_MILLIS), "DefaultCacheServer");
        thread.setDaemon(true);
        thread.start();
        }

    /**
    * Gets a ConfigurableCacheFactory based on the default configuration.
    *
    * @return a ConfigurableCacheFactory
    */
    protected static ConfigurableCacheFactory getConfigurableCacheFactory()
        {
        // we use getCCF() here instead of getCFB().getCCF() to be more
        // tolerant to possible prior setCCF() calls
        return CacheFactory.getConfigurableCacheFactory();
        }

    /**
    * Returns the DefaultCacheServer singleton, creating it if necessary.
    *
    * @param factory  the CacheFactory to use
    *
    * @return the DefaultCacheServer singleton
    */
    protected static synchronized DefaultCacheServer ensureInstance(ConfigurableCacheFactory factory)
        {
        DefaultCacheServer instance = s_instance;
        if (instance == null)
            {
            s_instance = instance = new DefaultCacheServer(factory);
            }
        else if (factory != instance.m_factory)
            {
            throw new IllegalArgumentException(
                "The DefaultCacheServer has already been started with " +
                "a different factory object");
            }

        return instance;
        }

    /**
    * Returns the DefaultCacheServer created by a previous invocation of
    * {@link #ensureInstance(ConfigurableCacheFactory)}.
    * Will throw an IllegalStateException if there is no instance.
    *
    * @return the DefaultCacheServer
    */
    public static synchronized DefaultCacheServer getInstance()
        {
        DefaultCacheServer instance = s_instance;
        if (instance == null)
            {
            throw new IllegalStateException(
                "The DefaultCacheServer has not been started");
            }
        return instance;
        }

    // ----- constants and data fields --------------------------------------

    /**
    * The default number of milliseconds between checks for service restart.
    */
    public static final long DEFAULT_WAIT_MILLIS = 5000;

    /**
    * The singleton instance of this class.
    */
    private static DefaultCacheServer s_instance = null;

    /**
    * A lock used for synchronization of state on shutdown.
    */
    private final Object m_lock = new Object();

    /**
    * The ConfigurableCacheFactory used by this DefaultCacheServer.
    */
    private final ConfigurableCacheFactory m_factory;

    /**
    * The ServiceMonitor used to monitor services.
    */
    protected volatile ServiceMonitor m_serviceMon;

    /**
    * Flag that indicates whether of not the shutdown was initiated.
    */
    protected volatile boolean m_fShutdown;

    /**
    * Flag that indicates whether this instance of DefaultCacheServer has
    * called start on the associated services.
    *
    * @see #initialStartServices
    */
    protected boolean m_fServicesStarted;

    /**
    * Shutdown hook thread.
    */
    protected Thread m_threadShutdown = makeThread(null, this::stopMonitoring, null);

    /**
     * The {@link LifecycleListener}s to be notified of lifecycle events
     * for this {@link DefaultCacheServer}.
     * <p>
     * The {@link LifecycleListener}s will be discovered and loaded using
     * the {@link java.util.ServiceLoader}.
     */
    protected List<LifecycleListener> m_listLifecycleListener;

    // ----- inner class: LifecycleContext ----------------------------------

    /**
     * An implementation of {@link Context} that will be passed
     * to {@link DefaultCacheServer} {@link LifecycleListener}s.
     */
    protected class LifecycleContext
            implements Context
        {
        @Override
        public ConfigurableCacheFactory getConfigurableCacheFactory()
            {
            return m_factory;
            }

        @Override
        public CacheFactoryBuilder getCacheFactoryBuilder()
            {
            return CacheFactory.getCacheFactoryBuilder();
            }

        @Override
        public ClassLoader getClassLoader()
            {
            if (m_factory instanceof ExtensibleConfigurableCacheFactory)
                {
                ((ExtensibleConfigurableCacheFactory) m_factory).getConfigClassLoader();
                }
            return m_factory.getClass().getClassLoader();
            }

        @Override
        public String getApplicationName()
            {
            return Coherence.DEFAULT_NAME;
            }

        @Override
        public ServiceMonitor getServiceMonitor()
            {
            return m_serviceMon;
            }

        @Override
        public String getPofConfigURI()
            {
            return Config.getProperty("coherence.pof.config", "pof-config.xml");
            }

        @Override
        public String getCacheConfigURI()
            {
            return Config.getProperty("coherence.cacheconfig", "coherence-cache-config.xml");
            }

        @Override
        public ContainerContext getContainerContext()
            {
            return null;
            }

        @Override
        @Deprecated
        public ExtendedContext getExtendedContext()
            {
            return null;
            }
        }
    }
