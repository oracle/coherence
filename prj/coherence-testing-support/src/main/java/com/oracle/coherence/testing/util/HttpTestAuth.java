/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

/**
 * Shared credentials for positive HTTP management and metrics test fixtures.
 * Servers must explicitly select {@link #loginConfig()}; negative authentication
 * tests retain their own server and client configuration.
 *
 * @author phf  2026.09.30
 * @since 26.10
 */
public final class HttpTestAuth
    {
    private HttpTestAuth()
        {
        }

    /**
     * Return the JAAS configuration URL to pass to each test server.
     *
     * @return the test configuration URL
     */
    public static String loginConfig()
        {
        return HttpTestAuth.class.getResource("/test-http-auth.login").toExternalForm();
        }

    /**
     * Return the Basic authentication header for this fixture.
     *
     * @return the authorization header
     */
    public static String authorization()
        {
        return "Basic " + Base64.getEncoder().encodeToString(
                (USER + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
        }

    /**
     * Authenticate a test URL connection.
     *
     * @param connection  the connection
     */
    public static void authenticate(HttpURLConnection connection)
        {
        connection.setRequestProperty("Authorization", authorization());
        }

    /**
     * Register on positive JAX-RS clients to authenticate every request.
     */
    public static class RequestFilter
            implements ClientRequestFilter
        {
        @Override
        public void filter(ClientRequestContext context)
            {
            context.getHeaders().putSingle("Authorization", authorization());
            }
        }

    /**
     * Test-only JAAS login module for management and metrics endpoints.
     */
    public static class TestLoginModule
            implements LoginModule
        {
        @Override
        public void initialize(Subject subject, CallbackHandler handler,
                               Map<String, ?> sharedState, Map<String, ?> options)
            {
            m_subject = subject;
            m_handler = handler;
            }

        @Override
        public boolean login() throws LoginException
            {
            NameCallback name = new NameCallback("username");
            PasswordCallback password = new PasswordCallback("password", false);
            char[] achPassword = null;
            try
                {
                m_handler.handle(new Callback[] {name, password});
                achPassword = password.getPassword();
                m_fSucceeded = USER.equals(name.getName())
                        && Arrays.equals(PASSWORD.toCharArray(), achPassword);
                if (!m_fSucceeded)
                    {
                    throw new LoginException("Invalid test credentials");
                    }
                return true;
                }
            catch (LoginException e)
                {
                throw e;
                }
            catch (Exception e)
                {
                throw (LoginException) new LoginException("Unable to read test credentials").initCause(e);
                }
            finally
                {
                password.clearPassword();
                if (achPassword != null)
                    {
                    Arrays.fill(achPassword, '\0');
                    }
                }
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
            return logout();
            }

        @Override
        public boolean logout()
            {
            m_subject.getPrincipals().remove(PRINCIPAL);
            m_fSucceeded = false;
            return true;
            }

        private Subject m_subject;
        private CallbackHandler m_handler;
        private boolean m_fSucceeded;
        }

    private static final String USER = "test-http-user";
    private static final String PASSWORD = "test-http-password";
    private static final Principal PRINCIPAL = () -> USER;
    }
