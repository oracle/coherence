/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.config.index;

import com.oracle.coherence.ai.index.BinaryQuantIndex;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;
import java.util.Objects;

/**
 * Configuration class for {@link BinaryQuantIndex} vector indexing.
 * <p/>
 * Binary quantization is a memory-efficient vector indexing technique that reduces
 * vector storage requirements by compressing high-dimensional vectors into binary
 * representations. This approach significantly reduces memory footprint and can
 * improve search performance, especially for large-scale vector datasets.
 * <p/>
 * <strong>Binary Quantization Benefits:</strong>
 * <ul>
 * <li><strong>Memory Efficiency</strong> - Reduces memory usage by up to 32x compared to full-precision vectors</li>
 * <li><strong>Fast Similarity Search</strong> - Binary operations (XOR, popcount) are extremely fast on modern CPUs</li>
 * <li><strong>Cache Friendliness</strong> - Compressed vectors fit better in CPU cache, improving performance</li>
 * <li><strong>Scalability</strong> - Enables indexing of larger datasets that wouldn't fit in memory otherwise</li>
 * </ul>
 * <p/>
 * <strong>Trade-offs:</strong>
 * <ul>
 * <li><strong>Accuracy</strong> - Some precision is lost due to quantization, though this is often acceptable</li>
 * <li><strong>Oversampling</strong> - Higher oversampling factors can recover accuracy at the cost of performance</li>
 * </ul>
 * <p/>
 * <strong>Configuration Example:</strong>
 * <pre>{@code
 * BinaryQuantIndexConfig config = new BinaryQuantIndexConfig()
 *     .setOversamplingFactor(5); // Higher accuracy, more computation
 * }</pre>
 * <p/>
 * This configuration class supports POF serialization for persistence and distribution
 * across Coherence cluster nodes, and provides fluent API for method chaining.
 *
 * @see BinaryQuantIndex
 * @see IndexConfig
 *
 * @author Aleks Seovic  2025.06.28
 * @since 25.09
 */
public class BinaryQuantIndexConfig
        extends IndexConfig<BinaryQuantIndex<?, ?, ?>>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Constructs a new BinaryQuantIndexConfig with default settings.
     * <p/>
     * Initializes the configuration with:
     * <ul>
     * <li>Index type: "BINARY"</li>
     * <li>Oversampling factor: 3 (balanced accuracy/performance)</li>
     * </ul>
     * <p/>
     * The default oversampling factor of 3 provides a good balance between
     * search accuracy and computational performance for most use cases.
     */
    public BinaryQuantIndexConfig()
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
     * Returns the oversampling factor used during binary quantization search.
     * <p/>
     * The oversampling factor determines how many additional candidates are
     * considered during the binary quantization process to improve search accuracy.
     * This parameter directly affects the accuracy vs. performance trade-off.
     *
     * @return the oversampling factor, defaults to {@value #DEFAULT_OVERSAMPLING_FACTOR}
     *
     * @see #setOversamplingFactor(int)
     */
    public int getOversamplingFactor()
        {
        return m_nOversamplingFactor;
        }

    /**
     * Sets the oversampling factor used during binary quantization search.
     * <p/>
     * The oversampling factor determines how many additional candidates are
     * considered during the binary quantization process to improve search accuracy.
     * This parameter directly affects the accuracy vs. performance trade-off.
     * <p/>
     * <strong>Recommended Values:</strong>
     * <ul>
     * <li><strong>Low Latency Applications:</strong> 1-2 (prioritize speed over accuracy)</li>
     * <li><strong>General Purpose:</strong> 3-5 (balanced approach, recommended default)</li>
     * <li><strong>High Accuracy Requirements:</strong> 6-10 (prioritize accuracy over speed)</li>
     * </ul>
     * <p/>
     * <strong>Performance Impact:</strong>
     * Higher oversampling factors increase computational cost approximately linearly,
     * as more candidates must be evaluated using full-precision similarity calculations.
     *
     * @param nOversamplingFactor the oversampling factor to set, must be positive
     *
     * @return this {@code BinaryQuantIndexConfig} instance for method chaining
     *
     * @throws IllegalArgumentException if nOversamplingFactor is less than 1
     *
     * @see #getOversamplingFactor()
     */
    public BinaryQuantIndexConfig setOversamplingFactor(int nOversamplingFactor)
        {
        if (nOversamplingFactor < 1)
            {
            throw new IllegalArgumentException("Oversampling factor must be positive, got: " + nOversamplingFactor);
            }
        m_nOversamplingFactor = nOversamplingFactor;
        return this;
        }

    // ---- AbstractConfig methods ------------------------------------------

    @Override
    public BinaryQuantIndex<?, ?, ?> apply(BinaryQuantIndex<?, ?, ?> target)
        {
        return target.oversamplingFactor(getOversamplingFactor());
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
        BinaryQuantIndexConfig that = (BinaryQuantIndexConfig) o;
        return m_nOversamplingFactor == that.m_nOversamplingFactor;
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_nOversamplingFactor);
        }

    @Override
    public String toString()
        {
        return "BinaryQuantIndexConfig[" +
                "oversamplingFactor=" + m_nOversamplingFactor + ']';
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
        m_nOversamplingFactor = in.readInt(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_nOversamplingFactor);
        }

    // ---- constants -------------------------------------------------------

    /**
     * The type of the index this configuration is for.
     */
    public static final String TYPE = "BINARY";

    /**
     * The implementation version for this class.
     * <p/>
     * This version is used by Coherence POF for class evolution support.
     * Increment this value when making incompatible changes to the class structure.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    /**
     * The default oversampling factor value.
     * <p/>
     * This default value of 3 provides a good balance between search accuracy
     * and computational performance for most binary quantization use cases.
     */
    public static final int DEFAULT_OVERSAMPLING_FACTOR = 3;

    // ---- data members ----------------------------------------------------

    /**
     * The oversampling factor used during binary quantization search.
     * <p/>
     * This factor determines how many additional candidates are considered
     * during the search process to improve accuracy. Higher values provide
     * better accuracy at the cost of increased computational overhead.
     * <p/>
     * <strong>Default:</strong> {@value #DEFAULT_OVERSAMPLING_FACTOR}
     * <br/>
     * <strong>Range:</strong> Positive integers (typically 1-10)
     * <br/>
     * <strong>Impact:</strong> Linear increase in search time with higher values
     */
    private int m_nOversamplingFactor = DEFAULT_OVERSAMPLING_FACTOR;
    }
