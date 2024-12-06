/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.search;

public class SimpleQueryResult<K, V>
        extends BaseQueryResult<K, V>
    {
    public SimpleQueryResult()
        {
        }

    public SimpleQueryResult(double result, K key, V value)
        {
        super(result, key, value);
        }
    }
