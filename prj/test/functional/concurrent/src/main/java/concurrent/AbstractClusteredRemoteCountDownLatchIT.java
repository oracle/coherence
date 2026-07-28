/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
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
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.AbstractTestLogs;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Randoms;

import com.oracle.coherence.concurrent.Latches;
import com.oracle.coherence.concurrent.RemoteCountDownLatch;

import com.tangosol.util.Base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.Serializable;

import java.time.Duration;
import java.time.Instant;

import java.util.Random;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test distributed countdown latch across multiple cluster members.
 * <p>
 * This class must be Serializable so that its methods can be used as
 * remote callables by Bedrock.
 *
 * @since 21.12
 *
 * @author lh
 */
public abstract class AbstractClusteredRemoteCountDownLatchIT
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    AbstractClusteredRemoteCountDownLatchIT(CoherenceClusterExtension coherenceResource)
        {
        m_coherenceResource = coherenceResource;
        }

    @BeforeEach
    void beforeEach(TestInfo info)
        {
        m_sTestName = getClass().getSimpleName() + "-"
                + info.getTestMethod().map(method -> method.getName()).orElse(info.getDisplayName());

        // print a message in the logs of all the cluster member to indicate the name of the test
        // that is about to start, this make debugging the logs simpler.
        String sMessage = ">>>>> Starting test method " + info.getDisplayName();
        logOnEachMember(sMessage);
        }

    @AfterEach
    void after(TestInfo info)
        {
        // print a message in the logs of all the cluster member to indicate the name of the test
        // that has just finished, this make debugging the logs simpler.
        String sMessage = "<<<<< Completed test method " + info.getDisplayName();
        try
            {
            logOnEachMember(sMessage);
            }
        finally
            {
            clearLatchesOnEachMember();
            }
        }

    private void logOnEachMember(String sMessage)
        {
        m_coherenceResource.getCluster()
                .forEach(member ->
                             {
                             try
                                 {
                                 member.invoke(() ->
                                                   {
                                                   Logger.info(sMessage);
                                                   return null;
                                                   });
                                 }
                             catch (Throwable ignore)
                                 {
                                 // ignoring "RemoteChannel is closed" exception from members that were shut down
                                 }
                             });
        }

    private void clearLatchesOnEachMember()
        {
        m_coherenceResource.getCluster()
                .forEach(member ->
                             {
                             try
                                 {
                                 member.invoke(() ->
                                                   {
                                                   ConcurrentHelper.clearLatches();
                                                   return null;
                                                   });
                                 }
                             catch (Throwable ignore)
                                 {
                                 // ignoring "RemoteChannel is closed" exception from members that were shut down
                                 }
                             });
        }

    @Test
    public void shouldAcquireAndCountDownOnStorageMember()
        {
        // Get a storage member from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("storage-1");
        String                 sName  = latchName("storage");
        // Run the "shouldAcquireAndCountDown" method on the storage member
        // If any assertions fail this method will throw an exception
        member.invoke(() -> shouldAcquireAndCountDown(sName));
        }

    @Test
    public void shouldAcquireAndCountDownOnStorageDisabledMember()
        {
        // Get a storage disabled application member from the cluster
        CoherenceClusterMember member = m_coherenceResource.getCluster().get("application-1");
        String                 sName  = latchName("application");
        // Run the "shouldAcquireAndCountDown" method on the storage disabled member
        // If any assertions fail this method will throw an exception
        member.invoke(() -> shouldAcquireAndCountDown(sName));
        }

    @Test
    void shouldCountDownIfTheLatchIsCreatedByAnotherMemberUsingStorageMembers() throws Exception
        {
        // Get storage members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");

        shouldCountDownIfTheLatchIsCreatedByAnotherMember(member1, member2, latchName("storage-members"));
        }

    @Test
    void shouldCountDownIfTheLatchIsCreatedByAnotherMemberUsingStorageDisabledMembers() throws Exception
        {
        // Get storage disabled members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("application-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("application-2");

        shouldCountDownIfTheLatchIsCreatedByAnotherMember(member1, member2, latchName("storage-disabled-members"));
        }

    /**
     * This test acquires and counts down latch on multiple cluster members,
     * storage enabled and disabled.
     *
     * @throws Exception if the test fails
     */
    @Test
    void shouldAcquireAndCountDownFromMultipleStorageMembers() throws Exception
        {
        // get all members from the cluster
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");
        CoherenceClusterMember member3 = m_coherenceResource.getCluster().get("application-1");
        CoherenceClusterMember member4 = m_coherenceResource.getCluster().get("application-2");

        String                                                  sName     = latchName("multiple-members");
        AbstractClusteredRemoteCountDownLatchIT.LatchEventListener listener1 = new AbstractClusteredRemoteCountDownLatchIT.LatchEventListener(sName);
        AbstractClusteredRemoteCountDownLatchIT.LatchEventListener listener2 = new AbstractClusteredRemoteCountDownLatchIT.LatchEventListener(sName);

        // add the listener to listen for latch events on member1 and member2
        member1.addListener(listener1);
        member2.addListener(listener2);

        // Acquire the latch on first member
        int                        count         = 4;
        Random                     random        = Randoms.getRandom();
        int                        limit         = 5;
        CompletableFuture<Boolean> futureAcquire = member1.submit(new AbstractClusteredRemoteCountDownLatchIT.AcquireLatch(sName, count));
        listener1.awaitAcquired(Duration.ofSeconds(20));

        assertRemoteCount(member1, sName, count, count);

        AbstractClusteredRemoteCountDownLatchIT.CountDown countDown       = new AbstractClusteredRemoteCountDownLatchIT.CountDown(sName, count, Duration.ofSeconds(random.nextInt(limit)));
        CompletableFuture<Void>                        futureCountDown = member1.submit(countDown);
        listener1.awaitCountedDown(Duration.ofSeconds(20));
        assertThat(futureCountDown.get(), nullValue());

        try
            {
            countDown = new AbstractClusteredRemoteCountDownLatchIT.CountDown(sName, count + 1, Duration.ofSeconds(1));
            member2.submit(countDown);
            listener2.awaitCountedDown(Duration.ofSeconds(10));
            fail("Should get TimeoutException.");
            }
        catch (TimeoutException e)
            {}

        assertRemoteCount(member4, sName, count, 3);

        countDown       = new AbstractClusteredRemoteCountDownLatchIT.CountDown(sName, count, Duration.ofSeconds(random.nextInt(limit)));
        futureCountDown = member2.submit(countDown);
        listener2.awaitCountedDown(Duration.ofSeconds(20));
        assertThat(futureCountDown.get(), nullValue());

        assertRemoteCount(member3, sName, count, 2);

        countDown = new AbstractClusteredRemoteCountDownLatchIT.CountDown(sName, count, Duration.ofSeconds(random.nextInt(limit)));
        member3.submit(countDown);

        try
            {
            futureAcquire.get(2, TimeUnit.SECONDS);
            fail("Should time out.");
            }
        catch (TimeoutException e)
            {}

        countDown       = new AbstractClusteredRemoteCountDownLatchIT.CountDown(sName, count, Duration.ofSeconds(random.nextInt(limit)));
        futureCountDown = member4.submit(countDown);

        assertThat(futureCountDown.get(), nullValue());
        assertThat(futureAcquire.get(), is(true));

        GetLatchsMapSize getSize = new GetLatchsMapSize();
        assertThat(member1.submit(getSize).get().intValue(), is(0));
        assertThat(member2.submit(getSize).get().intValue(), is(0));
        assertThat(member3.submit(getSize).get().intValue(), is(0));
        assertThat(member4.submit(getSize).get().intValue(), is(0));
        }

    /**
     * This test acquires and countdown latch on multiple cluster memebers
     * with rolling restart.
     *
     * @throws Exception if the test fails
     */
    @Test
    void shouldAcquireAndCountDownWithRollingRestart() throws Exception
        {
        CoherenceClusterMember member1 = m_coherenceResource.getCluster().get("storage-1");
        CoherenceClusterMember member2 = m_coherenceResource.getCluster().get("storage-2");

        String                                                  sName     = latchName("rolling-restart");
        AbstractClusteredRemoteCountDownLatchIT.LatchEventListener listener1 = new AbstractClusteredRemoteCountDownLatchIT.LatchEventListener(sName);
        AbstractClusteredRemoteCountDownLatchIT.LatchEventListener listener2 = new AbstractClusteredRemoteCountDownLatchIT.LatchEventListener(sName);

        // add the listener to listen for latch events on all members
        member1.addListener(listener1);
        member2.addListener(listener2);

        // Acquire the latch on first member
        int                        count         = 3;
        Random                     random        = Randoms.getRandom();
        int                        limit         = 5;
        CompletableFuture<Boolean> futureAcquire = member1.submit(new AbstractClusteredRemoteCountDownLatchIT.AcquireLatch(sName, count));

        listener1.awaitAcquired(Duration.ofSeconds(20));

        AbstractClusteredRemoteCountDownLatchIT.CountDown countDown       = new AbstractClusteredRemoteCountDownLatchIT.CountDown(sName, count, Duration.ofSeconds(random.nextInt(limit)));
        CompletableFuture<Void>                           futureCountDown = member1.submit(countDown);

        listener1.awaitCountedDown(Duration.ofSeconds(20));
        assertThat(futureCountDown.get(), nullValue());

        assertRemoteCount(member2, sName, count, 2);

        m_coherenceResource.getCluster().filter(member -> member.getLocalMemberUID().equals(member1.getLocalMemberUID())).relaunch();
        CoherenceClusterMember newMember1 = m_coherenceResource.getCluster().get("storage-1");

        ConcurrentHelper.ensureConcurrentServiceRunning(m_coherenceResource.getCluster());

        LatchEventListener listener3 = new LatchEventListener(sName);
        newMember1.addListener(listener3);

        futureAcquire = newMember1.submit(new AbstractClusteredRemoteCountDownLatchIT.AcquireLatch(sName, count));
        listener3.awaitAcquired(Duration.ofSeconds(10));

        assertRemoteCount(newMember1, sName, count, 2);

        futureCountDown = member2.submit(countDown);
        listener2.awaitCountedDown(Duration.ofSeconds(20));
        assertThat(futureCountDown.get(), nullValue());

        try
            {
            futureAcquire.get(2, TimeUnit.SECONDS);
            fail("Should time out");
            }
        catch (TimeoutException e)
            {}

        futureCountDown = newMember1.submit(countDown);
        listener3.awaitCountedDown(Duration.ofSeconds(20));
        assertThat(futureCountDown.get(), nullValue());
        assertThat(futureAcquire.get(), is(true));

        GetLatchsMapSize getSize = new GetLatchsMapSize();
        assertThat(newMember1.submit(getSize).get().intValue(), is(0));
        assertThat(member2.submit(getSize).get().intValue(), is(0));

        newMember1.removeListener(listener3);
        }

    //---- helper methods ---------------------------------------------------

    /**
     * Return a test-scoped latch name.
     *
     * @param sSuffix  the latch name suffix
     *
     * @return a test-scoped latch name
     */
    private String latchName(String sSuffix)
        {
        return m_sTestName + "-" + sSuffix;
        }

    /**
     * Eventually assert a remote latch count.
     *
     * @param member  the cluster member to query
     * @param sName   the latch name
     * @param count   the initial latch count
     * @param nValue  the expected current count
     */
    private void assertRemoteCount(CoherenceClusterMember member, String sName, int count, int nValue)
        {
        Eventually.assertDeferred(() ->
            {
            try
                {
                return member.submit(new GetCount(sName, count)).get().intValue();
                }
            catch (Exception e)
                {
                throw new RuntimeException(e);
                }
            }, is(nValue));
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
    Void shouldAcquireAndCountDown(String sName) throws InterruptedException
        {
        Logger.info("In shouldAcquireAndCountDown()");
        RemoteCountDownLatch latch     = Latches.remoteCountDownLatch(sName, 1);
        Semaphore                 semaphore = new Semaphore(0);
        Thread                    worker    = new Thread(new Runnable()
            {
            @Override
            public void run()
                {
                semaphore.acquireUninterruptibly();
                latch.countDown();
                System.out.println(Thread.currentThread().getName()
                        + " finished");
                }
            });

        worker.start();
        assertThat(latch.getCount(), is(1L));
        semaphore.release();
        latch.await();
        assertThat(ConcurrentHelper.latchesMap().size(), is(0));

        return null;
        }

    /**
     * This test acquires a latch on one cluster member and then tries to countdown
     * the same latch on another member.
     *
     * @param member1  the member to acquire the latch on
     * @param member2  the member to countdown the latch on
     *
     * @throws Exception if the test fails
     */
    void shouldCountDownIfTheLatchIsCreatedByAnotherMember(CoherenceClusterMember member1, CoherenceClusterMember member2,
                                                            String sName)
            throws Exception
        {
        AbstractClusteredRemoteCountDownLatchIT.LatchEventListener listener1 = new AbstractClusteredRemoteCountDownLatchIT.LatchEventListener(sName);
        AbstractClusteredRemoteCountDownLatchIT.LatchEventListener listener2 = new AbstractClusteredRemoteCountDownLatchIT.LatchEventListener(sName);

        // Add the listeners to listen for latch events from the all members.
        member1.addListener(listener1);
        member2.addListener(listener2);

        // Acquire the latch and wait on first member
        int                        count         = 1;
        CompletableFuture<Boolean> futureAcquire = member1.submit(new AbstractClusteredRemoteCountDownLatchIT.AcquireLatch(sName, count));
        listener1.awaitAcquired(Duration.ofSeconds(20));

        AbstractClusteredRemoteCountDownLatchIT.CountDown countDown       = new AbstractClusteredRemoteCountDownLatchIT.CountDown(sName, count, Duration.ofSeconds(1));
        CompletableFuture<Void>                        futureCountDown = member2.submit(countDown);

        listener2.awaitCountedDown(Duration.ofSeconds(20));
        assertThat(futureCountDown.get(), nullValue());
        assertThat(futureAcquire.get(), is(true));
        GetLatchsMapSize getSize = new GetLatchsMapSize();
        assertThat(member1.submit(getSize).get().intValue(), is(0));
        assertThat(member2.submit(getSize).get().intValue(), is(0));
        }

    // ----- inner class: AcquireLatch --------------------------------------

    /**
     * A Bedrock remote callable that acquires a {@link RemoteCountDownLatch}.
     * <p>
     * This callable fires remote events to indicate when the acquire happened.
     */
    static class AcquireLatch
            implements RemoteCallable<Boolean>
        {
        /**
         * A remote channel injected by Bedrock and used to fire events back to the test.
         */
        @RemoteChannel.Inject
        private RemoteChannel remoteChannel;

        /**
         * The name of the latch to acquire.
         */
        private final String f_sName;

        /**
         * The latch count.
         */
        private final long f_count;

        /**
         * Create an {@link RemoteCountDownLatch} callable.
         *
         * @param sName     the name of the latch to acquire
         * @param count     the latch count
         */
        AcquireLatch(String sName, long count)
            {
            f_sName = sName;
            f_count = count;
            }

        @Override
        public Boolean call()
            {
            Logger.info("Acquiring latch " + f_sName);
            try
                {
                RemoteCountDownLatch latch = Latches.remoteCountDownLatch(f_sName, (int) f_count);
                remoteChannel.raise(new LatchEvent(f_sName, LatchEventType.Acquired));
                Logger.info("Acquired latch " + f_sName + " with count " + latch.getCount());

                latch.await();
                }
            catch (InterruptedException e)
                {}
            catch (IllegalArgumentException e)
                {
                Logger.info("Acquired latch " + f_sName + " with count " + f_count + " got exception: " + e);
                throw e;
                }

            return true;
            }
        }

    // ----- inner class: CountDown -----------------------------------------

    /**
     * A Bedrock remote runnable that counts down a latch.
     */
    static class CountDown
            implements RemoteRunnable
        {
        /**
         * A remote channel injected by Bedrock and used to fire events back to the test.
         */
        @RemoteChannel.Inject
        private RemoteChannel remoteChannel;

        /**
         * The name of the latch.
         */
        private final String f_sName;

        /**
         * The latch count.
         */
        private final int f_nCount;

        /**
         * The amount of time to wait to before countdown.
         */
        private final Duration f_duration;

        /**
         * Create a {@link CountDown} callable.
         *
         * @param sName     the name of the latch to acquire
         * @param duration  the amount of time to wait to before countdown
         */
        public CountDown(String sName, int nCount, Duration duration)
            {
            f_sName = sName;
            f_nCount = nCount;
            f_duration = duration;
            }

        @Override
        public void run()
            {
            try
                {
                RemoteCountDownLatch latch = Latches.remoteCountDownLatch(f_sName, f_nCount);

                if (latch != null)
                    {
                    Base.sleep(f_duration.toMillis());
                    latch.countDown();
                    remoteChannel.raise(new LatchEvent(f_sName, LatchEventType.CountedDown));
                    }
                }
            catch (IllegalArgumentException e)
                {
                Logger.info("CountDown on latch " + f_sName + " with initial count " + f_nCount + " got exception: " + e);
                throw e;
                }
            }
        }

    // ----- inner class: GetCount ------------------------------------------

    /**
     * A class that get the current count of the named latch.
     */
    static class GetCount implements RemoteCallable<Long>
        {
        private final String f_sName;
        private final long   f_lInitialCount;

        public GetCount(String sName, int nInitialCount)
            {
            this.f_sName = sName;
            this.f_lInitialCount = nInitialCount;
            }

        @Override
        public Long call()
            {
            return Latches.remoteCountDownLatch(f_sName, (int) f_lInitialCount).getCount();
            }
        }

    // ----- inner class: GetLatchsMapSize ----------------------------------

    /**
     * A class that get the size of the latches named-map.
     */
    static class GetLatchsMapSize implements RemoteCallable<Integer>
        {
        @Override
        public Integer call()
            {
            return ConcurrentHelper.latchesMap().size();
            }
        }

    // ----- inner class: LatchEvent ----------------------------------------

    /**
     * A Bedrock remote event submitted by the {@link AcquireLatch} callable
     * to notify the calling test when the latch has been acquired and
     * counted down.
     */
    static class LatchEvent
            implements RemoteEvent
        {
        /**
         * The name of the latch.
         */
        private final String f_sName;

        /**
         * The type of the event.
         */
        private final LatchEventType f_type;

        /**
         * Create a latch event.
         *
         * @param sName  the name of the latch
         * @param type   the type of the event
         */
        public LatchEvent(String sName, LatchEventType type)
            {
            f_sName = sName;
            f_type  = type;
            }

        /**
         * Returns the name of the latch.
         *
         * @return  the name of the latch
         */
        public String getName()
            {
            return f_sName;
            }

        /**
         * Returns the event type.
         *
         * @return  the event type
         */
        public LatchEventType getEventType()
            {
            return f_type;
            }
        }

    // ----- inner class LatchEventListener ----------------------------------

    /**
     * A {@link RemoteEventListener} that listens for {@link LatchEvent events}.
     */
    static class LatchEventListener
            implements RemoteEventListener
        {
        /**
         * The name of the latch.
         */
        private final String f_sName;

        /**
         * A future that completes when the latch acquired event is received.
         */
        private final CompletableFuture<Boolean> f_futureAcquired = new CompletableFuture<>();

        /**
         * A future that completes when the countdown event is received.
         */
        private final CompletableFuture<Void> f_futureCountdown = new CompletableFuture<>();

        /**
         * The time the latch was acquired.
         */
        private Instant m_acquiredAt;

        /**
         * The time the latch was counted down.
         */
        private Instant m_countedDownAt;

        /**
         * Create a {@link LatchEventListener}.
         *
         * @param sName  the name of the latch
         */
        public LatchEventListener(String sName)
            {
            f_sName = sName;
            }

        @Override
        public void onEvent(RemoteEvent event)
            {
            if (event instanceof LatchEvent && f_sName.equals(((LatchEvent) event).getName()))
                {
                Logger.info("Received LatchEvent " + event);
                switch (((LatchEvent) event).getEventType())
                    {
                    case Acquired:
                        m_acquiredAt = Instant.now();
                        f_futureAcquired.complete(true);
                        break;
                    case CountedDown:
                        m_countedDownAt = Instant.now();
                        f_futureCountdown.complete(null);
                        break;
                    }
                }
            }

        /**
         * Wait for the latch acquired event.
         *
         * @param timeout  the maximum amount of time to wait
         */
        public void awaitAcquired(Duration timeout) throws Exception
            {
            f_futureAcquired.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }

        /**
         * Returns true if the latch has been acquired.
         *
         * @return true if the latch has been acquired
         */
        public boolean isAcquired()
            {
            return f_futureAcquired.isDone();
            }

        /**
         * Returns the time that the latch was acquired.
         *
         * @return the time that the latch was acquired
         */
        public Instant getAcquiredAt()
            {
            return m_acquiredAt;
            }

        /**
         * Wait for the latch countdown event.
         *
         * @param timeout  the maximum amount of time to wait
         */
        public void awaitCountedDown(Duration timeout) throws Exception
            {
            f_futureCountdown.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }

        /**
         * Returns true if the latch is counted down.
         *
         * @return true if the latch is counted down
         */
        public boolean isCountedDown()
            {
            return f_futureCountdown.isDone();
            }

        /**
         * Returns the time that the latch was counted down.
         *
         * @return the time that the latch was counted down
         */
        public Instant getCountedDownAt()
            {
            return m_countedDownAt;
            }
        }

    // ----- inner enum LatchEventType ---------------------------------------

    /**
     * An enum of latch event types.
     */
    enum LatchEventType {Acquired, CountedDown}

    // ----- data members ---------------------------------------------------

    /**
     * A Bedrock JUnit5 extension with a Coherence cluster for the tests.
     */     
    static CoherenceClusterExtension m_coherenceResource;

    /**
     * The current test name, used to scope latch names.
     */
    private String m_sTestName;

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
