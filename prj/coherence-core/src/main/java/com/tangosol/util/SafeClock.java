/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

/**
 * SafeClock maintains a "safe" time in milliseconds.
 * <p>
 * Unlike the {@link System#currentTimeMillis()} this clock guarantees that
 * the time never "goes back". More specifically, when queried twice on the
 * same thread, the second query will never return a value that is less then
 * the value returned by the first.
 * <p>
 * If we detect the system clock moving backward, an attempt will be made to
 * gradually compensate the safe clock (by slowing it down), so in the long
 * run the safe time is the same as the system time.
 * <p>
 * The SafeClock supports the concept of "clock jitter", which is a small
 * time interval that the system clock could fluctuate by without a
 * corresponding passage of wall time.
 *
 * @author mf  2009.12.09
 * @since Coherence 3.6
 */
public class SafeClock
        extends com.oracle.coherence.common.util.SafeClock
    {
    /**
     * Create a new SafeClock with the default maximum expected jitter as
     * specified by the "coherence.safeclock.jitter" system
     * property.
     *
     * @param ldtUnsafe the current unsafe time
     */
    public SafeClock(long ldtUnsafe)
        {
        super(ldtUnsafe, DEFAULT_JITTER_THRESHOLD);
        }

    /**
     * Create a new SafeClock with the specified jitter threshold.
     *
     * @param ldtUnsafe the current unsafe time
     * @param lJitter   the maximum expected jitter in the underlying system
     *                  clock
     */
    public SafeClock(long ldtUnsafe, long lJitter)
        {
        super(ldtUnsafe, lJitter);
        }
    }
