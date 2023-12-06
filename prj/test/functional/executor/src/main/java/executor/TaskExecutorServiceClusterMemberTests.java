/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.features.JmxFeature;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.oracle.coherence.concurrent.executor.AbstractTaskCoordinator;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorInfo;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;
import com.oracle.coherence.concurrent.executor.ClusteredProperties;
import com.oracle.coherence.concurrent.executor.ClusteredTaskCoordinator;
import com.oracle.coherence.concurrent.executor.Task;
import com.oracle.coherence.concurrent.executor.TaskCollectors;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import executor.common.CoherenceClusterResource;
import executor.common.LogOutput;
import executor.common.LongRunningTask;
import executor.common.SingleClusterForAllTests;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.oracle.coherence.concurrent.executor.subscribers.RecordingSubscriber;

import com.oracle.coherence.concurrent.executor.tasks.ValueTask;

import executor.common.Utils;
import executor.common.Watcher;

import java.util.concurrent.TimeUnit;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import org.hamcrest.core.Is;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.junit.experimental.categories.Category;

import static executor.AbstractClusteredExecutorServiceTests.EXECUTOR_LOGGING_PROPERTY;
import static executor.AbstractClusteredExecutorServiceTests.EXTEND_ENABLED_PROPERTY;
import static executor.AbstractClusteredExecutorServiceTests.STORAGE_DISABLED_MEMBER_ROLE;

import static executor.AbstractClusteredExecutorServiceTests.ensureConcurrentServiceRunning;
import static org.hamcrest.core.Is.is;

/**
 * Integration Tests for the {@link ClusteredExecutorService} to test the task coordinator
 * being a client member (storage disabled) of a Coherence cluster.
 *
 * @author lh
 * @since 21.12
 */
