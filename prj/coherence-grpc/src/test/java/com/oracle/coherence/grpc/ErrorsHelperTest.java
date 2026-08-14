/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.oracle.coherence.grpc.messages.common.v1.ErrorMessage;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.grpc.GrpcDiagnosticsPolicy;
import com.tangosol.util.Converter;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void shouldPreserveStatusExceptionWrappedByCompletionException()
        {
        StatusException rejected = Status.FAILED_PRECONDITION
                .withDescription("operation precondition failed")
                .asException();

        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeException(new CompletionException(rejected));

        assertEquals(Status.FAILED_PRECONDITION.getCode(), result.getStatus().getCode());
        assertEquals("operation precondition failed", result.getStatus().getDescription());
        }

    @Test
    void shouldMapSecurityExceptionToPermissionDeniedInDiagnosticMode()
        {
        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeException(
                new SecurityException("remote executable denied"));

        assertEquals(Status.PERMISSION_DENIED.getCode(), result.getStatus().getCode());
        assertEquals("remote executable denied", result.getStatus().getDescription());
        assertTrue(ErrorsHelper.getRemoteStack(result).orElse("").contains("SecurityException"));
        }

    @Test
    void shouldMapDescribedSecurityExceptionToPermissionDeniedInDiagnosticMode()
        {
        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeException(
                new SecurityException("remote executable denied"), "cache operation denied");

        assertEquals(Status.PERMISSION_DENIED.getCode(), result.getStatus().getCode());
        assertEquals("cache operation denied", result.getStatus().getDescription());
        assertTrue(ErrorsHelper.getRemoteStack(result).orElse("").contains("SecurityException"));
        }

    @Test
    void shouldMapWrappedSecurityExceptionToPermissionDeniedInDiagnosticMode()
        {
        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeException(
                new CompletionException(new SecurityException("remote executable denied")));

        assertEquals(Status.PERMISSION_DENIED.getCode(), result.getStatus().getCode());
        assertTrue(result.getStatus().getDescription().contains("remote executable denied"));
        assertTrue(ErrorsHelper.getRemoteStack(result).orElse("").contains("SecurityException"));
        }

    @Test
    void shouldSanitizeSecurityExceptionInSafeMode()
        {
        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeExceptionWithPolicy(
                new SecurityException("secret policy detail"), GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);

        assertEquals(Status.PERMISSION_DENIED.getCode(), result.getStatus().getCode());
        assertEquals(ErrorsHelper.SAFE_INTERNAL_ERROR_MESSAGE, result.getStatus().getDescription());
        assertFalse(result.getStatus().getDescription().contains("secret policy detail"));
        assertTrue(ErrorsHelper.getRemoteStack(result).isEmpty());
        }

    @Test
    void shouldSanitizeDescribedSecurityExceptionInSafeMode()
        {
        String sDescription = "bounded safe description ".repeat(50);

        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeException(
                new SecurityException("secret policy detail"), sDescription,
                GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);

        assertEquals(Status.PERMISSION_DENIED.getCode(), result.getStatus().getCode());
        assertEquals(sDescription.substring(0, 1000), result.getStatus().getDescription());
        assertFalse(result.getStatus().getDescription().contains("secret policy detail"));
        assertTrue(ErrorsHelper.getRemoteStack(result).isEmpty());
        }

    @Test
    void shouldSuppressStackMetadataInSafeMode()
        {
        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeExceptionWithPolicy(
                new IllegalStateException("secret password"),
                GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);

        assertEquals(Status.INTERNAL.getCode(), result.getStatus().getCode());
        assertEquals(ErrorsHelper.SAFE_INTERNAL_ERROR_MESSAGE, result.getStatus().getDescription());
        assertTrue(ErrorsHelper.getRemoteStack(result).isEmpty());
        }

    @Test
    void shouldPreserveExplicitStatusDescriptionInSafeMode()
        {
        StatusRuntimeException rejected = Status.INVALID_ARGUMENT
                .withDescription("invalid request format")
                .asRuntimeException();

        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeExceptionWithPolicy(
                new CompletionException(rejected),
                GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);

        assertEquals(Status.INVALID_ARGUMENT.getCode(), result.getStatus().getCode());
        assertEquals("invalid request format", result.getStatus().getDescription());
        assertTrue(ErrorsHelper.getRemoteStack(result).isEmpty());
        }

    @Test
    void shouldGenericizeStatusDescriptionWithCauseInSafeMode()
        {
        StatusRuntimeException rejected = Status.UNKNOWN
                .withDescription("Caught an exception while serializing or deserializing: secret token")
                .withCause(new IllegalStateException("secret token"))
                .asRuntimeException();

        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeExceptionWithPolicy(
                rejected,
                GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);

        assertEquals(Status.UNKNOWN.getCode(), result.getStatus().getCode());
        assertEquals(ErrorsHelper.SAFE_INTERNAL_ERROR_MESSAGE, result.getStatus().getDescription());
        assertTrue(ErrorsHelper.getRemoteStack(result).isEmpty());
        }

    @Test
    void shouldGenericizeErrorHandlingConverterStatusDescriptionInSafeMode()
            throws Exception
        {
        Converter<Object, Object> converter = errorHandlingConverter(value ->
            {
            throw new IllegalStateException("secret serializer token");
            });

        StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class, () -> converter.convert("value"));
        assertEquals(Status.UNKNOWN.getCode(), thrown.getStatus().getCode());
        assertTrue(thrown.getStatus().getDescription().contains("secret serializer token"));

        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeExceptionWithPolicy(
                thrown,
                GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);

        assertEquals(Status.UNKNOWN.getCode(), result.getStatus().getCode());
        assertEquals(ErrorsHelper.SAFE_INTERNAL_ERROR_MESSAGE, result.getStatus().getDescription());
        assertTrue(ErrorsHelper.getRemoteStack(result).isEmpty());
        }

    @Test
    void shouldPreserveStatusDescriptionWithCauseInDiagnosticMode()
        {
        StatusRuntimeException rejected = Status.UNKNOWN
                .withDescription("Caught an exception while serializing or deserializing: diagnostic")
                .withCause(new IllegalStateException("diagnostic"))
                .asRuntimeException();

        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeExceptionWithPolicy(
                rejected,
                GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC);

        assertEquals(Status.UNKNOWN.getCode(), result.getStatus().getCode());
        assertEquals("Caught an exception while serializing or deserializing: diagnostic",
                result.getStatus().getDescription());
        assertTrue(ErrorsHelper.getRemoteStack(result).isPresent());
        }

    @Test
    void shouldPreserveDiagnosticStackMetadata()
        {
        StatusRuntimeException result = ErrorsHelper.ensureStatusRuntimeExceptionWithPolicy(
                new IllegalStateException("diagnostic"),
                GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC);

        assertEquals(Status.INTERNAL.getCode(), result.getStatus().getCode());
        assertEquals("diagnostic", result.getStatus().getDescription());
        assertTrue(ErrorsHelper.getRemoteStack(result).orElse("").contains("IllegalStateException"));
        }

    @Test
    void shouldSuppressSerializedErrorInSafeMode()
        {
        Serializer   serializer = new DefaultSerializer();
        ErrorMessage message    = ErrorsHelper.createErrorMessage(
                new IllegalStateException("secret token"), serializer, GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);

        assertEquals(ErrorsHelper.SAFE_INTERNAL_ERROR_MESSAGE, message.getMessage());
        assertFalse(message.hasError());
        }

    @Test
    void shouldGenericizeStatusCauseErrorMessageInSafeMode()
        {
        Serializer   serializer = new DefaultSerializer();
        ErrorMessage message    = ErrorsHelper.createErrorMessage(
                Status.UNKNOWN
                        .withDescription("Caught an exception while serializing or deserializing: secret token")
                        .withCause(new IllegalStateException("secret token"))
                        .asRuntimeException(),
                serializer,
                GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);

        assertEquals(ErrorsHelper.SAFE_INTERNAL_ERROR_MESSAGE, message.getMessage());
        assertFalse(message.hasError());
        }

    @Test
    void shouldPreserveSerializedErrorInDiagnosticMode()
        {
        Serializer   serializer = new DefaultSerializer();
        ErrorMessage message    = ErrorsHelper.createErrorMessage(
                new IllegalStateException("diagnostic"), serializer, GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC);

        assertEquals("diagnostic", message.getMessage());
        assertTrue(message.hasError());
        }

    @Test
    void shouldCreateExceptionFromMessageOnlyError()
        {
        ErrorMessage               message = ErrorMessage.newBuilder().setMessage("bounded message").build();
        RequestIncompleteException result  = ErrorsHelper.createException(message, new DefaultSerializer());

        assertNotNull(result);
        assertEquals("bounded message", result.getMessage());
        assertNull(result.getCause());
        }

    @SuppressWarnings("unchecked")
    private static Converter<Object, Object> errorHandlingConverter(Converter<Object, Object> converter)
            throws ReflectiveOperationException
        {
        Class<?>      clz         = Class.forName("com.oracle.coherence.grpc.v0.RequestHolder$ErrorHandlingConverter");
        Constructor<?> constructor = clz.getDeclaredConstructor(Converter.class);
        constructor.setAccessible(true);
        try
            {
            return (Converter<Object, Object>) constructor.newInstance(converter);
            }
        catch (InvocationTargetException e)
            {
            Throwable cause = e.getCause();
            if (cause instanceof ReflectiveOperationException)
                {
                throw (ReflectiveOperationException) cause;
                }
            throw e;
            }
        }
    }
