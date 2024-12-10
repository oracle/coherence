/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.tangosol.coherence.rest.config.NamedQuery;

import com.tangosol.coherence.rest.query.QueryEngineRegistry;

import com.tangosol.coherence.rest.util.PartialObject;
import com.tangosol.coherence.rest.util.PropertySet;

import com.tangosol.coherence.rest.util.aggregator.AggregatorFactory;
import com.tangosol.coherence.rest.util.aggregator.AggregatorRegistry;

import com.tangosol.coherence.rest.util.processor.ProcessorFactory;
import com.tangosol.coherence.rest.util.processor.ProcessorRegistry;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.InvocableMap;

import com.tangosol.util.processor.AbstractProcessor;

import data.pof.Person;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link com.tangosol.coherence.rest.NamedQueryResource}.
 *
 * @author vp  2011.12.21
 */
@SuppressWarnings({"unchecked", "deprecation"})
public class NamedQueryResourceTest
    {
    @Before
    public void setUp()
        {
        m_cache = new WrapperNamedCache(new HashMap<Integer, Person>(), "persons");
        m_cache.put(1, new Person("Ivan",  new Date(78, 3, 25), 36));
        m_cache.put(2, new Person("Aleks", new Date(74, 7, 24), 39));
        m_cache.put(3, new Person("Vaso", new Date(74, 7, 7), 40));
        }

    @Test
    public void testSimpleGet()
        {
        NamedQuery         query     = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource  = createNamedQueryResource(m_cache, query, -1);
        UriInfo            uriInfo   = getUriInfo(new MultivaluedHashMap());
        Response           response  = resource.getValues(uriInfo, 0, -1, null, null);

        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(3, ((Collection) response.getEntity()).size());
        }

    @Test
    public void testRangeGet()
        {
        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.getValues(uriInfo, 1, 1, "name", null);

        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(1, ((List) response.getEntity()).size());
        }

    @Test
    public void testSortGet()
        {
        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.getValues(uriInfo, 0, -1, "name:desc", null);

        assertEquals(200 /* OK */, response.getStatus());
        List<Person> listPersons = (List<Person>) response.getEntity();
        assertEquals("Vaso",  listPersons.get(0).getName());
        assertEquals("Ivan",  listPersons.get(1).getName());
        assertEquals("Aleks", listPersons.get(2).getName());
        }

    @Test
    public void testSortGetEntries()
        {
        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.getEntries(uriInfo, 0, -1, "name:desc", null);

        assertEquals(200 /* OK */, response.getStatus());
        List<Map.Entry<Integer, Person>> listPersons = (List<Map.Entry<Integer, Person>>) response.getEntity();
        assertEquals("Vaso",  listPersons.get(0).getValue().getName());
        assertEquals("Ivan",  listPersons.get(1).getValue().getName());
        assertEquals("Aleks", listPersons.get(2).getValue().getName());
        }

    @Test
    public void testBadSortGet()
        {
        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response response           = resource.getValues(uriInfo, 0, -1, "name:INVALID", null);

        assertEquals(400 /* OK */, response.getStatus());
        assertTrue(response.getEntity().equals("An exception occurred while processing the request."));

        response = resource.getValues(uriInfo, 0, -1, "BAD_PROPERTY:asc", null);
        assertEquals(400 /* OK */, response.getStatus());
        assertTrue(response.getEntity().equals("An exception occurred while processing the request."));
        }

    @Test
    public void testPartialGet()
        {
        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.getValues(uriInfo, 0, -1, null, PropertySet.fromString("name"));

        assertEquals(200 /* OK */, response.getStatus());

        Collection<PartialObject> colPartials = (Collection<PartialObject>) response.getEntity();
        assertEquals(3, colPartials.size());
        for (PartialObject o : colPartials)
            {
            assertNotNull(o.get("name"));
            assertNull(o.get("dateOfBirth"));
            }
        }

    @Test
    public void testParamQueryGet()
        {
        NamedQuery         query    = new NamedQuery("age-query", "name is :name", "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        MultivaluedMap     params   = new MultivaluedHashMap();
        params.add("name", "Ivan");
        UriInfo  uriInfo  = getUriInfo(params);
        Response response = resource.getValues(uriInfo, 0, -1, null, null);

        assertEquals(200 /* OK */, response.getStatus());
        List<Person> listPersons = (List<Person>) response.getEntity();
        assertEquals(1, listPersons.size());
        assertEquals("Ivan", listPersons.get(0).getName());
        }

    @Test
    public void testBadParamTypeQueryGet()
        {
        NamedQuery         query    = new NamedQuery("age-query", "age is :age;i", "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        MultivaluedMap     params   = new MultivaluedHashMap();
        params.add("age", "Ivan");
        UriInfo  uriInfo  = getUriInfo(params);
        Response response = resource.getValues(uriInfo, 0, -1, null, null);

        assertEquals(400 /* Bad Request */, response.getStatus());
        assertTrue(response.getEntity().equals("An exception occurred while processing the request."));
        }

    @Test
    public void testAggregation()
        {
        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.aggregate(uriInfo, "long-sum(age)");

        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(36+39+40L, response.getEntity());

        response = resource.aggregate(uriInfo, "comparable-max(dateOfBirth)");
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(new Date(78, 3, 25), response.getEntity());
        }

    @Test
    public void testCustomAggregator()
        {
        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        resource.m_aggregatorRegistry.register("my-aggr", new AggregatorFactory()
            {
            public InvocableMap.EntryAggregator getAggregator(String... asArgs)
                {
                return new InvocableMap.EntryAggregator()
                    {
                    public Object aggregate(Set entries)
                        {
                        return "hoop";
                        }
                    };
                }
            });

        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.aggregate(uriInfo, "my-aggr(a,b,c)");
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals("hoop", response.getEntity());
        }

    @Test
    public void testFilterAggregation()
        {
        NamedQuery         query    = new NamedQuery("age-query", "name is :name1 OR name is :name2", "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        MultivaluedMap     params   = new MultivaluedHashMap();
        params.add("name1", "Ivan");
        params.add("name2", "Vaso");
        UriInfo  uriInfo  = getUriInfo(params);
        Response response = resource.aggregate(uriInfo, "long-sum(age)");

        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(36+40L, response.getEntity());

        query    = new NamedQuery("age-query", "name != :name", "DEFAULT", 10);
        resource = createNamedQueryResource(m_cache, query, -1);
        params   = new MultivaluedHashMap();
        params.add("name", "Ivan");
        uriInfo  = getUriInfo(params);
        response = resource.aggregate(uriInfo, "comparable-max(dateOfBirth)");

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

        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(cache, query, -1);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.aggregate(uriInfo, "long-max()");
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(3L, response.getEntity());

        response = resource.aggregate(uriInfo, "count()");
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(3, response.getEntity());
        }

    @Test
    public void testBadAggregator()
        {
        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.aggregate(uriInfo, "bad-aggr(dateOfBirth)");

        assertEquals(400 /* Bad Request */, response.getStatus());
        }

    @Test
    public void testCacheProcessing()
        {
        NamedCache cache = new WrapperNamedCache(new HashMap<Integer, Integer>(), "comparables");
        cache.put(1, 1);
        cache.put(3, 3);
        cache.put(2, 2);

        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(cache, query, -1);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.process(uriInfo, "increment(1)");
        assertEquals(200 /* OK */, response.getStatus());

        Map mapResults = (Map) response.getEntity();
        assertEquals(2, mapResults.get(1));
        assertEquals(3, mapResults.get(2));
        assertEquals(4, mapResults.get(3));

        response = resource.process(uriInfo, "post-increment(-1)");
        assertEquals(200 /* OK */, response.getStatus());

        mapResults = (Map) response.getEntity();
        assertEquals(2, mapResults.get(1));
        assertEquals(3, mapResults.get(2));
        assertEquals(4, mapResults.get(3));

        response = resource.process(uriInfo, "multiply(5)");
        assertEquals(200 /* OK */, response.getStatus());

        mapResults = (Map) response.getEntity();
        assertEquals(5, mapResults.get(1));
        assertEquals(10, mapResults.get(2));
        assertEquals(15, mapResults.get(3));
        }

    @Test
    public void testCustomProcessor()
        {
        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        resource.m_processorRegistry.register("my-proc", new ProcessorFactory()
            {
            public InvocableMap.EntryProcessor getProcessor(String... asArgs)
                {
                return new AbstractProcessor()
                    {
                    public Object process(InvocableMap.Entry entry)
                        {
                        return Integer.valueOf(entry.getKey() + "") + 1;
                        }
                    };
                }
            });

        UriInfo  uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response response = resource.process(uriInfo, "my-proc(a,b,c)");
        assertEquals(200 /* OK */, response.getStatus());

        Map mapResults = (Map) response.getEntity();
        assertEquals(2, mapResults.get(1));
        assertEquals(3, mapResults.get(2));
        assertEquals(4, mapResults.get(3));
        }

    @Test
    public void testFilterProcessing()
        {
        NamedQuery         query    = new NamedQuery("name-query", "name != \"${name}\"", "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        MultivaluedMap     params   = new MultivaluedHashMap();
        params.add("name", "Ivan");
        UriInfo  uriInfo  = getUriInfo(params);

        Response response = resource.process(uriInfo, "increment(age, 1)");
        assertEquals(200 /* OK */, response.getStatus());

        Map mapResults = (Map) response.getEntity();
        assertEquals(40, mapResults.get(2));
        assertEquals(41, mapResults.get(3));
        }

    @Test
    public void testBadProcessor()
        {
        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", 10);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.process(uriInfo, "my-proc()");

        assertEquals(400 /* Bad Request */, response.getStatus());
        }

    @Test
    public void testMaxResults()
        {
        NamedCache cache = new WrapperNamedCache(new HashMap<Integer, Integer>(), "paging");
        for (int i=0; i<30; i++)
            {
            cache.put(i, i);
            }

        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", -1);
        NamedQueryResource resource = createNamedQueryResource(cache, query, 25);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.getValues(uriInfo, 0, -1, null, null);

        // test resource max results limit
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(25, ((Collection) response.getEntity()).size());

        // test named query max results limit
        query    = new NamedQuery("named-query", null, "DEFAULT", 20);
        resource = createNamedQueryResource(cache, query, 25);
        response = resource.getValues(uriInfo, 0, -1, null, null);
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(20, ((Collection) response.getEntity()).size());

        // test url max results limit
        query    = new NamedQuery("named-query", null, "DEFAULT", 20);
        resource = createNamedQueryResource(cache, query, 25);
        response = resource.getValues(uriInfo, 0, 15, null, null);
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals(15, ((Collection) response.getEntity()).size());
        }

    @Test
    public void testPaging()
        {
        NamedCache cache = new WrapperNamedCache(new TreeMap<Integer, Integer>(), "paging");
        for (int i=0; i<30; i++)
            {
            cache.put(i, i);
            }

        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", -1);
        NamedQueryResource resource = createNamedQueryResource(cache, query, 25);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.getValues(uriInfo, 0, 4, null, null);

        assertEquals(200 /* OK */, response.getStatus());
        Collection col = (Collection) response.getEntity();
        assertEquals(4, col.size());
        assertTrue(col.contains(0));
        assertTrue(col.contains(1));
        assertTrue(col.contains(2));
        assertTrue(col.contains(3));

        response = resource.getValues(uriInfo, 10, 4, null, null);
        assertEquals(200 /* OK */, response.getStatus());
        col = (Collection) response.getEntity();
        assertEquals(4, col.size());
        assertTrue(col.contains(10));
        assertTrue(col.contains(11));
        assertTrue(col.contains(12));
        assertTrue(col.contains(13));
        }

    @Test
    public void testGetKeys()
        {
        NamedQuery         query    = new NamedQuery("named-query", null, "DEFAULT", -1);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        UriInfo            uriInfo  = getUriInfo(new MultivaluedHashMap());
        Response           response = resource.getKeys(uriInfo);

        assertEquals(200 /* OK */, response.getStatus());
        Set keys = (Set) response.getEntity();
        assertEquals(3, keys.size());
        assertTrue(keys.contains(1));
        assertTrue(keys.contains(2));
        assertTrue(keys.contains(3));
        }

    @Test
    public void testGetKeysParameterized()
        {
        NamedQuery         query    = new NamedQuery("age-query", "name is :name1 OR name is :name2", "DEFAULT", -1);
        NamedQueryResource resource = createNamedQueryResource(m_cache, query, -1);
        MultivaluedMap     params   = new MultivaluedHashMap();
        params.add("name1", "Ivan");
        params.add("name2", "Vaso");
        UriInfo            uriInfo  = getUriInfo(params);
        Response           response = resource.getKeys(uriInfo);

        assertEquals(200 /* OK */, response.getStatus());
        Set keys = (Set) response.getEntity();
        assertEquals(2, keys.size());
        assertTrue(keys.contains(1));
        assertTrue(keys.contains(3));
        }

    // ---- helper methods --------------------------------------------------

    protected NamedQueryResource createNamedQueryResource(NamedCache cache, NamedQuery query, int cMaxResults)
        {
        NamedQueryResource resource = new NamedQueryResource(cache, query, cMaxResults);

        resource.m_aggregatorRegistry  = new AggregatorRegistry();
        resource.m_processorRegistry   = new ProcessorRegistry();
        resource.m_registry            = new QueryEngineRegistry();

        return resource;
        }

    protected UriInfo getUriInfo(final MultivaluedMap<String, String> queryParameters)
        {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).then(invocation -> queryParameters);
        return  uriInfo;
        }

    // ---- data members ----------------------------------------------------

    protected NamedCache m_cache;
    }
