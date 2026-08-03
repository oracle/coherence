/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.io.SerializationRole;

import com.tangosol.io.internal.SerializationTelemetry;

import io.grpc.Status;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Policy for gRPC client-selected serializer formats.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.04
 */
public final class GrpcSerializerPolicy
    {
    /**
     * Validate a serializer format supplied by a remote gRPC client.
     *
     * @param sFormat  the serializer format
     *
     * @throws io.grpc.StatusRuntimeException if the format is not allowed in the current mode
     */
    public static void validateClientFormat(String sFormat)
        {
        String        sName     = normalize(sFormat);
        boolean       fDefault  = sName.isEmpty();
        boolean       fAllowed  = fDefault || DEFAULT_ALLOWED_FORMATS.contains(sName) || allowlist().contains(sName);
        boolean       fUnsafe   = isUnsafe(sName);
        String        sReason   = fAllowed ? REASON_ALLOWED : fUnsafe ? REASON_UNSAFE : REASON_NOT_ALLOWED;

        try (SerializationRole.Scope ignored = SerializationRole.setAndClose(SerializationRole.GRPC))
            {
            if (CoherenceMode.isLegacy())
                {
                SerializationTelemetry.recordSerializerCheck(fAllowed ? RESULT_ALLOWED : RESULT_WOULD_REJECT, sReason);
                return;
                }

            if (fAllowed)
                {
                SerializationTelemetry.recordSerializerCheck(RESULT_ALLOWED, sReason);
                return;
                }

            SerializationTelemetry.recordSerializerCheck(RESULT_REJECTED, sReason);
            throw Status.INVALID_ARGUMENT
                    .withDescription("invalid request format, client-selected serializer is not allowed")
                    .asRuntimeException();
            }
        }

    /**
     * Return the normalized serializer format name.
     *
     * @param sFormat  the serializer format
     *
     * @return the normalized format
     */
    static String normalize(String sFormat)
        {
        return sFormat == null ? "" : sFormat.trim().toLowerCase(Locale.ROOT);
        }

    /**
     * Return {@code true} if the format is a known unsafe compatibility format.
     *
     * @param sFormat  the normalized serializer format
     *
     * @return {@code true} if the format is unsafe
     */
    static boolean isUnsafe(String sFormat)
        {
        return UNSAFE_FORMATS.contains(sFormat);
        }

    private static Set<String> allowlist()
        {
        String sAllowed = Config.getProperty(PROP_ALLOWED_SERIALIZERS);
        if (sAllowed == null || sAllowed.isBlank())
            {
            return Set.of();
            }

        return Arrays.stream(sAllowed.split("[,;\\s]+"))
                .map(GrpcSerializerPolicy::normalize)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        }

    /**
     * Property containing additional gRPC client-selectable serializer formats.
     */
    public static final String PROP_ALLOWED_SERIALIZERS = "coherence.grpc.serializer.allowlist";

    /**
     * Telemetry result for allowed formats.
     */
    public static final String RESULT_ALLOWED = "allowed";

    /**
     * Telemetry result for rejected formats.
     */
    public static final String RESULT_REJECTED = "rejected";

    /**
     * Telemetry result for LEGACY shadow rejection.
     */
    public static final String RESULT_WOULD_REJECT = "would_reject";

    /**
     * Reason for explicitly allowed formats.
     */
    public static final String REASON_ALLOWED = "client-serializer-allowed";

    /**
     * Reason for known unsafe formats.
     */
    public static final String REASON_UNSAFE = "client-serializer-unsafe";

    /**
     * Reason for unapproved formats.
     */
    public static final String REASON_NOT_ALLOWED = "client-serializer-not-allowed";

    private static final Set<String> DEFAULT_ALLOWED_FORMATS = Set.of(
            "pof",
            "json",
            "genson",
            "com.oracle.coherence.io.json.jsonserializer");

    private static final Set<String> UNSAFE_FORMATS = Set.of(
            "java",
            "defaultserializer",
            "com.tangosol.io.defaultserializer");

    private GrpcSerializerPolicy()
        {
        }
    }
