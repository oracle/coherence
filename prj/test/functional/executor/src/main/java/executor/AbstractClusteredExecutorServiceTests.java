/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.bedrock.deferred.Deferred;

import com.oracle.bedrock.deferred.options.InitialDelay;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.options.Timeout;

import com.oracle.bedrock.runtime.ApplicationStream;
import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.concurrent.runnable.RuntimeHalt;
import com.oracle.bedrock.runtime.concurrent.runnable.SystemExit;

import com.oracle.bedrock.runtime.java.features.JmxFeature;

import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.deferred.Repetitively;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.AbstractTaskCoordinator;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorInfo;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;
import com.oracle.coherence.concurrent.executor.ClusteredTaskCoordinator;
import com.oracle.coherence.concurrent.executor.RecoveringTask;
import com.oracle.coherence.concurrent.executor.Task;
import com.oracle.coherence.concurrent.executor.TaskCollectors;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.oracle.coherence.concurrent.executor.management.ExecutorMBean;

import com.oracle.coherence.concurrent.executor.options.Member;
import com.oracle.coherence.concurrent.executor.options.Role;
import com.oracle.coherence.concurrent.executor.options.Storage;

import com.oracle.coherence.concurrent.executor.subscribers.RecordingSubscriber;

import com.oracle.coherence.concurrent.executor.tasks.CronTask;
import com.oracle.coherence.concurrent.executor.tasks.ValueTask;

import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.io.Serializer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.messaging.ConnectionException;

import com.tangosol.util.Base;

import executor.common.AbstractTaskExecutorServiceTests;
import executor.common.LongRunningTask;
import executor.common.RepeatedTask;

import executor.common.Watcher;

import java.time.Duration;

import java.util.Set;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import org.hamcrest.core.Is;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import static com.oracle.bedrock.deferred.DeferredHelper.ensure;
import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.oracle.bedrock.testsupport.deferred.Eventually.delayedBy;
import static com.oracle.bedrock.testsupport.deferred.Eventually.within;

import static com.oracle.coherence.concurrent.executor.TaskExecutorService.ExecutorInfo.State.REJECTING;
import static com.oracle.coherence.concurrent.executor.TaskExecutorService.ExecutorInfo.State.RUNNING;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import static org.hamcrest.core.Is.is;

import static org.junit.Assert.fail;

/**
 * Integration Tests for the {@link ClusteredExecutorService}.
 *
 * @author bo
 * @author lh
 * @since 21.12
 */
