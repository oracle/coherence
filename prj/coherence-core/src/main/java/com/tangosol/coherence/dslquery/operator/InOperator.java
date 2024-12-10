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
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.InFilter;
import com.tangosol.util.filter.InKeySetFilter;

/**
 * A class representing the "in"operator.
 * <p>
 * This operator creates instances of {@link InFilter} or
 * {@link InKeySetFilter} depending on the left hand argument
 * passed to the realize method.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public class InOperator
        extends BaseOperator<Filter>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a InOperator.
     */
    protected InOperator()
        {
        super("in", true);
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public Filter makeFilter(Object oLeft, Object oRight)
        {
        if (oLeft instanceof Filter)
            {
            return new InKeySetFilter((Filter) oLeft, unmodifiableSet(oRight));
            }

        return new InFilter((ValueExtractor) oLeft, unmodifiableSet(oRight));
        }

    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        tokenTable.addToken(new InfixOPToken(f_sSymbol, OPToken.PRECEDENCE_RELATIONAL, OPToken.BINARY_OPERATOR_NODE));
        addAliases(tokenTable);
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of the InOperator.
     */
    public static final InOperator INSTANCE = new InOperator();
    }
