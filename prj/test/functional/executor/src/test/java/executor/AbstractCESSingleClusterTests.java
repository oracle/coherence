/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package executor;

import executor.common.SingleClusterForAllTests;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.junit.experimental.categories.Category;

import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;

import org.junit.runner.Description;

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
        super.shouldCreateExecutorService();
        }

    @Override
    @Test
    public void shouldExecuteAndCompleteTask()
        {
        super.shouldExecuteAndCompleteTask();
        }

    @Override
    @Test
    public void shouldExecuteAndCompleteTaskWithNoCollector()
        {
        super.shouldExecuteAndCompleteTaskWithNoCollector();
        }

    @Override
    @Test
    public void shouldExecuteAndCompleteTaskWithMultipleResults()
        {
        super.shouldExecuteAndCompleteTaskWithMultipleResults();
        }

    @Override
    @Test
    public void shouldExecuteAndCompleteTaskSequentially()
        {
        super.shouldExecuteAndCompleteTaskSequentially();
        }

    @Override
    @Test
    public void shouldGetTaskIdFromContext()
        {
        super.shouldGetTaskIdFromContext();
        }

    @Override
    @Test
    public void shouldGetExecutorIdFromContext()
        {
        super.shouldGetExecutorIdFromContext();
        }

    @Override
    @Test
    public void shouldExecuteAndCompleteNullTask()
        {
        super.shouldExecuteAndCompleteNullTask();
        }

    @Override
    @Test
    public void shouldExecuteAndNotCompleteTask()
        {
        super.shouldExecuteAndNotCompleteTask();
        }

    @Override
    @Test
    public void shouldCancelTask()
        {
        super.shouldCancelTask();
        }

    @Override
    @Test
    public void shouldAcquireCoordinatorForSubmittedTask()
        {
        super.shouldAcquireCoordinatorForSubmittedTask();
        }

    @Override
    @Test
    public void shouldRegisterAndDeRegisterExecutor()
        {
        super.shouldRegisterAndDeRegisterExecutor();
        }

    @Override
    @Test
    public void shouldAutomaticallyRemoveTaskWhenCompleted()
        {
        super.shouldAutomaticallyRemoveTaskWhenCompleted();
        }

    @Override
    @Test
    public void shouldAutomaticallySubscribe()
        {
        super.shouldAutomaticallySubscribe();
        }

    @Override
    @Test
    public void shouldPublishResultWithMultipleExecutors()
        {
        super.shouldPublishResultWithMultipleExecutors();
        }

    @Override
    @Test
    public void shouldCollectFirstResultWithMultipleExecutors()
        {
        super.shouldCollectFirstResultWithMultipleExecutors();
        }

    @Override
    @Test
    public void shouldCollectFirstResultWithAnyOf()
        {
        super.shouldCollectFirstResultWithAnyOf();
        }

    @Override
    @Test
    public void shouldCollectResultsInOrder()
        {
        super.shouldCollectResultsInOrder();
        }

    @Override
    @Test
    public void shouldSupportMultipleSubscribers()
        {
        super.shouldSupportMultipleSubscribers();
        }

    @Override
    @Test
    public void shouldHandleExceptionTask()
        {
        super.shouldHandleExceptionTask();
        }

    @Override
    @Test
    public void shouldHandleDuplicateTaskId()
        {
        super.shouldHandleDuplicateTaskId();
        }

    @Override
    @Test
    public void shouldHandleExecutorServiceShutdown() throws Exception
        {
        super.shouldHandleExecutorServiceShutdown();
        }

    @Override
    @Test
    public void shouldHandleSubscriberException()
        {
        super.shouldHandleSubscriberException();
        }

    @Override
    @Test
    public void shouldYieldTask()
        {
        super.shouldYieldTask();
        }

    @Override
    @Test
    public void shouldHandleExceptionsInCallableFutures()
        {
        super.shouldHandleExceptionsInCallableFutures();
        }

    @Override
    @Test
    public void shouldHandleExceptionsInRunnableFutures()
        {
        super.shouldHandleExceptionsInRunnableFutures();
        }

    @Override
    @Test
    public void shouldHandleTasksFailingInInvokeAny()
        {
        super.shouldHandleTasksFailingInInvokeAny();
        }

    @Override
    @Test
    public void shouldUseDefaultExecutor() throws Exception
        {
        super.shouldUseDefaultExecutor();
        }

    @Override
    @Test
    public void shouldHandleProperties()
        {
        super.shouldHandleProperties();
        }

    @Override
    @Test
    public void shouldCallRunnableAfterTaskComplete()
        {
        super.shouldCallRunnableAfterTaskComplete();
        }

    @Override
    @Test
    @Ignore("disabled as completion no longer runs if task throws an exception")
    public void shouldCallRunnableAfterTaskCompleteWithException()
        {
        super.shouldCallRunnableAfterTaskCompleteWithException();
        }

    @Override
    @Test
    public void shouldChangeLogLevelWithDebugging()
        {
        super.shouldChangeLogLevelWithDebugging();
        }

    @Override
    @Test
    public void shouldRejectTaskSubmissionsAfterShutdown()
        {
        super.shouldRejectTaskSubmissionsAfterShutdown();
        }

    @Override
    @Test
    public void shouldRejectRegisterAfterShutdown()
        {
        super.shouldRejectRegisterAfterShutdown();
        }

    @Override
    @Test
    public void shouldShutdownGracefullyWithNoExecutorsNoTasks()
        {
        super.shouldShutdownGracefullyWithNoExecutorsNoTasks();
        }

    @Override
    @Test
    public void shouldShutdownGracefullyWithNoExecutorsWithTasks()
        {
        super.shouldShutdownGracefullyWithNoExecutorsWithTasks();
        }

    @Override
    @Test
    public void shouldShutdownGracefullyWithExecutorsNoTasks()
        {
        super.shouldShutdownGracefullyWithExecutorsNoTasks();
        }

    @Override
    @Test
    public void shouldShutdownGracefullyWithExecutorsWithTasks()
        {
        super.shouldShutdownGracefullyWithExecutorsWithTasks();
        }

    @Override
    @Test
    public void shouldShutdownNowWithNoExecutorsNoTasks()
        {
        super.shouldShutdownNowWithNoExecutorsNoTasks();
        }

    @Override
    @Test
    public void shouldShutdownNowWithNoExecutorsWithTasks()
        {
        super.shouldShutdownNowWithNoExecutorsWithTasks();
        }

    @Override
    @Test
    public void shouldShutdownNowWithExecutorsNoTasks()
        {
        super.shouldShutdownNowWithExecutorsNoTasks();
        }

    @Override
    @Test
    public void shouldShutdownNowWithExecutorsWithTasks()
        {
        super.shouldShutdownNowWithExecutorsWithTasks();
        }

    @Override
    @Test
    @Ignore("https://jira.oraclecorp.com/jira/browse/COH-23998")
    public void shouldRetainTask() throws InterruptedException
        {
        super.shouldRetainTask();
        }

    @Override
    @Test
    public void shouldNotAssignNewTasksDuringGracefulShutdown()
        {
        super.shouldNotAssignNewTasksDuringGracefulShutdown();
        }

    @Override
    @Test
    public void shouldRemoveCacheEntriesOnCancel()
        {
        super.shouldRemoveCacheEntriesOnCancel();
        }

    @Override
    @Test
    public void shouldAutomaticallyDeregisterExecutor()
        {
        super.shouldAutomaticallyDeregisterExecutor();
        }

    @Override
    @Test
    public void shouldNotExpireRunningExecutors()
        {
        super.shouldNotExpireRunningExecutors();
        }

    @Override
    @Test
    public void shouldSetRoleFromMember()
        {
        super.shouldSetRoleFromMember();
        }

    // ----- data members ---------------------------------------------------

    /**
     * Rule to demarcate tests in a single-log test run.
     */
    @Rule
    public final TestRule f_watcher = new TestWatcher()
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
    }
