/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.oracle.coherence.ai.search.BinaryQueryResult;
import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;

/**
 * A custom {@link MapIndex} that maintains a vector search index.
 *
 * @param <KeyType>     the type of the cache keys
 * @param <ValueType>   the type of the cache values
 * @param <VectorType>  the type of the vector
 */
public interface VectorIndex<KeyType, ValueType, VectorType>
        extends MapIndex<KeyType, ValueType, VectorType>
    {
    /**
     * Return the results of the query.
     *
     * @param vector  the vector to use to perform the search
     * @param k       the maximum number of results to return
     * @param filter  an optional {@link Filter} to filter the returned results
     *
     * @return  the search results
     */
    BinaryQueryResult[] query(VectorType vector, int k, Filter<?> filter);
    }
