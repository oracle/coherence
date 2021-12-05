/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.semaphores;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.Coherence;

import com.tangosol.util.Base;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link DistributedSemaphore} class.
 *
 * @author Vaso Putica  2021.11.30
 */
public class DistributedSemaphoreTest
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

    @AfterEach
    void clear()
        {
        Semaphores.clear();
        }

    @Test
    void shouldAcquireAndReleasePermits()
        {
        DistributedSemaphore semaphore = Semaphores.remoteSemaphore("foo", 5);
        semaphore.acquireUninterruptibly();
        assertThat(semaphore.availablePermits(), is(4));
        semaphore.release();
        assertThat(semaphore.availablePermits(), is(5));
        }

    @Test
    void shouldAcquireAndReleasePermitsnterruptibly()
            throws InterruptedException
        {
        DistributedSemaphore semaphore = Semaphores.remoteSemaphore("foo", 5);
        semaphore.acquire();
        assertThat(semaphore.availablePermits(), is(4));
        semaphore.release();
        assertThat(semaphore.availablePermits(), is(5));
        }

    @Test
    void shouldAcquireAndReleasePermitWithoutBlocking()
        {
        DistributedSemaphore semaphore = Semaphores.remoteSemaphore("foo", 5);
        assertThat(semaphore.tryAcquire(), is(true));
        assertThat(semaphore.availablePermits(), is(4));
        semaphore.release();
        assertThat(semaphore.availablePermits(), is(5));
        }

    @Test
    void shouldAcquireAndReleasePermitWithTimeout() throws InterruptedException
        {
        DistributedSemaphore semaphore = Semaphores.remoteSemaphore("foo", 5);
        assertThat(semaphore.tryAcquire(1L, TimeUnit.SECONDS), is(true));
        assertThat(semaphore.availablePermits(), is(4));
        semaphore.release();
        assertThat(semaphore.availablePermits(), is(5));
        }

    @Test
    void shouldAcquireAndReleaseMultipleTimes() throws InterruptedException
        {
        DistributedSemaphore semaphore = Semaphores.remoteSemaphore("foo", 5);
        semaphore.acquire();
        semaphore.acquire();
        semaphore.acquire();
        assertThat(semaphore.availablePermits(), is(2));
        semaphore.release();
        semaphore.release();
        assertThat(semaphore.availablePermits(), is(4));
        }

    @Test
    void shouldAcquireAndReleasePermitFromMultipleThreads()
            throws Throwable
        {
        DistributedSemaphore semaphore = Semaphores.remoteSemaphore("foo", 1);
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);


        Thread thread = new Thread(() ->
               {
               assertThat(semaphore.tryAcquire(), is(false));
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

        semaphore.release();

        s1.acquireUninterruptibly();
        assertThat(semaphore.availablePermits(), is(0));
        assertThat(semaphore.isAcquiredByCurrentThread(), is(false));
        s2.release();
        thread.join();
        if (exceptionThrown.get() != null)
            {
            throw exceptionThrown.get();
            }
        }

    @Test
    void shouldTimeOutIfAcquiredByAnotherThread()
        throws InterruptedException
        {
        DistributedSemaphore semaphore = Semaphores.remoteSemaphore("foo", 1);
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
        thread.join();
        }

    @Test
    void shouldBeAbleToInterruptAcquireRequest() throws Throwable
        {
        CountDownLatch latch = new CountDownLatch(1);
        DistributedSemaphore semaphore = Semaphores.remoteSemaphore("foo", 1);
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
        thread.join();

        semaphore.release();

        assertThat(exceptionThrown.get(), nullValue());
        }
    }


