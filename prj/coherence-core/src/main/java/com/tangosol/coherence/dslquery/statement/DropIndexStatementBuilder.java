/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.StatementResult;

import com.tangosol.coherence.dslquery.internal.SelectListMaker;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.util.ValueExtractor;

import java.io.PrintWriter;

import java.util.List;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link DropIndexStatement}.
 *
 * @author jk  2013.12.11
 * @since Coherence 12.2.1
 */
public class DropIndexStatementBuilder
        extends AbstractStatementBuilder<DropIndexStatementBuilder.DropIndexStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public DropIndexStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
                                      ParameterResolver namedBindVars)
        {
        String sCacheName = getCacheName(term);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed for drop index");
            }

        Term termExtractor = getExtractor(term);

        if (termExtractor == null || termExtractor.length() == 0)
            {
            throw new CohQLException("ValueExtractor(s) needed for drop index");
            }

        SelectListMaker transformer = new SelectListMaker(listBindVars, namedBindVars,
                ctx.getCoherenceQueryLanguage());

        transformer.makeSelectsForCache(sCacheName, (NodeTerm) termExtractor);

        ValueExtractor extractor = transformer.getResultsAsValueExtractor();

        if (extractor == null)
            {
            throw new CohQLException("ValueExtractor(s) needed for drop index");
            }

        return new DropIndexStatement(sCacheName, extractor);
        }

    @Override
    public String getSyntax()
        {
        return "DROP INDEX [ON] 'cache-name' value-extractor-list";
        }

    @Override
    public String getDescription()
        {
        return "Remove the index made from value-extractor-list from cache 'cache-name'.";
        }

    // ----- inner class: BackupStatement -----------------------------------

    /**
     * Implementation of the CohQL "create index" command.
     */
    public static class DropIndexStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a DropIndexStatement that will drop the index
         * created with the specified {@link com.tangosol.util.ValueExtractor} from the
         * cache with the specified name.
         *
         * @param sCacheName  the name of the cache to drop the index on
         * @param extractor   the ValueExtractor to use to identify the index to drop
         */
        public DropIndexStatement(String sCacheName, ValueExtractor extractor)
            {
            f_sCacheName = sCacheName;
            f_extractor  = extractor;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            ctx.getSession().getCache(f_sCacheName, withoutTypeChecking()).removeIndex(f_extractor);

            return StatementResult.NULL_RESULT;
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            out.printf("CacheFactory.getCache(\"%s\")).removeIndex(%s)",
                       f_sCacheName, f_extractor);
            }

        @Override
        public void sanityCheck(ExecutionContext ctx)
            {
            assertCacheName(f_sCacheName, ctx);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the cache the index will be added to.
         */
        protected final String f_sCacheName;

        /**
         * The {@link ValueExtractor} to be used to create the index.
         */
        protected final ValueExtractor f_extractor;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of DropIndexStatementBuilder.
     */
    public static final DropIndexStatementBuilder INSTANCE = new DropIndexStatementBuilder();
    }
