/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

import java.util.concurrent.atomic.AtomicBoolean;

import java.security.AccessController;
import java.security.PrivilegedAction;


/**
 * SafeClock maintains a "safe" time in milliseconds.
 * <p>
 * Unlike the {@link System#currentTimeMillis()} this clock guarantees that
 * the time never "goes back". More specifically, when queried twice on the
 * same thread, the second query will never return a value that is less then
 * the value returned by the first.
 * <p>
 * If it is detected that the system clock moved backward, an attempt will be
 * made to gradually compensate the safe clock (by slowing it down), so in the
 * long run the safe time is the same as the system time.
 * <p>
 * The SafeClock supports the concept of "clock jitter", which is a small
 * time interval that the system clock could fluctuate by without a
 * corresponding passage of wall time.
 * <p>
 * In most cases the {@link SafeClock#INSTANCE} singleton should be used
 * rather then creating new SafeClock instances.
 *
 * @author mf  2009.12.09
 */
public class SafeClock
    extends AtomicBoolean // embedded "lock" which will be on the same cache line as the clock's data members
    {
    /**
     * Create a new SafeClock with the default maximum expected jitter as
     * specified by the {@link #DEFAULT_JITTER_THRESHOLD} constant.
     */
    public SafeClock()
        {
        this(System.currentTimeMillis());
        }

    /**
     * Create a new SafeClock with the default maximum expected jitter as
     * specified by the {@link #DEFAULT_JITTER_THRESHOLD} constant.
     *
     * @param ldtUnsafe the current unsafe time
     */
    public SafeClock(long ldtUnsafe)
        {
        this(ldtUnsafe, DEFAULT_JITTER_THRESHOLD);
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
        m_ldtLastSafe = m_ldtLastUnsafe = ldtUnsafe;
        m_lJitter = lJitter;
        }

    /**
     * Returns a "safe" current time in milliseconds.
     *
     * @return the difference, measured in milliseconds, between the
     * corrected current time and midnight, January 1, 1970 UTC.
     */
    public final long getSafeTimeMillis()
        {
        return getSafeTimeMillis(System.currentTimeMillis());
        }

    /**
     * Returns a "safe" current time in milliseconds.
     *
     * @param ldtUnsafe the current unsafe time
     *
     * @return the difference, measured in milliseconds, between the
     * corrected current time and midnight, January 1, 1970 UTC.
     */
    public final long getSafeTimeMillis(long ldtUnsafe)
        {
        // optimization for heavy concurrent load: if no time has passed, or
        // time jumped back within the expected jitter just return the last
        // time and avoid CAS contention; keep short to encourage hot-spotting
        long lDelta = ldtUnsafe - m_ldtLastUnsafe;

        return lDelta == 0 || (lDelta < 0 && lDelta >= -m_lJitter)
                ? m_ldtLastSafe // common case during heavy load
                : updateSafeTimeMillis(ldtUnsafe);
        }

    /**
     * Returns the last "safe" time as computed by a previous call to the
     * {@link #getSafeTimeMillis} method.
     * <p>
     * Note: Since the underlying field is non-volatile, the returned value
     * is only guaranteed to be no less than the last value returned by
     * getSafeTimeMillis() call on the same thread.
     *
     * @return the last "safe" time in milliseconds
     */
    public final long getLastSafeTimeMillis()
        {
        return m_ldtLastSafe;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Updates and returns a "safe" current time in milliseconds based on the
     * "unsafe" time.
     *
     * @param ldtUnsafe the unsafe current time in milliseconds
     *
     * @return the corrected safe time
     */
    protected long updateSafeTimeMillis(long ldtUnsafe)
        {
        if (compareAndSet(false, true))
            {
            try
                {
                long lJitter     = m_lJitter;
                long ldtLastSafe = m_ldtLastSafe;
                long lDelta      = ldtUnsafe - m_ldtLastUnsafe;
                long ldtNewSafe  = ldtLastSafe;

                if (lDelta > 0)
                    {
                    // unsafe progressed
                    if (ldtUnsafe >= ldtLastSafe)
                        {
                        // common case; unsafe is at or ahead of safe; sync clocks
                        ldtNewSafe = ldtUnsafe;
                        }
                    else if (lDelta > lJitter && ldtLastSafe - ldtUnsafe <= lJitter)
                        {
                        // unsafe is behind safe and jumped; the jump brought it
                        // very close (within jitter) to where it was before the
                        // corresponding regression; this appears to be jitter, hold
                        // safe and avoid recording anything about this bogus jump as
                        // that could artificially push safe into the future
                        return ldtLastSafe;
                        }
                    else
                        {
                        // unsafe is behind safe and progressed; progress safe slowly
                        // at half the measured delta or every other ms if delta is 1ms
                        // allowing unsafe to eventually catch up
                        ldtNewSafe += lDelta == 1 ? ldtUnsafe % 2 : lDelta / 2;
                        }
                    }
                else if (lDelta >= -lJitter)
                    {
                    // unsafe made an insignificant (within jitter) regression; or
                    // didn't move at all; hold safe and avoid recording anything about
                    // this bogus jump as that could artificially push safe into the future
                    // Note: the same cases are handled in getSafeTimeMillis() but based
                    // on synchronization ordering it may not be detected until here
                    return ldtLastSafe;
                    }

                // except in the case of jitter we update our clocks
                m_ldtLastUnsafe = ldtUnsafe;
                return m_ldtLastSafe = ldtNewSafe;
                }
            finally
                {
                set(false); // unlock
                }
            }
        else
            {
            // some other thread has locked the clock we have a few options
            // - block until they complete, but who likes global contention
            // - spin until they complete, but then we just waste CPU, and for what gain?
            // - pretend like time has not advanced, no worse then the above and we get to do useful work
            return m_ldtLastSafe; // note since we've attempted the CAS this is as good as a volatile read
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The last known safe time value.
     */
    protected long m_ldtLastSafe;

    /**
     * The last recorded unsafe time value.
     */
    protected long m_ldtLastUnsafe;

    /**
     * The maximum expected jitter exposed by the underlying unsafe clock.
     */
    protected final long m_lJitter;

    // ----- constants ------------------------------------------------------

    /**
     * SafeClock singleton.
     */
    public static final SafeClock INSTANCE;

    /**
     * The default jitter threshold.
     */
    public static final long DEFAULT_JITTER_THRESHOLD;

    static
        {
        DEFAULT_JITTER_THRESHOLD = Long.parseLong(AccessController.doPrivileged(
                (PrivilegedAction<String>) () ->
                    {
                    // Note: we do not use Config.getProperty to avoid a com.tangosol
                    //       import and the acceptable loss of not supporting a property
                    //       name that starts with tangosol.
                    return System.getProperty("coherence.safeclock.jitter",
                                System.getProperty(SafeClock.class.getName() + ".jitter",
                                "16"));
                    }));

        INSTANCE = new SafeClock();
        }
    }
