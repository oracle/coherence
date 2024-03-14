/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.metrics.Histogram;
import com.tangosol.internal.net.metrics.Meter;
import com.tangosol.net.metrics.Rates;
import com.tangosol.net.metrics.Snapshot;
import com.tangosol.net.queue.MutableQueueStatistics;
import com.tangosol.net.queue.QueueStatistics;

import java.util.concurrent.atomic.LongAdder;

/**
 * A simple {@link QueueStatistics} implementation.
 */
public class SimpleQueueStatistics
        implements MutableQueueStatistics
    {
    @Override
    public void polled(long cNanos)
        {
        m_pollHistogram.update(cNanos);
        m_pollMeter.mark();
        }

    @Override
    public void offered(long cNanos)
        {
        m_offerHistogram.update(cNanos);
        m_offerMeter.mark();
        }

    @Override
    public void registerHit()
        {
        m_cHits.increment();
        }

    @Override
    public void registerMiss()
        {
        m_cMisses.increment();
        }

    @Override
    public void registerAccepted()
        {
        m_cAccepted.increment();
        }

    @Override
    public void registerRejected()
        {
        m_cRejected.increment();
        }

    @Override
    public Snapshot getPollSnapshot()
        {
        return ensurePollSnapshot();
        }

    @Override
    public Rates getPollRates()
        {
        return m_pollMeter;
        }

    @Override
    public Snapshot getOfferSnapshot()
        {
        return ensureOfferSnapshot();
        }

    @Override
    public Rates getOfferRates()
        {
        return m_offerMeter;
        }

    @Override
    public long getHits()
        {
        return m_cHits.longValue();
        }

    @Override
    public long getMisses()
        {
        return m_cMisses.longValue();
        }

    @Override
    public long getAccepted()
        {
        return m_cAccepted.longValue();
        }

    @Override
    public long getRejected()
        {
        return m_cRejected.longValue();
        }

    @Override
    public void logTo(StringBuilder s)
        {
        s.append("hits=")
                .append(m_cHits.longValue())
                .append(", misses=")
                .append(m_cMisses.longValue())
                .append(", accepted=")
                .append(m_cAccepted.longValue())
                .append(", rejected=")
                .append(m_cRejected.longValue())
                .append(", Poll Rates (in TPS)[");

        appendMeter(s, m_pollMeter);
        s.append("] Poll Latency (in millis)[");
        appendHistogram(s, ensurePollSnapshot());
        s.append("] Offer Rates (in TPS)[");
        appendMeter(s, m_offerMeter);
        s.append("] Offer Latency (in millis)[");
        appendHistogram(s, ensureOfferSnapshot());
        s.append(']');
        }

    // ----- helper methods -------------------------------------------------

    protected Snapshot ensurePollSnapshot()
        {
        long     nLastPollSnapshot = m_nLastPollSnapshot;
        long     nNow              = System.currentTimeMillis();
        Snapshot snapshot          = m_pollSnapshot;
        if ((nNow - nLastPollSnapshot) > 1000)
            {
            snapshot = m_pollSnapshot = m_pollHistogram.getSnapshot();
            m_nLastPollSnapshot = nNow;
            }
        return snapshot;
        }

    protected Snapshot ensureOfferSnapshot()
        {
        long     nLastOfferSnapshot = m_nLastOfferSnapshot;
        long     nNow               = System.currentTimeMillis();
        Snapshot snapshot           = m_offerSnapshot;
        if ((nNow - nLastOfferSnapshot) > 1000)
            {
            snapshot = m_offerSnapshot = m_offerHistogram.getSnapshot();
            m_nLastOfferSnapshot = nNow;
            }
        return snapshot;
        }

    protected void appendHistogram(StringBuilder s, Snapshot snapshot)
        {
        s.append("max=").append(toMillis(snapshot.getMax()))
                .append(",min=").append(toMillis(snapshot.getMin()))
                .append(", mean=").append(toMillis(snapshot.getMean()))
                .append(", median=").append(toMillis(snapshot.getMedian()))
                .append(", 75th%=").append(toMillis(snapshot.get75thPercentile()))
                .append(", 95th%=").append(toMillis(snapshot.get95thPercentile()))
                .append(", 99th%=").append(toMillis(snapshot.get99thPercentile()))
                .append(", 999th%=").append(toMillis(snapshot.get999thPercentile()));
        }

    protected void appendMeter(StringBuilder s, Meter meter)
        {
        s.append("count=").append(meter.getCount())
                .append(", 1Min=").append(toThreeDecimals(meter.getOneMinuteRate()))
                .append(", 5Min=").append(toThreeDecimals(meter.getFiveMinuteRate()))
                .append(", 15Min=").append(toThreeDecimals(meter.getFifteenMinuteRate()))
                .append(", Mean=").append(toThreeDecimals(meter.getMeanRate()));
        }

    private String toMillis(double cNanos)
        {
        return String.format("%.6f", (cNanos / 1000000));
        }

    private String toThreeDecimals(double d)
        {
        return String.format("%.3f", d);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The poll {@link Histogram}.
     */
    private final Histogram m_pollHistogram = new Histogram();

    /**
     * The latest poll {@link Snapshot}.
     */
    private Snapshot m_pollSnapshot;

    /**
     * The timestamp of the latest poll {@link Snapshot}.
     */
    private volatile long m_nLastPollSnapshot;

    /**
     * The poll {@link Meter}.
     */
    private final Meter m_pollMeter = new Meter();

    /**
     * The offer {@link Histogram}.
     */
    private final Histogram m_offerHistogram = new Histogram();

    /**
     * The latest offer {@link Snapshot}.
     */
    private Snapshot m_offerSnapshot;

    /**
     * The timestamp of the latest offer {@link Snapshot}.
     */
    private volatile long m_nLastOfferSnapshot;

    /**
     * The offer {@link Meter}.
     */
    private final Meter m_offerMeter = new Meter();

    private final LongAdder m_cHits = new LongAdder();

    private final LongAdder m_cMisses = new LongAdder();

    private final LongAdder m_cAccepted = new LongAdder();

    private final LongAdder m_cRejected = new LongAdder();
    }
