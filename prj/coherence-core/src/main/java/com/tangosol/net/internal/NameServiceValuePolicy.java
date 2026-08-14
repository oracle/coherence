/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.net.NameService;

import java.lang.reflect.Array;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

/**
 * NameServiceValuePolicy centralizes the passive value contract for
 * NameService bind and lookup boundaries.
 *
 * @author Aleks Seovic  2026.05.17
 *
 * @since 26.07
 */
public final class NameServiceValuePolicy
    {
    // ----- constructors ---------------------------------------------------

    private NameServiceValuePolicy()
        {
        }

    // ----- validation methods --------------------------------------------

    /**
     * Validate a NameService bind value before it is stored.
     *
     * @param sName    the NameService name
     * @param oValue   the value
     * @param fRemote  true for a remote client bind
     */
    public static void validateBindResource(String sName, Object oValue, boolean fRemote)
        {
        if (oValue instanceof NameService.Resolvable)
            {
            if (fRemote)
                {
                throw new IllegalArgumentException("remote NameService Resolvable bind is not supported: " + sName);
                }
            if (!isProductResolvable(oValue))
                {
                throw new IllegalArgumentException("unsupported NameService Resolvable bind: " + oValue.getClass().getName());
                }
            return;
            }

        validatePassiveValue("bind " + sName, oValue);
        }

    /**
     * Validate a NameService lookup result before it crosses the protocol
     * boundary.
     *
     * @param sName    the NameService name
     * @param oResult  the lookup result
     */
    public static void validateLookupResult(String sName, Object oResult)
        {
        validatePassiveValue("lookup " + sName, oResult);
        }

    /**
     * Validate a management JMX service URL obtained from NameService.
     *
     * @param url  the URL
     *
     * @return the validated URL
     */
    public static JMXServiceURL validateJmxServiceURL(JMXServiceURL url)
        {
        if (url == null)
            {
            return null;
            }

        String sProtocol = url.getProtocol();
        if ("iiop".equalsIgnoreCase(sProtocol) || "iiops".equalsIgnoreCase(sProtocol)
                || "t3".equalsIgnoreCase(sProtocol) || "t3s".equalsIgnoreCase(sProtocol))
            {
            String sPath = url.getURLPath();
            if (url.getHost() == null || url.getHost().isEmpty() || url.getPort() <= 0
                    || sPath == null || !sPath.startsWith("/jndi/") || sPath.length() == "/jndi/".length())
                {
                throw new IllegalArgumentException("unsupported IIOP/T3 JMX service URL: " + url);
                }
            return url;
            }

        if (!"rmi".equalsIgnoreCase(sProtocol))
            {
            throw new IllegalArgumentException("unsupported JMX service URL protocol: " + sProtocol);
            }

        String sPath = url.getURLPath();
        if (sPath == null || sPath.isEmpty())
            {
            return url;
            }
        if (!sPath.startsWith("/jndi/"))
            {
            throw new IllegalArgumentException("unsupported JMX service URL path: " + sPath);
            }

        String sUri = sPath.substring("/jndi/".length());
        try
            {
            URI uri = new URI(sUri);
            if (!"rmi".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)
                {
                throw new IllegalArgumentException("unsupported JMX service URL provider protocol: " + uri.getScheme());
                }
            }
        catch (URISyntaxException e)
            {
            throw new IllegalArgumentException("invalid JMX service URL provider path: " + sPath, e);
            }

        return url;
        }

    /**
     * Validate a management JMX service URL received from the cluster
     * management connector publish path.
     *
     * @param url  the URL
     *
     * @return the validated URL
     */
    public static JMXServiceURL validateManagementPublishJmxServiceURL(JMXServiceURL url)
        {
        try
            {
            return validateJmxServiceURL(url);
            }
        catch (IllegalArgumentException e)
            {
            if (isRmiStubJmxServiceURL(url))
                {
                CoherenceMode mode      = CoherenceMode.current();
                boolean       fHardened = CoherenceMode.isSecurityHardeningEnabled();
                String        sLog      = dynamicRmiStubManagementPublishMessage(url, mode, fHardened);
                if (fHardened)
                    {
                    throw new IllegalArgumentException(sLog, e);
                    }
                Logger.warn(sLog);
                return url;
                }
            throw e;
            }
        }

    /**
     * Convert an exception to a passive NameService failure result.
     *
     * @param e  the exception
     *
     * @return the failure result
     */
    public static String failureResult(Throwable e)
        {
        String sMessage = e.getMessage();
        return e.getClass().getName() + (sMessage == null || sMessage.isEmpty() ? "" : ": " + sMessage);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Validate a passive NameService value.
     *
     * @param sPath   the value path
     * @param oValue  the value
     */
    private static void validatePassiveValue(String sPath, Object oValue)
        {
        validatePassiveValue(sPath, oValue, 0);
        }

    /**
     * Validate a passive NameService value.
     *
     * @param sPath   the value path
     * @param oValue  the value
     * @param cDepth  the current nesting depth
     */
    private static void validatePassiveValue(String sPath, Object oValue, int cDepth)
        {
        if (cDepth > MAX_DEPTH)
            {
            throw new IllegalArgumentException("NameService value is too deeply nested: " + sPath);
            }

        if (oValue == null || isScalar(oValue))
            {
            return;
            }

        Class<?> clz = oValue.getClass();
        if (clz.isArray())
            {
            int cElements = Array.getLength(oValue);
            if (cElements > MAX_ELEMENTS)
                {
                throw new IllegalArgumentException("NameService array value is too large: " + sPath);
                }
            for (int i = 0; i < cElements; i++)
                {
                validatePassiveValue(sPath + '[' + i + ']', Array.get(oValue, i), cDepth + 1);
                }
            return;
            }

        if (oValue instanceof Collection)
            {
            Collection<?> collection = (Collection<?>) oValue;
            if (collection.size() > MAX_ELEMENTS)
                {
                throw new IllegalArgumentException("NameService collection value is too large: " + sPath);
                }

            int i = 0;
            for (Iterator<?> iter = collection.iterator(); iter.hasNext(); )
                {
                validatePassiveValue(sPath + '[' + i++ + ']', iter.next(), cDepth + 1);
                }
            return;
            }

        if (oValue instanceof Map)
            {
            Map<?, ?> map = (Map<?, ?>) oValue;
            if (map.size() > MAX_ELEMENTS)
                {
                throw new IllegalArgumentException("NameService map value is too large: " + sPath);
                }

            for (Map.Entry<?, ?> entry : map.entrySet())
                {
                validatePassiveValue(sPath + ".key", entry.getKey(), cDepth + 1);
                validatePassiveValue(sPath + ".value", entry.getValue(), cDepth + 1);
                }
            return;
            }

        throw new IllegalArgumentException("unsupported NameService value type at " + sPath + ": " + clz.getName());
        }

    /**
     * Return true if the URL is an RMI stub URL.
     *
     * @param url  the URL
     *
     * @return true if the URL is an RMI stub URL
     */
    private static boolean isRmiStubJmxServiceURL(JMXServiceURL url)
        {
        if (url == null || !"rmi".equalsIgnoreCase(url.getProtocol()))
            {
            return false;
            }

        String sPath = url.getURLPath();
        return sPath != null && sPath.startsWith("/stub/");
        }

    /**
     * Return the dynamic RMI stub management publish message.
     *
     * @param url        the URL
     * @param mode       the current mode
     * @param fHardened  {@code true} if security hardening is enabled
     *
     * @return the message
     */
    private static String dynamicRmiStubManagementPublishMessage(JMXServiceURL url, CoherenceMode mode,
                                                                 boolean fHardened)
        {
        String sMode   = mode.name().toLowerCase(Locale.ROOT);
        String sResult = fHardened ? "reject" : "would_reject";
        String sSecurityMode = fHardened ? "hardened" : "compatibility";
        String sIntro  = fHardened
                ? "Dynamic RMI stub JMX service URLs are not allowed for management publish when security hardening is enabled"
                : "Allowed compatibility management publish JMX service URL that security hardening would reject";

        return sIntro
                + ": route=management-publish"
                + ", gate=jmx-service-url"
                + ", reason=rmi-stub-url"
                + ", mode=" + sMode
                + ", security-mode=" + sSecurityMode
                + ", result=" + sResult
                + ", value=" + summarizeJmxUrl(url)
                + ". Configure " + PROP_MANAGEMENT_REMOTE_REGISTRYPORT
                + " to publish a static /jndi/rmi://.../server JMX URL instead.";
        }

    /**
     * Return a bounded URL summary suitable for warnings.
     *
     * @param url  the URL
     *
     * @return a bounded URL summary
     */
    private static String summarizeJmxUrl(JMXServiceURL url)
        {
        if (url == null)
            {
            return "null";
            }

        String sPath = url.getURLPath();
        if (sPath != null && sPath.startsWith("/stub/"))
            {
            sPath = "/stub/...(" + sPath.length() + " chars)";
            }

        return "service:jmx:" + url.getProtocol() + "://"
                + (url.getHost() == null ? "" : url.getHost())
                + (url.getPort() < 0 ? "" : ":" + url.getPort())
                + (sPath == null ? "" : sPath);
        }

    /**
     * Return true if the value is an allowed passive scalar.
     *
     * @param oValue  the value
     *
     * @return true if the value is allowed
     */
    private static boolean isScalar(Object oValue)
        {
        return oValue instanceof String
                || oValue instanceof Byte
                || oValue instanceof Short
                || oValue instanceof Integer
                || oValue instanceof Long
                || oValue instanceof Float
                || oValue instanceof Double
                || oValue instanceof Boolean
                || oValue instanceof Character
                || oValue instanceof InetAddress
                || oValue instanceof InetSocketAddress;
        }

    /**
     * Return true if the value is one of the product-local Resolvable
     * registrations that is resolved before crossing the wire.
     *
     * @param oValue  the value
     *
     * @return true if the Resolvable is allowed
     */
    private static boolean isProductResolvable(Object oValue)
        {
        String sClassName = oValue.getClass().getName();
        return "com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService".equals(sClassName)
                || "com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.FederatedCache".equals(sClassName)
                || "com.tangosol.coherence.component.net.management.Connector".equals(sClassName);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Maximum nested value depth.
     */
    private static final int MAX_DEPTH = 8;

    /**
     * Maximum array, collection, or map size.
     */
    private static final int MAX_ELEMENTS = 1024;

    /**
     * Management remote registry port property.
     */
    private static final String PROP_MANAGEMENT_REMOTE_REGISTRYPORT = "coherence.management.remote.registryport";
    }
