/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.Statement;
import com.tangosol.coherence.dslquery.StatementBuilder;

import com.tangosol.coherence.dsltools.precedence.IdentifierOPToken;

import java.io.PrintWriter;

/**
 * This is the base class for command builders that are specific to the QueryPlus
 * tool rather than general CohQL statements.
 *
 * @author jk  2014.01.06
 * @since Coherence 12.2.1
 */
public abstract class AbstractQueryPlusStatementBuilder
        implements StatementBuilder<Statement>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a AbstractQueryPlusStatementBuilder.
     */
    protected AbstractQueryPlusStatementBuilder()
        {
        }

    // ----- AbstractQueryPlusStatementBuilder methods ----------------------

    /**
     * Return the OPToken for this command.
     *
     * @return the OPToken for this command
     */
    public abstract AbstractOPToken instantiateOpToken();

    // ----- inner class: AbstractOPToken -----------------------------------

    /**
     * An {@link com.tangosol.coherence.dsltools.precedence.OPToken} implementation
     * that holds the name of the functor associated to an OPToken.
     */
    public abstract class AbstractOPToken
            extends IdentifierOPToken
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an AbstractOpToken.
         *
         * @param sIdentifier  the identifier of this AbstractOPToken
         * @param sNudASTName  the name of this token in an AST
         * @param sFunctor     the name of this token's functor
         */
        public AbstractOPToken(String sIdentifier, String sNudASTName, String sFunctor)
            {
            super(sIdentifier, sNudASTName);

            f_sFunctor = sFunctor;
            }

        // ----- accessor methods -------------------------------------------

        /**
         * Return the functor for this OPToken.
         *
         * @return the functor string for this OPToken
         */
        public String getFunctor()
            {
            return f_sFunctor;
            }

        // ----- data members -----------------------------------------------

        /**
         * The Functor string used by the parser for this token.
         */
        protected final String f_sFunctor;
        }

    // ----- inner class: AbstractStatement -------------------------------------

    /**
     * An abstract base class that allows sub classes to implement the applicable
     * methods on {@link Statement}.
     */
    public abstract class AbstractStatement
            implements Statement
        {
        // ----- Statement interface ----------------------------------------

        @Override
        public void sanityCheck(ExecutionContext ctx)
            {
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            }

        @Override
        public String getExecutionConfirmation(ExecutionContext ctx)
            {
            return null;
            }
        }
    }
