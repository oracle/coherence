/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.executor;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.AbstractTestLogs;

import com.oracle.coherence.concurrent.executor.NamedClusteredExecutorService;
import com.oracle.coherence.concurrent.executor.RemoteExecutor;
import com.oracle.coherence.concurrent.executor.Task;

import com.oracle.coherence.concurrent.executor.subscribers.RecordingSubscriber;

import com.oracle.coherence.concurrent.executor.tasks.ValueTask;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;

import com.tangosol.util.Base;

import com.tangosol.util.function.Remote;

import java.time.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.hamcrest.MatcherAssert;

import org.junit.Assume;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for RemoteExecutor being called from within extend or a cluster
 * member.
 *
 * @author rl   7.12.2021
 * @since 21.12
 */
public abstract class AbstractRemoteExecutorTest
    {
    static
        {
        System.setProperty("coherence.lambdas", "dynamic");
        }

    // ----- api ------------------------------------------------------------

    /**
     * Return the {@code client} member.
     *
     * @return the {@code client} member
     */
    protected abstract Coherence getClient();

    // ----- lifecycle methods ----------------------------------------------

    @BeforeAll
    public static void beforeAll()
        {
        Assume.assumeFalse(Boolean.getBoolean("coverage.enabled"));
        }

    @BeforeEach
    public void beforeEach()
        {
        m_clientMember = getClient().start().join();
        Base.sleep(4000);
        }

    @AfterEach
    public void afterAll()
        {
        Coherence.closeAll();
        CacheFactory.shutdown();
        m_clientMember = null;
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldReturnDefaultExecutor()
        {
        RemoteExecutor def = RemoteExecutor.getDefault();

        assertThat(def, is(notNullValue()));
        MatcherAssert.assertThat(((NamedClusteredExecutorService) def).getName().getName(), is(RemoteExecutor.DEFAULT_EXECUTOR_NAME));
        }

    @Test
    public void shouldExecuteOnDefaultExecutor()
            throws Exception
        {
        RemoteExecutor def = RemoteExecutor.getDefault();

        assertThat(def, is(notNullValue()));
        MatcherAssert.assertThat(((NamedClusteredExecutorService) def).getName().getName(), is(RemoteExecutor.DEFAULT_EXECUTOR_NAME));
        Future<String> future = def.submit(() -> "Executed");
        assertThat(future.get(), is("Executed"));
        }

    @Test
    public void shouldGetNamedExecutor()
            throws Exception
        {
        RemoteExecutor def = RemoteExecutor.get(RemoteExecutor.DEFAULT_EXECUTOR_NAME);

        assertThat(def, is(notNullValue()));
        assertThat(((NamedClusteredExecutorService) def).getName().getName(), is(RemoteExecutor.DEFAULT_EXECUTOR_NAME));
        Future<String> future = def.submit(() -> "Executed");
        assertThat(future.get(), is("Executed"));
        }

    @Test
    public void shouldThrowWhenNoNamedExecutorFound()
        {
        RemoteExecutor def = RemoteExecutor.get("unknown");

        assertThat(def, is(notNullValue()));

        Exception exception = assertThrows(RejectedExecutionException.class,
                () -> { def.execute(() -> System.out.println("Shouldn't Run")); });

        assertThat(exception.getMessage(), is("No RemoteExecutor service available by name [Name{unknown}]"));
        }

    @Test
    public void shouldExecuteAndCompleteTask()
        {
        RemoteExecutor def = RemoteExecutor.getDefault();

        RecordingSubscriber<String> subscriber  = new RecordingSubscriber<>();
        Task.Coordinator<String>    coordinator = def.submit(new ValueTask<>("Hello World"));

        coordinator.subscribe(subscriber);

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        assertThat(subscriber.getLast(), is("Hello World"));
        }

    @Test
    public void shouldOrchestrateAndCompleteTask()
        {
        RemoteExecutor def = RemoteExecutor.getDefault();

        RecordingSubscriber<String> subscriber    = new RecordingSubscriber<>();
        Task.Orchestration<String>  orchestration = def.orchestrate(new ValueTask<>("Hello World"));

        orchestration.limit(1).subscribe(subscriber).submit();

        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        assertThat(subscriber.size(),    is(1));
        assertThat(subscriber.getLast(), is("Hello World"));
        }

    @Test
    public void shouldObtainCoordinatorForTask()
        {
        RemoteExecutor def = RemoteExecutor.getDefault();

        RecordingSubscriber<String> subscriber    = new RecordingSubscriber<>();
        Task.Orchestration<String>  orchestration = def.orchestrate(new ValueTask<>("Hello World"));
        Task.Coordinator<String>    coordinator   = orchestration.limit(1).retain(Duration.ofMillis(3000)).submit();

        Base.sleep(1000);

        // get the coordinator from the task ID
        Task.Coordinator<String> coordinatorFromId = def.acquire(coordinator.getTaskId());
        assertThat(coordinatorFromId,             is(notNullValue()));
        assertThat(coordinatorFromId.getTaskId(), is(coordinator.getTaskId()));

        coordinatorFromId.subscribe(subscriber);
        Eventually.assertDeferred(subscriber::isCompleted, is(true));
        assertThat(subscriber.size(),    is(1));
        assertThat(subscriber.getLast(), is("Hello World"));

        Base.sleep(3000);
        assertThat(def.acquire(coordinator.getTaskId()), is(nullValue()));
        }

    @Test
    public void shouldThrowExpectedExceptions()
        {
        Collection<Remote.Callable<String>> listCallablesWithNull = new ArrayList<>();
        Collection<Remote.Callable<String>> listCallables         = List.of(() -> "a", () -> "b", () -> "c");
        RemoteExecutor                      def                   = RemoteExecutor.getDefault();

        listCallablesWithNull.add(null);

        assertThrows(NullPointerException.class,     () -> def.acquire(null));

        assertThrows(NullPointerException.class,     () -> def.orchestrate(null));

        assertThrows(NullPointerException.class,     () -> def.submit((Task<?>) null));
        assertThrows(NullPointerException.class,     () -> def.submit((Remote.Callable<?>) null));
        assertThrows(NullPointerException.class,     () -> def.submit((Remote.Runnable) null));
        assertThrows(NullPointerException.class,     () -> def.submit(null, "result"));

        assertThrows(NullPointerException.class,     () -> def.schedule((Remote.Runnable)    null, 1, TimeUnit.SECONDS));
        assertThrows(NullPointerException.class,     () -> def.schedule((Remote.Callable<?>) null, 1, TimeUnit.SECONDS));

        assertThrows(NullPointerException.class,     () -> def.schedule( () -> {}, 1, null));
        assertThrows(NullPointerException.class,     () -> def.schedule(() -> "a", 1, null));

        assertThrows(NullPointerException.class,     () -> def.scheduleAtFixedRate(    null, 1,  1, TimeUnit.SECONDS));
        assertThrows(NullPointerException.class,     () -> def.scheduleAtFixedRate(() -> {}, 1,  1, null));
        assertThrows(IllegalArgumentException.class, () -> def.scheduleAtFixedRate(() -> {}, 1,  0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> def.scheduleAtFixedRate(() -> {}, 1, -1, TimeUnit.SECONDS));

        assertThrows(NullPointerException.class,     () -> def.scheduleWithFixedDelay(    null, 1,  1, TimeUnit.SECONDS));
        assertThrows(NullPointerException.class,     () -> def.scheduleWithFixedDelay(() -> {}, 1,  1, null));
        assertThrows(IllegalArgumentException.class, () -> def.scheduleWithFixedDelay(() -> {}, 1,  0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> def.scheduleWithFixedDelay(() -> {}, 1, -1, TimeUnit.SECONDS));

        assertThrows(NullPointerException.class,     () -> def.invokeAll(null));
        assertThrows(NullPointerException.class,     () -> def.invokeAll(listCallablesWithNull));

        assertThrows(NullPointerException.class,     () -> def.invokeAny(null));
        assertThrows(NullPointerException.class,     () -> def.invokeAny(listCallablesWithNull));

        assertThrows(NullPointerException.class,     () -> def.invokeAll(null,                  1, TimeUnit.SECONDS));
        assertThrows(NullPointerException.class,     () -> def.invokeAll(listCallablesWithNull, 1, TimeUnit.SECONDS));
        assertThrows(NullPointerException.class,     () -> def.invokeAll(listCallables,         1, null));

        assertThrows(NullPointerException.class,     () -> def.invokeAny(null,                  1, TimeUnit.SECONDS));
        assertThrows(NullPointerException.class,     () -> def.invokeAny(listCallablesWithNull, 1, TimeUnit.SECONDS));
        assertThrows(NullPointerException.class,     () -> def.invokeAny(listCallables,         1, null));
        }

    // ----- inner class: TestLogs ------------------------------------------

    /**
     * This is a work-around to fix the fact that the JUnit5 test logs extension
     * in Bedrock does not work for BeforeAll methods and extensions.
     */
    static class TestLogs
            extends AbstractTestLogs
        {
        public TestLogs(Class<?> testClass)
            {
            init(testClass, "BeforeAll");
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Coherence client member (either a cluster member or an Extend client).
     */
    protected Coherence m_clientMember;
    }
