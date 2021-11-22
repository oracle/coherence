/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.Coherence;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link DistributedLock} class.
 *
 * @author Aleks Seovic  2021.10.20
 */
public class DistributedLockTest
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
    void sanityCheck()
        {
        DistributedLock lock = Locks.exclusiveLock("foo");
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.isHeldByCurrentThread(), is(false));
        assertThat(lock.getHoldCount(), is(0L));
        assertThat(lock.getPendingLocks().isEmpty(), is(true));
        }

    @Test
    void shouldAcquireAndReleaseLock()
        {
        DistributedLock lock = Locks.exclusiveLock("foo");
        lock.lock();
        System.out.println("Lock acquired by " + lock.getOwner());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        lock.unlock();
        System.out.println("Lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseLockInterruptibly() throws InterruptedException
        {
        DistributedLock lock = Locks.exclusiveLock("foo");
        lock.lockInterruptibly();
        System.out.println("Lock acquired by " + lock.getOwner());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        lock.unlock();
        System.out.println("Lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseLockWithoutBlocking()
        {
        DistributedLock lock = Locks.exclusiveLock("foo");
        assertThat(lock.tryLock(), is(true));
        System.out.println("Lock acquired by " + lock.getOwner());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        lock.unlock();
        System.out.println("Lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseLockWithTimeout() throws InterruptedException
        {
        DistributedLock lock = Locks.exclusiveLock("foo");
        assertThat(lock.tryLock(1L, TimeUnit.SECONDS), is(true));
        System.out.println("Lock acquired by " + lock.getOwner());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        lock.unlock();
        System.out.println("Lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseReentrantLock()
        {
        DistributedLock lock = Locks.exclusiveLock("foo");
        lock.lock();
        lock.lock();
        lock.lock();
        System.out.println("Lock acquired by " + lock.getOwner());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(3L));

        lock.unlock();
        lock.unlock();
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        lock.unlock();
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.isHeldByCurrentThread(), is(false));
        assertThat(lock.getHoldCount(), is(0L));
        System.out.println("Lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseLockInOrderFromMultipleThreads()
            throws InterruptedException
        {
        DistributedLock lock = Locks.exclusiveLock("foo");
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);

        Thread thread = new Thread(() ->
               {
               assertThat(lock.tryLock(), is(false));
               long start = System.currentTimeMillis();
               lock.lock();
               long elapsed = System.currentTimeMillis() - start;
               System.out.println("Lock acquired by " + lock.getOwner() + " after " + elapsed + "ms");
               assertThat(lock.isLocked(), is(true));
               assertThat(lock.isHeldByCurrentThread(), is(true));
               assertThat(lock.getHoldCount(), is(1L));

               s1.release();
               s2.acquireUninterruptibly();

               lock.unlock();
               assertThat(lock.isLocked(), is(false));
               assertThat(lock.isHeldByCurrentThread(), is(false));
               assertThat(lock.getHoldCount(), is(0L));
               System.out.println("Lock released by " + Thread.currentThread());
               });

        lock.lock();
        System.out.println("Lock acquired by " + lock.getOwner());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        thread.start();
        Eventually.assertDeferred(lock::getQueueLength, is(1));
        Eventually.assertDeferred(lock::hasQueuedThreads, is(true));
        Eventually.assertDeferred(() -> lock.hasQueuedThread(thread), is(true));

        lock.unlock();
        System.out.println("Lock released by " + Thread.currentThread());

        s1.acquire();
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(false));
        assertThat(lock.getHoldCount(), is(0L));

        s2.release();
        thread.join();
        }

    @Test
    void shouldTimeOutIfTheLockIsHeldByAnotherThread()
            throws InterruptedException
        {
        DistributedLock lock = Locks.exclusiveLock("foo");
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);

        final Thread thread = new Thread(() ->
               {
               lock.lock();
               System.out.println("Lock acquired by " + lock.getOwner());

               s1.release();
               s2.acquireUninterruptibly();

               lock.unlock();
               System.out.println("Lock released by " + Thread.currentThread());
               });

        thread.start();
        s1.acquire();
        assertThat(lock.tryLock(500, TimeUnit.MILLISECONDS), is(false));

        s2.release();
        thread.join();
        }

    @Test
    void shouldBeAbleToInterruptLockRequest()
            throws InterruptedException
        {
        DistributedLock lock = Locks.exclusiveLock("foo");
        Thread thread = new Thread(() ->
               {
               try
                   {
                   lock.lockInterruptibly();
                   }
               catch (InterruptedException e)
                   {
                   System.out.println("Lock interrupted");
                   assertThat(lock.isLocked(), is(true));
                   assertThat(lock.isHeldByCurrentThread(), is(false));
                   assertThat(lock.getQueueLength(), is(0));
                   }
               });

        lock.lock();
        thread.start();

        System.out.println("Lock acquired by " + lock.getOwner());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        Eventually.assertDeferred(lock::getQueueLength, is(1));
        thread.interrupt();
        thread.join();

        lock.unlock();
        System.out.println("Lock released by " + Thread.currentThread());
        }
    }
