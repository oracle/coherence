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

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.ComparisonFilter;
import com.tangosol.util.filter.LessFilter;

/**
 * A class representing the logical less than or equal to operator.
 * <p>
 * This operator will produce instances of {@link LessFilter}.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public class LessOperator
        extends ComparisonOperator
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a LessOperator.
     */
    protected LessOperator()
        {
        super("<");
        }

    // ----- ComparisonOperator methods -------------------------------------

    @Override
    public ComparisonOperator flip()
        {
        return GreaterOperator.INSTANCE;
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public ComparisonFilter makeFilter(Object oLeft, Object oRight)
        {
        return new LessFilter((ValueExtractor) oLeft, (Comparable) oRight);
        }

    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        tokenTable.addToken(new InfixOPToken(f_sSymbol, OPToken.PRECEDENCE_RELATIONAL, OPToken.BINARY_OPERATOR_NODE));
        addAliases(tokenTable);
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of the LessOperator.
     */
    public static final LessOperator INSTANCE = new LessOperator();
    }
