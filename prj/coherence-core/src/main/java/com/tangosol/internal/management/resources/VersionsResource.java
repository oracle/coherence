/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;

import com.tangosol.internal.management.EntityMBeanResponse;
import com.tangosol.internal.management.VersionUtils;

import com.tangosol.util.Filter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.function.Supplier;

/**
 * <p>
 * The versions resource contains information about the versions of the Coherence management REST interface that are
 * active and supported in the current the WLS domain.
 * </p>
 * <p>
 * For information about versions supported by the REST interface, see {@code version_indicator}.
 * </p>
 *
 * @author Jonathan Knight  2022.01.25
 */
public class VersionsResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    public VersionsResource(Supplier<Set<String>> f_supplierClusters)
        {
        this.f_supplierClusters = f_supplierClusters;
        }

    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot + "", this::get);

        router.addRoutes(sPathRoot + "/" + VersionUtils.V1, new VersionResource(f_supplierClusters));
        router.addRoutes(sPathRoot + "/" + VersionUtils.LATEST, new VersionResource(f_supplierClusters));
        }

    // ----- Versions API ---------------------------------------------------

    /**
     * The GET method on this resource returns information about each supported version of this REST interface.
     */
    public Response get(HttpRequest request)
        {
        String                    sIncludeFields   = request.getFirstQueryParameter(INCLUDE_FIELDS);
        String                    sExcludeFields   = request.getFirstQueryParameter(EXCLUDE_FIELDS);
        Filter<String>            propertiesFilter = getAttributesFilter(sIncludeFields, sExcludeFields);
        Filter<String>            linksFilter      = getLinksFilter(request);
        EntityMBeanResponse       mBeanResponse    = new EntityMBeanResponse(request, linksFilter);
        List<Map<String, Object>> items            = new ArrayList<>();

        mBeanResponse.addSelfResourceLinks(getCurrentUri(request));
        mBeanResponse.addResourceLink("current", getSubUri(getCurrentUri(request), VersionUtils.V1));

        items.add(getVersion(request, VersionUtils.V1, propertiesFilter, linksFilter));

        mBeanResponse.setEntities(items);

        return response(mBeanResponse);
        }

    // ----- helper methods -------------------------------------------------

    private Map<String, Object> getVersion(HttpRequest request, String sVersion, Filter<String> filterProps, Filter<String> filterLinks)
        {
        EntityMBeanResponse response = new EntityMBeanResponse(request, filterLinks);

        response.setEntity(new LinkedHashMap<>(VersionUtils.getVersion(sVersion, true, VersionUtils.ACTIVE, filterProps)));
        response.addSelfResourceLinks(getSubUri(getCurrentUri(request), sVersion));

        return response.toJson();
        }

    // ----- data members ---------------------------------------------------

    private final Supplier<Set<String>> f_supplierClusters;
    }
