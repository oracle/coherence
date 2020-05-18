/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.bdb;

import com.oracle.coherence.persistence.ConcurrentAccessException;
import com.oracle.coherence.persistence.FatalAccessException;
import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistentStore;

import com.oracle.datagrid.persistence.OfflinePersistenceInfo;
import com.oracle.datagrid.persistence.PersistenceTools;

import com.sleepycat.je.CacheMode;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DiskOrderedCursor;
import com.sleepycat.je.DiskOrderedCursorConfig;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.EnvironmentLockedException;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.VerifyConfig;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.dbi.EnvironmentFailureReason;
import com.sleepycat.je.util.DbBackup;
import com.sleepycat.je.util.LogVerificationInputStream;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.DaemonPool;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardian;
import com.tangosol.net.cache.KeyAssociation;

import com.tangosol.persistence.AbstractPersistenceManager;
import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.SafePersistenceWrappers;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.Unsafe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PersistenceManager implementation that uses BerkeleyDB.
 *
 * @author jh  2012.10.04
 */
public class BerkeleyDBManager
        extends AbstractPersistenceManager<BerkeleyDBManager.BerkeleyDBStore>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new BerkeleyDBManager.
     *
     * @param fileData   the directory containing the BerkeleyDB environments
     *                   managed by this BerkeleyDBManager
     * @param fileTrash  an optional trash directory
     * @param sName      an optional name to give the new manager
     *
     * @throws IOException on error creating the data or trash directory
     */
    public BerkeleyDBManager(File fileData, File fileTrash, String sName)
            throws IOException
        {
        super(fileData, fileTrash, sName);

        f_fRemoteData = !com.oracle.coherence.common.io.Files.isLocal(fileData);

        String sProp = EnvironmentParams.LOG_USE_ODSYNC.getName();

        if (f_fRemoteData && (USER_SPECIFIED_PROPERTIES == null || !USER_SPECIFIED_PROPERTIES.containsKey(sProp)))
            {
            // our remote FS detection is far from perfect and the performance cost is high if we set this when we
            // don't need it, so let the user know.  Note the cost of not setting it is potential data corruption.
            CacheFactory.log("\"" + fileData + "\" appears to reference a remote file-system and as such " +
                    "Coherence persistence is enabling \"" + SYS_PROP_PREFIX + sProp + "\" in order ensure the " +
                    "integrity of remote commits. As this may impact write performance you may explicitly set the " +
                    "system property to \"false\" to override this decision; though this is only recommended if the " +
                    "location is actually a local file-system.", CacheFactory.LOG_INFO);
            }
        }

    // ----- AbstractPersistenceManager methods -----------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getImplVersion()
        {
        return 0;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getStorageFormat()
        {
        return "BDB";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getStorageVersion()
        {
        return 0;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create a snapshot of this manager.
     *
     * @param fileSnapshot  the directory of the snapshot
     *
     * @throws PersistenceException if a general persistence error occurs
     */
    protected void createSnapshot(final File fileSnapshot)
        {
        executeTaskExclusive(new Task()
            {
            @Override
            public void execute()
                {
                Map<String, BerkeleyDBStore> map = getPersistentStoreMap();
                for (BerkeleyDBStore store : map.values())
                    {
                    GuardSupport.heartbeat();

                    File fileDirFrom = store.getDataDirectory();
                    try
                        {
                        // create a new snapshot directory
                        File fileDirTo = FileHelper.ensureDir(
                                new File(fileSnapshot, fileDirFrom.getName()));

                        // checkpoint the BerkeleyDB environment to
                        // reduce recovery time
                        store.checkPoint();

                        // create a new BerkeleyDB lock file
                        File fileLock = new File(fileDirTo, BerkeleyDBManager.LOCK_FILENAME);
                        if (!fileLock.createNewFile())
                            {
                            throw ensurePersistenceException(
                                    new IOException("cannot create new lock file\""
                                            + fileLock + '"'));
                            }

                        // copy metadata
                        CachePersistenceHelper.copyMetadata(fileDirFrom, fileDirTo);

                        // create the snapshot
                        Environment env    = store.m_env;
                        DbBackup backup = new DbBackup(env);
                        backup.startBackup();
                        try
                            {
                            copyLogFiles(env, backup.getLogFilesInBackupSet(), fileDirTo);
                            }
                        finally
                            {
                            backup.endBackup();
                            }
                        }
                    catch (Exception e)
                        {
                        throw ensurePersistenceException(e, "error creating snapshot \""
                                + fileSnapshot + "\" while copying persistent store \""
                                + fileDirFrom + '"');
                        }
                    }
                }
            });
        }

    /**
     * Copy the specified log files from the given BerkeleyDB environment to
     * the specified destination directory, verifying the log files as they
     * are being copied.
     *
     * @param env      the source BerkeleyDB environment
     * @param asFiles  the name of the log files to copy
     * @param fileDir  the destination directory
     *
     * @throws IOException on I/O error
     * @throws DatabaseException on verification error
     */
    protected static void copyLogFiles(Environment env, String[] asFiles, File fileDir)
            throws IOException, DatabaseException

        {
        File fileEnv = env.getHome();

        int cFiles = asFiles == null ? 0 : asFiles.length;
        if (cFiles > 0)
            {
            final byte[] BUF = new byte[10240];

            for (int i = 0; i < cFiles; ++i)
                {
                String sFile = asFiles[i];
                if (sFile != null && !(sFile = sFile.trim()).isEmpty())
                    {
                    File fileFrom = new File(fileEnv, sFile);
                    File fileTo   = new File(fileDir, sFile);

                    InputStream  in  = null;
                    OutputStream out = null;
                    try
                        {
                        in  = new LogVerificationInputStream(env,
                                new FileInputStream(fileFrom), sFile);
                        out = new FileOutputStream(fileTo);

                        while (true)
                            {
                            int cb = in.read(BUF);
                            if (cb < 0)
                                {
                                break;
                                }
                            out.write(BUF, 0, cb);
                            }
                        }
                    finally
                        {
                        // clean up
                        if (in != null)
                            {
                            try
                                {
                                in.close();
                                }
                            catch (IOException e)
                                {
                                // ignore
                                }
                            }
                        if (out != null)
                            {
                            try
                                {
                                out.close();
                                }
                            catch (IOException e)
                                {
                                // ignore
                                }
                            }
                        }
                    }
                }
            }
        }

    /**
     * Set the BerkeleyDB config parameter name and value iff it does not exist
     * in the System properties.
     *
     * @param cfg     the {@link EnvironmentConfig} to set the parameter against
     * @param sName   the name of the parameter
     * @param sValue  the value of the parameter
     */
    protected static void setConfigParam(EnvironmentConfig cfg, String sName, String sValue)
        {
        if (System.getProperty(sName, null) == null)
            {
            cfg.setConfigParam(sName, sValue);
            }
        }

    @Override
    protected PersistenceTools instantiatePersistenceTools(OfflinePersistenceInfo info)
        {
        return new AbstractPersistenceSnapshotTools(getDataDirectory(), info)
            {
            // ----- PersistenceTools methods -------------------------------

            @Override
            public void validate()
                {
                String[] asFileList      = f_info.getGUIDs();
                int      nImplVersion    = -1;
                int      nStorageVersion = -1;
                File     fileStore ;

                for (String sFileName : asFileList)
                    {
                    fileStore = new File(f_dirSnapshot, sFileName);
                    validateBDBEnvironment(fileStore);
                    validateStoreSealed(sFileName);

                    // validate that the metadata is consistent across all stores
                    try
                        {
                        Properties props = CachePersistenceHelper.readMetadata(fileStore);

                        int nThisImplVersion    = Integer.valueOf(props.getProperty(CachePersistenceHelper.META_IMPL_VERSION));
                        int nThisStorageVersion = Integer.valueOf(props.getProperty(CachePersistenceHelper.META_STORAGE_VERSION));

                        if (nImplVersion == -1)
                            {
                            // indicates that we have not yet set the impl version
                            nImplVersion    = nThisImplVersion;
                            nStorageVersion = nThisStorageVersion;
                            }
                        else
                            {
                            // check if current values differ from existing values.
                            // The process will stop on the first values that do not match
                            if (nThisImplVersion != nImplVersion || nThisStorageVersion != nStorageVersion)
                                {
                                throw new IllegalStateException(
                                        "Implementation and storage versions are inconsistent across stores in directory: "
                                                + f_dirSnapshot.getCanonicalPath());
                                }
                            }
                        }
                    catch (IOException ioe)
                        {
                        throw CachePersistenceHelper.ensurePersistenceException(ioe,
                                "Unable to read metadata for " + fileStore);
                        }
                    }
                }

            // ----- helpers ---------------------------------------------------------

            /**
             * Validate a BDB environment for completeness.
             *
             * @param fileEnvironment  the environment to validate
             */
            protected void validateBDBEnvironment(File fileEnvironment)
                {
                try (Environment env = new Environment(fileEnvironment, BerkeleyDBManager.ENVIRONMENT_CONFIG))
                    {
                    env.verify(VerifyConfig.DEFAULT, new PrintStream(System.out));
                    }
                catch (Exception e)
                    {
                    throw CachePersistenceHelper.ensurePersistenceException(e,
                            "Unable to validate BDB Environment at " + fileEnvironment.getAbsolutePath());
                    }
                }
            };
        }

    // ----- inner class: BerkeleyDBStore -----------------------------------

    /**
     * Factory method for BerkeleyDBStore implementations managed by this
     * BerkeleyDBManager.
     *
     * @param sId  the identifier of the store to create
     *
     * @return a new BerkeleyDBStore with the given identifier
     */
    @Override
    protected BerkeleyDBStore instantiatePersistentStore(String sId)
        {
        return new BerkeleyDBStore(sId);
        }

    /**
     * PersistentStore implementation that uses BerkeleyDB.
     *
     * @author jh  2012.10.04
     */
    protected class BerkeleyDBStore
            extends AbstractPersistenceManager<BerkeleyDBStore>.AbstractPersistentStore
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a new BerkeleyDBStore that is backed by a BerkeleyDB
         * environment.
         *
         * @param sId  the identifier for this store
         */
        protected BerkeleyDBStore(String sId)
            {
            super(sId);
            }

        // ----- AbstractPersistentStore methods ----------------------------

        @Override
        protected void copyAndOpenInternal(PersistentStore<ReadBuffer> storeFrom)
            {
            storeFrom = SafePersistenceWrappers.unwrap(storeFrom);

            if (storeFrom instanceof BerkeleyDBStore)
                {
                // copy the BDB journal files from the provided persistent
                // store to this store
                try
                    {
                    BerkeleyDBStore storeBDB = (BerkeleyDBStore) storeFrom;

                    storeBDB.validateMetadata();

                    Path dirStore = storeBDB.f_dirStore.toPath();

                    Files.walkFileTree(dirStore, EnumSet.noneOf(FileVisitOption.class), /*maxDepth*/ 1,
                            new SimpleFileVisitor<Path>()
                                {
                                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                                        throws IOException
                                    {
                                    File fileSrc = path.toFile();
                                    if (fileSrc.isFile() && fileSrc.getName().endsWith(".jdb"))
                                        {
                                        Files.copy(path, f_dirStore.toPath().resolve(fileSrc.getName()));
                                        }
                                    return FileVisitResult.CONTINUE;
                                    }

                                @Override
                                public FileVisitResult visitFileFailed(Path file, IOException exc)
                                        throws IOException
                                    {
                                    return FileVisitResult.CONTINUE;
                                    }
                                });

                    openInternal();
                    }
                catch (IOException | PersistenceException e)
                    {
                    delete(false);
                    throw e instanceof PersistenceException ? (PersistenceException) e :
                            ensurePersistenceException(e,
                            "Unable to copy from previous store to new store; from " +
                                    storeFrom + " to " + this);
                    }
                }
            else
                {
                super.copyAndOpenInternal(storeFrom);
                }
            }

        /**
         * Checkpoint the associated BDB Environment.
         */
        protected void checkPoint()
            {
            // checkpoint the BerkeleyDB environment to reduce recovery time
            ensureEnvironment().checkpoint(BerkeleyDBManager.CHECKPOINT_CONFIG);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void openInternal()
            {
            if (m_env == null)
                {
                EnvironmentConfig cfg = ENVIRONMENT_CONFIG.clone();

                if (f_fRemoteData)
                    {
                    // je docs strongly recommend enabling ODSYNC on remote file systems
                    String sProp = EnvironmentParams.LOG_USE_ODSYNC.getName();
                    if (USER_SPECIFIED_PROPERTIES == null || !USER_SPECIFIED_PROPERTIES.containsKey(sProp))
                        {
                        cfg.setConfigParam(sProp, Boolean.TRUE.toString());
                        }
                    // else; user seems to think they know better...
                    }

                // set lock timeout to the default guardian timeout
                DaemonPool pool = BerkeleyDBManager.this.getDaemonPool();
                if (pool != null)
                    {
                    Guardian guardian = pool.getGuardian();
                    if (guardian != null)
                        {
                        long cMillis = guardian.getDefaultGuardTimeout();
                        if (cMillis != cfg.getLockTimeout(TimeUnit.MILLISECONDS))
                            {
                            // limit the timeout to the MAX_LOCK_TIMEOUT value (see Bug 19233558)
                            cfg.setLockTimeout(Math.min(cMillis, MAX_LOCK_TIMEOUT), TimeUnit.MILLISECONDS);
                            }
                        }
                    }


                while (true)
                    {
                    try
                        {
                        m_env = new Environment(f_dirStore, cfg);
                        break;
                        }
                    catch (DatabaseException | AssertionError eTop)
                        {
                        if (eTop instanceof EnvironmentFailureException &&
                            EnvironmentFailureReason.FOUND_COMMITTED_TXN == ((EnvironmentFailureException) eTop).getReason())
                            {
                            CacheFactory.log("The persistence store " + FileHelper.getPath(f_dirStore) +
                                    " appears to be corrupt and could only be partially read\n" +
                                    eTop.getMessage() + '\n' + Base.getStackTrace(eTop), CacheFactory.LOG_ERR);

                            // the config param below will ensure we do not receive
                            // the same exception again
                            cfg = cfg.clone()
                                     .setConfigParam(EnvironmentConfig.HALT_ON_COMMIT_AFTER_CHECKSUMEXCEPTION, Boolean.FALSE.toString());
                            }
                        else
                            {
                            String sMessage = "error opening the BerkeleyDB environment in directory \""
                                    + f_dirStore + '"';

                            if (eTop instanceof EnvironmentLockedException)
                                {
                                // should never happen, but just in case...
                                throw ensurePersistenceException(
                                        new ConcurrentAccessException(sMessage, eTop));
                                }

                            throw ensurePersistenceException(
                                    new FatalAccessException(sMessage, eTop));
                            }
                        }
                    }
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void releaseInternal()
            {
            // close any open databases
            for (Database db : f_mapDB.values())
                {
                try
                    {
                    db.close();
                    }
                catch (Throwable e)
                    {
                    // ignore
                    }
                }
            f_mapDB.clear();

            // release the environment
            Environment env = m_env;
            if (env != null)
                {
                try
                    {
                    env.close();
                    }
                catch (Throwable e)
                    {
                    // ignore
                    }
                m_env = null;
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean deleteInternal()
            {
            // nothing to do
            return true;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void loadExtentIdsInternal(Set<Long> set)
            {
            try
                {
                for (String sName : ensureEnvironment().getDatabaseNames())
                    {
                    Long LId = Long.valueOf(sName);
                    if (set.add(Long.valueOf(sName)))
                        {
                        openDatabase(LId.longValue());
                        }
                    }
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void createExtentInternal(long lExtentId)
            {
            openDatabase(lExtentId);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void deleteExtentInternal(long lExtentId)
            {
            removeDatabase(lExtentId);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void moveExtentInternal(long lOldExtentId, long lNewExtentId)
            {
            renameDatabase(lOldExtentId, lNewExtentId);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void truncateExtentInternal(long lExtentId)
            {
            truncateDatabase(lExtentId);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ReadBuffer loadInternal(long lExtentId, ReadBuffer bufKey)
            {
            DatabaseEntry entryKey   = new DatabaseEntry(getByteArrayUnsafe(bufKey));
            DatabaseEntry entryValue = new DatabaseEntry();

            try
                {
                OperationStatus status = ensureDatabase(lExtentId).get(
                        null /*txn*/, entryKey, entryValue, null /*lockMode*/);
                return status ==  OperationStatus.SUCCESS
                        ? newBinaryUnsafe(entryValue.getData())
                        : null;
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void storeInternal(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue, Object oToken)
            {
            try
                {
                ensureDatabase(lExtentId).put(ensureTransaction(oToken),
                        new DatabaseEntry(getByteArrayUnsafe(bufKey)),
                        new DatabaseEntry(getByteArrayUnsafe(bufValue)));
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            maintainEnvironment(bufKey, bufValue);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void eraseInternal(long lExtentId, ReadBuffer bufKey, Object oToken)
            {
            try
                {
                ensureDatabase(lExtentId).delete(ensureTransaction(oToken),
                        new DatabaseEntry(getByteArrayUnsafe(bufKey)));
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            maintainEnvironment(bufKey, null /*bufValue*/);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void iterateInternal(Visitor<ReadBuffer> visitor)
            {
            // iterate each database
            for (Database db : f_mapDB.values())
                {
                long          lExtentId  = Long.valueOf(db.getDatabaseName()).longValue();
                DatabaseEntry entryKey   = new DatabaseEntry();
                DatabaseEntry entryValue = new DatabaseEntry();

                try
                    {
                    try (DiskOrderedCursor cursor = db.openCursor(CURSOR_CONFIG))
                        {
                        while (cursor.getNext(entryKey, entryValue, null /*lockMode*/)
                                == OperationStatus.SUCCESS)
                            {
                            // extract the key and value
                            ReadBuffer bufKey = newBinaryUnsafe(entryKey.getData());
                            ReadBuffer bufValue = newBinaryUnsafe(entryValue.getData());
                            if (!visitor.visit(lExtentId, bufKey, bufValue))
                                {
                                return;
                                }
                            }
                        }
                    }
                catch (Throwable e)
                    {
                    // DatabaseException or Throwable from Visitor#visit call
                    throw ensurePersistenceException(e);
                    }
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object beginInternal()
            {
            try
                {
                return ensureEnvironment().beginTransaction(null /*parent*/,
                        null /*txnConfig*/);
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void commitInternal(Object oToken)
            {
            try
                {
                ensureTransaction(oToken).commit();
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void abortInternal(Object oToken)
            {
            try
                {
                ensureTransaction(oToken).abort();
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Return the total size of the log files managed by this
         * BerkeleyDBStore.
         *
         * @return the total log file size in bytes
         */
        public long getTotalLogFileSize()
            {
            try
                {
                return ensureEnvironment().getStats(StatsConfig.DEFAULT).getTotalLogSize();
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * Return the content of the given buffer as a byte array, using an
         * unsafe method if the given buffer is a Binary.
         *
         * @param buf  the buffer
         *
         * @return the contents of the given buffer as a byte array
         */
        private byte[] getByteArrayUnsafe(ReadBuffer buf)
            {
            if (buf instanceof Binary)
                {
                Binary bin = (Binary) buf;
                if (UNSAFE.getArrayOffset(bin) == 0)
                    {
                    byte[] ab = UNSAFE.getByteArray(bin);
                    if (ab.length == bin.length())
                        {
                        return ab;
                        }
                    }
                }
            else if (buf instanceof ByteArrayReadBuffer)
                {
                ByteArrayReadBuffer babuf = (ByteArrayReadBuffer) buf;
                if (babuf.getRawOffset() == 0)
                    {
                    byte[] ab = babuf.getRawByteArray();
                    if (ab.length == babuf.length())
                        {
                        return ab;
                        }
                    }
                }
            return buf.toByteArray();
            }

        /**
         * Create a new Binary with the given content without copying it.
         *
         * @param ab  the content of the new Binary
         *
         * @return a new Binary with the given content
         */
        private Binary newBinaryUnsafe(byte[] ab)
            {
            return UNSAFE.newBinary(ab, 0, ab.length);
            }

        /**
         * Ensure that the underlying BerkeleyDB environment has been opened
         * before returning it.
         *
         * @return the opened BerkeleyDB environment
         */
        protected Environment ensureEnvironment()
            {
            Environment env = m_env;
            if (env == null)
                {
                throw new IllegalStateException("the BerkeleyDB environment \""
                        + f_dirStore + "\" is not open");
                }
            return env;
            }

        /**
         * Ensure that the underlying BerkeleyDB database used to store keys
         * associated with the given extent identifier has been opened before
         * returning it.
         *
         * @param lExtentId  the extent identifier
         *
         * @return the BerkeleyDB database used to store keys associated with
         *         the given extent identifier
         */
        protected Database ensureDatabase(long lExtentId)
            {
            Database db = f_mapDB.get(Long.valueOf(lExtentId));
            if (db == null)
                {
                throw new IllegalStateException("the BerkeleyDB database \""
                        + lExtentId + "\" is not open");
                }

            return db;
            }

        /**
         * Open the underlying BerkeleyDB database used to store keys
         * associated with the given extent identifier.
         *
         * @param lExtentId  the extent identifier
         *
         * @return the BerkeleyDB database used to store keys associated with
         *         the given extent identifier
         */
        protected Database openDatabase(long lExtentId)
            {
            try
                {
                // open the database
                Database db = ensureEnvironment().openDatabase(null /*txn*/,
                        String.valueOf(lExtentId), DATABASE_CONFIG);

                // cache and return the database
                f_mapDB.put(Long.valueOf(lExtentId), db);
                return db;
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * Close the underlying BerkeleyDB database used to store keys
         * associated with the given extent identifier.
         *
         * @param lExtentId  the extent identifier
         */
        protected void closeDatabase(long lExtentId)
            {
            Database db = f_mapDB.remove(Long.valueOf(lExtentId));
            if (db != null)
                {
                try
                    {
                    db.close();
                    }
                catch (Throwable e)
                    {
                    // ignore
                    }
                }
            }

        /**
         * Remove the underlying BerkeleyDB database used to store keys
         * associated with the given extent identifier.
         * <p>
         * Note: this will close and release the Database handle.
         *
         * @param lExtentId  the extent identifier
         */
        protected void removeDatabase(long lExtentId)
            {
            closeDatabase(lExtentId);
            try
                {
                ensureEnvironment().removeDatabase(null /*txn*/,
                        String.valueOf(lExtentId));
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * Rename the underlying BerkeleyDB database from the old extent id
         * to the new extent id.
         * <p>
         * Note: this will close and release the Database handle for the old
         *       extent. It is the callers responsibility to ensure the new extent.
         *
         * @param lOldExtentId  the old extent identifier
         * @param lNewExtentId  the new extent identifier
         */
        protected void renameDatabase(long lOldExtentId, long lNewExtentId)
            {
            closeDatabase(lOldExtentId);
            try
                {
                ensureEnvironment().renameDatabase(/*txn*/ null,
                        String.valueOf(lOldExtentId), String.valueOf(lNewExtentId));
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * Truncate the underlying BerkeleyDB database used to store keys
         * associated with the given extent identifier.
         * <p>
         * Note: this will temporarily close the Database handle.
         *
         * @param lExtentId  the extent identifier
         */
        protected void truncateDatabase(long lExtentId)
            {
            closeDatabase(lExtentId);
            try
                {
                ensureEnvironment().truncateDatabase(/*txn*/ null,
                        String.valueOf(lExtentId), /*returnCount*/ false);
                }
            catch (DatabaseException e)
                {
                throw ensurePersistenceException(e);
                }
            finally
                {
                // ensure a Database is always available after returning control
                openDatabase(lExtentId);
                }
            }

        /**
         * Ensure that the given token is a Transaction.
         *
         * @param oToken  the token
         *
         * @return the token cast to a Transaction
         */
        protected Transaction ensureTransaction(Object oToken)
            {
            if (oToken instanceof Transaction)
                {
                return (Transaction) oToken;
                }
            throw new IllegalArgumentException("illegal token: " + oToken);
            }

        /**
         * Perform any necessary maintenance of the underlying BerkeleyDB
         * environment.
         *
         * @param bufKey    the key of the current operation
         * @param bufValue  the value of the current operation (optional)
         */
        protected void maintainEnvironment(ReadBuffer bufKey, ReadBuffer bufValue)
            {
            if (MAINTENANCE_ENABLED)
                {
                // calculate the approximate number of bytes written to the
                // environment by the current operation
                int cbCurrent = bufKey.length();
                if (bufValue != null)
                    {
                    cbCurrent += bufValue.length();
                    }

                // update the approximate running count of bytes written
                long cbLast    = f_cbWritten.getAndAdd(cbCurrent);
                long cbWritten = cbLast + cbCurrent;

                // determine if maintenance is already in progress
                if (m_fMaintenanceScheduled)
                    {
                    // nothing to do
                    return;
                    }

                // determine if it's time to checkpoint the environment
                boolean fCheckpointRequired = m_cbLastCheckpoint >= CHECKPOINT_INTERVAL;

                // determine if it's time to clean the environment
                boolean fCleanRequired = m_cbLastClean >= CLEAN_INTERVAL;

                // determine if it's time to compress the environment
                boolean fCompressRequired = getSafeTimeMillis()
                        >= m_ldtLastCompress + COMPRESS_INTERVAL;

                // determine if it's time to update statistics
                boolean fStatsRequired = f_cChecks.incrementAndGet() == STATS_CHECK_COUNT ||
                        (cbWritten >= STATS_CHECK_BYTES && cbLast < STATS_CHECK_BYTES);

                if (fCheckpointRequired || fCleanRequired || fCompressRequired || fStatsRequired)
                    {
                    synchronized (this)
                        {
                        if (!m_fMaintenanceScheduled)
                            {
                            // schedule the maintenance for execution
                            m_fMaintenanceScheduled = true;
                            BerkeleyDBManager.this.submitTask(
                                    new MaintenanceTask(
                                            fCheckpointRequired,
                                            fCleanRequired,
                                            fCompressRequired));
                            }
                        }
                    }
                }
            }

        // ----- inner class: MaintenanceTask -------------------------------

        /**
         * Task used to perform maintenance on the underlying BerkeleyDB
         * environment.
         */
        protected class MaintenanceTask
                extends Task
                implements KeyAssociation
            {

            // ----- constructors -------------------------------------------

            /**
             * Create a new MaintenanceTask.
             *
             * @param fCheckpoint  true if the environment should be check-pointed
             * @param fClean       true if the environment should be cleaned
             * @param fCompress    true if the environment should be compressed
             */
            public MaintenanceTask(boolean fCheckpoint, boolean fClean, boolean fCompress)
                {
                f_fCheckpoint = fCheckpoint;
                f_fClean      = fClean;
                f_fCompress   = fCompress;
                }

            // ----- Task methods -------------------------------------------

            /**
             * Perform maintenance of the BerkeleyDB environment.
             */
            @Override
            public void execute()
                {
                long ldtStart        = -1L;
                long ldtLastCompress = -1L;

                if (MAINTENANCE_DEBUG_ENABLED)
                    {
                    ldtStart        = getSafeTimeMillis();
                    ldtLastCompress = m_ldtLastCompress;
                    }

                BerkeleyDBStore store = BerkeleyDBStore.this;
                store.lockRead();
                try
                    {
                    // make sure the environment hasn't been released and is
                    // still valid (see releaseInternal())
                    Environment env = store.m_env;
                    if (env != null && env.isValid())
                        {
                        // perform maintenance
                        if (f_fClean)
                            {
                            while (env.cleanLog() > 0)
                                {
                                }
                            }
                        if (f_fCheckpoint)
                            {
                            env.checkpoint(CHECKPOINT_CONFIG);
                            }
                        if (f_fCompress)
                            {
                            env.compress();
                            store.m_ldtLastCompress = getSafeTimeMillis();
                            }

                        // update log file statistics
                        long cbLog           = store.getTotalLogFileSize();
                        long cbLogCheckpoint = store.m_cbLogCheckpoint;
                        long cbLogClean      = store.m_cbLogClean;
                        if (f_fCheckpoint || cbLog < cbLogCheckpoint)
                            {
                            store.m_cbLastCheckpoint = 0;
                            store.m_cbLogCheckpoint  = cbLog;
                            }
                        else
                            {
                            store.m_cbLastCheckpoint = cbLog - cbLogCheckpoint;
                            }
                        if (f_fClean || cbLog < cbLogClean)
                            {
                            store.m_cbLastClean = 0;
                            store.m_cbLogClean  = cbLog;
                            }
                        else
                            {
                            store.m_cbLastClean = cbLog - cbLogClean;
                            }
                        }
                    }
                catch (Throwable e)
                    {
                    CacheFactory.log("Error maintaining the BerkeleyDB environment in directory "
                            + store.f_dirStore + "\": " + printStackTrace(e),
                            CacheFactory.LOG_WARN);
                    }
                finally
                    {
                    reset();
                    store.unlockRead();
                    }

                // display maintenance debug info if enabled and it was performed
                if (ldtStart != -1  && (f_fCheckpoint || f_fClean || f_fCompress))
                    {
                    CacheFactory.log("Maintenance of BDB Environment: Store=" +  BerkeleyDBStore.this.getId() +
                                ", Clean=" + f_fClean +
                                ", Checkpoint=" + f_fCheckpoint + ", Compress="  +
                                f_fCompress + " took " + (getSafeTimeMillis() - ldtStart) + " ms" +
                                (ldtLastCompress == -1 ? "" : ", last compress=" + new Date(ldtLastCompress)),
                                 CacheFactory.LOG_DEBUG);
                    }
                }

            /**
             * {@inheritDoc}
             */
            @Override
            public void notifyCanceled(Throwable eCause)
                {
                reset();
                }

            // ----- KeyAssociation methods ---------------------------------

            @Override
            public Object getAssociatedKey()
                {
                return BerkeleyDBStore.this.getId();
                }

            // ----- helper methods -----------------------------------------

            /**
             * Reset state associated with this MaintenanceTask.
             */
            protected void reset()
                {
                BerkeleyDBStore store = BerkeleyDBStore.this;
                store.f_cChecks.set(0);
                store.f_cbWritten.set(0L);
                store.m_fMaintenanceScheduled = false;
                }

            // ----- data members -------------------------------------------

            /**
             * True if the BerkeleyDB environment should be check-pointed.
             */
            protected final boolean f_fCheckpoint;

            /**
             * True if the BerkeleyDB environment should be cleaned.
             */
            protected final boolean f_fClean;

            /**
             * True if the BerkeleyDB environment should be compressed.
             */
            protected final boolean f_fCompress;
            }

        // ----- data members -----------------------------------------------

        /**
         * A map of opened Database instances, keyed by extent identifier.
         */
        protected final Map<Long, Database> f_mapDB = new HashMap<>();

        /**
         * The underlying BerkeleyDB environment.
         */
        protected Environment m_env;

        /**
         * The total log file size of the BerkeleyDB environment after it was
         * last check-pointed.
         */
        protected long m_cbLogCheckpoint;

        /**
         * The total log file size of the BerkeleyDB environment after it was
         * last cleaned.
         */
        protected long m_cbLogClean;

        /**
         * The number of bytes that have been written to the log files of the
         * BerkeleyDB environment since it was last check-pointed.
         */
        protected volatile long m_cbLastCheckpoint;

        /**
         * The number of bytes that have been written to the log files of the
         * BerkeleyDB environment since it was last cleaned.
         */
        protected volatile long m_cbLastClean;

        /**
         * The last time the BerkeleyDB environment was compressed.
         */
        protected volatile long m_ldtLastCompress;

        /**
         * True if maintenance of the BerkeleyDB environment is currently
         * being performed.
         */
        protected volatile boolean m_fMaintenanceScheduled;

        /**
         * The number of maintenance checks that have been performed since
         * statistics were last updated.
         */
        protected final AtomicInteger f_cChecks = new AtomicInteger();

        /**
         * The approximate number of bytes that have been written to the
         * BerkeleyDB environment since statistics were last updated.
         */
        protected final AtomicLong f_cbWritten = new AtomicLong();
        }


    // ----- data members ---------------------------------------------------

    /**
     * True if the data directory is on a remote file system.
     */
    protected final boolean f_fRemoteData;

    // ----- constants ------------------------------------------------------

    /**
     * System property prefix for all BDB Store specific properties.
     */
    public static final String SYS_PROP_PREFIX = "coherence.distributed.persistence.bdb.";

    /**
     * The maximum value of the je.lock.timeout parameter in milliseconds (75 seconds)
     */
    protected static final int MAX_LOCK_TIMEOUT = 75*60*1000;

    /**
     * The name of the file used to prevent concurrent access to a BerkeleyDB
     * environment.
     */
    protected static final String LOCK_FILENAME = "je.lck";

    /**
     * True if BerkeleyDBStore instances should perform their own BerkeleyDB
     * environment maintenance. (Enabled by default)
     */
    protected static final boolean MAINTENANCE_ENABLED =
            Config.getBoolean(SYS_PROP_PREFIX + "maintenance.enabled", true)  &&
            System.getProperty(EnvironmentConfig.ENV_RUN_CHECKPOINTER, null)  == null &&
            System.getProperty(EnvironmentConfig.ENV_RUN_CLEANER, null)       == null &&
            System.getProperty(EnvironmentConfig.ENV_RUN_IN_COMPRESSOR, null) == null;

    /**
     * True if BerkeleyDBStore instances should collect statistics about BDB
     * performance and maintenance. This option is only recommended for for use
     * as directed by Oracle Support. (Disabled by default)
     */
    protected static final boolean STATS_ENABLED =
            Config.getBoolean(SYS_PROP_PREFIX + "stats.enabled", false);

    /**
     * True if debug information should be displayed for BDB maintenance operations.
     * This option is only recommended for for use as directed by Oracle Support.
     * (Disabled by default)
     */
    protected static final boolean MAINTENANCE_DEBUG_ENABLED =
            Config.getBoolean(SYS_PROP_PREFIX + "maintenance.debug", false);

    /**
     * The maximum starting size of a BerkeleyDB log buffer.
     */
    protected static final long LOG_BUFFER_SIZE = parseMemorySize("25KB");

    /**
     * The number of BerkeleyDB log buffers.
     */
    protected static final int LOG_NUM_BUFFERS = 3;

    /**
     * The interval (in bytes) between attempts to checkpoint each BerkeleyDB
     * environment.
     */
    protected static final long CHECKPOINT_INTERVAL = (long) (Long.valueOf(
            EnvironmentParams.LOG_FILE_MAX.getDefault()) * .75);

    /**
     * The interval (in bytes) between attempts to clean each BerkeleyDB
     * environment.
     */
    protected static final long CLEAN_INTERVAL = Long.valueOf(
            EnvironmentParams.LOG_FILE_MAX.getDefault()) / 4;

    /**
     * The interval (in milliseconds) between attempts to compress each
     * BerkeleyDB environment.
     */
    protected static final long COMPRESS_INTERVAL = 15 * 1000L;   // 15 seconds

    /**
     * The interval (in bytes) between successive BerkeleyDB stats retrievals.
     */
    protected static final long STATS_CHECK_BYTES
            = Math.min(CLEAN_INTERVAL, CHECKPOINT_INTERVAL) / 2;

    /**
     * The interval (in number of store operations) between successive
     * BerkeleyDB stats retrievals.
     */
    protected static final long STATS_CHECK_COUNT = 1000;

    /**
     * The BerkeleyDB environment configuration.
     */
    protected static final EnvironmentConfig ENVIRONMENT_CONFIG;

    /**
     * The BerkeleyDB database configuration.
     */
    private static final DatabaseConfig DATABASE_CONFIG
            = new DatabaseConfig().setAllowCreate(true).setTransactional(true);

    /**
     * The BerkeleyDB cursor configuration.
     */
    private static final DiskOrderedCursorConfig CURSOR_CONFIG
            = new DiskOrderedCursorConfig();

    /**
     * The BerkeleyDB checkpoint configuration.
     */
    private static final CheckpointConfig CHECKPOINT_CONFIG
            = new CheckpointConfig().setForce(true);

    /**
     * Unsafe singleton.
     */
    private static final Unsafe UNSAFE = AccessController.doPrivileged(
        (PrivilegedAction<Unsafe>) Unsafe::getUnsafe);

    /**
     * Any je.* system properties we're passing down to je, i.e. external customizations.
     */
    private static final Properties USER_SPECIFIED_PROPERTIES;

    static
        {
        // set the JE logging level to WARNING
        // see: http://docs.oracle.com/cd/E17277_02/html/GettingStartedGuide/managelogging.html
        Level logLevel = Level.parse(System.getProperty("com.sleepycat.je.level", Level.WARNING.getName()));
        Logger.getLogger("com.sleepycat.je").setLevel(logLevel);

        // initialize environment configuration propagating any JVM settings
        // intended for BDB
        Properties props = null;
        for (String sKey : EnvironmentParams.SUPPORTED_PARAMS.keySet())
            {
            // we lookup both "je." and "coherence.persistence.distributed.bdb.je.", but pass both down as just "je."; this
            // way users can customize coherence's use of bdb without impacting other users in the same process.
            // Note: we don't insulate ourselves from their "je." settings, as we'd initially released just "je."
            // based lookup, and need to retain it for backwards compatibility.
            String sValue = System.getProperty(SYS_PROP_PREFIX + sKey, System.getProperty(sKey));

            if (sValue != null)
                {
                if (props == null)
                    {
                    props = new Properties();
                    }
                props.put(sKey, sValue);
                }
            }

        USER_SPECIFIED_PROPERTIES = props;

        EnvironmentConfig cfg = ENVIRONMENT_CONFIG = props == null
                ? new EnvironmentConfig() : new EnvironmentConfig(props);

        cfg.setAllowCreate(true)
           .setCacheMode(CacheMode.EVICT_LN)
           .setDurability(Durability.COMMIT_WRITE_NO_SYNC);

        setConfigParam(cfg, EnvironmentConfig.LOG_BUFFER_SIZE, String.valueOf(LOG_BUFFER_SIZE));
        setConfigParam(cfg, EnvironmentConfig.LOG_NUM_BUFFERS, String.valueOf(LOG_NUM_BUFFERS));
        setConfigParam(cfg, EnvironmentConfig.LOG_USE_WRITE_QUEUE, Boolean.FALSE.toString());
        setConfigParam(cfg, EnvironmentConfig.HALT_ON_COMMIT_AFTER_CHECKSUMEXCEPTION, Boolean.TRUE.toString());
        setConfigParam(cfg, EnvironmentConfig.STATS_COLLECT, Boolean.FALSE.toString());

        cfg.setSharedCache(true);
        cfg.setTransactional(true);

        // configure a very long timeout value by default
        cfg.setLockTimeout(305, TimeUnit.SECONDS);

        if (MAINTENANCE_ENABLED)
            {
            setConfigParam(cfg, EnvironmentConfig.ENV_RUN_CHECKPOINTER, Boolean.FALSE.toString());
            setConfigParam(cfg, EnvironmentConfig.ENV_RUN_CLEANER, Boolean.FALSE.toString());
            setConfigParam(cfg, EnvironmentConfig.ENV_RUN_IN_COMPRESSOR, Boolean.FALSE.toString());
            }
        }
    }
