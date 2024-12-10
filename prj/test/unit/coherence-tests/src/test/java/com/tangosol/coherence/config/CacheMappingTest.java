/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.coherence.config.CacheMapping;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit Tests for {@link CacheMapping}s.
 *
 * @author bo  2011.07.10
 */
public class CacheMappingTest
    {
    /**
     * Ensure that a new {@link CacheMapping} is built with the correct state.
     */
    @Test
    public void testCacheMappingUsingAWildcardPattern()
        {
        CacheMapping mapping = new CacheMapping("dist-*", "DistributedScheme");

        assertEquals("dist-*", mapping.getNamePattern());
        assertTrue(mapping.usesWildcard());
        assertEquals("DistributedScheme", mapping.getSchemeName());
        assertTrue(mapping.isForName("dist-me"));
        assertTrue(mapping.isForName("dist-"));
        assertFalse(mapping.isForName("dist"));

        assertEquals("dist-me", mapping.getNameUsing("me"));
        assertEquals("dist-*", mapping.getNameUsing("*"));

        assertEquals("me", mapping.getWildcardMatch("dist-me"));
        assertEquals("*", mapping.getWildcardMatch("dist-*"));
        assertNull(mapping.getWildcardMatch("repl-*"));
        assertNull(mapping.getWildcardMatch("*"));
        assertNull(mapping.getWildcardMatch(""));
        assertNull(mapping.getWildcardMatch(null));
        assertEquals("", mapping.getWildcardMatch("dist-"));

        assertNull(mapping.getResourceRegistry().getResource(String.class, "message"));
        }

    /**
     * Ensure that a new {@link CacheMapping} is built with the correct state
     * using a cache name pattern of simply a * wildcard.
     */
    @Test
    public void testCacheMappingUsingOnlyAWildcard()
        {
        CacheMapping mapping = new CacheMapping("*", "DistributedScheme");

        assertEquals("*", mapping.getNamePattern());
        assertTrue(mapping.usesWildcard());
        assertEquals("DistributedScheme", mapping.getSchemeName());
        assertTrue(mapping.isForName("dist-me"));
        assertTrue(mapping.isForName("dist-"));
        assertTrue(mapping.isForName("dist"));
        assertTrue(mapping.isForName("*"));

        assertEquals("me", mapping.getNameUsing("me"));
        assertEquals("*", mapping.getNameUsing("*"));

        assertEquals("dist-me", mapping.getWildcardMatch("dist-me"));
        assertEquals("dist-*", mapping.getWildcardMatch("dist-*"));
        assertEquals("*", mapping.getWildcardMatch("*"));
        assertNull(mapping.getWildcardMatch(""));
        assertNull(mapping.getWildcardMatch(null));

        assertNull(mapping.getResourceRegistry().getResource(String.class, "message"));
        }

    /**
     * Ensure that a new {@link CacheMapping} is built with the correct state
     * when not using a wildcard.
     */
    @Test
    public void testCacheMappingUsingASpecificPattern()
        {
        CacheMapping mapping = new CacheMapping("dist-fred", "DistributedScheme");

        assertEquals("dist-fred", mapping.getNamePattern());
        assertFalse(mapping.usesWildcard());
        assertEquals("DistributedScheme", mapping.getSchemeName());
        assertTrue(mapping.isForName("dist-fred"));
        assertFalse(mapping.isForName("dist-me"));
        assertFalse(mapping.isForName("dist-"));
        assertFalse(mapping.isForName("dist"));

        assertNull(mapping.getNameUsing("me"));
        assertNull(mapping.getNameUsing("*"));

        assertNull(mapping.getWildcardMatch("dist-fred"));
        assertNull(mapping.getWildcardMatch("dist-*"));
        assertNull(mapping.getWildcardMatch("repl-*"));
        assertNull(mapping.getWildcardMatch("*"));
        assertNull(mapping.getWildcardMatch(""));
        assertNull(mapping.getWildcardMatch(null));
        assertNull(mapping.getWildcardMatch("dist-"));

        assertNull(mapping.getResourceRegistry().getResource(String.class, "message"));
        }

    /**
     * Ensure that a {@link CacheMapping} resources behave as expected.
     */
    @Test
    public void testCacheMappingResourceRegistry()
        {
        CacheMapping mapping = new CacheMapping("dist-*", "DistributedScheme");

        assertNull(mapping.getResourceRegistry().getResource(String.class, "welcome"));

        mapping.getResourceRegistry().registerResource(String.class, "welcome", "Hello World");
        assertNotNull(mapping.getResourceRegistry().getResource(String.class, "welcome"));
        assertEquals("Hello World", mapping.getResourceRegistry().getResource(String.class, "welcome"));
        }

    @Test
    public void testCacheMappingBooleanGetValue()
        {
        Boolean      fShared;
        CacheMapping mapping = new CacheMapping("dist-*", "DistributedScheme");

        mapping.setParameterResolver(new ParameterResolver()
            {
            @Override
            public Parameter resolve(String sName)
                {
                return new Parameter("shared", Boolean.TRUE);
                }

            });
        fShared = mapping.getValue("shared", Boolean.class);
        assertTrue(fShared != null && fShared.booleanValue());

        mapping.setParameterResolver(new ParameterResolver()
            {
            @Override
            public Parameter resolve(String sName)
                {
                return null;
                }

            });
        fShared = mapping.getValue("shared", Boolean.class);
        assertFalse(fShared != null && fShared.booleanValue());

        mapping.setParameterResolver(new ParameterResolver()
            {
            @Override
            public Parameter resolve(String sName)
                {
                return new Parameter("shared", Boolean.FALSE);
                }
            });
        fShared = mapping.getValue("shared", Boolean.class);
        assertFalse(fShared != null && fShared.booleanValue());
        }
    }
