/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal;

import com.tangosol.internal.management.ManagementRoutes;

import javax.ws.rs.Path;

/**
 * A JAX-RS root resource that supports management info requests.
 *
 * @author hr  2016.07.12
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
@Path("{a:mgmt|management}/coherence")
public class ManagementRootResource
        extends BaseManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a root management resource.
     */
    public ManagementRootResource()
        {
        super(new ManagementRoutes());
        }
    }
