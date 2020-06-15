/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.util.Sentry;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicReference;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
* ThreadGateLite is a Gate implementation built around the {@link
* ReentrantReadWriteLock}.
*
* @author coh 2010.08.09
*
* @since  Coherence 3.7
*/
public final class ThreadGateLite<R>
        implements Gate<R>
    {
    /**
     * Default constructor.
     */
    public ThreadGateLite()
        {
        this(null);
        }

    /**
     * Construct a ThreadGateLite protected the specified resource.
     *
     * @param resource  the resource
     */
    public ThreadGateLite(R resource)
        {
        f_resource = resource;
        }

    // ---- Gate interface --------------------------------------------------


    @Override
    public Sentry<R> close()
        {
        close(-1L);
        return f_openSentry;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean close(long cMillis)
        {
        do
            {
            Bar bar = f_atomicBar.get();
            if (bar == null || bar.f_thread == Thread.currentThread())
                {
                long ldtStart = Base.getSafeTimeMillis();
                if (acquireLock(f_rwLock.writeLock(), cMillis))
                    {
                    if (f_atomicBar.get() == bar)
                        {
                        return true;
                        }
                    else
                        {
                        // some thread concurrently barred entry; release the write lock
                        f_rwLock.writeLock().unlock();

                        // account for time spent so far
                        cMillis = adjustWaitTime(cMillis, ldtStart);
                        }
                    }
                else
                    {
                    // we didn't manage to lock in the required time
                    return false;
                    }
                }
            else
                {
                // wait for the bar to be lifted and try again
                cMillis = waitForOpen(bar, cMillis);
                }
            }
        while (cMillis != 0); // keep going if cMillis is positive or -1
        return false;
        }

    @Override
    public Sentry<R> enter()
        {
        enter(-1);
        return f_exitSentry;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean enter(long cMillis)
        {
        do
            {
            Bar bar = f_atomicBar.get();
            if (bar == null || bar.f_thread == Thread.currentThread()
                    || f_rwLock.getReadHoldCount() > 0)
                {
                // Note: there may be a concurrent barEnter, but we are
                // allowed to complete the attempt to acquire the read lock
                return acquireLock(f_rwLock.readLock(), cMillis);
                }
            else
                {
                // entry is barred by other thread and we are not already in the gate
                cMillis = waitForOpen(bar, cMillis);
                }
            }
        while (cMillis != 0); // keep going if cMillis is positive or -1

        return false;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public void exit()
        {
        f_rwLock.readLock().unlock();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean isClosedByCurrentThread()
        {
        return f_rwLock.isWriteLockedByCurrentThread();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean isClosed()
        {
        return f_rwLock.isWriteLocked();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean isEnteredByCurrentThread()
        {
        return f_rwLock.getReadHoldCount() > 0;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public void open()
        {
        Bar bar = f_atomicBar.get();

        // first match all barEntry calls, then close calls
        if (bar == null)
            {
            f_rwLock.writeLock().unlock();
            }
        else if (bar.f_thread != Thread.currentThread())
            {
            // the bar was set by another thread - can't open it;
            // can't open a close either, because for barEntry
            // and close to be in effect at the same time, they must
            // be on the same thread; thus, this open is illegal
            throw new IllegalMonitorStateException(
                    "Gate was not closed by this thread");
            }
        else if (--bar.m_cBarred == 0)
            {
            synchronized (bar)
                {
                f_atomicBar.set(null);
                bar.notifyAll();
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean barEntry(long cMillis)
        {
        do
            {
            Bar bar = f_atomicBar.get();
            if (bar == null)
                {
                long ldtStart = Base.getSafeTimeMillis();
                if (enter(cMillis))
                    {
                    try
                        {
                        if (f_atomicBar.compareAndSet(null, new Bar(Thread.currentThread())))
                            {
                            return true;
                            }

                        cMillis = adjustWaitTime(cMillis, ldtStart);
                        }
                    finally
                        {
                        exit();
                        }
                    }
                else
                    {
                    // time is up and we didn't succeed
                    return false;
                    }
                }
            else if (bar.f_thread == Thread.currentThread())
                {
                ++bar.m_cBarred;
                return true;
                }
            else
                {
                cMillis = waitForOpen(bar, cMillis);
                }
            }
        while (cMillis != 0); // keep going if cMillis is positive or -1

        return false;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public String toString()
        {
        return "ThreadGateLite{lock=" + f_rwLock.toString()
                + ", bar=" + f_atomicBar.get() + "}";
        }

    // ---- helper methods --------------------------------------------------

    /**
    * Try to acquire the lock within the supplied time interval.
    *
    * @param lock    the lock that should be acquired
    * @param cMillis for how long to attempt to acquire the lock,
    *                pass -1 to wait indefinitely; 0 to return immediately
    *
    * @return true if the lock was acquired, false if not
    */
    private boolean acquireLock(Lock lock, long cMillis)
         {
         try
             {
             if (cMillis < 0)
                 {
                 lock.lock();
                 return true;
                 }
             else if (cMillis == 0)
                 {
                 return lock.tryLock();
                 }
             else
                 {
                 return Blocking.tryLock(lock, cMillis, TimeUnit.MILLISECONDS);
                 }
             }
         catch (InterruptedException e)
             {
             Thread.currentThread().interrupt();
             return false;
             }
         }

    /**
    * Wait for notification that the bar has been lifted
    * completely, i.e. all re-entrant barEntry calls were matched.
    * See {@link #open}.
    *
    * @param bar      the bar that needs to be lifted for this thread
    *                 to proceed (cannot be null)
    * @param cMillis  time to wait or -1 for unlimited wait
    *
    * @return the remaining wait time, or 0 if the wait time has expired
    */
    protected long waitForOpen(Bar bar, long cMillis)
        {
        // Note: re-checking the current bar is necessary here to protect
        //       against a concurrent call to open() and missed notification
        synchronized (bar)
            {
            if (bar == f_atomicBar.get())
                {
                if (!bar.f_thread.isAlive())
                    {
                    f_atomicBar.set(null);
                    bar.notifyAll();
                    }
                else if (cMillis != 0)
                    {
                    long ldtStart = Base.getSafeTimeMillis();
                    Base.wait(bar, cMillis < 0 ? 0 : cMillis);
                    cMillis = adjustWaitTime(cMillis, ldtStart);
                    }
                }
            }

        return cMillis;
        }

    /**
    * Calculate the time remaining from the total time allotted for an operation.
    *
    * @param cMillis   the total time allotted for an operation
    * @param ldtStart  the start of the time interval that have passed
    *
    * @return the remaining wait time in milliseconds. The value may be positive,
    *         zero for no time left or -1 for indefinite wait.
    */
    protected long adjustWaitTime(long cMillis, long ldtStart)
        {
        if (cMillis > 0)
            {
            cMillis = Math.max(0, cMillis - (Base.getSafeTimeMillis() - ldtStart));
            }
        return cMillis;
        }


    // ---- inner classes ---------------------------------------------------

    /**
    * Bar represents the state of the {@link #barEntry bars} placed on this gate.
    */
    protected static class Bar
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a Bar for the specified thread.
         *
         * @param thread  the thread to construct a bar for
         */
        protected Bar(Thread thread)
            {
            f_thread  = thread;
            m_cBarred = 1;
            }

        // ----- Object methods ---------------------------------------------

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return "Bar{m_thread=" + f_thread + "; m_cBarred=" +  m_cBarred + "}";
            }

        // ----- data members -----------------------------------------------

        // the thread that is barring entry
        protected final Thread f_thread;

        // the count of successful reentrant barEntry calls
        protected int m_cBarred;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The protected resource, or null
     */
    private final R f_resource;

    /**
    * The lock used to control the state of this ThreadGateLite instance.
    */
    private final ReentrantReadWriteLock f_rwLock = new ReentrantReadWriteLock();

    /**
    * The bar used in barEntry operation.
    */
    private final AtomicReference<Bar> f_atomicBar = new AtomicReference<>();

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
