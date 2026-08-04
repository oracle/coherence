/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.http.BasicAuthentication;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.net.security.UsernameAndPassword;

import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.net.SocketAddress;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import javax.security.auth.Subject;

/**
 * Shared gRPC authentication helpers.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
public final class GrpcAuthentication
    {
    /**
     * Return the normalized gRPC auth method.
     *
     * @param sMethod  the configured auth method
     *
     * @return the normalized auth method
     */
    public static String normalizeAuthMethod(String sMethod)
        {
        return sMethod == null || sMethod.isBlank()
                ? AUTH_METHOD_NONE
                : sMethod.trim().toLowerCase(Locale.ROOT);
        }

    /**
     * Return {@code true} if the configured auth method requires gRPC auth.
     *
     * @param sMethod  the configured auth method
     *
     * @return {@code true} if auth is required
     */
    public static boolean isAuthRequired(String sMethod)
        {
        return AUTH_METHOD_BASIC.equals(normalizeAuthMethod(sMethod));
        }

    /**
     * Validate the configured gRPC auth method.
     *
     * @param sMethod  the configured auth method
     *
     * @throws IllegalArgumentException if the method is unsupported
     */
    public static void validateAuthMethod(String sMethod)
        {
        String sNormalized = normalizeAuthMethod(sMethod);
        if (!AUTH_METHOD_NONE.equals(sNormalized) && !AUTH_METHOD_BASIC.equals(sNormalized))
            {
            throw new IllegalArgumentException("unsupported GrpcAuthMethod: " + sMethod);
            }
        }

    /**
     * Parse Basic credentials from gRPC metadata.
     *
     * @param headers  the gRPC metadata
     *
     * @return the parsed credentials
     *
     * @throws io.grpc.StatusRuntimeException if credentials are missing or malformed
     */
    public static UsernameAndPassword parseBasicCredentials(Metadata headers)
        {
        try
            {
            BasicAuthentication.Credentials credentials =
                    BasicAuthentication.parse(headers == null ? null : headers.get(KEY_AUTHORIZATION));

            if (credentials == null
                    || credentials.getUsername() == null
                    || credentials.getUsername().isBlank()
                    || credentials.getPassword() == null
                    || credentials.getPassword().isEmpty())
                {
                throw unauthenticated();
                }

            return new UsernameAndPassword(credentials.getUsername(), credentials.getPassword());
            }
        catch (IllegalArgumentException e)
            {
            throw unauthenticated();
            }
        }

    /**
     * Assert a subject from parsed Basic credentials.
     *
     * @param token     the parsed credentials
     * @param asserter  the assertion function
     *
     * @return the asserted subject
     *
     * @throws io.grpc.StatusRuntimeException if assertion fails
     */
    public static Subject assertSubject(UsernameAndPassword token, Function<Object, Subject> asserter)
        {
        try
            {
            Subject subject = asserter.apply(token);
            if (subject == null)
                {
                throw unauthenticated();
                }
            return subject;
            }
        catch (SecurityException e)
            {
            throw unauthenticated();
            }
        }

    /**
     * Enforce the Basic-over-cleartext mode policy.
     *
     * @param attributes  the gRPC transport attributes
     */
    public static void validateBasicTransport(Attributes attributes)
        {
        if (isSecure(attributes))
            {
            return;
            }

        if (CoherenceMode.isSecurityHardeningEnabled())
            {
            throw unauthenticated();
            }

        if (!CoherenceMode.isSecurityHardeningEnabled())
            {
            if (s_fLoggedCompatibilityCleartext.compareAndSet(false, true))
                {
                Logger.warn("gRPC Basic authentication would_reject; security-mode=compatibility; reason=cleartext-basic");
                }
            }
        }

    /**
     * Return {@code true} if the gRPC transport attributes indicate TLS or a
     * non-network in-process call.
     *
     * @param attributes  the transport attributes
     *
     * @return {@code true} if the call is secure enough for Basic credentials
     */
    static boolean isSecure(Attributes attributes)
        {
        if (attributes == null)
            {
            return false;
            }

        SocketAddress address = attributes.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        return attributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION) != null || address == null;
        }

    private static StatusRuntimeException unauthenticated()
        {
        return Status.UNAUTHENTICATED
                .withDescription("invalid authentication credentials")
                .asRuntimeException();
        }

    // ----- constants ------------------------------------------------------

    /**
     * gRPC auth method for anonymous compatibility.
     */
    public static final String AUTH_METHOD_NONE = "none";

    /**
     * gRPC Basic auth method.
     */
    public static final String AUTH_METHOD_BASIC = "basic";

    /**
     * Metadata key for the Authorization header.
     */
    public static final Metadata.Key<String> KEY_AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * System property overriding the configured gRPC auth method.
     */
    public static final String PROP_GRPC_AUTH_METHOD = "coherence.grpc.auth";

    private static final AtomicBoolean s_fLoggedCompatibilityCleartext = new AtomicBoolean();

    private GrpcAuthentication()
        {
        }
    }
