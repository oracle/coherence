/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.google.protobuf.BytesValue;

import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.util.ConverterCollections;
import com.tangosol.util.Filters;
import com.tangosol.util.PagedIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import java.util.concurrent.ExecutionException;

import java.util.stream.Collectors;

/**
 * A base class for {@link NamedCache} key set implementations.
 * <p>
 * This {@link Set} implementation allows removal but does not allow additions
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
 * @since 20.06
 */
public class RemoteKeySet<K, V>
        extends RemoteCollection<K, V, K>
        implements Set<K>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link RemoteKeySet}.
     *
     * @param client the {@link NamedCacheClient} that this key set is linked to
     */
    protected RemoteKeySet(AsyncNamedCacheClient<K, V> client)
        {
        super(client);
        }

    // ----- Set methods ----------------------------------------------------

    @Override
    public boolean contains(Object key)
        {
        if (key == null)
            {
            throw new NullPointerException("key cannot be null");
            }
        try
            {
            return getCache().containsKeyInternal(key).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<K> iterator()
        {
        return new PagedIterator(new KeysAdvancer(getCache()));
        }

    @Override
    public boolean remove(Object key)
        {
        try
            {
            return getCache().removeInternal(key).get() != null;
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public Object[] toArray()
        {
        try
            {
            return getCache().keySet(Filters.always()).get().toArray();
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
            return getCache().keySet(Filters.always()).get().toArray(array);
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    // ----- inner class: KeyAdvancer ---------------------------------------

    /**
     * An {@link PagedIterator.Advancer} to support a
     * {@link PagedIterator} over this key set.
     */
    protected static class KeysAdvancer
            implements PagedIterator.Advancer
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code KeysAdvancer}.
         *
         * @param client the {@link AsyncNamedCacheClient}
         */
        protected KeysAdvancer(AsyncNamedCacheClient<?, ?> client)
            {
            this.f_client = client;
            }

        // ----- Advancer interface -----------------------------------------

        @Override
        public void remove(Object o)
            {
            try
                {
                f_client.removeInternal(o).get();
                }
            catch (InterruptedException | ExecutionException e)
                {
                throw new RequestIncompleteException(e);
                }
            }

        @Override
        public Collection nextPage()
            {
            if (m_fExhausted)
                {
                return null;
                }

            LinkedList<BytesValue> list = f_client.getKeysPage(m_cookie).collect(Collectors.toCollection(LinkedList::new));

            if (list.size() > 0)
                {
                m_cookie = list.poll();
                }
            else
                {
                m_cookie = null;
                }

            m_fExhausted = m_cookie == null || m_cookie.getValue().isEmpty();

            return ConverterCollections.getCollection(list, f_client::fromBytesValue, f_client::toBytesValue);
            }

        // ----- data members -----------------------------------------------

        /**
         * The client to use to send gRPC requests.
         */
        protected final AsyncNamedCacheClient<?, ?> f_client;

        /**
         * A flag indicating whether this advancer has exhausted all of the pages.
         */
        protected boolean m_fExhausted;

        /**
         * The opaque cookie used by the server to maintain the page location.
         */
        protected BytesValue m_cookie;
        }
    }
