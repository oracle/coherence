/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.persistence.PersistenceEnvironment;

import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.bdb.BerkeleyDBEnvironment;

import java.io.File;
import java.io.IOException;

/**
 * Test archival and retrieval functionality with BerkeleyDB environment.
 *
 * @author tam  2014.07.22
 */
public class BerkeleyDBArchiverPersistenceTests
        extends AbstractArchiverPersistenceTests
    {
    // ---- AbstractArchiverPersistenceTests methods ------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected PersistenceEnvironment<ReadBuffer> createPersistenceEnv(File fileActive, File fileSnapshot,
        File fileTrash)
            throws IOException
        {
        return new BerkeleyDBEnvironment(fileActive, fileSnapshot, fileTrash);
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
