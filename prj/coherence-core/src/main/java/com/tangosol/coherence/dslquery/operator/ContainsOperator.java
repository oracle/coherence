/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.coherence.dsltools.precedence.ContainsOPToken;
import com.tangosol.coherence.dsltools.precedence.OPToken;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.ContainsFilter;

/**
 * An operator representing the conditional "contains" operation.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public class ContainsOperator
        extends BaseOperator<ContainsFilter>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ContainsOperator.
     */
    protected ContainsOperator()
        {
        super("contains", true);
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public ContainsFilter makeFilter(Object oLeft, Object oRight)
        {
        return new ContainsFilter((ValueExtractor) oLeft, oRight);
        }

    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        tokenTable.addToken(new ContainsOPToken(f_sSymbol, OPToken.PRECEDENCE_RELATIONAL),
                            OPToken.BINARY_OPERATOR_NODE);
        addAliases(tokenTable);
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of the ContainsOperator.
     */
    public static final ContainsOperator INSTANCE = new ContainsOperator();
    }
