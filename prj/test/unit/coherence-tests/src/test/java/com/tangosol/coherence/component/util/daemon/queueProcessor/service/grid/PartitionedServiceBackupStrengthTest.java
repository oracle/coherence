/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;

import com.tangosol.net.partition.Ownership;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.net.partition.VersionedOwnership;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests backup strength and transfer ordering with multiple backups.
 *
 * @author phf  2026.10.30
 * @since 27.01
 */
public class PartitionedServiceBackupStrengthTest
    {
    @Test
    public void shouldCheckEveryOwnerPairForMachineSafety()
        {
        TestService service = new TestService();
        for (int[] owners : new int[][]
                {
                {17, 16, 18}, {17, 18, 16}, {16, 17, 18},
                {16, 18, 17}, {18, 17, 16}, {18, 16, 17}
                })
            {
            assertEquals(Arrays.toString(owners), PartitionedService.BACKUP_STRENGTH_NODE,
                    service.getBackupStrength(owners));
            }
        }

    @Test
    public void shouldDetectMissingLastBackup()
        {
        TestService service = new TestService();

        assertEquals(PartitionedService.BACKUP_STRENGTH_ENDANGERED,
                service.getBackupStrength(new int[] {24, 14, 0}));
        assertEquals(PartitionedService.BACKUP_STRENGTH_ENDANGERED,
                service.getBackupStrength(new int[] {17, 18, 0}));
        }

    @Test
    public void shouldReportUnsafePartitionInLocalAndGlobalServiceStatus()
        {
        TestService service = new TestService();
        service.assignments[12] = new int[] {17, 16, 18};

        assertEquals(PartitionedService.BACKUP_STRENGTH_NODE, service.getBackupStrength(true));
        assertEquals(PartitionedService.BACKUP_STRENGTH_NODE, service.getBackupStrength());
        }

    @Test
    public void shouldDetectMissingPrimary()
        {
        assertEquals(PartitionedService.BACKUP_STRENGTH_ORPHANED,
                new TestService().getBackupStrength(new int[] {0, 14, 18}));
        }

    @Test
    public void shouldRequireBackupsForSafety()
        {
        TestService service = new TestService(0);

        assertEquals(PartitionedService.BACKUP_STRENGTH_ENDANGERED,
                service.getBackupStrength(new int[] {24}));
        }

    @Test
    public void shouldPreserveSingleBackupSafety()
        {
        TestService service = new TestService(1);

        assertEquals(PartitionedService.BACKUP_STRENGTH_MACHINE,
                service.getBackupStrength(new int[] {24, 14}));
        assertEquals(PartitionedService.BACKUP_STRENGTH_NODE,
                service.getBackupStrength(new int[] {17, 18}));
        assertEquals(PartitionedService.BACKUP_STRENGTH_ENDANGERED,
                service.getBackupStrength(new int[] {24, 0}));
        }

    @Test
    public void shouldFindWeakestRackAndSitePlacement()
        {
        TestService service = new TestService();
        service.members.add(new TestMember(1, 1, "rack-a", "site-a"));
        service.members.add(new TestMember(2, 2, "rack-b", "site-b"));
        service.members.add(new TestMember(3, 3, "rack-c", "site-a"));
        service.members.add(new TestMember(4, 4, "rack-a", "site-a"));
        service.members.add(new TestMember(5, 5, "rack-c", "site-c"));

        assertEquals(PartitionedService.BACKUP_STRENGTH_RACK,
                service.getBackupStrength(new int[] {1, 2, 3}));
        assertEquals(PartitionedService.BACKUP_STRENGTH_MACHINE,
                service.getBackupStrength(new int[] {1, 2, 4}));
        assertEquals(PartitionedService.BACKUP_STRENGTH_SITE,
                service.getBackupStrength(new int[] {1, 2, 5}));
        }

    @Test
    public void shouldCheckAdditionalBackups()
        {
        TestService service = new TestService(3);

        assertEquals(PartitionedService.BACKUP_STRENGTH_NODE,
                service.getBackupStrength(new int[] {24, 14, 20, 25}));
        assertEquals(PartitionedService.BACKUP_STRENGTH_ENDANGERED,
                service.getBackupStrength(new int[] {24, 14, 20, 0}));
        }

    @Test
    public void shouldEstablishSurvivingBackupBeforeMovingPrimary()
        {
        TestService      service      = new TestService();
        TestDistribution distribution = new TestDistribution(service);
        TestManager      manager      = new TestManager(distribution);
        Ownership        goal         = owners(17, 16, 20);

        Map<Ownership, PartitionSet> suggestions = new HashMap<>();
        suggestions.put(goal, service.instantiatePartitionSet(12));
        manager.suggestForTest(suggestions);

        Map<?, ?> safe = manager.getSafeSuggestions();

        assertEquals(PartitionedService.BACKUP_STRENGTH_MACHINE, distribution.getTargetStrength());
        assertArrayEquals(new int[] {24, 14, 20}, ownershipFor(safe, 12).getOwners());
        assertNotNull(manager.getSuggestionMap());
        assertTrue(((PartitionSet) manager.getSuggestionMap().get(goal)).contains(12));

        // the deferred goal can proceed once the surviving backup has committed
        service.assignments[12] = new int[] {24, 14, 20};
        assertArrayEquals(goal.getOwners(), ownershipFor(manager.getSafeSuggestions(), 12).getOwners());
        }

    @Test
    public void shouldDeferBackupTransferUntilSurvivingBackupCommits()
        {
        TestService      service      = new TestService();
        TestDistribution distribution = new TestDistribution(service);
        service.assignments[12] = new int[] {17, 24, 18};
        distribution.setTargetStrength(PartitionedService.BACKUP_STRENGTH_MACHINE);
        distribution.pending(2, 20, 12);

        assertTrue(distribution.defer(12, 1, 16));
        }

    @Test
    public void shouldAllowBackupTransferAfterSurvivingBackupCommits()
        {
        TestService      service      = new TestService();
        TestDistribution distribution = new TestDistribution(service);
        service.assignments[12] = new int[] {17, 24, 20};
        distribution.setTargetStrength(PartitionedService.BACKUP_STRENGTH_MACHINE);

        assertFalse(distribution.defer(12, 1, 16));
        }

    private static Ownership owners(int... members)
        {
        Ownership owners = new Ownership(members.length - 1);
        owners.setOwners(members);
        return owners;
        }

    private static Ownership ownershipFor(Map<?, ?> suggestions, int partition)
        {
        for (Map.Entry<?, ?> entry : suggestions.entrySet())
            {
            if (((PartitionSet) entry.getValue()).contains(partition))
                {
                return (Ownership) entry.getKey();
                }
            }
        throw new AssertionError("No suggestion for partition " + partition);
        }

    private static class TestService extends PartitionedService
        {
        TestService()
            {
            this(2);
            }

        TestService(int backups)
            {
            super(null, null, false);
            this.backups = backups;
            for (int id = 14; id <= 25; id++)
                {
                members.add(new TestMember(id, 100 + (id - 14) / 3, null, null));
                }
            for (int partition = 0; partition < assignments.length; partition++)
                {
                assignments[partition] = Arrays.copyOf(new int[] {24, 14, 18, 20}, backups + 1);
                }
            }

        @Override
        public int getBackupCount()
            {
            return backups;
            }

        @Override
        public int getPartitionCount()
            {
            return assignments.length;
            }

        @Override
        public int[][] getPartitionAssignments()
            {
            return assignments;
            }

        @Override
        public VersionedOwnership getPartitionConfig(int partition)
            {
            VersionedOwnership owners = new VersionedOwnership(backups, 1);
            owners.setOwners(assignments[partition]);
            return owners;
            }

        @Override
        public ServiceMemberSet getServiceMemberSet()
            {
            return members;
            }

        final int backups;
        final int[][] assignments = new int[13][];
        final ServiceMemberSet members = new ServiceMemberSet();
        }

    private static class TestMember extends Member
        {
        TestMember(int id, int machine, String rack, String site)
            {
            super(null, null, false);
            setId(id);
            setMachineId(machine);
            setRackName(rack);
            setSiteName(site);
            }
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static class TestDistribution extends PartitionedService.CentralDistribution
        {
        TestDistribution(TestService service)
            {
            super("Distribution", service, false);
            setService(service);
            setPendingChanges(new Map[] {new HashMap<>(), new HashMap<>(), new HashMap<>()});
            }

        void pending(int store, int member, int partition)
            {
            getPendingChanges(store).put(member, getService().instantiatePartitionSet(partition));
            }

        boolean defer(int partition, int store, int member)
            {
            return deferBackupTransfer(partition, store, member);
            }
        }

    private static class TestManager extends PartitionedService.CentralDistribution.DistributionManager
        {
        TestManager(TestDistribution distribution)
            {
            super("Manager", distribution, false);
            }

        void suggestForTest(Map<Ownership, PartitionSet> suggestions)
            {
            setSuggestionMap(suggestions);
            }
        }
    }
