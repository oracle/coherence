/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import javax.enterprise.inject.spi.DefinitionException;

/**
 * Abstract base class for caching CDI interceptors.
 */
public abstract class AbstractCacheInterceptor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct cache interceptor.
     *
     * @param coherence  the Coherence instance
     * @param extension  the Coherence extension
     */
    public AbstractCacheInterceptor(Coherence coherence, CoherenceExtension extension)
        {
        f_coherence = coherence;
        f_extension = extension;
        }

    /**
     * Obtains the named {@link Session} or the default one if session name
     * was not specified.
     *
     * @param sName  session name
     *
     * @return  the Coherence session
     */
    protected Session getSession(String sName)
        {
        String sSessionName;
        if (sName == null || sName.trim().isEmpty())
            {
            sSessionName = Coherence.DEFAULT_NAME;
            }
        else
            {
            sSessionName = sName;
            }

        return getCoherence().getSessionIfPresent(sSessionName)
                .orElseThrow(() -> new DefinitionException("No Session is configured with name " + sSessionName));
        }

    protected Coherence getCoherence()
        {
        return f_coherence;
        }

    protected CoherenceExtension getExtension()
        {
        return f_extension;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Coherence instance.
     */
    private final Coherence f_coherence;

    /**
     * Coherence CDI extension.
     */
    private final CoherenceExtension f_extension;
    }
