/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.metrics;

import com.tangosol.net.metrics.Rates;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/*
 * This class is heavily inspired by:
 * MeterImpl
 *
 * From Helidon v 2.0.2
 * Distributed under Apache License, Version 2.0
 */

/**
 * A meter metric which measures mean throughput and one-, five-, and fifteen-minute
 * exponentially-weighted moving average throughputs.
 */
public class Meter
        implements Rates
    {
    public Meter()
        {
        this(Clock.system());
        }

    Meter(Clock clock)
        {
        this.startTime = clock.nanoTick();
        this.lastTick = new AtomicLong(startTime);
        this.clock = clock;
        }

    public void mark()
        {
        mark(1);
        }

    public void mark(long n)
        {
        tickIfNecessary();
        count.add(n);
        m1Rate.update(n);
        m5Rate.update(n);
        m15Rate.update(n);
        }

    @Override
    public long getCount()
        {
        return count.sum();
        }

    @Override
    public double getFifteenMinuteRate()
        {
        tickIfNecessary();
        return m15Rate.getRate(TimeUnit.SECONDS);
        }

    @Override
    public double getFiveMinuteRate()
        {
        tickIfNecessary();
        return m5Rate.getRate(TimeUnit.SECONDS);
        }

    @Override
    public double getMeanRate()
        {
        if (getCount() == 0)
            {
            return 0.0;
            }
        else
            {
            final double elapsed = (clock.nanoTick() - startTime);
            return (getCount() / elapsed) * TimeUnit.SECONDS.toNanos(1);
            }
        }

    @Override
    public double getOneMinuteRate()
        {
        tickIfNecessary();
        return m1Rate.getRate(TimeUnit.SECONDS);
        }

    private void tickIfNecessary()
        {
        final long oldTick = lastTick.get();
        final long newTick = clock.nanoTick();
        final long age = newTick - oldTick;
        if (age > TICK_INTERVAL)
            {
            final long newIntervalStartTick = newTick - (age % TICK_INTERVAL);
            if (lastTick.compareAndSet(oldTick, newIntervalStartTick))
                {
                final long requiredTicks = age / TICK_INTERVAL;
                for (long i = 0; i < requiredTicks; i++)
                    {
                    m1Rate.tick();
                    m5Rate.tick();
                    m15Rate.tick();
                    }
                }
            }
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
        Meter that = (Meter) o;
        return getCount() == that.getCount();
        }

    // ----- constants ------------------------------------------------------

    private static final long TICK_INTERVAL = TimeUnit.SECONDS.toNanos(5);

    // ----- data members ---------------------------------------------------

    private final EWMA m1Rate = EWMA.oneMinuteEWMA();
    private final EWMA m5Rate = EWMA.fiveMinuteEWMA();
    private final EWMA m15Rate = EWMA.fifteenMinuteEWMA();
    private final LongAdder count = new LongAdder();
    private final long startTime;
    private final AtomicLong lastTick;
    private final Clock clock;
    }
