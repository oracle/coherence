/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.Statement;

import com.tangosol.util.Base;


/**
 * A base class for {@link Statement} implementations.
 *
 * @author jk  2013.12.10
 * @since Coherence 12.2.1
 */
public abstract class AbstractStatement
        extends Base
        implements Statement
    {
    // ----- Statement interface --------------------------------------------

    @Override
    public void sanityCheck(ExecutionContext ctx)
        {
        }

    @Override
    public String getExecutionConfirmation(ExecutionContext ctx)
        {
        // default is we do not want a confirmation
        return null;
        }

    /**
     * Test to see whether the given String is a known cache name.
     *
     * @param sName  the cache name
     * @param ctx  the execution context of the CohQL query
     *
     * @throws AssertionError if a cache with the given name
     *                        does not exist.
     */
    protected void assertCacheName(String sName, ExecutionContext ctx)
        {
        if (!ctx.getSession().isCacheActive(sName, null))
            {
            throw new AssertionError(String.format("cache '%s' does not exist!", sName));
            }
        }
    }
