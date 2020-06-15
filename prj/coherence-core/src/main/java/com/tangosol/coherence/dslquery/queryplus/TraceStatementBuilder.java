/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.coherence.dsltools.precedence.IdentifierOPToken;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.precedence.OPToken;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.config.expression.ParameterResolver;

import java.util.List;

/**
 * A {@link com.tangosol.coherence.dslquery.StatementBuilder} that builds the
 * QueryPlus "TRACE" command.
 *
 * @author jk  2014.01.06
 * @since Coherence 12.2.1
 */
public class TraceStatementBuilder
        extends AbstractQueryPlusStatementBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a TraceStatementBuilder.
     *
     * @param tokenDelegate  the delegate {@link OPToken}
     */
    public TraceStatementBuilder(OPToken tokenDelegate)
        {
        f_tokenDelegate = tokenDelegate;
        }

    // ----- StatementBuilder interface -------------------------------------

    @Override
    public TraceQueryPlusStatement realize(ExecutionContext ctx, NodeTerm term,
            List listBindVars, ParameterResolver namedBindVars)
        {
        AtomicTerm atomicTerm = (AtomicTerm) term.termAt(1);

        if ("on".equals(atomicTerm.getValue()))
            {
            return new TraceQueryPlusStatement(true);
            }
        else if ("off".equals(atomicTerm.getValue()))
            {
            return new TraceQueryPlusStatement(false);
            }

        throw new CohQLException("Invalid trace command - valid syntax is: " + getSyntax());
        }

    @Override
    public String getSyntax()
        {
        return "TRACE (ON | OFF)";
        }

    @Override
    public String getDescription()
        {
        return "Controls tracing mode. This shows information that can help with debugging";
        }

    @Override
    public AbstractOPToken instantiateOpToken()
        {
        return new TraceCommandOPToken();
        }

    // ----- inner class: TraceCommandOPToken -------------------------------

    /**
     * A CohQL OPToken representing the QueryPlus "trace" command.
     */
    public class TraceCommandOPToken
            extends AbstractOPToken
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a TraceCommandOPToken.
         */
        public TraceCommandOPToken()
            {
            super("trace", OPToken.IDENTIFIER_NODE, "traceCommand");
            }

        // ----- OpToken methods --------------------------------------------

        public Term nud(OPParser parser)
            {
            OPScanner scanner = parser.getScanner();
            String    action  = scanner.getCurrentAsString();

            if ("on".equals(action) || "off".equals(action) || f_tokenDelegate == null)
                {
                scanner.advance();

                return Terms.newTerm(getFunctor(), AtomicTerm.createString(action));
                }

            return f_tokenDelegate.nud(parser);
            }

        @Override
        public Term led(OPParser parser, Term termLeft)
            {
            if (f_tokenDelegate == null)
                {
                return super.led(parser, termLeft);
                }

            return f_tokenDelegate.led(parser, termLeft);
            }
        }

    // ----- inner class: TraceQueryPlusStatement ---------------------------

    /**
     * A command that turns on or off QueryPlus trace.
     */
    public class TraceQueryPlusStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a command to turn on or off QueryPlus tracing.
         *
         * @param fTrace  true to turn trace on, false to turn it off
         */
        protected TraceQueryPlusStatement(boolean fTrace)
            {
            m_fTrace = fTrace;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            ctx.setTraceEnabled(m_fTrace);

            return StatementResult.NULL_RESULT;
            }

        // ----- data members -----------------------------------------------

        /**
         * Flag indicating whether this command turns trace on or off.
         */
        protected boolean m_fTrace;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link IdentifierOPToken} to delegate to if we cannot process the token. Typically this would be a previously
     * registered OPToken for the same token string.
     */
    protected final OPToken f_tokenDelegate;
    }
