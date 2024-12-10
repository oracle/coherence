/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;


import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.coherence.persistence.PersistenceManager;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.dslquery.QueryPlus;
import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;

import com.tangosol.coherence.dslquery.statement.persistence.AbstractSnapshotStatement;
import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.persistence.CachePersistenceHelper;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.oracle.coherence.testing.AbstractRollingRestartTest;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.io.Serializable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Functional tests for CohQL persistence commands.
 *
 * @author tam  2014.09.12
 */
public abstract class AbstractCohQLPersistenceTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.management", "all");
        System.setProperty("coherence.management.remote", "true");
        System.setProperty("coherence.management.refresh.expiry", "1");
        System.setProperty("coherence.management.refresh.policy", "refresh-expired");
        System.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        AbstractFunctionalTest._startup();
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Test CohQL -f option to run scripts.
     *
     * @throws IOException if there is an error
     */
    @Test
    @Retry(count = 2)
    public void testCohQLCommands()
            throws IOException
        {
        String sServer          = "testCohQLScripts" + getPersistenceManagerName();
        String sPersistentCache = "simple-archiver";
        String sTransientCache  = "simple-transient";
        String sQuorumCache     = "dynamic-quorum";
        File   fileSnapshot     = FileHelper.createTempDir();
        File   fileActive       = FileHelper.createTempDir();
        File   fileTrash        = FileHelper.createTempDir();
        File   fileArchive      = FileHelper.createTempDir();
        File   fileTempDir      = FileHelper.createTempDir();

        Properties props  = new Properties();

        props.setProperty("test.persistence.mode", "active");
        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.archive.dir", fileArchive.getAbsolutePath());
        props.setProperty("test.persistence.members", "3");
        props.setProperty("coherence.management", "none");
        props.setProperty("coherence.management.remote", "true");
        props.setProperty("test.start.archiver", "true");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        final NamedCache       cache    = getNamedCache(sPersistentCache);
        final NamedCache       cache1   = getNamedCache(sTransientCache);
        final NamedCache       cache2   = getNamedCache(sQuorumCache);
        PartitionedService     service  = (PartitionedService) cache.getCacheService();
        String                 sService = service.getInfo().getServiceName();
        PersistenceToolsHelper helper;

        final String sEmptyClusterSnapshot = "empty-cluster";
        final String sSnapshot10000        = "snapshot-10000";
        try
            {
            CoherenceClusterMember[] arMember = new CoherenceClusterMember[3];

            arMember[0] = startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
            arMember[1] = startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);
            arMember[2] = startCacheServer(sServer + "-3", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
            for (CoherenceClusterMember member : arMember)
                {
                Eventually.assertThat(invoking(member).isServiceRunning(service.getInfo().getServiceName()), is(true));
                }
            AbstractRollingRestartTest.waitForNoOrphans(cache.getCacheService());

            helper = new PersistenceToolsHelper();
            helper.setPrintWriter(new PrintWriter(System.out));

            cache.clear();
            assertEquals(0, cache.size());

            File fileScript = new File(fileTempDir, "script1.cohql");

            writeScriptToFile(fileScript,
                              "list services;\n" +
                              "list snapshots;\n" +
                              "create snapshot '" + sEmptyClusterSnapshot + "' '" + sService + "';\n" +
                              "list snapshots;");

            runQueryPlus(fileScript);

            // list the current snapshots to be sure the new snapshot was created
            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(1));

            String[] asSnapshots = helper.listSnapshots(sService);
            assertEquals(sEmptyClusterSnapshot , asSnapshots[0]);

            populateData(cache, 10_000);
            assertEquals(10_000, cache.size());

            writeScriptToFile(fileScript,
                              "list services;\n" +
                              "list snapshots;\n" +
                              "create snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                              "validate snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                              "list snapshots;\n" +
                              "list snapshots;\n" +
                              "archive snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                              "list archived snapshots '" + sService + "'; \n" +
                              "remove snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                              "list archived snapshots '" + sService + "'; \n" +
                              "validate archived snapshot '" + sSnapshot10000 + "' '" + sService + "' verbose;" + "\n" +
                              "recover snapshot '" + sEmptyClusterSnapshot + "' '" + sService + "';\n");

            // run as invocable as system-property substitution only works this way
            RunQueryPlusAsInvocable(fileScript);

            // we should still have one local snapshot sEmptyClusterSnapshot and
            // one archived snapshot sSnapshot10000
            // list the current snapshots to be sure the new snapshot was created
            Eventually.assertThat("The following snapshots were found: " + listSnapshots(helper, sService),
                    invoking(this).listSnapshots(helper, sService).size(), is(1));

            // cache should be clear from recover command, then retrieve and recover the 10000 snapshot
            assertEquals(0, cache.size());

            writeScriptToFile(fileScript,
                              "list services;\n" +
                              "list snapshots;\n" +
                              "list archived snapshots '" + sService + "'; \n" +
                              "retrieve archived snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                              "list snapshots;\n" +
                              "recover snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" );

            runQueryPlus(fileScript);

            assertEquals(10_000, cache.size());

            String sRepeatOperation =
                    "remove snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                    "remove archived snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                    "list snapshots;\n" +
                    "create snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                    "archive snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                    "list snapshots;\n" +
                    "list archived snapshots '" + sService + "'; \n" +
                    "remove snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                    "list snapshots;\n" +
                    "retrieve archived snapshot '" + sSnapshot10000 + "' '" + sService + "';\n";

            // run a long series of commands to ensure that the operation status is valid
            writeScriptToFile(fileScript,
                              "list snapshots;\n" +
                              "remove snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                              "list snapshots;\n" +
                              "create snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                              "remove snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                              "list snapshots;\n" +
                              "create snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                              "remove snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                              "list snapshots;\n" +
                              "create snapshot '" + sSnapshot10000 + "' '" + sService + "';\n" +
                              sRepeatOperation +
                              sRepeatOperation +
                              sRepeatOperation +
                              sRepeatOperation +
                              sRepeatOperation +
                              sRepeatOperation +
                              sRepeatOperation
                               );

            runQueryPlus(fileScript);

            writeScriptToFile(fileScript,
                              "list snapshots;\n" +
                              "suspend service '" + sService + "';\n" +
                              "recover snapshot '" + sEmptyClusterSnapshot + "' '" + sService + "';\n" +
                              "resume service '" + sService + "';\n");

            QueryPlus.main(new String[] { "-s", "-c", "-t", "-f", fileScript.toString() } );
            assertEquals(0, cache.size());

            // try some individual commands using -l option
            QueryPlus.main(new String[]{"-c", "-t", "-timeout", "300000", "-l", "list snapshots"});

            QueryPlus.main(new String[] {"-s", "-c", "-t", "-timeout", "300000", "-l",  "recover snapshot '" +
                    sEmptyClusterSnapshot + "' '" + sService + "'" } );
            assertEquals(0, cache.size());

            QueryPlus.main(new String[] {"-s", "-c", "-t", "-timeout", "300000", "-l",  "recover snapshot '" +
                    sSnapshot10000 + "' '" + sService + "'" } );
            assertEquals(10_000, cache.size());

            // test macro substitution
            Calendar calendar = Calendar.getInstance();

            // snapshot name we are passing to CohQL
            String sMacroSnapshotName    = "backup-%w-%M";

            // the snapshot name that should be present
            String   sExpectedSnapshotName = sMacroSnapshotName
              .replaceAll("%w", AbstractSnapshotStatement.WEEKDAY_NAME.format(
                      calendar.getTime()))
              .replaceAll("%M", AbstractSnapshotStatement.MONTH_NAME.format(calendar.getTime()));

            writeScriptToFile(fileScript,
                              "create snapshot '" + sMacroSnapshotName + "' '" + sService + "'; \n");
            runQueryPlus(fileScript);

            Eventually.assertThat("The following snapshots were found: " +
                                  listSnapshots(helper, sService), invoking(this).listSnapshots(helper, sService).size(), is(3));

            List<String> listSnapshots = listSnapshots(helper, sService);
            assertTrue("list of snapshots does not include " + sExpectedSnapshotName, listSnapshots.contains(sExpectedSnapshotName));

            // execute all the operations to ensure all works ok
            writeScriptToFile(fileScript,
                    "archive  snapshot '" + sMacroSnapshotName + "' '" + sService + "'; \n" +
                    "remove   snapshot '" + sMacroSnapshotName + "' '" + sService + "'; \n" +
                    "list snapshots;\n" +
                    "retrieve archived snapshot '" + sMacroSnapshotName + "' '" + sService + "'; \n" +
                    "remove   archived snapshot '" + sMacroSnapshotName + "' '" + sService + "'; \n" +
                    "list archived snapshots '" + sService + "'; \n" +
                    "recover  snapshot  '" + sMacroSnapshotName + "' '" + sService + "'; \n" +
                    "remove    snapshot '" + sMacroSnapshotName + "' '" + sService + "'; \n");

            runQueryPlus(fileScript);

            Eventually.assertDeferred(
                    "The following snapshots were found: " + listSnapshots(helper, sService),
                    () -> listSnapshots(helper, sService).size(), is(2));
            }
        finally
            {
            stopAllApplications();

            CacheFactory.shutdown();

            FileHelper.deleteDirSilent(fileActive);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            FileHelper.deleteDirSilent(fileArchive);
            FileHelper.deleteDirSilent(fileTempDir);
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Run QueryPlus and read commands from a file.
     *
     * @param fileScript  the file to read commands from
     */
    protected void runQueryPlus(File fileScript)
        {
        QueryPlus.main(new String[] { "-s", "-c", "-t", "-timeout", "300000", "-f", fileScript.getAbsolutePath() } );
        }

    /**
     * Run QueryPlus and read commands from a file as an invocable so system-property
     * substitution is carried out correctly.
     *
     * @param fileScript  the file to read commands from
     */
    protected void RunQueryPlusAsInvocable(File fileScript)
        {
        InvocationService invocationService = (InvocationService) getFactory().ensureService("InvocationService");
        assertTrue(invocationService != null);

        Cluster     cluster        = invocationService.getCluster();
        Set<Member> setMembers     = new HashSet<>();
        Member      memberInvoking = null;

        Iterator<Member> iter      = cluster.getMemberSet().iterator();
        while (iter.hasNext())
            {
            Member member = iter.next();

            if (!member.equals(cluster.getLocalMember()))
                {
                memberInvoking = member;
                setMembers.add(memberInvoking);
                break;
                }
            }

        assertTrue("No members are running InvocationService", setMembers.size() != 0);

        try
            {
            CohQLInvocable invocable = new CohQLInvocable(fileScript.getAbsolutePath());
            Map    mapResult = invocationService.query(invocable, setMembers);
            Object oResult   = mapResult.get(memberInvoking);

            if (!oResult.equals(CohQLInvocable.OK))
                {
                throw new RuntimeException((String) oResult);
                }
            }
        catch (Exception ee)
            {
            throw CachePersistenceHelper.ensurePersistenceException(ee, "Invocation failed");
            }
        }

    /**
     * Write the contents of the String to the file.
     *
     * @param file     the file to write to
     * @param sScript  the String to write to the file
     *
     * @throws IOException  if any I/O related issues
     */
    protected void writeScriptToFile(File file, String sScript)
            throws IOException
        {
        final String EXIT = "whenever cohqlerror then exit;\n";

        try (PrintWriter out = new PrintWriter(new FileWriter(file, false)))
            {
            out.println(EXIT);
            out.println(sScript);
            }

        System.out.println("Written the following to " + file.getAbsolutePath());
        System.out.println(sScript);
        System.out.flush();
        }

    /**
     * Populate a cache with data.
     *
     * @param cache    the cache to populate
     * @param cMax  the amount of objects to insert
     */
    protected void populateData(NamedCache cache, int cMax)
        {
        Map<Integer, Integer> mapBuffer = new HashMap<>();
        final int             BATCH     = 1000;

        for (int i = 0; i < cMax; i++)
            {
            mapBuffer.put(Integer.valueOf(i), Integer.valueOf(i));

            if (i % BATCH == 0)
                {
                cache.putAll(mapBuffer);
                mapBuffer.clear();
                }
            }

        if (!mapBuffer.isEmpty())
            {
            cache.putAll(mapBuffer);
            }
        }

    /**
     * A helper method to call the static {@link PersistenceTestHelper#listSnapshots(String)}
     * method. This allows us to use this method in Eventually.assertThat(Deferred, Matcher)
     * tests.
     */
    public List<String> listSnapshots(PersistenceToolsHelper helper, String sService)
        {
        String[] asSnapshots = helper.listSnapshots(sService);
        return Arrays.asList(asSnapshots);
        }

    // ----- inner classes -----------------------------------------------

    /**
     * A JUnit 4 rule that repeats a test annotated with {@link Retry} a given number of times in case of failure.
     */
    public static class RetryRule
            implements TestRule
        {
        // ----- TestRule methods -----------------------------------------------

        public Statement apply(Statement base, Description description)
            {
            Retry retry = description.getAnnotation(Retry.class);
            if (retry == null)
                {
                return base;
                }
            int retryCount = retry.count();
            if (retryCount <= 0)
                {
                throw new IllegalArgumentException("@" + Retry.class.getSimpleName() + " 'count' parameter has to be a positive number.");
                }
            return statement(base, description, retryCount);
            }

        // ----- helper methods ---------------------------------------------

        private Statement statement(Statement base, Description description, int retryCount)
            {
            return new Statement()
                {
                @Override
                public void evaluate() throws Throwable
                    {
                    Throwable throwable = null;
                    for (int i = 0; i < retryCount; i++)
                        {
                        try
                            {
                            base.evaluate();
                            return;
                            }
                        catch (Throwable t)
                            {
                            throwable = t;
                            }
                        }
                    System.err.println(description.getDisplayName() + ": giving up after " + retryCount + " failure(s)");
                    throw throwable;
                    }
                };
            }
        }

    /**
     * Target annotated with Retry will be repeatedly executed a given number of times in case of failure.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface Retry
        {
        /**
         * @return the number of attempts to execute this method before the failure is
         *         propagated through
         */
        int count() default 2;
        }

    /**
     * This is executed from within the cluster as the "client"
     * does not have the correct archiver setup.
     */
    private static class CohQLInvocable
                extends AbstractInvocable
                implements Serializable
        {
        /**
         * run CohQL with specified file.
         *
         * @param sFileName  file name to read CohQL commands from
         */
        public CohQLInvocable(String sFileName)
            {
            f_sFileName = sFileName;
            }

        @Override
        public void run()
            {
            try
                {
                QueryPlus.main(new String[]{"-s", "-c", "-t", "-f", f_sFileName});
                }
            catch (Exception e)
                {
                setResult(e.getMessage() + " : " + e.getCause());
                return;
                }
                setResult(OK);
            }

            /**
             * Indicates that result was ok.
             */
            public static final String OK = "OK";

            // ----- data members -------------------------------------------

            private final String f_sFileName;
        }


    // ----- factory methods ------------------------------------------------

    /**
     * Create a PersistenceManager to validate results of tests.
     *
     * @param file  the persistence root
     *
     * @return a new PersistenceManager for the given root directory
     *
     * @throws IOException
     */
    protected abstract PersistenceManager<ReadBuffer> createPersistenceManager(File file)
            throws IOException;

    // ----- data members ------------------------------------------------------

    @Rule
    public RetryRule rule = new RetryRule();

    // ----- accessors ------------------------------------------------------

    /**
     * Return a name for the PersistenceManager being used by the tests.
     *
     * @return a name used in log files, etc.
     */
    public abstract String getPersistenceManagerName();

    /**
     * {@inheritDoc}
     */
    public abstract String getCacheConfigPath();

    /**
     * Return the project name.
     */
    public static String getProjectName()
        {
        return "persistence";
        }
    }