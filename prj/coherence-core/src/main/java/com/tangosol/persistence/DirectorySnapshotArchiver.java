/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistenceManager;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.GuardSupport;

import com.tangosol.util.Base;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Properties;

/**
 * An implementation of a {@link SnapshotArchiver} that uses a shared directory
 * to store archived snapshots.
 *
 * @since 12.2.1
 * @author tam  2014.08.19
 */
public class DirectorySnapshotArchiver
        extends AbstractSnapshotArchiver
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new DirectorySnapshotArchiver which uses a shared
     * directory available from all members to store archived snapshots.
     *
     * @param sClusterName   the name of the cluster
     * @param sServiceName   the service name
     * @param fileDirectory  a shared directory available from all members
     *
     * @throws IOException if errors creating directories
     */
    public DirectorySnapshotArchiver(String sClusterName, String sServiceName, File fileDirectory)
            throws IOException
        {
        super(sClusterName, sServiceName);

        f_fileSharedDirectoryPath = fileDirectory;

        FileHelper.ensureDir(f_fileSharedDirectoryPath);

        if (!f_fileSharedDirectoryPath.exists() || !f_fileSharedDirectoryPath.canWrite())
            {
            throw new IllegalArgumentException("Directory " + f_fileSharedDirectoryPath.getAbsolutePath()
                                               + " does not exist or cannot be written to");
            }

        }

    // ----- AbstractSnapshotArchiver methods -------------------------------

    @Override
    protected String[] listInternal()
        {
        ArrayList<String> snapshotArrayList = new ArrayList<>();

        // the base directory should contain just archived snapshot directories
        File[] aFiles = f_fileSharedDirectoryPath.listFiles(ArchiverHelper.DirectoryFileFilter.INSTANCE);

        if (aFiles != null)
            {
            // go through each snapshot directory
            for (File file : aFiles)
                {
                snapshotArrayList.add(file.getName());
                }
            }

        return snapshotArrayList.toArray(new String[snapshotArrayList.size()]);
        }

    @Override
    protected void archiveInternal(Snapshot snapshot, PersistenceManager<ReadBuffer> mgr)
        {
        String       sSnapshotName = snapshot.getName();
        OutputStream os            = null;

        for (String sStore : snapshot.listStores())
            {
            Logger.finer("Archiving store " + sStore + " for snapshot " + sSnapshotName);
            recordStartTime();

            try
                {
                File fileArchivedSnapshot = new File(f_fileSharedDirectoryPath, sSnapshotName);

                FileHelper.ensureDir(fileArchivedSnapshot);

                if (CachePersistenceHelper.isGlobalPartitioningSchemePID(GUIDHelper.getPartition(sStore)))
                    {
                    // write archived snapshot metadata properties only once
                    writeMetadata(fileArchivedSnapshot, mgr, sStore);
                    }

                // generate the file to write to
                File fileStore = new File(fileArchivedSnapshot, sStore);

                if (mgr.isEmpty(sStore))
                    {
                    fileStore.createNewFile();
                    }
                else
                    {
                    // the output stream will be used by the manager to write the store to
                    os = new FileOutputStream(fileStore);

                    mgr.write(sStore, os);    // instruct the mgr to write the store to the stream
                    }

                // issue heartbeat as operations could take a relatively long time
                GuardSupport.heartbeat();
                }
            catch (Exception e)
                {
                e.printStackTrace();

                throw CachePersistenceHelper.ensurePersistenceException(e, "Error writing store");
                }
            finally
                {
                if (os != null)
                    {
                    try
                        {
                        os.close();
                        }
                    catch (IOException ioe)
                        {
                        throw CachePersistenceHelper.ensurePersistenceException(ioe,
                                "Unable to close output stream for store " + sStore);
                        }
                    }
                }

            recordEndTime();
            }
        }

    @Override
    protected void retrieveInternal(Snapshot snapshot, PersistenceManager<ReadBuffer> mgr)
        {
        String      sSnapshotName = snapshot.getName();
        InputStream is            = null;

        for (String sStore : snapshot.listStores())
            {
            Logger.finer("Retrieving store " + sStore + " for snapshot " + sSnapshotName);
            recordStartTime();

            try
                {
                if (CachePersistenceHelper.isGlobalPartitioningSchemePID(GUIDHelper.getPartition(sStore)))
                    {
                    // validate that the metadata file exists for partition 0
                    if (getMetadata(sSnapshotName) == null)
                        {
                        throw new IllegalArgumentException("Cannot load properties file " +
                                     CachePersistenceHelper.META_FILENAME + " for snapshot " + sSnapshotName);
                        }
                    }

                File fileStore = new File(f_fileSharedDirectoryPath, sSnapshotName);
                fileStore      = new File(fileStore, sStore);

                if (!fileStore.exists())
                    {
                    throw new PersistenceException("Store " + fileStore + " does not exist. Unable to retrieve.");
                    }

                if (fileStore.length() != 0)
                    {
                    // the input stream will be used by the manager to retrieve the archives store
                    is = new FileInputStream(fileStore);

                    mgr.read(sStore, is);    // instruct the mgr to read the store from the stream

                    // issue heartbeat as operations could take a relatively long time
                    GuardSupport.heartbeat();
                    }
                else
                    {
                    mgr.createStore(sStore);
                    }
                }
            catch (IOException e)
                {
                throw CachePersistenceHelper.ensurePersistenceException(e, "Error reading store " + sStore);
                }
            finally
                {
                if (is != null)
                    {
                    try
                        {
                        is.close();
                        }
                    catch (IOException ioe)
                        {
                        throw CachePersistenceHelper.ensurePersistenceException(ioe, "Unable to close input stream for store " + sStore);
                        }
                    }
                }

            recordEndTime();
            }
        }

    @Override
    protected boolean removeInternal(String sSnapshot)
        {
        File fileSnapshot = null;

        try
            {
            fileSnapshot = new File(f_fileSharedDirectoryPath, FileHelper.toFilename(sSnapshot));

            if (!fileSnapshot.exists() || !fileSnapshot.isDirectory())
                {
                throw new PersistenceException(fileSnapshot + " is not a directory");
                }

            FileHelper.deleteDir(fileSnapshot);

            return true;
            }
        catch (IOException ioe)
            {
            Logger.warn("Unable to delete directory " + fileSnapshot + " " + ioe.getMessage());

            return false;
            }

        }

    @Override
    protected String[] listStoresInternal(String sSnapshot)
        {
        File fileSnapshot = new File(f_fileSharedDirectoryPath, sSnapshot);

        if (!fileSnapshot.exists() || !fileSnapshot.canRead() || !fileSnapshot.canExecute())
            {
            throw new IllegalArgumentException("Cannot open snapshot directory " + fileSnapshot);
            }

        // each of the files under the snapshot should be a store except the meta.properties
        File[] aSnapshotFiles = fileSnapshot.listFiles(
            (file) -> !file.getName().equals(CachePersistenceHelper.META_FILENAME));

        String[] aStores;

        if (aSnapshotFiles != null)
            {
            aStores = new String[aSnapshotFiles.length];

            int i = 0;

            for (File storeFile : aSnapshotFiles)
                {
                if (storeFile.isFile())
                    {
                    aStores[i++] = storeFile.getName();
                    }
                }
            }
        else
            {
            // empty directory
            aStores = new String[0];
            }

        return aStores;
        }

    @Override
    protected Properties getMetadata(String sSnapshot) throws IOException
        {
        File fileSnapshot = new File(f_fileSharedDirectoryPath, sSnapshot);

        if (!fileSnapshot.exists() || !fileSnapshot.canRead() || !fileSnapshot.canExecute())
            {
            throw new IllegalArgumentException("Cannot open snapshot directory " + fileSnapshot);
            }

        return CachePersistenceHelper.readMetadata(fileSnapshot);
        }

    @Override
    protected boolean isEmpty(String sSnapshot, String sStore)
        {
        File fileStore = new File(f_fileSharedDirectoryPath, sSnapshot);
        fileStore      = new File(fileStore, sStore);

        return fileStore.length() == 0;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "DirectorySnapshotArchiver(SharedDirectoryPath=" + f_fileSharedDirectoryPath.getAbsolutePath() + ")";
        }

    // ----- DirectorySnapshotArchiver methods ------------------------------

    /**
     * The shared directory to write archives to.
     *
     * @return shared directory to write archives to.
     */
    public File getSharedDirectoryPath()
        {
        return this.f_fileSharedDirectoryPath;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The shared directory to write archives to. On instantiation this
     * directory will include the cluster and service.
     */
    private final File f_fileSharedDirectoryPath;
    }
