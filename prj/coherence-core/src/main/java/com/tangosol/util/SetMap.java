/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Map implementation based on a known {@link Set set} of keys and a {@link
 * Function} that when given a key can derive the value. Once a value has been
 * derived for a key the function will not be called again for the same key.
 * <p>
 * This map can be mutated outside of the original set of keys or within the
 * set of keys. The latter will result in the function not being called for the
 * respective keys.
 * <p>
 * This implementation is the inverse to a {@link MapSet} which can trivially
 * distill a Map of keys and values to a Set of keys. With the use of a Function
 * this implementation allows a Set of keys to be converted lazily to a Map of
 * keys and values.
 *
 * @author hr  2016.09.29
 * @since 12.2.1.4.0
 */
public class SetMap<K, V>
        extends AbstractMap<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a SetMap.
     *
     * @param setKeys  a set of keys to base this Map on
     */
    public SetMap(Set<K> setKeys)
        {
        this(setKeys, k -> null);
        }

    /**
     * Construct a SetMap.
     *
     * @param setKeys        a set of keys to base this Map on
     * @param functionValue  a {@link Function} to derive the value for a given
     *                       key
     */
    public SetMap(Set<K> setKeys, Function<K, V> functionValue)
        {
        this(setKeys, functionValue, HashMap::new);
        }

    /**
     * Construct a SetMap.
     *
     * @param setKeys        a set of keys to base this Map on
     * @param functionValue  a {@link Function} to derive the value for a given
     *                       key
     * @param supplierMap    the Map to use to hold keys and values
     */
    public SetMap(Set<K> setKeys, Function<K, V> functionValue, Supplier<Map<K, V>> supplierMap)
        {
        f_functionValue = functionValue;
        f_map           = (Map<K, Object>) supplierMap.get();

        for (K key : setKeys)
            {
            f_map.put(key, NO_VALUE);
            }
        }

    // ----- Map interface --------------------------------------------------

    @Override
    public int size()
        {
        return f_map.size();
        }

    @Override
    public boolean isEmpty()
        {
        return f_map.isEmpty();
        }

    @Override
    public boolean containsKey(Object key)
        {
        return f_map.containsKey(key);
        }

    @Override
    public V get(Object key)
        {
        return ensureValue(key);
        }

    @Override
    public V put(K key, V value)
        {
        return getExternalMap().put(key, value);
        }

    @Override
    public V remove(Object key)
        {
        ensureValue(key);

        return getExternalMap().remove(key);
        }

    @Override
    public void putAll(Map<? extends K, ? extends V> map)
        {
        getExternalMap().putAll(map);
        }

    @Override
    public void clear()
        {
        getExternalMap().clear();
        }

    @Override
    public Set<K> keySet()
        {
        return getExternalMap().keySet();
        }

    @Override
    public Set<Entry<K, V>> entrySet()
        {
        return ConverterCollections.<Entry<K, Object>, Entry<K, V>>getSet(
                getInternalMap().entrySet(),
                entry ->
                    {
                    if (entry.getValue() == NO_VALUE)
                        {
                        entry.setValue(f_functionValue.apply(entry.getKey()));
                        }
                    return (Entry<K, V>) entry;
                    },
                NullImplementation.getConverter());
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the internal map allowing any object to be stored.
     *
     * @return the internal map
     */
    protected Map<K, Object> getInternalMap()
        {
        return f_map;
        }

    /**
     * Return an external map allowing type safe access.
     *
     * @return an external map
     */
    protected Map<K, V> getExternalMap()
        {
        return (Map<K, V>) f_map;
        }

    /**
     * Return a value for the specified key.
     *
     * @param oKey  the key to load a value for
     *
     * @return a value for the specified key
     */
    protected V ensureValue(Object oKey)
        {
        K      key    = (K) oKey;
        Object oValue = getInternalMap().get(key);
        V      value;
        if (oValue == NO_VALUE)
            {
            getExternalMap().put(key, value = f_functionValue.apply(key));
            }
        else
            {
            value = (V) oValue;
            }
        return value;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A reference that indicates a value has not been loaded using the function.
     */
    private static final Object NO_VALUE = new Object();

    // ----- data members ---------------------------------------------------

    /**
     * The underlying map.
     */
    protected Map<K, Object> f_map;

    /**
     * A function to load a value for a provided key.
     */
    protected Function<K, V> f_functionValue;
    }
