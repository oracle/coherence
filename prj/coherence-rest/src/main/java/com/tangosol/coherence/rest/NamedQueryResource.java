/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.tangosol.coherence.rest.config.ExpressionAliasConfig;
import com.tangosol.coherence.rest.config.NamedQuery;

import com.tangosol.coherence.rest.events.MapEventOutput;

import com.tangosol.coherence.rest.query.Query;
import com.tangosol.coherence.rest.query.QueryEngine;
import com.tangosol.coherence.rest.query.QueryEngineRegistry;

import com.tangosol.coherence.rest.query.QueryException;
import com.tangosol.coherence.rest.util.PropertySet;
import com.tangosol.coherence.rest.util.RestHelper;

import com.tangosol.coherence.rest.util.aggregator.AggregatorRegistry;

import com.tangosol.coherence.rest.util.processor.ProcessorRegistry;

import com.tangosol.internal.util.security.RemoteInstallGate;

import com.tangosol.io.SerializationRole;

import com.tangosol.net.NamedCache;

import com.tangosol.util.FilterBuildingException;
import com.tangosol.util.InvocableMap.EntryAggregator;
import com.tangosol.util.InvocableMap.EntryProcessor;

import com.tangosol.util.ValueExtractor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.sse.SseFeature;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * REST resource representing a set of filtered cache entries.
 *
 * @author ic  2011.12.03
 */
