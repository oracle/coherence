/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

/**
 * Error helper methods.
 *
 * @author Jonathan Knight  2019.11.28
 * @since 14.1.2
 */
final class ErrorsHelper
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
    static StatusRuntimeException ensureStatusRuntimeException(Throwable t)
        {
        if (t instanceof StatusRuntimeException)
            {
            return (StatusRuntimeException) t;
            }
        else if (t instanceof StatusException)
            {
            return ((StatusException) t).getStatus().asRuntimeException();
            }
        else
            {
            return Status.INTERNAL.withCause(t).withDescription(t.getMessage()).asRuntimeException();
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
    static StatusRuntimeException ensureStatusRuntimeException(Throwable t, String description)
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
        return status.withCause(t).withDescription(description).asRuntimeException();
        }
    }
