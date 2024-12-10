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

import static concurrent.locks.AbstractClusteredRemoteReadWriteLockIT.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractClusteredRemoteReadWriteLockExtendProxyIT
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    AbstractClusteredRemoteReadWriteLockExtendProxyIT(CoherenceClusterExtension coherenceResource)
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
    void shouldNotAcquireLockHeldByExtendClientThroughFailedProxy() throws Exception
        {
        // Get extend clients from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("client-3");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("client-2");

        shouldNotAcquireLockHeldByExtendClientThroughFailedProxy(member1, member2);
        }

    /**
     * This test checks that a lock held by a member connected via failed proxy
     * is not automatically released, and is not subsequently acquired by another member.
     *
     * @param member1  the first member to acquire the lock on and then kill its proxy
     * @param member2  the second member to try to acquire the lock on
     *
     * @throws Exception if the test fails
     */
    void shouldNotAcquireLockHeldByExtendClientThroughFailedProxy(CoherenceClusterMember member1, CoherenceClusterMember member2) throws Exception
        {
        LockEventListener foo1  = new LockEventListener("foo");
        LockEventListener foo2  = new LockEventListener("foo");
        LockEventListener bar1  = new LockEventListener("bar");
        LockEventListener bar2  = new LockEventListener("bar");

        // Add the listeners to listen for lock events from the first member.
        member1.addListener(foo1);
        member1.addListener(bar1);
        // Add the listeners to listen for lock events from the second member.
        member2.addListener(foo2);
        member2.addListener(bar2);

        Base.sleep(1000);

        // Acquire read and write lock on first member (the lock will be held for 40 seconds)
        member1.submit(new AcquireWriteLock("foo", Duration.ofSeconds(40)));
        member1.submit(new AcquireReadLock("bar", Duration.ofSeconds(40)));

        // Acquire read lock on second member
        member2.submit(new AcquireReadLock("bar", Duration.ofSeconds(40)));

        // wait for write and read lock acquired event
        foo1.awaitWriteAcquired(Duration.ofSeconds(20));
        bar1.awaitReadAcquired(Duration.ofSeconds(20));
        bar2.awaitReadAcquired(Duration.ofSeconds(20));

        // Find proxy that client is connected to
        CoherenceClusterMember proxy = m_coherenceResource.getCluster().stream()
                .filter(m -> m.getRoleName().equals("proxy"))
                .filter(m -> m.hasExtendConnection(CONCURRENT_PROXY_SERVICE_NAME, member1.getLocalMemberUUID()))
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
                                      }, instanceOf(Exception.class));

        // Try for 10 seconds to acquire write lock on second member (should fail)
        assertThat(member2.invoke(new TryWriteLock("foo", Duration.ofSeconds(10))), is(false));
        assertThat(member2.invoke(new TryReadLock("bar", Duration.ofSeconds(10))), is(true));

        foo1.awaitWriteReleased(Duration.ofSeconds(40));
        bar1.awaitReadReleased(Duration.ofSeconds(40));
        }

    // ----- data members ---------------------------------------------------

    /**
     * System property to enable the Concurrent service proxy.
     */
    protected static final String EXTEND_ENABLED_PROPERTY = "coherence.concurrent.extend.enabled";

    /**
     * Proxy service name.
     */
    protected static final String CONCURRENT_PROXY_SERVICE_NAME = "$SYS:ConcurrentProxy";

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
