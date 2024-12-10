/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.runtime.coherence.callables;


import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.Service;

/**
 * A {@link RemoteCallable} to determine whether a service is suspended.
 *
 * @author Jonathan Knight 2022.04.26
 * @since 22.06
 */
public class IsServiceSuspended
        implements RemoteCallable<Boolean>
    {
    /**
     * Create an {@link IsServiceSuspended} callable.
     *
     * @param sName  the name of the service to test
     */
    public IsServiceSuspended(String sName)
        {
        m_sName = sName;
        }

    // ----- RemoteCallable methods -----------------------------------------

    @Override
    public Boolean call()
        {
        Service service = CacheFactory.getCluster().getService(m_sName);
        return service != null && service.isSuspended();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the service.
     */
    private final String m_sName;
    }
