/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;

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

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.oracle.coherence.concurrent.executor.AbstractTaskCoordinator;
import com.oracle.coherence.concurrent.executor.ClusteredAssignment;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorInfo;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;
import com.oracle.coherence.concurrent.executor.ClusteredProperties;
import com.oracle.coherence.concurrent.executor.ClusteredTaskCoordinator;
import com.oracle.coherence.concurrent.executor.ClusteredTaskManager;
import com.oracle.coherence.concurrent.executor.Task;
import com.oracle.coherence.concurrent.executor.TaskCollectors;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;

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

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import org.hamcrest.core.Is;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.junit.experimental.categories.Category;

import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;

import org.junit.runner.Description;

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
@Category(SingleClusterForAllTests.class)
public class TaskExecutorServiceClusterMemberTests
    {
    // ----- test lifecycle -------------------------------------------------

    @AfterClass
    public static void afterClass()
        {
        s_coherence.after();
        }

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
        if (m_taskExecutorService != null)
            {
            m_taskExecutorService.shutdown();

            // clear the caches between tests
            getNamedCache(ClusteredTaskManager.CACHE_NAME).clear();
            getNamedCache(ClusteredAssignment.CACHE_NAME).clear();
            }
        m_session.close();
        m_local.close();
        }

    @SuppressWarnings("rawtypes")
    @Before
    public void setup()
        {
        System.setProperty("coherence.cluster", "TaskExecutorServiceClusterMemberTests");
        m_local = Coherence.clusterMember(CoherenceConfiguration.builder().discoverSessions().build());
        m_local.start().join();
        m_session = m_local.getSession(ConcurrentServicesSessionConfiguration.SESSION_NAME);

        // establish an ExecutorService based on storage disabled (client) member
        m_taskExecutorService = new ClusteredExecutorService(m_session);

        // verify that there are getInitialExecutorCount() Executors available and that they are in the RUNNING state
        NamedCache executors = m_session.getCache(ClusteredExecutorInfo.CACHE_NAME);

        Eventually.assertDeferred(executors::size, is(getInitialExecutorCount()));

        for (Object key : executors.keySet())
            {
            Eventually.assertDeferred(() -> getExecutorServiceInfo(executors, (String) key).getState(),
                                      Is.is(TaskExecutorService.ExecutorInfo.State.RUNNING));
            }
        }

    // ----- test methods ---------------------------------------------------

    @SuppressWarnings("rawtypes")
    @Test
    public void shouldExecuteAndCompleteTask()
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

        Eventually.assertDeferred(() -> getNamedCache(ClusteredTaskManager.CACHE_NAME).size(), is(0));
        Eventually.assertDeferred(() -> getNamedCache(ClusteredAssignment.CACHE_NAME).size(), is(0));
        }

    @SuppressWarnings("rawtypes")
    @Test
    public void shouldAllowFailoverLongRunningTest()
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
        Eventually.assertDeferred(() -> subscriber.received("DONE"), Matchers.is(true));
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("unchecked")
    public <K, V> NamedCache<K, V> getNamedCache(String sName)
        {
        return m_taskExecutorService.getCacheService().ensureCache(sName, null);
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
                          JmxFeature.enabled())
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
     * Rule to demarcate tests in a single-log test run.
     */
    @Rule
    public TestRule watcher = new TestWatcher()
        {
        protected void starting(Description description)
            {
            System.out.println("### Starting test: " + description.getMethodName());
            }

        protected void failed(Throwable e, Description description)
            {
            System.out.println("### Failed test: " + description.getMethodName());
            System.out.println("### Cause: " + e);
            e.printStackTrace();
            }

        protected void finished(Description description)
            {
            System.out.println("### Completed test: " + description.getMethodName());
            }
        };

    //protected ConfigurableCacheFactory m_cacheFactory;
    protected Coherence m_local;
    protected Session m_session;

    /**
     * The {@link TaskExecutorService}.
     */
    protected ClusteredExecutorService m_taskExecutorService;
    }
