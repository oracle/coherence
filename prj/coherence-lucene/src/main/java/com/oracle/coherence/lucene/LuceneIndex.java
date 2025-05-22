/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.lucene;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.MapIndex;
import com.tangosol.util.SimpleMapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.IndexAwareExtractor;
import com.tangosol.util.function.Remote;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LiveIndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

/**
 * A Coherence index implementation that uses Apache Lucene for full-text search capabilities.
 * This implementation maintains separate Lucene indices for each cache partition and
 * supports configurable text analysis, compression, and storage options.
 * <p>
 * The index can be configured using a fluent builder API:
 * <pre>
 * cache.addIndex(new LuceneIndex&lt;&gt;(Document::getText)
 *     .compressionMode(CompressionMode.MAX)
 *     .analyzer(StandardAnalyzer::new)
 *     .directory(partId -> new MMapDirectory(Path.of("index/part-" + partId)))
 *     .enableInverseMap());
 * </pre>
 *
 * @param <K> the type of cache entry keys
 * @param <V> the type of cache entry values
 *
 * @author Aleks Seovic  2025.05.16
 * @since 25.09
 */
@SuppressWarnings({"unused", "rawtypes"})
public class LuceneIndex<K, V>
        extends AbstractEvolvable
        implements IndexAwareExtractor<V, String>, ExternalizableLite, EvolvablePortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public LuceneIndex()
        {
        }

    /**
     * Constructs a new LuceneIndex with the specified value extractor.
     *
     * @param extractor the extractor to use to obtain indexable text from cache values
     */
    public LuceneIndex(ValueExtractor<V, String> extractor)
        {
        m_extractor = ValueExtractor.of(extractor);
        }

    // ---- fluent API ------------------------------------------------------

    /**
     * Sets the flag to enable and maintain a standard Coherence inverse map
     * in addition to the Lucene index, which allows standard Coherence queries
     * and extractors based on the indexed attribute to work efficiently.
     * <p/>
     * Enabling inverse map will consume more memory, so it is recommended to leave
     * it disabled unless you need to query indexed attribute using standard
     * Coherence filters.
     *
     * @return this LuceneIndex instance for method chaining
     */
    public LuceneIndex<K, V> enableInverseMap()
        {
        m_fInverseMap = true;
        return this;
        }

    /**
     * Sets the supplier of Lucene Analyzer to use for this index.
     * The supplier must be serializable and will be used to create
     * Analyzer instances on each storage-enabled member.
     * <p>
     * Example usage:
     * <pre>
     * // Use standard analyzer (default)
     * index.analyzer(StandardAnalyzer::new);
     *
     * // Use custom analyzer
     * index.analyzer(() -> {
     *     CustomAnalyzer analyzer = CustomAnalyzer.builder()
     *         .withTokenizer(StandardTokenizerFactory.class)
     *         .addTokenFilter(LowerCaseFilterFactory.class)
     *         .build();
     *     return analyzer;
     * });
     * </pre>
     *
     * @param analyzerSupplier the supplier of Analyzer to use
     * @return this LuceneIndex instance for method chaining
     */
    public LuceneIndex<K, V> analyzer(Remote.Supplier<Analyzer> analyzerSupplier)
        {
        Objects.requireNonNull(analyzerSupplier);
        m_config.setAnalyzerSupplier(analyzerSupplier);
        return this;
        }

    /**
     * Sets the supplier of Lucene Directory to use for this index.
     * <p>
     * The supplier must return a unique Directory instance for each partition.
     * This allows for custom directory implementations, such as {@link FSDirectory}
     * or {@link MMapDirectory} for disk-based storage or custom off-heap implementations.
     * <p>
     * The supplier must be serializable as it will be sent to storage-enabled
     * members. Each storage-enabled member will create {@link Directory} instances
     * for its local partitions using this supplier.
     * <p>
     * If not set, the default supplier which creates an instance of on-heap
     * {@link ByteBuffersDirectory} will be used.
     *
     * @param directorySupplier the function that creates Directory instances
     *                          based on partition ID
     * @return this LuceneIndex instance for method chaining
     */
    public LuceneIndex<K, V> directory(Remote.Function<Integer, Directory> directorySupplier)
        {
        Objects.requireNonNull(directorySupplier);
        m_config.setDirectorySupplier(directorySupplier);
        return this;
        }

    /**
     * Sets the supplier function for creating custom IndexSearcher instances.
     * <p>
     * This function allows customization of how IndexSearcher instances are created
     * for searching the Lucene index. The function receives both the current and
     * previous IndexReader instances, allowing for optimizations such as index
     * warming or custom caching strategies.
     * <p>
     * Example usage:
     * <pre>
     * // Basic searcher with custom similarity
     * index.searcherSupplier((cur, prev) -> {
     *     IndexSearcher searcher = new IndexSearcher(cur);
     *     searcher.setSimilarity(new BM25Similarity());
     *     return searcher;
     * });
     *
     * // Searcher with index warming
     * index.searcherSupplier((cur, prev) -> {
     *     IndexSearcher searcher = new IndexSearcher(cur);
     *     if (prev != null) {
     *         // Warm new searcher using popular terms from previous reader
     *         searcher.warmUp();
     *     }
     *     return searcher;
     * });
     * </pre>
     * <p>
     * If not set, a default searcher supplier will be used which creates a basic
     * IndexSearcher instance with default settings.
     *
     * @param searcherSupplier the function to create IndexSearcher instances
     *
     * @return this LuceneIndex instance for method chaining
     */
    public LuceneIndex<K, V> searcher(Remote.BiFunction<IndexReader, IndexReader, IndexSearcher> searcherSupplier)
        {
        Objects.requireNonNull(searcherSupplier);
        m_config.setSearcherSupplier(searcherSupplier);
        return this;
        }

    /**
     * Sets a consumer that can be used to customize the {@link IndexWriterConfig} before
     * creating the index. This allows for fine-grained control over all aspects of index
     * writing, including merge policies, RAM buffer size, and other performance settings.
     * <p>
     * Example usage:
     * <pre>
     * // Configure custom merge policy
     * index.configureIndexWriter(config -> {
     *     LogByteSizeMergePolicy policy = new LogByteSizeMergePolicy();
     *     policy.setMinMergeMB(32.0);
     *     policy.setMaxMergeMB(256.0);
     *     policy.setMergeFactor(5);
     *     config.setMergePolicy(policy);
     * });
     *
     * // Configure RAM buffer size and other settings
     * index.configureIndexWriter(config -> {
     *     config.setRAMBufferSizeMB(64.0);
     *     config.setMaxBufferedDocs(10000);
     *     config.setUseCompoundFile(true);
     * });
     * </pre>
     *
     * @param writerConfigurer the consumer that will configure the IndexWriterConfig
     *
     * @return this LuceneIndex instance for method chaining
     *
     * @throws NullPointerException if writerConfigurer is null
     */
    public LuceneIndex<K, V> configureIndexWriter(Remote.Consumer<IndexWriterConfig> writerConfigurer)
        {
        Objects.requireNonNull(writerConfigurer);
        m_config.setWriterConfigurer(writerConfigurer);
        return this;
        }

    // ---- IndexAwareExtractor interface -----------------------------------
    
    /**
     * Creates a new Lucene-based MapIndex instance.
     *
     * @param fOrdered    unused (maintained for compatibility with MapIndex interface)
     * @param comparator  unused (maintained for compatibility with MapIndex interface)
     * @param map         the map of extractors to indices
     * @param ctx         the backing map context
     *
     * @return a new LuceneMapIndex instance
     */
    public MapIndex<K, V, String> createIndex(boolean fOrdered, Comparator comparator, Map<ValueExtractor<V, String>, MapIndex> map, BackingMapContext ctx)
        {
        try
            {
            Analyzer  analyzer  = m_config.analyzerSupplier().get();
            Directory directory = m_config.directorySupplier().apply(COUNTER.getAndIncrement());  // TODO: replace with actual partition ID once available

            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            m_config.writerConfigurer().accept(config);

            IndexWriter    writer = new IndexWriter(directory, config);
            LuceneMapIndex index  = new LuceneMapIndex(writer, ctx);

            map.put(m_extractor, index);

            return index;
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Destroys the Lucene index associated with this extractor.
     *
     * @param map the map of extractors to indices
     *
     * @return the removed index instance
     */
    @SuppressWarnings("unchecked")
    public MapIndex<K, V, String> destroyIndex(Map<ValueExtractor<V, String>, MapIndex> map)
        {
        LuceneMapIndex index = (LuceneMapIndex) map.remove(m_extractor);
        index.clear();
        return index;
        }

    /**
     * Extracts the text to be indexed from the specified value.
     *
     * @param v the value to extract text from
     *
     * @return the extracted text
     */
    public String extract(V v)
        {
        return m_extractor.extract(v);
        }

    // ----- accessors (for serialization testing) --------------------------

    /**
     * Returns the configuration for this index.
     *
     * @return the index configuration
     */
    Config getConfig()
        {
        return m_config;
        }

    /**
     * Returns the value extractor used by this index.
     *
     * @return the value extractor
     */
    ValueExtractor<V, String> getValueExtractor()
        {
        return m_extractor;
        }

    /**
     * Returns whether the inverse map is enabled for this index.
     *
     * @return true if inverse map is enabled, false otherwise
     */
    boolean isInverseMapEnabled()
        {
        return m_fInverseMap;
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
        m_extractor   = in.readObject(0);
        m_fInverseMap = in.readBoolean(1);
        m_config      = in.readObject(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_extractor);
        out.writeBoolean(1, m_fInverseMap);
        out.writeObject(2, m_config);
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        throw new IOException("LuceneIndex requires POF serialization");
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        throw new IOException("LuceneIndex requires POF serialization");
        }

    // ---- inner class: LuceneMapIndex -------------------------------------

    /**
     * A Coherence MapIndex implementation that maintains a Lucene index for full-text search.
     * This inner class handles the actual indexing and searching operations using Lucene's APIs.
     */
    @SuppressWarnings("unchecked")
    public class LuceneMapIndex
            implements MapIndex<K, V, String>, Closeable
        {
        /**
         * Constructs a new LuceneMapIndex with the specified IndexWriter.
         *
         * @param indexWriter the Lucene IndexWriter to use for this index
         *
         * @throws RuntimeException if there is an error initializing the SearcherManager
         */
        public LuceneMapIndex(IndexWriter indexWriter, BackingMapContext ctx)
            {
            try
                {
                f_indexWriter = indexWriter;
                f_simpleIndex = m_fInverseMap
                                ? new SimpleMapIndex(m_extractor, false, null, ctx)
                                : null;

                f_searcherManager = new SearcherManager(f_indexWriter, new SearcherFactory()
                    {
                    public IndexSearcher newSearcher(IndexReader reader, IndexReader previousReader)
                        {
                        return m_config.searcherSupplier().apply(reader, previousReader);
                        }
                    });
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }
            }

        // ---- accessors ---------------------------------------------------

        /**
         * Returns the configuration for this index.
         *
         * @return the index configuration
         */
        public Config config()
            {
            return m_config;
            }

        // ---- MapIndex interface ------------------------------------------

        /**
         * Returns the ValueExtractor used to extract text from cache values.
         *
         * @return the ValueExtractor instance
         */
        public ValueExtractor<V, String> getValueExtractor()
            {
            return m_extractor;
            }

        /**
         * Returns whether this index maintains entries in order.
         *
         * @return always false as Lucene indices are not ordered
         */
        public boolean isOrdered()
            {
            return false;
            }

        /**
         * Returns whether this index contains partial results.
         *
         * @return always false as this implementation maintains complete index
         */
        public boolean isPartial()
            {
            return false;
            }

        /**
         * Returns the contents of this index.
         *
         * @return a Map of the index contents if inverse map is enabled, {@code null} otherwise
         */
        public Map<String, Set<K>> getIndexContents()
            {
            return f_simpleIndex != null
                   ? f_simpleIndex.getIndexContents()
                   : null;
            }

        /**
         * Returns the indexed value for the specified key.
         *
         * @param k the key
         *
         * @return indexed value for the specified key if forward map is enabled, {@code null} otherwise
         */
        public Object get(K k)
            {
            return f_simpleIndex != null
                   ? f_simpleIndex.get(k)
                   : null;
            }

        /**
         * Returns the comparator used by this index.
         *
         * @return always {@code null}, as Lucene indices are not ordered
         */
        public Comparator<String> getComparator()
            {
            return null;
            }

        /**
         * Returns the size of the index in bytes.
         *
         * @return the total size of all index files in bytes
         */
        public long getUnits()
            {
            long cUnits = f_simpleIndex == null ? 0 : f_simpleIndex.getUnits();
            try
                {
                Directory directory = f_indexWriter.getDirectory();
                for (String file : directory.listAll())
                    {
                    cUnits += directory.fileLength(file);
                    }
                }
            catch (IOException ignore)
                {
                }
            return cUnits;
            }

        /**
         * Forces a merge of the index down to the specified number of segments.
         * This is useful for testing and optimization.
         *
         * @param maxNumSegments maximum number of segments to merge to
         */
        public void forceMerge(int maxNumSegments)
            {
            f_lock.readLock().lock();
            try
                {
                f_indexWriter.forceMerge(maxNumSegments);
                f_indexWriter.commit();
                f_searcherManager.maybeRefresh();
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }
            finally
                {
                f_lock.readLock().unlock();
                }
            }

        /**
         * Forces a commit of any pending changes to the index.
         */
        public void commit()
            {
            f_lock.readLock().lock();
            try
                {
                f_indexWriter.commit();
                f_searcherManager.maybeRefresh();
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }
            finally
                {
                f_lock.readLock().unlock();
                }
            }

        /**
         * Inserts a new entry into the Lucene index.
         *
         * @param entry the entry to index
         *
         * @throws RuntimeException if there is an error updating the index
         */
        @SuppressWarnings("unchecked")
        public void insert(Map.Entry<? extends K, ? extends V> entry)
            {
            f_lock.readLock().lock();
            try
                {
                Binary   binKey   = ((BinaryEntry<K, V>) entry).getBinaryKey();
                BytesRef bytesKey = new BytesRef(binKey.toByteArray());

                Document doc = new Document();
                doc.add(new StoredField("key", bytesKey));  // For storage
                doc.add(new StringField("key", Base.toHex(bytesKey.bytes), Field.Store.NO));  // For term-based lookups
                doc.add(new TextField(m_extractor.getCanonicalName(), extract(entry.getValue()), Field.Store.NO));

                f_indexWriter.addDocument(doc);
                
                // Only flush and refresh if not in batch mode
                if (!m_batchMode)
                    {
                    f_indexWriter.flush();
                    f_searcherManager.maybeRefresh();
                    }
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }
            finally
                {
                f_lock.readLock().unlock();
                if (f_simpleIndex != null)
                    {
                    f_simpleIndex.insert(entry);
                    }
                }
            }

        /**
         * Updates an existing entry in the Lucene index.
         *
         * @param entry the entry to update
         *
         * @throws RuntimeException if there is an error updating the index
         */
        @SuppressWarnings("unchecked")
        public void update(Map.Entry<? extends K, ? extends V> entry)
            {
            f_lock.readLock().lock();
            try
                {
                Binary   binKey   = ((BinaryEntry<K, V>) entry).getBinaryKey();
                BytesRef bytesKey = new BytesRef(binKey.toByteArray());
                String   keyTerm  = Base.toHex(bytesKey.bytes);

                Document doc = new Document();
                doc.add(new StoredField("key", bytesKey));  // For storage
                doc.add(new StringField("key", keyTerm, Field.Store.NO));  // For term-based lookups
                doc.add(new TextField(m_extractor.getCanonicalName(), extract(entry.getValue()), Field.Store.NO));

                f_indexWriter.updateDocument(new Term("key", keyTerm), doc);
                
                // Only flush and refresh if not in batch mode
                if (!m_batchMode)
                    {
                    f_indexWriter.flush();
                    f_searcherManager.maybeRefresh();
                    }
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }
            finally
                {
                f_lock.readLock().unlock();
                if (f_simpleIndex != null)
                    {
                    f_simpleIndex.update(entry);
                    }
                }
            }

        /**
         * Deletes an entry from the Lucene index.
         *
         * @param entry the entry to delete
         *
         * @throws RuntimeException if there is an error updating the index
         */
        @SuppressWarnings("unchecked")
        public void delete(Map.Entry<? extends K, ? extends V> entry)
            {
            f_lock.readLock().lock();
            try
                {
                Binary binKey  = ((BinaryEntry<K, V>) entry).getBinaryKey();
                String keyTerm = Base.toHex(binKey.toByteArray());

                f_indexWriter.deleteDocuments(new Term("key", keyTerm));
                
                // Only flush and refresh if not in batch mode
                if (!m_batchMode)
                    {
                    f_indexWriter.flush();
                    f_searcherManager.maybeRefresh();
                    }
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }
            finally
                {
                f_lock.readLock().unlock();
                if (f_simpleIndex != null)
                    {
                    f_simpleIndex.delete(entry);
                    }
                }
            }

        /**
         * Executes a search query against the Lucene index.
         *
         * @param query the Lucene query to execute
         * @param n     the maximum number of results to return
         *
         * @return a map of binary keys to normalized scores (0.0-1.0)
         *
         * @throws RuntimeException if there is an error executing the search
         */
        public Map<Binary, Float> search(Query query, int n)
            {
            try
                {
                IndexSearcher searcher = getSearcher();
                try
                    {
                    TopDocs topDocs = searcher.search(query, n);
                    int totalHits = (int) topDocs.totalHits.value();
                    if (totalHits == 0)
                        {
                        return EMPTY_RESULT;
                        }

                    Map<Binary, Float> mapResults = new LinkedHashMap<>(totalHits, 1.0f);

                    for (ScoreDoc sd : topDocs.scoreDocs)
                        {
                        Document doc       = searcher.storedFields().document(sd.doc);
                        BytesRef bytesKey  = doc.getBinaryValue("key");
                        Binary   binKey    = new Binary(bytesKey.bytes);

                        mapResults.put(binKey, sd.score);
                        }

                    return mapResults;
                    }
                finally
                    {
                    releaseSearcher(searcher);
                    }
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }
            }

        /**
         * Closes this index by clearing and closing all resources.
         */
        public void close()
            {
            clear();
            }

        /**
         * Clears all entries from this index and releases resources.
         *
         * @throws RuntimeException if there is an error clearing the index
         */
        public void clear()
            {
            f_lock.writeLock().lock();
            try
                {
                f_indexWriter.deleteAll();
                f_indexWriter.commit();
                f_indexWriter.close();
                f_searcherManager.close();
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }
            finally
                {
                f_lock.writeLock().unlock();
                }
            }

        /**
         * Begins a batch operation mode, optimizing the index for bulk updates.
         * In batch mode, the index will:
         * - Use larger RAM buffer
         * - Delay commits
         * - Optimize merge policy
         * This should be called before performing bulk operations like partition transfer.
         */
        public void beginBatch()
            {
            if (!m_batchMode)
                {
                // use write lock to ensure only one thread performs batch mode transition
                // this is fine, because batch mode is used only during initial index creation
                f_lock.writeLock().lock();
                try
                    {
                    if (!m_batchMode)
                        {
                        // Save current config
                        LiveIndexWriterConfig config = f_indexWriter.getConfig();
                        originalRamBufferSizeMB = config.getRAMBufferSizeMB();
                        originalMaxBufferedDocs = config.getMaxBufferedDocs();
                        originalMergePolicy = config.getMergePolicy();

                        // Configure IndexWriter for batch operations
                        config.setRAMBufferSizeMB(256.0);
                        config.setMaxBufferedDocs(IndexWriterConfig.DISABLE_AUTO_FLUSH);

                        // Use optimized merge policy for bulk indexing
                        LogByteSizeMergePolicy mergePolicy = new LogByteSizeMergePolicy();
                        mergePolicy.setMinMergeMB(256.0);
                        mergePolicy.setMaxMergeMB(512.0);
                        mergePolicy.setMergeFactor(10);
                        config.setMergePolicy(mergePolicy);

                        m_batchMode = true;
                        }
                    }
                finally
                    {
                    f_lock.writeLock().unlock();
                    }
                }
            }

        /**
         * Ends the batch operation mode and optimizes the index.
         * This will:
         * - Commit any pending changes
         * - Force merge to a reasonable number of segments
         * - Reset IndexWriter configuration to normal operation
         */
        public void endBatch()
            {
            if (m_batchMode)
                {
                // use write lock to ensure only one thread performs batch mode transition
                // this is fine, because batch mode is used only during initial index creation
                f_lock.writeLock().lock();
                try
                    {
                    if (m_batchMode)
                        {
                        // Commit, then restore original config
                        commit();

                        LiveIndexWriterConfig config = f_indexWriter.getConfig();
                        config.setRAMBufferSizeMB(originalRamBufferSizeMB);
                        config.setMaxBufferedDocs(originalMaxBufferedDocs);
                        config.setMergePolicy(originalMergePolicy);

                        m_batchMode = false;
                        f_searcherManager.maybeRefresh();
                        }
                    }
                catch (IOException e)
                    {
                    throw new RuntimeException(e);
                    }
                finally
                    {
                    f_lock.writeLock().unlock();
                    }
                }
            }

        /**
         * Aborts the batch operation mode without committing changes.
         * This will:
         * - Roll back any uncommitted changes
         * - Reset IndexWriter configuration to normal operation
         */
        public void abortBatch()
            {
            if (m_batchMode)
                {
                // use write lock to ensure only one thread performs batch mode transition
                // this is fine, because batch mode is used only during initial index creation
                f_lock.writeLock().lock();
                try
                    {
                    if (m_batchMode)
                        {
                        // Roll back uncommitted changes
                        f_indexWriter.rollback();

                        // Reset to normal operation settings
                        LiveIndexWriterConfig config = f_indexWriter.getConfig();
                        config.setRAMBufferSizeMB(originalRamBufferSizeMB);
                        config.setMaxBufferedDocs(originalMaxBufferedDocs);
                        config.setMergePolicy(originalMergePolicy);

                        m_batchMode = false;

                        // Refresh searcher to see the changes
                        f_searcherManager.maybeRefresh();
                        }
                    }
                catch (IOException e)
                    {
                    throw new RuntimeException(e);
                    }
                finally
                    {
                    f_lock.writeLock().unlock();
                    }
                }
            }

        /**
         * Returns an IndexSearcher for debugging purposes.
         * The caller MUST call releaseSearcher when done.
         */
        IndexSearcher getSearcher() throws IOException
            {
            return f_searcherManager.acquire();
            }

        /**
         * Releases a previously acquired IndexSearcher.
         */
        void releaseSearcher(IndexSearcher searcher) throws IOException
            {
            f_searcherManager.release(searcher);
            }

        /**
         * The Lucene IndexWriter used to maintain the index.
         * This writer is responsible for all index modifications and must be properly
         * synchronized using the lock.
         */
        private final IndexWriter f_indexWriter;

        /**
         * The SearcherManager that provides access to IndexSearcher instances.
         * Manages the lifecycle of searchers and ensures proper index state visibility.
         */
        private final SearcherManager f_searcherManager;

        /**
         * A standard Coherence index that will be maintained in addition to the
         * Lucene index if either inverse or forward map is enabled.
         */
        private final SimpleMapIndex f_simpleIndex;

        /**
         * Lock for synchronizing index modifications.
         * <p>
         * Unlike most read-write locks, this one is used in somewhat unexpected
         * way: search/read doesn't use the lock at all, and index mutations
         * (insert/update/delete) use read lock instead of write lock, as Lucene
         * allows concurrent updates from multiple threads.
         * <p>
         * The only reason we have a lock at all is to be able to obtain exclusive
         * write lock when closing an index, as call to {@code IndexWriter.close} must
         * be called when no index updates are in progress.
         * <p>
         * We also use a write lock when transitioning to/from batch mode, to ensure
         * that only one thread changes index writer configuration. This is fine,
         * because it should never happen in a critical path -- only during initial
         * index creation when we have to index all the data in a partition, from
         * either a service or a single worker thread.
         */
        private final ReadWriteLock f_lock = new ReentrantReadWriteLock();

        /**
         * Indicates whether the index is currently in batch mode.
         * Batch mode optimizes the index for bulk operations by adjusting
         * IndexWriter configuration for higher throughput and reduced commit frequency.
         * This flag should only be enabled during initial index creation or bulk loading.
         */
        private boolean m_batchMode;

        /**
         * The original RAM buffer size (in MB) of the IndexWriter before entering batch mode.
         * This value is saved when batch mode is enabled and restored when batch mode ends or is aborted.
         */
        private double originalRamBufferSizeMB;

        /**
         * The original maximum number of buffered documents of the IndexWriter before entering batch mode.
         * This value is saved when batch mode is enabled and restored when batch mode ends or is aborted.
         */
        private int originalMaxBufferedDocs;

        /**
         * The original merge policy of the IndexWriter before entering batch mode.
         * This value is saved when batch mode is enabled and restored when batch mode ends or is aborted.
         */
        private MergePolicy originalMergePolicy;
        }

    // ----- inner class: Config --------------------------------------------

    /**
     * Configuration holder for LuceneIndex.
     * Stores suppliers and consumers for customizing Lucene components such as
     * Analyzer, Directory, IndexWriterConfig, and IndexSearcher.
     */
    public static class Config
            implements PortableObject
        {
        /**
         * Returns the supplier function for creating IndexSearcher instances.
         * This function is called each time a new searcher needs to be created,
         * allowing for custom searcher configuration and optimization.
         *
         * @return the searcher supplier function
         */
        public Remote.BiFunction<IndexReader, IndexReader, IndexSearcher> searcherSupplier()
            {
            return m_searcherSupplier;
            }

        /**
         * Sets the supplier function for creating IndexSearcher instances.
         *
         * @param searcherSupplier the searcher supplier function to set
         */
        private void setSearcherSupplier(Remote.BiFunction<IndexReader, IndexReader, IndexSearcher> searcherSupplier)
            {
            m_searcherSupplier = searcherSupplier;
            }

        /**
         * Returns the supplier of Lucene Analyzer instances.
         * The analyzer is used for both indexing and searching operations.
         *
         * @return the analyzer supplier
         */
        public Remote.Supplier<Analyzer> analyzerSupplier()
            {
            return m_analyzerSupplier;
            }

        /**
         * Sets the supplier of Lucene Analyzer instances.
         *
         * @param analyzerSupplier the analyzer supplier to set
         */
        private void setAnalyzerSupplier(Remote.Supplier<Analyzer> analyzerSupplier)
            {
            m_analyzerSupplier = analyzerSupplier;
            }

        /**
         * Returns the supplier function for creating Directory instances.
         * The supplier creates a unique directory for each partition.
         *
         * @return the directory supplier function
         */
        public Remote.Function<Integer, Directory> directorySupplier()
            {
            return m_directorySupplier;
            }

        /**
         * Sets the supplier function for creating Directory instances.
         *
         * @param directorySupplier the directory supplier function to set
         */
        private void setDirectorySupplier(Remote.Function<Integer, Directory> directorySupplier)
            {
            m_directorySupplier = directorySupplier;
            }

        /**
         * Returns the consumer for configuring IndexWriterConfig instances.
         * This consumer is called before creating each index writer, allowing
         * for customization of index writing behavior.
         *
         * @return the writer configurer consumer
         */
        public Remote.Consumer<IndexWriterConfig> writerConfigurer()
            {
            return m_writerConfigurer;
            }

        /**
         * Sets the consumer for configuring IndexWriterConfig instances.
         *
         * @param writerConfigurer the writer configurer consumer to set
         */
        private void setWriterConfigurer(Remote.Consumer<IndexWriterConfig> writerConfigurer)
            {
            m_writerConfigurer = writerConfigurer;
            }

        // ----- PortableObject interface ---------------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_searcherSupplier = in.readObject(0);
            m_writerConfigurer = in.readObject(1);
            m_analyzerSupplier = in.readObject(2);
            m_directorySupplier = in.readObject(3);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_searcherSupplier);
            out.writeObject(1, m_writerConfigurer);
            out.writeObject(2, m_analyzerSupplier);
            out.writeObject(3, m_directorySupplier);
            }

        // ---- data members ------------------------------------------------

        /**
         * The supplier function for creating custom IndexSearcher instances.
         * This function receives both the current and previous IndexReader instances
         * to allow for optimizations such as index warming.
         */
        private Remote.BiFunction<IndexReader, IndexReader, IndexSearcher> m_searcherSupplier = (cur, prev) ->
                {
                try
                    {
                    return new SearcherFactory().newSearcher(cur, prev);
                    }
                catch (IOException e)
                    {
                    throw new RuntimeException(e);
                    }
                };

        /**
         * The supplier of Lucene Analyzer to use for this index.
         * Defaults to StandardAnalyzer.
         */
        private Remote.Supplier<Analyzer> m_analyzerSupplier = StandardAnalyzer::new;

        /**
         * The supplier of Lucene Directory to use for this index.
         * The supplier must return a unique Directory instance for each partition.
         * Defaults to ByteBuffersDirectory.
         */
        private Remote.Function<Integer, Directory> m_directorySupplier = partitionId -> new ByteBuffersDirectory();

        /**
         * The consumer that can be used to customize {@link IndexWriterConfig}
         * for this index. This consumer is called before creating the index writer,
         * allowing for complete control over index writing behavior.
         * <p>
         * Defaults to a no-op consumer, which means the index will use Lucene's
         * default settings and auto-tuning features.
         */
        private Remote.Consumer<IndexWriterConfig> m_writerConfigurer = (config) -> {};
        }

    // ----- constants ------------------------------------------------------

    /**
     * The POF implementation version.
     * This version number is used for serialization compatibility.
     */
    public static final int IMPL_VERSION = 0;

    /**
     * An empty query result map.
     * Used as a default return value when no matches are found.
     */
    private static final Map<Binary, Float> EMPTY_RESULT = Collections.emptyMap();

    /**
     * A counter that will be used to provide unique partition IDs within the JVM
     * until we add support for partition IDs to createIndex.
     */
    private static final AtomicInteger COUNTER = new AtomicInteger();

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ValueExtractor} to use to extract the text from the map entry.
     * This extractor is used to obtain the text content that will be indexed by Lucene.
     */
    private ValueExtractor<V, String> m_extractor;

    /**
     * The flag to enable and maintain a standard Coherence inverse map
     * in addition to the Lucene index, which allows standard Coherence queries
     * and extractors based on the indexed attribute to work efficiently.
     */
    private boolean m_fInverseMap = false;

    /**
     * The configuration for this index.
     */
    private Config m_config = new Config();
    }
