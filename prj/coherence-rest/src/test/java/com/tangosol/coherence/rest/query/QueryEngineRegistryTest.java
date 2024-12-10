/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.query;

import com.tangosol.coherence.rest.config.QueryEngineConfig;

import com.tangosol.net.NamedCache;

import com.tangosol.util.ValueExtractor;

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link QueryEngineRegistry}.
 *
 * @author ic   2011.12.16
 */
public class QueryEngineRegistryTest
    {
    /**
     * Test for {@link QueryEngineRegistry#QueryEngineRegistry()}
     */
    @Test
    public void testDefaultCtor()
        {
        QueryEngineRegistry registry = new QueryEngineRegistry();
        // check the default query engine
        assertTrue(registry.getQueryEngine("DEFAULT") instanceof CoherenceQueryLanguageEngine);
        }

    /**
     * Test for {@link QueryEngineRegistry#QueryEngineRegistry(Collection)}
     */
    @Test
    public void testCtor()
        {
        QueryEngineConfig   config   = new QueryEngineConfig("test-engine", NullQueryEngine.class);
        QueryEngineRegistry registry = new QueryEngineRegistry(Collections.singleton(config));

        // check registered query engines
        assertTrue(registry.getQueryEngine("DEFAULT") instanceof CoherenceQueryLanguageEngine);
        assertTrue(registry.getQueryEngine("test-engine") instanceof NullQueryEngine);
        }

    /**
     * Test for {@link QueryEngineRegistry#registerQueryEngine(String, QueryEngine)}
     */
    @Test
    public void testRegisterQueryEngine()
        {
        QueryEngineRegistry registry = new QueryEngineRegistry();
        registry.registerQueryEngine("test-engine", new NullQueryEngine());

        assertTrue(registry.getQueryEngine("test-engine") instanceof NullQueryEngine);
        }

    /**
     * Test for {@link QueryEngineRegistry#getQueryEngine(String)}
     */
    @Test
    public void testGetQueryEngine()
        {
        QueryEngineRegistry registry = new QueryEngineRegistry();
        registry.registerQueryEngine("test-engine", NullQueryEngine.class);

        // assert registered query engine
        assertSame(NullQueryEngine.class, registry.getQueryEngine("test-engine").getClass());
        // assert fail safe mechanism
        assertSame(CoherenceQueryLanguageEngine.class, registry.getQueryEngine("not-registered").getClass());
        }

    /**
     * Test for {@link QueryEngineRegistry#registerQueryEngine(String, Class)}
     * with object not implementing QueryEngine interface
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterBadClass()
        {
        QueryEngineRegistry registry = new QueryEngineRegistry();
        registry.registerQueryEngine("test-engine", Object.class);
        }

    /**
     * Test for {@link QueryEngineRegistry#registerQueryEngine(String, Class)}
     * with object not implementing QueryEngine interface
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNull()
        {
        QueryEngineRegistry registry = new QueryEngineRegistry();
        registry.registerQueryEngine("test-engine", (Class) null);
        }

    /**
     * Null implementation of {@link QueryEngine} interface.
     */
    public static class NullQueryEngine
            implements QueryEngine
        {
        public Query prepareQuery(String sQuery, Map<String, Object> mapParams)
            {
            return new NullQuery();
            }

        private static class NullQuery
                implements Query
            {
            public <E> Collection<E> execute(NamedCache cache, ValueExtractor<Map.Entry, ? extends E> extractor, String sOrder, int nStart, int Count)
                {
                return null;
                }

            public Set keySet(NamedCache cache)
                {
                return null;
                }
            }
        }
    }
