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
 * A Condition-like object, usable by multiple threads to both wait and signal.
 * <p>
 * Note that no synchronization is needed to use this class; i.e. clients
 * must not synchronize on this class prior to calling <tt>await()</tt> or
 * <tt>signal()</tt>, nor should the use any of the primitive <tt>wait()</tt>
 * or <tt>notify()</tt> methods.
 * <p>
 * Unlike the {@link SingleWaiterMultiNotifier}, this notifier implementation requires
 * the notion of a {@link #isReady ready} check.  When the notifier is ready then a call
 * to await because a no-op.  An example ready check for a notifier based queue removal
 * would be <tt>!queue.isEmpty();</tt>
 *
 * @author mf  2018.04.13
 */
public abstract class ConcurrentNotifier
        implements Notifier
    {
    @Override
    public void await(long cMillis)
            throws InterruptedException
        {
        Thread threadThis = Thread.currentThread();
        Link   linkThis   = null;
        long   lBitThis   = -1L; // will be reset when we make our link

        while (!isReady())
            {
            Object oHead = m_oWaitHead;
            Object oNew;
            if (oHead == null)
                {
                // try to be the initial waiting thread; avoid creating a Link
                oNew = threadThis;
                }
            else if (linkThis == null)
                {
                // other threads are waiting; create a Link for myself
                lBitThis = 1L << (threadThis.hashCode() % 61);
                oNew     = linkThis = makeSelfLink(threadThis, lBitThis, oHead);
                // if linkThis is non-null linkThis.next has also been set to oHead
                // if null we are already in the stack and can avoid cas'ing and just park (see below)
                }
            else
                {
                // other threads are waiting; we've already created a Link for ourselves; just assign .next/lFilterThreads
                // no-need to recheck if we're in the stack
                Link linkNext = linkThis.next = oHead instanceof Link
                        ? (Link) oHead
                        : new Link((Thread) oHead);
                linkThis.lFilterThreads = lBitThis | linkNext.lFilterThreads;
                oNew = linkThis;
                }

            if (oNew == null || s_fuHead.compareAndSet(this, oHead, oNew))
                {
                park(cMillis);
                return;
                }
            // else; retry
            }
        }

    @Override
    public void signal()
        {
        Object oWaitHead = m_oWaitHead;
        if (oWaitHead == null)
            {
            // nobody waiting; nothing to do
            }
        else if (oWaitHead instanceof Thread && s_fuHead.compareAndSet(this, oWaitHead, null))
            {
            // common case, just one thread waiting, and we win the CAS on first attempt
            LockSupport.unpark((Thread) oWaitHead);
            }
        else
            {
            // take the slow path
            signalInternal();
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Full version of signal.
     */
    protected void signalInternal()
        {
        for (Object oWaitHead = m_oWaitHead; oWaitHead != null; oWaitHead = m_oWaitHead)
            {
            if (s_fuHead.compareAndSet(this, oWaitHead, null))
                {
                // we've signaled with waiting thread(s)
                if (oWaitHead instanceof Link)
                    {
                    for (Link link = (Link) oWaitHead; link != null; )
                        {
                        LockSupport.unpark(link.thread);

                        Link linkLast = link;

                        link          = link.next;
                        linkLast.next = null; // helps avoid pulling new stuff into old-gen
                        }
                    }
                else // single waiting thread
                    {
                    LockSupport.unpark((Thread) oWaitHead);
                    }

                // we've unparked all threads we are responsible for; if another cas'd in after our cas
                // they will rely on {@link #isReady} or appropriately wait for the next call to signal
                return;
                }
            }
        }


    /**
     * Block the calling thread if the notifier is not ready.
     *
     * @param cMillis  the time to block for
     *
     * @throws InterruptedException  if the calling thread is interrupted
     */
    protected void park(long cMillis)
            throws InterruptedException
        {
        if (!isReady())
            {
            if (cMillis == 0)
                {
                Blocking.park(/*blocker*/ this);
                }
            else
                {
                Blocking.parkNanos(/*blocker*/ this, cMillis * 1000000);
                }
            }

        if (m_oWaitHead != null && Blocking.interrupted()) // only pay the cost of interrupt check if we may not have been signaled
            {
            throw new InterruptedException();
            }
        }

    /**
     * Make a link for the calling thread, checking if one already exists for this notifier.
     *
     * @param threadThis  the calling thread
     * @param lBitThis    this thread's contribution to the bloom filter
     * @param oHead       the current head
     *
     * @return this thread's link, or null if we should not block
     */
    protected Link makeSelfLink(Thread threadThis, long lBitThis, Object oHead)
        {
        // It's possible this thread is already in the chain, but this can only happen if we had a induced
        // wakeup (ready-check, timeout, spurious) while waiting on this notifier previously.
        // We could try to move this check to after we wakeup, but this will be more harder
        // to do, and we're about to block anyway so we might as well do it now.  We make use of a bloom
        // filter to avoid scanning any deeper into the chain then is absolutely necessary, which in many
        // cases may eliminate the scan entirely.

        Link linkHead;
        if (oHead == threadThis)
            {
            return null; // we're already there
            }
        else if (oHead instanceof Link)
            {
            linkHead = (Link) oHead;
            for (Link link = linkHead; link != null && (link.lFilterThreads & lBitThis) != 0L; link = link.next)
                {
                if (link.thread == threadThis)
                    {
                    return null; // we're already there
                    }
                }
            }
        else
            {
            linkHead = new Link((Thread) oHead);
            linkHead.lFilterThreads = 1L << (oHead.hashCode() % 61);
            }

        Link linkThis = new Link(threadThis);

        linkThis.next           = linkHead;
        linkThis.lFilterThreads = lBitThis | linkHead.lFilterThreads;
        return linkThis;
        }

    /**
     * Return true if the notifier is ready, i.e. threads entering await cant return without blocking.
     *
     * @return true if the notifier is ready
     */
    abstract protected boolean isReady();


    // ----- inner class: Link ----------------------------------------------

    /**
     * A link in a stack of waiting threads.
     */
    protected static final class Link
        {
        /**
         * Construct a new Link for a given thread.
         *
         * @param thread  the thread
         */
        Link(Thread thread)
            {
            this.thread = thread;
            }

        /**
         * This waiting thread.
         */
        final Thread thread;

        /**
         * A bloom filter of the waiting threads.
         */
        long lFilterThreads;

        /**
         * The next waiting thread.
         */
        Link next;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The head of a stack of waiting threads.  The head can be either a Thread, or a Link.
    */
    protected volatile Object m_oWaitHead;

    /**
    * The atomic field updater for {@link #m_oWaitHead}.
    */
    private static final AtomicReferenceFieldUpdater<ConcurrentNotifier, Object> s_fuHead =
            AtomicReferenceFieldUpdater.newUpdater(ConcurrentNotifier.class, Object.class, "m_oWaitHead");
    }
