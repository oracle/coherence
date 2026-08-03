/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.tasks.CronTask;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.oracle.coherence.concurrent.executor.options.CloseExecutor;
import com.oracle.coherence.concurrent.executor.options.ClusterMember;
import com.oracle.coherence.concurrent.executor.options.Description;
import com.oracle.coherence.concurrent.executor.options.Member;
import com.oracle.coherence.concurrent.executor.options.Name;
import com.oracle.coherence.concurrent.executor.options.Role;
import com.oracle.coherence.concurrent.executor.options.Storage;

import com.oracle.coherence.concurrent.executor.tasks.internal.CallableTask;
import com.oracle.coherence.concurrent.executor.tasks.internal.RunnableTask;
import com.oracle.coherence.concurrent.executor.tasks.internal.RunnableWithResultTask;

import com.tangosol.internal.util.security.RemoteInstallGate;

import com.tangosol.io.SerializationRole;

import com.tangosol.util.function.Remote;

import javax.security.auth.Subject;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.UUID;

/**
 * Subject-aware install-gate cascade for Coherence Concurrent task state.
 *
 * @author Aleks Seovic  2026.05.13
 * @since 26.07
 */
final class ConcurrentTaskInstallGate
    {
    /**
     * Enforce the complete manager cascade.
     *
     * @param manager  the manager to enforce
     */
    static void enforce(ClusteredTaskManager<?, ?, ?> manager)
        {
        if (manager == null)
            {
            return;
            }

        Subject subject = manager.getSubject();
        enforceTask(manager.m_task, subject, 0);
        enforceExecutionStrategy(manager.m_executionStrategy, subject, 0);
        enforceCollector(manager.m_collector, subject, 0);
        enforcePredicate(manager.m_completionPredicate, subject, 0);
        enforceExecutable(manager.m_completionRunnable, subject, 0);
        }

    /**
     * Enforce a subscriber before registration or callbacks.
     *
     * @param subscriber  the subscriber to enforce
     * @param subject     the advisory subject
     */
    static void enforceSubscriber(Task.Subscriber<?> subscriber, Subject subject)
        {
        enforceExecutable(subscriber, subject, 0);
        }

    /**
     * Enforce a task and known wrapper state.
     *
     * @param task     the task
     * @param subject  the advisory subject
     * @param cDepth   the cascade depth
     */
    static void enforceTask(Task<?> task, Subject subject, int cDepth)
        {
        if (task == null)
            {
            return;
            }

        enforceExecutable(task, subject, cDepth);
        if (task instanceof CallableTask)
            {
            enforceExecutable(((CallableTask<?>) task).getCallable(), subject, cDepth + 1);
            }
        if (task instanceof RunnableTask)
            {
            enforceExecutable(((RunnableTask) task).getRunnable(), subject, cDepth + 1);
            }
        if (task instanceof RunnableWithResultTask)
            {
            enforceExecutable(((RunnableWithResultTask<?>) task).getRunnable(), subject, cDepth + 1);
            }
        if (task instanceof CronTask)
            {
            CronTask<?> cronTask = (CronTask<?>) task;
            enforceTask(cronTask.getOriginalTask(), subject, cDepth + 1);
            enforceTask(cronTask.getTask(), subject, cDepth + 1);
            }
        }

    private static void enforceExecutionStrategy(ExecutionStrategy strategy, Subject subject, int cDepth)
        {
        if (strategy == null)
            {
            return;
            }

        enforceExecutable(strategy, subject, cDepth);
        if (strategy instanceof StandardExecutionStrategy)
            {
            enforcePredicate(((StandardExecutionStrategy) strategy).getPredicate(), subject, cDepth + 1);
            }
        }

    private static void enforceCollector(Task.Collector<?, ?, ?> collector, Subject subject, int cDepth)
        {
        if (collector == null)
            {
            return;
            }

        enforceExecutable(collector, subject, cDepth);
        if (collector instanceof ConditionalCollector)
            {
            ConditionalCollector<?, ?, ?> conditional = (ConditionalCollector<?, ?, ?>) collector;
            enforcePredicate(conditional.getPredicate(), subject, cDepth + 1);
            enforceCollector(conditional.getCollector(), subject, cDepth + 1);
            }
        }

    private static void enforcePredicate(Remote.Predicate<?> predicate, Subject subject, int cDepth)
        {
        if (predicate == null)
            {
            return;
            }

        enforceExecutable(predicate, subject, cDepth);
        if (predicate instanceof Predicates.NegatePredicate)
            {
            enforcePredicate(((Predicates.NegatePredicate<?>) predicate).getPredicate(), subject, cDepth + 1);
            }
        if (predicate instanceof Predicates.AndPredicate)
            {
            Predicates.AndPredicate<?> and = (Predicates.AndPredicate<?>) predicate;
            enforcePredicate(and.getLeft(), subject, cDepth + 1);
            enforcePredicate(and.getRight(), subject, cDepth + 1);
            }
        if (predicate instanceof Predicates.EqualToPredicate)
            {
            enforcePredicateValue(((Predicates.EqualToPredicate<?>) predicate).getValue(), subject, cDepth + 1);
            }
        if (predicate instanceof Predicates.OptionPredicate)
            {
            enforceOption(((Predicates.OptionPredicate) predicate).getOption(), subject, cDepth + 1);
            }
        if (predicate instanceof Predicates.ThrowablePredicate)
            {
            enforceThrowable(((Predicates.ThrowablePredicate<?>) predicate).getThrowable(), subject, cDepth + 1);
            }
        }

    private static void enforcePredicateValue(Object value, Subject subject, int cDepth)
        {
        if (value == null || isSafeDataValue(value))
            {
            return;
            }

        enforceExecutable(value, subject, cDepth);
        }

    private static void enforceOption(TaskExecutorService.Registration.Option option, Subject subject, int cDepth)
        {
        if (option == null)
            {
            return;
            }
        if (option.getClass() == Member.class)
            {
            enforceMemberOption((Member) option, subject, cDepth);
            return;
            }
        if (isSafeRegistrationOption(option))
            {
            return;
            }

        enforceExecutable(option, subject, cDepth);
        }

    private static void enforceMemberOption(Member option, Subject subject, int cDepth)
        {
        com.tangosol.net.Member member = option.get();
        if (member == null || isSafeClusterMember(member))
            {
            return;
            }

        enforceExecutable(member, subject, cDepth);
        }

    private static void enforceThrowable(Throwable throwable, Subject subject, int cDepth)
        {
        if (throwable == null || isSafeThrowable(throwable))
            {
            return;
            }

        enforceExecutable(throwable, subject, cDepth);
        }

    private static void enforceExecutable(Object executable, Subject subject, int cDepth)
        {
        RemoteInstallGate.enforceConcurrentTaskInstall(executable, SerializationRole.CONCURRENT, subject, cDepth);
        }

    private static boolean isSafeDataValue(Object value)
        {
        Class<?> clz = value.getClass();
        return clz == String.class
                || clz == Boolean.class
                || clz == Byte.class
                || clz == Character.class
                || clz == Short.class
                || clz == Integer.class
                || clz == Long.class
                || clz == Float.class
                || clz == Double.class
                || clz == BigInteger.class
                || clz == BigDecimal.class
                || clz == UUID.class
                || clz == Class.class
                || clz.isEnum();
        }

    private static boolean isSafeRegistrationOption(TaskExecutorService.Registration.Option option)
        {
        Class<?> clz = option.getClass();
        return clz == CloseExecutor.class
                || clz == ClusterMember.class
                || clz == Description.class
                || clz == Name.class
                || clz == Role.class
                || clz == Storage.class;
        }

    private static boolean isSafeClusterMember(com.tangosol.net.Member member)
        {
        return member.getClass().getName().equals("com.tangosol.coherence.component.net.Member");
        }

    private static boolean isSafeThrowable(Throwable throwable)
        {
        Class<?> clz = throwable.getClass();
        return clz.getClassLoader() == null && clz.getName().startsWith("java.");
        }

    private ConcurrentTaskInstallGate()
        {
        }
    }
