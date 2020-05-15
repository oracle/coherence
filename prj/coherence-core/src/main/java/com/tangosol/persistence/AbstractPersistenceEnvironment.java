/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.common.base.Continuation;

import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.coherence.persistence.PersistentStore;

import com.oracle.datagrid.persistence.PersistenceEnvironment;

import com.tangosol.internal.util.DaemonPool;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

import java.io.File;
import java.io.IOException;

import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract implementation of a ReadBuffer-based PersistentEnvironment.
 *
 * @author jh  2013.05.21
 */
public abstract class AbstractPersistenceEnvironment
        extends Base
        implements PersistenceEnvironment<ReadBuffer>, PersistenceEnvironmentInfo
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new AbstractPersistenceEnvironment that manages a singleton
     * AbstractPersistenceManager with the specified data directory and that
     * creates, opens, and deletes snapshots under the specified snapshot
     * directory.
     *
     * @param fileActive    the data directory of the singleton active
     *                      manager or null if an active manager shouldn't
     *                      be maintained by this environment
     * @param fileSnapshot  the snapshot directory
     * @param fileTrash     an optional trash directory used for "safe"
     *                      deletes
     *
     * @throws IOException if the data directory could not be created
     *
     * @throws IllegalArgumentException if the data, snapshot, and trash
     *         directories are not unique
     */
    public AbstractPersistenceEnvironment(File fileActive, File fileSnapshot, File fileTrash)
            throws IOException
        {
        if (fileActive != null && !fileActive.exists())
            {
            CacheFactory.log("Creating persistence active directory \""
                    + fileActive.getAbsolutePath() + '"', CacheFactory.LOG_INFO);
            }

        f_fileActive   = fileActive == null ? null : FileHelper.ensureDir(fileActive);
        f_fileSnapshot = fileSnapshot;
        f_fileTrash    = fileTrash;

        // validate that the active and snapshot directories are not the same
        if (f_fileActive != null && f_fileActive.equals(f_fileSnapshot))
            {
            throw new IllegalArgumentException("active directory \""
                    + f_fileActive
                    + " \"cannot be the same as the snapshot directory");
            }

        // validate that the trash directory is unique as well
        if (f_fileTrash != null && (f_fileTrash.equals(f_fileActive) ||
                f_fileTrash.equals(f_fileSnapshot)))
            {
            throw new IllegalArgumentException("trash directory \""
                    + f_fileTrash
                    + " \"cannot be the same as the active or snapshot directory");
            }
        }

    // ----- PersistenceEnvironment interface -------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized PersistenceManager<ReadBuffer> openActive()
        {
        if (f_fileActive == null)
            {
            return null;
            }

        AbstractPersistenceManager manager = m_managerActive;
        if (manager == null)
            {
            m_managerActive = manager = openActiveInternal();
            manager.setPersistenceEnvironment(this);
            }

        return manager;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized PersistenceManager<ReadBuffer> openSnapshot(String sSnapshot)
        {
        File fileSnapshot = new File(f_fileSnapshot, FileHelper.toFilename(sSnapshot));
        if (!fileSnapshot.isDirectory())
            {
            return null; // unrecognized snapshot
            }

        AbstractPersistenceManager manager = f_mapSnapshots.get(sSnapshot);
        if (manager == null)
            {
            manager = openSnapshotInternal(fileSnapshot, sSnapshot);
            manager.setPersistenceEnvironment(this);
            f_mapSnapshots.put(sSnapshot, manager);
            }

        return manager;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized PersistenceManager<ReadBuffer> createSnapshot(String sSnapshot,
            PersistenceManager<ReadBuffer> manager)
        {
        if (f_mapSnapshots.containsKey(sSnapshot))
            {
            throw new IllegalArgumentException("duplicate snapshot: " + sSnapshot);
            }

        // validate the snapshot name
        String sFilename = FileHelper.toFilename(sSnapshot);
        if (sFilename.startsWith(DELETED_PREFIX))
            {
            throw new IllegalArgumentException("snapshot starts with a reserved character: "
                    + sSnapshot);
            }

        // create the snapshot directory
        if (!f_fileSnapshot.exists())
            {
            CacheFactory.log("Creating persistence snapshot directory \""
                    + f_fileSnapshot.getAbsolutePath() + '"', CacheFactory.LOG_INFO);
            }
        File fileSnapshot;
        try
            {
            fileSnapshot = FileHelper.ensureDir(new File(f_fileSnapshot,
                    sFilename));
            }
         catch (IOException e)
             {
             throw ensurePersistenceException(e);
             }

        AbstractPersistenceManager snapshot = createSnapshotInternal(
                fileSnapshot, sSnapshot, manager);
        snapshot.setPersistenceEnvironment(this);
        f_mapSnapshots.put(sSnapshot, snapshot);

        return snapshot;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean removeSnapshot(String sSnapshot)
        {
        AbstractPersistenceManager manager = f_mapSnapshots.get(sSnapshot);
        if (manager != null)
            {
            manager.release();
            }

        File fileSnapshot = new File(f_fileSnapshot, FileHelper.toFilename(sSnapshot));
        return fileSnapshot.isDirectory() && removeSnapshotInternal(fileSnapshot, sSnapshot);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] listSnapshots()
        {
        File[] aFiles = f_fileSnapshot.listFiles(file ->
                file.isDirectory() && !file.getName().startsWith(DELETED_PREFIX));

        int      cNames  = aFiles == null ? 0 : aFiles.length;
        String[] asNames = cNames == 0 ? NO_STRINGS : new String[cNames];
        for (int i = 0; i < cNames; ++i)
            {
            asNames[i] = aFiles[i].getName();
            }

        return asNames;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void release()
        {
        AbstractPersistenceManager managerActive = m_managerActive;
        if (managerActive != null)
            {
            managerActive.release();
            }

        AbstractPersistenceManager[] aSnapshots = new AbstractPersistenceManager[f_mapSnapshots.size()];
        aSnapshots = f_mapSnapshots.values().toArray(aSnapshots);
        for (int i = 0, c = aSnapshots.length; i < c; ++i)
            {
            aSnapshots[i].release();
            }
        }

    // ----- PersistenceEnvironmentInfo interface ---------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public File getPersistenceActiveDirectory()
        {
        return f_fileActive;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getPersistenceSnapshotDirectory()
        {
        return f_fileSnapshot;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getPersistenceTrashDirectory()
        {
        return f_fileTrash;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPersistenceActiveSpaceUsed()
        {
        long cBytes = 0L;

        if (m_managerActive != null && f_fileActive != null)
            {
            // go through each directory owned by the active manager and
            // sum up the bytes used by the files in the directory
            for (String sId : m_managerActive.f_mapStores.keySet())
                {
                File fileDir = new File(f_fileActive, sId);
                cBytes += FileHelper.sizeDir(fileDir);
                }
            }

        return cBytes;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return a PersistenceException with the given cause. The returned
     * exception is also initialized with this environment.
     *
     * @param eCause  an optional cause
     *
     * @return a PersistenceException with the given cause
     */
    protected PersistenceException ensurePersistenceException(Throwable eCause)
        {
        return ensurePersistenceException(eCause, null /*sMessage*/);
        }

    /**
     * Return a PersistenceException with the given cause and detail message.
     * The returned exception is also initialized with this environment.
     *
     * @param eCause    an optional cause
     * @param sMessage  an optional detail message
     *
     * @return a PersistenceException with the given cause and detail message
     */
    protected PersistenceException ensurePersistenceException(Throwable eCause, String sMessage)
        {
        PersistenceException e = CachePersistenceHelper.ensurePersistenceException(
                eCause, sMessage);
        e.initPersistenceEnvironment(this);
        return e;
        }

    /**
     * Called by the specified manager when it has been released.
     *
     * @param manager  the manager that was released
     */
    protected synchronized void onReleased(AbstractPersistenceManager manager)
        {
        if (manager == m_managerActive)
            {
            m_managerActive = null;
            }
        else
            {
            f_mapSnapshots.remove(manager.getName());
            }
        manager.setPersistenceEnvironment(null /*env*/);
        }

    /**
     * Open the active manager.
     * <p>
     * Note: this method is guaranteed to only be called by a thread that
     * holds a monitor on this environment.
     *
     * @return the active manager
     *
     * @throws PersistenceException if a general persistence error occurs
     */
    protected abstract AbstractPersistenceManager openActiveInternal();

    /**
     * Open the snapshot with the specified identifier.
     * <p>
     * Note: this method is guaranteed to only be called by a thread that
     * holds a monitor on this environment.
     *
     * @param fileSnapshot  the directory of the snapshot
     * @param sSnapshot     the snapshot identifier
     *
     * @return the snapshot
     *
     * @throws PersistenceException if a general persistence error occurs
     */
    protected abstract AbstractPersistenceManager openSnapshotInternal(File fileSnapshot,
            String sSnapshot);

    /**
     * Create a snapshot with the specified identifier.
     * <p>
     * Note: this method is guaranteed to only be called by a thread that
     * holds a monitor on this environment.
     *
     * @param fileSnapshot  the directory of the snapshot
     * @param sSnapshot     the snapshot identifier
     * @param manager       the optional manager to create a snapshot of; if
     *                      null, an empty snapshot will be created
     *
     * @return the snapshot
     *
     * @throws PersistenceException if a general persistence error occurs
     *
     * @throws IllegalArgumentException if the specified manager is
     *         incompatible with this environment
     */
    protected abstract AbstractPersistenceManager createSnapshotInternal(File fileSnapshot,
            String sSnapshot, PersistenceManager<ReadBuffer> manager);

    /**
     * Remove the snapshot with the specified identifier.
     * <p>
     * Note: this method is guaranteed to only be called by a thread that
     * holds a monitor on this environment.
     *
     * @param fileSnapshot  the directory of the snapshot
     * @param sSnapshot     the snapshot identifier
     *
     * @return true if the snapshot was successfully deleted, false otherwise
     *
     * @throws PersistenceException if a general persistence error occurs
     */
    protected boolean removeSnapshotInternal(File fileSnapshot, String sSnapshot)
        {
        File     fileLock = new File(fileSnapshot.getParentFile(), fileSnapshot.getName() + ".lck");
        FileLock lock     = null;
        if (fileSnapshot.exists())
            {
            // acquire process level lock
            lock = FileHelper.lockFile(fileLock);
            }
        if (lock == null)
            {
            return false;
            }

        File fileDest = null;
        try
            {
            Path        pathSnapshot = fileSnapshot.toPath();
            Path        pathDest     = null;
            int         nAttempt     = 128;
            IOException error        = null;

            // generate a hidden directory (hidden from snapshot listings) to move
            // the snapshot to, immediately attempting to delete the directory
            // Note: upon failure a destination path is occasionally re-generated
            //       in a *hope* to avoid some OS error, but is not necessary as
            //       only one process will attempt to remove a unique snapshot name
            //       for each shared volume thus there should be no contention
            do
                {
                if ((nAttempt & (nAttempt - 1)) == 0) // nAttempt is a power of 2
                    {
                    pathDest = createHiddenSnapshotDirectory(fileSnapshot.getName());
                    fileDest = pathDest.toFile();
                    }
                try
                    {
                    // windoze has been known to give false-negatives in windows function
                    // MoveFileEx, hence the compensation in the while condition
                    Files.move(pathSnapshot, pathDest,
                            StandardCopyOption.REPLACE_EXISTING);
                    }
                catch (IOException e)
                    {
                    error = e;
                    }
                }
            while (--nAttempt > 0 &&
                    (!fileDest.exists() || !fileDest.isDirectory() || fileSnapshot.exists()));

            if (nAttempt > 0)
                {
                FileHelper.deleteDir(fileDest);
                }
            else
                {
                // the move failed; last ditch attempt to remove the snapshot
                FileHelper.deleteDir(fileSnapshot);
                if (fileSnapshot.exists())
                    {
                    fileDest = fileSnapshot; // for logging
                    throw error;
                    }
                }
            }
        catch (IOException e)
            {
            String sMsg = "Unable to remove snapshot directory " + FileHelper.getPath(fileDest) +
                    "; subsequent snapshot creations will not succeed with the name: " + sSnapshot + '\n' +
                    e + '\n' + Base.getStackTrace(e);
            CacheFactory.log(sMsg, CacheFactory.LOG_ERR);

            throw CachePersistenceHelper.ensurePersistenceException(e, sMsg);
            }
        finally
            {
            fileLock.delete();
            FileHelper.unlockFile(lock);
            }
        return false;
        }

    /**
     * Return a Path to a hidden directory within the snapshot directory that
     * does not exist.
     *
     * @param sPrefix  a prefix for the directory name
     *
     * @return a Path to a hidden directory within the snapshot directory that
     *         does not exist
     */
    protected Path createHiddenSnapshotDirectory(String sPrefix)
        {
        Path path;
        do
            {
            path = f_fileSnapshot.toPath().resolve(DELETED_PREFIX +
                    sPrefix + '-' + f_atomicRemovesCounter.incrementAndGet());
            }
        while (path.toFile().exists());

        return path;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Return a human readable description of this AbstractPersistenceManager.
     *
     * @return a human readable description
     */
    @Override
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass())
                + "(ActiveDirectory=" + f_fileActive
                + ", SnapshotDirectory=" + f_fileSnapshot
                + ')';
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the optional DaemonPool used to execute tasks.
     *
     * @return the DaemonPool or <tt>null</tt> if one hasn't been configured
     */
    public DaemonPool getDaemonPool()
        {
        return m_pool;
        }

    /**
     * Configure the DaemonPool used to execute tasks.
     *
     * @param pool  the DaemonPool
     */
    public void setDaemonPool(DaemonPool pool)
        {
        m_pool = pool;
        }


    // ----- inner class ----------------------------------------------------

    /**
     * Continuation implementation that accepts a Throwable and throws a
     * PersistenceException. If the provided Throwable is a
     * PersistenceException, it is thrown as is; otherwise, a wrapper
     * PersistenceException is thrown.
     */
    public static class DefaultFailureContinuation
            implements Continuation<Throwable>
        {
        /**
         * Create a new DefaultFailureContinuation for the given exception
         * source.
         *
         * @param oSource the object that threw the exception
         */
        public DefaultFailureContinuation(Object oSource)
            {
            f_oSource = oSource;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void proceed(Throwable e)
            {
            // rethrow PersistenceException as-is
            if (e instanceof PersistenceException)
                {
                throw(PersistenceException) e;
                }

            // handle abstract extensions
            if (f_oSource instanceof AbstractPersistenceEnvironment)
                {
                throw((AbstractPersistenceEnvironment) f_oSource).ensurePersistenceException(e);
                }

            if (f_oSource instanceof AbstractPersistenceManager)
                {
                throw((AbstractPersistenceManager) f_oSource).ensurePersistenceException(e);
                }

            if (f_oSource instanceof AbstractPersistenceManager.AbstractPersistentStore)
                {
                throw((AbstractPersistenceManager.AbstractPersistentStore) f_oSource).ensurePersistenceException(e);
                }

            // handle custom implementations
            PersistenceException ee = new PersistenceException(e);

            if (f_oSource instanceof PersistenceEnvironment)
                {
                ee.initPersistenceEnvironment((PersistenceEnvironment) f_oSource);
                }
            else if (f_oSource instanceof PersistenceManager)
                {
                ee.initPersistenceManager((PersistenceManager) f_oSource);
                }
            else if (f_oSource instanceof PersistentStore)
                {
                ee.initPersistentStore((PersistentStore) f_oSource);
                }

            throw ee;
            }

        // ----- data members -----------------------------------------------

        /**
         * The exception source.
         */
        private final Object f_oSource;
        }

    // ----- constants ------------------------------------------------------

    /**
     * An empty String array (by definition immutable).
     */
    protected static final String[] NO_STRINGS = new String[0];

    /**
     * A filename prefix for deleted snapshots used to handle concurrent
     * deletions.
     */
    protected static final String DELETED_PREFIX = ".";

    // ----- data members ---------------------------------------------------

    /**
     * An atomic counter counter used during {@link #removeSnapshot(String)
     * snapshot removal}.
     */
    protected final AtomicInteger f_atomicRemovesCounter = new AtomicInteger();

    /**
     * The data directory of the active persistence manager.
     */
    protected final File f_fileActive;

    /**
     * The snapshot directory.
     */
    protected final File f_fileSnapshot;

    /**
     * An optional trash directory.
     */
    protected final File f_fileTrash;

    /**
     * This singleton active manager.
     */
    protected AbstractPersistenceManager<?> m_managerActive;

    /**
     * The map of snapshots, keyed by snapshot name.
     */
    protected final Map<String, AbstractPersistenceManager> f_mapSnapshots
            = new HashMap<>();

    /**
     * An optional DaemonPool used to execute tasks.
     */
    protected DaemonPool m_pool;
    }
