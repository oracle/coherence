/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.locks;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.concurrent.locks.Locks;
import com.oracle.coherence.concurrent.locks.RemoteLock;
import com.tangosol.net.Coherence;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link RemoteLock} class.
 *
 * @author Aleks Seovic  2021.10.20
 */
public class RemoteLockTest
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
        System.out.println(">>>>> Starting test method " + info.getDisplayName());
        }

    @AfterEach
    void afterEach(TestInfo info)
        {
        // sanity check: let's make sure the lock is not locked, and there are
        // no pending locks on it
        RemoteLock lock = Locks.remoteLock("foo");
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.isHeldByCurrentThread(), is(false));
        assertThat(lock.getHoldCount(), is(0L));

        System.out.println("<<<<< Completed test method " + info.getDisplayName());
        }

    @Test
    @Disabled("only run manually to debug locking")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void stressLocks() throws InterruptedException
        {
        Lock lock = Locks.remoteLock("foo");
        ExecutorService e = Executors.newFixedThreadPool(8);
        Runnable r = () ->
                {
                while (true)
                    {
                    try
                        {
                        lock.lockInterruptibly();
                        System.out.println(Thread.currentThread().getName() + ": LOCKED");
                        }
                    catch (InterruptedException ex)
                        {
                        break;
                        }
                    finally
                        {
                        lock.unlock();
                        System.out.println(Thread.currentThread().getName() + ": UNLOCKED");
                        }
                    }
                };

        for (int i = 0; i < 8; i++)
            {
            e.submit(r);
            }

        e.awaitTermination(30, TimeUnit.SECONDS);
        e.shutdownNow();
        e.awaitTermination(10, TimeUnit.SECONDS);
        }

    @Test
    void shouldAcquireAndReleaseLock()
        {
        RemoteLock lock = Locks.remoteLock("foo");
        lock.lock();
        System.out.println("Lock acquired by " + Thread.currentThread());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        System.out.println("Lock released by " + Thread.currentThread());
        lock.unlock();
        }

    @Test
    void shouldAcquireAndReleaseLockInterruptibly() throws InterruptedException
        {
        RemoteLock lock = Locks.remoteLock("foo");
        lock.lockInterruptibly();
        System.out.println("Lock acquired by " + Thread.currentThread());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        System.out.println("Lock released by " + Thread.currentThread());
        lock.unlock();
        }

    @Test
    void shouldAcquireAndReleaseLockWithoutBlocking()
        {
        RemoteLock lock = Locks.remoteLock("foo");
        assertThat(lock.tryLock(), is(true));
        System.out.println("Lock acquired by " + Thread.currentThread());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        System.out.println("Lock released by " + Thread.currentThread());
        lock.unlock();
        }

    @Test
    void shouldAcquireAndReleaseLockWithTimeout() throws InterruptedException
        {
        RemoteLock lock = Locks.remoteLock("foo");
        assertThat(lock.tryLock(1L, TimeUnit.SECONDS), is(true));
        System.out.println("Lock acquired by " + Thread.currentThread());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        System.out.println("Lock released by " + Thread.currentThread());
        lock.unlock();
        }

    @Test
    void shouldAcquireAndReleaseReentrantLock()
        {
        RemoteLock lock = Locks.remoteLock("foo");
        lock.lock();
        lock.lock();
        lock.lock();
        System.out.println("Lock acquired by " + Thread.currentThread());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(3L));

        lock.unlock();
        lock.unlock();
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        System.out.println("Lock released by " + Thread.currentThread());
        lock.unlock();
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.isHeldByCurrentThread(), is(false));
        assertThat(lock.getHoldCount(), is(0L));
        }

    @Test
    void shouldAcquireAndReleaseLockInOrderFromMultipleThreads()
            throws InterruptedException
        {
        RemoteLock lock = Locks.remoteLock("foo");
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);

        Thread thread = new Thread(() ->
               {
               assertThat(lock.tryLock(), is(false));
               long start = System.currentTimeMillis();
               lock.lock();
               long elapsed = System.currentTimeMillis() - start;
               System.out.println("Lock acquired by " + Thread.currentThread() + " after " + elapsed + "ms");
               assertThat(lock.isLocked(), is(true));
               assertThat(lock.isHeldByCurrentThread(), is(true));
               assertThat(lock.getHoldCount(), is(1L));

               s1.release();
               s2.acquireUninterruptibly();

               System.out.println("Lock released by " + Thread.currentThread());
               lock.unlock();
               assertThat(lock.isLocked(), is(false));
               assertThat(lock.isHeldByCurrentThread(), is(false));
               assertThat(lock.getHoldCount(), is(0L));
               });

        lock.lock();
        System.out.println("Lock acquired by " + Thread.currentThread());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        thread.start();
        Eventually.assertDeferred(lock::getQueueLength, is(1));
        Eventually.assertDeferred(lock::hasQueuedThreads, is(true));
        Eventually.assertDeferred(() -> lock.hasQueuedThread(thread), is(true));

        System.out.println("Lock released by " + Thread.currentThread());
        lock.unlock();

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
        RemoteLock lock = Locks.remoteLock("foo");
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);

        final Thread thread = new Thread(() ->
               {
               lock.lock();
               System.out.println("Lock acquired by " + Thread.currentThread());

               s1.release();
               s2.acquireUninterruptibly();

               System.out.println("Lock released by " + Thread.currentThread());
               lock.unlock();
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
        RemoteLock lock = Locks.remoteLock("foo");
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

        System.out.println("Lock acquired by " + Thread.currentThread());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        Eventually.assertDeferred(lock::getQueueLength, is(1));
        thread.interrupt();
        thread.join();

        System.out.println("Lock released by " + Thread.currentThread());
        lock.unlock();
        }
    }
