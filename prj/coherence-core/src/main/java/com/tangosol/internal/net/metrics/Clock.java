/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.metrics;

/*
 * This class is heavily inspired by:
 * Clock
 *
 * From Helidon v 2.0.2
 * Distributed under Apache License, Version 2.0
 */

/**
 * Clock interface to allow replacing system clock with
 * a custom one (e.g. for unit testing).
 */
interface Clock {
    /**
     * System clock. Please do not use directly, use {@link #system()}.
     * This is only visible as we cannot do private modifier in interfaces yet.
     */
    Clock SYSTEM = new Clock() {
        @Override
        public long nanoTick() {
            return System.nanoTime();
        }

        @Override
        public long milliTime() {
            return System.currentTimeMillis();
        }
    };

    /**
     * Create a clock based on system time.
     *
     * @return clock based on system time
     */
    static Clock system() {
        return SYSTEM;
    }

    /**
     * A nanosecond precision tick to use for time
     * measurements. The value is meaningless, it just must
     * increase correctly during a JVM runtime.
     *
     * @return nanosecond value
     */
    long nanoTick();

    /**
     * A millisecond precision current time, such as provided
     * by {@link System#currentTimeMillis()}.
     *
     * @return current time in milliseconds
     */
    long milliTime();
}
