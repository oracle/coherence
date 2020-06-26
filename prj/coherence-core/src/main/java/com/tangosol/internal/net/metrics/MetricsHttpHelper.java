/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.metrics;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.service.grid.DefaultProxyServiceDependencies;
import com.tangosol.internal.net.service.grid.LegacyXmlProxyServiceHelper;
import com.tangosol.internal.net.service.grid.ProxyServiceDependencies;

import com.tangosol.internal.net.service.peer.acceptor.HttpAcceptorDependencies;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.OperationalContext;

import com.tangosol.net.ProxyService;
import com.tangosol.net.Service;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Helper class to ensure a Metrics over HTTP service.
 *
 * @author jf  2018.09.10
 * @since 12.2.1.4.0
 */
public abstract class MetricsHttpHelper
    {
    /**
     * Helper method to build a {@link URL} to the Metrics over HTTP service given
     * a host and port.
     *
     * @param sHost  the Management over HTTP listening address
     * @param nPort  the Management over HTTP listening port
     *
     * @return the Metrics over HTTP URL
     *
     * @throws MalformedURLException
     */
    public static URL composeURL(String sHost, int nPort)
            throws MalformedURLException
        {
        return composeURL(sHost, nPort, "http");
        }

    /**
     * Helper method to build a {@link URL} to the Metrics over HTTP(S) service given
     * a host, port, and protocol.
     *
     * @param sHost      the Management over HTTP listening address
     * @param nPort      the Management over HTTP listening port
     * @param sProtocol  "http" or "https"
     *
     * @return the Metrics over HTTP/HTTPS URL
     *
     * @throws MalformedURLException
     */
    public static URL composeURL(String sHost, int nPort, String sProtocol)
            throws MalformedURLException
        {
        return new URL(sProtocol, sHost, nPort, "/metrics");
        }

    /**
     * Get the Management over HTTP service dependencies.
     *
     * @param ctx  the {@link OperationalContext}
     *
     * @return a {@link ProxyServiceDependencies}
     */
    public static ProxyServiceDependencies getDependencies(OperationalContext ctx)
        {
        URL urlConfig = Base.getContextClassLoader().getResource("metrics-http-config.xml");
        if (urlConfig == null)
            {
            throw new IllegalStateException("Unable to locate metrics-http-config.xml that should be resolvable from the coherence-metrics module on the class path.");
            }
        XmlElement xml = XmlHelper.loadXml(urlConfig);
        XmlHelper.replaceSystemProperties(xml, "system-property");
        return LegacyXmlProxyServiceHelper.fromXml(xml, new DefaultProxyServiceDependencies(), ctx, Base.getContextClassLoader());
        }

    /**
     * Get the Metrics over HTTP service name.
     *
     * @return the Metrics over HTTP service name
     */
    public static String getServiceName()
        {
        return "MetricsHttpProxy";
        }

    /**
     * Whether this Coherence node is capable of running the Metrics over HTTP service.
     *
     * @return whether this Coherence node is capable of running the Metrics over HTTP service
     */
    public static boolean isHttpCapable()
        {
        try
            {
            // coherence-rest
            Base.getContextClassLoader().loadClass("com.tangosol.coherence.metrics.internal.MetricsResourceConfig");
            }
        catch (Throwable t)
            {
            // don't bother logging the stack trace as having a stack trace in the logs can be alarming to administrators
            Logger.finest("One or more libraries are missing for Metrics over HTTP: " + t);

            return false;
            }

        return true;
        }

    /**
     * When pre-conditions are met, ensure start {@link MetricsHttpHelper#getServiceName() MetricsHttpService}.
     * <p>
     * Pre-conditions for starting service include {@link #PROP_METRICS_ENABLED} set to "true", management being enabled for this member, not "none", and
     * implementation of MetricsHttpService in classpath.
     *
     * @param mapServices  add started MetricsHttpService to this map if it is started.
     */
    public static void ensureMetricsService(Map<Service, String> mapServices)
        {
        boolean fEnabled = Config.getBoolean(PROP_METRICS_ENABLED, false);

        if (fEnabled)
            {
            Cluster cluster = CacheFactory.ensureCluster();
            if (cluster.getManagement() == null)
                {
                Logger.err("Metrics over HTTP is not available due to management not being enabled");
                }
            else
                {
                // start metrics service
                if (MetricsHttpHelper.isHttpCapable())
                    {
                    ProxyServiceDependencies deps = MetricsHttpHelper.getDependencies((OperationalContext) cluster);

                    // start the Metrics HTTP acceptor
                    try
                        {
                        ProxyService service = (ProxyService) cluster.ensureService(MetricsHttpHelper.getServiceName(), ProxyService.TYPE_DEFAULT);
                        service.setDependencies(deps);
                        service.start();
                        mapServices.put(service, MetricsHttpHelper.getServiceName());

                        return;
                        }
                    catch (Throwable t)
                        {
                        Throwable tOriginal = t instanceof RuntimeException ? Base.getOriginalException((RuntimeException) t) : t;
                        Throwable tCause    = tOriginal.getCause() == null ? tOriginal : tOriginal.getCause();

                        if (tCause instanceof ClassNotFoundException)
                            {
                            Logger.err("Metrics over HTTP is not available most likely due to this member missing "
                                + "the necessary libraries to run the service. Handled exception: " + tCause.getClass().getSimpleName() + ": " + tCause.getLocalizedMessage());
                            return;
                            }
                        else
                            {
                            // could be IOException for address in use or SecurityException or IllegalArgumentException for Configuration Errors
                            HttpAcceptorDependencies depsHttpAcceptor = (HttpAcceptorDependencies) deps.getAcceptorDependencies();

                            Logger.err("failed to start service " + MetricsHttpHelper.getServiceName() + " at address " + depsHttpAcceptor.getLocalAddress() + ":" +
                                depsHttpAcceptor.getLocalPort() + " due to " + tCause.getClass().getSimpleName() + " : " + Base.getDeepMessage(tCause, ":"));
                            return;
                            }
                        }
                    }
                else
                    {
                    Logger.err("Metrics over HTTP is not available most likely due to missing the necessary libraries to run the service ");
                    }
                }
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The System property used to enabled or disable running Coherence metrics.
     * <p>
     * If this property is not set the default value used by the {@link #ensureMetricsService(Map)} method
     * is {@code false}.
     */
    public static final String PROP_METRICS_ENABLED = "coherence.metrics.http.enabled";

    /**
     * Default Prometheus Metrics HTTP port.
     *
     * Registered at https://github.com/prometheus/prometheus/wiki/Default-port-allocations.
     */
    public static final int DEFAULT_PROMETHEUS_METRICS_PORT = 9612;
    }
