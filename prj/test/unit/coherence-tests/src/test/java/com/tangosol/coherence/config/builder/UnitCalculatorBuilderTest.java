/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator;

import com.tangosol.util.Base;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link UnitCalculatorBuilderTest} contains unit tests for {@link UnitCalculatorBuilder}s.
 *
 * @author pfm  2012.06.07
 */
public class UnitCalculatorBuilderTest
    {
    @Test
    public void testFixed()
        {
        String sType = "FIXED";

        // first test default
        UnitCalculatorBuilder bldr = new UnitCalculatorBuilder();
        UnitCalculator calculator = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);
        assertTrue(calculator.getName().toUpperCase().contains(sType));

        testAny(sType);
        }

    @Test
    public void testBinary()
        {
        testAny("BINARY");
        }

    @Test
    public void testCustom()
        {
        UnitCalculatorBuilder bldr = new UnitCalculatorBuilder();
        bldr.setCustomBuilder(new InstanceBuilder<UnitCalculator>(CustomUnitCalculator.class));
        UnitCalculator calculator = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);
        assertEquals(CustomUnitCalculator.class, calculator.getClass());
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Test that the correct type of UnitCalculator is created.
     *
     * @param sType  the calculator type
     */
    protected void testAny(String sType)
        {
        UnitCalculatorBuilder bldr = new UnitCalculatorBuilder();
        bldr.setUnitCalculatorType(new LiteralExpression<String>(sType));
        UnitCalculator calculator = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), null);
        assertTrue(calculator.getName().toUpperCase().contains(sType));
        }

    // ----- inner classes --------------------------------------------------

    /**
     * Custom UnitCalculator class.
     */
    public static class CustomUnitCalculator
            implements UnitCalculator
        {
        @Override
        public String getName()
            {
            return "CUSTOM";
            }

        @Override
        public int calculateUnits(Object oKey, Object oValue)
            {
            return 0;
            }
        }
    }
