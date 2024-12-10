/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.token;

import com.tangosol.coherence.dsltools.precedence.OPToken;
import com.tangosol.coherence.dsltools.precedence.PeekOPToken;

/**
 * An CohQL implementation of a {@link com.tangosol.coherence.dsltools.precedence.PeekOPToken}.
 *
 * @author jk 2014.02.12
 * @since Coherence 12.2.1
 */
public class SQLPeekOPToken
        extends PeekOPToken
    {

    /**
     * Construct a SQLPeekOPToken.
     *
     * @param sId     the id (initial keyword) for this token
     * @param tokens  the list of tokens based on the next keyword
     */
    public SQLPeekOPToken(String sId, OPToken... tokens)
        {
        super(sId, IDENTIFIER_NODE, tokens);
        }
    }
