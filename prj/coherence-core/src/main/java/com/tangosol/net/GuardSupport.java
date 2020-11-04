/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.oracle.coherence.common.util.Threads;
import com.tangosol.net.Guardian.GuardContext;

import com.tangosol.util.Base;

import com.oracle.coherence.common.util.Duration;

import java.util.concurrent.atomic.AtomicLong;


/**
* A concrete implementation of Guardian/Guardable interactions. GuardSupport
* is used by a Guardian to manage its responsibilities.
*
* @author rhl
* @since  Coherence 3.5
*/
public class GuardSupport
        extends Base
    {
    // ----- constructors -------------------------------------------------

    /**
    * Construct a GuardSupport for the specified Guardian.
    *
    * @param guardian  the guardian managing this GuardSupport
    */
    public GuardSupport(Guardian guardian)
        {
        m_guardian = guardian;
        }


    // ----- accessors ----------------------------------------------------

    /**
    * Return the array of GuardContext objects for the Guardables managed by
    * this GuardSupport.
    *
    * @return the array of managed GuardContext objects
    */
    protected Context[] getGuardContexts()
        {
        return m_aGuardContext;
        }

    /**
    * Set the array of GuardContext objects for the Guardables that are
    * managed by this GuardSupport.
    *
    * @param aGuardContext  the array of managed GuardContext objects
    */
    protected void setGuardContexts(Context[] aGuardContext)
        {
        m_aGuardContext = aGuardContext;
        }

    /**
    * Return the Guardian that manages this GuardSupport.
    *
    * @return the Guardian that manages this GuardSupport
    */
    protected Guardian getGuardian()
        {
        return m_guardian;
        }

    /**
    * Return the next time at which the Guardables should be checked.
    *
    * @return the next time at which the Guardables should be checked
    */
    public long getNextCheckTime()
        {
        return m_ldtNextCheck;
        }

    /**
    * Set the next time at which the Guardables should be checked.
    *
    * @param ldtNextCheck  the next time at which the Guardables should be checked
    */
    protected void setNextCheckTime(long ldtNextCheck)
        {
        m_ldtNextCheck = ldtNextCheck;
        }


    // ----- methods ------------------------------------------------------

    /**
    * Add the specified guardable to the set of Guardables managed by this
    * manager, subject to the specified service parameters.  If the specified
    * guardable is already guarded by this manager, adjust the service
    * parameters.
    *
    * @param guardable     the Guardable object to be managed
    * @param cMillis       the timeout for the specified Guardable
    * @param flPctRecover  the percentage of the timeout after which to attempt
    *                      recovery of the Guardable; 0 &lt; flPctRecover &lt;=
    *                      1.0
    *
    * @return the GuardContext for the specified Guardable
    */
    public synchronized GuardContext add(Guardable guardable,
                                         long cMillis, float flPctRecover)
        {
        Context   context    = instantiateContext(guardable, cMillis, flPctRecover);
        Context[] aContext   = getGuardContexts();
        int       cGuardable = getGuardableCount();
        int       cElements  = aContext.length;
        int       iContext   = -1;

        if (cElements == cGuardable)
            {
            // the array is full; need to grow it
            Context[] aContextNew = new Context[cGuardable + 5];

            System.arraycopy(aContext, 0, aContextNew, 0, cGuardable);
            setGuardContexts(aContext = aContextNew);
            iContext = cGuardable;
            }
        else
            {
            // there is an empty slot in the array somewhere
            for (int i = 0; i < cElements; i++)
                {
                if (aContext[i] == null)
                    {
                    iContext = i;
                    break;
                    }
                }
            }
        aContext[iContext] = context;
        setGuardableCount(cGuardable + 1);

        guardable.setContext(context);

        // force an initial heartbeat to start immediate monitoring
        context.heartbeat();

        return context;
        }

    /**
    * Remove the specified Guardable from the set of Guardables managed by
    * this manager.
    *
    * @param guardable  the guardable to remove
    */
    public synchronized void remove(Guardable guardable)
        {
        Context[] aContext  = getGuardContexts();
        int       cElements = aContext.length;

        for (int i = 0; i < cElements; i++)
            {
            Context context = aContext[i];
            if (context != null && Base.equals(context.getGuardable(), guardable))
                {
                aContext[i] = null;
                setGuardableCount(getGuardableCount() - 1);
                return;
                }
            }
        }

    /**
    * Return the number of Guardables that are registered with this
    * GuardSupport.
    *
    * @return the number of registered Guardable objects
    */
    public int getGuardableCount()
        {
        return m_cGuardable;
        }

    /**
    * Set the number of Guardables that are registered with this GuardSupport.
    *
    * @param cGuardable  the number of registered Guardable objects
    */
    protected void setGuardableCount(int cGuardable)
        {
        m_cGuardable = cGuardable;
        if (cGuardable == 0)
            {
            setNextCheckTime(0L);
            }
        }

    /**
    * Check the registered Guardable objects for timeout, taking appropriate
    * action and calculate the timestamp when the next check should be
    * performed.  The next check time can later be retrieved by the {@link
    * #getNextCheckTime} method.
    *
    * @return the number of milliseconds past the time when the guardables
    *         should have been checked, or 0 if the check was performed on time
    */
    public long check()
        {
        long ldtNow            = Base.getSafeTimeMillis();
        long ldtScheduledCheck = getNextCheckTime();
        long cLateMillis       = ldtNow - ldtScheduledCheck;

        if (-cLateMillis > GUARDIAN_EARLY_THRESHOLD)
            {
            // no need to check yet; check is very early
            return 0L;
            }

        if (cLateMillis > GUARDIAN_LATE_THRESHOLD && ldtScheduledCheck > 0L)
            {
            // the Guardian is running very late; don't take any action.  Give
            // the guardables a reasonable amount of time to catch up and
            // issue a heartbeat; if the Guardian is running slowly, it is
            // likely that the guardables are running behind as well, and may
            // not have been able to issue heartbeats
            long ldtNextCheck = ldtNow + GUARDIAN_MAX_CHECK_INTERVAL;
            setNextCheckTime(ldtNextCheck);
            return cLateMillis;
            }

        long      ldtTimeoutMin = ldtNow + 1000L; // max interval of 1 second
        Context[] aContext      = getGuardContexts();
        int       cContext      = aContext.length;
        for (int i = 0; i < cContext; i++)
            {
            Context context = aContext[i];
            if (context == null)
                {
                continue;
                }

            long ldtTimeoutThis = context.getSoftTimeout();
            switch (context.getState())
                {
                case Context.STATE_HEALTHY:
                    if (context.isSuspect(ldtNow))
                        {
                        context.setState(Context.STATE_RECOVERY);
                        context.onRecovery();

                        // set the hard timeout to one recovery interval after
                        // the current time
                        long cRecoveryMillis = context.getRecoveryMillis();
                        context.setTimeout(ldtNow + cRecoveryMillis);

                        // If a guardable is in recovery, ensure that it is
                        // checked fairly frequently so that if it is successfully
                        // recovered and issues a heartbeat, the guard status
                        // gets updated in a timely manner.
                        ldtTimeoutThis = Math.min(context.getTimeout(), ldtNow + 100L);
                        }
                    break;

                case Context.STATE_RECOVERY:
                    if (!context.isSuspect(ldtNow))
                        {
                        // Note: there are 2 possible cases here:
                        //   1) a guardable that was in recovery successfully
                        //      recovered (was heartbeated)
                        //   2) a guardable that was in recovery (missed a
                        //      soft-timeout), was heartbeated, and missed a
                        //      subsequent soft-timeout.  This can only happen
                        //      if the guardian thread is running too slowly.
                        context.setState(Context.STATE_HEALTHY);
                        context.setMissedSoftTimeout(0L);
                        context.setTimeout(0L);

                        // prevent the "next check time" from moving backward
                        // in the case where the guardian thread is slow
                        ldtTimeoutThis = Math.max(ldtTimeoutThis, ldtNow);
                        }
                    else if (context.isTimedOut(ldtNow))
                        {
                        context.setState(Context.STATE_TERMINATING);
                        context.onTerminate();
                        context.release();

                        // this context is being terminated; no need to check
                        // on it any longer.
                        ldtTimeoutThis = Long.MAX_VALUE;
                        }
                    else
                        {
                        // If a guardable is in recovery, ensure that it is
                        // checked fairly frequently so that if it is successfully
                        // recovered and issues a heartbeat, the guard status
                        // gets updated in a timely manner.
                        ldtTimeoutThis = Math.min(context.getTimeout(), ldtNow + 100L);
                        }
                    break;

                default:
                    // we don't want to throw exceptions on the Guardian thread
                    Base.err("Unexpected GuardContext state " + context);
                }

            ldtTimeoutMin = Math.min(ldtTimeoutMin, ldtTimeoutThis);
            }

        // next check-time should never move backward
        setNextCheckTime(Math.max(ldtScheduledCheck, ldtTimeoutMin));
        return 0L; // check was on-time
        }

    /**
    * Release the Guardables managed by this GuardSupport.  Releasing a
    * Guardable causes it to no longer be guarded.
    */
    public synchronized void release()
        {
        Context[] aContext = getGuardContexts();
        int       cContext = aContext.length;

        // go through and release each context
        for (int i = 0; i < cContext; i++)
            {
            Context context = aContext[i];
            if (context != null)
                {
                context.release();
                }
            aContext[i] = null;
            }
        setGuardableCount(0);
        }


    // ----- inner class: Context -----------------------------------------

    /**
    * @param guardable     the Guardable object to be managed
    * @param cMillis       the timeout for the specified Guardable
    * @param flPctRecover  the percentage of the timeout after which to attempt
    *                      recovery of the Guardable; 0 &lt; flPctRecover &lt;=
    *                      1.0
    *
    * @return a Context for the specified guardable and SLA parameters
    */
    protected Context instantiateContext(
            Guardable guardable, long cMillis, float flPctRecover)
        {
        return new Context(guardable, cMillis, flPctRecover);
        }

    /**
    * Base implementation for GuardContext's used by GuardSupport.
    */
    protected class Context
            implements GuardContext
        {
        /**
        * Construct a Context.
        *
        * @param guardable       the guardable represented by this Context
        * @param cTimeoutMillis  the timeout interval for the specified Guardable
        * @param flPctRecover    the percentage of the timeout after which to
        *                        attempt recovery of the Guardable;
        *                        0 &lt; flPctRecover &lt;= 1.0
        */
        protected Context(Guardable guardable, long cTimeoutMillis, float flPctRecover)
            {
            if (guardable == null || cTimeoutMillis <= 0 ||
                flPctRecover <= 0.0F || flPctRecover > 1.0F)
                {
                throw new IllegalArgumentException("guardable=" + guardable +
                    ", timeout=" + cTimeoutMillis + ", recover=" + flPctRecover);
                }

            f_guardable                 = guardable;
            m_cDefaultTimeoutMillis     = cTimeoutMillis;
            m_cTimeoutMillis            = cTimeoutMillis;
            m_cDefaultSoftTimeoutMillis = Math.max(1L, (long) (cTimeoutMillis * flPctRecover));
            m_flPctRecover              = flPctRecover;
            m_cRecoveryMillis           = m_cDefaultTimeoutMillis - m_cDefaultSoftTimeoutMillis;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Set the state of the GuardContext to the specified value.
        *
        * @param nState  the state to set this GuardContext to
        */
        protected void setState(int nState)
            {
            m_nState = nState;
            }

        /**
        * Return the next timeout for this Guardable.
        *
        * @return the next timestamp, after which this Guardable should be
        *         considered timed out
        */
        protected long getTimeout()
            {
            return m_ldtTimeout;
            }

        /**
        * Set the next timeout for this Guardable.
        *
        * @param ldtTimeout  the next timestamp, after which this Guardable
        *                    should be considered timed out
        */
        protected void setTimeout(long ldtTimeout)
            {
            m_ldtTimeout = ldtTimeout;
            }

        /**
        * Return the next soft-timeout for this Guardable.  The soft-timeout
        * is used to determine when to attempt recovery of the Guardable.
        *
        * @return the next timestamp, after which this Guardable should be
        *         considered suspect, and recovery attempted
        */
        protected long getSoftTimeout()
            {
            return m_ldtSoftTimeout;
            }

        /**
        * Set the next soft-timeout for this Guardable.
        *
        * @param ldtSoftTimeout  the next timestamp, after which this Guardable
        *                        should be considered suspect, and recovery
        *                        attempted
        */
        protected void setSoftTimeout(long ldtSoftTimeout)
            {
            m_ldtSoftTimeout = ldtSoftTimeout;
            }

        /**
        * Return the recovery interval for this Guardable.
        *
        * @return the recovery interval for this Guardable
        */
        protected long getRecoveryMillis()
            {
            return m_cRecoveryMillis;
            }

        /**
        * Set the recovery interval for this Guardable.
        *
        * @param cRecoveryMillis  the recovery interval for this Guardable
        */
        protected void setRecoveryMillis(long cRecoveryMillis)
            {
            m_cRecoveryMillis = cRecoveryMillis;
            }

        /**
        * Return the last missed soft-timeout for this Guardable (that
        * resulted in a recovery attempt).
        *
        * @return the last missed soft-timeout for this guardable
        */
        protected long getMissedSoftTimeout()
            {
            return m_ldtLastMissedSoftTimeout;
            }

        /**
        * Set the missed soft-timeout for this guardable.
        *
        * @param ldtSoftTimeout  the soft-timeout that was missed
        */
        protected void setMissedSoftTimeout(long ldtSoftTimeout)
            {
            m_ldtLastMissedSoftTimeout = ldtSoftTimeout;
            }

        /**
        * Is the Guardable represented by this GuardContext timed out?
        *
        * @param ldtNow  the current timestamp
        *
        * @return true iff the represented Guardable is timed out
        */
        protected boolean isTimedOut(long ldtNow)
            {
            return ldtNow > getTimeout();
            }

        /**
        * Is the Guardable represented by this GuardContext suspect (in danger
        * of timing out soon).
        *
        * @param ldtNow  the current timestamp
        *
        * @return true iff the represented Guardable is suspect
        */
        protected boolean isSuspect(long ldtNow)
            {
            // Don't consider the guardable suspect if the soft-timeout has
            // changed since the last one we missed; it means that an
            // intervening heartbeat was issued, so we should transition back
            // to a healthy state (even if we have again missed the next
            // soft-timeout).
            long ldtSoftTimeout       = getSoftTimeout();
            long ldtMissedSoftTimeout = getMissedSoftTimeout();
            return ldtNow > ldtSoftTimeout &&
                (ldtSoftTimeout == ldtMissedSoftTimeout ||
                 ldtMissedSoftTimeout == 0);
            }

        /**
        * If more than half of the soft-timeout interval has passed, issue
        * a heartbeat and clear the interrupted status if set.
        */
        public void reset()
            {
            long ldtNow         = Base.getSafeTimeMillis();
            long ldtSoftTimeout = getSoftTimeout() - (m_cDefaultSoftTimeoutMillis >> 1);

            if (ldtNow >= ldtSoftTimeout)
                {
                heartbeat();
                Thread.interrupted();
                }
            }

        // ----- Object methods -------------------------------------------

        /**
        * Return a human-readable description.
        *
        * @return a human-readable description
        */
        public String toString()
            {
            String sState;
            int    nState = getState();
            switch (nState)
                {
                case STATE_HEALTHY:
                    sState = "HEALTHY";
                    break;

                case STATE_RECOVERY:
                    sState = "RECOVERY";
                    break;

                case STATE_TERMINATING:
                    sState = "TERMINATING";
                    break;

                default:
                    sState = "{Unknown State: " + nState + "}";
                }

            return "GuardContext {Guardable=" + getGuardable() +
                ", timeout=" + getTimeout() +
                ", state=" + sState + "}";
            }

        // ----- GuardContext interface -----------------------------------

        /**
        * {@inheritDoc}
        */
        public Guardian getGuardian()
            {
            return GuardSupport.this.getGuardian();
            }

        /**
        * {@inheritDoc}
        */
        public Guardable getGuardable()
            {
            return f_guardable;
            }

        /**
        * {@inheritDoc}
        */
        public void heartbeat()
            {
            long ldtNow = Base.getSafeTimeMillis();

            // re-set the default recovery interval
            if (m_cTimeoutMillis != m_cDefaultTimeoutMillis)
                {
                setRecoveryMillis(m_cDefaultTimeoutMillis - m_cDefaultSoftTimeoutMillis);
                m_cTimeoutMillis = m_cDefaultTimeoutMillis;
                }

            // set the soft timeout
            setSoftTimeout(ldtNow + m_cDefaultSoftTimeoutMillis);
            }

        /**
        * {@inheritDoc}
        */
        public void heartbeat(long cMillis)
            {
            long ldtNow       = Base.getSafeTimeMillis();
            long cSoftTimeout = (long) (cMillis * m_flPctRecover);

            Base.azzert(cMillis > 0, "Invalid heartbeat interval");

            // set the recovery-interval for non-standard timeout
            setRecoveryMillis(cMillis - cSoftTimeout);

            // set the soft timeout
            setSoftTimeout(ldtNow + Math.max(1L, cSoftTimeout));
            m_cTimeoutMillis = cMillis;
            }

        /**
        * {@inheritDoc}
        */
        public int getState()
            {
            return m_nState;
            }

        /**
        * {@inheritDoc}
        */
        public void release()
            {
            Guardable guardable = getGuardable();
            guardable.setContext(null);
            GuardSupport.this.remove(guardable);
            }

        /**
        * {@inheritDoc}
        */
        public long getSoftTimeoutMillis()
            {
            return Math.max(1L, m_cTimeoutMillis - m_cRecoveryMillis);
            }

        /**
        * {@inheritDoc}
        */
        public long getTimeoutMillis()
            {
            return m_cTimeoutMillis;
            }

        // ----- internal helpers -----------------------------------------

        /**
        * Called when the guardable enters the "RECOVERY" state.
        */
        protected void onRecovery()
            {
            final Guardable guardable      = getGuardable();
            long            ldtSoftTimeout = getSoftTimeout();
            long            cMillisLate    = Base.getSafeTimeMillis() - ldtSoftTimeout;

            Base.err("Detected soft timeout" +
                     (cMillisLate > 1000 ? " (" + cMillisLate + "ms ago)" : "") +
                     " of " + guardable);
            setMissedSoftTimeout(ldtSoftTimeout);

            // do the recovery on a separate thread
            Base.makeThread(null,
                new Runnable()
                    {
                    public void run()
                        {
                        guardable.recover();
                        }
                    },
                            "Recovery Thread").start();
            }

        /**
        * Called when the guardable enters the "TERMINATING" state.
        */
        protected void onTerminate()
            {
            final Guardable guardable   = getGuardable();
            long            cMillisLate = Base.getSafeTimeMillis() - getTimeout();

            Base.err("Detected hard timeout after " + new Duration(m_cTimeoutMillis, Duration.Magnitude.MILLI) +
                     (cMillisLate > 1000 ? " (" + new Duration(cMillisLate, Duration.Magnitude.MILLI) + " ago)" : "") +
                     " of " + guardable);

            // do the terminate on a separate thread
            Base.makeThread(null,
                new Runnable()
                    {
                    public void run()
                        {
                        guardable.terminate();
                        }
                    },
                            "Termination Thread").start();
            }


        // ----- data members and constants -------------------------------

        /**
        * The current state of the Context.
        */
        protected volatile int m_nState = STATE_HEALTHY;

        /**
        * The next timeout for the Guardable.  This property is only used by
        * the guardian thread.
        */
        protected long m_ldtTimeout;

        /**
        * The amount of time to allow for recovery
        */
        protected volatile long m_cRecoveryMillis;

        /**
        * The next soft-timeout for the Guardable
        */
        protected volatile long m_ldtSoftTimeout = Long.MAX_VALUE;

        /**
        * The Guardable represented by this GuardContext
        */
        protected final Guardable f_guardable;

        /**
        * The default timeout interval to use for the represented Guardable.
        */
        protected long m_cDefaultTimeoutMillis;

        /**
        * The timeout interval to use for the represented Guardable.
        */
        protected long m_cTimeoutMillis;

        /**
        * The default soft-timeout interval to use for the represented Guardable.
        */
        protected long m_cDefaultSoftTimeoutMillis;

        /**
        * The soft-timeout stamp that was violated, resulting in a recovery
        * attempt.
        */
        protected long m_ldtLastMissedSoftTimeout;

        /**
        * Percentage of the timeout after which to attempt recovery of the
        * Guardable.
        */
        protected float m_flPctRecover;
        }


    // ----- helper methods -----------------------------------------------

    /**
    * Obtain the GuardContext associated with the current thread.
    *
    * @return the GuardContext or null if it has not been set
    */
    public static GuardContext getThreadContext()
        {
        return (GuardContext) m_tlContext.get();
        }

    /**
    * Associate the specified GuardContext with the current thread.
    *
    * @param context  the GuardContext to associate with the current thread
    */
    public static void setThreadContext(GuardContext context)
        {
        m_tlContext.set(context);
        }

    /**
    * Issue a heartbeat on the GuardContext associated with the current thread.
    */
    public static void heartbeat()
        {
        GuardContext context = getThreadContext();
        if (context != null)
            {
            context.heartbeat();
            }
        }

    /**
    * Issue a heartbeat of the specified duration on the GuardContext
    * associated with the current thread.
    *
    * @param cMillis  the number of milliseconds to heartbeat for
    */
    public static void heartbeat(long cMillis)
        {
        GuardContext context = getThreadContext();
        if (context != null)
            {
            context.heartbeat(cMillis);
            }
        }

    /**
    * Issue a reset on the GuardContext associated with the current thread.
    */
    public static void reset()
        {
        Context context = (Context) getThreadContext();

        if (context != null)
            {
            context.reset();
            }
        }

    /**
    * Collect stack traces and synchronization information
    * for all running threads and write them to the error log.
    */
    public static void logStackTraces()
        {
        AtomicLong atomicLogTime = s_atomicLogTime;
        long       ldtNow        = getSafeTimeMillis();
        long       ldtLast       = atomicLogTime.get();
        long       ldtNext       = ldtNow + GUARDIAN_LOG_INTERVAL;

        // COH-3131: Prevent concurrent or too frequent thread dumps
        if (ldtNow >= ldtLast + GUARDIAN_LOG_INTERVAL
            && atomicLogTime.compareAndSet(ldtLast, ldtNext))
            {
            Base.err(getThreadDump());
            }
        }

    /**
    * Get the full thread dump.
    *
    * @return a string containing the thread dump
    */
    public static String getThreadDump()
        {
        return Threads.getThreadDump();
        }

    // ----- constants ----------------------------------------------------

    /**
    * The maximum interval at which the Guardian thread should call check() to
    * verify its guardables.
    */
    public static final long GUARDIAN_MAX_CHECK_INTERVAL = 5000L;

    /**
    * The threshold past which a guardian considers itself late in checking
    * its guardables.
    */
    protected static final long GUARDIAN_LATE_THRESHOLD =
        2 * GUARDIAN_MAX_CHECK_INTERVAL;

    /**
    * The threshold before which than which a guardian considers itself early
    * in checking its guardables.
    */
    protected static final long GUARDIAN_EARLY_THRESHOLD = 500L;

    /**
    * The minimum interval at which the Guardian should log thread dumps.
    */
    protected static final long GUARDIAN_LOG_INTERVAL = 3000L;


    // ----- data members -------------------------------------------------

    /**
    * Array of GuardContexts for the registered Guardables.  May contain nulls.
    */
    protected Context[] m_aGuardContext = new Context[5];

    /**
    * The number of registered Guardables.
    */
    protected int m_cGuardable;

    /**
    * The Guardian that is managing this GuardSupport.
    */
    protected Guardian m_guardian;

    /**
    * The next time at which the Guardables should be checked.
    */
    protected long m_ldtNextCheck;

    /**
    * ThreadLocal containing the GuardContext associated with the
    * current thread.
    */
    protected static ThreadLocal m_tlContext = new ThreadLocal();

    /**
    * Atomic counter containing the last log time stamp.
    */
    protected static AtomicLong s_atomicLogTime = new AtomicLong();
    }
