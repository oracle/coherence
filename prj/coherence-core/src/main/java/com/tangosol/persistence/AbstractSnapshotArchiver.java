/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.persistence.PersistenceEnvironment;
import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.coherence.persistence.PersistentStore;

import com.oracle.datagrid.persistence.OfflinePersistenceInfo;
import com.oracle.datagrid.persistence.PersistenceStatistics;
import com.oracle.datagrid.persistence.PersistenceTools;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;

import com.tangosol.persistence.bdb.BerkeleyDBEnvironment;

import com.tangosol.util.Base;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * An abstract implementation of a {@link SnapshotArchiver} which must be extended
 * to create a specific implementation.
 *
 * @since 12.2.1
 * @author tam  2014.08.22
 */
public abstract class AbstractSnapshotArchiver
        implements SnapshotArchiver
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new AbstractSnapshotArchiver which is used to archive and
     * retrieve snapshot parts to/from a common archive location.<br>
     * There will be one instance of a AbstractSnapshotArchiver implementation
     * per storage node / per configured Persistence service.
     *
     * @param sClusterName  the name of the cluster
     * @param sServiceName  the service name
     */
    public AbstractSnapshotArchiver(String sClusterName, String sServiceName)
        {
        f_sClusterName = sClusterName;
        f_sServiceName = sServiceName;
        }

    // ----- SnapshotArchiver methods ---------------------------------------

    @Override
    public String[] list()
        {
        return listInternal();
        }

    @Override
    public Snapshot get(String sSnapshot)
        {
        return new Snapshot(sSnapshot, listStoresInternal(sSnapshot));
        }

    @Override
    public synchronized boolean remove(String sSnapshot)
        {
        if (!hasArchivedSnapshot(sSnapshot))
            {
            throw new IllegalArgumentException("The snapshot " + sSnapshot + " does not exist");
            }

        return removeInternal(sSnapshot);
        }

    @Override
    public synchronized void archive(Snapshot snapshot, PersistenceEnvironment<ReadBuffer> env)
        {
        String                         sSnapshot = snapshot.getName();
        PersistenceManager<ReadBuffer> mgr       = null;

        if (!hasLocalSnapshot(env, sSnapshot))
            {
            throw new IllegalArgumentException("The snapshot " + sSnapshot + " does not exist");
            }

        resetStatistics();

        synchronized (env)
            {
            try
                {
                mgr = env.openSnapshot(sSnapshot);

                archiveInternal(snapshot, mgr);
                }
            finally
                {
                if (mgr != null)
                    {
                    mgr.release();
                    }
                }
            }

        displayStatistics(snapshot, "archive");
        }

    @Override
    public synchronized void retrieve(Snapshot snapshot, PersistenceEnvironment<ReadBuffer> env)
        {
        String                         sSnapshotName = snapshot.getName();
        PersistenceManager<ReadBuffer> mgr           = null;

        resetStatistics();

        synchronized (env)
            {
            try
                {
                mgr = env.createSnapshot(sSnapshotName, null);

                retrieveInternal(snapshot, mgr);
                }
            finally
                {
                if (mgr != null)
                    {
                    mgr.release();
                    }
                }
            }

        displayStatistics(snapshot, "retrieve");
        }

    @Override
    public PersistenceTools getPersistenceTools(String sSnapshot)
        {
        // make sure we have the requested snapshot
        if (!hasArchivedSnapshot(sSnapshot))
            {
            throw new IllegalArgumentException("The snapshot " + sSnapshot + " is not known to the archiver " +
                    this.toString());
            }

        try
            {
            // retrieve the metadata for the archived snapshot
            Properties props = getMetadata(sSnapshot);
            if (props == null)
                {
                throw new IllegalArgumentException("Cannot load properties file " +
                                CachePersistenceHelper.META_FILENAME + " for snapshot " + sSnapshot);
                }

            String[] asStores = listStoresInternal(sSnapshot);
            if (asStores.length == 0)
                {
                throw new IllegalArgumentException("The snapshot " + sSnapshot + " has no stores, unable to continue");
                }

            OfflinePersistenceInfo info = new OfflinePersistenceInfo(
                    Integer.valueOf(props.getProperty(CachePersistenceHelper.META_PARTITION_COUNT)),
                    props.getProperty(CachePersistenceHelper.META_STORAGE_FORMAT), true, listStoresInternal(sSnapshot),
                    Integer.valueOf(props.getProperty(CachePersistenceHelper.META_STORAGE_VERSION)),
                    Integer.valueOf(props.getProperty(CachePersistenceHelper.META_IMPL_VERSION)),
                    props.get(CachePersistenceHelper.META_SERVICE_VERSION).toString());

            return instantiatePersistenceTools(info, sSnapshot);
            }
        catch (Exception e)
            {
            throw new RuntimeException("Unable to instantiate persistence tools for snapshot "
                    + sSnapshot + " - " + e.getMessage());
            }
        }

    // ----- AbstractSnapshotArchiver methods -------------------------------

    /**
     * Internal implementation to return the identifiers of the archived
     * snapshots known to this archiver.
     *
     * @return a list of the known archived snapshot identifiers
     */
    protected abstract String[] listInternal();

    /**
     * Internal implementation to Archive the specified snapshot.
     *
     * @param snapshot  the snapshot to archive
     * @param mgr       the PersistenceManager used to read the stores from
     */
    protected abstract void archiveInternal(Snapshot snapshot, PersistenceManager<ReadBuffer> mgr);

    /**
     * Internal implementation to retrieve the specified snapshot.
     *
     * @param snapshot  the snapshot to retrieve
     * @param mgr       the PersistenceManager used to write the stores to
     */
    protected abstract void retrieveInternal(Snapshot snapshot, PersistenceManager<ReadBuffer> mgr);

    /**
     * Internal implementation to remove the specified archived snapshot.
     * (Called by JMX operation removeArchivedSnapshot)
     *
     * @param sSnapshot  the snapshot name to remove
     *
     * @return true if the snapshot was removed
     */
    protected abstract boolean removeInternal(String sSnapshot);

    /**
     * List the stores for a given snapshot.
     *
     * @param sSnapshot the snapshot name to list stores for
     *
     * @return a {@link String}[] of store names
     */
    protected abstract String[] listStoresInternal(String sSnapshot);

    /**
     * Internal implementation to retrieve the metadata stored for the archived
     * snapshot.
     *
     * @param sSnapshot  the snapshot name to retrieve metadata
     *
     * @return the metadata for the archived snapshot
     *
     * @throws java.io.IOException if any I/O related problems
     */
    protected abstract Properties getMetadata(String sSnapshot) throws IOException;

    /**
     * Instantiate an instance of {@link PersistenceTools} relevant to this
     * archiver and the provided snapshot.
     *
     * @param info       the information about this archived snapshot
     * @param sSnapshot  the snapshot name to use
     *
     * @return an instance of PersistenceTools
     */
    protected PersistenceTools instantiatePersistenceTools(OfflinePersistenceInfo info, String sSnapshot)
        {
        return new SnapshotArchiverPersistenceTools(info, sSnapshot);
        }


    // ----- statistics methods ---------------------------------------------

    /**
     * Reset the statistics recording the archive and retrieve times.
     */
    protected void resetStatistics()
        {
        m_cMillisMax   = Long.MIN_VALUE;
        m_cMillisMin   = Long.MAX_VALUE;
        m_cMillisTotal = 0L;
        }

    /**
     * Record the start time of the operation.
     */
    protected void recordStartTime()
        {
        m_cMillisLastStart = Base.getLastSafeTimeMillis();
        }

    /**
     * Record the end time of the operation and update min and max values.
     */
    protected void recordEndTime()
        {
        long cMillisDuration = Base.getLastSafeTimeMillis() - m_cMillisLastStart;

        m_cMillisTotal += cMillisDuration;

        if (cMillisDuration > m_cMillisMax)
            {
            m_cMillisMax = cMillisDuration;
            }

        if (cMillisDuration < m_cMillisMin)
            {
            m_cMillisMin = cMillisDuration;
            }
        }

    /**
     * Display the collected statistics for the given snapshot and type of operation.
     *
     * @param snapshot  the snapshot that was archived or retrieved
     * @param sType     the type of operation, either "archive" or "retrieve"
     */
    protected void displayStatistics(Snapshot snapshot, String sType)
        {
        int cStores = snapshot.listStores().length;

        StringBuilder sb = new StringBuilder("Statistics for ");
        sb.append(sType)
          .append( " of snapshot ")
          .append(snapshot.getName())
          .append(": Number of stores ")
          .append(sType)
          .append("d by this member=")
          .append(cStores)
          .append(", Total time=")
          .append(m_cMillisTotal)
          .append("ms, Average=")
          .append(cStores == 0 ? 0 : m_cMillisTotal / cStores)
          .append("ms, Min=")
          .append(m_cMillisMin)
          .append("ms, Max=")
          .append(m_cMillisMax)
          .append("ms");

        CacheFactory.log(sb.toString(), CacheFactory.LOG_QUIET);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "AbstractSnapshotArchiver(class=" + this.getClass().getCanonicalName() + ", Cluster=" +
                                         f_sClusterName + ", Service=" + f_sServiceName + ")";
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return true if the specified snapshot exists for the PersistenceEnvironment.
     *
     * @param env        the {@link PersistenceEnvironment} to query
     * @param sSnapshot  the snapshot name to check for
     *
     * @return           true if the specified snapshot exists
     */
    protected boolean hasLocalSnapshot(PersistenceEnvironment<ReadBuffer> env, String sSnapshot)
        {
        return containsElement(env.listSnapshots(), sSnapshot);
        }

    /**
     * Return true if the specified snapshot exists for this archiver.
     *
     * @param sSnapshot  the snapshot name to check for
     *
     * @return true if the specified snapshot exists
     */
    protected boolean hasArchivedSnapshot(String sSnapshot)
        {
        return containsElement(list(), sSnapshot);
        }

    /**
     * Return true if the specified {@link String} element exists in the
     * provided {@link String}[].
     *
     * @param asString  the array to look through
     * @param sElement  the element to find
     *
     * @return true if the specified element exists
     */
    private boolean containsElement(String[] asString, String sElement)
        {
        for (int i = 0, c = asString.length; i < c; i++)
            {
            if (sElement != null && sElement.equals(asString[i]))
                {
                return true;
                }
            }

        return false;
        }

    /**
     * Create a temporary PersistenceEnvironment to write archived snapshots to
     * in the given format.
     *
     * @param fileBaseDir     the directory off which to create the environment
     * @param sStorageFormat  the storage format to use
     *
     * @return a PersistenceEnvironment which can be used to write archived snapshots to
     *
     * @throws IOException if any I/O related errors.
     */
    protected PersistenceEnvironment<ReadBuffer> createTempEnvironment(File fileBaseDir, String sStorageFormat)
                throws IOException
        {
        CacheFactory.log("Creating temporary PersistenceEnvironment under " + fileBaseDir.getAbsolutePath()
                         + " using format " + sStorageFormat, CacheFactory.LOG_DEBUG);

        if (sStorageFormat == null || sStorageFormat.isEmpty())
            {
            throw new IllegalArgumentException("Invalid validation format " + sStorageFormat);
            }
        else if (sStorageFormat.equals("BDB"))
            {
            return new BerkeleyDBEnvironment(new File(fileBaseDir, CachePersistenceHelper.DEFAULT_ACTIVE_DIR),
                    new File(fileBaseDir, CachePersistenceHelper.DEFAULT_SNAPSHOT_DIR),
                    new File(fileBaseDir, CachePersistenceHelper.DEFAULT_TRASH_DIR));
            }
        return null;
        }

    /**
     * Write the metadata using given manager for a particular store to a destination directory.
     *
     * @param fileDir  the directory to write metadata to
     * @param mgr      the PersistenceManager used to write the metadata
     * @param sStore   a store to use to read the partition count from
     *
     * @throws java.io.IOException if any errors writing metadata
     */
    protected void writeMetadata(File fileDir, PersistenceManager<ReadBuffer> mgr, String sStore)
            throws IOException
        {
        Properties                  props = new Properties();
        PersistentStore<ReadBuffer> store = null;
        try
            {
            AbstractPersistenceManager abstractMgr = (AbstractPersistenceManager) SafePersistenceWrappers.unwrap(mgr);
            store = mgr.open(sStore, null);

            props.setProperty(CachePersistenceHelper.META_IMPL_VERSION, String.valueOf(abstractMgr.getImplVersion()));
            props.setProperty(CachePersistenceHelper.META_STORAGE_VERSION, String.valueOf(abstractMgr.getStorageVersion()));
            props.setProperty(CachePersistenceHelper.META_STORAGE_FORMAT, String.valueOf(abstractMgr.getStorageFormat()));
            props.setProperty(CachePersistenceHelper.META_PARTITION_COUNT, String.valueOf(CachePersistenceHelper.getPartitionCount(store)));
            props.setProperty(CachePersistenceHelper.META_SERVICE_VERSION, String.valueOf(CachePersistenceHelper.getServiceVersion(store)));

            CachePersistenceHelper.writeMetadata(fileDir, props);
            }
        finally
            {
            if (store != null)
                {
                mgr.close(sStore);
                }
            }
        }

    // ----- inner class: SnapshotArchiverPersistenceTools ------------------

    /**
     * An implementation of PersistenceTools specifically for archived snapshots.
     */
    private class SnapshotArchiverPersistenceTools
                extends AbstractPersistenceTools
        {
        // ------ constructors --------------------------------------------------

        /**
         * Constructs a new instance of the tools for use with archived snapshots.
         *
         * @param info       the information collected about the archived snapshot
         * @param sSnapshot  the snapshot to run the tools on
         */
        public SnapshotArchiverPersistenceTools(OfflinePersistenceInfo info, String sSnapshot)
            {
            super(info);
            f_sSnapshot = sSnapshot;
            }

        // ----- PersistenceTools methods ---------------------------------------

        @Override
        public void validate()
            {
            // to validate archived snapshots, we retrieve all the parts and
            // iterate through without collecting stats
            validateArchivedSnapshot(false);
            }

        @Override
        public PersistenceStatistics getStatistics()
            {
            return validateArchivedSnapshot(true);
            }

        // ----- helpers --------------------------------------------------------

        /**
         * Validate and archived snapshot by retrieving each of the stores and
         * asking a PersistenceManager to instantiate them.  If fCollectStats is
         * true then also collect stats.
         *
         * @param fCollectStats  true if we want to collect stats
         *
         * @return the statistics for the archived snapshot
         */
        protected PersistenceStatistics validateArchivedSnapshot(boolean fCollectStats)
            {
            PersistentStore<ReadBuffer>        store;
            PersistenceStatistics              stats   = null;
            StatsVisitor                       visitor = null;
            PersistenceEnvironment<ReadBuffer> env     = null;
            PersistenceManager<ReadBuffer>     manager = null;
            File                               dirTemp = null;

            if (fCollectStats)
                {
                stats   = new PersistenceStatistics();
                visitor = new StatsVisitor(stats);
                }

            Snapshot snapshotAllStores = AbstractSnapshotArchiver.this.get(f_sSnapshot);

            if (snapshotAllStores == null)
                {
                throw new IllegalArgumentException("Snapshot " + f_sSnapshot + " is not known to this archiver");
                }

            String[] asStores = snapshotAllStores.listStores();

            // if the archived snapshot is missing files then raise exception early
            if (!f_info.isComplete())
                {
                throw new RuntimeException("The archived snapshot is not complete. Number of stores is " + asStores.length +
                        " but number of partitions is " + f_info.getPartitionCount());
                }

            try
                {
                // retrieve the storage format
                Properties props          = getMetadata(f_sSnapshot);
                String     sStorageFormat = props.getProperty(CachePersistenceHelper.META_STORAGE_FORMAT);

                // create a temporary environment to retrieve snapshot into
                dirTemp = FileHelper.createTempDir();
                env     = createTempEnvironment(dirTemp, sStorageFormat);

                // page through the stores retrieving a store at a time to reduce
                // the overhead; this could be run in parallel to reduce the time to
                // validate at the cost of increased resource usage (memory & cpu)
                for (String sStore : asStores)
                    {
                    // create a new snapshot with just one store as we want to minimize
                    // disk usage.
                    Snapshot snapshot = new Snapshot(f_sSnapshot, new String[] {sStore});

                    AbstractSnapshotArchiver.this.retrieve(snapshot, env);

                    manager = env.openSnapshot(f_sSnapshot);
                    store   = manager.open(sStore, null);

                    if (fCollectStats)
                        {
                        visitor.setCaches(CachePersistenceHelper.getCacheNames(store));
                        store.iterate(CachePersistenceHelper.instantiatePersistenceVisitor(visitor));
                        }

                    manager.close(sStore);

                    // we must remove the snapshot every iteration as the retrieve assumes
                    // the snapshot does not exist, even if we are just retrieving a store
                    env.removeSnapshot(f_sSnapshot);
                    }

                }
            catch (IOException ioe)
                {
                throw CachePersistenceHelper.ensurePersistenceException(ioe, "Unable to create temporary directory");
                }
            finally
                {
                if (manager != null)
                    {
                    manager.release();
                    }

                if (env != null)
                    {
                    env.release();
                    }

                if (dirTemp != null)
                    {
                    FileHelper.deleteDirSilent(dirTemp);
                    }
                }

            return stats;
            }

        // ----- data members -----------------------------------------------

        /**
         * Snapshot that we are validating.
         */
        protected final String f_sSnapshot;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The cluster name.
     */
    protected final String f_sClusterName;

    /**
     * The service name.
     */
    protected final String f_sServiceName;

    /**
     * The total time in millis taken to archive or retrieve snapshot stores.
     */
    protected long m_cMillisTotal = 0L;

    /**
     * The maxiumum time in millis to archive or retrieve a snapshot store.
     */
    protected long m_cMillisMax = Long.MIN_VALUE;

    /**
     * The minimum time in millis to archive or retrieve a snapshot store.
     */
    protected long m_cMillisMin = Long.MAX_VALUE;

    /**
     * The start time of the last operation.
     */
    protected long m_cMillisLastStart = -1L;
    }
