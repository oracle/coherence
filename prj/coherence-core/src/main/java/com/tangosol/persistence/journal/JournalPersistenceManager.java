/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.journal;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.persistence.OfflinePersistenceInfo;
import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistenceTools;
import com.oracle.coherence.persistence.PersistentStore;
import com.oracle.coherence.persistence.PersistentStore.Visitor;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.journal2.CheckpointFile;
import com.tangosol.io.journal2.JournalCompactor;
import com.tangosol.io.journal2.JournalEntry;
import com.tangosol.io.journal2.JournalRecovery;
import com.tangosol.io.journal2.PartitionJournal;
import com.tangosol.io.journal2.PartitionJournalConfig;
import com.tangosol.io.journal2.PersistentTicket;

import com.tangosol.internal.util.Daemons;

import com.tangosol.persistence.AbstractPersistenceManager;
import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.PersistenceRecoveryMetrics;
import com.tangosol.persistence.SafePersistenceWrappers;
import com.tangosol.persistence.journal.JournalPersistenceMBeanRegistry.ManagerRole;
import com.tangosol.persistence.journal.JournalPersistenceMBeanRegistry.MBeanRegistrar;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryLongMap;
import com.tangosol.util.BinaryRadixTree;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Journal-based persistence manager.
 * <p>
 * The same manager supports both unified-persistence deployment modes. In
 * side-channel mode it is opened through the standard persistence
 * environment APIs and acts as the durable store behind an ordinary backing
 * map. In {@code <journal-scheme>} mode the manager still owns the same
 * on-disk journal format, but the corresponding {@link JournalBackingMap}
 * reads and writes that store directly so the legacy persistence side-channel
 * is bypassed.
 *
 * @author rl  2026.03.07
 * @since 26.04
 */
