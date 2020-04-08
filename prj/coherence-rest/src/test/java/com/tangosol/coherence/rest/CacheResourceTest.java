/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.tangosol.coherence.rest.config.DirectQuery;
import com.tangosol.coherence.rest.config.QueryConfig;

import com.tangosol.coherence.rest.io.MarshallerRegistry;

import com.tangosol.coherence.rest.query.QueryEngineRegistry;

import com.tangosol.coherence.rest.util.aggregator.AggregatorRegistry;

import com.tangosol.coherence.rest.util.processor.ProcessorRegistry;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.WrapperNamedCache;

import data.pof.Person;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link CacheResource}.
 *
 * @author ic  2011.06.30
 */
@SuppressWarnings({"unchecked", "deprecation"})
public class CacheResourceTest
    {
    @Before
    public void setUp()
        {
        m_cache = new WrapperNamedCache(new HashMap<Integer, Person>(), "persons");
        m_cache.put(1, new Person("Ivan", new Date(78, 3, 25), 36));
        m_cache.put(2, new Person("Aleks", new Date(74, 7, 24), 39));
        m_cache.put(3, new Person("Vaso", new Date(74, 7, 7), 40));
        }

    @Test
    public void testSimpleGet()
        {
        CacheResource resource = createCacheResource(m_cache);
        Response      response = resource.getValues(0, -1, null, null, null);

        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(3, ((Collection) response.getEntity()).size());
        }

    @Test
    public void testRangeGet()
        {
        CacheResource resource = createCacheResource(m_cache);
        Response      response = resource.getValues(0, 1, null, null, null);

        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(1, ((List) response.getEntity()).size());
        }

    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    @Test
    public void testNaturalSortGet()
        {
        NamedCache cache = new WrapperNamedCache(new HashMap<Integer, Integer>(), "comparables");
        cache.put(1, 1);
        cache.put(3, 3);
        cache.put(2, 2);

        CacheResource resource = createCacheResource(cache);
        Response      response = resource.getValues(0, -1, null, null, null);

        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(Arrays.asList(1,2,3), response.getEntity());
        }

    @Test
    public void testSortGet()
        {
        CacheResource resource = createCacheResource(m_cache);
        Response      response = resource.getValues(0, -1, "name:desc", null, null);

        assertEquals(200 /* OK */, response.getStatus());
        List<Person> listPersons = (List<Person>) response.getEntity();
        assertEquals("Vaso",  listPersons.get(0).getName());
        assertEquals("Ivan",  listPersons.get(1).getName());
        assertEquals("Aleks", listPersons.get(2).getName());
        }

    @Test
    public void testSortGetEntries()
        {
        CacheResource resource = createCacheResource(m_cache);
        Response      response = resource.getEntries(0, -1, "name:desc", null, null);

        assertEquals(200 /* OK */, response.getStatus());
        List<Map.Entry<Integer, Person>> listPersons = (List<Map.Entry<Integer, Person>>) response.getEntity();
        assertEquals("Vaso", listPersons.get(0).getValue().getName());
        assertEquals("Ivan",  listPersons.get(1).getValue().getName());
        assertEquals("Aleks", listPersons.get(2).getValue().getName());
        }

    @Test
    public void testBadSortGet()
        {
        CacheResource resource = createCacheResource(m_cache);

        Response      response = resource.getValues(0, -1, "name:INVALID", null, null);

        assertEquals(400 /* OK */, response.getStatus());
        assertTrue(response.getEntity().equals("An exception occurred while processing the request."));

        response = resource.getValues(0, -1, "BAD_PROPERTY:asc", null, null);
        assertEquals(400 /* OK */, response.getStatus());
        assertTrue(response.getEntity().equals("An exception occurred while processing the request."));
        }

    @Test
    public void testPartialGet()
        {
        CacheResource resource = createCacheResource(m_cache);
        Response      response = resource.getValues(0, -1, null, com.tangosol.coherence.rest.util.PropertySet.fromString("name"), null);

        assertEquals(200 /* OK */, response.getStatus());

        Collection<com.tangosol.coherence.rest.util.PartialObject> colPartials = (Collection<com.tangosol.coherence.rest.util.PartialObject>) response.getEntity();
        assertEquals(3, colPartials.size());
        for (com.tangosol.coherence.rest.util.PartialObject o : colPartials)
            {
            assertNotNull(o.get("name"));
            assertNull(o.get("dateOfBirth"));
            }
        }

    @Test
    public void testQueryGet()
        {
        QueryConfig queryConfig = new QueryConfig();
        queryConfig.setDirectQuery(new DirectQuery(null, -1));

        CacheResource resource = createCacheResource(m_cache);
        resource.m_queryConfig = queryConfig;

        Response      response = resource.getValues(0, -1, null, null, "name is \"Ivan\"");

        assertEquals(200 /* OK */, response.getStatus());
        List<Person> listPersons = (List<Person>) response.getEntity();
        assertEquals(1, listPersons.size());
        assertEquals("Ivan", listPersons.get(0).getName());
        }

    @Test
    public void testBadQueryGet()
        {
        QueryConfig queryConfig = new QueryConfig();
        queryConfig.setDirectQuery(new DirectQuery(null, -1));

        CacheResource resource = createCacheResource(m_cache);
        resource.m_queryConfig = queryConfig;

        Response      response = resource.getValues(0, -1, null, null, "invalid COH query");

        assertEquals(400 /* Bad Request */, response.getStatus());
        assertTrue(response.getEntity().equals("An exception occurred while processing the request."));
        }

    @Test
    public void testCacheAggregation()
        {
        CacheResource resource = createCacheResource(m_cache);
        Response      response = resource.aggregate("long-sum(age)", null);
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(36 + 39 + 40L, response.getEntity());

        response = resource.aggregate("comparable-max(dateOfBirth)", null);
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(new Date(78, 3, 25), response.getEntity());
        }

    @Test
    public void testCustomAggregator()
        {
        CacheResource resource = createCacheResource(m_cache);
        resource.m_aggregatorRegistry.register("my-aggr", asArgs -> entries -> "hoop");

        Response response = resource.aggregate("my-aggr(a,b,c)", null);
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals("hoop", response.getEntity());
        }

    @Test
    public void testFilterAggregation()
        {
        QueryConfig queryConfig = new QueryConfig();
        queryConfig.setDirectQuery(new DirectQuery(null, -1));

        CacheResource resource = createCacheResource(m_cache);
        resource.m_queryConfig = queryConfig;

        Response      response = resource.aggregate("long-sum(age)", "name != \"Ivan\"");
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(39 + 40L, response.getEntity());

        response = resource.aggregate("comparable-max(dateOfBirth)", "name != \"Ivan\"");
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(new Date(74, 7, 24), response.getEntity());
        }

    @Test
    public void testIdentityAggregation()
        {
        NamedCache<Integer, Integer> cache = new WrapperNamedCache<>(new HashMap<>(), "comparables");
        cache.put(1, 1);
        cache.put(3, 3);
        cache.put(2, 2);

        CacheResource resource = createCacheResource(cache);
        Response      response = resource.aggregate("long-max()", null);
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(3L, response.getEntity());

        response = resource.aggregate("count()", null);
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(3, response.getEntity());
        }

    @Test
    public void testBadAggregator()
        {
        CacheResource resource = createCacheResource(m_cache);
        Response      response = resource.aggregate("bad-aggr(dateOfBirth)", null);
        assertEquals(400 /* Bad Request */, response.getStatus());
        }

    @Test
    public void testCacheProcessing()
        {
        NamedCache cache = new WrapperNamedCache(new HashMap<Integer, Integer>(), "comparables");
        cache.put(1, 1);
        cache.put(3, 3);
        cache.put(2, 2);

        CacheResource resource = createCacheResource(cache);
        Response      response = resource.process("increment(1)", null);
        assertEquals(200 /* OK */, response.getStatus());

        Map mapResults = (Map) response.getEntity();
        assertEquals(2, mapResults.get(1));
        assertEquals(3, mapResults.get(2));
        assertEquals(4, mapResults.get(3));

        response = resource.process("post-increment(-1)", null);
        assertEquals(200 /* OK */, response.getStatus());

        mapResults = (Map) response.getEntity();
        assertEquals(2, mapResults.get(1));
        assertEquals(3, mapResults.get(2));
        assertEquals(4, mapResults.get(3));

        response = resource.process("multiply(5)", null);
        assertEquals(200 /* OK */, response.getStatus());

        mapResults = (Map) response.getEntity();
        assertEquals(5, mapResults.get(1));
        assertEquals(10, mapResults.get(2));
        assertEquals(15, mapResults.get(3));
        }

    @Test
    public void testCustomProcessor()
        {
        CacheResource resource = createCacheResource(m_cache);
        resource.m_processorRegistry.register("my-proc", asArgs -> entry -> Integer.valueOf(entry.getKey() + "") + 1);

        Response response = resource.process("my-proc(a,b,c)", null);
        assertEquals(200 /* OK */, response.getStatus());

        Map mapResults = (Map) response.getEntity();
        assertEquals(2, mapResults.get(1));
        assertEquals(3, mapResults.get(2));
        assertEquals(4, mapResults.get(3));
        }

    @Test
    public void testFilterProcessing()
        {
        QueryConfig queryConfig = new QueryConfig();
        queryConfig.setDirectQuery(new DirectQuery("DEFAULT", -1));

        CacheResource resource = createCacheResource(m_cache);
        resource.m_queryConfig = queryConfig;

        Response      response = resource.process("increment(age, 1)", "name != \"Ivan\"");
        assertEquals(200 /* OK */, response.getStatus());

        Map mapResults = (Map) response.getEntity();
        assertEquals(40, mapResults.get(2));
        assertEquals(41, mapResults.get(3));
        }

    @Test
    public void testBadProcessor()
        {
        CacheResource resource = createCacheResource(m_cache);

        Response response = resource.process("my-proc()", null);
        assertEquals(400 /* Bad Request */, response.getStatus());
        }

    // ---- helper methods --------------------------------------------------

    protected CacheResource createCacheResource(NamedCache cache)
        {
        CacheResource resource = new CacheResource(
                cache, Integer.class, Person.class, null, new QueryConfig(), -1);

        resource.m_aggregatorRegistry  = new AggregatorRegistry();
        resource.m_processorRegistry   = new ProcessorRegistry();
        resource.m_marshallerRegistry  = new MarshallerRegistry();
        resource.m_queryEngineRegistry = new QueryEngineRegistry();

        return resource;
        }

    // ---- data members ----------------------------------------------------

    protected NamedCache m_cache;
    }
