/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.Converter;
import com.tangosol.coherence.management.internal.EntityMBeanResponse;
import com.tangosol.coherence.management.internal.MBeanResponse;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.management.MBeanAccessor;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.ValueExtractor;

import java.net.URI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import javax.ws.rs.WebApplicationException;

import javax.ws.rs.container.ContainerRequestContext;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import static com.tangosol.net.CacheFactory.getStackTrace;
import static com.tangosol.net.CacheFactory.log;

/**
 * The base resource for Coherence management resources.
 *
 * @author sr  2017.08.21
 * @since 12.2.1.4
 */
public abstract class AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public AbstractManagementResource()
        {
        }

    /**
     * Construct a AbstractManagementResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public AbstractManagementResource(AbstractManagementResource resource)
        {
        m_requestContext        = resource.m_requestContext;
        m_requestHeaders        = resource.m_requestHeaders;
        m_uriInfo               = resource.m_uriInfo;
        m_mBeanServerProxy      = resource.m_mBeanServerProxy;
        m_filterDomainPartition = resource.m_filterDomainPartition;
        m_sClusterName          = resource.m_sClusterName;
        m_sMBeanDomainName      = resource.m_sMBeanDomainName;
        if (m_sMBeanDomainName == null)
            {
            m_sMBeanDomainName = "*";
            }

        }

    // ----- AbstractManagementResource methods -----------------------------

    /**
     * Update the attributes of an MBean, with the provided json entity
     *
     * @param entity     the json object which contains the keys to be updated
     * @param bldrQuery  the {@link QueryBuilder} to be used to generate MBean query
     *
     * @return the Response object
     */
    protected Response update(Map<String, Object> entity, QueryBuilder bldrQuery)
        {
        try
            {
            if (bldrQuery.toString().contains(MANAGEMENT_QUERY))
                {
                checkAttributeTypeConversion(entity, true);
                }
            else
                {
                checkAttributeTypeConversion(entity);
                }

            MBeanResponse responseEntity = new MBeanResponse(getRequestContext());
            MBeanAccessor accessor = getMBeanAccessor();

            if (!entity.isEmpty())
                {
                Map<String, Object> attrMap = entity.entrySet()
                        .stream()
                        .collect(Collectors.toMap(e -> fromRestName(e.getKey()), Map.Entry::getValue));

                Map<String, Map<String, Object>> mapUpdatedMBeans = accessor.update(bldrQuery.build(), attrMap);

                if (mapUpdatedMBeans.isEmpty())
                    {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                    }

                for (Map.Entry<String,  Map<String, Object>> entry : mapUpdatedMBeans.entrySet())
                    {
                    String              sObjName      = entry.getKey();
                    Map<String, Object> mapAttributes = entry.getValue();

                    for (String sAttrKey : attrMap.keySet())
                        {
                        if (!mapAttributes.containsKey(sAttrKey))
                            {
                            responseEntity.addFailure(getRestName(sAttrKey),
                                    "Update attribute failed for MBean :" + sObjName);
                            }
                        }
                    }
                }
            return response(responseEntity.toJson());
            }
        catch (Exception e)
            {
            CacheFactory.log("Exception occurred while updating an MBean with query "
                + bldrQuery.toString() + ", " + e + '\n'
                + Base.getStackTrace(e), CacheFactory.LOG_ERR);

            if (e instanceof WebApplicationException)
                {
                throw e;
                }

            // internal server error
            throw new WebApplicationException();
            }
        }

    /**
     * Calculate the response body required for a single MBean.
     *
     * @param bldrQuery     the {@link QueryBuilder} to be used to generate MBean query
     * @param asChildLinks  the child links to be added to the response
     *
     * @return the entity response for the MBean
     */
    protected EntityMBeanResponse getResponseEntityForMbean(QueryBuilder bldrQuery,
                                                            String...    asChildLinks)
        {
        return getResponseEntityForMbean(bldrQuery, getParentUri(), getCurrentUri(),
                getAttributesFilter(), getLinksFilter(),  asChildLinks);
        }

    /**
     * Calculate the response body required for a single MBean.
     *
     * @param bldrQuery     the {@link QueryBuilder} to be used to generate MBean query
     * @param uriParent     the parent URI of the request, the parent URI is one level up
     *                      from the current resource
     * @param uriSelf       the self URI of the Mbean
     * @param mapQuery      the object query map, query map contains which child elements must be returned
     * @param asChildLinks  the child links to be added to the response
     *
     * @return the entity response for the MBean
     */
    protected EntityMBeanResponse getResponseEntityForMbean(QueryBuilder bldrQuery,
                                                            URI          uriParent,
                                                            URI          uriSelf,
                                                            Map          mapQuery,
                                                            String...    asChildLinks)
        {
        return getResponseEntityForMbean(bldrQuery, uriParent, uriSelf, getAttributesFilter(mapQuery),
                getLinksFilter(mapQuery), asChildLinks);
        }

    /**
     * Calculate the response body for a single MBean.
     *
     * @param bldrQuery         the {@link QueryBuilder} to be used to generate MBean query
     * @param uriParent         the parent URI of the resource
     * @param uriSelf           the self URI of the Mbean
     * @param filterAttributes  the attributes filter
     * @param filterLinks       the links filter
     * @param asChildLinks      the child links to be added to the response
     *
     * @return the entity response for the MBean
     */
    protected EntityMBeanResponse getResponseEntityForMbean(QueryBuilder   bldrQuery,
                                                            URI            uriParent,
                                                            URI            uriSelf,
                                                            Filter<String> filterAttributes,
                                                            Filter<String> filterLinks,
                                                            String...      asChildLinks)
        {
        try
            {
            EntityMBeanResponse responseEntity = createResponse(uriParent, uriSelf, filterLinks);
            MBeanAccessor       accessor       = getMBeanAccessor();

            Map<String, Map<String, Object>> mapResponses = accessor.getAttributes(bldrQuery.build());

            if (!mapResponses.isEmpty())
                {
                Map.Entry<String, Map<String, Object>> responseEntry = mapResponses.entrySet().iterator().next();
                responseEntity.setEntity(getMBeanAttributesMap(filterAttributes, responseEntry.getValue(),
                        responseEntry.getKey()));
                }
            else
                {
                return null;
                }

            if (asChildLinks != null)
                {
                Arrays.stream(asChildLinks).forEach(l -> responseEntity.addResourceLink(l, getSubUri(uriSelf, l)));
                }

            return responseEntity;
            }
        catch (Exception e)
            {
            log("Exception occurred while getting response body for MBean with query "
                    + bldrQuery.build() + "\n" + getStackTrace(e), CacheFactory.LOG_ERR);
            if (e instanceof WebApplicationException)
                {
                throw (WebApplicationException) e;
                }

            // internal server error, also do not propagate the error
            throw new WebApplicationException();
            }
        }

    /**
     * Execute an MBean operation with the provided parameters.
     *
     * @param bldrQuery       the {@link QueryBuilder} to be used to generate MBean query
     * @param sOperationName  the name of the operation
     * @param aoArguments     the arguments of the operation
     * @param asSignature     the signature for the operation
     *
     * @return the Response to be sent
     */
    protected Response executeMBeanOperation(QueryBuilder  bldrQuery,
                                             String        sOperationName,
                                             Object[]      aoArguments,
                                             String[]      asSignature)
        {
        try
            {
            MBeanAccessor accessor = getMBeanAccessor();

            Map<String, Object> mapMBeans = accessor.invoke(bldrQuery.build(), sOperationName, aoArguments, asSignature);

            if (mapMBeans.isEmpty())
                {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
                }
            }
        catch (RuntimeException e)
            {
            log("Exception occurred while executing an Mbean operation on query "
                            + bldrQuery.build() + ", and operationName " + sOperationName + "\n" + getStackTrace(e), CacheFactory.LOG_ERR);

            if (e instanceof WebApplicationException)
                {
                throw (WebApplicationException) e;
                }

            MBeanResponse response = new MBeanResponse(getRequestContext());
            String        sCause   = "unknown, refer to log files for more information";

            // extract the original error message
            Throwable tCause = Base.getOriginalException(e);
            if (tCause != null)
                {
                sCause = Base.getDeepMessage(tCause, "\n");
                }

            response.addFailure(sOperationName + " failed, Cause=" + sCause);
            return Response.status(Response.Status.BAD_REQUEST).entity(response.toJson()).build();
            }
        return response(new MBeanResponse(getRequestContext()).toJson());
        }

    /**
     * Execute an operation in an Mbean and return the response.
     *
     * @param bldrQuery       the {@link QueryBuilder} to be used to generate MBean query
     * @param sResponseKey    the key in the response map to which the result must be put
     * @param sOperationName  the operation name
     *
     * @return the response
     */
    protected EntityMBeanResponse getResponseFromMBeanOperation(QueryBuilder bldrQuery,
                                                                String       sResponseKey,
                                                                String       sOperationName)
        {
        return getResponseFromMBeanOperation(bldrQuery, sResponseKey, sOperationName,
                null, null);
        }

    /**
     * Execute an operation in an MBean and return the response.
     *
     * @param bldrQuery       the {@link QueryBuilder} to be used to generate MBean query
     * @param sResponseKey    the key in the response map to which the result must be put
     * @param sOperationName  the operation name
     * @param aoArguments     the arguments of the Mbean operations
     * @param asSignature     the signature of the MBean operation
     *
     * @return the response
     */
    protected EntityMBeanResponse getResponseFromMBeanOperation(QueryBuilder  bldrQuery,
                                                                String        sResponseKey,
                                                                String        sOperationName,
                                                                Object[]      aoArguments,
                                                                String[]      asSignature)
        {
        URI             uriParent        = getParentUri();
        URI             uriSelf          = getCurrentUri();
        Filter<String>  filterLinks      = getLinksFilter();
        Filter<String>  filterAttributes = getAttributesFilter();

        try
            {
            EntityMBeanResponse responseBody = createResponse(uriParent, uriSelf, filterLinks);
            MBeanAccessor       accessor     = getMBeanAccessor();
            Map<String, Object> mapMBeans    =
                    accessor.invoke(bldrQuery.build(), sOperationName, aoArguments, asSignature);

            if (!mapMBeans.isEmpty())
                {
                // when we are doing MBean operations isn REST with sResponseKey set, then the operation
                // should typically happen on only one MBean. In any case, we return the result
                // of only one MBean
                Map.Entry<String, Object> entry       = mapMBeans.entrySet().iterator().next();
                Map<String, Object>       mapResponse = new LinkedHashMap<>();

                if (filterAttributes.evaluate(sResponseKey.toUpperCase()))
                    {
                    mapResponse.put(getRestName(sResponseKey), Converter.convert(entry.getValue()));
                    }

                responseBody.setEntity(mapResponse);
                }
            return responseBody;
            }
        catch (RuntimeException e)
            {
            log("Exception occurred while executing an Mbean operation on query" + bldrQuery.toString()
                    + "and operationName " + sOperationName + "\n" + getStackTrace(e), CacheFactory.LOG_ERR);

            if (e instanceof WebApplicationException)
                {
                throw e;
                }
            
            EntityMBeanResponse responseBody = createResponse(uriParent, uriSelf, filterLinks);
            String              sCause       = "unknown, refer to log files for more information";

            // extract the original error message
            Throwable tCause = Base.getOriginalException(e);
            if (tCause != null)
                {
                sCause = Base.getDeepMessage(tCause, "\n");
                }

            responseBody.addFailure(sOperationName + " failed, Cause=" + sCause);
            return responseBody;
            }
        }

    /**
     * Generate a response for a collection query. There are cases wherein a list of MBeans needs
     * to be grouped based on a unique key, for example list of services, list of caches etc. In such cases
     * the MBeans are grouped with that unique key property and the response is generated using the child
     * resource class.
     *
     * @param bldrQuery           the {@link QueryBuilder} to be used to generate MBean query
     * @param resource            the resource object to be used to generate response for a individual element
     * @param sUniqueKeyProperty  the unique key property to use, in case multiple objects needs to be
     *                            coalesced into one. For example, there can be multiple Service MBeans
     *                            for the same service.
     * @param mapQuery            the object query map, query map contains which child elements must be returned
     * @param uriParent           the parent URI
     * @param uriSelf             the current URI
     *
     * @return the response for the collection
     */
    protected EntityMBeanResponse getResponseBodyForMBeanCollection(QueryBuilder               bldrQuery,
                                                                    AbstractManagementResource resource,
                                                                    String                     sUniqueKeyProperty,
                                                                    Map                        mapQuery,
                                                                    URI                        uriParent,
                                                                    URI                        uriSelf,
                                                                    Map<String, String>        mapArguments)
        {
        try
            {
            Filter<String>      filterLinks    = getLinksFilter(mapQuery);
            EntityMBeanResponse responseEntity = createResponse(uriParent, uriSelf, filterLinks);
            MBeanAccessor       accessor       = getMBeanAccessor();
            Set<String>         setObjectNames = accessor.queryKeys(bldrQuery.build());

            if (setObjectNames != null && !setObjectNames.isEmpty())
                {
                List<Map<String, Object>> listChildEntities = new ArrayList<>();

                // this is the case where there is a unique property, and the response needs to be fetched from
                // a child resource. For example, while querying for list of services, there can be multiple
                // ServiceMBeans
                // but there is only a single service, similarly CacheMBean as well. the unique property in both
                // theses cases is the name of the service/cache

                Set<ObjectName> setObjNames = convertToObjectNames(setObjectNames);

                Map<String, List<ObjectName>> mapMBeanToName = setObjNames.stream()
                        .collect(Collectors.groupingBy(o -> o.getKeyProperty(sUniqueKeyProperty)));

                for (Map.Entry<String, List<ObjectName>> entry : mapMBeanToName.entrySet())
                    {
                    // propagate the argument map to the child resources after appending the unique key
                    Map<String, String> mapChildResourceArgs = new HashMap<>();
                    if (mapArguments != null)
                        {
                        mapChildResourceArgs.putAll(mapArguments);
                        }

                    mapChildResourceArgs.put(sUniqueKeyProperty, entry.getKey());

                    Map<String, Object> mapObjResponse = resource.getQueryResult(mapQuery,
                                                                                 mapChildResourceArgs,
                                                                                 uriSelf).toJson();

                    listChildEntities.add(mapObjResponse);
                    }
                responseEntity.setEntities(listChildEntities);
                }
            else
                {
                return null;
                }

            return responseEntity;
            }
        catch (Exception e)
            {
            log("Exception occurred while getting response for an MBean collection with query " + bldrQuery.build().getQuery() +
                    "\n" + getStackTrace(e), CacheFactory.LOG_ERR);
            throw new WebApplicationException();
            }
        }

    /**
     * Generate a response for a collection query. For each child MBean, the response is generated
     * for its attributes followed by querying the child resources.
     *
     * @param bldrQuery     the {@link QueryBuilder} to be used to generate MBean query
     * @param resource      the resource object to be used to generate response for a individual element
     * @param mapQuery      the object query map, query map contains which child elements must be returned
     * @param mapArguments  the arguments to the child resource
     * @param uriParent     the parent URI
     * @param uriSelf       the current URI
     *
     * @return the response for the collection
     */
    protected EntityMBeanResponse getResponseBodyForMBeanCollection(QueryBuilder               bldrQuery,
                                                                    AbstractManagementResource resource,
                                                                    Map                        mapQuery,
                                                                    Map<String, String>        mapArguments,
                                                                    URI                        uriParent,
                                                                    URI                        uriSelf)
        {
        try
            {
            Filter<String>                   filterLinks    = getLinksFilter(mapQuery);
            EntityMBeanResponse              responseEntity = createResponse(uriParent, uriSelf, filterLinks);
            MBeanAccessor accessor       = getMBeanAccessor();
            Map<String, Map<String, Object>> mapMBeans      = accessor.getAttributes(bldrQuery.build());

            if (mapMBeans != null && !mapMBeans.isEmpty())
                {
                List<Map<String, Object>> listChildEntities = new ArrayList<>();
                Filter<String>            filterAttributes  = getAttributesFilter(mapQuery);

                for (Map.Entry<String, Map<String, Object>> entry: mapMBeans.entrySet())
                    {
                    String              sObjName = entry.getKey();
                    ObjectName          objName  = new ObjectName(sObjName);

                    // propagate the argument map to the child resources after appending the unique key
                    Map<String, String> mapChildResourceArgs = new HashMap<>();
                    if (mapArguments != null)
                        {
                        mapChildResourceArgs.putAll(mapArguments);
                        }

                    mapChildResourceArgs.put(MEMBER_KEY, getMemberReference(objName));

                    EntityMBeanResponse response = resource.getQueryResult(mapQuery, mapChildResourceArgs, uriSelf);

                    response.getEntity().putAll(getMBeanAttributesMap(
                            filterAttributes, entry.getValue(), entry.getKey()));

                    listChildEntities.add(response.toJson());
                    }

                responseEntity.setEntities(listChildEntities);
                }
            return responseEntity;
            }
        catch (Exception e)
            {
            log("Exception occurred while getting response for an MBean collection with query " +
                    bldrQuery.build() + "\n" + getStackTrace(e), CacheFactory.LOG_ERR);
            throw new WebApplicationException();
            }
        }

    /**
     * Generate a response for a collection query. Examples of collection queries are
     * list of members in a cluster, list of services in a cluster etc.
     *
     * @param bldrQuery           the {@link QueryBuilder} to be used to generate MBean query
     * @param sUniqueKeyProperty  the unique key property, which is used to create the self link for the MBean.
     *                            For example, while querying for the list of federation topologies, each topology MBean
     *                            response will have a self link, and the self link will be {parentUri}/{name}.
     *                            In this case, the unique key property is the name, which is the topology name.
     * @param mapQuery            the object query map, query map contains which child elements must be returned
     * @param uriParent           the parent URI
     * @param uriSelf             the current URI
     *
     * @return the response for the collection
     */
    protected EntityMBeanResponse getResponseBodyForMBeanCollection(QueryBuilder bldrQuery,
                                                                    String       sUniqueKeyProperty,
                                                                    Map          mapQuery,
                                                                    URI          uriParent,
                                                                    URI          uriSelf)
        {
        try
            {
            Filter<String>                   filterLinks    = getLinksFilter(mapQuery);
            EntityMBeanResponse              responseEntity = createResponse(uriParent, uriSelf, filterLinks);
            MBeanAccessor accessor       = getMBeanAccessor();
            Map<String, Map<String, Object>> mapMBeans      = accessor.getAttributes(bldrQuery.build());

            if (mapMBeans != null && !mapMBeans.isEmpty())
                {
                List<Map<String, Object>> listChildEntities = new ArrayList<>();

                // this is the cache where there are no child resource required
                // we just need to send the response for a list of MBeans, for example
                // list of back cache members for a single cache in a member
                Filter<String> filterAttributes = getAttributesFilter(mapQuery);

                for (Map.Entry<String, Map<String, Object>> entry: mapMBeans.entrySet())
                    {
                    String              sObjName      = entry.getKey();
                    Map<String, Object> mapAttributes = entry.getValue();
                    ObjectName          objectName    = new ObjectName(entry.getKey());
                    URI                 uriSub        =
                            getSubUri(uriSelf, objectName.getKeyProperty(sUniqueKeyProperty));

                    EntityMBeanResponse responseEntityChild = createResponse(uriSelf, uriSub, filterLinks);

                    responseEntityChild.setEntity(getMBeanAttributesMap(filterAttributes, mapAttributes, sObjName));

                    listChildEntities.add(responseEntityChild.toJson());
                    }
                responseEntity.setEntities(listChildEntities);
                }

            return responseEntity;
            }
        catch (Exception e)
            {
            log("Exception occurred while getting response for an MBean collection with query " + bldrQuery.build() +
                    getStackTrace(e), CacheFactory.LOG_ERR);
            throw new WebApplicationException();
            }
        }

    /**
     * Generate a response for a collection query. Examples of collection queries are
     * list of members in a cluster, list of services in a cluster etc.
     *
     * @param bldrQuery  the {@link QueryBuilder} to be used to generate MBean query
     * @param mapQuery   the object query map, query map contains which child elements must be returned
     * @param uriParent  the parent URI
     * @param uriSelf    the current URI
     *
     * @return the response for the collection
     */
    protected EntityMBeanResponse getResponseBodyForMBeanCollection(QueryBuilder bldrQuery,
                                                                    Map          mapQuery,
                                                                    URI          uriParent,
                                                                    URI          uriSelf)
        {
        try
            {
            Filter<String>                   filterLinks    = getLinksFilter(mapQuery);
            EntityMBeanResponse              responseEntity = createResponse(uriParent, uriSelf, filterLinks);
            MBeanAccessor accessor       = getMBeanAccessor();
            Map<String, Map<String, Object>> mapMBeans      = accessor.getAttributes(bldrQuery.build());

            if (mapMBeans != null && !mapMBeans.isEmpty())
                {
                List<Map<String, Object>> listChildEntities = new ArrayList<>();

                // this is the cache where there are no child resource required
                // we just need to send the response for a list of MBeans, for example
                // list of back cache members for a single cache in a member
                Filter<String> filterAttributes = getAttributesFilter(mapQuery);

                for (Map.Entry<String, Map<String, Object>> entry: mapMBeans.entrySet())
                    {
                    listChildEntities.add(getMBeanAttributesMap(filterAttributes, entry.getValue(), entry.getKey()));
                    }

                responseEntity.setEntities(listChildEntities);
                }
            return responseEntity;
            }
        catch (Exception e)
            {
            log("Exception occurred while getting response for an MBean collection with query " + bldrQuery.build()
                    + "\n" + getStackTrace(e), CacheFactory.LOG_ERR);
            throw new WebApplicationException();
            }
        }

    /**
     * Generate a response containing only links. Useful in cases where a URL may not have any
     * responses, but can have child URL's.
     *
     * @param uriParent    the parent URI
     * @param uriSelf      the self URI
     * @param filterLinks  the links filter
     * @param aChildLinks  the child links to be added
     *
     * @return the response
     */
    protected EntityMBeanResponse getLinksOnlyResponseBody(URI            uriParent,
                                                           URI            uriSelf,
                                                           Filter<String> filterLinks,
                                                           String...      aChildLinks)
        {
        EntityMBeanResponse responseEntity = createResponse(uriParent, uriSelf, filterLinks);

        if (aChildLinks != null)
            {
            Arrays.stream(aChildLinks).forEach(l -> responseEntity.addResourceLink(l, getSubUri(uriSelf, l)));
            }

        return responseEntity;
        }

    /**
     * Generate a response containing only links. Useful in cases where a URL may not have any
     * responses, but can have child URL's.
     *
     * @param uriParent    the parent URI
     * @param uriSelf      the current URI
     * @param aChildLinks  the child links to be added
     *
     * @return the response
     */
    protected EntityMBeanResponse getLinksOnlyResponseBody(URI uriParent, URI uriSelf, String... aChildLinks)
        {
        return getLinksOnlyResponseBody(uriParent, uriSelf, getLinksFilter(), aChildLinks);
        }


    protected Map<String, Object> getAggregatedMetrics(String       sLocator,
                                                       String       sAttribute,
                                                       String       sCollector,
                                                       QueryBuilder bldrQuery)
        {
        try
            {
            MBeanAccessor accessor = getMBeanAccessor();
            return accessor.aggregate(bldrQuery.build(), sLocator, sAttribute, sCollector);
            }
        catch (RuntimeException e)
            {
            Response.Status status = Response.Status.SERVICE_UNAVAILABLE;
            for (Throwable t = e; t != null; t = t.getCause())
                {
                if (t instanceof IllegalArgumentException ||
                        t instanceof AttributeNotFoundException)
                    {
                    status = Response.Status.BAD_REQUEST;
                    }
                }
            // if the request was well formed the issue must be with the remote
            // MBeanServer; log an error
            if (status.equals(Response.Status.BAD_REQUEST))
                {
                throw new WebApplicationException(Response.status(status).
                        entity("HTTP " + status.getStatusCode() + ' ' + status.getReasonPhrase() + '\n' +
                                e.getMessage()).build());
                }
            else
                {
                log("Exception occurred while aggregating metrics with query "
                        + bldrQuery.build() + "\n" + getStackTrace(e), CacheFactory.LOG_ERR);
                throw new WebApplicationException();
                }
            }
        }

    /**
     * Get the response, if the request is a query. Queries are special in the fact that
     * they start from the root resource(Cluster), and takes in an filter map.
     *
     * @param  mapQuery      the object query map, query map contains which child elements must be returned
     * @param  mapArguments  the arguments to the child resource
     * @param  uriParent     the parent URI
     *
     * @return the response for the query
     */
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        return null;
        }

    /**
     * For the provided child resource, add the response of the query request.
     *
     * @param resource        the child Resource class
     * @param mapResponse        the response map, to which the child response must be added
     * @param sChildResourceKey  the string key of the child in the query
     * @param mapQuery           the object query map, query map contains which child elements must be returned
     * @param uriParent          the parent URI
     */
    protected void addChildResourceQueryResult(AbstractManagementResource resource,
                                               String                     sChildResourceKey,
                                               Map<String, Object>        mapResponse,
                                               Map                        mapQuery,
                                               Map<String, String>        mapArguments,
                                               URI                        uriParent)
        {
        Object childQueryEntity = mapQuery.get(sChildResourceKey);
        if (childQueryEntity != null && childQueryEntity instanceof Map)
            {
            EntityMBeanResponse responseEntity
                    = resource.getQueryResult((Map) childQueryEntity, mapArguments, uriParent);
            // add the response only if there is an entity
            if (responseEntity != null)
                {
                mapResponse.put(sChildResourceKey, responseEntity.toJson());
                }
            }
        }

    /**
     * For the provided child MBean query, fetch the response and add to the parent JSON.
     *
     * @param sChildKey    the string key of the child in the query
     * @param bldrQuery    the child Mbean query builder
     * @param mapResponse  the response map, to which the child response must be added
     * @param mapQuery     the object query map, query map contains which child elements must be returned
     * @param childLinks   the child links
     */
    protected void addChildMbeanQueryResult(String              sChildKey,
                                            QueryBuilder        bldrQuery,
                                            Map<String, Object> mapResponse,
                                            Map                 mapQuery,
                                            String...           childLinks)
        {
        Object oChildValue = mapQuery.get(sChildKey);
        if (oChildValue != null && oChildValue instanceof Map)
            {
            Map mapChildrenQuery = (Map) oChildValue;
            EntityMBeanResponse responseEntity = getResponseEntityForMbean(bldrQuery, getParentUri(),
                getCurrentUri(), getAttributesFilter(mapChildrenQuery), getLinksFilter(mapChildrenQuery), childLinks);

            if (responseEntity != null)
                {
                mapResponse.put(sChildKey, responseEntity.toJson());
                }
            }
        }

    /**
     * Add the aggregated metrics of the provided MBean type in the response.
     *
     * @param sRoleName    either a regex to be applied against node ids or a role name
     * @param sCollector   the collector to use instead of the default
     * @param mapResponse  the response map to which the metrics needs to be added.
     */
    protected void addAggregatedMetricsToResponseMap(String              sRoleName,
                                                     String              sCollector,
                                                     QueryBuilder        bldrQuery,
                                                     Map<String, Object> mapResponse)
        {
        Map<String, Object> mapAggregatedMetrics =
                getAggregatedMetrics(sRoleName, null, sCollector, bldrQuery);

        Filter<String> filterAttributes = getAttributesFilter();

        for (Map.Entry<String, Object> entry : mapAggregatedMetrics.entrySet())
            {
            String sAttributeKey = entry.getKey();
            if (filterAttributes.evaluate(sAttributeKey.toUpperCase()))
                {
                mapResponse.put(getRestName(sAttributeKey), Converter.convert(entry.getValue()));
                }
            }
        }

    /**
     * The filter used against the domainPartition.
     *
     * @return the filter used against the domainPartition
     */
    protected Filter<String> getDomainPartitionFilter()
        {
        return m_filterDomainPartition;
        }

    /**
     * Return the reference to be used to link to a Member.
     * <p>
     * If the provided {@link ObjectName} has a key property {@link MBeanAccessor#MEMBER member}, return its
     * property value.  Otherwise, return the {@link ObjectName} key property value for
     * {@link MBeanAccessor#NODE_ID nodeId}.
     *
     * @param objName  the ObjectName of the MBean
     *
     * @return the member reference
     */
    protected String getMemberReference(ObjectName objName)
        {
        String sMemberName = objName.getKeyProperty(MEMBER);
        if (sMemberName != null)
            {
            return sMemberName;
            }

        return objName.getKeyProperty(MBeanAccessor.NODE_ID);
        }

    /**
     * Return the value corresponding to the key "children"
     *
     * @param mapQuery  the map which needs to be used
     *
     * @return the children value
     */
    protected Object getChildrenQuery(Map mapQuery)
        {
        return mapQuery == null ? null : mapQuery.get(CHILDREN);
        }

    /**
     * Return a {@link Map} which contains the attributes of the provided MBean.
     *
     * @param filterAttributes  the filter to be applied on the attributes
     * @param mapResponse       the response map
     * @param sObjName          the ObjectName of the MBean
     *
     * @return the {@link Map} containing the attributes of the MBean
     *
     * @throws Exception thrown in case of JMX exceptions
     */
    protected Map<String, Object> getMBeanAttributesMap(Filter<String>      filterAttributes,
                                                        Map<String, Object> mapResponse,
                                                        String              sObjName) throws Exception
        {
        Map<String, Object> mapAttributes = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : mapResponse.entrySet())
            {
            String sAttrName = getRestName(entry.getKey());
            if (filterAttributes.evaluate(sAttrName))
                {
                mapAttributes.put(sAttrName, Converter.convert(entry.getValue()));
                }
            }

        for (Map.Entry<String, String> entryKeyValue : new ObjectName(sObjName).getKeyPropertyList().entrySet())
            {
            String sKey = getRestName(entryKeyValue.getKey());

            if (filterAttributes.evaluate(sKey))
                {
                // avoid clobbering existing MBean attribute when there is a ObjectName key property with same name.
                // examples include Service MBean Type attribute and PSOldGen MemoryPool MBean Type attribute
                // clashing with ObjectName key property type.
                mapAttributes.putIfAbsent(sKey, entryKeyValue.getValue());
                }
            }

        return mapAttributes;
        }

    /**
     * Convert the entity map to an {@link AttributeList}
     *
     * @param mapEntity  the input entity map
     *
     * @return the {@link AttributeList}
     */
    protected AttributeList getAttributeList(Map<String, Object> mapEntity)
        {
        return new AttributeList(mapEntity.entrySet()
                .stream().map(e -> new Attribute(fromRestName(e.getKey()), e.getValue()))
                .collect(Collectors.toList()));
        }

    /**
     * Return an attributes filter based on the URI query parameters.
     *
     * @return the attributes filter
     */
    protected Filter<String> getAttributesFilter()
        {
        // MBean attributes are called "fields" in the REST language
        String sIncludeFields = m_uriInfo.getQueryParameters().getFirst(INCLUDE_FIELDS);
        String sExcludeFields = m_uriInfo.getQueryParameters().getFirst(EXCLUDE_FIELDS);
        return getAttributesFilter(sIncludeFields, sExcludeFields);
        }

    /**
     * Return an attributes filter based on the provided include and exclude strings.
     *
     * @param sIncludeFields  the comma separated list of included attributes
     * @param sExcludeFields  the comma separated list of excluded attributes
     *
     * @return the attributes filter
     */
    protected Filter<String> getAttributesFilter(String sIncludeFields, String sExcludeFields)
        {
        Filter<String> filterAttributes = Filters.always();
        if (sIncludeFields != null)
            {
            Set<String> setIncludedFields = Arrays.stream(sIncludeFields.split(","))
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            filterAttributes = Filters.in(String::toUpperCase, setIncludedFields);
            }

        if (sExcludeFields != null)
            {
            Set<String> setExcludedFields = Arrays.stream(sExcludeFields.split(","))
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
            Filter<String> filterExclude = Filters.not(Filters.in(String::toUpperCase, setExcludedFields));
            filterAttributes = filterExclude.and(filterAttributes);
            }

        return filterAttributes;
        }

    /**
     * Return a links filter based on the URI query parameters.
     *
     * @return the links filter
     */
    protected Filter<String> getLinksFilter()
        {
        String sIncludeLinks = m_uriInfo.getQueryParameters().getFirst(INCLUDE_LINKS);
        String sExcludeLinks = m_uriInfo.getQueryParameters().getFirst(EXCLUDE_LINKS);

        Filter<String> filterLinks = Filters.always();
        if (sIncludeLinks != null)
            {
            Set<String> setIncludedLinks = Arrays.stream(sIncludeLinks.split(","))
                    .collect(Collectors.toSet());

            filterLinks = Filters.in(ValueExtractor.identity(), setIncludedLinks);
            }

        if (sExcludeLinks != null)
            {
            Set<String> setExcludedLinks = Arrays.stream(sExcludeLinks.split(","))
                    .collect(Collectors.toSet());

            Filter<String> filterExclude = Filters.not(Filters.in(ValueExtractor.identity(), setExcludedLinks));
            filterLinks = filterExclude.and(filterLinks);
            }

        return filterLinks;
        }

    /**
     * Return a fields filter, the filter parameters is taken from the query map if that is not null
     * else it is taken from the URI query parameters.
     *
     * @param mapQuery  the query map
     *
     * @return the fields filter
     */
    protected Filter<String> getAttributesFilter(Map mapQuery)
        {
        if (mapQuery == null)
            {
            return getAttributesFilter();
            }

        List<String> listIncludeFields = (List<String>) mapQuery.get(INCLUDE_FIELDS);
        List<String> listExcludeFields = (List<String>) mapQuery.get(EXCLUDE_FIELDS);

        Filter<String> filterAttributes = Filters.always();

        if (listIncludeFields != null)
            {
            Set<String> setIncludedFields = listIncludeFields.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            filterAttributes = Filters.in(String::toUpperCase, setIncludedFields);
            }

        if (listExcludeFields != null)
            {
            Set<String> setExcludedFields = listExcludeFields.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
            Filter<String> filterExclude = Filters.not(Filters.in(String::toUpperCase, setExcludedFields));
            filterAttributes = filterExclude.and(filterAttributes);
            }

        return filterAttributes;
        }

    /**
     * Return a links filter, the filter parameters is taken from the query map if that is not null
     * else it is taken from the URI query parameters.
     *
     * @param mapQuery  the query map
     *
     * @return the links filter
     */
    protected Filter<String> getLinksFilter(Map mapQuery)
        {
        if (mapQuery == null)
            {
            return getLinksFilter();
            }

        List<String> listIncludeLinks = (List<String>) mapQuery.get(INCLUDE_LINKS);
        List<String> listExcludeLinks = (List<String>) mapQuery.get(EXCLUDE_LINKS);

        Filter<String> filterLinks = Filters.always();
        if (listIncludeLinks != null)
            {
            Set<String> setIncludedLinks = listIncludeLinks.stream()
                    .collect(Collectors.toSet());

            filterLinks = Filters.in(ValueExtractor.identity(), setIncludedLinks);
            }

        if (listExcludeLinks != null)
            {
            Set<String> setExcludedLinks = listExcludeLinks.stream()
                    .map(s -> fromRestName(s))
                    .collect(Collectors.toSet());
            Filter<String> filterExclude = Filters.not(Filters.in(ValueExtractor.identity(), setExcludedLinks));
            filterLinks = filterExclude.and(filterLinks);
            }

        return filterLinks;
        }

    /**
     * Get the exclude list, from the mapQuery, if not null, else from the
     * URI query parameter.
     *
     * @param mapQuery  the Query map
     *
     * @return the comma separated list of attributes which needs to be excluded
     */
    protected String getExcludeList(Map mapQuery)
        {
        if (mapQuery == null)
            {
            return m_uriInfo.getQueryParameters().getFirst(EXCLUDE_FIELDS);
            }

        List<String> listExcludeFields = (List<String>) mapQuery.get(EXCLUDE_FIELDS);

        return listExcludeFields == null ? null : listExcludeFields.stream().collect(Collectors.joining(","));
        }

    /**
     * Create an entity response.
     *
     * @param parentUri    the parent URI which needs to be added to the response
     * @param selfUri      the self URI which needs to be added to the response
     * @param linksFilter  the filter which needs to be applied on the links
     *
     * @return the entity response
     */
    protected EntityMBeanResponse createResponse(URI parentUri, URI selfUri, Filter<String> linksFilter)
        {
        EntityMBeanResponse responseBody = new EntityMBeanResponse(m_requestContext, linksFilter);
        responseBody.addParentResourceLink(parentUri);
        responseBody.addSelfResourceLinks(selfUri);
        return responseBody;
        }

    /**
     * Create a response from the provided entity response. This method will throw an exception
     * if there is no entity in the MBean response.
     *
     * @param responseEntity  the response entity
     *
     * @return the {@link Response} object
     */
    protected Response response(EntityMBeanResponse responseEntity)
        {
        if (responseEntity == null)
            {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        
        // check if there are failures, and if so, should be classified as bad request
        if (responseEntity.hasFailures())
            {
            return Response.status(Response.Status.BAD_REQUEST).entity(responseEntity.toJson()).build();
            }

        return Response.ok(responseEntity.toJson()).build();
        }

    /**
     * Set the filter to be applied against the domainPartition.
     *
     * @param filter  a filter to apply against the domainPartition
     */
    protected void setDomainPartitionFilter(Filter<String> filter)
        {
        m_filterDomainPartition = filter;
        }

    /**
     * Create an OK response with the provided map as entity.
     *
     * @param mapResponse  the response entity
     *
     * @return the {@link Response} object
     */
    protected Response response(Map<String, Object> mapResponse)
        {
        return Response.ok(mapResponse).build();
        }

    /**
     * Set the cluster name.
     *
     * @param sClusterName  the cluster name
     */
    public void setClusterName(String sClusterName)
        {
        m_sClusterName = sClusterName;
        }

    /**
     * Set the MBean domain name.
     *
     * @param sMBeanDomainName  the MBean domain name to use
     */
    public void setMBeanDomainName(String sMBeanDomainName)
        {
        m_sMBeanDomainName = sMBeanDomainName;
        }

    /**
     * Check for any attribute that must always converted to {@link Long}/{@link Integer}/{@link Float}.
     *
     * @param entity  {@link Map} to check
     */
    protected void checkAttributeTypeConversion(Map<String, Object> entity)
        {
        checkAttributeTypeConversion(entity, false);
        }

    /**
     * Check for any attribute that must always be converted to {@link Long}/{@link Integer}/{@link Float}.
     * <p>
     * The type of the {@code expiryDelay} attribute for {@code CacheMBean} is {@link Integer} but it
     * is {@link Long} for {@code ManagementMBean}, therefore, this code takes this into account.
     *
     * @param entity       {@link Map} to check
     * @param fManagement  a flag to indicate if the query type is Management
     */
    protected void checkAttributeTypeConversion(Map<String, Object> entity, boolean fManagement)
        {
          entity.replaceAll((k, v) ->{
            if ((SET_LONG.contains(k) || (fManagement && k.compareToIgnoreCase("expiryDelay") == 0))
                    && !(v instanceof Long))
                {
                return Long.valueOf(v.toString());
                }
            else if (SET_INTEGER.contains(k) && !(v instanceof Integer))
                {
                return Integer.valueOf(v.toString());
                }
           else if (SET_FLOAT.contains(k) && !(v instanceof Float))
                {
                return Float.valueOf(v.toString());
                }
            else
                {
                return v;
                }
            });
        }

    // -------------------------- accessors ---------------------------------------------------------------

    /**
     * The service name parameter from the URI.
     *
     * @return the service name
     */
    protected String getService()
        {
        return m_uriInfo.getPathParameters().getFirst(SERVICE_NAME);
        }

    /**
     * The parent URI of the resource.
     *
     * @return the parent URI
     */
    protected URI getParentUri()
        {
        return getParentUri(m_uriInfo);
        }

    /**
     * The URI of the current resource.
     *
     * @return the resource URI
     */
    protected URI getCurrentUri()
        {
        return getSubUri(m_uriInfo);
        }

    /**
     * The {@link MBeanServerProxy} to be used.
     *
     * @return the {@link MBeanServerProxy}
     */
    protected MBeanServerProxy getMBeanServerProxy()
        {
        return m_mBeanServerProxy;
        }

    /**
     * The {@link ContainerRequestContext} of the current request.
     *
     * @return the {@link ContainerRequestContext}
     */
    protected ContainerRequestContext getRequestContext()
        {
        return m_requestContext;
        }

    /**
     * The cluster name to be used in the resource.
     *
     * @return the cluster name
     */
    protected String getCluster()
        {
        return m_sClusterName;
        }

    /**
     * The MBean Domain name to be used in the resource.
     *
     * @return the MBean domain name
     */
    protected String getMBeanDomainName()
        {
        return m_sMBeanDomainName;
        }

    // ----- static helper methods ------------------------------------------

    /**
     * Convert a name into a REST standards compatible name.
     *
     * @param sName  the service name to be normalized
     *
     * @return the REST compatible name
     */
    public static String getRestName(String sName)
        {
        // find the first set of upper case letters
        int count = 0;
        for (; count < sName.length(); count++)
            {
            if (!Character.isUpperCase(sName.charAt(count)))
                {
                break;
                }
            }
        if (count == sName.length())
            {
            // all upper case - leave it alone
            return sName;
            }

        if (count == 0)
            {
            // doesn't start upper case - leave it alone
            return sName;
            }

        if (count == 1)
            {
            // first letter is upper case and next letter is lower case, so convert
            // first letter to lower case
            // for example RefreshTime must be returned as refreshTime
            return sName.substring(0, count).toLowerCase() + sName.substring(count);
            }

        // starts with an acronym - leave it alone
        return sName;
        }

    /**
     * Convert a name from a REST standards compatible name to attribute name.
     *
     * @param sName  the service name to be normalized
     *
     * @return the REST compatible name
     */
    public static String fromRestName(String sName)
        {
        // we are assuming here that any attribute which can be updated belongs to the standard
        // set of attributes which we converted in the getRestName method above
        // we need to have a better logic based on a cached set of MBeaninfo objects here
        // if we find more number of instances of attributes which starts with acronym etc
        return sName.substring(0, 1).toUpperCase() + sName.substring(1);
        }

    /**
     * Append the provided segments to the parent URI.
     *
     * @param uriParent   the parent URI
     * @param asSegments  the segments to be appended
     *
     * @return the resulting URI
     */
    public static URI getSubUri(URI uriParent, String... asSegments)
        {
        UriBuilder bldr = UriBuilder.fromUri(uriParent);
        for (String segment : asSegments)
            {
            int cSlash = segment.indexOf('/');
            if (cSlash == -1)
                {
                bldr.segment(segment);
                }
            else
                {
                for (String sPart : PATH_PATTERN.split(segment))
                    {
                    bldr.segment(sPart);
                    }
                }
            }
        return bldr.build();
        }


    /**
     * Append the provided segments to the current URI.
     *
     * @param uriInfo     the URI info object
     * @param asSegments  the segments to be appended
     *
     * @return the resulting URI
     */
    public static URI getSubUri(UriInfo uriInfo, String... asSegments)
        {
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        Arrays.stream(asSegments).forEach(builder::segment);
        return builder.build();
        }

    /**
     * Return the parent URI of the resource.
     *
     * @param uriInfo  the URI info object
     *
     * @return the parent URI
     */
    public static URI getParentUri(UriInfo uriInfo)
        {
        int               count        = getParentUriSegmentsCount(uriInfo);
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        UriBuilder        bldr         = uriInfo.getBaseUriBuilder();

        for (int i = 0; i < count; i++)
            {
            bldr.path(pathSegments.get(i).getPath());
            }

        return bldr.build();
        }

    /**
     * Convert the set string representations of {@link ObjectName}.
     *
     * @param setObjectNames  the set of object names(string objects)
     *
     * @return the set of {@link ObjectName}s
     *
      * @throws MalformedObjectNameException thrown in case of malformed object name
     */
    public static Set<ObjectName> convertToObjectNames(Set<String> setObjectNames)
            throws MalformedObjectNameException
        {
        Set<ObjectName> setObjNames = new HashSet<>();
        for (String sObjectName : setObjectNames)
            {
            setObjNames.add(new ObjectName(sObjectName));
            }
        return setObjNames;
        }

    /**
     * Return the number of URI segments in the parent URL.
     *
     * @param uriInfo  the URI Info
     *
     * @return the number of segments in the parent URL
     */
    protected static int getParentUriSegmentsCount(UriInfo uriInfo)
        {
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        int               count        = pathSegments.size() - 1; // go up a level to get to the parent
        if (pathSegments.get(count).getPath().isEmpty())
            {
            count--; // go up for one level because of trailing slash
            }

        return count;
        }

    /**
     * Create a QueryBuilder, also set the common parameters in the builder.
     *
     * @return the QueryBuilder
     */
    protected QueryBuilder createQueryBuilder()
        {
        QueryBuilder bldrQuery = new QueryBuilder().withMBeanDomainName(getMBeanDomainName())
                .withCluster(getCluster());

        Filter<String> filter = getDomainPartitionFilter();
        if (filter != null)
            {
            bldrQuery.withFilter(DOMAIN_PARTITION, getDomainPartitionFilter());
            }
        return bldrQuery;
        }

    protected MBeanAccessor getMBeanAccessor()
        {
        MBeanAccessor accessor = m_accessor;
        if (accessor == null)
            {
            accessor = m_accessor = new MBeanAccessor(m_mBeanServerProxy);
            }
        return accessor;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The query parameter used to filter attributes, the parameter value must be a subset of
     * attributes which needs to be included.
     */
    public static final String INCLUDE_FIELDS = "fields";

    /**
     * The query parameter used to filter links, the parameter value must be a subset of
     * links which needs to be included.
     */
    public static final String INCLUDE_LINKS = "links";

    /**
     * The query parameter used to filter attributes, the parameter value must be a subset of
     * attributes which needs to be excluded.
     */
    public static final String EXCLUDE_FIELDS = "excludeFields";

    /**
     * The query parameter used to filter links, the parameter value must be a subset of
     * links which needs to be excluded.
     */
    public static final String EXCLUDE_LINKS = "excludeLinks";

    /**
     * Constant used for json application type.
     */
    public static final String MEDIA_TYPE_JSON = MediaType.APPLICATION_JSON;

    /**
     * Constant used for Swagger.
     */
    public static final String MEDIA_TYPE_SWAGGER_JSON = "application/swagger+json";

    /**
     * Swagger resource file name.
     */
    public static final String SWAGGER_RESOURCE = "management-swagger.json";

    /**
     * Constant used for applicable media types in the management REST interface.
     */
    public static final String MEDIA_TYPES = MEDIA_TYPE_JSON;

    // ------------------------------ Mbean Query patterns -----------------------------------

    /**
     * MBean query to filter out all the CacheMBean objects in the cluster.
     */
    public static final String CACHES_QUERY = ":" + Registry.CACHE_TYPE;

    /**
     * MBean query to filter out all the CacheMBean objects of a particular cache.
     */
    public static final String CACHE_QUERY = CACHES_QUERY + ",name=";

    /**
     * MBean query to filter out all the CacheMBean objects of a particular cache and service.
     */
    public static final String CACHE_MEMBERS_WITH_SERVICE_QUERY = CACHES_QUERY +",name=";

    /**
     * MBean query to filter out all the NodeMBean objects.
     */
    public static final String CLUSTER_MEMBERS_QUERY = ":" + Registry.NODE_TYPE;

    /**
     * MBean query to filter out all the ReporterMBean objects.
     */
    public static final String REPORTER_MEMBERS_QUERY = ":" + Registry.REPORTER_TYPE;

    /**
     * MBean query to filter out all the Flash journal MBean objects.
     */
    public static final String FLASH_JOURNAL_QUERY = ":" + Registry.JOURNAL_TYPE + ",name=FlashJournalRM";

    /**
     * MBean query to filter out all the RAM journal MBean objects.
     */
    public static final String RAM_JOURNAL_QUERY = ":" + Registry.JOURNAL_TYPE + ",name=RamJournalRM";

    /**
     * MBean query to filter out all the Cluster MBeans.
     */
    public static final String CLUSTER_QUERY = ":" + Registry.CLUSTER_TYPE;

    /**
     * MBean query to filter out all the Management MBeans.
     */
    public static final String MANAGEMENT_QUERY = ":" + Registry.MANAGEMENT_TYPE;

    /**
     * MBean query to filter out all the PointToPoint MBeans.
     */
    public static final String POINT_TO_POINT_QUERY = ":" + Registry.POINT_TO_POINT_TYPE;

    /**
     * MBean query to filter out all the ConnectionManager(Proxy) MBeans.
     */
    public static final String CONNECTION_MANAGERS_QUERY = ":" + Registry.CONNECTION_MANAGER_TYPE;

    /**
     * MBean query to filter out all the ConnectionManager(Proxy) MBeans of a specific proxy service.
     */
    public static final String CONNECTION_MANAGER_QUERY = CONNECTION_MANAGERS_QUERY + ",name=";

    /**
     * MBean query to filter out all the Connection(Proxy) MBeans.
     */
    public static final String CONNECTIONS_QUERY = ":" + Registry.CONNECTION_TYPE + ",name=";

    /**
     * MBean query to filter out all the ServiceMBean of a specific service.
     */
    public static final String SERVICE_MEMBERS_QUERY = ":" + Registry.SERVICE_TYPE + ",name=";

    /**
     * MBean query to filter out PartitionAssignment MBean of a specific service.
     */
    public static final String PARTITION_ASSIGNMENT_QUERY = ":" + Registry.PARTITION_ASSIGNMENT_TYPE;

    /**
     * MBean query to filter out all ServiceMBean objects.
     */
    public static final String SERVICES_QUERY = ":" + Registry.SERVICE_TYPE;

    /**
     * MBean query to filter out all ServiceMBean objects.
     */
    public static final String FEDERATION_TYPE = ":" + Registry.FEDERATION_TYPE;

    /**
     * MBean query to filter out FederationManager MBean of a specific federated service.
     */
    public static final String FEDERATION_COORDINATOR_QUERY = FEDERATION_TYPE + ",responsibility=Coordinator";

    /**
     * MBean query to filter out Topology MBeans of a specific federated service and a specific participant.
     */
    public static final String FEDERATION_TOPOLOGY_MEMBER_QUERY = FEDERATION_TYPE + ",subType=Topology,name=%s";

    /**
     * MBean query to filter out Topology MBeans.
     */
    public static final String FEDERATION_TOPOLOGIES_QUERY = FEDERATION_TYPE + ",subType=Topology";

    /**
     * MBean query to filter out Destination MBeans of a specific federated service and a specific participant.
     */
    public static final String DESTINATIONS_QUERY = FEDERATION_TYPE + ",subType=Destination,name=";

    /**
     * MBean query to filter out Destination MBeans of a specific federated service.
     */
    public static final String DESTINATIONS_COLLECTION_QUERY= FEDERATION_TYPE + ",subType=Destination";

    /**
     * MBean query to filter out Origin MBeans of a specific federated service.
     */
    public static final String ORIGINS_COLLECTION_QUERY = FEDERATION_TYPE + ",subType=Origin";

    /**
     * MBean query to filter out Origin MBeans of a specific federated service and specific participant.
     */
    public static final String ORIGINS_QUERY = FEDERATION_TYPE + ",subType=Origin,name=";

    /**
     * MBean query to filter out PersistenceCoordinator MBean of a specific service.
     */
    public static final String PERSISTENCE_CONTROLLER_QUERY
            = ":" + Registry.PERSISTENCE_SNAPSHOT_TYPE + ",responsibility=PersistenceCoordinator";

    /**
     * MBean query to filter out platform(JVM) Memory MBean.
     */
    public static final String PLATFORM_MEMORY_QUERY = ":type=Platform,Domain=java.lang,subType=Memory";

    /**
     * MBean query to filter out platform(JVM) MemoryPool MBean(Compressed Class Space).
     */
    public static final String COMPRESSED_CLASS_SPACE_QUERY
            = ":type=Platform,Domain=java.lang,subType=MemoryPool,name=Compressed Class Space";

    /**
     * MBean query to filter out platform(JVM) MemoryPool MBean(Metaspace).
     */
    public static final String META_SPACE_QUERY
            = ":type=Platform,Domain=java.lang,subType=MemoryPool,name=Metaspace";

    /**
     * MBean query to filter out platform(JVM) PS GarbageCollector MBean(PS Mark sweep).
     */
    public static final String PS_MARK_SWEEP_QUERY
            = ":type=Platform,Domain=java.lang,subType=GarbageCollector,name=PS MarkSweep";

    /**
     * MBean query to filter out platform(JVM) PS GarbageCollector MBean(PS Scavenge).
     */
    public static final String PS_SCAVENGE_QUERY
            = ":type=Platform,Domain=java.lang,subType=GarbageCollector,name=PS Scavenge";

    /**
     * MBean query to filter out platform(JVM) PS MemoryPool MBean(PS Old Gen).
     */
    public static final String PS_OLDGEN_QUERY
            = ":type=Platform,Domain=java.lang,subType=MemoryPool,name=PS Old Gen";

    /**
     * MBean query to filter out platform(JVM) PS MemoryPool MBean(Code Cache).
     */
    public static final String CODECACHE_QUERY
            = ":type=Platform,Domain=java.lang,subType=MemoryPool,name=Code Cache";

    /**
     * MBean query to filter out platform(JVM) PS MemoryPool MBean(PS Eden Space).
     */
    public static final String PS_EDEN_SPACE_QUERY
            = ":type=Platform,Domain=java.lang,subType=MemoryPool,name=PS Eden Space";

    /**
     * MBean query to filter out platform(JVM) PS MemoryPool MBean(PS Survivor Space).
     */
    public static final String PS_SURVIVOR_SPACE_QUERY
            = ":type=Platform,Domain=java.lang,subType=MemoryPool,name=PS Survivor Space";

    /**
     * MBean query to filter out platform(JVM) OperatingSystem MBean.
     */
    public static final String OS_QUERY
            = ":type=Platform,Domain=java.lang,subType=OperatingSystem";

    /**
     * MBean query to filter out platform(JVM) Runtime MBean.
     */
    public static final String RUNTIME_QUERY = ":type=Platform,Domain=java.lang,subType=Runtime";

    /**
     * MBean query to filter out platform(JVM) G1 GarbageCollector MBean(G1 Old Generation).
     */
    public static final String G1_OLD_GENERATION_QUERY
            = ":type=Platform,Domain=java.lang,subType=GarbageCollector,name=G1 Old Generation";

    /**
     * MBean query to filter out platform(JVM) G1 GarbageCollector MBean(G1 Young Generation).
     */
    public static final String G1_YOUNG_GENERATION_QUERY
            = ":type=Platform,Domain=java.lang,subType=GarbageCollector,name=G1 Young Generation";

    /**
     * MBean query to filter out platform(JVM) G1 MemoryPool MBean(G1 Eden Space).
     */
    public static final String G1_EDEN_SPACE_QUERY
            = ":type=Platform,Domain=java.lang,subType=MemoryPool,name=G1 Eden Space";

    /**
     * MBean query to filter out platform(JVM) G1 MemoryPool MBean(G1 Old Gen).
     */
    public static final String G1_OLDGEN_QUERY
            = ":type=Platform,Domain=java.lang,subType=MemoryPool,name=G1 Old Gen";

    /**
     * MBean query to filter out platform(JVM) G1 MemoryPool MBean(G1 Survivor Space).
     */
    public static final String G1_SURVIVOR_SPACE_QUERY
            = ":type=Platform,Domain=java.lang,subType=MemoryPool,name=G1 Survivor Space";

    /**
     * MBean query to filter out platform(JVM) G1 MemoryPool MBean(CodeHeap 'non-nmethods').
     */
    public static final String G1_CODEHEAP_NON_NMETHODS_QUERY
            = ":type=Platform,Domain=java.lang,subType=MemoryPool,name=CodeHeap \'non-nmethods\'";

    /**
     * MBean query to filter out platform(JVM) G1 MemoryPool MBean(CodeHeap 'profiled nmethods').
     */
    public static final String G1_CODEHEAP_PROFILED_NMETHODS_QUERY
            = ":type=Platform,Domain=java.lang,subType=MemoryPool,name=CodeHeap \'profiled nmethods\'";

    /**
     * MBean query to filter out platform(JVM) G1 MemoryManager MBean(CodeCacheManager).
     */
    public static final String G1_CODECACHE_MANAGER
            = ":type=Platform,Domain=java.lang,subType=MemoryManager,name=CodeCacheManager";

    /**
     * MBean query to filter out platform(JVM) G1 MemoryManager MBean(Metaspace Manager).
     */
    public static final String G1_METASPACE_MANAGER
            = ":type=Platform,Domain=java.lang,subType=MemoryManager,name=Metaspace Manager";

    /**
     * MBean query to filter out HttpSessionManager(CWEb) MBeans. The * before the HttpSessionManager is to filter
     * out 2 kinds of MBeans, HttpSessionManager and WeblogicHttpSessionManager MBeans.
     */
    public static final String CWEB_APPLICATIONS_QUERY = ":type=*HttpSessionManager";

    /**
     * MBean query to filter out HttpSessionManager(CWEb) MBeans of a particular application.
     * The * before the HttpSessionManager is to filter out 2 kinds of MBeans, HttpSessionManager
     * and WeblogicHttpSessionManager MBeans.
     */
    public static final String CWEB_APPLICATION_QUERY = ":type=*HttpSessionManager,appId=";

    /**
     * MBean query to filter out StorageManager MBean of a specific cache and service, running on a specific node.
     */
    public static final String STORAGE_MANAGERS_QUERY = ":type=StorageManager,cache=%s";

    /**
     * MBean query to filter out StorageManager MBean of a specific cache and service, running on a specific node.
     */
    public static final String STORAGE_MANAGER_QUERY = ":type=StorageManager,cache=%s,service=%s,nodeId=%s";

    // ------------------------------ Mbean Query patterns ends ---------------------------------

    // ------------------------------ Path param constants --------------------------------------

    public static final String CLUSTER_NAME     = "clusterName";
    public static final String SERVICE_NAME     = "serviceName";
    public static final String CACHE_NAME       = "cacheName";
    public static final String MEMBER_KEY       = "memberKey";
    public static final String VERSION          = "versionName";
    public static final String PARTICIPANT_NAME = "participantName";
    public static final String TOPOLOGY_NAME    = "topologyName";
    public static final String OPERATION_NAME   = "operationName";
    public static final String PLATFORM_MBEAN   = "platformMBean";
    public static final String APPLICATION_ID   = "applicationId";
    public static final String DOMAIN_PARTITION = "domainPartition";
    public static final String TOPOLOGIES       = "topologies";
    public static final String SNAPSHOT_NAME    = "snapshotName";
    public static final String JFR_CMD          = "jfrCmd";

    // ------------------------------ Path param constants ends --------------------------------------

    // ------------------------------ URL constants --------------------------------------------------

    public static final String METADATA_CATALOG = "metadata-catalog";
    public static final String SERVICES         = "services";
    public static final String MEMBERS          = "members";
    public static final String REPORTERS        = "reporters";
    public static final String CACHES           = "caches";
    public static final String MANAGEMENT       = "management";
    public static final String SHUTDOWN         = "shutdown";
    public static final String CLUSTER          = "cluster";
    public static final String SEARCH           = "search";
    public static final String STORAGE          = "storage";
    public static final String JOURNAL          = "journal";
    public static final String JOURNAL_TYPE     = "journalType";
    public static final String NETWORK_STATS    = "networkStats";
    public static final String VERBOSE          = "verbose";
    public static final String WEB_APPS         = "webApplications";
    public static final String PERSISTENCE      = "persistence";
    public static final String PROXY            = "proxy";
    public static final String STATISTICS       = "statistics";
    public static final String INCOMING         = "incoming";
    public static final String OUTGOING         = "outgoing";

    // ------------------------------ URL constants ends ---------------------------------------------

    // ------------------------------ Misc constants -------------------------------------------------

    public static final String LOADER             = "loader";
    public static final String MEMBER             = "member";
    public static final String CONNECTIONS        = "connections";
    public static final String ROLE_NAME          = "role";
    public static final String COLLECTOR          = "collector";
    public static final String PLATFORM           = "platform";
    public static final String RAM_JOURNAL_TYPE   = "ram";
    public static final String FLASH_JOURNAL_TYPE = "flash";
    public static final String PARTITION          = "partition";
    public static final String RESET_STATS        = "resetStatistics";
    public static final String CHILDREN           = "children";
    public static final String NAME               = "name";
    public static final String TYPE               = "type";
    public static final String NODE_ID            = "nodeId";
    public static final String SERVICE            = "service";
    public static final String TIER_BACK          = "back";
    public static final String TIER               = "tier";
    public static final String OPTIONS            = "options";

    /**
     * Map of URL to platform Mbean query.
     */
    public static final Map<String, String> MAP_PLATFORM_URL_TO_MBEAN_QUERY =
            Collections.unmodifiableMap(new HashMap<String, String>()
                {{
                put("memory", PLATFORM_MEMORY_QUERY);
                put("metaSpace", META_SPACE_QUERY);
                put("compressedClassSpace", COMPRESSED_CLASS_SPACE_QUERY);
                put("operatingSystem", OS_QUERY);
                put("runtime", RUNTIME_QUERY);
                }});

    /**
     * Map of URL to platform PS GC Mbean query.
     */
    public static final Map<String, String> MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY =
            Collections.unmodifiableMap(new HashMap<String, String>()
                {{
                put("psMarkSweep", PS_MARK_SWEEP_QUERY);
                put("psScavenge", PS_SCAVENGE_QUERY);
                put("psOldGen", PS_OLDGEN_QUERY);
                put("psEdenSpace", PS_EDEN_SPACE_QUERY);
                put("psSurvivorSpace", PS_SURVIVOR_SPACE_QUERY);
                put("codeCache", CODECACHE_QUERY);
            }});

    /**
     * Map of URL to platform G1 GC Mbean query.
     */
    public static final Map<String, String> MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY =
            Collections.unmodifiableMap(new HashMap<String, String>()
            {{
                put("g1EdenSpace", G1_EDEN_SPACE_QUERY);
                put("g1OldGen", G1_OLDGEN_QUERY);
                put("g1OldGeneration", G1_OLD_GENERATION_QUERY);
                put("g1SurvivorSpace", G1_SURVIVOR_SPACE_QUERY);
                put("g1YoungGeneration", G1_YOUNG_GENERATION_QUERY);
                put("g1CodeHeapNonNMethods", G1_CODEHEAP_NON_NMETHODS_QUERY);
                put("g1CodeHeapProfiledNMethods", G1_CODEHEAP_PROFILED_NMETHODS_QUERY);
                put("g1CodeCacheManager", G1_CODECACHE_MANAGER);
                put("g1MetaSpaceManager", G1_METASPACE_MANAGER);
            }});

    /**
     * Map of journal type fo MBean query.
     */
    public static final Map<String, String> MAP_JOURNAL_URL_TO_MBEAN_QUERY =
            Collections.unmodifiableMap(new HashMap<String, String>()
                {{
                put("flash", FLASH_JOURNAL_QUERY);
                put("ram", RAM_JOURNAL_QUERY);
                }});

    /**
     * Set of attributes to be converted to Long.
     */
    private static final Set<String> SET_LONG = new HashSet<>(Arrays.asList(
            "intervalSeconds", "currentBatch",
            "maxQueryThresholdMillis", "batchFactor", "transportRetainedBytes"));

    /**
     * Set of attributes to be converted to Integer.
     */
    private static final Set<String> SET_INTEGER = new HashSet<>(Arrays.asList(
            "expiryDelay"));

    /**
     * Set of attributes to be converted to Float.
     */
    private static final Set<String> SET_FLOAT = new HashSet<>(Arrays.asList(
            "tracingSamplingRatio"));

    /**
     * Cached pattern for splitting request paths.
     */
    private static final Pattern PATH_PATTERN = Pattern.compile("/");


    // ----- data members ---------------------------------------------------

    /**
     * The request headers, available in the context.
     */
    @Context
    protected HttpHeaders m_requestHeaders;

    /**
     * The UriInfo available in the context.
     */
    @Context
    protected UriInfo m_uriInfo;

    /**
     * The container request context.
     */
    @Context
    protected ContainerRequestContext m_requestContext;

    /**
     * The Mbean server proxy.
     */
    @Context
    protected MBeanServerProxy m_mBeanServerProxy;

    /**
     * The filter used against the domainPartition.
     */
    private Filter<String> m_filterDomainPartition;

    /**
     * The cluster name to be used by the resource. This has to be set while
     * creating the resource.
     */
    private String m_sClusterName;

    /**
     * The domain MBean name to be used. This has to be set while
     * creating the resource. In standalone Coherence, there is no
     * need to set it as the MBean proxy will append the correct
     * domain name.
     */
    private String m_sMBeanDomainName;

    protected MBeanAccessor m_accessor;
    }
