/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import java.util.Set;

/**
* A DistributedCacheService is a clustered cache service that partitions its
* data across cluster members that provide backing storage.
*
* @author cp  2003.10.01
*
* @since Coherence 2.3
*/
public interface DistributedCacheService
        extends CacheService, PartitionedService
    {
    /**
    * Determine if local storage is enabled on this member.
    *
    * @return true if local storage is enabled on this member; false
    *         otherwise
    */
    public boolean isLocalStorageEnabled();

    /**
    * Return a Set of Member objects, one for each Member that
    * has registered this Service and has local storage enabled.
    *
    * @return a set of Member objects that provide local storage
    *        for this distributed cache service
    *
    * @see ServiceInfo#getServiceMembers()
    *
    * @deprecated  As of Coherence 3.6, replaced by
    *              {@link #getOwnershipEnabledMembers()}.
    */
    public Set getStorageEnabledMembers();
    }