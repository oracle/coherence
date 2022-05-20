/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest.data;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.LongSum;

/**
 * No-op extension of LongSum for testing custom aggregators via REST.
 *
 * @author jh  2012.02.27
 */
public class CustomLongSum
        extends LongSum
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for EL and POF serialization).
     */
    public CustomLongSum()
        {
        super();
        }

    /**
     * Construct a CustomLongSum for the specified property name.
     *
     * @param sName  a property name
     */
    public CustomLongSum(String sName)
        {
        super(sName);
        }

    /**
     * Construct a CustomLongSum for the specified extractor.
     *
     * @param extractor  extractor used with aggregator
     */
    public CustomLongSum(ValueExtractor extractor)
        {
        super(extractor);
        }

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, Long> supply()
        {
        return new CustomLongSum(getValueExtractor());
        }
    }