public class JournalPersistenceManager
        extends AbstractPersistenceManager<JournalPersistenceManager.JournalPersistentStore>
        implements JournalPersistenceMBeanRegistry.DataSource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new {@link JournalPersistenceManager}.
     *
     * @param fileData   the directory containing persistent stores
     * @param fileTrash  an optional trash directory
     * @param sName      an optional manager name
     *
     * @throws IOException if the directories cannot be created
     */
    public JournalPersistenceManager(File fileData, File fileTrash, String sName)
            throws IOException
        {
        super(fileData, fileTrash, sName);
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Configure journal file settings used by newly created stores.
     *
     * @param config  journal configuration
     *
     * @return this manager
     */
    public JournalPersistenceManager setJournalConfig(PartitionJournalConfig config)
        {
        if (config == null)
            {
            throw new IllegalArgumentException("journal config cannot be null");
            }

        m_journalConfig = config;
        return this;
        }

    /**
     * Set the optional MBean context used for journal persistence management.
     * <p>
     * This method must be called before the manager is opened. A {@code null}
     * or empty service name disables MBean wiring; this is used for auxiliary
     * managers such as events and snapshots. When a service name is supplied,
     * the role and registrar must also be supplied.
     *
     * @param sService   service name
     * @param role       active or backup manager role
     * @param registrar  MBean registrar
     *
     * @return this manager
     */
    public JournalPersistenceManager setMBeanContext(String sService, ManagerRole role, MBeanRegistrar registrar)
        {
        if (sService == null || sService.isEmpty())
            {
            m_sMBeanService = null;
            m_roleMBean     = null;
            m_registrarMBean = null;
            return this;
            }

        if (role == null)
            {
            throw new IllegalArgumentException("manager role cannot be null when service is set");
            }

        if (registrar == null)
            {
            throw new IllegalArgumentException("MBean registrar cannot be null when service is set");
            }

        m_sMBeanService  = sService;
        m_roleMBean      = role;
        m_registrarMBean = registrar;
        return this;
        }

    /**
     * Return the total number of currently open journal extents across all
     * open stores managed by this manager.
     *
     * @return the number of currently open journal extents
     */
    public int getOpenExtentCount()
        {
        int cOpen = 0;

        synchronized (this)
            {
            for (JournalPersistentStore store : getPersistentStoreMap().values())
                {
                if (store.isOpen())
                    {
                    cOpen += store.getOpenExtentCountSnapshot();
                    }
                }
            }

        return cOpen;
        }

    /**
     * Return a snapshot of the current open extent count.
     *
     * @return a snapshot of the current open extent count
     */
    @Override
    public int getOpenExtentCountSnapshot()
        {
        return getOpenExtentCount();
        }

    /**
     * Return a short, human-readable summary of current journal compaction
     * state.
     * <p>
     * The exact string is diagnostic and not a stable public contract.
     *
     * @return the current compaction progress summary
     */
    public String getCompactionProgressSummary()
        {
        CompactionStatus status = new CompactionStatus();

        synchronized (this)
            {
            for (JournalPersistentStore store : getPersistentStoreMap().values())
                {
                if (store.isOpen())
                    {
                    store.updateCompactionStatus(status);
                    }
                }
            }

        return status.format();
        }

    /**
     * Return the most recent recovery summary recorded by this manager.
     *
     * @return the most recent recovery summary, or
     *         {@link RecoverySummary#EMPTY}
     */
    public RecoverySummary getLastRecoverySummary()
        {
        return m_summaryRecovery;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
        {
        try
            {
            super.release();
            }
        finally
            {
            detachMBeanIfAttached();
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
        return "JOURNAL";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getStorageVersion()
        {
        return 0;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JournalPersistentStore instantiatePersistentStore(String sId)
        {
        return new JournalPersistentStore(sId);
        }

    /**
     * {@inheritDoc}
     *
     * A journal store that contains no extents is represented by a store
     * directory containing only the generic persistence metadata. Therefore,
     * directory emptiness is not sufficient to identify an empty journal
     * store, particularly after an abrupt process termination.
     */
    @Override
    public boolean isEmpty(String sId)
        {
        sId = validatePersistentStoreId(sId);

        File   dirStore   = new File(f_dirData, sId);
        File   fileExtents = new File(dirStore, EXTENTS_FILENAME);
        File[] aFile      = dirStore.listFiles();

        if (aFile == null || aFile.length == 0)
            {
            return true;
            }

        boolean fNoExtents;
        if (!fileExtents.isFile())
            {
            fNoExtents = true;
            }
        else
            {
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(fileExtents))))
                {
                fNoExtents = in.readInt() == 0;
                }
            catch (IOException e)
                {
                // The normal open path will report corrupt extent metadata
                // with the appropriate persistence context.
                return false;
                }
            }

        if (!fNoExtents)
            {
            return false;
            }

        try
            {
            Properties prop = readMetadata(dirStore);
            return isMetadataComplete(prop) && isMetadataCompatible(prop);
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
    protected PersistenceTools instantiatePersistenceTools(OfflinePersistenceInfo info)
        {
        throw new UnsupportedOperationException("Offline tools are not implemented for journal persistence yet");
        }

    /**
     * Create a snapshot of this manager.
     *
     * @param fileSnapshot  the snapshot directory
     */
    protected void createSnapshot(final File fileSnapshot)
        {
        executeTaskExclusive(new Task()
            {
            @Override
            public void execute()
                {
                for (JournalPersistentStore store : getPersistentStoreMap().values())
                    {
                    File fileStoreFrom = store.getDataDirectory();
                    try
                        {
                        File fileStoreTo = FileHelper.ensureDir(new File(fileSnapshot, fileStoreFrom.getName()));

                        Map<Long, Integer> mapCurrentFileIds = store.prepareSnapshot();

                        CachePersistenceHelper.copyMetadata(fileStoreFrom, fileStoreTo);
                        copyFileIfExists(new File(fileStoreFrom, EXTENTS_FILENAME),
                                new File(fileStoreTo, EXTENTS_FILENAME));

                        copyExtentDirectoriesForSnapshot(fileStoreFrom, fileStoreTo, mapCurrentFileIds);
                        }
                    catch (Exception e)
                        {
                        throw ensurePersistenceException(e,
                                "error creating snapshot \"" + fileSnapshot
                                        + "\" while copying persistent store \""
                                        + fileStoreFrom + '"');
                        }
                    }
                }
            });
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Attempt recovery-only journal-ordered materialization for the supplied
     * store.
     *
     * @param store    the source store
     * @param visitor  the recovery visitor
     *
     * @return {@code true} iff the optimized path was applied
     */
    public static boolean tryIterateRecoveryMaterialized(PersistentStore<ReadBuffer> store, Visitor<ReadBuffer> visitor)
        {
        PersistentStore<ReadBuffer> storeRaw = SafePersistenceWrappers.unwrap(store);
        if (storeRaw instanceof JournalPersistentStore)
            {
            ((JournalPersistentStore) storeRaw).iterateRecoveryMaterialized(visitor);
            return true;
            }

        return false;
        }

    /**
     * Copy extent directories to snapshot directory (hard-link preferred).
     *
     * @param fileStoreFrom   source store directory
     * @param fileStoreTo     destination store directory
     * @param mapCurrentFileIds  current APPENDING file ids by extent (excluded)
     *
     * @throws IOException on I/O failure
     */
    private void copyExtentDirectoriesForSnapshot(File fileStoreFrom, File fileStoreTo,
            Map<Long, Integer> mapCurrentFileIds)
            throws IOException
        {
        File dirExtentsTo = FileHelper.ensureDir(new File(fileStoreTo, EXTENTS_DIRNAME));

        for (Map.Entry<Long, Integer> entry : mapCurrentFileIds.entrySet())
            {
            long lExtentId       = entry.getKey();
            int  nCurrentFileId  = entry.getValue();
            File fileExtentFrom  = extentDirectory(fileStoreFrom, lExtentId);

            if (!fileExtentFrom.isDirectory())
                {
                continue;
                }

            File fileExtentTo = FileHelper.ensureDir(new File(dirExtentsTo, fileExtentFrom.getName()));

            copyFileIfExists(new File(fileExtentFrom, CHECKPOINT_FILENAME),
                    new File(fileExtentTo, CHECKPOINT_FILENAME));

            for (File fileSource : listJournalFiles(fileExtentFrom))
                {
                int nFileId = extractJournalFileId(fileSource);
                if (nFileId == nCurrentFileId)
                    {
                    continue;
                    }

                File fileTarget = new File(fileExtentTo, fileSource.getName());
                linkOrCopy(fileSource, fileTarget);
                }
            }
        }

    /**
     * Copy a file iff it exists.
     *
     * @param fileFrom  source file
     * @param fileTo    destination file
     *
     * @throws IOException on I/O failure
     */
    private void copyFileIfExists(File fileFrom, File fileTo)
            throws IOException
        {
        if (fileFrom.exists())
            {
            Files.copy(fileFrom.toPath(), fileTo.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

    /**
     * Create hard-link from source to destination, falling back to copy.
     *
     * @param fileSource  source file
     * @param fileTarget  destination file
     *
     * @throws IOException on I/O failure
     */
    private void linkOrCopy(File fileSource, File fileTarget)
            throws IOException
        {
        Path pathSource = fileSource.toPath();
        Path pathTarget = fileTarget.toPath();

        try
            {
            Files.createLink(pathTarget, pathSource);
            }
        catch (UnsupportedOperationException | IOException e)
            {
            Files.copy(pathSource, pathTarget, StandardCopyOption.REPLACE_EXISTING);
            }
        }

    /**
     * List journal files in the supplied directory.
     *
     * @param dirStore  store directory
     *
     * @return sorted list of journal files
     */
    private List<File> listJournalFiles(File dirStore)
        {
        File[] aFile = dirStore.listFiles(file -> JOURNAL_FILE_PATTERN.matcher(file.getName()).matches());
        List<File> list = new ArrayList<>();
        if (aFile != null)
            {
            for (File file : aFile)
                {
                list.add(file);
                }
            list.sort((f1, f2) -> Integer.compare(extractJournalFileId(f1), extractJournalFileId(f2)));
            }
        return list;
        }

    /**
     * Extract journal file id from file name.
     *
     * @param file  journal file
     *
     * @return file id or {@code -1} if not parseable
     */
    private int extractJournalFileId(File file)
        {
        Matcher matcher = JOURNAL_FILE_PATTERN.matcher(file.getName());
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : -1;
        }

    /**
     * Discover existing journal file ids in an extent directory.
     *
     * @param dirExtent  the extent directory
     *
     * @return sorted journal file ids
     */
    private List<Integer> listJournalFileIds(File dirExtent)
        {
        File[]        aFile   = dirExtent.listFiles(File::isFile);
        List<Integer> listIds = new ArrayList<>();

        if (aFile != null)
            {
            for (File file : aFile)
                {
                int nFileId = extractJournalFileId(file);
                if (nFileId >= 0)
                    {
                    listIds.add(nFileId);
                    }
                }
            listIds.sort(Integer::compareTo);
            }

        return listIds;
        }

    /**
     * Return the per-extent directory for the specified store and extent.
     *
     * @param dirStore   the store directory
     * @param lExtentId  the extent identifier
     *
     * @return the per-extent directory
     */
    private static File extentDirectory(File dirStore, long lExtentId)
        {
        return new File(new File(dirStore, EXTENTS_DIRNAME), formatExtentDirectory(lExtentId));
        }

    /**
     * Return the per-extent directory name.
     *
     * @param lExtentId  the extent identifier
     *
     * @return the formatted directory name
     */
    private static String formatExtentDirectory(long lExtentId)
        {
        return String.format("%016d", lExtentId);
        }

    /**
     * List extent directories in the supplied store directory.
     *
     * @param dirStore  the store directory
     *
     * @return the sorted extent directories
     */
    private List<File> listExtentDirectories(File dirStore)
        {
        File   dirExtents = new File(dirStore, EXTENTS_DIRNAME);
        File[] aFile      = dirExtents.listFiles(File::isDirectory);
        List<File> list   = new ArrayList<>();

        if (aFile != null)
            {
            for (File file : aFile)
                {
                list.add(file);
                }
            list.sort((f1, f2) -> f1.getName().compareTo(f2.getName()));
            }

        return list;
        }


    // ----- inner class: JournalPersistentStore ----------------------------

    /**
     * Journal-backed persistent store.
     */
    protected class JournalPersistentStore
            extends AbstractPersistenceManager<JournalPersistentStore>.AbstractPersistentStore
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a journal-backed persistent store.
         *
         * @param sId  store identifier
         */
        protected JournalPersistentStore(String sId)
            {
            super(sId);
            }


        // ----- AbstractPersistentStore methods ----------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        protected void openInternal()
            {
            try
                {
                m_mapExtentState.clear();
                Set<Long> setExtentIds     = readPersistedExtentIds();
                m_setPendingExtentIds      = setExtentIds;
                }
            catch (IOException e)
                {
                throw ensurePersistenceException(e);
                }

            attachMBeanIfConfigured();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void copyAndOpenInternal(PersistentStore<ReadBuffer> storeFrom)
            {
            storeFrom = SafePersistenceWrappers.unwrap(storeFrom);

            if (storeFrom instanceof JournalPersistentStore)
                {
                try
                    {
                    JournalPersistentStore storeJournal = (JournalPersistentStore) storeFrom;

                    storeJournal.validateMetadata();
                    copyStoreFiles(storeJournal.f_dirStore, f_dirStore);
                    openInternal();
                    }
                catch (IOException | PersistenceException e)
                    {
                    delete(false);
                    throw e instanceof PersistenceException
                            ? (PersistenceException) e
                            : ensurePersistenceException(e,
                                    "Unable to copy from previous store to new store; from "
                                            + storeFrom + " to " + this);
                    }
                }
            else
                {
                super.copyAndOpenInternal(storeFrom);
                }

            attachMBeanIfConfigured();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void releaseInternal()
            {
            if (!m_fDeleteInProgress)
                {
                try
                    {
                    writePersistedExtentIds(f_setExtentIds);
                    }
                catch (IOException e)
                    {
                    throw ensurePersistenceException(e);
                    }
                }

            closeExtentStates();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean deleteInternal()
            {
            return true;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean delete(boolean fSafe)
            {
            m_fDeleteInProgress = true;
            try
                {
                return super.delete(fSafe);
                }
            finally
                {
                m_fDeleteInProgress = false;
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void loadExtentIdsInternal(Set<Long> setIds)
            {
            try
                {
                Set<Long> setPendingExtentIds = m_setPendingExtentIds;
                if (setPendingExtentIds == null)
                    {
                    setIds.addAll(readPersistedExtentIds());
                    }
                else
                    {
                    setIds.addAll(setPendingExtentIds);
                    m_setPendingExtentIds = null;
                    }
                }
            catch (IOException e)
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
            try
                {
                ensureExtentDirectory(lExtentId);
                writePersistedExtentIds(f_setExtentIds);
                }
            catch (IOException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void deleteExtentInternal(long lExtentId)
            {
            try
                {
                ExtentState state = m_mapExtentState.get(lExtentId);
                if (state != null)
                    {
                    File fileExtent = state.getDirectory();
                    state.release();
                    m_mapExtentState.remove(lExtentId);
                    FileHelper.deleteDir(fileExtent);
                    }
                else
                    {
                    FileHelper.deleteDir(extentDirectory(f_dirStore, lExtentId));
                    }

                writePersistedExtentIds(f_setExtentIds);
                }
            catch (IOException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void truncateExtentInternal(long lExtentId)
            {
            try
                {
                getExtentState(lExtentId).truncate();
                }
            catch (IOException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void moveExtentInternal(long lOldExtentId, long lNewExtentId)
            {
            if (lOldExtentId == lNewExtentId)
                {
                return;
                }

            try
                {
                moveExtentStateInternal(lOldExtentId, lNewExtentId, true);
                writePersistedExtentIds(f_setExtentIds);
                }
            catch (IOException e)
                {
                throw ensurePersistenceException(e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void moveExtents(long[] alOldExtentIds, long[] alNewExtentIds)
            {
            if (alOldExtentIds == null || alNewExtentIds == null)
                {
                throw new IllegalArgumentException("extent identifier arrays cannot be null");
                }

            if (alOldExtentIds.length != alNewExtentIds.length)
                {
                throw new IllegalArgumentException("extent identifier array lengths must match");
                }

            ensureReady();
            lockWrite();
            List<long[]> listMovedPairs = new ArrayList<>();
            List<Long>   listCreated    = new ArrayList<>();
            Set<Long>    setCreated     = new HashSet<>();
            try
                {
                for (int i = 0; i < alOldExtentIds.length; i++)
                    {
                    long lOldExtentId = alOldExtentIds[i];
                    long lNewExtentId = alNewExtentIds[i];

                    if (lOldExtentId == lNewExtentId)
                        {
                        if (!f_setExtentIds.contains(lNewExtentId) && setCreated.add(lNewExtentId))
                            {
                            ensureExtentDirectory(lNewExtentId);
                            listCreated.add(lNewExtentId);
                            }
                        continue;
                        }

                    if (f_setExtentIds.contains(lOldExtentId))
                        {
                        moveExtentStateInternal(lOldExtentId, lNewExtentId, true);
                        listMovedPairs.add(new long[] {lOldExtentId, lNewExtentId});
                        }
                    else if (!f_setExtentIds.contains(lNewExtentId) && setCreated.add(lNewExtentId))
                        {
                        ensureExtentDirectory(lNewExtentId);
                        listCreated.add(lNewExtentId);
                        }
                    }

                Set<Long> setUpdatedExtentIds = new HashSet<>(f_setExtentIds);

                for (long[] alPair : listMovedPairs)
                    {
                    setUpdatedExtentIds.remove(alPair[0]);
                    setUpdatedExtentIds.add(alPair[1]);
                    }

                for (Long LExtentId : listCreated)
                    {
                    setUpdatedExtentIds.add(LExtentId);
                    }

                writePersistedExtentIds(setUpdatedExtentIds);

                f_setExtentIds.clear();
                f_setExtentIds.addAll(setUpdatedExtentIds);
                }
            catch (IOException e)
                {
                rollbackBulkExtentMoves(listMovedPairs, listCreated, e);
                throw ensurePersistenceException(e);
                }
            finally
                {
                unlockWrite();
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ReadBuffer loadInternal(long lExtentId, ReadBuffer bufKey)
            {
            try
                {
                return getExtentState(lExtentId, "load").load(bufKey.toBinary());
                }
            catch (IOException e)
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
            TxToken     token  = ensureToken(oToken);
            ExtentState state  = getExtentState(lExtentId, "store");
            Binary      binKey = bufKey.toBinary();

            token.add(new StoreTxOperation(state, binKey, bufValue));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void eraseInternal(long lExtentId, ReadBuffer bufKey, Object oToken)
            {
            TxToken     token  = ensureToken(oToken);
            ExtentState state  = getExtentState(lExtentId, "erase");
            Binary      binKey = bufKey.toBinary();

            token.add(new EraseTxOperation(state, binKey));
            }

        /**
         * Ensure and return a transaction token.
         *
         * @param oToken  token object
         *
         * @return transaction token
         */
        private TxToken ensureToken(Object oToken)
            {
            if (oToken instanceof TxToken)
                {
                return (TxToken) oToken;
                }

            throw new IllegalArgumentException("illegal token: " + oToken);
            }

        /**
         * Move an extent directory and retained in-memory state without
         * persisting the extent-id set.
         *
         * @param lOldExtentId   the source extent identifier
         * @param lNewExtentId   the destination extent identifier
         * @param fRecordTiming  {@code true} to record move timings
         *
         * @throws IOException on I/O failure
         */
        private void moveExtentStateInternal(long lOldExtentId, long lNewExtentId, boolean fRecordTiming)
                throws IOException
            {
            if (m_mapExtentState.containsKey(lNewExtentId))
                {
                throw new IllegalStateException("extent already exists: " + lNewExtentId);
                }

            ExtentState stateOld = m_mapExtentState.get(lOldExtentId);
            File        fileOld  = extentDirectory(f_dirStore, lOldExtentId);
            File        fileNew  = extentDirectory(f_dirStore, lNewExtentId);

            if (fileNew.exists())
                {
                throw new IllegalStateException("extent directory already exists: " + fileNew);
                }

            if (!fileOld.exists())
                {
                throw new IllegalStateException("extent directory does not exist: " + fileOld);
                }

            FileHelper.ensureDir(fileNew.getParentFile());

            if (stateOld == null)
                {
                Files.move(fileOld.toPath(), fileNew.toPath());
                return;
                }

            stateOld.closeForMove();

            m_mapExtentState.remove(lOldExtentId);
            try
                {
                Files.move(fileOld.toPath(), fileNew.toPath());
                }
            catch (IOException e)
                {
                m_mapExtentState.put(lOldExtentId, stateOld);
                stateOld.reopenAfterMove(lOldExtentId, fileOld);
                throw e;
                }

            try
                {
                stateOld.reopenAfterMove(lNewExtentId, fileNew);
                m_mapExtentState.put(lNewExtentId, stateOld);
                }
            catch (IOException e)
                {
                Files.move(fileNew.toPath(), fileOld.toPath());
                m_mapExtentState.put(lOldExtentId, stateOld);
                stateOld.reopenAfterMove(lOldExtentId, fileOld);
                throw e;
                }
            }

        /**
         * Roll back any successful extent moves or creations from a failed
         * bulk move operation.
         *
         * @param listMovedPairs  moved extent pairs
         * @param listCreated     created extent identifiers
         * @param eFailure        the original failure
         */
        private void rollbackBulkExtentMoves(List<long[]> listMovedPairs, List<Long> listCreated, IOException eFailure)
            {
            for (int i = listMovedPairs.size() - 1; i >= 0; i--)
                {
                long[] alPair = listMovedPairs.get(i);
                try
                    {
                    moveExtentStateInternal(alPair[1], alPair[0], false);
                    }
                catch (IOException eRollback)
                    {
                    eFailure.addSuppressed(eRollback);
                    }
                }

            for (int i = listCreated.size() - 1; i >= 0; i--)
                {
                try
                    {
                    deleteTransientExtent(listCreated.get(i));
                    }
                catch (IOException eRollback)
                    {
                    eFailure.addSuppressed(eRollback);
                    }
                }
            }

        /**
         * Delete a transient extent created during a failed bulk move before
         * the persisted extent-id set has been updated.
         *
         * @param lExtentId  the transient extent identifier
         *
         * @throws IOException on I/O failure
         */
        private void deleteTransientExtent(long lExtentId)
                throws IOException
            {
            ExtentState state = m_mapExtentState.remove(lExtentId);
            if (state != null)
                {
                File fileExtent = state.getDirectory();
                state.release();
                FileHelper.deleteDir(fileExtent);
                }
            else
                {
                FileHelper.deleteDir(extentDirectory(f_dirStore, lExtentId));
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void iterateInternal(Visitor<ReadBuffer> visitor)
            {
            ReadBuffer bufStart = visitor.visitFromKey();
            Binary     binStart = bufStart == null ? null : bufStart.toBinary();

            try
                {
                for (Long LExtentId : f_setExtentIds)
                    {
                    long lExtentId = LExtentId.longValue();
                    if (!visitor.visitExtent(lExtentId))
                        {
                        continue;
                        }

                    ExtentState state = getExtentState(lExtentId, "iterate");
                    state.iterate(binStart, visitor);
                    }
                }
            catch (StopIteration e)
                {
                }
            }

        /**
         * Iterate recovered live entries in journal order for recovery-only
         * full-store materialization.
         *
         * @param visitor  the visitor to apply
         */
        public void iterateRecoveryMaterialized(Visitor<ReadBuffer> visitor)
            {
            if (!isOpen())
                {
                return;
                }

            if (visitor.visitFromKey() != null)
                {
                iterate(visitor);
                return;
                }

            ensureReady();
            lockRead();
            try
                {
                try
                    {
                    for (Long LExtentId : f_setExtentIds)
                        {
                        long lExtentId = LExtentId.longValue();
                        if (!visitor.visitExtent(lExtentId))
                            {
                            continue;
                            }

                        File dirExtent = extentDirectory(f_dirStore, lExtentId);
                        try
                            {
                            JournalRecovery.iterateRecoveredEntries(dirExtent, lExtentId,
                                    (binKey, binValue) -> visitor.visit(lExtentId, binKey, binValue));
                            }
                        catch (IOException e)
                            {
                            throw ensurePersistenceException(e);
                            }
                        }
                    }
                catch (StopIteration e)
                    {
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
        protected Object beginInternal()
            {
            return new TxToken();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void commitInternal(Object oToken)
            {
            TxToken token = ensureToken(oToken);

            try
                {
                token.execute(m_journalConfig.isWriteSynchronous());
                }
            catch (IOException e)
                {
                throw ensurePersistenceException(e);
                }
            finally
                {
                token.clear();
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void abortInternal(Object oToken)
            {
            ensureToken(oToken).clear();
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Ensure and return the state for the specified extent.
         *
         * @param lExtentId  extent id
         *
         * @return the state for the extent
         *
         * @throws IOException on I/O failure
         */
        private ExtentState ensureExtentState(long lExtentId)
                throws IOException
            {
            try
                {
                return m_mapExtentState.computeIfAbsent(lExtentId, lExtent ->
                    {
                    try
                        {
                        ExtentState state = new ExtentState(lExtent, extentDirectory(f_dirStore, lExtent));
                        state.open();
                        return state;
                        }
                    catch (IOException e)
                        {
                        throw new IllegalStateException(e);
                        }
                    });
                }
            catch (IllegalStateException e)
                {
                Throwable cause = e.getCause();
                if (cause instanceof IOException)
                    {
                    throw (IOException) cause;
                    }
                throw e;
                }
            }

        /**
         * Ensure the on-disk directory exists for the specified extent
         * without opening journal state eagerly.
         *
         * @param lExtentId  extent id
         *
         * @throws IOException on I/O failure
         */
        private void ensureExtentDirectory(long lExtentId)
                throws IOException
            {
            FileHelper.ensureDir(extentDirectory(f_dirStore, lExtentId));
            }

        /**
         * Return the state for the specified extent.
         *
         * @param lExtentId  extent id
         *
         * @return the state
         */
        private ExtentState getExtentState(long lExtentId)
            {
            return getExtentState(lExtentId, "generic");
            }

        /**
         * Return the state for the specified extent and record the access
         * reason if the extent must be opened lazily.
         *
         * @param lExtentId  extent id
         * @param sReason    logical access reason
         *
         * @return the state
         */
        private ExtentState getExtentState(long lExtentId, String sReason)
            {
            Long LExtentId = Long.valueOf(lExtentId);
            if (!f_setExtentIds.contains(LExtentId))
                {
                throw new IllegalArgumentException("unknown extent: " + lExtentId);
                }

            ExtentState state = m_mapExtentState.get(lExtentId);
            if (state == null)
                {
                try
                    {
                    state = ensureExtentState(lExtentId);
                    }
                catch (IOException e)
                    {
                    throw ensurePersistenceException(e);
                    }
                }

            return state;
            }

        /**
         * Read persisted extent identifiers.
         *
         * @return persisted extent identifiers
         *
         * @throws IOException on I/O failure
         */
        private Set<Long> readPersistedExtentIds()
                throws IOException
            {
            File fileExtents = new File(f_dirStore, EXTENTS_FILENAME);
            if (!fileExtents.exists())
                {
                return new HashSet<>();
                }

            Set<Long> setExtentIds = new HashSet<>();

            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(fileExtents))))
                {
                int cExtents = in.readInt();
                if (cExtents < 0)
                    {
                    throw new IOException("invalid extent count: " + cExtents);
                    }

                for (int i = 0; i < cExtents; i++)
                    {
                    setExtentIds.add(in.readLong());
                    }
                }

            return setExtentIds;
            }

        /**
         * Persist extent identifiers.
         *
         * @param setExtentIds  extent identifiers to persist
         *
         * @throws IOException on I/O failure
         */
        private void writePersistedExtentIds(Set<Long> setExtentIds)
                throws IOException
            {
            File fileExtents = new File(f_dirStore, EXTENTS_FILENAME);
            File fileTemp    = new File(f_dirStore, EXTENTS_FILENAME + ".tmp");

            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(fileTemp))))
                {
                out.writeInt(setExtentIds.size());
                for (Long LExtentId : setExtentIds)
                    {
                    out.writeLong(LExtentId.longValue());
                    }
                }

            try
                {
                Files.move(fileTemp.toPath(), fileExtents.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                }
            catch (IOException e)
                {
                Files.move(fileTemp.toPath(), fileExtents.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

        /**
         * Prepare this store for snapshotting by forcing a rotate and
         * writing fresh checkpoint/extent metadata.
         *
         * @return current appending file ids after rotation
         *
         * @throws IOException on I/O failure
         */
        private Map<Long, Integer> prepareSnapshot()
                throws IOException
            {
            Map<Long, Integer> mapCurrentFileIds = new HashMap<>();

            for (Long LExtentId : f_setExtentIds)
                {
                ExtentState state = ensureExtentState(LExtentId.longValue());
                mapCurrentFileIds.put(LExtentId, state.prepareSnapshot());
                }

            writePersistedExtentIds(f_setExtentIds);
            return mapCurrentFileIds;
            }

        /**
         * Return the partition journal.
         *
         * @return the journal
         */
        PartitionJournal getJournal()
            {
            return getOnlyExtentState().getJournal();
            }

        /**
         * Return the radix tree.
         *
         * @return the tree
         */
        BinaryRadixTree getTree()
            {
            return getOnlyExtentState().getTree();
            }

        /**
         * Return the checkpoint file number.
         *
         * @return the checkpoint file number
         */
        int getCheckpointFileNo()
            {
            return getOnlyExtentState().getCheckpointFileNo();
            }

        /**
         * Copy store files from source to destination.
         *
         * @param fileFrom  source directory
         * @param fileTo    destination directory
         *
         * @throws IOException on I/O failure
         */
        private void copyStoreFiles(File fileFrom, File fileTo)
                throws IOException
            {
            for (File fileExtentFrom : listExtentDirectories(fileFrom))
                {
                File fileExtentTo = FileHelper.ensureDir(new File(new File(fileTo, EXTENTS_DIRNAME),
                        fileExtentFrom.getName()));

                copyFileIfExists(new File(fileExtentFrom, CHECKPOINT_FILENAME),
                        new File(fileExtentTo, CHECKPOINT_FILENAME));

                for (File fileSource : listJournalFiles(fileExtentFrom))
                    {
                    Files.copy(fileSource.toPath(),
                            new File(fileExtentTo, fileSource.getName()).toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                    }
                }

            copyFileIfExists(new File(fileFrom, EXTENTS_FILENAME),
                    new File(fileTo, EXTENTS_FILENAME));
            }

        /**
         * Return the only extent state, used by single-extent test helpers.
         *
         * @return the only extent state
         */
        private ExtentState getOnlyExtentState()
            {
            if (f_setExtentIds.size() != 1)
                {
                throw new IllegalStateException("expected exactly one extent state");
                }

            return getExtentState(f_setExtentIds.iterator().next());
            }

        /**
         * Release all extent states.
         */
        private void closeExtentStates()
            {
            IOException eFirst = null;
            try
                {
                for (ExtentState state : new ArrayList<>(m_mapExtentState.values()))
                    {
                    try
                        {
                        state.release();
                        }
                    catch (IOException e)
                        {
                        if (eFirst == null)
                            {
                            eFirst = e;
                            }
                        else
                            {
                            eFirst.addSuppressed(e);
                            }
                        }
                    }
                }
            finally
                {
                m_mapExtentState.clear();
                }

            if (eFirst != null)
                {
                throw ensurePersistenceException(eFirst);
                }
            }

        /**
         * Return the number of currently open extent states in this store.
         *
         * @return the number of currently open extent states
         */
        private int getOpenExtentCountSnapshot()
            {
            int cOpen = 0;
            for (ExtentState state : m_mapExtentState.values())
                {
                if (state.isOpenSnapshot())
                    {
                    ++cOpen;
                    }
                }

            return cOpen;
            }

        /**
         * Update the supplied compaction status with this store's extent
         * state.
         *
         * @param status  the status to update
         */
        private void updateCompactionStatus(CompactionStatus status)
            {
            for (ExtentState state : m_mapExtentState.values())
                {
                state.updateCompactionStatus(status);
                }
            }


        // ----- inner class: StopIteration --------------------------------

        /**
         * Fast runtime marker used to stop tree iteration early.
         */
        private static class StopIteration
                extends RuntimeException
            {
            /**
             * Shared singleton instance.
             */
            private static final StopIteration INSTANCE = new StopIteration();

            @Override
            public synchronized Throwable fillInStackTrace()
                {
                return this;
                }
            }

        /**
         * Transaction operation.
         */
        private abstract class TxOperation
            {
            /**
             * Create a transaction operation.
             *
             * @param state   target extent state
             * @param binKey  entry key
             */
            private TxOperation(ExtentState state, Binary binKey)
                {
                f_state  = state;
                f_binKey = binKey;
                }

            /**
             * Return the target extent state.
             *
             * @return target extent state
             */
            protected ExtentState getState()
                {
                return f_state;
                }

            /**
             * Return the entry key.
             *
             * @return entry key
             */
            protected Binary getKey()
                {
                return f_binKey;
                }

            /**
             * Return the journal entry type.
             *
             * @return journal entry type
             */
            protected abstract byte getType();

            /**
             * Return the entry value, or {@code null} if not applicable.
             *
             * @return entry value or {@code null}
             */
            protected ReadBuffer getValue()
                {
                return null;
                }

            /**
             * Target extent state.
             */
            private final ExtentState f_state;

            /**
             * Entry key.
             */
            private final Binary f_binKey;
            }

        /**
         * STORE transaction operation.
         */
        private class StoreTxOperation
                extends TxOperation
            {
            /**
             * Create a STORE operation.
             *
             * @param state     target extent state
             * @param binKey    entry key
             * @param bufValue  entry value
             */
            private StoreTxOperation(ExtentState state, Binary binKey, ReadBuffer bufValue)
                {
                super(state, binKey);
                f_bufValue = bufValue;
                }

            @Override
            protected byte getType()
                {
                return JournalEntry.TYPE_STORE;
                }

            @Override
            protected ReadBuffer getValue()
                {
                return f_bufValue;
                }

            /**
             * Entry value.
             */
            private final ReadBuffer f_bufValue;
            }

        /**
         * ERASE transaction operation.
         */
        private class EraseTxOperation
                extends TxOperation
            {
            /**
             * Create an ERASE operation.
             *
             * @param state   target extent state
             * @param binKey  entry key
             */
            private EraseTxOperation(ExtentState state, Binary binKey)
                {
                super(state, binKey);
                }

            @Override
            protected byte getType()
                {
                return JournalEntry.TYPE_ERASE;
                }
            }

        /**
         * Simulated key state while planning a batched extent mutation.
         */
        private class BatchState
            {
            /**
             * Create a batch state.
             *
             * @param nKind           state kind
             * @param lTicket         concrete ticket, if any
             * @param nPendingIndex   referenced pending mutation index
             */
            private BatchState(int nKind, long lTicket, int nPendingIndex)
                {
                f_nKind         = nKind;
                f_lTicket       = lTicket;
                f_nPendingIndex = nPendingIndex;
                }

            /**
             * Return {@code true} iff this state is ABSENT.
             *
             * @return {@code true} iff this state is ABSENT
             */
            private boolean isAbsent()
                {
                return f_nKind == KIND_ABSENT;
                }

            /**
             * Return the concrete ticket.
             *
             * @return concrete ticket
             */
            private long getTicket()
                {
                return f_lTicket;
                }

            /**
             * Return pending mutation index.
             *
             * @return pending mutation index
             */
            private int getPendingIndex()
                {
                return f_nPendingIndex;
                }

            /**
             * Return state kind.
             *
             * @return state kind
             */
            private int getKind()
                {
                return f_nKind;
                }

            /**
             * State kind: absent.
             */
            private static final int KIND_ABSENT  = 0;

            /**
             * State kind: concrete ticket.
             */
            private static final int KIND_TICKET  = 1;

            /**
             * State kind: pending store mutation.
             */
            private static final int KIND_PENDING = 2;

            /**
             * State kind.
             */
            private final int f_nKind;

            /**
             * Concrete ticket.
             */
            private final long f_lTicket;

            /**
             * Pending mutation index.
             */
            private final int f_nPendingIndex;
            }

        /**
         * Planned batch mutation.
         */
        private class BatchMutation
            {
            /**
             * Create a planned batch mutation.
             *
             * @param op        originating operation
             * @param oldState  previously visible key state
             */
            private BatchMutation(TxOperation op, BatchState oldState)
                {
                f_op       = op;
                f_oldState = oldState;
                }

            /**
             * Return the originating operation.
             *
             * @return originating operation
             */
            private TxOperation getOperation()
                {
                return f_op;
                }

            /**
             * Return previously visible key state.
             *
             * @return previously visible key state
             */
            private BatchState getOldState()
                {
                return f_oldState;
                }

            /**
             * Return the new STORE ticket.
             *
             * @return STORE ticket
             */
            private long getNewTicket()
                {
                return m_lNewTicket;
                }

            /**
             * Set the new STORE ticket.
             *
             * @param lNewTicket  STORE ticket
             */
            private void setNewTicket(long lNewTicket)
                {
                m_lNewTicket = lNewTicket;
                }

            /**
             * Originating operation.
             */
            private final TxOperation f_op;

            /**
             * Previously visible key state.
             */
            private final BatchState f_oldState;

            /**
             * New STORE ticket, if applicable.
             */
            private long m_lNewTicket;
            }

        /**
         * Resolve the concrete ticket represented by the supplied planned
         * state.
         *
         * @param state         planned state
         * @param listMutation  planned mutations
         *
         * @return concrete ticket or {@code 0L}
         */
        private long resolveBatchStateTicket(BatchState state, List<BatchMutation> listMutation)
            {
            switch (state.getKind())
                {
                case BatchState.KIND_ABSENT:
                    return 0L;

                case BatchState.KIND_TICKET:
                    return state.getTicket();

                case BatchState.KIND_PENDING:
                    return listMutation.get(state.getPendingIndex()).getNewTicket();

                default:
                    throw new IllegalStateException("unknown batch state kind: " + state.getKind());
                }
            }

        /**
         * In-memory transaction token.
         */
        private class TxToken
            {
            /**
             * Add an operation to this token.
             *
             * @param op  operation to add
             */
            private void add(TxOperation op)
                {
                m_listOps.add(op);
                }

            /**
             * Execute all queued operations.
             *
             * @param fSynchronous  {@code true} to fsync touched extents
             *
             * @throws IOException on I/O failure
             */
            private void execute(boolean fSynchronous)
                    throws IOException
                {
                Map<ExtentState, List<TxOperation>> mapOpsByExtent = new LinkedHashMap<>();

                for (TxOperation op : m_listOps)
                    {
                    mapOpsByExtent.computeIfAbsent(op.getState(), key -> new ArrayList<>()).add(op);
                    }

                for (Map.Entry<ExtentState, List<TxOperation>> entry : mapOpsByExtent.entrySet())
                    {
                    entry.getKey().applyBatch(entry.getValue());
                    }

                if (fSynchronous)
                    {
                    for (ExtentState state : mapOpsByExtent.keySet())
                        {
                        state.sync();
                        }
                    }
                }

            /**
             * Clear all queued operations.
             */
            private void clear()
                {
                m_listOps.clear();
                }

            /**
             * Operations in this token.
             */
            private final List<TxOperation> m_listOps = new ArrayList<>();
            }


        // ----- constants --------------------------------------------------

        /**
         * Approximate per-entry overhead in bytes (header + CRC + alignment).
         */
        private static final int ENTRY_OVERHEAD = 32;


        // ----- data members -----------------------------------------------

        /**
         * Per-extent state within this store.
         */
        private final Map<Long, ExtentState> m_mapExtentState = new ConcurrentHashMap<>();

        /**
         * {@code true} while this store is being deleted.
         */
        private boolean m_fDeleteInProgress;

        /**
         * Extent identifiers discovered during open and consumed during
         * subsequent extent-id initialization.
         */
        private Set<Long> m_setPendingExtentIds;


        // ----- inner class: ExtentState ----------------------------------

        /**
         * Per-extent journal state.
         */
        private class ExtentState
            {
            /**
             * Create state for the supplied extent.
             *
             * @param lExtentId  the extent identifier
             * @param dirExtent  the extent directory
             */
            private ExtentState(long lExtentId, File dirExtent)
                {
                m_lExtentId = lExtentId;
                m_dirExtent = dirExtent;
                }

            /**
             * Open this extent state.
             *
             * @throws IOException on I/O failure
             */
            private void open()
                    throws IOException
                {
                f_lockExtentWrite.lock();
                try
                    {
                    openInternal();
                    }
                finally
                    {
                    f_lockExtentWrite.unlock();
                    }
                }

            /**
             * Prepare this extent for a directory move by retaining the live
             * tree and rebuilding only journal metadata afterward.
             *
             * @throws IOException on I/O failure
             */
            private void closeForMove()
                    throws IOException
                {
                waitForCheckpointCompletion();

                waitForCompactionCompletion();

                f_lockExtentWrite.lock();
                try
                    {
                    if (m_journal == null || m_tree == null)
                        {
                        throw new IllegalStateException("extent is not open");
                        }

                    m_journal.closeForMove();
                    m_compactor = null;
                    }
                finally
                    {
                    f_lockExtentWrite.unlock();
                    }
                }

            /**
             * Reopen this extent after a directory move using the retained
             * in-memory tree and journal metadata.
             *
             * @param lExtentId  the new extent identifier
             * @param dirExtent  the new extent directory
             *
             * @throws IOException on I/O failure
             */
            private void reopenAfterMove(long lExtentId, File dirExtent)
                    throws IOException
                {
                f_lockExtentWrite.lock();
                try
                    {
                    m_lExtentId = lExtentId;
                    m_dirExtent = dirExtent;

                    m_journal.reopenAfterMove(m_dirExtent);
                    m_compactor = new JournalCompactor(m_journal, m_tree);
                    }
                finally
                    {
                    f_lockExtentWrite.unlock();
                    }
                }

            /**
             * Release this extent state.
             *
             * @throws IOException on I/O failure
             */
            private void release()
                    throws IOException
                {
                waitForCheckpointCompletion();
                waitForCompactionCompletion();
                if (m_journal != null)
                    {
                    writeCheckpoint("shutdown");
                    }
                f_lockExtentWrite.lock();
                try
                    {
                    closeInternal();
                    }
                finally
                    {
                    f_lockExtentWrite.unlock();
                    }
                }

            /**
             * Truncate this extent in place.
             *
             * @throws IOException on I/O failure
             */
            private void truncate()
                    throws IOException
                {
                waitForCheckpointCompletion();
                waitForCompactionCompletion();
                f_lockExtentWrite.lock();
                try
                    {
                    closeInternal();
                    FileHelper.deleteDir(m_dirExtent);
                    openInternal();
                    }
                finally
                    {
                    f_lockExtentWrite.unlock();
                    }
                }

            /**
             * Load a value by key.
             *
             * @param binKey  the key
             *
             * @return the stored value, or {@code null}
             *
             * @throws IOException on I/O failure
             */
            private ReadBuffer load(Binary binKey)
                    throws IOException
                {
                f_lockExtentRead.lock();
                try
                    {
                    long lTicket = m_tree.get(binKey);
                    return lTicket == 0L ? null : m_journal.read(lTicket);
                    }
                finally
                    {
                    f_lockExtentRead.unlock();
                    }
                }

            /**
             * Store a key/value pair.
             *
             * @param binKey    the key
             * @param bufValue  the value
             *
             * @throws IOException on I/O failure
             */
            private void store(Binary binKey, ReadBuffer bufValue)
                    throws IOException
                {
                applyBatch(Collections.singletonList(new StoreTxOperation(this, binKey, bufValue)));
                }

            /**
             * Erase a key.
             *
             * @param binKey  the key
             *
             * @throws IOException on I/O failure
             */
            private void erase(Binary binKey)
                    throws IOException
                {
                applyBatch(Collections.singletonList(new EraseTxOperation(this, binKey)));
                }

            /**
             * Apply a sequence of queued extent operations while holding the
             * extent write lock only once.
             *
             * @param listOps  operations to apply
             *
             * @throws IOException on I/O failure
             */
            private void applyBatch(List<TxOperation> listOps)
                    throws IOException
                {
                if (listOps.isEmpty())
                    {
                    return;
                    }

                f_lockExtentWrite.lock();
                try
                    {
                    List<PartitionJournal.AppendRequest> listAppend    = new ArrayList<>(listOps.size());
                    List<BatchMutation>                  listMutation  = new ArrayList<>(listOps.size());
                    Map<Binary, BatchState>              mapKeyState   = new HashMap<>(listOps.size());
                    long                                 cbWritten     = 0L;
                    int                                  cWrites       = 0;

                    for (TxOperation op : listOps)
                        {
                        Binary     binKey     = op.getKey();
                        BatchState statePrior = mapKeyState.get(binKey);

                        if (statePrior == null)
                            {
                            long lTicket = m_tree.get(binKey);
                            statePrior = lTicket == 0L
                                    ? new BatchState(BatchState.KIND_ABSENT, 0L, -1)
                                    : new BatchState(BatchState.KIND_TICKET, lTicket, -1);
                            mapKeyState.put(binKey, statePrior);
                            }

                        switch (op.getType())
                            {
                            case JournalEntry.TYPE_STORE:
                                {
                                ReadBuffer bufValue = op.getValue();
                                listAppend.add(new PartitionJournal.AppendRequest(JournalEntry.TYPE_STORE, binKey, bufValue));
                                listMutation.add(new BatchMutation(op, statePrior));
                                mapKeyState.put(binKey,
                                        new BatchState(BatchState.KIND_PENDING, 0L, listMutation.size() - 1));
                                cbWritten += binKey.length() + bufValue.length() + ENTRY_OVERHEAD;
                                ++cWrites;
                                break;
                                }

                            case JournalEntry.TYPE_ERASE:
                                {
                                if (statePrior.isAbsent())
                                    {
                                    break;
                                    }

                                listAppend.add(new PartitionJournal.AppendRequest(JournalEntry.TYPE_ERASE, binKey, null));
                                listMutation.add(new BatchMutation(op, statePrior));
                                mapKeyState.put(binKey, new BatchState(BatchState.KIND_ABSENT, 0L, -1));
                                cbWritten += binKey.length() + ENTRY_OVERHEAD;
                                ++cWrites;
                                break;
                                }

                            default:
                                throw new IllegalStateException("unsupported batched operation type: " + op.getType());
                            }
                        }

                    if (listMutation.isEmpty())
                        {
                        return;
                        }

                    long[] alNewTickets = m_journal.appendBatch(listAppend);
                    long[] alRelease    = new long[listMutation.size()];
                    int    cRelease     = 0;

                    for (int i = 0; i < listMutation.size(); i++)
                        {
                        BatchMutation mutation  = listMutation.get(i);
                        TxOperation   op        = mutation.getOperation();
                        Binary        binKey    = op.getKey();
                        long          lOldTicket = resolveBatchStateTicket(mutation.getOldState(), listMutation);

                        switch (op.getType())
                            {
                            case JournalEntry.TYPE_STORE:
                                {
                                long lNewTicket = alNewTickets[i];
                                mutation.setNewTicket(lNewTicket);
                                m_tree.put(binKey, lNewTicket);
                                if (lOldTicket != 0L)
                                    {
                                    alRelease[cRelease++] = lOldTicket;
                                    }
                                break;
                                }

                            case JournalEntry.TYPE_ERASE:
                                m_tree.remove(binKey);
                                if (lOldTicket != 0L)
                                    {
                                    alRelease[cRelease++] = lOldTicket;
                                    }
                                break;

                            default:
                                throw new IllegalStateException("unsupported batched operation type: " + op.getType());
                            }
                        }

                    if (cRelease > 0)
                        {
                        m_journal.releaseBatch(alRelease, cRelease);
                        }

                    trackWrites(cbWritten, cWrites);
                    }
                finally
                    {
                    f_lockExtentWrite.unlock();
                    }
                }

            /**
             * Synchronize dirty journal files for this extent.
             *
             * @throws IOException on I/O failure
             */
            private void sync()
                    throws IOException
                {
                f_lockExtentRead.lock();
                try
                    {
                    if (m_journal != null)
                        {
                        m_journal.sync();
                        }
                    }
                finally
                    {
                    f_lockExtentRead.unlock();
                    }
                }

            /**
             * Prepare this extent for snapshotting.
             *
             * @return the current appending file id
             *
             * @throws IOException on I/O failure
             */
            private int prepareSnapshot()
                    throws IOException
                {
                f_lockExtentWrite.lock();
                try
                    {
                    waitForCheckpointCompletion();

                    if (m_journal == null || m_tree == null)
                        {
                        throw new IllegalStateException("extent is not open");
                        }

                    m_journal.forceRotate();
                    writeCheckpoint("snapshot");

                    return m_nCheckpointFileNo;
                    }
                finally
                    {
                    f_lockExtentWrite.unlock();
                    }
                }

            /**
             * Write a checkpoint for this extent.
             *
             * @throws IOException on I/O failure
             */
            private void writeCheckpoint()
                    throws IOException
                {
                writeCheckpoint("manual");
                }

            /**
             * Write a checkpoint for this extent.
             *
             * @param sReason  checkpoint reason
             *
             * @throws IOException on I/O failure
             */
            private void writeCheckpoint(String sReason)
                    throws IOException
                {
                waitForCheckpointCompletion();
                CheckpointSnapshot snapshot = captureCheckpointSnapshot(sReason);
                try
                    {
                    writeCheckpoint(snapshot);
                    }
                finally
                    {
                    snapshot.release();
                    }
                }

            /**
             * Write the supplied checkpoint snapshot synchronously.
             *
             * @param snapshot  the checkpoint snapshot to persist
             *
             * @throws IOException on I/O failure
             */
            private void writeCheckpoint(CheckpointSnapshot snapshot)
                    throws IOException
                {
                synchronized (f_checkpointLock)
                    {
                    doWriteCheckpoint(snapshot);
                    onCheckpointSuccess(snapshot);
                    }
                }

            /**
             * Capture a checkpoint snapshot while holding the extent read lock.
             *
             * @param sReason  checkpoint reason
             *
             * @return the captured checkpoint snapshot
             *
             * @throws IOException on I/O failure
             */
            private CheckpointSnapshot captureCheckpointSnapshot(String sReason)
                    throws IOException
                {
                f_lockExtentRead.lock();
                try
                    {
                    int          nFileNo             = m_journal.getCurrentFileId();
                    long         ofCheckpoint        = m_journal.getCurrentOffset();
                    int          cCapacity           = m_tree.size();
                    Binary[]     aKeys               = new Binary[cCapacity];
                    long[]       alTickets           = new long[cCapacity];
                    Set<Integer> setReferencedFileIds = new HashSet<>();
                    int[]        acEntries           = new int[1];
                    long         cbDirty             = m_cbWrittenSinceCheckpoint.get();
                    long         cDirtyWrites        = m_cWritesSinceCheckpoint.get();

                    m_tree.visitAll(entry ->
                        {
                        long lTicket = entry.getValue();
                        if (!isVisibleAtCheckpoint(lTicket, nFileNo, ofCheckpoint))
                            {
                            return;
                            }

                        int i = acEntries[0]++;
                        aKeys[i]     = entry.getKey();
                        alTickets[i] = lTicket;
                        setReferencedFileIds.add(PersistentTicket.extractFileId(lTicket));
                        });

                    return new CheckpointSnapshot(
                            new File(m_dirExtent, CHECKPOINT_FILENAME),
                            sReason,
                            nFileNo,
                            ofCheckpoint,
                            cbDirty,
                            setReferencedFileIds,
                            aKeys,
                            alTickets,
                            acEntries[0],
                            cDirtyWrites);
                    }
                finally
                    {
                    f_lockExtentRead.unlock();
                    }
                }

            /**
             * Write a checkpoint file using the supplied captured snapshot.
             *
             * @param snapshot  the checkpoint snapshot to write
             *
             * @throws IOException on I/O failure
             */
            private void doWriteCheckpoint(CheckpointSnapshot snapshot)
                    throws IOException
                {
                long ldtStartTotal = System.nanoTime();
                long cNanosBuild = 0L;

                long ldtStartWrite = System.nanoTime();
                CheckpointFile.write(snapshot.getFileCheckpoint(),
                        snapshot.getFileNo(),
                        snapshot.getCheckpointOffset(),
                        snapshot.getKeys(),
                        snapshot.getTickets(),
                        snapshot.getEntryCount());
                long cNanosWrite = System.nanoTime() - ldtStartWrite;

                recordCheckpointMetrics(snapshot.getReason(),
                        System.nanoTime() - ldtStartTotal,
                        cNanosBuild,
                        cNanosWrite,
                        snapshot.getEntryCount(),
                        snapshot.getFileNo(),
                        snapshot.getCheckpointOffset(),
                        snapshot.getDirtyBytes());
                }

            /**
             * Iterate live entries for this extent while holding the extent
             * read lock for the full tree-walk and value-read sequence.
             *
             * @param binStart  optional starting key
             * @param visitor   the visitor to apply
             */
            private void iterate(Binary binStart, Visitor<ReadBuffer> visitor)
                {
                f_lockExtentRead.lock();
                try
                    {
                    m_tree.visitAll(entry ->
                        {
                        Binary binKey = entry.getKey();

                        if (binStart != null && binKey.compareTo(binStart) < 0)
                            {
                            return;
                            }

                        ReadBuffer bufValue;
                        try
                            {
                            bufValue = m_journal.read(entry.getValue());
                            }
                        catch (IOException e)
                            {
                            throw ensurePersistenceException(e);
                            }

                        if (!visitor.visit(m_lExtentId, binKey, bufValue))
                            {
                            throw StopIteration.INSTANCE;
                            }
                        });
                    }
                finally
                    {
                    f_lockExtentRead.unlock();
                    }
                }

            /**
             * Track bytes written for periodic checkpoints.
             * <p>
             * When {@link PartitionJournalConfig#getCheckpointBytesThreshold()}
             * is {@code 0}, this method returns immediately. That disables both
             * the byte-threshold trigger and the adaptive write-count trigger,
             * because adaptive checkpoint evaluation happens only from this
             * method. Users who want adaptive write-count checkpointing must
             * configure a non-zero byte threshold.
             *
             * @param cbWritten  the number of bytes written
             */
            private void trackBytesWritten(long cbWritten)
                {
                trackWrites(cbWritten, 1);
                }

            /**
             * Track bytes and mutation count written for periodic checkpoints.
             *
             * @param cbWritten  total bytes written
             * @param cWrites    total writes applied
             */
            private void trackWrites(long cbWritten, int cWrites)
                {
                if (!isAutomaticCheckpointingEnabled())
                    {
                    return;
                    }

                long cbDirty = m_cbWrittenSinceCheckpoint.addAndGet(cbWritten);
                long cDirtyWrites = m_cWritesSinceCheckpoint.addAndGet(cWrites);

                if (isPeriodicCheckpointDue(cbDirty, cDirtyWrites))
                    {
                    maybeCheckpoint();
                    }
                }

            /**
             * Attempt a periodic checkpoint for this extent.
             */
            private void maybeCheckpoint()
                {
                if (!isAutomaticCheckpointingEnabled())
                    {
                    return;
                    }

                synchronized (f_checkpointLock)
                    {
                    if (!isPeriodicCheckpointDue(m_cbWrittenSinceCheckpoint.get(), m_cWritesSinceCheckpoint.get())
                        || m_fCheckpointInProgress
                        || m_journal == null
                        || m_tree == null)
                        {
                        return;
                        }

                    // Reserve the checkpoint slot before capturing the
                    // snapshot so async follow-up attempts cannot race in.
                    m_fCheckpointInProgress = true;
                    }

                CheckpointSnapshot snapshot;
                try
                    {
                    snapshot = captureCheckpointSnapshot("periodic");
                    }
                catch (IOException e)
                    {
                    // checkpoint is an optimization; recovery replay covers the gap
                    Logger.warn(() -> "Failed to capture journal checkpoint snapshot for extent \""
                            + m_dirExtent.getAbsolutePath() + "\"; recovery replay will cover the gap.", e);
                    clearCheckpointInProgress();
                    return;
                    }

                Daemons.commonPool().execute(() -> runAsyncCheckpoint(snapshot));
                }

            /**
             * Clear the in-progress periodic-checkpoint flag and wake any
             * waiters blocked on checkpoint completion.
             */
            private void clearCheckpointInProgress()
                {
                synchronized (f_checkpointLock)
                    {
                    m_fCheckpointInProgress = false;
                    f_checkpointLock.notifyAll();
                    }
                }

            /**
             * Return {@code true} if automatic checkpointing is enabled for
             * this extent.
             *
             * @return {@code true} if automatic checkpointing is enabled
             */
            private boolean isAutomaticCheckpointingEnabled()
                {
                return m_journalConfig.getCheckpointBytesThreshold() > 0L;
                }

            /**
             * Return {@code true} if adaptive checkpointing is enabled.
             *
             * @return {@code true} if adaptive checkpointing is enabled
             */
            private boolean isAdaptiveCheckpointingEnabled()
                {
                return isAutomaticCheckpointingEnabled()
                        && m_journalConfig.getAdaptiveCheckpointInitialWrites() > 0L;
                }

            /**
             * Return {@code true} if a periodic checkpoint is currently due.
             *
             * @param cbDirty  dirty bytes since the last completed checkpoint
             * @param cWrites  writes since the last completed checkpoint
             *
             * @return {@code true} if a periodic checkpoint is due
             */
            private boolean isPeriodicCheckpointDue(long cbDirty, long cWrites)
                {
                return cbDirty >= m_journalConfig.getCheckpointBytesThreshold()
                        || isAdaptiveCheckpointDue(cWrites);
                }

            /**
             * Return {@code true} if the adaptive checkpoint write-count
             * trigger has been reached.
             *
             * @param cWrites  writes since the last completed checkpoint
             *
             * @return {@code true} if the adaptive write trigger has been reached
             */
            private boolean isAdaptiveCheckpointDue(long cWrites)
                {
                return isAdaptiveCheckpointingEnabled()
                        && m_cCheckpointWriteTrigger > 0L
                        && cWrites >= m_cCheckpointWriteTrigger;
                }

            /**
             * Record checkpoint metrics and optionally log the checkpoint.
             *
             * @param sReason      checkpoint reason
             * @param cNanosTotal  total checkpoint time
             * @param cNanosBuild  metadata-build time
             * @param cNanosWrite  checkpoint write time
             * @param cEntries     checkpoint entry count
             */
            private void recordCheckpointMetrics(String sReason, long cNanosTotal, long cNanosBuild,
                    long cNanosWrite, int cEntries, int nFileNo, long ofCheckpoint, long cbDirtyBytes)
                {
                String sReasonLabel = sReason == null || sReason.isEmpty() ? "unspecified" : sReason;
                Logger.finer(() -> String.format("Journal checkpoint[%s] extent=%s entries=%d file=%d offset=%d "
                                + "dirty-bytes=%d total-ms=%.3f build-ms=%.3f write-ms=%.3f",
                        sReasonLabel,
                        m_dirExtent.getAbsolutePath(),
                        cEntries,
                        nFileNo,
                        ofCheckpoint,
                        cbDirtyBytes,
                        cNanosTotal / 1_000_000.0d,
                        cNanosBuild / 1_000_000.0d,
                        cNanosWrite / 1_000_000.0d));
                }

            /**
             * Run an asynchronous periodic checkpoint.
             *
             * @param snapshot  the captured checkpoint snapshot
             */
            private void runAsyncCheckpoint(CheckpointSnapshot snapshot)
                {
                boolean fSuccess = false;
                try
                    {
                    long ldtStartTotal = System.nanoTime();
                    long cNanosBuild   = 0L;

                    long ldtStartWrite = System.nanoTime();
                    CheckpointFile.write(snapshot.getFileCheckpoint(),
                            snapshot.getFileNo(),
                            snapshot.getCheckpointOffset(),
                            snapshot.getKeys(),
                            snapshot.getTickets(),
                            snapshot.getEntryCount());
                    long cNanosWrite = System.nanoTime() - ldtStartWrite;

                    synchronized (f_checkpointLock)
                        {
                        if (m_journal != null && m_tree != null)
                            {
                            onCheckpointSuccess(snapshot);
                            recordAdaptiveCheckpointDuration(snapshot.getReason(), System.nanoTime() - ldtStartTotal);
                            fSuccess = true;
                            }
                        }
                    clearCheckpointInProgress();

                    if (fSuccess)
                        {
                        recordCheckpointMetrics(snapshot.getReason(),
                                System.nanoTime() - ldtStartTotal,
                                cNanosBuild,
                                cNanosWrite,
                                snapshot.getEntryCount(),
                                snapshot.getFileNo(),
                                snapshot.getCheckpointOffset(),
                                snapshot.getDirtyBytes());
                        }
                    }
                catch (IOException e)
                    {
                    Logger.warn(() -> "Failed to write async journal checkpoint for extent \""
                            + m_dirExtent.getAbsolutePath() + "\"; recovery replay will cover the gap.", e);
                    clearCheckpointInProgress();
                    }
                catch (RuntimeException e)
                    {
                    Logger.warn(() -> "Failed to finalize async journal checkpoint for extent \""
                            + m_dirExtent.getAbsolutePath() + "\".", e);
                    clearCheckpointInProgress();
                    }
                finally
                    {
                    snapshot.release();
                    if (fSuccess)
                        {
                        maybeCheckpoint();
                        }
                    }
                }

            /**
             * Return {@code true} iff the supplied ticket is visible at the
             * specified checkpoint boundary.
             *
             * @param lTicket        the ticket to evaluate
             * @param nFileNo        checkpoint file number
             * @param ofCheckpoint   checkpoint offset
             *
             * @return {@code true} iff the ticket is visible at the checkpoint
             */
            private boolean isVisibleAtCheckpoint(long lTicket, int nFileNo, long ofCheckpoint)
                {
                int nTicketFile = PersistentTicket.extractFileId(lTicket);
                if (nTicketFile < nFileNo)
                    {
                    return true;
                    }

                return nTicketFile == nFileNo
                        && PersistentTicket.extractEntryOffset(lTicket) < ofCheckpoint;
                }

            /**
             * Reduce dirty bytes by the amount covered by a successful checkpoint.
             *
             * @param cbCheckpointDirty  covered dirty bytes
             */
            private void reduceDirtyBytes(long cbCheckpointDirty)
                {
                m_cbWrittenSinceCheckpoint.updateAndGet(v -> Math.max(0L, v - cbCheckpointDirty));
                }

            /**
             * Reduce dirty writes by the amount covered by a successful
             * checkpoint.
             *
             * @param cCheckpointWrites  covered dirty writes
             */
            private void reduceDirtyWrites(long cCheckpointWrites)
                {
                long cWrites = m_cWritesSinceCheckpoint.addAndGet(-cCheckpointWrites);
                if (cWrites < 0L)
                    {
                    m_cWritesSinceCheckpoint.set(0L);
                    }
                }

            /**
             * Apply successful checkpoint side effects.
             *
             * @param snapshot  the completed checkpoint snapshot
             *
             * @throws IOException on I/O failure
             */
            private void onCheckpointSuccess(CheckpointSnapshot snapshot)
                    throws IOException
                {
                m_nCheckpointFileNo = snapshot.getFileNo();
                m_ofCheckpoint      = snapshot.getCheckpointOffset();
                reduceDirtyBytes(snapshot.getDirtyBytes());
                reduceDirtyWrites(snapshot.getDirtyWrites());
                reclaimCheckpointObsoleteFiles(snapshot.getReferencedFileIds(), snapshot.getFileNo());

                if (shouldScheduleCompaction(snapshot.getReason()))
                    {
                    requestCompaction();
                    }
                }

            /**
             * Return {@code true} if a successful checkpoint for the supplied
             * reason should trigger background compaction.
             *
             * @param sReason  checkpoint reason
             *
             * @return {@code true} if compaction should be considered
             */
            private boolean shouldScheduleCompaction(String sReason)
                {
                return !"snapshot".equals(sReason);
                }

            /**
             * Record a successful periodic checkpoint duration and, when the
             * rolling median exceeds the configured budget, back off the
             * adaptive checkpoint write trigger.
             *
             * @param sReason      checkpoint reason
             * @param cNanosTotal  total checkpoint duration
             */
            private void recordAdaptiveCheckpointDuration(String sReason, long cNanosTotal)
                {
                if (!"periodic".equals(sReason) || !isAdaptiveCheckpointingEnabled())
                    {
                    return;
                    }

                if (m_cAdaptiveCheckpointWarmupRemaining > 0)
                    {
                    --m_cAdaptiveCheckpointWarmupRemaining;
                    return;
                    }

                m_acCheckpointSamples[m_iCheckpointSample] = cNanosTotal;
                m_iCheckpointSample = (m_iCheckpointSample + 1) % m_acCheckpointSamples.length;
                if (m_cCheckpointSamples < m_acCheckpointSamples.length)
                    {
                    ++m_cCheckpointSamples;
                    }

                long cMedian = computeAdaptiveCheckpointMedianNanos();
                long cTarget = (long) (m_journalConfig.getAdaptiveCheckpointTargetMillis() * 1_000_000.0d);

                if (cMedian > cTarget)
                    {
                    long cOldTrigger = m_cCheckpointWriteTrigger;
                    long cNewTrigger = Math.min(m_journalConfig.getAdaptiveCheckpointMaxWrites(),
                            Math.max(m_journalConfig.getAdaptiveCheckpointMinWrites(),
                                    Math.max(m_cCheckpointWriteTrigger + 1L, m_cCheckpointWriteTrigger * 2L)));

                    if (cNewTrigger != m_cCheckpointWriteTrigger)
                        {
                        m_cCheckpointWriteTrigger = cNewTrigger;
                        Logger.config(() -> "Adaptive journal checkpoint trigger changed for extent \""
                                + m_dirExtent.getAbsolutePath() + "\": " + cOldTrigger + " -> " + cNewTrigger
                                + " writes (median=" + String.format("%.3f", cMedian / 1_000_000.0d)
                                + " ms, target="
                                + String.format("%.3f", m_journalConfig.getAdaptiveCheckpointTargetMillis())
                                + " ms)");
                        resetAdaptiveCheckpointSamples();
                        }
                    }
                }

            /**
             * Return the rolling median adaptive checkpoint duration.
             *
             * @return the rolling median adaptive checkpoint duration, in nanos
             */
            private long computeAdaptiveCheckpointMedianNanos()
                {
                if (m_cCheckpointSamples == 0)
                    {
                    return 0L;
                    }

                long[] acSamples = Arrays.copyOf(m_acCheckpointSamples, m_cCheckpointSamples);
                Arrays.sort(acSamples);
                return acSamples[m_cCheckpointSamples / 2];
                }

            /**
             * Reset the rolling adaptive checkpoint samples.
             */
            private void resetAdaptiveCheckpointSamples()
                {
                Arrays.fill(m_acCheckpointSamples, 0L);
                m_cCheckpointSamples = 0;
                m_iCheckpointSample  = 0;
                }

            /**
             * Reclaim files that are no longer needed once a checkpoint has
             * been durably written.
             *
             * @param setReferencedFileIds  file ids still referenced by the tree
             * @param nCheckpointFileNo     checkpoint file id
             *
             * @throws IOException on I/O failure
             */
            private void reclaimCheckpointObsoleteFiles(Set<Integer> setReferencedFileIds, int nCheckpointFileNo)
                    throws IOException
                {
                for (PartitionJournal.JournalFileInfo info : m_journal.getFiles())
                    {
                    int nFileId = info.getFileId();

                    if (nFileId >= nCheckpointFileNo
                        || info.getState() != PartitionJournal.FileState.FULL
                        || setReferencedFileIds.contains(nFileId))
                        {
                        continue;
                        }

                    m_journal.markEvacuating(nFileId);
                    m_journal.markGarbage(nFileId);
                    m_journal.discard(nFileId);
                    }
                }

            /**
             * Request background compaction for this extent.
             */
            private void requestCompaction()
                {
                synchronized (f_compactionLock)
                    {
                    if (m_journal == null || m_tree == null || m_compactor == null)
                        {
                        return;
                        }

                    if (m_fCompactionInProgress)
                        {
                        m_fCompactionRequested = true;
                        return;
                        }

                    m_fCompactionInProgress = true;
                    m_fCompactionRequested  = false;
                    Daemons.commonPool().execute(this::runAsyncCompaction);
                    }
                }

            /**
             * Run one bounded background compaction pass.
             */
            private void runAsyncCompaction()
                {
                boolean fReschedule = false;
                int     cCompacted  = 0;
                long    ldtStart    = System.nanoTime();

                try
                    {
                    f_lockExtentWrite.lock();
                    try
                        {
                        if (m_journal != null && m_tree != null && m_compactor != null && m_nCheckpointFileNo > 0)
                            {
                            cCompacted = m_compactor.compact(m_nCheckpointFileNo,
                                    m_journalConfig.getCompactionMinLoadFactor(),
                                    DEFAULT_COMPACTION_MAX_FILES_PER_RUN);
                            }
                        }
                    finally
                        {
                        f_lockExtentWrite.unlock();
                        }
                    }
                catch (IOException e)
                    {
                    synchronized (f_compactionLock)
                        {
                        m_fCompactionRequested  = false;
                        m_fCompactionInProgress = false;
                        f_compactionLock.notifyAll();
                        }
                    return;
                    }

                synchronized (f_compactionLock)
                    {
                    boolean fRequested = m_fCompactionRequested;
                    m_fCompactionRequested = false;

                    fReschedule = fRequested || cCompacted >= DEFAULT_COMPACTION_MAX_FILES_PER_RUN;
                    if (!fReschedule)
                        {
                        m_fCompactionInProgress = false;
                        f_compactionLock.notifyAll();
                        }
                    }

                if (fReschedule)
                    {
                    Daemons.commonPool().execute(this::runAsyncCompaction);
                    }
                }

            /**
             * Wait for any in-flight background compaction to complete.
             */
            private void waitForCompactionCompletion()
                {
                synchronized (f_compactionLock)
                    {
                    while (m_fCompactionInProgress)
                        {
                        try
                            {
                            f_compactionLock.wait();
                            }
                        catch (InterruptedException e)
                            {
                            Thread.currentThread().interrupt();
                            throw ensurePersistenceException(e);
                            }
                        }
                    }
                }

            /**
             * Wait for any in-flight periodic checkpoint to complete.
             */
            private void waitForCheckpointCompletion()
                {
                synchronized (f_checkpointLock)
                    {
                    while (m_fCheckpointInProgress)
                        {
                        try
                            {
                            f_checkpointLock.wait();
                            }
                        catch (InterruptedException e)
                            {
                            Thread.currentThread().interrupt();
                            throw ensurePersistenceException(e);
                            }
                        }
                    }
                }

            /**
             * Return the extent directory.
             *
             * @return the extent directory
             */
            private File getDirectory()
                {
                return m_dirExtent;
                }

            /**
             * Return the journal.
             *
             * @return the journal
             */
            private PartitionJournal getJournal()
                {
                return m_journal;
                }

            /**
             * Return the radix tree.
             *
             * @return the radix tree
             */
            private BinaryRadixTree getTree()
                {
                return m_tree;
                }

            /**
             * Return the checkpoint file id.
             *
             * @return the checkpoint file id
             */
            private int getCheckpointFileNo()
                {
                return m_nCheckpointFileNo;
                }

            /**
             * Return {@code true} if this extent is currently open.
             *
             * @return {@code true} if this extent is currently open
             */
            private boolean isOpenSnapshot()
                {
                f_lockExtentRead.lock();
                try
                    {
                    return m_journal != null;
                    }
                finally
                    {
                    f_lockExtentRead.unlock();
                    }
                }

            /**
             * Update the supplied compaction status with this extent's state.
             *
             * @param status  the status to update
             */
            private void updateCompactionStatus(CompactionStatus status)
                {
                synchronized (f_compactionLock)
                    {
                    if (m_journal != null)
                        {
                        status.m_cOpenExtents++;
                        }
                    if (m_fCompactionInProgress)
                        {
                        status.m_cCompacting++;
                        }
                    if (m_fCompactionRequested)
                        {
                        status.m_cRequested++;
                        }
                    }
                }

            /**
             * Extent identifier.
             */
            private long m_lExtentId;

            /**
             * Extent directory.
             */
            private File m_dirExtent;

            /**
             * Lock guarding periodic checkpoint writes.
             */
            private final Object f_checkpointLock = new Object();

            /**
             * Lock guarding background compaction scheduling.
             */
            private final Object f_compactionLock = new Object();

            /**
             * Extent read/write lock guarding tree and journal state access.
             */
            private final ReentrantReadWriteLock f_lockExtent = new ReentrantReadWriteLock();

            /**
             * Shared extent read lock.
             */
            private final Lock f_lockExtentRead = f_lockExtent.readLock();

            /**
             * Exclusive extent write lock.
             */
            private final Lock f_lockExtentWrite = f_lockExtent.writeLock();

            /**
             * Approximate bytes written since the last checkpoint.
             */
            private final AtomicLong m_cbWrittenSinceCheckpoint = new AtomicLong();

            /**
             * Approximate writes since the last checkpoint.
             */
            private final AtomicLong m_cWritesSinceCheckpoint = new AtomicLong();

            /**
             * Journal for this extent.
             */
            private PartitionJournal m_journal;

            /**
             * Tree for this extent.
             */
            private BinaryRadixTree m_tree;

            /**
             * Compactor for this extent.
             */
            private JournalCompactor m_compactor;

            /**
             * Last checkpoint file number.
             */
            private int m_nCheckpointFileNo;

            /**
             * Last checkpoint offset.
             */
            private long m_ofCheckpoint;

            /**
             * Flag indicating that a periodic checkpoint is currently in flight.
             */
            private boolean m_fCheckpointInProgress;

            /**
             * Flag indicating that background compaction is currently in flight.
             */
            private boolean m_fCompactionInProgress;

            /**
             * Flag indicating that another compaction pass should run when the
             * current pass completes.
             */
            private boolean m_fCompactionRequested;

            /**
             * Current adaptive checkpoint write trigger.
             */
            private volatile long m_cCheckpointWriteTrigger = m_journalConfig.getAdaptiveCheckpointInitialWrites();

            /**
             * Remaining adaptive checkpoint warmup samples.
             */
            private int m_cAdaptiveCheckpointWarmupRemaining = m_journalConfig.getAdaptiveCheckpointWarmupCount();

            /**
             * Rolling adaptive checkpoint duration samples, in nanos.
             */
            private final long[] m_acCheckpointSamples = new long[DEFAULT_ADAPTIVE_CHECKPOINT_SAMPLE_COUNT];

            /**
             * Number of adaptive checkpoint samples currently populated.
             */
            private int m_cCheckpointSamples;

            /**
             * Next adaptive checkpoint sample index.
             */
            private int m_iCheckpointSample;

            /**
             * Close this extent without taking the extent lock.
             *
             * @throws IOException on I/O failure
             */
            private void closeInternal()
                    throws IOException
                {
                if (m_journal != null)
                    {
                    m_journal.close();
                    }

                m_journal   = null;
                m_tree      = null;
                m_compactor = null;
                m_cbWrittenSinceCheckpoint.set(0);
                m_cWritesSinceCheckpoint.set(0L);
                m_cCheckpointWriteTrigger = m_journalConfig.getAdaptiveCheckpointInitialWrites();
                m_cAdaptiveCheckpointWarmupRemaining = m_journalConfig.getAdaptiveCheckpointWarmupCount();
                resetAdaptiveCheckpointSamples();
                }

            /**
             * Open this extent without taking the extent lock.
             *
             * @throws IOException on I/O failure
             */
            private void openInternal()
                    throws IOException
                {
                FileHelper.ensureDir(m_dirExtent);

                m_journal = new PartitionJournal(m_dirExtent, m_journalConfig);
                m_journal.open();

                m_tree = new BinaryRadixTree();

                JournalRecovery.RecoveryResult recoveryResult =
                        JournalRecovery.recover(m_dirExtent, m_journal, m_tree, m_lExtentId);

                if (recoveryResult.getTimings().hasCheckpoint())
                    {
                    m_nCheckpointFileNo = recoveryResult.getCheckpointFileNo();
                    m_ofCheckpoint      = recoveryResult.getCheckpointOffset();
                    }
                else
                    {
                    m_nCheckpointFileNo = m_journal.getCurrentFileId();
                    m_ofCheckpoint      = m_journal.getCurrentOffset();
                    }

                m_compactor = new JournalCompactor(m_journal, m_tree);

                m_cCheckpointWriteTrigger = m_journalConfig.getAdaptiveCheckpointInitialWrites();
                m_cAdaptiveCheckpointWarmupRemaining = m_journalConfig.getAdaptiveCheckpointWarmupCount();
                resetAdaptiveCheckpointSamples();

                JournalRecovery.RecoveryTimings timings = recoveryResult.getTimings();
                if (PersistenceRecoveryMetrics.isEnabled())
                    {
                    PersistenceRecoveryMetrics.recordSummary(
                            1L,
                            timings.getKeyCount(),
                            timings.getCheckpointBytes(),
                            timings.getTotalNanos(),
                            timings.getCheckpointLoadNanos(),
                            timings.getJournalScanNanos(),
                            timings.getJournalReplayNanos(),
                            0L);
                    }

                String sSummary = String.format("Journal recovery summary: extent=%s checkpoint=%s files=%d "
                                + "keys=%d checkpoint-bytes=%d total-ms=%.3f checkpoint-load-ms=%.3f "
                                + "journal-scan-ms=%.3f journal-replay-ms=%.3f",
                        m_dirExtent.getAbsolutePath(),
                        timings.hasCheckpoint(),
                        timings.getJournalFileCount(),
                        timings.getKeyCount(),
                        timings.getCheckpointBytes(),
                        timings.getTotalNanos() / 1_000_000.0d,
                        timings.getCheckpointLoadNanos() / 1_000_000.0d,
                        timings.getJournalScanNanos() / 1_000_000.0d,
                        timings.getJournalReplayNanos() / 1_000_000.0d);

                m_summaryRecovery = RecoverySummary.of(sSummary,
                        System.currentTimeMillis(),
                        JournalPersistentStore.this.getId());

                Logger.finer(sSummary);
                emitRecoveryEvents(timings);
                }

            /**
             * Emit JFR events for the journal recovery phases represented by
             * the supplied timings.
             *
             * @param timings  recovery timings
             */
            private void emitRecoveryEvents(JournalRecovery.RecoveryTimings timings)
                {
                boolean fCheckpoint = timings.hasCheckpoint();

                emitRecoveryEvent("checkpoint-load",
                        fCheckpoint,
                        0,
                        0L,
                        fCheckpoint ? timings.getCheckpointBytes() : -1L,
                        timings.getCheckpointLoadNanos());

                emitRecoveryEvent("journal-scan",
                        fCheckpoint,
                        timings.getJournalFileCount(),
                        0L,
                        -1L,
                        timings.getJournalScanNanos());

                emitRecoveryEvent("journal-replay",
                        fCheckpoint,
                        0,
                        0L,
                        -1L,
                        timings.getJournalReplayNanos());

                emitRecoveryEvent("total",
                        fCheckpoint,
                        timings.getJournalFileCount(),
                        timings.getKeyCount(),
                        -1L,
                        timings.getTotalNanos());
                }

            /**
             * Emit one journal recovery phase event if JFR is recording it.
             *
             * @param sPhase           recovery phase
             * @param fCheckpoint      {@code true} if checkpoint was loaded
             * @param cFiles           journal file count for this phase
             * @param cKeys            recovered key count for this phase
             * @param cbCheckpoint     checkpoint bytes, or {@code -1}
             * @param cDurationNanos   phase duration in nanoseconds
             */
            private void emitRecoveryEvent(String sPhase, boolean fCheckpoint, int cFiles, long cKeys,
                    long cbCheckpoint, long cDurationNanos)
                {
                JournalRecoveryEvent event = new JournalRecoveryEvent();
                if (event.shouldCommit())
                    {
                    event.phase           = sPhase;
                    event.extentPath      = m_dirExtent.getAbsolutePath();
                    event.checkpoint      = fCheckpoint;
                    event.fileCount       = cFiles;
                    event.keyCount        = cKeys;
                    event.checkpointBytes = cbCheckpoint;
                    event.durationNanos   = cDurationNanos;
                    event.commit();
                    }
                }

            // ----- inner class: CheckpointSnapshot -----------------------

            /**
             * Immutable periodic checkpoint snapshot captured under the extent lock.
             */
            private class CheckpointSnapshot
                {
                /**
                 * Create a checkpoint snapshot.
                 *
                 * @param fileCheckpoint  the checkpoint file
                 * @param sReason         the checkpoint reason
                 * @param nFileNo         checkpoint file number
                 * @param ofCheckpoint    checkpoint offset
                 * @param cbDirtyBytes    dirty bytes covered by the checkpoint
                 * @param setReferencedFileIds  referenced file ids at checkpoint
                 * @param aKeys           checkpoint keys
                 * @param alTickets       checkpoint tickets
                 * @param cEntries        number of valid entries
                 */
                private CheckpointSnapshot(File fileCheckpoint, String sReason, int nFileNo, long ofCheckpoint,
                        long cbDirtyBytes, Set<Integer> setReferencedFileIds, Binary[] aKeys, long[] alTickets,
                        int cEntries, long cDirtyWrites)
                    {
                    f_fileCheckpoint = fileCheckpoint;
                    f_sReason        = sReason;
                    f_nFileNo        = nFileNo;
                    f_ofCheckpoint   = ofCheckpoint;
                    f_cbDirtyBytes   = cbDirtyBytes;
                    f_cDirtyWrites   = cDirtyWrites;
                    f_setReferencedFileIds = Collections.unmodifiableSet(new HashSet<>(setReferencedFileIds));
                    f_aKeys          = aKeys;
                    f_alTickets      = alTickets;
                    f_cEntries       = cEntries;
                    }

                /**
                 * Return the checkpoint file.
                 *
                 * @return the checkpoint file
                 */
                private File getFileCheckpoint()
                    {
                    return f_fileCheckpoint;
                    }

                /**
                 * Return the checkpoint reason.
                 *
                 * @return the checkpoint reason
                 */
                private String getReason()
                    {
                    return f_sReason;
                    }

                /**
                 * Return the checkpoint file number.
                 *
                 * @return the checkpoint file number
                 */
                private int getFileNo()
                    {
                    return f_nFileNo;
                    }

                /**
                 * Return the checkpoint offset.
                 *
                 * @return the checkpoint offset
                 */
                private long getCheckpointOffset()
                    {
                    return f_ofCheckpoint;
                    }

                /**
                 * Return dirty bytes covered by the checkpoint.
                 *
                 * @return dirty bytes covered by the checkpoint
                 */
                private long getDirtyBytes()
                    {
                    return f_cbDirtyBytes;
                    }

                /**
                 * Return dirty writes covered by the checkpoint.
                 *
                 * @return dirty writes covered by the checkpoint
                 */
                private long getDirtyWrites()
                    {
                    return f_cDirtyWrites;
                    }

                /**
                 * Return the file ids referenced by the checkpoint tree.
                 *
                 * @return referenced file ids
                 */
                private Set<Integer> getReferencedFileIds()
                    {
                    return f_setReferencedFileIds;
                    }

                /**
                 * Return captured keys.
                 *
                 * @return captured keys
                 */
                private Binary[] getKeys()
                    {
                    return f_aKeys;
                    }

                /**
                 * Return captured tickets.
                 *
                 * @return captured tickets
                 */
                private long[] getTickets()
                    {
                    return f_alTickets;
                    }

                /**
                 * Return entry count.
                 *
                 * @return entry count
                 */
                private int getEntryCount()
                    {
                    return f_cEntries;
                    }

                /**
                 * Release strong references held by this snapshot.
                 */
                private void release()
                    {
                    for (int i = 0; i < f_cEntries; i++)
                        {
                        f_aKeys[i] = null;
                        }
                    }

                /**
                 * Checkpoint file.
                 */
                private final File f_fileCheckpoint;

                /**
                 * Checkpoint reason.
                 */
                private final String f_sReason;

                /**
                 * Checkpoint file number.
                 */
                private final int f_nFileNo;

                /**
                 * Checkpoint offset.
                 */
                private final long f_ofCheckpoint;

                /**
                 * Dirty bytes covered by the checkpoint.
                 */
                private final long f_cbDirtyBytes;

                /**
                 * Dirty writes covered by the checkpoint.
                 */
                private final long f_cDirtyWrites;

                /**
                 * File ids referenced by the checkpoint tree.
                 */
                private final Set<Integer> f_setReferencedFileIds;

                /**
                 * Captured keys.
                 */
                private final Binary[] f_aKeys;

                /**
                 * Captured tickets.
                 */
                private final long[] f_alTickets;

                /**
                 * Number of valid entries.
                 */
                private final int f_cEntries;
                }
            }
        }

    // ----- inner class: CompactionStatus ---------------------------------

    /**
     * Mutable local accumulator for compaction state snapshots.
     */
    private static class CompactionStatus
        {
        /**
         * Format this status.
         *
         * @return formatted status
         */
        private String format()
            {
            return String.format("%s open-extents=%d compacting=%d requested=%d",
                    m_cCompacting > 0 ? "active" : "idle",
                    m_cOpenExtents,
                    m_cCompacting,
                    m_cRequested);
            }

        /**
         * Open extent count.
         */
        private int m_cOpenExtents;

        /**
         * Extents with compaction currently in progress.
         */
        private int m_cCompacting;

        /**
         * Extents with another compaction pass requested.
         */
        private int m_cRequested;
        }


    // ----- constants ------------------------------------------------------

    /**
     * Default journal file size (4 GB).
     */
    private static final long DEFAULT_MAX_FILE_SIZE = 4L * 1024L * 1024L * 1024L;

    /**
     * Checkpoint file name.
     */
    private static final String CHECKPOINT_FILENAME = "checkpoint.coh";

    /**
     * Maximum number of files to compact in a single background pass.
     */
    private static final int DEFAULT_COMPACTION_MAX_FILES_PER_RUN = 1;

    /**
     * Number of checkpoint duration samples used for adaptive backoff.
     */
    private static final int DEFAULT_ADAPTIVE_CHECKPOINT_SAMPLE_COUNT = 16;

    /**
     * Extent metadata file name.
     */
    private static final String EXTENTS_FILENAME = "extents.coh";

    /**
     * Extent state directory name.
     */
    private static final String EXTENTS_DIRNAME = "extents";

    /**
     * Journal file name pattern.
     */
    private static final Pattern JOURNAL_FILE_PATTERN = Pattern.compile("journal-(\\d{6})\\.coh");


    // ----- MBean helpers --------------------------------------------------

    /**
     * Attach the manager to the service-level MBean if MBean context was set.
     */
    private void attachMBeanIfConfigured()
        {
        String         sService  = m_sMBeanService;
        ManagerRole    role      = m_roleMBean;
        MBeanRegistrar registrar = m_registrarMBean;

        if (sService != null && role != null && registrar != null && f_fMBeanAttached.compareAndSet(false, true))
            {
            try
                {
                JournalPersistenceMBeanRegistry.shared(registrar).attach(sService, role, this);
                }
            catch (RuntimeException e)
                {
                f_fMBeanAttached.set(false);
                throw e;
                }
            }
        }

    /**
     * Detach the manager from the service-level MBean if it was attached.
     */
    private void detachMBeanIfAttached()
        {
        String      sService = m_sMBeanService;
        ManagerRole role     = m_roleMBean;

        if (sService != null && role != null && f_fMBeanAttached.compareAndSet(true, false))
            {
            JournalPersistenceMBeanRegistry.shared(m_registrarMBean).detach(sService, role);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
     * Journal configuration used for newly instantiated stores.
     */
    private PartitionJournalConfig m_journalConfig =
            new PartitionJournalConfig().setMaximumFileSize(DEFAULT_MAX_FILE_SIZE);

    /**
     * Most recent local recovery summary.
     */
    private volatile RecoverySummary m_summaryRecovery = RecoverySummary.EMPTY;

    /**
     * Service name for journal persistence MBean registration, or {@code null}
     * when MBean registration is disabled.
     */
    private String m_sMBeanService;

    /**
     * Manager role for journal persistence MBean registration.
     */
    private ManagerRole m_roleMBean;

    /**
     * Registrar for journal persistence MBean registration.
     */
    private MBeanRegistrar m_registrarMBean;

    /**
     * Guard ensuring MBean attach/detach is once per manager lifecycle.
     */
    private final AtomicBoolean f_fMBeanAttached = new AtomicBoolean();
    }
