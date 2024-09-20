/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.transformer;

import com.tangosol.util.CollectionListener;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import java.util.Objects;

/**
 * A {@link MapListener} that wraps a {@link CollectionListener}.
 */
public class MapListenerCollectionListener<K, V>
        implements MapListener<K, V>
    {
    /**
     * Create a {@link MapListenerCollectionListener}.
     *
     * @param wrapped  the {@link CollectionListener} to wrap
     *
     * @throws NullPointerException if the wrapped listener is {@code null}
     */
    public MapListenerCollectionListener(CollectionListener<V> wrapped)
        {
        f_wrapped = Objects.requireNonNull(wrapped);
        }

    @Override
    public void entryInserted(MapEvent<K, V> evt)
        {
        f_wrapped.entryInserted(evt);
        }

    @Override
    public void entryUpdated(MapEvent<K, V> evt)
        {
        f_wrapped.entryUpdated(evt);
        }

    @Override
    public void entryDeleted(MapEvent<K, V> evt)
        {
        f_wrapped.entryDeleted(evt);
        }

    @Override
    public int characteristics()
        {
        return f_wrapped.characteristics();
        }

    @Override
    public boolean isAsynchronous()
        {
        return f_wrapped.isAsynchronous();
        }

    @Override
    public boolean isSynchronous()
        {
        return f_wrapped.isSynchronous();
        }

    @Override
    public boolean isVersionAware()
        {
        return f_wrapped.isVersionAware();
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapListenerCollectionListener<?, ?> that = (MapListenerCollectionListener<?, ?>) o;
        return Objects.equals(f_wrapped, that.f_wrapped);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_wrapped);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link CollectionListener}.
     */
    private final CollectionListener<V> f_wrapped;
    }
