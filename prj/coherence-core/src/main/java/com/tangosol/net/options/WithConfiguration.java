/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.options;

import com.oracle.coherence.common.util.Options;

import com.tangosol.config.ConfigurationException;

import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.Session;
import com.tangosol.net.SessionProvider;

/**
 * An {@link com.tangosol.net.Session.Option} specifying the location
 * of a module configuration descriptor for a {@link Session}.
 *
 * @see Session
 * @see SessionProvider
 *
 * @author bo  2015.07.27
 */
public interface WithConfiguration
        extends Session.Option
    {

    // ----- WithConfiguration methods --------------------------------------

    /**
     * Obtains the location of a module descriptor file, a URI represented
     * as a String.
     *
     * @return the location (URI) of the module descriptor
     */
    String getLocation();

    // ----- helper methods -------------------------------------------------

    /**
     * Obtains a {@link WithConfiguration} that will auto-detect the location
     * of a module descriptor.
     *
     * @return a {@link WithConfiguration}
     */
    @Options.Default
    static WithConfiguration autoDetect()
        {
        return () -> CacheFactoryBuilder.URI_DEFAULT;
        }

    /**
     * Creates a {@link WithConfiguration} for a specific module descriptor
     * location, a URI represented as a String.
     *
     * @return a {@link WithConfiguration}
     */
    static WithConfiguration using(String sUri) throws ConfigurationException
        {
        return () -> sUri;
        }
    }