@SuppressWarnings("unchecked")
public class NamedQueryResource
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Create a instance of <tt>NamedQueryResource</tt>.
     *
     * @param cache               cache to create a resource for
     * @param query               query filtering the cache entries
     * @param cMaxResults         max size of result set for this resource
     */
    public NamedQueryResource(NamedCache cache, NamedQuery query, int cMaxResults)
        {
        m_cache       = cache;
        m_query       = query;
        m_cMaxResults = cMaxResults;
        }

    // ---- resource methods ------------------------------------------------

    /**
     * Perform an aggregating operation against the entries that belong to
     * the named query.
     *
     * @param uriInfo  UriInfo for the invoked resource method
     * @param sAggr    name of the aggregator
     *
     * @return the result of the aggregation
     */
    @GET
    @Path("{aggr: " + AggregatorRegistry.AGGREGATOR_REQUEST_REGEX + "}")
    @Produces({APPLICATION_JSON, APPLICATION_XML, TEXT_PLAIN})
    public Response aggregate(
            @Context UriInfo uriInfo,
            @PathParam("aggr") String sAggr)
        {
        EntryAggregator aggregator;
        try
            {
            aggregator = m_aggregatorRegistry.getAggregator(
                    RestExpressionPolicy.resolveAggregator(m_expressionAliases, sAggr));
            RemoteInstallGate.enforceCacheAggregatorInstall(aggregator, SerializationRole.REST, null);
            }
        catch (IllegalArgumentException e)
            {
            RestHelper.log(e);
            // named-query aggregation rejections use the same client-visible bad-request body as other REST paths
            return Response.status(Response.Status.BAD_REQUEST).entity(CacheResource.BAD_REQUEST_MSG).build();
            }

        Collection colKeys = keys(uriInfo);
        Object     oResult = m_cache.aggregate(colKeys, aggregator);

        return Response.ok(oResult).build();
        }

    /**
     * Invoke the specified processor against the entries that belong to the
     * named query.
     *
     * @param uriInfo  UriInfo for the invoked resource method
     * @param sProc    the name of the processor
     *
     * @return a Map containing the results of invoking the EntryProcessor
     *         against the entries that are selected by the given query
     */
    @POST
    @Path("{proc: " + ProcessorRegistry.PROCESSOR_REQUEST_REGEX + "}")
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Response process(
            @Context UriInfo uriInfo,
            @PathParam("proc") String sProc)
        {
        EntryProcessor processor;
        try
            {
            processor = m_processorRegistry.getProcessor(
                    RestExpressionPolicy.resolveProcessor(m_expressionAliases, sProc));
            RemoteInstallGate.enforceCacheProcessorInstall(processor, SerializationRole.REST, null);
            }
        catch (IllegalArgumentException e)
            {
            RestHelper.log(e);
            return Response.status(Response.Status.BAD_REQUEST).entity(CacheResource.BAD_REQUEST_MSG).build();
            }

        Collection colKeys   = keys(uriInfo);
        Map        mapResult = m_cache.invokeAll(colKeys, processor);

        return Response.ok(mapResult).build();
        }

    /**
     * Return the cache values that satisfy criteria defined by this
     * resource.
     *
     * @param uriInfo      UriInfo for the invoked resource method
     * @param nStart       the starting index of result set to be returned
     * @param cResults     the size of result set to be returned (page size)
     * @param sSort        a string expression that represents ordering
     * @param sProjection  properties to return (if null, values will be
     *                     returned)
     *
     * @return values that satisfy criteria defined by this resource
     */
    @GET
    public Response getValues(@Context UriInfo uriInfo,
                              @MatrixParam("start") @DefaultValue("0") int nStart,
                              @MatrixParam("count") @DefaultValue("-1") int cResults,
                              @MatrixParam("sort") String sSort,
                              @MatrixParam("p") String sProjection)
        {
        try
            {
            PropertySet propertySet = RestExpressionPolicy.resolveProjection(m_expressionAliases, sProjection);
            ValueExtractor<Map.Entry, ?> extractor = RestValueExtractors.valueExtractor(propertySet);
            return Response.ok(executeQuery(uriInfo, extractor, nStart, cResults, sSort)).build();
            }
        catch (FilterBuildingException | QueryException | IllegalArgumentException e)
            {
            RestHelper.log(e);
            return Response.status(Response.Status.BAD_REQUEST).entity(CacheResource.BAD_REQUEST_MSG).build();
            }
        }

    /**
     * Return the cache entries that satisfy criteria defined by
     * this resource.
     *
     * @param uriInfo      UriInfo for the invoked resource method
     * @param nStart       the starting index of result set to be returned
     * @param cResults     the size of result set to be returned (page size)
     * @param sSort        a string expression that represents ordering
     * @param sProjection  the subset of properties to return for each value
     *                     (if null, the complete values will be returned)
     *
     * @return the cache entries satisfying criteria defined by this resource
     */
    @GET
    @Path("entries")
    @Produces({APPLICATION_JSON, APPLICATION_XML, TEXT_PLAIN})
    public Response getEntries(@Context UriInfo uriInfo,
                @MatrixParam("start") @DefaultValue("0") int nStart,
                @MatrixParam("count") @DefaultValue("-1") int cResults,
                @MatrixParam("sort") String sSort,
                @MatrixParam("p") String sProjection)
        {
        try
            {
            PropertySet propertySet = RestExpressionPolicy.resolveProjection(m_expressionAliases, sProjection);
            ValueExtractor<Map.Entry, ?> extractor = RestValueExtractors.entryExtractor(propertySet);
            return Response.ok(executeQuery(uriInfo, extractor, nStart, cResults, sSort)).build();
            }
        catch (FilterBuildingException | QueryException | IllegalArgumentException e)
            {
            RestHelper.log(e);
            return Response.status(Response.Status.BAD_REQUEST).entity(CacheResource.BAD_REQUEST_MSG).build();
            }
        }

    /**
     * Return the keys for cache entries that satisfy criteria defined by
     * this resource.
     *
     * @param uriInfo  UriInfo for the invoked resource method
     *
     * @return the keys of the cache entries satisfying criteria defined by
     *         this resource
     */
    @GET
    @Path("keys")
    @Produces({APPLICATION_JSON, APPLICATION_XML, TEXT_PLAIN})
    public Response getKeys(@Context UriInfo uriInfo)
        {
        Set setKeys = keys(uriInfo);
        return Response.ok(setKeys).build();
        }

    /**
     * Register SSE event listener for this named query.
     *
     * @param fLite    flag specifying whether to register for lite or full events
     * @param uriInfo  UriInfo for the invoked resource method
     *
     * @return the EventOutput that will be used to send events to the client
     */
    @GET
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public MapEventOutput addListener(@MatrixParam("lite") boolean fLite, @Context UriInfo uriInfo)
        {
        if ("DEFAULT".equals(m_query.getQueryEngineName()))
            {
            Map<String, Object> mapParams = RestHelper.getQueryParameters(uriInfo);

            MapEventOutput eventOutput = new MapEventOutput(m_cache, fLite);
            eventOutput.setFilter(RestQueryPolicy.createNamedQueryFilter(m_query.getExpression(), mapParams));
            eventOutput.register();

            return eventOutput;
            }

        return null;
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Returns a collection of extracted values for cache entries that satisfy
     * the criteria expressed by the query.
     *
     * @param uriInfo   UriInfo for the invoked resource method
     * @param extractor the extractor to apply to each entry in the result set
     * @param nStart    the starting index of result set to be returned
     * @param cResults  the size of result set to be returned (page size)
     * @param sSort     a string expression that represents sort order
     *
     * @return the cache entries that satisfy the criteria defined by this named query
     */
    protected Collection executeQuery(UriInfo uriInfo, ValueExtractor<Map.Entry, ?> extractor, int nStart, int cResults, String sSort)
        {
        NamedQuery          namedQuery   = m_query;
        String              sQueryEngine = namedQuery.getQueryEngineName();
        QueryEngine         queryEngine  = m_registry.getQueryEngine(sQueryEngine);
        Map<String, Object> mapParams    = RestHelper.getQueryParameters(uriInfo);
        int                 cMaxResults  = RestHelper.resolveMaxResults(cResults, namedQuery.getMaxResults(), m_cMaxResults);

        Query query = queryEngine.prepareQuery(namedQuery.getExpression(), mapParams);
        return query.execute(m_cache, extractor,
                RestExpressionPolicy.resolveSort(m_expressionAliases, sSort), nStart, cMaxResults);
        }

    /**
     * Set expression aliases for this resource.
     *
     * @param aliases  expression aliases
     */
    public void setExpressionAliases(ExpressionAliasConfig aliases)
        {
        m_expressionAliases = aliases == null ? ExpressionAliasConfig.EMPTY : aliases;
        }

    /**
     * Return expression aliases for this resource.
     *
     * @return expression aliases
     */
    public ExpressionAliasConfig getExpressionAliases()
        {
        return m_expressionAliases;
        }

    /**
     * Return the keys of cache entries that satisfy the named query criteria.
     *
     * @param uriInfo  UriInfo for invoked resource method
     *
     * @return filtered key set
     */
    protected Set keys(UriInfo uriInfo)
        {
        NamedQuery          namedQuery   = m_query;
        String              sQueryEngine = namedQuery.getQueryEngineName();
        QueryEngine         queryEngine  = m_registry.getQueryEngine(sQueryEngine);
        Map<String, Object> mapParams    = RestHelper.getQueryParameters(uriInfo);

        Query query = queryEngine.prepareQuery(namedQuery.getExpression(), mapParams);
        return query.keySet(m_cache);
        }

    // ---- data members ----------------------------------------------------

    /**
     * NamedCache wrapped by this resource.
     */
    protected NamedCache m_cache;

    /**
     * Named query responsible to filter cache entries.
     */
    protected NamedQuery m_query;

    /**
     * Maximum size of the result set this resource is allowed to return.
     */
    protected int m_cMaxResults;

    /**
     * Expression aliases configured for this resource.
     */
    protected ExpressionAliasConfig m_expressionAliases = ExpressionAliasConfig.EMPTY;

    /**
     * Query engine registry.
     */
    @Inject
    protected QueryEngineRegistry m_registry;

    /**
     * Aggregator registry that is used to map the given aggregator name to
     * an EntryAggregator instance.
     */
    @Inject
    protected AggregatorRegistry m_aggregatorRegistry;

    /**
     * a processor registry that is used to map the given processor name to
     * an EntryProcessor instance.
     */
    @Inject
    protected ProcessorRegistry m_processorRegistry;
    }
