/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ViewSchemeDefaultsTests
    {
    @BeforeClass
    public static void setup() throws Exception
        {
        System.setProperty("coherence.cacheconfig", "view-cache-config.xml");
        System.setProperty("coherence.distributed.partitioncount", "13");
        System.setProperty("coherence.distributed.backupcount", "2");

        Coherence coherence = Coherence.clusterMember().startAndWait();
        session = coherence.getSession();
        }

    @AfterClass
    public static void cleanup()
        {
        Coherence.closeAll();
        }

    @Test
    public void shouldHaveCorrectSerializerWithBackSchemeRef() throws Exception
        {
        NamedCache<?, ?> cache   = session.getCache("view-with-ref");
        CacheService     service = cache.getCacheService();
        assertThat(service.getSerializer(), is(instanceOf(ConfigurablePofContext.class)));
        }

    @Test
    public void shouldHaveCorrectSerializerWithBackScheme() throws Exception
        {
        NamedCache<?, ?> cache   = session.getCache("view-with-back");
        CacheService     service = cache.getCacheService();
        assertThat(service.getSerializer(), is(instanceOf(ConfigurablePofContext.class)));
        }

    @Test
    public void shouldHaveCorrectSerializerWithNoBackScheme() throws Exception
        {
        NamedCache<?, ?> cache   = session.getCache("view-with-no-back");
        CacheService     service = cache.getCacheService();
        assertThat(service.getSerializer(), is(instanceOf(ConfigurablePofContext.class)));
        }

    @Test
    public void shouldHaveCorrectPartitionCountWithBackSchemeRef() throws Exception
        {
        NamedCache<?, ?>        cache   = session.getCache("view-with-ref");
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        assertThat(service.getPartitionCount(), is(13));
        }

    @Test
    public void shouldHaveCorrectPartitionCountWithBackScheme() throws Exception
        {
        NamedCache<?, ?> cache   = session.getCache("view-with-back");
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        assertThat(service.getPartitionCount(), is(13));
        }

    @Test
    public void shouldHaveCorrectPartitionCountWithNoBackScheme() throws Exception
        {
        NamedCache<?, ?> cache   = session.getCache("view-with-no-back");
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        assertThat(service.getPartitionCount(), is(13));
        }

    @Test
    public void shouldHaveCorrectBackupCountWithBackSchemeRef() throws Exception
        {
        NamedCache<?, ?>        cache   = session.getCache("view-with-ref");
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        assertThat(service.getBackupCount(), is(2));
        }

    @Test
    public void shouldHaveCorrectBackupCountWithBackScheme() throws Exception
        {
        NamedCache<?, ?>        cache   = session.getCache("view-with-back");
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        assertThat(service.getBackupCount(), is(2));
        }

    @Test
    public void shouldHaveCorrectBackupCountWithNoBackScheme() throws Exception
        {
        NamedCache<?, ?>        cache   = session.getCache("view-with-no-back");
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        assertThat(service.getBackupCount(), is(2));
        }

    // ----- data members ---------------------------------------------------

    private static Session session;
    }
