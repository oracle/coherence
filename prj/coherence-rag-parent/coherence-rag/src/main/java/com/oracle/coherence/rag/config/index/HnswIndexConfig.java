/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.config.index;

import com.oracle.coherence.ai.hnsw.HnswIndex;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;
import java.util.Objects;

/**
 * Configuration class for {@link HnswIndex} vector indexing.
 * <p/>
 * HNSW (Hierarchical Navigable Small World) is a state-of-the-art graph-based algorithm
 * for approximate nearest neighbor search in high-dimensional vector spaces. It provides
 * excellent performance characteristics with logarithmic search complexity and high recall rates.
 * <p/>
 * <strong>HNSW Algorithm Benefits:</strong>
 * <ul>
 * <li><strong>High Performance</strong> - Logarithmic search complexity O(log N) for most practical datasets</li>
 * <li><strong>Excellent Recall</strong> - Achieves high recall rates (>95%) with proper parameter tuning</li>
 * <li><strong>Memory Efficient</strong> - Compact graph structure with configurable memory usage</li>
 * <li><strong>Scalable</strong> - Handles millions of vectors efficiently with incremental updates</li>
 * <li><strong>Flexible Distance Metrics</strong> - Supports various similarity measures (cosine, L2, inner product)</li>
 * </ul>
 * <p/>
 * <strong>Algorithm Overview:</strong>
 * HNSW constructs a multi-layer graph where each layer contains a subset of the data points.
 * Higher layers provide coarse navigation while lower layers offer fine-grained search. This
 * hierarchical structure enables efficient approximate nearest neighbor queries.
 * <p/>
 * <strong>Key Parameters:</strong>
 * <ul>
 * <li><strong>M</strong> - Controls connectivity and memory usage (typically 12-48)</li>
 * <li><strong>ef_construction</strong> - Index build quality vs. time trade-off (typically 200-400)</li>
 * <li><strong>ef_search</strong> - Query accuracy vs. speed trade-off (typically 50-200)</li>
 * <li><strong>Space Name</strong> - Distance metric (COSINE, L2, IP)</li>
 * </ul>
 * <p/>
 * <strong>Configuration Example:</strong>
 * <pre>{@code
 * HnswIndexConfig config = new HnswIndexConfig()
 *     .setSpaceName("COSINE")           // Use cosine similarity
 *     .setM(32)                         // Higher connectivity for better recall
 *     .setEfConstruction(400)           // Higher build quality
 *     .setEfSearch(100);                // Balance accuracy and speed
 * }</pre>
 * <p/>
 * This configuration class supports POF serialization for persistence and distribution
 * across Coherence cluster nodes, and provides a fluent API for method chaining.
 *
 * @see HnswIndex
 * @see IndexConfig
 *
 * @author Aleks Seovic  2025.06.28
 * @since 25.09
 */
