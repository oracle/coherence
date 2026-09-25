/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.util.function.Remote;

import java.util.Collection;
import java.util.Map;

/**
 * A handle for a streaming aggregation that is continuously maintained by a
 * {@link com.tangosol.util.InvocableMap}.
 * <p>
 * A handle is bound to the map that created it. Calling {@link #aggregate()}
 * performs a network operation that obtains the current partial results from
 * storage members and combines them into the final result. The final result is
 * not maintained or cached by the handle.
 *
 * @param <K>  the type of the map entry keys
 * @param <V>  the type of the map entry values
 * @param <R>  the type of the final aggregation result
 *
 * @author Aleks Seovic  2026.09.06
 * @since 26.10
 */
public interface ContinuousAggregator<K, V, R>
    {
    /**
     * Obtain the current aggregation result.
     * <p>
     * This method performs a network operation that collects continuously
     * maintained partial results from storage members and combines them into
     * the final result.
     *
     * @return the current aggregation result
     *
     * @throws IllegalStateException if this handle's registration has been
     *         removed
     */
    R aggregate();

    /**
     * Invoke a function against the continuously maintained result for the
     * partition containing the specified key.
     * <p>
     * This method performs an entry-processor network operation. The function
     * executes on the storage member after Coherence obtains and finalizes a
     * detached, exact result for that partition. The map entry itself is not
     * read or modified and does not need to be present.
     *
     * @param <T>       the function result type
     * @param key       a key identifying the partition to query
     * @param function  the function to apply to the partition result
     *
     * @return the function result
     *
     * @throws IllegalArgumentException if the key is associated with more
     *         than one partition
     * @throws IllegalStateException if this handle's registration has been
     *         removed
     */
    <T> T invoke(K key, Remote.Function<? super R, ? extends T> function);

    /**
     * Invoke a function against the continuously maintained partition result
     * for each specified key.
     * <p>
     * Each partition result is obtained and finalized at most once per
     * storage invocation, then reused for every key from that partition. Map
     * entries are not read or modified and do not need to be present.
     *
     * @param <T>       the function result type
     * @param keys      keys identifying the partitions to query
     * @param function  the function to apply to each key and its partition
     *                  result
     *
     * @return the function results, keyed by each supplied key
     *
     * @throws IllegalArgumentException if any key is associated with more
     *         than one partition
     * @throws IllegalStateException if this handle's registration has been
     *         removed
     */
    <T> Map<K, T> invokeAll(Collection<? extends K> keys,
            Remote.BiFunction<? super K, ? super R, ? extends T> function);
    }
