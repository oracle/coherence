/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.function.Predicates;
import com.oracle.coherence.concurrent.executor.processors.LocalOnlyProcessor;
import com.oracle.coherence.concurrent.executor.tasks.CronTask;
import com.oracle.coherence.concurrent.executor.tasks.ValueTask;
import com.oracle.coherence.concurrent.executor.tasks.internal.CallableTask;
import com.oracle.coherence.concurrent.executor.tasks.internal.RunnableTask;
import com.oracle.coherence.concurrent.executor.tasks.internal.RunnableWithResultTask;
import com.oracle.coherence.concurrent.executor.tasks.internal.ScheduledCallableTask;
import com.oracle.coherence.concurrent.executor.tasks.internal.ScheduledRunnableTask;
import com.oracle.coherence.concurrent.executor.util.OptionsByType;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.security.RemoteExecutionMode;
import com.tangosol.internal.util.security.RemoteInstallGate;

import com.tangosol.io.SerializationRole;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.InvocableMap;

import com.tangosol.util.function.Remote;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;

import java.time.Duration;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for concurrent task wrapper install-gate cascades.
 */
public class ConcurrentTaskInstallGateTest
    {
    @BeforeEach
    public void setUp()
        {
        m_sModeOld          = System.getProperty("coherence.mode");
        m_sClusterOld       = System.getProperty("coherence.cluster");
        m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        CoherenceModeHelper.restore("prod");
        System.setProperty("coherence.cluster", "cache01-f-finalfix-unit-" + System.nanoTime());
        System.clearProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        RemoteExecutionMode.resetForTesting();
        }

    @AfterEach
    public void tearDown()
        {
        restore("coherence.mode", m_sModeOld);
        restore("coherence.cluster", m_sClusterOld);
        restore(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        CacheFactory.shutdown();
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    @Test
    public void shouldAllowAnnotatedWrapperState()
        {
        assertDoesNotThrow(() -> enforce(new CallableTask<>(new AnnotatedCallable())));
        assertDoesNotThrow(() -> enforce(new RunnableTask(new AnnotatedRunnable())));
        assertDoesNotThrow(() -> enforce(new RunnableWithResultTask<>(new AnnotatedRunnable(), new PlainRunnable())));
        assertDoesNotThrow(() -> enforce(new ScheduledCallableTask<>(new AnnotatedCallable(), Duration.ZERO)));
        assertDoesNotThrow(() -> enforce(new ScheduledRunnableTask(new AnnotatedRunnable(), Duration.ZERO, null, null)));
        assertDoesNotThrow(() -> enforce(new CronTask<>(new AnnotatedTask(), "* * * * *", false)));
        assertDoesNotThrow(() -> enforce(new ValueTask<>(new PlainRunnable())));
        }

    @Test
    public void shouldRejectUnannotatedWrapperState()
        {
        assertThrows(SecurityException.class, () -> enforce(new CallableTask<>(new PlainCallable())));
        assertThrows(SecurityException.class, () -> enforce(new RunnableTask(new PlainRunnable())));
        assertThrows(SecurityException.class, () -> enforce(new RunnableWithResultTask<>(new PlainRunnable(), "ok")));
        assertThrows(SecurityException.class, () -> enforce(new ScheduledCallableTask<>(new PlainCallable(), Duration.ZERO)));
        assertThrows(SecurityException.class, () -> enforce(new ScheduledRunnableTask(new PlainRunnable(), Duration.ZERO, null, null)));
        assertThrows(SecurityException.class, () -> enforce(new CronTask<>(new PlainTask(), "* * * * *", false)));
        }

    @Test
    public void shouldGateCronOriginalAndCurrentTask()
            throws Exception
        {
        CronTask<String> cronTask = new CronTask<>(new AnnotatedTask(), "* * * * *", false);
        setField(cronTask, "m_origTask", new PlainTask());

        assertThrows(SecurityException.class, () -> enforce(cronTask));

        CronTask<String> secondCronTask = new CronTask<>(new AnnotatedTask(), "* * * * *", false);
        setField(secondCronTask, "m_task", new PlainTask());

        assertThrows(SecurityException.class, () -> enforce(secondCronTask));
        }

    @Test
    public void shouldTreatValueTaskValueAsData()
        {
        ValueTask<PlainRunnable> task = new ValueTask<>(new PlainRunnable());

        assertDoesNotThrow(() -> enforce(task));
        }

    @Test
    public void shouldGateLocalOnlyProcessorNestedProcessor()
        {
        PlainEntryProcessor.COUNT.set(0);

        SecurityException e = assertThrows(SecurityException.class,
                () -> enforceProcessor(LocalOnlyProcessor.of(new PlainEntryProcessor())));

        assertTrue(e.getMessage().contains(PlainEntryProcessor.class.getName()));
        assertEquals(0, PlainEntryProcessor.COUNT.get());
        assertDoesNotThrow(() -> enforceProcessor(LocalOnlyProcessor.of(new AnnotatedEntryProcessor())));
        }

    @Test
    public void shouldGateChainedProcessorNestedProcessors()
        {
        PlainManagerProcessor.COUNT.set(0);
        ClusteredTaskManager.ChainedProcessor chainPlain = ClusteredTaskManager.ChainedProcessor.empty()
                .andThen(new PlainManagerProcessor());

        SecurityException e = assertThrows(SecurityException.class, () -> enforceProcessor(chainPlain));

        assertTrue(e.getMessage().contains(PlainManagerProcessor.class.getName()));
        assertEquals(0, PlainManagerProcessor.COUNT.get());

        ClusteredTaskManager.ChainedProcessor chainAnnotated = ClusteredTaskManager.ChainedProcessor.empty()
                .andThen(new AnnotatedManagerProcessor());

        assertDoesNotThrow(() -> enforceProcessor(chainAnnotated));
        }

    private static void enforce(Task<?> task)
        {
        manager(task).enforceInstallGate();
        }

    private static void enforceProcessor(InvocableMap.EntryProcessor<?, ?, ?> processor)
        {
        RemoteInstallGate.enforceCacheProcessorInstall(processor, SerializationRole.CONCURRENT, null);
        }

    private static ClusteredTaskManager<?, ?, ?> manager(Task<?> task)
        {
        return new ClusteredTaskManager<>("test", task, new ExecutionStrategyBuilder().build(), null,
                Predicates.never(), null, null, OptionsByType.empty());
        }

    private static void restore(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    private static void setField(Object target, String sName, Object value)
            throws Exception
        {
        Field field = target.getClass().getDeclaredField(sName);
        field.setAccessible(true);
        field.set(target, value);
        }

    @Remote.Executable
    public static class AnnotatedCallable
            implements Callable<String>, Remote.Function<String, String>
        {
        @Override
        public String call()
            {
            return "ok";
            }

        @Override
        public String apply(String value)
            {
            return value;
            }
        }

    @Remote.Executable
    public static class AnnotatedRunnable
            implements Runnable
        {
        @Override
        public void run()
            {
            }
        }

    @Remote.Executable
    public static class AnnotatedTask
            implements Task<String>
        {
        @Override
        public String execute(Context<String> context)
            {
            return "ok";
            }

        @Override
        public void readExternal(DataInput in)
            {
            }

        @Override
        public void writeExternal(DataOutput out)
            {
            }
        }

    public static class PlainCallable
            implements Callable<String>
        {
        @Override
        public String call()
            {
            return "ok";
            }
        }

    public static class PlainRunnable
            implements Runnable, java.io.Serializable
        {
        @Override
        public void run()
            {
            }
        }

    public static class PlainTask
            implements Task<String>
        {
        @Override
        public String execute(Context<String> context)
            {
            return "ok";
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            }
        }

    public static class PlainEntryProcessor
            implements InvocableMap.EntryProcessor<Object, Object, Object>
        {
        @Override
        public Object process(InvocableMap.Entry<Object, Object> entry)
            {
            COUNT.incrementAndGet();
            return null;
            }

        static final AtomicInteger COUNT = new AtomicInteger();
        }

    @Remote.Executable
    public static class AnnotatedEntryProcessor
            extends PlainEntryProcessor
        {
        }

    public static class PlainManagerProcessor
            implements InvocableMap.EntryProcessor<String, ClusteredTaskManager, Void>
        {
        @Override
        public Void process(InvocableMap.Entry<String, ClusteredTaskManager> entry)
            {
            COUNT.incrementAndGet();
            return null;
            }

        static final AtomicInteger COUNT = new AtomicInteger();
        }

    @Remote.Executable
    public static class AnnotatedManagerProcessor
            extends PlainManagerProcessor
        {
        }

    private String m_sModeOld;

    private String m_sClusterOld;

    private String m_sDynamicRemoteOld;
    }
