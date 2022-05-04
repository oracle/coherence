/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor;

import executor.common.NewClusterPerTest;

import executor.common.Utils;

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
        Utils.assertWithFailureAction(super::shouldExecuteAfterExecutorIsAdded, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldCreateClusteredExecutor()
        {
        Utils.assertWithFailureAction(super::shouldCreateClusteredExecutor, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldWorkWithServerAdded()
        {
        Utils.assertWithFailureAction(super::shouldWorkWithServerAdded, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldWorkWithServerRemoved()
        {
        Utils.assertWithFailureAction(super::shouldWorkWithServerRemoved, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    @Ignore("https://jira.oraclecorp.com/jira/browse/COH-23971")
    public void shouldFailOverLongRunningTest()
        {
        Utils.assertWithFailureAction(super::shouldFailOverLongRunningTest, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    @Ignore("https://jira.oraclecorp.com/jira/browse/COH-23971")
    public void shouldCallRunnableAfterFailOverLongRunning()
        {
        Utils.assertWithFailureAction(super::shouldCallRunnableAfterFailOverLongRunning, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldFailOverRecoveringTest()
        {
        Utils.assertWithFailureAction(super::shouldFailOverRecoveringTest, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldAllowProxyRestartLongRunningTest()
        {
        Utils.assertWithFailureAction(super::shouldAllowProxyRestartLongRunningTest, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldAllowProxyFailoverLongRunningTest()
        {
        Utils.assertWithFailureAction(super::shouldAllowProxyFailoverLongRunningTest, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldAllowClientFailoverLongRunningTest()
        {
        Utils.assertWithFailureAction(super::shouldAllowClientFailoverLongRunningTest, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldWorkWithRollingRestart1()
        {
        Utils.assertWithFailureAction(super::shouldWorkWithRollingRestart1, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldWorkWithRollingRestart2()
        {
        Utils.assertWithFailureAction(super::shouldWorkWithRollingRestart2, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldHandleRollingRestartWithCronTask()
        {
        Utils.assertWithFailureAction(super::shouldHandleRollingRestartWithCronTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldHandleRemoveServerWithCronTask()
        {
        Utils.assertWithFailureAction(super::shouldHandleRemoveServerWithCronTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldHandleFailoverWithCronTask()
        {
        Utils.assertWithFailureAction(super::shouldHandleFailoverWithCronTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldNotExpireExecutorsWithRollingRestart()
        {
        Utils.assertWithFailureAction(super::shouldNotExpireExecutorsWithRollingRestart, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldSetStorageOption()
        {
        Utils.assertWithFailureAction(super::shouldSetStorageOption, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldUnregisterExecutorAfterReject()
        {
        Utils.assertWithFailureAction(super::shouldUnregisterExecutorAfterReject, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldDeregisterTerminatedExecutorService()
        {
        Utils.assertWithFailureAction(super::shouldDeregisterTerminatedExecutorService, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldScheduleYieldTask()
        {
        Utils.assertWithFailureAction(super::shouldScheduleYieldTask, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldUseScheduledExecutor()
        {
        Utils.assertWithFailureAction(super::shouldUseScheduledExecutor, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldScheduleTaskToOneExecutor()
        {
        Utils.assertWithFailureAction(super::shouldScheduleTaskToOneExecutor, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldScheduleCronTaskToMultipleExecutors()
        {
        Utils.assertWithFailureAction(super::shouldScheduleCronTaskToMultipleExecutors, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    public void shouldRunMultiLongRunningCronTasks()
        {
        Utils.assertWithFailureAction(super::shouldRunMultiLongRunningCronTasks, this::dumpExecutorCacheStates);
        }

    @Override
    @Test
    @Ignore("https://jira.oraclecorp.com/jira/browse/COH-23971")
    public void shouldRunMultipleTasks()
        {
        Utils.assertWithFailureAction(super::shouldRunMultipleTasks, this::dumpExecutorCacheStates);
        }
    }
