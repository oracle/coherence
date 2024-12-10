/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.net.SelectionService;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The ResumableSelectionService will automatically allocate and release
 * threads to handle the SelectionService.  When channels are first registered
 * with the service a thread will be started.  Once all channels are
 * deregistered, the service will automatically release the thread once the
 * service crosses its idle timeout.  Any subsequent registration will
 * allocate a new thread to handle the service.
 *
 * @author mf  2010.11.23
 */
public class ResumableSelectionService
        extends RunnableSelectionService
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a RestartableSelectionService.
     *
     * @param factory   the ThreadFactory to use in creating threads
     */
    public ResumableSelectionService(ThreadFactory factory)
        {
        m_factory = factory;
        }

    // ----- SelectionService interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized void register(SelectableChannel chan, SelectionService.Handler handler)
        throws IOException
        {
        super.register(chan, handler);

        // ensure there is a thread running this
        ensureThread();
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void invoke(SelectableChannel chan, Runnable runnable, long cMillis)
        throws IOException
        {
        super.invoke(chan, runnable, cMillis);
        // ensure there is a thread running this
        ensureThread();
        }

    /**
     * Ensure that there is a service thread running.
     *
     * @return the running thread
     */
    protected Thread ensureThread()
        {
        Thread thread = m_thread;
        if (thread == null)
            {
            synchronized (this)
                {
                thread = m_thread;
                if (thread == null)
                    {
                    // (re)start the service
                    thread = m_factory.newThread(this);
                    m_thread = thread;
                    thread.setName(toString());
                    thread.start();
                    }
                }
            }
        return thread;
        }

    @Override
    protected boolean processRegistrations()
            throws IOException
        {
        int     cChanPre = getActiveChannelCount();
        boolean fSelect  = super.processRegistrations();
        if (getActiveChannelCount() != cChanPre)
            {
            ensureThread().setName(toString());
            }
        return fSelect;
        }

    @Override
    protected void wakeup()
        {
        if (Thread.currentThread() != ensureThread())
            {
            super.wakeup();
            }
        }

    // ----- Runnable interface ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void run()
        {
        if (Thread.currentThread() != m_thread)
            {
            throw new UnsupportedOperationException();
            }

        Logger.getLogger(getClass().getName()).log(Level.FINER, (m_fUsed ? "Resuming " : "Starting ") + this);
        m_fUsed = true;

        try
            {
            while (true)
                {
                try
                    {
                    super.run();
                    }
                catch (Throwable e)
                    {
                    // If we drop this thread the channels managed by this service
                    // will not get processed, we must continue
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE,
                            "Unhandled exception in " + this +
                            ", attempting to continue", e);

                    try
                        {
                        // in case the service is broken, avoid eating CPU by
                        // sleeping for a bit
                        Blocking.sleep(1000);
                        }
                    catch (InterruptedException e2)
                        {
                        Thread.currentThread().interrupt(); // eat it for now
                        }
                    }

                synchronized (this)
                    {
                    if (isIdle())
                        {
                        m_thread = null;
                        return;
                        }
                    }
                }
            }
        finally
            {
            Logger.getLogger(getClass().getName()).log(Level.FINER, "Suspending " + this);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The ThreadFactory to use for producing the threads to run the service.
     */
    protected final ThreadFactory m_factory;

    /**
     * The thread running the service.
     */
    protected volatile Thread m_thread;

    /**
     * Single transition from false to true to indicate if it has ever been used
     */
    protected boolean m_fUsed;
    }
