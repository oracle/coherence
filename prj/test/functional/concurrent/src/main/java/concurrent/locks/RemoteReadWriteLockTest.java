/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.locks;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.concurrent.locks.Locks;
import com.oracle.coherence.concurrent.locks.RemoteReadWriteLock;
import com.tangosol.net.Coherence;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.ReadWriteLock;

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
 * Tests for {@link RemoteReadWriteLock} class.
 *
 * @author Aleks Seovic  2021.11.13
 */
public class RemoteReadWriteLockTest
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
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        assertThat(lock.isReadLocked(), is(false));
        assertThat(lock.isWriteLocked(), is(false));
        assertThat(lock.getReadHoldCount(), is(0));
        assertThat(lock.getWriteHoldCount(), is(0));

        System.out.println("<<<<< Completed test method " + info.getDisplayName());
        }

    @Test
    @Disabled("only run manually to debug locking")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void stressLocks() throws InterruptedException
        {
        ReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        ExecutorService e = Executors.newFixedThreadPool(8);
        Runnable write = () ->
                {
                while (true)
                    {
                    try
                        {
                        lock.writeLock().lockInterruptibly();
                        System.out.println(Thread.currentThread().getName() + ": LOCKED (W)");
                        }
                    catch (InterruptedException ex)
                        {
                        break;
                        }
                    finally
                        {
                        lock.writeLock().unlock();
                        System.out.println(Thread.currentThread().getName() + ": UNLOCKED");
                        }
                    }
                };

        Runnable read = () ->
                {
                while (true)
                    {
                    try
                        {
                        lock.readLock().lockInterruptibly();
                        System.out.println(Thread.currentThread().getName() + ": LOCKED (R)");
                        }
                    catch (InterruptedException ex)
                        {
                        break;
                        }
                    finally
                        {
                        lock.readLock().unlock();
                        System.out.println(Thread.currentThread().getName() + ": UNLOCKED");
                        }
                    }
                };

        for (int i = 0; i < 8; i++)
            {
            e.submit(i % 2 == 0 ? read : write);
            }

        e.awaitTermination(30, TimeUnit.SECONDS);
        e.shutdownNow();
        e.awaitTermination(10, TimeUnit.SECONDS);
        }

    @Test
    void shouldAcquireAndReleaseWriteLock()
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        lock.writeLock().lock();
        System.out.println("Write lock acquired by " + Thread.currentThread());
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(true));
        assertThat(lock.getWriteHoldCount(), is(1));

        lock.writeLock().unlock();
        System.out.println("Write lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseReadLock()
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        lock.readLock().lock();
        System.out.println("Read lock acquired by " + Thread.currentThread());
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.getReadLockCount(), is(1));
        assertThat(lock.getReadHoldCount(), is(1));

        lock.readLock().unlock();
        System.out.println("Read lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseWriteLockInterruptibly() throws InterruptedException
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        lock.writeLock().lockInterruptibly();
        System.out.println("Write lock acquired by " + Thread.currentThread());
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(true));
        assertThat(lock.getWriteHoldCount(), is(1));

        lock.writeLock().unlock();
        System.out.println("Write lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseReadLockInterruptibly() throws InterruptedException
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        lock.readLock().lockInterruptibly();

        System.out.println("Read lock acquired by " + Thread.currentThread());
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.getReadLockCount(), is(1));
        assertThat(lock.getReadHoldCount(), is(1));

        lock.readLock().unlock();
        System.out.println("Read lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseWriteLockWithoutBlocking()
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        assertThat(lock.writeLock().tryLock(), is(true));
        System.out.println("Write lock acquired by " + Thread.currentThread());
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(true));
        assertThat(lock.getWriteHoldCount(), is(1));

        lock.writeLock().unlock();
        System.out.println("Write lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseReadLockWithoutBlocking()
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        assertThat(lock.readLock().tryLock(), is(true));
        System.out.println("Read lock acquired by " + Thread.currentThread());
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.getReadLockCount(), is(1));
        assertThat(lock.getReadHoldCount(), is(1));

        lock.readLock().unlock();
        System.out.println("Read lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseWriteLockWithTimeout() throws InterruptedException
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        assertThat(lock.writeLock().tryLock(1L, TimeUnit.SECONDS), is(true));
        System.out.println("Write lock acquired by " + Thread.currentThread());
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(true));
        assertThat(lock.getWriteHoldCount(), is(1));

        lock.writeLock().unlock();
        System.out.println("Write lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseReadLockWithTimeout() throws InterruptedException
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        assertThat(lock.readLock().tryLock(1L, TimeUnit.SECONDS), is(true));
        System.out.println("Read lock acquired by " + Thread.currentThread());
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.getReadLockCount(), is(1));
        assertThat(lock.getReadHoldCount(), is(1));

        lock.readLock().unlock();
        System.out.println("Read lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseReentrantWriteLock()
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        lock.writeLock().lock();
        lock.writeLock().lock();
        lock.writeLock().lock();
        System.out.println("Write lock acquired by " + Thread.currentThread());
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(true));
        assertThat(lock.getWriteHoldCount(), is(3));

        lock.writeLock().unlock();
        lock.writeLock().unlock();
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(true));
        assertThat(lock.getWriteHoldCount(), is(1));
        lock.writeLock().unlock();
        System.out.println("Write lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseReentrantReadLock()
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        lock.readLock().lock();
        lock.readLock().lock();
        lock.readLock().lock();
        System.out.println("Read lock acquired by " + Thread.currentThread());
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.getReadLockCount(), is(1));
        assertThat(lock.getReadHoldCount(), is(3));

        lock.readLock().unlock();
        lock.readLock().unlock();
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.getReadLockCount(), is(1));
        assertThat(lock.getReadHoldCount(), is(1));

        lock.readLock().unlock();
        System.out.println("Read lock released by " + Thread.currentThread());
        }

    @Test
    void shouldAcquireAndReleaseWriteLockInOrderFromMultipleThreads()
            throws InterruptedException
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);

        Thread thread = new Thread(() ->
               {
               assertThat(lock.writeLock().tryLock(), is(false));
               long start = System.currentTimeMillis();
               lock.writeLock().lock();
               long elapsed = System.currentTimeMillis() - start;

               System.out.println("Write lock acquired by " + Thread.currentThread() + " after " + elapsed + "ms");
               assertThat(lock.isWriteLocked(), is(true));
               assertThat(lock.isWriteLockedByCurrentThread(), is(true));
               assertThat(lock.getWriteHoldCount(), is(1));

               s1.release();
               s2.acquireUninterruptibly();

               lock.writeLock().unlock();
               assertThat(lock.isWriteLocked(), is(false));
               assertThat(lock.isWriteLockedByCurrentThread(), is(false));
               assertThat(lock.getWriteHoldCount(), is(0));
               System.out.println("Write lock released by " + Thread.currentThread());
               });

        lock.writeLock().lock();
        System.out.println("Write lock acquired by " + Thread.currentThread());
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(true));
        assertThat(lock.getWriteHoldCount(), is(1));

        thread.start();
        Eventually.assertDeferred(lock::getQueueLength, is(1));
        Eventually.assertDeferred(lock::hasQueuedThreads, is(true));
        Eventually.assertDeferred(() -> lock.hasQueuedThread(thread), is(true));

        lock.writeLock().unlock();
        System.out.println("Write lock released by " + Thread.currentThread());

        s1.acquire();
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(false));
        assertThat(lock.getWriteHoldCount(), is(0));

        s2.release();
        thread.join();
        }

    @Test
    void shouldAcquireAndReleaseReadLockFromMultipleThreads()
            throws InterruptedException
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);

        Thread thread = new Thread(() ->
               {
               lock.readLock().lock();
               System.out.println("Read lock acquired by " + Thread.currentThread());
               assertThat(lock.isReadLocked(), is(true));
               assertThat(lock.getReadLockCount(), is(2));
               assertThat(lock.getReadHoldCount(), is(1));

               s1.release();
               s2.acquireUninterruptibly();
               lock.readLock().unlock();
               System.out.println("Read lock released by " + Thread.currentThread());
               });

        lock.readLock().lock();
        System.out.println("Read lock acquired by " + Thread.currentThread());
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.getReadLockCount(), is(1));
        assertThat(lock.getReadHoldCount(), is(1));

        thread.start();
        s1.acquire();
        assertThat(lock.getReadLockCount(), is(2));
        assertThat(lock.getReadHoldCount(), is(1));
        assertThat(lock.getQueueLength(), is(0));
        assertThat(lock.hasQueuedThreads(), is(false));
        assertThat(lock.hasQueuedThread(thread), is(false));

        s2.release();
        lock.readLock().unlock();
        System.out.println("Read lock released by " + Thread.currentThread());

        thread.join();
        }

    @Test
    void shouldTimeOutWriteLockIfWriteLockIsHeldByAnotherThread()
            throws InterruptedException
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);

        final Thread thread = new Thread(() ->
               {
               lock.writeLock().lock();
               System.out.println("Write lock acquired by " + Thread.currentThread());

               s1.release();
               s2.acquireUninterruptibly();

               lock.writeLock().unlock();
               System.out.println("Write lock released by " + Thread.currentThread());
               });

        thread.start();
        s1.acquire();
        assertThat(lock.writeLock().tryLock(100, TimeUnit.MILLISECONDS), is(false));

        s2.release();
        thread.join();
        }

    @Test
    void shouldTimeOutReadLockIfWriteLockIsHeldByAnotherThread()
            throws InterruptedException
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);

        final Thread thread = new Thread(() ->
               {
               lock.writeLock().lock();
               System.out.println("Write lock acquired by " + Thread.currentThread());

               s1.release();
               s2.acquireUninterruptibly();

               lock.writeLock().unlock();
               System.out.println("Write lock released by " + Thread.currentThread());
               });

        thread.start();
        s1.acquire();
        assertThat(lock.readLock().tryLock(100, TimeUnit.MILLISECONDS), is(false));

        s2.release();
        thread.join();
        }

    @Test
    void shouldBeAbleToInterruptWriteLockRequest()
            throws InterruptedException
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");

        Thread thread = new Thread(() ->
               {
               try
                   {
                   lock.writeLock().lockInterruptibly();
                   }
               catch (InterruptedException e)
                   {
                   System.out.println("Write lock interrupted");
                   assertThat(lock.isWriteLocked(), is(true));
                   assertThat(lock.isWriteLockedByCurrentThread(), is(false));
                   assertThat(lock.getQueueLength(), is(0));
                   }
               });

        lock.writeLock().lock();
        thread.start();

        System.out.println("Write lock acquired by " + Thread.currentThread());
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(true));
        assertThat(lock.getWriteHoldCount(), is(1));

        Eventually.assertDeferred(lock::getQueueLength, is(1));
        thread.interrupt();
        thread.join();

        lock.writeLock().unlock();
        System.out.println("Write lock released by " + Thread.currentThread());
        }

    @Test
    void shouldBeAbleToInterruptReadLockRequest()
            throws InterruptedException
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        Thread thread = new Thread(() ->
               {
               try
                   {
                   lock.readLock().lockInterruptibly();
                   }
               catch (InterruptedException e)
                   {
                   System.out.println("Read lock interrupted");
                   assertThat(lock.isReadLocked(), is(false));
                   assertThat(lock.isWriteLocked(), is(true));
                   assertThat(lock.isWriteLockedByCurrentThread(), is(false));
                   assertThat(lock.getQueueLength(), is(0));
                   }
               });

        lock.writeLock().lock();
        thread.start();

        System.out.println("Write lock acquired by " + Thread.currentThread());
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(true));
        assertThat(lock.getWriteHoldCount(), is(1));

        Eventually.assertDeferred(lock::getQueueLength, is(1));
        thread.interrupt();
        thread.join();

        lock.writeLock().unlock();
        System.out.println("Write lock released by " + Thread.currentThread());
        }

    @Test
    void shouldBeAbleToDowngradeWriteLockToReadLock()
            throws InterruptedException
        {
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");
        Semaphore s1 = new Semaphore(0);
        Semaphore s2 = new Semaphore(0);

        Thread thread = new Thread(() ->
               {
               lock.readLock().lock();
               System.out.println("Read lock acquired by " + Thread.currentThread());
               s1.release();

               s2.acquireUninterruptibly();
               lock.readLock().unlock();
               System.out.println("Read lock released by " + Thread.currentThread());
               });

        lock.writeLock().lock();
        thread.start();

        System.out.println("Write lock acquired by " + Thread.currentThread());
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(true));
        assertThat(lock.getWriteHoldCount(), is(1));

        lock.readLock().lock();
        System.out.println("Read lock acquired by " + Thread.currentThread());
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.getReadLockCount(), is(1));
        assertThat(lock.getReadHoldCount(), is(1));

        lock.writeLock().unlock();
        assertThat(lock.isWriteLockedByCurrentThread(), is(false));
        System.out.println("Write lock released by " + Thread.currentThread());

        s1.acquire();
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.getReadLockCount(), is(2));
        assertThat(lock.getReadHoldCount(), is(1));

        s2.release();
        lock.readLock().unlock();
        System.out.println("Read lock released by " + Thread.currentThread());
        thread.join();
        }
    }
