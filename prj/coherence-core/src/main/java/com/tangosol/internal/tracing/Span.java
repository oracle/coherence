/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing;

import java.io.Serializable;

import java.util.Map;

/**
 * This interface is based off the {@code Span} interface provided by {@code OpenTelemetry} and exposes
 * only the interface methods Coherence currently uses.
 * <p>
 * Some language and terms are attributed to {@code OpenTelemetry}.
 *
 * @author rl 11.5.2019
 * @since 14.1.1.0
 */
public interface Span
        extends NoopAware, Wrapper
    {
    /**
     * Sets metadata to the {@code Span}. If the {@code Span} previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     * <p>
     * This call is equivalent to {@code io.opentracing.Span#setTag(String, String)}.
     *
     * @param sKey    the key for this attribute
     * @param sValue  the value for this attribute
     *
     * @return this {@code Span}
     */
    public Span setMetadata(String sKey, String sValue);

    /**
     * Sets metadata to the {@code Span}. If the {@code Span} previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     * <p>
     * This call is equivalent to {@code io.opentracing.Span#setTag(String, Number)}.
     *
     * @param sKey    the key for this attribute
     * @param lValue  the value for this attribute
     *
     * @return this {@code Span}
     */
    public Span setMetadata(String sKey, long lValue);

    /**
     * Sets metadata to the {@code Span}. If the {@code Span} previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     * <p>
     * This call is equivalent to {@code io.opentracing.Span#setTag(String, Number)}.
     *
     * @param sKey    the key for this attribute
     * @param dValue  the value for this attribute
     *
     * @return this {@code Span}
     */
    public Span setMetadata(String sKey, double dValue);

    /**
     * Sets metadata to the {@code Span}. If the {@code Span} previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     * <p>
     * This call is equivalent to {@code io.opentracing.Span#setTag(String, boolean)}.
     *
     * @param sKey    the key for this attribute
     * @param fValue  the value for this attribute
     *
     * @return this {@code Span}
     */
    public Span setMetadata(String sKey, boolean fValue);

    /**
     * Logs the specified event and timestamp with the {@code Span}.
     *
     * @param sEvent  the event value; often a stable identifier for a moment in the Span lifecycle
     *
     * @return this {@code Span}
     */
    public Span log(String sEvent);

    /**
     * Log the supplied key/value and timestamps with the {@code Span}.
     *
     * @param mapFields  the key/value pairs to log with the {@code Span}.
     *
     * @return this {@code Span}
     */
    public Span log(Map<String, ? super Serializable> mapFields);

    /**
     * Updates the {@code Span} name.
     * <p>
     * If used, this will override the name provided via {@code Span.Builder}.
     * <p>
     * Upon this update, any sampling behavior based on {@code Span} name will depend on the
     * implementation.
     * <p>
     * This call is equivalent to {@code io.opentracing.Span#setOperationName(String)}.
     *
     * @param sName  the {@code Span} name
     *
     * @return this {@code Span}
     */
    public Span updateName(String sName);

    /**
     * Marks the end of {@code Span} execution.
     * <p>
     * Only the timing of the first end call for a given {@code Span} will be recorded, and
     * implementations are free to ignore all further calls.
     * <p>
     * This call is equivalent to {@code io.opentracing.Span#finish()}.
     */
    public void end();

    /**
     * Returns the {@code SpanContext} associated with this {@code Span}.
     * <p>
     * This call is equivalent to {@code io.opentracing.Span#context()}.
     *
     * @return the {@code SpanContext} associated with this {@code Span}
     */
    public SpanContext getContext();

    // ----- inner interface: Builder -----------------------------------------

    /**
     * {@link Builder} is used to construct {@link Span} instances which define arbitrary scopes of
     * code that are sampled for distributed tracing as a single atomic unit.
     * <p>
     * This is a simple example where all the work is being done within a single scope and a single
     * thread and the Context is automatically propagated:
     *
     * <pre>{@code
     * class MyClass {
     *   private static final Tracer tracer = TracingHelper.getTracer();
     *   void doWork {
     *     // Create a Span as a child of the current Span.
     *     Span span = tracer.spanBuilder("MyChildSpan").startSpan();
     *     try (Scope ss = tracer.withSpan(span)) {
     *       tracer.getCurrentSpan().addMetadata("event", "my event");
     *       doSomeWork();  // Here the new span is in the current Context, so it can be used
     *                      // implicitly anywhere down the stack.
     *     } finally {
     *       span.end();
     *     }
     *   }
     * }
     * }</pre>
     * <p>
     * This is a simple example where all the work is being done within a single scope and the
     * Context is manually propagated:
     *
     * <pre>{@code
     * class MyClass {
     *   private static final Tracer tracer = TracingHelper.getTracer();
     *   void DoWork(Span parent) {
     *     Span childSpan = tracer.spanBuilder("MyChildSpan")
     *         .setParent(parent).startSpan();
     *     childSpan.addMetadata("event", "my event");
     *     try {
     *       doSomeWork(childSpan); // Manually propagate the new span down the stack.
     *     } finally {
     *       // To make sure we end the span even in case of an exception.
     *       childSpan.end();  // Manually end the span.
     *     }
     *   }
     * }
     * }</pre>
     */
    public interface Builder
        {
        /**
         * Sets the parent {@code Span} to use. If not set, the value of {@code Tracer.getCurrentSpan()}
         * at {@link #startSpan()} time will be used as parent.
         * <p>
         * This <b>must</b> be used to create a {@code Span} when manual context propagation is used
         * OR when creating a root {@code Span} with a parent with an invalid {@link SpanContext}.
         *<p>
         * Observe this is the preferred method when the parent is a {@code Span} created within the
         * process. Using its {@code SpanContext} as parent remains as a valid, albeit inefficient,
         * operation.
         * <p>
         * If called multiple times, only the last specified value will be used. Observe that the
         * state defined by a previous call to {@link #setNoParent()} will be discarded.
         *
         * @param parent  the {@code Span} used as parent
         *
         * @return this
         *
         * @throws NullPointerException if {@code parent} is {@code null}
         *
         * @see #setNoParent()
         */
        public Builder setParent(Span parent);

        /**
         * Sets the parent {@link SpanContext} to use. If not set, the value of {@code
         * Tracer.getCurrentSpan()} at {@link #startSpan()} time will be used as parent.
         * <p>
         * Similar to {@link #setParent(Span parent)} but this <b>must</b> be used to create a {@code
         * Span} when the parent is in a different process. This is only intended for use by RPC systems
         * or similar.
         * <p>
         * If no {@link SpanContext} is available, users must call {@link #setNoParent()} in order to
         * create a root {@code Span} for a new trace.
         * <p>
         * If called multiple times, only the last specified value will be used. Observe that the
         * state defined by a previous call to {@link #setNoParent()} will be discarded.
         *
         * @param remoteParent  the {@link SpanContext} used as parent
         *
         * @return this
         *
         * @throws NullPointerException if {@code remoteParent} is {@code null}
         *
         * @see #setParent(Span parent)
         * @see #setNoParent()
         */
        public Builder setParent(SpanContext remoteParent);

        /**
         * Sets the option to become a root {@code Span} for a new trace. If not set, the value of
         * {@link Tracer#getCurrentSpan()} at {@link #startSpan()} time will be used as parent.
         * <p>
         * Observe that any previously set parent will be discarded.
         *
         * @return this
         */
        public Builder setNoParent();

        /**
         * Sets metadata to the {@link Span}. If the {@link Span} previously contained a mapping for
         * the key, the old value is replaced by the specified value.
         *
         * @param sKey    the key for this attribute
         * @param sValue  the value for this attribute
         *
         * @return this
         */
        public Builder withMetadata(String sKey, String sValue);

        /**
         * Sets metadata to the {@link Span}. If the {@link Span} previously contained a mapping for
         * the key, the old value is replaced by the specified value.
         *
         * @param sKey    the key for this attribute
         * @param fValue  the value for this attribute
         *
         * @return this
         */
        public Builder withMetadata(String sKey, boolean fValue);

        /**
         * Sets metadata to the {@link Span}. If the {@link Span} previously contained a mapping for
         * the key, the old value is replaced by the specified value.
         *
         * @param sKey    the key for this attribute
         * @param lValue  the value for this attribute
         *
         * @return this
         */
        Builder withMetadata(String sKey, long lValue);

        /**
         * Sets metadata to the {@link Span}. If the {@link Span} previously contained a mapping for
         * the key, the old value is replaced by the specified value.
         *
         * @param sKey    the key for this attribute
         * @param dValue  the value for this attribute
         *
         * @return this
         */
        public Builder withMetadata(String sKey, double dValue);

        /**
         * Adds an association between this {@code Span} and the {@link SpanContext context} of another {@code Span}.
         *
         * @param sLabel             the label for this association
         * @param associatedContext  the {@link SpanContext} to create an association with
         *
         * @return this
         */
        public Builder withAssociation(String sLabel, SpanContext associatedContext);

        /**
         * Sets an explicit start timestamp for the newly created {@link Span}.
         * <p>
         * Use this method to specify an explicit start timestamp. If not called, the implementation
         * will use the timestamp value at {@link #startSpan()} time, which should be the default case.
         * <p>
         * Important this is NOT equivalent with System.nanoTime().
         *
         * @param ldtStartTime   the explicit start timestamp of the newly created {@link Span} in nanoseconds
         *                       since epoch.
         *
         * @return this
         */
        public Builder setStartTimestamp(long ldtStartTime);

        /**
         * Starts a new {@link Span}.
         * <p>
         * Users <b>must</b> manually call {@link Span#end()} to end this {@link Span}.
         * <p>
         * Does not install the newly created {@link Span} to the current context.
         * <p>
         * Example of usage:
         *
         * <pre>{@code
         * class MyClass {
         *   private static final Tracer tracer = TracingHelper.getTracer();
         *   void DoWork(Span parent) {
         *     Span childSpan = tracer.spanBuilder("MyChildSpan")
         *          .setParent(parent)
         *          .startSpan();
         *     try {
         *       doSomeWork(childSpan); // Manually propagate the new span down the stack.
         *     } finally {
         *       // To make sure we end the span even in case of an exception.
         *       childSpan.end();  // Manually end the span.
         *     }
         *   }
         * }
         * }</pre>
         *
         * @return the newly created {@code Span}
         */
        public Span startSpan();

        // ----- inner class: Noop ------------------------------------------

        /**
         * A no-op implementation of {@link Builder}.
         */
        final class Noop
                implements Builder
            {
            // ----- constructors -------------------------------------------

            /**
             * @see #INSTANCE
             */
            private Noop()
                {
                }

            // ----- Builder interface --------------------------------------

            /**
             * This is a no-op.
             *
             * @param parent  {@inheritDoc}
             *
             * @return {@inheritDoc}
             */
            @Override
            public Builder setParent(Span parent)
                {
                return this;
                }

            /**
             * This is a no-op.
             *
             * @param remoteParent  {@inheritDoc}
             *
             * @return {@inheritDoc}
             */
            @Override
            public Builder setParent(SpanContext remoteParent)
                {
                return this;
                }

            /**
             * This is a no-op.
             *
             * @return {@inheritDoc}
             */
            @Override
            public Builder setNoParent()
                {
                return this;
                }

            /**
             * This is a no-op.
             *
             * @param sKey    {@inheritDoc}
             * @param sValue  {@inheritDoc}
             *
             * @return {@inheritDoc}
             */
            @Override
            public Builder withMetadata(String sKey, String sValue)
                {
                return this;
                }

            /**
             * This is a no-op.
             *
             * @param sKey    {@inheritDoc}
             * @param fValue  {@inheritDoc}
             *
             * @return {@inheritDoc}
             */
            @Override
            public Builder withMetadata(String sKey, boolean fValue)
                {
                return this;
                }

            /**
             * This is a no-op.
             *
             * @param sKey    {@inheritDoc}
             * @param lValue  {@inheritDoc}
             *
             * @return {@inheritDoc}
             */
            @Override
            public Builder withMetadata(String sKey, long lValue)
                {
                return this;
                }

            /**
             * This is a no-op.
             *
             * @param sKey    {@inheritDoc}
             * @param dValue  {@inheritDoc}
             *
             * @return {@inheritDoc}
             */
            @Override
            public Builder withMetadata(String sKey, double dValue)
                {
                return this;
                }

            /**
             * This is a no-op.
             *
             * @param sLabel              {@inheritDoc}
             * @param associatedContext  {@inheritDoc}
             *
             * @return {@inheritDoc}
             */
            public Builder withAssociation(String sLabel, SpanContext associatedContext)
                {
                return this;
                }

            /**
             * This is a no-op.
             *
             * @param ldtStartTime  {@inheritDoc}
             *
             * @return {@inheritDoc}
             */
            @Override
            public Builder setStartTimestamp(long ldtStartTime)
                {
                return this;
                }

            /**
             * Always returns {@link Span.Noop#INSTANCE}.
             *
             * @return {@link Span.Noop#INSTANCE}
             */
            @Override
            public Span startSpan()
                {
                return Span.Noop.INSTANCE;
                }

            // ----- constants ----------------------------------------------

            /**
             * Singleton no-op {@link Builder} instance.
             */
            public static final Builder INSTANCE = new Noop();
            }
        }

    // ----- inner class: Noop ----------------------------------------------

    /**
     * A no-op implementation of {@link Span}.
     */
    final class Noop
            implements Span
        {
        // ----- constructors -----------------------------------------------

        /**
         * @see #INSTANCE
         */
        private Noop()
            {
            }

        // ----- Span interface ---------------------------------------------

        /**
         * This is a no-op.
         *
         * @param sKey    {@inheritDoc}
         * @param sValue  {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public Span setMetadata(String sKey, String sValue)
            {
            return this;
            }

        /**
         * This is a no-op.
         *
         * @param sKey    {@inheritDoc}
         * @param lValue  {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public Span setMetadata(String sKey, long lValue)
            {
            return this;
            }

        /**
         * This is a no-op.
         *
         * @param sKey    {@inheritDoc}
         * @param dValue  {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public Span setMetadata(String sKey, double dValue)
            {
            return this;
            }

        /**
         * This is a no-op.
         *
         * @param sKey    {@inheritDoc}
         * @param fValue  {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public Span setMetadata(String sKey, boolean fValue)
            {
            return this;
            }

        /**
         * This is a no-op.
         *
         * @param sEvent  {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public Span log(String sEvent)
            {
            return this;
            }

        /**
         * This is a no-op.
         *
         * @param mapFields  {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public Span log(Map<String, ? super Serializable> mapFields)
            {
            return null;
            }

        /**
         * This is a no-op.
         *
         * @param sName  the {@code Span} name
         *
         * @return {@inheritDoc}
         */
        @Override
        public Span updateName(String sName)
            {
            return this;
            }

        /**
         * This is a no-op.
         */
        @Override
        public void end()
            {
            }

        /**
         * Always returns {@link SpanContext.Noop#INSTANCE}.
         *
         * @return {@link Tracer.Noop#INSTANCE}
         */
        @Override
        public SpanContext getContext()
            {
            return SpanContext.Noop.INSTANCE;
            }

        // ----- NoopAware interface ----------------------------------------

        /**
         * Always returns {@code true}.
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
            return "Span.Noop";
            }

        // ---- constants ---------------------------------------------------

        /**
         * Singleton no-op {@link Span} instance.
         */
        public static final Span INSTANCE = new Noop();
        }

    // ----- inner class: SpanType ------------------------------------------

    /**
     * The span types used internally by {@code Coherence}.
     */
    public enum Type
        {
        /**
         * Indicates that the span is associated with a particular {@code Coherence} component.
        */
        COMPONENT;

        // ----- constructors -----------------------------------------------

        /**
         * Creates a lower-case version of the enumerate that can be obtained by calling
         * {@link #key}.
         */
        Type()
            {
            f_sKey = super.name().toLowerCase();
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return the {@link String} key for use with tracing metadata.
         *
         * @return the {@link String} key for use with tracing metadata
         */
        public String key()
            {
            return f_sKey;
            }

        // ----- data members -----------------------------------------------

        /**
         * The value returned by {@link #key}.
         */
        private final String f_sKey;
        }

    /**
     * Common keys for {@code Coherence} {@link Span} metadata.
     */
    public enum Metadata
        {
        /**
         * Key {@code listener.classes}.
         */
        LISTENER_CLASSES;

        // ----- constructors -----------------------------------------------

        /**
         * Creates a lower-case version of the enumerate that can be obtained by calling
         * {@link #key}.
         */
        Metadata()
            {
            f_sKey = super.name().toLowerCase().replace('_', '.');
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return the {@link String} key for use with tracing metadata.
         *
         * @return the {@link String} key for use with tracing metadata
         */
        public String key()
            {
            return f_sKey;
            }

        // ----- data members -----------------------------------------------

        /**
         * The value returned by {@link #key}.
         */
        private final String f_sKey;
        }

    /**
     * Common keys for {@link Span} associations.
     */
    public enum Association
        {
        /**
         * Some parent Spans do not depend in any way on the result of their
         * child Spans. In these cases, we say merely that the child Span
         * FollowsFrom the parent Span in a causal sense.
         */
        FOLLOWS_FROM,

        /**
         * A Span may be the ChildOf a parent Span. In a ChildOf reference,
         * the parent Span depends on the child Span in some capacity.
         */
        CHILD_OF;

        // ----- constructors -----------------------------------------------

        /**
         * Creates a lower-case version of the enumerate that can be obtained by calling
         * {@link #key}.
         */
        Association()
            {
            f_sKey = super.name().toLowerCase();
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return the {@link String} key for use with span associations.
         *
         * @return the {@link String} key for use with span associations
         */
        public String key()
            {
            return f_sKey;
            }

        // ----- data members -----------------------------------------------

        /**
         * The value returned by {@link #key}.
         */
        private final String f_sKey;
        }
    }
