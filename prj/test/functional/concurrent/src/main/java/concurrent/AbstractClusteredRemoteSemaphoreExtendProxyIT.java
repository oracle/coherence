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
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.AbstractTestLogs;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.util.Base;

import java.io.Serializable;

import java.util.concurrent.TimeUnit;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test distributed semaphores across multiple cluster members.
 * <p>
 * This class must be Serializable so that its methods can be used as
 * remote callables by Bedrock.
 */
public abstract class AbstractClusteredRemoteSemaphoreExtendProxyIT
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    public AbstractClusteredRemoteSemaphoreExtendProxyIT(CoherenceClusterExtension coherenceResource)
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
    void shouldNotAcquirePermitHeldByExtendClientThroughFailedProxy() throws Exception
        {
        // Get extend client members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

        shouldNotAcquirePermitHeldByExtendClientThroughFailedProxy(member1, member2, "foo-shouldAcquirePermitHeldByExtendClientThroughFailedProxy");
        }

    /**
     * This test checks that a permit held by a member connected via failed proxy
     * is not automatically released, and is not subsequently acquired by another member.
     *
     * @param member1  the first member to acquire the permit on and then kill its proxy
     * @param member2  the second member to try to acquire the permit on
     *
     * @throws Exception if the test fails
     */
    void shouldNotAcquirePermitHeldByExtendClientThroughFailedProxy(CoherenceClusterMember member1, CoherenceClusterMember member2, String sSemaphoreName) throws Exception
        {
        ClusteredRemoteSemaphoreIT.SemaphoreEventListener listener1 = new ClusteredRemoteSemaphoreIT.SemaphoreEventListener(sSemaphoreName);
        ClusteredRemoteSemaphoreIT.SemaphoreEventListener listener2 = new ClusteredRemoteSemaphoreIT.SemaphoreEventListener(sSemaphoreName);

        try
            {
            // Add the listeners to listen for semaphore events
            member1.addListener(listener1);
            member2.addListener(listener2);

            Base.sleep(1000);

            // Acquire the permit on first member (the permit will be held for 40 seconds)
            member1.submit(new ClusteredRemoteSemaphoreIT.AcquirePermit(sSemaphoreName, 1, Duration.ofSeconds(40)));

            // wait for the permit acquired event
            listener1.awaitAcquired(Duration.ofSeconds(20));

            killProxyForMember(member1);

            // Try for 10 seconds to acquire the permit on second member (should fail)
            assertThat(member2.invoke(new ClusteredRemoteSemaphoreIT.TryAcquire(sSemaphoreName, 1, Duration.ofSeconds(10))), is(false));

            // wait for the permit release event from the first member
            listener1.awaitReleased(Duration.ofSeconds(40));
            }
        finally
            {
            member1.removeListener(listener1);
            member2.removeListener(listener2);
            }
        }

    private CoherenceClusterMember killProxyForMember(CoherenceClusterMember member)
        {
        // Find proxy that client is connected to
        CoherenceClusterMember proxy = m_coherenceResource.getCluster().stream()
                .filter(m -> m.getRoleName().equals("proxy"))
                .filter(m -> m.hasExtendConnection(CONCURRENT_PROXY_SERVICE_NAME, member.getLocalMemberUUID()))
                .findFirst()
                .orElseThrow();

        // Kill proxy
        RemoteCallable<Void> exit = new RemoteCallableStaticMethod<>("java.lang.System", "exit", 1);
        proxy.submit(exit);
        Eventually.assertDeferred(() ->
                                      {
                                      try
                                          {
                                          proxy.isReady();
                                          }
                                      catch (Exception e)
                                          {
                                          return e;
                                          }
                                      return null;
                                      }, instanceOf(IllegalStateException.class), Eventually.within(30, TimeUnit.SECONDS));
        return proxy;
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
     * Proxy service name.
     */
    protected static final String CONCURRENT_PROXY_SERVICE_NAME = "$SYS:ConcurrentProxy";

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
