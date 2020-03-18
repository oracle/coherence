/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

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
 * @deprecated use {@link com.oracle.coherence.common.base.Timeout} instead
 */
@Deprecated
public class Timeout
        extends com.oracle.coherence.common.base.Timeout
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
        super(cMillis, fForceOverride);
        }

    // ----- static methods -------------------------------------------------

    /**
     * Specify a new timeout.  Note that the calling thread's timeout will only be
     * changed if the specified timeout is less then any existing timeout already
     * active on the thread.
     *
     * @param time  the new timeout
     * @param unit  the unit the timeout is expressed in
     *
     * @return a Timeout instance to be used within a try-with-resource block
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Timeout#after(long, TimeUnit)}
     *             instead
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
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Timeout#after(long)}
     *             instead
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
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Timeout#override(long)}
     *             instead
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
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Timeout#remainingTimeoutMillis()}
     *             instead
     */
    public static long remainingTimeoutMillis()
        {
        return com.oracle.coherence.common.base.Timeout.remainingTimeoutMillis();
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
     *
     * @deprecated use {@link com.oracle.coherence.common.base.Timeout#isTimedOut()}
     *             instead
     */
    public static boolean isTimedOut()
        {
        return com.oracle.coherence.common.base.Timeout.isTimedOut();
        }
    }
