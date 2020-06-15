/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.tangosol.coherence.rest.util.PropertySet;

import com.tangosol.coherence.rest.util.aggregator.AggregatorRegistry;

import com.tangosol.coherence.rest.util.processor.ProcessorRegistry;

import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.SimpleMapEntry;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.processor.ConditionalRemove;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import java.util.stream.Collectors;

import javax.inject.Inject;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * REST resource representing a set of cache entries.
 *
 * @author as  2011.06.28
 */
@SuppressWarnings("unchecked")
public class EntrySetResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an EntrySetResource.
     *
     * @param cache               cache that stores the referenced entries
     * @param setKeys             keys of the referenced entries
     * @param clzValue            class of the referenced entries' values
     */
    public EntrySetResource(NamedCache cache, Set setKeys, Class clzValue)
        {
        m_cache    = cache;
        m_setKeys  = setKeys;
        m_clzValue = clzValue;
        }

    // ----- resource methods -----------------------------------------------

    /**
     * Return the entries' values or a subset of their properties.
     *
     * @param propertySet  properties to return (if null, values will be
     *                     returned)
     *
     * @return entries' values or a subset of their properties
     */
    @GET
    @Produces({APPLICATION_JSON, APPLICATION_XML, APPLICATION_OCTET_STREAM})
    public Response getValues(@MatrixParam("p") PropertySet propertySet)
        {
        Collection colValues = values();
        return Response.ok(propertySet == null
                ? colValues
                : propertySet.extract(colValues)).build();
        }

    @GET
    @Path("entries")
    @Produces({APPLICATION_JSON, APPLICATION_XML, TEXT_PLAIN})
    public Response getEntries(@MatrixParam("p") PropertySet propertySet)
        {
        Map<Object, Object> mapResults = m_cache.getAll(m_setKeys);
        Collection colResults = mapResults.entrySet().stream()
                .map(entry -> propertySet == null
                              ? entry
                              : new SimpleMapEntry<>(entry.getKey(), propertySet.extract(entry.getValue())))
                .collect(Collectors.toList());

        return Response.ok(colResults).build();
        }

    /**
     * Perform an aggregating operation against the entries.
     *
     * @param sAggr     name of the aggregator
     *
     * @return the result of the aggregation
     */
    @GET
    @Path("{aggr: " + AggregatorRegistry.AGGREGATOR_REQUEST_REGEX + "}")
    @Produces({APPLICATION_JSON, APPLICATION_XML, TEXT_PLAIN})
    public Response aggregate(@PathParam("aggr") String sAggr)
        {
        InvocableMap.EntryAggregator aggr = m_aggregatorRegistry.getAggregator(sAggr);

        Object oResult = m_cache.aggregate(m_setKeys, aggr);
        return Response.ok(oResult).build();
        }

    /**
     * Invoke the specified processor against the entries.
     *
     * @param sProc     name of the processor
     *
     * @return a Map containing the results of invoking the EntryProcessor
     *         against the entries
     */
    @POST
    @Path("{proc: " + ProcessorRegistry.PROCESSOR_REQUEST_REGEX + "}")
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Response process(@PathParam("proc") String sProc)
        {
        InvocableMap.EntryProcessor proc = m_processorRegistry.getProcessor(sProc);

        Map mapResult = m_cache.invokeAll(m_setKeys, proc);
        return Response.ok(mapResult).build();
        }

    /**
     * Remove the entries.
     *
     * @return response with a status of 200 (OK)
     */
    @DELETE
    public Response delete()
        {
        remove();
        return Response.ok().build();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Get the entries' values.
     *
     * @return entries' values
     */
    protected Collection values()
        {
        return m_cache.getAll(m_setKeys).values();
        }

    /**
     * Remove the entries from the cache.
     */
    protected void remove()
        {
        m_cache.invokeAll(m_setKeys, new ConditionalRemove(AlwaysFilter.INSTANCE));
        }

    // ---- data members -----------------------------------------------------

    /**
     * Cache which stores the referenced entries.
     */
    protected NamedCache m_cache;

    /**
     * Referenced entries' keys.
     */
    protected Set m_setKeys;

    /**
     * Class of the referenced entries' values.
     */
    protected Class m_clzValue;

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
