/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.Statement;
import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.coherence.dslquery.statement.DefaultStatementResult;

import com.tangosol.coherence.dsltools.precedence.OPException;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.precedence.OPToken;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.config.expression.ParameterResolver;

import java.util.List;
import java.util.StringJoiner;

/**
 * A class that builds the QueryPlus "ALTER SESSION SET TIMEOUT millis" statement.
 *
 * @author jk 2014.03.05
 * @since 12.2.1
 */
public class SetTimeoutStatementBuilder
        extends AbstractQueryPlusStatementBuilder
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public Statement realize(ExecutionContext ctx, NodeTerm term, List listBindVars, ParameterResolver namedBindVars)
        {
        AtomicTerm atomicTerm = (AtomicTerm) term.termAt(1);
        String     sTimeout   = atomicTerm.getValue();
        Duration   timeout;

        if (atomicTerm.getTypeCode() == AtomicTerm.NULLLITERAL || sTimeout.isEmpty())
            {
            throw new CohQLException("timeout value cannot be null or empty string");
            }

        try
            {
            if (sTimeout.matches("\\d+$"))
                {
                timeout = new Duration(sTimeout, Duration.Magnitude.MILLI);
                }
            else
                {
                timeout = new Duration(sTimeout);
                }

            return new SetTimeoutStatement(timeout);
            }
        catch (IllegalArgumentException e)
            {
            throw new CohQLException("The timeout value of [" + sTimeout + "] is invalid");
            }
        }

    @Override
    public AbstractOPToken instantiateOpToken()
        {
        return new SetTimeoutOPToken();
        }

    @Override
    public String getSyntax()
        {
        return "ALTER SESSION SET TIMEOUT <milli-seconds>";
        }

    @Override
    public String getDescription()
        {
        return "Set the timeout value to be used by the current QueryPlus session. Statements\n" +
               "will be interrupted if they take longer than this time to execute.";
        }

    // ----- inner class: SetTimeoutOPToken ----------------------------------

    /**
     * A CohQL OPToken representing the QueryPlus
     * "ALTER SESSION SET TIMEOUT" statement.
     */
    public class SetTimeoutOPToken
            extends AbstractOPToken
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a SetTimeoutOPToken.
         */
        public SetTimeoutOPToken()
            {
            super("timeout", OPToken.IDENTIFIER_NODE, "setTimeout");
            }

        // ----- AbstractOPToken methods ------------------------------------

        public Term nud(OPParser parser)
            {
            OPScanner scanner = parser.getScanner();

            if (scanner.isEndOfStatement())
                {
                throw new OPException("Invalid ALTER SESSION SET TIMEOUT statement, timeout value required.");
                }

            StringJoiner joiner = new StringJoiner(" ");

            while (!scanner.isEndOfStatement())
                {
                joiner.add(scanner.getCurrentAsStringWithAdvance());
                }

            String sTimeout = joiner.toString();

            return Terms.newTerm(getFunctor(), AtomicTerm.createString(sTimeout));
            }
        }

    // ----- inner class: SetTimeoutStatement -----------------------------

    /**
     * The implementation of the QueryPlus "ALTER SESSION SET TIMEOUT" statement.
     */
    public class SetTimeoutStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a SetTimeoutStatement that will set the
         * current timeout used by the QueryPlus session to the
         * specified timeout.
         *
         * @param timeout  the value to set for the statement timeout
         */
        public SetTimeoutStatement(Duration timeout)
            {
            f_durationTimeout = timeout;
            }

        // ----- AbstractStatement methods ----------------------------------

        /**
         * Set the current timeout to be used by the CohQL session.
         *
         * @param ctx  the {@link ExecutionContext context} to use
         *
         * @return Always returns {@link StatementResult#NULL_RESULT}
         */
        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            ctx.setTimeout(f_durationTimeout);

            return new DefaultStatementResult("CohQL statement timeout set to " + f_durationTimeout.toString(true));
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the timeout to set as the current timeout.
         */
        protected final Duration f_durationTimeout;
        }
    }
