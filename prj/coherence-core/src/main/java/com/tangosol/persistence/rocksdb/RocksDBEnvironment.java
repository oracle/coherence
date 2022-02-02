package com.tangosol.persistence.rocksdb;

import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistenceManager;
import com.tangosol.io.ReadBuffer;
import com.tangosol.persistence.AbstractPersistenceEnvironment;
import com.tangosol.persistence.AbstractPersistenceManager;
import java.io.File;
import java.io.IOException;

/**
 * PersistenceEnvironment implementation that uses RocksDB.
 *
 * @author mg  2022.01.21
 */
public class RocksDBEnvironment
        extends AbstractPersistenceEnvironment
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new BerkeleyDBEnvironment that manages a singleton
     * BerkeleyDBManager with the specified directories:
     * <ol>
     *     <li>data - active persistence</li>
     *     <li>snapshot - location for snapshots</li>
     *     <li>trash - optional location trashed stores</li>
     * </ol>
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
    public RocksDBEnvironment(File fileActive, File fileSnapshot, File fileTrash)
            throws IOException
        {
        this(fileActive, fileSnapshot, fileTrash, null);
        }

    /**
     * Create a new BerkeleyDBEnvironment that manages a singleton
     * BerkeleyDBManager with the specified directories:
     * <ol>
     *     <li>data - active persistence</li>
     *     <li>snapshot - location for snapshots</li>
     *     <li>trash - optional location trashed stores</li>
     *     <li>events - optional location for event storage</li>
     * </ol>
     *
     * @param fileActive    the data directory of the singleton active
     *                      manager or null if an active manager shouldn't
     *                      be maintained by this environment
     * @param fileSnapshot  the snapshot directory
     * @param fileTrash     an optional trash directory used for "safe"
     *                      deletes
     * @param fileEvents    an optional events directory used for to store
     *                      map events
     *
     * @throws IOException if the data directory could not be created
     *
     * @throws IllegalArgumentException if the data, snapshot, and trash
     *         directories are not unique
     */
    public RocksDBEnvironment(File fileActive, File fileSnapshot, File fileTrash, File fileEvents)
            throws IOException
        {
        super(fileActive, fileSnapshot, fileTrash, fileEvents);
        }

    // ----- AbstractPersistenceEnvironment methods -------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractPersistenceManager openActiveInternal()
        {
        try
            {
            return new RocksDBManager(getPersistenceActiveDirectory(),
                                      getPersistenceTrashDirectory(), null);
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
    protected AbstractPersistenceManager openEventsInternal()
        {
        try
            {
            return new RocksDBManager(getPersistenceEventsDirectory(),
                                      getPersistenceTrashDirectory(), null);
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
    protected AbstractPersistenceManager openSnapshotInternal(File fileSnapshot,
                                                              String sSnapshot)
        {
        try
            {
            return new RocksDBManager(fileSnapshot, null, sSnapshot);
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
    protected AbstractPersistenceManager createSnapshotInternal(final File fileSnapshot,
                                                                String sSnapshot, PersistenceManager<ReadBuffer> manager)
        {
        if (manager != null && !(manager instanceof RocksDBManager))
            {
            throw new IllegalArgumentException("incompatible persistence manager type: "
                                               + manager.getClass());
            }

        // create a new snapshot
        RocksDBManager snapshot = (RocksDBManager) openSnapshotInternal(
                fileSnapshot, sSnapshot);
        if (manager instanceof RocksDBManager)
            {
            try
                {
                ((RocksDBManager) manager).createSnapshot(fileSnapshot);
                }
            catch (PersistenceException e)
                {
                snapshot.release();
                throw e;
                }
            }

        return snapshot;
        }
    }