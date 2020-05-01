/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.common.base.Collector;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.persistence.AsyncPersistenceException;
import com.oracle.coherence.persistence.ConcurrentAccessException;
import com.oracle.coherence.persistence.FatalAccessException;
import com.oracle.coherence.persistence.OfflinePersistenceInfo;
import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.coherence.persistence.PersistenceStatistics;
import com.oracle.coherence.persistence.PersistenceTools;
import com.oracle.coherence.persistence.PersistentStore;

import com.tangosol.internal.util.DaemonPool;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.cache.KeyAssociation;

import com.tangosol.persistence.AbstractPersistenceManager.AbstractPersistentStore;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.NullImplementation;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;

import java.nio.channels.FileLock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstract implementation of a ReadBuffer-based PersistentManager.
 *
 * @param <PS>  the type of AbstractPersistentStore
 *
 * @author jh  2012.10.04
 */
public abstract class AbstractPersistenceManager<PS extends AbstractPersistentStore>
        extends Base
        implements PersistenceManager<ReadBuffer>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new AbstractPersistenceManager.
     *
     * @param fileData   the directory used to store persisted data
     * @param fileTrash  an optional trash directory
     * @param sName      an optional name to give the new manager
     *
     * @throws IOException on error creating the data or trash directory
     */
    public AbstractPersistenceManager(File fileData, File fileTrash, String sName)
            throws IOException
        {
        f_dirActive = FileHelper.ensureDir(fileData);
        f_dirTrash  = fileTrash;
        f_dirLock   = new File(f_dirActive, CachePersistenceHelper.DEFAULT_LOCK_DIR);
        f_sName     = sName == null ? fileData.toString() : sName;
        }

    // ----- PersistenceManager interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
        {
        return f_sName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public PS createStore(String sId)
        {
        // validate the store ID
        sId = validatePersistentStoreId(sId);

        // create the requested store if necessary

        return f_mapStores.computeIfAbsent(sId, s ->
            {
            ensureActive();

            return instantiatePersistentStore(s);
            });
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public PersistentStore<ReadBuffer> open(String sId, PersistentStore<ReadBuffer> storeFrom)
        {
        return open(sId, storeFrom, null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public PersistentStore<ReadBuffer> open(String sId, PersistentStore<ReadBuffer> storeFrom, Collector<Object> collector)
        {
        // validate the store ID
        sId = validatePersistentStoreId(sId);

        // create the requested store if necessary; if the store was created
        // it will be opened (either sync or async) outside of the
        // ConcurrentHashMap synchronization
        PersistentStore[] aStore = new PersistentStore[1];

        PS store = f_mapStores.computeIfAbsent(sId, s ->
            {
            ensureActive();

            PS storeNew = instantiatePersistentStore(s);

            aStore[0] = storeNew;

            return storeNew;
            });

        if (store == aStore[0])
            {
            store.submitOpen(storeFrom, collector);
            }

        // Note: an unopened store can be returned in both the sync (collector == null)
        //       and async cases; any operation that requires an opened store
        //       will block the calling thread until the store is either opened
        //       or fails to open - see #ensureReady()
        return store;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(String sId)
        {
        ensureActive();

        // validate the store ID
        sId = validatePersistentStoreId(sId);

        AbstractPersistentStore store = f_mapStores.remove(sId);
        if (store != null)
            {
            store.release();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(String sId, boolean fSafe)
        {
        ensureActive();

        // validate the store ID
        sId = validatePersistentStoreId(sId);

        AbstractPersistentStore store = f_mapStores.remove(sId);
        if (store == null)
            {
            // create a new store, but don't bother opening it
            store = instantiatePersistentStore(sId);
            }

        return store.delete(fSafe);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] list()
        {
        // Scan for persistence storages in the data directory. Commonly, the
        // persistence storages are structured as individual directories, each
        // holding instance-specific files. We can ensure that a given
        // directory has a correct format by checking for a properly
        // configured metadata.
        String[] asNames = f_dirActive.list((dir, name) ->
            {
            File fileEnv = new File(dir, name);
            if (fileEnv.isDirectory() &&
                !f_dirLock.getName().equals(fileEnv.getName()))
                {
                try
                    {
                    Properties prop = readMetadata(fileEnv);
                    if (isMetadataComplete(prop))
                        {
                        if (isMetadataCompatible(prop))
                            {
                            return true;
                            }

                        CacheFactory.log("Skipping incompatible persistent store directory \""
                                + fileEnv + "\"", CacheFactory.LOG_WARN);
                        }
                    else
                        {
                        // we return true if the metadata is incomplete
                        // so that the incomplete store will be cleaned
                        // up (see {@link #open})
                        return true;
                        }
                    }
                catch (IOException e)
                    {
                    // we return true if the metadata cannot be read
                    // so that the incomplete store will be cleaned
                    // up (see AbstractPersistenceStore#open)
                    return true;
                    }
                }
            return false;
            });

        return asNames == null ? NO_STRINGS : asNames;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] listOpen()
        {
        if (f_mapStores.isEmpty())
            {
            return NO_STRINGS;
            }
        Set<String> setIds = f_mapStores.keySet();
        return setIds.toArray(new String[setIds.size()]);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(String sId, InputStream in)
            throws IOException
        {
        ensureActive();

        final DataInputStream inData = in instanceof DataInputStream
                ? (DataInputStream) in : new DataInputStream(in);

        // the store being materialized into must be new/empty
        AbstractPersistentStore store;
        synchronized (this)
            {
            if (f_mapStores.containsKey(sId))
                {
                throw new ConcurrentAccessException("the store \"" + sId
                        + "\" is currently open");
                }
            store = (AbstractPersistentStore) open(sId, null);
            store.lockWrite();
            }

        // read magic, metadata, and contents of the store from the stream
        try
            {
            // read and validate magic
            if (inData.readInt() != MAGIC)
                {
                throw new StreamCorruptedException("the data stream is unrecognized");
                }

            // read and validate version
            int nVersion = inData.readByte();
            if (nVersion > VERSION)
                {
                throw new IOException("the data stream is a newer version ("
                        + nVersion + ") than is supported by this manager ("
                        + VERSION + ")");
                }

            // read contents of the store
            while (true)
                {
                // read key and value lengths
                int cbKey = inData.readInt();
                if (cbKey < 0) // see #write()
                    {
                    break;
                    }
                int cbValue = inData.readInt();
                if (cbValue < 0)
                    {
                    throw new StreamCorruptedException();
                    }

                // read extent identifier
                long lExtentId = inData.readLong();

                // read key and value
                byte[] ab = new byte[cbKey + cbValue];

                try
                    {
                    inData.readFully(ab, 0, cbKey);
                    }
                catch (EOFException e)
                    {
                    throw new EOFException("Expected " + cbKey + " bytes for key but reached end of stream");
                    }

                try
                    {
                    inData.readFully(ab, cbKey, cbValue);
                    }
                catch (EOFException e)
                    {
                    throw new EOFException("Expected " + cbValue + " bytes for value but reached end of stream");
                    }

                ReadBuffer bufKey   = new ByteArrayReadBuffer(ab, 0, cbKey);
                ReadBuffer bufValue = new ByteArrayReadBuffer(ab, cbKey, cbValue);

                store.ensureExtent(lExtentId);
                store.store(lExtentId, bufKey, bufValue, null);
                }
            }
        catch (IOException e)
            {
            delete(sId, false);
            throw e;
            }
        finally
            {
            store.unlockWrite();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(String sId, OutputStream out)
            throws IOException
        {
        ensureActive();

        final DataOutputStream outData = out instanceof DataOutputStream
                ? (DataOutputStream) out : new DataOutputStream(out);

        // open the store
        AbstractPersistentStore store = (AbstractPersistentStore) open(sId, null);

        // write magic, metadata, and contents of the store to the stream
        store.lockRead();
        try
            {
            // write magic
            outData.writeInt(MAGIC);

            // write version
            outData.writeByte(VERSION);

            // write contents of the store
            final IOException[] ae = new IOException[1];
            store.iterate((lExtentId, bufKey, bufValue) ->
                {
                try
                    {
                    // write key and value lengths
                    outData.writeInt(bufKey.length());
                    outData.writeInt(bufValue.length());

                    // write extent identifier
                    outData.writeLong(lExtentId);

                    // write key and value
                    bufKey.writeTo((DataOutput) outData);
                    bufValue.writeTo((DataOutput) outData);
                    }
                catch (IOException e)
                    {
                    ae[0] = e;
                    return false;
                    }
                return true;
                });

            if (ae[0] == null)
                {
                // terminate the stream
                outData.writeInt(-1);
                }
            else
                {
                throw ae[0];
                }
            }
        finally
            {
            store.unlockRead();
            try
                {
                outData.flush();
                }
            catch (IOException e)
                {
                // ignore
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
        {
        m_fReleased = true;

        boolean fInterrupted = false;

        // give outstanding tasks a chance to execute
        Set<Task> set = NullImplementation.getSet();
        synchronized (f_setTasks)
            {
            // give outstanding tasks a chance to complete
            try
                {
                long ldtEnd = getSafeTimeMillis() + 5000L;
                while (!f_setTasks.isEmpty())
                    {
                    Blocking.wait(f_setTasks, 1000L);
                    if (getSafeTimeMillis() >= ldtEnd)
                        {
                        break;
                        }
                    }
                }
            catch (InterruptedException e)
                {
                // ignore
                fInterrupted = true;
                }

            if (!f_setTasks.isEmpty())
                {
                set = new HashSet<>(f_setTasks);
                }
            }

        // cancel all tasks that weren't executed
        for (Task task : set)
            {
            try
                {
                task.cancel(null /*eCause*/);
                }
            catch (Throwable e)
                {
                // ignore
                }
            }

        // close all open stores
        for (AbstractPersistentStore store : f_mapStores.values())
            {
            store.release();
            }

        // Note: we mark the PM as released and then clear the stores, while
        //       open checks the released flag under synchronization of the
        //       ConcurrentHashMap$Node; therefore an open will not insert data
        //       into mapStores after the clear operation below
        f_mapStores.clear();

        // notify the environment that this manager has been released
        AbstractPersistenceEnvironment env = m_env;
        if (env != null)
            {
            env.onReleased(this);
            }

        // reset the interrupt flag, if necessary
        if (fInterrupted)
            {
            Thread.currentThread().interrupt();
            }

        }

    @Override
    public PersistenceTools getPersistenceTools()
        {
        // open the first snapshot store to get the partition count
        String[] asGUIDs = list();
        if (asGUIDs.length == 0)
            {
            throw new IllegalArgumentException("snapshot must have at least one GUID");
            }

        String sGUID = asGUIDs[0];
        int    nVersion;
        int    cPartitions;

        PersistentStore<ReadBuffer> store = null;

        try
            {
            store = open(sGUID, null);
            cPartitions = CachePersistenceHelper.getPartitionCount(store);
            nVersion    = CachePersistenceHelper.getPersistenceVersion(store);
            }
        finally
            {
            if (store != null)
                {
                close(sGUID);
                }
            }

        OfflinePersistenceInfo info = new OfflinePersistenceInfo(cPartitions, getStorageFormat(),
                false, asGUIDs, getStorageVersion(), getImplVersion(), nVersion);

        return instantiatePersistenceTools(info);
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
        return ClassHelper.getSimpleName(getClass()) +
                '(' +
                    (f_sName == null ? "" : f_sName + ", ") +
                    (m_fReleased ? "in" : "") + "active)";
        }

    // ----- versioning support ---------------------------------------------

    /**
     * Return metadata for this manager.
     *
     * @return the metadata for this manager
     */
    protected Properties getMetadata()
        {
        Properties props = new Properties();
        props.setProperty(CachePersistenceHelper.META_IMPL_VERSION, String.valueOf(getImplVersion()));
        props.setProperty(CachePersistenceHelper.META_STORAGE_FORMAT, getStorageFormat());
        props.setProperty(CachePersistenceHelper.META_STORAGE_VERSION, String.valueOf(getStorageVersion()));

        return props;
        }

    /**
     * Read persistence metadata from the specified directory.
     *
     * @param fileDir  the directory to read metadata from
     *
     * @return the metadata
     *
     * @throws IOException on error reading the metadata file
     */
    protected Properties readMetadata(File fileDir)
            throws IOException
        {
        return CachePersistenceHelper.readMetadata(fileDir);
        }

    /**
     * Write persistence metadata for this manager to the specified directory.
     *
     * @param fileDir  the directory to write metadata to
     *
     * @throws IOException on error writing the metadata file
     */
    protected void writeMetadata(File fileDir)
            throws IOException
        {
        CachePersistenceHelper.writeMetadata(fileDir, getMetadata());
        }

    /**
     * Determine if the given metadata in the {@link Properties} is complete.
     *
     * @param prop  the metadata to analyze
     *
     * @return true if the given metadata is complete; false otherwise
     */
    protected boolean isMetadataComplete(Properties prop)
        {
        return CachePersistenceHelper.isMetadataComplete(prop);
        }

    /**
     * Determine if the given metadata is compatible with this manager.
     *
     * @param prop the metadata to analyze
     *
     * @return true if the given metadata is compatible with this manager;
     *         false otherwise
     */
    protected boolean isMetadataCompatible(Properties prop)
        {
        return CachePersistenceHelper.isMetadataCompatible(
                prop,
                getImplVersion(),
                getStorageFormat(),
                getStorageVersion());
        }

    /**
     * Return the implementation version of this manager.
     *
     * @return the implementation version of this manager
     */
    protected abstract int getImplVersion();

    /**
     * Return the storage format used by this manager.
     *
     * @return the storage format used by this manager
     */
    protected abstract String getStorageFormat();

    /**
     * Return the version of the storage format used by this manager.
     *
     * @return the version of the storage format used by this manager
     */
    protected abstract int getStorageVersion();

    // ----- helper methods -------------------------------------------------

    /**
     * Return a PersistenceException with the given cause. The returned
     * exception is also initialized with this manager and its environment
     * (if available).
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
     * The returned exception is also initialized with this manager and its
     * environment (if available).
     *
     * @param eCause    an optional cause
     * @param sMessage  an optional detail message
     *
     * @return a PersistenceException with the given cause and detail message
     */
    protected PersistenceException ensurePersistenceException(Throwable eCause, String sMessage)
        {
        AbstractPersistenceEnvironment env = m_env;
        PersistenceException e = env == null
                ? CachePersistenceHelper.ensurePersistenceException(eCause, sMessage)
                : env.ensurePersistenceException(eCause, sMessage);
        e.initPersistenceManager(this);
        return e;
        }

    /**
     * Return control if this PersistenceManager is still active, otherwise
     * throw an exception.
     *
     * @throws IllegalStateException if this PersistenceManager has been released
     */
    protected void ensureActive()
        {
        if (m_fReleased)
            {
            throw new IllegalStateException(getClass().getSimpleName() + " has been released.");
            }
        }

    /**
     * Validate that the given identifier can be used for a persistent store.
     *
     * @param sId  the identifier to check
     *
     * @return the validated identifier
     */
    public String validatePersistentStoreId(String sId)
        {
        if (sId == null)
            {
            throw new IllegalArgumentException("null identifier");
            }

        sId = sId.trim();
        if (sId.length() == 0)
            {
            throw new IllegalArgumentException("empty identifier");
            }

        return sId;
        }

    // ----- inner interface: Task ------------------------------------------

    /**
     * Runnable extension that adds the ability to notify the task that it
     * has been canceled.
     */
    public abstract class Task
            extends Base
            implements Runnable
        {

        // ----- Task methods -----------------------------------------------

        /**
         * Execute the task.
         */
        public abstract void execute();

        /**
         * Cancel execution of the task.
         *
         * @param eCause  the optional cause of the cancellation
         */
        public final synchronized void cancel(Throwable eCause)
            {
            if (f_canceled)
                {
                return;
                }
            try
                {
                notifyCanceled(eCause);
                }
            finally
                {
                f_canceled = true;
                notifyCompleted();
                }
            }

        /**
         * Notify the task that it has been canceled.
         *
         * @param eCause  the optional cause of the cancellation
         */
        protected void notifyCanceled(Throwable eCause)
            {
            // no-op
            }

        /**
         * Notify the task that is has been completed.
         */
        private void notifyCompleted()
            {
            // see AbstractPersistenceManager#release()
            Set<Task> set = AbstractPersistenceManager.this.f_setTasks;
            synchronized (set)
                {
                set.remove(this);
                if (set.isEmpty())
                    {
                    set.notifyAll();
                    }
                }
            }

        // ----- Runnable interface -----------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public final synchronized void run()
            {
            if (f_canceled)
                {
                return;
                }
            try
                {
                execute();
                }
            finally
                {
                notifyCompleted();
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * Canceled flag.
         */
        protected boolean f_canceled;
        }

    /**
     * Submit the given task for execution by the daemon pool.
     *
     * @param task  the task to submit
     */
    protected void submitTask(Task task)
        {
        DaemonPool pool = getDaemonPool();
        if (pool == null)
            {
            executeTask(task);
            }
        else
            {
            synchronized (f_setTasks)
                {
                f_setTasks.add(task);
                pool.add(task);
                }
            }
        }

    /**
     * Execute the specified task with the calling thread.
     *
     * @param task  the task to execute
     */
    protected void executeTask(Task task)
        {
        task.execute();
        }

    /**
     * Execute the specified task with the calling thread. No other access to
     * this manager or any of its persistent stores is guaranteed to occur
     * while the task is being executed.
     *
     * @param task  the task to execute
     */
    protected synchronized void executeTaskExclusive(Task task)
        {
        List<AbstractPersistentStore> list = new ArrayList<>(f_mapStores.size());
        try
            {
            // lock all open stores for write
            for (AbstractPersistentStore store : f_mapStores.values())
                {
                store.lockWrite();
                list.add(store);
                }

            executeTask(task);
            }
        finally
            {
            // release all write locks
            for (AbstractPersistentStore store : list)
                {
                store.unlockWrite();
                }
            }
        }

    // ----- inner class: AbstractPersistentStore ---------------------------

    /**
     * Factory method for PersistentStore implementations managed by this
     * PersistenceManager.
     *
     * @param sId  the identifier of the store to create
     *
     * @return a new AbstractPersistentStore with the given identifier
     */
    protected abstract PS instantiatePersistentStore(String sId);

    /**
     * Factory method to create a {@link PersistenceTools} implementation.
     *
     * @param info  the {@link OfflinePersistenceInfo} relevant to the PersistenceTools
     *
     * @return a new PersistenceTools implementation
     */
    protected abstract PersistenceTools instantiatePersistenceTools(OfflinePersistenceInfo info);

    /**
     * Abstract implementation of a ReadBuffer-based PersistentStore.
     *
     * @author jh  2012.10.04
     */
    public abstract class AbstractPersistentStore
            extends Base
            implements PersistentStore<ReadBuffer>
        {

        // ----- constructors -----------------------------------------------

        /**
         * Create a new AbstractPersistentStore with the given identifier.
         *
         * @param sId  the identifier for the new store
         *
         * @throws IllegalArgumentException if the identifier is invalid
         */
        public AbstractPersistentStore(String sId)
            {
            if (sId == null)
                {
                throw new IllegalArgumentException("null identifier");
                }

            f_sId      = sId;
            f_dirStore = new File(f_dirActive, sId);
            f_fileLock = new File(getLockDirectory(), sId + ".lck");
            }

        // ----- PersistentStore interface ----------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public String getId()
            {
            return f_sId;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean ensureExtent(long lExtentId)
            {
            if (!f_setExtentIds.contains(lExtentId))
                {
                ensureReady();

                // flush any pending deleteExtent tasks
                while (f_setDeletedIds.contains(lExtentId))
                    {
                    synchronized (f_setDeletedIds)
                        {
                        try
                            {
                            f_setDeletedIds.wait(100L);
                            }
                        catch (InterruptedException e)
                            {
                            // regardless of the interrupt attempt to create
                            // the extent; the creation will throw if the extent
                            // deletion remains pending
                            break;
                            }
                        }
                    }

                lockWrite();
                try
                    {
                    return ensureExtentInternal(lExtentId);
                    }
                finally
                    {
                    unlockWrite();
                    }
                }

            return false;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void deleteExtent(long lExtentId)
            {
            Long LId = Long.valueOf(lExtentId);
            if (f_setExtentIds.contains(LId))
                {
                ensureReady();
                lockWrite();
                try
                    {
                    // remove the identifier from the set of known extents
                    if (f_setExtentIds.remove(LId))
                        {
                        // add the identifier to the set of deleted
                        if (f_setDeletedIds.add(LId))
                            {
                            // schedule a deletion of this extent
                            submitTask(new DeleteExtentTask(LId));
                            }
                        }
                    }
                finally
                    {
                    unlockWrite();
                    }
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void moveExtent(long lOldExtentId, long lNewExtentId)
            {
            Long    LId   = Long.valueOf(lOldExtentId);
            boolean fLock = f_setExtentIds.contains(LId);
            try
                {
                ensureReady();
                if (fLock)
                    {
                    lockWrite();

                    moveExtentInternal(lOldExtentId, lNewExtentId);

                    f_setExtentIds.remove(LId);
                    }
                }
            finally
                {
                ensureExtent(lNewExtentId);
                if (fLock)
                    {
                    unlockWrite();
                    }
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void truncateExtent(long lExtentId)
            {
            Long LId = Long.valueOf(lExtentId);
            if (f_setExtentIds.contains(LId))
                {
                ensureReady();
                lockWrite();
                try
                    {
                    truncateExtentInternal(lExtentId);
                    }
                finally
                    {
                    unlockWrite();
                    }
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long[] extents()
            {
            ensureReady();

            if (f_setExtentIds.isEmpty())
                {
                return NO_LONGS;
                }

            Object[] aL = f_setExtentIds.toArray();
            int      cL = aL.length;
            long[]   al = new long[cL];
            for (int i = 0; i < cL; ++i)
                {
                al[i] = ((Long) aL[i]).longValue();
                }

            return al;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ReadBuffer load(long lExtentId, ReadBuffer bufKey)
            {
            if (bufKey == null)
                {
                throw new IllegalArgumentException("null key");
                }

            ensureReady();
            lockRead();
            try
                {
                validateExtentId(lExtentId);
                return loadInternal(lExtentId, bufKey);
                }
            finally
                {
                unlockRead();
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void store(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue, Object oToken)
            {
            if (bufKey == null)
                {
                throw new IllegalArgumentException("null key");
                }
            if (bufValue == null)
                {
                throw new IllegalArgumentException("null value");
                }

            ensureReady();
            lockRead();
            try
                {
                validateExtentId(lExtentId);

                if (oToken instanceof AbstractPersistenceManager.AbstractPersistentStore.BatchTask)
                    {
                    ((BatchTask) oToken).store(lExtentId, bufKey, bufValue);
                    }
                else
                    {
                    // perform the store either by adding the operation to
                    // the supplied batch or to a newly created batch if one
                    // hasn't been supplied
                    boolean fAbort  = oToken == null;
                    boolean fCommit = fAbort;
                    if (fCommit)
                        {
                        oToken = begin();
                        }

                    try
                        {
                        storeInternal(lExtentId, bufKey, bufValue, oToken);

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
                }
            finally
                {
                unlockRead();
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void erase(long lExtentId, ReadBuffer bufKey, Object oToken)
            {
            if (bufKey == null)
                {
                throw new IllegalArgumentException("null key");
                }

            ensureReady();
            lockRead();
            try
                {
                validateExtentId(lExtentId);

                if (oToken instanceof AbstractPersistenceManager.AbstractPersistentStore.BatchTask)
                    {
                    ((BatchTask) oToken).erase(lExtentId, bufKey);
                    }
                else
                    {
                    // perform the erase either by adding the operation to
                    // the supplied batch or to a newly created batch if one
                    // hasn't been supplied
                    boolean fAbort  = oToken == null;
                    boolean fCommit = fAbort;
                    if (fCommit)
                        {
                        oToken = begin();
                        }

                    try
                        {
                        eraseInternal(lExtentId, bufKey, oToken);

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
                }
            finally
                {
                unlockRead();
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void iterate(Visitor<ReadBuffer> visitor)
            {
            ensureReady();
            lockRead();
            try
                {
                iterateInternal(visitor);
                }
            finally
                {
                unlockRead();
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object begin()
            {
            ensureReady();
            lockRead();
            try
                {
                return beginInternal();
                }
            finally
                {
                unlockRead();
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object begin(Collector<Object> collector, Object oReceipt)
            {
            return new BatchTask(begin(), collector, oReceipt);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void commit(Object oToken)
            {
            if (oToken instanceof AbstractPersistenceManager.AbstractPersistentStore.BatchTask)
                {
                AbstractPersistenceManager.this.submitTask((BatchTask) oToken);
                }
            else
                {
                ensureReady();

                lockRead();
                try
                    {
                    commitInternal(oToken);
                    }
                finally
                    {
                    unlockRead();
                    }
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void abort(Object oToken)
            {
            if (oToken instanceof AbstractPersistenceManager.AbstractPersistentStore.BatchTask)
                {
                ((BatchTask) oToken).abort(null /*eCause*/);
                }
            else
                {
                try
                    {
                    ensureReady();

                    lockRead();
                    try
                        {
                        abortInternal(oToken);
                        }
                    finally
                        {
                        unlockRead();
                        }
                    }
                catch (Throwable e)
                    {
                    // guard against any unexpected throwable
                    CacheFactory.log("Caught an exception while aborting transaction for token \""
                            + oToken + "\": " + printStackTrace(e), CacheFactory.LOG_QUIET);
                    }
                }
            }

        // ----- lifecycle methods ------------------------------------------

        /**
         * Open this store either asynchronously, iff both a store to open
         * from and a collector have been provided, or synchronously.
         *
         * @param storeFrom  a {@link PersistentStore} to copy from
         * @param collector  a {@link Collector collector} to notify when the
         *                   open completes
         */
        protected void submitOpen(PersistentStore<ReadBuffer> storeFrom, Collector<Object> collector)
            {
            setState(STORE_STATE_OPENING);

            Task task = new OpenTask(storeFrom, collector);
            if (collector == null || storeFrom == null)
                {
                AbstractPersistenceManager.this.executeTask(task);
                }
            else
                {
                AbstractPersistenceManager.this.submitTask(task);
                }
            }

        /**
         * Open this persistent store.
         *
         * @param storeFrom  the PersistenceStore the new store should be based upon
         *
         * @return true if the store was created
         */
        protected boolean open(PersistentStore<ReadBuffer> storeFrom)
            {
            boolean fClosed = true;
            boolean fNew    = false;

            lockWrite();
            try
                {
                // create the data directory
                if (!f_dirStore.exists())
                    {
                    if (!f_dirStore.mkdir() && !f_dirStore.exists())
                        {
                        throw ensurePersistenceException(new FatalAccessException(
                            "unable to create data directory \"" + f_dirStore + '"'));
                        }
                    fNew = true;
                    }

                // lock the data directory
                if (!lockStorage())
                    {
                    throw ensurePersistenceException(new ConcurrentAccessException(
                        "unable to lock data directory \"" + f_dirStore + '"'));
                    }

                // read and assert metadata
                if (!fNew)
                    {
                    validateMetadata();
                    }

                // copy data from the old store (if provided)

                // Note: the copy is performed prior to writing metadata thus
                //       the store is only considered valid after a successful copy

                copyAndOpenInternal(storeFrom);

                loadExtentIdsInternal(f_setExtentIds);

                // write metadata
                try
                    {
                    AbstractPersistenceManager.this.writeMetadata(f_dirStore);
                    }
                catch (IOException e)
                    {
                    throw ensurePersistenceException(new FatalAccessException(
                       "error writing metadata in directory \"" + f_dirStore + '"', e));
                    }

                // the persistent store is now opened
                fClosed = false;
                setState(STORE_STATE_READY);
                }
            finally
                {
                if (fClosed)
                    {
                    // cleanup
                    try
                        {
                        releaseInternal();
                        }
                    catch (Throwable e)
                        {
                        // ignore
                        }
                    setState(STORE_STATE_CLOSED);
                    }
                unlockStorage();
                unlockWrite();
                }
            return fNew;
            }

        /**
         * Release any resources held by this persistent store.
         */
        protected void release()
            {
            lockWrite();
            try
                {
                // cleanup
                try
                    {
                    releaseInternal();
                    }
                catch (Throwable e)
                    {
                    // ignore
                    }
                setState(STORE_STATE_CLOSED);
                f_setExtentIds.clear();
                f_setDeletedIds.clear();
                }
            finally
                {
                unlockWrite();
                }
            }

        /**
         * Release any resources held by this persistent store and delete any
         * underlying persistent storage.
         *
         * @param fSafe  if true, remove the store by moving it to a restorable
         *               location (if possible) rather than deleting it
         *
         * @return true if the store was successfully deleted, false otherwise
         */
        protected boolean delete(boolean fSafe)
            {
            boolean fDeleted = false;

            lockWrite();
            try
                {
                release();
                if (lockStorage())
                    {
                    try
                        {
                        File fileTrash = AbstractPersistenceManager.this.f_dirTrash;
                        if (fSafe && fileTrash != null)
                            {
                            // create the trash directory
                            if (!fileTrash.exists())
                                {
                                CacheFactory.log("Creating persistence trash directory \""
                                        + fileTrash.getAbsolutePath() + '"', CacheFactory.LOG_INFO);
                                fileTrash = FileHelper.ensureDir(fileTrash);
                                }

                            // move the data directory to the trash iff the meta.properties
                            // file exists - a sign of birth
                            File fileMeta = new File(f_dirStore, CachePersistenceHelper.META_FILENAME);
                            if (fileMeta.exists())
                                {
                                FileHelper.moveDir(f_dirStore, new File(fileTrash, f_sId));
                                }
                            }
                        deleteInternal();
                        fDeleted = true;
                        }
                    catch (Throwable e)
                        {
                        // fall through
                        }
                    finally
                        {
                        unlockStorage();

                        // delete the lock file followed by the store
                        fDeleted = fDeleted && f_fileLock.delete();
                        try
                            {
                            FileHelper.deleteDir(f_dirStore);
                            }
                        catch (IOException ignore) {}
                        }
                    }
                }
            finally
                {
                unlockWrite();
                }

            return fDeleted;
            }

        /**
         * Block the calling thread until the store is either ready to accept
         * requests or the store has been closed.
         *
         * @throws PersistenceException if the store has been closed or the
         *         thread was interrupted
         */
        protected void ensureReady()
            {
            while (!isReady())
                {
                if (isClosed())
                    {
                    throw ensurePersistenceException(null, "Store (" + toString() + ") has been closed");
                    }
                synchronized (this)
                    {
                    try
                        {
                        Blocking.wait(this, 10L);
                        }
                    catch (InterruptedException e)
                        {
                        Thread.currentThread().interrupt();
                        throw ensurePersistenceException(e, "Interrupted while waiting for store to be opened");
                        }
                    }
                }
            }

        /**
         * Return true if the store is ready to accept requests.
         *
         * @return true if the store is ready to accept requests
         */
        protected boolean isReady()
            {
            return (m_nState & STORE_STATE_READY) != 0;
            }

        /**
         * Return true if the store has been closed.
         *
         * @return true if the store has been closed
         */
        protected boolean isClosed()
            {
            return (m_nState & STORE_STATE_CLOSED) != 0;
            }

        /**
         * Set the state of this store.
         *
         * @param nState  the state the store should be transitioned to
         */
        protected void setState(int nState)
            {
            if (nState != m_nState)
                {
                synchronized (this)
                    {
                    m_nState = nState;
                    notifyAll();
                    }
                }
            }

        // ----- Object methods ---------------------------------------------

        /**
         * Return a human readable description of this AbstractPersistentStore.
         *
         * @return a human readable description
         */
        @Override
        public String toString()
            {
            return ClassHelper.getSimpleName(getClass()) + '(' + f_sId + ", "
                    + f_dirStore + ")";
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return a PersistenceException with the given cause. The returned
         * exception is also initialized with this store, its manager, and
         * its environment (if available).
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
         * Return a PersistenceException with the given cause and detail
         * message. The returned exception is also initialized with this
         * store, its manager, and its environment (if available).
         *
         * @param eCause    an optional cause
         * @param sMessage  an optional detail message
         *
         * @return a PersistenceException with the given cause and detail message
         */
        protected PersistenceException ensurePersistenceException(Throwable eCause, String sMessage)
            {
            PersistenceException e = AbstractPersistenceManager.this.ensurePersistenceException(eCause, sMessage);
            e.initPersistentStore(this);
            return e;
            }

        /**
         * Acquire an exclusive lock on the data directory underlying this
         * persistent store.
         *
         * @return true if an exclusive lock was obtained, false otherwise
         */
        protected final boolean lockStorage()
            {
            FileLock lock = m_lockFile;
            if (lock == null)
                {
                m_lockFile = lock = FileHelper.lockFile(f_fileLock);
                }
            return lock != null;
            }

        /**
         * Release an exclusive lock on the data directory underlying this
         * persistent store.
         */
        protected final void unlockStorage()
            {
            FileLock lock = m_lockFile;
            if (lock != null)
                {
                FileHelper.unlockFile(lock);
                m_lockFile = null;
                }
            }

        /**
         * Acquire a read lock on this persistent store.
         */
        protected final void lockRead()
            {
            f_lock.readLock().lock();
            }

        /**
         * Release a read lock on this persistent store.
         */
        protected final void unlockRead()
            {
            f_lock.readLock().unlock();
            }

        /**
         * Acquire a write lock on this persistent store.
         */
        protected final void lockWrite()
            {
            f_lock.writeLock().lock();
            }

        /**
         * Release a write lock on this persistent store.
         */
        protected final void unlockWrite()
            {
            f_lock.writeLock().unlock();
            }

        /**
         * Validate the given extent identifier.
         *
         * @param lExtentId  the extent identifier
         */
        protected void validateExtentId(long lExtentId)
            {
            Long LId = Long.valueOf(lExtentId);

            // validate that the given extent identifier is known
            if (!f_setExtentIds.contains(LId))
                {
                throw new IllegalArgumentException("unknown extent identifier: " + lExtentId);
                }
            }

        /**
         * Validate the metadata
         */
        protected void validateMetadata()
            {
            try
                {
                Properties prop = AbstractPersistenceManager.this.readMetadata(f_dirStore);
                if (!AbstractPersistenceManager.this.isMetadataComplete(prop))
                    {
                    throw ensurePersistenceException(new FatalAccessException(
                            "the data in directory \"" + f_dirStore + "\" appears to be incomplete"));
                    }
                if (!AbstractPersistenceManager.this.isMetadataCompatible(prop))
                    {
                    throw ensurePersistenceException(new FatalAccessException(
                            "the data in directory \"" + f_dirStore + "\" is incompatible with this manager"));
                    }

                }
            catch (IOException e)
                {
                throw ensurePersistenceException(new FatalAccessException(
                        "error reading metadata in directory \"" + f_dirStore + '"', e));
                }
            }

        /**
         * Ensure the provided extent id has been registered and created, thus
         * allowing subsequent load and store operations against the same extent
         * id.
         * <p>
         * Note: the caller is assumed to have exclusive access to this store.
         *
         * @param lExtentId  the extent id to register and create
         *
         * @return true if the extent id was not previously registered and was
         *         successfully created
         */
        protected boolean ensureExtentInternal(long lExtentId)
            {
            Long LId = Long.valueOf(lExtentId);
            if (!f_setExtentIds.contains(LId))
                {
                // make sure the extent isn't in the process of being
                // deleted
                if (f_setDeletedIds.contains(LId))
                    {
                    throw new IllegalArgumentException("deleted extent identifier: " + lExtentId);
                    }

                // add the identifier to the set of known extents
                if (f_setExtentIds.add(LId))
                    {
                    // create the extent
                    createExtentInternal(lExtentId);
                    }

                return true;
                }
            return false;
            }

        /**
         * Copy the provided store to ensure both the contents are available
         * in the new store and it is open thus ready to receive requests.
         * <p>
         * Note: overriders of this method must guarantee {@link #openInternal()}
         *       is called by either delegating to super or calling it directly.
         *
         * @param storeFrom  the store to copy from
         */
        protected void copyAndOpenInternal(PersistentStore<ReadBuffer> storeFrom)
            {
            openInternal();

            if (storeFrom != null)
                {
                ((AbstractPersistentStore) storeFrom).validateMetadata();

                final Object oToken = beginInternal();
                try
                    {
                    for (long lExtentId : storeFrom.extents())
                        {
                        ensureExtentInternal(lExtentId);
                        }

                    storeFrom.iterate((lExtentId, bufKey, bufValue) ->
                        {
                        storeInternal(lExtentId, bufKey, bufValue, oToken);
                        return true;
                        });
                    }
                catch (PersistenceException e)
                    {
                    abortInternal(oToken);
                    delete(false);

                    throw e;
                    }
                commitInternal(oToken);
                }
            }

        /**
         * Open the underlying persistent storage.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a write lock on this persistent store.
         *
         * @throws PersistenceException if a general persistence error occurs
         */
        protected abstract void openInternal();

        /**
         * Release the underlying persistent storage.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a write lock on this persistent store.
         *
         * @throws PersistenceException if a general persistence error occurs
         */
        protected abstract void releaseInternal();

        /**
         * Remove the underlying persistent storage.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a write lock on this persistent store.
         *
         * @return {@code true} on successful removal
         *
         * @throws PersistenceException if a general persistence error occurs
         */
        protected abstract boolean deleteInternal();

        /**
         * Populate the given set with the identifiers of extents in the
         * underlying persistent storage.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a write lock on this persistent store.
         *
         * @param setIds  a set of ids
         *
         * @throws PersistenceException if a general persistence error occurs
         */
        protected abstract void loadExtentIdsInternal(Set<Long> setIds);

        /**
         * Create the extent with the given identifier in the persistent
         * store.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a write lock on this persistent store.
         *
         * @param lExtentId  the identifier of the extent to create
         *
         * @throws PersistenceException if a general persistence error occurs
         */
        protected abstract void createExtentInternal(long lExtentId);

        /**
         * Delete the specified extent from the persistent store.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a write lock on this persistent store.
         *
         * @param lExtentId  the identifier of the extent to delete
         *
         * @throws PersistenceException if a general persistence error occurs
         */
        protected abstract void deleteExtentInternal(long lExtentId);

        /**
         * Move the specified extent from the old extent id to the new extent id.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a write lock on this persistent store.
         *
         * @param lOldExtentId  the old extent identifier
         * @param lNewExtentId  the new extent identifier
         *
         * @throws PersistenceException if a general persistence error occurs
         */
        protected void moveExtentInternal(long lOldExtentId, long lNewExtentId)
            {
            final Object oToken = begin();
            try
                {
                iterate((lExtentId, bufKey, bufValue) ->
                    {
                    if (lExtentId == lOldExtentId)
                        {
                        store(lNewExtentId, bufKey, bufValue, oToken);
                        }
                    return true;
                    });
                }
            catch (PersistenceException e)
                {
                abort(oToken);
                throw e;
                }
            commit(oToken);
            }

        /**
         * Truncate the specified extent from the persistent store.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a write lock on this persistent store.
         *
         * @param lExtentId  the identifier of the extent to truncate
         *
         * @throws PersistenceException if a general persistence error occurs
         */
        protected abstract void truncateExtentInternal(long lExtentId);

        /**
         * Load and return the value associated with the specified key from
         * the underlying persistent storage.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a read lock on this persistent store.
         *
         * @param lExtentId  the extent identifier for the key
         * @param bufKey     key whose associated value is to be returned
         *
         * @return the value associated with the specified key, or <tt>null</tt>
         *         if no value is available for that key
         *
         * @throws PersistenceException if a general persistence error occurs
         */
        protected abstract ReadBuffer loadInternal(long lExtentId, ReadBuffer bufKey);

        /**
         * Store the specified value under the specific key in the underlying
         * persistent storage.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a read lock on this persistent store.
         *
         * @param lExtentId  the extent identifier for the key
         * @param bufKey     key to store the value under
         * @param bufValue   value to be stored
         * @param oToken     a token that represents an atomic unit to commit
         *
         * @throws PersistenceException if a general persistence error occurs
         *
         * @throws IllegalArgumentException if the token is invalid
         */
        protected abstract void storeInternal(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue, Object oToken);

        /**
         * Remove the specified key from the underlying persistent storage
         * if present.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a read lock on this persistent store.
         *
         * @param lExtentId  the extent identifier for the key
         * @param bufKey     key whose mapping is to be removed from the map
         * @param oToken     a token that represents an atomic unit to commit
         *
         * @throws PersistenceException if a general persistence error occurs
         *
         * @throws IllegalArgumentException if the token is invalid
         */
        protected abstract void eraseInternal(long lExtentId, ReadBuffer bufKey, Object oToken);

        /**
         * Iterate the key-value pairs in the underlying persistent storage,
         * applying the specified visitor to each key-value pair.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a read lock on this persistent store.
         *
         * @param visitor  the visitor to apply
         *
         * @throws PersistenceException if a general persistence error occurs
         */
        protected abstract void iterateInternal(Visitor<ReadBuffer> visitor);

        /**
         * Begin a sequence of mutating operations that should be committed
         * atomically and return a token that represents the atomic unit.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a read lock on this persistent store.
         *
         * @return  a token that represents the atomic unit to commit
         *
         * @throws PersistenceException if a general persistence error occurs
         */
        protected abstract Object beginInternal();

        /**
         * Commit a sequence of mutating operations represented by the given
         * token as an atomic unit.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a read lock on this persistent store.
         *
         * @param oToken  a token that represents the atomic unit to commit
         *
         * @throws PersistenceException if a general persistence error occurs
         *
         * @throws IllegalArgumentException if the token is invalid
         */
        protected abstract void commitInternal(Object oToken);

        /**
         * Abort an atomic sequence of mutating operations.
         * <p>
         * Note: this method is guaranteed to only be called by a thread that
         * holds a read lock on this persistent store.
         *
         * @param oToken  a token that represents the atomic unit to abort
         *
         * @throws PersistenceException if a general persistence error occurs
         *
         * @throws IllegalArgumentException if the token is invalid
         */
        protected abstract void abortInternal(Object oToken);

        // ----- inner class: OpenTask --------------------------------------

        /**
         * An OpenTask opens the store (parent of this inner class) with the
         * provided store and notifies the Collector when complete.
         */
        protected class OpenTask
                extends Task
                implements KeyAssociation
            {
            // ----- constructors -------------------------------------------

            /**
             * Construct an OpenTask.
             *
             * @param storeFrom  store to open from
             * @param collector  collector to notify when the open completes
             */
            public OpenTask(PersistentStore<ReadBuffer> storeFrom, Collector<Object> collector)
                {
                f_storeFrom = storeFrom;
                f_collector = collector;
                }

            // ----- KeyAssociation methods ---------------------------------

            @Override
            public Object getAssociatedKey()
                {
                // the intent is to run under the same association as the store
                // we are copying from; this allows the open implementation to
                // assume there is no concurrent access to the store - see copyAndOpenInternal
                return f_storeFrom == null
                        ? AbstractPersistentStore.this.getId() : f_storeFrom.getId();
                }

            // ----- Task methods -------------------------------------------

            @Override
            public void execute()
                {
                AbstractPersistentStore store    = AbstractPersistentStore.this;
                PersistenceException    eFailure = null;
                try
                    {
                    boolean fNewStore = store.open(f_storeFrom);
                    if (fNewStore)
                        {
                        CacheFactory.log("Created persistent store " + FileHelper.getPath(store.f_dirStore)
                                + (f_storeFrom == null ? "" : " from " + f_storeFrom), CacheFactory.LOG_INFO);
                        }
                    }
                catch (PersistenceException e)
                    {
                    if (f_collector == null)
                        {
                        close(store.getId());
                        throw e;
                        }
                    eFailure = store.ensurePersistenceException(new AsyncPersistenceException("Error in opening store", e)
                        .initReceipt(store.getId()));
                    }
                finally
                    {
                    if (f_collector != null)
                        {
                        if (eFailure == null)
                            {
                            f_collector.add(store.getId());
                            }
                        else
                            {
                            close(store.getId());
                            f_collector.add(eFailure);
                            }

                        }
                    }
                }

            // ----- data members -------------------------------------------

            /**
             * The {@link PersistentStore} to open with. The contents of this
             * store are copied to the store being opened.
             */
            protected final PersistentStore<ReadBuffer> f_storeFrom;

            /**
             * The {@link Collector} to notify upon completion of opening the
             * store.
             */
            protected final Collector<Object>           f_collector;
            }

        // ----- inner class: DeleteExtentTask ------------------------------

        /**
         * A Task implementation that deletes an extent from the associated
         * store.
         */
        protected class DeleteExtentTask
                extends Task
                implements KeyAssociation
            {
            // ----- constructors -------------------------------------------

            /**
             * Construct a DeleteExtentTask with the provided extent id.
             *
             * @param LExtentId  the extent to delete
             */
            public DeleteExtentTask(Long LExtentId)
                {
                f_LExtentId = LExtentId;
                }

            // ----- KeyAssociation methods ---------------------------------

            @Override
            public Object getAssociatedKey()
                {
                return AbstractPersistentStore.this.getId();
                }

            // ----- Task methods -------------------------------------------

            @Override
            public void execute()
                {
                AbstractPersistentStore store = AbstractPersistentStore.this;
                store.lockWrite();
                try
                    {
                    if (store.f_setDeletedIds.remove(f_LExtentId))
                        {
                        store.deleteExtentInternal(f_LExtentId.longValue());

                        synchronized (store.f_setDeletedIds)
                            {
                            store.f_setDeletedIds.notifyAll();
                            }
                        }
                    }
                finally
                    {
                    store.unlockWrite();
                    }
                }

            // ----- data members -------------------------------------------

            /**
             * The extent to delete.
             */
            protected final Long f_LExtentId;
            }

        // ----- inner class: BatchTask -------------------------------------

        /**
         * Runnable implementation that is used to perform and commit a
         * sequence of mutating persistent store operations asynchronously.
         */
        protected class BatchTask
                extends Task
                implements KeyAssociation
            {

            // ----- constructors -------------------------------------------

            /**
             * Create a new BatchTask.
             *
             * @param oToken     a token that represents the atomic unit to commit
             * @param collector  an optional Collector to notify
             * @param oReceipt   the receipt to add to the Collector after the
             *                   unit is committed
             */
            public BatchTask(Object oToken, Collector<Object> collector, Object oReceipt)
                {
                f_oToken    = oToken;
                f_collector = collector;
                f_oReceipt  = oReceipt;
                }

            // ----- BatchTask methods --------------------------------------

            /**
             * Queue a store operation.
             *
             * @param lExtentId  the extent identifier for the key
             * @param bufKey     key to store the value under
             * @param bufValue   value to be stored
             */
            public void store(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue)
                {
                f_listOps.add(new StoreOperation(lExtentId, bufKey, bufValue));
                }

            /**
             * Queue an erase operation.
             *
             * @param lExtentId  the extent identifier for the key
             * @param bufKey     key whose mapping is to be removed
             */
            public void erase(long lExtentId, ReadBuffer bufKey)
                {
                f_listOps.add(new EraseOperation(lExtentId, bufKey));
                }

            /**
             * Abort all changes that have been made to the persistent store
             * by this BatchTask.
             *
             * @param eCause  optional cause for the abort
             */
            public void abort(Throwable eCause)
                {
                Object oReceipt = f_oReceipt;
                try
                    {
                    AbstractPersistentStore.this.abort(f_oToken);

                    // notify the collector with an AsyncPersistenceException
                    AsyncPersistenceException eAsync = new AsyncPersistenceException(
                            "\"transaction aborted: \"" + f_oToken, eCause)
                            .initReceipt(f_oReceipt);
                    oReceipt = ensurePersistenceException(eAsync);
                    }
                finally
                    {
                    notifyCollector(oReceipt, true);
                    }
                }

            // ----- Task interface -----------------------------------------

            /**
             * Execute all queued operations and commit changes.
             */
            public void execute()
                {
                AbstractPersistentStore store = AbstractPersistentStore.this;
                try
                    {
                    // BatchTask execution will exclusively run either store or erase
                    // operations (both require a read lock), therefore a read lock
                    // is acquired upfront and not released until all ops have completed;
                    // this will prevent other threads from acquiring a read or
                    // write lock (partition transfer) in-between ops
                    store.lockRead();
                    try
                        {
                        // execute queued operations
                        for (Operation op : f_listOps)
                            {
                            op.run();
                            }

                        // commit the changes to the persistent store
                        AbstractPersistentStore.this.commit(f_oToken);
                        }
                    finally
                        {
                        store.unlockRead();
                        }

                    // Note: notify the Collector without holding any locks
                    notifyCollector(f_oReceipt, true);
                    }
                catch (Throwable e)
                    {
                    // abort changes
                    abort(e);
                    }
                }

            /**
             * {@inheritDoc}
             */
            @Override
            public void notifyCanceled(Throwable eCause)
                {
                abort(eCause);
                }

            // ----- KeyAssociation interface -------------------------------

            /**
             * {@inheritDoc}
             */
            @Override
            public Object getAssociatedKey()
                {
                return AbstractPersistentStore.this.getId();
                }

            // ----- inner class: Operation ---------------------------------

            /**
             * Base class for Runnable implementations that encapsulate a
             * persistent store operation.
             */
            protected abstract class Operation
                    extends Base
                    implements Runnable
                {
                // ----- constructors ---------------------------------------

                /**
                 * Create a new Operation.
                 *
                 * @param lExtentId  extent identifier for the target key
                 * @param bufKey     target key of the operation
                 */
                public Operation(long lExtentId, ReadBuffer bufKey)
                    {
                    f_lExtentId = lExtentId;
                    f_bufKey    = bufKey;
                    }

                // ----- data members ---------------------------------------

                /**
                 * The extent identifier for the target key.
                 */
                protected final long f_lExtentId;

                /**
                 * The target key of the operation.
                 */
                protected final ReadBuffer f_bufKey;
                }

            // ----- inner class: EraseOperation ----------------------------

            /**
             * An <tt>erase()</tt> Operation.
             */
            protected class EraseOperation
                    extends Operation
                {
                // ----- constructors ---------------------------------------

                /**
                 * Create a new EraseOperation.
                 *
                 * @param lExtentId  extent identifier for the target key
                 * @param bufKey     key to erase
                 */
                public EraseOperation(long lExtentId, ReadBuffer bufKey)
                    {
                    super(lExtentId, bufKey);
                    }

                // ----- Runnable interface ---------------------------------

                /**
                 * Perform the erase operation.
                 */
                public void run()
                    {
                    AbstractPersistentStore.this.erase(f_lExtentId, f_bufKey,
                            BatchTask.this.f_oToken);
                    }
                }

            // ----- inner class: StoreOperation ----------------------------

            /**
             * A <tt>store()</tt> Operation.
             */
            protected class StoreOperation
                    extends Operation
                {
                // ----- constructors ---------------------------------------

                /**
                 * Create a new StoreOperation.
                 *
                 * @param lExtentId  extent identifier for the target key
                 * @param bufKey     target key
                 * @param bufValue   value to store
                 */
                public StoreOperation(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue)
                    {
                    super(lExtentId, bufKey);
                    f_bufValue = bufValue;
                    }

                // ----- Runnable interface ---------------------------------

                /**
                 * Perform the erase operation.
                 */
                public void run()
                    {
                    AbstractPersistentStore.this.store(f_lExtentId, f_bufKey,
                            f_bufValue, BatchTask.this.f_oToken);
                    }

                // ----- data members ---------------------------------------

                /**
                 * The value to store.
                 */
                protected final ReadBuffer f_bufValue;
                }

            // ----- helper methods -----------------------------------------

            /**
             * Add the given object to the configured collector (if any). If
             * the add operation throws an exception, it will be caught and
             * logged.
             *
             * @param oItem   the item to add
             * @param fFlush  if true, the collector will be flushed after
             *                adding the item
             */
            protected void notifyCollector(Object oItem, boolean fFlush)
                {
                if (f_collector != null)
                    {
                    try
                        {
                        f_collector.add(oItem);
                        if (fFlush)
                            {
                            f_collector.flush();
                            }
                        }
                    catch (Throwable e)
                        {
                        CacheFactory.log("Error adding an item to collector \""
                                + f_collector + "\": " + printStackTrace(e),
                                CacheFactory.LOG_ERR);
                        }
                    }
                }

            // ----- data members -------------------------------------------

            /**
             * A token representing the atomic unit that will be committed
             * asynchronously.
             */
            protected final Object f_oToken;

            /**
             * An optional Collector to add notifications to.
             */
            protected final Collector<Object> f_collector;

            /**
             * The receipt to add to the Collector after the unit is committed.
             */
            protected final Object f_oReceipt;

            /**
             * The sequence of operations to commit atomically.
             */
            protected final List<Operation> f_listOps = new ArrayList<>();
            }

        // ----- accessors --------------------------------------------------

        /**
         * The directory used to store persisted data.
         *
         * @return the underlying data storage directory
         */
        public File getDataDirectory()
            {
            return f_dirStore;
            }

        // ----- data members -----------------------------------------------

        /**
         * The identifier of this persistent store.
         */
        protected final String f_sId;

        /**
         * The directory used to store persisted data.
         */
        protected final File f_dirStore;

        /**
         * The file used to prevent concurrent access to the data directory
         * underlying this persistent store.
         */
        protected final File f_fileLock;

        /**
         * The state of the PersistenceStore.
         */
        protected volatile int m_nState;

        /**
         * The FileLock used to prevent concurrent access to the data
         * directory underlying this persistent store.
         */
        protected FileLock m_lockFile;

        /**
         * The ReadWriteLock used to protect against concurrent read/write
         * operations.
         */
        protected final ReadWriteLock f_lock = new ReentrantReadWriteLock();

        /**
         * The set of valid extent identifiers known to this persistent store.
         */
        protected final Set<Long> f_setExtentIds = new CopyOnWriteArraySet<>();

        /**
         * The set of extent identifiers that are in the process of being
         * deleted.
         */
        protected final Set<Long> f_setDeletedIds = new CopyOnWriteArraySet<>();
        }

    // ----- inner class: AbstractPersistenceSnapshotTools ------------------

    /**
     * Abstract implementation of PersistenceTools which can be extended to
     * support local snapshot operations for specific implementations.
     *
     * @author tam/hr  2014.11.21
     * @since 12.2.1
     */
    protected abstract class AbstractPersistenceSnapshotTools
                extends AbstractPersistenceTools
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an abstract implementation for a given snapshot directory.
         *
         * @param dirSnapshot  the directory where the snapshot is
         * @param info         the information collected regarding the snapshot
         */
        public AbstractPersistenceSnapshotTools(File dirSnapshot, OfflinePersistenceInfo info)
            {
            super(info);
            f_dirSnapshot = dirSnapshot;
            }

        // ----- PersistenceTools methods -----------------------------------

        /**
         * Get the {@link PersistenceStatistics} for a local snapshot by using the
         * implementation manager and visiting the store.
         *
         * @return the PersistenceStatistics for a local snapshot
         */
        @Override
        public PersistenceStatistics getStatistics()
            {
            String[]                    asFileList   = f_info.getGUIDs();
            String                      sCurrentGUID = null;
            PersistenceStatistics       stats        = new PersistenceStatistics();
            StatsVisitor                visitor      = new StatsVisitor(stats);
            PersistentStore<ReadBuffer> store;

            for (int i = 0; i < asFileList.length; i++)
                {
                try
                    {
                    sCurrentGUID = asFileList[i];
                    store        = AbstractPersistenceManager.this.open(sCurrentGUID, null);

                    validateStoreSealed(store);

                    visitor.setCaches(CachePersistenceHelper.getCacheNames(store));
                    store.iterate(CachePersistenceHelper.instantiatePersistenceVisitor(visitor));
                    }
                finally
                    {
                    AbstractPersistenceManager.this.close(sCurrentGUID);
                    }
                }

            return stats;
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Validate the given store within the GUID is sealed. Note: The store is
         * opened and closed during this method call.
         *
         * @param sCurrentGUID  the GUID to open store from
         *
         * @throws PersistenceException if the store is not sealed
         */
        protected void validateStoreSealed(String sCurrentGUID)
            {
            PersistentStore<ReadBuffer> store;
            try
                {
                store = AbstractPersistenceManager.this.open(sCurrentGUID, null);
                validateStoreSealed(store);
                }
            finally
                {
                AbstractPersistenceManager.this.close(sCurrentGUID);
                }
            }

        /**
         * Validate the given store is sealed.
         *
         * @param store  the persistent store to validate
         *
         * @throws PersistenceException if the store is not sealed
         */
        protected void validateStoreSealed(PersistentStore<ReadBuffer> store)
            {
            if (!CachePersistenceHelper.isSealed(store))
                {
                throw CachePersistenceHelper.ensurePersistenceException(
                        new IllegalStateException("Store " + store.getId() +
                                " was not sealed correctly"));
                }
            }

        // ----- data members --------------------------------------------

        /**
         * The snapshot directory.
         */
        protected final File f_dirSnapshot;
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

    /**
     * Return the directory used to store persisted data.
     *
     * @return the data storage directory
     */
    public File getDataDirectory()
        {
        return f_dirActive;
        }

    /**
     * Return the directory used to store "safe-deleted" persisted data.
     *
     * @return the trash storage directory
     */
    public File getTrashDirectory()
        {
        return f_dirTrash;
        }

    /**
     * Return the directory used to store lock files.
     *
     * @return the directory used to store lock files
     */
    protected File getLockDirectory()
        {
        try
            {
            return FileHelper.ensureDir(f_dirLock);
            }
        catch (IOException ignore) {}
        return f_dirLock;
        }

    /**
     * Return the environment that created this manager.
     *
     * @return the environment that created this manager
     */
    protected AbstractPersistenceEnvironment getPersistenceEnvironment()
        {
        return m_env;
        }

    /**
     * Configure the environment that created this manager.
     *
     * @param env the environment that created this manager
     */
    protected void setPersistenceEnvironment(AbstractPersistenceEnvironment env)
        {
        m_env = env;
        if (env == null)
            {
            setDaemonPool(null);
            }
        else
            {
            setDaemonPool(env.getDaemonPool());
            }
        }

    /**
     * Return the map of open PersistentStore instances keyed by their identifiers.
     * <p>
     * Note: The return map is "live". Any attempt to access or mutate it
     * should be done while holding a monitor on this manager.
     *
     * @return the map of open PersistentStore instances
     */
    public Map<String, PS> getPersistentStoreMap()
        {
        return f_mapStores;
        }


    // ----- constants ------------------------------------------------------

    /**
     * An empty long array (by definition immutable).
     */
    protected static final long[] NO_LONGS = new long[0];

    /**
     * An empty String array (by definition immutable).
     */
    protected static final String[] NO_STRINGS = AbstractPersistenceEnvironment.NO_STRINGS;

    /**
     * Magic header.
     */
    private static final int MAGIC = 0x6A683735;

    /**
     * Serialization version.
     */
    private static final int VERSION = 0;

    // ----- store constants ------------------------------------------------

    /**
     * The initial state of a PersistenceStore.
     */
    protected static final int STORE_STATE_INITIALIZED = 0;

    /**
     * The state of a PersistenceStore when it is in the process of being opened.
     */
    protected static final int STORE_STATE_OPENING     = 1;

    /**
     * The state of a PersistenceStore once it has been opened and is ready
     * to process requests.
     */
    protected static final int STORE_STATE_READY       = 2;

    /**
     * The state of a PersistenceStore once it has been released and closed.
     */
    protected static final int STORE_STATE_CLOSED      = 4;

    // ----- data members ---------------------------------------------------

    /**
     * The directory used to store persisted data.
     */
    protected final File f_dirActive;

    /**
     * The directory used to store "safe-deleted" data.
     */
    protected final File f_dirTrash;

    /**
     * The directory used to store lock files (to protect against multi-process
     * file system clean up).
     */
    protected final File f_dirLock;

    /**
     * The name of this AbstractPersistenceManager.
     */
    protected final String f_sName;

    /**
     * Map of open AbstractPersistentStore instances.
     */
    protected final ConcurrentMap<String, PS> f_mapStores = new ConcurrentHashMap<>();

    /**
     * Set of outstanding tasks.
     */
    protected final Set<Task> f_setTasks = new HashSet<>();

    /**
     * Whether this PersistenceManager has been released.
     */
    protected volatile boolean m_fReleased;

    /**
     * The environment that created this AbstractPersistenceManager.
     */
    protected AbstractPersistenceEnvironment m_env;

    /**
     * An optional DaemonPool used to execute tasks.
     */
    protected DaemonPool m_pool;
    }
