/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.Coherence;

/**
 * A {@link RemoteCallable} to determine whether a
 * {@link Coherence} instance is running.
 */
public class IsCoherenceRunning
        implements RemoteCallable<Boolean>
    {
    /**
     * Constructs an {@link IsCoherenceRunning} for the
     * default {@link Coherence} instance.
     */
    public IsCoherenceRunning()
        {
        this(Coherence.DEFAULT_NAME);
        }


    /**
     * Constructs an {@link IsCoherenceRunning}
     *
     * @param name the optional name of the Coherence instance
     */
    public IsCoherenceRunning(String name)
        {
        m_name = name == null ? Coherence.DEFAULT_NAME : name;
        }


    @Override
    public Boolean call() throws Exception
        {
        return Coherence.getInstances()
                .stream()
                .filter(c -> m_name.equals(c.getName()))
                .map(c -> c.whenStarted().isDone() && c.isStarted())
                .findFirst()
                .orElse(false);
        }

    // ----- helper methods -------------------------------------------------

    public static IsCoherenceRunning instance()
        {
        return s_fInstance;
        }

    public static IsCoherenceRunning named(String sName)
        {
        return new IsCoherenceRunning(sName);
        }

    // ----- constants ------------------------------------------------------

    /**
     * A singleton default instance.
     */
    private static final IsCoherenceRunning s_fInstance = new IsCoherenceRunning();

    // ----- data members ---------------------------------------------------

    /**
     * The name of the {@link Coherence} instance.
     */
    private final String m_name;
    }
