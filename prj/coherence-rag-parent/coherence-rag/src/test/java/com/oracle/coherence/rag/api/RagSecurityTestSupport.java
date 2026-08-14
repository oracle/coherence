/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;

import javax.net.ssl.SSLContext;

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

    /**
     * Override the direct-route policy used by HTTP(S) imports.
     *
     * @param policy  the policy to use, or {@code null} to reset
     */
    public static void setDirectConnectionPolicy(DirectConnectionPolicy policy)
        {
        RagSecurity.setDirectConnectionPolicyForTesting(policy == null ? null : policy::isDirect);
        }

    /**
     * Open the production pinned HTTP connection for a focused transport test.
     *
     * @param uri                    the logical destination URI
     * @param address                the validated address
     * @param cMillisConnectTimeout  the connection timeout
     * @param cMillisReadTimeout     the read timeout
     *
     * @return the opened connection
     *
     * @throws IOException if the connection cannot be opened
     */
    public static HttpURLConnection openPinnedHttpConnection(URI uri, InetAddress address,
            int cMillisConnectTimeout, int cMillisReadTimeout)
            throws IOException
        {
        return RagSecurity.openPinnedHttpConnection(uri, address,
                cMillisConnectTimeout, cMillisReadTimeout);
        }

    /**
     * Open the production pinned HTTPS connection with a test trust context.
     *
     * @param uri                    the logical destination URI
     * @param address                the validated address
     * @param cMillisConnectTimeout  the connection timeout
     * @param cMillisReadTimeout     the read timeout
     * @param sslContext             the test SSL context
     *
     * @return the opened connection
     *
     * @throws IOException if the connection cannot be opened
     */
    public static HttpURLConnection openPinnedHttpsConnection(URI uri, InetAddress address,
            int cMillisConnectTimeout, int cMillisReadTimeout, SSLContext sslContext)
            throws IOException
        {
        return RagSecurity.openPinnedHttpsConnectionForTesting(uri, address,
                cMillisConnectTimeout, cMillisReadTimeout, sslContext);
        }

    /**
     * Override the hook invoked after file validation and before secure open.
     *
     * @param hook  the hook to use, or {@code null} to reset
     */
    public static void setFileOpenHook(FileOpenHook hook)
        {
        RagSecurity.setFileOpenHookForTesting(hook == null ? null : hook::beforeOpen);
        }

    /**
     * Override the directory-stream factory used by secure file open.
     *
     * @param factory  the factory to use, or {@code null} to reset
     */
    public static void setDirectoryStreamFactory(DirectoryStreamFactory factory)
        {
        RagSecurity.setDirectoryStreamFactoryForTesting(factory == null ? null : factory::open);
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
         * @param uri                    the URI to open
         * @param address                the validated address to connect to
         * @param cMillisConnectTimeout  the connection timeout
         * @param cMillisReadTimeout     the read timeout
         *
         * @return the HTTP connection
         *
         * @throws IOException if the connection cannot be opened
         */
        HttpURLConnection open(URI uri, InetAddress address,
                int cMillisConnectTimeout, int cMillisReadTimeout) throws IOException;
        }

    // ---- inner interface: DirectConnectionPolicy -----------------------

    /**
     * Public test-facing direct-route policy facade.
     */
    public interface DirectConnectionPolicy
        {
        /**
         * Return whether the specified URI will use a direct route.
         *
         * @param uri  the URI to inspect
         *
         * @return {@code true} only for a direct route
         */
        boolean isDirect(URI uri);
        }

    // ---- inner interface: FileOpenHook ----------------------------------

    /**
     * Public test-facing file pre-open hook.
     */
    public interface FileOpenHook
        {
        /**
         * Run before secure traversal opens the file.
         *
         * @param path  the validated canonical path
         *
         * @throws IOException if the hook cannot complete
         */
        void beforeOpen(Path path) throws IOException;
        }

    // ---- inner interface: DirectoryStreamFactory -----------------------

    /**
     * Public test-facing directory-stream factory.
     */
    public interface DirectoryStreamFactory
        {
        /**
         * Open a test directory stream.
         *
         * @param path  the path to open
         *
         * @return the opened stream
         *
         * @throws IOException if the stream cannot be opened
         */
        DirectoryStream<Path> open(Path path) throws IOException;
        }
    }
