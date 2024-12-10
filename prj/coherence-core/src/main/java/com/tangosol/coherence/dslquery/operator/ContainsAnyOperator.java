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

import com.tangosol.util.filter.ContainsAnyFilter;

/**
 * An operator representing the conditional "contains any" operation.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public class ContainsAnyOperator
        extends BaseOperator<ContainsAnyFilter>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ContainsAnyOperator.
     */
    protected ContainsAnyOperator()
        {
        super("contains_any", true);
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public ContainsAnyFilter makeFilter(Object oLeft, Object oRight)
        {
        return new ContainsAnyFilter((ValueExtractor) oLeft, unmodifiableSet(oRight));
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
     * An instance of the ContainsAnyOperator.
     */
    public static final ContainsAnyOperator INSTANCE = new ContainsAnyOperator();
    }
