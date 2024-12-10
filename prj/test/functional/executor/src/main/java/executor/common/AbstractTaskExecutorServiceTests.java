/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor.common;

import com.oracle.bedrock.deferred.DeferredHelper;

import com.oracle.bedrock.options.Timeout;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.deferred.Repetitively;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.AbstractTaskCoordinator;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;
import com.oracle.coherence.concurrent.executor.Task;
import com.oracle.coherence.concurrent.executor.TaskCollectors;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;

import com.oracle.coherence.concurrent.executor.options.Debugging;
import com.oracle.coherence.concurrent.executor.options.Role;

import com.oracle.coherence.concurrent.executor.subscribers.RecordingSubscriber;

import com.oracle.coherence.concurrent.executor.tasks.CronTask;
import com.oracle.coherence.concurrent.executor.tasks.ValueTask;

import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.net.CacheService;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.function.Remote;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.time.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import org.hamcrest.MatcherAssert;

import org.hamcrest.Matchers;

import static com.oracle.bedrock.deferred.DeferredHelper.delayedBy;
import static com.oracle.bedrock.deferred.DeferredHelper.future;
import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.valueOf;
import static com.oracle.bedrock.deferred.DeferredHelper.within;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import static org.hamcrest.core.CombinableMatcher.either;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Common {@link TaskExecutorService} functional tests.
 *
 * @author phf
 * @author bo
 * @since 21.12
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractTaskExecutorServiceTests
    {
    // TODO - add tests for:
    // 1. Add subscriber to coordinator obtained via acquire()

    // ----- AbstractExecutorServiceTests interface -------------------------

    /**
     * Return the number of {@link Executor}s that were registered with the
     * {@link TaskExecutorService} prior to the start of the test.
     *
     * @return the number of {@link Executor}s that were registered with the
     *         {@link TaskExecutorService} prior to the start of the test.
     */
    protected abstract int getInitialExecutorCount();

    /**
     * Spin up a new {@link TaskExecutorService}.  This is used for ExecutorService lifecycle tests
     * such as shutdown tests.
     *
     * @return a new {@link TaskExecutorService}
     */
    protected abstract ClusteredExecutorService createExecutorService();

    /**
     * Return the {@link CacheService} used by the executor service.
     *
     * @return the {@link CacheService} used by the executor service
     */
    protected abstract CacheService getCacheService();

    /**
     * Obtain the {@link CoherenceCluster} under test.
     *
     * @return the {@link CoherenceCluster} under test.
     */
    public abstract CoherenceCluster getCluster();

    // ----- test methods ---------------------------------------------------

    public void shouldCreateExecutorService()
        {
        assertThat(m_taskExecutorService, is(notNullValue()));
        assertThat(m_taskExecutorService.isShutdown(), is(false));
        }

    public void shouldExecuteAndCompleteTask()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue())
                .subscribe(subscriber)
                .submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));

        // test: subscribe after completed

        subscriber = new RecordingSubscriber<>();

        coordinator.subscribe(subscriber);

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        }

    public void shouldExecuteAndCompleteTaskWithNoCollector()
        {
        RecordingSubscriber<String> subscriber         = new RecordingSubscriber<>();
        int                         cExpectedExecutors = getInitialExecutorCount();

        // execute and complete on all Executors
        Task.Coordinator<String> coordinator =
                m_taskExecutorService.orchestrate(new ValueTask<>("Hello World", 3000))
                        .subscribe(subscriber)
                        .submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(cExpectedExecutors));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));

        // check subscribe after completed
        subscriber = new RecordingSubscriber<>();

        coordinator.subscribe(subscriber);

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));

        // execute and complete on just one Executor
        subscriber = new RecordingSubscriber<>();

        m_taskExecutorService.orchestrate(new ValueTask<>("Hello World", 3000))
                .limit(1)
                .subscribe(subscriber)
                .submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        // execute and complete calling CES.submit()
        subscriber  = new RecordingSubscriber<>();
        coordinator = m_taskExecutorService.submit(new ValueTask<>("Hello World", 3000));
        coordinator.subscribe(subscriber);

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
        }

    /**
     * Ensure the ExecutorService can process thousands of results for a single task without losing any results or
     * hanging.
     */
    public void shouldExecuteAndCompleteTaskWithMultipleResults()
        {
        final int        NUM_RESULTS = 10000;
        ExecutorService localES     = Executors.newSingleThreadExecutor();

        try
            {
            RecordingSubscriber<Integer> subscriber      = new RecordingSubscriber<>();
            final int                     cExpectedResult = NUM_RESULTS * (getInitialExecutorCount() + 1);

            m_taskExecutorService.register(localES);
            m_taskExecutorService.orchestrate(new MultipleResultsTask(NUM_RESULTS))
                .subscribe(subscriber)
                .submit();

            // since it may take more than the Eventually assert time period to reach the
            // expectedResult, check instead that we are making progress
            for (int currentResult = subscriber.size(); currentResult < cExpectedResult; currentResult = subscriber.size())
                {
                Eventually.assertDeferred(subscriber::size, not(currentResult));
                }
            Eventually.assertDeferred(subscriber::isCompleted, is(true));
            MatcherAssert.assertThat(subscriber.size(), is(cExpectedResult));
            }
        finally
            {
            m_taskExecutorService.deregister(localES);
            localES.shutdown();
            }
        }

    public void shouldExecuteAndCompleteTaskSequentially()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .sequentially()
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue())
                .subscribe(subscriber).submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));

        // test: subscribe after completed

        subscriber = new RecordingSubscriber<>();

        coordinator.subscribe(subscriber);

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        }

    public void shouldGetTaskIdFromContext()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new GetTaskIdTask())
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue())
                .subscribe(subscriber)
                .submit();

        Eventually.assertDeferred(() -> subscriber.received(coordinator.getTaskId()), is(true));
        }

    public void shouldGetExecutorIdFromContext()
        {
        ExecutorService es = Executors.newSingleThreadExecutor();

        try
            {
            RecordingSubscriber<String> subscriber  = new RecordingSubscriber<>();
            String                      sExecutorId = m_taskExecutorService.register(es, Role.of("foo")).getId();

            m_taskExecutorService.orchestrate(new GetExecutorIdTask())
                .filter(Predicates.role("foo"))
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue())
                .subscribe(subscriber)
                .submit();

            Eventually.assertDeferred(() -> subscriber.received(sExecutorId), is(true));
            }
        finally
            {
            es.shutdown();
            m_taskExecutorService.deregister(es);
            }
        }

    public void shouldExecuteAndCompleteNullTask()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new NullTask<String>())
                .collect(TaskCollectors.lastOf()).until(Predicates.always())
                .subscribe(subscriber).submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received(null), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));

        // test: subscribe after completed
        subscriber = new RecordingSubscriber<>();

        coordinator.subscribe(subscriber);

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        }

    public void shouldExecuteAfterExecutorIsAdded()
        {
        ExecutorService             executorService = Executors.newSingleThreadExecutor();
        RecordingSubscriber<String> subscriber      = new RecordingSubscriber<>();

        try
            {
            Task.Coordinator<String> coordinator =
                m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                    .filter(Predicates.role("foo"))
                    .collect(TaskCollectors.lastOf())
                    .until(Predicates.notNullValue())
                    .subscribe(subscriber).submit();

            Eventually.assertDeferred(subscriber::isSubscribed, is(true));
            Eventually.assertDeferred(subscriber::isCompleted, is(false));
            MatcherAssert.assertThat(subscriber.size(), is(0));

            MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(true));
            MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
            MatcherAssert.assertThat(subscriber.received("Hello World"), is(false));
            MatcherAssert.assertThat(coordinator.isDone(), is(false));

            m_taskExecutorService.register(executorService, Role.of("foo"));

            Eventually.assertDeferred(subscriber::isCompleted, is(true));
            MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
            MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
            MatcherAssert.assertThat(subscriber.size(), is(1));

            MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
            MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
            MatcherAssert.assertThat(coordinator.isDone(), is(true));
            }
        finally
            {
            m_taskExecutorService.deregister(executorService);
            executorService.shutdown();
            }
        }

    public void shouldExecuteAndNotCompleteTask()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.lastOf()).subscribe(subscriber).submit();

        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(true));
        Eventually.assertDeferred(() -> subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(true));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(false));
        }

    public void shouldCancelTask()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        // start a task that never completes
        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.lastOf()).subscribe(subscriber).submit();

        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(true));
        Eventually.assertDeferred(() -> subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(true));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(false));

        // cancel
        MatcherAssert.assertThat(coordinator.cancel(true), is(true));

        Eventually.assertDeferred(subscriber::isSubscribed, is(false));
        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(true));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));

        // subsequent cancel attempts should fail
        MatcherAssert.assertThat(coordinator.cancel(true), is(false));

        // test: subscribe after canceled

        RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();

        coordinator.subscribe(subscriber2);

        Eventually.assertDeferred(subscriber2::isSubscribed, is(false));
        MatcherAssert.assertThat(subscriber2.isCompleted(), is(false));

        RecordingSubscriber<String> subscriber5 = new RecordingSubscriber<>();
        Task.Coordinator<String> coordinator5 = m_taskExecutorService.orchestrate(new RepeatedTask<>("Hello World", 50000))
            .collect(TaskCollectors.lastOf())
            .subscribe(subscriber5)
            .submit();

        // cancel
        MatcherAssert.assertThat(coordinator5.cancel(true), is(true));
        Eventually.assertDeferred(coordinator5::isCancelled, is(true));
        Eventually.assertDeferred(subscriber5::isSubscribed, is(false));
        MatcherAssert.assertThat(coordinator5.isDone(), is(true));
        }

    public void shouldAcquireCoordinatorForSubmittedTask()
        {
        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.lastOf()).submit();

        Task.Coordinator<String> other = m_taskExecutorService.acquire(coordinator.getTaskId());

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.getTaskId(), is(other.getTaskId()));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(false));
        }

    public void shouldRegisterAndDeRegisterExecutor()
        {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try
            {
            TaskExecutorService.Registration initial = m_taskExecutorService.register(executorService);

            MatcherAssert.assertThat(initial, is(notNullValue()));

            TaskExecutorService.Registration secondary = m_taskExecutorService.register(executorService);

            MatcherAssert.assertThat(initial, is(secondary));

            TaskExecutorService.Registration registration = m_taskExecutorService.deregister(executorService);

            MatcherAssert.assertThat(registration, is(initial));

            registration = m_taskExecutorService.deregister(executorService);

            MatcherAssert.assertThat(registration, is(nullValue()));
            }
        finally
            {
            executorService.shutdownNow();
            m_taskExecutorService.deregister(executorService);
            }
        }

    public void shouldAutomaticallyRemoveTaskWhenCompleted()
        {
        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.lastOf()).until(Predicates.notNullValue()).submit();

        Eventually.assertDeferred(() -> m_taskExecutorService.acquire(coordinator.getTaskId()),
                              is(nullValue()),
                              delayedBy(10, TimeUnit.SECONDS));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));
        }

    public void shouldAutomaticallySubscribe()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue())
                .subscribe(subscriber).submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));
        }

    public void shouldPublishResultWithMultipleExecutors()
        {
        RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<Integer> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.count()).until(Predicates.is(getInitialExecutorCount()))
                .subscribe(subscriber).submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received(getInitialExecutorCount()), is(true));

        // could get results reported up to InitialExecutorCount times
        MatcherAssert.assertThat(subscriber.size(), is(both(greaterThanOrEqualTo(1)).and(lessThanOrEqualTo(getInitialExecutorCount()))));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));
        }

    public void shouldScheduleTaskToOneExecutor()
        {
        RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<Integer> coordinator =
            m_taskExecutorService
                .orchestrate(CronTask.of(new CountingTask(), "* * * * *"))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber).submit();

        // Verify that the task is executed at least 3 times in the 3 minutes interval
        Repetitively.assertThat(invoking(subscriber).isCompleted(), is(false), Timeout.of(3, TimeUnit.MINUTES));
        MatcherAssert.assertThat(subscriber.received(2), is(true));

        MatcherAssert.assertThat(coordinator.cancel(true), is(true));

        Eventually.assertDeferred(subscriber::isSubscribed, is(false));
        Eventually.assertThat(subscriber.isCompleted(), is(false));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(true));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));
        }

    public void shouldScheduleCronTaskToMultipleExecutors()
        {
        RecordingSubscriber<Integer> subscriber    = new RecordingSubscriber<>();
        int                          cExpectedTotal = 2 * getInitialExecutorCount();

        Task.Coordinator<Integer> coordinator =
            m_taskExecutorService.orchestrate(CronTask.of(new ValueTask<>("Hello World"), "* * * * *")).concurrently()
                .collect(TaskCollectors.count())
                .subscribe(subscriber)
                .submit();

        Eventually.assertDeferred(() -> subscriber.received(cExpectedTotal), is(true),
                              within(3, TimeUnit.MINUTES), delayedBy(2, TimeUnit.MINUTES));
        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));

        MatcherAssert.assertThat(coordinator.cancel(true), is(true));

        Eventually.assertDeferred(subscriber::isSubscribed, is(false));
        MatcherAssert.assertThat(subscriber.size(), is(both(greaterThanOrEqualTo(2)).and(lessThanOrEqualTo(cExpectedTotal))));
        Eventually.assertThat(subscriber.isCompleted(), is(false));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(true));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));
        }

    public void shouldScheduleYieldTask()
        {
        int                          cYield     = 3;
        RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<Integer> coordinator =
            m_taskExecutorService.orchestrate(CronTask.of(new YieldingTask(cYield), "* * * * *"))
                .limit(1)
                .collect(TaskCollectors.firstOf())
                .subscribe(subscriber)
                .submit();

        Repetitively.assertThat(invoking(subscriber).isCompleted(), is(false), Timeout.of(3, TimeUnit.MINUTES));
        MatcherAssert.assertThat(subscriber.received(4), is(true));

        MatcherAssert.assertThat(coordinator.cancel(true), is(true));
        Eventually.assertDeferred(subscriber::isSubscribed, is(false));
        Eventually.assertThat(subscriber.isCompleted(), is(false));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(true));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));

        // schedule yield task that yields 0 second
        subscriber  = new RecordingSubscriber<>();
        coordinator =
            m_taskExecutorService.orchestrate(CronTask.of(new YieldingTask(5, 0), "* * * * *"))
                .limit(1)
                .collect(TaskCollectors.firstOf())
                .subscribe(subscriber)
                .submit();

        Repetitively.assertThat(invoking(subscriber).isCompleted(), is(false), Timeout.of(3, TimeUnit.MINUTES));
        MatcherAssert.assertThat(subscriber.received(6), is(true));

        MatcherAssert.assertThat(coordinator.cancel(true), is(true));

        Eventually.assertDeferred(subscriber::isSubscribed, is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(true));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));
        }

    public void shouldCollectFirstResultWithMultipleExecutors()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.firstOf()).until(Predicates.is(Predicates.notNullValue()))
                .subscribe(subscriber).submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));
        }

    public void shouldCollectFirstResultWithAnyOf()
        {
        ExecutorService executorService1 = Executors.newSingleThreadExecutor();
        ExecutorService executorService2 = Executors.newSingleThreadExecutor();
        ExecutorService executorService3 = Executors.newSingleThreadExecutor();
        ExecutorService executorService4 = Executors.newSingleThreadExecutor();
        ExecutorService executorService5 = Executors.newSingleThreadExecutor();
        ExecutorService executorService6 = Executors.newSingleThreadExecutor();

        try
            {
            m_taskExecutorService.register(executorService1);
            m_taskExecutorService.register(executorService2);
            m_taskExecutorService.register(executorService3);

            RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

            Task.Coordinator<String> coordinator =
                m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                    .limit(1)
                    .collect(TaskCollectors.firstOf())
                    .until(Predicates.is(Predicates.notNullValue()))
                    .subscribe(subscriber)
                    .submit();
            Logger.info(String.format("Task[1][%s] submitted", coordinator.getTaskId()));

            Eventually.assertDeferred(subscriber::isCompleted, is(true));
            MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
            MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
            MatcherAssert.assertThat(subscriber.size(), is(1));

            MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
            MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
            MatcherAssert.assertThat(coordinator.isDone(), is(true));

            subscriber = new RecordingSubscriber<>();
            coordinator =
                m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                    .limit(3)
                    .collect(TaskCollectors.firstOf())
                    .until(Predicates.is(Predicates.notNullValue())).subscribe(subscriber).submit();
            Logger.info(String.format("Task [2][%s] submitted", coordinator.getTaskId()));

            Eventually.assertDeferred(subscriber::isCompleted, is(true));
            MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
            MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
            MatcherAssert.assertThat(subscriber.size(), is(1));

            MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
            MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
            MatcherAssert.assertThat(coordinator.isDone(), is(true));

            m_taskExecutorService.deregister(executorService2);
            m_taskExecutorService.deregister(executorService3);

            // not enough executors initially.  Count should be higher than initial executors + 3
            // as executors are de-registered asynchronously and the above de-registers may not have
            // completed yet.
            subscriber = new RecordingSubscriber<>();
            coordinator =
                m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                    .limit(getInitialExecutorCount() + 4)
                    .collect(TaskCollectors.firstOf())
                    .until(Predicates.is(Predicates.notNullValue()))
                    .subscribe(subscriber)
                    .submit();
            Logger.info(String.format("Task [3][%s] submitted", coordinator.getTaskId()));

            Repetitively.assertThat(invoking(subscriber).isCompleted(), is(false), Timeout.of(20, TimeUnit.SECONDS));

            m_taskExecutorService.register(executorService4);
            m_taskExecutorService.register(executorService5);
            m_taskExecutorService.register(executorService6);

            Eventually.assertDeferred(subscriber::isCompleted, is(true), Timeout.of(2, TimeUnit.MINUTES));
            MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
            MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
            MatcherAssert.assertThat(subscriber.size(), is(1));

            MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
            MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
            MatcherAssert.assertThat(coordinator.isDone(), is(true));
            }
        finally
            {
            executorService1.shutdown();
            executorService2.shutdown();
            executorService3.shutdown();
            executorService4.shutdown();
            executorService5.shutdown();
            executorService6.shutdown();

            m_taskExecutorService.deregister(executorService1);
            m_taskExecutorService.deregister(executorService2);
            m_taskExecutorService.deregister(executorService3);
            m_taskExecutorService.deregister(executorService4);
            m_taskExecutorService.deregister(executorService5);
            m_taskExecutorService.deregister(executorService6);
            }
        }

    /**
     * Ensure that sequential execution of a task across multiple executors sequentially produces a list of results that
     * are collected in the order (in which they were produced)
     */
    public void shouldCollectResultsInOrder()
        {
        RecordingSubscriber<List<Long>> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<List<Long>> coordinator =
            m_taskExecutorService.orchestrate(new RandomSleepTask(3))
                .sequentially()
                .collect(TaskCollectors.listOf())
                .until(Predicates.is(Predicates.notNullValue()))
                .subscribe(subscriber)
                .submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));

        List<Long> list = subscriber.getLast();

        MatcherAssert.assertThat(list.size(), is(getInitialExecutorCount()));

        for (int i = 1; i < list.size(); i++)
            {
            MatcherAssert.assertThat(list.get(i - 1), lessThanOrEqualTo(list.get(i)));
            }

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));
        }

    public void shouldSupportMultipleSubscribers()
        {
        RecordingSubscriber<String> subscriber1 = new RecordingSubscriber<>();
        RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();

        // test: multiple subscribers on completed task

        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.lastOf()).until(Predicates.notNullValue())
                .subscribe(subscriber1).subscribe(subscriber2).submit();

        Eventually.assertDeferred(subscriber1::isCompleted, is(true));
        Eventually.assertDeferred(subscriber1::isSubscribed, is(false));
        MatcherAssert.assertThat(subscriber1.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber1.size(), is(1));

        Eventually.assertDeferred(subscriber2::isCompleted, is(true));
        Eventually.assertDeferred(subscriber2::isSubscribed, is(false));
        MatcherAssert.assertThat(subscriber2.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber2.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));

        // test: subscribe after completed

        subscriber1 = new RecordingSubscriber<>();

        coordinator.subscribe(subscriber1);

        Eventually.assertDeferred(subscriber1::isCompleted, is(true));
        Eventually.assertDeferred(subscriber1::isSubscribed, is(false));
        MatcherAssert.assertThat(subscriber1.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber1.size(), is(1));

        // test: add subscribers after submit()

        coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .filter(Predicates.role("foo"))
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue()).submit();

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));

        subscriber1 = new RecordingSubscriber<>();
        subscriber2 = new RecordingSubscriber<>();

        coordinator.subscribe(subscriber1);
        coordinator.subscribe(subscriber2);

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(true));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(false));

        // add executor to complete the task
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try
            {
            m_taskExecutorService.register(executorService, Role.of("foo"));

            Eventually.assertDeferred(subscriber1::isCompleted, is(true));
            Eventually.assertDeferred(subscriber1::isSubscribed, is(false));
            MatcherAssert.assertThat(subscriber1.received("Hello World"), is(true));
            MatcherAssert.assertThat(subscriber1.size(), is(1));

            Eventually.assertDeferred(subscriber2::isCompleted, is(true));
            Eventually.assertDeferred(subscriber2::isSubscribed, is(false));
            MatcherAssert.assertThat(subscriber2.received("Hello World"), is(true));
            MatcherAssert.assertThat(subscriber2.size(), is(1));

            MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
            MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
            MatcherAssert.assertThat(coordinator.isDone(), is(true));
            }
        finally
            {
            executorService.shutdown();
            m_taskExecutorService.deregister(executorService);
            }

        // test: canceled task
        RecordingSubscriber<String> subscriber3 = new RecordingSubscriber<>();
        RecordingSubscriber<String> subscriber4 = new RecordingSubscriber<>();

        // start a task that never completes
        coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World", 3000))
                .collect(TaskCollectors.lastOf()).subscribe(subscriber3).submit();

        coordinator.subscribe(subscriber4);

        MatcherAssert.assertThat(subscriber3.isCompleted(), is(false));
        MatcherAssert.assertThat(subscriber3.isSubscribed(), is(true));
        Eventually.assertDeferred(() -> subscriber3.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber3.size(), is(1));

        MatcherAssert.assertThat(subscriber4.isCompleted(), is(false));
        MatcherAssert.assertThat(subscriber4.isSubscribed(), is(true));
        Eventually.assertDeferred(() -> subscriber4.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber4.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(true));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(false));

        // cancel
        MatcherAssert.assertThat(coordinator.cancel(true), is(true));

        Eventually.assertDeferred(subscriber3::isSubscribed, is(false));
        Eventually.assertDeferred(subscriber3::isSubscribed, is(false));
        MatcherAssert.assertThat(subscriber3.size(), is(1));

        Eventually.assertDeferred(subscriber4::isSubscribed, is(false));
        Eventually.assertDeferred(subscriber4::isSubscribed, is(false));
        MatcherAssert.assertThat(subscriber4.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(true));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));

        // test: subscribe after canceled
        RecordingSubscriber<String> subscriber5 = new RecordingSubscriber<>();

        coordinator.subscribe(subscriber5);

        Eventually.assertDeferred(subscriber5::isSubscribed, is(false));
        MatcherAssert.assertThat(subscriber5.isCompleted(), is(false));
        }

    public void shouldHandleExceptionTask()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        ExceptionTask<String> exceptionTask = new ExceptionTask<>("Hello World");

        Task.Coordinator<String> coordinator = m_taskExecutorService.orchestrate(exceptionTask)
            .collect(TaskCollectors.lastOf()).subscribe(subscriber).submit();

        Eventually.assertDeferred(subscriber::getThrowable, Matchers.notNullValue());

        Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(false));

        coordinator.cancel(true);

        RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();

        m_taskExecutorService.orchestrate(exceptionTask)
            .collect(TaskCollectors.lastOf())
            .subscribe(subscriber2)
            .submit();

        Eventually.assertDeferred(subscriber2::isError, Matchers.is(true));
        Eventually.assertDeferred(() -> subscriber2.getThrowable().getMessage(), is(exceptionTask.getThrowable().getMessage()));

        RecordingSubscriber<String> subscriber3 = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator3 = m_taskExecutorService.orchestrate(exceptionTask)
            .collect(TaskCollectors.lastOf())
            .subscribe(subscriber3).submit();

        Eventually.assertDeferred(subscriber3::getThrowable, Matchers.notNullValue());
        Eventually.assertDeferred(subscriber3::isCompleted, Matchers.is(false));

        coordinator3.cancel(true);

        RecordingSubscriber<String> subscriber4 = new RecordingSubscriber<>();

        m_taskExecutorService.orchestrate(exceptionTask)
            .collect(TaskCollectors.lastOf())
            .subscribe(subscriber4)
            .submit();

        Eventually.assertDeferred(subscriber4::isError, Matchers.is(true));
        Eventually.assertDeferred(() -> subscriber4.getThrowable().getMessage(), is(exceptionTask.getThrowable().getMessage()));
        }

    public void shouldHandleDuplicateTaskId()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();
        m_taskExecutorService.orchestrate(new ValueTask<>("Hello World")).as("MyTask")
            .collect(TaskCollectors.firstOf()).subscribe(subscriber).submit();

        try
            {
            RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World 2")).as("MyTask")
                .collect(TaskCollectors.firstOf()).subscribe(subscriber2).submit();

            fail("Should get IllegalArgumentException");
            }
        catch (IllegalArgumentException ignored)
            {
            }

        try
            {
            RecordingSubscriber<String> subscriber3 = new RecordingSubscriber<>();
            m_taskExecutorService.orchestrate(new RepeatedTask<>("Hello World3",
                                                                 10000)).as("MyTask")
                .collect(TaskCollectors.firstOf())
                .subscribe(subscriber3)
                .submit();

            fail("Should get IllegalArgumentException");
            }
        catch (IllegalArgumentException ignored)
            {
            }

        Eventually.assertDeferred(subscriber::isSubscribed, Matchers.is(true));
        Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(false));
        Eventually.assertDeferred(() -> subscriber.received("Hello World"), Matchers.is(true));
        }

    public void shouldHandleExecutorServiceShutdown()
        {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        m_taskExecutorService.register(executorService, Role.of("foo"));

        try
            {
            RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .filter(Predicates.role("foo"))
                .collect(TaskCollectors.firstOf())
                .subscribe(subscriber).submit();

            Eventually.assertDeferred(subscriber::isSubscribed, Matchers.is(true));
            Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(false));
            Eventually.assertDeferred(() -> subscriber.received("Hello World"), Matchers.is(true));

            executorService.shutdownNow();

            boolean fTerminated;
            try
                {
                fTerminated = executorService.awaitTermination(1, TimeUnit.MINUTES);
                }
            catch (InterruptedException e)
                {
                throw Base.ensureRuntimeException(e);
                }

            MatcherAssert.assertThat(fTerminated, Matchers.is(true));
            MatcherAssert.assertThat(executorService.isTerminated(), Matchers.is(true));

            RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();

            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .filter(Predicates.role("foo"))
                .collect(TaskCollectors.firstOf())
                .subscribe(subscriber2).submit();

            Eventually.assertDeferred(subscriber2::isCompleted, Matchers.is(false));
            Repetitively.assertThat(invoking(subscriber2).received("Hello World"),
                                    Matchers.is(false),
                                    within(5, TimeUnit.SECONDS));
            }
        finally
            {
            executorService.shutdown();
            m_taskExecutorService.deregister(executorService);
            }
        }

    public void shouldHandleSubscriberException()
        {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try
            {
            m_taskExecutorService.register(executorService);
            TestSubscriber<String> subscriber = new TestSubscriber<>();

            m_taskExecutorService.orchestrate(new ValueTask<>("No exceptions"))
                .collect(TaskCollectors.firstOf()).until(Predicates.is(Predicates.notNullValue())).subscribe(subscriber)
                .submit();
            Eventually.assertDeferred(subscriber::isSubscribed, Matchers.is(true));
            Eventually.assertDeferred(() -> subscriber.received("No exceptions"), Matchers.is(true));
            Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(true));

            TestSubscriber<String> subscriber2 = new TestSubscriber<>(TestSubscriber.WhereToThrow.ON_SUBSCRIBE);

            m_taskExecutorService.orchestrate(new ValueTask<>("Exception in onSubscribe"))
                .collect(TaskCollectors.firstOf()).until(Predicates.is(Predicates.notNullValue())).subscribe(subscriber)
                .submit();
            Repetitively.assertThat(DeferredHelper.invoking(subscriber2).isSubscribed(),
                                    Matchers.is(false),
                                    Timeout.of(10, TimeUnit.SECONDS));

            TestSubscriber<String> subscriber3 = new TestSubscriber<>(TestSubscriber.WhereToThrow.ON_NEXT);

            m_taskExecutorService.orchestrate(new ValueTask<>("Exception in onNext"))
                .collect(TaskCollectors.firstOf()).until(Predicates.is(Predicates.notNullValue())).subscribe(subscriber3)
                .submit();
            Eventually.assertDeferred(subscriber3::isSubscribed, Matchers.is(true));
            Repetitively.assertThat(DeferredHelper.invoking(subscriber3).received("Exception in onNext"),
                                    Matchers.is(false),
                                    Timeout.of(10, TimeUnit.SECONDS));
            Eventually.assertDeferred(subscriber3::isCompleted, Matchers.is(false));

            TestSubscriber<String> subscriber4 = new TestSubscriber<>(TestSubscriber.WhereToThrow.ON_COMPLETE);

            m_taskExecutorService.orchestrate(new ValueTask<>("Exception in onComplete"))
                .collect(TaskCollectors.firstOf()).until(Predicates.is(Predicates.notNullValue())).subscribe(subscriber4)
                .submit();
            Eventually.assertDeferred(subscriber4::isSubscribed, Matchers.is(true));
            Eventually.assertDeferred(() -> subscriber4.received("Exception in onComplete"), Matchers.is(true));
            Eventually.assertDeferred(subscriber4::isCompleted, Matchers.is(false));
            }
        finally
            {
            executorService.shutdown();
            m_taskExecutorService.deregister(executorService);
            }
        }

    public void shouldYieldTask()
        {
        RecordingSubscriber<Integer> subscriber = new RecordingSubscriber<>();

        int cYield = 5;

        m_taskExecutorService.orchestrate(new YieldingTask(cYield))
            .limit(1)
            .collect(TaskCollectors.firstOf())
            .until(Predicates.notNullValue())
            .subscribe(subscriber)
            .submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received(cYield), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));
        }

    public void shouldHandleExceptionsInCallableFutures()
        {
        testExceptionsInFutures(true);
        }

    public void shouldHandleExceptionsInRunnableFutures()
        {
        testExceptionsInFutures(false);
        }

    /**
     * Test various scenarios where a {@link Future} is used to track failing tasks.
     *
     * @param fCallable whether to use {@link Callable}s or {@link Runnable}s tasks
     */
    public void testExceptionsInFutures(boolean fCallable)
        {
        TaskExecutorService executorService = m_taskExecutorService;

        // case 1: wait until task fails
        Future future;

        if (fCallable)
            {
            future = executorService.submit(new MyCallable().throwException());
            }
        else
            {
            future = executorService.submit(new MyRunnable().throwException());
            }

        try
            {
            // should throw an ExecutionException
            future.get();
            fail();
            }
        catch (ExecutionException ee)
            {
            // success
            }
        catch (InterruptedException ie)
            {
            fail(ie.getMessage());
            }

        MatcherAssert.assertThat(future.isDone(), is(true));

        // case 2: timeout before task fails
        if (fCallable)
            {
            future = executorService.submit(new MyCallable().throwException());
            }
        else
            {
            future = executorService.submit(new MyRunnable().throwException());
            }

        long startTime = System.nanoTime();
        try
            {
            future.get(500, TimeUnit.MILLISECONDS);
            fail();
            }
        catch (TimeoutException te)
            {
            // success
            Duration duration = Duration.ofNanos(System.nanoTime() - startTime);
            MatcherAssert.assertThat(duration, greaterThan(Duration.ofMillis(250)));
            MatcherAssert.assertThat(duration, lessThan(Duration.ofSeconds(1)));
            }
        catch (ExecutionException | InterruptedException e)
            {
            fail(e.getMessage());
            }

        // case 3: task fails before timeout
        if (fCallable)
            {
            future = executorService.submit(new MyCallable().throwException());
            }
        else
            {
            future = executorService.submit(new MyRunnable().throwException());
            }

        startTime = System.nanoTime();
        try
            {
            future.get(4, TimeUnit.SECONDS);
            fail();
            }
        catch (TimeoutException | InterruptedException e)
            {
            fail(e.getMessage());
            }
        catch (ExecutionException ee)
            {
            // success
            Duration duration = Duration.ofNanos(System.nanoTime() - startTime);
            MatcherAssert.assertThat(duration, greaterThan(Duration.ofMillis(900)));
            MatcherAssert.assertThat(duration, lessThan(Duration.ofMillis(3900)));
            }

        MatcherAssert.assertThat(future.isDone(), is(true));
        }

    public void shouldHandleTasksFailingInInvokeAny()
        {
        TaskExecutorService executorService = m_taskExecutorService;

        Collection colCallables = new ArrayList<>(3);
        colCallables.add(new MyCallable().throwException());
        colCallables.add(new MyCallable().throwException());

        // all fail with timeout
        try
            {
            executorService.invokeAny(colCallables, 10, TimeUnit.SECONDS);
            fail();
            }
        catch (ExecutionException ee)
            {
            // success
            }
        catch (InterruptedException | TimeoutException e)
            {
            fail(e.getMessage());
            }

        // all fail with no timeout
        try
            {
            executorService.invokeAny(colCallables);
            fail();
            }
        catch (ExecutionException ee)
            {
            // success
            }
        catch (InterruptedException e)
            {
            fail(e.getMessage());
            }

        // add a task which will succeed.  make the success twice as long as the failures
        // so the invokeAny() has to process the failures
        colCallables.add(new MyCallable().sleep(Duration.ofSeconds(2)));

        // some fail with timeout
        try
            {
            executorService.invokeAny(colCallables, 10, TimeUnit.SECONDS);
            }
        catch (ExecutionException | InterruptedException | TimeoutException e)
            {
            fail(e.getMessage());
            }

        // some fail with no timeout
        try
            {
            executorService.invokeAny(colCallables);
            }
        catch (ExecutionException | InterruptedException e)
            {
            fail(e.getMessage());
            }
        }

    public void shouldUseDefaultExecutor()
        {
        try
            {
            TaskExecutorService executorService = m_taskExecutorService;

            executorService.execute(new MyRunnable("hello"));

            Future<String> result = executorService.submit(new MyCallable(5, "MyCallable result"));

            assertEquals(result.get(), "MyCallable result");
            result = executorService.submit(new MyCallable(5));
            assertEquals(result.get(), "This is the result");

            Future<String> result2 = executorService.submit(new MyRunnable("submit hello"), "submit result");

            assertEquals(result2.get(), "submit result");

            Collection colCallables = new ArrayList<>(10);

            colCallables.add(new MyCallable(5));
            colCallables.add(new MyCallable(10, "MyCallable result for invokeAll"));

            List<Future<String>> results = executorService.invokeAll(colCallables);

            assertEquals(results.size(), 2);
            for (int i = 0; i < 2; ++i)
                {
                Eventually.assertThat(valueOf(future(String.class, results.get(i))),
                                      either(is("This is the result")).or(is("MyCallable result for invokeAll")));
                }

            colCallables.add(new MyCallable(10, "MyCallable result for invokeAny"));

            String result3 = (String) executorService.invokeAny(colCallables);

            assertTrue(result3.equals("This is the result")
                              || result3.equals("MyCallable result for invokeAny")
                              || result3.equals("MyCallable result for invokeAll"));

            for (int i = 3; i < 10; i++)
                {
                colCallables.add(new MyCallable(i, "MyCallable result for invokeAny " + i));
                }

            results = executorService.invokeAll(colCallables, 1, TimeUnit.MILLISECONDS);
            assertEquals(results.size(), 10);

            try
                {
                for (Future<String> r : results)
                    {
                    r.get(0, TimeUnit.MILLISECONDS);
                    }
                fail("Should fail with Exception.");
                }
            catch (ExecutionException | InterruptedException | NoSuchElementException | CancellationException e)
                {
                // success
                }

            System.out.println("Calling invokeAny() with a short timeout.");
            try
                {
                executorService.invokeAny(colCallables, 1, TimeUnit.MILLISECONDS);
                System.out.println("invokeAny() failed to timeout.");
                fail("Should fail with TimeoutException.");
                }
            catch (TimeoutException e)
                {
                // success
                }

            colCallables.clear();
            colCallables.add(new MyCallable(5, "null"));
            colCallables.add(new MyCallable(10, "null"));
            colCallables.add(new MyCallable(15, "null"));
            results = executorService.invokeAll(colCallables);
            assertEquals(3, results.size());
            }
        catch (Throwable t)
            {
            throw Base.ensureRuntimeException(t);
            }
        }

    public void shouldHandleProperties()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();
        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .define("key1", "value1")
                .define("key2", "value2")
                .collect(TaskCollectors.lastOf())
                .subscribe(subscriber).submit();

        assertEquals("value1", coordinator.getProperties().get("key1"));
        assertEquals("value2", coordinator.getProperties().get("key2"));

        MatcherAssert.assertThat(subscriber.isCompleted(), is(false));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(true));
        Eventually.assertDeferred(() -> subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();
        Task.Coordinator<String> coordinator2 = m_taskExecutorService.orchestrate(new RepeatedTask<>("Hello World", 10000))
            .define("key1", "value21")
            .define("key2", "value22")
            .collect(TaskCollectors.lastOf())
            .until(Predicates.notNullValue())
            .subscribe(subscriber2)
            .submit();

        assertEquals("value21", coordinator2.getProperties().get("key1"));
        assertEquals("value22", coordinator2.getProperties().get("key2"));

        Eventually.assertDeferred(subscriber2::isSubscribed, is(false));
        MatcherAssert.assertThat(subscriber2.isCompleted(), is(true));
        Eventually.assertDeferred(() -> coordinator2.getProperties().get("key1"), is(Matchers.nullValue()));
        Eventually.assertDeferred(() -> coordinator2.getProperties().get("key2"), is(Matchers.nullValue()));

        MatcherAssert.assertThat(coordinator.cancel(true), is(true));
        Eventually.assertDeferred(coordinator::isCancelled, is(true));
        Eventually.assertDeferred(coordinator::isDone, is(true));
        Eventually.assertDeferred(() -> subscriber.received("Hello World"), Matchers.is(true));
        Eventually.assertDeferred(subscriber::isSubscribed, Matchers.is(false));
        Eventually.assertDeferred(() -> coordinator.getProperties().get("key1"), is(Matchers.nullValue()));
        Eventually.assertDeferred(() -> coordinator.getProperties().get("key2"), is(Matchers.nullValue()));

        RecordingSubscriber<String> subscriber3  = new RecordingSubscriber<>();
        Task.Coordinator<String>    coordinator3 =
            m_taskExecutorService.orchestrate(new UpdatePropertiesTask())
                .define("key1", "value1")
                .define("key2", "value2")
                .collect(TaskCollectors.lastOf())
                .until(new Predicates.EqualToPredicate("finished"))
                .subscribe(subscriber3).submit();

        Eventually.assertDeferred(() -> subscriber3.received("started"), Matchers.is(true));
        Eventually.assertDeferred(() -> coordinator3.getProperties().get("key1"), is("newValue1"));
        Eventually.assertDeferred(() -> coordinator3.getProperties().get("key2"), is("newValue2"));
        Eventually.assertDeferred(() -> subscriber3.received("finished"), Matchers.is(true));
        Eventually.assertDeferred(() -> coordinator3.getProperties().get("key1"), is(Matchers.nullValue()));
        Eventually.assertDeferred(() -> coordinator3.getProperties().get("key2"), is(Matchers.nullValue()));

        RecordingSubscriber<String> subscriber4  = new RecordingSubscriber<>();
        Task.Coordinator<String>    coordinator4 =
            m_taskExecutorService.orchestrate(new UpdatePropertiesTask())
                .collect(TaskCollectors.lastOf())
                .until(new Predicates.EqualToPredicate("finished"))
                .subscribe(subscriber4).submit();

        Eventually.assertDeferred(() -> subscriber4.received("started"), Matchers.is(true));
        Eventually.assertDeferred(() -> coordinator4.getProperties().get("key1"), is("newValue1"));
        Eventually.assertDeferred(()-> coordinator4.getProperties().get("key2"), is("newValue2"));
        Eventually.assertDeferred(()-> subscriber4.received("finished"), Matchers.is(true));
        Eventually.assertDeferred(() -> coordinator4.getProperties().get("key1"), is(Matchers.nullValue()));
        Eventually.assertDeferred(() -> coordinator4.getProperties().get("key2"), is(Matchers.nullValue()));
        }

    public void shouldUseScheduledExecutor()
        {
        TaskExecutorService executorService = m_taskExecutorService;

        ScheduledFuture<?> result1 = executorService.schedule(new MyRunnable("hello"), 30, TimeUnit.SECONDS);
        Repetitively.assertThat(invoking(result1).isDone(), Matchers.is(false), within(25, TimeUnit.SECONDS));
        Eventually.assertDeferred(() ->
                {
                try
                    {
                    return result1.get();
                    }
                catch (InterruptedException | ExecutionException e)
                    {
                    e.printStackTrace();
                    throw new RuntimeException(new Exception (e));
                    }
                },
                is(true));

        ScheduledFuture<?> result2 = executorService.schedule(new MyCallable(5, "MyCallable result"), 20, TimeUnit.SECONDS);
        Repetitively.assertThat(invoking(result2).isDone(), Matchers.is(false), within(18, TimeUnit.SECONDS));
        Eventually.assertDeferred(() ->
                {
                try
                    {
                    return result2.get();
                    }
                catch (InterruptedException | ExecutionException e)
                    {
                    e.printStackTrace();
                    throw new RuntimeException(new Exception (e));
                    }
                },
                is("MyCallable result"));

        try
            {
            assertEquals(result2.get(), "MyCallable result");
            }
        catch (Throwable t)
            {
            throw Base.ensureRuntimeException(t);
            }

        ScheduledFuture<?> result3 = executorService.schedule(new MyCallable(5), 20, TimeUnit.SECONDS);
        Repetitively.assertThat(invoking(result3).isDone(), Matchers.is(false), within(18, TimeUnit.SECONDS));
        Eventually.assertDeferred(() ->
                {
                try
                    {
                    return result3.get();
                    }
                catch (InterruptedException | ExecutionException e)
                    {
                    e.printStackTrace();
                    throw new RuntimeException(new Exception (e));
                    }
                },
                is("This is the result"));


        try
            {
            assertEquals(result3.get(), "This is the result");
            }
        catch (Throwable t)
            {
            throw Base.ensureRuntimeException(t);
            }

        ScheduledFuture<?> result = executorService.scheduleAtFixedRate(new MyRunnable("counter"), 15, 10, TimeUnit.SECONDS);
        assertThat(result.getDelay(TimeUnit.SECONDS), is(15L));
        Base.sleep(30000);
        result.cancel(true);
        Eventually.assertDeferred(result::isCancelled, is(true));

        result = executorService.scheduleWithFixedDelay(new MyRunnable("counter"), 15, 10, TimeUnit.SECONDS);
        Base.sleep(30000);
        result.cancel(true);
        Eventually.assertDeferred(result::isCancelled, is(true));
        }

    public void shouldCallRunnableAfterTaskComplete()
        {
        RecordingSubscriber<String>     subscriber   = new RecordingSubscriber<>();
        Task.CompletionRunnable<String> myCompletion = new MyCompletion();

        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .retain(Duration.ofSeconds(20))
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue())
                .andThen(myCompletion)
                .subscribe(subscriber)
                .submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));
        Eventually.assertDeferred(() -> isCompletionCalled(myCompletion, coordinator.getTaskId()), is(true));
        }

    public void shouldCallRunnableAfterTaskCompleteWithException()
        {
        RecordingSubscriber<String>     subscriber    = new RecordingSubscriber<>();
        Task.CompletionRunnable<String> myCompletion  = new MyCompletion();
        ExceptionTask<String>           exceptionTask = new ExceptionTask<>("Hello World");

        Task.Coordinator<String> coordinator = m_taskExecutorService.orchestrate(exceptionTask)
            .retain(Duration.ofSeconds(20))
            .collect(TaskCollectors.lastOf())
            .andThen(myCompletion)
            .subscribe(subscriber).submit();

        Repetitively.assertThat(invoking(subscriber).getThrowable(),
                                Matchers.nullValue(),
                                within(10, TimeUnit.SECONDS));
        Eventually.assertDeferred(subscriber::isCompleted, Matchers.is(false));

        coordinator.cancel(true);
        Eventually.assertDeferred(subscriber::isError, is(true));
        MatcherAssert.assertThat(isCompletionCalled(myCompletion, coordinator.getTaskId()), is(true));

        RecordingSubscriber<String> subscriber1 = new RecordingSubscriber<>();
        myCompletion = new MyCompletion();

        coordinator = m_taskExecutorService.orchestrate(exceptionTask)
            .retain(Duration.ofSeconds(20))
            .collect(TaskCollectors.lastOf())
            .andThen(myCompletion)
            .subscribe(subscriber1)
            .submit();

        Eventually.assertDeferred(subscriber1::isError, Matchers.is(true));
        Eventually.assertDeferred(() -> subscriber1.getThrowable().getMessage(), is(exceptionTask.getThrowable().getMessage()));
        MatcherAssert.assertThat(isCompletionCalled(myCompletion, coordinator.getTaskId()), is(true));

        RecordingSubscriber<String> subscriber2 = new RecordingSubscriber<>();
        myCompletion = new MyCompletion();

        coordinator = m_taskExecutorService.orchestrate(exceptionTask)
            .retain(Duration.ofSeconds(20))
            .collect(TaskCollectors.lastOf())
            .andThen(myCompletion)
            .subscribe(subscriber2).submit();

        Eventually.assertDeferred(subscriber2::getThrowable, Matchers.nullValue());
        Eventually.assertDeferred(subscriber2::isCompleted, Matchers.is(false));

        coordinator.cancel(true);
        Eventually.assertDeferred(coordinator::isCancelled, Matchers.is(true));
        MatcherAssert.assertThat(isCompletionCalled(myCompletion, coordinator.getTaskId()), is(true));

        RecordingSubscriber<String> subscriber3 = new RecordingSubscriber<>();
        myCompletion = new MyCompletion();

        coordinator = m_taskExecutorService.orchestrate(exceptionTask)
            .retain(Duration.ofSeconds(20))
            .collect(TaskCollectors.lastOf())
            .andThen(myCompletion)
            .subscribe(subscriber3)
            .submit();

        Eventually.assertDeferred(subscriber3::isError, Matchers.is(true));
        Eventually.assertDeferred(() -> subscriber3.getThrowable().getMessage(), is(exceptionTask.getThrowable().getMessage()));
        MatcherAssert.assertThat(isCompletionCalled(myCompletion, coordinator.getTaskId()), is(true));
        }

    public void shouldChangeLogLevelWithDebugging()
        {
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .with(Debugging.of(Logger.FINEST))
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue())
                .subscribe(subscriber)
                .submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));

        // test: subscribe after completed

        subscriber = new RecordingSubscriber<>();

        coordinator.subscribe(subscriber);

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));

        // submit the task without debugging
        subscriber = new RecordingSubscriber<>();

        coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .collect(TaskCollectors.lastOf())
                .until(Predicates.notNullValue())
                .subscribe(subscriber)
                .submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), is(false));
        MatcherAssert.assertThat(subscriber.received("Hello World"), is(true));
        MatcherAssert.assertThat(subscriber.size(), is(1));

        MatcherAssert.assertThat(((AbstractTaskCoordinator) coordinator).hasSubscribers(), is(false));
        MatcherAssert.assertThat(coordinator.isCancelled(), is(false));
        MatcherAssert.assertThat(coordinator.isDone(), is(true));
        }

    public void shouldRejectTaskSubmissionsAfterShutdown()
        {
        assertThrows(RejectedExecutionException.class, () ->
            {
            ClusteredExecutorService testTaskExecutorService = createExecutorService();

            testTaskExecutorService.shutdown();

            testTaskExecutorService.submit(() -> {});
            });
        }

    public void shouldRejectRegisterAfterShutdown()
        {
        assertThrows(IllegalStateException.class, () ->
            {
            ClusteredExecutorService testTaskExecutorService = createExecutorService();

            testTaskExecutorService.shutdown();

            ExecutorService testES = Executors.newSingleThreadExecutor();
            try
                {
                testTaskExecutorService.register(testES);
                }
            finally
                {
                testES.shutdown();
                }
            });
        }

    public void shouldShutdownGracefullyWithNoExecutorsNoTasks()
        {
        testShutdown(/* graceful */ true, /* registerExecutors */ false, /* withTasks */ false);
        }

    public void shouldShutdownGracefullyWithNoExecutorsWithTasks()
        {
        testShutdown(/* graceful */ true, /* registerExecutors */ false, /* withTasks */ true);
        }

    public void shouldShutdownGracefullyWithExecutorsNoTasks()
        {
        testShutdown(/* graceful */ true, /* registerExecutors */ true, /* withTasks */ false);
        }

    public void shouldShutdownGracefullyWithExecutorsWithTasks()
        {
        testShutdown(/* graceful */ true, /* registerExecutors */ true, /* withTasks */ true);
        }

    public void shouldShutdownNowWithNoExecutorsNoTasks()
        {
        testShutdown(/* graceful */ false, /* registerExecutors */ false, /* withTasks */ false);
        }

    public void shouldShutdownNowWithNoExecutorsWithTasks()
        {
        testShutdown(/* graceful */ false, /* registerExecutors */ false, /* withTasks */ true);
        }

    public void shouldShutdownNowWithExecutorsNoTasks()
        {
        testShutdown(/* graceful */ false, /* registerExecutors */ true, /* withTasks */ false);
        }

    public void shouldShutdownNowWithExecutorsWithTasks()
        {
        testShutdown(/* graceful */ false, /* registerExecutors */ true, /* withTasks */ true);
        }

    /**
     * Test shutdown behavior under various scenarios.
     *
     * @param graceful           whether to call shutdown(), or shutdownNow()
     * @param registerExecutors  whether a new "local" Executor should be registered
     *                           (which will need to be de-registered by the shutdown process)
     * @param withTasks          whether to have a long-running task executing during the shutdown process
     */
    private void testShutdown(boolean graceful, boolean registerExecutors, boolean withTasks)
        {
        final int NUM_CALLABLES = 5;    // number of Callables awaiting ES termination
        ClusteredExecutorService testTaskExecutorService = createExecutorService();
        ExecutorService awaitTerminationES = Executors.newFixedThreadPool(NUM_CALLABLES);
        ExecutorService localES = Executors.newSingleThreadExecutor();

        try
            {
            if (registerExecutors)
                {
                // register an ES to verify that it is de-registered during the shutdown process
                testTaskExecutorService.register(localES, Role.of("localOnly"));
                }

            Collection<Future<Boolean>> results = new HashSet<>();

            for (int i = 0; i < NUM_CALLABLES; ++i)
                {
                results.add(awaitTerminationES.submit(new AwaitTerminationCallable(testTaskExecutorService)));
                }

            MatcherAssert.assertThat(testTaskExecutorService.isShutdown(), is(false));
            MatcherAssert.assertThat(testTaskExecutorService.isTerminated(), is(false));

            long startTime = System.nanoTime();

            // should be false, and the waiting will give time for the Callable threads to start
            MatcherAssert.assertThat(testTaskExecutorService.awaitTermination(1, TimeUnit.SECONDS), is(false));

            Duration duration = Duration.ofNanos(System.nanoTime() - startTime);

            // verify that awaitTermination() waited around 1 second
            MatcherAssert.assertThat(duration, greaterThan(Duration.ofMillis(500)));
            MatcherAssert.assertThat(duration, lessThan(Duration.ofMillis(1500)));

            // verify that the callables are still waiting
            for (Future<Boolean> result : results)
                {
                MatcherAssert.assertThat(result.isDone(), is(false));
                }

            RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();
            if (withTasks)
                {
                // start a long-running task
                Task.Orchestration<String> orchestration =
                    testTaskExecutorService.orchestrate(new SleeperTask()).limit(1);

                if (registerExecutors)
                    {
                    orchestration = orchestration.filter(Predicates.role("localOnly"));
                    }

                orchestration.collect(TaskCollectors.lastOf())
                    .until(new Predicates.EqualToPredicate("finished"))
                    .subscribe(subscriber)
                    .submit();

                // wait until the task has started
                Eventually.assertDeferred(subscriber::getLast, startsWith("started"));
                }

            if (graceful)
                {
                testTaskExecutorService.shutdown();
                }
            else
                {
                testTaskExecutorService.shutdownNow();
                }

            MatcherAssert.assertThat(testTaskExecutorService.isShutdown(), is(true));

            startTime = System.nanoTime();
            MatcherAssert.assertThat(testTaskExecutorService.awaitTermination(1, TimeUnit.MINUTES), is(true));
            duration = Duration.ofNanos(System.nanoTime() - startTime);

            if (withTasks && registerExecutors && graceful)
                {
                // some platforms come in really close to the 10-second wait time
                // example: 9.998011498; which for the purposes of this test, is close enough,
                // so round the
                double rounded = Math.round(Double.parseDouble(duration.getSeconds() + "." + duration.toMillisPart()));

                // should have waited for the task to finish
                MatcherAssert.assertThat(rounded, greaterThanOrEqualTo(10.0d));
                MatcherAssert.assertThat(rounded, lessThan(20.0d));
                }
            else
                {
                // in all other cases the ExecutorService would have shut down quickly
                MatcherAssert.assertThat(duration, lessThan(Duration.ofSeconds(5)));
                }

            MatcherAssert.assertThat(testTaskExecutorService.isTerminated(), is(true));

            if (registerExecutors)
                {
                // verify that localES has been de-registered
                MatcherAssert.assertThat(testTaskExecutorService.deregister(localES), is(nullValue()));
                }

            // should return immediately
            MatcherAssert.assertThat(testTaskExecutorService.awaitTermination(0, TimeUnit.SECONDS), is(true));

            for (Future<Boolean> result : results)
                {
                MatcherAssert.assertThat(result.get(5, TimeUnit.SECONDS), is(true));
                }

            if (withTasks)
                {
                if (registerExecutors)
                    {
                    if (graceful)
                        {
                        // graceful shutdown - the ExecutorService waited for the locally
                        // running task to complete

                        Eventually.assertDeferred(subscriber::getLast, is("finished"));
                        Eventually.assertDeferred(subscriber::size, is(2));
                        }
                    else
                        {
                        // shutdownNow() with a local executor that was running a task

                        // task is now waiting for a valid executor - register a new one with a temporary CES
                        localES.shutdown();
                        localES = Executors.newSingleThreadExecutor();
                        ClusteredExecutorService registerTaskExecutorService = createExecutorService();
                        try
                            {
                            registerTaskExecutorService.register(localES, Role.of("localOnly"));

                            Eventually.assertDeferred(subscriber::getLast, is("finished"));

                            // restart should trigger another "started" getting sent meaning the task is running
                            // on the new localES
                            Eventually.assertDeferred(subscriber::size, is(3));
                            }
                        finally
                            {
                            registerTaskExecutorService.shutdown();
                            }
                        }
                    }
                else
                    {
                    // the task should still be running
                    MatcherAssert.assertThat(subscriber.getLast(), startsWith("started"));
                    Eventually.assertDeferred(subscriber::getLast, is("finished"));
                    Eventually.assertDeferred(subscriber::size, is(2));
                    }
                Eventually.assertDeferred(subscriber::isCompleted, is(true));
                }

            }
        catch (InterruptedException | TimeoutException | ExecutionException e)
            {
            e.printStackTrace();

            throw new RuntimeException(e);
            }
        finally
            {
            testTaskExecutorService.shutdown();
            awaitTerminationES.shutdown();
            localES.shutdown();

            try
                {
                MatcherAssert.assertThat(testTaskExecutorService.awaitTermination(1, TimeUnit.MINUTES), is(true));
                MatcherAssert.assertThat(awaitTerminationES.awaitTermination(1, TimeUnit.MINUTES), is(true));
                MatcherAssert.assertThat(localES.awaitTermination(1, TimeUnit.MINUTES), is(true));
                }
            catch (InterruptedException e)
                {
                e.printStackTrace();

                //noinspection ThrowFromFinallyBlock
                throw new RuntimeException(e);
                }
            }
        }

    public void shouldRetainTask()
        {
        int remainingMillis = 30000;

        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        Task.Coordinator<String> coordinator =
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .retain(Duration.ofMillis(remainingMillis))
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

        try
            {
            Blocking.sleep(10000);
            }
        catch (InterruptedException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        remainingMillis -= 10000;

        // subscribe after completed
        subscriber = new RecordingSubscriber<>();

        coordinator.subscribe(subscriber);
        MatcherAssert.assertThat(subscriber.isCompleted(), Matchers.is(true));
        MatcherAssert.assertThat(subscriber.received("Hello World"), Matchers.is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), Matchers.is(false));

        String taskId = coordinator.getTaskId();
        subscriber    = new RecordingSubscriber<>();
        coordinator   = m_taskExecutorService.acquire(taskId);

        coordinator.subscribe(subscriber);
        MatcherAssert.assertThat(subscriber.isCompleted(), Matchers.is(true));
        MatcherAssert.assertThat(subscriber.received("Hello World"), Matchers.is(true));
        MatcherAssert.assertThat(subscriber.isSubscribed(), Matchers.is(false));

        // subscribe after the task is removed
        try
            {
            // sleep a bit longer than expiry time
            try
                {
                Blocking.sleep(remainingMillis + 12000);
                }
            catch (InterruptedException e)
                {
                throw Base.ensureRuntimeException(e);
                }

            subscriber = new RecordingSubscriber<>();
            coordinator.subscribe(subscriber);
            fail("Should fail with IllegalStateException.");
            }
        catch (IllegalStateException e)
            {
            // expected
            }

        coordinator = m_taskExecutorService.acquire(taskId);
        MatcherAssert.assertThat(coordinator, nullValue());
        }

    public void shouldNotAssignNewTasksDuringGracefulShutdown()
        {
        ClusteredExecutorService testTaskExecutorService = createExecutorService();
        ExecutorService          localES                 = Executors.newSingleThreadExecutor();

        try
            {
            testTaskExecutorService.register(localES, Role.of("localOnly"));

            // verify that a task can be assigned and run to completion on localOnly prior to shut down
            RecordingSubscriber<String> hwSubscriber = new RecordingSubscriber<>();

            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .filter(Predicates.role("localOnly")).limit(1)
                .collect(TaskCollectors.firstOf()).until(Predicates.notNullValue()).subscribe(hwSubscriber).submit();

            Eventually.assertDeferred(hwSubscriber::isCompleted, Matchers.is(true));

            RecordingSubscriber<String> sleeperSubscriber = new RecordingSubscriber<>();

            // start a long-running task to keep the test ExecutorService from terminating
            testTaskExecutorService.orchestrate(new SleeperTask())
                .filter(Predicates.role("localOnly"))
                .limit(1)
                .collect(TaskCollectors.lastOf())
                .until(new Predicates.EqualToPredicate("finished"))
                .subscribe(sleeperSubscriber).submit();

            // wait until the task has started
            Eventually.assertDeferred(sleeperSubscriber::getLast, startsWith("started"));

            // initiate graceful shutdown
            testTaskExecutorService.shutdown();

            hwSubscriber = new RecordingSubscriber<>();

            // this one should not be able to complete until a new localOnly ES is registered
            m_taskExecutorService.orchestrate(new ValueTask<>("Hello World"))
                .limit(1)
                .filter(Predicates.role("localOnly"))
                .collect(TaskCollectors.firstOf())
                .until(Predicates.notNullValue())
                .subscribe(hwSubscriber)
                .submit();

            // ExecutorService should not have completed shutdown as SleeperTask is still running on its registered ES
            Eventually.assertDeferred(testTaskExecutorService::isTerminated,
                                  Matchers.is(false),
                                  delayedBy(5, TimeUnit.SECONDS));

            Eventually.assertDeferred(sleeperSubscriber::isCompleted, Matchers.is(true));

            Eventually.assertDeferred(testTaskExecutorService::isTerminated, Matchers.is(true));

            MatcherAssert.assertThat(hwSubscriber.isCompleted(), Matchers.is(false));

            m_taskExecutorService.register(localES, Role.of("localOnly"));

            Eventually.assertDeferred(hwSubscriber::isCompleted, Matchers.is(true));

            m_taskExecutorService.deregister(localES);
            }
        finally
            {
            localES.shutdown();
            testTaskExecutorService.shutdown();

            try
                {
                MatcherAssert.assertThat(localES.awaitTermination(1, TimeUnit.MINUTES), Matchers.is(true));
                MatcherAssert.assertThat(testTaskExecutorService.awaitTermination(1, TimeUnit.MINUTES),
                                         Matchers.is(true));
                }
            catch (InterruptedException e)
                {
                e.printStackTrace();

                //noinspection ThrowFromFinallyBlock
                throw new RuntimeException(e);
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    abstract protected boolean isCompletionCalled(Task.CompletionRunnable completionRunnable, String taskId);

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

        Utils.heapdump(getCluster());
        }

    // ----- inner class: SleeperTask ---------------------------------------

    /**
     * Simulates a long-running {@link Task}. To check if the Task has started, look for a result that starts with
     * "started".
     */
    public static class SleeperTask
            implements Task<String>, PortableObject
        {
        // ----- constructors -----------------------------------------------
        /*
         * Constructs a {@link SleeperTask} (required for Serializable)
         */
        public SleeperTask()
            {
            }

        // ----- Task interface ---------------------------------------------

        @Override
        public String execute(Context<String> context)
                throws Exception
            {
            context.setResult("started @ " + new Date() + " (" + System.currentTimeMillis() + ')');

            Thread.sleep(10000);

            return "finished";
            }

        // ----- ExternalizableLite interface -------------------------------

        public void readExternal(DataInput in) throws IOException
            {
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in)
            {
            }

        @Override
        public void writeExternal(PofWriter out)
            {
            }
        }

    // ----- inner class: AwaitTerminationCallable --------------------------

    /**
     * {@link Callable} that waits for an {@link ExecutorService} to terminate.
     */
    public static class AwaitTerminationCallable
            implements Callable<Boolean>
        {
        // ----- constructors -----------------------------------------------

        public AwaitTerminationCallable(ClusteredExecutorService executorService)
            {
            f_executorService = executorService;
            }

        // ----- Callable interface -----------------------------------------

        @Override
        public Boolean call() throws InterruptedException
            {
            return f_executorService.awaitTermination(5, TimeUnit.MINUTES);
            }

        // ----- data members -----------------------------------------------

        private final ClusteredExecutorService f_executorService;
        }

    // ----- inner class: MyCallable ----------------------------------------

    @SuppressWarnings("unchecked")
    public static class MyCallable<T, R>
            implements Remote.Callable<R>, PortableObject
        {
        // ----- constructors -----------------------------------------------

        public MyCallable()
            {
            m_value  = null;
            m_result = null;
            }

        MyCallable(T value)
            {
            m_value  = value;
            m_result = null;
            }

        MyCallable(T value, R result)
            {
            m_value  = value;
            m_result = result;
            }

        // ----- public methods ---------------------------------------------

        public MyCallable throwException()
            {
            m_fError = true;
            return this;
            }

        public MyCallable sleep(Duration duration)
            {
            m_clSleep = duration.toMillis();
            return this;
            }

        // ----- Callable interface -----------------------------------------

        @Override
        public R call() throws Exception
            {
            R result = m_result;

            if (m_result == null)
                {
                result = (R) "This is the result";
                }
            else if (m_result.equals("null"))
                {
                result = null;
                }

            System.out.println("MyCallable, before sleep, with value: " + m_value + ", result: " + result + "!");
            Thread.sleep(m_clSleep);    // sleep to guarantee exceeding timeout value for timeout tests

            if (m_fError)
                {
                System.out.println("MyCallable, after sleep, with value: " + m_value + ", throwing exception: true");
                throw new RuntimeException();
                }
            else
                {
                System.out.println("MyCallable, after sleep, with value: " + m_value + ", result: " + result + "!");
                }

            return result;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_value   = in.readObject(0);
            m_result  = in.readObject(1);
            m_fError  = in.readBoolean(2);
            m_clSleep = in.readLong(3);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_value);
            out.writeObject(1, m_result);
            out.writeBoolean(2, m_fError);
            out.writeLong(3, m_clSleep);
            }

        // ----- data members -----------------------------------------------

        private T m_value;
        private R m_result;
        private boolean m_fError;
        private long m_clSleep = 1000L; // default to 1s sleep
        }

    // ----- inner class: MyRunnable ----------------------------------------

    public static class MyRunnable<T>
            implements Remote.Runnable, PortableObject
        {
        // ----- constructors -----------------------------------------------

        public MyRunnable()
            {
            }

        MyRunnable(T value)
            {
            m_value = value;
            }

        // ----- public methods ---------------------------------------------

        public MyRunnable<T> throwException()
            {
            m_fError = true;
            return this;
            }

        // ----- Runnable interface -----------------------------------------

        public void run()
            {
            if (m_value != null && m_value.equals("counter"))
                {
                m_counter++;
                System.out.println("Hello from MyRunnable with value: " + m_counter + "!");
                try
                    {
                    Thread.sleep(5000);
                    }
                catch (InterruptedException ignored)
                    {
                    }
                }
            else
                {
                System.out.println("Hello from MyRunnable with value: " + m_value + "!");
                }

            if (m_fError)
                {
                try
                    {
                    Thread.sleep(1000);
                    }
                catch (InterruptedException ie)
                    {
                    throw new RuntimeException(ie);
                    }
                System.out.println("Throwing exception from MyRunnable with value: " + m_value);
                throw new RuntimeException();
                }
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_value   = in.readObject(0);
            m_fError = in.readBoolean(1);
            m_counter = in.readInt(2);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_value);
            out.writeBoolean(1, m_fError);
            out.writeInt(2, m_counter);
            }

        // ----- data members -----------------------------------------------

        protected T m_value;
        protected boolean m_fError;
        protected int m_counter = 0;

        }

    // ----- inner class: MultipleResultsTask -------------------------------

    /**
     * A {@link Task} that sets results multiple times during execution.
     */
    public static class MultipleResultsTask
            implements Task<Integer>, PortableObject
        {
        // ----- constructors -----------------------------------------------

        /*
         * Constructs a {@link MultipleResultsTask} (required for Serializable)
         */
        @SuppressWarnings("unused")
        public MultipleResultsTask()
            {
            }

        /**
         * Constructor
         *
         * @param cResults the number of results to return on execution
         */
        public MultipleResultsTask(int cResults)
            {
            this.cResults = cResults;
            }

        // ----- Task interface ---------------------------------------------

        @Override
        public Integer execute(Context<Integer> context)
            {
            for (int i = 1; i < cResults; ++i)
                {
                context.setResult(i);
                }
            return cResults;
            }

        // ----- ExternalizableLite interface -------------------------------

        public void readExternal(DataInput in) throws IOException
            {
            cResults = ExternalizableHelper.readInt(in);
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeInt(out, cResults);
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            cResults = in.readInt(0);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeInt(0, cResults);
            }

        // ----- data members -----------------------------------------------

        private int cResults;
        }

    // ----- inner class: UpdatePropertiesTask ------------------------------

    /**
     * A {@link Task} that updates its {@link Properties}.
     */
    public static class UpdatePropertiesTask
            implements Task<String>, PortableObject
        {
        // ----- Task interface ---------------------------------------------

        @Override
        public String execute(Context<String> context) throws Exception
            {
            ExecutorTrace.entering(UpdatePropertiesTask.class, "execute", context);

            context.setResult("started");

            if (!context.isDone())
                {
                Properties properties = context.getProperties();
                properties.put("key1", "newValue1");
                properties.put("key2", "newValue2");
                }

            Blocking.sleep(5000);

            ExecutorTrace.exiting(UpdatePropertiesTask.class, "execute", "finished");

            return "finished";
            }

        // ----- ExternalizableLite interface -------------------------------

        public void readExternal(DataInput in) throws IOException
            {
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }

    // ----- inner class: CountingTask --------------------------------------

    /**
     * A {@link Task} that keeps track of the execution count through task {@link Properties}.
     */
    public static class CountingTask
            implements Task<Integer>, PortableObject
        {
        // ----- Task interface ---------------------------------------------

        @Override
        public Integer execute(Context<Integer> context)
            {
            Properties properties = context.getProperties();
            Integer    count      = properties.get("count");
            if (count == null)
                {
                count = 0;
                }
            count++;
            m_cInvoked++;
            m_sLastInvoke = new Date().toString();
            properties.put("count", count);
            System.out.println("CountingTask has executed: " + count + " times.");
            return count;
            }

        // ----- ExternalizableLite interface -------------------------------

        public void readExternal(DataInput in) throws IOException
            {
            m_sLastInvoke = ExternalizableHelper.readUTF(in);
            m_cInvoked    = ExternalizableHelper.readLong(in);
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeUTF(out,  m_sLastInvoke);
            ExternalizableHelper.writeLong(out, m_cInvoked);
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sLastInvoke = in.readString(0);
            m_cInvoked    = in.readLong(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sLastInvoke);
            out.writeLong(1,   m_cInvoked);
            }

        // ----- data members -----------------------------------------------

        protected String m_sLastInvoke;

        protected long m_cInvoked;
        }

    // ----- inner class: GetTaskIdTask -------------------------------------

    /**
     * A {@link Task} that returns the task id obtained from the {@link Context}.
     */
    public static class GetTaskIdTask
            implements Task<String>, PortableObject
        {
        // ----- Task interface ---------------------------------------------

        @Override
        public String execute(Context<String> context)
            {
            String sResult = context.getTaskId();

            System.out.println("Executor ["
                               + context.getExecutorId()
                               + "] completing execution of Task ["
                               + context.getTaskId()
                               + "] with result ["
                               + sResult
                               + ']');

            return sResult;
            }

        // ----- ExternalizableLite interface -------------------------------

        public void readExternal(DataInput in) throws IOException
            {
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }

    // ----- GetExecutorIdTask ----------------------------------------------

    /**
     * A {@link Task} that returns the {@link Executor} id obtained from the {@link Context}.
     */
    public static class GetExecutorIdTask
            implements Task<String>, PortableObject
        {
        // ----- Task interface ---------------------------------------------

        @Override
        public String execute(Context<String> context)
            {
            String sResult = context.getExecutorId();

            System.out.println("Executor ["
                               + sResult
                               + "] completing execution of Task ["
                               + context.getTaskId()
                               + "] with result ["
                               + sResult
                               + ']');

            return sResult;
            }

        // ----- ExternalizableLite interface -------------------------------

        public void readExternal(DataInput in) throws IOException
            {
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }

    // ----- inner class MyCompletion ---------------------------------------

    /**
     * A {@link Task.CompletionRunnable} that logs a message.
     */
    public static class MyCompletion
            implements Task.CompletionRunnable<String>, PortableObject
        {
        // ----- public methods ---------------------------------------------

        public boolean isLogTaskCalled()
            {
            return s_fLogTaskCalled;
            }

        // ----- Task.CompletionRunnable interface --------------------------

        @Override
        public void accept(String result)
            {
            s_fLogTaskCalled = true;
            System.out.println("Task has completed with result: " + result);
            }

        // ----- ExternalizableLite interface -------------------------------

        public void readExternal(DataInput in) throws IOException
            {
            s_fLogTaskCalled = in.readBoolean();
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeBoolean(s_fLogTaskCalled);
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            s_fLogTaskCalled = in.readBoolean(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeBoolean(0, s_fLogTaskCalled);
            }

        // ----- data members -----------------------------------------------

        static boolean s_fLogTaskCalled;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link TaskExecutorService}.
     */
    protected ClusteredExecutorService m_taskExecutorService;
    }
