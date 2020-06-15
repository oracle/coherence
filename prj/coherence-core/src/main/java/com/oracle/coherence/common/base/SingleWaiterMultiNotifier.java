/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;


import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;


/**
 * A Condition-like object, used by a single thread to block for a
 * notification, and optimized for many concurrent notifications by other
 * threads. Basically, this is a blocking queue without any state to
 * actually enqueue: the {@link SingleWaiterMultiNotifier#await()} method
 * is analogous to an imaginary "take all" variant of the
 * {@link java.util.concurrent.BlockingQueue#take() BlockingQueue.take()}
 * method, and the
 * {@link SingleWaiterMultiNotifier#signal()} method is analogous to
 * {@link java.util.concurrent.BlockingQueue#put(Object)
 * BlockingQueue.put()}.
 * <p>
 * Note that no synchronization is needed to use this class; i.e. clients
 * must not synchronize on this class prior to calling <tt>await()</tt> or
 * <tt>signal()</tt>, nor should the use any of the primitive <tt>wait()</tt>
 * or <tt>notify()</tt> methods.
 * <p>
 * Since SingleWaiterMultiNotifier is only usable by a single waiting thread it is
 * does not require an external readiness check, as signaling can record that state.
 *
 * @author cp/mf  2010-06-15
 */
public class SingleWaiterMultiNotifier
        implements Notifier
    {
    @Override
    public void await(long cMillis)
            throws InterruptedException
        {
        if (m_oState == this)
            {
            // pre-signaled; extra volatile read is "free" as compared to CAS
            // volatile write is same roughly the same cost as CAS
            // overall this path is about 2x faster then the equivalent pre-signaled CAS path
            consumeSignal();
            }
        else if (s_fuState.compareAndSet(this, null, Thread.currentThread()))
            {
            if (cMillis == 0)
                {
                Blocking.park(/*blocker*/ this);
                }
            else
                {
                Blocking.parkNanos(/*blocker*/ this, cMillis * 1000000);
                }

            if (m_oState != this && Blocking.interrupted()) // only pay the cost of interrupt check if we were not signaled
                {
                consumeSignal();
                throw new InterruptedException();
                }

            consumeSignal();
            }
        else if (m_oState == this)
            {
            // concurrently signaled
            consumeSignal();
            }
        else // m_oState references another thread (or null)
            {
            // note, even if m_oState is null we have an error as it could only happen because another
            // thread had concurrently awaited, which is not safe and could leave one of the thread stuck
            throw new IllegalStateException("unexpected thread '" + m_oState + "' is also awaiting");
            }
        }

    @Override
    public void signal()
        {
        LockSupport.unpark(signalInternal());
        }


    // ----- Object interface -----------------------------------------------

    @Override
    public String toString()
        {
        Object oState = m_oState;
        return super.toString() + (oState == this ? " signaled" : " unsignaled(" + /*Thread*/ oState + ")");
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Return the thread which is waiting on this notifier.
     *
     * @return  the waiting thread or null
     */
    public Thread getWaitingThread()
        {
        Object oState = m_oState;
        return oState == null || oState == this ? null : (Thread) oState;
        }

    /**
     * Signal the notifier returning any thread which needs to be unparked.
     *
     * @return a the thread if any which needs to be unparked.
     */
    protected Thread signalInternal()
        {
        // from a performance perspective we want to minimize the number of CASs and thread wakeups
        // we can optimize out repeated/concurrent signals as well as signals which occur concurrently
        // with a thread wakeing up, we cannot optimize out signals which occur concurrentlly with a
        // thread going into await

        Object oState = m_oState;
        if (oState == null)
            {
            if (s_fuState.compareAndSet(this, null, /*signaled*/ this))
                {
                // not yet signaled, no thread was waiting
                return null;
                }
            // else new waiter or concurrent signal; fall through
            oState = m_oState;
            }

        if (oState != this && oState != null && s_fuState.compareAndSet(this, /*thread*/ oState, /*signaled*/ this))
            {
            // we've signled a waiting thread
            return (Thread) oState;
            }
        else
            {
            // already signaled, concurrently signal, or thread concurrently awoke
            return null;
            }
        }

    /**
     * Consume the signal.
     */
    protected void consumeSignal()
        {
        m_oState = null;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The signaling state, null for unsignaled, this for signaled, or a thread for the single awaiting thread
    */
    protected volatile Object m_oState;

    /**
    * The atomic field updater for {@link #m_oState}.
    */
    private static final AtomicReferenceFieldUpdater<SingleWaiterMultiNotifier, Object> s_fuState =
            AtomicReferenceFieldUpdater.newUpdater(SingleWaiterMultiNotifier.class, Object.class, "m_oState");
    }
