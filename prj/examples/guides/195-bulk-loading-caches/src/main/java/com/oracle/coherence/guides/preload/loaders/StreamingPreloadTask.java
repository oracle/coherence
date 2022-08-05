/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.loaders;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.NamedMap;

import com.tangosol.net.Session;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A reactive streams preload task that takes values published by a {@link Publisher}
 * and loads them into a {@link NamedMap}.
 *
 * @param <P>  the type of the value published by the {@link Publisher}
 * @param <K>  the type of the cache key
 * @param <V>  they type of the cache value
 */
public abstract class StreamingPreloadTask<P, K, V>
        implements Runnable
    {
    /**
     * A create a {@link StreamingPreloadTask}.
     *
     * @param publisher  the {@link Publisher} supplying the values to load into the cache
     * @param session    the Coherence {@link Session} to use to obtain the cache to load
     * @param loader     the {@link JdbcPreloadTask.Loader} to use
     */
    protected StreamingPreloadTask(Publisher<P> publisher, Session session, Loader<P, K, V> loader)
        {
        this.publisher = publisher;
        this.session = session;
        this.loader = loader;
        }

    @Override
    public void run()
        {
        NamedMap<K, V> namedMap = session.getMap(loader.getMapName());

        Flux.from(publisher)
                .buffer(loader.getBatchSize())
                .map(list -> list.stream().collect(Collectors.toMap(loader::getKey, loader::getValue)))
                .doOnNext(map -> load(map, namedMap))
                .onErrorStop()
                .subscribe();
        }

    private void load(Map<K, V> map, NamedMap<K, V> namedMap)
        {
        namedMap.putAll(map);
        Logger.info(() -> String.format("Loaded batch of %d entries to NamedMap %s", map.size(), namedMap.getName()));
        }

    // ----- inner interface Loader ---------------------------------------------

    public interface Loader<P, K, V>
        {
        /**
         * Return the name of the {@link NamedMap} or {@link com.tangosol.net.NamedCache} to load.
         *
         * @return the name of the {@link NamedMap} or {@link com.tangosol.net.NamedCache} to load
         */
        String getMapName();

        /**
         * Returns the cache key for a given published value.
         *
         * @param published  the published value
         *
         * @return the cache key for a given published value
         */
        K getKey(P published);

        /**
         * Returns the cache value for a given published value.
         * <p>
         * This implementation returns the published value, but
         * subclasses can override this method to do other processing.
         *
         * @param published  the published value
         *
         * @return the cache value for a given published value
         */
        @SuppressWarnings("unchecked")
        default V getValue(P published)
            {
            return (V) published;
            }

        /**
         * Return the number of entries to load in a single batch.
         *
         * @return the number of entries to load in a single batch
         */
        int getBatchSize();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The reactive streams {@link Publisher} that will publish the values to load.
     */
    private final Publisher<P> publisher;

    /**
     * The Coherence {@link Session} to use.
     */
    private final Session session;

    /**
     * The {@link Loader} to use.
     */
    private final Loader<P, K, V> loader;
    }
