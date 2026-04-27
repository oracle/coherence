/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package processor;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;

import com.tangosol.net.NamedCache;
import com.tangosol.net.ServiceStoppedException;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.Base;
import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.AsynchronousProcessor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;

import java.util.concurrent.ExecutionException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Focused VT-enabled functional validation for processor-module request flow.
 *
 * @author Aleks Seovic  2026.04.24
 * @since 26.04
 */
public class VirtualDaemonPoolProcessorTests
        extends AbstractFunctionalTest
    {
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
        getNamedCache(CACHE_NAME).clear();
        }

    @Test
    public void shouldUseVirtualDaemonPoolForProcessorService()
        {
        NamedCache<Integer, Integer> cache   = getNamedCache(CACHE_NAME);
        PartitionedService           service = (PartitionedService) ((SafeService) cache.getCacheService()).getRunningService();

        assertThat(service.getDaemonPool(), instanceOf(PartitionedService.VirtualDaemonPool.class));
        assertThat(((PartitionedService.VirtualDaemonPool) service.getDaemonPool()).getTaskLimit(), is(0));
        }

    @Test
    public void shouldExecuteEntryProcessorsUnderVirtualDaemonPool()
            throws Exception
        {
        NamedCache<Integer, Integer> cache = getNamedCache(CACHE_NAME);

        cache.putAll(Map.of(1, 1, 2, 10, 3, 100));

        Map<Integer, Integer> mapResult = cache.invokeAll(Set.of(1, 2, 3), new IncrementProcessor());
        assertThat(mapResult.get(1), is(2));
        assertThat(mapResult.get(2), is(11));
        assertThat(mapResult.get(3), is(101));

        AsynchronousProcessor<Integer, Integer, Integer> processor =
                new AsynchronousProcessor<>(new IncrementProcessor());
        cache.invokeAll(Set.of(1, 2, 3), processor);

        Map<Integer, Integer> mapAsync = processor.get(30, TimeUnit.SECONDS);

        assertThat(mapAsync.get(1), is(3));
        assertThat(mapAsync.get(2), is(12));
        assertThat(mapAsync.get(3), is(102));
        assertThat(cache.get(1), is(3));
        assertThat(cache.get(2), is(12));
        assertThat(cache.get(3), is(102));
        }

    @Test
    public void shouldRecoverInterruptibleEntryProcessorWhenGuardTimeoutIsReduced()
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

    @Test
    public void shouldCompleteSequentialNoOpInvokesWithoutDeferredPollStall()
        {
        NamedCache<Integer, Integer> cache = getNamedCache(CACHE_NAME);

        cache.put(1, 1);

        for (int i = 0; i < 10; i++)
            {
            assertThat(cache.invoke(1, NoOpProcessor.INSTANCE), is(1));
            }

        long ldtStart = System.nanoTime();
        for (int i = 0; i < 200; i++)
            {
            assertThat(cache.invoke(1, NoOpProcessor.INSTANCE), is(1));
            }
        long cElapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - ldtStart);

        assertThat("Sequential VDP no-op invokes should not stall behind deferred cooperative poll wakeups",
                cElapsedMillis < 5_000L, is(true));
        }

    @Test
    public void shouldReportAsyncProcessorFailureWhenVirtualPoolServiceStops()
            throws Exception
        {
        NamedCache<Integer, Integer> cache = getNamedCache(CACHE_NAME);
        AtomicReference<Throwable>   error = new AtomicReference<>();

        cache.put(1, 1);

        RecordingAsyncProcessor processor =
                new RecordingAsyncProcessor(new SlowIncrementProcessor(10_000L), error);

        cache.invoke(1, processor);
        cache.getCacheService().stop();

        try
            {
            processor.get(30, TimeUnit.SECONDS);
            }
        catch (ExecutionException e)
            {
            assertThat(e.getCause(), instanceOf(ServiceStoppedException.class));
            }

        assertThat(error.get(), instanceOf(ServiceStoppedException.class));
        }

    public static class IncrementProcessor
            extends AbstractProcessor<Integer, Integer, Integer>
            implements Serializable
        {
        @Override
        public Integer process(InvocableMap.Entry<Integer, Integer> entry)
            {
            Integer nValue = entry.getValue();
            int     nNext  = (nValue == null ? 0 : nValue) + 1;

            entry.setValue(nNext);
            return nNext;
            }
        }

    public static class SlowIncrementProcessor
            extends AbstractProcessor<Integer, Integer, Integer>
            implements Serializable
        {
        public SlowIncrementProcessor(long cDelayMillis)
            {
            m_cDelayMillis = cDelayMillis;
            }

        @Override
        public Integer process(InvocableMap.Entry<Integer, Integer> entry)
            {
            Base.sleep(m_cDelayMillis);

            Integer nValue = entry.getValue();
            int     nNext  = (nValue == null ? 0 : nValue) + 1;

            entry.setValue(nNext);
            return nNext;
            }

        private final long m_cDelayMillis;
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

    public static class NoOpProcessor
            extends AbstractProcessor<Integer, Integer, Integer>
            implements Serializable
        {
        @Override
        public Integer process(InvocableMap.Entry<Integer, Integer> entry)
            {
            return entry.getValue();
            }

        public static final NoOpProcessor INSTANCE = new NoOpProcessor();
        }

    public static class RecordingAsyncProcessor
            extends AsynchronousProcessor<Integer, Integer, Integer>
        {
        public RecordingAsyncProcessor(InvocableMap.EntryProcessor<Integer, Integer, Integer> processor,
                AtomicReference<Throwable> error)
            {
            super(processor);
            f_error = error;
            }

        @Override
        public void onException(Throwable eReason)
            {
            f_error.compareAndSet(null, eReason);
            super.onException(eReason);
            }

        private final AtomicReference<Throwable> f_error;
        }

    protected static final String CACHE_NAME = "dist-pool-test1";
    }
