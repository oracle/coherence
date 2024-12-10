/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement.persistence;

import com.oracle.coherence.persistence.OfflinePersistenceInfo;
import com.oracle.coherence.persistence.PersistenceStatistics;
import com.oracle.coherence.persistence.PersistenceTools;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dslquery.StatementResult;
import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;
import com.tangosol.coherence.dslquery.statement.AbstractStatement;
import com.tangosol.coherence.dslquery.statement.AbstractStatementBuilder;
import com.tangosol.coherence.dslquery.statement.FormattedMapStatementResult;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.persistence.CachePersistenceHelper;

import java.io.File;
import java.io.PrintWriter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementBuilder}
 * that parses a CohQL term tree to produce an instance of a {@link ValidateSnapshotStatement}.
 *
 * @author tam  2014.08.06
 * @since Coherence 12.2.1
 */
public class ValidateSnapshotStatementBuilder
        extends AbstractStatementBuilder<ValidateSnapshotStatementBuilder.ValidateSnapshotStatement>
    {
    // ----- StatementBuilder interface -------------------------------------

    @Override
    public ValidateSnapshotStatement realize(ExecutionContext ctx, NodeTerm term, List listBindVars,
        ParameterResolver namedBindVars)
        {
        String  sSnapshotDir  = atomicStringValueOf(term.findAttribute("snapshotdirectory"));
        String  sSnapshotName = atomicStringValueOf(term.findAttribute("snapshotname"));
        String  sServiceName  = atomicStringValueOf(term.findAttribute("servicename"));

        boolean fVerbose      = "true".equals(atomicStringValueOf(term.findAttribute("verbose")));
        boolean fArchived     = "archived".equals(atomicStringValueOf(term.findAttribute("type")));

        return new ValidateSnapshotStatement(sSnapshotDir, fVerbose, fArchived,
                                             sSnapshotName, sServiceName);
        }

    @Override
    public String getSyntax()
        {
        return "VALIDATE SNAPSHOT 'snapshot-directory' [VERBOSE]\n"
               + "VALIDATE SNAPSHOT 'snapshot-name' 'service-name' [VERBOSE]\n"
               + "VALIDATE ARCHIVED SNAPSHOT 'snapshot-name' 'service-name' [VERBOSE]";
        }

    @Override
    public String getDescription()
        {
        return   "Validate a snapshot (or archived snapshot) to ensure contents are valid.\n"
               + "For snapshots, either a directory location can be specified or a snapshot\n"
               + "name and service name. When a directory name is not supplied, the\n"
               + "snapshot directory information is retrieved via the operational configuration.\n"
               + "Verbose mode provides more detailed information regarding the contents of the\n"
               + "snapshot at the cost of increased execution time and resouce usage.\n"
               + "Note: the relevant operational and cache configuration pertaining\n"
               + "to the cluster and services to be valdiated must be available to be loaded.";
        }

    /**
     * Implementation of the CohQL "VALIDATE [ARCHIVED] SNAPSHOT" command.
     */
    public static class ValidateSnapshotStatement
            extends AbstractStatement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a ValidateSnapshotStatement the will validate a snapshot on disk
         * to ensure the files are consistent.
         *
         * @param sSnapshotDir   the snapshot directory to validate
         * @param fVerbose       indicates if verbose output should be displayed
         * @param fArchived      indicates if an archived snapshot should be validated
         * @param sSnapshotName  the snapshot name to validate
         * @param sServiceName   the service name to validate
         */
        public ValidateSnapshotStatement(String sSnapshotDir, boolean fVerbose, boolean fArchived,
                                         String sSnapshotName,String sServiceName)
            {
            m_sSnapshotDir  = sSnapshotDir;
            f_fVerbose      = fVerbose;
            f_Archived      = fArchived;
            f_sSnapshotName = AbstractSnapshotStatement.replaceDateMacros(sSnapshotName);
            f_sServiceName  = sServiceName;
            }

        @Override
        public StatementResult execute(ExecutionContext ctx)
            {
            Object      oResult = null;
            PrintWriter out     = ctx.getWriter();

            ConfigurableCacheFactory ccf = ctx.getCacheFactory();

            try
                {
                PersistenceTools tools = null;

                if (f_Archived)
                    {
                    out.println("Validating archived snapshot " + f_sSnapshotName);
                    out.flush();
                    if (ccf instanceof ExtensibleConfigurableCacheFactory)
                        {
                        tools = CachePersistenceHelper.getArchiverPersistenceTools(
                          (ExtensibleConfigurableCacheFactory) ccf,f_sSnapshotName, f_sServiceName);
                        }
                    else
                        {
                        throw new UnsupportedOperationException("ConfigurableCacheFactory is not an instance of ExtensibleConfigurableCacheFactory");
                        }
                    }
                else
                    {
                    File fileSnapshot = null;

                    if (m_sSnapshotDir == null || m_sSnapshotDir.isEmpty())
                        {
                        // user has specified snapshot name and service name
                        if (f_sSnapshotName == null || f_sSnapshotName.isEmpty() ||
                            f_sServiceName  == null || f_sServiceName.isEmpty() )
                            {
                            throw new CohQLException("You must specify either a directory or snapshot and service names.");
                            }

                        fileSnapshot   = PersistenceToolsHelper.getSnapshotDirectory(ccf, f_sSnapshotName, f_sServiceName);
                        m_sSnapshotDir = fileSnapshot.getAbsolutePath();
                        }
                    else
                        {
                        // user has specified just directory
                        fileSnapshot = new File(m_sSnapshotDir);
                        }

                    if (!fileSnapshot.exists() || !fileSnapshot.isDirectory() || !fileSnapshot.canExecute())
                        {
                        throw new CohQLException("The directory '" + m_sSnapshotDir
                                                 + "' does not exist or is not readable");
                        }

                    out.println("Validating snapshot directory '" + m_sSnapshotDir + "'");
                    out.flush();

                    tools = CachePersistenceHelper.getSnapshotPersistenceTools(fileSnapshot);
                    }

                tools.validate();

                if (f_fVerbose)
                    {
                    PersistenceStatistics  stats      = tools.getStatistics();
                    OfflinePersistenceInfo info       = tools.getPersistenceInfo();

                    Map<String, String>    mapResults = new LinkedHashMap<>();

                    mapResults.put("Partition Count", String.valueOf(info.getPartitionCount()));
                    if (f_Archived)
                        {
                        mapResults.put("Archived Snapshot", "Name=" + f_sSnapshotName +
                                       ", Service=" + f_sServiceName);
                        mapResults.put("Original Storage Format", info.getStorageFormat());
                        }
                    else
                        {
                        mapResults.put("Directory", m_sSnapshotDir);
                        mapResults.put("Storage Format", info.getStorageFormat());
                        }

                    mapResults.put("Storage Version", String.valueOf(info.getStorageVersion()));
                    mapResults.put("Implementation Version", String.valueOf(info.getImplVersion()));
                    mapResults.put("Number of Partitions Present", String.valueOf(info.getGUIDs().length));
                    mapResults.put("Is Complete?", String.valueOf(info.isComplete()));
                    mapResults.put("Is Archived Snapshot?", String.valueOf(info.isArchived()));
                    mapResults.put("Persistence Version", String.valueOf(info.getPersistenceVersion()));
                    mapResults.put("Statistics", "");

                    if (stats != null)
                        {
                        for (String sCacheName : stats)
                            {
                            StringBuilder sb = new StringBuilder();

                            sb.append("Size=" + stats.getCacheSize(sCacheName));
                            sb.append(", Bytes=" + stats.getCacheBytes(sCacheName));
                            sb.append(", Indexes=" + stats.getIndexCount(sCacheName));
                            sb.append(", Triggers=" + stats.getTriggerCount(sCacheName));
                            sb.append(", Listeners=" + stats.getListenerCount(sCacheName));
                            sb.append(", Locks=" + stats.getLockCount(sCacheName));
                            mapResults.put(sCacheName, sb.toString());
                            }
                        }

                    oResult = mapResults;
                    }
                else
                    {
                    oResult = AbstractSnapshotStatement.SUCCESS;
                    }

                }
            catch (Exception e)
                {
                throw PersistenceToolsHelper.ensureCohQLException(e, "Error in VALIDATE SNAPSHOT:");
                }

            FormattedMapStatementResult result = new FormattedMapStatementResult(oResult);

            if (oResult instanceof Map)
                {
                result.setColumnHeaders(new String[] {"Attribute", "Value"});
                }

            return result;
            }

        @Override
        public void showPlan(PrintWriter out)
            {
            }

        // ----- data members -------------------------------------------

        /**
         * The directory to validate.
         */
        private String m_sSnapshotDir;

        /**
         * Indicates if verbose mode is on.
         */
        private final boolean f_fVerbose;

        /**
         * True if an archived snapshot should be validated.
         */
        private final boolean f_Archived;

        /**
         * The snapshot name to validate for archived snapshots;
         */
        private final String f_sSnapshotName;

        /**
         * The service name to validate for archived snapshots.
         */
        private final String f_sServiceName;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of {@link ValidateSnapshotStatementBuilder}.
     */
    public static final ValidateSnapshotStatementBuilder INSTANCE = new ValidateSnapshotStatementBuilder();
    }