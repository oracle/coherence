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
 * A class that builds the QueryPlus "WHENEVER" command.
 *
 * @author jk  2014.08.05
 * @since Coherence 12.2.1
 */
public class WheneverStatementBuilder
        extends AbstractQueryPlusStatementBuilder
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public WheneverQueryPlusStatement realize(ExecutionContext ctx, NodeTerm term,
            List listBindVars, ParameterResolver namedBindVars)
        {
        AtomicTerm action = (AtomicTerm) term.termAt(1);
        String     sValue = action.getValue();

        if ("continue".equalsIgnoreCase(sValue))
            {
            return new WheneverQueryPlusStatement(false);
            }
        else if ("exit".equalsIgnoreCase(sValue))
            {
            return new WheneverQueryPlusStatement(true);
            }

        throw new CohQLException("Invalid whenever command - valid syntax is: " + getSyntax());
        }

    @Override
    public String getSyntax()
        {
        return "WHENEVER COHQLERROR THEN (CONTINUE | EXIT)";
        }

    @Override
    public String getDescription()
        {
        return "Controls the action taken by QueryPlus when a statement fails to execute.";
        }

    @Override
    public AbstractOPToken instantiateOpToken()
        {
        return new WheneverCommandOPToken();
        }

    // ----- inner class: WheneverCommandOPToken ----------------------------

    /**
     * A CohQL OPToken representing the QueryPlus "whenever" command.
     */
    public class WheneverCommandOPToken
            extends AbstractOPToken
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a WheneverCommandOPToken.
         */
        public WheneverCommandOPToken()
            {
            super("whenever", OPToken.IDENTIFIER_NODE, "wheneverCommand");
            }

        // ----- OpToken methods --------------------------------------------

        @Override
        public Term nud(OPParser parser)
            {
            OPScanner scanner = parser.getScanner();

            scanner.advanceWhenMatching("cohqlerror");
            scanner.advanceWhenMatching("then");

            String sAction = scanner.getCurrentAsStringWithAdvance();

            return Terms.newTerm(getFunctor(), AtomicTerm.createString(sAction));
            }
        }

    // ----- inner class: WheneverQueryPlusStatement ------------------------

    /**
     * The command to set the QueryPlus error action.
     */
    public class WheneverQueryPlusStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a WheneverQueryPlusStatement to set the error action.
         *
         * @param fStopOnError  flag indicating that statement processing
         *                      should stop if an error occurs
         */
        protected WheneverQueryPlusStatement(boolean fStopOnError)
            {
            m_fStopOnError = fStopOnError;
            }

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            ctx.setStopOnError(m_fStopOnError);

            return StatementResult.NULL_RESULT;
            }

        // ----- data members -----------------------------------------------

        /**
         * The QueryPlus error action.
         */
        protected boolean m_fStopOnError;
        }
    }
