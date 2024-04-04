/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.oracle.coherence.grpc.proxy.common.NamedCacheService;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheEvent;

import com.tangosol.util.Base;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.TestObserver;

import static org.mockito.Mockito.mock;

/**
 * A base class for named cache gRPC service integration tests.
 */
public abstract class BaseNamedCacheServiceImplIT
        extends BaseGrpcIT
    {
    // ----- inner class: CollectingMapListener -----------------------------

    @SuppressWarnings("unchecked")
    protected static class CollectingMapListener<K, V>
            extends TestObserver<MapEvent<K, V>>
            implements MapListener<K, V>
        {

        public CollectingMapListener()
            {
            onSubscribe(mock(Disposable.class));
            }

        @Override
        public void entryInserted(MapEvent<K, V> mapEvent)
            {
            onNext(toSimpleEvent(mapEvent));
            }

        @Override
        public void entryUpdated(MapEvent<K, V> mapEvent)
            {
            onNext(toSimpleEvent(mapEvent));
            }

        @Override
        public void entryDeleted(MapEvent<K, V> mapEvent)
            {
            onNext(toSimpleEvent(mapEvent));
            }

        @SuppressWarnings("rawtypes")
        protected MapEvent<K, V> toSimpleEvent(MapEvent<K, V> event)
            {
            if (event instanceof CacheEvent)
                {
                CacheEvent<K, V> ce = (CacheEvent) event;
                return new CacheEvent<>(ce.getMap(), ce.getId(), ce.getKey(), ce.getOldValue(), ce.getNewValue(), ce.isSynthetic(), ce.getTransformationState(), ce.isPriming());
                }
            return new MapEvent<>(event.getMap(), event.getId(), event.getKey(), event.getOldValue(), event.getNewValue());
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create an instance of the {@link NamedCacheService} to use for testing.
     *
     * @return an instance of the {@link NamedCacheService} to use for testing
     */
    protected NamedCacheService createService()
        {
        NamedCacheService service = m_service;
        if (service == null)
            {
            service = m_service = createCacheService();
            }
        return service;
        }

    /**
     * Obtain the specified {@link NamedCache}.
     *
     * @param <K>  the type of the cache keys
     * @param <V>  the type of the cache values
     *
     * @param sScope  the scope name of the cache
     * @param name    the cache name
     * @return the specified {@link NamedCache}
     */
    protected <K, V> NamedCache<K, V> ensureEmptyCache(String sScope, String name)
        {
        NamedCache<K, V> cache = ensureCache(sScope, name, Base.getContextClassLoader());
        cache.clear();
        return cache;
        }

    // ----- data members ---------------------------------------------------

    private NamedCacheService m_service;
    }
