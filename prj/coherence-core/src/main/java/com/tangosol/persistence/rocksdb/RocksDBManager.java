package com.tangosol.persistence.rocksdb;

import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistentStore;

import com.oracle.datagrid.persistence.OfflinePersistenceInfo;
import com.oracle.datagrid.persistence.PersistenceTools;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.GuardSupport;

import com.tangosol.persistence.AbstractPersistenceManager;
import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.SafePersistenceWrappers;

import com.tangosol.util.Binary;
import com.tangosol.util.Unsafe;

import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.rocksdb.BackupEngine;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.DBOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Transaction;
import org.rocksdb.TransactionDB;
import org.rocksdb.TransactionDBOptions;
import org.rocksdb.WriteOptions;


/**
 * PersistenceManager implementation that uses RocksDB.
 *
 * @author mg  2022
 */
public class RocksDBManager
        extends AbstractPersistenceManager<RocksDBManager.RocksDBStore>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new RocksDBManager.
     *
     * @param fileData   the directory containing the RocksDB environments
     *                   managed by this RocksDBManager
     * @param fileTrash  an optional trash directory
     * @param sName      an optional name to give the new manager
     *
     * @throws IOException on error creating the data or trash directory
     */
    public RocksDBManager(File fileData, File fileTrash, String sName)
            throws IOException
        {
        super(fileData, fileTrash, sName);

        RocksDB.loadLibrary();
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
                Map<String, RocksDBStore> map = getPersistentStoreMap();
                for (RocksDBStore store : map.values())
                    {
                    GuardSupport.heartbeat();

                    File fileDirFrom = store.getDataDirectory();
                    try
                        {
                        // create a new snapshot directory
                        File fileDirTo = FileHelper.ensureDir(
                                new File(fileSnapshot, fileDirFrom.getName()));

                        // checkpoint the RocksDB environment to
                        // reduce recovery time
                        store.checkPoint();

                        // create a new RocksDB lock file
                        File fileLock = new File(fileDirTo, RocksDBManager.LOCK_FILENAME);
                        if (!fileLock.createNewFile())
                            {
                            throw ensurePersistenceException(
                                    new IOException("cannot create new lock file\""
                                                    + fileLock + '"'));
                            }

                        // copy metadata
                        CachePersistenceHelper.copyMetadata(fileDirFrom, fileDirTo);

                        // TODO
                        // create the snapshot
                        TransactionDB env    = store.m_env;
                        BackupEngine  backup = BackupEngine.open(env.getEnv(), null);
                        backup.createNewBackup(null);
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
            };
        }

    // ----- inner class: RocksDBStore -----------------------------------

    /**
     * Factory method for RocksDBStore implementations managed by this
     * RocksDBManager.
     *
     * @param sId  the identifier of the store to create
     *
     * @return a new RocksDBStore with the given identifier
     */
    @Override
    protected RocksDBStore instantiatePersistentStore(String sId)
        {
        return new RocksDBStore(sId);
        }

    /**
     * PersistentStore implementation that uses RocksDB.
     *
     * @author jh  2012.10.04
     */
    protected class RocksDBStore
            extends AbstractPersistenceManager<RocksDBStore>.AbstractPersistentStore
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a new RocksDBStore that is backed by a RocksDB
         * environment.
         *
         * @param sId  the identifier for this store
         */
        protected RocksDBStore(String sId)
            {
            super(sId);
            }

        // ----- AbstractPersistentStore methods ----------------------------

        @Override
        protected void copyAndOpenInternal(PersistentStore<ReadBuffer> storeFrom)
            {
            storeFrom = SafePersistenceWrappers.unwrap(storeFrom);

            if (storeFrom instanceof RocksDBStore)
                {
                // copy the RocksDB journal files from the provided persistent
                // store to this store
                try
                    {
                    RocksDBStore storeRocksDB = (RocksDBStore) storeFrom;

                    storeRocksDB.validateMetadata();

                    Path dirStore = storeRocksDB.f_dirStore.toPath();

                    Files.walk(dirStore).forEach(source ->
                         {
                         Path destination = Paths.get(f_dirStore.getAbsolutePath(), source.toString()
                                .substring(f_dirStore.getAbsolutePath().length()));
                         try
                             {
                             Files.copy(source, destination);
                             }
                         catch (FileAlreadyExistsException faee)
                             {
                             // do nothing
                             }
                         catch (IOException ioe)
                             {
                             ioe.printStackTrace();
                             // TODO resolve this
                             // throw ioe;
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
         * Checkpoint the associated RocksDB Environment.
         */
        protected void checkPoint()
            {
            // checkpoint the RocksDB environment to reduce recovery time
            // TODO
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void openInternal()
            {
            if (!f_dirStore.exists())
                {
                try
                    {
                    f_dirStore.mkdirs();
                    }
                catch (Exception e)
                    {
                    throw ensurePersistenceException(e);
                    }
                }

            if (m_env == null)
                {
                try
                    {
                    List<byte[]> extentIds = TransactionDB.listColumnFamilies(
                            new Options(),
                            f_dirStore.getAbsolutePath());

                    List<ColumnFamilyDescriptor> descriptors = new ArrayList<>();
                    descriptors.add(new ColumnFamilyDescriptor("default".getBytes(StandardCharsets.UTF_8)));
                    extentIds.forEach((id) -> descriptors.add(new ColumnFamilyDescriptor(id)));

                    List<ColumnFamilyHandle> handles = new ArrayList<>();

                    m_env = TransactionDB.open(new DBOptions().setCreateIfMissing(true),
                                               new TransactionDBOptions(),
                                               f_dirStore.getAbsolutePath(),
                                               descriptors,
                                               handles);

                    for (ColumnFamilyHandle db : handles)
                        {
                        String dbName = new String(db.getName(), StandardCharsets.UTF_8);
                        if (!"default".equals(dbName))
                            {
                            f_mapDB.put(Long.valueOf(dbName), db);
                            }
                        }
                    }
                catch (RocksDBException re)
                    {
                    throw ensurePersistenceException(re);
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
            for (ColumnFamilyHandle db : f_mapDB.values())
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
            TransactionDB env = m_env;
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
                List<byte[]> extentIds = m_env.listColumnFamilies(new Options(), f_dirStore.getAbsolutePath());

                for (byte[] extentId : extentIds)
                    {
                    if (!"default".equals(new String(extentId, StandardCharsets.UTF_8)))
                        {
                        Long LId = Long.valueOf(new String(extentId, StandardCharsets.UTF_8));
                        if (set.add(LId))
                            {
                            openDatabase(LId.longValue());
                            }
                        }
                    }
                }
            catch (RocksDBException re)
                {
                throw ensurePersistenceException(re);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void createExtentInternal(long lExtentId)
            {
            try
                {
                ensureDatabase(lExtentId);
                }
            catch (IllegalStateException ise)
                {
                openDatabase(lExtentId);
                }
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
            try
                {
                ensureDatabase(lNewExtentId);
                }
            catch (IllegalStateException ise)
                {
                openDatabase(lNewExtentId);
                }

            iterateInternal((lExtentId, bufKey, bufValue) ->
                    {
                    if (lExtentId == lOldExtentId)
                        {
                        storeInternal(lNewExtentId, bufKey, bufValue, null);
                        }
                    return true;
                    });

            try
                {
                m_env.dropColumnFamily(ensureDatabase(lOldExtentId));
                }
            catch (RocksDBException re)
                {

                }
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
            try
                {
                byte[] ab = m_env.get(ensureDatabase(lExtentId), getByteArrayUnsafe(bufKey));

                return ab != null ?
                       newBinaryUnsafe(ab) :
                       null;
                }
            catch (RocksDBException e)
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
                m_env.put(ensureDatabase(lExtentId),
                          getByteArrayUnsafe(bufKey),
                          getByteArrayUnsafe(bufValue));
                }
            catch (RocksDBException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void eraseInternal(long lExtentId, ReadBuffer bufKey, Object oToken)
            {
            try
                {
                m_env.delete(ensureDatabase(lExtentId), getByteArrayUnsafe(bufKey));
                }
            catch (RocksDBException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void iterateInternal(Visitor<ReadBuffer> visitor)
            {
            // iterate each database
            for (ColumnFamilyHandle db : f_mapDB.values())
                {
                try
                    {
                    long lExtentId = Long.valueOf(new String(db.getDescriptor().getName(), StandardCharsets.UTF_8));

                    if (!visitor.visitExtent(lExtentId))
                        {
                        continue;
                        }

                    RocksIterator iter = m_env.newIterator(db);
                    iter.seekToFirst();

                    for (; iter.isValid(); iter.next())
                        {
                        ReadBuffer bufKey = newBinaryUnsafe(iter.key());
                        ReadBuffer bufValue = newBinaryUnsafe(iter.value());
                        if (!visitor.visit(lExtentId, bufKey, bufValue))
                            {
                            return;
                            }
                        }

                    // TODO: iterate from bufStart   = visitor.visitFromKey();
                    }
                catch (Throwable e)
                    {
                    // RocksDBException or Throwable from Visitor#visit call
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
            return m_env.beginTransaction(new WriteOptions());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void commitInternal(Object oToken)
            {
            if (oToken != null)
                {
                try
                    {
                    ((Transaction) oToken).commit();
                    }
                catch (RocksDBException e)
                    {
                    throw ensurePersistenceException(e);
                    }
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void abortInternal(Object oToken)
            {
            if (oToken != null)
                {
                try
                    {
                    ((Transaction) oToken).rollback();
                    }
                catch (RocksDBException e)
                    {
                    throw ensurePersistenceException(e);
                    }
                }
            }

        // ----- helpers ----------------------------------------------------

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
         * Ensure that the underlying RocksDB environment has been opened
         * before returning it.
         *
         * @return the opened RocksDB environment
         */
        protected TransactionDB ensureEnvironment()
            {
            TransactionDB env = m_env;
            if (env == null || !f_dirStore.exists())
                {
                throw new IllegalStateException("the RocksDB environment \""
                                                + f_dirStore + "\" is not open");
                }
            return env;
            }

        /**
         * Ensure that the underlying RocksDB database used to store keys
         * associated with the given extent identifier has been opened before
         * returning it.
         *
         * @param lExtentId  the extent identifier
         *
         * @return the RocksDB database used to store keys associated with
         *         the given extent identifier
         */
        protected ColumnFamilyHandle ensureDatabase(long lExtentId)
            {
            ColumnFamilyHandle db = f_mapDB.get(Long.valueOf(lExtentId));
            if (db == null)
                {
                throw new IllegalStateException("the RocksDB database column family \""
                                                + lExtentId + "\" is not open");
                }

            return db;
            }

        /**
         * Open the underlying RocksDB database used to store keys
         * associated with the given extent identifier.
         *
         * @param lExtentId  the extent identifier
         *
         * @return the RocksDB database used to store keys associated with
         *         the given extent identifier
         */
        protected ColumnFamilyHandle openDatabase(long lExtentId)
            {
            ColumnFamilyHandle db = null;
            try
                {
                // open the database
                db = m_env.createColumnFamily(
                        new ColumnFamilyDescriptor(String.valueOf(lExtentId).getBytes(StandardCharsets.UTF_8)));

                // cache and return the database
                f_mapDB.put(Long.valueOf(lExtentId), db);
                }
            catch (RocksDBException e)
                {
                if (!e.getStatus().getState().contains("already exists"))
                    {
                    throw ensurePersistenceException(e);
                    }

                db = f_mapDB.get(Long.valueOf(lExtentId));
                }

            return db;
            }

        /**
         * Close the underlying RocksDB database used to store keys
         * associated with the given extent identifier.
         *
         * @param lExtentId  the extent identifier
         */
        protected void closeDatabase(long lExtentId)
            {
            ColumnFamilyHandle db = f_mapDB.remove(Long.valueOf(lExtentId));
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
         * Remove the underlying RocksDB database used to store keys
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
                ColumnFamilyHandle db = f_mapDB.get(lExtentId);
                m_env.dropColumnFamily(db);
                }
            catch (RocksDBException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * Rename database
         *
         * @param lOldExtentId  the old extent identifier
         * @param lNewExtentId  the new extent identifier
         */
        protected void renameDatabase(long lOldExtentId, long lNewExtentId)
            {
            // No op with RocksDB
            }

        /**
         * Truncate the underlying RocksDB database used to store keys
         * associated with the given extent identifier.
         * <p>
         * Note: this will temporarily close the Database handle.
         *
         * @param lExtentId  the extent identifier
         */
        protected void truncateDatabase(long lExtentId)
            {
            // TODO
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

        @Override
        protected AutoCloseable instantiateExclusiveClosable()
            {
            AutoCloseable parent = super.instantiateExclusiveClosable();
            return () ->
            {
            parent.close();
            };
            }

        // ----- data members -----------------------------------------------

        /**
         * A map of opened Database instances, keyed by extent identifier.
         */
        protected final Map<Long, ColumnFamilyHandle> f_mapDB = new HashMap<>();

        /**
         * The underlying RocksDB environment.
         */
        protected TransactionDB m_env;

        /**
         * The total log file size of the RocksDB environment after it was
         * last check-pointed.
         */
        protected long m_cbLogCheckpoint;

        /**
         * The total log file size of the RocksDB environment after it was
         * last cleaned.
         */
        protected long m_cbLogClean;

        /**
         * The number of bytes that have been written to the log files of the
         * RocksDB environment since it was last check-pointed.
         */
        protected volatile long m_cbLastCheckpoint;

        /**
         * The number of bytes that have been written to the log files of the
         * RocksDB environment since it was last cleaned.
         */
        protected volatile long m_cbLastClean;

        /**
         * The last time the RocksDB environment was compressed.
         */
        protected volatile long m_ldtLastCompress;

        /**
         * True if maintenance of the RocksDB environment is currently
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
         * RocksDB environment since statistics were last updated.
         */
        protected final AtomicLong f_cbWritten = new AtomicLong();
        }

    // ----- constants ------------------------------------------------------

    /**
     * System property prefix for all RocksDB Store specific properties.
     */
    public static final String SYS_PROP_PREFIX = "coherence.distributed.persistence.rocksdb.";

    /**
     * The maximum value of the je.lock.timeout parameter in milliseconds (75 seconds)
     */
    protected static final int MAX_LOCK_TIMEOUT = 75*60*1000;

    /**
     * The name of the file used to prevent concurrent access to a RocksDB
     * environment.
     */
    protected static final String LOCK_FILENAME = "rocksdb.lck";

    /**
     * Unsafe singleton.
     */
    private static final Unsafe UNSAFE = AccessController.doPrivileged(
            (PrivilegedAction<Unsafe>) Unsafe::getUnsafe);

    }
