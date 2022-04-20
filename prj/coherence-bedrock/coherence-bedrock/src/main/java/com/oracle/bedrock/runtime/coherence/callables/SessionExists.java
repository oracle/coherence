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
 * A {@link RemoteCallable} to determine whether a given {@link Session}
 * exists on a remote cluster member.
 */
public class SessionExists
        implements RemoteCallable<Boolean>
    {
    private final String coherenceName;

    private final String sessionName;

    public SessionExists()
        {
        this(Coherence.DEFAULT_NAME, Coherence.DEFAULT_NAME);
        }

    public SessionExists(String sessionName)
        {
        this(Coherence.DEFAULT_NAME, sessionName);
        }

    public SessionExists(String coherenceName, String sessionName)
        {
        this.coherenceName = coherenceName == null ? Coherence.DEFAULT_NAME : coherenceName;
        this.sessionName = sessionName == null ? Coherence.DEFAULT_NAME : sessionName;
        }

    @Override
    public Boolean call() throws Exception
        {
        return Coherence.getInstances()
                .stream()
                .filter(c -> c.getName().equals(coherenceName))
                .map(c -> c.hasSession(sessionName))
                .findFirst()
                .orElse(false);
        }
    }
