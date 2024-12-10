/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


/**
* A Guardian is responsible for monitoring the registered {@link Guardable}
* objects.
*
* @author rhl
* @since  Coherence 3.5
*/
public interface Guardian
    {
    /**
    * Register the specified Guardable based on the Guardian's SLA parameters.
    * <p>
    * It's possible that the Guardian's defaults will prevent the Guardable from
    * being registered. In such a case, this method returns null.
    *
    * @param guardable  the Guardable to be monitored by this Guardian
    *
    * @return the GuardContext for the specified Guardable (could be null)
    */
    public GuardContext guard(Guardable guardable);

    /**
    * Register the specified Guardable with the specified SLA parameters.
    *
    * @param guardable     the Guardable to be guarded by this Guardian
    * @param cMillis       the timeout for the specified Guardable
    * @param flPctRecover  the percentage of the timeout after which to attempt
    *                      recovery of the Guardable; 0 &lt; flPctRecover &lt;=
    *                      1.0
    *
    * @return the GuardContext for the specified Guardable
    */
    public GuardContext guard(Guardable guardable, long cMillis, float flPctRecover);

    /**
    * Default timeout interval (in milliseconds) for Guardables monitored by
    * this Guardian.
    *
    * @return the default timeout interval
    */
    public long getDefaultGuardTimeout();

    /**
    * Default recovery percentage for Guardables monitored by this Guardian.
    *
    * @return the default recovery percentage
    */
    public float getDefaultGuardRecovery();


    // ----- inner interface: GuardContext --------------------------------

    /**
    * A GuardContext represents the lifecycle status of a Guardable.  The
    * GuardContext is the point of coordination between the Guardian and the
    * Guardable.
    */
    public interface GuardContext
        {
        /**
        * Return the Guardian for this context.
        *
        * @return the Guardian for this context
        */
        public Guardian getGuardian();

        /**
        * Return the Guardable for this context.
        *
        * @return the Guardable for this context
        */
        public Guardable getGuardable();

        /**
        * Called by the Guardable to signal that it is still alive.
        */
        public void heartbeat();

        /**
        * Called by the Guardable to signal that it is still alive, and that
        * it should not be considered timed out for the specified number of
        * milliseconds.
        *
        * @param cMillis  the number of milliseconds for which the guardable
        *                 should not be considered timed out
        */
        public void heartbeat(long cMillis);

        /**
        * Return the state of the Guardable.  Valid values are STATE_*
        * constants.
        *
        * @return the state of the Guardable
        */
        public int getState();

        /**
        * Release this context, causing the Guardian to discontinue monitoring
        * of the represented Guardable.
        */
        public void release();

        /**
        * Return the soft timeout interval for the represented Guardable.
        * <p>
        * The soft timeout interval is the amount of time that must pass after
        * the last received heartbeat before a recovery attempt is made.
        *
        * @return the soft timeout interval for the represented Guardable
        */
        public long getSoftTimeoutMillis();

        /**
        * Return the hard timeout interval for the represented Guardable.
        *
        * @return the hard timeout interval for the represented Guardable
        */
        public long getTimeoutMillis();

        /**
        * State value indicating the Guardable is healthy/responsive.
        */
        public static final int STATE_HEALTHY     = 1;

        /**
        * State value indicating that recovery of the Guardable is underway.
        */
        public static final int STATE_RECOVERY    = 2;

        /**
        * State value indicating that the Guardable is being terminated.
        */
        public static final int STATE_TERMINATING = 3;
        }
    }
