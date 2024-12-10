/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing;

/**
 * This interface is based off the {@code SpanContext} interface provided by {@code OpenTelemetry} and exposes
 * only the interface methods Coherence currently uses.
 * <p>
 * Some language and terms are attributed to {@code OpenTelemetry}.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
public interface SpanContext
        extends NoopAware, Wrapper
    {
    /**
     * Return the trace identifier associated with this {@code SpanContext}.
     *
     * @return the trace identifier associated with this {@code SpanContext}
     */
    public String getTraceId();

    /**
     * Return the span identifier associated with this {@code SpanContext}.
     *
     * @return the span identifier associated with this {@code SpanContext}
     */
    public String getSpanId();

    // ----- inner class: Noop ----------------------------------------------

    /**
     * A no-op implementation of {@link SpanContext}.
     */
    final class Noop
            implements SpanContext
        {
        // ----- constructors -----------------------------------------------

        /**
         * @see #INSTANCE
         */
        private Noop()
            {
            }

        // ----- SpanContext inerface ---------------------------------------

        @Override
        public String getTraceId()
            {
            return "";
            }

        @Override
        public String getSpanId()
            {
            return "";
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
            return "SpanContext.Noop";
            }

        // ----- constants --------------------------------------------------

        /**
         * Singleton no-op {@link SpanContext} instance.
         */
        public static final SpanContext INSTANCE = new Noop();
        }
    }
