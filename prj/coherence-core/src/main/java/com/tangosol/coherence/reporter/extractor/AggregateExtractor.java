/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;


import com.tangosol.coherence.reporter.DataSource;
import com.tangosol.util.ValueExtractor;

/**
* Extractor wrapper class for aggregates.
*
* @author ew 2008.03.17
* @since Coherence 3.4
*/
public class AggregateExtractor
    implements ValueExtractor
    {
    // ----- constructors ----------------------------------------------------
    /**
    * Contruct an extractor for an aggregate result.
    *
    * @param source       the data source of the aggregate
    * @param AggregateNdx the aggregate index in the data source.
    */
    public AggregateExtractor(DataSource source, int AggregateNdx)
        {
        m_source = source;
        m_iAggregateNdx = AggregateNdx;
        }

    // ----- ValueExtractor interface ----------------------------------------
    /**
    * @inheritDoc
    */
    public Object extract(Object oTarget)
        {
        return m_source.getAggValue(oTarget, m_iAggregateNdx);
        }

    // ----- data members ----------------------------------------------------
    /*
    * the data source for the aggregate
    */
    protected DataSource m_source;

    /*
    * the location of the aggregate in the data source.
    */
    protected int m_iAggregateNdx;
    }
