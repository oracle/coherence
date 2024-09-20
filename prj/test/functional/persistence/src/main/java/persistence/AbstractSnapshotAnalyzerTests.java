/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.coherence.persistence.PersistenceStatistics;
import com.oracle.coherence.persistence.PersistenceTools;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.oracle.bedrock.runtime.coherence.callables.GetServiceStatus;
import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.persistence.CachePersistenceHelper;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;

import java.lang.reflect.InvocationTargetException;

import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import javax.management.MBeanException;

/**
 * Tools to test snapshot analyzer functionality.
 */
public abstract class AbstractSnapshotAnalyzerTests
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
        System.setProperty("coherence.management.refresh.expiry", "1s");
        System.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        AbstractFunctionalTest._startup();
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Test SnapshotAnalyzer functionality.
     *
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws MBeanException
     * @throws NoSuchMethodException
     */
    @Test
    public void testSnapshotAnalyzer()
            throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, MBeanException
        {
        boolean    fActive          = true;

        File       fileSnapshot     = FileHelper.createTempDir();
        File       fileActive       = fActive ? FileHelper.createTempDir() : null;
        File       fileTrash        = FileHelper.createTempDir();
        File       fileArchive      = FileHelper.createTempDir();

        String     sServer          = "testSnapshotAnalyzer";
        String     sPersistentCache = "simple-archiver";
        String     sArchiver        = "simple-directory-archiver";
        Properties props            = new Properties();

        props.setProperty("test.persistence.mode", fActive ? "active" : "on-demand");
        props.setProperty("test.persistence.active.dir", fActive ? fileActive.getAbsolutePath() : "");
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.archive.dir", fileArchive.getAbsolutePath());
        props.setProperty("coherence.management", "all");
        props.setProperty("coherence.management.refresh.expiry", "1s");
        props.setProperty("coherence.management.remote", "true");
        props.setProperty("test.start.archiver", "true");
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        System.setProperty("coherence.distributed.localstorage", "false");

        final NamedCache cache            = getNamedCache(sPersistentCache);
        CacheService     service          = cache.getCacheService();
        Cluster          cluster          = service.getCluster();
        String           sService         = service.getInfo().getServiceName();

        String           sSafeClusterName = FileHelper.toFilename(cluster.getClusterName());
        String           sSafeServiceName = FileHelper.toFilename(sService);

        CoherenceClusterMember clusterMember = startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(),
                                                   props);

        startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);

        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(3));
        waitForBalanced(service);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning(sService), is(true));

        PersistenceTestHelper helper = new PersistenceTestHelper();

        final String sEmptySnapshot = "empty-cluster";
        final String sSnapshot10000 = "snapshot-10000";

        try
            {
            // wait till status HA
            Eventually.assertThat(clusterMember, new GetServiceStatus(sService), is(ServiceStatus.NODE_SAFE));

            cache.clear();
            assertTrue(cache.size() == 0);

            // create 2 snapshots and archive them
            // create an empty-cluster snapshot
            helper.createSnapshot(sService, sEmptySnapshot);

            // create a second snapshot with 10,000 entries
            PersistenceTestHelper.populateData(cache, 10000);
            assertTrue(cache.size() == 10000);

            helper.createSnapshot(sService, sSnapshot10000);

            // archive the snapshots
            helper.archiveSnapshot(sService, sEmptySnapshot);
            helper.archiveSnapshot(sService, sSnapshot10000);

            String[] asArchivedSnapshots =
                (String[]) helper.invokeOperationWithReturn("listArchivedSnapshots", sService,
                    new String[] {}, new String[] {});

            assertTrue(asArchivedSnapshots != null && asArchivedSnapshots.length == 2
                       && (sSnapshot10000.equals(asArchivedSnapshots[0])
                           || sSnapshot10000.equals(asArchivedSnapshots[1])));

            //
            // Setup Complete - Start the tests
            //

            // run through valid cases for snapshots
            File fileSnapshotDirectory = new File(fileSnapshot, sSafeClusterName);

            fileSnapshotDirectory = new File(fileSnapshotDirectory, sSafeServiceName);

            File fileArchiveDirectory = new File(fileArchive, sSafeClusterName);

            fileArchiveDirectory = new File(fileArchiveDirectory, sSafeServiceName);

            Assert.assertTrue(fileSnapshotDirectory.exists() && fileSnapshotDirectory.canRead());
            Assert.assertTrue(fileArchiveDirectory.exists() && fileArchiveDirectory.canRead());

            File fileEmptySnapshot = new File(fileSnapshotDirectory, sEmptySnapshot);
            File file10000Snapshot = new File(fileSnapshotDirectory, sSnapshot10000);

            File fileEmptyArchive  = new File(fileArchiveDirectory, sEmptySnapshot);
            File file10000Archive  = new File(fileArchiveDirectory, sSnapshot10000);

            // test happy path
            testSnapshotAnalyzer(fileEmptySnapshot, true);    // valid
            testSnapshotAnalyzer(file10000Snapshot, true);    // valid

            System.out.println("Archive: " + fileEmptyArchive);

            System.out.println("test.persistence.archive.dir=" + System.getProperty("test.persistence.archive.dir"));

            testArchivedSnapshotAnalyzer(sEmptySnapshot,  sSafeServiceName, true);    // valid
            testArchivedSnapshotAnalyzer(sSnapshot10000,  sSafeServiceName, true);    // valid

            // now corrupt the snapshots and be sure we get exceptions

            // remove one of the files
            File fileRandom = getRandomFromDirectory(fileEmptyArchive, true);

            if (fileRandom != null)
                {
                fileRandom.delete();
                testArchivedSnapshotAnalyzer(sEmptySnapshot, sSafeServiceName, false);    // invalid
                }

            boolean notFound  = true;
            int     cAttempts = 0;
            while (notFound && cAttempts++ <= ((PartitionedService) service).getPartitionCount())
                {
                fileRandom   = getRandomFromDirectory(fileEmptySnapshot, false);
                File[] files = fileRandom.listFiles();
                // since lazy store open, an empty snapshot directory only contains the global partition,
                // all other directories are empty
                // if the global partition is deleted, the snapshot is no longer valid
                if (files != null && files.length == 0)
                    {
                    notFound = false;
                    }
                }
            if (notFound)
                {
                fail("Could not find suitable directory for random deletion");
                }

            if (fileRandom != null)
                {
                FileHelper.deleteDir(fileRandom);

                // the result of this should be valid as the analyzer treats a local snapshot
                // as valid if one more more partitions are not present as it could be
                // local to this machine only
                testSnapshotAnalyzer(fileEmptySnapshot, true);    // valid
                }

            // get a random file from the 10000 archive and corrupt the file
            fileRandom = null;

            int i = 10;       // try 10 times

            while (fileRandom == null)
                {
                fileRandom = getRandomFromDirectory(file10000Archive, true);

                // ignore meta.properties
                if (fileRandom != null)
                    {
                    corruptFile(fileRandom);
                    }

                if (--i <= 0)
                    {
                    break;    // just in case we can't find a file - don't try for ever
                    }
                }

            // now analyse the archived snapshot again - this time it should be invalid
            testArchivedSnapshotAnalyzer(sSnapshot10000, sSafeServiceName, false);    // invalid

            // corrupt a snapshot
            i          = 10;
            fileRandom = null;

            while (fileRandom == null)
                {
                fileRandom = getRandomFromDirectory(file10000Snapshot, true);

                if (fileRandom != null)
                    {
                    if (!fileRandom.isDirectory())
                        {
                        fileRandom = null;
                        }
                    else
                        {
                        // this file will actually be a directory so lets get a list of files
                        // and corrupt every file except the meta.properties
                        File[] afileList = fileRandom.listFiles();

                        for (File file : afileList)
                            {
                            if (file.getName().indexOf(CachePersistenceHelper.META_FILENAME) == -1)
                                {
                                corruptFile(file);
                                }
                            }
                        }
                    }

                if (--i <= 0)
                    {
                    break;    // just in case we can't find a file - don't try for ever
                    }
                }

            // now analyse the archived snapshot again - this time it should be invalid

            if (fileRandom != null)
                {
                testArchivedSnapshotAnalyzer(sSnapshot10000, sSafeServiceName, false);    // invalid
                }

            // some basic invalid directory tests
            testArchivedSnapshotAnalyzer("rubbish you know", sSafeServiceName, false);
            testSnapshotAnalyzer(new File("a_file_that_definitely_doesnt_exist"), false);

            stopAllApplications();
            }
        finally
            {
            if (fActive)
                {
                FileHelper.deleteDirSilent(fileActive);
                }

            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            FileHelper.deleteDirSilent(fileArchive);
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Corrupt a file by writing some bytes.
     *
     * @param file  the file to corrupt
     *
     * @throws IOException
     */
    private void corruptFile(File file)
            throws IOException
        {
        long             nSize            = file.length();    // size will not be very long
        final byte[]     abData           = new byte[(int) nSize];
        RandomAccessFile fileRandomAccess = null;
        FileChannel      fc               = null;

        for (int i = 0; i < nSize; i++)
            {
            abData[i] = 0x25;
            }

        ByteBuffer buffer = ByteBuffer.wrap(abData);

        try
            {
            fileRandomAccess = new RandomAccessFile(file, "rw");
            fc               = fileRandomAccess.getChannel();

            fc.position(0L);
            fc.write(buffer);
            }
        finally
            {
            fc.close();
            fileRandomAccess.close();
            }
        }

    /**
     * run the validate() method for an archived snapshot
     *
     * @param sSnapshotName   snapshot name to validate
     * @param sServiceName    service name to validate
     * @param fShouldBeValid  true if archived snapshot should be valid
     */
    private void testArchivedSnapshotAnalyzer(String sSnapshotName, String sServiceName, boolean fShouldBeValid)
        {
        Map<String, String> mapResults = null;
        Exception           e          = null;

        System.out.println("Checking archived snapshot for snapshot=" + sSnapshotName
                           + ", service=" + sServiceName +  ", ShouldBeValid="
                           + fShouldBeValid);

        InvocationService invocationService = (InvocationService) CacheFactory.getService("InvocationService");

        assertTrue(invocationService != null);

        Cluster          cluster        = invocationService.getCluster();

        Set<Member>      setMembers     = new HashSet<>();
        Member           memberInvoking = null;
        Iterator<Member> iter           = cluster.getMemberSet().iterator();

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
            ValidatorInvocable invocable = new ValidatorInvocable(sSnapshotName, sServiceName);
            Map    mapResult = invocationService.query(invocable, setMembers);
            Object oResult   = mapResult.get(memberInvoking);

            if (!oResult.equals(ValidatorInvocable.OK))
                {
                throw new RuntimeException((String) oResult);
                }
            }
        catch (Exception ee)
            {
            e = ee;
            e.printStackTrace();
            }

        if (fShouldBeValid)
            {
            Assert.assertNull("Should be valid, but error raised: " + (e != null ? e.getMessage() : ""), e);
            }
        else
            {
            // should not be valid
            Assert.assertTrue("Should not be valid, but no error raised", e != null);
            }
        }

    /**
     * Run the SnapshotAnalyzer and check for expected result.
     *
     * @param fileDirectory      Directory to check
     * @param fShouldBeValid     true if snapshot should be valid
     */
    private void testSnapshotAnalyzer(File fileDirectory, boolean fShouldBeValid)
        {
        Map<String, String> mapResults = null;
        Exception           e          = null;

        System.out.println("Checking snapshot: fShouldBeValid=" + fShouldBeValid);

        try
            {
            PersistenceTools tools =  CachePersistenceHelper.getSnapshotPersistenceTools(fileDirectory);

            Assert.assertFalse(tools.getPersistenceInfo().isArchived());

            tools.validate();
            dumpStats(tools);

            }
        catch (Exception ee)
            {
            e = ee;
            e.printStackTrace();
            }

        if (fShouldBeValid)
            {
            Assert.assertNull("Should be valid, but error raised: " + (e != null ? e.getMessage() : ""), e);
            }
        else
            {
            // should not be valid
            Assert.assertTrue("Should not be valid, but no error raised", e != null);
            }
        }

    /**
     * Dump information collected from tools.
     *
     * @param tools tools instance to dump information from
     */
    protected static void dumpStats(PersistenceTools tools)
        {
        System.out.println("Partition Count: " + tools.getPersistenceInfo().getPartitionCount());
        System.out.println("Storage Format: " + tools.getPersistenceInfo().getStorageFormat());
        System.out.println("Storage Version: " + tools.getPersistenceInfo().getStorageVersion());
        System.out.println("Impl Version: " + tools.getPersistenceInfo().getImplVersion());
        System.out.println("Number of stores: " + tools.getPersistenceInfo().getGUIDs().length);
        System.out.println("Is Complete?: " + tools.getPersistenceInfo().isComplete());
        System.out.println("Is Archive?: " + tools.getPersistenceInfo().isArchived());

        PersistenceStatistics stats = tools.getStatistics();

        if (stats != null)
            {
            for (String sCacheName : stats)
                {
                System.out.println("Cache Name: " + sCacheName);
                System.out.println("Size: " + stats.getCacheSize(sCacheName));
                System.out.println("Bytes: " + stats.getCacheBytes(sCacheName));
                System.out.println("Indexes: " + stats.getIndexCount(sCacheName));
                System.out.println("Triggers: " + stats.getTriggerCount(sCacheName));
                System.out.println("Listeners: " + stats.getListenerCount(sCacheName));
                System.out.println("Locks: " + stats.getLockCount(sCacheName));
                }
            }
        }

    /**
     * Return a randomly selected directory or file within a given directory so
     * that we can do nasty things to them to simulate corruption.
     *
     * @param fileBaseDirectory  the directory to query
     * @param fisFile            if true, we return a file otherwise we return a directory
     *
     * @return a randomly selected directory or file
     */
    private File getRandomFromDirectory(File fileBaseDirectory, boolean fisFile)
        {
        File   file = null;
        Random rand = new Random();

        Assert.assertTrue(fileBaseDirectory.exists() && fileBaseDirectory.canRead());

        // get the list of files
        File[] afileLIst = fileBaseDirectory.listFiles();
        int    nLen      = afileLIst.length;

        Assert.assertTrue(nLen != 0);

        int nOffset = rand.nextInt(nLen / 2);

        if (nOffset == 1)
            {
            nOffset = 0;
            }

        for (int i = nOffset; i < nLen; i++)
            {
            File fileSelected = afileLIst[i];

            if (!fileSelected.getName().equals(".") && !fileSelected.getName().equals("..")
                && fileSelected.getName().indexOf(".lck") == -1
                && fileSelected.getName().indexOf(".lock") == -1
                && !fileSelected.getName().equals(CachePersistenceHelper.META_FILENAME))
                {
                if ((fisFile && !fileSelected.isDirectory()) || !fisFile && afileLIst[i].isDirectory())
                    {
                    return afileLIst[i];
                    }
                }
            }

        return file;
        }

    /**
     * A helper method to call the static {@link PersistenceTestHelper#listSnapshots(String)}
     * method. This allows us to use this method in Eventually.assertThat(Deferred, Matcher)
     * tests.
     */
    public List<String> listSnapshots(PersistenceTestHelper helper, String sService)
        {
        String[] asSnapshots = helper.listSnapshots(sService);

        return Arrays.asList(asSnapshots);
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

    // ----- accessors ------------------------------------------------------

    /**
     * Return the project name.
     */
    public static String getProjectName()
        {
        return "persistence";
        }

    // ----- inner classes -----------------------------------------------

    /**
     * This is executed from within the cluster as the "client"
     * does not have the correct archiver setup.
     */
    private static class ValidatorInvocable
            extends AbstractInvocable
            implements Serializable
        {
        /**
         * run the validate() method for an archived snapshot
         *
         * @param sSnapshotName   snapshot name to validate
         * @param sServiceName    service name to validate
         */
        public ValidatorInvocable(String sSnapshotName, String sServiceName)
            {
            f_sSnapshotName = sSnapshotName;
            f_sServiceName  = sServiceName;
            }

        @Override
        public void run()
            {
            try
                {
                PersistenceTools tools = CachePersistenceHelper.getArchiverPersistenceTools(
                    (ExtensibleConfigurableCacheFactory) CacheFactory.getConfigurableCacheFactory(),
                                              f_sSnapshotName, f_sServiceName);

                if (!tools.getPersistenceInfo().isArchived())
                    {
                    throw new RuntimeException("Tools is not archived");
                    }

                tools.validate();
                dumpStats(tools);
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
        private final String f_sSnapshotName;
        private final String f_sServiceName;
        }
    }
