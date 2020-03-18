/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import javax.ws.rs.Path;

/**
 * A JAX-RS root resource that supports management info requests.
 *
 * @author hr  2016.07.12
 * @since 12.2.1.4.0
 */
@Path("{a:mgmt|management}/coherence")
public class ManagementRootResource
        extends AbstractManagementResource
    {
    // ----- jax-rs handlers ------------------------------------------------
    /**
     * Return a resource that can handle cluster management(MBean) requests.
     *
     * @return a resource that can handle cluster management(MBean) requests
     */
    @Path(CLUSTER)
    public ClusterResource getClusterResource()
        {
        return new ClusterResource(this);
        }
    }
