/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;
import com.tangosol.net.OperationalContext;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import java.io.IOException;

/**
 * A test {@link CallbackHandler} that allows a username and password
 * to be specified from system properties.
 *
 * @author Jonathan Knight 2025.04.11
 */
public class TestCallBackHandler
        implements CallbackHandler
    {
    /**
     * Create a default {@link TestCallBackHandler}.
     */
    public TestCallBackHandler()
        {
        m_sAlias = Config.getProperty("coherence.security.keystore.alias");
        m_acCredentials = Config.getProperty("coherence.security.keystore.password", "").toCharArray();
        }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
        {
        for (Callback callback : callbacks)
            {
            if (callback instanceof NameCallback)
                {
                NameCallback nameCallback = (NameCallback) callback;
                if (m_sAlias == null)
                    {
                    // Alias not set so use the Coherence member name
                    OperationalContext ctx    = (OperationalContext) CacheFactory.getCluster();
                    Member             member = ctx.getLocalMember();
                    m_sAlias = member.getMemberName();
                    }
                nameCallback.setName(m_sAlias);
                }
            else if (callback instanceof PasswordCallback)
                {
                ((PasswordCallback) callback).setPassword(m_acCredentials);
                }
            else
                {
                Logger.err("Callback Type Not Supported: " + callback.getClass());
                throw new UnsupportedCallbackException(callback);
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The alias name to pass to a {@link NameCallback}.
     */
    private String m_sAlias;

    /**
     * The credentials to pass to a {@link PasswordCallback}.
     */
    private final char[] m_acCredentials;
    }
