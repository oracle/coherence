/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package health;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import java.security.Principal;

import java.util.Map;

/**
 * Test login module for health HTTP mutator Basic authentication.
 *
 * @author jk  2026.05.14
 * @since 26.04
 */
public class HealthMutatorLoginModule
        implements LoginModule
    {
    @Override
    public void initialize(Subject subject, CallbackHandler handler, Map<String, ?> sharedState, Map<String, ?> options)
        {
        m_subject = subject;
        m_handler = handler;
        }

    @Override
    public boolean login()
            throws LoginException
        {
        NameCallback     name     = new NameCallback("username");
        PasswordCallback password = new PasswordCallback("password", false);
        try
            {
            m_handler.handle(new Callback[] {name, password});
            }
        catch (Exception e)
            {
            throw (LoginException) new LoginException("Unable to read credentials").initCause(e);
            }

        String sPassword = new String(password.getPassword());
        m_fSucceeded = "client".equals(name.getName()) && "pass:word".equals(sPassword);
        if (!m_fSucceeded)
            {
            throw new LoginException("Invalid credentials");
            }
        return true;
        }

    @Override
    public boolean commit()
        {
        if (m_fSucceeded)
            {
            m_subject.getPrincipals().add(PRINCIPAL);
            }
        return m_fSucceeded;
        }

    @Override
    public boolean abort()
        {
        m_fSucceeded = false;
        return true;
        }

    @Override
    public boolean logout()
        {
        m_subject.getPrincipals().remove(PRINCIPAL);
        m_fSucceeded = false;
        return true;
        }

    private Subject         m_subject;
    private CallbackHandler m_handler;
    private boolean         m_fSucceeded;

    private static final Principal PRINCIPAL = () -> "client";
    }
