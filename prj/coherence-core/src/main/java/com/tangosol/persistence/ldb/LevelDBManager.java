package com.tangosol.persistence.ldb;

import com.oracle.coherence.persistence.FatalAccessException;
import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistentStore;

import com.oracle.datagrid.persistence.OfflinePersistenceInfo;
import com.oracle.datagrid.persistence.PersistenceTools;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.GuardSupport;

import com.tangosol.persistence.AbstractPersistenceManager;

import com.tangosol.util.Binary;
import com.tangosol.util.BitHelper;
import com.tangosol.util.Unsafe;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.fusesource.leveldbjni.JniDBFactory;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;

/**
 * PersistenceManager implementation that uses LevelDB.
 *
 * @author jh  2012.10.04
 */
public class LevelDBManager
        extends AbstractPersistenceManager<LevelDBManager.LevelDBStore>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new LevelDBManager.
     *
     * @param fileData   the directory containing the LevelDB databases
     *                   managed by this LevelDBManager
     * @param fileTrash  an optional trash directory
     * @param sName      an optional name to give the new manager
     *
     * @throws IOException on error creating the data or trash directory
     */
    public LevelDBManager(File fileData, File fileTrash, String sName)
            throws IOException
        {
        super(fileData, fileTrash, sName);
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
        return "LDB";
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
        // create a snapshot of the specified manager
        executeTaskExclusive(new Task()
            {
            @Override
            public void execute()
                {
                Map<String, LevelDBStore> map = getPersistentStoreMap();
                for (LevelDBStore store : map.values())
                    {
                    GuardSupport.heartbeat();

                    File fileDirFrom = store.getDataDirectory();
                    File fileDirTo   = new File(fileSnapshot, fileDirFrom.getName());
                    try
                        {
                        FileHelper.copyDir(fileDirFrom, fileDirTo);
                        }
                    catch (IOException e)
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
     * {@inheritDoc}
     */
    @Override
    protected PersistenceTools instantiatePersistenceTools(OfflinePersistenceInfo info)
        {
        return new AbstractPersistenceSnapshotTools(getDataDirectory(), info)
            {
            // ----- PersistenceTools methods -------------------------------

            @Override
            public void validate()
                {
                // currently there is no way to "validate" a LevelDB environment like
                // we can do with BDB, so getting the statistics is the next best thing.
                // A issue (COH-12673) has been created to track this
                getStatistics();
                }
            };
        }

    // ----- inner class: LevelDBStore --------------------------------------

    /**
     * Factory method for LevelDBStore implementations managed by this
     * LevelDBManager.
     *
     * @param sId  the identifier of the store to create
     *
     * @return a new LevelDBStore with the given identifier
     */
    @Override
    protected LevelDBStore instantiatePersistentStore(String sId)
        {
        return new LevelDBStore(sId);
        }

    /**
     * PersistentStore implementation that uses LevelDB.
     *
     * @author jh  2012.10.04
     */
    protected class LevelDBStore
            extends AbstractPersistenceManager<LevelDBStore>.AbstractPersistentStore
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a new LevelDBStore that is backed by a LevelDB database.
         *
         * @param sId  the identifier for this store
         */
        protected LevelDBStore(String sId)
            {
            super(sId);
            }

        // ----- AbstractPersistentStore methods ----------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        protected void validateExtentId(long lExtentId)
            {
            if (lExtentId == METADATA_EXTENT)
                {
                throw new IllegalArgumentException("reserved extent identifier");
                }
            super.validateExtentId(lExtentId);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void openInternal()
            {
            // open the database
            if (m_db == null)
                {
                Options options = new Options();
                options.createIfMissing(true);
                options.cacheSize(0);

                try
                    {
                    // open the database
                    m_db = JniDBFactory.factory.open(f_dirStore, options);
                    }
                catch (IOException e)
                    {
                    throw ensurePersistenceException(
                            new FatalAccessException(
                                    "error opening the LevelDB database in directory \""
                                            + f_dirStore + '"', e));
                    }
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void releaseInternal()
            {
            // close the database
            DB db = m_db;
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
                m_db = null;
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean deleteInternal()
            {
            try
                {
                JniDBFactory.factory.destroy(f_dirStore, new Options());
                return true;
                }
            catch (IOException e)
                {
                return false;
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void loadExtentIdsInternal(Set<Long> set)
            {
            ReadBuffer buf = loadInternal(METADATA_EXTENT, EXTENT_IDS_KEY);

            byte[] ab = buf == null ? null : getByteArrayUnsafe(buf);
            if (ab != null && ab.length > 0)
                {
                // read the list of longs
                for (int i = 0, c = BitHelper.toInt(ab, 0), of = 4; i < c; ++i, of += 8)
                    {
                    set.add(Long.valueOf(BitHelper.toLong(ab, of)));
                    }
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void createExtentInternal(long lExtentId)
            {
            saveExtentIds(null /*oToken*/);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void deleteExtentInternal(long lExtentId)
            {
            DB db = m_db;
            if (db != null)
                {
                WriteBatch batch  = (WriteBatch) begin();
                boolean    fAbort = true;
                DBIterator iter   = db.iterator();
                try
                    {
                    iter.seekToFirst();
                    while (iter.hasNext())
                        {
                        Map.Entry<byte[], byte[]> entry = iter.next();

                        // extract the extent identifier
                        byte[] abKey = entry.getKey();
                        long   lId   = extractExtentId(abKey);

                        // delete the entry if necessary
                        if (lExtentId == lId)
                            {
                            batch.delete(abKey);
                            }
                        }

                    // update the set of known extent identifiers
                    saveExtentIds(batch);
                    commit(batch);
                    }
                finally
                    {
                    try
                        {
                        iter.close();
                        }
                    catch (IOException e)
                        {
                        // ignore
                        }
                    if (fAbort)
                        {
                        abort(batch);
                        }
                    }
                }
            }


        /**
         * {@inheritDoc}
         */
        @Override
        protected void truncateExtentInternal(long lExtentId)
            {
            deleteExtentInternal(lExtentId);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ReadBuffer loadInternal(long lExtentId, ReadBuffer bufKey)
            {
            try
                {
                byte[] ab = ensureDatabase().get(createKey(lExtentId, bufKey));
                return ab == null ? null : UNSAFE.newBinary(ab, 0, ab.length);
                }
            catch (DBException e)
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
                ensureWriteBatch(oToken).put(createKey(lExtentId, bufKey),
                        getByteArrayUnsafe(bufValue));
                }
            catch (DBException e)
                {
                // WriteBatch#put isn't documented as throwing DBException,
                // but handle it just in case...
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
                ensureWriteBatch(oToken).delete(createKey(lExtentId, bufKey));
                }
            catch (DBException e)
                {
                // WriteBatch#delete isn't documented as throwing DBException,
                // but handle it just in case...
                throw ensurePersistenceException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void iterateInternal(Visitor<ReadBuffer> visitor)
            {
            DB db = ensureDatabase();
            try
                {
                try (DBIterator iter = db.iterator())
                    {
                    iter.seekToFirst();
                    while (iter.hasNext())
                        {
                        Map.Entry<byte[], byte[]> entry = iter.next();

                        // extract the extent identifier
                        byte[] abKey = entry.getKey();
                        long lId = extractExtentId(abKey);
                        Long LId = Long.valueOf(lId);

                        // visit the entry iff the extent identifier is known
                        if (f_setExtentIds.contains(LId))
                            {
                            byte[] abValue = entry.getValue();
                            ReadBuffer bufKey = extractKeyContent(abKey);
                            ReadBuffer bufValue = UNSAFE.newBinary(abValue, 0, abValue.length);
                            if (!visitor.visit(lId, bufKey, bufValue))
                                {
                                return;
                                }
                            }
                        }
                    }
                }
            catch (Throwable e)
                {
                // DBException or Throwable from Visitor#vist call
                throw ensurePersistenceException(e);
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
                return ensureDatabase().createWriteBatch();
                }
            catch (DBException e)
                {
                // DB#createWriteBatch isn't documented as throwing DBException,
                // but handle it just in case...
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
                WriteBatch batch = ensureWriteBatch(oToken);
                try
                    {
                    ensureDatabase().write(batch);
                    }
                finally
                    {
                    try
                        {
                        batch.close();
                        }
                    catch (IOException e)
                        {
                        // ignore
                        }
                    }
                }
            catch (DBException e)
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
                ensureWriteBatch(oToken).close();
                }
            catch (IOException e)
                {
                throw ensurePersistenceException(e);
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
         * Ensure that the underlying LevelDB database has been opened before
         * returning it.
         *
         * @return the opened LevelDB database
         */
        protected DB ensureDatabase()
            {
            DB db = m_db;
            if (db == null)
                {
                throw new IllegalStateException("the LevelDB database \""
                        + f_dirStore + "\" is not open");
                }
            return db;
            }

        /**
         * Ensure that the given token is a WriteBatch.
         *
         * @param oToken  the token
         *
         * @return the token cast to a WriteBatch
         */
        protected WriteBatch ensureWriteBatch(Object oToken)
            {
            if (oToken instanceof WriteBatch)
                {
                return (WriteBatch) oToken;
                }
            throw new IllegalArgumentException("illegal token: " + oToken);
            }

        /**
         * Create and return a byte array that can be used as a key to
         * represent the given extent identifier and key content in the
         * underlying store.
         *
         * @param lExtentId  the extent identifier for the key
         * @param bufKey     the key content
         *
         * @return a new key
         */
        protected byte[] createKey(long lExtentId, ReadBuffer bufKey)
            {
            assert bufKey != null;

            int    cb = bufKey.length();
            byte[] ab = new byte[cb + 8];

            // extent identifier
            BitHelper.toBytes(lExtentId, ab, 0);

            // copy key content
            bufKey.copyBytes(0, cb, ab, 8);

            return ab;
            }

        /**
         * Extract and return the extent identifier associated with the
         * specified {@link #createKey key}.
         *
         * @param abKey  the key
         *
         * @return the extent identifier for the given key
         */
        protected long extractExtentId(byte[] abKey)
            {
            assert abKey != null && abKey.length >= 8;
            return BitHelper.toLong(abKey);
            }

        /**
         * Extract and return the key content associated with the specified
         * {@link #createKey key}.
         *
         * @param abKey  the key
         *
         * @return the key content for the key
         */
        protected ReadBuffer extractKeyContent(byte[] abKey)
            {
            assert abKey != null && abKey.length >= 8;
            return UNSAFE.newBinary(abKey, 8, abKey.length - 8);
            }

        /**
         * Save the set of known extent identifiers to the persistent store.
         *
         * @param oToken  optional token that represents a set of mutating
         *                operations to be committed as an atomic unit; if
         *                null, the update will be committed to the store
         *                automatically by this method
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a write lock on this persistent store.
         */
        protected void saveExtentIds(Object oToken)
            {
            int    c  = f_setExtentIds.size();
            byte[] ab = new byte[4 + c * 8]; // 4 bytes for the count, 8 bytes per long

            // write the list of longs
            BitHelper.toBytes(c, ab, 0);
            int of = 4;
            for (Long LId : f_setExtentIds)
                {
                BitHelper.toBytes(LId.longValue(), ab, of);
                of += 8;
                }

            // perform the store either by adding the operation to the
            // supplied batch or to a newly created batch if one hasn't
            // been supplied
            boolean fAbort  = oToken == null;
            boolean fCommit = fAbort;
            if (fCommit)
                {
                oToken = begin();
                }

            try
                {
                storeInternal(METADATA_EXTENT, EXTENT_IDS_KEY,
                        UNSAFE.newBinary(ab, 0, ab.length), oToken);

                // commit the change, if necessary
                if (fCommit)
                    {
                    commit(oToken);
                    fAbort = false;
                    }
                }
            finally
                {
                // abort the change, if necessary
                if (fAbort)
                    {
                    abort(oToken);
                    }
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The underlying LevelDB database.
         */
        protected DB m_db;
        }


    // ----- constants ------------------------------------------------------

    /**
     * The extent identifier used to store metadata.
     */
    public static final long METADATA_EXTENT = Long.MIN_VALUE;

    /**
     * The key of the LevelDB entry that stores the set of valid extent
     * identifiers.
     */
    protected static final ReadBuffer EXTENT_IDS_KEY = new Binary();

    /**
     * Unsafe singleton.
     */
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    }
