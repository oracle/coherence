/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.tangosol.util.extractor.IndexAwareExtractor;

/**
 * An {@link IndexAwareExtractor} used to create a {@link VectorIndex}
 *
 * @param <V>  the type of the cache value
 * @param <T>  the type of the vector (must be a primitive array of a supported vector type)
 */
public interface VectorIndexExtractor<V, T>
        extends IndexAwareExtractor<V, Vector<T>>
    {
    }
