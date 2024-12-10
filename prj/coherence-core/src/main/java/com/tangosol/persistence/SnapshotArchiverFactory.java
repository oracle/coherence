/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

/**
 * A factory for {@link SnapshotArchiver} objects.
 *
 * @since 12.2.1
 * @author jh/tm  2014.03.05
 */
public interface SnapshotArchiverFactory
    {
    /**
     * Create a new SnapshotArchiver.
     *
     * @param sClusterName  the name of the Cluster that is creating the
     *                      archiver
     * @param sServiceName  the name of the Service that is creating the
     *                      archiver
     *
     * @return a new SnapshotArchiver
     */
    public SnapshotArchiver createSnapshotArchiver(String sClusterName, String sServiceName);
    }
