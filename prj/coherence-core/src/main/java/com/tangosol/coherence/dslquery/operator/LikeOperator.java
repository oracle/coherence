/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.coherence.dsltools.precedence.LikeOPToken;
import com.tangosol.coherence.dsltools.precedence.OPToken;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.LikeFilter;

/**
 * A class representing the "like" operator.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public class LikeOperator
        extends BaseOperator<LikeFilter>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a LikeOperator.
     */
    protected LikeOperator()
        {
        super("like", true);
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public LikeFilter makeFilter(Object oLeft, Object oRight)
        {
        if (oRight.getClass().isArray())
            {
            Object[] ao      = (Object[]) oRight;
            char     cEscape = ((String) ao[1]).charAt(0);

            return new LikeFilter((ValueExtractor) oLeft, (String) ao[0], cEscape, false);
            }

        return new LikeFilter((ValueExtractor) oLeft, (String) oRight, (char) 0, false);
        }

    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        tokenTable.addToken(new LikeOPToken("like", OPToken.PRECEDENCE_RELATIONAL, OPToken.BINARY_OPERATOR_NODE));
        addAliases(tokenTable);
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of the LikeOperator.
     */
    public static final LikeOperator INSTANCE = new LikeOperator();
    }
