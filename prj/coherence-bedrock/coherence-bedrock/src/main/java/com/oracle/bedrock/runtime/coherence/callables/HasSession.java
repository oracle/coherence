/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.tangosol.net.Coherence;

import java.util.Collection;

/**
 * A {@link RemoteCallable} to determine whether a {@link com.tangosol.net.Session} has been
 * started using the Coherence bootstrap API.
 */
public class HasSession
        implements RemoteCallable<Boolean>
    {
    /**
     * Create a {@link HasSession} to find the default {@link com.tangosol.net.Session}
     * in any {@link Coherence} instance.
     */
    public HasSession()
        {
        this(Coherence.DEFAULT_NAME, null);
        }

    /**
     * Create a {@link HasSession} to find a specific {@link com.tangosol.net.Session}
     * in any {@link Coherence} instance.
     *
     * @param sSessionName  the name of the {@link com.tangosol.net.Session}
     */
    public HasSession(String sSessionName)
        {
        this(sSessionName, null);
        }

    /**
     * Create a {@link HasSession} to find a specific {@link com.tangosol.net.Session}
     * (optionally in a specific {@link Coherence} instance).
     *
     * @param sSessionName    the name of the {@link com.tangosol.net.Session}
     * @param sCoherenceName  the optional name of the {@link Coherence} instance
     */
    public HasSession(String sSessionName, String sCoherenceName)
        {
        f_sSessionName   = sSessionName == null ? Coherence.DEFAULT_NAME : sSessionName;
        f_sCoherenceName = sCoherenceName;
        }

    @Override
    public Boolean call() throws Exception
        {
        Collection<Coherence> col = Coherence.getInstances();
        if (col.isEmpty())
            {
            // no Coherence instances are running
            return false;
            }

        if (f_sCoherenceName == null)
            {
            return Coherence.findSession(f_sSessionName).isPresent();
            }

        Coherence coherence = Coherence.getInstance(f_sCoherenceName);
        if (coherence == null)
            {
            return false;
            }

        return coherence.hasSession(f_sSessionName);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the {@link Coherence} instance.
     */
    private final String f_sCoherenceName;

    /**
     * The name of the {@link com.tangosol.net.Session}.
     */
    private final String f_sSessionName;
    }
