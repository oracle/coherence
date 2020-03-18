/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;
import com.tangosol.coherence.management.internal.ClusterNameSupplier;
import com.tangosol.coherence.management.internal.VersionUtils;
import com.tangosol.util.Filter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;

/**
 * <p>The version resource contains information about the latest version of the
 * Coherence management REST interface that is supported by the WLS domain.</p>
 * <p>For information about versions supported by the REST interface,
 * see {@xref version_indicator}.</p>
 */
public class VersionResource
        extends AbstractManagementResource
    {
    public VersionResource(AbstractManagementResource resource, ClusterNameSupplier supplier)
        {
        super(resource);
        f_supplierClusters = supplier;
        }

    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        return response(getResponseBody());
        }

    @Path("/clusters")
    public Object getClustersResource()
        {
        return new ClustersResource(this, f_supplierClusters);
        }

    protected EntityMBeanResponse getResponseBody()
        {
        Filter<String> propertiesFilter = getAttributesFilter();
        Filter<String> linksFilter = getLinksFilter();
        EntityMBeanResponse rb = new EntityMBeanResponse(getRequestContext(), linksFilter);
        rb.addParentResourceLink(getParentUri());
        rb.addSelfResourceLinks(getCurrentUri());
        // don't add links for the fanout resources since they're internal resources we don't want customers to know about
        rb.setEntity(new LinkedHashMap<>(VersionUtils.getVersion(VersionUtils.V1, true, VersionUtils.ACTIVE, propertiesFilter)));
        return rb;
        }

    // ----- data members ---------------------------------------------------

    private final ClusterNameSupplier f_supplierClusters;
    }
