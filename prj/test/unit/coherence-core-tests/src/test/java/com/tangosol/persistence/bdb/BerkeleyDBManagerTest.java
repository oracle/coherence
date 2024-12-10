/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.bdb;

import com.tangosol.persistence.AbstractPersistenceManagerTest;
import com.tangosol.persistence.AbstractPersistenceManager;

import java.io.File;
import java.io.IOException;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test of the BerkeleyDBManager.
 *
 * @author jh  2012.10.15
 */
public class BerkeleyDBManagerTest
        extends AbstractPersistenceManagerTest
    {

    // ----- test lifecycle -------------------------------------------------

    @Override
    protected AbstractPersistenceManager createPersistenceManager()
            throws IOException
        {
        BerkeleyDBManager manager = new BerkeleyDBManager(m_fileData, m_fileTrash, null);
        manager.setDaemonPool(m_pool);
        return manager;
        }

    @Override
    protected void corruptPersistentStore(String sId)
            throws IOException
        {
        m_manager.release();

        // attempt to corrupt the journal file
        File file = new File(m_fileStore, "00000000.jdb");
        if (file.exists())
            {
            if (file.delete())
                {
                file.createNewFile();
                }
            }

        m_manager = createPersistenceManager();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testGetPersistentStoreMap()
        {
        BerkeleyDBManager managerImpl = (BerkeleyDBManager) m_manager;
        synchronized (managerImpl)
            {
            Map<String, BerkeleyDBManager.BerkeleyDBStore> map = managerImpl.getPersistentStoreMap();
            assertTrue(map.isEmpty());

            managerImpl.open(TEST_STORE_ID, null);
            assertEquals(1, map.size());

            managerImpl.close(TEST_STORE_ID);
            assertEquals(0, map.size());

            managerImpl.open(TEST_STORE_ID, null);
            assertEquals(1, map.size());

            managerImpl.delete(TEST_STORE_ID, false);
            assertEquals(0, map.size());
            }
        }
    }