@SuppressWarnings({"rawtypes", "resource"})
public abstract class AbstractClusteredExecutorServiceTests
        extends AbstractTaskExecutorServiceTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Test constructor.
     *
     * @param extendConfig  the Extend client configuration
     */
    public AbstractClusteredExecutorServiceTests(String extendConfig)
        {
        m_extendConfig = extendConfig;
        }

    // ----- test lifecycle -------------------------------------------------

    @Before
    public void setup()
        {
        initCluster();

        ensureConcurrentServiceRunning(getCluster());
        ensureExecutorProxyAvailable(getCluster());

        // establish an ExecutorService based on the *Extend client
        m_taskExecutorService = createExecutorService();

        // verify that there are getInitialExecutorCount() Executors available and that they are in the RUNNING state
        NamedCache<String, ClusteredExecutorInfo> executors = Caches.executors(getCacheService());

        Eventually.assertDeferred(executors::size, is(getInitialExecutorCount()));

        for (String key : executors.keySet())
            {
            Eventually.assertDeferred(() -> this.getExecutorServiceInfo(executors, key).getState(),
                                  Is.is(TaskExecutorService.ExecutorInfo.State.RUNNING));
            }

        String sMsg = ">>>>> Starting test: " + f_watcher.getMethodName();
        for (CoherenceClusterMember member : getCoherence().getCluster())
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
        if (m_cacheFactory != null && m_cacheFactory.isActive())
            {
            m_cacheFactory.dispose();
            m_cacheFactory = null;
            }
        for (CoherenceClusterMember member : getCoherence().getCluster())
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

            Eventually.assertDeferred(() -> m_taskExecutorService.isShutdown(), is(true));
            }
        CacheFactory.shutdown();
        }

    // ----- contract -------------------------------------------------------


    @Override
    public CoherenceCluster getCluster()
        {
        return getCoherence().getCluster();
        }

    /**
     * Return the {@link CoherenceClusterResource} under test.
     *
     * @return the {@link CoherenceClusterResource} under test.
     */
    public abstract CoherenceClusterResource getCoherence();

    /**
     * Return the logging label.
     *
     * @return the logging label
     */
    public abstract String getLabel();

    /**
     * Initialize the cluster and create an *Extend client.
     */
    protected void initCluster()
        {
        COUNTER.incrementAndGet();
        CoherenceClusterResource clusterResource = getCoherence();

        // ensure the proxy service is running (before we connect)
        clusterResource.getCluster();

        // connect as an *Extend client
        System.setProperty("coherence.client", "remote-fixed");
        m_cacheFactory = clusterResource.createSession((
                SessionBuilders.extendClient(m_extendConfig,
                                             SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, "true"),
                                             SystemProperty.of("coherence.client", "remote-fixed"))));
        m_cacheFactory.activate();
        }

    // ----- test methods ---------------------------------------------------

    public void shouldCreateClusteredExecutor()
        {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try
            {
            TaskExecutorService.Registration registration = m_taskExecutorService.register(executorService);

            Eventually.assertDeferred(() -> Caches.executors(getCacheService())
                                      .containsKey(registration.getId()), is(true));

            TaskExecutorService.ExecutorInfo info =
                (TaskExecutorService.ExecutorInfo) Caches.executors(getCacheService())
                    .get(registration.getId());

            MatcherAssert.assertThat(info.getOption(Member.class, null).get(),
                              is(registration.getOption(Member.class, null).get()));

            MatcherAssert.assertThat(info.getOption(Role.class, null), is(registration.getOption(Role.class, null)));
            }
        finally
            {
            executorService.shutdown();
            m_taskExecutorService.deregister(executorService);
            }
        }

    @Override
    public void shouldExecuteAndCompleteTask()
        {
        super.shouldExecuteAndCompleteTask();

        Eventually.assertDeferred(() -> Caches.tasks(getCacheService()).size(), is(0));
        Eventually.assertDeferred(() -> Caches.assignments(getCacheService()).size(), is(0));
        }

    @Override
    public void shouldCancelTask()
        {
        super.shouldCancelTask();

        Eventually.assertDeferred(() -> Caches.tasks(getCacheService()).size(), is(0));
        Eventually.assertDeferred(() -> Caches.assignments(getCacheService()).size(), is(0));
        }

    /**
     * Similar to shouldCancelTask() but check cache entries during test
     */
    public void shouldRemoveCacheEntriesOnCancel()
        {
        NamedCache taskManagers = Caches.tasks(getCacheService());
        NamedCache assignments  = Caches.assignments(getCacheService());

        MatcherAssert.assertThat(taskManagers.size(), is(0));
        MatcherAssert.assertThat(assignments.size(), is(0));

        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        // start a task that never completes
        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber)
                .submit();

        Eventually.assertDeferred(taskManagers::size, is(1));                           // task added
        Eventually.assertDeferred(assignments::size, is(getInitialExecutorCount()));    // task assigned

        Eventually.assertDeferred(() -> subscriber.received("Hello World"), is(true));

        MatcherAssert.assertThat(coordinator.cancel(true), is(true));

        Eventually.assertDeferred(subscriber::isSubscribed, is(false));

        Eventually.assertDeferred(taskManagers::size, is(0));
        Eventually.assertDeferred(assignments::size, is(0));
        }

    /**
     * Ensure that when {@link Executor} information is not updated, the {@link Executor}
     * is automatically de-registered.
     */
    public void shouldAutomaticallyDeregisterExecutor()
        {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try
            {
            // internally disable updates of ClusteredExecutorInfo
            ClusteredExecutorInfo.UpdateInfoRunnable.s_fPerformUpdate = false;

            TaskExecutorService.Registration registration = m_taskExecutorService.register(executorService);

            // ensure that the executor info is removed from the cache
            Eventually.assertDeferred(() -> Caches.executors(getCacheService()).containsKey(registration.getId()),
                                            is(false),
                                            InitialDelay.of(10, TimeUnit.SECONDS));

            // ensure that the executor has been deregistered locally
            Eventually.assertDeferred(() -> m_taskExecutorService.deregister(executorService), is(nullValue()));
            }
        finally
            {
            executorService.shutdown();

            m_taskExecutorService.deregister(executorService);

            // internally re-enable updates of ClusteredExecutorInfo
            ClusteredExecutorInfo.UpdateInfoRunnable.s_fPerformUpdate = true;
            }
        }

    public void shouldWorkWithServerAdded()
        {
        ExecutorService executorService1 = Executors.newSingleThreadExecutor();
        ExecutorService executorService2 = Executors.newSingleThreadExecutor();
        ExecutorService executorService3 = Executors.newSingleThreadExecutor();

        try
            {
            m_taskExecutorService.register(executorService1);
            m_taskExecutorService.register(executorService2);

            RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();

            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.count())
                .until(Predicates.is(getInitialExecutorCount() + 5))
                .subscribe(subscriber)
                .submit();

            CoherenceCluster cluster = getCoherence().getCluster();

            // add a few new cluster members
            cluster.filter(m -> m.getRoleName().contains(STORAGE_ENABLED_MEMBER_ROLE)).limit(1).clone(2);

            ConfigurableCacheFactory session =
                    getCoherence().createSession(SessionBuilders.extendClient(m_extendConfig));

            m_taskExecutorService.register(executorService3);

            NamedCache cacheExecutorService = Caches.executors(session);

            Eventually.assertDeferred(cacheExecutorService::size, is(getInitialExecutorCount() + 5));

            Eventually.assertDeferred(subscriber::getLast, Matchers.is(getInitialExecutorCount() + 5));
            }
        finally
            {
            executorService1.shutdown();
            executorService2.shutdown();
            executorService3.shutdown();

            m_taskExecutorService.deregister(executorService1);
            m_taskExecutorService.deregister(executorService2);
            m_taskExecutorService.deregister(executorService3);
            }
        }

    public void shouldWorkWithServerRemoved()
        {
        CoherenceCluster cluster              = getCoherence().getCluster();
        NamedCache       cacheExecutorService = Caches.executors(getCacheService());
        NamedCache       cacheTasks           = Caches.tasks(getCacheService());
        ExecutorService  executorService1     = Executors.newSingleThreadExecutor();
        ExecutorService  executorService2     = Executors.newSingleThreadExecutor();

        try
            {
            m_taskExecutorService.register(executorService1);
            m_taskExecutorService.register(executorService2);
            Eventually.assertDeferred(cacheExecutorService::size, is(getInitialExecutorCount() + 2));

            RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

            m_taskExecutorService.orchestrate(new RepeatedTask<>("Hello World", 10000))
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber)
                .submit();

            Eventually.assertDeferred(cacheTasks::size, is(1));

            int clusterSize = cluster.getClusterSize();

            cluster.filter(member -> !member.getRoleName().equals(PROXY_MEMBER_ROLE)).limit(1).close();
            Eventually.assertDeferred(cluster::getClusterSize, is(clusterSize - 1));

            // refresh in case connected member was stopped
            NamedCache cacheExecutorServiceInternal = Caches.executors(getCacheService());

            // ensure the executor information is eventually cleaned up
            Eventually.assertDeferred(cacheExecutorServiceInternal::size,
                                      is(getInitialExecutorCount() + 1),
                                      within(ClusteredExecutorInfo.LEASE_DURATION_MS + 15000, TimeUnit.MILLISECONDS));

            Eventually.assertDeferred(() -> subscriber.received("Hello World"), Matchers.is(true));
            }
        finally
            {
            executorService1.shutdownNow();
            executorService2.shutdownNow();

            m_taskExecutorService.deregister(executorService1);
            m_taskExecutorService.deregister(executorService2);
            }
        }

    public void shouldFailOverLongRunningTest()
        {
        CoherenceCluster            cluster    = getCoherence().getCluster();
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        // start a long-running task on a compute member
        Task.Coordinator<String> coordinator = m_taskExecutorService
            .orchestrate(new LongRunningTask(Duration.ofSeconds(30)))
            .filter(Predicates.role(STORAGE_DISABLED_MEMBER_ROLE))
            .limit(1)
            .collect(TaskCollectors.lastOf())
            .subscribe(subscriber)
            .submit();

        // verify that the task has started
        Task.Properties properties = coordinator.getProperties();
        Eventually.assertDeferred(() -> properties.get("count"), Matchers.notNullValue());

        // now close all compute members
        cluster.filter(member -> member.getRoleName().equals(STORAGE_DISABLED_MEMBER_ROLE)).close();

        // ensure that we haven't completed or cancelled
        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));

        // now start new compute members (based on storage members)
        cluster.filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE))
            .limit(1)
            .clone(STORAGE_DISABLED_MEMBER_COUNT,
                   DisplayName.of("ComputeServer"),
                   RoleName.of(STORAGE_DISABLED_MEMBER_ROLE),
                   LocalStorage.disabled(),
                   SystemProperty.of(EXTEND_ENABLED_PROPERTY, false));
        ensureConcurrentServiceRunning(cluster);

        // ensure that we are eventually done! (ie: a new compute member picks up the task)
        Eventually.assertDeferred(() -> subscriber.received("DONE"),
                                  Matchers.is(true),
                                  Eventually.within(3, TimeUnit.MINUTES));
        }

    public void shouldCallRunnableAfterFailOverLongRunning()
        {
        CoherenceCluster            cluster      = getCoherence().getCluster();
        RecordingSubscriber<String> subscriber   = new RecordingSubscriber<>();
        MyCompletion                myCompletion = new MyCompletion();

        // start a long-running task on a compute member
        Task.Coordinator<String> coordinator = m_taskExecutorService.orchestrate(
            new LongRunningTask(Duration.ofSeconds(30)))
            .filter(Predicates.role(STORAGE_DISABLED_MEMBER_ROLE))
            .limit(1)
            .retain(Duration.ofMinutes(1))
            .collect(TaskCollectors.lastOf())
            .andThen(myCompletion)
            .subscribe(subscriber)
            .submit();

        String sTaskId = coordinator.getTaskId();

        // verify that the task has started
        Task.Properties properties = coordinator.getProperties();
        Eventually.assertDeferred(() -> properties.get("count"), Matchers.notNullValue());

        // now close all compute members
        cluster.filter(member -> member.getRoleName().equals(STORAGE_DISABLED_MEMBER_ROLE)).close();

        // ensure that we haven't completed or cancelled
        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));

        // now start new compute members (based on storage members)
        cluster.filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE))
            .limit(1)
            .clone(STORAGE_DISABLED_MEMBER_COUNT,
                   DisplayName.of("ComputeServer"),
                   RoleName.of(STORAGE_DISABLED_MEMBER_ROLE),
                   LocalStorage.disabled(),
                   SystemProperty.of(EXTEND_ENABLED_PROPERTY, false));

        // ensure that we are eventually done! (ie: a new compute member picks up the task)
        Task.Coordinator<String> coordinatorInternal = m_taskExecutorService.acquire(sTaskId);
        Eventually.assertDeferred(() -> subscriber.received("DONE"), Matchers.is(true));
        coordinatorInternal.cancel(true);
        Eventually.assertDeferred(() -> isCompletionCalled(myCompletion, sTaskId), Matchers.is(true));
        Eventually.assertDeferred(coordinatorInternal::isCancelled, Matchers.is(true));
        Eventually.assertDeferred(() -> m_taskExecutorService.acquire(sTaskId),
                                  is(nullValue()),
                                  within(75, TimeUnit.SECONDS),
                                  delayedBy(1, TimeUnit.MINUTES)); // don't bother checking until after retain period
        }

    public void shouldFailOverRecoveringTest()
        {
        CoherenceCluster             cluster    = getCoherence().getCluster();
        RecordingSubscriber<Boolean> subscriber = new RecordingSubscriber<>();

        // start a long-running task on a compute member
        Task.Coordinator<Boolean> coordinator = m_taskExecutorService
            .orchestrate(new RecoveringTask(Duration.ofSeconds(5)))
            .filter(Predicates.role(STORAGE_DISABLED_MEMBER_ROLE))
            .limit(1)
            .collect(TaskCollectors.lastOf())
            .subscribe(subscriber)
            .submit();

        // verify that the task has started
        Task.Properties properties = coordinator.getProperties();
        Eventually.assertDeferred(() -> properties.get("count"), Matchers.notNullValue());

        // now close al compute members
        cluster.filter(member -> member.getRoleName().equals(STORAGE_DISABLED_MEMBER_ROLE)).close();

        // ensure that we haven't completed or cancelled
        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));

        // now start new compute members (based on storage members)
        cluster.filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE))
            .limit(1)
            .clone(STORAGE_DISABLED_MEMBER_COUNT,
                   DisplayName.of("ComputeServer"),
                   RoleName.of(STORAGE_DISABLED_MEMBER_ROLE),
                   LocalStorage.disabled(),
                   SystemProperty.of(EXTEND_ENABLED_PROPERTY, false));

        // make sure we can reconnect to the new proxy server and failover the task
        // ensure that we are eventually done! (ie: a new compute member picks up the task)
        Eventually.assertDeferred(() -> subscriber.received(true), Matchers.is(true));
        }

    public void shouldAllowProxyRestartLongRunningTest()
        {
        CoherenceCluster            cluster    = getCoherence().getCluster();
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        // start a long-running task on a compute member
        final String sTaskName = "longRunningTask";
        Task.Coordinator coordinator = m_taskExecutorService
            .orchestrate(new LongRunningTask(Duration.ofSeconds(30)))
            .as(sTaskName)
            .filter(Predicates.role(STORAGE_DISABLED_MEMBER_ROLE))
            .limit(1)
            .collect(TaskCollectors.lastOf())
            .subscribe(subscriber)
            .submit();

        // verify that the task has started
        Task.Properties properties = coordinator.getProperties();
        Eventually.assertDeferred(() -> properties.get("count"), Matchers.notNullValue());

        // ensure that we haven't completed or cancelled
        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));

        properties.put("key1", "value1");

        // now close all proxy members
        cluster.filter(member -> member.getRoleName().equals(PROXY_MEMBER_ROLE)).close();

        Eventually.assertDeferred(cluster::getClusterSize, Matchers.is(getInitialExecutorCount() - 1));

        try
            {
            properties.put("key2", "value2");
            fail("Expect ConnectionException");
            }
        catch (ConnectionException ignored) // this is expected
            {
            }
        catch (IllegalStateException ise)
            {
            // connection not quite closed in time
            if (!ise.getMessage().contains("SafeNamedCache was explicitly"))
                {
                throw ise;
                }
            }

        // now start new proxy members (based on storage members)
        cluster.filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE))
            .limit(1)
            .clone(PROXY_MEMBER_COUNT,
                   DisplayName.of("ProxyServer"),
                   RoleName.of(PROXY_MEMBER_ROLE),
                   LocalStorage.disabled(),
                   SystemProperty.of(EXTEND_ENABLED_PROPERTY, true));
        ensureExecutorProxyAvailable(cluster);

        MatcherAssert.assertThat(properties.get("key1"), Matchers.is("value1"));

        // re-subscribe
        ClusteredTaskCoordinator coordinator2 = (ClusteredTaskCoordinator) m_taskExecutorService.acquire(sTaskName);
        RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();
        coordinator2.subscribe(subscriber2);

        // ensure that both the existing subscriber and new subscriber
        // are done! (ie: we are able to reconnect and receive the result)
        Eventually.assertDeferred(() -> subscriber.received("DONE"), Matchers.is(true));
        Eventually.assertDeferred(() -> subscriber2.received("DONE"), Matchers.is(true));
        }

    public void shouldAllowProxyFailoverLongRunningTest()
        {
        CoherenceCluster            cluster    = getCoherence().getCluster();
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        // start a long-running task on a proxy member
        final String sTaskName = "longRunningTask";
        Task.Coordinator coordinator = m_taskExecutorService
            .orchestrate(new LongRunningTask(Duration.ofSeconds(30)))
            .as(sTaskName)
            .filter(Predicates.role(PROXY_MEMBER_ROLE))
            .limit(1)
            .collect(TaskCollectors.lastOf())
            .subscribe(subscriber)
            .submit();

        // verify that the task has started
        Task.Properties properties = coordinator.getProperties();
        Eventually.assertDeferred(() -> properties.get("count"), Matchers.notNullValue());

        // ensure that we haven't completed or cancelled
        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));

        properties.put("key1", "value1");

        // now restart the proxy member
        cluster.filter(member -> member.getRoleName().equals(PROXY_MEMBER_ROLE)).relaunch();
        ensureExecutorProxyAvailable(cluster);

        // make sure we can reconnect to the new proxy server and failover the task
        MatcherAssert.assertThat(properties.get("key1"), Matchers.is("value1"));

        Eventually.assertDeferred(() -> subscriber.received("DONE"),
                                  Matchers.is(true),
                                  Eventually.within(3, TimeUnit.MINUTES));

        }

    public void shouldAllowClientFailoverLongRunningTest()
        {
        CoherenceCluster            cluster    = getCoherence().getCluster();
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        // start a long-running task on a proxy member
        final String sTaskName = "longRunningTask";
        Task.Coordinator coordinator = m_taskExecutorService.orchestrate(new LongRunningTask(Duration.ofSeconds(30)))
            .as(sTaskName)
            .filter(Predicates.role(STORAGE_DISABLED_MEMBER_ROLE))
            .limit(1)
            .collect(TaskCollectors.lastOf())
            .subscribe(subscriber)
            .submit();

        // verify that the task has started
        Task.Properties properties = coordinator.getProperties();
        Eventually.assertDeferred(() -> properties.get("count"), Matchers.notNullValue());

        // ensure that we haven't completed or cancelled
        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));

        properties.put("key1", "value1");

        // now restart the proxy member and client member
        cluster.filter(member -> member.getRoleName().equals(STORAGE_DISABLED_MEMBER_ROLE)).relaunch();
        cluster.filter(member -> member.getRoleName().equals(PROXY_MEMBER_ROLE)).relaunch();
        ensureConcurrentServiceRunning(cluster);
        ensureExecutorProxyAvailable(cluster);

        // make sure we can reconnect to the new proxy server and failover the task
        MatcherAssert.assertThat(properties.get("key1"), Matchers.is("value1"));

        Eventually.assertDeferred(() -> subscriber.received("DONE"),
                                  Matchers.is(true),
                                  Eventually.within(3, TimeUnit.MINUTES));
        }

    public void shouldWorkWithRollingRestart1()
        {
        CoherenceCluster cluster          = getCoherence().getCluster();
        ExecutorService  executorService1 = Executors.newSingleThreadExecutor();
        ExecutorService  executorService2 = Executors.newSingleThreadExecutor();

        try
            {
            m_taskExecutorService.register(executorService1);
            m_taskExecutorService.register(executorService2);

            RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

            m_taskExecutorService.orchestrate(new RepeatedTask<>("Hello World", 10000))
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue())
                .subscribe(subscriber)
                .submit();

            cluster.filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE)).relaunch();
            ensureConcurrentServiceRunning(cluster);

            Eventually.assertDeferred(subscriber::isSubscribed, Matchers.is(false));
            Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(true));
            Eventually.assertDeferred(() -> subscriber.received("Hello World"), Matchers.is(true));

            Eventually.assertDeferred(subscriber::isSubscribed, Matchers.is(false));
            Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(true));
            Eventually.assertDeferred(() -> subscriber.received("Hello World"), Matchers.is(true));
            }
        finally
            {
            executorService1.shutdownNow();
            executorService2.shutdownNow();

            m_taskExecutorService.deregister(executorService1);
            m_taskExecutorService.deregister(executorService2);
            }
        }

    public void shouldWorkWithRollingRestart2()
        {
        CoherenceCluster cluster              = getCoherence().getCluster();
        final int         CLUSTER_SIZE         = cluster.getClusterSize();
        NamedCache       cacheExecutorService = Caches.executors(getCacheService());

        ExecutorService executorService1 = Executors.newSingleThreadExecutor();
        ExecutorService executorService2 = Executors.newSingleThreadExecutor();
        ExecutorService executorService3 = Executors.newSingleThreadExecutor();

        try
            {
            m_taskExecutorService.register(executorService1);
            m_taskExecutorService.register(executorService2);

            Eventually.assertDeferred(cacheExecutorService::size, is(getInitialExecutorCount() + 2));

            RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();

            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.count())
                .until(Predicates.is(getInitialExecutorCount() + 3))
                .subscribe(subscriber)
                .submit();

            cluster.filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE)).relaunch();
            ensureConcurrentServiceRunning(cluster);

            Eventually.assertDeferred(cluster::getClusterSize, is(CLUSTER_SIZE));

            // refresh in case connected member was stopped
            NamedCache cacheExecutorServiceInner = Caches.executors(getCacheService());

            // ensure the executor information is eventually cleaned up
            Eventually.assertDeferred(cacheExecutorServiceInner::size,
                                      is(getInitialExecutorCount() + 2),
                                      within(ClusteredExecutorInfo.LEASE_DURATION_MS + 8000, TimeUnit.MILLISECONDS));

            m_taskExecutorService.register(executorService3);

            Eventually.assertDeferred(cacheExecutorService::size, is(getInitialExecutorCount() + 3));

            Eventually.assertDeferred(() -> subscriber.received(getInitialExecutorCount() + 3), Matchers.is(true));
            }
        finally
            {
            executorService1.shutdown();
            executorService2.shutdown();
            executorService3.shutdown();

            m_taskExecutorService.deregister(executorService1);
            m_taskExecutorService.deregister(executorService2);
            m_taskExecutorService.deregister(executorService3);
            }
        }

    public void shouldRunMultiLongRunningCronTasks()
        {
        RecordingSubscriber<String> subscriber1 = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator1 =
            m_taskExecutorService
                .orchestrate(CronTask.of(new LongRunningTask(Duration.ofSeconds(35), 1), "* * * * *"))
                .filter(Predicates.role(STORAGE_DISABLED_MEMBER_ROLE))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber1)
                .submit();

        RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator2 =
            m_taskExecutorService
                .orchestrate(CronTask.of(new LongRunningTask(Duration.ofSeconds(15), 2), "* * * * *", false))
                .filter(Predicates.role(STORAGE_DISABLED_MEMBER_ROLE))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber2)
                .submit();

        RecordingSubscriber<String> subscriber3 = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator3 =
            m_taskExecutorService
                .orchestrate(CronTask.of(new LongRunningTask(Duration.ofSeconds(20), 3), "* * * * *"))
                .filter(Predicates.role(STORAGE_DISABLED_MEMBER_ROLE))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber3)
                .submit();

        // check after 70 seconds (cron yield of 1 min plus 10 seconds into Task execution) - none should have completed yet
        Eventually.assertDeferred(() -> subscriber1.received("DONE"), is(false),
                              within(0, TimeUnit.SECONDS), // check only once after initial delay
                              delayedBy(70, TimeUnit.SECONDS));
        MatcherAssert.assertThat(subscriber2.received("DONE"), is(false));
        MatcherAssert.assertThat(subscriber3.received("DONE"), is(false));

        // ensure that each Task has completed once

        // Task 1 may take over 2 minutes to complete if it gets put behind the other two:
        //   1-minute cron delay + 35 seconds for other two + 35 seconds for task 1
        //   minus 70-second delay above
        Eventually.assertDeferred(() -> subscriber1.received("DONE"),
                              is(true),
                              within(2, TimeUnit.MINUTES));

        Eventually.assertDeferred(() -> subscriber2.received("DONE"), Matchers.is(true), within(1, TimeUnit.MINUTES));
        Eventually.assertDeferred(() -> subscriber3.received("DONE"), Matchers.is(true), within(1, TimeUnit.MINUTES));

        MatcherAssert.assertThat(coordinator1.cancel(true), Matchers.is(true));
        MatcherAssert.assertThat(coordinator2.cancel(true), Matchers.is(true));
        MatcherAssert.assertThat(coordinator3.cancel(true), Matchers.is(true));

        Eventually.assertDeferred(subscriber3::isSubscribed, Matchers.is(false));
        MatcherAssert.assertThat(coordinator1.isCancelled(), Matchers.is(true));
        MatcherAssert.assertThat(coordinator2.isCancelled(), Matchers.is(true));
        MatcherAssert.assertThat(coordinator3.isCancelled(), Matchers.is(true));
        }

    public void shouldHandleRollingRestartWithCronTask()
        {
        RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<Integer> coordinator =
            m_taskExecutorService
                .orchestrate(CronTask.of(new CountingTask(), "* * * * *"))
                .filter(Predicates.role(STORAGE_ENABLED_MEMBER_ROLE))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber).submit();

        // Verify that the task is executed at least 1 time in the 2 minutes interval
        Repetitively.assertThat(invoking(subscriber).isCompleted(), Matchers.is(false), Timeout.of(2, TimeUnit.MINUTES));
        MatcherAssert.assertThat(subscriber.received(1), Matchers.is(true));

        Logger.info("Restarting storage");
        getCoherence().getCluster().filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE)).relaunch();
        ensureConcurrentServiceRunning(getCoherence().getCluster());
        Logger.info("Storage available");

        // Verify that the task is recovered from rolling restart and is executed at lease 1 time in the 2 minutes interval
        int cReceived = subscriber.size() + 1;

        Repetitively.assertThat(invoking(subscriber).isCompleted(), Matchers.is(false), Timeout.of(2, TimeUnit.MINUTES));
        MatcherAssert.assertThat(subscriber.getLast(), Matchers.greaterThanOrEqualTo(cReceived));

        MatcherAssert.assertThat(coordinator.cancel(true), Matchers.is(true));

        Eventually.assertDeferred(subscriber::isSubscribed, Matchers.is(false));
        Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(false));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), Matchers.is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), Matchers.is(true));
        MatcherAssert.assertThat(coordinator.isDone(), Matchers.is(true));
        }

    public void shouldHandleRemoveServerWithCronTask()
        {
        RecordingSubscriber<Integer> subscriber1 = new RecordingSubscriber<>();
        RecordingSubscriber<Integer> subscriber2 = new RecordingSubscriber<>();
        RecordingSubscriber<Integer> subscriber3 = new RecordingSubscriber<>();

        Task.Coordinator<Integer> coordinator1 =
            m_taskExecutorService
                .orchestrate(CronTask.of(new CountingTask(), "* * * * *"))
                .filter(Predicates.role(STORAGE_ENABLED_MEMBER_ROLE))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber1).submit();

        Task.Coordinator<Integer> coordinator2 =
            m_taskExecutorService
                .orchestrate(CronTask.of(new CountingTask(), "* * * * *"))
                .filter(Predicates.role(STORAGE_ENABLED_MEMBER_ROLE))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber2).submit();

        Task.Coordinator<Integer> coordinator3 =
            m_taskExecutorService
                .orchestrate(CronTask.of(new CountingTask(), "* * * * *"))
                .filter(Predicates.role(STORAGE_ENABLED_MEMBER_ROLE))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber3).submit();

        // Verify that the task is executed at least 1 time in the 2 minutes interval
        Repetitively.assertThat(invoking(subscriber1).isCompleted(), Matchers.is(false), Timeout.of(2, TimeUnit.MINUTES));
        MatcherAssert.assertThat(subscriber1.received(1), Matchers.is(true));
        MatcherAssert.assertThat(subscriber2.received(1), Matchers.is(true));
        MatcherAssert.assertThat(subscriber3.received(1), Matchers.is(true));

        CoherenceCluster cluster = getCoherence().getCluster();
        cluster.filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE)).limit(1).close(RuntimeHalt.withExitCode(-1));
        ApplicationStream<CoherenceClusterMember> server = cluster.filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE));

        cluster.filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE)).clone(2);
        server.close(SystemExit.withExitCode(-1));

        Logger.info("Storage member terminated ...");

        // Verify that the task is recovered from failover and is executed at lease 1 time in the 2 minutes interval
        int cReceived1 = subscriber1.size();
        Logger.info(String.format("Subscriber1 [%s] size=%s", coordinator1.getTaskId(), cReceived1));

        int cReceived2 = subscriber2.size();
        Logger.info(String.format("Subscriber2 [%s] size=%s", coordinator2.getTaskId(), cReceived2));

        int cReceived3 = subscriber3.size();
        Logger.info(String.format("Subscriber3 [%s] size=%s", coordinator3.getTaskId(), cReceived3));

        Logger.info("Begin wait for two minutes to ensure task doesn't complete");
        Repetitively.assertThat(invoking(subscriber1).isCompleted(), Matchers.is(false), Timeout.of(2, TimeUnit.MINUTES));

        Logger.info("Checking subscriber1 ...");
        MatcherAssert.assertThat(subscriber1.size(), Matchers.greaterThan(cReceived1));

        Logger.info("Checking subscriber2 ...");
        MatcherAssert.assertThat(subscriber2.size(), Matchers.greaterThan(cReceived2));

        Logger.info("Checking subscriber3 ...");
        MatcherAssert.assertThat(subscriber3.size(), Matchers.greaterThan(cReceived3));

        MatcherAssert.assertThat(coordinator1.cancel(true), Matchers.is(true));
        MatcherAssert.assertThat(coordinator2.cancel(true), Matchers.is(true));
        MatcherAssert.assertThat(coordinator3.cancel(true), Matchers.is(true));

        Eventually.assertDeferred(subscriber1::isSubscribed, Matchers.is(false));
        Eventually.assertDeferred(subscriber1::isCompleted, Matchers.is(false));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator1).hasSubscribers(), Matchers.is(false));
        MatcherAssert.assertThat(coordinator1.isCancelled(), Matchers.is(true));
        MatcherAssert.assertThat(coordinator1.isDone(), Matchers.is(true));
        }

    public void shouldHandleFailoverWithCronTask()
        {
        RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<Integer> coordinator =
            m_taskExecutorService
                .orchestrate(CronTask.of(new CountingTask(), "* * * * *"))
                .filter(Predicates.role(STORAGE_DISABLED_MEMBER_ROLE))
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber).submit();

        // Verify that the task is executed at least 1 time in the 2 minutes interval
        Repetitively.assertThat(invoking(subscriber).isCompleted(), Matchers.is(false), Timeout.of(2, TimeUnit.MINUTES));
        MatcherAssert.assertThat(subscriber.received(1), Matchers.is(true));

        CoherenceCluster cluster = getCoherence().getCluster();
        cluster.filter(member -> member.getRoleName().equals(STORAGE_DISABLED_MEMBER_ROLE)).close(SystemExit.withExitCode(0));

        // now start new compute members (based on storage members)
        cluster.filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE))
            .limit(1)
            .clone(STORAGE_DISABLED_MEMBER_COUNT,
                   DisplayName.of("ComputeServer"),
                   RoleName.of(STORAGE_DISABLED_MEMBER_ROLE),
                   LocalStorage.disabled(),
                   SystemProperty.of(EXTEND_ENABLED_PROPERTY, false));

        // Verify that the task is recovered from rolling restart and is executed at lease 1 time in the 2 minutes interval
        int cReceived = subscriber.size();

        Repetitively.assertThat(invoking(subscriber).isCompleted(), Matchers.is(false), Timeout.of(2, TimeUnit.MINUTES));
        MatcherAssert.assertThat(subscriber.size(), Matchers.greaterThan(cReceived));

        MatcherAssert.assertThat(coordinator.cancel(true), Matchers.is(true));

        Eventually.assertDeferred(subscriber::isSubscribed, Matchers.is(false));
        Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(false));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), Matchers.is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), Matchers.is(true));
        MatcherAssert.assertThat(coordinator.isDone(), Matchers.is(true));
        }

    public void shouldNotExpireRunningExecutors()
        {
        ExecutorService executorService1 = Executors.newSingleThreadExecutor();
        ExecutorService executorService2 = Executors.newSingleThreadExecutor();

        try
            {
            final int        EXPECTED_EXECUTORS   = getInitialExecutorCount() + 2;
            NamedCache      cacheExecutorService = Caches.executors(getCacheService());

            Eventually.assertDeferred(cacheExecutorService::size, is(getInitialExecutorCount()), Timeout.of(5, TimeUnit.SECONDS));

            m_taskExecutorService.register(executorService1);
            m_taskExecutorService.register(executorService2);

            Eventually.assertDeferred(cacheExecutorService::size, is(EXPECTED_EXECUTORS));

            // make sure that none of the leases expire for lease duration + 15 seconds
            Repetitively.assertThat(invoking(cacheExecutorService).size(),
                                    is(EXPECTED_EXECUTORS),
                                    Timeout.of(ClusteredExecutorInfo.LEASE_DURATION_MS + 15000, TimeUnit.MILLISECONDS));

            RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();

            // verify that the executors can all still execute tasks
            Task.Coordinator<Integer> coordinator =
                m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                    .collect(TaskCollectors.count())
                    .until(Predicates.is(EXPECTED_EXECUTORS))
                    .subscribe(subscriber).submit();

            Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(true), Eventually.within(3, TimeUnit.MINUTES));
            MatcherAssert.assertThat(subscriber.isSubscribed(), Matchers.is(false));
            MatcherAssert.assertThat(subscriber.received(EXPECTED_EXECUTORS), Matchers.is(true));

            MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), Matchers.is(false));
            MatcherAssert.assertThat(coordinator.isCancelled(), Matchers.is(false));
            MatcherAssert.assertThat(coordinator.isDone(), Matchers.is(true));
            }
        finally
            {
            executorService1.shutdown();
            executorService2.shutdown();

            m_taskExecutorService.deregister(executorService1);
            m_taskExecutorService.deregister(executorService2);
            }
        }

    public void shouldNotExpireExecutorsWithRollingRestart()
        {
        ExecutorService executorService1 = Executors.newSingleThreadExecutor();
        ExecutorService executorService2 = Executors.newSingleThreadExecutor();

        try
            {
            final int        EXPECTED_EXECUTORS    = getInitialExecutorCount() + 2;
            CoherenceCluster cluster              = getCoherence().getCluster();
            NamedCache       cacheExecutorService = Caches.executors(getCacheService());

            m_taskExecutorService.register(executorService1);
            m_taskExecutorService.register(executorService2);

            Eventually.assertDeferred(cacheExecutorService::size, is(EXPECTED_EXECUTORS));
            cluster.filter(member -> member.getRoleName().equals(STORAGE_ENABLED_MEMBER_ROLE)).relaunch();
            ensureConcurrentServiceRunning(cluster);

            cacheExecutorService = Caches.executors(getCacheService());

            // ensure the executor information is eventually cleaned up
            Eventually.assertDeferred(cacheExecutorService::size,
                                  is(EXPECTED_EXECUTORS),
                                  within(ClusteredExecutorInfo.LEASE_DURATION_MS + 15000, TimeUnit.MILLISECONDS));

            // verify that the executors can all still execute tasks
            RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();
            Task.Coordinator<Integer> coordinator =
                m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                    .collect(TaskCollectors.count())
                    .until(Predicates.is(EXPECTED_EXECUTORS))
                    .subscribe(subscriber)
                    .submit();

            Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(true));
            MatcherAssert.assertThat(subscriber.isSubscribed(), Matchers.is(false));
            MatcherAssert.assertThat(subscriber.received(EXPECTED_EXECUTORS), Matchers.is(true));

            MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), Matchers.is(false));
            MatcherAssert.assertThat(coordinator.isCancelled(), Matchers.is(false));
            MatcherAssert.assertThat(coordinator.isDone(), Matchers.is(true));
            }
        finally
            {
            executorService1.shutdown();
            executorService2.shutdown();

            m_taskExecutorService.deregister(executorService1);
            m_taskExecutorService.deregister(executorService2);
            }
        }

    public void shouldSetRoleFromMember()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
            .filter(Predicates.role(STORAGE_ENABLED_MEMBER_ROLE))
            .limit(1)
            .collect(TaskCollectors.lastOf())
            .subscribe(subscriber)
            .until(Predicates.anything())
            .submit();

        Eventually.assertDeferred(() -> subscriber.received("Hello World"), Matchers.is(true));
        Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(true));
        MatcherAssert.assertThat(subscriber.isError(), Matchers.is(false));
        }

    public void shouldSetStorageOption()
        {
        RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();

        // start with storage enabled members, then add another during the test
        int cExpectedExecutorCount1 = STORAGE_ENABLED_MEMBER_COUNT + 1;

        Task.Coordinator<Integer> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .filter(Predicates.has(Storage.enabled()))
                .collect(TaskCollectors.count())
                .until(Predicates.is(cExpectedExecutorCount1))
                .subscribe(subscriber)
                .submit();

        // not yet completed - need another storage executor
        Eventually.assertDeferred(() -> subscriber.received(cExpectedExecutorCount1 - 1), Matchers.is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), Matchers.is(true));
        Repetitively.assertThat(invoking(subscriber).isCompleted(), Matchers.is(false), Timeout.of(10, TimeUnit.SECONDS));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), Matchers.is(true));
        MatcherAssert.assertThat(coordinator.isCancelled(), Matchers.is(false));
        MatcherAssert.assertThat(coordinator.isDone(), Matchers.is(false));

        CoherenceCluster cluster = getCoherence().getCluster();

        // add a new storage member
        cluster.filter(m -> m.getRoleName().contains(STORAGE_ENABLED_MEMBER_ROLE)).limit(1).clone(1);

        // now that we have another storage member, should complete
        Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), Matchers.is(false));
        MatcherAssert.assertThat(subscriber.received(cExpectedExecutorCount1), Matchers.is(true));
        MatcherAssert.assertThat(subscriber.getLast(), Matchers.is(cExpectedExecutorCount1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), Matchers.is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), Matchers.is(false));
        MatcherAssert.assertThat(coordinator.isDone(), Matchers.is(true));

        // add an executor that is tagged with storage disabled

        // test that there is one and only one storage disabled node
        int cExpectedExecutorCount = STORAGE_DISABLED_MEMBER_COUNT + PROXY_MEMBER_COUNT + 1;    // one new executor to add
        RecordingSubscriber<Integer> subscriber2 = new RecordingSubscriber<>();

        coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .filter(Predicates.has(Storage.disabled()))
                .collect(TaskCollectors.count())
                .until(Predicates.is(cExpectedExecutorCount))
                .subscribe(subscriber2).submit();

        // not yet completed - need another storage disabled executor
        Eventually.assertDeferred(subscriber2::isSubscribed, Matchers.is(true));
        MatcherAssert.assertThat(subscriber2.isCompleted(), Matchers.is(false));
        Eventually.assertDeferred(subscriber2::getLast, Matchers.is(nullValue()));

        // ensure that no additional results come in
        Repetitively.assertThat(invoking(subscriber2).received(cExpectedExecutorCount), is(false),
                                Timeout.of(5, TimeUnit.SECONDS));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), Matchers.is(true));
        MatcherAssert.assertThat(coordinator.isCancelled(), Matchers.is(false));
        MatcherAssert.assertThat(coordinator.isDone(), Matchers.is(false));

        // add a new executor flagged as storage disabled
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try
            {
            m_taskExecutorService.register(executorService, Storage.disabled());

            // now that we have another storage disabled flagged executor, should complete
            Eventually.assertDeferred(subscriber2::isCompleted, Matchers.is(true));
            MatcherAssert.assertThat(subscriber2.isSubscribed(), Matchers.is(false));
            MatcherAssert.assertThat(subscriber2.received(cExpectedExecutorCount), Matchers.is(true));
            MatcherAssert.assertThat(subscriber2.getLast(), Matchers.is(cExpectedExecutorCount));

            MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), Matchers.is(false));
            MatcherAssert.assertThat(coordinator.isCancelled(), Matchers.is(false));
            MatcherAssert.assertThat(coordinator.isDone(), Matchers.is(true));
            }
        finally
            {
            m_taskExecutorService.deregister(executorService);
            executorService.shutdown();
            }
        }

    /**
     * ExecutorService throws RejectedExecutionException when it is shutting down. Test that we handle the situation.
     */
    public void shouldUnregisterExecutorAfterReject()
        {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try
            {
            String                   sId   = m_taskExecutorService.register(executorService, Role.of("foo")).getId();
            ClusteredExecutorService clsES = m_taskExecutorService;

            NamedCache<String, ClusteredExecutorInfo> executorCache = Caches.executors(clsES.getCacheService());

            // wait for the ES to be registered and in the RUNNING state before setting to REJECTING
            Eventually.assertDeferred(() -> executorCache.get(sId).getState(), is(RUNNING));

            // set the executor state to be rejecting
            executorCache.invoke(sId, new ClusteredExecutorInfo.SetStateProcessor(REJECTING));

            // now submit a task for the executor that is rejecting
            RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

            m_taskExecutorService.orchestrate(new ValueTask<>("Reject State"))
                .filter(Predicates.role("foo"))
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue())
                .subscribe(subscriber)
                .submit();

            // ensure that it's never completed (executed)
            Repetitively.assertThat(invoking(subscriber).isCompleted(),
                                    Matchers.is(false),
                                    within(10, TimeUnit.SECONDS));

            // now set the executor state to be running
            executorCache.invoke(sId, new ClusteredExecutorInfo.SetStateProcessor(RUNNING));
            Base.sleep(2000);

            // now ensure that the executor works normally
            RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();

            m_taskExecutorService.orchestrate(new ValueTask<>("Not rejecting"))
                .filter(Predicates.role("foo"))
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue())
                .subscribe(subscriber2)
                .submit();

            Eventually.assertDeferred(() -> subscriber2.received("Not rejecting"), is(true));
            Eventually.assertDeferred(subscriber2::isCompleted, Matchers.is(true));

            RecordingSubscriber<String> subscriber3 = new RecordingSubscriber<>();

            m_taskExecutorService.orchestrate(new RepeatedTask<>("RepeatedTask", 50000))
                .collect(TaskCollectors.firstOf())
                .subscribe(subscriber3)
                .submit();

            Eventually.assertDeferred(() -> subscriber3.received("RepeatedTask"), is(true));

            // now shutdown the executor service (this will cause it to reject locally)
            executorService.shutdown();
            Eventually.assertThat(executorService.isShutdown(), is(true));

            // submit a task that should fail due to rejection
            RecordingSubscriber<String> subscriber4 = new RecordingSubscriber<>();

            m_taskExecutorService.orchestrate(new ValueTask<>("Local reject"))
                .filter(Predicates.role("foo"))
                .collect(TaskCollectors.firstOf())
                .subscribe(subscriber4)
                .submit();

            // ensure the subscriber receives an error
            Eventually.assertDeferred(subscriber4::isSubscribed, is(false));
            MatcherAssert.assertThat(subscriber4.getThrowable(), notNullValue());
            MatcherAssert.assertThat(subscriber4.isCompleted(), is(false));
            }
        finally
            {
            executorService.shutdownNow();
            m_taskExecutorService.deregister(executorService);
            }
        }

    /**
     * A terminated ExecutorService should be automatically de-registered by the lease checker.
     */
    public void shouldDeregisterTerminatedExecutorService()
        {
        int             cExpectedExecutors = getInitialExecutorCount();
        ExecutorService terminatingES      = Executors.newSingleThreadExecutor();

        try
            {
            // verify that there are getInitialExecutorCount() Executors available and that they are in the RUNNING state
            NamedCache<String, ClusteredExecutorInfo> executorsCache = Caches.executors(getCacheService());

            Eventually.assertDeferred(executorsCache::size, is(cExpectedExecutors));

            m_taskExecutorService.register(terminatingES);
            ++cExpectedExecutors;

            Eventually.assertDeferred(executorsCache::size, is(cExpectedExecutors));

            // verify that all executors are in the RUNNING state
            for (String key : executorsCache.keySet())
                {
                Eventually.assertDeferred(() -> (this).getExecutorServiceInfo(executorsCache, key).getState(),
                                      Is.is(TaskExecutorService.ExecutorInfo.State.RUNNING));
                }

            terminatingES.shutdown();
            --cExpectedExecutors;

            // verify that it has been removed
            Eventually.assertDeferred(executorsCache::size, is(cExpectedExecutors));

            // can't check whether the executor has been de-registered as its removal from the map of
            // locally registered Executors may lag behind its removal from the Executors cache
            }
        finally
            {
            terminatingES.shutdown();
            m_taskExecutorService.deregister(terminatingES);
            }
        }

    public void shouldRunMultipleTasks()
        {
        final int                      TASK_SIZE          = 50;
        final int                      cExpectedExecutors = getInitialExecutorCount();
        RecordingSubscriber<Integer>[] subscribers        = new RecordingSubscriber[TASK_SIZE];
        Task.Coordinator<Integer>[]    coordinators       = new Task.Coordinator[TASK_SIZE];

        for (int i = 0; i < TASK_SIZE; i++)
            {
            subscribers[i]  = new RecordingSubscriber<>();
            coordinators[i] =
                m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                    .collect(TaskCollectors.count())
                    .until(Predicates.is(cExpectedExecutors))
                    .subscribe(subscribers[i])
                    .submit();
            }

        for (int i = 0; i < TASK_SIZE; i++)
            {
            String sFailureTask = "Task[" + i + "]: " + coordinators[i].getTaskId();
            int nIndex = i;
            Eventually.assertDeferred(sFailureTask, () -> subscribers[nIndex].isCompleted(), Matchers.is(true));
            MatcherAssert.assertThat(sFailureTask, subscribers[i].isSubscribed(), Matchers.is(false));
            MatcherAssert.assertThat(sFailureTask, subscribers[i].received(cExpectedExecutors), Matchers.is(true));

            MatcherAssert.assertThat(sFailureTask, ((AbstractTaskCoordinator) coordinators[i]).hasSubscribers(), Matchers.is(false));
            MatcherAssert.assertThat(sFailureTask, coordinators[i].isCancelled(), Matchers.is(false));
            MatcherAssert.assertThat(sFailureTask, coordinators[i].isDone(), Matchers.is(true));
            }

        NamedCache<String, ?> executorsCache = Caches.executors(getCacheService());

        // verify the Executor's JMX stats
        for (String key : executorsCache.keySet())
            {
            Deferred<ExecutorMBean> deferredExecutorMBean = getDeferredExecutorMBean(key);
            Eventually.assertDeferred(key, () -> deferredExecutorMBean.get().getTasksCompletedCount(), Matchers.is((long) TASK_SIZE));

            ExecutorMBean executorMBean = ensure(deferredExecutorMBean);
            MatcherAssert.assertThat(key, executorMBean.getTasksRejectedCount(), Matchers.is(0L));
            MatcherAssert.assertThat(key, executorMBean.getTasksInProgressCount(), Matchers.is(0L));
            MatcherAssert.assertThat(key, executorMBean.getState(), Matchers.is("RUNNING"));
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure the specified service has the expected member count.
     *
     *  @param cluster  the {@link CoherenceCluster} to query
     */
    public static void ensureConcurrentServiceRunning(CoherenceCluster cluster)
        {
        cluster.stream()
                .forEach(member ->
                                 Eventually.assertDeferred(() -> member.isServiceRunning(CONCURRENT_SERVICE_NAME), is(true)));
        }

    /**
     * Ensure the {@code $SYS:ConcurrentProxy} is running.
     *
     * @param cluster  the {@link CoherenceCluster} to query
     */
    public static void ensureExecutorProxyAvailable(CoherenceCluster cluster)
        {
        cluster.stream()
                .filter(member -> PROXY_MEMBER_ROLE.equals(member.getRoleName()))
                .forEach(member ->
                             Eventually.assertDeferred(() -> member.isServiceRunning(CONCURRENT_PROXY_SERVICE_NAME), is(true)));
        }

    public CacheService getCacheService()
        {
        return m_taskExecutorService == null
                ? null : m_taskExecutorService.getCacheService();
        }

    /**
     * Get a deferred ExecutorMBean.
     *
     * @return the deferred ExecutorMBean proxy
     */
    public Deferred<ExecutorMBean> getDeferredExecutorMBean(String key)
        {
        try
            {
            //noinspection OptionalGetWithoutIsPresent
            JmxFeature jmxFeature = getCoherence().getCluster().findFirst().get().get(JmxFeature.class);

            if (jmxFeature != null)
                {
                ObjectName          name     = new ObjectName("Coherence:" + ExecutorMBean.EXECUTOR_TYPE
                                                              + ExecutorMBean.EXECUTOR_NAME + key + ",*");
                Set<ObjectInstance> setMBeans = jmxFeature.queryMBeans(name, null);

                if (setMBeans != null)
                    {
                    name = setMBeans.iterator().next().getObjectName();

                    return jmxFeature.getDeferredMBeanProxy(name, ExecutorMBean.class);
                    }
                }

            return null;
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Obtains the {@link ClusteredExecutorInfo} for the specified {@link Executor}
     * from the {@link TaskExecutorService.ExecutorInfo} {@link NamedCache}.
     *
     * @param executorInfoCache  the {@link TaskExecutorService.ExecutorInfo} {@link NamedCache}
     * @param executorId         the {@link ExecutorService} identity
     *
     * @return the {@link ClusteredExecutorInfo} or <code>null</code> if it doesn't exist
     */
    public ClusteredExecutorInfo getExecutorServiceInfo(NamedCache<String, ClusteredExecutorInfo> executorInfoCache,
                                                        String executorId)
        {
        return executorInfoCache.get(executorId);
        }

    @Override
    protected boolean isCompletionCalled(Task.CompletionRunnable completionRunnable, String taskId)
        {
        return ((MyCompletion) completionRunnable).isLogTaskCalled();
        }

    @Override
    protected ClusteredExecutorService createExecutorService()
        {
        return new ClusteredExecutorService(m_cacheFactory);
        }

    @Override
    protected int getInitialExecutorCount()
        {
        return STORAGE_ENABLED_MEMBER_COUNT + STORAGE_DISABLED_MEMBER_COUNT + PROXY_MEMBER_COUNT;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Per-cluster test directory counter.
     */
    protected static final AtomicInteger COUNTER = new AtomicInteger();

    /**
     * The number of storage enabled members in the {@link CoherenceCluster}.
     */
    protected static final int STORAGE_ENABLED_MEMBER_COUNT = 2;

    /**
     * The role name of storage enabled members in the {@link CoherenceCluster}.
     */
    protected static final String STORAGE_ENABLED_MEMBER_ROLE = "storage";

    /**
     * The number of storage disabled members in the {@link CoherenceCluster}.
     */
    protected static final int STORAGE_DISABLED_MEMBER_COUNT = 1;

    /**
     * The role name of storage disabled members in the {@link CoherenceCluster}.
     */
    protected static final String STORAGE_DISABLED_MEMBER_ROLE = "compute";

    /**
     * The number of proxy servers members in the {@link CoherenceCluster}.
     */
    protected static final int PROXY_MEMBER_COUNT = 1;

    /**
     * The role name of proxy members in the {@link CoherenceCluster}.
     */
    protected static final String PROXY_MEMBER_ROLE = "proxy";

    /**
     * System property for the executor proxy address.
     */
    protected static final String EXTEND_ADDRESS_PROPERTY = "coherence.concurrent.extend.address";

    /**
     * System property for the executor proxy port.
     */
    protected static final String EXTEND_PORT_PROPERTY = "coherence.concurrent.extend.port";

    /**
     * System property to enable the Executor service proxy.
     */
    protected static final String EXTEND_ENABLED_PROPERTY = "coherence.concurrent.extend.enabled";

    /**
     * System property to configure the {@link Serializer} used by the concurrent module.
     */
    protected static final String SERIALIZER_PROPERTY = "coherence.concurrent.serializer";

    /**
     * System property to configure executor logging.
     */
    protected static final String EXECUTOR_LOGGING_PROPERTY = "coherence.executor.trace.logging";

    /**
     * System property to configure extend message debugging.
     */
    protected static final String EXTEND_DEBUG_PROPERTY = "coherence.messaging.debug";

    /**
     * Proxy service name.
     */
    protected static final String CONCURRENT_PROXY_SERVICE_NAME = "$SYS:ConcurrentProxy";

    /**
     * Concurrent distributed cache service name.
     */
    protected static final String CONCURRENT_SERVICE_NAME = "$SYS:Concurrent";

    /**
     * The default proxy port.
     */
    protected static final String EXTEND_PORT = "9099";

    /**
     * The default proxy address.
     */
    protected static final String EXTEND_HOST = LocalPlatform.get().getLoopbackAddress().getHostAddress();

    // ----- data members ---------------------------------------------------

    /**
     * The extend client cache configure file
     */
    protected String m_extendConfig;

    /**
     * Extend cache factory.
     */
    protected ConfigurableCacheFactory m_cacheFactory;

    /**
     * JUnit TestWatcher.
     */
    @Rule
    public final Watcher f_watcher = new Watcher();
    }
