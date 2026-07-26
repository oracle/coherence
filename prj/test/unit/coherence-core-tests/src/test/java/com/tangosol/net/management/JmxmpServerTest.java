/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputFilter;

import java.math.BigDecimal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.management.Attribute;
import javax.management.BadAttributeValueExpException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;

import javax.security.auth.Subject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link JmxmpServer}.
 *
 * @author Aleks Seovic  2026.05.14
 * @since 26.04
 */
public class JmxmpServerTest
    {
    @After
    public void cleanup()
        {
        System.clearProperty(JmxmpServer.JMXMP_HOST_PROPERTY);
        System.clearProperty(JmxmpServer.JMXMP_PORT_PROPERTY);
        System.clearProperty(JmxmpServer.JMXMP_AUTHENTICATOR_PROPERTY);
        System.clearProperty(MBeanConnector.RMI_CUSTOM_AUTHENTICATOR_PROPERTY);
        JmxmpServer.setSerialFilterAccess(null);
        }

    @Test
    public void shouldUseLoopbackAsDefaultHost()
        {
        assertEquals(JmxmpServer.DEFAULT_JMXMP_HOST, new JmxmpServer().getAddress());
        assertFalse("0.0.0.0".equals(new JmxmpServer().getAddress()));
        }

    @Test
    public void shouldUseConfiguredDefaultHost()
        {
        System.setProperty(JmxmpServer.JMXMP_HOST_PROPERTY, "192.0.2.10");

        assertEquals("192.0.2.10", new JmxmpServer().getAddress());
        }

    @Test
    public void shouldPreserveExplicitHost()
        {
        System.setProperty(JmxmpServer.JMXMP_HOST_PROPERTY, "192.0.2.10");

        assertEquals("0.0.0.0", new JmxmpServer("0.0.0.0").getAddress());
        }

    @Test
    public void shouldPreserveConfiguredPort()
            throws IOException
        {
        assertEquals(JmxmpServer.DEFAULT_JMXMP_PORT, JmxmpServer.createServiceURL("127.0.0.1").getPort());

        System.setProperty(JmxmpServer.JMXMP_PORT_PROPERTY, "9101");

        assertEquals(9101, JmxmpServer.createServiceURL("127.0.0.1").getPort());
        }

    @Test
    public void shouldFailClosedWithoutAuthenticator()
        {
        TestSerialFilterAccess access = new TestSerialFilterAccess(EXISTING_FILTER);
        JmxmpServer.setSerialFilterAccess(access);

        expectRuntime(() -> JmxmpServer.createConnectorEnvironment());

        assertEquals(0, access.getSetCount());
        }

    @Test
    public void shouldFailClosedForInvalidAuthenticatorClass()
        {
        System.setProperty(JmxmpServer.JMXMP_AUTHENTICATOR_PROPERTY, "no.such.Authenticator");
        JmxmpServer.setSerialFilterAccess(new TestSerialFilterAccess(EXISTING_FILTER));

        expectRuntime(() -> JmxmpServer.createConnectorEnvironment());
        }

    @Test
    public void shouldFailClosedForNonJmxAuthenticatorClass()
        {
        System.setProperty(JmxmpServer.JMXMP_AUTHENTICATOR_PROPERTY, NotAuthenticator.class.getName());
        JmxmpServer.setSerialFilterAccess(new TestSerialFilterAccess(EXISTING_FILTER));

        expectRuntime(() -> JmxmpServer.createConnectorEnvironment());
        }

    @Test
    public void shouldPreferJmxmpAuthenticator()
        {
        System.setProperty(JmxmpServer.JMXMP_AUTHENTICATOR_PROPERTY, PrimaryAuthenticator.class.getName());
        System.setProperty(MBeanConnector.RMI_CUSTOM_AUTHENTICATOR_PROPERTY, FallbackAuthenticator.class.getName());
        JmxmpServer.setSerialFilterAccess(new TestSerialFilterAccess(EXISTING_FILTER));

        Map<String, Object> mapEnv = JmxmpServer.createConnectorEnvironment();

        assertTrue(mapEnv.get(JMXConnectorServer.AUTHENTICATOR) instanceof PrimaryAuthenticator);
        }

    @Test
    public void shouldUseManagementAuthenticatorFallback()
        {
        System.setProperty(MBeanConnector.RMI_CUSTOM_AUTHENTICATOR_PROPERTY, FallbackAuthenticator.class.getName());
        JmxmpServer.setSerialFilterAccess(new TestSerialFilterAccess(EXISTING_FILTER));

        Map<String, Object> mapEnv = JmxmpServer.createConnectorEnvironment();

        assertTrue(mapEnv.get(JMXConnectorServer.AUTHENTICATOR) instanceof FallbackAuthenticator);
        }

    @Test
    public void shouldReturnNonNullEnvironment()
        {
        System.setProperty(JmxmpServer.JMXMP_AUTHENTICATOR_PROPERTY, PrimaryAuthenticator.class.getName());
        JmxmpServer.setSerialFilterAccess(new TestSerialFilterAccess(EXISTING_FILTER));

        Map<String, Object> mapEnv = JmxmpServer.createConnectorEnvironment();

        assertNotNull(mapEnv);
        assertTrue(mapEnv.get(JMXConnectorServer.AUTHENTICATOR) instanceof PrimaryAuthenticator);
        assertEquals("true", mapEnv.get("jmx.remote.x.daemon"));
        }

    @Test
    public void shouldInstallFallbackSerialFilterWhenAbsent()
        {
        System.setProperty(JmxmpServer.JMXMP_AUTHENTICATOR_PROPERTY, PrimaryAuthenticator.class.getName());
        TestSerialFilterAccess access = new TestSerialFilterAccess(null);
        JmxmpServer.setSerialFilterAccess(access);

        JmxmpServer.createConnectorEnvironment();

        assertEquals(1, access.getSetCount());
        assertNotNull(access.getSerialFilter());
        }

    @Test
    public void shouldNotReplaceExistingSerialFilter()
        {
        System.setProperty(JmxmpServer.JMXMP_AUTHENTICATOR_PROPERTY, PrimaryAuthenticator.class.getName());
        TestSerialFilterAccess access = new TestSerialFilterAccess(EXISTING_FILTER);
        JmxmpServer.setSerialFilterAccess(access);

        JmxmpServer.createConnectorEnvironment();

        assertEquals(0, access.getSetCount());
        assertSame(EXISTING_FILTER, access.getSerialFilter());
        }

    @Test
    public void shouldHandleSerialFilterRace()
        {
        System.setProperty(JmxmpServer.JMXMP_AUTHENTICATOR_PROPERTY, PrimaryAuthenticator.class.getName());
        TestSerialFilterAccess access = new TestSerialFilterAccess(null);
        access.setRaceWinnerFilter(EXISTING_FILTER);
        JmxmpServer.setSerialFilterAccess(access);

        JmxmpServer.createConnectorEnvironment();

        assertEquals(1, access.getSetCount());
        assertSame(EXISTING_FILTER, access.getSerialFilter());
        }

    @Test
    public void shouldFailClosedWhenSerialFilterCannotBeObservedOrInstalled()
        {
        System.setProperty(JmxmpServer.JMXMP_AUTHENTICATOR_PROPERTY, PrimaryAuthenticator.class.getName());
        TestSerialFilterAccess access = new TestSerialFilterAccess(null);
        access.setFailSet(true);
        JmxmpServer.setSerialFilterAccess(access);

        expectRuntime(() -> JmxmpServer.createConnectorEnvironment());

        assertEquals(1, access.getSetCount());
        }

    @Test
    public void shouldAllowExpectedJmxmpValueShapes()
            throws Exception
        {
        ObjectInputFilter filter = JmxmpServer.createJmxmpSerialFilter();

        assertAllowed(filter, String.class);
        assertAllowed(filter, BigDecimal.class);
        assertAllowed(filter, String[].class);
        assertAllowed(filter, Object[].class);
        assertAllowed(filter, Attribute.class);
        assertAllowed(filter, ObjectName.class);
        assertAllowed(filter, CompositeDataSupport.class);
        assertAllowed(filter, javax.management.remote.NotificationResult.class);
        }

    @Test
    public void shouldRejectExecutableAndApplicationShapes()
        {
        ObjectInputFilter filter = JmxmpServer.createJmxmpSerialFilter();

        assertRejected(filter, Runtime.class);
        assertRejected(filter, ProcessBuilder.class);
        assertRejected(filter, Class.class);
        assertRejected(filter, java.io.File.class);
        assertRejected(filter, BadAttributeValueExpException.class);
        assertRejected(filter, ManagementInvocationPolicy.class);
        assertRejected(filter, JmxmpServerTest.class);
        }

    @Test
    public void shouldRejectBehaviorBearingCollectionShapes()
        {
        ObjectInputFilter filter = JmxmpServer.createJmxmpSerialFilter();

        assertRejected(filter, TreeMap.class);
        assertRejected(filter, TreeSet.class);
        assertRejected(filter, Collections.reverseOrder().getClass());
        assertRejected(filter, Collections.unmodifiableSortedMap(new TreeMap<>()).getClass());
        assertRejected(filter, Collections.checkedMap(new HashMap<>(), String.class, String.class).getClass());
        }

    @Test
    public void shouldApplyFallbackResourceBoundsBeforeAllowingClasses()
        {
        ObjectInputFilter filter = JmxmpServer.createJmxmpSerialFilter();

        assertAllowed(filter, new TestFilterInfo(byte[].class, JmxmpServer.MAX_JMXMP_ARRAY_LENGTH,
                JmxmpServer.MAX_JMXMP_GRAPH_DEPTH, JmxmpServer.MAX_JMXMP_REFERENCES,
                JmxmpServer.MAX_JMXMP_STREAM_BYTES));
        assertAllowed(filter, new TestFilterInfo(Object[].class, 8L, 4L, 16L, 1_024L));
        assertEquals(ObjectInputFilter.Status.UNDECIDED,
                filter.checkInput(new TestFilterInfo(null, -1L, 4L, 16L, 1_024L)));

        assertRejected(filter, new TestFilterInfo(byte[].class, JmxmpServer.MAX_JMXMP_ARRAY_LENGTH + 1L,
                1L, 0L, 0L));
        assertRejected(filter, new TestFilterInfo(Object[].class, JmxmpServer.MAX_JMXMP_ARRAY_LENGTH + 1L,
                1L, 0L, 0L));
        assertRejected(filter, new TestFilterInfo(null, -1L, JmxmpServer.MAX_JMXMP_GRAPH_DEPTH + 1L,
                0L, 0L));
        assertRejected(filter, new TestFilterInfo(null, -1L, 1L, JmxmpServer.MAX_JMXMP_REFERENCES + 1L,
                0L));
        assertRejected(filter, new TestFilterInfo(null, -1L, 1L, 0L,
                JmxmpServer.MAX_JMXMP_STREAM_BYTES + 1L));
        }

    @Test
    public void shouldNotPassNullEnvironmentToConnectorFactory()
            throws IOException
        {
        String sSource = Files.readString(findRepoFile("coherence-core/src/main/java/com/tangosol/net/management/JmxmpServer.java"));

        assertFalse(sSource.contains("newJMXConnectorServer(jmxServiceURL, null"));
        }

    @Test
    public void shouldKeepFallbackFilterAllowlistExactAndBounded()
            throws IOException
        {
        String sSource = Files.readString(findRepoFile("coherence-core/src/main/java/com/tangosol/net/management/JmxmpServer.java"));

        assertFalse(sSource.contains("java.util.Collections$"));
        assertFalse(sSource.contains("java.util.Arrays$"));
        assertTrue(sSource.contains("MAX_JMXMP_ARRAY_LENGTH"));
        assertTrue(sSource.contains("MAX_JMXMP_GRAPH_DEPTH"));
        assertTrue(sSource.contains("MAX_JMXMP_REFERENCES"));
        assertTrue(sSource.contains("MAX_JMXMP_STREAM_BYTES"));
        assertTrue(sSource.contains("exceedsJmxmpSerialLimits(info)"));
        }

    private static void assertAllowed(ObjectInputFilter filter, Class<?> clz)
        {
        assertEquals(clz.getName(), ObjectInputFilter.Status.ALLOWED, filter.checkInput(new TestFilterInfo(clz)));
        }

    private static void assertAllowed(ObjectInputFilter filter, ObjectInputFilter.FilterInfo info)
        {
        assertEquals(ObjectInputFilter.Status.ALLOWED, filter.checkInput(info));
        }

    private static void assertRejected(ObjectInputFilter filter, Class<?> clz)
        {
        assertEquals(clz.getName(), ObjectInputFilter.Status.REJECTED, filter.checkInput(new TestFilterInfo(clz)));
        }

    private static void assertRejected(ObjectInputFilter filter, ObjectInputFilter.FilterInfo info)
        {
        assertEquals(ObjectInputFilter.Status.REJECTED, filter.checkInput(info));
        }

    private static void expectRuntime(ThrowingRunnable runnable)
        {
        try
            {
            runnable.run();
            fail("expected runtime exception");
            }
        catch (RuntimeException expected)
            {
            // expected
            }
        }

    private static Path findRepoFile(String sRelative)
            throws IOException
        {
        Path path = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (path != null)
            {
            Path file = path.resolve(sRelative);
            if (Files.isRegularFile(file))
                {
                return file;
                }
            path = path.getParent();
            }
        throw new IOException("Unable to find " + sRelative);
        }

    public static class PrimaryAuthenticator
            implements JMXAuthenticator
        {
        @Override
        public Subject authenticate(Object credentials)
            {
            return new Subject();
            }
        }

    public static class FallbackAuthenticator
            implements JMXAuthenticator
        {
        @Override
        public Subject authenticate(Object credentials)
            {
            return new Subject();
            }
        }

    public static class SmokeAuthenticator
            implements JMXAuthenticator
        {
        @Override
        public Subject authenticate(Object credentials)
            {
            if (containsOk(credentials))
                {
                return new Subject();
                }
            throw new SecurityException("bad credentials: " + describe(credentials));
            }

        private static boolean containsOk(Object credentials)
            {
            if ("ok".equals(credentials))
                {
                return true;
                }
            if (credentials instanceof Object[])
                {
                Object[] aoCredentials = (Object[]) credentials;
                for (Object credential : aoCredentials)
                    {
                    if (containsOk(credential))
                        {
                        return true;
                        }
                    }
                }
            return false;
            }

        private static String describe(Object credentials)
            {
            if (credentials == null)
                {
                return "null";
                }
            if (credentials instanceof Object[])
                {
                Object[] aoCredentials = (Object[]) credentials;
                return credentials.getClass().getName() + Arrays.deepToString(aoCredentials);
                }
            return credentials.getClass().getName() + ':' + credentials;
            }
        }

    public static class NotAuthenticator
        {
        }

    private interface ThrowingRunnable
        {
        void run();
        }

    private static class TestSerialFilterAccess
            implements JmxmpServer.SerialFilterAccess
        {
        TestSerialFilterAccess(Object filter)
            {
            m_filter = filter;
            }

        @Override
        public Object getSerialFilter()
            {
            if (m_filter == null && m_fSetFailed)
                {
                return m_filterRaceWinner;
                }
            return m_filter;
            }

        @Override
        public void setSerialFilter(ObjectInputFilter filter)
            {
            ++m_cSet;
            if (m_filterRaceWinner != null || m_fFailSet)
                {
                m_fSetFailed = true;
                throw new IllegalStateException("filter already configured");
                }
            m_filter = filter;
            }

        int getSetCount()
            {
            return m_cSet;
            }

        void setRaceWinnerFilter(Object filter)
            {
            m_filterRaceWinner = filter;
            }

        void setFailSet(boolean fFailSet)
            {
            m_fFailSet = fFailSet;
            }

        private Object  m_filter;
        private Object  m_filterRaceWinner;
        private boolean m_fFailSet;
        private boolean m_fSetFailed;
        private int     m_cSet;
        }

    private static class TestFilterInfo
            implements ObjectInputFilter.FilterInfo
        {
        TestFilterInfo(Class<?> serialClass)
            {
            this(serialClass, -1L, 1L, 0L, 0L);
            }

        TestFilterInfo(Class<?> serialClass, long cArrayLength, long cDepth, long cReferences, long cbStream)
            {
            m_clzSerialClass = serialClass;
            m_cArrayLength   = cArrayLength;
            m_cDepth         = cDepth;
            m_cReferences    = cReferences;
            m_cbStream       = cbStream;
            }

        @Override
        public Class<?> serialClass()
            {
            return m_clzSerialClass;
            }

        @Override
        public long arrayLength()
            {
            return m_cArrayLength;
            }

        @Override
        public long depth()
            {
            return m_cDepth;
            }

        @Override
        public long references()
            {
            return m_cReferences;
            }

        @Override
        public long streamBytes()
            {
            return m_cbStream;
            }

        private final Class<?> m_clzSerialClass;
        private final long     m_cArrayLength;
        private final long     m_cDepth;
        private final long     m_cReferences;
        private final long     m_cbStream;
        }

    private static final ObjectInputFilter EXISTING_FILTER = info -> ObjectInputFilter.Status.UNDECIDED;
    }
