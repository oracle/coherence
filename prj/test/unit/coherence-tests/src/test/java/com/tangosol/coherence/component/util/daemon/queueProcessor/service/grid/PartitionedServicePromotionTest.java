/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid;

import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistentStore;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.Member;

import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.GUIDHelper;
import com.tangosol.persistence.bdb.BerkeleyDBManager;
import com.tangosol.persistence.journal.JournalPersistenceManager;

import com.tangosol.util.Binary;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verify that ordinary backing-map promotion preserves a recoverable store
 * until the new active store has been sealed.
 *
 * @author Aleks Seovic  2026.10.26
 * @since 26.10
 */
@RunWith(Parameterized.class)
public class PartitionedServicePromotionTest
    {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> engines()
        {
        return List.of(new Object[] {"Journal"}, new Object[] {"BDB"});
        }

    public PartitionedServicePromotionTest(String sEngine)
        {
        f_sEngine = sEngine;
        }

    @Test
    public void shouldPreserveBackupAfterCrashBeforeActiveSeal()
            throws Exception
        {
        try (PromotionContext context = new PromotionContext(f_sEngine))
            {
            context.releaseManagers();

            String sJava = new File(System.getProperty("java.home"),
                    System.getProperty("os.name").startsWith("Windows") ? "bin/java.exe" : "bin/java").getPath();
            File fileOutput = new File(context.f_dirRoot, "child-output.log");
            Process process = new ProcessBuilder(sJava,
                    "-Dcoherence.cluster=journal-promotion-" + UUID.randomUUID(),
                    "-Dcoherence.log.level=2",
                    "-cp", System.getProperty("java.class.path"),
                    HaltDuringPromotion.class.getName(),
                    context.f_dirActive.getPath(), context.f_dirBackup.getPath(), f_sEngine)
                    .redirectErrorStream(true)
                    .redirectOutput(fileOutput)
                    .start();
            try
                {
                assertTrue("promotion process did not stop", process.waitFor(30, TimeUnit.SECONDS));
                assertEquals(Files.readString(fileOutput.toPath()), HALT_STATUS, process.exitValue());
                }
            finally
                {
                if (process.isAlive())
                    {
                    process.destroyForcibly();
                    assertTrue("promotion process did not terminate", process.waitFor(30, TimeUnit.SECONDS));
                    }
                }

            context.reopenManagers();
            PersistentStore<ReadBuffer> storeActive = context.m_active.open(STORE_ID, null);
            assertThrows(PersistenceException.class, () ->
                    CachePersistenceHelper.validateForActiveRecovery(storeActive, context.f_service));
            context.assertBackupRecoverable();
            }
        }

    @Test
    public void shouldDeleteBackupAfterSuccessfulActiveSeal()
            throws Exception
        {
        try (PromotionContext context = new PromotionContext(f_sEngine))
            {
            context.f_service.promote();

            CachePersistenceHelper.validateForActiveRecovery(context.m_storeActive, context.f_service);
            assertEquals(VALUE, context.m_storeActive.load(1L, KEY));
            assertFalse(context.backupMetadata().exists());
            }
        }

    @Test
    public void shouldPreserveBackupWhenServiceIsExiting()
            throws Exception
        {
        try (PromotionContext context = new PromotionContext(f_sEngine))
            {
            context.f_service.m_fExiting = true;
            context.f_service.promote();

            assertFalse(CachePersistenceHelper.isSealed(context.m_storeActive));
            context.assertBackupRecoverable();
            }
        }

    @Test
    public void shouldPreserveBackupWhenServiceIsStopping()
            throws Exception
        {
        try (PromotionContext context = new PromotionContext(f_sEngine))
            {
            context.f_service.m_fStopping = true;
            context.f_service.promote();

            assertFalse(CachePersistenceHelper.isSealed(context.m_storeActive));
            context.assertBackupRecoverable();
            }
        }

    @Test
    public void shouldPreserveBackupWhenActiveStoreCannotBeOpened()
            throws Exception
        {
        try (PromotionContext context = new PromotionContext(f_sEngine))
            {
            context.m_active.close(STORE_ID);
            context.f_service.f_control.m_fOpenStore = false;
            context.f_service.promote();

            context.assertBackupRecoverable();
            }
        }

    @Test
    public void shouldPreserveBackupWhenActiveSealFails()
            throws Exception
        {
        try (PromotionContext context = new PromotionContext(f_sEngine))
            {
            PersistentStore<ReadBuffer> store = mock(PersistentStore.class);
            when(store.isOpen()).thenReturn(true);
            when(store.begin()).thenThrow(new IllegalStateException("injected seal failure"));
            context.f_service.f_control.configure(store, context.m_backup.open(STORE_ID, null));
            assertThrows(IllegalStateException.class, context.f_service::promote);

            assertFalse(CachePersistenceHelper.isSealed(context.m_storeActive));
            context.assertBackupRecoverable();
            }
        }

    @Test
    public void shouldHonorDirectBackupRetentionAfterActiveSeal()
            throws Exception
        {
        try (PromotionContext context = new PromotionContext(f_sEngine))
            {
            context.f_service.m_fRetainBackup = true;
            context.f_service.promote();

            CachePersistenceHelper.validateForActiveRecovery(context.m_storeActive, context.f_service);
            context.assertBackupRecoverable();
            }
        }

    @Test
    public void shouldReopenActiveStoreBeforeRetiringBackup()
            throws Exception
        {
        try (PromotionContext context = new PromotionContext(f_sEngine))
            {
            context.m_active.close(STORE_ID);
            context.f_service.promote();

            PersistentStore<ReadBuffer> store = context.f_service.f_control.getPersistentStore();
            CachePersistenceHelper.validateForActiveRecovery(store, context.f_service);
            assertEquals(VALUE, store.load(1L, KEY));
            assertFalse(context.backupMetadata().exists());
            }
        }

    @Test
    public void shouldPreserveBackupWhenActiveOpenFails()
            throws Exception
        {
        try (PromotionContext context = new PromotionContext(f_sEngine))
            {
            context.m_active.close(STORE_ID);
            context.f_service.f_control.m_fFailOpen = true;
            assertThrows(IllegalStateException.class, context.f_service::promote);

            context.assertBackupRecoverable();
            }
        }

    @Test
    public void shouldRecoverLaterWritesAfterEmptyPromotion()
            throws Exception
        {
        try (PromotionContext context = new PromotionContext(f_sEngine, false))
            {
            assertFalse(context.m_storeActive.isOpen());
            context.f_service.promote();
            PersistentStore<ReadBuffer> store = context.f_service.f_control.getPersistentStore();
            boolean fSealed = store.isOpen() && CachePersistenceHelper.isSealed(store);

            // later mutations open and seal the current store, but cannot update an obsolete backup
            store = context.m_active.open(STORE_ID, null);
            CachePersistenceHelper.seal(store, context.f_service, null);
            context.populate(store);
            context.releaseManagers();
            context.reopenManagers();

            GUIDHelper.GUIDResolver resolver = new GUIDHelper.GUIDResolver(257);
            Member memberActive = mock(Member.class);
            Member memberBackup = mock(Member.class);
            when(memberActive.getId()).thenReturn(1);
            when(memberBackup.getId()).thenReturn(2);
            resolver.registerStoreInfo(memberActive, context.m_active.listStoreInfo());
            resolver.registerStoreInfo(memberBackup, context.m_backup.listStoreInfo());

            assertEquals("obsolete backup won recovery selection", STORE_ID, resolver.getNewestGUID(92));
            PersistentStore<ReadBuffer> recovered = context.m_active.open(resolver.getNewestGUID(92), null);
            CachePersistenceHelper.validateForActiveRecovery(recovered, context.f_service);
            assertEquals(VALUE, recovered.load(1L, KEY));
            assertTrue("empty active store was not sealed during promotion", fSealed);
            assertFalse("obsolete backup survived promotion", context.backupMetadata().exists());
            }
        }

    // ----- child process --------------------------------------------------

    /**
     * Stop at the quorum boundary immediately before the active-store seal.
     */
    public static class HaltDuringPromotion
        {
        public static void main(String[] asArgs)
                throws Exception
            {
            PersistenceManager<ReadBuffer> active = createManager(asArgs[2], new File(asArgs[0]));
            PersistenceManager<ReadBuffer> backup = createManager(asArgs[2], new File(asArgs[1]));
            TestService service = new TestService(active, backup);
            service.f_control.configure(active.open(STORE_ID, null), backup.open(STORE_ID, null));
            service.m_fHalt = true;
            service.promote();
            throw new AssertionError("promotion process did not halt");
            }
        }

    // ----- test fixtures --------------------------------------------------

    private static PersistenceManager<ReadBuffer> createManager(String sEngine, File directory)
            throws IOException
        {
        return "Journal".equals(sEngine)
                ? new JournalPersistenceManager(directory, null, null)
                : new BerkeleyDBManager(directory, null, null);
        }

    private static class PromotionContext
            implements AutoCloseable
        {
        private PromotionContext(String sEngine)
                throws IOException
            {
            this(sEngine, true);
            }

        private PromotionContext(String sEngine, boolean fPopulated)
                throws IOException
            {
            f_sEngine   = sEngine;
            f_sBackupId = fPopulated ? STORE_ID : BACKUP_ID;
            f_dirRoot   = FileHelper.createTempDir();
            f_dirActive = new File(f_dirRoot, "active");
            f_dirBackup = new File(f_dirRoot, "backup");
            reopenManagers();
            f_service = new TestService(m_active, m_backup);

            PersistentStore<ReadBuffer> storeBackup = m_backup.open(f_sBackupId, null);
            storeBackup.ensureExtent(1L);
            if (fPopulated)
                {
                populate(storeBackup);
                }
            CachePersistenceHelper.seal(storeBackup, f_service, null);

            // ordinary backing maps copy entries before the service seals the active store
            m_storeActive = fPopulated ? m_active.open(STORE_ID, null) : m_active.createStore(STORE_ID);
            if (fPopulated)
                {
                populate(m_storeActive);
                }
            f_service.f_control.configure(m_storeActive, storeBackup);
            }

        private void populate(PersistentStore<ReadBuffer> store)
            {
            store.ensureExtent(0L);
            store.ensureExtent(1L);
            store.store(1L, KEY, VALUE, null);
            }

        private File backupMetadata()
            {
            return new File(new File(f_dirBackup, f_sBackupId), CachePersistenceHelper.META_FILENAME);
            }

        private void assertBackupRecoverable()
            {
            assertTrue("last recoverable backup was removed", backupMetadata().isFile());
            PersistentStore<ReadBuffer> store = m_backup.open(f_sBackupId, null);
            CachePersistenceHelper.validateForActiveRecovery(store, f_service);
            assertEquals(VALUE, store.load(1L, KEY));
            }

        private void releaseManagers()
            {
            if (m_active != null)
                {
                m_active.release();
                m_active = null;
                }
            if (m_backup != null)
                {
                m_backup.release();
                m_backup = null;
                }
            }

        private void reopenManagers()
                throws IOException
            {
            m_active = createManager(f_sEngine, f_dirActive);
            m_backup = createManager(f_sEngine, f_dirBackup);
            }

        @Override
        public void close()
                throws IOException
            {
            releaseManagers();
            FileHelper.deleteDir(f_dirRoot);
            }

        private final File f_dirRoot;
        private final File f_dirActive;
        private final File f_dirBackup;
        private final TestService f_service;
        private final String f_sEngine;
        private final String f_sBackupId;
        private PersistenceManager<ReadBuffer> m_active;
        private PersistenceManager<ReadBuffer> m_backup;
        private PersistentStore<ReadBuffer> m_storeActive;
        }

    private static class TestService
            extends PartitionedService
        {
        private TestService(PersistenceManager<ReadBuffer> active, PersistenceManager<ReadBuffer> backup)
            {
            super(null, null, false);
            f_control = new TestControl(this, active);
            f_persistence = new TestPersistenceControl(backup);
            }

        private void promote()
            {
            super.movePartition(92, 0);
            }

        @Override
        public boolean isActivePersistence()
            {
            return true;
            }

        @Override
        public boolean isBackupPersistence()
            {
            return true;
            }

        @Override
        public boolean isExiting()
            {
            return m_fExiting;
            }

        @Override
        public int getServiceState()
            {
            return m_fStopping ? SERVICE_STOPPING : SERVICE_STARTED;
            }

        @Override
        public int getPartitionCount()
            {
            return 257;
            }

        @Override
        public PartitionControl getPartitionControl(int nPartition)
            {
            return f_control;
            }

        @Override
        public PersistenceControl getPersistenceControl()
            {
            return f_persistence;
            }

        @Override
        protected boolean shouldRetainBackupPersistentStore(int nPartition)
            {
            return m_fRetainBackup;
            }

        @Override
        public void saveQuorum(int nPartition)
            {
            // partition 92 has no quorum metadata; use its no-op boundary for the crash
            if (m_fHalt)
                {
                Runtime.getRuntime().halt(HALT_STATUS);
                }
            }

        private final TestControl f_control;
        private final TestPersistenceControl f_persistence;
        private boolean m_fExiting;
        private boolean m_fStopping;
        private boolean m_fRetainBackup;
        private boolean m_fHalt;
        }

    private static class TestControl
            extends PartitionedService.PartitionControl
        {
        private TestControl(PartitionedService service, PersistenceManager<ReadBuffer> active)
            {
            super(null, null, false);
            f_service = service;
            f_active  = active;
            }

        private void configure(PersistentStore<ReadBuffer> active, PersistentStore<ReadBuffer> backup)
            {
            setPersistentStore(active);
            setPersistentBackupStore(backup);
            }

        @Override
        public PartitionedService getService()
            {
            return f_service;
            }

        @Override
        public PersistentStore<ReadBuffer> ensurePersistentStore()
            {
            return getPersistentStore();
            }

        @Override
        public PersistentStore<ReadBuffer> ensureOpenPersistentStore()
            {
            if (m_fFailOpen)
                {
                throw new IllegalStateException("injected open failure");
                }
            PersistentStore<ReadBuffer> store = getPersistentStore();
            if (!store.isOpen() && m_fOpenStore)
                {
                store = f_active.open(store.getId(), null);
                setPersistentStore(store);
                }
            return store;
            }

        private final PartitionedService f_service;
        private final PersistenceManager<ReadBuffer> f_active;
        private boolean m_fOpenStore = true;
        private boolean m_fFailOpen;
        }

    private static class TestPersistenceControl
            extends PartitionedService.PersistenceControl
        {
        private TestPersistenceControl(PersistenceManager<ReadBuffer> backup)
            {
            super(null, null, false);
            f_backup = backup;
            }

        @Override
        public PersistenceManager<ReadBuffer> getBackupManager()
            {
            return f_backup;
            }

        private final PersistenceManager<ReadBuffer> f_backup;
        }

    private static final int HALT_STATUS = 23;
    private static final String STORE_ID = "92-2-1a16adaabc7-3";
    private static final String BACKUP_ID = "92-3-1a16adaabc7-3";
    private static final Binary KEY = new Binary(new byte[] {1, 8});
    private static final Binary VALUE = new Binary(new byte[] {8});
    private final String f_sEngine;
    }
