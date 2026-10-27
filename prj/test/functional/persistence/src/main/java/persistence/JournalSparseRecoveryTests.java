/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;

import com.tangosol.io.FileHelper;

import com.tangosol.net.Coherence;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedMap;

import java.io.File;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Full-process restart coverage for empty and sparsely populated journal maps.
 * These tests use the default recovery policy, not the permissive test policy.
 *
 * @author Aleks Seovic  2026.10.26
 * @since 26.10
 */
public class JournalSparseRecoveryTests
    {
    @Test
    public void testEmptyRestartRecovery()
            throws Exception
        {
        testRestart("empty");
        }

    @Test
    public void testSparseRestartRecovery()
            throws Exception
        {
        testRestart("sparse");
        }

    @Test
    public void testInitializedRestartRecovery()
            throws Exception
        {
        testRestart("initialized");
        }

    private void testRestart(String sPopulation)
            throws Exception
        {
        Path   dir      = Files.createTempDirectory("journal-" + sPopulation + '-');
        String sCluster = "journal-sparse-" + UUID.randomUUID();
        int    nPort;
        try (ServerSocket socket = new ServerSocket(0))
            {
            nPort = socket.getLocalPort();
            }
        try
            {
            runMember(dir, sCluster, nPort, sPopulation, "write");
            runMember(dir, sCluster, nPort, sPopulation, "recover");
            }
        finally
            {
            FileHelper.deleteDirSilent(dir.toFile());
            }
        }

    private void runMember(Path dir, String sCluster, int nPort, String sPopulation, String sPhase)
            throws Exception
        {
        Path fileLog = dir.resolve(sPhase + ".log");
        // run the child on a flat class path even when failsafe uses JPMS
        String sClassPath = System.getProperty("java.class.path") + File.pathSeparator
                + System.getProperty("jdk.module.path", "");
        Process process = new ProcessBuilder(new File(System.getProperty("java.home"), "bin/java").getPath(),
                "-cp", sClassPath,
                "-Dcoherence.cluster=" + sCluster,
                "-Dcoherence.wka=127.0.0.1", "-Dcoherence.localhost=127.0.0.1",
                "-Dcoherence.clusterport=" + nPort, "-Dcoherence.log.level=5",
                "-Dcoherence.override=journal-sparse-recovery-override.xml",
                "-Dcoherence.cacheconfig=journal-sparse-recovery-cache-config.xml",
                "-Dtest.sparse.active=" + dir.resolve("active").toAbsolutePath(),
                "-Dtest.sparse.backup=" + dir.resolve("backup").toAbsolutePath(),
                "-Dtest.sparse.snapshot=" + dir.resolve("snapshot").toAbsolutePath(),
                "-Dtest.sparse.trash=" + dir.resolve("trash").toAbsolutePath(),
                RecoveryMember.class.getName(), sPopulation, sPhase)
                .redirectErrorStream(true)
                .redirectOutput(fileLog.toFile())
                .start();
        boolean fExited;
        try
            {
            fExited = process.waitFor(60, TimeUnit.SECONDS);
            }
        finally
            {
            if (process.isAlive())
                {
                process.destroyForcibly();
                assertTrue("recovery member did not stop", process.waitFor(30, TimeUnit.SECONDS));
                }
            }

        String sLog = Files.readString(fileLog);
        assertTrue("recovery member timed out:\n" + sLog, fExited);
        assertEquals(sLog, 0, process.exitValue());
        assertFalse(sLog, sLog.contains("Failed to recover partition"));
        assertTrue(sLog, sLog.contains("JOURNAL_RECOVERY_PASSED"));
        }

    /**
     * Isolated member used for both sides of a full-process restart.
     */
    public static class RecoveryMember
        {
        public static void main(String[] asArgs)
                throws Exception
            {
            String sPopulation = asArgs[0];
            boolean fWrite     = asArgs[1].equals("write");
            try (Coherence coherence = Coherence.clusterMember())
                {
                coherence.start().join();
                NamedMap<String, String> map = coherence.getSession().getMap("orders");
                DistributedCacheService service = (DistributedCacheService) map.getService();
                PartitionedService component = (PartitionedService) ((SafeService) service).getRunningService();

                // a readable populated partition alone does not prove recovery:
                // every empty partition must also regain ownership
                long ldtEnd = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                while ((service.getOwnedPartitions(service.getCluster().getLocalMember()).cardinality()
                        != service.getPartitionCount() || component.isRecoveryInProgress())
                        && System.nanoTime() < ldtEnd)
                    {
                    Thread.sleep(10);
                    }
                assertEquals("all partitions must recover", service.getPartitionCount(),
                        service.getOwnedPartitions(service.getCluster().getLocalMember()).cardinality());
                assertFalse("initial recovery must finish before shutdown", component.isRecoveryInProgress());

                if (sPopulation.equals("initialized") && fWrite)
                    {
                    Set<Integer> setPartitions = new HashSet<>();
                    for (int i = 0; setPartitions.size() < service.getPartitionCount() && i < 10000; i++)
                        {
                        String sKey = "partition-proof-" + i;
                        if (setPartitions.add(service.getKeyPartitioningStrategy().getKeyPartition(sKey)))
                            {
                            map.put(sKey, "initialized");
                            }
                        }
                    assertEquals(service.getPartitionCount(), setPartitions.size());
                    }
                if (!sPopulation.equals("empty") && fWrite)
                    {
                    map.put("recovery-proof", "durable");
                    }

                int cExpected = sPopulation.equals("empty") ? 0
                        : sPopulation.equals("sparse") ? 1 : service.getPartitionCount() + 1;
                // size() opens every partition's store, hiding the sparse case
                if (!fWrite)
                    {
                    assertEquals(cExpected, map.size());
                    }
                if (cExpected != 0)
                    {
                    assertEquals("durable", map.get("recovery-proof"));
                    }
                System.out.println("JOURNAL_RECOVERY_PASSED");
                }
            }
        }
    }
