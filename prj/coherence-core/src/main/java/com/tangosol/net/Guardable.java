/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


/**
* A Guardable is a logical execution unit whose lifetime may be guarded by a
* Guardian.  A Guardable may be a simple thread, or a more complex service.
* <p>
* Guardable objects are responsible for periodically proving that they are
* alive.  Failure to do so will cause the Guardian to take corrective actions:
* <ol>
*  <li>The Guardian may attempt to revive the Guardable by calling
*  <tt>recover</tt>.
*  <li>Upon failure to recover, the Guardian will abandon the Guardable by
*  unregistering it and calling <tt>terminate</tt>.
* </ol>
* Note: To prevent cascading failure (e.g. deadlock) all corrective
* actions are performed by the Guardian on temporary threads.
* <p>
* Guardable objects should be registered with an sufficiently sized SLA to
* accommodate variances in system-load, GC latencies etc.  Guardians are not
* responsible for detecting or correcting for these conditions.
*
* @author rhl
* @since  Coherence 3.5
*/
public interface Guardable
    {
    /**
    * Set the guard context.  Passing in <tt>null</tt> indicates that the
    * Guardable is no longer being monitored by a Guardian.
    *
    * @param context  the context guarding this Guardable
    */
    public void setContext(Guardian.GuardContext context);

    /**
    * Return the guard context monitoring this guardable, or <tt>null</tt>
    * if this Guardable is no longer being monitored.
    *
    * @return the context guarding this Guardable
    */
    public Guardian.GuardContext getContext();

    /**
    * Attempt to recover this Guardable.
    * <p>
    * In a common case where the Guardable has an associated thread, a
    * suitable action would be to interrupt the thread.
    * <p>
    * If this method does not return before the timeout expires, the Guardable
    * will be considered unrecoverable and will be terminated.
    */
    public void recover();

    /**
    * Terminate this Guardable.
    * <p>
    * This is the final action taken by the Guardian before the Guardable is
    * removed from its responsibility list.
    */
    public void terminate();
    }
