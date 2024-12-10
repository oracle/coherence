/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.stream;

import java.util.stream.BaseStream;

/**
 * An extension of {@link java.util.stream.BaseStream} that adds support for
 * the {@link RemotePipeline} retrieval.
 *
 * @param <T> the type of the stream elements
 * @param <S> the type of of the stream implementing {@code BaseStream}
 *
 * @author as  2014.08.26          
 * @since 12.2.1
 */
public interface BaseRemoteStream<T, S extends BaseStream<T, S>>
        extends BaseStream<T, S>
    {
    /**
     * Return a pipeline of intermediate operations for this stream.
     * <p>
     * This is a terminal operation.
     *
     * @return a pipeline of intermediate operations for this stream
     */
    RemotePipeline<S> pipeline();
    }
