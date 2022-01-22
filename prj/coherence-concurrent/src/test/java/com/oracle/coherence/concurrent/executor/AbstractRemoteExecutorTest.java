/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.bedrock.testsupport.junit.AbstractTestLogs;
import com.tangosol.net.Coherence;

import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

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
    // ----- api ------------------------------------------------------------

    /**
     * Return the {@code client} member.
     *
     * @return the {@code client} member
     */
    protected abstract Coherence getClient();

    // ----- lifecycle methods ----------------------------------------------

    @BeforeEach
    public void beforeAll()
        {
        m_clientMember = getClient().start().join();
        try
            {
            Thread.sleep(4000);
            }
        catch (InterruptedException e)
            {
            e.printStackTrace();
            }
        }

    @AfterEach
    public void afterAll()
        {
        Coherence.closeAll();
        m_clientMember = null;
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldReturnDefaultExecutor()
        {
        RemoteExecutor def = RemoteExecutor.getDefault();

        assertThat(def, is(notNullValue()));
        assertThat(((NamedClusteredExecutorService) def).f_name.getName(), is(RemoteExecutor.DEFAULT_EXECUTOR_NAME));
        }

    @Test
    public void shouldGetNamedExecutor()
        {
        RemoteExecutor def = RemoteExecutor.get(RemoteExecutor.DEFAULT_EXECUTOR_NAME);

        assertThat(def, is(notNullValue()));
        assertThat(((NamedClusteredExecutorService) def).f_name.getName(), is(RemoteExecutor.DEFAULT_EXECUTOR_NAME));
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
     * Coherence client member (either a cluster member or an extend client).
     */
    protected Coherence m_clientMember;
    }
