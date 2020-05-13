/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

/**
 * A data structure encapsulating information derived from an implementation of
 * {@link PersistenceTools}.
 *
 * @since 12.2.1
 * @author hr/tam  2014.10.11
 */
public class OfflinePersistenceInfo
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an instance to store offline information about a persistent
     * snapshot or archived snapshot.
     *
     * @param cPartitions      the partition count as defined in metadata
     * @param sStorageFormat   the storage format
     * @param fArchive         true if the snapshot is archived
     * @param asGUIDs          the set of GUIDs
     * @param nStorageVersion  the storage version
     * @param nImplVersion     the implementation version
     * @param sServiceVersion  the service version
     *
     * @deprecated use {@link #OfflinePersistenceInfo(int, String ,boolean, String[], int, int, int)} instead
     */
    public OfflinePersistenceInfo(int cPartitions, String sStorageFormat, boolean fArchive, String[] asGUIDs,
                                  int nStorageVersion, int nImplVersion, String sServiceVersion)
        {
        this(cPartitions, sStorageFormat, fArchive, asGUIDs, nStorageVersion,
                nImplVersion, Integer.parseInt(sServiceVersion));
        }

    /**
     * Construct an instance to store offline information about a persistent
     * snapshot or archived snapshot.
     *
     * @param cPartitions          the partition count as defined in metadata
     * @param sStorageFormat       the storage format
     * @param fArchive             true if the snapshot is archived
     * @param asGUIDs              the set of GUIDs
     * @param nStorageVersion      the storage version
     * @param nImplVersion         the implementation version
     * @param nPersistenceVersion  the persistence version
     */
    public OfflinePersistenceInfo(int cPartitions, String sStorageFormat, boolean fArchive, String[] asGUIDs,
                                  int nStorageVersion, int nImplVersion, int nPersistenceVersion)
        {
        f_cPartitions         = cPartitions;
        f_sStorageFormat      = sStorageFormat;
        f_fArchive            = fArchive;
        f_asGUIDs             = asGUIDs == null ? new String[] {} : asGUIDs;
        f_nStorageVersion     = nStorageVersion;
        f_nImplVersion        = nImplVersion;
        f_nPersistenceVersion = nPersistenceVersion;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder("Persistence Information\n\n");

        sb.append("Storage version:              ").append(getStorageVersion()).append('\n')
          .append("Implementation version:       ").append(getImplVersion()).append('\n')
          .append("Number of partition:          ").append(getPartitionCount()).append('\n')
          .append("Number of partitions present: ").append(getGUIDs().length).append('\n')
          .append("Is Complete?:                 ").append(isComplete()).append('\n')
          .append("Is Archived?:                 ").append(isArchived()).append('\n')
          .append('\n');

        return sb.toString();
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the partition count as defined in the store metadata.
     *
     * @return the partition count as defined in the store metadata
     */
    public int getPartitionCount()
        {
        return f_cPartitions;
        }

    /**
     * Return the storage format of the snapshot.
     *
     * @return the storage format
     */
    public String getStorageFormat()
        {
        return f_sStorageFormat;
        }

    /**
     * Return the GUIDs that the related snapshot or archive is aware of.
     *
     * @return the GUIDs that the related snapshot or archive is aware of
     */
    public String[] getGUIDs()
        {
        return f_asGUIDs;
        }

    /**
     * Return true if all parts of the snapshot are present.
     *
     * @return true if all parts of the snapshot are present
     */
    public boolean isComplete()
        {
        return f_asGUIDs.length == f_cPartitions;
        }

    /**
     * Return true if the snapshot is an archived snapshot.
     *
     * @return true if the snapshot is an archived snapshot
     */
    public boolean isArchived()
        {
        return f_fArchive;
        }

    /**
     * Return the storage version.
     *
     * @return the storage version
     */
    public int getStorageVersion()
        {
        return f_nStorageVersion;
        }

    /**
     * Return the implementation version.
     *
     * @return the implementation version
     */
    public int getImplVersion()
        {
        return f_nImplVersion;
        }

    /**
     * Return the service version.
     *
     * @return the service version
     *
     * @deprecated use {@link #getPersistenceVersion()} instead
     */
    public String getServiceVersion()
        {
        return String.valueOf(f_nPersistenceVersion);
        }

    /**
     * Return the persistence version.
     *
     * @return the persistence version
     */
    public int getPersistenceVersion()
        {
        return f_nPersistenceVersion;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The partition count as defined in the store metadata.
     */
    private final int f_cPartitions;

    /**
     * The storage type.
     */
    private final String f_sStorageFormat;

    /**
     * The GUIDs in the snapshot.
     */
    private String[] f_asGUIDs;

    /**
     * Indicates if the snapshot is archived or not.
     */
    private final boolean f_fArchive;

    /**
     * Implementation version.
     */
    private final int f_nImplVersion;

    /**
     * Storage version.
     */
    private final int f_nStorageVersion;

    /**
     * Persistence version.
     */
    private final int f_nPersistenceVersion;
    }