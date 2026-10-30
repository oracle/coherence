/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.persistence.PersistenceManager;

import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.journal.JournalPersistenceManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;

import java.util.Collection;
import java.util.List;

/**
 * Verify ordinary backing-map recovery after empty partitions are promoted
 * during rolling ownership changes, using both persistence engines.
 *
 * @author Aleks Seovic  2026.10.29
 * @since 27.01
 */
@RunWith(Parameterized.class)
public class BackupPromotionRecoveryTests
        extends BerkeleyDBSimplePersistenceTests
    {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> engines()
        {
        return List.of(new Object[] {"BDB"}, new Object[] {"Journal"});
        }

    public BackupPromotionRecoveryTests(String sEngine)
        {
        f_sEngine = sEngine;
        }

    @Test(timeout = 240_000)
    public void testEmptyPartitionRollingRecovery()
            throws IOException
        {
        testBackupPersistence("active-backup", 4, "simple-persistent", true);
        }

    @Override
    protected PersistenceManager<ReadBuffer> createPersistenceManager(File file)
            throws IOException
        {
        return "Journal".equals(f_sEngine)
                ? new JournalPersistenceManager(file, null, null)
                : super.createPersistenceManager(file);
        }

    @Override
    public String getPersistenceManagerName()
        {
        return f_sEngine;
        }

    @Override
    public String getCacheConfigPath()
        {
        return "Journal".equals(f_sEngine)
                ? "simple-persistence-journal-cache-config.xml"
                : super.getCacheConfigPath();
        }

    private final String f_sEngine;
    }
