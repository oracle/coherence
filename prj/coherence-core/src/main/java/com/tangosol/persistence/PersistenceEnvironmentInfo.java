/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import java.io.File;

/**
 * Interface that exposes management attributes for a PersistenceEnvironment.
 *
 * @author jh  2013.07.02
 */
public interface PersistenceEnvironmentInfo
    {
    /**
     * Return the directory under which cached data is actively persisted
     * by the environment.
     *
     * @return the active directory
     */
    public File getPersistenceActiveDirectory();

    /**
     * Return the directory under which cached data is actively persisted
     * by the environment.
     *
     * @return the backup directory
     */
    public File getPersistenceBackupDirectory();

    /**
     * Return the directory under which cached data is actively persisted
     * by the environment.
     *
     * @return the events directory
     */
    public File getPersistenceEventsDirectory();

    /**
     * Return the directory under which copies of cached data are persisted
     * by the environment.
     *
     * @return the snapshot directory
     */
    public File getPersistenceSnapshotDirectory();

    /**
     * Return the directory under which potentially corrupted persisted data
     * is stored by the environment.
     *
     * @return the trash directory
     */
    public File getPersistenceTrashDirectory();

    /**
     * Return the total size in bytes used by the persistence layer to
     * persist mutating cache operations.
     *
     * @return the total size
     */
    public long getPersistenceActiveSpaceUsed();

    /**
     * Return the total size in bytes used by the persistence layer to
     * persist backups of mutating cache operations.
     *
     * @return the total size
     */
    public long getPersistenceBackupSpaceUsed();
    }
