/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.NamedMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * A {@link Collection} backed by a {@link com.tangosol.net.NamedMap} values.
 */
public abstract class NamedMapValuesCollection<K, V>
        implements NamedMapCollection<K, V, V>, Collection<V>
    {
    /**
     * Create a {@link Collection} that wraps a cache values.
     *
     * @param sName  the name of the cache to wrap
     * @param cache  the cache to wrap
     *
     * @throws NullPointerException      if either of the {@code name} or {@code cache} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public NamedMapValuesCollection(String sName, NamedMap<K, V> cache)
        {
        m_sName = Objects.requireNonNull(sName);
        m_cache = Objects.requireNonNull(cache);
        if (sName.isEmpty())
            {
            throw new IllegalArgumentException("the name parameter cannot be an empty String");
            }
        }

    // ----- accessors ------------------------------------------------------

    public NamedMap<K, V> getNamedMap()
        {
        return m_cache;
        }

    @Override
    public String getName()
        {
        return m_sName;
        }

    @Override
    public boolean isDestroyed()
        {
        return m_cache.isDestroyed();
        }

    @Override
    public boolean isActive()
        {
        return m_cache.isActive();
        }

    @Override
    public boolean isReleased()
        {
        return m_cache.isReleased();
        }

    @Override
    public void destroy()
        {
        destroy(m_cache);
        }

    @Override
    public void release()
        {
        release(m_cache);
        }

    // ----- Collection methods ---------------------------------------------

    @Override
    public int size()
        {
        return m_cache.size();
        }

    @Override
    public boolean isEmpty()
        {
        return m_cache.isEmpty();
        }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean contains(Object o)
        {
        return m_cache.containsValue(o);
        }

    @Override
    public Iterator<V> iterator()
        {
        return m_cache.values().iterator();
        }

    @Override
    public Object[] toArray()
        {
        return m_cache.values().toArray();
        }

    @Override
    public <T> T[] toArray(T[] a)
        {
        return m_cache.values().toArray(a);
        }

    @Override
    public boolean add(V v)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public boolean remove(Object o)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public boolean containsAll(Collection<?> c)
        {
        if (this.equals(c))
            {
            return true;
            }
        return m_cache.values().containsAll(c);
        }

    @Override
    public boolean addAll(Collection<? extends V> c)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public boolean removeAll(Collection<?> c)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public boolean retainAll(Collection<?> c)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public void clear()
        {
        m_cache.clear();
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedMapValuesCollection<?, ?> that = (NamedMapValuesCollection<?, ?>) o;
        return Objects.equals(m_sName, that.m_sName) && Objects.equals(m_cache, that.m_cache);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_sName, m_cache);
        }

    // ----- helper methods -------------------------------------------------

    protected void assertNotNull(V oValue)
        {
        if (oValue == null)
            {
            throw new NullPointerException("this collection does not accept null values");
            }
        }

    public void assertNotSameCollection(Collection<?> c, String sMsg)
        {
        if (this.equals(Objects.requireNonNull(c)))
            {
            throw new IllegalArgumentException(sMsg);
            }

        if (c instanceof NamedMapCollection<?,?,?>)
            {
            NamedMap<?, ?> map = ((NamedMapCollection<?,?,?>) c).getNamedMap();
            if (m_cache.equals(map))
                {
                throw new IllegalArgumentException(sMsg);
                }
            if (map instanceof SessionNamedCache<?,?>)
                {
                map = ((SessionNamedCache<?, ?>) map).getInternalNamedCache();
                }
            if (m_cache.equals(map))
                {
                throw new IllegalArgumentException(sMsg);
                }
            }
        }

    protected void release(NamedMap<?, ?> map)
        {
        if (map != null && !map.isReleased() && !map.isDestroyed())
            {
            try
                {
                map.release();
                }
            catch (Exception e)
                {
                Logger.err(e);
                }
            }
        }

    protected void destroy(NamedMap<?, ?> map)
        {
        if (map != null && !map.isReleased() && !map.isDestroyed())
            {
            try
                {
                map.destroy();
                }
            catch (Exception e)
                {
                Logger.err(e);
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of this collection.
     */
    protected final String m_sName;

    /**
     * The {@link NamedMap} that contains the collection data.
     */
    protected final NamedMap<K, V> m_cache;
    }
