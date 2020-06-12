/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Blocking;

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

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
                CacheFactory.log("Failed to start services: " + e, CacheFactory.LOG_ERR);
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
    * Shutdown the DefaultCacheServer.
    */
    public void shutdownServer()
        {
        stopMonitoring();
        CacheFactory.shutdown();
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
     * {@link ConfigurableCacheFactory} and register specified event interceptors.
     * This method is intended to be used within managed containers.
     *
     * @param ccf  the {@link ConfigurableCacheFactory} to use
     *
     * @return the instance of the {@link DefaultCacheServer} started
     *
     * @since Coherence 14.1.2
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

        DefaultCacheServer dcs;

        dcs = ensureInstance(getConfigurableCacheFactory());
        if (asTenant != null)
            {
            CacheFactory.log("Multi-tenancy is only supported for a GAR deployment",
                CacheFactory.LOG_WARN);
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
        ServiceMonitor monitor = m_serviceMon;
        if (monitor != null)
            {
            monitor.stopMonitoring();
            }

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

        Map<Service, String> mapServices = startServicesInternal();

        m_serviceMon.setConfigurableCacheFactory(m_factory);
        m_serviceMon.registerServices(mapServices);

        reportStarted();
        }

    /**
     * Log the start message.
     */
    protected void reportStarted()
        {
        CacheFactory.log(getServiceBanner(CacheFactory.getCluster()) +
                '\n' + "Started " + getClass().getSimpleName() + "...\n",
            CacheFactory.LOG_INFO);
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
            if (MetricsHttpHelper.isHttpCapable())
                {
                MetricsHttpHelper.ensureMetricsService(mapServices);
                }
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
    * Create a list of running services in the given Cluster.
    *
    * @param cluster Cluster for which to create a list of running services
    *
    * @return string containing listing of running services
    */
    protected String getServiceBanner(Cluster cluster)
        {
        // Extract the Cluster object out of the Safe layer to
        // de-clutter the logging
        try
            {
            cluster = (Cluster) ClassHelper.
                    invoke(cluster, "getCluster", null);
            }
        catch (Exception e)
            {
            // If this fails for some reason, revert to SafeCluster
            }

        StringBuilder sb = new StringBuilder();

        sb.append("\nServices\n  (\n  ");

        if (cluster != null)
            {
            for (Enumeration e = cluster.getServiceNames(); e.hasMoreElements();)
                {
                Service service = cluster.getService((String) e.nextElement());
                if (service != null)
                    {
                    sb.append(service)
                            .append("\n  ");
                    }
                }
            }
        sb.append(")\n");

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
    }
