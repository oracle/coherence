/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.locks;

import com.oracle.bedrock.junit.CoherenceClusterExtension;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.concurrent.RemoteChannel;
import com.oracle.bedrock.runtime.concurrent.RemoteEvent;
import com.oracle.bedrock.runtime.concurrent.RemoteEventListener;

import com.oracle.bedrock.runtime.concurrent.callable.RemoteCallableStaticMethod;

import com.oracle.bedrock.testsupport.junit.AbstractTestLogs;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.atomic.Atomics;

import com.oracle.coherence.concurrent.locks.Locks;
import com.oracle.coherence.concurrent.locks.RemoteLock;
import com.tangosol.util.Base;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.Serializable;

import java.time.Duration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * Test distributed locks across multiple cluster members.
 * <p>
 * This class must be Serializable so that its methods can be used as
 * remote callables by Bedrock.
 */
public abstract class AbstractClusteredRemoteLockIT
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    AbstractClusteredRemoteLockIT(CoherenceClusterExtension coherenceResource)
        {
        m_coherenceResource = coherenceResource;
        }

    @BeforeEach
    void beforeEach(TestInfo info)
        {
        // print a message in the logs of all the cluster members that are still running
        // to indicate the name of the test that is about to start
        String sMessage = ">>>>> Starting test method " + info.getDisplayName();
        logOnEachMember(sMessage);
        }

    @AfterEach
    void after(TestInfo info)
        {
        // print a message in the logs of all the cluster members that are still running
        // to indicate the name of the test that has just finished
        String sMessage = "<<<<< Completed test method " + info.getDisplayName();
        logOnEachMember(sMessage);
        }

    private void logOnEachMember(String sMessage)
        {
        m_coherenceResource.getCluster()
                .forEach(m ->
                         {
                         try
                             {
                             m.invoke(() ->
                                      {
                                      Logger.info(sMessage);
                                      return null;
                                      });
                             }
                         catch (Throwable ignore)
                             {
                             // ignoring "RemoteChannel is closed" exception
                             // from members that were shut down
                             }
                         });
        }

    @Test
    public void shouldAcquireAndReleaseLockOnStorageMember()
        {
        // Get a storage member from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("storage-1");
        // Run the "shouldAcquireAndReleaseLock" method on the storage member
        // If any assertions fail this method will throw an exception
        member.invoke(this::shouldAcquireAndReleaseLock);
        }

    @Test
    public void shouldAcquireAndReleaseLockOnStorageDisabledMember()
        {
        // Get a storage disabled application member from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("application-1");
        // Run the "shouldAcquireAndReleaseLock" method on the storage member
        // If any assertions fail this method will throw an exception
        member.invoke(this::shouldAcquireAndReleaseLock);
        }

    /**
     * This test method is invoked on remote processes by Bedrock.
     *
     * This method must have a return value as it is invoked as a
     * RemoteCallable so that the invoke call blocks until the
     * method has completes. In this case we do not care about the
     * actual return value, so we use Void.
     *
     * If any of the assertions fail, the invoke call in the test will fail.
     *
     * @return always returns Void (null).
     */
    Void shouldAcquireAndReleaseLock()
        {
        Logger.info("In shouldAcquireAndReleaseLock()");
        RemoteLock lock = Locks.remoteLock("foo");

        lock.lock();
        System.out.println("Lock acquired by " + lock.getOwner());
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isHeldByCurrentThread(), is(true));
        assertThat(lock.getHoldCount(), is(1L));

        lock.unlock();
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.isHeldByCurrentThread(), is(false));
        assertThat(lock.getHoldCount(), is(0L));
        System.out.println("Lock released by " + Thread.currentThread());
        return null;
        }

    @Test
    void shouldTimeOutIfTheLockIsHeldByAnotherMemberUsingStorageMembers() throws Exception
        {
        // Get storage members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");

        shouldTimeOutIfTheLockIsHeldByAnotherMember(member1, member2);
        }

    @Test
    void shouldTimeOutIfTheLockIsHeldByAnotherMemberUsingStorageDisabledMembers() throws Exception
        {
        // Get storage disabled application members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("application-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("application-2");

        shouldTimeOutIfTheLockIsHeldByAnotherMember(member1, member2);
        }

    /**
     * This test acquires a lock on one cluster member for a specific duration and then tries to acquire
     * the same lock on another member.
     *
     * @param member1  the member to acquire the lock on
     * @param member2  the member to try to acquire the lock on
     *
     * @throws Exception if the test fails
     */
    void shouldTimeOutIfTheLockIsHeldByAnotherMember(CoherenceClusterMember member1, CoherenceClusterMember member2) throws Exception
        {
        String            sLockName = "foo";
        LockEventListener listener1  = new LockEventListener(sLockName);
        LockEventListener listener2  = new LockEventListener(sLockName);

        // Add the listener to listen for lock events from the first member.
        member1.addListener(listener1);
        member2.addListener(listener2);

        // Acquire the lock on first member (the lock will be held for 5 seconds)
        member1.submit(new AcquireLock(sLockName, Duration.ofSeconds(5)));
        // wait for the lock acquired event
        listener1.awaitAcquired(Duration.ofMinutes(1));

        // try and acquire the lock on the second member (should time out after 500 millis)
        TryLock tryLock = new TryLock(sLockName, Duration.ofMillis(500));
        CompletableFuture<Boolean> futureTry = member2.submit(tryLock);
        assertThat(futureTry.get(), is(false));

        // wait for the lock released event from the first member
        listener1.awaitReleased(Duration.ofMinutes(1));

        // try again to acquire the lock on the second member (should succeed)
        futureTry = member2.submit(tryLock);
        assertThat(futureTry.get(), is(true));
        listener2.awaitAcquired(Duration.ofMinutes(1));
        listener2.awaitReleased(Duration.ofMinutes(1));
        }

    @Test
    void shouldAcquireLockHeldByFailedStorageMember() throws Exception
        {
        // Get storage members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-3");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");

        shouldAcquireLockHeldByFailedMember(member1, member2);
        }

    @Test
    void shouldAcquireLockHeldByFailedStorageDisabledMember() throws Exception
        {
        // Get storage disabled application members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("application-3");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("application-2");

        shouldAcquireLockHeldByFailedMember(member1, member2);
        }

    /**
     * This test checks that a lock held by a failed member is automatically released,
     * and subsequently acquired by another member.
     *
     * @param member1  the first member to acquire the lock on and then kill
     * @param member2  the second member to try to acquire the lock on
     *
     * @throws Exception if the test fails
     */
    void shouldAcquireLockHeldByFailedMember(CoherenceClusterMember member1, CoherenceClusterMember member2) throws Exception
        {
        String            sLockName = "foo";
        LockEventListener listener1  = new LockEventListener(sLockName);
        LockEventListener listener2  = new LockEventListener(sLockName);

        // Add the listeners to listen for lock events
        member1.addListener(listener1);
        member2.addListener(listener2);

        Base.sleep(1000);

        // Acquire the lock on first member (the lock will be held for 5 minutes)
        member1.submit(new AcquireLock(sLockName, Duration.ofMinutes(5)));

        // wait for the lock acquired event
        listener1.awaitAcquired(Duration.ofMinutes(1));

        // Acquire the lock on second member (the lock will be held for 5 seconds)
        member2.submit(new AcquireLock(sLockName, Duration.ofSeconds(5)));

        // Kill first member
        RemoteCallable<Void> closeAll = new RemoteCallableStaticMethod<>("com.tangosol.net.Coherence", "closeAll");
        member1.submit(closeAll);

        // wait for the lock acquired event from the second member
        listener2.awaitAcquired(Duration.ofMinutes(1));
        listener2.awaitReleased(Duration.ofMinutes(1));
        }

    @Test
    void shouldAcquireAndReleaseLockInOrderFromMultipleStorageMembers() throws Exception
        {
        // Get storage members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");

        shouldAcquireAndReleaseLockInOrderFromMultipleMembers(member1, member2);
        }

    @Test
    void shouldAcquireAndReleaseLockInOrderFromMultipleStorageDisabledMembers() throws Exception
        {
        // Get storage disabled application members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("application-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("application-2");

        shouldAcquireAndReleaseLockInOrderFromMultipleMembers(member1, member2);
        }

    /**
     * This test acquires the same lock from multiple members.
     * The first member should acquire the lock and the second member should block until the
     * first has released the lock.
     *
     * @param member1  the first member to acquire the lock
     * @param member2  the second member to acquire the lock
     *
     * @throws Exception if the test fails
     */
    void shouldAcquireAndReleaseLockInOrderFromMultipleMembers(CoherenceClusterMember member1, CoherenceClusterMember member2) throws Exception
        {
        String            sLockName = "foo";
        LockEventListener listener1 = new LockEventListener(sLockName);
        LockEventListener listener2 = new LockEventListener(sLockName);

        // Add the listener to listen for lock events from the first member.
        member1.addListener(listener1);
        member2.addListener(listener2);

        // Acquire the lock on first member (the lock will be held for 2 seconds)
        member1.submit(new AcquireLock(sLockName, Duration.ofSeconds(2)));
        // wait for the lock acquired event
        listener1.awaitAcquired(Duration.ofMinutes(1));

        // Try to acquire the lock on second member (should fail)
        assertThat(member2.invoke(new TryLock(sLockName)), is(false));

        // Acquire the lock on the second member, should block until the first member releases
        member2.submit(new AcquireLock(sLockName, Duration.ofSeconds(1)));

        // wait for the second member to acquire the lock (should be after member 1 releases the lock)
        listener2.awaitAcquired(Duration.ofMinutes(1));
        // wait for the second member to release the lock
        listener2.awaitReleased(Duration.ofMinutes(1));

        // Assert the locks were acquired and released in the order expected
        System.out.println("Acquired #1: " + listener1.getAcquiredOrder());
        System.out.println("Released #1: " + listener1.getReleasedOrder());
        System.out.println("Acquired #2: " + listener2.getAcquiredOrder());
        System.out.println("Released #2: " + listener2.getReleasedOrder());
        assertThat(listener1.getAcquiredOrder(), lessThan(listener1.getReleasedOrder()));
        assertThat(listener1.getReleasedOrder(), lessThan(listener2.getAcquiredOrder()));
        assertThat(listener2.getAcquiredOrder(), lessThan(listener2.getReleasedOrder()));
        }

    // ----- inner class: TryLock -------------------------------------------

    /**
     * A Bedrock remote callable that tries to acquire a lock within a given timeout.
     * <p>
     * The result of the call to {@link RemoteLock#tryLock()} is returned.
     * If the lock was acquired it is immediately released.
     */
    static class TryLock
            implements RemoteCallable<Boolean>
        {
        /**
         * A remote channel injected by Bedrock and used to fire events back to the test.
         */
        @RemoteChannel.Inject
        private RemoteChannel remoteChannel;

        /**
         * The name of the lock to acquire.
         */
        private final String f_sLockName;

        /**
         * The amount of time to wait to acquire the lock.
         */
        private final Duration f_timeout;

        /**
         * Create a {@link TryLock} callable.
         *
         * @param sLockName  the name of the lock to acquire
         */
        public TryLock(String sLockName)
            {
            f_sLockName = sLockName;
            f_timeout   = Duration.ZERO;
            }

        /**
         * Create a {@link TryLock} callable.
         *
         * @param sLockName  the name of the lock to acquire
         * @param duration   the amount of time to wait to acquire the lock
         */
        public TryLock(String sLockName, Duration duration)
            {
            f_sLockName = sLockName;
            f_timeout = duration;
            }

        @Override
        public Boolean call() throws Exception
            {
            RemoteLock lock = Locks.remoteLock(f_sLockName);

            boolean fAcquired;
            if (f_timeout.isZero())
                {
                Logger.info("Trying to acquire lock " + f_sLockName + " with zero timeout");
                fAcquired = lock.tryLock();
                }
            else
                {
                Logger.info("Trying to acquire lock " + f_sLockName + " with timeout of " + f_timeout);
                fAcquired = lock.tryLock(f_timeout.toMillis(), TimeUnit.MILLISECONDS);
                }

            if (fAcquired)
                {
                remoteChannel.raise(new LockEvent(f_sLockName, LockEventType.Acquired));
                Logger.info("Tried and succeeded to acquire lock " + f_sLockName + " within timeout " + f_timeout);
                remoteChannel.raise(new LockEvent(f_sLockName, LockEventType.Released));
                lock.unlock();
                }
            else
                {
                Logger.info("Tried and failed to acquire lock " + f_sLockName + " within timeout " + f_timeout);
                }

            return fAcquired;
            }
        }

    // ----- inner class: AcquireLock ---------------------------------------

    /**
     * A Bedrock remote callable that acquires a lock for a specific amount of time.
     * <p>
     * This callable fires remote events to indicate when the lock was acquired and released.
     */
    static class AcquireLock
            implements RemoteCallable<Void>
        {
        /**
         * A remote channel injected by Bedrock and used to fire events back to the test.
         */
        @RemoteChannel.Inject
        private RemoteChannel remoteChannel;

        /**
         * The name of the lock to acquire.
         */
        private final String f_sLockName;

        /**
         * The duration to hold the lock for.
         */
        private final Duration f_duration;

        /**
         * Create an {@link AcquireLock} callable.
         *
         * @param sLockName  the name of the lock to acquire
         * @param duration   the duration to hold the lock for
         */
        AcquireLock(String sLockName, Duration duration)
            {
            f_sLockName = sLockName;
            f_duration  = duration;
            }

        @Override
        public Void call()
            {
            Logger.info("Acquiring lock " + f_sLockName);
            RemoteLock lock = Locks.remoteLock(f_sLockName);
            lock.lock();
            try
                {
                remoteChannel.raise(new LockEvent(f_sLockName, LockEventType.Acquired));
                Logger.info("Lock " + f_sLockName + " acquired by " + lock.getOwner());
                Thread.sleep(f_duration.toMillis());
                }
            catch (InterruptedException ignore)
                {
                }
            finally
                {
                remoteChannel.raise(new LockEvent(f_sLockName, LockEventType.Released));
                lock.unlock();
                Logger.info("Lock " + f_sLockName + " released by " + Thread.currentThread());
                }
            return null;
            }
        }

    // ----- inner class: LockEvent -----------------------------------------

    /**
     * A Bedrock remote event submitted by the {@link AcquireLock} callable
     * to notify the calling test when the lock has been acquired and released.
     */
    static class LockEvent
            implements RemoteEvent
        {
        /**
         * The name of the lock.
         */
        private final String f_sLockName;

        /**
         * The type of the event.
         */
        private final LockEventType f_type;

        /**
         * The global order of the event.
         */
        private final int f_order;

        /**
         * Create a lock event.
         *
         * @param sLockName  the name of the lock
         * @param type       the type of the event
         */
        public LockEvent(String sLockName, LockEventType type)
            {
            f_sLockName = sLockName;
            f_type      = type;
            f_order     = Atomics.remoteAtomicInteger("ClusteredDistributedLockIT.eventCounter").incrementAndGet();
            }

        /**
         * Returns the name of the lock.
         *
         * @return  the name of the lock
         */
        public String getLockName()
            {
            return f_sLockName;
            }

        /**
         * Returns the event type.
         *
         * @return  the event type
         */
        public LockEventType getEventType()
            {
            return f_type;
            }

        /**
         * Return the global event order.
         *
         * @return the global event order
         */
        public int getOrder()
            {
            return f_order;
            }
        }

    // ----- inner class LockEventListener ----------------------------------

    /**
     * A {@link RemoteEventListener} that listens for {@link LockEvent lock events}.
     */
    static class LockEventListener
            implements RemoteEventListener
        {
        /**
         * The name of the lock.
         */
        private final String f_sLockName;

        /**
         * A future that completes when the lock acquired event is received.
         */
        private final CompletableFuture<Integer> f_futureAcquired = new CompletableFuture<>();

        /**
         * A future that completes when the lock released event is received.
         */
        private final CompletableFuture<Integer> f_futureReleased = new CompletableFuture<>();

        /**
         * Create a {@link LockEventListener}.
         *
         * @param sLockName  the name of the lock
         */
        public LockEventListener(String sLockName)
            {
            f_sLockName = sLockName;
            }

        @Override
        public void onEvent(RemoteEvent event)
            {
            if (event instanceof LockEvent)
                {
                LockEvent e = (LockEvent) event;
                if (f_sLockName.equals(e.getLockName()))
                    {
                    switch (e.getEventType())
                        {
                        case Acquired:
                            f_futureAcquired.complete(e.getOrder());
                            break;
                        case Released:
                            f_futureReleased.complete(e.getOrder());
                            break;
                        }
                    }
                }
            }

        /**
         * Wait for the lock acquired event.
         *
         * @param timeout  the maximum amount of time to wait
         *
         * @return the global order of the lock acquired event
         */
        public int awaitAcquired(Duration timeout) throws Exception
            {
            return f_futureAcquired.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }

        /**
         * Returns true if the lock has been acquired.
         *
         * @return true if the lock has been acquired
         */
        public boolean isAcquired()
            {
            return f_futureAcquired.isDone();
            }

        /**
         * Returns the global order of the lock acquired event.
         *
         * @return the global order of the lock acquired event
         */
        public int getAcquiredOrder()
            {
            return f_futureAcquired.join();
            }

        /**
         * Wait for the lock released event.
         *
         * @param timeout  the maximum amount of time to wait
         *
         * @return the global order of the lock released event
         */
        public int awaitReleased(Duration timeout) throws Exception
            {
            return f_futureReleased.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }

        /**
         * Returns true if the lock has been acquired and released.
         *
         * @return true if the lock has been acquired and released
         */
        public boolean isReleased()
            {
            return f_futureAcquired.isDone() && f_futureReleased.isDone();
            }

        /**
         * Returns the global order of the lock released event.
         *
         * @return the global order of the lock released event
         */
        public int getReleasedOrder()
            {
            return f_futureReleased.join();
            }
        }

    // ----- inner enum LockEventType ---------------------------------------

    /**
     * An enum of lock event types.
     */
    enum LockEventType {Acquired, Released}

    // ----- data members ---------------------------------------------------

    /**
     * A Bedrock JUnit5 extension with a Coherence cluster for the tests.
     */
    static CoherenceClusterExtension m_coherenceResource;

    /**
     * This is a work-around to fix the fact that the JUnit5 test logs extension
     * in Bedrock does not work for BeforeAll methods and extensions.
     */
    static class TestLogs
            extends AbstractTestLogs
        {
        public TestLogs(Class<?> testClass)
            {
            init(testClass, "BeforeAll");
            }
        }
    }
