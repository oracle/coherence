/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package partition;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.partition.PartitionSet;
import com.tangosol.net.partition.SimplePartitionKey;

import com.tangosol.util.Base;
import com.tangosol.util.CompositeKey;
import com.tangosol.util.SafeHashMap;

import common.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * A functional test to validate that the 'read-from-closest' feature works as
 * expected. For this feature to work a get or getAll request can be served
 * by a backup owner if the backup own is deemed 'closer' which is to say it is
 * on the same machine, rack or site.
 *
 * @author hr  2021.09.23
 */
public class ReadFromClosestTests
        extends AbstractFunctionalTest
    {

    // ----- set up methods -------------------------------------------------
    
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.machine", "mach-3");
        System.setProperty("coherence.rack", "rack-3");
        System.setProperty("coherence.site", "AZ1");

        setupProps();

        startCluster();
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void testReadFromClosest()
        {
        NamedCache mapIncoherent = getNamedCache("incoherent-reads");
        try
            {
            Properties props = new Properties();
            props.setProperty("coherence.machine", "mach-1");
            props.setProperty("coherence.rack", "rack-1");
            props.setProperty("coherence.site", "AZ2");

            CoherenceClusterMember testPrimary = startCacheServer("incoherent-primary", "partition", null, props);
            
            props.setProperty("coherence.machine", "mach-2");
            props.setProperty("coherence.rack", "rack-2");
            props.setProperty("coherence.site", "AZ1");
            CoherenceClusterMember testSecondary = startCacheServer("incoherent-secondary", "partition", null, props);

            DistributedCacheService service = (DistributedCacheService) mapIncoherent.getCacheService();

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
            waitForBalanced(service);

            Member       memberPrimary = service.getInfo().getServiceMember(testPrimary.getLocalMemberId());
            PartitionSet parts         = service.getOwnedPartitions(memberPrimary);

            Map<Integer, Set<Object>> mapKeysByPart = new HashMap<>();

            for (int i = 2; i >= 0; --i)
                {
                int iPart;

                // skip duplicate partitions
                while (mapKeysByPart.containsKey(iPart = parts.rnd()));

                for (int j = 9; j >= 0; --j)
                    {
                    String sValue = "test-" + j;
                    Object oKey   = new CompositeKey<>(SimplePartitionKey.getPartitionKey(iPart), sValue);

                    Set<Object> setKeys = mapKeysByPart.get(iPart);
                    if (setKeys == null)
                        {
                        mapKeysByPart.put(iPart, setKeys = new HashSet<>());
                        }

                    setKeys.add(oKey);

                    mapIncoherent.put(oKey, sValue);
                    }
                }

            Integer[]   aNParts = (Integer[]) Base.randomize(mapKeysByPart.keySet().toArray(new Integer[0]));
            Integer     IPart   = aNParts[0];
            Set<Object> setKeys = mapKeysByPart.get(IPart);

            Object  oResult = mapIncoherent.get(mapKeysByPart.get(aNParts[1]).iterator().next());
            Integer NReads  = testSecondary.invoke(TrackingBackupMap::getReadsAndReset);

            assertNotNull(oResult);
            assertEquals(1, NReads.intValue());

            Map<Object, String> mapResults = mapIncoherent.getAll(setKeys);

            assertEquals(setKeys.size(), mapResults.size());

            NReads = testSecondary.invoke(TrackingBackupMap::getReadsAndReset);

            assertEquals(setKeys.size(), NReads.intValue());
            }
        finally
            {
            mapIncoherent.destroy();

            stopCacheServer("incoherent-primary");
            stopCacheServer("incoherent-secondary");
            }

        }

    // ----- inner-class: TrackingBackupMap ---------------------------------

    /**
     * A custom backing that allows the counting of reads.
     */
    public static class TrackingBackupMap
            extends SafeHashMap
        {
        @Override
        public Object get(Object oKey)
            {
            Object oValue = super.get(oKey);

            if (oValue != null)
                {
                READ_COUNT.incrementAndGet();
                }

            return oValue;
            }

        public static int getReads()
            {
            return READ_COUNT.get();
            }

        public static int getReadsAndReset()
            {
            int nReads = READ_COUNT.get();
            reset();

            return nReads;
            }

        public static void reset()
            {
            READ_COUNT.set(0);
            }

        protected static final AtomicInteger READ_COUNT = new AtomicInteger();
        }

    // ----- console support ------------------------------------------------

    public static int getBackupReads()
        {
        return TrackingBackupMap.getReads();
        }
    }
