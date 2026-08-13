/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import java.io.IOException;

/**
 * Test support for package-private RAG security policy hooks.
 *
 * @author Aleks Seovic  2026.04.28
 * @since 26.04
 */
public final class RagSecurityTestSupport
    {
    private RagSecurityTestSupport()
        {
        }

    /**
     * Override the DNS resolver used by HTTP(S) import policy.
     *
     * @param resolver  the resolver to use, or {@code null} to reset
     */
    public static void setAddressResolver(AddressResolver resolver)
        {
        RagSecurity.setAddressResolverForTesting(resolver == null ? null : resolver::resolve);
        }

    /**
     * Override the connection factory used by HTTP(S) document loading.
     *
     * @param factory  the factory to use, or {@code null} to reset
     */
    public static void setHttpConnectionFactory(HttpConnectionFactory factory)
        {
        RagSecurity.setHttpConnectionFactoryForTesting(factory == null ? null : factory::open);
        }

    // ---- inner interface: AddressResolver -------------------------------

    /**
     * Public test-facing resolver facade.
     */
    public interface AddressResolver
        {
        /**
         * Resolve the specified host name.
         *
         * @param host  the host name
         *
         * @return the resolved addresses
         *
         * @throws UnknownHostException if the host cannot be resolved
         */
        InetAddress[] resolve(String host) throws UnknownHostException;
        }

    // ---- inner interface: HttpConnectionFactory -------------------------

    /**
     * Public test-facing connection factory facade.
     */
    public interface HttpConnectionFactory
        {
        /**
         * Open a test HTTP connection.
         *
         * @param uri  the URI to open
         *
         * @return the HTTP connection
         *
         * @throws IOException if the connection cannot be opened
         */
        HttpURLConnection open(URI uri) throws IOException;
        }
    }
