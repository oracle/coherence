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

import com.tangosol.util.filter.XorFilter;

/**
 * A class representing the logical XOR operator.
 * <p>
 * This class produces instances of {@link XorFilter}.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public class XorOperator
        extends BaseOperator
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an XorOperator.
     */
    protected XorOperator()
        {
        super("^^", true, "xor");
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public Filter makeFilter(Object oLeft, Object oRight)
        {
        return new XorFilter((Filter) oLeft, (Filter) oRight);
        }

    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        tokenTable.addToken(new InfixOPToken(f_sSymbol, OPToken.PRECEDENCE_LOGICAL, OPToken.BINARY_OPERATOR_NODE));
        addAliases(tokenTable);
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of the XorOperator.
     */
    public static final XorOperator INSTANCE = new XorOperator();
    }
