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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * The versions resource contains information about the versions of the Coherence management REST interface that are
 * active and supported in the current the WLS domain.
 * </p>
 * <p>
 * For information about versions supported by the REST interface, see {@xref version_indicator}.
 * </p>
 */
@Path("/coherence")
public class VersionsResource
        extends AbstractManagementResource
    {
    /**
     * The GET method on this resource returns information about each supported version of this REST interface.
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        return response(getResponseBody());
        }

    private EntityMBeanResponse getResponseBody()
        {
        Filter<String>            propertiesFilter = getAttributesFilter();
        Filter<String>            linksFilter      = getLinksFilter();
        EntityMBeanResponse       response         = new EntityMBeanResponse(getRequestContext(), linksFilter);
        List<Map<String, Object>> items            = new ArrayList<>();

        response.addSelfResourceLinks(getCurrentUri());
        response.addResourceLink("current", getSubUri(getCurrentUri(), VersionUtils.V1));

        items.add(getVersion(VersionUtils.V1, propertiesFilter, linksFilter));

        response.setEntities(items);

        return response;
        }

    private Map<String, Object> getVersion(String sVersion, Filter<String> filterProps, Filter<String> filterLinks)
        {
        EntityMBeanResponse response = new EntityMBeanResponse(getRequestContext(), filterLinks);

        response.setEntity(new LinkedHashMap<>(VersionUtils.getVersion(sVersion, true, VersionUtils.ACTIVE, filterProps)));
        response.addSelfResourceLinks(getSubUri(getCurrentUri(), sVersion));

        return response.toJson();
        }

    @Path(VersionUtils.V1)
    public Object getV1Resource()
        {
        return new VersionResource(this, f_supplierClusters);
        }

    @Path(VersionUtils.LATEST)
    public Object getLatestResource()
        {
        return new VersionResource(this, f_supplierClusters);
        }

    // ----- data members ---------------------------------------------------

    @Context
    protected ClusterNameSupplier f_supplierClusters;
    }
