/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.util.Base;

/**
 * An abstract base for ServiceLoadBalancer implementations.
 *
 * @author jh  2010.12,07
 */
public abstract class AbstractServiceLoadBalancer<S extends Service, T extends ServiceLoad>
        extends    Base
        implements ServiceLoadBalancer<S, T>
    {
    // ----- ServiceLoadBalancer interface ----------------------------------

    /**
     * {@inheritDoc}
     */
    public void init(S service)
        {
        m_service = service;
        }


    // ----- helper methods -------------------------------------------------

    /**
     * Return the Member object representing the local cluster member.
     *
     * @return the Member object representing the local cluster member
     */
    protected Member getLocalMember()
        {
        return getService().getCluster().getLocalMember();
        }

    /**
     * Check whether the specified Member object represents the local member
     * of the cluster.
     *
     * @param member  the Member object in question
     *
     * @return true iff the Member object represents the local cluster member
     */
    protected boolean isLocalMember(Member member)
        {
        try
            {
            return member == null || member.equals(getLocalMember());
            }
        catch (RuntimeException e)
            {
            // the local services are stopped
            // there is no good answer...
            return true;
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Obtain the containing Service.
     *
     * @return the containing Service
     */
    public S getService()
        {
        return m_service;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The containing Service.
     */
    protected S m_service;
    }
