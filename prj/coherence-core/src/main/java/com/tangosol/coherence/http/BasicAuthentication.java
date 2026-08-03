/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http;

import java.nio.charset.StandardCharsets;

import java.util.Base64;

/**
 * HTTP Basic authentication parsing helpers.
 *
 * @author jk  2026.05.14
 * @since 26.07
 */
public final class BasicAuthentication
    {
    /**
     * Parse an {@code Authorization} header as HTTP Basic credentials.
     *
     * @param sAuthorization  the header value
     *
     * @return parsed credentials, or {@code null} for no Basic header
     *
     * @throws IllegalArgumentException if a Basic header is malformed
     */
    public static Credentials parse(String sAuthorization)
        {
        if (sAuthorization == null)
            {
            return null;
            }

        if (!sAuthorization.startsWith(BASIC_PREFIX))
            {
            return null;
            }

        String sEncoded = sAuthorization.substring(BASIC_PREFIX.length());
        String sDecoded;
        try
            {
            sDecoded = new String(Base64.getDecoder().decode(sEncoded.getBytes(StandardCharsets.US_ASCII)),
                                  StandardCharsets.US_ASCII);
            }
        catch (IllegalArgumentException e)
            {
            throw new IllegalArgumentException("Invalid HTTP Basic credentials", e);
            }

        int nColon = sDecoded.indexOf(':');
        if (nColon < 0 || nColon == 0)
            {
            throw new IllegalArgumentException("Invalid HTTP Basic credentials");
            }

        return new Credentials(sDecoded.substring(0, nColon), sDecoded.substring(nColon + 1));
        }

    // ----- inner class: Credentials --------------------------------------

    /**
     * Parsed HTTP Basic credentials.
     */
    public static class Credentials
        {
        /**
         * Construct {@link Credentials}.
         *
         * @param sUsername  the username
         * @param sPassword  the password
         */
        Credentials(String sUsername, String sPassword)
            {
            f_sUsername = sUsername;
            f_sPassword = sPassword;
            }

        /**
         * Return the username.
         *
         * @return the username
         */
        public String getUsername()
            {
            return f_sUsername;
            }

        /**
         * Return the password.
         *
         * @return the password
         */
        public String getPassword()
            {
            return f_sPassword;
            }

        /**
         * The username.
         */
        private final String f_sUsername;

        /**
         * The password.
         */
        private final String f_sPassword;
        }

    // ----- constants ------------------------------------------------------

    /**
     * HTTP Basic authorization header prefix.
     */
    public static final String BASIC_PREFIX = "Basic ";
    }
