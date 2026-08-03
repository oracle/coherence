/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ErrorsHelper}.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
class ErrorsHelperTest
    {
    @Test
    void shouldPreserveStatusWrappedByCompletionException()
        {
        StatusRuntimeException rejected = Status.INVALID_ARGUMENT
                .withDescription("invalid request format")
                .asRuntimeException();

        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeException(new CompletionException(rejected));

        assertEquals(Status.INVALID_ARGUMENT.getCode(), result.getStatus().getCode());
        assertEquals("invalid request format", result.getStatus().getDescription());
        }

    @Test
    void shouldPreserveStatusWrappedByInternalStatus()
        {
        StatusRuntimeException rejected = Status.INVALID_ARGUMENT
                .withDescription("invalid request format")
                .asRuntimeException();
        StatusRuntimeException wrapper = Status.INTERNAL
                .withCause(rejected)
                .asRuntimeException();

        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeException(wrapper);

        assertEquals(Status.INVALID_ARGUMENT.getCode(), result.getStatus().getCode());
        assertEquals("invalid request format", result.getStatus().getDescription());
        }
    }
