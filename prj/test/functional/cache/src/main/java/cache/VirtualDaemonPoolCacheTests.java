/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Focused VT-enabled functional validation for cache-module request flow.
 *
 * @author Aleks Seovic  2026.04.24
 * @since 26.04
 */
public class VirtualDaemonPoolCacheTests
        extends AbstractFunctionalTest
    {
    public VirtualDaemonPoolCacheTests()
        {
        super(FILE_CFG_CACHE);
        }

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.test.daemonpool", "virtual");
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }

    @AfterClass
    public static void _shutdown()
        {
        try
            {
            AbstractFunctionalTest._shutdown();
            }
        finally
            {
            System.clearProperty("coherence.test.daemonpool");
            System.clearProperty("coherence.distributed.localstorage");
            }
        }

    @After
    public void cleanupCache()
        {
        NamedCache<Integer, Integer> cache = getNamedCache(CACHE_NAME);

        if (cache.isActive())
            {
            cache.clear();
            }
        }

    @Test
    public void shouldUseVirtualDaemonPoolForDistributedCacheService()
        {
        NamedCache<Integer, Integer> cache   = getNamedCache(CACHE_NAME);
        PartitionedService           service = (PartitionedService) ((SafeService) cache.getCacheService()).getRunningService();

        assertThat(service.getDaemonPool(), instanceOf(PartitionedService.VirtualDaemonPool.class));
        assertThat(((PartitionedService.VirtualDaemonPool) service.getDaemonPool()).getTaskLimit(), is(0));
        }

    @Test
    public void shouldExecuteBasicNamedCacheOperationsUnderVirtualDaemonPool()
        {
        NamedCache<Integer, Integer> cache = getNamedCache(CACHE_NAME);

        cache.put(1, 10);
        cache.putAll(Map.of(2, 20, 3, 30));

        assertThat(cache.get(1), is(10));
        assertThat(cache.getAll(Set.of(1, 2, 3)), is(Map.of(1, 10, 2, 20, 3, 30)));

        cache.remove(2);

        assertThat(cache.size(), is(2));

        cache.truncate();

        assertThat(cache.size(), is(0));
        }

    @Test
    public void shouldExecuteAsyncNamedCacheOperationsUnderVirtualDaemonPool()
            throws Exception
        {
        NamedCache<Integer, Integer>      cache = getNamedCache(CACHE_NAME);
        AsyncNamedCache<Integer, Integer> async = cache.async();

        assertThat(async.put(1, 100).get(1, TimeUnit.MINUTES), is((Integer) null));

        async.putAll(Map.of(2, 200, 3, 300)).get(1, TimeUnit.MINUTES);

        assertThat(async.get(1).get(1, TimeUnit.MINUTES), is(100));
        assertThat(async.getAll(Set.of(1, 2, 3)).get(1, TimeUnit.MINUTES),
                is(Map.of(1, 100, 2, 200, 3, 300)));
        assertThat(async.remove(2).get(1, TimeUnit.MINUTES), is(200));
        assertThat(cache.getAll(Set.of(1, 2, 3)), is(Map.of(1, 100, 3, 300)));
        }

    @Test
    public void shouldRecoverInterruptibleCacheProcessorWhenGuardTimeoutIsReduced()
        {
        NamedCache<Integer, Integer>           cache   = getNamedCache(CACHE_NAME);
        PartitionedService                     service = (PartitionedService) ((SafeService) cache.getCacheService()).getRunningService();
        PartitionedService.VirtualDaemonPool   pool    = (PartitionedService.VirtualDaemonPool) service.getDaemonPool();
        long                                   cGuard  = service.getDefaultGuardTimeout();
        int                                    cTimed  = pool.getStatsTimeoutCount();

        try
            {
            service.setDefaultGuardTimeout(2_000L);

            assertThat(cache.invoke(1, new InterruptibleSleepProcessor(10_000L)),
                    is(InterruptibleSleepProcessor.RESULT_INTERRUPTED));
            assertThat(pool.getStatsTimeoutCount() > cTimed, is(true));
            assertThat(service.isRunning(), is(true));
            }
        finally
            {
            service.setDefaultGuardTimeout(cGuard);
            }
        }

    public static class InterruptibleSleepProcessor
            extends AbstractProcessor<Integer, Integer, String>
            implements Serializable
        {
        public InterruptibleSleepProcessor(long cDelayMillis)
            {
            m_cDelayMillis = cDelayMillis;
            }

        @Override
        public String process(InvocableMap.Entry<Integer, Integer> entry)
            {
            try
                {
                Blocking.sleep(m_cDelayMillis);
                return RESULT_COMPLETED;
                }
            catch (InterruptedException e)
                {
                return RESULT_INTERRUPTED;
                }
            }

        public static final String RESULT_COMPLETED = "completed";

        public static final String RESULT_INTERRUPTED = "interrupted";

        private final long m_cDelayMillis;
        }

    public static final String FILE_CFG_CACHE = "coherence-cache-config.xml";

    public static final String CACHE_NAME = "dist-vt-cache-test";
    }
