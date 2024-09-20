/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

/**
 * The result of executing a query on a vector store.
 *
 * @param <K>  the type of the key
 * @param <V>  the type of the value
 */
public interface QueryResult<K, V>
    {
    /**
     * The result value obtained by executing the query on the vector.
     *
     * @return the result value obtained by executing the query on the vector
     */
    double getDistance();

    /**
     * Returns the key for the entry this result matches.
     *
     * @return the key for the entry this result matches
     */
    K getKey();

    /**
     * Returns the value for the entry this result matches.
     *
     * @return the value for the entry this result matches
     */
    V getValue();
    }
