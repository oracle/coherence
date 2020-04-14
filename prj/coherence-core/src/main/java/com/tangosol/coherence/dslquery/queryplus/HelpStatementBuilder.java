/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.QueryPlus;
import com.tangosol.coherence.dslquery.StatementBuilder;
import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.precedence.OPToken;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.config.expression.ParameterResolver;

import java.io.PrintWriter;

import java.util.Collection;
import java.util.List;

/**
 * A {@link StatementBuilder} that builds the QueryPlus "HELP" command.
 *
 * @author jk  2014.01.06
 * @since Coherence 12.2.1
 */
public class HelpStatementBuilder
        extends AbstractQueryPlusStatementBuilder
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public HelpQueryPlusStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
            ParameterResolver namedBindVars)
        {
        return f_command;
        }

    @Override
    public String getSyntax()
        {
        return "HELP";
        }

    @Override
    public String getDescription()
        {
        return "Displays the syntax and description of all of the commands";
        }

    @Override
    public AbstractOPToken instantiateOpToken()
        {
        return new HelpCommandOPToken();
        }

    // ----- inner class: HelpCommandOPToken --------------------------------

    /**
     * A CohQL OPToken representing the QueryPlus "help" command.
     */
    public class HelpCommandOPToken
            extends AbstractOPToken
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a HelpCommandOPToken.
         */
        public HelpCommandOPToken()
            {
            super("help", OPToken.IDENTIFIER_NODE, "showHelp");
            }

        // ----- OpToken methods --------------------------------------------

        public Term nud(OPParser parser)
            {
            return Terms.newTerm(getFunctor());
            }
        }

    // ----- inner class: HelpQueryPlusStatement ----------------------------

    /**
     * A class representing the QueryPlus "HELP" command.
     */
    public class HelpQueryPlusStatement
            extends AbstractStatement
        {

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            PrintWriter out = ctx.getWriter();

            QueryPlus.DependenciesHelper.usage(out);
            out.println();
            out.println(sEighty);
            out.println("BYE |  QUIT ");
            out.println("Exits the command line tool.");
            out.println();

            CoherenceQueryLanguage          language    = ctx.getCoherenceQueryLanguage();
            Collection<StatementBuilder<?>> colBuilders = language.getStatementBuilders().values();

            for (StatementBuilder builder : colBuilders)
                {
                out.println(sEighty);
                out.println(builder.getSyntax());
                out.println();
                out.println(builder.getDescription());
                out.println();
                }

            // Display information about the valid CohQL operators
            out.println(sEighty);
            out.println("For WHERE clauses the currently Supported conditionals are:\n"
                        + "Comparison operators: =, >, >=, <, <=, <>, [ NOT ] BETWEEN, [ NOT ] LIKE,\n"
                        + "[ NOT ] IN, IS [ NOT ] NULL, CONTAINS [ALL | ANY] *\n"
                        + "Logical operators: (AND, OR, NOT)\n"
                        + "Literal numbers, and the constants true, false, and null\n" + "\n"
                        + "Arguments to operators are properties and converted to Bean style getters and\n"
                        + "the \".\" operator may be used to make chains of calls. The optional alias may be\n"
                        + "prepended onto the beginning of these path expressions. The Pseudo functions\n"
                        + "key(), and value() may be used to specify the use of a key as in\n"
                        + "\"key() between 10 and 50\".\n"
                        + "The value() pseudo function is shorthand for the entire element as is the alias.\n"
                        + "The key() pseudo function my be specified as key(alias) for compatibility with\n"
                        + "JPQL.");

            return StatementResult.NULL_RESULT;
            }
        }

    // ----- data members ---------------------------------------------------

    private static final String sEighty = "----------------------------------------" +
                                          "----------------------------------------";
    /**
     * A {@link HelpStatementBuilder.HelpQueryPlusStatement} instance.
     */
    protected final HelpQueryPlusStatement f_command = new HelpQueryPlusStatement();
    }
