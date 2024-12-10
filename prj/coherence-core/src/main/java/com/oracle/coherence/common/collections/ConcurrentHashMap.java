/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.collections;

import java.util.function.BiFunction;
import java.util.function.Function;

import java.util.Map;

/**
 * An extension to the standard {@link java.util.concurrent.ConcurrentHashMap} that fixes some of its deficiencies.
 *
 * @author mf  2016.01.17
 */
public class ConcurrentHashMap<K, V>
    extends java.util.concurrent.ConcurrentHashMap<K, V>
    {
    /**
     * Creates a new, empty map with the default initial table size (16).
     */
    public ConcurrentHashMap()
        {
        super();
        }

    /**
     * Creates a new, empty map with an initial table size
     * accommodating the specified number of elements without the need
     * to dynamically resize.
     *
     * @param initialCapacity The implementation performs internal
     * sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative
     */
    public ConcurrentHashMap(int initialCapacity)
        {
        super(initialCapacity);
        }

    /**
     * Creates a new map with the same mappings as the given map.
     *
     * @param m the map
     */
    public ConcurrentHashMap(Map<? extends K, ? extends V> m)
        {
        super(m);
        }

    /**
     * Creates a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}) and
     * initial table density ({@code loadFactor}).
     *
     * @param initialCapacity the initial capacity. The implementation
     * performs internal sizing to accommodate this many elements,
     * given the specified load factor.
     * @param loadFactor the load factor (table density) for
     * establishing the initial table size
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative or the load factor is nonpositive
     */
    public ConcurrentHashMap(int initialCapacity, float loadFactor)
        {
        super(initialCapacity, loadFactor);
        }

    /**
     * Creates a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}), table
     * density ({@code loadFactor}), and number of concurrently
     * updating threads ({@code concurrencyLevel}).
     *
     * @param initialCapacity the initial capacity. The implementation
     * performs internal sizing to accommodate this many elements,
     * given the specified load factor.
     * @param loadFactor the load factor (table density) for
     * establishing the initial table size
     * @param concurrencyLevel the estimated number of concurrently
     * updating threads. The implementation may use this value as
     * a sizing hint.
     * @throws IllegalArgumentException if the initial capacity is
     * negative or the load factor or concurrencyLevel are
     * nonpositive
     */
    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel)
        {
        super(initialCapacity, loadFactor, concurrencyLevel);
        }

    /**
     *  As compared to {@link java.util.concurrent.ConcurrentHashMap#computeIfAbsent(Object, Function)} this
     *  implementation will not  synchronize unless the mappingFunction will be run.
     *
     * {@inheritDoc}
     */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
        {
        // j.u.c.CHM always syncs on the entry even if it won't update the value!
        V v = get(key);
        return v == null
                ? super.computeIfAbsent(key, mappingFunction)
                : v;
        }

    /**
     *  As compared to {@link java.util.concurrent.ConcurrentHashMap#computeIfPresent(Object, BiFunction)} this
     *  implementation will not synchronize unless the mappingFunction will be run.
     *
     * {@inheritDoc}
     */
    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        // j.u.c.CHM always syncs on the entry even if it won't update the value!
        return containsKey(key)
                ? super.computeIfPresent(key, remappingFunction)
                : null;
        }
    }
