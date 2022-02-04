/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Base64;
import java.util.Optional;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * Error helper methods.
 *
 * @author Jonathan Knight  2019.11.28
 * @since 20.06
 */
public final class ErrorsHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Utility class; construction not allowed.
     */
    private ErrorsHelper()
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Convert a {@link java.lang.Throwable} to a {@link io.grpc.StatusRuntimeException}.
     *
     * @param t  the {@link java.lang.Throwable} to convert
     *
     * @return a {@link io.grpc.StatusRuntimeException}
     */
    public static StatusRuntimeException ensureStatusRuntimeException(Throwable t)
        {
        if (t instanceof StatusRuntimeException)
            {
            return enrich((StatusRuntimeException) t);
            }
        else if (t instanceof StatusException)
            {
            return ((StatusException) t).getStatus()
                    .asRuntimeException(getErrorMetadata(t));
            }
        else
            {
            return Status.INTERNAL.withCause(t)
                    .withDescription(t.getMessage())
                    .asRuntimeException(getErrorMetadata(t));
            }
        }

    /**
     * Convert a {@link java.lang.Throwable} to a {@link io.grpc.StatusRuntimeException}.
     *
     * @param t            the {@link java.lang.Throwable} to convert
     * @param description  the description to add to the exception
     *
     * @return a {@link io.grpc.StatusRuntimeException}
     */
    public static StatusRuntimeException ensureStatusRuntimeException(Throwable t, String description)
        {
        Status status;
        if (t instanceof StatusRuntimeException)
            {
            status = ((StatusRuntimeException) t).getStatus().getCode().toStatus();
            }
        else if (t instanceof StatusException)
            {
            status = ((StatusException) t).getStatus().getCode().toStatus();
            }
        else
            {
            status = Status.INTERNAL;
            }
        return status.withCause(t)
                .withDescription(description)
                .asRuntimeException(getErrorMetadata(t));
        }

    /**
     * Obtain the remote stack trace from the specified {@link StatusRuntimeException}.
     *
     * @param e  the exception returned from a server call
     *
     * @return  the remote stack trace, or an empty {@link Optional} if no remote
     *          stack is present.
     */
    public static Optional<String> getRemoteStack(StatusRuntimeException e)
        {
        Metadata trailers = e.getTrailers();
        String s = trailers == null ? null : trailers.get(ErrorsHelper.KEY_ERROR);
        return s == null ? Optional.empty() : Optional.of(new String(s_decoder.decode(s)));
        }

    // ----- helper methods -------------------------------------------------

    private static StatusRuntimeException enrich(StatusRuntimeException e)
        {
        Metadata metadata = e.getTrailers();
        if (metadata == null)
            {
            metadata = new Metadata();
            e = e.getStatus().asRuntimeException(metadata);
            }
        metadata.put(KEY_ERROR, getStackTrace(e));
        return e;
        }

    private static Metadata getErrorMetadata(Throwable t)
        {
        Metadata metadata = new Metadata();
        metadata.put(KEY_ERROR, getStackTrace(t));
        return metadata;
        }

    private static String getStackTrace(Throwable t)
        {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(out));
        return new String(s_encoder.encode(out.toByteArray()));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The metadata key for an exception stack trace.
     */
    public static final Metadata.Key<String> KEY_ERROR = Metadata.Key.of("coherence-error-stack", ASCII_STRING_MARSHALLER);

    private static final Base64.Encoder s_encoder = Base64.getEncoder();

    private static final Base64.Decoder s_decoder = Base64.getDecoder();
    }
