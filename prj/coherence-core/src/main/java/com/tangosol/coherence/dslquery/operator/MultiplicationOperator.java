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

/**
 * An operator representing the mathematical multiplication operation.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public class MultiplicationOperator
        extends BaseOperator
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a MultiplicationOperator.
     */
    protected MultiplicationOperator()
        {
        super("*", false);
        }

    // ----- BaseOperator methods -------------------------------------------

    /**
     * Add this operator to the given {@link TokenTable}.
     * This typically means adding this operator
     * using its symbol and also adding any aliases.
     *
     * @param tokenTable  the TokenTable to add this operator to.
     */
    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        InfixOPToken token = new InfixOPToken("*", OPToken.PRECEDENCE_PRODUCT, OPToken.BINARY_OPERATOR_NODE,
                                 OPToken.UNARY_OPERATOR_NODE);

        token.setPrefixAllowed(true);
        tokenTable.addToken(token);
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of the AndOperator.
     */
    public static final MultiplicationOperator INSTANCE = new MultiplicationOperator();
    }
