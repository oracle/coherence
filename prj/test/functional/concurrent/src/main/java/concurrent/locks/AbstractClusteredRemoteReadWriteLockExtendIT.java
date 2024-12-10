/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.locks;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.concurrent.callable.RemoteCallableStaticMethod;
import com.oracle.bedrock.testsupport.junit.AbstractTestLogs;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.concurrent.locks.Locks;
import com.oracle.coherence.concurrent.locks.RemoteReadWriteLock;

import com.tangosol.util.Base;

import java.io.Serializable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

public abstract class AbstractClusteredRemoteReadWriteLockExtendIT
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    AbstractClusteredRemoteReadWriteLockExtendIT(CoherenceClusterExtension coherenceResource)
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
    public void shouldAcquireAndReleaseLocksOnExtendClientMember()
        {
        // Get extend client member from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("client-1");

        // If any assertions in the methods below fail this method will throw an exception
        member.invoke(this::shouldAcquireAndReleaseWriteLock);
        member.invoke(this::shouldAcquireAndReleaseReadLock);
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
    Void shouldAcquireAndReleaseWriteLock()
        {
        Logger.info("In shouldAcquireAndReleaseWriteLock()");
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");

        lock.writeLock().lock();
        System.out.println("Write lock acquired by " + lock.getOwner());
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedByCurrentThread(), is(true));
        assertThat(lock.getWriteHoldCount(), is(1));

        lock.writeLock().unlock();
        assertThat(lock.isWriteLocked(), is(false));
        assertThat(lock.isWriteLockedByCurrentThread(), is(false));
        assertThat(lock.getWriteHoldCount(), is(0));
        System.out.println("Write lock released by " + Thread.currentThread());
        return null;
        }

    Void shouldAcquireAndReleaseReadLock()
        {
        Logger.info("In shouldAcquireAndReleaseReadLock()");
        RemoteReadWriteLock lock = Locks.remoteReadWriteLock("foo");

        lock.readLock().lock();
        System.out.println("Read lock acquired by " + Thread.currentThread());
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.getReadLockCount(), is(1));
        assertThat(lock.getReadHoldCount(), is(1));

        lock.readLock().unlock();
        assertThat(lock.isReadLocked(), is(false));
        assertThat(lock.getReadLockCount(), is(0));
        assertThat(lock.getReadHoldCount(), is(0));
        System.out.println("Read lock released by " + Thread.currentThread());
        return null;
        }

    @Test
    void shouldTimeOutIfWriteLockIsHeldByAnotherMemberUsingExtendClientMembers() throws Exception
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

        shouldTimeOutIfWriteLockIsHeldByAnotherMember(member1, member2);
        }

    /**
     * This test acquires a write lock on one cluster member for a specific duration and then tries to acquire
     * the same lock on another member.
     *
     * @param member1  the member to acquire the lock on
     * @param member2  the member to try to acquire the lock on
     *
     * @throws Exception if the test fails
     */
    void shouldTimeOutIfWriteLockIsHeldByAnotherMember(CoherenceClusterMember member1, CoherenceClusterMember member2) throws Exception
        {
        String            sLockName = "foo";
        AbstractClusteredRemoteReadWriteLockIT.LockEventListener listener1  = new AbstractClusteredRemoteReadWriteLockIT.LockEventListener(sLockName);
        AbstractClusteredRemoteReadWriteLockIT.LockEventListener listener2  = new AbstractClusteredRemoteReadWriteLockIT.LockEventListener(sLockName);

        // Add the listeners to listen for lock events
        member1.addListener(listener1);
        member2.addListener(listener2);

        // Acquire the lock on first member (the lock will be held for 5 seconds)
        Duration writeLockDuration = Duration.ofSeconds(5);

        member1.submit(new AbstractClusteredRemoteReadWriteLockIT.AcquireWriteLock(sLockName, writeLockDuration));
        // wait for the lock acquired event
        listener1.awaitWriteAcquired(Duration.ofMinutes(1));

        long ldtStartLockHeld = System.currentTimeMillis();

        // try to acquire read lock on the second member (should time out after 500 millis)
        AbstractClusteredRemoteReadWriteLockIT.TryReadLock tryReadLock = new AbstractClusteredRemoteReadWriteLockIT.TryReadLock(sLockName, Duration.ofMillis(500));
        CompletableFuture<Boolean> futureTryRead = member2.submit(tryReadLock);
        assertThat("client2 must not get read lock unless client1 has released write lock=" + listener1.isWriteReleased(),
                   futureTryRead.get() == false ||
                   (listener1.isWriteReleased() && (System.currentTimeMillis() - ldtStartLockHeld > writeLockDuration.toMillis())),
                   is(true));

        // try to acquire write lock on the second member (should time out after 500 millis)
        AbstractClusteredRemoteReadWriteLockIT.TryWriteLock tryWriteLock = new AbstractClusteredRemoteReadWriteLockIT.TryWriteLock(sLockName, Duration.ofMillis(500));
        CompletableFuture<Boolean> futureTryWrite = member2.submit(tryWriteLock);
        assertThat("client2 must not get write lock unless client1 has released write lock=" + listener1.isWriteReleased(),
                   futureTryWrite.get() == false ||
                   (listener1.isWriteReleased() && (System.currentTimeMillis() - ldtStartLockHeld > writeLockDuration.toMillis())),
                   is(true));

        // wait for the write lock released event from the first member
        listener1.awaitWriteReleased(Duration.ofMinutes(1));

        // try again to acquire the write lock on the second member (should succeed)
        futureTryWrite = member2.submit(tryWriteLock);
        assertThat(futureTryWrite.get(), is(true));

        // wait for the write lock acquired and released event from the second member
        listener2.awaitWriteAcquired(Duration.ofMinutes(1));
        listener2.awaitWriteReleased(Duration.ofMinutes(1));

        // try again to acquire the read lock on the second member (should succeed)
        futureTryRead = member2.submit(tryReadLock);
        assertThat(futureTryRead.get(), is(true));

        // wait for the read lock acquired and released event from the second member
        listener2.awaitReadAcquired(Duration.ofMinutes(1));
        listener2.awaitReadReleased(Duration.ofMinutes(1));
        }

    @Test
    void shouldAcquireReadLockFromMultipleExtendClientMembers() throws Exception
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

        shouldAcquireReadLockFromMultipleMembers(member1, member2);
        }

    @Test
    void shouldAcquireReadLockFromAllMembers() throws Exception
        {
        // Get all members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");
        CoherenceClusterMember member3 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member4 = m_coherenceResource.getCluster().get("client-2");

        shouldAcquireReadLockFromMultipleMembers(member1, member2, member3, member4);
        }

    /**
     * This test acquires a read lock on each specified cluster member for a specific duration.
     *
     * @param aMembers  the members to acquire the read lock on
     *
     * @throws Exception if the test fails
     */
    void shouldAcquireReadLockFromMultipleMembers(CoherenceClusterMember... aMembers) throws Exception
        {
        String sLockName = "foo";

        Set<CoherenceClusterMember> members = Set.of(aMembers);

        // Add the listeners to listen for lock events from each first member.
        Set<AbstractClusteredRemoteReadWriteLockIT.LockEventListener> listeners = new HashSet<>();
        for (CoherenceClusterMember member : members)
            {
            AbstractClusteredRemoteReadWriteLockIT.LockEventListener listener = new AbstractClusteredRemoteReadWriteLockIT.LockEventListener(sLockName);
            member.addListener(listener);
            listeners.add(listener);
            }

        // Acquire the read lock on each member (the lock will be held for 5 seconds)
        members.forEach(member -> member.submit(new AbstractClusteredRemoteReadWriteLockIT.AcquireReadLock(sLockName, Duration.ofSeconds(5))));

        // wait for the lock acquired event from each member
        for (AbstractClusteredRemoteReadWriteLockIT.LockEventListener listener : listeners)
            {
            listener.awaitReadAcquired(Duration.ofMinutes(1));
            }

        // wait for the lock released event from each member
        for (AbstractClusteredRemoteReadWriteLockIT.LockEventListener listener : listeners)
            {
            listener.awaitReadReleased(Duration.ofMinutes(1));
            }
        }

    @Test
    void shouldAcquireAndReleaseLockInOrderFromMultipleExtendClientMembers() throws Exception
        {
        // Get storage members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

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
        AbstractClusteredRemoteReadWriteLockIT.LockEventListener listener1 = new AbstractClusteredRemoteReadWriteLockIT.LockEventListener(sLockName);
        AbstractClusteredRemoteReadWriteLockIT.LockEventListener listener2 = new AbstractClusteredRemoteReadWriteLockIT.LockEventListener(sLockName);

        // Add the listeners to listen for lock events
        member1.addListener(listener1);
        member2.addListener(listener2);

        // Acquire the lock on first member (the lock will be held for 2 seconds)
        member1.submit(new AbstractClusteredRemoteReadWriteLockIT.AcquireWriteLock(sLockName, Duration.ofSeconds(2)));
        // wait for the lock acquired event
        listener1.awaitWriteAcquired(Duration.ofMinutes(1));

        // Try to acquire the lock on second member (should fail)
        assertThat(member2.invoke(new AbstractClusteredRemoteReadWriteLockIT.TryWriteLock(sLockName)), is(false));

        // Acquire the lock on the second member, should block until the first member releases
        member2.submit(new AbstractClusteredRemoteReadWriteLockIT.AcquireWriteLock(sLockName, Duration.ofSeconds(1)));

        // wait for the second member to acquire the lock (should be after member 1 releases the lock)
        listener2.awaitWriteAcquired(Duration.ofMinutes(1));
        // wait for the second member to release the lock
        listener2.awaitWriteReleased(Duration.ofMinutes(1));

        // Assert the locks were acquired and released in the order expected
        System.out.println("Acquired #1: " + listener1.getWriteAcquiredOrder());
        System.out.println("Released #1: " + listener1.getWriteReleasedOrder());
        System.out.println("Acquired #2: " + listener2.getWriteAcquiredOrder());
        System.out.println("Released #2: " + listener2.getWriteReleasedOrder());
        assertThat(listener1.getWriteAcquiredOrder(), lessThan(listener1.getWriteReleasedOrder()));
        assertThat(listener1.getWriteReleasedOrder(), lessThan(listener2.getWriteAcquiredOrder()));
        assertThat(listener2.getWriteAcquiredOrder(), lessThan(listener2.getWriteReleasedOrder()));
        }

    @Test
    void shouldAcquireLockHeldByFailedExtendClientMember() throws Exception
        {
        // Get storage members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-3");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

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
        AbstractClusteredRemoteReadWriteLockIT.LockEventListener foo1  = new AbstractClusteredRemoteReadWriteLockIT.LockEventListener("foo");
        AbstractClusteredRemoteReadWriteLockIT.LockEventListener foo2  = new AbstractClusteredRemoteReadWriteLockIT.LockEventListener("foo");
        AbstractClusteredRemoteReadWriteLockIT.LockEventListener bar1  = new AbstractClusteredRemoteReadWriteLockIT.LockEventListener("bar");
        AbstractClusteredRemoteReadWriteLockIT.LockEventListener bar2  = new AbstractClusteredRemoteReadWriteLockIT.LockEventListener("bar");

        // Add the listeners to listen for lock events from the first member.
        member1.addListener(foo1);
        member1.addListener(bar1);
        // Add the listeners to listen for lock events from the second member.
        member2.addListener(foo2);
        member2.addListener(bar2);

        Base.sleep(1000);

        // Acquire read and write lock on first member (the lock will be held for 1 minute,
        // but should be released as soon as the member is killed)
        member1.submit(new AbstractClusteredRemoteReadWriteLockIT.AcquireWriteLock("foo", Duration.ofMinutes(1)));
        member1.submit(new AbstractClusteredRemoteReadWriteLockIT.AcquireReadLock("bar", Duration.ofMinutes(1)));

        // wait for write and read lock acquired event
        foo1.awaitWriteAcquired(Duration.ofMinutes(1));
        bar1.awaitReadAcquired(Duration.ofMinutes(1));

        // Acquire write locks on second member
        member2.submit(new AbstractClusteredRemoteReadWriteLockIT.AcquireWriteLock("foo", Duration.ofSeconds(5)));
        member2.submit(new AbstractClusteredRemoteReadWriteLockIT.AcquireWriteLock("bar", Duration.ofSeconds(5)));

        // Kill first member
        RemoteCallable<Void> exit = new RemoteCallableStaticMethod<>("java.lang.System", "exit", 1);
        member1.submit(exit);

        // wait for the lock acquired and released events from the second member
        foo2.awaitWriteAcquired(Duration.ofMinutes(1));
        bar2.awaitWriteAcquired(Duration.ofMinutes(1));
        foo2.awaitWriteReleased(Duration.ofMinutes(1));
        bar2.awaitWriteReleased(Duration.ofMinutes(1));
        }


    // ----- data members ---------------------------------------------------

    /**
     * System property to enable the Concurrent service proxy.
     */
    protected static final String EXTEND_ENABLED_PROPERTY = "coherence.concurrent.extend.enabled";

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
