/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.lucene;

import com.oracle.coherence.ai.QueryResult;
import com.oracle.coherence.ai.search.BinaryQueryResult;
import com.oracle.coherence.ai.search.ConverterResult;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.Streamer;
import com.tangosol.util.ValueExtractor;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;

/**
 * A Coherence aggregator that performs full-text search across distributed caches using Lucene.
 * This class executes Lucene queries in parallel across cache partitions and combines the results
 * with proper score normalization.
 *
 * @param <K> the type of cache entry keys
 * @param <V> the type of cache entry values
 *
 * @author Aleks Seovic  2025.05.16
 * @since 25.09
 */
public class LuceneSearch<K, V>
        implements InvocableMap.StreamingAggregator<K, V, LuceneSearch.PartialResult, List<QueryResult<K, V>>>,
                   ExternalizableLite, PortableObject
    {
    /**
     * Default constructor required for serialization.
     */
    public LuceneSearch()
        {
        }

    /**
     * Constructs a new LuceneSearch instance.
     *
     * @param extractor   the extractor to use to obtain searchable text from cache values
     * @param query       the Lucene query to execute
     * @param nMaxResults the maximum number of results to return
     */
    public LuceneSearch(ValueExtractor<? super V, String> extractor, Query query, int nMaxResults)
        {
        this(extractor, query, nMaxResults, null);
        }

    /**
     * Private constructor for internal use.
     *
     * @param extractor    the extractor to use
     * @param query       the query to execute
     * @param nMaxResults the maximum number of results
     * @param filter      optional filter to apply
     */
    private LuceneSearch(ValueExtractor<? super V, String> extractor, Query query, int nMaxResults, Filter<?> filter)
        {
        m_extractor = ValueExtractor.of(extractor);
        m_query = query;
        m_nMaxResults = nMaxResults;
        m_filter = filter;
        }

    // ---- fluent API ------------------------------------------------------

    /**
     * Set the {@link Filter filter} to use to limit the set of entries to search.
     *
     * @param filter  the filter to use
     *
     * @return this instance
     */
    public LuceneSearch<K, V> filter(Filter<?> filter)
        {
        m_filter = filter;
        return this;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Returns the value extractor used by this search.
     *
     * @return the value extractor
     */
    public ValueExtractor<? super V, String> getExtractor()
        {
        return m_extractor;
        }

    /**
     * Returns the maximum number of results to return.
     *
     * @return the maximum number of results
     */
    public int getMaxResults()
        {
        return m_nMaxResults;
        }

    /**
     * Returns the filter used to limit the search results.
     *
     * @return the filter, or null if none is set
     */
    public Filter<?> getFilter()
        {
        return m_filter;
        }

    /**
     * Returns the Lucene {@code Query} to execute.
     * 
     * @return the Lucene {@code Query} to execute
     */
    public Query getQuery()
        {
        return m_query;
        }

    // ---- StreamingAggregator interface -----------------------------------

    /**
     * Returns the characteristics of this aggregator.
     * This implementation supports parallel execution, partition-level
     * processing, and allows for inconsistencies during execution.
     *
     * @return the characteristics bit mask
     */
    @Override
    public int characteristics()
        {
        return PARALLEL | BY_PARTITION | ALLOW_INCONSISTENCIES;
        }

    /**
     * Creates a new instance of this aggregator for parallel execution.
     *
     * @return a new LuceneSearch instance with the same configuration
     */
    @Override
    public InvocableMap.StreamingAggregator<K, V, PartialResult, List<QueryResult<K, V>>> supply()
        {
        return new LuceneSearch<>(m_extractor, m_query, m_nMaxResults, m_filter);
        }

    /**
     * Processes a stream of entries from a single partition.
     * This method executes the Lucene query against the partition's local index
     * and collects matching entries.
     *
     * @param streamer the streamer providing access to partition entries
     *
     * @return false to indicate that streaming can stop after processing the first entry
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
        {
        // As we have BY_PARTITION characteristic, the streamer should contain entries from a single partition.
        // This allows us to just look at a single entry to get the partition and then execute the query on
        // the index for just that partition.
        if (streamer.hasNext())
            {
            BinaryEntry<? extends K, ? extends V> binEntry = streamer.next().asBinaryEntry();
            BackingMapContext ctx = binEntry.getBackingMapContext();

            MapIndex<K, V, String> mapIndex = binEntry.getIndexMap().get(m_extractor);
            if (mapIndex == null)
                {
                throw new IllegalStateException("LuceneIndex for the extractor %s does not exist, and needs to be added.".formatted(m_extractor.getCanonicalName()));
                }

            // we want to collect binary keys and values that satisfy the query, but don't really care
            // about partition-level scores, as we'll re-execute the query and obtain globally scored
            // results within finalizeResults below
            if (mapIndex instanceof LuceneIndex<K, V>.LuceneMapIndex idx)
                {
                // get config from the index, so we can include it within the partial result
                m_config = idx.config();

                // perform search and post-process results
                Map<Binary, Float> mapResults = idx.search(m_query, m_nMaxResults);
                for (Binary binKey : mapResults.keySet())
                    {
                    BinaryEntry<? extends K, ? extends V> e = (BinaryEntry<? extends K, ? extends V>) ctx.getReadOnlyEntry(binKey);
                    if (m_filter == null || InvocableMapHelper.evaluateEntry(m_filter, e))
                        {
                        m_mapResults.put(binKey, e.getBinaryValue());
                        }
                    }
                }
            else
                {
                throw new IllegalStateException("Index for the extractor %s is not LuceneIndex. Full-text search is not supported".formatted(m_extractor.getCanonicalName()));
                }
            }
        return false; // we return false because we have done everything, we do not need to iterate over entries
        }

    /**
     * Not supported by this implementation.
     *
     * @throws UnsupportedOperationException always
     */
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * Combines partial results from multiple partitions.
     *
     * @param partialResult the partial result to combine
     *
     * @return true to continue processing
     */
    public boolean combine(PartialResult partialResult)
        {
        if (partialResult != null)
            {
            m_config = partialResult.config();
            m_mapResults.putAll(partialResult.results());
            }
        return true;
        }

    /**
     * Returns the partial results collected so far.
     *
     * @return map of binary keys to binary values
     */
    public PartialResult getPartialResult()
        {
        return m_config == null ? null : new PartialResult(m_config, m_mapResults);
        }

    /**
     * Not supported by this implementation.
     *
     * @throws UnsupportedOperationException always
     */
    public List<QueryResult<K, V>> finalizeResult()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * Finalizes the search results by re-ranking all matches and normalizing scores.
     * <p>
     * This method creates a temporary Lucene index containing all matches from all
     * partitions, re-executes the query to get globally normalized scores, and
     * returns the top N results.
     *
     * @param converterBin the converter to use for binary-to-object conversion
     *
     * @return list of query results with normalized scores
     */
    @SuppressWarnings({"unchecked", "EnhancedSwitchMigration"})
    @Override
    public List<QueryResult<K, V>> finalizeResult(Converter<Binary, ?> converterBin)
        {
        if (m_mapResults.isEmpty())
            {
            return Collections.emptyList();
            }
        
        try (Directory directory = new ByteBuffersDirectory())
            {
            Analyzer analyzer = m_config.analyzerSupplier().get();

            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            m_config.writerConfigurer().accept(config);

            try (IndexWriter writer = new IndexWriter(directory, config))
                {
                for (Map.Entry<Binary, Binary> e : m_mapResults.entrySet())
                    {
                    V value = (V) converterBin.convert(e.getValue());
                    BytesRef bytesKey = new BytesRef(e.getKey().toByteArray());

                    Document doc = new Document();
                    doc.add(new StoredField("key", bytesKey));
                    doc.add(new TextField(m_extractor.getCanonicalName(), m_extractor.extract(value), Field.Store.NO));
                    writer.addDocument(doc);
                    }
                writer.commit();
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }

            List<QueryResult<K, V>> listResults = new ArrayList<>(m_nMaxResults);
            try (DirectoryReader reader = DirectoryReader.open(directory))
                {
                IndexSearcher searcher = m_config.searcherSupplier().apply(reader, null);

                TopDocs topDocs = searcher.search(m_query, m_nMaxResults);

                switch (topDocs.scoreDocs.length)
                    {
                    case 0:
                        // optimization for edge case where no document is returned
                        break;

                    case 1:
                        {
                        // optimization for edge case where only one document is returned

                        ScoreDoc sd = topDocs.scoreDocs[0];
                        Document doc = searcher.storedFields().document(sd.doc);
                        addDocument(listResults, doc, 1.0f, converterBin);
                        break;
                        }
                    default:
                        {
                        // normalize all BM25 scores using sigmoid function

                        // Step 1: Find min and max scores
                        float min = Float.MAX_VALUE;
                        float max = Float.MIN_VALUE;

                        for (ScoreDoc sd : topDocs.scoreDocs)
                            {
                            min = Math.min(min, sd.score);
                            max = Math.max(max, sd.score);
                            }

                        boolean fNormalize = min < max;

                        // Step 2: Compute shift and alpha
                        float shift = (min + max) / 2f;
                        float alpha = Math.min(5.0f, 2.944f / Math.max(1e-3f, max - shift));

                        // Step 3: Apply sigmoid normalization and collect results
                        for (ScoreDoc sd : topDocs.scoreDocs)
                            {
                            Document doc   = searcher.storedFields().document(sd.doc);
                            float    score = fNormalize ? sigmoid(sd, alpha, shift) : 1.0f;

                            addDocument(listResults, doc, score, converterBin);
                            }
                        }
                    }
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }

            return listResults;
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Applies a sigmoid normalization to the Lucene BM25 score to map it to [0, 1].
     *
     * @param sd    the Lucene ScoreDoc
     * @param alpha the steepness parameter for the sigmoid
     * @param shift the center shift for the sigmoid
     *              
     * @return the normalized score in [0, 1]
     */
    private static float sigmoid(ScoreDoc sd, float alpha, float shift)
        {
        return 1f / (1f + (float) Math.exp(-alpha * (sd.score - shift)));
        }

    /**
     * Adds a QueryResult to the result list, extracting the key from the Lucene document
     * and looking up the original value from the results map.
     *
     * @param listResults   the list to add to
     * @param doc           the Lucene document
     * @param score         the normalized score
     * @param converterBin  the converter for binary values
     *
     * @throws IOException if document extraction fails
     */
    private void addDocument(List<QueryResult<K, V>> listResults, Document doc, float score, Converter<Binary, ?> converterBin)
            throws IOException
        {
        BytesRef bytesKey = doc.getBinaryValue("key");
        Binary   binKey   = new Binary(bytesKey.bytes);

        listResults.add(createResult(converterBin, binKey, m_mapResults.get(binKey), score));
        }

    /**
     * Creates a QueryResult instance from binary key, value, and score.
     * Helper method used during result finalization to wrap binary data
     * with appropriate converters.
     *
     * @param converterBin the converter for binary data
     * @param binKey       the binary key
     * @param binValue     the binary value
     * @param score       the normalized score (0.0-1.0)
     *
     * @return a new QueryResult instance
     */
    private QueryResult<K, V> createResult(Converter<Binary, ?> converterBin, Binary binKey, Binary binValue, float score)
        {
        return new ConverterResult<>(new BinaryQueryResult(score, binKey, binValue), converterBin);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_extractor   = in.readObject(0);
        m_query       = in.readObject(1);
        m_nMaxResults = in.readInt(2);
        m_filter      = in.readObject(3);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_extractor);
        out.writeObject(1, m_query);
        out.writeInt(2, m_nMaxResults);
        out.writeObject(3, m_filter);
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        throw new IOException("FullTextSearch requires POF serialization");
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        throw new IOException("FullTextSearch requires POF serialization");
        }

    // ---- inner class: PartialResult --------------------------------------

    /**
     * PartialResult encapsulates the results and configuration from a single partition
     * during distributed Lucene search aggregation. It carries both the map of matching
     * entries and the Lucene index configuration (analyzer, searcher, writer config, etc.)
     * used by the local index, ensuring that global result finalization can use the
     * correct settings even on storage-disabled members.
     */
    public static class PartialResult
            implements PortableObject
        {
        /**
         * Deserialization constructor.
         */
        @SuppressWarnings("unused")
        public PartialResult()
            {
            }

        /**
         * Constructs a PartialResult with the given config and results.
         *
         * @param config     the Lucene index configuration
         * @param mapResults the map of matching entries
         */
        public PartialResult(LuceneIndex.Config config, Map<Binary, Binary> mapResults)
            {
            m_config = config;
            m_mapResults = mapResults;
            }

        // ---- accessors ---------------------------------------------------
        
        /**
         * Returns the Lucene index configuration for this partial result.
         *
         * @return the LuceneIndex.Config instance
         */
        public LuceneIndex.Config config()
            {
            return m_config;
            }

        /**
         * Returns the map of binary keys to binary values for this partial
         * result.
         *
         * @return the map of results
         */
        public Map<Binary, Binary> results()
            {
            return m_mapResults;
            }

        // ---- PortableObject interface ------------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_config = in.readObject(0);
            m_mapResults = in.readMap(1, new HashMap<>());
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_config);
            out.writeMap(1, m_mapResults);
            }

        // ---- data members ------------------------------------------------

        /**
         * The Lucene index configuration used by the local partition.
         */
        private LuceneIndex.Config m_config;

        /**
         * The map of binary keys to binary values representing matching entries
         * found in this partition.
         */
        private Map<Binary, Binary> m_mapResults;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The {@link ValueExtractor} that should be used to extract the
     * text to search from the entry value.
     * This extractor is used to obtain the text content that will be searched
     * during query execution.
     */
    protected ValueExtractor<? super V, String> m_extractor;

    /**
     * The Lucene {@code Query} to execute.
     * This query is executed against each partition's local index and then
     * re-executed during the final global ranking phase.
     */
    protected Query m_query;

    /**
     * The maximum number of results to return.
     * This limit is applied both at the partition level and to the final
     * merged result set.
     */
    protected int m_nMaxResults;

    /**
     * An optional {@link Filter} to use to filter the search results.
     * When specified, this filter is applied to entries before they are
     * included in the result set.
     */
    protected Filter<?> m_filter;

    /**
     * The interim results for the aggregator.
     * This map holds the binary keys and values of matching entries during
     * the aggregation phase. It is transient as it is not part of the
     * serialized state.
     */
    protected final transient Map<Binary, Binary> m_mapResults = new HashMap<>();

    /**
     * The Lucene index configuration used for global result finalization.
     * This is set from the first partial result during aggregation.
     */
    protected transient LuceneIndex.Config m_config;
    }
