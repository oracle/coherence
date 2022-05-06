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
 * A {@link RemoteCallable} to determine whether a given {@link Session}
 * exists on a remote cluster member.
 *
 * @author Jonathan Knight
 * @since 22.06
 */
public class SessionExists
        implements RemoteCallable<Boolean>
    {
    /**
     * Create a {@link SessionExists} to check whether the default {@link Session}
     * exists in the default {@link Coherence} instance.
     */
    public SessionExists()
        {
        this(Coherence.DEFAULT_NAME, Coherence.DEFAULT_NAME);
        }

    /**
     * Create a {@link SessionExists} to check whether the named {@link Session}
     * exists in the default {@link Coherence} instance.
     *
     * @param sSessionName  the name of the {@link Session}
     */
    public SessionExists(String sSessionName)
        {
        this(Coherence.DEFAULT_NAME, sSessionName);
        }

    /**
     * Create a {@link SessionExists} to check whether the named {@link Session}
     * exists in the named {@link Coherence} instance.
     *
     * @param sCoherenceName  the name of the {@link Coherence} instance
     * @param sSessionName    the name of the {@link Session}
     */
    public SessionExists(String sCoherenceName, String sSessionName)
        {
        m_sCoherenceName = sCoherenceName == null || sCoherenceName.isBlank() ? Coherence.DEFAULT_NAME : sCoherenceName;
        m_sSessionName   = sSessionName == null || sSessionName.isBlank() ? Coherence.DEFAULT_NAME : sSessionName;
        }

    @Override
    public Boolean call() throws Exception
        {
        Coherence coherence = Coherence.getInstance(m_sCoherenceName);
        if (coherence == null)
            {
            Logger.err("In Bedrock SessionExists: No Coherence instance exists with name \"" + m_sCoherenceName + "\"");
            return false;
            }

        Session session = coherence.getSession(m_sSessionName);
        if (session == null)
            {
            Logger.err("In Bedrock SessionExists: No Session instance exists with name \"" + m_sSessionName
                               + "\" in Coherence instance \"" + m_sCoherenceName + "\"");
            return false;
            }
        return true;
        }

    // ----- data members ---------------------------------------------------

    private final String m_sCoherenceName;

    private final String m_sSessionName;
    }
