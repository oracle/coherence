/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectStreamException;

import java.util.HashSet;
import java.util.Set;

/**
 * Expected-type filters for product-owned Java serialization bridge fields.
 * These filters narrow but never widen the global
 * {@link DefaultObjectInputFilter} baseline and must be composed at the use
 * site with {@link DefaultObjectInputFilter#create(ObjectInputFilter)} or the
 * local helper equivalent. New bridge categories should use a named factory
 * method here or in {@link SerializationBridgeFilters}.
 *
 * @author Aleks Seovic  2026.05.15
 * @since 26.07
 */
public final class BridgeObjectInputFilter
        implements ObjectInputFilter
    {
    // ----- factory methods -----------------------------------------------

    /**
     * Return a filter for transaction result exception bridges.
     *
     * @return a runtime exception bridge filter
     */
    public static ObjectInputFilter runtimeException()
        {
        return RUNTIME_EXCEPTION_FILTER;
        }

    /**
     * Return a filter for JCache exception bridges.
     *
     * @return an exception bridge filter
     */
    public static ObjectInputFilter exception()
        {
        return EXCEPTION_FILTER;
        }

    /**
     * Return a filter for management connector publish values.
     *
     * @return a management publish bridge filter
     */
    public static ObjectInputFilter managementPublish()
        {
        return MANAGEMENT_PUBLISH_FILTER;
        }

    /**
     * Return a filter for persistent store info metadata.
     *
     * @return a persistent store info bridge filter
     */
    public static ObjectInputFilter persistentStoreInfo()
        {
        return PERSISTENT_STORE_INFO_FILTER;
        }

    /**
     * Return a filter for passive partition-transfer metadata.
     *
     * @return a transfer metadata bridge filter
     */
    public static ObjectInputFilter transferMetadata()
        {
        return TRANSFER_METADATA_FILTER;
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

    // ----- ObjectInputFilter interface -----------------------------------

    @Override
    public Status checkInput(FilterInfo filterInfo)
        {
        Class<?> clz = filterInfo.serialClass();
        if (clz == null)
            {
            return Status.UNDECIDED;
            }

        if (isAllowed(clz))
            {
            return Status.ALLOWED;
            }

        SerializationTelemetry.recordFilterCheck("rejected", f_sReason, clz, null);
        return Status.REJECTED;
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
        return Set.copyOf(setAllowed);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Java serialization classes needed by ordinary exception state.
     */
    private static final Set<String> EXCEPTION_INFRASTRUCTURE = Set.of(
            "java.lang.StackTraceElement",
            "java.util.Collections$EmptyList");

    /**
     * Narrow JDK runtime exception classes accepted by transaction result
     * exception bridges.
     */
    private static final Set<String> RUNTIME_EXCEPTION_TYPES = Set.of(
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
            UnsupportedOperationException.class.getName());

    /**
     * Narrow JDK checked/runtime exception classes accepted by JCache exception
     * bridges.
     */
    private static final Set<String> EXCEPTION_TYPES = Set.of(
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
    private static final Set<String> MANAGEMENT_PUBLISH_TYPES = Set.of(
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
    private static final Set<String> PERSISTENT_STORE_INFO_TYPES = Set.of(
            "com.oracle.coherence.persistence.PersistentStoreInfo",
            "java.lang.String");

    /**
     * Exact classes accepted by partition transfer metadata bridges.
     */
    private static final Set<String> TRANSFER_METADATA_TYPES = Set.of(
            "com.tangosol.util.Binary");

    /**
     * Runtime exception bridge filter singleton.
     */
    private static final BridgeObjectInputFilter RUNTIME_EXCEPTION_FILTER =
            new BridgeObjectInputFilter("bridge-runtime-exception-type-rejected",
                    allowed(RUNTIME_EXCEPTION_TYPES, EXCEPTION_INFRASTRUCTURE));

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

    // ----- data members ---------------------------------------------------

    /**
     * Telemetry rejection reason.
     */
    private final String f_sReason;

    /**
     * Exact allowed class names.
     */
    private final Set<String> f_setAllowed;
    }
