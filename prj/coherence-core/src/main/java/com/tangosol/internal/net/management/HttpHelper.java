/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.management.MapJsonBodyHandler;

import com.tangosol.internal.net.service.grid.DefaultProxyServiceDependencies;
import com.tangosol.internal.net.service.grid.LegacyXmlProxyServiceHelper;

import com.tangosol.internal.net.service.grid.ProxyServiceDependencies;
import com.tangosol.internal.net.service.peer.acceptor.HttpAcceptorDependencies;

import com.tangosol.net.Cluster;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.ProxyService;
import com.tangosol.net.Service;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Resources;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Helper class to ensure a Management over HTTP service.
 *
 * @author phf  2018.08.03
 * @since 12.2.1.4.0
 */
public abstract class HttpHelper
    {
    /**
     * Helper method to build a {@link URL} to the Management over HTTP service given
     * a host and port.
     *
     * @param sHost  the Management over HTTP listening address
     * @param nPort  the Management over HTTP listening port
     *
     * @return the Management over HTTP URL
     *
     * @throws MalformedURLException if the URL cannot be created
     */
    public static URL composeURL(String sHost, int nPort)
            throws MalformedURLException
        {
        return composeURL(sHost, nPort, "http");
        }

    /**
     * Helper method to build a {@link URL} to the Management over HTTP(S) service given
     * a host, port, and protocol.
     *
     * @param sHost      the Management over HTTP listening address
     * @param nPort      the Management over HTTP listening port
     * @param sProtocol  "http" or "https"
     *
     * @return the Management over HTTP/HTTPS URL
     *
     * @throws MalformedURLException if the URL cannot be created
     */
    public static URL composeURL(String sHost, int nPort, String sProtocol)
            throws MalformedURLException
        {
        return new URL(sProtocol, sHost, nPort, DEFAULT_CLUSTER_PATH);
        }

    /**
     * Get the Management over HTTP service name.
     *
     * @return the Management over HTTP service name
     */
    public static String getServiceName()
        {
        return MANAGEMENT_SERVICE_NAME;
        }

    /**
     * Whether this Coherence node is capable of running the Management over HTTP service.
     *
     * @return whether this Coherence node is capable of running the Management over HTTP service
     */
    public static boolean isHttpCapable(ClassLoader ignored)
        {
        try
            {
            // The resources for Management over REST are now in coherence core.
            // All we need to run Management over REST is an implementation of MapJsonBodyHandler
            // discovered via the ServiceLoader (there is one in the Coherence JSON module)
            MapJsonBodyHandler.ensureMapJsonBodyHandler();
            }
        catch (Throwable t)
            {
            // don't bother logging the stack trace as having a stack trace in the logs can be alarming to administrators
            StringBuilder sMsg = new StringBuilder(t.getMessage());
            Throwable cause = t.getCause();
            while (cause != null)
                {
                sMsg.append(", caused by: ").append(cause.getMessage());
                cause = cause.getCause();
                }
            Logger.finest("One or more dependencies are missing for management over HTTP: " + sMsg);
            return false;
            }

        return true;
        }

    /**
     * When pre-conditions are met, start the Management over REST HTTP service.
     * <p>
     * Pre-conditions for starting the service include operational configuration element <code>management-config.http-managed-nodes</code>
     * being set to "all" or "inherit", management being enabled for this member and the implementation of Management over REST service is in classpath.
     *
     * @param cluster  the running {@link Cluster}
     *
     * @return true on successful service start
     */
    public static boolean startService(Cluster cluster)
        {
        ClassLoader classLoader = cluster.getContextClassLoader();

        if (isHttpCapable(classLoader))
            {
            ProxyServiceDependencies deps = null;
            try
                {
                URL urlConfig = Resources.findFileOrResourceOrDefault(MANAGEMENT_CONFIG, classLoader);

                if (urlConfig == null)
                    {
                    throw new IllegalStateException("Unable to locate " + MANAGEMENT_CONFIG + " which should be"
                        + " resolvable from the coherence-management module on the class path.");
                    }

                XmlElement xml = XmlHelper.loadXml(urlConfig);
                Logger.info("Loaded management over REST configuration from \"" + urlConfig + '"');
                XmlHelper.replaceSystemProperties(xml, "system-property");

                //noinspection deprecation
                deps = LegacyXmlProxyServiceHelper.fromXml(
                    xml, new DefaultProxyServiceDependencies(), (OperationalContext) cluster, classLoader);

                Service service = cluster.ensureService(getServiceName(), ProxyService.TYPE_DEFAULT);
                service.setDependencies(deps);
                service.start();

                return true;
                }
            catch (Throwable t)
                {
                Throwable tOriginal = t instanceof RuntimeException ? Base.getOriginalException((RuntimeException) t) : t;
                Throwable tCause    = tOriginal.getCause() == null ? tOriginal : tOriginal.getCause();

                if (tCause instanceof ClassNotFoundException || tCause instanceof NoClassDefFoundError)
                    {
                    Logger.err("Management over HTTP is not available most likely due to this member missing "
                        + "the necessary libraries to run the service. Handled exception: " + tCause.getClass().getCanonicalName() + ": " + tCause.getLocalizedMessage());
                    }
                else
                    {
                    // could be IOException for address in use or SecurityException or IllegalArgumentException for Configuration Errors
                    StringBuilder sb = new StringBuilder();

                    sb.append("failed to start service ").append(HttpHelper.getServiceName());
                    if (deps != null)
                        {
                        HttpAcceptorDependencies depsHttpAcceptor = (HttpAcceptorDependencies) deps.getAcceptorDependencies();

                        sb.append(" at address ").append(depsHttpAcceptor.getLocalAddress())
                          .append(":").append(depsHttpAcceptor.getLocalPort());
                        }
                    sb.append(" due to ").append(tCause.getClass().getSimpleName())
                      .append(" : ").append(tCause.getLocalizedMessage());
                    Logger.err(sb.toString());
                    }

                return false;
                }
            }
        else
            {
            Logger.err("Management over HTTP is not available most likely due to missing classes");
            return false;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Default Management over REST HTTP port.
     */
    public static final int DEFAULT_MANAGEMENT_OVER_REST_PORT = 30000;

    /**
     * The name of the Management over REST proxy service.
     */
    // NOTE - this name MUST match the service name in management-http-config.xml
    public static final String MANAGEMENT_SERVICE_NAME = "ManagementHttpProxy";

    /**
     * The default path to the Cluster endpoint.
     */
    public static final String DEFAULT_CLUSTER_PATH = "/management/coherence/cluster";

    /**
     * The name of the Management over REST proxy configuration file.
     */
    public static final String MANAGEMENT_CONFIG = "management-http-config.xml";
    }
