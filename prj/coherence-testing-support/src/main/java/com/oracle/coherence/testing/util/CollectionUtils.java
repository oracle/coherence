/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A set of utility methods to make creating various
 * collections of objects inside test methods less verbose.
 *
 * @author jk  2013.12.03
 */
public class CollectionUtils
    {
    /**
     * Create a {@link Set} made up of the specified values.
     *
     * @param values  the values to add to the {@link Set}
     * @param <T>     the type of element the {@link Set} contains
     *
     * @return a {@link Set} containing the specified values
     */
    @SafeVarargs
    public static <T> Set<T> setWith(T... values)
        {
        return new HashSet<T>(Arrays.asList(values));
        }

    /**
     * Create a {@link SortedSet} made up of the specified values.
     *
     * @param values  the values to add to the {@link SortedSet}
     * @param <T>     the type of element the {@link Set} contains
     *
     * @return a {@link SortedSet} containing the specified values
     */
    @SafeVarargs
    public static <T> SortedSet<T> sortedSetWith(T... values)
        {
        return sortedSetWith((Comparator) null, values);
        }

    /**
     * Create a {@link SortedSet} made up of the specified values
     * and sorted using the specified {@link Comparator}.
     *
     * @param comparator  the {@link Comparator} to use to sort the {@link SortedSet}
     * @param values      the values to add to the {@link SortedSet}
     * @param <T>         the type of element the {@link Set} contains
     *
     * @return a {@link SortedSet} containing the specified values
     */
    @SafeVarargs
    public static <T> SortedSet<T> sortedSetWith(Comparator<T> comparator, T... values)
        {
        TreeSet<T> set = new TreeSet<T>(comparator);

        set.addAll(Arrays.asList(values));

        return set;
        }

    /**
     * Create a {@link Map} made up of the specified key value pairs.
     * Each pair array should contain two values, the value at index zero
     * is used for the map entry's key and the value at index one is used
     * for the map entry's value. Any pair that is null or has a length
     * less than two cause an exception to be thrown.
     *
     * @param pairs  the key and value pairs to add to the Map
     * @param <K>    the type of the keys of the Map
     * @param <V>    the type of values of the Map
     *
     * @return a Map containing the specified key/value pairs
     *
     * @throws java.lang.IllegalArgumentException if any of the pairs is null
     *         or has a length less than two.
     */
    public static <K, V> Map<K, V> mapWith(Object[]... pairs)
        {
        Map map = new HashMap();

        for (Object[] pair : pairs)
            {
            if (pair == null && pair.length < 2)
                {
                throw new IllegalArgumentException("Invalid pair array specified");
                }

            map.put(pair[0], pair[1]);
            }

        return map;
        }

    /**
     * Construct an Object array of two elements.
     *
     * @param left   the first element of the array
     * @param right  the second element of the array
     * @param <T>    the type of the values in the array
     *
     * @return an array of the specified pair of values
     */
    public static <T> T[] pair(T left, T right)
        {
        return tuple(left, right);
        }

    /**
     * This method basically converts (or casts) a varargs call to an Object array.
     *
     * @param values  the varargs values to cast to an array
     * @param <T>     the type of the values in the tuple
     *
     * @return an array of the specified varargs types
     */
    @SafeVarargs
    public static <T> T[] tuple(T... values)
        {
        return values;
        }

    /**
     * Create a {@link List} from the elements contained in the
     * specified {@link Enumeration}.
     *
     * @param enumeration  the Enumeration to convert to a List
     * @param <T>          the type of the values in the List
     *
     * @return a List from the elements contained in the
     *         specified Enumeration.
     */
    public static <T> List<T> asList(Enumeration<T> enumeration)
        {
        ArrayList<T> list = new ArrayList<T>();

        while (enumeration.hasMoreElements())
            {
            list.add(enumeration.nextElement());
            }

        return list;
        }
    }
