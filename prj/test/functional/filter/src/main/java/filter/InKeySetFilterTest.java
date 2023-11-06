/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package filter;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.InKeySetFilter;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InKeySetFilterTest
    {
    @Test
    public void testInKeySetFilter()
        {
        try (Coherence coherence = Coherence.clusterMember().start().join())
            {
            //testing on remote cache
            NamedCache<Integer, Integer> cache = coherence.getSession().getCache("test");
            cache.put(1, 5);
            cache.put(2, 40);
            cache.put(3, 75);
            cache.put(4, 80);
            cache.put(5, 95);
            cache.put(6, 105);
            cache.put(7, Integer.MAX_VALUE);
            cache.put(8, Integer.MIN_VALUE);

            Filter<Integer> inKeySetFilter =
                    new InKeySetFilter<>(new GreaterFilter<>(IdentityExtractor.INSTANCE(), 50),
                                         Set.of(1, 2, 3, 6, 7, 8));

            Set<Integer> results = cache.keySet(inKeySetFilter);

            assertEquals(3, results.size());
            assertTrue(results.contains(3));
            assertTrue(results.contains(6));
            assertTrue(results.contains(7));

            Set<Map.Entry<Integer, Integer>> entries = cache.entrySet(inKeySetFilter);
            assertEquals(3, entries.size());
            }
        }
    }
