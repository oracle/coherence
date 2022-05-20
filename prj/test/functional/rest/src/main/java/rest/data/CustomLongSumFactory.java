/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest.data;

import com.tangosol.coherence.rest.util.aggregator.AggregatorFactory;

import com.tangosol.util.InvocableMap;

/**
 * AggregatorFactory that creates instances of CustomLongSum.
 *
 * @author jh  2012.02.27
 */
public class CustomLongSumFactory
        implements AggregatorFactory
    {

    // ----- AggregatorFactory interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public InvocableMap.EntryAggregator getAggregator(String... asArgs)
        {
        return new CustomLongSum(asArgs[0]);
        }
    }
