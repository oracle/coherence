/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.rest.config.DirectQuery;
import com.tangosol.coherence.rest.config.NamedQuery;
import com.tangosol.coherence.rest.config.QueryConfig;

import com.tangosol.coherence.rest.events.MapEventOutput;

import com.tangosol.coherence.rest.io.MarshallerRegistry;

import com.tangosol.coherence.rest.query.Query;
import com.tangosol.coherence.rest.query.QueryEngine;
import com.tangosol.coherence.rest.query.QueryEngineRegistry;
import com.tangosol.coherence.rest.query.QueryException;

import com.tangosol.coherence.rest.server.InjectionBinder;

import com.tangosol.coherence.rest.util.PropertySet;
import com.tangosol.coherence.rest.util.RestHelper;

import com.tangosol.coherence.rest.util.aggregator.AggregatorRegistry;

import com.tangosol.coherence.rest.util.processor.ProcessorRegistry;

import com.tangosol.net.NamedCache;

import com.tangosol.util.FilterBuildingException;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.QueryHelper;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.AlwaysFilter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.ws.rs.core.Response;

import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * RESTful wrapper around a Coherence {@link NamedCache}.
 *
 * @author as  2011.06.03
 */
@SuppressWarnings({"unchecked"})
public class CacheResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new CacheResource.
     *
     * @param cache         cache to create a resource for
     * @param clzKey        key class of the cached entries
     * @param clzValue      value class of the cached entries
     * @param keyConverter  key converter to use
     * @param queryConfig   query configuration for this resource
     * @param cMaxResults   max size of result set for this resource
     */
    public CacheResource(NamedCache cache, Class clzKey, Class clzValue,
            KeyConverter keyConverter, QueryConfig queryConfig, int cMaxResults)
        {
        m_cache                = cache;
        m_clzKey               = clzKey;
        m_clzValue             = clzValue;
        m_keyConverter         = keyConverter == null
                                 ? new DefaultKeyConverter(clzKey)
                                 : keyConverter;
        m_queryConfig          = queryConfig;
        m_cMaxResults          = cMaxResults;
        }

    // ----- resource methods -----------------------------------------------

    /**
     * Return the cache values (or a subset of their properties) that
     * satisfy the specified criteria.
     *
     * @param nStart       starting index of result set to be returned
     * @param cResults     size of result set to be returned (page size)
     * @param sSort        a string expression that represents ordering
     * @param propertySet  the subset of properties to return for each value
     *                     (if null, the complete values will be returned)
     * @param sQuery       where predicate of Coherence Query Language to
     *                     filter cache entries. If null, all cache values
     *                     will be returned
     *
     * @return the cache values (or a subset of their properties) that
     *         satisfy specified criteria
     */
    @GET
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Response getValues(
            @MatrixParam("start") @DefaultValue("0") int nStart,
            @MatrixParam("count") @DefaultValue("-1") int cResults,
            @MatrixParam("sort") String sSort,
            @MatrixParam("p") PropertySet propertySet,
            @QueryParam("q") String sQuery)
        {
        boolean fDirectQuery = sQuery != null && sQuery.length() > 0;
        if (fDirectQuery && !m_queryConfig.isDirectQueryEnabled())
            {
            return Response.status(Response.Status.FORBIDDEN)
                           .entity("Direct query is not allowed").build();
            }

        ValueExtractor<Map.Entry, ?> extractor = Map.Entry::getValue;
        if (propertySet != null)
            {
            extractor = extractor.andThen(propertySet);
            }

        try
            {
            return Response.ok(executeQuery(sQuery, extractor, nStart, cResults, sSort)).build();
            }
        catch (FilterBuildingException | QueryException | IllegalArgumentException e)
            {
            RestHelper.log(e);
            return Response.status(Response.Status.BAD_REQUEST).entity(BAD_REQUEST_MSG).build();
            }
        }

    /**
     * Return the cache entries that satisfy the given query criteria.
     *
     * @param nStart       starting index of result set to be returned
     * @param cResults     size of result set to be returned (page size)
     * @param sSort        a string expression that represents ordering
     * @param propertySet  the subset of properties to return for each value
     *                     (if null, the complete values will be returned)
     * @param sQuery       where predicate of Coherence Query Language to
     *                     filter cache entries. If null, all cache entries
     *                     will be returned
     *
     * @return the cache entries that satisfy specified criteria
     */
    @GET
    @Path("entries")
    @Produces({APPLICATION_JSON, APPLICATION_XML, TEXT_PLAIN})
    public Response getEntries(
            @MatrixParam("start") @DefaultValue("0") int nStart,
            @MatrixParam("count") @DefaultValue("-1") int cResults,
            @MatrixParam("sort") String sSort,
            @MatrixParam("p") PropertySet propertySet,
            @QueryParam("q") String sQuery)
        {
        boolean fDirectQuery = sQuery != null && sQuery.length() > 0;
        if (fDirectQuery && !m_queryConfig.isDirectQueryEnabled())
            {
            return Response.status(Response.Status.FORBIDDEN)
                           .entity("Direct query is not allowed").build();
            }

        ValueExtractor<Map.Entry, ?> extractor = propertySet == null
                ? ValueExtractor.identity()
                : (entry) -> new SimpleMapEntry<>(entry.getKey(), propertySet.extract(entry.getValue()));

        try
            {
            return Response.ok(executeQuery(sQuery, extractor, nStart, cResults, sSort)).build();
            }
        catch (FilterBuildingException | QueryException | IllegalArgumentException e)
            {
            RestHelper.log(e);
            return Response.status(Response.Status.BAD_REQUEST).entity(BAD_REQUEST_MSG).build();
            }
        }

    /**
     * Return the keys of cache entries that satisfy the given query criteria.
     *
     * @param sQuery  query expression
     *
     * @return the keys of cache entries that satisfy the given query criteria
     *
     */
    @GET
    @Path("keys")
    @Produces({APPLICATION_JSON, APPLICATION_XML, TEXT_PLAIN})
    public Response getKeys(@QueryParam("q") String sQuery)
        {
        boolean fDirectQuery = sQuery != null && sQuery.length() > 0;
        if (fDirectQuery && !m_queryConfig.isDirectQueryEnabled())
            {
            return Response.status(Response.Status.FORBIDDEN)
                           .entity("Direct query is not allowed").build();
            }

        try
            {
            return Response.ok(keys(sQuery)).build();
            }
        catch (FilterBuildingException | QueryException | IllegalArgumentException e)
            {
            RestHelper.log(e);
            return Response.status(Response.Status.BAD_REQUEST).entity(BAD_REQUEST_MSG).build();
            }
        }

    /**
     * Perform an aggregating operation against the entries that satisfy the
     * specified criteria. If the query string is empty all cache entries are
     * aggregated.
     *
     * @param sAggr   name of the aggregator
     * @param sQuery  where predicate of Coherence Query Language to filter
     *                cache entries (optional)
     * @return the result of the aggregation
     */
    @GET
    @Path("{aggr: " + AggregatorRegistry.AGGREGATOR_REQUEST_REGEX + "}")
    @Produces({APPLICATION_JSON, APPLICATION_XML, TEXT_PLAIN})
    public Response aggregate(
            @PathParam("aggr") String sAggr,
            @QueryParam("q") String sQuery)
        {
        QueryConfig queryConfig  = m_queryConfig;
        NamedCache  cache        = m_cache;
        boolean     fDirectQuery = sQuery != null && sQuery.length() > 0;

        if (fDirectQuery && !queryConfig.isDirectQueryEnabled())
            {
            return Response.status(Response.Status.FORBIDDEN)
                           .entity("Direct query is not allowed").build();
            }

        InvocableMap.EntryAggregator aggregator;
        try
            {
            aggregator = m_aggregatorRegistry.getAggregator(sAggr);
            }
        catch (IllegalArgumentException e)
            {
            RestHelper.log(e);
            return Response.status(Response.Status.BAD_REQUEST).entity(BAD_REQUEST_MSG).build();
            }

        Object oResult;
        if (fDirectQuery)
            {
            Set setKeys = keys(sQuery);
            oResult     = cache.aggregate(setKeys, aggregator);
            }
        else
            {
            oResult = cache.aggregate(AlwaysFilter.INSTANCE, aggregator);
            }

        return Response.ok(oResult).build();
        }

    /**
     * Invoke the specified processor against the entries that satisfy the
     * specified criteria. If the query string is empty all cache entries are
     * processed.
     *
     * @param sProc   the name of the processor
     * @param sQuery  where predicate of Coherence Query Language to filter
     *                cache entries (optional)
     *
     * @return a Map containing the results of invoking the EntryProcessor
     *         against the entries that are selected by the given query
     */
    @POST
    @Path("{proc: " + ProcessorRegistry.PROCESSOR_REQUEST_REGEX + "}")
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Response process(
            @PathParam("proc") String sProc,
            @QueryParam("q") String sQuery)
        {
        QueryConfig queryConfig  = m_queryConfig;
        NamedCache  cache        = m_cache;
        boolean     fDirectQuery = sQuery != null && sQuery.length() > 0;

        if (fDirectQuery && !queryConfig.isDirectQueryEnabled())
            {
            return Response.status(Response.Status.FORBIDDEN)
                           .entity("Direct query is not allowed").build();
            }

        InvocableMap.EntryProcessor processor;
        try
            {
            processor = m_processorRegistry.getProcessor(sProc);
            }
        catch (IllegalArgumentException e)
            {
            RestHelper.log(e);
            return Response.status(Response.Status.BAD_REQUEST).entity(BAD_REQUEST_MSG).build();
            }

        Map mapResult;
        if (fDirectQuery)
            {
            Set setKeys = keys(sQuery);
            mapResult   = cache.invokeAll(setKeys, processor);
            }
        else
            {
            mapResult = cache.invokeAll(AlwaysFilter.INSTANCE, processor);
            }

        return Response.ok(mapResult).build();
        }

    /**
     * Register SSE event listener for this cache.
     *
     * @param fLite   flag specifying whether to register for lite or full events
     * @param sQuery  an optional CohQL filter to register listener on
     *
     * @return the EventOutput that will be used to send events to the client
     */
    @GET
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput addListener(@MatrixParam("lite") boolean fLite, @QueryParam("q") String sQuery)
        {
        MapEventOutput eventOutput = new MapEventOutput(m_cache, fLite);
        if (sQuery != null)
            {
            eventOutput.setFilter(QueryHelper.createFilter(sQuery));
            }
        eventOutput.register();

        return eventOutput;
        }

    // ----- sub-resources --------------------------------------------------

    /**
     * Return a REST sub-resource representing either a configured named query
     * or a single cache entry.
     *
     * @param sKey  name of the configured query or referenced entry's key
     *
     * @return REST resource representing either a configured named query or
     *         a single cache entry
     */
    @Path("{key: [^/]+}")
    public Object getEntryOrQueryResource(@PathParam("key") String sKey)
        {
        QueryConfig config = m_queryConfig;
        if (config.containsNamedQuery(sKey))
            {
            return InjectionBinder.inject(instantiateNamedQueryResource(m_cache, config.getNamedQuery(sKey), m_cMaxResults),
                    m_serviceLocator);
            }
        else
            {
            Object oKey;
            try
                {
                oKey = m_keyConverter.fromString(sKey);
                }
            catch (Exception e)
                {
                Logger.warn("Failed to convert the key \"" + sKey + "\" to a " + m_clzKey);
                throw new NotFoundException();
                }
            return InjectionBinder.inject(instantiateEntryResource(m_cache, oKey, m_clzValue), m_serviceLocator);
            }
        }

    /**
     * Return a REST sub-resource representing a set of cache entries.
     *
     * @param sKeys  keys of the referenced entries
     *
     * @return REST sub-resource representing a set of cache entries
     */
    @Path("{keys: \\([^\\)]+\\)}")
    public EntrySetResource getEntrySetResource(@PathParam("keys") String sKeys)
        {
        sKeys = sKeys.substring(1, sKeys.length() - 1);

        KeyConverter converter = m_keyConverter;
        String[]     asKeys    = sKeys.split(",");
        Set          setKeys   = new HashSet(asKeys.length);
        for (String sKey : asKeys)
            {
            try
                {
                setKeys.add(converter.fromString(sKey.trim()));
                }
            catch (Exception e)
                {
                Logger.warn("Failed to convert the key \"" + sKey + "\" to a " + m_clzKey);
                }
            }

        return InjectionBinder.inject(instantiateEntrySetResource(m_cache, setKeys, m_clzValue), m_serviceLocator);
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Returns a collection of extracted values for cache entries that satisfy
     * the criteria expressed by the query.
     *
     * @param sQuery     where predicate of Coherence Query Language to
     *                   filter cache entries. If null, all cache entries
     *                   will be returned.
     * @param extractor  the extractor to apply to each entry in the result set
     * @param nStart     the starting index of result set to be returned
     * @param cResults   the size of result set to be returned (page size)
     * @param sSort      a string expression that represents sort order
     *
     * @return a collection of entries that satisfy the specified criteria
     */
    protected Collection executeQuery(String sQuery, ValueExtractor<Map.Entry, ?> extractor, int nStart, int cResults, String sSort)
        {
        QueryConfig queryConfig      = m_queryConfig;
        boolean     fDirectQuery     = sQuery != null && sQuery.length() > 0;
        DirectQuery directQuery      = queryConfig.getDirectQuery();
        String      sQueryEngine     = directQuery == null ? null : directQuery.getQueryEngineName();
        QueryEngine queryEngine      = m_queryEngineRegistry.getQueryEngine(sQueryEngine);
        int         cQueryMaxResults = (directQuery == null || !fDirectQuery) ? -1 : directQuery.getMaxResults();
        int         cMaxResults      = RestHelper.resolveMaxResults(cResults, cQueryMaxResults, m_cMaxResults);
        Query       query            = queryEngine.prepareQuery(sQuery, null);

        return query.execute(m_cache, extractor, sSort, nStart, cMaxResults);
        }

    /**
     * Returns a set of keys that satisfy the criteria expressed by the query.
     *
     * @param sQuery  query used to filter cache entries
     *
     * @return a set of keys for entries that satisfy the specified criteria
     */
    protected Set keys(String sQuery)
        {
        DirectQuery directQuery  = m_queryConfig.getDirectQuery();
        String      sQueryEngine = directQuery == null ? null : directQuery.getQueryEngineName();
        QueryEngine queryEngine  = m_queryEngineRegistry.getQueryEngine(sQueryEngine);
        Query       query        = queryEngine.prepareQuery(sQuery, null);

        return query.keySet(m_cache);
        }

    /**
     * Create an instance of {@link EntryResource} for the specified resource
     * configuration.
     *
     * @param cache              cache in which referenced entry is stored
     * @param oKey               referenced entry's key
     * @param clzValue           class of the referenced entry's value
     *
     * @return a cache entry resource
     */
    protected EntryResource instantiateEntryResource(NamedCache cache, Object oKey, Class clzValue)
        {
        return new EntryResource(cache, oKey, clzValue);
        }

    /**
     * Create an instance of {@link EntrySetResource} for the specified resource
     * configuration.
     *
     * @param cache               cache that stores the referenced entries
     * @param setKeys             keys of the referenced entries
     * @param clzValue            class of the referenced entries' values
     *
     * @return an entry set resource
     */
    protected EntrySetResource instantiateEntrySetResource(NamedCache cache, Set setKeys, Class clzValue)
        {
        return new EntrySetResource(cache, setKeys, clzValue);
        }

    /**
     * Create an instance of {@link NamedQueryResource} for the specified resource
     * configuration.
     *
     * @param cache               cache to create a resource for
     * @param query               query filtering the cache entries
     * @param cMaxResults         max size of result set for this resource
     *
     * @return a named query resource
     */
    public NamedQueryResource instantiateNamedQueryResource(NamedCache cache, NamedQuery query, int cMaxResults)
        {
        return new NamedQueryResource(cache, query, cMaxResults);
        }

    // ----- data members ---------------------------------------------------

    /**
     * Error message for bad request.
     */
    static final String BAD_REQUEST_MSG = "An exception occurred while processing the request.";

    /**
     * NamedCache wrapped by this resource.
     */
    protected NamedCache m_cache;

    /**
     * Key class for the entries stored in the wrapped cache.
     */
    protected Class m_clzKey;

    /**
     * Value class for the entries stored in the wrapped cache.
     */
    protected Class m_clzValue;

    /**
     * Key converter.
     */
    protected KeyConverter m_keyConverter;

    /**
     * Size of the result set this resource is allowed to return.
     */
    protected int m_cMaxResults;

    /**
     * Marshaller registry to obtain marshallers from.
     */
    @Inject
    protected MarshallerRegistry m_marshallerRegistry;

    /**
     * Query configuration for this resource.
     */
    protected QueryConfig m_queryConfig;

    /**
     * Query engine registry to obtain query engines from.
     */
    @Inject
    protected QueryEngineRegistry m_queryEngineRegistry;

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

    /**
     * The ServiceLocator for this resource.
     */
    @Inject
    protected ServiceLocator m_serviceLocator;
    }
