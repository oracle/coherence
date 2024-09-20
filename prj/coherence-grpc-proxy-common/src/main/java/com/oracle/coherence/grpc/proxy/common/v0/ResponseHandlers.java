/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy.common.v0;

import com.google.protobuf.BytesValue;

import com.google.protobuf.Int32Value;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.v0.RequestHolder;
import com.oracle.coherence.grpc.SafeStreamObserver;

import com.oracle.coherence.grpc.messages.cache.v0.Entry;
import com.tangosol.util.Binary;

import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.Set;

import java.util.function.Supplier;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helper methods to handle gRPC async responses.
 *
 * @author Jonathan Knight  2023.02.02
 * @since 23.03
 */
public class ResponseHandlers
    {
    /**
     * Handle an unary request.
     *
     * @param result    the result
     * @param observer  the {@link StreamObserver} to send the result to
     * @param <R>       the type of the result
     *
     * @return always returns {@link Void}
     */
    public static <R> Void handleUnary(R result, StreamObserver<R> observer)
        {
        return handleUnary(result, null, observer);
        }

    /**
     * Handle an unary request.
     *
     * @param result    the result
     * @param err       any error that may have occurred
     * @param observer  the {@link StreamObserver} to send the result to
     * @param <R>       the type of the result
     *
     * @return always returns {@link Void}
     */
    public static <R> Void handleUnary(R result, Throwable err, StreamObserver<R> observer)
        {
        if (err != null)
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        else
            {
            observer.onNext(result);
            observer.onCompleted();
            }
        return null;
        }

    /**
     * Handle the result of the asynchronous invoke all request sending the results, or any errors
     * to the {@link StreamObserver}.
     *
     * @param holder        the {@link RequestHolder} containing the request
     * @param err          any error that occurred during execution of the get all request
     * @param observer      the {@link StreamObserver} to receive the results
     * @return always return {@link Void}
     */
    public static Void handleMapOfEntries(RequestHolder<?, Map<Binary, Binary>> holder, Throwable err,
            StreamObserver<Entry> observer)
        {
        if (err == null)
            {
            handleStreamOfEntries(holder, holder.getResult().entrySet().stream(), observer);
            }
        else
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        return VOID;
        }

    /**
     * Handle the result of the asynchronous entry set request sending the results, or any errors
     * to the {@link StreamObserver}.
     *
     * @param holder        the {@link RequestHolder} containing the request
     * @param err           any error that occurred during execution of the get all request
     * @param observer      the {@link StreamObserver} to receive the results
     * @return always return {@link Void}
     */
    public static Void handleSetOfEntries(RequestHolder<?, Set<Map.Entry<Binary, Binary>>> holder, Throwable err,
            StreamObserver<Entry> observer)
        {
        if (err == null)
            {
            handleStreamOfEntries(holder, holder.getResult().stream(), observer);
            }
        else
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        return VOID;
        }

    /**
     * Handle the result of the asynchronous invoke all request sending the results, or any errors
     * to the {@link StreamObserver}.
     *
     * @param holder        the {@link RequestHolder} containing the request
     * @param entries       a {@link Stream} of entries
     * @param observer      the {@link StreamObserver} to receive the results
     */
    public static void handleStreamOfEntries(RequestHolder<?, ?> holder, Stream<Map.Entry<Binary, Binary>> entries,
            StreamObserver<Entry> observer)
        {
        try
            {
            entries.forEach(entry -> observer.onNext(holder.toEntry(entry.getKey(), entry.getValue())));
            observer.onCompleted();
            }
        catch (Throwable thrown)
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(thrown));
            }
        }

    /**
     * Send an {@link Iterable} of {@link Binary} instances to a {@link StreamObserver},
     * converting the {@link Binary} instances to a {@link BytesValue}.
     *
     * @param holder    the {@link RequestHolder} containing the request and
     *                  {@link Iterable} or {@link Binary} instances to stream
     * @param err       the error the pass to {@link StreamObserver#onError(Throwable)}
     * @param observer  the {@link StreamObserver} to receive the results
     *
     * @return always return {@link Void}
     */
    public static Void handleStream(RequestHolder<?, ? extends Iterable<Binary>> holder,
            Throwable err, StreamObserver<BytesValue> observer)
        {
        if (err == null)
            {
            try
                {
                Iterable<Binary>   iterable = holder.getResult();
                Stream<BytesValue> stream   = StreamSupport.stream(iterable.spliterator(), false)
                        .map(bin -> BinaryHelper.toBytesValue(holder.convertUp(bin)));
                stream(observer, stream);
                }
            catch (Throwable t)
                {
                observer.onError(ErrorsHelper.ensureStatusRuntimeException(t));
                }
            }
        else
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        return VOID;
        }

    /**
     * A handler method that streams results from an {@link Iterable} to a
     * {@link StreamObserver} and completes the {@link StreamObserver}, or
     * if an error is provided calls {@link StreamObserver#onError(Throwable)}.
     * <p>
     * Note: this method will complete by calling either {@link StreamObserver#onCompleted()}
     * or {@link StreamObserver#onError(Throwable)}.
     *
     * @param iterable  the elements to stream to the {@link StreamObserver}
     * @param err       the error the pass to {@link StreamObserver#onError(Throwable)}
     * @param observer  the {@link StreamObserver}
     * @param <Resp>    the type of the element to stream to the {@link StreamObserver}
     *
     * @return always returns {@link Void}
     */
    public static <Resp> Void handleStream(Iterable<Resp> iterable, Throwable err, StreamObserver<Resp> observer)
        {
        return handleStream(StreamSupport.stream(iterable.spliterator(), false), err, observer);
        }

    /**
     * A handler method that streams results to a {@link StreamObserver}
     * and completes the {@link StreamObserver} or if an error is
     * provided calls {@link StreamObserver#onError(Throwable)}.
     * <p>
     * Note: this method will complete by calling either {@link StreamObserver#onCompleted()}
     * or {@link StreamObserver#onError(Throwable)}.
     *
     * @param stream    the elements to stream to the {@link StreamObserver}
     * @param err       the error the pass to {@link StreamObserver#onError(Throwable)}
     * @param observer  the {@link StreamObserver}
     * @param <Resp>    the type of the element to stream to the {@link StreamObserver}
     *
     * @return always returns {@link Void}
     */
    public static <Resp> Void handleStream(Stream<Resp> stream, Throwable err, StreamObserver<Resp> observer)
        {
        if (err == null)
            {
            try
                {
                stream(observer, stream);
                }
            catch (Throwable t)
                {
                observer.onError(ErrorsHelper.ensureStatusRuntimeException(t));
                }
            }
        else
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        return VOID;
        }

    private static <T> void stream(StreamObserver<T> observer, Stream<? extends T> stream) {
        stream(observer, () -> stream);
    }

    private static <T> void stream(StreamObserver<T> observer, Supplier<Stream<? extends T>> supplier)
        {
        StreamObserver<T> safe = SafeStreamObserver.ensureSafeObserver(observer);
        Throwable thrown = null;

        try
            {
            supplier.get().forEach(safe::onNext);
            }
        catch (Throwable t)
            {
            thrown = t;
            }

        if (thrown == null)
            {
            safe.onCompleted();
            }
        else
            {
            safe.onError(thrown);
            }
        }

    /**
     * A handler method that will call {@link StreamObserver#onError(Throwable)} if the
     * error parameter is not {@code null}.
     * <p>
     * NOTE: this method will not complete the {@link StreamObserver} if there is no error.
     *
     * @param err       the error the pass to {@link StreamObserver#onError(Throwable)}
     * @param observer  the {@link StreamObserver}
     * @param <Resp>    the type of the element to stream to the {@link StreamObserver}
     *
     * @return always returns {@link Void}
     */
    public static <Resp> Void handleError(Throwable err, StreamObserver<Resp> observer)
        {
        if (err != null)
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        return VOID;
        }

    /**
     * A handler method that will call {@link StreamObserver#onError(Throwable)} if the
     * error parameter is not {@code null} otherwise calls {@link StreamObserver#onCompleted()}.
     * <p>
     * NOTE: this method will not complete the {@link StreamObserver} if there is no error.
     *
     * @param err       the error the pass to {@link StreamObserver#onError(Throwable)}
     * @param observer  the {@link StreamObserver}
     * @param <Resp>    the type of the element to stream to the {@link StreamObserver}
     *
     * @return always returns {@link Void}
     */
    public static <Resp> Void handleErrorOrComplete(Throwable err, StreamObserver<Resp> observer)
        {
        if (err == null)
            {
            observer.onCompleted();
            }
        else
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        return VOID;
        }

    /**
     * A handler method that will return the response if there is no error or if there
     * is an error then ensure that it is a {@link io.grpc.StatusRuntimeException}.
     *
     * @param response  the response to return if there is no error
     * @param err       the error to check
     * @param <Resp>    the type of the response
     *
     * @return always returns the passed in response
     */
    public static <Resp> Resp handleError(Resp response, Throwable err)
        {
        if (err != null)
            {
            throw ErrorsHelper.ensureStatusRuntimeException(err);
            }
        return response;
        }

    /**
     * Convert a value to an {@link Int32Value}.
     *
     * @param o  the value to convert
     *
     * @return the value of {@code o} as an {@link Int32Value} id {@code o}
     *         is a {@link Number}, otherwise {@link Int32Value#getDefaultInstance()}
     */
    public static Int32Value toInt32Value(Object o)
        {
        if (o instanceof Int32Value)
            {
            return (Int32Value) o;
            }
        if (o instanceof Number)
            {
            return Int32Value.of(((Number) o).intValue());
            }
        return Int32Value.getDefaultInstance();
        }

    // ----- constants ------------------------------------------------------

    /**
     * A {@link Void} value to make it obvious the return value in Void methods.
     */
    public static final Void VOID = null;
    }
