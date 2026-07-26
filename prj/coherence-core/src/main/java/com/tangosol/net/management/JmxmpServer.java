/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.util.ExternalizableHelper;

import javax.management.MBeanServer;
import javax.management.remote.JMXAuthenticator;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import java.io.IOException;
import java.io.ObjectInputFilter;

import java.lang.management.ManagementFactory;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * An implementation of a Coherence {@link MBeanServerFinder}
 * that creates a {@link JMXConnectorServer} server that uses
 * JMXMP as its transport rather than RMI. This allows JMX
 * to be visible from inside a container that uses NAT'ing.
 * <p>
 * The JMXMP server can be enabled by setting the
 * {@code coherence.management.serverfactory} system property
 * to the fully qualified name of this class, or by specifically
 * configuring it in the management section of the operational
 * configuration file.
 *
 * @author Jonathan Knight 2022.04.22
 * @since 22.06
 */
public class JmxmpServer
        implements MBeanServerFinder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link JmxmpServer} that binds to the configured JMXMP host.
     */
    public JmxmpServer()
        {
        this(Config.getProperty(JMXMP_HOST_PROPERTY, DEFAULT_JMXMP_HOST));
        }

    /**
     * Create a {@link JmxmpServer} that binds to the specified address.
     *
     * @param address the address to listen on
     */
    public JmxmpServer(String address)
        {
        this.address = address;
        }

    // ----- MBeanServerFinder methods --------------------------------------

    @Override
    public MBeanServer findMBeanServer(String s)
        {
        return ensureServer(address).getMBeanServer();
        }

    @Override
    public JMXServiceURL findJMXServiceUrl(String s)
        {
        return jmxServiceURL;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the JMXMP protocol {@link JMXConnectorServer} instance, creating the instance of the connector server if
     * one does not already exist.
     *
     * @param address the address to listen on
     * @return the JMXMP protocol {@link JMXConnectorServer} instance.
     */
    private static synchronized JMXConnectorServer ensureServer(String address)
        {
        try
            {
            if (connectorServer == null)
                {
                MBeanServer         server        = ManagementFactory.getPlatformMBeanServer();
                Map<String, Object> mapEnv        = createConnectorEnvironment();

                jmxServiceURL   = createServiceURL(address);
                connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(jmxServiceURL, mapEnv, server);

                connectorServer.start();

                Logger.info("Started JMXMP connector " + connectorServer.getAddress());
                }

            return connectorServer;
            }
        catch (IOException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Create a JMXMP service URL for the specified bind address.
     *
     * @param address  the address to listen on
     *
     * @return the JMXMP service URL
     *
     * @throws IOException if the service URL cannot be created
     */
    static JMXServiceURL createServiceURL(String address)
            throws IOException
        {
        return new JMXServiceURL("jmxmp", address, Config.getInteger(JMXMP_PORT_PROPERTY, DEFAULT_JMXMP_PORT));
        }

    /**
     * Create the connector environment required by the JMXMP connector.
     *
     * @return a mutable connector environment
     */
    static Map<String, Object> createConnectorEnvironment()
        {
        Map<String, Object> mapEnv = new HashMap<>();
        mapEnv.put(JMX_DAEMON_PROPERTY, "true");
        mapEnv.put(JMXConnectorServer.AUTHENTICATOR, createAuthenticator());
        ensureSerialFilter();
        return mapEnv;
        }

    /**
     * Create the configured JMXMP authenticator.
     *
     * @return the configured authenticator
     */
    private static JMXAuthenticator createAuthenticator()
        {
        String sAuthenticator = trim(Config.getProperty(JMXMP_AUTHENTICATOR_PROPERTY));
        if (sAuthenticator == null)
            {
            sAuthenticator = trim(Config.getProperty(MBeanConnector.RMI_CUSTOM_AUTHENTICATOR_PROPERTY));
            }

        if (sAuthenticator == null)
            {
            throw new IllegalStateException("JMXMP requires a configured JMXAuthenticator using "
                    + JMXMP_AUTHENTICATOR_PROPERTY + " or " + MBeanConnector.RMI_CUSTOM_AUTHENTICATOR_PROPERTY);
            }

        try
            {
            Class<?> clzAuthenticator = Class.forName(sAuthenticator);
            if (!JMXAuthenticator.class.isAssignableFrom(clzAuthenticator))
                {
                throw new IllegalStateException("Configured JMXMP authenticator " + sAuthenticator
                        + " does not implement " + JMXAuthenticator.class.getName());
                }

            return (JMXAuthenticator) clzAuthenticator.getDeclaredConstructor().newInstance();
            }
        catch (IllegalStateException e)
            {
            throw e;
            }
        catch (ReflectiveOperationException | LinkageError e)
            {
            throw Exceptions.ensureRuntimeException(e,
                    "Failed to instantiate configured JMXMP authenticator " + sAuthenticator);
            }
        }

    /**
     * Ensure a process-wide serial filter is present before the JMXMP connector
     * is created.
     */
    private static void ensureSerialFilter()
        {
        SerialFilterAccess access  = s_serialFilterAccess;
        Object             oFilter = access.getSerialFilter();
        if (oFilter != null)
            {
            Logger.info("JMXMP connector is using the existing JVM-wide ObjectInputFilter");
            return;
            }

        try
            {
            access.setSerialFilter(createJmxmpSerialFilter());
            }
        catch (IllegalStateException e)
            {
            oFilter = access.getSerialFilter();
            if (oFilter != null)
                {
                Logger.info("JMXMP connector is using the JVM-wide ObjectInputFilter installed by another component");
                return;
                }
            throw Exceptions.ensureRuntimeException(e,
                    "Unable to install or observe a JVM-wide ObjectInputFilter before starting JMXMP");
            }

        oFilter = access.getSerialFilter();
        if (oFilter == null)
            {
            throw new IllegalStateException("Unable to install or observe a JVM-wide ObjectInputFilter before starting JMXMP");
            }

        Logger.info("JMXMP connector installed a Coherence JVM-wide ObjectInputFilter fallback");
        }

    /**
     * Create the Coherence-owned fallback serial filter for JMXMP.
     *
     * @return the fallback serial filter
     */
    static ObjectInputFilter createJmxmpSerialFilter()
        {
        return JmxmpServer::checkJmxmpSerialInput;
        }

    /**
     * Check a serialized class against the Coherence-owned JMXMP fallback
     * serial filter.
     *
     * @param info  the filter info
     *
     * @return the filter status
     */
    private static ObjectInputFilter.Status checkJmxmpSerialInput(ObjectInputFilter.FilterInfo info)
        {
        if (exceedsJmxmpSerialLimits(info))
            {
            return ObjectInputFilter.Status.REJECTED;
            }

        Class<?> clz = info.serialClass();
        if (clz == null)
            {
            return ObjectInputFilter.Status.UNDECIDED;
            }

        if (clz.isArray())
            {
            return checkJmxmpArrayInput(clz);
            }

        if (clz.isPrimitive() || ALLOWED_JAVA_VALUE_CLASSES.contains(clz) || ALLOWED_JMX_VALUE_CLASSES.contains(clz))
            {
            return ObjectInputFilter.Status.ALLOWED;
            }

        String sName = clz.getName();
        if (isAllowedJmxmpPackage(sName) && !isRejectedClassName(sName))
            {
            return ObjectInputFilter.Status.ALLOWED;
            }

        return ObjectInputFilter.Status.REJECTED;
        }

    /**
     * Check array classes against the JMXMP fallback serial filter.
     *
     * @param clz  the array class
     *
     * @return the filter status
     */
    private static ObjectInputFilter.Status checkJmxmpArrayInput(Class<?> clz)
        {
        Class<?> clzComponent = clz.getComponentType();
        while (clzComponent != null && clzComponent.isArray())
            {
            clzComponent = clzComponent.getComponentType();
            }

        if (clzComponent == null || clzComponent.isPrimitive() || clzComponent == Object.class)
            {
            return ObjectInputFilter.Status.ALLOWED;
            }

        return checkJmxmpSerialInput(new FilterInfo(clzComponent));
        }

    /**
     * Return {@code true} if the serialized input metrics exceed the bounded
     * fallback contract for pre-authentication JMXMP traffic.
     *
     * @param info  the filter info
     *
     * @return {@code true} if any resource limit is exceeded
     */
    private static boolean exceedsJmxmpSerialLimits(ObjectInputFilter.FilterInfo info)
        {
        return exceedsLimit(info.arrayLength(), MAX_JMXMP_ARRAY_LENGTH)
                || exceedsLimit(info.depth(), MAX_JMXMP_GRAPH_DEPTH)
                || exceedsLimit(info.references(), MAX_JMXMP_REFERENCES)
                || exceedsLimit(info.streamBytes(), MAX_JMXMP_STREAM_BYTES);
        }

    /**
     * Return {@code true} if a known resource metric exceeds its configured
     * maximum.
     *
     * @param cValue  the observed metric, or a negative value when unknown
     * @param cMax    the maximum allowed value
     *
     * @return {@code true} if the metric is known and over the maximum
     */
    private static boolean exceedsLimit(long cValue, long cMax)
        {
        return cValue >= 0 && cValue > cMax;
        }

    /**
     * Return {@code true} if the class name belongs to an allowed JMXMP value
     * package.
     *
     * @param sName  the class name
     *
     * @return {@code true} if allowed
     */
    private static boolean isAllowedJmxmpPackage(String sName)
        {
        return sName.startsWith("javax.management.openmbean.")
                || sName.startsWith("javax.management.remote.message.")
                || sName.startsWith("javax.management.remote.generic.");
        }

    /**
     * Return {@code true} if the class name is known risky or outside the
     * fallback JMXMP value contract.
     *
     * @param sName  the class name
     *
     * @return {@code true} if rejected
     */
    private static boolean isRejectedClassName(String sName)
        {
        return sName.equals("javax.management.BadAttributeValueExpException")
                || sName.equals("javax.management.MBeanServerInvocationHandler")
                || sName.equals("javax.management.loading.MLet")
                || sName.equals("java.lang.Class")
                || sName.equals("java.lang.ClassLoader")
                || sName.equals("java.lang.Module")
                || sName.equals("java.lang.ProcessBuilder")
                || sName.equals("java.lang.Runtime")
                || sName.equals("java.lang.System")
                || sName.equals("java.lang.Thread")
                || sName.equals("java.lang.ThreadGroup")
                || sName.startsWith("java.io.")
                || sName.startsWith("java.lang.invoke.")
                || sName.startsWith("java.lang.reflect.")
                || sName.startsWith("java.net.")
                || sName.startsWith("java.nio.file.")
                || sName.startsWith("javax.naming.")
                || sName.startsWith("javax.script.")
                || sName.startsWith("com.tangosol.")
                || sName.startsWith("com.sun.org.apache.xalan.")
                || sName.startsWith("org.apache.commons.collections.");
        }

    /**
     * Trim a configured value and return {@code null} for empty strings.
     *
     * @param sValue  the configured value
     *
     * @return the trimmed value or {@code null}
     */
    private static String trim(String sValue)
        {
        if (sValue != null)
            {
            sValue = sValue.trim();
            }
        return sValue == null || sValue.isEmpty() ? null : sValue;
        }

    /**
     * Return the address this server will bind to.
     *
     * @return the address this server will bind to
     */
    String getAddress()
        {
        return address;
        }

    /**
     * Set the serial-filter access helper for tests.
     *
     * @param access  the serial-filter access helper
     */
    static void setSerialFilterAccess(SerialFilterAccess access)
        {
        s_serialFilterAccess = access == null ? DEFAULT_SERIAL_FILTER_ACCESS : access;
        }

    // ----- inner interface: SerialFilterAccess ---------------------------

    /**
     * Access to the JVM-wide serial filter.
     */
    interface SerialFilterAccess
        {
        /**
         * Return the current JVM-wide serial filter.
         *
         * @return the current JVM-wide serial filter, or {@code null}
         */
        Object getSerialFilter();

        /**
         * Set the JVM-wide serial filter.
         *
         * @param filter  the filter to set
         */
        void setSerialFilter(ObjectInputFilter filter);
        }

    // ----- inner class: DefaultSerialFilterAccess ------------------------

    /**
     * Default JVM-wide serial filter access.
     */
    private static class DefaultSerialFilterAccess
            implements SerialFilterAccess
        {
        @Override
        public Object getSerialFilter()
            {
            return ExternalizableHelper.getConfigSerialFilter();
            }

        @Override
        public void setSerialFilter(ObjectInputFilter filter)
            {
            ObjectInputFilter.Config.setSerialFilter(filter);
            }
        }

    // ----- inner class: FilterInfo ---------------------------------------

    /**
     * Minimal filter info used to recurse into array component checks.
     */
    private static class FilterInfo
            implements ObjectInputFilter.FilterInfo
        {
        /**
         * Create a {@link FilterInfo}.
         *
         * @param clzSerial  the class to check
         */
        private FilterInfo(Class<?> clzSerial)
            {
            m_clzSerial = clzSerial;
            }

        @Override
        public Class<?> serialClass()
            {
            return m_clzSerial;
            }

        @Override
        public long arrayLength()
            {
            return -1L;
            }

        @Override
        public long depth()
            {
            return 1L;
            }

        @Override
        public long references()
            {
            return 0L;
            }

        @Override
        public long streamBytes()
            {
            return 0L;
            }

        // ----- data members -----------------------------------------------

        /**
         * The class to check.
         */
        private final Class<?> m_clzSerial;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Property name to specify the default JMXMP host.
     */
    public static final String JMXMP_HOST_PROPERTY = "coherence.jmxmp.host";

    /**
     * Property name to specify the JMXMP port.
     */
    public static final String JMXMP_PORT_PROPERTY = "coherence.jmxmp.port";

    /**
     * Property name to specify the JMXMP authenticator.
     */
    public static final String JMXMP_AUTHENTICATOR_PROPERTY = "coherence.jmxmp.authenticator";

    /**
     * Default JMXMP host.
     */
    public static final String DEFAULT_JMXMP_HOST = "127.0.0.1";

    /**
     * Default JMXMP port.
     */
    public static final int DEFAULT_JMXMP_PORT = 9000;

    /**
     * JMX environment property that keeps the connector server daemonized.
     */
    private static final String JMX_DAEMON_PROPERTY = "jmx.remote.x.daemon";

    /**
     * Allowed Java value classes for the fallback JMXMP serial filter.
     */
    private static final Set<Class<?>> ALLOWED_JAVA_VALUE_CLASSES = Set.of(
            String.class,
            Boolean.class,
            Character.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            BigInteger.class,
            BigDecimal.class,
            ArrayList.class,
            LinkedList.class,
            HashMap.class,
            LinkedHashMap.class,
            HashSet.class,
            LinkedHashSet.class,
            Hashtable.class,
            Vector.class);

    /**
     * Maximum array length accepted by the Coherence JMXMP fallback filter.
     */
    static final long MAX_JMXMP_ARRAY_LENGTH = 16_384L;

    /**
     * Maximum object graph depth accepted by the Coherence JMXMP fallback
     * filter.
     */
    static final long MAX_JMXMP_GRAPH_DEPTH = 64L;

    /**
     * Maximum object reference count accepted by the Coherence JMXMP fallback
     * filter.
     */
    static final long MAX_JMXMP_REFERENCES = 100_000L;

    /**
     * Maximum stream byte count accepted by the Coherence JMXMP fallback
     * filter.
     */
    static final long MAX_JMXMP_STREAM_BYTES = 16L * 1024L * 1024L;

    /**
     * Allowed JMX value classes for the fallback JMXMP serial filter.
     */
    private static final Set<Class<?>> ALLOWED_JMX_VALUE_CLASSES = Set.of(
            javax.management.Attribute.class,
            javax.management.AttributeList.class,
            javax.management.ImmutableDescriptor.class,
            javax.management.MBeanAttributeInfo.class,
            javax.management.MBeanConstructorInfo.class,
            javax.management.MBeanFeatureInfo.class,
            javax.management.MBeanInfo.class,
            javax.management.MBeanNotificationInfo.class,
            javax.management.MBeanOperationInfo.class,
            javax.management.MBeanParameterInfo.class,
            javax.management.MBeanServerNotification.class,
            javax.management.Notification.class,
            javax.management.NotificationFilterSupport.class,
            javax.management.ObjectInstance.class,
            javax.management.ObjectName.class,
            javax.management.remote.JMXConnectionNotification.class,
            javax.management.remote.JMXPrincipal.class,
            javax.management.remote.NotificationResult.class,
            javax.management.remote.TargetedNotification.class,
            javax.security.auth.Subject.class);

    /**
     * Default serial-filter access helper.
     */
    private static final SerialFilterAccess DEFAULT_SERIAL_FILTER_ACCESS = new DefaultSerialFilterAccess();

    /**
     * Serial-filter access helper.
     */
    private static SerialFilterAccess s_serialFilterAccess = DEFAULT_SERIAL_FILTER_ACCESS;

    /**
     * The JMXServiceURL for the MBeanConnector used by the Coherence JMX framework.
     */
    private static JMXServiceURL jmxServiceURL;

    /**
     * The {@link JMXConnectorServer} using the JMXMP protocol.
     */
    private static JMXConnectorServer connectorServer;

    /**
     * The address to bind to.
     */
    private final String address;
    }
