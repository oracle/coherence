/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.bedrock.junit.ExtendClient;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.features.JmxFeature;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Blocking;

import com.oracle.coherence.concurrent.executor.ClusteredExecutorInfo;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;
import com.oracle.coherence.concurrent.executor.Task;
import com.oracle.coherence.concurrent.executor.TaskCollectors;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;

import com.tangosol.util.Base;

import executor.common.CoherenceClusterResource;
import executor.common.LogOutput;
import executor.common.SingleClusterForAllTests;
import executor.common.LongRunningTask;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.oracle.coherence.concurrent.executor.subscribers.RecordingSubscriber;

import com.tangosol.io.FileHelper;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import executor.common.Utils;
import executor.common.Watcher;

import java.util.concurrent.TimeUnit;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.time.Duration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.junit.experimental.categories.Category;

import static executor.AbstractClusteredExecutorServiceTests.EXECUTOR_LOGGING_PROPERTY;
import static executor.AbstractClusteredExecutorServiceTests.EXTEND_ADDRESS_PROPERTY;
import static executor.AbstractClusteredExecutorServiceTests.EXTEND_ENABLED_PROPERTY;
import static executor.AbstractClusteredExecutorServiceTests.EXTEND_PORT_PROPERTY;

import static org.hamcrest.core.Is.is;

/**
 * Integration Tests for the {@link ClusteredExecutorService} to test Persistence.
 *
 * @author lh
 * @since 21.12
 */
