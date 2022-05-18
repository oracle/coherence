/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.bdb;

import com.tangosol.persistence.AbstractPersistencePerformanceTest;
import com.tangosol.persistence.AbstractPersistenceManager;

import java.io.IOException;

/**
 * Performance test for a BerkeleyDBManager.
 *
 * @author jh  2012.11.13
 */
public class BerkeleyDBPerformanceTest
        extends AbstractPersistencePerformanceTest
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
