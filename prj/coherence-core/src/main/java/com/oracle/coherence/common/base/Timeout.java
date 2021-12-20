/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import com.oracle.coherence.common.util.SafeClock;

import java.util.concurrent.TimeUnit;

/**
 * Timeout provides a mechanism for allowing a thread to interrupt itself if it doesn't return
 * to a specific call site within a given timeout.  Timeout instances are intended to be
 * used with the try-with-resource pattern.  Once constructed a Timeout attempts to ensure that
 * the corresponding try-with-resource block completes within the specified timeout and if it
 * does not the thread will self-interrupt.   Exiting the timeout block will automatically clear
 * any interrupt present on the thread and in such a case an InterruptedException will be thrown.
 *
 * <pre>
 * try (Timeout t = Timeout.after(5, TimeUnit.SECONDS))
 *     {
 *     doSomething();
 *     } // this thread will self-interrupt if it doesn't reach this line within 5 seconds
 * catch (InterruptedException e)
 *     {
 *     // thread timed out or was otherwise interrupted
 *     }
 * </pre>
 *
 * In order for this to work any blocking code executed from within the context of the Timeout must use the
 * {@link Blocking} static helper methods for blocking.  An example of a compatible blocking call would be:
 *
 * <pre>
 * void doSomething()
 *     {
 *     Object oField = m_oField;
 *     synchronized (oField)
 *         {
 *         Blocking.wait(oField); // rather then oField.wait();
 *         }
 *     }
 * </pre>
 *
 * Note that Timeout can only self-interrupt at interruptible points, and does not defend against
 * CPU bound loops for example.
 *
 * @author  mf 2015.02.23
 */
