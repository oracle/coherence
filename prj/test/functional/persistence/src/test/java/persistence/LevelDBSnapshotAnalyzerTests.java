/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package persistence;

import com.oracle.coherence.persistence.PersistenceManager;

import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.ldb.LevelDBManager;

import java.io.File;
import java.io.IOException;

/**
 * Functional tests for SnapshotAnalzyer using LDB.
 *
 * @author tam  2014.10.23
 */
public class LevelDBSnapshotAnalyzerTests
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
        return new LevelDBManager(file, null, null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPersistenceManagerName()
        {
        return "LDB";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheConfigPath()
        {
        return "simple-persistence-ldb-cache-config.xml";
        }
    }
