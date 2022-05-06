/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

/**
 * A {@link RemoteCallable} to obtain a specific {@link Session}.
 *
 * @author Jonathan Knight
 * @since 22.06
 */
public class GetSession
        implements RemoteCallable<Session>
    {
    /**
     * Create a {@link GetSession} to obtain the default {@link Session}
     * from the default {@link Coherence} instance.
     */
    public GetSession()
        {
        this(Coherence.DEFAULT_NAME, Coherence.DEFAULT_NAME);
        }

    /**
     * Create a {@link GetSession} to obtain the named {@link Session}
     * from the default {@link Coherence} instance.
     *
     * @param sSessionName  the name of the {@link Session}
     */
    public GetSession(String sSessionName)
        {
        this(Coherence.DEFAULT_NAME, sSessionName);
        }

    /**
     * Create a {@link GetSession} to obtain the named {@link Session}
     * from the named {@link Coherence} instance.
     *
     * @param sCoherenceName the name of the {@link Coherence} instance
     * @param sSessionName   the name of the {@link Session}
     */
    public GetSession(String sCoherenceName, String sSessionName)
        {
        m_sCoherenceName = sCoherenceName == null || sCoherenceName.isBlank() ? Coherence.DEFAULT_NAME : sCoherenceName;
        m_sSessionName   = sSessionName == null || sSessionName.isBlank() ? Coherence.DEFAULT_NAME : sSessionName;
        }

    @Override
    public Session call() throws Exception
        {
        Coherence coherence = Coherence.getInstance(m_sCoherenceName);
        if (coherence == null)
            {
            Logger.err("In Bedrock GetSession: No Coherence instance exists with name \"" + m_sCoherenceName + "\"");
            return null;
            }

        Session session = coherence.getSession(m_sSessionName);
        if (session == null)
            {
            Logger.err("In Bedrock GetSession: No Session instance exists with name \"" + m_sSessionName
                               + "\" in Coherence instance \"" + m_sCoherenceName + "\"");
            return null;
            }
        return session;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the {@link Coherence} instance.
     */
    private final String m_sCoherenceName;

    /**
     * The name of the {@link Session} instance.
     */
    private final String m_sSessionName;
    }
