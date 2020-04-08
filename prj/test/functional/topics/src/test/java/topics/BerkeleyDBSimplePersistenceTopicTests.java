/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.coherence.persistence.PersistenceManager;

import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.bdb.BerkeleyDBManager;

import java.io.File;
import java.io.IOException;

/**
 * Functional tests for simple cache persistence and recovery using the
 * BerkeleyDBPersistenceManager.
 *
 * @author jh  2012.10.18
 */
public class BerkeleyDBSimplePersistenceTopicTests
        extends AbstractSimplePersistenceTopicTests
    {

    // ----- AbstractSimplePersistenceTopicTests methods -------------------------

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
