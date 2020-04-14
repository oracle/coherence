/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.aggregator;

import com.tangosol.util.InvocableMap;

import com.tangosol.util.aggregator.LongSum;

import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Test of {@link AggregatorRegistry}.
 *
 * @author vp 2011.07.06
 */
public class AggregatorRegistryTest
    {
    @Before
    public void setUp()
        {
        m_aggrRegistry = new AggregatorRegistry();
        m_aggrRegistry.register("LONGSUM", LongSum.class);
        }

    @Test
    public void getBuiltInAggregator()
        {
        assertEquals(LongSum.class, m_aggrRegistry.getAggregator("LONGSUM()").getClass());
        assertEquals(LongSum.class, m_aggrRegistry.getAggregator("LONGSUM(age)").getClass());
        }

    @Test(expected = IllegalArgumentException.class)
    public void missingAggregator()
        {
        assertEquals(LongSum.class, m_aggrRegistry.getAggregator("MISSING()").getClass());
        }

    @Test(expected = IllegalArgumentException.class)
    public void badSyntax()
        {
        assertEquals(LongSum.class, m_aggrRegistry.getAggregator("BAD").getClass());
        }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyArgs()
        {
        assertEquals(LongSum.class, m_aggrRegistry.getAggregator("LONGSUM(age, year)").getClass());
        }

    @Test
    public void registerAggregator()
        {
        final InvocableMap.EntryAggregator testAggr = new InvocableMap.EntryAggregator()
            {
            public Object aggregate(Set entries)
                {
                return null;
                }
            };

        AggregatorFactory factory = new AggregatorFactory()
            {
            public InvocableMap.EntryAggregator getAggregator(String... asArgs)
                {
                return testAggr;
                }
            };

        AggregatorRegistry aggrRegistry = new AggregatorRegistry();
        aggrRegistry.register("TEST", factory);
        assertEquals(testAggr, aggrRegistry.getAggregator("TEST()"));
        }

    private AggregatorRegistry m_aggrRegistry;
    }
