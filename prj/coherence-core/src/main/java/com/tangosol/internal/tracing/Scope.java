/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing;

import java.io.Closeable;

/**
 * A {@link java.io.Closeable} that represents a change to the current context over a scope of code.
 * {@link Scope#close} cannot throw a checked exception.
 * <p>
 * Some language and terms are attributed to {@code OpenTelemetry}.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
public interface Scope
        extends Closeable, NoopAware, Wrapper
    {
    // ----- Closable interface ---------------------------------------------

    @Override
    public void close();

    // ----- inner class: Noop ----------------------------------------------

    /**
     * A no-op implementation of {@link Scope}.
     */
    final class Noop
            implements Scope
        {
        // ----- constructors -----------------------------------------------

        /**
         * @see #INSTANCE
         */
        private Noop()
            {
            }

        // ----- Closeable interface ----------------------------------------

        @Override
        public void close()
            {
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
            return "Scope.Noop";
            }

        // ----- constants --------------------------------------------------

        /**
         * Singleton no-op {@link Scope} instance.
         */
        public static final Scope INSTANCE = new Noop();
        }
    }
