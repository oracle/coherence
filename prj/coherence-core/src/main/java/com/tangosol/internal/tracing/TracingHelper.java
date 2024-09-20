/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;

import com.tangosol.util.Base;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Objects;

import java.util.ServiceLoader;

/**
 * Set of utility methods to work with the Coherence tracing abstraction.
 * <p>
 * When this class is loaded, it will attempt to load the first {@link TracingShim} available on the classpath
 * and delegate the API calls exposed here to the loaded {@link TracingShim shim}.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
public final class TracingHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
     * No instance necessary.
     */
    private TracingHelper()
        {
        }

    // ----- public methods -------------------------------------------------

    /**
     * Construct a DefaultDependencies instance.
     *
     * @return default dependencies
     */
    public static TracingShim.DefaultDependencies defaultDependencies()
        {
        return new TracingShim.DefaultDependencies();
        }

    /**
     * Construct a DefaultDependencies instance, copying values from the supplied instance.
     *
     * @param deps  the {@link TracingShim.Dependencies} to copy
     *
     * @return default dependencies
     */
    public static TracingShim.DefaultDependencies defaultDependencies(TracingShim.Dependencies deps)
        {
        return new TracingShim.DefaultDependencies(deps);
        }

    /**
     * Initialize the {@link Tracer}, if not already initialized.
     *
     * @param dependencies  the tracing {@link TracingShim.Dependencies dependencies}
     *
     * @return a resource which can be used to terminate tracing, or {@code null} if it was already enabled
     */
    public static TracingShim.Control initialize(TracingShim.Dependencies dependencies)
        {
        TracingShim   tracingShim  = m_tracingShim;
        ClassLoader[] classLoaders = new ClassLoader[]
            {
            Base.getContextClassLoader(),
            TracingShimLoader.class.getClassLoader()
            };

        loader:
        for (int i = 0, len = classLoaders.length; i < len; i++)
            {
            ClassLoader loader = classLoaders[i];
            try
                {
                ServiceLoader<TracingShimLoader> tracingShimLoaderService =
                        ServiceLoader.load(TracingShimLoader.class, loader);

                for (TracingShimLoader tracingShimLoader : tracingShimLoaderService)
                    {
                    try
                        {
                        tracingShim = tracingShimLoader.loadTracingShim();
                        }
                    catch (Throwable t) // most likely a configuration issue with the tracing provider
                        {
                        Logger.err("Error invoking TracingShimLoader.loadTracingShim().  "
                                       + "Continuing to search for a valid tracing shim: ", t);
                        continue;
                        }

                    if (isNoop(tracingShim))
                        {
                        continue;
                        }
                    m_tracingShim = tracingShim;
                    break loader; // first non-noop implementation wins
                    }
                break; // break loader loop if no exception was thrown using the loop's classloader
                }
            catch (Throwable t) // exceptions here are from the ServiceLoader itself
                {
                if (Logger.isEnabled(Logger.WARNING))
                    {
                    String msg = "Error loading tracing shim using the %s classloader:";
                    if (i == 0)
                        {
                        Logger.warn(String.format(msg, "context"), t);
                        Logger.warn("Attempting to load tracing shim using the fallback classloader.");
                        }
                    else
                        {
                        Logger.warn(String.format(msg, "fallback"), t);
                        Logger.warn("Tracing failed to initialize.");
                        }
                    }
                }
            }

        return tracingShim.initialize(dependencies);
        }

    /**
     * Return {@code true} if the registered {@link Tracer} is effectively a no-op.
     *
     * @return {@code true} if the registered {@link Tracer} is effectively a no-op
     */
    public static boolean isEnabled()
        {
        return m_tracingShim.isEnabled();
        }

    /**
     * Return the {@link Tracer}.
     *
     * @return the current {@link Tracer}
     */
    public static Tracer getTracer()
        {
        return m_tracingShim.getTracer();
        }

    /**
     * Return the active {@link Span} if tracing is enabled and there is one active.
     *
     * @return the active {@link Span} or {@code null}
     */
    public static Span getActiveSpan()
        {
        return isEnabled() ? getTracer().getCurrentSpan() : null;
        }

    /**
     * Return the active {@link Span} if any, otherwise return a {@link Span} that is effectively a no-op.
     *
     * @return the active {@link Span} if any, otherwise return a {@link Span} that is effectively a no-op
     */
    @SuppressWarnings("unused")
    public static Span augmentSpan()
        {
        Span span = getActiveSpan();

        return span == null ? m_tracingShim.getNoopSpan() : span;
        }

    /**
     * Activate the specified {@link Span}.
     *
     * @param span  the {@link Span} to activate.
     *
     * @return the activated {@link Span}
     *
     * @since 24.03
     */
    public static Span activateSpan(Span span)
        {
        if (span != null)
            {
            return isEnabled() ? m_tracingShim.activateSpan(span) : span;
            }

        return null;
        }

    /**
     * Augments the specified {@link Span} with details related to the provided {@link Throwable}.
     *
     * @param span      the {@link Span} to augment
     * @param fIsError  {@code true} if this span should be marked as erroneous
     * @param t         the {@link Throwable} to log
     *
     * @return the specified {@link Span}
     */
    public static Span augmentSpanWithErrorDetails(Span span, boolean fIsError, Throwable t)
        {
        if (!isNoop(span))
            {
            if (fIsError)
                {
                span.setMetadata("error", true);
                }
            span.log(new HashMap<String, Serializable>(5)
                {{
                put("event",        "error");
                put("error.object", t);
                }});
            }
        return span;
        }

    /**
     * Return a {@link Span.Builder} for the specified operation.
     *
     * @param sOpName  the operation name
     *
     * @return the {@link Span.Builder}
     */
    public static Span.Builder newSpan(String sOpName)
        {
        if (isEnabled())
            {
            Span.Builder builder = getTracer().spanBuilder(sOpName)
                    .withMetadata("thread", Thread.currentThread().getName());

            try
                {
                Cluster cluster = CacheFactory.getCluster();
                Member  member  = cluster.isRunning() ? cluster.getLocalMember() : null;
                if (member != null)
                    {
                    builder = builder.withMetadata("member", member.getId());
                    }
                }
            catch (Exception ignored)
                {
                } // getLocalMember can throw IllegalStateException

            return Float.compare(0.0f, m_tracingShim.getDependencies().getSamplingRatio()) == 0
                   ? new ConditionalSpanBuilder(builder)
                   : builder;
            }
        else
            {
            return m_tracingShim.getNoopSpanBuilder();
            }
        }

    /**
     * Return a {@link Span.Builder} for the specified stage and operation.
     *
     * @param sStage  the stage of the operation
     * @param op      the operation
     *
     * @return the {@link Span.Builder builder}
     */
    @SuppressWarnings("unused")
    public static Span.Builder newSpan(String sStage, Object op)
        {
        if (isEnabled())
            {
            String sOpClass = null;
            String sOpName;

            if (op == null)
                {
                sOpName = sStage;
                }
            else
                {
                Class<?> clz = op.getClass();

                sOpClass = clz.getName();

                // strip off package names, and $Poll suffix
                sOpName = sOpClass;

                int ofDot = sOpName.lastIndexOf('.') + 1;

                sOpName = sOpName.endsWith("$Poll")
                          ? sOpName.substring(ofDot, sOpName.length() - 5)
                          : sOpName.substring(ofDot);

                // strip off any parent class, and any Request suffix
                int ofDol = sOpName.lastIndexOf('$') + 1;

                sOpName = sOpName.endsWith("Request")
                          ? sOpName.substring(ofDol, sOpName.length() - 7)
                          : sOpName.substring(ofDol);

                if (sStage != null)
                    {
                    sOpName += ('.' + sStage);
                    }
                }

            Span.Builder builder = newSpan(sOpName);

            if (sOpClass != null)
                {
                builder = builder.withMetadata("operation.class", sOpClass);
                }

            builder = builder.withMetadata("span.kind",
                                           sStage == null || !sStage.equals("request")
                                               ? "server"
                                               : "client");

            return builder;
            }
        else
            {
            return newSpan("no-op");
            }
        }

    /**
     * Returns {@code true} if the {@link NoopAware target} is {@code null} otherwise the result of calling
     * {@link NoopAware#isNoop()}.
     *
     * @param target the {@link NoopAware} to query
     *
     * @return {@code true} if the {@link NoopAware target} is {@code null} otherwise the result of calling
     * {@link NoopAware#isNoop()}
     */
    public static boolean isNoop(NoopAware target)
        {
        return target == null || target.isNoop();
        }

    // ---- inner class: ConditionalSpanBuilder -------------------------------

    /**
     * A {@link Span.Builder} which conditionally builds a real {@link Span}.
     */
    protected static class ConditionalSpanBuilder
            implements Span.Builder
        {
        // ----- constructors -----------------------------------------------

        /**
         * Wraps an existing {@link Span.Builder} to enable conditional creation of Spans.
         *
         * @param delegate  the {@link Span.Builder} to delegate calls to as appropriate
         *
         * @throws NullPointerException if {@code delegate} is {@code null}
         */
        protected ConditionalSpanBuilder(Span.Builder delegate)
            {
            Objects.requireNonNull(delegate, "Parameter delegate cannot be null");

            m_delegate = delegate;
            }

        // ----- Span.Builder interface -------------------------------------

        @Override
        public Span.Builder setParent(Span parent)
            {
            if (!TracingHelper.isNoop(parent))
                {
                m_delegate = m_delegate.setParent(parent);
                }
            return this;
            }

        @Override
        public Span.Builder setParent(SpanContext remoteParent)
            {
            if (!TracingHelper.isNoop(remoteParent))
                {
                m_delegate = m_delegate.setParent(remoteParent);
                }
            return this;
            }

        @Override
        public Span.Builder setNoParent()
            {
            m_fIgnoreActiveSpan = true;
            m_delegate          = m_delegate.setNoParent();
            return this;
            }

        @Override
        public Span.Builder withMetadata(String sKey, String sValue)
            {
            m_delegate = m_delegate.withMetadata(sKey, sValue);
            return this;
            }

        @Override
        public Span.Builder withMetadata(String sKey, boolean fValue)
            {
            m_delegate = m_delegate.withMetadata(sKey, fValue);
            return this;
            }

        @Override
        public Span.Builder withMetadata(String sKey, long lValue)
            {
            m_delegate = m_delegate.withMetadata(sKey, lValue);
            return this;
            }

        @Override
        public Span.Builder withMetadata(String sKey, double dValue)
            {
            m_delegate = m_delegate.withMetadata(sKey, dValue);
            return this;
            }

        @Override
        public Span.Builder withAssociation(String sLabel, SpanContext associatedContext)
            {
            m_delegate = m_delegate.withAssociation(sLabel, associatedContext);
            return this;
            }

        @Override
        public Span.Builder setStartTimestamp(long ldtStartTime)
            {
            m_delegate = m_delegate.setStartTimestamp(ldtStartTime);
            return this;
            }

        @Override
        public Span startSpan()
            {
            return dropSpan()
                       ? TracingHelper.m_tracingShim.getNoopSpan()
                       : m_delegate.startSpan();
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return {@code true} if the {@link Span span} should not be created.
         *
         * @return {@code true} if the {@link Span span} should not be created.
         */
        protected boolean dropSpan()
            {
            return m_fIgnoreActiveSpan || isNoop(getActiveSpan());
            }

        // ----- object methods ---------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (!(o instanceof ConditionalSpanBuilder))
                {
                return false;
                }
            ConditionalSpanBuilder that = (ConditionalSpanBuilder) o;
            return m_fIgnoreActiveSpan == that.m_fIgnoreActiveSpan
                   && Base.equals(m_delegate, that.m_delegate);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_delegate, m_fIgnoreActiveSpan);
            }

        @Override
        public String toString()
            {
            return "ConditionalSpanBuilder("
                   + "DelegateBuilder=" + m_delegate
                   + ", IgnoreActiveSpan=" + m_fIgnoreActiveSpan
                   + ')';
            }

        // ----- data members -----------------------------------------------

        /**
         * The delegate {@link Span.Builder}.
         */
        protected Span.Builder m_delegate;

        /**
         * Flag indicating if {@link #setNoParent()} ()} has been called.
         */
        protected boolean m_fIgnoreActiveSpan;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Singleton no-op {@link TracingShim} instance.
     */
    protected static TracingShim m_tracingShim = TracingShim.Noop.INSTANCE;
    }
