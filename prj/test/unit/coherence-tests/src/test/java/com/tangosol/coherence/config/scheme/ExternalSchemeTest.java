/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;
import com.tangosol.coherence.config.builder.storemanager.BinaryStoreManagerBuilder;
import com.tangosol.coherence.config.builder.storemanager.NioFileManagerBuilder;
import com.tangosol.coherence.config.unit.Seconds;
import com.tangosol.coherence.config.unit.Units;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Base;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

/**
 * Unit Tests for a {@link ExternalScheme}.
 *
 * @author pfm  2012.06.27
 * @since Coherence 12.1.2
 */
public class ExternalSchemeTest
    {
    /**
     * Test realize.
     */
    @Test
    public void testRealize()
        {
        ExternalScheme scheme = new ExternalScheme();

        MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(null,
                                                   Mockito.mock(BackingMapManagerContext.class),
                                                   Base.getContextClassLoader(), "TestCache", "");

        scheme.setBinaryStoreManagerBuilder(new NioFileManagerBuilder());

        Map map = scheme.realizeMap(new NullParameterResolver(), dependencies);

        assertNotNull(map);
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        ExternalScheme scheme = new ExternalScheme();

        assertNull(scheme.getBinaryStoreManagerBuilder());
        assertNull(scheme.getUnitCalculatorBuilder());

        assertEquals(0, scheme.getExpiryDelay(new NullParameterResolver()).getNanos());
        assertEquals(0, scheme.getHighUnits(new NullParameterResolver()).getUnitCount());
        assertEquals(1, scheme.getUnitFactor(new NullParameterResolver()));
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        ExternalScheme            scheme = new ExternalScheme();

        BinaryStoreManagerBuilder bldr   = new NioFileManagerBuilder();

        scheme.setBinaryStoreManagerBuilder(bldr);
        assertEquals(bldr, scheme.getBinaryStoreManagerBuilder());

        UnitCalculatorBuilder bldrCalculator = new UnitCalculatorBuilder();

        scheme.setUnitCalculatorBuilder(bldrCalculator);
        assertEquals(bldrCalculator, scheme.getUnitCalculatorBuilder());

        Seconds secs = new Seconds(10);

        scheme.setExpiryDelay(new LiteralExpression<Seconds>(secs));
        assertEquals(secs, scheme.getExpiryDelay(new NullParameterResolver()));

        int cHighUnits = 1000;

        scheme.setHighUnits(new LiteralExpression<Units>(new Units(cHighUnits)));
        assertEquals(cHighUnits, scheme.getHighUnits(new NullParameterResolver()).getUnitCount());

        int nUnitFactor = 10;

        scheme.setUnitFactor(new LiteralExpression<Integer>(nUnitFactor));
        assertEquals(nUnitFactor, scheme.getUnitFactor(new NullParameterResolver()));
        }

    /**
     * Test validate.
     */
    @Test
    public void testValidate()
        {
        try
            {
            new ExternalScheme().realizeMap(null, null);
            fail("Expected IllegalArgumentException due to missing ParameterResolver");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("resolver"));
            }

        try
            {
            MapBuilder.Dependencies realizeContext = new MapBuilder.Dependencies(null,
                                                         Mockito.mock(BackingMapManagerContext.class),
                                                         Base.getContextClassLoader(), "TestCache", "");

            new ExternalScheme().realizeMap(new NullParameterResolver(), realizeContext);
            fail("Expected IllegalArgumentException due to missing BinaryStoreManager");
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage().toLowerCase().contains("binarystoremanager"));
            }
        }
    }
