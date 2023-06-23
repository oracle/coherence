/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;


import com.oracle.coherence.common.base.IdentityHolder;
import com.oracle.coherence.common.base.MutableLong;
import com.oracle.coherence.common.collections.ConcurrentHashMap;
import com.oracle.coherence.common.collections.InflatableMap;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
* Use this class in cases that large numbers of threads can operate
* concurrently with an additional requirement that all threads be blocked for
* certain operations.  The algorithm is based on a gate concept, allowing
* threads in (enter) and out (exit), but occasionally shutting the gate (close)
* such that other threads cannot enter and exit.  However, since threads may
* "be inside", the gate cannot fully close until they leave (exit).  Once all
* threads are out, the gate is closed, and can be re-opened (open) or
* permanently closed (destroy).
* <p/>
* Each call to enter requires a corresponding call to exit, similar to the JVM
* implementation of the "synchronized" keyword that places a monitorenter op
* that the beginning of the synchronized portion and protects the synchronized
* portion with a try..finally construct that ensures the execution of a
* monitorexit op.  For example, the following would ensure proper clean-up
* using a ThreadGate:
* <pre>
* gate.enter();
* try
*     {
*     ...
*     }
* finally
*     {
*     gate.exit();
*     }
* </pre>
* or simply:
* <pre>
* try (Sentry sentry = gate.enter())
*     {
*     ...
*     }
* </pre>
* Similarly, each call to close() should be matched with a call to open(),
* unless the gate is being destroyed:
* <pre>
* gate.close();
* try
*     {
*     ...
*     }
* finally
*     {
*     gate.open();
*     }
* </pre>
* or simply:
* <pre>
* try (Sentry sentry = gate.close())
*     {
*     ...
*     }
* </pre>
* and to permanently close, i.e. destroy the gate:
* <pre>
* gate.close();
* gate.destroy();
* </pre>
* The enter/exit calls can be nested; the same thread can invoke enter multiple
* times as long as exit is invoked a corresponding number of times.  The
* close/open calls work in the same manner.  Lastly, the thread that closes the
* gate may continue to enter/exit the gate even when it is closed since that
* thread has exclusive control of the gate.
* <p>
* This Gate implementation also supports non-thread-based lock contenders. When non-thread-based
* contenders are used the contender may internally be synchronized on during gate operations.
* While any object can be used as a non-thread contender, {@link ThreadGate.LiteContender} instances
* are preferable.
* <p>
* For performance critical cases which don't require reentrancy consider the {@link NonReentrant} variant.
*
* @author cp 2003.05.26; mf 2007.04.27; coh 2010.08.13; mf 2017.03.21
* @since Coherence 2.2
*
* @param <R> the type of resource protected by this gate
*/
public class ThreadGate<R>
        implements Gate
    {
    /**
    * Default constructor.
    */
    public ThreadGate()
        {
        this (null);
        }

    /**
     * Construct a gate protecting the specified resource.
     *
     * @param resource  the resource, or null
     */
    public ThreadGate(R resource)
        {
        f_resource = resource;
        }

    // ---- public API ------------------------------------------------------
    /**
    * {@inheritDoc}
    */
    @Override
    public boolean barEntry(long cMillis)
        {
        return barEntryInternal(Thread.currentThread(), cMillis); // thread needed even if non-reentrant
        }

    /**
     * Bar entry, using the specified (potentially non-thread based) lock contender.
     *
     * @param oContender  the lock contender
     * @param cMillis  the timeout
     *
     * @return true iff barred
     */
    public boolean barEntry(Object oContender, long cMillis)
        {
        synchronized (oContender)
            {
            return barEntryInternal(oContender, cMillis);
            }
        }

    /**
     * Internal version of bar-entry.
     *
     * @param oContender  the lock contender
     * @param cMillis     the timeout
     *
     * @return true iff barred
     */
    protected boolean barEntryInternal(Object oContender, long cMillis)
        {
        if (oContender == getCloser())
            {
            // we've already closed or are closing the gate
            setCloseCount(getCloseCount() + 1);
            return true;
            }

        lock();
        try
            {
            while (true)
                {
                if (getCloser() == null)
                    {
                    // transition to CLOSING state
                    if (updateStatus(GATE_CLOSING) == GATE_DESTROYED)
                        {
                        // oops gate was destroyed while we were waiting
                        updateStatus(GATE_DESTROYED);
                        throw new IllegalStateException("ThreadGate.close:"
                            + " ThreadGate has been destroyed.");
                        }

                    setCloser(oContender);
                    setCloseCount(1);
                    return true;
                    }

                // gate is already closed or closing, wait for notification
                cMillis = doWait(cMillis);
                if (cMillis == 0)
                    {
                    return false;
                    }
                }
            }
        finally
            {
            unlock();
            }
        }

    /**
     * Wait to close the gate.
     *
     * @return an AutoCloseable which can be used with a try-with-resource block to perform the corresponding {@link #open}.
     */
    public Sentry<R> close()
        {
        close(-1);
        return f_openSentry;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean close(long cMillis)
        {
        return closeInternal(Thread.currentThread(), cMillis); // thread needed even if non-reentrant
        }

    /**
     * Close the gate, using the specified (potentially non-thread based) lock contender.
     *
     * @param oContender  the lock contender
     * @param cMillis  the timeout
     *
     * @return true iff barred
     */
    public boolean close(Object oContender, long cMillis)
        {
        synchronized (oContender)
            {
            return closeInternal(oContender, cMillis);
            }
        }

    /**
     * Internal version of close.
     *
     * @param oContender  the lock contender
     * @param cMillis  the timeout
     *
     * @return true iff barred
     */
    protected boolean closeInternal(Object oContender, long cMillis)
        {
        if (oContender == getCloser() && getStatus() == GATE_CLOSED)
            {
            // we've already closed the gate
            setCloseCount(getCloseCount() + 1);
            return true;
            }

        AtomicLong atomicState = f_atomicState;
        long       cEnterThis  = Math.min(1L, getEnterCount(oContender)); // cEnterThis is this just contenders contribution to the active count, which is at most 1
        long       lStatusReq  = EMPTY_GATE_OPEN   | cEnterThis;
        long       lStatusEnd  = EMPTY_GATE_CLOSED | cEnterThis;
        boolean    fReenter    = false;
        boolean    fReopen     = false;

        lock();
        try
            {
            try
                {
                if (oContender == getCloser())
                    {
                    lStatusReq = EMPTY_GATE_CLOSING;

                    // if we've also "entered" we need to temporarily
                    // decrement the counter so that the last thread to
                    // exit the gate will know to notify us
                    if (cEnterThis > 0)
                        {
                        fReenter = true;
                        atomicState.addAndGet(-cEnterThis);
                        }
                    }

                while (true)
                    {
                    if (atomicState.compareAndSet(lStatusReq, lStatusEnd))
                        {
                        // we've closed the gate
                        setCloseCount(getCloseCount() + 1);
                        setCloser(oContender); // in case we bypassed GATE_CLOSING
                        fReenter = fReopen = false;
                        return true;
                        }
                    else if (getCloser() == null)
                        {
                        // transition to CLOSING state
                        if (updateStatus(GATE_CLOSING) == GATE_DESTROYED)
                            {
                            // oops gate was destroyed while we were waiting
                            updateStatus(GATE_DESTROYED);
                            throw new IllegalStateException("ThreadGate.close: ThreadGate has been destroyed.");
                            }

                        setCloser(oContender);
                        lStatusReq = EMPTY_GATE_CLOSING;
                        fReopen    = true; // reopen if we fail

                        // if we've also "entered" we need to temporarily
                        // decrement the counter so that the last thread to
                        // exit the gate will know to notify us
                        if (cEnterThis > 0)
                            {
                            fReenter = true;
                            atomicState.addAndGet(-cEnterThis);
                            }

                        // as we've just transitioned to CLOSING we must
                        // retest the active count since exiting threads only
                        // notify if they when in the state is CLOSING, thus
                        // we can't go to doWait without retesting
                        continue;
                        }

                    // gate is closed or closing, wait for notification
                    cMillis = doWait(cMillis);
                    if (cMillis == 0)
                        {
                        return false;
                        }
                    }
                }
            finally
                {
                // if we transitioned to closing but didn't make it to
                // closed; re-open the gate
                if (fReenter)
                    {
                    atomicState.addAndGet(cEnterThis); // undo temporary decrement
                    }

                if (fReopen)
                    {
                    setCloser(null);
                    updateStatus(GATE_OPEN);
                    f_open.signalAll();
                    }
                }
            }
        finally
            {
            unlock();
            }
        }

    /**
    * Destroy the thread gate.  This method can only be invoked if the gate is
    * already closed.
    */
    public void destroy()
        {
        destroyInternal(Thread.currentThread());
        }

    /**
    * Destroy the thread gate.  This method can only be invoked if the gate is
    * already closed.
    *
    * @param oContender the contender destroying the gate
    */
    public void destroy(Object oContender)
        {
        synchronized (oContender)
            {
            destroyInternal(oContender);
            }
        }

    /**
    * Internal version of destroy.
    *
    * @param oContender the contender destroying the gate
    */
    protected void destroyInternal(Object oContender)
        {
        lock();
        try
            {
            switch (getStatus())
                {
                case GATE_CLOSED:
                {
                if (oContender != getCloser())
                    {
                    throw new IllegalStateException(
                            "ThreadGate.destroy: Gate was not closed by " + oContender + "; "
                            + this);
                    }

                updateStatus(GATE_DESTROYED);
                setCloser(null);
                f_open.signalAll();
                }
                break;

                case GATE_DESTROYED:
                    // the gate has already been destroyed
                    break;

                default:
                    throw new IllegalStateException(
                            "ThreadGate.destroy: Gate is not closed! " + this);
                }
            }
        finally
            {
            unlock();
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean enter(long cMillis)
        {
        // optimized common-path; i.e. already entered or open gate which hasn't become full.
        if (adjustThreadLocalEnters(1) > 1)
            {
            return true; // already entered
            }

        AtomicLong atomicState = f_atomicState;
        for (long lStatus = atomicState.get(); lStatus < FULL_GATE_OPEN; lStatus = atomicState.get())
            {
            if (atomicState.compareAndSet(lStatus, lStatus + 1))
                {
                // atomic set succeeded confirming that the gate
                // remained open and that we made it in
                return true;
                }
            }
        // otherwise; fall through

        adjustThreadLocalEnters(-1); // undo our increment from above
        return enterInternal(Thread.currentThread(), cMillis);
        }

    /**
     * Wait to enter the gate.
     *
     * @return an AutoCloseable which can be used with a try-with-resource block to perform the corresponding {@link #exit}.
     */
    public Sentry<R> enter()
        {
        enter(-1L);
        return f_exitSentry;
        }

    /**
     * Enter the gate, using the specified (potentially non-thread based) lock contender.
     *
     * @param oContender  the lock contender
     * @param cMillis  the timeout
     *
     * @return true iff barred
     */
    public boolean enter(Object oContender, long cMillis)
        {
        synchronized (oContender)
            {
            return enterInternal(oContender, cMillis);
            }
        }

    /**
     * Internal version of enter.
     *
     * @param oContender  the lock contender
     * @param cMillis  the timeout
     *
     * @return true iff barred
     */
    protected boolean enterInternal(Object oContender, long cMillis)
        {
        AtomicLong atomicState = f_atomicState;

        // increment local enter count and check if we are already entered
        if (incrementEnterCount(oContender) > 1L)
            {
            // we'd already entered; all we needed to do was increment our contender enter count
            return true;
            }
        else if (oContender == getCloser())
            {
            // we've either closed the gate or are closing the gate. in either case we must be allowed to
            // enter.  Since our local enter count was not > 1 it must just be 1, and thus we need to increment the
            // active count.
            if ((atomicState.get() & ACTIVE_COUNT_MASK) == ACTIVE_COUNT_MASK)
                {
                // the gate has been entered more times then we can track, i.e. 2^60
                decrementEnterCount(oContender);
                throw new IllegalStateException("The ThreadGate is full.");
                }

            // We don't need to worry about concurrent overflow since no others can increment now, but they
            // can still concurrently decrement and thus we must use the atomic increment operation rather
            // then just a blind set call.
            atomicState.incrementAndGet();
            return true;
            }

        boolean fSuccess = false;
        try
            {
            while (true)
                {
                long lStatus = atomicState.get();
                switch ((int) (lStatus >>> STATUS_OFFSET))
                    {
                    case GATE_OPEN:
                        if ((lStatus & ACTIVE_COUNT_MASK) == ACTIVE_COUNT_MASK)
                            {
                            // the gate has been entered more times then we can track, i.e. 2^60
                            throw new IllegalStateException("The ThreadGate is full.");
                            }
                        else if (atomicState.compareAndSet(lStatus, lStatus + 1))
                            {
                            // atomic set succeeded confirming that the gate
                            // remained open and that we made it in
                            return fSuccess = true;
                            }
                        // we failed to atomically enter an open gate, which
                        // can happen if either the gate closed just as we entered
                        // or if another thread entered at the same time
                        break; // retry

                    case GATE_CLOSING:
                    case GATE_CLOSED:
                        // we know that we were not already in the gate, and are
                        // not the one closing the gate; wait for it to open
                        lock();
                        try
                            {
                            long nStatus = getStatus();
                            if (nStatus == GATE_CLOSING || nStatus == GATE_CLOSED)
                                {
                                // wait for the gate to open
                                cMillis = doWait(cMillis);
                                if (cMillis == 0L)
                                    {
                                    return false;
                                    }
                                }
                            }
                        finally
                            {
                            unlock();
                            }
                        break; // retry

                    case GATE_DESTROYED:
                        throw new IllegalStateException("ThreadGate.enter: ThreadGate has been destroyed.");

                    default:
                        throw new IllegalStateException("ThreadGate.enter: ThreadGate has an invalid status. " + this);
                    }
                }
            }
        finally
            {
            if (!fSuccess)
                {
                decrementEnterCount(oContender);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public void exit()
        {
        exitInternal(Thread.currentThread());
        }

    /**
     * Exit the gate, using the specified (potentially non-thread based) lock contender.
     *
     * @param oContender  the lock contender
     */
    public void exit(Object oContender)
        {
        synchronized (oContender)
            {
            exitInternal(oContender);
            }
        }

    /**
     * Internal version of exit.
     *
     * @param oContender  the lock contender
     */
    protected void exitInternal(Object oContender)
        {
        long cEnterThis = decrementEnterCount(oContender);
        if (cEnterThis == 0)
            {
            // we've fully exited
            if (f_atomicState.decrementAndGet() == EMPTY_GATE_CLOSING)
                {
                // we were the last to exit, and the gate is in the CLOSING state
                // notify everyone, to ensure that we notify the closing thread
                lock();
                try
                    {
                    f_open.signalAll();
                    }
                finally
                    {
                    unlock();
                    }
                }
            }
        else if (cEnterThis < 0)
            {
            // Note: decrementEnterCount will not store a value less then 0, so we don't need to do a correction
            throw new IllegalMonitorStateException("ThreadGate.exit: (" + oContender + ") has already exited! " + this);
            }
        }


   /**
    * {@inheritDoc}
    */
    @Override
    public void open()
        {
        openInternal(Thread.currentThread());
        }

    /**
     * Open the gate, using the specified (potentially non-thread based) lock contender.
     *
     * @param oContender  the lock contender
     */
    public void open(Object oContender)
        {
        synchronized (oContender)
            {
            openInternal(oContender);
            }
        }

    /**
     * Internal version of open.
     *
     * @param oContender  the lock contender
     */
    protected void openInternal(Object oContender)
        {
        if (oContender == getCloser())
            {
            int cClosed = getCloseCount() - 1;
            if (cClosed >= 0)
                {
                setCloseCount(cClosed);
                if (cClosed == 0)
                    {
                    // we've opened the gate
                    lock();
                    try
                        {
                        updateStatus(GATE_OPEN);
                        setCloser(null);
                        f_open.signalAll();
                        }
                    finally
                        {
                        unlock();
                        }
                    }
                return;
                }
            }

        throw new IllegalMonitorStateException(
                "ThreadGate.open: Gate was not closed by " + oContender + ";"  + this);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean isClosedByCurrentThread()
        {
        return isClosedByInternal(Thread.currentThread());
        }

    /**
    * Return true if the gate is closed by the specified contender.
    *
    * @param oContender  the contender
    *
    * @return true if the gate is closed by the specified contender
    */
    public boolean isClosedBy(Object oContender)
        {
        synchronized (oContender)
            {
            return isClosedByInternal(oContender);
            }
        }

    /**
    * Return true if the gate is closed by the specified contender.
    *
    * @param oContender  the contender
    *
    * @return true if the gate is closed by the specified contender.
    */
    protected boolean isClosedByInternal(Object oContender)
        {
        return oContender == getCloser() && getStatus() == GATE_CLOSED;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean isEnteredByCurrentThread()
        {
        return isEnteredByInternal(Thread.currentThread());
        }

    /**
    * Determines if the specified non-thread contender has entered the gate and not yet exited.
    *
    *
    * @param oContender  the contender
    *
    * @return true if the specified contender has entered the gate
    */
    public boolean isEnteredBy(Object oContender)
        {
        synchronized (oContender)
            {
            return isClosedByInternal(oContender);
            }
        }

    /**
    * Determines if the specified non-thread contender has entered the gate and not yet exited.
    *
    * @param oContender  the contender
    *
    * @return true if the specified contender has entered the gate
    */
    protected boolean isEnteredByInternal(Object oContender)
        {
        return getEnterCount(oContender) > 0;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean isClosed()
        {
        return getStatus() == GATE_CLOSED;
        }

    // ----- internal helpers -----------------------------------------------

    /**
     * Lock this gate.
     */
    protected void lock()
        {
        f_lock.lock();
        }

    /**
     * Unlock this gate.
     */
    protected void unlock()
        {
        f_lock.unlock();
        }

    /**
    * Wait up to the specified number of milliseconds for notification.
    *
    * @param cMillis  the wait time
    *
    * @return the remaining wait time in milliseconds
    */
    protected long doWait(long cMillis)
        {
        if (cMillis == 0)
            {
            return 0;
            }

        long lTime = SafeClock.INSTANCE.getSafeTimeMillis();
        try
            {
            f_open.await(Math.max(0, cMillis), TimeUnit.MILLISECONDS);
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            onInterruptedException(e);
            }

        return cMillis < 0 ? cMillis :
            Math.max(0, cMillis - (SafeClock.INSTANCE.getSafeTimeMillis() - lTime));
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the number of entered contenders.
    *
    * @return the number of entered contenders.
    */
    public int getActiveCount()
        {
        return (int) (f_atomicState.get() & ACTIVE_COUNT_MASK);
        }

    /**
    * Return the number of unmatched completed close/barEntry calls.
    *
    * @return the number of unmatched completed close/barEntry calls.
    */
    public int getCloseCount()
        {
        return m_cClose;
        }

    /**
    * Specify the number of unmatched completed close/barEntry calls.
    *
    * The caller must have the gate closed/closing.
    *
    * @param cClose the close count
    */
    protected void setCloseCount(int cClose)
        {
        m_cClose = cClose;
        }

    /**
    * Return the thread that is closing the gates.
    *
    * @return the thread that is closing the gates.
    */
    protected Object getCloser()
        {
        return m_closer;
        }

    /**
    * Specify the thread that is closing the gates.
    *
    * The caller must hold the lock on the ThreadGate itself.
    *
    * @param oCloser  the closer
    */
    protected void setCloser(Object oCloser)
        {
        m_closer = oCloser;
        }

    /**
    * Return the current thread gate status.
    *
    * @return the current thread gate status.
    */
    public int getStatus()
        {
        return (int) (f_atomicState.get() >>> STATUS_OFFSET);
        }

    /**
    * Update the current thread gate status, without changing the active count.
    *
    * The caller must hold synchronization on the ThreadGate.
    *
    * @param nStatus the new status
    *
    * @return the old status
    */
    protected int updateStatus(int nStatus)
        {
        AtomicLong    atomicState = f_atomicState;
        long          lStatus     = ((long) nStatus) << STATUS_OFFSET;
        while (true)
            {
            long lCurr = atomicState.get();
            long lNew  = lStatus | (lCurr & ACTIVE_COUNT_MASK);
            if (atomicState.compareAndSet(lCurr, lNew))
                {
                return (int) (lCurr >>> STATUS_OFFSET);
                }
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Provide a human-readable representation of this ThreadGate.
    */
    @Override
    public String toString()
        {
        lock();
        try
            {
            String sState;
            switch (getStatus())
                {
                case GATE_OPEN:
                    sState = "GATE_OPEN";
                    break;
                case GATE_CLOSING:
                    sState = "GATE_CLOSING";
                    break;
                case GATE_CLOSED:
                    sState = "GATE_CLOSED";
                    break;
                case GATE_DESTROYED:
                    sState = "GATE_DESTROYED";
                    break;
                default:
                    sState = "INVALID";
                    break;
                }

            return "ThreadGate{State=" + sState
                   + ", ActiveCount=" + getActiveCount()
                   + ", CloseCount=" + getCloseCount()
                   + ", Closer= " + getCloser()
                   + '}';
            }
        finally
            {
            unlock();
            }
        }

    /**
     * Handle an InterruptedException
     *
     * @param e  the exception
     */
    protected void onInterruptedException(InterruptedException e)
        {
        throw new RuntimeException(toString(), e);
        }

    /**
     * Helper method for creating a MutableLong for the EnterMap if one isn't already present.
     *
     * @param oHolder  the contender holder
     *
     * @return the counter
     */
    protected static MutableLong makeCounterFromHolder(IdentityHolder<Object> oHolder)
        {
        return makeCounter(oHolder.get());
        }

    /**
     * Helper method for creating a MutableLong for the EnterMap if one isn't already present.
     *
     * @param oContender  the contender
     *
     * @return the counter
     */
    protected static MutableLong makeCounter(Object oContender)
        {
        if (oContender instanceof Thread)
            {
            // to be here we know that oContender is not the calling thread, we don't support this since
            // we use TLO to maintain the thread based contender count
            throw new IllegalArgumentException("thread based contenders may only be the current thread");
            }

        return new MutableLong();
        }

    /**
     * Return the enter count for the specified contender
     *
     * @param oContender  the contender
     *
     * @return the count
     */
    protected long getEnterCount(Object oContender)
        {
        return oContender == Thread.currentThread()
                ? adjustThreadLocalEnters(0) // most common case
                : oContender instanceof LiteContender
                    ? ((LiteContender) oContender).getCount(this)
                    : oContender == f_atomicState // marker from non-reentrant path
                        ? 0
                        : ensureEnterCountMap().getOrDefault(new IdentityHolder<>(oContender),
                            ThreadGate.makeCounter(oContender)).get();
        }

    /**
     * Increment and return the enter count for the specified contender
     *
     * @param oContender  the contender
     *
     * @return the new count
     */
    protected long incrementEnterCount(Object oContender)
        {
        return oContender == Thread.currentThread()
                ? adjustThreadLocalEnters(1) // most common case
                : oContender instanceof LiteContender
                    ? ((LiteContender) oContender).incrementAndGet(this)
                    : oContender == f_atomicState // marker from non-reentrant path
                        ? 0
                        : ensureEnterCountMap().computeIfAbsent(new IdentityHolder<>(oContender),
                          ThreadGate::makeCounterFromHolder).incrementAndGet();
        }

    /**
     * Decrement and return the enter count for the specified contender
     *
     * @param oContender  the contender
     *
     * @return the new count
     */
    protected long decrementEnterCount(Object oContender)
        {
        return oContender == Thread.currentThread()
                ? adjustThreadLocalEnters(-1) // most common case
                : oContender instanceof LiteContender
                    ? ((LiteContender) oContender).decrementAndGet(this)
                    : oContender == f_atomicState // marker from non-reentrant path
                        ? 0
                        : decrementEnterCountComplex(oContender);
        }

    /**
     * Complex (non-common case) implementation of contender decrement.
     *
     * @param oContender the contender
     *
     * @return the new count
     */
    protected long decrementEnterCountComplex(Object oContender)
        {
        ConcurrentHashMap<IdentityHolder<Object>, MutableLong> map     = ensureEnterCountMap();
        IdentityHolder<Object>                                 oHolder = new IdentityHolder<>(oContender);
        MutableLong                                            cEnter  = map.get(oHolder);

        if (cEnter == null)
            {
            if (oContender instanceof Thread)
                {
                // the specified thread may be entered, but we don't support exiting from another thread
                throw new IllegalArgumentException("thread-based contenders may only be the current thread");
                }
            return -1L;
            }

        long c = cEnter.decrementAndGet();
        if (c <= 0)
            {
            // Avoid leaving garbage counters around, we may never hear from this oContender again
            // ThreadGate prevents concurrent access for the same oContender, so we don't have to worry
            // about a concurrent increment for the same contender.
            map.remove(oHolder, cEnter);
            }

        return c;
        }

    /**
     * Return the map which tracks the enter count for non-thread based contenders.
     *
     * @return the map
     */
    protected ConcurrentHashMap<IdentityHolder<Object>, MutableLong> ensureEnterCountMap()
        {
        ConcurrentHashMap<IdentityHolder<Object>, MutableLong> map = m_mapContenderEnters;
        if (map == null)
            {
            lock();
            try
                {
                map = m_mapContenderEnters;
                if (map == null)
                    {
                    map = m_mapContenderEnters = new ConcurrentHashMap<>();
                    }
                }
            finally
                {
                unlock();
                }
            }

        return map;
        }

    /**
     * Increment the thread-local enter count.
     *
     * @param c the increment amount
     *
     * @return the new enter count
     */
    protected int adjustThreadLocalEnters(int c)
        {
        int[] ai = f_tlcEnters.get();
        if (ai == null)
            {
            ai = new int[1];
            f_tlcEnters.set(ai);
            }

        int i = ai[0] + c;
        if (i >= 0)
            {
            ai[0] = i;
            }

        return i;
        }

    // ----- inner class: LiteContender -------------------------------------

    /**
     * An optimized implementation of a object to use for non-thread-based gate contenders.
     * <p>
     * Using an object of any other type as the contender is allowable but may produce considerably more
     * garbage then if a LiteContender is used.
     *
     * @author mf  2017.03.21
     */
    public static class LiteContender
        extends InflatableMap<ThreadGate, MutableLong> // extend only to keep the total number of allocations to a minimum
        {
        /**
         * Increment the contender's count for the specified gate.
         *
         * @param gate  the gate
         *
         * @return the new count
         */
        protected long incrementAndGet(ThreadGate gate)
            {
            return gate == m_gatePrimary
                ? ++m_cEntersPrimary // common path
                : incrementComplex(gate);
            }

        /**
         * Increment the contender's count for the specified non-primary gate.
         *
         * @param gate  the gate
         *
         * @return the new count
         */
        protected long incrementComplex(ThreadGate gate)
            {
            MutableLong cEnters = get(gate);
            if (cEnters == null)
                {
                if (m_gatePrimary == null)
                    {
                    m_gatePrimary = gate;
                    return m_cEntersPrimary = 1;
                    }

                put(gate, cEnters = new MutableLong());
                }

            return cEnters.incrementAndGet();
            }

        /**
         * Decrement the contender's count for the specified gate.
         *
         * @param gate  the gate
         *
         * @return the new count
         */
        protected long decrementAndGet(ThreadGate gate)
            {
            if (gate == m_gatePrimary)
                {
                long cEnters = --m_cEntersPrimary;
                if (cEnters <= 0)
                    {
                    m_gatePrimary    = null;
                    m_cEntersPrimary = 0;
                    }

                return cEnters; // common path
                }

            MutableLong cEnters = get(gate);
            return cEnters == null ? -1 : cEnters.decrementAndGet();
            }

        /**
         * Return the contender's count for the specified gate.
         *
         * @param gate  the gate
         *
         * @return the current count
         */
        protected long getCount(ThreadGate gate)
            {
            if (gate == m_gatePrimary)
                {
                return m_cEntersPrimary; // common path
                }

            MutableLong cEnters = get(gate);
            return cEnters == null ? 0 : cEnters.get();
            }

        /**
         * The primary (usually first) gate this LiteContender is used with.  This avoids the creating of the underlying
         * LiteMap.Entry and MutableLong on the common path, resulting LiteContender being the only object created.
         */
        private ThreadGate m_gatePrimary;

        /**
         * The enter count for the primary gate.
         */
        private long m_cEntersPrimary;
        }


    // ----- inner class: NonReentrant --------------------------------------

    /**
     * A non-reentrant version of a ThreadGate.  The non-reentrant version does not support lock
     * promotion, but is much faster then the reentrant version, especially when the contenders are
     * not LiteContenders (including Threads).
     * @param <R>  the resource type
     */
    public static class NonReentrant<R>
        extends ThreadGate<R>
        {
        // performance tests against the reentrant gate have shown it to be about 3x faster than the reentrant
        // version; and about 30% faster than locking/unlocking the read lock of Java's ReentrantReadWriteLock.

        /**
         * Construct a NonReentrant gate.
         */
        public NonReentrant()
            {
            this(null);
            }

        /**
         * Construct a NonReentrant gate protecting the specified resource.
         *
         * @param resource  the resource
         */
        public NonReentrant(R resource)
            {
            super(resource);
            }

        @Override
        public boolean enter(long cMillis)
            {
            // optimized common-path for non-reentrant enter; i.e. open gate which hasn't become full.
            AtomicLong atomicState = f_atomicState;
            for (long lStatus = atomicState.get(); lStatus < FULL_GATE_OPEN; lStatus = atomicState.get())
                {
                if (atomicState.compareAndSet(lStatus, lStatus + 1L))
                    {
                    // atomic set succeeded confirming that the gate
                    // remained open and that we made it in
                    return true;
                    }
                }
            // otherwise; fall through

            return enterInternal(f_atomicState /*marker*/, cMillis);
            }

        @Override
        public boolean enter(Object oContender, long cMillis)
            {
            return enter(cMillis); // contender doesn't matter for non-reentrant
            }

        @Override
        public void exit()
            {
            exitInternal(f_atomicState);
            }

        @Override
        public void exit(Object oContender)
            {
            exitInternal(f_atomicState); // contender doesn't matter for non-reentrant
            }

        @Override
        protected void exitInternal(Object oContender)
            {
            // unlike re-entrant case we need to do extra checks to ensure we don't corrupt the state
            long lStatus = f_atomicState.decrementAndGet();
            if (lStatus == EMPTY_GATE_CLOSING)
                {
                // we were the last to exit, and the gate is in the CLOSING state
                // notify everyone, to ensure that we notify the closing thread
                lock();
                try
                    {
                    f_open.signalAll();
                    }
                finally
                    {
                    unlock();
                    }
                }
            else if (lStatus < 0)
                {
                throw new IllegalStateException();
                }
            // else; common path
            }
        }

    // ----- constants ------------------------------------------------------

    /**
    * GATE_OPEN: Threads may enter and exit the gates.
    */
    public static final int GATE_OPEN = 0;

    /**
    * GATE_CLOSING: A thread is waiting to be the only thread inside the
    * gates; other threads can only exit.
    */
    public static final int GATE_CLOSING = 1;

    /**
    * GATE_CLOSED: A single thread is inside the gates; other threads cannot
    * enter.
    */
    public static final int GATE_CLOSED = 2;

    /**
    * GATE_DESTROYED: Life-cycle is complete; the object is no longer usable.
    */
    public static final int GATE_DESTROYED = 3;

    /**
    * The bit offset at which the GATE_* status is stored within f_atomicState.
    */
    private static final int  STATUS_OFFSET = 60;

    /**
    * The bit mask covering the portion of f_atomicState used to store the
    * number contenders currently entered.
    */
    private static final long ACTIVE_COUNT_MASK = -1L >>> (64 - STATUS_OFFSET);

    /**
    * EMPTY_GATE_OPEN: Threads may enter, exit, or close the gates.
    */
    private static final long EMPTY_GATE_OPEN = ((long) GATE_OPEN << STATUS_OFFSET);

    /**
     * FULL_GATE_OPEN indicates a state at which no more enters are allowable.
     */
    private static final long FULL_GATE_OPEN = EMPTY_GATE_OPEN | ACTIVE_COUNT_MASK;

    /**
    * EMPTY_GATE_CLOSING: Closing thread may close the gates, all entered threads
    * have exited.
    */
    private static final long EMPTY_GATE_CLOSING = ((long) GATE_CLOSING << STATUS_OFFSET);

    /**
    * EMPTY_GATE_CLOSED: Gates are closed, with no threads inside.
    */
    private static final long EMPTY_GATE_CLOSED = ((long) GATE_CLOSED << STATUS_OFFSET);


    // ----- data members ---------------------------------------------------

    /**
     * Internal lock used for synchronized state access.
     */
    private final Lock f_lock = new ReentrantLock();

    /**
     * A condition associated with a synchronization lock above that is used
     * to wait for notification and to notify waiting threads.
     */
    protected final Condition f_open = f_lock.newCondition();

    /**
     * The protected resource.
     */
    private final R f_resource;

    /**
    * The state of the ThreadGate, including:
    * <pre>
    * bits  0 - 59 store the number of entered contenders
    * bits 60 - 61 store the state GATE_* value
    * bit  62 - 63 always zero
    * </pre>
    */
    protected final AtomicLong f_atomicState = new AtomicLong();

    /**
    * Number of unmatched completed close/barEntry calls.
    */
    private int m_cClose;

    /**
    * The closer (usually a thread) that is closing the gates.
    */
    private volatile transient Object m_closer;

    /**
    * Count of how many unmatched enter calls per thread.
    *
    * This rather odd ThreadLocal allows us to avoid having any non JDK class stored in the ThreadLocal and as
    * such allows our Class to be collected when needed.  We never remove the ThreadLocal, and allow it to be
    * cleaned up by the ThreadLocal machinery itself, i.e. either the Thread terminates or the ThreadLocalMap
    * which is key'd by weak reference will clean up this entry when space is needed. This is all done to avoid
    * the need to do a tlc.remove() each time the count reaches zero, as that more the doubles the cost of
    * entering the gate.
    */
    private final ThreadLocal<int[]> f_tlcEnters = new ThreadLocal<>();

    /**
     * When using not using thread-based or LiteContender contenders, this map will be created and
     * will hold the non-zero counts for only those contenders.  In most cases this map will never be created.
     *
     * The map is key'd by IdentityHolders to ensure that key equality is based on identity equality of the
     * contender rather then state equality.
     */
    private volatile ConcurrentHashMap<IdentityHolder<Object>, MutableLong> m_mapContenderEnters;

    /**
     * Sentry to return from {@link #enter} that will {@link #exit} when the sentry is closed.
     */
    protected final Sentry<R> f_exitSentry = new Sentry<R>()
        {
        @Override
        public R getResource()
            {
            return f_resource;
            }

        @Override
        public void close()
            {
            exit();
            }
        };

    /**
     * Sentry to return from {@link #close} that will {@link #open} when the sentry is closed.
     */
    protected final Sentry<R> f_openSentry = new Sentry<R>()
        {
        @Override
        public R getResource()
            {
            return f_resource;
            }

        @Override
        public void close()
            {
            open();
            }
        };
    }
