/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;

import com.oracle.coherence.common.base.Logger;

import java.net.URI;

/**
 * Helper class for automatic Java HTTP proxy configuration from environment variables.
 * <p/>
 * This class automatically configures Java HTTP proxy settings based on standard
 * environment variables. It supports the common proxy environment variables used
 * in containerized and enterprise environments, ensuring seamless integration with
 * corporate network infrastructure.
 * <p/>
 * Supported environment variables:
 * <ul>
 *   <li>{@code HTTP_PROXY} or {@code http_proxy} - HTTP proxy URL</li>
 *   <li>{@code HTTPS_PROXY} or {@code https_proxy} - HTTPS proxy URL</li>
 *   <li>{@code NO_PROXY} or {@code no_proxy} - Comma-separated list of hosts to bypass proxy</li>
 * </ul>
 * <p/>
 * The class only sets proxy properties if they are not already configured,
 * allowing explicit system property configuration to take precedence. This
 * provides flexibility for different deployment scenarios.
 * <p/>
 * Proxy URL format: {@code http://[username:password@]host:port}
 * <p/>
 * NO_PROXY format: {@code localhost,.example.com,192.168.1.0/24}
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public class HttpProxyHelper
    {
    /**
     * Configures Java HTTP proxy settings from standard environment variables.
     * <p/>
     * Supported environment variables:
     * <ul>
     *   <li>{@code HTTP_PROXY} or {@code http_proxy} - HTTP proxy URL</li>
     *   <li>{@code HTTPS_PROXY} or {@code https_proxy} - HTTPS proxy URL</li>
     *   <li>{@code NO_PROXY} or {@code no_proxy} - Comma-separated list of hosts to bypass proxy</li>
     * </ul>
     */
    public static void configureProxy()
        {
        configureProxyForScheme("http");
        configureProxyForScheme("https");
        configureNonProxyHosts();
        }

    /**
     * Configures proxy settings for a specific URL scheme (http or https).
     * <p/>
     * This method reads the proxy configuration from environment variables
     * for the specified scheme and sets the corresponding Java system properties.
     * It handles both uppercase and lowercase environment variable names for
     * broader compatibility.
     * 
     * @param scheme the URL scheme to configure ("http" or "https")
     */
    private static void configureProxyForScheme(String scheme)
        {
        String envVar = scheme.toUpperCase() + "_PROXY";
        String proxyEnv = System.getenv(envVar);
        if (proxyEnv == null)
            {
            proxyEnv = System.getenv(envVar.toLowerCase()); // fallback to lowercase
            }
        if (proxyEnv != null)
            {
            try
                {
                URI uri = new URI(proxyEnv);
                String host = uri.getHost();
                int port = uri.getPort() != -1
                           ? uri.getPort()
                           : defaultPortForScheme(scheme);

                String proxyHostProperty = scheme + ".proxyHost";
                String proxyPortProperty = scheme + ".proxyPort";

                if (System.getProperty(proxyHostProperty) == null)
                    {
                    System.setProperty(proxyHostProperty, host);
                    Logger.config("Set %s = %s".formatted(proxyHostProperty, host));
                    }
                else
                    {
                    Logger.fine("%s already set, skipping".formatted(proxyHostProperty));
                    }

                if (System.getProperty(proxyPortProperty) == null)
                    {
                    System.setProperty(proxyPortProperty, String.valueOf(port));
                    Logger.config("Set %s = %s".formatted(proxyPortProperty, port));
                    }
                else
                    {
                    Logger.fine("%s already set, skipping".formatted(proxyPortProperty));
                    }

                }
            catch (Exception e)
                {
                Logger.warn("Failed to parse %s = %s".formatted(envVar, proxyEnv), e);
                }
            }
        else
            {
            Logger.config("Environment variable %s not set".formatted(envVar));
            }
        }

    /**
     * Configures the list of hosts that should bypass the proxy.
     * <p/>
     * This method reads the NO_PROXY environment variable and converts it
     * to the format expected by Java's http.nonProxyHosts system property.
     * The conversion includes:
     * <ul>
     *   <li>Replacing commas with pipes (|) as separators</li>
     *   <li>Removing whitespace</li>
     *   <li>Converting .domain.com patterns to *.domain.com format</li>
     * </ul>
     * <p/>
     * Both uppercase (NO_PROXY) and lowercase (no_proxy) environment
     * variables are supported for compatibility.
     */
    private static void configureNonProxyHosts()
        {
        String noProxyEnv = System.getenv("NO_PROXY");
        if (noProxyEnv == null)
            {
            noProxyEnv = System.getenv("no_proxy");
            }
        if (noProxyEnv != null)
            {
            String nonProxyHosts = noProxyEnv.replace(",", "|").replaceAll("\\s+", "");

            // Java expects *.domain.com instead of just .domain.com
            nonProxyHosts = nonProxyHosts.replaceAll("\\|\\.", "|*."); // replace "|." with "|*."

            if (nonProxyHosts.startsWith("."))
                {
                nonProxyHosts = "*" + nonProxyHosts;
                }

            if (System.getProperty("http.nonProxyHosts") == null)
                {
                System.setProperty("http.nonProxyHosts", nonProxyHosts);
                Logger.config("Set http.nonProxyHosts = %s".formatted(nonProxyHosts));
                }
            else
                {
                Logger.fine("http.nonProxyHosts already set, skipping");
                }
            }
        else
            {
            Logger.config("NO_PROXY environment variable not set");
            }
        }

    /**
     * Returns the default port number for a given URL scheme.
     * <p/>
     * This method provides fallback port numbers when the proxy URL
     * does not explicitly specify a port.
     * 
     * @param scheme the URL scheme ("http" or "https")
     * 
     * @return 443 for HTTPS, 80 for HTTP and other schemes
     */
    private static int defaultPortForScheme(String scheme)
        {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
        }
    }
