/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;


import com.tangosol.util.InvocableMap;
import com.tangosol.util.aggregator.AbstractAggregator;
import com.tangosol.util.ValueExtractor;


/**
* Aggregator to return a single value without aggregation.   This is used when
* doing key group-by clauses.
*
* @author ew 2008.07.07
* @since Coherence 3.4
*/
public class Collection
        extends AbstractAggregator
    {
    /**
    * Public constructor to assign the value extractor for the processing.
    *
    * @param extractor the ValueExtractor of the data to be processed
    */
    public Collection(ValueExtractor extractor)
        {
        super(extractor);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator supply()
        {
        return new Collection(getValueExtractor());
        }

    @Override
    public int characteristics()
        {
        return PARALLEL | PRESENT_ONLY;
        }

    // ----- AbstractAggregator methods -------------------------------------

    /**
    * @inheritDoc
    */
    protected void init(boolean fFinal)
        {
        // No Implementation
        }

    /**
    * @inheritDoc
    */
    protected void process(Object o, boolean fFinal)
        {
        m_oValue = o;
        }

    /**
    * @inheritDoc
    */
    protected Object finalizeResult(boolean fFinal)
        {
        return m_oValue;
        }

    /**
    * The last value passed.
    */
    protected Object m_oValue;
    }
