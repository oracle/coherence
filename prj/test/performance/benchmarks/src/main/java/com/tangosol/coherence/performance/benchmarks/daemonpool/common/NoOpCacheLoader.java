/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool.common;

import com.tangosol.net.cache.CacheLoader;

/**
 * CacheLoader used only to make the benchmark backing map an RWBM shape.
 *
 * @author Aleks Seovic  2026.04.30
 * @since 26.04
 */
public class NoOpCacheLoader
        implements CacheLoader<Integer, Integer>
    {
    @Override
    public Integer load(Integer key)
        {
        return null;
        }
    }
