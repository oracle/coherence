/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.net.CacheService;
import executor.common.SingleClusterForAllTests;
import executor.common.Utils;

import org.junit.Ignore;
import org.junit.Test;

import org.junit.experimental.categories.Category;

/**
 * Abstract class providing tests that may share a single test cluster.
 *
 * @author rl 7.29.2009
 * @since 21.12
 */
@Category(SingleClusterForAllTests.class)
public abstract class AbstractCESSingleClusterTests
        extends AbstractClusteredExecutorServiceTests
    {
    // ----- test lifecycle -------------------------------------------------

    @SuppressWarnings("resource")
    @Override
    public void cleanup()
        {
        CacheService service = getCacheService();
        if (service != null)
            {
            Caches.tasks(service).clear();
            Caches.assignments(service).clear();
            }
        super.cleanup();
        }

    // ----- constructors ---------------------------------------------------

    public AbstractCESSingleClusterTests(String extendConfig)
        {
        super(extendConfig);
        }

    // ----- test methods ---------------------------------------------------

    @Override
    @Test
    public void shouldCreateExecutorService()
        {
        Utils.assertWithFailureAction(super::shouldCreateExecutorService, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldExecuteAndCompleteTask()
        {
        Utils.assertWithFailureAction(super::shouldExecuteAndCompleteTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldExecuteAndCompleteTaskWithNoCollector()
        {
        Utils.assertWithFailureAction(super::shouldExecuteAndCompleteTaskWithNoCollector, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldExecuteAndCompleteTaskWithMultipleResults()
        {
        Utils.assertWithFailureAction(super::shouldExecuteAndCompleteTaskWithMultipleResults, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldExecuteAndCompleteTaskSequentially()
        {
        Utils.assertWithFailureAction(super::shouldExecuteAndCompleteTaskSequentially, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldGetTaskIdFromContext()
        {
        Utils.assertWithFailureAction(super::shouldGetTaskIdFromContext, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldGetExecutorIdFromContext()
        {
        Utils.assertWithFailureAction(super::shouldGetExecutorIdFromContext, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldExecuteAndCompleteNullTask()
        {
        Utils.assertWithFailureAction(super::shouldExecuteAndCompleteNullTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldExecuteAndNotCompleteTask()
        {
        Utils.assertWithFailureAction(super::shouldExecuteAndNotCompleteTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldCancelTask()
        {
        Utils.assertWithFailureAction(super::shouldCancelTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldAcquireCoordinatorForSubmittedTask()
        {
        Utils.assertWithFailureAction(super::shouldAcquireCoordinatorForSubmittedTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldRegisterAndDeRegisterExecutor()
        {
        Utils.assertWithFailureAction(super::shouldRegisterAndDeRegisterExecutor, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldAutomaticallyRemoveTaskWhenCompleted()
        {
        Utils.assertWithFailureAction(super::shouldAutomaticallyRemoveTaskWhenCompleted, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldAutomaticallySubscribe()
        {
        Utils.assertWithFailureAction(super::shouldAutomaticallySubscribe, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldPublishResultWithMultipleExecutors()
        {
        Utils.assertWithFailureAction(super::shouldPublishResultWithMultipleExecutors, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldCollectFirstResultWithMultipleExecutors()
        {
        Utils.assertWithFailureAction(super::shouldCollectFirstResultWithMultipleExecutors, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldCollectFirstResultWithAnyOf()
        {
        Utils.assertWithFailureAction(super::shouldCollectFirstResultWithAnyOf, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldCollectResultsInOrder()
        {
        Utils.assertWithFailureAction(super::shouldCollectResultsInOrder, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldSupportMultipleSubscribers()
        {
        Utils.assertWithFailureAction(super::shouldSupportMultipleSubscribers, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldHandleExceptionTask()
        {
        Utils.assertWithFailureAction(super::shouldHandleExceptionTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldHandleDuplicateTaskId()
        {
        Utils.assertWithFailureAction(super::shouldHandleDuplicateTaskId, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldHandleExecutorServiceShutdown()
        {
        Utils.assertWithFailureAction(super::shouldHandleExecutorServiceShutdown, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldHandleSubscriberException()
        {
        Utils.assertWithFailureAction(super::shouldHandleSubscriberException, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldYieldTask()
        {
        Utils.assertWithFailureAction(super::shouldYieldTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldHandleExceptionsInCallableFutures()
        {
        Utils.assertWithFailureAction(super::shouldHandleExceptionsInCallableFutures, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldHandleExceptionsInRunnableFutures()
        {
        Utils.assertWithFailureAction(super::shouldHandleExceptionsInRunnableFutures, this::dumpExecutorCacheStates);
        }

    @Override
    @Test(timeout = 300000) // timeout after five minutes
    public void shouldHandleTasksFailingInInvokeAny()
        {
        Utils.assertWithFailureAction(super::shouldHandleTasksFailingInInvokeAny, this::dumpExecutorCacheStates);
        }

    @Override
    @Test(timeout = 300000) // timeout after five minutes
    public void shouldUseDefaultExecutor()
        {
        Utils.assertWithFailureAction(super::shouldUseDefaultExecutor, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldHandleProperties()
        {
        Utils.assertWithFailureAction(super::shouldHandleProperties, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldCallRunnableAfterTaskComplete()
        {
        Utils.assertWithFailureAction(super::shouldCallRunnableAfterTaskComplete, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    @Ignore("disabled as completion no longer runs if task throws an exception")
    public void shouldCallRunnableAfterTaskCompleteWithException()
        {
        Utils.assertWithFailureAction(super::shouldCallRunnableAfterTaskCompleteWithException, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldChangeLogLevelWithDebugging()
        {
        Utils.assertWithFailureAction(super::shouldChangeLogLevelWithDebugging, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldRejectTaskSubmissionsAfterShutdown()
        {
        Utils.assertWithFailureAction(super::shouldRejectTaskSubmissionsAfterShutdown, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldRejectRegisterAfterShutdown()
        {
        Utils.assertWithFailureAction(super::shouldRejectRegisterAfterShutdown, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldShutdownGracefullyWithNoExecutorsNoTasks()
        {
        Utils.assertWithFailureAction(super::shouldShutdownGracefullyWithNoExecutorsNoTasks, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldShutdownGracefullyWithNoExecutorsWithTasks()
        {
        Utils.assertWithFailureAction(super::shouldShutdownGracefullyWithNoExecutorsWithTasks, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldShutdownGracefullyWithExecutorsNoTasks()
        {
        Utils.assertWithFailureAction(super::shouldShutdownGracefullyWithExecutorsNoTasks, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldShutdownGracefullyWithExecutorsWithTasks()
        {
        Utils.assertWithFailureAction(super::shouldShutdownGracefullyWithExecutorsWithTasks, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldShutdownNowWithNoExecutorsNoTasks()
        {
        Utils.assertWithFailureAction(super::shouldShutdownNowWithNoExecutorsNoTasks, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldShutdownNowWithNoExecutorsWithTasks()
        {
        Utils.assertWithFailureAction(super::shouldShutdownNowWithNoExecutorsWithTasks, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldShutdownNowWithExecutorsNoTasks()
        {
        Utils.assertWithFailureAction(super::shouldShutdownNowWithExecutorsNoTasks, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldShutdownNowWithExecutorsWithTasks()
        {
        Utils.assertWithFailureAction(super::shouldShutdownNowWithExecutorsWithTasks, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    @Ignore("https://jira.oraclecorp.com/jira/browse/COH-23998")
    public void shouldRetainTask()
        {
        Utils.assertWithFailureAction(super::shouldRetainTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldNotAssignNewTasksDuringGracefulShutdown()
        {
        Utils.assertWithFailureAction(super::shouldNotAssignNewTasksDuringGracefulShutdown, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldRemoveCacheEntriesOnCancel()
        {
        Utils.assertWithFailureAction(super::shouldRemoveCacheEntriesOnCancel, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldAutomaticallyDeregisterExecutor()
        {
        Utils.assertWithFailureAction(super::shouldAutomaticallyDeregisterExecutor, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldNotExpireRunningExecutors()
        {
        Utils.assertWithFailureAction(super::shouldNotExpireRunningExecutors, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldSetRoleFromMember()
        {
        Utils.assertWithFailureAction(super::shouldSetRoleFromMember, this::dumpExecutorCacheStates);
        }
    }
