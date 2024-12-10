/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


/**
* ActionPolicy defines which aspects of a Service's behavior are currently
* allowed.  Action policy implementations may be used to dynamically restrict
* aspects of the associated service, for example based on the dynamic state of
* the service or cluster membership.
* <p>
* For example, ActionPolicy could be used to define a quorum-based policy to
* prevent distribution from occurring before a certain fraction (a quorum) of the
* expected service members are present.
* <p>
* ActionPolicy implementations must exercise extreme caution since any delay
* or unhandled exception will cause a delay or complete shutdown of the
* corresponding service.  ActionPolicy implementations must be thread-safe as
* the {@link #isAllowed} method may be invoked on a service's worker threads.
*
* @author rhl 2009.05.07
* @since  Coherence 3.6
*/
public interface ActionPolicy
    {
    /**
    * Called when the specified service loads and configures this policy.
    * <p>
    * Note: A policy could be applied to multiple services.
    *
    * @param service  the service that this policy applies to
    */
    public void init(Service service);

    /**
    * Evaluate if the specified action is currently allowed by this policy.
    * <p>
    * Note: for forward compatibility, implementations should generally return
    *       <tt>true</tt> for actions that are not recognized.
    *
    * @param service  the service that is performing the action
    * @param action   the action that is being performed
    *
    * @return true iff the specified action is currently allowed by this policy
    */
    public boolean isAllowed(Service service, Action action);

    /**
    * Return a human-readable String representation of this ActionPolicy.
    * <p>
    * Note: this method may be used to provide information about this
    *       ActionPolicy to management interfaces.
    */
    public String toString();
    }
