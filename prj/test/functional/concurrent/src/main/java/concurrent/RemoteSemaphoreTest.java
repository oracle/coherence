/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.concurrent.RemoteSemaphore;
import com.oracle.coherence.concurrent.Semaphores;
import com.tangosol.net.Coherence;

import com.tangosol.util.Base;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link RemoteSemaphore} class.
 *
 * @author Vaso Putica  2021.11.30
 */
public class RemoteSemaphoreTest
    {
    @BeforeAll
    static void startServer() throws Exception
        {
        Coherence.clusterMember().start().get(10, TimeUnit.MINUTES);
        }

    @AfterAll
    static void stopServer()
        {
        Coherence.closeAll();
        }

    @AfterEach
    void clear()
        {
        ConcurrentHelper.clearSemaphores();
        }

    @Test
    @Timeout(60)
    void shouldAcquireAndReleasePermits()
        {
        RemoteSemaphore semaphore = Semaphores.remoteSemaphore("foo", 5);
        semaphore.acquireUninterruptibly();
        assertThat(semaphore.availablePermits(), is(4));
        semaphore.release();
        assertThat(semaphore.availablePermits(), is(5));
        }

    @Test
    @Timeout(60)
    void shouldAcquireAndReleasePermitsnterruptibly()
            throws InterruptedException
        {
        RemoteSemaphore semaphore = Semaphores.remoteSemaphore("foo", 5);
        semaphore.acquire();
        assertThat(semaphore.availablePermits(), is(4));
        semaphore.release();
        assertThat(semaphore.availablePermits(), is(5));
        }

    @Test
    @Timeout(60)
    void shouldAcquireAndReleasePermitWithoutBlocking()
        {
        RemoteSemaphore semaphore = Semaphores.remoteSemaphore("foo", 5);
        assertThat(semaphore.tryAcquire(), is(true));
        assertThat(semaphore.availablePermits(), is(4));
        semaphore.release();
        assertThat(semaphore.availablePermits(), is(5));
        }

    @Test
    @Timeout(60)
    void shouldAcquireAndReleasePermitWithTimeout() throws InterruptedException
        {
        RemoteSemaphore semaphore = Semaphores.remoteSemaphore("foo", 5);
        assertThat(semaphore.tryAcquire(1L, TimeUnit.SECONDS), is(true));
        assertThat(semaphore.availablePermits(), is(4));
        semaphore.release();
        assertThat(semaphore.availablePermits(), is(5));
        }

    @Test
    @Timeout(60)
    void shouldAcquireAndReleaseMultipleTimes() throws InterruptedException
        {
        RemoteSemaphore semaphore = Semaphores.remoteSemaphore("foo", 5);
        semaphore.acquire();
        semaphore.acquire();
        semaphore.acquire();
        assertThat(semaphore.availablePermits(), is(2));
        semaphore.release();
        semaphore.release();
        assertThat(semaphore.availablePermits(), is(4));
        }

    @Test
    @Timeout(120)
    void shouldAcquireAndReleasePermitFromMultipleThreads()
            throws Throwable
        {
        RemoteSemaphore semaphore = Semaphores.remoteSemaphore("foo", 1);
        Semaphore s0 = new Semaphore(0);
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);


        Thread thread = new Thread(() ->
               {
               assertThat(semaphore.tryAcquire(), is(false));
               s0.release();

               semaphore.acquireUninterruptibly();
               assertThat(semaphore.availablePermits(), is(0));

               s1.release();
               s2.acquireUninterruptibly();

               semaphore.release();
               assertThat(semaphore.availablePermits(), is(1));
               assertThat(semaphore.isAcquiredByCurrentThread(), is(false));
               });

        semaphore.acquireUninterruptibly();
        assertThat(semaphore.availablePermits(), is(0));
        assertThat(semaphore.isAcquiredByCurrentThread(), is(true));

        final AtomicReference<Throwable> exceptionThrown = new AtomicReference<>();
        thread.setUncaughtExceptionHandler((t, throwable) -> exceptionThrown.set(throwable));
        thread.start();
        Eventually.assertDeferred(semaphore::availablePermits, is(0));

        s0.acquireUninterruptibly();
        semaphore.release();

        CompletableFuture.runAsync(s1::acquireUninterruptibly).get(45, TimeUnit.SECONDS);

        if (exceptionThrown.get() != null)
            {
            fail("alternate thread exception", exceptionThrown.get());
            }

        assertThat(semaphore.availablePermits(), is(0));
        assertThat(semaphore.isAcquiredByCurrentThread(), is(false));
        s2.release();
        thread.join();
        }

    @Test
    @Timeout(60)
    void shouldTimeOutIfAcquiredByAnotherThread()
        throws InterruptedException
        {
        RemoteSemaphore semaphore = Semaphores.remoteSemaphore("foo", 1);
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);

        final Thread thread = new Thread(() ->
            {
            semaphore.acquireUninterruptibly();

            s1.release();
            s2.acquireUninterruptibly();

            semaphore.release();
            });

        thread.start();
        s1.acquireUninterruptibly();
        assertThat(semaphore.tryAcquire(500, TimeUnit.MILLISECONDS), is(false));

        s2.release();
        thread.join(60000);
        }

    @Test
    @Timeout(60)
    void shouldBeAbleToInterruptAcquireRequest() throws Throwable
        {
        CountDownLatch latch = new CountDownLatch(1);
        RemoteSemaphore semaphore = Semaphores.remoteSemaphore("foo", 1);
        Thread thread = new Thread(() ->
               {
               try
                   {
                   latch.countDown();
                   semaphore.acquire();
                   }
               catch (InterruptedException e)
                   {
                   assertThat(semaphore.availablePermits(), is(0));
                   assertThat(semaphore.isAcquiredByCurrentThread(), is(false));
                   }
               });
        semaphore.acquireUninterruptibly();

        final AtomicReference<Throwable> exceptionThrown = new AtomicReference<>();
        thread.setUncaughtExceptionHandler((t, throwable) -> exceptionThrown.set(throwable));
        thread.start();

        latch.await();
        Base.sleep(1000);
        assertThat(semaphore.availablePermits(), is(0));
        assertThat(semaphore.isAcquiredByCurrentThread(), is(true));

        thread.interrupt();
        thread.join(60000);
        semaphore.release();

        assertThat(exceptionThrown.get(), nullValue());
        }
    }