@SuppressWarnings("unused")
public class HnswIndexConfig
        extends IndexConfig<HnswIndex<?, ?>>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Constructs a new HnswIndexConfig with default settings optimized for general use.
     * <p/>
     * Initializes the configuration with well-tested default values that provide
     * a good balance between index quality, memory usage, and search performance:
     * <ul>
     * <li>Index type: "HNSW"</li>
     * <li>Space name: "COSINE" (cosine similarity)</li>
     * <li>Max elements: 4096 (with automatic growth)</li>
     * <li>M: 16 (bidirectional links per element)</li>
     * <li>ef_construction: 200 (index build quality)</li>
     * <li>ef_search: 50 (query performance)</li>
     * <li>Random seed: 100 (reproducible results)</li>
     * </ul>
     * <p/>
     * These defaults are suitable for most applications but can be tuned based on
     * specific requirements for recall, latency, and memory usage.
     */
    public HnswIndexConfig()
        {
        }

    // ---- methods ---------------------------------------------------------

    /**
     * Returns the type of the index this configuration is for.
     *
     * @return the type of the index this configuration is for
     */
    public String type()
        {
        return TYPE;
        }

    // ---- properties ------------------------------------------------------

    /**
     * Returns the index space name.
     * <p/>
     * The space name determines the distance metric used for similarity
     * calculations. Common space names include "COSINE", "L2", and "IP" (inner
     * product).
     *
     * @return the space name, defaults to {@value #DEFAULT_SPACE_NAME}
     */
    public String getSpaceName()
        {
        return m_sSpaceName;
        }

    /**
     * Sets the index space name.
     * <p/>
     * The space name determines the distance metric used for similarity
     * calculations. Common space names include "COSINE", "L2", and "IP" (inner
     * product).
     *
     * @param sSpaceName the space name to set
     *
     * @return this {@code HnswIndexConfig} instance for method chaining
     */
    public HnswIndexConfig setSpaceName(String sSpaceName)
        {
        m_sSpaceName = sSpaceName;
        return this;
        }

    /**
     * Returns the maximum number of elements the index can contain.
     * <p/>
     * The index will grow automatically by doubling its capacity until it
     * reaches approximately 8m elements, at which point it will grow by 50%
     * whenever it gets full.
     *
     * @return the maximum number of elements, defaults to 4096
     */
    public int getMaxElements()
        {
        return m_cMaxElements;
        }

    /**
     * Sets the maximum number of elements the index can contain.
     * <p/>
     * The index will grow automatically by doubling its capacity until it
     * reaches approximately 8m elements, at which point it will grow by 50%
     * whenever it gets full.
     *
     * @param cMaxElements the maximum number of elements to set
     *
     * @return this {@code HnswIndexConfig} instance for method chaining
     */
    public HnswIndexConfig setMaxElements(int cMaxElements)
        {
        m_cMaxElements = cMaxElements;
        return this;
        }

    /**
     * Returns the number of bidirectional links created for every new element
     * during construction.
     * <p/>
     * Reasonable range for M is 2-100. Higher M work better on datasets with
     * high intrinsic dimensionality and/or high recall, while low M work better
     * for datasets with low intrinsic dimensionality and/or low recalls. The
     * parameter also determines the algorithm's memory consumption, which is
     * roughly M * 8-10 bytes per stored element.
     * <p/>
     * As an example for dim=4 random vectors optimal M for search is somewhere
     * around 6, while for high dimensional datasets (word embeddings, good face
     * descriptors), higher M are required (e.g. M=48-64) for optimal
     * performance at high recall. The range M=12-48 is ok for the most of the
     * use cases.
     *
     * @return the number of bidirectional links, defaults to 16
     */
    public int getM()
        {
        return m_nM;
        }

    /**
     * Sets the number of bidirectional links created for every new element
     * during construction.
     * <p/>
     * Reasonable range for M is 2-100. Higher M work better on datasets with
     * high intrinsic dimensionality and/or high recall, while low M work better
     * for datasets with low intrinsic dimensionality and/or low recalls. The
     * parameter also determines the algorithm's memory consumption, which is
     * roughly M * 8-10 bytes per stored element.
     * <p/>
     * When M is changed one has to update the other parameters. Nonetheless, ef
     * and ef_construction parameters can be roughly estimated by assuming that
     * M*ef_construction is a constant.
     *
     * @param nM the number of bidirectional links to set
     *
     * @return this {@code HnswIndexConfig} instance for method chaining
     */
    public HnswIndexConfig setM(int nM)
        {
        m_nM = nM;
        return this;
        }

    /**
     * Returns the ef_construction parameter value.
     * <p/>
     * This parameter has the same meaning as ef, which controls the
     * index_time/index_accuracy. Bigger ef_construction leads to longer
     * construction, but better index quality. At some point, increasing
     * ef_construction does not improve the quality of the index. One way to
     * check if the selection of ef_construction was ok is to measure a recall
     * for M nearest neighbor search when ef = ef_construction: if the recall is
     * lower than 0.9, than there is room for improvement.
     *
     * @return the ef_construction parameter value, defaults to 200
     */
    public int getEfConstruction()
        {
        return m_nEfConstruction;
        }

    /**
     * Sets the ef_construction parameter value.
     * <p/>
     * This parameter has the same meaning as ef, which controls the
     * index_time/index_accuracy. Bigger ef_construction leads to longer
     * construction, but better index quality. At some point, increasing
     * ef_construction does not improve the quality of the index.
     *
     * @param nEfConstruction the ef_construction parameter value to set
     *
     * @return this {@code HnswIndexConfig} instance for method chaining
     */
    public HnswIndexConfig setEfConstruction(int nEfConstruction)
        {
        m_nEfConstruction = nEfConstruction;
        return this;
        }

    /**
     * Returns the ef_search parameter value.
     * <p/>
     * This parameter controls the query time/accuracy trade-off. Higher values
     * provide better accuracy at the cost of increased search time.
     *
     * @return the ef_search parameter value, defaults to 50
     */
    public int getEfSearch()
        {
        return m_nEfSearch;
        }

    /**
     * Sets the ef_search parameter value.
     * <p/>
     * This parameter controls the query time/accuracy trade-off. Higher values
     * provide better accuracy at the cost of increased search time.
     *
     * @param nEfSearch the ef_search parameter value to set
     *
     * @return this {@code HnswIndexConfig} instance for method chaining
     */
    public HnswIndexConfig setEfSearch(int nEfSearch)
        {
        m_nEfSearch = nEfSearch;
        return this;
        }

    /**
     * Returns the random seed used for the index.
     * <p/>
     * This seed ensures reproducible index construction when the same data and
     * parameters are used across different runs.
     *
     * @return the random seed, defaults to 100
     */
    public int getRandomSeed()
        {
        return m_nRandomSeed;
        }

    /**
     * Sets the random seed used for the index.
     * <p/>
     * This seed ensures reproducible index construction when the same data and
     * parameters are used across different runs.
     *
     * @param nRandomSeed the random seed to set
     *
     * @return this {@code HnswIndexConfig} instance for method chaining
     */
    public HnswIndexConfig setRandomSeed(int nRandomSeed)
        {
        m_nRandomSeed = nRandomSeed;
        return this;
        }

    // ---- AbstractConfig methods ------------------------------------------

    @Override
    public HnswIndex<?, ?> apply(HnswIndex<?, ?> target)
        {
        return target.setSpaceName(getSpaceName())
                .setM(getM())
                .setEfConstruction(getEfConstruction())
                .setEfSearch(getEfSearch())
                .setMaxElements(getMaxElements())
                .setRandomSeed(getRandomSeed());
        }

    // ---- Object methods --------------------------------------------------

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
        HnswIndexConfig that = (HnswIndexConfig) o;
        return m_cMaxElements == that.m_cMaxElements &&
               m_nM == that.m_nM
               && m_nEfConstruction == that.m_nEfConstruction
               && m_nEfSearch == that.m_nEfSearch
               && m_nRandomSeed == that.m_nRandomSeed
               && Objects.equals(m_sSpaceName, that.m_sSpaceName);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_sSpaceName, m_cMaxElements, m_nM, m_nEfConstruction, m_nEfSearch, m_nRandomSeed);
        }

    @Override
    public String toString()
        {
        return "HnswIndexConfig[" +
               "spaceName='" + m_sSpaceName + '\'' +
               ", maxElements=" + m_cMaxElements +
               ", M=" + m_nM +
               ", efConstruction=" + m_nEfConstruction +
               ", efSearch=" + m_nEfSearch +
               ", randomSeed=" + m_nRandomSeed +
               ']';
        }

    // ---- AbstractEvolvable interface -------------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sSpaceName = in.readString(0);
        m_cMaxElements = in.readInt(1);
        m_nM = in.readInt(2);
        m_nEfConstruction = in.readInt(3);
        m_nEfSearch = in.readInt(4);
        m_nRandomSeed = in.readInt(5);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sSpaceName);
        out.writeInt(1, m_cMaxElements);
        out.writeInt(2, m_nM);
        out.writeInt(3, m_nEfConstruction);
        out.writeInt(4, m_nEfSearch);
        out.writeInt(5, m_nRandomSeed);
        }

    // ---- constants -------------------------------------------------------

    /**
     * The type of the index this configuration is for.
     */
    public static final String TYPE = "HNSW";

    /**
     * The implementation version for this class.
     * <p/>
     * This version is used by Coherence POF for class evolution support.
     * Increment this value when making incompatible changes to the class structure.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    /**
     * The default space name for similarity calculations.
     * <p/>
     * COSINE similarity is chosen as the default because it performs well
     * with normalized vectors and is widely used in text embeddings and
     * semantic search applications.
     */
    public static final String DEFAULT_SPACE_NAME = "COSINE";

    // ---- data members ----------------------------------------------------

    /**
     * The index space name that determines the distance metric.
     * <p/>
     * The space name controls how vector similarity is calculated during search operations.
     * Common options include:
     * <ul>
     * <li><strong>COSINE</strong> - Cosine similarity (default, good for normalized vectors)</li>
     * <li><strong>L2</strong> - Euclidean distance (good for spatial data)</li>
     * <li><strong>IP</strong> - Inner product (good for learned embeddings)</li>
     * </ul>
     * <p/>
     * <strong>Default:</strong> {@value #DEFAULT_SPACE_NAME}
     */
    private String m_sSpaceName = DEFAULT_SPACE_NAME;

    /**
     * The maximum number of elements the index can initially contain.
     * <p/>
     * The HNSW index will automatically grow beyond this limit by doubling its capacity
     * until reaching approximately 8 million elements, after which it grows by 50%.
     * This parameter primarily affects initial memory allocation and early performance.
     * <p/>
     * <strong>Default:</strong> 4096
     * <br/>
     * <strong>Range:</strong> Positive integers (typically 1K-1M)
     * <br/>
     * <strong>Impact:</strong> Higher values reduce early reallocations but use more initial memory
     */
    private int m_cMaxElements = 4096;

    /**
     * The number of bidirectional links created for each element during index construction.
     * <p/>
     * This parameter is crucial for balancing memory usage, index quality, and search performance.
     * It directly affects the graph connectivity and determines the algorithm's memory consumption
     * (approximately M * 8-10 bytes per stored element).
     * <p/>
     * <strong>Tuning Guidelines:</strong>
     * <ul>
     * <li><strong>Low dimensionality (&lt;100):</strong> M = 6-16</li>
     * <li><strong>Medium dimensionality (100-1000):</strong> M = 16-32</li>
     * <li><strong>High dimensionality (&gt;1000):</strong> M = 32-64</li>
     * </ul>
     * <p/>
     * <strong>Default:</strong> 16
     * <br/>
     * <strong>Range:</strong> 2-100 (typically 6-64)
     * <br/>
     * <strong>Impact:</strong> Higher M improves recall but increases memory usage and build time
     */
    private int m_nM = 16;

    /**
     * The size of the dynamic candidate list during index construction.
     * <p/>
     * This parameter controls the trade-off between index construction time and final index quality.
     * Larger values lead to longer construction time but result in better index structure and
     * improved search recall. The relationship M * ef_construction ≈ constant provides a
     * rough guideline for parameter tuning.
     * <p/>
     * <strong>Recommended Values:</strong>
     * <ul>
     * <li><strong>Fast build:</strong> 100-200 (acceptable quality)</li>
     * <li><strong>Balanced:</strong> 200-400 (good quality, reasonable time)</li>
     * <li><strong>High quality:</strong> 400-800 (best quality, longer build time)</li>
     * </ul>
     * <p/>
     * <strong>Default:</strong> 200
     * <br/>
     * <strong>Range:</strong> Positive integers (typically 100-800)
     * <br/>
     * <strong>Impact:</strong> Higher values improve index quality but increase construction time
     */
    private int m_nEfConstruction = 200;

    /**
     * The size of the dynamic candidate list during search operations.
     * <p/>
     * This parameter controls the query time vs. accuracy trade-off during search.
     * Higher values provide better recall at the cost of increased search latency.
     * This parameter can be adjusted at query time without rebuilding the index.
     * <p/>
     * <strong>Performance Guidelines:</strong>
     * <ul>
     * <li><strong>Real-time search:</strong> 10-50 (fast queries, good recall)</li>
     * <li><strong>Interactive search:</strong> 50-100 (balanced latency and accuracy)</li>
     * <li><strong>Batch processing:</strong> 100-500 (maximum accuracy)</li>
     * </ul>
     * <p/>
     * <strong>Default:</strong> 50
     * <br/>
     * <strong>Range:</strong> Positive integers (typically 10-500)
     * <br/>
     * <strong>Impact:</strong> Higher values improve recall but increase query latency
     */
    private int m_nEfSearch = 50;

    /**
     * The random seed used for reproducible index construction.
     * <p/>
     * This seed ensures that multiple index builds with the same data and parameters
     * will produce identical results. This is valuable for testing, debugging, and
     * ensuring consistent behavior across different environments.
     * <p/>
     * <strong>Default:</strong> 100
     * <br/>
     * <strong>Range:</strong> Any integer value
     * <br/>
     * <strong>Impact:</strong> Only affects reproducibility, not performance or quality
     */
    private int m_nRandomSeed = 100;
    }
