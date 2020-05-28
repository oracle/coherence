/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.ClusterNameSupplier;
import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.util.Filter;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.net.URI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The clusters resource is the base resource for anything Coherence specific. All the resources wrt Coherence
 * Management API will be a child of the clusters API.
 */
public class ClustersResource
        extends AbstractManagementResource
    {
    ClustersResource(AbstractManagementResource resource, ClusterNameSupplier supplierClusters)
        {
        super(resource);
        f_supplierClusters = supplierClusters;
        setDomainPartitionFilter(createDomainPartitionFilter());
        }

    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        return response(getClustersResponse());
        }

    @POST
    @Produces(MEDIA_TYPES)
    @Path(SEARCH)
    public Response search(Map<String, Object> entity)
        {
        if (entity == null)
            {
            entity = Collections.emptyMap();
            }
        return response(getSearchResponse(entity));
        }

    @Path("{" + CLUSTER_NAME + "}")
    public Object getClusterResource(@PathParam(CLUSTER_NAME) String clusterName)
        {
        ClusterResource resource = new ClusterResource(this);
        resource.setClusterName(clusterName);
        return resource;
        }

    protected EntityMBeanResponse getClustersResponse()
        {

        Set<String>               setCluster = getRunningCoherenceClusterNames();
        Filter<String>            filter     = getLinksFilter();
        EntityMBeanResponse       response   = new EntityMBeanResponse(getRequestContext(), filter);
        List<Map<String, Object>> list       = setCluster.stream()
                                                   .map(this::getJson)
                                                   .collect(Collectors.toList());

        response.addParentResourceLink(getParentUri());
        response.addSelfResourceLinks(getCurrentUri());
        response.setEntities(list);

        return response;
        }

    private EntityMBeanResponse getSearchResponse(Map<String, Object> entity)
        {
        Filter<String>            filter     = getLinksFilter();
        EntityMBeanResponse       response   = new EntityMBeanResponse(getRequestContext(), filter);
        Set<String>               setCluster = getRunningCoherenceClusterNames();
        List<Map<String, Object>> items      = new ArrayList<>();

        for (String cluster : setCluster)
            {
            items.add(getSearchResponseObject(cluster, entity));
            }
        response.setEntities(items);
        return response;
        }

    private Set<String> getRunningCoherenceClusterNames()
        {
        return f_supplierClusters.get();
        }


    private Map<String, Object> getJson(String clusterName)
        {
        ClusterResource resource = new ClusterResource(this);
        resource.setClusterName(clusterName);

        // we need to pass the parent URI as current uri(which is /clusters)
        return resource.getClusterResponseMap(getCurrentUri(), clusterName);
        }

    private Map<String, Object> getSearchResponseObject(String clusterName, Map<String, Object> entity)
        {
        ClusterResource resource = new ClusterResource(this);
        resource.setClusterName(clusterName);
        // we need to pass the parent URI as current uri(which is /clusters)
        URI parentUri = getParentUri();
        return resource.getSearchResults(new LinkedHashMap<>(entity), parentUri, getSubUri(parentUri, clusterName));
        }

    private Filter<String> createDomainPartitionFilter()
        {
        String sDomainPartitionName = m_uriInfo.getQueryParameters().getFirst(DOMAIN_PARTITION);
        return (sDomainPartitionName == null)
               ? null : s -> s.equals(sDomainPartitionName);
        }

    // ----- data members ---------------------------------------------------

    private final ClusterNameSupplier f_supplierClusters;
    }
