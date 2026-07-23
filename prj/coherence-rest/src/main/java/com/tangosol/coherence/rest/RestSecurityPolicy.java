/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.tangosol.util.CoherenceMode;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

/**
 * Central mode-aware policy for Coherence REST security boundaries.
 *
 * @author Vaso Putica  2026.05.07
 * @since 26.04
 */
public final class RestSecurityPolicy
    {
    private RestSecurityPolicy()
        {
        }

    /**
     * Return {@code true} if REST authentication is engaged for this request.
     *
     * @param context  the request security context
     *
     * @return {@code true} if authentication is engaged
     */
    public static boolean isAuthenticationEngaged(SecurityContext context)
        {
        return evaluateAuthentication(context).isEngaged();
        }

    /**
     * Return {@code true} if the request must have a usable principal.
     *
     * @param context  the request security context
     *
     * @return {@code true} if the request must have a usable principal
     */
    public static boolean isPrincipalRequired(SecurityContext context)
        {
        return evaluateAuthentication(context).isPrincipalRequired();
        }

    /**
     * Return {@code true} if the security context has a usable principal.
     *
     * @param context  the request security context
     *
     * @return {@code true} if the security context has a usable principal
     */
    public static boolean hasUsablePrincipal(SecurityContext context)
        {
        return evaluateAuthentication(context).hasUsablePrincipal();
        }

    /**
     * Safely evaluate the request authentication state.
     *
     * @param context  the request security context
     *
     * @return the evaluated authentication state
     */
    public static AuthenticationState evaluateAuthentication(SecurityContext context)
        {
        // centralize auth-state probing so every REST route interprets container auth failures the same way
        if (context == null)
            {
            return new AuthenticationState(null, null, null, false, false, false);
            }

        String    sScheme   = null;
        Principal principal = null;
        String    sName     = null;
        boolean   fFailure  = false;

        try
            {
            sScheme = context.getAuthenticationScheme();
            }
        catch (RuntimeException e)
            {
            // accessor failures mean auth is engaged but unusable
            fFailure = true;
            }

        if (!fFailure)
            {
            try
                {
                principal = context.getUserPrincipal();
                }
            catch (RuntimeException e)
                {
                // accessor failures mean auth is engaged but unusable
                fFailure = true;
                }
            }

        if (!fFailure && principal != null)
            {
            try
                {
                sName = principal.getName();
                }
            catch (RuntimeException e)
                {
                // accessor failures mean auth is engaged but unusable
                fFailure = true;
                }
            }

        boolean fUsable  = !fFailure && !isBlank(sName);
        boolean fEngaged = fFailure || sScheme != null || fUsable;
        return new AuthenticationState(sScheme, principal, sName, fEngaged, fUsable, fFailure);
        }

    /**
     * Return {@code true} if pass-through resources must be explicitly
     * configured.
     *
     * @return {@code true} if pass-through requires an explicit configuration
     */
    public static boolean isPassThroughAllowlistRequired()
        {
        return CoherenceMode.isCoherenceRestPassThroughAllowlistRequired();
        }

    /**
     * Return {@code true} if pass-through may auto-publish an unconfigured
     * cache name.
     *
     * @return {@code true} if pass-through may auto-publish a cache
     */
    public static boolean isPassThroughAutoPublishAllowed()
        {
        return !isPassThroughAllowlistRequired();
        }

    /**
     * Return {@code true} if a string is null, empty, or whitespace only.
     *
     * @param sValue  the value to test
     *
     * @return {@code true} if the value is blank
     */
    private static boolean isBlank(String sValue)
        {
        return sValue == null || sValue.trim().isEmpty();
        }

    // ---- inner class: AuthenticationState --------------------------------

    /**
     * Safely evaluated authentication state for a REST request.
     */
    public static final class AuthenticationState
        {
        /**
         * Construct an authentication state.
         *
         * @param sScheme     the authentication scheme
         * @param principal   the user principal
         * @param sName       the principal name
         * @param fEngaged    {@code true} if authentication is engaged
         * @param fUsable     {@code true} if the principal is usable
         * @param fFailure    {@code true} if a security accessor failed
         */
        private AuthenticationState(String sScheme, Principal principal, String sName,
                boolean fEngaged, boolean fUsable, boolean fFailure)
            {
            m_sScheme    = sScheme;
            m_principal  = principal;
            m_sName      = sName;
            m_fEngaged   = fEngaged;
            m_fUsable    = fUsable;
            m_fFailure   = fFailure;
            }

        /**
         * Return the authentication scheme.
         *
         * @return the authentication scheme
         */
        public String getAuthenticationScheme()
            {
            return m_sScheme;
            }

        /**
         * Return the user principal.
         *
         * @return the user principal
         */
        public Principal getPrincipal()
            {
            return m_principal;
            }

        /**
         * Return the principal name.
         *
         * @return the principal name
         */
        public String getPrincipalName()
            {
            return m_sName;
            }

        /**
         * Return {@code true} if authentication is engaged.
         *
         * @return {@code true} if authentication is engaged
         */
        public boolean isEngaged()
            {
            return m_fEngaged;
            }

        /**
         * Return {@code true} if the security context has a usable principal.
         *
         * @return {@code true} if the security context has a usable principal
         */
        public boolean hasUsablePrincipal()
            {
            return m_fUsable;
            }

        /**
         * Return {@code true} if security context access failed.
         *
         * @return {@code true} if security context access failed
         */
        public boolean hasAccessorFailure()
            {
            return m_fFailure;
            }

        /**
         * Return {@code true} if this request must have a usable principal.
         *
         * @return {@code true} if this request must have a usable principal
         */
        public boolean isPrincipalRequired()
            {
            // gates only engaged authentication, preserving anonymous resources that never opted into auth
            return CoherenceMode.isCoherenceRestAuthEnforced() && isEngaged();
            }

        private final String m_sScheme;

        private final Principal m_principal;

        private final String m_sName;

        private final boolean m_fEngaged;

        private final boolean m_fUsable;

        private final boolean m_fFailure;
        }
    }
