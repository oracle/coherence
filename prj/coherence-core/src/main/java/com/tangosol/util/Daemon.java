/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.net.Guardable;
import com.tangosol.net.Guardian;

import com.oracle.coherence.common.base.Blocking;
import com.tangosol.net.Guardian.GuardContext;
import com.tangosol.net.GuardSupport;


/**
* A abstract Daemon thread handler. A sub-class need only provide an
* implementation of the run() method. When the Daemon is told to start, it
* will create a Java thread, which means that the number of Daemon instances
* within a system must be limited to a reasonable number. If an arbitrarily
* large number of conceptual daemons are necessary, consider using a
* TaskDaemon, which allows arbitrary tasks to be queued for execution. If
* thread pooling is desired, consider using a WorkManager implementation.
*
* @author cp  2000.08.02
* @author cp  2006.02.23 (Coherence 3.2) added restartability
* @author jh  2010.05.07 (Coherence 3.6) added shutdown
*/
public abstract class Daemon
        extends Base
        implements Runnable, Guardable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.  Creates a Daemon using default settings. The
    * daemon will not be automatically started.
    */
    public Daemon()
        {
        this(null);
        }

    /**
    * Creates a Daemon with the specified name. The daemon will not be
    * automatically started.
    *
    * @param sName  the thread name (may be null)
    */
    public Daemon(String sName)
        {
        this(sName, Thread.NORM_PRIORITY, false);
        }

    /**
    * Creates a Daemon with a specified name and priority.
    * <p>
    * <b>Warning:</b> If the implementation class is a inner non-static
    * class that refers to its outer object ("MyOuterClass.this"), do not
    * use the auto-start option. The reason is that the run() method of the
    * Daemon may be invoked on the new thread before the construction of
    * the Daemon object itself unwinds, which means that the outer object
    * reference will not have been set yet, causing an inexplicable
    * NullPointerException.
    *
    * @param sName      the thread name (may be null)
    * @param nPriority  the thread priority, between Thread.MIN_PRIORITY and
    *                   Thread.MAX_PRIORITY inclusive
    * @param fStart     pass true to auto-start the thread as part of its
    *                   construction
    */
    public Daemon(String sName, int nPriority, boolean fStart)
        {
        setConfiguredName(sName);
        setConfiguredPriority(nPriority);

        // auto-start the daemon if requested to
        if (fStart)
            {
            start();
            }
        }


    // ----- Runnable interface ---------------------------------------------

    /**
    * The daemon's implementation method.  Override this method to implement
    * a daemon.
    * <p>
    * An example implementation is:
    * <pre>{@code
    *   while (!isStopping())
    *       {
    *       // do some processing
    *       // ...
    *
    *       synchronized (this)
    *           {
    *           // wait for notification of more work
    *           wait();
    *           }
    *       }
    * }</pre>
    */
    public abstract void run();


    // ----- Guardable interface --------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void setContext(Guardian.GuardContext context)
        {
        m_guardContext = context;
        }

    /**
    * {@inheritDoc}
    */
    public void recover()
        {
        Thread thread = getThread();
        if (thread != null)
            {
            thread.interrupt();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void terminate()
        {
        stop();
        }

    /**
    * {@inheritDoc}
    */
    public Guardian.GuardContext getContext()
        {
        return m_guardContext;
        }


    // ----- Guardian-related helpers ---------------------------------------

    /**
    * Return true iff this Daemon's execution is guarded.  See {@link
    * com.tangosol.net.Guardian Guardian}.
    *
    * @return true iff this Daemon is guarded
    */
    public boolean isGuarded()
        {
        return m_guardContext != null;
        }

    /**
    * If this Daemon is guarded, return the maximum wait time that the Daemon
    * is allowed to wait for, according to its SLA.
    *
    * @param cDefaultMillis  the default wait time to return if this Daemon
    *                        is not Guarded
    * @return the maximum wait time allowed, or cDefaultMillis if this Daemon
    *         is not guarded
    */
    public long getMaxWaitMillis(long cDefaultMillis)
        {
        long                  cMaxWaitMillis = cDefaultMillis;
        Guardian.GuardContext context        = getContext();
        if (context != null)
            {
            // Adjust the timeout given by the guard contract; only
            // wait for a fraction of that time between heartbeats
            long cTimeoutMillis = context.getSoftTimeoutMillis();
            cMaxWaitMillis = Math.min(cDefaultMillis, Math.max(1L, cTimeoutMillis >> 2));
            }
        return cMaxWaitMillis;
        }

    /**
    * If this Daemon is being guarded, issue a heartbeat to the Guardian.
    */
    protected void heartbeat()
        {
        GuardContext context = getContext();
        if (context != null)
            {
            context.heartbeat();
            }
        }

    /**
    * If this Daemon is being guarded, issue a heartbeat to the Guardian.
    *
    * @param cMillis  the number of milliseconds for which this Daemon
    *                 should not be considered timed out
    */
    protected void heartbeat(long cMillis)
        {
        GuardContext context = getContext();
        if (context != null)
            {
            context.heartbeat(cMillis);
            }
        }

    /**
    * Set the Guardian registration action.  This action is invoked when a new
    * DaemonWorker thread is started to register the daemon with its Guardian.
    *
    * @param action  the Guardian registration action
    */
    protected void setGuardRegisterAction(Runnable action)
        {
        m_actionGuardRegister = action;
        }

    /**
    * Return the Guardian registration action.
    *
    * @return the Guardian registration action
    */
    protected Runnable getGuardRegisterAction()
        {
        return m_actionGuardRegister;
        }

    /**
    * Set the Guardian and policy to guard this Daemon with.  The Daemon is
    * registered with the specified Guardian each time the Daemon is started,
    * and is released each time the Daemon is stopped.
    *
    * @param guardian        the Guardian that will be guarding this Daemon
    * @param cTimeoutMillis  the timeout in ms for this Daemon, or 0 for the
    *                        service guardian timeout
    * @param flPctRecover    the recovery percentage for this Daemon
    */
    protected void setGuardPolicy(final Guardian guardian, final long cTimeoutMillis,
                                  final float flPctRecover)
        {
        // if the Daemon is already guarded, remove the old guard policy
        GuardContext ctx = getContext();
        if (ctx != null)
            {
            ctx.release();
            }

        // capture the Guardian and SLA in a registration thunk
        setGuardRegisterAction(new Runnable()
            {
            public void run()
                {
                if (cTimeoutMillis == 0)
                    {
                    guardian.guard(Daemon.this);
                    }
                else
                    {
                    guardian.guard(Daemon.this, cTimeoutMillis, flPctRecover);
                    }
                }
            });

        if (isRunning())
            {
            // if the Daemon is running, enforce the new guard policy
            guardIfNeeded();
            }
        }

    /**
    * If this Daemon has a Guardian and SLA policy specified, ensure that it
    * is registered with its Guardian.
    */
    protected void guardIfNeeded()
        {
        Runnable actionGuard = getGuardRegisterAction();
        if (actionGuard != null)
            {
            actionGuard.run();
            }
        }


    // ----- Thread-related -------------------------------------------------

    /**
    * Accessor to obtain the Daemon thread object.
    * <p>
    * The thread returned by this accessor will be null if the Daemon is
    * stopped.
    *
    * @return the thread object
    */
    public Thread getThread()
        {
        DaemonWorker worker = getWorker();
        return worker == null ? null : worker.getThread();
        }

    /**
    * Accessor to obtain the Daemon worker object.
    * <p>
    * The worker returned by this accessor will be null if the Daemon is
    * stopped.
    *
    * @return the worker object
    */
    public DaemonWorker getWorker()
        {
        return m_worker;
        }

    /**
    * Performs a synchronized start of the thread if the thread is not
    * already started.  This means that when control returns to the caller
    * of this method, that the thread has started. This method has no effect
    * if the daemon thread is already running, even if it is in the process
    * of stopping. Note that a daemon can be re-started once it has stopped.
    */
    public synchronized void start()
        {
        DaemonWorker worker = getWorker();

        if (worker != null && worker.isCurrentThread())
            {
            // what does it mean when a daemon thread asks itself to
            // start? it must be started if it's calling this method,
            // right?
            return;
            }

        if (worker != null && !worker.getThread().isAlive())
            {
            // clean up from a failed thread
            changeState(STATE_STOPPED, worker);
            }

        guardIfNeeded();
        switch (getState())
            {
            default:
            case STATE_STOPPED:
                worker = instantiateWorker();
                configureWorker(worker);
                changeState(STATE_STARTING, worker);
                worker.getThread().start();

                // fall through
            case STATE_STARTING:
                azzert(worker != null);

                // wait for the thread to finish starting
                finishStarting(worker);
                return;

            case STATE_RUNNING:
                return;

            case STATE_STOPPING:
                // this is a strange situation, since the thread is
                // going to stop but it hasn't yet, so it is still
                // running, so consider the start to be successful
            }
        }

    /**
    * Check if the daemon is running (has started and has not stopped).
    *
    * @return true if and only if the daemon is running
    */
    public boolean isRunning()
        {
        switch (getState())
            {
            default:
            case STATE_STOPPED:     // not running
            case STATE_STARTING:    // not yet started
                return false;

            case STATE_RUNNING:     // running
            case STATE_STOPPING:    // not yet stopped running
                Thread thread = getThread();
                return thread != null && thread.isAlive();
            }
        }

    /**
    * Request the daemon to stop. This method will only have an effect if the
    * daemon sub-class respects the value returned from {@link #isStopping()}.
    */
    public synchronized void stop()
        {
        DaemonWorker worker = getWorker();
        if (worker != null && worker.isCurrentThread())
            {
            // stop called on self immediately marks the daemon as
            // having been stopped
            changeState(STATE_STOPPED, worker);
            }
        else if (isOnWorkerThread())
            {
            // this is being called from a daemon worker thread that is
            // already "stopped" from the client point of view
            }
        else if (worker != null && !worker.getThread().isAlive())
            {
            // the thread died already
            changeState(STATE_STOPPED, worker);
            }
        else
            {
            switch (getState())
                {
                case STATE_STARTING:
                    // wait for the thread to finish starting
                    finishStarting(worker);

                    if (getState() != STATE_RUNNING || worker != getWorker())
                        {
                        // while we were waiting in finishStarting(), the worker
                        // thread has skipped the RUNNING state, which means
                        // that it is stopping or has already stopped
                        return;
                        }
                    // fall through
                case STATE_RUNNING:
                    changeState(STATE_STOPPING, worker);
                    return;

                case STATE_STOPPING:    // already stopping
                case STATE_STOPPED:     // already stopped
                }
            }
        }

    /**
    * Request the daemon to stop and wait up to the specified number of
    * milliseconds for it to exit. This method will only have an effect if
    * the daemon sub-class respects the value returned from
    * {@link #isStopping()}.
    *
    * @param cWait  the maximum number of milliseconds to wait for the
    *               daemon to exit; pass zero to return immediately;
    *               pass -1 to block the calling thread until the worker
    *               finishes stopping
    */
    public synchronized void shutdown(long cWait)
        {
        if (isOnWorkerThread())
            {
            throw new IllegalStateException(
                    "shutdown cannot be called by a daemon thread");
            }
        else
            {
            stop();
            }

        if (getState() == STATE_STOPPING)
            {
            finishStopping(getWorker(), cWait);
            }
        }

    /**
    * Check if the daemon is supposed to stop. This method is primarily used
    * by the daemon thread itself to check if it should stop processing.
    *
    * @return true if and only if the worker thread is no longer supposed
    *         to be running
    */
    public boolean isStopping()
        {
        switch (getState())
            {
            case STATE_STOPPING:
                return true;

            case STATE_STOPPED:
                // normally we return false here, but in the case where the
                // caller is a child thread, then the result is true as
                // from its point of view it is still stopping.  This state
                // can be reached when a daemon calls stop on itself, in which
                // case it immediately goes into the stopped state, but has
                // to still think it is stopping until the thread dies.
                return isOnWorkerThread();

            default:
                return false;
            }
        }

    /**
    * Indicate if the current execution thread is a child of this daemon.
    *
    * @return true if the current thread is a child of the Daemon
    */
    public boolean isOnWorkerThread()
        {
        return m_threadGroup == Thread.currentThread().getThreadGroup();
        }

    /**
    * Obtain the state of the daemon.
    *
    * @return  one of the STATE_enums
    */
    protected int getState()
        {
        return m_nState;
        }

    /**
    * Wait for the specified worker to finish starting. This method is called
    * while the daemon is in the STARTING state. The caller must be
    * synchronized on <b>this</b> daemon object.
    *
    * @param worker  a worker created by this daemon
    */
    protected void finishStarting(DaemonWorker worker)
        {
        while (worker == getWorker() && worker.getThread().isAlive()
                  && getState() == STATE_STARTING)
            {
            try
                {
                // wait for the daemon worker thread to finish starting
                // (be aware that _anything_ can change while we are waiting)

                Blocking.wait(this, 1000);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * Wait for the specified worker to finish stopping. This method is called
    * while the daemon is in the STOPPING state. The caller must be
    * synchronized on <b>this</b> daemon object.
    *
    * @param worker  a worker created by this daemon
    * @param cWait   the maximum number of milliseconds to wait for the
    *                specified worker to finish stopping; pass zero to return
    *                immediately; pass -1 to block the calling thread until
    *                the worker finishes stopping
    */
    protected void finishStopping(DaemonWorker worker, long cWait)
        {
        long ldtStart = getSafeTimeMillis();
        long ldtNow   = ldtStart;
        long ldtStop  = cWait < 0  ? Long.MAX_VALUE :
                        cWait == 0 ? ldtStart : ldtStart + cWait;

        while (worker == getWorker() && worker.getThread().isAlive()
                  && getState() == STATE_STOPPING)
            {
            try
                {
                if (ldtNow >= ldtStop)
                    {
                    return;
                    }
                Blocking.wait(this, ldtStop - ldtNow);

                ldtNow = getSafeTimeMillis();
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * Change the state of the daemon.
    *
    * @param nState  one of the STATE_enums
    * @param worker  the new worker, if starting, otherwise the current
    *                worker
    */
    protected synchronized void changeState(int nState, DaemonWorker worker)
        {
        DaemonWorker workerPrev = getWorker();
        switch (nState)
            {
            case STATE_STARTING:
                if (workerPrev != null)
                    {
                    err("unexpected state: a thread (" + worker
                        + ") is starting while another thread (" + workerPrev
                        + ") still exists");
                    }

                m_worker = worker;
                m_nState = STATE_STARTING;
                break;

            case STATE_RUNNING:
                if (worker == workerPrev)
                    {
                    m_nState = STATE_RUNNING;

                    if (isGuarded())
                        {
                        // set the guardian context for the current thread
                        GuardSupport.setThreadContext(getContext());
                        }
                    }
                break;

            case STATE_STOPPING:
                if (worker == workerPrev)
                    {
                    worker.notifyStopping();
                    m_nState = STATE_STOPPING;
                    }
                break;

            case STATE_STOPPED:
                if (worker == workerPrev)
                    {
                    worker.notifyStopping();
                    m_worker = null;
                    m_nState = STATE_STOPPED;

                    // release the GuardContext if this Daemon is being guarded
                    if (isGuarded())
                        {
                        getContext().release();
                        GuardSupport.setThreadContext(null);
                        }
                    }
                break;
            }

        notifyAll();
        }


    // ----- daemon configuration -------------------------------------------

    /**
    * Configure the priority for the daemon.
    *
    * @param nPriority  the thread priority for the daemon
    */
    protected void setConfiguredPriority(int nPriority)
        {
        nPriority = Math.min(nPriority, Thread.MAX_PRIORITY);
        nPriority = Math.max(nPriority, Thread.MIN_PRIORITY);
        m_nConfiguredPriority = nPriority;
        }

    /**
    * Determine the configured priority for the daemon.
    *
    * @return  the configured thread priority for the daemon
    */
    protected int getConfiguredPriority()
        {
        return m_nConfiguredPriority;
        }

    /**
    * Configure the name for the daemon.
    *
    * @param sName  the thread name for the daemon
    */
    protected void setConfiguredName(String sName)
        {
        m_sConfiguredName = sName;
        }

    /**
    * Determine the configured name for the daemon.
    *
    * @return  the configured thread name for the daemon
    */
    protected String getConfiguredName()
        {
        return m_sConfiguredName;
        }

    /**
    * Configure the context ClassLoader for the daemon thread.
    * <p>
    * If the daemon thread is not currently running, the specified ClassLoader
    * will be associated with the daemon thread when it is started (or
    * restarted). Otherwise, {@link Thread#setContextClassLoader(ClassLoader)
    * setContextClassLoader} will be called on the daemon thread object
    * immediately as well as during subsequent restarts.
    *
    * @param loader  the context ClassLoader for the daemon thread
    */
    public void setThreadContextClassLoader(ClassLoader loader)
        {
        Thread thread = getThread();
        if (thread != null)
            {
            thread.setContextClassLoader(loader);
            }

        m_loaderConfigured = loader;
        }

    /**
    * Determine the configured context ClassLoader for the daemon thread.
    *
    * @return  the configured context ClassLoader for the daemon thread
    */
    public ClassLoader getThreadContextClassLoader()
        {
        return m_loaderConfigured;
        }


    // ----- inner class: DaemonWorker --------------------------------------

    /**
    * Instantiate a DaemonWorker that will be used as a daemon.
    *
    * @return a new instance of DaemonWorker or a sub-class thereof
    */
    protected DaemonWorker instantiateWorker()
        {
        return new DaemonWorker();
        }

    /**
    * Configure a worker to use as a daemon.
    *
    * @param worker  the DaemonWorker to configure
    */
    protected void configureWorker(DaemonWorker worker)
        {
        Thread      threadWorker;
        ThreadGroup curThreadGroup = ensureThreadGroup();
        synchronized (curThreadGroup) // ensures that the thread group is not destroyed concurrently
            {
            if (curThreadGroup.isDestroyed())
                {
                ensureThreadGroup();
                }
            threadWorker = makeThread(m_threadGroup, worker, null);
            }

        threadWorker.setDaemon(true);
        threadWorker.setPriority(getConfiguredPriority());

        String sName = getConfiguredName();
        if (sName != null)
            {
            threadWorker.setName(sName);
            }

        ClassLoader loader = getThreadContextClassLoader();
        if (loader != null)
            {
            threadWorker.setContextClassLoader(loader);
            }

        worker.setThread(threadWorker);
        }

    /**
    * Obtain the existing ThreadGroup or create one if none exists or the
    * current one is destroyed.
    *
    * @return the ThreadGroup that the worker will be part of
    */
    protected ThreadGroup ensureThreadGroup()
        {
        ThreadGroup threadGroup = m_threadGroup;
        if (threadGroup == null || threadGroup.isDestroyed())
            {
            threadGroup = m_threadGroup = new ThreadGroup(getConfiguredName());
            // Make it a daemon so that it is destroyed automatically.
            threadGroup.setDaemon(true);
            }
        return threadGroup;
        }

    /**
    * The sub-class of Thread that this Daemon uses as the actual thread
    * of execution.
    */
    public class DaemonWorker
            implements Runnable
        {
        public void run()
            {
            Daemon daemon = getDaemon();
            azzert(daemon != null);

            try
                {
                daemon.changeState(STATE_RUNNING, this);
                daemon.run();
                }
            finally
                {
                daemon.changeState(STATE_STOPPED, this);
                }

            // when this method returns, the daemon thread terminates
            }

        protected Daemon getDaemon()
            {
            return Daemon.this;
            }

        protected void notifyStopping()
            {
            m_fStopping = true;
            }

        protected boolean isStopping()
            {
            return m_fStopping;
            }

        protected void setThread(Thread thread)
            {
            m_thread = thread;
            }

        protected Thread getThread()
            {
            return m_thread;
            }

        protected boolean isCurrentThread()
            {
            return Thread.currentThread() == getThread();
            }

        private volatile boolean m_fStopping;
        private          Thread  m_thread;
        }


    // ----- logging support ------------------------------------------------

    /**
    * Return a human-readable String representation of the Daemon.
    *
    * @return a String describing the Daemon
    */
    public String toString()
        {
        return "Daemon{" + getDescription() + '}';
        }

    /**
    * Format the Daemon attributes into a String for inclusion in the String
    * returned from the {@link #toString} method.
    *
    * @return a String listing the attributes of the Daemon
    */
    protected String getDescription()
        {
        return "Thread=\"" + getThread() + '\"'
           + ", State=" + toStateString(getState());
        }

    /**
    * Convert a state value to a human-readable String.
    *
    * @param nState  a Daemon state, one of the STATE_* enums
    *
    * @return a human-readable name for the state
    */
    protected static String toStateString(int nState)
        {
        switch (nState)
            {
            case STATE_STARTING:
                return "Starting";
            case STATE_RUNNING:
                return "Running";
            case STATE_STOPPING:
                return "Stopping";
            case STATE_STOPPED:
                return "Stopped";
            default:
                return "Unknown";
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * State: Dormant, not running, no thread, etc.
    */
    private static final int STATE_STOPPED  = 0;
    /**
    * State: Starting up a daemon thread.
    */
    private static final int STATE_STARTING = 1;
    /**
    * State: Daemon thread is running.
    */
    private static final int STATE_RUNNING  = 2;
    /**
    * State: Daemon thread is running, but has been told to stop and thus
    * will likely soon be stopped.
    */
    private static final int STATE_STOPPING = 3;


    // ----- data members ---------------------------------------------------

    /**
    * The name configured for the daemon thread.
    */
    private String m_sConfiguredName;

    /**
    * The priority configured for the daemon thread.
    */
    private int m_nConfiguredPriority;

    /**
    * The context ClassLoader configured for the daemon thread.
    */
    private ClassLoader m_loaderConfigured;

    /**
    * The DaemonWorker object.
    */
    private volatile DaemonWorker m_worker;

    /**
    * State of the daemon. One of the STATE_* enums.
    */
    private volatile int m_nState;

    /**
    * ThreadGroup used to identify child workers.
    */
    private ThreadGroup m_threadGroup;

    /**
    * Action to register this Daemon with its Guardian.
    */
    private Runnable m_actionGuardRegister;

    /**
    * The GuardContext guarding this Guardable.
    */
    private Guardian.GuardContext m_guardContext;
    }
