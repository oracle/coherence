/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package util;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.base.NonBlocking;
import com.oracle.coherence.common.base.Notifier;
import com.oracle.coherence.common.base.SingleWaiterMultiNotifier;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.InvocationObserver;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;

import com.tangosol.util.Base;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Functional validation for the conservative Phase 1 VirtualDaemonPool path.
 *
 * @author Aleks Seovic  2026.04.24
 * @since 26.04
 */
public class VirtualDaemonPoolTests
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.test.daemonpool", "virtual");
        System.setProperty("coherence.events.limit", "32");

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
            System.clearProperty("coherence.events.limit");
            }
        }

    @Test
    public void shouldUseVirtualDaemonPoolForInvocationService()
        {
        InvocationService service = (InvocationService) CacheFactory.getService("invocation");
        Grid              grid    = (Grid) ((SafeService) service).getRunningService();

        assertThat(grid.getDaemonPool(), instanceOf(Service.VirtualDaemonPool.class));
        assertThat(((Service.VirtualDaemonPool) grid.getDaemonPool()).getTaskLimit(), is(0));
        }

    @Test
    public void shouldPreserveAssociatedOrderingUnderVirtualDaemonPool()
            throws Exception
        {
        InvocationService         service      = (InvocationService) CacheFactory.getService("invocation");
        AtomicReference<Throwable> failure     = new AtomicReference<>();
        CountDownLatch             latchClients = new CountDownLatch(ASSOCIATION_COUNT);

        s_aiCounter = new int[ASSOCIATION_COUNT];

        for (int i = 0; i < ASSOCIATION_COUNT; i++)
            {
            new Client(service, i, failure, latchClients).start();
            }

        assertTrue(latchClients.await(30, TimeUnit.SECONDS));

        Throwable thrown = failure.get();
        if (thrown != null)
            {
            throw Base.ensureRuntimeException(thrown);
            }
        }

    @Test
    public void shouldEngageBackpressureUnderSaturation()
            throws Exception
        {
        InvocationService         service    = (InvocationService) CacheFactory.getService(BACKPRESSURE_SERVICE);
        Grid                      grid       = (Grid) ((SafeService) service).getRunningService();
        Service.VirtualDaemonPool pool       = (Service.VirtualDaemonPool) grid.getDaemonPool();
        Service.EventDispatcher   dispatcher = grid.ensureEventDispatcher();
        CountDownLatch            latchDone  = new CountDownLatch(BACKPRESSURE_REQUESTS);

        assertThat(pool.getTaskLimit(), is(8));
        if (!pool.isStarted())
            {
            pool.start();
            }

        AtomicBoolean fBacklogNormal = new AtomicBoolean();
        Notifier      notifier       = new SingleWaiterMultiNotifier();

        try (NonBlocking ignored = new NonBlocking())
            {
            for (int i = 0; i < BACKPRESSURE_REQUESTS; i++)
                {
                pool.add(new SleepingBackpressureTask(latchDone));
                }

            Continuation<Void> contNormal = ignoredResult ->
                {
                fBacklogNormal.set(true);
                notifier.signal();
                };

            assertEventually("Expected VDP TaskBacklog to trip event-dispatcher flow control",
                    () -> pool.getBacklog() > 32 && dispatcher.checkBacklog(null));
            assertTrue("Expected event-dispatcher flow control to report VDP-backed service backlog",
                    dispatcher.checkBacklog(contNormal));
            }

        notifier.await(30_000L);

        assertTrue("Backpressure continuation did not fire after the VDP backlog drained",
                fBacklogNormal.get());
        assertTrue("Expected all VDP saturation tasks to complete",
                latchDone.await(30L, TimeUnit.SECONDS));
        }

    // ----- helper classes -----------------------------------------------

    protected static void assertEventually(String sMessage, Assertion assertion)
            throws TimeoutException
        {
        long ldtTimeout = Base.getSafeTimeMillis() + 5_000L;
        while (Base.getSafeTimeMillis() <= ldtTimeout)
            {
            if (assertion.evaluate())
                {
                return;
                }

            Base.sleep(10L);
            }

        throw new TimeoutException(sMessage);
        }

    @FunctionalInterface
    protected interface Assertion
        {
        boolean evaluate();
        }

    protected static class Client
            extends Thread
        {
        protected Client(InvocationService service, int index, AtomicReference<Throwable> failure,
                CountDownLatch latchClients)
            {
            f_service      = service;
            f_index        = index;
            f_failure      = failure;
            f_latchClients = latchClients;
            }

        @Override
        public void run()
            {
            AtomicInteger   cCompleted = new AtomicInteger();
            CountDownLatch  latchDone  = new CountDownLatch(ITERATIONS);
            InvocationObserver observer = new InvocationObserver()
                {
                @Override
                public void memberCompleted(Member member, Object result)
                    {
                    cCompleted.incrementAndGet();
                    latchDone.countDown();
                    }

                @Override
                public void memberFailed(Member member, Throwable eFailure)
                    {
                    f_failure.compareAndSet(null, eFailure);
                    while (latchDone.getCount() > 0)
                        {
                        latchDone.countDown();
                        }
                    }

                @Override
                public void memberLeft(Member member)
                    {
                    memberFailed(member, new AssertionError("Member left during invocation"));
                    }

                @Override
                public void invocationCompleted()
                    {
                    }
                };

            try
                {
                for (int i = 0; i < ITERATIONS && f_failure.get() == null; i++)
                    {
                    f_service.execute(new OrderedIncrementor(f_index, i), null, observer);

                    while (i - cCompleted.get() >= MAX_PENDING && f_failure.get() == null)
                        {
                        Base.sleep(1L);
                        }
                    }

                if (f_failure.get() == null && !latchDone.await(30, TimeUnit.SECONDS))
                    {
                    f_failure.compareAndSet(null, new AssertionError("Timed out waiting for invocations to complete"));
                    }
                }
            catch (Throwable t)
                {
                f_failure.compareAndSet(null, t);
                }
            finally
                {
                f_latchClients.countDown();
                }
            }

        private final InvocationService       f_service;
        private final int                     f_index;
        private final AtomicReference<Throwable> f_failure;
        private final CountDownLatch          f_latchClients;
        }

    public static class OrderedIncrementor
            extends AbstractInvocable
            implements com.oracle.coherence.common.base.Associated<Integer>
        {
        public OrderedIncrementor(int index, int nExpect)
            {
            m_index   = index;
            m_nExpect = nExpect;
            }

        @Override
        public Integer getAssociatedKey()
            {
            return m_index;
            }

        @Override
        public void run()
            {
            int nValueOld = s_aiCounter[m_index];

            if (nValueOld != m_nExpect)
                {
                throw new AssertionError("Expected " + m_nExpect + " for key " + m_index + " but was " + nValueOld);
                }

            Base.sleep(1L);
            s_aiCounter[m_index] = nValueOld + 1;
            setResult(nValueOld);
            }

        protected int m_index;
        protected int m_nExpect;
        }

    protected static class SleepingBackpressureTask
            implements Runnable
        {
        protected SleepingBackpressureTask(CountDownLatch latchDone)
            {
            f_latchDone = latchDone;
            }

        @Override
        public void run()
            {
            try
                {
                Base.sleep(250L);
                }
            finally
                {
                f_latchDone.countDown();
                }
            }

        private final CountDownLatch f_latchDone;
        }

    // ----- constants -----------------------------------------------------

    protected static final int ASSOCIATION_COUNT = 4;
    protected static final int ITERATIONS        = 128;
    protected static final int MAX_PENDING       = 16;
    protected static final int BACKPRESSURE_REQUESTS = 128;

    protected static final String BACKPRESSURE_SERVICE = "vdp-backpressure";

    // ----- data members --------------------------------------------------

    protected static int[] s_aiCounter;
    }
