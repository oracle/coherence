/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import java.util.Set;


/**
* The ServiceInfo represents information about a Service that
* operates in a clustered network environment. A ServiceInfo
* may be available for a Service that is not running on the
* local cluster member.
*
* @author gg  2002.02.08
*
* @since Coherence 1.1
*/
public interface ServiceInfo
    {
    /**
    * Return the name of the Service.
    *
    * @return the name of the Service
    */
    public String getServiceName();

    /**
    * Return the type of the Service.
    *
    * @return the type of the Service
    *
    * @since Coherence 2.0
    *
    * @see Cluster#ensureService(String, String)
    */
    public String getServiceType();

    /**
    * Return a Set of Member objects, one for each Member that
    * has registered this Service.
    *
    * @return Set of cluster members for this service (could be empty)
    */
    public Set getServiceMembers();

    /**
    * Return a String with Service version for the specified
    * service Member.
    *
    * @param member member of the service
    *
    * @return service version or null if the specified member
    *         does not run this service.
    *
    * @exception IllegalStateException thrown if the service
    *            is not running or has stopped
    */
    public String getServiceVersion(Member member);

    /**
    * Return the "most senior" Member that is running this Service.
    *
    * @return the senior Member
    *
    * @since Coherence 2.0
    */
    public Member getOldestMember();

    /**
    * Return the Member with the specified mini-id that is running
    * this Service.
    *
    * @param  nId  the member mini-id
    *
    * @return the Member with the specified id or null
    *
    * @see Member#getId()
    * @since Coherence 2.4
    */
    public Member getServiceMember(int nId);
    }