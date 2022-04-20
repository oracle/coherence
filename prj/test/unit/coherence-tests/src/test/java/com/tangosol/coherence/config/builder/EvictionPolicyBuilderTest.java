/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.net.cache.ConfigurableCacheMap.Entry;
import com.tangosol.net.cache.ConfigurableCacheMap.EvictionPolicy;

import com.tangosol.util.Base;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * {@link EvictionPolicyBuilderTest} contains unit tests for {@link EvictionPolicyBuilder}s.
 *
 * @author pfm  2012.06.07
 */
public class EvictionPolicyBuilderTest
    {
    @Test
    public void testHybrid()
        {
        String sType = "HYBRID";

        // first test default
        EvictionPolicyBuilder bldr = new EvictionPolicyBuilder();
        EvictionPolicy policy = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);
        assertTrue(policy.getName().toUpperCase().contains(sType));

        testAny(sType);
        }

    @Test
    public void testLFU()
        {
        testAny("LFU");
        }

    @Test
    public void testRFU()
        {
        testAny("LRU");
        }

    @Test
    public void testCustom()
        {
        EvictionPolicyBuilder bldr = new EvictionPolicyBuilder();
        bldr.setCustomBuilder(new InstanceBuilder<EvictionPolicy>(CustomEvictionPolicy.class));
        EvictionPolicy policy = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);
        assertTrue(policy.getName().toUpperCase().contains("CUSTOM"));
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Test that the correct type of EvictionPolicy is created.
     *
     * @param sType  the eviction type
     */
    protected void testAny(String sType)
        {
        EvictionPolicyBuilder bldr = new EvictionPolicyBuilder();
        bldr.setEvictionType(new LiteralExpression<String>(sType));
        EvictionPolicy policy = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);
        assertTrue(policy.getName().toUpperCase().contains(sType));
        }

    // ----- inner classes --------------------------------------------------

    /**
     * Custom EvictionPolicy class.
     */
    public static class CustomEvictionPolicy
            implements EvictionPolicy
        {
        @Override
        public void entryTouched(Entry entry)
            {
            }

        @Override
        public void requestEviction(int cMaximum)
            {
            }

        @Override
        public String getName()
            {
            return "CUSTOM";
            }
        }
    }
