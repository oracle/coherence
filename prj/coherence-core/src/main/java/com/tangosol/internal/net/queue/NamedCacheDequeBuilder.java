/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.net.SessionNamedDeque;
import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

import java.util.ServiceLoader;

public interface NamedCacheDequeBuilder
        extends NamedCollection.Option
    {
    /**
     * Return the cache name to use for a given collection name.
     *
     * @param sName  the name of the collection
     *
     * @return the cache name to use for the given collection name
     */
    String getCacheName(String sName);

    /**
     * Return the collection name from a given cache name.
     *
     * @param sCacheName  the name of the cache
     *
     * @return the collection name from the given cache name
     */
    String getCollectionName(String sCacheName);

    <E> NamedCacheDeque<E> build(String sName, NamedCache<QueueKey, E> cache);


    <E> SessionNamedDeque<E, ?> wrapForSession(Session session, NamedDeque<E> deque,
            ClassLoader loader, ValueTypeAssertion<E> typeAssertion);

    // ----- inner class: DefaultNamedCacheDequeBuilder ---------------------

    /**
     * The default implementation of {@link NamedCacheDequeBuilder}.
     */
    class DefaultNamedCacheDequeBuilder
            implements NamedCacheDequeBuilder
        {
        @Override
        public String getCacheName(String sName)
            {
            return sName;
            }

        @Override
        public String getCollectionName(String sCacheName)
            {
            return sCacheName;
            }

        @Override
        public <E> NamedCacheDeque<E> build(String sName, NamedCache<QueueKey, E> cache)
            {
            return new NamedCacheDeque<>(sName, cache);
            }

        /**
         * Create a {@link SessionNamedDeque} wrapper for a {@link NamedDeque}.
         *
         * @param session  the {@link ConfigurableCacheFactorySession} that produced this {@link SessionNamedDeque}
         * @param deque          the {@link NamedDeque} to wrap
         * @param loader         the {@link ClassLoader} associated with the deque
         * @param typeAssertion  the {@link ValueTypeAssertion} for the NamedDeque
         */
        @Override
        public <E> SessionNamedDeque<E, ?> wrapForSession(Session session, NamedDeque<E> deque,
                ClassLoader loader, ValueTypeAssertion<E> typeAssertion)
            {
            return new SessionNamedDeque<>(session, deque, loader, typeAssertion);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The singleton instance of the default {@link NamedCacheDequeBuilder}.
     */
    NamedCacheDequeBuilder DEFAULT = new DefaultNamedCacheDequeBuilder();
    }
