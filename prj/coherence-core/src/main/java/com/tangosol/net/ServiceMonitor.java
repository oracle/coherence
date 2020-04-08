/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import java.util.Map;
import java.util.Set;

import com.oracle.coherence.common.base.Disposable;

/**
 * The {@link ServiceMonitor} is a facility that keeps registered services
 * alive. It is bound to a {@link ConfigurableCacheFactory} whilst alive
 * and should allow for {@link ConfigurableCacheFactory}s to be changed and
 * reflected upon restart of the monitor.
 *
 * @author cf 2011.05.24
 * @author hr 2012.06.28
 *
 * @since Coherence 12.1.2
 */
public interface ServiceMonitor
        extends Disposable
    {
    /**
     * Adds a set of {@link Service}s to be monitored providing life support.
     * If any of the services are not running they are started either via the
     * configured {@link ConfigurableCacheFactory} iff provided or the
     * provided {@link Service}.
     *
     * @param mapServices a map of {@link Service}s where values are non-scoped
     *                    service names
     */
    public void registerServices(Map<Service, String> mapServices);

    /**
     * Removes the supplied set of {@link Service}s from monitoring.
     * Note that the services are not being stopped; they are just removed
     * from the list of {@link Service}s that are being monitored.
     *
     * @param setServices the set of {@link Service}s to be removed from monitoring
     */
    public void unregisterServices(Set<Service> setServices);

    /**
     * Stop monitoring all registered services.
     * Note that the services are not being stopped; they are just removed
     * from the list of {@link Service}s that are being monitored.
     */
    public void stopMonitoring();

    /**
     * Returns true iff the ServiceMonitor is monitoring a number of
     * services.
     *
     * @return  returns true iff the ServiceMonitor is monitoring a number of
     *          services
     */
    public boolean isMonitoring();

    /**
     * Set the {@link ConfigurableCacheFactory} a {@link ServiceMonitor} can
     * operate with. Setting the {@link ConfigurableCacheFactory} should
     * be performed prior to registering services or requires a
     * {@link #stopMonitoring()} and {@link #registerServices(Map)} call.
     *
     * @param ccf  the {@link ConfigurableCacheFactory} used to start the
     *             service
     */
    public void setConfigurableCacheFactory(ConfigurableCacheFactory ccf);

    /**
     * Return the thread used by this ServiceMonitor.
     */
    public Thread getThread();
    }
