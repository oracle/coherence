/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;


import com.tangosol.internal.net.service.peer.initiator.InitiatorDependencies;
import com.tangosol.net.ServiceDependencies;


/**
 * The RemoteServiceDependencies interface provides a RemoteService with its external
 * dependencies.
 *
 * @author pfm  2011.09.05
 * @since Coherence 12.1.2
 */
public interface RemoteServiceDependencies
        extends ServiceDependencies
    {
    /**
     * Return the name of the remote Cluster to which this RemoteService will connect.
     *
     * @return the cluster name, or null if unknown
     */
    public String getRemoteClusterName();

    /**
     * Return the name of the remote service to which this RemoteService will connect.
     *
     * @return the remote service name, or null
     */
    public String getRemoteServiceName();

    /**
     * Return the InitiatorDependencies for the Initiator used by the RemoteService.
     *
     * @return the InitiatorDependencies
     */
    public InitiatorDependencies getInitiatorDependencies();
    }
