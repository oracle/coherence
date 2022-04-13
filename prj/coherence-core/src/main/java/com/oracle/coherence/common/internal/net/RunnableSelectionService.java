/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.NonBlocking;
import com.oracle.coherence.common.collections.ConcurrentLinkedQueue;
import com.oracle.coherence.common.net.SelectionService;
import com.oracle.coherence.common.util.Timers;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


/**
 * RunnableSelectionService is a single-threaded SelectionService implementation.
 * <p>
 * RunnableSelectionService is Runnable but to be functional needs to be
 * serviced by a thread.  The {@link #run} method is not thread-safe, and
 * should not be invoked by more then one thread.
 * <p>
 * The service will create its Selector as part of the first registration,
 * which will ultimately dictate the family of SelectableChannels which
 * can be used with the service.
 *
 * @author mf  2010.10.29
 */
public class RunnableSelectionService
        implements SelectionService, Runnable
    {
    // ----- SelectionService interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized void register(SelectableChannel chan, Handler handler)
            throws IOException
        {
        if (chan.isBlocking())
            {
            throw new IllegalBlockingModeException();
            }

        ensureSelector(chan);

        // add registration
        f_mapRegistrations.put(chan, handler);

        // ensure selector thread sees the new registration
        if (!m_fPendingRegistrations)
            {
            m_fPendingRegistrations = true;
            wakeup();
            }
        }

    /**
     * {@inheritDoc}
     */
    public void invoke(final SelectableChannel chan, final Runnable runnable, long cMillis)
        throws IOException
        {
        ensureSelector(chan);

        if (cMillis == 0)
            {
            boolean fEmpty = f_tasks.isEmpty();
            f_tasks.add(runnable);
            if (fEmpty)
                {
                wakeup();
                }
            }
        else
            {
            Timers.scheduleNonBlockingTask(new Runnable()
                {
                @Override
                public void run()
                    {
                    try
                        {
                        invoke(chan, runnable, 0);
                        }
                    catch (IOException e) {}
                    }
                }, cMillis);
            }
        }

    /**
     * Invoke a wakeup on the selector.
     */
    protected void wakeup()
        {
        Selector selector = m_selector;
        if (selector != null)
            {
            selector.wakeup();
            }
        }

    /**
     * Ensure that the selector is available
     *
     * @param chan the channel which needs a selector
     *
     * @return the selector
     *
     * @throws IOException if an IO error occurs
     */
    private Selector ensureSelector(SelectableChannel chan)
        throws IOException
        {
        Selector selector = m_selector;
        if (selector == null)
            {
            synchronized (this)
                {
                selector = m_selector;
                if (selector == null)
                    {
                    m_selector = selector = chan.provider().openSelector();
                    notifyAll(); // see run()
                    }
                }
            }

        if (!selector.isOpen())
            {
            throw new ClosedSelectorException();
            }
        else if (!chan.provider().equals(selector.provider()))
            {
            throw new IllegalSelectorException();
            }

        return selector;
        }

    /**
     * Return the number of channels currently managed by the service.
     *
     * @return the channel count.
     */
    protected synchronized int getChannelCount()
        {
        Selector selector = m_selector;
        return selector == null
                ? f_mapRegistrations.size()
                : Math.max(f_mapRegistrations.size(), selector.keys().size());
        }

    /**
     * Return the number of active channels currently managed by the service.
     *
     * @return the channel count.
     */
    protected int getActiveChannelCount()
        {
        Selector selector = m_selector;
        return selector == null
                ? 0
                : selector.keys().size();
        }

    @Override
    public void associate(SelectableChannel chanParent, SelectableChannel chanChild)
            throws IOException
        {
        register(chanChild, null);
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void shutdown()
        {
        Selector selector = m_selector;
        if (selector != null)
            {
            try
                {
                selector.close();
                }
            catch (IOException e) {}
            }
        }


    // ----- RunnableSelectionService interface -----------------------------

    /**
     * Set the duration the {@link #run} method should block with no
     * registered keys before returning.
     * <p>
     * Upon timing out the {@link #run} method will return, but the service
     * will not be shutdown.
     * <p>
     * A timeout specified while {@link #run} is active may not immediately
     * take effect.
     *
     * @param cMillis the idle timeout, or zero for infinite
     *
     * @return this object
     */
    public RunnableSelectionService setIdleTimeout(long cMillis)
        {
        m_cMillisTimeout = cMillis;
        return this;
        }

    /**
     * Return the duration the {@link #run} method should block with no
     * registered keys before returning.
     *
     * @return the idle timeout, or zero for infinite
     */
    public long getIdleTimeout()
        {
        return m_cMillisTimeout;
        }

    /**
     * Indicate if the service is idle (has no registered channels)).
     *
     * @return true iff the service is idle
     */
    public synchronized boolean isIdle()
        {
        Selector selector = m_selector;
        return selector == null || !selector.isOpen() ||
            (selector.keys().isEmpty() && f_mapRegistrations.isEmpty() &&
             f_tasks.isEmpty());
        }


    // ----- Runnable interface ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void run()
        {
        Selector selector = null;
        try
            {
            // wait for the lazily set selector; see ensureSelector()
            synchronized (this)
                {
                while ((selector = m_selector) == null)
                    {
                    long cWait = getIdleTimeout();
                    Blocking.wait(this, cWait);
                    if (cWait != 0 && m_selector == null)
                        {
                        return;
                        }
                    }
                }

            // perform service processing
            process();

            // try to close the selector to avoid fd leaks - COH-5911
            synchronized(this)
                {
                // ensure that there is no pending work for the selector. If there is
                // pending work, ResumableSelectionService should call the run
                // method again.
                if (isIdle())
                    {
                    // unlike shutdown, we close the selector but are
                    // still open for business
                    m_selector = null;
                    selector.close();
                    }
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        catch (InterruptedIOException e)
            {
            Thread.currentThread().interrupt();
            }
        catch (ClosedSelectorException e)
            {
            // we've been shutdown
            }
        catch (IOException e)
            {
            if (selector.isOpen())
                {
                throw new RuntimeException(e);
                }
            }
        }


    // ----- Object interface ----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized String toString()
        {
        // TODO: include load (percentage of time awake)?
        return "SelectionService(channels=" + getChannelCount() + ", selector=" + m_selector + ", id=" + System.identityHashCode(this) + ")";
        }


    // ----- helper methods ------------------------------------------------

    /**
     * Perform service processing.
     * <p>
     * It is unsafe to call this method outside of the service thread.
     *
     * @throws IOException on an I/O error which is not related to a specific
     *         channel
     */
    protected void process()
        throws IOException
        {
        Selector          selector   = m_selector;
        Set<SelectionKey> setReg     = selector.keys();
        Set<SelectionKey> setReady   = selector.selectedKeys();
        boolean           fImmediate = false;

        try (NonBlocking nonBlocking = new NonBlocking()) // mark thread as non-blocking during processing loop
            {
            for (int i = 0; ; ++i)
                {
                if (!fImmediate) // don't execute runnables or registrations while there are pending canceled keys
                    {
                    processRunnables();

                    if (m_fPendingRegistrations)
                        {
                        fImmediate |= processRegistrations();
                        }
                    }

                // perform select
                try
                    {
                    if (fImmediate || m_fPendingRegistrations || !f_tasks.isEmpty())
                        {
                        selector.selectNow();
                        fImmediate = false;
                        continue; // ensure any pending registrations get picked up so we don't risk using an old handler for new IO
                        }
                    else
                        {
                        // we split the idle timeout in half and do up to two timed selects.  This is because
                        // select(timeout) == 0 doesn't necessarily mean that timeout time has passed, it could
                        // mean that no time has passed and that it just cleaned up some canceled keys. By breaking
                        // it in two we help ensure that on the second == 0 that some time has really passed
                        long cMillisTimeout = (getIdleTimeout() + 1) / 2;
                        if (Blocking.select(selector, cMillisTimeout) == 0 && cMillisTimeout != 0 && setReg.isEmpty() && isIdle() &&
                            Blocking.select(selector, cMillisTimeout) == 0 && isIdle())
                            {
                            return;
                            }
                        }
                    }
                catch (CancelledKeyException e)
                    {
                    // This can happen if another thread concurrently closes
                    // a registered channel, during the start of the select
                    // operation. We need to force another selection.
                    fImmediate = true;
                    }

                // process selected keys
                boolean fEager = true;
                for (int iSpin = 0; fEager && iSpin < 4; ++iSpin) // TODO: identify the proper spin limiter; make configurable?
                    {
                    fEager = false;
                    for (Iterator<SelectionKey> iter = setReady.iterator(); iter.hasNext(); )
                        {
                        SelectionKey key = iter.next();
                        try
                            {
                            int nInterest = key.interestOps();
                            int nReady    = key.readyOps(); // nReady is a sub-set of nInterest

                            int nInterestNew = ((Handler) key.attachment()).onReady(
                                    iSpin > 0
                                        ? Handler.OP_EAGER | nInterest
                                        : nReady);

                            if ((nInterestNew & Handler.OP_EAGER) == 0)
                                {
                                // not eager; remove from ready set
                                iter.remove();
                                }
                            else // eager
                                {
                                // leave eager channels in the ready set
                                fEager        = true;
                                nInterestNew &= ~Handler.OP_EAGER; // hide eager from actual selector layer
                                }

                            if (nInterestNew != nInterest)
                                {
                                // profiling has shown that setting interestOps is not "free", so avoid doing
                                // it unless necessary; note that it is cheaper to read it then to write it
                                key.interestOps(nInterestNew);
                                }
                            }
                        catch (Throwable t)
                            {
                            // on error, unregister the handler and force a
                            // immediate selection to clear the key from selector
                            // allowing for future registrations
                            key.cancel();
                            fImmediate = true;
                            try
                                {
                                iter.remove();
                                }
                            catch (IllegalStateException e)
                                {
                                // we may have already removed above; eat it
                                }
                            }
                        }
                    }

               if (fEager && (i & 0xF) == 0) // TODO: identify the proper eagerness limiter; make configurable?
                    {
                    // periodically clear any eager channels in case they are being over eager
                    setReady.clear();
                    }
                }
            }
        }

    /**
     * Process any pending registrations.
     * <p>
     * This is an internal operation which occurs as part of the
     * {@link #process} operation.  It is not safe to call it outside of the
     * service thread.
     *
     * @return true if immediate selection is required
     *
     * @throws IOException on an I/O error which is not related to a specific
     *         channel
     */
    protected synchronized boolean processRegistrations()
        throws IOException
        {
        boolean fImmediate = false;

        // Note: I've evaluated using ConcurrentHashMap or
        // ConcurrentLinkedQueue in place of a HashMap protected by service
        // synchronization. The cost of an isEmpty() check on either of the
        // concurrent options is more expensive then the single volatile
        // read used in the synchronized solution. Since the majority of the
        // time the registration map should be empty, the current approach
        // seems to be a good choice.
        Map<SelectableChannel, Handler> mapReg = f_mapRegistrations;

        Selector selector = m_selector;
        for (Map.Entry<SelectableChannel, Handler> entry : mapReg.entrySet())
            {
            SelectableChannel chan    = entry.getKey();
            Handler           handler = entry.getValue();

            if (handler == null)
                {
                SelectionKey key = chan.keyFor(selector);
                if (key != null)
                    {
                    fImmediate = true;
                    key.cancel();
                    }
                }
            else
                {
                try
                    {
                    chan.register(selector, chan.validOps(), handler);
                    }
                catch (IOException e)
                    {
                    fImmediate = true;
                    }
                catch (CancelledKeyException e)
                    {
                    fImmediate = true;
                    }
                }
            }

        mapReg.clear();
        m_fPendingRegistrations = false;

        return fImmediate;
        }

    /**
     * Execute Runnable in the SelectionService thread
     */
    protected void processRunnables()
        {
        // it is allowable for a task to add another task to the queue in which case we
        // could loop forever (MultiplexedSocket accept can cause this), add a marker so we can
        // avoid doing any more work then necessary.
        // Note, if we have EOS followed by more work it simply means we'll do a selectNow and
        // then come back here, so the impact is quite minimal, it just serves to allow for
        // some task swapping
        f_tasks.add(EOS);
        for (Runnable task = f_tasks.poll(); task != EOS; task = f_tasks.poll())
            {
            try
                {
                task.run();
                }
            catch (Throwable thr) {}
            }
        }


    // ----- data members --------------------------------------------------

    /**
     * Marker for end of task stream
     */
    private static final Runnable EOS = () -> {};

    /**
     * The selector associated with this service.
     */
    private volatile Selector m_selector;

    /**
     * Map of pending (re)registrations.
     * <p>
     * All access to the map is protected via synchronization on the service.
     */
    private final Map<SelectableChannel, Handler> f_mapRegistrations = new HashMap<>();

    /**
     * Queue of tasks to run.
     */
    private final Queue<Runnable> f_tasks = new ConcurrentLinkedQueue<>();

    /**
     * Flag indicating if the pending registration map contains any values.
     */
    private volatile boolean m_fPendingRegistrations;

    /**
     * The selector idle timeout.
     */
    private long m_cMillisTimeout;
    }
