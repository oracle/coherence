/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.http;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * A service interface that is used for discovery of HTTP applications that
 * should be deployed to {@code http-acceptor} via {@code ServiceLoader}.
 * <p/>
 * This interface should only be implemented by the classes that extend
 * {@link Application javax.ws.rs.core.Application} either directly, or
 * indirectly (typically by extending Jersey {@link ResourceConfig}).
 *
 * @author Aleks Seovic  2022.04.13
 * @since 22.06
 */
public interface HttpApplication
    {
    /**
     * Return the path for this {@code HttpApplication}.
     *
     * @return the path for this {@code HttpApplication}
     */
    default String getPath()
        {
        ApplicationPath annotation = getClass().getAnnotation(ApplicationPath.class);
        String sPath = annotation == null ? "/" : annotation.value();

        if (sPath.isEmpty() || sPath.charAt(0) != '/')
            {
            sPath = '/' + sPath;
            }
        return sPath;
        }

    /**
     * Configure and return Jersey {@link ResourceConfig} for
     * this {@code HttpApplication}.
     * <p/>
     * Can be overridden by the implementation to provide additional configuration,
     * but any overriding implementation MUST call this method first to obtain the
     * {@link ResourceConfig} instance to configure.
     *
     * @return Jersey {@link ResourceConfig} for this application
     *
     * @throws ClassCastException  if this is not an instance of JAX-RS {@link Application}.
     */
    default ResourceConfig configure()
        {
        return ResourceConfig.forApplication((Application) this);
        }
    }
