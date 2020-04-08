/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;


/**
* The Gate interface acts as an abstraction for {@link ThreadGate}.
*
* @author coh 2010-08-13
*
* @since Coherence 3.7
*/
public interface Gate<R>
    {
    /**
     * Wait to close the gate.
     *
     * @return an AutoCloseable which can be used with a try-with-resource block to perform the corresponding {@link #open}.
     */
    public Sentry<R> close();

    /**
    * Close the gate.  A thread uses this method to obtain exclusive access to
    * the resource represented by the gate.  Each invocation of this method must
    * ultimately have a corresponding invocation of the {@link #open} method.
    *
    * @param cMillis  maximum number of milliseconds to wait;
    *                 pass -1 to wait indefinitely or 0 to return immediately
    *
    * @return true iff entry into the gate was successfully closed by the
    *              calling thread and no other threads remain in the gate
    */
    public boolean close(long cMillis);

    /**
    * Re-open the closed gate. This method can be called only if the calling
    * thread successfully closed the gate.
    *
    * @throws IllegalMonitorStateException  if the gate is not closed or
    *        was closed by a different thread
    */
    public void open();

    /**
     * Wait to enter the gate.
     *
     * @return an AutoCloseable which can be used with a try-with-resource block to perform the corresponding {@link #exit}.
     */
    public Sentry<R> enter();

    /**
    * Enter the gate.  A thread uses this method to obtain non-exclusive
    * access to the resource represented by the gate.  Each invocation
    * of this method must ultimately have a corresponding invocation of the
    * {@link #exit} method.
    *
    * @param cMillis  maximum number of milliseconds to wait;
    *                 pass -1 to wait indefinitely or 0 to return immediately
    *
    * @return true iff the calling thread successfully entered the gate
    */
    public boolean enter(long cMillis);

    /**
    * Exit the gate.  A thread must invoke this method corresponding to each
    * invocation of the {@link #enter} method.
    *
    * @throws IllegalMonitorStateException  if the gate is not entered by the
    *                                       current thread
    */
    public void exit();

    /**
    * Bar entry to the thread gate by other threads, but do not wait for the
    * gate to close. When all other threads have exited, the status of the
    * thread gate will be closeable by the thread which barred entry.
    * Threads that have already entered the gate at the time of this method
    * call should be allowed to succeed in additional #enter calls.
    * <p>
    * Each successful invocation of this method must ultimately have a
    * corresponding invocation of the open method (assuming the thread gate
    * is not destroyed) even if the calling thread does not subsequently
    * close the gate.
    *
    * <pre><code>
    * gate.barEntry(-1);
    * try
    *     {
    *     // processing that does not require the gate to be closed
    *     // ...
    *     }
    * finally
    *     {
    *     gate.close(-1);
    *     try
    *         {
    *         // processing that does require the gate to be closed
    *         // ...
    *         }
    *     finally
    *         {
    *         gate.open(); // matches gate.close()
    *         }
    *     gate.open(); // matches gate.barEntry()
    *     }
    * </code></pre>
    *
    * @param cMillis maximum number of milliseconds to wait;
    *                pass -1 for forever or 0 for no wait
    *
    * @return true iff entry into the thread gate was successfully barred by
    *              the calling thread
    */
    public boolean barEntry(long cMillis);

    /**
    * Determine if the calling thread has closed the gate and continues
    * to hold exclusive access.
    *
    * @return true iff the calling thread holds exclusive access to the gate
    */
    public boolean isClosedByCurrentThread();

    /**
    * Determines if the current thread has entered the gate and not yet exited.
    *
    * @return true if the current thread has entered the gate
    */
    public boolean isEnteredByCurrentThread();

    /**
    * Determine if any thread has closed the gate and continues to hold exclusive access.
    *
    * @return true iff there is a thread that holds exclusive access to the gate
    */
    public boolean isClosed();
    }