/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import com.tangosol.internal.net.management.MBeanCollectorFunction;

import com.tangosol.util.Filter;
import com.tangosol.util.function.Remote;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.NeverFilter;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import java.lang.management.ManagementFactory;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link ManagementInvocationPolicy}.
 *
 * @author as 2026.05.14
 * @since 26.07
 */
public class ManagementInvocationPolicyTest
    {
    @Before
    public void registerMBean()
            throws Exception
        {
        m_mode = CoherenceModeHelper.securityHardened();
        m_server = ManagementFactory.getPlatformMBeanServer();
        m_name   = new ObjectName("Coherence:type=PolicyTest,name=unit");
        if (m_server.isRegistered(m_name))
            {
            m_server.unregisterMBean(m_name);
            }
        m_server.registerMBean(new StandardMBean(new PolicyTest(), PolicyTestMBean.class), m_name);
        }

    @After
    public void unregisterMBean()
            throws Exception
        {
        if (m_server != null && m_name != null && m_server.isRegistered(m_name))
            {
            m_server.unregisterMBean(m_name);
            }
        if (m_mode != null)
            {
            m_mode.close();
            }
        }

    @Test
    public void shouldAllowExactProductRemoteFunctions()
        {
        MBeanAccessor.QueryBuilder.ParsedQuery query = query();

        ManagementInvocationPolicy.validateRemoteFunction(new MBeanAccessor.GetAttributes(query), null, "test");
        ManagementInvocationPolicy.validateRemoteFunction(new MBeanAccessor.SetAttributes(query,
                Collections.singletonMap("Value", "updated")), null, "test");
        ManagementInvocationPolicy.validateRemoteFunction(new MBeanAccessor.Invoke(query, "echo",
                new Object[] {"value"}, new String[] {String.class.getName()}), null, "test");
        ManagementInvocationPolicy.validateRemoteFunction(new MBeanCollectorFunction(null, null, null, query),
                null, "test");
        }

    @Test
    public void shouldRejectPlainRemoteFunction()
        {
        expectSecurity(() -> ManagementInvocationPolicy.validateRemoteFunction(
                (Remote.Function<MBeanServer, Object>) server -> "nope", null, "test"));
        }

    @Test
    public void shouldValidateConcreteMutationObjectNames()
            throws Exception
        {
        assertEquals("Coherence", ManagementInvocationPolicy.validateObjectName("type=Cluster",
                "Coherence", "test").getDomain());
        assertEquals("Coherence@unit", ManagementInvocationPolicy.validateObjectName("Coherence@unit:type=Cluster",
                "Coherence", "test").getDomain());
        assertEquals("Test", ManagementInvocationPolicy.validateObjectName("Test:type=Service",
                "Coherence", "test").getDomain());

        expectSecurity(() -> ManagementInvocationPolicy.validateObjectName(
                "com.sun.management:type=DiagnosticCommand", "Coherence", "test"));
        expectSecurity(() -> ManagementInvocationPolicy.validateObjectName("java.lang:type=Runtime",
                "Coherence", "test"));
        }

    @Test
    public void shouldAllowBroadQueryPatterns()
            throws Exception
        {
        assertEquals("Coherence*", ManagementInvocationPolicy.validateQueryPattern(
                "Coherence*:type=Cache,*", "Coherence", "test").getDomain());
        assertEquals("Coherence*", ManagementInvocationPolicy.validateQueryPattern(
                new ObjectName("Coherence*:type=Cache,*"), "test").getDomain());
        assertEquals("java.*", ManagementInvocationPolicy.validateQueryPattern(
                "java.*:type=Runtime,*", "Coherence", "test").getDomain());
        expectSecurity(() -> ManagementInvocationPolicy.validateObjectName(
                "Coherence*:type=Cache,*", "Coherence", "test"));
        }

    @Test
    public void shouldRejectExecutableShapedArguments()
            throws Exception
        {
        ManagementInvocationPolicy.validateArguments(new Object[] {
                null, "value", Byte.valueOf((byte) 1), Short.valueOf((short) 2), Integer.valueOf(3),
                Long.valueOf(4L), Float.valueOf(5.0F), Double.valueOf(6.0D), BigInteger.ONE, BigDecimal.ONE,
                Boolean.TRUE, Character.valueOf('x'), new String[] {"a", "b"}, m_name},
                new String[] {String.class.getName()}, "test");

        expectSecurity(() -> ManagementInvocationPolicy.validateArguments(
                new Object[] {(Remote.Function<MBeanServer, Object>) server -> "nope"}, null, "test"));
        expectSecurity(() -> ManagementInvocationPolicy.validateArguments(new Object[] {String.class}, null, "test"));
        expectSecurity(() -> ManagementInvocationPolicy.validateArguments(new Object[] {new CustomNumber()}, null,
                "test"));
        expectSecurity(() -> ManagementInvocationPolicy.validateArguments(new Object[] {new ExecutableNumber()}, null,
                "test"));
        expectSecurity(() -> ManagementInvocationPolicy.validateArguments(new Object[] {new Object[] {
                new CustomNumber()}}, null, "test"));
        expectSecurity(() -> ManagementInvocationPolicy.validateArguments(new Object[] {new ExecutableNumber[] {
                new ExecutableNumber()}}, null, "test"));
        ManagementInvocationPolicy.validateQueryFilter(AlwaysFilter.INSTANCE(), "test");
        ManagementInvocationPolicy.validateQueryFilter(NeverFilter.INSTANCE(), "test");
        expectSecurity(() -> ManagementInvocationPolicy.validateQueryFilter(o -> true, "test"));
        }

    @Test
    public void shouldAllowProductSafeParsedQueryFilters()
        {
        ManagementInvocationPolicy.validateParsedQuery(queryWithFilter(AlwaysFilter.INSTANCE()), "test");
        ManagementInvocationPolicy.validateParsedQuery(queryWithFilter(NeverFilter.INSTANCE()), "test");
        ManagementInvocationPolicy.validateParsedQuery(queryWithFilter(
                new MBeanAccessor.QueryBuilder.NullValueFilter()), "test");
        ManagementInvocationPolicy.validateParsedQuery(queryWithFilter(
                new MBeanAccessor.QueryBuilder.EqualsValueFilter("unit")), "test");
        }

    @Test
    public void shouldRejectNestedParsedQueryFiltersBeforeEvaluation()
        {
        assertProductFunctionRejectsNestedFilter(counter ->
                new MBeanAccessor.GetAttributes(queryWithFilter(new SentinelStringFilter(counter))));
        assertProductFunctionRejectsNestedFilter(counter ->
                new MBeanAccessor.SetAttributes(queryWithFilter(new SentinelStringFilter(counter)),
                        Collections.singletonMap("Value", "updated")));
        assertProductFunctionRejectsNestedFilter(counter ->
                new MBeanAccessor.Invoke(queryWithFilter(new SentinelStringFilter(counter)), "echo",
                        new Object[] {"value"}, new String[] {String.class.getName()}));
        assertProductFunctionRejectsNestedFilter(counter ->
                new MBeanCollectorFunction(null, null, null, queryWithFilter(new SentinelStringFilter(counter))));

        AtomicInteger counter = new AtomicInteger();
        Filter<String> filter = sValue ->
            {
            counter.incrementAndGet();
            return true;
            };
        expectSecurity(() -> ManagementInvocationPolicy.validateParsedQuery(queryWithFilter(filter), "test"));
        assertEquals(0, counter.get());
        }

    @Test
    public void shouldRejectAttributeFilterBeforeEvaluation()
        {
        AtomicInteger counter = new AtomicInteger();

        expectSecurity(() -> new MBeanAccessor.GetAttributes(query(), new SentinelAttributeFilter(counter), false)
                .apply(m_server));
        assertEquals(0, counter.get());

        ManagementInvocationPolicy.validateAttributeFilter(AlwaysFilter.INSTANCE(), "test");
        ManagementInvocationPolicy.validateAttributeFilter(NeverFilter.INSTANCE(), "test");
        expectSecurity(() -> ManagementInvocationPolicy.validateAttributeFilter(new SentinelAttributeFilter(counter),
                "test"));
        assertEquals(0, counter.get());
        }

    @Test
    public void shouldValidateAttributeAndOperationDescriptors()
            throws Exception
        {
        ManagementInvocationPolicy.validateGetAttribute(m_server, m_name, "Value", "test");
        ManagementInvocationPolicy.validateSetAttribute(m_server, m_name, new Attribute("Value", "updated"), "test");
        ManagementInvocationPolicy.validateInvoke(m_server, m_name, "echo",
                new Object[] {"value"}, new String[] {String.class.getName()}, "test");
        ManagementInvocationPolicy.validateInvoke(m_server, m_name, "enabled",
                new Object[] {Boolean.TRUE}, new String[] {Boolean.class.getName()}, "test");
        ManagementInvocationPolicy.validateGetAttribute(m_server, new ObjectName("java.lang:type=Runtime"),
                "Name", "wrapper-jmx");

        expectSecurity(() -> ManagementInvocationPolicy.validateGetAttribute(m_server, m_name, "Missing", "test"));
        expectSecurity(() -> ManagementInvocationPolicy.validateInvoke(m_server, m_name, "missing",
                null, null, "test"));
        expectSecurity(() -> ManagementInvocationPolicy.validateInvoke(m_server,
                new ObjectName("com.sun.management:type=DiagnosticCommand"), "vmSystemProperties",
                null, null, "test"));
        }

    @Test
    public void shouldShadowNonWritableAttributeInCompatibilityMode()
            throws Exception
        {
        m_mode.close();
        m_mode = CoherenceModeHelper.securityCompatibility();

        ManagementInvocationPolicy.validateSetAttribute(m_server, m_name, new Attribute("ReadOnly", "updated"),
                "test");
        }

    @Test
    public void shouldRejectNonWritableAttributeInHardenedMode()
            throws Exception
        {
        expectSecurity(() -> ManagementInvocationPolicy.validateSetAttribute(m_server, m_name,
                new Attribute("ReadOnly", "updated"), "test"));
        }

    private MBeanAccessor.QueryBuilder.ParsedQuery query()
        {
        return new MBeanAccessor.QueryBuilder()
                .withMBeanDomainName("Coherence:")
                .withBaseQuery("type=PolicyTest")
                .build();
        }

    private MBeanAccessor.QueryBuilder.ParsedQuery queryWithFilter(Filter<String> filter)
        {
        return new MBeanAccessor.QueryBuilder()
                .withMBeanDomainName("Coherence:")
                .withBaseQuery("type=PolicyTest")
                .withFilter("name", filter)
                .build();
        }

    private void assertProductFunctionRejectsNestedFilter(FunctionBuilder builder)
        {
        AtomicInteger counter = new AtomicInteger();

        expectSecurity(() -> builder.build(counter).apply(m_server));
        assertEquals(0, counter.get());
        }

    private static void expectSecurity(ThrowingRunnable runnable)
        {
        try
            {
            runnable.run();
            fail("expected SecurityException");
            }
        catch (SecurityException expected)
            {
            // expected
            }
        catch (Exception e)
            {
            throw new AssertionError(e);
            }
        }

    /**
     * Test MBean contract.
     */
    public interface PolicyTestMBean
        {
        String getValue();

        void setValue(String sValue);

        String getReadOnly();

        String echo(String sValue);

        String enabled(boolean fValue);
        }

    /**
     * Test MBean implementation.
     */
    public static class PolicyTest
            implements PolicyTestMBean
        {
        @Override
        public String getValue()
            {
            return m_sValue;
            }

        @Override
        public void setValue(String sValue)
            {
            m_sValue = sValue;
            }

        @Override
        public String getReadOnly()
            {
            return "read-only";
            }

        @Override
        public String echo(String sValue)
            {
            return sValue;
            }

        @Override
        public String enabled(boolean fValue)
            {
            return String.valueOf(fValue);
            }

        private String m_sValue = "initial";
        }

    /**
     * Runnable that throws checked exceptions.
     */
    @FunctionalInterface
    private interface ThrowingRunnable
        {
        void run()
                throws Exception;
        }

    /**
     * Builds a product management function with a sentinel filter.
     */
    @FunctionalInterface
    private interface FunctionBuilder
        {
        Remote.Function<MBeanServer, ?> build(AtomicInteger counter);
        }

    /**
     * Test number subclass that must not be accepted as a scalar.
     */
    public static class CustomNumber
            extends Number
            implements Serializable
        {
        @Override
        public int intValue()
            {
            return 1;
            }

        @Override
        public long longValue()
            {
            return 1L;
            }

        @Override
        public float floatValue()
            {
            return 1.0F;
            }

        @Override
        public double doubleValue()
            {
            return 1.0D;
            }
        }

    /**
     * Test number subclass that is also executable-shaped.
     */
    public static class ExecutableNumber
            extends CustomNumber
            implements Filter<String>
        {
        @Override
        public boolean evaluate(String sValue)
            {
            return true;
            }
        }

    /**
     * Test filter that records if it is evaluated.
     */
    public static class SentinelStringFilter
            implements Filter<String>
        {
        public SentinelStringFilter(AtomicInteger counter)
            {
            m_counter = counter;
            }

        @Override
        public boolean evaluate(String sValue)
            {
            m_counter.incrementAndGet();
            return true;
            }

        private final AtomicInteger m_counter;
        }

    /**
     * Test attribute filter that records if it is evaluated.
     */
    public static class SentinelAttributeFilter
            implements Filter<MBeanAttributeInfo>
        {
        public SentinelAttributeFilter(AtomicInteger counter)
            {
            m_counter = counter;
            }

        @Override
        public boolean evaluate(MBeanAttributeInfo info)
            {
            m_counter.incrementAndGet();
            return true;
            }

        private final AtomicInteger m_counter;
        }

    private MBeanServer m_server;

    private ObjectName m_name;

    private CoherenceModeHelper.ModeScope m_mode;
    }
