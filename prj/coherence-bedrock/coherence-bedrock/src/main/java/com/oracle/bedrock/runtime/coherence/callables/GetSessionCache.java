/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

/**
 * A {@link RemoteCallable} to obtain a {@link NamedCache} from a specific {@link Session}.
 */
@SuppressWarnings("rawtypes")
public class GetSessionCache
        implements RemoteCallable<NamedCache>
    {
    /**
     * The name of the {@link Coherence} instance.
     */
    private final String coherenceName;

    /**
     * The name of the {@link Session} instance.
     */
    private final String sessionName;

    /**
     * The name of the {@link NamedCache}.
     */
    private final String cacheName;

    public GetSessionCache(String coherenceName, String sessionName, String cacheName)
        {
        this.coherenceName = coherenceName == null ? Coherence.DEFAULT_NAME : coherenceName;
        this.sessionName = sessionName == null ? Coherence.DEFAULT_NAME : sessionName;
        this.cacheName = cacheName;
        }

    @Override
    public NamedCache call() throws Exception
        {
        return Coherence.getInstance(coherenceName).getSession(sessionName).getCache(cacheName);
        }
    }
