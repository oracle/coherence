/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.concurrent.RemoteChannel;
import com.oracle.bedrock.runtime.concurrent.RemoteEvent;
import com.oracle.bedrock.runtime.concurrent.RemoteEventListener;
import com.oracle.bedrock.runtime.concurrent.callable.RemoteCallableStaticMethod;
import com.oracle.bedrock.testsupport.junit.AbstractTestLogs;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.concurrent.RemoteSemaphore;
import com.oracle.coherence.concurrent.Semaphores;
import com.oracle.coherence.concurrent.atomic.Atomics;

import com.tangosol.util.Base;

import java.io.Serializable;

import java.util.concurrent.CompletableFuture;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * Test distributed semaphores across multiple cluster members.
 * <p>
 * This class must be Serializable so that its methods can be used as
 * remote callables by Bedrock.
 */
public abstract class AbstractClusteredRemoteSemaphoreIT
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    public AbstractClusteredRemoteSemaphoreIT(CoherenceClusterExtension coherenceResource)
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
        clearSemaphoresOnEachMember();
        }

    private void clearSemaphoresOnEachMember()
        {
        m_coherenceResource.getCluster()
                .forEach(member ->
                             {
                             try
                                 {
                                 member.invoke(() ->
                                                   {
                                                   ConcurrentHelper.clearSemaphores();
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
    public void shouldAcquireAndReleasePermitOnStorageMember()
        {
        // Get a storage member from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("storage-1");
        // Run the "shouldAcquireAndReleasePermits" method on the storage member
        // If any assertions fail this method will throw an exception
        member.invoke(() ->
                          {
                          this.shouldAcquireAndReleasePermits("foo-shouldAcquireAndReleasePermitOnStorageMember");
                          return null;
                          });
        }

    @Test
    public void shouldAcquireAndReleasePermitOnStorageDisabledMember()
        {
        // Get a storage disabled application member from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("application-1");
        // Run the "shouldAcquireAndReleasePermits" method on the storage member
        // If any assertions fail this method will throw an exception
        member.invoke(() ->
                          {
                          this.shouldAcquireAndReleasePermits("foo-shouldAcquireAndReleasePermitOnStorageDisabledMember");
                          return null;
                          });
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
    Void shouldAcquireAndReleasePermits(String semaphoreName)
        {
        RemoteSemaphore semaphore = Semaphores.remoteSemaphore(semaphoreName, 1);
        semaphore.acquireUninterruptibly();
        try
            {
            assertThat(semaphore.availablePermits(), is(0));
            }
        finally
            {
            semaphore.release();
            assertThat(semaphore.availablePermits(), is(1));
            }
        return null;
        }

    @Test
    public void shouldAcquireAndReleaseMultiplePermitsOnStorageMember()
        {
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("storage-1");
        member.invoke(() ->
                          {
                          this.shouldAcquireAndReleaseMultiplePermits("foo-shouldAcquireAndReleaseMultiplePermitsOnStorageMember");
                          return null;
                          });
        }

    @Test
    public void shouldAcquireAndReleaseMultiplePermitsOnStorageDisabledMember()
        {
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("application-1");
        member.invoke(() ->
                          {
                          this.shouldAcquireAndReleaseMultiplePermits("foo-shouldAcquireAndReleaseMultiplePermitsOnStorageDisabledMember");
                          return null;
                          });
        }

    Void shouldAcquireAndReleaseMultiplePermits(String semaphoreName)
        {
        RemoteSemaphore semaphore = Semaphores.remoteSemaphore(semaphoreName, 5);
        semaphore.acquireUninterruptibly(3);
        try
            {
            assertThat(semaphore.availablePermits(), is(2));
            }
        finally
            {
            semaphore.release(3);
            assertThat(semaphore.availablePermits(), is(5));
            }
        return null;
        }

    @Test
    void shouldTimeOutIfThePermitIsHeldByAnotherMemberUsingStorageMembers() throws Exception
        {
        // Get storage members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");

        shouldTimeOutIfThePermitIsHeldByAnotherMember(member1, member2, "foo-shouldTimeOutIfThePermitIsHeldByAnotherMemberUsingStorageMembers");
        }

    @Test
    void shouldTimeOutIfThePermitIsHeldByAnotherMemberUsingStorageDisabledMembers() throws Exception
        {
        // Get storage disabled application members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("application-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("application-2");

        shouldTimeOutIfThePermitIsHeldByAnotherMember(member1, member2, "foo-shouldTimeOutIfThePermitIsHeldByAnotherMemberUsingStorageDisabledMembers");
        }
    /**
     * This test acquires a permit on one cluster member for a specific duration and then tries to acquire
     * the permit on another member.
     *

     * @param member1  the member to acquire the permit on
     * @param member2  the member to try to acquire the permit on
     *
     * @throws Exception if the test fails
     */
    void shouldTimeOutIfThePermitIsHeldByAnotherMember(CoherenceClusterMember member1, CoherenceClusterMember member2, String sSemaphoreName) throws Exception
        {
        ClusteredRemoteSemaphoreIT.SemaphoreEventListener listener1 = new ClusteredRemoteSemaphoreIT.SemaphoreEventListener(sSemaphoreName);
        ClusteredRemoteSemaphoreIT.SemaphoreEventListener listener2 = new ClusteredRemoteSemaphoreIT.SemaphoreEventListener(sSemaphoreName);

        try
            {
            // Add the listener to listen for semaphore events from the first member.
            member1.addListener(listener1);
            member2.addListener(listener2);

            // Acquire the permit on first member (the permit will be held for 5 seconds)
            member1.submit(new ClusteredRemoteSemaphoreIT.AcquirePermit(sSemaphoreName, 1, Duration.ofSeconds(5)));
            // wait for the permit acquired event
            listener1.awaitAcquired(Duration.ofMinutes(1));

            // try and acquire the permit on the second member (should time out after 500 millis)
            ClusteredRemoteSemaphoreIT.TryAcquire tryAcquire = new ClusteredRemoteSemaphoreIT.TryAcquire(sSemaphoreName, 1, Duration.ofMillis(500));
            CompletableFuture<Boolean> futureTry = member2.submit(tryAcquire);
            assertThat(futureTry.get(), is(false));

            // wait for the permit released event from the first member
            listener1.awaitReleased(Duration.ofMinutes(1));

            // try again to acquire the permit on the second member (should succeed)
            futureTry = member2.submit(tryAcquire);
            assertThat(futureTry.get(), is(true));
            listener2.awaitAcquired(Duration.ofMinutes(1));
            listener2.awaitReleased(Duration.ofMinutes(1));
            }
        finally
            {
            member1.removeListener(listener1);
            member2.removeListener(listener2);
            }
        }

    @Test
    void shouldAcquirePermitHeldByFailedStorageMember() throws Exception
        {
        // Get storage members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-3");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");

        shouldAcquirePermitHeldByFailedMember(member1, member2, "foo-shouldAcquirePermitHeldByFailedStorageMember");
        }

    @Test
    void shouldAcquirePermitHeldBFailedStorageDisabledMember() throws Exception
        {
        // Get storage disabled application members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("application-3");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("application-2");

        shouldAcquirePermitHeldByFailedMember(member1, member2, "foo-shouldAcquirePermitHeldBFailedStorageDisabledMember");
        }

    /**
     * This test checks that a permit held by a failed member is automatically released,
     * and subsequently acquired by another member.
     *
     * @param member1  the first member to acquire the permit on and then kill
     * @param member2  the second member to try to acquire the permit on
     *
     * @throws Exception if the test fails
     */
    void shouldAcquirePermitHeldByFailedMember(CoherenceClusterMember member1, CoherenceClusterMember member2, String sSemaphoreName) throws Exception
        {
        ClusteredRemoteSemaphoreIT.SemaphoreEventListener listener1 = new ClusteredRemoteSemaphoreIT.SemaphoreEventListener(sSemaphoreName);
        ClusteredRemoteSemaphoreIT.SemaphoreEventListener listener2 = new ClusteredRemoteSemaphoreIT.SemaphoreEventListener(sSemaphoreName);

        try
            {
            // Add the listeners to listen for semaphore events
            member1.addListener(listener1);
            member2.addListener(listener2);

            Base.sleep(1000);

            // Acquire the permit on first member (the permit will be held for 5 minutes)
            member1.submit(new ClusteredRemoteSemaphoreIT.AcquirePermit(sSemaphoreName, 1, Duration.ofMinutes(5)));

            // wait for the permit acquired event
            listener1.awaitAcquired(Duration.ofMinutes(1));

            // Acquire the permit on second member (the permit will be held for 5 seconds)
            member2.submit(new ClusteredRemoteSemaphoreIT.AcquirePermit(sSemaphoreName, 1, Duration.ofSeconds(5)));

            // Kill first member
            RemoteCallable<Void> closeAll = new RemoteCallableStaticMethod<>("com.tangosol.net.Coherence", "closeAll");
            member1.submit(closeAll);

            // wait for the permit acquired event from the second member
            listener2.awaitAcquired(Duration.ofMinutes(1));
            listener2.awaitReleased(Duration.ofMinutes(1));
            }
        finally
            {
            member1.removeListener(listener1);
            member2.removeListener(listener2);
            }
        }

    @Test
    void shouldAcquireAndReleasePermitInOrderFromMultipleStorageMembers() throws Exception
        {
        // Get storage members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");

        shouldAcquireAndReleasePermitInOrderFromMultipleMembers(member1, member2, "foo-shouldAcquireAndReleasePermitInOrderFromMultipleStorageMembers");
        }

    @Test
    void shouldAcquireAndReleasePermitInOrderFromMultipleStorageDisabledMembers() throws Exception
        {
        // Get storage disabled application members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("application-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("application-2");

        shouldAcquireAndReleasePermitInOrderFromMultipleMembers(member1, member2, "foo-shouldAcquireAndReleasePermitInOrderFromMultipleStorageDisabledMembers");
        }

    /**
     * This test acquires the same permit from multiple members.
     * The first member should acquire the permit and the second member should block until the
     * first has released the permit.
     *
     * @param member1  the first member to acquire the permit
     * @param member2  the second member to acquire the permit
     *
     * @throws Exception if the test fails
     */
    void shouldAcquireAndReleasePermitInOrderFromMultipleMembers(CoherenceClusterMember member1, CoherenceClusterMember member2, String sSemaphoreName) throws Exception
        {
        ClusteredRemoteSemaphoreIT.SemaphoreEventListener listener1 = new ClusteredRemoteSemaphoreIT.SemaphoreEventListener(sSemaphoreName);
        ClusteredRemoteSemaphoreIT.SemaphoreEventListener listener2 = new ClusteredRemoteSemaphoreIT.SemaphoreEventListener(sSemaphoreName);

        try
            {
            // Add the listener to listen for semaphore events from the first member.
            member1.addListener(listener1);
            member2.addListener(listener2);

            // Acquire the permit on first member (the permit will be held for 5 seconds)
            member1.submit(new ClusteredRemoteSemaphoreIT.AcquirePermit(sSemaphoreName, 1, Duration.ofSeconds(5)));
            // wait for the permit acquired event
            listener1.awaitAcquired(Duration.ofMinutes(1));

            // Try to acquire the permit on second member (should fail)
            assertThat(member2.invoke(new ClusteredRemoteSemaphoreIT.TryAcquire(sSemaphoreName, 1)), is(false));

            // Acquire the permit on the second member, should block until the first member releases
            member2.submit(new ClusteredRemoteSemaphoreIT.AcquirePermit(sSemaphoreName, 1, Duration.ofSeconds(1)));

            // wait for the second member to acquire the permit (should be after member 1 releases the permit)
            listener2.awaitAcquired(Duration.ofMinutes(1));
            // wait for the second member to release the permit
            listener2.awaitReleased(Duration.ofMinutes(1));

            // Assert the permits were acquired and released in the order expected
            System.out.println("Acquired #1: " + listener1.getAcquiredOrder());
            System.out.println("Released #1: " + listener1.getReleasedOrder());
            System.out.println("Acquired #2: " + listener2.getAcquiredOrder());
            System.out.println("Released #2: " + listener2.getReleasedOrder());
            assertThat(listener1.getAcquiredOrder(), lessThan(listener1.getReleasedOrder()));
            assertThat(listener1.getReleasedOrder(), lessThan(listener2.getAcquiredOrder()));
            assertThat(listener2.getAcquiredOrder(), lessThan(listener2.getReleasedOrder()));
            }
        finally
            {
            member1.removeListener(listener1);
            member2.removeListener(listener2);
            }
        }

    @Test
    void shouldNotAcquireOnZeroPermitsOnStorageMember()
        {
        // Get storage members from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("storage-1");

        shouldNotAcquireOnZeroPermits(member, "foo-shouldNotAcquireOnZeroPermitsOnStorageMember");
        }

    @Test
    void shouldNotAcquireOnZeroPermitsOnStorageDisabledMember()
        {
        // Get storage disabled application members from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("application-2");

        shouldNotAcquireOnZeroPermits(member, "foo-shouldNotAcquireOnZeroPermitsOnStorageDisabledMember");
        }

    void shouldNotAcquireOnZeroPermits(CoherenceClusterMember member, String sSemaphoreName)
        {
        // Acquire the semaphore on first member
        assertThat(member.invoke(new ClusteredRemoteSemaphoreIT.TryAcquire(sSemaphoreName, 0, Duration.ZERO)), is(false));
        assertThat(member.invoke(() -> Semaphores.remoteSemaphore(sSemaphoreName, 0).availablePermits()), is(0));
        }

    @Test
    void shouldNotAcquireManyOnStorageMembers() throws Exception
        {
        // Get storage members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");

        shouldNotAcquireMany(member1, member2, "foo-shouldNotAcquireManyOnStorageMembers");
        }

    @Test
    void shouldNotAcquireManyOnStorageDisabledMembers() throws Exception
        {
        // Get storage disabled application members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("application-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("application-2");

        shouldNotAcquireMany(member1, member2, "foo-shouldNotAcquireManyOnStorageDisabledMembers");
        }

    void shouldNotAcquireMany(CoherenceClusterMember member1, CoherenceClusterMember member2, String sSemaphoreName)
            throws Exception
        {
        ClusteredRemoteSemaphoreIT.SemaphoreEventListener listener1 = new ClusteredRemoteSemaphoreIT.SemaphoreEventListener(sSemaphoreName);
        ClusteredRemoteSemaphoreIT.SemaphoreEventListener listener2 = new ClusteredRemoteSemaphoreIT.SemaphoreEventListener(sSemaphoreName);

        try
            {
            // Add the listener to listen for semaphore events from the first member.
            member1.addListener(listener1);
            member2.addListener(listener2);

            member1.submit(new ClusteredRemoteSemaphoreIT.AcquirePermit(sSemaphoreName, 10, 7, Duration.ofSeconds(30)));
            listener1.awaitAcquired(Duration.ofMinutes(1));
            assertThat(member1.invoke(() -> Semaphores.remoteSemaphore(sSemaphoreName, 10).availablePermits()), is(3));

            // Try to acquire five permits (should fail)
            assertThat(member2.invoke(new ClusteredRemoteSemaphoreIT.TryAcquire(sSemaphoreName, 10, 5, Duration.ZERO)), is(false));
            assertThat(member2.invoke(() -> Semaphores.remoteSemaphore(sSemaphoreName, 10).availablePermits()), is(3));
            }
        finally
            {
            member1.removeListener(listener1);
            member2.removeListener(listener2);
            }
        }

    @Test
    void shouldReleaseAndAcquireOnNegativeNumberOfPermitsOnStorageMembers()
        {
        // Get storage members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");

        shouldReleaseAndAcquireOnNegativeNumberOfPermits(member1, member2, "foo-shouldReleaseAndAcquireOnNegativeNumberOfPermitsOnStorageMembers");
        }

    @Test
    void shouldReleaseAndAcquireOnNegativeNumberOfPermitsOnStorageDisabledMembers()
        {
        // Get storage disabled application members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("application-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("application-2");

        shouldReleaseAndAcquireOnNegativeNumberOfPermits(member1, member2, "foo-shouldReleaseAndAcquireOnNegativeNumberOfPermitsOnStorageDisabledMembers");
        }

    void shouldReleaseAndAcquireOnNegativeNumberOfPermits(CoherenceClusterMember member1, CoherenceClusterMember member2, String sSemaphoreName)
        {
        // Try to acquire single permit (should fail)
        assertThat(member2.invoke(new ClusteredRemoteSemaphoreIT.TryAcquire(sSemaphoreName, -3, Duration.ZERO)), is(false));

        // Release 4 permits
        member1.invoke(() -> {Semaphores.remoteSemaphore(sSemaphoreName, -3).release(4); return null;});
        assertThat(member2.invoke(() -> Semaphores.remoteSemaphore(sSemaphoreName, -3).availablePermits()), is(1));

        // Try to acquire permit
        assertThat(member2.invoke(new ClusteredRemoteSemaphoreIT.TryAcquire(sSemaphoreName, -3, Duration.ZERO)), is(true));
        }

    // ----- inner class: TryAcquire-----------------------------------------

    /**
     * A Bedrock remote callable that tries to acquire a permit within a given timeout.
     * <p>
     * The result of the call to {@link RemoteSemaphore#tryAcquire()}} is returned.
     * If the permit was acquired it is immediately released.
     */
    static class TryAcquire
            implements RemoteCallable<Boolean>
        {
        /**
         * A remote channel injected by Bedrock and used to fire events back to the test.
         */
        @RemoteChannel.Inject
        private RemoteChannel remoteChannel;

        /**
         * The name of the semaphore to acquire.
         */
        private final String f_sSemaphoreName;

        /**
         * The amount of time to wait to acquire the semaphore.
         */
        private final Duration f_timeout;

        /**
         * Initial number of semaphore permits.
         */
        private final int f_permits;

        /**
         * Number of permits to acquire.
         */
        private final int f_acquirePermits;

        /**
         * Create a {@link ClusteredRemoteSemaphoreIT.TryAcquire} callable.
         *
         * @param sSemaphoreName  the name of the semaphore to acquire
         * @param nPermits initial number of semaphore permits
         * */
        public TryAcquire(String sSemaphoreName, int nPermits)
            {
            f_sSemaphoreName = sSemaphoreName;
            f_permits        = nPermits;
            f_timeout        = Duration.ZERO;
            f_acquirePermits = 1;
            }

        /**
         * Create a {@link ClusteredRemoteSemaphoreIT.TryAcquire} callable.
         *
         * @param sSemaphoreName  the name of the semaphore to acquire
         * @param nPermits        initial number of semaphore permits
         * @param duration        the amount of time to wait to acquire the permit
         */
        public TryAcquire(String sSemaphoreName, int nPermits, Duration duration)
            {
            f_sSemaphoreName = sSemaphoreName;
            f_timeout   = duration;
            f_permits   = nPermits;
            f_acquirePermits = 1;
            }

        /**
         * Create a {@link ClusteredRemoteSemaphoreIT.TryAcquire} callable.
         *
         * @param sSemaphoreName  the name of the semaphore to acquire
         * @param nPermits        initial number of semaphore permits
         * @param nAcquirePermits number of permits to acquire
         * @param duration        the amount of time to wait to acquire the permit
         */
        public TryAcquire(String sSemaphoreName, int nPermits, int nAcquirePermits, Duration duration)
            {
            f_sSemaphoreName = sSemaphoreName;
            f_timeout   = duration;
            f_permits   = nPermits;
            f_acquirePermits = nAcquirePermits;
            }

        @Override
        public Boolean call() throws Exception
            {
            RemoteSemaphore distributedSemaphore = Semaphores.remoteSemaphore(f_sSemaphoreName, f_permits);

            boolean fAcquired;
            if (f_timeout.isZero())
                {
                Logger.info("Trying to acquire a permit from semaphore " + f_sSemaphoreName + " with zero timeout");
                fAcquired = distributedSemaphore.tryAcquire(f_acquirePermits);
                }
            else
                {
                Logger.info("Trying to acquire a permit from semaphore " + f_sSemaphoreName + " with timeout of " + f_timeout);
                fAcquired = distributedSemaphore.tryAcquire(f_acquirePermits, f_timeout.toMillis(), MILLISECONDS);
                }

            if (fAcquired)
                {
                remoteChannel.raise(new ClusteredRemoteSemaphoreIT.SemaphoreEvent(f_sSemaphoreName, ClusteredRemoteSemaphoreIT.SemaphoreEventType.Acquired));
                Logger.info("Tried and succeeded to acquire a permit from semaphore " + f_sSemaphoreName + " within timeout " + f_timeout);
                remoteChannel.raise(new ClusteredRemoteSemaphoreIT.SemaphoreEvent(f_sSemaphoreName, ClusteredRemoteSemaphoreIT.SemaphoreEventType.Released));
                distributedSemaphore.release();
                }
            else
                {
                Logger.info("Tried and failed to acquire a permit from semaphore " + f_sSemaphoreName + " within timeout " + f_timeout);
                }

            return fAcquired;
            }
        }

    // ----- inner class: AcquirePermit -------------------------------------

    /**
     * A Bedrock remote callable that acquires a permit for a specific amount of time.
     * <p>
     * This callable fires remote events to indicate when the permit was acquired and released.
     */
    static class AcquirePermit
            implements RemoteCallable<Void>
        {
        /**
         * A remote channel injected by Bedrock and used to fire events back to the test.
         */
        @RemoteChannel.Inject
        private RemoteChannel remoteChannel;

        /**
         * The name of the semaphore to acquire.
         */
        private final String f_sSemaphoreName;

        /**
         * The duration to hold the permit for.
         */
        private final Duration f_duration;

        /**
         * Initial number of semaphore permits.
         */
        private final int f_permits;

        /**
         * Number of permits to acquire.
         */
        private final int f_acquirePermits;

        /**
         * Create an {@link ClusteredRemoteSemaphoreIT.AcquirePermit} callable.
         *
         * @param sSemaphoreName  the name of the semaphore to acquire
         * @param permits         initial number of semaphore permits
         * @param duration        the duration to hold the permit for
         */
        AcquirePermit(String sSemaphoreName, int permits, Duration duration)
            {
            this(sSemaphoreName, permits, 1, duration);
            }

        /**
         * Create an {@link ClusteredRemoteSemaphoreIT.AcquirePermit} callable.
         *
         * @param sSemaphoreName  the name of the semaphore to acquire
         * @param permits         initial number of semaphore permits
         * @param acquirePermits  number of permits to acquire
         * @param duration        the duration to hold the permit for
         */
        AcquirePermit(String sSemaphoreName, int permits, int acquirePermits, Duration duration)
            {
            f_sSemaphoreName = sSemaphoreName;
            f_permits        = permits;
            f_acquirePermits = acquirePermits;
            f_duration       = duration;
            }

        @Override
        public Void call()
            {
            Logger.info("Acquiring a permit from semaphore " + f_sSemaphoreName);
            RemoteSemaphore semaphore = Semaphores.remoteSemaphore(f_sSemaphoreName, f_permits);
            if (f_acquirePermits == 1)
                {
                semaphore.acquireUninterruptibly();
                }
            else
                {
                semaphore.acquireUninterruptibly(f_acquirePermits);
                }
            try
                {
                remoteChannel.raise(new ClusteredRemoteSemaphoreIT.SemaphoreEvent(f_sSemaphoreName, ClusteredRemoteSemaphoreIT.SemaphoreEventType.Acquired));
                Logger.info("Semaphore " + f_sSemaphoreName + " permit acquired");
                Thread.sleep(f_duration.toMillis());
                }
            catch (InterruptedException ignore)
                {
                }
            finally
                {
                remoteChannel.raise(new ClusteredRemoteSemaphoreIT.SemaphoreEvent(f_sSemaphoreName, ClusteredRemoteSemaphoreIT.SemaphoreEventType.Released));
                if (f_acquirePermits == 1)
                    {
                    semaphore.release();
                    }
                else
                    {
                    semaphore.release(f_acquirePermits);
                    }
                Logger.info("Semaphore " + f_sSemaphoreName + " permit released");
                }
            return null;
            }
        }

    // ----- inner class: SemaphoreEvent-------------------------------------

    /**
     * A Bedrock remote event submitted by the {@link ClusteredRemoteSemaphoreIT.AcquirePermit} callable
     * to notify the calling test when the permit has been acquired and released.
     */
    static class SemaphoreEvent
            implements RemoteEvent
        {
        /**
         * The name of the semaphore.
         */
        private final String f_sSemaphoreName;

        /**
         * The type of the event.
         */
        private final ClusteredRemoteSemaphoreIT.SemaphoreEventType f_type;

        /**
         * The global order of the event.
         */
        private final int f_order;

        /**
         * Create a semaphore event.
         *
         * @param sSemaphoreName  the name of the semaphore
         * @param type            the type of the event
         */
        public SemaphoreEvent(String sSemaphoreName, ClusteredRemoteSemaphoreIT.SemaphoreEventType type)
            {
            f_sSemaphoreName = sSemaphoreName;
            f_type           = type;
            f_order          = Atomics.remoteAtomicInteger("ClusteredDistributedSemaphoreIT.eventCounter").incrementAndGet();
            }

        /**
         * Returns the name of the semaphore.
         *
         * @return  the name of the semaphore
         */
        public String getSemaphoreName()
            {
            return f_sSemaphoreName;
            }

        /**
         * Returns the event type.
         *
         * @return  the event type
         */
        public ClusteredRemoteSemaphoreIT.SemaphoreEventType getEventType()
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

        public String toString()
            {
            return "SemaphoreEvent{" +
                   "f_sSemaphoreName='" + f_sSemaphoreName + '\'' +
                   ", f_type=" + f_type +
                   ", f_order=" + f_order +
                   '}';
            }
        }

    // ----- inner class SemaphoreEventListener -----------------------------

    /**
     * A {@link RemoteEventListener} that listens for {@link ClusteredRemoteSemaphoreIT.SemaphoreEvent semaphore events}.
     */
    static class SemaphoreEventListener
            implements RemoteEventListener
        {
        /**
         * The name of the semaphore.
         */
        private final String f_sSemaphoreName;

        /**
         * A future that completes when the permit acquired event is received.
         */
        private final CompletableFuture<Integer> f_futureAcquired = new CompletableFuture<>();

        /**
         * A future that completes when the permit released event is received.
         */
        private final CompletableFuture<Integer> f_futureReleased = new CompletableFuture<>();

        /**
         * Create a {@link ClusteredRemoteSemaphoreIT.SemaphoreEventListener}.
         *
         * @param sSemaphoreName  the name of the permit
         */
        public SemaphoreEventListener(String sSemaphoreName)
            {
            f_sSemaphoreName = sSemaphoreName;
            }

        @Override
        public void onEvent(RemoteEvent event)
            {
            if (event instanceof ClusteredRemoteSemaphoreIT.SemaphoreEvent)
                {
                ClusteredRemoteSemaphoreIT.SemaphoreEvent e = (ClusteredRemoteSemaphoreIT.SemaphoreEvent) event;
                if (f_sSemaphoreName.equals(e.getSemaphoreName()))
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
         * Wait for the permit acquired event.
         *
         * @param timeout the maximum amount of time to wait
         *
         * @return the global order of the permit acquired event
         */
        public int awaitAcquired(Duration timeout) throws Exception
            {
            return f_futureAcquired.get(timeout.toMillis(), MILLISECONDS);
            }

        /**
         * Returns true if the permit has been acquired.
         *
         * @return true if the permit has been acquired
         */
        public boolean isAcquired()
            {
            return f_futureAcquired.isDone();
            }

        /**
         * Returns the global order of the permit acquired event.
         *
         * @return the global order of the permit acquired event
         */
        public int getAcquiredOrder()
            {
            return f_futureAcquired.join();
            }

        /**
         * Wait for the permit released event.
         *
         * @param timeout  the maximum amount of time to wait
         *
         * @return the global order of the permit released event
         */
        public int awaitReleased(Duration timeout) throws Exception
            {
            return f_futureReleased.get(timeout.toMillis(), MILLISECONDS);
            }

        /**
         * Returns true if the permit has been acquired and released.
         *
         * @return true if the permit has been acquired and released
         */
        public boolean isReleased()
            {
            return f_futureAcquired.isDone() && f_futureReleased.isDone();
            }

        /**
         * Returns the global order of the permit released event.
         *
         * @return the global order of the permit released event
         */
        public int getReleasedOrder()
            {
            return f_futureReleased.join();
            }
        }

    // ----- inner enum PermitEventType -------------------------------------

    /**
     * An enum of lock event types.
     */
    enum SemaphoreEventType {Acquired, Released}

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
