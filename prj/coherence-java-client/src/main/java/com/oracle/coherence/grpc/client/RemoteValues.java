/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.PagedIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import java.util.concurrent.ExecutionException;

/**
 * A base class for {@link NamedCache} values collection implementations.
 * <p>
 * This {@link Collection} implementation allows removal but does not allow additions
 * to the collection. Methods {@link #add(Object)} {@link #addAll(Collection)} will
 * throw an {@link UnsupportedOperationException}.
 * <p>
 * Some methods in this class are intentionally inefficient partly due to their being a
 * more efficient means to perform the same task using the underlying {@link NamedCache}
 * and partly to ensure that using this class on a client will not cause all of the data
 * from the underlying {@link NamedCache} to be pulled back to the caller in one result.
 *
 * @param <K> the type of the underlying cache's keys
 * @param <V> the type of the underlying cache's values
 *
 * @author Jonathan Knight  2019.11.12
 * @since 14.1.2
 */
public class RemoteValues<K, V>
        extends RemoteCollection<K, V, V>
        implements Collection<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link RemoteValues}.
     *
     * @param client  the {@link AsyncNamedCacheClient} that this key set is linked to
     */
    protected RemoteValues(AsyncNamedCacheClient<K, V> client)
        {
        super(client);
        }

    // ----- Set interface --------------------------------------------------

    @Override
    public boolean contains(Object value)
        {
        try
            {
            return getCache().containsValue(value).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public Iterator<V> iterator()
        {
        return new ValuesIterator<>(getCache());
        }

    @Override
    public boolean remove(Object value)
        {
        AsyncNamedCacheClient<K, V> cache  = getCache();
        Filter<V>                   filter = Filters.equal(Extractors.identity(), value);
        InvocableMap.Entry<K, V>    entry  = cache.stream(filter).findFirst().orElse(null);

        NamedCache x;

        if (entry == null)
            {
            return false;
            }
        else
            {
            return cache.remove(entry.getKey()) != null;
            }
        }

    @Override
    public Object[] toArray()
        {
        try
            {
            return getCache().values(Filters.always()).get().toArray();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T1> T1[] toArray(T1[] array)
        {
        try
            {
            return getCache().values(Filters.always()).get().toArray(array);
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    // ----- inner class: ValuesIterator ------------------------------------

    /**
     * An {@link Iterator} to iterate over the cache values.
     *
     * @param <K>  the type of the cache keys
     * @param <V>  the type of the cache values
     */
    protected static class ValuesIterator<K, V>
            implements Iterator<V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link ValuesIterator} to iterate over a cache's values.
         *
         * @param client the cache to iterate over
         */
        @SuppressWarnings("unchecked")
        protected ValuesIterator(AsyncNamedCacheClient<K, V> client)
            {
            this.f_client   = client;
            this.f_iterator = new PagedIterator(new EntryAdvancer(client));
            }

        // ----- Iterator interface -----------------------------------------

        @Override
        public boolean hasNext()
            {
            return f_iterator.hasNext();
            }

        @Override
        public V next()
            {
            m_currentEntry = f_iterator.next();
            return m_currentEntry.getValue();
            }

        // ----- Iterator methods -------------------------------------------

        @Override
        public void remove()
            {
            try
                {
                f_client.remove(m_currentEntry.getKey()).get();
                }
            catch (InterruptedException | ExecutionException e)
                {
                throw new RequestIncompleteException(e);
                }
            }
        
        // ----- data members -----------------------------------------------

        /**
         * The underlying cache client.
         */
        protected final AsyncNamedCacheClient<K, V> f_client;

        /**
         * The paged iterator to use to iterate over the cache values
         * one page at a time.
         */
        protected final Iterator<Map.Entry<K, V>> f_iterator;

        /**
         * The current entry in the iterator.
         */
        protected Map.Entry<K, V> m_currentEntry;
        }
    }
