/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.oracle.coherence.common.base.Logger;

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
     * @throws MalformedURLException
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
     * @throws MalformedURLException
     */
    public static URL composeURL(String sHost, int nPort, String sProtocol)
            throws MalformedURLException
        {
        return new URL(sProtocol, sHost, nPort, "/management/coherence/cluster");
        }

    /**
     * Get the Management over HTTP service name.
     *
     * @return the Management over HTTP service name
     */
    public static String getServiceName()
        {
        return "ManagementHttpProxy";
        }

    /**
     * Whether this Coherence node is capable of running the Management over HTTP service.
     *
     * @return whether this Coherence node is capable of running the Management over HTTP service
     */
    public static boolean isHttpCapable(ClassLoader classLoader)
        {
        try
            {
            // check that the coherence-rest module is accessible
            classLoader.loadClass("com.tangosol.coherence.management.internal.ManagementResourceConfig");
            }
        catch (Throwable t)
            {
            // don't bother logging the stack trace as having a stack trace in the logs can be alarming to administrators
            Logger.finest("One or more libraries are missing for management over HTTP: " + t);

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
                String sConfigFile = "management-http-config.xml";
                URL    urlConfig   = classLoader.getResource(sConfigFile);

                if (urlConfig == null)
                    {
                    throw new IllegalStateException("Unable to locate " + sConfigFile + " which should be"
                        + " resolvable from the coherence-management module on the class path.");
                    }

                XmlElement xml = XmlHelper.loadXml(urlConfig);
                XmlHelper.replaceSystemProperties(xml, "system-property");

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

                if (tCause instanceof ClassNotFoundException)
                    {
                    Logger.err("Management over HTTP is not available most likely due to this member missing "
                        + "the necessary libraries to run the service. Handled exception: " + tCause.getClass().getCanonicalName() + ": " + tCause.getLocalizedMessage());
                    return false;
                    }
                else
                    {
                    // could be IOException for address in use or SecurityException or IllegalArgumentException for Configuration Errors
                    HttpAcceptorDependencies depsHttpAcceptor = (HttpAcceptorDependencies) deps.getAcceptorDependencies();

                    Logger.err("failed to start service " + HttpHelper.getServiceName() + " at address " + depsHttpAcceptor.getLocalAddress() + ":" +
                        depsHttpAcceptor.getLocalPort() + " due to " + tCause.getClass().getSimpleName() + " : " + tCause.getLocalizedMessage());
                    return false;
                    }
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
    }
