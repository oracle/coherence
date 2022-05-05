/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package partition;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.net.partition.SimplePartitionKey;

import com.tangosol.util.Base;
import com.tangosol.util.CompositeKey;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import static org.junit.Assert.assertTrue;

/**
 * A functional test to validate that changes in primary are sent to the backup
 * nodes via scheduled backup tasks. There should be no data loss if primary
 * node restart happen afters the configured send backup interval.
 *
 * @author bbc  2021.11.10
 */
public class ScheduledBackupTests
        extends AbstractFunctionalTest
    {

    // ----- set up methods -------------------------------------------------
    
    @BeforeClass
    public static void _startup()
        {
        setupProps();

        startCluster();
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void testBackupFromPut()
        {
        NamedCache cache = getNamedCache("dist-1");
        try
            {
            Properties props = new Properties();
            // test system property
            props.setProperty("coherence.distributed.asyncbackup", "10s");

            CoherenceClusterMember testPrimary   = startCacheServer("testScheduledBackupFromPut-1", "partition", null, props);
            CoherenceClusterMember testSecondary = startCacheServer("testScheduledBackupFromPut-2", "partition", null, props);

            DistributedCacheService service = (DistributedCacheService) cache.getCacheService();

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
            waitForBalanced(service);

            Member       memberPrimary = service.getInfo().getServiceMember(testPrimary.getLocalMemberId());
            PartitionSet parts         = service.getOwnedPartitions(memberPrimary);
            int          cKeys         = parts.cardinality();
            Set<Object>  setKeys       = new HashSet<>(cKeys);
            long         lInterval     = 10_000l; //10s
            for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
                {
                String sValue = "test-" + iPart;
                Object oKey   = new CompositeKey<>(SimplePartitionKey.getPartitionKey(iPart), sValue);

                setKeys.add(oKey);

                cache.put(oKey, sValue);
                }

            assertTrue(cache.size() == cKeys);

            Base.sleep(lInterval + 1000l);
            // the scheduled backup should already happened; stop the primary node should not lead to data loss
            stopCacheServer("testScheduledBackupFromPut-1");
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));
            waitForBalanced(service);

            assertTrue(cache.size() == cKeys);
            }
        finally
            {
            cache.destroy();

            stopAllApplications();
            }
        }

    @Test
    public void testBackupFromPutAll()
        {
        testBackupFromPutAll("testScheduledBackupFromPutAll", 10_000l, true);
        }

    @Test
    public void testBackupFromPutAllNoWait()
        {
        testBackupFromPutAll("testScheduledBackupFromPutAllNoWait", 30_000l, false);
        }

    public void testBackupFromPutAll(String sServerPrefix, long cInterval, boolean fExpected)
        {
        NamedCache cache = getNamedCache("sched-1");
        try
            {
            Properties props = new Properties();
            // test config-driven value, overriden
            props.setProperty("coherence.distributed.custom.asyncbackup", String.valueOf(cInterval) + "ms");

            CoherenceClusterMember testPrimary   = startCacheServer(sServerPrefix + "-1", "partition", null, props);
            CoherenceClusterMember testSecondary = startCacheServer(sServerPrefix + "-2", "partition", null, props);

            DistributedCacheService service = (DistributedCacheService) cache.getCacheService();

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
            waitForBalanced(service);

            Member               memberPrimary = service.getInfo().getServiceMember(testPrimary.getLocalMemberId());
            PartitionSet         parts         = service.getOwnedPartitions(memberPrimary);
            int                  cKeys         = parts.cardinality();
            Map<Object, String>  mapKeys       = new HashMap<>(cKeys);
            for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
                {
                String sValue = "test-" + iPart;
                Object oKey   = new CompositeKey<>(SimplePartitionKey.getPartitionKey(iPart), sValue);

                mapKeys.put(oKey, sValue);
                }

            cache.putAll(mapKeys);

            assertTrue(cache.size() == cKeys);

            // wait for backup to take place
            if (fExpected)
                {
                Base.sleep(cInterval + 1000l);
                }
            // the scheduled backup should already happened; stop the primary node should not lead to data loss
            // (unless it is the intended outcome)
            stopCacheServer(sServerPrefix + "-1");
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));
            waitForBalanced(service);

            Eventually.assertThat(invoking(cache).size(), fExpected ? is(cKeys) : not(cKeys));
            }
        finally
            {
            cache.destroy();

            stopAllApplications();
            }
        }

    @Test
    public void testBackupFromPartitionTransfer()
        {
        NamedCache cache = getNamedCache("sched-1");
        try
            {
            // test config-driven value
            Properties props = new Properties();

            CoherenceClusterMember testPrimary   = startCacheServer("testScheduledBackupFromTransfer-1", "partition", null, props);
            CoherenceClusterMember testSecondary = startCacheServer("testScheduledBackupFromTransfer-2", "partition", null, props);

            DistributedCacheService service  = (DistributedCacheService) cache.getCacheService();
            String                  sService = service.getInfo().getServiceName();

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
            waitForBalanced(service);

            Member               memberPrimary = service.getInfo().getServiceMember(testPrimary.getLocalMemberId());
            PartitionSet         parts         = service.getOwnedPartitions(memberPrimary);
            int                  cKeys         = parts.cardinality();
            Map<Object, String>  mapKeys       = new HashMap<>(cKeys);
            long                 lInterval     = 10_000l; //10s
            for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
                {
                String sValue = "test-" + iPart;
                Object oKey   = new CompositeKey<>(SimplePartitionKey.getPartitionKey(iPart), sValue);

                mapKeys.put(oKey, sValue);
                }

            cache.putAll(mapKeys);

            assertTrue(cache.size() == cKeys);

            // the scheduled backup does not happen yet; start new members should lead to partition transfer
            CoherenceClusterMember memberControl = startCacheServer("testScheduledBackupFromTransfer-3", "partition", null, props);
            startCacheServer("testScheduledBackupFromTransfer-4", "partition", null, props);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(4));
            waitForBalanced(service);

            PartitionSet partsNew = service.getOwnedPartitions(memberPrimary);
            // part of owned partitions should be transferred out
            assertTrue(!partsNew.contains(parts));

            Eventually.assertThat(invoking(cache).size(), is(cKeys));

            // make sure the SendBackupTask run
            Base.sleep(lInterval + 1000);

            stopCacheServer("testScheduledBackupFromTransfer-1");

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
            Eventually.assertThat(invoking(this)
                            .getServiceStatus(memberControl, sService), is(ServiceStatus.NODE_SAFE.name()), within(3, TimeUnit.MINUTES));
            waitForBalanced(service);

            Eventually.assertThat(invoking(cache).size(), is(cKeys));

            stopCacheServer("testScheduledBackupFromTransfer-2");

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
            Eventually.assertThat(invoking(this)
                    .getServiceStatus(memberControl, sService), is(ServiceStatus.NODE_SAFE.name()), within(3, TimeUnit.MINUTES));
            waitForBalanced(service);

            Eventually.assertThat(invoking(cache).size(), is(cKeys));
            }
        finally
            {
            cache.destroy();

            stopAllApplications();
            }
        }

    // ----- Helper methods -------------------------------------------------

    public String getServiceStatus(CoherenceClusterMember member, String sService)
        {
        return member.getServiceStatus(sService).name();
        }
    }
