/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.processor;

import com.tangosol.util.InvocableMap;

import com.tangosol.util.aggregator.Count;

import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.NumberMultiplier;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Test of {@link ProcessorRegistry}.
 *
 * @author vp 2011.07.06
 */
public class ProcessorRegistryTest
    {
    @Before
    public void setUp()
        {
        m_procRegistry = new ProcessorRegistry();
        m_procRegistry.register(NumberIncrementor.class.getSimpleName(),
                new NumberIncrementorFactory(false));
        m_procRegistry.register(NumberMultiplier.class.getSimpleName(),
                new NumberMultiplierFactory(false));
        }

    @Test(expected = IllegalArgumentException.class)
    public void missingProcessor()
        {
        assertEquals(Count.class, m_procRegistry.getProcessor("MISSING()").getClass());
        }

    @Test(expected = IllegalArgumentException.class)
    public void badSyntax()
        {
        assertEquals(Count.class, m_procRegistry.getProcessor("BAD").getClass());
        }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyArgs()
        {
        assertEquals(Count.class, m_procRegistry.getProcessor("NumberIncrementor(1, 2, 3, 4)").getClass());
        }

    @Test
    public void registerProcessor()
        {
        final InvocableMap.EntryProcessor processor = new InvocableMap.EntryProcessor()
            {
            public Object process(InvocableMap.Entry entry)
                {
                return null;
                }

            public Map processAll(Set setEntries)
                {
                return null;
                }
            };

        ProcessorFactory factory = new ProcessorFactory()
            {
            public InvocableMap.EntryProcessor getProcessor(String... asArgs)
                {
                return processor;
                }
            };

        ProcessorRegistry procRegistry = new ProcessorRegistry();
        procRegistry.register("TEST", factory);
        assertEquals(processor, procRegistry.getProcessor("TEST()"));
        }

    private ProcessorRegistry m_procRegistry;

    }