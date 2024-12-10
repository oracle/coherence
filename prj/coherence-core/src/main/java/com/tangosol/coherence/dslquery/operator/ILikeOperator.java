/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.coherence.dsltools.precedence.LikeOPToken;
import com.tangosol.coherence.dsltools.precedence.OPToken;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.util.filter.LikeFilter;

/**
 * A class representing the case-insensitive "ilike" operator.
 *
 * @author rl 2021.6.24
 * @since 21.06
 */
public class ILikeOperator
        extends LikeOperator
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@code ILikeOperator}.
     */
    protected ILikeOperator()
        {
        super("ilike", true);
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        tokenTable.addToken(new LikeOPToken("ilike", OPToken.PRECEDENCE_RELATIONAL, OPToken.BINARY_OPERATOR_NODE));
        addAliases(tokenTable);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Returns {@code true} if the {@link LikeFilter} should be case-insensitive, otherwise returns
     * {@code false}.
     *
     * @return the {@code ILikeOperator} is case-insensitive, therefore this returns {@code true}
     */
    @Override
    protected boolean isIgnoreCase()
        {
        return true;
        }

    /**
     * An instance of the LikeOperator.
     */
    public static final ILikeOperator INSTANCE = new ILikeOperator();
    }
