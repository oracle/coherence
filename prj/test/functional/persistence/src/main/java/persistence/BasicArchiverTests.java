/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.persistence.PersistenceEnvironment;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.Member;

import com.tangosol.persistence.DirectorySnapshotArchiver;
import com.tangosol.persistence.GUIDHelper;
import com.tangosol.persistence.Snapshot;
import com.tangosol.persistence.SnapshotArchiver;
import com.tangosol.persistence.bdb.BerkeleyDBEnvironment;

import com.tangosol.util.UID;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import java.util.Date;

/**
 * Functional tests for basic archiver functionality.
 *
 * @author tam  2014.03.17
 */
public class BasicArchiverTests
    {
    // ----- test methods ---------------------------------------------------

    /**
     * Test some basic functionality of the directory snapshot archiver.
     */
    @Test
    public void testDirectorySnapshotArchiver()
        {
        File fileArchiveDirectory = null;
        File fileActive           = null;
        File fileSnapshot         = null;
        File fileTrash            = null;

        try
            {
            fileArchiveDirectory           = FileHelper.createTempDir();
            File fileFinalArchiveDirectory = new File(fileArchiveDirectory, FileHelper.toFilename(CLUSTER_NAME));
            fileFinalArchiveDirectory      = new File(fileFinalArchiveDirectory, FileHelper.toFilename(SERVICE_NAME));

            fileActive                = FileHelper.createTempDir();
            fileSnapshot              = FileHelper.createTempDir();
            fileTrash                 = FileHelper.createTempDir();

            PersistenceEnvironment<ReadBuffer> env = createPersistenceEnv(fileActive, fileSnapshot, fileTrash);

            SnapshotArchiver archiver = new DirectorySnapshotArchiver(CLUSTER_NAME, SERVICE_NAME,
                                            fileFinalArchiveDirectory);

            testListSnapshots(archiver, fileArchiveDirectory);

            }
        catch (Exception e)
            {
            e.printStackTrace();
            fail(e.getMessage());
            }
        finally
            {
            deleteDir(fileArchiveDirectory);
            deleteDir(fileActive);
            deleteDir(fileSnapshot);
            deleteDir(fileTrash);
            }
        }

    /**
     * Ensure that archiver correctly lists snapshots.
     *
     * @param archiver       the archiver implementation to test
     * @param fileDirectory  the directory to create the archived snapshots
     *
     * @throws IOException if any IO related errors
     */
    private void testListSnapshots(SnapshotArchiver archiver, File fileDirectory)
            throws IOException
        {
        String sSnapshot1 = "my-snapshot-1";
        String sSnapshot2 = "my-snapshot-2";
        String sSnapshot3 = "my-snapshot-3";

        System.out.println("testListSnapshots(): archiver = " + archiver.toString());

        String[]   aSnapshots = archiver.list();
        Snapshot[] snapshots;

        assertTrue(aSnapshots != null && aSnapshots.length == 0);    // should be none

        createMockArchivedSnapshot(sSnapshot1, fileDirectory, 10);
        snapshots = getAllSnapshots(archiver);
        validateSnapshot(snapshots, 1, sSnapshot1, 10);

        createMockArchivedSnapshot(sSnapshot2, fileDirectory, 20);
        snapshots = getAllSnapshots(archiver);
        validateSnapshot(snapshots, 2, sSnapshot2, 20);

        createMockArchivedSnapshot(sSnapshot3, fileDirectory, 40);
        snapshots = getAllSnapshots(archiver);
        validateSnapshot(snapshots, 3, sSnapshot3, 40);

        removeMockArchivedSnapshot(sSnapshot1, fileDirectory);
        snapshots = getAllSnapshots(archiver);
        validateSnapshot(snapshots, 2, sSnapshot2, 20);

        removeMockArchivedSnapshot(sSnapshot2, fileDirectory);
        snapshots = getAllSnapshots(archiver);
        validateSnapshot(snapshots, 1, sSnapshot3, 40);

        removeMockArchivedSnapshot(sSnapshot3, fileDirectory);
        snapshots = getAllSnapshots(archiver);
        assertTrue(snapshots != null && snapshots.length == 0);    // should be none
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return an array of snapshots for a given archiver
     *
     * @param archiver the archiver to query
     *
     * @return the array of snapshots
     *
     * @throws IOException if any I/O related issues
     */
    private Snapshot[] getAllSnapshots(SnapshotArchiver archiver)
            throws IOException
        {
        String[]   asSnapshots = archiver.list();
        Snapshot[] aSnapshots  = null;

        if (asSnapshots != null)
            {
            aSnapshots = new Snapshot[asSnapshots.length];

            for (int i = 0; i < asSnapshots.length; i++)
                {
                aSnapshots[i] = archiver.get(asSnapshots[i]);
                }
            }
        else
            {
            return new Snapshot[]
                {
                };
            }

        return aSnapshots;
        }

    /**
     * Validate the list of snapshots returned.
     *
     * @param snapshots         the {@link Snapshot} array to check
     * @param nNumberSnapshots  the expected number of snapshots
     * @param sSnapshotName     the snapshot name to look for
     * @param nNumberStores     the expected number of stores for the snapshot
     */
    private void validateSnapshot(Snapshot[] snapshots, int nNumberSnapshots, String sSnapshotName, int nNumberStores)
        {
        assertTrue("Snapshot[] is null", snapshots != null);
        assertTrue("Number of snapshots should be " + nNumberSnapshots + " but is " + snapshots.length,
                   nNumberSnapshots == snapshots.length);

        // find the snapshot
        for (Snapshot snapshot : snapshots)
            {
            if (sSnapshotName.equals(snapshot.getName()))
                {
                assertTrue("Snapshot name should be " + sSnapshotName + " but is " + snapshot.getName(),
                           sSnapshotName.equals(snapshot.getName()));
                assertTrue("Number of stores should be " + nNumberStores + " but is "
                           + (snapshot.listStores() == null
                              ? "null" : snapshot.listStores().length), snapshot.listStores() != null
                                  && snapshot.listStores().length == nNumberStores);

                // return as we have found the snapshot
                return;
                }
            }

        // if we get here then the snapshot was not found
        fail("Snapshot " + sSnapshotName + " was not found in Snapshot[]");
        }

    /**
     * Create a random archived snapshot in the given directory.
     *
     * @param sSnapshot     the name of the snapshot to create the cluster, service then snapshot
     * @param fileSnapshot  the directory off which to create it
     * @param nPartitions   the number of partitions to create
     *
     * @throws IOException if any I/O related issues
     */
    private void createMockArchivedSnapshot(String sSnapshot, File fileSnapshot, int nPartitions)
            throws IOException
        {
        File fileSnapshotDir = new File(fileSnapshot, CLUSTER_NAME);

        fileSnapshotDir = new File(fileSnapshotDir, SERVICE_NAME);
        fileSnapshotDir = new File(fileSnapshotDir, sSnapshot);

        fileSnapshotDir.mkdir();

        for (int i = 0; i < nPartitions; i++)
            {
            File fileGUIDDir = new File(fileSnapshotDir, GUIDHelper.generateGUID(i, 1, MEMBER_DATE, getMockMember(1)));

            fileGUIDDir.createNewFile();
            }
        }

    /**
     * Remove a given snapshot directory and all its contents.
     *
     * @param sSnapshot     the name of the snapshot to remove
     * @param fileSnapshot  the directory off which to create it
     *
     * @throws IOException if any I/O related issues
     */
    private void removeMockArchivedSnapshot(String sSnapshot, File fileSnapshot)
            throws IOException
        {
        File fileSnapshotDir = new File(fileSnapshot, CLUSTER_NAME);

        fileSnapshotDir = new File(fileSnapshotDir, SERVICE_NAME);
        fileSnapshotDir = new File(fileSnapshotDir, sSnapshot);

        FileHelper.deleteDir(fileSnapshotDir);
        }

    /**
     * Create a PersistenceEnvironment for testing.
     *
     * @param fileActive    active directory
     * @param fileSnapshot  snapshot directory
     * @param fileTrash     trash directory
     *
     * @return a new PersistenceEnvironment
     *
     * @throws IOException if any I/O related issues
     */
    protected PersistenceEnvironment<ReadBuffer> createPersistenceEnv(File fileActive, File fileSnapshot,
        File fileTrash)
            throws IOException
        {
        return new BerkeleyDBEnvironment(fileActive, fileSnapshot, fileTrash);
        }

    /**
     * Delete a directory and eat the exception
     *
     * @param fileDir directory to delete.
     */
    private void deleteDir(File fileDir)
        {
        try
            {
            FileHelper.deleteDir(fileDir);
            }
        catch (Exception e)
            {
            // ignore
            }
        }

    /**
     * Create and return a mock member.
     *
     * @param nMember  the member id
     *
     * @return the mock member
     */
    public static Member getMockMember(int nMember)
        {
        Member member = mock(Member.class);

        when(member.getId()).thenReturn(nMember);
        when(member.getUid()).thenReturn(new UID(2130706433 /* 127.0.0.1 */, new Date().getTime(), nMember));

        return member;
        }

    // ----- static ---------------------------------------------------------

    private static final long MEMBER_DATE = new Date("Mon Jul 09 20:22:45 PDT 2012").getTime();    ////1341890565000

    /**
     * Cluster name.
     */
    private static final String CLUSTER_NAME = "TestCluster";

    /**
     * Service Name.
     */
    private static final String SERVICE_NAME = "DistributedCachePersistence";
    }
