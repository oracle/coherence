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

import java.util.LinkedHashMap;
import java.util.Set;

import java.util.function.Supplier;

/**
 * <p>The version resource contains information about the latest version of the
 * Coherence management REST interface that is supported by the WLS domain.</p>
 * <p>For information about versions supported by the REST interface,
 * see {@code version_indicator}.</p>
 *
 * @author Jonathan Knight  2022.01.25
 */
public class VersionResource
        extends AbstractManagementResource
    {
    public VersionResource(Supplier<Set<String>> supplier)
        {
        f_supplierClusters = supplier;
        }

    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        router.addRoutes(sPathRoot + "/clusters", new ClustersResource(f_supplierClusters));
        }

    // ----- Version API ----------------------------------------------------

    public Response get(HttpRequest request)
        {
        String              sIncludeFields   = request.getFirstQueryParameter(INCLUDE_FIELDS);
        String              sExcludeFields   = request.getFirstQueryParameter(EXCLUDE_FIELDS);
        Filter<String>      propertiesFilter = getAttributesFilter(sIncludeFields, sExcludeFields);
        Filter<String>      linksFilter      = getLinksFilter(request);
        EntityMBeanResponse mBeanResponse    = new EntityMBeanResponse(request, linksFilter);

        mBeanResponse.addParentResourceLink(getParentUri(request));
        mBeanResponse.addSelfResourceLinks(getCurrentUri(request));
        // don't add links for the fanout resources since they're internal resources we don't want customers to know about
        mBeanResponse.setEntity(new LinkedHashMap<>(VersionUtils.getVersion(VersionUtils.V1, true, VersionUtils.ACTIVE, propertiesFilter)));

        return response(mBeanResponse);
        }

    // ----- data members ---------------------------------------------------

    private final Supplier<Set<String>> f_supplierClusters;
    }
