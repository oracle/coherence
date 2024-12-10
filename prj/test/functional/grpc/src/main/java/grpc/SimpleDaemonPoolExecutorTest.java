/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc;

import com.oracle.coherence.grpc.SimpleDaemonPoolExecutor;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.DaemonPoolDependencies;

import com.tangosol.run.xml.XmlElement;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2020.09.22
 * @since 20.06
 */
class SimpleDaemonPoolExecutorTest
    {
    // ----- test methods ---------------------------------------------------
    
    @Test
    public void shouldNotStartPoolOnCreation()
        {
        DaemonPool               pool     = mock(DaemonPool.class);
        new SimpleDaemonPoolExecutor(pool);

        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldExecuteTask()
        {
        Runnable                 task     = mock(Runnable.class);
        DaemonPool               pool     = mock(DaemonPool.class);
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor(pool);

        executor.execute(task);
        verify(pool).add(same(task));
        }

    @Test
    public void shouldSetThreadCount()
        {
        int                      cThreads = 19;
        int                      cMin     = -1;
        int                      cMax     = -1;
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor("foo", cThreads, cMin, cMax);
        DaemonPoolDependencies   deps     = executor.getPool().getDependencies();

        assertThat(deps.getThreadCount(), is(19));
        assertThat(deps.getThreadCountMin(), is(1));
        assertThat(deps.getThreadCountMax(), is(Integer.MAX_VALUE));
        }

    @Test
    public void shouldSetMinThreadCount()
        {
        int                      cThreads = -1;
        int                      cMin     = 19;
        int                      cMax     = -1;
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor("foo", cThreads, cMin, cMax);
        DaemonPoolDependencies   deps     = executor.getPool().getDependencies();

        assertThat(deps.getThreadCount(), is(1));
        assertThat(deps.getThreadCountMin(), is(19));
        assertThat(deps.getThreadCountMax(), is(Integer.MAX_VALUE));
        }

    @Test
    public void shouldSetMaxThreadCount()
        {
        int                      cThreads = -1;
        int                      cMin     = -1;
        int                      cMax     = 19;
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor("foo", cThreads, cMin, cMax);
        DaemonPoolDependencies   deps     = executor.getPool().getDependencies();

        assertThat(deps.getThreadCount(), is(1));
        assertThat(deps.getThreadCountMin(), is(1));
        assertThat(deps.getThreadCountMax(), is(19));
        }

    @Test
    public void shouldSetName()
        {
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor("foo");
        DaemonPoolDependencies   deps     = executor.getPool().getDependencies();
        assertThat(deps.getName(), is("foo"));
        }

    @Test
    public void shouldStartPool()
        {
        DaemonPool               pool     = mock(DaemonPool.class);
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor(pool);

        executor.start();
        verify(pool).start();
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldStopPool()
        {
        DaemonPool               pool     = mock(DaemonPool.class);
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor(pool);

        executor.stop();
        verify(pool).stop();
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldShutdownPool()
        {
        DaemonPool               pool     = mock(DaemonPool.class);
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor(pool);

        executor.shutdown();
        verify(pool).shutdown();
        verifyNoMoreInteractions(pool);
        }

    @Test
    @SuppressWarnings("deprecation")
    public void shouldConfigurePool()
        {
        XmlElement               xml      = mock(XmlElement.class);
        DaemonPool               pool     = mock(DaemonPool.class);
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor(pool);

        executor.configure(xml);
        verify(pool).configure(same(xml));
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldBeRunning()
        {
        DaemonPool               pool     = mock(DaemonPool.class);
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor(pool);

        when(pool.isRunning()).thenReturn(true);

        assertThat(executor.isRunning(), is(true));
        verify(pool).isRunning();
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldNotBeRunning()
        {
        DaemonPool               pool     = mock(DaemonPool.class);
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor(pool);

        when(pool.isRunning()).thenReturn(false);

        assertThat(executor.isRunning(), is(false));
        verify(pool).isRunning();
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldBeStuck()
        {
        DaemonPool               pool     = mock(DaemonPool.class);
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor(pool);

        when(pool.isStuck()).thenReturn(true);

        assertThat(executor.isStuck(), is(true));
        verify(pool).isStuck();
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldNotBeStuck()
        {
        DaemonPool               pool     = mock(DaemonPool.class);
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor(pool);

        when(pool.isStuck()).thenReturn(false);

        assertThat(executor.isStuck(), is(false));
        verify(pool).isStuck();
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldSetClassLoader()
        {
        ClassLoader              loader   = mock(ClassLoader.class);
        DaemonPool               pool     = mock(DaemonPool.class);
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor(pool);

        executor.setContextClassLoader(loader);
        verify(pool).setContextClassLoader(loader);
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldGetClassLoader()
        {
        ClassLoader              loader   = mock(ClassLoader.class);
        DaemonPool               pool     = mock(DaemonPool.class);
        SimpleDaemonPoolExecutor executor = new SimpleDaemonPoolExecutor(pool);

        when(pool.getContextClassLoader()).thenReturn(loader);

        assertThat(executor.getContextClassLoader(), is(sameInstance(loader)));
        verify(pool).getContextClassLoader();
        verifyNoMoreInteractions(pool);
        }
    }