public class Timeout
    implements AutoCloseable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Specify a new timeout.
     *
     * This constructor variant allows the caller to override a parent timeout.  This is
     * rarely needed, and is roughly the equivalent of silently consuming a thread interrupt
     * without rethrowing the InterruptedException.
     *
     * @param cMillis         the new timeout.
     * @param fForceOverride  true if this timeout is allowed to extend a parent timeout.
     */
    protected Timeout(long cMillis, boolean fForceOverride)
        {
        MutableLong mlTimeout = s_tloTimeout.get();
        if (mlTimeout == null)
            {
            s_tloTimeout.set(mlTimeout = new MutableLong(Long.MAX_VALUE));
            f_fTloCreator = true;
            }
        else
            {
            f_fTloCreator = false;
            }

        f_mlTimeout    = mlTimeout;
        f_lTimeoutOrig = f_mlTimeout.get();

        if (f_lTimeoutOrig == Long.MAX_VALUE) // orig is disabled (common)
            {
            f_cMillisTimeout = cMillis;
            f_mlTimeout.set(-cMillis);
            }
        else if (f_lTimeoutOrig < 0) // orig is relative (common)
            {
            if (fForceOverride || cMillis < -f_lTimeoutOrig)
                {
                f_cMillisTimeout = cMillis;
                f_mlTimeout.set(-cMillis);
                }
            else // we are not allowed to extend an existing timeout
                {
                f_cMillisTimeout = f_lTimeoutOrig;
                }
            }
        else // orig is timestamp
            {
            // TODO: we could avoid pulling the time here if we retained a ref to the prior Timeout object
            // rather then just it's timeout value.  In this case we'd have the absolute timeout and it's
            // relative value and could then compute our updated absolute from those.
            long ldtTimeout = SafeClock.INSTANCE.getSafeTimeMillis() + cMillis;
            if (fForceOverride || ldtTimeout < f_lTimeoutOrig)
                {
                f_cMillisTimeout = cMillis;
                f_mlTimeout.set(ldtTimeout);
                }
            else // we are not allowed to extend an existing timeout
                {
                f_cMillisTimeout = f_lTimeoutOrig;
                }
            }
        }


    // ----- AutoCloseable interface ----------------------------------------

    /**
     * As part of closing the Timeout resource any former timeout will be restored.
     *
     * @throws InterruptedException if the calling thread is interrupted
     */
    @Override
    public void close()
        throws InterruptedException
        {
        // we restore the former timeout, even if it is expired

        if (f_fTloCreator)
            {
            // cleanup the TLO when the Timeout stack has been fully popped
            s_tloTimeout.set(null);
            }
        else if (f_lTimeoutOrig < 0) // orig was never realized
            {
            long lTimeoutCurr = f_mlTimeout.get();
            if (lTimeoutCurr < 0 ||             // we've yet to block
                lTimeoutCurr == Long.MAX_VALUE) // timeout was disabled (note restore is suspect, but override has already violated orig)
                {
                // simply restore the orig value
                f_mlTimeout.set(f_lTimeoutOrig);
                }
            else
                {
                // curr was realized, orig was not, adjust orig accordingly
                // and set it as new timeout
                f_mlTimeout.set(lTimeoutCurr + (-f_lTimeoutOrig - f_cMillisTimeout));
                }
            }
        else // orig is realized, simply restore it
            {
            f_mlTimeout.set(f_lTimeoutOrig);
            }

        // checking to see if the thread is interrupted here ensures that if the nested code within the
        // interrupt block were to suppress the InterruptedException (possibly from a timeout) that it
        // gets recreated here so the application is forced to deal with it
        // Note we don't just throw because of a timeout, as the general contract of a method which
        // throws InterruptedException is that it throws if the thread in interrupted, period
        // Note: we don't try to throw some derived exception such as InterruptedTimeoutException as
        // we can't ensure that all timeout points would actually result in that exception.  For instance
        // a timeout in LockSupport.park() will interrupt the thread by throw nothing, and some other code
        // could then detect the interrupt and throw a normal InterruptedException.  Overall the intent
        // here is to just make the timeout feature be indistinugisable from another thread interrupting
        // this the thread.
        if (Thread.interrupted())
            {
            throw new InterruptedException();
            }
        }

    // ----- static interface -----------------------------------------------

    // Note: the use of static factory methods in addition to being more expressive
    // then public constructors allows for the potential to pool Timeout objects
    // in the future to further reduce the cost of creating a timeout block.
    // It would seem likely that Timeout objects may live for a decent enough duration
    // that they could become tenured, and thus pooling would be worth consideration.
    // The pool could also be stored in a ThreadLocal and could simply be an array of
    // Timeouts and an index into the next free slot.  Considering that they are
    // effectively bound to a callsite and the stack depth the expectation is that
    // there would be a relatively small number of them per thread.  If implemented
    // this ThreadLocal could also hold the MutableLong timeout thus avoiding the
    // need for multiple ThreadLocal lookups.

    /**
     * Specify a new timeout.  Note that the calling thread's timeout will only be
     * changed if the specified timeout is less then any existing timeout already
     * active on the thread.
     *
     * @param time  the new timeout
     * @param unit  the unit the timeout is expressed in
     *
     * @return a Timeout instance to be used within a try-with-resource block
     */
    public static Timeout after(long time, TimeUnit unit)
        {
        return after(Math.max(unit.toMillis(time), 1)); // ensure at least 1ms in case duration was expressed as sub-millisecond
        }

    /**
     * Specify a new timeout.  Note that the calling thread's timeout will only be
     * changed if the specified timeout is less then any existing timeout already
     * active on the thread.
     *
     * @param cMillis  the new timeout
     *
     * @return a Timeout instance to be used within a try-with-resource block
     */
    public static Timeout after(long cMillis)
        {
        return new Timeout(cMillis, /*fForceOverride*/ false);
        }

    /**
     * Specify a new timeout, potentially extending an already active timeout.
     * <p>
     * This variant allows the caller to extend a parent timeout.  This is rarely
     * needed, and is roughly the equivalent of silently consuming a thread interrupt
     * without rethrowing the InterruptedException.  Use of this method should
     * be extremely limited.
     *
     * @param cMillis  the new timeout
     *
     * @return a Timeout instance to be used within a try-with-resource block
     */
    public static Timeout override(long cMillis)
        {
        return new Timeout(cMillis, /*fForceOverride*/ true);
        }

    /**
     * Return the number of milliseconds before this thread will timeout.
     *
     * Note if the current thread is timed out then invoking this method will
     * also interrupt the thread. This method can be used to externally
     * add Timeout support for other blocking APIs not covered by the existing
     * Timeout helpers.
     *
     * @return the number of remaining milliseconds, 0 if timed out, or Long.MAX_VALUE if disabled.
     */
    public static long remainingTimeoutMillis()
        {
        MutableLong mlTimeout = s_tloTimeout.get();
        if (mlTimeout == null)
            {
            // no timeout configured; avoid pulling local time
            return Long.MAX_VALUE;
            }

        long lTimeout = mlTimeout.get();
        long ldtNow   = SafeClock.INSTANCE.getSafeTimeMillis();
        if (lTimeout < 0)
            {
            // timeout is still relative; actualize and store it
            mlTimeout.set(ldtNow - lTimeout); // sets timeout as now + -lTimeout
            return -lTimeout; // no need to compute relative
            }
        // else; timeout was already realized, compute remainder

        long cMillis = lTimeout - ldtNow;
        if (cMillis <= 0)
            {
            Thread.currentThread().interrupt();
            return 0;
            }
        return cMillis;
        }

    /**
     * Return true if the calling thread is timed out.
     *
     * Note if the current thread is timed out then invoking this method will
     * also interrupt the thread. This method can be used to externally
     * add Timeout support for other blocking APIs not covered by the existing
     * Timeout helpers.
     *
     * @return true if the calling thread is timed out
     */
    public static boolean isTimedOut()
        {
        return remainingTimeoutMillis() == 0;
        }

    /**
     * Return true if calling thread specified a timeout.
     *
     * @return true if a timeout is set
     */
    public static boolean isSet()
        {
        return remainingTimeoutMillis() != Long.MAX_VALUE;
        }


    // ----- data members ---------------------------------------------------

    /**
     * True iff this Timeout created (and thus must ultimately destroy) the TLO.
     */
    protected final boolean f_fTloCreator;

    /**
     * Cached reference to the thread's MutableLong holding it's current timeout.
     */
    protected final MutableLong f_mlTimeout;

    /**
     * This Timeout's timeout.
     */
    protected final long f_cMillisTimeout;

    /**
     * The original timeout before this instance changed it.
     */
    protected final long f_lTimeoutOrig;

    /**
     * A thread-local containing the calling thread's timeout value. Value which are greater or equal to zero
     * are used to indicate timeout timestamps.  Negative values are relative timeouts which haven't yet been
     * realized into a timestamp.  This allows for an optimization where we can avoid obtaining
     * the current time when "setting" the timeout, and defer it until we are about to block.
     */
    protected static final ThreadLocal<MutableLong> s_tloTimeout = new ThreadLocal<>();
    }
