/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.persistence.PersistenceEnvironment;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.DirectorySnapshotArchiver;
import com.tangosol.persistence.Snapshot;
import com.tangosol.persistence.SnapshotArchiver;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Functional tests for snapshot archiving and retrieval.
 * These tests test the individual SnapshotArchivers by calling them
 * directly rather than via JMX operations.
 *
 * @author tam 2014.08.22
 */
public abstract class AbstractArchiverPersistenceTests
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
        System.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        AbstractFunctionalTest._startup();
        }

    // ---- tests -----------------------------------------------------------

    /**
     * Test directory based archiver with SEONE enabled.
     */
    @Test
    public void testDirectoryBasedArchiverSEONE()
        {
        try
            {
            testArchiveSnapshotPrimitives("testDirArchiver" + getPersistenceManagerName(), true);
            }
        catch (Exception e)
            {
            e.printStackTrace();
            fail(e.getMessage());
            }
        }

    /**
     * Test directory based archiver with SEONE disabled.
     */
    @Test
    public void testDirectoryBasedArchiverNoSEONE()
        {
        try
            {
            testArchiveSnapshotPrimitives("testDirArchiverNoSEONE" + getPersistenceManagerName(), false);
            }
        catch (Exception e)
            {
            e.printStackTrace();
            fail(e.getMessage());
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Run base archiver tests using the primitive operations
     * rather than JMX operations.
     *
     * @param sServer   the name of the server
     * @param fIsSEONE  indicates if SEONE (new two-member policy) is in effect
     */
    private void testArchiveSnapshotPrimitives(String sServer, boolean fIsSEONE)
        {
        String sPersistentCache = "simple-archiver";
        String sSnapshotName    = "test-snapshot";
        String sSnapshotName2   = "test-snapshot-2";

        int    COUNT1           = 1000000;
        int    COUNT2           = 50000;

        File   fileSnapshotBase = null;
        File   fileActive       = null;
        File   fileSnapshot     = null;
        File   fileTrash        = null;

        try
            {
            fileActive   = FileHelper.createTempDir();
            fileSnapshot = FileHelper.createTempDir();
            fileTrash    = FileHelper.createTempDir();
            }
        catch (IOException ioe)
            {
            throw new RuntimeException(ioe);
            }

        Properties props = fIsSEONE ? new Properties() : PROPS_SEONE;

        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.start.archiver", "true");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        String           sClusterName    = CacheFactory.ensureCluster().getClusterName();
        final NamedCache cache           = getNamedCache(sPersistentCache);
        CacheService     service         = cache.getCacheService();
        String           sServiceName    = service.getInfo().getServiceName();
        Cluster          cluster         = service.getCluster();
        int              nPartitionCount = ((PartitionedService) service).getPartitionCount();

        startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
        startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);

        // create the archive once we have the service and cluster
        SnapshotArchiver                   archiver             = null;
        PersistenceEnvironment<ReadBuffer> env                  = null;
        File                               fileArchiveDirectory = null;

        try
            {
            // create directories
            fileArchiveDirectory = FileHelper.createTempDir();

            env = createPersistenceEnv(getFullFileName(fileActive, sClusterName, sServiceName),
                                       getFullFileName(fileSnapshot, sClusterName, sServiceName),
                                       getFullFileName(fileTrash, sClusterName, sServiceName));
            fileArchiveDirectory = new File(fileArchiveDirectory, FileHelper.toFilename(sClusterName));
            fileArchiveDirectory = new File(fileArchiveDirectory, FileHelper.toFilename(sServiceName));

            archiver = new DirectorySnapshotArchiver(sClusterName, sServiceName, fileArchiveDirectory);

            System.out.println("Archiver = " + archiver);

            // populate some data

            populateData(cache, COUNT1);

            // check that all data is inserted
            assertEquals(COUNT1, cache.size());

            PersistenceTestHelper helper = new PersistenceTestHelper();

            // Test flow is:
            // 1. Create a new snapshot called sSnapshotName ("test-snapshot") with COUNT1 objects
            // 2. Archive the snapshot
            // 3. Validate that individual files were created
            // 4. Clear the cache
            // 5. Remove the local snapshot
            // 6. Retrieve the archived snapshot
            // 7. Recover the snapshot retrieve-snapshot and validate size == COUNT1
            // 8. Create a second snapshot and archive
            // 9. Validate exceptions

            // 1. Create 1 new snapshot
            cluster.suspendService(sServiceName);
            helper.createSnapshot(sServiceName, sSnapshotName);
            cluster.resumeService(sServiceName);

            // get path to where the snapshot directory should be
            fileSnapshotBase = new File(fileSnapshot, FileHelper.toFilename(sClusterName));
            fileSnapshotBase = new File(fileSnapshotBase, FileHelper.toFilename(sServiceName));

            File dir = new File(fileSnapshotBase, FileHelper.toFilename(sSnapshotName));

            assertTrue(dir.exists());

            // validate the new snapshot exists
            Eventually.assertThat(invoking(this).getSnapshotCount(helper, sServiceName), is(1));
            String[] asSnapshots = helper.listSnapshots(sServiceName);

            FilenameFilter fileFilter = (dirRoot, sFilename) -> !".lock".equals(sFilename);

            Snapshot localSnapshot = new Snapshot(sSnapshotName, dir.list(fileFilter));

            String[] asStore = localSnapshot.listStores();
            assertNotNull(asStore);

            assertThat("Number of stores on disk is " + asStore.length + " but the partition count is " + nPartitionCount,
                    asStore.length, is(nPartitionCount));

            // 2. Archive the snapshot
            archiver.archive(localSnapshot, env);

            // 3. Validate the individual files were created under the destination archiver directory
            File     fileRoot = ((DirectorySnapshotArchiver) archiver).getSharedDirectoryPath();
            fileRoot = new File(fileRoot, FileHelper.toFilename(sSnapshotName));
            String[] asFiles = fileRoot.list();

            assertNotNull(asFiles);
            assertTrue("The number of files in the archive directory " + fileRoot + " is "
                       + (asFiles.length - 1) + " but should be " + nPartitionCount,
                    asFiles.length - 1 == nPartitionCount);

            String[] asSnapshotNames = archiver.list();

            assertTrue("The number of archives should be 1 but is "
                       + (asSnapshotNames == null ? null : asSnapshotNames.length), asSnapshotNames != null
                           && asSnapshotNames.length == 1);

            // make sure that a meta.properties file exists and contains correct values
            File fileMetaProperties = new File(fileRoot, CachePersistenceHelper.META_FILENAME);
            assertTrue(fileMetaProperties.exists());
            Properties propsMeta = CachePersistenceHelper.readMetadata(fileRoot);

            assertNotNull(propsMeta);
            assertEquals(nPartitionCount, Integer.parseInt(propsMeta.getProperty(CachePersistenceHelper.META_PARTITION_COUNT)));

            // 4. Clear the cache and validate size
            System.out.println("Clearing cache");
            cache.clear();
            assertEquals(0, cache.size());

            String[] asRemoteSnapshots = archiver.list();

            assertTrue(asRemoteSnapshots != null && asRemoteSnapshots.length == 1);

            Snapshot remoteSnapshot = archiver.get(asRemoteSnapshots[0]);

            assertTrue("Remote snapshot " + sSnapshotName + " does not exist according to archiver",
                       sSnapshotName.equals(remoteSnapshot.getName()));

            // 5. remove the local snapshot
            cluster.suspendService(sServiceName);
            helper.removeSnapshot(sServiceName, sSnapshotName);
            cluster.resumeService(sServiceName);

            Eventually.assertThat(invoking(this).getSnapshotCount(helper, sServiceName), is(0));
            Eventually.assertThat(
                    "Expected no snapshots to exist but found the following: " + Arrays.toString(asSnapshots),
                    invoking(this).listSnapshots(helper, sServiceName), arrayWithSize(0));

            // 6. retrieve the archived snapshot
            archiver.retrieve(remoteSnapshot, env);

            // 7. Recover the snapshot
            cluster.suspendService(sServiceName);
            helper.recoverSnapshot(sServiceName, sSnapshotName);
            cluster.resumeService(sServiceName);

            // check that all data is inserted
            assertEquals(COUNT1, cache.size());

            // 8. create another second "snapshot-test-2"
            cache.clear();
            populateData(cache, COUNT2);
            assertEquals(COUNT2, cache.size());

            cluster.suspendService(sServiceName);
            helper.createSnapshot(sServiceName, sSnapshotName2);
            cluster.resumeService(sServiceName);
            Eventually.assertThat(invoking(this).getSnapshotCount(helper, sServiceName), is(2));
            asSnapshots = helper.listSnapshots(sServiceName);

            assertTrue("Number of snapshots is " + (asSnapshots == null ? null : asSnapshots.length)
                       + ", but should be 2", asSnapshots != null && asSnapshots.length == 2);

            // archive the new snapshot
            dir           = new File(fileSnapshotBase, FileHelper.toFilename(sSnapshotName2));
            localSnapshot = new Snapshot(sSnapshotName2, dir.list(fileFilter));

            archiver.archive(localSnapshot, env);
            asSnapshotNames = archiver.list();
            assertTrue("The number of archives should be 2 but is "
                       + (asSnapshotNames == null ? null : asSnapshotNames.length), asSnapshotNames != null
                           && asSnapshotNames.length == 2);

            archiver.remove(sSnapshotName);

            asSnapshotNames = archiver.list();
            assertTrue("The number of archives should be 1 but is "
                       + (asSnapshotNames == null ? null : asSnapshotNames.length), asSnapshotNames != null
                           && asSnapshotNames.length == 1);

            // 9. test exceptions

             try
                {
                // try to archive a snapshot that doesn't exist
                Snapshot dummySnapshot = new Snapshot("bad", new String[] {"1", "2", "3", "4"});

                archiver.archive(dummySnapshot, env);
                assertTrue("Exception from archiving non-existent snapshot was not raised", true);
                }
            catch (Exception e)
                {
                // ignore
                }

            try
                {
                // Try to retrieve snapshot localSnapshot which still exists
                archiver.retrieve(localSnapshot, env);
                    assertTrue("Exception from retrieving existing snapshot was not raised", true);
                }
            catch (Exception e)
                {
                // ignore
                }

            try
                {
                // Try to remove snapshot localSnapshot which doesn't exists
                archiver.remove("dummy");
                assertTrue("Exception from remove existing snapshot was not raised", true);
                }
            catch (Exception e)
                {
                // ignore
                }
            stopCacheServer(sServer + "-1");
            stopCacheServer(sServer + "-2");
            CacheFactory.shutdown();
            }
        catch (Exception e)
            {
            e.printStackTrace();
            fail();
            }
        finally
            {
            FileHelper.deleteDirSilent(fileArchiveDirectory);
            FileHelper.deleteDirSilent(fileActive);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            FileHelper.deleteDirSilent(fileArchiveDirectory);
            }
        }

    /**
     * @see PersistenceTestHelper#listSnapshots(String)
     */
    public String[] listSnapshots(PersistenceTestHelper helper, String sServiceName)
        {
        return helper.listSnapshots(sServiceName);
        }

    /**
     * Return the number of snapshots for a service.
     */
    public int getSnapshotCount(PersistenceTestHelper helper, String sServiceName)
        {
        String[] asSnapshots = helper.listSnapshots(sServiceName);
        return asSnapshots == null ? 0 : asSnapshots.length;
        }

    /**
     * Return the full file name with cluster and service appended.
     *
     * @param file          the base directory
     * @param sClusterName  the cluster name
     * @param sServiceName  the service name
     *
     * @return the file file name
     */
    private File getFullFileName(File file, String sClusterName, String sServiceName)
        {
        File fileDir = new File(file, FileHelper.toFilename(sClusterName));

        return new File(fileDir, FileHelper.toFilename(sServiceName));
        }

    /**
     * Populate a cache with data.
     *
     * @param nc    the cache to populate
     * @param cMax  the amount of objects to inser
     */
    protected void populateData(NamedCache nc, int cMax)
        {
        Map<Integer, Integer> mapBuffer = new HashMap<>();
        int                   BATCH     = 1000;

        for (int i = 0; i < cMax; i++)
            {
            mapBuffer.put(i, i);

            if (i % BATCH == 0)
                {
                nc.putAll(mapBuffer);
                mapBuffer.clear();
                }
            }

        if (!mapBuffer.isEmpty())
            {
            nc.putAll(mapBuffer);
            }
        }

    // ----- factory methods ------------------------------------------------

    protected abstract PersistenceEnvironment<ReadBuffer> createPersistenceEnv(File fileActive, File fileSnapshot,
        File fileTrash)
            throws IOException;

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
