/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.Latches;
import com.oracle.coherence.concurrent.RemoteCountDownLatch;
import com.tangosol.net.Coherence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Basic Tests for {@link RemoteCountDownLatch} class.
 *
 * @author as, lh  2021.11.17
 *
 * @since 21.12
 */
public class RemoteCountDownLatchTest
    {
    @BeforeAll
    static void startServer()
        {
        Coherence.clusterMember().start().join();
        }

    @AfterAll
    static void stopServer()
        {
        Coherence.closeAll();
        }

    @BeforeEach
    void beforeEach(TestInfo info)
        {
        m_sTestName = getClass().getSimpleName() + "-"
                + info.getTestMethod().map(method -> method.getName()).orElse(info.getDisplayName());
        }

    @AfterEach
    void clear()
        {
        ConcurrentHelper.clearLatches();
        }

    @Test
    void shouldAcquireAndCountDown() throws InterruptedException
        {
        RemoteCountDownLatch latch     = Latches.remoteCountDownLatch(latchName("foo"), 1);
        Semaphore                 semaphore = new Semaphore(0);
        Thread                    worker    = new Thread(new Runnable()
            {
            @Override
            public void run()
                {
                semaphore.acquireUninterruptibly();
                latch.countDown();
                System.out.println(Thread.currentThread().getName()
                            + " finished");
                }
            });

        worker.start();
        assertThat(latch.getCount(), is(1L));
        semaphore.release();
        latch.await();
        Eventually.assertDeferred(ConcurrentHelper.latchesMap()::size, is(0));
        }

    @Test
    void shouldAcquireAndTimedOut() throws InterruptedException
        {
        int                       count     = 1;
        String                    sName     = latchName("foo");
        RemoteCountDownLatch latch     = Latches.remoteCountDownLatch(sName, count);
        Semaphore                 semaphore = new Semaphore(0);
        Thread                    worker    = new Thread(new Runnable()
            {
            @Override
            public void run()
                {
                semaphore.acquireUninterruptibly();
                latch.countDown();
                System.out.println(Thread.currentThread().getName()
                        + " finished");
                }
            });

        worker.start();
        latch.await(100, TimeUnit.MILLISECONDS);
        assertThat(latch.getCount(), is(1L));

        semaphore.release();
        latch.await();
        Eventually.assertDeferred(ConcurrentHelper.latchesMap()::size, is(0));

        latch.await();

        try
            {
            RemoteCountDownLatch latch1 = Latches.remoteCountDownLatch(sName, 2 * count);
            assertThat((int) latch.getCount(), is(2*count));
            RemoteCountDownLatch latch2 = Latches.remoteCountDownLatch(sName, count);
            fail("Should return IllegalArgumentException") ;
            }
        catch (IllegalArgumentException e)
            {
            Logger.info("Got expected exception: " + e.getMessage());
            }
        }

    @Test
    void shouldAcquireAndCountDownMany() throws InterruptedException
        {
        final int                 size      = 5;
        RemoteCountDownLatch latch     = Latches.remoteCountDownLatch(latchName("manyFoo"), size);
        Semaphore                 semaphore = new Semaphore(0);
        Thread[]                  workers   = new Thread[size];

        for (int i = 0; i < size; i++)
            {
            workers[i] = new Thread(new Runnable()
                {
                @Override
                public void run()
                    {
                    semaphore.acquireUninterruptibly();
                    latch.countDown();
                    System.out.println(Thread.currentThread().getName()
                            + " finished");
                    }
                });
            workers[i].start();
            }

        assertThat(latch.getCount(), is(5L));
        semaphore.release(size);
        latch.await();
        Eventually.assertDeferred(ConcurrentHelper.latchesMap()::size, is(0));
        }

    /**
     * Return a test-scoped latch name.
     *
     * @param sSuffix  the latch name suffix
     *
     * @return a test-scoped latch name
     */
    private String latchName(String sSuffix)
        {
        return m_sTestName + "-" + sSuffix;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The current test name, used to scope latch names.
     */
    private String m_sTestName;
    }
