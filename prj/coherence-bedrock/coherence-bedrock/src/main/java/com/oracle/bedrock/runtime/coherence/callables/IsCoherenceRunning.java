/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.Coherence;

public class IsCoherenceRunning
        implements RemoteCallable<Boolean>
    {
    /**
     * The name of the {@link Coherence} instance.
     */
    private final String name;


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
     * @param name the optional name of the service
     */
    public IsCoherenceRunning(String name)
        {
        this.name = name == null ? Coherence.DEFAULT_NAME : name;
        }


    @Override
    public Boolean call() throws Exception
        {
        return Coherence.getInstances()
                .stream()
                .filter(c -> name.equals(c.getName()))
                .map(Coherence::isStarted)
                .findFirst()
                .orElse(false);
        }
    }
