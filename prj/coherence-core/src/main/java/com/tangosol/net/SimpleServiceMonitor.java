/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Blocking;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.application.ContainerHelper;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import com.tangosol.net.management.Registry;

import com.tangosol.util.Base;
import com.tangosol.util.CopyOnWriteMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * The {@link SimpleServiceMonitor} starts and monitors services that are
 * registered for monitoring.
 *
 * @author cf 2011.05.24
 * @author hr 2012.06.28
 * @author gh 2022.11.22
 * @since Coherence 12.1.2
 */
public class SimpleServiceMonitor
        implements ServiceMonitor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a SimpleServiceMonitor with the default interval to check
     * that the registered services are running ({@link #DEFAULT_WAIT_MILLIS}).
     */
    public SimpleServiceMonitor()
        {
        this(DEFAULT_WAIT_MILLIS);
        }

    /**
     * Construct a SimpleServiceMonitor with the provided interval to check
     * that the registered services are running.
     *
     * @param cWaitMillis  the number of milliseconds in between checking
     *                     that the registered services are running
     */
    public SimpleServiceMonitor(long cWaitMillis)
        {
        m_cWaitMillis = cWaitMillis;
        }

    // ----- ServiceMonitor interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerServices(Map<Service, String> mapServices)
        {
        Map<Service, String> mapServicesByName = m_mapServices;

        mapServicesByName.putAll(mapServices);

        if (!mapServicesByName.isEmpty())
            {
            start();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterServices(Set<Service> setServices)
        {
        Map<Service, String> mapServices = m_mapServices;

        mapServices.keySet().removeAll(setServices);

        if (mapServices.isEmpty())
            {
            stopMonitoring();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stopMonitoring()
        {
        Thread thread = m_thread;

        if (thread != null)
            {
            thread.interrupt();
            try
                {
                thread.join(m_cWaitMillis);
                }
            catch (InterruptedException e)
                {
                // ignore since we are stopping
                }
            }

        m_fStarted = false;
        m_thread  = null;
        m_ccf     = null;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMonitoring()
        {
        return m_fStarted;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfigurableCacheFactory(ConfigurableCacheFactory ccf)
        {
        m_ccf = ccf;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Thread getThread()
        {
        return m_thread;
        }


    // ----- Disposable interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose()
        {
        m_fDisposed = true;
        m_ccf       = null;

        stopMonitoring();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Start monitoring the services.
     */
    protected synchronized void start()
        {
        if (!m_fStarted)
            {
            assertNotDisposed();

            Thread thread = m_thread = Base.makeThread(THREAD_GROUP, new Runnable()
                {
                public void run()
                    {
                    monitorServices(m_cWaitMillis);
                    }
                }, "ServiceMonitor");
            thread.setDaemon(true);
            thread.start();

            m_fStarted = true;
            }
        }

    /**
     * Check the service status periodically (keep alive interval), restarting
     * any stopped services.
     *
     * @param cWaitMillis  the number of milliseconds between checks
     */
    protected void monitorServices(long cWaitMillis)
        {
        Thread   thread    = m_thread;
        Cluster  cluster   = CacheFactory.getCluster();
        Registry registry  = cluster == null || !cluster.isRunning() ? null : cluster.getManagement();
        String   sNodeName = registry == null
                ? null : registry.ensureGlobalName(Registry.NODE_TYPE);

        Base.azzert(thread == Thread.currentThread(),
                "monitorServices should only be called on the ServiceMonitor thread");
        while (!thread.isInterrupted())
            {
            try
                {
                Blocking.sleep(cWaitMillis);

                ConfigurableCacheFactory ccf = m_ccf;
                for (Map.Entry<Service, String> entry : m_mapServices.entrySet())
                    {
                    Service service = entry.getKey();
                    if (!service.isRunning())
                        {
                        ContainerHelper.initializeThreadContext(service);

                        if (ccf == null)
                            {
                            service.start();
                            }
                        else
                            {
                            // the reference to the safe service continues to
                            // exist and be valid

                            String sServiceName = entry.getValue();
                            if (MetricsHttpHelper.getServiceName().equals(sServiceName))
                                {
                                MetricsHttpHelper.ensureMetricsService(m_mapServices);
                                }
                            else
                                {
                                // Ensure the Scheme-based Service
                                ccf.ensureService(sServiceName);
                                }
                            }
                        }
                    }

                // Note: this Registry.isRegistered call is a 'heartbeat' against
                //       the management service to ensure it is running
                if (registry != null && !registry.isRegistered(sNodeName))
                    {
                    // the node name may change due to cluster restart
                    sNodeName = registry.ensureGlobalName(Registry.NODE_TYPE);
                    }
                }
            catch (RuntimeException e)
                {
                Logger.err("Failed to restart services: ", e);
                }
            catch (InterruptedException e)
                {
                break;
                }
            }
        }

    /**
     * Return true if this {@link ServiceMonitor} has been disposed via
     * invocation of {@link #dispose()}.
     *
     * @return true if this monitor has been disposed
     */
    protected boolean isDisposed()
        {
        return m_fDisposed;
        }

    /**
     * Throws {@link IllegalStateException} if this monitor has been disposed
     * via invocation of {@link #dispose()}.
     *
     * @throws IllegalStateException if this monitor has been disposed
     */
    protected void assertNotDisposed()
        {
        if (isDisposed())
            {
            throw new IllegalStateException("This ServiceMonitor has been disposed and cannot be reused");
            }
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The default time interval by which we check the alive status.
     */
    protected static final Integer DEFAULT_WAIT_MILLIS = 5000;

    /**
     * The {@link ThreadGroup} used to group service monitor threads.
     */
    private   static final ThreadGroup THREAD_GROUP = new ThreadGroup("ServiceMonitors");

    /**
     * Indicates that the {@link ServiceMonitor} has started.
     */
    protected volatile boolean m_fStarted;

    /**
     * The {@link Thread} we are using for monitoring the services periodically.
     */
    protected Thread m_thread;

    /**
     * Indicates that this {@link ServiceMonitor} has been disposed.
     */
    protected boolean m_fDisposed;

    /**
     * A {@link ConfigurableCacheFactory} instance used to start the service.
     */
    // Note: m_ccf is not volatile as by the contract a stop and start of
    //       the monitor is required for CCF changes to be reflected.
    protected ConfigurableCacheFactory m_ccf;

    /**
     * A map of {@link Service}s to be monitored with values being non-scoped service names.
     */
    protected Map<Service, String> m_mapServices = new CopyOnWriteMap(HashMap.class);

    /**
     * The number of milliseconds between checking the status of the services.
     */
    protected final long m_cWaitMillis;
    }
