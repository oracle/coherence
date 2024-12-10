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
import com.tangosol.net.Session;

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
 * that parses a CohQL term tree to produce an instance of a {@link BackupStatement}.
 *
 * @author jk  2013.12.09
 * @since Coherence 12.2.1
 */
public class BackupStatementBuilder
        extends AbstractStatementBuilder<BackupStatementBuilder.BackupStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public BackupStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
                                   ParameterResolver namedBindVars)
        {
        String sCacheName = getCacheName(term);

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw new CohQLException("Cache name needed for backing up cache");
            }

        String sFile = getFile(term);

        if (sFile == null || sFile.isEmpty())
            {
            throw new CohQLException("File name needed for backing up cache");
            }

        return new BackupStatement(sCacheName, sFile);
        }

    @Override
    public String getSyntax()
        {
        return "BACKUP CACHE 'cache-name' [TO] [FILE] 'filename'";
        }

    @Override
    public String getDescription()
        {
        return "Backup the cache named 'cache-name' to the file named 'filename'.\n"
               + "WARNING: This backup command should not be used on active data set, as it\n"
               + "makes no provisions that ensure data consistency during the backup. Please see\n"
               + "the documentation for more detailed information.\n"
               + "Note: As of Coherence 12.2.1 this command is deprecated. Please use Persistence\n"
               + "command 'CREATE SNAPSHOT' instead.";

        }

    // ----- inner class: BackupStatement -----------------------------------

    /**
     * Implementation of the CohQL "BACKUP" command.
     */
    public static class BackupStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a BackupStatement that backs the specified cache
         * up to the specified file.
         *
         * @param sCache  the name of the cache to be backed up
         * @param sFile   the name of the file to use to back up the cache
         */
        public BackupStatement(String sCache, String sFile)
            {
            f_sCache = sCache;
            f_sFile  = sFile;
            }

        // ----- Statement interface ----------------------------------------

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            try (RandomAccessFile file = new RandomAccessFile(new File(f_sFile), "rw"))
                {
                Session    session = ctx.getSession();
                NamedCache cache   = session.getCache(f_sCache, withoutTypeChecking(),
                                                      WithClassLoader.using(NullImplementation.getClassLoader()));

                MapBackupHelper.writeMap(file, cache);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e, "Error in BACKUP");
                }

            return StatementResult.NULL_RESULT;
            }

        @Override
        public void sanityCheck(ExecutionContext ctx)
            {
            assertCacheName(f_sCache, ctx);
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            out.printf("ExternalizableHelper.writeMap(" +
                    "new RandomAccessFile(new File(\"%s\"),\"rw\")," +
                    "CacheFactory.getCache(\"%s\"))", f_sFile, f_sCache);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the cache to be backed up.
         */
        protected final String f_sCache;

        /**
         * The file name to write the cache contents to.
         */
        protected final String f_sFile;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of {@link BackupStatementBuilder}.
     */
    public static final BackupStatementBuilder INSTANCE = new BackupStatementBuilder();
    }
