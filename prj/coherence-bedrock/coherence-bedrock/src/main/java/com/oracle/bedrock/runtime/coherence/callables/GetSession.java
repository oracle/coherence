/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

/**
 * A {@link RemoteCallable} to obtain a specific {@link Session},
 */
public class GetSession
        implements RemoteCallable<Session>
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
     * Create a {@link GetSession} to obtain the default {@link Session}.
     */
    public GetSession()
        {
        this(Coherence.DEFAULT_NAME, Coherence.DEFAULT_NAME);
        }

    /**
     * Create a {@link GetSession} to obtain the named {@link Session}
     * from the default {@link Coherence} instance.
     *
     * @param sessionName the name of the {@link Session}
     */
    public GetSession(String sessionName)
        {
        this(Coherence.DEFAULT_NAME, sessionName);
        }

    /**
     * Create a {@link GetSession} to obtain the named {@link Session}
     * from the named {@link Coherence} instance.
     *
     * @param coherenceName the name of the {@link Coherence} instance
     * @param sessionName   the name of the {@link Session}
     */
    public GetSession(String coherenceName, String sessionName)
        {
        this.coherenceName = coherenceName == null ? Coherence.DEFAULT_NAME : coherenceName;
        this.sessionName = sessionName == null ? Coherence.DEFAULT_NAME : sessionName;
        }

    @Override
    public Session call() throws Exception
        {
        return Coherence.getInstance(coherenceName).getSession(sessionName);
        }
    }
