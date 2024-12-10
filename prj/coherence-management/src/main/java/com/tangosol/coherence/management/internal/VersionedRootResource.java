/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal;

import com.tangosol.internal.management.ManagementRoutes;
import org.glassfish.jersey.server.ContainerRequest;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A JAX-RS root resource that supports versioned management info requests.
 *
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
@Path(VersionedRootResource.PATH_ROOT)
public class VersionedRootResource
        extends BaseManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a root management resource.
     */
    public VersionedRootResource(@Context ContainerRequest containerRequest, @Context ClusterNameSupplier supplier)
        {
        super(getRoutes(containerRequest, supplier));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns the {@link ManagementRoutes} for a given path root, creating
     * a {@link ManagementRoutes} if required.
     *
     * @param request   the request to obtain a {@link ManagementRoutes} for
     * @param supplier  the supplier of cluster names
     *
     * @return the {@link ManagementRoutes} for the given path root
     */
    private static ManagementRoutes getRoutes(ContainerRequest request, ClusterNameSupplier supplier)
        {
        String sPath  = request.getUriInfo().getRequestUri().getPath();
        String sFind  = "/" + PATH_ROOT;
        int    nIndex = sPath.indexOf(sFind);
        String sRoot  = sPath.substring(0, nIndex) + sFind;
        return s_mapRoutes.computeIfAbsent(sRoot, k -> new ManagementRoutes("", true, supplier, sRoot));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The path for this resource.
     */
    public static final String PATH_ROOT = "coherence";

    // ----- data members ---------------------------------------------------

    /**
     * A map of URI path roots to {@link ManagementRoutes} instances.
     */
    private static final Map<String,ManagementRoutes> s_mapRoutes = new ConcurrentHashMap<>();
    }
