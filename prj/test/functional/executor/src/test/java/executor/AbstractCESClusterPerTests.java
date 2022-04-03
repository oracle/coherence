/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package executor;

import executor.common.NewClusterPerTest;

import org.junit.Ignore;
import org.junit.Test;

import org.junit.experimental.categories.Category;

/**
 * Abstract class providing tests that require a new cluster for each test.
 *
 * @author rl 7.29.2009
 * @since 21.12
 */
@Category(NewClusterPerTest.class)
public abstract class AbstractCESClusterPerTests
        extends AbstractClusteredExecutorServiceTests
    {
    // ----- constructors ---------------------------------------------------

    public AbstractCESClusterPerTests(String extendConfig)
        {
        super(extendConfig);
        }

    // ----- test methods ----------------------------------------------------

    @Override
    @Test
    public void shouldExecuteAfterExecutorIsAdded()
        {
        super.shouldExecuteAfterExecutorIsAdded();
        }

    @Override
    @Test
    public void shouldCreateClusteredExecutor()
        {
        super.shouldCreateClusteredExecutor();
        }

    @Override
    @Test
    public void shouldWorkWithServerAdded()
        {
        super.shouldWorkWithServerAdded();
        }

    @Override
    @Test
    public void shouldWorkWithServerRemoved()
        {
        super.shouldWorkWithServerRemoved();
        }

    @Override
    @Test
    @Ignore("https://jira.oraclecorp.com/jira/browse/COH-23971")
    public void shouldFailOverLongRunningTest()
        {
        super.shouldFailOverLongRunningTest();
        }

    @Override
    @Test
    @Ignore("https://jira.oraclecorp.com/jira/browse/COH-23971")
    public void shouldCallRunnableAfterFailOverLongRunning()
        {
        super.shouldCallRunnableAfterFailOverLongRunning();
        }

    @Override
    @Test
    public void shouldFailOverRecoveringTest()
        {
        super.shouldFailOverRecoveringTest();
        }

    @Override
    @Test
    public void shouldAllowProxyRestartLongRunningTest()
        {
        super.shouldAllowProxyRestartLongRunningTest();
        }

    @Override
    @Test
    public void shouldAllowProxyFailoverLongRunningTest()
        {
        super.shouldAllowProxyFailoverLongRunningTest();
        }

    @Override
    @Test
    public void shouldAllowClientFailoverLongRunningTest()
        {
        super.shouldAllowClientFailoverLongRunningTest();
        }

    @Override
    @Test
    public void shouldWorkWithRollingRestart1()
        {
        super.shouldWorkWithRollingRestart1();
        }

    @Override
    @Test
    public void shouldWorkWithRollingRestart2()
        {
        super.shouldWorkWithRollingRestart2();
        }

    @Override
    @Test
    public void shouldHandleRollingRestartWithCronTask()
        {
        super.shouldHandleRollingRestartWithCronTask();
        }

    @Override
    @Test
    public void shouldHandleRemoveServerWithCronTask()
        {
        super.shouldHandleRemoveServerWithCronTask();
        }

    @Override
    @Test
    public void shouldHandleFailoverWithCronTask()
        {
        super.shouldHandleFailoverWithCronTask();
        }

    @Override
    @Test
    public void shouldNotExpireExecutorsWithRollingRestart()
        {
        super.shouldNotExpireExecutorsWithRollingRestart();
        }

    @Override
    @Test
    public void shouldSetStorageOption()
        {
        super.shouldSetStorageOption();
        }

    @Override
    @Test
    public void shouldUnregisterExecutorAfterReject()
        {
        super.shouldUnregisterExecutorAfterReject();
        }

    @Override
    @Test
    public void shouldDeregisterTerminatedExecutorService()
        {
        super.shouldDeregisterTerminatedExecutorService();
        }

    @Override
    @Test
    public void shouldScheduleYieldTask()
        {
        super.shouldScheduleYieldTask();
        }

    @Override
    @Test
    public void shouldUseScheduledExecutor() throws Exception
        {
        super.shouldUseScheduledExecutor();
        }

    @Override
    @Test
    public void shouldScheduleTaskToOneExecutor()
        {
        super.shouldScheduleTaskToOneExecutor();
        }

    @Override
    @Test
    public void shouldScheduleCronTaskToMultipleExecutors()
        {
        super.shouldScheduleCronTaskToMultipleExecutors();
        }

    @Override
    @Test
    public void shouldRunMultiLongRunningCronTasks()
        {
        super.shouldRunMultiLongRunningCronTasks();
        }

    @Override
    @Test
    @Ignore("https://jira.oraclecorp.com/jira/browse/COH-23971")
    public void shouldRunMultipleTasks()
        {
        super.shouldRunMultipleTasks();
        }
    }
