/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.QueryContext;
import com.tangosol.util.QueryRecord;

import java.util.Map;
import java.util.Set;


/**
* Filter which returns the logical exclusive or ("xor") of two other filters.
*
* @author cp/gg 2002.10.27
*/
public class XorFilter
        extends ArrayFilter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public XorFilter()
        {
        }

    /**
    * Construct a "xor" filter. The result is defined as:
    * <blockquote>
    *   filterLeft ^ filterRight
    * </blockquote>
    *
    * @param filterLeft   the "left" filter
    * @param filterRight  the "right" filter
    */
    public XorFilter(Filter filterLeft, Filter filterRight)
        {
        super(new Filter[] {filterLeft, filterRight});
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(Object o)
        {
        Filter[] afilter = m_aFilter;
        return afilter[0].evaluate(o) ^ afilter[1].evaluate(o);
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        return Integer.MAX_VALUE;
        }


    // ----- ArrayFilter methods --------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected Filter applyIndex(Map mapIndexes, Set setKeys, QueryContext ctx,
            QueryRecord.PartialResult.TraceStep step)
        {
        // no use for indexes
        return this;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected Set<Filter<?>> simplifyFilters()
        {
        return Set.of(m_aFilter);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected boolean evaluateEntry(Map.Entry entry, QueryContext context,
            QueryRecord.PartialResult.TraceStep step)
        {
        Filter[] afilter = m_aFilter;
        return InvocableMapHelper.evaluateEntry(afilter[0], entry)
             ^ InvocableMapHelper.evaluateEntry(afilter[1], entry);
        }

    protected String getOperator()
        {
        return "XOR";
        }
    }
