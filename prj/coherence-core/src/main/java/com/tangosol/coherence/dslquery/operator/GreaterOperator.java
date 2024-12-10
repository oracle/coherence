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
import com.tangosol.util.filter.GreaterFilter;

/**
 * A class representing the greater than operator.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public class GreaterOperator
        extends ComparisonOperator
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a GreaterOperator.
     */
    protected GreaterOperator()
        {
        super(">");
        }

    // ----- ComparisonOperator methods -------------------------------------

    @Override
    public ComparisonOperator flip()
        {
        return LessOperator.INSTANCE;
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public ComparisonFilter makeFilter(Object oLeft, Object oRight)
        {
        return new GreaterFilter((ValueExtractor) oLeft, (Comparable) oRight);
        }

    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        tokenTable.addToken(new InfixOPToken(f_sSymbol, OPToken.PRECEDENCE_RELATIONAL, OPToken.BINARY_OPERATOR_NODE));
        addAliases(tokenTable);
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of the GreaterOperator.
     */
    public static final GreaterOperator INSTANCE = new GreaterOperator();
    }
