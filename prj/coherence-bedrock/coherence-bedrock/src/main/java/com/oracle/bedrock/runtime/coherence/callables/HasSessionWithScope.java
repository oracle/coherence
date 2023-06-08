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
 * started with a specific scope using the Coherence bootstrap API.
 */
public class HasSessionWithScope
        implements RemoteCallable<Boolean>
    {
    /**
     * Create a {@link HasSessionWithScope} to find a {@link com.tangosol.net.Session}
     * with the default scope in any {@link Coherence} instance.
     */
    public HasSessionWithScope()
        {
        this(Coherence.DEFAULT_SCOPE, null);
        }

    /**
     * Create a {@link HasSessionWithScope} to find a {@link com.tangosol.net.Session}
     * with a specific scope in any {@link Coherence} instance.
     *
     * @param sSessionName  the name of the {@link com.tangosol.net.Session}
     */
    public HasSessionWithScope(String sSessionName)
        {
        this(sSessionName, null);
        }

    /**
     * Create a {@link HasSessionWithScope} to find a {@link com.tangosol.net.Session}
     * with a specific scope (optionally in a specific {@link Coherence} instance).
     *
     * @param sScopeName      the scope of the {@link com.tangosol.net.Session}
     * @param sCoherenceName  the optional name of the {@link Coherence} instance
     */
    public HasSessionWithScope(String sScopeName, String sCoherenceName)
        {
        f_sScopeName     = sScopeName == null ? Coherence.DEFAULT_SCOPE : sScopeName;
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
            return !Coherence.findSessionsByScope(f_sScopeName).isEmpty();
            }

        Coherence coherence = Coherence.getInstance(f_sCoherenceName);
        if (coherence == null)
            {
            return false;
            }

        return !coherence.getSessionsWithScope(f_sScopeName).isEmpty();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the {@link Coherence} instance.
     */
    private final String f_sCoherenceName;

    /**
     * The {@link com.tangosol.net.Session} scope name.
     */
    private final String f_sScopeName;
    }
