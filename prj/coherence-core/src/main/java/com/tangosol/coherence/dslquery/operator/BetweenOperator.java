/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.coherence.dsltools.precedence.BetweenOPToken;
import com.tangosol.coherence.dsltools.precedence.OPToken;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.BetweenFilter;

/**
 * An operator representing the "between" conditional operator.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public class BetweenOperator
        extends BaseOperator<BetweenFilter>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a BetweenOperator.
     */
    protected BetweenOperator()
        {
        super("between", true);
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public BetweenFilter makeFilter(Object oLeft, Object oRight)
        {
        Object[] ao = (Object[]) oRight;

        return new BetweenFilter((ValueExtractor) oLeft, (Comparable) ao[0], (Comparable) ao[1]);
        }

    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        tokenTable.addToken(new BetweenOPToken(f_sSymbol, OPToken.PRECEDENCE_RELATIONAL, OPToken.BINARY_OPERATOR_NODE));
        addAliases(tokenTable);
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of the BetweenOperator.
     */
    public static final BetweenOperator INSTANCE = new BetweenOperator();
    }
