/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import javax.annotation.Priority;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Alternative;

import com.tangosol.net.options.WithConfiguration;

/**
 * A class that can resolve the URI of a Coherence cache configuration file from
 * a {@code String} value.
 *
 * @author Jonathan Knight  2019.10.21
 * @since 20.06
 */
public interface CacheFactoryUriResolver
    {
    /**
     * The priority for the default {@link CacheFactoryUriResolver} bean.
     */
    public int PRIORITY = 0;

    /**
     * Resolve a String value into a Coherence cache configuration URI.
     *
     * @param sValue  the value to resolve
     *
     * @return the cache configuration URI
     */
    public String resolve(String sValue);

    // ---- inner class: Default --------------------------------------------

    /**
     * The default implementation of a {@link CacheFactoryUriResolver}.
     * <p>
     * This implementation returns the passed in {@code value} unchanged.
     */
    @ApplicationScoped
    @Alternative
    @Priority(PRIORITY)
    class Default
            implements CacheFactoryUriResolver
        {
        @Override
        public String resolve(String sValue)
            {
            if (sValue == null || sValue.trim().isEmpty())
                {
                return WithConfiguration.autoDetect().getLocation();
                }
            return sValue;
            }
        }
    }
