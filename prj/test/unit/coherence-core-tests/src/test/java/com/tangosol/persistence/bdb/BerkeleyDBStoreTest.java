/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.bdb;

import com.tangosol.persistence.AbstractPersistentStoreTest;
import com.tangosol.persistence.AbstractPersistenceManager;

import java.io.IOException;

/**
 * Unit test of the BerkeleyDBStore.
 *
 * @author jh  2012.10.10
 */
public class BerkeleyDBStoreTest
        extends AbstractPersistentStoreTest
    {

    // ----- test lifecycle -------------------------------------------------

    @Override
    protected AbstractPersistenceManager createPersistenceManager()
            throws IOException
        {
        BerkeleyDBManager manager = new BerkeleyDBManager(m_file, null, null);
        manager.setDaemonPool(m_pool);
        return manager;
        }
    }
