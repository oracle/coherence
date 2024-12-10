/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;
import com.tangosol.internal.management.Converter;
import com.tangosol.internal.management.EntityMBeanResponse;
import java.net.URI;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;
import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.AlwaysFilter;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Handles management API requests for a topic in a service.
 */
public class TopicResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addGet(sPathRoot + "/" + CHANNELS, this::getChannelsResponse);

        router.addPost(sPathRoot + "/disconnectAll", this::disconnectAll);

        // child resources
        router.addRoutes(sPathRoot + "/" + MEMBERS, new TopicMembersResource());
        router.addRoutes(sPathRoot + "/" + SUBSCRIBER_GROUPS, new SubscriberGroupsResource());
        router.addRoutes(sPathRoot + "/" + SUBSCRIBER_GROUPS_LCASE, new SubscriberGroupsResource());
        router.addRoutes(sPathRoot + "/" + SUBSCRIBERS, new SubscribersResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the aggregated metrics of PagedTopicMBean's for a single topic belonging to a Service.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String       sTopicName     = request.getFirstPathParameter(TOPIC_NAME);
        String       sRoleName      = request.getFirstQueryParameter(ROLE_NAME);
        String       sCollector     = request.getFirstQueryParameter(COLLECTOR);
        String       sServiceName   = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder   = getQuery(request, sTopicName, sServiceName);
        Set<String>  setObjectNames = ensureBeanAccessor(request).queryKeys(queryBuilder.build());

        if (setObjectNames == null || setObjectNames.isEmpty())
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        EntityMBeanResponse responseEntity = getResponseEntityForMbean(request,
                                                                       queryBuilder,
                                                                       getParentUri(request),
                                                                       getCurrentUri(request),
                                                                       getAttributesFilter(request),
                                                                       getLinksFilter(request),
                                                                       CHILD_LINKS);

        if (responseEntity == null) // if there is no entity, the topic could not be found
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        addObjectNamesToResponse(request, setObjectNames, responseEntity);

        Map<String, Object> responseMap = responseEntity.toJson();
        addAggregatedMetricsToResponseMap(request, sRoleName, sCollector, queryBuilder, responseMap);
        return response(responseMap);
        }

    // ----- POST API -------------------------------------------------------

    /**
     * Call "DisconnectAll" operation on PagedTopic MBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response disconnectAll(HttpRequest request)
        {
        String       sTopicName   = request.getFirstPathParameter(TOPIC_NAME);
        String       sServiceName = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder = getQuery(request, sTopicName, sServiceName);
        return executeMBeanOperation(request,
                                     queryBuilder,
                                     "disconnectAll",
                                     null,
                                     null);
        }

    /**
     * Return the list of a topic channels.
     *
     * @return the response object
     */
    public Response getChannelsResponse(HttpRequest request)
        {
        String       sTopicName    = request.getFirstPathParameter(TOPIC_NAME);
        String       sServiceName  = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder  = getQuery(request, sTopicName, sServiceName);

        EntityMBeanResponse response = getResponseBodyForMBeanCollection(request,
                                                                         queryBuilder,
                                                                         Map.of(INCLUDE_FIELDS,  List.of(CHANNELS, NODE_ID)),
                                                                         getParentUri(request),
                                                                         getCurrentUri(request));
        return response(response);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String sTopicName = MBeanHelper.safeUnquote(mapArguments.get(NAME));

        // collect attributes from the ObjectNames
        URI            uriSelf        = getSubUri(uriParent, sTopicName);
        Filter<String> filterLinks    = getLinksFilter(request, mapQuery);
        String         sServiceName   = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder   queryBuilder   = getQuery(request, sTopicName, sServiceName);
        Set<String>    setObjectNames = ensureBeanAccessor(request).queryKeys(queryBuilder.build());

        if (setObjectNames == null || setObjectNames.isEmpty())
            {
            return null;
            }

        EntityMBeanResponse response = getResponseEntityForMbean(request,
                                                                 queryBuilder,
                                                                 uriParent,
                                                                 uriSelf,
                                                                 getAttributesFilter(request),
                                                                 filterLinks,
                                                                 CHILD_LINKS);

        addObjectNamesToResponse(request, setObjectNames, response);
        Map<String, Object> mapEntity = response.getEntity();
        addAggregatedMetricsToResponseMap(request, "*", null, queryBuilder, mapEntity);
        return response;
        }

    // ----- PagedTopicResource methods-------------------------------------------

    /**
     * MBean query to retrieve PagedTopicMBeans for the provided topic.
     *
     * @param request       the request
     * @param sTopicName    the topic name
     * @param sServiceName  the service name
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(HttpRequest request, String sTopicName, String sServiceName)
        {
        return createQueryBuilder(request)
                .withBaseQuery(TOPIC_QUERY + sTopicName)
                .withService(sServiceName);
        }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void addObjectNamesToResponse(HttpRequest         request,
                                            Set<String>         setObjectNames,
                                            EntityMBeanResponse responseEntity)
        {
        Filter<String>      filterAttributes = getAttributesFilter(request);
        Map<String, Object> mapAttributes    = new LinkedHashMap<>();

        // return name, service, and node_id if no field is specified
        if (filterAttributes instanceof AlwaysFilter)
            {
            filterAttributes = getAttributesFilter(String.join(",", NAME, SERVICE, NODE_ID, TYPE), null);
            }

        for (String sName : setObjectNames)
            {
            try
                {
                ObjectName objectName = new ObjectName(sName);
                for (String sKey : objectName.getKeyPropertyList().keySet())
                    {
                    if (filterAttributes.evaluate(sKey))
                        {
                        Object oValue   = Converter.convert(objectName.getKeyProperty(sKey));
                        Object oCurrent = mapAttributes.get(sKey);

                        if (oCurrent == null)
                            {
                            mapAttributes.put(sKey, oValue);
                            }
                        else if (oCurrent instanceof Set)
                            {
                            ((Set) oCurrent).add(oValue);
                            }
                        else if (!Objects.equals(oCurrent, oValue))
                            {
                            Set values = new HashSet<>();
                            values.add(oCurrent);
                            values.add(oValue);
                            mapAttributes.put(sKey, values);
                            }
                        }
                    }
                }
            catch (MalformedObjectNameException e)
                {
                Base.log("Exception occurred while creating an ObjectName " +
                         sName + "\n" + Base.getStackTrace(e));
                }
            }

        responseEntity.setEntity(mapAttributes);
        }

    public static String[] CHILD_LINKS = {MEMBERS, CHANNELS, SUBSCRIBERS, SUBSCRIBER_GROUPS};
    }
