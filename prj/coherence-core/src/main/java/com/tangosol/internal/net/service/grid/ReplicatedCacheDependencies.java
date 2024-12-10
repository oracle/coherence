/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

/**
* The ReplicateCacheDependencies interface provides a ReplicatedCache with its external
* dependencies.
*
* @author pfm  2011.07.07
* @since Coherence 12.1.2
*/
public interface ReplicatedCacheDependencies
        extends GridDependencies, LeaseConfig
    {
    /**
     * Return the ensure-cache timeout.
     *
     * @return the ensure-cache timeout
     */
    public long getEnsureCacheTimeoutMillis();

    /**
     * Return the graveyard size of the lease map.
     *
     * @return the graveyard size
     */
    public int getGraveyardSize();

    /**
     * Return the mobile-issues flag which specifies whether leases are issued
     * to the first requesting member.  If false, the senior service member
     * serves as the issuer for all new leases.
     *
     * NOTE: this is for compatibility with Coherence pre-3.6 lease behavior.
     *
     * @return the mobile-issues flag
     */
    public boolean isMobileIssues();
    }
