/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus.util;

import com.oracle.coherence.common.base.Collector;
import com.oracle.coherence.common.net.exabus.Event;

import java.util.concurrent.TimeUnit;

/**
 * A PollingEventCollector is a collector for applications which wish to have a greater level of control
 * as to when and where events are generated.  Bus implementations may be optimized for for this type of
 * collector, and {@link #bind(PollingEventCollector.BusProcessor) bind}
 * to it.  In such a case it will be up to the collector to call {@link BusProcessor#poll} in order to
 * <tt>run</tt> the bus and ensure it can process work and emit events.  The collector should still be ready
 * to have events {@link Collector#add(Object) added} to it at any point, even if the bus has bound itself
 * to the collector.
 *
 * @author mf 02.18.2013
 */
public interface PollingEventCollector
    extends Collector<Event>
    {
    // ----- inner class: BusProcessor -------------------------------------

    /**
     * The BusProcessor can be provided to a bus in {@link #bind} to allow
     * the bus to be more tightly coupled to the application thread.
     */
    public static interface BusProcessor
        {
        /**
         * Poll for events.
         * <p>
         * The method may execute for up to the specified timeout and may emit
         * multiple events within that time.  To terminate eagerly return from
         * poll the collector may invoke {@link #cancel}.
         * </p>
         *
         * @param timeout  the timeout
         * @param unit     the time unit for timeout
         *
         * @throws InterruptedException if the thread is interrupted
         */
        public void poll(long timeout, TimeUnit unit)
                throws InterruptedException;

        /**
         * Cancel the current or next {@link #poll execution} as soon as possible.
         */
        public void cancel();
        }

    /**
     * Called by a supporting Bus implementation to bind itself to the collector, and
     * provide the collector with a {@link BusProcessor callback} with which to control
     * the bus execution
     *
     * @param processor the bus processor
     */
    public void bind(BusProcessor processor);
    }
