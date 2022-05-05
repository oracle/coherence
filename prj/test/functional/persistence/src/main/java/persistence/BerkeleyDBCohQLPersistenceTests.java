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
 * Functional tests for CohQL persistence commands using BDB.
 *
 * @author tam  2014.09.12
 */
public class BerkeleyDBCohQLPersistenceTests
        extends AbstractCohQLPersistenceTests
    {
    // ----- AbstractCohQLPersistenceTests methods -------------------------

    @Override
    protected PersistenceManager<ReadBuffer> createPersistenceManager(File file)
            throws IOException
        {
        return new BerkeleyDBManager(file, null, null);
        }

    @Override
    public String getPersistenceManagerName()
        {
        return "BDB";
        }

    @Override
    public String getCacheConfigPath()
        {
        return "simple-persistence-bdb-cache-config.xml";
        }
    }