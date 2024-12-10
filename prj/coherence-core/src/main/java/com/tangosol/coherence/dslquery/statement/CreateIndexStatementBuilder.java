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
 * that parses a CohQL term tree to produce an instance of a {@link CreateIndexStatement}.
 *
 * @author jk  2013.12.11
 * @since Coherence 12.2.1
 */
public class CreateIndexStatementBuilder
        extends AbstractStatementBuilder<CreateIndexStatementBuilder.CreateIndexStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public CreateIndexStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
            ParameterResolver namedBindVars)
        {
        String sCacheName = getCacheName(term);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed for create index");
            }

        Term termExtractor = getExtractor(term);

        if (termExtractor == null || termExtractor.length() == 0)
            {
            throw new CohQLException("ValueExtractor(s) needed for create index");
            }

        SelectListMaker transformer = new SelectListMaker(listBindVars, namedBindVars,
                ctx.getCoherenceQueryLanguage());

        transformer.makeSelectsForCache(sCacheName, (NodeTerm) termExtractor);

        ValueExtractor extractor = transformer.getResultsAsValueExtractor();

        if (extractor == null)
            {
            throw new CohQLException("ValueExtractor(s) needed for create index");
            }

        return new CreateIndexStatement(sCacheName, extractor);
        }

    @Override
    public String getSyntax()
        {
        return "(ENSURE | CREATE) INDEX [ON] 'cache-name' value-extractor-list";
        }

    @Override
    public String getDescription()
        {
        return "Make sure the Index on 'cache-name' that is made from the value-extractor-list\nexists.";
        }

    // ----- inner class: CreateIndexStatement ------------------------------

    /**
     * Implementation of the CohQL "CREATE INDEX" command.
     */
    public static class CreateIndexStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a CreateIndexStatement that will create an index on the
         * specified cache using the specified {@link ValueExtractor}.
         *
         * @param sCache     the name of the cache to create the index on
         * @param extractor  the ValueExtractor to use to create the index
         */
        public CreateIndexStatement(String sCache, ValueExtractor extractor)
            {
            f_sCache    = sCache;
            f_extractor = extractor;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            ctx.getSession().getCache(f_sCache, withoutTypeChecking()).addIndex(f_extractor, true, null);

            return StatementResult.NULL_RESULT;
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            out.printf("CacheFactory.getCache(\"%s\")).addIndex(%s, true, null)",
                       f_sCache, f_extractor);
            }

        @Override
        public void sanityCheck(ExecutionContext ctx)
            {
            assertCacheName(f_sCache, ctx);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the cache the index will be added to.
         */
        protected final String f_sCache;

        /**
         * The {@link ValueExtractor} to be used to create the index.
         */
        protected final ValueExtractor f_extractor;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of a CreateIndexStatementBuilder.
     */
    public static final CreateIndexStatementBuilder INSTANCE = new CreateIndexStatementBuilder();
    }
