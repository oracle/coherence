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
 * A class that builds the QueryPlus "EXTENDED LANGUAGE" command.
 *
 * @author jk  2014.01.06
 * @since Coherence 12.2.1
 */
public class ExtendedLanguageStatementBuilder
        extends AbstractQueryPlusStatementBuilder
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public ExtendedLanguageQueryPlusStatement realize(ExecutionContext ctx, NodeTerm term,
            List listBindVars, ParameterResolver namedBindVars)
        {
        AtomicTerm action = (AtomicTerm) term.termAt(1);

        if ("on".equals(action.getValue()))
            {
            return new ExtendedLanguageQueryPlusStatement(true);
            }
        else if ("off".equals(action.getValue()))
            {
            return new ExtendedLanguageQueryPlusStatement(false);
            }

        throw new CohQLException("Invalid extended language command - valid syntax is: " + getSyntax());
        }

    @Override
    public String getSyntax()
        {
        return "EXTENDED LANGUAGE (ON | OFF)";
        }

    @Override
    public String getDescription()
        {
        return "Controls extended language mode.";
        }

    @Override
    public AbstractOPToken instantiateOpToken()
        {
        return new ExtendedLanguageCommandOPToken();
        }

    // ----- inner class: ExtendedLanguageCommandOPToken --------------------

    /**
     * A CohQL OPToken representing the QueryPlus "extended language" command.
     */
    public class ExtendedLanguageCommandOPToken
            extends AbstractOPToken
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a ExtendedLanguageCommandOPToken.
         */
        public ExtendedLanguageCommandOPToken()
            {
            super("extended", OPToken.IDENTIFIER_NODE, "extendedLanguageCommand");
            }

        // ----- OpToken methods --------------------------------------------

        @Override
        public Term nud(OPParser parser)
            {
            OPScanner scanner = parser.getScanner();

            scanner.advanceWhenMatching("language");

            String action = scanner.getCurrentAsStringWithAdvance();

            return Terms.newTerm(getFunctor(), AtomicTerm.createString(action));
            }
        }

    // ----- inner class: ExtendedLanguageQueryPlusStatement ----------------

    /**
     * The command to turn on or off extended CohQL.
     */
    public class ExtendedLanguageQueryPlusStatement
            extends AbstractStatement
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a ExtendedLanguageQueryPlusStatement to turn on or off
         * extended CohQL.
         *
         * @param fExtended  true to turn on extended CohQL
         */
        protected ExtendedLanguageQueryPlusStatement(boolean fExtended)
            {
            m_fExtended = fExtended;
            }

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            ctx.setExtendedLanguage(m_fExtended);

            return StatementResult.NULL_RESULT;
            }

        // ----- data members -----------------------------------------------

        /**
         * Flag indicating whether this command turns trace on or off.
         */
        protected boolean m_fExtended;
        }
    }
