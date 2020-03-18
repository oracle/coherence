/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.bdb;

import com.tangosol.persistence.AbstractPersistenceManager;
import com.tangosol.persistence.AbstractPersistencePerformanceTest;

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
