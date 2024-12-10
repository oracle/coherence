/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.stream;

import com.tangosol.util.InvocableMap;

import java.io.Serializable;

/**
 * A serializable pipeline of intermediate stream operations.
 *
 * @author as  2014.10.01
 * @since 12.2.1
 */
public interface RemotePipeline<S_OUT> extends Serializable
    {
    /**
     * Return whether this pipeline should be executed in parallel.
     *
     * @return <code>true</code> if this pipeline should be executed in parallel,
     *         <code>false</code> otherwise
     */
    public boolean isParallel();

    /**
     * Evaluate this pipeline against the specified stream of InvocableMap.Entry
     * objects.
     *
     * @param stream  the stream to evaluate pipeline against
     * @param <K>     key type
     * @param <V>     value type
     *
     * @return transformed stream
     */
    public <K, V> S_OUT evaluate(java.util.stream.Stream<? extends InvocableMap.Entry<? extends K, ? extends V>> stream);
    }
