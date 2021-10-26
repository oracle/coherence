/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks;

import com.tangosol.net.Coherence;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link DistributedLock} class.
 *
 * @author Aleks Seovic  2021.10.20
 */
public class DistributedLockIT
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

    @Test
    void shouldAcquireAndReleaseLock()
        {
        DistributedLock lock = Locks.exclusive("foo");
        lock.lock();
        try
            {
            System.out.println("Lock acquired by " + lock.getOwner());
            assertThat(lock.isLocked(), is(true));
            assertThat(lock.isHeldByCurrentThread(), is(true));
            assertThat(lock.getHoldCount(), is(1L));
            }
        finally
            {
            lock.unlock();
            assertThat(lock.isLocked(), is(false));
            assertThat(lock.isHeldByCurrentThread(), is(false));
            assertThat(lock.getHoldCount(), is(0L));
            System.out.println("Lock released by " + Thread.currentThread());
            }
        }

    @Test
    void shouldAcquireAndReleaseLockInterruptibly() throws InterruptedException
        {
        DistributedLock lock = Locks.exclusive("foo");
        lock.lockInterruptibly();
        try
            {
            System.out.println("Lock acquired by " + lock.getOwner());
            assertThat(lock.isLocked(), is(true));
            assertThat(lock.isHeldByCurrentThread(), is(true));
            assertThat(lock.getHoldCount(), is(1L));
            }
        finally
            {
            lock.unlock();
            assertThat(lock.isLocked(), is(false));
            assertThat(lock.isHeldByCurrentThread(), is(false));
            assertThat(lock.getHoldCount(), is(0L));
            System.out.println("Lock released by " + Thread.currentThread());
            }
        }

    @Test
    void shouldAcquireAndReleaseLockWithoutBlocking()
        {
        DistributedLock lock = Locks.exclusive("foo");
        assertThat(lock.tryLock(), is(true));
        try
            {
            System.out.println("Lock acquired by " + lock.getOwner());
            assertThat(lock.isLocked(), is(true));
            assertThat(lock.isHeldByCurrentThread(), is(true));
            assertThat(lock.getHoldCount(), is(1L));
            }
        finally
            {
            lock.unlock();
            assertThat(lock.isLocked(), is(false));
            assertThat(lock.isHeldByCurrentThread(), is(false));
            assertThat(lock.getHoldCount(), is(0L));
            System.out.println("Lock released by " + Thread.currentThread());
            }
        }

    @Test
    void shouldAcquireAndReleaseLockWithTimeout() throws InterruptedException
        {
        DistributedLock lock = Locks.exclusive("foo");
        assertThat(lock.tryLock(1L, TimeUnit.SECONDS), is(true));
        try
            {
            System.out.println("Lock acquired by " + lock.getOwner());
            assertThat(lock.isLocked(), is(true));
            assertThat(lock.isHeldByCurrentThread(), is(true));
            assertThat(lock.getHoldCount(), is(1L));
            }
        finally
            {
            lock.unlock();
            assertThat(lock.isLocked(), is(false));
            assertThat(lock.isHeldByCurrentThread(), is(false));
            assertThat(lock.getHoldCount(), is(0L));
            System.out.println("Lock released by " + Thread.currentThread());
            }
        }

    @Test
    void shouldAcquireAndReleaseReentrantLock()
        {
        DistributedLock lock = Locks.exclusive("foo");
        lock.lock();
        lock.lock();
        lock.lock();
        try
            {
            System.out.println("Lock acquired by " + lock.getOwner());
            assertThat(lock.isLocked(), is(true));
            assertThat(lock.isHeldByCurrentThread(), is(true));
            assertThat(lock.getHoldCount(), is(3L));
            }
        finally
            {
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
        }

    @Test
    void shouldAcquireAndReleaseLockInOrderFromMultipleThreads()
            throws InterruptedException
        {
        DistributedLock lock = Locks.exclusive("foo");
        Thread thread = new Thread(() ->
               {
               assertThat(lock.tryLock(), is(false));
               long start = System.currentTimeMillis();
               lock.lock();
               try
                   {
                   long elapsed = System.currentTimeMillis() - start;
                   System.out.println("Lock acquired by " + lock.getOwner() + " after " + elapsed + "ms");
                   assertThat(lock.isLocked(), is(true));
                   assertThat(lock.isHeldByCurrentThread(), is(true));
                   assertThat(lock.getHoldCount(), is(1L));
                   }
               finally
                   {
                   lock.unlock();
                   assertThat(lock.isLocked(), is(false));
                   assertThat(lock.isHeldByCurrentThread(), is(false));
                   assertThat(lock.getHoldCount(), is(0L));
                   System.out.println("Lock released by " + Thread.currentThread());
                   }
               });

        lock.lock();
        try
            {
            thread.start();

            System.out.println("Lock acquired by " + lock.getOwner());
            assertThat(lock.isLocked(), is(true));
            assertThat(lock.isHeldByCurrentThread(), is(true));
            assertThat(lock.getHoldCount(), is(1L));
            Thread.sleep(500L);
            assertThat(lock.getQueueLength(), is(1));
            assertThat(lock.hasQueuedThreads(), is(true));
            assertThat(lock.hasQueuedThread(thread), is(true));
            Thread.sleep(500L);
            }
        finally
            {
            lock.unlock();
            assertThat(lock.isHeldByCurrentThread(), is(false));
            System.out.println("Lock released by " + Thread.currentThread());
            thread.join();
            assertThat(lock.isLocked(), is(false));
            }
        }

    @Test
    void shouldTimeOutIfTheLockIsHeldByAnotherThread()
            throws InterruptedException
        {
        DistributedLock lock = Locks.exclusive("foo");
        final Thread thread = new Thread(() ->
               {
               lock.lock();
               try
                   {
                   System.out.println("Lock acquired by " + lock.getOwner());
                   Thread.sleep(1000L);
                   }
               catch (InterruptedException ignore)
                   {
                   }
               finally
                   {
                   lock.unlock();
                   System.out.println("Lock released by " + Thread.currentThread());
                   }
               });

        thread.start();
        Thread.sleep(100L);
        assertThat(lock.tryLock(500, TimeUnit.MILLISECONDS), is(false));
        thread.join();
        assertThat(lock.isLocked(), is(false));
        }

    @Test
    void shouldBeAbleToInterruptLockRequest()
            throws InterruptedException
        {
        DistributedLock lock = Locks.exclusive("foo");
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
        try
            {
            thread.start();

            System.out.println("Lock acquired by " + lock.getOwner());
            assertThat(lock.isLocked(), is(true));
            assertThat(lock.isHeldByCurrentThread(), is(true));
            assertThat(lock.getHoldCount(), is(1L));
            Thread.sleep(500L);
            assertThat(lock.getQueueLength(), is(1));
            thread.interrupt();
            }
        finally
            {
            lock.unlock();
            assertThat(lock.isHeldByCurrentThread(), is(false));
            System.out.println("Lock released by " + Thread.currentThread());
            thread.join();
            assertThat(lock.isLocked(), is(false));
            }
        }
    }
