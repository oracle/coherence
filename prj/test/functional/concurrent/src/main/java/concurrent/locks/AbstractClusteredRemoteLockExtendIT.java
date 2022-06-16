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
import com.oracle.bedrock.runtime.concurrent.callable.RemoteCallableStaticMethod;

import com.oracle.bedrock.testsupport.junit.AbstractTestLogs;
import concurrent.locks.AbstractClusteredRemoteLockIT.AcquireLock;
import concurrent.locks.AbstractClusteredRemoteLockIT.LockEventListener;
import concurrent.locks.AbstractClusteredRemoteLockIT.TryLock;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.concurrent.locks.Locks;
import com.oracle.coherence.concurrent.locks.RemoteLock;

import com.tangosol.util.Base;

import java.io.Serializable;

import java.util.concurrent.CompletableFuture;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * Test distributed locks across Extend clients.
 * <p>
 * This class must be Serializable so that its methods can be used as
 * remote callables by Bedrock.
 */
public abstract class AbstractClusteredRemoteLockExtendIT
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    public AbstractClusteredRemoteLockExtendIT(CoherenceClusterExtension coherenceResource)
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
    public void shouldAcquireAndReleaseLockOnExtendClientMember()
        {
        // Get a storage member from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("client-1");
        // Run the "shouldAcquireAndReleaseLock" method on the extend client member
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
    void shouldTimeOutIfTheLockIsHeldByAnotherMemberUsingExtendClientMembers() throws Exception
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

        shouldTimeOutIfTheLockIsHeldByAnotherMember(member1, member2);
        }

    @Test
    void shouldTimeOutIfTheLockIsHeldByAnotherMemberUsingClientAndStorageMembers() throws Exception
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-1");

        shouldTimeOutIfTheLockIsHeldByAnotherMember(member1, member2);
        }

    @Test
    void shouldTimeOutIfTheLockIsHeldByAnotherMemberUsingStorageAndClientMembers() throws Exception
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-2");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

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
        TryLock tryLock = new AbstractClusteredRemoteLockIT.TryLock(sLockName, Duration.ofMillis(500));
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
    void shouldAcquireLockHeldByFailedExtendClient() throws Exception
        {
        // Get extend client member from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-3");
        // Get extend client member from the cluster
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
        RemoteCallable<Void> exit = new RemoteCallableStaticMethod<>("java.lang.System", "exit", 1);
        member1.submit(exit);

        // wait for the lock acquired event from the second member
        listener2.awaitAcquired(Duration.ofMinutes(1));
        listener2.awaitReleased(Duration.ofMinutes(1));
        }

    @Test
    void shouldAcquireAndReleaseLockInOrderFromMultipleExtendClientMembers() throws Exception
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

        shouldAcquireAndReleaseLockInOrderFromMultipleMembers(member1, member2);
        }

    @Test
    void shouldAcquireAndReleaseLockInOrderFromStorageAndExtendClientMembers() throws Exception
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-1");

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
