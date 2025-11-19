/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.common.net.HttpProxyHelper;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

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
 * Usage: This extension is NOT automatically discovered and activated by the CDI
 * container. In order to register and activate it, add it to the application's
 * {@code jakarta.enterprise.inject.spi.Extension} service definition file.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public class HttpProxyExtension
        implements Extension
    {
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
        HttpProxyHelper.configureProxy();
        }
    }
