/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestIncompleteException;

import java.util.Collection;
import java.util.Iterator;

import java.util.concurrent.ExecutionException;

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
 * that using this class on a client will not cause all the data from the underlying
 * {@link AsyncNamedCacheClient} to be pulled back to the caller in one result.
 *
 * @author Jonathan Knight  2019.11.12
 * @since 20.06
 */
@SuppressWarnings("PatternVariableCanBeUsed")
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
            for (Iterator<?> iter = iterator(); iter.hasNext(); )
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

        for (Iterator<?> iter = iterator(); iter.hasNext(); )
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

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link AsyncNamedCacheClient}.
     */
    protected  final AsyncNamedCacheClient<K, V> f_client;
    }
