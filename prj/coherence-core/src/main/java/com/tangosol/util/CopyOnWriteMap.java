/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * A thread-safe variant of {@link Map} in which all mutating operations
 * (e.g. {@code put}, {@code putAll}) are implemented by making a fresh copy of
 * the underlying map.
 * <p>
 * Iterators over this map are guaranteed to produce a safe-iteration and
 * not to throw {@code ConcurrentModificationException}. The iterator will not
 * reflect concurrent additions, removals, or changes to this map.
 * <p>
 * Note: mutations on this map are costly, but may be <em>more</em>
 * efficient than alternatives when "get" operations vastly outnumber
 * mutations.  All mutating operations are synchronized, so concurrent mutation
 * can be prevented by holding synchronization on this object.
 *
 * @since  Coherence 3.7.2
 *
 * @author pp 2011.09.17, rhl 2010.09.09 (from CopyOnWriteLongArray)
 */
public class CopyOnWriteMap<K, V>
        implements Map<K, V>
    {
    // ----- constructors -------------------------------------------------

    public CopyOnWriteMap(Class clazz)
        {
        setInternalMap(instantiateMap(clazz));
        }

    /**
     * Construct a CopyOnWriteMap, initialized with the contents of the
     * specified map.
     *
     * @param map  the initial map
     */
    public CopyOnWriteMap(Map<K, V> map)
        {
        setInternalMap(copyMap(map));
        }


    // ----- accessors ----------------------------------------------------

    /**
     * Return the internal map.
     *
     * @return  the internal map
     */
    protected Map<K, V> getInternalMap()
        {
        return m_mapInternal;
        }

    /**
     * Set the internal map.
     *
     * @param map  the new internal map
     */
    protected void setInternalMap(Map<K, V> map)
        {
        m_mapInternal = map;
        }


    // ----- helpers ------------------------------------------------------

    /**
     * Create a new instance of {@link Map} based on the provided {@link Class}.
     *
     * @param clazz  the type of {@link Map} to instantiate
     *
     * @return a new empty instance of {@link Map}
     *
     * @throws IllegalArgumentException if the provided {@link Class} does not implement {@link Map}.
     */
    protected Map<K, V> instantiateMap(Class clazz)
        {
        if (!Map.class.isAssignableFrom(clazz))
            {
            throw new IllegalArgumentException("Expected a class assignable from java.util.Map instead of "
                    + clazz.getName());
            }

        try
            {
            return (Map) clazz.newInstance();
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Could not create new instance of " + clazz.getName());
            }
        }

    /**
     * Create an instance of {@link Map} based on the contents of the provided map.
     *
     * @param map  the map to copy
     *
     * @return  an instance of {@link Map} populated with the contents of the provided map
     */
    protected Map<K, V> copyMap(Map<K, V> map)
        {
        Map<K, V> mapNew = instantiateMap(map.getClass());

        mapNew.putAll(map);

        return mapNew;
        }


    // ----- Map methods --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public int size()
        {
        return getInternalMap().size();
        }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
        {
        return getInternalMap().isEmpty();
        }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object oKey)
        {
        return getInternalMap().containsKey(oKey);
        }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object oValue)
        {
        return getInternalMap().containsValue(oValue);
        }

    /**
     * {@inheritDoc}
     */
    public V get(Object oKey)
        {
        return getInternalMap().get(oKey);
        }

    /**
     * {@inheritDoc}
     */
    public synchronized V put(K oKey, V oValue)
        {
        Map<K, V> mapNew    = copyMap(getInternalMap());
        V         oValueOld = mapNew.put(oKey, oValue);
        setInternalMap(mapNew);

        return oValueOld;
        }

    /**
     * {@inheritDoc}
     */
    public synchronized V remove(Object oKey)
        {
        Map<K, V> mapNew    = copyMap(getInternalMap());
        V         oValueOld = mapNew.remove(oKey);
        setInternalMap(mapNew);

        return oValueOld;
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void putAll(Map<? extends K, ? extends V> map)
        {
        Map<K, V> mapNew = copyMap(getInternalMap());
        mapNew.putAll(map);
        setInternalMap(mapNew);
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void clear()
        {
        setInternalMap(instantiateMap(m_mapInternal.getClass()));
        }

    /**
     * Returns a Set view of the keys contained in the underlying map.
     *
     * @return a Set view of the keys contained in the underlying map
     */
    public Set<K> keySet()
        {
        KeySet set = m_setKeys;
        if (set == null)
            {
            m_setKeys = set = new KeySet();
            }
        return set;
        }

    /**
     * Return a set view of the underlying map.
     *
     * @return a set view of the underlying map
     */
    public Set<Entry<K, V>> entrySet()
        {
        EntrySet set = m_setEntries;
        if (set == null)
            {
            m_setEntries = set = new EntrySet();
            }
        return set;
        }

    /**
     * {@inheritDoc}
     */
    public Collection<V> values()
        {
        return Collections.unmodifiableCollection(getInternalMap().values());
        }


    // ----- inner class: KeySet --------------------------------------------

    /**
     * A set of keys backed by this map.
     */
    protected class KeySet
            extends AbstractSet<K>
        {
        // ----- Set interface ------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<K> iterator()
            {
            return new Iterator()
                {
                public boolean hasNext()
                    {
                    return m_iter.hasNext();
                    }

                public K next()
                    {
                    return m_keyNext = m_iter.next();
                    }

                public void remove()
                    {
                    CopyOnWriteMap.this.remove(m_keyNext);
                    m_keyNext = null;
                    }

                private Iterator<K> m_iter =
                        CopyOnWriteMap.this.getInternalMap().keySet().iterator();
                private K m_keyNext;
                };
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size()
            {
            return CopyOnWriteMap.this.size();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean contains(Object oKey)
            {
            return CopyOnWriteMap.this.containsKey(oKey);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean remove(Object o)
            {
            CopyOnWriteMap map = CopyOnWriteMap.this;

            if (map.containsKey(o))
                {
                map.remove(o);
                return true;
                }
            else
                {
                return false;
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean removeAll(Collection coll)
            {
            assert coll != null;

            boolean fRemoved;

            synchronized (CopyOnWriteMap.this)
                {
                Map<K, V> mapNew = copyMap(getInternalMap());

                fRemoved = mapNew.keySet().removeAll(coll);
                setInternalMap(mapNew);
                }
            return fRemoved;
            }

        /**
         * {@inheritDoc}
         */
        public void clear()
            {
            CopyOnWriteMap.this.clear();
            }
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
     * A set of entries backed by this map.
     */
    protected class EntrySet
            extends AbstractSet<Entry<K, V>>
        {
        // ----- Set interface ------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<Entry<K, V>> iterator()
            {
            return new Iterator()
                {
                public boolean hasNext()
                    {
                    return m_iter.hasNext();
                    }

                public Entry<K, V> next()
                    {
                    return m_entryNext = m_iter.next();
                    }

                public void remove()
                    {
                    CopyOnWriteMap.this.remove(m_entryNext.getKey());
                    m_entryNext = null;
                    }

                private Iterator<Entry<K, V>>  m_iter =
                        CopyOnWriteMap.this.getInternalMap().entrySet().iterator();
                private Entry<K, V> m_entryNext;
                };
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size()
            {
            return CopyOnWriteMap.this.size();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean contains(Object o)
            {
            if (o instanceof Entry)
                {
                Entry entryThat = (Entry) o;
                Entry entryThis = (Entry) CopyOnWriteMap.this.get(entryThat.getKey());
                return entryThis != null && entryThis.equals(entryThat);
                }
            return false;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean remove(Object o)
            {
            CopyOnWriteMap map = CopyOnWriteMap.this;
            if (contains(o))
                {
                map.remove(((Map.Entry) o).getKey());
                return true;
                }
            else
                {
                return false;
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear()
            {
            CopyOnWriteMap.this.clear();
            }
        }


    // ----- Object methods -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        CopyOnWriteMap that = (CopyOnWriteMap) o;

        return Base.equals(this.m_mapInternal, that.m_mapInternal);
        }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
        {
        return m_mapInternal == null ? 0 : m_mapInternal.hashCode();
        }

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return "CopyOnWriteMap{" + m_mapInternal + "}";
        }


    // ----- constants and data members -----------------------------------

    /**
     * The internal map.
     */
    private volatile Map<K, V> m_mapInternal;

    /**
     * The set of keys backed by this map.
     */
    private KeySet m_setKeys;

    /**
     * The set of entries backed by this map.
     */
    private EntrySet m_setEntries;
    }
