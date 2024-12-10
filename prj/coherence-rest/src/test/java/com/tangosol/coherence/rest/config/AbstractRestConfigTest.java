/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.config;

import com.tangosol.coherence.rest.query.QueryEngineRegistry;
import com.tangosol.coherence.rest.query.QueryEngineRegistryTest.NullQueryEngine;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link RestConfig}.
 *
 * @author ic  2011.12.16
 */
public abstract class AbstractRestConfigTest
    {
    protected abstract String getConfigFile();

    @Before
    public void setup()
        {
        System.setProperty(RestConfig.DESCRIPTOR_PROPERTY, getConfigFile());
        m_restConfig = RestConfig.create();
        }

    @Test
    public void testName()
        {
        assertNull(m_restConfig.getResources().get("test-cache"));
        assertNull(m_restConfig.getResources().get("test-alias"));
        assertNotNull(m_restConfig.getResources().get("test-name"));
        }

    @Test
    public void testNamedQuery()
        {
        ResourceConfig resourceConfig = m_restConfig.getResources().get("test-cache-named-query");

        NamedQuery query = resourceConfig.getQueryConfig().getNamedQuery("query-no-engine");
        assertEquals("query-no-engine", query.getName());
        assertEquals("query-no-engine-expression", query.getExpression());
        assertEquals("", query.getQueryEngineName());
        assertEquals(100, query.getMaxResults());

        query = resourceConfig.getQueryConfig().getNamedQuery("query-default-engine");
        assertEquals("query-default-engine", query.getName());
        assertEquals("query-default-engine-expression", query.getExpression());
        assertEquals("DEFAULT", query.getQueryEngineName());
        assertEquals(-1, query.getMaxResults());

        query = resourceConfig.getQueryConfig().getNamedQuery("query-custom-engine");
        assertEquals("query-custom-engine", query.getName());
        assertEquals("query-custom-engine-expression", query.getExpression());
        assertEquals("null-engine", query.getQueryEngineName());
        assertEquals(-1, query.getMaxResults());
        }

    @Test
    public void testDirectQuery()
        {
        ResourceConfig resourceConfig = m_restConfig.getResources().get("test-cache-direct-query1");
        assertTrue(resourceConfig.getQueryConfig().isDirectQueryEnabled());
        DirectQuery directQuery = resourceConfig.getQueryConfig().getDirectQuery();
        assertEquals(500, directQuery.getMaxResults());
        assertEquals("null-engine", directQuery.getQueryEngineName());

        resourceConfig = m_restConfig.getResources().get("test-cache-direct-query2");
        assertTrue(resourceConfig.getQueryConfig().isDirectQueryEnabled());
        directQuery = resourceConfig.getQueryConfig().getDirectQuery();
        assertEquals(-1, directQuery.getMaxResults());
        assertEquals("", directQuery.getQueryEngineName());

        resourceConfig = m_restConfig.getResources().get("test-cache-direct-query3");
        assertFalse(resourceConfig.getQueryConfig().isDirectQueryEnabled());
        }

    @Test
    public void testQueryEngine()
        {
        QueryEngineRegistry registry = m_restConfig.getQueryEngineRegistry();
        assertTrue(registry.getQueryEngine("null-engine") instanceof NullQueryEngine);
        }

    @Test
    public void testMaxResults()
        {
        assertEquals(1000, m_restConfig.getResources().get("test-cache-direct-query1").getMaxResults());
        assertEquals(-1, m_restConfig.getResources().get("test-cache-direct-query2").getMaxResults());
        }

    private RestConfig m_restConfig;
    }
