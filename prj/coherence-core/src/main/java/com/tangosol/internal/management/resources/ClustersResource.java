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

import com.tangosol.util.Filter;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;

import java.net.URI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.function.Supplier;

import java.util.stream.Collectors;

import static com.tangosol.util.BuilderHelper.using;

/**
 * The {@link ClustersResource} is the base resource for anything Coherence specific in a multi-cluster environment,
 * for example in WebLogic Managed Coherence.
 * All the resources wrt Coherence Management API will be a child of the clusters API.
 *
 * @author Jonathan Knight  2022.01.25
 */
public class ClustersResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    public ClustersResource(Supplier<Set<String>> supplierClusters)
        {
        f_supplierClusters = supplierClusters;
        f_clusterResource  = new ClusterResource();
        }

    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addPost(sPathRoot + "/" + SEARCH, this::search);

        router.addRoutes(sPathRoot + "/{" + CLUSTER_NAME + "}", f_clusterResource);
        }

    // ----- Clusters API ---------------------------------------------------

    /**
     * Return the attributes of all the cluster MBeans.
     *
     * @param request  the http request
     *
     * @return the attributes of all the cluster MBeans
     */
    public Response get(HttpRequest request)
        {
        Set<String>               setCluster    = f_supplierClusters.get();
        Filter<String>            filter        = getLinksFilter(request);
        EntityMBeanResponse       mBeanResponse = new EntityMBeanResponse(request, filter);
        List<Map<String, Object>> list          = setCluster.stream()
                                                   .map(sName -> getJson(sName, request))
                                                   .collect(Collectors.toList());

        mBeanResponse.addParentResourceLink(getParentUri(request));
        mBeanResponse.addSelfResourceLinks(getCurrentUri(request));
        mBeanResponse.setEntities(list);

        return response(mBeanResponse);
        }

    /**
     * Search the cluster MBeans.
     *
     * @param request  the http request
     *
     * @return the search results
     */
    public Response search(HttpRequest request)
        {
        Map<String, Object> entity = getJsonBody(request);
        if (entity == null)
            {
            entity = Collections.emptyMap();
            }

        Filter<String>            filter        = getLinksFilter(request);
        EntityMBeanResponse       mBeanResponse = new EntityMBeanResponse(request, filter);
        Set<String>               setCluster    = f_supplierClusters.get();
        ClusterResource           resource      = new ClusterResource();
        List<Map<String, Object>> items         = new ArrayList<>();
        URI                       parentUri     = getParentUri(request);
        URI                       currentUri    = getCurrentUri(request);
        ResourceRegistry          registry      = request.getResourceRegistry();

        for (String sCluster : setCluster)
            {
            registry.registerResource(String.class, CLUSTER_NAME, using(sCluster), RegistrationBehavior.REPLACE, null);

            URI                 subUri     = getSubUri(parentUri, sCluster);
            Map<String, Object> mapQuery   = new LinkedHashMap<>(entity);
            Map<String, Object> mapCluster = resource.getSearchResults(request, sCluster, mapQuery, parentUri, subUri, currentUri);

            items.add(mapCluster);
            }
        mBeanResponse.setEntities(items);

        return response(mBeanResponse);
        }

    // ----- helper methods -------------------------------------------------

    private Map<String, Object> getJson(String clusterName, HttpRequest request)
        {
        request.getResourceRegistry().registerResource(String.class, CLUSTER_NAME,
                using(clusterName), RegistrationBehavior.REPLACE, null);
        
        return f_clusterResource.getClusterResponseMap(request, getCurrentUri(request), clusterName);
        }

    // ----- data members ---------------------------------------------------

    /**
     * A supplier that provides the set of cluster names.
     */
    private final Supplier<Set<String>> f_supplierClusters;

    /**
     * The {@link ClusterResource} to use to obtain cluster responses.
     */
    private final ClusterResource f_clusterResource;
    }
