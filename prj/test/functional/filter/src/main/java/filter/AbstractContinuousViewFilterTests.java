/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package filter;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.ValueExtractor;

/**
 * Base class for tests using continuous view caches.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public abstract class AbstractContinuousViewFilterTests
        extends AbstractFilterTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new AbstractContinuousViewFilterTests that will use the cache with
     * the given name in all test methods.
     *
     * @param sCache  the test cache name
     */
    public AbstractContinuousViewFilterTests(String sCache)
        {
        super(sCache);
        }

    // ----- methods from AbstractEntryAggregatorTests ----------------------

    /**
     * Overridden because the indexes on the wrapped cache won't be removed
     * without an explicit call against the wrapped cache.
     */
    public void removeIndexFrom(NamedCache namedCache, ValueExtractor extractor)
        {
        super.removeIndexFrom(namedCache, extractor);
        super.removeIndexFrom(((ContinuousQueryCache) namedCache).getCache(), extractor);
        }
    }
