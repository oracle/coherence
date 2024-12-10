/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.testing.SystemPropertyIsolation;
import com.oracle.coherence.testing.SystemPropertyResource;

import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link Config}.
 *
 * @author jf 2015.04.23
 * @since Coherence 12.2.1
 */
public class ConfigTest
    {
    @Test
    public void testGetBooleanWithDefault()
        {
        final String COHERENCE_PROPERTY = "tangosol.coherence.tcpdatagram.splitsocket";
        boolean      bExpected          = Boolean.parseBoolean(System.getProperty(COHERENCE_PROPERTY, "false"));

        assertEquals(bExpected, Config.getBoolean(COHERENCE_PROPERTY, false));

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_PROPERTY, "true"))
            {
            bExpected = Boolean.parseBoolean(System.getProperty(COHERENCE_PROPERTY, "false"));
            assertEquals(bExpected, Config.getBoolean(COHERENCE_PROPERTY.replaceFirst("tangosol.", ""), false));
            assertEquals(bExpected, Config.getBoolean(COHERENCE_PROPERTY, false));
            }

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_PROPERTY, "invalid value"))
            {
            bExpected = Boolean.parseBoolean(System.getProperty(COHERENCE_PROPERTY, "false"));
            assertEquals(bExpected, Config.getBoolean(COHERENCE_PROPERTY.replaceFirst("tangosol.", ""), false));
            }

        assertEquals(false, Config.getBoolean("coherence.notdefined.boolean", false));
        }

    @Test
    public void testGetIntegerWithDefault()
        {
        final String COHERENCE_PROPERTY = "tangosol.coherence.socket";
        Integer iExpected = Integer.parseInt(System.getProperty(COHERENCE_PROPERTY, "1"));

        assertEquals(iExpected, Config.getInteger(COHERENCE_PROPERTY, 1));

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_PROPERTY, "2"))
            {
            try
                {
                iExpected = Integer.parseInt(System.getProperty(COHERENCE_PROPERTY, "1"));
                assertEquals((Integer) 2, iExpected);
                }
            catch (NumberFormatException e)
                {
                }

            iExpected = Integer.parseInt(System.getProperty(COHERENCE_PROPERTY, "1"));
            assertEquals(iExpected, Config.getInteger(COHERENCE_PROPERTY.replaceFirst("tangosol.", ""), 1));
            assertEquals(iExpected, Config.getInteger(COHERENCE_PROPERTY, 1));
            }

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_PROPERTY, "invalid value"))
            {
            iExpected = Integer.getInteger(COHERENCE_PROPERTY, 1);
            assertEquals(iExpected, Config.getInteger(COHERENCE_PROPERTY.replaceFirst("tangosol.", ""), 1));
            }
        }

    @Test
    public void testGetLongWithDefault()
        {
        final String COHERENCE_PROPERTY = "tangosol.coherence.rwbm.requeue.delay";
        Long lExpected = Long.parseLong(System.getProperty(COHERENCE_PROPERTY, "60000"));

        assertEquals(lExpected, Config.getLong(COHERENCE_PROPERTY, 60000));

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_PROPERTY, "200"))
            {
            lExpected = Long.parseLong(System.getProperty(COHERENCE_PROPERTY, "60000L"));
            assertEquals(lExpected, Config.getLong(COHERENCE_PROPERTY.replaceFirst("tangosol.", ""), 200));
            assertEquals(lExpected, Config.getLong(COHERENCE_PROPERTY, 200));
            }

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_PROPERTY, "invalid value"))
            {
            try
                {
                Long.parseLong(System.getProperty(COHERENCE_PROPERTY, "60000"));
                assertTrue("should have thrown a NumberFormatException", false);
                }
            catch (NumberFormatException e)
                {
                // expected
                }

            lExpected = Long.getLong(COHERENCE_PROPERTY, 60000L);
            assertEquals(lExpected, Config.getLong(COHERENCE_PROPERTY.replaceFirst("tangosol.", ""), 60000L));
            }
        }

    @Test
    public void testGetFloatWithDefault()
        {
        final String COHERENCE_PROPERTY = "tangosol.coherence.nio.cleanup.frequency";
        Float fExpected = Float.parseFloat(System.getProperty(COHERENCE_PROPERTY, "0.10F"));

        assertEquals(fExpected, Config.getFloat(COHERENCE_PROPERTY, 0.10F));

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_PROPERTY, "0.10F"))
            {
            fExpected = Float.parseFloat(System.getProperty(COHERENCE_PROPERTY, "0.10F"));
            assertEquals(fExpected, Config.getFloat(COHERENCE_PROPERTY.replaceFirst("tangosol.", ""), 0.10F));
            assertEquals(fExpected, Config.getFloat(COHERENCE_PROPERTY, 0.10F));
            }

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_PROPERTY, "invalid value"))
            {
            try
                {
                fExpected = Float.parseFloat(System.getProperty(COHERENCE_PROPERTY, "0.10F"));
                assertTrue("should have thrown a NumberFormatException", false);
                }
            catch (NumberFormatException e)
                {
                // expected
                }
            fExpected = 0.010F;
            assertEquals(fExpected, Config.getFloat(COHERENCE_PROPERTY.replaceFirst("tangosol.", ""), 0.010F));
            assertEquals(fExpected, Config.getFloat(COHERENCE_PROPERTY, 0.010F));
            }
        }

    @Test
    public void testGetDoubleWithDefault()
        {
        final String COHERENCE_PROPERTY = "tangosol.coherence.daemonpool.idle.threshold";
        Double fExpected = Double.parseDouble(System.getProperty(COHERENCE_PROPERTY, "0.333"));

        assertEquals(fExpected, Config.getDouble(COHERENCE_PROPERTY, 0.333));

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_PROPERTY, "0.333"))
            {
            fExpected = Double.parseDouble(System.getProperty(COHERENCE_PROPERTY, "0.333"));
            assertEquals(fExpected, Config.getDouble(COHERENCE_PROPERTY.replaceFirst("tangosol.", ""), 0.333));
            assertEquals(fExpected, Config.getDouble(COHERENCE_PROPERTY, 0.333));
            }

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_PROPERTY, "invalid value"))
            {
            try
                {
                fExpected = Double.parseDouble(System.getProperty(COHERENCE_PROPERTY, "0.333"));
                assertTrue("should have thrown a NumberFormatException", false);
                }
            catch (NumberFormatException e)
                {
                // expected
                }
            fExpected = 0.333;
            assertEquals(fExpected, Config.getDouble(COHERENCE_PROPERTY.replaceFirst("tangosol.", ""), 0.333));
            assertEquals(fExpected, Config.getDouble(COHERENCE_PROPERTY, 0.333));
            }
        }

    /**
     * Test Config.getProperty when both system environment sets both Coherence system property pattern
     * 12.2.1 <tt>coherence.</tt> and prior to 12.2.1 <tt>tangosol.</tt>.
     */
    @Test
    public void testAmbiguousCoherenceSystemProperty()
        {
        final String COHERENCE_PROPERTY = "coherence.ambiguous";

        // pre-condition
        assertNull(Config.getProperty(COHERENCE_PROPERTY));

        try (SystemPropertyResource p1 = new SystemPropertyResource(COHERENCE_PROPERTY, "Coherence system property 12.2.1");
             SystemPropertyResource p2 = new SystemPropertyResource("tangosol." + COHERENCE_PROPERTY, "Coherence system property pre 12.2.1");)
            {
            assertEquals("Coherence system property 12.2.1", Config.getProperty(COHERENCE_PROPERTY));
            assertEquals("Coherence system property pre 12.2.1", Config.getProperty("tangosol." + COHERENCE_PROPERTY));
            }

        try (SystemPropertyResource p1 = new SystemPropertyResource(COHERENCE_PROPERTY, "Coherence system property 12.2.1");)
            {
            assertEquals("Coherence system property 12.2.1", Config.getProperty(COHERENCE_PROPERTY));
            assertEquals("Coherence system property 12.2.1", Config.getProperty("tangosol." + COHERENCE_PROPERTY));
            }

        try (SystemPropertyResource p2 = new SystemPropertyResource("tangosol." + COHERENCE_PROPERTY, "Coherence system property pre 12.2.1");)
            {
            assertEquals("Coherence system property pre 12.2.1", Config.getProperty(COHERENCE_PROPERTY));
            assertEquals("Coherence system property pre 12.2.1", Config.getProperty("tangosol." + COHERENCE_PROPERTY));
            }

        // post-condition
        assertNull(Config.getProperty(COHERENCE_PROPERTY));
        }

    @Test
    public void shouldGetPropertyWithDefaultSupplier()
        {
        String sResult = Config.getProperty("coherence." + UUID.randomUUID().toString(), () -> "bar");
        assertThat(sResult, is("bar"));
        }

    @Test
    public void shouldGetBooleanWhenTrue()
        {
        try (SystemPropertyResource r = new SystemPropertyResource("coherence.foo", "true"))
            {
            boolean fResult = Config.getBoolean("coherence.foo");
            assertThat(fResult, is(true));
            }
        }

    @Test
    public void shouldGetBooleanWhenFalse()
        {
        try (SystemPropertyResource r = new SystemPropertyResource("coherence.foo", "false"))
            {
            boolean fResult = Config.getBoolean("coherence.foo");
            assertThat(fResult, is(false));
            }
        }

    @Test
    public void shouldGetBooleanWhenNotSet()
        {
        System.clearProperty("coherence.foo");
        boolean fResult = Config.getBoolean("coherence.foo");
        assertThat(fResult, is(false));
        }

    @Test
    public void shouldGetDuration() throws Exception
        {
        try (SystemPropertyResource r = new SystemPropertyResource("coherence.foo", "10m"))
            {
            Duration result = Config.getDuration("coherence.foo");
            assertThat(result, is(new Duration("10m")));
            }
        }

    @Test
    public void shouldGetDurationWithIllegalValue() throws Exception
        {
        try (SystemPropertyResource r = new SystemPropertyResource("coherence.foo", "ABC"))
            {
            Duration result = Config.getDuration("coherence.foo");
            assertThat(result, is(nullValue()));
            }
        }

    @Test
    public void shouldGetDurationDefaultValue()
        {
        System.clearProperty("coherence.foo");
        Duration duration = new Duration("1m");
        Duration result = Config.getDuration("coherence.foo", duration);
        assertThat(result, is(duration));
        }

    /**
     * Test Coherence system property accessing, both 12.2.1 pattern beginning with <tt>coherence.</tt>
     * and prior to 12.2.1 pattern of <tt>tangosol.</tt>
     */
    @Test
    public void testGetPropertyDefaulting()
        {
        final String COHERENCE_PROPERTY = "coherence.system.property";
        final String DEFAULT_VALUE      = "theDefaultValue";
        final String SET_VALUE          = "theSetValue";

        // pre-condition
        assertNull(System.getProperty(COHERENCE_PROPERTY));
        assertNull(System.getProperty("tangosol." + COHERENCE_PROPERTY));
        assertNull(Config.getProperty(COHERENCE_PROPERTY));
        assertEquals(DEFAULT_VALUE, Config.getProperty(COHERENCE_PROPERTY, DEFAULT_VALUE));
        assertEquals(DEFAULT_VALUE, Config.getProperty("tangsol." + COHERENCE_PROPERTY, DEFAULT_VALUE));

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_PROPERTY, SET_VALUE);)
            {
            assertEquals(SET_VALUE, Config.getProperty(COHERENCE_PROPERTY));
            assertEquals(SET_VALUE, Config.getProperty(COHERENCE_PROPERTY, DEFAULT_VALUE));
            assertEquals(SET_VALUE, Config.getProperty("tangosol." + COHERENCE_PROPERTY));
            assertEquals(SET_VALUE, Config.getProperty("tangosol." + COHERENCE_PROPERTY, DEFAULT_VALUE));
            }

        // post-condition
        assertNull(System.getProperty(COHERENCE_PROPERTY));
        assertNull(System.getProperty("tangosol." + COHERENCE_PROPERTY));
        assertNull(Config.getProperty(COHERENCE_PROPERTY));
        assertEquals(DEFAULT_VALUE, Config.getProperty(COHERENCE_PROPERTY, DEFAULT_VALUE));
        assertEquals(DEFAULT_VALUE, Config.getProperty("tangsol." + COHERENCE_PROPERTY, DEFAULT_VALUE));
        }


    /**
     * Test Coherence system property accessing, both 12.2.1 pattern beginning with <tt>coherence.</tt>
     * and prior to 12.2.1 pattern of <tt>tangosol.</tt>
     */
    @Test
    public void testGetPropertyDefaultingBackwardsCompatible()
        {
        final String COHERENCE_PROPERTY = "coherence.system.property";
        final String DEFAULT_VALUE      = "theDefaultValue";
        final String SET_VALUE          = "theSetValue";

        // pre-condition
        assertNull(System.getProperty(COHERENCE_PROPERTY));
        assertNull(System.getProperty("tangosol." + COHERENCE_PROPERTY));
        assertNull(Config.getProperty(COHERENCE_PROPERTY));
        assertEquals(DEFAULT_VALUE, Config.getProperty(COHERENCE_PROPERTY, DEFAULT_VALUE));
        assertEquals(DEFAULT_VALUE, Config.getProperty("tangsol." + COHERENCE_PROPERTY, DEFAULT_VALUE));

        try (SystemPropertyResource p = new SystemPropertyResource("tangosol." + COHERENCE_PROPERTY, SET_VALUE);)
            {
            assertEquals(SET_VALUE, Config.getProperty(COHERENCE_PROPERTY));
            assertEquals(SET_VALUE, Config.getProperty(COHERENCE_PROPERTY, DEFAULT_VALUE));
            assertEquals(SET_VALUE, Config.getProperty("tangosol." + COHERENCE_PROPERTY));
            assertEquals(SET_VALUE, Config.getProperty("tangosol." + COHERENCE_PROPERTY, DEFAULT_VALUE));
            }

        // post-condition
        assertNull(System.getProperty(COHERENCE_PROPERTY));
        assertNull(System.getProperty("tangosol." + COHERENCE_PROPERTY));
        assertNull(Config.getProperty(COHERENCE_PROPERTY));
        assertEquals(DEFAULT_VALUE, Config.getProperty(COHERENCE_PROPERTY, DEFAULT_VALUE));
        assertEquals(DEFAULT_VALUE, Config.getProperty("tangsol." + COHERENCE_PROPERTY, DEFAULT_VALUE));
        }


    @Test
    public void testGetenv()
        {
        Map<String, String> env = System.getenv();
        if (env != null)
            {
            for (String key : env.keySet())
                {
                assertEquals(System.getenv(key), Config.getenv(key));
                }
            }
        }

    @Test
    public void shouldGetPropertyFromSystemPropertyResolver()
        {
        SystemPropertyResolver      sysProps = mock(SystemPropertyResolver.class);
        EnvironmentVariableResolver envVars  = mock(EnvironmentVariableResolver.class);

        when(sysProps.getProperty("coherence.foo")).thenReturn("bar");

        String result = Config.getPropertyInternal("coherence.foo", sysProps, envVars);
        assertThat(result, is("bar"));
        verifyNoInteractions(envVars);
        }

    @Test
    public void shouldGetPropertyFromSystemPropertyResolverUsingTangosolPrefix()
        {
        SystemPropertyResolver      sysProps = mock(SystemPropertyResolver.class);
        EnvironmentVariableResolver envVars  = mock(EnvironmentVariableResolver.class);

        when(sysProps.getProperty("tangosol.coherence.foo")).thenReturn("bar");

        String result = Config.getPropertyInternal("coherence.foo", sysProps, envVars);
        assertThat(result, is("bar"));
        verifyNoInteractions(envVars);
        }

    @Test
    public void shouldGetPropertyFromSystemPropertyResolverReplacingCoherenceWithTangosolPrefix()
        {
        SystemPropertyResolver      sysProps = mock(SystemPropertyResolver.class);
        EnvironmentVariableResolver envVars  = mock(EnvironmentVariableResolver.class);

        when(sysProps.getProperty("tangosol.foo")).thenReturn("bar");

        String result = Config.getPropertyInternal("coherence.foo", sysProps, envVars);
        assertThat(result, is("bar"));
        verifyNoInteractions(envVars);
        }

    @Test
    public void shouldGetPropertyFromSystemPropertyResolverRemovingTangosolPrefix()
        {
        SystemPropertyResolver      sysProps = mock(SystemPropertyResolver.class);
        EnvironmentVariableResolver envVars  = mock(EnvironmentVariableResolver.class);

        when(sysProps.getProperty("coherence.foo")).thenReturn("bar");

        String result = Config.getPropertyInternal("tangosol.coherence.foo", sysProps, envVars);
        assertThat(result, is("bar"));
        verifyNoInteractions(envVars);
        }

    @Test
    public void shouldGetPropertyFromSystemPropertyResolverReplacingTangosolPrefixWithCoherence()
        {
        SystemPropertyResolver      sysProps = mock(SystemPropertyResolver.class);
        EnvironmentVariableResolver envVars  = mock(EnvironmentVariableResolver.class);

        when(sysProps.getProperty("coherence.foo")).thenReturn("bar");

        String result = Config.getPropertyInternal("tangosol.foo", sysProps, envVars);
        assertThat(result, is("bar"));
        verifyNoInteractions(envVars);
        }

    @Test
    public void shouldGetPropertyFromEnvironmentVariableResolverWhenNotInSystemPropertyResolver()
        {
        SystemPropertyResolver      sysProps = mock(SystemPropertyResolver.class);
        EnvironmentVariableResolver envVars  = mock(EnvironmentVariableResolver.class);

        when(sysProps.getProperty("coherence.foo")).thenReturn(null);
        when(envVars.getEnv("coherence.foo")).thenReturn("bar");

        String result = Config.getPropertyInternal("coherence.foo", sysProps, envVars);
        assertThat(result, is("bar"));
        verify(envVars).getEnv("coherence.foo");
        }

    @Test
    public void shouldGetPropertyFromEnvironmentVariableResolver()
        {
        EnvironmentVariableResolver envVars = mock(EnvironmentVariableResolver.class);

        when(envVars.getEnv("coherence.foo")).thenReturn("bar");

        String result = Config.getEnvInternal("coherence.foo", envVars);
        assertThat(result, is("bar"));
        }

    @Test
    public void shouldGetPropertyFromEnvironmentVariableResolverUsingTangosolPrefix()
        {
        EnvironmentVariableResolver envVars = mock(EnvironmentVariableResolver.class);

        when(envVars.getEnv("tangosol.coherence.foo")).thenReturn("bar");

        String result = Config.getEnvInternal("coherence.foo", envVars);
        assertThat(result, is("bar"));
        }

    @Test
    public void shouldGetPropertyFromEnvironmentVariableResolverReplacingCoherenceWithTangosolPrefix()
        {
        EnvironmentVariableResolver envVars = mock(EnvironmentVariableResolver.class);

        when(envVars.getEnv("tangosol.foo")).thenReturn("bar");

        String result = Config.getEnvInternal("coherence.foo", envVars);
        assertThat(result, is("bar"));
        }

    @Test
    public void shouldGetPropertyFromEnvironmentVariableResolverRemovingTangosolPrefix()
        {
        EnvironmentVariableResolver envVars = mock(EnvironmentVariableResolver.class);

        when(envVars.getEnv("coherence.foo")).thenReturn("bar");

        String result = Config.getEnvInternal("tangosol.coherence.foo", envVars);
        assertThat(result, is("bar"));
        }

    @Test
    public void shouldGetPropertyFromEnvironmentVariableResolverReplacingTangosolPrefixWithCoherence()
        {
        EnvironmentVariableResolver envVars = mock(EnvironmentVariableResolver.class);

        when(envVars.getEnv("coherence.foo")).thenReturn("bar");

        String result = Config.getEnvInternal("tangosol.foo", envVars);
        assertThat(result, is("bar"));
        }

    @Test
    public void shouldGetPropertyFromEnvironmentVariableResolverUpperCase()
        {
        EnvironmentVariableResolver envVars = mock(EnvironmentVariableResolver.class);

        when(envVars.getEnv("COHERENCE_FOO")).thenReturn("bar");

        String result = Config.getEnvInternal("coherence.foo", envVars);
        assertThat(result, is("bar"));
        }

    @Test
    public void shouldGetPropertyFromEnvironmentVariableResolverUpperCaseUsingTangosolPrefix()
        {
        EnvironmentVariableResolver envVars = mock(EnvironmentVariableResolver.class);

        when(envVars.getEnv("TANGOSOL_COHERENCE_FOO")).thenReturn("bar");

        String result = Config.getEnvInternal("coherence.foo", envVars);
        assertThat(result, is("bar"));
        }

    @Test
    public void shouldGetPropertyFromEnvironmentVariableResolverUpperCaseReplacingCoherenceWithTangosolPrefix()
        {
        EnvironmentVariableResolver envVars = mock(EnvironmentVariableResolver.class);

        when(envVars.getEnv("TANGOSOL_FOO")).thenReturn("bar");

        String result = Config.getEnvInternal("coherence.foo", envVars);
        assertThat(result, is("bar"));
        }

    @Test
    public void shouldGetPropertyFromEnvironmentVariableResolverUpperCaseRemovingTangosolPrefix()
        {
        EnvironmentVariableResolver envVars = mock(EnvironmentVariableResolver.class);

        when(envVars.getEnv("COHERENCE_FOO")).thenReturn("bar");

        String result = Config.getEnvInternal("tangosol.coherence.foo", envVars);
        assertThat(result, is("bar"));
        }

    @Test
    public void shouldGetPropertyFromEnvironmentVariableResolverUpperCaseReplacingTangosolPrefixWithCoherence()
        {
        EnvironmentVariableResolver envVars = mock(EnvironmentVariableResolver.class);

        when(envVars.getEnv("COHERENCE_FOO")).thenReturn("bar");

        String result = Config.getEnvInternal("tangosol.foo", envVars);
        assertThat(result, is("bar"));
        }

    //----- constants --------------------------------------------------------

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
