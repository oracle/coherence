/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.search;

import com.oracle.coherence.ai.DistanceAlgorithm;
import com.oracle.coherence.ai.QueryResult;
import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.VectorIndex;
import com.oracle.coherence.ai.distance.CosineDistance;
import com.oracle.coherence.ai.index.BinaryQuantIndex;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.StreamingAggregator;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.SortedBag;
import com.tangosol.util.Streamer;
import com.tangosol.util.ValueExtractor;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An {@link StreamingAggregator} to execute a similarity query.
 *
 * @param <K>  the type of the cache key
 * @param <V>  the type of the cache value
 * @param <T>  the type of the vector
 */
@SuppressWarnings({"unused", "rawtypes"})
public class SimilaritySearch<K, V, T>
        implements StreamingAggregator<K, V, List<BinaryQueryResult>, List<QueryResult<K, V>>>,
                   ExternalizableLite, PortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public SimilaritySearch()
        {
        }

    /**
     * Create a {@link SimilaritySearch} aggregator that will use cosine distance to calculate and return
     * up to {@code maxResults} results that are closest to the specified {@code vector}.
     * <p/>
     * To use a different distance algorithm, provide it via {@link #algorithm(DistanceAlgorithm)} method.
     * <p/>
     * You can also specify a filter criteria using {@link #filter(Filter)} method, and force the aggregator
     * to perform a brute force calculation by calling {@link #bruteForce()} method. The latter is useful for
     * testing, as it will ignore any available indexes, which allows you to compare the results of an
     * index-based query against the exact matches returned by the brute force search to verify that the recall
     * is where you need it to be, and tune index parameters if it isn't.
     *
     * @param extractor   the {@link ValueExtractor} to extract the vector from the cache value
     * @param vector      the vector to calculate similarity with
     * @param maxResults  the maximum number of results to return
     */
    public SimilaritySearch(ValueExtractor<V, Vector<T>> extractor, Vector<T> vector, int maxResults)
        {
        m_extractor   = ValueExtractor.of(Objects.requireNonNull(extractor));
        m_vector      = Objects.requireNonNull(vector);
        m_nMaxResults = maxResults;
        }

    private SimilaritySearch(ValueExtractor<? super V, ? extends Vector<T>> extractor, Vector<T> vector, DistanceAlgorithm<T> algorithm, int nMaxResults, Filter<?> filter, boolean fBruteForce)
        {
        m_extractor   = extractor;
        m_vector      = vector;
        m_algorithm   = algorithm;
        m_nMaxResults = nMaxResults;
        m_filter      = filter;
        m_fBruteForce = fBruteForce;
        }

    /**
     * Set the {@link DistanceAlgorithm algorithm} to use for distance calculation between vectors.
     *
     * @param algorithm  the distance algorithm to use
     *
     * @return this instance
     */
    public SimilaritySearch<K, V, T> algorithm(DistanceAlgorithm<T> algorithm)
        {
        m_algorithm = algorithm;
        return this;
        }

    /**
     * Force brute force search, ignoring any available indexes.
     *
     * @return this instance
     */
    public SimilaritySearch<K, V, T> bruteForce()
        {
        m_fBruteForce = true;
        return this;
        }

    /**
     * Set the {@link Filter filter} to use to limit the set of entries to search.
     *
     * @param filter  the filter to use
     *
     * @return this instance
     */
    public SimilaritySearch<K, V, T> filter(Filter<?> filter)
        {
        m_filter = filter;
        return this;
        }

    public ValueExtractor<? super V, ? extends Vector<T>> getExtractor()
        {
        return m_extractor;
        }

    public Vector<T> getVector()
        {
        return m_vector;
        }

    public DistanceAlgorithm<T> getAlgorithm()
        {
        return m_algorithm;
        }

    public int getMaxResults()
        {
        return m_nMaxResults;
        }

    public boolean isBruteForce()
        {
        return m_fBruteForce;
        }

    public Filter<?> getFilter()
        {
        return m_filter;
        }

    @Override
    public int characteristics()
        {
        if (m_fBruteForce)
            {
            return PARALLEL | ALLOW_INCONSISTENCIES;
            }
        return PARALLEL | BY_PARTITION | ALLOW_INCONSISTENCIES;
        }

    @Override
    public StreamingAggregator<K, V, List<BinaryQueryResult>, List<QueryResult<K, V>>> supply()
        {
        return new SimilaritySearch<>(m_extractor, m_vector, m_algorithm, m_nMaxResults, m_filter, m_fBruteForce);
        }

    @Override
    public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
        {
        if (m_fBruteForce)
            {
            // we are not using the index, so brute force by iterating over all the entries
            return bruteForce(streamer, null);
            }
        
        // As we have BY_PARTITION characteristics, the streamer should contain entries from a single partition.
        // This allows us to just look at a single entry to get the partition and then execute the query on
        // the index for just that partition.
        if (streamer.hasNext())
            {
            InvocableMap.Entry<? extends K, ? extends V> entry = streamer.next();
            if (!searchPartition(entry.asBinaryEntry(), m_vector))
                {
                return bruteForce(streamer, entry);
                }
            }
        return false; // we return false because we have done everything, we do not need to iterate over entries
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        if (m_filter == null || InvocableMapHelper.evaluateEntry(m_filter, entry))
            {
            BinaryEntry<?, ?>             binaryEntry = entry.asBinaryEntry();
            Binary                        binaryKey   = binaryEntry.getBinaryKey();
            Map<ValueExtractor, MapIndex> mapIndex    = binaryEntry.getIndexMap();
            MapIndex                      index       = mapIndex.get(m_extractor);
            Vector<T> vector = index instanceof VectorIndex
                               ? InvocableMapHelper.extractFromEntry(m_extractor, entry)
                               : entry.extract(m_extractor);

            if (vector == null)
                {
                return true;
                }

            double distance = m_algorithm.distance(m_vector, vector);
            BinaryQueryResult result = new BinaryQueryResult(distance, binaryKey, binaryEntry.getBinaryValue());

            m_results.add(result);
            if (m_results.size() > m_nMaxResults)
                {
                m_results.removeLast();
                }
            }
        
        return true;
        }

    @Override
    public boolean combine(List<BinaryQueryResult> partialResult)
        {
        Iterator<BinaryQueryResult> it   = partialResult.iterator();
        int                         size = m_results.size();

        while (size < m_nMaxResults && it.hasNext())
            {
            m_results.add(it.next());
            size++;
            }

        while (it.hasNext())
            {
            m_results.add(it.next());
            m_results.removeLast();
            }
        return true;
        }

    @Override
    public List<BinaryQueryResult> getPartialResult()
        {
        return new ArrayList<>(m_results);
        }

    @Override
    public List<QueryResult<K, V>> finalizeResult()
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public List<QueryResult<K, V>> finalizeResult(Converter<Binary, ?> converterBin)
        {
        Converter<BinaryQueryResult, QueryResult<K, V>> convUp = r -> new ConverterResult<>(r, converterBin);
        return ConverterCollections.getList(new ArrayList<>(m_results), convUp, x -> null);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_extractor   = in.readObject(0);
        m_vector      = in.readObject(1);
        m_algorithm   = in.readObject(2);
        m_nMaxResults = in.readInt(3);
        m_filter      = in.readObject(4);
        m_fBruteForce = in.readBoolean(5);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_extractor);
        out.writeObject(1, m_vector);
        out.writeObject(2, m_algorithm);
        out.writeInt(3, m_nMaxResults);
        out.writeObject(4, m_filter);
        out.writeBoolean(5, m_fBruteForce);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_extractor   = ExternalizableHelper.readObject(in);
        m_vector      = ExternalizableHelper.readObject(in);
        m_algorithm   = ExternalizableHelper.readObject(in);
        m_nMaxResults = in.readInt();
        m_filter      = ExternalizableHelper.readObject(in);
        m_fBruteForce = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_extractor);
        ExternalizableHelper.writeObject(out, m_vector);
        ExternalizableHelper.writeObject(out, m_algorithm);
        out.writeInt(m_nMaxResults);
        ExternalizableHelper.writeObject(out, m_filter);
        out.writeBoolean(m_fBruteForce);
        }

    // ----- helper methods -------------------------------------------------

    protected boolean bruteForce(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer,
            InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        if (entry != null)
            {
            accumulate(entry);
            }

        while (streamer.hasNext())
            {
            accumulate(streamer.next());
            }

        return true;
        }

    /**
     * If a {@link VectorIndex} exists for the specified partition, then use it to
     * perform the KNN search.
     *
     * @param binaryEntry  the {@link BinaryEntry} to use to identify the partition
     * @param vector       the target vector to find the nearest neighbours to
     *
     * @return  {@code true} if a {@link VectorIndex} was present and used for the search
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected boolean searchPartition(BinaryEntry binaryEntry, Vector<T> vector)
        {
        Map<ValueExtractor, MapIndex> mapIndex = binaryEntry.getIndexMap();
        MapIndex                      index    = mapIndex.get(m_extractor);

        if (index instanceof VectorIndex)
            {
            BinaryQueryResult[] results = ((VectorIndex) index).query(vector, m_nMaxResults, m_filter);
            boolean             fRemove = false;
            double              nBottom = m_results.isEmpty() ? Float.MAX_VALUE : m_results.last().getDistance();

            for (BinaryQueryResult result : results)
                {
                if (index instanceof BinaryQuantIndex.BinaryQuantMapIndex)
                    {
                    // we need to replace Hamming distances with the actual distance before processing results
                    BackingMapContext ctx   = binaryEntry.getBackingMapContext();
                    Map.Entry         entry = ctx.getReadOnlyEntry(result.getKey());

                    result.setDistance(m_algorithm.distance(m_vector, InvocableMapHelper.extractFromEntry(m_extractor, entry)));
                    }

                double nScore = result.getDistance();
                if (nScore < nBottom)
                    {
                    m_results.add(result);
                    if (fRemove || m_results.size() > m_nMaxResults)
                        {
                        fRemove = true;
                        m_results.removeLast();
                        nBottom = m_results.last().getDistance();
                        }
                    }
                }
            return true;
            }

        return false;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ValueExtractor} to extract the vector from the entry value.
     */
    @JsonbProperty("extractor")
    protected ValueExtractor<? super V, ? extends Vector<T>> m_extractor;

    /**
     * The {@link Vector} to extract the vector from the entry value.
     */
    @JsonbProperty("vector")
    protected Vector<T> m_vector;

    /**
     * The {@link DistanceAlgorithm} to execute.
     */
    @JsonbProperty("algorithm")
    protected DistanceAlgorithm<T> m_algorithm = new CosineDistance<>();

    /**
     * The maximum number of results to return.
     */
    @JsonbProperty("maxResults")
    protected int m_nMaxResults;

    /**
     * A flag indicating whether to ignore any {@link VectorIndex} that may be
     * present and just use the algorithm directly (i.e. use a brute force calculation)
     */
    @JsonbProperty("bruteForce")
    protected boolean m_fBruteForce;

    /**
     * An optional {@link Filter} to use to filter the search results.
     */
    @JsonbProperty("filter")
    protected Filter<?> m_filter;

    /**
     * The interim results for the aggregator.
     */
    @JsonbTransient
    protected final transient SortedBag<BinaryQueryResult> m_results = new SortedBag<>(Comparator.naturalOrder());
    }
