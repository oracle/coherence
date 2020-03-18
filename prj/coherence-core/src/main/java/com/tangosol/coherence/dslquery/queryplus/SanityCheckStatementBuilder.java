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
 * A {@link com.tangosol.coherence.dslquery.StatementBuilder} that builds
 * the QueryPlus "SANITY CHECK" command.
 *
 * @author jk  2014.01.06
 * @since Coherence 12.2.1
 */
public class SanityCheckStatementBuilder
        extends AbstractQueryPlusStatementBuilder
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public SanityCheckQueryPlusStatement realize(ExecutionContext ctx, NodeTerm term,
            List listBindVars, ParameterResolver namedBindVars)
        {
        AtomicTerm action = (AtomicTerm) term.termAt(1);

        if ("on".equals(action.getValue()))
            {
            return new SanityCheckQueryPlusStatement(true);
            }
        else if ("off".equals(action.getValue()))
            {
            return new SanityCheckQueryPlusStatement(false);
            }

        throw new CohQLException("Invalid sanity check command - valid syntax is: " + getSyntax());
        }

    @Override
    public String getSyntax()
        {
        return "SANITY [CHECK] (ON | OFF)";
        }

    @Override
    public String getDescription()
        {
        return "Controls sanity checking mode to verify a cache exists prior to executing an\n" +
                "operation on it.";
        }

    @Override
    public AbstractOPToken instantiateOpToken()
        {
        return new SanityCheckCommandOPToken();
        }

    // ----- inner class: SanityCheckCommandOPToken -------------------------

    /**
     * A CohQL OPToken representing the QueryPlus "sanity check" command.
     */
    public class SanityCheckCommandOPToken
            extends AbstractOPToken
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a SanityCheckCommandOPToken.
         */
        public SanityCheckCommandOPToken()
            {
            super("sanity", OPToken.IDENTIFIER_NODE, "sanityCheckCommand");
            }

        // ----- OpToken methods --------------------------------------------

        @Override
        public Term nud(OPParser parser)
            {
            OPScanner scanner = parser.getScanner();
            String    sAction = scanner.getCurrentAsString();

            if ("check".equalsIgnoreCase(sAction))
                {
                sAction = scanner.next().getValue();
                }

            if ("on".equals(sAction) || "off".equals(sAction))
                {
                scanner.advance();

                return Terms.newTerm(getFunctor(), AtomicTerm.createString(sAction));
                }

            return super.nud(parser);
            }
        }

    // ----- inner class: SanityCheckQueryPlusStatement ---------------------

    /**
     * A class representing the QueryPlus "SANITY CHECK" command.
     */
    public class SanityCheckQueryPlusStatement
            extends AbstractStatement
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a SanityCheckQueryPlusStatement to turn on or off
         * QueryPlus sanity checks.
         *
         * @param fSanity  true to turn on sanity checking, false to turn it off.
         */
        protected SanityCheckQueryPlusStatement(boolean fSanity)
            {
            f_fSanity = fSanity;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            ctx.setSanityCheckingEnabled(f_fSanity);

            return StatementResult.NULL_RESULT;
            }

        // ----- data members -----------------------------------------------

        /**
         * Flag indicating whether this command turns sanity checking on or off.
         */
        protected final boolean f_fSanity;
        }
    }
