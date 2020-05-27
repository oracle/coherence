/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.DaemonPoolDependencies;

import com.tangosol.net.management.Registry;

import com.tangosol.run.xml.XmlElement;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import java.util.Properties;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2019.11.27
 * @since 14.1.2
 */
class DaemonPoolExecutorTest
    {
    // ----- test methods ---------------------------------------------------
    
    @Test
    public void shouldNotStartPoolOnCreation()
        {
        DaemonPool pool = mock(DaemonPool.class);
        Supplier   fn   = new Supplier(pool);
        
        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);
        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));
        verify(supplier).apply(any(DaemonPoolDependencies.class));
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldCreateExecutor()
        {
        Runnable           task     = mock(Runnable.class);
        DaemonPoolExecutor executor = DaemonPoolExecutor.create();
        assertThat(executor, is(notNullValue()));

        executor.execute(task);
        verify(task).run();
        }

    @Test
    public void shouldExecuteTask()
        {
        DaemonPool pool = mock(DaemonPool.class);
        Supplier   fn   = new Supplier(pool);
        
        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);
        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));

        Runnable task = mock(Runnable.class);
        executor.execute(task);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(pool).add(captor.capture());
        Runnable runnable = captor.getValue();
        assertThat(runnable, is(instanceOf(TracingDaemonPool.TracingRunnable.class)));
        assertThat(((TracingDaemonPool.TracingRunnable) runnable).getDelegate(), is(sameInstance(task)));
        verify(supplier).apply(any(DaemonPoolDependencies.class));
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldSetThreadCount()
        {
        DaemonPool pool = mock(DaemonPool.class);
        
        Function<DaemonPoolDependencies, DaemonPool> fn       = new Supplier(pool);
        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        Properties props = new Properties();
        props.setProperty("pool." + DaemonPoolExecutor.CONFIG_THREAD_COUNT, "19");

        Config config = Config.create(() -> ConfigSources.create(props).build());

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder(config.get("pool"))
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));
        ArgumentCaptor<DaemonPoolDependencies> captor = ArgumentCaptor.forClass(DaemonPoolDependencies.class);
        verify(supplier).apply(captor.capture());

        DaemonPoolDependencies deps = captor.getValue();
        assertThat(deps.getThreadCount(), is(19));
        assertThat(deps.getThreadCountMax(), is(Integer.MAX_VALUE));
        assertThat(deps.getThreadCountMin(), is(1));
        }

    @Test
    public void shouldSetMaxThreadCount()
        {
        DaemonPool pool = mock(DaemonPool.class);
        
        Function<DaemonPoolDependencies, DaemonPool> fn       = new Supplier(pool);
        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        Properties props = new Properties();
        props.setProperty("pool." + DaemonPoolExecutor.CONFIG_THREAD_COUNT_MAX, "19");

        Config config = Config.create(() -> ConfigSources.create(props).build());

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder(config.get("pool"))
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));
        ArgumentCaptor<DaemonPoolDependencies> captor = ArgumentCaptor.forClass(DaemonPoolDependencies.class);
        verify(supplier).apply(captor.capture());

        DaemonPoolDependencies deps = captor.getValue();
        assertThat(deps.getThreadCountMax(), is(19));
        assertThat(deps.getThreadCountMin(), is(1));
        assertThat(deps.getThreadCount(), is(1));
        }

    @Test
    public void shouldSetMinThreadCount()
        {
        DaemonPool pool = mock(DaemonPool.class);
        
        Function<DaemonPoolDependencies, DaemonPool> fn       = new Supplier(pool);
        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        Properties props = new Properties();
        props.setProperty("pool." + DaemonPoolExecutor.CONFIG_THREAD_COUNT_MIN, "19");

        Config config = Config.create(() -> ConfigSources.create(props).build());

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder(config.get("pool"))
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));
        ArgumentCaptor<DaemonPoolDependencies> captor = ArgumentCaptor.forClass(DaemonPoolDependencies.class);
        verify(supplier).apply(captor.capture());

        DaemonPoolDependencies deps = captor.getValue();
        assertThat(deps.getThreadCountMax(), is(Integer.MAX_VALUE));
        assertThat(deps.getThreadCountMin(), is(19));
        assertThat(deps.getThreadCount(), is(1));
        }

    @Test
    public void shouldSetName()
        {
        DaemonPool pool = mock(DaemonPool.class);
        
        Function<DaemonPoolDependencies, DaemonPool> fn       = new Supplier(pool);
        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .name("foo")
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));
        ArgumentCaptor<DaemonPoolDependencies> captor = ArgumentCaptor.forClass(DaemonPoolDependencies.class);
        verify(supplier).apply(captor.capture());

        DaemonPoolDependencies deps = captor.getValue();
        assertThat(deps.getName(), is("foo"));
        }

    @Test
    public void shouldStartPool()
        {
        DaemonPoolDependencies dependencies = mock(DaemonPoolDependencies.class);
        DaemonPool             pool         = mock(com.tangosol.coherence.component.util.DaemonPool.class);
        Registry               registry     = mock(Registry.class);
        String                 sMBeanName   = "Coherence:name=foo";

        when(registry.ensureGlobalName(anyString())).thenReturn(sMBeanName);
        when(pool.getDependencies()).thenReturn(dependencies);
        when(dependencies.getName()).thenReturn("foo");

        Supplier fn = new Supplier(pool);
        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .registry(registry)
                .build();

        assertThat(executor, is(notNullValue()));
        executor.start();
        verify(pool).start();
        verify(pool).getDependencies();
        verifyNoMoreInteractions(pool);
        verify(registry).register(eq(sMBeanName), any());
        }

    @Test
    public void shouldStopPool()
        {
        DaemonPoolDependencies dependencies = mock(DaemonPoolDependencies.class);
        DaemonPool             pool         = mock(DaemonPool.class);
        Registry               registry     = mock(Registry.class);
        String                 sMBeanName   = "Coherence:name=foo";

        when(registry.ensureGlobalName(anyString())).thenReturn(sMBeanName);
        when(pool.getDependencies()).thenReturn(dependencies);
        when(dependencies.getName()).thenReturn("foo");

        Supplier fn = new Supplier(pool);

        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .registry(registry)
                .build();

        assertThat(executor, is(notNullValue()));
        executor.stop();
        verify(pool).stop();
        verify(pool).getDependencies();
        verifyNoMoreInteractions(pool);
        verify(registry).unregister(sMBeanName);
        }

    @Test
    public void shouldShutdownPool()
        {
        DaemonPoolDependencies dependencies = mock(DaemonPoolDependencies.class);
        DaemonPool             pool         = mock(DaemonPool.class);
        Registry               registry     = mock(Registry.class);
        String                 sMBeanName   = "Coherence:name=foo";

        when(registry.ensureGlobalName(anyString())).thenReturn(sMBeanName);
        when(pool.getDependencies()).thenReturn(dependencies);
        when(dependencies.getName()).thenReturn("foo");

        Supplier fn = new Supplier(pool);

        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .registry(registry)
                .build();

        assertThat(executor, is(notNullValue()));
        executor.shutdown();
        verify(pool).shutdown();
        verify(pool).getDependencies();
        verifyNoMoreInteractions(pool);
        verify(registry).unregister(sMBeanName);
        }

    @Test
    @SuppressWarnings("deprecation")
    public void shouldConfigurePool()
        {
        DaemonPool pool = mock(DaemonPool.class);
        Supplier   fn   = new Supplier(pool);

        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));

        XmlElement xml = mock(XmlElement.class);
        executor.configure(xml);
        verify(pool).configure(same(xml));
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldBeRunning()
        {
        DaemonPool pool = mock(DaemonPool.class);

        when(pool.isRunning()).thenReturn(true);

        Supplier fn = new Supplier(pool);

        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));
        assertThat(executor.isRunning(), is(true));
        verify(pool).isRunning();
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldNotBeRunning()
        {
        DaemonPool pool = mock(DaemonPool.class);

        when(pool.isRunning()).thenReturn(false);

        Supplier fn = new Supplier(pool);

        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));
        assertThat(executor.isRunning(), is(false));
        verify(pool).isRunning();
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldBeStuck()
        {
        DaemonPool pool = mock(DaemonPool.class);

        when(pool.isStuck()).thenReturn(true);

        Supplier fn = new Supplier(pool);

        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));
        assertThat(executor.isStuck(), is(true));
        verify(pool).isStuck();
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldNotBeStuck()
        {
        DaemonPool pool = mock(DaemonPool.class);

        when(pool.isStuck()).thenReturn(false);

        Supplier fn = new Supplier(pool);

        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));
        assertThat(executor.isStuck(), is(false));
        verify(pool).isStuck();
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldSetClassLoader()
        {
        ClassLoader loader = mock(ClassLoader.class);
        DaemonPool  pool   = mock(DaemonPool.class);

        Supplier fn = new Supplier(pool);

        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));
        executor.setContextClassLoader(loader);
        verify(pool).setContextClassLoader(loader);
        verifyNoMoreInteractions(pool);
        }

    @Test
    public void shouldGetClassLoader()
        {
        ClassLoader loader = mock(ClassLoader.class);
        DaemonPool  pool   = mock(DaemonPool.class);

        when(pool.getContextClassLoader()).thenReturn(loader);

        Supplier fn = new Supplier(pool);

        Function<DaemonPoolDependencies, DaemonPool> supplier = spy(fn);

        DaemonPoolExecutor executor = DaemonPoolExecutor.builder()
                .supplier(supplier)
                .build();

        assertThat(executor, is(notNullValue()));
        assertThat(executor.getContextClassLoader(), is(sameInstance(loader)));
        verify(pool).getContextClassLoader();
        verifyNoMoreInteractions(pool);
        }

    // ----- inner class: Supplier ------------------------------------------

    public static class Supplier
            implements Function<DaemonPoolDependencies, DaemonPool>
        {

        private final DaemonPool pool;

        Supplier(DaemonPool pool)
            {
            this.pool = pool;
            }

        @Override
        public DaemonPool apply(DaemonPoolDependencies deps)
            {
            return pool;
            }
        }
    }
