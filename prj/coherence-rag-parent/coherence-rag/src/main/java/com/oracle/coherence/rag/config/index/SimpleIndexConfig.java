/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.config.index;

import com.oracle.coherence.ai.hnsw.HnswIndex;
import com.oracle.coherence.ai.index.BinaryQuantIndex;

import java.util.Objects;

/**
 * Configuration class for {@code SIMPLE} vector indexing.
 * <p/>
 * Simple index uses standard Coherence index on a vector field, in order to
 * avoid chunk and vector deserialization when performing similarity search.
 * <p/>
 * Unlike {@link HnswIndex} and {@link BinaryQuantIndex}, simple index cannot be
 * queried directly, and requires vector distance calculation to be performed
 * for each document chunk in the store. That means that it will always provide the
 * most accurate results, at the cost of performance.
 * <p/>
 * Unless the store is fairly small, {@link HnswIndex} and {@link BinaryQuantIndex}
 * are better options for most use cases, abd should be preferred.
 *
 * @see HnswIndexConfig
 * @see BinaryQuantIndexConfig
 *
 * @author Aleks Seovic  2025.06.28
 * @since 25.09
 */
public class SimpleIndexConfig
        extends IndexConfig<Object>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Constructs a new SimpleIndexConfig with default settings.
     * <p/>
     * Initializes the configuration with:
     * <ul>
     * <li>Index type: "SIMPLE"</li>
     * </ul>
     */
    public SimpleIndexConfig()
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

    // ---- Object methods --------------------------------------------------

    @Override
    public int hashCode()
        {
        return Objects.hash(TYPE);
        }

    @Override
    public String toString()
        {
        return "SimpleIndexConfig[type=SIMPLE]";
        }

    // ---- AbstractEvolvable interface -------------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    // ---- constants -------------------------------------------------------

    /**
     * The type of the index this configuration is for.
     */
    public static final String TYPE = "SIMPLE";

    /**
     * The implementation version for this class.
     * <p/>
     * This version is used by Coherence POF for class evolution support.
     * Increment this value when making incompatible changes to the class structure.
     */
    public static final int IMPLEMENTATION_VERSION = 1;
    }
