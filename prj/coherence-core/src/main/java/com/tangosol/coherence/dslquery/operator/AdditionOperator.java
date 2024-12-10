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
 * A {@link BaseOperator} implementation representing the
 * addition (+) operator.
 *
 * @author jk 2014.04.23
 * @since Coherence 12.2.1
 */
public class AdditionOperator
        extends BaseOperator
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an AdditionOperator.
     */
    protected AdditionOperator()
        {
        super("+", false);
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        InfixOPToken token = new InfixOPToken(f_sSymbol, OPToken.PRECEDENCE_SUM, OPToken.BINARY_OPERATOR_NODE,
                                 OPToken.UNARY_OPERATOR_NODE);

        token.setPrefixAllowed(true);
        tokenTable.addToken(token);
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of the AndOperator.
     */
    public static final AdditionOperator INSTANCE = new AdditionOperator();
    }
