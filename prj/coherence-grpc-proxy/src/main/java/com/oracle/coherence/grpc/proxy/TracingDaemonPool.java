/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.DaemonPoolDependencies;

import com.tangosol.run.xml.XmlElement;

import io.grpc.Context;

import io.opentracing.Span;

import io.opentracing.contrib.grpc.OpenTracingContextKey;

import io.opentracing.util.GlobalTracer;

import java.util.function.Supplier;

/**
 * A {@link DaemonPool} implementation that wraps another
 * {@link DaemonPool} and adds activation of tracing spans
 * for the {@link Runnable}s executed by this pool.
 *
 * @author Jonathan Knight  2020.01.10
 * @since 20.06
 */
class TracingDaemonPool
        implements DaemonPool
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link TracingDaemonPool}.
     *
     * @param delegate the {@link DaemonPool} to delegate to
     */
    TracingDaemonPool(DaemonPool delegate)
        {
        this(delegate, null);
        }

    /**
     * Create a {@link TracingDaemonPool}.
     *
     * @param delegate the {@link DaemonPool} to delegate to
     * @param supplier the {@link Supplier} that will provide active spans
     */
    TracingDaemonPool(DaemonPool delegate, Supplier<Span> supplier)
        {
        this.f_delegate   = delegate;
        this.f_activeSpan = supplier == null ? () -> GlobalTracer.get().activeSpan() : supplier;
        }

    // ----- DaemonPool interface -------------------------------------------

    @Override
    public void add(Runnable task)
        {
        Span span = findSpan();
        long hash = task.hashCode();
        if (span != null)
            {
            span.log("adding to daemon pool task=" + hash);
            f_delegate.add(new TracingRunnable(span, task));
            }
        else
            {
            // no span so add the plain task
            f_delegate.add(task);
            }
        }

    @Override
    public DaemonPoolDependencies getDependencies()
        {
        return f_delegate.getDependencies();
        }

    @Override
    public void setDependencies(DaemonPoolDependencies deps)
        {
        f_delegate.setDependencies(deps);
        }

    @Override
    public boolean isRunning()
        {
        return f_delegate.isRunning();
        }

    @Override
    public boolean isStuck()
        {
        return f_delegate.isStuck();
        }

    @Override
    public void schedule(Runnable task, long cMillis)
        {
        Span span = findSpan();
        long hash = task.hashCode();
        if (span != null)
            {
            span.log("scheduling in daemon pool in " + cMillis + " task=" + hash);
            f_delegate.schedule(new TracingRunnable(span, task), cMillis);
            }
        else
            {
            // no span so schedule the plain runnable
            f_delegate.schedule(task, cMillis);
            }
        }

    @Override
    public void shutdown()
        {
        f_delegate.shutdown();
        }

    @Override
    public void start()
        {
        f_delegate.start();
        }

    @Override
    public void stop()
        {
        f_delegate.stop();
        }

    // ----- Controllable interface -----------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public void configure(XmlElement xml)
        {
        f_delegate.configure(xml);
        }

    // ----- ClassloaderAware interface -------------------------------------

    @Override
    public ClassLoader getContextClassLoader()
        {
        return f_delegate.getContextClassLoader();
        }

    @Override
    public void setContextClassLoader(ClassLoader loader)
        {
        f_delegate.setContextClassLoader(loader);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the current active span, or try to find the span in the current {@link Context}.
     *
     * @return the current active span, or try to find the span in the current {@link Context}
     */
    protected Span findSpan()
        {
        Span span = f_activeSpan.get();
        if (span == null)
            {
            // try the gRPC context
            Context context = Context.current();
            if (context != null)
                {
                span = OpenTracingContextKey.getKey().get(context);
                }
            }
        return span;
        }

    /**
     * Returns the {@link DaemonPool} to delegate to.
     *
     * @return the {@link DaemonPool} to delegate to
     */
    protected DaemonPool getDelegate()
        {
        return f_delegate;
        }

    // ----- inner class: TracingRunnable -----------------------------------

    /**
     * A {@link Runnable} that executes another runnable
     * after activating a tracing span.
     */
    protected static class TracingRunnable
            implements Runnable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code TracingRunnable} for the provided span and
         * {@link Runnable} delegate.
         *
         * @param span       the {@link Span} to use
         * @param delegate   the task to trace
         */
        protected TracingRunnable(Span span, Runnable delegate)
            {
            this.f_span     = span;
            this.f_delegate = delegate;
            f_lHash         = delegate.hashCode();
            }

        // ----- Runnable interface -----------------------------------------

        @Override
        public void run()
            {
            GlobalTracer.get().scopeManager().activate(f_span);
            f_span.log("starting execution in daemon pool task=" + f_lHash);
            f_delegate.run();
            f_span.log("finished execution in daemon pool task=" + f_lHash);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Returns the {@link Runnable} delegate.
         *
         * @return the {@link Runnable} delegate
         */
        protected Runnable getDelegate()
            {
            return f_delegate;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Span}.
         */
        protected final Span f_span;

        /**
         * The {@link Runnable}.
         */
        protected final Runnable f_delegate;

        /**
         * The {@link Runnable}'s hash.
         */
        protected final long f_lHash;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link DaemonPool} to delegate to.
     */
    protected final DaemonPool f_delegate;

    /**
     * The {@link Supplier} that will provide active spans.
     */
    protected final Supplier<Span> f_activeSpan;
    }
