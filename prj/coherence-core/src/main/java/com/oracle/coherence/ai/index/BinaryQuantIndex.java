/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.index;

import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.VectorIndex;
import com.oracle.coherence.ai.VectorIndexExtractor;
import com.oracle.coherence.ai.search.BinaryQueryResult;
import com.oracle.coherence.ai.util.Vectors;
import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.net.BackingMapContext;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InflatableSet;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ValueExtractor;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntIterator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An {@link VectorIndexExtractor} to create a {@link VectorIndex} using binary quantization of vectors.
 * <p/>
 * Binary quantization converts any vector of floating point numbers into a vector of bit values.
 *
 * @param <K>  the type of the cache key
 * @param <V>  the type of the cache value
 * @param <T>  the type of the vector
 */
public class BinaryQuantIndex<K, V, T>
        extends AbstractEvolvable
        implements VectorIndexExtractor<V, T>, ExternalizableLite, EvolvablePortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public BinaryQuantIndex()
        {
        }

    /**
     * Create a {@link BinaryQuantIndex}.
     *
     * @param extractor  the {@link ValueExtractor} to use to extract the {@link Vector}
     */
    public BinaryQuantIndex(ValueExtractor<V, Vector<T>> extractor)
        {
        m_extractor = ValueExtractor.of(Objects.requireNonNull(extractor));
        }

    /**
     * Set the oversampling factor.
     *
     * @param nOversamplingFactor  the oversampling factor
     *
     * @return this {@link BinaryQuantIndex} for fluent API calls
     */
    public BinaryQuantIndex<K, V, T> oversamplingFactor(int nOversamplingFactor)
        {
        m_nOversamplingFactor = nOversamplingFactor;
        return this;
        }

    @Override
    public Vector<T> extract(V v)
        {
        // this method is never called
        throw new UnsupportedOperationException();
        }

    @Override
    public MapIndex<K, V, Vector<T>> createIndex(boolean b, Comparator comparator, Map<ValueExtractor<V, Vector<T>>, MapIndex> map, BackingMapContext backingMapContext)
        {
        BinaryQuantMapIndex mapIndex = new BinaryQuantMapIndex(backingMapContext);
        map.put(m_extractor, mapIndex);
        return mapIndex;
        }

    @SuppressWarnings("unchecked")
    @Override
    public MapIndex<K, V, Vector<T>> destroyIndex(Map<ValueExtractor<V, Vector<T>>, MapIndex> map)
        {
        return map.remove(m_extractor);
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        if (!super.equals(o))
            {
            return false;
            }
        BinaryQuantIndex<?, ?, ?> that = (BinaryQuantIndex<?, ?, ?>) o;
        return Objects.equals(m_extractor, that.m_extractor);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(super.hashCode(), m_extractor);
        }

    @Override
    public String toString()
        {
        return "BinaryQuantIndex{" +
               "extractor=" + m_extractor +
               '}';
        }

    @Override
    public int getImplVersion()
        {
        return POF_IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_extractor           = in.readObject(0);
        m_nOversamplingFactor = in.readInt(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_extractor);
        out.writeInt(1, m_nOversamplingFactor);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_extractor           = ExternalizableHelper.readObject(in);
        m_nOversamplingFactor = in.readInt();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_extractor);
        out.writeInt(m_nOversamplingFactor);
        }

    // ----- inner class: BinaryQuantMapIndex -------------------------------

    /**
     * A Binary Quantization {@link VectorIndex}.
     */
    @SuppressWarnings("unchecked")
    public class BinaryQuantMapIndex
            implements VectorIndex<K, V, Vector<T>>
        {
        /**
         * Create a {@link BinaryQuantMapIndex}.
         *
         * @param ctx  the cache {@link BackingMapContext}
         */
        private BinaryQuantMapIndex(BackingMapContext ctx)
            {
            f_backingMapContext = ctx;
            }

        @Override
        public ValueExtractor<V, Vector<T>> getValueExtractor()
            {
            return m_extractor;
            }

        @Override
        public boolean isOrdered()
            {
            return false;
            }

        @Override
        public boolean isPartial()
            {
            return false;
            }

        @Override
        public Map<Vector<T>, Set<K>> getIndexContents()
            {
            return NullImplementation.getMap();
            }

        @Override
        public Object get(K k)
            {
            return f_mapIndex.get(k);
            }

        @Override
        public Comparator<Vector<T>> getComparator()
            {
            return null;
            }

        @Override
        public void insert(Map.Entry<? extends K, ? extends V> entry)
            {
            Vector<?> v = InvocableMapHelper.extractFromEntry(m_extractor, entry);
            if (v != null)
                {
                Object oKey = entry instanceof BinaryEntry
                              ? ((BinaryEntry<?, ?>) entry).getBinaryKey()
                              : entry.getKey();
                f_mapIndex.put((K) oKey, v.binaryQuant().get());
                }
            }

        @Override
        public void update(Map.Entry<? extends K, ? extends V> entry)
            {
            Vector<?> v = InvocableMapHelper.extractFromEntry(m_extractor, entry);
            if (v != null)
                {
                Object oKey = entry instanceof BinaryEntry
                              ? ((BinaryEntry<?, ?>) entry).getBinaryKey()
                              : entry.getKey();
                f_mapIndex.put((K) oKey, v.binaryQuant().get());
                }
            else
                {
                delete(entry);
                }
            }

        @Override
        public void delete(Map.Entry<? extends K, ? extends V> entry)
            {
            Object oKey = entry instanceof BinaryEntry
                          ? ((BinaryEntry<?, ?>) entry).getBinaryKey()
                          : entry.getKey();
            f_mapIndex.remove((K) oKey);
            }

        @Override
        public BinaryQueryResult[] query(Vector<T> vector, int k, Filter<?> filter)
            {
            BitSet                           bitSet       = Objects.requireNonNull(vector).binaryQuant().get();
            Int2ObjectSortedMap<Set<Binary>> mapDistances = new Int2ObjectAVLTreeMap<>();

            for (Map.Entry<K, BitSet> entry : f_mapIndex.entrySet())
                {
                int d = Vectors.hammingDistance(bitSet, entry.getValue());
                Set<Binary> setKeys = mapDistances.get(d);
                if (setKeys == null)
                    {
                    setKeys = new InflatableSet();
                    mapDistances.put(d, setKeys);
                    }
                setKeys.add((Binary) entry.getKey());
                }

            int                 cAdded   = 0;
            int                 cResults = Math.min(k * m_nOversamplingFactor, f_mapIndex.size());
            BinaryQueryResult[] aResults = new BinaryQueryResult[cResults];

            for (IntIterator it = mapDistances.keySet().intIterator(); it.hasNext() && cAdded < cResults; )
                {
                int         d       = it.nextInt();
                Set<Binary> setKeys = mapDistances.get(d);

                for (Binary binKey : setKeys)
                    {
                    BinaryEntry<K, V> entry = f_backingMapContext.getReadOnlyEntry(binKey).asBinaryEntry();
                    if (filter == null || InvocableMapHelper.evaluateEntry(filter, entry))
                        {
                        Binary binValue = entry.getBinaryValue();
                        aResults[cAdded++] = new BinaryQueryResult(d, binKey, binValue);
                        if (cAdded == cResults)
                            {
                            break;
                            }
                        }
                    }
                }

            return cAdded == cResults
                   ? aResults
                   : Arrays.copyOfRange(aResults, 0, cAdded);
            }

        // ----- data members -----------------------------------------------

        /**
         * The cache {@link BackingMapContext}.
         */
        private final BackingMapContext f_backingMapContext;

        /**
         * The index of cache keys to bit vectors.
         */
        private final ConcurrentMap<K, BitSet> f_mapIndex = new ConcurrentHashMap<>();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The POF implementation version.
     */
    public static final int POF_IMPL_VERSION = 0;

    /**
     * The {@link ValueExtractor} to use to extract the {@link Vector}.
     */
    private ValueExtractor<V, Vector<T>> m_extractor;

    /**
     * The oversampling factor to use.
     */
    private int m_nOversamplingFactor = 3;
    }
