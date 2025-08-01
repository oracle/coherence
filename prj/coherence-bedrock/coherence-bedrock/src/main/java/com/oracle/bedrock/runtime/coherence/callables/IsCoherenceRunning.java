/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.net.Coherence;

import java.util.Set;

/**
 * A {@link RemoteCallable} to determine whether a
 * {@link Coherence} instance is running.
 */
public class IsCoherenceRunning
        implements RemoteCallable<Boolean>, ExternalizableLite
    {
    /**
     * Constructs an {@link IsCoherenceRunning} for the
     * default {@link Coherence} instance.
     */
    public IsCoherenceRunning()
        {
        this(Set.of(Coherence.DEFAULT_NAME));
        }

    /**
     * Constructs an {@link IsCoherenceRunning}
     *
     * @param name the optional name of the Coherence instance
     */
    public IsCoherenceRunning(String name)
        {
        this(name == null ? Set.of() : Set.of(name));
        }

    /**
     * Constructs an {@link IsCoherenceRunning}
     *
     * @param setName the optional names of the Coherence instances
     */
    public IsCoherenceRunning(Set<String> setName)
        {
        if (setName == null || setName.isEmpty())
            {
            m_asName = DEFAULT_NAMES;
            }
        else
            {
            m_asName = setName.toArray(String[]::new);
            }
        }


    @Override
    public Boolean call()
        {
        for (String sName : m_asName)
            {
            Coherence coherence = Coherence.getInstance(sName);
            if (coherence != null)
                {
                if (!coherence.whenStarted().isDone() || !coherence.isStarted())
                    {
                    return false;
                    }
                }
            else
                {
                return false;
                }
            }
        return true;
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

    private static final String[] DEFAULT_NAMES = new String[]{Coherence.DEFAULT_NAME};

    /**
     * The names of the {@link Coherence} instances.
     */
    private final String[] m_asName;
    }
