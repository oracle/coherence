/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.util;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CDI extension for automatic HTTP proxy configuration from environment variables.
 * <p/>
 * This extension automatically configures Java HTTP proxy settings based on
 * standard environment variables during CDI container startup. It supports
 * the common proxy environment variables used in containerized and enterprise
 * environments, ensuring seamless integration with corporate network infrastructure.
 * <p/>
 * Supported environment variables:
 * <ul>
 *   <li>{@code HTTP_PROXY} or {@code http_proxy} - HTTP proxy URL</li>
 *   <li>{@code HTTPS_PROXY} or {@code https_proxy} - HTTPS proxy URL</li>
 *   <li>{@code NO_PROXY} or {@code no_proxy} - Comma-separated list of hosts to bypass proxy</li>
 * </ul>
 * <p/>
 * The extension only sets proxy properties if they are not already configured,
 * allowing explicit system property configuration to take precedence. This
 * provides flexibility for different deployment scenarios.
 * <p/>
 * Proxy URL format: {@code http://[username:password@]host:port}
 * <p/>
 * NO_PROXY format: {@code localhost,.example.com,192.168.1.0/24}
 * <p/>
 * Usage: This extension is automatically discovered and activated by the CDI
 * container if present on the classpath. No explicit configuration is required.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public class HttpProxyExtension
        implements Extension
    {
    /**
     * Logger for proxy configuration messages.
     * <p/>
     * Uses the "coherence" logger to align with other Coherence RAG logging.
     */
    private static final Logger LOGGER = Logger.getLogger("coherence");

    /**
     * Configures HTTP proxy settings during CDI container startup.
     * <p/>
     * This method is automatically called by the CDI container before bean
     * discovery begins. It reads proxy configuration from environment variables
     * and sets the corresponding Java system properties.
     * 
     * @param event the BeforeBeanDiscovery event (unused but required by CDI)
     */
    public void configureProxy(@Observes BeforeBeanDiscovery event)
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
    private void configureProxyForScheme(String scheme)
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
                    LOGGER.log(Level.CONFIG, "Set {0} = {1}", new Object[] {proxyHostProperty, host});
                    }
                else
                    {
                    LOGGER.log(Level.FINE, "{0} already set, skipping", proxyHostProperty);
                    }

                if (System.getProperty(proxyPortProperty) == null)
                    {
                    System.setProperty(proxyPortProperty, String.valueOf(port));
                    LOGGER.log(Level.CONFIG, "Set {0} = {1}", new Object[] {proxyPortProperty, port});
                    }
                else
                    {
                    LOGGER.log(Level.FINE, "{0} already set, skipping", proxyPortProperty);
                    }

                }
            catch (Exception e)
                {
                LOGGER.log(Level.WARNING, "Failed to parse " + envVar + " = " + proxyEnv, e);
                }
            }
        else
            {
            LOGGER.log(Level.CONFIG, "Environment variable {0} not set", envVar);
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
    private void configureNonProxyHosts()
        {
        String noProxyEnv = System.getenv("NO_PROXY");
        if (noProxyEnv == null)
            {
            noProxyEnv = System.getenv("no_proxy");
            }
        if (noProxyEnv != null)
            {
            String formatted = noProxyEnv.replace(",", "|").replaceAll("\\s+", "");

            // Java expects *.domain.com instead of just .domain.com
            formatted = formatted.replaceAll("\\|\\.", "|*."); // replace "|." with "|*."

            if (formatted.startsWith("."))
                {
                formatted = "*" + formatted;
                }

            if (System.getProperty("http.nonProxyHosts") == null)
                {
                System.setProperty("http.nonProxyHosts", formatted);
                LOGGER.log(Level.CONFIG, "Set http.nonProxyHosts = {0}", formatted);
                }
            else
                {
                LOGGER.log(Level.FINE, "http.nonProxyHosts already set, skipping");
                }
            }
        else
            {
            LOGGER.log(Level.CONFIG, "NO_PROXY environment variable not set");
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
    private int defaultPortForScheme(String scheme)
        {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
        }
    }
