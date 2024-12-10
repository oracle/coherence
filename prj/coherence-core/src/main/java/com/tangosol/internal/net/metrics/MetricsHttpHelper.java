/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.metrics;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.metrics.MetricsHttpHandler;

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
     * @throws MalformedURLException if the composed URL is invalid
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
     * @throws MalformedURLException if the composed URL is invalid
     */
    public static URL composeURL(String sHost, int nPort, String sProtocol)
            throws MalformedURLException
        {
        return new URL(sProtocol, sHost, nPort, s_metricsHandler == null ? "/metrics" : s_metricsHandler.getPath());
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
        ClassLoader loader    = Base.getContextClassLoader();
        URL         urlConfig = loader.getResource(METRICS_CONFIG);
        if (urlConfig == null)
            {
            throw new IllegalStateException("Unable to locate " + METRICS_CONFIG);
            }
        XmlElement  xml       = XmlHelper.loadXml(urlConfig);
        XmlHelper.replaceSystemProperties(xml, "system-property");
        return LegacyXmlProxyServiceHelper.fromXml(xml, new DefaultProxyServiceDependencies(), ctx, loader);
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
     * When pre-conditions are met, ensure start {@link MetricsHttpHelper#getServiceName() MetricsHttpService}.
     * <p>
     * Pre-conditions for starting service include {@link #PROP_METRICS_ENABLED} set to "true",
     * management being enabled for this member.
     *
     * @param mapServices  add started MetricsHttpService to this map if it is started.
     */
    @SuppressWarnings("rawtypes")
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
                ProxyServiceDependencies deps = MetricsHttpHelper.getDependencies((OperationalContext) cluster);

                // get context root
                Map.Entry entryResource = ((HttpAcceptorDependencies)
                       deps.getAcceptorDependencies()).getResourceConfig().entrySet().iterator().next();
                s_metricsHandler        = (MetricsHttpHandler) entryResource.getValue();
                s_metricsHandler.setPath((String) entryResource.getKey());

                // start the Metrics HTTP acceptor
                ProxyService service = (ProxyService)
                        cluster.ensureService(MetricsHttpHelper.getServiceName(), ProxyService.TYPE_DEFAULT);
                if (service.getDependencies() == null)
                    {
                    service.setDependencies(deps);
                    }
                service.start();
                mapServices.put(service, MetricsHttpHelper.getServiceName());
                }
            }
        }

    /**
     * The MetricsHttpHandler for the running metrics service.
     *
     * @since 14.1.2.0.0
     */
    public static MetricsHttpHandler s_metricsHandler;

    // ----- constants ------------------------------------------------------

    /**
     * The System property used to enabled or disable running Coherence metrics.
     * <p>
     * If this property is not set the default value used by the {@link #ensureMetricsService(Map)} method
     * is {@code false}.
     */
    public static final String PROP_METRICS_ENABLED = "coherence.metrics.http.enabled";

    /**
     * The name of the metrics configuration file.
     */
    public static final String METRICS_CONFIG = "metrics-http-config.xml";

    /**
     * Default Prometheus Metrics HTTP port.
     *
     * Registered at https://github.com/prometheus/prometheus/wiki/Default-port-allocations.
     */
    public static final int DEFAULT_PROMETHEUS_METRICS_PORT = 9612;
    }
