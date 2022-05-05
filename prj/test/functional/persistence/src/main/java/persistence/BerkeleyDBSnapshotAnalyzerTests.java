/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.persistence.PersistenceManager;

import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.bdb.BerkeleyDBManager;

import java.io.File;
import java.io.IOException;

/**
 * Functional tests for SnapshotAnalzyer using BDB.
 *
 * @author tam  2014.10.23
 */
public class BerkeleyDBSnapshotAnalyzerTests
        extends AbstractSnapshotAnalyzerTests
    {
    // ----- AbstractCohQLPersistenceTests methods -------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected PersistenceManager<ReadBuffer> createPersistenceManager(File file)
            throws IOException
        {
        return new BerkeleyDBManager(file, null, null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPersistenceManagerName()
        {
        return "BDB";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheConfigPath()
        {
        return "simple-persistence-bdb-cache-config.xml";
        }
    }