@SuppressWarnings("resource")
@Category(SingleClusterForAllTests.class)
public class TaskExecutorServiceClusterMemberTests
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void setupClass()
        {
        // ensure the cluster service is running
        ensureConcurrentServiceRunning(s_coherence.getCluster());
        }

    @After
    public void cleanup()
            throws Exception
        {
        String sMsg = ">>>>> Finished test: " + f_watcher.getMethodName();
        for (CoherenceClusterMember member : s_coherence.getCluster())
            {
            if (member != null)
                {
                member.submit(() ->
                              {
                              System.err.println(sMsg);
                              System.err.flush();
                              return null;
                              }).join();
                }
            }

        if (m_taskExecutorService != null)
            {
            m_taskExecutorService.shutdown();

            // clear the caches between tests
            Caches.tasks(getCacheService()).clear();
            Caches.assignments(getCacheService()).clear();
            }
        Coherence.closeAll();
        }

    @SuppressWarnings("rawtypes")
    @Before
    public void setup() throws Exception
        {
        System.setProperty("coherence.cluster", "TaskExecutorServiceClusterMemberTests");
        System.setProperty("tangosol.coherence.distributed.localstorage", "false");
        Coherence coherence = Coherence.clusterMember(CoherenceConfiguration.builder().discoverSessions().build());
        coherence.start().get(5, TimeUnit.MINUTES);
        Session session = coherence.getSession(ConcurrentServicesSessionConfiguration.SESSION_NAME);

        // establish an ExecutorService based on storage disabled (client) member
        m_taskExecutorService = new ClusteredExecutorService(session);

        // verify that there are getInitialExecutorCount() Executors available and that they are in the RUNNING state
        NamedCache executors = Caches.executors(session);

        Eventually.assertDeferred(executors::size, is(getInitialExecutorCount()));

        for (Object key : executors.keySet())
            {
            Eventually.assertDeferred(() -> getExecutorServiceInfo(executors, (String) key).getState(),
                                      Is.is(TaskExecutorService.ExecutorInfo.State.RUNNING));
            }

        String sMsg = ">>>>> Starting test: " + f_watcher.getMethodName();
        for (CoherenceClusterMember member : s_coherence.getCluster())
            {
            if (member != null)
                {
                member.submit(() ->
                              {
                              System.err.println(sMsg);
                              System.err.flush();
                              return null;
                              }).join();
                }
            }
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldExecuteAndCompleteTask()
        {
        Utils.assertWithFailureAction(this::doShouldExecuteAndCompleteTask, this::dumpExecutorCacheStates);
        }

    @Test
    public void shouldAllowFailoverLongRunningTest()
        {
        Utils.assertWithFailureAction(this::doShouldAllowFailoverLongRunningTest, this::dumpExecutorCacheStates);
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("rawtypes")
    protected void doShouldExecuteAndCompleteTask()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator =
                m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                        .collect(TaskCollectors.lastOf())
                        .until(Predicates.notNullValue())
                        .subscribe(subscriber)
                        .submit();

        Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), Matchers.is(false));
        MatcherAssert.assertThat(subscriber.received("Hello World"), Matchers.is(true));
        MatcherAssert.assertThat(subscriber.size(), Matchers.is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), Matchers.is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), Matchers.is(false));
        MatcherAssert.assertThat(coordinator.isDone(), Matchers.is(true));

        Eventually.assertDeferred(() -> Caches.tasks(getCacheService()).size(), is(0));
        Eventually.assertDeferred(() -> Caches.assignments(getCacheService()).size(), is(0));
        }

    @SuppressWarnings("rawtypes")
    protected void doShouldAllowFailoverLongRunningTest()
        {
        CoherenceCluster            cluster    = s_coherence.getCluster();
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        // start a long-running task on a client member
        final String taskName = "longRunningTask";
        ClusteredTaskCoordinator coordinator = (ClusteredTaskCoordinator) m_taskExecutorService.orchestrate(new LongRunningTask(Duration.ofSeconds(30)))
                .as(taskName)
                .filter(Predicates.role(STORAGE_DISABLED_MEMBER_ROLE))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber)
                .submit();

        // verify that the task has started
        ClusteredProperties properties = (ClusteredProperties) coordinator.getProperties();
        Eventually.assertDeferred(() -> properties.get("count"), Matchers.notNullValue());

        properties.put("key1", "value1");

        // now restart the storage disabled member
        cluster.filter(member -> member.getRoleName().equals(STORAGE_DISABLED_MEMBER_ROLE)).relaunch();
        AbstractClusteredExecutorServiceTests.ensureConcurrentServiceRunning(cluster);

        // make sure the task is failed over to the new member and the subscriber received the result
        MatcherAssert.assertThat(properties.get("key1"), Matchers.is("value1"));
        Eventually.assertDeferred(() -> subscriber.received("DONE"),
                                  Matchers.is(true),
                                  Eventually.within(3, TimeUnit.MINUTES));
        }

    /**
     * Dump current executor cache states.
     */
    protected void dumpExecutorCacheStates()
        {
        CacheService service = getCacheService();

        Utils.dumpExecutorCacheStates(Caches.executors(service),
                                      Caches.assignments(service),
                                      Caches.tasks(service),
                                      Caches.properties(service));

        //Utils.heapdump(s_coherence.getCluster());
        }

    public CacheService getCacheService()
        {
        return m_taskExecutorService.getCacheService();
        }

    protected int getInitialExecutorCount()
        {
        return STORAGE_ENABLED_MEMBER_COUNT + STORAGE_DISABLED_MEMBER_COUNT + 1;
        }

    /**
     * Obtains the {@link ClusteredExecutorInfo} for the specified {@link Executor}
     * from the {@link TaskExecutorService.ExecutorInfo} {@link NamedCache}.
     *
     * @param executorInfoCache  the {@link NamedCache}
     * @param executorId         the {@link ExecutorService} identity
     *
     * @return the {@link ClusteredExecutorInfo} or <code>null</code> if it doesn't exist
     */
    @SuppressWarnings("rawtypes")
    public ClusteredExecutorInfo getExecutorServiceInfo(NamedCache executorInfoCache,
                                                        String     executorId)
        {
        return (ClusteredExecutorInfo) executorInfoCache.get(executorId);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The number of storage enabled members in the {@link CoherenceCluster}.
     */
    protected final static int STORAGE_ENABLED_MEMBER_COUNT = 2;

    /**
     * The number of storage disabled members in the {@link CoherenceCluster}.
     */
    protected final static int STORAGE_DISABLED_MEMBER_COUNT = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The {@link CoherenceClusterResource} to establish a {@link CoherenceCluster} for testing.
     */
    @ClassRule
    public static CoherenceClusterResource s_coherence =
            (CoherenceClusterResource) new CoherenceClusterResource()
                    .with(ClassName.of(Coherence.class),
                          Multicast.ttl(0),
                          LocalHost.only(),
                          Logging.at(9),
                          ClusterPort.of(7574),
                          ClusterName.of(TaskExecutorServiceClusterMemberTests.class.getSimpleName()),
                          JmxFeature.enabled(),
                          StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()))
                    .include(STORAGE_ENABLED_MEMBER_COUNT,
                             DisplayName.of("CacheServer"),
                             LogOutput.to(TaskExecutorServiceClusterMemberTests.class.getSimpleName(), "CacheServer"),
                             RoleName.of("storage"),
                             LocalStorage.enabled(),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, false),
                             SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, true))
                    .include(STORAGE_DISABLED_MEMBER_COUNT,
                             DisplayName.of("ComputeServer"),
                             LogOutput.to(TaskExecutorServiceClusterMemberTests.class.getSimpleName(), "ComputeServer"),
                             RoleName.of("compute"),
                             LocalStorage.disabled(),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, false),
                             SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, true));

    /**
     * The {@link TaskExecutorService}.
     */
    protected ClusteredExecutorService m_taskExecutorService;

    /**
     * JUnit TestWatcher.
     */
    @Rule
    public final Watcher f_watcher = new Watcher();
    }
