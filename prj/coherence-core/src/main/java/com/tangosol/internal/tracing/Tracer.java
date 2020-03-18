/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing;

import java.util.Map;

/**
 * {@code Tracer} is a simple, interface for {@link Span} creation and in-process context interaction.
 * <p>
 * Some language and terms are attributed to {@code OpenTelemetry}.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
public interface Tracer
        extends NoopAware, Wrapper
    {
    /**
     * Gets the current Span from the current context.
     * <p>
     * To install a {@link Span} to the current context use {@link #withSpan(Span)}.
     *
     * @return a default {@link Span} that does nothing and has an invalid {@link SpanContext} if no
     *         {@link Span} is associated with the current context, otherwise the current {@link Span}
     *         from the context.
     */
    public Span getCurrentSpan();

    /**
     * Enters the scope of code where the given {@link Span} is in the current context, and returns an
     * object that represents that {@link Scope scope}. The {@link Scope scope} is exited when the returned object
     * is closed.
     * <p>
     * Supports try-with-resource idiom.
     *
     * @param span  the {@link Span} to be set to the current context
     *
     * @return an object that defines a {@link Scope scope} where the given {@link Span} will be set to the current
     *         context
     *
     * @throws NullPointerException if {@link Span span} is {@code null}.
     */
    public Scope withSpan(Span span);

    /**
     * Returns a {@link Span.Builder} to create and start a new {@link Span}.
     *
     * @param spanName  the name of the returned {@link Span}
     *
     * @return a {@link Span.Builder} to create and start a new {@link Span}
     *
     * @throws NullPointerException if {@code spanName} is {@code null}
     *
     * @see Span.Builder
     */
    public Span.Builder spanBuilder(String spanName);

    /**
     * Inject a {@link SpanContext} for propagation across process boundaries.
     *
     * @param spanContext  the {@link SpanContext} instance to inject
     *
     * @return a {@link Map} holding the serialized {@link SpanContext} data
     */
    public Map<String, String> inject(SpanContext spanContext);

    /**
     * Extract a {@link SpanContext} after propagation across a process boundary.
     *
     * @param carrier  the carrier of serialized {@link SpanContext} state
     *
     * @return the {@link SpanContext} instance holding context to create a {@link Span}, otherwise returns
     *         {@code null}
     *
     * @throws IllegalArgumentException if serialization state is invalid (corrupt, wrong version, etc.)
     */
    public SpanContext extract(Map<String, String> carrier);

    // ----- inner class: Noop ----------------------------------------------

    /**
     * A no-op implementation of {@link Tracer}.
     */
    final class Noop
            implements Tracer
        {
        // ----- constructors -----------------------------------------------

        /**
         * @see #INSTANCE
         */
        public Noop()
            {
            }

        // ----- Tracer interface -------------------------------------------

        /**
         * Always returns {@link Span.Noop#INSTANCE}.
         *
         * @return {@link Span.Noop#INSTANCE}
         */
        @Override
        public Span getCurrentSpan()
            {
            return Span.Noop.INSTANCE;
            }

        /**
         * Always returns {@link Scope.Noop#INSTANCE}.
         *
         * @return {@link Scope.Noop#INSTANCE}
         */
        @Override
        public Scope withSpan(Span span)
            {
            return Scope.Noop.INSTANCE;
            }

        /**
         * Always returns {@link Span.Builder.Noop#INSTANCE}.
         *
         * @return {@link Span.Builder.Noop#INSTANCE}
         */
        @Override
        public Span.Builder spanBuilder(String spanName)
            {
            return Span.Builder.Noop.INSTANCE;
            }

        /**
         * Always returns {@code null}.
         *
         * @return {@code null}
         */
        @Override
        public Map<String, String> inject(SpanContext spanContext)
            {
            return null;
            }

        /**
         * Always returns {@link SpanContext.Noop#INSTANCE}.
         *
         * @return {@link SpanContext.Noop#INSTANCE}
         */
        @Override
        public SpanContext extract(Map<String, String> carrier)
            {
            return SpanContext.Noop.INSTANCE;
            }

        // ----- NoopAware interface ----------------------------------------

        /**
         * Returns {@code true}.
         *
         * @return {@code true}
         */
        @Override
        public boolean isNoop()
            {
            return true;
            }

        // ----- Wrapper interface ------------------------------------------

        /**
         * Always returns {@code null}.
         *
         * @return {@code null}
         */
        @Override
        public <T> T underlying()
            {
            return null;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return "Tracer.Noop";
            }

        // ----- constants --------------------------------------------------

        /**
         * Singleton no-op {@link Tracer} instance.
         */
        public static final Tracer INSTANCE = new Noop();
        }
    }
