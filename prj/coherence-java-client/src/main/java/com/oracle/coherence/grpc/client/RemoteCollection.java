/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.google.protobuf.ByteString;

import com.oracle.coherence.grpc.EntryResult;

import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.util.ConverterCollections;
import com.tangosol.util.PagedIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import java.util.concurrent.ExecutionException;

import java.util.stream.Collectors;

/**
 * A base class for implementations of collections related to a remote {@link AsyncNamedCacheClient},
 * for example key set, entry set and values.
 * <p>
 * This {@link Collection} implementation allows removal but does not allow additions to the
 * collection. Methods {@link #add(Object)} {@link #addAll(Collection)} will throw an
 * {@link UnsupportedOperationException}.
 * <p>
 * Some methods in this class are intentionally inefficient partly due to their being a more efficient
 * means to perform the same task using the underlying {@link AsyncNamedCacheClient} and partly to ensure
 * that using this class on a client will not cause all of the data from the underlying
 * {@link AsyncNamedCacheClient} to be pulled back to the caller in one result.
 *
 * @author Jonathan Knight  2019.11.12
 * @since 14.1.2
 */
public abstract class RemoteCollection<K, V, T>
        implements Collection<T>
    {
    // ----- constructors ---------------------------------------------------

    protected RemoteCollection(AsyncNamedCacheClient<K, V> client)
        {
        this.f_client = client;
        }

    // ----- Collection methods ---------------------------------------------

    @Override
    public int size()
        {
        try
            {
            return f_client.size().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public boolean isEmpty()
        {
        try
            {
            return f_client.isEmpty().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public boolean add(T t)
        {
        throw new UnsupportedOperationException("add operations are not supported");
        }

    @Override
    public boolean addAll(Collection<? extends T> c)
        {
        throw new UnsupportedOperationException("add operations are not supported");
        }

    @Override
    public void clear()
        {
        try
            {
            f_client.clear().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public boolean containsAll(Collection<?> colKeys)
        {
        if (colKeys == null)
            {
            throw new NullPointerException("collection parameter cannot be null");
            }

        if (colKeys.isEmpty())
            {
            return true;
            }

        for (Object o : colKeys)
            {
            if (!contains(o))
                {
                return false;
                }
            }
        return true;
        }

    @Override
    public boolean removeAll(Collection<?> colKeys)
        {
        boolean fModified = false;
        if (size() > colKeys.size())
            {
            for (Object colKey : colKeys)
                {
                fModified |= remove(colKey);
                }
            }
        else
            {
            for (Iterator iter = iterator(); iter.hasNext(); )
                {
                if (colKeys.contains(iter.next()))
                    {
                    iter.remove();
                    fModified = true;
                    }
                }
            }

        return fModified;
        }

    @Override
    public boolean retainAll(Collection<?> colKeys)
        {
        boolean fModified = false;

        for (Iterator iter = iterator(); iter.hasNext(); )
            {
            Object o = iter.next();
            if (!colKeys.contains(o))
                {
                iter.remove();
                fModified = true;
                }
            }
        return fModified;
        }

    @Override
    public boolean equals(Object other)
        {
        if (other == this)
            {
            return true;
            }

        if (other instanceof Collection)
            {
            Collection<?> colOther = (Collection<?>) other;

            if (colOther.size() != size())
                {
                return false;
                }

            try
                {
                return containsAll(colOther);
                }
            catch (ClassCastException | NullPointerException unused)
                {
                return false;
                }
            }

        return false;
        }

    @Override
    public int hashCode()
        {
        int h = 0;

        for (T obj : this)
            {
            if (obj != null)
                {
                h += obj.hashCode();
                }
            }

        return h;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return getClass().getSimpleName() + "(name='" + f_client.getCacheName() + "')";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the underlying {@link NamedCache}.
     *
     * @return the underlying {@link NamedCache}
     */
    protected AsyncNamedCacheClient<K, V> getCache()
        {
        return f_client;
        }

    /**
     * Create an {@link EntryAdvancer} to use in a {@link PagedIterator}.
     *
     * @return an {@link EntryAdvancer} to use in a {@link PagedIterator}
     */
    protected EntryAdvancer<K, V> createEntryAdvancer()
        {
        return new EntryAdvancer<>(getCache());
        }

    // ----- inner class: EntryAdvancer -------------------------------------

    /**
     * A {@link PagedIterator.Advancer} to support a
     * {@link PagedIterator} over an entry set.
     */
    protected static class EntryAdvancer<K, V>
            implements PagedIterator.Advancer
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code EntryAdvancer} using the provided {@link AsyncNamedCacheClient}.
         *
         * @param client  the async client
         */
        protected EntryAdvancer(AsyncNamedCacheClient<K, V> client)
            {
            this.m_client = client;
            }

        // ----- Advancer interface -----------------------------------------

        @Override
        public void remove(Object oCurr)
            {
            Map.Entry entry = (Map.Entry) oCurr;
            try
                {
                m_client.removeInternal(entry.getKey()).get();
                }
            catch (InterruptedException | ExecutionException e)
                {
                throw new RequestIncompleteException(e);
                }
            }

        @Override
        public Collection nextPage()
            {
            if (m_exhausted)
                {
                return null;
                }

            LinkedList<EntryResult> list = m_client.getEntriesPage(m_cookie).collect(Collectors.toCollection(LinkedList::new));

            if (list.size() > 0)
                {
                m_cookie = list.poll().getCookie();
                }
            else
                {
                m_cookie = null;
                }

            m_exhausted = m_cookie == null || m_cookie.isEmpty();

            // Use ConverterCollections so that deserialization is only done if for entries as they
            // are iterated over. This is more efficient that deserializing all of the entries
            // now as there is no guarantee that the caller will actually iterate over the whole
            // entry set or that they will call getKey() or getValue() on all entries.
            Collection<Map.Entry<ByteString, ByteString>> entries = ConverterCollections
                    .getCollection(list, this::fromEntryResult, this::toEntryResult);

            return ConverterCollections.getEntrySet(entries, m_client::fromByteString, m_client::toByteString,
                                                    m_client::fromByteString, m_client::toByteString);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Convert a {@link EntryResult} to a {@link Map.Entry} where the
         * key and value are {@link ByteString} instances.
         *
         * @param result the {@link EntryResult} to wrap in a {@link Map.Entry}
         *
         * @return a {@link Map.Entry} that wraps an {@link EntryResult}
         */
        protected Map.Entry<ByteString, ByteString> fromEntryResult(EntryResult result)
            {
            return new EntryResultMapEntry(result);
            }

        /**
         * Convert a {@link Map.Entry} to an {@link EntryResult}.
         *
         * @param entry the {@link Map.Entry} to convert
         *
         * @return an {@link EntryResult} that takes its key and value from a {@link Map.Entry}
         */
        protected EntryResult toEntryResult(Map.Entry<ByteString, ByteString> entry)
            {
            return EntryResult.newBuilder()
                    .setKey(m_client.toByteString(entry.getKey()))
                    .setValue(m_client.toByteString(entry.getValue()))
                    .build();
            }

        // ----- data members -----------------------------------------------

        /**
         * A flag indicating whether this advancer has exhausted all of the pages.
         */
        protected boolean m_exhausted;

        /**
         * The opaque cookie used by the server to maintain the page location.
         */
        protected ByteString m_cookie;

        /**
         * The {@link AsyncNamedCacheClient} used to send gRPC requests.
         */
        protected final AsyncNamedCacheClient<K, V> m_client;
        }

    // ----- inner class: EntryResultMapEntry -------------------------------

    /**
     * A {@link Map.Entry} implementation that wraps a {@link EntryResult}.
     */
    protected static class EntryResultMapEntry
            implements Map.Entry<ByteString, ByteString>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link EntryResultMapEntry} wrapping a {@link EntryResult}.
         *
         * @param entryResult the {@link EntryResult} to wrap
         */
        protected EntryResultMapEntry(EntryResult entryResult)
            {
            this.entryResult = entryResult;
            }

        // ----- Entry interface --------------------------------------------

        @Override
        public ByteString getKey()
            {
            return entryResult.getKey();
            }

        @Override
        public ByteString getValue()
            {
            return entryResult.getValue();
            }

        @Override
        public ByteString setValue(ByteString value)
            {
            ByteString old = entryResult.getValue();
            entryResult = entryResult.toBuilder().setValue(value).build();
            return old;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link EntryResult}.
         */
        private EntryResult entryResult;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link AsyncNamedCacheClient}.
     */
    protected  final AsyncNamedCacheClient<K, V> f_client;
    }
