/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;

import com.oracle.coherence.testing.SystemPropertyResource;
import com.oracle.coherence.testing.TestHelper;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.cache.ConfigurableCacheMap;

import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Exercises percentage eviction in a real partitioned cache service.
 *
 * @author Aleks Seovic  2026.11.06
 * @since 27.01
 */
public class LowUnitsPercentageTests
    {
    @Test
    public void shouldRetainPercentagesDuringEvictionAndResize()
        {
        var configuration = CoherenceConfiguration.builder()
                .withSession(SessionConfiguration.builder()
                        .withConfigUri("low-units-percentage-cache-config.xml").build())
                .build();
        try (var cluster = new SystemPropertyResource("coherence.cluster", "low-units-" + UUID.randomUUID());
             var storage = new SystemPropertyResource("coherence.distributed.localstorage", "true");
             var coherence = Coherence.clusterMember(configuration))
            {
            coherence.start().join();
            verifyCache(coherence.getSession().getCache("partitioned"), 0.9, 900);
            }
        }

    private void verifyCache(NamedCache<Integer, Integer> cache, double dflRatio, int cLow)
        {
        var map = (ConfigurableCacheMap) TestHelper.getBackingMap(cache);
        assertEquals(dflRatio, map.getLowUnitsRatio(), 0.0);
        assertEquals(cLow, map.getLowUnits());
        for (int i = 0; i < 2000; i++)
            {
            cache.put(i, i);
            }
        assertTrue("capacity pruning must remove entries", cache.size() < 2000);
        assertTrue("the cache should retain entries", cache.size() > 0);
        map.setHighUnits(2000);
        assertEquals(cLow * 2, map.getLowUnits());
        map.setHighUnits(2000);
        assertEquals(dflRatio, map.getLowUnitsRatio(), 0.0);
        assertEquals(cLow * 2, map.getLowUnits());
        cache.clear();
        }
    }
