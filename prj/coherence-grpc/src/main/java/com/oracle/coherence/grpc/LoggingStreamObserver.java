/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.oracle.coherence.common.base.Logger;
import io.grpc.stub.StreamObserver;

/**
 * A {@link StreamObserver} that logs calls.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class LoggingStreamObserver<V>
        implements StreamObserver<V>
    {
    /**
     * Create a {@link LockingStreamObserver}.
     *
     * @param delegate  the {@link StreamObserver} to delegate to
     * @param sPrefix   the prefix to add to log messages
     */
    public LoggingStreamObserver(StreamObserver<V> delegate, String sPrefix)
        {
        f_delegate = delegate;
        f_sPrefix  = sPrefix;
        }

    @Override
    public void onNext(V v)
        {
        Logger.info( "LoggingStreamObserver: " + f_sPrefix + " onNext() called message=" + v);
        f_delegate.onNext(v);
        }

    @Override
    public void onError(Throwable throwable)
        {
        Logger.info( "LoggingStreamObserver: " + f_sPrefix + " onError() called error=" + throwable);
        f_delegate.onError(throwable);
        }

    @Override
    public void onCompleted()
        {
        Logger.info( "LoggingStreamObserver: " + f_sPrefix + " onCompleted() called");
        f_delegate.onCompleted();
        }

    // ----- data members ---------------------------------------------------

    private final StreamObserver<V> f_delegate;

    private final String f_sPrefix;
    }
