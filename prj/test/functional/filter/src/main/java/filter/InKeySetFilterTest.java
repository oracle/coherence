/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package filter;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilder;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.InKeySetFilter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class InKeySetFilterTest
    {
    @ClassRule
    public static final CoherenceClusterResource s_clusterRunner = new CoherenceClusterResource()
            .include(2, LocalStorage.enabled())
            .with(ClusterName.of(InKeySetFilterTest.class.getSimpleName() + "Cluster"),
                  SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)));

    public static SessionBuilder MEMBER = SessionBuilders.storageDisabledMember();

    public static SessionBuilder EXTEND = SessionBuilders.extendClient("client-cache-config.xml");

    @Parameterized.Parameters(name = "client={0}, serializer={1}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][] {{MEMBER}, {EXTEND}});
        }

    private final SessionBuilder m_bldrSession;

    public InKeySetFilterTest(SessionBuilder bldrSession)
        {
        m_bldrSession = bldrSession;
        }

    @Test
    public void testInKeySetFilter()
        {
        NamedCache<Integer, Integer> cache = s_clusterRunner
                .createSession(m_bldrSession)
                .ensureCache("test", null);
        
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
