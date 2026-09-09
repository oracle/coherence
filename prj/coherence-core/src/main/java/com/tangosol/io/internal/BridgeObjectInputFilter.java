/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectStreamException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Expected-type filters for product-owned Java serialization bridge fields.
 * These filters narrow but never widen the global
 * {@link DefaultObjectInputFilter} baseline and must be composed at the use
 * site with {@link DefaultObjectInputFilter#create(Object)} or the
 * local helper equivalent. New bridge categories should use a named factory
 * method here or in {@link SerializationBridgeFilters}.
 *
 * @author Aleks Seovic  2026.05.15
 * @since 26.04
 */
public final class BridgeObjectInputFilter
        implements InvocationHandler
    {
    // ----- factory methods -----------------------------------------------

    /**
     * Return a filter for JCache exception bridges.
     *
     * @return an exception bridge filter
     */
    public static Object exception()
        {
        return EXCEPTION_FILTER.proxy();
        }

    /**
     * Return a filter for management connector publish values.
     *
     * @return a management publish bridge filter
     */
    public static Object managementPublish()
        {
        return MANAGEMENT_PUBLISH_FILTER.proxy();
        }

    /**
     * Return a filter for persistent store info metadata.
     *
     * @return a persistent store info bridge filter
     */
    public static Object persistentStoreInfo()
        {
        return PERSISTENT_STORE_INFO_FILTER.proxy();
        }

    /**
     * Return a filter for passive partition-transfer metadata.
     *
     * @return a transfer metadata bridge filter
     */
    public static Object transferMetadata()
        {
        return TRANSFER_METADATA_FILTER.proxy();
        }

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a bridge filter.
     *
     * @param sReason       the telemetry rejection reason
     * @param setAllowed    the exact allowed class names
     */
    private BridgeObjectInputFilter(String sReason, Set<String> setAllowed)
        {
        f_sReason    = sReason;
        f_setAllowed = setAllowed;
        }

    // ----- InvocationHandler interface -----------------------------------

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
        String sName = method.getName();
        if ("checkInput".equals(sName))
            {
            Class<?> clz = serialClass(args[0]);
            if (clz == null)
                {
                return status("UNDECIDED");
                }

            if (isAllowed(clz))
                {
                return status("ALLOWED");
                }

            SerializationTelemetry.recordFilterCheck("rejected", f_sReason, clz, null);
            return status("REJECTED");
            }
        else if ("toString".equals(sName))
            {
            return getClass().getName();
            }
        else if ("hashCode".equals(sName))
            {
            return System.identityHashCode(this);
            }
        else if ("equals".equals(sName))
            {
            return proxy == args[0];
            }
        return null;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return {@code true} if the specified class is expected by this bridge.
     *
     * @param clz  the class to check
     *
     * @return {@code true} if the class is expected
     */
    private boolean isAllowed(Class<?> clz)
        {
        if (clz.isArray())
            {
            Class<?> clzComponent = clz.getComponentType();
            return clzComponent.isPrimitive() || isAllowed(clzComponent);
            }

        return clz.isPrimitive() || f_setAllowed.contains(clz.getName());
        }

    /**
     * Return a set containing all entries from the specified sets.
     *
     * @param setFirst   the first set
     * @param setSecond  the second set
     *
     * @return a combined set
     */
    private static Set<String> allowed(Set<String> setFirst, Set<String> setSecond)
        {
        Set<String> setAllowed = new HashSet<>(setFirst);
        setAllowed.addAll(setSecond);
        return Collections.unmodifiableSet(setAllowed);
        }

    /**
     * Return a set containing all specified class names.
     *
     * @param asClassNames  the class names
     *
     * @return a set containing all specified class names
     */
    private static Set<String> allowed(String... asClassNames)
        {
        Set<String> setAllowed = new HashSet<>();
        Collections.addAll(setAllowed, asClassNames);
        return Collections.unmodifiableSet(setAllowed);
        }

    /**
     * Return this handler's ObjectInputFilter proxy.
     *
     * @return this handler's ObjectInputFilter proxy
     */
    private Object proxy()
        {
        Object proxy = m_proxy;
        if (proxy == null && CLZ_FILTER != null)
            {
            proxy = Proxy.newProxyInstance(CLZ_FILTER.getClassLoader(), new Class<?>[] {CLZ_FILTER}, this);
            m_proxy = proxy;
            }
        return proxy;
        }

    /**
     * Return the serial class from an ObjectInputFilter.FilterInfo proxy.
     *
     * @param filterInfo  the filter info
     *
     * @return the serial class
     *
     * @throws Exception if the serial class cannot be read
     */
    private static Class<?> serialClass(Object filterInfo) throws Exception
        {
        return (Class<?>) METHOD_SERIAL_CLASS.invoke(filterInfo);
        }

    /**
     * Return an ObjectInputFilter.Status enum value.
     *
     * @param sStatus  the status name
     *
     * @return an ObjectInputFilter.Status enum value
     */
    private static Enum status(String sStatus)
        {
        return Enum.valueOf((Class<Enum>) CLZ_STATUS, sStatus);
        }

    /**
     * Return an ObjectInputFilter class for the current runtime.
     *
     * @return an ObjectInputFilter class for the current runtime
     */
    private static Class<?> filterClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return an ObjectInputFilter.Status class for the current runtime.
     *
     * @return an ObjectInputFilter.Status class for the current runtime
     */
    private static Class<?> statusClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter$Status");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter$Status");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return an ObjectInputFilter.FilterInfo class for the current runtime.
     *
     * @return an ObjectInputFilter.FilterInfo class for the current runtime
     */
    private static Class<?> filterInfoClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter$FilterInfo");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter$FilterInfo");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return the serialClass method for the current runtime FilterInfo type.
     *
     * @return the serialClass method
     */
    private static Method serialClassMethod()
        {
        try
            {
            return CLZ_FILTER_INFO == null ? null : CLZ_FILTER_INFO.getMethod("serialClass");
            }
        catch (NoSuchMethodException e)
            {
            throw new IllegalStateException(e);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Java serialization classes needed by ordinary exception state.
     */
    private static final Set<String> EXCEPTION_INFRASTRUCTURE = allowed(
            "java.lang.StackTraceElement",
            "java.util.ArrayList",
            "java.util.Collections$EmptyList",
            "java.util.Collections$UnmodifiableCollection",
            "java.util.Collections$UnmodifiableList",
            "java.util.Collections$UnmodifiableRandomAccessList");

    /**
     * Narrow JDK checked/runtime exception classes accepted by JCache exception
     * bridges.
     */
    private static final Set<String> EXCEPTION_TYPES = allowed(
            Throwable.class.getName(),
            Exception.class.getName(),
            RuntimeException.class.getName(),
            ArithmeticException.class.getName(),
            ArrayStoreException.class.getName(),
            ClassCastException.class.getName(),
            IllegalArgumentException.class.getName(),
            IllegalMonitorStateException.class.getName(),
            IllegalStateException.class.getName(),
            IndexOutOfBoundsException.class.getName(),
            NegativeArraySizeException.class.getName(),
            NullPointerException.class.getName(),
            NumberFormatException.class.getName(),
            SecurityException.class.getName(),
            UnsupportedOperationException.class.getName(),
            IOException.class.getName(),
            ObjectStreamException.class.getName(),
            InvalidClassException.class.getName());

    /**
     * Exact passive classes accepted by management connector publish bridges.
     */
    private static final Set<String> MANAGEMENT_PUBLISH_TYPES = allowed(
            "javax.management.remote.JMXServiceURL",
            "java.lang.String",
            "java.net.InetAddress",
            "java.net.Inet4Address",
            "java.net.Inet4Address$Inet4AddressHolder",
            "java.net.Inet6Address",
            "java.net.Inet6Address$Inet6AddressHolder");

    /**
     * Exact classes accepted by persistent store info bridges.
     */
    private static final Set<String> PERSISTENT_STORE_INFO_TYPES = allowed(
            "com.oracle.coherence.persistence.PersistentStoreInfo",
            "java.lang.String");

    /**
     * Exact classes accepted by partition transfer metadata bridges.
     */
    private static final Set<String> TRANSFER_METADATA_TYPES = allowed(
            "com.tangosol.util.Binary");

    /**
     * Exception bridge filter singleton.
     */
    private static final BridgeObjectInputFilter EXCEPTION_FILTER =
            new BridgeObjectInputFilter("bridge-exception-type-rejected",
                    allowed(EXCEPTION_TYPES, EXCEPTION_INFRASTRUCTURE));

    /**
     * Management publish bridge filter singleton.
     */
    private static final BridgeObjectInputFilter MANAGEMENT_PUBLISH_FILTER =
            new BridgeObjectInputFilter("bridge-management-publish-type-rejected", MANAGEMENT_PUBLISH_TYPES);

    /**
     * PersistentStoreInfo bridge filter singleton.
     */
    private static final BridgeObjectInputFilter PERSISTENT_STORE_INFO_FILTER =
            new BridgeObjectInputFilter("bridge-persistent-store-info-type-rejected", PERSISTENT_STORE_INFO_TYPES);

    /**
     * Transfer metadata bridge filter singleton.
     */
    private static final BridgeObjectInputFilter TRANSFER_METADATA_FILTER =
            new BridgeObjectInputFilter("bridge-transfer-metadata-type-rejected", TRANSFER_METADATA_TYPES);


    /**
     * The runtime ObjectInputFilter class.
     */
    private static final Class<?> CLZ_FILTER = filterClass();

    /**
     * The runtime ObjectInputFilter.Status class.
     */
    private static final Class<?> CLZ_STATUS = statusClass();

    /**
     * The runtime ObjectInputFilter.FilterInfo class.
     */
    private static final Class<?> CLZ_FILTER_INFO = filterInfoClass();

    /**
     * The FilterInfo.serialClass method.
     */
    private static final Method METHOD_SERIAL_CLASS = serialClassMethod();

    // ----- data members ---------------------------------------------------

    /**
     * Telemetry rejection reason.
     */
    private final String f_sReason;

    /**
     * Exact allowed class names.
     */
    private final Set<String> f_setAllowed;

    /**
     * This handler's ObjectInputFilter proxy.
     */
    private Object m_proxy;
    }
