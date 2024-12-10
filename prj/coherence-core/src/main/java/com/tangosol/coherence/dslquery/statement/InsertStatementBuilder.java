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

import com.tangosol.coherence.dslquery.internal.UpdateSetListMaker;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.util.ClassHelper;

import java.io.PrintWriter;

import java.util.List;

import java.util.concurrent.CompletableFuture;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link InsertStatement}.
 *
 * @author jk  2013.12.17
 * @since Coherence 12.2.1
 */
public class InsertStatementBuilder
        extends AbstractStatementBuilder<InsertStatementBuilder.InsertStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public InsertStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
                                   ParameterResolver namedBindVars)
        {
        String             sCacheName  = getCacheName(term);
        Term               termKey     = getInsertKey(term);
        Term               termValue   = getInsertValue(term);
        UpdateSetListMaker transformer = createUpdateSetListMaker(ctx, listBindVars, namedBindVars);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed for insert command");
            }

        Object oValue = createInsertValue(termValue, transformer);
        Object oKey   = createInsertKey(termKey, transformer, oValue);

        return new InsertStatement(sCacheName, oKey, oValue);
        }

    @Override
    public String getSyntax()
        {
        return "INSERT INTO 'cache-name' [KEY (literal | new java-constructor | static method)]\n"
               + "        VALUE (literal |  new java-constructor | static method)";
        }

    @Override
    public String getDescription()
        {
        return "Insert into the cache named 'cache-name a new key value pair. If the KEY part\n"
               + "is omitted then getKey() will be sent to the VALUE object.";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create the key to use in the insert statement.
     *
     * @param termKey      the AST representing the Key term
     * @param transformer  the {@link UpdateSetListMaker} that will create the key instance
     * @param oValue       the value being inserted that will be used to call its getKey method if no
     *                     key term is present
     *
     * @return the value to use as the key for the insert
     *
     * @throws CohQLException if there are any errors creating the key instance
     */
    protected Object createInsertKey(Term termKey, UpdateSetListMaker transformer, Object oValue)
        {
        Object oKey;

        if (termKey == null)
            {
            if (oValue == null)
                {
                throw new RuntimeException("No key specified for insert");
                }

            try
                {
                oKey = ClassHelper.invoke(oValue, "getKey", new Object[]
                    {
                    });
                }
            catch (NoSuchMethodException e)
                {
                throw new RuntimeException("No key specified and missing or inaccessible method: "
                                           + oValue.getClass().getName() + ".getKey()");
                }
            catch (Exception e)
                {
                throw new CohQLException("Error creating key for insert", e);
                }
            }
        else
            {
            try
                {
                oKey = transformer.makeObjectForKey((NodeTerm) termKey, oValue);
                }
            catch (Exception e)
                {
                throw new CohQLException("Error creating key (from value) for insert", e);
                }
            }

        return oKey;
        }

    /**
     * Create the instance of the value that will be inserted into the cache.
     *
     * @param termValue    the AST term to use to create the value
     * @param transformer  the {@link UpdateSetListMaker} that can create values from AST terms
     *
     * @return an instance of a value to insert into the cache
     *
     * @throws CohQLException if any errors occur creating the value
     */
    protected Object createInsertValue(Term termValue, UpdateSetListMaker transformer)
        {
        try
            {
            if (termValue == null)
                {
                termValue = Terms.newTerm("literal", AtomicTerm.createNull());
                }

            return transformer.makeObject((NodeTerm) termValue);
            }
        catch (Exception e)
            {
            throw new CohQLException("Error creating value object", e);
            }
        }

    /**
     * Create an {@link UpdateSetListMaker}.
     *
     * @param ctx            the {@link ExecutionContext} to use
     * @param listBindVars   the indexed bind variables to pass to the UpdateSetListMaker
     * @param namedBindVars  the named bind variables to pass to the UpdateSetListMaker
     *
     * @return an UpdateSetListMaker
     */
    protected UpdateSetListMaker createUpdateSetListMaker(ExecutionContext ctx, List listBindVars,
            ParameterResolver namedBindVars)
        {
        UpdateSetListMaker transformer = new UpdateSetListMaker(listBindVars, namedBindVars,
                ctx.getCoherenceQueryLanguage());

        transformer.setExtendedLanguage(ctx.isExtendedLanguageEnabled());

        return transformer;
        }

    // ----- inner class: InsertStatement -----------------------------------

    /**
     * Implementation of the CohQL "INSERT" command.
     */
    public static class InsertStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a InsertStatement that will insert the specified
         * key and value into the specified cache.
         *
         * @param sCacheName  then name of the cache to insert the key and value into
         * @param oKey        the key of the entry to insert into the cache
         * @param oValue      the value to insert into the cache
         */
        public InsertStatement(String sCacheName, Object oKey, Object oValue)
            {
            f_sCacheName = sCacheName;
            f_oKey       = oKey;
            f_oValue     = oValue;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            Object oResult = ctx.getSession().getCache(f_sCacheName, withoutTypeChecking()).put(f_oKey, f_oValue);

            return new DefaultStatementResult(oResult);
            }

        @Override
        public CompletableFuture<StatementResult> executeAsync(ExecutionContext ctx)
            {
            return ctx.getSession()
                    .getCache(f_sCacheName, withoutTypeChecking()).async()
                    .put(f_oKey, f_oValue)
                    .thenApply(DefaultStatementResult::new);
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            out.printf("CacheFactory.getCache(\"%s\").put(%s, %s)",
                       f_sCacheName, f_oKey, f_oValue);
            }

        @Override
        public void sanityCheck(ExecutionContext ctx)
            {
            assertCacheName(f_sCacheName, ctx);
            }

        // ----- data members -----------------------------------------------

        /**
         * The cache name to be used in the CohQL "insert" command.
         */
        protected final String f_sCacheName;

        /**
         * The key to use to put the value into the cache.
         */
        protected final Object f_oKey;

        /**
         * The value being inserted into the cache.
         */
        protected final Object f_oValue;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of a InsertStatementBuilder.
     */
    public static final InsertStatementBuilder INSTANCE = new InsertStatementBuilder();
    }
