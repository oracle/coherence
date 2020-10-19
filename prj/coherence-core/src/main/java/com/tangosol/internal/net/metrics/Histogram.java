/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.metrics;

import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

/*
 * This class is heavily inspired by:
 * HistogramImpl
 *
 * From Helidon v 2.0.2
 * Distributed under Apache License, Version 2.0
 */

/**
 * A metric which calculates the distribution of a value.
 */
public class Histogram
    {
    public Histogram()
        {
        this(Clock.system());
        }

    public Histogram(Clock clock)
        {
        this.reservoir = new ExponentiallyDecayingReservoir(clock);
        }

    public void update(int value)
        {
        update((long) value);
        }

    public void update(long value)
        {
        counter.increment();
        reservoir.update(value);
        }

    public void update(long value, long timestamp)
        {
        counter.increment();
        reservoir.update(value, timestamp);
        }

    public long getCount()
        {
        return counter.sum();
        }

    public Snapshot getSnapshot()
        {
        return reservoir.getSnapshot();
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(super.hashCode(), getCount());
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        Histogram that = (Histogram) o;
        return getCount() == that.getCount();
        }

    // ----- data members ---------------------------------------------------

    private final LongAdder counter = new LongAdder();

    private final ExponentiallyDecayingReservoir reservoir;
    }
