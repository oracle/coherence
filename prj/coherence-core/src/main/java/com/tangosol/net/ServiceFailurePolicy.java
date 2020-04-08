/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


/**
* ServiceFailurePolicy is used to control how a Guardable associated with a
* service is managed by its Guardian after the configured timeout.
*
* @author rhl
* @since  Coherence 3.5
*/
public interface ServiceFailurePolicy
    {
    /**
    * Take appropriate action after the specified guardable (associated with the
    * specified service) has missed a "soft-timeout" and has entered the
    * "RECOVERY" state.
    * <p>
    * This method will be invoked on a dedicated thread; further failures, hangs
    * or deadlocks of recovery handling logic will not directly affect other
    * healthy service threads.
    * <p>
    * Note: in general, implementations should call the {@link Guardable#recover}
    *       method on the specified guardable
    *
    * @param guardable  the guardable that entered the RECOVERY state
    * @param service    the service that the guardable is associated with
    *
    * @since Coherence 3.6
    */
    public void onGuardableRecovery(Guardable guardable, Service service);

    /**
    * Take appropriate action after the specified guardable (associated with the
    * specified service) has missed a "hard-timeout" and has entered the
    * "TERMINATING" state.
    * <p>
    * This method will be invoked on a dedicated thread; further failures, hangs
    * or deadlocks of termination handling logic will not directly affect other
    * healthy service threads.
    * <p>
    * Note: in general, implementations should call the {@link Guardable#terminate}
    *       method on the specified guardable
    *
    * @param guardable  the context that entered the TERMINATING state
    * @param service    the service that the guardable is associated with
    *
    * @since Coherence 3.6
    */
    public void onGuardableTerminate(Guardable guardable, Service service);

    /**
    * Take appropriate action after some service failed to stop.  The failed
    * service may be hung, deadlocked, or otherwise in an inconsistent and/or
    * unresponsive state.
    * <p>
    * This method will be invoked on a dedicated thread; further failures,
    * hangs, or deadlocks of failure handling logic will not directly affect
    * other healthy service threads.
    * <p>
    * Note: it is strongly advised that, at a minimum, any failure policy
    *       should attempt to stop the cluster services (e.g. via
    *       <tt>Cluster.stop()</tt>) or otherwise ensure that they are stopped,
    *       in order to isolate any failure from the rest of the cluster.
    *
    * @param cluster  the cluster
    */
    public void onServiceFailed(Cluster cluster);
    }
