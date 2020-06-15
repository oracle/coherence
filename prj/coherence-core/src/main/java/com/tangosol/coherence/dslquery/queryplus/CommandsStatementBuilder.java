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
 * A class that builds the QueryPlus "COMMANDS" command.
 *
 * @author jk  2014.01.06
 * @since Coherence 12.2.1
 */
public class CommandsStatementBuilder
        extends AbstractQueryPlusStatementBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CommandsStatementBuilder that builds
     * a {@link CommandsStatementBuilder.CommandsQueryPlusStatement}.
     */
    public CommandsStatementBuilder()
        {
        f_command = new CommandsQueryPlusStatement();
        }

    // ----- StatementBuilder interface -------------------------------------

    @Override
    public CommandsQueryPlusStatement realize(ExecutionContext ctx, NodeTerm term,
            List listBindVars, ParameterResolver namedBindVars)
        {
        return f_command;
        }

    @Override
    public String getSyntax()
        {
        return "COMMANDS";
        }

    @Override
    public String getDescription()
        {
        return "Print a simple list of commands without explanations.";
        }

    @Override
    public AbstractOPToken instantiateOpToken()
        {
        return new CommandsOPToken();
        }

    // ----- inner class: CommandsOPToken -----------------------------------

    /**
     * A CohQL OPToken representing the QueryPlus "commands" command.
     */
    public class CommandsOPToken
            extends AbstractOPToken
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a CommandsOPToken.
         */
        public CommandsOPToken()
            {
            super("commands", OPToken.IDENTIFIER_NODE, "showCommands");
            }

        // ----- AbstractOPToken methods ----------------------------------------

        public Term nud(OPParser parser)
            {
            return Terms.newTerm(getFunctor());
            }
        }

    // ----- inner class: CommandsOPToken -----------------------------------

    /**
     * The implementation of the QueryPlus "commands" command.
     */
    public class CommandsQueryPlusStatement
            extends AbstractStatement
        {
        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            PrintWriter out = ctx.getWriter();

            QueryPlus.DependenciesHelper.usage(out);
            out.println("BYE |  QUIT");

            CoherenceQueryLanguage          language    = ctx.getCoherenceQueryLanguage();
            Collection<StatementBuilder<?>> colBuilders = language.getStatementBuilders().values();

            for (StatementBuilder builder : colBuilders)
                {
                out.println(builder.getSyntax());
                }

            return StatementResult.NULL_RESULT;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * An instance of the {@link CommandsStatementBuilder.CommandsQueryPlusStatement}.
     */
    protected final CommandsQueryPlusStatement f_command;
    }
