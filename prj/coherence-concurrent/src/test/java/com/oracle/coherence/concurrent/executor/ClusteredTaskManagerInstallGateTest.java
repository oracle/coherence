/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.function.Predicates;
import com.oracle.coherence.concurrent.executor.options.Member;
import com.oracle.coherence.concurrent.executor.options.Storage;
import com.oracle.coherence.concurrent.executor.util.OptionsByType;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.security.RemoteExecutionMode;

import com.tangosol.util.function.Remote;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.net.InetAddress;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ClusteredTaskManager} executable callback cascades.
 */
public class ClusteredTaskManagerInstallGateTest
    {
    @BeforeEach
    public void setUp()
        {
        m_sModeOld         = System.getProperty("coherence.mode");
        m_sSecurityModeOld = System.getProperty("coherence.security.mode");
        CoherenceModeHelper.restore("prod");
        CoherenceModeHelper.restoreSecurityMode("hardened");
        RemoteExecutionMode.resetForTesting();
        }

    @AfterEach
    public void tearDown()
        {
        if (m_sModeOld == null)
            {
            System.clearProperty("coherence.mode");
            }
        else
            {
            System.setProperty("coherence.mode", m_sModeOld);
            }
        if (m_sSecurityModeOld == null)
            {
            System.clearProperty("coherence.security.mode");
            }
        else
            {
            System.setProperty("coherence.security.mode", m_sSecurityModeOld);
            }
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    @Test
    public void shouldGateManagerTask()
        {
        assertThrows(SecurityException.class, () -> manager(new PlainTask(), null, null, null, null).enforceInstallGate());
        assertDoesNotThrow(() -> manager(new AnnotatedTask(), null, null, null, null).enforceInstallGate());
        }

    @Test
    public void shouldGateExecutionStrategyPredicate()
        {
        StandardExecutionStrategy strategyPlain = new StandardExecutionStrategy(1, new PlainExecutorPredicate(), true);

        assertThrows(SecurityException.class, () -> manager(new AnnotatedTask(), strategyPlain, null, null, null).enforceInstallGate());

        AtomicInteger cInvoked = new AtomicInteger();
        StandardExecutionStrategy strategyNestedPlain =
                new StandardExecutionStrategy(1, Predicates.not(new PlainExecutorPredicate(cInvoked)), true);

        assertThrows(SecurityException.class,
                () -> manager(new AnnotatedTask(), strategyNestedPlain, null, null, null).enforceInstallGate());
        assertEquals(0, cInvoked.get());

        StandardExecutionStrategy strategyNestedAnnotated =
                new StandardExecutionStrategy(1, Predicates.not(new AnnotatedExecutorPredicate()), true);

        assertDoesNotThrow(() -> manager(new AnnotatedTask(), strategyNestedAnnotated, null, null, null).enforceInstallGate());
        }

    @Test
    public void shouldGateConditionalCollectorPredicateAndNestedCollector()
        {
        ConditionalCollector<String, List<String>, String> collectorPlain =
                new ConditionalCollector<>(new PlainIteratorPredicate(), new AnnotatedCollector(), "default");

        assertThrows(SecurityException.class,
                () -> manager(new AnnotatedTask(), null, collectorPlain, null, null).enforceInstallGate());

        AtomicInteger cInvoked = new AtomicInteger();
        ConditionalCollector<String, List<String>, String> collectorNestedPlain =
                new ConditionalCollector<>(Predicates.not(new PlainIteratorPredicate(cInvoked)),
                new AnnotatedCollector(), "default");

        assertThrows(SecurityException.class,
                () -> manager(new AnnotatedTask(), null, collectorNestedPlain, null, null).enforceInstallGate());
        assertEquals(0, cInvoked.get());

        ConditionalCollector<String, List<String>, String> collectorNestedAnnotated =
                new ConditionalCollector<>(Predicates.not(new AnnotatedIteratorPredicate()),
                new AnnotatedCollector(), "default");

        assertDoesNotThrow(
                () -> manager(new AnnotatedTask(), null, collectorNestedAnnotated, null, null).enforceInstallGate());

        ConditionalCollector<String, List<String>, String> nested =
                new ConditionalCollector<>(new AnnotatedIteratorPredicate(), new PlainCollector(), "default");

        assertThrows(SecurityException.class, () -> manager(new AnnotatedTask(), null, nested, null, null).enforceInstallGate());
        }

    @Test
    public void shouldGateCompletionPredicateAndRunnableAndSubscriber()
        {
        assertThrows(SecurityException.class,
                () -> manager(new AnnotatedTask(), null, null, new PlainResultPredicate(), null).enforceInstallGate());

        AtomicInteger cInvoked = new AtomicInteger();

        assertThrows(SecurityException.class,
                () -> manager(new AnnotatedTask(), null, null, Predicates.not(new PlainResultPredicate(cInvoked)), null)
                        .enforceInstallGate());
        assertEquals(0, cInvoked.get());
        assertDoesNotThrow(
                () -> manager(new AnnotatedTask(), null, null, Predicates.not(new AnnotatedResultPredicate()), null)
                        .enforceInstallGate());

        assertThrows(SecurityException.class,
                () -> manager(new AnnotatedTask(), null, null, null, new PlainCompletionRunnable()).enforceInstallGate());
        assertThrows(SecurityException.class,
                () -> ConcurrentTaskInstallGate.enforceSubscriber(new PlainSubscriber(), null));
        }

    @Test
    public void shouldGateAndPredicateNestedPredicates()
        {
        AtomicInteger cInvoked = new AtomicInteger();
        StandardExecutionStrategy strategyPlain = new StandardExecutionStrategy(1,
                Predicates.and(Predicates.has(Storage.enabled()), new PlainExecutorPredicate(cInvoked)), true);

        assertThrows(SecurityException.class,
                () -> manager(new AnnotatedTask(), strategyPlain, null, null, null).enforceInstallGate());
        assertEquals(0, cInvoked.get());

        StandardExecutionStrategy strategyAnnotated = new StandardExecutionStrategy(1,
                Predicates.and(Predicates.has(Storage.enabled()), new AnnotatedExecutorPredicate()), true);

        assertDoesNotThrow(() -> manager(new AnnotatedTask(), strategyAnnotated, null, null, null).enforceInstallGate());
        }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldGateEqualToPredicateValueBeforeEquals()
        {
        AtomicInteger cInvoked = new AtomicInteger();
        StandardExecutionStrategy strategy = new StandardExecutionStrategy(1,
                (Remote.Predicate) Predicates.equalTo(new PlainEqualsPayload(cInvoked)), true);

        assertThrows(SecurityException.class,
                () -> manager(new AnnotatedTask(), strategy, null, null, null).enforceInstallGate());
        assertEquals(0, cInvoked.get());

        StandardExecutionStrategy strategySafe = new StandardExecutionStrategy(1,
                (Remote.Predicate) Predicates.equalTo("executor-1"), true);
        StandardExecutionStrategy strategyAnnotated = new StandardExecutionStrategy(1,
                (Remote.Predicate) Predicates.equalTo(new AnnotatedEqualsPayload()), true);

        assertDoesNotThrow(() -> manager(new AnnotatedTask(), strategySafe, null, null, null).enforceInstallGate());
        assertDoesNotThrow(() -> manager(new AnnotatedTask(), strategyAnnotated, null, null, null).enforceInstallGate());
        }

    @Test
    public void shouldGateOptionPredicateValueBeforeEquals()
        {
        AtomicInteger cInvoked = new AtomicInteger();
        StandardExecutionStrategy strategy = new StandardExecutionStrategy(1,
                Predicates.has(new PlainRegistrationOption(cInvoked)), true);

        assertThrows(SecurityException.class,
                () -> manager(new AnnotatedTask(), strategy, null, null, null).enforceInstallGate());
        assertEquals(0, cInvoked.get());

        StandardExecutionStrategy strategySafe = new StandardExecutionStrategy(1, Predicates.has(Storage.enabled()), true);
        StandardExecutionStrategy strategyAnnotated = new StandardExecutionStrategy(1,
                Predicates.has(new AnnotatedRegistrationOption()), true);

        assertDoesNotThrow(() -> manager(new AnnotatedTask(), strategySafe, null, null, null).enforceInstallGate());
        assertDoesNotThrow(() -> manager(new AnnotatedTask(), strategyAnnotated, null, null, null).enforceInstallGate());
        }

    @Test
    public void shouldGateMemberRegistrationOptionNestedMember()
        {
        AtomicInteger cInvoked = new AtomicInteger();
        StandardExecutionStrategy strategyPlain = new StandardExecutionStrategy(1,
                Predicates.has(Member.of(new PlainMember(cInvoked))), true);

        assertThrows(SecurityException.class,
                () -> manager(new AnnotatedTask(), strategyPlain, null, null, null).enforceInstallGate());
        assertEquals(0, cInvoked.get());

        StandardExecutionStrategy strategyNull = new StandardExecutionStrategy(1, Predicates.has(Member.of(null)), true);

        assertDoesNotThrow(() -> manager(new AnnotatedTask(), strategyNull, null, null, null).enforceInstallGate());
        }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldGateThrowablePredicateValueBeforeToString()
        {
        AtomicInteger cInvoked = new AtomicInteger();
        Remote.Predicate<? super String> predicate = (Remote.Predicate) Predicates.onException(new PlainThrowable(cInvoked));

        assertThrows(SecurityException.class,
                () -> manager(new AnnotatedTask(), null, null, predicate, null).enforceInstallGate());
        assertEquals(0, cInvoked.get());

        Remote.Predicate<? super String> predicateSafe =
                (Remote.Predicate) Predicates.onException(new RuntimeException("expected"));
        Remote.Predicate<? super String> predicateAnnotated =
                (Remote.Predicate) Predicates.onException(new AnnotatedThrowable());

        assertDoesNotThrow(() -> manager(new AnnotatedTask(), null, null, predicateSafe, null).enforceInstallGate());
        assertDoesNotThrow(() -> manager(new AnnotatedTask(), null, null, predicateAnnotated, null).enforceInstallGate());
        }

    @Test
    public void shouldTreatTaskOptionsAsData()
        {
        OptionsByType<Task.Option> options = OptionsByType.from(Task.Option.class, new Task.Option[] {new PlainOption()});
        ClusteredTaskManager<?, ?, ?> manager = new ClusteredTaskManager<>("test", new AnnotatedTask(),
                new ExecutionStrategyBuilder().build(), null, Predicates.never(), null, null, options);

        assertDoesNotThrow(manager::enforceInstallGate);
        }

    private static ClusteredTaskManager<String, ?, String> manager(Task<String> task, ExecutionStrategy strategy,
            Task.Collector<? super String, ?, String> collector, Remote.Predicate<? super String> predicate,
            Task.CompletionRunnable<? super String> runnable)
        {
        return new ClusteredTaskManager<>("test", task,
                strategy == null ? new ExecutionStrategyBuilder().build() : strategy,
                collector, predicate == null ? Predicates.never() : predicate, runnable, null, OptionsByType.empty());
        }

    @Remote.Executable
    public static class AnnotatedTask
            extends ConcurrentTaskInstallGateTest.AnnotatedTask
        {
        }

    public static class PlainTask
            extends ConcurrentTaskInstallGateTest.PlainTask
        {
        }

    public static class PlainExecutorPredicate
            implements Remote.Predicate<TaskExecutorService.ExecutorInfo>
        {
        public PlainExecutorPredicate()
            {
            this(null);
            }

        PlainExecutorPredicate(AtomicInteger cInvoked)
            {
            m_cInvoked = cInvoked;
            }

        @Override
        public boolean test(TaskExecutorService.ExecutorInfo info)
            {
            if (m_cInvoked != null)
                {
                m_cInvoked.incrementAndGet();
                }
            return true;
            }

        private final AtomicInteger m_cInvoked;
        }

    @Remote.Executable
    public static class AnnotatedExecutorPredicate
            extends PlainExecutorPredicate
        {
        }

    public static class PlainIteratorPredicate
            implements Remote.Predicate<Iterator<String>>
        {
        public PlainIteratorPredicate()
            {
            this(null);
            }

        PlainIteratorPredicate(AtomicInteger cInvoked)
            {
            m_cInvoked = cInvoked;
            }

        @Override
        public boolean test(Iterator<String> iterator)
            {
            if (m_cInvoked != null)
                {
                m_cInvoked.incrementAndGet();
                }
            return true;
            }

        private final AtomicInteger m_cInvoked;
        }

    @Remote.Executable
    public static class AnnotatedIteratorPredicate
            extends PlainIteratorPredicate
        {
        }

    public static class PlainResultPredicate
            implements Remote.Predicate<String>
        {
        public PlainResultPredicate()
            {
            this(null);
            }

        PlainResultPredicate(AtomicInteger cInvoked)
            {
            m_cInvoked = cInvoked;
            }

        @Override
        public boolean test(String value)
            {
            if (m_cInvoked != null)
                {
                m_cInvoked.incrementAndGet();
                }
            return true;
            }

        private final AtomicInteger m_cInvoked;
        }

    @Remote.Executable
    public static class AnnotatedResultPredicate
            extends PlainResultPredicate
        {
        }

    public static class PlainCompletionRunnable
            implements Task.CompletionRunnable<String>
        {
        @Override
        public void accept(String value)
            {
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

    @Remote.Executable
    public static class AnnotatedCollector
            extends PlainCollector
        {
        }

    public static class PlainCollector
            implements Task.Collector<String, List<String>, String>
        {
        @Override
        public BiConsumer<List<String>, String> accumulator()
            {
            return List::add;
            }

        @Override
        public Function<List<String>, String> finisher()
            {
            return list -> list.isEmpty() ? null : list.get(0);
            }

        @Override
        public Remote.Predicate<List<String>> finishable()
            {
            return list -> false;
            }

        @Override
        public Supplier<List<String>> supplier()
            {
            return java.util.ArrayList::new;
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

    public static class PlainSubscriber
            implements Task.Subscriber<String>
        {
        @Override
        public void onComplete()
            {
            }

        @Override
        public void onError(Throwable throwable)
            {
            }

        @Override
        public void onNext(String item)
            {
            }

        @Override
        public void onSubscribe(Task.Subscription<? extends String> subscription)
            {
            }
        }

    public static class PlainEqualsPayload
        {
        public PlainEqualsPayload()
            {
            this(null);
            }

        PlainEqualsPayload(AtomicInteger cInvoked)
            {
            m_cInvoked = cInvoked;
            }

        @Override
        public boolean equals(Object obj)
            {
            if (m_cInvoked != null)
                {
                m_cInvoked.incrementAndGet();
                }
            return obj instanceof PlainEqualsPayload;
            }

        @Override
        public int hashCode()
            {
            return 1;
            }

        private final AtomicInteger m_cInvoked;
        }

    @Remote.Executable
    public static class AnnotatedEqualsPayload
            extends PlainEqualsPayload
        {
        }

    public static class PlainRegistrationOption
            implements TaskExecutorService.Registration.Option
        {
        public PlainRegistrationOption()
            {
            this(null);
            }

        PlainRegistrationOption(AtomicInteger cInvoked)
            {
            m_cInvoked = cInvoked;
            }

        @Override
        public boolean equals(Object obj)
            {
            if (m_cInvoked != null)
                {
                m_cInvoked.incrementAndGet();
                }
            return obj instanceof PlainRegistrationOption;
            }

        @Override
        public int hashCode()
            {
            return 1;
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            }

        private final AtomicInteger m_cInvoked;
        }

    @Remote.Executable
    public static class AnnotatedRegistrationOption
            extends PlainRegistrationOption
        {
        }

    public static class PlainThrowable
            extends RuntimeException
        {
        public PlainThrowable()
            {
            this(null);
            }

        PlainThrowable(AtomicInteger cInvoked)
            {
            super("plain");
            m_cInvoked = cInvoked;
            }

        @Override
        public String toString()
            {
            if (m_cInvoked != null)
                {
                m_cInvoked.incrementAndGet();
                }
            return super.toString();
            }

        private final AtomicInteger m_cInvoked;
        }

    @Remote.Executable
    public static class AnnotatedThrowable
            extends PlainThrowable
        {
        }

    public static class PlainOption
            implements Task.Option
        {
        @Override
        public void readExternal(DataInput in) throws IOException
            {
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            }
        }

    public static class PlainMember
            implements com.tangosol.net.Member
        {
        PlainMember(AtomicInteger cInvoked)
            {
            m_cInvoked = cInvoked;
            }

        @Override
        public InetAddress getAddress()
            {
            return null;
            }

        @Override
        public int getPort()
            {
            return 0;
            }

        @Override
        public long getTimestamp()
            {
            return 0L;
            }

        @Override
        public com.tangosol.util.UID getUid()
            {
            return null;
            }

        @Override
        public int getId()
            {
            return 0;
            }

        @Override
        public String getClusterName()
            {
            return null;
            }

        @Override
        public int getMachineId()
            {
            return 0;
            }

        @Override
        public String getMachineName()
            {
            return null;
            }

        @Override
        public String getMemberName()
            {
            return null;
            }

        @Override
        public int getPriority()
            {
            return 0;
            }

        @Override
        public String getProcessName()
            {
            return null;
            }

        @Override
        public String getRackName()
            {
            return null;
            }

        @Override
        public String getSiteName()
            {
            return null;
            }

        @Override
        public String getRoleName()
            {
            return null;
            }

        @Override
        public boolean equals(Object obj)
            {
            m_cInvoked.incrementAndGet();
            return obj instanceof PlainMember;
            }

        @Override
        public int hashCode()
            {
            return 1;
            }

        private final AtomicInteger m_cInvoked;
        }

    private String m_sModeOld;

    private String m_sSecurityModeOld;
    }
