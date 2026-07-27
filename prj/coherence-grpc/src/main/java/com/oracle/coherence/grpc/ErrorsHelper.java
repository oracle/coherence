/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.grpc.messages.common.v1.ErrorMessage;

import com.tangosol.io.Serializer;

import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.grpc.GrpcDiagnosticsPolicy;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import java.net.SocketException;

import java.nio.charset.StandardCharsets;

import java.util.Base64;
import java.util.Optional;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import java.util.function.BiFunction;

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
        return ensureStatusRuntimeExceptionWithPolicy(t, GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC);
        }

    /**
     * Convert a {@link java.lang.Throwable} to a {@link io.grpc.StatusRuntimeException}.
     *
     * @param t                 the {@link java.lang.Throwable} to convert
     * @param sErrorDisclosure  the gRPC error-disclosure policy
     *
     * @return a {@link io.grpc.StatusRuntimeException}
     */
    public static StatusRuntimeException ensureStatusRuntimeExceptionWithPolicy(Throwable t, String sErrorDisclosure)
        {
        Throwable cause = unwrapStatusException(t);
        boolean   fSafe = GrpcDiagnosticsPolicy.isErrorDisclosureSafe(sErrorDisclosure);

        if (cause instanceof StatusRuntimeException)
            {
            return fSafe
                    ? sanitize((StatusRuntimeException) cause)
                    : enrich((StatusRuntimeException) cause);
            }
        else if (cause instanceof StatusException)
            {
            Status status = ((StatusException) cause).getStatus();
            return fSafe
                    ? sanitize(status).asRuntimeException()
                    : status.asRuntimeException(getErrorMetadata(cause));
            }
        else
            {
            return Status.INTERNAL.withCause(t)
                    .withDescription(fSafe ? SAFE_INTERNAL_ERROR_MESSAGE : t.getMessage())
                    .asRuntimeException(fSafe ? new Metadata() : getErrorMetadata(t));
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
        return ensureStatusRuntimeException(t, description, GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC);
        }

    /**
     * Convert a {@link java.lang.Throwable} to a {@link io.grpc.StatusRuntimeException}.
     *
     * @param t                 the {@link java.lang.Throwable} to convert
     * @param description       the description to add to the exception
     * @param sErrorDisclosure  the gRPC error-disclosure policy
     *
     * @return a {@link io.grpc.StatusRuntimeException}
     */
    public static StatusRuntimeException ensureStatusRuntimeException(Throwable t, String description, String sErrorDisclosure)
        {
        Status status;
        Throwable cause = unwrapStatusException(t);
        boolean   fSafe = GrpcDiagnosticsPolicy.isErrorDisclosureSafe(sErrorDisclosure);

        if (cause instanceof StatusRuntimeException)
            {
            status = ((StatusRuntimeException) cause).getStatus().getCode().toStatus();
            }
        else if (cause instanceof StatusException)
            {
            status = ((StatusException) cause).getStatus().getCode().toStatus();
            }
        else
            {
            status = Status.INTERNAL;
            }
        return status.withCause(t)
                .withDescription(fSafe ? bound(description) : description)
                .asRuntimeException(fSafe ? new Metadata() : getErrorMetadata(t));
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

    /**
     * Obtain the remote stack trace from the specified {@link StatusException}.
     *
     * @param e  the exception returned from a server call
     *
     * @return  the remote stack trace, or an empty {@link Optional} if no remote
     *          stack is present.
     */
    public static Optional<String> getRemoteStack(StatusException e)
        {
        Metadata trailers = e.getTrailers();
        String s = trailers == null ? null : trailers.get(ErrorsHelper.KEY_ERROR);
        return s == null ? Optional.empty() : Optional.of(new String(s_decoder.decode(s)));
        }

    /**
     * Create a {@link RequestIncompleteException} from an {@link ErrorMessage}.
     *
     * @param error       the {@link ErrorMessage}
     * @param serializer  the {@link Serializer} to deserialize the stack trace
     *
     * @return the {@link RequestIncompleteException} created from the {@link ErrorMessage}
     */
    public static RequestIncompleteException createException(ErrorMessage error, Serializer serializer)
        {
        return createException(error, serializer, RequestIncompleteException::new);
        }

    /**
     * Create an exception from an {@link ErrorMessage}.
     *
     * @param error       the {@link ErrorMessage}
     * @param serializer  the {@link Serializer} to deserialize the stack trace
     * @param factory     the factory function to create the resulting exception
     * @param <E>         the type of exception to return
     *
     * @return the exception created from the {@link ErrorMessage}
     */
    public static <E extends Exception> E createException(ErrorMessage error, Serializer serializer, BiFunction<String, Throwable, E> factory)
        {
        if (error == null)
            {
            return null;
            }
        Throwable cause = null;
        if (error.hasError())
            {
            cause = BinaryHelper.fromByteString(error.getError(), serializer);
            }
        return factory.apply(error.getMessage(), cause);
        }

    /**
     * Create an {@link ErrorMessage} from a {@link Throwable}.
     *
     * @param error       the error to create the message from
     * @param serializer  the serializer to use to serialize the stack trace
     *
     * @return  the error message
     */
    public static ErrorMessage createErrorMessage(Throwable error, Serializer serializer)
        {
        return createErrorMessage(error, serializer, GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC);
        }

    /**
     * Create an {@link ErrorMessage} from a {@link Throwable}.
     *
     * @param error             the error to create the message from
     * @param serializer        the serializer to use to serialize the stack trace
     * @param sErrorDisclosure  the gRPC error-disclosure policy
     *
     * @return  the error message
     */
    public static ErrorMessage createErrorMessage(Throwable error, Serializer serializer, String sErrorDisclosure)
        {
        boolean fSafe = GrpcDiagnosticsPolicy.isErrorDisclosureSafe(sErrorDisclosure);
        String  sMsg  = fSafe ? getSafeMessage(error) : error.getMessage();
        if (!fSafe && (sMsg == null || sMsg.isEmpty()))
            {
            sMsg = error.getClass().getSimpleName();
            }

        ErrorMessage.Builder builder = ErrorMessage.newBuilder()
                .setMessage(sMsg);

        if (!fSafe && serializer != null)
            {
            try
                {
                if (error instanceof StatusRuntimeException)
                    {
                    Throwable cause = error.getCause();
                    if (cause != null)
                        {
                        error = cause;
                        }
                    }
                if (error instanceof StatusException)
                    {
                    Throwable cause = error.getCause();
                    if (cause != null)
                        {
                        error = cause;
                        }
                    }
                builder.setError(BinaryHelper.toByteString(error, serializer));
                }
            catch (Throwable t)
                {
                Logger.err(t);
                }
            }

        return builder.build();
        }

    // ----- helper methods -------------------------------------------------

    private static StatusRuntimeException sanitize(StatusRuntimeException e)
        {
        return sanitize(e.getStatus()).asRuntimeException();
        }

    private static Status sanitize(Status status)
        {
        String sDescription = status.getDescription();
        if (hasInternalCause(status))
            {
            sDescription = SAFE_INTERNAL_ERROR_MESSAGE;
            }
        return status.getCode().toStatus()
                .withCause(status.getCause())
                .withDescription(sDescription == null ? null : bound(sDescription));
        }

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

    private static Throwable unwrapStatusException(Throwable t)
        {
        Throwable result = t;
        Throwable cause  = result.getCause();
        while (cause != null && isStatusException(cause) && isStatusWrapper(result))
            {
            result = cause;
            cause  = result.getCause();
            }
        return result;
        }

    private static boolean isStatusWrapper(Throwable t)
        {
        return t instanceof CompletionException
                || t instanceof ExecutionException
                || (t instanceof StatusRuntimeException sre && sre.getStatus().getCode() == Status.Code.INTERNAL)
                || (t instanceof StatusException se && se.getStatus().getCode() == Status.Code.INTERNAL);
        }

    private static boolean isStatusException(Throwable t)
        {
        return t instanceof StatusRuntimeException || t instanceof StatusException;
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
        t.printStackTrace(new PrintStream(out, true, StandardCharsets.UTF_8));
        byte[] abStack = out.toByteArray();
        byte[] abEncoded;
        if (abStack.length > MAX_STACK_LENGTH)
            {
            byte[] abTruncated = new byte[MAX_STACK_LENGTH];
            System.arraycopy(abStack, 0, abTruncated, 0, MAX_STACK_LENGTH);
            abEncoded = s_encoder.encode(abTruncated);
            }
        else
            {
            abEncoded = s_encoder.encode(abStack);
            }
        return new String(abEncoded);
        }

    private static String getSafeMessage(Throwable error)
        {
        Throwable cause = unwrapStatusException(error);
        if (cause instanceof StatusRuntimeException)
            {
            return safeStatusMessage(((StatusRuntimeException) cause).getStatus());
            }
        if (cause instanceof StatusException)
            {
            return safeStatusMessage(((StatusException) cause).getStatus());
            }
        return SAFE_INTERNAL_ERROR_MESSAGE;
        }

    private static String safeStatusMessage(Status status)
        {
        if (hasInternalCause(status))
            {
            return SAFE_INTERNAL_ERROR_MESSAGE;
            }
        String sDescription = status.getDescription();
        return sDescription == null || sDescription.isBlank()
                ? SAFE_INTERNAL_ERROR_MESSAGE
                : bound(sDescription);
        }

    private static boolean hasInternalCause(Status status)
        {
        return status.getCause() != null;
        }

    private static String bound(String sDescription)
        {
        if (sDescription == null || sDescription.length() <= MAX_DESCRIPTION_LENGTH)
            {
            return sDescription;
            }
        return sDescription.substring(0, MAX_DESCRIPTION_LENGTH);
        }

    /**
     * Log the exception unless it is a {@link StatusRuntimeException}
     * with a status of {@link Status#CANCELLED}.
     *
     * @param t  the exception to log
     */
    public static void logIfNotCancelled(Throwable t)
        {
        if (rootCause(t) instanceof SocketException)
            {
            Logger.err(t.getMessage());
            }
        else
            {
            boolean     fLog = true;
            Status.Code code = null;

            if (t instanceof StatusRuntimeException sre)
                {
                code = sre.getStatus().getCode();
                }
            else if (t instanceof StatusException se)
                {
                code = se.getStatus().getCode();
                }

            if (code == Status.Code.UNAVAILABLE)
                {
                fLog = !t.getMessage().equals("Channel shutdownNow invoked");
                }
            else
                {
                fLog = code != null && code != Status.Code.CANCELLED && code != Status.Code.UNIMPLEMENTED;
                }

            if (fLog)
                {
                Logger.err(t);
                }
            }
        }

    private static Throwable rootCause(Throwable t)
        {
        Throwable rootCause = t;
        Throwable cause     = t.getCause();
        while (cause != null)
            {
            rootCause = cause;
            cause     = cause.getCause();
            }
        return rootCause;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The metadata key for an exception stack trace.
     */
    public static final Metadata.Key<String> KEY_ERROR = Metadata.Key.of("coherence-error-stack", ASCII_STRING_MARSHALLER);

    private static final Base64.Encoder s_encoder = Base64.getEncoder();

    private static final Base64.Decoder s_decoder = Base64.getDecoder();

    private static final int MAX_STACK_LENGTH = 1000;

    /**
     * Bounded generic message for unhandled internal errors in safe mode.
     */
    public static final String SAFE_INTERNAL_ERROR_MESSAGE = "internal gRPC request failed";

    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    }
