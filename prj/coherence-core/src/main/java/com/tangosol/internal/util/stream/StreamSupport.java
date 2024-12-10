/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.function.Remote;

import com.tangosol.util.stream.RemotePipeline;
import com.tangosol.util.stream.RemoteStream;

import java.util.Collection;
import java.util.Objects;

import java.util.stream.Stream;

/**
 * Low-level utility methods for creating and manipulating streams.
 *
 * @author as  2014.09.16
 * @since 12.2.1
 */
public abstract class StreamSupport
    {
    /**
     * Create a new sequential or parallel {@code Stream} for an {@code
     * InvocableMap} entries.
     *
     * @param map         an {@code InvocableMap} containing the stream elements
     * @param fParallel   if {@code true} then the returned stream is a parallel
     *                    stream; if {@code false} the returned stream is a
     *                    sequential stream
     * @param colKeys     an optional collection of keys
     * @param filter      an optional filter
     *
     * @return a new sequential or parallel {@code Stream}
     */
    public static <K, V> RemoteStream<InvocableMap.Entry<K, V>> entryStream(
            InvocableMap<K, V> map, boolean fParallel, Collection<? extends K> colKeys, Filter filter)
        {
        Objects.requireNonNull(map);

        ReferencePipeline<K, V, InvocableMap.Entry<K, V>, InvocableMap.Entry<K, V>, Stream<InvocableMap.Entry<K, V>>> head =
                new ReferencePipeline.Head<>(map, fParallel, colKeys, filter, Remote.Function.identity());
        return head.unordered();
        }

    /**
     * Create a builder for a {@code Pipeline} for a {@code Stream} of {@code
     * InvocableMap} entries.
     * <p>
     * Calling any terminal operation other than {@link RemoteStream#pipeline()} on
     * the returned stream will throw an {@code IllegalStateException}.
     *
     * @return a new parallel {@code Stream} that can be used to build a
     *         {@link RemotePipeline} of intermediate operations
     *
     * @throws IllegalStateException if any terminal operation other than
     *                               {@link RemoteStream#pipeline()} is invoked
     *                               on the returned stream
     */
    public static <K, V> RemoteStream<InvocableMap.Entry<K, V>> pipelineBuilder()
        {
        ReferencePipeline<K, V, InvocableMap.Entry<K, V>, InvocableMap.Entry<K, V>, Stream<InvocableMap.Entry<K, V>>> head =
                new ReferencePipeline.Head<>(null, true, null, null, Remote.Function.identity());
        return head.unordered();
        }

    /**
     * Create an empty pipeline for a stream of {@code InvocableMap} entries.
     *
     * @param <K> the type of entry key
     * @param <V> the type of entry value
     *
     * @return an empty pipeline for a stream of entries
     */
    public static <K, V> RemotePipeline<Stream<InvocableMap.Entry<K, V>>> emptyPipeline()
        {
        return new ReferencePipeline.Head<>(null, true, null, null, Remote.Function.identity());
        }
    }
