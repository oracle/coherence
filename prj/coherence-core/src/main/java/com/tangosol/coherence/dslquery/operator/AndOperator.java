/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.coherence.dsltools.precedence.InfixOPToken;
import com.tangosol.coherence.dsltools.precedence.OPToken;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.util.Filter;

import com.tangosol.util.filter.AllFilter;

/**
 * An operator representing a logical AND (&amp;&amp;).
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public class AndOperator
        extends BaseOperator<AllFilter>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an AndOperator.
     */
    protected AndOperator()
        {
        super("&&", true, "and");
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public AllFilter makeFilter(Object oLeft, Object oRight)
        {
        int cFilter = oLeft instanceof AllFilter
                      ? ((AllFilter) oLeft).getFilters().length
                      : 1;

        cFilter += oRight instanceof AllFilter
                   ? ((AllFilter) oRight).getFilters().length
                   : 1;

        Filter[] aFilters = new Filter[cFilter];

        populateFilterArray(aFilters, (Filter) oLeft, (Filter) oRight);

        return new AllFilter(aFilters);
        }

    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        tokenTable.addToken(new InfixOPToken(f_sSymbol, OPToken.PRECEDENCE_LOGICAL, OPToken.BINARY_OPERATOR_NODE));
        addAliases(tokenTable);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Populate the specified target {@link Filter} array with the Filters in the source
     * array.
     * If the any of the Filters in the source array is an {@link AllFilter} then rather
     * than adding the AllFilter itself to the target array all of the filters contained
     * within the AllFilter are added to the array.
     *
     * @param aFilterDest the Filter array to be populated
     * @param aFilterSrc  the outer filter to add to the array
     */
    protected void populateFilterArray(Filter[] aFilterDest, Filter... aFilterSrc)
        {
        int offset = 0;

        for (Filter filter : aFilterSrc)
            {
            if (filter instanceof AllFilter)
                {
                for (Filter innerFilter : ((AllFilter) filter).getFilters())
                    {
                    aFilterDest[offset++] = innerFilter;
                    }
                }
            else
                {
                aFilterDest[offset++] = filter;
                }
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of the AndOperator.
     */
    public static final AndOperator INSTANCE = new AndOperator();
    }
