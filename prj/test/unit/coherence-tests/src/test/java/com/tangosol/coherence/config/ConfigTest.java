/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.common.util.MemorySize;
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

    /**
     * Test Coherence system property accessing, both 12.2.1 pattern beginning with <tt>coherence.</tt>
     * and prior to 12.2.1 pattern of <tt>tangosol.</tt>
     */
    @Test
    public void testClassPrefixPropertyDefaultingBackwardsCompatible()
        {
        final String COHERENCE_COMMON       = "com.oracle.coherence.common";  // from Coherence 14.1.1.0 and greater
        final String ORACLE_COMMON          = "com.oracle.common";            // before Coherence 14.1.1.0
        final String RECONNECT_LIMIT_SUFFIX = ".internal.net.socketbus.SocketBusDriver.reconnectLimit";
        final int DEFAULT_VALUE             = 3;
        final int SET_VALUE                 = 20;

        // pre-condition
        assertNull(System.getProperty(COHERENCE_COMMON + RECONNECT_LIMIT_SUFFIX));
        assertNull(System.getProperty(ORACLE_COMMON + RECONNECT_LIMIT_SUFFIX));
        assertThat(Config.getInteger(COHERENCE_COMMON + RECONNECT_LIMIT_SUFFIX, DEFAULT_VALUE), is(DEFAULT_VALUE));
        assertThat(Config.getInteger(ORACLE_COMMON + RECONNECT_LIMIT_SUFFIX, DEFAULT_VALUE), is(DEFAULT_VALUE));

        try (SystemPropertyResource p = new SystemPropertyResource(ORACLE_COMMON + RECONNECT_LIMIT_SUFFIX, Integer.toString(SET_VALUE));)
            {
            assertThat(Config.getInteger(ORACLE_COMMON + RECONNECT_LIMIT_SUFFIX),    is(SET_VALUE));
            assertThat(Config.getInteger(COHERENCE_COMMON + RECONNECT_LIMIT_SUFFIX), is(SET_VALUE));
            }

        // post-condition
        assertNull(System.getProperty(COHERENCE_COMMON + RECONNECT_LIMIT_SUFFIX));
        assertNull(System.getProperty(ORACLE_COMMON + RECONNECT_LIMIT_SUFFIX));
        assertThat(Config.getInteger(COHERENCE_COMMON + RECONNECT_LIMIT_SUFFIX, DEFAULT_VALUE), is(DEFAULT_VALUE));
        assertThat(Config.getInteger(ORACLE_COMMON + RECONNECT_LIMIT_SUFFIX, DEFAULT_VALUE), is(DEFAULT_VALUE));

        try (SystemPropertyResource p = new SystemPropertyResource(COHERENCE_COMMON + RECONNECT_LIMIT_SUFFIX, Integer.toString(SET_VALUE));)
            {
            assertThat(Config.getInteger(ORACLE_COMMON + RECONNECT_LIMIT_SUFFIX),    is(SET_VALUE));
            assertThat(Config.getInteger(COHERENCE_COMMON + RECONNECT_LIMIT_SUFFIX), is(SET_VALUE));
            }

        // post-condition
        assertNull(System.getProperty(COHERENCE_COMMON + RECONNECT_LIMIT_SUFFIX));
        assertNull(System.getProperty(ORACLE_COMMON + RECONNECT_LIMIT_SUFFIX));
        assertThat(Config.getInteger(COHERENCE_COMMON + RECONNECT_LIMIT_SUFFIX, DEFAULT_VALUE), is(DEFAULT_VALUE));
        assertThat(Config.getInteger(ORACLE_COMMON + RECONNECT_LIMIT_SUFFIX, DEFAULT_VALUE), is(DEFAULT_VALUE));
        }

    /**
     * COH-32433: ensure no duration magnitude is needed in system property value ending in "Millis" and provided numeric value defaults to millis.
     */
    @Test
    public void testValidMillisDuration()
        {
        final String ORACLE_COMMON    = "com.oracle.common";            // before Coherence 14.1.1.0
        final String ACKTIMEOUTMILLIS = ".internal.net.socketbus.SocketBusDriver.ackTimeoutMillis";
        final long   dMillis          = 1200L;

        try (SystemPropertyResource p = new SystemPropertyResource(ORACLE_COMMON + ACKTIMEOUTMILLIS, Long.toString(dMillis));)
            {
            long millis = Config.getDuration(ORACLE_COMMON + ACKTIMEOUTMILLIS,
                                             new Duration("111", Duration.Magnitude.MILLI),
                                             Duration.Magnitude.MILLI).as(Duration.Magnitude.MILLI);
            assertThat("system property value does not contain magnitude but defaults to millis as expected for duration for system property ending in Millis",
                       millis, is(dMillis));
            }
        }

    @Test
    public void testMillisDurationWithDuration()
        {
        final String ORACLE_COMMON    = "com.oracle.common";            // before Coherence 14.1.1.0
        final String ACKTIMEOUTMILLIS = ".internal.net.socketbus.SocketBusDriver.ackTimeoutMillis";
        final String  sValueSec       = "2s";

        try (SystemPropertyResource p = new SystemPropertyResource(ORACLE_COMMON + ACKTIMEOUTMILLIS, sValueSec);)
            {
            long dMillis = Config.getDuration(ORACLE_COMMON + ACKTIMEOUTMILLIS,
                                             new Duration("111", Duration.Magnitude.MILLI),
                                             Duration.Magnitude.MILLI).as(Duration.Magnitude.MILLI);
            assertThat("verify specified magnitude in system property value overrides default magnitude",
                       dMillis, is(new Duration(sValueSec).as(Duration.Magnitude.MILLI)));
            }
        }

    @Test
    public void testInvalidMillisDuration()
        {
        final String ORACLE_COMMON    = "com.oracle.common";
        final String ACKTIMEOUTMILLIS = ".internal.net.socketbus.SocketBusDriver.ackTimeoutMillis";
        final long   dMillis          = 1200L;

        try (SystemPropertyResource p = new SystemPropertyResource(ORACLE_COMMON + ACKTIMEOUTMILLIS, Long.toString(dMillis));)
            {
            long millis = Config.getDuration(ORACLE_COMMON + ACKTIMEOUTMILLIS,
                                             new Duration("111", Duration.Magnitude.MILLI)).as(Duration.Magnitude.MILLI);
            assertThat("system property value does not contain magnitude and has no defaults to millis as expected for duration for system property ending in Millis",
                       millis, is(111L));
            }
        }

    @Test
    public void testInvalidPropertyValues()
        {
        // test for Int, Long, Float, Double, Duration, MemorySize
        try (SystemPropertyResource p = new SystemPropertyResource("IntProperty", "123InvalidInt");)
            {
            final int DEFAULT = 5;
            int       dValue = Config.getInteger("IntProperty", DEFAULT);

            assertThat(dValue, is(DEFAULT));
            }
        try (SystemPropertyResource p = new SystemPropertyResource("LongProperty", "123InvalidLong");)
            {
            final long DEFAULT = 5;
            long       lValue = Config.getLong("LongProperty", DEFAULT);

            assertThat(lValue, is(DEFAULT));
            }
        try (SystemPropertyResource p = new SystemPropertyResource("FloatProperty", "123InvalidFloat");)
            {
            final float DEFAULT = 5.0f;
            float       fValue = Config.getFloat("FloatProperty", DEFAULT);

            assertThat(fValue, is(DEFAULT));
            }
        try (SystemPropertyResource p = new SystemPropertyResource("DoubleProperty", "123InvalidDouble");)
            {
            final double DEFAULT = 5.0f;
            double       fValue = Config.getDouble("DoubleProperty", DEFAULT);

            assertThat(fValue, is(DEFAULT));
            }

        try (SystemPropertyResource p = new SystemPropertyResource("DurationProperty", "1234");)
            {
            // missing duration magnitude in string and Config.getDuration() does not provide a default magnitude.
            final Duration DEFAULT = new Duration("5s");
            Duration       durValue = Config.getDuration("DurationProperty", DEFAULT, null);

            assertThat(durValue, is(DEFAULT));
            }
        try (SystemPropertyResource p = new SystemPropertyResource("MemorySizeProperty", "123invalidMagnitude");)
            {
            final MemorySize DEFAULT    = new MemorySize("1m");
            MemorySize       memorySize = Config.getMemorySize("MemorySizeProperty", "1m");

            assertThat(memorySize, is(DEFAULT));
            }
        }

    //----- constants --------------------------------------------------------

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
