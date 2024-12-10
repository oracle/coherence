/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.oracle.coherence.testing.SystemPropertyIsolation;
import com.oracle.coherence.testing.SystemPropertyResource;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the {@link SystemPropertyParameterResolver}.
 *
 * @author jf 2015.04.16
 *
 * @since Coherence 12.2.1
 */
public class SystemPropertyParameterResolverTest
    {
    /**
     * Simulate accessing system property in coherence when commandline system property -Dcoherence.pof.config=XXXX
     */
    @Test
    public void testSetGetProperty()
        {
        SystemPropertyParameterResolver resolver = new SystemPropertyParameterResolver();
        try (SystemPropertyResource p = new SystemPropertyResource("coherence.pof.config", POFCONFIG))
            {
            assertEquals(POFCONFIG, resolver.resolve("coherence.pof.config").evaluate(resolver).as(String.class));
            }

        assertNull(resolver.resolve("coherence.pof.config"));
        }

    /**
     * Test handling of non-converted Coherence system property with commandline set to
     * Coherence 12.2.1 convention of "coherence.*".
     */
    @Test
    public void testSetGetPropertyTangosolCoherence()
        {
        SystemPropertyParameterResolver resolver = new SystemPropertyParameterResolver();

        try (SystemPropertyResource p = new SystemPropertyResource("coherence.system.property", SET_VALUE))
            {
            assertEquals(SET_VALUE, resolver.resolve("tangosol.coherence.system.property").evaluate(resolver).as(String.class));
            }

        assertNull(resolver.resolve("tangosol.coherence.system.property"));
        }

    /**
     * Simulate accessing system property in coherence when commandline system property -Dcoherence.pof.config=abcdef
     */
    @Test
    public void testSetGetPropertyNoChangeToCoherenceProduct()
        {
        SystemPropertyParameterResolver resolver = new SystemPropertyParameterResolver();

        try (SystemPropertyResource p = new SystemPropertyResource("coherence.pof.config", POFCONFIG))
            {
            assertEquals(POFCONFIG, resolver.resolve("tangosol.pof.config").evaluate(resolver).as(String.class));
            }

        assertNull(resolver.resolve("tangosol.pof.config"));
        }

    /**
     * Test property defaulting. No commandline system property defined.
     */
    @Test
    public void testGetPropertyDefaulting()
        {
        SystemPropertyParameterResolver resolver = new SystemPropertyParameterResolver();

        assertNull(resolver.resolve("coherence.system.property.notdefined"));
        }

    /**
     * Test property defaulting. No commandline system property defined.
     */
    @Test
    public void testTangosolGetPropertyDefaulting()
        {
        SystemPropertyParameterResolver resolver = new SystemPropertyParameterResolver();

        assertNull(resolver.resolve("tangosol.coherence.system.property.notdefined"));
        }

    /**
     * Test coherence system property access with a commandline setting of
     * -Dcoherence.set.system.property=SET_VALUE.  Ensure that getProperty with
     * a default value, {@link SystemPropertyParameterResolver#resolve(String)}, does not impact result.
     */
    @Test
    public void testSetPropertyNotDefaulted()
        {
        SystemPropertyParameterResolver resolver = new SystemPropertyParameterResolver();

        try (SystemPropertyResource p = new SystemPropertyResource("coherence.set.system.property", SET_VALUE))
            {
            assertNotNull(resolver.resolve("coherence.set.system.property"));
            assertEquals(SET_VALUE, resolver.resolve("coherence.set.system.property").evaluate(resolver).as(String.class));
            }
        }

    /**
     * Verify that commandline -Dtangosol.pof.config=abcdef still works when BACKWARDSCOMPATIBILITY
     * is enabled.
     */
    @Test
    public void testTangosolBackwardsCompatibilty()
        {
        SystemPropertyParameterResolver resolver = new SystemPropertyParameterResolver();

        try (SystemPropertyResource p = new SystemPropertyResource("tangosol.pof.config", POFCONFIG))
            {
            assertEquals(POFCONFIG, resolver.resolve("coherence.pof.config").evaluate(resolver).as(String.class));
            }
        }

    /**
     * Verify that -Dtangosol.coherence.distributed.localstorage=false still works when BACKWARDSCOMPATIBILITY
     * is enabled.
     */
    @Test
    public void testTangosolCoherenceBackwardsCompatibilty()
        {
        SystemPropertyParameterResolver resolver = new SystemPropertyParameterResolver();

        final String BOOLEAN_VALUE = "false";

        try (SystemPropertyResource p = new SystemPropertyResource("tangosol.coherence.distributed.localstorage", BOOLEAN_VALUE))
            {
            assertEquals(BOOLEAN_VALUE,
                         resolver.resolve("coherence.distributed.localstorage").evaluate(resolver).as(String.class));
            }
        }

    /**
     * Test valid and invalid value resolutions for {@link SystemPropertyParameterResolver#resolve(String, Class))}
     */
    @Test
    public void testResolveAndCoerceValue()
        {
        SystemPropertyParameterResolver resolver = new SystemPropertyParameterResolver();

        try (SystemPropertyResource p1 = new SystemPropertyResource("coherence.localstorage", "false");
             SystemPropertyResource p2 = new SystemPropertyResource("coherence.clusterport", "7574");
             SystemPropertyResource p3 = new SystemPropertyResource("coherence.jointimeout", "30000000");
             SystemPropertyResource p4 = new SystemPropertyResource("coherence.stringvalue", "justsomestringvalue");
             SystemPropertyResource p5 = new SystemPropertyResource("coherence.notvalid.integer", "two");
             SystemPropertyResource p6 = new SystemPropertyResource("coherence.notvalid.boolean", "1");
             SystemPropertyResource p7 = new SystemPropertyResource("coherence.notvalid.long", "not a long number");
             SystemPropertyResource p8 = new SystemPropertyResource("coherence.notvalid.float", "not a float number");)
            {
            // test resolves that must work
            assertFalse(resolver.resolve("coherence.localstorage", Boolean.class));
            assertEquals((Integer) 7574, resolver.resolve("coherence.clusterport", Integer.class));
            assertEquals((Long) 30000000L, resolver.resolve("coherence.jointimeout", Long.class));
            assertEquals("justsomestringvalue", resolver.resolve("coherence.stringvalue", String.class));

            // test failed coercion cases
            try
                {
                resolver.resolve("coherence.notvalid.boolean", Boolean.class);
                assertFalse("expected exception", true);
                }
            catch (Exception e)
                {
                // expected result
                }

            try
                {
                resolver.resolve("coherence.notvalid.integer", Integer.class);
                assertFalse("expected exception", true);
                }
            catch (Exception e)
                {
                // expected result
                }

            try
                {
                resolver.resolve("coherence.notvalid.long", Integer.class);
                assertFalse("expected exception", true);
                }
            catch (Exception e)
                {
                // expected result
                }

            try
                {
                resolver.resolve("coherence.notvalid.float", Integer.class);
                assertFalse("expected exception", true);
                }
            catch (Exception e)
                {
                // expected result
                }

            }
        }

    static final String SET_VALUE     = "SET_VALUE";
    static final String POFCONFIG     = "abcdef";

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
