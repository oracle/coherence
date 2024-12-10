/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.metrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static java.lang.StrictMath.exp;

/*
 * This class is heavily inspired by:
 * EWMA
 *
 * From Helidon v 2.0.2
 * Distributed under Apache License, Version 2.0
 */

/**
 * An exponentially-weighted moving average.
 *
 * @see <a href="http://www.teamquest.com/pdfs/whitepaper/ldavg1.pdf">UNIX Load Average Part 1: How
 * It Works</a>
 * @see <a href="http://www.teamquest.com/pdfs/whitepaper/ldavg2.pdf">UNIX Load Average Part 2: Not
 * Your Average Average</a>
 * @see <a href="http://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average">EMA</a>
 */
final class EWMA
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new EWMA with a specific smoothing constant.
     *
     * @param nAlpha       the smoothing constant
     * @param nInterval    the expected tick interval
     * @param intervalUnit the time unit of the tick interval
     */
    private EWMA(double nAlpha, long nInterval, TimeUnit intervalUnit)
        {
        this.m_nInterval = intervalUnit.toNanos(nInterval);
        this.m_nAlpha = nAlpha;
        }

    // ----- EWMA methods ---------------------------------------------------

    /**
     * Update the moving average with a new value.
     *
     * @param n the new value
     */
    void update(long n)
        {
        m_cUncounted.add(n);
        }

    /**
     * Mark the passage of time and decay the current rate accordingly.
     */
    void tick()
        {
        final long count = m_cUncounted.sumThenReset();
        final double instantRate = count / m_nInterval;
        if (m_fInitialized)
            {
            double currentRate = m_nRate;
            currentRate += (m_nAlpha * (instantRate - currentRate));
            // we may lose changes that happen at the very same moment as the previous two lines,
            // though better than being inconsistent
            m_nRate = currentRate;
            }
        else
            {
            m_nRate = instantRate;
            m_fInitialized = true;
            }
        }

    /**
     * Returns the rate in the given units of time.
     *
     * @param rateUnit the unit of time
     * @return the rate
     */
    double getRate(TimeUnit rateUnit)
        {
        return m_nRate * (double) rateUnit.toNanos(1);
        }

    /**
     * Creates a new EWMA which is equivalent to the UNIX one minute load average and which expects to be ticked every 5
     * seconds.
     *
     * @return a one-minute EWMA
     */
    static EWMA oneMinuteEWMA()
        {
        return new EWMA(M1_ALPHA, INTERVAL, TimeUnit.SECONDS);
        }

    /**
     * Creates a new EWMA which is equivalent to the UNIX five minute load average and which expects to be ticked every
     * 5 seconds.
     *
     * @return a five-minute EWMA
     */
    static EWMA fiveMinuteEWMA()
        {
        return new EWMA(M5_ALPHA, INTERVAL, TimeUnit.SECONDS);
        }

    /**
     * Creates a new EWMA which is equivalent to the UNIX fifteen minute load average and which expects to be ticked
     * every 5 seconds.
     *
     * @return a fifteen-minute EWMA
     */
    static EWMA fifteenMinuteEWMA()
        {
        return new EWMA(M15_ALPHA, INTERVAL, TimeUnit.SECONDS);
        }

    // ----- constants ------------------------------------------------------

    private static final int INTERVAL = 5;
    private static final double SECONDS_PER_MINUTE = 60.0;
    private static final int ONE_MINUTE = 1;
    private static final int FIVE_MINUTES = 5;
    private static final int FIFTEEN_MINUTES = 15;
    private static final double M1_ALPHA = 1 - exp(-INTERVAL / SECONDS_PER_MINUTE / ONE_MINUTE);
    private static final double M5_ALPHA = 1 - exp(-INTERVAL / SECONDS_PER_MINUTE / FIVE_MINUTES);
    private static final double M15_ALPHA = 1 - exp(-INTERVAL / SECONDS_PER_MINUTE / FIFTEEN_MINUTES);

    // ----- data members ---------------------------------------------------

    private final LongAdder m_cUncounted = new LongAdder();
    private final double m_nAlpha;
    private final double m_nInterval;
    private volatile boolean m_fInitialized = false;
    private volatile double m_nRate = 0.0;
    }
