/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

/**
 * Common monitors allow for a low-cost means to reduce contention by
 * spreading synchronization over a large number of monitors. An example
 * usage would be to produce an "atomic array" without utilizing the
 * Java 1.5 java.util.concurrent features. For instance to atomically change
 * an element within an array which is being simultaneously updated by
 * multiple threads:
 * <pre>
 * synchronized (getCommonMonitor(System.identityHashCode(aoShared) + i))
 *     {
 *     oOld = aoShared[i];
 *     aoShared[i] = oNew;
 *     }
 * </pre>
 * With this approach many threads may concurrently access various array
 * elements without having to synchronize on the array itself, and contend
 * with each other. The use of common monitors also avoids the overhead of
 * allocating a unique monitor per index. This example additionally makes
 * use of the array's identity hash code to avoid frequent collisions against
 * other atomic arrays for the same indices.
 * <p>
 * As they are shared, these monitors will apply to any number of unrelated
 * entities, and as such certain precautions must be employed when using
 * them.
 * <ul>
 * <li>The holder of a common monitor MUST not synchronize on any other
 *     common monitor. Failure to adhere to this precaution will result in
 *     a deadlock.
 * <li>Notifications on a common monitor MUST use notifyAll() rather then
 *     notify(), as there may be unrelated threads waiting for notification
 *     on the same monitor which could consume a single notification. Thus
 *     the only way to ensure that the desired thread does receive
 *     notification is to notify all threads waiting on the monitor.
 * <li>Threads waiting for a notification must protect themselves against
 *     spurious style wake-ups. While this is a general, though often
 *     overlooked part of the normal use of notification, with common
 *     monitors it is far more likely that a thread will be notified due to
 *     an unrelated event.
 * <li>A thread which is holding synchronization on a common monitor should
 *     avoid blocking operations as this could block unrelated threads which
 *     happen to be utilizing the same common monitor.
 * </ul>
 * The ideal number of common monitors in a JVM is one per concurrently
 * executing thread. As this number is generally unknown the actual number
 * of monitors is pre-sized based on a multiple of the number of processors
 * available to the JVM. The value may also be manually specified via the
 * <code>com.oracle.coherence.common.util.CommonMonitor.monitors</code> system property.
 *
 * @author mf 2007.07.05
 */
public final class CommonMonitor
    {
    // ----- CommonMonitor interface -------------------------------------

    /**
    * Ensure all writes made prior to this call to be visible to any thread
    * which calls the corresponding readBarrier method.
    */
    public final void writeBarrier()
        {
        // read from a non-volatile to avoid a read barrier
        int n = ++m_nonvolatile;
        m_barrier = n == -1 ? 0 : n; // write barrier
        }

    /**
    * Ensure all reads made after this call will have visibility to any
    * writes made prior to a corresponding call to writeBarrier on another
    * thread.
    */
    public final void readBarrier()
        {
        if (m_barrier == -1) // read barrier
            {
            // this check should ensure that the compiler does not optimize
            // out the read
            throw new IllegalStateException();
            }
        }


    // ----- static accessors --------------------------------------------

    /**
    * Return the common monitor associated with the specified integer value.
    *
    * @param i the common monitor identifier
    *
    * @return the associated monitor
    */
    public static CommonMonitor getCommonMonitor(int i)
        {
        CommonMonitor[] aMonitors = MONITORS;
        return aMonitors[(i & 0x7FFFFFFF) % aMonitors.length];
        }

    /**
    * Return the common monitor associated with the specified long value.
    *
    * @param l the common monitor identifier
    *
    * @return the associated monitor
    *
    * @see #getCommonMonitor(int)
    */
    public static CommonMonitor getCommonMonitor(long l)
        {
        CommonMonitor[] aMonitors = MONITORS;
        return aMonitors[(int) ((l & 0x7FFFFFFFFFFFFFFFL) % aMonitors.length)];
        }

    /**
    * Return the common monitor associated with the specified object based on
    * its identity hashCode.
    *
    * @param o  the object to obtain a common monitor for
    *
    * @return the associated monitor
    *
    * @see #getCommonMonitor(int)
    */
    public static CommonMonitor getCommonMonitor(Object o)
        {
        return getCommonMonitor(System.identityHashCode(o));
        }


    // ----- constants ------------------------------------------------------

    /**
    * An array of common monitors.
    *
    * @see #getCommonMonitor(int)
    */
    private static final CommonMonitor[] MONITORS;
    static
        {
        // The total overhead of a common monitor is 8 bytes for a 32 bit JVM
        // or 12 bytes for a 64 bit JVM. Ideally the number of monitors will
        // just exceed the number of active threads. Considering that under-
        // allocating will result in increased contention, while over-allocating
        // is not particularly expensive. The default is 512 monitors per CPU, or
        // approximately 4-6KB per CPU.
        String sMonitors = System.getProperty(CommonMonitor.class.getName() + ".monitors");
        int    cMonitors = Runtime.getRuntime().availableProcessors() * 512;
        if (sMonitors != null)
            {
            int c = Integer.parseInt(sMonitors);
            if (c < cMonitors / 8)
                {
                // _trace not available yet
                System.err.println("The specified number of " + c +
                 " common monitors is significantly lower then the recommended " +
                 cMonitors + " and may result in performance degradation.");
                }
            cMonitors = c;
            }

        MONITORS = new CommonMonitor[cMonitors];
        for (int i = 0; i < cMonitors; ++i)
            {
            MONITORS[i] = new CommonMonitor();
            }
        }

    // ----- data members ------------------------------------------------

    protected          int m_nonvolatile;
    protected volatile int m_barrier;
    }
