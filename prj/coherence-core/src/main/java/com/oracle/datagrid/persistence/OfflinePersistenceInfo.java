/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.datagrid.persistence;

/**
 * A data structure encapsulating information derived from an implementation of
 * {@link PersistenceTools}.
 *
 * @since 12.2.1
 * @author hr/tam  2014.10.11
 *
 * @deprecated use {@link com.oracle.coherence.persistence.OfflinePersistenceInfo} instead
 */
@Deprecated
public class OfflinePersistenceInfo
    extends com.oracle.coherence.persistence.OfflinePersistenceInfo
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
        super(cPartitions, sStorageFormat, fArchive, asGUIDs, nStorageVersion, nImplVersion, sServiceVersion);
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
        super(cPartitions, sStorageFormat, fArchive, asGUIDs, nStorageVersion, nImplVersion, nPersistenceVersion);
        }
    }
