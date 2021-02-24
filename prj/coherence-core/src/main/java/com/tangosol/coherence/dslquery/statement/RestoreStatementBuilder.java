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

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.util.MapBackupHelper;

import com.tangosol.net.NamedCache;

import com.tangosol.net.options.WithClassLoader;

import com.tangosol.util.NullImplementation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import java.util.List;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link RestoreStatement}.
 *
 * @author jk  2013.12.09
 * @since Coherence 12.2.1
 */
public class RestoreStatementBuilder
        extends AbstractStatementBuilder<RestoreStatementBuilder.RestoreStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public RestoreStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
                                    ParameterResolver namedBindVars)
        {
        String sCacheName = getCacheName(term);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed to restore cache");
            }

        String sFile = getFile(term);

        if (sFile == null || sFile.isEmpty())
            {
            throw new CohQLException("File name needed to restore cache");
            }

        return new RestoreStatement(sCacheName, sFile);
        }

    @Override
    public String getSyntax()
        {
        return "RESTORE CACHE 'cache-name' [FROM] [FILE] 'filename'";
        }

    @Override
    public String getDescription()
        {
        return "Restore the cache named 'cache-name' from the file named 'filename'.\n"
               + "WARNING: This restore command should not be used on active data set, as it makes\n"
               + "no provisions that ensure data consistency during the restore. Please see the\n"
               + "documentation for more detailed information.\n"
               + "Note: As of Coherence 12.2.1 this command is deprecated. Please use Persistence\n"
               + "command 'RECOVER SNAPSHOT' instead.";
        }

    // ----- inner class: RestoreStatement ----------------------------------

    /**
     * Implementation of the CohQL "RESTORE" command.
     */
    public static class RestoreStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a RestoreStatement that restores a cache from a
         * given file.
         *
         * @param sCacheName  the name of the cache to restore
         * @param sFile       the file to restore the cache from
         */
        public RestoreStatement(String sCacheName, String sFile)
            {
            f_sCacheName = sCacheName;
            f_sFile      = sFile;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            try (RandomAccessFile file = new RandomAccessFile(new File(f_sFile), "rw"))
                {
                NamedCache cache = ctx.getSession()
                        .getCache(f_sCacheName, withoutTypeChecking(),
                                  WithClassLoader.using(NullImplementation.getClassLoader()));

                MapBackupHelper.readMap(file, cache, 0, null);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e, "Error in RESTORE");
                }

            return StatementResult.NULL_RESULT;
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            out.printf("ExternalizableHelper.readMap(" +
                    "new RandomAccessFile(new File(\"%s\"),\"rw\")," +
                    "CacheFactory.getCache(\"%s\"), null)",
                    f_sFile, f_sCacheName);
            }

        // ----- data members -----------------------------------------------

        /**
         * The cache name to be used in the CohQL "backup" command.
         */
        protected final String f_sCacheName;

        /**
         * The file name to be used in the CohQL "backup" command.
         */
        protected final String f_sFile;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of a RestoreStatementBuilder.
     */
    public static final RestoreStatementBuilder INSTANCE = new RestoreStatementBuilder();
    }