@Ignore("COH-27716")
@Category(SingleClusterForAllTests.class)
public class TaskExecutorServicePersistenceTests
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void setupClass()
        {
        // ensure the proxy service is running (before we connect)
        AbstractClusteredExecutorServiceTests.ensureConcurrentServiceRunning(s_coherence.getCluster());
        }

    @AfterClass
    public static void cleanupClass()
        {
        deleteDir(s_fileActive);
        deleteDir(s_fileSnapshot);
        deleteDir(s_fileTrash);
        }

    @SuppressWarnings("rawtypes")
    @Before
    public void setup()
        {
        // connect as an *Extend client
        m_cacheFactory = s_coherence.createSession(new ExtendClient(EXTEND_CONFIG, SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, "true")));

        // establish an ExecutorService based on the *Extend client
        m_taskExecutorService = createExecutorService();

        // verify that there are getInitialExecutorCount() Executors available and that they are in the RUNNING state
        NamedCache executors = Caches.executors(getCacheService());

        Eventually.assertDeferred(executors::size, is(getInitialExecutorCount()));

        for (Object key : executors.keySet())
            {
            Eventually.assertDeferred(() -> getExecutorServiceInfo(executors, (String) key).getState(),
                                      is(TaskExecutorService.ExecutorInfo.State.RUNNING));
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

    @After
    public void cleanup()
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
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldPersistLongRunningTest()
        {
        Utils.assertWithFailureAction(this::doShouldPersistLongRunningTest, this::dumpExecutorCacheStates);
        }

    @Test
    @Ignore
    public void shouldPersistLongRunningTaskAfterRollingRestartCluster()
        {
        Utils.assertWithFailureAction(this::doShouldPersistLongRunningTaskAfterRollingRestartCluster, this::dumpExecutorCacheStates);
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void doShouldPersistLongRunningTest()
        {
        CoherenceCluster cluster = s_coherence.getCluster();

        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        // start a long running task on a storage member
        Task.Coordinator coordinator = m_taskExecutorService.orchestrate(new LongRunningTask(Duration.ofSeconds(30)))
                .filter(Predicates.role("storage"))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber)
                .submit();

        String taskId = coordinator.getTaskId();

        // wait a bit for the task to start
        try
            {
            Blocking.sleep(2000);
            }
        catch (InterruptedException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        // ensure that we haven't completed or cancelled
        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));

        // now close all storage members
        cluster.filter(member -> member.getRoleName().equals("storage")).close();

        // now start new storage members (based on compute members)
        cluster.filter(member -> member.getRoleName().equals("compute"))
                .limit(1)
                .clone(STORAGE_ENABLED_MEMBER_COUNT,
                       DisplayName.of("CacheServer"),
                       RoleName.of("storage"),
                       LocalStorage.enabled(),
                       SystemProperty.of("coherence.extend.enabled", false));

        coordinator = m_taskExecutorService.acquire(taskId);
        RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();
        coordinator.subscribe(subscriber2);

        // ensure that we are eventually done! (ie: a new compute member picks up the task)
        Eventually.assertDeferred(() -> subscriber2.received("DONE"),
                                  Matchers.is(true),
                                  Eventually.within(4, TimeUnit.MINUTES));
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void doShouldPersistLongRunningTaskAfterRollingRestartCluster()
        {
        CoherenceCluster            cluster    = s_coherence.getCluster();
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        // start a long running task on a compute member
        Task.Coordinator coordinator = m_taskExecutorService.orchestrate(new LongRunningTask(Duration.ofSeconds(30)))
                .filter(Predicates.role("compute"))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber)
                .submit();

        String taskId = coordinator.getTaskId();

        // wait for the task to start
        try
            {
            Blocking.sleep(2000);
            }
        catch (InterruptedException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        // ensure that we haven't completed or cancelled
        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));

        // now restart cluster
        m_taskExecutorService.shutdown();
        cluster.relaunch();
        AbstractClusteredExecutorServiceTests.ensureConcurrentServiceRunning(cluster);

        setup();
        coordinator = m_taskExecutorService.acquire(taskId);
        RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();
        coordinator.subscribe(subscriber2);

        // ensure that we are eventually done! (ie: a new compute member picks up the task)
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

    protected CacheService getCacheService()
        {
        return m_taskExecutorService.getCacheService();
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

    protected ClusteredExecutorService createExecutorService()
        {
        return new ClusteredExecutorService(m_cacheFactory);
        }

    protected int getInitialExecutorCount()
        {
        return STORAGE_ENABLED_MEMBER_COUNT + STORAGE_DISABLED_MEMBER_COUNT + PROXY_MEMBER_COUNT;
        }

    /**
     * Create a unique temporary directory.
     *
     * @return a unique temporary directory
     */
    protected static File createTempDir()
        {
        try
            {
            return FileHelper.createTempDir();
            }
        catch (IOException e)
            {
            System.out.println("got an exception: " + e);
            return null;
            }
        }

    /**
     * Delete the file from disk.
     *
     * @param dir the file to delete
     */
    protected static void deleteDir(File dir)
        {
        try
            {
            FileHelper.deleteDir(dir);
            }
        catch (IOException e)
            {
            // ignore
            }
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

    /**
     * The number of proxy servers members in the {@link CoherenceCluster}.
     */
    protected final static int PROXY_MEMBER_COUNT = 1;

    // ----- constants ------------------------------------------------------

    protected static final String EXTEND_CONFIG = "coherence-concurrent-client-config.xml";

    // ----- data members ---------------------------------------------------

    protected static File s_fileActive   = createTempDir();
    protected static File s_fileSnapshot = createTempDir();
    protected static File s_fileTrash    = createTempDir();

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
                          ClusterName.of(TaskExecutorServicePersistenceTests.class.getSimpleName()), // default name is too long
                          SystemProperty.of(EXTEND_ADDRESS_PROPERTY, LocalPlatform.get().getLoopbackAddress().getHostAddress()),
                          SystemProperty.of(EXTEND_PORT_PROPERTY, "9099"),
                          JmxFeature.enabled(),
                          Pof.config("coherence-executor-test-pof-config.xml"),
                          SystemProperty.of("coherence.concurrent.persistence.environment",   "default-active"),
                          SystemProperty.of("coherence.distributed.persistence.active.dir",   s_fileActive.getAbsoluteFile()),
                          SystemProperty.of("coherence.distributed.persistence.snapshot.dir", s_fileSnapshot.getAbsoluteFile()),
                          SystemProperty.of("coherence.distributed.persistence.trash.dir",    s_fileTrash.getAbsoluteFile()),
                          StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()))
                    .include(STORAGE_ENABLED_MEMBER_COUNT,
                             DisplayName.of("CacheServer"),
                             LogOutput.to(TaskExecutorServicePersistenceTests.class.getSimpleName(), "CacheServer"),
                             RoleName.of("storage"),
                             LocalStorage.enabled(),
                             SystemProperty.of("coherence.distributed.threads", "10"),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, false),
                             SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, true))
                    .include(STORAGE_DISABLED_MEMBER_COUNT,
                             DisplayName.of("ComputeServer"),
                             LogOutput.to(TaskExecutorServicePersistenceTests.class.getSimpleName(), "ComputeServer"),
                             RoleName.of("compute"),
                             LocalStorage.disabled(),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, false),
                             SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, true))
                    .include(PROXY_MEMBER_COUNT,
                             DisplayName.of("ProxyServer"),
                             LogOutput.to(TaskExecutorServicePersistenceTests.class.getSimpleName(), "ProxyServer"),
                             RoleName.of("proxy"),
                             LocalStorage.disabled(),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, true),
                             SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, true));

    /**
     * JUnit TestWatcher.
     */
    @Rule
    public final Watcher f_watcher = new Watcher();

    /**
     * The {@link CacheFactory}.
     */
    protected ConfigurableCacheFactory m_cacheFactory;

    /**
     * The {@link TaskExecutorService}.
     */
    protected ClusteredExecutorService m_taskExecutorService;
    }
