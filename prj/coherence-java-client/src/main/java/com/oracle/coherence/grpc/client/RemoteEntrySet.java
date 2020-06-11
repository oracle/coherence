/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.util.Filters;
import com.tangosol.util.PagedIterator;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ExecutionException;

/**
 * A base class for {@link NamedCache} entry set implementations.
 * <p>
 * This {@link Set} implementation allows removal but does not allow additions
 * to the collection. Methods {@link #add(Object)} {@link #addAll(java.util.Collection)} will
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
public class RemoteEntrySet<K, V>
        extends RemoteCollection<K, V, Map.Entry<K, V>>
        implements Set<Map.Entry<K, V>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link RemoteEntrySet}.
     *
     * @param map the {@link NamedCache} that this entry set is linked to
     */
    protected RemoteEntrySet(AsyncNamedCacheClient<K, V> map)
        {
        super(map);
        }

    // ----- Set interface --------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o)
        {
        if (o == null)
            {
            throw new NullPointerException("entry cannot be null");
            }

        if (o instanceof Map.Entry)
            {
            Map.Entry<K, V> entry = (Map.Entry<K, V>) o;

            K oKey   = entry.getKey();
            V oValue = entry.getValue();

            return getCache().containsEntry(oKey, oValue);
            }

        return false;
        }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Map.Entry<K, V>> iterator()
        {
        return new PagedIterator(createEntryAdvancer());
        }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o)
        {
        if (o instanceof Map.Entry)
            {
            Map.Entry<K, V> entry = (Map.Entry<K, V>) o;

            K oKey   = entry.getKey();
            V oValue = entry.getValue();

            try
                {
                return getCache().remove(oKey, oValue).get();
                }
            catch (InterruptedException | ExecutionException e)
                {
                throw new RequestIncompleteException(e);
                }
            }
        return false;
        }

    @Override
    public Object[] toArray()
        {
        try
            {
            return getCache().entrySet(Filters.always()).get().toArray();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public <T1> T1[] toArray(T1[] array)
        {
        try
            {
            //noinspection SuspiciousToArrayCall
            return getCache().entrySet(Filters.always()).get().toArray(array);
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }
    }
