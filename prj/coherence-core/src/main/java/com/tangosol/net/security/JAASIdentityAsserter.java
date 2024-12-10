/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.security;

import com.tangosol.net.Service;

import javax.security.auth.Subject;

import javax.security.auth.callback.CallbackHandler;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * JAAS-based implementation of {@link IdentityAsserter}.
 *
 * @author as  2011.12.23
 */
public class JAASIdentityAsserter
        implements IdentityAsserter
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a JAASIdentityAsserter instance.
     *
     * @param sConfigName  the name used as the index into the Configuration
     */
    public JAASIdentityAsserter(String sConfigName)
        {
        m_sConfigName = sConfigName;
        }

    // ----- IdentityAsserter implementation --------------------------------

    /**
    * {@inheritDoc}
    */
    public Subject assertIdentity(Object oToken, Service service)
            throws SecurityException
        {
        if (oToken instanceof UsernameAndPassword)
            {
            UsernameAndPassword token   = (UsernameAndPassword) oToken;
            CallbackHandler     handler = new SimpleHandler(token.getUsername(),
                    token.getPassword());

            try
                {
                LoginContext ctx = new LoginContext(m_sConfigName, handler);
                ctx.login();
                return ctx.getSubject();
                }
            catch (LoginException e)
                {
                throw new SecurityException(e);
                }
            }

        throw new SecurityException("identity token is unsupported type");
        }

    // ----- data members ---------------------------------------------------

    /**
     * JAAS configuration name.
     */
    private String m_sConfigName;
    }
