/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.persistence.ConcurrentAccessException;
import com.oracle.coherence.persistence.FatalAccessException;
import com.oracle.coherence.persistence.PersistentStore;

import com.oracle.coherence.persistence.PersistentStore.Visitor;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.persistence.AbstractPersistenceManager.AbstractPersistentStore;

import com.tangosol.util.Base;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;

import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import com.tangosol.util.Binary;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Abstract unit test for an AbstractPersistenceManager subclass.
 *
 * @author jh  2012.10.15
 */
public abstract class AbstractPersistenceManagerTest
        extends Base
    {
    // ----- test lifecycle -------------------------------------------------

    @Before
    public void setupTest()
            throws IOException
        {
        m_fileData  = FileHelper.createTempDir();
        m_fileStore = new File(m_fileData, TEST_STORE_ID);
        m_fileTrash = FileHelper.createTempDir();

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setThreadCount(1);

        m_pool = Daemons.newDaemonPool(deps);
        m_pool.start();

        m_manager = createPersistenceManager();
        }

    @After
    public void teardownTest()
        {
        m_manager.release();
        m_pool.stop();

        try
            {
            FileHelper.deleteDir(m_fileData);
            }
        catch (IOException e)
            {
            // ignore
            }
        try
            {
            FileHelper.deleteDir(m_fileTrash);
            }
        catch (IOException e)
            {
            // ignore
            }
        }

    protected void makePersistentStoreIncompatible(String sId)
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;

        File file = m_fileStore;
        if (!file.exists())
            {
            file.createNewFile();
            manager.writeMetadata(file);
            }

        Properties prop = manager.readMetadata(file);
        prop.setProperty(CachePersistenceHelper.META_IMPL_VERSION,
                         String.valueOf(manager.getImplVersion() + 1));

        CachePersistenceHelper.writeMetadata(file, prop);
        }

    protected abstract AbstractPersistenceManager createPersistenceManager()
            throws IOException;
    protected abstract void corruptPersistentStore(String sId)
            throws IOException;

    // ----- test methods ---------------------------------------------------

    @Test
    public void testGetDataDirectory()
        {
        assertEquals(m_fileData, m_manager.getDataDirectory());
        }

    @Test
    public void testGetTrashDirectory()
        {
        assertEquals(m_fileTrash, m_manager.getTrashDirectory());
        }

    @Test
    public void testGetName()
        {
        assertEquals(m_fileData.toString(), m_manager.getName());
        }

    @Test
    public void testGetPersistenceEnvironment()
        {
        assertNull(m_manager.getPersistenceEnvironment());
        }

    @Test
    public void testList()
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;

        // create a new PersistenceManager for the same directory
        AbstractPersistenceManager manager2 = createPersistenceManager();
        try
            {
            assertArrayEquals(manager.list(), manager2.list());

            // create a new PersistentStore and make sure it shows up in both
            // manager's store lists
            AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
            assertEquals(TEST_STORE_ID, store.getId());
            assertArrayEquals(new String[] {TEST_STORE_ID}, manager.list());
            assertArrayEquals(manager.list(), manager2.list());

            // delete the PersistentStore and make sure it disappears from both
            // manager's store lists
            manager.delete(TEST_STORE_ID, false);
            assertArrayEquals(AbstractPersistenceManager.NO_STRINGS, manager.list());
            assertArrayEquals(manager.list(), manager2.list());
            }
        finally
            {
            manager2.release();
            }
        }

    @Test
    public void testListIncompatible()
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;
        assertArrayEquals(AbstractPersistenceManager.NO_STRINGS, manager.list());

        // create a new PersistentStore and make sure it shows up in the list
        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        assertEquals(TEST_STORE_ID, store.getId());
        assertArrayEquals(new String[] {TEST_STORE_ID}, manager.list());

        // close the PersistentStore and modify it's metadata to make it
        // incompatible and make sure it disappears from the list
        manager.close(TEST_STORE_ID);
        makePersistentStoreIncompatible(TEST_STORE_ID);
        assertArrayEquals(AbstractPersistenceManager.NO_STRINGS, manager.list());
        }

    @Test
    public void testListOpen()
        {
        AbstractPersistenceManager manager = m_manager;
        assertArrayEquals(AbstractPersistenceManager.NO_STRINGS, manager.listOpen());

        // create a new PersistentStore and make sure it shows up in the list
        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        assertEquals(TEST_STORE_ID, store.getId());
        assertArrayEquals(new String[] {TEST_STORE_ID}, manager.listOpen());

        // close the store and make sure it is removed from the list
        manager.close(TEST_STORE_ID);
        assertArrayEquals(AbstractPersistenceManager.NO_STRINGS, manager.listOpen());
        }

    @Test
    public void testOpenFromEmpty()
            throws IOException
        {
        // assert that a PersistenceManager created with an empty directory
        // returns an empty array from it's list() method
        AbstractPersistenceManager manager = m_manager;
        assertEquals(0, manager.list().length);

        // create a new PersistentStore and make sure it shows up in the list
        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        assertEquals(TEST_STORE_ID, store.getId());
        assertArrayEquals(new String[] {TEST_STORE_ID}, manager.list());

        // release the PersistenceManager
        manager.release();

        // assert that the actual underlying persistent store directory exists
        assertTrue(m_fileStore.exists());

        // assert that the PersistentStore metadata file exists
        Properties prop = manager.readMetadata(m_fileStore);
        assertEquals(String.valueOf(manager.getImplVersion()),
                prop.getProperty(CachePersistenceHelper.META_IMPL_VERSION));
        assertEquals(manager.getStorageFormat(),
                prop.getProperty(CachePersistenceHelper.META_STORAGE_FORMAT));
        assertEquals(String.valueOf(manager.getStorageVersion()),
                prop.getProperty(CachePersistenceHelper.META_STORAGE_VERSION));

        // recreate the same PersistenceManager and make sure the newly
        // created PersistentStore shows up in the list
        m_manager = manager = createPersistenceManager();
        assertArrayEquals(new String[] {TEST_STORE_ID}, manager.list());
        }

    @Test
    public void testOpenAndCopy()
            throws IOException
        {
        // assert that a PersistenceManager created with an empty directory
        // returns an empty array from it's list() method
        AbstractPersistenceManager manager = m_manager;
        assertEquals(0, manager.list().length);

        // create a new PersistentStore to be used as the storeFrom in the API
        // manager.open(sGUID, storeFrom);
        // all data residing in storeFrom should be made available in the new store
        AbstractPersistentStore storeFrom = (AbstractPersistentStore)
                manager.open(TEST_FROM_STORE_ID, null);

        // populate the base store
        Binary   binValue = Base.getRandomBinary(4, 8);
        Binary[] aBinKeys = new Binary[]
                {
                new Binary(new byte[] {0x0, 0x0, 0x0, 0x1}),
                new Binary(new byte[] {0x0, 0x0, 0x0, 0x2}),
                new Binary(new byte[] {0x0, 0x0, 0x0, 0x4}),
                new Binary(new byte[] {0x0, 0x0, 0x0, 0x8}),
                };

        storeFrom.ensureExtent(1L);
        Object oToken = storeFrom.begin();
        for (int i = 0, c = aBinKeys.length; i < c; ++i)
            {
            storeFrom.store(1L, aBinKeys[i], binValue, oToken);
            }
        storeFrom.commit(oToken);

        // open the subsequent store with the base store
        AbstractPersistentStore store = (AbstractPersistentStore)
                manager.open(TEST_STORE_ID, storeFrom);

        assertEquals(TEST_STORE_ID, store.getId());
        assertThat(manager.list(), Matchers.arrayContainingInAnyOrder(TEST_FROM_STORE_ID, TEST_STORE_ID));

        // assert data has been copied from the base store
        int[] acEntries = new int[1];
        store.iterate((lExtentId, bufKey, bufValue) ->
            {
            acEntries[0]++;
            int iBinKeys = Arrays.binarySearch(aBinKeys, bufKey);

            assertEquals(1L, lExtentId);
            assertTrue(iBinKeys >= 0);
            assertTrue(aBinKeys[iBinKeys] != null);

            aBinKeys[iBinKeys] = null;
            while (++iBinKeys < aBinKeys.length)
                {
                aBinKeys[iBinKeys - 1] = aBinKeys[iBinKeys];
                }

            return true;
            });
        assertEquals(4, acEntries[0]);

        // release the PersistenceManager
        manager.release();

        // assert that the actual underlying persistent store directory exists
        assertTrue(m_fileStore.exists());

        // assert that the PersistentStore metadata file exists
        Properties prop = manager.readMetadata(m_fileStore);
        assertEquals(String.valueOf(manager.getImplVersion()),
                prop.getProperty(CachePersistenceHelper.META_IMPL_VERSION));
        assertEquals(manager.getStorageFormat(),
                prop.getProperty(CachePersistenceHelper.META_STORAGE_FORMAT));
        assertEquals(String.valueOf(manager.getStorageVersion()),
                prop.getProperty(CachePersistenceHelper.META_STORAGE_VERSION));

        // recreate the same PersistenceManager and make sure the newly
        // created PersistentStore shows up in the list
        m_manager = manager = createPersistenceManager();
        assertThat(manager.list(), Matchers.arrayContainingInAnyOrder(TEST_FROM_STORE_ID, TEST_STORE_ID));
        }

    @Test
    public void testOpenAndCopyIncompatible()
            throws IOException
        {
        // assert that a PersistenceManager created with an empty directory
        // returns an empty array from it's list() method
        AbstractPersistenceManager manager = m_manager;
        assertEquals(0, manager.list().length);

        // create a mocked PersistentStore to be used as the storeFrom in the API
        // manager.open(sGUID, storeFrom);
        // all data residing in storeFrom should be made available in the new store
        AbstractPersistentStore storeFrom = mock(AbstractPersistentStore.class);

        // populate the base store
        Binary   binValue = Base.getRandomBinary(4, 8);
        Binary[] aBinKeys = new Binary[]
                {
                new Binary(new byte[] {0x0, 0x0, 0x0, 0x1}),
                new Binary(new byte[] {0x0, 0x0, 0x0, 0x2}),
                new Binary(new byte[] {0x0, 0x0, 0x0, 0x4}),
                new Binary(new byte[] {0x0, 0x0, 0x0, 0x8}),
                };

        when(storeFrom.extents()).thenReturn(new long[]{1L});
        when(storeFrom.isOpen()).thenReturn(true);
        doAnswer(invMock ->
            {
            Visitor<ReadBuffer> visitor = (Visitor<ReadBuffer>) invMock.getArguments()[0];
            for (int i = 0, c = aBinKeys.length; i < c; ++i)
                {
                visitor.visit(1L, aBinKeys[i], binValue);
                }
            return null;
            }).when(storeFrom).iterate(any());
        // open the subsequent store with the base store
        AbstractPersistentStore store = (AbstractPersistentStore)
                manager.open(TEST_STORE_ID, storeFrom);

        assertEquals(TEST_STORE_ID, store.getId());
        assertArrayEquals(new String[] {TEST_STORE_ID}, manager.list());

        // assert data has been copied from the base store
        int[] acEntries = new int[1];
        store.iterate((lExtentId, bufKey, bufValue) ->
            {
            acEntries[0]++;
            int iBinKeys = Arrays.binarySearch(aBinKeys, bufKey);

            assertEquals(1L, lExtentId);
            assertTrue(iBinKeys >= 0);
            assertTrue(aBinKeys[iBinKeys] != null);

            aBinKeys[iBinKeys] = null;
            while (++iBinKeys < aBinKeys.length)
                {
                aBinKeys[iBinKeys - 1] = aBinKeys[iBinKeys];
                }

            return true;
            });
        assertEquals(4, acEntries[0]);

        // release the PersistenceManager
        manager.release();

        // assert that the actual underlying persistent store directory exists
        assertTrue(m_fileStore.exists());

        // assert that the PersistentStore metadata file exists
        Properties prop = manager.readMetadata(m_fileStore);
        assertEquals(String.valueOf(manager.getImplVersion()),
                prop.getProperty(CachePersistenceHelper.META_IMPL_VERSION));
        assertEquals(manager.getStorageFormat(),
                prop.getProperty(CachePersistenceHelper.META_STORAGE_FORMAT));
        assertEquals(String.valueOf(manager.getStorageVersion()),
                prop.getProperty(CachePersistenceHelper.META_STORAGE_VERSION));

        // recreate the same PersistenceManager and make sure the newly
        // created PersistentStore shows up in the list
        m_manager = manager = createPersistenceManager();
        assertArrayEquals(new String[] {TEST_STORE_ID}, manager.list());
        }

    @Test(expected = FatalAccessException.class)
    public void testOpenIncompatible()
            throws IOException
        {
        // assert that a PersistenceManager created with an empty directory
        // returns an empty array from it's list() method
        AbstractPersistenceManager manager = m_manager;
        assertEquals(0, manager.list().length);

        // create a new PersistentStore and make sure it shows up in the list
        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        assertEquals(TEST_STORE_ID, store.getId());
        assertArrayEquals(new String[] {TEST_STORE_ID}, manager.list());

        // close the PersistentStore and modify it's metadata to make it
        // incompatible and try to open it again
        manager.close(TEST_STORE_ID);
        makePersistentStoreIncompatible(TEST_STORE_ID);
        manager.open(TEST_STORE_ID, null);
        }

    @Test
    public void testClose()
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;

        // create a new PersistentStore
        manager.open(TEST_STORE_ID, null);

        // release the PersistenceManager
        manager.release();

        // assert that the actual underlying persistent store directory exists
        assertTrue(m_fileStore.exists());

        // recreate the same PersistenceManager, close the PersistentStore,
        // and assert that it still exists in the list
        m_manager = manager = createPersistenceManager();
        manager.close(TEST_STORE_ID);
        assertArrayEquals(new String[]{TEST_STORE_ID}, manager.list());

        // release the PersistenceManager and assert that the underlying
        // persistence store directory still exists
        manager.release();
        assertTrue(m_fileStore.exists());
        }

    protected void testDelete(boolean fOpen)
            throws IOException
        {
        try
            {
            AbstractPersistenceManager manager = m_manager;

            // create a new PersistentStore
            manager.open(TEST_STORE_ID, null);

            // release the PersistenceManager
            manager.release();

            // assert that the actual underlying persistent store directory exists
            assertTrue(m_fileStore.exists());

            // recreate the same PersistenceManager, delete the PersistentStore,
            // and assert that it no longer exists in the list
            m_manager = manager = createPersistenceManager();
            if (fOpen)
                {
                manager.open(TEST_STORE_ID, null);
                }
            manager.delete(TEST_STORE_ID, false);
            assertEquals(0, manager.list().length);

            // assert that the actual underlying persistent store directory no
            // longer exists
            assertFalse(m_fileStore.exists());

            // recreate the same PersistenceManager and make sure the newly
            // deleted PersistentStore no longer exists in the list
            manager.release();
            m_manager = manager = createPersistenceManager();
            assertEquals(0, manager.list().length);
            }
        catch (Exception e)
            {
            Logger.info("##got exception ", e);
            fail();
            }
        }

    @Test
    public void testDelete()
            throws IOException
        {
        testDelete(true);
        }

    @Test
    public void testDeleteBlind()
            throws IOException
        {
        testDelete(false);
        }

    @Test
    public void testDeleteCorrupted()
            throws Exception
        {
        AbstractPersistenceManager manager = m_manager;

        // create a new PersistentStore, populate it with some data, and
        // then corrupt it
        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        assertEquals(TEST_STORE_ID, store.getId());
        assertArrayEquals(new String[]{TEST_STORE_ID}, manager.list());

        store.ensureExtent(1);
        store.store(1,
                AbstractPersistentStoreTest.BINARY_KEY_1,
                AbstractPersistentStoreTest.BINARY_VALUE_1,
                null);

        manager.close(TEST_STORE_ID);
        corruptPersistentStore(TEST_STORE_ID);
        manager = m_manager;

        try
            {
            manager.open(TEST_STORE_ID, null);
            fail("fatal exception expected");
            }
        catch (FatalAccessException e)
            {
            // expected
            }

        // assert that the actual underlying Level DB database directory no
        // longer exists
        m_manager.delete(TEST_STORE_ID, false);
        assertFalse(m_fileStore.exists());
        }

    @Test
    public void testDeleteSafe()
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;

        // create a new PersistentStore
        manager.open(TEST_STORE_ID, null);

        // release the PersistenceManager
        manager.release();

        // assert that the actual underlying persistent store directory exists
        assertTrue(m_fileStore.exists());

        // recreate the same PersistenceManager, delete the PersistentStore,
        // and assert that it no longer exists in the list
        m_manager = manager = createPersistenceManager();
        manager.delete(TEST_STORE_ID, true);
        assertEquals(0, manager.list().length);

        // validate that the underlying persistence directory was moved to
        // the trash
        assertFalse(m_fileStore.exists());
        assertTrue(new File(m_fileTrash, TEST_STORE_ID).exists());

        // recreate the same PersistenceManager and make sure the newly
        // deleted PersistentStore no longer exists in the list
        manager.release();
        m_manager = manager = createPersistenceManager();
        assertEquals(0, manager.list().length);
        }

    @Test(expected = ConcurrentAccessException.class)
    public void testReadWhileOpen()
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;

        // create a new PersistentStore
        manager.open(TEST_STORE_ID, null);

        manager.read(TEST_STORE_ID, (BufferInput) null);
        }

    @Test(expected = StreamCorruptedException.class)
    public void testReadCorrupted()
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;

        // create a stream without magic
        InputStream in = new ByteArrayInputStream(new byte[] {0, 1, 2, 3});
        try
            {
            manager.read(TEST_STORE_ID, in);
            }
        catch (StreamCorruptedException e)
            {
            assertTrue(manager.list().length == 0);
            throw e;
            }
        }

    @Test(expected = IOException.class)
    public void testReadNewerVersion()
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;

        // create a stream with a higher version number
        InputStream in = new ByteArrayInputStream(new byte[] {0x6A, 0x68, 0x37, 0x35, 0x02});
        try
            {
            manager.read(TEST_STORE_ID, in);
            }
        catch (IOException e)
            {
            assertTrue(manager.list().length == 0);
            throw e;
            }
        }

    /**
     * Test APM.read from input stream that is buffered, see COH-18388.
     */
    @Test
    public void testReadFully()
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;

        // create a new PersistentStore and populate it with some data
        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);

        store.ensureExtent(1);

        store.store(1,
                AbstractPersistentStoreTest.BINARY_KEY_1,
                AbstractPersistentStoreTest.BINARY_VALUE_1,
                null);

        File             file = new File(m_fileTrash, "test");
        FileOutputStream out  = new FileOutputStream(file);
        InputStream      in   = null;
        try
            {
            manager.write(TEST_STORE_ID, out);
            out.close();
            assertTrue(file.exists());

            // delete the store
            manager.delete(TEST_STORE_ID, false);
            assertTrue(manager.list().length == 0);

            // read the file into a store
            in = new FileInputStream(file);

            manager.read(TEST_STORE_ID, new TestInputStream(in));
            }
        catch (IOException e)
            {
            throw e;
            }
        finally
            {
            if (in != null)
                {
                in.close();
                }
            assertTrue(file.delete());
            }
        }

    @Test
    public void testWriteRead()
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;

        // create a new PersistentStore and populate it with some data
        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        assertEquals(TEST_STORE_ID, store.getId());
        assertArrayEquals(new String[]{TEST_STORE_ID}, manager.list());

        store.ensureExtent(1);
        store.ensureExtent(2);
        store.store(1,
                AbstractPersistentStoreTest.BINARY_KEY_1,
                AbstractPersistentStoreTest.BINARY_VALUE_1,
                null);
        store.store(2,
                AbstractPersistentStoreTest.BINARY_KEY_2,
                AbstractPersistentStoreTest.BINARY_VALUE_2,
                null);

        for (int i = 0; i < 2; ++i)
            {
            // write the content of the store to a file
            File             file = new File(m_fileTrash, "test");
            FileOutputStream out  = new FileOutputStream(file);
            FileInputStream  in   = null;
            try
                {
                manager.write(TEST_STORE_ID, out);
                out.close();
                assertTrue(file.exists());

                // delete the store
                manager.delete(TEST_STORE_ID, false);
                assertTrue(manager.list().length == 0);

                // read the file into a store
                assertTrue(file.exists());
                in = new FileInputStream(file);
                manager.read(TEST_STORE_ID, in);

                // assert the contents of the store
                assertTrue(manager.list().length == 1);
                store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
                assertEquals(AbstractPersistentStoreTest.BINARY_VALUE_1,
                        store.load(1, AbstractPersistentStoreTest.BINARY_KEY_1));
                assertEquals(AbstractPersistentStoreTest.BINARY_VALUE_2,
                        store.load(2, AbstractPersistentStoreTest.BINARY_KEY_2));

                final int[] anCount = new int[1];
                store.iterate(new PersistentStore.Visitor<ReadBuffer>()
                    {
                    @Override
                    public boolean visit(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue)
                        {
                        anCount[0]++;
                        return true;
                        }
                    });
                assertEquals(2, anCount[0]);
                }
            finally
                {
                out.close();
                if (in != null)
                    {
                    in.close();
                    }
                assertTrue(file.delete());
                }
            }
        }

    /**
     * Custom InputStream to simulate buffered InputStream that read
     * may not return all the bytes requested.
     */
    public static class TestInputStream extends InputStream
        {
        public TestInputStream(InputStream input)
            {
            m_inputStream = input;
            }

        @Override
        public int read() throws IOException
            {
            return m_inputStream.read();
            }

        @Override
        public int read(byte[] ab, int off, int nLen) throws IOException
            {
            if (off == 0)
                {
                return m_inputStream.read(ab, off, nLen / 2);
                }
            else
                {
                return m_inputStream.read(ab, off, nLen);
                }
            }

        // data members
        protected volatile InputStream m_inputStream;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the test persistent store identifier.
     */
    public static String TEST_STORE_ID = AbstractPersistenceEnvironmentTest.TEST_STORE_ID;

    /**
     * The name of test persistent store a persistent store will be opened with.
     */
    public static String TEST_FROM_STORE_ID = GUIDHelper.generateGUID(1, 2L,
                                                                      new Date().getTime(), GUIDHelperTest.getMockMember(1));

    // ----- data members ---------------------------------------------------

    protected File                       m_fileData;
    protected File                       m_fileStore;
    protected File                       m_fileTrash;
    protected DaemonPool                 m_pool;
    protected AbstractPersistenceManager m_manager;
    }
