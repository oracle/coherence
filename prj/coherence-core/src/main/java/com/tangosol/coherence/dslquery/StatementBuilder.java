/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import java.util.List;

/**
 * Classes implementing this interface build instances of
 * {@link Statement} implementations.
 *
 * @author jk  2013.12.09
 * @since Coherence 12.2.1
 */
public interface StatementBuilder<T extends Statement>
    {
    /**
     * Realizes an implementation of a {@link Statement} that can be
     * executed to perform a specific CohQL command.
     *
     * @param ctx            the {@link ExecutionContext} to use to create commands
     * @param term           the parsed {@link NodeTerm} used to create the relevant Statement
     * @param listBindVars   the indexed bind variables
     * @param namedBindVars  the named bind variables
     *
     * @return an executable instance of a Statement
     */
    public T realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
                     ParameterResolver namedBindVars);

    /**
     * Return the syntax of the CohQL command.
     *
     * @return the syntax of the CohQL command
     */
    public String getSyntax();

    /**
     * Return a description of the CohQL command suitable for displaying
     * as help text.
     *
     * @return a description of the CohQL command suitable for displaying
     *         as help text
     */
    public String getDescription();
    }
