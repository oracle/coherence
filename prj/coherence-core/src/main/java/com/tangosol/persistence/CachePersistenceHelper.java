/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.persistence.FatalAccessException;
import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistentStore;

import com.oracle.datagrid.persistence.PersistenceTools;

import com.tangosol.coherence.config.builder.PersistenceEnvironmentParamBuilder;
import com.tangosol.coherence.config.scheme.AbstractServiceScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.internal.net.cluster.ClusterDependencies;
import com.tangosol.internal.net.service.ServiceDependencies;
import com.tangosol.internal.net.service.grid.PartitionedServiceDependencies;
import com.tangosol.internal.net.service.grid.PersistenceDependencies;

import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.FileHelper;
import com.tangosol.io.MultiBufferReadBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.io.Serializer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.WriteBuffer.BufferOutput;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.internal.QuorumInfo;

import com.tangosol.net.management.Registry;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.persistence.bdb.BerkeleyDBManager;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.LongArray;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SparseArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Properties;

import static com.tangosol.util.ExternalizableHelper.toBinary;

/**
 * Static helper methods used in the persistence of a partitioned cache.
 *
 * @author rhl 2012.08.15
 */
public class CachePersistenceHelper
    {

    // ----- exception helpers ----------------------------------------------

    /**
     * Return a PersistenceException with the given cause. If the specified
     * cause is an instance of PersistenceException, the given throwable will
     * be returned as is; otherwise, a new PersistenceException will be
     * allocated and returned.
     *
     * @param eCause an optional cause
     *
     * @return a PersistenceException with the given cause
     */
    public static PersistenceException ensurePersistenceException(Throwable eCause)
        {
        return ensurePersistenceException(eCause, null);
        }

    /**
     * Return a PersistenceException with the given cause and detail message.
     * If the specified cause is an instance of PersistenceException and the
     * detail message is null, the given throwable will be returned as is;
     * otherwise, a new PersistenceException will be allocated and returned.
     *
     * @param eCause   an optional cause
     * @param sMessage an optional detail message
     *
     * @return a PersistenceException with the given cause and detail message
     */
    public static PersistenceException ensurePersistenceException(Throwable eCause, String sMessage)
        {
        PersistenceException e;
        if (sMessage == null && eCause instanceof PersistenceException)
            {
            e = (PersistenceException) eCause;
            }
        else if (sMessage == null && eCause == null)
            {
            e = new PersistenceException();
            }
        else if (eCause == null)
            {
            e = new PersistenceException(sMessage);
            }
        else if (sMessage == null)
            {
            e = new PersistenceException(eCause);
            }
        else
            {
            e = new PersistenceException(sMessage, eCause);
            }

        return e;
        }

    // ----- cache metadata support -----------------------------------------

    /**
     * Seal the specified PersistentStore on behalf of the specified service,
     * indicating that it is fully initialized and eligible to be recovered
     * from this point forward.
     *
     * @param store    the persistent store to seal
     * @param service  the partitioned service
     * @param oToken   batch token to use for the seal operation, or null
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void seal(PersistentStore<ReadBuffer> store,
                            PartitionedService service, Object oToken)
        {
        store.ensureExtent(META_EXTENT);

        boolean fCommit = false;
        if (oToken == null)
            {
            oToken  = store.begin();
            fCommit = true;
            }

        Binary binPartsCount = toBinary(service.getPartitionCount(), serializer());

        // prior to 12.2.1.1.0 the version was serialized as a string;
        // need to maintain it for backward compatibility
        Binary binServiceVersion = toBinary(String.valueOf(PERSISTENCE_VERSION), serializer());

        store.store(META_EXTENT, BINARY_PARTITION_COUNT, binPartsCount, oToken);
        store.store(META_EXTENT, BINARY_PERSISTENCE_VERSION, binServiceVersion, oToken);
        store.store(META_EXTENT, BINARY_SEAL, BINARY_SEAL, oToken);

        if (fCommit)
            {
            store.commit(oToken);
            }
        }

    /**
     * Remove a seal from a PersistentStore.
     *
     * @param store  the {@link PersistentStore store} the seal should be
     *               removed from
     */
    public static void unseal(PersistentStore<ReadBuffer> store)
        {
        Object oToken = store.begin();

        store.erase(META_EXTENT, BINARY_PARTITION_COUNT, oToken);
        store.erase(META_EXTENT, BINARY_PERSISTENCE_VERSION, oToken);
        store.erase(META_EXTENT, BINARY_SEAL, oToken);

        store.commit(oToken);
        }

    /**
     * Return true if the specified store has been sealed.
     *
     * @param store  the persistent store to check
     *
     * @return true if the specified store has been sealed
     */
    public static boolean isSealed(PersistentStore<ReadBuffer> store)
        {
        return store.load(META_EXTENT, BINARY_SEAL) != null;
        }

    /**
     * Write the current membership information in raw format to the "META" extent.
     *
     * @param store    the store to write into
     * @param binInfo  the Binary object to write to the "META" extent
     */
    public static void writeQuorumRaw(PersistentStore<ReadBuffer> store, Binary binInfo)
        {
        store.ensureExtent(META_EXTENT);

        store.store(META_EXTENT, BINARY_QUORUM, binInfo, /*oToken*/ null);
        }

    /**
     * Write the current membership information to the "META" extent.
     *
     * Note: this method also {@link #seal seals} the store.
     *
     * @param store    the store to write into
     * @param service  the service for which the information is stored
     *
     * @return the Binary object that was written to the "META" extent
     */
    public static Binary writeQuorum(PersistentStore<ReadBuffer> store, PartitionedService service)
        {
        QuorumInfo info    = new QuorumInfo(service);
        Binary     binInfo = ExternalizableHelper.toBinary(info, serializer());

        writeQuorumRaw(store, binInfo);

        seal(store, service, null);

        return binInfo;
        }

    /**
     * Read the membership information in Binary format from the "META" extent.
     *
     * @param store  the store to read from
     *
     * @return the membership information in a Binary format
     */
    public static Binary readQuorumRaw(PersistentStore<ReadBuffer> store)
        {
        store.ensureExtent(META_EXTENT);

        ReadBuffer bufMembers = store.load(META_EXTENT, BINARY_QUORUM);

        return bufMembers == null ? null : bufMembers.toBinary();
        }

    /**
     * Read the membership information from the "META" extent.
     *
     * @param store  the store to read from
     *
     * @return the membership information
     */
    public static QuorumInfo readQuorum(PersistentStore<ReadBuffer> store)
        {
        Binary binMembers = readQuorumRaw(store);

        return binMembers == null ? null :
            (QuorumInfo) ExternalizableHelper.fromBinary(
                binMembers.toBinary(), serializer());
        }

    /**
     * Validate the specified store to check that it has been sealed, indicating
     * that it is eligible to be recovered by the specified service.
     * <p>
     * Successful validation is determined by this method not appending to the
     * provided StringBuilder. Failure to validate the store results in a
     * description of the validation being appended to the provided StringBuilder.
     *
     * @param store    the persistent store to validate
     * @param service  the partitioned service
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void validate(PersistentStore store, PartitionedService service)
        {
        StringBuilder sb = new StringBuilder();
        try
            {
            String sConjunction  = "";
            int    cParts        = getPartitionCount(store);
            int    cPartsService = service.getPartitionCount();

            if (cParts != cPartsService)
                {
                sb.append("partition-count mismatch ").append(cParts).append("(persisted)")
                  .append(" != ").append(cPartsService).append("(service)");

                sConjunction = " and ";
                }
            if (!isSealed(store))
                {
                sb.append(sConjunction).append("store has not been sealed");
                }
            }
        catch (RuntimeException e)
            {
            Throwable tCause = e instanceof PersistenceException
                    ? e.getCause() : e;

            if (tCause instanceof IllegalArgumentException)
                {
                // missing meta extent thus throw a FatalAccessException (deleting
                // the store) and avoid a service restart
                sb.append("missing internal extent");
                }
            else
                {
                throw e;
                }
            }

        if (sb.length() > 0)
            {
            throw ensurePersistenceException(new FatalAccessException(sb.toString()));
            }
        }

    /**
     * Persist the specified cache names in the persistent store.
     *
     * @param store     the persistent store to store the cache names to
     * @param laCaches  the LongArray of cache names, indexed by cache-id
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void storeCacheNames(PersistentStore store, LongArray laCaches)
        {
        int          cCaches = laCaches.getSize();
        WriteBuffer  buf     = new ByteArrayWriteBuffer(cCaches * 100);
        BufferOutput out     = buf.getBufferOutput();

        try
            {
            out.writeInt(cCaches);
            for (LongArray.Iterator iter = laCaches.iterator(); iter.hasNext(); )
                {
                String sCache   = (String) iter.next();
                long   lCacheId = iter.getIndex();

                out.writeLong(lCacheId);
                out.writeSafeUTF(sCache);
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        store.ensureExtent(META_EXTENT);
        store.store(META_EXTENT, BINARY_CACHES, buf.getReadBuffer(), /*oToken*/ null);
        }

    /**
     * Return the cache names that have been {@link #storeCacheNames stored}
     * in the specified store.
     *
     * @param store  the persistent store to load the cache names from
     *
     * @return a LongArray of cache names, indexed by the cache-id
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static LongArray<String> getCacheNames(PersistentStore<ReadBuffer> store)
        {
        ReadBuffer        bufVal   = store.load(META_EXTENT, BINARY_CACHES);
        LongArray<String> laCaches = new SparseArray<>();

        if (bufVal != null)
            {
            try
                {
                BufferInput in = bufVal.getBufferInput();
                int         c  = in.readInt();

                for (int i = 0; i < c; i++)
                    {
                    long   lCacheId = in.readLong();
                    String sCache   = in.readSafeUTF();

                    laCaches.set(lCacheId, sCache);
                    }
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }

        return laCaches;
        }

    /**
     * Return the partition count that has been stored in the specified store.
     *
     * @param store  the persistent store to load the partition count from
     *
     * @return the partition count or -1 if the specified store does not contain
     *         a partition count
     */
    public static int getPartitionCount(PersistentStore<ReadBuffer> store)
        {
        ReadBuffer bufPartsCount = store.load(META_EXTENT, BINARY_PARTITION_COUNT);
        return bufPartsCount == null ? -1 :
            (Integer) ExternalizableHelper.fromBinary(bufPartsCount.toBinary(), serializer());
        }

    /**
     * Return the service version that has been stored in the specified store.
     *
     * @param store  the persistent store to load the service version from
     *
     * @return the service version or <tt>"&lt;none&gt;</tt> if the specified
     *         store does not contain a service version
     *
     * @deprecated use {@link #getPersistenceVersion(PersistentStore)} instead
     */
    @Deprecated
    public static String getServiceVersion(PersistentStore<ReadBuffer> store)
        {
        int nVersion = getPersistenceVersion(store);
        return nVersion == 0 ? "<none>" : String.valueOf(nVersion);
        }

    /**
     * Return the persistence version the provided store was written with.
     *
     * @param store  the persistent store to load the persistence version from
     *
     * @return the persistence version or {@code 0} if the specified
     *         store does not contain a persistence version
     */
    public static int getPersistenceVersion(PersistentStore<ReadBuffer> store)
        {
        ReadBuffer bufVersion = store.load(META_EXTENT, BINARY_PERSISTENCE_VERSION);

        int nVersion = 0;
        if (bufVersion != null)
            {
            Object oVersion = ExternalizableHelper.fromBinary(bufVersion.toBinary(), serializer());
            try
                {
                nVersion = oVersion instanceof Integer
                    ? ((Integer) oVersion).intValue()
                    : Integer.parseInt((String) oVersion);
                }
            catch (NumberFormatException ignore) {}
            }

        return nVersion;
        }

    /**
     * Return true iff the specified partition-id is to be used to persist
     * (meta-)data that is logically "global".
     * <p>
     * In an ideal implementation, these (meta)-data would belong to a separate
     * partitioning-scheme of a single partition (and many replicas), however
     * today we "fake it" by artificially assigning them to a single well-known
     * partition.
     *
     * @param nPartition  the partition-id
     *
     * @return true iff the specified partition is to be used to persist global
     *              meta-data
     */
    public static boolean isGlobalPartitioningSchemePID(int nPartition)
        {
        // arbitrarily use PID 0, with no replicas
        return nPartition == 0;
        }

    /**
     * Return a PartitionSet that contains partitions to be used to persist
     * (meta-)data that is logically "global".
     *
     * @param service  the service for which the global partitions are requested
     *
     * @return the PartitionSet containing {@link #isGlobalPartitioningSchemePID
     *         global partition ids}
     */
    public static PartitionSet getGlobalPartitions(PartitionedService service)
        {
        // arbitrarily use PID 0, with no replicas
        PartitionSet parts =  new PartitionSet(service.getPartitionCount());
        parts.add(0);
        return parts;
        }

    /**
     * Delete the provided extent from the specified {@link PersistentStore
     * store} and all associated extents (meta extents).
     *
     * @param store      the PersistenceStore
     * @param lExtentId  the extent id
     */
    public static void deleteExtents(PersistentStore store, long lExtentId)
        {
        long lDelExtentId = lExtentId;

        for (int i = 0; i <= RESERVED_META_EXTENTS; ++i)
            {
            store.deleteExtent(lDelExtentId);

            lDelExtentId = -lExtentId - i;
            }
        }

    /**
     * Move the old extent to the new extent in the specified {@link PersistentStore
     * store} including all associated extents (meta extents).
     *
     * @param store         the PersistenceStore
     * @param lOldExtentId  the old extent id
     * @param lNewExtentId  the new extent id
     */
    public static void moveExtents(PersistentStore store, long lOldExtentId, long lNewExtentId)
        {
        long lSrcExtent  = lOldExtentId;
        long lDestExtent = lNewExtentId;

        for (int i = 0; i <= RESERVED_META_EXTENTS; ++i)
            {
            store.moveExtent(lSrcExtent, lDestExtent);

            lSrcExtent  = -lOldExtentId - i;
            lDestExtent = -lNewExtentId - i;
            }
        }

    // ----- persistence metadata support -----------------------------------

    /**
     * Read persistence metadata from the specified directory.
     *
     * @param fileDir  the directory to read metadata from
     *
     * @return the metadata
     *
     * @throws IOException on error reading the metadata file
     */
    public static Properties readMetadata(File fileDir)
            throws IOException
        {
        FileInputStream in = new FileInputStream(new File(fileDir, META_FILENAME));
        try
            {
            Properties prop = new Properties();
            prop.load(in);
            return prop;
            }
        finally
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
        }

    /**
     * Write persistence metadata to the specified directory.
     *
     * @param fileDir  the directory to write metadata to
     * @param prop     the metadata to write
     *
     * @throws IOException on error writing the metadata file
     */
    public static void writeMetadata(File fileDir, Properties prop)
            throws IOException
        {
        // validate that the metadata is complete
        if (!isMetadataComplete(prop))
            {
            throw new IllegalArgumentException("incomplete metadata");
            }

        FileOutputStream out = new FileOutputStream(new File(fileDir, META_FILENAME));
        try
            {
            prop.store(out, null);
            }
        finally
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

    /**
     * Copy the metadata file from one directory to another.
     *
     * @param fileDirFrom  the directory to copy from
     * @param fileDirTo    the directory to copy to
     *
     * @throws IOException on copying the metadata
     */
    public static void copyMetadata(File fileDirFrom, File fileDirTo)
            throws IOException
        {
        FileHelper.copyFile(new File(fileDirFrom, CachePersistenceHelper.META_FILENAME),
                            new File(fileDirTo, CachePersistenceHelper.META_FILENAME));
        }

    /**
     * Determine if the given metadata in the {@link Properties} is complete.
     *
     * @param prop  the metadata to analyze
     *
     * @return true if the given metadata is complete; false otherwise
     */
    public static boolean isMetadataComplete(Properties prop)
        {
        return prop != null                                                 &&
               !prop.getProperty(META_IMPL_VERSION,    "").trim().isEmpty() &&
               !prop.getProperty(META_STORAGE_FORMAT,  "").trim().isEmpty() &&
               !prop.getProperty(META_STORAGE_VERSION, "").trim().isEmpty();
        }

    /**
     * Determine if the given metadata in the {@link Properties} is compatible
     * with the metadata supplied.
     *
     * @param prop             the metadata to analyze
     * @param nImplVersion     the impl version to compare
     * @param sStorageFormat   the storage format to compare
     * @param nStorageVersion  the storage version to compare
     *
     * @return true if the given metadata is compatible; false otherwise
     */
    public static boolean isMetadataCompatible(Properties prop, int nImplVersion,
            String sStorageFormat, int nStorageVersion)
        {
        // parse implementation version
        int nImplVersionExpect;
        try
            {
            nImplVersionExpect = Integer.valueOf(prop.getProperty(META_IMPL_VERSION));
            }
        catch (RuntimeException e)
            {
            nImplVersionExpect = -1;
            }

        // parse storage format
        String sStorageFormatExpect = prop.getProperty(META_STORAGE_FORMAT);

        // parse storage version
        int nStorageVersionExpect;
        try
            {
            nStorageVersionExpect = Integer.valueOf(prop.getProperty(META_STORAGE_VERSION));
            }
            catch (RuntimeException e)
            {
            nStorageVersionExpect = -1;
            }

        return nImplVersionExpect    <= nImplVersion    &&
               nStorageVersionExpect == nStorageVersion &&
               sStorageFormatExpect.equals(sStorageFormat);
        }

    // ----- listener support -----------------------------------------------

    /**
     * Return the extent identifier that contains listener registration
     * information for the cache with the given identifier.
     *
     * @param lCacheId  the cacheId
     *
     * @return the listener extent identifier
     */
    protected static long getListenerExtentId(long lCacheId)
        {
        assert lCacheId > 0L;
        return -lCacheId;
        }

    /**
     * Create a key representing a cache listener registration.
     *
     * @param lMemberId  the unique service-identifier of the listening member
     * @param lCacheId   the cache-id
     * @param binKey     the key to listen to
     *
     * @return a ReadBuffer representing the listener registration
     */
    protected static ReadBuffer createListenerRegistrationKey(long lMemberId, long lCacheId, Binary binKey)
        {
        WriteBuffer buf = new ByteArrayWriteBuffer(17);
        try
            {
            BufferOutput out = buf.getBufferOutput();

            out.writeByte(KEY_TYPE_LISTENER);
            out.writeLong(lMemberId);
            out.writeLong(lCacheId);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        return new MultiBufferReadBuffer(new ReadBuffer[] { buf.getReadBuffer(), binKey });
        }

    /**
     * Store the listener registration in the specified persistent store.
     *
     * @param store        the PersistentStore to store the listener registration
     * @param lCacheId     the cache-id
     * @param binKey       the key to listen on
     * @param lListenerId  the unique service-identifier of the listening member
     * @param fLite        true iff the listener expects "lite" events
     * @param oToken       batch token to use for the store operation, or null
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void registerListener(PersistentStore<ReadBuffer> store, long lCacheId,
                                        Binary binKey, long lListenerId, boolean fLite, Object oToken)
        {
        ReadBuffer bufListenerKey = createListenerRegistrationKey(lListenerId, lCacheId, binKey);

        long lExtentId = getListenerExtentId(lCacheId);
        store.ensureExtent(lExtentId);
        store.store(lExtentId, bufListenerKey, fLite ? BINARY_TRUE : BINARY_FALSE, oToken);
        }

    /**
     * Clear the listener registration from the specified persistent store.
     *
     * @param store        the persistent store
     * @param lCacheId     the cache-id
     * @param binKey       the key
     * @param lListenerId  the unique service-identifier of the listening member
     * @param oToken       batch token to use for the store operation, or null
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void unregisterListener(PersistentStore<ReadBuffer> store, long lCacheId,
                                          Binary binKey, long lListenerId, Object oToken)
        {
        ReadBuffer bufListenerKey = createListenerRegistrationKey(lListenerId, lCacheId, binKey);

        long lExtentId = getListenerExtentId(lCacheId);
        store.ensureExtent(lExtentId);
        store.erase(lExtentId, bufListenerKey, oToken);
        }

    /**
     * Clear all listener registrations for the specified cache from the
     * specified persistent store.
     *
     * @param store     the persistent store
     * @param lCacheId  the cache-id
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void unregisterListeners(PersistentStore<ReadBuffer> store, long lCacheId)
        {
        store.deleteExtent(getListenerExtentId(lCacheId));
        }

    /**
     * Return a DefaultSerializer, regardless of what the calling service is using.
     */
    private static Serializer serializer()
        {
        return ExternalizableHelper.ensureSerializer(NullImplementation.getClassLoader());
        }

    // ----- lock support ---------------------------------------------------

    /**
     * Return the extent identifier that contains lock registration
     * information for the cache with the given identifier.
     *
     * @param lCacheId  the cacheId
     *
     * @return the lock extent identifier
     */
    protected static long getLockExtentId(long lCacheId)
        {
        assert lCacheId > 0L;
        return -lCacheId - 1L;
        }

    /**
     * Create a key representing a cache entry lock.
     *
     * @param lHolderId        the unique service-identifier of the listening member
     * @param lHolderThreadId  the thread-id of the lock holder
     * @param lCacheId         the cache-id
     * @param binKey           the locked key
     *
     * @return a ReadBuffer representing the cache entry lock
     */
    protected static ReadBuffer createLockRegistrationKey(long lHolderId, long lHolderThreadId, long lCacheId, Binary binKey)
        {
        WriteBuffer buf = new ByteArrayWriteBuffer(25);
        try
            {
            BufferOutput out = buf.getBufferOutput();

            out.writeByte(KEY_TYPE_LOCK);
            out.writeLong(lHolderId);
            out.writeLong(lHolderThreadId);
            out.writeLong(lCacheId);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        return new MultiBufferReadBuffer(new ReadBuffer[] { buf.getReadBuffer(), binKey });
        }

    /**
     * Store the cache entry lock in the specified persistent store.
     *
     * @param store            the PersistentStore to store the lock
     * @param lCacheId         the cache-id
     * @param binKey           the locked key
     * @param lHolderId        the unique service-identifier of the lock holder
     * @param lHolderThreadId  the thread-id of the lock holder
     * @param oToken           batch token to use for the store operation, or null
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void registerLock(PersistentStore<ReadBuffer> store, long lCacheId,
                                    Binary binKey, long lHolderId, long lHolderThreadId,
                                    Object oToken)
        {
        ReadBuffer bufHolderKey = createLockRegistrationKey(lHolderId, lHolderThreadId, lCacheId, binKey);

        long lExtentId = getLockExtentId(lCacheId);
        store.ensureExtent(lExtentId);
        store.store(lExtentId, bufHolderKey, BINARY_TRUE, oToken);
        }

    /**
     * Clear the cache entry lock from the specified persistent store.
     *
     * @param store            the persistent store
     * @param lCacheId         the cache-id
     * @param binKey           the key
     * @param lHolderId        the unique service-identifier of the lock holder
     * @param lHolderThreadId  the thread-id of the lock holder
     * @param oToken           batch token to use for the store operation, or null
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void unregisterLock(PersistentStore<ReadBuffer> store, long lCacheId,
                                      Binary binKey, long lHolderId, long lHolderThreadId,
                                      Object oToken)
        {
        ReadBuffer bufHolderKey = createLockRegistrationKey(lHolderId, lHolderThreadId, lCacheId, binKey);

        long lExtentId = getLockExtentId(lCacheId);
        store.ensureExtent(lExtentId);
        store.erase(lExtentId, bufHolderKey, oToken);
        }

    /**
     * Clear all cache entry locks for the specified cache from the specified
     * persistent store.
     *
     * @param store     the persistent store
     * @param lCacheId  the cache-id
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void unregisterLocks(PersistentStore<ReadBuffer> store, long lCacheId)
        {
        store.deleteExtent(getLockExtentId(lCacheId));
        }

    // ----- index support --------------------------------------------------

    /**
     * Return the extent identifier that contains index registration
     * information for the cache with the given identifier.
     *
     * @param lCacheId  the cacheId
     *
     * @return the index extent identifier
     */
    protected static long getIndexExtentId(long lCacheId)
        {
        assert lCacheId > 0L;
        return -lCacheId - 2;
        }

    /**
     * Create a key representing an index registration.
     *
     * @param lCacheId      the cache-id
     * @param binExtractor  the index extractor
     *
     * @return a ReadBuffer representing the index registration
     */
    protected static ReadBuffer createIndexRegistrationKey(long lCacheId, Binary binExtractor)
        {
        WriteBuffer buf = new ByteArrayWriteBuffer(9);
        try
            {
            BufferOutput out = buf.getBufferOutput();

            out.writeByte(KEY_TYPE_INDEX);
            out.writeLong(lCacheId);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        return new MultiBufferReadBuffer(new ReadBuffer[] { buf.getReadBuffer(), binExtractor });
        }

    /**
     * Store the index registration in the specified persistent store.
     *
     * @param store          the persistent store
     * @param lCacheId       the cache id
     * @param binExtractor   the index extractor
     * @param binComparator  the index comparator
     * @param oToken         batch token to use for the store operation, or null
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void registerIndex(PersistentStore<ReadBuffer> store, long lCacheId,
                                     Binary binExtractor, Binary binComparator,
                                     Object oToken)
        {
        ReadBuffer bufIndex = createIndexRegistrationKey(lCacheId, binExtractor);

        long lExtentId = getIndexExtentId(lCacheId);
        store.ensureExtent(lExtentId);
        store.store(lExtentId, bufIndex, binComparator, oToken);
        }

    /**
     * Clear the index registration from the specified persistent store.
     *
     * @param store          the persistent store
     * @param lCacheId       the cache id
     * @param binExtractor   the index extractor
     * @param oToken         batch token to use for the store operation, or null
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void unregisterIndex(PersistentStore<ReadBuffer> store, long lCacheId,
                                       Binary binExtractor, Object oToken)
        {
        ReadBuffer bufIndex = createIndexRegistrationKey(lCacheId, binExtractor);

        long lExtentId = getIndexExtentId(lCacheId);
        store.ensureExtent(lExtentId);
        store.erase(lExtentId, bufIndex, oToken);
        }

    /**
     * Clear all index registrations for the specified cache from the specified
     * persistent store.
     *
     * @param store     the persistent store
     * @param lCacheId  the cache-id
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void unregisterIndices(PersistentStore<ReadBuffer> store, long lCacheId)
        {
        store.deleteExtent(getIndexExtentId(lCacheId));
        }

    // ----- trigger support --------------------------------------------------

    /**
     * Return the extent identifier that contains trigger registration
     * information for the cache with the given identifier.
     *
     * @param lCacheId  the cacheId
     *
     * @return the trigger extent identifier
     */
    protected static long getTriggerExtentId(long lCacheId)
        {
        assert lCacheId > 0L;
        return -lCacheId - 3;
        }

    /**
     * Create a key representing an trigger registration.
     *
     * @param lCacheId    the cache-id
     * @param binTrigger  the trigger
     *
     * @return a ReadBuffer representing the trigger registration
     */
    protected static ReadBuffer createTriggerRegistrationKey(long lCacheId, Binary binTrigger)
        {
        WriteBuffer buf = new ByteArrayWriteBuffer(9);
        try
            {
            BufferOutput out = buf.getBufferOutput();

            out.writeByte(KEY_TYPE_TRIGGER);
            out.writeLong(lCacheId);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        return new MultiBufferReadBuffer(new ReadBuffer[] { buf.getReadBuffer(), binTrigger });
        }

    /**
     * Store the trigger registration in the specified persistent store.
     *
     * @param store       the persistent store
     * @param lCacheId    the cache id
     * @param binTrigger  the trigger
     * @param oToken      batch token to use for the store operation, or null
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void registerTrigger(PersistentStore<ReadBuffer> store, long lCacheId,
                                       Binary binTrigger, Object oToken)
        {
        ReadBuffer bufTrigger = createTriggerRegistrationKey(lCacheId, binTrigger);

        long lExtentId = getTriggerExtentId(lCacheId);
        store.ensureExtent(lExtentId);
        store.store(lExtentId, bufTrigger, BINARY_TRUE, oToken);
        }

    /**
     * Clear the trigger registration from the specified persistent store.
     *
     * @param store       the persistent store
     * @param lCacheId    the cache id
     * @param binTrigger  the trigger
     * @param oToken      batch token to use for the store operation, or null
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void unregisterTrigger(PersistentStore<ReadBuffer> store, long lCacheId,
                                         Binary binTrigger, Object oToken)
        {
        ReadBuffer bufTrigger = createTriggerRegistrationKey(lCacheId, binTrigger);

        long lExtentId = getTriggerExtentId(lCacheId);
        store.ensureExtent(lExtentId);
        store.erase(lExtentId, bufTrigger, oToken);
        }

    /**
     * Clear all trigger registrations for the specified cache from the specified
     * persistent store.
     *
     * @param store     the persistent store
     * @param lCacheId  the cache-id
     *
     * @throws PersistenceException if the persistent store operations fail
     */
    public static void unregisterTriggers(PersistentStore<ReadBuffer> store, long lCacheId)
        {
        store.deleteExtent(getTriggerExtentId(lCacheId));
        }

    // ----- service resume support -----------------------------------------

    /**
     * Resume a service on a separate thread and wait for the resume to complete.
     *
     * @param cluster       the cluster to resume service for
     * @param sServiceName  the service to resume
     */
    public static void resumeService(final Cluster cluster, final String sServiceName)
        {
        // This is currently a temporary measure and will be removed once the solution
        // for COH-12164 is implemented.
        new Thread(() -> {cluster.resumeService(sServiceName);}).start();
        }

    // ----- tools support --------------------------------------------------

    /**
     * Return an implementation specific instance of {@link PersistenceTools} for
     * the given local snapshot directory.
     *
     * @param dirSnapshot  the snapshot directory to get tools for
     *
     * @return an implementation specific instance of PersistenceTools
     *
     * @throws PersistenceException if any errors
     */
    public static PersistenceTools getSnapshotPersistenceTools(File dirSnapshot)
        {
        PersistenceTools tools;

        // determine all the valid directories
        if (dirSnapshot == null || !dirSnapshot.isDirectory() ||
            !dirSnapshot.canRead() || !dirSnapshot.canExecute())
            {
            throw ensurePersistenceException(new IllegalArgumentException(
                    "The directory " + dirSnapshot + " does not exist or can not be read"));
            }

        File[] aFiles = dirSnapshot.listFiles((File file) ->
            {
            String sDirName = file.getName();
            if (file.isDirectory() && !DEFAULT_LOCK_DIR.equals(sDirName))
                {
                if (!GUIDHelper.validateGUID(sDirName))
                    {
                    throw new IllegalStateException("Unexpected directory (" + sDirName +
                            ") in snapshot directory: " + dirSnapshot.getAbsolutePath());
                    }
                return true;
                }
            return false;
            });

        if (aFiles == null || aFiles.length == 0)
            {
            throw ensurePersistenceException(new IllegalArgumentException("Directory " +
                    dirSnapshot.getAbsolutePath() + " does not contain any valid snapshot directories"));
            }

        // it appears we have a valid snapshot directory; read the metadata from
        // one of the stores and instantiate the appropriate manager
        try
            {
            Properties props = readMetadata(aFiles[0]);

            String sPersistenceType = props.getProperty(META_STORAGE_FORMAT);

            if ("BDB".equals(sPersistenceType))
                {
                tools = new BerkeleyDBManager(dirSnapshot, null, null).getPersistenceTools();
                }
            else
                {
                // In 12.2.1 we do not support this. See COH-12674
                throw new UnsupportedOperationException("Storage format " + sPersistenceType +
                        " not supported for tools");
                }
            }
        catch (IOException ioe)
            {
            throw ensurePersistenceException(ioe, "Error getting persistence tools for snapshot");
            }

        return tools;
        }

    /**
     * Return an implementation specific instance of PersistenceTools for
     * the given archived snapshot.
     *
     * @param eccf           ExtensibleConfigurableCacheFactory to use
     * @param sSnapshotName  the snapshot to get tools
     * @param sServiceName   the service name to get tools
     *
     * @return an implementation specific instance of PersistenceTools
     *
     * @throws PersistenceException if any errors
     */
    public static PersistenceTools getArchiverPersistenceTools(ExtensibleConfigurableCacheFactory eccf, String sSnapshotName,
                                                               String sServiceName)
        {
        PersistenceDependencies depsPersistence = getPersistenceDependencies(eccf, sServiceName);

        SnapshotArchiverFactory factory = depsPersistence.getArchiverFactory();

        if (factory == null)
            {
            throw new PersistenceException("Unable to find archiver for service " + sServiceName +
                        " in operational configuration");
            }

        SnapshotArchiver archiver = factory.createSnapshotArchiver(getClusterName(), sServiceName);

        return archiver.getPersistenceTools(sSnapshotName);
        }

    /**
     * Return the {@link PersistenceDependencies} for a given ccf and service name
     * using the current {@link ExtensibleConfigurableCacheFactory} without starting the services.
     *
     * @param eccf          ExtensibleConfigurableCacheFactory to use
     * @param sServiceName  the service name to use
     *
     * @return the PersistenceDependencies for the given ccf and service
     */
    private static PersistenceDependencies getPersistenceDependencies(ExtensibleConfigurableCacheFactory eccf,
                                                                     String sServiceName)
        {
        ServiceScheme scheme = eccf.getCacheConfig().findSchemeByServiceName(sServiceName);

        if (scheme != null)
            {
            ServiceDependencies depsService =
                    (ServiceDependencies) ((AbstractServiceScheme) scheme).getServiceDependencies();

            if (depsService instanceof PartitionedServiceDependencies &&
                    sServiceName.equals(scheme.getServiceName()))
                {
                PersistenceDependencies depsPersistence =
                        ((PartitionedServiceDependencies) depsService)
                                .getPersistenceDependencies();

                if (depsPersistence != null)
                    {
                    return depsPersistence;
                    }
                }
            }

        throw new IllegalArgumentException("Unable to find caching scheme for service " + sServiceName);
        }

    /**
     * Return the {@link PersistenceEnvironmentInfo} for a given cluster and service name
     * using the current {@link ConfigurableCacheFactory} without starting the services.
     *
     * @param eccf          ExtensibleConfigurableCacheFactory to use
     * @param sServiceName  the service name to use
     *
     * @return the PersistenceEnvironmentInfo for the given service and cluster
     */
    public static PersistenceEnvironmentInfo getEnvironmentInfo(ExtensibleConfigurableCacheFactory eccf,
                                                                String sServiceName)
        {
        PersistenceDependencies depsPersistence = getPersistenceDependencies(eccf, sServiceName);

        PersistenceEnvironmentParamBuilder bldr = (PersistenceEnvironmentParamBuilder)
                depsPersistence.getPersistenceEnvironmentBuilder();
        return bldr.getPersistenceEnvironmentInfo(getClusterName(), sServiceName);
        }

    /**
     * Return the cluster name without starting the cluster service.
     *
     * @return  the name of the cluster
     */
    protected static String getClusterName()
        {
        ClusterDependencies deps =
                (ClusterDependencies) CacheFactory.getCluster().getDependencies();

        if (deps == null)
            {
            throw new IllegalArgumentException("Unable to find cluster dependencies");
            }

        // do not use SafeCluster.getClusterName().  It unfortunately ensures cluster is running.
        // this should access all persistence info without starting cluster.
        return deps.getMemberIdentity().getClusterName();
        }

    // ----- recovery support -----------------------------------------------

    /**
     * Instantiate a {@link PersistentStore.Visitor visitor} for the PersistentStore
     * backed by the
     *
     * @param visitorCache  the cache visitor to delegate to
     *
     * @return a visitor for the PersistentStore
     */
    public static PersistentStore.Visitor<ReadBuffer> instantiatePersistenceVisitor(final Visitor visitorCache)
        {
        return (lExtentId, bufKey, bufValue) ->
                {
                if (lExtentId > 0L)
                    {
                    return visitorCache.visitCacheEntry(lExtentId,
                            bufKey.toBinary(), bufValue.toBinary());
                    }

                if (META_EXTENT == lExtentId)
                    {
                    // completely internal to CachePersistenceHelper
                    return true;
                    }

                BufferInput in = bufKey.getBufferInput();
                try
                    {
                    switch (in.readByte())  // key type
                        {
                        case KEY_TYPE_LISTENER:
                            {
                            long    lListenerId = in.readLong();
                            long    lCacheId    = in.readLong();
                            boolean fLite       = Base.equals(bufValue, BINARY_TRUE);
                            int     cbHeader    = 17;

                            Binary  binCacheKey = bufKey.toBinary(cbHeader, bufKey.length() - cbHeader);

                            return visitorCache.visitListener(lCacheId, binCacheKey, lListenerId, fLite);
                            }

                        case KEY_TYPE_LOCK:
                            {
                            long   lHolderId = in.readLong();
                            long   lThreadId = in.readLong();
                            long   lCacheId  = in.readLong();
                            int    cbHeader  = 25;

                            Binary binCacheKey = bufKey.toBinary(cbHeader, bufKey.length() - cbHeader);

                            return visitorCache.visitLock(lCacheId, binCacheKey, lHolderId, lThreadId);
                            }

                        case KEY_TYPE_INDEX:
                            {
                            long lCacheId = in.readLong();
                            int  cbHeader = 9;

                            Binary binExtractor  = bufKey.toBinary(cbHeader, bufKey.length() - cbHeader);
                            Binary binComparator = bufValue.toBinary();

                            return visitorCache.visitIndex(lCacheId, binExtractor, binComparator);
                            }

                        case KEY_TYPE_TRIGGER:
                            {
                            long lCacheId = in.readLong();
                            int  cbHeader = 9;

                            Binary binTrigger = bufKey.toBinary(cbHeader, bufKey.length() - cbHeader);

                            return visitorCache.visitTrigger(lCacheId, binTrigger);
                            }

                        default:
                            return false;
                        }
                    }
                catch (IOException e)
                    {
                    throw Base.ensureRuntimeException(e);
                    }
                };
        }

    /**
     * Obtain the PersistenceManagerMBean name for a given service.
     *
     * @param sService  the service name
     *
     * @return the MBean name
     */
    public static String getMBeanName(String sService)
        {
        return Registry.PERSISTENCE_SNAPSHOT_TYPE + ",service=" + sService + ","
             + Registry.KEY_RESPONSIBILITY + PersistenceManagerMBean.PERSISTENCE_COORDINATOR;
        }

    // ----- inner interface: Visitor -------------------------------------

    /**
     * The Visitor interface allows the "iteration" of the persisted contents
     * (and metadata) of a cache service in the style of the
     * <a href="http://en.wikipedia.org/wiki/Visitor_pattern">Visitor Pattern</a>.
     */
    public static interface Visitor
        {
        /**
         * Apply the visitor to the specified cache entry (key-value pair).
         *
         * @param lOldCacheId  the persisted cache-id
         * @param binKey       the cache key
         * @param binValue     the cache value
         *
         * @return false to terminate the iteration
         */
        public boolean visitCacheEntry(long lOldCacheId, Binary binKey, Binary binValue);

        /**
         * Apply the visitor to the specified cache entry listener.
         *
         * @param lOldCacheId  the persisted cache-id
         * @param binKey       the cache key
         * @param lListenerId  the service-unique identifier of the listening member
         * @param fLite        true iff the listener should receive "lite" events
         *
         * @return false to terminate the iteration
         */
        public boolean visitListener(long lOldCacheId, Binary binKey, long lListenerId, boolean fLite);

        /**
         * Apply the visitor to the specified cache entry lock.
         *
         * @param lOldCacheId      the persisted cache-id
         * @param binKey           the cache key
         * @param lHolderId        the service-unique identifier of the lock holder
         * @param lHolderThreadId  the thread-id of the lock holder
         *
         * @return false to terminate the iteration
         */
        public boolean visitLock(long lOldCacheId, Binary binKey, long lHolderId, long lHolderThreadId);

        /**
         * Apply the visitor to the specified cache index.
         *
         * @param lOldCacheId    the persisted cache-id
         * @param binExtractor   the index extractor
         * @param binComparator  the index comparator
         *
         * @return false to terminate the iteration
         */
        public boolean visitIndex(long lOldCacheId, Binary binExtractor, Binary binComparator);

        /**
         * Apply the visitor to the specified trigger.
         *
         * @param lOldCacheId  the persisted cache-id
         * @param binTrigger   the trigger
         *
         * @return false to terminate the iteration
         */
        public boolean visitTrigger(long lOldCacheId, Binary binTrigger);
        }

    // ----- constants ----------------------------------------------------

    /**
     * Default persistence directory system property.
     */
    public static final String DEFAULT_BASE_DIR_PROPERTY = "coherence.distributed.persistence.base.dir";

    /**
     * Persistence metadata filename.
     */
    public static final String META_FILENAME = "meta.properties";

    /**
     * Persistence metadata property: implementation version.
     */
    public static final String META_IMPL_VERSION = "implementation.version";

    /**
     * Persistence metadata property: storage format.
     */
    public static final String META_STORAGE_FORMAT = "storage.format";

    /**
     * Persistence metadata property: storage version.
     */
    public static final String META_STORAGE_VERSION = "storage.version";

    /**
     * Persistence metadata property: partition count.
     */
    public static final String META_PARTITION_COUNT = "partition.count";

    /**
     * Persistence metadata property: service version.
     */
    public static final String META_SERVICE_VERSION = "service.version";

    /**
     * Persistence protocol version.
     */
    public static final int PERSISTENCE_VERSION = 14;

    /**
     * Default base persistence directory name.
     */
    public static final String DEFAULT_BASE_DIR = "coherence";

    /**
     * Default active directory name.
     */
    public static final String DEFAULT_ACTIVE_DIR = "active";

    /**
     * Default snapshot directory name.
     */
    public static final String DEFAULT_SNAPSHOT_DIR = "snapshots";

    /**
     * Default trash directory name.
     */
    public static final String DEFAULT_TRASH_DIR = "trash";

    /**
     * Default lock directory name.
     */
    public static final String DEFAULT_LOCK_DIR = ".lock";

    /**
     * The extent-id used to store cache metadata.
     */
    public static final long META_EXTENT = 0L;

    /**
     * Reserve a certain number of extents identifiers for holding metadata.
     */
    public static final int  RESERVED_META_EXTENTS = 8;

    /**
     * Listener metadata key type.
     */
    private static final byte KEY_TYPE_LISTENER = 0;

    /**
     * Lock metadata key type.
     */
    private static final byte KEY_TYPE_LOCK = 1;

    /**
     * Index metadata key type.
     */
    private static final byte KEY_TYPE_INDEX = 2;

    /**
     * Trigger metadata key type.
     */
    private static final byte KEY_TYPE_TRIGGER = 3;

    /**
     * The marker Binary used to seal a partition.
     */
    private static final Binary BINARY_SEAL =
            new Binary(new byte[] { 0x53, 0x45, 0x41, 0x4c }); // SEAL

    /**
     * The marker Binary used as a key for the membership quorum info.
     */
    private static final Binary BINARY_QUORUM =
            new Binary(new byte[] { 0x51, 0x55, 0x4F, 0x52, 0x55, 0x4D }); // QUORUM

    /**
     * The marker Binary used as the key for the service partition count.
     */
    private static final Binary BINARY_PARTITION_COUNT =
            new Binary(new byte[] { 0x50, 0x41, 0x52, 0x54, 0x49, 0x54, 0x49,
                                    0x4F, 0x4E, 0x5F, 0x43, 0x4F, 0x55, 0x4E, 0x54 }); // PARTITION_COUNT

    /**
     * The marker Binary used as the key for the persistence version.
     */
    private static final Binary BINARY_PERSISTENCE_VERSION =
            new Binary(new byte[] { 0x53, 0x45, 0x52, 0x56, 0x49, 0x43, 0x45,
                                    0x5F, 0x56, 0x45, 0x52, 0x53, 0x49, 0x4F, 0x4E}); // SERVICE_VERSION

    /**
     * The marker Binary used as the key for the cache-names.
     */
    private static final Binary BINARY_CACHES =
            new Binary(new byte[] { 0x43, 0x41, 0x43, 0x48, 0x45, 0x53 }); // CACHES

    /**
     * The marker Binary to represent a "true" binary value.
     */
    private static final Binary BINARY_TRUE = new Binary(new byte[] { 0x1 });

    /**
     * The marker Binary to represent a "false" binary value.
     */
    private static final Binary BINARY_FALSE = new Binary(new byte[] { 0x0 });
    }
