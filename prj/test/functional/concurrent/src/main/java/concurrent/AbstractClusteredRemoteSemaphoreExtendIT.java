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
import com.oracle.bedrock.runtime.concurrent.callable.RemoteCallableStaticMethod;
import com.oracle.bedrock.testsupport.junit.AbstractTestLogs;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.concurrent.RemoteSemaphore;
import com.oracle.coherence.concurrent.Semaphores;

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
 * Test distributed semaphores across multiple cluster members.
 * <p>
 * This class must be Serializable so that its methods can be used as
 * remote callables by Bedrock.
 */
public abstract class AbstractClusteredRemoteSemaphoreExtendIT
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    public AbstractClusteredRemoteSemaphoreExtendIT(CoherenceClusterExtension coherenceResource)
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
    public void shouldAcquireAndReleasePermitOnExtendClientMember()
        {
        // Get a extend client member from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("client-1");
        // Run the "shouldAcquireAndReleasePermits" method on the extend client member
        // If any assertions fail this method will throw an exception
        member.invoke(() ->
                          {
                          this.shouldAcquireAndReleasePermits("foo-shouldAcquireAndReleasePermitOnExtendClientMember");
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
    public void shouldAcquireAndReleaseMultiplePermitsOnExtendClientMember()
        {
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("client-1");
        member.invoke(() ->
                          {
                          this.shouldAcquireAndReleaseMultiplePermits("foo-shouldAcquireAndReleaseMultiplePermitsOnExtendClientMember");
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
    void shouldTimeOutIfThePermitIsHeldByAnotherMemberUsingExtendClientMembers() throws Exception
        {
        // Get extend clients members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

        shouldTimeOutIfThePermitIsHeldByAnotherMember(member1, member2, "foo-shouldTimeOutIfThePermitIsHeldByAnotherMemberUsingExtendClientMembers");
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
    void shouldAcquirePermitHeldByFailedExtendClientMember() throws Exception
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-3");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

        shouldAcquirePermitHeldByFailedMember(member1, member2, "foo-shouldAcquirePermitHeldByFailedExtendClientMember");
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
            RemoteCallable<Void> exit = new RemoteCallableStaticMethod<>("java.lang.System", "exit", 1);
            member1.submit(exit);

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
    void shouldAcquireAndReleasePermitInOrderFromMultipleExtendClientMembers() throws Exception
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

        shouldAcquireAndReleasePermitInOrderFromMultipleMembers(member1, member2, "foo-shouldAcquireAndReleasePermitInOrderFromMultipleExtendClientMembers");
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
    void shouldNotAcquireOnZeroPermitsOnExtendClientMember()
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("client-2");

        shouldNotAcquireOnZeroPermits(member, "foo-shouldNotAcquireOnZeroPermitsOnExtendClientMember");
        }

    void shouldNotAcquireOnZeroPermits(CoherenceClusterMember member, String sSemaphoreName)
        {
        // Acquire the semaphore on first member
        assertThat(member.invoke(new ClusteredRemoteSemaphoreIT.TryAcquire(sSemaphoreName, 0, Duration.ZERO)), is(false));
        assertThat(member.invoke(() -> Semaphores.remoteSemaphore(sSemaphoreName, 0).availablePermits()), is(0));
        }

    @Test
    void shouldNotAcquireManyOnExtendClientMembers() throws Exception
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

        shouldNotAcquireMany(member1, member2, "foo-shouldNotAcquireManyOnExtendClientMembers");
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
    void shouldReleaseAndAcquireOnNegativeNumberOfPermitsOnExtendClientMembers()
        {
        // Get storage disabled application members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

        shouldReleaseAndAcquireOnNegativeNumberOfPermits(member1, member2, "foo-shouldReleaseAndAcquireOnNegativeNumberOfPermitsOnExtendClientMembers");
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

    // ----- data members ---------------------------------------------------

    /**
     * A Bedrock JUnit5 extension with a Coherence cluster for the tests.
     */
    static CoherenceClusterExtension m_coherenceResource;

    /**
     * System property to enable the Concurrent service proxy.
     */
    protected static final String EXTEND_ENABLED_PROPERTY = "coherence.concurrent.extend.enabled";

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
