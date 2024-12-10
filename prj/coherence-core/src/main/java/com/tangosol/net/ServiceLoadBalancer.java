/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import java.util.Comparator;
import java.util.List;

/**
* A ServiceLoadBalancer is a pluggable strategy for controlling the
* client load across individual members of a clustered Service.
*
* @author jh  2010.12.07
*
* @since Coherence 3.7
*/
public interface ServiceLoadBalancer<S extends Service, T extends ServiceLoad>
    extends Comparator<T>
    {
    /**
    * Called by the Service exactly once on this object as part of its
    * initialization.
    *
    * @param service  the containing Service
    */
    public void init(S service);

    /**
    * Update the load balancing strategy in response to a change in a
    * Service utilization.
    *
    * @param member  the Member for which the utilization changed
    * @param load    the updated ServiceLoad; if null, the utilization
    *                for the specified Member is unknown (e.g. when the
    *                Service on the specified Member leaves the cluster)
    */
    public void update(Member member, T load);

    /**
    * Called by the Service when a new client connects to obtain an
    * ordered list of Members to which the new client should be redirected.
    * If the returned list is null, empty, or contains a single Member that
    * is the "local" Member, the client will remain connected to the calling
    * Service.
    *
    * @param client  the Member object that represents the remote client
    *
    * @return the ordered list of Member objects to which the client should
    *         be redirected
    */
    public List<Member> getMemberList(Member client);

    /**
     * Compare to ServerLoad objects.
     *
     * @param load1  the first ServiceLoad
     * @param load2  the second ServiceLoad
     *
     * @return a negative integer, zero, or a positive integer indicating if load1 is less then, equal, or greater then load2
     */
    @Override
    default int compare(T load1, T load2)
        {
        return load1.compareTo(load2);
        }
    }
