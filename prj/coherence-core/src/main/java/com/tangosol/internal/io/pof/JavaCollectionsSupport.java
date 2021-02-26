/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.io.pof;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * {@link PofSerializer} implementations for the {@link java.util} collection
 * classes that are not natively supported in POF.
 *
 * @author Aleks Seovic  2021.02.25
 * @since 21.06
 */
public class JavaCollectionsSupport
    {
    /**
     * SortedSet serializer.
     */
    public static class SortedSetSerializer<T>
            implements PofSerializer<SortedSet<T>>
        {
        @Override
        public void serialize(PofWriter out, SortedSet<T> set) throws IOException
            {
            out.writeObject(0, set.comparator());
            out.writeCollection(1, set);
            out.writeRemainder(null);
            }

        @Override
        public SortedSet<T> deserialize(PofReader in) throws IOException
            {
            Comparator<? super T> comparator = in.readObject(0);
            SortedSet<T> set = in.readCollection(1, new TreeSet<>(comparator)); 
            in.readRemainder();
            return set;
            }
        }

    /**
     * SortedMap serializer.
     */
    public static class SortedMapSerializer<K, V>
            implements PofSerializer<SortedMap<K, V>>
        {
        @Override
        public void serialize(PofWriter out, SortedMap<K, V> map) throws IOException
            {
            out.writeObject(0, map.comparator());
            out.writeMap(1, map);
            out.writeRemainder(null);
            }

        @Override
        public SortedMap<K, V> deserialize(PofReader in) throws IOException
            {
            Comparator<? super K> comparator = in.readObject(0);
            SortedMap<K, V> map = in.readMap(1, new TreeMap<>(comparator));
            in.readRemainder();
            return map;
            }
        }
    }
