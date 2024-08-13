/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.hnsw;

import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.VectorIndex;
import com.oracle.coherence.ai.VectorIndexExtractor;
import com.oracle.coherence.ai.search.BinaryQueryResult;
import com.oracle.coherence.hnswlib.Hnswlib.QueryFilter;
import com.oracle.coherence.hnswlib.Index;
import com.oracle.coherence.hnswlib.QueryTuple;
import com.oracle.coherence.hnswlib.SpaceName;
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
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jakarta.json.bind.annotation.JsonbProperty;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An HNSW index implementation.
 * <p/>
 * This implementation provides a thin wrapper around native
 * <a href="https://github.com/nmslib/hnswlib">hnswlib</a> library, which is
 * ultimately responsible for indexing and similarity searches.
 * <p/>
 * It supports indexing of any {@code Vector<float[]>} property, and uses cosine
 * distance for similarity searches by default. Just like the underlying {@code hnswlib}
 * library, it assumes that both the indexed and search vectors are normalized
 * ahead of time when using cosine distance.
 * <p/>
 * The created native index has the initial size of 4,096 elements, but will be
 * automatically resized as necessary. To avoid or reduce the resizing, you can
 * change the initial size by calling {@link #setMaxElements(int)} before registering
 * index with a cache.
 * <p/>
 * To create an index, you need to provide a {@code ValueExtractor} that can be used
 * to extract a {@code Vector<float[]>} property from an entry, as well as the
 * expected vector dimension. You can optionally specify algorithm parameters such
 * as {@code spaceName}, {@code efConstr}, {@code efSearch}, {@code M}, etc. via the
 * fluent setters on a constructed {@code HnswIndex} instance, before registering
 * index with a cache.
 * <p/>
 * For example:
 * <pre>
 * var idx = new HnswIndex<>(ValueWithVector::getVector, DIMENSIONS)
 *                  .setSpaceName("L2")
 *                  .setMaxElements(100_000)
 *                  .setEfConstr(100)
 *                  .setM(30);
 *
 * NamedMap<Integer, ValueWithVector> vectors = session.getMap("vectors");
 * vectors.addIndex(idx);
 * </pre>
 *
 * @param <K>  the type of entry keys
 * @param <V>  the type of entry values
 *
 * @author Aleks Seovic  2024.07.20
 * @since 24.09
 */
public class HnswIndex<K, V>
        extends AbstractEvolvable
        implements VectorIndexExtractor<V, float[]>, ExternalizableLite, EvolvablePortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor for serialization.
     */
    public HnswIndex()
        {
        }

    /**
     * Create a {@link HnswIndex} using the {@link #DEFAULT_SPACE_NAME default space name}.
     *
     * @param extractor   the {@link ValueExtractor} to use to extract the float
     *                    array {@link Vector} from the cache entry
     * @param nDimension  the number of dimensions in the vector
     */
    public HnswIndex(ValueExtractor<V, Vector<float[]>> extractor, int nDimension)
        {
        m_extractor  = ValueExtractor.of(Objects.requireNonNull(extractor));
        m_nDimension = nDimension;
        }

    /**
     * Create a {@link HnswIndex}.
     *
     * @param extractor   the {@link ValueExtractor} to use to extract the float
     *                    array {@link Vector} from the cache entry
     * @param sSpaceName  the index space name to use
     * @param nDimension  the number of dimensions in the vector
     */
    public HnswIndex(ValueExtractor<V, Vector<float[]>> extractor, String sSpaceName, int nDimension)
        {
        this(extractor, nDimension);

        m_sSpaceName = sSpaceName == null || sSpaceName.isBlank() ? "" : sSpaceName;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the index space name.
     *
     * @return the index space name
     */
    public String getSpaceName()
        {
        return m_sSpaceName;
        }

    /**
     * Set the index space name.
     *
     * @param sSpaceName  the index space name
     *
     * @return this {@link HnswIndex} to allow fluent API calls
     */
    public HnswIndex<K, V> setSpaceName(String sSpaceName)
        {
        m_sSpaceName = sSpaceName;
        return this;
        }

    /**
     * Return the {@link ValueExtractor} to use to extract the float
     * array {@link Vector} from the cache entry.
     *
     * @return the {@link ValueExtractor} to use to extract the float
     *         array {@link Vector} from the cache entry.
     */
    public ValueExtractor<V, Vector<float[]>> getExtractor()
        {
        return m_extractor;
        }

    /**
     * Return the number of dimensions in the vectors the index contains.
     *
     * @return the number of dimensions in the vectors the index contains
     */
    public int getDimension()
        {
        return m_nDimension;
        }

    /**
     * Return the maximum number of elements the index can contain.
     *
     * @return the maximum number of elements the index can contain
     */
    public int getMaxElements()
        {
        return m_cMaxElements;
        }

    /**
     * Set the maximum number of elements the index can contain.
     *
     * @param cMaxElements  the maximum number of elements the index can contain
     *
     * @return this {@link HnswIndex} to allow fluent API calls
     */
    public HnswIndex<K, V> setMaxElements(int cMaxElements)
        {
        m_cMaxElements = cMaxElements;
        return this;
        }

    /**
     * Return the number of bidirectional links created for every new element during construction.
     *
     * @return the number of bidirectional links created for every new element during construction
     */
    public int getM()
        {
        return m_nM;
        }

    /**
     * Set the number of bidirectional links created for every new element during construction.
     *
     * @param nM  the number of bidirectional links created for every new element during construction
     *
     * @return this {@link HnswIndex} to allow fluent API calls
     */
    public HnswIndex<K, V> setM(int nM)
        {
        m_nM = nM;
        return this;
        }

    /**
     * Return the ef construction value.
     * This is the parameter has the same meaning as ef, but controls the index_time/index_accuracy.
     *
     * @return the ef construction value
     */
    public int getEfConstr()
        {
        return m_nEfConstr;
        }

    /**
     * Set the ef construction value.
     * This is the parameter has the same meaning as ef, but controls the index_time/index_accuracy.
     *
     * @param nEfConstr  the ef construction value
     *
     * @return this {@link HnswIndex} to allow fluent API calls
     */
    public HnswIndex<K, V> setEfConstruction(int nEfConstr)
        {
        m_nEfConstr = nEfConstr;
        return this;
        }

    /**
     * Return the ef search value.
     * This is the parameter controlling query time/accuracy trade-off.
     *
     * @return the ef search value
     */
    public int getEfSearch()
        {
        return m_nEfSearch;
        }

    /**
     * Set the ef search value.
     * This is the parameter controlling query time/accuracy trade-off.
     *
     * @param nEfSearch  the ef search value
     *
     * @return this {@link HnswIndex} to allow fluent API calls
     */
    public HnswIndex<K, V> setEfSearch(int nEfSearch)
        {
        m_nEfSearch = nEfSearch;
        return this;
        }

    /**
     * Return the random seed used by the index.
     *
     * @return the random seed used by the index
     */
    public int getRandomSeed()
        {
        return m_nRandomSeed;
        }

    /**
     * Set the random seed the index should use.
     *
     * @param nRandomSeed  the random seed the index should use
     *
     * @return this {@link HnswIndex} to allow fluent API calls
     */
    public HnswIndex<K, V> setRandomSeed(int nRandomSeed)
        {
        m_nRandomSeed = nRandomSeed;
        return this;
        }

    // ----- IndexAwareExtractor interface ----------------------------------

    @Override
    public MapIndex<K, V, Vector<float[]>> createIndex(boolean fSorted, Comparator comparator, Map<ValueExtractor<V, Vector<float[]>>, MapIndex> map, BackingMapContext backingMapContext)
        {
        HnswMapIndex hnswMapIndex = new HnswMapIndex(backingMapContext);
        map.put(m_extractor, hnswMapIndex);
        return hnswMapIndex;
        }

    @Override
    @SuppressWarnings("unchecked")
    public MapIndex<K, V, Vector<float[]>> destroyIndex(Map<ValueExtractor<V, Vector<float[]>>, MapIndex> map)
        {
        HnswMapIndex index = (HnswMapIndex) map.remove(m_extractor);
        index.clear();
        return index;
        }

    // ----- ValueExtractor interface ---------------------------------------

    @Override
    public Vector<float[]> extract(V v)
        {
        return m_extractor.extract(v);
        }

    // ----- Evolvable interface --------------------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPL_VERSION;
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_extractor    = in.readObject(0);
        m_nDimension   = in.readInt(1);
        m_sSpaceName   = in.readString(2);
        m_cMaxElements = in.readInt(3);
        m_nM           = in.readInt(4);
        m_nEfConstr    = in.readInt(5);
        m_nEfSearch    = in.readInt(6);
        m_nRandomSeed  = in.readInt(7);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_extractor);
        out.writeInt(1, m_nDimension);
        out.writeString(2, m_sSpaceName);
        out.writeInt(3, m_cMaxElements);
        out.writeInt(4, m_nM);
        out.writeInt(5, m_nEfConstr);
        out.writeInt(6, m_nEfSearch);
        out.writeInt(7, m_nRandomSeed);
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_extractor    = ExternalizableHelper.readObject(in);
        m_nDimension   = ExternalizableHelper.readInt(in);
        m_sSpaceName   = ExternalizableHelper.readSafeUTF(in);
        m_cMaxElements = ExternalizableHelper.readInt(in);
        m_nM           = ExternalizableHelper.readInt(in);
        m_nEfConstr    = ExternalizableHelper.readInt(in);
        m_nEfSearch    = ExternalizableHelper.readInt(in);
        m_nRandomSeed  = ExternalizableHelper.readInt(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_extractor);
        ExternalizableHelper.writeInt(out, m_nDimension);
        ExternalizableHelper.writeUTF(out, m_sSpaceName);
        ExternalizableHelper.writeInt(out, m_cMaxElements);
        ExternalizableHelper.writeInt(out, m_nM);
        ExternalizableHelper.writeInt(out, m_nEfConstr);
        ExternalizableHelper.writeInt(out, m_nEfSearch);
        ExternalizableHelper.writeInt(out, m_nRandomSeed);
        }

    // ----- inner class: HnswMapIndex --------------------------------------

    /**
     * The HNSW {@link MapIndex} and {@link VectorIndex} implementation.
     */
    @SuppressWarnings("rawtypes")
    public class HnswMapIndex
            implements VectorIndex<K, V, Vector<float[]>>, Closeable
        {
        // ----- constructor ------------------------------------------------

        /**
         * Construct {@code HnswMapIndex} instance.
         *
         * @param backingMapContext  the backing map context to use
         */
        public HnswMapIndex(BackingMapContext backingMapContext)
            {
            f_backingMapContext = backingMapContext;
            f_mapLabelsToKeys   = new Int2ObjectOpenHashMap<>(m_cMaxElements);
            f_mapKeysToLabels   = new Object2IntOpenHashMap<>(m_cMaxElements);

            Index index = new Index(SpaceName.valueOf(m_sSpaceName.toUpperCase()), m_nDimension);
            index.initialize(m_cMaxElements, m_nM, m_nEfConstr, m_nRandomSeed, true);
            index.setEf(m_nEfSearch);
            f_index = index;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the number of dimensions in the vectors.
         *
         * @return the number of dimensions in the vectors
         */
        public int getDimensions()
            {
            return m_nDimension;
            }

        // ----- MapIndex interface -----------------------------------------

        @Override
        public ValueExtractor<V, Vector<float[]>> getValueExtractor()
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
        public Map<Vector<float[]>, Set<K>> getIndexContents()
            {
            return null;
            }

        @Override
        public Object get(K k)
            {
            return null;
            }

        @Override
        public Comparator<Vector<float[]>> getComparator()
            {
            return null;
            }

        @Override
        public void insert(Map.Entry<? extends K, ? extends V> entry)
            {
            Vector<float[]> v = InvocableMapHelper.extractFromEntry(m_extractor, entry);
            if (v != null)
                {
                Binary binKey = ((BinaryEntry) entry).getBinaryKey();
                int    nId    = f_idGenerator.incrementAndGet();

                f_lock.writeLock().lock();
                try
                    {
                    f_mapLabelsToKeys.put(nId, binKey);
                    f_mapKeysToLabels.put(binKey, nId);
                    f_index.addItem(v.get(), nId, true);
                    }
                finally
                    {
                    f_lock.writeLock().unlock();
                    }
                }
            }

        @Override
        public void update(Map.Entry<? extends K, ? extends V> entry)
            {
            Vector<float[]> v      = InvocableMapHelper.extractFromEntry(m_extractor, entry);
            Binary          binKey = ((BinaryEntry) entry).getBinaryKey();
            int             nId    = f_mapKeysToLabels.getInt(binKey);

            if (v != null)
                {
                if (nId > 0 && f_index.hasId(nId))
                    {
                    f_lock.writeLock().lock();
                    try
                        {
                        f_index.addItem(v.get(), nId, true);
                        }
                    finally
                        {
                        f_lock.writeLock().unlock();
                        }
                    }
                else
                    {
                    insert(entry);
                    }
                }
            else
                {
                if (nId > 0)
                    {
                    f_lock.writeLock().lock();
                    try
                        {
                        f_mapLabelsToKeys.remove(nId);
                        f_mapKeysToLabels.removeInt(binKey);
                        }
                    finally
                        {
                        f_lock.writeLock().unlock();
                        }
                    }
                }
            }

        @Override
        public void delete(Map.Entry<? extends K, ? extends V> entry)
            {
            Binary binKey = ((BinaryEntry) entry).getBinaryKey();
            int    nId    = f_mapKeysToLabels.getInt(binKey);

            if (nId > 0 && f_index.hasId(nId))
                {
                f_lock.writeLock().lock();
                try
                    {
                    f_index.markDeleted(nId);
                    f_mapLabelsToKeys.remove(nId);
                    f_mapKeysToLabels.removeInt(binKey);
                    }
                finally
                    {
                    f_lock.writeLock().unlock();
                    }
                }
            }

        // ----- VectorIndex interface --------------------------------------

        @Override
        @SuppressWarnings("unchecked")
        public BinaryQueryResult[] query(Vector<float[]> vector, int k, Filter<?> filter)
            {
            f_lock.readLock().lock();
            try
                {
                QueryTuple tuple;
                if (filter == null || filter instanceof AlwaysFilter<?>)
                    {
                    tuple = f_index.knnQuery(vector.get(), k);
                    }
                else
                    {
                    QueryFilter queryFilter = id ->
                        {
                        Binary             binKey = f_mapLabelsToKeys.get(id);
                        InvocableMap.Entry entry  = f_backingMapContext.getReadOnlyEntry(binKey);
                        return InvocableMapHelper.evaluateEntry(filter, entry);
                        };
                    tuple = f_index.knnQuery(vector.get(), k, queryFilter);
                    }

                if (tuple.empty())
                    {
                    return EMPTY_RESULT;
                    }

                int[]               aIds          = tuple.getIds();
                float[]             aCoefficients = tuple.getCoefficients();
                int                 nResult       = tuple.count();
                BinaryQueryResult[] aResults      = new BinaryQueryResult[nResult];
                for (int i = 0; i < nResult; i++)
                    {
                    Binary binKey    = f_mapLabelsToKeys.get(aIds[i]);
                    Binary binValue  = f_backingMapContext.getReadOnlyEntry(binKey).asBinaryEntry().getBinaryValue();
                    float  nDistance = Math.abs(aCoefficients[i]);
                    aResults[i]      = new BinaryQueryResult(nDistance, binKey, binValue);
                    }
                return aResults;
                }
            finally
                {
                f_lock.readLock().unlock();
                }
            }

        // ----- Closeable interface ----------------------------------------

        @Override
        public void close()
            {
            clear();
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Release the native resources held by this index.
         */
        void clear()
            {
            f_lock.writeLock().lock();
            try
                {
                f_index.clear();
                }
            finally
                {
                f_lock.writeLock().unlock();
                }
            }

        // ----- data members -----------------------------------------------

        private final BackingMapContext f_backingMapContext;
        private final Index f_index;
        private final AtomicInteger f_idGenerator = new AtomicInteger();
        private final Int2ObjectMap<Binary> f_mapLabelsToKeys;
        private final Object2IntMap<Binary> f_mapKeysToLabels;
        private final ReadWriteLock f_lock = new ReentrantReadWriteLock();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The POF implementation version.
     */
    public static final int IMPL_VERSION = 0;

    /**
     * An empty query result array.
     */
    private static final BinaryQueryResult[] EMPTY_RESULT = new BinaryQueryResult[0];

    /**
     * The default space name.
     */
    public static final String DEFAULT_SPACE_NAME = "COSINE";

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ValueExtractor} to use to extract the vector from the cache entry.
     */
    @JsonbProperty("extractor")
    private ValueExtractor<V, Vector<float[]>> m_extractor;

    /**
     * The number of dimensions in the vector.
     */
    @JsonbProperty("dimension")
    private int m_nDimension;

    /**
     * The index space name.
     */
    @JsonbProperty("spaceName")
    private String m_sSpaceName = DEFAULT_SPACE_NAME;

    /**
     * The maximum number of elements the index can contain.
     * <p/>
     * The default value is 4096, but the index will grow automatically by
     * doubling its capacity until it reaches approximately 8m elements, at which
     * point it will grow by 50% whenever it gets full.
     */
    @JsonbProperty("maxElements")
    private int m_cMaxElements = 4096;

    /**
     * The number of bidirectional links created for every new element during construction. Reasonable range
     * for M is 2-100. Higher M work better on datasets with high intrinsic dimensionality and/or high recall,
     * while low M work better for datasets with low intrinsic dimensionality and/or low recalls.
     * The parameter also determines the algorithm's memory consumption, which is roughly M * 8-10 bytes per
     * stored element.
     * <p/>
     * As an example for dim=4 random vectors optimal M for search is somewhere around 6, while for high dimensional
     * datasets (word embeddings, good face descriptors), higher M are required (e.g. M=48-64) for optimal performance
     * at high recall.
     * The range M=12-48 is ok for the most of the use cases. When M is changed one has to update the other parameters.
     * Nonetheless, ef and ef_construction parameters can be roughly estimated by assuming that M*ef_{construction}
     * is a constant.
     * <p/>
     * The default value is 16.
     */
    @JsonbProperty("m")
    private int m_nM = 16;

    /**
     * The parameter has the same meaning as ef, which controls the index_time/index_accuracy. Bigger ef_construction
     * leads to longer construction, but better index quality. At some point, increasing ef_construction does not
     * improve the quality of the index. One way to check if the selection of ef_construction was ok is to measure
     * a recall for M nearest neighbor search when ef =ef_construction: if the recall is lower than 0.9, than there
     * is room for improvement.
     * <p/>
     * The default value is 200.
     */
    @JsonbProperty("efConstruction")
    private int m_nEfConstr = 200;

    /**
     * The parameter controlling query time/accuracy trade-off.
     * <p/>
     * The default value is 50.
     */
    @JsonbProperty("efSearch")
    private int m_nEfSearch    = 50;

    /**
     * The random seed used for the index.
     */
    @JsonbProperty("randomSeed")
    private int m_nRandomSeed  = 100;
    }
